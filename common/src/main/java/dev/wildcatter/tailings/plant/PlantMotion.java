package dev.wildcatter.tailings.plant;

/**
 * How fast the crushers' and apron feeders' moving parts move while their motors run (drawn by the client's PlantRenderer, in the
 * part's own frame: facing north, x east, y up, z south, metres from the main cell's north-west bottom corner, as tools/models drew
 * them before leaving them out of the still models).
 * <ul>
 * <li>jaw crusher (C100 class): the eccentric shaft and flywheels at 260 rpm, its real speed;</li>
 * <li>gyratory crusher (42-65 class): the eccentric at 150 rpm, its real speed (its pinion turns 600 rpm on a 4:1 bevel set);</li>
 * <li>apron feeders: the pans at 0.1 m/s.</li>
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

	/** The part's real shaft rpm while it runs, or 0 for a part with no moving parts drawn. */
	public static float rpm(Machine machine) {
		return switch (machine) {
			case JAW -> JAW_RPM;
			case GYRATORY_CRUSHER -> GYRATORY_RPM;
			case PRIMARY_APRON_FEEDER -> PRIMARY_APRON.rpm();
			case RECLAIM_FEEDER -> RECLAIM_APRON.rpm();
			case APRON_FEEDER -> DUMP_APRON.rpm();
			default -> 0;
		};
	}
}
