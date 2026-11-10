package dev.wildcatter.tailings.neoforge.gametest;

import dev.wildcatter.tailings.TailingsBlocks;
import dev.wildcatter.tailings.TailingsItems;
import dev.wildcatter.tailings.block.DrillBlockEntity;
import dev.wildcatter.tailings.block.PlantBlockEntity;
import dev.wildcatter.tailings.block.TbmCore;
import dev.wildcatter.tailings.block.VentDuctBlock;
import dev.wildcatter.tailings.block.VentNetwork;
import dev.wildcatter.tailings.entity.GantryEntity;
import dev.wildcatter.tailings.entity.TbmEntity;
import dev.wildcatter.tailings.mine.Airflow;
import dev.wildcatter.tailings.mine.DrillCodes;
import dev.wildcatter.tailings.mine.TbmGantry;
import dev.wildcatter.tailings.mine.TunnelDrill;
import dev.wildcatter.tailings.plant.Machine;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

/**
 * Server scenes for the drive's ventilation (docs/multiblocks/ventilation.md, L1; decisions in docs/questions/l1-ventilation.md): a fan
 * and 200 m of duct keep a TBM running and the interlock stops it with the fan off; the reel gantry's cassette pays duct out; a player
 * in stale air; a booster on 3.5 km of duct; and a world with the rules off (the default) runs as before.
 */
final class VentGameTests {
	static final String[] SCENES = {"vent_fresh", "vent_cassette", "vent_stale_player", "vent_booster", "vent_default_off", "vent_parts"};
	static final Set<String> LONG = Set.of("vent_fresh", "vent_cassette", "vent_stale_player", "vent_default_off");
	private static final Map<String, Integer> PADDING = Map.of("vent_fresh", 80, "vent_cassette", 80, "vent_stale_player", 80, "vent_default_off", 60);

	private VentGameTests() {
	}

	static void register(BiConsumer<String, Consumer<GameTestHelper>> out) {
		out.accept("vent_fresh", VentGameTests::fresh);
		out.accept("vent_cassette", VentGameTests::cassette);
		out.accept("vent_stale_player", VentGameTests::stalePlayer);
		out.accept("vent_booster", VentGameTests::booster);
		out.accept("vent_default_off", VentGameTests::defaultOff);
		out.accept("vent_parts", VentGameTests::parts);
	}

	static int padding(String scene, int otherwise) {
		return PADDING.getOrDefault(scene, otherwise);
	}

	static int ticks(String scene, int otherwise) {
		return scene.equals("vent_stale_player") ? 2400 : otherwise;
	}

	private static void force(GameTestHelper helper, int dx0, int dx1, int dz0, int dz1) {
		ChunkPos origin = ChunkPos.containing(helper.absolutePos(BlockPos.ZERO));
		for (int dx = dx0; dx <= dx1; dx++) {
			for (int dz = dz0; dz <= dz1; dz++) {
				helper.getLevel().setChunkForced(origin.x() + dx, origin.z() + dz, true);
			}
		}
	}

	private static void duct(GameTestHelper helper, BlockPos at) {
		helper.setBlock(at, TailingsBlocks.VENT_DUCT);
	}

	/** A fan facing north, its main cell at {@code at}, forced on: its outlet is south of its back cells, at z + 5. */
	private static PlantBlockEntity fan(GameTestHelper helper, BlockPos at) {
		PlantBlockEntity fan = TailingsGameTests.plant(helper, Machine.VENT_FAN, at);
		fan.forceRunning();
		return fan;
	}

	/**
	 * A fan and a 200 m line of duct laid back and forth above the back-up, its end within 30 m of the back-up's tail: the TBM reads
	 * fresh air and drives with the interlock on; the fan stopped, the air goes and the TBM stops, NO VENTILATION.
	 */
	private static void fresh(GameTestHelper helper) {
		force(helper, -2, 3, -1, 4);
		BlockPos c = new BlockPos(4, 40, 20);
		DrillBlockEntity cab = TailingsGameTests.tbm(helper, c, 24, true);
		TailingsGameTests.feed(helper, c);
		PlantBlockEntity fan = fan(helper, new BlockPos(-12, 50, 30));
		int laid = 0;
		for (int z = 35; z <= 40; z++, laid++) {
			duct(helper, new BlockPos(-11, 51, z));
		}
		boolean east = true;
		for (int z = 40; z <= 50 && laid < 200; z += 2) {
			for (int i = z == 40 ? 1 : 0; i < 38 && laid < 200; i++, laid++) {
				duct(helper, new BlockPos(east ? -11 + i : 26 - i, 51, z));
			}
			if (z < 50 && laid < 200) {
				duct(helper, new BlockPos(east ? 26 : -11, 51, z + 1));
				laid++;
			}
			east = !east;
		}
		final int metres = laid;
		cab.core().setTestInterlock(true, 60);
		TailingsGameTests.go(cab, 20);
		int[] at = {0};
		helper.runAfterDelay(120, () -> {
			helper.assertTrue(fan.vent().ductCells() == metres && metres >= 200, Component.literal("the fan should read its " + metres + " m of duct, got " + fan.vent().ductCells()));
			Airflow.Result air = fan.vent().air();
			helper.assertTrue(air.enough(), Component.literal("200 m of duct should carry 10.8 m3/s, got " + air.endFlow()));
			TbmEntity machine = TailingsGameTests.machine(helper, c);
			helper.assertTrue(machine != null && machine.core().air() >= Airflow.NEED, Component.literal("the back-up should read fresh air, got " + (machine == null ? "no machine" : machine.core().airLine())));
			helper.assertTrue(machine.core().status() == DrillCodes.DRIVING && machine.core().drill().advanceCount() >= 3,
					Component.literal("it should drive in fresh air, at " + machine.core().readout()));
			fan.stopForced();
		});
		helper.runAfterDelay(260, () -> {
			TbmEntity machine = TailingsGameTests.machine(helper, c);
			helper.assertTrue(machine.core().status() == DrillCodes.NO_VENTILATION, Component.literal("with the fan off it should stop NO VENTILATION, at " + machine.core().readout()));
			at[0] = machine.core().drill().advanceCount();
		});
		helper.runAfterDelay(320, () -> {
			TbmEntity machine = TailingsGameTests.machine(helper, c);
			helper.assertTrue(machine.core().drill().advanceCount() == at[0], Component.literal("and cut no more"));
			fan.forceRunning();
			helper.succeedWhen(() -> helper.assertTrue(machine.core().status() == DrillCodes.DRIVING && machine.core().drill().advanceCount() > at[0],
					Component.literal("air back, it drives on, at " + machine.core().readout())));
		});
	}

	/**
	 * A cassette clicked onto the reel gantry; the hand-laid duct from a fan ends where the cassette's first metre goes. Each block it
	 * advances, the machine pays a metre out at the crown, joined to the line, until the cassette is empty; the fan's line grows by each.
	 */
	private static void cassette(GameTestHelper helper) {
		force(helper, -1, 3, -1, 4);
		BlockPos c = new BlockPos(4, 40, 20);
		DrillBlockEntity cab = TailingsGameTests.tbm(helper, c, 24, true);
		TailingsGameTests.feed(helper, c);
		helper.runAfterDelay(2, () -> cassetteRun(helper, c, cab));
	}

	private static void cassetteRun(GameTestHelper helper, BlockPos c, DrillBlockEntity cab) {
		GantryEntity reel = TailingsGameTests.gantry(helper, cab, TbmGantry.REEL);
		helper.assertTrue(reel != null, Component.literal("the reel gantry should be coupled"));
		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
		player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(TailingsItems.DUCT_CASSETTE));
		reel.interact(player, InteractionHand.MAIN_HAND, Vec3.ZERO);
		helper.assertTrue(cab.core().ductLeft() == Airflow.CASSETTE_METRES, Component.literal("the cassette should load 100 m, got " + cab.core().ductLeft()));
		helper.assertTrue(player.getMainHandItem().isEmpty(), Component.literal("and be used up"));
		cab.core().setDuctLeft(6);   // the test's ground holds a few metres of drive, not 100
		TailingsGameTests.go(cab, 10);
		TunnelDrill drill = cab.core().drill();
		BlockPos end = helper.relativePos(new BlockPos(drill.worldX(TbmCore.DUCT_ACROSS, -TbmCore.DUCT_ROW), drill.worldY(TbmCore.DUCT_UP, -TbmCore.DUCT_ROW),
				drill.worldZ(TbmCore.DUCT_ACROSS, -TbmCore.DUCT_ROW)));
		for (int z = 0; z <= 9; z++) {
			duct(helper, end.offset(0, 0, z));
		}
		for (int x = 1; x <= 4; x++) {
			duct(helper, end.offset(x, 0, 9));
		}
		PlantBlockEntity fan = fan(helper, end.offset(3, -1, 4));
		List<BlockPos> paid = new ArrayList<>();
		for (int i = 1; i <= 6; i++) {
			paid.add(end.offset(0, 0, -i));
		}
		helper.succeedWhen(() -> {
			TbmEntity machine = TailingsGameTests.machine(helper, c);
			helper.assertTrue(machine != null && machine.core().ductLeft() == 0, Component.literal("the cassette should run empty, at " + (machine == null ? "none" : machine.core().airLine())));
			for (BlockPos at : paid) {
				helper.assertTrue(helper.getBlockState(at).getBlock() instanceof VentDuctBlock, Component.literal("a metre of duct paid out at " + at));
			}
			helper.assertTrue(!(helper.getBlockState(end.offset(0, 0, -7)).getBlock() instanceof VentDuctBlock), Component.literal("and none past the cassette's end"));
			helper.assertTrue(fan.vent().ductCells() == 14 + 6, Component.literal("the fan's line takes in the paid-out duct, got " + fan.vent().ductCells()));
			helper.assertTrue(helper.absolutePos(paid.get(5)).equals(fan.vent().end()), Component.literal("its end is the last metre paid out, got " + fan.vent().end()));
			helper.assertTrue(machine.core().air() >= Airflow.NEED && machine.core().readout().contains("duct cassette 0 m"), Component.literal(machine.core().readout()));
		});
	}

	/**
	 * A drive's floor under a roof, a fan's duct ending at one end: a player 60 m past the duct's end gets Mining Fatigue after 30 s of
	 * its stale air, and walking back into fresh air clears it within 10 s.
	 */
	private static void stalePlayer(GameTestHelper helper) {
		force(helper, -1, 5, -1, 1);
		int y = 4;
		for (int x = 0; x <= 70; x++) {
			for (int z = 0; z <= 2; z++) {
				helper.setBlock(new BlockPos(x, y, z), TailingsBlocks.MINE_FLOOR);
				helper.setBlock(new BlockPos(x, y + 5, z), Blocks.STONE);
			}
		}
		fan(helper, new BlockPos(-1, y + 1, -10));
		for (int z = -5; z <= 1; z++) {
			duct(helper, new BlockPos(0, y + 3, z));
		}
		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		VentNetwork.TEST_STALE.add(player.getUUID());
		BlockPos far = helper.absolutePos(new BlockPos(60, y + 1, 1));
		BlockPos near = helper.absolutePos(new BlockPos(10, y + 1, 1));
		player.teleportTo(far.getX() + 0.5, far.getY(), far.getZ() + 0.5);
		helper.runAfterDelay(400, () -> {
			helper.assertTrue(VentNetwork.inDrive(helper.getLevel(), player.blockPosition()), Component.literal("the player should be in a drive at " + player.blockPosition()));
			helper.assertTrue(VentNetwork.airAt(helper.getLevel(), near).fresh(), Component.literal("10 m from the duct's end should be fresh"));
			helper.assertTrue(!VentNetwork.airAt(helper.getLevel(), player.blockPosition()).fresh(), Component.literal("60 m past it is stale"));
			helper.assertTrue(!player.hasEffect(MobEffects.MINING_FATIGUE), Component.literal("no fatigue before 30 s, at " + VentNetwork.staleSeconds(player) + " s"));
		});
		helper.runAfterDelay(700, () -> {
			helper.assertTrue(player.hasEffect(MobEffects.MINING_FATIGUE), Component.literal("Mining Fatigue after 30 s of stale air, at " + VentNetwork.staleSeconds(player) + " s"));
			helper.assertTrue(!player.hasEffect(MobEffects.WEAKNESS), Component.literal("not yet Weakness"));
			player.teleportTo(near.getX() + 0.5, near.getY(), near.getZ() + 0.5);
		});
		helper.runAfterDelay(900, () -> {
			helper.assertTrue(!player.hasEffect(MobEffects.MINING_FATIGUE), Component.literal("cleared within 10 s of fresh air"));
			VentNetwork.TEST_STALE.remove(player.getUUID());
			helper.succeed();
		});
	}

	/** 3.5 km of duct falls short on one fan; a booster in the line, running, brings it to 10.8 m3/s. */
	private static void booster(GameTestHelper helper) {
		BlockPos f = new BlockPos(2, 2, 0);
		PlantBlockEntity fan = fan(helper, f);
		for (int z = 5; z <= 9; z++) {
			duct(helper, f.offset(1, 1, z));
		}
		PlantBlockEntity booster = TailingsGameTests.plant(helper, Machine.BOOSTER_FAN, f.offset(1, 1, 10));
		for (int z = 13; z <= 22; z++) {
			duct(helper, f.offset(1, 1, z));
		}
		fan.vent().setExtraMetres(3_500 - 18);
		helper.startSequence()
				.thenExecuteAfter(30, () -> {
					helper.assertTrue(fan.vent().ductCells() == 15, Component.literal("the line runs through the booster, got " + fan.vent().ductCells() + " cells"));
					helper.assertTrue(fan.vent().air().metres() >= 3_500, Component.literal("3.5 km, got " + fan.vent().air().metres()));
					helper.assertTrue(!fan.vent().air().enough(), Component.literal("one fan should fall short on 3.5 km, got " + fan.vent().air().endFlow()));
					booster.forceRunning();
				})
				.thenWaitUntil(() -> helper.assertTrue(fan.vent().air().enough(), Component.literal("with the booster running it should reach 10.8 m3/s, got " + fan.vent().air().endFlow())))
				.thenSucceed();
	}

	/**
	 * With nothing in config/tailings-ventilation.properties (every world before L1), a TBM with no ventilation at all reads POOR AIR
	 * and drives on past the grace, as it always has.
	 */
	private static void defaultOff(GameTestHelper helper) {
		BlockPos c = new BlockPos(4, 40, 20);
		DrillBlockEntity cab = TailingsGameTests.tbm(helper, c, 24, true);
		TailingsGameTests.feed(helper, c);
		cab.core().setTestInterlock(false, 20);
		TailingsGameTests.go(cab, 10);
		helper.runAfterDelay(200, () -> {
			TbmEntity machine = TailingsGameTests.machine(helper, c);
			helper.assertTrue(machine != null && machine.core().air() == 0, Component.literal("no fan: no air at the back-up"));
			helper.assertTrue(machine.core().readout().contains("POOR AIR"), Component.literal("it reads POOR AIR, got " + machine.core().readout()));
			helper.assertTrue(machine.core().status() != DrillCodes.NO_VENTILATION && machine.core().drill().advanceCount() >= 8,
					Component.literal("and drives on with the interlock off, at " + machine.core().readout()));
			helper.succeed();
		});
	}

	/** The parts are their spec's size, duct grows arms toward duct and the fans, and the fan's PLC FLOW reads its air. */
	private static void parts(GameTestHelper helper) {
		PlantBlockEntity fan = fan(helper, new BlockPos(1, 2, 1));
		helper.assertTrue(fan.cells().size() == 45, Component.literal("the fan is 3 x 3 x 5"));
		duct(helper, new BlockPos(2, 3, 6));
		duct(helper, new BlockPos(2, 3, 7));
		helper.startSequence()
				.thenWaitUntil(() -> {
					var state = helper.getBlockState(new BlockPos(2, 3, 6));
					helper.assertTrue(state.getValue(dev.wildcatter.block.WireShape.ARMS.get(net.minecraft.core.Direction.NORTH))
							&& state.getValue(dev.wildcatter.block.WireShape.ARMS.get(net.minecraft.core.Direction.SOUTH)), Component.literal("the duct joins the fan and the next duct, got " + state));
				})
				.thenWaitUntil(() -> helper.assertTrue(fan.vent().air().fanFlow() > 20, Component.literal("near free delivery on 2 m of duct, got " + fan.vent().air().fanFlow())))
				.thenExecute(() -> {
					double ma = fan.readMa(6);
					helper.assertTrue(ma > 20 && ma <= 20.5, Component.literal("FLOW at the top of its 15 m3/s span, got " + ma));
					helper.assertTrue(fan.points().get(6).name().startsWith("FLOW"), Component.literal("its seventh point is FLOW"));
				})
				.thenSucceed();
	}
}
