package dev.wildcatter.tailings.fabric.gametest;

import dev.wildcatter.block.BigPartBlock;
import dev.wildcatter.block.GensetControlPanelBlockEntity;
import dev.wildcatter.block.GensetPartBlockEntity;
import dev.wildcatter.block.GensetSkid;
import dev.wildcatter.energy.Voltage;
import dev.wildcatter.multiblock.BigParts;
import dev.wildcatter.tailings.TailingsBlocks;
import dev.wildcatter.tailings.TailingsEntities;
import dev.wildcatter.tailings.block.BatteryChargerBlockEntity;
import dev.wildcatter.tailings.block.DumpBlockEntity;
import dev.wildcatter.tailings.block.MineRailBlock;
import dev.wildcatter.tailings.block.RailGrade;
import dev.wildcatter.tailings.client.BatteryLocomotiveModel;
import dev.wildcatter.tailings.entity.MineVehicleEntity;
import dev.wildcatter.tailings.fabric.gametest.Trailer.Cam;
import dev.wildcatter.tailings.haul.Battery;
import dev.wildcatter.tailings.haul.Haul;
import dev.wildcatter.tailings.mine.Heading;
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
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Heavy Iron, episode 6, "The battery locomotive" (TAILINGS_ONLY=hi06): the 8 t battery locomotive (docs/multiblocks/battery-locomotive.md)
 * and three bottom-dump cars (bottom-dump-cars.md) on a drive cut into a ridge, on real power.
 * <ul>
 * <li>the site: a ridge of stone north of a yard, its east side cut away as a section so the drive shows; one track at x 0 from a
 * charging bay in the yard, through the portal, over a track grizzly with its cam rail and the dump pocket under it, up a 10 m
 * 1-in-2 rack decline to a lower level;</li>
 * <li>the power: a Wildcatter 500 kW diesel set, its cable on the ground to the world's three-phase 30 A receptacle beside the
 * charger; the charger draws from it as a receptacle load; fuel put in by the scene;</li>
 * <li>every film real time; the train placed, loaded and held by the scene between films (said on screen); a villager stands on the
 * locomotive's step for scale; the battery swap is done by the scene as a player's clicks would do it.</li>
 * </ul>
 * Filmed at 2880 x 1620 with a readout card read each tick. HI06_SHOTS=a,b films only those.
 */
final class Hi06Scenes {
	private static final int W = 1440, H = 810;
	private static final int PAD = 60;
	private static final String SEED = System.getenv().getOrDefault("TAILINGS_TRAILER_SEED", "tailings");
	private static final String LV = "wildcatter:cable_industrial";
	/** The decline: its top step's upper half at this z, {@link #DROP} m down to the lower level, 1 in 2. */
	private static final int TOP = -24, DROP = 10;
	/** The ends of the track: the lower level's face and the charging bay's stop. */
	private static final int LOW_END = -64, BAY_END = 26;
	/** The track grizzly's north end; it runs 4 m south. */
	private static final int GRIZ = -8;
	/** The charger's main cell, on the west side of the track facing it. */
	private static final BlockPos CHARGER = new BlockPos(-2, 0, 21);
	/** The diesel set's control panel, its skid to the west. */
	private static final BlockPos PANEL = new BlockPos(-12, 0, 11);

	private final ClientGameTestContext context;
	private TestServerContext server;
	/**
	 * The track's origin: x 0 is the rail, z 0 the portal, y 0 the yard's surface. The rail block is the floor's own cell (as the TBM
	 * lays it, gravel under it), so its top, and the train's wheels, are level with the yard and the drive's floor.
	 */
	private BlockPos c;
	private final List<String> summary = new ArrayList<>();
	private long filmStart;
	private int ft;
	private float lastYaw = Float.NaN;
	private int keeps;
	private UUID loco;
	private final List<UUID> cars = new ArrayList<>();
	private UUID villager;
	private BlockPos pocketAt;
	/** The battery's charge on the last tick and the draw from it, smoothed (kW). */
	private double lastWh = -1, drawKw;
	/** Card lines for the film in hand: the charger, the supply and the pocket. */
	private volatile boolean showCharger, showSupply, showPocket;
	private volatile double[] trainAt;

	Hi06Scenes(ClientGameTestContext context) {
		this.context = context;
	}

	void run() {
		context.runOnClient(client -> {
			client.options.renderDistance().set(16);
			client.options.particles().set(ParticleStatus.ALL);
			client.options.entityDistanceScaling().set(3.0);
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
			cardWidth("Charger: 18.0 kW in, 20.0 kW from the line");
			try {
				stage();
				power();
				scaleFilm();
				chargeFilm();
				powerFilm();
				swapFilm();
				outFilm();
				climbFilm();
				dumpFilm();
				grizzlyFilm();
				flatFilm();
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
		System.out.println("[hi06] " + line);
	}

	private static void require(boolean ok, String what) {
		if (!ok) {
			throw new AssertionError("[hi06] " + what);
		}
	}

	private static boolean wanted(String id) {
		String only = System.getenv("HI06_SHOTS");
		return only == null || only.isBlank() || java.util.Arrays.asList(only.split(",")).contains(id);
	}

	private BlockPos d(int x, int y, int z) {
		return c.offset(x, y, z);
	}

	// ---- window, card, camera, films (as Hi05Scenes) ------------------------------------------------------------------------------

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

	private void cam(Cam c, boolean cut) {
		double dx = c.tx() - c.x(), dy = c.ty() - c.y(), dz = c.tz() - c.z();
		float yaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
		float pitch = (float) -Math.toDegrees(Math.atan2(dy, Math.sqrt(dx * dx + dz * dz)));
		if (!cut && !Float.isNaN(lastYaw)) {
			yaw = lastYaw + Mth.wrapDegrees(yaw - lastYaw);
		}
		lastYaw = yaw;
		float y = yaw;
		if (cut) {
			server.runCommand(String.format(Locale.ROOT, "tp @p %.3f %.3f %.3f %.2f %.2f", c.x(), c.y() - 1.62, c.z(), yaw, pitch));
		}
		context.runOnClient(client -> {
			var p = client.player;
			if (p == null) {
				return;
			}
			p.setDeltaMovement(Vec3.ZERO);
			p.setPos(c.x(), c.y() - 1.62, c.z());
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

	private static IntFunction<Cam> via(Cam a, Cam b, Cam c, int move) {
		return t -> t < move / 2 ? a.lerp(b, ease(t / (move / 2.0))) : b.lerp(c, ease((t - move / 2.0) / (move - move / 2.0)));
	}

	/** Round a centre (relative to {@code base}) at a radius and height, from an angle (degrees, 0 = south of it), {@code turn} over the move. */
	private static IntFunction<Cam> orbitAround(BlockPos base, double cx, double cy, double cz, double r, double h, double from, double turn, int move) {
		return t -> {
			double a = Math.toRadians(from + turn * ease((double) t / move) * 0.15 + turn * ((double) t / move) * 0.85);
			return Cam.of(base, cx + Math.sin(a) * r, cy + h, cz + Math.cos(a) * r, cx, cy, cz);
		};
	}

	/**
	 * A camera on the cut-away (east) side, abreast of the train: {@code out} metres east of the rail, {@code up} above the
	 * locomotive, {@code lead} metres along the track from it (positive south), looking at a point {@code look} along from it. Eased
	 * toward where the train is (kept by film()'s beat, so no frame waits on the server).
	 */
	private IntFunction<Cam> abreast(double out, double up, double lead, double look, double lookUp) {
		double[] at = {Double.NaN, 0, 0, 0};
		return t -> {
			double[] e = trainAt;
			if (e == null) {
				return Cam.of(c, out, up, 0, 0, 0, 0);
			}
			double z = e[2] + lead, y = e[1] + up, tz = e[2] + look, ty = e[1] + lookUp;
			if (Double.isNaN(at[0])) {
				at[0] = z;
				at[1] = y;
				at[2] = tz;
				at[3] = ty;
			}
			at[0] += (z - at[0]) * 0.04;
			at[1] += (y - at[1]) * 0.04;
			at[2] += (tz - at[2]) * 0.08;
			at[3] += (ty - at[3]) * 0.08;
			return new Cam(c.getX() + 0.5 + out, at[1], at[0], c.getX() + 0.5, at[3], at[2]);
		};
	}

	private void film(String id, int move, IntFunction<Cam> path, Beat beat) {
		boolean want = wanted(id);
		for (String mob : new String[] {"cow", "pig", "sheep", "chicken", "horse", "donkey", "rabbit", "llama", "goat", "fox", "wolf", "cat", "frog", "bee", "armadillo", "turtle", "parrot"}) {
			server.runCommand("kill @e[type=minecraft:" + mob + "]");
		}
		server.runOnServer(srv -> track(srv.overworld()));
		// saplings and sticks from the leaves the pad left to decay, lying about the section
		server.runCommand("kill @e[type=minecraft:item]");
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
		for (int t = 0; t < most; t += 20) {
			if (server.computeOnServer(srv -> done.test(srv.overworld()))) {
				return true;
			}
			ticks(20);
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

	private static void lay(ServerLevel level, String id, BlockPos cell) {
		BlockState there = level.getBlockState(cell);
		if (there.getBlock() == wc(id)) {
			return;
		}
		require(there.canBeReplaced() || there.is(Blocks.GRASS_BLOCK) || there.is(Blocks.DIRT) || there.is(Blocks.STONE_BRICKS) || there.isAir(),
				id + " at " + cell.toShortString() + " is blocked by " + there);
		level.setBlock(cell, wc(id).defaultBlockState(), Block.UPDATE_ALL);
		// a conductor set in its armless default state sets itself again with its arms, with no neighbour update: tell the run
		level.updateNeighborsAt(cell, wc(id), null);
	}

	private static void part(ServerLevel level, Block block, BlockPos at, Direction facing) {
		BlockState state = block.defaultBlockState().setValue(BigPartBlock.FACING, facing);
		level.setBlock(at, state, Block.UPDATE_ALL);
		BigParts.grow(level, at, level.getBlockState(at));
	}

	// ---- the site -----------------------------------------------------------------------------------------------------------------

	/**
	 * The floor's height at {@code z} (the cell over the floor, where the train stands): the yard and top level at 0, the 1-in-2
	 * decline, the lower level at -DROP. The rail block is the cell under it, {@link #rail}.
	 */
	private static int railY(int z) {
		if (z > TOP) {
			return 0;
		}
		if (z <= TOP - 2 * DROP) {
			return -DROP;
		}
		return -((TOP - z) / 2);
	}

	/** The rail block's cell at {@code z}: the floor's own cell. */
	private BlockPos rail(int z) {
		return d(0, railY(z) - 1, z);
	}

	/** The section's floor east of the drive at {@code z}: under the top level's pocket, then down with the decline. */
	private static int floorY(int z) {
		return Math.min(-7, railY(z) - 2);
	}

	private void stage() {
		server.runOnServer(srv -> {
			ServerLevel level = srv.overworld();
			BlockPos s = Trailer.site(level, 400, -200, 70, 130);
			int g = s.getY();
			c = new BlockPos(s.getX() + 34, g, s.getZ() + 90);
			Trailer.pad(level, c.getX() - 40, c.getZ() - 80, c.getX() + 30, c.getZ() + 40, g, 14, 40);
			// solid rock under the ridge and the section, so the drive is cut in stone and no cave shows in the cut
			Trailer.fillBox(level, c, Blocks.STONE, -36, 26, -17, -1, -84, -1);
			// the ridge west of the rail: its south face the portal, rising inward, its flanks easing down; its east face (x 2) the cut
			Trailer.hill(level, c.getX() - 36, c.getZ() - 84, c.getX() + 1, c.getZ() - 1, g, (x, z) -> {
				int fromSouth = c.getZ() - 1 - z;
				int fromWest = x - (c.getX() - 36);
				int fromNorth = z - (c.getZ() - 84);
				int h = Math.min(20, 9 + fromSouth / 2);
				return Math.min(h, Math.min(fromWest, fromNorth) + 2);
			});
			// the section east of the drive, cut down to its floor; dressed stone along its far side
			for (int x = 2; x <= 26; x++) {
				for (int z = -84; z <= -1; z++) {
					int floor = floorY(z);
					for (int y = -17; y <= 30; y++) {
						BlockPos at = d(x, y, z);
						BlockState want = y <= floor ? Blocks.STONE.defaultBlockState() : Blocks.AIR.defaultBlockState();
						if (x >= 25) {
							want = y < -1 ? Blocks.STONE.defaultBlockState() : y == -1 ? Blocks.GRASS_BLOCK.defaultBlockState() : Blocks.AIR.defaultBlockState();
						}
						if (level.getBlockState(at) != want) {
							level.setBlock(at, want, Block.UPDATE_CLIENTS);
						}
					}
				}
			}
			// the drive, 4 m wide (x -2 to 1) and 4 m high over the rail, its east side open to the section; its floor graded with the
			// rail on the decline, gravel under the rail
			for (int z = LOW_END - 1; z <= 0; z++) {
				int r = railY(z);
				for (int x = -2; x <= 1; x++) {
					for (int y = r; y <= r + 3; y++) {
						level.setBlock(d(x, y, z), Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
					}
					BlockState floor = Blocks.STONE.defaultBlockState();
					if (z <= TOP && z > TOP - 2 * DROP && x != 0) {
						floor = TailingsBlocks.SLOPE.defaultBlockState().setValue(dev.wildcatter.tailings.block.SlopeBlock.FACING, Direction.NORTH)
								.setValue(dev.wildcatter.tailings.block.SlopeBlock.HALF, (TOP - z) % 2 == 0 ? dev.wildcatter.tailings.block.SlopeHalf.UPPER
										: dev.wildcatter.tailings.block.SlopeHalf.LOWER);
					}
					level.setBlock(d(x, r - 1, z), floor, Block.UPDATE_CLIENTS);
					if (x == 0) {
						level.setBlock(d(x, r - 2, z), Blocks.GRAVEL.defaultBlockState(), Block.UPDATE_CLIENTS);
					}
				}
			}
			// the yard's track bed to the bay: gravel under the rail, and past its end
			for (int z = 1; z <= BAY_END + 2; z++) {
				level.setBlock(d(0, z > BAY_END ? -1 : -2, z), Blocks.GRAVEL.defaultBlockState(), Block.UPDATE_CLIENTS);
			}
			// the rails, one track along x 0 heading north; the decline's steps graded and racked as the TBM lays them
			BlockState flat = TailingsBlocks.MINE_RAIL.defaultBlockState().setValue(MineRailBlock.FACING, Direction.NORTH);
			for (int z = LOW_END; z <= BAY_END; z++) {
				if (z >= GRIZ && z <= GRIZ + 3) {
					continue;
				}
				BlockState rail = flat;
				if (z <= TOP && z > TOP - 2 * DROP) {
					rail = flat.setValue(MineRailBlock.GRADE, (TOP - z) % 2 == 0 ? RailGrade.UPPER : RailGrade.LOWER).setValue(MineRailBlock.RACK, true);
				}
				level.setBlock(rail(z), rail, Block.UPDATE_ALL);
			}
			// the bottom-dump station, all in the floor: the track grizzly under the rails, the cam rail beside them in the open floor
			// (its top under the rail head, where the doors' rollers run), the pocket under the grizzly along the track
			for (int x = 0; x <= 4; x++) {
				for (int z = GRIZ - 1; z <= GRIZ + 4; z++) {
					for (int y = -6; y <= -1; y++) {
						if (x > 0 || y == -2) {
							level.setBlock(d(x, y, z), Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
						}
					}
				}
			}
			part(level, TailingsBlocks.TRACK_GRIZZLY, d(0, -2, GRIZ), Direction.NORTH);
			part(level, TailingsBlocks.DUMP_CAM_RAIL, d(1, -1, GRIZ), Direction.NORTH);
			pocketAt = d(1, -5, GRIZ);
			part(level, TailingsBlocks.DUMP_POCKET, pocketAt, Direction.EAST);
			Trailer.fillBox(level, c, Blocks.STONE, 0, 4, -7, -7, GRIZ - 1, GRIZ + 4);
			for (int k = 0; k < 4; k++) {
				level.setBlock(d(0, -1, GRIZ + k), flat.setValue(MineRailBlock.BRIDGE, true), Block.UPDATE_ALL);
			}
			log("pocket cells " + BigParts.cells(pocketAt, level.getBlockState(pocketAt)).stream().map(BlockPos::toShortString).toList()
					+ "; the grizzly finds it: " + (DumpBlockEntity.grizzlyUnder(level, d(0, -1, GRIZ + 1)) != null));
			log("site: origin " + c.toShortString() + ", rail at the decline's foot " + rail(TOP - 2 * DROP).toShortString() + "; the yard's grass at "
					+ level.getBlockState(d(3, -1, 10)).getBlock() + ", the rail there " + level.getBlockState(rail(10)).getBlock());
		});
		context.waitTicks(40);
	}

	/** The charging bay in the yard, and the diesel set that feeds its receptacle. */
	private void power() {
		server.runOnServer(srv -> {
			ServerLevel level = srv.overworld();
			BlockPos panel = d(PANEL.getX(), 0, PANEL.getZ());
			Trailer.fillBox(level, c, Blocks.SMOOTH_STONE, PANEL.getX() - 9, PANEL.getX() + 1, -1, -1, PANEL.getZ() - 5, PANEL.getZ() + 9);
			List<BlockPos> overwrote = GensetSkid.build(level, panel, Direction.NORTH, false);
			if (!overwrote.isEmpty()) {
				log("the set overwrote " + overwrote.size() + " blocks");
			}
			// the bay's wall west of the track, the charger on it facing the track
			Trailer.fillBox(level, c, Blocks.STONE_BRICKS, -3, -3, 0, 3, 17, 25);
			Trailer.fillBox(level, c, Blocks.STONE_BRICK_SLAB, -3, -1, 4, 4, 17, 25);
			BlockPos main = c.offset(CHARGER);
			part(level, TailingsBlocks.BATTERY_CHARGER, main, Direction.EAST);
			List<BlockPos> cells = BigParts.cells(main, level.getBlockState(main));
			int minZ = cells.stream().mapToInt(BlockPos::getZ).min().orElseThrow();
			int cx = cells.stream().filter(p -> p.getY() == c.getY()).mapToInt(BlockPos::getX).findFirst().orElseThrow();
			// the world's three-phase 30 A receptacle on the industrial tier, against the charger's north end
			boolean na = Voltage.INDUSTRIAL.lineToLine() > 440;
			BlockPos socket = new BlockPos(cx, c.getY(), minZ - 1);
			level.setBlock(socket, wc(na ? "wildcatter:receptacle_nema_l22_30" : "wildcatter:receptacle_iec_60309_red_32").defaultBlockState(), Block.UPDATE_ALL);
			// its cable on the ground from the set's panel: east, then south along the wall's back, then through it
			List<BlockPos> run = new ArrayList<>();
			BlockPos at = panel.east();
			int wallBack = c.getX() - 4;
			while (at.getX() < wallBack) {
				run.add(at);
				at = at.east();
			}
			while (at.getZ() < socket.getZ()) {
				run.add(at);
				at = at.south();
			}
			run.add(at);
			for (BlockPos p = at.east(); p.getX() < socket.getX(); p = p.east()) {
				level.setBlock(p, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
				run.add(p);
			}
			for (BlockPos p : run) {
				lay(level, LV, p);
			}
			log("charger cells " + cells.stream().map(BlockPos::toShortString).toList() + "; receptacle at " + socket.toShortString() + " (" + (na ? "NEMA L22-30" : "IEC red 32 A")
					+ "); cable " + run.size() + " m");
		});
		context.waitTicks(40);
		server.runOnServer(srv -> {
			fuel(srv.overworld());
			((GensetControlPanelBlockEntity) srv.overworld().getBlockEntity(d(PANEL.getX(), 0, PANEL.getZ()))).setWantRun(true);
		});
		// the set starts and comes up to speed off camera
		waitFor(1200, level -> set(level) != null && set(level).breakerClosed());
		ticks(100);
		placeTrain(BAY_END - 2, Heading.SOUTH, false, 0.62);
		ticks(100);
		server.runOnServer(srv -> {
			log("set: " + set(srv.overworld()).inspect().getString());
			log("charger: " + charger(srv.overworld()).inspect().getString());
		});
	}

	private void fuel(ServerLevel level) {
		if (level.getBlockEntity(GensetSkid.base(d(PANEL.getX(), 0, PANEL.getZ()), Direction.NORTH, false)) instanceof GensetPartBlockEntity tank) {
			tank.setFuel(2000);   // fuel put in by the scene
		}
	}

	private GensetControlPanelBlockEntity set(ServerLevel level) {
		return level.getBlockEntity(d(PANEL.getX(), 0, PANEL.getZ())) instanceof GensetControlPanelBlockEntity b ? b : null;
	}

	private BatteryChargerBlockEntity charger(ServerLevel level) {
		return (BatteryChargerBlockEntity) level.getBlockEntity(c.offset(CHARGER));
	}

	private DumpBlockEntity pocket(ServerLevel level) {
		return (DumpBlockEntity) level.getBlockEntity(pocketAt);
	}

	// ---- the train ----------------------------------------------------------------------------------------------------------------

	/**
	 * Off camera: the train put on the rail with its locomotive's centre near {@code z}, heading {@code heading}, three bottom-dump cars
	 * behind it, loaded or empty, its battery at {@code share} of its usable charge; held.
	 */
	private void placeTrain(int z, Heading heading, boolean loaded, double share) {
		server.runOnServer(srv -> {
			ServerLevel level = srv.overworld();
			for (MineVehicleEntity v : level.getEntitiesOfClass(MineVehicleEntity.class, new AABB(c).inflate(200))) {
				v.discard();
			}
			cars.clear();
			MineVehicleEntity l = MineVehicleEntity.placeLocomotive(level, rail(z), heading, TailingsEntities.BATTERY_LOCOMOTIVE);
			require(l != null, "the battery locomotive on the rail at z " + z);
			l.setBatteryWh(Battery.USABLE_WH * share);
			l.setRunning(false);
			loco = l.getUUID();
			int sign = heading == Heading.SOUTH ? -1 : 1;
			String[] loads = {"minecraft:copper_ore", "minecraft:cobblestone", "minecraft:copper_ore"};
			for (int i = 0; i < 3; i++) {
				double slot = l.getZ() + sign * Haul.carOffset(i);
				int cz = (int) Math.floor(slot);
				MineVehicleEntity car = MineVehicleEntity.placeCar(level, rail(cz - c.getZ()), heading, TailingsEntities.BOTTOM_DUMP_CAR);
				require(car != null, "car " + i + " on the rail at z " + cz);
				if (loaded) {
					car.load().add(loads[i], Haul.CAR_ITEMS);   // loaded by the scene
					car.refreshLoad();
				}
				cars.add(car.getUUID());
			}
			lastWh = -1;
			log("train placed: loco at " + l.position() + ", " + l.carIds().size() + " cars coupled, " + (loaded ? "loaded" : "empty"));
		});
		ticks(10);
	}

	private MineVehicleEntity loco(ServerLevel level) {
		return loco != null && level.getEntity(loco) instanceof MineVehicleEntity m ? m : null;
	}

	/** Each tick: where the train is for the cameras, the battery's draw, and the villager on the step. */
	private void track(ServerLevel level) {
		MineVehicleEntity l = loco(level);
		if (l == null) {
			return;
		}
		trainAt = new double[] {l.getX(), l.getY(), l.getZ()};
		double wh = l.batteryWh();
		if (lastWh >= 0 && wh >= 0) {
			double kw = Math.max(0, (lastWh - wh) * 3600 * 20 / 1000);
			drawKw += (kw - drawKw) * 0.1;
		}
		lastWh = wh;
		if (villager != null && level.getEntity(villager) instanceof Entity v) {
			// held on the step beside the cab by the scene: the east side, the cab's middle (the locomotive's rear half)
			double yaw = Math.toRadians(l.getYRot());
			double fx = -Math.sin(yaw), fz = Math.cos(yaw);
			double rearX = l.getX() - fx * 0.875, rearZ = l.getZ() - fz * 0.875;
			v.setPos(rearX + BatteryLocomotiveModel.STEP_OUT + 0.12, l.getY() + BatteryLocomotiveModel.STEP_UP, rearZ);
			v.setYRot(-90);
			v.setYHeadRot(-90);
			v.setYBodyRot(-90);
		}
	}

	private String[] card(ServerLevel level) {
		MineVehicleEntity l = loco(level);
		List<String> out = new ArrayList<>();
		out.add("^BATTERY LOCOMOTIVE · 8 t");
		if (l == null) {
			return out.toArray(String[]::new);
		}
		String status = l.status();
		int cut = status.indexOf("; battery");
		if (cut < 0) {
			cut = status.indexOf("; no battery");
		}
		String state = (cut >= 0 ? status.substring(0, cut) : status);
		if (state.startsWith("FLAT")) {
			state = "FLAT: braked on the rack";
		}
		boolean bad = state.startsWith("FLAT") || state.startsWith("NO ");
		out.add((bad ? "!" : "+") + state.toUpperCase(Locale.ROOT));
		out.add(l.hasBattery() ? "Battery: " + dev.wildcatter.tailings.item.LocoBatteryItem.describe(l.batteryWh()) : "!Battery: swapped out");
		out.add("From the battery: " + f1(drawKw) + " kW (motors " + f0(Battery.TRACTION_WATTS / 1000) + " kW)");
		int items = 0;
		for (UUID id : cars) {
			if (level.getEntity(id) instanceof MineVehicleEntity car) {
				items += car.load().total();
			}
		}
		if (!cars.isEmpty()) {
			out.add("3 bottom-dump cars: " + f0(items) + " items of rock");
		}
		if (showCharger) {
			BatteryChargerBlockEntity ch = charger(level);
			if (ch.heldWh() >= 0) {
				out.add("Charger, its own box: " + dev.wildcatter.tailings.item.LocoBatteryItem.describe(ch.heldWh()));
			}
			out.add(ch.intoBattery() > 100 ? "+Charger: " + f1(ch.intoBattery() / 1000) + " kW in, " + f1(ch.lineWatts() / 1000) + " kW from the line"
					: "Charger: " + (ch.status().startsWith("FULL") ? "FULL" : "idle"));
		}
		if (showSupply) {
			GensetControlPanelBlockEntity s = set(level);
			out.add("Diesel set: " + f1(s.watts() / 1000) + " kW of " + f0(s.ratedWatts() / 1000) + " kW");
		}
		if (showPocket) {
			out.add("Pocket under the grizzly: " + f0(pocket(level).pocket() + pocket(level).oversize()) + " of " + f0(Haul.POCKET_ITEMS) + " items");
		}
		return out.toArray(String[]::new);
	}

	private void keep(MinecraftServer srv) {
		ServerLevel level = srv.overworld();
		fuel(level);
		if (++keeps % 15 == 0) {
			MineVehicleEntity l = loco(level);
			log(String.format(Locale.ROOT, "tick %.1f ms; %s; at %s", srv.getAverageTickTimeNanos() / 1e6, l == null ? "no train" : l.status(), l == null ? "-" : l.position()));
		}
	}

	private void placeVillager() {
		server.runOnServer(srv -> {
			ServerLevel level = srv.overworld();
			var v = net.minecraft.world.entity.EntityTypes.VILLAGER.create(level, EntitySpawnReason.COMMAND);
			require(v != null, "a villager");
			v.setNoAi(true);
			v.setSilent(true);
			v.addTag("hi06v");
			MineVehicleEntity l = loco(level);
			v.setPos(l.getX() + 1, l.getY(), l.getZ());
			level.addFreshEntity(v);
			villager = v.getUUID();
			track(level);
		});
	}

	private void removeVillager() {
		villager = null;
		server.runCommand("kill @e[tag=hi06v]");
	}

	// ---- the films ----------------------------------------------------------------------------------------------------------------

	/** hi06_scale: the locomotive standing at the charger, a villager on its step: from along the yard round to its side. */
	private void scaleFilm() {
		placeVillager();
		showCard(true);
		showCharger = true;
		double lz = BAY_END - 2.5;
		// the train's wheels at the yard's surface (y 0), the rail in the ground
		Cam a = Cam.of(c, 9, 4.0, lz + 12, 0.5, 0.2, lz - 2);
		Cam b = Cam.of(c, 4.2, 1.8, lz - 1.5, 0.5, 1.0, lz - 0.6);
		film("hi06_scale", 900, move(a, b, 900), null);
	}

	/** hi06_charge: the charger on the bay's wall, the receptacle and its cable, the locomotive charging where it stands. */
	private void chargeFilm() {
		double lz = BAY_END - 2.5;
		Cam a = Cam.of(c, 3.8, 2.0, 17.5, -2, 0.8, 21.5);
		Cam b = Cam.of(c, 4.6, 2.4, 26.5, -1.5, 0.6, 21.0);
		film("hi06_charge", 700, move(a, b, 700), null);
		removeVillager();
	}

	/** hi06_power: the diesel set and its cable along the ground to the receptacle at the bay. */
	private void powerFilm() {
		showSupply = true;
		Cam a = Cam.of(c, -6, 9, 2, -14, 1, 12);
		Cam b = Cam.of(c, 4, 6, 6, -6, 1, 18);
		film("hi06_power", 700, move(a, b, 700), null);
	}

	/** hi06_swap: the scene lifts the box out (the bay empty), sets it in the charger, and puts a charged one in. */
	private void swapFilm() {
		showSupply = false;
		double lz = BAY_END - 2.5;
		Cam a = Cam.of(c, 4.5, 2.6, lz + 3.5, 0.0, 0.6, lz - 1.5);
		Cam b = Cam.of(c, 4.0, 2.4, lz + 1.0, -0.5, 0.6, lz - 2.0);
		film("hi06_swap", 700, move(a, b, 700), (level, t) -> {
			MineVehicleEntity l = loco(level);
			BatteryChargerBlockEntity ch = charger(level);
			if (t == PAD + 80) {
				double wh = l.batteryWh();
				l.takeBattery();
				event("battery taken out, " + f1(wh) + " Wh");
				ch.setHeldWh(wh);
				event("the box set in the charger");
			}
			if (t == PAD + 300) {
				l.setBatteryWh(Battery.USABLE_WH);
				event("a charged box put in the locomotive");
			}
		});
	}

	/** hi06_out: the empty train backs out of the bay, through the portal and down the decline to the lower level. */
	private void outFilm() {
		showCharger = false;
		int[] started = {-1};
		film("hi06_out", 1500, abreast(6, 1.8, 2, -3, 0.5), (level, t) -> {
			MineVehicleEntity l = loco(level);
			if (t == PAD) {
				l.reverse();
				l.setRunning(true);
				started[0] = t;
				event("run, backing its cars out of the bay");
			}
			if (l.status().startsWith("at the end")) {
				l.setRunning(false);
			}
		});
		// off camera: the cars loaded at the lower level by the scene, the train turned to run locomotive first
		server.runOnServer(srv -> {
			ServerLevel level = srv.overworld();
			MineVehicleEntity l = loco(level);
			log("out: stopped at " + l.position() + ": " + l.status());
		});
		waitFor(600, level -> loco(level).standing() && loco(level).getZ() < c.getZ() + TOP - 2 * DROP);
		server.runOnServer(srv -> {
			ServerLevel level = srv.overworld();
			String[] loads = {"minecraft:copper_ore", "minecraft:cobblestone", "minecraft:copper_ore"};
			int i = 0;
			for (UUID id : cars) {
				if (level.getEntity(id) instanceof MineVehicleEntity car) {
					car.load().clear();
					car.load().add(loads[i++ % 3], Haul.CAR_ITEMS);   // loaded by the scene
					car.refreshLoad();
				}
			}
			MineVehicleEntity l = loco(level);
			l.reverse();
			l.setRunning(false);
			log("loaded at the lower level: " + l.position());
		});
		ticks(20);
	}

	/** hi06_climb: the loaded train climbs the 1-in-2 rack decline on its battery, from the lower level to the top. */
	private void climbFilm() {
		film("hi06_climb", 1700, abreast(8, 1.0, 2, -2.5, 0.2), (level, t) -> {
			MineVehicleEntity l = loco(level);
			if (t == PAD + 20) {
				l.setRunning(true);
				event("run, locomotive first, loaded");
			}
			if (l.getZ() > c.getZ() + TOP + 4 && l.getY() > c.getY() - 0.5) {
				l.setRunning(false);   // held at the top by the scene, for the dump film
			}
		});
		require(server.computeOnServer(srv -> loco(srv.overworld()).getY() > c.getY() - 0.5), "the train should have climbed to the top: "
				+ server.computeOnServer(srv -> loco(srv.overworld()).status()));
	}

	/** hi06_dump: from the top of the decline, the train runs over the track grizzly and every car drops its load on the move. */
	private void dumpFilm() {
		showPocket = true;
		// the rail's top and the train at the floor (y 0); the pocket's fill seen under its bars in the open floor beside the track.
		// Back and up from the first takes' (5, 1.2): the cars run through the middle of the frame, their tops below the readout card,
		// so the episode no longer crops them to keep the card out (fix2-hi05-06)
		Cam a = Cam.of(c, 9.5, 3.2, GRIZ + 1.0, 0.5, 0.0, GRIZ + 2.5);
		film("hi06_dump", 800, t -> a, (level, t) -> {
			MineVehicleEntity l = loco(level);
			if (t == PAD + 10) {
				l.setRunning(true);
				event("run toward the grizzly");
			}
			dumpEvents(level, l);
			if (l.status().startsWith("at the end")) {
				l.setRunning(false);
			}
		});
	}

	/** Logs each car's doors opening, its load falling (its count each half second) and the car empty. */
	private final java.util.Set<String> marks = new java.util.HashSet<>();

	private boolean mark(MineVehicleEntity car, String what) {
		return marks.add(car.getUUID() + what);
	}

	private void dumpEvents(ServerLevel level, MineVehicleEntity l) {
		int n = 0;
		for (UUID id : cars) {
			n++;
			if (level.getEntity(id) instanceof MineVehicleEntity car) {
				if (car.doorsOpen() && mark(car, "opened")) {
					event(String.format(Locale.ROOT, "car %d: doors open over the grizzly at %.1f m/s, %d items in it", n, l.speed(), car.load().total()));
				}
				if (car.dumping() && ft % 10 == 0) {
					event(String.format(Locale.ROOT, "car %d: %d items left, pocket %d", n, car.load().total(), pocket(level).pocket() + pocket(level).oversize()));
				}
				if (marks.contains(car.getUUID() + "opened") && car.load().total() == 0 && mark(car, "empty")) {
					event(String.format(Locale.ROOT, "car %d: empty; pocket %d", n, pocket(level).pocket() + pocket(level).oversize()));
				}
				if (!car.doorsOpen() && marks.contains(car.getUUID() + "empty") && mark(car, "shut")) {
					event("car " + n + ": doors shut");
				}
			}
		}
	}

	/** hi06_grizzly: a second run, closer: the doors swing open over the grizzly and shut at the closing ramp. */
	private void grizzlyFilm() {
		waitFor(600, level -> loco(level).standing());
		placeTrain(TOP + 4, Heading.SOUTH, true, 0.9);
		// from above and ahead, looking back along the track down onto the grizzly as the cars come over it
		Cam a = Cam.of(c, 2.6, 2.6, GRIZ + 8.5, 0.5, -0.4, GRIZ + 1.5);
		Cam b = Cam.of(c, 2.2, 2.2, GRIZ + 7.0, 0.5, -0.4, GRIZ + 1.8);
		film("hi06_grizzly", 600, move(a, b, 600), (level, t) -> {
			MineVehicleEntity l = loco(level);
			dumpEvents(level, l);
			if (t == PAD) {
				l.setRunning(true);
				event("run toward the grizzly (placed and loaded by the scene)");
			}
			if (l.status().startsWith("at the end")) {
				l.setRunning(false);
			}
		});
		showPocket = false;
	}

	/** hi06_flat: a locomotive alone, its battery all but flat, starts up the decline and stops FLAT, braked on the rack. */
	private void flatFilm() {
		waitFor(600, level -> loco(level).standing());
		server.runOnServer(srv -> {
			ServerLevel level = srv.overworld();
			for (MineVehicleEntity v : level.getEntitiesOfClass(MineVehicleEntity.class, new AABB(c).inflate(200))) {
				v.discard();
			}
			cars.clear();
			int z = TOP - 2 * DROP - 3;
			MineVehicleEntity l = MineVehicleEntity.placeLocomotive(level, rail(z), Heading.SOUTH, TailingsEntities.BATTERY_LOCOMOTIVE);
			require(l != null, "the lone locomotive");
			l.setBatteryWh(160);   // all but flat, set by the scene
			l.setRunning(false);
			loco = l.getUUID();
			lastWh = -1;
		});
		ticks(10);
		film("hi06_flat", 700, abreast(7, 1.0, 1, -1, 0.3), (level, t) -> {
			if (t == PAD + 20) {
				loco(level).setRunning(true);
				event("run, battery all but flat");
			}
			if (loco(level).status().startsWith("FLAT") && loco(level).addTag("flat")) {
				event("FLAT at " + loco(level).position());
			}
		});
	}

	/** hi06_wide: from the air, a slow turn over the ridge cut open: the yard, the bay, the portal, the grizzly, the decline. */
	private void wideFilm() {
		placeTrain(BAY_END - 2, Heading.SOUTH, false, 0.8);
		film("hi06_wide", 1000, orbitAround(c, 0, 0, -4, 36, 16, 125, 55, 1000), null);
	}
}
