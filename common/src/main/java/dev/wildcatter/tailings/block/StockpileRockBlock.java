package dev.wildcatter.tailings.block;

import dev.wildcatter.tailings.pit.Loose;
import dev.wildcatter.tailings.rock.SizeClass;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.FallingBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/**
 * A block of the coarse-ore stockpile (primary-gyratory-stockpile.md, P7a; P7-7): the gyratory's crushed rock, dropped by the stockpile
 * head and settled by the rule, three quarters of a bank block each as muck is. Unlike muck it also rolls when it stands on the pile's
 * top steeper than the rule allows: when a reclaim feeder draws the block over its opening the column above falls into the gap and the
 * slopes round it roll in, so the pile drains as a cone over each feeder and the rest stands as the dead stockpile.
 */
public class StockpileRockBlock extends MuckBlock {
	public StockpileRockBlock(Properties properties) {
		super(properties);
	}

	@Override
	protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
		if (pos.getY() <= level.getMinY()) {
			return;
		}
		Map<String, Integer> held = level.getBlockEntity(pos) instanceof MuckBlockEntity muck ? Map.copyOf(muck.quarters()) : Map.of();
		if (FallingBlock.isFree(level.getBlockState(pos.below()))) {
			level.removeBlock(pos, false);
			Loose.muck(level, pos, state, held);
			stir(level, pos);
			return;
		}
		// only a block on the pile's top rolls: one with something on it is held by it
		if (!FallingBlock.isFree(level.getBlockState(pos.above()))) {
			return;
		}
		BlockPos to = Loose.wouldRoll(level, pos);
		if (!to.equals(pos)) {
			level.removeBlock(pos, false);
			level.setBlock(to, state, Block.UPDATE_ALL);
			if (level.getBlockEntity(to) instanceof MuckBlockEntity muck) {
				muck.set(held);
			}
			stir(level, pos);
		}
	}

	/**
	 * A cell of the pile at {@code gone} was emptied: the tops of the columns round it look again whether they now stand too steep, so
	 * the slopes roll in as the column over a feeder goes down (a block only hears its own neighbours, and the top of the next column
	 * stands above the cell that changed).
	 */
	public static void stir(ServerLevel level, BlockPos gone) {
		for (int dx = -1; dx <= 1; dx++) {
			for (int dz = -1; dz <= 1; dz++) {
				if (dx == 0 && dz == 0) {
					continue;
				}
				BlockPos at = gone.offset(dx, 0, dz);
				int up = 0;
				while (up < 64 && level.getBlockState(at.above()).getBlock() instanceof StockpileRockBlock) {
					at = at.above();
					up++;
				}
				BlockState there = level.getBlockState(at);
				if (there.getBlock() instanceof StockpileRockBlock rock) {
					level.scheduleTick(at, rock, 2);
				}
			}
		}
	}

	/** Broken by hand it drops its crushed rock, a quarter at a time by chance, as muck drops its rock. */
	@Override
	public void playerDestroy(ServerLevel server, ServerPlayer player, BlockPos pos, BlockState state, @Nullable BlockEntity entity, ItemStack tool) {
		if (!(entity instanceof MuckBlockEntity muck)) {
			return;
		}
		player.causeFoodExhaustion(0.005F);
		muck.quarters().forEach((id, q) -> {
			int whole = q / 4 + (server.getRandom().nextInt(4) < q % 4 ? 1 : 0);
			Feeds.Charge charge = Feeds.parse(Feeds.stack(id, 1));
			if (whole > 0 && charge != null) {
				Block.popResource(server, pos, Feeds.stack(SizeClass.CRUSHED.itemId(charge.rock()), whole));
			}
		});
	}
}
