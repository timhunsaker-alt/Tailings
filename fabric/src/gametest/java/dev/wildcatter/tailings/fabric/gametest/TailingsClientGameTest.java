package dev.wildcatter.tailings.fabric.gametest;

import dev.wildcatter.block.BigPartBlock;
import dev.wildcatter.multiblock.BigParts;
import dev.wildcatter.tailings.TailingsBlocks;
import dev.wildcatter.tailings.block.DewateringPumpBlock;
import dev.wildcatter.tailings.block.DrillBlockEntity;
import dev.wildcatter.tailings.entity.TbmEntity;
import dev.wildcatter.tailings.block.MineRailBlock;
import dev.wildcatter.tailings.block.RectifierBlockEntity;
import dev.wildcatter.tailings.entity.MineVehicleEntity;
import dev.wildcatter.tailings.mine.Heading;
import dev.wildcatter.tailings.mine.TbmPart;
import dev.wildcatter.tailings.plant.Machine;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;

/**
 * Every scene in a real game client, filmed: the first live look at what the NeoForge server scenes check (docs/ROADMAP.md, R7).
 * TAILINGS_ONLY=sceneA,sceneB runs just those. The films land in {@code fabric/build/scene-videos}, the screenshots in the run's
 * screenshots folder.
 */
public class TailingsClientGameTest implements FabricClientGameTest {
	private int y;

	@Override
	public void runTest(ClientGameTestContext context) {
		try (TestSingleplayerContext world = context.worldBuilder().create()) {
			TestServerContext server = world.getServer();
			context.waitTicks(40);
			y = server.computeOnServer(srv -> srv.overworld().getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, 0, 0));
			server.runCommand("time set noon");
			server.runCommand("gamemode spectator @p");
			if (only("parts")) {
				parts(context, world, server);
			}
			if (only("train")) {
				train(context, world, server);
			}
			if (only("tbm")) {
				tbm(context, world, server);
			}
		}
	}

	private static boolean only(String scene) {
		String only = System.getenv("TAILINGS_ONLY");
		return only == null || only.isBlank() || java.util.Arrays.asList(only.split(",")).contains(scene);
	}

	private static void part(ServerLevel level, Block block, BlockPos at) {
		BlockState state = block.defaultBlockState().setValue(BigPartBlock.FACING, Direction.NORTH);
		level.setBlock(at, state, Block.UPDATE_ALL);
		BigParts.grow(level, at, level.getBlockState(at));
	}

	private Filming film(ClientGameTestContext context, TestSingleplayerContext world, String scene) {
		context.waitTicks(40);
		return context.computeOnClient(client -> Filming.start(client, scene));
	}

	/** All the plant and machine parts in rows, the camera moving along them. */
	private void parts(ClientGameTestContext context, TestSingleplayerContext world, TestServerContext server) {
		int z0 = 40;
		server.runOnServer(srv -> {
			ServerLevel level = srv.overworld();
			int x = 0;
			for (Machine machine : Machine.values()) {
				part(level, TailingsBlocks.plantBlock(machine), new BlockPos(x, y, z0));
				x += machine.width() + 3;
			}
			x = 0;
			for (TbmPart part : TbmPart.values()) {
				part(level, TailingsBlocks.tbmBlock(part), new BlockPos(x, y, z0 + 22));
				x += part.width() + 4;
			}
		});
		server.runCommand("tp @p 4 " + (y + 9) + " " + (z0 + 19) + " 180 22");
		context.waitTicks(40);
		Filming film = film(context, world, "parts");
		for (int x = 4; x <= 112; x += 2) {
			server.runCommand("tp @p " + x + " " + (y + 9) + " " + (z0 + 19) + " 180 22");
			context.waitTicks(4);
		}
		context.takeScreenshot("tailings-parts");
		server.runCommand("tp @p 12 " + (y + 12) + " " + (z0 + 44) + " 180 25");
		context.waitTicks(60);
		context.takeScreenshot("tailings-tbm-parts");
		film.stop();
	}

	/** A locomotive and three cars, one loaded, glide along fed track to its end and back. */
	private void train(ClientGameTestContext context, TestSingleplayerContext world, TestServerContext server) {
		int x = 0;
		int z0 = 120;
		MineVehicleEntity[] loco = new MineVehicleEntity[1];
		server.runOnServer(srv -> {
			ServerLevel level = srv.overworld();
			BlockState rail = TailingsBlocks.MINE_RAIL.defaultBlockState().setValue(MineRailBlock.FACING, Direction.NORTH);
			for (int z = 0; z <= 40; z++) {
				level.setBlock(new BlockPos(x, y, z0 + z), rail, Block.UPDATE_ALL);
			}
			part(level, TailingsBlocks.TRACTION_RECTIFIER, new BlockPos(x - 2, y, z0 + 36));
			((RectifierBlockEntity) level.getBlockEntity(new BlockPos(x - 2, y, z0 + 36))).forceRunning();
		});
		server.runCommand("tp @p " + (x + 7) + " " + (y + 4) + " " + (z0 + 22) + " 90 15");
		context.waitTicks(30);
		server.runOnServer(srv -> {
			ServerLevel level = srv.overworld();
			loco[0] = MineVehicleEntity.placeLocomotive(level, new BlockPos(x, y, z0 + 30), Heading.NORTH);
			for (int i = 1; i <= 3; i++) {
				MineVehicleEntity car = MineVehicleEntity.placeCar(level, new BlockPos(x, y, z0 + 30 + i), Heading.NORTH);
				if (i == 1) {
					car.load().add("minecraft:cobblestone", 64);
					car.refreshLoad();
				}
			}
		});
		Filming film = film(context, world, "train");
		for (int i = 0; i < 40; i++) {
			context.waitTicks(10);
			double z = server.computeOnServer(srv -> loco[0].getZ());
			server.runCommand("tp @p " + (x + 7) + " " + (y + 4) + " " + ((int) z + 2) + " 90 15");
			if (i == 15) {
				context.takeScreenshot("tailings-train");
			}
		}
		film.stop();
	}

	/** The machine as blocks drives flat into a stone hill: the rings and track go down behind it. */
	/** Filmed from the side as the machine lifts and glides into the rock; on the flat it then stops and sets down. */
	private void tbm(ClientGameTestContext context, TestSingleplayerContext world, TestServerContext server) {
		drive(context, world, server, "tbm", 0, 0, 8);
		drive(context, world, server, "tbm_decline", 60, 8, 12);
	}

	private void drive(ClientGameTestContext context, TestSingleplayerContext world, TestServerContext server, String scene, int cx, int drop, int advances) {
		int cz = 220;
		BlockPos c = new BlockPos(cx, y + drop, cz);
		BlockPos beam = c.offset(3, 3, 3);
		server.runOnServer(srv -> {
			ServerLevel level = srv.overworld();
			for (int z = 1; z <= 30; z++) {
				for (int x = -2; x < 10; x++) {
					for (int yy = -drop - 2; yy < 10; yy++) {
						level.setBlock(c.offset(x, yy, -z), Blocks.STONE.defaultBlockState(), Block.UPDATE_CLIENTS);
					}
				}
			}
			part(level, TailingsBlocks.CUTTERHEAD, c);
			part(level, TailingsBlocks.SEGMENT_ERECTOR, c.offset(0, 0, 1));
			part(level, TailingsBlocks.MAIN_BEAM, beam);
			part(level, TailingsBlocks.TRACK_LAYER, c.offset(1, 2, 8));
			level.setBlock(c.offset(3, 3, 11), TailingsBlocks.DEWATERING_PUMP.defaultBlockState().setValue(DewateringPumpBlock.FACING, Direction.NORTH)
					.setValue(DewateringPumpBlock.PIECE, 0), Block.UPDATE_ALL);
			level.setBlock(c.offset(3, 0, 14), TailingsBlocks.CABLE_JUNCTION.defaultBlockState(), Block.UPDATE_ALL);
			DrillBlockEntity cab = (DrillBlockEntity) level.getBlockEntity(beam);
			cab.forceRunning();
			cab.program().setTargetY(c.getY() + 1 - drop);
			cab.program().setFlatLength(32);
			cab.setItem(DrillBlockEntity.SLOT_RAIL, new ItemStack(TailingsBlocks.MINE_RAIL, 64));
			cab.setItem(DrillBlockEntity.SLOT_GRAVEL, new ItemStack(Items.GRAVEL, 64));
			cab.setItem(DrillBlockEntity.SLOT_COBBLE, new ItemStack(Items.COBBLESTONE, 64));
			cab.setItem(DrillBlockEntity.SLOT_SEGMENTS, new ItemStack(TailingsBlocks.LINING, 64));
			cab.core().setTestTicks(40);
		});
		server.runCommand("tp @p " + (cx + 20) + " " + (c.getY() + 8) + " " + (cz + 8) + " 90 15");
		context.waitTicks(40);
		Filming film = film(context, world, scene);
		context.takeScreenshot("tailings-" + scene + "-built");
		server.runOnServer(srv -> ((DrillBlockEntity) srv.overworld().getBlockEntity(beam)).press(10, null));
		for (int t = 0; t < advances * 2; t++) {
			context.waitTicks(20);
			server.runOnServer(srv -> {
				for (TbmEntity machine : srv.overworld().getEntitiesOfClass(TbmEntity.class, new AABB(c).inflate(40))) {
					machine.setItem(DrillBlockEntity.SLOT_SEGMENTS, new ItemStack(TailingsBlocks.LINING, 64));
					System.out.println("[tailings-test] " + scene + " " + machine.position() + " pitch " + machine.pitch() + ": " + machine.core().readout());
				}
			});
			if (t == advances) {
				context.takeScreenshot("tailings-" + scene + "-gliding");
			}
		}
		context.takeScreenshot("tailings-" + scene + "-driven");
		server.runOnServer(srv -> {
			for (TbmEntity machine : srv.overworld().getEntitiesOfClass(TbmEntity.class, new AABB(c).inflate(40))) {
				machine.core().setRunning(false);
			}
		});
		context.waitTicks(80);
		context.takeScreenshot("tailings-" + scene + "-stopped");
		server.runCommand("tp @p " + (cx + 4) + " " + (c.getY() + 4) + " " + (cz + 26) + " 180 10");
		context.waitTicks(40);
		context.takeScreenshot("tailings-" + scene + "-behind");
		film.stop();
	}
}
