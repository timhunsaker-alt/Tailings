package dev.wildcatter.tailings.block;

import dev.wildcatter.tailings.TailingsItems;
import java.util.Locale;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;

/**
 * A blasthole (docs/multiblocks/open-pit-p3-blasting.md): a column of these from the collar down, each remembering the rock it
 * replaced. The collar carries the hole: its state, its cuttings, its charge. Its tag is coloured by state (a placeholder): white
 * drilled, yellow primed, orange charged, grey stemmed, green logged, red misfired.
 *
 * <p>On the collar: a cast booster or a detonator primes it; gravel stems it 13 m a block; an empty hand shovels its own cuttings in as
 * stemming; a water bucket washes the charge out (not once stemmed, unless it misfired); sneak with an empty hand reads it.
 */
public class BlastholeBlock extends BaseEntityBlock {
	public enum Stage implements StringRepresentable {
		DRILLED, PRIMED, CHARGED, STEMMED, LOGGED, MISFIRED;

		@Override
		public String getSerializedName() {
			return name().toLowerCase(Locale.ROOT);
		}
	}

	public static final EnumProperty<Stage> STAGE = EnumProperty.create("stage", Stage.class);
	public static final BooleanProperty COLLAR = BooleanProperty.create("collar");

	public BlastholeBlock(Properties properties) {
		super(properties);
		registerDefaultState(stateDefinition.any().setValue(STAGE, Stage.DRILLED).setValue(COLLAR, false));
	}


	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(STAGE, COLLAR);
	}

	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new BlastholeBlockEntity(pos, state);
	}

	@Override
	protected RenderShape getRenderShape(BlockState state) {
		return RenderShape.MODEL;
	}

	@Override
	protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
		if (!state.getValue(COLLAR) || !(level.getBlockEntity(pos) instanceof BlastholeBlockEntity hole)) {
			return InteractionResult.TRY_WITH_EMPTY_HAND;
		}
		if (level.isClientSide()) {
			return stack.is(TailingsItems.CAST_BOOSTER) || stack.is(TailingsItems.DETONATOR) || stack.is(Items.GRAVEL) || stack.is(Items.WATER_BUCKET)
					? InteractionResult.SUCCESS : stack.isEmpty() ? InteractionResult.TRY_WITH_EMPTY_HAND : InteractionResult.PASS;
		}
		BlastholeBlockEntity.Said said;
		if (stack.is(TailingsItems.CAST_BOOSTER)) {
			said = hole.addBooster();
		} else if (stack.is(TailingsItems.DETONATOR)) {
			said = hole.addDetonator(level.getRandom().nextInt(1, 1_000_000));
		} else if (stack.is(Items.GRAVEL)) {
			said = hole.stemWithGravel();
		} else if (stack.is(Items.WATER_BUCKET)) {
			said = hole.washOut(level, player);
			if (said.took() && !player.getAbilities().instabuild) {
				player.setItemInHand(hand, new ItemStack(Items.BUCKET));
			}
			tell(level, player, said.text());
			return InteractionResult.SUCCESS;
		} else {
			return stack.isEmpty() ? InteractionResult.TRY_WITH_EMPTY_HAND : InteractionResult.PASS;
		}
		if (said.took() && !player.getAbilities().instabuild) {
			stack.shrink(1);
		}
		tell(level, player, said.text());
		return InteractionResult.SUCCESS;
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		if (!(level.getBlockEntity(pos) instanceof BlastholeBlockEntity hole)) {
			return InteractionResult.PASS;
		}
		BlastholeBlockEntity collar = hole.collarEntity();
		if (collar == null) {
			return InteractionResult.PASS;
		}
		if (!level.isClientSide()) {
			tell(level, player, player.isShiftKeyDown() || !state.getValue(COLLAR) ? collar.readout() : collar.stemWithCuttings().text());
		}
		return InteractionResult.SUCCESS;
	}

	private static void tell(Level level, Player player, String said) {
		if (!level.isClientSide()) {
			player.sendOverlayMessage(Component.literal("Blasthole: " + said));
		}
	}

	/** Mined by hand, it gives the rock it replaced, as that rock would. */
	@Override
	public void playerDestroy(ServerLevel server, ServerPlayer player, BlockPos pos, BlockState state, @Nullable BlockEntity entity, ItemStack tool) {
		super.playerDestroy(server, player, pos, state, entity, tool);
		if (entity instanceof BlastholeBlockEntity hole) {
			Block rock = BuiltInRegistries.BLOCK.getValue(Identifier.parse(hole.replaced()));
			if (rock != null && rock.asItem() != Items.AIR) {
				Block.popResource(server, pos, new ItemStack(rock.asItem()));
			}
		}
	}
}
