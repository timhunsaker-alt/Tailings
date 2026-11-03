package dev.wildcatter.tailings.block;

import dev.wildcatter.circuit.Circuit;
import dev.wildcatter.circuit.CircuitPart;
import dev.wildcatter.circuit.Conductor;
import dev.wildcatter.circuit.Motor;
import dev.wildcatter.energy.Inspectable;
import dev.wildcatter.energy.Voltage;
import dev.wildcatter.multiblock.Multiblock;
import dev.wildcatter.multiblock.MultiblockLayout;
import dev.wildcatter.tailings.TailingsBlocks;
import dev.wildcatter.tailings.plant.Concentrator;
import dev.wildcatter.tailings.plant.Machine;
import dev.wildcatter.tailings.plant.PlantPlan;
import dev.wildcatter.tailings.plant.Reagent;
import dev.wildcatter.tailings.plant.ReagentStock;
import dev.wildcatter.tailings.rock.Milling;
import dev.wildcatter.tailings.rock.NuggetBank;
import dev.wildcatter.tailings.rock.SizeClass;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Prediction;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

/**
 * A powered part of the crushing circuit or the flotation bank. The jaw and the conditioning tank are the controllers:
 * they check the layout and, when every motor is running, move one block of feed through the line.
 */
public class PlantBlockEntity extends BlockEntity implements CircuitPart, Inspectable, WorldlyContainer {
	private static final MultiblockLayout CRUSHING = new MultiblockLayout(PlantPlan.CRUSHING_CONTROLLER, new String[] {PlantPlan.row(PlantPlan.CRUSHING)});
	private static final MultiblockLayout FLOTATION = new MultiblockLayout(PlantPlan.FLOTATION_CONTROLLER, new String[] {PlantPlan.row(PlantPlan.FLOTATION)});

	private final NonNullList<ItemStack> slots = NonNullList.withSize(6, ItemStack.EMPTY);
	private final NuggetBank bank = new NuggetBank();
	private final ReagentStock reagents = new ReagentStock();
	private Motor motor;
	private boolean demand;
	private boolean forced;
	private boolean formed;
	private boolean loading;
	private String status = "idle";

	public PlantBlockEntity(BlockPos pos, BlockState state) {
		super(TailingsBlocks.PLANT, pos, state);
		Machine machine = machine();
		if (machine.watts() > 0) {
			Voltage voltage = machine.supply() == Machine.Supply.MEDIUM ? Voltage.MEDIUM : Voltage.INDUSTRIAL;
			motor = new Motor(true, voltage, machine.watts());
		}
	}

	public Machine machine() {
		return ((PlantMachineBlock) getBlockState().getBlock()).machine();
	}

	public static void tick(Level level, BlockPos pos, BlockState state, PlantBlockEntity plant) {
		plant.serverTick();
	}

	private void serverTick() {
		Machine machine = machine();
		if (machine == Machine.JAW || machine == Machine.CONDITIONER) {
			if (level.getGameTime() % 20 == 0) {
				formed = matches();
			}
			boolean work = formed && hasFeed();
			for (PlantBlockEntity part : parts()) {
				part.demand = work;
			}
			if (formed && level.getGameTime() % PlantPlan.CYCLE_TICKS == 0 && allRunning()) {
				if (machine == Machine.JAW) {
					crush();
				} else {
					floatOre();
				}
			}
		} else if (!formed && level.getGameTime() % PlantPlan.CYCLE_TICKS == 0 && running()) {
			local();
		}
	}

	private boolean matches() {
		List<PlantPlan.Station> plan = machine() == Machine.JAW ? PlantPlan.CRUSHING : PlantPlan.FLOTATION;
		char controller = machine() == Machine.JAW ? PlantPlan.CRUSHING_CONTROLLER : PlantPlan.FLOTATION_CONTROLLER;
		MultiblockLayout layout = machine() == Machine.JAW ? CRUSHING : FLOTATION;
		Map<Character, java.util.function.Predicate<BlockState>> legend = new HashMap<>();
		Map<Character, String> names = new HashMap<>();
		for (PlantPlan.Station station : plan) {
			legend.putIfAbsent(station.mark(), state -> state.getBlock() instanceof PlantMachineBlock block
					&& block.machine() == station.machine()
					&& state.getValue(PlantMachineBlock.PIECE) == 0
					&& state.getValue(PlantMachineBlock.FACING) == getBlockState().getValue(PlantMachineBlock.FACING));
			names.putIfAbsent(station.mark(), station.machine().displayName());
		}
		String missing = Multiblock.missing(level, worldPosition, getBlockState().getValue(PlantMachineBlock.FACING), layout, legend, names);
		status = missing == null ? "formed" : "missing " + missing;
		return missing == null && legend.containsKey(controller);
	}

	private List<PlantBlockEntity> parts() {
		List<PlantPlan.Station> plan = machine() == Machine.JAW ? PlantPlan.CRUSHING : PlantPlan.FLOTATION;
		int origin = 0;
		char controller = machine() == Machine.JAW ? PlantPlan.CRUSHING_CONTROLLER : PlantPlan.FLOTATION_CONTROLLER;
		for (PlantPlan.Station station : plan) {
			if (station.mark() == controller) {
				origin = station.x();
			}
		}
		Direction facing = getBlockState().getValue(PlantMachineBlock.FACING);
		int turns = PlantMachineBlock.turns(facing);
		java.util.ArrayList<PlantBlockEntity> found = new java.util.ArrayList<>();
		for (PlantPlan.Station station : plan) {
			int dx = station.x() - origin;
			int dz = 0;
			for (int i = 0; i < turns; i++) {
				int t = dx;
				dx = -dz;
				dz = t;
			}
			if (level.getBlockEntity(worldPosition.offset(dx, 0, dz)) instanceof PlantBlockEntity part) {
				found.add(part);
			}
		}
		return found;
	}

	private boolean allRunning() {
		for (PlantBlockEntity part : parts()) {
			if (part.machine().watts() > 0 && !part.running()) {
				status = part.machine().displayName() + " is not running: " + part.motor.describe();
				return false;
			}
		}
		return true;
	}

	private boolean hasFeed() {
		if (machine() == Machine.JAW) {
			PlantBlockEntity grizzly = part(Machine.GRIZZLY);
			return grizzly != null && Feeds.parse(grizzly.slots.get(0)) instanceof Feeds.Charge charge && charge.size() == null;
		}
		PlantBlockEntity tank = this;
		Feeds.Charge charge = Feeds.parse(tank.slots.get(0));
		return charge != null && charge.size() == SizeClass.FINES;
	}

	private @Nullable PlantBlockEntity part(Machine machine) {
		for (PlantBlockEntity part : parts()) {
			if (part.machine() == machine) {
				return part;
			}
		}
		return null;
	}

	private void crush() {
		PlantBlockEntity grizzly = part(Machine.GRIZZLY);
		PlantBlockEntity cyclone = part(Machine.CYCLONE);
		if (grizzly == null || cyclone == null) {
			return;
		}
		Feeds.Charge charge = Feeds.parse(grizzly.slots.get(0));
		if (charge == null || charge.size() != null) {
			status = "the grizzly wants unbroken rock";
			return;
		}
		ItemStack fines = Feeds.stack(SizeClass.FINES.itemId(charge.rock()), 1);
		if (fines.isEmpty() || !cyclone.roomFor(fines)) {
			status = "the cyclone discharge is full";
			return;
		}
		grizzly.slots.get(0).shrink(1);
		cyclone.add(fines);
		status = "crushing " + charge.rock().displayName() + " to fines";
		setChanged();
	}

	private void floatOre() {
		Feeds.Charge charge = Feeds.parse(slots.get(0));
		if (charge == null || charge.size() != SizeClass.FINES) {
			status = "the tank wants fines";
			return;
		}
		if (!reagents.covers(charge.rock())) {
			status = "short of " + reagents.missing(charge.rock());
			return;
		}
		PlantBlockEntity filter = part(Machine.FILTER);
		if (filter == null) {
			return;
		}
		List<Milling.Yield> yields = Concentrator.floatBank(charge.rock(), copyBank());
		if (!filter.fits(yields)) {
			status = "the filter is full";
			return;
		}
		NuggetBank next = copyBank();
		Concentrator.floatBank(charge.rock(), next);
		reagents.consume(charge.rock());
		slots.get(0).shrink(1);
		for (Milling.Yield yield : yields) {
			filter.add(Feeds.stack(yield.itemId(), yield.count()));
		}
		bank.load(next.save());
		status = "floating " + charge.rock().displayName();
		setChanged();
	}

	private void local() {
		Feeds.Charge charge = Feeds.parse(slots.get(0));
		SizeClass next = charge == null ? null : Concentrator.crush(machine(), charge.size());
		if (machine() == Machine.BELT && charge != null) {
			ItemStack moved = slots.get(0).copy();
			if (!roomFor(moved)) {
				return;
			}
			slots.set(0, ItemStack.EMPTY);
			add(moved);
			setChanged();
			return;
		}
		if (next == null || charge == null) {
			return;
		}
		ItemStack out = Feeds.stack(next.itemId(charge.rock()), 1);
		if (out.isEmpty() || !roomFor(out)) {
			return;
		}
		slots.get(0).shrink(1);
		add(out);
		setChanged();
	}

	public boolean running() {
		return forced || (motor != null && motor.output() > 0);
	}

	/** The game test turns the motors on without building a grid. */
	public void forceRunning() {
		forced = true;
		for (PlantBlockEntity part : parts()) {
			part.forced = true;
		}
	}

	public boolean formed() {
		return formed;
	}

	public boolean refresh() {
		formed = matches();
		return formed;
	}

	public String status() {
		return status;
	}

	public ItemStack product() {
		return slots.get(1);
	}

	public boolean offer(ItemStack stack) {
		Reagent reagent = reagent(stack);
		if (reagent != null && machine() == Machine.CONDITIONER) {
			for (int slot = 2; slot <= 5; slot++) {
				ItemStack there = slots.get(slot);
				if (there.isEmpty()) {
					setItem(slot, stack.split(1));
					return true;
				}
				if (ItemStack.isSameItemSameComponents(there, stack) && there.getCount() < there.getMaxStackSize()) {
					there.grow(1);
					stack.shrink(1);
					reagents.addKilogram(reagent);
					setChanged();
					return true;
				}
			}
			return false;
		}
		if (!takes(stack) || !slots.get(0).isEmpty()) {
			return false;
		}
		slots.set(0, stack.split(1));
		setChanged();
		return true;
	}

	public boolean give(Player player) {
		ItemStack stack = slots.get(1);
		if (stack.isEmpty()) {
			return false;
		}
		player.getInventory().placeItemBackInInventory(stack, Prediction.SERVER_ONLY);
		slots.set(1, ItemStack.EMPTY);
		setChanged();
		return true;
	}

	private boolean takes(ItemStack stack) {
		Feeds.Charge charge = Feeds.parse(stack);
		if (charge == null) {
			return false;
		}
		return switch (machine()) {
			case GRIZZLY, JAW -> charge.size() == null;
			case CONE -> charge.size() == SizeClass.CRUSHED;
			case SCREEN -> charge.size() == SizeClass.CRUSHED || charge.size() == SizeClass.GRAVEL;
			case BELT -> true;
			case CONDITIONER -> charge.size() == SizeClass.FINES;
			default -> false;
		};
	}

	private static @Nullable Reagent reagent(ItemStack stack) {
		if (stack.isEmpty()) {
			return null;
		}
		String id = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
		for (Reagent reagent : Reagent.values()) {
			if (reagent.itemId().equals(id)) {
				return reagent;
			}
		}
		return null;
	}

	private NuggetBank copyBank() {
		NuggetBank copy = new NuggetBank();
		copy.load(bank.save());
		return copy;
	}

	private boolean fits(List<Milling.Yield> yields) {
		for (Milling.Yield yield : yields) {
			ItemStack stack = Feeds.stack(yield.itemId(), yield.count());
			if (stack.isEmpty() || !roomFor(stack)) {
				return false;
			}
		}
		return true;
	}

	private boolean roomFor(ItemStack stack) {
		ItemStack there = slots.get(1);
		if (there.isEmpty()) {
			return true;
		}
		return ItemStack.isSameItemSameComponents(there, stack) && there.getCount() + stack.getCount() <= there.getMaxStackSize();
	}

	private void add(ItemStack stack) {
		ItemStack there = slots.get(1);
		if (there.isEmpty()) {
			slots.set(1, stack);
		} else if (ItemStack.isSameItemSameComponents(there, stack)) {
			there.grow(stack.getCount());
		}
	}

	@Override
	public boolean hasPort(Direction face, Conductor role, Voltage voltage) {
		return motor != null && voltage == motor.voltage() && role != Conductor.N && role != Conductor.G;
	}

	@Override
	public void build(Circuit circuit) {
		if (motor != null) {
			motor.build(circuit, worldPosition);
		}
	}

	@Override
	public void apply(Circuit circuit, int ticks) {
		if (motor != null) {
			boolean work = demand || !slots.get(0).isEmpty();
			motor.apply(circuit, ticks, work);
		}
	}

	@Override
	public Component inspect() {
		String power = motor == null ? "no motor" : motor.describe();
		return Component.literal(machine().displayName() + " (" + machine().watts() + " W): " + status + "; " + power);
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		ContainerHelper.saveAllItems(output, slots);
		output.putString("bank", bank.save());
		output.putString("reagents", reagents.save());
		output.putString("status", status);
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		slots.clear();
		loading = true;
		ContainerHelper.loadAllItems(input, slots);
		loading = false;
		bank.load(input.getStringOr("bank", ""));
		reagents.load(input.getStringOr("reagents", ""));
		status = input.getStringOr("status", "idle");
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
		if (!loading && slot >= 2 && machine() == Machine.CONDITIONER) {
			Reagent reagent = reagent(stack);
			if (reagent != null) {
				for (int i = 0; i < stack.getCount(); i++) {
					reagents.addKilogram(reagent);
				}
			}
		}
		setChanged();
	}

	@Override
	public boolean stillValid(Player player) {
		return false;
	}

	@Override
	public void clearContent() {
		slots.clear();
	}

	@Override
	public int[] getSlotsForFace(Direction side) {
		return side == Direction.UP ? new int[] {0, 2, 3, 4, 5} : new int[] {1};
	}

	@Override
	public boolean canPlaceItem(int slot, ItemStack stack) {
		if (slot == 0) {
			return takes(stack);
		}
		return slot >= 2 && machine() == Machine.CONDITIONER && reagent(stack) != null;
	}

	@Override
	public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) {
		return side == Direction.UP && canPlaceItem(slot, stack);
	}

	@Override
	public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
		return slot == 1 && side != Direction.UP;
	}
}
