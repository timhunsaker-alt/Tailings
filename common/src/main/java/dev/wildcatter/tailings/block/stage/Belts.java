package dev.wildcatter.tailings.block.stage;

import dev.wildcatter.multiblock.BigParts;
import dev.wildcatter.tailings.block.BeltBlockEntity;
import dev.wildcatter.tailings.block.BeltConveyorBlock;
import dev.wildcatter.tailings.block.Feeds;
import dev.wildcatter.tailings.block.PlantBlockEntity;
import dev.wildcatter.tailings.plant.BeltTravel;
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

	/** Kg a metre of belt carries when it is full, at its speed: three times the plant's rate (proposed). */
	static final double FULL_KG_PER_METRE = BELT_KG_PER_TICK * 20 / PlantRates.BELT_METRES_PER_SECOND;

	static final Stage DRIVE = new Stage() {
		@Override
		public double dropAt(PlantBlockEntity part, @Nullable BlockPos belt) {
			if (!(part.run instanceof Scan scan) || scan.run().isEmpty()) {
				return 0;
			}
			int index = belt == null ? -1 : scan.run().indexOf(belt);
			int last = scan.run().size() - 1;
			boolean headLast = scan.run().get(last).equals(scan.head());
			if (index < 0) {
				return scan.run().size();   // onto the tail end
			}
			return (headLast ? last - index : index) + 0.5;
		}

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
			part.travel().match(part.hold().total());
			int needed = PlantRates.drivesFor(scan.run().size());
			int running = 0;
			for (BlockPos at : scan.drives()) {
				if (level.getBlockEntity(at) instanceof PlantBlockEntity drive && drive.running()) {
					running++;
				}
			}
			boolean moving = running >= needed;
			if (!moving) {
				if (part.scanDue()) {
					draw(part, scan, false);
				}
				part.say(running == 0 ? "NO DRIVE: the drive's motor is not running"
						: "NO DRIVE: a " + scan.run().size() + " m run needs " + needed + " drives running (7.5 kW each 30 m)");
				return;
			}
			boolean blocked = deliver(part, scan);
			// a run whose head end is blocked stops, as a real belt trips on a full chute, rather than piling rock at its head
			if (!blocked) {
				part.travel().advance(PlantRates.BELT_METRES_PER_SECOND / 20);
			}
			if (part.scanDue()) {
				draw(part, scan, !blocked);
			}
		}
	};

	/** Delivers what has reached the head end; true when the head end is blocked (nothing takes it, or what it feeds is full). */
	private static boolean deliver(PlantBlockEntity part, Scan scan) {
		if (part.hold().isEmpty()) {
			part.say("running empty");
			return false;
		}
		double ready = part.travel().ready();
		PlantBlockEntity.Sink sink = receiver(part, scan);
		net.minecraft.world.Container box = sink == null ? container(part, scan) : null;
		if (box != null) {
			bag(part, box);
		}
		if (sink == null && box == null) {
			part.say(Crushing.NOWHERE + " (nothing takes the load at its head end)");
			return ready > 0;
		}
		if (ready < 1e-6) {
			part.say(String.format("carrying %.0f kg of %s, %.0f s from its head end", part.hold().total(), Crushing.main(part.hold()),
					BeltTravel.transitSeconds(scan.run().size(), PlantRates.BELT_METRES_PER_SECOND)));
			return false;
		}
		double room = sink != null ? sink.room(part.hold()) : Math.max(0, 2 * PlantRates.BLOCK_KG - part.made().total());
		double max = Math.min(Math.min(BELT_KG_PER_TICK * part.strength(), room), ready);
		if (max < 1e-6) {
			part.say(box != null ? "FULL: the container at its head end is full" : "FULL: what the head end feeds is full");
			return true;
		}
		RockLoad taken = part.hold().take(max);
		part.travel().take(max);
		if (sink != null) {
			sink.put(taken);
		} else {
			part.made().add(taken);
			bag(part, box);
		}
		part.say(String.format("carrying %.0f kg of %s", part.hold().total(), Crushing.main(part.hold())));
		part.setChanged();
		return false;
	}

	/** A hopper or chest at the head end, which takes the run's rock as whole items (a block's worth of one rock and size each). */
	private static net.minecraft.world.@Nullable Container container(PlantBlockEntity part, Scan scan) {
		BlockPos head = scan.head();
		for (BlockPos at : new BlockPos[] {head.below(), head.relative(scan.way()), head.relative(scan.way()).below()}) {
			if (BigParts.entity(part.getLevel(), at) == null && part.getLevel().getBlockEntity(at) instanceof net.minecraft.world.Container box
					&& !(part.getLevel().getBlockEntity(at) instanceof BeltBlockEntity)) {
				return box;
			}
		}
		return null;
	}

	/** Each block's worth of one rock and size at the head end goes into {@code box} as its item, while it fits. */
	private static void bag(PlantBlockEntity part, net.minecraft.world.Container box) {
		java.util.List<Object[]> whole = new ArrayList<>();
		part.made().forEach((rock, size, kg) -> {
			if (kg >= PlantRates.BLOCK_KG - 1e-6) {
				whole.add(new Object[] {rock, size});
			}
		});
		for (Object[] one : whole) {
			Rock rock = (Rock) one[0];
			dev.wildcatter.tailings.rock.SizeClass size = (dev.wildcatter.tailings.rock.SizeClass) one[1];
			net.minecraft.world.item.ItemStack item = Feeds.stack(size == null ? Feeds.blockId(rock) : size.itemId(rock), 1);
			if (item.isEmpty()) {
				continue;
			}
			net.minecraft.world.item.ItemStack left = net.minecraft.world.level.block.entity.HopperBlockEntity.addItem(null, box, item, Direction.UP);
			if (left.isEmpty()) {
				part.made().take(PlantRates.BLOCK_KG, (r, s) -> r == rock && s == size);
				box.setChanged();
				part.setChanged();
			}
		}
	}

	/** What the run's head end delivers into: straight ahead, under it, ahead and under it, beside it, then over it. Never the run or a drive. */
	private static PlantBlockEntity.@Nullable Sink receiver(PlantBlockEntity part, Scan scan) {
		BlockPos head = scan.head();
		List<BlockPos> tries = new ArrayList<>();
		List<Direction> into = new ArrayList<>();
		tries.add(head.relative(scan.way()));
		into.add(scan.way());
		tries.add(head.below());
		into.add(Direction.DOWN);
		tries.add(head.relative(scan.way()).below());
		into.add(Direction.DOWN);
		for (Direction side : Direction.Plane.HORIZONTAL) {
			tries.add(head.relative(side));
			into.add(side);
		}
		tries.add(head.above());
		into.add(Direction.UP);
		for (int i = 0; i < tries.size(); i++) {
			BlockPos at = tries.get(i);
			if (scan.run().contains(at) || scan.drives().contains(BigParts.owner(part.getLevel(), at))) {
				continue;
			}
			if (BigParts.entity(part.getLevel(), at) instanceof PlantBlockEntity other && other.machine() == Machine.CONVEYOR_DRIVE) {
				continue;
			}
			PlantBlockEntity.Sink sink = part.sinkAt(at, into.get(i), part);
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

	/** Each belt of the run shows the load's main rock, how much rides on that metre of it, and which way it moves. */
	private static void draw(PlantBlockEntity part, Scan scan, boolean moving) {
		Rock rock = part.hold().main();
		String id = rock == null ? "" : rock.name();
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
			int fromHead = run.size() - 1 - i;
			float fill = (float) Math.min(1, part.travel().between(fromHead, fromHead + 1) / FULL_KG_PER_METRE);
			if (part.getLevel().getBlockEntity(run.get(i)) instanceof BeltBlockEntity belt) {
				belt.set(part.getBlockPos(), fill > 0 ? id : "", fill, toward, moving);
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
			PlantBlockEntity.Sink sink = part.sink(part.side(Direction.DOWN));
			if (sink == null) {
				part.say(part.binCount() == 0 && part.hold().isEmpty() ? "NO FEED: empty" : Crushing.NOWHERE + " (a belt under it)");
				return;
			}
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
