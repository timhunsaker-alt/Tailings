package dev.wildcatter.tailings.plant;

import java.util.ArrayList;
import java.util.List;

/**
 * Where each part's main cell sits, facing north, in metres from the west edge. The layout lists main cells only; each
 * part's footprint fills its own box east and south of that cell. The jaw and the conditioning tank are the controllers.
 */
public final class PlantPlan {
	public record Station(char mark, Machine machine, int x) {
	}

	/** Grizzly, jaw, 6 m belt, screen, cone, ball mill, cyclones, in that order, boxes touching but not overlapping. */
	public static final List<Station> CRUSHING = line(
			new Station('G', Machine.GRIZZLY, 0),
			new Station('J', Machine.JAW, 0),
			new Station('B', Machine.BELT, 0),
			new Station('S', Machine.SCREEN, 0),
			new Station('C', Machine.CONE, 0),
			new Station('M', Machine.BALL_MILL, 0),
			new Station('Y', Machine.CYCLONE, 0));

	/**
	 * Conditioning tank, four roughers, two scavengers, two cleaners, the blower, the thickener and the filter.
	 * Cell count is fixed: recovery does not grow if you add cells past this bank.
	 */
	public static final List<Station> FLOTATION = line(
			new Station('K', Machine.CONDITIONER, 0),
			new Station('R', Machine.CELL, 0),
			new Station('R', Machine.CELL, 0),
			new Station('R', Machine.CELL, 0),
			new Station('R', Machine.CELL, 0),
			new Station('V', Machine.CELL, 0),
			new Station('V', Machine.CELL, 0),
			new Station('L', Machine.CELL, 0),
			new Station('L', Machine.CELL, 0),
			new Station('A', Machine.BLOWER, 0),
			new Station('T', Machine.THICKENER, 0),
			new Station('F', Machine.FILTER, 0));

	public static final char CRUSHING_CONTROLLER = 'J';
	public static final char FLOTATION_CONTROLLER = 'K';

	/** One block of feed per second while the motors run. Real tonnes per hour are slower; the clock is compressed. */
	public static final int CYCLE_TICKS = 20;

	/** Fines are 90% liberated. The game still pays the even 75% ladder, not 90% times the real flotation recovery. */
	public static final double FINES_LIBERATION = 0.90;
	public static final double SAND_LIBERATION = 0.60;

	private PlantPlan() {
	}

	public static List<Station> line(Station... unordered) {
		List<Station> placed = new ArrayList<>();
		int x = 0;
		for (Station station : unordered) {
			placed.add(new Station(station.mark, station.machine, x));
			x += station.machine.width();
		}
		return List.copyOf(placed);
	}

	public static String row(List<Station> stations) {
		int width = 0;
		for (Station station : stations) {
			width = Math.max(width, station.x + station.machine.width());
		}
		char[] chars = new char[width];
		java.util.Arrays.fill(chars, '.');
		for (Station station : stations) {
			chars[station.x] = station.mark;
		}
		return new String(chars);
	}

	public static boolean overlaps(List<Station> stations) {
		for (int i = 0; i < stations.size(); i++) {
			for (int j = i + 1; j < stations.size(); j++) {
				Station a = stations.get(i);
				Station b = stations.get(j);
				boolean x = a.x < b.x + b.machine.width() && b.x < a.x + a.machine.width();
				boolean z = a.machine.depth() > 0 && b.machine.depth() > 0;
				if (x && z && a.machine.height() > 0 && b.machine.height() > 0) {
					return true;
				}
			}
		}
		return false;
	}

	public static int count(List<Station> stations, char mark) {
		int n = 0;
		for (Station station : stations) {
			if (station.mark == mark) {
				n++;
			}
		}
		return n;
	}
}
