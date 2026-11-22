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
import dev.wildcatter.tailings.plant.Selective;
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
 * Rock to Metal, episode 6, "Lead from Zinc" (TAILINGS_ONLY=rtm06): selective lead-zinc flotation on the small plant
 * (selective-flotation.md), every motor on real power from Wildcatter generator sets at the industrial voltage, every cable, busway and
 * pipe run laid joined.
 * <ul>
 * <li>the selective line: lead-zinc fines into the conditioning tank set to SELECTIVE (zinc sulfate holds the sphalerite down), bank 1
 * floating the galena, its concentrate to a thickener and filter paying lead; bank 1's tails to the activation tank (copper sulfate
 * frees the sphalerite), bank 2 floating it, its own thickener and filter paying zinc;</li>
 * <li>the same feed on a plain line (the tank on BULK): the lead pays, the zinc the bank floats is lost;</li>
 * <li>the plain line's tank set to SELECTIVE with no zinc sulfate in it (NO REAGENT), then zinc sulfate by its hopper.</li>
 * </ul>
 * Filmed in real time at 2880 x 1620 with a readout card, read from the blocks each tick. RTM06_SHOTS=a,b films only those.
 * <p>
 * Staged, and said so in the episode: galena and sphalerite are separate ore blocks in the game, so the feed is the fines of a block of
 * each, mixed, topped up as if from the mill's cyclones; bank 2's and the plain bank's tails are drawn off as if to the tailings line; the
 * reagent chests over the tanks are kept stocked; the setting is flipped by the scene (a sneaking click in play).
 */
final class Rtm06Scenes {
	private static final int W = 1440, H = 810;
	private static final int PAD = 60;
	private static final String SEED = System.getenv().getOrDefault("TAILINGS_TRAILER_SEED", "tailings");
	private static final String LV = "wildcatter:cable_industrial", BUS = "wildcatter:busway_industrial", MV = "wildcatter:cable_mv";
	/** The plain line's origin, east of the selective line's. */
	private static final int BULK_DX = 60;

	private final ClientGameTestContext context;
	private TestServerContext server;
	private BlockPos o, q, r;
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

	// the selective line
	private PlantBlockEntity src, cond, bank1, blower1, thick1, filter1, act, bank2, blower2, thick2, filter2;
	private BlockPos condChest, actChest, hopper1, chest1, hopper2, chest2;
	// the plain line
	private PlantBlockEntity bsrc, bcond, bbank, bblower, bthick, bfilter;
	private BlockPos bcondChest, bhopper, bchest;
	private volatile boolean zincSulfateToPlain;
	private String note = "";

	private static final Mineral LEAD = Mineral.GALENA, ZINC = Mineral.SPHALERITE;
	/** Galena in a kg of the fines slurry: a galena ore block's 644 kg in its 2.7 t, half the solids, at 35 % solids. */
	private static final double GALENA_KG = share(Rock.GALENA, LEAD);
	/** Sphalerite in a kg of the fines slurry: a sphalerite ore block's 2,240 kg in its 2.7 t, the other half. */
	private static final double SPHALERITE_KG = share(Rock.SPHALERITE, ZINC);

	private static double share(Rock rock, Mineral mineral) {
		return 0.5 * rock.contained(mineral).doubleValue() * mineral.kgPerUnit() / (Reagent.BLOCK_TONNES.doubleValue() * 1000) * PlantRates.OVERFLOW_SOLIDS;
	}

	Rtm06Scenes(ClientGameTestContext context) {
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
			cardWidth("Conditioning tank (15 kW): 000 A [SELECTIVE]");
			try {
				stage();
				power();
				warm();
				lineFilm();
				frothFilm();
				bankOneFilm();
				activationFilm();
				bankTwoFilm();
				filtersFilm();
				bulkFilm();
				setsFilm();
				switchFilm();
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
		System.out.println("[rtm06] " + line);
	}

	private static void require(boolean ok, String what) {
		if (!ok) {
			throw new AssertionError("[rtm06] " + what);
		}
	}

	private static boolean wanted(String id) {
		String only = System.getenv("RTM06_SHOTS");
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


	/** A camera relative to the plain line's origin. */
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

	/**
	 * The selective line (selective-flotation.md's circuit, the parts facing north): the feed's stand-in, the conditioning tank set to
	 * SELECTIVE, bank 1 with its blower against its west side, its concentrate east to the lead thickener and filter, its tails south into
	 * the activation tank, bank 2 and its blower, its concentrate to the zinc thickener and filter. The plain line east of it, laid as
	 * Rtm05Scenes lays the small plant, its tank on BULK.
	 */
	private void stage() {
		server.runOnServer(srv -> {
			ServerLevel level = srv.overworld();
			BlockPos s = Trailer.site(level, -1600, 2400, 260, 160);
			int g = s.getY();
			q = new BlockPos(s.getX() + 90, g, s.getZ() + 50);
			o = q;
			r = q.offset(BULK_DX, 0, 0);
			Trailer.pad(level, q.getX() - 50, q.getZ() - 30, r.getX() + 50, q.getZ() + 90, g, 16, 40);
			Trailer.forceChunks(level, q.getX() - 70, q.getZ() - 50, r.getX() + 70, q.getZ() + 110);
			// ---- the selective line
			src = plant(level, Machine.CYCLONE, sq(0, 0, 0));
			boxCells.remove(box(src));
			Trailer.pipe(level, sq(0, 0, 2), sq(1, 0, 2));
			cond = plant(level, Machine.CONDITIONER, sq(0, 0, 3));
			if (!cond.selective()) {
				cond.toggleSelective();
			}
			Trailer.pipe(level, sq(0, 0, 6));
			bank1 = plant(level, Machine.FLOTATION_BANK, sq(0, 0, 7));
			blower1 = plant(level, Machine.BLOWER, sq(-2, 0, 9));
			require(bank1.touching(Machine.BLOWER) == blower1, "blower 1 touches bank 1");
			Trailer.pipe(level, sq(6, 0, 20), sq(7, 0, 20), sq(8, 0, 20), sq(9, 0, 20));   // the lead concentrate
			thick1 = plant(level, Machine.THICKENER, sq(9, 0, 21));
			Trailer.pipe(level, sq(9, 0, 29), sq(9, 1, 29));
			filter1 = plant(level, Machine.FILTER, sq(9, 1, 30));
			hopper1 = underFilter(level, sq(9, 0, 34));
			chest1 = hopper1.west();
			Trailer.pipe(level, sq(0, 0, 20));   // bank 1's tails, the zinc still held down
			act = plant(level, Machine.ACTIVATION_TANK, sq(0, 0, 21));
			Trailer.pipe(level, sq(0, 0, 24));
			bank2 = plant(level, Machine.FLOTATION_BANK, sq(0, 0, 25));
			blower2 = plant(level, Machine.BLOWER, sq(-2, 0, 27));
			require(bank2.touching(Machine.BLOWER) == blower2, "blower 2 touches bank 2");
			Trailer.pipe(level, sq(6, 0, 38));   // the zinc concentrate
			thick2 = plant(level, Machine.THICKENER, sq(6, 0, 39));
			Trailer.pipe(level, sq(6, 0, 47), sq(6, 1, 47));
			filter2 = plant(level, Machine.FILTER, sq(6, 1, 48));
			hopper2 = underFilter(level, sq(6, 0, 52));
			chest2 = hopper2.west();
			condChest = reagentRig(level, sq(1, 4, 4));
			actChest = reagentRig(level, sq(1, 4, 22));
			// ---- the plain line
			bsrc = plant(level, Machine.CYCLONE, sr(0, 0, 0));
			boxCells.remove(box(bsrc));
			Trailer.pipe(level, sr(0, 0, 2), sr(1, 0, 2));
			bcond = plant(level, Machine.CONDITIONER, sr(0, 0, 3));
			Trailer.pipe(level, sr(0, 0, 6));
			bbank = plant(level, Machine.FLOTATION_BANK, sr(0, 0, 7));
			bblower = plant(level, Machine.BLOWER, sr(-2, 0, 9));
			require(bbank.touching(Machine.BLOWER) == bblower, "the plain line's blower touches its bank");
			Trailer.pipe(level, sr(6, 0, 20));
			bthick = plant(level, Machine.THICKENER, sr(6, 0, 21));
			Trailer.pipe(level, sr(6, 0, 29), sr(6, 1, 29));
			bfilter = plant(level, Machine.FILTER, sr(6, 1, 30));
			bhopper = underFilter(level, sr(6, 0, 34));
			bchest = bhopper.west();
			bcondChest = reagentRig(level, sr(1, 4, 4));
			for (PlantBlockEntity p : all()) {
				setHoa(p, "OFF");
			}
			log(String.format(Locale.ROOT, "feed: galena %.4f, sphalerite %.4f kg a kg of slurry", GALENA_KG, SPHALERITE_KG));
		});
		server.runCommand("kill @e[type=item]");
		server.runCommand("kill @e[type=!player]");
	}

	private List<PlantBlockEntity> all() {
		return List.of(cond, bank1, blower1, thick1, filter1, act, bank2, blower2, thick2, filter2, bcond, bbank, bblower, bthick, bfilter);
	}

	/**
	 * The yards west of each line: each bank on a 1 MW gas set by busway; the selective line's other motors on a 500 kW diesel set by
	 * cable, and the plain line's on another.
	 */
	private void power() {
		server.runOnServer(srv -> {
			ServerLevel level = srv.overworld();
			BlockPos g1 = set(level, sq(-28, 0, 6), true);
			wire(level, BUS, null, g1.east(), box(bank1));
			BlockPos g2 = set(level, sq(-28, 0, 26), true);
			wire(level, BUS, null, g2.east(), box(bank2));
			BlockPos d1 = set(level, sq(-28, 0, 46), false);
			wire(level, LV, ConductorSize.KCMIL_500, d1.east(), box(blower2), box(act), box(blower1), box(cond), box(thick1), box(filter1), box(thick2), box(filter2));
			BlockPos g3 = set(level, sr(-28, 0, 6), true);
			wire(level, BUS, null, g3.east(), box(bbank));
			BlockPos d2 = set(level, sr(-28, 0, 26), false);
			wire(level, LV, ConductorSize.KCMIL_500, d2.east(), box(bblower), box(bcond), box(bthick), box(bfilter));
		});
		context.waitTicks(40);
		server.runOnServer(srv -> {
			for (BlockPos panel : panels) {
				((GensetControlPanelBlockEntity) srv.overworld().getBlockEntity(panel)).setWantRun(true);
			}
			keep(srv);
		});
		ticks(400);
		server.runOnServer(srv -> hand(filter1, thick1, filter2, thick2, bfilter, bthick));
		ticks(60);
		server.runOnServer(srv -> hand(blower1, blower2, bblower, cond, act, bcond));
		ticks(100);
		server.runOnServer(srv -> hand(bank1, bbank));
		ticks(100);
		server.runOnServer(srv -> hand(bank2));
		ticks(100);
		server.runOnServer(srv -> {
			for (BlockPos panel : panels) {
				log("set " + panel.toShortString() + ": " + ((GensetControlPanelBlockEntity) srv.overworld().getBlockEntity(panel)).inspect().getString());
			}
			keeps = 9;
			keep(srv);
		});
	}

	/** Until the lead and zinc filters and the plain filter all pay. */
	private void warm() {
		boolean ok = waitFor(12000, level -> paid(level, filter1, LEAD) > 0 && paid(level, filter2, ZINC) > 0 && paid(level, bfilter, LEAD) > 0);
		log("all three filters paying: " + ok);
		ticks(600);
	}

	// ---- keeping the stand-ins up -------------------------------------------------------------------------------------------------

	private static SlurryAssay leadZinc() {
		return SlurryAssay.of(Map.of(LEAD.mineralItemId(), GALENA_KG, ZINC.mineralItemId(), SPHALERITE_KG));
	}

	/**
	 * A tank's reagent chest, run as its level control would: a few kilograms of a reagent into the chest only when the tank is short of
	 * it and the chest holds none, so the hopper under it never fills with what the tank will not take and blocks the rest.
	 */
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

	/** Every second: fuel, both feeds, the tails drawn off, the reagent chests, the chests emptied when near full. */
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
		for (PlantBlockEntity s : new PlantBlockEntity[] {src, bsrc}) {
			if (s.out().held(Product.FINES_SLURRY) < 400) {
				s.out().offer(Product.FINES_SLURRY, 400 - s.out().held(Product.FINES_SLURRY), leadZinc());   // as if from the mill's cyclones
			}
		}
		bank2.out().give(Product.TAILS_SLURRY, Double.MAX_VALUE);   // on to the tailings line
		bbank.out().give(Product.TAILS_SLURRY, Double.MAX_VALUE);
		stock(level, condChest, cond, Reagent.LIME, Reagent.XANTHATE, Reagent.MIBC, Reagent.ZINC_SULFATE);
		stock(level, actChest, act, Reagent.LIME, Reagent.XANTHATE, Reagent.MIBC, Reagent.COPPER_SULFATE);
		stock(level, bcondChest, bcond, Reagent.LIME, Reagent.XANTHATE, Reagent.MIBC, Reagent.COPPER_SULFATE);
		if (zincSulfateToPlain) {
			stock(level, bcondChest, bcond, Reagent.ZINC_SULFATE);
		}
		if (++keeps % 10 == 0) {
			log(String.format(Locale.ROOT, "tick %.1f ms", srv.getAverageTickTimeNanos() / 1e6));
			for (PlantBlockEntity p : all()) {
				log("  " + p.inspect().getString());
			}
			log(String.format(Locale.ROOT, "  paid: lead %.2f, zinc %.2f (lead filter zinc %.2f, zinc filter lead %.2f); plain lead %.2f, zinc %.2f ingots", paid(level, filter1, LEAD),
					paid(level, filter2, ZINC), paid(level, filter1, ZINC), paid(level, filter2, LEAD), paid(level, bfilter, LEAD), paid(level, bfilter, ZINC)));
		}
	}

	/**
	 * A metal a filter has paid, in ingots: its ingots and nuggets (nine to an ingot) in its output, its hopper and its chest. The hopper
	 * under it takes the nuggets before nine of them pack into an ingot.
	 */
	private double paid(ServerLevel level, PlantBlockEntity f, Mineral m) {
		BlockPos h = f == filter1 ? hopper1 : f == filter2 ? hopper2 : bhopper;
		Item nugget = item(m.nuggetId()), ingot = item(m.phaseOneProductId());
		int in = f.countIn(new int[] {1, 2}, new ItemStack(ingot)), nug = f.countIn(new int[] {1, 2}, new ItemStack(nugget));
		for (BlockPos at : new BlockPos[] {h, h.west()}) {
			in += count(level, at, ingot);
			nug += count(level, at, nugget);
		}
		return in + nug / 9.0;   // a chest holds far more than a film's nuggets: none carried off
	}

	private int nuggets(ServerLevel level, PlantBlockEntity f, Mineral m) {
		BlockPos h = f == filter1 ? hopper1 : f == filter2 ? hopper2 : bhopper;
		return count(level, h, item(m.nuggetId())) + count(level, h.west(), item(m.nuggetId()));
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
		return st.length() > 34 ? st.substring(0, 34) : st;
	}

	private static String setting(PlantBlockEntity tank) {
		return tank.selective() ? "SELECTIVE" : "BULK";
	}

	private static String kg(PlantBlockEntity tank, Reagent r) {
		return f1(tank.reagents().kilograms(r).doubleValue());
	}

	/** The last non-empty assay of each part's held feed: a buffer drawn and used in one tick reads empty between ticks. */
	private final Map<String, SlurryAssay> lastFeed = new java.util.HashMap<>();

	private SlurryAssay feed(PlantBlockEntity part, Product product) {
		String key = part.getBlockPos().toShortString() + product;
		if (part.back().held(product) > 1e-6) {
			lastFeed.put(key, part.back().assayOf(product));
		}
		return lastFeed.getOrDefault(key, SlurryAssay.of(Map.of()));
	}

	private static final double RUNG = dev.wildcatter.tailings.recovery.RecoveryRoute.FLOTATION.fraction().doubleValue();

	/**
	 * A bank's feed. Bank 2 draws its feed and floats it in the same tick, so its buffer reads empty between ticks: its feed is the
	 * game's own chain from bank 1's (Selective.bank's tails, then Selective.activate), as the activation tank makes it.
	 */
	private SlurryAssay bankFeed(PlantBlockEntity bank) {
		if (bank != bank2) {
			return feed(bank, Product.CONDITIONED_SLURRY);
		}
		SlurryAssay one = feed(bank1, Product.CONDITIONED_SLURRY);
		if (one.minerals().isEmpty()) {
			return one;
		}
		return Selective.activate(Selective.bank(one, 1, RUNG, 0.30).tailsAssay());
	}

	/**
	 * A bank's froth by mineral: the share of galena and of sphalerite in what it floats, from the game's own split
	 * ({@code Selective.bank}, the bank's 75 % rung to a 30 % concentrate) applied to the feed the bank holds.
	 */
	private String froth(PlantBlockEntity bank) {
		SlurryAssay in = bankFeed(bank);
		if (in.minerals().isEmpty()) {
			return "-";
		}
		SlurryAssay a = Selective.bank(in, 1, RUNG, 0.30).concentrateAssay();
		double g = a.get(LEAD.mineralItemId()), z = a.get(ZINC.mineralItemId()) + a.get(Selective.ZINC_CONCENTRATE);
		if (g + z <= 0) {
			return "-";
		}
		return String.format(Locale.ROOT, "galena %.0f %% · sphalerite %.0f %%", 100 * g / (g + z), 100 * z / (g + z));
	}

	/** What a bank's feed holds of the sphalerite: held down (zinc sulfate), or free to float (activated). */
	private String zincHeld(PlantBlockEntity bank) {
		SlurryAssay a = bankFeed(bank);
		double down = a.get(Selective.DEPRESSED), free = a.get(ZINC.mineralItemId());
		if (down + free <= 0) {
			return "-";
		}
		return down >= free ? "held down" : "free to float";
	}

	private String[] lineCard(ServerLevel level) {
		return new String[] {"^SELECTIVE FLOTATION · LEAD, THEN ZINC", "Feed: lead-zinc fines, 50 t/h at 35 %", rd(cond, "Conditioning tank", "15 kW") + " [" + setting(cond) + "]",
				rd(bank1, "Bank 1, lead", "8 x 45 kW"), rd(act, "Activation tank", "15 kW"), rd(bank2, "Bank 2, zinc", "8 x 45 kW"),
				"Lead paid: " + f1(paid(level, filter1, LEAD)) + " ingots' worth", "Zinc paid: " + f1(paid(level, filter2, ZINC)) + " ingots' worth"};
	}

	private String[] bankOneCard(ServerLevel level) {
		return new String[] {"^BANK 1 · THE LEAD BANK", rd(cond, "Conditioning tank", "15 kW") + " [" + setting(cond) + "]",
				"  zinc sulfate " + kg(cond, Reagent.ZINC_SULFATE) + " · lime " + kg(cond, Reagent.LIME) + " · xanthate " + kg(cond, Reagent.XANTHATE) + " kg",
				rd(bank1, "Bank 1", "8 x 45 kW"), rd(blower1, "Air blower", "75 kW"), "Its froth: " + froth(bank1), "Its feed and tails: the sphalerite " + zincHeld(bank1)};
	}

	private String[] activationCard(ServerLevel level) {
		return new String[] {"^ACTIVATION TANK · COPPER SULFATE", rd(act, "Activation tank", "15 kW"),
				"  copper sulfate " + kg(act, Reagent.COPPER_SULFATE) + " · lime " + kg(act, Reagent.LIME) + " · xanthate " + kg(act, Reagent.XANTHATE) + " kg",
				"  sphalerite in: " + zincHeld(bank1) + " · out: " + zincHeld(bank2), rd(bank2, "Bank 2", "8 x 45 kW"), rd(blower2, "Air blower", "75 kW"), "Bank 2's froth: " + froth(bank2)};
	}

	private String[] bankTwoCard(ServerLevel level) {
		return new String[] {"^BANK 2 · THE ZINC BANK", rd(act, "Activation tank", "15 kW"), rd(bank2, "Bank 2", "8 x 45 kW"), rd(blower2, "Air blower", "75 kW"),
				"Its froth: " + froth(bank2), "Its tails: on to the tailings line", "Zinc paid: " + f1(paid(level, filter2, ZINC)) + " ingots' worth"};
	}

	private String[] filtersCard(ServerLevel level) {
		return new String[] {"^TWO THICKENERS, TWO FILTERS", rd(thick1, "Lead thickener", "5.5 kW"), rd(filter1, "Lead filter", "15 kW"),
				"  lead: " + nuggets(level, filter1, LEAD) + " nuggets, " + f1(paid(level, filter1, LEAD)) + " ingots' worth", rd(thick2, "Zinc thickener", "5.5 kW"),
				rd(filter2, "Zinc filter", "15 kW"), "  zinc: " + nuggets(level, filter2, ZINC) + " nuggets, " + f1(paid(level, filter2, ZINC)) + " ingots' worth",
				"Zinc off the lead filter: " + f1(paid(level, filter1, ZINC)) + " · lead off the zinc: " + f1(paid(level, filter2, LEAD))};
	}

	private String[] bulkCard(ServerLevel level) {
		List<String> l = new ArrayList<>(List.of("^THE SAME FEED ON A PLAIN BANK", rd(bcond, "Conditioning tank", "15 kW") + " [" + setting(bcond) + "]",
				bcond.selective() ? "  zinc sulfate " + kg(bcond, Reagent.ZINC_SULFATE) + " kg in the tank" : "  copper sulfate: the zinc floats with the lead",
				rd(bbank, "Bank", "8 x 45 kW"), rd(bfilter, "Pressure filter", "15 kW"), "Lead paid: " + f1(paid(level, bfilter, LEAD)) + " ingots' worth",
				(paid(level, bfilter, ZINC) > 0 ? "+" : "!") + "Zinc paid: " + f1(paid(level, bfilter, ZINC)) + " ingots' worth"));
		if (!note.isEmpty()) {
			l.add(note);
		}
		return l.toArray(String[]::new);
	}

	// ---- the films ----------------------------------------------------------------------------------------------------------------

	/** The selective line whole, from the tank down both banks to the two filters. */
	private void lineFilm() {
		if (!wanted("rtm06_line")) {
			return;
		}
		showCard(true);
		Cam a = scam(-18, 18, -12, 4, 2, 12);
		Cam b = scam(-20, 17, 28, 4, 2, 28);
		Cam e = scam(28, 19, 64, 7, 2, 38);
		film("rtm06_line", 1300, via(a, b, e, 1300), (level, t) -> card(lineCard(level)));
		showCard(false);
	}

	/** Bank 1's froth close, over its rougher cells. */
	private void frothFilm() {
		if (!wanted("rtm06_froth_close")) {
			return;
		}
		showCard(true);
		Cam a = scam(1.0, 8.0, 5.5, 5.25, 3.0, 10.0);
		Cam b = scam(2.0, 7.2, 9.5, 5.25, 3.0, 13.5);
		film("rtm06_froth_close", 800, move(a, b, 800), (level, t) -> card(bankOneCard(level)));
		showCard(false);
	}

	private void bankOneFilm() {
		if (!wanted("rtm06_bank_one")) {
			return;
		}
		showCard(true);
		Cam a = scam(-9, 8, 0, 1.5, 3.5, 6);
		Cam b = scam(-9, 7, 16, 2, 3.0, 14);
		film("rtm06_bank_one", 900, move(a, b, 900), (level, t) -> card(bankOneCard(level)));
		showCard(false);
	}

	/** The activation tank, its hopper and reagent chest, then down to bank 2. */
	private void activationFilm() {
		if (!wanted("rtm06_activation")) {
			return;
		}
		showCard(true);
		Cam a = scam(-6, 8.5, 17, 1.5, 4.0, 22.5);
		Cam b = scam(-7, 8, 27, 2, 3.0, 25.5);
		film("rtm06_activation", 900, move(a, b, 900), (level, t) -> card(activationCard(level)));
		showCard(false);
	}

	private void bankTwoFilm() {
		if (!wanted("rtm06_bank_two")) {
			return;
		}
		showCard(true);
		Cam a = scam(-10, 9, 24, 2, 3.0, 30);
		Cam b = scam(-9, 7.5, 40, 3, 3.0, 33);
		film("rtm06_bank_two", 800, move(a, b, 800), (level, t) -> card(bankTwoCard(level)));
		showCard(false);
	}

	/** Both thickeners and filters from the east, the nuggets into each hopper and chest. */
	private void filtersFilm() {
		if (!wanted("rtm06_filters")) {
			return;
		}
		showCard(true);
		Cam a = scam(26, 11, 20, 10, 1.5, 31);
		Cam b = scam(24, 9, 54, 7, 1.5, 47);
		film("rtm06_filters", 1000, move(a, b, 1000), (level, t) -> card(filtersCard(level)));
		showCard(false);
	}

	/** The plain line on BULK: the lead pays, the zinc does not. */
	private void bulkFilm() {
		if (!wanted("rtm06_bulk")) {
			return;
		}
		showCard(true);
		Cam a = rcam(-16, 15, -10, 3, 2, 10);
		Cam b = rcam(-14, 12, 26, 4, 2, 16);
		Cam e = rcam(18, 14, 40, 6, 2, 22);
		film("rtm06_bulk", 1100, via(a, b, e, 1100), (level, t) -> card(bulkCard(level)));
		showCard(false);
	}

	private void setsFilm() {
		if (!wanted("rtm06_sets")) {
			return;
		}
		showCard(true);
		Cam a = scam(-50, 16, -12, -30, 2, 20);
		Cam b = scam(-56, 18, 66, -30, 2, 36);
		film("rtm06_sets", 700, move(a, b, 700), (level, t) -> {
			List<String> l = new ArrayList<>(List.of("^THE SETS: LOAD OF RATING"));
			String[] names = {"Bank 1 (gas)", "Bank 2 (gas)", "Selective line's other motors (diesel)", "Plain bank (gas)", "Plain line's other motors (diesel)"};
			for (int i = 0; i < panels.size() && i < names.length; i++) {
				String s = ((GensetControlPanelBlockEntity) level.getBlockEntity(panels.get(i))).inspect().getString();
				java.util.regex.Matcher m = java.util.regex.Pattern.compile("\\((\\d+) kW.*?(\\d+) kW \\((\\d+)%\\)").matcher(s);
				l.add(names[i] + ": " + (m.find() ? m.group(2) + " of " + m.group(1) + " kW" : "?"));
			}
			card(l.toArray(String[]::new));
		});
		showCard(false);
	}

	/**
	 * The plain line's tank set to SELECTIVE (a sneaking click in play) with no zinc sulfate in it: NO REAGENT, and nothing goes on to the
	 * bank. Then zinc sulfate into its reagent chest, through the hopper, and it conditions again, holding the zinc down.
	 */
	private void switchFilm() {
		if (!wanted("rtm06_switch")) {
			return;
		}
		showCard(true);
		Cam a = rcam(-7, 8, 0, 1.5, 4, 4.5);
		Cam b = rcam(-8, 7, 6, 1, 3, 5);
		int[] since = {-1, -1};
		film("rtm06_switch", 1300, move(a, b, 1300), (level, t) -> {
			int s = t - PAD;
			if (s == 100) {
				bcond.toggleSelective();
				note = "+Setting changed: SELECTIVE";
				event("plain tank set to " + setting(bcond) + "; zinc sulfate " + kg(bcond, Reagent.ZINC_SULFATE) + " kg");
			}
			if (s > 100 && since[0] < 0 && bcond.status().startsWith("NO REAGENT")) {
				since[0] = s;
				note = "!Tank: no slurry on to the bank";
				event("plain tank: " + bcond.status() + "; bank " + bbank.status());
			}
			if (since[0] >= 0 && s == since[0] + 260) {
				zincSulfateToPlain = true;
				if (level.getBlockEntity(bcondChest) instanceof Container c) {
					c.setItem(c.getContainerSize() - 1, new ItemStack(reagent(Reagent.ZINC_SULFATE), 12));
				}
				note = "+Zinc sulfate into its chest";
				event("zinc sulfate into the plain tank's chest");
			}
			if (since[0] >= 0 && since[1] < 0 && zincSulfateToPlain && bcond.status().startsWith("conditioning")) {
				since[1] = s;
				event("plain tank: " + bcond.status());
			}
			card(bulkCard(level));
		});
		note = "";
		showCard(false);
	}
}
