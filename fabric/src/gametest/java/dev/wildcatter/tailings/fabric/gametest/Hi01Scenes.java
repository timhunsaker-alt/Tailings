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
import dev.wildcatter.tailings.block.PitSwitchHouseBlockEntity;
import dev.wildcatter.tailings.block.PlantBlockEntity;
import dev.wildcatter.tailings.block.TailingsPartBlock;
import dev.wildcatter.tailings.entity.DraglineEntity;
import dev.wildcatter.tailings.entity.UltraHaulTruckEntity;
import dev.wildcatter.tailings.fabric.gametest.Trailer.Cam;
import dev.wildcatter.tailings.item.TrailingCableItem;
import dev.wildcatter.tailings.pit.Dragline;
import dev.wildcatter.tailings.pit.DraglineRopes;
import dev.wildcatter.tailings.pit.Slopes;
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
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.Vec3;

/**
 * Heavy Iron, episode 1, "Walking dragline" (TAILINGS_ONLY=hi01): a walking dragline (docs/multiblocks/dragline.md,
 * dragline-hand-control.md) stripping overburden off a bauxite seam, on real power: a Wildcatter 16.5 MW gas turbine set, its
 * starter's diesel set, a unit auxiliary transformer down to the medium voltage, MV cable to a pit switch house, and the dragline's
 * trailing cable from the house's first outlet.
 * <ul>
 * <li>the site: grass over 8 m of dirt over a 2 m bauxite seam over stone; the current strip 20 m wide beside the dragline, the
 * last strip's hole beyond it with the spoil already cast into it, an older backfilled strip beyond that;</li>
 * <li>the films: its whole 55 s cycle, the drag, the cast, the swing, a wide, a scale shot (a villager and an ultra truck at its tub),
 * the power yard, a bucket by hand from the cab into the ultra truck, a whole 2 m step, the uncovered seam; then two side sites
 * with the world rule slope_failures on: a highwall slide and a dragline wrecked by its own highwall.</li>
 * </ul>
 * Filmed in real time at 2880 x 1620 with a readout card, read from the machine and the switch house each tick. HI01_SHOTS=a,b films
 * only those.
 * <p>
 * Staged, and said so in the episode: the earlier strips (their hole, their spoil, and the strip dug behind and around the dragline
 * before filming); the gas turbine's purge and run-up skipped off camera; the fuel in its tank and the sets'; the villager. Between
 * films the dragline runs its own job at ten times speed (fastCycles) to dig out its reach so it walks; every film is at its own pace.
 * The slope films run Wildcatter's time scale (10:1 and 20:1), which shortens the slope's failure clock only (waits, not rates).
 */
final class Hi01Scenes {
	private static final int W = 1440, H = 810;
	private static final int PAD = 60;
	private static final String SEED = System.getenv().getOrDefault("TAILINGS_TRAILER_SEED", "tailings");
	private static final String LV = "wildcatter:cable_industrial", BUS = "wildcatter:busway_industrial", MV = "wildcatter:cable_mv",
			DUCT = "wildcatter:bus_duct_gen";
	/** The strip: x 13-32 (20 m wide, its near edge 12.5 m from the dragline's centre), along z from 0; the seam's top 8 m down. */
	private static final int SX0 = 13, SX1 = 32, SEAM = 8, LEN = 100;
	/** The overburden block left in reach for the films to dig: x 13-32, z 16-27, the 8 m down to the seam. */
	private static final int BZ0 = 16, BZ1 = 27;
	/** The last strip's hole beyond it (its seam taken), and the older backfilled strip beyond that. */
	private static final int HX0 = 33, HX1 = 58, OX1 = 78;

	private final ClientGameTestContext context;
	private TestServerContext server;
	/** o: the dragline's centre is at o + (0.5, 0, 0.5); o's y is the ground's first air. */
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

	private BlockPos turbine, house, uat;
	private UUID dl, truck, wreckDl;
	private int dugEnd = 0;
	private long fillStart = -1;
	private Dragline.Phase lastPhase;
	private long cycleStartTick = -1;

	Hi01Scenes(ClientGameTestContext context) {
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
			cardWidth("Draw at the switch house: 6,000 kW, 1,000 A");
			try {
				stage();
				power();
				startDragline();
				cycleFilm();
				dragFilm();
				castFilm();
				swingFilm();
				wideFilm();
				scaleFilm();
				powerFilm();
				handFilm();
				walkFilm();
				seamFilm();
				slideFilm();
				wreckFilm();
			} catch (RuntimeException | AssertionError ex) {
				log("FAILED: " + ex);
				ex.printStackTrace();
				throw ex;
			} finally {
				server.runCommand("tick rate 20");
				server.runCommand("tailings rule slope_failures false");
				server.runCommand("wildcatter rule timeScale 1");
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
		System.out.println("[hi01] " + line);
	}

	private static void require(boolean ok, String what) {
		if (!ok) {
			throw new AssertionError("[hi01] " + what);
		}
	}

	private static boolean wanted(String id) {
		String only = System.getenv("HI01_SHOTS");
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
		minY = Math.max(o.getY() - 8, minY - 2);
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

	private static boolean inReach(int x, int z) {
		return Dragline.inReach(x, z);
	}

	/** True if the current strip's cell (x, z) was dug before filming: the strip behind the dragline, and its reach but the block. */
	private static boolean preDug(int x, int z) {
		if (x < SX0 || x > SX1 || z > LEN - 1) {
			return false;
		}
		if (z < 0) {
			return true;
		}
		return inReach(x, z) && !(z >= BZ0 && z <= BZ1);
	}

	/** The spoil cast into the last strip's hole, in metres over its floor: a ridge at the spoil's 37 degrees, tapering at its ends. */
	private int spoil(int x, int z) {
		double ridge = 14 - 0.75 * Math.abs(x - 47);
		double end;
		if (z < BZ0 - 4) {
			end = 0.75 * (BZ0 - 4 - z);
		} else if (z > BZ1 + 1 && z <= dugEnd) {
			end = 0.75 * Math.min(z - BZ1 - 1, dugEnd + 1 - z);
		} else {
			end = 0;
		}
		return (int) Math.floor(Math.max(0, Math.min(ridge, end)));
	}

	/** Lays the strip mine: grass over dirt over the bauxite seam over stone, the strip, the last strip's hole and its spoil. */
	private void ground(ServerLevel level) {
		Block bauxite = wc("wildcatter:bauxite_ore");
		for (int z = 0; z < LEN; z++) {
			for (int x = SX0; x <= SX1; x++) {
				if (preDug(x, z)) {
					dugEnd = Math.max(dugEnd, z);
				}
			}
		}
		BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
		for (int x = -25; x <= 115; x++) {
			for (int z = -110; z <= 130; z++) {
				for (int y = -16; y <= 8; y++) {
					BlockState st;
					if (y <= -11) {
						st = Blocks.STONE.defaultBlockState();
					} else if (y <= -9) {
						st = bauxite.defaultBlockState();
					} else if (y <= -2) {
						st = Blocks.DIRT.defaultBlockState();
					} else if (y == -1) {
						st = Blocks.GRASS_BLOCK.defaultBlockState();
					} else {
						st = Blocks.AIR.defaultBlockState();
					}
					boolean strip = x >= SX0 && x <= SX1 && z >= -110 && z < LEN;
					boolean hole = x >= HX0 && x <= HX1 && z >= -110 && z < LEN;
					boolean older = x > HX1 && x <= OX1 && z >= -110 && z < LEN;
					if (strip && preDug(x, z) && y >= -SEAM) {
						st = Blocks.AIR.defaultBlockState();
					}
					if (hole && y >= -10) {
						int top = -11 + spoil(x, z);
						st = y <= top ? (Math.floorMod(x * 7 + z * 13 + y * 5, 5) == 0 ? Blocks.COARSE_DIRT : Blocks.DIRT).defaultBlockState() : Blocks.AIR.defaultBlockState();
					}
					if (older && y >= -1) {
						int top = -1 + (int) Math.floor(Math.max(0, 4 - 0.45 * Math.abs(x - 68)));
						st = y < top ? Blocks.DIRT.defaultBlockState() : y == top ? Blocks.GRASS_BLOCK.defaultBlockState() : Blocks.AIR.defaultBlockState();
					}
					level.setBlock(p.set(o.getX() + x, o.getY() + y, o.getZ() + z), st, Block.UPDATE_CLIENTS);
				}
			}
		}
	}

	private void stage() {
		server.runOnServer(srv -> {
			ServerLevel level = srv.overworld();
			BlockPos s = Trailer.site(level, 2600, -2200, 260, 260);
			int g = s.getY();
			o = new BlockPos(s.getX() + 140, g, s.getZ() + 110);
			Trailer.pad(level, o.getX() - 140, o.getZ() - 110, o.getX() + 115, o.getZ() + 130, g, 18, 70);
			Trailer.forceChunks(level, o.getX() - 200, o.getZ() - 130, o.getX() + 140, o.getZ() + 560);
			ground(level);
			log("site at " + o.toShortString() + "; strip dug to z " + dugEnd);
		});
		server.runCommand("kill @e[type=item]");
	}

	// ---- power: a gas turbine set, stepped down to the medium voltage, to a pit switch house --------------------------------------

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

	private void power() {
		server.runOnServer(srv -> {
			ServerLevel level = srv.overworld();
			Trailer.fillBox(level, o, Blocks.SMOOTH_STONE, -112, -24, -1, -1, -50, 4);
			Trailer.fillBox(level, o, Blocks.AIR, -112, -24, 0, 14, -50, 4);
			turbine = d(-75, 0, -40);
			turbineSet(level, turbine);
			// the turbine's starter: a diesel set and a unit substation run backward to its starter's box
			BlockPos ds = set(level, d(-104, 0, -26), false);
			BlockPos dMv = stepUp(level, ds);
			run(level, MV, turbine.offset(-1, 0, 2), dMv, new HashSet<>(), 0);
			// the unit auxiliary transformer, its generator-voltage bushings north, the medium voltage out low on its back
			uat = d(-40, 0, -20);
			big(level, wc("wildcatter:unit_auxiliary_transformer"), uat, Direction.NORTH);
			run(level, DUCT, uat.offset(0, 4, -3), turbine.offset(5, 0, 9), new HashSet<>(), 0);
			// the pit switch house, its medium voltage in on its north face
			house = d(-40, 0, -8);
			Trailer.part(level, TailingsBlocks.PIT_SWITCH_HOUSE, house);
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
		if (level.getBlockEntity(uat) instanceof dev.wildcatter.block.TransformerBlockEntity t) {
			log("  UAT: " + t.inspect().getString());
		}
		if (level.getBlockEntity(house) instanceof PitSwitchHouseBlockEntity h) {
			log("  house: " + h.inspect().getString());
		}
		DraglineEntity d = drag(level);
		if (d != null) {
			log("  dragline: " + d.status());
		}
	}

	private DraglineEntity drag(ServerLevel level) {
		return dl != null && level.getEntity(dl) instanceof DraglineEntity d ? d : null;
	}

	private PitSwitchHouseBlockEntity houseBe(ServerLevel level) {
		return level.getBlockEntity(house) instanceof PitSwitchHouseBlockEntity h ? h : null;
	}

	/** The dragline set down at o + (0.5, 0, 0.5) facing south, its job the strip, its cable on the house's first outlet; RUN. */
	private void startDragline() {
		server.runOnServer(srv -> {
			ServerLevel level = srv.overworld();
			PitSwitchHouseBlockEntity h = houseBe(level);
			DraglineEntity d = TailingsEntities.DRAGLINE.create(level, EntitySpawnReason.COMMAND);
			d.setPos(o.getX() + 0.5, o.getY(), o.getZ() + 0.5);
			d.setYRot(0);
			d.job().set(0, SX1 - SX0 + 1);
			d.job().set(1, LEN);
			d.job().set(2, SEAM);
			d.job().set(3, 0);
			level.addFreshEntity(d);
			ItemStack reel = new ItemStack(TailingsItems.TRAILING_CABLE, 1);
			log("reel: " + TrailingCableItem.pickUp(reel, h));
			log("cable: " + TrailingCableItem.couple(level, reel, d) + "; outlet " + h.close(0));
			d.start();
			dl = d.getUUID();
			// for scale: an ultra truck parked on the bench beside the tub, a villager at the tub's foot
			UltraHaulTruckEntity t = TailingsEntities.ULTRA_HAUL_TRUCK.create(level, EntitySpawnReason.COMMAND);
			Vec3 at = d.bucketAt(55, 29, 0);
			t.setPos(at.x, o.getY(), at.z);
			t.setYRot(55 + 90);
			t.setFuel(t.tank());
			level.addFreshEntity(t);
			truck = t.getUUID();
			log("truck at " + (at.x - o.getX()) + ", " + (at.z - o.getZ()) + " from o (bucket swing 55)");
		});
		server.runCommand(String.format(Locale.ROOT, "summon villager %d %d %d {NoAI:1b,Silent:1b,Invulnerable:1b,Rotation:[60f,0f]}", o.getX() - 16, o.getY(), o.getZ() - 5));
		ticks(200);
		server.runOnServer(srv -> status("dragline started", srv));
	}

	// ---- readouts -----------------------------------------------------------------------------------------------------------------

	private static String phaseName(Dragline.Phase p) {
		if (p == null) {
			return "-";
		}
		return switch (p) {
			case FILL -> "dragging in: the bucket fills";
			case SWING_OUT -> "hoisting and swinging to the spoil";
			case DUMP -> "dumping: drag rope paid out";
			case SWING_BACK -> "swinging back and lowering";
			case WALK -> "walking";
			case IDLE -> "standing";
		};
	}

	/** Watches the phase, so the card can say how far into its cycle it is. */
	private void track(ServerLevel level) {
		DraglineEntity d = drag(level);
		if (d == null) {
			return;
		}
		Dragline.Phase p = d.phase();
		if (p == Dragline.Phase.FILL && lastPhase != Dragline.Phase.FILL) {
			cycleStartTick = level.getGameTime();
		}
		if (p != lastPhase && filming) {
			event(String.valueOf(p));
		}
		lastPhase = p;
	}

	private String[] dragCard(ServerLevel level) {
		track(level);
		DraglineEntity d = drag(level);
		PitSwitchHouseBlockEntity h = houseBe(level);
		if (d == null || h == null) {
			return new String[] {"^Walking dragline", "gone"};
		}
		Dragline.Phase p = d.phase();
		String clock = cycleStartTick < 0 || p == Dragline.Phase.WALK ? "" : String.format(Locale.ROOT, " (%.0f s)", (level.getGameTime() - cycleStartTick) / 20.0);
		return new String[] {"^Walking dragline",
				"Now: " + (d.hand() ? "by hand" : phaseName(p)) + clock,
				String.format(Locale.ROOT, "Bucket: %.1f t of overburden", d.load().total() / 1000.0),
				String.format(Locale.ROOT, "Draw at the switch house: %s kW, %s A", f0(h.watts(0) / 1000), f0(h.amps(0))),
				String.format(Locale.ROOT, "Cycles: %d; bank moved: %s m³", d.cycles(), f0(d.bankM3())),
				String.format(Locale.ROOT, "Walked: %.1f m", d.walked())};
	}

	private String[] powerCard(ServerLevel level) {
		track(level);
		GeneratorPartBlockEntity g = gen(level, turbine);
		PitSwitchHouseBlockEntity h = houseBe(level);
		DraglineEntity d = drag(level);
		return new String[] {"^Pit power",
				g == null ? "Gas turbine: -" : String.format(Locale.ROOT, "Gas turbine: %s MW, %s Hz", f1(g.deliveredWatts() / 1e6), f1(g.hertz())),
				h == null ? "" : String.format(Locale.ROOT, "Switch house outlet 1: %s kW, %s A", f0(h.watts(0) / 1000), f0(h.amps(0))),
				d == null || d.cable() == null ? "" : String.format(Locale.ROOT, "Trailing cable paid out: %.0f m", d.cable().paid()),
				"Dragline: " + (d == null ? "-" : phaseName(d.phase()))};
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

	private void fast(int divisor) {
		server.runOnServer(srv -> {
			DraglineEntity d = drag(srv.overworld());
			if (d != null) {
				d.fastCycles(divisor);
			}
		});
	}

	private Dragline.Phase phase() {
		return server.computeOnServer(srv -> {
			DraglineEntity d = drag(srv.overworld());
			return d == null ? null : d.phase();
		});
	}

	/** Waits (real ticks) until the dragline's phase is {@code want}, a whole new one: first out of it, then into it. */
	private void until(Dragline.Phase want) {
		for (int i = 0; i < 2400 && phase() == want; i++) {
			step(i);
		}
		for (int i = 0; i < 2400; i++) {
			if (phase() == want) {
				return;
			}
			step(i);
		}
		log("never reached " + want + "; now " + phase());
	}

	/** One tick, keeping the stand-ins up once a second. */
	private void step(int i) {
		if (i % 20 == 0) {
			server.runOnServer(this::keep);
		}
		context.waitTick();
	}

	private Vec3 dragPos() {
		return server.computeOnServer(srv -> {
			DraglineEntity d = drag(srv.overworld());
			return d == null ? Vec3.ZERO : d.position().subtract(o.getX(), o.getY(), o.getZ());
		});
	}

	// ---- the films ----------------------------------------------------------------------------------------------------------------

	/** hi01_cycle: one whole cycle from the start of a drag, in real time, from beside the machine across the strip. */
	private void cycleFilm() {
		if (!wanted("hi01_cycle")) {
			return;
		}
		until(Dragline.Phase.FILL);
		ticks(1);
		showCard(true);
		// from the bench behind the tub's right, looking across the strip and the hole
		film("hi01_cycle", 1140, move(dcam(16, 30, -44, 30, -2, 22), dcam(24, 26, -40, 32, -2, 22), 1140), (level, k) -> card(dragCard(level)));
		showCard(false);
	}

	/** hi01_drag: the bucket dragged in through the bank, close, from the dug strip floor. */
	private void dragFilm() {
		if (!wanted("hi01_drag")) {
			return;
		}
		until(Dragline.Phase.SWING_BACK);
		ticks(300);
		showCard(true);
		film("hi01_drag", 560, move(dcam(47, 9, 41, 22, -2, 21), dcam(45, 7.5, 37, 22, -2, 21), 560), (level, k) -> card(dragCard(level)));
		showCard(false);
	}

	/** hi01_cast: the cast, from the floor of the last strip's hole, looking up at the bucket as it dumps (the cold open). */
	private void castFilm() {
		if (!wanted("hi01_cast")) {
			return;
		}
		until(Dragline.Phase.SWING_OUT);
		BlockPos dump = server.computeOnServer(srv -> {
			DraglineEntity d = drag(srv.overworld());
			return d == null ? null : d.dumpAt();
		});
		double ux = dump == null ? 46 : dump.getX() - o.getX(), uy = dump == null ? -8 : dump.getY() - o.getY(), uz = dump == null ? 20 : dump.getZ() - o.getZ();
		log("casting at " + ux + ", " + uy + ", " + uz + " from o");
		showCard(false);
		film("hi01_cast", 560, move(dcam(ux + 12, uy + 20, uz + 4, ux - 8, uy + 7, uz), dcam(ux + 10, uy + 19, uz - 1, ux - 8, uy + 7, uz), 560), (level, k) -> track(level));
	}

	/** hi01_swing: low by the tub, looking up the boom as it swings out loaded. */
	private void swingFilm() {
		if (!wanted("hi01_swing")) {
			return;
		}
		until(Dragline.Phase.SWING_OUT);
		Vec3 c = dragPos();
		film("hi01_swing", 440, move(dcam(c.x + 14, 1.4, c.z - 9, c.x + 34, 26, c.z + 16), dcam(c.x + 15, 1.4, c.z - 11, c.x + 36, 24, c.z + 14), 440), (level, k) -> track(level));
	}

	/** hi01_wide: the strip mine from the air, the dragline casting into the last strip's hole: a slow quarter turn. */
	private void wideFilm() {
		if (!wanted("hi01_wide")) {
			return;
		}
		showCard(true);
		film("hi01_wide", 1200, orbitAround(o, 25, -4, 22, 88, 50, 115, 65, 1200), (level, k) -> card(dragCard(level)));
		showCard(false);
	}

	/** hi01_scale: a villager at the tub's foot and the ultra truck beside it; the camera rises from the villager to the boom point. */
	private void scaleFilm() {
		if (!wanted("hi01_scale")) {
			return;
		}
		Cam a = dcam(-24, 1.7, -13, -16, 1.3, -5);
		Cam b = dcam(-36, 9, -32, -6, 9, 0);
		Cam c = dcam(-72, 34, -68, 16, 22, 24);
		film("hi01_scale", 1000, via(a, b, c, 1000), (level, k) -> track(level));
	}

	/** hi01_power: the turbine set, the transformer, the switch house and the dragline beyond; the card reads the power through. */
	private void powerFilm() {
		if (!wanted("hi01_power")) {
			return;
		}
		showCard(true);
		film("hi01_power", 1000, move(dcam(-96, 16, 8, -56, 2, -26), dcam(-60, 22, 22, -8, 6, -6), 1000), (level, k) -> card(powerCard(level)));
		showCard(false);
	}

	private record Pose(double swing, double rate, double out, double up, long casts, double tons, Vec3 eye, Vec3 bucket, String text) {
	}

	private Pose pose(ServerLevel level) {
		DraglineEntity d = drag(level);
		if (d == null) {
			return null;
		}
		return new Pose(d.swing(), d.swingRate(), d.bucketOut(), d.bucketUp(), d.handCasts(), d.load().total() / 1000.0, d.cabEye(),
				d.bucketAt(d.swing(), d.bucketOut(), d.bucketUp()), d.handReadoutText());
	}

	private static Input keys(boolean forward, boolean back, boolean left, boolean right, boolean jump, boolean sprint) {
		return new Input(forward, back, left, right, jump, false, sprint);
	}

	/**
	 * hi01_hand: from the cab. The operator takes the levers as the bucket comes back empty, lowers it onto the overburden block, drags
	 * it full, hoists it clear, swings right and lets the house coast to the ultra truck, and pays out the drag to dump into its body;
	 * then hands it back to its job. Real time, the machine's own rates.
	 */
	private void handFilm() {
		if (!wanted("hi01_hand")) {
			return;
		}
		until(Dragline.Phase.SWING_BACK);
		final double truckSwing = 55, truckOut = 29;
		server.runOnServer(srv -> {
			DraglineEntity d = drag(srv.overworld());
			d.testSeat(true);
			d.testDrive(keys(false, false, false, false, true, false));
		});
		context.waitTicks(2);
		server.runOnServer(srv -> {
			DraglineEntity d = drag(srv.overworld());
			d.testDrive(Input.EMPTY);
			d.emptyBucket();
			d.handPose(-48, 34, 10);
		});
		Pose[] p = {server.computeOnServer(srv -> pose(srv.overworld()))};
		String[] phase = {"pre"};
		int[] at = {0};
		double[] last = {Double.NaN, Double.NaN};
		int[] still = {0};
		Vec3[] look = {null};
		showCard(true);
		IntFunction<Cam> cam = t -> {
			Pose q = p[0];
			Vec3 target = q.bucket().add(0, -2, 0);
			look[0] = look[0] == null ? target : look[0].lerp(target, 0.08);
			Vec3 e = q.eye();
			return new Cam(e.x, e.y, e.z, look[0].x, look[0].y, look[0].z);
		};
		endMove = false;
		film("hi01_hand", 3000, cam, (level, t) -> {
			Pose q = pose(level);
			if (q == null) {
				return;
			}
			p[0] = q;
			Input k = Input.EMPTY;
			String next = phase[0];
			int in = t - PAD - at[0];
			switch (phase[0]) {
				case "pre" -> next = t >= PAD ? "lower" : "pre";
				case "lower" -> {
					k = keys(false, false, false, false, false, true);
					still[0] = Math.abs(q.up() - last[0]) < 0.01 ? still[0] + 1 : 0;
					last[0] = q.up();
					if (still[0] >= 10 && in > 20) {
						next = "drag";
						still[0] = 0;
					}
				}
				case "drag" -> {
					k = keys(true, false, false, false, false, false);
					if (q.tons() >= 39.9 || in > 900) {
						next = "hoist";
					}
				}
				case "hoist" -> {
					k = keys(false, false, false, false, true, false);
					still[0] = Math.abs(q.up() - last[0]) < 0.01 ? still[0] + 1 : 0;
					last[0] = q.up();
					if (q.up() >= 11 || (still[0] >= 10 && q.up() > 9)) {
						next = "swing";
						still[0] = 0;
					}
				}
				case "swing" -> {
					double stop = q.swing() + q.rate() * Math.abs(q.rate()) / (2 * DraglineRopes.SWING_ACCEL);
					boolean pull = q.out() > truckOut + 0.5;
					k = keys(pull, false, false, stop < truckSwing, false, false);
					if (stop >= truckSwing) {
						next = "coast";
					}
				}
				case "coast" -> {
					boolean pull = q.out() > truckOut + 0.5;
					k = keys(pull, false, false, false, false, false);
					if (q.rate() == 0 && !pull) {
						next = "dump";
					}
				}
				case "dump" -> {
					k = keys(false, true, false, false, false, false);
					if (q.casts() >= 1) {
						next = "done";
					}
				}
				default -> {
					if (in > 60) {
						endMove = true;
					}
				}
			}
			if (!next.equals(phase[0])) {
				event(phase[0] + " -> " + next + String.format(Locale.ROOT, " (swing %.0f, %.0f m out, %+.0f m, %.0f t)", q.swing(), q.out(), q.up(), q.tons()));
				phase[0] = next;
				at[0] = t - PAD;
			}
			DraglineEntity d = drag(level);
			d.testDrive(k);
			String[] lines = q.text().isEmpty() ? new String[0] : q.text().split("\\s*\\|\\s*");
			String[] out = new String[Math.min(6, lines.length + 1)];
			out[0] = "^Walking dragline, from the cab";
			for (int i = 1; i < out.length; i++) {
				out[i] = lines[i - 1];
			}
			card(out);
		});
		showCard(false);
		double tons = server.computeOnServer(srv -> srv.overworld().getEntity(truck) instanceof UltraHaulTruckEntity u ? (Double) (u.load().total() / 1000.0) : (Double) (-1.0));
		server.runOnServer(srv -> {
			DraglineEntity d = drag(srv.overworld());
			d.testSeat(false);
			d.testDrive(Input.EMPTY);
		});
		log("hand: phase " + phase[0] + "; the truck holds " + tons + " t");
		summary.add("hand: " + phase[0] + ", truck " + tons + " t");
	}

	/** hi01_walk: the job dug out of its reach (at ten times speed, off camera), then a whole 2 m step in real time, low by a shoe. */
	private void walkFilm() {
		if (!wanted("hi01_walk") && !wanted("hi01_seam")) {
			return;
		}
		fast(10);
		boolean walking = false;
		for (int i = 0; i < 20 * 60 * 25; i++) {
			boolean w = server.computeOnServer(srv -> {
				DraglineEntity d = drag(srv.overworld());
				return d != null && (d.walking() || d.status().startsWith("DONE"));
			});
			if (w) {
				walking = true;
				break;
			}
			step(i);
		}
		fast(1);
		server.runOnServer(srv -> status("walking? ", srv));
		if (!walking || !wanted("hi01_walk")) {
			return;
		}
		Vec3 c = dragPos();
		showCard(true);
		film("hi01_walk", 1240, move(dcam(c.x + 23, -6.4, c.z + 9, c.x + 8, 1.5, c.z + 1), dcam(c.x + 21, -6.4, c.z - 3, c.x + 8, 1.5, c.z + 2), 1240), (level, k) -> card(dragCard(level)));
		showCard(false);
	}

	/** hi01_seam: along the uncovered seam on the strip's floor, the highwall to the left and the spoil to the right. */
	private void seamFilm() {
		if (!wanted("hi01_seam")) {
			return;
		}
		film("hi01_seam", 800, move(dcam(23, -6.2, -70, 23, -7, 0), dcam(23, -5.5, -22, 23, -4, 30), 800), (level, k) -> track(level));
	}

	// ---- the slopes, on two side sites with the world rule on -------------------------------------------------------------------

	private static void fill(ServerLevel level, BlockPos o, Block block, int x0, int x1, int y0, int y1, int z0, int z1) {
		Trailer.fillBox(level, o, block, x0, x1, y0, y1, z0, z1);
	}

	private static void clearSite(ServerLevel level, BlockPos o) {
		fill(level, o, Blocks.AIR, -40, 110, 1, 60, -50, 90);
		fill(level, o, Blocks.STONE, -40, 110, -2, 0, -50, 90);
	}

	/** As SlopeScenes.highwallSlide: a 21 m dirt highwall laid back 2 in 1, three prisms; the failure clock at 10:1. */
	private void slideFilm() {
		if (!wanted("hi01_slide")) {
			return;
		}
		BlockPos s = o.offset(-150, 0, 330);
		long[] id = new long[1];
		int crest = 22;
		server.runCommand("tailings rule slope_failures true");
		server.runCommand("wildcatter rule timeScale 10");
		server.runOnServer(srv -> {
			ServerLevel level = srv.overworld();
			clearSite(level, s);
			fill(level, s, Blocks.DIRT, -40, 32, 1, crest - 2, -30, 40);
			fill(level, s, Blocks.GRASS_BLOCK, -40, 32, crest - 1, crest - 1, -30, 40);
			for (int dd = 1; dd <= crest - 1; dd++) {
				int off = Dragline.batterOffset(Dragline.BATTER_2_IN_1, dd);
				if (off > 0) {
					fill(level, s, Blocks.DIRT, 33, 32 + off, crest - dd, crest - dd, -30, 40);
				}
			}
			for (int z : new int[] {3, 6, 9}) {
				level.setBlock(s.offset(29, crest, z), TailingsBlocks.SLOPE_PRISM.defaultBlockState(), Block.UPDATE_ALL);
			}
		});
		ticks(40);
		String[] stage = {"STABLE"};
		int[] cracks = {0}, steps = {0};
		String[] fs = {""};
		showCard(true);
		endMove = false;
		film("hi01_slide", 1400, t -> Cam.of(s, 52, crest + 6, -12, 30, crest - 7, 6), (level, t) -> {
			if (t == PAD) {
				id[0] = Slopes.register(level, UUID.randomUUID(), s.getX() + 20.5, s.getZ() + 0.5, 0, 1, 1, 0, s.getY() + crest, 30, 10, Dragline.BATTER_2_IN_1);
				Slopes.checkNow(level, id[0], 0);
			}
			if (id[0] != 0) {
				Slopes.Part p = Slopes.strip(level.getServer(), id[0]).section(0).wall();
				String now = p.stage().name();
				fs[0] = String.format(Locale.ROOT, "%.2f", p.fs());
				if (!now.equals(stage[0]) && !stage[0].equals("SLID")) {
					event(now + " (FS " + fs[0] + ")");
					stage[0] = now;
				}
				if (p.cracks() > cracks[0]) {
					cracks[0] = p.cracks();
					event("cracks " + cracks[0]);
				}
				if (p.steps() > steps[0]) {
					steps[0] = p.steps();
					event("slab moves " + steps[0]);
				}
				if (stage[0].equals("SLID") && Slopes.pending(level) == 0) {
					endMove = true;
				}
			}
			card("^Highwall: 21 m of dirt, laid back 2 in 1", "Stage: " + stage[0], "Tension cracks on the bench: " + cracks[0], "Failure clock: 10 times speed");
		});
		showCard(false);
		server.runCommand("wildcatter rule timeScale 1");
		log("slide: " + stage[0] + ", FS " + fs[0] + ", steps " + steps[0]);
		summary.add("slide: " + stage[0]);
	}

	/** As SlopeScenes.wrecked, on real power: a second dragline by a 30 m vertical dirt highwall; the wall slides and it tips in. */
	private void wreckFilm() {
		if (!wanted("hi01_wreck")) {
			return;
		}
		BlockPos s = o.offset(-150, 0, 170);
		long[] stripId = new long[1];
		server.runCommand("tailings rule slope_failures true");
		server.runCommand("wildcatter rule timeScale 20");
		server.runOnServer(srv -> {
			ServerLevel level = srv.overworld();
			clearSite(level, s);
			fill(level, s, Blocks.DIRT, -40, 32, 1, 29, -40, 40);
			fill(level, s, Blocks.GRASS_BLOCK, -40, 32, 30, 30, -40, 40);
			PitSwitchHouseBlockEntity h = houseBe(level);
			DraglineEntity d = TailingsEntities.DRAGLINE.create(level, EntitySpawnReason.COMMAND);
			d.setPos(s.getX() + 20.5, s.getY() + 31, s.getZ() + 0.5);
			d.setYRot(0);
			d.job().set(0, 30);
			d.job().set(1, 10);
			d.job().set(2, 1);
			d.job().set(3, 0);
			level.addFreshEntity(d);
			ItemStack reel = new ItemStack(TailingsItems.TRAILING_CABLE, 1);
			log("wreck reel: " + TrailingCableItem.pickUp(reel, h));
			log("wreck cable: " + TrailingCableItem.couple(level, reel, d) + "; outlet " + h.close(1));
			d.start();
			wreckDl = d.getUUID();
			stripId[0] = d.stripId();
		});
		ticks(100);
		boolean[] tipping = {false}, gone = {false};
		showCard(true);
		endMove = false;
		film("hi01_wreck", 1400, t -> Cam.of(s, 74, 42, -38, 26, 24, 2), (level, t) -> {
			if (t == PAD) {
				Slopes.checkNow(level, stripId[0], 0);
				event("failing: cracks open");
			}
			var e = level.getEntity(wreckDl);
			boolean tip = e instanceof DraglineEntity d && d.tipping();
			if (tip && !tipping[0]) {
				event("the wall slides; the dragline tips into the cut");
				tipping[0] = true;
			}
			if (!(e instanceof DraglineEntity) && !gone[0]) {
				event("the dragline lands and breaks into its parts");
				gone[0] = true;
			}
			if (gone[0] && Slopes.pending(level) == 0) {
				endMove = true;
			}
			PitSwitchHouseBlockEntity h = houseBe(level);
			card("^Dragline on a 30 m vertical dirt highwall", "Dragline: " + (e instanceof DraglineEntity d ? (tip ? "going down with the slide" : "standing on the bench, 12.5 m from the crest") : "WRECKED: broken into its parts"),
					h == null ? "" : "Switch house outlet 2: " + h.state(1).name(), "Failure clock: 20 times speed");
		});
		showCard(false);
		server.runCommand("wildcatter rule timeScale 1");
		log("wreck: tipped " + tipping[0] + ", gone " + gone[0]);
		summary.add("wreck: tipped " + tipping[0] + ", gone " + gone[0]);
	}
}
