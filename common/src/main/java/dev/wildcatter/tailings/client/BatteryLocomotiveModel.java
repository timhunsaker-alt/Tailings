package dev.wildcatter.tailings.client;

import dev.wildcatter.tailings.Tailings;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;

/**
 * The 8 t battery locomotive (docs/multiblocks/battery-locomotive.md), 3 m long: the trolley locomotive's frame and cab, its traction
 * battery's steel box in the middle, no collector shoe. The root sits 24 pixels above the rail. Texture is 256x256, painted by
 * tools/models/vehicles.py.
 */
public class BatteryLocomotiveModel extends EntityModel<EntityRenderState> {
	public static final ModelLayerLocation LAYER = new ModelLayerLocation(Tailings.id("battery_locomotive"), "main");
	public static final String HEADLIGHT = "headlight";
	public static final String BATTERY = "battery";

	public BatteryLocomotiveModel(ModelPart root) {
		super(root, id -> RenderTypes.entityCutout(id));
	}

	public static LayerDefinition createBodyLayer() {
		MeshDefinition mesh = new MeshDefinition();
		PartDefinition root = mesh.getRoot();
		root.addOrReplaceChild("frame", CubeListBuilder.create().texOffs(0, 0).addBox(-7F, 19F, -24F, 14F, 3F, 48F), PartPose.ZERO);
		root.addOrReplaceChild("hood", CubeListBuilder.create().texOffs(125, 0).addBox(-6F, 11F, -22F, 12F, 8F, 8F), PartPose.ZERO);
		root.addOrReplaceChild("battery", CubeListBuilder.create().texOffs(166, 0).addBox(-7F, 8F, -14F, 14F, 11F, 20F), PartPose.ZERO);
		root.addOrReplaceChild("lid", CubeListBuilder.create().texOffs(0, 52).addBox(-7F, 7F, -14F, 14F, 1F, 20F), PartPose.ZERO);
		root.addOrReplaceChild("lug_f", CubeListBuilder.create().texOffs(69, 52).addBox(-1F, 5F, -12F, 2F, 2F, 1F), PartPose.ZERO);
		root.addOrReplaceChild("lug_r", CubeListBuilder.create().texOffs(76, 52).addBox(-1F, 5F, 3F, 2F, 2F, 1F), PartPose.ZERO);
		root.addOrReplaceChild("socket", CubeListBuilder.create().texOffs(83, 52).addBox(7F, 12F, -6F, 1F, 3F, 3F), PartPose.ZERO);
		root.addOrReplaceChild("cab", CubeListBuilder.create().texOffs(92, 52).addBox(-7F, 3F, 6F, 14F, 16F, 16F), PartPose.ZERO);
		root.addOrReplaceChild("roof", CubeListBuilder.create().texOffs(153, 52).addBox(-8F, 2F, 5F, 16F, 1F, 18F), PartPose.ZERO);
		root.addOrReplaceChild("headlight", CubeListBuilder.create().texOffs(222, 52).addBox(-2F, 13F, -23F, 4F, 3F, 1F), PartPose.ZERO);
		root.addOrReplaceChild("bumper_f", CubeListBuilder.create().texOffs(0, 85).addBox(-7F, 20F, -25F, 14F, 2F, 1F), PartPose.ZERO);
		root.addOrReplaceChild("bumper_r", CubeListBuilder.create().texOffs(31, 85).addBox(-7F, 20F, 24F, 14F, 2F, 1F), PartPose.ZERO);
		root.addOrReplaceChild("coupler", CubeListBuilder.create().texOffs(62, 85).addBox(-1F, 20F, 25F, 2F, 2F, 3F), PartPose.ZERO);
		root.addOrReplaceChild("pinion", CubeListBuilder.create().texOffs(73, 85).addBox(-1F, 21F, -3F, 2F, 3F, 6F), PartPose.ZERO);
		root.addOrReplaceChild("wheel_fl", CubeListBuilder.create().texOffs(90, 85).addBox(-7F, 20F, -17F, 1F, 1F, 2F).addBox(-7F, 21F, -18F, 1F, 1F, 4F).addBox(-7F, 22F, -18F, 1F, 1F, 4F).addBox(-7F, 23F, -17F, 1F, 1F, 2F), PartPose.ZERO);
		root.addOrReplaceChild("wheel_fr", CubeListBuilder.create().texOffs(101, 85).addBox(6F, 20F, -17F, 1F, 1F, 2F).addBox(6F, 21F, -18F, 1F, 1F, 4F).addBox(6F, 22F, -18F, 1F, 1F, 4F).addBox(6F, 23F, -17F, 1F, 1F, 2F), PartPose.ZERO);
		root.addOrReplaceChild("wheel_rl", CubeListBuilder.create().texOffs(112, 85).addBox(-7F, 20F, 15F, 1F, 1F, 2F).addBox(-7F, 21F, 14F, 1F, 1F, 4F).addBox(-7F, 22F, 14F, 1F, 1F, 4F).addBox(-7F, 23F, 15F, 1F, 1F, 2F), PartPose.ZERO);
		root.addOrReplaceChild("wheel_rr", CubeListBuilder.create().texOffs(123, 85).addBox(6F, 20F, 15F, 1F, 1F, 2F).addBox(6F, 21F, 14F, 1F, 1F, 4F).addBox(6F, 22F, 14F, 1F, 1F, 4F).addBox(6F, 23F, 15F, 1F, 1F, 2F), PartPose.ZERO);
		return LayerDefinition.create(mesh, 256, 256);
	}
}
