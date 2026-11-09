package dev.wildcatter.tailings.pit;

import net.minecraft.util.StringRepresentable;

/** What a survey stake marks on a haul road (docs/multiblocks/open-pit.md, Haul roads). */
public enum StopKind implements StringRepresentable {
	WAYPOINT("waypoint", "waypoint"),
	LOAD("load", "LOAD"),
	DUMP_POCKET("dump_pocket", "DUMP POCKET"),
	LOADOUT("loadout", "LOADOUT"),
	DUMP_POINT("dump_point", "DUMP POINT"),
	FUEL("fuel", "FUEL"),
	PARK("park", "PARK");

	private final String id;
	private final String label;

	StopKind(String id, String label) {
		this.id = id;
		this.label = label;
	}

	@Override
	public String getSerializedName() {
		return id;
	}

	public String label() {
		return label;
	}

	public StopKind next() {
		return values()[(ordinal() + 1) % values().length];
	}

	/** A truck fills here: a loader loads it, or (P4) the load-out. */
	public boolean fills() {
		return this == LOAD || this == LOADOUT;
	}

	/** A truck tips here. */
	public boolean tips() {
		return this == DUMP_POCKET || this == DUMP_POINT;
	}
}
