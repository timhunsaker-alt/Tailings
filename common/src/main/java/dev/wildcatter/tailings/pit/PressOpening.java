package dev.wildcatter.tailings.pit;

/**
 * How the tailings filter opens and drops its cake, as the client draws it ({@code PlantRenderer.press}): plain Java, unit-tested
 * (docs/multiblocks/open-pit-p4-dry-stack.md, "The press's opening drawn"). Metres in the part's own frame (facing north: x east,
 * y up, z south from the main cell's north-west bottom corner); seconds into the press's 15-minute cycle ({@link DryStack}).
 * <p>
 * Closed through filling, squeezing and blowing. When blowing ends the tail draws back by one {@link #GAP} over 20 s; then the plate
 * shifter pulls the plates one at a time from the tail end to the head, each moving a {@link #GAP} toward the tail in 2.5 s, so an
 * open gap travels down the pack. As each chamber opens its cake stands a second on the cloth, then comes away in four slabs, the
 * lower two first, which tumble and fall the full height under the pack, through the open frame, onto the cake chute's pans; the
 * last, by the head, lands just before the cycle ends, when the game counts the whole cake into the bin. The ram closes the pack
 * over the next cycle's first 20 s.
 */
public final class PressOpening {
	/** The plates: 78 of them, the first's head-side face, their pitch and thickness (the chambers between them 5 cm), along z. */
	public static final int PLATES = 78;
	public static final float PLATE_Z0 = 2.1f, PLATE_PITCH = 0.25f, PLATE_T = 0.2f;
	/** The plates' bottom and top edges (1.7 m of plate under the side beams), and the cake's across x and up y in each chamber. */
	public static final float PLATE_BOTTOM = 4.1f, PLATE_TOP = 5.8f;
	public static final float CAKE_X0 = 1.95f, CAKE_X1 = 4.05f, CAKE_Y0 = 4.15f, CAKE_Y1 = 5.75f;
	/**
	 * The gap the shifter opens between plates, and so the tail's draw-back (fix3-hi04: 0.8 m, it was 0.5): a real side-bar or
	 * overhead-beam shifter opens several hundred millimetres at each plate, room for a large cake to drop clear.
	 */
	public static final float GAP = 0.8f;
	/** The moving tail's closed head-side face and its thickness (0.6 m, so it opens the full gap short of the cylinder's stand). */
	public static final float TAIL_Z = 21.8f, TAIL_T = 0.6f;
	/** The ram cylinder's end stand's head-side face (tools/models/pit.py). */
	public static final float STAND_Z = 23.2f;

	/** Seconds: opening starts when blowing ends; the tail draws back over 20 s; each plate's shift takes 2.5 s. */
	public static final float OPEN_AT = DryStack.FILL_SECONDS + DryStack.SQUEEZE_SECONDS + DryStack.BLOW_SECONDS;
	public static final float TAIL_MOVE = 20f, SHIFT_MOVE = 2.5f;
	/** A cake stands in its opened chamber this long before it comes away. */
	public static final float CAKE_HANG = 1.0f;
	/** The four slabs of a chamber's cake (0, 1 the lower two; 2, 3 the upper; even on the west half): when each comes away after the hang. */
	public static final float[] SLAB_DELAY = {0f, 0.12f, 0.3f, 0.42f};
	/** The cake's slabs drawn 0.12 m thick (a real cake is about 40-50 mm: drawn thicker so a slab seen edge-on still reads). */
	public static final float SLAB_T = 0.12f;
	/** Gravity, m/s². */
	public static final float G = 9.8f;

	/** The cake chute under the pack: its pans' tops (stepping down from each end to the chute over the bin) and its side skirts' height. */
	public static final float PAN_END = 2.0f, PAN_2 = 1.6f, PAN_3 = 1.2f, PAN_CHUTE = 0.6f, SKIRT = 0.5f;

	/** The last plate (by the head) starts its shift so its cake's last slab lands 0.1 s before the cycle ends. */
	public static final float LAST_SHIFT = DryStack.CYCLE_SECONDS - CAKE_HANG - SLAB_DELAY[3] - fallSeconds(CAKE_Y0 + (CAKE_Y1 - CAKE_Y0) / 2, panAt(PLATE_Z0)) - 0.1f;
	/** A plate is shifted about every 5.6 s. */
	public static final float SHIFT_EVERY = (LAST_SHIFT - OPEN_AT - TAIL_MOVE) / (PLATES - 1);

	private PressOpening() {
	}

	/** Where the cake chute's pan top is under {@code z} (pit.py: stepping down from each end to the chute over the bin). */
	public static float panAt(float z) {
		if (z < 5.0f || z >= 19.5f) {
			return PAN_END;
		}
		if (z < 7.5f || z >= 16.5f) {
			return PAN_2;
		}
		if (z < 9.6f || z >= 14.4f) {
			return PAN_3;
		}
		return PAN_CHUTE;
	}

	/** Seconds to fall from {@code from} to {@code to}, m. */
	public static float fallSeconds(float from, float to) {
		return (float) Math.sqrt(2 * Math.max(0, from - to) / G);
	}

	/** An ease from 0 to 1. */
	public static float smooth(float t) {
		t = Math.max(0, Math.min(1, t));
		return t * t * (3 - 2 * t);
	}

	/** When plate {@code k} (0 at the head) starts its shift, s into the cycle: from the tail end to the head. */
	public static float shiftAt(int k) {
		return OPEN_AT + TAIL_MOVE + (PLATES - 1 - k) * SHIFT_EVERY;
	}

	/** When chamber {@code k} opens (0 by the head, between it and plate 0; {@link #PLATES} by the tail, opened as the tail draws back). */
	public static float openedAt(int k) {
		return k == PLATES ? OPEN_AT : shiftAt(k);
	}

	/** The middle of chamber {@code k} along z, closed. */
	public static float chamberZ(int k) {
		return k == 0 ? PLATE_Z0 - 0.025f : k == PLATES ? PLATE_Z0 + (PLATES - 1) * PLATE_PITCH + PLATE_T + 0.05f : PLATE_Z0 + k * PLATE_PITCH - 0.025f;
	}

	/** How far the tail has drawn back at {@code s} s into the cycle (-1: not known); {@code pressed}: it has opened before. */
	public static float tail(float s, boolean pressed) {
		if (s >= OPEN_AT) {
			return GAP * smooth((s - OPEN_AT) / TAIL_MOVE);
		}
		if (s >= 0 && s < TAIL_MOVE && pressed) {
			return GAP * (1 - smooth(s / TAIL_MOVE));   // closing: the ram pushes the tail and the shifted plates back up to the head
		}
		return 0;
	}

	/** How far plate {@code k} has been shifted toward the tail at {@code s}. */
	public static float plate(int k, float s, boolean pressed) {
		if (s >= OPEN_AT) {
			return GAP * smooth((s - shiftAt(k)) / SHIFT_MOVE);
		}
		return tail(s, pressed);
	}

	/** Slab {@code j}'s bottom edge before it falls. */
	public static float slabBottom(int j) {
		return j < 2 ? CAKE_Y0 : CAKE_Y0 + (CAKE_Y1 - CAKE_Y0) / 2;
	}

	/** Seconds after chamber {@code k} opens that slab {@code j} lands on the pan under it. */
	public static float landsAfter(int k, int j) {
		return CAKE_HANG + SLAB_DELAY[j] + fallSeconds(slabBottom(j), panAt(chamberZ(k)));
	}

	/** Seconds after chamber {@code k} opens that its last slab lands. */
	public static float allLandedAfter(int k) {
		float most = 0;
		for (int j = 0; j < SLAB_DELAY.length; j++) {
			most = Math.max(most, landsAfter(k, j));
		}
		return most;
	}
}
