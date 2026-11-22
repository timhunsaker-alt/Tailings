package dev.wildcatter.tailings.fabric.gametest;

import dev.wildcatter.block.BigPartBlock;
import dev.wildcatter.block.DayTankBlockEntity;
import dev.wildcatter.block.DecantTowerBlockEntity;
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
import dev.wildcatter.tailings.TailingsItems;
import dev.wildcatter.tailings.block.FillOutletBlockEntity;
import dev.wildcatter.tailings.block.PlantBlockEntity;
import dev.wildcatter.tailings.block.SurveyStakeBlock;
import dev.wildcatter.tailings.block.TailingsPartBlock;
import dev.wildcatter.tailings.block.stage.Paste;
import dev.wildcatter.tailings.entity.HaulTruckEntity;
import dev.wildcatter.tailings.entity.TrackDozerEntity;
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
import net.minecraft.world.Container;
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
 * Rock to Metal, episode 8, "Where the Tails Go" (TAILINGS_ONLY=rtm08): the tails of both plants, every motor on real power from
 * Wildcatter generator sets at the industrial voltage, every cable, busway and pipe run laid joined.
 * <ul>
 * <li>small plant, dry stack (open-pit-p4-dry-stack.md): tails into the tailings thickener; its underflow up to the tailings filter's batch
 * press over the truck load-out; a self-driving truck through the load-out's lane to a DUMP POINT at a crest with no berm (NO BERM); the
 * track dozer pushes up a berm; the truck backs to it and tips over it;</li>
 * <li>small plant, paste fill (paste-backfill.md): a second tailings thickener, the disc filter, the mixer with cement by hopper, the piston
 * pump; its fill line coiled on the ground, down a borehole into a closed drive branch inside a hill, one wall glass;</li>
 * <li>pit plant (pit-flotation.md): the 45 m tailings thickener at the line's 1,000 t/h, its underflow over the rim of Wildcatter's lined
 * cell to the decant tower, the water of both back on water pipe.</li>
 * </ul>
 * Filmed in real time at 2880 x 1620 with a readout card, read from the blocks each tick. RTM08_SHOTS=a,b films only those.
 * <p>
 * Staged, and said so in the episode: the tails into each thickener (as if from the flotation bank, or the pit's tank-cell row); the
 * cement chest kept stocked; the water tanks drawn down as a plant would use the water; the load-out bin stocked to 100 t; the dozer's
 * blade loaded with dry tails; the lined cell part-filled; the fill line coiled on the ground for its length.
 */
final class Rtm08Scenes {
	private static final int W = 1440, H = 810;
	private static final int PAD = 60;
	private static final String SEED = System.getenv().getOrDefault("TAILINGS_TRAILER_SEED", "tailings");
	private static final String LV = "wildcatter:cable_industrial", BUS = "wildcatter:busway_industrial", MV = "wildcatter:cable_mv";
	private static final String WATER = "wildcatter:pipe";

	private final ClientGameTestContext context;
	private TestServerContext server;
	/** o = q: the dry stack's origin (road level); p: the paste plant's; t: the pit's. */
	private BlockPos o, q, p, t;
	private ConductorSize runSize = ConductorSize.KCMIL_500;
	private final List<BlockPos> panels = new ArrayList<>();
	private final List<String> setNames = new ArrayList<>();
	private final List<String> summary = new ArrayList<>();
	private final Set<BlockPos> boxCells = new HashSet<>();
	private final List<int[]> yards = new ArrayList<>();
	private long filmStart;
	private volatile boolean endMove;
	private int ft;
	private float lastYaw = Float.NaN;
	private int keeps;

	private PlantBlockEntity s1, thick, filter, frame, bin;
	private PlantBlockEntity s2, thick2, pfilter, mixer, pump;
	private PlantBlockEntity cellSrc, big;
	private BlockPos cementChest, outletPos, towerPos;
	private final List<BlockPos> tanksSmall = new ArrayList<>(), tanksBig = new ArrayList<>(), tanksCell = new ArrayList<>();
	private double drainedSmall, drainedBig, drainedCell;
	private UUID truckId, dozerId;
	private volatile double tx, tz, tyaw;
	private String note = "";

	private static final Mineral CU = Mineral.COPPER;
	/** Copper in a kg of the fines slurry (as Rtm07Scenes.RICH); the tails carry the quarter flotation leaves. */
	private static final double RICH = Rock.COPPER.contained(CU).doubleValue() * CU.kgPerUnit() / (Reagent.BLOCK_TONNES.doubleValue() * 1000 / PlantRates.OVERFLOW_SOLIDS);
	/** The pit line's tails, kg of slurry a second: 1,000 t/h of solids at 35 %. */
	private static final double PIT_SLURRY_PER_SECOND = 1_000_000.0 / 3600 / 0.35;

	Rtm08Scenes(ClientGameTestContext context) {
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
			cardWidth("Tails in: 1,000 t/h of solids at 35 % (staged)");
			try {
				stage();
				power();
				ticks(600);
				pressFilm();
				haulFilm();
				bermFilm();
				thickenerFilm();
				pitFilm();
				cellFilm();
				boolean filled = waitFor(6000, level -> level.getBlockEntity(outletPos) instanceof FillOutletBlockEntity outlet && outlet.filled() >= 25);
				log("paste in the branch before its films: " + filled);
				pasteFilm();
				voidFilm();
				lineFilm();
				dropFilm();
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

	/** The truck's road and its balloon loop, under 11 m: no cable runs there, so nothing stands in a truck's way. */
	private boolean noGo(BlockPos c) {
		if (c.getY() >= q.getY() + 11) {
			return false;
		}
		int x = c.getX() - q.getX(), z = c.getZ() - q.getZ();
		return x >= 13 && x <= 26 && z >= -12 && z <= 118 || x >= -10 && x <= 26 && z >= 64 && z <= 122;
	}

	private BlockPos d(int x, int y, int z) {
		return q.offset(x, y, z);
	}

	private BlockPos pp(int x, int y, int z) {
		return p.offset(x, y, z);
	}

	private BlockPos pt(int x, int y, int z) {
		return t.offset(x, y, z);
	}

	private Cam dcam(double x, double y, double z, double ax, double ay, double az) {
		return Cam.of(q, x, y, z, ax, ay, az);
	}

	private Cam pcam(double x, double y, double z, double ax, double ay, double az) {
		return Cam.of(p, x, y, z, ax, ay, az);
	}

	private Cam tcam(double x, double y, double z, double ax, double ay, double az) {
		return Cam.of(t, x, y, z, ax, ay, az);
	}

	private static void log(String line) {
		System.out.println("[rtm08] " + line);
	}

	private static void require(boolean ok, String what) {
		if (!ok) {
			throw new AssertionError("[rtm08] " + what);
		}
	}

	private static boolean wanted(String id) {
		String only = System.getenv("RTM08_SHOTS");
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
				feedPit(PIT_SLURRY_PER_SECOND / 20);
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


	private static void hand(PlantBlockEntity... parts) {
		for (PlantBlockEntity p : parts) {
			setHoa(p, "HAND");
		}
	}

	private static void off(PlantBlockEntity... parts) {
		for (PlantBlockEntity p : parts) {
			setHoa(p, "OFF");
		}
	}

	private static int count(ServerLevel level, BlockPos at, Item item) {
		int n = 0;
		if (level.getBlockEntity(at) instanceof Container c) {
			for (int i = 0; i < c.getContainerSize(); i++) {
				if (c.getItem(i).is(item)) {
					n += c.getItem(i).getCount();
				}
			}
		}
		return n;
	}

	/** Steel slurry pipe, each length told of its neighbours so the run draws joined (Trailer.pipe). */
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
	// ---- the site -----------------------------------------------------------------------------------------------------------------

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

	/**
	 * The dry stack at q (the P4 film's layout: the load-out on the road, the press over its bin, the thickener east of it, a crest 6 m high
	 * north of the road), the paste plant at p (on and in a hill), the pit's 45 m thickener and lined cell at t.
	 */
	private void stage() {
		server.runOnServer(srv -> {
			ServerLevel level = srv.overworld();
			BlockPos s = Trailer.site(level, -1600, 2600, 330, 180);
			int g = s.getY();
			q = new BlockPos(s.getX() + 70, g, s.getZ() + 50);
			o = q;
			p = q.offset(120, 0, 30);
			t = q.offset(200, 0, 0);
			Trailer.pad(level, q.getX() - 70, q.getZ() - 50, q.getX() + 260, q.getZ() + 125, g, 16, 40);
			Trailer.forceChunks(level, q.getX() - 90, q.getZ() - 70, q.getX() + 280, q.getZ() + 145);
			// ---- the dump: the ground north of the crest dug 6 m down
			Trailer.fillBox(level, q, Blocks.STONE, -30, 60, -8, -7, -42, -11);
			Trailer.fillBox(level, q, Blocks.AIR, -30, 60, -6, -1, -42, -11);
			// ---- dry stack: tails (as from the bank) into the thickener; its underflow up to the press over the load-out
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
			tanksSmall.add(dayTank(level, d(47, 0, 80)));
			tanksSmall.add(dayTank(level, d(46, 0, 79)));
			tanksSmall.add(dayTank(level, d(46, 0, 81)));
			// ---- paste: a hill of stone, a drive branch inside it closed by bulkheads, its east wall glass, lit from its roof
			Trailer.fillBox(level, p, Blocks.STONE, 0, 15, 0, 12, -3, 15);
			Trailer.fillBox(level, p, Blocks.AIR, 5, 14, 2, 7, 5, 10);
			Trailer.fillBox(level, p, TailingsBlocks.FILL_BULKHEAD, 5, 14, 2, 7, 4, 4);
			Trailer.fillBox(level, p, Blocks.GLASS, 15, 15, 2, 7, 4, 11);
			for (int[] c : new int[][] {{6, 6}, {13, 6}, {6, 9}, {13, 9}, {8, 7}}) {
				level.setBlock(pp(c[0], 8, c[1]), Blocks.GLOWSTONE.defaultBlockState(), Block.UPDATE_ALL);
			}
			s2 = plant(level, Machine.FLOTATION_BANK, pp(0, 0, -49));
			boxCells.remove(box(s2));
			pipe(level, pp(0, 0, -36));
			thick2 = plant(level, Machine.TAILINGS_THICKENER, pp(0, 0, -35));
			mixer = plant(level, Machine.PASTE_MIXER, pp(1, 13, 1));
			pfilter = plant(level, Machine.PASTE_FILTER, pp(4, 13, -2));
			require(mixer.touching(Machine.PASTE_FILTER) == pfilter, "the paste filter touches the mixer");
			pump = plant(level, Machine.PASTE_PUMP, pp(1, 13, 7));
			List<BlockPos> feed = new ArrayList<>(line(pp(2, 0, -20), pp(2, 0, -4)));
			feed.addAll(line(pp(2, 1, -4), pp(2, 13, -4)));
			feed.addAll(List.of(pp(2, 13, -3), pp(3, 13, -3), pp(4, 13, -3), pp(2, 13, -2), pp(2, 13, -1), pp(2, 13, 0)));
			pipe(level, feed.toArray(BlockPos[]::new));
			pipe(level, pp(2, 13, 6));
			level.setBlock(pp(2, 16, 3), Blocks.HOPPER.defaultBlockState(), Block.UPDATE_ALL);
			level.setBlock(pp(2, 17, 3), Blocks.CHEST.defaultBlockState(), Block.UPDATE_ALL);
			cementChest = pp(2, 17, 3);
			// the fill line: out of the pump's south face, down to the ground, coiled for its length, back and down a borehole into the branch
			List<BlockPos> fill = new ArrayList<>(line(pp(2, 13, 13), pp(2, 13, 16)));
			fill.addAll(line(pp(2, 12, 16), pp(2, 0, 16)));
			fill.add(pp(2, 0, 17));
			for (int r = 0; r <= 8; r++) {
				int z = 18 + 2 * r;
				boolean east = r % 2 == 0;
				int from = r == 0 ? 2 : east ? -20 : 42, to = east ? 42 : -20;
				fill.addAll(line(pp(from, 0, z), pp(to, 0, z)));
				if (r < 8) {
					fill.add(pp(to, 0, z + 1));
				}
			}
			fill.addAll(List.of(pp(43, 0, 34), pp(44, 0, 34)));
			fill.addAll(line(pp(44, 0, 33), pp(44, 0, 16)));
			fill.addAll(line(pp(43, 0, 16), pp(10, 0, 16)));
			fill.addAll(line(pp(10, 1, 16), pp(10, 8, 16)));
			fill.addAll(line(pp(10, 8, 15), pp(10, 8, 9)));
			for (BlockPos c : fill) {
				level.setBlock(c, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
			}
			pipe(level, fill.toArray(BlockPos[]::new));
			outletPos = pp(10, 7, 9);
			level.setBlock(outletPos, TailingsBlocks.FILL_OUTLET.defaultBlockState(), Block.UPDATE_ALL);
			log("fill line laid: " + fill.size() + " lengths of pipe");
			// ---- the pit: the row's last tank cell (its tails staged), the 45 m thickener, its underflow to the lined cell
			cellSrc = plant(level, Machine.TANK_CELL, pt(22, 0, 5));
			boxCells.remove(box(cellSrc));
			pipe(level, line(pt(22, 0, 12), pt(22, 0, 19)).toArray(BlockPos[]::new));
			big = plant(level, Machine.TAILINGS_THICKENER_LARGE, pt(0, 0, 20));
			Block liner = wc("wildcatter:cell_liner");
			Trailer.fillBox(level, t, liner, 11, 32, -1, -1, 79, 100);
			Trailer.fillBox(level, t, Blocks.AIR, 12, 31, 0, 6, 80, 99);
			for (int x = 11; x <= 32; x++) {
				for (int z = 79; z <= 100; z++) {
					if (x == 11 || x == 32 || z == 79 || z == 100) {
						for (int y = 0; y <= 2; y++) {
							level.setBlock(pt(x, y, z), liner.defaultBlockState(), Block.UPDATE_ALL);
						}
					}
				}
			}
			towerPos = pt(22, 0, 90);
			level.setBlock(towerPos, wc("wildcatter:decant_tower").defaultBlockState(), Block.UPDATE_ALL);
			List<BlockPos> uf = new ArrayList<>(line(pt(22, 0, 65), pt(22, 0, 77)));
			uf.addAll(line(pt(22, 1, 77), pt(22, 3, 77)));
			uf.addAll(line(pt(22, 3, 78), pt(22, 3, 90)));
			uf.addAll(List.of(pt(22, 2, 90), pt(22, 1, 90)));
			pipe(level, uf.toArray(BlockPos[]::new));
			water(level, pt(22, 0, 91), pt(22, 1, 91), pt(22, 2, 91), pt(22, 3, 91));
			for (BlockPos c : line(pt(22, 3, 92), pt(22, 3, 101))) {
				water(level, c);
			}
			water(level, pt(22, 2, 101), pt(22, 1, 101), pt(22, 0, 101));
			tanksCell.add(dayTank(level, pt(21, 0, 101)));
			tanksCell.add(dayTank(level, pt(23, 0, 101)));
			tanksCell.add(dayTank(level, pt(22, 0, 102)));
			water(level, pt(45, 0, 30), pt(46, 0, 30));
			tanksBig.add(dayTank(level, pt(47, 0, 30)));
			tanksBig.add(dayTank(level, pt(46, 0, 29)));
			tanksBig.add(dayTank(level, pt(46, 0, 31)));
			for (PlantBlockEntity part : all()) {
				setHoa(part, "OFF");
			}
		});
		context.waitTicks(60);
		server.runOnServer(srv -> {
			if (srv.overworld().getBlockEntity(towerPos) instanceof DecantTowerBlockEntity tower) {
				tower.addSolidsForTest(1_150_000);   // part-filled for the film: about a layer and a third
			}
		});
		server.runCommand("kill @e[type=item]");
		server.runCommand("kill @e[type=!player]");
	}

	private List<PlantBlockEntity> all() {
		return List.of(thick, filter, frame, thick2, pfilter, mixer, pump, big);
	}

	/** Three diesel sets: the dry stack's east of it, the paste plant's west of it, the pit's west of its thickener. */
	private void power() {
		server.runOnServer(srv -> {
			ServerLevel level = srv.overworld();
			// west of the road, clear of the truck's loop: the load-out's gate and the press, by the frame's west side
			BlockPos a = set(level, d(-20, 0, 30), false);
			setNames.add("Dry stack: press, load-out (diesel)");
			wire(level, LV, ConductorSize.KCMIL_500, a.east(), box(frame), box(filter));
			// east: the paste plant and the dry stack's thickener, on the ground, no run over the road
			BlockPos b = set(level, d(84, 0, 12), false);
			setNames.add("Paste plant and the dry stack's thickener (diesel)");
			wire(level, LV, ConductorSize.KCMIL_500, b.east(), box(thick2), box(pfilter), box(mixer), box(pump), box(thick));
			BlockPos c = set(level, d(200, 0, -20), false);
			setNames.add("Pit: 45 m thickener (diesel)");
			wire(level, LV, ConductorSize.KCMIL_500, c.east(), box(big));
		});
		context.waitTicks(40);
		server.runOnServer(srv -> {
			for (BlockPos panel : panels) {
				((GensetControlPanelBlockEntity) srv.overworld().getBlockEntity(panel)).setWantRun(true);
			}
			keep(srv);
		});
		ticks(400);
		server.runOnServer(srv -> hand(thick, thick2, big, frame));
		ticks(60);
		server.runOnServer(srv -> hand(filter, pfilter, mixer));
		ticks(100);
		server.runOnServer(srv -> hand(pump));
		ticks(200);
		server.runOnServer(srv -> {
			for (BlockPos panel : panels) {
				log("set " + panel.toShortString() + ": " + ((GensetControlPanelBlockEntity) srv.overworld().getBlockEntity(panel)).inspect().getString());
			}
			keeps = 9;
			keep(srv);
		});
	}

	// ---- keeping the stand-ins up -------------------------------------------------------------------------------------------------

	private static double drain(ServerLevel level, List<BlockPos> tanks) {
		double got = 0;
		for (BlockPos at : tanks) {
			if (level.getBlockEntity(at) instanceof DayTankBlockEntity tank) {
				got += tank.waterLevel() * 1000;
				tank.setLitres(0);
			}
		}
		return got;
	}

	private volatile boolean filming;

	/** The row's tails, 1,000 t/h of solids at 35 %: a second's worth from keep(), a tick's worth each tick while filming. */
	private void feedPit(double kg) {
		if (cellSrc != null && cellSrc.out().held(Product.TAILS_SLURRY) < 3 * PIT_SLURRY_PER_SECOND) {
			cellSrc.out().offer(Product.TAILS_SLURRY, kg, tails());
		}
	}

	/** Every second: fuel, the tails into each thickener, the cement chest, the water tanks drawn down, the vehicles' fuel. */
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
		if (s1 == null) {
			return;
		}
		for (PlantBlockEntity src : new PlantBlockEntity[] {s1, s2}) {
			if (src.out().held(Product.TAILS_SLURRY) < 400) {
				src.out().offer(Product.TAILS_SLURRY, 400 - src.out().held(Product.TAILS_SLURRY), tails());   // as if from the flotation bank
			}
		}
		if (!filming) {
			feedPit(PIT_SLURRY_PER_SECOND);
		}
		if (level.getBlockEntity(cementChest) instanceof Container c && count(level, cementChest, TailingsItems.CEMENT) < 64) {
			for (int i = 0; i < c.getContainerSize(); i++) {
				if (c.getItem(i).isEmpty()) {
					c.setItem(i, new ItemStack(TailingsItems.CEMENT, 64));
					break;
				}
			}
		}
		drainedSmall += drain(level, tanksSmall);
		drainedBig += drain(level, tanksBig);
		drainedCell += drain(level, tanksCell);
		for (UUID id : new UUID[] {truckId, dozerId}) {
			if (id != null && level.getEntity(id) instanceof dev.wildcatter.tailings.entity.PitVehicleEntity v) {
				v.setFuel(v.tank());
			}
		}
		if (++keeps % 10 == 0) {
			log(String.format(Locale.ROOT, "tick %.1f ms", srv.getAverageTickTimeNanos() / 1e6));
			for (PlantBlockEntity part : all()) {
				log("  " + part.inspect().getString());
			}
			if (level.getBlockEntity(towerPos) instanceof DecantTowerBlockEntity tower) {
				log("  " + tower.inspect().getString());
			}
			if (level.getBlockEntity(outletPos) instanceof FillOutletBlockEntity outlet) {
				log("  outlet: " + outlet.status());
			}
			log(String.format(Locale.ROOT, "  water back: small %.0f L, 45 m %.0f L, cell %.0f L; press cycle %.0f s, %d cakes; bin %.1f t", drainedSmall, drainedBig, drainedCell,
					filter.cycle() / 20.0, filter.cycles(), bin.dryKg() / 1000));
		}
	}

	// ---- readouts -----------------------------------------------------------------------------------------------------------------

	private static String rd(PlantBlockEntity part, String name, String kw) {
		String st = part.status();
		boolean fault = st.startsWith("NO ") || st.startsWith("NOT ") || st.startsWith("FULL") || st.startsWith("waiting") || st.startsWith("LINE");
		if (fault) {
			return (st.startsWith("waiting") ? "" : "!") + name + ": " + brief(st);
		}
		return part.running() ? "+" + name + " (" + kw + "): " + f0(amps(part)) + " A, " + brief(st) : name + ": " + brief(st);
	}

	/** As rd(), with the status cut to its first words: for cards with long lines. */
	private static String rs(PlantBlockEntity part, String name, String kw, String doing) {
		String st = part.status();
		if (st.startsWith("NO ") || st.startsWith("NOT ") || st.startsWith("FULL") || st.startsWith("LINE")) {
			return "!" + name + ": " + brief(st);
		}
		return part.running() ? "+" + name + " (" + kw + "): " + f0(amps(part)) + " A, " + doing : name + ": " + brief(st);
	}

	private static String brief(String st) {
		int cut = st.indexOf(':');
		if (st.startsWith("NO ") || st.startsWith("NOT ") || st.startsWith("FULL")) {
			return cut > 0 ? st.substring(0, cut) : st;
		}
		return st.length() > 30 ? st.substring(0, 30) : st;
	}

	private final Map<BlockPos, Double> lastSolids = new java.util.HashMap<>();

	/** The underflow's solids as last seen on its way out (the pipe takes it as fast as it is made). */
	private double solids(PlantBlockEntity part) {
		double now = Math.max(part.out().assayOf(Product.TAILS_SLURRY).get(DryStack.SOLIDS_KEY), part.back().assayOf(Product.TAILS_SLURRY).get(DryStack.SOLIDS_KEY)) * 100;
		if (now > 0) {
			lastSolids.put(part.getBlockPos(), now);
		}
		return lastSolids.getOrDefault(part.getBlockPos(), 0.0);
	}

	private String[] thickCard(ServerLevel level) {
		return new String[] {"^TAILINGS THICKENER · SMALL PLANT", "Tails in: 50 t/h of solids at 35 % (staged)", rd(thick, "Thickener, 15 m", "11 kW"),
				String.format(Locale.ROOT, "Underflow: %.0f %% solids, up to the press", solids(filter)), "Overflow water back: " + f1(drainedSmall / 1000) + " m³"};
	}

	private String[] pressCard(ServerLevel level) {
		int sec = (int) (filter.cycle() / 20);
		List<String> l = new ArrayList<>(List.of("^TAILINGS FILTER · BATCH PRESS", rd(filter, "Filter press", "150 kW"),
				String.format(Locale.ROOT, "  cycle %d:%02d of 15:00 (%s)", sec / 60, sec % 60, DryStack.phase(sec)), String.format(Locale.ROOT, "  in the press: %.1f t", filter.dryKg() / 1000),
				"Cakes dropped: " + filter.cycles(), String.format(Locale.ROOT, "Load-out bin under it: %.1f t (holds 400 t)", bin.dryKg() / 1000)));
		if (!note.isEmpty()) {
			l.add(note);
		}
		return l.toArray(String[]::new);
	}

	private String[] pasteCard(ServerLevel level) {
		Paste.Line ln = Paste.line(pump);
		String flow = ln == null || ln.pressure <= 0 && !pump.status().startsWith("pumping") ? "-"
				: String.format(Locale.ROOT, "%.0f m³/h at %.1f MPa, %d m of line", Paste.flowM3PerHour(pump), ln.pressure / 1e6, ln.pipes);
		String fill = level.getBlockEntity(outletPos) instanceof FillOutletBlockEntity outlet ? outlet.filled() + " blocks of paste in the branch" : "-";
		return new String[] {"^PASTE FILL · SMALL PLANT", rs(thick2, "Thickener, 15 m", "11 kW", "thickening"), rs(pfilter, "Disc filter", "75 kW", "cake at 80 %"),
				rs(mixer, "Paste mixer", "75 kW", "78 %, 5 % cement"), rs(pump, "Piston pump", "110 kW", "pumping"), "  " + flow, "Fill outlet: " + fill};
	}

	private String[] pitCard(ServerLevel level) {
		return new String[] {"^PIT PLANT · 45 M TAILINGS THICKENER", "Tails in: 1,000 t/h of solids at 35 % (staged)", rd(big, "Thickener, 45 m", "55 kW"),
				"Underflow: by pipe over the rim of the lined cell", "Overflow water back: " + f1(drainedBig / 1000) + " m³"};
	}

	private String[] cellCard(ServerLevel level) {
		if (!(level.getBlockEntity(towerPos) instanceof DecantTowerBlockEntity tower)) {
			return new String[] {"^LINED CELL"};
		}
		return new String[] {"^LINED TAILINGS CELL · DECANT TOWER", String.format(Locale.ROOT, "Settled tails: %,.1f of %,.0f t", tower.settledKg() / 1000, tower.capacityKg() / 1000),
				"  (part-filled for the film)", "Decant water back: " + f1(drainedCell / 1000) + " m³", rd(big, "Thickener, 45 m", "55 kW")};
	}

	private String[] haulCard(ServerLevel level) {
		List<String> l = new ArrayList<>(List.of("^LOAD-OUT AND DUMP", String.format(Locale.ROOT, "Load-out bin: %.1f t of dry tails", bin.dryKg() / 1000),
				frame.running() ? "+Load-out gate (7.5 kW): " + f0(amps(frame)) + " A, loading" : "Load-out gate: waiting for a truck"));
		if (truckId != null && level.getEntity(truckId) instanceof HaulTruckEntity truck) {
			String st = truck.status();
			l.add((st.startsWith("NO BERM") ? "!" : "") + "Truck (91 t): " + (st.length() > 34 ? st.substring(0, 34) : st));
			l.add("  carrying " + truck.load().tonnes());
		}
		if (dozerId != null && level.getEntity(dozerId) instanceof TrackDozerEntity dozer) {
			l.add("Dozer: " + dozer.bladeBlocks() + " blocks of dry tails on its blade");
		}
		if (!note.isEmpty()) {
			l.add(note);
		}
		return l.toArray(String[]::new);
	}

	// ---- the films ----------------------------------------------------------------------------------------------------------------

	private void thickenerFilm() {
		if (!wanted("rtm08_thickener")) {
			return;
		}
		showCard(true);
		Cam a = dcam(60, 16, 66, 37, 2, 82);
		Cam b = dcam(58, 13, 100, 37, 2, 82);
		film("rtm08_thickener", 1000, move(a, b, 1000), (level, k) -> card(thickCard(level)));
		showCard(false);
	}

	/** The press while it squeezes, over the load-out bin. */
	private void pressFilm() {
		if (!wanted("rtm08_press")) {
			return;
		}
		boolean ok = waitFor(18000, level -> filter.cycle() / 20 >= 125 && filter.cycle() / 20 < 200);
		log("press squeezing: " + ok + ", " + filter.status());
		showCard(true);
		Cam a = dcam(36, 24, 50, 19.5, 17, 66);
		Cam b = dcam(38, 21, 82, 19.5, 15, 68);
		film("rtm08_press", 1100, move(a, b, 1100), (level, k) -> card(pressCard(level)));
		showCard(false);
	}

	private void pasteFilm() {
		if (!wanted("rtm08_paste")) {
			return;
		}
		showCard(true);
		Cam a = pcam(-10, 21, -10, 4, 14, 3);
		Cam b = pcam(-9, 19, 18, 3, 14, 6);
		film("rtm08_paste", 1100, move(a, b, 1100), (level, k) -> card(pasteCard(level)));
		showCard(false);
	}

	/** Through the glass wall into the closed branch: paste filling it from the floor up. */
	private void voidFilm() {
		if (!wanted("rtm08_void")) {
			return;
		}
		showCard(true);
		Cam a = pcam(26, 6, 7.5, 9, 3, 7.5);
		Cam b = pcam(20, 5, 9, 9, 3, 7);
		film("rtm08_void", 1100, move(a, b, 1100), (level, k) -> card(pasteCard(level)));
		showCard(false);
	}

	/** Over the fill line coiled on the ground, the pump's pressure on the card. */
	private void lineFilm() {
		if (!wanted("rtm08_line")) {
			return;
		}
		showCard(true);
		Cam a = pcam(-22, 22, 44, 8, 2, 24);
		Cam b = pcam(40, 24, 48, 14, 2, 22);
		film("rtm08_line", 900, move(a, b, 900), (level, k) -> card(pasteCard(level)));
		showCard(false);
	}

	private void pitFilm() {
		if (!wanted("rtm08_pit")) {
			return;
		}
		showCard(true);
		Cam a = tcam(-22, 42, 4, 22, 2, 42);
		Cam b = tcam(66, 38, 24, 22, 2, 46);
		film("rtm08_pit", 1200, move(a, b, 1200), (level, k) -> card(pitCard(level)));
		showCard(false);
	}

	private void cellFilm() {
		if (!wanted("rtm08_cell")) {
			return;
		}
		showCard(true);
		Cam a = tcam(48, 18, 80, 22, 0, 90);
		Cam b = tcam(46, 15, 106, 22, 0, 90);
		film("rtm08_cell", 1100, move(a, b, 1100), (level, k) -> card(cellCard(level)));
		showCard(false);
	}

	/** The end of a cycle: the press opens and drops its cake into the bin. */
	private void dropFilm() {
		if (!wanted("rtm08_drop")) {
			return;
		}
		int startCycles = server.computeOnServer(srv -> filter.cycles());
		boolean ok = waitFor(20000, level -> filter.cycles() > startCycles || filter.cycle() / 20 >= 862);
		log("press near the end of its cycle: " + ok + ", " + filter.status());
		showCard(true);
		Cam a = dcam(36, 17, 54, 19.5, 14, 66);
		Cam b = dcam(38, 14, 76, 19.5, 13, 66);
		int[] seen = {-1};
		film("rtm08_drop", 1000, move(a, b, 1000), (level, k) -> {
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

	/** Behind and beside the truck, eased toward where it is. */
	private IntFunction<Cam> follow(double side, double back, double up) {
		double[] at = {Double.NaN, 0, 0, 0, 0, 0};
		return k -> {
			double yaw = Math.toRadians(tyaw);
			double fx = -Math.sin(yaw), fz = Math.cos(yaw);   // its heading
			double cx = tx - fx * back + fz * side, cz = tz - fz * back - fx * side;
			double ty = q.getY() + 2.5;
			if (Double.isNaN(at[0])) {
				at[0] = cx;
				at[1] = cz;
				at[2] = tx;
				at[3] = tz;
			}
			at[0] += (cx - at[0]) * 0.02;
			at[1] += (cz - at[1]) * 0.02;
			at[2] += (tx - at[2]) * 0.08;
			at[3] += (tz - at[3]) * 0.08;
			return new Cam(at[0], q.getY() + up, at[1], at[2], ty, at[3]);
		};
	}

	/** A self-driving truck through the load-out's lane, filled under the gate, round its loop, to the crest: NO BERM. */
	private void haulFilm() {
		if (!wanted("rtm08_haul") && !wanted("rtm08_berm")) {
			return;
		}
		server.runOnServer(srv -> {
			ServerLevel level = srv.overworld();
			bin.setDryKg(Math.max(bin.dryKg(), 100_000));   // stocked for the film
			int[] zs = {-8, 25, 45, 59};
			StopKind[] kinds = {StopKind.DUMP_POINT, StopKind.WAYPOINT, StopKind.WAYPOINT, StopKind.LOADOUT};
			List<BlockPos> route = new ArrayList<>();
			for (int i = 0; i < zs.length; i++) {
				BlockPos at = d(19, 0, zs[i]);
				level.setBlock(at, TailingsBlocks.SURVEY_STAKE.defaultBlockState().setValue(SurveyStakeBlock.KIND, kinds[i]), Block.UPDATE_ALL);
				route.add(at);
			}
			HaulTruckEntity truck = TailingsEntities.HAUL_TRUCK.create(level, EntitySpawnReason.COMMAND);
			truck.setPos(q.getX() + 19.5, q.getY(), q.getZ() + 38);
			truck.setYRot(0);
			truck.setFuel(truck.tank());
			level.addFreshEntity(truck);
			log("truck: " + truck.takeRoute(level, route));
			truckId = truck.getUUID();
			tx = truck.getX();
			tz = truck.getZ();
			tyaw = truck.getYRot();
			TrackDozerEntity dozer = TailingsEntities.TRACK_DOZER.create(level, EntitySpawnReason.COMMAND);
			dozer.setPos(q.getX() + 6.5, q.getY(), q.getZ() - 3.5);
			dozer.setYRot(-90);
			dozer.setFuel(dozer.tank());
			dozer.loadBlade(PitMass.DRY_TAILS, 24);   // loaded with dry tails for the film
			level.addFreshEntity(dozer);
			dozerId = dozer.getUUID();
		});
		showCard(true);
		int[] noBerm = {-1};
		endMove = false;
		film("rtm08_haul", 7000, follow(24, 6, 13), (level, k) -> {
			if (level.getEntity(truckId) instanceof HaulTruckEntity truck) {
				tx = truck.getX();
				tz = truck.getZ();
				tyaw = truck.getYRot();
				if (truck.status().startsWith("NO BERM") && noBerm[0] < 0) {
					noBerm[0] = k;
					event("truck: " + truck.status());
				}
			}
			if (noBerm[0] >= 0 && k == noBerm[0] + 160) {
				endMove = true;
			}
			card(haulCard(level));
		});
		showCard(false);
		require(noBerm[0] >= 0, "the truck should stop short of the unbermed crest");
	}

	/** From the dump's floor: the dozer pushes a berm along the crest; the truck backs to it and tips over it, down the face. */
	private void bermFilm() {
		if (!wanted("rtm08_berm") || truckId == null) {
			return;
		}
		showCard(true);
		Cam a = dcam(30, 1, -27, 17, 1, -9);
		Cam b = dcam(10, 2, -25, 18, 1, -9);
		int[] state = {0, -1};
		endMove = false;
		film("rtm08_berm", 3000, move(a, b, 3000), (level, k) -> {
			int s = k - PAD;
			TrackDozerEntity dozer = level.getEntity(dozerId) instanceof TrackDozerEntity dz ? dz : null;
			HaulTruckEntity truck = level.getEntity(truckId) instanceof HaulTruckEntity tr ? tr : null;
			if (s == 40 && dozer != null) {
				dozer.testDrive(new Input(true, false, false, false, false, false, false));
				note = "Dozer driven along the crest";
				event("dozer forward");
				state[0] = 1;
			}
			if (state[0] == 1 && dozer != null && (dozer.getX() - q.getX() > 30 || dozer.bladeBlocks() <= 0)) {
				dozer.testDrive(Input.EMPTY);
				state[0] = 2;
				note = "+A berm along the crest";
				event("dozer stopped: " + dozer.status());
			}
			if (truck != null && truck.load().empty() && state[1] < 0) {
				state[1] = k;
				note = "+Tipped over the berm";
				event("truck tipped: " + truck.status());
			}
			if (state[1] >= 0 && k == state[1] + 200) {
				endMove = true;
			}
			card(haulCard(level));
		});
		note = "";
		showCard(false);
	}
}
