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

/** Battery locomotive. The root sits 24 pixels above the rail; the wheels rest on it. Texture is 128x128. */
public class MineLocomotiveModel extends EntityModel<EntityRenderState> {
	public static final ModelLayerLocation LAYER = new ModelLayerLocation(Tailings.id("mine_locomotive"), "main");
	public static final String HEADLIGHT = "headlight";

	public MineLocomotiveModel(ModelPart root) {
		super(root, id -> RenderTypes.entityCutout(id));
	}

	public static LayerDefinition createBodyLayer() {
		MeshDefinition mesh = new MeshDefinition();
		PartDefinition root = mesh.getRoot();
		root.addOrReplaceChild("frame", CubeListBuilder.create().texOffs(0, 0).addBox(-7F, 19F, -12F, 14F, 3F, 24F), PartPose.ZERO);
		root.addOrReplaceChild("battery", CubeListBuilder.create().texOffs(76, 0).addBox(-6F, 13F, -11F, 12F, 6F, 13F), PartPose.ZERO);
		root.addOrReplaceChild("cab", CubeListBuilder.create().texOffs(0, 27).addBox(-6F, 10F, 3F, 12F, 9F, 8F), PartPose.ZERO);
		root.addOrReplaceChild("roof", CubeListBuilder.create().texOffs(40, 27).addBox(-7F, 9F, 2F, 14F, 1F, 10F), PartPose.ZERO);
		root.addOrReplaceChild(HEADLIGHT, CubeListBuilder.create().texOffs(88, 27).addBox(-2F, 14F, -12F, 4F, 3F, 1F), PartPose.ZERO);
		root.addOrReplaceChild("bumper_f", CubeListBuilder.create().texOffs(98, 27).addBox(-7F, 20F, -13F, 14F, 2F, 1F), PartPose.ZERO);
		root.addOrReplaceChild("bumper_r", CubeListBuilder.create().texOffs(0, 44).addBox(-7F, 20F, 12F, 14F, 2F, 1F), PartPose.ZERO);
		root.addOrReplaceChild("wheel_fl", CubeListBuilder.create().texOffs(30, 44).addBox(-7F, 20F, -9F, 1F, 4F, 4F), PartPose.ZERO);
		root.addOrReplaceChild("wheel_fr", CubeListBuilder.create().texOffs(40, 44).addBox(6F, 20F, -9F, 1F, 4F, 4F), PartPose.ZERO);
		root.addOrReplaceChild("wheel_rl", CubeListBuilder.create().texOffs(50, 44).addBox(-7F, 20F, 5F, 1F, 4F, 4F), PartPose.ZERO);
		root.addOrReplaceChild("wheel_rr", CubeListBuilder.create().texOffs(60, 44).addBox(6F, 20F, 5F, 1F, 4F, 4F), PartPose.ZERO);
		return LayerDefinition.create(mesh, 128, 128);
	}
}
