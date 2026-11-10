package dev.wildcatter.tailings.neoforge.gametest;

import static dev.wildcatter.tailings.neoforge.gametest.PitHaulGameTests.floor;
import static dev.wildcatter.tailings.neoforge.gametest.PitHaulGameTests.part;
import static dev.wildcatter.tailings.neoforge.gametest.PitHaulGameTests.relX;
import static dev.wildcatter.tailings.neoforge.gametest.PitHaulGameTests.relZ;
import static dev.wildcatter.tailings.neoforge.gametest.PitHaulGameTests.stake;
import static dev.wildcatter.tailings.neoforge.gametest.PitHaulGameTests.vehicle;
import static dev.wildcatter.tailings.neoforge.gametest.PitHaulGameTests.where;

import dev.wildcatter.fluid.Product;
import dev.wildcatter.fluid.SlurryAssay;
import dev.wildcatter.tailings.TailingsBlocks;
import dev.wildcatter.tailings.TailingsEntities;
import dev.wildcatter.tailings.TailingsItems;
import dev.wildcatter.tailings.block.HaulDispatchBlockEntity;
import dev.wildcatter.tailings.block.PlantBlockEntity;
import dev.wildcatter.tailings.entity.HaulTruckEntity;
import dev.wildcatter.tailings.entity.TrackDozerEntity;
import dev.wildcatter.tailings.pit.Dispatch;
import dev.wildcatter.tailings.pit.DryStack;
import dev.wildcatter.tailings.pit.PitMass;
import dev.wildcatter.tailings.pit.StopKind;
import dev.wildcatter.tailings.plant.Machine;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;

/**
 * The open pit's P4 (docs/multiblocks/open-pit-p4-dry-stack.md): the tailings thickener and filter, the truck load-out, waste dumps,
 * berms, going over the crest, and the track dozer.
 */
final class PitDryStackGameTests {
	static final String[] SCENES = {"pit_thickener", "pit_filter_cycle", "pit_loadout", "pit_loadout_haul", "pit_berm", "pit_no_berm", "pit_over_crest",
			"pit_dump_advance", "pit_dozer_cut", "pit_dozer_berm", "pit_dozer_hard", "pit_dozer_push"};
	static final Map<String, Integer> TICKS = Map.of("pit_thickener", 1200, "pit_filter_cycle", 1600, "pit_loadout", 1600, "pit_loadout_haul", 9000,
			"pit_berm", 3600, "pit_no_berm", 3600, "pit_over_crest", 1200, "pit_dozer_cut", 1600, "pit_dozer_berm", 1600, "pit_dozer_push", 1600);
	static final Map<String, Integer> PADDING = Map.of("pit_loadout_haul", 180, "pit_berm", 120, "pit_no_berm", 120);

	private static final Input FORWARD = new Input(true, false, false, false, false, false, false);
	private static final Input BACK = new Input(false, true, false, false, false, false, false);

	private PitDryStackGameTests() {
	}

	static int[] reach(String scene) {
		return switch (scene) {
			case "pit_loadout_haul" -> new int[] {-3, 5, -3, 9};
			case "pit_berm", "pit_no_berm" -> new int[] {-2, 4, -1, 6};
			case "pit_filter_cycle" -> new int[] {-1, 3, -1, 3};
			default -> new int[] {-1, 3, -1, 4};
		};
	}

	private static PlantBlockEntity plant(GameTestHelper helper, Machine machine, BlockPos pos) {
		return part(helper, TailingsBlocks.plantBlock(machine), pos, PlantBlockEntity.class);
	}

	private static Block block(String ns, String path) {
		return BuiltInRegistries.BLOCK.getValue(Identifier.fromNamespaceAndPath(ns, path));
	}

	/** Thickened tails at 60 % solids, as the thickener puts them on the pipe. */
	private static SlurryAssay thickened() {
		return SlurryAssay.of(Map.of(DryStack.SOLIDS_KEY, DryStack.UNDERFLOW_SOLIDS, "tailings:copper_nugget", 1e-5));
	}

	private static int count(GameTestHelper helper, Block block, int x0, int x1, int y0, int y1, int z0, int z1) {
		int n = 0;
		for (int x = x0; x <= x1; x++) {
			for (int y = y0; y <= y1; y++) {
				for (int z = z0; z <= z1; z++) {
					if (helper.getBlockState(new BlockPos(x, y, z)).is(block)) {
						n++;
					}
				}
			}
		}
		return n;
	}

	/** The highest solid block in the column at (x, z), from y {@code top} down, or -1. */
	private static int top(GameTestHelper helper, int x, int z, int from) {
		for (int y = from; y >= 0; y--) {
			if (!helper.getBlockState(new BlockPos(x, y, z)).isAir()) {
				return y;
			}
		}
		return -1;
	}

	// ---- P4a: the thickener, the filter, the load-out ----------------------------------------------------------------------------

	/**
	 * Tails slurry in: 60 % underflow on the pipe to the filter (the press takes it), and about 1.2 L of overflow water a kg of solids
	 * back on the water pipe to a ball mill.
	 */
	static void thickener(GameTestHelper helper) {
		floor(helper, 0, 30, 0, 0, 0, 45);
		PlantBlockEntity thickener = plant(helper, Machine.TAILINGS_THICKENER, new BlockPos(0, 1, 0));
		thickener.forceRunning();
		thickener.back().offer(Product.TAILS_SLURRY, 2_000, SlurryAssay.of(Map.of("tailings:copper_nugget", 2e-5)));
		// overflow water east to a ball mill
		Block water = block("wildcatter", "pipe");
		helper.setBlock(new BlockPos(15, 1, 7), water);
		helper.setBlock(new BlockPos(16, 1, 7), water);
		PlantBlockEntity mill = plant(helper, Machine.BALL_MILL, new BlockPos(17, 1, 4));
		// the underflow south on steel pipe to the filter's north face
		Block steel = block("wildcatter", "gas_line");
		for (int z = 15; z <= 17; z++) {
			helper.setBlock(new BlockPos(2, 1, z), steel);
		}
		PlantBlockEntity filter = plant(helper, Machine.TAILINGS_FILTER, new BlockPos(0, 1, 18));
		filter.forceRunning();
		helper.succeedWhen(() -> {
			double used = 2_000 - thickener.back().held(Product.TAILS_SLURRY);
			helper.assertTrue(used > 1_500, Component.literal("the thickener should work its feed: used " + used + "; " + thickener.status()));
			double solids = used * DryStack.FEED_SOLIDS;
			double inFilter = filter.dryKg() + filter.back().held(Product.TAILS_SLURRY) * DryStack.UNDERFLOW_SOLIDS + filter.cakeOut();
			double onPipe = thickener.out().held(Product.TAILS_SLURRY) * DryStack.UNDERFLOW_SOLIDS;
			helper.assertTrue(Math.abs(inFilter + onPipe - solids) < 2, Component.literal("its solids should reach the filter as 60 % underflow: " + (inFilter + onPipe) + " of " + solids + "; " + filter.status()));
			helper.assertTrue(filter.dryKg() > 0, Component.literal("the filter should press thickened tails: " + filter.status()));
			double overflow = thickener.waterReturned() + thickener.waterOut() + thickener.waterLost();
			helper.assertTrue(Math.abs(overflow / solids - DryStack.overflowPerKgSolids()) < 0.05, Component.literal("about 1.2 L of overflow a kg: " + overflow / solids));
			helper.assertTrue(mill.water() > 0 && thickener.waterReturned() > 0, Component.literal("the overflow should reach the mill on the water pipe: " + mill.water()));
		});
	}

	/**
	 * One cycle drops 15 t of dry tails into the load-out bin under the press (the cycle run 60 times fast); a press fed unthickened
	 * tails reads NOT THICKENED; one with nowhere to drop its cake reads FULL.
	 */
	static void filterCycle(GameTestHelper helper) {
		floor(helper, 0, 40, 0, 0, 0, 30);
		PlantBlockEntity bin = plant(helper, Machine.LOADOUT_BIN, new BlockPos(0, 1, 0));
		PlantBlockEntity filter = plant(helper, Machine.TAILINGS_FILTER, new BlockPos(2, 8, 0));
		filter.forceRunning();
		filter.fastCycle(60);
		filter.back().offer(Product.TAILS_SLURRY, 26_000, thickened());
		PlantBlockEntity raw = plant(helper, Machine.TAILINGS_FILTER, new BlockPos(14, 1, 0));
		raw.forceRunning();
		raw.back().offer(Product.TAILS_SLURRY, 1_000, SlurryAssay.EMPTY);
		PlantBlockEntity full = plant(helper, Machine.TAILINGS_FILTER, new BlockPos(24, 1, 0));
		full.forceRunning();
		full.fastCycle(60);
		full.back().offer(Product.TAILS_SLURRY, 26_000, thickened());
		helper.startSequence()
				.thenWaitUntil(() -> helper.assertTrue(filter.cycles() == 1, Component.literal("one cycle should drop: " + filter.status())))
				.thenExecute(() -> {
					helper.assertTrue(Math.abs(bin.dryKg() - DryStack.BATCH_KG) < 1, Component.literal("15 t of dry tails into the bin: " + bin.dryKg()));
					helper.assertTrue(raw.status().startsWith("NOT THICKENED"), Component.literal("plain tails are refused: " + raw.status()));
				})
				.thenWaitUntil(() -> helper.assertTrue(full.status().startsWith("FULL"), Component.literal("nowhere to drop, it reads FULL: " + full.status())))
				.thenExecute(() -> helper.assertTrue(full.cakeOut() > 14_000, Component.literal("and holds its cake: " + full.cakeOut())))
				.thenSucceed();
	}

	/** Dry tails fill a 91 t truck stopped under the gate at 2 t/s, about 46 s; rock is refused. */
	static void loadout(GameTestHelper helper) {
		floor(helper, 0, 20, 0, 0, 0, 24);
		PlantBlockEntity frame = plant(helper, Machine.LOADOUT_FRAME, new BlockPos(0, 1, 0));
		PlantBlockEntity bin = plant(helper, Machine.LOADOUT_BIN, new BlockPos(1, 11, 1));
		frame.forceRunning();
		helper.assertFalse(bin.offer(new ItemStack(Blocks.COBBLESTONE)), Component.literal("rock is refused"));
		helper.assertTrue(bin.status().startsWith("dry tails only"), Component.literal("it says why: " + bin.status()));
		for (int i = 0; i < 60; i++) {
			bin.offer(new ItemStack(TailingsBlocks.DRY_TAILS));
		}
		helper.assertTrue(Math.abs(bin.dryKg() - 108_000) < 1, Component.literal("60 blocks of dry tails are 108 t: " + bin.dryKg()));
		// its body's centre under the gate, at the lane's centre
		HaulTruckEntity truck = vehicle(helper, TailingsEntities.HAUL_TRUCK, 6.0, 1, 7.0, 0);
		long[] start = {helper.getLevel().getGameTime()};
		helper.startSequence()
				.thenWaitUntil(() -> helper.assertTrue(truck.load().room() == 0, Component.literal("the truck should fill: " + truck.load().tonnes() + "; " + frame.status())))
				.thenExecute(() -> {
					double seconds = (helper.getLevel().getGameTime() - start[0]) / 20.0;
					helper.assertTrue(seconds > 43 && seconds < 49, Component.literal("91 t at 2 t/s is about 46 s: " + seconds));
					helper.assertTrue(truck.load().kg(PitMass.DRY_TAILS) == 91_000, Component.literal("all dry tails: " + truck.load().save()));
					helper.assertTrue(Math.abs(bin.dryKg() - 17_000) < 1, Component.literal("17 t left in the bin: " + bin.dryKg()));
				})
				.thenSucceed();
	}

	/**
	 * A self-driving truck on a LOADOUT route: it drives through the lane to the gate and is filled, out of the far side and round a
	 * balloon loop, back through, and on to tip on a DUMP POINT (a paddock); the dispatch office counts the load.
	 */
	static void loadoutHaul(GameTestHelper helper) {
		floor(helper, -30, 60, 56, 58, -25, 140);
		ServerLevel level = helper.getLevel();
		PlantBlockEntity frame = plant(helper, Machine.LOADOUT_FRAME, new BlockPos(14, 59, 60));
		PlantBlockEntity bin = plant(helper, Machine.LOADOUT_BIN, new BlockPos(15, 69, 61));
		frame.forceRunning();
		bin.setDryKg(100_000);
		List<BlockPos> route = List.of(stake(helper, 19, 59, 2, StopKind.DUMP_POINT), stake(helper, 19, 59, 30, StopKind.WAYPOINT),
				stake(helper, 19, 59, 45, StopKind.WAYPOINT), stake(helper, 19, 59, 59, StopKind.LOADOUT));
		HaulDispatchBlockEntity office = part(helper, TailingsBlocks.HAUL_DISPATCH, new BlockPos(45, 59, 10), HaulDispatchBlockEntity.class);
		office.forcePowered(true);
		office.addRoute(route);
		HaulTruckEntity truck = vehicle(helper, TailingsEntities.HAUL_TRUCK, 19.5, 59, 40, 0);
		truck.setFuel(truck.tank());
		helper.assertTrue(truck.takeRoute(level, route) == null, Component.literal("a LOADOUT and DUMP POINT route should be taken"));
		helper.startSequence()
				.thenWaitUntil(() -> helper.assertTrue(truck.load().room() == 0, Component.literal("it should be filled under the gate: " + where(helper, truck) + "; " + frame.status())))
				.thenExecute(() -> {
					helper.assertTrue(Math.abs(relZ(helper, truck) - 1 - 66) < 2.5, Component.literal("its body under the gate at z 66: " + where(helper, truck)));
					helper.assertTrue(Math.abs(relX(helper, truck) - 20) < 1.5, Component.literal("in the middle of the lane: " + where(helper, truck)));
				})
				.thenWaitUntil(() -> helper.assertTrue(relZ(helper, truck) > 85, Component.literal("out of the far side: " + where(helper, truck))))
				.thenWaitUntil(() -> helper.assertTrue(relZ(helper, truck) < 45, Component.literal("round the loop and back through the lane: " + where(helper, truck))))
				.thenWaitUntil(() -> helper.assertTrue(office.readPulses(Dispatch.LOADS) == 1, Component.literal("it tips at the dump point, and dispatch counts the load: " + where(helper, truck))))
				.thenExecute(() -> helper.assertTrue(count(helper, TailingsBlocks.DRY_TAILS, 10, 30, 59, 66, -12, 2) >= 35, Component.literal("about 50 blocks of dry tails at the dump point: "
						+ count(helper, TailingsBlocks.DRY_TAILS, 10, 30, 59, 66, -12, 2))))
				.thenSucceed();
	}

	// ---- P4b: dumps, berms, the crest ---------------------------------------------------------------------------------------------

	/** A bench at y 58 to z 59 with a 6 m drop beyond: the dump's crest at z 60. */
	private static void crest(GameTestHelper helper) {
		floor(helper, -15, 55, 50, 58, -5, 59);
		floor(helper, -15, 55, 50, 52, 60, 90);
	}

	private static List<BlockPos> dumpRoute(GameTestHelper helper) {
		return List.of(stake(helper, 20, 59, 0, StopKind.LOAD), stake(helper, 20, 59, 30, StopKind.WAYPOINT), stake(helper, 20, 59, 57, StopKind.DUMP_POINT));
	}

	private static void berm(GameTestHelper helper, int z) {
		for (int x = 10; x <= 30; x++) {
			helper.setBlock(new BlockPos(x, 59, z), TailingsBlocks.DRY_TAILS);
		}
	}

	private static HaulTruckEntity loadedTruck(GameTestHelper helper, List<BlockPos> route) {
		HaulTruckEntity truck = vehicle(helper, TailingsEntities.HAUL_TRUCK, 20.5, 59, 20, 0);
		truck.setFuel(truck.tank());
		truck.load().add(PitMass.DRY_TAILS, 30_600);
		truck.refreshLoad();
		helper.assertTrue(truck.takeRoute(helper.getLevel(), route) == null, Component.literal("the route should be taken"));
		return truck;
	}

	/** The spec's scene: a self-driving truck backs to a 1-block berm at the crest, stops 1 m short, and tips over it down the face. */
	static void bermScene(GameTestHelper helper) {
		crest(helper);
		berm(helper, 59);
		HaulTruckEntity truck = loadedTruck(helper, dumpRoute(helper));
		helper.startSequence()
				.thenWaitUntil(() -> helper.assertTrue(truck.load().empty(), Component.literal("it should back to the berm and tip: " + where(helper, truck))))
				.thenExecute(() -> {
					double tail = relZ(helper, truck) + 5;
					helper.assertTrue(tail > 56.4 && tail < 58.6, Component.literal("its tail 1 m short of the berm at z 59: " + tail));
					int down = count(helper, TailingsBlocks.DRY_TAILS, 0, 40, 53, 58, 60, 80);
					helper.assertTrue(down >= 8, Component.literal("the load should run over the berm and down the face: " + down));
				})
				.thenSucceed();
	}

	/** With no berm at the crest it stops at the stake before and reads NO BERM, BLOCKED in dispatch after 30 s; a berm pushed up, it goes and tips. */
	static void noBerm(GameTestHelper helper) {
		crest(helper);
		List<BlockPos> route = dumpRoute(helper);
		HaulDispatchBlockEntity office = part(helper, TailingsBlocks.HAUL_DISPATCH, new BlockPos(45, 59, 5), HaulDispatchBlockEntity.class);
		office.forcePowered(true);
		office.addRoute(route);
		HaulTruckEntity truck = loadedTruck(helper, route);
		helper.startSequence()
				.thenWaitUntil(() -> helper.assertTrue(truck.status().startsWith("NO BERM"), Component.literal("it should read NO BERM: " + where(helper, truck))))
				.thenExecute(() -> helper.assertTrue(Math.abs(relZ(helper, truck) - 30) < 8, Component.literal("at the stake before the dump point: " + where(helper, truck))))
				.thenWaitUntil(() -> helper.assertTrue(office.readPoint(Dispatch.BLOCKED), Component.literal("BLOCKED after 30 s: " + where(helper, truck))))
				.thenExecute(() -> {
					helper.assertFalse(truck.load().empty(), Component.literal("it has not tipped"));
					berm(helper, 59);
				})
				.thenWaitUntil(() -> helper.assertTrue(truck.load().empty(), Component.literal("with a berm it backs in and tips: " + where(helper, truck))))
				.thenExecute(() -> helper.assertFalse(office.readPoint(Dispatch.BLOCKED), Component.literal("and BLOCKED clears")))
				.thenSucceed();
	}

	/** A truck driven by hand backs over a 5 m crest with no berm: it falls, and is wrecked at the bottom into its item and its load. */
	static void overCrest(GameTestHelper helper) {
		floor(helper, 0, 20, 51, 58, 0, 40);
		floor(helper, 0, 20, 51, 53, 41, 70);
		HaulTruckEntity truck = vehicle(helper, TailingsEntities.HAUL_TRUCK, 10.5, 59, 28, 180);
		truck.load().add("minecraft:sand", 32_000);
		truck.refreshLoad();
		truck.testDrive(BACK);
		helper.startSequence()
				.thenWaitUntil(() -> helper.assertTrue(truck.isRemoved(), Component.literal("it should go over the crest and be wrecked: " + where(helper, truck))))
				.thenExecute(() -> {
					BlockPos lo = helper.absolutePos(new BlockPos(0, 50, 35)), hi = helper.absolutePos(new BlockPos(20, 62, 70));
					List<ItemEntity> items = helper.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(lo.getX(), lo.getY(), lo.getZ(), hi.getX(), hi.getY(), hi.getZ()),
							e -> e.getItem().is(TailingsItems.HAUL_TRUCK));
					helper.assertTrue(!items.isEmpty() && items.get(0).getY() < helper.absolutePos(new BlockPos(0, 57, 0)).getY(),
							Component.literal("its item should lie at the bottom: " + items.size()));
					int sand = count(helper, Blocks.SAND, 0, 20, 54, 60, 38, 60);
					helper.assertTrue(sand >= 18, Component.literal("its load tipped where it landed: " + sand + " blocks of sand"));
				})
				.thenSucceed();
	}

	/** Ten 91 t loads tipped at one point at a 5 m crest advance the crest out over the face, and the face stands near 37 degrees. */
	static void dumpAdvance(GameTestHelper helper) {
		floor(helper, 0, 40, 51, 58, 0, 40);
		floor(helper, 0, 40, 51, 53, 41, 80);
		HaulTruckEntity truck = vehicle(helper, TailingsEntities.HAUL_TRUCK, 20.5, 59, 34.0, 180);
		for (int i = 0; i < 10; i++) {
			truck.load().add(PitMass.DRY_TAILS, 91_000);
			truck.tip(helper.getLevel());
			helper.assertTrue(truck.load().empty(), Component.literal("load " + (i + 1) + " should tip: " + truck.status()));
		}
		// the crest: the furthest column out at the bench's level or above; the toe: the first beyond at the floor's
		int crest = 40, toe = 40;
		for (int z = 40; z <= 80; z++) {
			int t = top(helper, 20, z, 75);
			if (t >= 58) {
				crest = z;
			}
			if (t <= 53) {
				toe = z;
				break;
			}
		}
		StringBuilder tops = new StringBuilder();
		for (int z = 30; z <= 60; z++) {
			tops.append(z).append(':').append(top(helper, 20, z, 120)).append(' ');
		}
		helper.assertTrue(crest >= 43, Component.literal("the crest should advance out from z 40: " + crest + "; tops " + tops));
		double slope = 5.0 / Math.max(1, toe - crest);
		double degrees = Math.toDegrees(Math.atan(slope));
		helper.assertTrue(degrees >= 28 && degrees <= 46, Component.literal("the face should stand near 37 degrees: " + degrees + " (crest " + crest + ", toe " + toe + ")"));
		helper.succeed();
	}

	// ---- the track dozer ----------------------------------------------------------------------------------------------------------

	/** In CUT at 10 % it cuts a 20 m ramp in sand: a block down at its start, two down at its end. */
	static void dozerCut(GameTestHelper helper) {
		floor(helper, 0, 30, 50, 55, 0, 50);
		for (int x = 0; x <= 30; x++) {
			for (int z = 0; z <= 50; z++) {
				for (int y = 56; y <= 58; y++) {
					helper.setBlock(new BlockPos(x, y, z), Blocks.SAND);
				}
			}
		}
		TrackDozerEntity dozer = vehicle(helper, TailingsEntities.TRACK_DOZER, 15.5, 59, 5.0, 0);
		dozer.setBlade(TrackDozerEntity.Blade.CUT);
		dozer.setGrade(DryStack.Grade.TEN);
		dozer.testDrive(FORWARD);
		double startZ = relZ(helper, dozer);
		helper.startSequence()
				.thenWaitUntil(() -> helper.assertTrue(relZ(helper, dozer) - startZ >= 20, Component.literal("it should cut 20 m: " + where(helper, dozer))))
				.thenExecute(() -> dozer.testDrive(Input.EMPTY))
				// under a loaded test server it can take more than 10 ticks to stop; wait for it, then look well behind the blade
				// (where it stops it drops what the blade carries)
				.thenWaitUntil(() -> helper.assertTrue(dozer.speed() == 0, Component.literal("it should stop: " + dozer.speed())))
				.thenExecute(() -> {
					int first = top(helper, 15, 12, 60);
					int last = top(helper, 15, 22, 60);
					helper.assertTrue(first == 57, Component.literal("a block down at the ramp's start: " + first));
					helper.assertTrue(last == 56, Component.literal("two down at its end: " + last));
					helper.assertTrue(dozer.speed() == 0 && dozer.fuel() < dozer.tank() * 0.10, Component.literal("it burnt diesel cutting: " + dozer.fuel()));
				})
				.thenSucceed();
	}

	/** In CARRY with a load, driving along a crest with the crest on its left, it drops a berm 1 block high along it. */
	static void dozerBerm(GameTestHelper helper) {
		floor(helper, 0, 29, 50, 58, 0, 40);
		floor(helper, 30, 45, 50, 52, 0, 40);
		TrackDozerEntity dozer = vehicle(helper, TailingsEntities.TRACK_DOZER, 25.5, 59, 5.0, 0);
		dozer.loadBlade(PitMass.DRY_TAILS, 16);
		dozer.testDrive(FORWARD);
		helper.startSequence()
				.thenWaitUntil(() -> helper.assertTrue(relZ(helper, dozer) > 22, Component.literal("it should drive along the crest: " + where(helper, dozer))))
				.thenExecute(() -> {
					int berm = count(helper, TailingsBlocks.DRY_TAILS, 27, 29, 59, 59, 0, 30);
					helper.assertTrue(berm >= 10, Component.literal("a berm should stand along the crest: " + berm + "; " + dozer.status()));
					helper.assertTrue(count(helper, TailingsBlocks.DRY_TAILS, 27, 29, 60, 61, 0, 30) == 0, Component.literal("1 block high"));
					helper.assertTrue(dozer.bladeBlocks() < 16, Component.literal("from the blade's load: " + dozer.bladeBlocks()));
				})
				.thenSucceed();
	}

	/** Standing stone in front of the blade stops it: "hard rock: blast it first". */
	static void dozerHard(GameTestHelper helper) {
		floor(helper, 0, 20, 56, 58, 0, 30);
		for (int x = 0; x <= 20; x++) {
			helper.setBlock(new BlockPos(x, 59, 16), Blocks.STONE);
			helper.setBlock(new BlockPos(x, 60, 16), Blocks.STONE);
		}
		TrackDozerEntity dozer = vehicle(helper, TailingsEntities.TRACK_DOZER, 10.5, 59, 5.0, 0);
		dozer.testDrive(FORWARD);
		helper.startSequence()
				.thenWaitUntil(() -> helper.assertTrue(dozer.status().equals("hard rock: blast it first"), Component.literal("it should stop at the stone: " + where(helper, dozer))))
				.thenIdle(20)
				.thenExecute(() -> {
					helper.assertTrue(relZ(helper, dozer) < 16 - 4.4, Component.literal("short of the stone: " + where(helper, dozer)));
					helper.assertTrue(helper.getBlockState(new BlockPos(10, 59, 16)).is(Blocks.STONE), Component.literal("the stone still stands"));
				})
				.thenSucceed();
	}

	/** In CARRY it takes a heap of sand into the blade, pushes it to the crest and over, and stops at the edge. */
	static void dozerPush(GameTestHelper helper) {
		floor(helper, 0, 20, 50, 58, 0, 30);
		floor(helper, 0, 20, 50, 52, 31, 50);
		for (int x = 8; x <= 12; x++) {
			for (int z = 15; z <= 16; z++) {
				helper.setBlock(new BlockPos(x, 59, z), Blocks.SAND);
				helper.setBlock(new BlockPos(x, 60, z), Blocks.SAND);
			}
		}
		TrackDozerEntity dozer = vehicle(helper, TailingsEntities.TRACK_DOZER, 10.5, 59, 4.0, 0);
		dozer.testDrive(FORWARD);
		helper.startSequence()
				.thenWaitUntil(() -> helper.assertTrue(dozer.bladeBlocks() >= 18, Component.literal("the blade should take the heap: " + dozer.bladeBlocks() + "; " + where(helper, dozer))))
				.thenWaitUntil(() -> helper.assertTrue(dozer.status().contains("pushed the load over"), Component.literal("it should push the load over the crest: " + where(helper, dozer))))
				.thenExecute(() -> {
					helper.assertTrue(dozer.bladeBlocks() == 0, Component.literal("the blade is empty"));
					helper.assertTrue(count(helper, Blocks.SAND, 0, 20, 53, 58, 31, 45) >= 15, Component.literal("the sand ran down the face: " + count(helper, Blocks.SAND, 0, 20, 53, 58, 31, 45)));
					helper.assertTrue(dozer.getY() >= helper.absolutePos(new BlockPos(0, 59, 0)).getY() - 0.01, Component.literal("the dozer stays on the bench"));
				})
				.thenSucceed();
	}
}
