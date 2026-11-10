package dev.wildcatter.tailings.block.stage;

import dev.wildcatter.block.Lines;
import dev.wildcatter.multiblock.BigParts;
import dev.wildcatter.tailings.TailingsBlocks;
import dev.wildcatter.tailings.block.BeltBlockEntity;
import dev.wildcatter.tailings.block.BeltConveyorBlock;
import dev.wildcatter.tailings.block.DumpBlockEntity;
import dev.wildcatter.tailings.block.Feeds;
import dev.wildcatter.tailings.block.MuckBlockEntity;
import dev.wildcatter.tailings.block.PlantBlockEntity;
import dev.wildcatter.tailings.block.PlantMachineBlock;
import dev.wildcatter.tailings.block.StockpileRockBlock;
import dev.wildcatter.tailings.pit.Loose;
import dev.wildcatter.tailings.pit.PitMass;
import dev.wildcatter.tailings.plant.Machine;
import dev.wildcatter.tailings.plant.PitLine;
import dev.wildcatter.tailings.plant.PlantRates;
import dev.wildcatter.tailings.plant.RockLoad;
import dev.wildcatter.tailings.rock.Rock;
import dev.wildcatter.tailings.rock.SizeClass;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/**
 * The pit line's primary crushing and stockpile (docs/multiblocks/primary-gyratory-stockpile.md, P7a; decisions in
 * docs/questions/p7-pit-line.md): the gyratory under its dump pocket, the rock breaker on the pocket's rim, the surge pocket under the
 * crusher, the primary apron feeder, the stockpile head dropping crushed rock onto the pile by the settle rule, the reclaim feeders
 * drawing it from under the pile, and the belt weigher whose setpoint they follow. As the small plant's parts, each runs on its own
 * motor while it has work and somewhere to put it.
 */
public final class PitLineStages {
	private PitLineStages() {
	}

	// ---- finding the parts round a part -------------------------------------------------------------------------------------------

	/** The gyratory's dump pocket standing on its top, or null. */
	static @Nullable DumpBlockEntity pocketAbove(PlantBlockEntity part) {
		List<BlockPos> mine = part.cells();
		for (BlockPos cell : mine) {
			BlockPos up = cell.above();
			if (!mine.contains(up) && BigParts.entity(part.getLevel(), up) instanceof DumpBlockEntity pocket && is(part.getLevel(), up, Machine.GYRATORY_POCKET)) {
				return pocket;
			}
		}
		return null;
	}

	/** The gyratory standing under {@code pocket}'s floor, or null. */
	static @Nullable PlantBlockEntity crusherUnder(Level level, DumpBlockEntity pocket) {
		List<BlockPos> cells = BigParts.cells(pocket.getBlockPos(), pocket.getBlockState());
		for (BlockPos cell : cells) {
			BlockPos down = cell.below();
			if (!cells.contains(down) && BigParts.entity(level, down) instanceof PlantBlockEntity crusher && crusher.machine() == Machine.GYRATORY_CRUSHER) {
				return crusher;
			}
		}
		return null;
	}

	private static boolean is(Level level, BlockPos pos, Machine machine) {
		return level.getBlockState(pos).getBlock() instanceof PlantMachineBlock block && block.machine() == machine;
	}

	/** The gyratory's pocket's level, 0 to 1 (its FILL). */
	public static double pocketLevel(PlantBlockEntity crusher) {
		DumpBlockEntity pocket = pocketAbove(crusher);
		return pocket == null ? 0 : pocket.pocket() / (double) pocket.capacity();
	}

	/** True when the pocket cannot take another 363 t load: FULL, the next truck waits. */
	public static boolean pocketFull(PlantBlockEntity crusher) {
		DumpBlockEntity pocket = pocketAbove(crusher);
		return pocket != null && pocket.pocket() * (double) PitMass.ROCK_KG + 363_000 > pocket.capacity() * (double) PitMass.ROCK_KG;
	}

	/** Hard rock, which can come as a boulder too big for the bowl: everything but the soft ores, tuff and sandstone. */
	static boolean hard(String itemId, Rock rock) {
		return !PitMass.soft(itemId) && rock != Rock.TUFF && rock != Rock.SANDSTONE;
	}

	/** Where a part puts rock out of its {@code end} (its south, discharge end): a belt or part beyond that face, or under that end. */
	static PlantBlockEntity.@Nullable Sink discharge(PlantBlockEntity part) {
		PlantBlockEntity.Sink out = part.sink(part.side(Direction.SOUTH));
		return out != null ? out : part.sink(part.faces(at -> at[2] == part.machine().depth() - 1, Direction.DOWN));
	}

	/** The belt a part discharges onto, if any: the first belt beyond its south face, or under its south end. */
	static @Nullable BlockPos dischargeBelt(PlantBlockEntity part) {
		List<Lines.Face> faces = new ArrayList<>(part.side(Direction.SOUTH));
		faces.addAll(part.faces(at -> at[2] == part.machine().depth() - 1, Direction.DOWN));
		for (Lines.Face face : faces) {
			if (part.getLevel().getBlockState(face.pipe()).getBlock() instanceof BeltConveyorBlock) {
				return face.pipe();
			}
		}
		return null;
	}

	/** The belt weigher touching any belt of the run {@code belt} is on, or null. */
	static @Nullable PlantBlockEntity weigherOn(Level level, BlockPos belt) {
		for (BlockPos at : BeltConveyorBlock.run(level, belt)) {
			for (Direction d : Direction.values()) {
				if (BigParts.entity(level, at.relative(d)) instanceof PlantBlockEntity weigher && weigher.machine() == Machine.BELT_WEIGHER) {
					return weigher;
				}
			}
		}
		return null;
	}

	/** The part the run {@code belt} is on delivers into at its head end, by its drive's last scan; null if none. */
	public static @Nullable PlantBlockEntity headOf(Level level, BlockPos belt) {
		if (!(level.getBlockEntity(belt) instanceof BeltBlockEntity entity) || entity.drive() == null
				|| !(level.getBlockEntity(entity.drive()) instanceof PlantBlockEntity drive) || !(drive.run instanceof Belts.Scan scan) || scan.run().isEmpty()) {
			return null;
		}
		BlockPos head = scan.head();
		for (BlockPos at : new BlockPos[] {head.relative(scan.way()), head.below(), head.relative(scan.way()).below()}) {
			if (!scan.run().contains(at) && BigParts.entity(level, at) instanceof PlantBlockEntity part && !scan.drives().contains(part.getBlockPos())) {
				return part;
			}
		}
		return null;
	}

	// ---- the gyratory -------------------------------------------------------------------------------------------------------------

	/**
	 * The gyratory: it takes the bank blocks the trucks tipped into the pocket over its bowl, a block at a time, crushes them to the
	 * crushed class at about 2,400 t/h and drops them into the surge pocket under it. Every 200th block of hard rock is a boulder that
	 * bridges the bowl: it stops until the rock breaker has broken it, 30 s.
	 */
	static final Stage GYRATORY = new Stage() {
		@Override
		public double holdKg() {
			return PlantRates.BLOCK_KG;
		}

		@Override
		public void tick(PlantBlockEntity part) {
			DumpBlockEntity pocket = pocketAbove(part);
			if (pocket == null) {
				part.say("NOT BUILT: needs a Gyratory Dump Pocket standing on its top, over the bowl");
				return;
			}
			PlantBlockEntity surge = part.under(Machine.SURGE_POCKET);
			if (surge == null) {
				part.say("NOT BUILT: needs a Surge Pocket under it");
				return;
			}
			if (part.pit().bridged()) {
				part.want();
				part.say(String.format("BRIDGED: a boulder jams the bowl; the rock breaker has worked on it %d of %d s", part.pit().breakTicks() / 20, PitLine.BREAK_SECONDS));
				return;
			}
			if (part.hold().isEmpty() && pocket.pocket() > 0) {
				Map<String, Integer> one = pocket.draw(1);
				for (Map.Entry<String, Integer> e : one.entrySet()) {
					Feeds.Charge charge = Feeds.parse(Feeds.stack(e.getKey(), 1));
					if (charge == null) {
						continue;
					}
					part.hold().add(charge.rock(), null, PlantRates.BLOCK_KG * e.getValue());
					if (part.pit().countBlock(hard(e.getKey(), charge.rock()))) {
						part.setChanged();
						part.say("BRIDGED: a boulder jams the bowl");
						return;
					}
				}
				part.setChanged();
			}
			if (part.hold().isEmpty()) {
				part.say("NO FEED: the pocket over the bowl is empty");
				return;
			}
			part.want();
			if (!part.running()) {
				part.say("waiting for its motor");
				return;
			}
			double max = Math.min(PitLine.kgPerTick(PitLine.GYRATORY_TPH) * part.strength(), surge.room());
			if (max < 1e-6) {
				part.say("FULL: the surge pocket under it is full");
				return;
			}
			RockLoad taken = part.hold().take(max);
			RockLoad crushed = new RockLoad();
			taken.forEach((rock, size, kg) -> crushed.add(rock, SizeClass.CRUSHED, kg));
			surge.receive(crushed);
			part.say("crushing " + Crushing.main(taken) + String.format(", %d of %d t in its pocket", pocket.pocket() * PitMass.ROCK_KG / 1000, pocket.capacity() * PitMass.ROCK_KG / 1000));
			part.setChanged();
		}
	};

	/** The pedestal rock breaker on the pocket's rim: it works on a boulder bridging the bowl of the gyratory under the pocket it touches. */
	static final Stage BREAKER = new Stage() {
		@Override
		public void tick(PlantBlockEntity part) {
			DumpBlockEntity pocket = Dump.pocket(part);
			if (pocket == null || !is(part.getLevel(), pocket.getBlockPos(), Machine.GYRATORY_POCKET)) {
				part.say("NOT BUILT: stands on a Gyratory Dump Pocket's rim, touching it");
				return;
			}
			PlantBlockEntity crusher = crusherUnder(part.getLevel(), pocket);
			if (crusher == null) {
				part.say("NOT BUILT: no Gyratory Crusher under its pocket");
				return;
			}
			if (!crusher.pit().bridged()) {
				part.say("standing by: the bowl is clear");
				return;
			}
			part.want();
			if (!part.running()) {
				part.say("a boulder bridges the bowl: waiting for its motor");
				return;
			}
			if (crusher.pit().breakTick()) {
				part.say("broke the boulder: the bowl is clear");
				crusher.say("the rock breaker broke the boulder");
			} else {
				part.say(String.format("breaking a boulder in the bowl, %d of %d s", crusher.pit().breakTicks() / 20, PitLine.BREAK_SECONDS));
			}
			crusher.setChanged();
		}
	};

	/** The surge pocket: it holds the crusher's product, 500 t, for the primary apron feeder under it. */
	static final Stage SURGE = new Stage() {
		@Override
		public double holdKg() {
			return PitLine.SURGE_KG;
		}

		@Override
		public boolean accepts(Rock rock, @Nullable SizeClass size) {
			return size == SizeClass.CRUSHED;
		}

		@Override
		public void tick(PlantBlockEntity part) {
			if (part.scanDue()) {
				part.say(part.hold().isEmpty() ? "empty" : String.format("holds %.0f of %.0f t of crushed rock", part.hold().total() / 1000, PitLine.SURGE_KG / 1000));
			}
		}
	};

	/** The 2.4 m primary apron feeder under the surge pocket: up to 3,000 t/h onto the belt at its south (discharge) end. */
	static final Stage PRIMARY_FEEDER = new Stage() {
		@Override
		public void tick(PlantBlockEntity part) {
			PlantBlockEntity surge = part.touching(Machine.SURGE_POCKET);
			if (surge == null) {
				part.say("NOT BUILT: needs a Surge Pocket over it");
				return;
			}
			if (surge.hold().isEmpty()) {
				part.say("NO FEED: the surge pocket is empty");
				return;
			}
			PlantBlockEntity.Sink out = discharge(part);
			if (out == null) {
				part.say(Crushing.NOWHERE + " (a heavy belt at its south end)");
				return;
			}
			part.want();
			if (!part.running()) {
				part.say("waiting for its motor");
				return;
			}
			double max = Math.min(PitLine.kgPerTick(PitLine.PRIMARY_FEEDER_TPH) * part.strength(), out.room(surge.hold()));
			if (max < 1e-6) {
				part.say("FULL: its belt is full");
				return;
			}
			RockLoad taken = surge.hold().take(max);
			out.put(taken);
			surge.setChanged();
			part.say("feeding " + Crushing.main(taken));
			part.setChanged();
		}
	};

	// ---- the stockpile ------------------------------------------------------------------------------------------------------------

	/**
	 * The stockpile head: the belt delivers over its top, and it drops the load through its chute, at its south end, as stockpile blocks
	 * that settle by the rule, three quarters of a bank block each: a whole block of one rock when it has one, else a mixed block.
	 */
	static final Stage STOCKPILE_HEAD = new Stage() {
		@Override
		public double holdKg() {
			return 3 * PitLine.STOCKPILE_BLOCK_KG;
		}

		@Override
		public boolean accepts(Rock rock, @Nullable SizeClass size) {
			return size == SizeClass.CRUSHED || size == SizeClass.GRAVEL;
		}

		@Override
		public boolean feeds(PlantBlockEntity part, int[] at, Direction localSide) {
			return Stage.top(part, at, localSide);
		}

		@Override
		public void tick(PlantBlockEntity part) {
			if (!(part.getLevel() instanceof ServerLevel level)) {
				return;
			}
			Map<String, Integer> quarters = nextBlock(part.hold());
			if (quarters == null) {
				if (part.scanDue()) {
					part.say(String.format("stacking: the pile is %d m under its chute", depthUnder(part)));
				}
				return;
			}
			BlockPos chute = chute(part);
			BlockPos rest = Loose.muck(level, chute, TailingsBlocks.STOCKPILE_ROCK.defaultBlockState(), quarters);
			if (rest == null) {
				part.say("FULL: the pile reaches its chute");
				return;
			}
			quarters.forEach((id, q) -> {
				Feeds.Charge charge = Feeds.parse(Feeds.stack(id, 1));
				if (charge != null) {
					part.hold().take(q * PlantRates.BLOCK_KG / 4, (r, s) -> r == charge.rock());
				}
			});
			part.pit().stacked(PitLine.STOCKPILE_BLOCK_KG);
			part.say(String.format("stacking: the pile is %d m under its chute", depthUnder(part)));
			part.setChanged();
		}
	};

	/** The cell the head's chute drops into: under the middle of its south end. */
	static BlockPos chute(PlantBlockEntity part) {
		Machine m = part.machine();
		List<Lines.Face> under = part.faces(at -> at[0] == m.width() / 2 && at[1] == 0 && at[2] == m.depth() - 1, Direction.DOWN);
		return under.isEmpty() ? part.getBlockPos().below() : under.get(0).pipe();
	}

	/** Metres from the chute down to the pile's top under it (its height readout). */
	static int depthUnder(PlantBlockEntity part) {
		BlockPos at = chute(part);
		int down = 0;
		while (down < 64 && part.getLevel().getBlockState(at.below(down)).isAir()) {
			down++;
		}
		return down;
	}

	/** The quarters of the next block to drop, or null when the head has not a block's worth: a rock with a whole block first. */
	static @Nullable Map<String, Integer> nextBlock(RockLoad hold) {
		double block = PitLine.STOCKPILE_BLOCK_KG;
		Rock[] whole = {null};
		hold.forEach((rock, size, kg) -> {
			if (whole[0] == null && hold.kg(rock, SizeClass.CRUSHED) + hold.kg(rock, SizeClass.GRAVEL) >= block - 1e-6) {
				whole[0] = rock;
			}
		});
		Map<String, Integer> out = new LinkedHashMap<>();
		if (whole[0] != null) {
			out.put(Feeds.blockId(whole[0]), PitLine.STOCKPILE_QUARTERS);
			return out;
		}
		if (hold.total() < 2 * block) {
			return null;
		}
		// a mixed block: a quarter of each of the rocks with the most, as many quarters as it has
		List<Rock> rocks = new ArrayList<>();
		hold.forEach((rock, size, kg) -> {
			if (!rocks.contains(rock)) {
				rocks.add(rock);
			}
		});
		rocks.sort((a, b) -> Double.compare(hold.kg(b, SizeClass.CRUSHED) + hold.kg(b, SizeClass.GRAVEL), hold.kg(a, SizeClass.CRUSHED) + hold.kg(a, SizeClass.GRAVEL)));
		int left = PitLine.STOCKPILE_QUARTERS;
		boolean added = true;
		while (left > 0 && added) {
			added = false;
			for (Rock rock : rocks) {
				double has = hold.kg(rock, SizeClass.CRUSHED) + hold.kg(rock, SizeClass.GRAVEL) - out.getOrDefault(Feeds.blockId(rock), 0) * PlantRates.BLOCK_KG / 4;
				if (left > 0 && has >= PlantRates.BLOCK_KG / 4 - 1e-6) {
					out.merge(Feeds.blockId(rock), 1, Integer::sum);
					left--;
					added = true;
				}
			}
		}
		if (out.isEmpty()) {
			return null;
		}
		return out;
	}

	/**
	 * A reclaim feeder in the tunnel under the pile: it draws the stockpile block standing over the opening at its north (feed) end and
	 * feeds it onto the belt at its south end, up to 500 t/h, at its share of the belt weigher's setpoint. The column over the opening
	 * falls into the gap and the slopes round it roll in by the settle rule.
	 */
	static final Stage RECLAIM_FEEDER = new Stage() {
		@Override
		public double holdKg() {
			return 2 * PitLine.STOCKPILE_BLOCK_KG;
		}

		@Override
		public void tick(PlantBlockEntity part) {
			Level level = part.getLevel();
			long now = level.getGameTime();
			PlantBlockEntity.Sink out = discharge(part);
			BlockPos belt = dischargeBelt(part);
			PlantBlockEntity weigher = belt == null ? null : weigherOn(level, belt);
			if (out == null) {
				part.setFeederTph(0);
				part.say(Crushing.NOWHERE + " (a heavy belt at its south end)");
				return;
			}
			double target = PitLine.RECLAIM_FEEDER_TPH;
			if (weigher != null) {
				// the setpoint shared among the feeders drawing for this weigher, never over a feeder's own 500 t/h
				target = PitLine.feederShareTph(weigher.setpointTph(), Math.max(1, weigher.pit().feeders(now)));
			}
			// keep a block in hand: draw the one over the opening
			if (part.hold().total() < PitLine.STOCKPILE_BLOCK_KG) {
				BlockPos over = opening(part);
				if (over != null && level.getBlockEntity(over) instanceof MuckBlockEntity muck) {
					RockLoad got = new RockLoad();
					muck.quarters().forEach((id, q) -> {
						Feeds.Charge charge = Feeds.parse(Feeds.stack(id, 1));
						if (charge != null) {
							got.add(charge.rock(), SizeClass.CRUSHED, q * PlantRates.BLOCK_KG / 4);
						}
					});
					level.setBlock(over, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
					if (level instanceof ServerLevel server) {
						StockpileRockBlock.stir(server, over);
					}
					part.hold().add(got);
					part.setChanged();
				}
			}
			if (part.hold().isEmpty()) {
				part.setFeederTph(0);
				part.say("NO FEED: no pile over its opening");
				return;
			}
			if (weigher != null) {
				weigher.pit().feederRan(part.getBlockPos().asLong(), now);
			}
			part.want();
			if (!part.running()) {
				part.setFeederTph(0);
				part.say("waiting for its motor");
				return;
			}
			double max = Math.min(PitLine.kgPerTick(target) * part.strength() * part.fast(), out.room(part.hold()));
			if (max < 1e-6) {
				part.setFeederTph(0);
				part.say("FULL: its belt is full");
				return;
			}
			RockLoad taken = part.hold().take(max);
			out.put(taken);
			part.pit().drew(taken.total());
			if (weigher != null) {
				weigher.pit().weigh(taken.total());
			}
			part.setFeederTph(taken.total() * 20 * 3.6 / part.fast());
			part.say(String.format("reclaiming %s at %.0f t/h%s", Crushing.main(taken), part.feederTph(), weigher == null ? " (no belt weigher: its full rate)" : ""));
			part.setChanged();
		}
	};

	/** The stockpile block over a feeder's opening: the first block over its north end's top, through the tunnel's roof, up to 4 m up. */
	static @Nullable BlockPos opening(PlantBlockEntity part) {
		Level level = part.getLevel();
		for (Lines.Face face : part.faces(at -> at[2] == 0 && at[1] == part.machine().height() - 1, Direction.UP)) {
			for (int up = 0; up < 4; up++) {
				BlockPos at = face.pipe().above(up);
				BlockState state = level.getBlockState(at);
				if (state.getBlock() instanceof StockpileRockBlock) {
					return at;
				}
				if (!state.isAir()) {
					break;
				}
			}
		}
		return null;
	}

	/**
	 * The belt weigher: it counts what the feeders put on its run each second as its rate and total, and while the mill its run feeds
	 * reads OVERLOAD it holds its setpoint 10 % back.
	 */
	static final Stage WEIGHER = new Stage() {
		@Override
		public void tick(PlantBlockEntity part) {
			Level level = part.getLevel();
			if (level.getGameTime() % 20 == 0) {
				part.pit().second();
				part.setChanged();
			}
			if (part.scanDue()) {
				BlockPos belt = null;
				for (BlockPos cell : part.cells()) {
					for (Direction d : Direction.values()) {
						if (level.getBlockState(cell.relative(d)).getBlock() instanceof BeltConveyorBlock) {
							belt = cell.relative(d);
						}
					}
				}
				part.run = belt;
				part.say(belt == null ? "NOT BUILT: sits beside or under a heavy belt" : "weighing its belt");
			}
			if (part.run instanceof BlockPos belt) {
				PlantBlockEntity mill = headOf(level, belt);
				if (mill != null && mill.status().startsWith("OVERLOAD")) {
					part.pit().sawOverload(level.getGameTime());
				}
			}
		}
	};
}
