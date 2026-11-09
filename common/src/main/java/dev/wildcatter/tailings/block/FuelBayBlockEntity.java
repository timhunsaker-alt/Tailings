package dev.wildcatter.tailings.block;

import dev.wildcatter.energy.Inspectable;
import dev.wildcatter.fluid.Product;
import dev.wildcatter.multiblock.BigPartEntity;
import dev.wildcatter.tailings.TailingsBlocks;
import dev.wildcatter.tailings.entity.PitVehicleEntity;
import dev.wildcatter.tailings.pit.PitFuel;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * The fuel bay (docs/multiblocks/open-pit.md, P2a): a diesel dispenser island with a 2,000 L day tank, filled by Wildcatter pipe on any
 * face. It fills a stopped haul truck or loader within 8 m at 600 L a minute, one at a time.
 */
public class FuelBayBlockEntity extends BlockEntity implements BigPartEntity, Inspectable {
	private double litres;
	private boolean grown;
	private int pullIn;
	private String status = "idle";

	public FuelBayBlockEntity(BlockPos pos, BlockState state) {
		super(TailingsBlocks.FUEL_BAY_ENTITY, pos, state);
	}

	public double litres() {
		return litres;
	}

	/** Test hook. */
	public void setLitres(double value) {
		litres = Math.max(0, Math.min(PitFuel.BAY_TANK_L, value));
		setChanged();
	}

	public String status() {
		return status;
	}

	public static void tick(Level level, BlockPos pos, BlockState state, FuelBayBlockEntity bay) {
		if (level instanceof ServerLevel server) {
			bay.serverTick(server, state);
		}
	}

	private void serverTick(ServerLevel level, BlockState state) {
		if (pullIn-- <= 0 && litres < PitFuel.BAY_TANK_L - 1) {
			pullIn = 20;
			double got = dev.wildcatter.block.Lines.pull(level, PartFrame.all(getBlockPos(), state), Product.DIESEL, PitFuel.BAY_TANK_L - litres,
					dev.wildcatter.block.FluidNode.Pull.MACHINE, this);
			if (got > 0) {
				litres += got;
				setChanged();
			}
		}
		PitVehicleEntity vehicle = nearest(level, state);
		if (vehicle == null) {
			status = litres < 1 ? "EMPTY: pipe diesel to it" : "idle";
			return;
		}
		if (litres < 1) {
			status = "EMPTY: pipe diesel to it";
			return;
		}
		double give = Math.min(PitFuel.BAY_LITRES_PER_SECOND / 20.0, Math.min(litres, vehicle.tank() - vehicle.fuel()));
		vehicle.addFuel(give);
		litres -= give;
		status = String.format(java.util.Locale.ROOT, "filling: %.0f of %.0f L", vehicle.fuel(), vehicle.tank());
		setChanged();
	}

	/** The nearest stopped machine within reach whose tank is not full. */
	private @Nullable PitVehicleEntity nearest(ServerLevel level, BlockState state) {
		Vec3 centre = centre(state);
		PitVehicleEntity best = null;
		double bestDistance = Double.MAX_VALUE;
		for (PitVehicleEntity v : level.getEntitiesOfClass(PitVehicleEntity.class, new AABB(centre, centre).inflate(PitFuel.BAY_REACH + 8))) {
			if (Math.abs(v.speed()) > 0.05 || v.fuel() >= v.tank() - 0.01) {
				continue;
			}
			double d = distance(v, centre);
			if (d <= PitFuel.BAY_REACH && d < bestDistance) {
				bestDistance = d;
				best = v;
			}
		}
		return best;
	}

	/** From the bay's centre to the nearest point of the machine's box, on the ground. */
	private static double distance(PitVehicleEntity v, Vec3 centre) {
		AABB box = v.getBoundingBox();
		double dx = Math.max(0, Math.max(box.minX - centre.x, centre.x - box.maxX));
		double dz = Math.max(0, Math.max(box.minZ - centre.z, centre.z - box.maxZ));
		return Math.hypot(dx, dz);
	}

	private Vec3 centre(BlockState state) {
		double x = 0, y = 0, z = 0;
		var cells = dev.wildcatter.multiblock.BigParts.cells(getBlockPos(), state);
		for (BlockPos cell : cells) {
			x += cell.getX() + 0.5;
			y += cell.getY();
			z += cell.getZ() + 0.5;
		}
		int n = Math.max(1, cells.size());
		return new Vec3(x / n, y / n, z / n);
	}

	/** Whether a machine at {@code at} is in the hose's reach of a bay; for the truck to know it is at the bay. */
	public boolean reaches(PitVehicleEntity v) {
		return distance(v, centre(getBlockState())) <= PitFuel.BAY_REACH;
	}

	@Override
	public Component inspect() {
		return Component.literal(String.format(java.util.Locale.ROOT, "Fuel Bay: %.0f of %.0f L of diesel; %s", litres, PitFuel.BAY_TANK_L, status));
	}

	@Override
	public boolean grown() {
		return grown;
	}

	@Override
	public void markGrown() {
		grown = true;
		setChanged();
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		output.putBoolean("grown", grown);
		output.putDouble("litres", litres);
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		grown = input.getBooleanOr("grown", false);
		litres = input.getDoubleOr("litres", 0);
	}
}
