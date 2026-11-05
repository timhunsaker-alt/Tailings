package dev.wildcatter.tailings.entity;

import dev.wildcatter.tailings.TailingsEntities;
import dev.wildcatter.tailings.block.ChunkRunner;
import dev.wildcatter.tailings.block.RailBlockEntity;
import dev.wildcatter.tailings.haul.TrainHarm;
import dev.wildcatter.tailings.platform.Platform;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LightBlock;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;

/**
 * A locomotive or car as an entity on the rail. The locomotive steps the consist, refreshes a chunk ticket on its route,
 * lights the block above it, and hurts living things it touches unless the config turns that off.
 */
public class MineTrainEntity extends Entity {
	private static final EntityDataAccessor<Boolean> DATA_LOCOMOTIVE = SynchedEntityData.defineId(MineTrainEntity.class, EntityDataSerializers.BOOLEAN);

	private BlockPos rail = BlockPos.ZERO;
	private BlockPos lamp;
	private int wait;
	private int refresh;

	public MineTrainEntity(EntityType<?> type, Level level) {
		super(type, level);
		setNoGravity(true);
	}

	public MineTrainEntity(Level level, boolean locomotive) {
		this(TailingsEntities.TRAIN, level);
		setLocomotive(locomotive);
	}

	public void attach(BlockPos rail) {
		this.rail = rail;
	}

	public boolean locomotive() {
		return entityData.get(DATA_LOCOMOTIVE);
	}

	public void setLocomotive(boolean locomotive) {
		entityData.set(DATA_LOCOMOTIVE, locomotive);
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		builder.define(DATA_LOCOMOTIVE, true);
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
		setLocomotive(input.getBooleanOr("loco", true));
		rail = BlockPos.of(input.getLongOr("rail", 0));
	}

	@Override
	protected void addAdditionalSaveData(ValueOutput output) {
		output.putBoolean("loco", locomotive());
		output.putLong("rail", rail.asLong());
	}

	@Override
	public void tick() {
		super.tick();
		if (!(level() instanceof ServerLevel server) || !locomotive()) {
			return;
		}
		if (!(server.getBlockEntity(rail) instanceof RailBlockEntity track) || track.kind() != RailBlockEntity.Kind.LOCO) {
			discard();
			clearLamp(server);
			return;
		}
		if (refresh-- <= 0) {
			refresh = ChunkRunner.REFRESH_TICKS;
			ChunkRunner.refresh(server, rail);
			ChunkRunner.refresh(server, rail.relative(track.getBlockState().getValue(dev.wildcatter.tailings.block.MineRailBlock.FACING)));
		}
		strike(server, track);
		if (wait > 0) {
			wait--;
		} else {
			String id = track.trainId();
			BlockPos from = rail;
			if (track.stepOnce()) {
				boolean decline = track.getBlockState().hasProperty(dev.wildcatter.tailings.block.MineRailBlock.GRADE)
						&& track.getBlockState().getValue(dev.wildcatter.tailings.block.MineRailBlock.GRADE) != dev.wildcatter.tailings.block.RailGrade.FLAT;
				wait = dev.wildcatter.tailings.haul.Motion.ticks(decline);
				for (net.minecraft.core.Direction direction : net.minecraft.core.Direction.values()) {
					BlockPos next = from.relative(direction);
					if (server.getBlockEntity(next) instanceof RailBlockEntity moved && id.equals(moved.trainId())
							&& moved.kind() == RailBlockEntity.Kind.LOCO) {
						rail = next;
						setPos(next.getX() + 0.5, next.getY(), next.getZ() + 0.5);
					}
				}
			}
		}
		headlight(server);
	}

	private void strike(ServerLevel server, RailBlockEntity track) {
		float amount = TrainHarm.damageFor(server.getServer(), Platform.INSTANCE.configDir().resolve("tailings-train.properties"));
		if (amount <= 0) {
			return;
		}
		AABB box = new AABB(rail);
		for (RailBlockEntity part : track.consist()) {
			box = box.minmax(new AABB(part.getBlockPos()));
		}
		box = box.inflate(0.4, 1.2, 0.4);
		for (LivingEntity living : server.getEntities(EntityTypeTest.forClass(LivingEntity.class), box, entity -> true)) {
			living.hurtServer(server, server.damageSources().generic(), amount);
		}
	}

	/** Moves the light block to the air above the locomotive, only when the locomotive has moved. */
	private void headlight(ServerLevel server) {
		BlockPos above = rail.above();
		if (above.equals(lamp) && server.getBlockState(lamp).getBlock() == Blocks.LIGHT) {
			return;
		}
		clearLamp(server);
		if (server.getBlockState(above).isAir()) {
			server.setBlock(above, Blocks.LIGHT.defaultBlockState().setValue(LightBlock.LEVEL, 15), 3);
			lamp = above;
		}
	}

	private void clearLamp(ServerLevel server) {
		if (lamp != null && server.getBlockState(lamp).getBlock() == Blocks.LIGHT) {
			server.removeBlock(lamp, false);
		}
		lamp = null;
	}

	@Override
	public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
		return false;
	}

	@Override
	public void remove(RemovalReason reason) {
		if (level() instanceof ServerLevel server) {
			clearLamp(server);
		}
		super.remove(reason);
	}
}
