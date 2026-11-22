package dev.wildcatter.tailings.fabric.gametest;

import dev.wildcatter.block.BigPartBlock;
import dev.wildcatter.block.ChlorinatorBlockEntity;
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
import dev.wildcatter.tailings.TailingsItems;
import dev.wildcatter.tailings.block.BeltConveyorBlock;
import dev.wildcatter.tailings.block.Feeds;
import dev.wildcatter.tailings.block.PlantBlockEntity;
import dev.wildcatter.tailings.block.TailingsPartBlock;
import dev.wildcatter.tailings.block.stage.DryMillStages;
import dev.wildcatter.tailings.fabric.gametest.Trailer.Cam;
import dev.wildcatter.tailings.item.HeavyMineralConcentrateItem;
import dev.wildcatter.tailings.plant.DryMill;
import dev.wildcatter.tailings.plant.HeavyMinerals;
import dev.wildcatter.tailings.plant.Machine;
import dev.wildcatter.tailings.plant.PlantRates;
import dev.wildcatter.tailings.rock.Mineral;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
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
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.Vec3;

/**
 * Rock to Metal, episode 4, "Spirals and Sands" (TAILINGS_ONLY=rtm04): the gravity circuit and the mineral-sands dry mill, every motor
 * on real power from a Wildcatter diesel set at the industrial voltage, every cable and pipe run laid joined.
 * <ul>
 * <li>the cyclone cluster's overflow, a heavy-mineral sand slurry, over the spiral bank; the spirals' tails over the scavenger spiral
 * bank, its tails drawn off as if to flotation; both concentrates to the concentrate thickener and the pressure filter;</li>
 * <li>the filter's heavy mineral concentrate into the gas-fired fluid-bed dryer touching it, the gas off a pipe from a tank standing in
 * for the gas main; the gas line opened (NO GAS) and closed again;</li>
 * <li>the rare-earth roll magnet (ilmenite), the high-tension roll (rutile) and the induced-roll magnet (monazite, the zircon passing);
 * a bag of undried concentrate offered to the roll magnet, refused WET;</li>
 * <li>the zircon concentrate carried to Wildcatter's chlorinator, which takes it by the hopper on top (chlorine on a pipe, coal in the
 * hopper, its blower on the same set).</li>
 * </ul>
 * Filmed in real time at 2880 x 1620 with a clean view and a readout card at the top right, read from the blocks each tick. RTM04_SHOTS=a,b
 * films only those. Films to {@code fabric/build/scene-videos}.
 * <p>
 * Staged, and said so in the episode: the cyclone cluster's overflow is topped up with a heavy-mineral sand slurry as if from the mill (the
 * cluster itself is not run); the scavenger spirals' tails are drawn off as if to flotation; the gas and chlorine tanks are topped up as if
 * piped; a mannequin stands in for the player with the wet bag and the zircon.
 */
final class Rtm04Scenes {
	private static final int W = 1440, H = 810;
	private static final int PAD = 60;
	private static final String SEED = System.getenv().getOrDefault("TAILINGS_TRAILER_SEED", "tailings");
	private static final String LV = "wildcatter:cable_industrial";

	private final ClientGameTestContext context;
	private TestServerContext server;
	private BlockPos o;
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

	private PlantBlockEntity cyclone, spirals, scav, thick, filter, dryer, rer, ht, irm;
	private ChlorinatorBlockEntity chlor;
	private BlockPos gasTank, clTank, gasCut, chest;
	private boolean gasOpen = true;
	private String wetNote = "";
	private LivingEntity hand;
	private int carried;

	/** The feed's heavy minerals: 3.5 % of the solids, split as Wildcatter's heavy mineral sand (25 zircon, 55 ilmenite, 5 rutile, 3 monazite). */
	private static final double HM_OF_SOLIDS = 0.035;
	private static final Map<Mineral, Double> HM = new LinkedHashMap<>();
	static {
		HM.put(Mineral.ZIRCON, 25 / 88.0);
		HM.put(Mineral.ILMENITE, 55 / 88.0);
		HM.put(Mineral.RUTILE, 5 / 88.0);
		HM.put(Mineral.MONAZITE, 3 / 88.0);
	}

	Rtm04Scenes(ClientGameTestContext context) {
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
			cardWidth("Roll magnet: taking the ilmenite, 12 A");
			try {
				stage();
				power();
				warm();
				lineFilm();
				spiralsFilm();
				scavengerFilm();
				filterFilm();
				dryerGas();
				separators();
				zircon();
				setFilm();
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
		System.out.println("[rtm04] " + line);
	}

	private static void require(boolean ok, String what) {
		if (!ok) {
			throw new AssertionError("[rtm04] " + what);
		}
	}

	private static boolean wanted(String id) {
		String only = System.getenv("RTM04_SHOTS");
		return only == null || only.isBlank() || java.util.Arrays.asList(only.split(",")).contains(id);
	}

	private BlockPos at(int x, int y, int z) {
		return o.offset(x, y, z);
	}

	private static BlockPos off(BlockPos b, int[] a) {
		return b.offset(a[0], a[1], a[2]);
	}

	// ---- the run ------------------------------------------------------------------------------------------------------------------


	// ---- window, card, camera, films (as Rtm02Scenes) ----------------------------------------------------------------------------
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
		// Set in its armless default state, a conductor sets itself again with its arms from onPlace, with no neighbour update,
		// and the first set then skips its own: the run before it never hears of it. Tell it, so it grows its arm to this one.
		level.updateNeighborsAt(cell, wc(id), null);
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
			lay(level, id, runSize, cells.get(i));
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

	// ---- keeping the stand-ins up ---------------------------------------------------------------------------------------------------

	/** The feed slurry's assay: kg of each heavy mineral in a kg of overflow at 35 % solids. */
	private static SlurryAssay feedAssay() {
		Map<String, Double> a = new HashMap<>();
		HM.forEach((m, share) -> a.put(m.mineralItemId(), PlantRates.OVERFLOW_SOLIDS * HM_OF_SOLIDS * share));
		return SlurryAssay.of(a);
	}

	/** Every second: fuel, the feed, the tails drawn off, the gas and chlorine, and a log line every 10 s. */
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
		if (cyclone != null) {
			// the cyclone cluster's overflow, as if from the mill
			if (cyclone.out().held(Product.FINES_SLURRY) < 400) {
				cyclone.out().offer(Product.FINES_SLURRY, 400, feedAssay());
			}
			scav.out().give(Product.FINES_SLURRY, Double.MAX_VALUE);   // on to flotation (episode 5)
		}
		if (gasTank != null && level.getBlockEntity(gasTank) instanceof StorageTankBlockEntity t && t.amount() < 2000) {
			t.testFill(Product.PIPELINE_GAS, 2000);   // as if from the gas main
		}
		if (clTank != null && level.getBlockEntity(clTank) instanceof StorageTankBlockEntity t && t.amount() < 2000) {
			t.testFill(Product.CHLORINE, 2000);
		}
		if (dryer != null) {
			measureDrying(level);
		}
		if (++keeps % 10 == 0) {
			log(String.format(Locale.ROOT, "tick %.1f ms", srv.getAverageTickTimeNanos() / 1e6));
			for (PlantBlockEntity p : new PlantBlockEntity[] {spirals, scav, thick, filter, dryer, rer, ht, irm}) {
				if (p != null) {
					log("  " + p.inspect().getString());
				}
			}
			if (chlor != null) {
				log("  chlorinator: " + chlor.inspect().getString());
			}
		}
	}

	private PlantBlockEntity plant(ServerLevel level, Machine machine, BlockPos main) {
		return plant(level, machine, main, Direction.NORTH);
	}

	private void wire(ServerLevel level, BlockPos out, BlockPos... boxes) {
		Set<BlockPos> net = new HashSet<>();
		boolean first = true;
		for (BlockPos b : boxes) {
			run(level, LV, b, first ? out : null, net, 0);
			first = false;
		}
	}

	private void hand(PlantBlockEntity... parts) {
		for (PlantBlockEntity p : parts) {
			setHoa(p, "HAND");
		}
	}

	// ---- building -----------------------------------------------------------------------------------------------------------------

	/**
	 * The line as dry-mill.md's scene lays it, the parts facing north: the cyclone cluster, the spiral bank and the scavenger spirals in a
	 * row south; both concentrates on one pipe to the thickener; the filter, then the dryer south of it (its box free on its west face), the
	 * three separators south of that; the gas tank east of the dryer; the chlorinator east of the separators.
	 */
	private void stage() {
		server.runOnServer(srv -> {
			ServerLevel level = srv.overworld();
			BlockPos s = Trailer.site(level, -1600, 1400, 200, 160);
			int g = s.getY();
			o = new BlockPos(s.getX() + 60, g, s.getZ() + 60);
			Trailer.pad(level, o.getX() - 50, o.getZ() - 40, o.getX() + 70, o.getZ() + 80, g, 16, 40);
			Trailer.forceChunks(level, o.getX() - 70, o.getZ() - 60, o.getX() + 90, o.getZ() + 100);
			cyclone = plant(level, Machine.CYCLONE, at(0, 0, 0));
			boxCells.remove(box(cyclone));
			Trailer.pipe(level, at(0, 0, 2), at(1, 0, 2));
			spirals = plant(level, Machine.SPIRAL_BANK, at(0, 0, 3));
			Trailer.pipe(level, at(0, 0, 9), at(0, 0, 10));
			scav = plant(level, Machine.SCAVENGER_SPIRAL_BANK, at(0, 0, 11));
			Trailer.pipe(level, at(3, 0, 9), at(4, 0, 9), at(5, 0, 9), at(6, 0, 9));
			Trailer.pipe(level, at(3, 0, 17), at(4, 0, 17));
			for (int z = 16; z >= 10; z--) {
				Trailer.pipe(level, at(4, 0, z));
			}
			thick = plant(level, Machine.THICKENER, at(6, 0, 10));
			Trailer.pipe(level, at(6, 0, 18));
			filter = plant(level, Machine.FILTER, at(6, 0, 19));
			dryer = plant(level, Machine.FLUID_BED_DRYER, at(6, 0, 24));
			rer = plant(level, Machine.RER_SEPARATOR, at(6, 0, 30));
			ht = plant(level, Machine.HT_ROLL_SEPARATOR, at(6, 0, 33));
			irm = plant(level, Machine.IRM_SEPARATOR, at(6, 0, 37));
			require(filter.touching(Machine.FLUID_BED_DRYER) == dryer, "the dryer touches the filter");
			require(dryer.touching(Machine.RER_SEPARATOR) == rer && rer.touching(Machine.HT_ROLL_SEPARATOR) == ht && ht.touching(Machine.IRM_SEPARATOR) == irm,
					"the separators touch in a line");
			// the gas: a tank standing in for the gas main, a pipe to the dryer's east side
			gasTank = at(14, 0, 25);
			Trailer.part(level, wc("wildcatter:stock_tank"), gasTank);
			for (int x = 9; x <= 13; x++) {
				Trailer.pipe(level, at(x, 0, 26));
			}
			gasCut = at(11, 0, 26);
			// Wildcatter's chlorinator: chlorine on its east face, coal and zircon by the hopper on top
			BlockPos ch = at(14, 0, 34);
			Trailer.part(level, wc("wildcatter:chlorinator"), ch);
			require(level.getBlockEntity(ch) instanceof ChlorinatorBlockEntity, "the chlorinator at " + ch.toShortString());
			chlor = (ChlorinatorBlockEntity) level.getBlockEntity(ch);
			boxCells.add(ch.west());
			clTank = at(19, 0, 34);
			Trailer.part(level, wc("wildcatter:stock_tank"), clTank);
			Trailer.pipe(level, at(17, 0, 35), at(18, 0, 35));
			int roof = 1;
			while (!level.getBlockState(ch.offset(1, roof, 1)).isAir() && roof < 16) {
				roof++;
			}
			level.setBlock(ch.offset(1, roof, 1), Blocks.HOPPER.defaultBlockState(), Block.UPDATE_ALL);
			chest = ch.offset(1, roof + 1, 1);
			level.setBlock(chest, Blocks.CHEST.defaultBlockState(), Block.UPDATE_ALL);
			((Container) level.getBlockEntity(chest)).setItem(0, new ItemStack(net.minecraft.world.item.Items.COAL, 32));
			log("chlorinator roof at +" + roof);
			for (PlantBlockEntity p : List.of(spirals, scav, thick, filter, dryer, rer, ht, irm)) {
				setHoa(p, "OFF");
			}
			for (Mineral m : HM.keySet()) {
				log(String.format(Locale.ROOT, "%s: nugget %.3f kg, item %.3f kg, product %s", m, m.nuggetAmount().doubleValue(), m.itemAmount().doubleValue(), m.phaseOneProductId()));
			}
		});
		server.runCommand("kill @e[type=item]");
		server.runCommand("kill @e[type=!player]");
	}

	/** One diesel set west of the line; every box on one cable net from it, the parts started from the end back. */
	private void power() {
		server.runOnServer(srv -> {
			ServerLevel level = srv.overworld();
			BlockPos ds = set(level, at(-24, 0, 24), false);
			wire(level, ds.east(), box(spirals), box(scav), box(thick), box(filter), box(dryer), box(rer), box(ht), box(irm), chlor.getBlockPos().west());
		});
		context.waitTicks(40);
		server.runOnServer(srv -> {
			for (BlockPos panel : panels) {
				((GensetControlPanelBlockEntity) srv.overworld().getBlockEntity(panel)).setWantRun(true);
			}
			keep(srv);
		});
		ticks(400);
		server.runOnServer(srv -> hand(irm, ht, rer));
		ticks(40);
		server.runOnServer(srv -> hand(dryer, filter, thick));
		ticks(40);
		server.runOnServer(srv -> hand(scav, spirals));
		ticks(100);
		server.runOnServer(srv -> {
			for (BlockPos panel : panels) {
				log("set " + panel.toShortString() + ": " + ((GensetControlPanelBlockEntity) srv.overworld().getBlockEntity(panel)).inspect().getString());
			}
			keeps = 9;
			keep(srv);
		});
	}

	/** The line run until the dry mill has its feed. */
	private void warm() {
		boolean ok = waitFor(6000, level -> irm.hmc().total() > 1 || won(irm, Mineral.ZIRCON) > 0);
		log("dry mill fed: " + ok);
		ticks(1200);
	}

	// ---- readouts -----------------------------------------------------------------------------------------------------------------

	private static String name(Mineral m) {
		return switch (m) {
			case ZIRCON -> "Zircon";
			case ILMENITE -> "Ilmenite";
			case RUTILE -> "Rutile";
			case MONAZITE -> "Monazite";
			default -> m.name();
		};
	}

	/** kg a separator has paid of a mineral: its nuggets and items, and what it holds toward the next nugget. */
	private static double won(PlantBlockEntity part, Mineral m) {
		int nuggets = part.countIn(DryMillStages.OUTPUT_SLOTS, Feeds.stack(m.nuggetId(), 1));
		int items = part.countIn(DryMillStages.OUTPUT_SLOTS, Feeds.stack(m.phaseOneProductId(), 1));
		return nuggets * m.nuggetAmount().doubleValue() + items * m.itemAmount().doubleValue() + part.cake().held(m);
	}

	private String rd(PlantBlockEntity p, String kw) {
		return p.running() ? "+" + p.machine().displayName() + " (" + kw + "): " + f0(amps(p)) + " A" : p.machine().displayName() + ": " + p.status();
	}

	private static String hmcLine(String what, HeavyMinerals h) {
		return what + ": " + f0(h.total()) + " kg";
	}

	private String gasM3() {
		java.util.regex.Matcher m = java.util.regex.Pattern.compile("([\\d.,]+) m3 of gas burnt").matcher(dryer.inspect().getString());
		return m.find() ? m.group(1) : "0";
	}

	private String[] lineCard() {
		return new String[] {"^GRAVITY CIRCUIT AND DRY MILL", "Feed: 50 t/h at 35 % solids, 3.5 % heavy", rd(spirals, "37 kW"), rd(scav, "37 kW"),
				rd(thick, "5.5 kW"), rd(filter, "15 kW"), rd(dryer, "30 kW"), "Separators: " + (rer.running() && ht.running() && irm.running() ? "+all three running" : "starting")};
	}

	private String[] spiralCard() {
		return new String[] {"^SPIRAL BANK · 24 twin-start spirals", "Feed: cyclone overflow, 50 t/h of solids", "  at 35 % solids, 3.5 % heavy minerals",
				"  1 t/h a start (real: about 2)", rd(spirals, "37 kW"), "To concentrate: half of each heavy mineral", "  at 30 % solids, no reagent", "Tails: on to the scavenger spirals"};
	}

	private String[] scavCard() {
		return new String[] {"^SCAVENGER SPIRAL BANK", "Feed: the spiral bank's tails", rd(scav, "37 kW"), "To concentrate: another 45 % of each", "Tails: on to flotation",
				"Both concentrates: one pipe to the thickener", rd(thick, "5.5 kW")};
	}

	private String[] filterCard() {
		return new String[] {"^THICKENER, FILTER, DRYER", rd(thick, "5.5 kW"), "  thickened to 60 % solids", rd(filter, "15 kW"), "  its cake to the dryer it touches",
				rd(dryer, "30 kW"), "Drying: " + (Double.isNaN(dryRate) ? "-" : f1(dryRate) + " t/h of concentrate")};
	}

	private String[] dryerCard() {
		String st = dryer.status();
		boolean noGas = st.startsWith("NO GAS");
		return new String[] {"^FLUID-BED DRYER · 30 kW blower", (noGas ? "!" : dryer.running() && st.startsWith("drying") ? "+" : "") + "Dryer: " + (noGas ? "NO GAS" : st),
				"Gas line: " + (gasOpen ? "+open" : "!a length taken out"), "Gas: 0.3 GJ a tonne, 7.9 m³ a tonne", "Gas burnt so far: " + gasM3() + " m³",
				"Drying: " + (Double.isNaN(dryRate) ? "-" : f1(dryRate) + " t/h of concentrate")};
	}

	private double dryRate = Double.NaN, gasMark = -1;
	private long gasMarkTick;

	/** t/h dried, from the gas burnt over the last 10 s (7.9 m³ a tonne). */
	private void measureDrying(ServerLevel level) {
		double now = Double.parseDouble(gasM3().replace(",", ""));
		long tick = level.getGameTime();
		if (gasMark < 0 || now < gasMark) {
			gasMark = now;
			gasMarkTick = tick;
		} else if (tick - gasMarkTick >= 200) {
			dryRate = (now - gasMark) / DryMill.gasM3(1000, dev.wildcatter.Tuning.GAS_JOULES_PER_M3) / ((tick - gasMarkTick) / 72_000.0);
			gasMark = now;
			gasMarkTick = tick;
		}
	}

	private String[] sepCard() {
		List<String> l = new ArrayList<>();
		l.add("^DRY MILL · 2 t/h a separator");
		l.add("Dry feed: " + (Double.isNaN(dryRate) ? "-" : f1(dryRate) + " t/h from the dryer"));
		l.add(rd(rer, "5.5 kW"));
		l.add("  Ilmenite paid: " + f0(won(rer, Mineral.ILMENITE)) + " kg");
		l.add(rd(ht, "7.5 kW"));
		l.add("  Rutile paid: " + f1(won(ht, Mineral.RUTILE)) + " kg");
		l.add(rd(irm, "11 kW"));
		l.add("  Monazite: " + f1(won(irm, Mineral.MONAZITE)) + " kg · Zircon: " + f0(won(irm, Mineral.ZIRCON) + carried * Mineral.ZIRCON.itemAmount().doubleValue()) + " kg");
		if (!wetNote.isEmpty()) {
			l.add("!" + wetNote);
		}
		return l.toArray(String[]::new);
	}

	private String[] zirconCard() {
		String s = chlor.inspect().getString();
		String brief = s.contains("chlorinating") ? "+chlorinating at about 1,200 °C" : s.contains("NOT BUILT") || s.contains("waiting for zircon") ? "no zircon in its hopper" : s.length() > 40 ? s.substring(0, 40) : s;
		java.util.regex.Matcher m = java.util.regex.Pattern.compile("([\\d.]+) kg/h of zircon").matcher(s);
		return new String[] {"^ZIRCON TO THE CHLORINATOR", rd(irm, "11 kW"), "  Zircon concentrate ready: " + zirconItems() + " (56.25 kg each)", "Carried to the chlorinator: " + carried,
				"Chlorinator (15 kW blower): " + brief, "  " + (m.find() ? m.group(1) + " kg/h of zircon" : "-"), String.format(Locale.ROOT, "  Zircon fed: %.1f kg", chlor.zirconFeedKg())};
	}

	private int zirconItems() {
		return irm.countIn(DryMillStages.OUTPUT_SLOTS, Feeds.stack(Mineral.ZIRCON.phaseOneProductId(), 1));
	}

	// ---- the films ------------------------------------------------------------------------------------------------------------------

	private void lineFilm() {
		showCard(true);
		Cam a = cam(-14, 16, -12, 3, 2, 8);
		Cam b = cam(24, 18, 6, 8, 2, 22);
		Cam e = cam(30, 13, 44, 10, 3, 32);
		film("rtm04_line", 1100, via(a, b, e, 1100), (level, t) -> card(lineCard()));
		showCard(false);
	}

	private void spiralsFilm() {
		showCard(true);
		film("rtm04_spirals", 900, orbitAround(2, 2.5, 6, 10, 4.5, 110, -70, 900), (level, t) -> card(spiralCard()));
		Cam a = cam(9, 7, 1, 2, 3, 5.5);
		Cam b = cam(7, 5.5, 3, 2, 2.5, 6);
		film("rtm04_spirals_close", 600, move(a, b, 600), (level, t) -> card(spiralCard()));
		showCard(false);
	}

	private void scavengerFilm() {
		showCard(true);
		Cam a = cam(-11, 8, 24, 2, 2, 13);
		Cam b = cam(12, 9, 22, 3, 1.5, 12);
		film("rtm04_scavenger", 800, move(a, b, 800), (level, t) -> card(scavCard()));
		showCard(false);
	}

	private void filterFilm() {
		showCard(true);
		Cam a = cam(20, 10, 6, 9, 1, 16);
		Cam b = cam(18, 8, 22, 7.5, 1.5, 23);
		film("rtm04_filter", 800, move(a, b, 800), (level, t) -> card(filterCard()));
		showCard(false);
	}

	/** The dryer on its gas line: a length taken out (NO GAS), then put back. */
	private void dryerGas() {
		showCard(true);
		Cam a = cam(17, 7, 17, 9.5, 1, 26);
		Cam b = cam(14.5, 5.5, 19.5, 9.5, 1, 26);
		film("rtm04_dryer_gas", 1500, move(a, b, 1500), (level, t) -> {
			int s = t - PAD;
			if (s == 300) {
				level.levelEvent(2001, gasCut, Block.getId(level.getBlockState(gasCut)));
				level.setBlock(gasCut, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
				gasOpen = false;
				event("a length of the gas line taken out");
			}
			if (gasOpen == false && s > 300 && dryer.status().startsWith("NO GAS") && wetNote.isEmpty()) {
				wetNote = " ";
				event("dryer: " + dryer.status());
			}
			if (s == 900) {
				Trailer.pipe(level, gasCut);
				gasOpen = true;
				event("the gas line put back");
			}
			if (s > 900 && wetNote.equals(" ") && dryer.status().startsWith("drying")) {
				wetNote = "";
				event("dryer: " + dryer.status());
			}
			card(dryerCard());
		});
		wetNote = "";
		showCard(false);
	}

	/** Along the three separators; a bag of moist concentrate offered to the roll magnet by hand is refused, WET. */
	private void separators() {
		server.runOnServer(srv -> {
			ServerLevel level = srv.overworld();
			var m = net.minecraft.world.entity.EntityTypes.MANNEQUIN.create(level, net.minecraft.world.entity.EntitySpawnReason.COMMAND);
			require(m != null, "a mannequin");
			BlockPos stand = at(9, 0, 31);
			m.snapTo(stand.getX() + 0.5, stand.getY(), stand.getZ() + 0.5, 90f, 20f);
			m.setYHeadRot(90f);
			HeavyMinerals bag = new HeavyMinerals();
			HM.forEach((min, share) -> bag.add(min, Math.floor(DryMill.ITEM_KG * share)));
			m.setItemSlot(EquipmentSlot.MAINHAND, HeavyMineralConcentrateItem.of(TailingsItems.HEAVY_MINERAL_CONCENTRATE, bag));
			level.addFreshEntity(m);
			hand = m;
		});
		showCard(true);
		Cam a = cam(12.5, 4.5, 29.5, 7, 1.5, 31.5);
		Cam b = cam(14, 5.5, 45, 7, 1.5, 37);
		film("rtm04_separators", 1500, move(a, b, 1500), (level, t) -> {
			int s = t - PAD;
			if (s == 160) {
				hand.swing(InteractionHand.MAIN_HAND, net.minecraft.world.item.component.SwingAnimation.DEFAULT, true);
				ItemStack bag = hand.getMainHandItem().copy();
				boolean took = rer.offer(bag);
				wetNote = took ? "the roll magnet took the moist bag" : "Moist bag offered: " + rer.status();
				event("moist bag offered to the roll magnet: took " + took + "; " + rer.status());
				if (wetNote.length() > 44) {
					wetNote = "Moist bag offered: WET, refused";
				}
			}
			if (s == 560) {
				wetNote = "";
				hand.discard();
			}
			card(sepCard());
		});
		showCard(false);
	}

	/** The zircon concentrate carried from the induced-roll magnet to the chlorinator's hopper (a mannequin standing in for the player). */
	private void zircon() {
		boolean ok = waitFor(18000, level -> zirconItems() >= 1);
		log("zircon concentrate ready: " + ok + " (" + zirconItems() + ")");
		server.runOnServer(srv -> {
			ServerLevel level = srv.overworld();
			var m = net.minecraft.world.entity.EntityTypes.MANNEQUIN.create(level, net.minecraft.world.entity.EntitySpawnReason.COMMAND);
			require(m != null, "a mannequin");
			BlockPos stand = chest.south().below();
			m.snapTo(stand.getX() + 0.5, stand.getY(), stand.getZ() + 0.5, 180f, 30f);
			m.setYHeadRot(180f);
			m.setItemSlot(EquipmentSlot.MAINHAND, Feeds.stack(Mineral.ZIRCON.phaseOneProductId(), 1));
			level.addFreshEntity(m);
			hand = m;
			log("mannequin at " + stand.toShortString() + "; chest at " + chest.toShortString());
		});
		showCard(true);
		Cam a = cam(4, 6, 46, 7, 2, 37);
		Cam b = cam(23, 12, 44, 15, 7, 35);
		film("rtm04_zircon", 1400, move(a, b, 1100), (level, t) -> {
			int s = t - PAD;
			if (s == 600) {
				int n = zirconItems();
				irm.removeFrom(DryMillStages.OUTPUT_SLOTS, Feeds.stack(Mineral.ZIRCON.phaseOneProductId(), 1), n);
				((Container) level.getBlockEntity(chest)).setItem(1, Feeds.stack(Mineral.ZIRCON.phaseOneProductId(), n));
				carried += n;
				hand.swing(InteractionHand.MAIN_HAND, net.minecraft.world.item.component.SwingAnimation.DEFAULT, true);
				event("carried " + n + " zircon concentrate to the chlorinator's hopper");
			}
			if (s % 200 == 0) {
				event("chlorinator: " + chlor.inspect().getString());
			}
			card(zirconCard());
		});
		showCard(false);
		server.runOnServer(srv -> {
			if (hand != null) {
				hand.discard();
			}
		});
	}

	private void setFilm() {
		showCard(true);
		Cam a = cam(-38, 14, 44, -18, 2, 22);
		Cam b = cam(-12, 20, 50, 6, 2, 20);
		film("rtm04_set", 700, move(a, b, 700), (level, t) -> {
			String s = ((GensetControlPanelBlockEntity) level.getBlockEntity(panels.get(0))).inspect().getString();
			java.util.regex.Matcher m = java.util.regex.Pattern.compile("^(\\w+) genset \\((\\d+) kW.*?(RUNNING|STOPPED)[^,]*,.*?(\\d+) kW \\((\\d+)%\\)").matcher(s);
			card("^THE SET: LOAD OF RATING", m.find() ? "Diesel set, " + m.group(2) + " kW: " + m.group(4) + " kW (" + m.group(5) + " %)" : "?",
					"Spirals 2 x 37 kW · thickener 5.5 kW", "Filter 15 kW · dryer blower 30 kW", "Separators 5.5, 7.5 and 11 kW", "Chlorinator blower 15 kW");
		});
		showCard(false);
	}
}
