package dev.wildcatter.tailings.block;

import dev.wildcatter.tailings.TailingsBlocks;
import dev.wildcatter.tailings.TailingsItems;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

/**
 * The blasting machine (open-pit-p3-blasting.md): a box on a stand. Click it with the logger to load the shot, with the key to put the
 * key in; with an empty hand, ARM, then FIRE; sneak with an empty hand to take the key out.
 */
public class BlastingMachineBlock extends BaseEntityBlock {
	private static final VoxelShape SHAPE = Block.box(3, 0, 3, 13, 12, 13);

	public BlastingMachineBlock(Properties properties) {
		super(properties);
	}


	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return SHAPE;
	}

	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new BlastingMachineBlockEntity(pos, state);
	}

	@Override
	protected RenderShape getRenderShape(BlockState state) {
		return RenderShape.MODEL;
	}

	@Override
	public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
		return level.isClientSide() ? null : createTickerHelper(type, TailingsBlocks.BLASTING_MACHINE_ENTITY, BlastingMachineBlockEntity::tick);
	}

	@Override
	protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
		if (!(level.getBlockEntity(pos) instanceof BlastingMachineBlockEntity machine)) {
			return InteractionResult.PASS;
		}
		if (stack.is(TailingsItems.BLAST_LOGGER)) {
			if (!level.isClientSide()) {
				tell(player, machine.load(stack));
			}
			return InteractionResult.SUCCESS;
		}
		if (stack.is(TailingsItems.BLASTING_KEY)) {
			if (!level.isClientSide()) {
				if (machine.insertKey()) {
					if (!player.getAbilities().instabuild) {
						stack.shrink(1);
					}
					tell(player, "key in");
				} else {
					tell(player, "its key is in");
				}
			}
			return InteractionResult.SUCCESS;
		}
		return stack.isEmpty() ? InteractionResult.TRY_WITH_EMPTY_HAND : InteractionResult.PASS;
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		if (level.isClientSide() || !(level.getBlockEntity(pos) instanceof BlastingMachineBlockEntity machine)) {
			return InteractionResult.SUCCESS;
		}
		if (player.isShiftKeyDown()) {
			if (machine.takeKey()) {
				player.getInventory().placeItemBackInInventory(new ItemStack(TailingsItems.BLASTING_KEY), net.minecraft.util.Prediction.SERVER_ONLY);
				tell(player, "key out");
			} else {
				player.sendSystemMessage(machine.inspect());
			}
			return InteractionResult.SUCCESS;
		}
		tell(player, machine.armed() ? machine.fire() : machine.arm());
		return InteractionResult.SUCCESS;
	}

	private static void tell(Player player, String what) {
		player.sendSystemMessage(Component.literal("Blasting Machine: " + what));
	}
}
