package dev.wildcatter.tailings.plant;

/**
 * How fast the crushers' and apron feeders' moving parts move while their motors run (drawn by the client's PlantRenderer, in the
 * part's own frame: facing north, x east, y up, z south, metres from the main cell's north-west bottom corner, as tools/models drew
 * them before leaving them out of the still models).
 * <ul>
 * <li>jaw crusher (C100 class): the eccentric shaft and flywheels at 260 rpm, its real speed;</li>
 * <li>gyratory crusher (42-65 class): the eccentric at 150 rpm, its real speed (its pinion turns 600 rpm on a 4:1 bevel set);</li>
 * <li>apron feeders: the pans at 0.1 m/s;</li>
 * <li>the SAG mill (9.75 m) and the large ball mill (7.3 m): their shells at 75 % of critical speed, 42.3 / sqrt(D) rpm, as
 * sag-mill-line.md runs them (10.2 and 11.7 rpm);</li>
 * <li>the tank and column cells: their froth crowding out from the middle to the launder at the lip, about 0.1 m/s (proposed);</li>
 * <li>the X-ray sorter: its 1.2 m belt at 3 m/s (xrt-ore-sorter.md), the pieces on it read and sent off its head.</li>
 * </ul>
 */
public final class PlantMotion {
	private PlantMotion() {
	}

	public static final float JAW_RPM = 260;
	public static final float GYRATORY_RPM = 150;
	/** An apron feeder's pan speed, m/s. */
	public static final double APRON_SPEED = 0.1;
	/** Pans per turn of an apron feeder's head sprocket (its teeth): one turn moves the apron this many pan pitches. */
	public static final int SPROCKET_TEETH = 6;

	/** A mill's shell at 75 % of its critical speed (sag-mill-line.md), rpm, for its inside diameter in metres. */
	public static float millRpm(double diameter) {
		return (float) (0.75 * 42.3 / Math.sqrt(diameter));
	}

	public static final float SAG_RPM = millRpm(9.75);
	public static final float BALL_MILL_LARGE_RPM = millRpm(7.3);
	/** The froth's drift, a cycle a turn: two cycles a minute, centre to lip in 30 s (proposed: about 0.1 m/s in a 6.5 m cell). */
	public static final float FROTH_RPM = 2;
	/** The X-ray sorter's belt speed, m/s (xrt-ore-sorter.md). */
	public static final double XRT_BELT_SPEED = 3;
	/** The sorter's drawn pieces repeat every this many metres of belt: a turn of its "shaft" is this much belt. */
	public static final float XRT_LOOP_METRES = 12;
	public static final float XRT_RPM = (float) (XRT_BELT_SPEED * 60 / XRT_LOOP_METRES);

	/** One apron: its pans across from x0 to x1, y0 to y1 high, {@code count} pans of {@code length} every {@code pitch} from z0. */
	public record Apron(float x0, float x1, float y0, float y1, float z0, float pitch, float length, int count, String tex) {
		public float end() {
			return z0 + pitch * count;
		}

		/** The head sprocket's rpm for the pans' speed. */
		public float rpm() {
			return (float) (APRON_SPEED * 60 / (SPROCKET_TEETH * pitch));
		}
	}

	public static final Apron PRIMARY_APRON = new Apron(0.4f, 2.6f, 1.2f, 1.35f, 0.3f, 0.5f, 0.46f, 23, "steel");
	public static final Apron RECLAIM_APRON = new Apron(0.3f, 1.7f, 1.1f, 1.22f, 0.2f, 0.5f, 0.46f, 11, "steel");
	public static final Apron DUMP_APRON = new Apron(0.3f, 1.7f, 0.45f, 0.55f, 0.2f, 0.22f, 0.24f, 16, "iron");

	/**
	 * The vent fan's and the booster's impellers, rpm (proposed: a direct-drive axial fan on a four-pole motor; films-hi07). The hoist's
	 * drums and the head sheaves are not here: they turn with the rope, as far as the skips move (PlantRenderer).
	 */
	public static final float FAN_RPM = 1480;

	/** The part's real shaft rpm while it runs, or 0 for a part with no moving parts drawn. */
	public static float rpm(Machine machine) {
		return switch (machine) {
			case JAW -> JAW_RPM;
			case GYRATORY_CRUSHER -> GYRATORY_RPM;
			case PRIMARY_APRON_FEEDER -> PRIMARY_APRON.rpm();
			case RECLAIM_FEEDER -> RECLAIM_APRON.rpm();
			case APRON_FEEDER -> DUMP_APRON.rpm();
			case SAG_MILL -> SAG_RPM;
			case BALL_MILL_LARGE -> BALL_MILL_LARGE_RPM;
			case TANK_CELL, COLUMN_CELL, FLOTATION_BANK, SCAVENGER_BANK, CLEANER_BANK -> FROTH_RPM;
			case XRT_SORTER -> XRT_RPM;
			case VENT_FAN, BOOSTER_FAN -> FAN_RPM;
			default -> 0;
		};
	}
}
