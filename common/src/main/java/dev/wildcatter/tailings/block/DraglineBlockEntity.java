package dev.wildcatter.tailings.block;

import dev.wildcatter.energy.Inspectable;
import dev.wildcatter.multiblock.BigPartEntity;
import dev.wildcatter.multiblock.BigParts;
import dev.wildcatter.tailings.TailingsBlocks;
import dev.wildcatter.tailings.TailingsEntities;
import dev.wildcatter.tailings.entity.CableLink;
import dev.wildcatter.tailings.entity.DraglineEntity;
import dev.wildcatter.tailings.pit.Dragline;
import dev.wildcatter.tailings.pit.DraglineLayout;
import dev.wildcatter.tailings.pit.PitPower;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
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
 * The walking dragline standing as its parts (docs/multiblocks/dragline.md, P6), at the tub's main cell: the machine's cable reel, its
 * panel and its job. It checks every part stands where it should (NOT BUILT names the missing one) and that a trailing cable reaches it
 * (NO CABLE); RUN then lifts it into one machine, a {@link DraglineEntity}, which carries the cable and the job on and sets its parts
 * back down wherever it stops, if their cells are free. As the rope shovel's crawlers ({@link RopeShovelBlockEntity}).
 */
public class DraglineBlockEntity extends BlockEntity implements BigPartEntity, Inspectable, PitPanelMenu.Panel {
	private boolean grown;
	/** The machine's own name on its cable: the outlet knows it by this, standing or lifted. */
	private UUID machine = UUID.randomUUID();
	private @Nullable CableLink cable;
	private String note = "standing as its parts";
	private final Dragline.Job job = new Dragline.Job();

	public DraglineBlockEntity(BlockPos pos, BlockState state) {
		super(TailingsBlocks.DRAGLINE_ENTITY, pos, state);
	}

	public UUID machine() {
		return machine;
	}

	public @Nullable CableLink cable() {
		return cable;
	}

	public Dragline.Job job() {
		return job;
	}

	/** A trailing cable from a switch house's outlet reaches its reel; another reel adds 400 m. Why not, or null. */
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
			return String.format(Locale.ROOT, "the dragline is %.0f m from the switch house: a reel is 400 m", straight);
		}
		String why = h.couple(outlet, machine, "Dragline", PitPower.CABLE_M);
		if (why != null) {
			return why;
		}
		link.setPaid(straight);
		cable = link;
		note = "cable coupled: " + link.describe();
		setChanged();
		return null;
	}

	/** Its centre on the ground, where it turns and swings. */
	public Vec3 centre() {
		return PitSwitchHouseBlockEntity.world(worldPosition, getBlockState(), DraglineLayout.CENTRE_X, 0, DraglineLayout.CENTRE_Z);
	}

	/** Where a part's main cell should be, turned with the tub. */
	public BlockPos expected(DraglineLayout.Part part) {
		int[] at = DraglineLayout.turned(part.offset(), BigParts.turns(getBlockState()));
		return worldPosition.offset(at[0], at[1], at[2]);
	}

	/** What is missing, or null when every part stands where it should, facing the tub's way. */
	public @Nullable String check() {
		if (level == null) {
			return "NOT BUILT";
		}
		Direction facing = getBlockState().getValue(PlantMachineBlock.FACING);
		for (DraglineLayout.Part part : DraglineLayout.Part.values()) {
			BlockPos at = expected(part);
			BlockState there = level.getBlockState(at);
			boolean ok = there.getBlock() instanceof PlantMachineBlock block && block.machine() == part.machine()
					&& PlantMachineBlock.mainOf(at, there).equals(at) && there.getValue(PlantMachineBlock.FACING) == facing;
			if (!ok) {
				return "NOT BUILT: needs the " + part.describe() + " (" + part.machine().id() + ") with its main cell at " + at.toShortString();
			}
		}
		return null;
	}

	/** RUN: checks it, then lifts it into one machine and starts its job; what happened. Public for tests. */
	public String run(ServerLevel level) {
		String missing = check();
		if (missing != null) {
			return note = missing;
		}
		if (cable == null) {
			return note = "NO CABLE: couple a trailing cable from a pit switch house to the tub";
		}
		if (!Dragline.fits(job.width())) {
			return note = "a strip " + job.width() + " m wide leaves no room to cast within its 65 m reach";
		}
		DraglineEntity dragline = lift(level);
		return "lifted: " + dragline.status();
	}

	/** The parts' cells become air, quietly, and one machine stands in their place, carrying the cable and the job on. */
	private DraglineEntity lift(ServerLevel level) {
		int turns = BigParts.turns(getBlockState());
		Vec3 centre = centre();
		List<BlockPos> cells = new ArrayList<>();
		for (DraglineLayout.Part part : DraglineLayout.Part.values()) {
			BlockPos main = expected(part);
			cells.addAll(BigParts.cells(main, level.getBlockState(main)));
		}
		DraglineEntity dragline = TailingsEntities.DRAGLINE.create(level, net.minecraft.world.entity.EntitySpawnReason.TRIGGERED);
		dragline.setUUID(machine);
		dragline.setPos(centre.x, worldPosition.getY(), centre.z);
		dragline.setYRot(DraglineLayout.yaw(turns));
		CableLink link = cable;
		cable = null;
		int quiet = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE | Block.UPDATE_SUPPRESS_DROPS;
		for (BlockPos cell : cells) {
			level.setBlock(cell, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), quiet);
		}
		dragline.setCable(link);
		dragline.job().copy(job);
		dragline.start();
		level.addFreshEntity(dragline);
		level.playSound(null, worldPosition, dev.wildcatter.WildcatterSounds.STRUCTURE_FORMED, net.minecraft.sounds.SoundSource.BLOCKS, 2F, 0.7F);
		return dragline;
	}

	/**
	 * Sets a lifted dragline's parts down with the tub's main cell at {@code main}, facing {@code turns}; why not, or null. Every cell
	 * must be free. The new tub takes its name, its cable and its job.
	 */
	public static @Nullable String setDown(ServerLevel level, BlockPos main, int turns, UUID machine, @Nullable CableLink cable, Dragline.Job job) {
		Direction facing = Direction.from2DDataValue(Math.floorMod(2 + turns, 4));   // 0 north, 1 east, 2 south, 3 west
		List<BlockPos> mains = new ArrayList<>();
		for (DraglineLayout.Part part : DraglineLayout.Part.values()) {
			int[] at = DraglineLayout.turned(part.offset(), turns);
			BlockPos pm = main.offset(at[0], at[1], at[2]);
			mains.add(pm);
			BlockState state = TailingsBlocks.plantBlock(part.machine()).defaultBlockState().setValue(PlantMachineBlock.FACING, facing);
			for (BlockPos cell : BigParts.cells(pm, state)) {
				BlockState there = level.getBlockState(cell);
				if (!there.isAir() && !there.canBeReplaced()) {
					return "can't set down: blocked at " + cell.toShortString() + " by " + net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(there.getBlock())
							+ " (the " + part.describe() + ", " + (cell.getX() - main.getX()) + ", " + (cell.getY() - main.getY()) + ", " + (cell.getZ() - main.getZ())
							+ " from the tub)";
				}
			}
		}
		int i = 0;
		for (DraglineLayout.Part part : DraglineLayout.Part.values()) {
			BlockPos pm = mains.get(i++);
			BlockState state = TailingsBlocks.plantBlock(part.machine()).defaultBlockState().setValue(PlantMachineBlock.FACING, facing);
			level.setBlock(pm, state, Block.UPDATE_ALL);
			BigParts.grow(level, pm, level.getBlockState(pm));
		}
		if (level.getBlockEntity(main) instanceof DraglineBlockEntity tub) {
			tub.machine = machine;
			tub.cable = cable;
			tub.job.copy(job);
			tub.note = "set down: standing as its parts";
			tub.setChanged();
		}
		return null;
	}

	public static void tick(Level level, BlockPos pos, BlockState state, DraglineBlockEntity tub) {
		if (level instanceof ServerLevel server && tub.cable != null) {
			tub.cable.answer(server, tub.machine, 0, Dragline.PEAK_WATTS);   // standing as its parts, it draws nothing
		}
	}

	// ---- the panel ---------------------------------------------------------------------------------------------------------------

	@Override
	public PitPanelMenu.Kind kind() {
		return PitPanelMenu.Kind.DRAGLINE;
	}

	@Override
	public int setting(int i) {
		return job.get(i);
	}

	@Override
	public void set(int i, int value, Player player) {
		job.set(i, value);
		setChanged();
		player.sendOverlayMessage(Component.literal("Dragline: " + job.describe()));
	}

	@Override
	public void action(int a, Player player) {
		if (!(level instanceof ServerLevel server)) {
			return;
		}
		switch (a) {
			case 0 -> player.sendOverlayMessage(Component.literal("Dragline: " + run(server)));
			case 1 -> player.sendOverlayMessage(Component.literal("Dragline: STOPPED, standing as its parts"));
			default -> {
			}
		}
	}

	@Override
	public String status() {
		String missing = check();
		return missing != null ? missing : cable == null ? "NO CABLE" : "built; " + job.describe() + "; " + cable.describe();
	}

	@Override
	public boolean stillValid(Player player) {
		return !isRemoved() && player.distanceToSqr(Vec3.atCenterOf(worldPosition)) < 40 * 40;
	}

	@Override
	public Component inspect() {
		return Component.literal("Dragline (standing as its parts): " + status() + "; " + note);
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

	/** Saves a job under its own keys (the tub and the lifted machine both carry one). */
	public static void saveJob(ValueOutput output, Dragline.Job job) {
		output.putInt("job_width", job.width());
		output.putInt("job_length", job.length());
		output.putInt("job_seam", job.seam());
		output.putBoolean("job_cast_right", job.castRight());
	}

	public static void loadJob(ValueInput input, Dragline.Job job) {
		job.set(0, input.getIntOr("job_width", Dragline.WIDTH));
		job.set(1, input.getIntOr("job_length", Dragline.LENGTH));
		job.set(2, input.getIntOr("job_seam", Dragline.SEAM));
		job.set(3, input.getBooleanOr("job_cast_right", true) ? 1 : 0);
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		output.putBoolean("grown", grown);
		output.putString("machine", machine.toString());
		saveJob(output, job);
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
		loadJob(input, job);
		cable = CableLink.load(input);
	}
}
