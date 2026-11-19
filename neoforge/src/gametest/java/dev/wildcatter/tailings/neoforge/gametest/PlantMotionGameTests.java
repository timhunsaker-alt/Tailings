package dev.wildcatter.tailings.neoforge.gametest;

import dev.wildcatter.tailings.block.PlantBlockEntity;
import dev.wildcatter.tailings.plant.Machine;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;

/**
 * The crushers' and apron feeders' moving parts (PlantRenderer) move while the part runs and stop when it stops: the state the
 * renderer reads, {@link PlantBlockEntity#moving()}, and what the server sends the client of it. A part commanded on with no power
 * does not move; one running does, work or none; stopped, it does not.
 */
final class PlantMotionGameTests {
	static final String[] SCENES = {"jaw_moves", "gyratory_moves", "apron_feeder_moves", "primary_apron_moves", "reclaim_feeder_moves"};

	private PlantMotionGameTests() {
	}

	static void register(BiConsumer<String, Consumer<GameTestHelper>> out) {
		out.accept("jaw_moves", helper -> moves(helper, Machine.JAW));
		out.accept("gyratory_moves", helper -> moves(helper, Machine.GYRATORY_CRUSHER));
		out.accept("apron_feeder_moves", helper -> moves(helper, Machine.APRON_FEEDER));
		out.accept("primary_apron_moves", helper -> moves(helper, Machine.PRIMARY_APRON_FEEDER));
		out.accept("reclaim_feeder_moves", helper -> moves(helper, Machine.RECLAIM_FEEDER));
	}

	private static boolean sent(GameTestHelper helper, PlantBlockEntity part) {
		return part.getUpdateTag(helper.getLevel().registryAccess()).getBooleanOr("moving", false);
	}

	private static void moves(GameTestHelper helper, Machine machine) {
		BlockPos at = new BlockPos(1, 2, 1);
		PlantBlockEntity part = TailingsGameTests.plant(helper, machine, at);
		helper.assertTrue(dev.wildcatter.tailings.plant.PlantMotion.rpm(machine) > 0, Component.literal(machine + " has moving parts drawn"));
		// in AUTO and commanded on by a PLC, but with no supply its motor does not turn
		BlockPos cell = helper.absolutePos(at);
		part.cycleHoa(cell);
		part.cycleHoa(cell);
		part.commandPoint(0, true, helper.getLevel().getGameTime());
		helper.startSequence()
				.thenIdle(5)
				.thenExecute(() -> {
					helper.assertFalse(part.running(), Component.literal("unpowered, it should not run"));
					helper.assertFalse(part.moving() || sent(helper, part), Component.literal("commanded with no power, its parts should stay still"));
					part.forceRunning();
				})
				.thenIdle(3)
				.thenExecute(() -> {
					helper.assertTrue(part.moving(), Component.literal("running, its parts should move (" + part.status() + ")"));
					helper.assertTrue(sent(helper, part), Component.literal("and the client should be told"));
					part.stopForced();
				})
				.thenIdle(3)
				.thenExecute(() -> {
					helper.assertFalse(part.moving(), Component.literal("stopped, its parts should stop"));
					helper.assertFalse(sent(helper, part), Component.literal("and the client should be told that too"));
				})
				.thenSucceed();
	}
}
