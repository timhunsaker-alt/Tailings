package dev.wildcatter.tailings.quest;

import dev.wildcatter.quest.QuestEvents;
import dev.wildcatter.tailings.Tailings;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.Level;

/**
 * The quest milestones Tailings' machines fire for Wildcatter's quest book (Wildcatter docs/quests.md; the chapter is
 * {@code data/tailings/wildcatter_quests/open_pit.json}). Each is fired from the place that already knows the machine is working, every
 * tick it works: Wildcatter counts a position and key at most once in 5 s and awards every player within 64 blocks.
 */
public final class Milestones {
	/** A jaw or cone crusher crushing rock. */
	public static final Identifier CRUSHER_RUNNING = Tailings.id("crusher_running");
	/** A flotation bank or a tank cell floating slurry. */
	public static final Identifier FLOTATION_RUNNING = Tailings.id("flotation_running");
	/** A blasting machine's shot fired and done. */
	public static final Identifier BLAST_FIRED = Tailings.id("blast_fired");
	/** The walking dragline working its cycle or walking, on power. */
	public static final Identifier DRAGLINE_RUNNING = Tailings.id("dragline_running");
	/** The gyratory crushing bank blocks from its dump pocket into the surge pocket. */
	public static final Identifier GYRATORY_RUNNING = Tailings.id("gyratory_running");
	/** The rope shovel hoisting its dipper through the face, on power. */
	public static final Identifier SHOVEL_RUNNING = Tailings.id("shovel_running");
	/** The leach tank leaching tails with sulfuric acid. */
	public static final Identifier LEACH_RUNNING = Tailings.id("leach_running");
	/** A pit switch house with a trailing cable live: its outlet closed, the house fed, the cable's far end at its volts. */
	public static final Identifier PIT_ENERGISED = Tailings.id("pit_energised");
	/** The stamp mill crushing a piece of ore or rock (2026-11-19, wave A). */
	public static final Identifier STAMP_MILL_RUNNING = Tailings.id("stamp_mill_running");
	/** A dry mill separator separating heavy mineral concentrate (2026-11-19, wave A). */
	public static final Identifier DRY_MILL_RUNNING = Tailings.id("dry_mill_running");
	/** The DMS cyclone module separating gravel against its ferrosilicon medium (2026-11-19, wave A). */
	public static final Identifier DMS_RUNNING = Tailings.id("dms_running");

	private Milestones() {
	}

	/** Fires {@code key} at {@code at}; nothing on a client level. */
	public static void fire(Level level, BlockPos at, Identifier key) {
		QuestEvents.milestone(level, at, key);
	}
}
