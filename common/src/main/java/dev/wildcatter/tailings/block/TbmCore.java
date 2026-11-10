package dev.wildcatter.tailings.block;

import dev.wildcatter.tailings.TailingsBlocks;
import dev.wildcatter.tailings.entity.MineVehicleEntity;
import dev.wildcatter.tailings.haul.CarLoad;
import dev.wildcatter.tailings.haul.Haul;
import dev.wildcatter.tailings.mine.Advance;
import dev.wildcatter.tailings.mine.DrillCodes;
import dev.wildcatter.tailings.mine.DrillProgram;
import dev.wildcatter.tailings.mine.DrillStock;
import dev.wildcatter.tailings.mine.DriveCourse;
import dev.wildcatter.tailings.mine.Grade;
import dev.wildcatter.tailings.mine.Ground;
import dev.wildcatter.tailings.mine.Heading;
import dev.wildcatter.tailings.mine.TbmGantry;
import dev.wildcatter.tailings.mine.TunnelDrill;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import org.jspecify.annotations.Nullable;

/**
 * The tunnel boring machine's cab (docs/multiblocks/tunnel-boring-machine.md): its slots, program, stock, muck buffer and
 * progress, and the advance cycle, apart from where the machine stands. The cab's block entity carries it while the machine is
 * blocks; the contraption carries it while it runs and on the decline.
 */
public final class TbmCore {
	/** 1 MW at 4.16 kV through the trailing cable (unverified, small TBM class). */
	public static final int BEAM_WATTS = 1_000_000;
	public static final int TANK_MAX = 256;
	/** The trailing cable on the reel, 300 m (proposed); each cable item in the cable slot adds a metre. */
	public static final int REEL_METRES = 300;
	public static final int SLOT_RAIL = 0;
	public static final int SLOT_GRAVEL = 1;
	public static final int SLOT_COBBLE = 2;
	public static final int SLOT_SEGMENTS = 3;
	public static final int SLOT_WATER = 4;
	public static final int SLOT_CABLE = 5;
	public static final int INPUTS = 6;
	public static final int MUCK_SLOTS = 18;
	public static final int SLOTS = INPUTS + MUCK_SLOTS;
	/** The Wildcatter 4.16 kV cable, which the reel pays out. */
	public static final String CABLE_ID = "wildcatter:cable_mv";

	/** The pump on its gantry, while it pumps or quenches, and the conveyor, while it moves muck: through the gantry transformer. */
	public static final int PUMP_WATTS = 75_000;
	public static final int CONVEYOR_WATTS = 7_500;
	/** A button on the cab's screen: seats the player, on the running machine. */
	public static final int BUTTON_RIDE = 13;
	/** The rails each machine is loading cars at, per level, with the tick it last said so: the trains wait there. */
	private static final Map<Level, Map<Long, Loading>> LOADING = new WeakHashMap<>();

	private record Loading(TbmCore core, long tick) {
	}

	/** What the advance cycle needs from the machine. */
	public interface Body {
		/** True while its power comes in. */
		boolean powered();

		/** True if nothing solid stands in the bore it moves through next. */
		boolean clearAhead();

		/** What is wrong with the gantries behind it, or null. */
		@Nullable String backupFault();
	}

	private final NonNullList<ItemStack> slots = NonNullList.withSize(SLOTS, ItemStack.EMPTY);
	private final DrillProgram program = new DrillProgram(0, Heading.NORTH, 64);
	private final DrillStock stock = new DrillStock();
	private final TunnelDrill drill = new TunnelDrill(program, stock, 0, 0, 0);
	private final List<String> pending = new ArrayList<>();
	private boolean running;
	private int wait;
	/** The quench tank's water: the pump gantry's, which the machine takes each tick (TbmBackup#loadTank) and hands back. */
	private int tank;
	private boolean tankAboard = true;
	/** A world saved with the tank in the cab: this goes to the pump gantry the first time it is seen. */
	private int legacyTank;
	private int status = DrillCodes.IDLE;
	private int lack = DrillCodes.LACK_NONE;
	private String note = "";
	/** Muck moves in bursts of a few ticks: its pour sounds once in this many at most. */
	private static final int POUR_TICKS = 30;
	private long lastPour;
	private @Nullable BlockPos junction;
	private boolean forced;
	/** For the tests and films: this many ticks a block, not the real 20 s; 0 is real. */
	private int testTicks;
	/** For the tests: this many metres of cable, not the reel's; 0 is real. */
	private int testCable;
	private final UUID[] gantries = new UUID[TbmGantry.values().length];
	private boolean pumping;
	private boolean conveying;
	private boolean auxPower = true;

	public boolean forced() {
		return forced;
	}

	/** The game test runs it without building a grid. */
	public void setForced(boolean forced) {
		this.forced = forced;
	}

	public void setTestTicks(int ticks) {
		testTicks = ticks;
	}

	public void setTestCable(int metres) {
		testCable = metres;
	}

	public @Nullable UUID gantry(TbmGantry kind) {
		return gantries[kind.ordinal()];
	}

	public void couple(TbmGantry kind, UUID id) {
		gantries[kind.ordinal()] = id;
	}

	public void uncouple(TbmGantry kind) {
		gantries[kind.ordinal()] = null;
	}

	/** What it draws through the reel's cable now: the beam at 4.16 kV, and the pump and conveyor through the reel gantry's transformer. */
	public int drawWatts() {
		return BEAM_WATTS + auxWatts();
	}

	/** The pump's and conveyor's draw on the reel gantry's transformer now. */
	public int auxWatts() {
		return (pumping ? PUMP_WATTS : 0) + (conveying ? CONVEYOR_WATTS : 0);
	}

	/** Whether the transformer's 480 V side is up: without it the conveyor does not move muck. */
	public void setAuxPower(boolean on) {
		auxPower = on;
	}

	/** Says why it stands as it does. */
	public void report(int code, String why) {
		status = code;
		note = why;
	}

	public NonNullList<ItemStack> slots() {
		return slots;
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

	public String note() {
		return note;
	}

	public boolean running() {
		return running;
	}

	public void setRunning(boolean running) {
		this.running = running;
	}

	public int waitTicks() {
		return wait;
	}

	public void countDown() {
		if (wait > 0) {
			wait--;
		}
	}

	public @Nullable BlockPos junction() {
		return junction;
	}

	public void setJunction(@Nullable BlockPos junction) {
		this.junction = junction;
	}

	/** Whether a pump gantry is here with the tank now, and the water in it (no pump gantry: none; one not loaded: what was last seen). */
	public void setTankAboard(boolean aboard, int water) {
		tankAboard = aboard;
		tank = Math.max(0, water);
	}

	public boolean tankAboard() {
		return tankAboard;
	}

	public int legacyTank() {
		return legacyTank;
	}

	public void clearLegacyTank() {
		legacyTank = 0;
	}

	public void addQuench(int buckets) {
		tank = Math.min(TANK_MAX, tank + Math.max(0, buckets));
	}

	/** The cable it has: the reel's 300 m and a metre for each cable item. */
	public int cableMetres() {
		if (testCable > 0) {
			return testCable;
		}
		ItemStack cable = slots.get(SLOT_CABLE);
		return REEL_METRES + (cable.isEmpty() ? 0 : cable.getCount());
	}

	public int cableLeft() {
		return Math.max(0, cableMetres() - drill.advanceCount());
	}

	/** Screen buttons; the same ids are the menu's click buttons. */
	public void press(int button, Level level) {
		switch (button) {
			case 0 -> program.setTargetY(program.targetY() - 1);
			case 1 -> program.setTargetY(program.targetY() + 1);
			case 3 -> program.setFlatLength(program.flatLength() - 8);
			case 4 -> program.setFlatLength(program.flatLength() + 8);
			case 6 -> program.setStopOnOre(!program.stopOnOre());
			case 7 -> program.setStopOnCable(!program.stopOnCable());
			case 8 -> program.setStopOnLiquid(!program.stopOnLiquid());
			case 9 -> program.setStopOnEmpty(!program.stopOnEmpty());
			case 10 -> toggleRun(level);
			default -> {
			}
		}
	}

	private void toggleRun(Level level) {
		if (running) {
			running = false;
			status = DrillCodes.IDLE;
			return;
		}
		drill.resetStop();
		running = true;
		status = DrillCodes.DRIVING;
		note = "";
	}

	/**
	 * One advance, when its wait is over: cuts the face. The machine then glides into the cut for the time that sets ({@link
	 * #waitTicks}), and lines and floors behind itself when it arrives ({@link TunnelDrill#line}).
	 */
	public Advance advance(Level level, Body body) {
		if (!body.powered()) {
			report(DrillCodes.NEED_POWER, "waiting for power at the cable junction");
			return Advance.stopped(Advance.Stop.NONE, "power");
		}
		String fault = body.backupFault();
		if (fault != null) {
			report(DrillCodes.NOT_BUILT, "NOT BUILT: needs the " + fault);
			return Advance.stopped(Advance.Stop.NONE, "gantry");
		}
		pumping = false;
		WorldGround ground = new WorldGround(level, program.heading());
		pullStock();
		flushPending();
		if (!pending.isEmpty() || !roomForFace(ground)) {
			status = DrillCodes.STOP_BUFFER;
			lack = DrillCodes.LACK_NONE;
			return Advance.stopped(Advance.Stop.BUFFER_FULL, "muck buffer full");
		}
		if (!body.clearAhead()) {
			running = false;
			lack = DrillCodes.LACK_NONE;
			report(DrillCodes.STOP_BLOCKED, "blocked: something solid is in the machine's way");
			return Advance.stopped(Advance.Stop.BLOCKED, "blocked");
		}
		program.setCableBlocks(cableMetres());
		int ticks = testTicks > 0 ? testTicks : drill.ticksFor(ground);
		Advance drove = drill.step(ground);
		pumping = drove.pumpedWater() > 0 || drove.quenchedLava() > 0;
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
			note = drill.detail();
		} else if (drove.placed()) {
			status = DrillCodes.DRIVING;
			lack = DrillCodes.LACK_NONE;
			note = "";
			wait = ticks;
		}
		return drove;
	}

	/** The rails just behind the conveyor's tail, on columns 2 and 5: a car there takes the machine's muck, and a train waits for it. */
	public void tickLoading(Level level) {
		conveying = false;
		if (drill.advanceCount() == 0 || gantry(TbmGantry.CONVEYOR) == null || !auxPower) {
			return;
		}
		Map<Long, Loading> here = LOADING.computeIfAbsent(level, l -> new HashMap<>());
		for (int row = TbmGantry.LOADING_ROW; row < TbmGantry.LOADING_ROW + TbmGantry.LOADING_ROWS; row++) {
			int ahead = -(row + 1);
			for (int across : new int[] {TbmGantry.LEFT_RAIL + 1, TbmGantry.RIGHT_RAIL + 1}) {
				here.put(new BlockPos(drill.worldX(across, ahead), drill.worldY(1, ahead), drill.worldZ(across, ahead)).asLong(), new Loading(this, level.getGameTime()));
			}
		}
		for (int row = TbmGantry.LOADING_ROW; row < TbmGantry.LOADING_ROW + TbmGantry.LOADING_ROWS; row++) {
			int ahead = -(row + 1);
			for (int across : new int[] {TbmGantry.LEFT_RAIL + 1, TbmGantry.RIGHT_RAIL + 1}) {
				BlockPos rail = new BlockPos(drill.worldX(across, ahead), drill.worldY(1, ahead), drill.worldZ(across, ahead));
				for (MineVehicleEntity car : level.getEntitiesOfClass(MineVehicleEntity.class, new AABB(rail).inflate(0, 1, 0),
						e -> !e.locomotive() && e.load().room() > 0 && BlockPos.containing(e.getX(), e.getY() - 0.5, e.getZ()).equals(rail))) {
					if (giveMuck(car.load(), Haul.TRANSFER) > 0) {
						car.refreshLoad();
						conveying = true;
						if (level.getGameTime() - lastPour >= POUR_TICKS) {
							lastPour = level.getGameTime();
							level.playSound(null, rail, dev.wildcatter.tailings.TailingsSounds.MUCK_LOAD, net.minecraft.sounds.SoundSource.BLOCKS, 2F, 1F);
						}
					}
					return;
				}
			}
		}
	}

	/** The machine whose loading point is the rail at {@code rail}, or null. */
	public static @Nullable TbmCore loadingAt(Level level, BlockPos rail) {
		Map<Long, Loading> here = LOADING.get(level);
		Loading loading = here == null ? null : here.get(rail.asLong());
		return loading != null && level.getGameTime() - loading.tick() <= 2 ? loading.core() : null;
	}

	public void acceptBuckets() {
		ItemStack water = slots.get(SLOT_WATER);
		if (tankAboard && water.getItem() == Items.WATER_BUCKET && tank < TANK_MAX) {
			tank++;
			slots.set(SLOT_WATER, new ItemStack(Items.BUCKET));
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
		int space = 0;
		for (int slot = INPUTS; slot < SLOTS; slot++) {
			ItemStack stack = slots.get(slot);
			space += stack.isEmpty() ? 64 : stack.getMaxStackSize() - stack.getCount();
		}
		return space >= solid;
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

	/** True while it has muck waiting for a car. */
	public boolean hasMuck() {
		for (int slot = INPUTS; slot < SLOTS; slot++) {
			if (!slots.get(slot).isEmpty()) {
				return true;
			}
		}
		return false;
	}

	/** Hands up to {@code max} items of muck into a car's load; returns how many. */
	public int giveMuck(CarLoad load, int max) {
		int moved = 0;
		for (int slot = INPUTS; slot < SLOTS && moved < max; slot++) {
			ItemStack stack = slots.get(slot);
			while (!stack.isEmpty() && load.room() > 0 && moved < max) {
				if (load.add(BuiltInRegistries.ITEM.getKey(stack.getItem()).toString(), 1) != 0) {
					return moved;
				}
				stack.shrink(1);
				moved++;
			}
		}
		return moved;
	}

	public static boolean canPlace(int slot, ItemStack stack) {
		return switch (slot) {
			case SLOT_RAIL -> stack.getItem() == TailingsBlocks.MINE_RAIL.asItem();
			case SLOT_GRAVEL -> stack.getItem() == Items.GRAVEL;
			case SLOT_COBBLE -> stack.getItem() == Items.COBBLESTONE;
			case SLOT_SEGMENTS -> stack.getItem() == TailingsBlocks.LINING.asItem();
			case SLOT_WATER -> stack.getItem() == Items.WATER_BUCKET;
			case SLOT_CABLE -> CABLE_ID.equals(BuiltInRegistries.ITEM.getKey(stack.getItem()).toString());
			default -> false;
		};
	}

	/** The status in words: the fuller note where it has one, as "NOT BUILT: needs the Pump Gantry". */
	public String statusLine() {
		String phrase = DrillCodes.phrase(status, lack);
		return note.isEmpty() ? phrase : note.startsWith(DrillCodes.phrase(status, DrillCodes.LACK_NONE)) ? note : phrase + " (" + note + ")";
	}

	public String readout() {
		return statusLine()
				+ "; floor Y " + drill.floorY() + "; " + drill.advanceCount() + " blocks driven; cable left " + cableLeft() + " m";
	}

	public void save(ValueOutput output) {
		ContainerHelper.saveAllItems(output, slots);
		output.putInt("advance", drill.advanceCount());
		output.putInt("flatDriven", drill.flatDriven());
		output.putInt("pumped", drill.pumped());
		output.putInt("originX", drill.originX());
		output.putInt("originY", drill.startFloorY());
		output.putInt("originZ", drill.originZ());
		output.putString("stop", drill.stopped().name());
		output.putString("detail", drill.detail());
		output.putString("line", drill.savePending());
		output.putString("rocks", stock.floorRockSave());
		output.putInt("target", program.targetY());
		output.putString("heading", program.heading().name());
		output.putInt("flatLength", program.flatLength());
		output.putBoolean("stopOre", program.stopOnOre());
		output.putBoolean("stopCable", program.stopOnCable());
		output.putBoolean("stopLiquid", program.stopOnLiquid());
		output.putBoolean("stopEmpty", program.stopOnEmpty());
		output.putBoolean("running", running);
		output.putInt("wait", wait);
		output.putInt("tank", tank);
		output.putBoolean("tankOnGantry", true);
		output.putInt("legacyTank", legacyTank);
		output.putInt("status", status);
		output.putInt("lack", lack);
		output.putString("note", note);
		output.putString("pending", String.join("|", pending));
		output.putBoolean("forced", forced);
		output.putInt("testTicks", testTicks);
		output.putInt("testCable", testCable);
		for (TbmGantry kind : TbmGantry.values()) {
			output.putString("gantry_" + kind.id(), gantries[kind.ordinal()] == null ? "" : gantries[kind.ordinal()].toString());
		}
		output.putLong("junction", junction == null ? Long.MIN_VALUE : junction.asLong());
	}

	public void load(ValueInput input) {
		slots.clear();
		ContainerHelper.loadAllItems(input, slots);
		program.setTargetY(input.getIntOr("target", program.targetY()));
		program.setHeading(enumOr(Heading.class, input.getStringOr("heading", program.heading().name()), program.heading()));
		program.setFlatLength(input.getIntOr("flatLength", program.flatLength()));
		// decided with Tim (R6): 1 in 2 only; an old 1-in-1 program reads as 1 in 2
		program.setGrade(Grade.ONE_IN_TWO);
		program.setStopOnOre(input.getBooleanOr("stopOre", true));
		program.setStopOnCable(input.getBooleanOr("stopCable", true));
		program.setStopOnLiquid(input.getBooleanOr("stopLiquid", true));
		program.setStopOnEmpty(input.getBooleanOr("stopEmpty", true));
		stock.loadFloorRock(input.getStringOr("rocks", ""));
		drill.placeOrigin(input.getIntOr("originX", 0), input.getIntOr("originY", 0), input.getIntOr("originZ", 0));
		drill.restore(input.getIntOr("advance", 0), input.getIntOr("flatDriven", 0), input.getIntOr("pumped", 0),
				enumOr(Advance.Stop.class, input.getStringOr("stop", "NONE"), Advance.Stop.NONE), input.getStringOr("detail", ""));
		drill.loadPending(input.getStringOr("line", ""));
		running = input.getBooleanOr("running", false);
		wait = input.getIntOr("wait", 0);
		tank = input.getIntOr("tank", 0);
		legacyTank = input.getBooleanOr("tankOnGantry", false) ? input.getIntOr("legacyTank", 0) : tank;
		status = input.getIntOr("status", DrillCodes.IDLE);
		lack = input.getIntOr("lack", DrillCodes.LACK_NONE);
		note = input.getStringOr("note", "");
		pending.clear();
		String saved = input.getStringOr("pending", "");
		if (!saved.isEmpty()) {
			for (String line : saved.split("\\|")) {
				if (!line.isEmpty()) {
					pending.add(line);
				}
			}
		}
		forced = input.getBooleanOr("forced", false);
		testTicks = input.getIntOr("testTicks", 0);
		testCable = input.getIntOr("testCable", 0);
		for (TbmGantry kind : TbmGantry.values()) {
			String id = input.getStringOr("gantry_" + kind.id(), "");
			gantries[kind.ordinal()] = id.isEmpty() ? null : UUID.fromString(id);
		}
		long at = input.getLongOr("junction", Long.MIN_VALUE);
		junction = at == Long.MIN_VALUE ? null : BlockPos.of(at);
	}

	private static <T extends Enum<T>> T enumOr(Class<T> type, String name, T fallback) {
		try {
			return Enum.valueOf(type, name);
		} catch (IllegalArgumentException ex) {
			return fallback;
		}
	}
}
