package dev.wildcatter.tailings.entity;

import dev.wildcatter.tailings.TailingsEntities;
import dev.wildcatter.tailings.TailingsItems;
import dev.wildcatter.tailings.block.DrillBlockEntity;
import dev.wildcatter.tailings.block.RailView;
import dev.wildcatter.tailings.block.TbmBackup;
import dev.wildcatter.tailings.block.TbmCab;
import dev.wildcatter.tailings.block.TbmMachine;
import dev.wildcatter.tailings.haul.Track;
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
	/** The reel's: the cable junction its trailing cable runs back to, and the metres paid out, for drawing the cable and turning the reel. */
	private static final EntityDataAccessor<java.util.Optional<BlockPos>> DATA_JUNCTION = SynchedEntityData.defineId(GantryEntity.class, EntityDataSerializers.OPTIONAL_BLOCK_POS);
	private static final EntityDataAccessor<Float> DATA_PAID = SynchedEntityData.defineId(GantryEntity.class, EntityDataSerializers.FLOAT);
	/** How far a gantry may stand from its place behind the machine and still couple. */
	private static final double COUPLE_REACH = 8;
	/** The track columns each gantry stood over last tick, per level. */
	private static final Map<Level, Map<Long, Stand>> STANDING = new WeakHashMap<>();

	private record Stand(int y, long tick) {
	}

	private boolean coupled;
	/** Its wheels on the twin track. */
	private final GantryWheels wheels = new GantryWheels();
	/** The pump's: the quench tank's water, in buckets (m3). It rides here, not in the cab; it is lost with the gantry. */
	private int water;

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

	/** True while both its rails carry it. */
	public boolean onTrack() {
		return wheels.railed();
	}

	public int water() {
		return water;
	}

	public void setWater(int buckets) {
		water = Math.max(0, buckets);
	}

	/** The reel's cable: the junction it runs back to and the metres paid out. */
	public void setCable(@Nullable BlockPos junction, float paid) {
		java.util.Optional<BlockPos> at = java.util.Optional.ofNullable(junction);
		if (!entityData.get(DATA_JUNCTION).equals(at)) {
			entityData.set(DATA_JUNCTION, at);
		}
		if (Math.abs(entityData.get(DATA_PAID) - paid) > 1e-3) {
			entityData.set(DATA_PAID, paid);
		}
	}

	public @Nullable BlockPos cableJunction() {
		return entityData.get(DATA_JUNCTION).orElse(null);
	}

	public float paidOut() {
		return entityData.get(DATA_PAID);
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
		gantry.railUnder(view);
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

	/** Lays its wheels on the rails under its front row, where it stands now. */
	private boolean railUnder(Track.View view) {
		Direction facing = facing();
		Vec3 at = position();
		BlockPos left = TbmMachine.offset(facing, TbmGantry.LEFT_RAIL, 0, 0), right = TbmMachine.offset(facing, TbmGantry.RIGHT_RAIL, 0, 0);
		int y = (int) Math.floor(at.y + 1e-3);
		for (int dy : new int[] {0, -1, 1}) {
			int[] l = {(int) Math.floor(at.x + left.getX() + 1e-3), y + dy, (int) Math.floor(at.z + left.getZ() + 1e-3)};
			int[] r = {(int) Math.floor(at.x + right.getX() + 1e-3), y + dy, (int) Math.floor(at.z + right.getZ() + 1e-3)};
			if (wheels.rail(view, l, r, RailView.heading(facing))) {
				return true;
			}
		}
		return false;
	}

	/**
	 * The machine tows it: its main cell's corner is to stand at {@code slot}, as far along the bore as the machine's course puts it.
	 * Its wheels run along the twin track to that place, and the rails set where it stands, its height and its pitch. Off the track,
	 * it stays where it is; returns false.
	 */
	public boolean tow(Vec3 slot, Direction facing) {
		if (!(level() instanceof ServerLevel server)) {
			return false;
		}
		RailView view = new RailView(server);
		if (facing != facing()) {
			entityData.set(DATA_FACING, facing.get3DDataValue());
			wheels.derail();
		}
		dev.wildcatter.tailings.mine.Heading heading = RailView.heading(facing);
		BlockPos left = TbmMachine.offset(facing, TbmGantry.LEFT_RAIL, 0, 0);
		// the front edge of its front row, on the left rail's centreline
		double along = (slot.x + left.getX() + 0.5) * heading.forwardX() + (slot.z + left.getZ() + 0.5) * heading.forwardZ() + 0.5;
		if (!wheels.railed()) {
			// put back on the track: its wheels go on the rails at its place
			Vec3 was = position();
			setPos(slot.x, slot.y, slot.z);
			boolean railed = railUnder(view);
			setPos(was.x, was.y, was.z);
			if (!railed) {
				return false;
			}
		}
		double[] front = wheels.follow(view, along, kind().length(), heading);
		if (front == null) {
			return false;
		}
		// the rail cell under its front row, from the front edge's point; its corner, then the gantry's
		double cellX = front[0] - 0.5 * heading.forwardX() - 0.5, cellZ = front[2] - 0.5 * heading.forwardZ() - 0.5;
		float pitch = (float) Math.toDegrees(Math.atan2(front[3] - front[1], kind().length()));
		setAt(new Vec3(cellX - left.getX(), front[1] - 1, cellZ - left.getZ()), facing, Math.abs(pitch) < 1e-4 ? 0 : pitch);
		return true;
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
		if (placedBeforeWheels) {
			placedBeforeWheels = false;
			railUnder(new RailView(server));
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

	/** A duct cassette goes on the reel gantry for the machine it is coupled to (ventilation.md): 100 m of duct to pay out. */
	private String loadCassette(ServerLevel server, Player player, InteractionHand hand) {
		if (!coupled) {
			return "Reel Gantry: couple it to its machine first";
		}
		for (TbmCab cab : cabsNear(server)) {
			if (getUUID().equals(cab.core().gantry(TbmGantry.REEL))) {
				if (!cab.core().loadCassette()) {
					return "Reel Gantry: the cassette aboard still has " + cab.core().ductLeft() + " m of duct";
				}
				player.getItemInHand(hand).consume(1, player);
				return "Reel Gantry: a new duct cassette, " + cab.core().ductLeft() + " m to pay out";
			}
		}
		return "Reel Gantry: no machine within reach";
	}

	/** A sneaking click with an empty hand couples it. */
	@Override
	public InteractionResult interact(Player player, InteractionHand hand, Vec3 location) {
		if (level().isClientSide()) {
			return InteractionResult.SUCCESS;
		}
		if (kind() == TbmGantry.REEL && player.getItemInHand(hand).is(dev.wildcatter.tailings.TailingsItems.DUCT_CASSETTE) && level() instanceof ServerLevel server) {
			player.sendOverlayMessage(Component.literal(loadCassette(server, player, hand)));
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
		builder.define(DATA_JUNCTION, java.util.Optional.empty());
		builder.define(DATA_PAID, 0F);
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
		entityData.set(DATA_FACING, input.getIntOr("facing", Direction.NORTH.get3DDataValue()));
		entityData.set(DATA_PITCH, input.getFloatOr("pitch", 0F));
		coupled = input.getBooleanOr("coupled", false);
		water = input.getIntOr("water", 0);
		wheels.load(input.getStringOr("wheels", ""));
		if (!wheels.railed() && input.getStringOr("wheels", "").isEmpty()) {
			placedBeforeWheels = true;   // a world saved before the gantries had wheels: they go on the rails under it
		}
	}

	private boolean placedBeforeWheels;

	@Override
	protected void addAdditionalSaveData(ValueOutput output) {
		output.putInt("facing", entityData.get(DATA_FACING));
		output.putFloat("pitch", entityData.get(DATA_PITCH));
		output.putBoolean("coupled", coupled);
		output.putInt("water", water);
		output.putString("wheels", wheels.save());
	}
}
