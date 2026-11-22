package dev.wildcatter.tailings.fabric.gametest;

import dev.wildcatter.block.BigPartBlock;
import dev.wildcatter.block.GensetControlPanelBlockEntity;
import dev.wildcatter.block.GensetPartBlockEntity;
import dev.wildcatter.block.GensetSkid;
import dev.wildcatter.control.Measurable;
import dev.wildcatter.energy.ConductorSize;
import dev.wildcatter.multiblock.BigParts;
import dev.wildcatter.tailings.TailingsBlocks;
import dev.wildcatter.tailings.block.BeltConveyorBlock;
import dev.wildcatter.tailings.block.DumpBlockEntity;
import dev.wildcatter.tailings.block.MineRailBlock;
import dev.wildcatter.tailings.block.PlantBlockEntity;
import dev.wildcatter.tailings.block.SorterMenu;
import dev.wildcatter.tailings.block.TailingsPartBlock;
import dev.wildcatter.tailings.entity.MineVehicleEntity;
import dev.wildcatter.tailings.fabric.gametest.Trailer.Cam;
import dev.wildcatter.tailings.haul.Haul;
import dev.wildcatter.tailings.mine.Heading;
import dev.wildcatter.tailings.plant.DenseMedium;
import dev.wildcatter.tailings.plant.Machine;
import dev.wildcatter.tailings.plant.RockLoad;
import dev.wildcatter.tailings.plant.XrtSorting;
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
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ParticleStatus;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Rock to Metal, episode 3, "Don't Grind the Waste" (TAILINGS_ONLY=rtm03): the underground plant's front end and its pre-concentration,
 * every motor on real power from Wildcatter's gas and diesel sets at the industrial voltage, every run laid joined.
 * <ul>
 * <li>the dump station: a trolley train (its traction rectifier on a gas set) tips on the move into the dump pocket; the apron feeder and
 * the ore sorter send copper ore to one bin and the rest to the waste bin; the sorter's screen; oversize gathers on the pocket's 300 mm
 * bars until 32 pieces choke it and the next car waits, and a pickaxe (a mannequin standing in for the player) breaks them through;</li>
 * <li>the crushing circuit: an ore bin, the grizzly over the jaw, the double-deck screen and the cone in closed circuit, sand to a chest;</li>
 * <li>dense-medium separation: mixed gravel through the deslime screen and the DMS cyclone module, the medium's density stepped from 2.60
 * to 3.30 on the module's setting, the floats out as gravel, the sinks on a belt; the ferrosilicon medium crafted from Wildcatter's bag;</li>
 * <li>the X-ray sorter: mixed crushed rock, its threshold stepped from LOW to HIGH and on round to REVERSE.</li>
 * </ul>
 * Filmed in real time at 2880 x 1620 with a clean view and a readout card at the top right, read from the blocks each tick. RTM03_SHOTS=a,b
 * films only those. Films to {@code fabric/build/scene-videos}.
 * <p>
 * Staged, and said so in the episode's notes: the crushing circuit's ore bin is topped up with copper ore and granite as if from the dump;
 * the DMS feed belt and the X-ray sorter's feed belt are loaded at their tails as if from the screen's gravel face and from a jaw; the DMS
 * module's water is topped up as if piped; the second train is put on the track loaded, off camera.
 */
final class Rtm03Scenes {
	private static final int W = 1440, H = 810;
	private static final int PAD = 60;
	private static final String SEED = System.getenv().getOrDefault("TAILINGS_TRAILER_SEED", "tailings");
	private static final String LV = "wildcatter:cable_industrial";
	/** The stations, from the site's origin: the dump's track (rail level), the crushing circuit, the DMS, the X-ray sorter, the crafting table. */
	private static final int[] RAIL = {0, 3, 0}, CRUSH = {70, 1, 0}, DMS = {130, 0, 0}, XRT = {180, 0, 0}, TABLE = {110, 0, -30};

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

	// the dump station
	private BlockPos rail;
	private DumpBlockEntity pocket;
	private PlantBlockEntity feeder, sorter, oreBin, wasteBin;
	private final List<MineVehicleEntity> train = new ArrayList<>();
	private LivingEntity breaker;
	// the crushing circuit
	private BlockPos c;
	private PlantBlockEntity bin, grizzly, jaw, screen, cone, sandChestDrive;
	private final List<PlantBlockEntity> crushDrives = new ArrayList<>();
	private BlockPos sandChest;
	private int binTurn;
	// the DMS
	private BlockPos d;
	private PlantBlockEntity deslime, module, floatsScreen, sinksScreen, recovery, dmsFeed, sinksBelt, finesBelt;
	private BlockPos floatsChest, sinksChest, finesChest;
	private double dmsFedMark, dmsFloatsMark;
	private double floatShare = Double.NaN;
	// the X-ray sorter
	private BlockPos x;
	private PlantBlockEntity xrt, compressor, xrtFeed, productBelt, wasteBelt;
	private BlockPos productChest, wasteChest;
	private double xrtProdMark, xrtWasteMark;
	private double productShare = Double.NaN;

	/** The DMS feed's rocks: country rock and ores either side of the medium's range. */
	private static final Rock[] DMS_ROCKS = {Rock.GRANITE, Rock.DIORITE, Rock.CASSITERITE, Rock.COPPER};
	/** The X-ray sorter's feed: andesite, a light ore, and three heavy-element ores of rising atomic number. */
	private static final Rock[] XRT_ROCKS = {Rock.GRAPHITE, Rock.ANDESITE, Rock.MOLYBDENITE, Rock.COPPER, Rock.GALENA};

	Rtm03Scenes(ClientGameTestContext context) {
		this.context = context;
	}

	private static void log(String line) {
		System.out.println("[rtm03] " + line);
	}

	private static void require(boolean ok, String what) {
		if (!ok) {
			throw new AssertionError("[rtm03] " + what);
		}
	}

	private static boolean wanted(String id) {
		String only = System.getenv("RTM03_SHOTS");
		return only == null || only.isBlank() || java.util.Arrays.asList(only.split(",")).contains(id);
	}

	private BlockPos at(int x, int y, int z) {
		return o.offset(x, y, z);
	}

	private static BlockPos off(BlockPos b, int[] a) {
		return b.offset(a[0], a[1], a[2]);
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
			cardWidth("Waste bin: 1,152 items (cobble, granite)");
			try {
				stage();
				power();
				dumpFilm();
				sorterScreen();
				chokeFilm();
				crushing();
				fesiCraft();
				dms();
				xrtFilms();
				sets();
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

	/** Every second: fuel, the stand-ins for what lies outside the stations, the measured shares, and a log line every 10 s. */
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
		if (bin != null && bin.binCount() < 2) {
			// the crushing circuit's ore bin, as if from the dump: copper ore and granite in turn
			bin.binned().merge(binTurn++ % 2 == 0 ? "minecraft:copper_ore" : "minecraft:granite", 1, Integer::sum);
			bin.setChanged();
		}
		if (dmsFeed != null) {
			module.addWater(1000);   // as if piped
			if (dmsFeed.hold().total() < 400) {
				RockLoad load = new RockLoad();
				for (Rock r : DMS_ROCKS) {
					load.add(r, SizeClass.GRAVEL, 100);
				}
				dmsFeed.receive(load);
			}
			var dms = module.dms();
			double fed = dms.fedKg() - dmsFedMark, fl = dms.floatsKg() - dmsFloatsMark;
			if (fed > 60) {
				floatShare = fl / fed;
				dmsFedMark = dms.fedKg();
				dmsFloatsMark = dms.floatsKg();
			}
		}
		if (xrtFeed != null) {
			if (xrtFeed.hold().total() < 500) {
				RockLoad load = new RockLoad();
				for (Rock r : XRT_ROCKS) {
					load.add(r, SizeClass.CRUSHED, 100);
				}
				xrtFeed.receive(load);
			}
			var s = xrt.xrt();
			double p = s.productKg() - xrtProdMark, w = s.wasteKg() - xrtWasteMark;
			if (p + w > 150) {
				productShare = p / (p + w);
				xrtProdMark = s.productKg();
				xrtWasteMark = s.wasteKg();
			}
		}
		for (BlockPos chest : new BlockPos[] {sandChest, floatsChest, sinksChest, finesChest, productChest, wasteChest}) {
			if (chest != null && level.getBlockEntity(chest) instanceof Container box) {
				box.clearContent();   // taken away
			}
		}
		if (++keeps % 10 == 0) {
			log(String.format(Locale.ROOT, "tick %.1f ms", srv.getAverageTickTimeNanos() / 1e6));
			for (PlantBlockEntity p : new PlantBlockEntity[] {feeder, sorter, jaw, screen, cone, deslime, module, floatsScreen, sinksScreen, recovery, xrt, compressor}) {
				if (p != null) {
					log("  " + p.machine() + ": " + p.status() + (p.running() ? " [running, " + f0(amps(p)) + " A]" : ""));
				}
			}
			if (pocket != null) {
				log("  pocket: " + pocket.inspect().getString());
			}
		}
	}

	private static BlockState belt(Direction.Axis axis) {
		return TailingsBlocks.BELT_CONVEYOR.defaultBlockState().setValue(BeltConveyorBlock.AXIS, axis);
	}

	private static void belts(ServerLevel level, BlockPos base, Direction.Axis axis, int[][] cells) {
		for (int[] a : cells) {
			level.setBlock(base.offset(a[0], a[1], a[2]), belt(axis), Block.UPDATE_ALL);
		}
	}

	private PlantBlockEntity plant(ServerLevel level, Machine machine, BlockPos main) {
		return plant(level, machine, main, Direction.NORTH);
	}

	private static void chestAt(ServerLevel level, BlockPos at) {
		level.setBlock(at, Blocks.CHEST.defaultBlockState(), Block.UPDATE_ALL);
	}

	/** Lays a cable from each box to the set's output, the first to the set, the rest joining the run so far. */
	private void wire(ServerLevel level, BlockPos out, PlantBlockEntity... parts) {
		Set<BlockPos> net = new HashSet<>();
		boolean first = true;
		for (PlantBlockEntity p : parts) {
			run(level, LV, box(p), first ? out : null, net, 0);
			first = false;
		}
	}

	private void hand(PlantBlockEntity... parts) {
		for (PlantBlockEntity p : parts) {
			setHoa(p, "HAND");
		}
	}

	// ---- building -----------------------------------------------------------------------------------------------------------------

	private void stage() {
		server.runOnServer(srv -> {
			ServerLevel level = srv.overworld();
			BlockPos s = Trailer.site(level, -1600, 1400, 300, 200);
			int g = s.getY();
			o = new BlockPos(s.getX() + 60, g, s.getZ() + 80);
			Trailer.pad(level, o.getX() - 70, o.getZ() - 80, o.getX() + 240, o.getZ() + 110, g, 16, 40);
			Trailer.forceChunks(level, o.getX() - 90, o.getZ() - 100, o.getX() + 260, o.getZ() + 130);
			stageDump(level);
			stageCrushing(level);
			stageDms(level);
			stageXrt(level);
			level.setBlock(off(o, TABLE), Blocks.CRAFTING_TABLE.defaultBlockState(), Block.UPDATE_ALL);
			for (Rock r : DMS_ROCKS) {
				log(String.format(Locale.ROOT, "DMS: %s SG %.3f", r, DenseMedium.pieceSg(r)));
			}
			for (Rock r : XRT_ROCKS) {
				log(String.format(Locale.ROOT, "XRT: %s Zeff %.2f", r, XrtSorting.pieceZeff(r)));
			}
		});
		server.runCommand("kill @e[type=item]");
		server.runCommand("kill @e[type=!player]");
	}

	/** The dump station as dump-station.md lays it, the track on an embankment over the pocket, the bins east and west of the sorter. */
	private void stageDump(ServerLevel level) {
		rail = off(o, RAIL);
		BlockState track = TailingsBlocks.MINE_RAIL.defaultBlockState().setValue(MineRailBlock.FACING, Direction.NORTH);
		for (int z = -44; z <= 44; z++) {
			if (z < 0 || z > 9) {
				for (int y = -3; y < 0; y++) {
					level.setBlock(rail.offset(0, y, z), Blocks.GRAVEL.defaultBlockState(), Block.UPDATE_CLIENTS);
				}
			}
			level.setBlock(rail.offset(0, 0, z), track, Block.UPDATE_ALL);
		}
		Trailer.part(level, TailingsBlocks.DUMP_POCKET, rail.offset(-1, -3, 0));
		Trailer.part(level, TailingsBlocks.DUMP_RAMP, rail.offset(1, 0, 0));
		pocket = (DumpBlockEntity) level.getBlockEntity(rail.offset(-1, -3, 0));
		feeder = plant(level, Machine.APRON_FEEDER, rail.offset(-1, -3, 3));
		sorter = plant(level, Machine.ORE_SORTER, rail.offset(-2, -3, 7));
		oreBin = plant(level, Machine.ORE_BIN, rail.offset(1, -3, 7));
		wasteBin = plant(level, Machine.ORE_BIN, rail.offset(-5, -3, 8));
		sorter.setSide(Direction.EAST, new ItemStack(Blocks.COPPER_ORE));
		sorter.setWasteSide(Direction.WEST);
		// the traction rectifier on a plinth beside the track, touching the rail
		Trailer.fillBox(level, rail, Blocks.SMOOTH_STONE, -2, -1, -3, -1, -9, -6);
		Trailer.part(level, TailingsBlocks.TRACTION_RECTIFIER, rail.offset(-2, 0, -8));
		boxCells.add(rail.offset(-3, 0, -8));
		// a ledge beside the pocket for whoever clears the bars
		Trailer.fillBox(level, rail, Blocks.SMOOTH_STONE, -3, -2, -3, -1, 0, 2);
		for (PlantBlockEntity p : new PlantBlockEntity[] {feeder, sorter}) {
			setHoa(p, "OFF");
		}
	}

	/** The crushing circuit as the playthrough lays it (Playthrough.crushing), an ore bin over its feed belt, the sand to a chest. */
	private void stageCrushing(ServerLevel level) {
		c = off(o, CRUSH);
		Trailer.fillBox(level, c, Blocks.AIR, -14, 16, -1, 10, -4, 14);
		jaw = plant(level, Machine.JAW, c);
		grizzly = plant(level, Machine.GRIZZLY, c.offset(0, 4, 0));
		screen = plant(level, Machine.SCREEN, c.offset(6, 3, 0));
		cone = plant(level, Machine.CONE, c.offset(6, -1, 7));
		bin = plant(level, Machine.ORE_BIN, c.offset(-11, 0, 1));
		belts(level, c, Direction.Axis.X, new int[][] {{-9, -1, 2}, {-8, -1, 2}, {-7, 0, 2}, {-6, 1, 2}, {-5, 2, 2}, {-4, 3, 2}, {-3, 4, 2}, {-2, 5, 2}, {-1, 6, 2}, {0, 6, 2}, {1, 6, 2}});
		crushDrives.add(plant(level, Machine.CONVEYOR_DRIVE, c.offset(2, 6, 1)));
		belts(level, c, Direction.Axis.X, new int[][] {{3, 5, 1}, {4, 6, 1}, {5, 6, 1}, {6, 6, 1}});
		crushDrives.add(plant(level, Machine.CONVEYOR_DRIVE, c.offset(6, 7, 0)));
		belts(level, c, Direction.Axis.X, new int[][] {{0, 0, 4}, {1, 1, 4}, {2, 2, 4}, {3, 3, 4}, {4, 4, 4}, {5, 5, 4}, {6, 6, 4}});
		crushDrives.add(plant(level, Machine.CONVEYOR_DRIVE, c.offset(6, 7, 3)));
		belts(level, c, Direction.Axis.Z, new int[][] {{7, 6, 4}, {7, 6, 3}, {7, 6, 2}, {7, 6, 1}});
		crushDrives.add(plant(level, Machine.CONVEYOR_DRIVE, c.offset(7, 6, -1)));
		belts(level, c, Direction.Axis.X, new int[][] {{9, 3, 1}, {10, 3, 1}, {11, 3, 1}});
		sandChestDrive = plant(level, Machine.CONVEYOR_DRIVE, c.offset(12, 3, 0));
		crushDrives.add(sandChestDrive);
		Trailer.fillBox(level, c, Blocks.SMOOTH_STONE, 11, 11, -1, 1, 1, 1);
		sandChest = c.offset(11, 2, 1);
		chestAt(level, sandChest);
		belts(level, c, Direction.Axis.Z, new int[][] {{7, 3, 6}, {7, 3, 7}});
		crushDrives.add(plant(level, Machine.CONVEYOR_DRIVE, c.offset(7, 3, 8)));
		belts(level, c, Direction.Axis.X, new int[][] {{8, -1, 10}, {7, -1, 10}, {6, -1, 10}, {5, -1, 10}, {4, -1, 10}});
		crushDrives.add(plant(level, Machine.CONVEYOR_DRIVE, c.offset(4, 0, 10)));
		belts(level, c, Direction.Axis.Z, new int[][] {{3, -1, 10}, {3, 0, 9}, {3, 1, 8}, {3, 2, 7}, {3, 3, 6}, {3, 4, 5}});
		crushDrives.add(plant(level, Machine.CONVEYOR_DRIVE, c.offset(3, 5, 5)));
		for (PlantBlockEntity p : List.of(jaw, screen, cone)) {
			setHoa(p, "OFF");
		}
		for (PlantBlockEntity p : crushDrives) {
			setHoa(p, "OFF");
		}
	}

	/** The DMS plant as the dms film lays it: the deslime screen on the module, the floats screen north, the sinks south, the recovery east. */
	private void stageDms(ServerLevel level) {
		d = off(o, DMS);
		Trailer.fillBox(level, d, Blocks.AIR, -12, 10, 0, 12, -4, 20);
		floatsScreen = plant(level, Machine.DRAIN_RINSE_SCREEN, d);
		level.setBlock(d.offset(0, -1, 0), Blocks.HOPPER.defaultBlockState(), Block.UPDATE_ALL);
		floatsChest = d.offset(0, -2, 0);
		chestAt(level, floatsChest);
		module = plant(level, Machine.DMS_MODULE, d.offset(0, 0, 5));
		sinksScreen = plant(level, Machine.DRAIN_RINSE_SCREEN, d.offset(0, 0, 9));
		belts(level, d, Direction.Axis.Z, new int[][] {{0, 0, 14}, {0, 0, 15}, {0, 0, 16}});
		sinksBelt = plant(level, Machine.CONVEYOR_DRIVE, d.offset(-1, 0, 16));
		sinksChest = d.offset(0, -1, 16);
		chestAt(level, sinksChest);
		recovery = plant(level, Machine.MEDIUM_RECOVERY, d.offset(-2, 0, 6));
		deslime = plant(level, Machine.DESLIME_SCREEN, d.offset(0, 6, 5));
		belts(level, d, Direction.Axis.X, new int[][] {{2, 6, 6}, {3, 6, 6}, {4, 6, 6}});
		finesBelt = plant(level, Machine.CONVEYOR_DRIVE, d.offset(4, 6, 7));
		finesChest = d.offset(4, 5, 6);
		chestAt(level, finesChest);
		// the gravel to the deslime screen's north (feed) end, from the west
		int[][] feed = new int[10][];
		for (int i = 0; i < 10; i++) {
			feed[i] = new int[] {-8 + i, 8, 5};
		}
		belts(level, d, Direction.Axis.X, feed);
		dmsFeed = plant(level, Machine.CONVEYOR_DRIVE, d.offset(2, 8, 4));
		for (int i = 0; i < 40; i++) {
			recovery.offer(new ItemStack(dev.wildcatter.tailings.TailingsItems.FERROSILICON));
		}
		module.dms().setMedium(DenseMedium.SG_LOW);
		for (PlantBlockEntity p : List.of(deslime, module, floatsScreen, sinksScreen, recovery, dmsFeed, sinksBelt, finesBelt)) {
			setHoa(p, "OFF");
		}
	}

	/** The X-ray sorter as the xrt film lays it: its compressor west, the feed belt over its north end, product south, waste east. */
	private void stageXrt(ServerLevel level) {
		x = off(o, XRT);
		Trailer.fillBox(level, x, Blocks.AIR, -10, 14, 0, 8, -4, 18);
		xrt = plant(level, Machine.XRT_SORTER, x);
		compressor = plant(level, Machine.SORTER_COMPRESSOR, x.offset(-2, 0, 3));
		int[][] feed = new int[9][];
		for (int i = 0; i < 9; i++) {
			feed[i] = new int[] {-6 + i, 5, 1};
		}
		belts(level, x, Direction.Axis.X, feed);
		xrtFeed = plant(level, Machine.CONVEYOR_DRIVE, x.offset(3, 5, 0));
		int[][] prod = new int[7][];
		for (int i = 0; i < 7; i++) {
			prod[i] = new int[] {1, 0, 8 + i};
		}
		belts(level, x, Direction.Axis.Z, prod);
		productBelt = plant(level, Machine.CONVEYOR_DRIVE, x.offset(0, 0, 14));
		productChest = x.offset(1, -1, 14);
		chestAt(level, productChest);
		int[][] waste = new int[7][];
		for (int i = 0; i < 7; i++) {
			waste[i] = new int[] {4 + i, 0, 4};
		}
		belts(level, x, Direction.Axis.X, waste);
		wasteBelt = plant(level, Machine.CONVEYOR_DRIVE, x.offset(10, 0, 5));
		wasteChest = x.offset(10, -1, 4);
		chestAt(level, wasteChest);
		for (PlantBlockEntity p : List.of(xrt, compressor, xrtFeed, productBelt, wasteBelt)) {
			setHoa(p, "OFF");
		}
	}

	/** The sets: a gas set for the traction rectifier, diesel sets for the dump, two for the crushing circuit, one each for the DMS and the X-ray sorter. */
	private void power() {
		server.runOnServer(srv -> {
			ServerLevel level = srv.overworld();
			BlockPos gr = set(level, at(-40, 0, -30), true);
			run(level, LV, rail.offset(-3, 0, -8), gr.east(), new HashSet<>(), 0);
			BlockPos dd = set(level, at(-40, 0, 20), false);
			wire(level, dd.east(), sorter, feeder);
			BlockPos c1 = set(level, at(60, 0, 40), false);
			wire(level, c1.east(), jaw, crushDrives.get(0), crushDrives.get(1), crushDrives.get(2), crushDrives.get(3));
			BlockPos c2 = set(level, at(100, 0, 40), false);
			wire(level, c2.east(), cone, screen, crushDrives.get(4), crushDrives.get(5), crushDrives.get(6), crushDrives.get(7));
			BlockPos dm = set(level, at(140, 0, 40), false);
			wire(level, dm.east(), module, deslime, floatsScreen, sinksScreen, recovery, dmsFeed, sinksBelt, finesBelt);
			BlockPos xs = set(level, at(185, 0, 40), false);
			wire(level, xs.east(), compressor, xrt, xrtFeed, productBelt, wasteBelt);
		});
		context.waitTicks(40);
		server.runOnServer(srv -> {
			for (BlockPos panel : panels) {
				((GensetControlPanelBlockEntity) srv.overworld().getBlockEntity(panel)).setWantRun(true);
			}
			keep(srv);
		});
		ticks(400);
		// the plant started from the end back, each part on its own set's power: what takes a part's product runs before it
		server.runOnServer(srv -> hand(sorter, feeder));
		ticks(60);
		server.runOnServer(srv -> hand(crushDrives.get(4), crushDrives.get(5), crushDrives.get(6), crushDrives.get(7)));
		ticks(60);
		server.runOnServer(srv -> hand(cone, screen, crushDrives.get(3), crushDrives.get(2), crushDrives.get(1)));
		ticks(60);
		server.runOnServer(srv -> hand(jaw, crushDrives.get(0)));
		ticks(60);
		server.runOnServer(srv -> hand(sinksBelt, finesBelt, sinksScreen, floatsScreen, recovery));
		ticks(60);
		server.runOnServer(srv -> hand(module, deslime, dmsFeed));
		ticks(60);
		server.runOnServer(srv -> hand(productBelt, wasteBelt, compressor, xrt, xrtFeed));
		ticks(200);
		server.runOnServer(srv -> {
			for (BlockPos panel : panels) {
				log("set " + panel.toShortString() + ": " + ((GensetControlPanelBlockEntity) srv.overworld().getBlockEntity(panel)).inspect().getString());
			}
			keeps = 9;
			keep(srv);
		});
	}

	// ---- the dump station ---------------------------------------------------------------------------------------------------------

	/** A train on the track south of the pocket, heading north: the locomotive, then {@code loads.length} cars, each a full car of one rock. */
	private void placeTrain(String... loads) {
		server.runOnServer(srv -> {
			ServerLevel level = srv.overworld();
			for (MineVehicleEntity v : train) {
				v.discard();
			}
			train.clear();
			train.add(MineVehicleEntity.placeLocomotive(level, rail.offset(0, 0, 30), Heading.NORTH));
			int z = 33;
			for (String load : loads) {
				MineVehicleEntity car = MineVehicleEntity.placeCar(level, rail.offset(0, 0, z), Heading.NORTH);
				require(car != null, "a car at z " + z);
				car.load().add(load, Haul.CAR_ITEMS);
				car.refreshLoad();
				train.add(car);
				z += 2;
			}
			log("train placed: " + train.size() + "; " + train.get(0).status());
		});
	}

	private int loaded() {
		int n = 0;
		for (int i = 1; i < train.size(); i++) {
			if (train.get(i).load().total() > 0) {
				n++;
			}
		}
		return n;
	}

	private String[] dumpCard() {
		int bars = pocket.oversize();
		String ts = train.isEmpty() ? "-" : train.get(0).status();
		ts = ts.contains("choked") ? "!waiting: the grizzly is choked" : ts.startsWith("running") ? ts : ts.length() > 30 ? ts.substring(0, 30) : ts;
		return new String[] {"^DUMP STATION", (ts.startsWith("!") ? "!Train: " + ts.substring(1) : "Train: " + ts),
				"Cars still loaded: " + loaded() + " of " + (train.size() - 1),
				"Pocket: " + f0(pocket.pocket()) + " of " + f0(Haul.POCKET_ITEMS) + " items",
				(pocket.choked() ? "!" : "") + "On the 300 mm bars: " + bars + " pieces" + (pocket.choked() ? ", CHOKED" : " (32 choke it)"),
				"Feeder: " + (feeder.running() && pocket.pocket() > 0 ? "+feeding the sorter" : feeder.status()), "Copper ore bin: " + f0(oreBin.binCount()) + " items", "Waste bin: " + f0(wasteBin.binCount()) + " items"};
	}

	private void dumpFilm() {
		placeTrain("minecraft:copper_ore", "minecraft:cobblestone", "minecraft:copper_ore", "minecraft:granite");
		ticks(20);
		showCard(true);
		Cam a = cam(11, 7, -10, 0, 3, 3);
		Cam b = cam(8, 6, -5, 0, 3, 2);
		Cam e = cam(9, 8, 15, -2, 2, 8);
		film("rtm03_dump", 1100, via(a, b, e, 1100), (level, t) -> {
			if (t == PAD + 1000) {
				log("dump film end: " + pocket.inspect().getString());
			}
			card(dumpCard());
		});
		showCard(false);
		server.runOnServer(srv -> log("after the dump film: " + pocket.inspect().getString() + "; ore bin " + oreBin.binCount() + "; waste bin " + wasteBin.binCount()));
	}

	/** The sorter's screen, opened on it: a slot a side, the waste side marked; the south side set to granite and cleared again. */
	private void sorterScreen() {
		Cam a = cam(-5.5, 6.5, 13.5, -1, 1.5, 8.5);
		cam(a, true);
		settle(40);
		boolean want = wanted("rtm03_sorter_screen");
		guiWidth(400);
		server.runOnServer(srv -> {
			ServerPlayer p = srv.getPlayerList().getPlayers().get(0);
			p.openMenu(new SimpleMenuProvider((id, inv, opener) -> new SorterMenu(id, inv, sorter), Component.literal(sorter.machine().displayName())));
		});
		context.waitTicks(10);
		film("rtm03_sorter_screen", 320, t -> a, (level, t) -> {
			int s = t - PAD;
			if (s == 100) {
				sorter.setSide(Direction.SOUTH, new ItemStack(Blocks.GRANITE));
				event("south side set to granite");
			}
			if (s == 240) {
				sorter.clearSide(2);
				event("south side cleared");
			}
		});
		server.runOnServer(srv -> {
			srv.getPlayerList().getPlayers().get(0).closeContainer();
			sorter.clearSide(2);
			log("sorter after the screen: " + sorter.inspect().getString());
		});
		context.runOnClient(client -> client.gui.setScreen(null));
		guiWidth(720);
		log("sorter screen filmed: " + want);
	}

	/** Oversize gathers on the bars until they choke and a car waits; a pickaxe breaks them through, a piece a swing, and the train goes on. */
	private void chokeFilm() {
		waitFor(2400, level -> pocket.pocket() < 200);
		server.runOnServer(srv -> {
			ServerLevel level = srv.overworld();
			BlockPos stand = rail.offset(-3, 0, 1);
			var m = net.minecraft.world.entity.EntityTypes.MANNEQUIN.create(level, net.minecraft.world.entity.EntitySpawnReason.COMMAND);
			require(m != null, "a mannequin");
			m.snapTo(stand.getX() + 0.5, stand.getY(), stand.getZ() + 0.5, -90f, 0f);
			m.setYHeadRot(-90f);
			m.setItemSlot(net.minecraft.world.entity.EquipmentSlot.MAINHAND, new ItemStack(net.minecraft.world.item.Items.IRON_PICKAXE));
			level.addFreshEntity(m);
			breaker = m;
			log("on the bars before the second train: " + pocket.oversize());
		});
		placeTrain("minecraft:copper_ore", "minecraft:cobblestone", "minecraft:copper_ore", "minecraft:cobblestone", "minecraft:copper_ore");
		ticks(20);
		showCard(true);
		Cam a = cam(-9, 9, 9, 0, 3, 1);
		Cam b = cam(-8, 7.5, 5, 0, 3, 1);
		int[] choked = {-1}, swing = {-1}, clear = {-1};
		film("rtm03_grizzly_choke", 3000, move(a, b, 1400), (level, t) -> {
			int s = t - PAD;
			if (choked[0] < 0 && pocket.choked()) {
				choked[0] = s;
				event("CHOKED: " + pocket.oversize() + " pieces on the bars; " + train.get(0).status());
			}
			if (choked[0] >= 0 && swing[0] < 0 && s >= choked[0] + 200) {
				swing[0] = s;
				event("the pickaxe starts breaking them through");
			}
			if (swing[0] >= 0 && clear[0] < 0 && (s - swing[0]) % 12 == 0) {
				breaker.swing(InteractionHand.MAIN_HAND, net.minecraft.world.item.component.SwingAnimation.DEFAULT, true);
				String broke = pocket.breakOne();
				if (broke != null) {
					level.levelEvent(2001, rail.offset(-1, 0, 1), Block.getId(Blocks.COBBLESTONE.defaultBlockState()));
				}
				if (pocket.oversize() == 0) {
					clear[0] = s;
					event("bars clear; " + train.get(0).status());
				}
			}
			if (clear[0] >= 0 && s >= clear[0] + 300 && loaded() == 0) {
				endMove = true;
			}
			card(dumpCard());
		});
		showCard(false);
		server.runOnServer(srv -> log("after the choke film: " + pocket.inspect().getString()));
	}

	// ---- the crushing circuit ------------------------------------------------------------------------------------------------------

	private String rate(PlantBlockEntity p) {
		return p.running() ? "+" + p.machine().displayName() + ": running, " + f0(amps(p)) + " A" : p.machine().displayName() + ": " + p.status();
	}

	private void crushing() {
		waitFor(3600, level -> screen.running() && cone.running());
		ticks(600);
		showCard(true);
		Cam a = cam(70 - 14, 12, -12, 70 + 2, 4, 4);
		Cam b = cam(70 + 16, 12, -12, 70 + 4, 3, 4);
		Cam e = cam(70 + 18, 9, 18, 70 + 5, 2, 5);
		film("rtm03_crushing", 1400, via(a, b, e, 1400), (level, t) -> card("^CRUSHING CIRCUIT, 50 t/h", rate(jaw), "  closed side 100 mm", rate(screen),
				"  decks 40 and 6 mm", rate(cone), "  closed side 13 mm", "Ore bin: " + bin.status()));
		Cam f = cam(70 + 9, 10, -9, 70 + 1, 4, 2);
		Cam g = cam(70 + 6, 8, -7, 70 + 1, 3.5, 2);
		film("rtm03_jaw_close", 600, move(f, g, 600), (level, t) -> card("^GRIZZLY AND JAW", "Grizzly: 150 mm bars, no motor", rate(jaw), "  110 kW · closed side 100 mm"));
		showCard(false);
	}

	// ---- the ferrosilicon medium, crafted from Wildcatter's bag ----------------------------------------------------------------------

	private void fesiCraft() {
		BlockPos table = off(o, TABLE);
		Cam a = cam(TABLE[0] + 0.5, 1.7, TABLE[2] + 2.5, TABLE[0] + 0.5, 0.5, TABLE[2] + 0.5);
		cam(a, true);
		settle(30);
		guiWidth(400);
		server.runOnServer(srv -> {
			ServerPlayer p = srv.getPlayerList().getPlayers().get(0);
			p.openMenu(new SimpleMenuProvider((id, inv, opener) -> new CraftingMenu(id, inv, ContainerLevelAccess.create(srv.overworld(), table)),
					Component.translatable("container.crafting")));
		});
		context.waitTicks(10);
		String[] grid = {"wildcatter:ferrosilicon_bag", "minecraft:iron_block", "minecraft:iron_ingot", "minecraft:iron_ingot", "minecraft:iron_ingot", "minecraft:iron_ingot"};
		film("rtm03_fesi_craft", 360, t -> a, (level, t) -> {
			int s = t - PAD;
			if (s % 30 == 0 && s / 30 < grid.length && s >= 0) {
				ServerPlayer p = level.getServer().getPlayerList().getPlayers().get(0);
				if (p.containerMenu instanceof CraftingMenu menu) {
					int i = s / 30;
					menu.getSlot(1 + (i < 3 ? i : i + 0)).set(new ItemStack(BuiltInRegistries.ITEM.getValue(Identifier.parse(grid[i]))));
					menu.broadcastChanges();
					event("put " + grid[i] + "; result " + menu.getSlot(0).getItem());
				}
			}
		});
		server.runOnServer(srv -> {
			ServerPlayer p = srv.getPlayerList().getPlayers().get(0);
			if (p.containerMenu instanceof CraftingMenu menu) {
				log("crafting result: " + menu.getSlot(0).getItem());
				for (int i = 1; i <= 9; i++) {
					menu.getSlot(i).set(ItemStack.EMPTY);
				}
			}
			p.closeContainer();
		});
		context.runOnClient(client -> client.gui.setScreen(null));
		guiWidth(720);
	}

	// ---- dense-medium separation --------------------------------------------------------------------------------------------------

	private String[] dmsCard() {
		var dms = module.dms();
		double medium = dms.medium();
		List<String> l = new ArrayList<>();
		l.add("^DMS CYCLONE, 610 mm");
		l.add(String.format(Locale.ROOT, "Medium: SG %.2f · DENSITY %.1f mA", medium, 4 + 16 * (medium - DenseMedium.SG_LOW) / (DenseMedium.SG_HIGH - DenseMedium.SG_LOW)));
		for (Rock r : DMS_ROCKS) {
			boolean sinks = DenseMedium.sinks(r, medium);
			l.add((sinks ? "" : "!") + String.format(Locale.ROOT, "%s, SG %.2f: %s", r.displayName(), DenseMedium.pieceSg(r), sinks ? "sinks" : "FLOATS"));
		}
		l.add("Floating: " + (Double.isNaN(floatShare) ? "-" : f0(floatShare * 100) + " % of the feed"));
		l.add(String.format(Locale.ROOT, "Ferrosilicon lost: %.2f kg (%.1f t fed)", dms.fesiKg(), dms.fedKg() / 1000));
		return l.toArray(String[]::new);
	}

	private void dms() {
		waitFor(3600, level -> module.running());
		ticks(400);
		showCard(true);
		Cam a = cam(DMS[0] + 12, 10, 22, DMS[0] + 4, 6.5, 8);
		Cam b = cam(DMS[0] + 13, 10, -6, DMS[0] + 4, 6.5, 6);
		film("rtm03_dms", 900, move(a, b, 900), (level, t) -> card(dmsCard()));
		// the medium stepped up a setting at a time, held at the points where what floats changes
		double[] holds = {2.60, 2.70, 2.90, 3.20, 3.30};
		int[] next = {0}, since = {0};
		Cam e = cam(DMS[0] - 10, 9, -5, DMS[0] + 3, 6.5, 7);
		Cam f = cam(DMS[0] - 9, 8.5, 0, DMS[0] + 3, 6.5, 8);
		film("rtm03_dms_density", 3600, move(e, f, 3600), (level, t) -> {
			int s = t - PAD;
			var dms = module.dms();
			if (s > 0 && next[0] < holds.length) {
				if (Math.abs(dms.medium() - holds[next[0]]) < 0.001) {
					if (++since[0] >= 400) {
						next[0]++;
						since[0] = 0;
					}
				} else if (s % 20 == 0) {
					event(module.stepMedium());
				}
			}
			if (next[0] >= holds.length) {
				endMove = true;
			}
			card(dmsCard());
		});
		showCard(false);
		server.runOnServer(srv -> {
			module.dms().setMedium(DenseMedium.SG_DEFAULT);
			log("DMS after: " + module.inspect().getString() + "; " + recovery.inspect().getString());
		});
	}

	// ---- the X-ray sorter ----------------------------------------------------------------------------------------------------------

	private String[] xrtCard() {
		int step = xrt.xrtStep();
		boolean rev = xrt.xrtReverse();
		List<String> l = new ArrayList<>();
		l.add("^X-RAY SORTER, 1.2 m · 192 ejectors");
		l.add(String.format(Locale.ROOT, "Threshold: step %d of 5, Zeff %.1f%s", step + 1, XrtSorting.THRESHOLDS[step], rev ? ", REVERSE" : ""));
		for (Rock r : XRT_ROCKS) {
			boolean kept = XrtSorting.over(r, step) != rev;
			l.add((kept ? "+" : "") + String.format(Locale.ROOT, "%s, Zeff %.1f: %s", r.displayName(), XrtSorting.pieceZeff(r), kept ? "PRODUCT" : "waste"));
		}
		l.add("To product: " + (Double.isNaN(productShare) ? "-" : f0(productShare * 100) + " % of the feed"));
		l.add("Compressor: " + (compressor.running() ? "+running, " + f0(amps(compressor)) + " A" : compressor.status()));
		return l.toArray(String[]::new);
	}

	private void xrtFilms() {
		waitFor(3600, level -> xrt.running());
		ticks(300);
		showCard(true);
		Cam a = cam(XRT[0] + 15, 10, 21, XRT[0] + 4, 5.5, 5);
		Cam b = cam(XRT[0] + 17, 9, -6, XRT[0] + 4, 5.5, 4);
		film("rtm03_xrt", 900, move(a, b, 900), (level, t) -> card(xrtCard()));
		// LOW to HIGH, then on round into REVERSE at LOW
		server.runOnServer(srv -> {
			for (int i = 0; i < 12 && !(xrt.xrtStep() == 0 && !xrt.xrtReverse()); i++) {
				xrt.stepThreshold();
			}
			log("threshold before its film: " + XrtSorting.describe(xrt.xrtStep(), xrt.xrtReverse()));
		});
		ticks(200);
		int[] holds = {0, 1, 2, 3, 4, 0};
		int[] next = {0}, since = {0};
		Cam e = cam(XRT[0] + 13, 8, 15, XRT[0] + 4, 4.5, 6);
		Cam f = cam(XRT[0] + 9, 7, 18, XRT[0] + 4, 4.5, 7);
		film("rtm03_xrt_threshold", 3600, move(e, f, 3600), (level, t) -> {
			int s = t - PAD;
			if (s > 0 && next[0] < holds.length) {
				boolean revWanted = next[0] == holds.length - 1;
				if (xrt.xrtStep() == holds[next[0]] && xrt.xrtReverse() == revWanted) {
					if (++since[0] >= 360) {
						next[0]++;
						since[0] = 0;
					}
				} else if (s % 20 == 0) {
					xrt.stepThreshold();
					event("threshold to " + XrtSorting.describe(xrt.xrtStep(), xrt.xrtReverse()));
				}
			}
			if (next[0] >= holds.length) {
				endMove = true;
			}
			card(xrtCard());
		});
		showCard(false);
	}

	// ---- the sets -----------------------------------------------------------------------------------------------------------------

	private void sets() {
		showCard(true);
		Cam a = cam(40, 30, 80, 70, 2, 40);
		Cam b = cam(120, 26, 84, 120, 2, 40);
		film("rtm03_sets", 700, move(a, b, 700), (level, t) -> {
			List<String> l = new ArrayList<>(List.of("^THE SETS: LOAD OF RATING"));
			String[] names = {"Trolley line", "Dump", "Crushing 1", "Crushing 2", "DMS", "X-ray sorter"};
			for (int i = 0; i < panels.size(); i++) {
				BlockPos p = panels.get(i);
				String s = ((GensetControlPanelBlockEntity) level.getBlockEntity(p)).inspect().getString();
				java.util.regex.Matcher m = java.util.regex.Pattern.compile("^(\\w+) genset \\((\\d+) kW.*?(RUNNING|STOPPED)[^,]*,.*?(\\d+) kW \\((\\d+)%\\)").matcher(s);
				l.add(m.find() ? names[i] + ": " + m.group(1).toLowerCase(Locale.ROOT) + " set, " + m.group(2) + " kW: " + m.group(4) + " kW (" + m.group(5) + " %)" : "?");
			}
			card(l.toArray(String[]::new));
		});
		showCard(false);
	}
}
