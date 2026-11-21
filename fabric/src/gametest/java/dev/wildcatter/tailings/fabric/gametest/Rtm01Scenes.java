package dev.wildcatter.tailings.fabric.gametest;

import dev.wildcatter.block.GensetControlPanelBlockEntity;
import dev.wildcatter.block.GensetPartBlockEntity;
import dev.wildcatter.block.GensetSkid;
import dev.wildcatter.block.BigPartBlock;
import dev.wildcatter.control.ControlDevice;
import dev.wildcatter.control.Measurable;
import dev.wildcatter.multiblock.BigParts;
import dev.wildcatter.tailings.TailingsBlocks;
import dev.wildcatter.tailings.TailingsEntities;
import dev.wildcatter.tailings.block.DumpBlockEntity;
import dev.wildcatter.tailings.block.PlantBlockEntity;
import dev.wildcatter.tailings.block.TailingsPartBlock;
import dev.wildcatter.tailings.entity.UltraHaulTruckEntity;
import dev.wildcatter.tailings.fabric.gametest.Trailer.Cam;
import dev.wildcatter.tailings.plant.Machine;
import dev.wildcatter.tailings.plant.PitLine;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.IntConsumer;
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
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ParticleStatus;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * Rock to Metal, episode 1, "Breaking Rock" (TAILINGS_ONLY=rtm01): the pit line's primary gyratory, its 150 m stockpile conveyor, the
 * stockpile and its reclaim, every motor on real power. Five generator sets stand in a yard round the line (Wildcatter's gas and
 * diesel sets; the medium-voltage ones through unit substations run backward), each motor on its own cable to its west-face box.
 * Filmed in real time at 2880 x 1620 (1440 x 810 points) with a clean view: no hotbar, chat or cursor; readouts on a large card at
 * the top right, read from the blocks each tick; the camera driven on the client each tick, so the game draws it smoothly between
 * ticks. Each film holds still {@link #PAD} ticks at each end. RTM01_SHOTS=rtm01_tip,... films only those (the rest still run, unfilmed,
 * so the plant is where it would be). Films to {@code fabric/build/scene-videos/<scene>.mov}; each film's beats are logged at their
 * second ("[rtm01] event").
 * <p>
 * The episode's twelve new scenes: {@code rtm01_ore_close}, {@code rtm01_size_ladder}, {@code rtm01_build_stack}, {@code rtm01_build_power},
 * {@code rtm01_gyratory_orbit}, {@code rtm01_pocket_readout}, {@code rtm01_bridged}, {@code rtm01_belt_drives}, {@code rtm01_pile_growth},
 * {@code rtm01_drawdown}, {@code rtm01_weigher} (and {@code rtm01_weigher_setpoint}), {@code rtm01_plc_points} (and
 * {@code rtm01_plc_points_gyratory}); and the shots the plan took from older, force-run films, filmed again here on real power:
 * {@code rtm01_tip}, {@code rtm01_start_seq}, {@code rtm01_wide}, {@code rtm01_gallery}, {@code rtm01_reclaim}, {@code rtm01_sag_hook}.
 */
final class Rtm01Scenes {
	private static final int W = 1440, H = 810;
	private static final int PAD = 60;
	private static final String SEED = System.getenv().getOrDefault("TAILINGS_TRAILER_SEED", "tailings");
	private static final String MV = "wildcatter:cable_mv", LV = "wildcatter:cable_industrial";

	private final ClientGameTestContext context;
	private TestServerContext server;
	private BlockPos o;
	/** The stockpile conveyor's head: its last belt cell and its height. */
	private static final int HEAD_Z = 164, HEAD_Y = 30;
	/** Blocks in the pile already standing when the scenes start. */
	private static final int PILE = Integer.parseInt(System.getenv().getOrDefault("RTM01_PILE", "2500"));
	/** The SAG feed belt's last cell (over the SAG's feed end) and its height. */
	private int sagZ, sagY;
	private PlantBlockEntity feeder, crusher, breaker, driveA, driveB, sagDrive, head, weigher, sag;
	private final PlantBlockEntity[] reclaim = new PlantBlockEntity[3];
	private UltraHaulTruckEntity truck;
	private final List<BlockPos> panels = new ArrayList<>();
	/** The gyratory's medium-voltage run, box first: laid on camera in rtm01_build_power. */
	private final List<BlockPos> gyratoryRun = new ArrayList<>();
	private final List<String> summary = new ArrayList<>();
	private long filmStart;
	private int ft;
	private float lastYaw = Float.NaN;
	private boolean drainSag = true;
	private int tips;

	Rtm01Scenes(ClientGameTestContext context) {
		this.context = context;
	}

	private static void log(String line) {
		System.out.println("[rtm01] " + line);
	}

	private static void require(boolean ok, String what) {
		if (!ok) {
			throw new AssertionError("[rtm01] " + what);
		}
	}

	private static boolean wanted(String id) {
		String only = System.getenv("RTM01_SHOTS");
		return only == null || only.isBlank() || java.util.Arrays.asList(only.split(",")).contains(id);
	}

	private BlockPos at(int x, int y, int z) {
		return o.offset(x, y, z);
	}

	// ---- the run ------------------------------------------------------------------------------------------------------------------

	void run() {
		context.runOnClient(client -> {
			client.options.renderDistance().set(16);
			client.options.particles().set(ParticleStatus.ALL);
			client.options.entityDistanceScaling().set(3.0);
			client.options.fov().set(70);
			client.options.gamma().set(1.0);
			client.options.bobView().set(false);
			client.options.chatVisibility().set(net.minecraft.world.entity.player.ChatVisiblity.HIDDEN);
		});
		context.getInput().resizeWindow(W, H);
		context.waitTicks(10);
		guiWidth(720);
		try (TestSingleplayerContext world = context.worldBuilder().setUseConsistentSettings(false).adjustSettings(s -> {
			s.setSeed(SEED);
			s.setGameMode(WorldCreationUiState.SelectedGameMode.CREATIVE);
		}).create()) {
			server = world.getServer();
			context.waitTicks(40);
			server.runCommand("gamerule advance_time false");
			server.runCommand("gamerule advance_weather false");
			server.runCommand("gamerule do_mob_spawning false");
			server.runCommand("weather clear");
			server.runCommand("time set 1500");
			server.runCommand("difficulty peaceful");
			server.runCommand("gamemode spectator @p");
			server.runCommand("tp @p 0 160 0");
			context.waitTicks(20);
			hud(false);
			try {
				stage();
				oreClose();
				sizeLadder();
				buildStack();
				wirePrimary();
				buildPower();
				tip();
				startSequence();
				pocketReadout();
				wide();
				orbit();
				bridged();
				beltDrives();
				gallery();
				reclaimTunnel();
				weigherDrop();
				weigherSetpoint();
				plcPoints();
				pileGrowth();
				drawdown();
				sagHook();
			} catch (RuntimeException | AssertionError ex) {
				log("FAILED: " + ex);
				ex.printStackTrace();
				throw ex;
			} finally {
				server.runCommand("tick rate 20");
				context.runOnClient(client -> client.gui.setScreen(null));
				hud(true);
				log("summary:\n  " + String.join("\n  ", summary));
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
	 * A transparent screen drawing the readout card at the top right of the frame (the episode's titles go top left, its subtitles at
	 * the bottom). Lines starting {@code ^} are titles (amber), {@code !} alarms (red), {@code +} good news (green).
	 */
	static final class Card extends Screen {
		static volatile String[] lines = new String[0];
		/** If set, the card is sized for this text at the least, so it keeps its size while its lines change (episode 2). */
		static volatile String widthOf = "";

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
			int widest = Math.max(1, font.width(widthOf));
			for (String l : now) {
				widest = Math.max(widest, font.width(text(l)));
			}
			float s = Math.min(3.0f, (width * 0.40f) / widest);
			int line = 11;
			int boxW = (int) (widest * s) + 14;
			int boxH = (int) (now.length * line * s) + 8;
			int x0 = (int) (width * 0.97f) - boxW;
			int y0 = (int) (height * 0.06f);
			graphics.fill(x0, y0, x0 + boxW, y0 + boxH, 0xC8101418);
			graphics.fill(x0, y0, x0 + 2, y0 + boxH, 0xFFE2A048);
			graphics.pose().pushMatrix();
			graphics.pose().translate(x0 + 8, y0 + 5);
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

	/** Puts the camera at {@code c} on the client: set after a tick, it is drawn eased from where it was over the next. */
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

	private Cam cam(double x, double y, double z, double tx, double ty, double tz) {
		return Cam.of(o, x, y, z, tx, ty, tz);
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

	/** Eased 0 to 1. */
	private static double ease(double t) {
		t = Math.max(0, Math.min(1, t));
		return t * t * (3 - 2 * t);
	}

	/** A camera move from a to b over {@code move} ticks, eased. */
	private static IntFunction<Cam> move(Cam a, Cam b, int move) {
		return t -> a.lerp(b, ease((double) t / move));
	}

	/**
	 * A film: {@link #PAD} ticks still at the start, then {@code move} ticks of camera path ({@code path(0..move)}), then {@link #PAD}
	 * still. {@code beat} runs every tick on the server with the film's tick. Unwanted shots still run their beats, unfilmed.
	 */
	private void film(String id, int move, IntFunction<Cam> path, Beat beat) {
		boolean want = wanted(id);
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
			if (beat != null) {
				server.runOnServer(srv -> beat.at(srv.overworld(), tick));
			}
			if (ft % 10 == 0) {
				hideCursor();
			}
			if (ft % 20 == 0) {
				server.runOnServer(srv -> keep(srv));
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

	/** Every second: the sets' fuel topped up, the stopped SAG's feed taken away (it waits for episode 2), items cleared. */
	private void keep(MinecraftServer srv) {
		ServerLevel level = srv.overworld();
		for (BlockPos panel : panels) {
			if (level.getBlockEntity(panel) instanceof GensetControlPanelBlockEntity box) {
				if (box.gas()) {
					box.setGasForTest(200);
				} else if (level.getBlockEntity(GensetSkid.base(panel, Direction.NORTH, false)) instanceof GensetPartBlockEntity tank) {
					tank.setFuel(2000);
				}
			}
		}
		if (drainSag && sag != null && !sag.hold().isEmpty()) {
			sag.hold().take(sag.hold().total());
		}
		if (++keeps % 10 == 0 && head != null) {
			double drawn = 0;
			for (PlantBlockEntity f : reclaim) {
				drawn += f.pit().drawnKg();
			}
			log(String.format(Locale.ROOT, "tick %.1f ms; head dropped %d; drawn %.0f t; pocket %d; entities %d", srv.getAverageTickTimeNanos() / 1e6, head.pit().dropped(), drawn / 1000,
					pocketBlocks(level), level.getAllEntities().spliterator().getExactSizeIfKnown()));
		}
	}

	private int keeps;

	private void ticks(int n) {
		for (int t = 0; t < n; t++) {
			if (t % 20 == 0) {
				server.runOnServer(this::keep);
			}
			context.waitTick();
		}
	}

	// ---- reading the plant --------------------------------------------------------------------------------------------------------

	private static String f0(double v) {
		return String.format(Locale.ROOT, "%,.0f", v);
	}

	private static String f1(double v) {
		return String.format(Locale.ROOT, "%,.1f", v);
	}

	private static double amps(PlantBlockEntity part) {
		double a = part.measure(Measurable.Quantity.AMPS);
		return Double.isNaN(a) ? 0 : a;
	}

	private static String state(PlantBlockEntity part) {
		String hoa = hoa(part);
		if (!hoa.equals("HAND") && !hoa.equals("AUTO")) {
			return hoa;
		}
		return part.running() ? "RUNNING, " + f0(amps(part)) + " A" : "stopped";
	}

	/** Each part's hand / off / auto selector, as this scene last turned it (every part starts in HAND). */
	private static final Map<BlockPos, String> HOA = new java.util.HashMap<>();

	private static String hoa(PlantBlockEntity part) {
		return HOA.getOrDefault(part.getBlockPos(), "HAND");
	}

	/** Turns its selector to {@code to}. */
	private static void setHoa(PlantBlockEntity part, String to) {
		for (int i = 0; i < 3; i++) {
			String now = part.cycleHoa(part.getBlockPos());
			HOA.put(part.getBlockPos().immutable(), now);
			if (to.equals(now)) {
				log(part.machine() + " selector to " + now);
				return;
			}
		}
		throw new AssertionError("could not turn " + part.machine() + " to " + to);
	}

	private DumpBlockEntity pocket(ServerLevel level) {
		return (DumpBlockEntity) BigParts.entity(level, at(0, 18, 0));
	}

	private void status(String what, MinecraftServer srv) {
		log(what + ":");
		for (BlockPos panel : panels) {
			log("  set " + panel.toShortString() + ": " + ((GensetControlPanelBlockEntity) srv.overworld().getBlockEntity(panel)).inspect().getString());
		}
		for (PlantBlockEntity p : new PlantBlockEntity[] {crusher, breaker, feeder, driveA, driveB, head, reclaim[0], reclaim[1], reclaim[2], weigher, sagDrive}) {
			if (p != null) {
				log("  " + p.machine() + " " + p.getBlockPos().toShortString() + ": " + p.status() + " | " + p.inspect().getString());
			}
		}
	}

	// ---- building -----------------------------------------------------------------------------------------------------------------

	private static BlockState facing(Block block, Direction facing) {
		return block.defaultBlockState().setValue(BigPartBlock.FACING, facing);
	}

	private static PlantBlockEntity plant(ServerLevel level, Machine machine, BlockPos main, Direction facing) {
		level.setBlock(main, facing(TailingsBlocks.plantBlock(machine), facing), Block.UPDATE_ALL);
		BigParts.grow(level, main, level.getBlockState(main));
		require(level.getBlockEntity(main) instanceof PlantBlockEntity, machine + " at " + main.toShortString());
		return (PlantBlockEntity) level.getBlockEntity(main);
	}

	/** The cell its motor box's cable plugs into. */
	private static BlockPos box(PlantBlockEntity part) {
		return part.getBlockPos().relative(TailingsPartBlock.boxFace(part.getBlockState()));
	}

	private static Block wc(String id) {
		return BuiltInRegistries.BLOCK.getValue(Identifier.parse(id));
	}

	/** The cells of a run through the corners, one axis at a time. */
	private static List<BlockPos> path(BlockPos... corners) {
		List<BlockPos> out = new ArrayList<>();
		out.add(corners[0]);
		for (int i = 1; i < corners.length; i++) {
			BlockPos at = corners[i - 1];
			BlockPos to = corners[i];
			while (!at.equals(to)) {
				if (at.getX() != to.getX()) {
					at = at.offset(Integer.signum(to.getX() - at.getX()), 0, 0);
				} else if (at.getY() != to.getY()) {
					at = at.offset(0, Integer.signum(to.getY() - at.getY()), 0);
				} else {
					at = at.offset(0, 0, Integer.signum(to.getZ() - at.getZ()));
				}
				out.add(at);
			}
		}
		return out;
	}

	private static void lay(ServerLevel level, String id, dev.wildcatter.energy.ConductorSize size, List<BlockPos> cells) {
		for (BlockPos cell : cells) {
			lay(level, id, size, cell);
		}
	}

	private static void lay(ServerLevel level, String id, dev.wildcatter.energy.ConductorSize size, BlockPos cell) {
		BlockState there = level.getBlockState(cell);
		if (there.getBlock() == wc(id)) {
			return;
		}
		require(there.canBeReplaced() || there.is(Blocks.SMOOTH_STONE) || there.is(Blocks.STONE) || there.is(Blocks.DIRT) || there.is(Blocks.GRASS_BLOCK),
				id + " at " + cell.toShortString() + " is blocked by " + there);
		level.setBlock(cell, wc(id).defaultBlockState(), Block.UPDATE_ALL);
		if (size != null && level.getBlockEntity(cell) instanceof dev.wildcatter.block.MultiCableBlockEntity cable) {
			cable.setSize(size);
		}
	}

	/** A set at {@code panel}, its skid west of it, facing north. Gas sets are 1 MW, diesel 500 kW, both at the industrial voltage. */
	private BlockPos set(ServerLevel level, BlockPos panel, boolean gas) {
		Trailer.fillBox(level, panel, Blocks.SMOOTH_STONE, -8, 19, -1, -1, -5, 9);
		Trailer.fillBox(level, panel, Blocks.AIR, -8, 19, 0, 6, -5, 9);
		List<BlockPos> overwrote = GensetSkid.build(level, panel, Direction.NORTH, gas);
		if (!overwrote.isEmpty()) {
			log("the set at " + panel.toShortString() + " overwrote " + overwrote.size() + " blocks");
		}
		panels.add(panel);
		return panel;
	}

	/** A unit substation run backward east of a gas set's panel: the industrial voltage in, the medium voltage out. Its MV cell. */
	private static BlockPos stepUp(ServerLevel level, BlockPos panel) {
		BlockState state = wc("wildcatter:unit_substation").defaultBlockState()
				.setValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.HORIZONTAL_FACING, Direction.SOUTH);
		BlockPos main = panel.east(4);
		level.setBlock(main, state, Block.UPDATE_ALL);
		BigParts.grow(level, main, state);
		return panel.east(5);
	}

	/** The site, the line except the primary's stack, the yard and every cable but the primary's. */
	private void stage() {
		server.runOnServer(srv -> {
			ServerLevel level = srv.overworld();
			BlockPos s = Trailer.site(level, 1200, -900, 140, 340);
			int g = s.getY();
			o = new BlockPos(s.getX() + 70, g, s.getZ() + 50);
			Trailer.pad(level, o.getX() - 70, o.getZ() - 50, o.getX() + 70, o.getZ() + 280, g, 16, 46);
			Trailer.forceChunks(level, o.getX() - 140, o.getZ() - 90, o.getX() + 90, o.getZ() + 300);
			// the trucks' bench north of the primary, as the pit line's scenes lay it: a quarried bench, its dump wall at the pocket's rim
			Trailer.hill(level, o.getX() - 40, o.getZ() - 50, o.getX() + 40, o.getZ() - 1, g, (x, z) -> {
				int dx = Math.max(Math.max(o.getX() - 2 - x, x - (o.getX() + 15)), 0);
				int dz = Math.max(z - (o.getZ() - 3), 0);
				int north = z - (o.getZ() - 50);
				return Math.min(26 - Math.max(dx, dz) * 2, north);
			}, true);
			Trailer.fillBox(level, o, Blocks.STONE, -2, 15, 0, 25, -40, -3);
			Trailer.fillBox(level, o, Blocks.STONE, -2, 15, 0, 21, -2, -1);
			Trailer.fillBox(level, o, Blocks.GRAVEL, -2, 15, 25, 25, -40, -3);
			// the stockpile conveyor: 150 m south off the feeder's head, 60 m flat then 90 m up at 1 in 3, 30 m up at its head
			int by = 0;
			for (int z = 15; z <= HEAD_Z; z++) {
				if (z > 74 && (z - 74) % 3 == 0) {
					by++;
				}
				level.setBlock(at(6, by, z), Trailer.heavyBelt(Direction.Axis.Z), Block.UPDATE_ALL);
			}
			require(by == HEAD_Y, "the stockpile conveyor should end 30 m up, got " + by);
			// its two drives side by side on the east of the run below the head, their boxes on their east faces
			driveA = plant(level, Machine.HEAVY_CONVEYOR_DRIVE, at(8, HEAD_Y, HEAD_Z), Direction.SOUTH);
			driveB = plant(level, Machine.HEAVY_CONVEYOR_DRIVE, at(8, 28, 160), Direction.SOUTH);
			head = plant(level, Machine.STOCKPILE_HEAD, at(5, HEAD_Y - 4, HEAD_Z + 1), Direction.NORTH);
			// the reclaim bench under the pile, the tunnel in it (cut open behind glass only for rtm01_drawdown)
			Trailer.fillBox(level, o, Blocks.SMOOTH_STONE, -30, 42, 0, 4, 140, 200);
			Trailer.fillBox(level, o, Blocks.AIR, 6, 9, 1, 3, 148, 200);
			for (int z = 150; z <= 198; z += 5) {
				level.setBlock(at(9, 3, z), Blocks.LIGHT.defaultBlockState(), Block.UPDATE_ALL);
			}
			// three reclaim feeders 6 m apart under the head, end to end, their feed ends (and the roof's openings) to the south
			int[] starts = {155, 161, 167};
			for (int i = 0; i < 3; i++) {
				reclaim[i] = plant(level, Machine.RECLAIM_FEEDER, at(7, 2, starts[i] + 5), Direction.SOUTH);
				Trailer.fillBox(level, o, Blocks.AIR, 6, 7, 4, 4, starts[i] + 5, starts[i] + 5);
			}
			// the SAG feed belt under them: along the tunnel and out, up 1 in 3 to the SAG's feed end; the weigher beside it
			int ry = 1;
			int z = 150;
			for (; ; z++) {
				if (z > 200 && (z - 200) % 3 == 0) {
					ry++;
				}
				level.setBlock(at(6, ry, z), Trailer.heavyBelt(Direction.Axis.Z), Block.UPDATE_ALL);
				if (ry == 13) {
					break;
				}
			}
			sagZ = z;
			sagY = ry;
			weigher = plant(level, Machine.BELT_WEIGHER, at(7, 1, 185), Direction.NORTH);
			// the drive touches the run's last cell (its head end), over the SAG's feed end
			sagDrive = plant(level, Machine.HEAVY_CONVEYOR_DRIVE, at(8, sagY, sagZ), Direction.SOUTH);
			Trailer.fillBox(level, o, Blocks.IRON_BARS, 8, 8, 0, sagY - 1, sagZ - 2, sagZ - 2);
			// the SAG mill on its slab at the belt's head, standing: it waits for episode 2
			Trailer.fillBox(level, o, Blocks.SMOOTH_STONE, -2, 14, 0, sagY - 13, sagZ, sagZ + 15);
			sag = plant(level, Machine.SAG_MILL, at(1, sagY - 11, sagZ), Direction.NORTH);
			// the yard. West: the gyratory's gas set and substation. East of the primary: the diesel set for its feeder and breaker.
			// East of the line: one gas set for the stockpile conveyor's two drives, a diesel set for the reclaim feeders, one gas set
			// for the SAG feed belt's drive. Each an island: no set runs in parallel.
			BlockPos g1 = set(level, at(-40, 0, 20), true);
			BlockPos gyroMv = stepUp(level, g1);
			gyratoryRun.addAll(path(at(3, 8, 2), at(2, 8, 2), at(2, 0, 2), at(2, 0, 20), gyroMv));
			set(level, at(32, 0, 10), false);
			BlockPos d = set(level, at(52, 0, 128), true);
			BlockPos dMv = stepUp(level, d);
			lay(level, MV, null, path(dMv, at(57, 0, 138), at(9, 0, 138), at(9, 28, 138), box(driveB), at(9, 28, 161), at(9, 30, 161), box(driveA)));
			BlockPos r = set(level, at(52, 0, 214), false);
			lay(level, LV, dev.wildcatter.energy.ConductorSize.AWG_4_0,
					path(box(reclaim[0]), box(reclaim[1]), box(reclaim[2]), at(8, 2, 200), at(8, 2, 202), at(8, 0, 202), at(53, 0, 202), r.east()));
			BlockPos sg = set(level, at(52, 0, sagZ + 6), true);
			BlockPos sgMv = stepUp(level, sg);
			lay(level, MV, null, path(sgMv, at(57, 0, sagZ + 6), at(57, 0, sagZ - 6), at(10, 0, sagZ - 6), at(10, sagY, sagZ - 6), at(10, sagY, sagZ), box(sagDrive)));
			// every motor stopped on its selector before the sets start; brought up one at a time
			for (PlantBlockEntity p : new PlantBlockEntity[] {driveA, driveB, sagDrive, reclaim[0], reclaim[1], reclaim[2]}) {
				setHoa(p, "OFF");
			}
			// a pile already standing under the head, in copper ore
			BlockPos tip = at(6, HEAD_Y - 5, HEAD_Z + 2);   // under the head's chute, as the head drops it
			for (int i = 0; i < PILE; i++) {
				dev.wildcatter.tailings.pit.Loose.muck(level, tip, TailingsBlocks.STOCKPILE_ROCK.defaultBlockState(), Map.of("minecraft:copper_ore", 3));
			}
			log("staged at " + o.toShortString() + "; the SAG belt ends at z " + sagZ + ", " + sagY + " m up; boxes: drive A " + box(driveA).toShortString()
					+ ", drive B " + box(driveB).toShortString() + ", SAG drive " + box(sagDrive).toShortString() + ", feeders " + box(reclaim[0]).toShortString());
		});
		// the sets start
		context.waitTicks(40);
		server.runOnServer(srv -> {
			for (BlockPos panel : panels) {
				((GensetControlPanelBlockEntity) srv.overworld().getBlockEntity(panel)).setWantRun(true);
			}
			keep(srv);
		});
		ticks(600);
		server.runOnServer(srv -> status("sets started", srv));
		// the SAG belt, then two reclaim feeders; the third is the spare
		server.runOnServer(srv -> setHoa(sagDrive, "HAND"));
		ticks(200);
		server.runOnServer(srv -> setHoa(reclaim[0], "HAND"));
		ticks(100);
		server.runOnServer(srv -> setHoa(reclaim[1], "HAND"));
		ticks(200);
		server.runOnServer(srv -> status("reclaim up", srv));
		server.runCommand("kill @e[type=item]");
	}

	// ---- 1. rtm01_ore_close: a copper ore block beside granite, pushed in on -----------------------------------------------------

	private void oreClose() {
		BlockPos[] w = new BlockPos[1];
		server.runOnServer(srv -> {
			ServerLevel level = srv.overworld();
			BlockPos b = at(-56, 0, 60);
			w[0] = b;
			// a broken rock face: stone with andesite, the copper ore block and a granite block side by side at eye height
			for (int x = -4; x <= 4; x++) {
				for (int yy = 0; yy <= 4; yy++) {
					for (int z = 0; z <= 2; z++) {
						Block k = Math.floorMod(x * 7 + yy * 13 + z * 5, 7) == 0 ? Blocks.ANDESITE : Blocks.STONE;
						level.setBlock(b.offset(x, yy, z), k.defaultBlockState(), Block.UPDATE_CLIENTS);
					}
				}
			}
			level.setBlock(b.offset(0, 1, 0), Blocks.COPPER_ORE.defaultBlockState(), Block.UPDATE_ALL);
			level.setBlock(b.offset(1, 1, 0), Blocks.GRANITE.defaultBlockState(), Block.UPDATE_ALL);
		});
		BlockPos b = w[0];
		Cam a = Cam.of(b, 0.9, 3.2, -7.0, 1.0, 1.5, 0.0);
		Cam z = Cam.of(b, 1.0, 1.7, -1.9, 1.0, 1.5, 0.0);
		film("rtm01_ore_close", 360, move(a, z, 360), null);
	}

	// ---- 2. rtm01_size_ladder: crushed, gravel, sand, fines in frames, the camera tracking along -------------------------------

	private void sizeLadder() {
		BlockPos[] w = new BlockPos[1];
		server.runOnServer(srv -> {
			ServerLevel level = srv.overworld();
			BlockPos b = at(-56, 0, 90);
			w[0] = b;
			Trailer.fillBox(level, b, Blocks.POLISHED_DEEPSLATE, -2, 12, 0, 4, 1, 1);
			String[] items = {"tailings:crushed_copper", "tailings:gravel_copper", "tailings:sand_copper", "tailings:fines_copper"};
			for (int i = 0; i < items.length; i++) {
				BlockPos at = b.offset(i * 3, 2, 0);
				var frame = new net.minecraft.world.entity.decoration.ItemFrame(level, at, Direction.NORTH);
				frame.setItem(new ItemStack(BuiltInRegistries.ITEM.getValue(Identifier.parse(items[i]))));
				level.addFreshEntity(frame);
			}
		});
		BlockPos b = w[0];
		Cam a = Cam.of(b, -1.0, 2.9, -2.3, -0.2, 2.5, 0.5);
		Cam z = Cam.of(b, 10.0, 2.9, -2.3, 9.2, 2.5, 0.5);
		film("rtm01_size_ladder", 400, move(a, z, 400), null);
	}

	/** Held at a for {@code hold} ticks, then eased to b by {@code move}. */
	private static IntFunction<Cam> holdThen(Cam a, Cam b, int hold, int move) {
		return t -> a.lerp(b, ease((double) (t - hold) / (move - hold)));
	}

	/** Through a, b and c: eased a to b over the first half, b to c over the second. */
	private static IntFunction<Cam> via(Cam a, Cam b, Cam c, int move) {
		return t -> t < move / 2 ? a.lerp(b, ease(t / (move / 2.0))) : b.lerp(c, ease((t - move / 2.0) / (move - move / 2.0)));
	}

	/** Round a centre at a radius and height, starting at an angle (degrees, 0 = south of it), {@code turn} degrees over the move. */
	private IntFunction<Cam> orbitAround(double cx, double cy, double cz, double r, double h, double from, double turn, int move) {
		return t -> {
			double a = Math.toRadians(from + turn * ease((double) t / move) * 0.15 + turn * ((double) t / move) * 0.85);
			return cam(cx + Math.sin(a) * r, cy + h, cz + Math.cos(a) * r, cx, cy, cz);
		};
	}

	// ---- 3. rtm01_build_stack: the primary placed part by part, apron feeder up to the dump pocket -----------------------------

	private void buildStack() {
		Cam a = cam(36, 16, -16, 6, 10, 6);
		Cam b = cam(32, 32, 24, 6, 14, 4);
		film("rtm01_build_stack", 420, move(a, b, 420), (level, t) -> {
			int s = t - PAD;
			if (s == 0) {
				feeder = plant(level, Machine.PRIMARY_APRON_FEEDER, at(5, 0, 3), Direction.NORTH);
				setHoa(feeder, "OFF");
				event("primary apron feeder placed");
			} else if (s == 80) {
				plant(level, Machine.SURGE_POCKET, at(3, 2, 1), Direction.NORTH);
				event("surge pocket placed");
			} else if (s == 160) {
				crusher = plant(level, Machine.GYRATORY_CRUSHER, at(4, 8, 2), Direction.NORTH);
				event("gyratory placed");
			} else if (s == 240) {
				level.setBlock(at(0, 18, 0), facing(TailingsBlocks.GYRATORY_POCKET, Direction.NORTH), Block.UPDATE_ALL);
				BigParts.grow(level, at(0, 18, 0), level.getBlockState(at(0, 18, 0)));
				level.setBlock(at(1, 22, -2), facing(TailingsBlocks.TRUCK_DUMP_WALL, Direction.NORTH), Block.UPDATE_ALL);
				BigParts.grow(level, at(1, 22, -2), level.getBlockState(at(1, 22, -2)));
				event("dump pocket and dump wall placed");
			} else if (s == 320) {
				breaker = plant(level, Machine.ROCK_BREAKER, at(12, 26, 2), Direction.NORTH);
				event("rock breaker placed");
			}
		});
	}

	/** The apron feeder's and the rock breaker's supply: the diesel set east of the primary, off camera. */
	private void wirePrimary() {
		server.runOnServer(srv -> {
			ServerLevel level = srv.overworld();
			BlockPos out = at(32, 0, 10).east();
			lay(level, LV, dev.wildcatter.energy.ConductorSize.KCMIL_500, path(out, at(33, 0, 0), at(4, 0, 0), box(feeder)));
			lay(level, LV, dev.wildcatter.energy.ConductorSize.AWG_4_0, path(box(breaker), at(11, 26, 1), at(14, 26, 1), at(14, 0, 1), at(14, 0, 0)));
			log("primary wired: feeder box " + box(feeder).toShortString() + ", breaker box " + box(breaker).toShortString() + ", gyratory box " + box(crusher).toShortString()
					+ " (run starts " + gyratoryRun.get(0).toShortString() + ")");
			require(box(crusher).equals(gyratoryRun.get(0)), "the gyratory's run should start at its box");
		});
		ticks(40);
	}

	// ---- 4. rtm01_build_power: the medium-voltage cable from the gyratory's west-face box back to its substation ----------------

	private void buildPower() {
		int n = gyratoryRun.size();
		int every = 3;
		int move = n * every + 60;
		Cam a = cam(-4, 10.5, -3, 3, 8.5, 2);
		Cam b = cam(-6, 14, 10, 0, 5, 6);
		Cam c = cam(-14, 24, 40, -30, 2, 18);
		showCard(true);
		film("rtm01_build_power", move, via(a, b, c, move), (level, t) -> {
			int s = t - PAD;
			if (s >= 0 && s % every == 0 && s / every < n) {
				lay(level, MV, null, gyratoryRun.get(s / every));
				if (s / every == n - 1) {
					event("cable reaches the substation");
				}
			}
			int laid = Math.max(0, Math.min(n, s < 0 ? 0 : s / every + 1));
			card("^GYRATORY MOTOR BOX: WEST FACE", "450 kW, the plant's medium voltage", laid < n ? "Cable laid: " + laid + " of " + n + " m" : "+Cable joined to the substation",
					"Motor: " + (laid < n ? "no supply" : crusher.running() ? "RUNNING" : "ready, idle (no rock yet)"));
		});
		showCard(false);
		server.runOnServer(srv -> status("gyratory wired", srv));
	}

	// ---- rtm01_tip: the ultra truck tips into the pocket (E1-01, 08, 11) -----------------------------------------------------------

	private int pocketBlocks(ServerLevel level) {
		DumpBlockEntity p = pocket(level);
		return p == null ? 0 : p.pocket();
	}

	private boolean pocketRoom(ServerLevel level) {
		DumpBlockEntity p = pocket(level);
		return p != null && (p.pocket() * (double) dev.wildcatter.tailings.pit.PitMass.ROCK_KG + 363_000) <= p.capacity() * (double) dev.wildcatter.tailings.pit.PitMass.ROCK_KG;
	}

	/** A loaded ultra at the dump wall, tipping when told. */
	private void spawnTruck(ServerLevel level) {
		if (truck == null || truck.isRemoved()) {
			truck = TailingsEntities.ULTRA_HAUL_TRUCK.create(level, EntitySpawnReason.COMMAND);
			truck.setPos(o.getX() + 7.0, o.getY() + 26, o.getZ() - 3.0 - truck.spec().length() / 2);
			truck.setYRot(180);
			level.addFreshEntity(truck);
		}
		truck.setFuel(truck.tank());
		if (truck.load().total() < 1) {
			truck.load().add("minecraft:copper_ore", 363_000);
			truck.refreshLoad();
		}
	}

	private void tipNow() {
		truck.testDrive(new Input(false, false, false, false, true, false, false));
	}

	private void tip() {
		server.runOnServer(srv -> {
			setHoa(crusher, "OFF");
			spawnTruck(srv.overworld());
		});
		ticks(40);
		showCard(true);
		Cam a = cam(34, 36, -24, 7, 23, -4);
		Cam b = cam(26, 31, -14, 7, 22, 0);
		int[] tipAt = {-1};
		film("rtm01_tip", 500, move(a, b, 500), (level, t) -> {
			int s = t - PAD;
			if (s == 20) {
				tipNow();
				tipAt[0] = t;
				event("truck tips");
			} else if (s == 30) {
				truck.testDrive(Input.EMPTY);
			}
			int blocks = pocketBlocks(level);
			card("^ULTRA TRUCK AT THE DUMP WALL", "Load: " + f0(truck.load().total() / 1000) + " t", "Pocket: " + blocks + " bank blocks, " + f0(blocks * 2.7) + " t",
					blocks > 0 && !pocketRoom(level) ? "!Next truck: FULL, it waits" : "Room for a whole load: tips");
		});
		showCard(false);
		server.runOnServer(srv -> {
			log("after the tip: pocket " + pocketBlocks(srv.overworld()) + " blocks, truck " + truck.load().total() + " kg left");
			truck.discard();   // it drives off back to the pit
		});
		tips++;
	}

	// ---- rtm01_start_seq: belt first, then the feeder and the crusher (E1-15, 17) -------------------------------------------------

	/** Running and through its start (no longer drawing its starting current). */
	private static boolean started(PlantBlockEntity part) {
		return part.running() && !part.inspect().getString().contains("starting");
	}

	private boolean beltUp() {
		return started(driveA) && started(driveB);
	}

	private void startSequence() {
		showCard(true);
		Cam a = cam(30, 22, 34, 6, 6, 12);
		Cam b = cam(26, 18, 28, 6, 8, 6);
		int[] up = {-1};
		film("rtm01_start_seq", 700, move(a, b, 700), (level, t) -> {
			int s = t - PAD;
			if (s == 20) {
				setHoa(driveA, "HAND");
				event("stockpile conveyor drive 1 to HAND");
			} else if (s > 40 && hoa(driveB).equals("OFF") && started(driveA)) {
				setHoa(driveB, "HAND");
				event("drive 1 through its start; drive 2 to HAND");
			}
			if (up[0] < 0 && s > 70 && beltUp()) {
				up[0] = s;
				event("stockpile conveyor RUNNING");
			}
			if (up[0] >= 0 && s == up[0] + 40) {
				setHoa(feeder, "HAND");
				setHoa(crusher, "HAND");
				event("apron feeder and gyratory to HAND");
			}
			card("^START-UP SEQUENCE", "1  Stockpile conveyor: " + (beltUp() ? "RUNNING" : hoa(driveA).equals("OFF") ? "OFF" : "starting"),
					"2  Apron feeder: " + state(feeder), "3  Gyratory: " + state(crusher));
		});
		showCard(false);
		server.runOnServer(srv -> status("line started", srv));
		server.runOnServer(srv -> require(beltUp() && crusher.running(), "the line should be up after the start-up sequence"));
	}

	// ---- rtm01_pocket_readout: the gyratory's readout counting the pocket down (E1-12) ---------------------------------------------

	private String[] gyratoryCard(ServerLevel level) {
		int blocks = pocketBlocks(level);
		String st = crusher.status();
		String head = st.startsWith("BRIDGED") ? "!BRIDGED: a boulder in the bowl" : st.startsWith("crushing") ? "+CRUSHING" : st.startsWith("FULL") ? "!SURGE POCKET FULL"
				: st.startsWith("NO FEED") ? "NO FEED: pocket empty" : st;
		return new String[] {"^PRIMARY GYRATORY", head, "Pocket: " + blocks + " bank blocks, " + f0(blocks * 2.7) + " t", "Motor: " + f0(amps(crusher)) + " A"};
	}

	private void pocketReadout() {
		showCard(true);
		Cam a = cam(20, 40, -10, 7, 24, 4);
		Cam b = cam(15, 36, -6, 7, 24, 4);
		film("rtm01_pocket_readout", 700, move(a, b, 700), (level, t) -> card(gyratoryCard(level)));
		showCard(false);
	}

	// ---- rtm01_wide: the primary and the stockpile conveyor climbing away (E1-02) ---------------------------------------------------

	private void wide() {
		Cam a = cam(44, 40, -26, 6, 10, 30);
		Cam b = cam(48, 34, 70, 6, 14, 160);
		film("rtm01_wide", 640, move(a, b, 640), null);
	}

	// ---- rtm01_gyratory_orbit: round the stack while it crushes (E1-06, 07) ----------------------------------------------------------

	private void orbit() {
		showCard(true);
		// the south half only: the trucks' bench stands against its north
		film("rtm01_gyratory_orbit", 900, orbitAround(7, 11, 5, 25, 6, -95, 190, 900), (level, t) -> card(gyratoryCard(level)));
		showCard(false);
	}

	// ---- rtm01_bridged: a boulder bridges the bowl; the rock breaker clears it (E1-13, 14) ------------------------------------------

	private void bridged() {
		showCard(true);
		int[] bridgedAt = {-1}, clearAt = {-1};
		Cam a = cam(26, 33, 12, 11, 26, 4);
		Cam b = cam(22, 31, 6, 10, 25, 4);
		film("rtm01_bridged", 1000, move(a, b, 1000), (level, t) -> {
			int s = t - PAD;
			if (s == 0) {
				crusher.pit().setSinceBoulder(dev.wildcatter.tailings.plant.PitLine.BOULDER_EVERY - 1);
				event("the next hard block is the 200th: a boulder");
			}
			boolean b0 = crusher.pit().bridged();
			if (b0 && bridgedAt[0] < 0) {
				bridgedAt[0] = s;
				event("BRIDGED");
			}
			if (!b0 && bridgedAt[0] >= 0 && clearAt[0] < 0) {
				clearAt[0] = s;
				event("cleared after " + (s - bridgedAt[0]) / 20.0 + " s");
			}
			String[] g = gyratoryCard(level);
			String br = breaker.status();
			card(g[0], g[1], br.startsWith("breaking") ? "Rock breaker: " + br.substring("breaking a boulder in the bowl, ".length()) : br.startsWith("broke") ? "+Rock breaker: boulder broken"
					: "Rock breaker: " + (breaker.running() ? "RUNNING" : "standing by"), "Rock breaker motor: " + f0(amps(breaker)) + " A");
		});
		showCard(false);
		require(bridgedAt[0] >= 0 && clearAt[0] > bridgedAt[0], "the crusher should bridge and the breaker clear it: " + bridgedAt[0] + ", " + clearAt[0]);
	}

	// ---- rtm01_belt_drives: the two drives under the head, then down the 150 m run (E1-16) ------------------------------------------

	private void beltDrives() {
		showCard(true);
		double kw = PitLine.heavyBeltWatts(150, HEAD_Y, PitLine.HEAVY_BELT_TPH) / 1000;
		int need = PitLine.heavyDrivesFor(150, HEAD_Y);
		Cam a = cam(18, 34, 170, 8, 29.5, 161);
		Cam b = cam(17, 40, 176, 6, 2, 30);
		film("rtm01_belt_drives", 760, holdThen(a, b, 240, 760), (level, t) -> card("^STOCKPILE CONVEYOR", "150 m long, lifting " + HEAD_Y + " m", "At 3,000 t/h: " + f0(kw) + " kW = " + need + " drives",
				"Drive 1: " + state(driveA), "Drive 2: " + state(driveB)));
		showCard(false);
	}

	// ---- rtm01_gallery: from the head's gallery down on the pile (E1-05, 19) ---------------------------------------------------------

	private void gallery() {
		Cam a = cam(16, 33, 158, 14, 8, 176);
		Cam b = cam(24, 31, 156, 12, 7, 174);
		film("rtm01_gallery", 560, move(a, b, 560), null);
	}

	// ---- rtm01_reclaim: in the tunnel, along the three feeders (E1-22) ----------------------------------------------------------------

	private String feederLine(int i) {
		PlantBlockEntity f = reclaim[i];
		String hoa = hoa(f);
		if (hoa.equals("OFF")) {
			return "Feeder " + (i + 1) + ": OFF" + (i == 2 ? " (spare)" : "");
		}
		return "Feeder " + (i + 1) + ": " + (f.running() && f.feederTph() > 0 ? f0(f.feederTph()) + " t/h" : f.status().startsWith("NO FEED") ? "NO FEED" : "stopped");
	}

	private void reclaimTunnel() {
		showCard(true);
		Cam a = cam(9.4, 3.0, 149, 7, 2.6, 175);
		Cam b = cam(9.4, 3.0, 162, 7, 2.6, 190);
		film("rtm01_reclaim", 560, move(a, b, 560), (level, t) -> card("^RECLAIM FEEDERS", feederLine(0), feederLine(1), feederLine(2)));
		showCard(false);
	}

	// ---- rtm01_weigher: one feeder stopped, the weigher reads 500 t/h (E1-23) ---------------------------------------------------------

	private String[] weigherCard() {
		return new String[] {"^BELT WEIGHER, MILL FEED", "Setpoint: " + f0(weigher.setpointTph()) + " t/h", "Rate: " + f0(weigher.pit().rateTph()) + " t/h", feederLine(0), feederLine(1), feederLine(2)};
	}

	private void weigherDrop() {
		showCard(true);
		Cam a = cam(9.5, 3.3, 178, 7.5, 1.4, 186);
		Cam b = cam(9.5, 3.0, 181, 7.5, 1.4, 187);
		film("rtm01_weigher", 640, move(a, b, 640), (level, t) -> {
			if (t - PAD == 160) {
				setHoa(reclaim[1], "OFF");
				event("feeder 2 to OFF");
			}
			card(weigherCard());
		});
		showCard(false);
		server.runOnServer(srv -> setHoa(reclaim[1], "HAND"));
	}

	private void weigherSetpoint() {
		server.runOnServer(srv -> weigher.pit().setSetpoint(600));
		ticks(300);
		showCard(true);
		Cam a = cam(9.5, 3.0, 181, 7.5, 1.4, 187);
		Cam b = cam(9.5, 3.4, 177, 7.0, 1.6, 184);
		film("rtm01_weigher_setpoint", 560, move(a, b, 560), (level, t) -> {
			if (t - PAD == 120) {
				weigher.pit().setSetpoint(1000);
				event("setpoint raised to 1,000 t/h");
			}
			card(weigherCard());
		});
		showCard(false);
	}

	// ---- rtm01_plc_points: each device's PLC points, live (E1-24) ---------------------------------------------------------------------

	private static String value(PlantBlockEntity d, int i, ControlDevice.Point p) {
		if (p.output()) {
			return "PLC out";
		}
		return switch (p.signal()) {
			case BOOL -> d.readPoint(i) ? "ON" : "off";
			case MA -> f1(d.readMa(i)) + " mA";
			case PULSE -> Long.toString(d.readPulses(i));
		};
	}

	private static String points(String name, PlantBlockEntity d) {
		StringBuilder b = new StringBuilder(name).append(":");
		List<ControlDevice.Point> ps = d.points();
		for (int i = 0; i < ps.size(); i++) {
			b.append(i == 0 ? " " : " · ").append(ps.get(i).name()).append(" ").append(value(d, i, ps.get(i)));
		}
		return b.toString();
	}

	private static List<String> pointLines(String title, PlantBlockEntity d) {
		List<String> out = new ArrayList<>(List.of("^" + title + "  [" + hoa(d) + "]"));
		List<ControlDevice.Point> ps = d.points();
		for (int i = 0; i < ps.size(); i++) {
			out.add(ps.get(i).name() + ": " + value(d, i, ps.get(i)));
		}
		return out;
	}

	private void plcPoints() {
		server.runOnServer(srv -> {
			log("points: " + points("weigher", weigher));
			log("points: " + points("feeder", reclaim[0]));
			log("points: " + points("gyratory", crusher));
		});
		showCard(true);
		Cam a = cam(9.5, 3.4, 176, 7.0, 1.8, 186);
		Cam b = cam(9.5, 3.0, 170, 7.0, 2.2, 160);
		film("rtm01_plc_points", 520, move(a, b, 520), (level, t) -> {
			List<String> l = new ArrayList<>(pointLines("BELT WEIGHER", weigher));
			l.addAll(pointLines("FEEDER 1", reclaim[0]));
			card(l.toArray(String[]::new));
		});
		Cam c = cam(24, 17, 12, 7, 12, 5);
		Cam d = cam(20, 15, 2, 7, 12, 5);
		film("rtm01_plc_points_gyratory", 420, move(c, d, 420), (level, t) -> card(pointLines("GYRATORY", crusher).toArray(String[]::new)));
		showCard(false);
	}

	// ---- rtm01_pile_growth: the cone growing side-on (time-lapse), then a crane up to the head (E1-18, 20) ----------------------------

	private int pileBlocks(ServerLevel level) {
		int n = 0;
		for (BlockPos p : BlockPos.betweenClosed(at(-30, 0, 130), at(45, HEAD_Y, 210))) {
			if (level.getBlockState(p).is(TailingsBlocks.STOCKPILE_ROCK)) {
				n++;
			}
		}
		return n;
	}

	private void pileGrowth() {
		server.runOnServer(srv -> {
			setHoa(reclaim[0], "OFF");
			setHoa(reclaim[1], "OFF");
			log("pile before growth: " + pileBlocks(srv.overworld()) + " blocks");
		});
		int[] start = {-1};
		Cam a = cam(34, 20, 150, 6, 17, 166);
		Cam b = cam(31, 19, 153, 6, 16, 166);
		film("rtm01_pile_growth", 900, move(a, b, 900), (level, t) -> {
			if (t % 40 == 0) {
				spawnTruck(level);
				if (pocketRoom(level) && truck.load().total() > 1) {
					tipNow();
					tips++;
					event("truck tips (load " + tips + ")");
				}
			} else if (t % 40 == 10) {
				truck.testDrive(Input.EMPTY);
			}
			if (t == 0) {
				start[0] = head.pit().dropped();
			}
		});
		server.runOnServer(srv -> {
			log("pile after growth: " + pileBlocks(srv.overworld()) + " blocks; the head dropped " + (head.pit().dropped() - start[0]) + " in the film");
			truck.discard();
		});
		Cam c = cam(44, 6, 182, 8, 10, 168);
		Cam d = cam(24, 36, 170, 6, 28, 165);
		film("rtm01_pile_crane", 600, move(c, d, 600), null);
	}

	// ---- rtm01_drawdown: three feeders draw funnels in the pile, seen through the glass (E1-21) ---------------------------------------

	private void drawdown() {
		int fast = Integer.parseInt(System.getenv().getOrDefault("RTM01_DRAW_FAST", "40"));
		int[] before = {0};
		server.runOnServer(srv -> {
			ServerLevel level = srv.overworld();
			// the cutaway: glass along the feeders' line, everything west of it (the pile's west half and the bench) taken away
			Trailer.fillBox(level, o, Blocks.GLASS, 5, 5, 1, HEAD_Y - 5, 136, 204);
			Trailer.fillBox(level, o, Blocks.AIR, -40, 4, 0, HEAD_Y, 130, 210);
			setHoa(feeder, "OFF");
			setHoa(crusher, "OFF");
			for (PlantBlockEntity f : reclaim) {
				setHoa(f, "HAND");
				f.fastCycle(fast);
			}
			sagDrive.fastCycle(fast);
			before[0] = pileBlocks(srv.overworld());
			log("pile before drawdown: " + before[0] + " blocks");
		});
		Cam fixed = cam(-24, 9, 167, 6, 7, 167);
		int[] noFeed = {-1};
		film("rtm01_drawdown", 1800, t -> fixed, (level, t) -> {
			if (noFeed[0] < 0 && t % 20 == 0 && reclaim[0].status().startsWith("NO FEED") && reclaim[1].status().startsWith("NO FEED") && reclaim[2].status().startsWith("NO FEED")) {
				noFeed[0] = t;
				event("all three feeders NO FEED");
			}
		});
		server.runOnServer(srv -> {
			int after = pileBlocks(srv.overworld());
			log(String.format(Locale.ROOT, "pile after drawdown: %d blocks; drawn %d of %d, %.0f %% live", after, before[0] - after, before[0], 100.0 * (before[0] - after) / before[0]));
			for (PlantBlockEntity f : reclaim) {
				f.fastCycle(1);
			}
			sagDrive.fastCycle(1);
		});
	}

	// ---- rtm01_sag_hook: the SAG mill at the end of the belt, standing (E1-26) ---------------------------------------------------------

	private void sagHook() {
		Cam a = cam(36, 28, sagZ - 26, 6, 8, sagZ + 6);
		Cam b = cam(20, 11, sagZ - 2, 6, 7, sagZ + 7);
		film("rtm01_sag_hook", 800, move(a, b, 800), null);
	}
}
