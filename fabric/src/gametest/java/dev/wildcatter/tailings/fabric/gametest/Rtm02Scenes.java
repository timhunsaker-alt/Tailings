package dev.wildcatter.tailings.fabric.gametest;

import dev.wildcatter.WildcatterBlocks;
import dev.wildcatter.block.BigPartBlock;
import dev.wildcatter.block.GasTurbineBlockEntity;
import dev.wildcatter.block.GeneratorPartBlockEntity;
import dev.wildcatter.block.GensetControlPanelBlockEntity;
import dev.wildcatter.block.GensetPartBlockEntity;
import dev.wildcatter.block.GensetSkid;
import dev.wildcatter.control.Measurable;
import dev.wildcatter.fluid.Product;
import dev.wildcatter.multiblock.BigParts;
import dev.wildcatter.tailings.TailingsBlocks;
import dev.wildcatter.tailings.block.PlantBlockEntity;
import dev.wildcatter.tailings.block.TailingsPartBlock;
import dev.wildcatter.tailings.block.stage.SagLineStages;
import dev.wildcatter.tailings.fabric.gametest.Trailer.Cam;
import dev.wildcatter.tailings.plant.Machine;
import dev.wildcatter.tailings.plant.PitLine;
import dev.wildcatter.tailings.plant.RockLoad;
import dev.wildcatter.tailings.rock.Rock;
import dev.wildcatter.tailings.rock.SizeClass;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.function.IntFunction;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
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
import net.minecraft.world.Container;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.Vec3;

/**
 * Rock to Metal, episode 2, "Grinding Rock with Rock" (TAILINGS_ONLY=rtm02): the pit line's SAG mill, pebble crusher, cyclone feed pump,
 * twelve-cyclone cluster and large ball mill, every motor on real power. The two mills' pinions are on the generator tier (films-rtm02.md
 * #1), each mill on its own gas turbine's bus duct; the pump, the pebble crusher and the belts on Wildcatter's gas sets through unit
 * substations run backward; the reclaim feeders on a diesel set. The stockpile, its tunnel and feeders and the SAG feed belt are as in
 * episode 1 (Rtm01Scenes). Filmed in real time at 2880 x 1620 with a clean view and a readout card at the top right, read from the
 * blocks each tick. RTM02_SHOTS=a,b films only those (the rest still run, unfilmed). Films to {@code fabric/build/scene-videos}.
 * <p>
 * Staged, and said so in the episode's notes: the SAG starts with its normal 83 t charge in it; the SAG, the pump, the small mill and its
 * cyclones get their water as if piped; the cyclones' overflow is drawn away as if to flotation (episode 5); the crushed pebbles land in a
 * chest that is emptied; the small mill is fed sand by hand; the turbines' purge and run-up (minutes) are skipped off camera.
 */
final class Rtm02Scenes {
	private static final int W = 1440, H = 810;
	private static final int PAD = 60;
	private static final String SEED = System.getenv().getOrDefault("TAILINGS_TRAILER_SEED", "tailings");
	private static final String MV = "wildcatter:cable_mv", LV = "wildcatter:cable_industrial", DUCT = "wildcatter:bus_duct_gen";
	private static final int PILE = Integer.parseInt(System.getenv().getOrDefault("RTM02_PILE", "2500"));
	/** The SAG feed belt's last cell, over the SAG's feed end; the SAG's base. */
	private static final int B = 2, Z0 = 200 + 3 * (B + 10);

	private final ClientGameTestContext context;
	private TestServerContext server;
	private BlockPos o;
	private PlantBlockEntity sag, ball, pump, cluster, pebble, sagDrive, pebbleDrive, returnDrive, weigher, smallMill, smallCyclone;
	private final PlantBlockEntity[] reclaim = new PlantBlockEntity[3];
	private BlockPos ta, tb, chest;
	private final List<BlockPos> panels = new ArrayList<>();
	private final List<BlockPos> tanks = new ArrayList<>();
	/** The last duct cell of each mill's second pinion, laid on camera (its breaker closing). */
	private BlockPos sagBox2, ballBox2;
	private final List<String> summary = new ArrayList<>();
	private final Set<BlockPos> boxCells = new HashSet<>();
	private final List<int[]> yards = new ArrayList<>();
	private long filmStart;
	/** Set by a beat to end the film's move early: the camera holds where it is, then the end's still. */
	private volatile boolean endMove;
	private int ft;
	private float lastYaw = Float.NaN;
	private int keeps;

	Rtm02Scenes(ClientGameTestContext context) {
		this.context = context;
	}

	private static void log(String line) {
		System.out.println("[rtm02] " + line);
	}

	private static void require(boolean ok, String what) {
		if (!ok) {
			throw new AssertionError("[rtm02] " + what);
		}
	}

	private static boolean wanted(String id) {
		String only = System.getenv("RTM02_SHOTS");
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
			s.setGameMode(net.minecraft.client.gui.screens.worldselection.WorldCreationUiState.SelectedGameMode.CREATIVE);
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
			cardWidth("Underflow to the ball mill: 2,500 t/h");
			try {
				stage();
				power();
				sagStart();
				ballStart();
				feedUp();
				sagWide();
				sagCharge();
				pebbles();
				cyclones();
				ballOrbit();
				overload();
				smallMill();
				turbines();
			} catch (RuntimeException | AssertionError ex) {
				log("FAILED: " + ex);
				ex.printStackTrace();
				throw ex;
			} finally {
				server.runCommand("tick rate 20");
				cardWidth("");
				context.runOnClient(client -> client.gui.setScreen(null));
				hud(true);
				log("summary:\n  " + String.join("\n  ", summary));
			}
		}
	}

	// ---- window, card, camera, films (as Rtm01Scenes) -----------------------------------------------------------------------------

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

	/** The card keeps one size through a film: as wide as this text. */
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

	/** Round a centre at a radius and height, from an angle (degrees, 0 = south of it), {@code turn} degrees over the move. */
	private IntFunction<Cam> orbitAround(double cx, double cy, double cz, double r, double h, double from, double turn, int move) {
		return t -> {
			double a = Math.toRadians(from + turn * ease((double) t / move) * 0.15 + turn * ((double) t / move) * 0.85);
			return cam(cx + Math.sin(a) * r, cy + h, cz + Math.cos(a) * r, cx, cy, cz);
		};
	}

	/**
	 * A film: {@link #PAD} ticks still, {@code move} ticks of camera path, {@link #PAD} still; {@code beat} every tick on the server.
	 * {@code until}, if set, ends the move early (checked each tick on the server) and holds the end.
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
		endMove = false;
		int frozen = -1;
		for (ft = 0; ft <= total; ft++) {
			if (endMove && frozen < 0 && ft > PAD) {
				frozen = Math.min(move, ft - PAD);
				total = ft + PAD;
			}
			int t = frozen >= 0 ? frozen : Math.max(0, Math.min(move, ft - PAD));
			cam(path.apply(t), false);
			int tick = ft;
			if (beat != null) {
				server.runOnServer(srv -> beat.at(srv.overworld(), tick));
			}
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
			context.waitTick();
		}
	}

	/** Waits up to {@code most} ticks for {@code done} (read on the server each second). */
	private boolean waitFor(int most, java.util.function.Predicate<ServerLevel> done) {
		for (int t = 0; t < most; t += 20) {
			if (server.computeOnServer(srv -> done.test(srv.overworld()))) {
				return true;
			}
			ticks(20);
		}
		return false;
	}

	/** Every second: fuel, water, the stand-ins for what lies outside this episode, and a log line every 10 s. */
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
		for (BlockPos t : tanks) {
			if (level.getBlockEntity(t) instanceof dev.wildcatter.block.StorageTankBlockEntity tank) {
				tank.testFill(Product.KEROSENE, 40_000);
			}
		}
		if (sag == null) {
			return;
		}
		sag.addWater(100_000);
		pump.addWater(100_000);
		cluster.out().give(Product.FINES_SLURRY, Double.MAX_VALUE);   // to flotation (episode 5)
		if (level.getBlockEntity(chest) instanceof Container c) {
			c.clearContent();   // the crushed pebbles, back to the SAG feed in a real plant
		}
		if (smallMill != null) {
			smallMill.addWater(2000);
			smallCyclone.addWater(2000);
			smallCyclone.out().give(Product.FINES_SLURRY, Double.MAX_VALUE);
			double room = Math.min(smallMill.room(), 14);   // 50 t/h of sand, about 14 kg a second
			if (room > 0) {
				RockLoad sand = new RockLoad();
				sand.add(Rock.COPPER, SizeClass.SAND, room);
				smallMill.receive(sand);
			}
		}
		if (++keeps % 10 == 0) {
			log(String.format(Locale.ROOT, "tick %.1f ms; SAG %s | %s; ball %s; pump %s; cluster %s; weigher %s",
					srv.getAverageTickTimeNanos() / 1e6, sag.status(), pinions(sag), ball.status(), pump.status(), cluster.status(), weigher.status()));
			log("  turbines: A " + turbine(level, ta) + "; B " + turbine(level, tb));
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

	/** One pinion's state from its readout: "running, 214 A", "starting, 1,286 A", "stopped", "breaker open". */
	private static String pinion(PlantBlockEntity mill, int which) {
		String s = mill.inspect().getString();
		int i = s.indexOf("pinion " + which + ": ");
		if (i < 0) {
			return "?";
		}
		String rest = s.substring(i + 10);
		int end = rest.indexOf("; pinion");
		if (end >= 0) {
			rest = rest.substring(0, end);
		}
		Matcher m = Pattern.compile("([0-9.]+) A").matcher(rest);
		String a = m.find() ? f0(Double.parseDouble(m.group(1))) + " A" : "";
		if (rest.startsWith("running")) {
			return "RUNNING, " + a;
		}
		if (rest.startsWith("starting")) {
			return "!STARTING, inrush " + a;
		}
		if (rest.startsWith("NO POWER")) {
			return "breaker open";
		}
		if (rest.startsWith("motor stopped")) {
			return "stopped";
		}
		return rest.length() > 30 ? rest.substring(0, 30) : rest;
	}

	private static String pinions(PlantBlockEntity mill) {
		return "pinion 1 " + pinion(mill, 1) + ", pinion 2 " + pinion(mill, 2);
	}

	private static GeneratorPartBlockEntity gen(ServerLevel level, BlockPos turbine) {
		return level.getBlockEntity(turbine.offset(4, 0, 1)) instanceof GeneratorPartBlockEntity g ? g : null;
	}

	private static String turbine(ServerLevel level, BlockPos turbine) {
		GeneratorPartBlockEntity g = gen(level, turbine);
		return g == null ? "no generator" : f1(g.deliveredWatts() / 1e6) + " MW, " + f1(g.hertz()) + " Hz";
	}

	private static double mw(ServerLevel level, BlockPos turbine) {
		GeneratorPartBlockEntity g = gen(level, turbine);
		return g == null ? 0 : g.deliveredWatts() / 1e6;
	}

	private static double charge(PlantBlockEntity mill) {
		return mill.hold().total() / 1000;
	}

	private static double filling(PlantBlockEntity mill) {
		return SagLineStages.filling(mill.hold().total()) * 100;
	}

	/** What the SAG grinds now, t/h: its rate at its charge, its pinions' output and its pebble crusher, as its stage computes it. */
	private double sagTph() {
		if (!sag.running()) {
			return 0;
		}
		return PitLine.sagGrindKg(sag.hold().total(), sag.strength(), !hoa(pebble).equals("OFF")) * 20 * 3.6;
	}

	/** The SAG's state, steady from tick to tick: a full pump box or pebble belt holds it back for a tick now and then. */
	private static String sagState(PlantBlockEntity mill) {
		if (mill.hold().total() > PitLine.SAG_OVERLOAD_KG) {
			return "!OVERLOAD";
		}
		return mill.running() ? "+GRINDING" : mill.status();
	}

	private static String hoa(PlantBlockEntity part) {
		return HOA.getOrDefault(part.getBlockPos(), "HAND");
	}

	private static final Map<BlockPos, String> HOA = new java.util.HashMap<>();

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

	private void status(String what, MinecraftServer srv) {
		ServerLevel level = srv.overworld();
		log(what + ":");
		for (BlockPos panel : panels) {
			log("  set " + panel.toShortString() + ": " + ((GensetControlPanelBlockEntity) level.getBlockEntity(panel)).inspect().getString());
		}
		for (BlockPos t : new BlockPos[] {ta, tb}) {
			if (t != null && level.getBlockEntity(t) instanceof GasTurbineBlockEntity gt) {
				log("  turbine " + t.toShortString() + ": " + gt.inspect().getString());
				log("    generator: " + (gen(level, t) == null ? "none" : gen(level, t).inspect().getString()));
			}
		}
		for (PlantBlockEntity p : new PlantBlockEntity[] {sag, ball, pump, cluster, pebble, sagDrive, pebbleDrive, returnDrive, weigher, reclaim[0], reclaim[1], reclaim[2], smallMill, smallCyclone}) {
			if (p != null) {
				log("  " + p.machine() + " " + p.getBlockPos().toShortString() + ": " + p.status() + " | " + p.inspect().getString());
			}
		}
	}

	// ---- building -----------------------------------------------------------------------------------------------------------------

	private static BlockState facing(Block block, Direction facing) {
		return block.defaultBlockState().setValue(BigPartBlock.FACING, facing);
	}

	private PlantBlockEntity plant(ServerLevel level, Machine machine, BlockPos main, Direction facing) {
		level.setBlock(main, facing(TailingsBlocks.plantBlock(machine), facing), Block.UPDATE_ALL);
		BigParts.grow(level, main, level.getBlockState(main));
		require(level.getBlockEntity(main) instanceof PlantBlockEntity, machine + " at " + main.toShortString());
		PlantBlockEntity p = (PlantBlockEntity) level.getBlockEntity(main);
		if (machine.watts() > 0) {
			boxCells.add(box(p));
			if (machine.motors() == 2) {
				boxCells.add(box2(p));
			}
		}
		return p;
	}

	private static void big(ServerLevel level, Block block, BlockPos main, Direction facing) {
		BlockState state = block.defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, facing);
		level.setBlock(main, state, Block.UPDATE_ALL);
		BigParts.grow(level, main, state);
	}

	private static BlockPos box(PlantBlockEntity part) {
		return part.getBlockPos().relative(TailingsPartBlock.boxFace(part.getBlockState()));
	}

	/** A twin-pinion mill's second box: 4 m south of the first (the parts here face north). */
	private static BlockPos box2(PlantBlockEntity part) {
		return box(part).south(4);
	}

	private static Block wc(String id) {
		return BuiltInRegistries.BLOCK.getValue(Identifier.parse(id));
	}

	private static boolean free(BlockState s) {
		return s.canBeReplaced() || s.is(Blocks.SMOOTH_STONE) || s.is(Blocks.STONE) || s.is(Blocks.DIRT) || s.is(Blocks.GRASS_BLOCK) || s.is(Blocks.GRAVEL) || s.is(Blocks.SAND)
				|| s.is(Blocks.ANDESITE) || s.is(Blocks.DIORITE) || s.is(Blocks.GRANITE) || s.is(Blocks.TUFF) || s.is(Blocks.DEEPSLATE) || s.is(Blocks.CLAY) || s.is(Blocks.COARSE_DIRT);
	}

	private static void lay(ServerLevel level, String id, dev.wildcatter.energy.ConductorSize size, BlockPos cell) {
		BlockState there = level.getBlockState(cell);
		if (there.getBlock() == wc(id)) {
			return;
		}
		require(free(there), id + " at " + cell.toShortString() + " is blocked by " + there);
		level.setBlock(cell, wc(id).defaultBlockState(), Block.UPDATE_ALL);
		if (size != null && level.getBlockEntity(cell) instanceof dev.wildcatter.block.MultiCableBlockEntity cable) {
			cable.setSize(size);
		}
	}

	private static void lay(ServerLevel level, String id, dev.wildcatter.energy.ConductorSize size, List<BlockPos> cells) {
		for (BlockPos cell : cells) {
			lay(level, id, size, cell);
		}
	}

	/** The cells of a run through the corners, one axis at a time (x, then y, then z). */
	private static List<BlockPos> path(BlockPos... corners) {
		List<BlockPos> out = new ArrayList<>();
		out.add(corners[0]);
		for (int i = 1; i < corners.length; i++) {
			BlockPos a = corners[i - 1];
			BlockPos to = corners[i];
			while (!a.equals(to)) {
				if (a.getX() != to.getX()) {
					a = a.offset(Integer.signum(to.getX() - a.getX()), 0, 0);
				} else if (a.getY() != to.getY()) {
					a = a.offset(0, Integer.signum(to.getY() - a.getY()), 0);
				} else {
					a = a.offset(0, 0, Integer.signum(to.getZ() - a.getZ()));
				}
				out.add(a);
			}
		}
		return out;
	}

	/**
	 * A run of {@code id} from {@code from} to any cell in {@code goals}, found through free cells (fewest turns, then shortest, then low):
	 * never through another part's motor box, never beside a run of the same kind that is not in {@code own} (that would join two islands),
	 * never into a yard but its own. Returns the cells, {@code from} first.
	 */
	private List<BlockPos> route(ServerLevel level, String id, BlockPos from, Set<BlockPos> goals, Set<BlockPos> own) {
		int minX = from.getX(), maxX = from.getX(), minY = from.getY(), maxY = from.getY(), minZ = from.getZ(), maxZ = from.getZ();
		for (BlockPos g : goals) {
			minX = Math.min(minX, g.getX());
			maxX = Math.max(maxX, g.getX());
			minY = Math.min(minY, g.getY());
			maxY = Math.max(maxY, g.getY());
			minZ = Math.min(minZ, g.getZ());
			maxZ = Math.max(maxZ, g.getZ());
		}
		minX -= 12;
		maxX += 12;
		minZ -= 12;
		maxZ += 12;
		minY = Math.max(o.getY() - 8, minY - 2);
		maxY += 6;
		int nx = maxX - minX + 1, ny = maxY - minY + 1, nz = maxZ - minZ + 1;
		int n = nx * ny * nz;
		Block kind = wc(id);
		byte[] ok = new byte[n];   // 0 unknown, 1 free, 2 blocked
		int[] dist = new int[n * 6];
		java.util.Arrays.fill(dist, Integer.MAX_VALUE);
		int[] prev = new int[n * 6];
		PriorityQueue<long[]> q = new PriorityQueue<>((a, b) -> Long.compare(a[0], b[0]));
		Direction[] dirs = Direction.values();
		final int fx = minX, fy = minY, fz = minZ;
		java.util.function.IntFunction<BlockPos> pos = i -> new BlockPos(fx + i % nx, fy + (i / nx) % ny, fz + i / (nx * ny));
		java.util.function.Function<BlockPos, Integer> idx = p -> (p.getX() - fx) + nx * ((p.getY() - fy) + ny * (p.getZ() - fz));
		java.util.function.IntPredicate passable = i -> {
			if (ok[i] == 0) {
				BlockPos p = pos.apply(i);
				boolean good = free(level.getBlockState(p)) && !(boxCells.contains(p) && !goals.contains(p) && !p.equals(from));
				for (int[] y : yards) {
					if (p.getX() >= y[0] && p.getX() <= y[1] && p.getZ() >= y[2] && p.getZ() <= y[3] && !goals.contains(p)) {
						good = false;
					}
				}
				if (good) {
					for (Direction d : dirs) {
						BlockPos q2 = p.relative(d);
						if (level.getBlockState(q2).getBlock() == kind && !own.contains(q2) && !goals.contains(q2)) {
							good = false;
							break;
						}
					}
				}
				ok[i] = (byte) (good ? 1 : 2);
			}
			return ok[i] == 1;
		};
		int start = idx.apply(from);
		for (int d = 0; d < 6; d++) {
			dist[start * 6 + d] = 0;
			prev[start * 6 + d] = -1;
			q.add(new long[] {0, start * 6L + d});
		}
		int found = -1;
		while (!q.isEmpty()) {
			long[] e = q.poll();
			int s = (int) e[1];
			if (e[0] > dist[s]) {
				continue;
			}
			int cell = s / 6, dir = s % 6;
			if (goals.contains(pos.apply(cell))) {
				found = s;
				break;
			}
			BlockPos p = pos.apply(cell);
			for (int d = 0; d < 6; d++) {
				BlockPos q2 = p.relative(dirs[d]);
				if (q2.getX() < minX || q2.getX() > maxX || q2.getY() < minY || q2.getY() > maxY || q2.getZ() < minZ || q2.getZ() > maxZ) {
					continue;
				}
				int c2 = idx.apply(q2);
				if (!goals.contains(q2) && !passable.test(c2)) {
					continue;
				}
				int cost = 10 + (d == dir || cell == start ? 0 : 40) + (q2.getY() > o.getY() ? 2 : 0);
				int nd = dist[s] + cost;
				int s2 = c2 * 6 + d;
				if (nd < dist[s2]) {
					dist[s2] = nd;
					prev[s2] = s;
					q.add(new long[] {nd, s2});
				}
			}
		}
		require(found >= 0, "no free route for " + id + " from " + from.toShortString() + " to " + goals.iterator().next().toShortString());
		ArrayDeque<BlockPos> cells = new ArrayDeque<>();
		for (int s = found; s >= 0; s = prev[s]) {
			cells.addFirst(pos.apply(s / 6));
			if (s / 6 == start) {
				break;
			}
		}
		return new ArrayList<>(cells);
	}

	/** Lays a run from {@code from} to {@code to} (or to {@code own}'s cells if {@code to} is null), all but its first {@code hold} cells; returns all its cells. */
	private List<BlockPos> run(ServerLevel level, String id, BlockPos from, BlockPos to, Set<BlockPos> own, int hold) {
		Set<BlockPos> goals = new HashSet<>();
		if (to != null) {
			goals.add(to);
		} else {
			for (BlockPos c : own) {
				for (Direction d : Direction.values()) {
					if (free(level.getBlockState(c.relative(d))) && !own.contains(c.relative(d))) {
						goals.add(c.relative(d));
					}
				}
			}
		}
		require(free(level.getBlockState(from)) || level.getBlockState(from).getBlock() == wc(id), id + " cannot start at " + from.toShortString() + ": " + level.getBlockState(from));
		List<BlockPos> cells = route(level, id, from, goals, own);
		for (int i = hold; i < cells.size(); i++) {
			lay(level, id, null, cells.get(i));
		}
		own.addAll(cells);
		log(id + " run of " + cells.size() + " m from " + from.toShortString() + " to " + cells.get(cells.size() - 1).toShortString());
		return cells;
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
		yards.add(new int[] {panel.getX() - 8, panel.getX(), panel.getZ() - 5, panel.getZ() + 9});   // the skid; its outputs are east of it
		return panel;
	}

	/** A unit substation run backward east of a set's panel: the industrial voltage in, the medium voltage out. Its MV cell. */
	private static BlockPos stepUp(ServerLevel level, BlockPos panel) {
		BlockState state = wc("wildcatter:unit_substation").defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.SOUTH);
		BlockPos main = panel.east(4);
		level.setBlock(main, state, Block.UPDATE_ALL);
		BigParts.grow(level, main, state);
		return panel.east(5);
	}

	/**
	 * A 16.5 MW gas turbine set as gas-turbine.md lays it, facing north, at {@code t} (the package x 0-3, z 0-9): inlet house north, generator
	 * east (x 4-7, z 1-6) with its breaker behind (x 5-6, z 7-8), the simple-cycle stack south, a kerosene tank west on two lengths of line.
	 * Its starter's box is at x -1, z 2; its bus duct leaves the breaker's back at x 5, z 9.
	 */
	private void turbineSet(ServerLevel level, BlockPos t) {
		Trailer.fillBox(level, t, Blocks.SMOOTH_STONE, -8, 9, -1, -1, -6, 14);
		Trailer.fillBox(level, t, Blocks.AIR, -8, 9, 0, 13, -6, 14);
		big(level, WildcatterBlocks.GAS_TURBINE, t, Direction.NORTH);
		big(level, WildcatterBlocks.GT_INLET_HOUSE, t.offset(0, 0, -4), Direction.NORTH);
		big(level, WildcatterBlocks.GT_GENERATOR, t.offset(4, 0, 1), Direction.NORTH);
		big(level, WildcatterBlocks.GEN_BREAKER, t.offset(5, 0, 7), Direction.NORTH);
		big(level, WildcatterBlocks.GT_EXHAUST_STACK, t.offset(0, 0, 10), Direction.NORTH);
		big(level, WildcatterBlocks.STOCK_TANK, t.offset(-6, 0, 4), Direction.NORTH);
		Trailer.pipe(level, t.offset(-2, 0, 6), t.offset(-1, 0, 6));
		tanks.add(t.offset(-6, 0, 4));
		boxCells.add(t.offset(-1, 0, 2));
	}

	/** The site, the stockpile and feed belt as episode 1's, the SAG line, the yard and every run but the second pinions' last cells. */
	private void stage() {
		server.runOnServer(srv -> {
			ServerLevel level = srv.overworld();
			BlockPos s = Trailer.site(level, -1600, 1400, 220, 360);
			int g = s.getY();
			o = new BlockPos(s.getX() + 110, g, s.getZ() - 120);
			Trailer.pad(level, o.getX() - 100, o.getZ() + 130, o.getX() + 125, o.getZ() + 360, g, 16, 46);
			Trailer.forceChunks(level, o.getX() - 110, o.getZ() + 120, o.getX() + 135, o.getZ() + 370);
			// the reclaim bench and tunnel, as episode 1's; three feeders 6 m apart
			Trailer.fillBox(level, o, Blocks.SMOOTH_STONE, -30, 42, 0, 4, 140, 200);
			Trailer.fillBox(level, o, Blocks.AIR, 6, 9, 1, 3, 148, 200);
			for (int z = 150; z <= 198; z += 5) {
				level.setBlock(at(9, 3, z), Blocks.LIGHT.defaultBlockState(), Block.UPDATE_ALL);
			}
			int[] starts = {155, 161, 167};
			for (int i = 0; i < 3; i++) {
				reclaim[i] = plant(level, Machine.RECLAIM_FEEDER, at(7, 2, starts[i] + 5), Direction.SOUTH);
				Trailer.fillBox(level, o, Blocks.AIR, 6, 7, 4, 4, starts[i] + 5, starts[i] + 5);
			}
			// the SAG feed belt: along the tunnel and out, up 1 in 3 to 17 m over the SAG's feed end; the weigher beside it
			int ry = 1;
			int z = 150;
			for (; ; z++) {
				if (z > 200 && (z - 200) % 3 == 0) {
					ry++;
				}
				level.setBlock(at(6, ry, z), Trailer.heavyBelt(Direction.Axis.Z), Block.UPDATE_ALL);
				if (ry == B + 11) {
					break;
				}
			}
			require(z == Z0, "the SAG feed belt should end at z " + Z0 + ", got " + z);
			weigher = plant(level, Machine.BELT_WEIGHER, at(7, 1, 185), Direction.NORTH);
			sagDrive = plant(level, Machine.HEAVY_CONVEYOR_DRIVE, at(8, ry, Z0), Direction.SOUTH);
			Trailer.fillBox(level, o, Blocks.IRON_BARS, 8, 8, 0, ry - 1, Z0 - 2, Z0 - 2);
			// the SAG line on its slab (as sag-mill-line.md and the p7_line scene), the cluster on the ball mill's east face so both of the
			// ball mill's motor boxes on its west face stay free
			Trailer.fillBox(level, o, Blocks.SMOOTH_STONE, -2, 27, 0, B - 2, Z0, Z0 + 37);
			Trailer.fillBox(level, o, Blocks.AIR, 17, 22, B - 6, B - 2, Z0 + 8, Z0 + 19);   // the pebble crusher's pit, dug in
			sag = plant(level, Machine.SAG_MILL, at(1, B, Z0), Direction.NORTH);
			pump = plant(level, Machine.CYCLONE_FEED_PUMP, at(4, B, Z0 + 14), Direction.NORTH);
			ball = plant(level, Machine.BALL_MILL_LARGE, at(11, B, Z0 + 20), Direction.NORTH);
			cluster = plant(level, Machine.CYCLONE_CLUSTER_LARGE, at(20, B, Z0 + 20), Direction.NORTH);
			// the ball mill's discharge back to the pump's east side
			Trailer.pipe(level, at(11, B, Z0 + 36), at(11, B - 1, Z0 + 36), at(10, B - 1, Z0 + 36), at(9, B - 1, Z0 + 36), at(8, B - 1, Z0 + 36));
			for (int zz = Z0 + 35; zz >= Z0 + 16; zz--) {
				Trailer.pipe(level, at(8, B - 1, zz));
			}
			Trailer.pipe(level, at(8, B, Z0 + 16));
			// the cyclone feed, a metre up, from the pump's south face along the ball mill's feed end to the cluster's north face
			for (int x = 5; x <= 20; x++) {
				Trailer.pipe(level, at(x, B + 1, Z0 + 19));
			}
			// the overflow (fines slurry) away south from the cluster
			for (int zz = Z0 + 26; zz <= Z0 + 30; zz++) {
				Trailer.pipe(level, at(22, B, zz));
			}
			// the pebbles: a heavy belt off the SAG's east face over the pebble crusher in its pit, the crushed pebbles south on a belt to a bin
			for (int x = 12; x <= 17; x++) {
				level.setBlock(at(x, B, Z0 + 10), Trailer.heavyBelt(Direction.Axis.X), Block.UPDATE_ALL);
			}
			pebbleDrive = plant(level, Machine.HEAVY_CONVEYOR_DRIVE, at(16, B + 1, Z0 + 8), Direction.NORTH);
			pebble = plant(level, Machine.PEBBLE_CRUSHER, at(18, B - 5, Z0 + 9), Direction.NORTH);
			for (int zz = Z0 + 12; zz <= Z0 + 18; zz++) {
				level.setBlock(at(19, B - 6, zz), Trailer.heavyBelt(Direction.Axis.Z), Block.UPDATE_ALL);
			}
			returnDrive = plant(level, Machine.HEAVY_CONVEYOR_DRIVE, at(17, B - 6, Z0 + 16), Direction.NORTH);   // west of the belt's head, its box west of it
			chest = at(19, B - 6, Z0 + 19);
			level.setBlock(chest, Blocks.CHEST.defaultBlockState(), Block.UPDATE_ALL);
			// the SAG starts with its normal charge in it, as a mill that has run
			RockLoad charge = new RockLoad();
			charge.add(Rock.COPPER, SizeClass.CRUSHED, PitLine.SAG_NORMAL_CHARGE_KG);
			sag.receive(charge);
			// the stockpile over the tunnel, in copper ore
			BlockPos tip = at(6, 25, 166);
			for (int i = 0; i < PILE; i++) {
				dev.wildcatter.tailings.pit.Loose.muck(level, tip, TailingsBlocks.STOCKPILE_ROCK.defaultBlockState(), Map.of("minecraft:copper_ore", 3));
			}
			for (PlantBlockEntity p : new PlantBlockEntity[] {sag, ball, pump, pebble, sagDrive, pebbleDrive, returnDrive, reclaim[0], reclaim[1], reclaim[2]}) {
				setHoa(p, "OFF");
			}
			log("staged at " + o.toShortString() + "; SAG boxes " + box(sag).toShortString() + ", " + box2(sag).toShortString() + "; ball boxes " + box(ball).toShortString() + ", "
					+ box2(ball).toShortString() + "; pump box " + box(pump).toShortString() + "; pebble crusher box " + box(pebble).toShortString() + "; drives " + box(sagDrive).toShortString()
					+ ", " + box(pebbleDrive).toShortString() + ", " + box(returnDrive).toShortString());
		});
		server.runCommand("kill @e[type=item]");
	}

	/** The yard: two turbine sets west, their starters' diesel set, gas sets for the pump, the pebble crusher and the belts, a diesel set for the feeders. */
	private void power() {
		server.runOnServer(srv -> {
			ServerLevel level = srv.overworld();
			ta = at(-46, 0, Z0 - 6);
			tb = at(-46, 0, Z0 + 22);
			turbineSet(level, ta);
			turbineSet(level, tb);
			// the turbines' starters: a diesel set and a unit substation run backward, 4.16 kV to each starter's box
			BlockPos d = set(level, at(-80, 0, Z0 + 8), false);
			BlockPos dMv = stepUp(level, d);
			Set<BlockPos> starter = new HashSet<>();
			run(level, MV, ta.offset(-1, 0, 2), dMv, starter, 0);
			run(level, MV, tb.offset(-1, 0, 2), null, starter, 0);
			// turbine A's bus duct to the SAG's two boxes; turbine B's to the ball mill's. The second pinion's last cell waits.
			Set<BlockPos> busA = new HashSet<>();
			run(level, DUCT, box(sag), ta.offset(5, 0, 9), busA, 0);
			List<BlockPos> a2 = run(level, DUCT, box2(sag), null, busA, 1);
			sagBox2 = a2.get(0);
			Set<BlockPos> busB = new HashSet<>();
			run(level, DUCT, box(ball), tb.offset(5, 0, 9), busB, 0);
			List<BlockPos> b2 = run(level, DUCT, box2(ball), null, busB, 1);
			ballBox2 = b2.get(0);
			// gas sets: one for the pump, one for the pebble crusher and the pebble belts, one for the SAG feed belt's drive
			BlockPos g1 = set(level, at(-30, 0, Z0 + 62), true);
			run(level, MV, box(pump), stepUp(level, g1), new HashSet<>(), 0);
			BlockPos g2 = set(level, at(56, 0, Z0 + 10), true);
			Set<BlockPos> pebbleNet = new HashSet<>();
			run(level, MV, box(pebble), stepUp(level, g2), pebbleNet, 0);
			run(level, MV, box(returnDrive), null, pebbleNet, 0);
			// each motor draws its rating while it runs: the pebble belt's drive on a set of its own
			BlockPos g5 = set(level, at(56, 0, Z0 + 40), true);
			run(level, MV, box(pebbleDrive), stepUp(level, g5), new HashSet<>(), 0);
			BlockPos g3 = set(level, at(56, 0, Z0 - 6), true);
			run(level, MV, box(sagDrive), stepUp(level, g3), new HashSet<>(), 0);
			// the reclaim feeders on a diesel set, as episode 1's
			BlockPos r = set(level, at(52, 0, 214 - 8), false);
			lay(level, LV, dev.wildcatter.energy.ConductorSize.AWG_4_0,
					path(box(reclaim[0]), box(reclaim[1]), box(reclaim[2]), at(8, 2, 200), at(8, 2, 202), at(8, 0, 202), at(53, 0, 202), r.east()));
		});
		context.waitTicks(40);
		server.runOnServer(srv -> {
			for (BlockPos panel : panels) {
				((GensetControlPanelBlockEntity) srv.overworld().getBlockEntity(panel)).setWantRun(true);
			}
			keep(srv);
		});
		ticks(400);
		// the turbines start one after the other on the starter set; their purge and run-up (minutes) are skipped, off camera
		for (BlockPos t : new BlockPos[] {ta, tb}) {
			server.runOnServer(srv -> ((GasTurbineBlockEntity) srv.overworld().getBlockEntity(t)).setWantRun(true));
			ticks(200);
			server.runOnServer(srv -> ((GasTurbineBlockEntity) srv.overworld().getBlockEntity(t)).skipStartForTest());
			ticks(200);
		}
		server.runOnServer(srv -> status("sets and turbines started", srv));
		// the pump first, the pebble crusher and its belts, then the ball mill and the SAG wait for their films
		server.runOnServer(srv -> setHoa(pump, "HAND"));
		ticks(100);
		server.runOnServer(srv -> setHoa(returnDrive, "HAND"));
		ticks(200);
		server.runOnServer(srv -> setHoa(pebbleDrive, "HAND"));
		ticks(200);
		server.runOnServer(srv -> setHoa(pebble, "HAND"));
		ticks(100);
		server.runOnServer(srv -> status("ready to start the mills", srv));
	}

	// ---- rtm02_motor_start: the SAG's two pinions start on turbine A ---------------------------------------------------------------

	private String[] startCard(ServerLevel level, String title, PlantBlockEntity mill, BlockPos turbine, String tname) {
		return new String[] {"^" + title, "Turbine " + tname + ": " + turbine(level, turbine), "Pinion 1: " + pinion(mill, 1), "Pinion 2: " + pinion(mill, 2)};
	}

	private void sagStart() {
		showCard(true);
		Cam a = cam(-17, 12, Z0 - 9, 0, 4, Z0 + 4);
		Cam b = cam(-12, 9, Z0 - 5, 0, 4, Z0 + 4);
		int[] up1 = {-1}, closed = {-1};
		film("rtm02_motor_start", 1000, move(a, b, 1000), (level, t) -> {
			int s = t - PAD;
			if (s == 40) {
				setHoa(sag, "HAND");
				event("SAG to HAND: pinion 1 starts");
			}
			if (up1[0] < 0 && s > 40 && pinion(sag, 1).startsWith("RUNNING")) {
				up1[0] = s;
				event("pinion 1 RUNNING");
			}
			if (up1[0] >= 0 && closed[0] < 0 && s >= up1[0] + 200) {
				closed[0] = s;
				lay(level, DUCT, null, sagBox2);
				event("pinion 2's breaker closes: it starts");
			}
			if (closed[0] >= 0 && pinion(sag, 2).startsWith("RUNNING") && s % 20 == 0) {
				if (closed[0] > 0) {
					event("pinion 2 RUNNING");
					closed[0] = 0;
				}
			}
			card(startCard(level, "STARTING THE SAG MILL", sag, ta, "A"));
		});
		showCard(false);
		server.runOnServer(srv -> {
			if (srv.overworld().getBlockState(sagBox2).getBlock() != wc(DUCT)) {
				lay(srv.overworld(), DUCT, null, sagBox2);
			}
			status("SAG started", srv);
		});
	}

	private void ballStart() {
		boolean fed = waitFor(2400, level -> ball.back().held(Product.MILL_DISCHARGE) > 1000);
		log("ball mill underflow waiting before its film: " + fed);
		showCard(true);
		Cam a = cam(-15, 9, Z0 + 33, 10, 4, Z0 + 23);
		Cam b = cam(-11, 7, Z0 + 30, 10, 4, Z0 + 23);
		int[] up1 = {-1}, closed = {-1};
		film("rtm02_ball_start", 900, move(a, b, 900), (level, t) -> {
			int s = t - PAD;
			if (s == 40) {
				setHoa(ball, "HAND");
				event("ball mill to HAND: pinion 1 starts when the underflow comes");
			}
			if (up1[0] < 0 && s > 40 && pinion(ball, 1).startsWith("RUNNING")) {
				up1[0] = s;
				event("pinion 1 RUNNING");
			}
			if (up1[0] >= 0 && closed[0] < 0 && s >= up1[0] + 200) {
				closed[0] = s;
				lay(level, DUCT, null, ballBox2);
				event("pinion 2's breaker closes");
			}
			card(startCard(level, "STARTING THE BALL MILL", ball, tb, "B"));
		});
		showCard(false);
		server.runOnServer(srv -> {
			if (srv.overworld().getBlockState(ballBox2).getBlock() != wc(DUCT)) {
				lay(srv.overworld(), DUCT, null, ballBox2);
			}
			status("ball mill started", srv);
		});
	}

	/** The SAG feed belt and two feeders, to the weigher's 1,000 t/h; then the line settles off camera. */
	private void feedUp() {
		server.runOnServer(srv -> setHoa(sagDrive, "HAND"));
		ticks(200);
		server.runOnServer(srv -> setHoa(reclaim[0], "HAND"));
		ticks(100);
		server.runOnServer(srv -> setHoa(reclaim[1], "HAND"));
		ticks(1200);
		boolean back = waitFor(9000, level -> charge(sag) >= PitLine.SAG_NORMAL_CHARGE_KG / 1000 - 3);
		log("SAG charge back to normal before the films: " + back + ", " + server.computeOnServer(srv -> f1(charge(sag))) + " t");
		server.runOnServer(srv -> status("line up", srv));
	}

	// ---- rtm02_sag_wide: down the feed belt to the SAG and the ball mill -------------------------------------------------------------

	private void sagWide() {
		Cam a = cam(40, 36, Z0 - 40, 8, 6, Z0 + 10);
		Cam b = cam(42, 22, Z0 + 2, 10, 5, Z0 + 18);
		Cam c = cam(30, 12, Z0 + 46, 10, 5, Z0 + 18);
		film("rtm02_sag_wide", 900, via(a, b, c, 900), null);
	}

	// ---- rtm02_sag_charge: close on the turning shell, the charge read off the mill ----------------------------------------------------

	private String[] chargeCard(ServerLevel level) {
		int weightPoint = -1;
		var ps = sag.points();
		for (int i = 0; i < ps.size(); i++) {
			if (ps.get(i).name().startsWith("WEIGHT")) {
				weightPoint = i;
			}
		}
		return new String[] {"^SAG MILL, 9.75 m x 4.6 m", sagState(sag), "Rock charge: " + f0(charge(sag)) + " t, " + f0(filling(sag)) + " % full with the balls",
				"Normal " + f0(PitLine.SAG_NORMAL_CHARGE_KG / 1000) + " t · OVERLOAD over " + f0(PitLine.SAG_OVERLOAD_KG / 1000) + " t",
				"WEIGHT to the PLC: " + (weightPoint < 0 ? "?" : f1(sag.readMa(weightPoint)) + " mA"), "Grinding: " + f0(sagTph()) + " t/h"};
	}

	private void sagCharge() {
		showCard(true);
		Cam a = cam(25, 11, Z0 + 1, 6, 6, Z0 + 7);
		Cam b = cam(22, 8, Z0 + 10, 6, 6, Z0 + 7);
		film("rtm02_sag_charge", 800, move(a, b, 800), (level, t) -> card(chargeCard(level)));
		showCard(false);
	}

	// ---- rtm02_pebbles: the pebble belt and crusher; the crusher stopped, the SAG's rate falls -------------------------------------

	private void pebbles() {
		showCard(true);
		Cam a = cam(30, 10, Z0 + 2, 16, 0, Z0 + 11);
		Cam b = cam(28, 7, Z0 + 5, 17, 0, Z0 + 11);
		film("rtm02_pebbles", 1000, move(a, b, 1000), (level, t) -> {
			int s = t - PAD;
			if (s == 360) {
				setHoa(pebble, "OFF");
				event("pebble crusher to OFF: BYPASSED");
			}
			card("^PEBBLE CRUSHER", hoa(pebble).equals("OFF") ? "!BYPASSED: pebbles go round it" : "+CRUSHING pebbles to gravel", "Pebbles: " + f0(PitLine.PEBBLE_SHARE * 100) + " % of the SAG's feed", "SAG grinding: " + f0(sagTph()) + " t/h");
		});
		showCard(false);
		server.runOnServer(srv -> setHoa(pebble, "HAND"));
		ticks(400);
	}

	// ---- rtm02_cyclones: the twelve-cyclone cluster, its feed pressure, overflow and underflow ---------------------------------------

	private void cyclones() {
		showCard(true);
		Cam a = cam(34, 12, Z0 + 12, 22, 5, Z0 + 22);
		Cam b = cam(31, 9, Z0 + 32, 22, 5, Z0 + 23);
		film("rtm02_cyclones", 900, move(a, b, 900), (level, t) -> {
			double feed = cluster.pit().rateTph();
			double over = feed / (1 + PitLine.CIRCULATING_LOAD);
			card("^CYCLONE CLUSTER: 12 x 660 mm", "Feed pressure: " + f0(SagLineStages.pressureKpa(feed)) + " kPa", "Feed: " + f0(feed) + " t/h of solids",
					"Overflow, under 150 µm: " + f0(over) + " t/h", "Underflow to the ball mill: " + f0(feed - over) + " t/h");
		});
		showCard(false);
	}

	// ---- rtm02_ball_orbit: round the ball mill while it grinds ------------------------------------------------------------------------

	private void ballOrbit() {
		showCard(true);
		film("rtm02_ball_orbit", 1100, orbitAround(15.5, 6, Z0 + 28, 26, 8, 80, -140, 1100), (level, t) -> card("^BALL MILL, 7.3 m x 11 m", ball.status().startsWith("grinding") ? "+GRINDING the underflow" : ball.status(),
				"Pinion 1: " + pinion(ball, 1), "Pinion 2: " + pinion(ball, 2), "Turbine B: " + turbine(level, tb)));
		showCard(false);
	}

	// ---- rtm02_overload: fed at 1,500 t/h the charge climbs past 120 t; OVERLOAD holds the weigher back; the feed is backed off ---------

	private String[] overloadCard() {
		return new String[] {"^SAG MILL WEIGHT", sagState(sag), "Rock charge: " + f1(charge(sag)) + " t (OVERLOAD over " + f0(PitLine.SAG_OVERLOAD_KG / 1000) + " t)",
				"Weigher setpoint: " + f0(weigher.pit().ownSetpoint()) + " t/h" + (weigher.setpointTph() < weigher.pit().ownSetpoint() - 1 ? ", held to " + f0(weigher.setpointTph()) : ""),
				"Feed on the belt: " + f0(weigher.pit().rateTph()) + " t/h", "Grinding: " + f0(sagTph()) + " t/h"};
	}

	private void overload() {
		server.runOnServer(srv -> {
			setHoa(reclaim[2], "HAND");
			weigher.pit().setSetpoint(1500);
			log("setpoint 1,500 t/h, three feeders: the charge climbs");
		});
		boolean near = waitFor(9000, level -> charge(sag) >= PitLine.SAG_OVERLOAD_KG / 1000 - 3.5);
		log("charge before the film: " + server.computeOnServer(srv -> f1(charge(sag))) + " t (" + (near ? "near OVERLOAD" : "did not get there") + ")");
		showCard(true);
		Cam a = cam(-9, 13, Z0 - 9, 6, 6, Z0 + 5);
		Cam b = cam(-6, 11, Z0 - 6, 6, 6, Z0 + 5);
		int[] over = {-1}, backed = {-1}, clear = {-1};
		film("rtm02_overload", 4000, move(a, b, 4000), (level, t) -> {
			int s = t - PAD;
			boolean ol = sag.status().startsWith("OVERLOAD");
			if (ol && over[0] < 0) {
				over[0] = s;
				event("OVERLOAD at " + f1(charge(sag)) + " t; weigher held to " + f0(weigher.setpointTph()));
			}
			if (over[0] >= 0 && backed[0] < 0 && s >= over[0] + 240) {
				backed[0] = s;
				weigher.pit().setSetpoint(750);
				event("feed backed off: setpoint to 750 t/h");
			}
			if (backed[0] >= 0 && clear[0] < 0 && !ol) {
				clear[0] = s;
				event("OVERLOAD cleared at " + f1(charge(sag)) + " t");
			}
			if (clear[0] >= 0 && s >= clear[0] + 200) {
				endMove = true;
			}
			card(overloadCard());
		});
		showCard(false);
		server.runOnServer(srv -> {
			setHoa(reclaim[2], "OFF");
			weigher.pit().setSetpoint(500);
		});
		waitFor(9000, level -> charge(sag) < PitLine.SAG_NORMAL_CHARGE_KG / 1000 + 5);   // the feed stays backed off until the charge is back to normal
		server.runOnServer(srv -> weigher.pit().setSetpoint(1000));
	}

	// ---- rtm02_small_mill: the 50 t/h plant's 3.2 m ball mill and its cyclones, for scale ----------------------------------------------

	private final BlockPos[] smallSets = new BlockPos[2];

	private void smallMill() {
		BlockPos[] m = new BlockPos[1];
		server.runOnServer(srv -> {
			ServerLevel level = srv.overworld();
			BlockPos mp = at(70, 0, 300);
			m[0] = mp;
			Trailer.fillBox(level, mp, Blocks.SMOOTH_STONE, -6, 8, -1, -1, -4, 10);
			smallMill = plant(level, Machine.BALL_MILL, mp, Direction.NORTH);
			smallCyclone = plant(level, Machine.CYCLONE, mp.offset(-2, 0, 2), Direction.NORTH);
			// the mill's discharge round its east side and north end to the cyclones' north face; the underflow back by touching
			Trailer.pipe(level, mp.offset(3, 0, 6), mp.offset(4, 0, 6));
			for (int zz = 5; zz >= -1; zz--) {
				Trailer.pipe(level, mp.offset(4, 0, zz));
			}
			for (int x = 3; x >= -2; x--) {
				Trailer.pipe(level, mp.offset(x, 0, -1));
			}
			Trailer.pipe(level, mp.offset(-2, 0, 0), mp.offset(-2, 0, 1));
			BlockPos g4 = set(level, at(100, 0, 300), true);
			run(level, MV, box(smallMill), stepUp(level, g4), new HashSet<>(), 0);
			BlockPos d3 = set(level, at(100, 0, 322), false);
			run(level, LV, box(smallCyclone), d3.east(), new HashSet<>(), 0);
			m[0] = mp;
			smallSets[0] = g4;
			smallSets[1] = d3;
		});
		context.waitTicks(40);
		server.runOnServer(srv -> {
			for (BlockPos p : smallSets) {
				((GensetControlPanelBlockEntity) srv.overworld().getBlockEntity(p)).setWantRun(true);
			}
			keep(srv);
		});
		ticks(600);
		server.runOnServer(srv -> status("small mill up", srv));
		showCard(true);
		BlockPos mp = m[0];
		film("rtm02_small_mill", 700, orbitAround(mp.getX() - o.getX() + 1, 2, mp.getZ() - o.getZ() + 3, 13, 5, 120, 70, 700),
				(level, t) -> card("^50 t/h PLANT: BALL MILL", "3.2 m x 4.5 m, 600 kW", smallMill.running() ? "+RUNNING, " + f0(amps(smallMill)) + " A" : smallMill.status(),
						"Cyclones: 4 x 250 mm, 45 kW"));
		showCard(false);
	}

	// ---- rtm02_turbines: the two turbine sets carrying the mills ------------------------------------------------------------------------

	private void turbines() {
		showCard(true);
		Cam a = cam(-20, 30, Z0 - 30, -42, 4, Z0 + 10);
		Cam b = cam(-14, 26, Z0 + 48, -42, 4, Z0 + 16);
		film("rtm02_turbines", 900, move(a, b, 900), (level, t) -> card("^THE MILLS' POWER", "Turbine A, the SAG mill: " + f1(mw(level, ta)) + " MW",
				"Turbine B, the ball mill: " + f1(mw(level, tb)) + " MW", "Two 16.5 MW gas turbine sets"));
		showCard(false);
		server.runOnServer(srv -> status("end", srv));
	}
}
