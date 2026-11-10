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

/** The rotary blasthole drill at its real size, 7 x 15 m on its crawlers and 26 m to the mast top; its rotary head runs down the mast as it drills. Written by tools/models/pit_vehicles.py, with its 2048 x 2048 skin painted in Ludo's materials: change the script, not this file. */
public class BlastholeDrillModel extends EntityModel<PitVehicleRenderer.PitState> {
	public static final ModelLayerLocation LAYER = new ModelLayerLocation(Tailings.id("blasthole_drill"), "main");
	/** Its wheels, each turning on its own axle as it rolls, and their radius in metres. */
	public static final String[] WHEELS = {};
	public static final float WHEEL_RADIUS = 1.0F;
	public static final String HEAD = "head";

	public BlastholeDrillModel(ModelPart root) {
		super(root, id -> RenderTypes.entityCutout(id));
	}

	public static LayerDefinition createBodyLayer() {
		MeshDefinition mesh = new MeshDefinition();
		PartDefinition root = mesh.getRoot();
		root.addOrReplaceChild("mast", CubeListBuilder.create().texOffs(0, 0).addBox(-14F, -416F, 106F, 29F, 365F, 10F), PartPose.ZERO);
		root.addOrReplaceChild("mast_l", CubeListBuilder.create().texOffs(79, 0).addBox(11F, -416F, 77F, 3F, 365F, 29F), PartPose.ZERO);
		root.addOrReplaceChild("mast_r", CubeListBuilder.create().texOffs(144, 0).addBox(-14F, -416F, 77F, 3F, 365F, 29F), PartPose.ZERO);
		root.addOrReplaceChild("pipe", CubeListBuilder.create().texOffs(209, 0).addBox(-2F, -360F, 94F, 5F, 360F, 5F), PartPose.ZERO);
		root.addOrReplaceChild("track_l", CubeListBuilder.create().texOffs(230, 0).addBox(35F, -26F, -88F, 21F, 26F, 160F), PartPose.ZERO);
		root.addOrReplaceChild("track_r", CubeListBuilder.create().texOffs(593, 0).addBox(-56F, -26F, -88F, 21F, 26F, 160F), PartPose.ZERO);
		root.addOrReplaceChild("carbody", CubeListBuilder.create().texOffs(956, 0).addBox(-35F, -26F, -64F, 70F, 18F, 112F), PartPose.ZERO);
		root.addOrReplaceChild("deck", CubeListBuilder.create().texOffs(1321, 0).addBox(-56F, -35F, -120F, 112F, 10F, 240F), PartPose.ZERO);
		root.addOrReplaceChild("house", CubeListBuilder.create().texOffs(0, 395).addBox(-16F, -83F, -96F, 69F, 48F, 136F), PartPose.ZERO);
		root.addOrReplaceChild("cab", CubeListBuilder.create().texOffs(411, 395).addBox(-54F, -90F, -118F, 34F, 54F, 45F), PartPose.ZERO);
		root.addOrReplaceChild("compressor", CubeListBuilder.create().texOffs(570, 395).addBox(-13F, -64F, 45F, 66F, 29F, 27F), PartPose.ZERO);
		root.addOrReplaceChild("jack_f", CubeListBuilder.create().texOffs(757, 395).addBox(-6F, -26F, -117F, 13F, 26F, 11F), PartPose.ZERO);
		root.addOrReplaceChild("jack_rl", CubeListBuilder.create().texOffs(806, 395).addBox(42F, -26F, 106F, 11F, 26F, 11F), PartPose.ZERO);
		root.addOrReplaceChild("jack_rr", CubeListBuilder.create().texOffs(851, 395).addBox(-53F, -26F, 106F, 11F, 26F, 11F), PartPose.ZERO);
		root.addOrReplaceChild("mast_table", CubeListBuilder.create().texOffs(896, 395).addBox(-24F, -51F, 74F, 48F, 16F, 45F), PartPose.ZERO);
		root.addOrReplaceChild("crown", CubeListBuilder.create().texOffs(1083, 395).addBox(-16F, -416F, 75F, 32F, 8F, 42F), PartPose.ZERO);
		root.addOrReplaceChild("dust", CubeListBuilder.create().texOffs(1232, 395).addBox(-42F, -83F, 48F, 26F, 48F, 24F), PartPose.ZERO);
		root.addOrReplaceChild("skirt", CubeListBuilder.create().texOffs(1333, 395).addBox(-19F, -26F, 80F, 38F, 16F, 32F), PartPose.ZERO);
		PartDefinition head = root.addOrReplaceChild("head", CubeListBuilder.create(), PartPose.offset(0F, -368F, 96F));
		head.addOrReplaceChild("head", CubeListBuilder.create().texOffs(1474, 395).addBox(-10F, -24F, -10F, 19F, 32F, 19F), PartPose.ZERO);
		return LayerDefinition.create(mesh, 2048, 2048);
	}

	/** Set when the frame is drawn, from this machine's own state. */
	@Override
	public void setupAnim(PitVehicleRenderer.PitState state) {
		super.setupAnim(state);
		for (String wheel : WHEELS) {
			root().getChild(wheel).xRot = state.roll / WHEEL_RADIUS;   // turned through the distance rolled over the radius
		}
		ModelPart head = root().getChild(HEAD);
		head.y = -368F + state.lift * 256F;   // the head comes down 16 m as the hole deepens
	}
}
