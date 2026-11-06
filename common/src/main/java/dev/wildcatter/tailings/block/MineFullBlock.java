package dev.wildcatter.tailings.block;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;

/** A full floor block or a landing, cut from mined rock. */
public class MineFullBlock extends Block {
	public static final EnumProperty<FloorLook> LOOK = EnumProperty.create("look", FloorLook.class, look -> look != FloorLook.BALLAST);

	public MineFullBlock(Properties properties) {
		super(properties);
		registerDefaultState(stateDefinition.any().setValue(LOOK, FloorLook.STONE));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(LOOK);
	}
}
