package dev.wildcatter.tailings.fabric.gametest;

import dev.wildcatter.block.BigPartBlock;
import dev.wildcatter.block.GensetControlPanelBlockEntity;
import dev.wildcatter.block.GensetPartBlockEntity;
import dev.wildcatter.block.GensetSkid;
import dev.wildcatter.multiblock.BigParts;
import dev.wildcatter.tailings.TailingsBlocks;
import dev.wildcatter.tailings.TailingsEntities;
import dev.wildcatter.tailings.block.DumpBlockEntity;
import dev.wildcatter.tailings.block.MineRailBlock;
import dev.wildcatter.tailings.block.PlantBlockEntity;
import dev.wildcatter.tailings.block.ShaftLayout;
import dev.wildcatter.tailings.block.VentNetwork;
import dev.wildcatter.tailings.block.VentState;
import dev.wildcatter.tailings.entity.MineVehicleEntity;
import dev.wildcatter.tailings.entity.ShaftSkipEntity;
import dev.wildcatter.tailings.fabric.gametest.Trailer.Cam;
import dev.wildcatter.tailings.haul.Battery;
import dev.wildcatter.tailings.haul.Haul;
import dev.wildcatter.tailings.mine.Airflow;
import dev.wildcatter.tailings.mine.Heading;
import dev.wildcatter.tailings.plant.Machine;
import dev.wildcatter.tailings.shaft.Hoisting;
import dev.wildcatter.tailings.shaft.RaiseBoring;
import dev.wildcatter.tailings.shaft.Winder;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.function.IntFunction;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ParticleStatus;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ButtonBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Heavy Iron, episode 7, "Shaft, hoist and ventilation" (TAILINGS_ONLY=hi07): one small mine on real power.
 * <ul>
 * <li>the shaft: a headframe over a 5 x 5 shaft about 120 m deep, its bin, the hoist house behind it with its two drums, two 3 t skips
 * in balance and the cage beside them; at the bottom, the rail level: a track grizzly over a dump pocket, the apron feeder and the
 * measuring flask that loads the skips; the rock west of the shaft cut away as a section, open to the sky, so the shaft and the
 * bottom show;</li>
 * <li>the vent raise: a raise borer north of it bores a 5 m raise down to the rail level's drive (the pilot down, the reamer up; the
 * last of it sped up off camera); then a vent fan at its collar blows down a duct to the drive's face;</li>
 * <li>the power: two Wildcatter 1 MW gas sets stepped up to the medium voltage for the hoist and the raise borer; a 500 kW diesel set
 * at the industrial voltage for the fan and, down the raise, the feeder; fuel and gas put in by the scene;</li>
 * <li>a villager (no AI) at the collar for scale, and riding the cage; the cage called with the landings' call buttons; the cage on its
 * own winder beside the hoist house, its drum and its sheave in the headframe turning (fix2-hi07, cage-winder.md).</li>
 * </ul>
 * Filmed at 2880 x 1620 with a readout card read each tick. HI07_SHOTS=a,b films only those.
 */
final class Hi07Scenes {
	private static final int W = 1440, H = 810;
	private static final int PAD = 60;
	private static final String SEED = System.getenv().getOrDefault("TAILINGS_TRAILER_SEED", "tailings");
	private static final String LV = "wildcatter:cable_industrial", MV = "wildcatter:cable_mv";
	/** The section west of the shaft: x -24 to 1 (x 0 and 1 only under the collar's 3 m lip), z -45 to 12. */
	private static final int SX0 = -24, SZ0 = -45, SZ1 = 12;
	/** The rail level's drive north of the section, to its face. */
	private static final int FACE_Z = -95;
	/** The raise borer's main cell: its raise x -2 to 2, z -65 to -61, its pilot down x 0, z -63. */
	private static final BlockPos BORER = new BlockPos(-2, 0, -66);
	private static final int RAISE_Z = -63;
	/** The vent fan at the raise's collar, its inlet north, its outlet south into the duct. */
	private static final BlockPos FAN = new BlockPos(-1, 0, -73);
	/** The track grizzly's north end (it runs 4 m south), the pocket's, the feeder's and the flask's main cells; y from the bottom. */
	private static final int GRIZ = -4;
	/** The cage winder's main cell, east of the hoist house: its drum x 13 to 16, z 22.5 (cage-winder.md). */
	private static final BlockPos CAGE_WINDER = new BlockPos(11, 0, 20);
	/** The gas sets' panels and the diesel set's. */
	private static final BlockPos GAS1 = new BlockPos(-40, 0, 24), GAS2 = new BlockPos(-40, 0, 40), DIESEL = new BlockPos(-14, 0, -86);

	private final ClientGameTestContext context;
	private TestServerContext server;
	/** The headframe's main cell: the collar at y 0, the shaft x and z 2 to 6. */
	private BlockPos c;
	/** The shaft's depth, collar to the flask's floor, m. */
	private int depth;
	private final List<String> summary = new ArrayList<>();
	private long filmStart;
	private int ft;
	private float lastYaw = Float.NaN;
	private int keeps;
	private UUID loco, villager, cage;
	private final List<UUID> cars = new ArrayList<>();
	private final List<BlockPos> mvBorer = new ArrayList<>();
	private volatile String mode = "";
	/** Where the camera follows (a skip, the cage, the train), read each tick on the server. */
	private volatile double[] follow;
	private volatile int followSkip = -1;
	private boolean ventOn;

	Hi07Scenes(ClientGameTestContext context) {
		this.context = context;
	}

	void run() {
		context.runOnClient(client -> {
			client.options.renderDistance().set(16);
			client.options.particles().set(ParticleStatus.ALL);
			client.options.entityDistanceScaling().set(5.0);
			client.options.fov().set(70);
			client.options.gamma().set(1.0);
			client.options.bobView().set(false);
			client.options.menuBackgroundBlurriness().set(0);
			client.options.chatVisibility().set(net.minecraft.world.entity.player.ChatVisiblity.HIDDEN);
			// a client with no input for ten minutes drops to 10 fps (FramerateLimitTracker LONG_AFK), and a client game test waits a
			// frame a tick: every film after that would draw at half speed (Rock to Metal episode 9). Throttle only when minimized.
			client.options.inactivityFpsLimit().set(net.minecraft.client.InactivityFpsLimit.MINIMIZED);
		});
		context.getInput().resizeWindow(W, H);
		context.waitTicks(10);
		guiWidth(720);
		try (TestSingleplayerContext world = context.worldBuilder().setUseConsistentSettings(false).adjustSettings(s -> {
			s.setSeed(SEED);
			s.setGameMode(net.minecraft.client.gui.screens.worldselection.WorldCreationUiState.SelectedGameMode.CREATIVE);
		}).create()) {
			server = world.getServer();
			context.waitTicks(40);
			server.runCommand("gamerule advance_time false");
			server.runCommand("gamerule advance_weather false");
			server.runCommand("gamerule do_mob_spawning false");
			server.runCommand("weather clear");
			server.runCommand("time set 6000");
			server.runCommand("difficulty peaceful");
			server.runCommand("gamemode spectator @p");
			server.runCommand("tp @p 0 160 0");
			context.waitTicks(20);
			hud(false);
			cardWidth("Air at the face: 23.0 m3/s (the drive needs 10.8)");
			try {
				stage();
				power();
				parts();
				pilotFilm();
				pilotDeepFilm();
				breakFilm();
				if (until("hi07_break")) {
					return;
				}
				reamFilm();
				finishRaise();
				fanFilm();
				faceFilm();
				trainFilm();
				flaskFilm();
				windFilm();
				if (until("hi07_wind")) {
					return;
				}
				dumpFilm();
				sheavesFilm();
				hoistFilm();
				scaleFilm();
				rideFilm();
				cageDrumFilm();
				cageSheaveFilm();
				callFilm();
				powerFilm();
				wideFilm();
			} catch (RuntimeException | AssertionError ex) {
				log("FAILED: " + ex);
				ex.printStackTrace();
				throw ex;
			} finally {
				server.runCommand("gamemode spectator @p");
				cardWidth("");
				context.runOnClient(client -> client.gui.setScreen(null));
				hud(true);
				log("summary:\n  " + String.join("\n  ", summary));
			}
		}
	}

	private static void log(String line) {
		System.out.println("[hi07] " + line);
	}

	private static void require(boolean ok, String what) {
		if (!ok) {
			throw new AssertionError("[hi07] " + what);
		}
	}

	/** HI07_UNTIL=id ends the run after that film (a re-take of the first films without the rest). */
	private static boolean until(String id) {
		return id.equals(System.getenv("HI07_UNTIL"));
	}

	private static boolean wanted(String id) {
		String only = System.getenv("HI07_SHOTS");
		return only == null || only.isBlank() || java.util.Arrays.asList(only.split(",")).contains(id);
	}

	private BlockPos d(int x, int y, int z) {
		return c.offset(x, y, z);
	}

	private BlockPos d(BlockPos p) {
		return c.offset(p);
	}

	/** The bottom: the flask's floor. */
	private int bot() {
		return -depth;
	}

	/** The rail's cell at the rail level (the train stands a metre over it). */
	private int railY() {
		return -depth + 4;
	}

	// ---- window, card, camera, films (as Hi06Scenes) ------------------------------------------------------------------------------

	private void hud(boolean shown) {
		context.runOnClient(client -> {
			if (client.gui.hud.isHidden() == shown) {
				client.gui.hud.toggle();
			}
		});
	}

	private void guiWidth(int units) {
		context.runOnClient(client -> {
			int scale = Math.max(1, Math.round(client.getWindow().getWidth() / (float) units));
			client.options.guiScale().set(scale);
			client.resizeGui();
		});
		context.waitTicks(2);
	}

	private void hideCursor() {
		context.runOnClient(client -> org.lwjgl.sdl.SDLMouse.SDL_HideCursor());
	}

	private static void card(String... lines) {
		Rtm01Scenes.Card.lines = lines;
	}

	private static void cardWidth(String text) {
		Rtm01Scenes.Card.widthOf = text;
	}

	private void showCard(boolean on) {
		card();
		context.runOnClient(client -> {
			if (on && !(client.gui.screen() instanceof Rtm01Scenes.Card)) {
				client.gui.setScreen(new Rtm01Scenes.Card());
			} else if (!on && client.gui.screen() instanceof Rtm01Scenes.Card) {
				client.gui.setScreen(null);
			}
		});
		context.waitTicks(2);
	}

	private void cam(Cam k, boolean cut) {
		double dx = k.tx() - k.x(), dy = k.ty() - k.y(), dz = k.tz() - k.z();
		float yaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
		float pitch = (float) -Math.toDegrees(Math.atan2(dy, Math.sqrt(dx * dx + dz * dz)));
		if (!cut && !Float.isNaN(lastYaw)) {
			yaw = lastYaw + Mth.wrapDegrees(yaw - lastYaw);
		}
		lastYaw = yaw;
		float y = yaw;
		if (cut) {
			server.runCommand(String.format(Locale.ROOT, "tp @p %.3f %.3f %.3f %.2f %.2f", k.x(), k.y() - 1.62, k.z(), yaw, pitch));
		}
		context.runOnClient(client -> {
			var p = client.player;
			if (p == null) {
				return;
			}
			p.setDeltaMovement(Vec3.ZERO);
			p.setPos(k.x(), k.y() - 1.62, k.z());
			p.setYRot(y);
			p.setYHeadRot(y);
			p.setXRot(pitch);
			if (cut) {
				p.setOldPosAndRot();
			}
		});
	}

	private void settle(int ticks) {
		context.waitTicks(ticks);
		try {
			context.waitFor(client -> client.levelRenderer.hasRenderedAllSections(), 600);
		} catch (RuntimeException | AssertionError ex) {
			log("sections still compiling: " + ex.getMessage());
		}
		context.waitTicks(5);
	}

	private void event(String what) {
		log(String.format(Locale.ROOT, "event %.1f s (clock %.1f s): %s", ft / 20.0, (System.nanoTime() - filmStart) / 1e9, what));
	}

	interface Beat {
		void at(ServerLevel level, int t);
	}

	private static double ease(double t) {
		t = Math.max(0, Math.min(1, t));
		return t * t * (3 - 2 * t);
	}

	private static IntFunction<Cam> move(Cam a, Cam b, int move) {
		return t -> a.lerp(b, ease((double) t / move));
	}

	private static IntFunction<Cam> orbitAround(BlockPos base, double cx, double cy, double cz, double r, double h, double from, double turn, int move) {
		return t -> {
			double a = Math.toRadians(from + turn * ease((double) t / move) * 0.15 + turn * ((double) t / move) * 0.85);
			return Cam.of(base, cx + Math.sin(a) * r, cy + h, cz + Math.cos(a) * r, cx, cy, cz);
		};
	}

	/**
	 * A camera at a fixed x and z in the section, at the height of what it follows plus {@code up}, looking at it from there: eased
	 * toward it (kept by film()'s beat, so no frame waits on the server).
	 */
	private IntFunction<Cam> riding(double x, double z, double up, double tx, double tz, double lookUp, double lo, double hi) {
		double[] at = {Double.NaN, Double.NaN, Double.NaN, 0};
		return t -> {
			double[] e = follow;
			double y = e == null ? c.getY() : e[1];
			// what it follows moves this far a tick (smoothed): led by that, the eased camera keeps up with it at speed rather than
			// trailing it (fix2-hi07: framed tighter, a lag of 4 m at 10 m/s would lose the skip)
			if (!Double.isNaN(at[2])) {
				at[3] += ((y - at[2]) - at[3]) * 0.3;
			}
			at[2] = y;
			double want = Math.max(c.getY() + lo, Math.min(c.getY() + hi, y + up + at[3] * (1 - 0.12) / 0.12)), look = y + lookUp + at[3] * (1 - 0.18) / 0.18;
			if (Double.isNaN(at[0])) {
				at[0] = want;
				at[1] = look;
			}
			at[0] += (want - at[0]) * 0.12;
			at[1] += (look - at[1]) * 0.18;
			return new Cam(c.getX() + x, at[0], c.getZ() + z, c.getX() + tx, at[1], c.getZ() + tz);
		};
	}

	/** The camera's field of view for the next film, degrees (70 unless set: fix2-hi07 zooms the break and frames the riding shots tighter). */
	private int fov = 70;

	private void fov(int degrees) {
		fov = degrees;
	}

	/** Underground films: the camera sees as with night vision (lit for the camera, said on screen). */
	private void dark(boolean on) {
		server.runCommand(on ? "effect give @p minecraft:night_vision infinite 0 true" : "effect clear @p minecraft:night_vision");
	}

	private void film(String id, int move, IntFunction<Cam> path, Beat beat) {
		boolean want = wanted(id);
		int view = fov;
		fov = 70;
		context.runOnClient(client -> client.options.fov().set(view));
		dark(id.equals("hi07_break") || id.equals("hi07_ream") || id.equals("hi07_face"));
		server.runCommand("kill @e[type=minecraft:item]");
		server.runOnServer(srv -> track(srv.overworld()));
		cam(path.apply(0), true);
		if (want) {
			settle(40);
			cam(path.apply(0), true);
			context.waitTicks(5);
		}
		hideCursor();
		Filming film = want ? context.computeOnClient(client -> Filming.start(client, id)) : null;
		filmStart = System.nanoTime();
		int total = PAD + move + PAD;
		for (ft = 0; ft <= total; ft++) {
			int t = Math.max(0, Math.min(move, ft - PAD));
			cam(path.apply(t), false);
			int tick = ft;
			server.runOnServer(srv -> {
				ServerLevel level = srv.overworld();
				if (beat != null) {
					beat.at(level, tick);
				}
				track(level);
				card(card(level));
			});
			if (ft % 10 == 0) {
				hideCursor();
			}
			if (ft % 20 == 0) {
				server.runOnServer(this::keep);
			}
			context.waitTick();
		}
		double clock = (System.nanoTime() - filmStart) / 1e9;
		if (film != null) {
			film.stop();
		}
		String line = String.format(Locale.ROOT, "%s: %.1f s by ticks, %.1f s by the clock%s", id, total / 20.0, clock, want ? "" : " (not filmed)");
		log(line);
		summary.add(line);
	}

	private void ticks(int n) {
		for (int t = 0; t < n; t++) {
			if (t % 20 == 0) {
				server.runOnServer(this::keep);
			}
			server.runOnServer(srv -> track(srv.overworld()));
			context.waitTick();
		}
	}

	private boolean waitFor(int most, java.util.function.Predicate<ServerLevel> done) {
		for (int t = 0; t < most; t += 5) {
			if (server.computeOnServer(srv -> done.test(srv.overworld()))) {
				return true;
			}
			ticks(5);
		}
		return false;
	}

	private static String f0(double v) {
		return String.format(Locale.ROOT, "%,.0f", v);
	}

	private static String f1(double v) {
		return String.format(Locale.ROOT, "%,.1f", v);
	}

	private static Block wc(String id) {
		return BuiltInRegistries.BLOCK.getValue(Identifier.parse(id));
	}

	/** A conductor or duct, joined to its neighbours as it is laid. */
	private static void lay(ServerLevel level, Block block, BlockPos cell) {
		if (level.getBlockState(cell).getBlock() == block) {
			return;
		}
		level.setBlock(cell, block.defaultBlockState(), Block.UPDATE_ALL);
		level.updateNeighborsAt(cell, block, null);
	}

	/** Cells from a to b along one axis at a time (x, then y, then z), both ends in. */
	private static List<BlockPos> path(BlockPos... points) {
		List<BlockPos> out = new ArrayList<>();
		out.add(points[0]);
		for (int i = 1; i < points.length; i++) {
			BlockPos at = out.get(out.size() - 1), to = points[i];
			while (at.getX() != to.getX()) {
				at = at.offset(Integer.signum(to.getX() - at.getX()), 0, 0);
				out.add(at);
			}
			while (at.getY() != to.getY()) {
				at = at.offset(0, Integer.signum(to.getY() - at.getY()), 0);
				out.add(at);
			}
			while (at.getZ() != to.getZ()) {
				at = at.offset(0, 0, Integer.signum(to.getZ() - at.getZ()));
				out.add(at);
			}
		}
		return out;
	}

	private void fill(ServerLevel level, Block block, int x0, int x1, int y0, int y1, int z0, int z1) {
		Trailer.fillBox(level, c, block, x0, x1, y0, y1, z0, z1);
	}

	private static BlockPos box(PlantBlockEntity part) {
		return part.getBlockPos().relative(dev.wildcatter.tailings.block.TailingsPartBlock.boxFace(part.getBlockState()));
	}

	private static void big(ServerLevel level, Block block, BlockPos main, Direction facing) {
		BlockState state = block.defaultBlockState().setValue(BigPartBlock.FACING, facing);
		level.setBlock(main, state, Block.UPDATE_ALL);
		BigParts.grow(level, main, level.getBlockState(main));
	}

	// ---- the site -----------------------------------------------------------------------------------------------------------------

	private void stage() {
		server.runOnServer(srv -> {
			ServerLevel level = srv.overworld();
			BlockPos s = Trailer.site(level, 400, -200, 70, 150);
			int g = s.getY();
			c = new BlockPos(s.getX() + 50, g, s.getZ() + 100);
			depth = Math.min(120, (g + 56) / 10 * 10);
			require(depth >= 80, "ground at " + g + " leaves only " + depth + " m of shaft");
			Trailer.pad(level, c.getX() - 52, c.getZ() - 102, c.getX() + 20, c.getZ() + 50, g, 14, 40);
			int b = bot();
			// solid rock round everything underground, so no cave, water or lava shows in the cut
			fill(level, Blocks.STONE, SX0 - 2, 9, b - 2, -2, SZ0 - 2, SZ1 + 2);
			fill(level, Blocks.STONE, -5, 4, b - 2, -2, FACE_Z - 2, SZ0 - 2);
			// the section: open to the sky west of the collar, and under the collar's 3 m lip at x 0 and 1, down to the bottom
			fill(level, Blocks.AIR, SX0, -1, b, 40, SZ0, SZ1);
			fill(level, Blocks.AIR, 0, 1, b, -4, SZ0, SZ1);
			// the shaft, 5 x 5 from the collar down to the flask's floor
			fill(level, Blocks.AIR, 2, 6, b, -1, 2, 6);
			// the rail level: a 2 m ledge of rock beside the shaft (the bed at y +3), the feeder, flask and pocket set in it
			fill(level, Blocks.STONE, 0, 1, b, b + 3, SZ0, SZ1);
			// the drive north of the section, 4 m wide and 5 m high over its bed, to the face
			fill(level, Blocks.AIR, -2, 1, b + 4, b + 8, FACE_Z, SZ0 - 1);
			fill(level, Blocks.STONE, -2, 1, b + 3, b + 3, FACE_Z, SZ0 - 1);
			// lit for the camera: lights up the raise's four corners every 3 m (the reamer leaves them: they cannot be broken); the fix
			// for episode 7 doubled them from two corners every 4 m, as the shots up the raise read dim
			for (int y = -4; y > b + 8; y -= 3) {
				for (int[] k : new int[][] {{2, 2}, {-2, 2}, {2, -2}, {-2, -2}}) {
					level.setBlock(d(k[0], y, RAISE_Z + k[1]), Blocks.LIGHT.defaultBlockState(), Block.UPDATE_ALL);
				}
			}
			// lit for the camera: lights in the drive's crown
			for (int z = FACE_Z; z < SZ0; z += 3) {
				level.setBlock(d(-2, b + 8, z), Blocks.LIGHT.defaultBlockState(), Block.UPDATE_ALL);
				level.setBlock(d(1, b + 8, z + 1), Blocks.LIGHT.defaultBlockState(), Block.UPDATE_ALL);
				level.setBlock(d(-2, b + 5, z + 2), Blocks.LIGHT.defaultBlockState(), Block.UPDATE_ALL);
			}
			// the track: along x 1 from the face, over the grizzly, past the shaft's west side to its end
			BlockState rail = TailingsBlocks.MINE_RAIL.defaultBlockState().setValue(MineRailBlock.FACING, Direction.NORTH);
			for (int z = FACE_Z; z <= 10; z++) {
				if (z >= GRIZ && z <= GRIZ + 3) {
					continue;
				}
				level.setBlock(d(1, railY(), z), rail, Block.UPDATE_ALL);
			}
			// spaces for the pocket, the feeder and the flask
			fill(level, Blocks.AIR, 1, 5, b, b + 3, GRIZ - 1, GRIZ + 4);
			fill(level, Blocks.AIR, 0, 1, b, b, 0, 3);
			fill(level, Blocks.AIR, 0, 1, b, b + 2, 4, 5);
			big(level, TailingsBlocks.TRACK_GRIZZLY, d(1, b + 3, GRIZ), Direction.NORTH);
			big(level, TailingsBlocks.DUMP_CAM_RAIL, d(2, b + 4, GRIZ), Direction.NORTH);
			big(level, TailingsBlocks.DUMP_POCKET, d(2, b, GRIZ), Direction.EAST);
			for (int k = 0; k < 4; k++) {
				level.setBlock(d(1, railY(), GRIZ + k), rail.setValue(MineRailBlock.BRIDGE, true), Block.UPDATE_ALL);
			}
			log("site: collar " + c.toShortString() + ", ground " + g + ", shaft " + depth + " m, bottom at y " + (c.getY() + b) + "; the grizzly finds its pocket: "
					+ (DumpBlockEntity.grizzlyUnder(level, d(1, railY(), GRIZ + 1)) != null));
		});
		context.waitTicks(40);
	}

	/** The shaft's parts, the borer, the call buttons. */
	private void parts() {
		server.runOnServer(srv -> {
			ServerLevel level = srv.overworld();
			int b = bot();
			PlantBlockEntity headframe = Trailer.plant(level, Machine.HEADFRAME, c);
			Trailer.plant(level, Machine.HEADFRAME_BIN, d(2, 0, -4));
			PlantBlockEntity hoist = Trailer.plant(level, Machine.HOIST_HOUSE, d(0, 0, 20));
			// the cage's own winder beside the hoist house (fix2-hi07, cage-winder.md), on the same medium-voltage run
			Trailer.plant(level, Machine.CAGE_WINDER, d(CAGE_WINDER));
			Trailer.plant(level, Machine.APRON_FEEDER, d(0, b, 0));
			Trailer.plant(level, Machine.MEASURING_FLASK, d(0, b, 4));
			ShaftLayout.Frame frame = ShaftLayout.Frame.of(headframe);
			require(ShaftSkipEntity.hang(level, frame) != null && ShaftSkipEntity.hang(level, frame) != null, "two skips hung");
			ShaftSkipEntity cg = ShaftSkipEntity.hangCage(level, frame);
			require(cg != null, "the cage hung");
			cage = cg.getUUID();
			// the landings' call buttons: a stone button on a post beside the headframe at the collar, and beside the flask at the bottom
			post(level, d(5, 0, 9), Direction.SOUTH);
			// at the bottom the ledge's own rock beside the flask is the post: the button on its face, south of the flask
			level.setBlock(d(0, b + 1, 6), Blocks.STONE_BRICKS.defaultBlockState(), Block.UPDATE_ALL);
			level.setBlock(d(-1, b + 1, 6), Blocks.STONE_BUTTON.defaultBlockState().setValue(BlockStateProperties.ATTACH_FACE, AttachFace.WALL)
					.setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.WEST), Block.UPDATE_ALL);
			log("headframe cell beside the collar post: " + level.getBlockState(d(5, 1, 8)).getBlock() + "; flask cell beside the bottom post: "
					+ level.getBlockState(d(0, b + 1, 5)).getBlock());
			log("hoist: " + hoist.status());
			log("cage winder: " + cageWinder(level).status());
			PlantBlockEntity borer = Trailer.plant(level, Machine.RAISE_BORER, d(BORER));
			log("raise borer: " + borer.status());
		});
		context.waitTicks(20);
		// the MV run to the borer, after it stands
		server.runOnServer(srv -> {
			ServerLevel level = srv.overworld();
			PlantBlockEntity borer = (PlantBlockEntity) level.getBlockEntity(d(BORER));
			BlockPos to = box(borer);
			// a branch off the hoist's run at x -30, z 20, alongside it on x -31; taken up again when the raise is done
			List<BlockPos> run = path(d(-31, 0, 20), d(-31, 0, -69), new BlockPos(to.getX(), c.getY(), c.getZ() - 69), to.offset(0, 0, 0));
			for (BlockPos p : run) {
				lay(level, wc(MV), p);
			}
			mvBorer.addAll(run);
			log("MV to the raise borer: " + run.size() + " m to " + to.toShortString());
		});
		ticks(100);
		server.runOnServer(srv -> {
			ServerLevel level = srv.overworld();
			log("hoist: " + hoist(level).status());
			log("raise borer: " + borer(level).status());
		});
	}

	/** A post of two stone bricks with a stone button on its {@code face} side, the post's other side against the part. */
	private static void post(ServerLevel level, BlockPos foot, Direction face) {
		level.setBlock(foot, Blocks.STONE_BRICKS.defaultBlockState(), Block.UPDATE_ALL);
		level.setBlock(foot.above(), Blocks.STONE_BRICKS.defaultBlockState(), Block.UPDATE_ALL);
		BlockState button = Blocks.STONE_BUTTON.defaultBlockState().setValue(BlockStateProperties.ATTACH_FACE, AttachFace.WALL)
				.setValue(BlockStateProperties.HORIZONTAL_FACING, face);
		level.setBlock(foot.above().relative(face), button, Block.UPDATE_ALL);
	}

	/** Presses a landing's call button as a hand would. */
	private static void press(ServerLevel level, BlockPos button) {
		BlockState s = level.getBlockState(button);
		require(s.getBlock() instanceof ButtonBlock, "a button at " + button.toShortString() + ": " + s);
		((ButtonBlock) s.getBlock()).press(s, level, button, null);
	}

	/** Two 1 MW gas sets stepped up to the medium voltage, joined, run to the hoist house; a diesel set for the industrial loads. */
	private void power() {
		server.runOnServer(srv -> {
			ServerLevel level = srv.overworld();
			List<BlockPos> outs = new ArrayList<>();
			for (BlockPos p : new BlockPos[] {GAS1, GAS2}) {
				BlockPos panel = d(p);
				fill(level, Blocks.SMOOTH_STONE, p.getX() - 8, p.getX() + 7, -1, -1, p.getZ() - 5, p.getZ() + 9);
				List<BlockPos> overwrote = GensetSkid.build(level, panel, Direction.NORTH, true);
				if (!overwrote.isEmpty()) {
					log("the set at " + panel.toShortString() + " overwrote " + overwrote.size() + " blocks");
				}
				BlockState sub = wc("wildcatter:unit_substation").defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.SOUTH);
				level.setBlock(panel.east(4), sub, Block.UPDATE_ALL);
				BigParts.grow(level, panel.east(4), sub);
				outs.add(panel.east(5));
			}
			// the medium voltage: from each substation east, joined along x -30, to the hoist house's box (x -1, z 20)
			List<BlockPos> mv = new ArrayList<>();
			mv.addAll(path(outs.get(1), d(-30, 0, GAS2.getZ()), d(-30, 0, GAS1.getZ())));
			mv.addAll(path(outs.get(0), d(-30, 0, GAS1.getZ()), d(-30, 0, 20), d(-1, 0, 20)));
			// a branch to the cage winder's box (x 10, z 20): off the run at x -2, round the south of the hoist house (fix2-hi07)
			mv.addAll(path(d(-2, 0, 21), d(-2, 0, 33), d(10, 0, 33), d(10, 0, 20)));
			for (BlockPos p : mv) {
				lay(level, wc(MV), p);
			}
			// the diesel set by the vent raise
			BlockPos dp = d(DIESEL);
			fill(level, Blocks.SMOOTH_STONE, DIESEL.getX() - 9, DIESEL.getX() + 1, -1, -1, DIESEL.getZ() - 5, DIESEL.getZ() + 9);
			GensetSkid.build(level, dp, Direction.NORTH, false);
			log("power: gas sets " + d(GAS1).toShortString() + ", " + d(GAS2).toShortString() + ", MV " + mv.size() + " m; diesel set " + dp.toShortString());
		});
		context.waitTicks(40);
		server.runOnServer(srv -> {
			ServerLevel level = srv.overworld();
			for (BlockPos p : new BlockPos[] {GAS1, GAS2, DIESEL}) {
				GensetControlPanelBlockEntity box = panel(level, p);
				box.setGasForTest(400);
				box.setWantRun(true);
			}
			fuel(level);
		});
		// the sets start and come up to speed off camera
		waitFor(1600, level -> panel(level, GAS1).online() && panel(level, GAS2).online() && panel(level, DIESEL).online());
		ticks(100);
		server.runOnServer(srv -> {
			for (BlockPos p : new BlockPos[] {GAS1, GAS2, DIESEL}) {
				log("set " + p.toShortString() + ": " + panel(srv.overworld(), p).inspect().getString());
			}
		});
	}

	private GensetControlPanelBlockEntity panel(ServerLevel level, BlockPos p) {
		return (GensetControlPanelBlockEntity) level.getBlockEntity(d(p));
	}

	private void fuel(ServerLevel level) {
		if (level.getBlockEntity(GensetSkid.base(d(DIESEL), Direction.NORTH, false)) instanceof GensetPartBlockEntity tank) {
			tank.setFuel(2000);   // fuel put in by the scene
		}
		for (BlockPos p : new BlockPos[] {GAS1, GAS2}) {
			if (level.getBlockEntity(d(p)) instanceof GensetControlPanelBlockEntity b) {
				b.setGasForTest(400);   // gas put in by the scene
			}
		}
	}

	private double gasKw(ServerLevel level) {
		return (panel(level, GAS1).watts() + panel(level, GAS2).watts()) / 1000;
	}

	private PlantBlockEntity hoist(ServerLevel level) {
		return level.getBlockEntity(d(0, 0, 20)) instanceof PlantBlockEntity p ? p : null;
	}

	private PlantBlockEntity cageWinder(ServerLevel level) {
		return level.getBlockEntity(d(CAGE_WINDER)) instanceof PlantBlockEntity p ? p : null;
	}

	private PlantBlockEntity borer(ServerLevel level) {
		return level.getBlockEntity(d(BORER)) instanceof PlantBlockEntity p ? p : null;
	}

	private PlantBlockEntity flask(ServerLevel level) {
		return (PlantBlockEntity) level.getBlockEntity(d(0, bot(), 4));
	}

	private PlantBlockEntity feeder(ServerLevel level) {
		return (PlantBlockEntity) level.getBlockEntity(d(0, bot(), 0));
	}

	private PlantBlockEntity bin(ServerLevel level) {
		return level.getBlockEntity(d(2, 0, -4)) instanceof PlantBlockEntity p ? p : null;
	}

	private PlantBlockEntity fan(ServerLevel level) {
		return level.getBlockEntity(d(FAN)) instanceof PlantBlockEntity p ? p : null;
	}

	private DumpBlockEntity pocket(ServerLevel level) {
		return (DumpBlockEntity) level.getBlockEntity(d(2, bot(), GRIZ));
	}

	private ShaftSkipEntity skip(ServerLevel level, int which) {
		for (ShaftSkipEntity s : ShaftSkipEntity.near(level, ShaftLayout.Frame.of((PlantBlockEntity) level.getBlockEntity(c)))) {
			if (!s.cage() && s.compartment() == which) {
				return s;
			}
		}
		return null;
	}

	private ShaftSkipEntity cage(ServerLevel level) {
		return cage != null && level.getEntity(cage) instanceof ShaftSkipEntity s ? s : null;
	}

	private MineVehicleEntity loco(ServerLevel level) {
		return loco != null && level.getEntity(loco) instanceof MineVehicleEntity m ? m : null;
	}

	/** Each tick: what the camera follows, and the villager in the cage or at the collar. */
	private void track(ServerLevel level) {
		if (c == null) {
			return;
		}
		if (followSkip >= 0) {
			ShaftSkipEntity s = followSkip == 2 ? cage(level) : skip(level, followSkip);
			if (s != null) {
				follow = new double[] {s.getX(), s.getY(), s.getZ()};
			}
		} else if (mode.equals("borer") && borer(level) != null && borer(level).raise().phase() == RaiseBoring.Phase.PILOT) {
			follow = new double[] {0, -borer(level).raise().pilot(), 0};
		} else if (mode.equals("train") && loco(level) != null) {
			MineVehicleEntity l = loco(level);
			follow = new double[] {l.getX(), l.getY(), l.getZ()};
		}
	}

	private String[] card(ServerLevel level) {
		List<String> out = new ArrayList<>();
		switch (mode) {
			case "borer" -> {
				PlantBlockEntity b = borer(level);
				out.add("^RAISE BORER · 400 kW");
				if (b != null) {
					String st = b.status();
					out.add((st.startsWith("NO ") ? "!" : "+") + cut(st, 46));
					out.add("Rate here: " + (b.fast() > 1 ? b.fast() + "x the game's " : "") + "pilot 1 m in 10 s, ream 1 m in 60 s");
				}
				out.add("Two gas sets: " + f0(gasKw(level)) + " kW of 2,000 kW");
			}
			case "fan", "face" -> {
				PlantBlockEntity f = fan(level);
				out.add("^VENT FAN · 75 kW");
				if (f != null) {
					out.add((f.running() ? "+RUNNING" : "!STOPPED"));
					VentState v = f.vent();
					if (v != null && v.air() != null) {
						out.add("Fan: " + f1(v.air().fanFlow()) + " m3/s at " + f0(v.air().pressurePa()) + " Pa, " + f0(v.air().metres()) + " m of duct");
					}
				}
				Airflow.Result air = VentNetwork.airAt(level, d(1, railY() + 1, FACE_Z + 4)).run() == null ? null : VentNetwork.airAt(level, d(1, railY() + 1, FACE_Z + 4)).run().air();
				out.add((air != null && air.endFlow() >= Airflow.NEED ? "+" : "!") + "Air at the face: " + (air == null ? "none" : f1(air.endFlow()) + " m3/s") + " (the drive needs " + f1(Airflow.NEED) + ")");
				if (mode.equals("face")) {
					out.add(cut(VentNetwork.detector(level, d(1, railY() + 1, FACE_Z + 4)).getString(), 50));
				} else {
					out.add("Diesel set: " + f0(panel(level, DIESEL).watts() / 1000) + " kW of 500 kW");
				}
			}
			case "train" -> {
				MineVehicleEntity l = loco(level);
				out.add("^SHAFT BOTTOM · RAIL LEVEL");
				if (l != null) {
					String st = l.status();
					int k = st.indexOf("; battery");
					out.add("+Train: " + (k > 0 ? st.substring(0, k) : st).toUpperCase(Locale.ROOT));
				}
				out.add("Pocket under the grizzly: " + f0(pocket(level).pocket() + pocket(level).oversize()) + " items");
				out.add("Feeder: " + cut(feeder(level).status(), 40));
				out.add("Measuring flask: " + f1(flask(level).hold().total() / 1000) + " of 3 t");
			}
			case "flask" -> {
				out.add("^MEASURING FLASK · 3 t");
				out.add("Flask: " + f1(flask(level).hold().total() / 1000) + " of 3 t");
				out.add("Feeder: " + cut(feeder(level).status(), 40));
				out.add(hoistLine(level));
			}
			case "hoist", "wind" -> {
				out.add("^HOIST · 450 kW · 2 SKIPS OF 3 t");
				out.add(hoistLine(level));
				Winder w = hoist(level).winder();
				out.add("Rope: " + f1(Math.abs(w.speed())) + " m/s of 10; " + f0(w.travel()) + " m a wind");
				out.add("Two gas sets: " + f0(gasKw(level)) + " kW of 2,000 kW");
			}
			case "dump" -> {
				out.add("^HEADFRAME · 30 m");
				out.add(hoistLine(level));
				out.add("Headframe bin: " + f1(bin(level).hold().total() / 1000) + " of 100 t");
			}
			case "cage" -> {
				out.add("^CAGE WINDER · 160 kW · 2.5 m DRUM");
				PlantBlockEntity cw = cageWinder(level);
				String st = cw.status();
				out.add((st.startsWith("NOT") || st.startsWith("STOPPED") ? "!" : "+") + cut(st, 50).toUpperCase(Locale.ROOT));
				dev.wildcatter.tailings.shaft.CageWinder w = cw.cageWinder();
				out.add("Rope: " + f1(w.speed()) + " m/s of 4; drum " + f1(w.drumDegrees() / 360) + " turns out");
				ShaftSkipEntity cg = cage(level);
				out.add("Riders: " + (cg == null ? 0 : cg.getPassengers().size()) + " · Skips: " + cut(hoist(level).status(), 30));
			}
			case "power" -> {
				out.add("^POWER");
				out.add("Two gas sets: " + f0(gasKw(level)) + " kW of 2,000 kW");
				out.add("Diesel set: " + f0(panel(level, DIESEL).watts() / 1000) + " kW of 500 kW");
			}
			default -> {
				return new String[0];
			}
		}
		return out.toArray(String[]::new);
	}

	private String hoistLine(ServerLevel level) {
		String st = hoist(level).status();
		boolean bad = st.startsWith("NOT") || st.startsWith("STOPPED") || st.startsWith("WAITING") || st.startsWith("BIN");
		return (bad ? "!" : "+") + cut(st, 50).toUpperCase(Locale.ROOT);
	}

	private static String cut(String s, int n) {
		return s.length() <= n ? s : s.substring(0, n - 1) + "…";
	}

	private void keep(MinecraftServer srv) {
		ServerLevel level = srv.overworld();
		fuel(level);
		// the headframe bin drawn down by the scene (no belt under it here)
		PlantBlockEntity bin = bin(level);
		if (bin != null && bin.hold().total() > 60_000) {
			bin.hold().take(bin.hold().total() - 6_000);
			bin.setChanged();
		}
		if (++keeps % 15 == 0) {
			log(String.format(Locale.ROOT, "tick %.1f ms; hoist %s; gas %.0f kW", srv.getAverageTickTimeNanos() / 1e6, hoist(level) == null ? "-" : hoist(level).status(), gasKw(level)));
		}
	}

	// ---- the raise ------------------------------------------------------------------------------------------------------------------

	/** hi07_pilot: the raise borer on its pad, drilling its pilot down at the game's rate: a metre each 10 s, the cuttings at the collar. */
	private void pilotFilm() {
		mode = "borer";
		showCard(true);
		require(waitFor(600, level -> borer(level).raise().phase() == RaiseBoring.Phase.PILOT && borer(level).running()), "the borer drilling: "
				+ server.computeOnServer(srv -> borer(srv.overworld()).status()));
		// the rock cut away east of the hole for the camera (heavy-iron ep 7 fix: the rod could not be seen going in): a trench 3 m wide
		// and 9 m deep from the pad's east edge, open to the pilot's column, so the string and the bit show going down; put back after
		java.util.Map<BlockPos, BlockState> cut = new java.util.LinkedHashMap<>();
		server.runOnServer(srv -> {
			ServerLevel level = srv.overworld();
			for (int x = 0; x <= 5; x++) {
				for (int z = RAISE_Z - 1; z <= RAISE_Z + 1; z++) {
					if (x == 0 && z != RAISE_Z) {
						continue;
					}
					// the pad stays under the baseplate (x 1 and west at y -1); the trench is open to the sky east of it
					for (int y = x <= 1 ? -2 : -1; y >= -9; y--) {
						BlockPos p = d(x, y, z);
						cut.put(p, level.getBlockState(p));
						level.setBlock(p, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
					}
				}
			}
			log("rock cut away beside the pilot: " + cut.size() + " cells; " + borer(level).status());
		});
		// from the east over the trench, wide on the rig and down into the cut: the drill string and its bit going down
		Cam a = Cam.of(c, 10.0, 6.0, -57.0, 0.5, -0.5, -63.0);
		Cam b = Cam.of(c, 6.6, 0.8, -61.2, 0.5, -3.2, -63.0);
		film("hi07_pilot", 700, move(a, b, 560), null);
		server.runOnServer(srv -> {
			ServerLevel level = srv.overworld();
			cut.forEach((p, st) -> level.setBlock(p, st, Block.UPDATE_ALL));
			log("the rock put back beside the pilot");
		});
	}

	/**
	 * hi07_pilotdeep (fix2-hi07: the cut showed only the first 4 m of the 112 m pilot): the rock cut away east of the pilot's column from
	 * the pad down 92 m, 9 m by 5 m and open to the sky, and the camera going down the cut 6.5 m from the string with the bit, at 20 times
	 * the game's rate (a metre in 0.5 s) to 86 m, then the game's rate again so the breakthrough is left for the drive's film; the rock
	 * put back after the film.
	 */
	private void pilotDeepFilm() {
		mode = "borer";
		showCard(true);
		int from = server.computeOnServer(srv -> borer(srv.overworld()).raise().pilot());
		java.util.Map<BlockPos, BlockState> cut = new java.util.LinkedHashMap<>();
		server.runOnServer(srv -> {
			ServerLevel level = srv.overworld();
			for (int x = 0; x <= 8; x++) {
				for (int z = RAISE_Z - 2; z <= RAISE_Z + 2; z++) {
					if (x == 0 && z != RAISE_Z) {
						continue;
					}
					for (int y = x <= 1 ? -2 : -1; y >= -DEEP_CUT; y--) {
						BlockPos p = d(x, y, z);
						cut.put(p, level.getBlockState(p));
						level.setBlock(p, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
					}
				}
			}
			log("rock cut away beside the pilot down " + DEEP_CUT + " m: " + cut.size() + " cells; pilot at " + borer(level).raise().pilot() + " m");
		});
		ticks(20);
		server.runOnServer(srv -> borer(srv.overworld()).fastCycle(20));
		// the camera in the cut, 6.5 m east of the string and a little south, a little over the bit, led down by the bit's depth; the
		// drawn string runs about a second behind the server's metre count, so the camera aims 1.5 m over the server's bit
		double[] at = {Double.NaN, 0, Double.NaN};
		int move = 900;
		film("hi07_pilotdeep", move, t -> {
			double[] e = follow;
			double bit = e == null ? -from : e[1];
			if (!Double.isNaN(at[2])) {
				at[1] += ((bit - at[2]) - at[1]) * 0.3;
			}
			at[2] = bit;
			double want = bit + 3.2 + at[1] * (1 - 0.15) / 0.15;
			at[0] = Double.isNaN(at[0]) ? want : at[0] + (want - at[0]) * 0.15;
			double y = Math.max(-DEEP_CUT + 4, Math.min(1.5, at[0]));
			return Cam.of(c, 7.0, y, RAISE_Z + 1.8, 0.5, y - 1.7, RAISE_Z + 0.5);
		}, (level, t) -> {
			if (t % 100 == 0) {
				event(borer(level).status());
			}
			if (borer(level).fast() > 1 && borer(level).raise().pilot() >= DEEP_CUT - 6) {
				borer(level).fastCycle(1);
				event("back to the game's rate: " + borer(level).status());
			}
		});
		server.runOnServer(srv -> {
			ServerLevel level = srv.overworld();
			borer(level).fastCycle(1);
			cut.forEach((p, st) -> level.setBlock(p, st, Block.UPDATE_ALL));
			log("the rock put back beside the pilot; pilot at " + borer(level).raise().pilot() + " m");
		});
	}

	/** How deep the second pilot film's cut goes below the pad, m. */
	private static final int DEEP_CUT = 92;

	/** hi07_break: from the drive under it, the pilot bit breaking through its roof (its last 2 m at the game's rate). */
	private void breakFilm() {
		int k = server.computeOnServer(srv -> borer(srv.overworld()).raise().drive());
		// off camera, the pilot run on fast to 2 m over the drive's roof (at 100x to 12 m over it, then 10x)
		server.runOnServer(srv -> borer(srv.overworld()).fastCycle(100));
		require(waitFor(2400, level -> borer(level).raise().pilot() >= k - 12), "the pilot down to 12 m over the drive");
		server.runOnServer(srv -> borer(srv.overworld()).fastCycle(10));
		require(waitFor(2400, level -> borer(level).raise().pilot() >= k - 2), "the pilot down to 2 m over the drive");
		server.runOnServer(srv -> {
			borer(srv.overworld()).fastCycle(1);
			log("pilot " + borer(srv.overworld()).raise().pilot() + " of " + k + " m, into the drive's roof next");
		});
		int b = bot();
		// closer under the spot it breaks through (heavy-iron ep 7 fix: the bit was a dot far down the drive), and zoomed in (fix2-hi07:
		// the 300 mm bit still read small in the 4 m drive): a 30 degree view from 4.5 m frames 2.4 m of roof round it
		Cam a = Cam.of(c, -0.9, b + 5.5, RAISE_Z + 3.4, 0.5, b + 9.0, RAISE_Z + 0.5);
		Cam e = Cam.of(c, -0.7, b + 5.7, RAISE_Z + 3.0, 0.5, b + 8.9, RAISE_Z + 0.5);
		fov(30);
		boolean[] flag = {false};
		film("hi07_break", 560, move(a, e, 560), (level, t) -> {
			if (borer(level).raise().phase() == RaiseBoring.Phase.REAM && !flag[0]) {
				flag[0] = true;
				event("broke through: the reamer on");
			}
		});
	}

	/** hi07_ream: the reamer cutting the 5 m raise back up, shown at 20 times the game's rate, from the drive under it. */
	private void reamFilm() {
		server.runOnServer(srv -> borer(srv.overworld()).fastCycle(20));
		int b = bot();
		mode = "borer";
		// under the raise, looking up it: the reamer's underside rising, the cuttings falling past
		Cam a = Cam.of(c, 0.0, b + 4.8, RAISE_Z + 3.4, 0.5, b + 24, RAISE_Z + 0.0);
		Cam e = Cam.of(c, 0.2, b + 5.2, RAISE_Z + 2.8, 0.5, b + 34, RAISE_Z + 0.2);
		film("hi07_ream", 800, move(a, e, 800), (level, t) -> {
			if (t % 200 == 0) {
				event(borer(level).status());
			}
		});
	}

	/**
	 * Off camera: the rest of the ream at speed, its cuttings taken away; the borer lifted off and the collar slab taken out; the fan
	 * set at the collar, its duct down the raise and along the drive's crown to the face; the diesel set's cable to the fan, and down
	 * the raise and along the drive to the feeder at the shaft bottom.
	 */
	private void finishRaise() {
		mode = "";
		showCard(false);
		server.runOnServer(srv -> borer(srv.overworld()).fastCycle(1200));
		for (int i = 0; i < 40; i++) {
			if (server.computeOnServer(srv -> borer(srv.overworld()).raise().phase() == RaiseBoring.Phase.DONE)) {
				break;
			}
			ticks(20);
			server.runCommand("kill @e[type=minecraft:item]");
		}
		server.runOnServer(srv -> {
			ServerLevel level = srv.overworld();
			PlantBlockEntity br = borer(level);
			log("raise: " + br.status());
			require(br.raise().phase() == RaiseBoring.Phase.DONE, "the raise reamed");
			for (BlockPos p : BigParts.cells(br.getBlockPos(), br.getBlockState())) {
				level.setBlock(p, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
			}
			for (BlockPos p : mvBorer) {
				level.setBlock(p, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
			}
			fill(level, Blocks.AIR, -2, 2, -1, -1, RAISE_Z - 2, RAISE_Z + 2);
			int b = bot();
			PlantBlockEntity f = Trailer.plant(level, Machine.VENT_FAN, d(FAN));
			// the duct: out of the fan's south face, over the raise, down it, and along the drive's crown to 3 m from the face
			Block duct = TailingsBlocks.VENT_DUCT;
			for (BlockPos p : path(d(0, 1, FAN.getZ() + 5), d(0, 1, RAISE_Z), d(0, b + 8, RAISE_Z), d(0, b + 8, FACE_Z + 3))) {
				lay(level, duct, p);
			}
			// the cable: the set to the fan's box, and on down the raise's west corner and the drive to the feeder's box
			Block lv = wc(LV);
			BlockPos fanBox = box(f), feedBox = box(feeder(level));
			for (BlockPos p : path(d(DIESEL.getX() + 1, 0, DIESEL.getZ()), d(-4, 0, DIESEL.getZ()), d(-4, 0, fanBox.getZ() - c.getZ()), fanBox)) {
				lay(level, lv, p);
			}
			for (BlockPos p : path(d(-4, 0, fanBox.getZ() - c.getZ() + 1), d(-4, 0, RAISE_Z - 2), d(-2, 0, RAISE_Z - 2), d(-2, b + 4, RAISE_Z - 2), d(-2, b + 4, SZ0),
					d(-2, b, SZ0), d(-2, b, 0), feedBox)) {
				lay(level, lv, p);
			}
			ventOn = true;
			log("fan " + f.status() + "; feeder " + feeder(level).status());
		});
		server.runCommand("kill @e[type=minecraft:item]");
		ticks(200);
		server.runOnServer(srv -> {
			ServerLevel level = srv.overworld();
			log("fan: " + fan(level).status() + "; air at the face " + VentNetwork.detector(level, d(1, railY() + 1, FACE_Z + 4)).getString());
		});
	}

	// ---- ventilation ----------------------------------------------------------------------------------------------------------------

	/** hi07_fan: the vent fan at the raise's collar, its impeller turning behind the inlet screen, the duct going down the raise. */
	private void fanFilm() {
		mode = "fan";
		showCard(true);
		Cam a = Cam.of(c, -5.5, 3.2, FAN.getZ() - 8.5, 0.5, 1.5, FAN.getZ());
		Cam b = Cam.of(c, 6.5, 5.0, RAISE_Z - 2.5, 0.0, -2.0, RAISE_Z);
		film("hi07_fan", 800, move(a, b, 800), null);
	}

	/** hi07_face: in the drive, the duct along the crown to the face, the air read at the face. */
	private void faceFilm() {
		mode = "face";
		int b = bot();
		Cam a = Cam.of(c, -1.3, b + 6.6, FACE_Z + 22, 0.0, b + 6.5, FACE_Z);
		Cam e = Cam.of(c, -1.0, b + 6.4, FACE_Z + 10, 0.2, b + 6.0, FACE_Z);
		film("hi07_face", 600, move(a, e, 600), null);
	}

	// ---- the shaft bottom -----------------------------------------------------------------------------------------------------------

	/** hi07_train: a loaded train of three bottom-dump cars runs over the grizzly at the shaft bottom and drops its loads on the move. */
	private void trainFilm() {
		server.runOnServer(srv -> {
			ServerLevel level = srv.overworld();
			int z = -26;
			MineVehicleEntity l = MineVehicleEntity.placeLocomotive(level, d(1, railY(), z), Heading.SOUTH, TailingsEntities.BATTERY_LOCOMOTIVE);
			require(l != null, "the battery locomotive on the rail");
			l.setBatteryWh(Battery.USABLE_WH * 0.9);
			l.setRunning(false);
			loco = l.getUUID();
			String[] loads = {"minecraft:copper_ore", "minecraft:cobblestone", "minecraft:copper_ore"};
			for (int i = 0; i < 3; i++) {
				int cz = (int) Math.floor(l.getZ() - Haul.carOffset(i));
				MineVehicleEntity car = MineVehicleEntity.placeCar(level, d(1, railY(), cz - c.getZ()), Heading.SOUTH, TailingsEntities.BOTTOM_DUMP_CAR);
				require(car != null, "car " + i);
				car.load().add(loads[i], Haul.CAR_ITEMS);   // loaded by the scene
				car.refreshLoad();
				cars.add(car.getUUID());
			}
			log("train: " + l.carIds().size() + " cars coupled at " + l.position());
		});
		ticks(20);
		mode = "train";
		int b = bot();
		Cam a = Cam.of(c, -7.0, b + 8.5, GRIZ + 9, 1.0, b + 4.5, GRIZ - 2);
		Cam e = Cam.of(c, -6.0, b + 7.5, GRIZ + 7, 1.0, b + 4.0, GRIZ + 1);
		film("hi07_train", 760, move(a, e, 760), (level, t) -> {
			MineVehicleEntity l = loco(level);
			if (t == PAD) {
				l.setRunning(true);
				event("run toward the grizzly");
			}
			for (UUID id : cars) {
				if (level.getEntity(id) instanceof MineVehicleEntity car && car.doorsOpen() && car.addTag("opened")) {
					event(String.format(Locale.ROOT, "doors open over the grizzly at %.1f m/s", l.speed()));
				}
			}
			if (l.status().startsWith("at the end")) {
				l.setRunning(false);
			}
		});
	}

	/** hi07_flask: the feeder fills the flask to 3 t; the skip at the bottom takes it and the hoist winds. */
	private void flaskFilm() {
		mode = "flask";
		int b = bot();
		// close on the flask's west face, its sight window filling (heavy-iron ep 7 fix: it showed only on the card), the feeder over it
		Cam a = Cam.of(c, -4.6, b + 3.4, 8.6, 0.2, b + 2.0, 3.6);
		Cam e = Cam.of(c, -3.4, b + 2.8, 7.4, 0.2, b + 1.9, 4.2);
		boolean[] flag = {false};
		film("hi07_flask", 900, move(a, e, 900), (level, t) -> {
			Winder w = hoist(level).winder();
			if (w.winding() && !flag[0]) {
				flag[0] = true;
				event("winding: " + hoist(level).status());
			}
		});
	}

	// ---- the hoist ------------------------------------------------------------------------------------------------------------------

	/** Waits off camera for the hoist to be resting with a loaded skip at the bottom, about to wind; returns which skip it is. */
	private int waitForRest() {
		require(waitFor(2400, level -> {
			Winder w = hoist(level).winder();
			return !w.winding() && w.rest() > 3 && !w.load(w.bottom()).isEmpty();
		}) || waitFor(2400, level -> !hoist(level).winder().winding()), "the hoist resting with a loaded skip at the bottom: "
				+ server.computeOnServer(srv -> hoist(srv.overworld()).status()));
		return server.computeOnServer(srv -> hoist(srv.overworld()).winder().bottom());
	}

	/** hi07_wind: a whole wind in real time, the camera riding with the loaded skip from the bottom up into the headframe. */
	private void windFilm() {
		mode = "wind";
		// the west skip at the bottom, the nearer to the section (fix2-hi07: framed tighter, 4 m from it, the camera led by its speed)
		require(waitFor(4800, level -> {
			Winder w = hoist(level).winder();
			return !w.winding() && w.position() < 1e-3 && w.rest() > 3;
		}), "the west skip resting at the bottom: " + server.computeOnServer(srv -> hoist(srv.overworld()).status()));
		followSkip = 0;
		server.runOnServer(srv -> track(srv.overworld()));
		double z = followSkip == 0 ? 3.5 : 3.5;
		double sx = followSkip == 0 ? 2.5 : 5.5;
		long[] wound = {-1};
		// nearer the skip than the first take's 11 m out in the section (heavy-iron ep 7 fix: the skip read small on the grey wall)
		// 5 m out in the section, south of the skip; kept 5 m over the bottom so it clears the ledge, the flask and the button's post
		fov(55);
		film("hi07_wind", 1000, riding(-1.5, z + 3.1, 1.8, sx + 0.5, z - 0.5, 1.9, bot() + 5, 22), (level, t) -> {
			Winder w = hoist(level).winder();
			if (w.winding() && wound[0] < 0) {
				wound[0] = t;
				event("winding");
			}
			if (!w.winding() && wound[0] >= 0 && wound[0] < 1_000_000) {
				event(String.format(Locale.ROOT, "at the dump after %.2f s", (t - wound[0]) / 20.0));
				wound[0] = 1_000_000;
			}
		});
		followSkip = -1;
	}

	/** hi07_dump: the loaded skip coming up into the headframe and dumping into the bin: its door open, the rock pouring. */
	private void dumpFilm() {
		mode = "dump";
		int up = waitForRest();
		require(waitFor(1200, level -> hoist(level).winder().winding()), "winding");
		require(waitFor(1200, level -> hoist(level).winder().position() > 0.55), "the skip near the top");
		double sx = up == 0 ? 2.5 : 5.5;
		Cam a = Cam.of(c, -9.0, 15.0, -6.0, sx, 16.0, 1.0);
		Cam e = Cam.of(c, -7.5, 14.0, -5.0, sx, 14.0, 0.5);
		film("hi07_dump", 640, move(a, e, 640), (level, t) -> {
			for (int k = 0; k < 2; k++) {
				ShaftSkipEntity s = skip(level, k);
				if (s != null && s.pour() > 0.99 && s.addTag("pour")) {
					event("the door opens: pouring");
				}
			}
		});
	}

	/** hi07_sheaves: the two head sheaves 27.5 m up, turning as the ropes run over them through a wind. */
	private void sheavesFilm() {
		mode = "";
		showCard(false);
		waitForRest();
		Cam a = Cam.of(c, -1.5, 29.6, 10.5, 4.5, 27.3, 5.5);
		Cam e = Cam.of(c, 0.5, 28.6, 9.0, 4.5, 27.5, 5.5);
		film("hi07_sheaves", 700, move(a, e, 700), null);
	}

	/** hi07_hoist: the hoist house's two drums through its open north wall, turning on one shaft as the ropes wind on and off. */
	private void hoistFilm() {
		mode = "hoist";
		showCard(true);
		waitForRest();
		Cam a = Cam.of(c, 8.5, 6.0, 12.0, 4.5, 3.4, 23.0);
		Cam e = Cam.of(c, 6.0, 5.2, 14.5, 4.5, 3.4, 23.0);
		film("hi07_hoist", 800, move(a, e, 800), null);
	}

	// ---- scale and the cage ---------------------------------------------------------------------------------------------------------

	/** hi07_scale: a villager at the collar beside the call button, the camera rising from it up the 30 m headframe. */
	private void scaleFilm() {
		mode = "";
		showCard(false);
		server.runOnServer(srv -> {
			ServerLevel level = srv.overworld();
			var v = net.minecraft.world.entity.EntityTypes.VILLAGER.create(level, EntitySpawnReason.COMMAND);
			require(v != null, "a villager");
			v.setNoAi(true);
			v.setSilent(true);
			v.addTag("hi07v");
			v.setPos(c.getX() + 3.6, c.getY(), c.getZ() + 10.4);
			v.setYRot(180);
			v.setYHeadRot(180);
			v.setYBodyRot(180);
			level.addFreshEntity(v);
			villager = v.getUUID();
		});
		Cam a = Cam.of(c, 9.0, 1.4, 18.5, 3.6, 1.2, 10.4);
		Cam e = Cam.of(c, 13.0, 2.5, 16.5, 4.5, 26.0, 4.5);
		film("hi07_scale", 900, move(a, e, 900), null);
	}

	/**
	 * hi07_ride: the villager rides the cage down the shaft, called from the bottom landing's button; the camera rides with it, 4.6 m out
	 * in the section (fix2-hi07: framed tighter, led by its speed), the cage right of centre. The skips go on winding: the cage has its
	 * own winder.
	 */
	private void rideFilm() {
		server.runOnServer(srv -> {
			ServerLevel level = srv.overworld();
			ShaftSkipEntity cg = cage(level);
			Entity v = level.getEntity(villager);
			v.setPos(cg.getX() + 1, cg.getY(), cg.getZ() + 1);
			v.startRiding(cg);
			log("villager in the cage: " + cg.getPassengers().size() + " riders; cage at " + cg.position() + "; " + cageWinder(level).status());
		});
		ticks(10);
		mode = "cage";
		showCard(true);
		followSkip = 2;
		server.runOnServer(srv -> track(srv.overworld()));
		double[] at = cageCentre();
		log(String.format(Locale.ROOT, "the cage's centre at x %.1f, z %.1f from the headframe", at[0], at[1]));
		fov(62);
		// aimed 2.4 m north of it so it sits right of centre; kept 5 m over the bottom, clear of the ledge, the flask and the post
		film("hi07_ride", 1000, riding(at[0] - 4.6, at[1] + 1.3, 1.5, at[0], at[1] - 2.4, 1.4, bot() + 5, 12), (level, t) -> {
			if (t == PAD + 10) {
				press(level, d(-1, bot() + 1, 6));
				event("the bottom landing's button pressed: " + cageWinder(level).status() + "; skips: " + hoist(level).status());
			}
			cageEvents(level, t);
		});
		followSkip = -1;
	}

	/** The cage's centre across the shaft, from the headframe's main cell. */
	private double[] cageCentre() {
		return server.computeOnServer(srv -> new double[] {cage(srv.overworld()).getX() + 1 - c.getX(), cage(srv.overworld()).getZ() + 1 - c.getZ()});
	}

	private boolean cageMoving;

	/** Logs when the cage starts and stops, and its drum's turns. */
	private void cageEvents(ServerLevel level, int t) {
		dev.wildcatter.tailings.shaft.CageWinder w = cageWinder(level).cageWinder();
		boolean moving = w.speed() > 0;
		if (moving != cageMoving) {
			cageMoving = moving;
			event((moving ? "the cage moving" : "the cage stopped") + String.format(Locale.ROOT, ": %.1f m below the collar, the drum %.2f turns out", w.rope(), w.drumDegrees() / 360));
		}
	}

	/**
	 * hi07_cagedrum (fix2-hi07): the cage winder beside the hoist house, its 2.5 m drum turning through the open north side as the cage is
	 * called up from the bottom with its rider; the camera draws back and up along the rope toward the headframe.
	 */
	private void cageDrumFilm() {
		mode = "cage";
		showCard(true);
		waitFor(600, level -> cageWinder(level).cageWinder().atLanding());
		Cam a = Cam.of(c, 14.6, 3.3, 16.8, 14.5, 2.3, 22.5);
		Cam e = Cam.of(c, 19.0, 7.5, 12.5, 9.5, 10.5, 16.0);
		film("hi07_cagedrum", 900, move(a, e, 900), (level, t) -> {
			if (t == PAD + 10) {
				press(level, d(5, 1, 10));
				event("the collar's button pressed: " + cageWinder(level).status());
			}
			cageEvents(level, t);
		});
	}

	/** hi07_cagesheave (fix2-hi07): the cage sheave in the headframe, under the skips' sheaves, turning as the cage goes back down. */
	private void cageSheaveFilm() {
		mode = "";
		showCard(false);
		waitFor(1600, level -> cageWinder(level).cageWinder().atLanding());
		// zoomed on its axle (the first take at 70 degrees showed it as one wheel of three)
		Cam a = Cam.of(c, 0.4, 28.4, 12.6, 4.0, 26.95, 7.25);
		Cam e = Cam.of(c, 1.2, 28.0, 11.6, 4.0, 26.95, 7.25);
		fov(40);
		film("hi07_cagesheave", 900, move(a, e, 900), (level, t) -> {
			if (t == PAD + 10) {
				press(level, d(-1, bot() + 1, 6));
				event("the bottom landing's button pressed: " + cageWinder(level).status());
			}
			cageEvents(level, t);
		});
	}

	/** hi07_call: at the collar, the call button pressed; the bell, and the cage comes up with its rider. */
	private void callFilm() {
		mode = "cage";
		showCard(true);
		waitFor(1600, level -> cageWinder(level).cageWinder().atLanding() && !cageWinder(level).cageWinder().up());
		Cam a = Cam.of(c, 1.2, 2.3, 12.5, 4.6, 1.3, 9.5);
		Cam e = Cam.of(c, 1.0, 2.6, 13.0, 3.6, 1.6, 6.0);
		film("hi07_call", 1000, move(a, e, 200), (level, t) -> {
			if (t == PAD + 10) {
				press(level, d(5, 1, 10));
				event("the collar's button pressed: " + cageWinder(level).status());
			}
			cageEvents(level, t);
		});
	}

	// ---- power and the whole --------------------------------------------------------------------------------------------------------

	/** hi07_power: the two gas sets and their substations, the medium-voltage cable to the hoist house. */
	private void powerFilm() {
		mode = "power";
		showCard(true);
		Cam a = Cam.of(c, -20, 12, 52, -40, 1, 32);
		Cam b = Cam.of(c, -14, 9, 34, -36, 1, 26);
		film("hi07_power", 600, move(a, b, 600), null);
	}

	/** hi07_wide: from the air, a slow turn over the mine: the headframe and hoist house, the section, the fan at the raise. */
	private void wideFilm() {
		mode = "";
		showCard(false);
		film("hi07_wide", 1100, orbitAround(c, -4, 0, -20, 75, 48, 225, 70, 1100), null);
	}
}
