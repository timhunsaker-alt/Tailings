package dev.wildcatter.tailings.block.stage;

import dev.wildcatter.block.Lines;
import dev.wildcatter.block.Plumbing;
import dev.wildcatter.block.Slurry;
import dev.wildcatter.block.WaterUser;
import dev.wildcatter.fluid.Product;
import dev.wildcatter.fluid.SlurryAssay;
import dev.wildcatter.tailings.TailingsBlocks;
import dev.wildcatter.tailings.block.PlantBlockEntity;
import dev.wildcatter.tailings.entity.HaulTruckEntity;
import dev.wildcatter.tailings.pit.DryStack;
import dev.wildcatter.tailings.pit.Loose;
import dev.wildcatter.tailings.pit.PitMass;
import dev.wildcatter.tailings.plant.Machine;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * The dry stack (docs/multiblocks/open-pit-p4-dry-stack.md): the tailings thickener, the tailings filter's batch press, and the truck
 * load-out's bin and gate.
 */
final class DryStackStages {
	private DryStackStages() {
	}

	/** Thickened tails the thickener's cone holds for the press while it opens: 20 t of slurry (proposed). */
	static final double UNDERFLOW_HOLD = 20_000;

	/** The solids of a tails slurry, from its assay: thickened tails carry their own; plain tails are the plant's 35 %. */
	static double solids(SlurryAssay assay) {
		double marked = assay.get(DryStack.SOLIDS_KEY);
		return marked > 0 ? marked : DryStack.FEED_SOLIDS;
	}

	/** Once a second, water waiting goes back to the water users on a water pipe at the part's east face; with none, it is lost. */
	static void returnWater(PlantBlockEntity part) {
		if (!part.scanDue() || part.waterOut() < 1) {
			return;
		}
		List<BlockPos> cells = new ArrayList<>();
		for (Lines.Face face : part.side(Direction.EAST)) {
			cells.add(face.cell());
		}
		Set<BlockPos> pipes = Plumbing.network(part.getLevel(), cells);
		int budget = (int) part.waterOut();
		int moved = 0;
		for (BlockEntity user : Plumbing.machines(part.getLevel(), cells, pipes, false, BlockEntity.class)) {
			if (user != part && user instanceof WaterUser water && moved < budget) {
				moved += water.addWater(budget - moved);
			}
		}
		if (moved > 0) {
			part.returned(moved, false);
		} else {
			part.returned(part.waterOut(), true);   // nowhere to go: counted as lost, as built mills do
		}
	}

	/** The tailings thickener: tails slurry in at its north face, 60 % underflow at its south face, overflow water at its east. */
	static final Stage THICKENER = new Stage() {
		@Override
		public @Nullable Product outlet(PlantBlockEntity part, int[] at, Direction localSide) {
			return Flotation.backFace(part, at, localSide, -1) ? Product.TAILS_SLURRY : null;
		}

		@Override
		public void tick(PlantBlockEntity part) {
			returnWater(part);
			Flotation.draw(part, Product.TAILS_SLURRY);
			double waiting = part.back().held(Product.TAILS_SLURRY);
			if (waiting < 1e-6) {
				part.say("NO FEED");
				return;
			}
			part.want();
			if (!part.running()) {
				part.say("waiting for its motor");
				return;
			}
			SlurryAssay feed = part.back().assayOf(Product.TAILS_SLURRY);
			double ratio = Math.min(1, solids(feed) / DryStack.UNDERFLOW_SOLIDS);
			double room = Math.max(0, UNDERFLOW_HOLD - part.out().held(Product.TAILS_SLURRY));
			double max = Math.min(Math.min(Flotation.FEED_PER_TICK * part.strength(), waiting), room / ratio);
			if (max < 1e-6) {
				part.say(Crushing.NOWHERE + " (thickened tails on the pipe at its south side)");
				return;
			}
			Slurry.Taken taken = Flotation.use(part, Product.TAILS_SLURRY, max);
			Map<String, Double> shares = new HashMap<>();
			for (String id : taken.assay().minerals()) {
				if (!id.equals(DryStack.SOLIDS_KEY)) {
					shares.put(id, taken.assay().get(id) / ratio);
				}
			}
			shares.put(DryStack.SOLIDS_KEY, DryStack.UNDERFLOW_SOLIDS);
			part.out().offer(Product.TAILS_SLURRY, taken.kg() * ratio, SlurryAssay.of(shares));
			part.overflow(taken.kg() * (1 - ratio));   // the overflow: the water the underflow no longer carries
			part.say("thickening");
			part.setChanged();
		}
	};

	/** The press's cycle in ticks, for a part (a game test runs it faster). */
	static double cycleTicks(PlantBlockEntity part) {
		return DryStack.CYCLE_SECONDS * 20.0 / part.fast();
	}

	/** The tailings filter: a batch press on thickened tails, dropping its cake each cycle. */
	static final Stage FILTER = new Stage() {
		@Override
		public void tick(PlantBlockEntity part) {
			returnWater(part);
			if (part.cakeOut() > 0) {
				drop(part);
				if (part.cakeOut() > 0) {
					part.say("FULL: nowhere to drop the cake (a load-out bin, a container or open ground under its chute)");
					return;
				}
			}
			Flotation.draw(part, Product.TAILS_SLURRY);
			double waiting = part.back().held(Product.TAILS_SLURRY);
			if (waiting < 1e-6 && part.dryKg() <= 0) {
				part.say("NO FEED");
				return;
			}
			if (waiting > 1e-6 && solids(part.back().assayOf(Product.TAILS_SLURRY)) < DryStack.UNDERFLOW_SOLIDS - 0.01) {
				part.say("NOT THICKENED: feed it from a Tailings Thickener's underflow");
				return;
			}
			part.want();
			if (!part.running()) {
				part.say("waiting for its motor");
				return;
			}
			double ticks = cycleTicks(part);
			double seconds = part.cycle() / ticks * DryStack.CYCLE_SECONDS;
			double pressing = (DryStack.FILL_SECONDS + DryStack.SQUEEZE_SECONDS + DryStack.BLOW_SECONDS) / (double) DryStack.CYCLE_SECONDS * ticks;
			if (part.cycle() < pressing && waiting > 1e-6) {
				// the chambers fill to the batch over the pressing time: 15 t in 7.5 minutes
				double perTick = DryStack.BATCH_KG / pressing * part.strength();
				double solidsKg = Math.min(perTick, Math.max(0, DryStack.BATCH_KG - part.dryKg()));
				double slurry = Math.min(waiting, solidsKg / DryStack.UNDERFLOW_SOLIDS);
				if (slurry > 1e-6) {
					Flotation.use(part, Product.TAILS_SLURRY, slurry);
					double dry = slurry * DryStack.UNDERFLOW_SOLIDS;
					part.setDryKg(part.dryKg() + dry);
					part.overflow(dry * DryStack.filtratePerKgSolids());
				}
			}
			if (part.dryKg() <= 0) {
				part.say("waiting for thickened tails");
				return;
			}
			part.setCycle(part.cycle() + part.strength());
			part.say(DryStack.phase(seconds) + String.format(java.util.Locale.ROOT, ": %.1f t in the press", part.dryKg() / 1000));
			if (part.cycle() >= ticks) {
				part.setCakeOut(part.cakeOut() + part.dryKg());
				part.setDryKg(0);
				part.setCycle(0);
				part.countCycle();
				BlockPos chute = chute(part);
				part.getLevel().playSound(null, chute, dev.wildcatter.tailings.TailingsSounds.MUCK_TIP, SoundSource.BLOCKS, 3F, 0.6F);
				drop(part);
			}
		}
	};

	/** The cell at the centre of the filter's bottom, where its cake chute is. */
	static BlockPos chute(PlantBlockEntity part) {
		List<BlockPos> cells = part.cells();
		int lowest = Integer.MAX_VALUE;
		double cx = 0, cz = 0;
		for (BlockPos c : cells) {
			lowest = Math.min(lowest, c.getY());
			cx += c.getX();
			cz += c.getZ();
		}
		cx /= cells.size();
		cz /= cells.size();
		BlockPos best = part.getBlockPos();
		double bestD = Double.MAX_VALUE;
		for (BlockPos c : cells) {
			if (c.getY() == lowest) {
				double d = Math.hypot(c.getX() - cx, c.getZ() - cz);
				if (d < bestD) {
					bestD = d;
					best = c;
				}
			}
		}
		return best;
	}

	/** Drops the waiting cake: into a load-out bin under it, a container under its chute, or onto open ground under its chute. */
	static void drop(PlantBlockEntity part) {
		PlantBlockEntity bin = part.under(Machine.LOADOUT_BIN);
		if (bin != null) {
			double move = Math.min(part.cakeOut(), DryStack.BIN_KG - bin.dryKg());
			if (move > 0) {
				bin.setDryKg(bin.dryKg() + move);
				part.setCakeOut(part.cakeOut() - move);
			}
			return;
		}
		BlockPos below = chute(part).below();
		if (part.getLevel().getBlockEntity(below) instanceof Container container) {
			while (part.cakeOut() >= PitMass.DRY_TAILS_KG) {
				ItemStack left = HopperBlockEntity.addItem(null, container, new ItemStack(TailingsBlocks.DRY_TAILS), Direction.UP);
				if (!left.isEmpty()) {
					return;
				}
				part.setCakeOut(part.cakeOut() - PitMass.DRY_TAILS_KG);
			}
			return;
		}
		if (part.getLevel() instanceof ServerLevel server && part.getLevel().getBlockState(below).isAir()) {
			while (part.cakeOut() >= PitMass.DRY_TAILS_KG) {
				if (Loose.block(server, below, TailingsBlocks.DRY_TAILS.defaultBlockState()) == null) {
					return;
				}
				part.setCakeOut(part.cakeOut() - PitMass.DRY_TAILS_KG);
			}
		}
	}

	/** The load-out bin: holds dry tails for the gate under it; takes them by hand, hopper, or from a filter over it. */
	static final Stage BIN = new Stage() {
		@Override
		public void tick(PlantBlockEntity part) {
			if (part.scanDue() && !part.status().startsWith("dry tails only")) {
				part.say(part.dryKg() >= DryStack.BIN_KG - PitMass.DRY_TAILS_KG ? "FULL" : String.format(java.util.Locale.ROOT, "holds %.1f t", part.dryKg() / 1000));
			}
		}
	};

	/** Where the gate is: the centre of the frame's lane, at its floor. */
	static Vec3 gate(PlantBlockEntity part) {
		return part.loadoutGate();
	}

	/** The truck stopped with its body under the gate, with room, or null. */
	static @Nullable HaulTruckEntity truckUnder(PlantBlockEntity part) {
		Vec3 gate = gate(part);
		for (HaulTruckEntity truck : part.getLevel().getEntitiesOfClass(HaulTruckEntity.class, new AABB(gate, gate).inflate(12, 4, 12))) {
			Vec3 body = truck.bodyCentre();
			if (Math.hypot(body.x - gate.x, body.z - gate.z) <= DryStack.GATE_REACH && Math.abs(truck.speed()) < 0.1 && !truck.busy() && truck.load().room() > 0) {
				return truck;
			}
		}
		return null;
	}

	/** The frame's clamshell gate: fills the truck standing under it at 2 t/s from the bin on the frame. */
	static final Stage FRAME = new Stage() {
		@Override
		public void tick(PlantBlockEntity part) {
			PlantBlockEntity bin = part.touching(Machine.LOADOUT_BIN);
			if (bin == null) {
				part.say("NOT BUILT: needs a Load-Out Bin on it");
				return;
			}
			HaulTruckEntity truck = truckUnder(part);
			if (truck == null) {
				part.say(String.format(java.util.Locale.ROOT, "waiting for a truck: %.1f t in the bin", bin.dryKg() / 1000));
				return;
			}
			if (bin.dryKg() < 1) {
				part.say("the bin is empty");
				return;
			}
			part.want();
			if (!part.running()) {
				part.say("waiting for its motor");
				return;
			}
			double move = Math.min(Math.min(DryStack.LOADOUT_KG_PER_SECOND / 20 * part.strength(), truck.load().room()), bin.dryKg());
			truck.load().add(PitMass.DRY_TAILS, Math.round(move));
			truck.refreshLoad();
			bin.setDryKg(bin.dryKg() - Math.round(move));
			part.say("loading: the truck holds " + truck.load().tonnes());
			if (part.getLevel().getGameTime() % 40 == 0) {
				part.getLevel().playSound(null, truck.getX(), truck.getY(), truck.getZ(), dev.wildcatter.tailings.TailingsSounds.MUCK_LOAD, SoundSource.BLOCKS, 2F, 1.0F);
			}
		}
	};
}
