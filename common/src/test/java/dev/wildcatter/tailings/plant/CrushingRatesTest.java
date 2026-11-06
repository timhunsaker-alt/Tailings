package dev.wildcatter.tailings.plant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.wildcatter.fluid.SlurryAssay;
import dev.wildcatter.tailings.rock.Mineral;
import dev.wildcatter.tailings.rock.Rock;
import dev.wildcatter.tailings.rock.SizeClass;
import org.junit.jupiter.api.Test;

/** The crushing circuit's plain Java: rock loads, the rates and the assays (docs/multiblocks/crushing-circuit.md). */
class CrushingRatesTest {
	@Test
	void fiftyTonnesAnHourIsAboutFourteenKgASecond() {
		assertEquals(13.89, PlantRates.KG_PER_SECOND, 0.01);
		// a block of rock passes in about 3 minutes 15 seconds
		assertEquals(194, PlantRates.BLOCK_KG / PlantRates.KG_PER_SECOND, 1);
	}

	@Test
	void bondsLawGivesTheMillItsShare() {
		double kwh = PlantRates.bondKwhPerTonne(15, 150, 3000);
		assertEquals(9.5, kwh, 0.1);
		assertTrue(kwh * PlantRates.TONNES_PER_HOUR < Machine.BALL_MILL.watts() / 1000.0, "the 600 kW mill covers it");
	}

	@Test
	void aDriveForEachThirtyMetresRoundedUp() {
		assertEquals(1, PlantRates.drivesFor(1));
		assertEquals(1, PlantRates.drivesFor(30));
		assertEquals(2, PlantRates.drivesFor(31));
		assertEquals(4, PlantRates.drivesFor(120));
	}

	@Test
	void slurryWaterFollowsItsSolids() {
		assertEquals(0.43, PlantRates.waterPerKg(PlantRates.MILL_SOLIDS), 0.01);
		assertEquals(1.86, PlantRates.waterPerKg(PlantRates.OVERFLOW_SOLIDS), 0.01);
	}

	@Test
	void aRockLoadTakesInProportionAndSaves() {
		RockLoad load = new RockLoad();
		load.add(Rock.GRANITE, null, 300);
		load.add(Rock.COPPER, SizeClass.CRUSHED, 100);
		RockLoad taken = load.take(200);
		assertEquals(150, taken.kg(Rock.GRANITE, null), 1e-9);
		assertEquals(50, taken.kg(Rock.COPPER, SizeClass.CRUSHED), 1e-9);
		assertEquals(200, load.total(), 1e-9);
		assertEquals(Rock.GRANITE, load.main());

		RockLoad onlySand = new RockLoad();
		onlySand.add(Rock.GRANITE, SizeClass.SAND, 10);
		onlySand.add(Rock.GRANITE, SizeClass.GRAVEL, 10);
		RockLoad sand = onlySand.take(100, (rock, size) -> size == SizeClass.SAND);
		assertEquals(10, sand.total(), 1e-9);
		assertEquals(10, onlySand.kg(Rock.GRANITE, SizeClass.GRAVEL), 1e-9);

		RockLoad back = new RockLoad();
		back.load(load.save());
		assertEquals(load.kg(Rock.GRANITE, null), back.kg(Rock.GRANITE, null), 1e-9);
		assertEquals(load.kg(Rock.COPPER, SizeClass.CRUSHED), back.kg(Rock.COPPER, SizeClass.CRUSHED), 1e-9);
		back.load("NOT_A_ROCK:0=5,GRANITE:x=1");
		assertTrue(back.isEmpty(), "what does not read is left out");
		assertNull(back.main());
	}

	@Test
	void aRocksAssayIsTheMineralItHolds() {
		SlurryAssay copper = Assays.dry(Rock.COPPER);
		assertEquals(Rock.COPPER.contained(Mineral.COPPER).doubleValue() / PlantRates.BLOCK_KG, copper.get("minecraft:copper_ingot"), 1e-12);
		SlurryAssay granite = Assays.dry(Rock.GRANITE);
		assertEquals(8 / PlantRates.BLOCK_KG, granite.get(Mineral.ZIRCON.mineralItemId()), 1e-12);
		assertEquals(Mineral.ZIRCON, Assays.mineral(Mineral.ZIRCON.mineralItemId()));
	}

	@Test
	void mixedRockBlendsByMassAndWaterDilutes() {
		RockLoad load = new RockLoad();
		load.add(Rock.COPPER, SizeClass.SAND, 100);
		load.add(Rock.COBBLESTONE, SizeClass.SAND, 300);
		SlurryAssay dry = Assays.dry(load);
		assertEquals(Assays.dry(Rock.COPPER).get("minecraft:copper_ingot") / 4, dry.get("minecraft:copper_ingot"), 1e-12);
		SlurryAssay wet = Assays.wet(dry, PlantRates.OVERFLOW_SOLIDS);
		assertEquals(dry.get("minecraft:copper_ingot") * 0.35, wet.get("minecraft:copper_ingot"), 1e-12);
	}
}
