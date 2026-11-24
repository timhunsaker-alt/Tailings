package dev.wildcatter.tailings.fabric.gametest;

import dev.wildcatter.block.BigPartBlock;
import dev.wildcatter.block.DayTankBlockEntity;
import dev.wildcatter.block.GensetControlPanelBlockEntity;
import dev.wildcatter.block.GensetPartBlockEntity;
import dev.wildcatter.block.GensetSkid;
import dev.wildcatter.control.Measurable;
import dev.wildcatter.energy.ConductorSize;
import dev.wildcatter.fluid.Product;
import dev.wildcatter.fluid.SlurryAssay;
import dev.wildcatter.multiblock.BigParts;
import dev.wildcatter.tailings.TailingsBlocks;
import dev.wildcatter.tailings.TailingsEntities;
import dev.wildcatter.tailings.block.MuckBlockEntity;
import dev.wildcatter.tailings.block.PlantBlockEntity;
import dev.wildcatter.tailings.block.SurveyStakeBlock;
import dev.wildcatter.tailings.block.TailingsPartBlock;
import dev.wildcatter.tailings.entity.HaulTruckEntity;
import dev.wildcatter.tailings.entity.PitVehicleEntity;
import dev.wildcatter.tailings.entity.TrackDozerEntity;
import dev.wildcatter.tailings.entity.UltraHaulTruckEntity;
import dev.wildcatter.tailings.fabric.gametest.Trailer.Cam;
import dev.wildcatter.tailings.pit.DryStack;
import dev.wildcatter.tailings.pit.PitMass;
import dev.wildcatter.tailings.pit.StopKind;
import dev.wildcatter.tailings.plant.Machine;
import dev.wildcatter.tailings.plant.PlantRates;
import dev.wildcatter.tailings.plant.Reagent;
import dev.wildcatter.tailings.rock.Mineral;
import dev.wildcatter.tailings.rock.Rock;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;
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
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.Vec3;

/**
 * Heavy Iron, episode 4, "Dozers and the dry stack" (TAILINGS_ONLY=hi04): the track dozer (docs/multiblocks/open-pit-p4-dry-stack.md)
 * at a waste dump and on the plant's dry stack.
 * <ul>
 * <li>the waste dump (origin o): a dump top of tipped waste, its crest along z 0 with a 10 m face to the north; a lane of soil 3 m deep
 * over rock where the dozer cuts to a 10 % grade until the rock stops it; a pile of waste tipped short of the crest that it pushes over;
 * a loaded ultra truck on a self-driving route to a DUMP POINT at the crest with no berm (NO BERM); the dozer takes a windrow of waste
 * into its blade and drops a 2 m berm along the crest a column a metre; the ultra backs to it and tips over it, down the face;</li>
 * <li>the dry stack (origin q, Rtm08's P4 layout): tails (as from the plant) into the tailings thickener, its underflow up to the
 * tailings filter's batch press over the load-out bin and frame, on a Wildcatter diesel set; a self-driving 100 t truck through the
 * load-out, filled under the gate, round its loop to a DUMP POINT on the stack's lift (a paddock); it tips; the dozer, set to SPREAD,
 * pushes the load to the lift's edge and lays it as a 1 m lift;</li>
 * <li>the dozer and the trucks burn their own diesel (tanks filled at the start); the press, the thickener and the gate run on the set;
 * the dozer is driven by the scene's keys, as a player drives it, except in hi04_auto, where it runs itself on its autopilot in an
 * area marked by two DOZE AREA stakes; a villager stands at its blade for scale;</li>
 * <li>Tim's decisions of 2026-11-23: hi04_auto (the autopilot spreading and compacting), hi04_slump (a dozer pass compacts the lift's
 * edge; a layer laid on it stands, on the loose edge beside it slumps), hi04_bin (the bin's level in its windows falling as the gate
 * fills a truck); the press drops only full 15 t batches (the game holds it at its fill now, not the scene).</li>
 * </ul>
 * Filmed at 2880 x 1620 with a readout card read each tick. HI04_SHOTS=a,b films only those.
 */
final class Hi04Scenes {
	private static final int W = 1440, H = 810;
	private static final int PAD = 60;
	private static final String SEED = System.getenv().getOrDefault("TAILINGS_TRAILER_SEED", "tailings");
	private static final String LV = "wildcatter:cable_industrial", BUS = "wildcatter:busway_industrial", MV = "wildcatter:cable_mv";
	private static final String WATER = "wildcatter:pipe";
	/** The waste dump's floor, first air, 10 m under the dump top's first air (o's y). */
	private static final int DF = -10;
	/** The dry stack, x from o: q = o + (QX, 0, 0). */
	private static final int QX = 170;
	/** The ultra's DUMP POINT at the crest (x, z) and its turn stake; the dozer runs along the crest at z BERM_Z, its centre 6.5 m in. */
	private static final int SX = 10, SZ = 2, TZ = 50;
	private static final double BERM_Z = 6.5;
	/** The cut lane: soil 3 m deep over rock, x 58-68, z 26-90; the dozer cuts south along x 63. */
	private static final int CX = 63;
	/** The stack's lift: dry tails 1 m thick, x -15..45, z -36..-11 of q; its edge at z -37 over the ground a metre lower. */
	private static final int LIFT_EDGE = -37;

	private static final Input FORWARD = new Input(true, false, false, false, false, false, false);
	private static final Input FORWARD_LEFT = new Input(true, false, true, false, false, false, false);
	private static final Input FORWARD_RIGHT = new Input(true, false, false, true, false, false, false);

	private final ClientGameTestContext context;
	private TestServerContext server;
	private BlockPos o, q;
	private ConductorSize runSize = ConductorSize.KCMIL_500;
	private final List<BlockPos> panels = new ArrayList<>();
	private final List<String> summary = new ArrayList<>();
	private final Set<BlockPos> boxCells = new HashSet<>();
	private final List<int[]> yards = new ArrayList<>();
	private long filmStart;
	private volatile boolean endMove;
	private int ft;
	private float lastYaw = Float.NaN;
	private int keeps;
	private volatile boolean filming;

	private PlantBlockEntity s1, thick, filter, frame, bin;
	private final List<BlockPos> tanks = new ArrayList<>();
	private double drained;
	private UUID dozerId, ultraId, truckId;
	private final List<BlockPos> ultraRoute = new ArrayList<>(), truckRoute = new ArrayList<>();
	private double lastFuel = Double.NaN;
	private double engineKw;
	private String lastStatus = "";
	private String note = "";

	private static final Mineral CU = Mineral.COPPER;
	/** Copper in a kg of the fines slurry (as Rtm08Scenes.RICH); the tails carry the quarter flotation leaves. */
	private static final double RICH = Rock.COPPER.contained(CU).doubleValue() * CU.kgPerUnit() / (Reagent.BLOCK_TONNES.doubleValue() * 1000 / PlantRates.OVERFLOW_SOLIDS);

	Hi04Scenes(ClientGameTestContext context) {
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
			cardWidth("Now: pushing up a berm 2 m along the crest: 18 b");
			try {
				stage();
				power();
				vehicles();
				scaleFilm();
				tracksFilm();
				cutFilm();
				pushFilm();
				noBermFilm();
				bermFilm();
				tipFilm();
				dumpWideFilm();
				thickenerFilm();
				loadoutFilm();
				stackFilm();
				spreadFilm();
				slumpFilm();
				autoFilm();
				binFilm();
				powerFilm();
				pressFilm();
				dropFilm();
				stackWideFilm();
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

	private Cam dcam(double x, double y, double z, double ax, double ay, double az) {
		return Cam.of(o, x, y, z, ax, ay, az);
	}

	private Cam qcam(double x, double y, double z, double ax, double ay, double az) {
		return Cam.of(q, x, y, z, ax, ay, az);
	}

	private BlockPos d(int x, int y, int z) {
		return q.offset(x, y, z);
	}

	private static void log(String line) {
		System.out.println("[hi04] " + line);
	}

	private static void require(boolean ok, String what) {
		if (!ok) {
			throw new AssertionError("[hi04] " + what);
		}
	}

	private static boolean wanted(String id) {
		String only = System.getenv("HI04_SHOTS");
		return only == null || only.isBlank() || java.util.Arrays.asList(only.split(",")).contains(id);
	}

	// ---- window, card, camera, films (as Hi03Scenes) ------------------------------------------------------------------------------

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

	/** A camera beside an entity, eased toward where it is: {@code side} metres to its right (negative: left), {@code back} behind, {@code up}. */
	/** The entity a follow camera is on, and where it was at the last server tick (kept by film()'s beat, so no frame waits on the server). */
	private volatile UUID followId;
	private volatile double[] followAt;

	private IntFunction<Cam> follow(UUID id, double side, double back, double up, double lookUp) {
		double[] at = {Double.NaN, 0, 0, 0, 0, 0};
		followId = id;
		followAt = server.computeOnServer(srv -> srv.overworld().getEntity(id) instanceof PitVehicleEntity v
				? new double[] {v.getX(), v.getY(), v.getZ(), v.getYRot()} : null);
		return t -> {
			double[] e = followAt;
			if (e == null) {
				return new Cam(at[0], at[4], at[1], at[2], at[5], at[3]);
			}
			double yaw = Math.toRadians(e[3]);
			double fx = -Math.sin(yaw), fz = Math.cos(yaw);   // its heading
			double rx = -Math.cos(yaw), rz = -Math.sin(yaw);   // its right
			double cx = e[0] - fx * back + rx * side, cz = e[2] - fz * back + rz * side;
			if (Double.isNaN(at[0])) {
				at[0] = cx;
				at[1] = cz;
				at[2] = e[0];
				at[3] = e[2];
			}
			at[0] += (cx - at[0]) * 0.03;
			at[1] += (cz - at[1]) * 0.03;
			at[2] += (e[0] - at[2]) * 0.1;
			at[3] += (e[2] - at[3]) * 0.1;
			at[4] = e[1] + up;
			at[5] = e[1] + lookUp;
			return new Cam(at[0], at[4], at[1], at[2], at[5], at[3]);
		};
	}

	private void film(String id, int move, IntFunction<Cam> path, Beat beat) {
		boolean want = wanted(id);
		for (String mob : new String[] {"cow", "pig", "sheep", "chicken", "horse", "donkey", "rabbit", "llama", "goat", "fox", "wolf", "cat", "frog", "bee", "armadillo", "turtle", "parrot"}) {
			server.runCommand("kill @e[type=minecraft:" + mob + "]");
		}
		cam(path.apply(0), true);
		if (want) {
			settle(40);
			cam(path.apply(0), true);
			context.waitTicks(5);
		}
		hideCursor();
		Filming film = want ? context.computeOnClient(client -> Filming.start(client, id)) : null;
		filmStart = System.nanoTime();
		filming = true;
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
			server.runOnServer(srv -> {
				if (beat != null) {
					beat.at(srv.overworld(), tick);
				}
				UUID f = followId;
				if (f != null && srv.overworld().getEntity(f) instanceof PitVehicleEntity v) {
					followAt = new double[] {v.getX(), v.getY(), v.getZ(), v.getYRot()};
				}
			});
			if (ft % 10 == 0) {
				hideCursor();
			}
			if (ft % 20 == 0) {
				server.runOnServer(this::keep);
			}
			context.waitTick();
		}
		filming = false;
		followId = null;
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

	private static double amps(PlantBlockEntity part) {
		double a = part.measure(Measurable.Quantity.AMPS);
		return Double.isNaN(a) ? 0 : a;
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

	// ---- building (as Rtm08Scenes) ------------------------------------------------------------------------------------------------

	private static BlockState facing(Block block, Direction facing) {
		return block.defaultBlockState().setValue(BigPartBlock.FACING, facing);
	}

	private PlantBlockEntity plant(ServerLevel level, Machine machine, BlockPos main) {
		level.setBlock(main, facing(TailingsBlocks.plantBlock(machine), Direction.NORTH), Block.UPDATE_ALL);
		BigParts.grow(level, main, level.getBlockState(main));
		require(level.getBlockEntity(main) instanceof PlantBlockEntity, machine + " at " + main.toShortString());
		PlantBlockEntity p = (PlantBlockEntity) level.getBlockEntity(main);
		if (machine.watts() > 0) {
			boxCells.add(box(p));
		}
		return p;
	}

	private static BlockPos box(PlantBlockEntity part) {
		return part.getBlockPos().relative(TailingsPartBlock.boxFace(part.getBlockState()));
	}

	private static Block wc(String id) {
		return BuiltInRegistries.BLOCK.getValue(Identifier.parse(id));
	}

	private static boolean free(BlockState s) {
		return s.canBeReplaced() || s.is(Blocks.SMOOTH_STONE) || s.is(Blocks.STONE) || s.is(Blocks.DIRT) || s.is(Blocks.GRASS_BLOCK) || s.is(Blocks.GRAVEL) || s.is(Blocks.SAND)
				|| s.is(Blocks.ANDESITE) || s.is(Blocks.DIORITE) || s.is(Blocks.GRANITE) || s.is(Blocks.TUFF) || s.is(Blocks.DEEPSLATE) || s.is(Blocks.CLAY) || s.is(Blocks.COARSE_DIRT);
	}

	private static void lay(ServerLevel level, String id, ConductorSize size, BlockPos cell) {
		BlockState there = level.getBlockState(cell);
		if (there.getBlock() == wc(id)) {
			return;
		}
		require(free(there), id + " at " + cell.toShortString() + " is blocked by " + there);
		level.setBlock(cell, wc(id).defaultBlockState(), Block.UPDATE_ALL);
		if (size != null && level.getBlockEntity(cell) instanceof dev.wildcatter.block.MultiCableBlockEntity cable) {
			cable.setSize(size);
		}
		// a conductor set in its armless default state sets itself again with its arms, with no neighbour update: tell the run
		level.updateNeighborsAt(cell, wc(id), null);
	}

	/** The truck's road and its balloon loop at the dry stack, under 11 m: no cable runs there. */
	private boolean noGo(BlockPos c) {
		if (c.getY() >= q.getY() + 11) {
			return false;
		}
		int x = c.getX() - q.getX(), z = c.getZ() - q.getZ();
		return x >= 13 && x <= 26 && z >= -40 && z <= 118 || x >= -10 && x <= 26 && z >= 64 && z <= 122;
	}

	/** As Rtm08Scenes.route: a run through free cells (fewest turns, then shortest, then low), clear of other runs of its kind. */
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
		minY = Math.max(q.getY() - 8, minY - 2);
		maxY += 6;
		int nx = maxX - minX + 1, ny = maxY - minY + 1, nz = maxZ - minZ + 1;
		int n = nx * ny * nz;
		Block kind = wc(id);
		byte[] ok = new byte[n];
		int[] dist = new int[n * 6];
		java.util.Arrays.fill(dist, Integer.MAX_VALUE);
		int[] prev = new int[n * 6];
		PriorityQueue<long[]> pq = new PriorityQueue<>((a, b) -> Long.compare(a[0], b[0]));
		Direction[] dirs = Direction.values();
		final int fx = minX, fy = minY, fz = minZ;
		IntFunction<BlockPos> pos = i -> new BlockPos(fx + i % nx, fy + (i / nx) % ny, fz + i / (nx * ny));
		java.util.function.Function<BlockPos, Integer> idx = p -> (p.getX() - fx) + nx * ((p.getY() - fy) + ny * (p.getZ() - fz));
		java.util.function.IntPredicate passable = i -> {
			if (ok[i] == 0) {
				BlockPos p = pos.apply(i);
				boolean good = free(level.getBlockState(p)) && !(boxCells.contains(p) && !goals.contains(p) && !p.equals(from));
				if (noGo(p) && !goals.contains(p)) {
					good = false;
				}
				for (int[] y : yards) {
					if (p.getX() >= y[0] && p.getX() <= y[1] && p.getZ() >= y[2] && p.getZ() <= y[3] && !goals.contains(p)) {
						good = false;
					}
				}
				if (good) {
					for (Direction d : dirs) {
						BlockPos q2 = p.relative(d);
						Block b = level.getBlockState(q2).getBlock();
						if ((b == kind || conductor(b)) && !own.contains(q2) && !goals.contains(q2)) {
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
			pq.add(new long[] {0, start * 6L + d});
		}
		int found = -1;
		while (!pq.isEmpty()) {
			long[] e = pq.poll();
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
				int cost = 10 + (d == dir || cell == start ? 0 : 40) + (q2.getY() > q.getY() ? 2 : 0);
				int nd = dist[s] + cost;
				int s2 = c2 * 6 + d;
				if (nd < dist[s2]) {
					dist[s2] = nd;
					prev[s2] = s;
					pq.add(new long[] {nd, s2});
				}
			}
		}
		require(found >= 0, "no free route for " + id + " from " + from.toShortString() + " to " + goals.iterator().next().toShortString());
		ArrayDeque<BlockPos> out = new ArrayDeque<>();
		for (int s = found; s >= 0; s = prev[s]) {
			out.addFirst(pos.apply(s / 6));
			if (s / 6 == start) {
				break;
			}
		}
		return new ArrayList<>(out);
	}

	private static boolean conductor(Block b) {
		return b == wc(LV) || b == wc(BUS) || b == wc(MV);
	}

	private List<BlockPos> run(ServerLevel level, String id, BlockPos from, BlockPos to, Set<BlockPos> own) {
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
		List<BlockPos> cellsOf = route(level, id, from, goals, own);
		for (BlockPos c : cellsOf) {
			lay(level, id, runSize, c);
		}
		own.addAll(cellsOf);
		log(id + " run of " + cellsOf.size() + " m from " + from.toShortString() + " to " + cellsOf.get(cellsOf.size() - 1).toShortString());
		return cellsOf;
	}

	/** A diesel set at {@code panel}, its skid west of it, facing north: 500 kW at the industrial voltage. */
	private BlockPos set(ServerLevel level, BlockPos panel) {
		Trailer.fillBox(level, panel, Blocks.SMOOTH_STONE, -8, 19, -1, -1, -5, 9);
		Trailer.fillBox(level, panel, Blocks.AIR, -8, 19, 0, 6, -5, 9);
		List<BlockPos> overwrote = GensetSkid.build(level, panel, Direction.NORTH, false);
		if (!overwrote.isEmpty()) {
			log("the set at " + panel.toShortString() + " overwrote " + overwrote.size() + " blocks");
		}
		panels.add(panel);
		yards.add(new int[] {panel.getX() - 8, panel.getX(), panel.getZ() - 5, panel.getZ() + 9});
		return panel;
	}

	/** Runs from {@code out} to each box in turn, all one net. */
	private void wire(ServerLevel level, String id, ConductorSize size, BlockPos out, BlockPos... boxes) {
		ConductorSize was = runSize;
		runSize = size;
		Set<BlockPos> net = new HashSet<>();
		boolean first = true;
		for (BlockPos b : boxes) {
			run(level, id, b, first ? out : null, net);
			first = false;
		}
		runSize = was;
	}

	private static void pipe(ServerLevel level, BlockPos... at) {
		Trailer.pipe(level, at);
	}

	private static List<BlockPos> line(BlockPos a, BlockPos b) {
		List<BlockPos> out = new ArrayList<>();
		int dx = Integer.signum(b.getX() - a.getX()), dy = Integer.signum(b.getY() - a.getY()), dz = Integer.signum(b.getZ() - a.getZ());
		BlockPos at = a;
		out.add(at);
		while (!at.equals(b)) {
			at = at.offset(dx, dy, dz);
			out.add(at);
		}
		return out;
	}

	private static void water(ServerLevel level, BlockPos... at) {
		for (BlockPos c : at) {
			lay(level, WATER, null, c);
		}
	}

	private static BlockPos dayTank(ServerLevel level, BlockPos at) {
		level.setBlock(at, wc("wildcatter:day_tank").defaultBlockState(), Block.UPDATE_ALL);
		return at;
	}

	private static SlurryAssay tails() {
		return SlurryAssay.of(Map.of(CU.mineralItemId(), RICH * 0.25));
	}

	private static void muck(ServerLevel level, BlockPos at, int k) {
		level.setBlock(at, TailingsBlocks.MUCK.defaultBlockState(), Block.UPDATE_CLIENTS);
		if (level.getBlockEntity(at) instanceof MuckBlockEntity m) {
			m.set(Map.of(Math.floorMod(k, 4) == 0 ? "minecraft:andesite" : "minecraft:cobblestone", PitMass.MUCK_QUARTERS));
		}
	}

	// ---- the site -----------------------------------------------------------------------------------------------------------------

	/**
	 * The ground from o: the waste dump's top (z 0 on, x -60..110) of tipped waste, its 10 m face to the north over the dump's floor;
	 * the cut lane's soil; the plain ground east of it at road level; the dry stack at q, its lift 1 m thick north of the road.
	 */
	private void ground(ServerLevel level) {
		BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
		BlockState air = Blocks.AIR.defaultBlockState();
		for (int x = -70; x <= QX + 110; x++) {
			for (int z = -70; z <= 130; z++) {
				boolean dumpFloor = x <= 110 && z < 0;
				boolean lane = x >= CX - 5 && x <= CX + 5 && z >= 26 && z <= 92;
				int qx = x - QX;
				boolean stackGround = qx >= -40 && qx <= 70 && z < -10;   // the stack's ground, a metre under the road
				boolean lift = qx >= -15 && qx <= 45 && z >= LIFT_EDGE + 1 && z <= -11;
				int k = Math.floorMod((x * 73856093) ^ (z * 19349663) ^ ((x * z) * 83492791), 13);
				for (int y = -16; y <= -1; y++) {
					BlockState st = (Math.floorMod(y * 3 + (x + z) / 19, 9) == 0 ? Blocks.ANDESITE : Math.floorMod(x * 7 + y * 13 + z * 5, 13) == 0 ? Blocks.TUFF : Blocks.STONE).defaultBlockState();
					if (x <= 110 && y == -1 && !dumpFloor) {   // the dump's top: tipped waste, stone and andesite with gravel
						st = (k < 3 ? Blocks.GRAVEL : k < 5 ? Blocks.ANDESITE : k == 6 ? Blocks.COARSE_DIRT : Blocks.STONE).defaultBlockState();
					}
					if (lane && y >= -3) {   // soil 3 m deep over the rock
						st = (y == -1 && k < 4 ? Blocks.COARSE_DIRT : Blocks.DIRT).defaultBlockState();
					}
					// the face: 4 m standing at the crest, then the talus of earlier tips at its angle of repose down to the floor
					int talus = -5 - (int) Math.floor((-z - 1) * 0.75);
					if (dumpFloor && y >= DF && y <= talus && z >= -12) {
						int r = Math.floorMod((x * 73856093) ^ (z * 19349663) ^ (y * 83492791), 7);
						st = (r < 2 ? Blocks.GRAVEL : r < 4 ? Blocks.COBBLESTONE : r == 4 ? Blocks.ANDESITE : Blocks.STONE).defaultBlockState();
					} else if (dumpFloor && y >= DF) {
						st = air;
					} else if (dumpFloor && y == DF - 1) {
						st = (k < 3 ? Blocks.GRAVEL : k == 5 ? Blocks.ANDESITE : Blocks.STONE).defaultBlockState();
					}
					if (x > 110 && y == -1) {   // the plant's ground and road
						st = (k < 2 ? Blocks.GRAVEL : k == 4 ? Blocks.COARSE_DIRT : Blocks.STONE).defaultBlockState();
					}
					if (stackGround && y == -1) {
						st = lift ? TailingsBlocks.DRY_TAILS.defaultBlockState() : air;
					}
					level.setBlock(p.set(o.getX() + x, o.getY() + y, o.getZ() + z), st, Block.UPDATE_CLIENTS);
				}
				for (int y = 0; y <= 40; y++) {
					level.setBlock(p.set(o.getX() + x, o.getY() + y, o.getZ() + z), air, Block.UPDATE_CLIENTS);
				}
			}
		}
		log("ground laid");
	}

	private void stage() {
		server.runOnServer(srv -> {
			ServerLevel level = srv.overworld();
			BlockPos s = Trailer.site(level, -2400, 2600, 360, 200);
			int g = s.getY();
			o = new BlockPos(s.getX() + 70, g, s.getZ() + 70);
			q = o.offset(QX, 0, 0);
			Trailer.pad(level, o.getX() - 70, o.getZ() - 70, o.getX() + QX + 110, o.getZ() + 130, g, 18, 50);
			Trailer.forceChunks(level, o.getX() - 110, o.getZ() - 110, o.getX() + QX + 150, o.getZ() + 170);
			ground(level);
			// ---- the waste dump: the windrow of waste along the crest the dozer will take (laid by the scene), 3 wide, x -2..15
			int n = 0;
			for (int x = -2; x <= 15; x++) {
				for (int z = 5; z <= 7; z++) {
					muck(level, o.offset(x, 0, z), n++);
				}
			}
			// a pile of waste short of the crest, west (laid by the scene): 18 blocks of muck, a blade's load, for the dozer to push over
			for (int x = -42; x <= -38; x++) {
				for (int z = 10; z <= 12; z++) {
					muck(level, o.offset(x, 0, z), n++);
					if (z == 11 && x >= -41 && x <= -39) {
						muck(level, o.offset(x, 1, z), n++);
					}
				}
			}
			// the ultra's route: the DUMP POINT 2 m in from the crest, the turn stake behind it, the LOAD end far back
			int[][] st = {{SX, 0, SZ}, {SX, 0, TZ}, {SX, 0, 115}};
			StopKind[] kinds = {StopKind.DUMP_POINT, StopKind.WAYPOINT, StopKind.LOAD};
			for (int i = 0; i < st.length; i++) {
				BlockPos at = o.offset(st[i][0], st[i][1], st[i][2]);
				level.setBlock(at, TailingsBlocks.SURVEY_STAKE.defaultBlockState().setValue(SurveyStakeBlock.KIND, kinds[i]), Block.UPDATE_ALL);
				ultraRoute.add(at.immutable());
			}
			// ---- the dry stack (Rtm08's P4 layout): tails (as from the bank) into the thickener; its underflow up to the press over the load-out
			s1 = plant(level, Machine.FLOTATION_BANK, d(46, 0, 61));
			boxCells.remove(box(s1));
			pipe(level, d(46, 0, 74), d(45, 0, 74), d(44, 0, 74));
			thick = plant(level, Machine.TAILINGS_THICKENER, d(30, 0, 75));
			frame = plant(level, Machine.LOADOUT_FRAME, d(14, 0, 60));
			bin = plant(level, Machine.LOADOUT_BIN, d(15, 10, 61));
			boxCells.remove(box(bin));
			filter = plant(level, Machine.TAILINGS_FILTER, d(17, 17, 58));
			List<BlockPos> under = new ArrayList<>(line(d(44, 0, 90), d(44, 20, 90)));
			under.addAll(line(d(44, 20, 89), d(44, 20, 57)));
			under.addAll(line(d(43, 20, 57), d(22, 20, 57)));
			pipe(level, under.toArray(BlockPos[]::new));
			water(level, d(45, 0, 80), d(46, 0, 80));
			tanks.add(dayTank(level, d(47, 0, 80)));
			tanks.add(dayTank(level, d(46, 0, 79)));
			tanks.add(dayTank(level, d(46, 0, 81)));
			// the truck's route: the DUMP POINT on the lift (a paddock: no crest within 10 m), two turn stakes, the LOADOUT
			int[] zs = {-22, 12, 45, 59};
			StopKind[] tk = {StopKind.DUMP_POINT, StopKind.WAYPOINT, StopKind.WAYPOINT, StopKind.LOADOUT};
			for (int i = 0; i < zs.length; i++) {
				BlockPos at = d(19, 0, zs[i]);
				level.setBlock(at, TailingsBlocks.SURVEY_STAKE.defaultBlockState().setValue(SurveyStakeBlock.KIND, tk[i]), Block.UPDATE_ALL);
				truckRoute.add(at);
			}
			for (PlantBlockEntity part : List.of(thick, filter, frame)) {
				setHoa(part, "OFF");
			}
			log("site at " + o.toShortString() + "; dry stack at " + q.toShortString());
		});
		context.waitTicks(60);
		server.runCommand("kill @e[type=item]");
		server.runCommand("kill @e[type=!player]");
	}

	/** Two diesel sets: the press and the load-out gate's west of the road, the thickener's east (Rtm08's). */
	private void power() {
		server.runOnServer(srv -> {
			ServerLevel level = srv.overworld();
			BlockPos a = set(level, d(-20, 0, 30));
			wire(level, LV, ConductorSize.KCMIL_500, a.east(), box(frame), box(filter));
			BlockPos b = set(level, d(84, 0, 12));
			wire(level, LV, ConductorSize.KCMIL_500, b.east(), box(thick));
		});
		context.waitTicks(40);
		server.runOnServer(srv -> {
			for (BlockPos panel : panels) {
				((GensetControlPanelBlockEntity) srv.overworld().getBlockEntity(panel)).setWantRun(true);
			}
			keep(srv);
		});
		ticks(400);
		server.runOnServer(srv -> {
			setHoa(thick, "HAND");
			setHoa(frame, "HAND");
		});
		ticks(60);
		server.runOnServer(srv -> setHoa(filter, "HAND"));
		ticks(100);
		server.runOnServer(srv -> {
			for (BlockPos panel : panels) {
				log("set " + panel.toShortString() + ": " + ((GensetControlPanelBlockEntity) srv.overworld().getBlockEntity(panel)).inspect().getString());
			}
		});
	}

	/** The dozer (parked on the dump top for its scale film), the ultra parked beside it; tanks filled. */
	private void vehicles() {
		server.runOnServer(srv -> {
			ServerLevel level = srv.overworld();
			TrackDozerEntity dz = TailingsEntities.TRACK_DOZER.create(level, EntitySpawnReason.COMMAND);
			dz.setPos(o.getX() + 34.5, o.getY(), o.getZ() + 26.5);
			dz.setYRot(180);
			dz.setFuel(dz.tank());
			dz.setBlade(TrackDozerEntity.Blade.CARRY);
			level.addFreshEntity(dz);
			dozerId = dz.getUUID();
			UltraHaulTruckEntity u = TailingsEntities.ULTRA_HAUL_TRUCK.create(level, EntitySpawnReason.COMMAND);
			u.setPos(o.getX() + 19.5, o.getY(), o.getZ() + 28.5);
			u.setYRot(180);
			u.setFuel(u.tank());
			level.addFreshEntity(u);
			ultraId = u.getUUID();
		});
		ticks(20);
	}

	// ---- keeping the stand-ins up -------------------------------------------------------------------------------------------------

	private TrackDozerEntity dozer(ServerLevel level) {
		return dozerId != null && level.getEntity(dozerId) instanceof TrackDozerEntity d ? d : null;
	}

	private UltraHaulTruckEntity ultra(ServerLevel level) {
		return ultraId != null && level.getEntity(ultraId) instanceof UltraHaulTruckEntity u ? u : null;
	}

	private HaulTruckEntity truck(ServerLevel level) {
		return truckId != null && level.getEntity(truckId) instanceof HaulTruckEntity t && !(t instanceof UltraHaulTruckEntity) ? t : null;
	}

	/** Every second: the sets' fuel, the tails into the thickener (as if from the bank), the water tanks drawn down. */
	private void keep(MinecraftServer srv) {
		ServerLevel level = srv.overworld();
		for (BlockPos panel : panels) {
			if (level.getBlockEntity(GensetSkid.base(panel, Direction.NORTH, false)) instanceof GensetPartBlockEntity tank) {
				tank.setFuel(2000);
			}
		}
		if (s1 == null) {
			return;
		}
		if (s1.out().held(Product.TAILS_SLURRY) < 400) {
			s1.out().offer(Product.TAILS_SLURRY, 400 - s1.out().held(Product.TAILS_SLURRY), tails());   // as if from the flotation bank
		}
		solids(thick);   // the underflow's solids as last seen, so the card has it from the first frame
		for (BlockPos at : tanks) {
			if (level.getBlockEntity(at) instanceof DayTankBlockEntity tank) {
				drained += tank.waterLevel() * 1000;
				tank.setLitres(0);
			}
		}
		if (++keeps % 15 == 0) {
			TrackDozerEntity dz = dozer(level);
			UltraHaulTruckEntity u = ultra(level);
			HaulTruckEntity t = truck(level);
			log(String.format(Locale.ROOT, "tick %.1f ms; dozer: %s; ultra: %s; truck: %s; press %s, %d cakes, bin %.1f t; thickener %s", srv.getAverageTickTimeNanos() / 1e6,
					dz == null ? "-" : dz.status(), u == null ? "-" : u.status(), t == null ? "-" : t.status(), filter.status(), filter.cycles(), bin.dryKg() / 1000, thick.status()));
		}
	}

	// ---- readouts -----------------------------------------------------------------------------------------------------------------

	/** An engine's output from its burn: 36 MJ a litre at 38 % (PitFuel), measured over a second. */
	private void meter(PitVehicleEntity v, int tick) {
		if (v == null || tick % 20 != 0) {
			return;
		}
		double f = v.fuel();
		if (!Double.isNaN(lastFuel) && lastFuel >= f) {
			engineKw = (lastFuel - f) * 36.0e6 * 0.38 / 1000.0;
		}
		lastFuel = f;
	}

	private void watch(String who, String st) {
		String head = st.split(":")[0];
		if (filming && !head.equals(lastStatus)) {
			event(who + ": " + st);
		}
		lastStatus = head;
	}

	private String[] dozerCard(ServerLevel level, int tick, String... extra) {
		TrackDozerEntity dz = dozer(level);
		if (dz == null) {
			return new String[] {"^Track dozer", "gone"};
		}
		meter(dz, tick);
		String st = dz.status();
		watch("dozer", st);
		String blade = switch (dz.blade()) {
			case CUT -> "CUT, to " + dz.grade().label();
			case CARRY -> dz.spreading() ? "CARRY, spreading" : "CARRY, berm " + dz.bermHigh() + " m";
			case RAISED -> "RAISED";
		};
		List<String> l = new ArrayList<>(List.of("^Track dozer, 70 t, 447 kW",
				"Now: " + shortNow(st),
				String.format(Locale.ROOT, "Blade: %s; %d of %d blocks", blade, dz.bladeBlocks(), TrackDozerEntity.BLADE_BLOCKS),
				String.format(Locale.ROOT, "Speed: %.1f km/h; diesel %s kW (from its burn)", Math.abs(dz.speed()) * 3.6, f0(engineKw))));
		l.addAll(List.of(extra));
		if (!note.isEmpty()) {
			l.add(note);
		}
		return l.toArray(String[]::new);
	}

	private static String shortNow(String st) {
		String now = st.replaceAll("; blade \\d+ of \\d+", "").replace(": 0 blocks left", ": blade empty");
		return now.length() > 46 ? now.substring(0, 46) : now;
	}

	private String[] ultraCard(ServerLevel level, int tick) {
		UltraHaulTruckEntity u = ultra(level);
		List<String> l = new ArrayList<>();
		l.add("^Ultra truck, 363 t body");
		if (u != null) {
			String st = u.status();
			watch("ultra", st);
			if (st.startsWith("NO BERM")) {
				l.add("!NO BERM at the DUMP POINT:");
				l.add("!  push one up along the crest");
			} else {
				l.add("Now: " + (st.length() > 44 ? st.substring(0, 44) : st));
			}
			l.add("Carrying: " + u.load().tonnes() + " of waste rock");
			l.add(String.format(Locale.ROOT, "Berm it needs: %d m (its wheel's radius %.1f m)", DryStack.bermBlocks(u.wheelRadius()), u.wheelRadius()));
		}
		TrackDozerEntity dz = dozer(level);
		if (dz != null && dz.bladeBlocks() > 0) {
			l.add("Dozer: " + dz.bladeBlocks() + " blocks of waste on its blade");
		}
		if (!note.isEmpty()) {
			l.add(note);
		}
		return l.toArray(String[]::new);
	}

	private static String rd(PlantBlockEntity part, String name, String kw) {
		String st = part.status();
		boolean fault = st.startsWith("NO ") || st.startsWith("NOT ") || st.startsWith("FULL") || st.startsWith("waiting") || st.startsWith("LINE");
		if (fault) {
			return (st.startsWith("waiting") ? "" : "!") + name + ": " + brief(st);
		}
		return part.running() ? "+" + name + " (" + kw + "): " + f0(amps(part)) + " A, " + brief(st) : name + ": " + brief(st);
	}

	private static String brief(String st) {
		int cut = st.indexOf(':');
		if (st.startsWith("NO ") || st.startsWith("NOT ") || st.startsWith("FULL")) {
			return cut > 0 ? st.substring(0, cut) : st;
		}
		return st.length() > 30 ? st.substring(0, 30) : st;
	}

	private final Map<BlockPos, Double> lastSolids = new java.util.HashMap<>();

	/**
	 * The thickener's underflow solids, as marked on the thickened tails: on its outlet while the cone holds some, else in the press's
	 * feed, the same slurry piped up to it. (The press draws the outlet empty as fast as it is made, and the thickener's own buffer is
	 * its 35 % feed, so reading only the thickener read 0 %.)
	 */
	private double solids(PlantBlockEntity part) {
		double now = Math.max(part.out().assayOf(Product.TAILS_SLURRY).get(DryStack.SOLIDS_KEY), filter == null ? 0 : filter.back().assayOf(Product.TAILS_SLURRY).get(DryStack.SOLIDS_KEY)) * 100;
		if (now > 0) {
			lastSolids.put(part.getBlockPos(), now);
		}
		return lastSolids.getOrDefault(part.getBlockPos(), 0.0);
	}

	private String setLine(ServerLevel level, int i, String name) {
		if (i < panels.size() && level.getBlockEntity(panels.get(i)) instanceof GensetControlPanelBlockEntity panel) {
			return "+" + name + ": " + f0(panel.watts() / 1000) + " kW of " + f0(panel.ratedWatts() / 1000);
		}
		return name + ": -";
	}

	private String[] thickCard(ServerLevel level) {
		return new String[] {"^TAILINGS THICKENER, 15 M", "Tails in: 35 % solids (fed by the scene, as from the plant)", rd(thick, "Thickener", "11 kW"),
				String.format(Locale.ROOT, "Underflow: %.0f %% solids, piped up to the press", solids(thick)), "Overflow water back: " + f1(drained / 1000) + " m³",
				setLine(level, 1, "Diesel set")};
	}

	private String[] pressCard(ServerLevel level) {
		int sec = (int) (filter.cycle() / 20);
		String phase = filter.pressHeld() ? "filling: waiting for a full batch" : DryStack.phase(sec);
		List<String> l = new ArrayList<>(List.of("^TAILINGS FILTER · BATCH PRESS", rd(filter, "Filter press", "150 kW"),
				String.format(Locale.ROOT, "  cycle %d:%02d of 15:00 (%s)", sec / 60, sec % 60, phase), String.format(Locale.ROOT, "  in the press: %.1f t", filter.dryKg() / 1000),
				"Cakes dropped: " + filter.cycles() + " (15 t each, 85 % solids)", String.format(Locale.ROOT, "Load-out bin under it: %.1f t (holds 400 t)", bin.dryKg() / 1000),
				setLine(level, 0, "Diesel set")));
		if (!note.isEmpty()) {
			l.add(note);
		}
		return l.toArray(String[]::new);
	}

	private String[] haulCard(ServerLevel level, int tick) {
		List<String> l = new ArrayList<>(List.of("^LOAD-OUT AND DRY STACK", String.format(Locale.ROOT, "Load-out bin: %.1f t of dry tails", bin.dryKg() / 1000),
				frame.running() ? "+Load-out gate (7.5 kW): " + f0(amps(frame)) + " A, loading" : "Load-out gate: waiting for a truck"));
		HaulTruckEntity t = truck(level);
		if (t != null) {
			String st = t.status();
			watch("truck", st);
			st = st.replace(" to its gate", "");
			l.add("Truck (91 t body): " + (st.length() > 34 ? st.substring(0, 34) : st));
			l.add("  carrying " + t.load().tonnes() + " of dry tails");
		}
		if (!note.isEmpty()) {
			l.add(note);
		}
		return l.toArray(String[]::new);
	}

	// ---- driving the dozer by keys (it has no autopilot: a player drives it) ------------------------------------------------------

	private static void keys(TrackDozerEntity dz, Input in) {
		if (dz != null) {
			dz.testDrive(in);
		}
	}

	private void placeDozer(double x, double z, float yaw, TrackDozerEntity.Blade blade) {
		server.runOnServer(srv -> dozer(srv.overworld()).testDrive(Input.EMPTY));
		waitFor(400, level -> Math.abs(dozer(level).speed()) < 0.01);
		if (server.computeOnServer(srv -> dozer(srv.overworld()).bladeBlocks() > 0)) {
			// off camera: a touch of reverse leaves what the blade still carries where it stands, as a player empties it
			server.runOnServer(srv -> {
				TrackDozerEntity dz = dozer(srv.overworld());
				dz.setBlade(TrackDozerEntity.Blade.CARRY);   // a load is left by reversing in CARRY
				dz.testDrive(new Input(false, true, false, false, false, false, false));
			});
			waitFor(200, level -> dozer(level).bladeBlocks() == 0);
			server.runOnServer(srv -> dozer(srv.overworld()).testDrive(Input.EMPTY));
			waitFor(400, level -> Math.abs(dozer(level).speed()) < 0.01);
		}
		server.runOnServer(srv -> {
			TrackDozerEntity dz = dozer(srv.overworld());
			dz.setPos(o.getX() + x, o.getY(), o.getZ() + z);
			dz.setYRot(yaw);
			dz.setBlade(blade);
			dz.setFuel(dz.tank());
		});
		ticks(40);
		lastFuel = Double.NaN;
		engineKw = 0;
	}

	// ---- the waste dump's films ---------------------------------------------------------------------------------------------------

	/** hi04_scale: a villager at the dozer's blade, the ultra parked beside it; the camera rises and pulls back over both. */
	private void scaleFilm() {
		server.runCommand(String.format(Locale.ROOT, "summon villager %.1f %d %.1f {NoAI:1b,Silent:1b,Invulnerable:1b,Rotation:[200f,0f],Tags:[\"hi04a\"]}",
				o.getX() + 32.5, o.getY(), o.getZ() + 20.5));
		ticks(10);
		Cam a = dcam(28.5, 1.4, 16.5, 33, 1.3, 21.5);
		Cam b = dcam(24, 4, 10, 32, 2.5, 24);
		Cam c = dcam(14, 12, 0, 28, 3, 27);
		film("hi04_scale", 1000, via(a, b, c, 1000), null);
		server.runCommand("kill @e[tag=hi04a]");
	}

	/** hi04_tracks: beside its left track, low, as it drives east over a hummock, blade raised: the grousers running, the body tilting. */
	private void tracksFilm() {
		placeDozer(-30.5, 40.5, -90, TrackDozerEntity.Blade.RAISED);
		server.runOnServer(srv -> {
			for (int x = 4; x <= 6; x++) {
				for (int z = 36; z <= 45; z++) {
					int k = Math.floorMod((x * 73856093) ^ (z * 19349663), 5);
					srv.overworld().setBlock(o.offset(x, 0, z), (k == 0 ? Blocks.GRAVEL : k == 1 ? Blocks.ANDESITE : Blocks.STONE).defaultBlockState(), Block.UPDATE_ALL);
				}
			}
		});
		showCard(true);
		int[] started = {-1};
		film("hi04_tracks", 900, follow(dozerId, -5.5, -1, 1.0, 0.8), (level, t) -> {
			TrackDozerEntity dz = dozer(level);
			if (t == PAD) {
				keys(dz, FORWARD);
				note = "-Driven by the scene's keys, as a player drives it";
				event("forward, blade raised");
				started[0] = t;
			}
			if (dz != null && dz.getX() - o.getX() > 30 && started[0] > 0) {
				keys(dz, Input.EMPTY);
				if (t > started[0] + 40) {
					endMove = true;
				}
			}
			card(dozerCard(level, t));
		});
		note = "";
		showCard(false);
		server.runOnServer(srv -> keys(dozer(srv.overworld()), Input.EMPTY));
	}

	/** hi04_cut: in CUT to 10 %, south along the soil lane, a block down each 10 m, the blade filling and spilling, until the rock under the soil stops it. */
	private void cutFilm() {
		placeDozer(CX + 0.5, 30.5, 0, TrackDozerEntity.Blade.CUT);
		server.runOnServer(srv -> dozer(srv.overworld()).setGrade(DryStack.Grade.TEN));
		showCard(true);
		int[] stopped = {-1};
		film("hi04_cut", 1600, follow(dozerId, 13, 9, 6, 0), (level, t) -> {
			TrackDozerEntity dz = dozer(level);
			if (t == PAD) {
				keys(dz, FORWARD);
				note = "-Driven by the scene's keys, as a player drives it";
				event("forward, blade CUT to 10 %");
			}
			if (dz != null && stopped[0] < 0 && dz.status().startsWith("hard rock")) {
				stopped[0] = t;
				keys(dz, Input.EMPTY);
				event("stopped: " + dz.status() + " at z " + f1(dz.getZ() - o.getZ()));
			}
			if (stopped[0] > 0 && t > stopped[0] + 80) {
				endMove = true;
			}
			card(dozerCard(level, t, "Soil 3 m deep over rock"));
		});
		note = "";
		showCard(false);
		server.runOnServer(srv -> {
			keys(dozer(srv.overworld()), Input.EMPTY);
			StringBuilder tops = new StringBuilder();
			for (int z = 36; z <= 86; z += 5) {
				int top = DF;
				for (int y = 3; y >= -6; y--) {
					if (!srv.overworld().getBlockState(o.offset(CX, y, z)).isAir()) {
						top = y;
						break;
					}
				}
				tops.append(z).append(':').append(top).append(' ');
			}
			log("cut lane tops (z:y) " + tops);
		});
		require(stopped[0] > 0, "the dozer should cut down to the rock and stop: hard rock");
	}

	/** hi04_push: in CARRY it drives north into a pile of waste tipped short of the crest, takes it on its blade and pushes it over the crest. */
	private void pushFilm() {
		placeDozer(-39.5, 30.5, 180, TrackDozerEntity.Blade.CARRY);
		showCard(true);
		int[] over = {-1};
		film("hi04_push", 1200, move(dcam(-54, 3, -9, -40, -1, 7), dcam(-53, 2, -11, -40, -2, 3), 1200), (level, t) -> {
			TrackDozerEntity dz = dozer(level);
			if (t == PAD) {
				keys(dz, FORWARD);
				note = "-Pile laid by the scene; driven by its keys";
				event("forward, blade CARRY");
			}
			if (dz != null && over[0] < 0 && dz.status().startsWith("at the crest")) {
				over[0] = t;
				keys(dz, Input.EMPTY);
				event(dz.status());
			}
			if (over[0] > 0 && t > over[0] + 120) {
				endMove = true;
			}
			card(dozerCard(level, t));
		});
		note = "";
		showCard(false);
		server.runOnServer(srv -> keys(dozer(srv.overworld()), Input.EMPTY));
	}

	/** hi04_noberm: the ultra, loaded with waste off camera, on its route to the DUMP POINT at a crest with no berm: it stops, NO BERM. */
	private void noBermFilm() {
		placeDozer(-12.5, BERM_Z, -90, TrackDozerEntity.Blade.CARRY);
		server.runOnServer(srv -> {
			ServerLevel level = srv.overworld();
			dozer(level).setBermHigh(2);
			UltraHaulTruckEntity u = ultra(level);
			u.setPos(o.getX() + SX + 0.5, o.getY(), o.getZ() + 108.5);
			u.setYRot(180);
			u.setFuel(u.tank());
			u.load().add("minecraft:cobblestone", 270_000);
			u.load().add("minecraft:andesite", 90_000);
			u.refreshLoad();
			log("ultra loaded (off camera): " + u.load().tonnes() + "; route: " + u.takeRoute(level, ultraRoute));
		});
		ticks(20);
		showCard(true);
		int[] stopped = {-1};
		film("hi04_noberm", 2400, follow(ultraId, 16, 18, 10, 2), (level, t) -> {
			UltraHaulTruckEntity u = ultra(level);
			if (u != null && stopped[0] < 0 && u.status().startsWith("NO BERM")) {
				stopped[0] = t;
				event("ultra: " + u.status());
			}
			if (stopped[0] > 0 && t > stopped[0] + 700) {
				endMove = true;
			}
			if (t == PAD + 10) {
				note = "-Loaded off camera";
			}
			card(ultraCard(level, t));
		});
		note = "";
		showCard(false);
		require(stopped[0] > 0, "the ultra should stop short of the crest with no berm");
	}

	/** hi04_berm: from the dump's floor, the dozer drives along the crest, takes the windrow onto its blade and drops a 2 m berm a column a metre. */
	private void bermFilm() {
		showCard(true);
		int[] state = {0, -1};
		film("hi04_berm", 1800, follow(dozerId, -15, 3, 8, 0), (level, t) -> {
			TrackDozerEntity dz = dozer(level);
			if (t == PAD) {
				keys(dz, FORWARD);
				note = "-Windrow laid by the scene; driven by its keys";
				event("dozer forward along the crest, berm 2 m");
				state[0] = 1;
			}
			if (state[0] == 1 && dz != null && dz.getX() - o.getX() > 34) {
				keys(dz, Input.EMPTY);
				state[0] = 2;
				state[1] = t;
				event("dozer stopped: " + dz.status() + " at x " + f1(dz.getX() - o.getX()));
			}
			if (state[0] == 2 && t > state[1] + 100) {
				endMove = true;
			}
			card(dozerCard(level, t));
		});
		note = "";
		showCard(false);
		server.runOnServer(srv -> {
			ServerLevel level = srv.overworld();
			keys(dozer(level), Input.EMPTY);
			StringBuilder b = new StringBuilder();
			int cols = 0;
			for (int x = -12; x <= 36; x++) {
				int h = 0;
				while (h < 3 && !level.getBlockState(o.offset(x, h, 0)).isAir()) {
					h++;
				}
				if (h > 0) {
					cols++;
				}
				b.append(h);
			}
			log("berm along the crest x -12..36 (heights): " + b + "; " + cols + " columns");
		});
	}

	/** hi04_tip: from the dump's floor, the ultra turns, backs to the berm and tips over it; the load pours down the face. */
	private void tipFilm() {
		showCard(true);
		boolean[] tipped = {false};
		int[] down = {-1};
		film("hi04_tip", 3600, move(dcam(SX + 24, DF + 8, -26, SX, -3, 1), dcam(SX + 20, DF + 7, -24, SX, -4, 1), 3600), (level, t) -> {
			card(ultraCard(level, t));
			UltraHaulTruckEntity u = ultra(level);
			if (u == null) {
				return;
			}
			if (u.lift() > 0.5 && !tipped[0]) {
				tipped[0] = true;
				event("body up: tipping");
			}
			if (tipped[0] && u.lift() == 0 && u.pouring() == 0) {
				if (down[0] < 0) {
					down[0] = t;
					event("body down, the pour set");
				}
				if (t - down[0] > 60) {
					endMove = true;
				}
			}
		});
		showCard(false);
		require(tipped[0], "the ultra should tip over the berm");
	}

	/** hi04_dump: a slow turn over the dump from the air: the berm, the face and the new tip, the dozer, the ultra. */
	private void dumpWideFilm() {
		// nearer and lower than the first take (70 m out, 38 m up), so the face, the talus and the berm on the crest read
		film("hi04_dump", 1200, orbitAround(o, 14, -2, 4, 42, 12, 200, 40, 1200), null);
	}

	// ---- the dry stack's films ------------------------------------------------------------------------------------------------------

	/** hi04_thickener: the tailings thickener, the press beyond. */
	private void thickenerFilm() {
		showCard(true);
		film("hi04_thickener", 900, move(qcam(52, 14, 98, 30, 2, 75), qcam(54, 12, 88, 30, 2, 74), 900), (level, k) -> card(thickCard(level)));
		showCard(false);
	}

	/** hi04_press: the batch press over the load-out bin and frame, mid-cycle. */
	private void pressFilm() {
		if (!wanted("hi04_press")) {
			return;
		}
		// squeezing a batch (every batch is a full 15 t: the press holds at its fill till it is)
		boolean ok = waitFor(40000, level -> !filter.pressHeld() && filter.dryKg() > DryStack.BATCH_KG - 1 && filter.cycle() / 20 >= 150 && filter.cycle() / 20 <= 260);
		log("press squeezing a full batch: " + ok + ", " + filter.status());
		showCard(true);
		// above the press's floor, east of it, down its length from the fixed head to the moving tail
		film("hi04_press", 1000, move(qcam(30, 25, 60, 20, 21.5, 64), qcam(31, 24.5, 76, 20, 21.5, 74), 1000), (level, k) -> card(pressCard(level)));
		note = "";
		showCard(false);
	}

	/** hi04_loadout: a self-driving 100 t truck through the load-out's lane, filled under the gate (the bin stocked before filming). */
	private void loadoutFilm() {
		server.runOnServer(srv -> {
			ServerLevel level = srv.overworld();
			bin.setDryKg(Math.max(bin.dryKg(), 100_000));   // stocked for the film
			HaulTruckEntity t = TailingsEntities.HAUL_TRUCK.create(level, EntitySpawnReason.COMMAND);
			t.setPos(q.getX() + 19.5, q.getY(), q.getZ() + 34);
			t.setYRot(0);
			t.setFuel(t.tank());
			level.addFreshEntity(t);
			log("truck: " + t.takeRoute(level, truckRoute));
			truckId = t.getUUID();
		});
		ticks(20);
		// off camera: up to the mouth of the lane (it creeps up to the frame's legs)
		boolean in = waitFor(3000, level -> {
			HaulTruckEntity t = truck(level);
			return t != null && (t.getZ() - q.getZ() > 47 || t.status().startsWith("waiting to load") || t.status().startsWith("loading"));
		});
		log("truck at the lane's mouth: " + in);
		// it creeps the last metres to the gate for over a minute (the first take's first 80 s): start the film about 8 s before it stops
		boolean near = waitFor(1400, level -> {
			HaulTruckEntity t = truck(level);
			return t == null || t.status().startsWith("loading") || t.status().startsWith("waiting to load");
		});
		log("truck near the gate: " + near);
		showCard(true);
		int[] full = {-1};
		film("hi04_loadout", 4000, move(qcam(36, 9, 44, 19, 3, 62), qcam(34, 8, 50, 19, 3, 62), 4000), (level, k) -> {
			HaulTruckEntity t = truck(level);
			if (t != null && full[0] < 0 && t.load().room() <= 0) {
				full[0] = k;
				event("truck full: " + t.load().tonnes() + "; bin " + f1(bin.dryKg() / 1000) + " t");
			}
			if (k == PAD + 10) {
				note = "-Bin stocked before filming";
			}
			if (full[0] > 0 && k > full[0] + 80) {
				endMove = true;
			}
			card(haulCard(level, k));
		});
		note = "";
		showCard(false);
		require(full[0] > 0, "the truck should be filled under the gate");
	}

	/** hi04_stack: the truck comes round, backs onto the stack's lift to the DUMP POINT (a paddock) and tips. */
	private void stackFilm() {
		boolean got = waitFor(4000, level -> {
			HaulTruckEntity t = truck(level);
			return t != null && t.load().total() > 0 && (t.status().startsWith("turning round") || t.status().startsWith("backing"));
		});
		log("truck coming to the stack: " + got);
		showCard(true);
		boolean[] tipped = {false};
		int[] down = {-1};
		film("hi04_stack", 2000, move(qcam(44, 9, -12, 19, 0, -24), qcam(42, 10, -18, 19, 0, -26), 2000), (level, k) -> {
			card(haulCard(level, k));
			HaulTruckEntity t = truck(level);
			if (t == null) {
				return;
			}
			if (t.lift() > 0.5 && !tipped[0]) {
				tipped[0] = true;
				event("body up: tipping on the lift");
			}
			if (tipped[0] && t.lift() == 0 && t.pouring() == 0) {
				if (down[0] < 0) {
					down[0] = k;
					event("body down");
				}
				if (k - down[0] > 40) {
					endMove = true;
				}
			}
		});
		showCard(false);
		require(tipped[0], "the truck should tip on the lift");
		ticks(100);   // it drives off on its route, back toward the load-out, out of the dozer's way
	}

	/** hi04_spread: set to SPREAD, the dozer pushes the tipped load north to the lift's edge and lays it as a 1 m lift, driving onto it. */
	private void spreadFilm() {
		placeDozer(QX + 19.5, -12.5, 180, TrackDozerEntity.Blade.CARRY);
		server.runOnServer(srv -> dozer(srv.overworld()).setSpread());
		showCard(true);
		int[] state = {0, -1};
		int[] placed0 = {0};
		film("hi04_spread", 1800, move(qcam(40, 7, -20, 19, 0, -34), qcam(38, 8, -46, 19, 0, -38), 1800), (level, k) -> {
			TrackDozerEntity dz = dozer(level);
			if (k == PAD) {
				keys(dz, FORWARD);
				note = "-Driven by the scene's keys, as a player drives it";
				event("dozer forward, SPREAD");
				state[0] = 1;
			}
			if (dz != null && dz.bladeBlocks() > 0) {
				placed0[0] = 1;   // it has the load on its blade
			}
			if (state[0] == 1 && dz != null && (placed0[0] > 0 && dz.status().startsWith("spreading: blade empty") || dz.getZ() - q.getZ() < LIFT_EDGE - 12 || dz.status().startsWith("at the crest"))) {
				keys(dz, Input.EMPTY);
				state[0] = 2;
				state[1] = k;
				event("dozer stopped: " + dz.status() + " at z " + f1(dz.getZ() - q.getZ()));
			}
			if (state[0] == 2 && k > state[1] + 80) {
				endMove = true;
			}
			card(dozerCard(level, k, "Lift: 1 m of dry tails"));
		});
		note = "";
		showCard(false);
		server.runOnServer(srv -> {
			keys(dozer(srv.overworld()), Input.EMPTY);
			int n = 0;
			for (int x = -15; x <= 45; x++) {
				for (int z = LIFT_EDGE - 20; z <= LIFT_EDGE; z++) {
					if (srv.overworld().getBlockState(d(x, -1, z)).is(TailingsBlocks.DRY_TAILS)) {
						n++;
					}
				}
			}
			log("the lift grew past its edge by " + n + " blocks");
			HaulTruckEntity t = truck(srv.overworld());
			if (t != null) {
				t.discard();   // off the stack's films from here (it would come back for another load)
			}
		});
	}

	// ---- Tim's decisions of 2026-11-23: compaction and the slump, the autopilot, the bin's level ---------------------------------------

	private int standing(ServerLevel level, int x0, int x1) {
		int n = 0;
		for (int x = x0; x <= x1; x++) {
			if (level.getBlockState(d(x, 0, LIFT_EDGE + 1)).is(TailingsBlocks.DRY_TAILS)) {
				n++;
			}
		}
		return n;
	}

	/**
	 * hi04_slump: driven by the scene's keys along the lift's edge, its blade raised, the dozer's tracks compact the loose cake (pale, in
	 * clods) to compacted (dark, smooth); then the scene lays a row of cake flush on the edge where it passed and on the loose edge to the
	 * west: on the loose edge the row slumps off, on the compacted edge it stands.
	 */
	private void slumpFilm() {
		placeDozer(QX + 4.5, LIFT_EDGE + 3.6, 270, TrackDozerEntity.Blade.RAISED);
		showCard(true);
		int[] st = {0, -1};
		film("hi04_slump", 1300, via(qcam(2, 10, -47, 4, -1, -34), qcam(2, 6, -48, 2, -1, -36), qcam(2, 4, -47, 2, -0.5, -36), 1300), (level, k) -> {
			TrackDozerEntity dz = dozer(level);
			if (k == PAD) {
				keys(dz, FORWARD);
				note = "-Driven by the scene's keys, blade raised";
				event("dozer forward along the edge");
				st[0] = 1;
			}
			if (st[0] == 1 && dz != null && dz.getX() - q.getX() >= 17) {
				keys(dz, Input.EMPTY);
				st[0] = 2;
				st[1] = k;
				event("dozer stopped: compacted " + dz.compactedBlocks());
			}
			if (st[0] == 2 && k == st[1] + 100) {
				for (int x = -8; x <= -4; x++) {
					level.setBlock(d(x, 0, LIFT_EDGE + 1), TailingsBlocks.DRY_TAILS.defaultBlockState(), Block.UPDATE_ALL);
				}
				for (int x = 8; x <= 12; x++) {
					level.setBlock(d(x, 0, LIFT_EDGE + 1), TailingsBlocks.DRY_TAILS.defaultBlockState(), Block.UPDATE_ALL);
				}
				note = "-Rows laid by the scene, as a truck tips";
				event("rows laid on the loose and the compacted edge");
				st[0] = 3;
			}
			if (st[0] == 3 && k > st[1] + 420) {
				endMove = true;
			}
			card("^DRY STACK · LOOSE AND COMPACTED CAKE", dz == null ? "Dozer: -" : "Dozer: compacted " + dz.compactedBlocks() + " blocks under its tracks",
					st[0] >= 3 ? "Row on the loose edge: " + standing(level, -8, -4) + " of 5 standing" : "West: loose, as laid",
					st[0] >= 3 ? "Row on the compacted edge: " + standing(level, 8, 12) + " of 5 standing" : "East: compacted by its pass", note);
		});
		note = "";
		showCard(false);
		server.runOnServer(srv -> log("slump: loose row " + standing(srv.overworld(), -8, -4) + " of 5 standing, compacted row " + standing(srv.overworld(), 8, 12) + " of 5"));
	}

	/**
	 * hi04_auto: two DOZE AREA stakes at the corners of a 20 x 34 m area over the lift's north part and past its edge; started as the
	 * survey book starts it, the dozer works the area by itself: lanes a blade wide, pushing north and spreading at the lift's edge, back
	 * in reverse to the next lane, its tracks compacting the lift. Two tipped piles are laid by the scene.
	 */
	private void autoFilm() {
		placeDozer(QX + 16.5, -16.5, 180, TrackDozerEntity.Blade.RAISED);
		server.runOnServer(srv -> {
			ServerLevel level = srv.overworld();
			BlockPos a = d(14, 0, -12), b = d(33, -1, -45);
			for (BlockPos at : List.of(a, b)) {
				level.setBlock(at, TailingsBlocks.SURVEY_STAKE.defaultBlockState().setValue(SurveyStakeBlock.KIND, StopKind.DOZE_AREA), Block.UPDATE_ALL);
			}
			int[][] piles = {{20, 23, -25, -22}, {28, 32, -30, -27}};
			for (int[] p : piles) {
				for (int x = p[0]; x <= p[1]; x++) {
					for (int z = p[2]; z <= p[3]; z++) {
						level.setBlock(d(x, 0, z), TailingsBlocks.DRY_TAILS.defaultBlockState(), Block.UPDATE_ALL);
						if (x > p[0] && x < p[1] && z > p[2] && z < p[3]) {
							level.setBlock(d(x, 1, z), TailingsBlocks.DRY_TAILS.defaultBlockState(), Block.UPDATE_ALL);
						}
					}
				}
			}
			TrackDozerEntity dz = dozer(level);
			log("autopilot: " + dz.setAutoArea(level, List.of(a, b)) + "; " + dz.status());
		});
		showCard(true);
		int[] lastLane = {-1};
		film("hi04_auto", 2600, move(qcam(5, 11, -46, 20, -1, -28), qcam(7, 10, -40, 22, -1, -28), 2600), (level, k) -> {
			TrackDozerEntity dz = dozer(level);
			if (k == PAD) {
				note = "-Piles laid by the scene, as trucks tip them";
			}
			if (dz != null && dz.lane() != lastLane[0]) {
				lastLane[0] = dz.lane();
				event("autopilot lane " + (dz.lane() + 1) + ", pass " + (dz.passes() + 1));
			}
			card(dozerCard(level, k, dz == null ? "" : String.format(Locale.ROOT, "Autopilot: lane %d of %d, pass %d", dz.lane() + 1, dz.lanes(), dz.passes() + 1),
					"No one in the cab: two DOZE AREA stakes", dz == null ? "" : "Compacted: " + dz.compactedBlocks() + " blocks under its tracks"));
		});
		note = "";
		showCard(false);
		server.runOnServer(srv -> {
			TrackDozerEntity dz = dozer(srv.overworld());
			log("autopilot after the film: " + dz.status() + "; lane " + (dz.lane() + 1) + ", pass " + (dz.passes() + 1) + ", compacted " + dz.compactedBlocks());
			dz.stopAuto("autopilot stopped");
		});
	}

	/** hi04_bin: the bin's sight windows as the gate fills a truck under it: the level falls (the bin stocked before filming). */
	private void binFilm() {
		if (!wanted("hi04_bin")) {
			return;
		}
		server.runOnServer(srv -> {
			ServerLevel level = srv.overworld();
			bin.setDryKg(Math.max(bin.dryKg(), 160_000));
			HaulTruckEntity t = TailingsEntities.HAUL_TRUCK.create(level, EntitySpawnReason.COMMAND);
			t.setPos(q.getX() + 19.5, q.getY(), q.getZ() + 34);
			t.setYRot(0);
			t.setFuel(t.tank());
			level.addFreshEntity(t);
			log("truck for the bin film: " + t.takeRoute(level, truckRoute));
			truckId = t.getUUID();
		});
		ticks(20);
		boolean near = waitFor(4400, level -> {
			HaulTruckEntity t = truck(level);
			return t == null || t.status().startsWith("loading") || t.status().startsWith("waiting to load");
		});
		log("bin film: truck at the gate " + near);
		showCard(true);
		int[] full = {-1};
		film("hi04_bin", 2000, move(qcam(31, 14.5, 71, 25, 14.5, 68), qcam(30.5, 14.3, 70, 25, 14.5, 68), 2000), (level, k) -> {
			HaulTruckEntity t = truck(level);
			if (k == PAD + 10) {
				note = "-Bin stocked before filming";
			}
			if (t != null && full[0] < 0 && t.load().room() <= 0) {
				full[0] = k;
				event("truck full; bin " + f1(bin.dryKg() / 1000) + " t");
			}
			if (full[0] > 0 && k > full[0] + 60) {
				endMove = true;
			}
			card("^LOAD-OUT BIN · 400 T", String.format(Locale.ROOT, "Holds: %.1f t (%.0f %%)", bin.dryKg() / 1000, bin.dryKg() / DryStack.BIN_KG * 100),
					"Windows: a mark every 100 t", frame.running() ? "+Gate: filling the truck at 2 t/s" : "Gate: waiting for a truck", note);
		});
		note = "";
		showCard(false);
		server.runOnServer(srv -> {
			HaulTruckEntity t = truck(srv.overworld());
			if (t != null) {
				t.discard();
			}
		});
	}

	/** hi04_power: the diesel set west of the road and its run to the press and the gate. */
	private void powerFilm() {
		showCard(true);
		film("hi04_power", 700, move(qcam(-34, 8, 14, -16, 3, 40), qcam(-36, 10, 22, -12, 6, 44), 700), (level, k) -> card("^POWER · DIESEL SET, 500 kW", setLine(level, 0, "Set west of the road"),
				rd(filter, "Filter press", "150 kW"), frame.running() ? "+Load-out gate (7.5 kW): " + f0(amps(frame)) + " A" : "Load-out gate: stopped", setLine(level, 1, "Set by the thickener"),
				rd(thick, "Thickener", "11 kW")));
		showCard(false);
	}

	/** hi04_drop: the end of a cycle in real time: the press opens and drops its cake into the bin. */
	private void dropFilm() {
		if (!wanted("hi04_drop")) {
			return;   // it waits up to a whole press cycle for the drop
		}
		// the last plates shifted at the head end, the drop, and the press closing (the hold comes 25 s into the next cycle)
		boolean ok = waitFor(60000, level -> filter.cycles() >= 1 && !filter.pressHeld() && filter.cycle() / 20 >= 866);
		log("press near the end of a full batch: " + ok + ", " + filter.status());
		showCard(true);
		int[] seen = {-1};
		// from above the head end, looking down into the last chambers as the gap opens along the pack and their cakes fall into the
		// trough (fix2: from the side, the trough's wall hid the falling cake; the lumps read only as specks)
		film("hi04_drop", 1000, move(qcam(23.5, 27.5, 55.5, 20, 21.2, 62.5), qcam(23, 27, 56.5, 20, 21.2, 62.5), 1000), (level, k) -> {
			if (seen[0] < 0) {
				seen[0] = filter.cycles();
			}
			if (filter.cycles() > seen[0] && !note.startsWith("+")) {
				note = "+The cake dropped into the bin";
				event("cake dropped: bin " + f1(bin.dryKg() / 1000) + " t");
			}
			card(pressCard(level));
		});
		note = "";
		showCard(false);
	}

	/** hi04_stackwide: from the air over the dry stack: the lift and its new edge, the dozer, the load-out and press, the thickener. */
	private void stackWideFilm() {
		// from north of the lift's edge, low (the first take was 75 m out and 40 m up): the lift and its new strip in front, the dozer on
		// it, the load-out, the press and the thickener beyond the road
		film("hi04_stackwide", 1200, orbitAround(q, 19, 0, -28, 30, 9, 160, 45, 1200), null);
	}
}
