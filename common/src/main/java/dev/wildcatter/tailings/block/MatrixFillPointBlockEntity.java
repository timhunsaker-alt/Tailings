package dev.wildcatter.tailings.block;

import dev.wildcatter.energy.Inspectable;
import dev.wildcatter.fluid.Product;
import dev.wildcatter.multiblock.BigPartEntity;
import dev.wildcatter.tailings.TailingsBlocks;
import dev.wildcatter.tailings.entity.EmulsionTruckEntity;
import dev.wildcatter.tailings.pit.Blasting;
import java.util.Locale;
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
 * The matrix fill point (open-pit-p3-blasting.md, decision P3-9): a standpipe and hose that takes Wildcatter's emulsion matrix by pipe
 * on any face, from a stock tank or IBC, and fills an emulsion truck stopped within 6 m at 1 t a minute, as the fuel bay fills diesel.
 * It holds what is in its standpipe and hose, drawing from the pipe as the truck takes it.
 */
public class MatrixFillPointBlockEntity extends BlockEntity implements BigPartEntity, Inspectable {
	/** What it draws from the line ahead of the truck, kg: a minute's fill. */
	private static final double HOLD_KG = 1_000;
	private double kg;
	private boolean grown;
	private int pullIn;
	private String status = "idle";

	public MatrixFillPointBlockEntity(BlockPos pos, BlockState state) {
		super(TailingsBlocks.MATRIX_FILL_POINT_ENTITY, pos, state);
	}

	public double kg() {
		return kg;
	}

	public String status() {
		return status;
	}

	public static void tick(Level level, BlockPos pos, BlockState state, MatrixFillPointBlockEntity point) {
		if (level instanceof ServerLevel server) {
			point.serverTick(server, state);
		}
	}

	private void serverTick(ServerLevel level, BlockState state) {
		if (pullIn-- <= 0 && kg < HOLD_KG - 1) {
			pullIn = 10;
			double got = dev.wildcatter.block.Lines.pull(level, PartFrame.all(getBlockPos(), state), Product.EMULSION_MATRIX, HOLD_KG - kg,
					dev.wildcatter.block.FluidNode.Pull.MACHINE, this);
			if (got > 0) {
				kg += got;
				setChanged();
			}
		}
		EmulsionTruckEntity truck = nearest(level);
		if (truck == null) {
			status = kg < 1 ? "EMPTY: pipe emulsion matrix to it" : "idle";
			return;
		}
		if (kg < 1) {
			status = "EMPTY: pipe emulsion matrix to it";
			return;
		}
		double took = truck.fill(Math.min(Blasting.FILL_KG_PER_TICK, kg));
		kg -= took;
		status = String.format(Locale.ROOT, "filling: %.0f of %.0f kg", truck.matrixKg(), Blasting.TRUCK_MATRIX_KG);
		setChanged();
	}

	/** The nearest stopped emulsion truck within 6 m whose tank is not full. */
	private @Nullable EmulsionTruckEntity nearest(ServerLevel level) {
		Vec3 at = Vec3.atBottomCenterOf(getBlockPos());
		EmulsionTruckEntity best = null;
		double bestDistance = Double.MAX_VALUE;
		for (EmulsionTruckEntity truck : level.getEntitiesOfClass(EmulsionTruckEntity.class, new AABB(at, at).inflate(Blasting.FILL_REACH + 10))) {
			if (Math.abs(truck.speed()) > 0.05 || truck.matrixKg() >= Blasting.TRUCK_MATRIX_KG - 0.01) {
				continue;
			}
			AABB box = truck.getBoundingBox();
			double d = Math.hypot(Math.max(0, Math.max(box.minX - at.x, at.x - box.maxX)), Math.max(0, Math.max(box.minZ - at.z, at.z - box.maxZ)));
			if (d <= Blasting.FILL_REACH && d < bestDistance) {
				bestDistance = d;
				best = truck;
			}
		}
		return best;
	}

	@Override
	public Component inspect() {
		return Component.literal(String.format(Locale.ROOT, "Matrix Fill Point: %.0f kg of emulsion matrix in the standpipe; %s", kg, status));
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
		output.putDouble("kg", kg);
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		grown = input.getBooleanOr("grown", false);
		kg = input.getDoubleOr("kg", 0);
	}
}
