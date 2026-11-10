package dev.wildcatter.tailings.entity;

import dev.wildcatter.tailings.TailingsItems;
import dev.wildcatter.tailings.pit.PitDrive;
import dev.wildcatter.tailings.pit.PitFuel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * The 360 t ultra-class mechanical-drive truck (docs/multiblocks/open-pit-p5-big-machines.md, P5b): 363 t in its body, 15.08 m long,
 * 9.76 m wide and 7.71 m to its canopy, a 2,828 kW net engine. It is the 100 t truck's class with these figures: it drives, tips,
 * takes routes, burns diesel and answers its dispatch office the same way. Its body comes up in 20 s (proposed), it needs about 40 m
 * to turn round, its berms are 2 blocks (mid-axle on its 4 m tyres), and its 7.0 m loading height is over the wheel loader's reach.
 */
public class UltraHaulTruckEntity extends HaulTruckEntity {
	/** The body up, or down, in 20 s (proposed; the 100 t is 13 s). */
	public static final int ULTRA_RAISE_TICKS = 400;
	/** Its tyres, about 4.0 m across (unverified). */
	public static final double WHEEL_RADIUS = 2.0;
	/** Its loading height, empty: 7.0 m (spec sheet). */
	public static final double LOADING_HEIGHT = 7.0;

	public UltraHaulTruckEntity(EntityType<?> type, Level level) {
		super(type, level);
	}

	@Override
	public PitDrive.Spec spec() {
		return PitDrive.ULTRA_TRUCK;
	}

	@Override
	protected Item item() {
		return TailingsItems.ULTRA_HAUL_TRUCK;
	}

	@Override
	public double tank() {
		return PitFuel.ULTRA_TANK_L;
	}

	@Override
	protected int raiseTicks() {
		return ULTRA_RAISE_TICKS;
	}

	@Override
	public double wheelRadius() {
		return WHEEL_RADIUS;
	}

	@Override
	public double loadingHeight() {
		return LOADING_HEIGHT;
	}

	@Override
	protected Vec3 seat() {
		return new Vec3(-3.3, 5.0, 5.6);   // the cab on the deck over the left front wheel, reached by its stairway
	}

	@Override
	public Vec3 bodyCentre() {
		return local(0, 0, -1.9);   // its body runs from behind the cab to the tail
	}
}
