package dev.wildcatter.tailings;

import dev.wildcatter.tailings.entity.MineTrainEntity;
import dev.wildcatter.tailings.entity.MineVehicleEntity;
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

	private TailingsEntities() {
	}

	public static void register() {
		Registry.register(BuiltInRegistries.ENTITY_TYPE, Tailings.id("tunnel_drill"), DRILL);
		Registry.register(BuiltInRegistries.ENTITY_TYPE, Tailings.id("mine_train"), TRAIN);
		Registry.register(BuiltInRegistries.ENTITY_TYPE, Tailings.id("mine_locomotive"), LOCOMOTIVE);
		Registry.register(BuiltInRegistries.ENTITY_TYPE, Tailings.id("mine_car"), CAR);
	}
}
