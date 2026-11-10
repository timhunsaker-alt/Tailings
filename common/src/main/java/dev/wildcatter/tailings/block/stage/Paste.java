package dev.wildcatter.tailings.block.stage;

import dev.wildcatter.block.Lines;
import dev.wildcatter.block.PipeBlock;
import dev.wildcatter.fluid.Product;
import dev.wildcatter.fluid.SlurryAssay;
import dev.wildcatter.tailings.block.FillOutletBlockEntity;
import dev.wildcatter.tailings.block.PlantBlockEntity;
import dev.wildcatter.tailings.paste.PasteFill;
import dev.wildcatter.tailings.pit.DryStack;
import dev.wildcatter.tailings.plant.Machine;
import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

/**
 * The paste fill plant (docs/multiblocks/paste-backfill.md, L5): the vacuum disc filter on thickened tails, the twin-shaft mixer that
 * makes paste of its cake, a trim of thickened tails and cement, and the piston pump that pushes the paste down its steel fill line to a
 * fill outlet in a closed void. Paste goes on the pipe as tails slurry whose assay carries its solids and its binder, as thickened tails
 * carry their solids (Wildcatter 0.5.1 has no product for either).
 */
public final class Paste {
	private Paste() {
	}

	/** The mixer's paste hopper and the pump's feed hopper: a cubic metre (proposed). */
	static final double PASTE_HOLD = PasteFill.PASTE_KG_PER_M3;

	/** The paste's assay: 78 % solids, 5 % binder. */
	static SlurryAssay pasteAssay() {
		return SlurryAssay.of(Map.of(DryStack.SOLIDS_KEY, PasteFill.PASTE_SOLIDS, PasteFill.BINDER_KEY, PasteFill.BINDER));
	}

	static boolean isPaste(SlurryAssay assay) {
		return assay.get(PasteFill.BINDER_KEY) > 0;
	}

	/** Thickened tails: 60 % solids marked, and no binder. */
	static boolean thickened(SlurryAssay assay) {
		return !isPaste(assay) && DryStackStages.solids(assay) >= DryStack.UNDERFLOW_SOLIDS - 0.01;
	}

	/** The fault in a feed of thickened tails, or null if it is thickened tails. */
	static @Nullable String notThickened(SlurryAssay assay) {
		if (isPaste(assay)) {
			return "NOT THICKENED: that is paste; feed it a Tailings Thickener's underflow";
		}
		return thickened(assay) ? null : "NOT THICKENED: feed it a Tailings Thickener's underflow";
	}

	/** The vacuum disc filter: thickened tails in at its north face, its cake to the mixer touching it, filtrate back at its east face. */
	static final Stage FILTER = new Stage() {
		@Override
		public void tick(PlantBlockEntity part) {
			DryStackStages.returnWater(part);
			Flotation.draw(part, Product.TAILS_SLURRY);
			double waiting = part.back().held(Product.TAILS_SLURRY);
			if (waiting < 1e-6) {
				part.say("NO FEED");
				return;
			}
			SlurryAssay feed = part.back().assayOf(Product.TAILS_SLURRY);
			String fault = notThickened(feed);
			if (fault != null) {
				part.say(fault);
				return;
			}
			double room = PasteFill.CAKE_HOLD_KG - part.dryKg();
			if (room < 1) {
				part.say(String.format(Locale.ROOT, "FULL: %.1f t of cake waiting for a Paste Mixer touching it", part.dryKg() / 1000));
				return;
			}
			part.want();
			if (!part.running()) {
				part.say("waiting for its motor");
				return;
			}
			double solids = DryStackStages.solids(feed);
			double kg = Math.min(Math.min(PasteFill.filterKgPerTick() * part.strength() * part.fast(), waiting * solids), room);
			Flotation.use(part, Product.TAILS_SLURRY, kg / solids);
			part.setDryKg(part.dryKg() + kg);
			part.overflow(kg * (PasteFill.waterPerKgSolids(solids) - PasteFill.waterPerKgSolids(PasteFill.CAKE_SOLIDS)));
			part.say(String.format(Locale.ROOT, "filtering to %.0f %% solids: %.1f t of cake", PasteFill.CAKE_SOLIDS * 100, part.dryKg() / 1000));
		}
	};

	/**
	 * The paste mixer: cake from the filter touching it, thickened tails on pipe at its north face to trim it to 78 %, cement by its
	 * hopper (a click or a hopper on top); paste out at its south face.
	 */
	static final Stage MIXER = new Stage() {
		@Override
		public @Nullable Product outlet(PlantBlockEntity part, int[] at, Direction localSide) {
			return Flotation.backFace(part, at, localSide, -1) ? Product.TAILS_SLURRY : null;
		}

		@Override
		public void tick(PlantBlockEntity part) {
			Flotation.draw(part, Product.TAILS_SLURRY);
			PlantBlockEntity filter = part.touching(Machine.PASTE_FILTER);
			if (filter == null) {
				part.say("NOT BUILT: needs a Paste Filter touching it for its cake");
				return;
			}
			if (filter.dryKg() < 1e-6) {
				part.say("NO FEED: waiting for cake from the Paste Filter");
				return;
			}
			double trimWaiting = part.back().held(Product.TAILS_SLURRY);
			if (trimWaiting < 1e-6) {
				part.say("NO FEED: needs thickened tails on the pipe at its north face to trim the cake");
				return;
			}
			String fault = notThickened(part.back().assayOf(Product.TAILS_SLURRY));
			if (fault != null) {
				part.say(fault);
				return;
			}
			if (part.dryKg() < 1e-6) {
				part.say("NO CEMENT: put cement in its hopper");
				return;
			}
			double room = PASTE_HOLD - part.out().held(Product.TAILS_SLURRY);
			if (room < 1e-6) {
				part.say(Crushing.NOWHERE + " (paste on the pipe at its south side)");
				return;
			}
			part.want();
			if (!part.running()) {
				part.say("waiting for its motor");
				return;
			}
			double paste = Math.min(PasteFill.pumpKgPerTick() * part.strength() * part.fast(), room);
			double cake = PasteFill.cakeFor(paste);
			double trim = cake * PasteFill.trimPerKgCake();
			double cement = (cake + trim * PasteFill.UNDERFLOW_SOLIDS) * PasteFill.BINDER;
			double share = Math.min(1, Math.min(filter.dryKg() / cake, Math.min(trimWaiting / trim, part.dryKg() / cement)));
			filter.setDryKg(filter.dryKg() - cake * share);
			Flotation.use(part, Product.TAILS_SLURRY, trim * share);
			part.setDryKg(part.dryKg() - cement * share);
			part.out().offer(Product.TAILS_SLURRY, paste * share, pasteAssay());
			part.say(String.format(Locale.ROOT, "mixing paste at %.0f %% solids, %.0f %% cement; %.0f bags in the hopper", PasteFill.PASTE_SOLIDS * 100, PasteFill.BINDER * 100,
					Math.floor(part.dryKg() / PasteFill.CEMENT_BAG_KG)));
			part.setChanged();
		}
	};

	/** What a pump found on its fill line, read once a second. */
	public static final class Line {
		public final @Nullable BlockPos outlet;
		public final int pipes;
		/** The pump's discharge pressure while it pumps, Pa; 0 stopped. */
		public double pressure;

		Line(@Nullable BlockPos outlet, int pipes) {
			this.outlet = outlet;
			this.pipes = pipes;
		}
	}

	/** True for Wildcatter's steel pipe that passes flow (its valve, if any, open). */
	static boolean steel(Level level, BlockPos pos) {
		return level.getBlockState(pos).getBlock() instanceof PipeBlock pipe && pipe.kind() == PipeBlock.Kind.GAS && PipeBlock.conducts(level, pos);
	}

	/** Follows the steel pipe from the pump's south face to the first fill outlet on it, counting its metres (up to a limit). */
	static Line trace(PlantBlockEntity part) {
		Level level = part.getLevel();
		Set<BlockPos> seen = new HashSet<>();
		ArrayDeque<BlockPos> queue = new ArrayDeque<>();
		for (Lines.Face face : part.faces(at -> at[2] == part.machine().depth() - 1, Direction.SOUTH)) {
			if (steel(level, face.pipe()) && seen.add(face.pipe())) {
				queue.add(face.pipe());
			}
		}
		BlockPos outlet = null;
		while (!queue.isEmpty() && seen.size() <= PasteFill.MOST_LINE_PIPES) {
			BlockPos pipe = queue.poll();
			for (Direction d : Direction.values()) {
				BlockPos next = pipe.relative(d);
				if (seen.contains(next) || !level.isLoaded(next)) {
					continue;
				}
				if (outlet == null && level.getBlockEntity(next) instanceof FillOutletBlockEntity) {
					outlet = next;
				} else if (steel(level, next)) {
					seen.add(next);
					queue.add(next);
				}
			}
		}
		return new Line(outlet, seen.size());
	}

	/** A pump's line as last read, or null. */
	public static @Nullable Line line(PlantBlockEntity part) {
		return part.run instanceof Line line ? line : null;
	}

	/**
	 * The paste pump: paste in at its north face, pushed down the steel pipe at its south face to the fill outlet on it, while the outlet's
	 * void is closed and not full and the line's pressure is within its 8 MPa.
	 */
	static final Stage PUMP = new Stage() {
		@Override
		public void tick(PlantBlockEntity part) {
			Flotation.draw(part, Product.TAILS_SLURRY);
			if (part.run == null || part.scanDue()) {
				part.run = trace(part);
			}
			Line line = (Line) part.run;
			line.pressure = 0;
			double waiting = part.back().held(Product.TAILS_SLURRY);
			if (waiting < 1e-6) {
				part.say("NO FEED");
				return;
			}
			if (!isPaste(part.back().assayOf(Product.TAILS_SLURRY))) {
				part.say("NOT PASTE: feed it from a Paste Mixer");
				return;
			}
			if (line.outlet == null || !(part.getLevel().getBlockEntity(line.outlet) instanceof FillOutletBlockEntity outlet)) {
				part.say(line.pipes > PasteFill.MOST_LINE_PIPES ? String.format(Locale.ROOT, "LINE PRESSURE: no Fill Outlet within %d m of steel pipe", PasteFill.MOST_LINE_PIPES)
						: "NO OUTLET: no Fill Outlet on the steel pipe at its south side");
				return;
			}
			double drop = part.getBlockPos().getY() - line.outlet.getY();
			double pressure = PasteFill.linePressure(line.pipes, drop);
			if (!PasteFill.pumpable(line.pipes, drop)) {
				part.say(String.format(Locale.ROOT, "LINE PRESSURE: %.1f MPa to push %d m of line, over its %.0f MPa", pressure / 1e6, line.pipes, PasteFill.PUMP_MAX_PA / 1e6));
				return;
			}
			if (!outlet.accepting()) {
				part.say(outlet.status());
				return;
			}
			part.want();
			if (!part.running()) {
				part.say("waiting for its motor");
				return;
			}
			double kg = outlet.receive(Math.min(PasteFill.pumpKgPerTick() * part.strength() * part.fast(), waiting));
			if (kg <= 0) {
				part.say("the outlet's line is full");
				return;
			}
			Flotation.use(part, Product.TAILS_SLURRY, kg);
			part.countFlow(kg / PasteFill.PASTE_KG_PER_M3 * 1000);
			line.pressure = pressure;
			part.say(String.format(Locale.ROOT, "pumping %.0f m3/h at %.1f MPa through %d m of line; %s", PasteFill.PUMP_M3_PER_HOUR * part.strength(), pressure / 1e6, line.pipes,
					outlet.status()));
		}
	};

	/** The pump's FLOW, m3/h, from its litres a second. */
	public static double flowM3PerHour(PlantBlockEntity part) {
		return part.flow() * 3.6;
	}
}
