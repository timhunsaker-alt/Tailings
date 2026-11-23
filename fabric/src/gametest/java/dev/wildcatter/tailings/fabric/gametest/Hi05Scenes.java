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
import dev.wildcatter.tailings.block.CableJunctionBlockEntity;
import dev.wildcatter.tailings.block.DrillBlockEntity;
import dev.wildcatter.tailings.block.TbmCore;
import dev.wildcatter.tailings.client.TbmRenderer;
import dev.wildcatter.tailings.entity.MineVehicleEntity;
import dev.wildcatter.tailings.entity.TbmEntity;
import dev.wildcatter.tailings.mine.DriveCourse;
import dev.wildcatter.tailings.mine.Heading;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
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
 * Heavy Iron, episode 5, "Tunnel boring machine" (TAILINGS_ONLY=hi05): the TBM (docs/multiblocks/tunnel-boring-machine.md) boring
 * into a rock ridge from a portal, on real power.
 * <ul>
 * <li>the site: a pad at the foot of a ridge of stone, its south face cut as a 12 m high portal face; the machine built as blocks at
 * the face, heading north, its three gantries on the twin track behind, a muck train (a battery locomotive and two cars) at the
 * loading point;</li>
 * <li>the power: two Wildcatter 1 MW gas sets, each through its own unit substation run backward, joined on the medium-voltage side and
 * run to the cable junction (as the R10 playthrough found works: docs/multiblocks/tunnel-boring-machine.md, "Powering it"); nothing
 * force-run; gas put in by the scene;</li>
 * <li>every film real time (1 m in 20 s in stone); between films the advance is run fast off camera, its muck taken off with it; the
 * ventilation interlock is off (no fan or duct on a short drive: episode 7); the ridge's east side is cut away for the section films
 * after the inside shot; a villager stands at the face, and later rides the cab's seat.</li>
 * </ul>
 * Filmed at 2880 x 1620 with a readout card read each tick. HI05_SHOTS=a,b films only those.
 */
final class Hi05Scenes {
	private static final int W = 1440, H = 810;
	private static final int PAD = 60;
	private static final String SEED = System.getenv().getOrDefault("TAILINGS_TRAILER_SEED", "tailings");
	private static final String LV = "wildcatter:cable_industrial", BUS = "wildcatter:busway_industrial", MV = "wildcatter:cable_mv";
	private static final String WATER = "wildcatter:pipe";
	/** The ridge's east side, cut away for the section films: from this column east of the cutterhead's main cell. */
	private static final int EAST = 8;

	private final ClientGameTestContext context;
	private TestServerContext server;
	/** The cutterhead's main cell as built (o and q are the same: the harness's frames). */
	private BlockPos o, q, c;
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
	private volatile boolean section;
	/** For the wheel film: the whole face ahead cleared 6 m, so the wheel shows whole. */
	private volatile boolean fullFace;
	/** For the cab film: the bore's east shoulder and crown cut away along the machine too, so nothing stands between the camera and the seat. */
	private volatile boolean shoulder;
	private volatile double front;
	private BlockPos junction;
	private String note = "";

	Hi05Scenes(ClientGameTestContext context) {
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
			// a short drive with no fan and duct (ventilation is episode 7): the air interlock off, said on screen
			server.runCommand("tailings rule ventilation_interlock false");
			context.waitTicks(20);
			hud(false);
			cardWidth("Two gas sets: 1,000 kW of 2,000 kW");
			try {
				stage();
				power();
				if (!System.getenv().getOrDefault("HI05_PROBE", "").isBlank()) {
					probe();
					return;
				}
				portalFilm();
				startFilm();
				powerFilm();
				forward(12);
				backupFilm();
				forward(34);
				boreFilm();
				openSection();
				faceFilm();
				wheelFilm();
				ringsFilm();
				floorFilm();
				cabFilm();
				beltFilm();
				trainFilm();
				wideFilm();
			} catch (RuntimeException | AssertionError ex) {
				log("FAILED: " + ex);
				ex.printStackTrace();
				throw ex;
			} finally {
				server.runCommand("gamemode spectator @p");
				server.runCommand("effect clear @p");
				cardWidth("");
				context.runOnClient(client -> client.gui.setScreen(null));
				hud(true);
				log("summary:\n  " + String.join("\n  ", summary));
			}
		}
	}

	private static void log(String line) {
		System.out.println("[hi05] " + line);
	}

	private static void require(boolean ok, String what) {
		if (!ok) {
			throw new AssertionError("[hi05] " + what);
		}
	}

	private static boolean wanted(String id) {
		String only = System.getenv("HI05_SHOTS");
		return only == null || only.isBlank() || java.util.Arrays.asList(only.split(",")).contains(id);
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
		server.runCommand("kill @e[type=minecraft:item]");   // anything the section cut knocked loose
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


	// ---- building (as Hi04Scenes) ----

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
	private boolean noGo(BlockPos cell) {
		int x = cell.getX() - c.getX(), z = cell.getZ() - c.getZ();
		return x >= -1 && x <= 8 && z >= -2 && z <= 60;   // the machine, its track and the gantries
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

	// ---- the site -----------------------------------------------------------------------------------------------------------------

	/** The TBM as Trailer.tbm builds it, on real power: its cab not forced, the advance at its real rate. */
	private void stage() {
		server.runOnServer(srv -> {
			ServerLevel level = srv.overworld();
			BlockPos s = Trailer.site(level, 400, -200, 90, 120);
			int g = s.getY();
			c = new BlockPos(s.getX() + 40, g, s.getZ() + 80);
			o = c;
			q = c;
			Trailer.pad(level, c.getX() - 45, c.getZ() - 95, c.getX() + 50, c.getZ() + 50, g, 14, 40);
			// the ridge: a 12 m portal face along z -1, rising inward, its flanks easing down to the pad
			Trailer.hill(level, c.getX() - 34, c.getZ() - 95, c.getX() + 42, c.getZ() - 1, g, (x, z) -> {
				int fromSouth = c.getZ() - 1 - z;
				int fromWest = x - (c.getX() - 34);
				int fromEast = c.getX() + 42 - x;
				int fromNorth = z - (c.getZ() - 95);
				int h = Math.min(22, 12 + fromSouth / 2);
				return Math.min(h, Math.min(Math.min(fromWest, fromEast), fromNorth) + 2);
			});
			// the portal face is cut rock: the hill's grass and dirt over the face trimmed back to stone a metre round the bore
			Trailer.fillBox(level, c, Blocks.STONE, -2, 9, 0, 9, -3, -1);
			BlockPos cab = Trailer.tbm(level, c, 0);
			DrillBlockEntity d = (DrillBlockEntity) level.getBlockEntity(cab);
			d.core().setForced(false);   // Trailer.tbm forces it: here it runs on the junction's power
			d.program().setFlatLength(120);
			junction = c.offset(-2, 0, 14);
			// the muck train at the loading point, heading out: a battery locomotive and two cars behind the conveyor gantry's tail
			for (MineVehicleEntity v : level.getEntitiesOfClass(MineVehicleEntity.class, new AABB(c).inflate(60))) {
				v.discard();   // Trailer.tbm's lone car
			}
			MineVehicleEntity loco = MineVehicleEntity.placeLocomotive(level, c.offset(2, 1, 35), Heading.SOUTH, TailingsEntities.BATTERY_LOCOMOTIVE);
			require(loco != null, "the battery locomotive on the rail");
			MineVehicleEntity car1 = MineVehicleEntity.placeCar(level, c.offset(2, 1, 32), Heading.SOUTH);
			MineVehicleEntity car2 = MineVehicleEntity.placeCar(level, c.offset(2, 1, 29), Heading.SOUTH);
			log("train: loco " + loco.position() + ", cars " + (car1 == null ? "-" : car1.position()) + ", " + (car2 == null ? "-" : car2.position()) + "; coupled " + loco.carIds().size());
			log("site: cutterhead at " + c.toShortString() + ", cab " + cab.toShortString() + ", junction " + junction.toShortString());
		});
		context.waitTicks(40);
	}

	/** A unit substation run backward east of {@code panel}: the set's industrial voltage in, medium voltage out. Returns the cell its MV side feeds. */
	private BlockPos stepUp(ServerLevel level, BlockPos panel) {
		Block sub = wc("wildcatter:unit_substation");
		BlockState state = sub.defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.SOUTH);
		BlockPos main = panel.relative(Direction.EAST, 4);
		level.setBlock(main, state, Block.UPDATE_ALL);
		BigParts.grow(level, main, state);
		return panel.relative(Direction.EAST, 5);
	}

	/** Two 1 MW gas sets, each stepped up through its own unit substation, joined on the MV side and run to the cable junction. */
	private void power() {
		server.runOnServer(srv -> {
			ServerLevel level = srv.overworld();
			BlockPos[] at = {c.offset(-16, 0, 9), c.offset(-16, 0, 19)};
			List<BlockPos> outs = new ArrayList<>();
			for (BlockPos panel : at) {
				List<BlockPos> overwrote = GensetSkid.build(level, panel, Direction.NORTH, true);
				if (!overwrote.isEmpty()) {
					log("the set at " + panel.toShortString() + " overwrote " + overwrote.size() + " blocks");
				}
				panels.add(panel);
				outs.add(stepUp(level, panel));
			}
			for (BlockPos out : outs) {
				boxCells.add(out);
			}
			wire(level, MV, null, junction.west(), outs.get(0), outs.get(1));
			log("power: gas sets at " + panels + ", MV to the junction at " + junction.toShortString());
		});
		// a set takes its start only once its skid has formed (a start before then is dropped)
		context.waitTicks(40);
		server.runOnServer(srv -> {
			for (BlockPos panel : panels) {
				GensetControlPanelBlockEntity box = (GensetControlPanelBlockEntity) srv.overworld().getBlockEntity(panel);
				box.setGasForTest(400);
				box.setWantRun(true);
			}
		});
		// the sets start and come up to speed off camera
		waitFor(1200, level -> panels.stream().allMatch(p -> level.getBlockEntity(p) instanceof GensetControlPanelBlockEntity b && b.online()));
		ticks(200);
		server.runOnServer(srv -> {
			for (BlockPos p : panels) {
				log("set " + p.toShortString() + ": " + ((GensetControlPanelBlockEntity) srv.overworld().getBlockEntity(p)).inspect().getString());
			}
			log("junction: " + ((CableJunctionBlockEntity) srv.overworld().getBlockEntity(junction)).inspect().getString());
		});
	}

	// ---- the machine and its readout ----------------------------------------------------------------------------------------------

	private TbmEntity machine(ServerLevel level) {
		List<TbmEntity> all = level.getEntitiesOfClass(TbmEntity.class, new AABB(c).inflate(160));
		return all.isEmpty() ? null : all.get(0);
	}

	/** Metres the cutterhead has gone in from where it was built. */
	private double driven(ServerLevel level) {
		return c.getZ() - Trailer.tbmFront(level, c);
	}

	private double setKw(ServerLevel level) {
		double w = 0;
		for (BlockPos p : panels) {
			if (level.getBlockEntity(p) instanceof GensetControlPanelBlockEntity b) {
				w += b.watts();
			}
		}
		return w / 1000;
	}

	private String[] card(ServerLevel level) {
		TbmEntity m = machine(level);
		String sets = "+Two gas sets: " + f0(setKw(level)) + " kW of 2,000 kW";
		if (m == null) {
			DrillBlockEntity cab = level.getBlockEntity(c.offset(3, 3, 3)) instanceof DrillBlockEntity d ? d : null;
			return new String[] {"^TUNNEL BORING MACHINE", cab == null ? "-" : "!" + cab.core().statusLine().toUpperCase(Locale.ROOT), "Parked as blocks",
					"Cutterhead 8 m across", sets};
		}
		int rings = m.core().drill().advanceCount();
		return new String[] {"^TUNNEL BORING MACHINE", m.cutting() ? "+CUTTING" : "!" + m.core().statusLine().toUpperCase(Locale.ROOT),
				"Cutterhead " + f0(TbmRenderer.CUTTERHEAD_RPM) + " rpm, 8 m across", "Advance 1 m in " + f0(DriveCourse.STONE_TICKS / 20.0) + " s (stone)",
				"Bored " + rings + " m, " + rings + " rings (18 segments each)", "Draw: " + f0(m.core().drawWatts() / 1000.0) + " kW", sets, "Cable left on the reel: " + m.core().cableLeft() + " m",
				note};
	}

	private Beat beat() {
		return (level, t) -> {
			front = Trailer.tbmFront(level, c);
			if (section && t % 20 == 0) {
				cutSection(level);
			}
			card(card(level));
		};
	}

	private double f() {
		return front - c.getZ();
	}

	private Cam at(double x, double y, double z, double ax, double ay, double az) {
		return Cam.of(c, x, y, f() + z, ax, ay, f() + az);
	}

	/** Off camera: the advance run fast (half a second a metre) until it is {@code metres} in, its muck taken off as it comes. */
	private void forward(int metres) {
		server.runOnServer(srv -> {
			TbmEntity m = machine(srv.overworld());
			if (m != null) {
				m.core().setTestTicks(10);
			}
		});
		boolean ok = waitFor(metres * 40 + 2000, level -> {
			TbmEntity m = machine(level);
			if (m != null) {
				for (int i = TbmCore.INPUTS; i < TbmCore.SLOTS; i++) {
					m.setItem(i, ItemStack.EMPTY);
				}
			}
			for (MineVehicleEntity v : level.getEntitiesOfClass(MineVehicleEntity.class, new AABB(c).inflate(160))) {
				v.load().clear();
			}
			return driven(level) >= metres;
		});
		server.runOnServer(srv -> {
			TbmEntity m = machine(srv.overworld());
			if (m != null) {
				m.core().setTestTicks(0);
				log("forward to " + f1(driven(srv.overworld())) + " m (" + ok + "): " + m.core().readout());
			}
		});
		// on to the end of the advance it is in, so the next film starts on a real one
		ticks(40);
		placeTrain();
	}

	/** Off camera: the muck train put back at the loading point behind the conveyor gantry's tail, heading out (it has no battery fitted). */
	private void placeTrain() {
		server.runOnServer(srv -> {
			ServerLevel level = srv.overworld();
			for (MineVehicleEntity v : level.getEntitiesOfClass(MineVehicleEntity.class, new AABB(c).inflate(160))) {
				v.discard();
			}
			int fz = (int) Math.floor(Trailer.tbmFront(level, c));
			MineVehicleEntity loco = null;
			for (int dy = 0; dy <= 2 && loco == null; dy++) {
				loco = MineVehicleEntity.placeLocomotive(level, new BlockPos(c.getX() + 2, c.getY() + dy, fz + 35), Heading.SOUTH, TailingsEntities.BATTERY_LOCOMOTIVE);
				if (loco != null) {
					MineVehicleEntity.placeCar(level, new BlockPos(c.getX() + 2, c.getY() + dy, fz + 32), Heading.SOUTH);
					MineVehicleEntity.placeCar(level, new BlockPos(c.getX() + 2, c.getY() + dy, fz + 29), Heading.SOUTH);
				}
			}
			log("train re-placed at rows 29-35: " + (loco == null ? "no rail" : loco.position() + ", " + loco.carIds().size() + " cars"));
		});
		ticks(10);
	}

	private void keep(MinecraftServer srv) {
		ServerLevel level = srv.overworld();
		Trailer.feedTbm(level, c);
		TbmEntity tbm = machine(level);
		if (tbm != null) {
			tbm.setItem(TbmCore.SLOT_COBBLE, new ItemStack(net.minecraft.world.item.Items.COBBLESTONE, 64));   // fill for the cut-away, put in by the scene
		}
		for (BlockPos p : panels) {
			if (level.getBlockEntity(p) instanceof GensetControlPanelBlockEntity b) {
				b.setGasForTest(400);   // gas put in by the scene
			}
		}
		if (++keeps % 15 == 0) {
			TbmEntity m = machine(level);
			log(String.format(Locale.ROOT, "tick %.1f ms; %s; sets %.0f kW", srv.getAverageTickTimeNanos() / 1e6, m == null ? "parked" : m.core().readout(), setKw(level)));
			for (MineVehicleEntity v : level.getEntitiesOfClass(MineVehicleEntity.class, new AABB(c).inflate(160))) {
				if (!v.locomotive()) {
					log("  car at " + v.blockPosition().toShortString() + ": " + v.load().total() + " items");
				}
			}
		}
	}

	// ---- the section --------------------------------------------------------------------------------------------------------------

	/** The ridge east of the bore cut away, once, after the inside shot: the machine shows from its east side. */
	private void openSection() {
		server.runOnServer(srv -> {
			ServerLevel level = srv.overworld();
			for (int x = EAST; x <= 19; x++) {
				for (int z = -96; z <= 0; z++) {
					for (int y = 0; y <= 26; y++) {
						BlockPos at = c.offset(x, y, z);
						if (!level.getBlockState(at).isAir()) {
							level.setBlock(at, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
						}
					}
				}
			}
			section = true;
			cutSection(level);
		});
		ticks(40);
	}

	/** The bore's east wall along the machine, and the east half of the face ahead of it, cut away. */
	private void cutSection(ServerLevel level) {
		int fz = (int) Math.floor(Trailer.tbmFront(level, c)) - c.getZ();
		for (int z = fz - 40; z <= Math.min(0, fz + 40); z++) {
			for (int x = z < fz ? (fullFace && z >= fz - 6 ? 0 : 4) : shoulder ? 5 : 7; x < EAST; x++) {
				for (int y = z < fz ? 0 : -1; y <= 9; y++) {   // ahead of the face, never the ground under the floor to come
					BlockPos at = c.offset(x, y, z);
					BlockState s = level.getBlockState(at);
					if (!s.isAir() && !s.is(TailingsBlocks.MINE_RAIL) && !(z >= fz && x < (shoulder && y >= 6 ? 5 : 7))) {
						level.setBlock(at, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
					}
				}
			}
		}
	}

	// ---- the films ----------------------------------------------------------------------------------------------------------------

	/** hi05_portal: the machine parked as blocks at the portal face, its round head; a villager at the face; from wide to low beside him. */
	private void portalFilm() {
		server.runCommand(String.format(Locale.ROOT, "summon villager %.1f %d %.1f {NoAI:1b,Silent:1b,Invulnerable:1b,Rotation:[150f,0f],Tags:[\"hi05a\"]}",
				c.getX() - 1.3, c.getY(), c.getZ() + 0.6));
		showCard(true);
		Cam a = Cam.of(c, 30, 14, 34, 2, 3, 6);
		Cam b = Cam.of(c, 12, 9, 10, 1, 3, 2);   // over the beam, not through its cab
		Cam e = Cam.of(c, -8.5, 2.4, 6.5, 2, 3.4, 0);
		film("hi05_portal", 1000, via(a, b, e, 1000), (level, t) -> card(card(level)));
	}

	/** hi05_start: Go at the cab; the parts lift into one machine, the wheel turns and it bores its first metres. */
	private void startFilm() {
		server.runCommand("kill @e[tag=hi05a]");
		Cam a = Cam.of(c, 18, 8, 16, 3, 4, 0);
		Cam b = Cam.of(c, 13, 6, 9, 3, 4, -1);
		film("hi05_start", 1300, move(a, b, 1300), (level, t) -> {
			if (t == PAD + 40) {
				((DrillBlockEntity) level.getBlockEntity(c.offset(3, 3, 3))).press(10, null);
				event("Go pressed at the cab");
			}
			TbmEntity m = machine(level);
			if (m != null && note.isEmpty()) {
				note = " ";
				event("lifted into one machine");
			}
			front = Trailer.tbmFront(level, c);
			card(card(level));
		});
		require(server.computeOnServer(srv -> machine(srv.overworld()) != null), "the machine should lift and run: " + server.computeOnServer(srv ->
				level(srv).getBlockEntity(c.offset(3, 3, 3)) instanceof DrillBlockEntity d ? d.core().statusLine() : "no cab"));
	}

	private static ServerLevel level(MinecraftServer srv) {
		return srv.overworld();
	}

	/** hi05_power: the two gas sets and their substations, the MV run to the junction, the trailing cable to the reel. */
	private void powerFilm() {
		// nearer than the first take's (-4, 10, 34) to (6, 6, 26), so the episode needs no hard crop (fix-weak-spots-hi05)
		Cam a = Cam.of(c, -7.5, 7.5, 27.5, -16, 1, 13);
		Cam b = Cam.of(c, 0.5, 5.0, 23.0, -4, 1, 14);   // west of the gantries (their frames span x 1-6)
		film("hi05_power", 800, move(a, b, 800), beat());
	}

	/** hi05_backup: from outside the portal, the back-up going in on the track it laid, the cable paying out behind it. */
	private void backupFilm() {
		Cam a = Cam.of(c, 15, 7, 30, 3, 3, 10);
		Cam b = Cam.of(c, 10, 4.5, 21, 3, 3, 4);
		film("hi05_backup", 800, move(a, b, 800), beat());
	}

	/** hi05_bore: in the bored drive, lit for the camera, looking in along the twin track to the train and the back-up. */
	private void boreFilm() {
		server.runCommand("effect give @p minecraft:night_vision infinite 0 true");
		Cam a = Cam.of(c, 3.5, 3.6, 4, 3.5, 3.2, -20);
		Cam b = Cam.of(c, 3.5, 3.4, -6, 3.5, 3.0, -30);
		film("hi05_bore", 800, move(a, b, 800), beat());
		server.runCommand("effect clear @p");
	}

	/** hi05_face: from ahead of the face on the cut-away side: the wheel turning against the rock, the machine creeping on. */
	private void faceFilm() {
		front = server.computeOnServer(srv -> Trailer.tbmFront(srv.overworld(), c));
		// nearer than the first take's (15.5, 5.5, -9.5), for no hard crop; the chips and dust off the face show
		film("hi05_face", 1000, t -> at(11.5, 5.0, -7.0 - t * 0.003, 3.8, 4.0, 0.5), beat());
	}

	/** hi05_wheel: close on the wheel: its spokes, disc cutters and muck buckets turning together at 6 rpm. */
	private void wheelFilm() {
		fullFace = true;
		server.runOnServer(srv -> cutSection(srv.overworld()));
		film("hi05_wheel", 700, t -> {
			double k = ease(t / 700.0);
			return at(6.5 - 2.0 * k, 4.2, -6.5 + 1.3 * k, 4.0, 4.0, 0);   // nearer than the first take's, for no hard crop
		}, beat());
		fullFace = false;
	}

	/** hi05_rings: alongside behind the cutterhead: the erector setting a ring at its tail, the corners put back, the lined bore behind. */
	private void ringsFilm() {
		film("hi05_rings", 900, t -> at(13.0, 6.0, 5.0, 2.0, 4.0, 3.0), beat());
	}

	/** hi05_floor: the track layer under the beam's rear, laying the floor pieces and the twin track. */
	private void floorFilm() {
		film("hi05_floor", 700, t -> at(11.0, 3.2, 14.0, 3.0, 1.0, 9.0), beat());
	}

	/**
	 * hi05_cab: a villager riding the cab's seat on its roof, put there by the scene. The first take's villager looked sunk into the roof:
	 * the game draws a rider right (the probe: server and client both put it at the roof, y +5), but the section cut left the bore's east
	 * shoulder and crown at the camera's eye height, hiding all but its feet. Here they are cut away along the machine too, and the
	 * camera is nearer (fix-weak-spots-hi05).
	 */
	private void cabFilm() {
		shoulder = true;
		server.runOnServer(srv -> {
			ServerLevel level = srv.overworld();
			cutSection(level);
			TbmEntity m = machine(level);
			var v = net.minecraft.world.entity.EntityTypes.VILLAGER.create(level, EntitySpawnReason.COMMAND);
			if (v != null && m != null) {
				v.setNoAi(true);
				v.setSilent(true);
				v.addTag("hi05b");
				v.setPos(m.getX() + 4, m.getY() + 5, m.getZ() + 10.5);
				level.addFreshEntity(v);
				boolean rode = v.startRiding(m);
				log(String.format(Locale.ROOT, "seat: rode %s; machine y %.2f, the rider at y %.2f (the cab's roof at %.2f)", rode, m.getY(), v.getY(), m.getY() + 5));
			}
		});
		ticks(20);
		film("hi05_cab", 800, t -> {
			double k = ease(t / 800.0);
			return at(8.6 - 1.2 * k, 6.5, 8.5 + k, 4.0, 5.8, 9.0 + k);   // the seat left of the card
		}, beat());
		server.runCommand("kill @e[tag=hi05b]");
		shoulder = false;
	}

	/**
	 * HI05_PROBE: no films, a check of the fixes in stills (in {@code hi05probe/}): a villager riding the cab's seat and one stood on
	 * the beam, their server and client heights logged; the lined bore from inside; the face cutting, from the cut-away side.
	 */
	private void probe() {
		server.runOnServer(srv -> ((DrillBlockEntity) srv.overworld().getBlockEntity(c.offset(3, 3, 3))).press(10, null));
		ticks(60);
		forward(14);
		int[] ids = {-1, -1, -1};
		server.runOnServer(srv -> {
			ServerLevel level = srv.overworld();
			TbmEntity m = machine(level);
			ids[0] = m.getId();
			var v = net.minecraft.world.entity.EntityTypes.VILLAGER.create(level, EntitySpawnReason.COMMAND);
			v.setNoAi(true);
			v.addTag("hi05b");
			v.setPos(m.getX() + 4, m.getY() + 5, m.getZ() + 10.5);
			level.addFreshEntity(v);
			log("probe: rode " + v.startRiding(m) + "; machine y " + m.getY() + ", rider y " + v.getY());
			ids[1] = v.getId();
			var w = net.minecraft.world.entity.EntityTypes.VILLAGER.create(level, EntitySpawnReason.COMMAND);
			w.setNoAi(true);
			w.addTag("hi05b");
			w.setPos(m.getX() + 4, m.getY() + 5, m.getZ() + 6.5);
			level.addFreshEntity(w);
			ids[2] = w.getId();
			log("probe: stood villager y " + w.getY());
		});
		ticks(40);
		String client = context.computeOnClient(cl -> {
			StringBuilder b = new StringBuilder();
			for (int id : ids) {
				var e = cl.level.getEntity(id);
				b.append(e == null ? "missing" : e.getClass().getSimpleName() + " y " + e.getY() + " vehicle " + e.getVehicle() + " bb " + e.getBoundingBox()).append("; ");
			}
			return b.toString();
		});
		log("probe client: " + client);
		server.runOnServer(srv -> {
			for (int id : ids) {
				var e = srv.overworld().getEntity(id);
				log("probe server: " + (e == null ? "missing" : e.getClass().getSimpleName() + " y " + e.getY() + " vehicle " + e.getVehicle()));
			}
		});
		front = server.computeOnServer(srv -> Trailer.tbmFront(srv.overworld(), c));
		showCard(false);
		shot("seat", at(6.6, 6.2, 13.5, 4.0, 6.0, 9.0));
		server.runCommand("effect give @p minecraft:night_vision infinite 0 true");
		shot("bore", Cam.of(c, 3.5, 4.0, 4, 3.5, 4.2, -20));
		shot("bore_up", Cam.of(c, 3.5, 3.0, 2, 3.5, 6.5, -6));
		server.runCommand("effect clear @p");
		openSection();
		front = server.computeOnServer(srv -> Trailer.tbmFront(srv.overworld(), c));
		shot("face", at(12.0, 5.0, -7.0, 3.8, 4.0, 0.5));
		shot("cab_section", at(8.5, 6.4, 8.5, 4.0, 6.0, 10.5));
	}

	private void shot(String name, Cam cam) {
		cam(cam, true);
		settle(40);
		cam(cam, true);
		context.waitTicks(10);
		hideCursor();
		java.nio.file.Path out = java.nio.file.Path.of("hi05probe").toAbsolutePath();
		context.takeScreenshot(net.fabricmc.fabric.api.client.gametest.v1.screenshot.TestScreenshotOptions.of(name).withSize(W * 2, H * 2)
				.withDestinationDir(out).disableCounterPrefix());
		log("probe shot " + name + " to " + out);
	}

	/** hi05_belt: the conveyor gantry's belt carrying the muck back and dropping it into the car under its tail. */
	private void beltFilm() {
		placeTrain();   // the train up behind the back-up again, as a driver would bring it
		film("hi05_belt", 800, t -> at(13.0, 6.5, 21.0, 3.0, 3.0, 26.0), beat());
	}

	/** hi05_train: the muck train at the loading point, the battery locomotive at its head, the cars filling under the gantry's tail. */
	private void trainFilm() {
		film("hi05_train", 800, t -> {
			double k = ease(t / 800.0);
			return at(12.0, 4.5 + k, 40.0 - 6 * k, 3.0, 2.0, 30.0);
		}, beat());
	}

	/** hi05_wide: from the air, a slow turn over the ridge cut open, the portal, the drive, the machine and the power yard. */
	private void wideFilm() {
		front = server.computeOnServer(srv -> Trailer.tbmFront(srv.overworld(), c));
		double mid = (f() + 26) / 2;
		film("hi05_wide", 1200, orbitAround(c, 4, 4, mid, 62, 34, 120, 70, 1200), beat());
	}
}
