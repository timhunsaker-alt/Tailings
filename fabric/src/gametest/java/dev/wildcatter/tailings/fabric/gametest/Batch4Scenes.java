package dev.wildcatter.tailings.fabric.gametest;

import dev.wildcatter.tailings.TailingsBlocks;
import dev.wildcatter.tailings.TailingsEntities;
import dev.wildcatter.tailings.block.DrillBlockEntity;
import dev.wildcatter.tailings.block.PlantBlockEntity;
import dev.wildcatter.tailings.block.ShaftLayout;
import dev.wildcatter.tailings.block.TbmCore;
import dev.wildcatter.tailings.client.TbmRenderer;
import dev.wildcatter.tailings.entity.ShaftSkipEntity;
import dev.wildcatter.tailings.entity.TbmEntity;
import dev.wildcatter.tailings.entity.UltraHaulTruckEntity;
import dev.wildcatter.tailings.fabric.gametest.Trailer.Cam;
import dev.wildcatter.tailings.mine.DriveCourse;
import dev.wildcatter.tailings.plant.Machine;
import dev.wildcatter.tailings.plant.PlantMotion;
import dev.wildcatter.tailings.plant.RockLoad;
import dev.wildcatter.tailings.rock.Rock;
import dev.wildcatter.tailings.rock.SizeClass;
import dev.wildcatter.tailings.shaft.Hoisting;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.IntFunction;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ParticleStatus;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Shorts batch 4's footage (TAILINGS_ONLY=batch4), each set its own run in the trailers' seed world (BATCH4_SET):
 * <ul>
 * <li>{@code tbm}: the tunnel boring machine boring into a rock ridge cut open on its east side (and, ahead of the face, its east half,
 * so the cutterhead's wheel shows turning): {@code tbm__01_face}, {@code tbm__02_rings}, {@code tbm__03_belt}. Real rate: 1 m in 20 s.</li>
 * <li>{@code ore}: rock to concentrate, one quick shot a stage: {@code ore__01_gyratory} (an ultra tips 363 t into the primary),
 * {@code ore__02_xrt} (the X-ray sorter on copper ore and andesite), {@code ore__03_sag} (the SAG and ball mills turning),
 * {@code ore__04_froth} (the tank cells' froth), {@code ore__05_concentrate} (the column cleaners and the concentrate filter).</li>
 * <li>{@code hoist}: a 3 t skip wound up a 5 x 5 shaft (cut open on its east side) at up to 10 m/s and dumping at the headframe:
 * {@code hoist__01_shaft} (following it up), {@code hoist__02_headframe} (the other skip arriving and pouring).</li>
 * </ul>
 * Filmed in real time at 2880 x 1620 (1440 x 810 points), clean view, no cursor, a readout card read from the machines each tick kept in
 * the frame's middle 9:16 for the vertical cut; {@link #PAD} still at each end. BATCH4_SHOTS=01,03 films only those (the rest still
 * run, unfilmed). Films to {@code fabric/build/scene-videos/<set>__<NN_name>.mov}; beats logged as "[batch4] event".
 */
final class Batch4Scenes {
	private static final int W = 1440, H = 810;
	private static final int PAD = 60;
	private static final String SEED = System.getenv().getOrDefault("TAILINGS_TRAILER_SEED", "tailings");
	private static final String SET = System.getenv().getOrDefault("BATCH4_SET", "tbm");

	private final ClientGameTestContext context;
	private TestServerContext server;
	private int ft;
	private long filmStart;
	private float lastYaw = Float.NaN;
	private final List<String> summary = new ArrayList<>();

	Batch4Scenes(ClientGameTestContext context) {
		this.context = context;
	}

	private static void log(String line) {
		System.out.println("[batch4] " + line);
	}

	private static boolean wanted(String id) {
		String only = System.getenv("BATCH4_SHOTS");
		return only == null || only.isBlank() || java.util.Arrays.stream(only.split(",")).anyMatch(id::contains);
	}

	void run() {
		context.runOnClient(client -> {
			var o = client.options;
			o.applyGraphicsPreset(net.minecraft.client.GraphicsPreset.FANCY);
			o.renderDistance().set(12);
			o.particles().set(ParticleStatus.ALL);
			o.entityDistanceScaling().set(3.0);
			o.bobView().set(false);
			o.fov().set(70);
			o.gamma().set(0.8);
			o.cloudStatus().set(net.minecraft.client.CloudStatus.FANCY);
			o.chatVisibility().set(net.minecraft.world.entity.player.ChatVisiblity.HIDDEN);
			// a client with no input for ten minutes drops to 10 fps (FramerateLimitTracker LONG_AFK), and a client game test waits a
			// frame a tick: every film after that would draw at half speed (Rock to Metal episode 9). Throttle only when minimized.
			o.inactivityFpsLimit().set(net.minecraft.client.InactivityFpsLimit.MINIMIZED);
		});
		context.getInput().resizeWindow(W, H);
		context.waitTicks(10);
		try (TestSingleplayerContext world = context.worldBuilder().setUseConsistentSettings(false).adjustSettings(s -> {
			s.setSeed(SEED);
			s.setGameMode(WorldCreationUiState.SelectedGameMode.CREATIVE);
		}).create()) {
			server = world.getServer();
			context.waitTicks(40);
			server.runCommand("gamerule advance_time false");
			server.runCommand("gamerule advance_weather false");
			server.runCommand("gamerule spawn_mobs false");
			server.runCommand("weather clear");
			server.runCommand("difficulty peaceful");
			server.runCommand("gamemode spectator @p");
			server.runCommand("time set 6000");
			server.runCommand("effect give @p minecraft:night_vision infinite 0 true");
			server.runCommand("tp @p 0 160 0");
			hud(false);
			guiWidth(480);
			context.waitTicks(20);
			try {
				switch (SET) {
					case "ore" -> ore();
					case "hoist" -> hoist();
					default -> tbm();
				}
			} catch (RuntimeException | AssertionError ex) {
				log("FAILED: " + ex);
				ex.printStackTrace();
			} finally {
				showCard(false);
				log("summary:\n  " + String.join("\n  ", summary));
				log("done");
			}
		}
	}

	// ---- window, card, camera, films ----------------------------------------------------------------------------------------------

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

	/**
	 * A transparent screen drawing the readout card across the top of the frame inside its middle 9:16 (the vertical cut), as large
	 * as that allows. Lines starting {@code ^} are titles (amber), {@code !} alarms (red), {@code +} good news (green).
	 */
	static final class Card extends Screen {
		static volatile String[] lines = new String[0];

		Card() {
			super(Component.literal("readout"));
		}

		@Override
		public boolean isPauseScreen() {
			return false;
		}

		@Override
		public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partial) {
		}

		@Override
		public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partial) {
			String[] now = lines;
			if (now.length == 0) {
				return;
			}
			int widest = 1;
			for (String l : now) {
				widest = Math.max(widest, font.width(text(l)));
			}
			float room = Math.min(width, height * 9f / 16f);
			float s = Math.min(3.2f, (room * 0.90f) / widest);
			int line = 11;
			int boxW = (int) (widest * s) + 14;
			int boxH = (int) (now.length * line * s) + 8;
			int x0 = (width - boxW) / 2;
			int y0 = (int) (height * 0.06f);
			graphics.fill(x0, y0, x0 + boxW, y0 + boxH, 0xC8101418);
			graphics.pose().pushMatrix();
			graphics.pose().translate(x0 + 7, y0 + 5);
			graphics.pose().scale(s, s);
			for (int i = 0; i < now.length; i++) {
				String l = now[i];
				int colour = l.startsWith("^") ? 0xFFF0C040 : l.startsWith("!") ? 0xFFFF5A50 : l.startsWith("+") ? 0xFF70E070 : 0xFFF2F2F2;
				graphics.text(font, text(l), 0, i * line, colour, true);
			}
			graphics.pose().popMatrix();
		}

		private static String text(String l) {
			return l.startsWith("^") || l.startsWith("!") || l.startsWith("+") ? l.substring(1) : l;
		}
	}

	private static void card(String... lines) {
		Card.lines = lines;
	}

	private void showCard(boolean on) {
		card();
		context.runOnClient(client -> {
			if (on && !(client.gui.screen() instanceof Card)) {
				client.gui.setScreen(new Card());
			} else if (!on && client.gui.screen() instanceof Card) {
				client.gui.setScreen(null);
			}
		});
		context.waitTicks(2);
	}

	/** Puts the camera at {@code c} on the client (and on the server for a cut), so the game draws it eased between ticks. */
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

	private void event(String id, String what) {
		log(String.format(Locale.ROOT, "event %s %.2f s (clock %.2f s): %s", id, ft / 20.0, (System.nanoTime() - filmStart) / 1e9, what));
	}

	interface Beat {
		/** Runs on the server thread each tick of the film; returns the card's lines (or null to leave them). */
		String[] at(ServerLevel level, int t);
	}

	private static double ease(double t) {
		t = Math.max(0, Math.min(1, t));
		return t * t * (3 - 2 * t);
	}

	private static IntFunction<Cam> move(Cam a, Cam b, int ticks) {
		return t -> a.lerp(b, ease((double) t / ticks));
	}

	/**
	 * A film: {@link #PAD} still, {@code move} ticks along {@code path}, {@link #PAD} still. {@code path} is asked each tick with the
	 * move's tick (0 to move); it may follow a machine. {@code done}, when given, ends the move early (then the end pad).
	 */
	private void film(String id, int move, IntFunction<Cam> path, Beat beat, java.util.function.BooleanSupplier done) {
		boolean want = wanted(id);
		cam(path.apply(0), true);
		settle(want ? 40 : 2);
		cam(path.apply(0), true);
		context.waitTicks(5);
		hideCursor();
		Filming film = want ? context.computeOnClient(client -> Filming.start(client, id)) : null;
		filmStart = System.nanoTime();
		int end = -1;
		for (ft = 0; ft <= PAD + move + PAD && (end < 0 || ft <= end); ft++) {
			int t = Math.max(0, Math.min(move, ft - PAD));
			cam(path.apply(t), false);
			int tick = ft;
			String[] lines = server.computeOnServer(srv -> beat == null ? null : beat.at(srv.overworld(), tick));
			if (lines != null) {
				card(lines);
			}
			if (ft % 10 == 0) {
				hideCursor();
			}
			if (ft % 20 == 0) {
				server.runCommand("kill @e[type=item]");
			}
			context.waitTick();
			if (end < 0 && done != null && ft > PAD && done.getAsBoolean()) {
				end = ft + PAD;
				event(id, "over; held still for the end caption");
			}
		}
		double clock = (System.nanoTime() - filmStart) / 1e9;
		if (film != null) {
			film.stop();
		}
		String line = String.format(Locale.ROOT, "%s: %.1f s by ticks, %.1f s by the clock%s", id, ft / 20.0, clock, want ? "" : " (not filmed)");
		log(line);
		summary.add(line);
	}

	private void ticks(int n, Runnable each) {
		for (int t = 0; t < n; t++) {
			if (t % 20 == 0 && each != null) {
				each.run();
			}
			context.waitTick();
		}
	}

	private static String f0(double v) {
		return String.format(Locale.ROOT, "%,.0f", v);
	}

	private static String f1(double v) {
		return String.format(Locale.ROOT, "%,.1f", v);
	}

	// ---- tbm --------------------------------------------------------------------------------------------------------------------

	/** The machine's entity near c, or null before it lifts. */
	private static TbmEntity machine(ServerLevel level, BlockPos c) {
		List<TbmEntity> all = level.getEntitiesOfClass(TbmEntity.class, new AABB(c).inflate(90));
		return all.isEmpty() ? null : all.get(0);
	}

	/** Ahead of the face, the bore's east half cut away too, so the wheel turning on the face shows from the open side. */
	private static void openAhead(ServerLevel level, BlockPos c, int front) {
		for (int z = front - 1; z >= front - 40; z--) {
			for (int x = 4; x < 10; x++) {
				for (int yy = -2; yy < 24; yy++) {
					BlockPos at = c.offset(x, yy, z - c.getZ());
					BlockState there = level.getBlockState(at);
					if (!there.isAir() && (there.is(Blocks.STONE) || there.is(Blocks.DIRT) || there.is(Blocks.GRASS_BLOCK) || there.is(Blocks.ANDESITE)
							|| there.is(Blocks.GRAVEL))) {
						level.setBlock(at, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
					}
				}
			}
		}
	}

	private String[] tbmCard(ServerLevel level, BlockPos c) {
		TbmEntity m = machine(level, c);
		if (m == null) {
			return new String[] {"^TUNNEL BORING MACHINE", "starting"};
		}
		int driven = m.core().drill().advanceCount();
		double perMetre = DriveCourse.STONE_TICKS / 20.0;
		boolean cutting = m.cutting();
		return new String[] {"^TUNNEL BORING MACHINE", (cutting ? "+CUTTING" : "!" + m.core().statusLine().toUpperCase(Locale.ROOT)),
				"Cutterhead " + f0(TbmRenderer.CUTTERHEAD_RPM) + " rpm", "Advance 1 m in " + f0(perMetre) + " s", "Bored " + driven + " m, " + driven + " rings",
				"Drive " + f0(TbmCore.BEAM_WATTS / 1000.0) + " kW"};
	}

	private void tbm() {
		BlockPos[] at = new BlockPos[1];
		server.runOnServer(srv -> {
			ServerLevel level = srv.overworld();
			BlockPos s = Trailer.site(level, 400, -200, 90, 120);
			int g = s.getY();
			BlockPos c = new BlockPos(s.getX() + 40, g, s.getZ() + 80);
			Trailer.pad(level, c.getX() - 40, c.getZ() - 80, c.getX() + 50, c.getZ() + 45, g, 14, 40);
			Trailer.hill(level, c.getX() - 40, c.getZ() - 80, c.getX() + 6, c.getZ() - 1, g, (x, z) -> {
				int fromSouth = c.getZ() - 1 - z;
				int fromWest = x - (c.getX() - 40);
				int fromNorth = z - (c.getZ() - 80);
				int h = Math.min(16, Math.min(6 + fromSouth * 3, Math.min(fromWest, fromNorth)));
				if (x >= c.getX() - 2) {
					h = Math.max(h, Math.min(12, 4 + fromSouth * 4));
				}
				return h;
			});
			for (int x = c.getX() + 10; x <= c.getX() + 30; x++) {
				for (int z = c.getZ() - 70; z <= c.getZ() + 2; z++) {
					level.setBlock(new BlockPos(x, g - 1, z), Blocks.GRAVEL.defaultBlockState(), Block.UPDATE_CLIENTS);
				}
			}
			Trailer.tbm(level, c, 0);   // the real advance: 1 m in 20 s in stone
			Trailer.cutAway(level, c);
			openAhead(level, c, c.getZ());
			at[0] = c;
		});
		BlockPos c = at[0];
		server.runCommand("tailings rule ventilation_interlock false");   // a short drive with no fan and duct yet: the gas interlock off
		Runnable keep = () -> server.runOnServer(srv -> {
			ServerLevel level = srv.overworld();
			Trailer.feedTbm(level, c);
			Trailer.cutAway(level, c);
			openAhead(level, c, (int) Math.floor(Trailer.tbmFront(level, c)));
		});
		cam(Cam.of(c, 20, 8, 6, 3, 4, -4), true);
		settle(80);
		server.runOnServer(srv -> ((DrillBlockEntity) srv.overworld().getBlockEntity(c.offset(3, 3, 3))).press(10, null));
		// let it lift and bore its first metre
		ticks(500, keep);
		showCard(true);
		double[] front = {server.computeOnServer(srv -> Trailer.tbmFront(srv.overworld(), c))};
		Beat beat = (level, t) -> {
			if (t % 20 == 0) {
				Trailer.feedTbm(level, c);
				Trailer.cutAway(level, c);
				openAhead(level, c, (int) Math.floor(Trailer.tbmFront(level, c)));
			}
			front[0] = Trailer.tbmFront(level, c);
			return tbmCard(level, c);
		};
		double z0 = front[0] - c.getZ();
		// 1: from ahead of the face on the open side: the wheel turning against the rock, the machine creeping toward the camera
		film("tbm__01_face", 360, t -> {
			double f = front[0] - c.getZ();
			return Cam.of(c, 15.5, 5.5, f - 9.5, 3.8, 4.0, f + 0.5);
		}, beat, null);
		// 2: alongside, behind the cutterhead: the erector setting each ring at its tail, the lined bore behind
		film("tbm__02_rings", 900, t -> {
			double f = front[0] - c.getZ();
			return Cam.of(c, 14.0, 6.0, f + 5.0, 2.0, 4.0, f + 3.0);
		}, beat, null);
		// 3: the back-up: muck on the conveyor gantry's belt, dropping into the car at its tail
		film("tbm__03_belt", 360, t -> {
			double f = front[0] - c.getZ();
			return Cam.of(c, 13.0, 6.5, f + 21.0, 3.0, 3.0, f + 25.0);
		}, beat, null);
		double driven = server.computeOnServer(srv -> z0 - (Trailer.tbmFront(srv.overworld(), c) - c.getZ()));
		log("tbm: bored " + f1(driven) + " m during the films; " + server.computeOnServer(srv -> {
			TbmEntity m = machine(srv.overworld(), c);
			return m == null ? "no machine" : m.core().readout();
		}));
	}

	// ---- ore: rock to concentrate ------------------------------------------------------------------------------------------------

	private void ore() {
		Object[] v = new Object[16];
		BlockPos[] at = new BlockPos[1];
		server.runOnServer(srv -> {
			ServerLevel level = srv.overworld();
			BlockPos s = Trailer.site(level, -400, 300, 110, 170);
			int g = s.getY();
			BlockPos o = new BlockPos(s.getX() + 50, g, s.getZ() + 60);
			Trailer.pad(level, o.getX() - 50, o.getZ() - 60, o.getX() + 70, o.getZ() + 110, g, 16, 45);
			// the primary as the pit line has it: the trucks' bench 26 up north, the pocket, the gyratory, the surge pocket, the apron feeder
			Trailer.hill(level, o.getX() - 40, o.getZ() - 60, o.getX() + 40, o.getZ() - 1, g, (x, z) -> {
				int dx = Math.max(Math.max(o.getX() - 2 - x, x - (o.getX() + 15)), 0);
				int dz = Math.max(z - (o.getZ() - 3), 0);
				int north = z - (o.getZ() - 60);
				return Math.min(26 - Math.max(dx, dz) * 2, north);
			}, true);
			Trailer.fillBox(level, o, Blocks.STONE, -2, 15, 0, 25, -40, -3);
			Trailer.fillBox(level, o, Blocks.STONE, -2, 15, 0, 21, -2, -1);
			Trailer.fillBox(level, o, Blocks.GRAVEL, -2, 15, 25, 25, -40, -3);
			PlantBlockEntity feeder = Trailer.plant(level, Machine.PRIMARY_APRON_FEEDER, o.offset(5, 0, 3));
			PlantBlockEntity surge = Trailer.plant(level, Machine.SURGE_POCKET, o.offset(3, 2, 1));
			PlantBlockEntity crusher = Trailer.plant(level, Machine.GYRATORY_CRUSHER, o.offset(4, 8, 2));
			Trailer.part(level, TailingsBlocks.GYRATORY_POCKET, o.offset(0, 18, 0));
			PlantBlockEntity breaker = Trailer.plant(level, Machine.ROCK_BREAKER, o.offset(12, 26, 2));
			Trailer.part(level, TailingsBlocks.TRUCK_DUMP_WALL, o.offset(1, 22, -2));
			int by = 0;
			for (int z = 15; z <= 75; z++) {
				if (z > 15 && (z - 15) % 3 == 0) {
					by++;
				}
				level.setBlock(o.offset(6, by, z), Trailer.heavyBelt(Direction.Axis.Z), Block.UPDATE_ALL);
			}
			PlantBlockEntity stackDrive = Trailer.plant(level, Machine.HEAVY_CONVEYOR_DRIVE, o.offset(7, by, 73));
			Trailer.plant(level, Machine.STOCKPILE_HEAD, o.offset(5, by - 4, 76));
			var truck = TailingsEntities.ULTRA_HAUL_TRUCK.create(level, EntitySpawnReason.COMMAND);
			truck.setPos(o.getX() + 7.0, o.getY() + 26, o.getZ() - 3.0 - truck.spec().length() / 2);
			truck.setYRot(180);
			truck.setFuel(truck.tank());
			truck.load().add("minecraft:copper_ore", 363_000);
			truck.refreshLoad();
			level.addFreshEntity(truck);
			// the X-ray sorter west of the belt, its compressor, its feed belt over its north end, product and waste belts over chests
			BlockPos x = o.offset(-30, 0, 70);
			PlantBlockEntity sorter = Trailer.plant(level, Machine.XRT_SORTER, x);
			PlantBlockEntity compressor = Trailer.plant(level, Machine.SORTER_COMPRESSOR, x.offset(-2, 0, 0));
			for (int dx = -6; dx <= 2; dx++) {
				level.setBlock(x.offset(dx, 5, 1), belt(Direction.Axis.X), Block.UPDATE_ALL);
			}
			PlantBlockEntity sortFeed = Trailer.plant(level, Machine.CONVEYOR_DRIVE, x.offset(3, 5, 0));
			for (int z = 8; z <= 14; z++) {
				level.setBlock(x.offset(1, 0, z), belt(Direction.Axis.Z), Block.UPDATE_ALL);
			}
			PlantBlockEntity product = Trailer.plant(level, Machine.CONVEYOR_DRIVE, x.offset(2, 0, 14));
			level.setBlock(x.offset(1, -1, 14), Blocks.CHEST.defaultBlockState(), Block.UPDATE_ALL);
			for (int dx = 4; dx <= 10; dx++) {
				level.setBlock(x.offset(dx, 0, 4), belt(Direction.Axis.X), Block.UPDATE_ALL);
			}
			PlantBlockEntity waste = Trailer.plant(level, Machine.CONVEYOR_DRIVE, x.offset(11, 0, 4));
			level.setBlock(x.offset(10, -1, 4), Blocks.CHEST.defaultBlockState(), Block.UPDATE_ALL);
			// the SAG mill and the ball mill on their slab, west of the bench
			BlockPos m = o.offset(-40, 0, 12);
			PlantBlockEntity sag = Trailer.plant(level, Machine.SAG_MILL, m);
			PlantBlockEntity ball = Trailer.plant(level, Machine.BALL_MILL_LARGE, m.offset(14, 0, 2));
			// the flotation row east of the belt, as p7_flotation lays it, its concentrate through two column cells to the filter
			BlockPos f = o.offset(24, 0, 4);
			Block tubing = BuiltInRegistries.BLOCK.getValue(Identifier.fromNamespaceAndPath("wildcatter", "air_tubing"));
			PlantBlockEntity source = Trailer.plant(level, Machine.CYCLONE, f);
			Trailer.pipe(level, f.offset(0, 0, 2), f.offset(1, 0, 2));
			PlantBlockEntity tank = Trailer.plant(level, Machine.CONDITIONING_TANK_LARGE, f.offset(0, 0, 3));
			Trailer.pipe(level, f.offset(3, 0, 11));
			PlantBlockEntity[] cells = new PlantBlockEntity[8];
			for (int k = 0; k < 8; k++) {
				int z = 12 + 8 * k;
				cells[k] = Trailer.plant(level, Machine.TANK_CELL, f.offset(0, 0, z));
				Trailer.pipe(level, f.offset(3, 0, z + 7));
				for (int dz = 0; dz < 8; dz++) {
					if (z + dz <= 74) {
						Trailer.pipe(level, f.offset(7, 0, z + dz));
					}
					level.setBlock(f.offset(3, 7, z + dz), tubing.defaultBlockState(), Block.UPDATE_ALL);
				}
			}
			PlantBlockEntity blower = Trailer.plant(level, Machine.FLOTATION_BLOWER_LARGE, f.offset(9, 0, 12));
			for (int dx = 4; dx <= 10; dx++) {
				level.setBlock(f.offset(dx, 7, 13), tubing.defaultBlockState(), Block.UPDATE_ALL);
			}
			for (int dy = 4; dy <= 6; dy++) {
				level.setBlock(f.offset(10, dy, 13), tubing.defaultBlockState(), Block.UPDATE_ALL);
			}
			PlantBlockEntity[] columns = {Trailer.plant(level, Machine.COLUMN_CELL, f.offset(8, 0, 40)), Trailer.plant(level, Machine.COLUMN_CELL, f.offset(8, 0, 46))};
			Trailer.pipe(level, f.offset(8, 0, 39), f.offset(8, 0, 45));
			for (int dx = 4; dx <= 7; dx++) {
				level.setBlock(f.offset(dx, 7, 41), tubing.defaultBlockState(), Block.UPDATE_ALL);
				level.setBlock(f.offset(dx, 7, 47), tubing.defaultBlockState(), Block.UPDATE_ALL);
			}
			Trailer.pipe(level, f.offset(11, 0, 44), f.offset(12, 0, 44), f.offset(13, 0, 44), f.offset(14, 0, 44), f.offset(15, 0, 44));
			Trailer.pipe(level, f.offset(11, 0, 50), f.offset(11, 1, 50), f.offset(12, 1, 50), f.offset(13, 1, 50), f.offset(13, 1, 49), f.offset(13, 1, 48), f.offset(13, 1, 47),
					f.offset(13, 1, 46), f.offset(13, 1, 45), f.offset(13, 1, 44));
			PlantBlockEntity concThickener = Trailer.plant(level, Machine.THICKENER, f.offset(15, 0, 45));
			Trailer.pipe(level, f.offset(18, 0, 53));
			PlantBlockEntity filter = Trailer.plant(level, Machine.FILTER, f.offset(17, 0, 54));
			for (PlantBlockEntity part : new PlantBlockEntity[] {feeder, crusher, breaker, stackDrive, sorter, compressor, sortFeed, product, waste, sag, ball, tank, blower,
					columns[0], columns[1], concThickener, filter}) {
				part.forceRunning();
			}
			for (PlantBlockEntity part : cells) {
				part.forceRunning();
			}
			for (String reagent : new String[] {"lime", "xanthate", "mibc"}) {
				ItemStack stack = new ItemStack(BuiltInRegistries.ITEM.getValue(dev.wildcatter.tailings.Tailings.id(reagent)), 64);
				while (!stack.isEmpty() && tank.offer(stack)) {
					// a stack of each into the hopper
				}
			}
			v[0] = truck;
			v[1] = surge;
			v[2] = crusher;
			v[3] = sorter;
			v[4] = sortFeed;
			v[5] = sag;
			v[6] = ball;
			v[7] = source;
			v[8] = cells;
			v[9] = columns;
			v[10] = filter;
			v[11] = concThickener;
			at[0] = o;
		});
		BlockPos o = at[0];
		String copper = dev.wildcatter.tailings.rock.Mineral.COPPER.mineralItemId();
		PlantBlockEntity surge = (PlantBlockEntity) v[1], sorter = (PlantBlockEntity) v[3], sortFeed = (PlantBlockEntity) v[4], sag = (PlantBlockEntity) v[5],
				ball = (PlantBlockEntity) v[6], source = (PlantBlockEntity) v[7], filter = (PlantBlockEntity) v[10];
		PlantBlockEntity[] cells = (PlantBlockEntity[]) v[8], columns = (PlantBlockEntity[]) v[9];
		Runnable keep = () -> server.runOnServer(srv -> {
			double short_ = 3000 - source.out().held(dev.wildcatter.fluid.Product.FINES_SLURRY);
			if (short_ > 0) {
				source.out().offer(dev.wildcatter.fluid.Product.FINES_SLURRY, short_, dev.wildcatter.fluid.SlurryAssay.of(Map.of(copper, 0.004)));
			}
			for (PlantBlockEntity cell : cells) {
				cell.out().give(dev.wildcatter.fluid.Product.TAILS_SLURRY, Double.MAX_VALUE);   // to the tailings thickener, off camera
			}
			for (PlantBlockEntity column : columns) {
				column.out().give(dev.wildcatter.fluid.Product.TAILS_SLURRY, Double.MAX_VALUE);
			}
			if (sortFeed.hold().total() < 400) {
				RockLoad load = new RockLoad();
				load.add(Rock.COPPER, SizeClass.CRUSHED, 200);
				load.add(Rock.ANDESITE, SizeClass.CRUSHED, 200);
				sortFeed.receive(load);
			}
			for (PlantBlockEntity mill : new PlantBlockEntity[] {sag, ball}) {
				mill.addWater(100_000);
			}
			if (sag.hold().total() < dev.wildcatter.tailings.plant.PitLine.SAG_NORMAL_CHARGE_KG * 0.9) {
				RockLoad charge = new RockLoad();
				charge.add(Rock.COPPER, SizeClass.CRUSHED, dev.wildcatter.tailings.plant.PitLine.SAG_NORMAL_CHARGE_KG - sag.hold().total());
				sag.receive(charge);
			}
			sag.out().give(dev.wildcatter.fluid.Product.FINES_SLURRY, Double.MAX_VALUE);
			ball.out().give(dev.wildcatter.fluid.Product.FINES_SLURRY, Double.MAX_VALUE);
		});
		keep.run();
		showCard(true);
		cam(Cam.of(o, 30, 30, -20, 8, 20, 0), true);
		settle(100);
		ticks(400, keep);   // the flotation row fills and floats; the sorter and the mills run
		// 1: the ultra tips 363 t into the gyratory's pocket from the bench; the crusher fills its surge pocket
		double[] surgeAt = {0, 0};
		boolean[] tipped = {false};
		film("ore__01_gyratory", 260, move(Cam.of(o, 30, 29, 18, 7, 25, -5), Cam.of(o, 27, 28, 15, 7, 25, -5), 260), (level, t) -> {
			if (t % 20 == 0) {
				keep.run();
			}
			UltraHaulTruckEntity truck = (UltraHaulTruckEntity) v[0];
			if (t == 30) {
				truck.testDrive(new Input(false, false, false, false, true, false, false));
			}
			if (t == 40) {
				truck.testDrive(Input.EMPTY);
			}
			int inPocket = level.getBlockEntity(o.offset(0, 18, 0)) instanceof dev.wildcatter.tailings.block.DumpBlockEntity d ? d.pocket() * dev.wildcatter.tailings.pit.PitMass.ROCK_KG / 1000 : 0;
			PlantBlockEntity crusher = (PlantBlockEntity) v[2];
			boolean crushing = crusher.status().startsWith("crushing");
			if (!tipped[0] && truck.load().total() < 360_000) {
				tipped[0] = true;
				event("ore__01_gyratory", "the truck tips");
			}
			return new String[] {"^PRIMARY GYRATORY", "1,066 mm feed opening", "Truck: " + f0(truck.load().total() / 1000) + " t of ore", "In its pocket: " + inPocket + " t",
					crushing ? "+Crushing " + f0(dev.wildcatter.tailings.plant.PitLine.GYRATORY_TPH) + " t/h to 150 mm" : "Waiting for rock"};
		}, null);
		// 2: the X-ray sorter: pieces read on its belt, the ore blown on to product, the waste dropped aside
		BlockPos x = o.offset(-30, 0, 70);
		film("ore__02_xrt", 200, move(Cam.of(x, 5.4, 3.7, 10.6, 2.2, 1.9, 7.2), Cam.of(x, 4.8, 3.4, 9.8, 2.2, 1.9, 7.2), 200), (level, t) -> {
			if (t % 20 == 0) {
				keep.run();
			}
			double p = sorter.xrt().productKg() / 1000, w = sorter.xrt().wasteKg() / 1000;
			return new String[] {"^X-RAY ORE SORTER", "Belt 3 m/s, read piece by piece", "+Ore blown on: " + f1(p) + " t", "!Waste dropped: " + f1(w) + " t",
					"Ore kept " + f0(100 * p / Math.max(1e-9, p + w)) + " % of the feed"};
		}, null);
		// 3: the SAG mill turning, the ball mill behind it
		BlockPos m = o.offset(-40, 0, 12);
		film("ore__03_sag", 200, move(Cam.of(m, -12.0, 7.5, 2.0, 5.5, 5.9, 6.7), Cam.of(m, -10.5, 7.0, 4.0, 5.5, 5.9, 6.7), 200), (level, t) -> {
			if (t % 20 == 0) {
				keep.run();
			}
			return new String[] {"^SAG MILL", "9.75 m across, " + f1(PlantMotion.SAG_RPM) + " rpm", "Rated 1,000 t/h", "Ball mill behind: " + f1(PlantMotion.BALL_MILL_LARGE_RPM) + " rpm"};
		}, null);
		// 4: over the tank cells: froth crowding to the launders
		BlockPos f = o.offset(24, 0, 4);
		film("ore__04_froth", 200, move(Cam.of(f, 3.5, 13.5, 14.5, 3.5, 5.8, 22.0), Cam.of(f, 3.5, 12.0, 17.0, 3.5, 5.8, 23.5), 200), (level, t) -> {
			if (t % 20 == 0) {
				keep.run();
			}
			double kg = 0;
			for (PlantBlockEntity cell : cells) {
				kg += cell.pit().totalKg();
			}
			return new String[] {"^FLOTATION", "Air and reagents float", "the copper minerals", "8 tank cells, 160 m3 each", "Floated: " + f1(kg / 1000) + " t"};
		}, null);
		// 5: the column cleaners and the concentrate filter
		long[] nuggets = {0};
		film("ore__05_concentrate", 220, move(Cam.of(f, 30.0, 11.0, 38.0, 13.0, 4.0, 50.0), Cam.of(f, 28.0, 10.0, 40.0, 13.0, 4.0, 50.0), 220), (level, t) -> {
			if (t % 20 == 0) {
				keep.run();
			}
			int n = filter.countIn(new int[] {1, 2}, dev.wildcatter.tailings.block.Feeds.stack(dev.wildcatter.tailings.rock.Mineral.COPPER.nuggetId(), 1))
					+ 9 * filter.countIn(new int[] {1, 2}, dev.wildcatter.tailings.block.Feeds.stack(dev.wildcatter.tailings.rock.Mineral.COPPER.phaseOneProductId(), 1));
			if (n >= nuggets[0] + 5 || nuggets[0] == 0 && n > 0) {
				event("ore__05_concentrate", "filter pays " + n + " nuggets");
				nuggets[0] = n;
			}
			return new String[] {"^COPPER CONCENTRATE", "Column cells clean the froth", "The filter presses it to cake", "+Paid: " + n + " copper nuggets"};
		}, null);
		server.runOnServer(srv -> {
			log("ore end: surge " + surge.hold().total() + " kg; sorter " + sorter.inspect().getString());
			log("ore end: sag " + sag.inspect().getString());
			log("ore end: cell 1 " + cells[0].inspect().getString());
			log("ore end: column " + columns[0].inspect().getString());
			log("ore end: filter " + filter.inspect().getString());
		});
	}

	private static BlockState belt(Direction.Axis axis) {
		return TailingsBlocks.BELT_CONVEYOR.defaultBlockState().setValue(dev.wildcatter.tailings.block.BeltConveyorBlock.AXIS, axis);
	}

	// ---- hoist ------------------------------------------------------------------------------------------------------------------

	private void hoist() {
		PlantBlockEntity[] parts = new PlantBlockEntity[4];
		ShaftSkipEntity[] skips = new ShaftSkipEntity[2];
		int[] geo = new int[4];
		server.runOnServer(srv -> {
			ServerLevel level = srv.overworld();
			BlockPos s = Trailer.site(level, 520, -60, 60, 60);   // dry ground (the TBM's ridge is 120 m west)
			int collar = s.getY();
			int depth = Math.min(150, collar - level.getMinY() - 12);
			int x0 = s.getX() + 20, z0 = s.getZ() + 16;
			Trailer.pad(level, x0 - 16, z0 - 14, x0 + 34, z0 + 40, collar, 18, 40);
			for (int x = x0 - 8; x <= x0 + 22; x++) {
				for (int z = z0 - 8; z <= z0 + 32; z++) {
					boolean mouth = x >= x0 + 2 && x <= x0 + 6 && z >= z0 + 2 && z <= z0 + 6;   // the shaft's mouth stays open (gravel there falls down it)
					level.setBlock(new BlockPos(x, collar - 1, z), (mouth ? Blocks.AIR : Math.floorMod(Mth.getSeed(x, 0, z) >> 16, 5) < 2 ? Blocks.STONE : Blocks.COARSE_DIRT)
							.defaultBlockState(), Block.UPDATE_CLIENTS);
				}
			}
			// the shaft, 5 x 5 down to the flask's chamber, in solid rock; east of it the rock cut away (a section) to show the skips
			for (int yy = collar - depth - 1; yy < collar - 1; yy++) {
				for (int x = x0 - 2; x <= x0 + 20; x++) {
					for (int z = z0 - 4; z <= z0 + 12; z++) {
						boolean shaft = x >= x0 + 2 && x <= x0 + 6 && z >= z0 + 2 && z <= z0 + 6;
						boolean chamber = x >= x0 + 7 && x <= x0 + 14 && z >= z0 && z <= z0 + 10 && yy < collar - depth + 8;
						boolean cut = x >= x0 + 7 && x <= x0 + 18 && z >= z0 - 2 && z <= z0 + 10 && yy < collar - 3;   // a wall of rock all round it, so no aquifer runs in
						BlockState to = yy == collar - depth - 1 || !(shaft || chamber || cut) ? Blocks.STONE.defaultBlockState() : Blocks.AIR.defaultBlockState();
						level.setBlock(new BlockPos(x, yy, z), to, Block.UPDATE_CLIENTS);
					}
				}
			}
			// shaft sets every 4 m (steel bands across the open side's back wall and down the cut's walls), a lamp every 12 m: so a
			// camera riding with the skip sees the shaft go by
			for (int yy = collar - depth + 2; yy < collar - 3; yy++) {
				boolean set = Math.floorMod(yy, 4) == 0, lamp = Math.floorMod(yy, 12) == 6;
				for (int z = z0 + 1; z <= z0 + 7 && (set || lamp); z++) {
					BlockPos back = new BlockPos(x0 + 1, yy, z);
					level.setBlock(back, (lamp && (z == z0 + 1 || z == z0 + 7) ? Blocks.SEA_LANTERN : set ? Blocks.POLISHED_DEEPSLATE : Blocks.STONE).defaultBlockState(),
							Block.UPDATE_CLIENTS);
				}
				for (int x = x0 + 7; x <= x0 + 18 && set; x++) {
					level.setBlock(new BlockPos(x, yy, z0 - 3), Blocks.POLISHED_DEEPSLATE.defaultBlockState(), Block.UPDATE_CLIENTS);
					level.setBlock(new BlockPos(x, yy, z0 + 11), Blocks.POLISHED_DEEPSLATE.defaultBlockState(), Block.UPDATE_CLIENTS);
				}
			}
			parts[0] = Trailer.plant(level, Machine.HEADFRAME, new BlockPos(x0, collar, z0));
			parts[1] = Trailer.plant(level, Machine.HEADFRAME_BIN, new BlockPos(x0 + 2, collar, z0 - 4));
			parts[2] = Trailer.plant(level, Machine.HOIST_HOUSE, new BlockPos(x0, collar, z0 + 20));
			parts[3] = Trailer.plant(level, Machine.MEASURING_FLASK, new BlockPos(x0 + 7, collar - depth, z0 + 4));
			parts[2].forceRunning();
			var frame = ShaftLayout.Frame.of(parts[0]);
			skips[0] = ShaftSkipEntity.hang(level, frame);
			skips[1] = ShaftSkipEntity.hang(level, frame);
			parts[3].made().add(Rock.COPPER, null, 6000);
			geo[0] = x0;
			geo[1] = z0;
			geo[2] = collar;
			geo[3] = depth;
		});
		int x0 = geo[0], z0 = geo[1], collar = geo[2], depth = geo[3];
		server.runCommand(String.format(Locale.ROOT, "forceload add %d %d %d %d", x0 - 10, z0 - 10, x0 + 30, z0 + 34));
		BlockPos h = new BlockPos(x0, collar, z0);
		Runnable keep = () -> server.runOnServer(srv -> {
			if (parts[3].made().total() < 3000) {
				parts[3].made().add(Rock.COPPER, null, 6000);
			}
		});
		var w = parts[2].winder();
		double[] top = {0};
		double[] fastest = {0};
		Beat beat = (level, t) -> {
			if (w.speed() > fastest[0] + 0.5 || w.speed() >= Hoisting.ROPE_SPEED - 1e-6 && fastest[0] < Hoisting.ROPE_SPEED - 1e-6) {
				fastest[0] = w.speed();
				log(String.format(Locale.ROOT, "event speed %.1f m/s at %.2f s", w.speed(), ft / 20.0));
			}
			if (t % 20 == 0 && parts[3].made().total() < 3000) {
				parts[3].made().add(Rock.COPPER, null, 6000);
			}
			// the skip coming up is the loaded one (top() flips at half way, so it cannot say which)
			int up = w.winding() ? (!w.load(0).isEmpty() ? 0 : !w.load(1).isEmpty() ? 1 : w.bottom()) : w.top();
			top[0] = skips[up].getY();
			double speed = w.speed();
			String line = w.winding() ? String.format(Locale.ROOT, "+%.1f m/s", speed) : w.rest() > 0 ? "DUMPING / LOADING" : "WAITING";
			return new String[] {"^SHAFT HOIST", "Rope speed " + line, "Skip: 3 t of ore", "Shaft " + f0(w.travel()) + " m, skip at " + f0(w.height(up)) + " m",
					"Hoisted " + w.dumps() + (w.dumps() == 1 ? " skip, " : " skips, ") + f0(w.dumps() * Hoisting.SKIP_KG / 1000) + " t"};
		};
		cam(Cam.of(h, 26, 4, 4, 4, 0, 4), true);
		settle(100);
		showCard(true);
		// the first wind: wait until it is under way (the flask fills in 10 s)
		for (int s = 0; s < 40 && !server.computeOnServer(srv -> w.winding()); s++) {
			ticks(20, keep);
		}
		log("hoist: winding " + server.computeOnServer(srv -> w.winding()) + "; travel " + w.travel() + " m; " + parts[2].status());
		// 1: following the loaded skip up the open side of the shaft, from the bottom to the collar
		film("hoist__01_shaft", 900, t -> {
			double y = Math.min(collar + 4, Math.max(collar - depth + 4, top[0] + 2.5));
			return new Cam(x0 + 13.0, y + 2.0, z0 + 6.5, x0 + 4.5, y, z0 + 4.0);
		}, beat, () -> server.computeOnServer(srv -> !w.winding()));
		// 2: the next wind: the headframe from the yard, the skip arriving, slowing to creep and pouring into the bin
		for (int s = 0; s < 40 && !server.computeOnServer(srv -> w.winding()); s++) {
			ticks(20, keep);
		}
		boolean[] arrived = {false};
		film("hoist__02_headframe", 1200, move(Cam.of(h, 17, 19.5, -10, 4, 19.5, 1), Cam.of(h, 16, 19.5, -9, 4, 19.5, 1), 1200), (level, t) -> {
			String[] lines = beat.at(level, t);
			if (!arrived[0] && !w.winding() && w.rest() > 0) {
				arrived[0] = true;
				event("hoist__02_headframe", "the skip at the dump, pouring");
			}
			return lines;
		}, () -> arrived[0] && server.computeOnServer(srv -> w.rest() < Hoisting.REST_SECONDS - 6));
		log("hoist end: " + parts[2].status() + "; bin " + parts[1].hold().total() + " kg; dumps " + w.dumps());
	}
}
