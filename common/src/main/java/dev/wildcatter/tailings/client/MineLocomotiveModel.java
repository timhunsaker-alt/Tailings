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

/** The mine locomotive, 3 m long. The root sits 24 pixels above the rail; the wheels rest on it. Texture is 256x256, painted by tools/models/vehicles.py. */
public class MineLocomotiveModel extends EntityModel<EntityRenderState> {
	public static final ModelLayerLocation LAYER = new ModelLayerLocation(Tailings.id("mine_locomotive"), "main");
	public static final String HEADLIGHT = "headlight";

	public MineLocomotiveModel(ModelPart root) {
		super(root, id -> RenderTypes.entityCutout(id));
	}

	public static LayerDefinition createBodyLayer() {
		MeshDefinition mesh = new MeshDefinition();
		PartDefinition root = mesh.getRoot();
		root.addOrReplaceChild("frame", CubeListBuilder.create().texOffs(0, 0).addBox(-7F, 19F, -24F, 14F, 3F, 48F), PartPose.ZERO);
		root.addOrReplaceChild("hood", CubeListBuilder.create().texOffs(125, 0).addBox(-6F, 9F, -22F, 12F, 10F, 28F), PartPose.ZERO);
		root.addOrReplaceChild("cab", CubeListBuilder.create().texOffs(0, 52).addBox(-7F, 3F, 6F, 14F, 16F, 16F), PartPose.ZERO);
		root.addOrReplaceChild("roof", CubeListBuilder.create().texOffs(61, 52).addBox(-8F, 2F, 5F, 16F, 1F, 18F), PartPose.ZERO);
		root.addOrReplaceChild(HEADLIGHT, CubeListBuilder.create().texOffs(130, 52).addBox(-2F, 11F, -23F, 4F, 3F, 1F), PartPose.ZERO);
		root.addOrReplaceChild("bumper_f", CubeListBuilder.create().texOffs(141, 52).addBox(-7F, 20F, -25F, 14F, 2F, 1F), PartPose.ZERO);
		root.addOrReplaceChild("bumper_r", CubeListBuilder.create().texOffs(172, 52).addBox(-7F, 20F, 24F, 14F, 2F, 1F), PartPose.ZERO);
		root.addOrReplaceChild("coupler", CubeListBuilder.create().texOffs(203, 52).addBox(-1F, 20F, 25F, 2F, 2F, 3F), PartPose.ZERO);
		root.addOrReplaceChild("pinion", CubeListBuilder.create().texOffs(214, 52).addBox(-1F, 21F, -3F, 2F, 3F, 6F), PartPose.ZERO);
		root.addOrReplaceChild("shoe", CubeListBuilder.create().texOffs(231, 52).addBox(7F, 21F, -2F, 2F, 2F, 4F), PartPose.ZERO);
		root.addOrReplaceChild("wheel_fl", CubeListBuilder.create().texOffs(244, 52).addBox(-7F, 20F, -17F, 1F, 1F, 2F).addBox(-7F, 21F, -18F, 1F, 1F, 4F).addBox(-7F, 22F, -18F, 1F, 1F, 4F).addBox(-7F, 23F, -17F, 1F, 1F, 2F), PartPose.ZERO);
		root.addOrReplaceChild("wheel_fr", CubeListBuilder.create().texOffs(0, 85).addBox(6F, 20F, -17F, 1F, 1F, 2F).addBox(6F, 21F, -18F, 1F, 1F, 4F).addBox(6F, 22F, -18F, 1F, 1F, 4F).addBox(6F, 23F, -17F, 1F, 1F, 2F), PartPose.ZERO);
		root.addOrReplaceChild("wheel_rl", CubeListBuilder.create().texOffs(11, 85).addBox(-7F, 20F, 15F, 1F, 1F, 2F).addBox(-7F, 21F, 14F, 1F, 1F, 4F).addBox(-7F, 22F, 14F, 1F, 1F, 4F).addBox(-7F, 23F, 15F, 1F, 1F, 2F), PartPose.ZERO);
		root.addOrReplaceChild("wheel_rr", CubeListBuilder.create().texOffs(22, 85).addBox(6F, 20F, 15F, 1F, 1F, 2F).addBox(6F, 21F, 14F, 1F, 1F, 4F).addBox(6F, 22F, 14F, 1F, 1F, 4F).addBox(6F, 23F, 15F, 1F, 1F, 2F), PartPose.ZERO);
		return LayerDefinition.create(mesh, 256, 256);
	}
}
