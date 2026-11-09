package dev.wildcatter.tailings.entity;

import dev.wildcatter.tailings.TailingsBlocks;
import dev.wildcatter.tailings.TailingsItems;
import dev.wildcatter.tailings.block.MuckBlockEntity;
import dev.wildcatter.tailings.pit.PitDrive;
import dev.wildcatter.tailings.pit.PitLoad;
import dev.wildcatter.tailings.pit.PitMass;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * The wheel loader (docs/multiblocks/open-pit.md, Loaders): a 12 m³ bucket of 20.4 t. Jump with the bucket empty digs a pass from
 * the face in front, soft ground block by block or muck, up to 6 m high; standing hard rock it cannot dig ("hard rock: blast it
 * first"). Jump with the bucket full dumps it into a haul truck within reach of the bucket. A pass is 25 s digging and 15 s dumping,
 * its 40 s cycle (proposed split).
 */
public class WheelLoaderEntity extends PitVehicleEntity {
	public static final int DIG_TICKS = 500;
	public static final int DUMP_TICKS = 300;
	/** The face it digs: up to 6 m high from its own floor, the bucket's 5 m wide, 3 m in from the bucket's lip. */
	public static final int REACH_UP = 6;
	public static final int REACH_ACROSS = 2;
	public static final int REACH_IN = 3;

	private int passLeft;
	private boolean dumping;
	/** A game test's shorter passes. */
	private int passDivisor = 1;

	public WheelLoaderEntity(EntityType<?> type, Level level) {
		super(type, level);
	}

	@Override
	public PitDrive.Spec spec() {
		return PitDrive.LOADER;
	}

	@Override
	protected Item item() {
		return TailingsItems.WHEEL_LOADER;
	}

	@Override
	public boolean busy() {
		return passLeft > 0;
	}

	@Override
	protected Vec3 seat() {
		return new Vec3(0, 3.2, -1.0);
	}

	/** The bucket. */
	public PitLoad bucket() {
		return load;
	}

	public void fastPasses(int divisor) {
		this.passDivisor = Math.max(1, divisor);
	}

	private void say(@Nullable Player driver, String what) {
		status = what;
		if (driver != null) {
			driver.sendOverlayMessage(Component.literal("Wheel Loader: " + what));
		}
	}

	@Override
	protected void jump(ServerLevel level, @Nullable Player driver) {
		if (passLeft > 0) {
			return;
		}
		if (load.empty()) {
			dig(level, driver);
		} else {
			HaulTruckEntity truck = truckInReach(level);
			if (truck == null) {
				say(driver, "no haul truck under the bucket");
				return;
			}
			if (truck.load().room() <= 0) {
				say(driver, "the truck is full");
				return;
			}
			dumping = true;
			passLeft = DUMP_TICKS / passDivisor;
			say(driver, "dumping");
		}
	}

	/** One pass: fills the bucket from the face in front. Public for tests. */
	public void dig(ServerLevel level, @Nullable Player driver) {
		boolean hard = false;
		Vec3 lip = local(0, 0, spec().length() / 2);
		BlockPos base = BlockPos.containing(lip.x, getY() + 0.5, lip.z);
		Vec3 in = ahead();
		Vec3 across = right();
		outer:
		for (int up = REACH_UP - 1; up >= 0; up--) {
			for (int depth = 0; depth < REACH_IN; depth++) {
				for (int side = -REACH_ACROSS; side <= REACH_ACROSS; side++) {
					BlockPos pos = BlockPos.containing(lip.x + in.x * (depth + 0.5) + across.x * side, base.getY() + up,
							lip.z + in.z * (depth + 0.5) + across.z * side);
					BlockState state = level.getBlockState(pos);
					if (state.isAir() || !state.getFluidState().isEmpty()) {
						continue;
					}
					if (state.getBlock() == TailingsBlocks.MUCK && level.getBlockEntity(pos) instanceof MuckBlockEntity muck) {
						long kg = 0;
						for (long v : muck.kilograms().values()) {
							kg += v;
						}
						if (!load.empty() && kg > load.room()) {
							break outer;
						}
						for (Map.Entry<String, Long> e : muck.kilograms().entrySet()) {
							load.add(e.getKey(), e.getValue());
						}
						level.removeBlock(pos, false);
						continue;
					}
					String id = PitMass.dugAs(BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString());
					if (PitMass.soft(id)) {
						if (!load.empty() && PitMass.kg(id) > load.room()) {
							break outer;
						}
						load.add(id, PitMass.kg(id));
						level.removeBlock(pos, false);
					} else if (state.canBeReplaced()) {
						level.destroyBlock(pos, false);
					} else {
						hard = true;
					}
				}
			}
		}
		refreshLoad();
		if (load.empty()) {
			say(driver, hard ? "hard rock: blast it first" : "nothing in reach of the bucket");
			return;
		}
		dumping = false;
		passLeft = DIG_TICKS / passDivisor;
		say(driver, "digging: " + load.tonnes());
		level.playSound(null, lip.x, lip.y, lip.z, dev.wildcatter.tailings.TailingsSounds.MUCK_LOAD, net.minecraft.sounds.SoundSource.BLOCKS, 2F, 0.9F);
	}

	/** The nearest haul truck whose body is within 4 m of the bucket's lip. */
	public @Nullable HaulTruckEntity truckInReach(Level level) {
		Vec3 lip = local(0, 3, spec().length() / 2);
		HaulTruckEntity best = null;
		double bestDistance = Double.MAX_VALUE;
		for (HaulTruckEntity truck : level.getEntitiesOfClass(HaulTruckEntity.class, new AABB(lip, lip).inflate(12))) {
			if (truck.getBoundingBox().inflate(4).contains(lip)) {
				double d = truck.distanceToSqr(lip);
				if (d < bestDistance) {
					bestDistance = d;
					best = truck;
				}
			}
		}
		return best;
	}

	@Override
	protected void work(ServerLevel level) {
		if (passLeft <= 0) {
			return;
		}
		passLeft--;
		int length = (dumping ? DUMP_TICKS : DIG_TICKS) / passDivisor;
		float done = 1 - passLeft / (float) Math.max(1, length);
		setLift(dumping ? 1 - done : done);
		if (passLeft == 0 && dumping) {
			Player driver = getFirstPassenger() instanceof Player p ? p : null;
			HaulTruckEntity truck = truckInReach(level);
			if (truck == null) {
				say(driver, "the truck moved away");
				setLift(1);
				return;
			}
			long before = truck.load().total();
			load.pourInto(truck.load());
			truck.refreshLoad();
			refreshLoad();
			setLift(0);
			say(driver, String.format(java.util.Locale.ROOT, "loaded %.1f t; the truck holds %s", (truck.load().total() - before) / 1000.0, truck.load().tonnes()));
			level.playSound(null, truck.getX(), truck.getY(), truck.getZ(), dev.wildcatter.tailings.TailingsSounds.MUCK_TIP, net.minecraft.sounds.SoundSource.BLOCKS, 2F, 1.1F);
		}
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
		super.readAdditionalSaveData(input);
		passLeft = input.getIntOr("pass_left", 0);
		dumping = input.getBooleanOr("dumping", false);
	}

	@Override
	protected void addAdditionalSaveData(ValueOutput output) {
		super.addAdditionalSaveData(output);
		output.putInt("pass_left", passLeft);
		output.putBoolean("dumping", dumping);
	}
}
