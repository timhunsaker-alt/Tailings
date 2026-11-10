package dev.wildcatter.tailings.neoforge.gametest;

import dev.wildcatter.fluid.Product;
import dev.wildcatter.fluid.SlurryAssay;
import dev.wildcatter.multiblock.BigParts;
import dev.wildcatter.tailings.TailingsBlocks;
import dev.wildcatter.tailings.block.PlantBlockEntity;
import dev.wildcatter.tailings.block.Feeds;
import dev.wildcatter.tailings.plant.Machine;
import dev.wildcatter.tailings.plant.PitFlotation;
import dev.wildcatter.tailings.plant.Reagent;
import dev.wildcatter.tailings.rock.Mineral;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Server scenes for the pit line's flotation (docs/multiblocks/pit-flotation.md, P7c; decisions in docs/questions/p7-pit-line.md): a
 * row of eight tank cells floats 75 % of the copper, 35/15/8/5/4/3/3/2 cell by cell, through the column cleaners to the existing
 * concentrate thickener and filter; four cells float 63 %; the blower stopped stops the row (NO AIR); the 45 m thickener sends its
 * underflow to Wildcatter's lined cell and its water back; every part is its spec size with its PLC points.
 */
final class PitFlotationGameTests {
	static final String[] SCENES = {"p7_tank_row", "p7_row_short", "p7_no_air", "p7_tails", "p7c_parts"};
	static final Map<String, Integer> TICKS = Map.of("p7_tank_row", 1600, "p7_row_short", 1600, "p7_no_air", 600, "p7_tails", 1200, "p7c_parts", 200);
	static final Map<String, Integer> PADDING = Map.of("p7_tank_row", 60, "p7_row_short", 60, "p7_no_air", 50, "p7_tails", 90, "p7c_parts", 90);
	/** Chunks each scene reaches from its origin: {x from, x to, z from, z to}. */
	private static final Map<String, int[]> REACH = Map.of("p7_tank_row", new int[] {-1, 2, -1, 7}, "p7_row_short", new int[] {-1, 2, -1, 5},
			"p7_no_air", new int[] {-1, 2, -1, 4}, "p7_tails", new int[] {-1, 4, -2, 5}, "p7c_parts", new int[] {-1, 6, -1, 4});
	static final Set<String> ALL = Set.of(SCENES);
	private static final String COPPER = Mineral.COPPER.mineralItemId();

	private PitFlotationGameTests() {
	}

	static void register(BiConsumer<String, Consumer<GameTestHelper>> out) {
		out.accept("p7_tank_row", wide("p7_tank_row", PitFlotationGameTests::tankRow));
		out.accept("p7_row_short", wide("p7_row_short", PitFlotationGameTests::rowShort));
		out.accept("p7_no_air", wide("p7_no_air", PitFlotationGameTests::noAir));
		out.accept("p7_tails", wide("p7_tails", PitFlotationGameTests::tails));
		out.accept("p7c_parts", wide("p7c_parts", PitFlotationGameTests::parts));
	}

	static int ticks(String scene, int otherwise) {
		return TICKS.getOrDefault(scene, otherwise);
	}

	static int padding(String scene, int otherwise) {
		return PADDING.getOrDefault(scene, otherwise);
	}

	private static Consumer<GameTestHelper> wide(String name, Consumer<GameTestHelper> scene) {
		int[] reach = REACH.get(name);
		return helper -> {
			ChunkPos origin = ChunkPos.containing(helper.absolutePos(BlockPos.ZERO));
			for (int dx = reach[0]; dx <= reach[1]; dx++) {
				for (int dz = reach[2]; dz <= reach[3]; dz++) {
					helper.getLevel().setChunkForced(origin.x() + dx, origin.z() + dz, true);
				}
			}
			scene.accept(helper);
		};
	}

	static PlantBlockEntity plant(GameTestHelper helper, Machine machine, BlockPos pos) {
		return TailingsGameTests.plant(helper, machine, pos);
	}

	static Block wildcatter(String id) {
		return BuiltInRegistries.BLOCK.getValue(Identifier.fromNamespaceAndPath("wildcatter", id));
	}

	/** A little over four nuggets of copper in kg (copper is counted in ingots of 100 kg), so the rung's 75 % pays three. */
	static double fourNuggets() {
		return 4.04 * Mineral.COPPER.nuggetAmount().doubleValue() * Mineral.COPPER.kgPerUnit();
	}

	/** The row as built: its tank, cells, column, blower, the concentrate thickener and filter, and the pond its tails go to. */
	record Row(PlantBlockEntity tank, List<PlantBlockEntity> cells, PlantBlockEntity column, PlantBlockEntity blower, PlantBlockEntity thickener,
			PlantBlockEntity filter, PlantBlockEntity pond) {
		double floated() {
			double kg = 0;
			for (PlantBlockEntity cell : cells) {
				kg += cell.pit().totalKg();
			}
			return kg;
		}

		String describe() {
			StringBuilder out = new StringBuilder(String.format("floated %.3f kg, pond copper %.3f kg, column %s, thickener %s; ", floated(), pondCopper(pond),
					column.inspect().getString(), thickener.inspect().getString())).append(tank.status());
			for (PlantBlockEntity cell : cells) {
				out.append(" | ").append(cell.status());
			}
			return out.append(" | ").append(column.status()).append(" | ").append(blower.status()).append(" | ").append(thickener.status()).append(" | ")
					.append(filter.status()).toString();
		}
	}

	/**
	 * The row along z at y 4: fines slurry holding {@code copper} kg in 1,000 kg, the large conditioning tank, {@code n} tank cells joined
	 * by pipe south face to north face, their concentrate header on their east faces to a column cell, the column's cleaned concentrate
	 * to the concentrate thickener and filter, the last cell's tails to a pond. The large blower's air line, Wildcatter's air tubing,
	 * runs over the cells' tops and to the column. Every part runs but the blower, which runs if {@code air}.
	 */
	static Row row(GameTestHelper helper, int n, double copper, boolean air) {
		int y = 4;
		TailingsGameTests.fines(helper, new BlockPos(0, y, 0), 1000, Map.of(COPPER, copper / 1000));
		TailingsGameTests.pipe(helper, new BlockPos(0, y, 2), new BlockPos(1, y, 2));
		PlantBlockEntity tank = plant(helper, Machine.CONDITIONING_TANK_LARGE, new BlockPos(0, y, 3));
		TailingsGameTests.pipe(helper, new BlockPos(3, y, 11));
		List<PlantBlockEntity> cells = new ArrayList<>();
		Block tubing = wildcatter("air_tubing");
		for (int k = 0; k < n; k++) {
			int z = 12 + 8 * k;
			cells.add(plant(helper, Machine.TANK_CELL, new BlockPos(0, y, z)));
			for (int dz = 0; dz < 8; dz++) {
				helper.setBlock(new BlockPos(7, y, z + dz), wildcatter("gas_line"));   // the concentrate header along their east faces
				helper.setBlock(new BlockPos(3, y + 7, z + dz), tubing);              // the air line over their tops
			}
			TailingsGameTests.pipe(helper, new BlockPos(3, y, z + 7));                 // tails on, to the next cell or the pond
		}
		int end = 12 + 8 * n;   // the z past the last cell's tails pipe
		PlantBlockEntity pond = plant(helper, Machine.TAILINGS_POND, new BlockPos(0, y, end));
		PlantBlockEntity column = plant(helper, Machine.COLUMN_CELL, new BlockPos(6, y, end + 1));
		helper.setBlock(new BlockPos(7, y, end), wildcatter("gas_line"));
		for (int z = end; z <= end + 2; z++) {
			helper.setBlock(new BlockPos(3, y + 7, z), tubing);
		}
		helper.setBlock(new BlockPos(4, y + 7, end + 2), tubing);
		helper.setBlock(new BlockPos(5, y + 7, end + 2), tubing);                       // to the column's west face
		TailingsGameTests.pipe(helper, new BlockPos(7, y, end + 5));
		PlantBlockEntity thickener = plant(helper, Machine.THICKENER, new BlockPos(4, y, end + 6));
		TailingsGameTests.pipe(helper, new BlockPos(7, y, end + 14));
		PlantBlockEntity filter = plant(helper, Machine.FILTER, new BlockPos(6, y, end + 15));
		// the blower beside cell 1, its air up into the line
		PlantBlockEntity blower = plant(helper, Machine.FLOTATION_BLOWER_LARGE, new BlockPos(9, y, 12));
		for (int x = 4; x <= 10; x++) {
			helper.setBlock(new BlockPos(x, y + 7, 13), tubing);
		}
		for (int dy = 4; dy <= 6; dy++) {
			helper.setBlock(new BlockPos(10, y + dy, 13), tubing);
		}
		for (PlantBlockEntity part : List.of(tank, column, thickener, filter)) {
			part.forceRunning();
		}
		cells.forEach(PlantBlockEntity::forceRunning);
		if (air) {
			blower.forceRunning();
		}
		TailingsGameTests.reagents(helper, tank, Reagent.LIME, Reagent.XANTHATE, Reagent.MIBC);
		return new Row(tank, cells, column, blower, thickener, filter, pond);
	}

	static int nuggets(PlantBlockEntity filter) {
		return filter.countIn(new int[] {1, 2}, Feeds.stack(Mineral.COPPER.nuggetId(), 1));
	}

	/** The copper left in the pond's tails, under its own name. */
	static double pondCopper(PlantBlockEntity pond) {
		return PitFlotation.plain(pond.back().assayOf(Product.TAILS_SLURRY)).kg(COPPER, pond.back().held(Product.TAILS_SLURRY));
	}

	/**
	 * p7_tank_row: copper fines slurry through eight cells: each floats its share of the block's copper, 35, 15, 8, 5, 4, 3, 3 and 2 %,
	 * 75 % in all; the column cleans it and the existing thickener and filter pay it, three nuggets of a little over four, as the small bank
	 * pays; the other quarter goes to the tails.
	 */
	private static void tankRow(GameTestHelper helper) {
		double copper = fourNuggets();
		Row row = row(helper, 8, copper, true);
		helper.succeedWhen(() -> {
			helper.assertTrue(nuggets(row.filter()) == 3, Component.literal("the filter should pay 3 copper nuggets, got " + nuggets(row.filter()) + ": " + row.describe()));
			for (int k = 0; k < 8; k++) {
				double want = copper * PitFlotation.SHARES_PERCENT[k] / 100.0;
				double got = row.cells().get(k).pit().totalKg();
				helper.assertTrue(Math.abs(got - want) < 1e-3, Component.literal("cell " + (k + 1) + " should float " + want + " kg, got " + got + ": " + row.describe()));
			}
			helper.assertTrue(Math.abs(row.floated() - copper * 0.75) < 1e-3, Component.literal("the row floats 75 %, got " + row.floated() / copper));
			helper.assertTrue(Math.abs(pondCopper(row.pond()) - copper * 0.25) < 0.01, Component.literal("the tails carry the rest, got " + pondCopper(row.pond())));
			helper.assertTrue(row.column().getBlockState() != null && row.column().out().held(Product.TAILS_SLURRY) > 0,
					Component.literal("the column gives up water as its tails: " + row.column().status()));
		});
	}

	/** p7_row_short: four cells float 63 % (35 + 15 + 8 + 5) and no more; the filter pays two nuggets of a little over four. */
	private static void rowShort(GameTestHelper helper) {
		double copper = fourNuggets();
		Row row = row(helper, 4, copper, true);
		helper.succeedWhen(() -> {
			helper.assertTrue(Math.abs(row.floated() - copper * 0.63) < 1e-3, Component.literal("four cells float 63 %, got " + row.floated() / copper + ": " + row.describe()));
			helper.assertTrue(nuggets(row.filter()) == 2, Component.literal("the filter should pay 2 nuggets, got " + nuggets(row.filter()) + ": " + row.describe()));
			helper.assertTrue(Math.abs(pondCopper(row.pond()) - copper * 0.37) < 0.01, Component.literal("the tails carry 37 %, got " + pondCopper(row.pond()) / copper));
		});
	}

	/** p7_no_air: with the blower stopped the cells read NO AIR and float nothing; AIR reads 4 mA; started, the row floats. */
	private static void noAir(GameTestHelper helper) {
		Row row = row(helper, 2, fourNuggets(), false);
		helper.startSequence()
				.thenWaitUntil(() -> helper.assertTrue(row.cells().get(0).status().startsWith("NO AIR"), Component.literal("cell 1 reads NO AIR: " + row.describe())))
				.thenExecuteAfter(100, () -> {
					helper.assertTrue(row.floated() == 0, Component.literal("nothing floats without air, got " + row.floated()));
					helper.assertTrue(row.cells().get(0).readMa(6) == 4, Component.literal("AIR reads 4 mA, got " + row.cells().get(0).readMa(6)));
					row.blower().forceRunning();
				})
				.thenWaitUntil(() -> helper.assertTrue(row.floated() > 0, Component.literal("with the blower running it floats: " + row.describe())))
				.thenExecute(() -> helper.assertTrue(row.cells().get(0).readMa(6) == 20, Component.literal("AIR reads 20 mA, got " + row.cells().get(0).readMa(6))))
				.thenSucceed();
	}

	/**
	 * p7_tails: the row's tails (marked as having passed its eight cells) into the 45 m thickener: its underflow at 60 % by pipe over the
	 * rim of Wildcatter's lined cell, where it settles, carrying its copper under its own name; its overflow water back on a water pipe to
	 * a mill; TORQUE on the PLC while it rakes.
	 */
	private static void tails(GameTestHelper helper) {
		int y = 4;
		PlantBlockEntity source = plant(helper, Machine.FLOTATION_BANK, new BlockPos(22, y, -11));
		source.out().offer(Product.TAILS_SLURRY, 2000, SlurryAssay.of(Map.of(PitFlotation.marked(8, COPPER), 0.002)));
		TailingsGameTests.pipe(helper, new BlockPos(22, y, 2));
		PlantBlockEntity thickener = plant(helper, Machine.TAILINGS_THICKENER_LARGE, new BlockPos(0, y, 3));
		thickener.forceRunning();
		// the lined cell south of it, 5 x 5 inside, its tower in the middle
		Block liner = wildcatter("cell_liner");
		for (int x = 19; x <= 25; x++) {
			for (int z = 52; z <= 58; z++) {
				helper.setBlock(new BlockPos(x, y, z), liner);
				if (x == 19 || x == 25 || z == 52 || z == 58) {
					helper.setBlock(new BlockPos(x, y + 1, z), liner);
					helper.setBlock(new BlockPos(x, y + 2, z), liner);
				}
			}
		}
		helper.setBlock(new BlockPos(22, y + 1, 55), wildcatter("decant_tower"));
		TailingsGameTests.pipe(helper, new BlockPos(22, y, 48), new BlockPos(22, y + 1, 48), new BlockPos(22, y + 2, 48), new BlockPos(22, y + 3, 48));
		for (int z = 49; z <= 55; z++) {
			TailingsGameTests.pipe(helper, new BlockPos(22, y + 3, z));
		}
		TailingsGameTests.pipe(helper, new BlockPos(22, y + 2, 55));
		// the overflow water back to a mill east of it
		helper.setBlock(new BlockPos(45, y, 20), wildcatter("pipe"));
		helper.setBlock(new BlockPos(46, y, 20), wildcatter("pipe"));
		PlantBlockEntity mill = plant(helper, Machine.BALL_MILL, new BlockPos(47, y, 18));
		double[] torque = {0};
		boolean[] plain = {true};
		helper.onEachTick(() -> {
			torque[0] = Math.max(torque[0], thickener.readMa(6));
			for (String id : thickener.out().assayOf(Product.TAILS_SLURRY).minerals()) {
				plain[0] &= !id.startsWith("tailings:row");
			}
		});
		helper.succeedWhen(() -> {
			var tower = helper.getBlockEntity(new BlockPos(22, y + 1, 55), dev.wildcatter.block.DecantTowerBlockEntity.class);
			helper.assertTrue(tower.settledKg() > 100, Component.literal("the underflow settles in the lined cell, got " + tower.settledKg() + ": " + tower.status() + " | " + thickener.status()));
			helper.assertTrue(mill.water() > 0 && thickener.waterReturned() > 0, Component.literal("its overflow water goes back to the mill: " + thickener.status()));
			helper.assertTrue(plain[0], Component.literal("the underflow carries the copper under its own name"));
			helper.assertTrue(torque[0] > 4 + 16 * 0.2 - 0.01, Component.literal("TORQUE reads while it rakes, got " + torque[0] + " mA"));
		});
	}

	/** p7c_parts: the five parts are their spec sizes; the cells have AIR on the PLC, the thickener TORQUE; the blower's box is medium voltage. */
	private static void parts(GameTestHelper helper) {
		Machine[] machines = {Machine.CONDITIONING_TANK_LARGE, Machine.TANK_CELL, Machine.COLUMN_CELL, Machine.FLOTATION_BLOWER_LARGE, Machine.TAILINGS_THICKENER_LARGE};
		int x = 0;
		for (Machine machine : machines) {
			BlockPos pos = new BlockPos(x, 2, 0);
			PlantBlockEntity part = plant(helper, machine, pos);
			int cells = 0;
			BlockState state = helper.getLevel().getBlockState(helper.absolutePos(pos));
			for (BlockPos cell : BigParts.cells(helper.absolutePos(pos), state)) {
				if (helper.getLevel().getBlockState(cell).getBlock() == TailingsBlocks.plantBlock(machine)) {
					cells++;
				}
			}
			helper.assertTrue(cells == machine.cells(), Component.literal(machine + " should fill " + machine.cells() + " cells, got " + cells));
			String point = machine == Machine.TANK_CELL || machine == Machine.COLUMN_CELL ? "AIR" : machine == Machine.TAILINGS_THICKENER_LARGE ? "TORQUE" : null;
			if (point != null) {
				helper.assertTrue(part.points().stream().anyMatch(p -> p.name().startsWith(point)), Component.literal(machine + " has " + point + " on the PLC"));
			}
			var voltage = machine.supply() == Machine.Supply.MEDIUM ? dev.wildcatter.energy.Voltage.MEDIUM : dev.wildcatter.energy.Voltage.INDUSTRIAL;
			Direction box = dev.wildcatter.tailings.block.TailingsPartBlock.boxFace(state);
			helper.assertTrue(part.hasPortAt(helper.absolutePos(pos), box, dev.wildcatter.circuit.Conductor.A, voltage), Component.literal(machine + ": its motor box at its main cell"));
			x += machine.width() + 1;
		}
		helper.succeed();
	}
}
