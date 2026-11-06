package dev.wildcatter.tailings.entity;

import dev.wildcatter.multiblock.BigPartEntity;
import dev.wildcatter.tailings.TailingsBlocks;
import dev.wildcatter.tailings.TailingsEntities;
import dev.wildcatter.tailings.block.CableJunctionBlockEntity;
import dev.wildcatter.tailings.block.ChunkRunner;
import dev.wildcatter.tailings.block.DrillBlockEntity;
import dev.wildcatter.tailings.block.TbmCab;
import dev.wildcatter.tailings.block.TbmCore;
import dev.wildcatter.tailings.block.TbmMachine;
import dev.wildcatter.tailings.block.WorldGround;
import dev.wildcatter.tailings.mine.DriveCourse;
import dev.wildcatter.tailings.mine.DrillCodes;
import dev.wildcatter.tailings.mine.Heading;
import dev.wildcatter.tailings.mine.TbmPart;
import dev.wildcatter.tailings.mine.TunnelDrill;
import dev.wildcatter.tailings.platform.Platform;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * The tunnel boring machine as a contraption (docs/multiblocks/tunnel-boring-machine.md): while it runs its parts' cells are air, and
 * this one entity carries the cab and draws them. It cuts the face, glides into the cut over the advance time, and on arrival lines
 * and floors behind itself. On the decline it follows the grade, pitched by the floor's height under its front and rear. Stopped on
 * the flat it sets its parts back down as blocks; on the decline it parks as it is.
 */
public class TbmEntity extends Entity implements TbmCab {
	private static final EntityDataAccessor<Integer> DATA_FACING = SynchedEntityData.defineId(TbmEntity.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Float> DATA_PITCH = SynchedEntityData.defineId(TbmEntity.class, EntityDataSerializers.FLOAT);
	/** The machine from the cutterhead to the pump's tail. */
	private static final int LENGTH = 13;
	/** The rear of the rigid machine, for its pitch: the track layer's tail. */
	private static final int PITCH_BASE = 11;
	/** The seat, on the cab's roof at the main beam's tail: from the cutterhead's main cell along the machine, and up. */
	private static final double SEAT_BACK = 10;
	private static final double SEAT_UP = 5;

	private final TbmCore core = new TbmCore();
	/** The cutterhead's main cell from the bore's origin; both move together. */
	private int dx;
	private int dz;
	/** How long the glide into the current cut takes. */
	private int glide;
	private int refresh;

	public TbmEntity(EntityType<?> type, Level level) {
		super(type, level);
		setNoGravity(true);
		noPhysics = true;
	}

	/** Lifts a whole, fed machine standing as blocks: its cells become air, and this carries on from its cab. */
	public static TbmEntity lift(ServerLevel level, DrillBlockEntity cab, BlockPos cutter, Direction facing) {
		TbmEntity machine = new TbmEntity(TailingsEntities.TBM, level);
		TagValueOutput output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, level.registryAccess());
		cab.core().save(output);
		machine.core.load(TagValueInput.create(ProblemReporter.DISCARDING, level.registryAccess(), output.buildResult()));
		machine.entityData.set(DATA_FACING, facing.get3DDataValue());
		TunnelDrill drill = machine.core.drill();
		machine.dx = cutter.getX() - drill.originX();
		machine.dz = cutter.getZ() - drill.originZ();
		cab.releaseForLift();
		int quiet = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE | Block.UPDATE_SUPPRESS_DROPS;
		for (TbmMachine.Cell cell : TbmMachine.cells(facing)) {
			level.setBlock(cutter.offset(cell.x(), cell.y(), cell.z()), net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), quiet);
		}
		machine.place();
		level.addFreshEntity(machine);
		return machine;
	}

	public Direction facing() {
		return Direction.from3DDataValue(entityData.get(DATA_FACING));
	}

	/** Degrees the machine is pitched, its rear up. */
	public float pitch() {
		return entityData.get(DATA_PITCH);
	}

	@Override
	public TbmCore core() {
		return core;
	}

	@Override
	public BlockPos menuPos() {
		return blockPosition();
	}

	@Override
	public void setChanged() {
	}

	@Override
	public boolean stillValid(Player player) {
		return isAlive() && player.distanceToSqr(this) < 40 * 40;
	}

	/** Go or stop; the Ride button seats the player in the cab. */
	@Override
	public void press(int button, Player player) {
		if (button == TbmCore.BUTTON_RIDE) {
			if (player instanceof net.minecraft.server.level.ServerPlayer server) {
				server.closeContainer();
			}
			player.startRiding(this);
			return;
		}
		core.press(button, level());
	}

	// ---- where it is ---------------------------------------------------------------------------------------------------------------

	/** The slice under the cutterhead, between whole slices while it glides. */
	private double slice() {
		double progress = glide <= 0 ? 1 : 1 - (double) core.waitTicks() / glide;
		return core.drill().advanceCount() - 2 + progress;
	}

	/** The cutterhead's main cell once it has arrived. */
	private BlockPos arrived() {
		TunnelDrill drill = core.drill();
		return new BlockPos(drill.originX() + dx, drill.floorY(drill.advanceCount() - 1) - 1, drill.originZ() + dz);
	}

	private void place() {
		TunnelDrill drill = core.drill();
		double slice = slice();
		Heading heading = core.program().heading();
		double back = drill.advanceCount() - 1 - slice;
		double x = drill.originX() + dx - heading.forwardX() * back;
		double z = drill.originZ() + dz - heading.forwardZ() * back;
		setPos(x, drill.floorExact(slice) - 1, z);
		double rise = drill.floorExact(slice - PITCH_BASE) - drill.floorExact(slice);
		entityData.set(DATA_PITCH, (float) Math.toDegrees(Math.atan2(rise, PITCH_BASE)));
		setBoundingBox(makeBoundingBox(position()));
	}

	/** The whole machine's outline: 8 x 8 by its length, its rear up when pitched. */
	@Override
	protected AABB makeBoundingBox(Vec3 at) {
		if (entityData == null) {
			return super.makeBoundingBox(at);
		}
		BlockPos far = TbmMachine.offset(facing(), 7, 0, LENGTH - 1);
		double rise = LENGTH * Math.sin(Math.toRadians(pitch()));
		return new AABB(at.x + Math.min(0, far.getX()), at.y, at.z + Math.min(0, far.getZ()),
				at.x + Math.max(0, far.getX()) + 1, at.y + 8 + rise, at.z + Math.max(0, far.getZ()) + 1);
	}

	@Override
	public void onSyncedDataUpdated(EntityDataAccessor<?> accessor) {
		super.onSyncedDataUpdated(accessor);
		if (DATA_FACING.equals(accessor) || DATA_PITCH.equals(accessor)) {
			setBoundingBox(makeBoundingBox(position()));
		}
	}

	// ---- running -------------------------------------------------------------------------------------------------------------------

	private final TbmCore.Body body = new TbmCore.Body() {
		@Override
		public boolean powered() {
			return core.forced() || core.junction() != null && level().getBlockEntity(core.junction()) instanceof CableJunctionBlockEntity junction && junction.live();
		}

		/** Nothing but air in the bore the beam and the track layer move through next. */
		@Override
		public boolean clearAhead() {
			TunnelDrill drill = core.drill();
			for (int ahead = -3; ahead >= -10; ahead--) {
				for (int up = 2; up <= 6; up++) {
					for (int across = 1; across <= DriveCourse.CLEAR; across++) {
						BlockState there = level().getBlockState(new BlockPos(drill.worldX(across, ahead), drill.worldY(up, ahead), drill.worldZ(across, ahead)));
						if (!there.isAir() && !there.canBeReplaced()) {
							return false;
						}
					}
				}
			}
			return true;
		}
	};

	@Override
	public void tick() {
		super.tick();
		if (!(level() instanceof ServerLevel server)) {
			return;
		}
		core.acceptBuckets();
		core.tickLoading(server);
		if (core.running() && core.junction() != null && server.getBlockEntity(core.junction()) instanceof CableJunctionBlockEntity junction) {
			junction.draw(TbmCore.BEAM_WATTS);
		}
		lockChunks(server);
		if (core.waitTicks() > 0) {
			core.countDown();
			place();
			return;
		}
		place();
		WorldGround ground = new WorldGround(server, core.program().heading());
		core.drill().line(ground);
		if (!core.running()) {
			if (server.getGameTime() % 20 == 0) {
				setDown(server);
			}
			return;
		}
		core.advance(server, body);
		glide = core.waitTicks();
	}

	private void lockChunks(ServerLevel server) {
		if (refresh-- > 0 || !core.running() && core.waitTicks() == 0) {
			return;
		}
		refresh = ChunkRunner.REFRESH_TICKS;
		TunnelDrill drill = core.drill();
		ChunkRunner.refresh(server, blockPosition());
		ChunkRunner.refresh(server, BlockPos.containing(drill.worldX(0, -DriveCourse.FLOOR_BEHIND), drill.worldY(0, -DriveCourse.FLOOR_BEHIND), drill.worldZ(0, -DriveCourse.FLOOR_BEHIND)));
		ChunkRunner.refresh(server, BlockPos.containing(drill.worldX(0, DriveCourse.PROBE_BLOCKS), drill.worldY(0, DriveCourse.PROBE_BLOCKS), drill.worldZ(0, DriveCourse.PROBE_BLOCKS)));
		if (core.junction() != null) {
			ChunkRunner.refresh(server, core.junction());
		}
	}

	/** True when the floor under the whole machine is level: only then can it stand as blocks. */
	public boolean onTheFlat() {
		TunnelDrill drill = core.drill();
		int under = drill.advanceCount() - 1;
		return drill.floorExact(under) == drill.floorExact(under - LENGTH - 1);
	}

	/** Sets its parts back down as blocks where it has stopped, if it is on the flat and their cells are free. */
	public boolean setDown(ServerLevel server) {
		if (!onTheFlat()) {
			if (core.note().isEmpty()) {
				core.report(core.status(), "parked on the decline; the cab still works");
			}
			return false;
		}
		Direction facing = facing();
		BlockPos cutter = arrived();
		List<TbmMachine.Cell> cells = TbmMachine.cells(facing);
		for (TbmMachine.Cell cell : cells) {
			BlockPos at = cutter.offset(cell.x(), cell.y(), cell.z());
			BlockState there = server.getBlockState(at);
			if (!there.isAir() && !there.canBeReplaced()) {
				core.report(core.status(), "can't set down: blocked at " + at.getX() + ", " + at.getY() + ", " + at.getZ());
				return false;
			}
		}
		ejectPassengers();
		int quiet = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE | Block.UPDATE_SUPPRESS_DROPS;
		for (TbmMachine.Cell cell : cells) {
			BlockPos at = cutter.offset(cell.x(), cell.y(), cell.z());
			server.setBlock(at, cell.state(), quiet);
			if (server.getBlockEntity(at) instanceof BigPartEntity part) {
				part.markGrown();
			}
		}
		if (server.getBlockEntity(TbmMachine.main(TbmPart.MAIN_BEAM, cutter, facing)) instanceof DrillBlockEntity cab) {
			TagValueOutput output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, server.registryAccess());
			core.save(output);
			cab.core().load(TagValueInput.create(ProblemReporter.DISCARDING, server.registryAccess(), output.buildResult()));
			cab.setChanged();
		}
		core.slots().clear();
		discard();
		return true;
	}

	/** Every part drops as its item and every slot's contents drop. */
	public void dismantle(ServerLevel server) {
		ejectPassengers();
		List<ItemStack> drops = new ArrayList<>(core.slots());
		for (TbmPart part : TbmPart.values()) {
			drops.add(new ItemStack(TailingsBlocks.tbmBlock(part)));
		}
		drops.add(new ItemStack(TailingsBlocks.DEWATERING_PUMP));
		for (ItemStack stack : drops) {
			if (!stack.isEmpty()) {
				Containers.dropItemStack(server, getX(), getY() + 1, getZ(), stack);
			}
		}
		core.slots().clear();
		discard();
	}

	// ---- hands ---------------------------------------------------------------------------------------------------------------------

	/** A click opens the cab's screen; a sneaking click with an empty hand, while stopped, dismantles it. */
	@Override
	public InteractionResult interact(Player player, InteractionHand hand, Vec3 location) {
		if (!(level() instanceof ServerLevel server)) {
			return InteractionResult.SUCCESS;
		}
		if (player.isShiftKeyDown() && player.getItemInHand(hand).isEmpty()) {
			if (core.running()) {
				core.report(core.status(), "stop it before dismantling it");
			} else {
				dismantle(server);
			}
			return InteractionResult.SUCCESS;
		}
		Platform.INSTANCE.openDrill(player, this);
		return InteractionResult.SUCCESS;
	}

	@Override
	public boolean isPickable() {
		return !isRemoved();
	}

	@Override
	public boolean canBeCollidedWith(Entity other) {
		return true;
	}

	@Override
	public boolean isPushable() {
		return false;
	}

	/** The seat, on the cab's roof at the main beam's tail. */
	@Override
	protected void positionRider(Entity passenger, MoveFunction move) {
		Direction facing = facing();
		Direction across = facing.getClockWise();
		double angle = Math.toRadians(pitch());
		double back = SEAT_BACK * Math.cos(angle) - SEAT_UP * Math.sin(angle);
		double up = SEAT_UP * Math.cos(angle) + SEAT_BACK * Math.sin(angle);
		move.accept(passenger, getX() + 0.5 + across.getStepX() * 3.5 - facing.getStepX() * back, getY() + up,
				getZ() + 0.5 + across.getStepZ() * 3.5 - facing.getStepZ() * back);
	}

	@Override
	public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
		return false;
	}

	// ---- saving --------------------------------------------------------------------------------------------------------------------

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		builder.define(DATA_FACING, Direction.NORTH.get3DDataValue());
		builder.define(DATA_PITCH, 0F);
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
		core.load(input.childOrEmpty("cab"));
		dx = input.getIntOr("dx", 0);
		dz = input.getIntOr("dz", 0);
		glide = input.getIntOr("glide", 0);
		entityData.set(DATA_FACING, input.getIntOr("facing", Direction.NORTH.get3DDataValue()));
	}

	@Override
	protected void addAdditionalSaveData(ValueOutput output) {
		core.save(output.child("cab"));
		output.putInt("dx", dx);
		output.putInt("dz", dz);
		output.putInt("glide", glide);
		output.putInt("facing", entityData.get(DATA_FACING));
	}

	/** Status words the cab reads, for tests. */
	public boolean driving() {
		return core.status() == DrillCodes.DRIVING;
	}
}
