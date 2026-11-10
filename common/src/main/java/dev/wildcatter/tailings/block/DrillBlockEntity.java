package dev.wildcatter.tailings.block;

import dev.wildcatter.energy.Inspectable;
import dev.wildcatter.multiblock.BigPartEntity;
import dev.wildcatter.multiblock.BigParts;
import dev.wildcatter.tailings.TailingsBlocks;
import dev.wildcatter.tailings.entity.TbmEntity;
import dev.wildcatter.tailings.mine.DrillCodes;
import dev.wildcatter.tailings.mine.Heading;
import dev.wildcatter.tailings.mine.TbmPart;
import dev.wildcatter.tailings.mine.TunnelDrill;
import dev.wildcatter.tailings.mine.DrillProgram;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.Containers;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

/**
 * The operator cab, at the tail of the TBM's main beam (docs/multiblocks/tunnel-boring-machine.md). It carries the machine's
 * {@link TbmCore} while the machine stands as blocks: it checks the cutterhead, segment erector and track layer stand where they
 * should and the cable junction is live. Started, the machine lifts into a {@link TbmEntity}, which takes the core and runs.
 * A cab left from before R6 (the one-block {@code tunnel_drill}) drops what it held and a main beam item.
 */
public class DrillBlockEntity extends BlockEntity implements Inspectable, WorldlyContainer, BigPartEntity, TbmCab {
	public static final int SLOTS = TbmCore.SLOTS;
	public static final int INPUTS = TbmCore.INPUTS;
	public static final int MUCK_SLOTS = TbmCore.MUCK_SLOTS;
	public static final int SLOT_RAIL = TbmCore.SLOT_RAIL;
	public static final int SLOT_GRAVEL = TbmCore.SLOT_GRAVEL;
	public static final int SLOT_COBBLE = TbmCore.SLOT_COBBLE;
	public static final int SLOT_SEGMENTS = TbmCore.SLOT_SEGMENTS;
	public static final int SLOT_WATER = TbmCore.SLOT_WATER;
	public static final int SLOT_CABLE = TbmCore.SLOT_CABLE;
	/** A cable junction is looked for this far from the cab when the machine starts. */
	private static final int JUNCTION_REACH = 16;
	private final TbmCore core = new TbmCore();
	private boolean grown;
	private boolean moving;

	public DrillBlockEntity(BlockPos pos, BlockState state) {
		super(TailingsBlocks.DRILL, pos, state);
		if (state.hasProperty(TbmPartBlock.FACING)) {
			core.program().setHeading(RailView.heading(state.getValue(TbmPartBlock.FACING)));
		}
	}

	public static void tick(Level level, BlockPos pos, BlockState state, DrillBlockEntity cab) {
		cab.serverTick();
	}

	@Override
	public TbmCore core() {
		return core;
	}

	@Override
	public BlockPos menuPos() {
		return worldPosition;
	}

	/** False for an old one-block cab. */
	public boolean standing() {
		return getBlockState().getBlock() instanceof TbmPartBlock && !isRemoved();
	}

	@Override
	public Vec3 cutterCorner() {
		BlockPos cutter = cutter();
		return new Vec3(cutter.getX(), cutter.getY(), cutter.getZ());
	}

	@Override
	public Direction machineFacing() {
		return facing();
	}

	@Override
	public double machineSlice() {
		return core.drill().advanceCount() - 1;
	}

	public DrillProgram program() {
		return core.program();
	}

	public TunnelDrill drill() {
		return core.drill();
	}

	public int status() {
		return core.status();
	}

	public boolean running() {
		return core.running();
	}

	/** The game test runs it without building a grid. */
	public void forceRunning() {
		core.setForced(true);
	}

	/** Go: the machine's heading is the way it was built, and the bore is set from the cutterhead the first time. */
	@Override
	public void press(int button, Player player) {
		if (button == TbmCore.BUTTON_RIDE) {
			if (player != null) {
				player.sendOverlayMessage(Component.literal("Start the machine first: the seat rides with it"));
			}
			return;
		}
		if (button == 10 && !core.running()) {
			core.program().setHeading(RailView.heading(facing()));
			if (core.drill().advanceCount() == 0) {
				placeOrigin();
			}
			if (core.junction() == null || !(level.getBlockEntity(core.junction()) instanceof CableJunctionBlockEntity)) {
				core.setJunction(findJunction());
			}
		}
		core.press(button, level);
		setChanged();
	}

	private Direction facing() {
		return getBlockState().getValue(TbmPartBlock.FACING);
	}

	private BlockPos cutter() {
		return TbmMachine.cutterFromBeam(worldPosition, facing());
	}

	// ---- the machine as blocks ---------------------------------------------------------------------------------------------------

	/** The part missing or out of place, or null when the machine is whole. */
	public @Nullable String missing() {
		BlockPos cutter = cutter();
		for (TbmPart part : new TbmPart[] {TbmPart.CUTTERHEAD, TbmPart.SEGMENT_ERECTOR, TbmPart.TRACK_LAYER}) {
			BlockPos at = TbmMachine.main(part, cutter, facing());
			BlockState state = level.getBlockState(at);
			if (!(state.getBlock() instanceof TbmPartBlock block) || block.part() != part || !BigParts.main(at, state).equals(at)
					|| state.getValue(TbmPartBlock.FACING) != facing()) {
				return part.displayName() + " (" + where(part) + ")";
			}
		}
		if (core.junction() == null || !(level.getBlockEntity(core.junction()) instanceof CableJunctionBlockEntity)) {
			return "Cable Junction within " + JUNCTION_REACH + " blocks, wired to 4.16 kV";
		}
		return level instanceof ServerLevel server ? TbmBackup.fault(server, core) : null;
	}

	private static String where(TbmPart part) {
		return switch (part) {
			case CUTTERHEAD -> "at the front, against the segment erector";
			case SEGMENT_ERECTOR -> "against the main beam's front";
			case TRACK_LAYER -> "under the main beam's rear, a block above the floor";
			case MAIN_BEAM -> "";
		};
	}

	private boolean powered() {
		return core.forced() || core.junction() != null && level.getBlockEntity(core.junction()) instanceof CableJunctionBlockEntity junction && junction.live();
	}

	private @Nullable BlockPos findJunction() {
		for (BlockPos at : BlockPos.betweenClosed(worldPosition.offset(-JUNCTION_REACH, -JUNCTION_REACH, -JUNCTION_REACH),
				worldPosition.offset(JUNCTION_REACH, JUNCTION_REACH, JUNCTION_REACH))) {
			if (level.getBlockEntity(at) instanceof CableJunctionBlockEntity) {
				return at.immutable();
			}
		}
		return null;
	}

	/** The bore starts at the cutterhead: its slice 0 is the rock just ahead of it, its floor a block above the cutterhead's bottom. */
	private void placeOrigin() {
		BlockPos cutter = cutter();
		BlockState state = level.getBlockState(cutter);
		Heading heading = core.program().heading();
		if (!(state.getBlock() instanceof TbmPartBlock)) {
			return;
		}
		List<BlockPos> cells = BigParts.cells(cutter, state);
		for (BlockPos cell : cells) {
			if (cell.getY() == cutter.getY() && cells.contains(cell.offset(heading.acrossX() * 7, 0, heading.acrossZ() * 7))) {
				core.drill().placeOrigin(cell.getX() + heading.forwardX(), cell.getY() + 1, cell.getZ() + heading.forwardZ());
				return;
			}
		}
	}

	/** The contraption takes the cab's contents with it: going, it drops nothing. */
	public void releaseForLift() {
		moving = true;
		core.slots().clear();
	}

	/** Every part drops as its item, and the cab's slots drop. Only while it is stopped. */
	public void dismantle() {
		if (core.running()) {
			core.report(core.status(), "stop it before dismantling it");
			return;
		}
		BlockPos cutter = cutter();
		Direction facing = facing();
		for (TbmPart part : new TbmPart[] {TbmPart.CUTTERHEAD, TbmPart.SEGMENT_ERECTOR, TbmPart.TRACK_LAYER}) {
			BlockPos at = TbmMachine.main(part, cutter, facing);
			if (level.getBlockState(at).getBlock() instanceof TbmPartBlock block && block.part() == part) {
				level.destroyBlock(at, true);
			}
		}
		level.destroyBlock(worldPosition, true);
	}

	// ---- ticking -----------------------------------------------------------------------------------------------------------------

	private void serverTick() {
		if (level == null || level.isClientSide()) {
			return;
		}
		if (getBlockState().getBlock() instanceof TunnelDrillBlock) {
			retireOldCab();
			return;
		}
		dev.wildcatter.tailings.entity.GantryEntity pump = level instanceof ServerLevel server ? TbmBackup.loadTank(server, this) : null;
		try {
			cabTick();
		} finally {
			TbmBackup.storeTank(pump, core);
		}
	}

	private void cabTick() {
		core.acceptBuckets();
		core.tickLoading(level);
		if (core.junction() == null && level.getGameTime() % 100 == 0) {
			core.setJunction(findJunction());   // so its signals reach it before anyone has pressed Go
		}
		if (core.junction() != null && level.getBlockEntity(core.junction()) instanceof CableJunctionBlockEntity reporting) {
			reporting.report(core, level, button -> press(button, null));
		}
		if (level.getGameTime() % 20 == 0 && core.drill().advanceCount() > 0) {
			BlockPos tail = core.backupTail();
			core.setAir(VentNetwork.airAt(level, tail).flow(), VentNetwork.radonRock(level, tail), 20);
		}
		if (!core.running() || !(level instanceof ServerLevel server)) {
			return;
		}
		String missing = missing();
		if (missing != null) {
			core.report(DrillCodes.NOT_BUILT, "NOT BUILT: needs the " + missing);
			return;
		}
		if (!powered()) {
			if (level.getBlockEntity(core.junction()) instanceof CableJunctionBlockEntity junction) {
				junction.draw(TbmCore.BEAM_WATTS);
			}
			core.report(DrillCodes.NEED_POWER, "waiting for power at the cable junction");
			return;
		}
		TbmEntity.lift(server, this, cutter(), facing());
	}

	// ---- old worlds --------------------------------------------------------------------------------------------------------------

	/** The one-block cab of a world saved before R6: it drops what it held and a main beam item, and goes. */
	private void retireOldCab() {
		Containers.dropContents(level, worldPosition, this);
		Containers.dropItemStack(level, worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(), new ItemStack(TailingsBlocks.MAIN_BEAM));
		String where = "The tunnel drill at " + worldPosition.getX() + " " + worldPosition.getY() + " " + worldPosition.getZ()
				+ " is now a true-size machine: it dropped its stock and a main beam; build the whole machine (tunnel-boring-machine.md)";
		for (Player player : level.players()) {
			if (player.blockPosition().closerThan(worldPosition, 128)) {
				player.sendSystemMessage(Component.literal(where));
			}
		}
		moving = true;
		level.removeBlock(worldPosition, false);
	}

	// ---- reading and saving ------------------------------------------------------------------------------------------------------

	@Override
	public Component inspect() {
		return Component.literal("Tunnel boring machine (1 MW): " + core.readout());
	}

	/** Breaking the cab, any way at all, drops what it holds; moving itself does not. */
	@Override
	public void preRemoveSideEffects(BlockPos pos, BlockState state) {
		super.preRemoveSideEffects(pos, state);
		if (level instanceof ServerLevel server && !moving) {
			TbmBackup.release(server, core);
		}
		if (level != null && !moving) {
			Containers.dropContents(level, pos, this);
		}
	}

	@Override
	public boolean grown() {
		return grown;
	}

	@Override
	public void markGrown() {
		grown = true;
		setChanged();
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		core.save(output);
		output.putBoolean("grown", grown);
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		core.load(input);
		grown = input.getBooleanOr("grown", false);
	}

	@Override
	public boolean stillValid(Player player) {
		return Container.stillValidBlockEntity(this, player);
	}

	@Override
	public int[] getSlotsForFace(Direction side) {
		if (side == Direction.UP) {
			return new int[] {SLOT_RAIL, SLOT_GRAVEL, SLOT_COBBLE, SLOT_SEGMENTS, SLOT_WATER, SLOT_CABLE};
		}
		int[] out = new int[MUCK_SLOTS + 1];
		out[0] = SLOT_WATER;
		for (int i = 0; i < MUCK_SLOTS; i++) {
			out[i + 1] = INPUTS + i;
		}
		return out;
	}

	@Override
	public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) {
		return side == Direction.UP && canPlaceItem(slot, stack);
	}

	@Override
	public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
		if (side == Direction.UP) {
			return false;
		}
		if (slot == SLOT_WATER) {
			return stack.getItem() == net.minecraft.world.item.Items.BUCKET;
		}
		return slot >= INPUTS;
	}
}
