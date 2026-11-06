package dev.wildcatter.tailings.client;

import dev.wildcatter.tailings.block.TbmMachine;
import dev.wildcatter.tailings.entity.GantryEntity;
import java.util.List;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.core.Direction;

/** Draws a back-up gantry, its frame's cells as they stand. */
public class GantryRenderer extends PitchedCellsRenderer<GantryEntity> {
	public GantryRenderer(EntityRendererProvider.Context context) {
		super(context);
	}

	@Override
	protected List<TbmMachine.Cell> cells(GantryEntity entity) {
		return TbmMachine.gantryCells(entity.kind(), entity.facing());
	}

	@Override
	protected Direction facing(GantryEntity entity) {
		return entity.facing();
	}

	@Override
	protected float pitch(GantryEntity entity) {
		return entity.pitch();
	}
}
