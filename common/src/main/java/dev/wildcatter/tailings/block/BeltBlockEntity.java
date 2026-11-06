package dev.wildcatter.tailings.block;

import dev.wildcatter.tailings.TailingsBlocks;
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
