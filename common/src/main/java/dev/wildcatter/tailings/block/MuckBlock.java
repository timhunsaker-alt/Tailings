package dev.wildcatter.tailings.block;

import dev.wildcatter.tailings.pit.Loose;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.FallingBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;

/**
 * Muck: broken rock (docs/multiblocks/open-pit.md). It falls like gravel and settles by the settle rule, carrying what it holds; a
 * bucket digs it as it lies. Broken by hand it drops the rock it holds, a quarter at a time by chance.
 */
public class MuckBlock extends BaseEntityBlock {
	public MuckBlock(Properties properties) {
		super(properties);
	}

	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new MuckBlockEntity(pos, state);
	}

	@Override
	protected RenderShape getRenderShape(BlockState state) {
		return RenderShape.MODEL;
	}

	@Override
	protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
		level.scheduleTick(pos, this, 2);
	}

	@Override
	protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos, Direction toNeighbour,
			BlockPos neighbourPos, BlockState neighbourState, RandomSource random) {
		ticks.scheduleTick(pos, this, 2);
		return super.updateShape(state, level, ticks, pos, toNeighbour, neighbourPos, neighbourState, random);
	}

	/** The ground went from under it: it settles again, by the rule, with what it holds. */
	@Override
	protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
		if (!FallingBlock.isFree(level.getBlockState(pos.below())) || pos.getY() <= level.getMinY()) {
			return;
		}
		Map<String, Integer> held = level.getBlockEntity(pos) instanceof MuckBlockEntity muck ? Map.copyOf(muck.quarters()) : Map.of();
		level.removeBlock(pos, false);
		Loose.muck(level, pos, held);
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		if (!level.isClientSide() && level.getBlockEntity(pos) instanceof MuckBlockEntity muck) {
			player.sendOverlayMessage(muck.inspect());
		}
		return InteractionResult.SUCCESS;
	}

	@Override
	public void playerDestroy(ServerLevel server, ServerPlayer player, BlockPos pos, BlockState state, @Nullable BlockEntity entity, ItemStack tool) {
		super.playerDestroy(server, player, pos, state, entity, tool);
		if (entity instanceof MuckBlockEntity muck) {
			muck.quarters().forEach((id, q) -> {
				int whole = q / 4 + (server.getRandom().nextInt(4) < q % 4 ? 1 : 0);
				if (whole > 0) {
					Block.popResource(server, pos, Feeds.stack(id, whole));
				}
			});
		}
	}
}
