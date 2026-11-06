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

/** Granby car, 2 m long. The {@code load} cube is shown only while the car is carrying rock. Texture is 256x256, painted by tools/models/vehicles.py. */
public class MineCarModel extends EntityModel<EntityRenderState> {
	public static final ModelLayerLocation LAYER = new ModelLayerLocation(Tailings.id("mine_car"), "main");
	public static final String LOAD = "load";

	public MineCarModel(ModelPart root) {
		super(root, id -> RenderTypes.entityCutout(id));
	}

	public static LayerDefinition createBodyLayer() {
		MeshDefinition mesh = new MeshDefinition();
		PartDefinition root = mesh.getRoot();
		root.addOrReplaceChild("frame", CubeListBuilder.create().texOffs(0, 0).addBox(-6F, 20F, -16F, 12F, 2F, 32F), PartPose.ZERO);
		root.addOrReplaceChild("floor", CubeListBuilder.create().texOffs(89, 0).addBox(-8F, 19F, -16F, 16F, 1F, 32F), PartPose.ZERO);
		root.addOrReplaceChild("side_l", CubeListBuilder.create().texOffs(186, 0).addBox(-8F, 5F, -16F, 1F, 14F, 32F), PartPose.ZERO);
		root.addOrReplaceChild("side_r", CubeListBuilder.create().texOffs(0, 47).addBox(7F, 5F, -16F, 1F, 14F, 32F), PartPose.ZERO);
		root.addOrReplaceChild("end_f", CubeListBuilder.create().texOffs(67, 47).addBox(-7F, 5F, -16F, 14F, 14F, 1F), PartPose.ZERO);
		root.addOrReplaceChild("end_b", CubeListBuilder.create().texOffs(98, 47).addBox(-7F, 5F, 15F, 14F, 14F, 1F), PartPose.ZERO);
		root.addOrReplaceChild(LOAD, CubeListBuilder.create().texOffs(129, 47).addBox(-7F, 6F, -15F, 14F, 13F, 30F), PartPose.ZERO);
		root.addOrReplaceChild("coupler_f", CubeListBuilder.create().texOffs(218, 47).addBox(-1F, 20F, -20F, 2F, 2F, 4F), PartPose.ZERO);
		root.addOrReplaceChild("coupler_b", CubeListBuilder.create().texOffs(231, 47).addBox(-1F, 20F, 16F, 2F, 2F, 4F), PartPose.ZERO);
		root.addOrReplaceChild("dump_wheel", CubeListBuilder.create().texOffs(0, 94).addBox(8F, 17F, -3F, 1F, 3F, 6F), PartPose.ZERO);
		root.addOrReplaceChild("wheel_fl", CubeListBuilder.create().texOffs(15, 94).addBox(-7F, 20F, -13F, 1F, 4F, 4F), PartPose.ZERO);
		root.addOrReplaceChild("wheel_fr", CubeListBuilder.create().texOffs(26, 94).addBox(6F, 20F, -13F, 1F, 4F, 4F), PartPose.ZERO);
		root.addOrReplaceChild("wheel_rl", CubeListBuilder.create().texOffs(37, 94).addBox(-7F, 20F, 9F, 1F, 4F, 4F), PartPose.ZERO);
		root.addOrReplaceChild("wheel_rr", CubeListBuilder.create().texOffs(48, 94).addBox(6F, 20F, 9F, 1F, 4F, 4F), PartPose.ZERO);
		return LayerDefinition.create(mesh, 256, 256);
	}
}
