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
		if (machine == Machine.PIT_SWITCH_HOUSE) {
			return new PitSwitchHouseBlockEntity(pos, state);
		}
		if (machine == Machine.SHOVEL_CRAWLERS) {
			return new RopeShovelBlockEntity(pos, state);
		}
		if (machine == Machine.BATTERY_CHARGER) {
			return new BatteryChargerBlockEntity(pos, state);
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
		if (machine == Machine.PIT_SWITCH_HOUSE) {
			return createTickerHelper(type, TailingsBlocks.PIT_SWITCH_HOUSE_ENTITY, PitSwitchHouseBlockEntity::tick);
		}
		if (machine == Machine.SHOVEL_CRAWLERS) {
			return createTickerHelper(type, TailingsBlocks.ROPE_SHOVEL_ENTITY, RopeShovelBlockEntity::tick);
		}
		if (machine == Machine.BATTERY_CHARGER) {
			return createTickerHelper(type, TailingsBlocks.BATTERY_CHARGER_ENTITY, BatteryChargerBlockEntity::tick);
		}
		return machine == Machine.DUMP_POCKET || machine == Machine.PIT_HOPPER ? createTickerHelper(type, TailingsBlocks.DUMP, DumpBlockEntity::tick)
				: createTickerHelper(type, TailingsBlocks.PLANT, PlantBlockEntity::tick);
	}

	@Override
	protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
			InteractionHand hand, BlockHitResult hit) {
		if (stack.getItem() instanceof dev.wildcatter.tailings.item.TrailingCableItem) {
			return InteractionResult.PASS;   // the reel takes an outlet of a switch house: the item's own use
		}
		if (level.getBlockEntity(main(pos, state)) instanceof BatteryChargerBlockEntity charger) {
			// a traction battery goes into the charger; it hands back the one it held
			if (!level.isClientSide() && charger.offer(player, hand, stack)) {
				player.sendOverlayMessage(charger.inspect());
			}
			return stack.getItem() == dev.wildcatter.tailings.TailingsItems.LOCO_BATTERY ? InteractionResult.SUCCESS : super.useItemOn(stack, state, level, pos, player, hand, hit);
		}
		if (level.getBlockEntity(main(pos, state)) instanceof DumpBlockEntity pocket && !pocket.pit() && pocket.oversize() > 0
				&& stack.is(net.minecraft.tags.ItemTags.PICKAXES)) {
			// a pickaxe breaks a piece of oversize through the grizzly's bars, as a rock breaker does
			if (!level.isClientSide()) {
				String broke = pocket.breakOne();
				stack.hurtAndBreak(1, player, hand);
				player.sendOverlayMessage(net.minecraft.network.chat.Component.literal("broke " + Feeds.stack(broke, 1).getHoverName().getString()
						+ " through the grizzly; " + pocket.oversize() + " oversize left on its bars"));
			}
			return InteractionResult.SUCCESS;
		}
		if (machine == Machine.HEADFRAME && stack.getItem() == dev.wildcatter.tailings.TailingsItems.SKIP
				&& level.getBlockEntity(main(pos, state)) instanceof PlantBlockEntity headframe) {
			// a skip hangs on the next free rope, at the dump (shaft-and-hoist.md, L4)
			if (level instanceof net.minecraft.server.level.ServerLevel server) {
				var skip = dev.wildcatter.tailings.entity.ShaftSkipEntity.hang(server, ShaftLayout.Frame.of(headframe));
				if (skip == null) {
					player.sendOverlayMessage(net.minecraft.network.chat.Component.literal("both ropes have their skips"));
				} else {
					stack.consume(1, player);
					player.sendOverlayMessage(net.minecraft.network.chat.Component.literal("hung a skip in the " + (skip.compartment() == 0 ? "west" : "east") + " compartment"));
				}
			}
			return InteractionResult.SUCCESS;
		}
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
		if (level.getBlockEntity(main(pos, state)) instanceof PitSwitchHouseBlockEntity house) {
			// its panel: each outlet's breaker and a reset; sneak for the full readout
			if (!level.isClientSide()) {
				if (player.isShiftKeyDown()) {
					player.sendSystemMessage(house.inspect());
				} else {
					player.openMenu(new net.minecraft.world.SimpleMenuProvider((id, inventory, opener) -> new PitPanelMenu(id, inventory, house),
							net.minecraft.network.chat.Component.literal(Machine.PIT_SWITCH_HOUSE.displayName())));
				}
			}
			return InteractionResult.SUCCESS;
		}
		if (level.getBlockEntity(main(pos, state)) instanceof RopeShovelBlockEntity crawlers) {
			// the rope shovel standing as its parts: its panel, RUN to lift it; sneak for the readout
			if (!level.isClientSide()) {
				if (player.isShiftKeyDown()) {
					player.sendSystemMessage(crawlers.inspect());
				} else {
					player.openMenu(new net.minecraft.world.SimpleMenuProvider((id, inventory, opener) -> new PitPanelMenu(id, inventory, crawlers),
							net.minecraft.network.chat.Component.literal("Rope Shovel")));
				}
			}
			return InteractionResult.SUCCESS;
		}
		if (level.getBlockEntity(main(pos, state)) instanceof HaulDispatchBlockEntity office) {
			if (!level.isClientSide()) {
				player.sendSystemMessage(office.inspect());
			}
			return InteractionResult.SUCCESS;
		}
		if (level.getBlockEntity(main(pos, state)) instanceof BatteryChargerBlockEntity charger) {
			// a sneaking click takes its battery out; a click reads it
			if (!level.isClientSide()) {
				if (player.isShiftKeyDown()) {
					charger.takeOut(player);
				}
				player.sendOverlayMessage(charger.inspect());
			}
			return InteractionResult.SUCCESS;
		}
		if (player.isShiftKeyDown() && level.getBlockEntity(main(pos, state)) instanceof DumpBlockEntity pocket && !pocket.pit() && pocket.oversize() > 0) {
			// lifting the oversize off the grizzly by hand: it comes back to the player
			if (!level.isClientSide()) {
				pocket.liftOff().forEach((id, n) -> {
					for (int left = n; left > 0; left -= 64) {
						player.getInventory().placeItemBackInInventory(Feeds.stack(id, Math.min(64, left)), net.minecraft.util.Prediction.SERVER_ONLY);
					}
				});
				player.sendOverlayMessage(net.minecraft.network.chat.Component.literal("lifted the oversize off the grizzly"));
			}
			return InteractionResult.SUCCESS;
		}
		if (level.getBlockEntity(main(pos, state)) instanceof PlantBlockEntity plant) {
			if (plant.machine() == Machine.ORE_SORTER) {
				// its screen: each side's type, and the waste side
				if (!level.isClientSide()) {
					player.openMenu(new net.minecraft.world.SimpleMenuProvider((id, inventory, opener) -> new SorterMenu(id, inventory, plant),
							net.minecraft.network.chat.Component.literal(plant.machine().displayName())));
				}
				return InteractionResult.SUCCESS;
			}
			if (player.isShiftKeyDown() && plant.machine() == Machine.DMS_MODULE) {
				// the module's medium setting, a step a click (dense-medium-separation.md, L7)
				if (!level.isClientSide()) {
					player.sendOverlayMessage(net.minecraft.network.chat.Component.literal(plant.stepMedium()));
				}
				return InteractionResult.SUCCESS;
			}
			if (player.isShiftKeyDown() && plant.machine() == Machine.XRT_SORTER) {
				// its screen's threshold, a step a click (xrt-ore-sorter.md, L8)
				if (!level.isClientSide()) {
					plant.stepThreshold();
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
