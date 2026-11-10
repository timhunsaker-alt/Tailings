package dev.wildcatter.tailings.block.stage;

import dev.wildcatter.block.PipeBlock;
import dev.wildcatter.block.Plumbing;
import dev.wildcatter.block.Slurry;
import dev.wildcatter.fluid.Product;
import dev.wildcatter.fluid.SlurryAssay;
import dev.wildcatter.tailings.block.PlantBlockEntity;
import dev.wildcatter.tailings.plant.Assays;
import dev.wildcatter.tailings.plant.Machine;
import dev.wildcatter.tailings.plant.PitFlotation;
import dev.wildcatter.tailings.plant.PitLine;
import dev.wildcatter.tailings.plant.Reagent;
import dev.wildcatter.tailings.plant.Selective;
import dev.wildcatter.tailings.rock.Mineral;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import org.jspecify.annotations.Nullable;

/**
 * The pit line's flotation and tails (docs/multiblocks/pit-flotation.md, P7c; decisions in docs/questions/p7-pit-line.md): the large
 * conditioning tank, the row of tank cells, the column cleaners, the large blower whose air line serves them, and the 45 m tailings
 * thickener. The row works as one flow: each cell takes conditioned slurry, or the tails of the cell before it, in at its north face,
 * floats its share of what floats ({@link PitFlotation#cell}) out of its east launder as concentrate slurry and passes the rest out of its
 * south face. The concentrate goes through the columns to the existing concentrate thickener and pressure filter, which pay it.
 */
public final class PitFlotationStages {
	private PitFlotationStages() {
	}

	/** A tank cell's or column's air, as its air line last showed it. */
	public enum Air {
		/** No large blower on its air line. */
		NONE,
		/** Its blowers are not running. */
		OFF,
		/** Its running blowers serve ten cells each, and this one is past them. */
		SHORT,
		ON
	}

	static double outRoom(PlantBlockEntity part, Product product, double hold) {
		return Math.max(0, hold - part.out().held(product));
	}

	private static boolean aerated(Machine machine) {
		return machine == Machine.TANK_CELL || machine == Machine.COLUMN_CELL;
	}

	/** The parts on {@code part}'s air line: Wildcatter's air tubing joined to it, and the parts touching it. */
	private static List<PlantBlockEntity> onAirLine(PlantBlockEntity part) {
		List<BlockPos> cells = part.cells();
		Set<BlockPos> tubing = Plumbing.network(part.getLevel(), cells, PipeBlock.Kind.AIR);
		return Plumbing.machines(part.getLevel(), cells, tubing, true, PlantBlockEntity.class);
	}

	/** Looks again at the air line once a second; the answer is kept on the part between looks. */
	static Air air(PlantBlockEntity part) {
		if (part.scanDue() || !(part.run instanceof Air)) {
			part.run = lookForAir(part);
		}
		return (Air) part.run;
	}

	/** The air a cell's PLC point and readout show: its last look. */
	public static boolean hasAir(PlantBlockEntity part) {
		return part.run == Air.ON;
	}

	private static Air lookForAir(PlantBlockEntity part) {
		List<PlantBlockEntity> line = onAirLine(part);
		int running = 0;
		boolean any = false;
		List<PlantBlockEntity> served = new ArrayList<>();
		served.add(part);
		for (PlantBlockEntity other : line) {
			if (other.machine() == Machine.FLOTATION_BLOWER_LARGE) {
				any = true;
				running += other.running() ? 1 : 0;
			} else if (aerated(other.machine())) {
				served.add(other);
			}
		}
		if (!any) {
			return Air.NONE;
		}
		if (running == 0) {
			return Air.OFF;
		}
		served.sort(Comparator.comparingLong(p -> p.getBlockPos().asLong()));
		return served.indexOf(part) < running * PitFlotation.CELLS_PER_BLOWER ? Air.ON : Air.SHORT;
	}

	/** Says why a cell has no air and returns false, or returns true. */
	private static boolean airOrSay(PlantBlockEntity part) {
		switch (air(part)) {
			case NONE -> part.say("NOT BUILT: needs a Large Flotation Blower on its air line (air tubing) or touching it");
			case OFF -> part.say("NO AIR: the blower is not running");
			case SHORT -> part.say("NO AIR: its blowers serve " + PitFlotation.CELLS_PER_BLOWER + " cells each; add a blower");
			default -> {
				return true;
			}
		}
		return false;
	}

	/** The large conditioning tank: fines slurry in at its north face, reagents by hopper per tonne; conditioned slurry out at its south. */
	static final Stage CONDITIONER = new Stage() {
		@Override
		public @Nullable Product outlet(PlantBlockEntity part, int[] at, Direction localSide) {
			return Flotation.backFace(part, at, localSide, -1) ? Product.CONDITIONED_SLURRY : null;
		}

		@Override
		public void tick(PlantBlockEntity part) {
			Flotation.draw(part, Product.FINES_SLURRY, PitFlotation.ROW_HOLD, PitFlotation.ROW_KG_PER_TICK);
			double waiting = part.back().held(Product.FINES_SLURRY);
			if (waiting < 1e-6) {
				part.say("NO FEED");
				return;
			}
			part.want();
			if (!part.running()) {
				part.say("waiting for its motor");
				return;
			}
			double max = Math.min(Math.min(PitFlotation.ROW_KG_PER_TICK * part.strength(), waiting),
					outRoom(part, Product.CONDITIONED_SLURRY, PitFlotation.ROW_HOLD));
			if (max < 1e-6) {
				part.say(Crushing.NOWHERE + " (conditioned slurry on the pipe at its south side)");
				return;
			}
			SlurryAssay assay = part.back().assayOf(Product.FINES_SLURRY);
			Set<Reagent> dose = Selective.conditioning(false, assay.get(Mineral.SPHALERITE.mineralItemId()) > 0);
			double tonnes = max * PitLine.OVERFLOW_SOLIDS / 1000;
			String missing = part.reagents().missing(tonnes, dose);
			if (!missing.isEmpty()) {
				part.say("NO REAGENT: " + missing);
				return;
			}
			part.reagents().consume(tonnes, dose);
			part.trimReagents();
			Slurry.Taken taken = Flotation.use(part, Product.FINES_SLURRY, max);
			part.out().offer(Product.CONDITIONED_SLURRY, taken.kg(), taken.assay());
			part.say("conditioning");
			part.setChanged();
		}
	};

	/** Which cell of the row a feed's floating mineral is at (1 to 8), the heaviest first; 9 when all of it has passed the row; 0 for none. */
	static int cellOf(SlurryAssay feed) {
		double most = 0;
		int at = 0;
		for (String id : feed.minerals()) {
			Mineral mineral = Assays.mineral(PitFlotation.plain(id));
			if (mineral != null && mineral.floats() && feed.get(id) > most) {
				most = feed.get(id);
				at = PitFlotation.passed(id) + 1;
			}
		}
		return at;
	}

	/**
	 * A tank cell: conditioned slurry, or the tails of the cell before it, in at its north face; its share of what floats out of its east
	 * launder as rougher concentrate, the rest out of its south face as tails, to the next cell or the thickener. It needs air.
	 */
	static final Stage CELL = new Stage() {
		@Override
		public @Nullable Product outlet(PlantBlockEntity part, int[] at, Direction localSide) {
			if (localSide == Direction.EAST && at[0] == part.machine().width() - 1) {
				return Product.CONCENTRATE_SLURRY;
			}
			return Flotation.backFace(part, at, localSide, -1) ? Product.TAILS_SLURRY : null;
		}

		@Override
		public void tick(PlantBlockEntity part) {
			air(part);   // its AIR point and readout follow the line even while it has no feed
			Flotation.draw(part, Product.CONDITIONED_SLURRY, PitFlotation.ROW_HOLD, PitFlotation.ROW_KG_PER_TICK);
			Flotation.draw(part, Product.TAILS_SLURRY, PitFlotation.ROW_HOLD, PitFlotation.ROW_KG_PER_TICK);
			double waiting = part.back().held(Product.CONDITIONED_SLURRY) + part.back().held(Product.TAILS_SLURRY);
			if (waiting < 1e-6) {
				part.say("NO FEED");
				return;
			}
			part.want();
			if (!airOrSay(part)) {
				return;
			}
			if (!part.running()) {
				part.say("waiting for its motor");
				return;
			}
			double budget = PitFlotation.ROW_KG_PER_TICK * part.strength() * part.fast();
			double floated = 0;
			boolean moved = false;
			int cell = 0;
			for (Product feed : new Product[] {Product.CONDITIONED_SLURRY, Product.TAILS_SLURRY}) {
				double there = part.back().held(feed);
				if (there < 1e-6 || budget < 1e-6) {
					continue;
				}
				SlurryAssay assay = part.back().assayOf(feed);
				cell = Math.max(cell, cellOf(assay));
				Selective.Split per = PitFlotation.cell(assay, 1);
				double max = Math.min(budget, there);
				if (per.concentrate() > 0) {
					max = Math.min(max, outRoom(part, Product.CONCENTRATE_SLURRY, PitFlotation.ROW_HOLD) / per.concentrate());
				}
				if (per.tails() > 0) {
					max = Math.min(max, outRoom(part, Product.TAILS_SLURRY, PitFlotation.ROW_HOLD) / per.tails());
				}
				if (max < 1e-6) {
					continue;
				}
				Slurry.Taken taken = Flotation.use(part, feed, max);
				Selective.Split split = PitFlotation.cell(taken.assay(), taken.kg());
				part.out().offer(Product.CONCENTRATE_SLURRY, split.concentrate(), split.concentrateAssay());
				part.out().offer(Product.TAILS_SLURRY, split.tails(), split.tailsAssay());
				floated += split.concentrate() * PitFlotation.concentrateSolids(split.concentrateAssay());
				budget -= taken.kg();
				moved = true;
			}
			if (!moved) {
				part.say(Crushing.NOWHERE + " (" + (outRoom(part, Product.TAILS_SLURRY, PitFlotation.ROW_HOLD) < 1e-6 ? "the tails" : "the concentrate") + " pipe is full)");
				return;
			}
			part.pit().weigh(floated);
			if (cell == 0 || cell > PitFlotation.CELLS) {
				part.say("passing the tails on: nothing left in them that a cell floats");
			} else {
				part.say(String.format("floating as cell %d of the row (%s): %d %% of the block's mineral", cell, cell <= 5 ? "rougher" : "scavenger",
						PitFlotation.SHARES_PERCENT[cell - 1]));
			}
			part.setChanged();
		}
	};

	/**
	 * A column cleaner: the row's rougher concentrate in at its north face, cleaned concentrate out at its south face for the concentrate
	 * thickener, its tails (the water it gives up) out of its east face, back to cell 1. It needs air.
	 */
	static final Stage COLUMN = new Stage() {
		@Override
		public @Nullable Product outlet(PlantBlockEntity part, int[] at, Direction localSide) {
			if (localSide == Direction.EAST && at[0] == part.machine().width() - 1 && at[1] == 0) {
				return Product.TAILS_SLURRY;
			}
			return Flotation.backFace(part, at, localSide, -1) ? Product.CONCENTRATE_SLURRY : null;
		}

		@Override
		public void tick(PlantBlockEntity part) {
			air(part);
			Flotation.draw(part, Product.CONCENTRATE_SLURRY, PitFlotation.COLUMN_HOLD, PitFlotation.COLUMN_KG_PER_TICK);
			double waiting = part.back().held(Product.CONCENTRATE_SLURRY);
			if (waiting < 1e-6) {
				part.say("NO FEED");
				return;
			}
			part.want();
			if (!airOrSay(part)) {
				return;
			}
			if (!part.running()) {
				part.say("waiting for its motor");
				return;
			}
			Selective.Split per = PitFlotation.column(part.back().assayOf(Product.CONCENTRATE_SLURRY), 1);
			double max = Math.min(PitFlotation.COLUMN_KG_PER_TICK * part.strength() * part.fast(), waiting);
			max = Math.min(max, outRoom(part, Product.CONCENTRATE_SLURRY, PitFlotation.COLUMN_HOLD) / Math.max(1e-9, per.concentrate()));
			if (per.tails() > 0) {
				max = Math.min(max, outRoom(part, Product.TAILS_SLURRY, PitFlotation.COLUMN_HOLD) / per.tails());
			}
			if (max < 1e-6) {
				part.say(Crushing.NOWHERE + " (" + (outRoom(part, Product.CONCENTRATE_SLURRY, PitFlotation.COLUMN_HOLD) < 1e-6 ? "the concentrate" : "the tails") + " pipe is full)");
				return;
			}
			Slurry.Taken taken = Flotation.use(part, Product.CONCENTRATE_SLURRY, max);
			Selective.Split split = PitFlotation.column(taken.assay(), taken.kg());
			part.out().offer(Product.CONCENTRATE_SLURRY, split.concentrate(), split.concentrateAssay());
			if (split.tails() > 0) {
				part.out().offer(Product.TAILS_SLURRY, split.tails(), split.tailsAssay());
			}
			part.say(split.tails() > 0 ? "cleaning the concentrate" : "passing concentrate already cleaned");
			part.setChanged();
		}
	};

	/** The large blower runs while a tank cell or column is on its air line, or touching it. */
	static final Stage BLOWER = new Stage() {
		@Override
		public void tick(PlantBlockEntity part) {
			if (part.scanDue() || !(part.run instanceof Integer)) {
				int cells = 0;
				for (PlantBlockEntity other : onAirLine(part)) {
					cells += aerated(other.machine()) ? 1 : 0;
				}
				part.run = cells;
			}
			int cells = (Integer) part.run;
			if (cells == 0) {
				part.say("NOT BUILT: needs tank cells or column cells on its air line (air tubing) or touching it");
				return;
			}
			part.want();
			part.say(part.running() ? String.format("blowing air to %d cells (it serves %d)", cells, PitFlotation.CELLS_PER_BLOWER) : "waiting for its motor");
		}
	};

	/** The 45 m tailings thickener: the P4 thickener's stage at the line's rate, the row's marks taken off its tails. */
	static final Stage THICKENER = DryStackStages.thickener(PitFlotation.THICKENER_KG_PER_TICK, PitFlotation.ROW_HOLD, PitFlotation.THICKENER_UNDERFLOW_HOLD,
			PitFlotation::plain);

	/** The large thickener's rake torque, 0 to 1 of its rating: a fifth turning in its bed, the rest rising with the underflow backed up. */
	public static double torque(PlantBlockEntity part) {
		if (!part.running()) {
			return 0;
		}
		return 0.2 + 0.8 * Math.min(1, part.out().held(Product.TAILS_SLURRY) / PitFlotation.THICKENER_UNDERFLOW_HOLD);
	}
}
