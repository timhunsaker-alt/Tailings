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
 * In compartment 2 it is the shaft's cage (heavy-iron-fixes): 2 x 3 x 2, for up to 4 people, who ride it, and 9 stacks of materials on
 * its deck; the hoist winds it between the collar and the bottom landing at the man-riding 4 m/s when someone boards it or calls it.
 *
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
	/** It is the cage (compartment 2), for the client to draw it so. */
	private static final EntityDataAccessor<Boolean> DATA_CAGE = SynchedEntityData.defineId(ShaftSkipEntity.class, EntityDataSerializers.BOOLEAN);

	/** The headframe's main cell it hangs in, and which compartment (0 west, 1 east, in the headframe's own frame). */
	private BlockPos headframe = BlockPos.ZERO;
	private int compartment;
	/** The cage: its deck's materials, a landing it was called to (-1 none, 0 the bottom, 1 the collar), and a rider's bell. */
	private final java.util.List<ItemStack> deck = new java.util.ArrayList<>();
	private int call = -1;
	private boolean bell;

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

	/** The shaft's cage, not a skip. */
	public boolean cage() {
		return compartment == ShaftLayout.CAGE || entityData != null && entityData.get(DATA_CAGE);
	}

	public List<ItemStack> deck() {
		return deck;
	}

	/** Calls the cage to the collar (true) or the bottom landing. */
	public void call(boolean top) {
		call = top ? 1 : 0;
	}

	/** The landing it was called to since the hoist last looked (-1 none, 0 the bottom, 1 the collar); cleared. */
	public int takeCall() {
		int c = call;
		call = -1;
		return c;
	}

	/** Whether a rider rang the bell to go since the hoist last looked; cleared. */
	public boolean takeBell() {
		boolean b = bell;
		bell = false;
		return b;
	}

	/** The cage on the move, as the hoist sets it: its doors closed. */
	private boolean moving;
	private Vec3 lastAt = Vec3.ZERO;

	@Override
	public void tick() {
		super.tick();
		if (!cage() || !(level() instanceof ServerLevel server)) {
			return;
		}
		moving = position().distanceToSqr(lastAt) > 1e-6;
		lastAt = position();
		if (moving) {
			// its gate is shut while it moves: a person in it rides on
			for (Player p : server.getEntitiesOfClass(Player.class, getBoundingBox(), p -> !p.isPassenger() && !p.isSpectator())) {
				p.startRiding(this);
			}
		}
	}

	@Override
	protected boolean canAddPassenger(Entity passenger) {
		return cage() && getPassengers().size() < dev.wildcatter.tailings.shaft.Hoisting.CAGE_PEOPLE;
	}

	/** Its riders stand at the four quarters of its floor. */
	@Override
	protected void positionRider(Entity passenger, Entity.MoveFunction move) {
		if (!hasPassenger(passenger)) {
			return;
		}
		int i = getPassengers().indexOf(passenger);
		AABB box = getBoundingBox();
		double x = box.minX + (i % 2 == 0 ? 0.55 : 1.45), z = box.minZ + (i / 2 == 0 ? 0.55 : 1.45);
		move.accept(passenger, x, box.minY + 0.1, z);
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
		double high = cage() ? dev.wildcatter.tailings.shaft.Hoisting.CAGE_HEIGHT : dev.wildcatter.tailings.block.SkipPartBlock.HEIGHT;
		return new AABB(at.x + Math.min(0, far.getX()), at.y, at.z + Math.min(0, far.getZ()),
				at.x + Math.max(0, far.getX()) + 1, at.y + high, at.z + Math.max(0, far.getZ()) + 1);
	}

	@Override
	public void onSyncedDataUpdated(EntityDataAccessor<?> accessor) {
		super.onSyncedDataUpdated(accessor);
		if (DATA_FACING.equals(accessor) || DATA_CAGE.equals(accessor)) {
			setBoundingBox(makeBoundingBox(position()));
		}
	}

	@Override
	public InteractionResult interact(Player player, InteractionHand hand, Vec3 location) {
		if (cage()) {
			if (!level().isClientSide()) {
				useCage(player, hand);
			}
			return InteractionResult.SUCCESS;
		}
		if (!level().isClientSide()) {
			player.sendOverlayMessage(Component.literal("Skip (3 t), " + (compartment == 0 ? "west" : "east") + " compartment: " + (loaded() ? "loaded" : "empty")));
		}
		return InteractionResult.SUCCESS;
	}

	/**
	 * The cage: an empty hand boards it and rings the bell to go to the other landing; with something in hand, sneaking loads that stack
	 * on its deck; sneaking with an empty hand unloads the deck to you.
	 */
	private void useCage(Player player, InteractionHand hand) {
		ItemStack held = player.getItemInHand(hand);
		if (player.isShiftKeyDown()) {
			if (held.isEmpty()) {
				int n = 0;
				for (ItemStack s : deck) {
					n += s.getCount();
					if (!player.getInventory().add(s.copy())) {
						player.spawnAtLocation((ServerLevel) level(), s.copy());
					}
				}
				deck.clear();
				setLoaded(false);
				player.sendOverlayMessage(Component.literal("Cage: unloaded " + n + " items from its deck"));
			} else if (deck.size() < dev.wildcatter.tailings.shaft.Hoisting.CAGE_STACKS) {
				deck.add(held.copy());
				held.setCount(0);
				setLoaded(true);
				player.sendOverlayMessage(Component.literal("Cage: " + deck.size() + " of " + dev.wildcatter.tailings.shaft.Hoisting.CAGE_STACKS + " stacks on its deck"));
			} else {
				player.sendOverlayMessage(Component.literal("Cage: its deck is full"));
			}
			return;
		}
		if (!hasPassenger(player) && player.startRiding(this)) {
			bell = true;
			player.sendOverlayMessage(Component.literal("Cage: boarded; the bell rings and it goes in "
					+ (int) dev.wildcatter.tailings.shaft.Hoisting.CAGE_BELL_SECONDS + " s (sneak at a landing to step off)"));
		} else if (!hasPassenger(player)) {
			player.sendOverlayMessage(Component.literal("Cage: full (" + dev.wildcatter.tailings.shaft.Hoisting.CAGE_PEOPLE + " people)"));
		}
	}

	/** Hangs the cage in its compartment beside the skips, its floor at the collar. Null when one hangs there already. */
	public static @Nullable ShaftSkipEntity hangCage(ServerLevel level, ShaftLayout.Frame frame) {
		if (near(level, frame).stream().anyMatch(s -> s.hangsIn(frame.main(), ShaftLayout.CAGE))) {
			return null;
		}
		ShaftSkipEntity cage = new ShaftSkipEntity(TailingsEntities.SKIP, level);
		cage.headframe = frame.main();
		cage.compartment = ShaftLayout.CAGE;
		cage.entityData.set(DATA_CAGE, true);
		cage.setAt(frame.skipCorner(ShaftLayout.CAGE, frame.collarY()), frame.facing());
		level.addFreshEntity(cage);
		return cage;
	}

	/** The cage in this headframe, or null. */
	public static @Nullable ShaftSkipEntity cageIn(ServerLevel level, ShaftLayout.Frame frame) {
		return near(level, frame).stream().filter(ShaftSkipEntity::cage).findFirst().orElse(null);
	}

	@Override
	public boolean isPickable() {
		return !isRemoved();
	}

	/** Only a sneaking hit with an empty hand takes it off its rope, into its item; the hoist then reads NOT BUILT. */
	@Override
	public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
		if (source.getEntity() instanceof Player player && player.isShiftKeyDown() && player.getMainHandItem().isEmpty()) {
			ejectPassengers();
			for (ItemStack s : deck) {
				spawnAtLocation(level, s);
			}
			deck.clear();
			spawnAtLocation(level, new ItemStack(cage() ? TailingsItems.CAGE : TailingsItems.SKIP));
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
		builder.define(DATA_CAGE, false);
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
		entityData.set(DATA_FACING, input.getIntOr("facing", Direction.NORTH.get3DDataValue()));
		headframe = BlockPos.of(input.getLongOr("headframe", 0L));
		compartment = input.getIntOr("compartment", 0);
		entityData.set(DATA_CAGE, compartment == ShaftLayout.CAGE);
		entityData.set(DATA_LOADED, input.getBooleanOr("loaded", false));
		deck.clear();
		input.read("deck", ItemStack.CODEC.listOf()).ifPresent(deck::addAll);
	}

	@Override
	protected void addAdditionalSaveData(ValueOutput output) {
		output.putInt("facing", entityData.get(DATA_FACING));
		output.putLong("headframe", headframe.asLong());
		output.putInt("compartment", compartment);
		output.putBoolean("loaded", loaded());
		if (!deck.isEmpty()) {
			output.store("deck", ItemStack.CODEC.listOf(), List.copyOf(deck));
		}
	}
}
