package dev.wildcatter.tailings.block;

import dev.wildcatter.circuit.Circuit;
import dev.wildcatter.circuit.CircuitPart;
import dev.wildcatter.circuit.Conductor;
import dev.wildcatter.circuit.Motor;
import dev.wildcatter.energy.Inspectable;
import dev.wildcatter.energy.Voltage;
import dev.wildcatter.tailings.TailingsBlocks;
import dev.wildcatter.tailings.mine.Advance;
import dev.wildcatter.tailings.mine.DrillCodes;
import dev.wildcatter.tailings.mine.DrillProgram;
import dev.wildcatter.tailings.mine.DrillStock;
import dev.wildcatter.tailings.mine.DriveCourse;
import dev.wildcatter.tailings.mine.Grade;
import dev.wildcatter.tailings.mine.Ground;
import dev.wildcatter.tailings.mine.Heading;
import dev.wildcatter.tailings.mine.TunnelDrill;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

/**
 * The operator cab. It stays where it was built and the bore grows ahead of it. The main beam is a 1 MW, 4.16 kV motor.
 * While it is driving it keeps its own chunk and the face's chunk loaded, and drops them when it stops.
 */
public class DrillBlockEntity extends BlockEntity implements CircuitPart, Inspectable, WorldlyContainer {
	public static final int BEAM_WATTS = 1_000_000;
	public static final int TANK_MAX = 256;
	public static final int INPUTS = 5;
	public static final int MUCK_SLOTS = 18;
	public static final int SLOTS = INPUTS + MUCK_SLOTS;
	public static final int SLOT_RAIL = 0;
	public static final int SLOT_GRAVEL = 1;
	public static final int SLOT_COBBLE = 2;
	public static final int SLOT_SEGMENTS = 3;
	public static final int SLOT_WATER = 4;

	private final NonNullList<ItemStack> slots = NonNullList.withSize(SLOTS, ItemStack.EMPTY);
	private final Motor motor = new Motor(true, Voltage.MEDIUM, BEAM_WATTS);
	private final DrillProgram program = new DrillProgram(0, Heading.NORTH, 64);
	private final DrillStock stock = new DrillStock();
	private final TunnelDrill drill = new TunnelDrill(program, stock, 0, 0, 0);
	private final List<String> pending = new ArrayList<>();
	private final Set<Long> forced = new HashSet<>();
	private boolean demand;
	private boolean forcedPower;
	private boolean running;
	private int wait;
	private int tank;
	private int status = DrillCodes.IDLE;
	private int lack = DrillCodes.LACK_NONE;
	private boolean loading;

	public DrillBlockEntity(BlockPos pos, BlockState state) {
		super(TailingsBlocks.DRILL, pos, state);
		if (state.hasProperty(TunnelDrillBlock.FACING)) {
			program.setHeading(Heading.valueOf(state.getValue(TunnelDrillBlock.FACING).name()));
		}
	}

	public static void tick(Level level, BlockPos pos, BlockState state, DrillBlockEntity drill) {
		drill.serverTick();
	}

	public DrillProgram program() {
		return program;
	}

	public TunnelDrill drill() {
		return drill;
	}

	public int tank() {
		return tank;
	}

	public int status() {
		return status;
	}

	public int lack() {
		return lack;
	}

	public boolean running() {
		return running;
	}

	public void addQuench(int buckets) {
		tank = Math.min(TANK_MAX, tank + Math.max(0, buckets));
		setChanged();
	}

	public void forceRunning() {
		forcedPower = true;
		PumpBlockEntity pump = pump();
		if (pump != null) {
			pump.forceRunning();
		}
	}

	public boolean powered() {
		return forcedPower || motor.output() > 0;
	}

	/** Screen buttons. The same ids are the menu's click buttons. */
	public void press(int button) {
		switch (button) {
			case 0 -> program.setTargetY(program.targetY() - 1);
			case 1 -> program.setTargetY(program.targetY() + 1);
			case 2 -> turn();
			case 3 -> program.setFlatLength(program.flatLength() - 8);
			case 4 -> program.setFlatLength(program.flatLength() + 8);
			case 5 -> toggleGrade();
			case 6 -> program.setStopOnOre(!program.stopOnOre());
			case 7 -> program.setStopOnCable(!program.stopOnCable());
			case 8 -> program.setStopOnLiquid(!program.stopOnLiquid());
			case 9 -> program.setStopOnEmpty(!program.stopOnEmpty());
			case 10 -> toggleRun();
			case 11 -> program.setCableBlocks(program.cableBlocks() - 64);
			case 12 -> program.setCableBlocks(program.cableBlocks() + 64);
			default -> {
				return;
			}
		}
		setChanged();
	}

	public Advance stepNow() {
		if (level == null || level.isClientSide()) {
			return Advance.stopped(Advance.Stop.NONE, "");
		}
		if (drill.advanceCount() == 0) {
			drill.placeOriginFrom(worldPosition.getX(), worldPosition.getY(), worldPosition.getZ());
		}
		if (!formed()) {
			status = DrillCodes.NEED_PUMP;
			return Advance.stopped(Advance.Stop.NONE, "pump");
		}
		PumpBlockEntity pump = pump();
		if (!powered() || pump == null || !pump.powered()) {
			status = DrillCodes.NEED_POWER;
			return Advance.stopped(Advance.Stop.NONE, "power");
		}
		acceptBuckets();
		pullStock();
		flushPending();
		if (!pending.isEmpty()) {
			status = DrillCodes.STOP_BUFFER;
			lack = DrillCodes.LACK_NONE;
			return Advance.stopped(Advance.Stop.BUFFER_FULL, "muck buffer full");
		}
		WorldGround ground = new WorldGround(level, program.heading());
		if (!roomForFace(ground)) {
			status = DrillCodes.STOP_BUFFER;
			return Advance.stopped(Advance.Stop.BUFFER_FULL, "muck buffer full");
		}
		Advance drove = drill.step(ground);
		for (var load : drove.loads().entrySet()) {
			if (!push(load.getKey(), load.getValue())) {
				pending.add(load.getKey() + " " + load.getValue());
			}
		}
		pushStock();
		if (drill.stopped() != Advance.Stop.NONE) {
			running = false;
			status = DrillCodes.fromStop(drill.stopped());
			lack = DrillCodes.lack(drill.detail());
			releaseChunks();
		} else if (drove.placed()) {
			status = DrillCodes.DRIVING;
			lack = DrillCodes.LACK_NONE;
			wait = drill.ticksFor(ground);
			lockChunks();
		}
		setChanged();
		return drove;
	}

	public BlockPos cell(int across, int up, int ahead) {
		return new BlockPos(drill.worldX(across, ahead), drill.worldY(up, ahead), drill.worldZ(across, ahead));
	}

	private void serverTick() {
		if (level == null || level.isClientSide()) {
			return;
		}
		acceptBuckets();
		PumpBlockEntity pump = pump();
		if (pump != null) {
			pump.setDemand(running);
		}
		demand = running;
		if (!running) {
			if (drill.stopped() == Advance.Stop.NONE) {
				status = DrillCodes.IDLE;
			}
			releaseChunks();
			return;
		}
		if (wait > 0) {
			wait--;
			status = formed() && powered() && pump != null && pump.powered() ? DrillCodes.DRIVING : status;
			lockChunks();
			return;
		}
		stepNow();
	}

	private void toggleRun() {
		if (running) {
			running = false;
			status = DrillCodes.IDLE;
			releaseChunks();
			return;
		}
		drill.resetStop();
		running = true;
		status = DrillCodes.DRIVING;
		if (drill.advanceCount() == 0) {
			drill.placeOriginFrom(worldPosition.getX(), worldPosition.getY(), worldPosition.getZ());
			if (level != null) {
				wait = drill.ticksFor(new WorldGround(level, program.heading()));
			}
		}
	}

	private void turn() {
		if (drill.advanceCount() > 0) {
			return;
		}
		program.setHeading(program.heading().turn());
		if (level != null && getBlockState().hasProperty(TunnelDrillBlock.FACING)) {
			level.setBlock(worldPosition, getBlockState().setValue(TunnelDrillBlock.FACING, Direction.valueOf(program.heading().name())), 3);
		}
	}

	private void toggleGrade() {
		if (drill.advanceCount() > 0) {
			return;
		}
		program.setGrade(program.grade() == Grade.ONE_IN_TWO ? Grade.ONE_IN_ONE : Grade.ONE_IN_TWO);
	}

	private boolean formed() {
		return pump() != null;
	}

	private @Nullable PumpBlockEntity pump() {
		if (level == null || !getBlockState().hasProperty(TunnelDrillBlock.FACING)) {
			return null;
		}
		Direction facing = getBlockState().getValue(TunnelDrillBlock.FACING);
		BlockEntity behind = level.getBlockEntity(worldPosition.relative(facing.getOpposite()));
		return behind instanceof PumpBlockEntity pump ? pump : null;
	}

	private void acceptBuckets() {
		ItemStack water = slots.get(SLOT_WATER);
		if (water.getItem() == Items.WATER_BUCKET && tank < TANK_MAX) {
			tank++;
			slots.set(SLOT_WATER, new ItemStack(Items.BUCKET));
			setChanged();
		}
	}

	private void pullStock() {
		stock.setRail(count(SLOT_RAIL, TailingsBlocks.MINE_RAIL.asItem()));
		stock.setGravel(count(SLOT_GRAVEL, Items.GRAVEL));
		stock.setCobble(count(SLOT_COBBLE, Items.COBBLESTONE));
		stock.setSegments(count(SLOT_SEGMENTS, TailingsBlocks.LINING.asItem()));
		stock.setWater(tank);
	}

	private void pushStock() {
		setCount(SLOT_RAIL, TailingsBlocks.MINE_RAIL.asItem(), stock.rail());
		setCount(SLOT_GRAVEL, Items.GRAVEL, stock.gravel());
		setCount(SLOT_COBBLE, Items.COBBLESTONE, stock.cobble());
		setCount(SLOT_SEGMENTS, TailingsBlocks.LINING.asItem(), stock.segments());
		tank = Math.max(0, stock.water());
	}

	private int count(int slot, Item item) {
		ItemStack stack = slots.get(slot);
		return stack.getItem() == item ? stack.getCount() : 0;
	}

	private void setCount(int slot, Item item, int count) {
		if (count <= 0) {
			slots.set(slot, ItemStack.EMPTY);
			return;
		}
		int capped = Math.min(count, item.getDefaultMaxStackSize());
		ItemStack stack = slots.get(slot);
		if (stack.getItem() != item) {
			slots.set(slot, new ItemStack(item, capped));
		} else {
			stack.setCount(capped);
		}
	}

	private boolean roomForFace(WorldGround ground) {
		int solid = 0;
		for (int up = 0; up < DriveCourse.FACE; up++) {
			for (int across = 0; across < DriveCourse.FACE; across++) {
				Ground cell = ground.get(drill.worldX(across, 0), drill.worldY(up, 0), drill.worldZ(across, 0));
				if (cell.isMuck() || cell == Ground.ORE) {
					solid++;
				}
			}
		}
		return freeSpace() >= solid;
	}

	private int freeSpace() {
		int space = 0;
		for (int slot = INPUTS; slot < SLOTS; slot++) {
			ItemStack stack = slots.get(slot);
			space += stack.isEmpty() ? 64 : stack.getMaxStackSize() - stack.getCount();
		}
		return space;
	}

	private void flushPending() {
		List<String> left = new ArrayList<>();
		for (String line : pending) {
			int space = line.lastIndexOf(' ');
			if (space <= 0) {
				continue;
			}
			String id = line.substring(0, space);
			int count = Integer.parseInt(line.substring(space + 1));
			if (!push(id, count)) {
				left.add(id + " " + count);
			}
		}
		pending.clear();
		pending.addAll(left);
	}

	private boolean push(String id, int count) {
		Item item = BuiltInRegistries.ITEM.getValue(Identifier.parse(id));
		if (item == null || item == Items.AIR) {
			return true;
		}
		int left = count;
		for (int slot = INPUTS; slot < SLOTS && left > 0; slot++) {
			ItemStack there = slots.get(slot);
			if (there.isEmpty()) {
				int take = Math.min(left, item.getDefaultMaxStackSize());
				slots.set(slot, new ItemStack(item, take));
				left -= take;
			} else if (there.getItem() == item && there.getCount() < there.getMaxStackSize()) {
				int take = Math.min(left, there.getMaxStackSize() - there.getCount());
				there.grow(take);
				left -= take;
			}
		}
		return left <= 0;
	}

	private void lockChunks() {
		if (!(level instanceof ServerLevel server) || drill.advanceCount() < 0) {
			return;
		}
		if (drill.advanceCount() == 0) {
			drill.placeOriginFrom(worldPosition.getX(), worldPosition.getY(), worldPosition.getZ());
		}
		Set<Long> want = new HashSet<>();
		want.add(ChunkPos.pack(worldPosition));
		want.add(ChunkPos.pack(cell(0, 0, 0)));
		want.add(ChunkPos.pack(cell(0, 0, DriveCourse.PROBE_BLOCKS)));
		forced.removeIf(old -> {
			if (want.contains(old)) {
				return false;
			}
			server.setChunkForced(ChunkPos.getX(old), ChunkPos.getZ(old), false);
			return true;
		});
		for (long packed : want) {
			if (forced.add(packed)) {
				server.setChunkForced(ChunkPos.getX(packed), ChunkPos.getZ(packed), true);
			}
		}
	}

	private void releaseChunks() {
		if (!(level instanceof ServerLevel server)) {
			forced.clear();
			return;
		}
		for (long packed : forced) {
			server.setChunkForced(ChunkPos.getX(packed), ChunkPos.getZ(packed), false);
		}
		forced.clear();
	}

	public void dropContents() {
		if (level == null) {
			return;
		}
		for (ItemStack stack : slots) {
			if (!stack.isEmpty()) {
				Block.popResource(level, worldPosition, stack);
			}
		}
		slots.clear();
	}

	@Override
	public void setRemoved() {
		releaseChunks();
		super.setRemoved();
	}

	@Override
	public boolean hasPort(Direction face, Conductor role, Voltage voltage) {
		return voltage == Voltage.MEDIUM && role != Conductor.N && role != Conductor.G;
	}

	@Override
	public void build(Circuit circuit) {
		motor.build(circuit, worldPosition);
	}

	@Override
	public void apply(Circuit circuit, int ticks) {
		motor.apply(circuit, ticks, demand);
	}

	@Override
	public Component inspect() {
		return Component.literal("Tunnel drill (1 MW): " + DrillCodes.phrase(status, lack) + "; Y " + drill.floorY()
				+ "; " + drill.advanceCount() + " blocks; " + motor.describe());
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		ContainerHelper.saveAllItems(output, slots);
		output.putInt("advance", drill.advanceCount());
		output.putInt("flatDriven", drill.flatDriven());
		output.putInt("pumped", drill.pumped());
		output.putInt("originX", drill.originX());
		output.putInt("originY", drill.startFloorY());
		output.putInt("originZ", drill.originZ());
		output.putString("stop", drill.stopped().name());
		output.putString("detail", drill.detail());
		output.putString("rocks", stock.floorRockSave());
		output.putInt("target", program.targetY());
		output.putString("heading", program.heading().name());
		output.putInt("flatLength", program.flatLength());
		output.putString("grade", program.grade().name());
		output.putBoolean("stopOre", program.stopOnOre());
		output.putBoolean("stopCable", program.stopOnCable());
		output.putBoolean("stopLiquid", program.stopOnLiquid());
		output.putBoolean("stopEmpty", program.stopOnEmpty());
		output.putInt("cable", program.cableBlocks());
		output.putBoolean("running", running);
		output.putInt("wait", wait);
		output.putInt("tank", tank);
		output.putInt("status", status);
		output.putInt("lack", lack);
		output.putString("pending", String.join("|", pending));
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		slots.clear();
		loading = true;
		ContainerHelper.loadAllItems(input, slots);
		loading = false;
		program.setTargetY(input.getIntOr("target", program.targetY()));
		program.setHeading(enumOr(Heading.class, input.getStringOr("heading", program.heading().name()), program.heading()));
		program.setFlatLength(input.getIntOr("flatLength", program.flatLength()));
		program.setGrade(enumOr(Grade.class, input.getStringOr("grade", program.grade().name()), program.grade()));
		program.setStopOnOre(input.getBooleanOr("stopOre", true));
		program.setStopOnCable(input.getBooleanOr("stopCable", true));
		program.setStopOnLiquid(input.getBooleanOr("stopLiquid", true));
		program.setStopOnEmpty(input.getBooleanOr("stopEmpty", true));
		program.setCableBlocks(input.getIntOr("cable", program.cableBlocks()));
		stock.loadFloorRock(input.getStringOr("rocks", ""));
		drill.placeOrigin(input.getIntOr("originX", 0), input.getIntOr("originY", 0), input.getIntOr("originZ", 0));
		drill.restore(input.getIntOr("advance", 0), input.getIntOr("flatDriven", 0), input.getIntOr("pumped", 0),
				enumOr(Advance.Stop.class, input.getStringOr("stop", "NONE"), Advance.Stop.NONE), input.getStringOr("detail", ""));
		running = input.getBooleanOr("running", false);
		wait = input.getIntOr("wait", 0);
		tank = input.getIntOr("tank", 0);
		status = input.getIntOr("status", DrillCodes.IDLE);
		lack = input.getIntOr("lack", DrillCodes.LACK_NONE);
		pending.clear();
		String saved = input.getStringOr("pending", "");
		if (!saved.isEmpty()) {
			for (String line : saved.split("\\|")) {
				if (!line.isEmpty()) {
					pending.add(line);
				}
			}
		}
	}

	private static <T extends Enum<T>> T enumOr(Class<T> type, String name, T fallback) {
		try {
			return Enum.valueOf(type, name);
		} catch (IllegalArgumentException ex) {
			return fallback;
		}
	}

	@Override
	public int getContainerSize() {
		return slots.size();
	}

	@Override
	public boolean isEmpty() {
		return slots.stream().allMatch(ItemStack::isEmpty);
	}

	@Override
	public ItemStack getItem(int slot) {
		return slots.get(slot);
	}

	@Override
	public ItemStack removeItem(int slot, int count) {
		ItemStack out = ContainerHelper.removeItem(slots, slot, count);
		setChanged();
		return out;
	}

	@Override
	public ItemStack removeItemNoUpdate(int slot) {
		return ContainerHelper.takeItem(slots, slot);
	}

	@Override
	public void setItem(int slot, ItemStack stack) {
		slots.set(slot, stack);
		setChanged();
	}

	@Override
	public boolean stillValid(Player player) {
		return Container.stillValidBlockEntity(this, player);
	}

	@Override
	public void clearContent() {
		slots.clear();
	}

	@Override
	public int[] getSlotsForFace(Direction side) {
		if (side == Direction.UP) {
			return new int[] {SLOT_RAIL, SLOT_GRAVEL, SLOT_COBBLE, SLOT_SEGMENTS, SLOT_WATER};
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
		return switch (slot) {
			case SLOT_RAIL -> stack.getItem() == TailingsBlocks.MINE_RAIL.asItem();
			case SLOT_GRAVEL -> stack.getItem() == Items.GRAVEL;
			case SLOT_COBBLE -> stack.getItem() == Items.COBBLESTONE;
			case SLOT_SEGMENTS -> stack.getItem() == TailingsBlocks.LINING.asItem();
			case SLOT_WATER -> stack.getItem() == Items.WATER_BUCKET;
			default -> false;
		};
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
			return stack.getItem() == Items.BUCKET;
		}
		return slot >= INPUTS;
	}
}
