package dev.wildcatter.tailings.block;

import dev.wildcatter.circuit.Circuit;
import dev.wildcatter.circuit.CircuitPart;
import dev.wildcatter.circuit.Conductor;
import dev.wildcatter.circuit.Motor;
import dev.wildcatter.energy.Inspectable;
import dev.wildcatter.multiblock.BigPartEntity;
import dev.wildcatter.energy.Voltage;
import dev.wildcatter.multiblock.BigParts;
import dev.wildcatter.tailings.TailingsBlocks;
import dev.wildcatter.tailings.plant.Concentrator;
import dev.wildcatter.tailings.plant.Machine;
import dev.wildcatter.tailings.plant.PlantPlan;
import dev.wildcatter.tailings.plant.Reagent;
import dev.wildcatter.tailings.plant.ReagentStock;
import dev.wildcatter.tailings.rock.Milling;
import dev.wildcatter.tailings.rock.NuggetBank;
import dev.wildcatter.tailings.rock.SizeClass;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Prediction;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.Containers;
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
 * The main cell of a plant or dump-station part (docs/multiblocks/crushing-circuit.md, flotation-bank.md). The jaw and the
 * conditioning tank lead a line: every 20 ticks each finds the parts joined to it, by touching or through a belt run with a
 * drive, and when its line is whole and every motor in it runs it moves one block of feed through. Until R4 that move is the
 * old shortcut, grizzly to fines at the cyclones and fines to the 75 % ladder at the filter.
 */
public class PlantBlockEntity extends BlockEntity implements CircuitPart, Inspectable, WorldlyContainer, BigPartEntity {
	/** What the jaw needs joined to it, besides itself. */
	static final List<Machine> CRUSHING = List.of(Machine.GRIZZLY, Machine.SCREEN, Machine.CONE, Machine.BALL_MILL, Machine.CYCLONE);
	/** What the conditioning tank needs joined to it. Touching stands in for R4's pipes. */
	static final List<Machine> FLOTATION = List.of(Machine.FLOTATION_BANK, Machine.BLOWER, Machine.THICKENER, Machine.FILTER);
	/** A line is never searched past this many cells. */
	private static final int SEARCH_CELLS = 8192;

	private boolean grown;
	private final NonNullList<ItemStack> slots = NonNullList.withSize(6, ItemStack.EMPTY);
	private final NuggetBank bank = new NuggetBank();
	private final ReagentStock reagents = new ReagentStock();
	private List<PlantBlockEntity> line = List.of();
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
		if (machine.waitsForR4()) {
			status = "joins the process in R4";
		}
	}

	public Machine machine() {
		return ((PlantMachineBlock) getBlockState().getBlock()).machine();
	}

	private boolean leads() {
		return machine() == Machine.JAW || machine() == Machine.CONDITIONER;
	}

	public static void tick(Level level, BlockPos pos, BlockState state, PlantBlockEntity plant) {
		plant.serverTick();
	}

	private void serverTick() {
		if (!leads()) {
			return;
		}
		if (level.getGameTime() % 20 == 0) {
			formed = matches();
		}
		boolean work = formed && hasFeed();
		for (PlantBlockEntity part : parts()) {
			part.demand = work;
		}
		if (formed && level.getGameTime() % PlantPlan.CYCLE_TICKS == 0 && allRunning()) {
			if (machine() == Machine.JAW) {
				crush();
			} else {
				floatOre();
			}
		}
	}

	/** Finds the line joined to this part and checks it is whole; the status names what is missing. */
	private boolean matches() {
		List<Machine> needs = machine() == Machine.JAW ? CRUSHING : FLOTATION;
		List<PlantBlockEntity> joined = joined();
		List<PlantBlockEntity> mine = new ArrayList<>();
		mine.add(this);
		StringBuilder missing = new StringBuilder();
		for (Machine need : needs) {
			PlantBlockEntity found = null;
			for (PlantBlockEntity part : joined) {
				if (part.machine() == need) {
					found = part;
					break;
				}
			}
			if (found == null) {
				missing.append(missing.length() > 0 ? ", " : "").append(need.displayName());
			} else {
				mine.add(found);
			}
		}
		for (PlantBlockEntity part : joined) {
			if (part.machine() == Machine.CONVEYOR_DRIVE) {
				mine.add(part);
			}
		}
		if (missing.isEmpty() && machine() == Machine.JAW && !onTop(find(mine, Machine.GRIZZLY), this)) {
			missing.append("Grizzly on top of the jaw");
		}
		if (missing.isEmpty() && machine() == Machine.CONDITIONER && !touching(find(mine, Machine.BLOWER), find(mine, Machine.FLOTATION_BANK))) {
			missing.append("Air Blower touching the bank");
		}
		line = List.copyOf(mine);
		status = missing.isEmpty() ? "formed" : "NOT BUILT: needs " + missing;
		return missing.isEmpty();
	}

	private static @Nullable PlantBlockEntity find(List<PlantBlockEntity> parts, Machine machine) {
		for (PlantBlockEntity part : parts) {
			if (part.machine() == machine) {
				return part;
			}
		}
		return null;
	}

	private List<BlockPos> cells() {
		return BigParts.cells(worldPosition, getBlockState());
	}

	/** Some cell of {@code upper} sits directly on a cell of {@code lower}. */
	private static boolean onTop(@Nullable PlantBlockEntity upper, PlantBlockEntity lower) {
		if (upper == null) {
			return false;
		}
		List<BlockPos> below = lower.cells();
		for (BlockPos cell : upper.cells()) {
			if (below.contains(cell.below())) {
				return true;
			}
		}
		return false;
	}

	private static boolean touching(@Nullable PlantBlockEntity a, @Nullable PlantBlockEntity b) {
		if (a == null || b == null) {
			return false;
		}
		List<BlockPos> other = b.cells();
		for (BlockPos cell : a.cells()) {
			for (Direction side : Direction.values()) {
				if (other.contains(cell.relative(side))) {
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * Every plant part joined to this one: parts that touch, and parts at either end of a belt run that a conveyor drive touches.
	 * A belt run is all the belt blocks joined to each other; one with no drive carries nothing and joins nothing.
	 */
	private List<PlantBlockEntity> joined() {
		Set<BlockPos> seen = new HashSet<>(cells());
		ArrayDeque<BlockPos> queue = new ArrayDeque<>(cells());
		List<PlantBlockEntity> found = new ArrayList<>();
		Set<BlockPos> mains = new HashSet<>();
		mains.add(worldPosition);
		while (!queue.isEmpty() && seen.size() < SEARCH_CELLS) {
			BlockPos cell = queue.removeFirst();
			for (Direction side : Direction.values()) {
				BlockPos next = cell.relative(side);
				if (seen.contains(next)) {
					continue;
				}
				BlockState state = level.getBlockState(next);
				if (state.getBlock() instanceof PlantMachineBlock) {
					BlockPos main = BigParts.main(next, state);
					if (mains.add(main) && level.getBlockEntity(main) instanceof PlantBlockEntity part) {
						found.add(part);
						for (BlockPos its : part.cells()) {
							if (seen.add(its)) {
								queue.add(its);
							}
						}
					}
				} else if (state.getBlock() instanceof BeltConveyorBlock) {
					List<BlockPos> run = BeltConveyorBlock.run(level, next);
					seen.addAll(run);
					if (BeltConveyorBlock.driven(level, run)) {
						queue.addAll(run);
					}
				}
			}
		}
		return found;
	}

	private List<PlantBlockEntity> parts() {
		return line;
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
		Feeds.Charge charge = Feeds.parse(slots.get(0));
		return charge != null && charge.size() == SizeClass.FINES;
	}

	private @Nullable PlantBlockEntity part(Machine machine) {
		return find(parts(), machine);
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
		trimReagents();
		slots.get(0).shrink(1);
		for (Milling.Yield yield : yields) {
			filter.add(Feeds.stack(yield.itemId(), yield.count()));
		}
		bank.load(next.save());
		status = "floating " + charge.rock().displayName();
		setChanged();
	}

	public boolean running() {
		return forced || (motor != null && motor.output() > 0);
	}

	/** The game test turns the motors on without building a grid. */
	public void forceRunning() {
		forced = true;
		if (leads() && level != null) {
			formed = matches();
		}
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
			case CONDITIONER -> charge.size() == SizeClass.FINES;
			default -> false;
		};
	}

	/** Takes out the reagent items whose kilogram has been dosed, so the slots never fill with spent reagent. */
	private void trimReagents() {
		for (Reagent reagent : Reagent.values()) {
			int extra = -reagents.items(reagent);
			for (int slot = 2; slot <= 5; slot++) {
				if (reagent(slots.get(slot)) == reagent) {
					extra += slots.get(slot).getCount();
				}
			}
			for (int slot = 2; slot <= 5 && extra > 0; slot++) {
				ItemStack there = slots.get(slot);
				if (reagent(there) == reagent) {
					int take = Math.min(extra, there.getCount());
					there.shrink(take);
					extra -= take;
				}
			}
		}
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
		return false;
	}

	/** The motor box is on the west face of the main cell, turned with the part. */
	@Override
	public boolean hasPortAt(BlockPos cell, Direction face, Conductor role, Voltage voltage) {
		return motor != null && cell.equals(worldPosition) && face == TailingsPartBlock.boxFace(getBlockState()) && voltage == motor.voltage()
				&& role != Conductor.N;
	}

	@Override
	public void build(Circuit circuit) {
		if (motor != null) {
			motor.build(circuit, worldPosition, List.of(TailingsPartBlock.boxFace(getBlockState())));
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
		output.putBoolean("grown", grown);
		ContainerHelper.saveAllItems(output, slots);
		output.putString("bank", bank.save());
		output.putString("reagents", reagents.save());
		output.putString("status", status);
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		grown = input.getBooleanOr("grown", false);
		slots.clear();
		loading = true;
		ContainerHelper.loadAllItems(input, slots);
		loading = false;
		bank.load(input.getStringOr("bank", ""));
		reagents.load(input.getStringOr("reagents", ""));
		// a tank saved by an older build may hold spent reagent items
		trimReagents();
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
		return Container.stillValidBlockEntity(this, player);
	}

	/** Breaking the machine, any way at all, drops what it holds. */
	@Override
	public void preRemoveSideEffects(BlockPos pos, BlockState state) {
		super.preRemoveSideEffects(pos, state);
		if (level != null) {
			Containers.dropContents(level, pos, this);
		}
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
