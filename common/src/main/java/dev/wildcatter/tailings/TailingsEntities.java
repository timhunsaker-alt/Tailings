package dev.wildcatter.tailings;

import dev.wildcatter.tailings.entity.MineTrainEntity;
import dev.wildcatter.tailings.entity.MineVehicleEntity;
import dev.wildcatter.tailings.entity.GantryEntity;
import dev.wildcatter.tailings.entity.TbmEntity;
import dev.wildcatter.tailings.mine.TbmGantry;
import dev.wildcatter.tailings.entity.TunnelDrillEntity;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;

/** Contraption entities. The drill is registered with the blocks, before any world loads. */
public final class TailingsEntities {
	public static final EntityType<TunnelDrillEntity> DRILL = EntityType.Builder.<TunnelDrillEntity>of(TunnelDrillEntity::new, MobCategory.MISC)
			.sized(0.98F, 1.98F)
			.clientTrackingRange(10)
			.build(ResourceKey.create(Registries.ENTITY_TYPE, Tailings.id("tunnel_drill")));
	/** The block-stepped train before R5; one left in an old world discards itself. */
	public static final EntityType<MineTrainEntity> TRAIN = EntityType.Builder.<MineTrainEntity>of(MineTrainEntity::new, MobCategory.MISC)
			.sized(0.9F, 1.4F)
			.clientTrackingRange(10)
			.build(ResourceKey.create(Registries.ENTITY_TYPE, Tailings.id("mine_train")));
	public static final EntityType<MineVehicleEntity> LOCOMOTIVE = EntityType.Builder.<MineVehicleEntity>of(MineVehicleEntity::new, MobCategory.MISC)
			.sized(0.9F, 1.4F)
			.clientTrackingRange(10)
			.updateInterval(1)
			.build(ResourceKey.create(Registries.ENTITY_TYPE, Tailings.id("mine_locomotive")));
	public static final EntityType<MineVehicleEntity> CAR = EntityType.Builder.<MineVehicleEntity>of(MineVehicleEntity::new, MobCategory.MISC)
			.sized(0.9F, 1.2F)
			.clientTrackingRange(10)
			.updateInterval(1)
			.build(ResourceKey.create(Registries.ENTITY_TYPE, Tailings.id("mine_car")));

	/** The tunnel boring machine as a contraption: its bounding box is its own (TbmEntity), so these sizes do not matter. */
	public static final EntityType<TbmEntity> TBM = EntityType.Builder.<TbmEntity>of(TbmEntity::new, MobCategory.MISC)
			.sized(8F, 8F)
			.clientTrackingRange(16)
			.updateInterval(1)
			.build(ResourceKey.create(Registries.ENTITY_TYPE, Tailings.id("tunnel_boring_machine")));

	/** The open pit's machines at their real size; their boxes are their own (PitVehicleEntity). */
	public static final EntityType<dev.wildcatter.tailings.entity.HaulTruckEntity> HAUL_TRUCK = EntityType.Builder
			.<dev.wildcatter.tailings.entity.HaulTruckEntity>of(dev.wildcatter.tailings.entity.HaulTruckEntity::new, MobCategory.MISC)
			.sized(6.2F, 5.2F)
			.clientTrackingRange(16)
			.updateInterval(1)
			.build(ResourceKey.create(Registries.ENTITY_TYPE, Tailings.id("haul_truck")));
	public static final EntityType<dev.wildcatter.tailings.entity.WheelLoaderEntity> WHEEL_LOADER = EntityType.Builder
			.<dev.wildcatter.tailings.entity.WheelLoaderEntity>of(dev.wildcatter.tailings.entity.WheelLoaderEntity::new, MobCategory.MISC)
			.sized(4.5F, 6.0F)
			.clientTrackingRange(16)
			.updateInterval(1)
			.build(ResourceKey.create(Registries.ENTITY_TYPE, Tailings.id("wheel_loader")));

	public static final EntityType<dev.wildcatter.tailings.entity.BlastholeDrillEntity> BLASTHOLE_DRILL = EntityType.Builder
			.<dev.wildcatter.tailings.entity.BlastholeDrillEntity>of(dev.wildcatter.tailings.entity.BlastholeDrillEntity::new, MobCategory.MISC)
			.sized(7.0F, 26.0F)
			.clientTrackingRange(16)
			.updateInterval(1)
			.build(ResourceKey.create(Registries.ENTITY_TYPE, Tailings.id("blasthole_drill")));
	public static final EntityType<dev.wildcatter.tailings.entity.EmulsionTruckEntity> EMULSION_TRUCK = EntityType.Builder
			.<dev.wildcatter.tailings.entity.EmulsionTruckEntity>of(dev.wildcatter.tailings.entity.EmulsionTruckEntity::new, MobCategory.MISC)
			.sized(2.5F, 3.8F)
			.clientTrackingRange(16)
			.updateInterval(1)
			.build(ResourceKey.create(Registries.ENTITY_TYPE, Tailings.id("emulsion_truck")));
	public static final EntityType<dev.wildcatter.tailings.entity.UltraHaulTruckEntity> ULTRA_HAUL_TRUCK = EntityType.Builder
			.<dev.wildcatter.tailings.entity.UltraHaulTruckEntity>of(dev.wildcatter.tailings.entity.UltraHaulTruckEntity::new, MobCategory.MISC)
			.sized(9.8F, 7.7F)
			.clientTrackingRange(16)
			.updateInterval(1)
			.build(ResourceKey.create(Registries.ENTITY_TYPE, Tailings.id("ultra_haul_truck")));
	public static final EntityType<dev.wildcatter.tailings.entity.HydraulicShovelEntity> HYDRAULIC_SHOVEL = EntityType.Builder
			.<dev.wildcatter.tailings.entity.HydraulicShovelEntity>of(dev.wildcatter.tailings.entity.HydraulicShovelEntity::new, MobCategory.MISC)
			.sized(8.0F, 7.3F)
			.clientTrackingRange(16)
			.updateInterval(1)
			.build(ResourceKey.create(Registries.ENTITY_TYPE, Tailings.id("hydraulic_shovel")));
	public static final EntityType<dev.wildcatter.tailings.entity.TrackDozerEntity> TRACK_DOZER = EntityType.Builder
			.<dev.wildcatter.tailings.entity.TrackDozerEntity>of(dev.wildcatter.tailings.entity.TrackDozerEntity::new, MobCategory.MISC)
			.sized(4.9F, 4.6F)
			.clientTrackingRange(16)
			.updateInterval(1)
			.build(ResourceKey.create(Registries.ENTITY_TYPE, Tailings.id("track_dozer")));

	public static final EntityType<GantryEntity> PUMP_GANTRY = gantryType(TbmGantry.PUMP);
	public static final EntityType<GantryEntity> REEL_GANTRY = gantryType(TbmGantry.REEL);
	public static final EntityType<GantryEntity> CONVEYOR_GANTRY = gantryType(TbmGantry.CONVEYOR);

	private static EntityType<GantryEntity> gantryType(TbmGantry kind) {
		return EntityType.Builder.<GantryEntity>of(GantryEntity::new, MobCategory.MISC)
				.sized(TbmGantry.WIDTH, TbmGantry.HEIGHT)
				.clientTrackingRange(16)
				.updateInterval(1)
				.build(ResourceKey.create(Registries.ENTITY_TYPE, Tailings.id(kind.id())));
	}

	public static EntityType<GantryEntity> gantry(TbmGantry kind) {
		return switch (kind) {
			case PUMP -> PUMP_GANTRY;
			case REEL -> REEL_GANTRY;
			case CONVEYOR -> CONVEYOR_GANTRY;
		};
	}

	private TailingsEntities() {
	}

	public static void register() {
		Registry.register(BuiltInRegistries.ENTITY_TYPE, Tailings.id("tunnel_drill"), DRILL);
		Registry.register(BuiltInRegistries.ENTITY_TYPE, Tailings.id("mine_train"), TRAIN);
		Registry.register(BuiltInRegistries.ENTITY_TYPE, Tailings.id("mine_locomotive"), LOCOMOTIVE);
		Registry.register(BuiltInRegistries.ENTITY_TYPE, Tailings.id("mine_car"), CAR);
		Registry.register(BuiltInRegistries.ENTITY_TYPE, Tailings.id("tunnel_boring_machine"), TBM);
		Registry.register(BuiltInRegistries.ENTITY_TYPE, Tailings.id("haul_truck"), HAUL_TRUCK);
		Registry.register(BuiltInRegistries.ENTITY_TYPE, Tailings.id("wheel_loader"), WHEEL_LOADER);
		Registry.register(BuiltInRegistries.ENTITY_TYPE, Tailings.id("blasthole_drill"), BLASTHOLE_DRILL);
		Registry.register(BuiltInRegistries.ENTITY_TYPE, Tailings.id("emulsion_truck"), EMULSION_TRUCK);
		Registry.register(BuiltInRegistries.ENTITY_TYPE, Tailings.id("track_dozer"), TRACK_DOZER);
		Registry.register(BuiltInRegistries.ENTITY_TYPE, Tailings.id("ultra_haul_truck"), ULTRA_HAUL_TRUCK);
		Registry.register(BuiltInRegistries.ENTITY_TYPE, Tailings.id("hydraulic_shovel"), HYDRAULIC_SHOVEL);
		for (TbmGantry kind : TbmGantry.values()) {
			Registry.register(BuiltInRegistries.ENTITY_TYPE, Tailings.id(kind.id()), gantry(kind));
		}
	}
}
