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
import net.minecraft.client.renderer.rendertype.RenderTypes;

/** The 100 t class haul truck at its real size, 10.0 x 6.2 x 5.2 m; its body tips about its rear hinge. Written by tools/models/pit_vehicles.py, with its 1024 x 1024 placeholder skin: change the script, not this file. */
public class HaulTruckModel extends EntityModel<PitVehicleRenderer.PitState> {
	public static final ModelLayerLocation LAYER = new ModelLayerLocation(Tailings.id("haul_truck"), "main");
	public static final String BODY = "body";

	public HaulTruckModel(ModelPart root) {
		super(root, id -> RenderTypes.entityCutout(id));
	}

	public static LayerDefinition createBodyLayer() {
		MeshDefinition mesh = new MeshDefinition();
		PartDefinition root = mesh.getRoot();
		root.addOrReplaceChild("tyre_fl", CubeListBuilder.create().texOffs(0, 0).addBox(37F, -43F, -67F, 13F, 43F, 43F), PartPose.ZERO);
		root.addOrReplaceChild("tyre_fr", CubeListBuilder.create().texOffs(113, 0).addBox(-50F, -43F, -67F, 13F, 43F, 43F), PartPose.ZERO);
		root.addOrReplaceChild("tyre_rlo", CubeListBuilder.create().texOffs(226, 0).addBox(37F, -43F, 16F, 13F, 43F, 43F), PartPose.ZERO);
		root.addOrReplaceChild("tyre_rli", CubeListBuilder.create().texOffs(339, 0).addBox(23F, -43F, 16F, 13F, 43F, 43F), PartPose.ZERO);
		root.addOrReplaceChild("tyre_rri", CubeListBuilder.create().texOffs(452, 0).addBox(-36F, -43F, 16F, 13F, 43F, 43F), PartPose.ZERO);
		root.addOrReplaceChild("tyre_rro", CubeListBuilder.create().texOffs(565, 0).addBox(-50F, -43F, 16F, 13F, 43F, 43F), PartPose.ZERO);
		root.addOrReplaceChild("axle", CubeListBuilder.create().texOffs(678, 0).addBox(-23F, -26F, 34F, 46F, 8F, 8F), PartPose.ZERO);
		root.addOrReplaceChild("frame", CubeListBuilder.create().texOffs(0, 87).addBox(-14F, -32F, -78F, 29F, 11F, 142F), PartPose.ZERO);
		root.addOrReplaceChild("hood", CubeListBuilder.create().texOffs(343, 87).addBox(-18F, -48F, -80F, 35F, 22F, 38F), PartPose.ZERO);
		root.addOrReplaceChild("grille", CubeListBuilder.create().texOffs(490, 87).addBox(-19F, -48F, -82F, 38F, 24F, 2F), PartPose.ZERO);
		root.addOrReplaceChild("bumper", CubeListBuilder.create().texOffs(571, 87).addBox(-32F, -26F, -83F, 64F, 6F, 5F), PartPose.ZERO);
		root.addOrReplaceChild("deck", CubeListBuilder.create().texOffs(710, 87).addBox(-50F, -50F, -80F, 99F, 2F, 42F), PartPose.ZERO);
		root.addOrReplaceChild("cab", CubeListBuilder.create().texOffs(0, 241).addBox(21F, -80F, -74F, 27F, 30F, 32F), PartPose.ZERO);
		PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create(), PartPose.offset(0F, -32F, 74F));
		body.addOrReplaceChild("floor", CubeListBuilder.create().texOffs(119, 241).addBox(-44F, -5F, -112F, 88F, 5F, 117F), PartPose.ZERO);
		body.addOrReplaceChild("side_l", CubeListBuilder.create().texOffs(530, 241).addBox(41F, -42F, -112F, 3F, 37F, 110F), PartPose.ZERO);
		body.addOrReplaceChild("side_r", CubeListBuilder.create().texOffs(757, 241).addBox(-44F, -42F, -112F, 3F, 37F, 110F), PartPose.ZERO);
		body.addOrReplaceChild("front_wall", CubeListBuilder.create().texOffs(0, 389).addBox(-44F, -48F, -116F, 88F, 43F, 3F), PartPose.ZERO);
		body.addOrReplaceChild("canopy", CubeListBuilder.create().texOffs(183, 389).addBox(-50F, -51F, -154F, 99F, 3F, 42F), PartPose.ZERO);
		body.addOrReplaceChild("load", CubeListBuilder.create().texOffs(466, 389).addBox(-41F, -38F, -112F, 82F, 34F, 109F), PartPose.ZERO);
		return LayerDefinition.create(mesh, 1024, 1024);
	}

	/** Set when the frame is drawn, from this machine's own state. */
	@Override
	public void setupAnim(PitVehicleRenderer.PitState state) {
		super.setupAnim(state);
		ModelPart body = root().getChild(BODY);
		body.xRot = -state.lift * 0.84F;   // the body tips to 48 degrees (proposed)
		body.getChild("load").visible = state.loaded;
	}
}
