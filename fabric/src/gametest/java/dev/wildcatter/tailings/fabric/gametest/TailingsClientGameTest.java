package dev.wildcatter.tailings.fabric.gametest;

import dev.wildcatter.block.BigPartBlock;
import java.util.List;
import net.fabricmc.fabric.api.client.gametest.v1.world.TestWorldSave;
import net.minecraft.server.level.ServerPlayer;
import dev.wildcatter.fluid.Product;
import dev.wildcatter.fluid.SlurryAssay;
import dev.wildcatter.tailings.Tailings;
import dev.wildcatter.tailings.block.BeltConveyorBlock;
import dev.wildcatter.tailings.block.PlantBlockEntity;
import dev.wildcatter.tailings.plant.Reagent;
import dev.wildcatter.tailings.rock.Mineral;
import dev.wildcatter.tailings.rock.Rock;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import dev.wildcatter.multiblock.BigParts;
import dev.wildcatter.tailings.TailingsBlocks;
import dev.wildcatter.tailings.block.DewateringPumpBlock;
import dev.wildcatter.tailings.block.DrillBlockEntity;
import dev.wildcatter.tailings.entity.GantryEntity;
import dev.wildcatter.tailings.entity.MineVehicleEntity;
import dev.wildcatter.tailings.entity.TbmEntity;
import dev.wildcatter.tailings.mine.TbmGantry;
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
	/** Every sound the client started: what the scenes check the machines' sounds against, since nobody can listen in a test. */
	/** Counted by sound instance, since the game re-announces a playing loop to its listeners many times. */
	private static final java.util.Map<String, java.util.Set<Integer>> HEARD = new java.util.concurrent.ConcurrentHashMap<>();

	private static void heard(String scene, String... sounds) {
		java.util.List<String> missing = new java.util.ArrayList<>();
		for (String sound : sounds) {
			if (!HEARD.containsKey(sound)) {
				missing.add(sound);
			}
		}
		System.out.println("[tailings-test] " + scene + " heard " + new java.util.TreeMap<>(HEARD).entrySet().stream().filter(h -> h.getKey().startsWith("tailings:") || h.getKey().startsWith("wildcatter:")).map(h -> h.getKey() + "=" + h.getValue().size()).toList());
		require(missing.isEmpty(), scene + " should have sounded " + missing);
		HEARD.clear();
	}

	@Override
	public void runTest(ClientGameTestContext context) {
		// the playthrough is its own long run in its own real world: only when named
		if ("playthrough".equals(System.getenv("TAILINGS_ONLY"))) {
			new Playthrough(context).run();
			return;
		}
		context.runOnClient(client -> client.getSoundManager().addListener((sound, accessor, range) -> HEARD.computeIfAbsent(sound.getIdentifier().toString(), id -> java.util.concurrent.ConcurrentHashMap.newKeySet()).add(System.identityHashCode(sound))));
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
			if (only("crushing")) {
				crushing(context, world, server);
			}
			if (only("flotation")) {
				flotation(context, world, server);
			}
			if (only("spirals")) {
				spirals(context, world, server);
			}
			if (only("recovery")) {
				recovery(context, world, server);
			}
			if (only("selective")) {
				selective(context, world, server);
			}
			if (only("dump")) {
				dump(context, world, server);
			}
			if (only("haul")) {
				haul(context, world, server);
			}
			if (only("pit")) {
				pit(context, world, server);
			}
		}
		if (only("hand")) {
			hand(context);
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
				MineVehicleEntity car = MineVehicleEntity.placeCar(level, new BlockPos(x, y, z0 + 30 + 3 + (int) ((i - 1) * 2.5)), Heading.NORTH);
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
		drive(context, world, server, "tbm_decline", 60, 14, 30);
	}

	private void drive(ClientGameTestContext context, TestSingleplayerContext world, TestServerContext server, String scene, int cx, int drop, int advances) {
		int cz = 220;
		BlockPos c = new BlockPos(cx, y + drop, cz);
		BlockPos beam = c.offset(3, 3, 3);
		server.runOnServer(srv -> {
			ServerLevel level = srv.overworld();
			for (int z = 1; z <= 36; z++) {
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
			level.setBlock(c.offset(-2, 0, 14), TailingsBlocks.CABLE_JUNCTION.defaultBlockState(), Block.UPDATE_ALL);
			BlockState track = TailingsBlocks.MINE_RAIL.defaultBlockState().setValue(MineRailBlock.FACING, Direction.NORTH);
			for (int z = 11; z <= 40; z++) {
				level.setBlock(c.offset(2, 1, z), track, Block.UPDATE_ALL);
				level.setBlock(c.offset(5, 1, z), track, Block.UPDATE_ALL);
			}
			DrillBlockEntity cab = (DrillBlockEntity) level.getBlockEntity(beam);
			cab.forceRunning();
			cab.program().setTargetY(c.getY() + 1 - drop);
			cab.program().setFlatLength(32);
			cab.setItem(DrillBlockEntity.SLOT_RAIL, new ItemStack(TailingsBlocks.MINE_RAIL, 64));
			cab.setItem(DrillBlockEntity.SLOT_GRAVEL, new ItemStack(Items.GRAVEL, 64));
			cab.setItem(DrillBlockEntity.SLOT_COBBLE, new ItemStack(Items.COBBLESTONE, 64));
			cab.setItem(DrillBlockEntity.SLOT_SEGMENTS, new ItemStack(TailingsBlocks.LINING, 64));
			cab.core().setTestTicks(40);
			for (TbmGantry kind : TbmGantry.values()) {
				GantryEntity gantry = GantryEntity.place(level, c.offset(2, 1, kind.row()), kind, Direction.NORTH);
				System.out.println("[tailings-test] " + kind.displayName() + ": " + gantry.couple());
			}
			MineVehicleEntity.placeCar(level, c.offset(2, 1, 28), dev.wildcatter.tailings.mine.Heading.NORTH);
		});
		// the decline is filmed in section: no rock or lining on the camera's side, so the train can be seen going over the crest
		boolean section = drop > 0;
		if (section) {
			server.runOnServer(srv -> cutAway(srv.overworld(), c, drop));
			server.runCommand("tp @p " + (cx + 34) + " " + (c.getY() - 3) + " " + (cz - 4) + " 90 0");
		} else {
			server.runCommand("tp @p " + (cx + 24) + " " + (c.getY() + 8) + " " + (cz + 14) + " 90 15");
		}
		context.waitTicks(40);
		Filming film = film(context, world, scene);
		context.takeScreenshot("tailings-" + scene + "-built");
		server.runOnServer(srv -> ((DrillBlockEntity) srv.overworld().getBlockEntity(beam)).press(10, null));
		for (int t = 0; t < advances * 2; t++) {
			context.waitTicks(20);
			server.runOnServer(srv -> {
				if (section) {
					cutAway(srv.overworld(), c, drop);
				}
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
		server.runCommand("tp @p " + (cx + 4) + " " + (c.getY() + 4) + " " + (cz + 46) + " 180 10");
		context.waitTicks(40);
		context.takeScreenshot("tailings-" + scene + "-behind");
		film.stop();
		if (section) {   // the camera stands off further than the one-shots carry
			heard(scene, "tailings:tbm_cutter", "tailings:belt_conveyor", "wildcatter:motor_hum");
		} else {
			heard(scene, "tailings:tbm_cutter", "tailings:ring_set", "tailings:muck_load", "tailings:belt_conveyor", "wildcatter:motor_hum", "wildcatter:structure_formed");
		}
	}

	private static PlantBlockEntity plant(ServerLevel level, Machine machine, BlockPos at) {
		part(level, TailingsBlocks.plantBlock(machine), at);
		return (PlantBlockEntity) level.getBlockEntity(at);
	}

	private static BlockState belt(Direction.Axis axis) {
		return TailingsBlocks.BELT_CONVEYOR.defaultBlockState().setValue(BeltConveyorBlock.AXIS, axis);
	}

	private static void pipe(ServerLevel level, BlockPos... at) {
		Block steel = BuiltInRegistries.BLOCK.getValue(Identifier.fromNamespaceAndPath("wildcatter", "gas_line"));
		for (BlockPos pos : at) {
			level.setBlock(pos, steel.defaultBlockState(), Block.UPDATE_ALL);
		}
	}

	/** The crushing circuit running on copper ore, its parts laid out as crushing-circuit.md's game test has them. */
	private void crushing(ClientGameTestContext context, TestSingleplayerContext world, TestServerContext server) {
		BlockPos o = new BlockPos(0, y + 3, 340);
		PlantBlockEntity[][] parts = new PlantBlockEntity[1][];
		server.runOnServer(srv -> {
			ServerLevel level = srv.overworld();
			PlantBlockEntity jaw = plant(level, Machine.JAW, o);
			PlantBlockEntity grizzly = plant(level, Machine.GRIZZLY, o.offset(0, 4, 0));
			for (BlockPos at : new BlockPos[] {o.offset(0, 0, 4), o.offset(1, 0, 4), o.offset(2, 0, 4), o.offset(3, 0, 4), o.offset(4, 1, 4), o.offset(5, 2, 4)}) {
				level.setBlock(at, belt(Direction.Axis.X), Block.UPDATE_ALL);
			}
			PlantBlockEntity jawDrive = plant(level, Machine.CONVEYOR_DRIVE, o.offset(6, 2, 4));
			PlantBlockEntity screen = plant(level, Machine.SCREEN, o.offset(3, 3, 0));
			PlantBlockEntity cone = plant(level, Machine.CONE, o.offset(3, 3, 6));
			for (int x = 6; x <= 8; x++) {
				level.setBlock(o.offset(x, 3, 1), belt(Direction.Axis.X), Block.UPDATE_ALL);
			}
			PlantBlockEntity sandDrive = plant(level, Machine.CONVEYOR_DRIVE, o.offset(9, 3, 1));
			PlantBlockEntity mill = plant(level, Machine.BALL_MILL, o.offset(7, -1, 0));
			PlantBlockEntity cyclone = plant(level, Machine.CYCLONE, o.offset(11, -1, 2));
			pipe(level, o.offset(10, -1, 6), o.offset(11, -1, 6), o.offset(12, -1, 6), o.offset(13, -1, 6), o.offset(13, -1, 5), o.offset(13, -1, 4),
					o.offset(13, -1, 3), o.offset(13, -1, 2), o.offset(13, -1, 1), o.offset(12, -1, 1));
			for (PlantBlockEntity part : new PlantBlockEntity[] {jaw, screen, cone, jawDrive, sandDrive, mill, cyclone}) {
				part.forceRunning();
			}
			parts[0] = new PlantBlockEntity[] {grizzly, jaw, screen, cone, mill, cyclone};
		});
		server.runCommand("tp @p 19 " + (y + 11) + " 358 135 22");
		context.waitTicks(40);
		Filming film = film(context, world, "crushing");
		context.takeScreenshot("tailings-crushing-built");
		for (int t = 0; t < 30; t++) {
			int second = t;
			server.runOnServer(srv -> {
				PlantBlockEntity[] p = parts[0];
				p[4].addWater(1000);
				p[5].addWater(1000);
				if (second % 4 == 0) {
					p[0].offer(new ItemStack(Blocks.COPPER_ORE));
				}
				if (second % 5 == 0) {
					System.out.println("[tailings-test] crushing: jaw " + p[1].status() + "; screen " + p[2].status() + "; cone " + p[3].status() + "; mill " + p[4].status()
							+ "; cyclone " + p[5].status() + "; fines " + p[5].out().held(Product.FINES_SLURRY) + " kg");
				}
			});
			context.waitTicks(20);
			if (t == 15) {
				context.takeScreenshot("tailings-crushing-running");
			}
		}
		context.takeScreenshot("tailings-crushing-end");
		film.stop();
		heard("crushing", "tailings:jaw_crusher", "tailings:vibrating_screen", "tailings:ball_mill", "tailings:belt_conveyor", "tailings:slurry_rush");
	}

	/** Fines slurry through the conditioner, the bank with its blower, thickener and filter to ingots, and the tails to the pond. */
	private void flotation(ClientGameTestContext context, TestSingleplayerContext world, TestServerContext server) {
		BlockPos o = new BlockPos(60, y + 1, 340);
		PlantBlockEntity[][] parts = new PlantBlockEntity[1][];
		server.runOnServer(srv -> {
			ServerLevel level = srv.overworld();
			double copper = Rock.COPPER.contained(Mineral.COPPER).doubleValue();
			PlantBlockEntity source = plant(level, Machine.CYCLONE, o);
			source.out().offer(Product.FINES_SLURRY, 100, SlurryAssay.of(java.util.Map.of("minecraft:copper_ingot", copper / 100)));
			pipe(level, o.offset(0, 0, 2), o.offset(1, 0, 2));
			PlantBlockEntity tank = plant(level, Machine.CONDITIONER, o.offset(0, 0, 3));
			pipe(level, o.offset(0, 0, 6));
			PlantBlockEntity bank = plant(level, Machine.FLOTATION_BANK, o.offset(0, 0, 7));
			PlantBlockEntity blower = plant(level, Machine.BLOWER, o.offset(7, 0, 7));
			pipe(level, o.offset(6, 0, 20));
			PlantBlockEntity thickener = plant(level, Machine.THICKENER, o.offset(6, 0, 21));
			pipe(level, o.offset(6, 0, 29));
			PlantBlockEntity filter = plant(level, Machine.FILTER, o.offset(6, 0, 30));
			pipe(level, o.offset(0, 0, 20), o.offset(0, 0, 21));
			PlantBlockEntity pond = plant(level, Machine.TAILINGS_POND, o.offset(0, 0, 22));
			for (PlantBlockEntity part : new PlantBlockEntity[] {tank, bank, blower, thickener, filter}) {
				part.forceRunning();
			}
			for (Reagent reagent : new Reagent[] {Reagent.LIME, Reagent.XANTHATE, Reagent.MIBC}) {
				tank.offer(new ItemStack(BuiltInRegistries.ITEM.getValue(Tailings.id(reagent.id()))));
			}
			parts[0] = new PlantBlockEntity[] {tank, bank, thickener, filter, pond};
		});
		server.runCommand("tp @p 85 " + (y + 15) + " 358 90 30");
		context.waitTicks(40);
		Filming film = film(context, world, "flotation");
		context.takeScreenshot("tailings-flotation-built");
		for (int t = 0; t < 30; t++) {
			int second = t;
			context.waitTicks(20);
			server.runOnServer(srv -> {
				if (second % 5 == 0) {
					PlantBlockEntity[] p = parts[0];
					System.out.println("[tailings-test] flotation: tank " + p[0].status() + "; bank " + p[1].status() + "; thickener " + p[2].status() + "; filter " + p[3].status()
							+ " paid " + p[3].countIn(new int[] {1, 2}, new ItemStack(Items.COPPER_INGOT)) + "; pond " + p[4].status());
				}
			});
			if (t == 15) {
				context.takeScreenshot("tailings-flotation-running");
			}
		}
		context.takeScreenshot("tailings-flotation-end");
		film.stop();
		heard("flotation", "tailings:agitator", "wildcatter:fan", "wildcatter:motor_hum", "tailings:pressure_filter");
	}

	/** Phase 4: a block's last quarter of copper through the scavengers, regrind, cleaners, leach, wash and cells (full-recovery.md). */
	private void recovery(ClientGameTestContext context, TestSingleplayerContext world, TestServerContext server) {
		BlockPos o = new BlockPos(120, y + 1, 400);
		PlantBlockEntity[][] parts = new PlantBlockEntity[1][];
		server.runOnServer(srv -> {
			ServerLevel level = srv.overworld();
			PlantBlockEntity source = plant(level, Machine.FLOTATION_BANK, o.offset(3, 0, 0));
			source.out().offer(Product.TAILS_SLURRY, 800, SlurryAssay.of(java.util.Map.of("minecraft:copper_ingot", 2.0 / 800)));
			pipe(level, o.offset(3, 0, 13));
			PlantBlockEntity scavenger = plant(level, Machine.SCAVENGER_BANK, o.offset(3, 0, 14));
			PlantBlockEntity blower = plant(level, Machine.BLOWER, o.offset(1, 0, 15));
			pipe(level, o.offset(7, 0, 23), o.offset(8, 0, 23), o.offset(9, 0, 23), o.offset(10, 0, 23), o.offset(11, 0, 23), o.offset(12, 0, 23));
			PlantBlockEntity regrind = plant(level, Machine.REGRIND_MILL, o.offset(12, 0, 24));
			pipe(level, o.offset(12, 0, 29));
			PlantBlockEntity cleaner = plant(level, Machine.CLEANER_BANK, o.offset(12, 0, 30));
			pipe(level, o.offset(12, 0, 36));
			PlantBlockEntity thickener = plant(level, Machine.THICKENER, o.offset(12, 0, 37));
			pipe(level, o.offset(12, 0, 45));
			PlantBlockEntity filter = plant(level, Machine.FILTER, o.offset(12, 0, 46));
			pipe(level, o.offset(3, 0, 23));
			PlantBlockEntity leach = plant(level, Machine.LEACH_TANK, o.offset(1, 0, 24));
			part(level, BuiltInRegistries.BLOCK.getValue(Identifier.fromNamespaceAndPath("wildcatter", "stock_tank")), o.offset(7, 0, 25));
			((dev.wildcatter.block.StorageTankBlockEntity) level.getBlockEntity(o.offset(7, 0, 25))).testFill(Product.SULFURIC_ACID, 5000);
			pipe(level, o.offset(6, 0, 26), o.offset(3, 0, 29));
			PlantBlockEntity ccd = plant(level, Machine.CCD_THICKENER, o.offset(0, 0, 30));
			pipe(level, o.offset(11, 0, 42));
			PlantBlockEntity cells = plant(level, Machine.ELECTROWINNING_BANK, o.offset(7, 0, 43));
			pipe(level, o.offset(0, 0, 42));
			PlantBlockEntity pond = plant(level, Machine.TAILINGS_POND, o.offset(0, 0, 43));
			for (PlantBlockEntity one : new PlantBlockEntity[] {scavenger, blower, regrind, cleaner, thickener, filter, leach, ccd, cells}) {
				one.forceRunning();
			}
			parts[0] = new PlantBlockEntity[] {scavenger, leach, ccd, filter, cells, pond};
		});
		server.runCommand("tp @p " + (o.getX() + 34) + " " + (o.getY() + 24) + " " + (o.getZ() + 24) + " 90 35");
		context.waitTicks(40);
		Filming film = film(context, world, "recovery");
		context.takeScreenshot("tailings-recovery-built");
		for (int t = 0; t < 30; t++) {
			context.waitTicks(20);
			if (t % 5 == 0) {
				server.runOnServer(srv -> System.out.println("[tailings-test] recovery: scavenger " + parts[0][0].status() + "; leach " + parts[0][1].status() + "; wash " + parts[0][2].status() + "; filter " + parts[0][3].status() + "; cells " + parts[0][4].status()));
			}
		}
		context.takeScreenshot("tailings-recovery-end");
		film.stop();
		var nugget = dev.wildcatter.tailings.block.Feeds.stack(Mineral.COPPER.nuggetId(), 1);
		int cells = server.computeOnServer(srv -> parts[0][4].countIn(new int[] {1, 2}, nugget) + 9 * parts[0][4].countIn(new int[] {1, 2}, new ItemStack(Items.COPPER_INGOT)));
		require(cells > 0, "the cells should plate copper");
		heard("recovery", "tailings:agitator");
	}

	/** selective-flotation.md: lead off the first bank, zinc off the second, each through its own filter. Force-run, as the other part films. */
	private void selective(ClientGameTestContext context, TestSingleplayerContext world, TestServerContext server) {
		BlockPos o = new BlockPos(180, y + 1, 400);
		PlantBlockEntity[][] parts = new PlantBlockEntity[1][];
		Mineral lead = Mineral.GALENA, zinc = Mineral.SPHALERITE;
		server.runOnServer(srv -> {
			ServerLevel level = srv.overworld();
			double kg = 1500;
			PlantBlockEntity tank = plant(level, Machine.CONDITIONER, o.offset(0, 0, 3));
			tank.back().offer(Product.FINES_SLURRY, kg, SlurryAssay.of(java.util.Map.of(lead.mineralItemId(), 72.0 * 3 / kg, zinc.mineralItemId(), 250.0 * 3 / kg)));
			tank.toggleSelective();
			pipe(level, o.offset(0, 0, 6));
			PlantBlockEntity one = plant(level, Machine.FLOTATION_BANK, o.offset(0, 0, 7));
			PlantBlockEntity blowerOne = plant(level, Machine.BLOWER, o.offset(7, 0, 7));
			pipe(level, o.offset(6, 0, 20), o.offset(7, 0, 20), o.offset(8, 0, 20), o.offset(9, 0, 20));
			PlantBlockEntity thickenerOne = plant(level, Machine.THICKENER, o.offset(9, 0, 21));
			pipe(level, o.offset(9, 0, 29));
			PlantBlockEntity filterOne = plant(level, Machine.FILTER, o.offset(9, 0, 30));
			pipe(level, o.offset(0, 0, 20));
			PlantBlockEntity activation = plant(level, Machine.ACTIVATION_TANK, o.offset(0, 0, 21));
			pipe(level, o.offset(0, 0, 24));
			PlantBlockEntity two = plant(level, Machine.FLOTATION_BANK, o.offset(0, 0, 25));
			PlantBlockEntity blowerTwo = plant(level, Machine.BLOWER, o.offset(7, 0, 25));
			pipe(level, o.offset(6, 0, 38));
			PlantBlockEntity thickenerTwo = plant(level, Machine.THICKENER, o.offset(6, 0, 39));
			pipe(level, o.offset(6, 0, 47));
			PlantBlockEntity filterTwo = plant(level, Machine.FILTER, o.offset(6, 0, 48));
			pipe(level, o.offset(0, 0, 38));
			plant(level, Machine.TAILINGS_POND, o.offset(0, 0, 39));
			for (PlantBlockEntity part : new PlantBlockEntity[] {tank, one, blowerOne, thickenerOne, filterOne, activation, two, blowerTwo, thickenerTwo, filterTwo}) {
				part.forceRunning();
			}
			for (Reagent reagent : java.util.List.of(Reagent.LIME, Reagent.XANTHATE, Reagent.MIBC, Reagent.ZINC_SULFATE)) {
				tank.offer(new ItemStack(BuiltInRegistries.ITEM.getValue(Identifier.parse(reagent.itemId()))));
			}
			for (Reagent reagent : java.util.List.of(Reagent.LIME, Reagent.XANTHATE, Reagent.MIBC, Reagent.COPPER_SULFATE)) {
				activation.offer(new ItemStack(BuiltInRegistries.ITEM.getValue(Identifier.parse(reagent.itemId()))));
			}
			parts[0] = new PlantBlockEntity[] {tank, one, activation, two, filterOne, filterTwo};
		});
		server.runCommand("tp @p " + (o.getX() + 30) + " " + (o.getY() + 26) + " " + (o.getZ() + 26) + " 90 40");
		context.waitTicks(40);
		Filming film = film(context, world, "selective");
		context.takeScreenshot("tailings-selective-built");
		for (int t = 0; t < 40; t++) {
			context.waitTicks(20);
			if (t % 5 == 0) {
				server.runOnServer(srv -> System.out.println("[tailings-test] selective: tank " + parts[0][0].status() + "; lead " + parts[0][4].status() + "; activation " + parts[0][2].status() + "; zinc " + parts[0][5].status()));
			}
		}
		context.takeScreenshot("tailings-selective-end");
		film.stop();
		// nine nuggets pack into the ladder's item, so both count
		java.util.function.BiFunction<PlantBlockEntity, Mineral, Integer> paid = (filter, mineral) -> filter.countIn(new int[] {1, 2}, dev.wildcatter.tailings.block.Feeds.stack(mineral.nuggetId(), 1))
				+ 9 * filter.countIn(new int[] {1, 2}, dev.wildcatter.tailings.block.Feeds.stack(mineral.phaseOneProductId(), 1));
		int[] pay = server.computeOnServer(srv -> new int[] {paid.apply(parts[0][4], lead), paid.apply(parts[0][5], zinc), paid.apply(parts[0][4], zinc), paid.apply(parts[0][5], lead)});
		System.out.println("[tailings-test] selective paid: lead filter " + pay[0] + " lead, " + pay[2] + " zinc; zinc filter " + pay[1] + " zinc, " + pay[3] + " lead (nuggets)");
		require(pay[0] > 0, "the first filter should pay lead");
		require(pay[1] > 0, "the second filter should pay zinc");
		require(pay[2] == 0, "no zinc through the lead filter");
		require(pay[3] == 0, "no lead floats a second time");
		heard("selective", "tailings:agitator");
	}

	/** Tin slurry over the spiral bank: half its tin to the filter, the rest on (gravity-circuit.md). */
	private void spirals(ClientGameTestContext context, TestSingleplayerContext world, TestServerContext server) {
		BlockPos o = new BlockPos(60, y + 1, 400);
		PlantBlockEntity[][] parts = new PlantBlockEntity[1][];
		Mineral tin = Mineral.CASSITERITE;
		server.runOnServer(srv -> {
			ServerLevel level = srv.overworld();
			PlantBlockEntity source = plant(level, Machine.CYCLONE, o);
			source.out().offer(Product.FINES_SLURRY, 800, SlurryAssay.of(java.util.Map.of(tin.mineralItemId(), tin.itemAmount().doubleValue() * 2 / 400)));
			pipe(level, o.offset(0, 0, 2), o.offset(1, 0, 2));
			PlantBlockEntity bank = plant(level, Machine.SPIRAL_BANK, o.offset(0, 0, 3));
			pipe(level, o.offset(0, 0, 9));
			PlantBlockEntity pond = plant(level, Machine.TAILINGS_POND, o.offset(0, 0, 10));
			pipe(level, o.offset(3, 0, 9), o.offset(4, 0, 9), o.offset(5, 0, 9), o.offset(6, 0, 9), o.offset(7, 0, 9));
			PlantBlockEntity thickener = plant(level, Machine.THICKENER, o.offset(7, 0, 10));
			pipe(level, o.offset(7, 0, 18));
			PlantBlockEntity filter = plant(level, Machine.FILTER, o.offset(7, 0, 19));
			for (PlantBlockEntity part : new PlantBlockEntity[] {bank, thickener, filter}) {
				part.forceRunning();
			}
			parts[0] = new PlantBlockEntity[] {bank, thickener, filter, pond};
		});
		server.runCommand("tp @p " + (o.getX() + 14) + " " + (o.getY() + 9) + " " + (o.getZ() + 2) + " 120 28");
		context.waitTicks(40);
		Filming film = film(context, world, "spirals");
		context.takeScreenshot("tailings-spirals-built");
		for (int t = 0; t < 30; t++) {
			context.waitTicks(20);
			if (t % 5 == 0) {
				server.runOnServer(srv -> System.out.println("[tailings-test] spirals: " + parts[0][0].status() + "; thickener " + parts[0][1].status() + "; filter " + parts[0][2].inspect().getString()));
			}
		}
		context.takeScreenshot("tailings-spirals-end");
		film.stop();
		int paid = server.computeOnServer(srv -> parts[0][2].countIn(new int[] {1, 2}, dev.wildcatter.tailings.block.Feeds.stack(tin.phaseOneProductId(), 1)));
		require(paid == 2, "the spirals should pay half the tin: two items, got " + paid);
		heard("spirals", "tailings:slurry_rush", "tailings:pressure_filter");
	}

	/** The pocket, its feeder, the sorter and bins under one rail. Returns the parts: feeder, sorter, ore bin, waste bin. */
	private static PlantBlockEntity[] dumpStation(ServerLevel level, BlockPos rail) {
		part(level, TailingsBlocks.DUMP_POCKET, rail.offset(-1, -3, 0));
		part(level, TailingsBlocks.DUMP_RAMP, rail.offset(1, 0, 0));
		PlantBlockEntity feeder = plant(level, Machine.APRON_FEEDER, rail.offset(-1, -3, 3));
		PlantBlockEntity sorter = plant(level, Machine.ORE_SORTER, rail.offset(-2, -3, 7));
		PlantBlockEntity oreBin = plant(level, Machine.ORE_BIN, rail.offset(1, -3, 7));
		PlantBlockEntity wasteBin = plant(level, Machine.ORE_BIN, rail.offset(-2, -3, 10));
		feeder.forceRunning();
		sorter.forceRunning();
		sorter.setSide(Direction.EAST, new ItemStack(Blocks.COPPER_ORE));
		sorter.setWasteSide(Direction.SOUTH);
		return new PlantBlockEntity[] {feeder, sorter, oreBin, wasteBin};
	}

	/** A train tips a car of copper ore and a car of cobble on the move, and the sorter splits them to their bins. */
	private void dump(ClientGameTestContext context, TestSingleplayerContext world, TestServerContext server) {
		BlockPos rail = new BlockPos(120, y + 4, 346);
		PlantBlockEntity[][] parts = new PlantBlockEntity[1][];
		MineVehicleEntity[] cars = new MineVehicleEntity[3];
		server.runOnServer(srv -> {
			ServerLevel level = srv.overworld();
			BlockState track = TailingsBlocks.MINE_RAIL.defaultBlockState().setValue(MineRailBlock.FACING, Direction.NORTH);
			for (int z = -6; z <= 9; z++) {
				level.setBlock(rail.offset(0, 0, z), track, Block.UPDATE_ALL);
			}
			part(level, TailingsBlocks.TRACTION_RECTIFIER, rail.offset(-2, 0, -4));
			((RectifierBlockEntity) level.getBlockEntity(rail.offset(-2, 0, -4))).forceRunning();
			parts[0] = dumpStation(level, rail);
		});
		server.runCommand("tp @p " + (rail.getX() + 13) + " " + (rail.getY() + 4) + " " + (rail.getZ() + 3) + " 90 15");
		context.waitTicks(30);
		server.runOnServer(srv -> {
			ServerLevel level = srv.overworld();
			cars[0] = MineVehicleEntity.placeLocomotive(level, rail.offset(0, 0, 3), Heading.NORTH);
			cars[1] = MineVehicleEntity.placeCar(level, rail.offset(0, 0, 6), Heading.NORTH);
			cars[2] = MineVehicleEntity.placeCar(level, rail.offset(0, 0, 8), Heading.NORTH);
			cars[1].load().add("minecraft:copper_ore", 64);
			cars[2].load().add("minecraft:cobblestone", 64);
			cars[1].refreshLoad();
			cars[2].refreshLoad();
		});
		Filming film = film(context, world, "dump");
		context.takeScreenshot("tailings-dump-built");
		for (int t = 0; t < 30; t++) {
			int second = t;
			context.waitTicks(20);
			server.runOnServer(srv -> {
				if (second % 5 == 0) {
					PlantBlockEntity[] p = parts[0];
					System.out.println("[tailings-test] dump: train " + cars[0].status() + " cars " + cars[1].load().total() + "," + cars[2].load().total() + "; feeder " + p[0].status() + "; sorter " + p[1].status()
							+ "; ore bin " + p[2].binned() + "; waste bin " + p[3].binned());
				}
			});
			if (t == 10) {
				context.takeScreenshot("tailings-dump-tipping");
			}
		}
		context.takeScreenshot("tailings-dump-end");
		film.stop();
		heard("dump", "tailings:mine_locomotive", "tailings:muck_tip", "tailings:apron_feeder", "tailings:ore_sorter", "tailings:car_couple");
	}

	/**
	 * The open pit, P1 (open-pit.md): the player sits in the wheel loader and loads a haul truck from a face of broken rock with the
	 * jump key, then drives the truck back to the truck dump's bumper with the movement keys and tips it into the hopper, which the
	 * apron feeder draws to the sorter's bin. A second truck tips sand on the ground, where it settles. Passes are a quarter of their
	 * real 40 s, so the film stays short.
	 */
	private void pit(ClientGameTestContext context, TestSingleplayerContext world, TestServerContext server) {
		BlockPos b = new BlockPos(40, y, 600);
		dev.wildcatter.tailings.entity.PitVehicleEntity[] v = new dev.wildcatter.tailings.entity.PitVehicleEntity[3];
		dev.wildcatter.tailings.block.DumpBlockEntity[] hopper = new dev.wildcatter.tailings.block.DumpBlockEntity[1];
		PlantBlockEntity[] plant = new PlantBlockEntity[3];
		server.runOnServer(srv -> {
			ServerLevel level = srv.overworld();
			// a flat stone pad, cleared above
			for (int x = -12; x <= 44; x++) {
				for (int z = -12; z <= 60; z++) {
					for (int yy = -8; yy <= 14; yy++) {
						level.setBlock(b.offset(x, yy, z), (yy < 0 ? Blocks.STONE : Blocks.AIR).defaultBlockState(), Block.UPDATE_CLIENTS);
					}
				}
			}
			// the truck dump in an open cut: the hopper under the pad's edge, its wall and bumper, the feeder, sorter and bin
			for (int x = 2; x <= 17; x++) {
				for (int z = -9; z <= 9; z++) {
					for (int yy = -7; yy <= -1; yy++) {
						level.setBlock(b.offset(x, yy, z), Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
					}
				}
			}
			part(level, TailingsBlocks.PIT_HOPPER, b.offset(5, -6, 4));
			part(level, TailingsBlocks.TRUCK_DUMP_WALL, b.offset(4, -3, 10));
			part(level, TailingsBlocks.plantBlock(Machine.APRON_FEEDER), b.offset(8, -6, 0));
			part(level, TailingsBlocks.plantBlock(Machine.ORE_SORTER), b.offset(7, -6, -3));
			part(level, TailingsBlocks.plantBlock(Machine.ORE_BIN), b.offset(7, -6, -6));
			hopper[0] = (dev.wildcatter.tailings.block.DumpBlockEntity) level.getBlockEntity(b.offset(5, -6, 4));
			plant[0] = (PlantBlockEntity) level.getBlockEntity(b.offset(8, -6, 0));
			plant[1] = (PlantBlockEntity) level.getBlockEntity(b.offset(7, -6, -3));
			plant[2] = (PlantBlockEntity) level.getBlockEntity(b.offset(7, -6, -6));
			plant[1].setWasteSide(Direction.NORTH);
			plant[0].forceRunning();
			plant[1].forceRunning();
			// a face of broken copper-ore rock and cobble, as a blast leaves it
			for (int x = 0; x <= 6; x++) {
				for (int z = 30; z <= 34; z++) {
					for (int yy = 0; yy <= 4; yy++) {
						BlockPos at = b.offset(x, yy, z);
						level.setBlock(at, TailingsBlocks.MUCK.defaultBlockState(), Block.UPDATE_ALL);
						((dev.wildcatter.tailings.block.MuckBlockEntity) level.getBlockEntity(at)).set(java.util.Map.of((x + yy + z) % 3 == 0 ? "minecraft:copper_ore" : "minecraft:cobblestone", 3));
					}
				}
			}
			var loader = dev.wildcatter.tailings.TailingsEntities.WHEEL_LOADER.create(level, net.minecraft.world.entity.EntitySpawnReason.COMMAND);
			loader.setPos(b.getX() + 3.5, b.getY(), b.getZ() + 22.3);
			loader.setYRot(0);
			loader.fastPasses(4);
			level.addFreshEntity(loader);
			var truck = dev.wildcatter.tailings.TailingsEntities.HAUL_TRUCK.create(level, net.minecraft.world.entity.EntitySpawnReason.COMMAND);
			truck.setPos(b.getX() + 10.0, b.getY(), b.getZ() + 26.0);
			truck.setYRot(0);
			level.addFreshEntity(truck);
			var other = dev.wildcatter.tailings.TailingsEntities.HAUL_TRUCK.create(level, net.minecraft.world.entity.EntitySpawnReason.COMMAND);
			other.setPos(b.getX() + 30.0, b.getY(), b.getZ() + 30.0);
			other.setYRot(0);
			other.load().add("minecraft:sand", 40_000);
			other.refreshLoad();
			level.addFreshEntity(other);
			v[0] = loader;
			v[1] = truck;
			v[2] = other;
		});
		server.runCommand("tp @p " + (b.getX() + 26) + " " + (b.getY() + 14) + " " + (b.getZ() + 40) + " 135 30");
		context.waitTicks(40);
		context.takeScreenshot("tailings-pit-built");
		Filming film = film(context, world, "pit");
		// into the loader's cab, and five passes with the jump key
		server.runCommand("gamemode creative @p");
		server.runOnServer(srv -> srv.getPlayerList().getPlayers().get(0).startRiding(v[0]));
		context.waitTicks(20);
		for (int pass = 0; pass < 12 && v[1].load().room() > 0; pass++) {
			context.getInput().holdKey(options -> options.keyJump);
			context.waitTicks(3);
			context.getInput().releaseKey(options -> options.keyJump);
			context.waitTicks(5);
			int p = pass;
			server.runOnServer(srv -> System.out.println("[tailings-test] pit: press " + p + ": loader " + v[0].status() + "; truck " + v[1].load().tonnes()));
			for (int t = 0; t < 40 && server.computeOnServer(srv -> v[0].busy()); t++) {
				context.waitTicks(5);
			}
			if (pass == 1) {
				context.takeScreenshot("tailings-pit-loader-cab");
			}
		}
		double loaded = server.computeOnServer(srv -> (double) v[1].load().total());
		require(loaded >= 85_000, "the loader should fill the truck with the jump key, it holds " + loaded + " kg");
		// out, and into the truck: back it up to the bumper with the movement keys
		context.getInput().holdKey(options -> options.keyShift);
		context.waitTicks(5);
		context.getInput().releaseKey(options -> options.keyShift);
		server.runOnServer(srv -> srv.getPlayerList().getPlayers().get(0).startRiding(v[1]));
		context.waitTicks(20);
		context.getInput().holdKey(options -> options.keyDown);
		for (int t = 0; t < 30; t++) {
			context.waitTicks(10);
			if (server.computeOnServer(srv -> v[1].speed() == 0 && v[1].getZ() < b.getZ() + 18)) {
				break;
			}
		}
		context.getInput().releaseKey(options -> options.keyDown);
		server.runOnServer(srv -> System.out.println("[tailings-test] pit: truck backed to z " + (v[1].getZ() - b.getZ()) + "; hopper under it " + (((dev.wildcatter.tailings.entity.HaulTruckEntity) v[1]).hopperBehind(srv.overworld()) != null)));
		context.getInput().holdKey(options -> options.keyShift);
		context.waitTicks(5);
		context.getInput().releaseKey(options -> options.keyShift);
		server.runCommand("gamemode spectator @p");
		server.runCommand("tp @p " + (b.getX() + 24) + " " + (b.getY() + 8) + " " + (b.getZ() + 6) + " 100 25");
		server.runOnServer(srv -> {
			v[1].pressJump(srv.overworld());
			v[2].pressJump(srv.overworld());
		});
		context.waitTicks(150);
		context.takeScreenshot("tailings-pit-tipping");
		for (int t = 0; t < 30 && server.computeOnServer(srv -> v[1].busy()); t++) {
			context.waitTicks(10);
		}
		server.runOnServer(srv -> System.out.println("[tailings-test] pit: truck " + v[1].status() + "; hopper " + hopper[0].pocket() + " blocks; other truck " + v[2].status()));
		require(server.computeOnServer(srv -> hopper[0].pocket() > 0 || plant[2].binned().getOrDefault("minecraft:cobblestone", 0) > 0), "the truck should tip into the hopper");
		server.runCommand("tp @p " + (b.getX() + 30) + " " + (b.getY() + 6) + " " + (b.getZ() + 10) + " 0 25");
		context.waitTicks(60);
		context.takeScreenshot("tailings-pit-sand-tipped");
		server.runCommand("tp @p " + (b.getX() + 22) + " " + (b.getY() + 2) + " " + (b.getZ() - 6) + " 90 10");
		context.waitTicks(200);
		server.runOnServer(srv -> System.out.println("[tailings-test] pit: feeder " + plant[0].status() + "; sorter " + plant[1].status() + "; bin " + plant[2].binned() + "; hopper " + hopper[0].pocket()));
		context.takeScreenshot("tailings-pit-feeder");
		film.stop();
		// the contact sheet's close looks: the truck with its body up, the loader with its arms up
		server.runOnServer(srv -> {
			v[2].load().add("minecraft:gravel", 9_000);
			v[2].pressJump(srv.overworld());
			((dev.wildcatter.tailings.entity.WheelLoaderEntity) v[0]).fastPasses(1);
			v[0].pressJump(srv.overworld());   // a real-length pass from what is left of the face: its arms are up as it carries
		});
		context.waitTicks(200);
		server.runCommand("tp @p " + (b.getX() + 40) + " " + (b.getY() + 5) + " " + (b.getZ() + 22) + " 120 15");
		context.waitTicks(20);
		context.takeScreenshot("tailings-pit-truck-close");
		server.runCommand("tp @p " + (b.getX() + 14) + " " + (b.getY() + 5) + " " + (b.getZ() + 14) + " 135 15");
		context.waitTicks(20);
		context.takeScreenshot("tailings-pit-loader-close");
		heard("pit", "wildcatter:diesel_idle", "tailings:muck_load", "tailings:muck_tip", "tailings:apron_feeder");
	}

	/** The whole mine: the machine drives down a decline in section while a real train takes its muck to the dump and comes back. */
	private void haul(ClientGameTestContext context, TestSingleplayerContext world, TestServerContext server) {
		int drop = 8;
		BlockPos c = new BlockPos(180, y + drop, 340);
		BlockPos beam = c.offset(3, 3, 3);
		BlockPos rail = c.offset(2, 1, 44);
		PlantBlockEntity[][] parts = new PlantBlockEntity[1][];
		MineVehicleEntity[] train = new MineVehicleEntity[3];
		server.runOnServer(srv -> {
			ServerLevel level = srv.overworld();
			for (int z = 1; z <= 36; z++) {
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
			level.setBlock(c.offset(-2, 0, 14), TailingsBlocks.CABLE_JUNCTION.defaultBlockState(), Block.UPDATE_ALL);
			BlockState track = TailingsBlocks.MINE_RAIL.defaultBlockState().setValue(MineRailBlock.FACING, Direction.NORTH);
			for (int z = 11; z <= 40; z++) {
				level.setBlock(c.offset(5, 1, z), track, Block.UPDATE_ALL);
			}
			for (int z = 11; z <= 50; z++) {
				level.setBlock(c.offset(2, 1, z), track, Block.UPDATE_ALL);
			}
			part(level, TailingsBlocks.TRACTION_RECTIFIER, c.offset(0, 1, 36));
			((RectifierBlockEntity) level.getBlockEntity(c.offset(0, 1, 36))).forceRunning();
			parts[0] = dumpStation(level, rail);
			DrillBlockEntity cab = (DrillBlockEntity) level.getBlockEntity(beam);
			cab.forceRunning();
			cab.program().setTargetY(c.getY() + 1 - drop);
			cab.program().setFlatLength(32);
			cab.setItem(DrillBlockEntity.SLOT_RAIL, new ItemStack(TailingsBlocks.MINE_RAIL, 64));
			cab.setItem(DrillBlockEntity.SLOT_GRAVEL, new ItemStack(Items.GRAVEL, 64));
			cab.setItem(DrillBlockEntity.SLOT_COBBLE, new ItemStack(Items.COBBLESTONE, 64));
			cab.setItem(DrillBlockEntity.SLOT_SEGMENTS, new ItemStack(TailingsBlocks.LINING, 64));
			cab.core().setTestTicks(40);
			for (TbmGantry kind : TbmGantry.values()) {
				GantryEntity gantry = GantryEntity.place(level, c.offset(2, 1, kind.row()), kind, Direction.NORTH);
				System.out.println("[tailings-test] " + kind.displayName() + ": " + gantry.couple());
			}
			cutAway(level, c, drop);
		});
		server.runCommand("tp @p " + (c.getX() + 38) + " " + (c.getY() + 1) + " " + (c.getZ() + 22) + " 90 0");
		context.waitTicks(40);
		server.runOnServer(srv -> {
			ServerLevel level = srv.overworld();
			train[0] = MineVehicleEntity.placeLocomotive(level, c.offset(2, 1, 42), Heading.NORTH);
			train[1] = MineVehicleEntity.placeCar(level, c.offset(2, 1, 45), Heading.NORTH);
			train[2] = MineVehicleEntity.placeCar(level, c.offset(2, 1, 47), Heading.NORTH);
		});
		Filming film = film(context, world, "haul");
		context.takeScreenshot("tailings-haul-built");
		server.runOnServer(srv -> ((DrillBlockEntity) srv.overworld().getBlockEntity(beam)).press(10, null));
		for (int t = 0; t < 110; t++) {
			int second = t;
			context.waitTicks(20);
			server.runOnServer(srv -> {
				cutAway(srv.overworld(), c, drop);
				for (TbmEntity machine : srv.overworld().getEntitiesOfClass(TbmEntity.class, new AABB(c).inflate(60))) {
					machine.setItem(DrillBlockEntity.SLOT_SEGMENTS, new ItemStack(TailingsBlocks.LINING, 64));
					machine.setItem(DrillBlockEntity.SLOT_RAIL, new ItemStack(TailingsBlocks.MINE_RAIL, 64));
					machine.setItem(DrillBlockEntity.SLOT_GRAVEL, new ItemStack(Items.GRAVEL, 64));
					if (second % 5 == 0) {
						PlantBlockEntity[] p = parts[0];
						System.out.println("[tailings-test] haul " + second + "s: " + machine.core().readout() + " | train z " + (train[0].getZ() - c.getZ()) + " " + train[0].status()
								+ " cars " + train[1].load().total() + "," + train[2].load().total() + " | waste bin " + p[3].binned() + "; sorter " + p[1].status());
					}
				}
			});
			if (t % 25 == 24) {
				context.takeScreenshot("tailings-haul-" + (t + 1));
			}
		}
		film.stop();
		heard("haul", "tailings:tbm_cutter", "tailings:mine_locomotive");
	}

	/** Stands the creative player, flying, at the point and aims the crosshair at the block. */
	private void aim(ClientGameTestContext context, TestServerContext server, double x, double yy, double z, BlockPos at) {
		server.runCommand("tp @p " + x + " " + yy + " " + z);
		context.waitTicks(3);
		context.getInput().lookAt(at);
		context.waitTicks(3);
	}

	private static void require(boolean ok, String what) {
		if (!ok) {
			throw new AssertionError("[tailings-test] " + what);
		}
	}

	private static List<TbmEntity> machines(ServerLevel level, BlockPos c) {
		return level.getEntitiesOfClass(TbmEntity.class, new AABB(c).inflate(80));
	}

	/**
	 * What only a player can do, and what only a real save can show: the gantries placed and coupled with real clicks, the cab opened
	 * and started from its screen, a gantry broken by hand; then save and quit in mid-drive, and the machine goes on after reopening.
	 */
	private void hand(ClientGameTestContext context) {
		BlockPos c;
		BlockPos beam;
		TestWorldSave save;
		try (TestSingleplayerContext world = context.worldBuilder().create()) {
			TestServerContext server = world.getServer();
			context.waitTicks(40);
			y = server.computeOnServer(srv -> srv.overworld().getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, 0, 0));
			c = new BlockPos(0, y, 40);
			beam = c.offset(3, 3, 3);
			save = world.getWorldSave();
			BlockPos cc = c;
			BlockPos bb = beam;
			server.runCommand("time set noon");
			server.runCommand("gamemode creative @p");
			server.runOnServer(srv -> {
				ServerLevel level = srv.overworld();
				for (int z = 1; z <= 24; z++) {
					for (int x = -2; x < 10; x++) {
						for (int yy = -2; yy < 10; yy++) {
							level.setBlock(cc.offset(x, yy, -z), Blocks.STONE.defaultBlockState(), Block.UPDATE_CLIENTS);
						}
					}
				}
				part(level, TailingsBlocks.CUTTERHEAD, cc);
				part(level, TailingsBlocks.SEGMENT_ERECTOR, cc.offset(0, 0, 1));
				part(level, TailingsBlocks.MAIN_BEAM, bb);
				part(level, TailingsBlocks.TRACK_LAYER, cc.offset(1, 2, 8));
				level.setBlock(cc.offset(-2, 0, 14), TailingsBlocks.CABLE_JUNCTION.defaultBlockState(), Block.UPDATE_ALL);
				BlockState track = TailingsBlocks.MINE_RAIL.defaultBlockState().setValue(MineRailBlock.FACING, Direction.NORTH);
				for (int z = 11; z <= 40; z++) {
					level.setBlock(cc.offset(2, 1, z), track, Block.UPDATE_ALL);
					level.setBlock(cc.offset(5, 1, z), track, Block.UPDATE_ALL);
				}
				DrillBlockEntity cab = (DrillBlockEntity) level.getBlockEntity(bb);
				cab.forceRunning();
				cab.program().setTargetY(cc.getY() + 1);
				cab.program().setFlatLength(20);
				cab.setItem(DrillBlockEntity.SLOT_RAIL, new ItemStack(TailingsBlocks.MINE_RAIL, 64));
				cab.setItem(DrillBlockEntity.SLOT_GRAVEL, new ItemStack(Items.GRAVEL, 64));
				cab.setItem(DrillBlockEntity.SLOT_COBBLE, new ItemStack(Items.COBBLESTONE, 64));
				cab.setItem(DrillBlockEntity.SLOT_SEGMENTS, new ItemStack(TailingsBlocks.LINING, 64));
				cab.core().setTestTicks(40);
				for (ServerPlayer player : srv.getPlayerList().getPlayers()) {
					player.getAbilities().flying = true;
					player.onUpdateAbilities();
				}
			});
			context.waitTicks(20);
			Filming film = film(context, world, "hand");
			// place each gantry on the left rail, looking north along it
			for (TbmGantry kind : TbmGantry.values()) {
				server.runCommand("item replace entity @p weapon.mainhand with tailings:" + kind.id());
				aim(context, server, cc.getX() + 2.5, cc.getY() + 4, cc.getZ() + kind.row() + 3.5, cc.offset(2, 1, kind.row()));
				context.getInput().pressKey(options -> options.keyUse);
				context.waitTicks(10);
			}
			server.runCommand("item replace entity @p weapon.mainhand with air");
			context.takeScreenshot("tailings-hand-placed");
			server.runOnServer(srv -> {
				List<GantryEntity> placed = srv.overworld().getEntitiesOfClass(GantryEntity.class, new AABB(cc).inflate(60));
				for (GantryEntity gantry : placed) {
					System.out.println("[tailings-test] hand: " + gantry.kind() + " at " + gantry.position().subtract(cc.getX(), cc.getY(), cc.getZ()));
				}
				require(placed.size() == 3, "three gantries should be on the rail, got " + placed.size());
			});
			// its screen with the gantries not coupled yet: Go reads NOT BUILT and names the first
			aim(context, server, cc.getX() + 4.5, cc.getY() + 7, cc.getZ() + 11.5, cc.offset(4, 4, 9));
			context.getInput().pressKey(options -> options.keyUse);
			context.waitTicks(20);
			context.runOnClient(client -> client.gameMode.handleInventoryButtonClick(client.player.containerMenu.containerId, 10));
			context.waitTicks(20);
			context.takeScreenshot("tailings-hand-not-built");
			// and off again, or it starts by itself as soon as it is whole
			context.runOnClient(client -> client.gameMode.handleInventoryButtonClick(client.player.containerMenu.containerId, 10));
			context.waitTicks(5);
			context.runOnClient(client -> client.player.closeContainer());
			context.waitTicks(5);
			server.runOnServer(srv -> System.out.println("[tailings-test] hand: uncoupled, " + ((DrillBlockEntity) srv.overworld().getBlockEntity(bb)).core().readout()));
			// without the sneak a click only reports; with it, it couples
			aim(context, server, cc.getX() + 8.5, cc.getY() + 3, cc.getZ() + 13.5, cc.offset(6, 3, 13));
			context.getInput().pressKey(options -> options.keyUse);
			context.waitTicks(5);
			server.runOnServer(srv -> require(((DrillBlockEntity) srv.overworld().getBlockEntity(bb)).core().gantry(TbmGantry.PUMP) == null, "a plain click should not couple"));
			context.getInput().holdKey(options -> options.keyShift);
			for (TbmGantry kind : TbmGantry.values()) {
				int row = kind.row() + 1;
				aim(context, server, cc.getX() + 8.5, cc.getY() + 3, cc.getZ() + row + 0.5, cc.offset(6, 3, row));
				context.getInput().pressKey(options -> options.keyUse);
				context.waitTicks(10);
			}
			context.getInput().releaseKey(options -> options.keyShift);
			server.runOnServer(srv -> {
				DrillBlockEntity cab = (DrillBlockEntity) srv.overworld().getBlockEntity(bb);
				for (TbmGantry kind : TbmGantry.values()) {
					require(cab.core().gantry(kind) != null, kind.displayName() + " should couple from a sneak-click");
				}
			});
			context.takeScreenshot("tailings-hand-coupled");
			// a spare gantry placed behind, broken with a sneaking hit: it comes back as its item
			server.runCommand("item replace entity @p weapon.mainhand with tailings:" + TbmGantry.PUMP.id());
			aim(context, server, cc.getX() + 2.5, cc.getY() + 4, cc.getZ() + 39.5, cc.offset(2, 1, 36));
			context.getInput().pressKey(options -> options.keyUse);
			context.waitTicks(10);
			server.runCommand("item replace entity @p weapon.mainhand with air");
			context.waitTicks(3);
			aim(context, server, cc.getX() + 8.5, cc.getY() + 3, cc.getZ() + 37.5, cc.offset(6, 3, 37));
			context.getInput().holdKey(options -> options.keyShift);
			context.waitTicks(2);
			context.getInput().pressKey(options -> options.keyAttack);
			context.waitTicks(10);
			context.getInput().releaseKey(options -> options.keyShift);
			server.runOnServer(srv -> require(srv.overworld().getEntitiesOfClass(GantryEntity.class, new AABB(cc).inflate(60)).size() == 3, "a sneaking hit should break the spare gantry"));
			server.runCommand("kill @e[type=item]");
			// the cab's screen, opened by a click on the beam's tail and started from it
			aim(context, server, cc.getX() + 4.5, cc.getY() + 7, cc.getZ() + 11.5, cc.offset(4, 4, 9));
			context.getInput().pressKey(options -> options.keyUse);
			context.waitTicks(20);
			require(context.computeOnClient(client -> client.player.containerMenu != client.player.inventoryMenu), "a click on the cab should open its screen");
			context.takeScreenshot("tailings-hand-cab");
			boolean waiting = server.computeOnServer(srv -> srv.overworld().getBlockEntity(bb) instanceof DrillBlockEntity cab && !cab.core().running());
			if (waiting) {
				context.runOnClient(client -> client.gameMode.handleInventoryButtonClick(client.player.containerMenu.containerId, 10));
			}
			context.waitTicks(20);
			context.runOnClient(client -> client.player.closeContainer());
			server.runCommand("tp @p " + (cc.getX() + 24) + " " + (cc.getY() + 8) + " " + (cc.getZ() + 14) + " 90 15");
			context.waitTicks(200);
			int before = server.computeOnServer(srv -> {
				List<TbmEntity> found = machines(srv.overworld(), cc);
				require(found.size() == 1, "the machine should be running from its screen");
				System.out.println("[tailings-test] hand: " + found.get(0).core().readout());
				return found.get(0).core().drill().advanceCount();
			});
			require(before >= 3, "it should have driven, got " + before);
			context.takeScreenshot("tailings-hand-driving");
			film.stop();
		}
		// save and quit in mid-drive; reopen
		try (TestSingleplayerContext world = save.open()) {
			TestServerContext server = world.getServer();
			BlockPos cc = c;
			context.waitTicks(40);
			Filming film = film(context, world, "reload");
			context.takeScreenshot("tailings-reload-open");
			int at = server.computeOnServer(srv -> {
				List<TbmEntity> found = machines(srv.overworld(), cc);
				require(found.size() == 1, "the machine should come back from the save, found " + found.size());
				TbmEntity machine = found.get(0);
				System.out.println("[tailings-test] reload: " + machine.position() + " " + machine.core().readout());
				for (TbmGantry kind : TbmGantry.values()) {
					require(srv.overworld().getEntity(machine.core().gantry(kind)) instanceof GantryEntity, kind.displayName() + " should still be coupled after the reload");
				}
				return machine.core().drill().advanceCount();
			});
			context.waitTicks(200);
			int later = server.computeOnServer(srv -> {
				TbmEntity machine = machines(srv.overworld(), cc).get(0);
				System.out.println("[tailings-test] reload later: " + machine.position() + " " + machine.core().readout());
				for (GantryEntity gantry : srv.overworld().getEntitiesOfClass(GantryEntity.class, new AABB(cc).inflate(80))) {
					System.out.println("[tailings-test] reload gantry " + gantry.kind() + " " + gantry.position());
				}
				return machine.core().drill().advanceCount();
			});
			require(later > at, "the machine should go on driving after the reload: " + at + " then " + later);
			context.takeScreenshot("tailings-reload-driving");
			film.stop();
		}
	}

	/** Clears the bore's camera-side wall (column 7) and the rock outside it, the length of the mountain and back to the start. */
	private static void cutAway(ServerLevel level, BlockPos c, int drop) {
		for (int z = -36; z <= 12; z++) {
			for (int x = 7; x < 10; x++) {
				for (int yy = -drop - 2; yy < 10; yy++) {
					BlockPos at = c.offset(x, yy, z);
					BlockState there = level.getBlockState(at);
					if (there.is(Blocks.STONE) || there.is(TailingsBlocks.LINING)) {
						level.setBlock(at, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
					}
				}
			}
		}
	}
}
