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

/** The wheel loader at its real size, 15 x 4.5 x 6 m; its arms and bucket lift about the boom pins. Written by tools/models/pit_vehicles.py, with its 1024 x 1024 placeholder skin: change the script, not this file. */
public class WheelLoaderModel extends EntityModel<PitVehicleRenderer.PitState> {
	public static final ModelLayerLocation LAYER = new ModelLayerLocation(Tailings.id("wheel_loader"), "main");
	public static final String ARM = "arm";

	public WheelLoaderModel(ModelPart root) {
		super(root, id -> RenderTypes.entityCutout(id));
	}

	public static LayerDefinition createBodyLayer() {
		MeshDefinition mesh = new MeshDefinition();
		PartDefinition root = mesh.getRoot();
		root.addOrReplaceChild("tyre_fl", CubeListBuilder.create().texOffs(0, 0).addBox(20F, -48F, -64F, 16F, 48F, 48F), PartPose.ZERO);
		root.addOrReplaceChild("tyre_fr", CubeListBuilder.create().texOffs(129, 0).addBox(-36F, -48F, -64F, 16F, 48F, 48F), PartPose.ZERO);
		root.addOrReplaceChild("tyre_rl", CubeListBuilder.create().texOffs(258, 0).addBox(20F, -48F, 24F, 16F, 48F, 48F), PartPose.ZERO);
		root.addOrReplaceChild("tyre_rr", CubeListBuilder.create().texOffs(387, 0).addBox(-36F, -48F, 24F, 16F, 48F, 48F), PartPose.ZERO);
		root.addOrReplaceChild("front_frame", CubeListBuilder.create().texOffs(516, 0).addBox(-16F, -35F, -72F, 32F, 19F, 75F), PartPose.ZERO);
		root.addOrReplaceChild("rear_frame", CubeListBuilder.create().texOffs(0, 97).addBox(-19F, -54F, 3F, 38F, 38F, 109F), PartPose.ZERO);
		root.addOrReplaceChild("counterweight", CubeListBuilder.create().texOffs(295, 97).addBox(-22F, -42F, 112F, 45F, 26F, 8F), PartPose.ZERO);
		root.addOrReplaceChild("fender_f", CubeListBuilder.create().texOffs(402, 97).addBox(-37F, -51F, -66F, 74F, 3F, 51F), PartPose.ZERO);
		root.addOrReplaceChild("fender_r", CubeListBuilder.create().texOffs(653, 97).addBox(-37F, -51F, 22F, 74F, 3F, 51F), PartPose.ZERO);
		root.addOrReplaceChild("cab", CubeListBuilder.create().texOffs(0, 245).addBox(-18F, -96F, -10F, 35F, 42F, 35F), PartPose.ZERO);
		PartDefinition arm = root.addOrReplaceChild("arm", CubeListBuilder.create(), PartPose.offset(0F, -48F, -61F));
		arm.addOrReplaceChild("boom_l", CubeListBuilder.create().texOffs(141, 245).addBox(13F, -3F, -51F, 6F, 35F, 54F), PartPose.ZERO);
		arm.addOrReplaceChild("boom_r", CubeListBuilder.create().texOffs(262, 245).addBox(-19F, -3F, -51F, 6F, 35F, 54F), PartPose.ZERO);
		arm.addOrReplaceChild("bucket", CubeListBuilder.create().texOffs(383, 245).addBox(-36F, 19F, -59F, 72F, 29F, 24F), PartPose.ZERO);
		arm.addOrReplaceChild("bucket_load", CubeListBuilder.create().texOffs(576, 245).addBox(-34F, 14F, -57F, 67F, 13F, 21F), PartPose.ZERO);
		return LayerDefinition.create(mesh, 1024, 1024);
	}

	/** Set when the frame is drawn, from this machine's own state. */
	@Override
	public void setupAnim(PitVehicleRenderer.PitState state) {
		super.setupAnim(state);
		ModelPart arm = root().getChild(ARM);
		arm.xRot = -state.lift * 0.9F;   // the arms lift the bucket over a truck's side
		arm.getChild("bucket_load").visible = state.loaded;
	}
}
