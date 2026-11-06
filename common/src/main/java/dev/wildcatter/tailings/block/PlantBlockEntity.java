package dev.wildcatter.tailings.block;

import dev.wildcatter.block.Assayed;
import dev.wildcatter.block.FluidNode;
import dev.wildcatter.block.Lines;
import dev.wildcatter.block.WaterUser;
import dev.wildcatter.circuit.Circuit;
import dev.wildcatter.circuit.CircuitPart;
import dev.wildcatter.circuit.Conductor;
import dev.wildcatter.circuit.Motor;
import dev.wildcatter.energy.Inspectable;
import dev.wildcatter.energy.Voltage;
import dev.wildcatter.fluid.Product;
import dev.wildcatter.fluid.SlurryAssay;
import dev.wildcatter.fluid.SlurryHold;
import dev.wildcatter.multiblock.BigPartEntity;
import dev.wildcatter.multiblock.BigParts;
import dev.wildcatter.tailings.TailingsBlocks;
import dev.wildcatter.tailings.block.stage.Stage;
import dev.wildcatter.tailings.plant.FilterCake;
import dev.wildcatter.tailings.plant.Machine;
import dev.wildcatter.tailings.plant.PlantRates;
import dev.wildcatter.tailings.plant.Reagent;
import dev.wildcatter.tailings.plant.ReagentStock;
import dev.wildcatter.tailings.plant.RockLoad;
import dev.wildcatter.tailings.rock.Rock;
import dev.wildcatter.tailings.rock.SizeClass;
import java.util.List;
import java.util.function.Predicate;
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
 * The main cell of a plant or dump-station part (docs/multiblocks/crushing-circuit.md, flotation-bank.md). Each part runs on its
 * own: its {@link Stage} moves rock or slurry while its motor runs, it has feed, and its product has somewhere to go. Dry rock is
 * held in kg ({@link RockLoad}); slurry for the pipe in a Wildcatter {@link SlurryHold}, with its assay. Items exist only where the
 * player hands rock in or takes a product out.
 */
public class PlantBlockEntity extends BlockEntity implements CircuitPart, Inspectable, WorldlyContainer, BigPartEntity, FluidNode, Assayed,
		WaterUser, dev.wildcatter.kinetic.Sounding {
	/** How long a part keeps sounding after its last work, so a stage that works in bursts does not flutter. */
	private static final int SOUND_HOLD_TICKS = 20;
	private boolean sounding;
	private int soundHold;
	private boolean grown;
	private final NonNullList<ItemStack> slots = NonNullList.withSize(6, ItemStack.EMPTY);
	/** The pressure filter's cake, paid out as nuggets. */
	private final FilterCake cake = new FilterCake();
	private final ReagentStock reagents = new ReagentStock();
	/** Dry rock waiting to be worked, or, in a conveyor drive, the load on its run. */
	private final RockLoad hold = new RockLoad();
	/** Rock worked but not yet paid out as an item (the screen's sand for its hopper). */
	private final RockLoad made = new RockLoad();
	/** What it puts on steel pipe. */
	private final SlurryHold out = new SlurryHold();
	/** The ball mill's cyclone underflow (dry kg with its assay, ground before new feed); a flotation part's inlet buffer; the pond's tails. */
	private final SlurryHold back = new SlurryHold();
	/** An ore bin's rock items, by charge, past the one in its intake slot. */
	private final java.util.Map<String, Integer> binned = new java.util.LinkedHashMap<>();
	/** An ore sorter's screen: the type each side takes, and the waste side. */
	private final dev.wildcatter.tailings.haul.Sorter sorter = new dev.wildcatter.tailings.haul.Sorter();
	private double water;
	private Motor motor;
	private boolean demand;
	private boolean forced;
	private boolean loading;
	private String status = "idle";
	/** A conveyor drive's last scan of its run (not saved). */
	public @Nullable Object run;

	private static final List<Product> SLURRIES = List.of(Product.MILL_DISCHARGE, Product.FINES_SLURRY, Product.CONDITIONED_SLURRY,
			Product.CONCENTRATE_SLURRY, Product.THICKENED_CONCENTRATE, Product.TAILS_SLURRY);

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

	public Stage stage() {
		return Stage.of(machine());
	}

	public static void tick(Level level, BlockPos pos, BlockState state, PlantBlockEntity plant) {
		plant.serverTick();
	}

	private void serverTick() {
		demand = false;
		intake();
		stage().tick(this);
		soundHold = running() && demand ? SOUND_HOLD_TICKS : Math.max(0, soundHold - 1);
		boolean now = soundHold > 0;
		if (now != sounding) {
			sounding = now;
			level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), net.minecraft.world.level.block.Block.UPDATE_CLIENTS);
		}
	}

	@Override
	public net.minecraft.sounds.SoundEvent loop() {
		return sounding ? dev.wildcatter.tailings.TailingsSounds.loop(machine()) : null;
	}

	@Override
	public float soundPitch() {
		return 1F;
	}

	@Override
	public float soundVolume() {
		return sounding ? dev.wildcatter.tailings.TailingsSounds.volume(machine()) : 0;
	}

	@Override
	public net.minecraft.network.protocol.Packet<net.minecraft.network.protocol.game.ClientGamePacketListener> getUpdatePacket() {
		return net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket.create(this);
	}

	/** All the client needs of a part: whether to sound. */
	@Override
	public net.minecraft.nbt.CompoundTag getUpdateTag(net.minecraft.core.HolderLookup.Provider registries) {
		net.minecraft.nbt.CompoundTag tag = new net.minecraft.nbt.CompoundTag();
		tag.putBoolean("sounding", sounding);
		return tag;
	}

	/** A hopper's item in the intake slot goes into the hold (or an ore bin's store) as soon as there is room. */
	private void intake() {
		ItemStack in = slots.get(0);
		Feeds.Charge charge = Feeds.parse(in);
		if (charge == null) {
			return;
		}
		if (machine() == Machine.ORE_BIN) {
			if (binCount() <= PlantRates.BIN_ITEMS) {
				binned.merge(net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(in.getItem()).toString(), in.getCount(), Integer::sum);
				slots.set(0, ItemStack.EMPTY);
				setChanged();
			}
		} else if (accepts(charge.rock(), charge.size()) && room() >= PlantRates.BLOCK_KG) {
			in.shrink(1);
			hold.add(charge.rock(), charge.size(), PlantRates.BLOCK_KG);
			setChanged();
		}
	}

	// ---- what stages use ---------------------------------------------------------------------------------------------------------

	/** The fraction of its rated rate the part runs at now: its motor's output (an unpowered part runs whenever it has work). */
	public double strength() {
		if (forced || motor == null) {
			return 1;
		}
		return motor.output();
	}

	public boolean running() {
		return forced || (motor != null && motor.output() > 0);
	}

	/** It has work: its motor should run. */
	public void want() {
		demand = true;
	}

	public void say(String status) {
		if (!status.equals(this.status)) {
			this.status = status;
			setChanged();
		}
	}

	/** True one tick in 20, spread over parts by position: for scans that need not run every tick. */
	public boolean scanDue() {
		return level != null && (level.getGameTime() + (worldPosition.asLong() & 15)) % 20 == 0;
	}

	public RockLoad hold() {
		return hold;
	}

	public RockLoad made() {
		return made;
	}

	public SlurryHold out() {
		return out;
	}

	public SlurryHold back() {
		return back;
	}

	public double water() {
		return water;
	}

	public void useWater(double litres) {
		water = Math.max(0, water - litres);
		setChanged();
	}

	public List<BlockPos> cells() {
		return BigParts.cells(worldPosition, getBlockState());
	}

	/** The faces of the cells that match {@code where} (own frame) on {@code localSide}. */
	public List<Lines.Face> faces(Predicate<int[]> where, Direction localSide) {
		return PartFrame.faces(worldPosition, getBlockState(), where, localSide);
	}

	/** Every outward face on {@code localSide}. */
	public List<Lines.Face> side(Direction localSide) {
		return PartFrame.side(worldPosition, getBlockState(), localSide);
	}

	/** How many kg of dry rock it can still take. */
	public double room() {
		return Math.max(0, stage().holdKg() - hold.total());
	}

	public boolean accepts(Rock rock, @Nullable SizeClass size) {
		return stage().accepts(rock, size);
	}

	/** Takes rock into its hold; the caller checked {@link #room} and {@link #accepts}. */
	public void receive(RockLoad load) {
		hold.add(load);
		setChanged();
	}

	/** A part of kind {@code machine} touching this one, or null. */
	public @Nullable PlantBlockEntity touching(Machine machine) {
		List<BlockPos> mine = cells();
		for (BlockPos cell : mine) {
			for (Direction side : Direction.values()) {
				BlockPos next = cell.relative(side);
				if (!mine.contains(next) && BigParts.entity(level, next) instanceof PlantBlockEntity part && part.machine() == machine) {
					return part;
				}
			}
		}
		return null;
	}

	/** A part of kind {@code machine} right under this one's bottom, or null. */
	public @Nullable PlantBlockEntity under(Machine machine) {
		List<BlockPos> mine = cells();
		for (BlockPos cell : mine) {
			BlockPos below = cell.below();
			if (!mine.contains(below) && BigParts.entity(level, below) instanceof PlantBlockEntity part && part.machine() == machine) {
				return part;
			}
		}
		return null;
	}

	/** Where rock goes out of these faces: a belt run's load, or a part that takes it. */
	public interface Sink {
		/** How many kg of {@code what} it can take now (0 if it does not take all of it). */
		double room(RockLoad what);

		void put(RockLoad load);
	}

	/** The first place out of {@code faces} that takes rock: a moving belt, or a part touching that face. */
	public @Nullable Sink sink(List<Lines.Face> faces) {
		for (Lines.Face face : faces) {
			Sink there = sinkAt(face.pipe(), this);
			if (there != null) {
				return there;
			}
		}
		return null;
	}

	/** What takes rock at {@code pos}, never {@code except}: a belt's run (through its drive) or a part. */
	public @Nullable Sink sinkAt(BlockPos pos, @Nullable PlantBlockEntity except) {
		if (level.getBlockEntity(pos) instanceof BeltBlockEntity belt) {
			BlockPos at = belt.drive();
			if (at != null && level.getBlockEntity(at) instanceof PlantBlockEntity drive && drive != except) {
				return new Sink() {
					@Override
					public double room(RockLoad what) {
						return belt.moving() ? Math.max(0, PlantRates.BELT_HOLD_KG - drive.hold.total()) : 0;
					}

					@Override
					public void put(RockLoad load) {
						drive.receive(load);
					}
				};
			}
			return null;
		}
		if (BigParts.entity(level, pos) instanceof PlantBlockEntity part && part != except && part != this) {
			return new Sink() {
				@Override
				public double room(RockLoad what) {
					boolean[] ok = {true};
					what.forEach((rock, size, kg) -> ok[0] &= part.accepts(rock, size));
					return ok[0] ? part.room() : 0;
				}

				@Override
				public void put(RockLoad load) {
					part.receive(load);
				}
			};
		}
		return null;
	}

	/** Puts a made item in the product slot, for a hopper under the part or a hand; false if it does not fit. */
	public boolean emit(ItemStack stack) {
		if (stack.isEmpty() || !roomFor(stack)) {
			return false;
		}
		add(stack);
		setChanged();
		return true;
	}

	public boolean roomForItem(ItemStack stack) {
		return roomFor(stack);
	}

	/** An ore bin's items, intake slot first: the count of each rock item id it holds. */
	public java.util.Map<String, Integer> binned() {
		return binned;
	}

	public int binCount() {
		int n = slots.get(0).getCount();
		for (int c : binned.values()) {
			n += c;
		}
		return n;
	}

	public NonNullList<ItemStack> slots() {
		return slots;
	}

	/** The game test turns the motors on without building a grid, or off again. */
	public void forceRunning() {
		forced = true;
	}

	public void stopForced() {
		forced = false;
	}

	public String status() {
		return status;
	}

	public ItemStack product() {
		return slots.get(1);
	}

	// ---- hands and hoppers -------------------------------------------------------------------------------------------------------

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
		Feeds.Charge charge = Feeds.parse(stack);
		if (charge == null) {
			return false;
		}
		if (machine() == Machine.ORE_BIN) {
			if (binCount() >= PlantRates.BIN_ITEMS) {
				return false;
			}
			String id = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
			binned.merge(id, 1, Integer::sum);
			stack.shrink(1);
			setChanged();
			return true;
		}
		if (!accepts(charge.rock(), charge.size()) || room() < PlantRates.BLOCK_KG) {
			return false;
		}
		stack.shrink(1);
		hold.add(charge.rock(), charge.size(), PlantRates.BLOCK_KG);
		setChanged();
		return true;
	}

	public boolean give(Player player) {
		boolean gave = false;
		for (int slot : machine() == Machine.FILTER ? new int[] {1, 2} : new int[] {1}) {
			ItemStack stack = slots.get(slot);
			if (!stack.isEmpty()) {
				player.getInventory().placeItemBackInInventory(stack, Prediction.SERVER_ONLY);
				slots.set(slot, ItemStack.EMPTY);
				gave = true;
			}
		}
		if (gave) {
			setChanged();
		}
		return gave;
	}

	public dev.wildcatter.tailings.haul.Sorter sorter() {
		return sorter;
	}

	/** A click on a sorter's side with a rock item sets that side to take it; false for anything else. */
	public boolean setSide(Direction face, ItemStack stack) {
		if (machine() != Machine.ORE_SORTER || !face.getAxis().isHorizontal() || Feeds.parse(stack) == null) {
			return false;
		}
		sorter.set((face.get2DDataValue() + 2) % 4, net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem()).toString());
		say("the " + face.getName() + " side takes " + stack.getHoverName().getString());
		return true;
	}

	/** A sneaking click with an empty hand on a sorter's side sets it as the waste side. */
	public boolean setWasteSide(Direction face) {
		if (machine() != Machine.ORE_SORTER || !face.getAxis().isHorizontal()) {
			return false;
		}
		sorter.setWaste((face.get2DDataValue() + 2) % 4);
		say("the " + face.getName() + " side takes the waste");
		return true;
	}

	public ReagentStock reagents() {
		return reagents;
	}

	public FilterCake cake() {
		return cake;
	}

	/** Puts {@code stack} into the first of {@code into} that takes it whole; false if none does. */
	public boolean emitTo(int[] into, ItemStack stack) {
		for (int slot : into) {
			ItemStack there = slots.get(slot);
			if (there.isEmpty()) {
				slots.set(slot, stack.copy());
				setChanged();
				return true;
			}
			if (ItemStack.isSameItemSameComponents(there, stack) && there.getCount() + stack.getCount() <= there.getMaxStackSize()) {
				there.grow(stack.getCount());
				setChanged();
				return true;
			}
		}
		return false;
	}

	/** How many of {@code like} are in these slots. */
	public int countIn(int[] in, ItemStack like) {
		int n = 0;
		for (int slot : in) {
			if (ItemStack.isSameItemSameComponents(slots.get(slot), like)) {
				n += slots.get(slot).getCount();
			}
		}
		return n;
	}

	/** Takes {@code count} of {@code like} out of these slots. */
	public void removeFrom(int[] in, ItemStack like, int count) {
		for (int slot : in) {
			if (count > 0 && ItemStack.isSameItemSameComponents(slots.get(slot), like)) {
				int take = Math.min(count, slots.get(slot).getCount());
				slots.get(slot).shrink(take);
				count -= take;
			}
		}
		setChanged();
	}

	/** Takes out the reagent items whose kilogram has been dosed, so the slots never fill with spent reagent. */
	public void trimReagents() {
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

	// ---- steel pipe and water ----------------------------------------------------------------------------------------------------

	/** {@code side} (in the world) seen in the part's own frame. */
	private Direction local(Direction side) {
		Direction out = side;
		for (int i = 0; side.getAxis().isHorizontal() && i < BigParts.turns(getBlockState()); i++) {
			out = out.getCounterClockWise();
		}
		return out;
	}

	@Override
	public @Nullable Product offers(BlockPos cell, Direction side) {
		return stage().outlet(this, PartFrame.local(worldPosition, getBlockState(), cell), local(side));
	}

	@Override
	public double available(BlockPos cell, Direction side, Product product, Pull pull) {
		return offers(cell, side) == product && pull != Pull.FLARE ? out.held(product) : 0;
	}

	@Override
	public double take(BlockPos cell, Direction side, Product product, double max, Pull pull) {
		if (available(cell, side, product, pull) <= 0) {
			return 0;
		}
		double got = out.give(product, max);
		setChanged();
		return got;
	}

	@Override
	public SlurryAssay assayOf(Product product) {
		return out.assayOf(product);
	}

	@Override
	public double waterLevel() {
		double hold = stage().waterHold();
		return hold <= 0 ? 1 : water / hold;
	}

	@Override
	public int addWater(int litres) {
		int fits = (int) Math.max(0, Math.min(litres, stage().waterHold() - water));
		water += fits;
		if (fits > 0) {
			setChanged();
		}
		return fits;
	}

	// ---- the motor ---------------------------------------------------------------------------------------------------------------

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
			motor.apply(circuit, ticks, demand);
		}
	}

	@Override
	public Component inspect() {
		String power = motor == null ? "no motor" : motor.describe();
		StringBuilder held = new StringBuilder();
		if (!hold.isEmpty()) {
			held.append(String.format("; holds %.0f kg of rock", hold.total()));
		}
		for (Product p : SLURRIES) {
			for (SlurryHold h : List.of(back, out)) {
				if (h.held(p) > 0.5 && (h == out || machine() != Machine.BALL_MILL)) {
					held.append(String.format("; %.0f kg of %s (%s)", h.held(p), p.label(), h.assayOf(p).describe()));
				}
			}
		}
		if (stage().waterHold() > 0) {
			held.append(String.format("; water %.0f/%.0f L", water, stage().waterHold()));
		}
		if (machine() == Machine.ORE_BIN) {
			held.append("; ").append(binCount()).append(" of ").append(PlantRates.BIN_ITEMS).append(" items");
		}
		if (machine() == Machine.ORE_SORTER) {
			String[] names = {"north", "east", "south", "west"};
			for (int side = 0; side < names.length; side++) {
				String set = side == sorter.waste() ? "waste" : sorter.product(side);
				if (set != null) {
					held.append("; ").append(names[side]).append(": ").append(set);
				}
			}
		}
		return Component.literal(machine().displayName() + " (" + machine().watts() / 1000.0 + " kW): " + status + held + "; " + power);
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
		output.putString("cake", cake.save());
		output.putString("reagents", reagents.save());
		output.putString("status", status);
		output.putString("hold", hold.save());
		output.putString("made", made.save());
		output.putString("out", out.save());
		output.putString("back", back.save());
		output.putDouble("water", water);
		output.putString("sort", sorter.save());
		StringBuilder bin = new StringBuilder();
		binned.forEach((id, n) -> bin.append(bin.length() == 0 ? "" : ",").append(id).append('=').append(n));
		output.putString("binned", bin.toString());
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		sounding = input.getBooleanOr("sounding", false);
		grown = input.getBooleanOr("grown", false);
		slots.clear();
		loading = true;
		ContainerHelper.loadAllItems(input, slots);
		loading = false;
		cake.load(input.getStringOr("cake", ""));
		reagents.load(input.getStringOr("reagents", ""));
		// a tank saved by an older build may hold spent reagent items
		trimReagents();
		status = input.getStringOr("status", "idle");
		hold.load(input.getStringOr("hold", ""));
		made.load(input.getStringOr("made", ""));
		out.load(input.getStringOr("out", ""));
		back.load(input.getStringOr("back", ""));
		water = input.getDoubleOr("water", 0);
		sorter.load(input.getStringOr("sort", ""));
		binned.clear();
		for (String part : input.getStringOr("binned", "").split(",")) {
			int eq = part.lastIndexOf('=');
			if (eq > 0) {
				try {
					binned.put(part.substring(0, eq), Integer.parseInt(part.substring(eq + 1)));
				} catch (NumberFormatException ex) {
					// left out
				}
			}
		}
	}

	// ---- the container: slot 0 the intake, slot 1 the product, 2-5 reagents ------------------------------------------------------

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

	/** Breaking the part, any way at all, drops what it holds as items: its slots, and an ore bin's rock. */
	@Override
	public void preRemoveSideEffects(BlockPos pos, BlockState state) {
		super.preRemoveSideEffects(pos, state);
		if (level != null) {
			Containers.dropContents(level, pos, this);
			binned.forEach((id, n) -> {
				for (int left = n; left > 0; left -= 64) {
					Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), Feeds.stack(id, Math.min(64, left)));
				}
			});
			binned.clear();
		}
	}

	@Override
	public void clearContent() {
		slots.clear();
	}

	@Override
	public int[] getSlotsForFace(Direction side) {
		if (machine() == Machine.FILTER) {
			return side == Direction.UP ? new int[0] : new int[] {1, 2};
		}
		return side == Direction.UP ? new int[] {0, 2, 3, 4, 5} : new int[] {1};
	}

	@Override
	public boolean canPlaceItem(int slot, ItemStack stack) {
		if (slot == 0) {
			Feeds.Charge charge = Feeds.parse(stack);
			if (charge == null) {
				return false;
			}
			return switch (machine()) {
				case ORE_BIN -> binCount() < PlantRates.BIN_ITEMS;
				case CONDITIONER, FILTER -> false;
				default -> accepts(charge.rock(), charge.size());
			};
		}
		return slot >= 2 && machine() == Machine.CONDITIONER && reagent(stack) != null;
	}

	@Override
	public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) {
		return side == Direction.UP && canPlaceItem(slot, stack);
	}

	@Override
	public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
		return (slot == 1 || slot == 2 && machine() == Machine.FILTER) && side != Direction.UP;
	}
}
