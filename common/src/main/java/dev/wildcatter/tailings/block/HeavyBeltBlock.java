package dev.wildcatter.tailings.block;

import dev.wildcatter.tailings.plant.Machine;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * One metre of the pit line's heavy belt (primary-gyratory-stockpile.md, P7a; P7-9): a 1,400 mm troughed belt at 4 m/s, up to 3,000
 * t/h, laid as the 600 mm belt is, driven by {@link Machine#HEAVY_CONVEYOR_DRIVE}s. It is drawn the full metre of its block wide: the
 * real 1.4 m does not fit one, as the vent duct's does not.
 */
public class HeavyBeltBlock extends BeltConveyorBlock {
	private static final VoxelShape X = Block.box(0, 0, 0, 16, 13, 16);
	private static final VoxelShape Z = Block.box(0, 0, 0, 16, 13, 16);

	public HeavyBeltBlock(Properties properties) {
		super(properties);
	}

	@Override
	public Machine drive() {
		return Machine.HEAVY_CONVEYOR_DRIVE;
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return state.getValue(AXIS) == Direction.Axis.X ? X : Z;
	}
}
