package dev.wildcatter.tailings.fabric.gametest;

import dev.wildcatter.block.BigPartBlock;
import dev.wildcatter.block.GensetControlPanelBlockEntity;
import dev.wildcatter.block.GensetPartBlockEntity;
import dev.wildcatter.block.GensetSkid;
import dev.wildcatter.control.ControlDevice;
import dev.wildcatter.control.Measurable;
import dev.wildcatter.energy.ConductorSize;
import dev.wildcatter.fluid.Product;
import dev.wildcatter.fluid.SlurryAssay;
import dev.wildcatter.multiblock.BigParts;
import dev.wildcatter.tailings.Tailings;
import dev.wildcatter.tailings.TailingsBlocks;
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
import net.minecraft.core.FrontAndTop;
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
import net.minecraft.world.level.block.ButtonBlock;
import net.minecraft.world.level.block.HopperBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.Vec3;

/**
 * Rock to Metal, episode 5, "Making Rock Float" (TAILINGS_ONLY=rtm05): froth flotation, every motor on real power from Wildcatter
 * generator sets at the industrial voltage (the pit row's large blower at the medium voltage, through a unit substation), every cable,
 * busway and pipe run laid joined.
 * <ul>
 * <li>the 50 t/h plant: copper ore fines into the conditioning tank, its reagents made in the world on top of it (quicklime burned from
 * calcite in a furnace; xanthate, frother and copper sulfate from crafters), the bank of eight cells with its blower, the concentrate
 * thickener and the pressure filter, the copper ingots into a hopper and a chest;</li>
 * <li>its faults: the blower's selector to OFF (NO AIR), and the lime run out (NO REAGENT) and made again;</li>
 * <li>the pit row: the large conditioning tank, eight 160 m3 tank cells, two column cleaners, the large blower on its air line, the
 * concentrate thickener and the filter; the froth on cell 1; each cell's AIR point as the blower stops and starts.</li>
 * </ul>
 * Filmed in real time at 2880 x 1620 with a readout card, read from the blocks each tick. RTM05_SHOTS=a,b films only those.
 * <p>
 * Staged, and said so in the episode: the feed is topped up as if from the mill's cyclones (the cluster stand-in is not run); the bank's,
 * cell 8's and the columns' tails are drawn off as if to the tailings cell; the crafters' ingredients and the pit tank's reagent chest are
 * kept stocked; the lime in the small tank is drawn down off camera before the fault film.
 */
final class Rtm05Scenes {
	private static final int W = 1440, H = 810;
	private static final int PAD = 60;
	private static final String SEED = System.getenv().getOrDefault("TAILINGS_TRAILER_SEED", "tailings");
	private static final String LV = "wildcatter:cable_industrial", BUS = "wildcatter:busway_industrial", MV = "wildcatter:cable_mv";
	/** The small plant's origin, east of the pit row's. */
	private static final int SMALL_DX = 110;

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

	// the small plant
	private PlantBlockEntity src, cond, bank, blower, thick, filter;
	private BlockPos furnace, hopper, filterChest, filterHopper;
	private final BlockPos[] crafters = new BlockPos[3], buttons = new BlockPos[3];
	private boolean limeStopped;
	private volatile boolean filmingReagents;
	// the pit row
	private PlantBlockEntity rsrc, rtank, rblower, rthick, rfilter;
	private final PlantBlockEntity[] cells = new PlantBlockEntity[8], cols = new PlantBlockEntity[2];
	private BlockPos rowChest, rowFilterChest, rowFilterHopper;
	private int rowIngotsTaken, smallIngotsTaken;
	private String note = "";

	/** Fines of the game's copper ore: 400 kg of copper in a 2.7 t block, at 35 % solids. */
	private static final double RICH = Rock.COPPER.contained(Mineral.COPPER).doubleValue() * Mineral.COPPER.kgPerUnit() / (Reagent.BLOCK_TONNES.doubleValue() * 1000 / PlantRates.OVERFLOW_SOLIDS);
	/** The pit row's leaner feed: 0.4 % copper in the slurry, about 1.1 % of the solids. */
	private static final double LEAN = 0.004;

	Rtm05Scenes(ClientGameTestContext context) {
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
			cardWidth("Tank cell 1 (160 kW): floating, 35 % of it");
			try {
				stage();
				power();
				warm();
				if (wanted("rtm05_bank") || wanted("rtm05_bank_close")) {
					bankFilms();
				}
				reagentsFilm();
				filterFilm();
				faultsFilm();
				rowFilm();
				frothFilm();
				columnsFilm();
				airPlcFilm();
				setsFilm();
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
		System.out.println("[rtm05] " + line);
	}

	private static void require(boolean ok, String what) {
		if (!ok) {
			throw new AssertionError("[rtm05] " + what);
		}
	}

	private static boolean wanted(String id) {
		String only = System.getenv("RTM05_SHOTS");
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

	private static void hand(PlantBlockEntity... parts) {
		for (PlantBlockEntity p : parts) {
			setHoa(p, "HAND");
		}
	}

	private static void put(ServerLevel level, BlockPos at, Item item, int count, int slot) {
		if (level.getBlockEntity(at) instanceof Container c) {
			c.setItem(slot, new ItemStack(item, count));
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

	/**
	 * The small plant (flotation-bank.md's layout, the parts facing north): the feed's stand-in, the conditioning tank with its reagent rig
	 * on top, the bank with its blower against its west side, the thickener off the bank's concentrate, the filter raised a cell over its
	 * hopper and chest. The pit row west of it, as the p7_flotation scene lays it but the column cleaners side by side east of the header
	 * (so their motor boxes are free for their cables), the large blower east of cell 1.
	 */
	private void stage() {
		server.runOnServer(srv -> {
			ServerLevel level = srv.overworld();
			BlockPos s = Trailer.site(level, -1600, 2400, 260, 160);
			int g = s.getY();
			o = new BlockPos(s.getX() + 60, g, s.getZ() + 50);
			q = o.offset(SMALL_DX, 0, 10);
			Trailer.pad(level, o.getX() - 50, o.getZ() - 30, o.getX() + SMALL_DX + 50, o.getZ() + 100, g, 16, 40);
			Trailer.forceChunks(level, o.getX() - 70, o.getZ() - 50, o.getX() + SMALL_DX + 70, o.getZ() + 120);
			// ---- the small plant
			src = plant(level, Machine.CYCLONE, sq(0, 0, 0));
			boxCells.remove(box(src));
			Trailer.pipe(level, sq(0, 0, 2), sq(1, 0, 2));
			cond = plant(level, Machine.CONDITIONER, sq(0, 0, 3));
			Trailer.pipe(level, sq(0, 0, 6));
			bank = plant(level, Machine.FLOTATION_BANK, sq(0, 0, 7));
			blower = plant(level, Machine.BLOWER, sq(-2, 0, 9));
			require(bank.touching(Machine.BLOWER) == blower, "the blower touches the bank");
			Trailer.pipe(level, sq(6, 0, 20));
			thick = plant(level, Machine.THICKENER, sq(6, 0, 21));
			Trailer.pipe(level, sq(6, 0, 29), sq(6, 1, 29));
			filter = plant(level, Machine.FILTER, sq(6, 1, 30));
			filterHopper = sq(6, 0, 34);
			level.setBlock(filterHopper, Blocks.HOPPER.defaultBlockState().setValue(HopperBlock.FACING, Direction.WEST), Block.UPDATE_ALL);
			filterChest = sq(5, 0, 34);
			level.setBlock(filterChest, Blocks.CHEST.defaultBlockState(), Block.UPDATE_ALL);
			// the reagent rig on the tank's top (y 4): a hopper in the middle into the tank; a furnace over it burning calcite to lime; three
			// crafters round it, each facing it (xanthate north of it, frother west, copper sulfate east), a button on each one's far side
			hopper = sq(1, 4, 4);
			level.setBlock(hopper, Blocks.HOPPER.defaultBlockState(), Block.UPDATE_ALL);
			furnace = sq(1, 5, 4);
			level.setBlock(furnace, Blocks.FURNACE.defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.WEST), Block.UPDATE_ALL);
			Direction[] side = {Direction.NORTH, Direction.WEST, Direction.EAST};
			for (int i = 0; i < 3; i++) {
				crafters[i] = hopper.relative(side[i]);
				FrontAndTop front = switch (side[i].getOpposite()) {
					case SOUTH -> FrontAndTop.SOUTH_UP;
					case EAST -> FrontAndTop.EAST_UP;
					default -> FrontAndTop.WEST_UP;
				};
				level.setBlock(crafters[i], Blocks.CRAFTER.defaultBlockState().setValue(BlockStateProperties.ORIENTATION, front), Block.UPDATE_ALL);
				buttons[i] = crafters[i].relative(side[i]);
				level.setBlock(buttons[i], Blocks.STONE_BUTTON.defaultBlockState().setValue(BlockStateProperties.ATTACH_FACE, AttachFace.WALL)
						.setValue(BlockStateProperties.HORIZONTAL_FACING, side[i]), Block.UPDATE_ALL);
			}
			stockRig(level);
			// ---- the pit row
			rsrc = plant(level, Machine.CYCLONE, at(0, 0, 0));
			boxCells.remove(box(rsrc));
			Trailer.pipe(level, at(0, 0, 2), at(1, 0, 2));
			rtank = plant(level, Machine.CONDITIONING_TANK_LARGE, at(0, 0, 3));
			Trailer.pipe(level, at(3, 0, 11));
			Block tubing = wc("wildcatter:air_tubing");
			for (int k = 0; k < 8; k++) {
				int z = 12 + 8 * k;
				cells[k] = plant(level, Machine.TANK_CELL, at(0, 0, z));
				if (k < 7) {
					Trailer.pipe(level, at(3, 0, z + 7));
				}
				for (int dz = 0; dz < 8; dz++) {
					if (z + dz <= 74) {
						Trailer.pipe(level, at(7, 0, z + dz));   // the concentrate header along their east launders
						tube(level, tubing, at(3, 7, z + dz));   // the air line over their tops
					}
				}
			}
			rblower = plant(level, Machine.FLOTATION_BLOWER_LARGE, at(9, 0, 12));
			for (int x = 4; x <= 10; x++) {
				tube(level, tubing, at(x, 7, 13));
			}
			for (int dy = 4; dy <= 6; dy++) {
				tube(level, tubing, at(10, dy, 13));
			}
			// the column cleaners side by side, fed off the header along z 39; their cleaned concentrate along z 44 to the thickener
			cols[0] = plant(level, Machine.COLUMN_CELL, at(11, 0, 40));
			cols[1] = plant(level, Machine.COLUMN_CELL, at(17, 0, 40));
			for (int x = 8; x <= 20; x++) {
				Trailer.pipe(level, at(x, 0, 39));
			}
			for (int x = 11; x <= 20; x++) {
				Trailer.pipe(level, at(x, 0, 44));
			}
			Trailer.pipe(level, at(16, 0, 45));
			for (int x = 4; x <= 10; x++) {
				tube(level, tubing, at(x, 7, 41));
			}
			for (int y = 8; y <= 14; y++) {
				tube(level, tubing, at(10, y, 41));
			}
			for (int x = 11; x <= 17; x++) {
				tube(level, tubing, at(x, 14, 41));
			}
			rthick = plant(level, Machine.THICKENER, at(13, 0, 46));
			Trailer.pipe(level, at(13, 0, 54), at(13, 1, 54));
			rfilter = plant(level, Machine.FILTER, at(13, 1, 55));
			rowFilterHopper = at(13, 0, 59);
			level.setBlock(rowFilterHopper, Blocks.HOPPER.defaultBlockState().setValue(HopperBlock.FACING, Direction.WEST), Block.UPDATE_ALL);
			rowFilterChest = at(12, 0, 59);
			level.setBlock(rowFilterChest, Blocks.CHEST.defaultBlockState(), Block.UPDATE_ALL);
			// the large tank's reagents: a hopper on its top and the reagent store's chest on that
			level.setBlock(at(3, 9, 6), Blocks.HOPPER.defaultBlockState(), Block.UPDATE_ALL);
			rowChest = at(3, 10, 6);
			level.setBlock(rowChest, Blocks.CHEST.defaultBlockState(), Block.UPDATE_ALL);
			for (PlantBlockEntity p : List.of(cond, bank, blower, thick, filter, rtank, rblower, rthick, rfilter, cols[0], cols[1])) {
				setHoa(p, "OFF");
			}
			for (PlantBlockEntity p : cells) {
				setHoa(p, "OFF");
			}
			log(String.format(Locale.ROOT, "rich feed %.4f kg of copper a kg of slurry; lean %.4f", RICH, LEAN));
			log("boxes: tank " + box(rtank).toShortString() + ", cell 1 " + box(cells[0]).toShortString() + ", blower " + box(rblower).toShortString() + ", columns "
					+ box(cols[0]).toShortString() + " " + box(cols[1]).toShortString() + ", thickener " + box(rthick).toShortString() + ", filter " + box(rfilter).toShortString());
		});
		server.runCommand("kill @e[type=item]");
		server.runCommand("kill @e[type=!player]");
	}

	/** A length of air tubing, its neighbours told so the run draws joined (as lay() does for conductors). */
	private static void tube(ServerLevel level, Block tubing, BlockPos at) {
		level.setBlock(at, tubing.defaultBlockState(), Block.UPDATE_ALL);
		level.updateNeighborsAt(at, tubing, null);
	}

	/** The rig's stock: calcite and coal in the furnace (unless the lime is stopped), each crafter's ingredients. */
	private void stockRig(ServerLevel level) {
		put(level, furnace, Items.COAL, 16, 1);
		Item lime = item(Tailings.id(Reagent.LIME.id()).toString()), sulfur = item("wildcatter:sulfur");
		put(level, crafters[0], lime, 16, 0);
		put(level, crafters[0], sulfur, 16, 1);
		put(level, crafters[0], Items.SUGAR, 16, 2);
		put(level, crafters[1], Items.SUGAR, 16, 0);
		put(level, crafters[1], Items.SUGAR, 16, 1);
		put(level, crafters[2], Items.COPPER_INGOT, 16, 0);
		put(level, crafters[2], sulfur, 16, 1);
	}

	private static void press(ServerLevel level, BlockPos button) {
		BlockState state = level.getBlockState(button);
		if (state.getBlock() instanceof ButtonBlock b && !state.getValue(ButtonBlock.POWERED)) {
			b.press(state, level, button, null);
		}
	}

	/**
	 * The yards: the small plant's bank on a gas set by busway, its other motors on a diesel set by cable; the pit row's tank and cells
	 * on three gas sets by busway (three cells, about 700 A, to a set's 800 A busway), the large blower on a fourth stepped up to the
	 * medium voltage, the columns, thickener and filter on a diesel set by cable.
	 */
	private void power() {
		server.runOnServer(srv -> {
			ServerLevel level = srv.overworld();
			BlockPos gb = set(level, sq(-28, 0, 6), true);
			wire(level, BUS, null, gb.east(), box(bank));
			BlockPos ds = set(level, sq(-28, 0, 26), false);
			wire(level, LV, ConductorSize.KCMIL_500, ds.east(), box(blower), box(cond), box(thick), box(filter));
			BlockPos g1 = set(level, at(-30, 0, 4), true);
			wire(level, BUS, null, g1.east(), box(rtank), box(cells[0]), box(cells[1]));
			BlockPos g2 = set(level, at(-30, 0, 30), true);
			wire(level, BUS, null, g2.east(), box(cells[2]), box(cells[3]), box(cells[4]));
			BlockPos g3 = set(level, at(-30, 0, 56), true);
			wire(level, BUS, null, g3.east(), box(cells[5]), box(cells[6]), box(cells[7]));
			BlockPos g4 = set(level, at(36, 0, 6), true);
			wire(level, MV, null, stepUp(level, g4), box(rblower));
			BlockPos d2 = set(level, at(36, 0, 66), false);
			wire(level, LV, ConductorSize.KCMIL_500, d2.east(), box(rfilter), box(rthick), box(cols[1]), box(cols[0]));
		});
		context.waitTicks(40);
		server.runOnServer(srv -> {
			for (BlockPos panel : panels) {
				((GensetControlPanelBlockEntity) srv.overworld().getBlockEntity(panel)).setWantRun(true);
			}
			keep(srv);
		});
		ticks(400);
		server.runOnServer(srv -> hand(filter, thick, rfilter, rthick, cols[0], cols[1]));
		ticks(60);
		server.runOnServer(srv -> hand(blower, cond, rblower));
		ticks(100);
		server.runOnServer(srv -> hand(bank, rtank));
		ticks(100);
		for (int k = 7; k >= 0; k--) {
			PlantBlockEntity c = cells[k];
			server.runOnServer(srv -> hand(c));
			ticks(60);
		}
		server.runOnServer(srv -> {
			for (BlockPos panel : panels) {
				log("set " + panel.toShortString() + ": " + ((GensetControlPanelBlockEntity) srv.overworld().getBlockEntity(panel)).inspect().getString());
			}
			keeps = 9;
			keep(srv);
		});
	}

	/** Until both filters pay. */
	private void warm() {
		boolean ok = waitFor(9000, level -> ingots(level, false) > 0 && ingots(level, true) > 0);
		log("both filters paying: " + ok);
		ticks(600);
	}

	// ---- keeping the stand-ins up -------------------------------------------------------------------------------------------------

	private static SlurryAssay copper(double kgPerKg) {
		return SlurryAssay.of(Map.of(Mineral.COPPER.mineralItemId(), kgPerKg));
	}

	/** Every second: fuel, both feeds, the tails drawn off, the rig and the pit tank's reagents, the chests emptied when near full. */
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
			src.out().offer(Product.FINES_SLURRY, 400, copper(RICH));   // as if from the mill's cyclones
		}
		bank.out().give(Product.TAILS_SLURRY, Double.MAX_VALUE);   // on to the tailings cell
		double short_ = 3000 - rsrc.out().held(Product.FINES_SLURRY);
		if (short_ > 0) {
			rsrc.out().offer(Product.FINES_SLURRY, short_, copper(LEAN));
		}
		cells[7].out().give(Product.TAILS_SLURRY, Double.MAX_VALUE);
		for (PlantBlockEntity c : cols) {
			c.out().give(Product.TAILS_SLURRY, Double.MAX_VALUE);   // their water, back to cell 1 in a real plant
		}
		if (count(level, furnace, Items.COAL) < 8 || count(level, crafters[0], Items.SUGAR) < 8 || count(level, crafters[1], Items.SUGAR) < 8
				|| count(level, crafters[2], Items.COPPER_INGOT) < 8) {
			stockRig(level);
		}
		// the small tank's rig, run as its level control would: the furnace burns calcite while the tank is short of lime (and always on
		// the reagents film), each crafter crafts when the tank is short of its reagent, so no reagent crowds the hopper out
		boolean limeLow = cond.reagents().kilograms(Reagent.LIME).doubleValue() < 6 || filmingReagents;
		Container fur = (Container) level.getBlockEntity(furnace);
		if (limeStopped || !limeLow) {
			fur.setItem(0, ItemStack.EMPTY);
		} else if (fur.getItem(0).getCount() < 8) {
			fur.setItem(0, new ItemStack(Items.CALCITE, 16));
		}
		if (!filmingReagents) {
			if (cond.reagents().kilograms(Reagent.XANTHATE).doubleValue() < 3) {
				press(level, buttons[0]);
			}
			if (cond.reagents().kilograms(Reagent.MIBC).doubleValue() < 3) {
				press(level, buttons[1]);
			}
		}
		// the pit tank's reagent store: refilled only once the chest is empty, in proportion to the doses (lime 500, xanthate 50,
		// frother 20 g a tonne), so the hopper passes all three
		if (level.getBlockEntity(rowChest) instanceof Container c && c.isEmpty()) {
			c.setItem(0, new ItemStack(item(Tailings.id(Reagent.LIME.id()).toString()), 20));
			c.setItem(1, new ItemStack(item(Tailings.id(Reagent.XANTHATE.id()).toString()), 2));
			c.setItem(2, new ItemStack(item(Tailings.id(Reagent.MIBC.id()).toString()), 1));
		}
		Item ingot = item(Mineral.COPPER.nuggetId());
		if (count(level, filterChest, ingot) > 1500) {
			smallIngotsTaken += count(level, filterChest, ingot);   // nuggets, as if carried off
			((Container) level.getBlockEntity(filterChest)).clearContent();
		}
		if (count(level, rowFilterChest, ingot) > 1500) {
			rowIngotsTaken += count(level, rowFilterChest, ingot);
			((Container) level.getBlockEntity(rowFilterChest)).clearContent();
		}
		if (++keeps % 10 == 0) {
			log(String.format(Locale.ROOT, "tick %.1f ms", srv.getAverageTickTimeNanos() / 1e6));
			for (PlantBlockEntity p : new PlantBlockEntity[] {cond, bank, blower, thick, filter, rtank, rblower, cols[0], cols[1], rthick, rfilter}) {
				log("  " + p.inspect().getString());
			}
			for (PlantBlockEntity c : cells) {
				log("  " + c.inspect().getString());
			}
			log(String.format(Locale.ROOT, "  ingots: small %.1f, row %.1f; lime %s kg, xanthate %s kg, MIBC %s kg in the small tank", ingots(level, false), ingots(level, true),
					cond.reagents().kilograms(Reagent.LIME).toPlainString(), cond.reagents().kilograms(Reagent.XANTHATE).toPlainString(),
					cond.reagents().kilograms(Reagent.MIBC).toPlainString()));
		}
	}

	/**
	 * Copper a filter has paid, in ingots' worth: its ingots and nuggets (nine to an ingot) in it, its hopper and its chest, and those
	 * emptied from the chest. The hopper under it takes the nuggets before nine of them pack into an ingot.
	 */
	private double ingots(ServerLevel level, boolean row) {
		PlantBlockEntity f = row ? rfilter : filter;
		int in = 0, nug = 0;
		Item nugget = item(Mineral.COPPER.nuggetId());
		in += f.countIn(new int[] {1, 2}, new ItemStack(Items.COPPER_INGOT));
		nug += f.countIn(new int[] {1, 2}, new ItemStack(nugget));
		for (BlockPos at : new BlockPos[] {row ? rowFilterHopper : filterHopper, row ? rowFilterChest : filterChest}) {
			in += count(level, at, Items.COPPER_INGOT);
			nug += count(level, at, nugget);
		}
		return in + nug / 9.0 + (row ? rowIngotsTaken : smallIngotsTaken) / 9.0;
	}

	private int nuggets(ServerLevel level, boolean row) {
		return count(level, row ? rowFilterChest : filterChest, item(Mineral.COPPER.nuggetId())) + count(level, row ? rowFilterHopper : filterHopper, item(Mineral.COPPER.nuggetId()));
	}

	// ---- readouts -----------------------------------------------------------------------------------------------------------------

	/** "+name (kW): A" while it works, "!name: status" when it reads a fault, or "name: status". */
	private static String rd(PlantBlockEntity p, String name, String kw) {
		String st = p.status();
		boolean fault = st.startsWith("NO ") || st.startsWith("NOT BUILT") || st.startsWith("FULL") || st.startsWith("waiting");
		if (fault) {
			return (st.startsWith("waiting") ? "" : "!") + name + ": " + brief(st);
		}
		return p.running() ? "+" + name + " (" + kw + "): " + f0(amps(p)) + " A" : name + ": " + brief(st);
	}

	private static String brief(String st) {
		if (st.startsWith("NO AIR")) {
			return "NO AIR";
		}
		if (st.startsWith("NO REAGENT")) {
			return st.replace("Quicklime", "quicklime");
		}
		return st.length() > 34 ? st.substring(0, 34) : st;
	}

	private static String kg(PlantBlockEntity tank, Reagent r) {
		return f1(tank.reagents().kilograms(r).doubleValue());
	}

	private String[] bankCard(ServerLevel level) {
		return new String[] {"^FLOTATION BANK · eight 20 m³ cells", "Feed: copper ore fines, 50 t/h at 35 %", rd(cond, "Conditioning tank", "15 kW"), rd(bank, "Bank", "8 x 45 kW"),
				rd(blower, "Air blower", "75 kW"), rd(thick, "Thickener", "5.5 kW"), rd(filter, "Pressure filter", "15 kW"), "Copper paid: " + f1(ingots(level, false)) + " ingots' worth"};
	}

	private String[] reagentCard() {
		return new String[] {"^REAGENTS, MADE IN THE WORLD", "Quicklime: calcite burned in a furnace", "Xanthate: lime + sulfur + sugar, 4 a craft", "Frother: 2 sugar, 4 a craft (game)",
				"Copper sulfate: copper + sulfur, 4 a craft", "Doses a tonne: lime 500 g, xanthate 50 g,", "  frother 20 g (copper sulfate 300 g: zinc ores)",
				"In the tank: lime " + kg(cond, Reagent.LIME) + " · xanthate " + kg(cond, Reagent.XANTHATE) + " · frother " + kg(cond, Reagent.MIBC) + " kg", rd(cond, "Tank", "15 kW")};
	}

	private String[] faultCard() {
		List<String> l = new ArrayList<>(List.of("^NO AIR, NO REAGENT", rd(blower, "Air blower", "75 kW"), "  its selector: " + hoa(blower), rd(bank, "Bank", "8 x 45 kW"),
				rd(cond, "Conditioning tank", "15 kW"), "  lime in the tank: " + kg(cond, Reagent.LIME) + " kg", "  furnace: " + (limeStopped ? "!no calcite" : "+burning calcite")));
		if (!note.isEmpty()) {
			l.add(note);
		}
		return l.toArray(String[]::new);
	}

	private String[] filterCard(ServerLevel level) {
		return new String[] {"^THICKENER AND PRESSURE FILTER", rd(thick, "Thickener", "5.5 kW"), "  thickened to 60 % solids", rd(filter, "Pressure filter", "15 kW"),
				"  cake to the hopper under it", "Copper paid: " + nuggets(level, false) + " nuggets in the chest,", "  " + f1(ingots(level, false)) + " ingots' worth (9 nuggets an ingot)", "A block of ore: 14 ingots of copper;", "  flotation pays 75 %: 10.5"};
	}

	private int floating() {
		int n = 0;
		for (PlantBlockEntity c : cells) {
			n += c.status().startsWith("floating") ? 1 : 0;
		}
		return n;
	}

	private String[] rowCard(ServerLevel level) {
		return new String[] {"^PIT FLOTATION ROW · 1,000 t/h", "Feed: 2,230 m³/h at 35 %, 1.1 % copper", rd(rtank, "Large tank", "90 kW"), "Tank cells: " + floating() + " of 8 floating, 160 kW each",
				rd(rblower, "Large blower", "400 kW"), rd(cols[0], "Column 1", "15 kW"), colTwo(), "Copper paid: " + f1(ingots(level, true)) + " ingots' worth"};
	}

	private static int airPoint(PlantBlockEntity d) {
		List<ControlDevice.Point> ps = d.points();
		for (int i = 0; i < ps.size(); i++) {
			if (ps.get(i).name().startsWith("AIR")) {
				return i;
			}
		}
		return -1;
	}

	private static String cellLine(int k, PlantBlockEntity c) {
		String st = c.status();
		String what = st.startsWith("floating") ? "+floating, " + PitFlotationShare(k) + " %" : st.startsWith("NO AIR") ? "!NO AIR" : brief(st);
		return "Cell " + (k + 1) + ": " + what;
	}

	private static int PitFlotationShare(int k) {
		return dev.wildcatter.tailings.plant.PitFlotation.SHARES_PERCENT[k];
	}

	private String[] frothCard() {
		PlantBlockEntity c = cells[0];
		int a = airPoint(c);
		String st = brief(c.status());
		int cut = st.indexOf(" (");   // the status's bracketed tail ran off the card's edge at "(rou": the card says it below
		return new String[] {"^TANK CELL 1 · ROUGHER · 160 m³", rd(c, "Cell 1", "160 kW"), "  " + (cut > 0 ? st.substring(0, cut) : st), "AIR point: " + (a < 0 ? "-" : f1(c.readMa(a)) + " mA"),
				"Its share: 35 % of the copper in the feed", "The row's eight: 35 15 8 5 4 3 3 2 = 75 %"};
	}

	/** Column 2 with no feed is standing by: at this grade column 1 cleans it all. */
	private String colTwo() {
		return cols[1].status().startsWith("NO FEED") ? "Column 2: standing by" : rd(cols[1], "Column 2", "15 kW");
	}

	private String[] columnsCard(ServerLevel level) {
		return new String[] {"^COLUMN CLEANERS · 4 m x 14 m", rd(cols[0], "Column 1", "15 kW"), "  " + brief(cols[0].status()), colTwo(), "  " + (cols[1].status().startsWith("NO FEED") ? "column 1 takes all of this feed" : brief(cols[1].status())),
				rd(rthick, "Thickener", "5.5 kW"), rd(rfilter, "Pressure filter", "15 kW"), "Copper paid: " + f1(ingots(level, true)) + " ingots' worth"};
	}

	private String[] airCard() {
		List<String> l = new ArrayList<>();
		l.add("^PLC POINTS · each cell's AIR, 4-20 mA");
		l.add(rd(rblower, "Large blower", "400 kW") + " [" + hoa(rblower) + "]");
		for (int k = 0; k < 8; k++) {
			PlantBlockEntity c = cells[k];
			int a = airPoint(c);
			double ma = a < 0 ? Double.NaN : c.readMa(a);
			l.add((ma > 12 ? "+" : "!") + "Cell " + (k + 1) + " AIR: " + f1(ma) + " mA" + (c.status().startsWith("NO AIR") ? " · NO AIR" : c.status().startsWith("floating") ? " · floating" : ""));
		}
		return l.toArray(String[]::new);
	}

	// ---- the films ----------------------------------------------------------------------------------------------------------------

	/** The small plant whole, then the bank and its blower close. */
	private void bankFilms() {
		showCard(true);
		Cam a = scam(-16, 15, -10, 3, 2, 10);
		Cam b = scam(-14, 12, 26, 4, 2, 16);
		Cam e = scam(18, 14, 40, 6, 2, 22);
		film("rtm05_bank", 1100, via(a, b, e, 1100), (level, t) -> card(bankCard(level)));
		Cam c = scam(-9, 7.5, 4, 1, 3.5, 11);
		Cam d = scam(-8, 6.5, 18, 2, 3.5, 13);
		film("rtm05_bank_close", 700, move(c, d, 700), (level, t) -> card(bankCard(level)));
		showCard(false);
	}

	/** The rig on the tank: the furnace burning lime, each crafter's button pressed in turn and its reagent into the hopper. */
	private void reagentsFilm() {
		if (!wanted("rtm05_reagents")) {
			return;
		}
		filmingReagents = true;
		server.runOnServer(this::keep);
		ticks(200);
		showCard(true);
		Cam a = scam(-3.2, 7.2, 0.2, 1.5, 4.8, 4.5);
		Cam b = scam(-2.2, 6.6, 2.4, 1.5, 4.6, 4.5);
		film("rtm05_reagents", 1200, move(a, b, 1200), (level, t) -> {
			int s = t - PAD;
			for (int i = 0; i < 3; i++) {
				if (s >= 0 && s % 300 == 60 + 80 * i) {
					press(level, buttons[i]);
					event("crafter " + i + " button pressed");
				}
			}
			card(reagentCard());
		});
		filmingReagents = false;
		showCard(false);
	}

	private void filterFilm() {
		if (!wanted("rtm05_filter")) {
			return;
		}
		showCard(true);
		Cam a = scam(-4, 5, 26, 6, 1.5, 33);
		Cam b = scam(-3, 4, 40, 6, 1.5, 33);
		film("rtm05_filter", 900, move(a, b, 900), (level, t) -> card(filterCard(level)));
		showCard(false);
	}

	/** The blower's selector to OFF and back (NO AIR); then the lime stopped, drawn down off camera, run out on camera and made again. */
	private void faultsFilm() {
		if (!wanted("rtm05_faults")) {
			return;
		}
		showCard(true);
		Cam a = scam(-11, 7, 2, -1, 2, 11);
		Cam b = scam(-9, 7.5, 12, -1, 2.5, 7);
		film("rtm05_faults", 900, move(a, b, 900), (level, t) -> {
			int s = t - PAD;
			if (s == 160) {
				setHoa(blower, "OFF");
				event("blower selector to OFF");
			}
			if (s > 160 && s < 600 && s % 20 == 0 && !sawNoAir && bank.status().startsWith("NO AIR")) {
				sawNoAir = true;
				event("bank: " + bank.status());
			}
			if (s == 600) {
				setHoa(blower, "HAND");
				event("blower selector to HAND");
			}
			if (s > 600 && s % 40 == 0) {
				event("bank: " + bank.status());
			}
			card(faultCard());
		});
		// the lime stopped and drawn down off camera, then run out on camera
		server.runOnServer(srv -> {
			ServerLevel level = srv.overworld();
			limeStopped = true;
			((Container) level.getBlockEntity(furnace)).setItem(0, ItemStack.EMPTY);
			((Container) level.getBlockEntity(furnace)).setItem(2, ItemStack.EMPTY);
			((Container) level.getBlockEntity(hopper)).clearContent();
			String saved = cond.reagents().save().replaceAll("LIME=[0-9.]+", "LIME=0.30");
			cond.reagents().load(saved.contains("LIME=") ? saved : saved + (saved.isEmpty() ? "" : ",") + "LIME=0.30");
			cond.trimReagents();
			log("lime drawn down to " + cond.reagents().kilograms(Reagent.LIME).toPlainString() + " kg");
		});
		Cam c = scam(-7, 8, 0, 1.5, 4, 4.5);
		Cam d = scam(-8, 7, 6, 1, 3, 5);
		film("rtm05_no_reagent", 1500, move(c, d, 1500), (level, t) -> {
			int s = t - PAD;
			if (noReagentSince < 0 && cond.status().startsWith("NO REAGENT")) {
				noReagentSince = s;
				note = "!Tank: no slurry on to the bank";
				event("conditioning tank: " + cond.status() + "; bank: " + bank.status());
			}
			if (noReagentSince >= 0 && s == noReagentSince + 240 && limeStopped) {
				limeStopped = false;
				put(level, furnace, Items.CALCITE, 16, 0);
				note = "+Calcite back in the furnace";
				event("calcite back in the furnace");
			}
			if (!limeStopped && noReagentSince >= 0 && !resumed && cond.status().startsWith("conditioning")) {
				resumed = true;
				event("conditioning tank: " + cond.status());
			}
			card(faultCard());
		});
		note = "";
		limeStopped = false;
		showCard(false);
	}

	private int noReagentSince = -1;
	private boolean sawNoAir, resumed;

	private void rowFilm() {
		if (!wanted("rtm05_row")) {
			return;
		}
		showCard(true);
		Cam a = cam(-22, 26, -16, 4, 2, 20);
		Cam b = cam(-26, 24, 40, 4, 2, 40);
		Cam e = cam(-8, 30, 96, 6, 2, 56);
		film("rtm05_row", 1300, via(a, b, e, 1300), (level, t) -> card(rowCard(level)));
		showCard(false);
	}

	private void frothFilm() {
		if (!wanted("rtm05_froth_close")) {
			return;
		}
		showCard(true);
		Cam a = cam(-1.5, 10.5, 11, 3.5, 5.6, 15.5);
		Cam b = cam(-0.5, 9.0, 13.5, 3.5, 5.6, 15.5);
		film("rtm05_froth_close", 800, move(a, b, 800), (level, t) -> card(frothCard()));
		showCard(false);
	}

	private void columnsFilm() {
		if (!wanted("rtm05_columns")) {
			return;
		}
		showCard(true);
		Cam a = cam(32, 20, 30, 15, 7, 42);
		Cam b = cam(30, 14, 66, 15, 2, 50);
		film("rtm05_columns", 1000, move(a, b, 1000), (level, t) -> card(columnsCard(level)));
		showCard(false);
	}

	/** Each cell's AIR point; the large blower's selector to OFF and back. */
	private void airPlcFilm() {
		if (!wanted("rtm05_air_plc")) {
			return;
		}
		showCard(true);
		Cam a = cam(-20, 18, 6, 4, 4, 30);
		Cam b = cam(-20, 18, 50, 4, 4, 40);
		film("rtm05_air_plc", 1400, move(a, b, 1400), (level, t) -> {
			int s = t - PAD;
			if (s == 300) {
				setHoa(rblower, "OFF");
				event("large blower selector to OFF");
			}
			if (s == 800) {
				setHoa(rblower, "HAND");
				event("large blower selector to HAND");
			}
			if (s % 100 == 0) {
				event("cell 1: " + cells[0].status() + "; blower " + rblower.status());
			}
			card(airCard());
		});
		showCard(false);
	}

	private void setsFilm() {
		if (!wanted("rtm05_sets")) {
			return;
		}
		showCard(true);
		Cam a = cam(-50, 16, -12, -30, 2, 20);
		Cam b = cam(-56, 18, 70, -30, 2, 40);
		film("rtm05_sets", 700, move(a, b, 700), (level, t) -> {
			List<String> l = new ArrayList<>(List.of("^THE SETS: LOAD OF RATING"));
			String[] names = {"Bank (gas)", "Small plant (diesel)", "Tank, cells 1-2 (gas)", "Cells 3-5 (gas)", "Cells 6-8 (gas)", "Large blower (gas)", "Columns, filter (diesel)"};
			for (int i = 0; i < panels.size() && i < names.length; i++) {
				String s = ((GensetControlPanelBlockEntity) level.getBlockEntity(panels.get(i))).inspect().getString();
				java.util.regex.Matcher m = java.util.regex.Pattern.compile("\\((\\d+) kW.*?(\\d+) kW \\((\\d+)%\\)").matcher(s);
				l.add(names[i] + ": " + (m.find() ? m.group(2) + " of " + m.group(1) + " kW" : "?"));
			}
			card(l.toArray(String[]::new));
		});
		showCard(false);
	}
}
