package dev.wildcatter.tailings.fabric.gametest;

import dev.wildcatter.block.BigPartBlock;
import dev.wildcatter.multiblock.BigParts;
import dev.wildcatter.tailings.TailingsBlocks;
import dev.wildcatter.tailings.TailingsEntities;
import dev.wildcatter.tailings.TailingsItems;
import dev.wildcatter.tailings.block.BeltConveyorBlock;
import dev.wildcatter.tailings.block.DraglineBlockEntity;
import dev.wildcatter.tailings.block.DrillBlockEntity;
import dev.wildcatter.tailings.block.MineRailBlock;
import dev.wildcatter.tailings.block.MuckBlockEntity;
import dev.wildcatter.tailings.block.PitSwitchHouseBlockEntity;
import dev.wildcatter.tailings.block.PlantBlockEntity;
import dev.wildcatter.tailings.block.RectifierBlockEntity;
import dev.wildcatter.tailings.block.RopeShovelBlockEntity;
import dev.wildcatter.tailings.block.ShaftLayout;
import dev.wildcatter.tailings.entity.DraglineEntity;
import dev.wildcatter.tailings.entity.GantryEntity;
import dev.wildcatter.tailings.entity.MineVehicleEntity;
import dev.wildcatter.tailings.entity.RopeShovelEntity;
import dev.wildcatter.tailings.entity.ShaftSkipEntity;
import dev.wildcatter.tailings.entity.TbmEntity;
import dev.wildcatter.tailings.entity.UltraHaulTruckEntity;
import dev.wildcatter.tailings.item.TrailingCableItem;
import dev.wildcatter.tailings.mine.Heading;
import dev.wildcatter.tailings.mine.TbmGantry;
import dev.wildcatter.tailings.pit.DraglineLayout;
import dev.wildcatter.tailings.pit.RopeShovelLayout;
import dev.wildcatter.tailings.plant.Machine;
import dev.wildcatter.tailings.rock.Rock;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.fabric.api.client.gametest.v1.screenshot.TestScreenshotOptions;
import net.minecraft.client.CloudStatus;
import net.minecraft.client.GraphicsPreset;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ParticleStatus;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * The trailer's shots (TAILINGS_ONLY=trailer): the mod's machines staged on a real generated world at golden hour, each shot a still
 * at the start and the end of a short camera move (1920 x 1080, no HUD) and a film of the move. The sites are levelled pads blended
 * into the seed's own ground; every block and machine is the mod's or vanilla's, built the way the scenes in
 * {@link TailingsClientGameTest} build them. TAILINGS_TRAILER_SHOTS=1,3 runs only those shots; the stills go to
 * TAILINGS_TRAILER_OUT (default: the run folder's {@code trailer}), the films to {@code fabric/build/scene-videos/trailer_*.mov}.
 */
final class Trailer {
	private static final String SEED = System.getenv().getOrDefault("TAILINGS_TRAILER_SEED", "tailings");
	private static final Path OUT = Path.of(System.getenv().getOrDefault("TAILINGS_TRAILER_OUT", "trailer")).toAbsolutePath();
	private static final int W = 1920, H = 1080;
	/** Ticks of each filmed camera move. */
	private static final int MOVE = 180;

	private final ClientGameTestContext context;
	private TestServerContext server;

	Trailer(ClientGameTestContext context) {
		this.context = context;
	}

	private static void log(String line) {
		System.out.println("[tailings-trailer] " + line);
	}

	private static boolean wanted(int shot) {
		String only = System.getenv("TAILINGS_TRAILER_SHOTS");
		return only == null || only.isBlank() || java.util.Arrays.asList(only.split(",")).contains(Integer.toString(shot));
	}

	void run() {
		context.runOnClient(client -> {
			client.options.renderDistance().set(20);
			client.options.graphicsPreset().set(GraphicsPreset.FANCY);
			client.options.particles().set(ParticleStatus.ALL);
			client.options.cloudStatus().set(CloudStatus.FANCY);
			client.options.entityDistanceScaling().set(3.0);
			client.options.fov().set(70);
			client.options.chatVisibility().set(net.minecraft.world.entity.player.ChatVisiblity.HIDDEN);
		});
		try (TestSingleplayerContext world = context.worldBuilder().setUseConsistentSettings(false).adjustSettings(s -> {
			s.setSeed(SEED);
			s.setGameMode(WorldCreationUiState.SelectedGameMode.CREATIVE);
		}).create()) {
			server = world.getServer();
			context.waitTicks(40);
			server.runCommand("gamerule advance_time false");
			server.runCommand("gamerule advance_weather false");
			server.runCommand("weather clear");
			server.runCommand("difficulty peaceful");
			server.runCommand("gamemode spectator @p");
			server.runCommand("tp @p 0 160 0");
			context.runOnClient(client -> {
				if (!client.gui.hud.isHidden()) {
					client.gui.hud.toggle();
				}
			});
			context.waitTicks(20);
			List<String> failed = new ArrayList<>();
			Runnable[] shots = {this::tbmFace, this::tbmPortal, this::openPit, this::crusher, this::headframe};
			for (int i = 0; i < shots.length; i++) {
				if (!wanted(i + 1)) {
					continue;
				}
				try {
					shots[i].run();
				} catch (RuntimeException | AssertionError ex) {
					log("shot " + (i + 1) + " failed: " + ex);
					ex.printStackTrace();
					failed.add(Integer.toString(i + 1));
				}
			}
			log("done; failed shots " + failed);
		}
	}

	// ---- the camera ----

	/** Where the camera stands and the point it looks at. */
	private record Cam(double x, double y, double z, double tx, double ty, double tz) {
		Cam lerp(Cam o, double t) {
			return new Cam(x + (o.x - x) * t, y + (o.y - y) * t, z + (o.z - z) * t, tx + (o.tx - tx) * t, ty + (o.ty - ty) * t, tz + (o.tz - tz) * t);
		}

		static Cam of(BlockPos o, double x, double y, double z, double tx, double ty, double tz) {
			return new Cam(o.getX() + x, o.getY() + y, o.getZ() + z, o.getX() + tx, o.getY() + ty, o.getZ() + tz);
		}
	}

	private void put(Cam c) {
		double dx = c.tx - c.x, dy = c.ty - c.y - 1.62, dz = c.tz - c.z;
		double yaw = Math.toDegrees(Math.atan2(-dx, dz));
		double pitch = -Math.toDegrees(Math.atan2(dy, Math.sqrt(dx * dx + dz * dz)));
		server.runCommand(String.format(Locale.ROOT, "tp @p %.3f %.3f %.3f %.2f %.2f", c.x, c.y, c.z, yaw, pitch));
	}

	private void settle(int ticks) {
		context.waitTicks(ticks);
		try {
			context.waitFor(client -> client.levelRenderer.hasRenderedAllSections(), 400);
		} catch (RuntimeException | AssertionError ex) {
			log("sections still compiling: " + ex.getMessage());
		}
		context.waitTicks(10);
	}

	private void still(String name) {
		server.runCommand("kill @e[type=item]");
		context.waitTicks(2);
		Path at = context.takeScreenshot(TestScreenshotOptions.of(name).withSize(W, H).withDestinationDir(OUT).disableCounterPrefix());
		log("still " + at);
	}

	/**
	 * A shot: the start still, the filmed move from {@code a} to {@code b} (eased), the end still. {@code each} runs every second of
	 * the move, to keep the machines fed.
	 */
	private void shot(String id, Cam a, Cam b, Runnable each) {
		put(a);
		settle(60);
		still(id + "_a");
		Filming film = context.computeOnClient(client -> Filming.start(client, "trailer_" + id));
		for (int t = 0; t <= MOVE; t++) {
			double s = (double) t / MOVE;
			put(a.lerp(b, s * s * (3 - 2 * s)));
			context.waitTick();
			if (t % 20 == 0) {
				server.runCommand("kill @e[type=item]");
			}
			if (each != null && t % 20 == 0) {
				each.run();
			}
		}
		film.stop();
		settle(5);
		still(id + "_b");
	}

	// ---- the ground ----

	private static boolean loose(BlockState s) {
		return s.isAir() || s.is(BlockTags.LOGS) || s.is(BlockTags.LEAVES) || (s.canBeReplaced() && s.getFluidState().isEmpty()) || s.is(Blocks.SNOW)
				|| s.is(Blocks.BAMBOO) || s.is(Blocks.CACTUS) || s.is(Blocks.SUGAR_CANE) || s.is(Blocks.PUMPKIN) || s.is(Blocks.MELON) || s.is(BlockTags.FLOWERS);
	}

	/** The first free block over the natural ground at a column: trees and plants not counted. */
	private static int surface(ServerLevel level, int x, int z) {
		int y = level.getHeight(Heightmap.Types.WORLD_SURFACE, x, z);
		BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos(x, y - 1, z);
		while (p.getY() > level.getMinY() && loose(level.getBlockState(p))) {
			p.move(0, -1, 0);
		}
		return p.getY() + 1;
	}

	private static boolean wet(ServerLevel level, int x, int z) {
		return !level.getBlockState(new BlockPos(x, surface(level, x, z) - 1, z)).getFluidState().isEmpty();
	}

	/**
	 * A dry, fairly even site near (x, z) for a w by d pad, searched outward in rings: its corner at the ground's middle height.
	 */
	private static BlockPos site(ServerLevel level, int x, int z, int w, int d) {
		BlockPos best = null;
		double bestScore = Double.MAX_VALUE;
		for (int ring = 0; ring <= 6 && (best == null || bestScore > 4); ring++) {
			for (int i = -ring; i <= ring; i++) {
				for (int j = -ring; j <= ring; j++) {
					if (Math.max(Math.abs(i), Math.abs(j)) != ring) {
						continue;
					}
					int cx = x + i * 64, cz = z + j * 64;
					List<Integer> hs = new ArrayList<>();
					boolean dry = true;
					for (int a = 0; a <= 4 && dry; a++) {
						for (int b = 0; b <= 4 && dry; b++) {
							int px = cx + a * w / 4, pz = cz + b * d / 4;
							if (wet(level, px, pz)) {
								dry = false;
							}
							hs.add(surface(level, px, pz));
						}
					}
					if (!dry) {
						continue;
					}
					hs.sort(Comparator.naturalOrder());
					int mid = hs.get(hs.size() / 2);
					if (mid < level.getSeaLevel() + 2 || mid > 110) {
						continue;
					}
					double score = hs.get(hs.size() - 2) - hs.get(1);
					if (score < bestScore) {
						bestScore = score;
						best = new BlockPos(cx, mid, cz);
					}
				}
			}
		}
		if (best == null) {
			best = new BlockPos(x, Math.max(surface(level, x, z), level.getSeaLevel() + 2), z);
		}
		log("site near " + x + "," + z + ": " + best.toShortString() + ", unevenness " + bestScore);
		return best;
	}

	private static void forceChunks(ServerLevel level, int x0, int z0, int x1, int z1) {
		for (int cx = Math.floorDiv(x0, 16); cx <= Math.floorDiv(x1, 16); cx++) {
			for (int cz = Math.floorDiv(z0, 16); cz <= Math.floorDiv(z1, 16); cz++) {
				level.setChunkForced(cx, cz, true);
			}
		}
	}

	/**
	 * Levels the rectangle to ground at {@code g} (the first air), grass over dirt over stone, cleared {@code clear} above; a ring
	 * {@code blend} wide around it eases back to the natural ground.
	 */
	private static void pad(ServerLevel level, int x0, int z0, int x1, int z1, int g, int blend, int clear) {
		forceChunks(level, x0 - blend, z0 - blend, x1 + blend, z1 + blend);
		for (int x = x0 - blend; x <= x1 + blend; x++) {
			for (int z = z0 - blend; z <= z1 + blend; z++) {
				int d = Math.max(Math.max(x0 - x, x - x1), Math.max(z0 - z, z - z1));
				int nat = surface(level, x, z);
				int h = g;
				if (d > 0) {
					double t = (double) d / (blend + 1);
					t = t * t * (3 - 2 * t);
					h = (int) Math.round(g + (nat - g) * t);
				}
				column(level, x, z, h, Math.min(nat, h) - 4, Math.max(nat, h) + (d > 0 ? 24 : clear));
			}
		}
	}

	/** Ground up to h (grass on top) from {@code from}, air from h to {@code to}. */
	private static void column(ServerLevel level, int x, int z, int h, int from, int to) {
		BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
		for (int yy = from; yy < h; yy++) {
			BlockState s = yy == h - 1 ? Blocks.GRASS_BLOCK.defaultBlockState() : yy >= h - 4 ? Blocks.DIRT.defaultBlockState() : Blocks.STONE.defaultBlockState();
			level.setBlock(p.set(x, yy, z), s, Block.UPDATE_CLIENTS);
		}
		for (int yy = h; yy <= to; yy++) {
			if (!level.getBlockState(p.set(x, yy, z)).isAir()) {
				level.setBlock(p, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
			}
		}
	}

	/** A rock hill: ground to {@code g + height(x, z)} where that is above g, stone under a skin of dirt and grass. */
	private interface Height {
		int at(int x, int z);
	}

	private static void hill(ServerLevel level, int x0, int z0, int x1, int z1, int g, Height height) {
		hill(level, x0, z0, x1, z1, g, height, false);
	}

	/** As above; {@code quarry}: bare stone with gravel on its treads, a worked bench rather than a grassy hill. */
	private static void hill(ServerLevel level, int x0, int z0, int x1, int z1, int g, Height height, boolean quarry) {
		BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
		for (int x = x0; x <= x1; x++) {
			for (int z = z0; z <= z1; z++) {
				int h = height.at(x, z);
				if (h <= 0) {
					continue;
				}
				for (int yy = g - 1; yy < g + h; yy++) {
					BlockState s = quarry ? (yy == g + h - 1 && Math.floorMod(x * 31 + z * 17, 3) == 0 ? Blocks.GRAVEL : Math.floorMod(x * 7 + yy * 13 + z * 5, 9) == 0 ? Blocks.ANDESITE : Blocks.STONE).defaultBlockState()
							: yy == g + h - 1 ? Blocks.GRASS_BLOCK.defaultBlockState() : yy >= g + h - 3 ? Blocks.DIRT.defaultBlockState() : Blocks.STONE.defaultBlockState();
					level.setBlock(p.set(x, yy, z), s, Block.UPDATE_CLIENTS);
				}
			}
		}
	}

	private static void time(TestServerContext server, int t) {
		server.runCommand("time set " + t);
	}

	// ---- the mod's parts ----

	private static void part(ServerLevel level, Block block, BlockPos at) {
		BlockState state = block.defaultBlockState().setValue(BigPartBlock.FACING, Direction.NORTH);
		level.setBlock(at, state, Block.UPDATE_ALL);
		BigParts.grow(level, at, level.getBlockState(at));
	}

	private static PlantBlockEntity plant(ServerLevel level, Machine machine, BlockPos at) {
		part(level, TailingsBlocks.plantBlock(machine), at);
		return (PlantBlockEntity) level.getBlockEntity(at);
	}

	private static void pipe(ServerLevel level, BlockPos... at) {
		Block steel = BuiltInRegistries.BLOCK.getValue(Identifier.fromNamespaceAndPath("wildcatter", "gas_line"));
		for (BlockPos pos : at) {
			level.setBlock(pos, steel.defaultBlockState(), Block.UPDATE_ALL);
		}
	}

	private static BlockState heavyBelt(Direction.Axis axis) {
		return TailingsBlocks.HEAVY_BELT_CONVEYOR.defaultBlockState().setValue(BeltConveyorBlock.AXIS, axis);
	}

	private static void fillBox(ServerLevel level, BlockPos o, Block block, int x0, int x1, int y0, int y1, int z0, int z1) {
		for (int x = x0; x <= x1; x++) {
			for (int z = z0; z <= z1; z++) {
				for (int yy = y0; yy <= y1; yy++) {
					level.setBlock(o.offset(x, yy, z), block.defaultBlockState(), Block.UPDATE_CLIENTS);
				}
			}
		}
	}

	/**
	 * The TBM as the drive scene builds it, its cutterhead at c heading north into the rock, its gantries and track behind, a car at
	 * the loading row; started. Returns the cab's position.
	 */
	private static BlockPos tbm(ServerLevel level, BlockPos c, int ticksPerAdvance) {
		BlockPos beam = c.offset(3, 3, 3);
		part(level, TailingsBlocks.CUTTERHEAD, c);
		part(level, TailingsBlocks.SEGMENT_ERECTOR, c.offset(0, 0, 1));
		part(level, TailingsBlocks.MAIN_BEAM, beam);
		part(level, TailingsBlocks.TRACK_LAYER, c.offset(1, 2, 8));
		level.setBlock(c.offset(-2, 0, 14), TailingsBlocks.CABLE_JUNCTION.defaultBlockState(), Block.UPDATE_ALL);
		BlockState track = TailingsBlocks.MINE_RAIL.defaultBlockState().setValue(MineRailBlock.FACING, Direction.NORTH);
		for (int z = 11; z <= 40; z++) {
			level.setBlock(c.offset(2, 1, z), track, Block.UPDATE_ALL);
			level.setBlock(c.offset(5, 1, z), track, Block.UPDATE_ALL);
			level.setBlock(c.offset(2, 0, z), Blocks.GRAVEL.defaultBlockState(), Block.UPDATE_ALL);
			level.setBlock(c.offset(5, 0, z), Blocks.GRAVEL.defaultBlockState(), Block.UPDATE_ALL);
		}
		DrillBlockEntity cab = (DrillBlockEntity) level.getBlockEntity(beam);
		cab.forceRunning();
		cab.program().setTargetY(c.getY() + 1);
		cab.program().setFlatLength(60);
		cab.setItem(DrillBlockEntity.SLOT_RAIL, new ItemStack(TailingsBlocks.MINE_RAIL, 64));
		cab.setItem(DrillBlockEntity.SLOT_GRAVEL, new ItemStack(Items.GRAVEL, 64));
		cab.setItem(DrillBlockEntity.SLOT_COBBLE, new ItemStack(Items.COBBLESTONE, 64));
		cab.setItem(DrillBlockEntity.SLOT_SEGMENTS, new ItemStack(TailingsBlocks.LINING, 64));
		cab.core().setTestTicks(ticksPerAdvance);
		for (TbmGantry kind : TbmGantry.values()) {
			GantryEntity gantry = GantryEntity.place(level, c.offset(2, 1, kind.row()), kind, Direction.NORTH);
			log(kind.displayName() + ": " + gantry.couple());
		}
		MineVehicleEntity.placeCar(level, c.offset(2, 1, 28), Heading.NORTH);
		return beam;
	}

	private static void feedTbm(ServerLevel level, BlockPos c) {
		for (TbmEntity machine : level.getEntitiesOfClass(TbmEntity.class, new AABB(c).inflate(90))) {
			machine.setItem(DrillBlockEntity.SLOT_SEGMENTS, new ItemStack(TailingsBlocks.LINING, 64));
			machine.setItem(DrillBlockEntity.SLOT_RAIL, new ItemStack(TailingsBlocks.MINE_RAIL, 64));
			machine.setItem(DrillBlockEntity.SLOT_GRAVEL, new ItemStack(Items.GRAVEL, 64));
		}
	}

	/** The front of the running TBM: the least z of its machine entities, or c's z before it lifts. */
	private static double tbmFront(ServerLevel level, BlockPos c) {
		double z = c.getZ();
		for (TbmEntity machine : level.getEntitiesOfClass(TbmEntity.class, new AABB(c).inflate(90))) {
			z = Math.min(z, machine.getZ());
		}
		return z;
	}

	/** Lets the TBM bore until its front is {@code blocks} into the rock (or the time runs out), feeding it and clearing the cut. */
	private void bore(BlockPos c, int blocks, int maxSeconds, boolean section) {
		for (int s = 0; s < maxSeconds; s++) {
			context.waitTicks(20);
			double front = server.computeOnServer(srv -> {
				ServerLevel level = srv.overworld();
				feedTbm(level, c);
				if (section) {
					cutAway(level, c);
				}
				return tbmFront(level, c);
			});
			if (s % 10 == 0) {
				server.runOnServer(srv -> {
					for (TbmEntity machine : srv.overworld().getEntitiesOfClass(TbmEntity.class, new AABB(c).inflate(90))) {
						log("tbm at " + machine.position() + ": " + machine.core().readout());
					}
				});
			}
			if (c.getZ() - front >= blocks) {
				log("tbm bored " + (c.getZ() - front) + " in " + s + " s");
				return;
			}
		}
		log("tbm still short after " + maxSeconds + " s");
	}

	/** The section's open side: rock and lining east of the bore, cleared so the machine shows. */
	private static void cutAway(ServerLevel level, BlockPos c) {
		for (int z = -60; z <= 12; z++) {
			for (int x = 7; x < 10; x++) {
				for (int yy = -2; yy < 10; yy++) {
					BlockPos at = c.offset(x, yy, z);
					BlockState there = level.getBlockState(at);
					if (there.is(Blocks.STONE) || there.is(TailingsBlocks.LINING) || there.is(Blocks.DIRT) || there.is(Blocks.GRASS_BLOCK)) {
						level.setBlock(at, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
					}
				}
			}
		}
	}

	// ---- 1. the TBM at the face ----

	/**
	 * The TBM boring into a rock ridge cut open on its east side, the morning sun on the cut: the cutterhead in the face, the machine
	 * and its gantries behind it in the lined bore.
	 */
	private void tbmFace() {
		BlockPos[] at = new BlockPos[1];
		server.runOnServer(srv -> {
			ServerLevel level = srv.overworld();
			BlockPos s = site(level, 400, -200, 90, 120);
			int g = s.getY();
			BlockPos c = new BlockPos(s.getX() + 40, g, s.getZ() + 80);
			pad(level, c.getX() - 40, c.getZ() - 80, c.getX() + 50, c.getZ() + 45, g, 14, 40);
			// a ridge west of the cut, the bore's rock up to its east face at x + 6
			hill(level, c.getX() - 40, c.getZ() - 80, c.getX() + 6, c.getZ() - 1, g, (x, z) -> {
				int fromSouth = c.getZ() - 1 - z;
				int fromWest = x - (c.getX() - 40);
				int fromNorth = z - (c.getZ() - 80);
				int h = Math.min(16, Math.min(6 + fromSouth * 3, Math.min(fromWest, fromNorth)));
				if (x >= c.getX() - 2) {
					h = Math.max(h, Math.min(12, 4 + fromSouth * 4));
				}
				return h;
			});
			// the cut's floor east of the bore: a bench of gravel the camera can stand over
			for (int x = c.getX() + 7; x <= c.getX() + 30; x++) {
				for (int z = c.getZ() - 70; z <= c.getZ() + 2; z++) {
					level.setBlock(new BlockPos(x, g - 1, z), Blocks.GRAVEL.defaultBlockState(), Block.UPDATE_CLIENTS);
				}
			}
			tbm(level, c, 30);
			at[0] = c;
		});
		BlockPos c = at[0];
		time(server, 23950);
		put(Cam.of(c, 18, 9, 10, 3, 3, -4));
		settle(80);
		server.runOnServer(srv -> ((DrillBlockEntity) srv.overworld().getBlockEntity(c.offset(3, 3, 3))).press(10, null));
		bore(c, 9, 150, true);
		double front = server.computeOnServer(srv -> tbmFront(srv.overworld(), c));
		int f = (int) Math.round(front) - c.getZ();   // the cutterhead's z now, from c
		log("tbm_face: front at " + f);
		Runnable keep = () -> server.runOnServer(srv -> {
			feedTbm(srv.overworld(), c);
			cutAway(srv.overworld(), c);
		});
		// low on the cut's floor, just behind the cutterhead, pushing in along the machine to the face
		shot("01_tbm_face", Cam.of(c, 26, 8, f + 14, 3, 3, f - 1), Cam.of(c, 21, 5, f + 6, 3, 3, f - 2), keep);
	}

	// ---- 2. the portal ----

	/** The tunnel's portal in a grassy ridge: the lined bore going in, its track and a loaded train coming out under the lights. */
	private void tbmPortal() {
		BlockPos[] at = new BlockPos[1];
		server.runOnServer(srv -> {
			ServerLevel level = srv.overworld();
			BlockPos s = site(level, -300, -300, 100, 140);
			int g = s.getY();
			BlockPos c = new BlockPos(s.getX() + 48, g, s.getZ() + 80);
			pad(level, c.getX() - 48, c.getZ() - 80, c.getX() + 52, c.getZ() + 60, g, 16, 40);
			// a ridge north of the pad, its face cut steep round the portal
			hill(level, c.getX() - 48, c.getZ() - 80, c.getX() + 52, c.getZ() + 6, g, (x, z) -> {
				int fromWest = x - (c.getX() - 48), fromEast = c.getX() + 52 - x, fromNorth = z - (c.getZ() - 80);
				int band = x >= c.getX() - 4 && x <= c.getX() + 11 ? 0 : Math.min(Math.abs(x - (c.getX() - 4)), Math.abs(x - (c.getX() + 11)));
				int fromFace = c.getZ() - 1 - z;   // the portal's face at c.z - 1
				int face = band == 0 ? (fromFace < 0 ? 0 : 13 + fromFace / 2) : 13 + fromFace / 2 + band / 2 - Math.max(0, -fromFace) * 2;
				return Math.min(Math.min(20, face), Math.min(fromWest, Math.min(fromEast, fromNorth)) / 2 + 2);
			});
			for (int x = c.getX() - 2; x <= c.getX() + 9; x++) {
				for (int z = c.getZ() - 60; z <= c.getZ() - 1; z++) {
					for (int yy = -1; yy < 11; yy++) {
						BlockPos p = new BlockPos(x, g + yy, z);
						if (level.getBlockState(p).isAir() || level.getBlockState(p).is(Blocks.GRASS_BLOCK) || level.getBlockState(p).is(Blocks.DIRT)) {
							level.setBlock(p, Blocks.STONE.defaultBlockState(), Block.UPDATE_CLIENTS);
						}
					}
				}
			}
			tbm(level, c, 12);
			// the haulage track on south from the machine's, a rectifier on it and lights along it
			BlockState track = TailingsBlocks.MINE_RAIL.defaultBlockState().setValue(MineRailBlock.FACING, Direction.NORTH);
			for (int z = 41; z <= 70; z++) {
				level.setBlock(c.offset(2, 0, z), Blocks.GRAVEL.defaultBlockState(), Block.UPDATE_ALL);
				level.setBlock(c.offset(2, 1, z), track, Block.UPDATE_ALL);
			}
			part(level, TailingsBlocks.TRACTION_RECTIFIER, c.offset(-1, 1, 62));
			((RectifierBlockEntity) level.getBlockEntity(c.offset(-1, 1, 62))).forceRunning();
			at[0] = c;
		});
		BlockPos c = at[0];
		time(server, 23950);
		put(Cam.of(c, 4, 6, 50, 4, 4, 0));
		settle(80);
		server.runOnServer(srv -> ((DrillBlockEntity) srv.overworld().getBlockEntity(c.offset(3, 3, 3))).press(10, null));
		bore(c, 22, 240, false);
		// a loaded train out of the portal toward the camera
		MineVehicleEntity[] train = new MineVehicleEntity[1];
		server.runOnServer(srv -> {
			ServerLevel level = srv.overworld();
			train[0] = MineVehicleEntity.placeLocomotive(level, c.offset(2, 1, 52), Heading.SOUTH);
			for (int i = 1; i <= 3; i++) {
				MineVehicleEntity car = MineVehicleEntity.placeCar(level, c.offset(2, 1, 52 - 3 - (int) ((i - 1) * 2.5)), Heading.SOUTH);
				car.load().add(i == 2 ? "minecraft:copper_ore" : "minecraft:cobblestone", 64);
				car.refreshLoad();
			}
		});
		Runnable keep = () -> server.runOnServer(srv -> feedTbm(srv.overworld(), c));
		// low by the track south of the portal, craning up and back as the train comes out
		shot("02_tbm_portal", Cam.of(c, 11, 2.5, 68, 3, 6, 0), Cam.of(c, 9, 4.5, 60, 3, 6, 0), keep);
	}

	// ---- 3. the open pit ----

	/**
	 * A benched open pit cut into the seed's ground: on its floor the rope shovel loads an ultra truck from a muck pile; on the west rim
	 * the walking dragline strips its first cut and casts into the pit. From high on the east rim.
	 */
	private void openPit() {
		BlockPos[] at = new BlockPos[1];
		Object[] v = new Object[5];
		server.runOnServer(srv -> {
			ServerLevel level = srv.overworld();
			BlockPos s = site(level, 300, 400, 170, 160);
			int g = s.getY();
			BlockPos b = new BlockPos(s.getX() + 20, g, s.getZ() + 90);   // the dragline scene's origin: b + (x, 0, z) is the rim's ground
			pad(level, b.getX() - 30, b.getZ() - 95, b.getX() + 150, b.getZ() + 50, g, 18, 45);
			// the pit: benches 5 deep and 6 wide, x 52..136, z -64..34, its floor 20 down
			int px0 = 52, px1 = 136, pz0 = -64, pz1 = 34;
			for (int x = px0; x <= px1; x++) {
				for (int z = pz0; z <= pz1; z++) {
					int in = Math.min(Math.min(x - px0, px1 - x), Math.min(z - pz0, pz1 - z));
					int depth = Math.min(4, in / 6 + 1) * 5;
					for (int yy = -depth; yy < 0; yy++) {
						level.setBlock(b.offset(x, yy, z), Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
					}
					// the bench faces show their rock: stone floors, a little gravel on the haul benches
					level.setBlock(b.offset(x, -depth - 1, z), ((x * 7 + z * 13) % 5 == 0 ? Blocks.GRAVEL : Blocks.STONE).defaultBlockState(), Block.UPDATE_CLIENTS);
					for (int yy = -depth - 6; yy < -depth - 1; yy++) {
						level.setBlock(b.offset(x, yy, z), Blocks.STONE.defaultBlockState(), Block.UPDATE_CLIENTS);
					}
				}
			}
			// the dragline's strip on the west rim, as its scene lays it: grass and dirt over a bauxite seam
			Block bauxite = BuiltInRegistries.BLOCK.getValue(Identifier.parse("wildcatter:bauxite_ore"));
			for (int x = -20; x <= 51; x++) {
				for (int z = -85; z <= 45; z++) {
					boolean strip = x >= 40 && x <= 49 && z >= -52 && z <= 17;
					boolean dug = strip && z >= -46 && z <= 5;
					for (int yy = -10; yy <= -1; yy++) {
						BlockState st;
						if (strip && yy >= -3) {
							st = dug ? Blocks.AIR.defaultBlockState() : (yy == -1 ? Blocks.GRASS_BLOCK : Blocks.DIRT).defaultBlockState();
						} else if (yy == -5 || yy == -4) {
							st = bauxite.defaultBlockState();
						} else {
							st = (yy == -1 ? Blocks.GRASS_BLOCK : yy >= -3 ? Blocks.DIRT : Blocks.STONE).defaultBlockState();
						}
						level.setBlock(b.offset(x, yy, z), st, Block.UPDATE_CLIENTS);
					}
				}
			}
			BlockPos dl = b.offset(20, 0, 10);
			for (DraglineLayout.Part p : DraglineLayout.Part.values()) {
				int[] o = p.offset();
				part(level, TailingsBlocks.plantBlock(p.machine()), dl.offset(o[0], o[1], o[2]));
			}
			part(level, TailingsBlocks.PIT_SWITCH_HOUSE, b.offset(2, 0, 40));
			var dHouse = (PitSwitchHouseBlockEntity) level.getBlockEntity(b.offset(2, 0, 40));
			dHouse.forceSupply(true);
			ItemStack reel = new ItemStack(TailingsItems.TRAILING_CABLE);
			TrailingCableItem.pickUp(reel, dHouse);
			long[] from = TrailingCableItem.from(reel);
			var tub = (DraglineBlockEntity) level.getBlockEntity(dl);
			log("dragline cable " + tub.couple(level, BlockPos.of(from[0]), (int) from[1]));
			tub.job().set(0, 10);
			tub.job().set(1, 70);
			tub.job().set(2, 3);
			// the rope shovel on the pit floor, a muck pile against the north bench, the ultra on its right, its own switch house
			int fy = -20;
			BlockPos sh = b.offset(80, fy, -30);
			for (RopeShovelLayout.Part p : RopeShovelLayout.Part.values()) {
				int[] o = p.offset();
				part(level, TailingsBlocks.plantBlock(p.machine()), sh.offset(o[0], o[1], o[2]));
			}
			part(level, TailingsBlocks.PIT_SWITCH_HOUSE, b.offset(118, fy, 10));
			var sHouse = (PitSwitchHouseBlockEntity) level.getBlockEntity(b.offset(118, fy, 10));
			sHouse.forceSupply(true);
			ItemStack reel2 = new ItemStack(TailingsItems.TRAILING_CABLE);
			TrailingCableItem.pickUp(reel2, sHouse);
			long[] from2 = TrailingCableItem.from(reel2);
			var crawlers = (RopeShovelBlockEntity) level.getBlockEntity(sh);
			log("shovel cable " + crawlers.couple(level, BlockPos.of(from2[0]), (int) from2[1]));
			var truck = TailingsEntities.ULTRA_HAUL_TRUCK.create(level, EntitySpawnReason.COMMAND);
			truck.setPos(sh.getX() + 30.0, sh.getY(), sh.getZ() + 6.0);
			truck.setYRot(180);
			truck.setFuel(truck.tank());
			level.addFreshEntity(truck);
			v[0] = dHouse;
			v[1] = tub;
			v[2] = sHouse;
			v[3] = crawlers;
			v[4] = truck;
			at[0] = b;
		});
		BlockPos b = at[0];
		time(server, 23950);
		Cam high = Cam.of(b, 150, 34, 52, 72, -12, -16);
		put(high);
		settle(100);
		UUID[] ids = server.computeOnServer(srv -> {
			ServerLevel level = srv.overworld();
			System.out.println("[tailings-trailer] dragline outlet " + ((PitSwitchHouseBlockEntity) v[0]).close(0) + "; shovel outlet " + ((PitSwitchHouseBlockEntity) v[2]).close(0));
			var tub = (DraglineBlockEntity) v[1];
			UUID d = tub.machine();
			log("dragline: " + tub.run(level));
			if (level.getEntity(d) instanceof DraglineEntity e) {
				e.fastCycles(4);
			}
			var crawlers = (RopeShovelBlockEntity) v[3];
			UUID sh = crawlers.machine();
			log("shovel: " + crawlers.run(level));
			BlockPos s = crawlers.getBlockPos();
			for (int x = 2; x <= 11; x++) {
				for (int z = -6; z <= -2; z++) {
					for (int yy = 0; yy <= 8 - Math.max(0, -2 - z); yy++) {
						BlockPos p = s.offset(x, yy, z);
						level.setBlock(p, TailingsBlocks.MUCK.defaultBlockState(), Block.UPDATE_ALL);
						((MuckBlockEntity) level.getBlockEntity(p)).set(Map.of((x + z + yy) % 3 == 0 ? "minecraft:copper_ore" : "minecraft:cobblestone", 3));
					}
				}
			}
			if (level.getEntity(sh) instanceof RopeShovelEntity e) {
				e.fastPasses(3);
				e.setAutoHere();
			}
			return new UUID[] {d, sh};
		});
		context.waitTicks(200);
		Runnable say = () -> server.runOnServer(srv -> {
			ServerLevel level = srv.overworld();
			log("pit: dragline " + (level.getEntity(ids[0]) instanceof DraglineEntity d ? d.mode() + "; " + d.status() : "gone")
					+ "; shovel " + (level.getEntity(ids[1]) instanceof RopeShovelEntity e ? e.passes() + " passes; " + e.status() : "gone"));
		});
		say.run();
		// a slow crane down and in from high on the east rim, over the shovel loading toward the dragline beyond
		shot("03_open_pit", high, Cam.of(b, 143, 28, 46, 70, -12, -16), say);
	}

	// ---- 4. the gyratory and the flotation row ----

	/**
	 * The pit line's primary on its bench (P7, as p7_line lays it): an ultra truck tipping 363 t into the gyratory's pocket, the
	 * crusher and apron feeder running, the heavy belt carrying it off to the stockpile; beside it a row of flotation tank cells with
	 * their blower and air line, frothing.
	 */
	private void crusher() {
		BlockPos[] at = new BlockPos[1];
		Object[] v = new Object[4];
		server.runOnServer(srv -> {
			ServerLevel level = srv.overworld();
			BlockPos s = site(level, -400, 300, 110, 170);
			int g = s.getY();
			BlockPos o = new BlockPos(s.getX() + 40, g, s.getZ() + 60);
			pad(level, o.getX() - 40, o.getZ() - 60, o.getX() + 70, o.getZ() + 110, g, 16, 45);
			// the trucks' bench: a plateau 26 up north of the crusher, its sides sloping into the ground
			hill(level, o.getX() - 40, o.getZ() - 60, o.getX() + 40, o.getZ() - 1, g, (x, z) -> {
				int dx = Math.max(Math.max(o.getX() - 2 - x, x - (o.getX() + 15)), 0);
				int dz = Math.max(z - (o.getZ() - 3), 0);
				int north = z - (o.getZ() - 60);
				return Math.min(26 - Math.max(dx, dz) * 2, north);
			}, true);
			fillBox(level, o, Blocks.STONE, -2, 15, 0, 25, -40, -3);
			fillBox(level, o, Blocks.STONE, -2, 15, 0, 21, -2, -1);
			fillBox(level, o, Blocks.GRAVEL, -2, 15, 25, 25, -40, -3);
			PlantBlockEntity feeder = plant(level, Machine.PRIMARY_APRON_FEEDER, o.offset(5, 0, 3));
			plant(level, Machine.SURGE_POCKET, o.offset(3, 2, 1));
			PlantBlockEntity crusher = plant(level, Machine.GYRATORY_CRUSHER, o.offset(4, 8, 2));
			part(level, TailingsBlocks.GYRATORY_POCKET, o.offset(0, 18, 0));
			PlantBlockEntity breaker = plant(level, Machine.ROCK_BREAKER, o.offset(12, 26, 2));
			part(level, TailingsBlocks.TRUCK_DUMP_WALL, o.offset(1, 22, -2));
			int by = 0;
			for (int z = 15; z <= 75; z++) {
				if (z > 15 && (z - 15) % 3 == 0) {
					by++;
				}
				level.setBlock(o.offset(6, by, z), heavyBelt(Direction.Axis.Z), Block.UPDATE_ALL);
			}
			PlantBlockEntity stackDrive = plant(level, Machine.HEAVY_CONVEYOR_DRIVE, o.offset(7, by, 73));
			plant(level, Machine.STOCKPILE_HEAD, o.offset(5, by - 4, 76));
			BlockPos tip = o.offset(6, 30, 79);
			for (int i = 0; i < 900; i++) {
				dev.wildcatter.tailings.pit.Loose.muck(level, tip, TailingsBlocks.STOCKPILE_ROCK.defaultBlockState(), Map.of("minecraft:copper_ore", 3));
			}
			for (PlantBlockEntity part : new PlantBlockEntity[] {feeder, crusher, breaker, stackDrive}) {
				part.forceRunning();
			}
			var truck = TailingsEntities.ULTRA_HAUL_TRUCK.create(level, EntitySpawnReason.COMMAND);
			truck.setPos(o.getX() + 7.0, o.getY() + 26, o.getZ() - 3.0 - truck.spec().length() / 2);
			truck.setYRot(180);
			truck.setFuel(truck.tank());
			truck.load().add("minecraft:copper_ore", 363_000);
			truck.refreshLoad();
			level.addFreshEntity(truck);
			// the flotation row east of the belt, as p7_flotation lays it: cyclone feed, the conditioning tank, eight tank cells, the blower
			BlockPos f = o.offset(24, 0, 4);
			Block tubing = BuiltInRegistries.BLOCK.getValue(Identifier.fromNamespaceAndPath("wildcatter", "air_tubing"));
			PlantBlockEntity source = plant(level, Machine.CYCLONE, f);
			pipe(level, f.offset(0, 0, 2), f.offset(1, 0, 2));
			PlantBlockEntity tank = plant(level, Machine.CONDITIONING_TANK_LARGE, f.offset(0, 0, 3));
			pipe(level, f.offset(3, 0, 11));
			PlantBlockEntity[] cells = new PlantBlockEntity[8];
			for (int k = 0; k < 8; k++) {
				int z = 12 + 8 * k;
				cells[k] = plant(level, Machine.TANK_CELL, f.offset(0, 0, z));
				pipe(level, f.offset(3, 0, z + 7));
				for (int dz = 0; dz < 8; dz++) {
					if (z + dz <= 74) {
						pipe(level, f.offset(7, 0, z + dz));
					}
					level.setBlock(f.offset(3, 7, z + dz), tubing.defaultBlockState(), Block.UPDATE_ALL);
				}
			}
			PlantBlockEntity blower = plant(level, Machine.FLOTATION_BLOWER_LARGE, f.offset(9, 0, 12));
			for (int x = 4; x <= 10; x++) {
				level.setBlock(f.offset(x, 7, 13), tubing.defaultBlockState(), Block.UPDATE_ALL);
			}
			for (int dy = 4; dy <= 6; dy++) {
				level.setBlock(f.offset(10, dy, 13), tubing.defaultBlockState(), Block.UPDATE_ALL);
			}
			for (PlantBlockEntity part : cells) {
				part.forceRunning();
			}
			tank.forceRunning();
			blower.forceRunning();
			for (String reagent : new String[] {"lime", "xanthate", "mibc"}) {
				ItemStack stack = new ItemStack(BuiltInRegistries.ITEM.getValue(dev.wildcatter.tailings.Tailings.id(reagent)), 64);
				while (!stack.isEmpty() && tank.offer(stack)) {
					// a stack of each into the hopper
				}
			}
			v[0] = truck;
			v[1] = source;
			v[2] = cells;
			at[0] = o;
		});
		BlockPos o = at[0];
		String copper = dev.wildcatter.tailings.rock.Mineral.COPPER.mineralItemId();
		Runnable feed = () -> server.runOnServer(srv -> {
			PlantBlockEntity source = (PlantBlockEntity) v[1];
			double short_ = 3000 - source.out().held(dev.wildcatter.fluid.Product.FINES_SLURRY);
			if (short_ > 0) {
				source.out().offer(dev.wildcatter.fluid.Product.FINES_SLURRY, short_, dev.wildcatter.fluid.SlurryAssay.of(Map.of(copper, 0.004)));
			}
			for (PlantBlockEntity cell : (PlantBlockEntity[]) v[2]) {
				cell.out().give(dev.wildcatter.fluid.Product.TAILS_SLURRY, Double.MAX_VALUE);
				cell.out().give(dev.wildcatter.fluid.Product.CONCENTRATE_SLURRY, Double.MAX_VALUE);
			}
		});
		time(server, 23950);
		Cam a = Cam.of(o, 52, 20, 40, 8, 16, -2);
		put(a);
		settle(100);
		for (int s = 0; s < 20; s++) {
			feed.run();
			context.waitTicks(20);
		}
		// the truck backs onto the dump wall and tips
		server.runOnServer(srv -> ((UltraHaulTruckEntity) v[0]).testDrive(new Input(false, false, false, false, true, false, false)));
		context.waitTicks(10);
		server.runOnServer(srv -> ((UltraHaulTruckEntity) v[0]).testDrive(Input.EMPTY));
		context.waitTicks(60);
		// over the flotation row toward the crusher, the truck's body up against the sky
		shot("04_blast_or_crusher", a, Cam.of(o, 46, 17, 33, 8, 17, -2), () -> {
			feed.run();
			server.runOnServer(srv -> log("crusher: truck " + ((UltraHaulTruckEntity) v[0]).load().tonnes() + "; " + ((PlantBlockEntity[]) v[2])[0].status()));
		});
	}

	// ---- 5. the headframe at sunset ----

	/** The shaft's headframe and hoist house on a mine yard, the skips winding, against the setting sun. */
	private void headframe() {
		int depth = 40;
		BlockPos[] at = new BlockPos[1];
		PlantBlockEntity[] parts = new PlantBlockEntity[4];
		server.runOnServer(srv -> {
			ServerLevel level = srv.overworld();
			BlockPos s = site(level, 0, 700, 60, 60);
			int collar = s.getY();
			int x0 = s.getX() + 20, z0 = s.getZ() + 16;
			pad(level, x0 - 16, z0 - 14, x0 + 34, z0 + 40, collar, 18, 40);
			for (int x = x0 - 8; x <= x0 + 22; x++) {
				for (int z = z0 - 8; z <= z0 + 32; z++) {
					level.setBlock(new BlockPos(x, collar - 1, z), (Math.floorMod(net.minecraft.util.Mth.getSeed(x, 0, z) >> 16, 5) < 2 ? Blocks.GRAVEL : Blocks.COARSE_DIRT).defaultBlockState(), Block.UPDATE_CLIENTS);
				}
			}
			// the shaft, 5 x 5 down to the flask's chamber; the chamber and its rock closed over
			for (int yy = collar - depth - 1; yy < collar - 1; yy++) {
				for (int x = x0 - 2; x <= x0 + 16; x++) {
					for (int z = z0 - 4; z <= z0 + 12; z++) {
						boolean shaft = x >= x0 + 2 && x <= x0 + 6 && z >= z0 + 2 && z <= z0 + 6;
						boolean chamber = x >= x0 + 7 && x <= x0 + 14 && z >= z0 && z <= z0 + 10 && yy < collar - depth + 8;
						BlockState to = yy == collar - depth - 1 || !(shaft || chamber) ? Blocks.STONE.defaultBlockState() : Blocks.AIR.defaultBlockState();
						level.setBlock(new BlockPos(x, yy, z), to, Block.UPDATE_CLIENTS);
					}
				}
			}
			parts[0] = plant(level, Machine.HEADFRAME, new BlockPos(x0, collar, z0));
			parts[1] = plant(level, Machine.HEADFRAME_BIN, new BlockPos(x0 + 2, collar, z0 - 4));
			parts[2] = plant(level, Machine.HOIST_HOUSE, new BlockPos(x0, collar, z0 + 20));
			parts[3] = plant(level, Machine.MEASURING_FLASK, new BlockPos(x0 + 7, collar - depth, z0 + 4));
			parts[2].forceRunning();
			var frame = ShaftLayout.Frame.of(parts[0]);
			ShaftSkipEntity.hang(level, frame);
			ShaftSkipEntity.hang(level, frame);
			parts[3].made().add(Rock.GRANITE, null, 2700);
			at[0] = new BlockPos(x0, collar, z0);
		});
		BlockPos h = at[0];
		Runnable keep = () -> server.runOnServer(srv -> {
			if (parts[3].made().isEmpty()) {
				parts[3].made().add(Rock.GRANITE, null, 2700);
			}
		});
		time(server, 12500);
		// from the east, low, looking west into the sunset over the headframe; a slow orbit round to the south-east
		Cam a = Cam.of(h, 34, 2, 2, 4, 13, 6);
		put(a);
		settle(100);
		for (int s = 0; s < 12; s++) {
			keep.run();
			context.waitTicks(20);
		}
		shot("05_headframe", a, Cam.of(h, 31, 4, 16, 4, 13, 8), () -> {
			keep.run();
			server.runOnServer(srv -> log("headframe: " + parts[2].status()));
		});
	}
}
