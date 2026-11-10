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
			if (only("pit_haul")) {
				pitHaul(context, world, server);
			}
			if (only("pit_blast")) {
				pitBlast(context, world, server);
			}
			if (only("pit_dispatch")) {
				pitDispatch(context, world, server);
			}
			if (only("pit_dry_stack")) {
				pitDryStack(context, world, server);
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
			// every part fed only through its feed face (crushing-circuit.md, "Nozzles and faces"); the NeoForge scene crushing_circuit has the same layout
			PlantBlockEntity jaw = plant(level, Machine.JAW, o);
			PlantBlockEntity grizzly = plant(level, Machine.GRIZZLY, o.offset(0, 4, 0));
			PlantBlockEntity screen = plant(level, Machine.SCREEN, o.offset(5, 0, 4));
			PlantBlockEntity cone = plant(level, Machine.CONE, o.offset(5, 0, 14));
			PlantBlockEntity mill = plant(level, Machine.BALL_MILL, o.offset(9, -2, 4));
			PlantBlockEntity cyclone = plant(level, Machine.CYCLONE, o.offset(13, -2, 6));
			PlantBlockEntity bypassDrive = plant(level, Machine.CONVEYOR_DRIVE, o.offset(3, 3, 3));
			PlantBlockEntity jawDrive = plant(level, Machine.CONVEYOR_DRIVE, o.offset(7, 3, 3));
			PlantBlockEntity oversizeDrive = plant(level, Machine.CONVEYOR_DRIVE, o.offset(6, 4, 15));
			PlantBlockEntity sandDrive = plant(level, Machine.CONVEYOR_DRIVE, o.offset(11, 2, 4));
			// the jaw's discharge up over the screen's north end; the sand off its east face over the mill's north end
			for (BlockPos at : new BlockPos[] {o.offset(0, 0, 4), o.offset(1, 0, 4), o.offset(2, 0, 4), o.offset(3, 1, 4), o.offset(4, 2, 4), o.offset(5, 3, 4), o.offset(6, 3, 4),
					o.offset(8, 2, 5), o.offset(9, 2, 5), o.offset(10, 2, 5)}) {
				level.setBlock(at, belt(Direction.Axis.X), Block.UPDATE_ALL);
			}
			// the grizzly's undersize down onto the jaw's belt; the screen's oversize up over the cone
			for (BlockPos at : new BlockPos[] {o.offset(3, 4, 1), o.offset(3, 3, 2), o.offset(3, 2, 3),
					o.offset(6, 0, 10), o.offset(6, 1, 11), o.offset(6, 2, 12), o.offset(6, 3, 13), o.offset(6, 4, 14)}) {
				level.setBlock(at, belt(Direction.Axis.Z), Block.UPDATE_ALL);
			}
			pipe(level, o.offset(12, -2, 10), o.offset(13, -2, 10), o.offset(14, -2, 10), o.offset(15, -2, 10), o.offset(15, -2, 9), o.offset(15, -2, 8),
					o.offset(15, -2, 7), o.offset(15, -2, 6), o.offset(15, -2, 5), o.offset(14, -2, 5));
			for (PlantBlockEntity part : new PlantBlockEntity[] {jaw, screen, cone, bypassDrive, jawDrive, oversizeDrive, sandDrive, mill, cyclone}) {
				part.forceRunning();
			}
			parts[0] = new PlantBlockEntity[] {grizzly, jaw, screen, cone, mill, cyclone};
		});
		server.runCommand("tp @p 22 " + (y + 12) + " 362 135 25");
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
			// the assay counts metal in kg: the block's four copper ingots are 400 kg
			double copper = Rock.COPPER.contained(Mineral.COPPER).doubleValue() * Mineral.INGOT_KG;
			PlantBlockEntity source = plant(level, Machine.CYCLONE, o);
			source.out().offer(Product.FINES_SLURRY, 1200, SlurryAssay.of(java.util.Map.of("minecraft:copper_ingot", copper / 1200)));
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
			source.out().offer(Product.TAILS_SLURRY, 800, SlurryAssay.of(java.util.Map.of("minecraft:copper_ingot", 2 * Mineral.INGOT_KG / 800)));
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
		BlockPos b = new BlockPos(40, y + 8, 600);   // a raised pad: the hopper's cut goes 7 below it, past a flat world's floor
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
			// vehicles left from an earlier run of this scene in the same test world
			var stale = level.getEntitiesOfClass(dev.wildcatter.tailings.entity.PitVehicleEntity.class, new net.minecraft.world.phys.AABB(b).inflate(80));
			System.out.println("[tailings-test] pit: " + stale.size() + " vehicles already here");
			stale.forEach(e -> e.discard());
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
		// their chunks may have unloaded and loaded again before the camera came: hold the entities the world has now
		java.util.UUID[] ids = server.computeOnServer(srv -> new java.util.UUID[] {v[0].getUUID(), v[1].getUUID(), v[2].getUUID()});
		Runnable fresh = () -> server.runOnServer(srv -> {
			for (int i = 0; i < 3; i++) {
				if (srv.overworld().getEntity(ids[i]) instanceof dev.wildcatter.tailings.entity.PitVehicleEntity e) {
					v[i] = e;
				}
			}
		});
		fresh.run();
		context.takeScreenshot("tailings-pit-built");
		Filming film = film(context, world, "pit");
		// into the loader's cab, and five passes with the jump key
		server.runCommand("gamemode creative @p");
		server.runOnServer(srv -> srv.getPlayerList().getPlayers().get(0).startRiding(v[0]));
		context.waitTicks(20);
		for (int pass = 0; pass < 16 && v[1].load().room() > 0; pass++) {
			context.getInput().holdKey(options -> options.keyJump);
			context.waitTicks(10);   // held long enough for the server to see it down
			context.getInput().releaseKey(options -> options.keyJump);
			context.waitTicks(5);
			int p = pass;
			server.runOnServer(srv -> System.out.println("[tailings-test] pit: press " + p + ": loader " + v[0].status() + "; truck " + v[1].load().tonnes() + " at " + v[1].position() + "; other " + v[2].load().tonnes() + " at " + v[2].position() + "; loader at " + v[0].position()));
			for (int t = 0; t < 20 && !server.computeOnServer(srv -> v[0].busy()); t++) {
				context.waitTicks(2);   // the press may still be on its way to the server
			}
			for (int t = 0; t < 60 && server.computeOnServer(srv -> v[0].busy()); t++) {
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
		context.waitTicks(15);   // the sneak reaches the server first, or it would take them out of the truck too
		server.runOnServer(srv -> srv.getPlayerList().getPlayers().get(0).startRiding(v[1]));
		context.waitTicks(20);
		context.getInput().holdKey(options -> options.keyDown);
		for (int t = 0; t < 30; t++) {
			context.waitTicks(10);
			server.runOnServer(srv -> {
				var pl = srv.getPlayerList().getPlayers().get(0);
				System.out.println("[tailings-test] pit: backing: riding " + (pl.getVehicle() != null) + "; z " + (v[1].getZ() - b.getZ()) + "; speed " + v[1].speed() + "; " + v[1].status());
			});
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
		fresh.run();
		server.runOnServer(srv -> {
			v[1].pressJump(srv.overworld());
			v[2].pressJump(srv.overworld());
		});
		context.waitTicks(150);
		context.takeScreenshot("tailings-pit-tipping");
		for (int t = 0; t < 30 && server.computeOnServer(srv -> v[1].busy()); t++) {
			context.waitTicks(10);
		}
		server.runOnServer(srv -> System.out.println("[tailings-test] pit: truck " + v[1].status() + ", left in it " + v[1].load().contents() + "; hopper " + hopper[0].pocket() + " blocks; other truck " + v[2].status()));
		require(server.computeOnServer(srv -> hopper[0].pocket() > 0 || plant[2].binned().getOrDefault("minecraft:cobblestone", 0) > 0), "the truck should tip into the hopper");
		server.runCommand("tp @p " + (b.getX() + 30) + " " + (b.getY() + 6) + " " + (b.getZ() + 10) + " 0 25");
		fresh.run();
		context.waitTicks(60);
		context.takeScreenshot("tailings-pit-sand-tipped");
		server.runCommand("tp @p " + (b.getX() + 22) + " " + (b.getY() + 2) + " " + (b.getZ() - 6) + " 90 10");
		fresh.run();
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
		server.runCommand("tp @p " + (b.getX() + 40) + " " + (b.getY() + 6) + " " + (b.getZ() + 20) + " 50 15");
		fresh.run();
		context.waitTicks(20);
		context.takeScreenshot("tailings-pit-truck-close");
		server.runCommand("tp @p " + (b.getX() + 13) + " " + (b.getY() + 6) + " " + (b.getZ() + 14) + " 50 20");
		fresh.run();
		context.waitTicks(20);
		context.takeScreenshot("tailings-pit-loader-close");
		heard("pit", "wildcatter:diesel_idle", "tailings:muck_load", "tailings:muck_tip", "tailings:apron_feeder");
	}


	/**
	 * The open pit's P2a haul, filmed (docs/multiblocks/open-pit.md): a truck on a 7-stake survey route turns round and backs to a
	 * self-running loader, is filled, climbs a 10 % ramp, turns round and backs to the truck dump's bumper, tips into the hopper and
	 * comes back, with nobody driving. The camera follows the truck. The same layout as the server scene pit_truck_haul.
	 */
	private void pitHaul(ClientGameTestContext context, TestSingleplayerContext world, TestServerContext server) {
		BlockPos b = new BlockPos(40, y + 8, 1000).offset(0, -59, 0);   // b + (x, 59, z) is the dump pad's road level
		dev.wildcatter.tailings.entity.PitVehicleEntity[] v = new dev.wildcatter.tailings.entity.PitVehicleEntity[2];
		dev.wildcatter.tailings.block.DumpBlockEntity[] hopper = new dev.wildcatter.tailings.block.DumpBlockEntity[1];
		server.runOnServer(srv -> {
			ServerLevel level = srv.overworld();
			for (int x = -14; x <= 128; x++) {
				for (int z = -8; z <= 80; z++) {
					int top = x <= 60 ? (z >= 12 ? 58 : 51) : x <= 70 ? 57 : x <= 80 ? 56 : 55;
					if (x <= 90 && x > 60 && z < 12) {
						top = 51;
					}
					for (int yy = 50; yy <= 75; yy++) {
						level.setBlock(b.offset(x, yy, z), (yy <= top ? Blocks.STONE : Blocks.AIR).defaultBlockState(), Block.UPDATE_CLIENTS);
					}
				}
			}
			part(level, TailingsBlocks.PIT_HOPPER, b.offset(2, 53, 4));
			part(level, TailingsBlocks.TRUCK_DUMP_WALL, b.offset(1, 56, 10));
			hopper[0] = (dev.wildcatter.tailings.block.DumpBlockEntity) level.getBlockEntity(b.offset(2, 53, 4));
			for (int x = 98; x <= 102; x++) {
				for (int z = 18; z <= 21; z++) {
					for (int yy = 56; yy <= 58; yy++) {
						BlockPos at = b.offset(x, yy, z);
						level.setBlock(at, TailingsBlocks.MUCK.defaultBlockState(), Block.UPDATE_ALL);
						((dev.wildcatter.tailings.block.MuckBlockEntity) level.getBlockEntity(at)).set(java.util.Map.of((x + yy + z) % 3 == 0 ? "minecraft:copper_ore" : "minecraft:cobblestone", 3));
					}
				}
			}
			int[][] stakes = {{93, 56, 15}, {93, 56, 58}, {88, 56, 52}, {55, 59, 52}, {40, 59, 42}, {14, 59, 42}, {7, 59, 13}};
			dev.wildcatter.tailings.pit.StopKind[] kinds = {dev.wildcatter.tailings.pit.StopKind.LOAD, null, null, null, null, null, dev.wildcatter.tailings.pit.StopKind.DUMP_POCKET};
			ItemStack book = new ItemStack(dev.wildcatter.tailings.TailingsItems.SURVEY_BOOK);
			java.util.List<BlockPos> route = new java.util.ArrayList<>();
			for (int i = 0; i < stakes.length; i++) {
				BlockPos at = b.offset(stakes[i][0], stakes[i][1], stakes[i][2]);
				level.setBlock(at, TailingsBlocks.SURVEY_STAKE.defaultBlockState().setValue(dev.wildcatter.tailings.block.SurveyStakeBlock.KIND,
						kinds[i] == null ? dev.wildcatter.tailings.pit.StopKind.WAYPOINT : kinds[i]), Block.UPDATE_ALL);
				System.out.println("[tailings-test] pit_haul: " + dev.wildcatter.tailings.item.SurveyBookItem.addStake(level, book, at));
				route.add(at);
			}
			var loader = dev.wildcatter.tailings.TailingsEntities.WHEEL_LOADER.create(level, net.minecraft.world.entity.EntitySpawnReason.COMMAND);
			loader.setPos(b.getX() + 100.5, b.getY() + 56, b.getZ() + 28);
			loader.setYRot(180);
			loader.fastPasses(8);
			loader.setFuel(loader.tank());
			level.addFreshEntity(loader);
			System.out.println("[tailings-test] pit_haul: loader " + loader.setAuto(level, route));
			var truck = dev.wildcatter.tailings.TailingsEntities.HAUL_TRUCK.create(level, net.minecraft.world.entity.EntitySpawnReason.COMMAND);
			truck.setPos(b.getX() + 93.5, b.getY() + 56, b.getZ() + 62);
			truck.setYRot(0);
			truck.setFuel(truck.tank());
			level.addFreshEntity(truck);
			System.out.println("[tailings-test] pit_haul: truck " + truck.takeRoute(level, route));
			v[0] = loader;
			v[1] = truck;
		});
		server.runCommand("tp @p " + (b.getX() + 120) + " " + (b.getY() + 95) + " " + (b.getZ() + 95) + " 130 35");
		context.waitTicks(60);
		java.util.UUID[] ids = server.computeOnServer(srv -> new java.util.UUID[] {v[0].getUUID(), v[1].getUUID()});
		Runnable fresh = () -> server.runOnServer(srv -> {
			for (int i = 0; i < 2; i++) {
				if (srv.overworld().getEntity(ids[i]) instanceof dev.wildcatter.tailings.entity.PitVehicleEntity e) {
					v[i] = e;
				}
			}
		});
		fresh.run();
		context.takeScreenshot("tailings-pit-haul-route");
		Filming film = film(context, world, "pit_haul");
		java.util.Set<String> shot = new java.util.HashSet<>();
		boolean tipped = false;
		for (int t = 0; t < 900; t++) {
			fresh.run();
			String status = server.computeOnServer(srv -> v[1].status());
			double[] at = server.computeOnServer(srv -> new double[] {v[1].getX(), v[1].getZ(), v[1].getYRot(), v[1].lift(), v[1].load().total()});
			// the camera: behind and above the truck's right side, looking at it
			double yaw = Math.toRadians(at[2]);
			double cx = at[0] + Math.cos(yaw) * 22 + Math.sin(yaw) * 14, cz = at[1] + Math.sin(yaw) * 22 - Math.cos(yaw) * 14;
			double look = Math.toDegrees(Math.atan2(-(at[0] - cx), at[1] - cz));
			if (t % 4 == 0) {
				server.runCommand(String.format(java.util.Locale.ROOT, "tp @p %.1f %.1f %.1f %.0f 30", cx, b.getY() + 74.0, cz, look));
			}
			context.waitTicks(10);
			String key = status.startsWith("turning round to back in to LOAD") ? "turn-load" : status.startsWith("backing in to LOAD") ? "back-load"
					: status.startsWith("loading") ? "loading" : status.startsWith("hauling") && at[0] < b.getX() + 80 && at[0] > b.getX() + 58 ? "ramp"
					: status.startsWith("backing in to DUMP") ? "back-dump" : at[3] > 0.6 ? "tipping" : tipped && at[0] > b.getX() + 30 ? "back" : null;
			if (key != null && shot.add(key)) {
				context.takeScreenshot("tailings-pit-haul-" + key);
				System.out.println("[tailings-test] pit_haul: " + key + " at tick " + t * 10 + ": " + status);
			}
			if (at[3] > 0.6) {
				tipped = true;
			}
			if (tipped && at[0] > b.getX() + 30 && at[4] == 0) {
				break;
			}
		}
		film.stop();
		server.runOnServer(srv -> System.out.println("[tailings-test] pit_haul: truck " + v[1].status() + "; fuel " + Math.round(v[1].fuel()) + " L; hopper " + hopper[0].pocket()
				+ " blocks; loader " + v[0].status() + "; fuel " + Math.round(v[0].fuel()) + " L"));
		require(server.computeOnServer(srv -> hopper[0].pocket() >= 33), "the truck should tip a full load into the hopper");
		require(shot.contains("back"), "the truck should come back after tipping");
		heard("pit_haul", "wildcatter:diesel_load", "tailings:muck_load", "tailings:muck_tip");
	}

	/**
	 * The open pit's P3 (open-pit-p3-blasting.md), the spec's film: the drill walks a 3 x 4 staggered pattern on a 14 m bench, each hole
	 * is primed, the emulsion truck charges them from two stands, they are stemmed with their cuttings and logged, the blasting machine
	 * 60 m off arms, sounds its siren and fires; the bench breaks to muck spilled off the face; a loader digs it into a truck.
	 */
	private void pitBlast(ClientGameTestContext context, TestSingleplayerContext world, TestServerContext server) {
		BlockPos b = new BlockPos(300, y + 8, 1400).offset(0, -59, 0);   // b + (x, 59, z) is the bench top; its face north at z 43
		Object[] v = new Object[4];
		server.runOnServer(srv -> {
			ServerLevel level = srv.overworld();
			for (int x = -10; x <= 70; x++) {
				for (int z = -10; z <= 84; z++) {
					int top = x >= 6 && x <= 60 && z >= 44 ? 59 : 45;
					for (int yy = 40; yy <= 75; yy++) {
						level.setBlock(b.offset(x, yy, z), (yy <= top ? Blocks.STONE : Blocks.AIR).defaultBlockState(), Block.UPDATE_CLIENTS);
					}
				}
			}
			var drill = dev.wildcatter.tailings.TailingsEntities.BLASTHOLE_DRILL.create(level, net.minecraft.world.entity.EntitySpawnReason.COMMAND);
			drill.setPos(b.getX() + 44.5, b.getY() + 60, b.getZ() + 56.5);   // its mast over the pattern's first hole, x 44, z 50
			drill.setYRot(0);
			drill.setFuel(drill.tank());
			drill.fast(40);   // the film's speed-up: 12 holes in under 2 minutes (real: about 40 minutes); kept over a chunk reload
			drill.setting("Rows", 3);
			drill.setting("Columns", 4);
			level.addFreshEntity(drill);
			System.out.println("[tailings-test] pit_blast: " + drill.go());
			v[0] = drill;
		});
		server.runCommand("tp @p " + (b.getX() + 78) + " " + (b.getY() + 92) + " " + (b.getZ() + 18) + " 45 35");
		context.waitTicks(60);
		context.takeScreenshot("tailings-pit-blast-bench");
		Filming film = film(context, world, "pit_blast");
		java.util.UUID drillId = server.computeOnServer(srv -> ((net.minecraft.world.entity.Entity) v[0]).getUUID());
		for (int t = 0; t < 1200; t++) {
			context.waitTicks(10);
			// done when it says so: a drill whose chunk is reloading is not found for a tick, which is not done
			boolean running = server.computeOnServer(srv -> !(srv.overworld().getEntity(drillId) instanceof dev.wildcatter.tailings.entity.BlastholeDrillEntity d)
					|| d.running() || !(d.status().startsWith("DONE") || d.status().contains("OUT OF FUEL") || d.status().contains("BLOCKED") || d.status().contains("NOT LEVEL")));
			if (t % 50 == 0) {
				server.runOnServer(srv -> System.out.println("[tailings-test] pit_blast: drill at " + (srv.overworld().getEntity(drillId) instanceof dev.wildcatter.tailings.entity.BlastholeDrillEntity d ? d.status() + ", running " + d.running() : "not found")));
			}
			if (t == 20) {
				context.takeScreenshot("tailings-pit-blast-drilling");
			}
			if (!running) {
				break;
			}
		}
		context.takeScreenshot("tailings-pit-blast-drilled");
		// prime every hole, then the truck charges them from two stands
		java.util.List<BlockPos> collars = server.computeOnServer(srv -> {
			ServerLevel level = srv.overworld();
			java.util.List<BlockPos> out = new java.util.ArrayList<>();
			if (level.getEntity(drillId) instanceof dev.wildcatter.tailings.entity.BlastholeDrillEntity d) {
				System.out.println("[tailings-test] pit_blast: drill " + d.status());
				d.setPos(b.getX() + 54.5, b.getY() + 60, b.getZ() + 74.5);   // out of the shot
			}
			for (BlockPos p : BlockPos.betweenClosed(b.offset(6, 59, 44), b.offset(60, 59, 80))) {
				if (level.getBlockEntity(p) instanceof dev.wildcatter.tailings.block.BlastholeBlockEntity hole && hole.isCollar()) {
					hole.addBooster();
					hole.addDetonator(1 + out.size());
					out.add(p.immutable());
				}
			}
			var truck = dev.wildcatter.tailings.TailingsEntities.EMULSION_TRUCK.create(level, net.minecraft.world.entity.EntitySpawnReason.COMMAND);
			truck.setPos(b.getX() + 38.5, b.getY() + 60, b.getZ() + 53.25);
			truck.setYRot(180);
			truck.setFuel(truck.tank());
			truck.setMatrix(dev.wildcatter.tailings.pit.Blasting.TRUCK_MATRIX_KG);
			truck.fastPump(10);
			level.addFreshEntity(truck);
			v[1] = truck;
			return out;
		});
		System.out.println("[tailings-test] pit_blast: " + collars.size() + " holes primed");
		require(collars.size() == 12, "the drill should leave 12 holes, found " + collars.size());
		java.util.UUID truckId = server.computeOnServer(srv -> ((net.minecraft.world.entity.Entity) v[1]).getUUID());
		for (double standX : new double[] {38.5, 22.5}) {
			server.runOnServer(srv -> {
				if (srv.overworld().getEntity(truckId) instanceof dev.wildcatter.tailings.entity.EmulsionTruckEntity truck) {
					truck.setPos(b.getX() + standX, b.getY() + 60, b.getZ() + 53.25);
				}
			});
			context.waitTicks(5);
			server.runOnServer(srv -> {
				if (srv.overworld().getEntity(truckId) instanceof dev.wildcatter.tailings.entity.EmulsionTruckEntity truck) {
					System.out.println("[tailings-test] pit_blast: truck " + truck.chargeAll(srv.overworld()));
				}
			});
			for (int t = 0; t < 120; t++) {
				context.waitTicks(10);
				if (t == 4) {
					context.takeScreenshot("tailings-pit-blast-charging-" + (int) standX);
				}
				if (!server.computeOnServer(srv -> srv.overworld().getEntity(truckId) instanceof dev.wildcatter.tailings.entity.EmulsionTruckEntity truck && truck.busy())) {
					break;
				}
			}
		}
		// stem with the cuttings, log, and fire from 60 m
		BlockPos firing = b.offset(30, 60, 126);
		String armed = server.computeOnServer(srv -> {
			ServerLevel level = srv.overworld();
			if (level.getEntity(truckId) instanceof dev.wildcatter.tailings.entity.EmulsionTruckEntity truck) {
				truck.setPos(b.getX() + 30.5, b.getY() + 46, b.getZ() + 6.5);   // out of the shot
			}
			ItemStack logger = new ItemStack(dev.wildcatter.tailings.TailingsItems.BLAST_LOGGER);
			for (BlockPos p : collars) {
				if (level.getBlockEntity(p) instanceof dev.wildcatter.tailings.block.BlastholeBlockEntity hole) {
					hole.charge(0, level.getGameTime() - dev.wildcatter.tailings.pit.Blasting.SLEEP_TICKS);   // the film does not wait the gassing minute
					hole.stemWithCuttings();
					dev.wildcatter.tailings.item.BlastLoggerItem.log(level, logger, p);
				}
			}
			level.setBlock(firing, TailingsBlocks.BLASTING_MACHINE.defaultBlockState(), Block.UPDATE_ALL);
			var machine = (dev.wildcatter.tailings.block.BlastingMachineBlockEntity) level.getBlockEntity(firing);
			System.out.println("[tailings-test] pit_blast: machine " + machine.load(logger));
			machine.setKey(true);
			String said = machine.arm();
			System.out.println("[tailings-test] pit_blast: " + said + " | " + machine.fire());
			return said;
		});
		require(armed.startsWith("ARMED: 12 of 12"), "the shot should arm with all 12 holes: " + armed);
		context.takeScreenshot("tailings-pit-blast-siren");
		for (int t = 0; t < 60; t++) {
			context.waitTicks(10);
			String status = server.computeOnServer(srv -> srv.overworld().getBlockEntity(firing) instanceof dev.wildcatter.tailings.block.BlastingMachineBlockEntity m ? m.status() : "gone");
			if (status.startsWith("FIRED")) {
				System.out.println("[tailings-test] pit_blast: " + status);
				break;
			}
		}
		context.waitTicks(20);
		context.takeScreenshot("tailings-pit-blast-fired");
		context.waitTicks(100);
		server.runCommand("tp @p " + (b.getX() + 30) + " " + (b.getY() + 66) + " " + (b.getZ() + 4) + " 0 25");
		context.waitTicks(40);
		context.takeScreenshot("tailings-pit-blast-muck");
		// a loader digs the muck pile at the face into a haul truck beside it
		server.runOnServer(srv -> {
			ServerLevel level = srv.overworld();
			var loader = dev.wildcatter.tailings.TailingsEntities.WHEEL_LOADER.create(level, net.minecraft.world.entity.EntitySpawnReason.COMMAND);
			loader.setPos(b.getX() + 30.5, b.getY() + 46, b.getZ() + 22.5);   // its bucket at the toe of the pile (tune when filming)
			loader.setYRot(0);
			loader.fastPasses(8);
			loader.setFuel(loader.tank());
			level.addFreshEntity(loader);
			var truck = dev.wildcatter.tailings.TailingsEntities.HAUL_TRUCK.create(level, net.minecraft.world.entity.EntitySpawnReason.COMMAND);
			truck.setPos(b.getX() + 38.5, b.getY() + 46, b.getZ() + 28.5);
			truck.setYRot(90);
			truck.setFuel(truck.tank());
			level.addFreshEntity(truck);
			v[2] = loader;
			v[3] = truck;
		});
		java.util.UUID loaderId = server.computeOnServer(srv -> ((net.minecraft.world.entity.Entity) v[2]).getUUID());
		for (int t = 0; t < 120; t++) {
			context.waitTicks(10);
			server.runOnServer(srv -> {
				if (srv.overworld().getEntity(loaderId) instanceof dev.wildcatter.tailings.entity.WheelLoaderEntity loader && !loader.busy()) {
					loader.pressJump(srv.overworld());
				}
			});
			if (t == 30) {
				context.takeScreenshot("tailings-pit-blast-loading");
			}
		}
		film.stop();
		server.runOnServer(srv -> System.out.println("[tailings-test] pit_blast: loader " + (srv.overworld().getEntity(loaderId) instanceof dev.wildcatter.tailings.entity.WheelLoaderEntity l ? l.status() : "gone")));
		heard("pit_blast", "tailings:tbm_cutter", "wildcatter:diesel_idle", "minecraft:entity.generic.explode");
	}

	/**
	 * The open pit's P2b, filmed: a haul dispatch office runs a route. In HAND the truck hauls and tips at the dump point (LOADS counts
	 * it); switched OFF it drives back and parks at the PARK stake with its engine off; back in HAND it goes to work. (The PLC's RUN in
	 * AUTO is proved by the server scenes pit_dispatch and pit_dispatch_fallback.)
	 */
	private void pitDispatch(ClientGameTestContext context, TestSingleplayerContext world, TestServerContext server) {
		BlockPos b = new BlockPos(-300, y + 8, 1400).offset(0, -59, 0);   // b + (x, 59, z) is road level
		dev.wildcatter.tailings.entity.HaulTruckEntity[] truck = new dev.wildcatter.tailings.entity.HaulTruckEntity[1];
		dev.wildcatter.tailings.block.HaulDispatchBlockEntity[] office = new dev.wildcatter.tailings.block.HaulDispatchBlockEntity[1];
		server.runOnServer(srv -> {
			ServerLevel level = srv.overworld();
			for (int x = -28; x <= 68; x++) {
				for (int z = -4; z <= 170; z++) {
					for (int yy = 54; yy <= 72; yy++) {
						level.setBlock(b.offset(x, yy, z), (yy <= 58 ? Blocks.STONE : Blocks.AIR).defaultBlockState(), Block.UPDATE_CLIENTS);
					}
				}
			}
			int[] zs = {0, 30, 70, 110, 140};
			dev.wildcatter.tailings.pit.StopKind[] kinds = {dev.wildcatter.tailings.pit.StopKind.LOAD, dev.wildcatter.tailings.pit.StopKind.WAYPOINT,
					dev.wildcatter.tailings.pit.StopKind.PARK, dev.wildcatter.tailings.pit.StopKind.WAYPOINT, dev.wildcatter.tailings.pit.StopKind.DUMP_POINT};
			java.util.List<BlockPos> route = new java.util.ArrayList<>();
			for (int i = 0; i < zs.length; i++) {
				BlockPos at = b.offset(20, 59, zs[i]);
				level.setBlock(at, TailingsBlocks.SURVEY_STAKE.defaultBlockState().setValue(dev.wildcatter.tailings.block.SurveyStakeBlock.KIND, kinds[i]), Block.UPDATE_ALL);
				route.add(at);
			}
			part(level, TailingsBlocks.HAUL_DISPATCH, b.offset(50, 59, 4));
			office[0] = (dev.wildcatter.tailings.block.HaulDispatchBlockEntity) level.getBlockEntity(b.offset(50, 59, 4));
			office[0].forcePowered(true);
			System.out.println("[tailings-test] pit_dispatch: " + office[0].addRoute(route));
			var t = dev.wildcatter.tailings.TailingsEntities.HAUL_TRUCK.create(level, net.minecraft.world.entity.EntitySpawnReason.COMMAND);
			t.setPos(b.getX() + 16.5, b.getY() + 59, b.getZ() + 40);
			t.setYRot(0);
			t.setFuel(t.tank());
			t.load().add("minecraft:sand", 30_000);
			t.refreshLoad();
			level.addFreshEntity(t);
			System.out.println("[tailings-test] pit_dispatch: truck " + t.takeRoute(level, route));
			truck[0] = t;
		});
		server.runCommand("tp @p " + (b.getX() + 75) + " " + (b.getY() + 100) + " " + (b.getZ() - 15) + " 33 38");   // over the office, down the road
		context.waitTicks(60);
		java.util.UUID id = server.computeOnServer(srv -> truck[0].getUUID());
		Runnable fresh = () -> server.runOnServer(srv -> {
			if (srv.overworld().getEntity(id) instanceof dev.wildcatter.tailings.entity.HaulTruckEntity e) {
				truck[0] = e;
			}
		});
		fresh.run();
		context.takeScreenshot("tailings-pit-dispatch-route");
		Filming film = film(context, world, "pit_dispatch");
		java.util.function.Consumer<String> follow = name -> {
			fresh.run();
			double[] at = server.computeOnServer(srv -> new double[] {truck[0].getX(), truck[0].getZ(), truck[0].getYRot()});
			double yaw = Math.toRadians(at[2]);
			double cx = at[0] + Math.cos(yaw) * 20 + Math.sin(yaw) * 12, cz = at[1] + Math.sin(yaw) * 20 - Math.cos(yaw) * 12;
			double look = Math.toDegrees(Math.atan2(-(at[0] - cx), at[1] - cz));
			server.runCommand(String.format(java.util.Locale.ROOT, "tp @p %.1f %.1f %.1f %.0f 30", cx, b.getY() + 74.0, cz, look));
		};
		// HAND: it hauls to the dump point and tips
		for (int t = 0; t < 400 && server.computeOnServer(srv -> office[0].loads(0)) == 0; t++) {
			if (t % 4 == 0) {
				follow.accept("haul");
			}
			context.waitTicks(10);
		}
		server.runOnServer(srv -> System.out.println("[tailings-test] pit_dispatch: tipped; loads " + office[0].loads(0) + ", " + Math.round(office[0].tonnesLastHour(0)) + " t in the hour; " + truck[0].status()));
		follow.accept("tipped");
		context.waitTicks(20);
		context.takeScreenshot("tailings-pit-dispatch-tipped");
		// OFF: it drives back and parks at the PARK stake
		server.runOnServer(srv -> System.out.println("[tailings-test] pit_dispatch: selector " + office[0].cycleHoa(b.offset(50, 59, 4))));
		for (int t = 0; t < 400 && !server.computeOnServer(srv -> truck[0].parked() && truck[0].speed() == 0); t++) {
			if (t % 4 == 0) {
				follow.accept("park");
			}
			context.waitTicks(10);
		}
		server.runOnServer(srv -> System.out.println("[tailings-test] pit_dispatch: " + truck[0].status() + " at z " + Math.round(truck[0].getZ() - b.getZ()) + "; engine on " + truck[0].engineOn()));
		follow.accept("parked");
		context.waitTicks(40);
		context.takeScreenshot("tailings-pit-dispatch-parked");
		require(server.computeOnServer(srv -> truck[0].parked() && Math.abs(truck[0].getZ() - b.getZ() - 70) < 8), "switched OFF, the truck should park at the PARK stake");
		// back to HAND (through AUTO): it goes to work
		server.runOnServer(srv -> {
			office[0].cycleHoa(b.offset(50, 59, 4));
			System.out.println("[tailings-test] pit_dispatch: selector " + office[0].cycleHoa(b.offset(50, 59, 4)));
		});
		for (int t = 0; t < 120 && server.computeOnServer(srv -> truck[0].parked() || Math.abs(truck[0].getZ() - b.getZ() - 70) < 15); t++) {
			if (t % 4 == 0) {
				follow.accept("work");
			}
			context.waitTicks(10);
		}
		follow.accept("work");
		context.waitTicks(20);
		context.takeScreenshot("tailings-pit-dispatch-back-to-work");
		film.stop();
		require(server.computeOnServer(srv -> !truck[0].parked()), "back in HAND, the truck should go to work");
		heard("pit_dispatch", "wildcatter:diesel_load", "tailings:muck_tip");
	}

	/**
	 * The open pit's P4, filmed: the tailings filter over the load-out drops a cake into the bin; a self-driving truck drives through the
	 * load-out's lane, is filled under the gate, loops round and goes to a DUMP POINT at a crest with no berm, where it stops reading NO
	 * BERM; the track dozer pushes a berm up along the crest; the truck backs to the berm and tips over it, down the face.
	 */
	private void pitDryStack(ClientGameTestContext context, TestSingleplayerContext world, TestServerContext server) {
		BlockPos b = new BlockPos(-700, y + 8, 1400).offset(0, -59, 0);   // b + (x, 59, z) is the bench's road level
		dev.wildcatter.tailings.entity.HaulTruckEntity[] truck = new dev.wildcatter.tailings.entity.HaulTruckEntity[1];
		dev.wildcatter.tailings.entity.TrackDozerEntity[] dozer = new dev.wildcatter.tailings.entity.TrackDozerEntity[1];
		dev.wildcatter.tailings.block.PlantBlockEntity[] filter = new dev.wildcatter.tailings.block.PlantBlockEntity[1];
		dev.wildcatter.tailings.block.PlantBlockEntity[] bin = new dev.wildcatter.tailings.block.PlantBlockEntity[1];
		server.runOnServer(srv -> {
			ServerLevel level = srv.overworld();
			// the bench, its crest at z -10 with a 6 m drop to the dump's floor beyond
			for (int x = -30; x <= 60; x++) {
				for (int z = -40; z <= 140; z++) {
					for (int yy = 44; yy <= 100; yy++) {
						boolean solid = z >= -10 ? yy <= 58 : yy <= 52;
						level.setBlock(b.offset(x, yy, z), (solid ? Blocks.STONE : Blocks.AIR).defaultBlockState(), Block.UPDATE_CLIENTS);
					}
				}
			}
			part(level, TailingsBlocks.LOADOUT_FRAME, b.offset(14, 59, 60));
			part(level, TailingsBlocks.LOADOUT_BIN, b.offset(15, 69, 61));
			part(level, TailingsBlocks.TAILINGS_FILTER, b.offset(17, 76, 58));
			part(level, TailingsBlocks.TAILINGS_THICKENER, b.offset(30, 59, 75));
			var frame = (dev.wildcatter.tailings.block.PlantBlockEntity) level.getBlockEntity(b.offset(14, 59, 60));
			frame.forceRunning();
			bin[0] = (dev.wildcatter.tailings.block.PlantBlockEntity) level.getBlockEntity(b.offset(15, 69, 61));
			bin[0].setDryKg(80_000);
			filter[0] = (dev.wildcatter.tailings.block.PlantBlockEntity) level.getBlockEntity(b.offset(17, 76, 58));
			filter[0].forceRunning();
			filter[0].fastCycle(30);
			filter[0].back().offer(dev.wildcatter.fluid.Product.TAILS_SLURRY, 26_000, dev.wildcatter.fluid.SlurryAssay.of(java.util.Map.of(dev.wildcatter.tailings.pit.DryStack.SOLIDS_KEY, 0.6)));
			int[] zs = {-8, 25, 45, 59};
			dev.wildcatter.tailings.pit.StopKind[] kinds = {dev.wildcatter.tailings.pit.StopKind.DUMP_POINT, dev.wildcatter.tailings.pit.StopKind.WAYPOINT,
					dev.wildcatter.tailings.pit.StopKind.WAYPOINT, dev.wildcatter.tailings.pit.StopKind.LOADOUT};
			java.util.List<BlockPos> route = new java.util.ArrayList<>();
			for (int i = 0; i < zs.length; i++) {
				BlockPos at = b.offset(19, 59, zs[i]);
				level.setBlock(at, TailingsBlocks.SURVEY_STAKE.defaultBlockState().setValue(dev.wildcatter.tailings.block.SurveyStakeBlock.KIND, kinds[i]), Block.UPDATE_ALL);
				route.add(at);
			}
			var t = dev.wildcatter.tailings.TailingsEntities.HAUL_TRUCK.create(level, net.minecraft.world.entity.EntitySpawnReason.COMMAND);
			t.setPos(b.getX() + 19.5, b.getY() + 59, b.getZ() + 38);
			t.setYRot(0);
			t.setFuel(t.tank());
			level.addFreshEntity(t);
			System.out.println("[tailings-test] pit_dry_stack: truck " + t.takeRoute(level, route));
			truck[0] = t;
			var d = dev.wildcatter.tailings.TailingsEntities.TRACK_DOZER.create(level, net.minecraft.world.entity.EntitySpawnReason.COMMAND);
			// along the crest, heading east, the crest on its left; the berm (a block a metre) must run past the stake at x 19 across
			// the whole tail (x 16 to 22), so it starts at x 6 with blocks to spare
			d.setPos(b.getX() + 6.5, b.getY() + 59, b.getZ() - 3.5);
			d.setYRot(-90);
			d.setFuel(d.tank());
			d.loadBlade(dev.wildcatter.tailings.pit.PitMass.DRY_TAILS, 24);
			level.addFreshEntity(d);
			dozer[0] = d;
		});
		server.runCommand("tp @p " + (b.getX() + 45) + " " + (b.getY() + 90) + " " + (b.getZ() + 40) + " 120 35");   // over the load-out
		context.waitTicks(60);
		java.util.UUID truckId = server.computeOnServer(srv -> truck[0].getUUID());
		java.util.UUID dozerId = server.computeOnServer(srv -> dozer[0].getUUID());
		Runnable fresh = () -> server.runOnServer(srv -> {
			if (srv.overworld().getEntity(truckId) instanceof dev.wildcatter.tailings.entity.HaulTruckEntity e) {
				truck[0] = e;
			}
			if (srv.overworld().getEntity(dozerId) instanceof dev.wildcatter.tailings.entity.TrackDozerEntity e) {
				dozer[0] = e;
			}
		});
		fresh.run();
		context.takeScreenshot("tailings-pit-dry-stack-layout");
		Filming film = film(context, world, "pit_dry_stack");
		// the filter's cycle: a cake drops into the bin
		for (int t = 0; t < 120 && server.computeOnServer(srv -> filter[0].cycles()) == 0; t++) {
			context.waitTicks(10);
		}
		server.runOnServer(srv -> System.out.println("[tailings-test] pit_dry_stack: cake dropped; bin " + Math.round(bin[0].dryKg() / 1000) + " t; " + filter[0].status()));
		context.takeScreenshot("tailings-pit-dry-stack-cake");
		java.util.function.Consumer<double[]> follow = at -> {
			double yaw = Math.toRadians(at[2]);
			double cx = at[0] + Math.cos(yaw) * 22 + Math.sin(yaw) * 12, cz = at[1] + Math.sin(yaw) * 22 - Math.cos(yaw) * 12;
			double look = Math.toDegrees(Math.atan2(-(at[0] - cx), at[1] - cz));
			server.runCommand(String.format(java.util.Locale.ROOT, "tp @p %.1f %.1f %.1f %.0f 30", cx, b.getY() + 76.0, cz, look));
		};
		Runnable followTruck = () -> {
			fresh.run();
			follow.accept(server.computeOnServer(srv -> new double[] {truck[0].getX(), truck[0].getZ(), truck[0].getYRot()}));
		};
		// through the lane and filled under the gate
		for (int t = 0; t < 400 && server.computeOnServer(srv -> truck[0].load().room()) > 0; t++) {
			if (t % 4 == 0) {
				followTruck.run();
			}
			context.waitTicks(10);
		}
		server.runOnServer(srv -> System.out.println("[tailings-test] pit_dry_stack: filled " + truck[0].load().tonnes() + " at z " + Math.round(truck[0].getZ() - b.getZ())));
		context.takeScreenshot("tailings-pit-dry-stack-loaded");
		// round the loop, back through, to the dump: no berm
		for (int t = 0; t < 600 && !server.computeOnServer(srv -> truck[0].status().startsWith("NO BERM")); t++) {
			if (t % 4 == 0) {
				followTruck.run();
			}
			context.waitTicks(10);
		}
		server.runOnServer(srv -> System.out.println("[tailings-test] pit_dry_stack: " + truck[0].status()));
		context.takeScreenshot("tailings-pit-dry-stack-no-berm");
		require(server.computeOnServer(srv -> truck[0].status().startsWith("NO BERM")), "the truck should stop short of an unbermed crest");
		// the dozer pushes up the berm along the crest
		server.runOnServer(srv -> dozer[0].testDrive(new net.minecraft.world.entity.player.Input(true, false, false, false, false, false, false)));
		for (int t = 0; t < 300 && server.computeOnServer(srv -> dozer[0].getX() - b.getX() < 30 && dozer[0].bladeBlocks() > 0); t++) {
			if (t % 4 == 0) {
				fresh.run();
				follow.accept(server.computeOnServer(srv -> new double[] {dozer[0].getX(), dozer[0].getZ(), dozer[0].getYRot()}));
			}
			context.waitTicks(10);
		}
		server.runOnServer(srv -> {
			dozer[0].testDrive(net.minecraft.world.entity.player.Input.EMPTY);
			System.out.println("[tailings-test] pit_dry_stack: dozer " + dozer[0].status());
		});
		context.takeScreenshot("tailings-pit-dry-stack-berm");
		// the truck backs to the berm and tips over it
		for (int t = 0; t < 400 && !server.computeOnServer(srv -> truck[0].load().empty()); t++) {
			if (t % 4 == 0) {
				followTruck.run();
			}
			context.waitTicks(10);
		}
		server.runOnServer(srv -> System.out.println("[tailings-test] pit_dry_stack: " + truck[0].status()));
		context.waitTicks(40);
		context.takeScreenshot("tailings-pit-dry-stack-tipped");
		film.stop();
		require(server.computeOnServer(srv -> truck[0].load().empty()), "with the berm up, the truck should tip over it");
		heard("pit_dry_stack", "wildcatter:diesel_load", "tailings:muck_tip");
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
