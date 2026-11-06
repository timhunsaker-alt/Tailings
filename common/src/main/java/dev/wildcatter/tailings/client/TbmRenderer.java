package dev.wildcatter.tailings.client;

import dev.wildcatter.tailings.block.TbmMachine;
import dev.wildcatter.tailings.entity.TbmEntity;
import java.util.List;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.core.Direction;

/** Draws the running machine, every part's cells where they stand in it. */
public class TbmRenderer extends PitchedCellsRenderer<TbmEntity> {
	public TbmRenderer(EntityRendererProvider.Context context) {
		super(context);
	}

	@Override
	protected List<TbmMachine.Cell> cells(TbmEntity entity) {
		return TbmMachine.cells(entity.facing());
	}

	@Override
	protected Direction facing(TbmEntity entity) {
		return entity.facing();
	}

	@Override
	protected float pitch(TbmEntity entity) {
		return entity.pitch();
	}
}
