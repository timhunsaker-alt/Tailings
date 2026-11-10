package dev.wildcatter.tailings.block;

import dev.wildcatter.energy.Inspectable;
import dev.wildcatter.multiblock.BigPartEntity;
import dev.wildcatter.multiblock.BigParts;
import dev.wildcatter.tailings.TailingsBlocks;
import dev.wildcatter.tailings.TailingsEntities;
import dev.wildcatter.tailings.entity.CableLink;
import dev.wildcatter.tailings.entity.RopeShovelEntity;
import dev.wildcatter.tailings.pit.RopeShovelLayout;
import java.util.ArrayList;
import java.util.List;
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
 * The electric rope shovel standing as its six big parts (docs/multiblocks/open-pit-p5-big-machines.md, P5d), at the crawlers' main
 * cell: the machine's cable reel and its panel. It checks every part stands where it should (NOT BUILT names the missing one) and that a
 * trailing cable reaches it (NO CABLE); RUN then lifts it into one machine, a {@link RopeShovelEntity}, which carries the cable on and
 * sets its parts back down here, or wherever it stops, when told STOP.
 */
public class RopeShovelBlockEntity extends BlockEntity implements BigPartEntity, Inspectable, PitPanelMenu.Panel {
	private boolean grown;
	/** The machine's own name on its cable: the outlet knows it by this, standing or lifted. */
	private UUID machine = UUID.randomUUID();
	private @Nullable CableLink cable;
	private String status = "standing as its parts";
	private boolean autoOnLift;

	public RopeShovelBlockEntity(BlockPos pos, BlockState state) {
		super(TailingsBlocks.ROPE_SHOVEL_ENTITY, pos, state);
	}

	public UUID machine() {
		return machine;
	}

	public @Nullable CableLink cable() {
		return cable;
	}

	/** What it last did (lifted, set down, coupled), for the readout. */
	public String note() {
		return status;
	}

	/** A trailing cable from a switch house's outlet reaches its reel; why not, or null. */
	public @Nullable String couple(ServerLevel level, BlockPos house, int outlet) {
		if (cable != null) {
			cable.addMetres(dev.wildcatter.tailings.pit.PitPower.CABLE_M);
			setChanged();
			return null;
		}
		if (!(level.getBlockEntity(house) instanceof PitSwitchHouseBlockEntity h)) {
			return "the switch house it was taken from is gone";
		}
		CableLink link = new CableLink(house, outlet, dev.wildcatter.tailings.pit.PitPower.CABLE_M, 0);
		Vec3 at = link.coupler(level), centre = centre();
		double straight = Math.hypot(centre.x - at.x, centre.z - at.z);
		if (straight > dev.wildcatter.tailings.pit.PitPower.CABLE_M) {
			return String.format(java.util.Locale.ROOT, "the shovel is %.0f m from the switch house: a reel is 400 m", straight);
		}
		String why = h.couple(outlet, machine, "Rope Shovel", dev.wildcatter.tailings.pit.PitPower.CABLE_M);
		if (why != null) {
			return why;
		}
		link.setPaid(straight);
		cable = link;
		status = "cable coupled: " + link.describe();
		setChanged();
		return null;
	}

	/** Its centre on the ground, where it turns and swings. */
	public Vec3 centre() {
		return PitSwitchHouseBlockEntity.world(worldPosition, getBlockState(), RopeShovelLayout.CENTRE_X, 0, RopeShovelLayout.CENTRE_Z);
	}

	/** Where a part's main cell should be, turned with the crawlers. */
	public BlockPos expected(RopeShovelLayout.Part part) {
		int[] at = RopeShovelLayout.turned(part.offset(), BigParts.turns(getBlockState()));
		return worldPosition.offset(at[0], at[1], at[2]);
	}

	/** What is missing, or null when every part stands where it should, facing the crawlers' way. */
	public @Nullable String check() {
		if (level == null) {
			return "NOT BUILT";
		}
		var facing = getBlockState().getValue(PlantMachineBlock.FACING);
		for (RopeShovelLayout.Part part : RopeShovelLayout.Part.values()) {
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

	/** RUN: checks it, then lifts it into one machine; what happened. Public for tests. */
	public String run(ServerLevel level) {
		String missing = check();
		if (missing != null) {
			return status = missing;
		}
		if (cable == null) {
			return status = "NO CABLE: couple a trailing cable from a pit switch house to the crawlers";
		}
		RopeShovelEntity shovel = lift(level);
		return "lifted: " + shovel.status();
	}

	/** The parts' cells become air, quietly, and one machine stands in their place, carrying the cable on. */
	private RopeShovelEntity lift(ServerLevel level) {
		int turns = BigParts.turns(getBlockState());
		Vec3 centre = centre();
		List<BlockPos> cells = new ArrayList<>();
		for (RopeShovelLayout.Part part : RopeShovelLayout.Part.values()) {
			BlockPos main = expected(part);
			cells.addAll(BigParts.cells(main, level.getBlockState(main)));
		}
		RopeShovelEntity shovel = TailingsEntities.ROPE_SHOVEL.create(level, net.minecraft.world.entity.EntitySpawnReason.TRIGGERED);
		shovel.setUUID(machine);
		shovel.setPos(centre.x, worldPosition.getY(), centre.z);
		shovel.setYRot(RopeShovelLayout.yaw(turns));
		CableLink link = cable;
		cable = null;
		int quiet = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE | Block.UPDATE_SUPPRESS_DROPS;
		for (BlockPos cell : cells) {
			level.setBlock(cell, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), quiet);
		}
		shovel.setCable(link);
		shovel.lifted();
		if (autoOnLift) {
			shovel.setAutoHere();
		}
		level.addFreshEntity(shovel);
		level.playSound(null, worldPosition, dev.wildcatter.WildcatterSounds.STRUCTURE_FORMED, net.minecraft.sounds.SoundSource.BLOCKS, 2F, 0.8F);
		return shovel;
	}

	/**
	 * Sets a lifted shovel's parts down with their crawlers' main cell at {@code main}, facing {@code turns}; why not, or null. The cells
	 * must be free. The new crawlers take its name and cable.
	 */
	public static @Nullable String setDown(ServerLevel level, BlockPos main, int turns, UUID machine, @Nullable CableLink cable) {
		net.minecraft.core.Direction facing = net.minecraft.core.Direction.from2DDataValue(Math.floorMod(2 + turns, 4));   // 0 north, 1 east, 2 south, 3 west
		List<BlockPos> mains = new ArrayList<>();
		for (RopeShovelLayout.Part part : RopeShovelLayout.Part.values()) {
			int[] at = RopeShovelLayout.turned(part.offset(), turns);
			BlockPos pm = main.offset(at[0], at[1], at[2]);
			mains.add(pm);
			Block block = TailingsBlocks.plantBlock(part.machine());
			BlockState state = block.defaultBlockState().setValue(PlantMachineBlock.FACING, facing);
			for (BlockPos cell : BigParts.cells(pm, state)) {
				BlockState there = level.getBlockState(cell);
				if (!there.isAir() && !there.canBeReplaced()) {
					return "can't set down: blocked at " + cell.toShortString() + " by " + net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(there.getBlock())
							+ " (the " + part.machine().displayName() + ", " + (cell.getX() - main.getX()) + ", " + (cell.getY() - main.getY()) + ", " + (cell.getZ() - main.getZ())
							+ " from the crawlers)";
				}
			}
		}
		int i = 0;
		for (RopeShovelLayout.Part part : RopeShovelLayout.Part.values()) {
			BlockPos pm = mains.get(i++);
			BlockState state = TailingsBlocks.plantBlock(part.machine()).defaultBlockState().setValue(PlantMachineBlock.FACING, facing);
			level.setBlock(pm, state, Block.UPDATE_ALL);
			BigParts.grow(level, pm, level.getBlockState(pm));
		}
		if (level.getBlockEntity(main) instanceof RopeShovelBlockEntity crawlers) {
			crawlers.machine = machine;
			crawlers.cable = cable;
			crawlers.status = "set down: standing as its parts";
			crawlers.setChanged();
		}
		return null;
	}

	public static void tick(Level level, BlockPos pos, BlockState state, RopeShovelBlockEntity crawlers) {
		if (level instanceof ServerLevel server && crawlers.cable != null) {
			crawlers.cable.answer(server, crawlers.machine, 0, dev.wildcatter.tailings.pit.PitPower.ROPE_HOIST_WATTS);   // standing, it draws nothing
		}
	}

	// ---- the panel ---------------------------------------------------------------------------------------------------------------

	@Override
	public PitPanelMenu.Kind kind() {
		return PitPanelMenu.Kind.ROPE_SHOVEL;
	}

	@Override
	public int setting(int i) {
		return 0;
	}

	@Override
	public void set(int i, int value, Player player) {
	}

	@Override
	public void action(int a, Player player) {
		if (!(level instanceof ServerLevel server)) {
			return;
		}
		switch (a) {
			case 0 -> player.sendOverlayMessage(Component.literal("Rope Shovel: " + run(server)));
			case 1 -> player.sendOverlayMessage(Component.literal("Rope Shovel: standing as its parts"));
			case 2 -> {
				autoOnLift = !autoOnLift;
				status = autoOnLift ? "AUTO when it runs" : "by hand when it runs";
				player.sendOverlayMessage(Component.literal("Rope Shovel: " + status));
			}
			default -> {
			}
		}
	}

	@Override
	public String status() {
		String missing = check();
		return (missing != null ? missing : cable == null ? "NO CABLE" : "built; " + cable.describe()) + (autoOnLift ? "; AUTO when it runs" : "");
	}

	@Override
	public boolean stillValid(Player player) {
		return !isRemoved() && player.distanceToSqr(Vec3.atCenterOf(worldPosition)) < 40 * 40;
	}

	@Override
	public Component inspect() {
		return Component.literal("Rope Shovel (standing as its parts): " + status() + "; " + note());
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
		output.putBoolean("auto", autoOnLift);
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
		autoOnLift = input.getBooleanOr("auto", false);
		cable = CableLink.load(input);
	}
}
