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
 * The bottom-dump car (docs/multiblocks/bottom-dump-cars.md), 2 m long, the Granby car's size: its floor is two drop-bottom doors
 * hinged along its sides, which swing down over a track grizzly. The {@code load} cube is shown only while it carries rock. Texture
 * is 256x256, painted by tools/models/vehicles.py.
 */
public class BottomDumpCarModel extends EntityModel<EntityRenderState> {
	public static final ModelLayerLocation LAYER = new ModelLayerLocation(Tailings.id("bottom_dump_car"), "main");
	public static final String LOAD = "load";
	public static final String DOOR_LEFT = "door_l";
	public static final String DOOR_RIGHT = "door_r";
	/** How far the doors swing down when open, in radians (60 degrees). */
	public static final float DOOR_OPEN = (float) Math.toRadians(60);

	public BottomDumpCarModel(ModelPart root) {
		super(root, id -> RenderTypes.entityCutout(id));
	}

	/** Swings both doors open or shut. */
	public void doors(boolean open) {
		root().getChild(DOOR_LEFT).zRot = open ? DOOR_OPEN : 0F;
		root().getChild(DOOR_RIGHT).zRot = open ? -DOOR_OPEN : 0F;
	}

	public static LayerDefinition createBodyLayer() {
		MeshDefinition mesh = new MeshDefinition();
		PartDefinition root = mesh.getRoot();
		root.addOrReplaceChild("sill_l", CubeListBuilder.create().texOffs(0, 0).addBox(-8F, 20F, -16F, 2F, 2F, 32F), PartPose.ZERO);
		root.addOrReplaceChild("sill_r", CubeListBuilder.create().texOffs(69, 0).addBox(6F, 20F, -16F, 2F, 2F, 32F), PartPose.ZERO);
		root.addOrReplaceChild("side_l", CubeListBuilder.create().texOffs(138, 0).addBox(-8F, 5F, -16F, 1F, 14F, 32F), PartPose.ZERO);
		root.addOrReplaceChild("side_r", CubeListBuilder.create().texOffs(0, 47).addBox(7F, 5F, -16F, 1F, 14F, 32F), PartPose.ZERO);
		root.addOrReplaceChild("end_f", CubeListBuilder.create().texOffs(67, 47).addBox(-7F, 5F, -16F, 14F, 15F, 1F), PartPose.ZERO);
		root.addOrReplaceChild("end_b", CubeListBuilder.create().texOffs(98, 47).addBox(-7F, 5F, 15F, 14F, 15F, 1F), PartPose.ZERO);
		root.addOrReplaceChild("door_l", CubeListBuilder.create().texOffs(129, 47).addBox(0F, -0.5F, -15F, 7F, 1F, 30F), PartPose.offset(-7F, 19.5F, 0F));
		root.addOrReplaceChild("door_r", CubeListBuilder.create().texOffs(0, 94).addBox(-7F, -0.5F, -15F, 7F, 1F, 30F), PartPose.offset(7F, 19.5F, 0F));
		root.addOrReplaceChild("load", CubeListBuilder.create().texOffs(75, 94).addBox(-7F, 6F, -15F, 14F, 13F, 30F), PartPose.ZERO);
		root.addOrReplaceChild("coupler_f", CubeListBuilder.create().texOffs(164, 94).addBox(-1F, 20F, -20F, 2F, 2F, 4F), PartPose.ZERO);
		root.addOrReplaceChild("coupler_b", CubeListBuilder.create().texOffs(177, 94).addBox(-1F, 20F, 16F, 2F, 2F, 4F), PartPose.ZERO);
		root.addOrReplaceChild("cam_roller", CubeListBuilder.create().texOffs(190, 94).addBox(8F, 18F, -2F, 1F, 2F, 4F), PartPose.ZERO);
		root.addOrReplaceChild("wheel_fl", CubeListBuilder.create().texOffs(201, 94).addBox(-7F, 20F, -12F, 1F, 1F, 2F).addBox(-7F, 21F, -13F, 1F, 1F, 4F).addBox(-7F, 22F, -13F, 1F, 1F, 4F).addBox(-7F, 23F, -12F, 1F, 1F, 2F), PartPose.ZERO);
		root.addOrReplaceChild("wheel_fr", CubeListBuilder.create().texOffs(212, 94).addBox(6F, 20F, -12F, 1F, 1F, 2F).addBox(6F, 21F, -13F, 1F, 1F, 4F).addBox(6F, 22F, -13F, 1F, 1F, 4F).addBox(6F, 23F, -12F, 1F, 1F, 2F), PartPose.ZERO);
		root.addOrReplaceChild("wheel_rl", CubeListBuilder.create().texOffs(223, 94).addBox(-7F, 20F, 10F, 1F, 1F, 2F).addBox(-7F, 21F, 9F, 1F, 1F, 4F).addBox(-7F, 22F, 9F, 1F, 1F, 4F).addBox(-7F, 23F, 10F, 1F, 1F, 2F), PartPose.ZERO);
		root.addOrReplaceChild("wheel_rr", CubeListBuilder.create().texOffs(234, 94).addBox(6F, 20F, 10F, 1F, 1F, 2F).addBox(6F, 21F, 9F, 1F, 1F, 4F).addBox(6F, 22F, 9F, 1F, 1F, 4F).addBox(6F, 23F, 10F, 1F, 1F, 2F), PartPose.ZERO);
		return LayerDefinition.create(mesh, 256, 256);
	}
}
