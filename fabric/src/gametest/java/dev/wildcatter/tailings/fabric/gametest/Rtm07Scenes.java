package dev.wildcatter.tailings.fabric.gametest;

import dev.wildcatter.block.BigPartBlock;
import dev.wildcatter.block.GensetControlPanelBlockEntity;
import dev.wildcatter.block.GensetPartBlockEntity;
import dev.wildcatter.block.GensetSkid;
import dev.wildcatter.block.StorageTankBlockEntity;
import dev.wildcatter.control.Measurable;
import dev.wildcatter.energy.ConductorSize;
import dev.wildcatter.fluid.Product;
import dev.wildcatter.fluid.SlurryAssay;
import dev.wildcatter.multiblock.BigParts;
import dev.wildcatter.tailings.TailingsBlocks;
import dev.wildcatter.tailings.block.BeltConveyorBlock;
import dev.wildcatter.tailings.block.PlantBlockEntity;
import dev.wildcatter.tailings.block.TailingsPartBlock;
import dev.wildcatter.tailings.fabric.gametest.Trailer.Cam;
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
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HopperBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.Vec3;

/**
 * Rock to Metal, episode 7, "The Last Quarter" (TAILINGS_ONLY=rtm07): the small plant's copper line taken past flotation's 75 %
 * (full-recovery.md), every motor on real power from Wildcatter generator sets at the industrial voltage, every cable, busway, pipe and
 * belt run laid joined.
 * <ul>
 * <li>copper fines into the conditioning tank and the flotation bank, its concentrate to a thickener and filter;</li>
 * <li>the bank's tails to the scavenger bank and its blower, the scavengers' concentrate to the regrind mill, the cleaner bank, and a
 * thickener and filter of their own (so the card counts the step apart);</li>
 * <li>the scavengers' tails into the leach tank, with sulfuric acid by pipe from a stock tank; the CCD thickener; the pregnant liquor to
 * the electrowinning bank, which pays copper nuggets and ingots;</li>
 * <li>an ore bin of andesite (the waste bin's stand-in) feeding a belt onto the heap leach pad, its acid from a second tank, its liquor
 * joining the same cells;</li>
 * <li>the by-product plant working lead ingots for their silver (handed in, as a player does);</li>
 * <li>a spare gas set with a flotation bank and its air blowers, to film a bank's start dipping the frequency, and tripping the set.</li>
 * </ul>
 * Filmed in real time at 2880 x 1620 with a readout card, read from the blocks each tick. RTM07_SHOTS=a,b films only those.
 * <p>
 * Staged, and said so in the episode: the feed (copper ore fines topped up as if from the mill's cyclones); the reagent chest kept
 * stocked; the acid in its tanks; the andesite in the bin; the lead ingots handed in; the CCD's washed tails drawn off as if to the
 * tailings line; the spare set's bank fed a stand-in slurry.
 */
final class Rtm07Scenes {
	private static final int W = 1440, H = 810;
	private static final int PAD = 60;
	private static final String SEED = System.getenv().getOrDefault("TAILINGS_TRAILER_SEED", "tailings");
	private static final String LV = "wildcatter:cable_industrial", BUS = "wildcatter:busway_industrial", MV = "wildcatter:cable_mv";
	/** The spare set's rig, east of the plant. */
	private static final int RIG_DX = 110;
	/** The set yards' panels, west of the plant. */
	private static final int YARD_X = -48;

	private final ClientGameTestContext context;
	private TestServerContext server;
	private BlockPos o, q, r;
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

	// the line
	private PlantBlockEntity src, cond, bank, blower, thick, filter;
	private PlantBlockEntity scav, scavBlower, regrind, cleaner, thick2, filter2;
	private PlantBlockEntity leach, ccd, ew, heap, drive, bin, byp;
	private BlockPos condChest, hopper1, hopper2, acidTank, heapTank, acidCut;
	// the spare set's rig
	private PlantBlockEntity rsrc, rbank;
	private final List<PlantBlockEntity> rblowers = new ArrayList<>();
	private BlockPos rigPanel;
	private BlockPos[] ewPanels;
	private String note = "";

	private static final Mineral CU = Mineral.COPPER;
	/** Copper in a kg of the fines slurry: a copper ore block's 400 kg in its 2.7 t of rock, at 35 % solids (as Rtm05Scenes.RICH). */
	private static final double RICH = Rock.COPPER.contained(CU).doubleValue() * CU.kgPerUnit() / (Reagent.BLOCK_TONNES.doubleValue() * 1000 / PlantRates.OVERFLOW_SOLIDS);

	Rtm07Scenes(ClientGameTestContext context) {
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
			server.runCommand("time set 1500");
			server.runCommand("difficulty peaceful");
			server.runCommand("gamemode spectator @p");
			server.runCommand("tp @p 0 160 0");
			context.waitTicks(20);
			hud(false);
			cardWidth("Scavengers, regrind, cleaners (own filter): 00.0 ingots");
			try {
				stage();
				power();
				rigTrials();
				warm();
				scavengerFilm();
				regrindFilm();
				leachFilm();
				ewFilm();
				heapFilm();
				byproductFilm();
				lineFilm();
				noAcidFilm();
				setsFilm();
				tripFilm();
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


	private static void log(String line) {
		System.out.println("[rtm07] " + line);
	}

	private static void require(boolean ok, String what) {
		if (!ok) {
			throw new AssertionError("[rtm07] " + what);
		}
	}

	private static boolean wanted(String id) {
		String only = System.getenv("RTM07_SHOTS");
		return only == null || only.isBlank() || java.util.Arrays.asList(only.split(",")).contains(id);
	}

	private BlockPos at(int x, int y, int z) {
		return o.offset(x, y, z);
	}

	private BlockPos sq(int x, int y, int z) {
		return q.offset(x, y, z);
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

	/** A camera relative to the pit row's origin. */
	private Cam cam(double x, double y, double z, double tx, double ty, double tz) {
		return Cam.of(o, x, y, z, tx, ty, tz);
	}

	/** A camera relative to the small plant's origin. */
	private Cam scam(double x, double y, double z, double tx, double ty, double tz) {
		return Cam.of(q, x, y, z, tx, ty, tz);
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


	/** A camera relative to the spare set's rig. */
	private Cam rcam(double x, double y, double z, double tx, double ty, double tz) {
		return Cam.of(r, x, y, z, tx, ty, tz);
	}

	private BlockPos sr(int x, int y, int z) {
		return r.offset(x, y, z);
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

	private static Item reagent(Reagent r) {
		return item(r.itemId());
	}

	/** A filter raised a cell over a hopper (facing west into a chest), as Rtm05Scenes lays the small plant's. Its hopper. */
	private static BlockPos underFilter(ServerLevel level, BlockPos hopper) {
		level.setBlock(hopper, Blocks.HOPPER.defaultBlockState().setValue(HopperBlock.FACING, Direction.WEST), Block.UPDATE_ALL);
		level.setBlock(hopper.west(), Blocks.CHEST.defaultBlockState(), Block.UPDATE_ALL);
		return hopper;
	}

	/** A hopper on a tank's top, a reagent chest on it. The chest. */
	private static BlockPos reagentRig(ServerLevel level, BlockPos hopper) {
		level.setBlock(hopper, Blocks.HOPPER.defaultBlockState(), Block.UPDATE_ALL);
		level.setBlock(hopper.above(), Blocks.CHEST.defaultBlockState(), Block.UPDATE_ALL);
		return hopper.above();
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

	/** A Wildcatter stock tank (4 x 6 x 4 from its main cell), filled with sulfuric acid: staged, as the playthrough stages it. */
	private static BlockPos acidTankAt(ServerLevel level, BlockPos main) {
		Block tank = wc("wildcatter:stock_tank");
		level.setBlock(main, tank.defaultBlockState(), Block.UPDATE_ALL);
		BigParts.grow(level, main, level.getBlockState(main));
		require(level.getBlockEntity(main) instanceof StorageTankBlockEntity, "stock tank at " + main.toShortString());
		((StorageTankBlockEntity) level.getBlockEntity(main)).testFill(Product.SULFURIC_ACID, 5000);
		return main;
	}

	private static BlockState belt(Direction.Axis axis) {
		return TailingsBlocks.BELT_CONVEYOR.defaultBlockState().setValue(BeltConveyorBlock.AXIS, axis);
	}

	/**
	 * The small plant's copper line and the steps after its bank (full-recovery.md's circuit, the parts facing north, laid as
	 * TailingsGameTests.fullRecovery and the playthrough lay them): the feed's stand-in, the conditioning tank, the bank and its blower,
	 * its concentrate east to a thickener and filter; its tails south to the scavenger bank and blower; the scavengers' concentrate east to
	 * the regrind mill, cleaner bank, a thickener and filter of their own; their tails into the leach tank (acid by pipe from a stock tank
	 * at its east side), the CCD thickener, the electrowinning bank. West of the CCD, the heap pad under a belt from an ore bin of
	 * andesite, its acid from a second tank, its liquor over to the same cells. Beside the yard, the by-product plant; east, the spare set's
	 * rig.
	 */
	private void stage() {
		server.runOnServer(srv -> {
			ServerLevel level = srv.overworld();
			BlockPos s = Trailer.site(level, -1600, 2400, 260, 160);
			int g = s.getY();
			q = new BlockPos(s.getX() + 70, g, s.getZ() + 40);
			o = q;
			r = q.offset(RIG_DX, 0, 0);
			Trailer.pad(level, q.getX() - 64, q.getZ() - 22, r.getX() + 26, q.getZ() + 112, g, 16, 40);
			Trailer.forceChunks(level, q.getX() - 84, q.getZ() - 42, r.getX() + 46, q.getZ() + 132);
			// ---- flotation, as episode 5's small plant
			src = plant(level, Machine.CYCLONE, sq(0, 0, 0));
			boxCells.remove(box(src));
			pipe(level, sq(0, 0, 2), sq(1, 0, 2));
			cond = plant(level, Machine.CONDITIONER, sq(0, 0, 3));
			pipe(level, sq(0, 0, 6));
			bank = plant(level, Machine.FLOTATION_BANK, sq(0, 0, 7));
			blower = plant(level, Machine.BLOWER, sq(-2, 0, 9));
			require(bank.touching(Machine.BLOWER) == blower, "the blower touches the bank");
			pipe(level, line(sq(6, 0, 20), sq(20, 0, 20)).toArray(BlockPos[]::new));   // the bank's concentrate
			thick = plant(level, Machine.THICKENER, sq(20, 0, 21));
			pipe(level, sq(20, 0, 29), sq(20, 1, 29));
			filter = plant(level, Machine.FILTER, sq(20, 1, 30));
			hopper1 = underFilter(level, sq(20, 0, 34));
			condChest = reagentRig(level, sq(1, 4, 4));
			// ---- 4a: the bank's tails to the scavengers; their concentrate to the regrind mill, the cleaners, a thickener and filter
			pipe(level, sq(0, 0, 20));
			scav = plant(level, Machine.SCAVENGER_BANK, sq(0, 0, 21));
			scavBlower = plant(level, Machine.BLOWER, sq(-2, 0, 22));
			require(scav.touching(Machine.BLOWER) == scavBlower, "the scavengers' blower touches them");
			pipe(level, line(sq(4, 0, 30), sq(12, 0, 30)).toArray(BlockPos[]::new));
			regrind = plant(level, Machine.REGRIND_MILL, sq(12, 0, 31));
			pipe(level, sq(12, 0, 36));
			cleaner = plant(level, Machine.CLEANER_BANK, sq(12, 0, 37));
			pipe(level, sq(12, 0, 43));
			thick2 = plant(level, Machine.THICKENER, sq(12, 0, 44));
			pipe(level, sq(12, 0, 52), sq(12, 1, 52));
			filter2 = plant(level, Machine.FILTER, sq(12, 1, 53));
			hopper2 = underFilter(level, sq(12, 0, 57));
			// ---- 4b: the scavengers' tails into the leach, acid at its east side, the wash, the cells
			pipe(level, sq(0, 0, 30));
			leach = plant(level, Machine.LEACH_TANK, sq(-2, 0, 31));
			pipe(level, sq(3, 0, 33), sq(4, 0, 33), sq(5, 0, 33));
			acidCut = sq(4, 0, 33);
			acidTank = acidTankAt(level, sq(6, 0, 32));
			pipe(level, sq(0, 0, 36));
			ccd = plant(level, Machine.CCD_THICKENER, sq(-3, 0, 37));
			pipe(level, sq(8, 0, 49));
			ew = plant(level, Machine.ELECTROWINNING_BANK, sq(4, 0, 50));
			// ---- the heap: an ore bin of andesite over a belt onto the pad, acid at its east side, its liquor round to the cells
			heap = plant(level, Machine.HEAP_PAD, sq(-20, 0, 39));
			pipe(level, sq(-10, 0, 41), sq(-9, 0, 41));
			heapTank = acidTankAt(level, sq(-8, 0, 40));
			List<BlockPos> liquor = new ArrayList<>(line(sq(-12, 0, 49), sq(-4, 0, 49)));
			liquor.addAll(line(sq(-4, 1, 49), sq(4, 1, 49)));
			pipe(level, liquor.toArray(BlockPos[]::new));
			for (BlockPos at : line(sq(-25, 3, 44), sq(-15, 3, 44))) {
				level.setBlock(at, belt(Direction.Axis.X), Block.UPDATE_ALL);
				level.updateNeighborsAt(at, TailingsBlocks.BELT_CONVEYOR, null);
			}
			drive = plant(level, Machine.CONVEYOR_DRIVE, sq(-14, 3, 43));
			bin = plant(level, Machine.ORE_BIN, sq(-26, 4, 43));
			int fed = 0;
			while (fed < 400 && bin.offer(new ItemStack(Items.ANDESITE))) {
				fed++;
			}
			log("andesite into the bin: " + fed + " blocks");
			// ---- 4c: the by-product plant
			byp = plant(level, Machine.BYPRODUCT_PLANT, sq(-22, 0, 58));
			// ---- the spare set's rig: a stand-in for the conditioned slurry, a bank, five air blowers round it
			rsrc = plant(level, Machine.CONDITIONER, sr(0, 0, 3));
			boxCells.remove(box(rsrc));
			pipe(level, sr(0, 0, 6));
			rbank = plant(level, Machine.FLOTATION_BANK, sr(0, 0, 7));
			// three along its west side clear of its motor box (west of its first cell), two against its south face (each blower's box
			// is west of it)
			for (int[] at : new int[][] {{-2, 10}, {-2, 13}, {-2, 16}, {2, 20}, {5, 20}}) {
				rblowers.add(plant(level, Machine.BLOWER, sr(at[0], 0, at[1])));
			}
			for (PlantBlockEntity p : all()) {
				setHoa(p, "OFF");
			}
			for (PlantBlockEntity p : rig()) {
				setHoa(p, "OFF");
			}
			log(String.format(Locale.ROOT, "feed: copper %.5f kg a kg of slurry", RICH));
		});
		server.runCommand("kill @e[type=item]");
		server.runCommand("kill @e[type=!player]");
	}

	private List<PlantBlockEntity> all() {
		return List.of(cond, bank, blower, thick, filter, scav, scavBlower, regrind, cleaner, thick2, filter2, leach, ccd, ew, heap, drive, byp);
	}

	private List<PlantBlockEntity> rig() {
		List<PlantBlockEntity> l = new ArrayList<>(rblowers);
		l.add(rbank);
		return l;
	}

	/**
	 * The yards west of the plant: the bank on a 1 MW gas set by busway; the tank, blowers, thickener and filter on a 500 kW diesel set;
	 * the scavengers, regrind mill, cleaners and their thickener and filter on a gas set by busway; the leach, CCD, heap pad and belt
	 * drive on a diesel set; the cells on two gas sets in parallel by busway. East: the by-product plant on a diesel set, and the spare gas
	 * set with its bank and blowers by busway.
	 */
	private void power() {
		server.runOnServer(srv -> {
			ServerLevel level = srv.overworld();
			BlockPos a = set(level, sq(YARD_X, 0, 6), true);
			setNames.add("Flotation bank (gas)");
			wire(level, BUS, null, a.east(), box(bank));
			BlockPos b = set(level, sq(YARD_X, 0, 22), false);
			setNames.add("Tank, blowers, thickener, filter (diesel)");
			wire(level, LV, ConductorSize.KCMIL_500, b.east(), box(scavBlower), box(blower), box(cond), box(thick), box(filter));
			BlockPos c = set(level, sq(YARD_X, 0, 38), true);
			setNames.add("Scavengers, regrind, cleaners (gas)");
			wire(level, BUS, null, c.east(), box(scav), box(regrind), box(cleaner), box(thick2), box(filter2));
			BlockPos d = set(level, sq(YARD_X, 0, 54), false);
			setNames.add("Leach, CCD, heap pad, belt (diesel)");
			wire(level, LV, ConductorSize.KCMIL_500, d.east(), box(drive), box(heap), box(leach), box(ccd));
			// the cells on two gas sets in parallel: alone on one, their start tripped it on frequency (56.0 Hz, run 3's log)
			BlockPos e = set(level, sq(YARD_X, 0, 70), true);
			setNames.add("Electrowinning bank, set 1 (gas)");
			BlockPos e2 = set(level, sq(YARD_X, 0, 86), true);
			setNames.add("Electrowinning bank, set 2 (gas)");
			ewPanels = new BlockPos[] {e, e2};
			Set<BlockPos> net = new HashSet<>();
			ConductorSize was = runSize;
			runSize = null;
			run(level, BUS, box(ew), e.east(), net, 0);
			run(level, BUS, e2.east(), null, net, 0);
			runSize = was;
			// the by-product plant beside the yard on a gas set of its own (a diesel set east of the plant left it reading NO POWER, run 4)
			BlockPos f = set(level, sq(YARD_X, 0, 102), true);
			setNames.add("By-product plant (gas)");
			wire(level, BUS, null, f.east(), box(byp));
			rigPanel = set(level, sr(-30, 0, 8), true);
			setNames.add("Spare set (gas)");
			List<BlockPos> boxes = new ArrayList<>();
			boxes.add(box(rbank));
			for (PlantBlockEntity bl : rblowers) {
				boxes.add(box(bl));
			}
			wire(level, BUS, null, rigPanel.east(), boxes.toArray(BlockPos[]::new));
		});
		context.waitTicks(40);
		server.runOnServer(srv -> {
			for (BlockPos panel : panels) {
				((GensetControlPanelBlockEntity) srv.overworld().getBlockEntity(panel)).setWantRun(true);
			}
			keep(srv);
		});
		ticks(400);
		server.runOnServer(srv -> hand(filter, thick, filter2, thick2, ccd, drive));
		ticks(60);
		server.runOnServer(srv -> hand(blower, scavBlower, cond, regrind, cleaner, leach, heap));
		ticks(100);
		server.runOnServer(srv -> hand(bank));
		ticks(100);
		server.runOnServer(srv -> hand(scav));
		ticks(100);
		boolean both = waitFor(2400, level -> {
			for (BlockPos p : ewPanels) {
				GensetControlPanelBlockEntity panel = (GensetControlPanelBlockEntity) level.getBlockEntity(p);
				if (!panel.breakerClosed() || panel.isTripped()) {
					return false;
				}
			}
			return true;
		});
		log("the cells' two sets both on the bus: " + both);
		server.runOnServer(srv -> hand(ew));
		ticks(200);
		server.runOnServer(srv -> {
			for (BlockPos panel : panels) {
				log("set " + panel.toShortString() + ": " + ((GensetControlPanelBlockEntity) srv.overworld().getBlockEntity(panel)).inspect().getString());
			}
			keeps = 9;
			keep(srv);
		});
	}

	// ---- the spare set: a bank's start ---------------------------------------------------------------------------------------------

	private GensetControlPanelBlockEntity rigSet(ServerLevel level) {
		return (GensetControlPanelBlockEntity) level.getBlockEntity(rigPanel);
	}

	private boolean rigLive(ServerLevel level) {
		GensetControlPanelBlockEntity p = rigSet(level);
		return p.run() == GensetControlPanelBlockEntity.Run.RUNNING && p.breakerClosed() && !p.isTripped();
	}

	/** Every rig motor off; the set reset if it tripped, and running again. */
	private boolean rigReset() {
		server.runOnServer(srv -> {
			off(rig().toArray(PlantBlockEntity[]::new));
			GensetControlPanelBlockEntity p = rigSet(srv.overworld());
			if (p.isTripped()) {
				p.pushButton();
			}
			p.setWantRun(true);
		});
		boolean up = waitFor(1200, this::rigLive);
		ticks(60);
		return up;
	}

	private volatile double rigMin = 99;
	private volatile String rigTrip = "";
	private int nOk = -1, nTrip = -1;

	/** Hz, or NaN when the set is not running. */
	private double rigHz(ServerLevel level) {
		return rigLive(level) ? rigSet(level).measure(Measurable.Quantity.HERTZ) : Double.NaN;
	}

	/** {@code n} blowers started one by one, then the bank: the lowest frequency and whether the set tripped, watched for 15 s. */
	private boolean trial(int n) {
		require(rigReset(), "the spare set should run");
		for (int i = 0; i < n; i++) {
			int k = i;
			server.runOnServer(srv -> hand(rblowers.get(k)));
			ticks(60);
		}
		ticks(100);
		rigMin = 99;
		rigTrip = "";
		server.runOnServer(srv -> hand(rbank));
		for (int t = 0; t < 300; t++) {
			context.waitTick();
			if (t % 20 == 0) {
				server.runOnServer(this::keep);
			}
			boolean done = server.computeOnServer(srv -> {
				ServerLevel level = srv.overworld();
				double hz = rigHz(level);
				if (!Double.isNaN(hz)) {
					rigMin = Math.min(rigMin, hz);
				}
				if (rigSet(level).isTripped()) {
					rigTrip = rigSet(level).inspect().getString();
					return true;
				}
				return false;
			});
			if (done) {
				break;
			}
		}
		String line = String.format(Locale.ROOT, "trial: %d blowers then the bank: lowest %.2f Hz, %s", n, rigMin, rigTrip.isEmpty() ? "no trip" : rigTrip);
		log(line);
		summary.add(line);
		boolean tripped = !rigTrip.isEmpty();
		rigReset();
		return tripped;
	}

	/** Unfilmed: how many blowers must already run on the spare set before the bank's start trips it. */
	private void rigTrials() {
		if (!(wanted("rtm07_set_trip"))) {
			return;
		}
		for (int n = 0; n <= 4; n++) {
			if (trial(n)) {
				nTrip = n;
				break;
			}
			nOk = n;
		}
		log("spare set: starts with " + nOk + " blowers running, trips with " + nTrip);
	}

	/** Until both filters and the cells pay, then a baseline for the ladder, then a few minutes more. */
	private final double[] base = new double[3];

	private void warm() {
		boolean ok = waitFor(18000, level -> paid(level, 0) > 0 && paid(level, 1) > 0 && paid(level, 2) > 0);
		log("both filters and the cells paying: " + ok);
		ticks(1200);
		server.runOnServer(srv -> {
			for (int i = 0; i < 3; i++) {
				base[i] = paid(srv.overworld(), i);
			}
			log(String.format(Locale.ROOT, "baseline: %.2f / %.2f / %.2f ingots", base[0], base[1], base[2]));
		});
		ticks(1200);
	}

	// ---- keeping the stand-ins up -------------------------------------------------------------------------------------------------

	private static SlurryAssay copper() {
		return SlurryAssay.of(Map.of(CU.mineralItemId(), RICH));
	}

	/** A tank's reagent chest, run as its level control would (Rtm06Scenes.stock). */
	private static void stock(ServerLevel level, BlockPos chest, PlantBlockEntity tank, Reagent... which) {
		if (!(level.getBlockEntity(chest) instanceof Container c)) {
			return;
		}
		for (Reagent r : which) {
			if (tank.reagents().kilograms(r).doubleValue() < 6 && count(level, chest, reagent(r)) == 0 && count(level, chest.below(), reagent(r)) == 0) {
				for (int i = 0; i < c.getContainerSize(); i++) {
					if (c.getItem(i).isEmpty()) {
						c.setItem(i, new ItemStack(reagent(r), 8));
						break;
					}
				}
			}
		}
	}

	private volatile boolean acidCutNow;

	/** Every second: fuel, the feed, the CCD's tails drawn off, the reagent chest, the acid tanks, the rig's stand-in and its outlets. */
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
		if (src == null) {
			return;
		}
		if (src.out().held(Product.FINES_SLURRY) < 400) {
			src.out().offer(Product.FINES_SLURRY, 400 - src.out().held(Product.FINES_SLURRY), copper());   // as if from the mill's cyclones
		}
		ccd.out().give(Product.TAILS_SLURRY, Double.MAX_VALUE);   // on to the tailings line
		stock(level, condChest, cond, Reagent.LIME, Reagent.XANTHATE, Reagent.MIBC);
		for (BlockPos t : new BlockPos[] {acidTank, heapTank}) {
			if (level.getBlockEntity(t) instanceof StorageTankBlockEntity tank && tank.amount() < 3000) {
				tank.testFill(Product.SULFURIC_ACID, 5000 - tank.amount());
			}
		}
		if (rsrc != null) {
			if (rsrc.out().held(Product.CONDITIONED_SLURRY) < 400) {
				rsrc.out().offer(Product.CONDITIONED_SLURRY, 400 - rsrc.out().held(Product.CONDITIONED_SLURRY), SlurryAssay.EMPTY);
			}
			rbank.out().give(Product.TAILS_SLURRY, Double.MAX_VALUE);
			rbank.out().give(Product.CONCENTRATE_SLURRY, Double.MAX_VALUE);
		}
		if (++keeps % 10 == 0) {
			log(String.format(Locale.ROOT, "tick %.1f ms", srv.getAverageTickTimeNanos() / 1e6));
			for (PlantBlockEntity p : all()) {
				log("  " + p.inspect().getString());
			}
			log(String.format(Locale.ROOT, "  paid: filter %.2f, scavengers' filter %.2f, cells %.2f ingots; heap holds %.1f t; bin %d", paid(level, 0), paid(level, 1), paid(level, 2),
					heap.hold().total() / 1000, bin.binCount()));
		}
	}

	/** Copper paid, in ingots: 0 the bank's filter, 1 the scavengers' filter, 2 the cells (their own two output stacks). */
	private double paid(ServerLevel level, int which) {
		Item nugget = item(CU.nuggetId()), ingot = Items.COPPER_INGOT;
		PlantBlockEntity f = which == 0 ? filter : which == 1 ? filter2 : ew;
		int in = f.countIn(new int[] {1, 2}, new ItemStack(ingot)), nug = f.countIn(new int[] {1, 2}, new ItemStack(nugget));
		if (which < 2) {
			BlockPos h = which == 0 ? hopper1 : hopper2;
			for (BlockPos at : new BlockPos[] {h, h.west()}) {
				in += count(level, at, ingot);
				nug += count(level, at, nugget);
			}
		}
		return in + nug / 9.0;
	}

	// ---- readouts -----------------------------------------------------------------------------------------------------------------

	/** "+name (kW): A" while it works, "!name: status" when it reads a fault, or "name: status". */
	private static String rd(PlantBlockEntity p, String name, String kw) {
		String st = p.status();
		boolean fault = st.startsWith("NO ") || st.startsWith("NOT BUILT") || st.startsWith("FULL") || st.startsWith("waiting");
		if (fault) {
			return (st.startsWith("waiting") ? "" : "!") + name + ": " + brief(st);
		}
		return p.running() ? "+" + name + " (" + kw + "): " + f0(amps(p)) + " A, " + brief(st) : name + ": " + brief(st);
	}

	private static String brief(String st) {
		if (st.startsWith("NO AIR")) {
			return "NO AIR";
		}
		if (st.startsWith("NO ACID")) {
			return "NO ACID";
		}
		return st.length() > 30 ? st.substring(0, 30) : st;
	}

	private String ladder(ServerLevel level) {
		double a = paid(level, 0) - base[0], b = paid(level, 1) - base[1], c = paid(level, 2) - base[2], t = a + b + c;
		if (t <= 0) {
			return "Shares of the copper paid: -";
		}
		return String.format(Locale.ROOT, "Shares of the copper paid: %.0f %% · %.0f %% · %.0f %%", 100 * a / t, 100 * b / t, 100 * c / t);
	}

	private String[] lineCard(ServerLevel level) {
		return new String[] {"^THE LAST QUARTER · COPPER", "Feed: copper ore fines, 50 t/h at 35 %", "Bank, to its own filter: " + f1(paid(level, 0)) + " ingots",
				"Scavengers, regrind, cleaners (own filter): " + f1(paid(level, 1)) + " ingots", "Leach, CCD, cells: " + f1(paid(level, 2)) + " ingots", ladder(level),
				"  (since the line settled, bank · scavengers · cells)",
				"World rule copper_shortcut: " + (dev.wildcatter.tailings.OptInRules.on(level.getServer(), dev.wildcatter.tailings.OptInRules.COPPER_SHORTCUT) ? "on, filters pay 85 %" : "off")};
	}

	private String[] scavCard(ServerLevel level) {
		return new String[] {"^SCAVENGER BANK · THE BANK'S TAILS", rd(bank, "Flotation bank", "8 x 45 kW"), rd(scav, "Scavenger bank", "4 x 45 kW"),
				rd(scavBlower, "Air blower", "75 kW"), "Its concentrate: to the regrind mill", "Its tails: to the leach tank",
				"Copper paid by this step: " + f1(paid(level, 1)) + " ingots"};
	}

	private String[] regrindCard(ServerLevel level) {
		int nug = count(level, hopper2, item(CU.nuggetId())) + count(level, hopper2.west(), item(CU.nuggetId()));
		return new String[] {"^REGRIND AND CLEAN", rd(regrind, "Regrind mill", "150 kW"), rd(cleaner, "Cleaner bank", "4 x 11 kW"), rd(thick2, "Thickener", "5.5 kW"),
				rd(filter2, "Pressure filter", "15 kW"), "  copper: " + nug + " nuggets, " + f1(paid(level, 1)) + " ingots' worth"};
	}

	private String[] leachCard(ServerLevel level) {
		List<String> l = new ArrayList<>(List.of("^LEACH AND WASH", rd(leach, "Leach tank", "30 kW"),
				"  sulfuric acid by pipe from its tank (" + f0(((StorageTankBlockEntity) level.getBlockEntity(acidTank)).amount()) + " L)", rd(ccd, "CCD thickener", "7.5 kW"),
				"  pregnant liquor: to the cells", "  washed solids: on to the tailings line"));
		if (!note.isEmpty()) {
			l.add(note);
		}
		return l.toArray(String[]::new);
	}

	private String[] ewCard(ServerLevel level) {
		int in = ew.countIn(new int[] {1, 2}, new ItemStack(Items.COPPER_INGOT)), nug = ew.countIn(new int[] {1, 2}, new ItemStack(item(CU.nuggetId())));
		return new String[] {"^ELECTROWINNING BANK", rd(ew, "Electrowinning bank", "400 kW"), "  pregnant liquor from the CCD thickener and the heap",
				"Copper in its output: " + in + " ingots, " + nug + " nuggets", "  (nine nuggets to an ingot)"};
	}

	private String[] heapCard(ServerLevel level) {
		return new String[] {"^HEAP LEACH PAD · WASTE ROCK", "Ore bin (waste rock): " + bin.binCount() + " blocks of andesite", rd(drive, "Belt drive", "7.5 kW"),
				rd(heap, "Heap leach pad", "15 kW"), "  holds " + f1(heap.hold().total() / 1000) + " t of rock", "  acid by pipe from its tank; liquor to the cells"};
	}

	private int fedLead;

	private String[] bypCard(ServerLevel level) {
		Item lead = item("wildcatter:lead_ingot"), silver = item("wildcatter:silver_ingot");
		int waiting = byp.getItem(0).is(lead) ? byp.getItem(0).getCount() : 0;
		int back = byp.countIn(new int[] {1, 2}, new ItemStack(lead)), ag = byp.countIn(new int[] {1, 2}, new ItemStack(silver));
		double carried = byp.byproducts().getOrDefault("wildcatter:silver_ingot", 0.0);
		return new String[] {"^BY-PRODUCT PLANT · SILVER FROM LEAD", rd(byp, "By-product plant", "250 kW"), "Lead ingots handed in: " + fedLead + " (" + waiting + " waiting)",
				"  this one: " + Math.min(20, byp.working(0) / 20) + " of 20 s", "Back out: " + back + " lead ingots, " + ag + " silver ingots",
				"  silver carried over: " + f1(carried) + " kg"};
	}

	private String[] setsCard(ServerLevel level) {
		List<String> l = new ArrayList<>(List.of("^THE SETS: LOAD OF RATING"));
		for (int i = 0; i < panels.size() - 1 && i < setNames.size(); i++) {
			String s = ((GensetControlPanelBlockEntity) level.getBlockEntity(panels.get(i))).inspect().getString();
			java.util.regex.Matcher m = java.util.regex.Pattern.compile("\\((\\d+) kW.*?(\\d+) kW \\((\\d+)%\\)").matcher(s);
			l.add(setNames.get(i) + ": " + (m.find() ? m.group(2) + " of " + m.group(1) + " kW" : "?"));
		}
		return l.toArray(String[]::new);
	}

	private volatile int running;

	private String[] tripCard(ServerLevel level) {
		GensetControlPanelBlockEntity p = rigSet(level);
		String state;
		if (p.isTripped()) {
			String s = p.inspect().getString();
			int a = s.indexOf("TRIPPED"), b = s.indexOf(':', a);
			state = "!" + (a >= 0 ? s.substring(a, b > a ? b : s.length()) : "TRIPPED");
		} else if (rigLive(level)) {
			state = String.format(Locale.ROOT, "+Gas set (1,000 kW): %.1f Hz, %.0f kW", p.measure(Measurable.Quantity.HERTZ), p.measure(Measurable.Quantity.WATTS) / 1000);
		} else {
			state = "Gas set: " + p.run().name().toLowerCase(Locale.ROOT);
		}
		List<String> l = new ArrayList<>(List.of("^A SPARE SET · STARTING A BANK", state, rigMin < 90 ? String.format(Locale.ROOT, "Lowest this start: %.1f Hz", rigMin) : "Lowest this start: -",
				"Running first: " + running + " air blower" + (running == 1 ? "" : "s") + " (" + running + " x 75 kW)", rd(rbank, "Flotation bank", "8 x 45 kW"),
				"A set trips below 57 Hz held for 1 s"));
		if (!note.isEmpty()) {
			l.add(note);
		}
		return l.toArray(String[]::new);
	}

	// ---- the films ----------------------------------------------------------------------------------------------------------------

	/** The scavenger bank close, froth on its four cells, its blower; the bank's tails coming in. */
	private void scavengerFilm() {
		if (!wanted("rtm07_scavenger")) {
			return;
		}
		showCard(true);
		Cam a = scam(-7, 9, 17, 2.5, 3, 23.5);
		Cam b = scam(-6, 8, 31, 2.5, 3, 27);
		film("rtm07_scavenger", 1000, move(a, b, 1000), (level, t) -> card(scavCard(level)));
		showCard(false);
	}

	/** The scavengers' concentrate east to the regrind mill, the cleaner bank, their thickener and filter. */
	private void regrindFilm() {
		if (!wanted("rtm07_regrind")) {
			return;
		}
		showCard(true);
		Cam a = scam(4, 11, 26, 13.5, 1.5, 34);
		Cam b = scam(5, 11, 50, 14, 1.5, 50);
		film("rtm07_regrind", 1100, move(a, b, 1100), (level, t) -> card(regrindCard(level)));
		showCard(false);
	}

	/** The leach tank and its acid tank, then the CCD thickener. */
	private void leachFilm() {
		if (!wanted("rtm07_leach")) {
			return;
		}
		showCard(true);
		Cam a = scam(11, 12, 25, 1, 3, 34);
		Cam b = scam(14, 13, 45, 2, 1, 42);
		film("rtm07_leach", 1100, move(a, b, 1100), (level, t) -> card(leachCard(level)));
		showCard(false);
	}

	/** The cells close; the copper in their output. */
	private void ewFilm() {
		if (!wanted("rtm07_ew")) {
			return;
		}
		showCard(true);
		Cam a = scam(15, 7, 63, 6, 1.5, 54);
		Cam b = scam(3, 6, 65, 6, 1.5, 54);
		film("rtm07_ew", 1000, move(a, b, 1000), (level, t) -> card(ewCard(level)));
		showCard(false);
	}

	/** The ore bin of andesite, the belt over the heap pad, the pad. */
	private void heapFilm() {
		if (!wanted("rtm07_heap")) {
			return;
		}
		showCard(true);
		Cam a = scam(-31, 13, 31, -17, 2, 44);
		Cam b = scam(-29, 11, 57, -15, 2, 44);
		film("rtm07_heap", 1100, move(a, b, 1100), (level, t) -> card(heapCard(level)));
		showCard(false);
	}

	/** Lead ingots handed in (a stack in its feed slot, as by hand); each 20 s one comes back, with silver. */
	private void byproductFilm() {
		if (!wanted("rtm07_byproduct")) {
			return;
		}
		server.runOnServer(srv -> {
			byp.setItem(0, new ItemStack(item("wildcatter:lead_ingot"), 6));
			fedLead = 6;
			hand(byp);
		});
		ticks(100);
		showCard(true);
		Cam a = scam(-29, 8, 54, -20, 2.5, 61.5);
		Cam b = scam(-29, 7, 69, -20, 2.5, 62);
		film("rtm07_byproduct", 1300, move(a, b, 1300), (level, t) -> card(bypCard(level)));
		showCard(false);
	}

	/** The whole chain, from the bank down to the cells and across to the heap. */
	private void lineFilm() {
		if (!wanted("rtm07_line")) {
			return;
		}
		showCard(true);
		Cam a = scam(-16, 22, 4, 4, 2, 24);
		Cam b = scam(-18, 24, 40, 2, 2, 40);
		Cam e = scam(26, 26, 72, 4, 2, 46);
		film("rtm07_line", 1300, via(a, b, e, 1300), (level, t) -> card(lineCard(level)));
		showCard(false);
	}

	/** A length of the acid line broken out: NO ACID; put back: leaching again. */
	private void noAcidFilm() {
		if (!wanted("rtm07_no_acid")) {
			return;
		}
		showCard(true);
		// from the south, over the CCD thickener, down into the gap between the leach tank and its acid tank, where the pipe runs
		Cam a = scam(4.5, 10, 43, 4.5, 0.5, 33.5);
		Cam b = scam(5.5, 8.5, 40.5, 4.5, 0.5, 33.5);
		int[] since = {-1, -1, -1};
		film("rtm07_no_acid", 1100, move(a, b, 1100), (level, t) -> {
			int s = t - PAD;
			if (s == 100) {
				level.setBlock(acidCut, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
				level.updateNeighborsAt(acidCut, Blocks.AIR, null);
				note = "!A length of the acid line broken out";
				event("acid line broken at " + acidCut.toShortString());
			}
			if (s > 100 && since[0] < 0 && leach.status().startsWith("NO ACID")) {
				since[0] = s;
				event("leach: " + leach.status());
			}
			if (since[0] >= 0 && s == since[0] + 360) {
				pipe(level, acidCut);
				note = "+The length put back";
				since[1] = s;
				event("acid line mended");
			}
			if (since[1] >= 0 && since[2] < 0 && leach.status().startsWith("leaching")) {
				since[2] = s;
				event("leach: " + leach.status());
			}
			card(leachCard(level));
		});
		note = "";
		showCard(false);
	}

	private void setsFilm() {
		if (!wanted("rtm07_sets")) {
			return;
		}
		showCard(true);
		Cam a = scam(YARD_X - 22, 18, -8, YARD_X - 2, 2, 22);
		Cam b = scam(YARD_X - 24, 20, 86, YARD_X - 2, 2, 52);
		film("rtm07_sets", 800, move(a, b, 800), (level, t) -> card(setsCard(level)));
		showCard(false);
	}

	/**
	 * The spare set: with {@code nOk} blowers running, the bank starts, the frequency dips and recovers; the bank stopped, more blowers
	 * started, and the bank again: the frequency falls below 57 Hz for a second and the set trips.
	 */
	private void tripFilm() {
		if (!wanted("rtm07_set_trip")) {
			return;
		}
		if (nTrip < 0) {
			log("the spare set never tripped in the trials: filming the dip only");
		}
		int first = Math.max(0, nOk), second = nTrip < 0 ? 4 : nTrip;
		require(rigReset(), "the spare set should run");
		for (int i = 0; i < first; i++) {
			int k = i;
			server.runOnServer(srv -> hand(rblowers.get(k)));
			ticks(60);
		}
		running = first;
		rigMin = 99;
		ticks(100);
		showCard(true);
		Cam a = rcam(15, 12, -7, -6, 2, 13);
		Cam b = rcam(17, 10, 5, -8, 2, 13);
		int[] at = {-1};
		film("rtm07_set_trip", 1400, move(a, b, 1400), (level, t) -> {
			int s = t - PAD;
			double hz = rigHz(level);
			if (!Double.isNaN(hz) && at[0] >= 0) {
				rigMin = Math.min(rigMin, hz);
			}
			if (s == 60) {
				rigMin = 99;
				at[0] = s;
				hand(rbank);
				note = "+Bank started";
				event("bank started with " + running + " blowers running");
			}
			if (s == 460) {
				event(String.format(Locale.ROOT, "first start: lowest %.2f Hz, set %s", rigMin, rigLive(level) ? "running" : "DOWN"));
				off(rbank);
				note = "Bank stopped";
			}
			for (int i = first; i < second; i++) {
				if (s == 520 + 60 * (i - first)) {
					hand(rblowers.get(i));
					running = i + 1;
					note = "+Another blower started";
					event("blower " + (i + 1) + " started");
				}
			}
			int again = 520 + 60 * (second - first) + 140;
			if (s == again) {
				rigMin = 99;
				hand(rbank);
				note = "+Bank started again";
				event("bank started again with " + running + " blowers running");
			}
			if (s > again && rigSet(level).isTripped() && !note.startsWith("!")) {
				note = "!The set tripped";
				event("set: " + rigSet(level).inspect().getString() + String.format(Locale.ROOT, "; lowest %.2f Hz", rigMin));
			}
			card(tripCard(level));
		});
		note = "";
		showCard(false);
		rigReset();
	}
}
