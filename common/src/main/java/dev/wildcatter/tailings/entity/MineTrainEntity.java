package dev.wildcatter.tailings.entity;

import dev.wildcatter.tailings.TailingsEntities;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * The block-stepped train of a world saved before R5. Its train came back as {@link MineVehicleEntity} from the rails it was
 * stored on, so this discards itself on its first tick.
 */
public class MineTrainEntity extends Entity {
	public MineTrainEntity(EntityType<?> type, Level level) {
		super(type, level);
		setNoGravity(true);
	}

	public MineTrainEntity(Level level) {
		this(TailingsEntities.TRAIN, level);
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
	}

	@Override
	protected void addAdditionalSaveData(ValueOutput output) {
	}

	@Override
	public void tick() {
		super.tick();
		if (!level().isClientSide()) {
			discard();
		}
	}

	@Override
	public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
		return false;
	}
}
