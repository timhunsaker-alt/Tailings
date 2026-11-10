package dev.wildcatter.tailings.neoforge.gametest;

import static dev.wildcatter.tailings.neoforge.gametest.PitHaulGameTests.floor;
import static dev.wildcatter.tailings.neoforge.gametest.PitHaulGameTests.part;
import static dev.wildcatter.tailings.neoforge.gametest.PitHaulGameTests.relZ;
import static dev.wildcatter.tailings.neoforge.gametest.PitHaulGameTests.stake;
import static dev.wildcatter.tailings.neoforge.gametest.PitHaulGameTests.vehicle;
import static dev.wildcatter.tailings.neoforge.gametest.PitHaulGameTests.where;

import dev.wildcatter.energy.Voltage;
import dev.wildcatter.tailings.TailingsBlocks;
import dev.wildcatter.tailings.TailingsEntities;
import dev.wildcatter.tailings.TailingsItems;
import dev.wildcatter.tailings.block.DumpBlockEntity;
import dev.wildcatter.tailings.block.PitSwitchHouseBlockEntity;
import dev.wildcatter.tailings.block.MuckBlockEntity;
import dev.wildcatter.tailings.block.RopeShovelBlockEntity;
import dev.wildcatter.tailings.entity.HydraulicShovelEntity;
import dev.wildcatter.tailings.entity.RopeShovelEntity;
import dev.wildcatter.tailings.pit.RopeShovelLayout;
import dev.wildcatter.tailings.entity.UltraHaulTruckEntity;
import dev.wildcatter.tailings.entity.WheelLoaderEntity;
import dev.wildcatter.tailings.pit.StopKind;
import java.util.ArrayList;
import java.util.List;
import dev.wildcatter.tailings.entity.BlastholeDrillEntity;
import dev.wildcatter.tailings.entity.PitVehicleEntity;
import dev.wildcatter.tailings.item.TrailingCableItem;
import dev.wildcatter.tailings.pit.PitPower;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.item.ItemStack;

/**
 * The open pit's P5 (docs/multiblocks/open-pit-p5-big-machines.md): the pit switch house and its trailing cables (P5a), the 360 t
 * ultra truck (P5b), the hydraulic front shovel (P5c) and the electric rope shovel (P5d).
 */
final class PitBigGameTests {
	static final String[] SCENES = {"pit_switch_house", "pit_cable_end", "pit_ultra_haul", "pit_ultra_loader", "pit_hyd_shovel", "pit_hyd_electric",
			"pit_rope_shovel_built", "pit_rope_shovel", "pit_low_voltage", "pit_switch_house_fed", "pit_shovel_edge"};
	static final Map<String, Integer> TICKS = Map.ofEntries(Map.entry("pit_low_voltage", 1200), Map.entry("pit_shovel_edge", 1200), Map.entry("pit_switch_house_fed", 1200), Map.entry("pit_switch_house", 1200), Map.entry("pit_cable_end", 1600),
			Map.entry("pit_ultra_haul", 7200), Map.entry("pit_ultra_loader", 400), Map.entry("pit_hyd_shovel", 2400), Map.entry("pit_hyd_electric", 1200),
			Map.entry("pit_rope_shovel_built", 400), Map.entry("pit_rope_shovel", 3000));
	static final Map<String, Integer> PADDING = Map.of("pit_switch_house", 60, "pit_cable_end", 60, "pit_ultra_haul", 200, "pit_ultra_loader", 60,
			"pit_hyd_shovel", 80, "pit_hyd_electric", 60, "pit_rope_shovel_built", 80, "pit_rope_shovel", 120);
	private static final Input FORWARD = new Input(true, false, false, false, false, false, false);

	private PitBigGameTests() {
	}

	static int[] reach(String scene) {
		return switch (scene) {
			case "pit_switch_house", "pit_cable_end", "pit_ultra_loader", "pit_hyd_shovel", "pit_hyd_electric", "pit_low_voltage", "pit_switch_house_fed", "pit_shovel_edge" -> new int[] {-1, 4, -1, 4};
			case "pit_ultra_haul" -> new int[] {-3, 4, -1, 11};
			case "pit_rope_shovel_built", "pit_rope_shovel" -> new int[] {-1, 5, -1, 6};
			default -> DraglineGameTests.reach(scene);
		};
	}

	// ---- building ---------------------------------------------------------------------------------------------------------------

	/** A switch house facing north with its main cell at {@code at}, fed by the test's ideal supply. */
	static PitSwitchHouseBlockEntity house(GameTestHelper helper, BlockPos at) {
		PitSwitchHouseBlockEntity house = part(helper, TailingsBlocks.PIT_SWITCH_HOUSE, at, PitSwitchHouseBlockEntity.class);
		house.forceSupply(true);
		return house;
	}

	/** Couples a machine to the house's next free outlet, as a player does with a reel; why not, or null. */
	static String couple(GameTestHelper helper, PitSwitchHouseBlockEntity house, PitVehicleEntity machine) {
		ItemStack reel = new ItemStack(TailingsItems.TRAILING_CABLE, 4);
		TrailingCableItem.pickUp(reel, house);
		return TrailingCableItem.couple(helper.getLevel(), reel, machine);
	}

	static BlastholeDrillEntity electricDrill(GameTestHelper helper, double x, int y, double z, float yaw) {
		BlastholeDrillEntity drill = vehicle(helper, TailingsEntities.BLASTHOLE_DRILL, x, y, z, yaw);
		drill.set(7, 1, null);   // its panel's Electric
		drill.setFuel(500);
		return drill;
	}

	// ---- P5a: the switch house and trailing cables -----------------------------------------------------------------------------

	/**
	 * An electric drill on outlet 1 draws its power through its cable: drilling it takes about 440 kW from the outlet and no diesel. An
	 * outlet with no cable will not close (the ground check). A load held at 1.3 x its peak trips its outlet in about 1 s; at 1.2 x it
	 * holds.
	 */
	static void switchHouse(GameTestHelper helper) {
		floor(helper, 0, 40, 1, 1, 0, 40);
		PitSwitchHouseBlockEntity house = house(helper, new BlockPos(4, 2, 4));
		BlastholeDrillEntity drill = electricDrill(helper, 20.5, 2, 26.5, 0);
		String why = couple(helper, house, drill);
		helper.assertTrue(why == null, Component.literal("the drill should couple: " + why));
		UUID fake = UUID.randomUUID();
		helper.assertTrue(house.couple(2, fake, "Test Load", PitPower.CABLE_M) == null, Component.literal("a cable on outlet 3"));
		double peak = 1_000_000;
		double[] overload = {0};
		helper.onEachTick(() -> house.report(2, fake, overload[0] * peak, peak, PitPower.CABLE_M, 50));
		long[] overFrom = {0};
		helper.startSequence()
				.thenIdle(5)
				.thenExecute(() -> {
					helper.assertTrue(house.close(0) == null, Component.literal("outlet 1 should close on the drill's cable: " + house.close(0)));
					String refused = house.close(1);
					helper.assertTrue(refused != null && refused.contains("GROUND CHECK"), Component.literal("outlet 2 has no cable: it should not close, " + refused));
					helper.assertTrue(house.close(2) == null, Component.literal("outlet 3 should close"));
				})
				.thenWaitUntil(() -> helper.assertTrue(drill.cable() != null && drill.cable().live(), Component.literal("the drill should have power: " + drill.powerInfo())))
				.thenExecute(() -> drill.pressJump(helper.getLevel()))
				.thenWaitUntil(() -> helper.assertTrue(drill.lift() > 0.05F, Component.literal("it should be drilling: " + drill.status())))
				.thenIdle(20)
				.thenExecute(() -> {
					double nominal = Voltage.MEDIUM.lineToLine();
					String at = house.inspect().getString();
					System.out.println("[tailings-test] pit_switch_house: " + at.replace('\n', '|') + "; drill " + drill.powerInfo());
					helper.assertTrue(drill.fuel() == 500, Component.literal("on its cable it burns no diesel: " + drill.fuel()));
					helper.assertTrue(drill.demand() > 400_000 && Math.abs(house.watts(0) - drill.demand()) < drill.demand() * 0.1,
							Component.literal("the outlet should carry the drill's draw, " + Math.round(drill.demand() / 1000) + " kW: " + at));
					double amps = PitPower.amps(drill.demand(), nominal, PitPower.POWER_FACTOR);
					helper.assertTrue(Math.abs(house.amps(0) - amps) < amps * 0.1, Component.literal("about " + Math.round(amps) + " A: " + at));
					helper.assertTrue(house.volts(0) < nominal && house.volts(0) > nominal * 0.97, Component.literal("a little drop on 400 m of cable: " + at));
					overload[0] = 1.2;
				})
				.thenIdle(60)
				.thenExecute(() -> {
					helper.assertTrue(house.state(2) == PitSwitchHouseBlockEntity.State.CLOSED,
							Component.literal("at 1.2 x its peak the outlet holds: " + house.inspect().getString()));
					overload[0] = 1.3;
					overFrom[0] = helper.getTick();
				})
				.thenIdle(12)
				.thenExecute(() -> helper.assertTrue(house.state(2) == PitSwitchHouseBlockEntity.State.CLOSED,
						Component.literal("0.6 s over its pickup is not yet a trip: " + house.inspect().getString())))
				.thenWaitUntil(() -> helper.assertTrue(house.state(2) == PitSwitchHouseBlockEntity.State.TRIPPED,
						Component.literal("at 1.3 x its peak it trips: " + house.inspect().getString())))
				.thenExecute(() -> {
					long took = helper.getTick() - overFrom[0];
					System.out.println("[tailings-test] pit_switch_house: tripped " + took + " ticks after the overload");
					helper.assertTrue(took >= 18 && took <= 40, Component.literal("in about 1 s: " + took + " ticks"));
					helper.assertTrue(house.state(0) == PitSwitchHouseBlockEntity.State.CLOSED, Component.literal("the drill's outlet stays closed"));
					helper.assertTrue(house.close(2) != null, Component.literal("a tripped outlet stays open until it is reset"));
				})
				.thenSucceed();
	}

	/** An electric drill whose cable is all but paid out drives on, away from the house, and stops at CABLE END at 400 m of path. */
	static void cableEnd(GameTestHelper helper) {
		floor(helper, 0, 40, 1, 1, 0, 40);
		PitSwitchHouseBlockEntity house = house(helper, new BlockPos(4, 2, 4));
		BlastholeDrillEntity drill = electricDrill(helper, 20.5, 2, 16.5, 0);   // facing south, away from the house
		String why = couple(helper, house, drill);
		helper.assertTrue(why == null, Component.literal("the drill should couple: " + why));
		double[] stoppedZ = {0};
		helper.startSequence()
				.thenIdle(5)
				.thenExecute(() -> {
					helper.assertTrue(house.close(0) == null, Component.literal("outlet 1 should close"));
					drill.cable().setPaid(PitPower.CABLE_M - 4);
					drill.testDrive(FORWARD);
				})
				.thenWaitUntil(() -> helper.assertTrue(drill.status().startsWith("CABLE END"), Component.literal("it should stop at its cable's end: " + where(helper, drill)
						+ "; " + drill.powerInfo())))
				.thenExecute(() -> {
					stoppedZ[0] = drill.getZ();
					helper.assertTrue(drill.cable().paid() <= PitPower.CABLE_M + 1e-6 && drill.cable().paid() > PitPower.CABLE_M - 0.5,
							Component.literal("400 m paid out: " + drill.cable().describe()));
				})
				.thenIdle(40)
				.thenExecute(() -> {
					helper.assertTrue(Math.abs(drill.getZ() - stoppedZ[0]) < 0.05, Component.literal("it goes no further: " + where(helper, drill)));
					helper.assertTrue(house.state(0) == PitSwitchHouseBlockEntity.State.CLOSED, Component.literal("its outlet stays closed: " + house.inspect().getString()));
					System.out.println("[tailings-test] pit_cable_end: " + where(helper, drill) + "; " + drill.cable().describe());
					drill.testDrive(Input.EMPTY);
				})
				.thenSucceed();
	}

	// ---- P5b: the ultra truck ------------------------------------------------------------------------------------------------------

	/** The road's level (its first air) at {@code z}: the dump's pad at 59, a 10 % ramp down a block every 10 m to the floor at 53. */
	private static int road(int z) {
		return z <= 80 ? 59 : z > 130 ? 53 : 59 - ((z - 81) / 10 + 1);
	}

	/**
	 * A loaded ultra truck on a route climbs a 10 % ramp at about 11 km/h (2,828 kW x 0.8 over 623.7 t x g x 0.12), turns round in the
	 * 40 m it needs at the top, backs to the truck dump's bumper and tips 363 t into the pit hopper.
	 */
	static void ultraHaul(GameTestHelper helper) {
		for (int z = 12; z <= 160; z++) {
			int x0 = z <= 85 ? -40 : -10, x1 = z <= 85 ? 55 : 25;
			for (int x = x0; x <= x1; x++) {
				for (int y = 47; y < road(z); y++) {
					helper.setBlock(x, y, z, net.minecraft.world.level.block.Blocks.STONE);
				}
			}
		}
		DumpBlockEntity hopper = part(helper, TailingsBlocks.PIT_HOPPER, new BlockPos(2, 53, 4), DumpBlockEntity.class);
		part(helper, TailingsBlocks.TRUCK_DUMP_WALL, new BlockPos(1, 56, 10), null);
		List<BlockPos> route = new ArrayList<>();
		route.add(stake(helper, 7, 53, 150, StopKind.LOAD));
		route.add(stake(helper, 7, 53, 131, StopKind.WAYPOINT));
		route.add(stake(helper, 7, 59, 71, StopKind.WAYPOINT));   // the ramp's top: the leg up it is 10 %, as the ramp
		route.add(stake(helper, 7, 59, 55, StopKind.WAYPOINT));   // where it turns round, 40 m of pad on either side
		route.add(stake(helper, 7, 59, 13, StopKind.DUMP_POCKET));
		UltraHaulTruckEntity truck = vehicle(helper, TailingsEntities.ULTRA_HAUL_TRUCK, 7.5, 53, 134, 180);
		truck.setFuel(truck.tank());
		truck.load().add("minecraft:cobblestone", 363_000);
		truck.refreshLoad();
		String why = truck.takeRoute(helper.getLevel(), route);
		helper.assertTrue(why == null, Component.literal("the ultra should take the route: " + why));
		double[] climb = {0, 0, 1e9, 0};   // sum of speeds, samples, slowest, fastest, over the ramp's middle
		helper.onEachTick(() -> {
			double z = relZ(helper, truck);
			if (z > 88 && z < 124 && truck.speed() > 0.1) {
				climb[0] += truck.speed();
				climb[1]++;
				climb[2] = Math.min(climb[2], truck.speed());
				climb[3] = Math.max(climb[3], truck.speed());
			}
		});
		helper.startSequence()
				.thenWaitUntil(() -> helper.assertTrue(relZ(helper, truck) < 80, Component.literal("it should climb the ramp: " + where(helper, truck))))
				.thenExecute(() -> {
					double mean = climb[0] / Math.max(1, climb[1]) * 3.6;
					System.out.printf(java.util.Locale.ROOT, "[tailings-test] pit_ultra_haul: up the 10 %% ramp loaded at %.1f km/h (%.1f to %.1f), %s%n", mean,
							climb[2] * 3.6, climb[3] * 3.6, truck.load().tonnes());
					helper.assertTrue(climb[1] > 100 && mean > 9.5 && mean < 12.5, Component.literal("about 11 km/h up 10 % loaded: " + mean + " km/h over " + climb[1] + " ticks"));
				})
				.thenWaitUntil(() -> helper.assertTrue(truck.lift() > 0 || hopper.pocket() > 0, Component.literal("it should turn round and back to the bumper: "
						+ where(helper, truck) + ", hopper behind " + (truck.hopperBehind(helper.getLevel()) != null))))
				.thenWaitUntil(() -> helper.assertTrue(truck.load().empty() && hopper.pocket() >= 134,
						Component.literal("363 t into the hopper, 134 bank blocks: " + hopper.pocket() + "; " + where(helper, truck))))
				.thenExecute(() -> System.out.println("[tailings-test] pit_ultra_haul: tipped; hopper " + hopper.pocket() + " blocks; " + where(helper, truck)))
				.thenSucceed();
	}

	/** The wheel loader cannot load the ultra: its body is 7.0 m up, over the bucket's 6 m ("too high to load", P5-6). */
	static void ultraLoader(GameTestHelper helper) {
		floor(helper, 0, 40, 1, 1, 0, 40);
		WheelLoaderEntity loader = vehicle(helper, TailingsEntities.WHEEL_LOADER, 20.5, 2, 8.5, 0);
		loader.setFuel(loader.tank());
		loader.bucket().add("minecraft:cobblestone", 20_000);
		loader.refreshLoad();
		// the truck across the loader's bucket, its body under the lip
		UltraHaulTruckEntity truck = vehicle(helper, TailingsEntities.ULTRA_HAUL_TRUCK, 20.5, 2, 23.5, 90);
		helper.startSequence()
				.thenIdle(5)
				.thenExecute(() -> {
					helper.assertTrue(loader.truckInReach(helper.getLevel()) == truck, Component.literal("the ultra is under the bucket"));
					loader.pressJump(helper.getLevel());
					helper.assertTrue(loader.status().startsWith("too high to load"), Component.literal("it should refuse: " + loader.status()));
					helper.assertTrue(truck.load().empty() && !loader.bucket().empty(), Component.literal("nothing loaded"));
				})
				.thenSucceed();
	}

	// ---- P5c: the hydraulic front shovel -----------------------------------------------------------------------------------------

	/** A block of muck, each block a whole bank block of cobblestone in quarters (3 of 4: the 4th went to the swell). */
	static void muck(GameTestHelper helper, int x0, int x1, int y0, int y1, int z0, int z1) {
		for (int x = x0; x <= x1; x++) {
			for (int z = z0; z <= z1; z++) {
				for (int y = y0; y <= y1; y++) {
					BlockPos pos = new BlockPos(x, y, z);
					helper.setBlock(pos, TailingsBlocks.MUCK);
					helper.getBlockEntity(pos, MuckBlockEntity.class).set(Map.of("minecraft:cobblestone", 3));
				}
			}
		}
	}

	/**
	 * In AUTO, the hydraulic shovel loads an ultra truck on its right from the muck face in front: 13 passes of 27 t (351 t; the 12 t of
	 * room left is under half a bucket, so the truck is loaded), then it swings to the ultra waiting on its left (double-side loading).
	 */
	static void hydShovel(GameTestHelper helper) {
		floor(helper, 0, 60, 1, 1, 0, 40);
		muck(helper, 26, 33, 2, 13, 10, 12);
		HydraulicShovelEntity shovel = vehicle(helper, TailingsEntities.HYDRAULIC_SHOVEL, 30.0, 2, 20.5, 180);   // facing north, the face ahead
		shovel.setFuel(shovel.tank());
		shovel.fastPasses(20);
		UltraHaulTruckEntity right = vehicle(helper, TailingsEntities.ULTRA_HAUL_TRUCK, 45.0, 2, 22.5, 180);
		UltraHaulTruckEntity left = vehicle(helper, TailingsEntities.ULTRA_HAUL_TRUCK, 15.0, 2, 22.5, 180);
		UltraHaulTruckEntity[] first = new UltraHaulTruckEntity[1];
		int[] passesInFirst = {0};
		helper.startSequence()
				.thenIdle(5)
				.thenExecute(() -> {
					helper.assertTrue(shovel.trucksInReach(helper.getLevel()).size() == 2, Component.literal("both trucks under its arc: " + shovel.trucksInReach(helper.getLevel()).size()));
					shovel.setAutoHere();
				})
				.thenWaitUntil(() -> helper.assertTrue(right.load().total() + left.load().total() > 0, Component.literal("it should dig and load: " + shovel.status())))
				.thenExecute(() -> first[0] = right.load().total() > 0 ? right : left)
				.thenWaitUntil(() -> helper.assertTrue(shovel.loadingTruck() != null && !shovel.loadingTruck().equals(first[0].getUUID()) || (first[0] == right ? left : right).load().total() > 0,
						Component.literal("it should fill the first truck, then turn to the other: " + shovel.status() + "; first " + first[0].load().tonnes())))
				.thenExecute(() -> {
					UltraHaulTruckEntity other = first[0] == right ? left : right;
					passesInFirst[0] = (int) (shovel.passes() - (other.load().total() > 0 ? 1 : 0));
					System.out.println("[tailings-test] pit_hyd_shovel: the first ultra holds " + first[0].load().tonnes() + " after " + passesInFirst[0] + " passes; "
							+ shovel.status());
					helper.assertTrue(passesInFirst[0] == 13, Component.literal("13 passes fill an ultra: " + passesInFirst[0]));
					helper.assertTrue(first[0].load().total() == 13 * 27_000L, Component.literal("13 x 27 t: " + first[0].load().tonnes()));
				})
				.thenWaitUntil(() -> helper.assertTrue((first[0] == right ? left : right).load().total() >= 27_000, Component.literal("and load the other side's: " + shovel.status())))
				.thenExecute(() -> helper.assertTrue(shovel.fuel() < shovel.tank(), Component.literal("on diesel it burns some")))
				.thenSucceed();
	}

	/**
	 * The hydraulic shovel's electric variant: with no cable it will not dig (NO CABLE); on an outlet it digs, its 860 kW motor drawing
	 * through the cable, and burns no diesel.
	 */
	static void hydElectric(GameTestHelper helper) {
		floor(helper, 0, 40, 1, 1, 0, 40);
		muck(helper, 16, 23, 2, 6, 8, 10);
		PitSwitchHouseBlockEntity house = house(helper, new BlockPos(2, 2, 30));
		HydraulicShovelEntity shovel = vehicle(helper, TailingsEntities.HYDRAULIC_SHOVEL, 20.0, 2, 18.5, 180);
		shovel.setElectric(true);
		shovel.setFuel(500);
		helper.startSequence()
				.thenIdle(3)
				.thenExecute(() -> {
					shovel.pressJump(helper.getLevel());
					helper.assertTrue(shovel.bucket().empty() && shovel.status().startsWith("NO CABLE"), Component.literal("no cable, no digging: " + shovel.status()));
					String why = couple(helper, house, shovel);
					helper.assertTrue(why == null, Component.literal("it should couple: " + why));
				})
				.thenIdle(3)
				.thenExecute(() -> helper.assertTrue(house.close(0) == null, Component.literal("its outlet should close")))
				.thenWaitUntil(() -> helper.assertTrue(shovel.cable().live(), Component.literal("power at the shovel: " + shovel.powerInfo())))
				.thenExecute(() -> shovel.pressJump(helper.getLevel()))
				.thenWaitUntil(() -> helper.assertTrue(shovel.digging() && house.watts(0) > 400_000, Component.literal("digging on its cable: " + shovel.status() + "; "
						+ house.inspect().getString())))
				.thenExecute(() -> {
					System.out.println("[tailings-test] pit_hyd_electric: " + shovel.status() + " | outlet " + Math.round(house.watts(0) / 1000) + " kW, " + Math.round(house.amps(0)) + " A");
					helper.assertTrue(shovel.bucket().total() == 27_000, Component.literal("a full bucket: " + shovel.bucket().tonnes()));
					helper.assertTrue(shovel.fuel() == 500, Component.literal("no diesel burnt"));
					helper.assertTrue(shovel.demand() <= 860_000 + 1, Component.literal("within its 860 kW"));
				})
				.thenSucceed();
	}

	// ---- P5d: the electric rope shovel --------------------------------------------------------------------------------------------

	/** The rope shovel's parts, facing north, its crawlers' main cell at {@code at}, all but {@code leaveOut}. */
	static RopeShovelBlockEntity ropeShovel(GameTestHelper helper, BlockPos at, RopeShovelLayout.Part leaveOut) {
		for (RopeShovelLayout.Part part : RopeShovelLayout.Part.values()) {
			if (part != leaveOut) {
				int[] o = part.offset();
				part(helper, TailingsBlocks.plantBlock(part.machine()), at.offset(o[0], o[1], o[2]), null);
			}
		}
		return helper.getBlockEntity(at, RopeShovelBlockEntity.class);
	}

	/** Built without its boom it reads NOT BUILT naming the boom; with it, NO CABLE until a trailing cable reaches it. */
	static void ropeShovelBuilt(GameTestHelper helper) {
		floor(helper, 0, 50, 1, 1, 0, 70);
		BlockPos at = new BlockPos(20, 2, 40);
		RopeShovelBlockEntity crawlers = ropeShovel(helper, at, RopeShovelLayout.Part.BOOM);
		String said = crawlers.run(helper.getLevel());
		helper.assertTrue(said.startsWith("NOT BUILT") && said.contains("Shovel Boom"), Component.literal("it should name the missing boom: " + said));
		int[] o = RopeShovelLayout.Part.BOOM.offset();
		part(helper, TailingsBlocks.SHOVEL_BOOM, at.offset(o[0], o[1], o[2]), null);
		helper.assertTrue(crawlers.check() == null, Component.literal("built: " + crawlers.check()));
		said = crawlers.run(helper.getLevel());
		helper.assertTrue(said.startsWith("NO CABLE"), Component.literal("no cable, no lift: " + said));
		helper.assertTrue(helper.getLevel().getEntitiesOfClass(RopeShovelEntity.class, new net.minecraft.world.phys.AABB(helper.absolutePos(at)).inflate(40)).isEmpty(),
				Component.literal("it stays as its parts"));
		helper.succeed();
	}

	/**
	 * Built and on a cable from a switch house, RUN lifts the rope shovel into one machine; in AUTO it fills an ultra on its right in 4
	 * passes from the muck face (3 x 99.9 t and a part dipper last), the outlet carrying its 3.5 MW hoist and 0.6 MW the rest of each
	 * cycle; STOP sets its parts back down, its cable still on.
	 */
	static void ropeShovelLoads(GameTestHelper helper) {
		floor(helper, 0, 75, 1, 1, 0, 90);
		BlockPos at = new BlockPos(20, 2, 40);
		RopeShovelBlockEntity crawlers = ropeShovel(helper, at, null);
		PitSwitchHouseBlockEntity house = house(helper, new BlockPos(62, 2, 75));
		ItemStack reel = new ItemStack(TailingsItems.TRAILING_CABLE);
		TrailingCableItem.pickUp(reel, house);
		long[] from = TrailingCableItem.from(reel);
		String why = crawlers.couple(helper.getLevel(), BlockPos.of(from[0]), (int) from[1]);
		helper.assertTrue(why == null, Component.literal("the crawlers should take the cable: " + why));
		UUID name = crawlers.machine();
		UltraHaulTruckEntity truck = vehicle(helper, TailingsEntities.ULTRA_HAUL_TRUCK, 50.0, 2, 46.0, 180);
		RopeShovelEntity[] shovel = new RopeShovelEntity[1];
		double[] most = {0}, rest = {0};
		helper.onEachTick(() -> {
			if (shovel[0] != null && house.state(0) == PitSwitchHouseBlockEntity.State.CLOSED) {
				most[0] = Math.max(most[0], house.watts(0));
				if (shovel[0].busy() && !shovel[0].digging() && house.watts(0) > 1000) {
					rest[0] = house.watts(0);
				}
			}
		});
		helper.startSequence()
				.thenIdle(3)
				.thenExecute(() -> {
					helper.assertTrue(house.close(0) == null, Component.literal("its outlet should close on the standing machine's cable"));
					String said = crawlers.run(helper.getLevel());
					helper.assertTrue(said.startsWith("lifted"), Component.literal("RUN should lift it: " + said));
					var found = helper.getLevel().getEntity(name);
					helper.assertTrue(found instanceof RopeShovelEntity, Component.literal("one machine, by the crawlers' name: " + found));
					shovel[0] = (RopeShovelEntity) found;
					helper.assertTrue(helper.getLevel().getBlockState(helper.absolutePos(at)).isAir(), Component.literal("its parts' cells are air"));
					// the face, where the boom lay: 10 wide, 4 deep in front of its crawlers
					muck(helper, 22, 31, 2, 14, 35, 38);
					shovel[0].fastPasses(4);
					shovel[0].setAutoHere();
				})
				.thenWaitUntil(() -> helper.assertTrue(truck.load().total() > 300_000 && shovel[0].loadingTruck() == null,
						Component.literal("it should fill the ultra: " + shovel[0].status() + "; truck " + truck.load().tonnes())))
				.thenExecute(() -> {
					System.out.printf(java.util.Locale.ROOT, "[tailings-test] pit_rope_shovel: %s after %d passes; outlet peak %.0f kW, the rest of the cycle %.0f kW%n",
							truck.load().tonnes(), shovel[0].passes(), most[0] / 1000, rest[0] / 1000);
					helper.assertTrue(shovel[0].passes() == 4, Component.literal("4 passes fill an ultra: " + shovel[0].passes()));
					helper.assertTrue(truck.load().room() < 50_000, Component.literal("the ultra full: " + truck.load().tonnes()));
					helper.assertTrue(Math.abs(most[0] - 3_500_000) < 350_000, Component.literal("the hoist's 3.5 MW on its cable: " + most[0]));
					helper.assertTrue(Math.abs(rest[0] - 600_000) < 100_000, Component.literal("0.6 MW the rest of its cycle: " + rest[0]));
					shovel[0].setAuto(helper.getLevel(), List.of());
				})
				.thenWaitUntil(() -> helper.assertTrue(!shovel[0].busy(), Component.literal("its pass done")))
				.thenExecute(() -> {
					// its boom, handle and dipper are laid down ahead of it: that ground must be clear, so the face's last muck goes
					for (int x = 22; x <= 31; x++) {
						for (int y = 2; y <= 14; y++) {
							for (int z = 35; z <= 38; z++) {
								helper.setBlock(x, y, z, net.minecraft.world.level.block.Blocks.AIR);
							}
						}
					}
					String said = shovel[0].stopAndSetDown(helper.getLevel());
					helper.assertTrue(said.startsWith("set down"), Component.literal("STOP should set it down: " + said));
					RopeShovelBlockEntity back = helper.getBlockEntity(at, RopeShovelBlockEntity.class);
					helper.assertTrue(back.check() == null && back.cable() != null && back.machine().equals(name),
							Component.literal("its parts back where they were, its cable on: " + back.status()));
				})
				.thenSucceed();
	}

	/**
	 * A switch house on a weak feed (5 ohms a phase behind it): an electric drill drilling draws its volts down under 90 % and stops,
	 * LOW VOLTAGE; its drives stay stopped 10 s before they try again.
	 */
	static void lowVoltage(GameTestHelper helper) {
		floor(helper, 0, 40, 1, 1, 0, 40);
		PitSwitchHouseBlockEntity house = house(helper, new BlockPos(4, 2, 4));
		house.forceSupply(true, 5.0);
		BlastholeDrillEntity drill = electricDrill(helper, 20.5, 2, 26.5, 0);
		String why = couple(helper, house, drill);
		helper.assertTrue(why == null, Component.literal("the drill should couple: " + why));
		helper.startSequence()
				.thenIdle(5)
				.thenExecute(() -> helper.assertTrue(house.close(0) == null, Component.literal("outlet 1 should close")))
				.thenWaitUntil(() -> helper.assertTrue(drill.cable().live(), Component.literal("standing, its volts are good: " + drill.powerInfo())))
				.thenExecute(() -> drill.pressJump(helper.getLevel()))
				.thenWaitUntil(() -> helper.assertTrue(drill.status().startsWith("LOW VOLTAGE"), Component.literal("drilling pulls the weak feed under 90 %: " + drill.status()
						+ "; " + house.inspect().getString())))
				.thenExecute(() -> {
					System.out.println("[tailings-test] pit_low_voltage: " + drill.status());
					helper.assertTrue(!drill.cable().live() && drill.demand() == 0, Component.literal("its drives stopped"));
				})
				.thenIdle(40)
				.thenExecute(() -> helper.assertTrue(!drill.cable().live(), Component.literal("and stay stopped a while: " + drill.powerInfo())))
				.thenSucceed();
	}

	/**
	 * The switch house on Wildcatter's own medium voltage, not the scenes' ideal supply: a 500 kW diesel set at the industrial voltage,
	 * stepped up by a unit substation run backward, medium-voltage cable to the house's north face. An electric drill on outlet 1 has
	 * power at the nominal volts.
	 */
	static void switchHouseFed(GameTestHelper helper) {
		BlockPos panel = new BlockPos(8, 2, 3);
		var set = TbmGapsGameTests.genset(helper, panel);
		net.minecraft.world.level.block.Block sub = net.minecraft.core.registries.BuiltInRegistries.BLOCK.getValue(net.minecraft.resources.Identifier.fromNamespaceAndPath("wildcatter", "unit_substation"));
		BlockPos main = panel.east(4);
		helper.setBlock(main, sub.defaultBlockState().setValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.HORIZONTAL_FACING, net.minecraft.core.Direction.SOUTH));
		dev.wildcatter.multiblock.BigParts.grow(helper.getLevel(), helper.absolutePos(main), helper.getLevel().getBlockState(helper.absolutePos(main)));
		TbmGapsGameTests.put(helper, "cable_mv", panel.east(5));
		TbmGapsGameTests.put(helper, "cable_mv", panel.east(6));
		TbmGapsGameTests.put(helper, "cable_mv", panel.east(7));
		floor(helper, 0, 40, 1, 1, 12, 40);
		PitSwitchHouseBlockEntity house = part(helper, TailingsBlocks.PIT_SWITCH_HOUSE, panel.east(7).south(), PitSwitchHouseBlockEntity.class);
		TbmGapsGameTests.start(helper, panel);
		BlastholeDrillEntity drill = electricDrill(helper, 26.5, 2, 26.5, 0);
		String why = couple(helper, house, drill);
		helper.assertTrue(why == null, Component.literal("the drill should couple: " + why));
		helper.runAfterDelay(400, () -> helper.succeedWhen(() -> {
			helper.assertTrue(set.breakerClosed(), Component.literal("the set should be on: " + set.inspect().getString()));
			helper.assertTrue(house.fed(), Component.literal("the medium voltage on the house's bus: " + house.inspect().getString()));
			if (house.state(0) != PitSwitchHouseBlockEntity.State.CLOSED) {
				house.close(0);
			}
			double nominal = Voltage.MEDIUM.lineToLine();
			helper.assertTrue(drill.cable().live() && Math.abs(house.volts(0) - nominal) < nominal * 0.05,
					Component.literal("the drill has power at about " + Math.round(nominal) + " V: " + house.inspect().getString() + "; " + drill.powerInfo()));
		}));
	}

	/**
	 * In AUTO, a shovel whose face is out of reach edges up to it a metre at a time, up to 5 m past where it was set to work, then digs
	 * (docs/questions/p3-blasting.md, 21: the self-running loader's plan, shared).
	 */
	static void shovelEdge(GameTestHelper helper) {
		floor(helper, 0, 60, 1, 1, 0, 40);
		muck(helper, 26, 33, 2, 5, 6, 8);   // 2 m beyond the bucket's 3 m
		HydraulicShovelEntity shovel = vehicle(helper, TailingsEntities.HYDRAULIC_SHOVEL, 30.0, 2, 20.5, 180);
		shovel.setFuel(shovel.tank());
		shovel.fastPasses(20);
		double startZ = shovel.getZ();
		shovel.setAutoHere();
		helper.startSequence()
				.thenWaitUntil(() -> helper.assertTrue(!shovel.bucket().empty(), Component.literal("it should edge up and dig: " + shovel.status() + " at "
						+ (startZ - shovel.getZ()) + " m on")))
				.thenExecute(() -> {
					double on = startZ - shovel.getZ();
					System.out.println("[tailings-test] pit_shovel_edge: edged " + on + " m and dug " + shovel.bucket().tonnes());
					helper.assertTrue(on >= 1.5 && on <= 5.01, Component.literal("a few metres, no more than 5: " + on));
				})
				.thenSucceed();
	}
}
