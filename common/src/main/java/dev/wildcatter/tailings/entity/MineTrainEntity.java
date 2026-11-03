package dev.wildcatter.tailings.entity;

import dev.wildcatter.tailings.TailingsEntities;
import dev.wildcatter.tailings.block.RailBlockEntity;
import dev.wildcatter.tailings.haul.TrainHarm;
import dev.wildcatter.tailings.platform.Platform;
import java.util.List;
import net.minecraft.core.BlockPos;
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
 * A locomotive or car as an entity on the rail. The locomotive steps the consist, keeps a chunk ticket on the route,
 * lights the block above it, and hurts living things it touches unless the config turns that off.
 */
public class MineTrainEntity extends Entity {
	private boolean locomotive = true;
	private BlockPos rail = BlockPos.ZERO;
	private BlockPos lamp;
	private int wait;

	public MineTrainEntity(EntityType<?> type, Level level) {
		super(type, level);
		setNoGravity(true);
	}

	public MineTrainEntity(Level level, boolean locomotive) {
		this(TailingsEntities.TRAIN, level);
		this.locomotive = locomotive;
	}

	public void attach(BlockPos rail) {
		this.rail = rail;
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
		locomotive = input.getBooleanOr("loco", true);
		rail = BlockPos.of(input.getLongOr("rail", 0));
	}

	@Override
	protected void addAdditionalSaveData(ValueOutput output) {
		output.putBoolean("loco", locomotive);
		output.putLong("rail", rail.asLong());
	}

	@Override
	public void tick() {
		super.tick();
		if (!(level() instanceof ServerLevel server) || !locomotive) {
			return;
		}
		if (!(server.getBlockEntity(rail) instanceof RailBlockEntity track) || track.kind() != RailBlockEntity.Kind.LOCO) {
			discard();
			clearLamp(server);
			return;
		}
		Platform.INSTANCE.forceChunk(server, getUUID(), rail.getX() >> 4, rail.getZ() >> 4, true);
		BlockPos ahead = rail.relative(track.getBlockState().getValue(dev.wildcatter.tailings.block.MineRailBlock.FACING));
		Platform.INSTANCE.forceChunk(server, getUUID(), ahead.getX() >> 4, ahead.getZ() >> 4, true);
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
		List<String> config = TrainHarm.read(Platform.INSTANCE.configDir().resolve("tailings-train.properties"));
		if (!TrainHarm.enabled(config)) {
			return;
		}
		float amount = TrainHarm.amount(config);
		AABB box = new AABB(rail);
		for (RailBlockEntity part : track.consist()) {
			box = box.minmax(new AABB(part.getBlockPos()));
		}
		box = box.inflate(0.4, 1.2, 0.4);
		for (LivingEntity living : server.getEntities(EntityTypeTest.forClass(LivingEntity.class), box, entity -> true)) {
			living.hurtServer(server, server.damageSources().generic(), amount);
		}
	}

	private void headlight(ServerLevel server) {
		clearLamp(server);
		BlockPos above = rail.above();
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
			Platform.INSTANCE.forceChunk(server, getUUID(), rail.getX() >> 4, rail.getZ() >> 4, false);
		}
		super.remove(reason);
	}
}
