package dev.wildcatter.tailings.neoforge.gametest;

import static dev.wildcatter.tailings.neoforge.gametest.PitHaulGameTests.floor;
import static dev.wildcatter.tailings.neoforge.gametest.PitHaulGameTests.part;
import static dev.wildcatter.tailings.neoforge.gametest.PitHaulGameTests.vehicle;
import static dev.wildcatter.tailings.neoforge.gametest.PitHaulGameTests.where;

import dev.wildcatter.energy.Voltage;
import dev.wildcatter.tailings.TailingsBlocks;
import dev.wildcatter.tailings.TailingsEntities;
import dev.wildcatter.tailings.TailingsItems;
import dev.wildcatter.tailings.block.PitSwitchHouseBlockEntity;
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
	static final String[] SCENES = {"pit_switch_house", "pit_cable_end"};
	static final Map<String, Integer> TICKS = Map.of("pit_switch_house", 1200, "pit_cable_end", 1600);
	static final Map<String, Integer> PADDING = Map.of("pit_switch_house", 60, "pit_cable_end", 60);
	private static final Input FORWARD = new Input(true, false, false, false, false, false, false);

	private PitBigGameTests() {
	}

	static int[] reach(String scene) {
		return switch (scene) {
			case "pit_switch_house", "pit_cable_end" -> new int[] {-1, 3, -1, 3};
			default -> new int[] {-1, 2, -1, 2};
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
}
