package dev.wildcatter.tailings.pit;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * The haul dispatch office's numbers (docs/multiblocks/open-pit.md, P2b): how far its radio reaches, its routes, and the ranges of
 * its 4-20 mA points. Plain Java, for the unit tests.
 */
public final class Dispatch {
	/** It reaches trucks within 2 km by radio (spec). */
	public static final double RADIO_M = 2_000;
	/** Up to 8 routes (spec). */
	public static final int ROUTES = 8;
	/** Each route's points: RUN, RUNNING, TRUCKS, LOADS, TONNES. */
	public static final int POINTS_PER_ROUTE = 5;
	public static final int RUN = 0, RUNNING = 1, TRUCKS = 2, LOADS = 3, TONNES = 4;
	/** The fleet's points, after the routes'. */
	public static final int FUEL_LOW = ROUTES * POINTS_PER_ROUTE, BLOCKED = FUEL_LOW + 1, AUTO = FUEL_LOW + 2;
	/** TRUCKS spans 0 to 16 trucks: 4 mA and 1 mA a truck (proposed). */
	public static final int TRUCKS_SPAN = 16;
	/** TONNES spans 0 to 2,000 t an hour (proposed: about twenty 91 t loads an hour). */
	public static final double TONNES_SPAN = 2_000;
	/** TONNES counts what was tipped in the last hour, 72,000 ticks. */
	public static final long HOUR_TICKS = 72_000;
	/** A truck's report counts this long: it reports once a second. */
	public static final long REPORT_FRESH_TICKS = 40;
	/** The office's own load on the low tier: radio, computers, lights and air conditioning (proposed). */
	public static final double OFFICE_WATTS = 5_000;
	/** Below this share of the tier's nominal the office's radio and computers drop out. */
	public static final double DROPOUT = 0.85;

	private Dispatch() {
	}

	/** The loop current for a number of trucks, 4 to 20 mA over 0 to 16, held at 20. */
	public static double trucksMa(int trucks) {
		return 4 + 16 * Math.min(1.0, Math.max(0, trucks) / (double) TRUCKS_SPAN);
	}

	/** The loop current for tonnes tipped in the last hour, 4 to 20 mA over 0 to 2,000 t, held at 20. */
	public static double tonnesMa(double tonnes) {
		return 4 + 16 * Math.min(1.0, Math.max(0, tonnes) / TONNES_SPAN);
	}

	/** Which route a point belongs to, or -1 for the fleet's points. */
	public static int routeOf(int point) {
		return point >= 0 && point < FUEL_LOW ? point / POINTS_PER_ROUTE : -1;
	}

	/** What a route's point is: RUN, RUNNING, TRUCKS, LOADS or TONNES. */
	public static int kindOf(int point) {
		return point % POINTS_PER_ROUTE;
	}

	/** The loads tipped on one route over the last hour, kept by game time. */
	public static final class Hour {
		private final Deque<long[]> loads = new ArrayDeque<>();

		public void add(long gameTime, long kg) {
			loads.addLast(new long[] {gameTime, kg});
		}

		/** Tonnes tipped in the hour before {@code now}. */
		public double tonnes(long now) {
			while (!loads.isEmpty() && now - loads.peekFirst()[0] >= HOUR_TICKS) {
				loads.removeFirst();
			}
			long kg = 0;
			for (long[] load : loads) {
				kg += load[1];
			}
			return kg / 1000.0;
		}

		public void clear() {
			loads.clear();
		}

		/** "time:kg,time:kg". */
		public String save() {
			StringBuilder out = new StringBuilder();
			for (long[] load : loads) {
				if (!out.isEmpty()) {
					out.append(',');
				}
				out.append(load[0]).append(':').append(load[1]);
			}
			return out.toString();
		}

		public void load(String saved) {
			loads.clear();
			if (saved == null || saved.isEmpty()) {
				return;
			}
			for (String part : saved.split(",")) {
				String[] pair = part.split(":");
				if (pair.length == 2) {
					try {
						add(Long.parseLong(pair[0]), Long.parseLong(pair[1]));
					} catch (NumberFormatException ignored) {
						// a bad entry is dropped
					}
				}
			}
		}
	}
}
