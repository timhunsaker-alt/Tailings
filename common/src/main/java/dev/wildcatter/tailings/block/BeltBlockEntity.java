package dev.wildcatter.tailings.block;

import dev.wildcatter.tailings.TailingsBlocks;
import dev.wildcatter.tailings.plant.BeltGrade;
import java.util.Objects;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

/**
 * One metre of belt: which conveyor drive holds its run's load, and what the renderer draws on it (the run's main rock, how full the
 * run is, and which way it moves). The load itself is the drive's. It never ticks: the drive sets it.
 */
public class BeltBlockEntity extends BlockEntity {
	private @Nullable BlockPos drive;
	private String rock = "";
	private float fill;
	private @Nullable Direction toward;
	private boolean moving;
	/** The belt's surface at this block's start, middle and end along its run, above its floor ({@link BeltGrade}). */
	private final float[] grade = {(float) BeltGrade.FLAT, (float) BeltGrade.FLAT, (float) BeltGrade.FLAT};
	/** Loaded from a save made before belts were graded. */
	private boolean ungraded;
	/** On the client: how many open blocks are under this one, down to the ground its legs stand on, and the game time it was looked at. */
	private int drop;
	private long dropAt = -1000;
	/** The shape for {@link #grade}, made when first asked for. */
	private @Nullable VoxelShape shape;

	public BeltBlockEntity(BlockPos pos, BlockState state) {
		super(TailingsBlocks.BELT, pos, state);
	}

	/** The main cell of the drive holding this run's load, or null when the run has no head drive. */
	public @Nullable BlockPos drive() {
		return drive;
	}

	public String rock() {
		return rock;
	}

	public float fill() {
		return fill;
	}

	public @Nullable Direction toward() {
		return toward;
	}

	public boolean moving() {
		return moving;
	}

	/** The belt's surface at this block's start, middle and end along its run (toward +x or +z), above the block's floor. */
	public float[] grade() {
		return grade;
	}

	/** Sets where this block sits on its run's grade; tells the client when it changes. */
	public void setGrade(double[] profile) {
		boolean changed = false;
		for (int i = 0; i < 3; i++) {
			if (Math.abs(grade[i] - profile[i]) > 1e-4) {
				grade[i] = (float) profile[i];
				changed = true;
			}
		}
		if (!changed) {
			return;
		}
		shape = null;
		ungraded = false;
		setChanged();
		if (level != null && !level.isClientSide()) {
			level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
		}
	}

	/** How far its legs reach down to the ground, as last looked at; {@code look} counts again at most once a second. */
	public int drop(long now, java.util.function.IntSupplier look) {
		if (now - dropAt >= 20 || now < dropAt) {
			drop = look.getAsInt();
			dropAt = now;
		}
		return drop;
	}

	void graded() {
		ungraded = false;
	}

	/** Whether it was loaded from a save made before belts were graded, and not graded since. */
	public boolean ungraded() {
		return ungraded;
	}

	/** The cached shape for this grade, or null when it is to be made again. */
	@Nullable VoxelShape shape() {
		return shape;
	}

	void shape(VoxelShape made) {
		shape = made;
	}

	/** The drive's scan: who holds the run, and what to draw. Sends the client an update only when it changes. */
	public void set(@Nullable BlockPos drive, String rock, float fill, @Nullable Direction toward, boolean moving) {
		float shown = Math.round(fill * 8) / 8f;
		boolean drawn = !rock.equals(this.rock) || shown != this.fill || toward != this.toward || moving != this.moving;
		if (!drawn && Objects.equals(drive, this.drive)) {
			return;
		}
		this.drive = drive;
		this.rock = rock;
		this.fill = shown;
		this.toward = toward;
		this.moving = moving;
		setChanged();
		if (drawn && level != null && !level.isClientSide()) {
			level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
		}
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		output.putLong("drive", drive == null ? Long.MIN_VALUE : drive.asLong());
		output.putString("rock", rock);
		output.putFloat("fill", fill);
		output.putInt("toward", toward == null ? -1 : toward.get3DDataValue());
		output.putBoolean("moving", moving);
		output.putFloat("grade0", grade[0]);
		output.putFloat("grade1", grade[1]);
		output.putFloat("grade2", grade[2]);
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		long at = input.getLongOr("drive", Long.MIN_VALUE);
		drive = at == Long.MIN_VALUE ? null : BlockPos.of(at);
		rock = input.getStringOr("rock", "");
		fill = input.getFloatOr("fill", 0);
		int way = input.getIntOr("toward", -1);
		toward = way < 0 ? null : Direction.from3DDataValue(way);
		moving = input.getBooleanOr("moving", false);
		// a belt saved before grades were drawn is graded again when its drive next scans its run
		ungraded = input.getFloatOr("grade0", -1) < 0;
		for (int i = 0; i < 3; i++) {
			grade[i] = input.getFloatOr("grade" + i, (float) BeltGrade.FLAT);
		}
		shape = null;
	}

	@Override
	public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
		return saveCustomOnly(registries);
	}

	@Override
	public Packet<ClientGamePacketListener> getUpdatePacket() {
		return ClientboundBlockEntityDataPacket.create(this);
	}
}
