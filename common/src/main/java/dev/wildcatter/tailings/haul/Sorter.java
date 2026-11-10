package dev.wildcatter.tailings.haul;

import java.util.Arrays;

/**
 * The ore sorter's screen (docs/multiblocks/dump-station.md): each of its four sides (0 north, 1 east, 2 south, 3 west) set to one
 * rock or ore type by item id, and one side set as waste. Each type goes out of the side set for it; a type with no side goes to
 * waste. Plain Java.
 */
public final class Sorter {
	public static final int SIDES = 4;
	private final String[] product = new String[SIDES];
	private int waste = -1;
	/** A diverter gate shut on a side, by a PLC: what would leave by it goes to waste. Not saved: it must keep coming. */
	private final boolean[] gateShut = new boolean[SIDES];

	/** What a PLC chose for each side, while it chooses: an item id, or null for nothing of its own. Not saved: it must keep coming. */
	private final String[] chosen = new String[SIDES];
	private final boolean[] choosing = new boolean[SIDES];

	public void setGate(int side, boolean shut) {
		gateShut[side] = shut;
	}

	/** A PLC sets {@code side} to take {@code itemId} (null: nothing of its own), over what its screen set, while it keeps saying so. */
	public void choose(int side, String itemId) {
		choosing[side] = true;
		chosen[side] = itemId;
	}

	/** The side goes back to what its screen set. */
	public void release(int side) {
		choosing[side] = false;
		chosen[side] = null;
	}

	public boolean chosen(int side) {
		return choosing[side];
	}

	/** What {@code side} takes now: the PLC's choice while it chooses, else the screen's. */
	public String takes(int side) {
		return choosing[side] ? chosen[side] : product[side];
	}

	/** Sets {@code side} to take {@code itemId}; the side stops being waste, and no other side keeps that type. */
	public void set(int side, String itemId) {
		for (int i = 0; i < SIDES; i++) {
			if (itemId.equals(product[i])) {
				product[i] = null;
			}
		}
		product[side] = itemId;
		if (waste == side) {
			waste = -1;
		}
	}

	/** Clears {@code side}: it takes no one type and is not the waste side. */
	public void clear(int side) {
		product[side] = null;
		if (waste == side) {
			waste = -1;
		}
	}

	/** Sets {@code side} as the waste side; it takes no one type any more. */
	public void setWaste(int side) {
		product[side] = null;
		waste = side;
	}

	public String product(int side) {
		return product[side];
	}

	public int waste() {
		return waste;
	}

	/** The side {@code itemId} leaves by, or -1 when it has none and there is no waste side. Two sides choosing one type: the first takes it. */
	public int route(String itemId) {
		for (int i = 0; i < SIDES; i++) {
			if (itemId.equals(takes(i))) {
				return gateShut[i] ? waste : i;
			}
		}
		return waste;
	}

	/** Saved as {@code north|east|south|west|waste}, empty for an unset side. */
	public String save() {
		StringBuilder out = new StringBuilder();
		for (int i = 0; i < SIDES; i++) {
			out.append(product[i] == null ? "" : product[i]).append('|');
		}
		return out.append(waste).toString();
	}

	public void load(String text) {
		Arrays.fill(product, null);
		waste = -1;
		if (text == null || text.isBlank()) {
			return;
		}
		String[] part = text.split("\\|", -1);
		for (int i = 0; i < SIDES && i < part.length; i++) {
			product[i] = part[i].isEmpty() ? null : part[i];
		}
		try {
			waste = part.length > SIDES ? Integer.parseInt(part[SIDES]) : -1;
		} catch (NumberFormatException ex) {
			waste = -1;
		}
		if (waste < -1 || waste >= SIDES) {
			waste = -1;
		}
	}
}
