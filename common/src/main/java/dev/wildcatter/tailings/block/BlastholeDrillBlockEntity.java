package dev.wildcatter.tailings.block;

import dev.wildcatter.energy.Inspectable;
import dev.wildcatter.multiblock.BigPartEntity;
import dev.wildcatter.multiblock.BigParts;
import dev.wildcatter.tailings.TailingsBlocks;
import dev.wildcatter.tailings.TailingsEntities;
import dev.wildcatter.tailings.entity.BlastholeDrillEntity;
import dev.wildcatter.tailings.entity.CableLink;
import dev.wildcatter.tailings.pit.BlastholeDrillLayout;
import dev.wildcatter.tailings.pit.PitPower;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * The large electric rotary blasthole drill standing as its four big parts (docs/multiblocks/open-pit-p3-blasting.md, As built: four
 * parts), at the crawlers' main cell: the machine's cable reel, its pattern panel and its PLC answers. It checks every part stands where
 * it should (NOT BUILT names the missing one) and that a trailing cable reaches it (NO CABLE); GO then lifts it into one machine, a
 * {@link BlastholeDrillEntity}, which walks the pattern set here and sets its parts back down where it stands when told STOP.
 *
 * <p>On its switch house outlet's points (P6's MACHINE RUN, RUNNING, DONE, CYCLES, POWER): standing, it answers RUNNING off, DONE as its
 * last pattern ended and CYCLES as the holes it has drilled, ever; RUN coming on lifts it and starts its pattern.
 */
public class BlastholeDrillBlockEntity extends BlockEntity implements BigPartEntity, Inspectable, PitPanelMenu.Panel {
	private boolean grown;
	/** The machine's own name on its cable: the outlet knows it by this, standing or lifted. */
	private UUID machine = UUID.randomUUID();
	private @Nullable CableLink cable;
	private String status = "standing as its parts";
	private final int[] settings = new int[PitPanelMenu.Kind.DRILL.settings().length];
	/** The holes it has drilled, ever, and whether its last pattern was drilled to the end: carried through each lift and set-down. */
	private long holes;
	private boolean done;
	/** The PLC's RUN as last read; a lift needs it to come on (true after a set-down, so a RUN held on does not lift it again at once). */
	private boolean lastRun;
	/** A game test's faster drilling, handed to the lifted drill. */
	private int fast = 1;

	public BlastholeDrillBlockEntity(BlockPos pos, BlockState state) {
		super(TailingsBlocks.BLASTHOLE_DRILL_ENTITY, pos, state);
		for (int i = 0; i < settings.length; i++) {
			settings[i] = PitPanelMenu.Kind.DRILL.settings()[i].initial();
		}
		settings[ELECTRIC] = 1;
	}

	/** The panel's Electric setting: always on, it has no diesel engine. */
	private static final int ELECTRIC = 7;

	public UUID machine() {
		return machine;
	}

	public @Nullable CableLink cable() {
		return cable;
	}

	public long holes() {
		return holes;
	}

	public boolean done() {
		return done;
	}

	/** What it last did (lifted, set down, coupled), for the readout. */
	public String note() {
		return status;
	}

	/** Test and film hook: the lifted drill drills, trams and sets up this many times faster. */
	public void fast(int divisor) {
		fast = Math.max(1, divisor);
		setChanged();
	}

	/** Test hook: a pattern setting by its label's start, as the drill's own. */
	public void setting(String label, int value) {
		PitPanelMenu.Setting[] all = PitPanelMenu.Kind.DRILL.settings();
		for (int i = 0; i < all.length; i++) {
			if (i != ELECTRIC && all[i].label().startsWith(label)) {
				settings[i] = all[i].clamp(value);
			}
		}
		setChanged();
	}

	/** A trailing cable from a switch house's outlet reaches its reel; why not, or null. */
	public @Nullable String couple(ServerLevel level, BlockPos house, int outlet) {
		if (cable != null) {
			cable.addMetres(PitPower.CABLE_M);
			setChanged();
			return null;
		}
		if (!(level.getBlockEntity(house) instanceof PitSwitchHouseBlockEntity h)) {
			return "the switch house it was taken from is gone";
		}
		CableLink link = new CableLink(house, outlet, PitPower.CABLE_M, 0);
		Vec3 at = link.coupler(level), centre = centre();
		double straight = Math.hypot(centre.x - at.x, centre.z - at.z);
		if (straight > PitPower.CABLE_M) {
			return String.format(Locale.ROOT, "the drill is %.0f m from the switch house: a reel is 400 m", straight);
		}
		String why = h.couple(outlet, machine, "Blasthole Drill", PitPower.CABLE_M);
		if (why != null) {
			return why;
		}
		link.setPaid(straight);
		cable = link;
		status = "cable coupled: " + link.describe();
		setChanged();
		return null;
	}

	/** Its centre on the ground, where the lifted drill stands and turns. */
	public Vec3 centre() {
		return PitSwitchHouseBlockEntity.world(worldPosition, getBlockState(), BlastholeDrillLayout.CENTRE_X, 0, BlastholeDrillLayout.CENTRE_Z);
	}

	/** Where a part's main cell should be, turned with the crawlers. */
	public BlockPos expected(BlastholeDrillLayout.Part part) {
		int[] at = BlastholeDrillLayout.turned(part.offset(), BigParts.turns(getBlockState()));
		return worldPosition.offset(at[0], at[1], at[2]);
	}

	/** What is missing, or null when every part stands where it should, facing the crawlers' way. */
	public @Nullable String check() {
		if (level == null) {
			return "NOT BUILT";
		}
		var facing = getBlockState().getValue(PlantMachineBlock.FACING);
		for (BlastholeDrillLayout.Part part : BlastholeDrillLayout.Part.values()) {
			BlockPos at = expected(part);
			BlockState there = level.getBlockState(at);
			boolean ok = there.getBlock() instanceof PlantMachineBlock block && block.machine() == part.machine()
					&& PlantMachineBlock.mainOf(at, there).equals(at) && there.getValue(PlantMachineBlock.FACING) == facing;
			if (!ok) {
				return "NOT BUILT: needs the " + part.machine().displayName() + " (" + part.machine().id() + ") with its main cell at " + at.toShortString();
			}
		}
		return null;
	}

	/** What the lifted drill does first. */
	public enum Start {
		/** GO: it walks its pattern from the hole under its mast. */
		PATTERN,
		/** Drill one: one hole where it stands. */
		ONE,
		/** It stands, lifted, waiting for its panel or the PLC. */
		NOTHING
	}

	/** GO: checks it, then lifts it into one machine and starts its pattern; what happened. Public for tests. */
	public String run(ServerLevel level) {
		return run(level, Start.PATTERN);
	}

	public String run(ServerLevel level, Start start) {
		String missing = check();
		if (missing != null) {
			setChanged();
			return status = missing;
		}
		if (cable == null) {
			setChanged();
			return status = "NO CABLE: couple a trailing cable from a pit switch house to the crawlers";
		}
		BlastholeDrillEntity drill = lift(level);
		String said = switch (start) {
			case PATTERN -> drill.go();
			case ONE -> drill.drillOne();
			case NOTHING -> drill.status();
		};
		return "lifted: " + said;
	}

	/** The parts' cells become air, quietly, and one machine stands in their place, carrying the cable, the pattern and its count on. */
	private BlastholeDrillEntity lift(ServerLevel level) {
		int turns = BigParts.turns(getBlockState());
		Vec3 centre = centre();
		List<BlockPos> cells = new ArrayList<>();
		for (BlastholeDrillLayout.Part part : BlastholeDrillLayout.Part.values()) {
			BlockPos main = expected(part);
			cells.addAll(BigParts.cells(main, level.getBlockState(main)));
		}
		BlastholeDrillEntity drill = TailingsEntities.BLASTHOLE_DRILL.create(level, net.minecraft.world.entity.EntitySpawnReason.TRIGGERED);
		drill.setUUID(machine);
		drill.setPos(centre.x, worldPosition.getY(), centre.z);
		drill.setYRot(BlastholeDrillLayout.yaw(turns));
		CableLink link = cable;
		cable = null;
		int quiet = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE | Block.UPDATE_SUPPRESS_DROPS;
		for (BlockPos cell : cells) {
			level.setBlock(cell, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), quiet);
		}
		drill.fromParts(settings, holes, done);
		drill.fast(fast);
		drill.setCable(link);
		level.addFreshEntity(drill);
		level.playSound(null, worldPosition, dev.wildcatter.WildcatterSounds.STRUCTURE_FORMED, net.minecraft.sounds.SoundSource.BLOCKS, 2F, 0.8F);
		return drill;
	}

	/**
	 * Sets a lifted drill's parts down with their crawlers' main cell at {@code main}, facing {@code turns}; why not, or null. The cells
	 * must be free. The new crawlers take its name, cable, pattern, count and DONE.
	 */
	public static @Nullable String setDown(ServerLevel level, BlockPos main, int turns, UUID machine, @Nullable CableLink cable, int[] settings, long holes,
			boolean done, int fast) {
		net.minecraft.core.Direction facing = net.minecraft.core.Direction.from2DDataValue(Math.floorMod(2 + turns, 4));   // 0 north, 1 east, 2 south, 3 west
		List<BlockPos> mains = new ArrayList<>();
		for (BlastholeDrillLayout.Part part : BlastholeDrillLayout.Part.values()) {
			int[] at = BlastholeDrillLayout.turned(part.offset(), turns);
			BlockPos pm = main.offset(at[0], at[1], at[2]);
			mains.add(pm);
			BlockState state = TailingsBlocks.plantBlock(part.machine()).defaultBlockState().setValue(PlantMachineBlock.FACING, facing);
			for (BlockPos cell : BigParts.cells(pm, state)) {
				BlockState there = level.getBlockState(cell);
				if (!there.isAir() && !there.canBeReplaced()) {
					return "can't set down: blocked at " + cell.toShortString() + " by " + net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(there.getBlock())
							+ " (the " + part.machine().displayName() + ")";
				}
			}
		}
		int i = 0;
		for (BlastholeDrillLayout.Part part : BlastholeDrillLayout.Part.values()) {
			BlockPos pm = mains.get(i++);
			BlockState state = TailingsBlocks.plantBlock(part.machine()).defaultBlockState().setValue(PlantMachineBlock.FACING, facing);
			level.setBlock(pm, state, Block.UPDATE_ALL);
			BigParts.grow(level, pm, level.getBlockState(pm));
		}
		if (level.getBlockEntity(main) instanceof BlastholeDrillBlockEntity crawlers) {
			crawlers.machine = machine;
			crawlers.cable = cable;
			System.arraycopy(settings, 0, crawlers.settings, 0, Math.min(settings.length, crawlers.settings.length));
			crawlers.settings[ELECTRIC] = 1;
			crawlers.holes = holes;
			crawlers.done = done;
			crawlers.fast = fast;
			crawlers.lastRun = true;
			crawlers.status = "set down: standing as its parts";
			crawlers.setChanged();
		}
		return null;
	}

	public static void tick(Level level, BlockPos pos, BlockState state, BlastholeDrillBlockEntity crawlers) {
		if (!(level instanceof ServerLevel server) || crawlers.cable == null) {
			return;
		}
		crawlers.cable.answer(server, crawlers.machine, 0, PitPower.DRILL_WATTS);   // standing, it draws nothing
		PitSwitchHouseBlockEntity house = crawlers.cable.house(server);
		if (house == null) {
			return;
		}
		int outlet = crawlers.cable.outlet();
		house.reportPlc(outlet, crawlers.machine, false, crawlers.done, crawlers.holes);
		Boolean run = house.runCommand(outlet);
		if (run == null) {
			return;
		}
		boolean rising = run && !crawlers.lastRun;
		crawlers.lastRun = run;
		if (rising) {
			// RUN on: it lifts and starts its pattern, as GO on its panel
			String said = crawlers.run(server, Start.PATTERN);
			if (!crawlers.isRemoved() && server.getBlockState(pos).getBlock() == TailingsBlocks.DRILL_CRAWLERS) {
				crawlers.status = "RUN by the PLC: " + said;
				crawlers.setChanged();
			}
		}
	}

	// ---- the panel ---------------------------------------------------------------------------------------------------------------

	@Override
	public PitPanelMenu.Kind kind() {
		return PitPanelMenu.Kind.DRILL;
	}

	@Override
	public int setting(int i) {
		return i < settings.length ? settings[i] : 0;
	}

	@Override
	public void set(int i, int value, Player player) {
		if (i == ELECTRIC) {
			player.sendOverlayMessage(Component.literal("Blasthole Drill: electric only, on its trailing cable"));
			return;
		}
		if (i < settings.length) {
			settings[i] = PitPanelMenu.Kind.DRILL.settings()[i].clamp(value);
			setChanged();
		}
	}

	@Override
	public void action(int a, Player player) {
		if (!(level instanceof ServerLevel server)) {
			return;
		}
		switch (a) {
			case 0 -> player.sendOverlayMessage(Component.literal("Blasthole Drill: " + run(server, Start.PATTERN)));
			case 1 -> player.sendOverlayMessage(Component.literal("Blasthole Drill: standing as its parts"));
			case 2 -> player.sendOverlayMessage(Component.literal("Blasthole Drill: " + run(server, Start.ONE)));
			default -> {
			}
		}
	}

	@Override
	public String status() {
		String missing = check();
		return (missing != null ? missing : cable == null ? "NO CABLE" : "built; " + cable.describe()) + String.format(Locale.ROOT, "; holes drilled %d%s", holes,
				done ? "; DONE" : "");
	}

	@Override
	public boolean stillValid(Player player) {
		return !isRemoved() && player.distanceToSqr(Vec3.atCenterOf(worldPosition)) < 32 * 32;
	}

	@Override
	public Component inspect() {
		return Component.literal("Blasthole Drill (standing as its parts): " + status() + "; " + note());
	}

	// ---- saving ------------------------------------------------------------------------------------------------------------------

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
		output.putString("machine", machine.toString());
		for (int i = 0; i < settings.length; i++) {
			output.putInt("setting" + i, settings[i]);
		}
		output.putLong("holes", holes);
		output.putBoolean("done", done);
		output.putBoolean("lastRun", lastRun);
		output.putInt("fast", fast);
		if (cable != null) {
			cable.save(output);
		}
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		grown = input.getBooleanOr("grown", false);
		String id = input.getStringOr("machine", "");
		if (!id.isEmpty()) {
			machine = UUID.fromString(id);
		}
		for (int i = 0; i < settings.length; i++) {
			settings[i] = PitPanelMenu.Kind.DRILL.settings()[i].clamp(input.getIntOr("setting" + i, settings[i]));
		}
		settings[ELECTRIC] = 1;
		holes = input.getLongOr("holes", 0);
		done = input.getBooleanOr("done", false);
		lastRun = input.getBooleanOr("lastRun", false);
		fast = Math.max(1, input.getIntOr("fast", 1));
		cable = CableLink.load(input);
	}
}
