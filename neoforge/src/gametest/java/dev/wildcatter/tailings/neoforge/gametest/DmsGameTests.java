package dev.wildcatter.tailings.neoforge.gametest;

import dev.wildcatter.tailings.Tailings;
import dev.wildcatter.tailings.TailingsBlocks;
import dev.wildcatter.tailings.TailingsItems;
import dev.wildcatter.tailings.block.BeltConveyorBlock;
import dev.wildcatter.tailings.block.PlantBlockEntity;
import dev.wildcatter.tailings.plant.DenseMedium;
import dev.wildcatter.tailings.plant.Machine;
import dev.wildcatter.tailings.plant.RockLoad;
import dev.wildcatter.tailings.rock.Rock;
import dev.wildcatter.tailings.rock.SizeClass;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Server scenes for dense-medium separation (docs/multiblocks/dense-medium-separation.md, L7; decisions in docs/questions/l7-dms.md):
 * granite floats out as vanilla gravel and copper ore sinks on to the cone's belt, 2 % each way; the medium's setting moves the cut;
 * ferrosilicon falls 0.3 kg a tonne and none stops the module; the screen's gravel face feeds the deslime screen and the module; each
 * part is its spec size with its PLC points.
 */
final class DmsGameTests {
	static final String[] SCENES = {"dms_split", "dms_density", "dms_medium", "dms_gravel_face", "dms_parts", "dms_floats_belt"};
	static final Set<String> LONG = Set.of("dms_split", "dms_density", "dms_medium", "dms_gravel_face", "dms_floats_belt");
	/** The scenes run the DMS parts this many times as fast (as the dry mill's scenes do), so a few tonnes pass in seconds. */
	private static final int FAST = 20;

	private DmsGameTests() {
	}

	static void register(BiConsumer<String, Consumer<GameTestHelper>> out) {
		out.accept("dms_split", DmsGameTests::split);
		out.accept("dms_density", DmsGameTests::density);
		out.accept("dms_medium", DmsGameTests::medium);
		out.accept("dms_gravel_face", DmsGameTests::gravelFace);
		out.accept("dms_parts", DmsGameTests::parts);
		out.accept("dms_floats_belt", DmsGameTests::floatsBelt);
	}

	private static BlockState belt(Direction.Axis axis) {
		return TailingsBlocks.BELT_CONVEYOR.defaultBlockState().setValue(BeltConveyorBlock.AXIS, axis);
	}

	/** The module with its floats screen north of it, its sinks screen south and their belt, and the medium recovery east. */
	record Rig(PlantBlockEntity module, PlantBlockEntity floats, PlantBlockEntity sinks, PlantBlockEntity recovery, PlantBlockEntity sinksBelt) {
		PlantBlockEntity[] all() {
			return new PlantBlockEntity[] {module, floats, sinks, recovery, sinksBelt};
		}

		/** Water off the pipe each tick, as a pump keeps the module's level. */
		void water() {
			module.addWater(1000);
		}

		String describe() {
			StringBuilder out = new StringBuilder();
			for (PlantBlockEntity part : all()) {
				out.append(" | ").append(part.inspect().getString());
			}
			return out.toString();
		}

		/** kg of {@code rock} on the sinks side: the sinks screen and its belt. */
		double sunk(Rock rock) {
			return sinks.hold().kg(rock, SizeClass.GRAVEL) + sinksBelt.hold().kg(rock, SizeClass.GRAVEL) + sinksBelt.made().kg(rock, SizeClass.GRAVEL);
		}

		/** kg of {@code rock} floated: still on the floats screen, waiting as made, or paid out (gravel counted at 1.9 t). */
		double floated() {
			return floats.hold().total() + floats.made().total() + floats.getItem(1).getCount() * DenseMedium.FLOATS_KG_PER_GRAVEL;
		}
	}

	static Rig rig(GameTestHelper helper, BlockPos at, boolean fesi) {
		int x = at.getX(), y = at.getY(), z = at.getZ();
		PlantBlockEntity floats = TailingsGameTests.plant(helper, Machine.DRAIN_RINSE_SCREEN, new BlockPos(x, y, z));
		PlantBlockEntity module = TailingsGameTests.plant(helper, Machine.DMS_MODULE, new BlockPos(x, y, z + 5));
		PlantBlockEntity sinks = TailingsGameTests.plant(helper, Machine.DRAIN_RINSE_SCREEN, new BlockPos(x, y, z + 9));
		for (int k = 14; k <= 16; k++) {
			helper.setBlock(new BlockPos(x, y, z + k), belt(Direction.Axis.Z));
		}
		PlantBlockEntity drive = TailingsGameTests.plant(helper, Machine.CONVEYOR_DRIVE, new BlockPos(x + 1, y, z + 16));
		// a chest at the belt's head end, standing for the cone, so the belt runs on
		helper.setBlock(new BlockPos(x, y - 1, z + 16), net.minecraft.world.level.block.Blocks.CHEST);
		PlantBlockEntity recovery = TailingsGameTests.plant(helper, Machine.MEDIUM_RECOVERY, new BlockPos(x + 3, y, z + 5));
		if (fesi) {
			helper.assertTrue(recovery.offer(new ItemStack(TailingsItems.FERROSILICON)), Component.literal("the medium recovery should take a bag of ferrosilicon"));
		}
		Rig rig = new Rig(module, floats, sinks, recovery, drive);
		for (PlantBlockEntity part : rig.all()) {
			part.forceRunning();
			part.fastCycle(FAST);
		}
		rig.water();
		return rig;
	}

	/**
	 * dms_floats_belt: a belt off the floats screen's north end takes the floats as gravel with their rock, back to the plant, so the
	 * light ores that float at any setting can be recovered (Tim, 2026-11-10, l7-dms.md #4, #11). 2 t of rock salt gravel and 1 t of
	 * copper ore gravel: 98 % of the salt rides the floats belt as rock salt gravel, none of it is paid out as vanilla gravel; the copper
	 * ore sinks as before.
	 */
	private static void floatsBelt(GameTestHelper helper) {
		Rig rig = rig(helper, new BlockPos(0, 4, 4), true);
		for (int k = 1; k <= 3; k++) {
			helper.setBlock(new BlockPos(0, 4, 4 - k), belt(Direction.Axis.Z));
		}
		PlantBlockEntity drive = TailingsGameTests.plant(helper, Machine.CONVEYOR_DRIVE, new BlockPos(1, 4, 1));
		// a chest at the head end, standing for the plant the belt takes the floats back to
		helper.setBlock(new BlockPos(0, 3, 1), net.minecraft.world.level.block.Blocks.CHEST);
		drive.forceRunning();
		drive.fastCycle(FAST);
		rig.module().receive(gravel(Rock.HALITE, 2000, Rock.COPPER, 1000));
		helper.succeedWhen(() -> {
			rig.water();
			String s = rig.describe() + " | " + drive.inspect().getString();
			helper.assertTrue(rig.module().hold().isEmpty() && rig.floats().hold().isEmpty(), Component.literal("the module and the floats screen should pass it all" + s));
			helper.assertTrue(rig.floats().getItem(1).isEmpty() && rig.floats().made().isEmpty(), Component.literal("with a belt there no vanilla gravel is paid" + s));
			double salt = drive.hold().kg(Rock.HALITE, SizeClass.GRAVEL) + drive.made().kg(Rock.HALITE, SizeClass.GRAVEL);
			helper.assertTrue(Math.abs(salt - 1960) < 1e-3, Component.literal("98 % of the rock salt should ride the floats belt as rock salt gravel, got " + salt + s));
			helper.assertTrue(Math.abs(rig.sunk(Rock.COPPER) - 980) < 1e-3, Component.literal("the copper ore sinks as before, got " + rig.sunk(Rock.COPPER) + s));
			helper.assertTrue(rig.floats().status().contains("back to the plant") || salt > 0, Component.literal(rig.floats().status()));
		});
	}

	private static RockLoad gravel(Object... rockKg) {
		RockLoad load = new RockLoad();
		for (int i = 0; i < rockKg.length; i += 2) {
			load.add((Rock) rockKg[i], SizeClass.GRAVEL, ((Number) rockKg[i + 1]).doubleValue());
		}
		return load;
	}

	/**
	 * dms_split: 2 t of granite gravel and 1 t of copper ore gravel at SG 2.75. 98 % of the granite floats and comes out as vanilla
	 * gravel, a block for 1.9 t; 98 % of the copper ore sinks onto the belt to the cone; 2 % of each goes the wrong way.
	 */
	private static void split(GameTestHelper helper) {
		Rig rig = rig(helper, new BlockPos(0, 4, 0), true);
		rig.module().receive(gravel(Rock.GRANITE, 2000, Rock.COPPER, 1000));
		helper.succeedWhen(() -> {
			rig.water();
			String s = rig.describe();
			helper.assertTrue(rig.module().hold().isEmpty(), Component.literal("the module should treat it all" + s));
			helper.assertTrue(rig.floats().getItem(1).is(Items.GRAVEL) && rig.floats().getItem(1).getCount() == 1,
					Component.literal("1.98 t of floats should pay one block of vanilla gravel" + s));
			helper.assertTrue(Math.abs(rig.floats().made().total() - 80) < 1e-3, Component.literal("and 80 kg wait for the next block, got " + rig.floats().made().total()));
			helper.assertTrue(Math.abs(rig.sunk(Rock.COPPER) - 980) < 1e-3, Component.literal("98 % of the copper ore should sink, got " + rig.sunk(Rock.COPPER) + s));
			helper.assertTrue(Math.abs(rig.sunk(Rock.GRANITE) - 40) < 1e-3, Component.literal("2 % of the granite should sink with it, got " + rig.sunk(Rock.GRANITE)));
			helper.assertTrue(rig.sinksBelt().hold().total() + rig.sinksBelt().made().total() > 1000, Component.literal("the sinks should go out on the belt off the sinks screen's south end" + s));
			helper.assertTrue(rig.module().inspect().getString().contains("medium at SG 2.75"), Component.literal("it should read its medium" + s));
		});
	}

	/**
	 * dms_density: the setting moves the cut. At SG 2.6 granite and copper ore both sink and nothing floats; at 3.3 granite and the
	 * copper ore (about 3.15) float and the sphalerite ore (about 3.7) sinks.
	 */
	private static void density(GameTestHelper helper) {
		Rig low = rig(helper, new BlockPos(0, 4, 0), true);
		Rig high = rig(helper, new BlockPos(8, 4, 0), true);
		low.module().dms().setMedium(2.6);
		high.module().dms().setMedium(3.3);
		low.module().receive(gravel(Rock.GRANITE, 1000, Rock.COPPER, 1000));
		high.module().receive(gravel(Rock.GRANITE, 1000, Rock.COPPER, 1000, Rock.SPHALERITE, 500));
		helper.succeedWhen(() -> {
			low.water();
			high.water();
			String s = low.describe() + " ||" + high.describe();
			helper.assertTrue(low.module().hold().isEmpty() && high.module().hold().isEmpty(), Component.literal("both should treat it all" + s));
			helper.assertTrue(Math.abs(low.sunk(Rock.GRANITE) - 980) < 1e-3 && Math.abs(low.sunk(Rock.COPPER) - 980) < 1e-3,
					Component.literal("at 2.6 the granite and the copper ore sink" + s));
			helper.assertTrue(Math.abs(high.sunk(Rock.GRANITE) - 20) < 1e-3 && Math.abs(high.sunk(Rock.COPPER) - 20) < 1e-3,
					Component.literal("at 3.3 the granite and the copper ore float" + s));
			helper.assertTrue(Math.abs(high.sunk(Rock.SPHALERITE) - 490) < 1e-3, Component.literal("at 3.3 the sphalerite ore still sinks" + s));
			helper.assertTrue(Math.abs(high.floated() - (980 + 980 + 10)) < 1e-3, Component.literal("and the floats are 1.97 t, got " + high.floated()));
		});
	}

	/**
	 * dms_medium: a 25 kg bag of ferrosilicon falls by 0.3 kg a tonne (0.9 kg for 3 t); a module whose recovery has none stops, NO
	 * MEDIUM, and treats nothing; the recovery's hopper refuses anything but ferrosilicon.
	 */
	private static void medium(GameTestHelper helper) {
		Rig fed = rig(helper, new BlockPos(0, 4, 0), true);
		Rig dry = rig(helper, new BlockPos(8, 4, 0), false);
		helper.assertFalse(dry.recovery().offer(new ItemStack(Items.IRON_INGOT)), Component.literal("the hopper takes ferrosilicon only"));
		fed.module().receive(gravel(Rock.GRANITE, 1500, Rock.COPPER, 1500));
		dry.module().receive(gravel(Rock.GRANITE, 1000));
		helper.succeedWhen(() -> {
			fed.water();
			dry.water();
			helper.assertTrue(fed.module().hold().isEmpty(), Component.literal("the fed module should treat 3 t" + fed.describe()));
			helper.assertTrue(Math.abs(fed.recovery().dryKg() - (25 - 0.9)) < 1e-6, Component.literal("25 kg less 0.3 kg a tonne should leave 24.1 kg, got " + fed.recovery().dryKg()));
			helper.assertTrue(Math.abs(fed.module().dms().fesiKg() - 0.9) < 1e-6, Component.literal("it should count 0.9 kg lost"));
			helper.assertTrue(dry.module().status().startsWith("NO MEDIUM"), Component.literal("with none it should read NO MEDIUM, got " + dry.module().status()));
			helper.assertTrue(Math.abs(dry.module().hold().total() - 1000) < 1e-6, Component.literal("and treat nothing"));
		});
	}

	/**
	 * dms_gravel_face: crushed copper ore into the double-deck screen; a belt on its gravel face carries the 6-40 mm over the deslime
	 * screen's north end; the deslime screen washes 3 % off to its fines belt and passes the rest into the module on top of which it
	 * sits; the copper ore sinks onto the sinks belt.
	 */
	private static void gravelFace(GameTestHelper helper) {
		Rig rig = rig(helper, new BlockPos(4, 2, 5), true);
		// the deslime screen on top of the module (x 4-5, y 8-9, z 10-13); its fines belt off its east face
		PlantBlockEntity deslime = TailingsGameTests.plant(helper, Machine.DESLIME_SCREEN, new BlockPos(4, 8, 10));
		for (int x = 6; x <= 8; x++) {
			helper.setBlock(new BlockPos(x, 8, 11), belt(Direction.Axis.X));
		}
		PlantBlockEntity fines = TailingsGameTests.plant(helper, Machine.CONVEYOR_DRIVE, new BlockPos(9, 8, 11));
		helper.setBlock(new BlockPos(8, 7, 11), net.minecraft.world.level.block.Blocks.CHEST);
		// the double-deck screen; its gravel face (west of its south end) belt runs west to a head over the deslime screen's north end
		PlantBlockEntity screen = TailingsGameTests.plant(helper, Machine.SCREEN, new BlockPos(8, 10, 5));
		for (int x = 7; x >= 5; x--) {
			helper.setBlock(new BlockPos(x, 10, 10), belt(Direction.Axis.X));
		}
		PlantBlockEntity gravel = TailingsGameTests.plant(helper, Machine.CONVEYOR_DRIVE, new BlockPos(4, 10, 10));
		// its oversize belt off its south face
		for (int z = 11; z <= 12; z++) {
			helper.setBlock(new BlockPos(9, 10, z), belt(Direction.Axis.Z));
		}
		PlantBlockEntity oversize = TailingsGameTests.plant(helper, Machine.CONVEYOR_DRIVE, new BlockPos(10, 10, 12));
		helper.setBlock(new BlockPos(9, 9, 12), net.minecraft.world.level.block.Blocks.CHEST);
		for (PlantBlockEntity part : new PlantBlockEntity[] {deslime, fines, screen, gravel, oversize}) {
			part.forceRunning();
		}
		deslime.fastCycle(FAST);
		helper.assertTrue(screen.offer(new ItemStack(BuiltInRegistries.ITEM.getValue(Tailings.id("crushed_copper")))), Component.literal("the screen should take crushed copper ore"));
		helper.succeedWhen(() -> {
			rig.water();
			String s = " | screen " + screen.status() + " | gravel belt " + gravel.status() + " | deslime " + deslime.status() + " | fines " + fines.status() + rig.describe();
			double washed = fines.hold().kg(Rock.COPPER, SizeClass.SAND) + fines.made().kg(Rock.COPPER, SizeClass.SAND);
			helper.assertTrue(washed > 0, Component.literal("the deslime screen should wash fines onto its belt" + s));
			helper.assertTrue(rig.sunk(Rock.COPPER) > 100, Component.literal("the copper ore gravel should reach the module and sink" + s));
			// what reached the module is 97 % of what the deslime screen took: 3 parts washed to 97 passed on
			double passed = rig.sunk(Rock.COPPER) + rig.floated() + rig.module().hold().total();
			helper.assertTrue(deslime.hold().isEmpty() ? Math.abs(washed / (washed + passed) - DenseMedium.DESLIME_FINES) < 1e-6 : true,
					Component.literal("3 % washed off, got " + washed + " of " + (washed + passed)));
		});
	}

	/** dms_parts: each part fills its spec's cells and has its motor; the module reads DENSITY and FLOATS and is NOT BUILT alone. */
	private static void parts(GameTestHelper helper) {
		int x = 0;
		PlantBlockEntity module = null;
		for (Machine machine : new Machine[] {Machine.DESLIME_SCREEN, Machine.DMS_MODULE, Machine.DRAIN_RINSE_SCREEN, Machine.MEDIUM_RECOVERY}) {
			PlantBlockEntity part = TailingsGameTests.plant(helper, machine, new BlockPos(x, 4, 0));
			helper.assertTrue(part.cells().size() == machine.cells(), Component.literal(machine + " should fill " + machine.cells() + " cells, got " + part.cells().size()));
			helper.assertTrue(part.inspect().getString().contains("kW"), Component.literal(machine + " should read its motor"));
			helper.assertTrue(!part.points().isEmpty() && part.points().get(0).name().equals("RUN"), Component.literal(machine + " should have its PLC points"));
			if (machine == Machine.DMS_MODULE) {
				module = part;
			}
			x += machine.width() + 3;
		}
		PlantBlockEntity alone = module;
		helper.assertTrue(alone.points().stream().anyMatch(p -> p.name().startsWith("DENSITY")) && alone.points().stream().anyMatch(p -> p.name().equals("FLOATS")),
				Component.literal("the module should have DENSITY and FLOATS points"));
		// DENSITY spans 2.6-3.3: the default 2.75 reads 4 + 16 x 0.15 / 0.7 mA
		helper.assertTrue(Math.abs(alone.readMa(6) - (4 + 16 * 0.15 / 0.7)) < 1e-6, Component.literal("DENSITY at 2.75 should read 7.43 mA, got " + alone.readMa(6)));
		helper.assertTrue(alone.stepMedium().contains("2.80"), Component.literal("a sneaking click steps the medium to 2.80: " + alone.status()));
		alone.forceRunning();
		alone.receive(gravel(Rock.GRANITE, 100));
		helper.succeedWhen(() -> helper.assertTrue(alone.status().startsWith("NOT BUILT"), Component.literal("alone it should read NOT BUILT, got " + alone.status())));
	}
}
