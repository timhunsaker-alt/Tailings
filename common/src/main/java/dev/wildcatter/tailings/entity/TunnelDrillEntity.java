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
 * The contraption entity that owns the moving TBM. The cab and pump blocks ride with it and are re-placed each
 * advance; this entity is what the chunk ticket is filed against, so the machine keeps cutting when no player is near.
 * It is invisible: the blocks are the machine the player sees.
 */
public class TunnelDrillEntity extends Entity {
	public TunnelDrillEntity(EntityType<?> type, Level level) {
		super(type, level);
		setNoGravity(true);
		setInvisible(true);
	}

	public TunnelDrillEntity(Level level) {
		this(TailingsEntities.DRILL, level);
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
	public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
		return false;
	}

	@Override
	public boolean isPickable() {
		return false;
	}
}
