package dev.wildcatter.tailings.item;

import dev.wildcatter.tailings.entity.PitVehicleEntity;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.phys.Vec3;

/** A haul truck or wheel loader item: right-click the ground to set the machine down there, facing the way you look. */
public class PitVehicleItem extends Item {
	private final Supplier<EntityType<? extends PitVehicleEntity>> type;

	public PitVehicleItem(Properties properties, Supplier<EntityType<? extends PitVehicleEntity>> type) {
		super(properties);
		this.type = type;
	}

	@Override
	public InteractionResult useOn(UseOnContext context) {
		if (!(context.getLevel() instanceof ServerLevel level)) {
			return InteractionResult.SUCCESS;
		}
		BlockPos on = context.getClickedPos().relative(context.getClickedFace());
		Player player = context.getPlayer();
		PitVehicleEntity vehicle = type.get().create(level, net.minecraft.world.entity.EntitySpawnReason.SPAWN_ITEM_USE);
		if (vehicle == null) {
			return InteractionResult.FAIL;
		}
		vehicle.setPos(new Vec3(on.getX() + 0.5, on.getY(), on.getZ() + 0.5));
		vehicle.setYRot(player == null ? 0 : player.getYRot());
		if (!level.noCollision(vehicle, vehicle.getBoundingBox().deflate(0.05))) {
			if (player != null) {
				player.sendOverlayMessage(Component.literal("No room: it needs " + (int) vehicle.spec().length() + " x " + (int) Math.ceil(vehicle.spec().width())
						+ " m of clear ground"));
			}
			return InteractionResult.FAIL;
		}
		level.addFreshEntity(vehicle);
		if (player == null || !player.hasInfiniteMaterials()) {
			context.getItemInHand().shrink(1);
		}
		return InteractionResult.SUCCESS;
	}
}
