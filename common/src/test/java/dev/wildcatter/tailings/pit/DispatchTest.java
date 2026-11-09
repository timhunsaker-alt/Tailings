package dev.wildcatter.tailings.pit;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/** The dispatch office's points and ranges (docs/multiblocks/open-pit.md, P2b). */
class DispatchTest {
	@Test
	void trucksAreAMilliampEach() {
		assertEquals(4, Dispatch.trucksMa(0), 1e-9);
		assertEquals(7, Dispatch.trucksMa(3), 1e-9);
		assertEquals(20, Dispatch.trucksMa(16), 1e-9);
		assertEquals(20, Dispatch.trucksMa(30), 1e-9);
	}

	@Test
	void tonnesSpanTwoThousandAnHour() {
		assertEquals(4, Dispatch.tonnesMa(0), 1e-9);
		assertEquals(12, Dispatch.tonnesMa(1_000), 1e-9);
		assertEquals(20, Dispatch.tonnesMa(5_000), 1e-9);
	}

	@Test
	void pointsAreFivePerRouteThenTheFleets() {
		assertEquals(43, Dispatch.AUTO + 1);
		assertEquals(0, Dispatch.routeOf(0));
		assertEquals(7, Dispatch.routeOf(39));
		assertEquals(-1, Dispatch.routeOf(Dispatch.FUEL_LOW));
		assertEquals(Dispatch.LOADS, Dispatch.kindOf(5 * 3 + 3));
	}

	@Test
	void theHourForgetsOldLoads() {
		Dispatch.Hour hour = new Dispatch.Hour();
		hour.add(0, 91_000);
		hour.add(36_000, 91_000);
		assertEquals(182, hour.tonnes(40_000), 1e-9);
		assertEquals(91, hour.tonnes(72_000), 1e-9);
		Dispatch.Hour back = new Dispatch.Hour();
		back.load(hour.save());
		assertEquals(91, back.tonnes(72_001), 1e-9);
	}
}
