package dev.wildcatter.tailings.fabric.gametest;

import dev.wildcatter.WildcatterBlocks;
import dev.wildcatter.block.BigPartBlock;
import dev.wildcatter.block.GasTurbineBlockEntity;
import dev.wildcatter.block.GeneratorPartBlockEntity;
import dev.wildcatter.block.GensetControlPanelBlockEntity;
import dev.wildcatter.block.GensetPartBlockEntity;
import dev.wildcatter.block.GensetSkid;
import dev.wildcatter.block.StorageTankBlockEntity;
import dev.wildcatter.control.Measurable;
import dev.wildcatter.energy.ConductorSize;
import dev.wildcatter.fluid.Product;
import dev.wildcatter.multiblock.BigParts;
import dev.wildcatter.tailings.TailingsBlocks;
import dev.wildcatter.tailings.TailingsEntities;
import dev.wildcatter.tailings.TailingsItems;
import dev.wildcatter.tailings.block.HaulDispatchBlockEntity;
import dev.wildcatter.tailings.block.MuckBlockEntity;
import dev.wildcatter.tailings.block.PitSwitchHouseBlockEntity;
import dev.wildcatter.tailings.block.PlantBlockEntity;
import dev.wildcatter.tailings.block.RopeShovelBlockEntity;
import dev.wildcatter.tailings.block.SurveyStakeBlock;
import dev.wildcatter.tailings.block.TailingsPartBlock;
import dev.wildcatter.tailings.entity.HaulTruckEntity;
import dev.wildcatter.tailings.entity.RopeShovelEntity;
import dev.wildcatter.tailings.entity.UltraHaulTruckEntity;
import dev.wildcatter.tailings.fabric.gametest.Trailer.Cam;
import dev.wildcatter.tailings.item.TrailingCableItem;
import dev.wildcatter.tailings.pit.PitMass;
import dev.wildcatter.tailings.pit.RopeShovelLayout;
import dev.wildcatter.tailings.pit.StopKind;
import dev.wildcatter.tailings.plant.Machine;
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
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.Vec3;

/**
 * Heavy Iron, episode 2, "Rope shovel and ultra truck" (TAILINGS_ONLY=hi02): the 4100-class electric rope shovel
 * (docs/multiblocks/open-pit-p5-big-machines.md, P5d) loading the 363 t ultra truck (P5b) from a muck pile in an open pit; the truck
 * drives itself on a surveyed route (open-pit.md, P2a) up a 10 % ramp to a waste dump, backs to the berm and tips over the crest; a
 * haul dispatch office (P2b) counts its loads. On real power: a Wildcatter 16.5 MW gas turbine set, its starter's diesel set, a unit
 * auxiliary transformer down to the medium voltage, one 500 kcmil MV cable to a pit switch house on the pit's rim, the shovel's
 * trailing cable from the house's first outlet.
 * <ul>
 * <li>the site: a pit 12 m deep in rock under 4 m of soil; a muck pile against its north bench (the blast staged: blasting is the
 * next episode); a ramp 120 m long at 10 % up its south end; the waste dump's crest 60 m beyond its top, with a 2 m berm;</li>
 * <li>the films: the shovel as its six parts and lifted; the ultra beside a 100 t truck with a villager at its wheel; the ultra
 * backing in; its four passes in real time; the haul up the ramp; the tip over the crest; the dispatch office; the dipper in the face;
 * the swing; the power yard; the shovel beside a villager; a wide.</li>
 * </ul>
 * Filmed in real time at 2880 x 1620 with a readout card read from the machines each tick. HI02_SHOTS=a,b films only those.
 * <p>
 * Staged, and said so in the episode: the muck pile (laid by the scene, not blasted); the turbine's purge and run-up skipped off camera;
 * the fuel in its tank and the sets'; the villagers; the 100 t truck parked; the dispatch office's 5 kW on its test feed (it is not a
 * motor; its radio and counts are its own). If the shovel works its face out of reach it is moved on 4 m into the muck between films.
 */
final class Hi02Scenes {
	private static final int W = 1440, H = 810;
	private static final int PAD = 60;
	private static final String SEED = System.getenv().getOrDefault("TAILINGS_TRAILER_SEED", "tailings");
	private static final String LV = "wildcatter:cable_industrial", BUS = "wildcatter:busway_industrial", MV = "wildcatter:cable_mv",
			DUCT = "wildcatter:bus_duct_gen";
	/** The pit's floor (first air) 12 m under the ground's first air at o's y. */
	private static final int PF = -12;
	/** The pit: x -20 to 75, z -15 to 64; the bench behind the muck at z -16; the ramp x -5 to 55 (a 60 m haul road: two lanes of ultras and room to line up at its foot) from z 65 to 184. */
	private static final int PX0 = -20, PX1 = 75, PZ0 = -15, PZ1 = 64, RX0 = -5, RX1 = 55, RZ1 = 184;
	/** The dump's crest: the ground ends at z 243 (the berm on it), the dump's floor 12 m down beyond. */
	private static final int CREST = 243;
	/** The shovel's crawlers' main cell: its centre at (7, 10), facing north into the muck. */
	private static final int SHX = 0, SHZ = 4;
	/** The route's stakes: LOAD on the shovel's right, the turn before it, the ramp's foot and top, the turn and the DUMP POINT. */
	private static final int[][] STAKES = {{25, PF, 4}, {25, PF, 40}, {25, PF, 62}, {25, 0, 188}, {25, 0, 200}, {25, 0, CREST - 2}};

	private final ClientGameTestContext context;
	private TestServerContext server;
	/** o's y is the ground's first air; the pit's floor is at y PF from it. */
	private BlockPos o;
	private ConductorSize runSize = ConductorSize.KCMIL_500;
	private final List<BlockPos> panels = new ArrayList<>();
	private final List<String> summary = new ArrayList<>();
	private final Set<BlockPos> boxCells = new HashSet<>();
	private final List<int[]> yards = new ArrayList<>();
	private final List<BlockPos> tanks = new ArrayList<>();
	private long filmStart;
	private volatile boolean endMove;
	private int ft;
	private float lastYaw = Float.NaN;
	private int keeps;
	private volatile boolean filming;

	private BlockPos turbine, house, uat, office, crawlers;
	private UUID shovel, ultra, small;
	private final List<BlockPos> route = new ArrayList<>();
	private double lastFuel = Double.NaN;
	private double engineKw;
	private long passStart = -1;
	private boolean wasDigging;

	Hi02Scenes(ClientGameTestContext context) {
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
			cardWidth("Draw at the switch house: 3,500 kW, 1,000 A");
			try {
				stage();
				power();
				buildShovel();
				parkForScale();
				partsFilm();
				scaleTruckFilm();
				startRoute();
				spotFilm();
				passesFilm();
				haulFilm();
				dumpFilm();
				dispatchFilm();
				// the second load: the dipper close, the swing, the power yard; then the shovel beside a villager
				untilTruck("loading", 6000);
				digFilm();
				powerFilm();
				swingFilm();
				untilShovelIdle(4000);
				scaleShovelFilm();
				// the third load, from the air
				untilTruck("loading", 6000);
				wideFilm();
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

	private boolean noGo(BlockPos c) {
		return false;
	}

	private BlockPos d(int x, int y, int z) {
		return o.offset(x, y, z);
	}

	private Cam dcam(double x, double y, double z, double ax, double ay, double az) {
		return Cam.of(o, x, y, z, ax, ay, az);
	}

	private static void log(String line) {
		System.out.println("[hi02] " + line);
	}

	private static void require(boolean ok, String what) {
		if (!ok) {
			throw new AssertionError("[hi02] " + what);
		}
	}

	private static boolean wanted(String id) {
		String only = System.getenv("HI02_SHOTS");
		return only == null || only.isBlank() || java.util.Arrays.asList(only.split(",")).contains(id);
	}

	// ---- window, card, camera, films (as Rtm04Scenes) -----------------------------------------------------------------------------

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

	private static String hoa(PlantBlockEntity part) {
		return HOA.getOrDefault(part.getBlockPos(), "HAND");
	}

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

	// ---- building -----------------------------------------------------------------------------------------------------------------

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

	private static Item item(String id) {
		return BuiltInRegistries.ITEM.getValue(Identifier.parse(id));
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

	/** As Rtm04Scenes.route: a run through free cells (fewest turns, then shortest, then low), clear of other runs of its kind. */
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
		minY = minY - 2;
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
				int cost = 10 + (d == dir || cell == start ? 0 : 40) + (q2.getY() > o.getY() ? 2 : 0);
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

	/** Any power conductor: busway and cable of the same voltage join, so a run keeps clear of every other net's. */
	private static boolean conductor(Block b) {
		return b == wc(LV) || b == wc(BUS) || b == wc(MV);
	}

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
		List<BlockPos> cellsOf = route(level, id, from, goals, own);
		for (int i = hold; i < cellsOf.size(); i++) {
			lay(level, id, runSize, cellsOf.get(i));
		}
		own.addAll(cellsOf);
		log(id + " run of " + cellsOf.size() + " m from " + from.toShortString() + " to " + cellsOf.get(cellsOf.size() - 1).toShortString());
		return cellsOf;
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
		yards.add(new int[] {panel.getX() - 8, panel.getX(), panel.getZ() - 5, panel.getZ() + 9});
		return panel;
	}

	/** A unit substation run backward east of a set's panel: the industrial voltage in, the medium voltage out. Its MV cell. */
	private BlockPos stepUp(ServerLevel level, BlockPos panel) {
		BlockState state = wc("wildcatter:unit_substation").defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.SOUTH);
		BlockPos main = panel.east(4);
		level.setBlock(main, state, Block.UPDATE_ALL);
		BigParts.grow(level, main, state);
		yards.add(new int[] {panel.getX(), panel.getX() + 4, panel.getZ() - 5, panel.getZ() + 9});
		return panel.east(5);
	}

	/** Runs from {@code out} to each box in turn, all one net, in {@code id} at {@code size}. */
	private void wire(ServerLevel level, String id, ConductorSize size, BlockPos out, BlockPos... boxes) {
		ConductorSize was = runSize;
		runSize = size;
		Set<BlockPos> net = new HashSet<>();
		boolean first = true;
		for (BlockPos b : boxes) {
			run(level, id, b, first ? out : null, net, 0);
			first = false;
		}
		runSize = was;
	}

	// ---- the site -----------------------------------------------------------------------------------------------------------------

	/** The ramp's top block at z: the pit's floor at its foot, a block up every 10 m, the ground's at its top (10 %). */
	private static int rampTop(int z) {
		return PF - 1 + Math.max(0, Math.min(12, (z - PZ1) / 10));
	}

	/** The muck pile's height at z (blocks over the pit's floor): its toe at z -5, rising 1.5 in 1 to the bench's 12 m. */
	private static int muckHeight(int z) {
		return z > -5 || z < PZ0 ? 0 : (int) Math.min(12, Math.round(1.5 * (-4 - z)));
	}

	/** Lays the pit: rock under 4 m of soil, the pit and its ramp, the muck against the bench, the dump's crest, its berm. */
	private void ground(ServerLevel level) {
		BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
		BlockState air = Blocks.AIR.defaultBlockState();
		for (int x = -125; x <= 95; x++) {
			for (int z = -45; z <= 305; z++) {
				boolean pit = x >= PX0 && x <= PX1 && z >= PZ0 && z <= PZ1;
				boolean ramp = x >= RX0 && x <= RX1 && z > PZ1 && z <= RZ1;
				boolean dump = x >= PX0 - 20 && x <= PX1 + 15 && z > CREST;
				for (int y = -22; y <= -1; y++) {
					BlockState st;
					if (y <= -5) {
						st = (Math.floorMod(y, 5) == 0 ? Blocks.ANDESITE : Math.floorMod(x * 7 + y * 13 + z * 5, 11) == 0 ? Blocks.TUFF : Blocks.STONE).defaultBlockState();
					} else if (y == -1) {
						st = Blocks.GRASS_BLOCK.defaultBlockState();
					} else {
						st = Blocks.DIRT.defaultBlockState();
					}
					if ((pit || dump) && y >= PF) {
						st = air;
					} else if ((pit || dump) && y == PF - 1 && Math.floorMod(x * 31 + z * 17, 5) == 0) {
						st = Blocks.GRAVEL.defaultBlockState();
					}
					if (ramp && y > rampTop(z)) {
						st = air;
					} else if (ramp && y == rampTop(z)) {
						st = (Math.floorMod(x * 31 + z * 17, 3) == 0 ? Blocks.GRAVEL : Blocks.STONE).defaultBlockState();
					}
					// the haul road on top, from the ramp to the crest: gravel
					if (!ramp && !pit && !dump && y == -1 && x >= RX0 - 10 && x <= RX1 + 10 && z > RZ1 && z <= CREST) {
						st = (Math.floorMod(x * 31 + z * 17, 4) == 0 ? Blocks.COARSE_DIRT : Blocks.GRAVEL).defaultBlockState();
					}
					level.setBlock(p.set(o.getX() + x, o.getY() + y, o.getZ() + z), st, Block.UPDATE_CLIENTS);
				}
			}
		}
		// the muck, blasted rock against the north bench (staged: laid by the scene), a muck block of 3 quarters each
		int muck = 0;
		for (int x = -6; x <= 20; x++) {
			for (int z = PZ0; z <= -5; z++) {
				int h = muckHeight(z);
				for (int y = PF; y < PF + h; y++) {
					BlockPos at = o.offset(x, y, z);
					level.setBlock(at, TailingsBlocks.MUCK.defaultBlockState(), Block.UPDATE_CLIENTS);
					if (level.getBlockEntity(at) instanceof MuckBlockEntity m) {
						m.set(Map.of(Math.floorMod(x * 13 + y * 7 + z * 3, 4) == 0 ? "minecraft:andesite" : "minecraft:cobblestone", PitMass.MUCK_QUARTERS));
						muck++;
					}
				}
			}
		}
		// the berm along the crest: 2 m, mid-axle on the ultra's tyres
		for (int x = PX0 - 20; x <= PX1 + 15; x++) {
			for (int y = 0; y <= 1; y++) {
				level.setBlock(p.set(o.getX() + x, o.getY() + y, o.getZ() + CREST), Blocks.COARSE_DIRT.defaultBlockState(), Block.UPDATE_CLIENTS);
			}
		}
		log("ground laid: " + muck + " muck blocks");
	}

	private void stage() {
		server.runOnServer(srv -> {
			ServerLevel level = srv.overworld();
			BlockPos s = Trailer.site(level, -2400, 2600, 220, 350);
			int g = s.getY();
			o = new BlockPos(s.getX() + 125, g, s.getZ() + 45);
			Trailer.pad(level, o.getX() - 125, o.getZ() - 45, o.getX() + 95, o.getZ() + 305, g, 18, 70);
			Trailer.forceChunks(level, o.getX() - 160, o.getZ() - 80, o.getX() + 130, o.getZ() + 340);
			ground(level);
			// the survey stakes of the haul route
			for (int i = 0; i < STAKES.length; i++) {
				StopKind kind = i == 0 ? StopKind.LOAD : i == STAKES.length - 1 ? StopKind.DUMP_POINT : StopKind.WAYPOINT;
				BlockPos at = o.offset(STAKES[i][0], STAKES[i][1], STAKES[i][2]);
				level.setBlock(at, TailingsBlocks.SURVEY_STAKE.defaultBlockState().setValue(SurveyStakeBlock.KIND, kind), Block.UPDATE_ALL);
				route.add(at.immutable());
			}
			// the haul dispatch office east of the ramp's top (its 5 kW on its test feed: decision in films-hi02.md)
			office = o.offset(66, 0, 176);
			Trailer.part(level, TailingsBlocks.HAUL_DISPATCH, office);
			HaulDispatchBlockEntity d = (HaulDispatchBlockEntity) level.getBlockEntity(office);
			d.forcePowered(true);
			log("dispatch: " + d.addRoute(route));
			log("site at " + o.toShortString());
		});
		server.runCommand("kill @e[type=item]");
	}

	// ---- power: a gas turbine set, stepped down to the medium voltage, to a pit switch house on the rim ---------------------------

	private void power() {
		server.runOnServer(srv -> {
			ServerLevel level = srv.overworld();
			Trailer.fillBox(level, o, Blocks.SMOOTH_STONE, -112, -26, -1, -1, -20, 34);
			Trailer.fillBox(level, o, Blocks.AIR, -112, -26, 0, 14, -20, 34);
			turbine = d(-75, 0, -10);
			turbineSet(level, turbine);
			BlockPos ds = set(level, d(-104, 0, 4), false);
			BlockPos dMv = stepUp(level, ds);
			run(level, MV, turbine.offset(-1, 0, 2), dMv, new HashSet<>(), 0);
			uat = d(-44, 0, 10);
			big(level, wc("wildcatter:unit_auxiliary_transformer"), uat, Direction.NORTH);
			run(level, DUCT, uat.offset(0, 4, -3), turbine.offset(5, 0, 9), new HashSet<>(), 0);
			house = d(-36, 0, 24);
			Trailer.part(level, TailingsBlocks.PIT_SWITCH_HOUSE, house);
			// one 500 kcmil MV cable, 450 A: the shovel's 3.5 MW hoist is about 530 A for 12 s of each 35 s cycle, under its burn rise
			runSize = ConductorSize.MV_500;
			run(level, MV, house.north(), uat.south(), new HashSet<>(), 0);
			log("yard: turbine " + turbine.toShortString() + ", UAT " + uat.toShortString() + ", switch house " + house.toShortString());
		});
		context.waitTicks(40);
		server.runOnServer(srv -> {
			for (BlockPos panel : panels) {
				((GensetControlPanelBlockEntity) srv.overworld().getBlockEntity(panel)).setWantRun(true);
			}
			keep(srv);
		});
		ticks(400);
		// the turbine starts on its starter set; its purge and run-up (minutes) are skipped, off camera
		server.runOnServer(srv -> ((GasTurbineBlockEntity) srv.overworld().getBlockEntity(turbine)).setWantRun(true));
		ticks(200);
		server.runOnServer(srv -> ((GasTurbineBlockEntity) srv.overworld().getBlockEntity(turbine)).skipStartForTest());
		ticks(400);
		server.runOnServer(srv -> status("turbine started", srv));
	}

	private static void big(ServerLevel level, Block block, BlockPos main, Direction facing) {
		BlockState state = block.defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, facing);
		level.setBlock(main, state, Block.UPDATE_ALL);
		BigParts.grow(level, main, state);
	}

	/** As Rtm02Scenes.turbineSet: the 16.5 MW set facing north at t, its kerosene tank west; starter box at t + (-1, 0, 2), bus duct from t + (5, 0, 9). */
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

	private static GeneratorPartBlockEntity gen(ServerLevel level, BlockPos turbine) {
		return level.getBlockEntity(turbine.offset(4, 0, 1)) instanceof GeneratorPartBlockEntity g ? g : null;
	}

	private void status(String what, MinecraftServer srv) {
		ServerLevel level = srv.overworld();
		log(what + ":");
		for (BlockPos panel : panels) {
			log("  set " + panel.toShortString() + ": " + ((GensetControlPanelBlockEntity) level.getBlockEntity(panel)).inspect().getString());
		}
		if (level.getBlockEntity(turbine) instanceof GasTurbineBlockEntity gt) {
			log("  turbine: " + gt.inspect().getString());
			log("  generator: " + (gen(level, turbine) == null ? "none" : gen(level, turbine).inspect().getString()));
		}
		if (level.getBlockEntity(house) instanceof PitSwitchHouseBlockEntity h) {
			log("  house: " + h.inspect().getString());
		}
		RopeShovelEntity s = shovel(level);
		if (s != null) {
			log("  shovel: " + s.status());
		}
		UltraHaulTruckEntity t = truck(level);
		if (t != null) {
			log(String.format(Locale.ROOT, "  ultra: %s; %s; at %.1f, %.1f, %.1f; %.1f km/h; fuel %.0f L", t.status(), t.load().tonnes(), t.getX() - o.getX(),
					t.getY() - o.getY(), t.getZ() - o.getZ(), t.speed() * 3.6, t.fuel()));
		}
	}

	private PitSwitchHouseBlockEntity houseBe(ServerLevel level) {
		return level.getBlockEntity(house) instanceof PitSwitchHouseBlockEntity h ? h : null;
	}

	private RopeShovelEntity shovel(ServerLevel level) {
		return shovel != null && level.getEntity(shovel) instanceof RopeShovelEntity s ? s : null;
	}

	private UltraHaulTruckEntity truck(ServerLevel level) {
		return ultra != null && level.getEntity(ultra) instanceof UltraHaulTruckEntity t ? t : null;
	}

	private HaulDispatchBlockEntity officeBe(ServerLevel level) {
		return level.getBlockEntity(office) instanceof HaulDispatchBlockEntity d ? d : null;
	}

	// ---- the shovel and the trucks -----------------------------------------------------------------------------------------------

	/** The rope shovel's six parts on the pit's floor facing the muck, its trailing cable from the house's first outlet, closed. */
	private void buildShovel() {
		server.runOnServer(srv -> {
			ServerLevel level = srv.overworld();
			crawlers = d(SHX, PF, SHZ);
			for (RopeShovelLayout.Part part : RopeShovelLayout.Part.values()) {
				int[] off = part.offset();
				Trailer.part(level, TailingsBlocks.plantBlock(part.machine()), crawlers.offset(off[0], off[1], off[2]));
			}
			PitSwitchHouseBlockEntity h = houseBe(level);
			ItemStack reel = new ItemStack(TailingsItems.TRAILING_CABLE);
			log("reel: " + TrailingCableItem.pickUp(reel, h));
			long[] from = TrailingCableItem.from(reel);
			RopeShovelBlockEntity c = (RopeShovelBlockEntity) level.getBlockEntity(crawlers);
			log("cable: " + c.couple(level, BlockPos.of(from[0]), (int) from[1]) + "; outlet " + h.close(0));
			shovel = c.machine();
			log("crawlers: " + c.status());
		});
		server.runCommand(String.format(Locale.ROOT, "summon villager %d %d %d {NoAI:1b,Silent:1b,Invulnerable:1b,Rotation:[200f,0f],Tags:[\"hi02a\"]}", o.getX() - 2, o.getY() + PF,
				o.getZ() + 3));
		ticks(40);
	}

	/** RUN: the parts lift into one machine; it works by itself at the trucks that come. */
	private String lift(ServerLevel level) {
		RopeShovelBlockEntity c = level.getBlockEntity(crawlers) instanceof RopeShovelBlockEntity r ? r : null;
		if (c == null) {
			return "no crawlers";
		}
		String said = c.run(level);
		if (level.getEntity(shovel) instanceof RopeShovelEntity s) {
			s.setAutoHere();
		}
		// the boom, handle and dipper were laid on the ground ahead, in the muck and the bench: their cells are filled again
		int filled = 0;
		for (int x = SHX - 4; x <= SHX + 20; x++) {
			for (int z = SHZ - 34; z <= SHZ - 8; z++) {
				for (int y = PF; y <= PF + 5; y++) {
					BlockPos at = o.offset(x, y, z);
					if (!level.getBlockState(at).isAir()) {
						continue;
					}
					if (z < PZ0 && x >= PX0 && x <= PX1) {
						level.setBlock(at, Blocks.STONE.defaultBlockState(), Block.UPDATE_ALL);
						filled++;
					} else if (y - PF < muckHeight(z) && x >= -6 && x <= 20) {
						level.setBlock(at, TailingsBlocks.MUCK.defaultBlockState(), Block.UPDATE_ALL);
						if (level.getBlockEntity(at) instanceof MuckBlockEntity m) {
							m.set(Map.of("minecraft:cobblestone", PitMass.MUCK_QUARTERS));
						}
						filled++;
					}
				}
			}
		}
		log("refilled " + filled + " cells where the boom, handle and dipper lay");
		return said;
	}

	/** For scale: the ultra parked on the pit's floor beside a 100 t truck, a villager at its front wheel and one on its deck. */
	private void parkForScale() {
		server.runOnServer(srv -> {
			ServerLevel level = srv.overworld();
			UltraHaulTruckEntity t = TailingsEntities.ULTRA_HAUL_TRUCK.create(level, EntitySpawnReason.COMMAND);
			t.setPos(o.getX() + 50.5, o.getY() + PF, o.getZ() + 26.5);
			t.setYRot(90);
			t.setFuel(t.tank());
			level.addFreshEntity(t);
			ultra = t.getUUID();
			HaulTruckEntity s = TailingsEntities.HAUL_TRUCK.create(level, EntitySpawnReason.COMMAND);
			s.setPos(o.getX() + 50.5, o.getY() + PF, o.getZ() + 45.5);
			s.setYRot(90);
			s.setFuel(s.tank());
			level.addFreshEntity(s);
			small = s.getUUID();
			Vec3 wheel = t.local(-6.6, 0, 4.6);
			Vec3 deck = t.local(-2.4, 4.6, 6.6);
			log(String.format(Locale.ROOT, "ultra at its wheel %.1f, %.1f; deck %.1f, %.1f, %.1f", wheel.x - o.getX(), wheel.z - o.getZ(), deck.x - o.getX(), deck.y - o.getY(), deck.z - o.getZ()));
			srv.getCommands().performPrefixedCommand(srv.createCommandSourceStack().withSuppressedOutput(), String.format(Locale.ROOT,
					"summon villager %.2f %.2f %.2f {NoAI:1b,Silent:1b,Invulnerable:1b,Rotation:[160f,0f],Tags:[\"hi02b\"]}", wheel.x, wheel.y, wheel.z));
			srv.getCommands().performPrefixedCommand(srv.createCommandSourceStack().withSuppressedOutput(), String.format(Locale.ROOT,
					"summon villager %.2f %.2f %.2f {NoAI:1b,Silent:1b,Invulnerable:1b,Rotation:[160f,0f],Tags:[\"hi02b\"]}", deck.x, deck.y, deck.z));
		});
		ticks(20);
	}

	/** The scale shot over: the 100 t truck and the truck's villagers go; the ultra takes the route. */
	private void startRoute() {
		server.runCommand("kill @e[tag=hi02b]");
		server.runOnServer(srv -> {
			ServerLevel level = srv.overworld();
			if (small != null && level.getEntity(small) instanceof Entity e) {
				e.discard();
			}
			UltraHaulTruckEntity t = truck(level);
			log("ultra takes the route: " + t.takeRoute(level, route));
		});
		ticks(20);
		server.runOnServer(srv -> status("route taken", srv));
	}

	/** Waits (real ticks, keeping the stand-ins up) until the ultra's status starts with {@code what}: first out of it, then into it. */
	private void untilTruck(String what, int most) {
		java.util.function.BooleanSupplier now = () -> server.computeOnServer(srv -> {
			UltraHaulTruckEntity t = truck(srv.overworld());
			return t != null && t.status().startsWith(what);
		});
		int i = 0;
		for (; i < most && now.getAsBoolean(); i += 10) {
			ticks(10);
		}
		for (; i < most; i += 10) {
			if (now.getAsBoolean()) {
				return;
			}
			ticks(10);
		}
		log("the ultra never got to " + what);
		server.runOnServer(srv -> status("waited for " + what, srv));
	}

	/** Waits until the shovel starts a dig (its dipper into the face). */
	private void untilDig(int most) {
		for (int i = 0; i < most; i++) {
			boolean d = server.computeOnServer(srv -> {
				RopeShovelEntity s = shovel(srv.overworld());
				return s != null && s.digging();
			});
			if (d) {
				return;
			}
			if (i % 20 == 0) {
				moveOnIfWorkedOut();
			}
			ticks(1);
		}
		log("no dig");
	}

	/** Waits until a dig ends (it swings to the truck). */
	private void untilSwing(int most) {
		for (int i = 0; i < most; i++) {
			boolean d = server.computeOnServer(srv -> {
				RopeShovelEntity s = shovel(srv.overworld());
				return s != null && s.busy() && !s.digging();
			});
			if (d) {
				return;
			}
			ticks(1);
		}
		log("no swing");
	}

	private void untilShovelIdle(int most) {
		for (int i = 0; i < most; i += 10) {
			boolean idle = server.computeOnServer(srv -> {
				RopeShovelEntity s = shovel(srv.overworld());
				return s != null && !s.busy() && s.loadingTruck() == null;
			});
			if (idle) {
				return;
			}
			ticks(10);
		}
	}

	/** If its face is worked out of reach, the shovel is moved on 4 m into the muck, off camera (staged, said in the log). */
	private void moveOnIfWorkedOut() {
		server.runOnServer(srv -> {
			RopeShovelEntity s = shovel(srv.overworld());
			if (s != null && !filming && s.status().contains("worked out")) {
				s.setPos(s.getX(), s.getY(), s.getZ() - 4);
				s.setAutoHere();
				log("the shovel's face was worked out: moved on 4 m into the muck, off camera");
				summary.add("shovel moved on 4 m (off camera)");
			}
		});
	}

	// ---- readouts -----------------------------------------------------------------------------------------------------------------

	private String shovelNow(ServerLevel level, RopeShovelEntity s) {
		if (s == null) {
			return "-";
		}
		boolean dig = s.digging();
		if (dig && !wasDigging) {
			passStart = level.getGameTime();
			if (filming) {
				event("dipper into the face");
			}
		}
		if (!dig && wasDigging && filming) {
			event("dipper out of the face: swinging");
		}
		wasDigging = dig;
		float cyc = s.cycle();
		String clock = !s.busy() ? "" : String.format(Locale.ROOT, " (%.0f s)", cyc <= 1 ? cyc * 12 : 12 + (cyc - 1) * 23);
		if (dig) {
			return "hoisting the dipper through the face" + clock;
		}
		if (s.busy()) {
			return "swinging, dumping and back" + clock;
		}
		String st = s.status();
		return st.contains("WAITING") ? "dipper full, waiting for a truck" : st.length() > 44 ? st.substring(0, 44) : st;
	}

	private long lastPasses = -1;

	private String[] shovelCard(ServerLevel level) {
		RopeShovelEntity s = shovel(level);
		PitSwitchHouseBlockEntity h = houseBe(level);
		UltraHaulTruckEntity t = truck(level);
		if (s == null || h == null) {
			return new String[] {"^Rope shovel", "gone"};
		}
		if (s.passes() != lastPasses && filming && lastPasses >= 0) {
			event("pass " + s.passes() + " into the truck");
		}
		lastPasses = s.passes();
		String truckLine = t == null ? "Truck: -" : String.format(Locale.ROOT, "Ultra truck: pass %d, %s of 363 t", s.loadingTruck() == null ? 0 : s.passesThisTruck(),
				f1(t.load().total() / 1000.0) + " t");
		return new String[] {"^Rope shovel", "Now: " + shovelNow(level, s), String.format(Locale.ROOT, "Dipper: %.1f t of waste rock", s.bucket().total() / 1000.0),
				String.format(Locale.ROOT, "Draw at the switch house: %s kW, %s A", f0(h.watts(0) / 1000), f0(h.amps(0))), truckLine,
				String.format(Locale.ROOT, "Passes: %d; loaded: %s t", s.passes(), f0(s.tonnesKg() / 1000.0))};
	}

	/** The ultra's engine output from its burn: 36 MJ a litre at 38 % (PitFuel), measured over a second. */
	private void meter(ServerLevel level, int tick) {
		UltraHaulTruckEntity t = truck(level);
		if (t == null || tick % 20 != 0) {
			return;
		}
		double f = t.fuel();
		if (!Double.isNaN(lastFuel) && lastFuel >= f) {
			engineKw = (lastFuel - f) * 36.0e6 * 0.38 / 1000.0;
		}
		lastFuel = f;
	}

	private String lastTruck = "";

	private String[] truckCard(ServerLevel level, int tick) {
		meter(level, tick);
		UltraHaulTruckEntity t = truck(level);
		if (t == null) {
			return new String[] {"^Ultra haul truck", "gone"};
		}
		String st = t.status();
		String head = st.split("[:(]")[0];
		if (!head.equals(lastTruck) && filming) {
			event("truck: " + st);
		}
		lastTruck = head;
		return new String[] {"^Ultra haul truck, driving itself", "Now: " + (st.length() > 46 ? st.substring(0, 46) : st),
				String.format(Locale.ROOT, "Speed: %.1f km/h; grade %.0f %%", Math.abs(t.speed()) * 3.6, t.slope() * 100),
				String.format(Locale.ROOT, "Load: %s t; with the truck %s t", f1(t.load().total() / 1000.0), f0(t.massKg() / 1000.0)),
				String.format(Locale.ROOT, "Engine: %s kW (from its burn); fuel %s L", f0(engineKw), f0(t.fuel()))};
	}

	private String[] officeCard(ServerLevel level, int tick) {
		meter(level, tick);
		HaulDispatchBlockEntity d = officeBe(level);
		UltraHaulTruckEntity t = truck(level);
		if (d == null) {
			return new String[] {"^Haul dispatch office", "gone"};
		}
		return new String[] {"^Haul dispatch office", "Radio: " + (d.powered() ? "on, 2 km reach" : "dead"),
				String.format(Locale.ROOT, "Route 1 (6 stakes): %s; %d truck", d.runs(0) ? "RUN" : "PARK", d.trucks(0)),
				String.format(Locale.ROOT, "Loads tipped: %d; %s t in the last hour", d.loads(0), f0(d.tonnesLastHour(0))),
				"Ultra: " + (t == null ? "-" : t.status().length() > 44 ? t.status().substring(0, 44) : t.status())};
	}

	private String[] powerCard(ServerLevel level) {
		GeneratorPartBlockEntity g = gen(level, turbine);
		PitSwitchHouseBlockEntity h = houseBe(level);
		RopeShovelEntity s = shovel(level);
		return new String[] {"^Pit power",
				g == null ? "Gas turbine: -" : String.format(Locale.ROOT, "Gas turbine: %s MW, %s Hz", f1(g.deliveredWatts() / 1e6), f1(g.hertz())),
				"Feed: one 450 A cable to the switch house",
				h == null ? "" : String.format(Locale.ROOT, "Switch house outlet 1: %s kW, %s A", f0(h.watts(0) / 1000), f0(h.amps(0))),
				"Rope shovel: " + (s == null ? "-" : shovelNow(level, s))};
	}

	// ---- keeping the stand-ins up -------------------------------------------------------------------------------------------------

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
			if (level.getBlockEntity(t) instanceof StorageTankBlockEntity tank && tank.amount() < 20_000) {
				tank.testFill(Product.KEROSENE, 40_000);
			}
		}
		if (++keeps % 15 == 0 && turbine != null) {
			status(String.format(Locale.ROOT, "tick %.1f ms", srv.getAverageTickTimeNanos() / 1e6), srv);
		}
	}

	private Vec3 truckPos() {
		return server.computeOnServer(srv -> {
			UltraHaulTruckEntity t = truck(srv.overworld());
			return t == null ? Vec3.ZERO : t.position().subtract(o.getX(), o.getY(), o.getZ());
		});
	}

	private Vec3 shovelPos() {
		return server.computeOnServer(srv -> {
			RopeShovelEntity s = shovel(srv.overworld());
			return s == null ? new Vec3(7, PF, 10) : s.position().subtract(o.getX(), o.getY(), o.getZ());
		});
	}

	// ---- the films ----------------------------------------------------------------------------------------------------------------

	/** hi02_parts: the shovel as its six parts, the camera coming round; RUN lifts them into one machine halfway. */
	private void partsFilm() {
		boolean[] lifted = {false};
		showCard(true);
		film("hi02_parts", 400, orbitAround(o, 7, PF + 5, 4, 24, 8, 250, 45, 400), (level, t) -> {
			if (t == PAD + 160 && !lifted[0]) {
				lifted[0] = true;
				String said = lift(level);
				event("RUN: " + said);
				log("lift: " + said);
			}
			RopeShovelBlockEntity c = level.getBlockEntity(crawlers) instanceof RopeShovelBlockEntity r ? r : null;
			RopeShovelEntity s = shovel(level);
			card("^Rope shovel", c != null ? "Six parts on the pit floor, built: press RUN" : s != null ? "Lifted: one machine on its crawlers" : "-",
					"Trailing cable: switch house outlet 1");
		});
		showCard(false);
		if (!lifted[0]) {
			server.runOnServer(srv -> log("lift (unfilmed): " + lift(srv.overworld())));
		}
	}

	/** hi02_scale_truck: a villager at the ultra's front wheel, one on its deck; the camera pulls back to it beside the 100 t truck. */
	private void scaleTruckFilm() {
		Cam a = dcam(41.5, PF + 1.7, 38.5, 46.5, PF + 1.4, 32.5);
		Cam b = dcam(32, PF + 4.5, 44, 48, PF + 4, 30);
		Cam c = dcam(10, PF + 12, 52, 50, PF + 3, 35);
		film("hi02_scale_truck", 1000, via(a, b, c, 1000), null);
	}

	/** hi02_spot: from above the pit's floor, the ultra turning round and backing in beside the shovel; ends as it stops to load. */
	private void spotFilm() {
		untilTruck("turning round", 4000);
		showCard(true);
		int[] stopped = {-1};
		endMove = false;
		film("hi02_spot", 2400, move(dcam(70, PF + 22, 66, 26, PF, 18), dcam(64, PF + 18, 58, 22, PF, 12), 2400), (level, t) -> {
			card(truckCard(level, t));
			UltraHaulTruckEntity tr = truck(level);
			if (tr != null && tr.status().startsWith("loading") || tr != null && tr.status().startsWith("waiting to load")) {
				if (stopped[0] < 0) {
					stopped[0] = t;
				}
				if (t - stopped[0] > 80) {
					endMove = true;
				}
			}
		});
		showCard(false);
	}

	/** hi02_passes: the four passes into the ultra in real time, from beside the truck's tail; ends as the loaded truck pulls away. */
	private void passesFilm() {
		showCard(true);
		int[] away = {-1};
		lastPasses = -1;
		endMove = false;
		film("hi02_passes", 4000, move(dcam(42, PF + 16, -12, 12, PF + 4, 2), dcam(40, PF + 15, -10, 12, PF + 4, 2), 4000), (level, t) -> {
			card(shovelCard(level));
			UltraHaulTruckEntity tr = truck(level);
			if (tr != null && Math.abs(tr.speed()) > 0.3 && tr.load().total() > 300_000) {
				if (away[0] < 0) {
					away[0] = t;
					event("loaded: the truck pulls away, " + tr.load().tonnes());
				}
				if (t - away[0] > 60) {
					endMove = true;
				}
			}
		});
		showCard(false);
	}

	/** hi02_haul: following the loaded ultra up the ramp from its east side; ends at the turn at the top. */
	private void haulFilm() {
		Vec3[] look = {truckPos()};
		Vec3[] eye = {null};
		showCard(true);
		endMove = false;
		film("hi02_haul", 2600, t -> {
			Vec3 p = server.computeOnServer(srv -> {
				UltraHaulTruckEntity tr = truck(srv.overworld());
				return tr == null ? Vec3.ZERO : tr.position().subtract(o.getX(), o.getY(), o.getZ());
			});
			look[0] = look[0].lerp(p, 0.06);
			Vec3 want = new Vec3(RX1 + 6, Math.max(Math.max(p.y, PF) + 7, 2), look[0].z + 20);
			eye[0] = eye[0] == null ? want : eye[0].lerp(want, 0.04);
			return dcam(eye[0].x, eye[0].y, eye[0].z, look[0].x, look[0].y + 3, look[0].z);
		}, (level, t) -> {
			card(truckCard(level, t));
			UltraHaulTruckEntity tr = truck(level);
			if (tr != null && tr.getZ() - o.getZ() > 200) {
				endMove = true;
			}
		});
		showCard(false);
	}

	/** hi02_dump: from the dump's floor below the crest: the ultra backs to the berm and tips; the load pours down the face. */
	private void dumpFilm() {
		untilTruck("backing in to DUMP", 3000);
		boolean[] tipped = {false};
		int[] down = {-1};
		showCard(true);
		endMove = false;
		film("hi02_dump", 2400, move(dcam(50, 3, CREST + 30, 22, -5, CREST - 2), dcam(46, 2, CREST + 28, 22, -5, CREST - 2), 2400), (level, t) -> {
			card(truckCard(level, t));
			UltraHaulTruckEntity tr = truck(level);
			if (tr == null) {
				return;
			}
			if (tr.lift() > 0.5) {
				tipped[0] = true;
			}
			if (tipped[0] && tr.lift() == 0 && tr.pouring() == 0) {
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
	}

	/** hi02_dispatch: the dispatch office by the ramp's top as the empty ultra drives back past it; its readout on the card. */
	private void dispatchFilm() {
		showCard(true);
		film("hi02_dispatch", 700, move(dcam(76, 3.5, 166, 64, 1.5, 180), dcam(74, 4, 170, 30, 0, 186), 700), (level, t) -> card(officeCard(level, t)));
		showCard(false);
	}

	/** hi02_dig: the dipper hoisted through the face, close, from the pit's floor west of the muck. */
	private void digFilm() {
		untilDig(3000);
		Vec3 c = shovelPos();
		showCard(true);
		film("hi02_dig", 560, move(dcam(c.x - 19, PF + 4, c.z - 6, c.x, PF + 6, c.z - 19), dcam(c.x - 17, PF + 5, c.z - 9, c.x, PF + 7, c.z - 20), 560),
				(level, t) -> card(shovelCard(level)));
		showCard(false);
	}

	/** hi02_swing: low by the crawlers, looking up the boom as it swings loaded to the truck and dumps. */
	private void swingFilm() {
		untilSwing(1200);
		Vec3 c = shovelPos();
		film("hi02_swing", 520, move(dcam(c.x + 2, PF + 1.6, c.z + 26, c.x + 14, PF + 12, c.z - 4), dcam(c.x + 5, PF + 1.6, c.z + 26, c.x + 17, PF + 11, c.z - 3), 520),
				(level, t) -> shovelNow(level, shovel(level)));
	}

	/** hi02_power: the turbine set, the transformer, the switch house on the rim and the shovel below; the card reads the power through. */
	private void powerFilm() {
		untilDig(1600);
		showCard(true);
		film("hi02_power", 1000, move(dcam(-14, 16, 58, -52, 0, 6), dcam(-24, 18, 56, 2, -10, 8), 1000), (level, t) -> card(powerCard(level)));
		showCard(false);
	}

	/** hi02_scale_shovel: from a villager at the crawlers' front corner, the camera rises to the whole machine and its boom. */
	private void scaleShovelFilm() {
		Vec3 c = shovelPos();
		Cam a = dcam(c.x - 13, PF + 1.7, c.z - 2, c.x - 9, PF + 1.4, c.z - 7);
		Cam b = dcam(c.x - 22, PF + 6, c.z + 14, c.x - 4, PF + 6, c.z - 4);
		Cam d = dcam(c.x - 44, PF + 16, c.z + 40, c.x, PF + 9, c.z - 6);
		film("hi02_scale_shovel", 1000, via(a, b, d, 1000), (level, t) -> shovelNow(level, shovel(level)));
	}

	/** hi02_wide: the pit from the air: the shovel loading, the ramp and the dump beyond; a slow quarter turn. */
	private void wideFilm() {
		film("hi02_wide", 1400, orbitAround(o, 25, PF + 4, 70, 100, 62, 205, 40, 1400), (level, t) -> shovelNow(level, shovel(level)));
	}
}
