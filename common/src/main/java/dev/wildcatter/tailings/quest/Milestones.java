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

	private Milestones() {
	}

	/** Fires {@code key} at {@code at}; nothing on a client level. */
	public static void fire(Level level, BlockPos at, Identifier key) {
		QuestEvents.milestone(level, at, key);
	}
}
