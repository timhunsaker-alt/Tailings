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

/** Granby car. The {@code load} cube is shown only while the car is carrying rock. Texture is 128x128. */
public class MineCarModel extends EntityModel<EntityRenderState> {
	public static final ModelLayerLocation LAYER = new ModelLayerLocation(Tailings.id("mine_car"), "main");
	public static final String LOAD = "load";

	public MineCarModel(ModelPart root) {
		super(root, id -> RenderTypes.entityCutout(id));
	}

	public static LayerDefinition createBodyLayer() {
		MeshDefinition mesh = new MeshDefinition();
		PartDefinition root = mesh.getRoot();
		root.addOrReplaceChild("frame", CubeListBuilder.create().texOffs(0, 0).addBox(-6F, 20F, -10F, 12F, 2F, 20F), PartPose.ZERO);
		root.addOrReplaceChild("floor", CubeListBuilder.create().texOffs(0, 22).addBox(-7F, 19F, -10F, 14F, 1F, 20F), PartPose.ZERO);
		root.addOrReplaceChild("side_l", CubeListBuilder.create().texOffs(68, 22).addBox(-7F, 11F, -10F, 1F, 8F, 20F), PartPose.ZERO);
		root.addOrReplaceChild("side_r", CubeListBuilder.create().texOffs(0, 50).addBox(6F, 11F, -10F, 1F, 8F, 20F), PartPose.ZERO);
		root.addOrReplaceChild("end_f", CubeListBuilder.create().texOffs(42, 50).addBox(-6F, 11F, -10F, 12F, 8F, 1F), PartPose.ZERO);
		root.addOrReplaceChild("end_b", CubeListBuilder.create().texOffs(68, 50).addBox(-6F, 11F, 9F, 12F, 8F, 1F), PartPose.ZERO);
		root.addOrReplaceChild(LOAD, CubeListBuilder.create().texOffs(0, 78).addBox(-6F, 12F, -9F, 12F, 7F, 18F), PartPose.ZERO);
		root.addOrReplaceChild("coupler_f", CubeListBuilder.create().texOffs(60, 78).addBox(-1F, 20F, -12F, 2F, 2F, 2F), PartPose.ZERO);
		root.addOrReplaceChild("coupler_b", CubeListBuilder.create().texOffs(68, 78).addBox(-1F, 20F, 10F, 2F, 2F, 2F), PartPose.ZERO);
		root.addOrReplaceChild("dump_wheel", CubeListBuilder.create().texOffs(76, 78).addBox(7F, 17F, -3F, 1F, 3F, 6F), PartPose.ZERO);
		root.addOrReplaceChild("wheel_fl", CubeListBuilder.create().texOffs(90, 78).addBox(-7F, 20F, -8F, 1F, 4F, 4F), PartPose.ZERO);
		root.addOrReplaceChild("wheel_fr", CubeListBuilder.create().texOffs(100, 78).addBox(6F, 20F, -8F, 1F, 4F, 4F), PartPose.ZERO);
		root.addOrReplaceChild("wheel_rl", CubeListBuilder.create().texOffs(110, 78).addBox(-7F, 20F, 4F, 1F, 4F, 4F), PartPose.ZERO);
		root.addOrReplaceChild("wheel_rr", CubeListBuilder.create().texOffs(0, 103).addBox(6F, 20F, 4F, 1F, 4F, 4F), PartPose.ZERO);
		return LayerDefinition.create(mesh, 128, 128);
	}
}
