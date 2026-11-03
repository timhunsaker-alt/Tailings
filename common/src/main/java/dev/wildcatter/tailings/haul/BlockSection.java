package dev.wildcatter.tailings.haul;

import java.util.HashMap;
import java.util.Map;

/** One train per block section. The same train may re-enter the section it already holds. */
public final class BlockSection {
	private final Map<String, String> occupant = new HashMap<>();

	public boolean enter(String section, String train) {
		if (section == null || section.isEmpty() || train == null) {
			return true;
		}
		String who = occupant.get(section);
		if (who == null || who.equals(train)) {
			occupant.put(section, train);
			return true;
		}
		return false;
	}

	public void leave(String section, String train) {
		if (train != null && train.equals(occupant.get(section))) {
			occupant.remove(section);
		}
	}

	public String occupant(String section) {
		return occupant.get(section);
	}
}
