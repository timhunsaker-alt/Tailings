package dev.wildcatter.tailings.block.stage;

import dev.wildcatter.multiblock.BigParts;
import dev.wildcatter.tailings.block.BeltBlockEntity;
import dev.wildcatter.tailings.block.BeltConveyorBlock;
import dev.wildcatter.tailings.block.Feeds;
import dev.wildcatter.tailings.block.PlantBlockEntity;
import dev.wildcatter.tailings.plant.Machine;
import dev.wildcatter.tailings.plant.PlantRates;
import dev.wildcatter.tailings.plant.RockLoad;
import dev.wildcatter.tailings.rock.Rock;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

/**
 * Belt conveyors and what feeds them (docs/multiblocks/crushing-circuit.md): the conveyor drive at a run's head end holds the run's
 * load and delivers it into whatever the head end touches; a run moves only while it has 7.5 kW of running drive for each 30 m. The
 * ore bin feeds the belt under it from the rock items it holds.
 */
final class Belts {
	private Belts() {
	}

	/** A belt carries up to three times the plant's rate, for the screen and cone's circulating load (proposed). */
	static final double BELT_KG_PER_TICK = 3 * PlantRates.KG_PER_TICK;

	/** A drive's scan of the run it touches. {@code holder} is true for the drive at the head end, which holds the load. */
	record Scan(List<BlockPos> run, BlockPos head, Direction way, boolean holder, Set<BlockPos> drives) {
	}

	static final Stage DRIVE = new Stage() {
		@Override
		public void tick(PlantBlockEntity part) {
			Level level = part.getLevel();
			if (part.scanDue() || !(part.run instanceof Scan)) {
				part.run = scan(part);
			}
			Scan scan = (Scan) part.run;
			part.want();
			if (scan.run().isEmpty()) {
				part.say("no belt run touches it");
				return;
			}
			if (!scan.holder()) {
				part.say("helping drive a " + scan.run().size() + " m run");
				return;
			}
			int needed = PlantRates.drivesFor(scan.run().size());
			int running = 0;
			for (BlockPos at : scan.drives()) {
				if (level.getBlockEntity(at) instanceof PlantBlockEntity drive && drive.running()) {
					running++;
				}
			}
			boolean moving = running >= needed;
			if (part.scanDue()) {
				draw(part, scan, moving);
			}
			if (!moving) {
				part.say(running == 0 ? "NO DRIVE: the drive's motor is not running"
						: "NO DRIVE: a " + scan.run().size() + " m run needs " + needed + " drives running (7.5 kW each 30 m)");
				return;
			}
			if (part.hold().isEmpty()) {
				part.say("running empty");
				return;
			}
			PlantBlockEntity.Sink sink = receiver(part, scan);
			if (sink == null) {
				part.say(Crushing.NOWHERE + " (nothing takes the load at its head end)");
				return;
			}
			double max = Math.min(BELT_KG_PER_TICK * part.strength(), sink.room(part.hold()));
			if (max < 1e-6) {
				part.say("FULL: what the head end feeds is full");
				return;
			}
			sink.put(part.hold().take(max));
			part.say(String.format("carrying %.0f kg of %s", part.hold().total(), Crushing.main(part.hold())));
			part.setChanged();
		}
	};

	/** What the run's head end delivers into: straight ahead, under it, beside it, then over it (a chute). Never the run or a drive. */
	private static PlantBlockEntity.@Nullable Sink receiver(PlantBlockEntity part, Scan scan) {
		BlockPos head = scan.head();
		List<BlockPos> tries = new ArrayList<>();
		tries.add(head.relative(scan.way()));
		tries.add(head.below());
		tries.add(head.relative(scan.way()).below());
		for (Direction side : Direction.Plane.HORIZONTAL) {
			tries.add(head.relative(side));
		}
		tries.add(head.above());
		for (BlockPos at : tries) {
			if (scan.run().contains(at) || scan.drives().contains(BigParts.owner(part.getLevel(), at))) {
				continue;
			}
			if (BigParts.entity(part.getLevel(), at) instanceof PlantBlockEntity other && other.machine() == Machine.CONVEYOR_DRIVE) {
				continue;
			}
			PlantBlockEntity.Sink sink = part.sinkAt(at, part);
			if (sink != null) {
				return sink;
			}
		}
		return null;
	}

	/** Finds the run this drive touches, its head end and every drive on it, and tells each belt who holds the load. */
	private static Scan scan(PlantBlockEntity part) {
		Level level = part.getLevel();
		List<BlockPos> mine = part.cells();
		for (BlockPos cell : mine) {
			for (Direction side : Direction.values()) {
				BlockPos next = cell.relative(side);
				if (!(level.getBlockState(next).getBlock() instanceof BeltConveyorBlock)) {
					continue;
				}
				List<BlockPos> run = BeltConveyorBlock.run(level, next);
				Set<BlockPos> drives = new LinkedHashSet<>();
				for (BlockPos belt : run) {
					for (Direction d : Direction.values()) {
						if (BigParts.entity(level, belt.relative(d)) instanceof PlantBlockEntity drive && drive.machine() == Machine.CONVEYOR_DRIVE) {
							drives.add(drive.getBlockPos());
						}
					}
				}
				BlockPos first = run.get(0), last = run.get(run.size() - 1);
				BlockPos head = touches(level, last, drives) != null ? last : touches(level, first, drives) != null ? first : null;
				if (head == null) {
					return new Scan(run, last, Direction.NORTH, false, drives);
				}
				BlockPos holder = touches(level, head, drives);
				Direction way = BeltConveyorBlock.outward(level, run, head, holder == null ? head : nearest(level, head, holder));
				boolean holds = part.getBlockPos().equals(holder);
				if (holds) {
					for (BlockPos belt : run) {
						if (level.getBlockEntity(belt) instanceof BeltBlockEntity entity) {
							entity.set(holder, entity.rock(), entity.fill(), entity.toward(), entity.moving());
						}
					}
				}
				return new Scan(run, head, way, holds, drives);
			}
		}
		return new Scan(List.of(), part.getBlockPos(), Direction.NORTH, false, Set.of());
	}

	/** The drive (by main cell) among {@code drives} touching {@code belt}, lowest position first; null if none. */
	private static @Nullable BlockPos touches(Level level, BlockPos belt, Set<BlockPos> drives) {
		BlockPos best = null;
		for (Direction d : Direction.values()) {
			BlockPos owner = BigParts.owner(level, belt.relative(d));
			if (owner != null && drives.contains(owner) && (best == null || owner.asLong() < best.asLong())) {
				best = owner;
			}
		}
		return best;
	}

	/** The cell of the drive at {@code main} next to {@code belt}. */
	private static BlockPos nearest(Level level, BlockPos belt, BlockPos main) {
		for (Direction d : Direction.values()) {
			if (main.equals(BigParts.owner(level, belt.relative(d)))) {
				return belt.relative(d);
			}
		}
		return main;
	}

	/** Each belt of the run shows the load's main rock, how full the run is, and which way it moves. */
	private static void draw(PlantBlockEntity part, Scan scan, boolean moving) {
		Rock rock = part.hold().main();
		String id = rock == null ? "" : rock.name();
		float fill = (float) Math.min(1, part.hold().total() / PlantRates.BELT_HOLD_KG);
		List<BlockPos> run = new ArrayList<>(scan.run());
		if (!run.get(run.size() - 1).equals(scan.head())) {
			java.util.Collections.reverse(run);
		}
		for (int i = 0; i < run.size(); i++) {
			Direction toward = scan.way();
			if (i + 1 < run.size()) {
				BlockPos a = run.get(i), b = run.get(i + 1);
				int dx = b.getX() - a.getX(), dz = b.getZ() - a.getZ();
				toward = dx > 0 ? Direction.EAST : dx < 0 ? Direction.WEST : dz > 0 ? Direction.SOUTH : Direction.NORTH;
			}
			if (part.getLevel().getBlockEntity(run.get(i)) instanceof BeltBlockEntity belt) {
				belt.set(part.getBlockPos(), id, fill, toward, moving);
			}
		}
	}

	/** The ore bin: rock items in by hand, hopper or hook; out of its bottom onto the belt under it. */
	static final Stage BIN = new Stage() {
		@Override
		public double holdKg() {
			return PlantRates.BLOCK_KG;
		}

		@Override
		public void tick(PlantBlockEntity part) {
			if (part.hold().isEmpty()) {
				Iterator<Map.Entry<String, Integer>> it = part.binned().entrySet().iterator();
				if (it.hasNext()) {
					Map.Entry<String, Integer> next = it.next();
					Feeds.Charge charge = Feeds.parse(Feeds.stack(next.getKey(), 1));
					if (next.getValue() <= 1) {
						it.remove();
					} else {
						next.setValue(next.getValue() - 1);
					}
					if (charge != null) {
						part.hold().add(charge.rock(), charge.size(), PlantRates.BLOCK_KG);
					}
					part.setChanged();
				}
			}
			if (part.hold().isEmpty()) {
				part.say("NO FEED: empty");
				return;
			}
			PlantBlockEntity.Sink sink = part.sink(part.side(Direction.DOWN));
			if (sink == null) {
				part.say(Crushing.NOWHERE + " (a belt under it)");
				return;
			}
			double max = Math.min(BELT_KG_PER_TICK, sink.room(part.hold()));
			if (max < 1e-6) {
				part.say("FULL: its belt is full");
				return;
			}
			sink.put(part.hold().take(max));
			part.say("feeding " + Crushing.main(part.hold()));
			part.setChanged();
		}
	};
}
