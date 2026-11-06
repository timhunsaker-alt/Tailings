package dev.wildcatter.tailings.entity;

import dev.wildcatter.tailings.TailingsEntities;
import dev.wildcatter.tailings.TailingsItems;
import dev.wildcatter.tailings.block.DrillBlockEntity;
import dev.wildcatter.tailings.block.RailView;
import dev.wildcatter.tailings.block.TbmBackup;
import dev.wildcatter.tailings.block.TbmCab;
import dev.wildcatter.tailings.block.TbmMachine;
import dev.wildcatter.tailings.mine.TbmGantry;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
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
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * A back-up gantry (docs/multiblocks/tunnel-boring-machine.md): placed on the twin track like a car, coupled to the machine with a
 * sneaking click, and from then on the machine sets it at its place behind itself each tick. Its position is its main cell's corner,
 * like the machine's. A train stops short of it, at the end of the way.
 */
public class GantryEntity extends Entity implements SoundingEntity {
	private static final EntityDataAccessor<Integer> DATA_FACING = SynchedEntityData.defineId(GantryEntity.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Boolean> DATA_WORKING = SynchedEntityData.defineId(GantryEntity.class, EntityDataSerializers.BOOLEAN);
	private static final int WORK_HOLD_TICKS = 20;
	private int workHold;
	private static final EntityDataAccessor<Float> DATA_PITCH = SynchedEntityData.defineId(GantryEntity.class, EntityDataSerializers.FLOAT);
	/** How far a gantry may stand from its place behind the machine and still couple. */
	private static final double COUPLE_REACH = 8;
	/** The track columns each gantry stood over last tick, per level. */
	private static final Map<Level, Map<Long, Stand>> STANDING = new WeakHashMap<>();

	private record Stand(int y, long tick) {
	}

	private boolean coupled;

	public GantryEntity(EntityType<?> type, Level level) {
		super(type, level);
		setNoGravity(true);
		noPhysics = true;
	}

	public TbmGantry kind() {
		return getType() == TailingsEntities.PUMP_GANTRY ? TbmGantry.PUMP : getType() == TailingsEntities.REEL_GANTRY ? TbmGantry.REEL : TbmGantry.CONVEYOR;
	}

	public Direction facing() {
		return Direction.from3DDataValue(entityData.get(DATA_FACING));
	}

	public float pitch() {
		return entityData.get(DATA_PITCH);
	}

	/** The machine it serves is driving. */
	public void working() {
		workHold = WORK_HOLD_TICKS;
	}

	@Override
	public net.minecraft.sounds.@Nullable SoundEvent loop() {
		if (!entityData.get(DATA_WORKING)) {
			return null;
		}
		return switch (kind()) {
			case PUMP -> dev.wildcatter.WildcatterSounds.MOTOR_HUM;
			case CONVEYOR -> dev.wildcatter.tailings.TailingsSounds.BELT_CONVEYOR;
			case REEL -> null;
		};
	}

	@Override
	public float soundPitch() {
		return 1F;
	}

	@Override
	public float soundVolume() {
		return entityData.get(DATA_WORKING) ? 0.6F : 0;
	}

	public boolean coupled() {
		return coupled;
	}

	/** The machine lets it go: it stands where it is. */
	public void uncouple() {
		coupled = false;
	}

	/** Puts a gantry astride the twin track at the rail {@code pos}, facing {@code facing}; either rail will do. */
	public static @Nullable GantryEntity place(ServerLevel level, BlockPos pos, TbmGantry kind, Direction facing) {
		RailView view = new RailView(level);
		if (view.at(pos) == null) {
			return null;
		}
		Direction across = facing.getClockWise();
		int gap = TbmGantry.RIGHT_RAIL - TbmGantry.LEFT_RAIL;
		BlockPos corner = view.at(pos.relative(across, gap)) != null ? pos.relative(across, -TbmGantry.LEFT_RAIL) : pos.relative(across, -TbmGantry.RIGHT_RAIL);
		GantryEntity gantry = new GantryEntity(TailingsEntities.gantry(kind), level);
		gantry.setAt(new Vec3(corner.getX(), corner.getY(), corner.getZ()), facing, 0);
		level.addFreshEntity(gantry);
		return gantry;
	}

	/** Sets it at {@code at}, the corner of its main cell. */
	public void setAt(Vec3 at, Direction facing, float pitch) {
		entityData.set(DATA_FACING, facing.get3DDataValue());
		entityData.set(DATA_PITCH, pitch);
		setPos(at.x, at.y, at.z);
		setBoundingBox(makeBoundingBox(at));
	}

	/** Couples to the machine whose next gantry this is and which it stands near; says what happened. */
	public String couple() {
		if (coupled) {
			return "already coupled";
		}
		if (!(level() instanceof ServerLevel server)) {
			return "";
		}
		String why = "no machine within reach";
		for (TbmCab cab : cabsNear(server)) {
			TbmGantry next = TbmBackup.next(cab.core());
			Vec3 slot = TbmBackup.slot(cab, kind());
			if (position().distanceToSqr(slot) > COUPLE_REACH * COUPLE_REACH) {
				continue;
			}
			if (next != kind()) {
				why = next == null ? "this machine has its gantries" : "couple the " + next.displayName() + " first";
				continue;
			}
			cab.core().couple(kind(), getUUID());
			coupled = true;
			level().playSound(null, getX(), getY(), getZ(), dev.wildcatter.tailings.TailingsSounds.CAR_COUPLE, net.minecraft.sounds.SoundSource.BLOCKS, 1F, 1F);
			TbmBackup.place(server, cab);
			return "coupled";
		}
		return why;
	}

	private List<TbmCab> cabsNear(ServerLevel server) {
		List<TbmCab> out = new ArrayList<>(server.getEntitiesOfClass(TbmEntity.class, getBoundingBox().inflate(40)));
		ChunkPos here = ChunkPos.containing(blockPosition());
		for (int dx = -3; dx <= 3; dx++) {
			for (int dz = -3; dz <= 3; dz++) {
				if (server.hasChunk(here.x() + dx, here.z() + dz)) {
					for (BlockEntity entity : server.getChunk(here.x() + dx, here.z() + dz).getBlockEntities().values()) {
						if (entity instanceof DrillBlockEntity cab && cab.standing()) {
							out.add(cab);
						}
					}
				}
			}
		}
		return out;
	}

	/** True if a gantry stands over the track at this place. */
	public static boolean standingAt(Level level, BlockPos rail) {
		Map<Long, Stand> here = STANDING.get(level);
		Stand stand = here == null ? null : here.get(BlockPos.asLong(rail.getX(), 0, rail.getZ()));
		return stand != null && level.getGameTime() - stand.tick() <= 2 && Math.abs(stand.y() - rail.getY()) <= 8;
	}

	@Override
	public void tick() {
		super.tick();
		if (!(level() instanceof ServerLevel server)) {
			SoundingEntity.Hook.start.accept(this);
			return;
		}
		workHold = Math.max(0, workHold - 1);
		entityData.set(DATA_WORKING, workHold > 0);
		Map<Long, Stand> here = STANDING.computeIfAbsent(server, l -> new HashMap<>());
		long now = server.getGameTime();
		if (now % 200 == 0) {
			here.values().removeIf(s -> now - s.tick() > 2);
		}
		BlockPos corner = BlockPos.containing(getX(), getY(), getZ());
		Direction facing = facing();
		for (int row = -1; row <= kind().length(); row++) {
			for (int across : new int[] {TbmGantry.LEFT_RAIL, TbmGantry.RIGHT_RAIL}) {
				BlockPos at = corner.offset(TbmMachine.offset(facing, across, 0, row));
				here.put(BlockPos.asLong(at.getX(), 0, at.getZ()), new Stand(at.getY(), now));
			}
		}
	}

	/** Its frame's outline, the rear up when pitched. */
	@Override
	protected AABB makeBoundingBox(Vec3 at) {
		if (entityData == null) {
			return super.makeBoundingBox(at);
		}
		BlockPos far = TbmMachine.offset(facing(), TbmGantry.WIDTH - 1, 0, kind().length() - 1);
		double rise = kind().length() * Math.sin(Math.toRadians(pitch()));
		return new AABB(at.x + Math.min(0, far.getX()), at.y, at.z + Math.min(0, far.getZ()),
				at.x + Math.max(0, far.getX()) + 1, at.y + TbmGantry.HEIGHT + rise, at.z + Math.max(0, far.getZ()) + 1);
	}

	@Override
	public void onSyncedDataUpdated(EntityDataAccessor<?> accessor) {
		super.onSyncedDataUpdated(accessor);
		if (DATA_FACING.equals(accessor) || DATA_PITCH.equals(accessor)) {
			setBoundingBox(makeBoundingBox(position()));
		}
	}

	/** A sneaking click with an empty hand couples it. */
	@Override
	public InteractionResult interact(Player player, InteractionHand hand, Vec3 location) {
		if (level().isClientSide()) {
			return InteractionResult.SUCCESS;
		}
		String said = player.isShiftKeyDown() && player.getItemInHand(hand).isEmpty() ? couple() : coupled ? "coupled" : "not coupled";
		player.sendOverlayMessage(Component.literal(kind().displayName() + ": " + said));
		return InteractionResult.SUCCESS;
	}

	@Override
	public boolean isPickable() {
		return !isRemoved();
	}

	/** Only a sneaking hit with an empty hand breaks it, into its item. The machine stops, reading the missing gantry. */
	@Override
	public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
		if (source.getEntity() instanceof Player player && player.isShiftKeyDown() && player.getMainHandItem().isEmpty()) {
			spawnAtLocation(level, new ItemStack(TailingsItems.gantry(kind())));
			discard();
			return true;
		}
		return false;
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		builder.define(DATA_FACING, Direction.NORTH.get3DDataValue());
		builder.define(DATA_PITCH, 0F);
		builder.define(DATA_WORKING, false);
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
		entityData.set(DATA_FACING, input.getIntOr("facing", Direction.NORTH.get3DDataValue()));
		entityData.set(DATA_PITCH, input.getFloatOr("pitch", 0F));
		coupled = input.getBooleanOr("coupled", false);
	}

	@Override
	protected void addAdditionalSaveData(ValueOutput output) {
		output.putInt("facing", entityData.get(DATA_FACING));
		output.putFloat("pitch", entityData.get(DATA_PITCH));
		output.putBoolean("coupled", coupled);
	}
}
