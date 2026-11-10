package dev.wildcatter.tailings.entity;

import dev.wildcatter.tailings.TailingsEntities;
import dev.wildcatter.tailings.TailingsItems;
import dev.wildcatter.tailings.block.ShaftLayout;
import dev.wildcatter.tailings.block.TbmMachine;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * A 3 t bottom-dump skip on its rope in a headframe's compartment (docs/multiblocks/shaft-and-hoist.md, L4). Hung by clicking the
 * headframe with a skip; from then on the hoist house sets it in its compartment each tick, as far up the shaft as its drum puts it.
 * Its load rides in the hoist's winder; the skip shows it. Its position is its main cell's corner, as the TBM's parts.
 */
public class ShaftSkipEntity extends Entity {
	private static final EntityDataAccessor<Integer> DATA_FACING = SynchedEntityData.defineId(ShaftSkipEntity.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Boolean> DATA_LOADED = SynchedEntityData.defineId(ShaftSkipEntity.class, EntityDataSerializers.BOOLEAN);
	/** The head sheave's top, for drawing the rope up to it; below the world for no rope. */
	private static final EntityDataAccessor<Float> DATA_SHEAVE_Y = SynchedEntityData.defineId(ShaftSkipEntity.class, EntityDataSerializers.FLOAT);
	/** The hoist's drum the rope leads back to over the sheave, when a hoist works it. */
	private static final EntityDataAccessor<Optional<BlockPos>> DATA_DRUM = SynchedEntityData.defineId(ShaftSkipEntity.class, EntityDataSerializers.OPTIONAL_BLOCK_POS);
	public static final float NO_ROPE = -1.0e4F;

	/** The headframe's main cell it hangs in, and which compartment (0 west, 1 east, in the headframe's own frame). */
	private BlockPos headframe = BlockPos.ZERO;
	private int compartment;

	public ShaftSkipEntity(EntityType<?> type, Level level) {
		super(type, level);
		setNoGravity(true);
		noPhysics = true;
	}

	public Direction facing() {
		return Direction.from3DDataValue(entityData.get(DATA_FACING));
	}

	public boolean loaded() {
		return entityData.get(DATA_LOADED);
	}

	public float sheaveY() {
		return entityData.get(DATA_SHEAVE_Y);
	}

	public @Nullable BlockPos drum() {
		return entityData.get(DATA_DRUM).orElse(null);
	}

	public BlockPos headframe() {
		return headframe;
	}

	public int compartment() {
		return compartment;
	}

	public boolean hangsIn(BlockPos frame, int which) {
		return headframe.equals(frame) && compartment == which;
	}

	/** Shows its load. */
	public void setLoaded(boolean loaded) {
		if (loaded != entityData.get(DATA_LOADED)) {
			entityData.set(DATA_LOADED, loaded);
		}
	}

	/** Where its rope runs: up to the sheave at {@code sheaveY} and back to {@code drum}. */
	public void setRope(float sheaveY, @Nullable BlockPos drum) {
		if (Math.abs(entityData.get(DATA_SHEAVE_Y) - sheaveY) > 1e-3) {
			entityData.set(DATA_SHEAVE_Y, sheaveY);
		}
		Optional<BlockPos> at = Optional.ofNullable(drum);
		if (!entityData.get(DATA_DRUM).equals(at)) {
			entityData.set(DATA_DRUM, at);
		}
	}

	/** Sets it at {@code at}, its main cell's corner. */
	public void setAt(Vec3 at, Direction facing) {
		if (facing != facing()) {
			entityData.set(DATA_FACING, facing.get3DDataValue());
		}
		setPos(at.x, at.y, at.z);
		setBoundingBox(makeBoundingBox(at));
	}

	/**
	 * Hangs a skip in the headframe whose main cell is {@code frame}: in the first compartment with none, at the dump. Null when both
	 * compartments have their skips.
	 */
	public static @Nullable ShaftSkipEntity hang(ServerLevel level, ShaftLayout.Frame frame) {
		List<ShaftSkipEntity> there = near(level, frame);
		for (int which = 0; which < 2; which++) {
			final int c = which;
			if (there.stream().noneMatch(s -> s.hangsIn(frame.main(), c))) {
				ShaftSkipEntity skip = new ShaftSkipEntity(TailingsEntities.SKIP, level);
				skip.headframe = frame.main();
				skip.compartment = c;
				skip.setAt(frame.skipCorner(c, frame.collarY() + dev.wildcatter.tailings.shaft.Hoisting.DUMP_HEIGHT), frame.facing());
				level.addFreshEntity(skip);
				return skip;
			}
		}
		return null;
	}

	/** The skips hanging in this headframe, anywhere up or down its shaft. */
	public static List<ShaftSkipEntity> near(ServerLevel level, ShaftLayout.Frame frame) {
		return level.getEntitiesOfClass(ShaftSkipEntity.class, frame.shaftBox(), s -> !s.isRemoved() && s.headframe.equals(frame.main()));
	}

	@Override
	protected AABB makeBoundingBox(Vec3 at) {
		if (entityData == null) {
			return super.makeBoundingBox(at);
		}
		BlockPos far = TbmMachine.offset(facing(), dev.wildcatter.tailings.block.SkipPartBlock.WIDTH - 1, 0, dev.wildcatter.tailings.block.SkipPartBlock.WIDTH - 1);
		return new AABB(at.x + Math.min(0, far.getX()), at.y, at.z + Math.min(0, far.getZ()),
				at.x + Math.max(0, far.getX()) + 1, at.y + dev.wildcatter.tailings.block.SkipPartBlock.HEIGHT, at.z + Math.max(0, far.getZ()) + 1);
	}

	@Override
	public void onSyncedDataUpdated(EntityDataAccessor<?> accessor) {
		super.onSyncedDataUpdated(accessor);
		if (DATA_FACING.equals(accessor)) {
			setBoundingBox(makeBoundingBox(position()));
		}
	}

	@Override
	public InteractionResult interact(Player player, InteractionHand hand, Vec3 location) {
		if (!level().isClientSide()) {
			player.sendOverlayMessage(Component.literal("Skip (3 t), " + (compartment == 0 ? "west" : "east") + " compartment: " + (loaded() ? "loaded" : "empty")));
		}
		return InteractionResult.SUCCESS;
	}

	@Override
	public boolean isPickable() {
		return !isRemoved();
	}

	/** Only a sneaking hit with an empty hand takes it off its rope, into its item; the hoist then reads NOT BUILT. */
	@Override
	public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
		if (source.getEntity() instanceof Player player && player.isShiftKeyDown() && player.getMainHandItem().isEmpty()) {
			spawnAtLocation(level, new ItemStack(TailingsItems.SKIP));
			discard();
			return true;
		}
		return false;
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		builder.define(DATA_FACING, Direction.NORTH.get3DDataValue());
		builder.define(DATA_LOADED, false);
		builder.define(DATA_SHEAVE_Y, NO_ROPE);
		builder.define(DATA_DRUM, Optional.empty());
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
		entityData.set(DATA_FACING, input.getIntOr("facing", Direction.NORTH.get3DDataValue()));
		headframe = BlockPos.of(input.getLongOr("headframe", 0L));
		compartment = input.getIntOr("compartment", 0);
		entityData.set(DATA_LOADED, input.getBooleanOr("loaded", false));
	}

	@Override
	protected void addAdditionalSaveData(ValueOutput output) {
		output.putInt("facing", entityData.get(DATA_FACING));
		output.putLong("headframe", headframe.asLong());
		output.putInt("compartment", compartment);
		output.putBoolean("loaded", loaded());
	}
}
