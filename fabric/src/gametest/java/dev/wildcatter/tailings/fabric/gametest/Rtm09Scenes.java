package dev.wildcatter.tailings.fabric.gametest;

import dev.wildcatter.block.BigPartBlock;
import dev.wildcatter.block.FinalAbsorberBlockEntity;
import dev.wildcatter.block.GensetControlPanelBlockEntity;
import dev.wildcatter.block.GensetSkid;
import dev.wildcatter.block.StorageTankBlockEntity;
import dev.wildcatter.control.Measurable;
import dev.wildcatter.energy.ConductorSize;
import dev.wildcatter.fluid.Product;
import dev.wildcatter.fluid.SlurryAssay;
import dev.wildcatter.multiblock.BigParts;
import dev.wildcatter.tailings.TailingsBlocks;
import dev.wildcatter.tailings.TailingsItems;
import dev.wildcatter.tailings.block.PlantBlockEntity;
import dev.wildcatter.tailings.block.TailingsPartBlock;
import dev.wildcatter.tailings.block.stage.SmelterStages;
import dev.wildcatter.tailings.fabric.gametest.Trailer.Cam;
import dev.wildcatter.tailings.plant.Machine;
import dev.wildcatter.tailings.plant.Smelter;
import dev.wildcatter.tailings.rock.Mineral;
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
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.Vec3;

/**
 * Rock to Metal, episode 9, "Concentrate to Metal" (TAILINGS_ONLY=rtm09): the copper smelter line (copper-smelter.md) at its own pace,
 * every motor on real power from three Wildcatter gas sets, every cable and pipe run laid joined.
 * <ul>
 * <li>the pressure filter, its copper to the concentrate dryer touching it; the flash smelting furnace (silica sand by a hopper on
 * top), the Peirce-Smith converter, the anode furnace and the anode casting wheel, each touching the next; pipeline gas from a stock
 * tank to the dryer, the furnace and the anode furnace;</li>
 * <li>the furnace's and the converter's off-gas on one pipe to a Wildcatter acid plant converter, its interpass and final absorbers,
 * the acid to a stock tank;</li>
 * <li>the electrorefining tankhouse a level below the casting floor, in a pit: each anode drops from the wheel through a hopper into
 * it; its Wildcatter rectifier against its north end; cathodes melted to copper ingots in a blast furnace beside the pit;</li>
 * <li>a second pressure filter with no dryer: the early-game shortcut, paying copper as ingots at 85 %.</li>
 * </ul>
 * Filmed in real time at 2880 x 1620 with a readout card, read from the blocks each tick. RTM09_SHOTS=a,b films only those.
 * <p>
 * Staged, and said so in the episode: thickened concentrate on both filters' inlets (as if from the concentrate thickener); the gas
 * in the stock tanks; the final absorber's process water; the sand in the hoppers' chests; cathodes put into the blast furnace by hand;
 * the anode, cathode and ingot shown in item frames.
 */
final class Rtm09Scenes {
	private static final int W = 1440, H = 810;
	private static final int PAD = 60;
	private static final String SEED = System.getenv().getOrDefault("TAILINGS_TRAILER_SEED", "tailings");
	private static final String LV = "wildcatter:cable_industrial", BUS = "wildcatter:busway_industrial", MV = "wildcatter:cable_mv";

	private final ClientGameTestContext context;
	private TestServerContext server;
	/** o: the line's origin (ground level, the filter's north-west corner a little east of it). */
	private BlockPos o;
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
	private volatile boolean filming;

	private PlantBlockEntity filter, dryer, furnace, conv, af, wheel, tank, sfilter;
	private BlockPos rectifierPos, acidConv, absorberPos, acidTank, blastPos;
	private final List<BlockPos> gasTanks = new ArrayList<>();
	private final List<BlockPos> sandChests = new ArrayList<>();
	private long lineStart = -1;
	private int cathodesIn, ingotsOut;
	private String note = "";

	/** Thickened concentrate, kg of copper a kg (60 % solids of a 28 % copper concentrate, Smelter.CONC_CU). */
	private static final double CU_PER_KG = 0.60 * Smelter.CONC_CU;

	Rtm09Scenes(ClientGameTestContext context) {
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
			// frame a tick: every film after that drew at half speed (episode 8's rtm08_drop too). Throttle only when minimized.
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
			cardWidth("Sulfur to the acid plant: 1,000 kg; acid 1,000 L");
			try {
				stage();
				power();
				ticks(200);
				dryerFilm();
				furnaceFilm();
				converterFilm();
				acidFilm();
				shortcutFilm();
				lineFilm();
				castFilm();
				tankhouseFilm();
				setsFilm();
				ingotsFilm();
				summaryFilm();
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
		System.out.println("[rtm09] " + line);
	}

	private static void require(boolean ok, String what) {
		if (!ok) {
			throw new AssertionError("[rtm09] " + what);
		}
	}

	private static boolean wanted(String id) {
		String only = System.getenv("RTM09_SHOTS");
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

	private static BlockState wcFacing(String id, Direction facing) {
		return wc(id).defaultBlockState().setValue(BigPartBlock.FACING, facing);
	}

	private BlockPos wcPart(ServerLevel level, String id, BlockPos main) {
		BlockState state = wcFacing(id, Direction.NORTH);
		level.setBlock(main, state, Block.UPDATE_ALL);
		BigParts.grow(level, main, state);
		return main;
	}

	private BlockPos gasTank(ServerLevel level, BlockPos main) {
		wcPart(level, "wildcatter:stock_tank", main);
		if (level.getBlockEntity(main) instanceof StorageTankBlockEntity t) {
			t.testFill(Product.PIPELINE_GAS, 50_000);
		}
		gasTanks.add(main);
		return main;
	}

	private BlockPos sandHopper(ServerLevel level, BlockPos hopper) {
		level.setBlock(hopper, Blocks.HOPPER.defaultBlockState(), Block.UPDATE_ALL);
		level.setBlock(hopper.above(), Blocks.CHEST.defaultBlockState(), Block.UPDATE_ALL);
		if (level.getBlockEntity(hopper.above()) instanceof Container c) {
			c.setItem(0, new ItemStack(Items.SAND, 4));
		}
		sandChests.add(hopper.above());
		return hopper;
	}

	/**
	 * The line along +z from the filter (x 2, z 0) to the casting wheel (z 55); the acid plant north-east of the furnace; the
	 * tankhouse in a pit under the wheel's south half; three gas sets west of the line.
	 */
	private void stage() {
		server.runOnServer(srv -> {
			ServerLevel level = srv.overworld();
			BlockPos s = Trailer.site(level, -1600, 2600, 200, 160);
			int g = s.getY();
			o = new BlockPos(s.getX() + 60, g, s.getZ() + 40);
			Trailer.pad(level, o.getX() - 50, o.getZ() - 40, o.getX() + 70, o.getZ() + 110, g, 16, 40);
			Trailer.forceChunks(level, o.getX() - 70, o.getZ() - 60, o.getX() + 90, o.getZ() + 130);
			// ---- the pit for the tankhouse and its rectifier, a level below the casting floor
			Trailer.fillBox(level, o, Blocks.POLISHED_ANDESITE, -2, 15, -5, -1, 52, 87);
			Trailer.fillBox(level, o, Blocks.AIR, -1, 14, -4, -1, 53, 86);
			// ---- the line
			filter = plant(level, Machine.FILTER, d(2, 0, 0));
			dryer = plant(level, Machine.CONCENTRATE_DRYER, d(2, 0, 5));
			furnace = plant(level, Machine.FLASH_FURNACE, d(2, 0, 21));
			conv = plant(level, Machine.COPPER_CONVERTER, d(2, 0, 37));
			af = plant(level, Machine.ANODE_FURNACE, d(2, 0, 46));
			wheel = plant(level, Machine.ANODE_CASTING_WHEEL, d(2, 0, 55));
			tank = plant(level, Machine.REFINERY_TANKHOUSE, d(2, -4, 58));
			require(filter.touching(Machine.CONCENTRATE_DRYER) == dryer && dryer.touching(Machine.FLASH_FURNACE) == furnace
					&& furnace.touching(Machine.COPPER_CONVERTER) == conv && conv.touching(Machine.ANODE_FURNACE) == af
					&& af.touching(Machine.ANODE_CASTING_WHEEL) == wheel, "the line's parts touch in order");
			// each anode drops from the wheel through one hopper into the tankhouse below
			level.setBlock(d(5, -1, 60), Blocks.HOPPER.defaultBlockState(), Block.UPDATE_ALL);
			// the tankhouse's rectifier against its north end, its AC box on the west face of its main cell
			rectifierPos = wcPart(level, "wildcatter:rectifier", d(2, -4, 54));
			boxCells.add(rectifierPos.west());
			// pipeline gas to the dryer, the furnace and the anode furnace, the east face of each one's front cells
			pipe(level, d(6, 0, 5));
			gasTank(level, d(7, 0, 5));
			pipe(level, d(9, 0, 21));
			gasTank(level, d(10, 0, 21));
			pipe(level, d(7, 0, 46));
			gasTank(level, d(8, 0, 46));
			// silica sand by a hopper on top of the furnace and the converter
			sandHopper(level, d(4, 12, 24));
			sandHopper(level, d(4, 5, 40));
			// ---- the acid plant: its converter, the interpass absorber on its west, the final absorber on its east, acid to a stock tank
			acidConv = wcPart(level, "wildcatter:converter", d(24, 0, 24));
			wcPart(level, "wildcatter:interpass_absorber", d(21, 0, 25));
			absorberPos = wcPart(level, "wildcatter:final_absorber", d(29, 0, 25));
			pipe(level, d(30, 0, 28));
			acidTank = wcPart(level, "wildcatter:stock_tank", d(29, 0, 29));
			// the off-gas: the furnace's (east face of its back cell, top layer) and the converter's, one pipe to the acid plant's front
			List<BlockPos> gas = new ArrayList<>(line(d(9, 11, 36), d(18, 11, 36)));
			gas.addAll(line(d(18, 11, 35), d(18, 11, 22)));
			gas.addAll(line(d(19, 11, 22), d(26, 11, 22)));
			gas.addAll(line(d(26, 10, 22), d(26, 0, 22)));
			gas.add(d(26, 0, 23));
			pipe(level, gas.toArray(BlockPos[]::new));
			List<BlockPos> cgas = new ArrayList<>(line(d(7, 4, 45), d(7, 8, 45)));
			cgas.addAll(line(d(8, 8, 45), d(18, 8, 45)));
			cgas.addAll(line(d(18, 8, 44), d(18, 8, 36)));
			cgas.addAll(line(d(18, 9, 36), d(18, 10, 36)));
			pipe(level, cgas.toArray(BlockPos[]::new));
			// ---- the shortcut: a second filter with no dryer
			sfilter = plant(level, Machine.FILTER, d(16, 0, 0));
			// ---- a blast furnace beside the pit for the cathodes; a wall for the item frames
			blastPos = d(16, 0, 68);
			level.setBlock(blastPos, Blocks.BLAST_FURNACE.defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.NORTH), Block.UPDATE_ALL);
			Trailer.fillBox(level, o, Blocks.POLISHED_ANDESITE, 17, 21, 0, 2, 70, 70);
			for (PlantBlockEntity part : all()) {
				setHoa(part, "OFF");
			}
		});
		context.waitTicks(60);
		server.runCommand("kill @e[type=item]");
		server.runCommand("kill @e[type=!player]");
		String[] shown = {"tailings:copper_anode", "tailings:copper_cathode", "minecraft:copper_ingot"};
		for (int i = 0; i < 3; i++) {
			BlockPos at = d(18 + i, 1, 69);
			server.runCommand(String.format(Locale.ROOT, "summon minecraft:glow_item_frame %d %d %d {Facing:2b,Fixed:1b,Invulnerable:1b,Item:{id:\"%s\",count:1}}", at.getX(), at.getY(),
					at.getZ(), shown[i]));
		}
	}

	private List<PlantBlockEntity> all() {
		return List.of(filter, dryer, furnace, conv, af, wheel, tank, sfilter);
	}

	private BlockPos rbox() {
		return rectifierPos.west();
	}

	/** Three 1 MW gas sets: the industrial-voltage parts on one; the converter's blower and the rectifier each on a set stepped up. */
	private void power() {
		server.runOnServer(srv -> {
			ServerLevel level = srv.overworld();
			BlockPos b = set(level, d(-24, 0, 34), true);
			setNames.add("Converter (gas, stepped up)");
			wire(level, MV, null, stepUp(level, b), box(conv));
			BlockPos c = set(level, d(-24, 0, 66), true);
			setNames.add("Tankhouse rectifier (gas, stepped up)");
			wire(level, MV, null, stepUp(level, c), rbox());
			BlockPos a = set(level, d(-24, 0, 10), true);
			setNames.add("The rest of the line (gas)");
			wire(level, LV, ConductorSize.KCMIL_500, a.east(), box(filter), box(dryer), box(furnace), box(af), box(wheel), box(tank), box(sfilter));
		});
		context.waitTicks(40);
		server.runOnServer(srv -> {
			for (BlockPos panel : panels) {
				((GensetControlPanelBlockEntity) srv.overworld().getBlockEntity(panel)).setWantRun(true);
			}
			keep(srv);
		});
		ticks(400);
		// downstream first, as a real line starts
		server.runOnServer(srv -> hand(tank, wheel, af));
		ticks(80);
		server.runOnServer(srv -> hand(conv));
		ticks(160);
		server.runOnServer(srv -> hand(furnace));
		ticks(80);
		server.runOnServer(srv -> {
			hand(dryer, filter, sfilter);
			lineStart = srv.overworld().getGameTime();
		});
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

	/** A second's thickened concentrate at the line's rate and a twentieth over, as if from the concentrate thickener. */
	private static void feed(PlantBlockEntity f) {
		double kg = 1.05 * Smelter.CU_KGH / 3600 / CU_PER_KG;
		if (f.back().held(Product.THICKENED_CONCENTRATE) < 10 * kg) {
			f.back().offer(Product.THICKENED_CONCENTRATE, kg, SlurryAssay.of(Map.of(Smelter.COPPER, CU_PER_KG)));
		}
	}

	/** Every second: the sets' gas, the filters' feed, the stock tanks' gas, the absorber's water, the sand chests; a log every ten. */
	private void keep(MinecraftServer srv) {
		ServerLevel level = srv.overworld();
		for (BlockPos panel : panels) {
			if (level.getBlockEntity(panel) instanceof GensetControlPanelBlockEntity box) {
				box.setGasForTest(200);
			}
		}
		if (filter == null) {
			return;
		}
		feed(filter);
		feed(sfilter);
		for (BlockPos at : gasTanks) {
			if (level.getBlockEntity(at) instanceof StorageTankBlockEntity t && t.amount() < 20_000) {
				t.testFill(Product.PIPELINE_GAS, 50_000);
			}
		}
		if (level.getBlockEntity(absorberPos) instanceof FinalAbsorberBlockEntity a) {
			a.addWater(100_000);   // process water, topped up for the film
		}
		for (BlockPos at : sandChests) {
			if (level.getBlockEntity(at) instanceof Container c && c.getItem(0).getCount() < 2) {
				c.setItem(0, new ItemStack(Items.SAND, 4));
			}
		}
		if (++keeps % 10 == 0) {
			log(String.format(Locale.ROOT, "tick %.1f ms; line %s", srv.getAverageTickTimeNanos() / 1e6, runTime(level)));
			for (PlantBlockEntity part : all()) {
				log("  " + part.inspect().getString());
			}
			log("  " + acidText(level));
		}
	}

	// ---- readouts -----------------------------------------------------------------------------------------------------------------

	/** Game time a filter last read "filtering": it takes a second's concentrate in a fraction of that and waits for the next. */
	private static final Map<BlockPos, Long> FILTERED = new java.util.HashMap<>();

	/** A filter fed at the line's rate: filtering as its concentrate comes, idle between (its own pace is the small plant's). */
	private static String rf(PlantBlockEntity f, String doing) {
		long now = f.getLevel().getGameTime();
		if (f.status().startsWith("filtering")) {
			FILTERED.put(f.getBlockPos(), now);
		}
		Long last = FILTERED.get(f.getBlockPos());
		if (last != null && now - last < 40 && !f.status().startsWith("FULL")) {
			return "+Pressure filter (15 kW): " + doing + ", as it comes";
		}
		return rd(f, "Pressure filter", "15 kW", doing);
	}

	private static String rd(PlantBlockEntity part, String name, String kw, String doing) {
		String st = part.status();
		if (st.startsWith("NO ") || st.startsWith("NOT ") || st.startsWith("FULL") || st.startsWith("OFF-GAS") || st.startsWith("nowhere")) {
			int cut = st.indexOf(':');
			return "!" + name + ": " + (cut > 0 ? st.substring(0, cut) : st);
		}
		if (part.running() && !st.startsWith("waiting")) {
			return "+" + name + " (" + kw + "): " + f0(amps(part)) + " A, " + doing;
		}
		return name + ": " + (st.length() > 40 ? st.substring(0, 40) : st);
	}

	private static double gasM3(PlantBlockEntity part) {
		Matcher m = Pattern.compile("([\\d.]+) m3 of gas burnt").matcher(part.inspect().getString());
		return m.find() ? Double.parseDouble(m.group(1)) : 0;
	}

	private String runTime(ServerLevel level) {
		if (lineStart < 0) {
			return "-";
		}
		long s = (level.getGameTime() - lineStart) / 20;
		return String.format(Locale.ROOT, "%d min %02d s", s / 60, s % 60);
	}

	private double acidLitres(ServerLevel level) {
		return level.getBlockEntity(acidTank) instanceof StorageTankBlockEntity t ? t.amount() : 0;
	}

	private String acidText(ServerLevel level) {
		return level.getBlockEntity(acidConv) instanceof dev.wildcatter.block.ConverterBlockEntity c ? c.inspect().getString() : "-";
	}

	private int cathodes() {
		return tank.smelter().madeCount();
	}

	private String[] setsCard(ServerLevel level) {
		List<String> l = new ArrayList<>(List.of("^THREE GAS SETS · LOAD OF RATING"));
		for (int i = 0; i < panels.size() && i < setNames.size(); i++) {
			GensetControlPanelBlockEntity p = (GensetControlPanelBlockEntity) level.getBlockEntity(panels.get(i));
			l.add((p.breakerClosed() ? "+" : "") + setNames.get(i) + ": " + f0(p.measure(Measurable.Quantity.WATTS) / 1000) + " of 1,000 kW");
		}
		double sum = 0;
		for (BlockPos at : panels) {
			sum += ((GensetControlPanelBlockEntity) level.getBlockEntity(at)).measure(Measurable.Quantity.WATTS);
		}
		l.add("All three: " + f0(sum / 1000) + " kW");
		return l.toArray(String[]::new);
	}

	private String[] dryerCard(ServerLevel level) {
		return new String[] {"^PRESSURE FILTER · CONCENTRATE DRYER", "Thickened concentrate in (staged)", rf(filter, "copper to the dryer"),
				rd(dryer, "Rotary dryer", "75 kW", "drying to 0.3 % moisture"),
				String.format(Locale.ROOT, "  dried: %.2f t of concentrate (%,.0f kg Cu)", dryer.smelter().fedKg() * Smelter.concPerCu() / 1000, dryer.smelter().fedKg()),
				String.format(Locale.ROOT, "  gas burnt: %.1f m³", gasM3(dryer))};
	}

	private String[] furnaceCard(ServerLevel level) {
		var s = furnace.smelter();
		return new String[] {"^FLASH SMELTING FURNACE", rd(furnace, "Flash furnace", "132 kW", "smelting"),
				String.format(Locale.ROOT, "  concentrate smelted: %.2f t", s.fedKg() * Smelter.concPerCu() / 1000),
				String.format(Locale.ROOT, "  slag to waste: %.2f t, %.1f kg of copper in it", s.slagKg() / 1000, s.lostKg()),
				String.format(Locale.ROOT, "  sulfur to the acid plant: %,.0f kg", s.sulfurKg()), String.format(Locale.ROOT, "  silica flux in its bin: %,.0f kg", furnace.dryKg()),
				String.format(Locale.ROOT, "  gas burnt: %.1f m³", gasM3(furnace))};
	}

	private String[] converterCard(ServerLevel level) {
		return new String[] {"^PEIRCE-SMITH CONVERTER · ANODE FURNACE", rd(conv, "Converter blower", "450 kW", "blowing"),
				String.format(Locale.ROOT, "  matte's copper blown: %,.0f kg", conv.smelter().fedKg()),
				String.format(Locale.ROOT, "  sulfur to the acid plant: %,.0f kg", conv.smelter().sulfurKg()), rd(af, "Anode furnace", "55 kW", "fire refining"),
				String.format(Locale.ROOT, "  copper refined: %,.0f kg", af.smelter().fedKg())};
	}

	private String[] acidCard(ServerLevel level) {
		String st = acidText(level);
		Matcher m = Pattern.compile("([\\d.]+) % converted, ([\\d.]+) kg/s of acid \\((\\d+) t/day\\)").matcher(st);
		Matcher k = Pattern.compile("([\\d.]+) kg/h of SO2").matcher(st);
		List<String> l = new ArrayList<>(List.of("^OFF-GAS TO THE ACID PLANT",
				String.format(Locale.ROOT, "Sulfur off the furnace and converter: %,.0f kg", furnace.smelter().sulfurKg() + conv.smelter().sulfurKg())));
		if (m.find()) {
			l.add("+Acid plant converter: " + m.group(1) + " % converted");
			l.add("  acid: " + m.group(2) + " kg/s (" + m.group(3) + " t/day)");
		} else {
			int at = st.indexOf(": ");
			l.add("!Acid plant converter: " + (at > 0 ? st.substring(at + 2, Math.min(st.length(), at + 42)) : st));
		}
		if (k.find()) {
			l.add("  up the stack: " + k.group(1) + " kg/h of SO2");
		}
		l.add(String.format(Locale.ROOT, "Acid in the tank: %,.0f L", acidLitres(level)));
		l.add("  (absorber's water topped up for the film)");
		return l.toArray(String[]::new);
	}

	private String[] castCard(ServerLevel level) {
		List<String> l = new ArrayList<>(List.of("^ANODE CASTING WHEEL", rd(af, "Anode furnace", "55 kW", "anode copper to the wheel"), rd(wheel, "Casting wheel", "30 kW", "casting"),
				String.format(Locale.ROOT, "  in the mould: %,.0f of 400 kg", wheel.smelter().plated()), "  anodes cast: " + wheel.smelter().madeCount(),
				String.format(Locale.ROOT, "Tankhouse below: %.2f anodes' copper", tank.smelter().copper() / Smelter.anodeKg())));
		if (!note.isEmpty()) {
			l.add(note);
		}
		return l.toArray(String[]::new);
	}

	private String[] tankCard(ServerLevel level) {
		var e = tank.electrolysis();
		List<String> l = new ArrayList<>(List.of("^ELECTROREFINING TANKHOUSE · 72 CELLS", rd(tank, "Electrolyte pumps", "45 kW", "circulating"),
				e.amps() > 1 ? String.format(Locale.ROOT, "+Rectifier: %.1f kA at %.1f V DC", e.amps() / 1000, e.volts()) : "Rectifier: no current",
				String.format(Locale.ROOT, "  anodes' copper in the cells: %.2f", tank.smelter().copper() / Smelter.anodeKg()),
				String.format(Locale.ROOT, "  next cathode: %.1f of %.1f kg", tank.smelter().plated(), Smelter.cathodeKg()), "  cathodes pulled: " + cathodes()));
		if (!note.isEmpty()) {
			l.add(note);
		}
		return l.toArray(String[]::new);
	}

	private String[] ingotCard(ServerLevel level) {
		List<String> l = new ArrayList<>(List.of("^CATHODE TO INGOT", "Cathodes pulled: " + cathodes(), "Blast furnace: " + cathodesIn + " cathodes in (by hand)",
				"  copper ingots out: " + ingots(level)));
		if (!note.isEmpty()) {
			l.add(note);
		}
		return l.toArray(String[]::new);
	}

	private int ingots(ServerLevel level) {
		return level.getBlockEntity(blastPos) instanceof Container c && c.getItem(2).is(Items.COPPER_INGOT) ? c.getItem(2).getCount() + ingotsOut : ingotsOut;
	}

	private String shortcutSays = "";

	private String[] shortcutCard(ServerLevel level) {
		String st = sfilter.status();
		int cut = st.indexOf(" (");
		if (st.startsWith("filtering: ") && cut > 0) {
			shortcutSays = st.substring(cut + 1);   // the filter's own words: "(85 %; a smelter line pays 97.5 %)"
		}
		String a = "copper paid as ingots, the shortcut", b = shortcutSays;
		int ingots = sfilter.countIn(SmelterStages.OUTPUT_SLOTS, new ItemStack(Items.COPPER_INGOT));
		int nuggets = sfilter.countIn(SmelterStages.OUTPUT_SLOTS, dev.wildcatter.tailings.block.Feeds.stack(Mineral.COPPER.nuggetId(), 1));
		return new String[] {"^THE SHORTCUT · A FILTER WITH NO DRYER", "Thickened concentrate in (staged)", rf(sfilter, a), "  " + b,
				"  paid: " + ingots + " copper ingots, " + nuggets + " nuggets"};
	}

	private String[] lineCard(ServerLevel level) {
		return new String[] {"^COPPER SMELTER LINE · RATED 1.68 t/h OF COPPER", rf(filter, "to the dryer"), rd(dryer, "Dryer", "75 kW", "drying"),
				rd(furnace, "Flash furnace", "132 kW", "smelting"), rd(conv, "Converter", "450 kW", "blowing"), rd(af, "Anode furnace", "55 kW", "refining"),
				wheel.status().startsWith("waiting for a mould") ? String.format(Locale.ROOT, "Casting wheel: filling, %,.0f of 400 kg", wheel.smelter().copper() + wheel.smelter().plated()) : rd(wheel, "Casting wheel", "30 kW", "casting"), rd(tank, "Tankhouse", "45 kW", "refining")};
	}

	private String[] summaryCard(ServerLevel level) {
		var f = furnace.smelter();
		double gas = gasM3(dryer) + gasM3(furnace) + gasM3(af);
		return new String[] {"^THIS RUN, FROM THE READOUTS", "Line running: " + runTime(level),
				String.format(Locale.ROOT, "Concentrate dried: %.2f t (%,.0f kg Cu)", dryer.smelter().fedKg() * Smelter.concPerCu() / 1000, dryer.smelter().fedKg()),
				String.format(Locale.ROOT, "Slag to waste: %.2f t, %.1f kg Cu in it", f.slagKg() / 1000, f.lostKg()),
				String.format(Locale.ROOT, "Sulfur to the acid plant: %,.0f kg; acid %,.0f L", f.sulfurKg() + conv.smelter().sulfurKg(), acidLitres(level)),
				String.format(Locale.ROOT, "Pipeline gas burnt: %.1f m³", gas), "Anodes cast: " + wheel.smelter().madeCount() + " · cathodes pulled: " + cathodes(),
				"Copper ingots: " + ingots(level)};
	}

	// ---- the films ----------------------------------------------------------------------------------------------------------------

	private void setsFilm() {
		if (!wanted("rtm09_sets")) {
			return;
		}
		showCard(true);
		film("rtm09_sets", 900, move(dcam(-4, 16, 0, -26, 2, 30), dcam(-4, 14, 70, -26, 2, 50), 900), (level, k) -> card(setsCard(level)));
		showCard(false);
	}

	private void dryerFilm() {
		if (!wanted("rtm09_dryer")) {
			return;
		}
		showCard(true);
		film("rtm09_dryer", 1100, move(dcam(17, 9, -10, 4, 2, 8), dcam(19, 12, 7, 4, 2, 13), 1100), (level, k) -> card(dryerCard(level)));
		showCard(false);
	}

	private void furnaceFilm() {
		if (!wanted("rtm09_furnace")) {
			return;
		}
		showCard(true);
		film("rtm09_furnace", 1200, orbitAround(o, 5.5, 5, 28.5, 27, 9, 215, 100, 1200), (level, k) -> card(furnaceCard(level)));
		showCard(false);
	}

	private void converterFilm() {
		if (!wanted("rtm09_converter")) {
			return;
		}
		showCard(true);
		film("rtm09_converter", 1100, move(dcam(-11, 9, 35, 4, 2, 42), dcam(-10, 8, 57, 4, 2, 50), 1100), (level, k) -> card(converterCard(level)));
		showCard(false);
	}

	private void acidFilm() {
		if (!wanted("rtm09_acid")) {
			return;
		}
		showCard(true);
		film("rtm09_acid", 1100, move(dcam(44, 14, 48, 22, 8, 28), dcam(42, 22, 8, 22, 8, 28), 1100), (level, k) -> card(acidCard(level)));
		showCard(false);
	}

	private void shortcutFilm() {
		if (!wanted("rtm09_shortcut")) {
			return;
		}
		showCard(true);
		film("rtm09_shortcut", 800, move(dcam(27, 6, -7, 17, 1.5, 2), dcam(25, 5, 11, 17, 1.5, 2), 800), (level, k) -> card(shortcutCard(level)));
		showCard(false);
	}

	private void lineFilm() {
		if (!wanted("rtm09_line")) {
			return;
		}
		showCard(true);
		film("rtm09_line", 1400, move(dcam(24, 24, -14, 5, 4, 20), dcam(26, 20, 84, 6, -2, 62), 1400), (level, k) -> card(lineCard(level)));
		showCard(false);
	}

	/** The wheel from its first mould's worth: casts a 400 kg anode, which drops through the hopper into the tankhouse. */
	private void castFilm() {
		if (!wanted("rtm09_cast") && !wanted("rtm09_tankhouse") && !wanted("rtm09_ingots")) {
			return;
		}
		boolean ok = waitFor(40000, level -> wheel.smelter().copper() + wheel.smelter().plated() >= 392 || wheel.smelter().madeCount() > 0);
		log("wheel near a mould's worth: " + ok + ", " + wheel.inspect().getString());
		if (!wanted("rtm09_cast")) {
			waitFor(4000, level -> wheel.smelter().madeCount() > 0);
			return;
		}
		showCard(true);
		int[] seen = {-1, -1};
		endMove = false;
		film("rtm09_cast", 2400, move(dcam(22, 9, 51, 6, 0, 59), dcam(20, 6, 70, 6, -1, 60), 2400), (level, k) -> {
			if (wheel.smelter().madeCount() > 0 && seen[0] < 0) {
				seen[0] = k;
				note = "+Anode cast, down the hopper";
				event("anode cast");
			}
			if (seen[0] >= 0 && seen[1] < 0 && tank.smelter().copper() > 1) {
				seen[1] = k;
				event("anode in the tankhouse");
			}
			if (seen[0] >= 0 && k == seen[0] + 300) {
				endMove = true;
			}
			card(castCard(level));
		});
		note = "";
		showCard(false);
	}

	private void tankhouseFilm() {
		if (!wanted("rtm09_tankhouse")) {
			return;
		}
		boolean ok = waitFor(6000, level -> tank.smelter().plated() >= 30 || cathodes() > 0);
		log("tankhouse plating: " + ok + ", " + tank.inspect().getString());
		showCard(true);
		int[] seen = {-1};
		int start = server.computeOnServer(srv -> cathodes());
		film("rtm09_tankhouse", 1300, move(dcam(18, 9, 60, 6, -3, 68), dcam(17, 7, 92, 6, -3, 74), 1300), (level, k) -> {
			if (cathodes() > start && seen[0] < 0) {
				seen[0] = k;
				note = "+Cathode pulled";
				event("cathode pulled: " + cathodes());
			}
			card(tankCard(level));
		});
		note = "";
		showCard(false);
	}

	/** Two cathodes into the blast furnace by hand, with coal: four copper ingots; the anode, cathode and ingot in frames. */
	private void ingotsFilm() {
		if (!wanted("rtm09_ingots")) {
			return;
		}
		boolean ok = waitFor(8000, level -> tank.countIn(SmelterStages.OUTPUT_SLOTS, new ItemStack(TailingsItems.COPPER_CATHODE)) >= 2);
		log("two cathodes for the blast furnace: " + ok);
		showCard(true);
		film("rtm09_ingots", 900, move(dcam(20.5, 2.6, 61.5, 17.5, 1, 69), dcam(19, 1.6, 66.6, 19, 1.1, 69.5), 900), (level, k) -> {
			if (k == PAD + 60 && level.getBlockEntity(blastPos) instanceof Container bf) {
				int n = Math.min(2, tank.countIn(SmelterStages.OUTPUT_SLOTS, new ItemStack(TailingsItems.COPPER_CATHODE)));
				tank.removeFrom(SmelterStages.OUTPUT_SLOTS, new ItemStack(TailingsItems.COPPER_CATHODE), n);
				bf.setItem(0, new ItemStack(TailingsItems.COPPER_CATHODE, n));
				bf.setItem(1, new ItemStack(Items.COAL, 2));
				cathodesIn += n;
				note = "Cathodes put in by hand";
				event(n + " cathodes into the blast furnace");
			}
			card(ingotCard(level));
		});
		note = "";
		showCard(false);
	}

	private void summaryFilm() {
		if (!wanted("rtm09_summary")) {
			return;
		}
		showCard(true);
		film("rtm09_summary", 1300, move(dcam(-6, 26, 96, 8, 2, 40), dcam(-30, 58, 124, 10, 0, 38), 1300), (level, k) -> card(summaryCard(level)));
		showCard(false);
	}
}
