package dev.wildcatter.tailings.block;

import dev.wildcatter.energy.Inspectable;
import dev.wildcatter.multiblock.BigPartEntity;
import dev.wildcatter.multiblock.BigParts;
import dev.wildcatter.tailings.TailingsBlocks;
import dev.wildcatter.tailings.entity.MineVehicleEntity;
import dev.wildcatter.tailings.haul.Haul;
import dev.wildcatter.tailings.mine.DriveCourse;
import dev.wildcatter.tailings.mine.Heading;
import dev.wildcatter.tailings.mine.TbmPart;
import dev.wildcatter.tailings.mine.TunnelDrill;
import dev.wildcatter.tailings.mine.DrillProgram;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.Containers;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

/**
 * The operator cab, at the tail of the TBM's main beam (docs/multiblocks/tunnel-boring-machine.md). It carries the machine's
 * {@link TbmCore}. While the machine is blocks it is the machine's body: it checks the cutterhead, segment erector and track layer
 * stand where they should, draws its power through the cable junction, and after each advance moves every part one block on.
 * A cab left from before R6 (the one-block {@code tunnel_drill}) drops what it held and a main beam item.
 */
public class DrillBlockEntity extends BlockEntity implements Inspectable, WorldlyContainer, BigPartEntity {
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
	/** The rails each machine is loading cars at, per level: for the trains, which wait there. */
	private static final Map<Level, Map<Long, BlockPos>> LOADING = new WeakHashMap<>();

	private final TbmCore core = new TbmCore();
	private boolean grown;
	private boolean moving;
	private boolean forced;
	private int refresh;
	private @Nullable DrillBlockEntity movedTo;

	public DrillBlockEntity(BlockPos pos, BlockState state) {
		super(TailingsBlocks.DRILL, pos, state);
		if (state.hasProperty(TbmPartBlock.FACING)) {
			core.program().setHeading(RailView.heading(state.getValue(TbmPartBlock.FACING)));
		}
	}

	public static void tick(Level level, BlockPos pos, BlockState state, DrillBlockEntity cab) {
		cab.serverTick();
	}

	public TbmCore core() {
		return core;
	}

	public DrillProgram program() {
		return core.program();
	}

	public TunnelDrill drill() {
		return core.drill();
	}

	public int tank() {
		return core.tank();
	}

	public int status() {
		return core.status();
	}

	public int lack() {
		return core.lack();
	}

	public boolean running() {
		return core.running();
	}

	public void addQuench(int buckets) {
		core.addQuench(buckets);
		setChanged();
	}

	/** The game test runs it without building a grid. */
	public void forceRunning() {
		forced = true;
		PumpBlockEntity pump = pump();
		if (pump != null) {
			pump.forceRunning();
		}
	}

	public void press(int button) {
		if (button == 10 && !core.running()) {
			start();
		}
		core.press(button, level);
		setChanged();
	}

	/** On starting: the machine's heading is the way it was built, and the bore is set from the cutterhead the first time. */
	private void start() {
		core.program().setHeading(RailView.heading(facing()));
		if (core.drill().advanceCount() == 0) {
			placeOrigin();
		}
		if (core.junction() == null || !(level.getBlockEntity(core.junction()) instanceof CableJunctionBlockEntity)) {
			core.setJunction(findJunction());
		}
		refresh = 0;
	}

	private Direction facing() {
		return getBlockState().getValue(TbmPartBlock.FACING);
	}

	// ---- the machine as blocks ---------------------------------------------------------------------------------------------------

	/** A cell in the main beam's own frame (x right, y up, z back, looking along the heading), in the world. */
	private BlockPos local(int x, int y, int z) {
		int dx = x, dz = z;
		for (int i = 0; i < BigParts.turns(getBlockState()); i++) {
			int t = dx;
			dx = -dz;
			dz = t;
		}
		return worldPosition.offset(dx, y, dz);
	}

	/** Where a part's main cell must be, from the main beam's: the layout in tunnel-boring-machine.md. */
	private BlockPos expected(TbmPart part) {
		return local(part.across() - TbmPart.MAIN_BEAM.across(), part.up() - TbmPart.MAIN_BEAM.up(), TbmPart.MAIN_BEAM.ahead() - part.ahead());
	}

	/** The old dewatering pump, against the main beam's tail until the pump gantry (R6c). */
	private @Nullable PumpBlockEntity pump() {
		if (level == null || !getBlockState().hasProperty(TbmPartBlock.FACING)) {
			return null;
		}
		BlockPos behind = local(0, 0, TbmPart.MAIN_BEAM.length());
		BlockState there = level.getBlockState(behind);
		if (there.getBlock() instanceof DewateringPumpBlock block) {
			return level.getBlockEntity(block.mainOf(behind, there)) instanceof PumpBlockEntity pump ? pump : null;
		}
		return null;
	}

	/** The part missing or out of place, or null when the machine is whole. */
	private @Nullable String missing() {
		for (TbmPart part : new TbmPart[] {TbmPart.CUTTERHEAD, TbmPart.SEGMENT_ERECTOR, TbmPart.TRACK_LAYER}) {
			BlockPos at = expected(part);
			BlockState state = level.getBlockState(at);
			if (!(state.getBlock() instanceof TbmPartBlock block) || block.part() != part || !BigParts.main(at, state).equals(at)
					|| state.getValue(TbmPartBlock.FACING) != facing()) {
				return part.displayName() + " (" + where(part) + ")";
			}
		}
		if (pump() == null) {
			return "Dewatering Pump against the main beam's tail";
		}
		if (core.junction() == null || !(level.getBlockEntity(core.junction()) instanceof CableJunctionBlockEntity)) {
			return "Cable Junction within " + JUNCTION_REACH + " blocks, wired to 4.16 kV";
		}
		return null;
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
		if (forced) {
			return true;
		}
		PumpBlockEntity pump = pump();
		return core.junction() != null && level.getBlockEntity(core.junction()) instanceof CableJunctionBlockEntity junction && junction.live()
				&& pump != null && pump.powered();
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
		BlockPos cutter = expected(TbmPart.CUTTERHEAD);
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

	/** Every cell of the machine: its four parts and the pump behind it. */
	private List<BlockPos> machineCells() {
		List<BlockPos> cells = new ArrayList<>(BigParts.cells(worldPosition, getBlockState()));
		for (TbmPart part : new TbmPart[] {TbmPart.CUTTERHEAD, TbmPart.SEGMENT_ERECTOR, TbmPart.TRACK_LAYER}) {
			BlockPos at = expected(part);
			cells.addAll(BigParts.cells(at, level.getBlockState(at)));
		}
		PumpBlockEntity pump = pump();
		if (pump != null && level.getBlockState(pump.getBlockPos()).getBlock() instanceof DewateringPumpBlock block) {
			for (BlockPos cell : block.cells(pump.getBlockPos(), facing())) {
				if (level.getBlockState(cell).getBlock() == TailingsBlocks.DEWATERING_PUMP && !cells.contains(cell)) {
					cells.add(cell);
				}
			}
		}
		return cells;
	}

	/**
	 * Moves every cell of the machine one block along the heading, block entities and all. Every cell it moves into must be air,
	 * replaceable, or its own; otherwise nothing moves. Cells go without neighbour updates or drops, so no part breaks on the way.
	 */
	private @Nullable TbmCore move() {
		if (!(level instanceof ServerLevel server)) {
			return null;
		}
		Heading heading = core.program().heading();
		BlockPos delta = new BlockPos(heading.forwardX(), 0, heading.forwardZ());
		List<BlockPos> cells = machineCells();
		for (BlockPos cell : cells) {
			BlockPos dest = cell.offset(delta);
			BlockState there = level.getBlockState(dest);
			if (!cells.contains(dest) && !there.isAir() && !there.canBeReplaced()) {
				return null;
			}
		}
		List<BlockState> states = new ArrayList<>();
		List<CompoundTag> tags = new ArrayList<>();
		for (BlockPos cell : cells) {
			states.add(level.getBlockState(cell));
			BlockEntity entity = level.getBlockEntity(cell);
			if (entity == null) {
				tags.add(null);
			} else {
				TagValueOutput output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, level.registryAccess());
				entity.saveWithoutMetadata(output);
				tags.add(output.buildResult());
			}
		}
		int quiet = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE | Block.UPDATE_SUPPRESS_DROPS;
		moving = true;
		for (BlockPos cell : cells) {
			server.setBlock(cell, Blocks.AIR.defaultBlockState(), quiet);
		}
		for (int i = 0; i < cells.size(); i++) {
			BlockPos dest = cells.get(i).offset(delta);
			server.setBlock(dest, states.get(i), quiet);
			if (tags.get(i) != null && server.getBlockEntity(dest) != null) {
				server.getBlockEntity(dest).loadCustomOnly(TagValueInput.create(ProblemReporter.DISCARDING, level.registryAccess(), tags.get(i)));
			}
		}
		if (server.getBlockEntity(worldPosition.offset(delta)) instanceof DrillBlockEntity moved) {
			movedTo = moved;
			moved.forced = forced;
			moved.refresh = 0;
			moved.setChanged();
			return moved.core;
		}
		return null;
	}

	private final TbmCore.Body body = new TbmCore.Body() {
		@Override
		public @Nullable String missing() {
			return DrillBlockEntity.this.missing();
		}

		@Override
		public boolean powered() {
			return DrillBlockEntity.this.powered();
		}

		@Override
		public @Nullable TbmCore move() {
			return DrillBlockEntity.this.move();
		}
	};

	// ---- ticking -----------------------------------------------------------------------------------------------------------------

	private void serverTick() {
		if (level == null || level.isClientSide()) {
			return;
		}
		if (getBlockState().getBlock() instanceof TunnelDrillBlock) {
			retireOldCab();
			return;
		}
		core.acceptBuckets();
		loadCars();
		PumpBlockEntity pump = pump();
		if (pump != null) {
			pump.setDemand(core.running());
		}
		if (core.running() && core.junction() != null && level.getBlockEntity(core.junction()) instanceof CableJunctionBlockEntity junction) {
			junction.draw(TbmCore.BEAM_WATTS);
		}
		if (!core.running()) {
			return;
		}
		lockChunks();
		if (core.waitTicks() > 0) {
			core.countDown();
			return;
		}
		movedTo = null;
		core.advance(level, body);
		setChanged();
	}

	/** For game tests: one advance now, whatever the wait. */
	public dev.wildcatter.tailings.mine.Advance stepNow() {
		if (!core.running()) {
			start();
			core.setRunning(true);
		}
		movedTo = null;
		return core.advance(level, body);
	}

	/** The cab where the machine now stands, after a move; this one, if it has not moved. */
	public DrillBlockEntity current() {
		return movedTo != null ? movedTo : this;
	}

	/** Refreshes the ticket on the cab's chunk, the face's and the probe's while it drives; it times out by itself after. */
	private void lockChunks() {
		if (!(level instanceof ServerLevel server) || refresh-- > 0) {
			return;
		}
		refresh = ChunkRunner.REFRESH_TICKS;
		ChunkRunner.refresh(server, worldPosition);
		ChunkRunner.refresh(server, cell(0, 0, 0));
		ChunkRunner.refresh(server, cell(0, 0, DriveCourse.PROBE_BLOCKS));
	}

	public BlockPos cell(int across, int up, int ahead) {
		TunnelDrill drill = core.drill();
		return new BlockPos(drill.worldX(across, ahead), drill.worldY(up, ahead), drill.worldZ(across, ahead));
	}

	// ---- the loading point -------------------------------------------------------------------------------------------------------

	/** The two rails just laid behind the track layer, on columns 2 and 5: cars there take the machine's muck. */
	private List<BlockPos> loadingRails() {
		int ahead = -DriveCourse.FLOOR_BEHIND;
		return List.of(cell(2, 1, ahead), cell(5, 1, ahead));
	}

	private void loadCars() {
		if (core.drill().advanceCount() == 0) {
			return;
		}
		Map<Long, BlockPos> here = LOADING.computeIfAbsent(level, l -> new HashMap<>());
		here.values().removeIf(worldPosition::equals);
		for (BlockPos rail : loadingRails()) {
			here.put(rail.asLong(), worldPosition);
			for (MineVehicleEntity car : level.getEntitiesOfClass(MineVehicleEntity.class, new net.minecraft.world.phys.AABB(rail).inflate(0, 1, 0),
					e -> !e.locomotive() && e.load().room() > 0 && BlockPos.containing(e.getX(), e.getY() - 0.5, e.getZ()).equals(rail))) {
				if (core.giveMuck(car.load(), Haul.TRANSFER) > 0) {
					car.refreshLoad();
					setChanged();
				}
				return;
			}
		}
	}

	/** True while it has muck waiting for a car. */
	public boolean hasMuck() {
		return core.hasMuck();
	}

	/** The machine whose loading point is the rail at {@code rail}, or null. */
	public static @Nullable DrillBlockEntity loadingAt(Level level, BlockPos rail) {
		Map<Long, BlockPos> here = LOADING.get(level);
		BlockPos cab = here == null ? null : here.get(rail.asLong());
		return cab != null && level.getBlockEntity(cab) instanceof DrillBlockEntity drill ? drill : null;
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
	public int getContainerSize() {
		return SLOTS;
	}

	@Override
	public boolean isEmpty() {
		return core.slots().stream().allMatch(ItemStack::isEmpty);
	}

	@Override
	public ItemStack getItem(int slot) {
		return core.slots().get(slot);
	}

	@Override
	public ItemStack removeItem(int slot, int count) {
		ItemStack out = ContainerHelper.removeItem(core.slots(), slot, count);
		setChanged();
		return out;
	}

	@Override
	public ItemStack removeItemNoUpdate(int slot) {
		return ContainerHelper.takeItem(core.slots(), slot);
	}

	@Override
	public void setItem(int slot, ItemStack stack) {
		core.slots().set(slot, stack);
		setChanged();
	}

	@Override
	public boolean stillValid(Player player) {
		return Container.stillValidBlockEntity(this, player);
	}

	@Override
	public void clearContent() {
		core.slots().clear();
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
	public boolean canPlaceItem(int slot, ItemStack stack) {
		return core.canPlace(slot, stack);
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
