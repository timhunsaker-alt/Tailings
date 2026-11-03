package dev.wildcatter.tailings.block;

import dev.wildcatter.tailings.TailingsBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

/** A tunnel lamp. It lights while a neighboring mine rail is fed by the rectifier, and it never shocks. */
public class MineLightBlock extends Block {
	public static final BooleanProperty LIT = BooleanProperty.create("lit");

	public MineLightBlock(Properties properties) {
		super(properties);
		registerDefaultState(stateDefinition.any().setValue(LIT, false));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(LIT);
	}

	public static void refresh(Level level, BlockPos pos, BlockState state) {
		boolean lit = false;
		for (Direction direction : Direction.values()) {
			BlockEntity entity = level.getBlockEntity(pos.relative(direction));
			if (entity instanceof RailBlockEntity rail && rail.conductorLive()) {
				lit = true;
			}
		}
		if (state.getValue(LIT) != lit && state.getBlock() == TailingsBlocks.MINE_LIGHT) {
			level.setBlock(pos, state.setValue(LIT, lit), 3);
		}
	}
}
