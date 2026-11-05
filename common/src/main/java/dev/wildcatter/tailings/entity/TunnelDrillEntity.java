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
 * Reserved for the contraption drill (a later wave). Nothing spawns it now: the drill's chunks are held by a
 * {@link dev.wildcatter.tailings.block.ChunkRunner} ticket. One left in a saved world by an older build discards itself.
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

	@Override
	public boolean isPickable() {
		return false;
	}
}
