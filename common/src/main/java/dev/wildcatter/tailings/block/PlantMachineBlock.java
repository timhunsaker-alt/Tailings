package dev.wildcatter.tailings.block;

import dev.wildcatter.multiblock.Footprint;
import dev.wildcatter.tailings.TailingsBlocks;
import dev.wildcatter.tailings.plant.Machine;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;

/**
 * One true-size part of the plant or the dump station, on Wildcatter's big parts. Piece 0 is the main cell and holds the block
 * entity; a click on any piece is the main cell's. The dump pocket's main cell is a {@link DumpBlockEntity}; every other part's
 * is a {@link PlantBlockEntity}.
 */
public class PlantMachineBlock extends TailingsPartBlock {
	public static final EnumProperty<Direction> FACING = TailingsPartBlock.FACING;

	private final Machine machine;

	public PlantMachineBlock(Properties properties, Machine machine) {
		super(properties, Footprint.box(machine.width(), machine.height(), machine.depth(), 0, 0), machine.id());
		this.machine = machine;
	}

	public Machine machine() {
		return machine;
	}

	/** The main cell of the part {@code pos} belongs to. */
	public static BlockPos mainOf(BlockPos pos, BlockState state) {
		return main(pos, state);
	}

	@Override
	protected BlockEntity createMain(BlockPos pos, BlockState state) {
		if (machine == Machine.FUEL_BAY) {
			return new FuelBayBlockEntity(pos, state);
		}
		if (machine == Machine.HAUL_DISPATCH) {
			return new HaulDispatchBlockEntity(pos, state);
		}
		if (machine == Machine.MATRIX_FILL_POINT) {
			return new MatrixFillPointBlockEntity(pos, state);
		}
		return machine == Machine.DUMP_POCKET || machine == Machine.PIT_HOPPER ? new DumpBlockEntity(pos, state) : new PlantBlockEntity(pos, state);
	}

	@Override
	protected <T extends BlockEntity> @Nullable BlockEntityTicker<T> mainTicker(Level level, BlockState state, BlockEntityType<T> type) {
		if (level.isClientSide()) {
			return dev.wildcatter.tailings.TailingsSounds.loop(machine) == null ? null : (at, pos, cell, entity) -> dev.wildcatter.kinetic.Sounding.Hook.start.accept(entity);
		}
		if (machine == Machine.FUEL_BAY) {
			return createTickerHelper(type, TailingsBlocks.FUEL_BAY_ENTITY, FuelBayBlockEntity::tick);
		}
		if (machine == Machine.HAUL_DISPATCH) {
			return createTickerHelper(type, TailingsBlocks.HAUL_DISPATCH_ENTITY, HaulDispatchBlockEntity::tick);
		}
		if (machine == Machine.MATRIX_FILL_POINT) {
			return createTickerHelper(type, TailingsBlocks.MATRIX_FILL_POINT_ENTITY, MatrixFillPointBlockEntity::tick);
		}
		return machine == Machine.DUMP_POCKET || machine == Machine.PIT_HOPPER ? createTickerHelper(type, TailingsBlocks.DUMP, DumpBlockEntity::tick)
				: createTickerHelper(type, TailingsBlocks.PLANT, PlantBlockEntity::tick);
	}

	@Override
	protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
			InteractionHand hand, BlockHitResult hit) {
		if (level.getBlockEntity(main(pos, state)) instanceof PlantBlockEntity plant) {
			if (plant.machine() == Machine.ORE_SORTER) {
				if (!level.isClientSide() && plant.setSide(hit.getDirection(), stack)) {
					player.sendOverlayMessage(net.minecraft.network.chat.Component.literal(plant.status()));
				}
				return Feeds.parse(stack) != null ? InteractionResult.SUCCESS : super.useItemOn(stack, state, level, pos, player, hand, hit);
			}
			if (plant.offer(stack)) {
				return InteractionResult.SUCCESS;
			}
		}
		return super.useItemOn(stack, state, level, pos, player, hand, hit);
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		if (level.getBlockEntity(main(pos, state)) instanceof MatrixFillPointBlockEntity fill) {
			if (!level.isClientSide()) {
				player.sendOverlayMessage(fill.inspect());
			}
			return InteractionResult.SUCCESS;
		}
		if (level.getBlockEntity(main(pos, state)) instanceof HaulDispatchBlockEntity office) {
			if (!level.isClientSide()) {
				player.sendSystemMessage(office.inspect());
			}
			return InteractionResult.SUCCESS;
		}
		if (level.getBlockEntity(main(pos, state)) instanceof PlantBlockEntity plant) {
			if (player.isShiftKeyDown() && plant.machine() == Machine.ORE_SORTER) {
				if (!level.isClientSide() && plant.setWasteSide(hit.getDirection())) {
					player.sendOverlayMessage(net.minecraft.network.chat.Component.literal(plant.status()));
				}
				return InteractionResult.SUCCESS;
			}
			if (player.isShiftKeyDown() && plant.machine() == Machine.CONDITIONER) {
				if (!level.isClientSide()) {
					plant.toggleSelective();
					player.sendOverlayMessage(net.minecraft.network.chat.Component.literal(plant.status()));
				}
				return InteractionResult.SUCCESS;
			}
			if (plant.give(player)) {
				return InteractionResult.SUCCESS;
			}
		}
		return super.useWithoutItem(state, level, pos, player, hit);
	}
}
