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

/** The 4100-class electric rope shovel at its real size: crawlers 14 x 12 m, the house 12 x 15 m swinging on them, the 20 m boom fixed at 45 degrees on its suspension ropes, and the dipper on its handle: hoisted up the face, crowded out and retracted through the saddle, its door tripping over the truck. Written by tools/models/pit_vehicles.py, with its 2048 x 2048 skin painted in Ludo's materials: change the script, not this file. */
public class RopeShovelModel extends EntityModel<PitVehicleRenderer.PitState> {
	public static final ModelLayerLocation LAYER = new ModelLayerLocation(Tailings.id("rope_shovel"), "main");
	/** Its wheels, each turning on its own axle as it rolls, and their radius in metres. */
	public static final String[] WHEELS = {"idler_l", "idler_r", "tumbler_l", "tumbler_r"};
	public static final float WHEEL_RADIUS = 1.0F;
	public static final String HOUSE = "house";
	public static final String BOOM = "boom";
	public static final String DIPPER = "dipper";

	public RopeShovelModel(ModelPart root) {
		super(root, id -> RenderTypes.entityCutout(id));
	}

	public static LayerDefinition createBodyLayer() {
		MeshDefinition mesh = new MeshDefinition();
		PartDefinition root = mesh.getRoot();
		root.addOrReplaceChild("crawler_l", CubeListBuilder.create().texOffs(0, 0).addBox(40F, -35F, -88F, 72F, 29F, 176F), PartPose.ZERO);
		root.addOrReplaceChild("crawler_r", CubeListBuilder.create().texOffs(497, 0).addBox(-112F, -35F, -88F, 72F, 29F, 176F), PartPose.ZERO);
		root.addOrReplaceChild("shoes_top_l", CubeListBuilder.create().texOffs(994, 0).addBox(40F, -42F, -91F, 72F, 6F, 182F), PartPose.ZERO);
		root.addOrReplaceChild("shoes_top_r", CubeListBuilder.create().texOffs(1503, 0).addBox(-112F, -42F, -91F, 72F, 6F, 182F), PartPose.ZERO);
		root.addOrReplaceChild("shoes_low_l", CubeListBuilder.create().texOffs(0, 206).addBox(40F, -6F, -91F, 72F, 6F, 182F), PartPose.ZERO);
		root.addOrReplaceChild("shoes_low_r", CubeListBuilder.create().texOffs(509, 206).addBox(-112F, -6F, -91F, 72F, 6F, 182F), PartPose.ZERO);
		root.addOrReplaceChild("carbody", CubeListBuilder.create().texOffs(1018, 206).addBox(-40F, -42F, -48F, 80F, 29F, 96F), PartPose.ZERO);
		root.addOrReplaceChild("roller_circle", CubeListBuilder.create().texOffs(1371, 206).addBox(-72F, -48F, -72F, 144F, 6F, 144F), PartPose.ZERO);
		PartDefinition house = root.addOrReplaceChild("house", CubeListBuilder.create(), PartPose.offset(0F, -48F, 0F));
		house.addOrReplaceChild("frame", CubeListBuilder.create().texOffs(0, 395).addBox(-96F, -13F, -112F, 192F, 13F, 240F), PartPose.ZERO);
		house.addOrReplaceChild("machinery", CubeListBuilder.create().texOffs(865, 395).addBox(-91F, -120F, -72F, 182F, 107F, 189F), PartPose.ZERO);
		house.addOrReplaceChild("roof", CubeListBuilder.create().texOffs(0, 692).addBox(-94F, -125F, -75F, 189F, 5F, 195F), PartPose.ZERO);
		house.addOrReplaceChild("counterweight", CubeListBuilder.create().texOffs(769, 692).addBox(-88F, -72F, 117F, 176F, 59F, 11F), PartPose.ZERO);
		house.addOrReplaceChild("cab_stand", CubeListBuilder.create().texOffs(1144, 692).addBox(40F, -56F, -112F, 51F, 43F, 40F), PartPose.ZERO);
		house.addOrReplaceChild("cab", CubeListBuilder.create().texOffs(1327, 692).addBox(40F, -109F, -112F, 51F, 53F, 40F), PartPose.ZERO);
		house.addOrReplaceChild("foot_brackets", CubeListBuilder.create().texOffs(1510, 692).addBox(-24F, -42F, -112F, 48F, 29F, 19F), PartPose.ZERO);
		house.addOrReplaceChild("gantry_leg_l", CubeListBuilder.create().texOffs(1645, 692).addBox(38F, -221F, -64F, 10F, 96F, 10F), PartPose.ZERO);
		house.addOrReplaceChild("gantry_leg_r", CubeListBuilder.create().texOffs(1686, 692).addBox(-48F, -221F, -64F, 10F, 96F, 10F), PartPose.ZERO);
		house.addOrReplaceChild("gantry_strut_l", CubeListBuilder.create().texOffs(1727, 692).addBox(38F, -197F, -26F, 10F, 72F, 10F), PartPose.ZERO);
		house.addOrReplaceChild("gantry_strut_r", CubeListBuilder.create().texOffs(1768, 692).addBox(-48F, -197F, -26F, 10F, 72F, 10F), PartPose.ZERO);
		house.addOrReplaceChild("gantry_head", CubeListBuilder.create().texOffs(0, 893).addBox(-48F, -221F, -64F, 96F, 10F, 29F), PartPose.ZERO);
		PartDefinition boom = house.addOrReplaceChild("boom", CubeListBuilder.create(), PartPose.offset(0F, -32F, -104F));
		boom.addOrReplaceChild("chord_l", CubeListBuilder.create().texOffs(251, 893).addBox(8F, -19F, -320F, 13F, 38F, 320F), PartPose.ZERO);
		boom.addOrReplaceChild("chord_r", CubeListBuilder.create().texOffs(918, 893).addBox(-21F, -19F, -320F, 13F, 38F, 320F), PartPose.ZERO);
		boom.addOrReplaceChild("bracing", CubeListBuilder.create().texOffs(0, 1252).addBox(-8F, -6F, -320F, 16F, 13F, 320F), PartPose.ZERO);
		boom.addOrReplaceChild("point_sheaves", CubeListBuilder.create().texOffs(673, 1252).addBox(-19F, -21F, -344F, 38F, 42F, 24F), PartPose.ZERO);
		PartDefinition dipper = house.addOrReplaceChild("dipper", CubeListBuilder.create(), PartPose.offset(0F, -134F, -206F));
		dipper.addOrReplaceChild("saddle", CubeListBuilder.create().texOffs(798, 1252).addBox(-22F, -16F, -12F, 45F, 34F, 24F), PartPose.ZERO);
		dipper.addOrReplaceChild("handle", CubeListBuilder.create().texOffs(937, 1252).addBox(-13F, -8F, -192F, 26F, 16F, 224F), PartPose.ZERO);
		dipper.addOrReplaceChild("dipper", CubeListBuilder.create().texOffs(1438, 1252).addBox(-37F, -34F, -264F, 74F, 72F, 72F), PartPose.ZERO);
		dipper.addOrReplaceChild("teeth", CubeListBuilder.create().texOffs(1731, 1252).addBox(-35F, 38F, -268F, 70F, 6F, 13F), PartPose.ZERO);
		dipper.addOrReplaceChild("door", CubeListBuilder.create().texOffs(1438, 1252).addBox(-37F, 38F, -264F, 74F, 4F, 72F), PartPose.ZERO);
		dipper.addOrReplaceChild("dipper_load", CubeListBuilder.create().texOffs(0, 1586).addBox(-32F, -36F, -260F, 64F, 18F, 62F), PartPose.ZERO);
		PartDefinition idler_l = root.addOrReplaceChild("idler_l", CubeListBuilder.create(), PartPose.offset(0.0F, -16F, -88F));
		idler_l.addOrReplaceChild("t0", CubeListBuilder.create().texOffs(253, 1586).addBox(40F, -16F, -4F, 72F, 1.0F, 8F), PartPose.ZERO);
		idler_l.addOrReplaceChild("t1", CubeListBuilder.create().texOffs(253, 1586).addBox(40F, -15F, -7F, 72F, 1.0F, 14F), PartPose.ZERO);
		idler_l.addOrReplaceChild("t2", CubeListBuilder.create().texOffs(253, 1586).addBox(40F, -14F, -9F, 72F, 1.0F, 18F), PartPose.ZERO);
		idler_l.addOrReplaceChild("t3", CubeListBuilder.create().texOffs(253, 1586).addBox(40F, -13F, -10F, 72F, 1.0F, 20F), PartPose.ZERO);
		idler_l.addOrReplaceChild("t4", CubeListBuilder.create().texOffs(253, 1586).addBox(40F, -12F, -11F, 72F, 1.0F, 22F), PartPose.ZERO);
		idler_l.addOrReplaceChild("t5", CubeListBuilder.create().texOffs(253, 1586).addBox(40F, -11F, -12F, 72F, 1.0F, 24F), PartPose.ZERO);
		idler_l.addOrReplaceChild("t6", CubeListBuilder.create().texOffs(253, 1586).addBox(40F, -10F, -13F, 72F, 1.0F, 26F), PartPose.ZERO);
		idler_l.addOrReplaceChild("t7", CubeListBuilder.create().texOffs(253, 1586).addBox(40F, -9F, -14F, 72F, 1.0F, 28F), PartPose.ZERO);
		idler_l.addOrReplaceChild("t8", CubeListBuilder.create().texOffs(253, 1586).addBox(40F, -8F, -14F, 72F, 1.0F, 28F), PartPose.ZERO);
		idler_l.addOrReplaceChild("t9", CubeListBuilder.create().texOffs(253, 1586).addBox(40F, -7F, -15F, 72F, 1.0F, 30F), PartPose.ZERO);
		idler_l.addOrReplaceChild("t10", CubeListBuilder.create().texOffs(253, 1586).addBox(40F, -6F, -15F, 72F, 1.0F, 30F), PartPose.ZERO);
		idler_l.addOrReplaceChild("t11", CubeListBuilder.create().texOffs(253, 1586).addBox(40F, -5F, -15F, 72F, 1.0F, 30F), PartPose.ZERO);
		idler_l.addOrReplaceChild("t12", CubeListBuilder.create().texOffs(253, 1586).addBox(40F, -4F, -16F, 72F, 1.0F, 32F), PartPose.ZERO);
		idler_l.addOrReplaceChild("t13", CubeListBuilder.create().texOffs(253, 1586).addBox(40F, -3F, -16F, 72F, 1.0F, 32F), PartPose.ZERO);
		idler_l.addOrReplaceChild("t14", CubeListBuilder.create().texOffs(253, 1586).addBox(40F, -2F, -16F, 72F, 1.0F, 32F), PartPose.ZERO);
		idler_l.addOrReplaceChild("t15", CubeListBuilder.create().texOffs(253, 1586).addBox(40F, -1F, -16F, 72F, 1.0F, 32F), PartPose.ZERO);
		idler_l.addOrReplaceChild("t16", CubeListBuilder.create().texOffs(253, 1586).addBox(40F, 0F, -16F, 72F, 1.0F, 32F), PartPose.ZERO);
		idler_l.addOrReplaceChild("t17", CubeListBuilder.create().texOffs(253, 1586).addBox(40F, 1F, -16F, 72F, 1.0F, 32F), PartPose.ZERO);
		idler_l.addOrReplaceChild("t18", CubeListBuilder.create().texOffs(253, 1586).addBox(40F, 2F, -16F, 72F, 1.0F, 32F), PartPose.ZERO);
		idler_l.addOrReplaceChild("t19", CubeListBuilder.create().texOffs(253, 1586).addBox(40F, 3F, -16F, 72F, 1.0F, 32F), PartPose.ZERO);
		idler_l.addOrReplaceChild("t20", CubeListBuilder.create().texOffs(253, 1586).addBox(40F, 4F, -15F, 72F, 1.0F, 30F), PartPose.ZERO);
		idler_l.addOrReplaceChild("t21", CubeListBuilder.create().texOffs(253, 1586).addBox(40F, 5F, -15F, 72F, 1.0F, 30F), PartPose.ZERO);
		idler_l.addOrReplaceChild("t22", CubeListBuilder.create().texOffs(253, 1586).addBox(40F, 6F, -15F, 72F, 1.0F, 30F), PartPose.ZERO);
		idler_l.addOrReplaceChild("t23", CubeListBuilder.create().texOffs(253, 1586).addBox(40F, 7F, -14F, 72F, 1.0F, 28F), PartPose.ZERO);
		idler_l.addOrReplaceChild("t24", CubeListBuilder.create().texOffs(253, 1586).addBox(40F, 8F, -14F, 72F, 1.0F, 28F), PartPose.ZERO);
		idler_l.addOrReplaceChild("t25", CubeListBuilder.create().texOffs(253, 1586).addBox(40F, 9F, -13F, 72F, 1.0F, 26F), PartPose.ZERO);
		idler_l.addOrReplaceChild("t26", CubeListBuilder.create().texOffs(253, 1586).addBox(40F, 10F, -12F, 72F, 1.0F, 24F), PartPose.ZERO);
		idler_l.addOrReplaceChild("t27", CubeListBuilder.create().texOffs(253, 1586).addBox(40F, 11F, -11F, 72F, 1.0F, 22F), PartPose.ZERO);
		idler_l.addOrReplaceChild("t28", CubeListBuilder.create().texOffs(253, 1586).addBox(40F, 12F, -10F, 72F, 1.0F, 20F), PartPose.ZERO);
		idler_l.addOrReplaceChild("t29", CubeListBuilder.create().texOffs(253, 1586).addBox(40F, 13F, -9F, 72F, 1.0F, 18F), PartPose.ZERO);
		idler_l.addOrReplaceChild("t30", CubeListBuilder.create().texOffs(253, 1586).addBox(40F, 14F, -7F, 72F, 1.0F, 14F), PartPose.ZERO);
		idler_l.addOrReplaceChild("t31", CubeListBuilder.create().texOffs(253, 1586).addBox(40F, 15F, -4F, 72F, 1.0F, 8F), PartPose.ZERO);
		idler_l.addOrReplaceChild("h0", CubeListBuilder.create().texOffs(462, 1586).addBox(112F, -7F, -3F, 1.0F, 1.0F, 6F), PartPose.ZERO);
		idler_l.addOrReplaceChild("h1", CubeListBuilder.create().texOffs(462, 1586).addBox(112F, -6F, -5F, 1.0F, 1.0F, 10F), PartPose.ZERO);
		idler_l.addOrReplaceChild("h2", CubeListBuilder.create().texOffs(462, 1586).addBox(112F, -5F, -6F, 1.0F, 1.0F, 12F), PartPose.ZERO);
		idler_l.addOrReplaceChild("h3", CubeListBuilder.create().texOffs(462, 1586).addBox(112F, -4F, -6F, 1.0F, 1.0F, 12F), PartPose.ZERO);
		idler_l.addOrReplaceChild("h4", CubeListBuilder.create().texOffs(462, 1586).addBox(112F, -3F, -7F, 1.0F, 1.0F, 14F), PartPose.ZERO);
		idler_l.addOrReplaceChild("h5", CubeListBuilder.create().texOffs(462, 1586).addBox(112F, -2F, -7F, 1.0F, 1.0F, 14F), PartPose.ZERO);
		idler_l.addOrReplaceChild("h6", CubeListBuilder.create().texOffs(462, 1586).addBox(112F, -1F, -7F, 1.0F, 1.0F, 14F), PartPose.ZERO);
		idler_l.addOrReplaceChild("h7", CubeListBuilder.create().texOffs(462, 1586).addBox(112F, 0F, -7F, 1.0F, 1.0F, 14F), PartPose.ZERO);
		idler_l.addOrReplaceChild("h8", CubeListBuilder.create().texOffs(462, 1586).addBox(112F, 1F, -7F, 1.0F, 1.0F, 14F), PartPose.ZERO);
		idler_l.addOrReplaceChild("h9", CubeListBuilder.create().texOffs(462, 1586).addBox(112F, 2F, -7F, 1.0F, 1.0F, 14F), PartPose.ZERO);
		idler_l.addOrReplaceChild("h10", CubeListBuilder.create().texOffs(462, 1586).addBox(112F, 3F, -6F, 1.0F, 1.0F, 12F), PartPose.ZERO);
		idler_l.addOrReplaceChild("h11", CubeListBuilder.create().texOffs(462, 1586).addBox(112F, 4F, -6F, 1.0F, 1.0F, 12F), PartPose.ZERO);
		idler_l.addOrReplaceChild("h12", CubeListBuilder.create().texOffs(462, 1586).addBox(112F, 5F, -5F, 1.0F, 1.0F, 10F), PartPose.ZERO);
		idler_l.addOrReplaceChild("h13", CubeListBuilder.create().texOffs(462, 1586).addBox(112F, 6F, -3F, 1.0F, 1.0F, 6F), PartPose.ZERO);
		idler_l.addOrReplaceChild("n0", CubeListBuilder.create().texOffs(529, 1586).addBox(113F, -1F, 3F, 1.0F, 2.0F, 2.0F), PartPose.ZERO);
		idler_l.addOrReplaceChild("n1", CubeListBuilder.create().texOffs(529, 1586).addBox(113F, 3F, 1F, 1.0F, 2.0F, 2.0F), PartPose.ZERO);
		idler_l.addOrReplaceChild("n2", CubeListBuilder.create().texOffs(529, 1586).addBox(113F, 3F, -3F, 1.0F, 2.0F, 2.0F), PartPose.ZERO);
		idler_l.addOrReplaceChild("n3", CubeListBuilder.create().texOffs(529, 1586).addBox(113F, -1F, -5F, 1.0F, 2.0F, 2.0F), PartPose.ZERO);
		idler_l.addOrReplaceChild("n4", CubeListBuilder.create().texOffs(529, 1586).addBox(113F, -5F, -3F, 1.0F, 2.0F, 2.0F), PartPose.ZERO);
		idler_l.addOrReplaceChild("n5", CubeListBuilder.create().texOffs(529, 1586).addBox(113F, -5F, 1F, 1.0F, 2.0F, 2.0F), PartPose.ZERO);
		PartDefinition idler_r = root.addOrReplaceChild("idler_r", CubeListBuilder.create(), PartPose.offset(0.0F, -16F, -88F));
		idler_r.addOrReplaceChild("t0", CubeListBuilder.create().texOffs(253, 1586).addBox(-112F, -16F, -4F, 72F, 1.0F, 8F), PartPose.ZERO);
		idler_r.addOrReplaceChild("t1", CubeListBuilder.create().texOffs(253, 1586).addBox(-112F, -15F, -7F, 72F, 1.0F, 14F), PartPose.ZERO);
		idler_r.addOrReplaceChild("t2", CubeListBuilder.create().texOffs(253, 1586).addBox(-112F, -14F, -9F, 72F, 1.0F, 18F), PartPose.ZERO);
		idler_r.addOrReplaceChild("t3", CubeListBuilder.create().texOffs(253, 1586).addBox(-112F, -13F, -10F, 72F, 1.0F, 20F), PartPose.ZERO);
		idler_r.addOrReplaceChild("t4", CubeListBuilder.create().texOffs(253, 1586).addBox(-112F, -12F, -11F, 72F, 1.0F, 22F), PartPose.ZERO);
		idler_r.addOrReplaceChild("t5", CubeListBuilder.create().texOffs(253, 1586).addBox(-112F, -11F, -12F, 72F, 1.0F, 24F), PartPose.ZERO);
		idler_r.addOrReplaceChild("t6", CubeListBuilder.create().texOffs(253, 1586).addBox(-112F, -10F, -13F, 72F, 1.0F, 26F), PartPose.ZERO);
		idler_r.addOrReplaceChild("t7", CubeListBuilder.create().texOffs(253, 1586).addBox(-112F, -9F, -14F, 72F, 1.0F, 28F), PartPose.ZERO);
		idler_r.addOrReplaceChild("t8", CubeListBuilder.create().texOffs(253, 1586).addBox(-112F, -8F, -14F, 72F, 1.0F, 28F), PartPose.ZERO);
		idler_r.addOrReplaceChild("t9", CubeListBuilder.create().texOffs(253, 1586).addBox(-112F, -7F, -15F, 72F, 1.0F, 30F), PartPose.ZERO);
		idler_r.addOrReplaceChild("t10", CubeListBuilder.create().texOffs(253, 1586).addBox(-112F, -6F, -15F, 72F, 1.0F, 30F), PartPose.ZERO);
		idler_r.addOrReplaceChild("t11", CubeListBuilder.create().texOffs(253, 1586).addBox(-112F, -5F, -15F, 72F, 1.0F, 30F), PartPose.ZERO);
		idler_r.addOrReplaceChild("t12", CubeListBuilder.create().texOffs(253, 1586).addBox(-112F, -4F, -16F, 72F, 1.0F, 32F), PartPose.ZERO);
		idler_r.addOrReplaceChild("t13", CubeListBuilder.create().texOffs(253, 1586).addBox(-112F, -3F, -16F, 72F, 1.0F, 32F), PartPose.ZERO);
		idler_r.addOrReplaceChild("t14", CubeListBuilder.create().texOffs(253, 1586).addBox(-112F, -2F, -16F, 72F, 1.0F, 32F), PartPose.ZERO);
		idler_r.addOrReplaceChild("t15", CubeListBuilder.create().texOffs(253, 1586).addBox(-112F, -1F, -16F, 72F, 1.0F, 32F), PartPose.ZERO);
		idler_r.addOrReplaceChild("t16", CubeListBuilder.create().texOffs(253, 1586).addBox(-112F, 0F, -16F, 72F, 1.0F, 32F), PartPose.ZERO);
		idler_r.addOrReplaceChild("t17", CubeListBuilder.create().texOffs(253, 1586).addBox(-112F, 1F, -16F, 72F, 1.0F, 32F), PartPose.ZERO);
		idler_r.addOrReplaceChild("t18", CubeListBuilder.create().texOffs(253, 1586).addBox(-112F, 2F, -16F, 72F, 1.0F, 32F), PartPose.ZERO);
		idler_r.addOrReplaceChild("t19", CubeListBuilder.create().texOffs(253, 1586).addBox(-112F, 3F, -16F, 72F, 1.0F, 32F), PartPose.ZERO);
		idler_r.addOrReplaceChild("t20", CubeListBuilder.create().texOffs(253, 1586).addBox(-112F, 4F, -15F, 72F, 1.0F, 30F), PartPose.ZERO);
		idler_r.addOrReplaceChild("t21", CubeListBuilder.create().texOffs(253, 1586).addBox(-112F, 5F, -15F, 72F, 1.0F, 30F), PartPose.ZERO);
		idler_r.addOrReplaceChild("t22", CubeListBuilder.create().texOffs(253, 1586).addBox(-112F, 6F, -15F, 72F, 1.0F, 30F), PartPose.ZERO);
		idler_r.addOrReplaceChild("t23", CubeListBuilder.create().texOffs(253, 1586).addBox(-112F, 7F, -14F, 72F, 1.0F, 28F), PartPose.ZERO);
		idler_r.addOrReplaceChild("t24", CubeListBuilder.create().texOffs(253, 1586).addBox(-112F, 8F, -14F, 72F, 1.0F, 28F), PartPose.ZERO);
		idler_r.addOrReplaceChild("t25", CubeListBuilder.create().texOffs(253, 1586).addBox(-112F, 9F, -13F, 72F, 1.0F, 26F), PartPose.ZERO);
		idler_r.addOrReplaceChild("t26", CubeListBuilder.create().texOffs(253, 1586).addBox(-112F, 10F, -12F, 72F, 1.0F, 24F), PartPose.ZERO);
		idler_r.addOrReplaceChild("t27", CubeListBuilder.create().texOffs(253, 1586).addBox(-112F, 11F, -11F, 72F, 1.0F, 22F), PartPose.ZERO);
		idler_r.addOrReplaceChild("t28", CubeListBuilder.create().texOffs(253, 1586).addBox(-112F, 12F, -10F, 72F, 1.0F, 20F), PartPose.ZERO);
		idler_r.addOrReplaceChild("t29", CubeListBuilder.create().texOffs(253, 1586).addBox(-112F, 13F, -9F, 72F, 1.0F, 18F), PartPose.ZERO);
		idler_r.addOrReplaceChild("t30", CubeListBuilder.create().texOffs(253, 1586).addBox(-112F, 14F, -7F, 72F, 1.0F, 14F), PartPose.ZERO);
		idler_r.addOrReplaceChild("t31", CubeListBuilder.create().texOffs(253, 1586).addBox(-112F, 15F, -4F, 72F, 1.0F, 8F), PartPose.ZERO);
		idler_r.addOrReplaceChild("h0", CubeListBuilder.create().texOffs(462, 1586).addBox(-113F, -7F, -3F, 1.0F, 1.0F, 6F), PartPose.ZERO);
		idler_r.addOrReplaceChild("h1", CubeListBuilder.create().texOffs(462, 1586).addBox(-113F, -6F, -5F, 1.0F, 1.0F, 10F), PartPose.ZERO);
		idler_r.addOrReplaceChild("h2", CubeListBuilder.create().texOffs(462, 1586).addBox(-113F, -5F, -6F, 1.0F, 1.0F, 12F), PartPose.ZERO);
		idler_r.addOrReplaceChild("h3", CubeListBuilder.create().texOffs(462, 1586).addBox(-113F, -4F, -6F, 1.0F, 1.0F, 12F), PartPose.ZERO);
		idler_r.addOrReplaceChild("h4", CubeListBuilder.create().texOffs(462, 1586).addBox(-113F, -3F, -7F, 1.0F, 1.0F, 14F), PartPose.ZERO);
		idler_r.addOrReplaceChild("h5", CubeListBuilder.create().texOffs(462, 1586).addBox(-113F, -2F, -7F, 1.0F, 1.0F, 14F), PartPose.ZERO);
		idler_r.addOrReplaceChild("h6", CubeListBuilder.create().texOffs(462, 1586).addBox(-113F, -1F, -7F, 1.0F, 1.0F, 14F), PartPose.ZERO);
		idler_r.addOrReplaceChild("h7", CubeListBuilder.create().texOffs(462, 1586).addBox(-113F, 0F, -7F, 1.0F, 1.0F, 14F), PartPose.ZERO);
		idler_r.addOrReplaceChild("h8", CubeListBuilder.create().texOffs(462, 1586).addBox(-113F, 1F, -7F, 1.0F, 1.0F, 14F), PartPose.ZERO);
		idler_r.addOrReplaceChild("h9", CubeListBuilder.create().texOffs(462, 1586).addBox(-113F, 2F, -7F, 1.0F, 1.0F, 14F), PartPose.ZERO);
		idler_r.addOrReplaceChild("h10", CubeListBuilder.create().texOffs(462, 1586).addBox(-113F, 3F, -6F, 1.0F, 1.0F, 12F), PartPose.ZERO);
		idler_r.addOrReplaceChild("h11", CubeListBuilder.create().texOffs(462, 1586).addBox(-113F, 4F, -6F, 1.0F, 1.0F, 12F), PartPose.ZERO);
		idler_r.addOrReplaceChild("h12", CubeListBuilder.create().texOffs(462, 1586).addBox(-113F, 5F, -5F, 1.0F, 1.0F, 10F), PartPose.ZERO);
		idler_r.addOrReplaceChild("h13", CubeListBuilder.create().texOffs(462, 1586).addBox(-113F, 6F, -3F, 1.0F, 1.0F, 6F), PartPose.ZERO);
		idler_r.addOrReplaceChild("n0", CubeListBuilder.create().texOffs(529, 1586).addBox(-114F, -1F, 3F, 1.0F, 2.0F, 2.0F), PartPose.ZERO);
		idler_r.addOrReplaceChild("n1", CubeListBuilder.create().texOffs(529, 1586).addBox(-114F, 3F, 1F, 1.0F, 2.0F, 2.0F), PartPose.ZERO);
		idler_r.addOrReplaceChild("n2", CubeListBuilder.create().texOffs(529, 1586).addBox(-114F, 3F, -3F, 1.0F, 2.0F, 2.0F), PartPose.ZERO);
		idler_r.addOrReplaceChild("n3", CubeListBuilder.create().texOffs(529, 1586).addBox(-114F, -1F, -5F, 1.0F, 2.0F, 2.0F), PartPose.ZERO);
		idler_r.addOrReplaceChild("n4", CubeListBuilder.create().texOffs(529, 1586).addBox(-114F, -5F, -3F, 1.0F, 2.0F, 2.0F), PartPose.ZERO);
		idler_r.addOrReplaceChild("n5", CubeListBuilder.create().texOffs(529, 1586).addBox(-114F, -5F, 1F, 1.0F, 2.0F, 2.0F), PartPose.ZERO);
		PartDefinition tumbler_l = root.addOrReplaceChild("tumbler_l", CubeListBuilder.create(), PartPose.offset(0.0F, -16F, 88F));
		tumbler_l.addOrReplaceChild("t0", CubeListBuilder.create().texOffs(253, 1586).addBox(40F, -16F, -4F, 72F, 1.0F, 8F), PartPose.ZERO);
		tumbler_l.addOrReplaceChild("t1", CubeListBuilder.create().texOffs(253, 1586).addBox(40F, -15F, -7F, 72F, 1.0F, 14F), PartPose.ZERO);
		tumbler_l.addOrReplaceChild("t2", CubeListBuilder.create().texOffs(253, 1586).addBox(40F, -14F, -9F, 72F, 1.0F, 18F), PartPose.ZERO);
		tumbler_l.addOrReplaceChild("t3", CubeListBuilder.create().texOffs(253, 1586).addBox(40F, -13F, -10F, 72F, 1.0F, 20F), PartPose.ZERO);
		tumbler_l.addOrReplaceChild("t4", CubeListBuilder.create().texOffs(253, 1586).addBox(40F, -12F, -11F, 72F, 1.0F, 22F), PartPose.ZERO);
		tumbler_l.addOrReplaceChild("t5", CubeListBuilder.create().texOffs(253, 1586).addBox(40F, -11F, -12F, 72F, 1.0F, 24F), PartPose.ZERO);
		tumbler_l.addOrReplaceChild("t6", CubeListBuilder.create().texOffs(253, 1586).addBox(40F, -10F, -13F, 72F, 1.0F, 26F), PartPose.ZERO);
		tumbler_l.addOrReplaceChild("t7", CubeListBuilder.create().texOffs(253, 1586).addBox(40F, -9F, -14F, 72F, 1.0F, 28F), PartPose.ZERO);
		tumbler_l.addOrReplaceChild("t8", CubeListBuilder.create().texOffs(253, 1586).addBox(40F, -8F, -14F, 72F, 1.0F, 28F), PartPose.ZERO);
		tumbler_l.addOrReplaceChild("t9", CubeListBuilder.create().texOffs(253, 1586).addBox(40F, -7F, -15F, 72F, 1.0F, 30F), PartPose.ZERO);
		tumbler_l.addOrReplaceChild("t10", CubeListBuilder.create().texOffs(253, 1586).addBox(40F, -6F, -15F, 72F, 1.0F, 30F), PartPose.ZERO);
		tumbler_l.addOrReplaceChild("t11", CubeListBuilder.create().texOffs(253, 1586).addBox(40F, -5F, -15F, 72F, 1.0F, 30F), PartPose.ZERO);
		tumbler_l.addOrReplaceChild("t12", CubeListBuilder.create().texOffs(253, 1586).addBox(40F, -4F, -16F, 72F, 1.0F, 32F), PartPose.ZERO);
		tumbler_l.addOrReplaceChild("t13", CubeListBuilder.create().texOffs(253, 1586).addBox(40F, -3F, -16F, 72F, 1.0F, 32F), PartPose.ZERO);
		tumbler_l.addOrReplaceChild("t14", CubeListBuilder.create().texOffs(253, 1586).addBox(40F, -2F, -16F, 72F, 1.0F, 32F), PartPose.ZERO);
		tumbler_l.addOrReplaceChild("t15", CubeListBuilder.create().texOffs(253, 1586).addBox(40F, -1F, -16F, 72F, 1.0F, 32F), PartPose.ZERO);
		tumbler_l.addOrReplaceChild("t16", CubeListBuilder.create().texOffs(253, 1586).addBox(40F, 0F, -16F, 72F, 1.0F, 32F), PartPose.ZERO);
		tumbler_l.addOrReplaceChild("t17", CubeListBuilder.create().texOffs(253, 1586).addBox(40F, 1F, -16F, 72F, 1.0F, 32F), PartPose.ZERO);
		tumbler_l.addOrReplaceChild("t18", CubeListBuilder.create().texOffs(253, 1586).addBox(40F, 2F, -16F, 72F, 1.0F, 32F), PartPose.ZERO);
		tumbler_l.addOrReplaceChild("t19", CubeListBuilder.create().texOffs(253, 1586).addBox(40F, 3F, -16F, 72F, 1.0F, 32F), PartPose.ZERO);
		tumbler_l.addOrReplaceChild("t20", CubeListBuilder.create().texOffs(253, 1586).addBox(40F, 4F, -15F, 72F, 1.0F, 30F), PartPose.ZERO);
		tumbler_l.addOrReplaceChild("t21", CubeListBuilder.create().texOffs(253, 1586).addBox(40F, 5F, -15F, 72F, 1.0F, 30F), PartPose.ZERO);
		tumbler_l.addOrReplaceChild("t22", CubeListBuilder.create().texOffs(253, 1586).addBox(40F, 6F, -15F, 72F, 1.0F, 30F), PartPose.ZERO);
		tumbler_l.addOrReplaceChild("t23", CubeListBuilder.create().texOffs(253, 1586).addBox(40F, 7F, -14F, 72F, 1.0F, 28F), PartPose.ZERO);
		tumbler_l.addOrReplaceChild("t24", CubeListBuilder.create().texOffs(253, 1586).addBox(40F, 8F, -14F, 72F, 1.0F, 28F), PartPose.ZERO);
		tumbler_l.addOrReplaceChild("t25", CubeListBuilder.create().texOffs(253, 1586).addBox(40F, 9F, -13F, 72F, 1.0F, 26F), PartPose.ZERO);
		tumbler_l.addOrReplaceChild("t26", CubeListBuilder.create().texOffs(253, 1586).addBox(40F, 10F, -12F, 72F, 1.0F, 24F), PartPose.ZERO);
		tumbler_l.addOrReplaceChild("t27", CubeListBuilder.create().texOffs(253, 1586).addBox(40F, 11F, -11F, 72F, 1.0F, 22F), PartPose.ZERO);
		tumbler_l.addOrReplaceChild("t28", CubeListBuilder.create().texOffs(253, 1586).addBox(40F, 12F, -10F, 72F, 1.0F, 20F), PartPose.ZERO);
		tumbler_l.addOrReplaceChild("t29", CubeListBuilder.create().texOffs(253, 1586).addBox(40F, 13F, -9F, 72F, 1.0F, 18F), PartPose.ZERO);
		tumbler_l.addOrReplaceChild("t30", CubeListBuilder.create().texOffs(253, 1586).addBox(40F, 14F, -7F, 72F, 1.0F, 14F), PartPose.ZERO);
		tumbler_l.addOrReplaceChild("t31", CubeListBuilder.create().texOffs(253, 1586).addBox(40F, 15F, -4F, 72F, 1.0F, 8F), PartPose.ZERO);
		tumbler_l.addOrReplaceChild("h0", CubeListBuilder.create().texOffs(462, 1586).addBox(112F, -7F, -3F, 1.0F, 1.0F, 6F), PartPose.ZERO);
		tumbler_l.addOrReplaceChild("h1", CubeListBuilder.create().texOffs(462, 1586).addBox(112F, -6F, -5F, 1.0F, 1.0F, 10F), PartPose.ZERO);
		tumbler_l.addOrReplaceChild("h2", CubeListBuilder.create().texOffs(462, 1586).addBox(112F, -5F, -6F, 1.0F, 1.0F, 12F), PartPose.ZERO);
		tumbler_l.addOrReplaceChild("h3", CubeListBuilder.create().texOffs(462, 1586).addBox(112F, -4F, -6F, 1.0F, 1.0F, 12F), PartPose.ZERO);
		tumbler_l.addOrReplaceChild("h4", CubeListBuilder.create().texOffs(462, 1586).addBox(112F, -3F, -7F, 1.0F, 1.0F, 14F), PartPose.ZERO);
		tumbler_l.addOrReplaceChild("h5", CubeListBuilder.create().texOffs(462, 1586).addBox(112F, -2F, -7F, 1.0F, 1.0F, 14F), PartPose.ZERO);
		tumbler_l.addOrReplaceChild("h6", CubeListBuilder.create().texOffs(462, 1586).addBox(112F, -1F, -7F, 1.0F, 1.0F, 14F), PartPose.ZERO);
		tumbler_l.addOrReplaceChild("h7", CubeListBuilder.create().texOffs(462, 1586).addBox(112F, 0F, -7F, 1.0F, 1.0F, 14F), PartPose.ZERO);
		tumbler_l.addOrReplaceChild("h8", CubeListBuilder.create().texOffs(462, 1586).addBox(112F, 1F, -7F, 1.0F, 1.0F, 14F), PartPose.ZERO);
		tumbler_l.addOrReplaceChild("h9", CubeListBuilder.create().texOffs(462, 1586).addBox(112F, 2F, -7F, 1.0F, 1.0F, 14F), PartPose.ZERO);
		tumbler_l.addOrReplaceChild("h10", CubeListBuilder.create().texOffs(462, 1586).addBox(112F, 3F, -6F, 1.0F, 1.0F, 12F), PartPose.ZERO);
		tumbler_l.addOrReplaceChild("h11", CubeListBuilder.create().texOffs(462, 1586).addBox(112F, 4F, -6F, 1.0F, 1.0F, 12F), PartPose.ZERO);
		tumbler_l.addOrReplaceChild("h12", CubeListBuilder.create().texOffs(462, 1586).addBox(112F, 5F, -5F, 1.0F, 1.0F, 10F), PartPose.ZERO);
		tumbler_l.addOrReplaceChild("h13", CubeListBuilder.create().texOffs(462, 1586).addBox(112F, 6F, -3F, 1.0F, 1.0F, 6F), PartPose.ZERO);
		tumbler_l.addOrReplaceChild("n0", CubeListBuilder.create().texOffs(529, 1586).addBox(113F, -1F, 3F, 1.0F, 2.0F, 2.0F), PartPose.ZERO);
		tumbler_l.addOrReplaceChild("n1", CubeListBuilder.create().texOffs(529, 1586).addBox(113F, 3F, 1F, 1.0F, 2.0F, 2.0F), PartPose.ZERO);
		tumbler_l.addOrReplaceChild("n2", CubeListBuilder.create().texOffs(529, 1586).addBox(113F, 3F, -3F, 1.0F, 2.0F, 2.0F), PartPose.ZERO);
		tumbler_l.addOrReplaceChild("n3", CubeListBuilder.create().texOffs(529, 1586).addBox(113F, -1F, -5F, 1.0F, 2.0F, 2.0F), PartPose.ZERO);
		tumbler_l.addOrReplaceChild("n4", CubeListBuilder.create().texOffs(529, 1586).addBox(113F, -5F, -3F, 1.0F, 2.0F, 2.0F), PartPose.ZERO);
		tumbler_l.addOrReplaceChild("n5", CubeListBuilder.create().texOffs(529, 1586).addBox(113F, -5F, 1F, 1.0F, 2.0F, 2.0F), PartPose.ZERO);
		PartDefinition tumbler_r = root.addOrReplaceChild("tumbler_r", CubeListBuilder.create(), PartPose.offset(0.0F, -16F, 88F));
		tumbler_r.addOrReplaceChild("t0", CubeListBuilder.create().texOffs(253, 1586).addBox(-112F, -16F, -4F, 72F, 1.0F, 8F), PartPose.ZERO);
		tumbler_r.addOrReplaceChild("t1", CubeListBuilder.create().texOffs(253, 1586).addBox(-112F, -15F, -7F, 72F, 1.0F, 14F), PartPose.ZERO);
		tumbler_r.addOrReplaceChild("t2", CubeListBuilder.create().texOffs(253, 1586).addBox(-112F, -14F, -9F, 72F, 1.0F, 18F), PartPose.ZERO);
		tumbler_r.addOrReplaceChild("t3", CubeListBuilder.create().texOffs(253, 1586).addBox(-112F, -13F, -10F, 72F, 1.0F, 20F), PartPose.ZERO);
		tumbler_r.addOrReplaceChild("t4", CubeListBuilder.create().texOffs(253, 1586).addBox(-112F, -12F, -11F, 72F, 1.0F, 22F), PartPose.ZERO);
		tumbler_r.addOrReplaceChild("t5", CubeListBuilder.create().texOffs(253, 1586).addBox(-112F, -11F, -12F, 72F, 1.0F, 24F), PartPose.ZERO);
		tumbler_r.addOrReplaceChild("t6", CubeListBuilder.create().texOffs(253, 1586).addBox(-112F, -10F, -13F, 72F, 1.0F, 26F), PartPose.ZERO);
		tumbler_r.addOrReplaceChild("t7", CubeListBuilder.create().texOffs(253, 1586).addBox(-112F, -9F, -14F, 72F, 1.0F, 28F), PartPose.ZERO);
		tumbler_r.addOrReplaceChild("t8", CubeListBuilder.create().texOffs(253, 1586).addBox(-112F, -8F, -14F, 72F, 1.0F, 28F), PartPose.ZERO);
		tumbler_r.addOrReplaceChild("t9", CubeListBuilder.create().texOffs(253, 1586).addBox(-112F, -7F, -15F, 72F, 1.0F, 30F), PartPose.ZERO);
		tumbler_r.addOrReplaceChild("t10", CubeListBuilder.create().texOffs(253, 1586).addBox(-112F, -6F, -15F, 72F, 1.0F, 30F), PartPose.ZERO);
		tumbler_r.addOrReplaceChild("t11", CubeListBuilder.create().texOffs(253, 1586).addBox(-112F, -5F, -15F, 72F, 1.0F, 30F), PartPose.ZERO);
		tumbler_r.addOrReplaceChild("t12", CubeListBuilder.create().texOffs(253, 1586).addBox(-112F, -4F, -16F, 72F, 1.0F, 32F), PartPose.ZERO);
		tumbler_r.addOrReplaceChild("t13", CubeListBuilder.create().texOffs(253, 1586).addBox(-112F, -3F, -16F, 72F, 1.0F, 32F), PartPose.ZERO);
		tumbler_r.addOrReplaceChild("t14", CubeListBuilder.create().texOffs(253, 1586).addBox(-112F, -2F, -16F, 72F, 1.0F, 32F), PartPose.ZERO);
		tumbler_r.addOrReplaceChild("t15", CubeListBuilder.create().texOffs(253, 1586).addBox(-112F, -1F, -16F, 72F, 1.0F, 32F), PartPose.ZERO);
		tumbler_r.addOrReplaceChild("t16", CubeListBuilder.create().texOffs(253, 1586).addBox(-112F, 0F, -16F, 72F, 1.0F, 32F), PartPose.ZERO);
		tumbler_r.addOrReplaceChild("t17", CubeListBuilder.create().texOffs(253, 1586).addBox(-112F, 1F, -16F, 72F, 1.0F, 32F), PartPose.ZERO);
		tumbler_r.addOrReplaceChild("t18", CubeListBuilder.create().texOffs(253, 1586).addBox(-112F, 2F, -16F, 72F, 1.0F, 32F), PartPose.ZERO);
		tumbler_r.addOrReplaceChild("t19", CubeListBuilder.create().texOffs(253, 1586).addBox(-112F, 3F, -16F, 72F, 1.0F, 32F), PartPose.ZERO);
		tumbler_r.addOrReplaceChild("t20", CubeListBuilder.create().texOffs(253, 1586).addBox(-112F, 4F, -15F, 72F, 1.0F, 30F), PartPose.ZERO);
		tumbler_r.addOrReplaceChild("t21", CubeListBuilder.create().texOffs(253, 1586).addBox(-112F, 5F, -15F, 72F, 1.0F, 30F), PartPose.ZERO);
		tumbler_r.addOrReplaceChild("t22", CubeListBuilder.create().texOffs(253, 1586).addBox(-112F, 6F, -15F, 72F, 1.0F, 30F), PartPose.ZERO);
		tumbler_r.addOrReplaceChild("t23", CubeListBuilder.create().texOffs(253, 1586).addBox(-112F, 7F, -14F, 72F, 1.0F, 28F), PartPose.ZERO);
		tumbler_r.addOrReplaceChild("t24", CubeListBuilder.create().texOffs(253, 1586).addBox(-112F, 8F, -14F, 72F, 1.0F, 28F), PartPose.ZERO);
		tumbler_r.addOrReplaceChild("t25", CubeListBuilder.create().texOffs(253, 1586).addBox(-112F, 9F, -13F, 72F, 1.0F, 26F), PartPose.ZERO);
		tumbler_r.addOrReplaceChild("t26", CubeListBuilder.create().texOffs(253, 1586).addBox(-112F, 10F, -12F, 72F, 1.0F, 24F), PartPose.ZERO);
		tumbler_r.addOrReplaceChild("t27", CubeListBuilder.create().texOffs(253, 1586).addBox(-112F, 11F, -11F, 72F, 1.0F, 22F), PartPose.ZERO);
		tumbler_r.addOrReplaceChild("t28", CubeListBuilder.create().texOffs(253, 1586).addBox(-112F, 12F, -10F, 72F, 1.0F, 20F), PartPose.ZERO);
		tumbler_r.addOrReplaceChild("t29", CubeListBuilder.create().texOffs(253, 1586).addBox(-112F, 13F, -9F, 72F, 1.0F, 18F), PartPose.ZERO);
		tumbler_r.addOrReplaceChild("t30", CubeListBuilder.create().texOffs(253, 1586).addBox(-112F, 14F, -7F, 72F, 1.0F, 14F), PartPose.ZERO);
		tumbler_r.addOrReplaceChild("t31", CubeListBuilder.create().texOffs(253, 1586).addBox(-112F, 15F, -4F, 72F, 1.0F, 8F), PartPose.ZERO);
		tumbler_r.addOrReplaceChild("h0", CubeListBuilder.create().texOffs(462, 1586).addBox(-113F, -7F, -3F, 1.0F, 1.0F, 6F), PartPose.ZERO);
		tumbler_r.addOrReplaceChild("h1", CubeListBuilder.create().texOffs(462, 1586).addBox(-113F, -6F, -5F, 1.0F, 1.0F, 10F), PartPose.ZERO);
		tumbler_r.addOrReplaceChild("h2", CubeListBuilder.create().texOffs(462, 1586).addBox(-113F, -5F, -6F, 1.0F, 1.0F, 12F), PartPose.ZERO);
		tumbler_r.addOrReplaceChild("h3", CubeListBuilder.create().texOffs(462, 1586).addBox(-113F, -4F, -6F, 1.0F, 1.0F, 12F), PartPose.ZERO);
		tumbler_r.addOrReplaceChild("h4", CubeListBuilder.create().texOffs(462, 1586).addBox(-113F, -3F, -7F, 1.0F, 1.0F, 14F), PartPose.ZERO);
		tumbler_r.addOrReplaceChild("h5", CubeListBuilder.create().texOffs(462, 1586).addBox(-113F, -2F, -7F, 1.0F, 1.0F, 14F), PartPose.ZERO);
		tumbler_r.addOrReplaceChild("h6", CubeListBuilder.create().texOffs(462, 1586).addBox(-113F, -1F, -7F, 1.0F, 1.0F, 14F), PartPose.ZERO);
		tumbler_r.addOrReplaceChild("h7", CubeListBuilder.create().texOffs(462, 1586).addBox(-113F, 0F, -7F, 1.0F, 1.0F, 14F), PartPose.ZERO);
		tumbler_r.addOrReplaceChild("h8", CubeListBuilder.create().texOffs(462, 1586).addBox(-113F, 1F, -7F, 1.0F, 1.0F, 14F), PartPose.ZERO);
		tumbler_r.addOrReplaceChild("h9", CubeListBuilder.create().texOffs(462, 1586).addBox(-113F, 2F, -7F, 1.0F, 1.0F, 14F), PartPose.ZERO);
		tumbler_r.addOrReplaceChild("h10", CubeListBuilder.create().texOffs(462, 1586).addBox(-113F, 3F, -6F, 1.0F, 1.0F, 12F), PartPose.ZERO);
		tumbler_r.addOrReplaceChild("h11", CubeListBuilder.create().texOffs(462, 1586).addBox(-113F, 4F, -6F, 1.0F, 1.0F, 12F), PartPose.ZERO);
		tumbler_r.addOrReplaceChild("h12", CubeListBuilder.create().texOffs(462, 1586).addBox(-113F, 5F, -5F, 1.0F, 1.0F, 10F), PartPose.ZERO);
		tumbler_r.addOrReplaceChild("h13", CubeListBuilder.create().texOffs(462, 1586).addBox(-113F, 6F, -3F, 1.0F, 1.0F, 6F), PartPose.ZERO);
		tumbler_r.addOrReplaceChild("n0", CubeListBuilder.create().texOffs(529, 1586).addBox(-114F, -1F, 3F, 1.0F, 2.0F, 2.0F), PartPose.ZERO);
		tumbler_r.addOrReplaceChild("n1", CubeListBuilder.create().texOffs(529, 1586).addBox(-114F, 3F, 1F, 1.0F, 2.0F, 2.0F), PartPose.ZERO);
		tumbler_r.addOrReplaceChild("n2", CubeListBuilder.create().texOffs(529, 1586).addBox(-114F, 3F, -3F, 1.0F, 2.0F, 2.0F), PartPose.ZERO);
		tumbler_r.addOrReplaceChild("n3", CubeListBuilder.create().texOffs(529, 1586).addBox(-114F, -1F, -5F, 1.0F, 2.0F, 2.0F), PartPose.ZERO);
		tumbler_r.addOrReplaceChild("n4", CubeListBuilder.create().texOffs(529, 1586).addBox(-114F, -5F, -3F, 1.0F, 2.0F, 2.0F), PartPose.ZERO);
		tumbler_r.addOrReplaceChild("n5", CubeListBuilder.create().texOffs(529, 1586).addBox(-114F, -5F, 1F, 1.0F, 2.0F, 2.0F), PartPose.ZERO);
		return LayerDefinition.create(mesh, 2048, 2048);
	}

	/** The boom's angle up from level, radians. */
	public static final float BOOM_ANGLE = (float) Math.toRadians(45);

	/**
	 * The saddle block on the boom, where the handle runs through it: metres out from its centre and up from the ground. The boom is
	 * fixed in service, held by its suspension ropes, as a real shovel's: what moves is the handle, crowded out and retracted through
	 * the saddle and pivoting on it while the hoist ropes lift the dipper.
	 */
	public static final float SADDLE_OUT = 12.86F, SADDLE_UP = 11.36F;
	/** The handle is drawn at 0.6 of its modelled length; the dipper rides in by the rest, 4.8 m. */
	public static final float HANDLE_SCALE = 0.6F, IN_BY = 192F * (1 - HANDLE_SCALE) / 16F;
	/** The teeth, out along the handle from the saddle and below its line, and the hoist ropes' bail, at no crowd. */
	public static final float TEETH_ALONG = 16.74F - IN_BY, TEETH_BELOW = 2.56F;
	public static final float BAIL_ALONG = 14.2F - IN_BY, BAIL_UP = 2.2F;
	/** Where its teeth go through a pass (out, up): the toe of the face, the top of the face, over the truck (RopeShovelEntity.teethUp cuts by these heights). */
	private static final float[] TOE = {16.5F, 0.8F}, TOP = {22.0F, 13.0F}, OVER_TRUCK = {21.0F, 11.5F};

	private static float ease(float t) {
		t = Math.max(0F, Math.min(1F, t));
		return t * t * (3 - 2 * t);
	}

	/**
	 * The handle's pose for a point in the pass ({@code cycle}: 0 to 1 the dig, 1 to 2 the swing, dump and back): its angle down from
	 * level (radians), its crowd out through the saddle (metres; less than nothing is retracted), and the door, 0 shut to 1 open. The
	 * teeth go up the face from its toe to its top through the dig, the handle crowding out as they rise; the dipper swings out held
	 * high, its door trips over the truck and the load falls out, and it comes back down to the toe as the door swings shut.
	 */
	public static float[] pose(float cycle) {
		float out, up, door = 0;
		if (cycle <= 1) {
			float e = ease(cycle);
			out = TOE[0] + (TOP[0] - TOE[0]) * (float) Math.pow(e, 1.4);
			up = TOE[1] + (TOP[1] - TOE[1]) * e;
		} else {
			float s = cycle - 1;
			if (s < 0.4F) {
				float e = ease(s / 0.4F);
				out = TOP[0] + (OVER_TRUCK[0] - TOP[0]) * e;
				up = TOP[1] + (OVER_TRUCK[1] - TOP[1]) * e;
			} else if (s < 0.6F) {
				out = OVER_TRUCK[0];
				up = OVER_TRUCK[1];
			} else {
				float e = ease((s - 0.6F) / 0.4F);
				out = OVER_TRUCK[0] + (TOE[0] - OVER_TRUCK[0]) * e;
				up = OVER_TRUCK[1] + (TOE[1] - OVER_TRUCK[1]) * e;
			}
			// the door trips at the top of the swing and swings shut as the dipper comes back down
			door = s < 0.42F ? 0 : s < 0.47F ? (s - 0.42F) / 0.05F : s < 0.65F ? 1 : Math.max(0, 1 - (s - 0.65F) / 0.25F);
		}
		double f = out - SADDLE_OUT, dn = SADDLE_UP - up;
		double along = Math.sqrt(Math.max(1, f * f + dn * dn - TEETH_BELOW * TEETH_BELOW));
		float angle = (float) (Math.atan2(dn, f) - Math.atan2(TEETH_BELOW, along));
		return new float[] {angle, (float) (along - TEETH_ALONG), door};
	}

	/** Set when the frame is drawn, from this machine's own state. */
	@Override
	public void setupAnim(PitVehicleRenderer.PitState state) {
		super.setupAnim(state);
		for (String wheel : WHEELS) {
			root().getChild(wheel).xRot = state.roll / WHEEL_RADIUS;   // turned through the distance rolled over the radius
		}
		ModelPart house = root().getChild(HOUSE);
		house.yRot = (float) Math.toRadians(state.swing);   // plus swings its front to its right
		house.getChild(BOOM).xRot = -BOOM_ANGLE;   // the boom stands at 45 degrees
		ModelPart dipper = house.getChild(DIPPER);
		float[] pose = pose(state.cycle);
		dipper.xRot = pose[0];   // the handle pivots on the saddle
		float crowd = pose[1] * 16F;
		ModelPart handle = dipper.getChild("handle");
		handle.zScale = HANDLE_SCALE;
		handle.z -= crowd;   // the handle slides out along itself through the saddle
		float in = IN_BY * 16F - crowd;
		for (String part : new String[] {"dipper", "teeth", "dipper_load", "door"}) {
			dipper.getChild(part).z += in;
		}
		// the door: hinged at its inner edge under the dipper, its outer end dropping open
		ModelPart door = dipper.getChild("door");
		float t = pose[2] * 1.2F, hy = 38F, hz = -192F;
		door.xRot = t;
		door.y += hy - (hy * (float) Math.cos(t) - hz * (float) Math.sin(t));
		door.z += hz - (hy * (float) Math.sin(t) + hz * (float) Math.cos(t));
		dipper.getChild("dipper_load").visible = state.loaded && pose[2] < 0.05F;
	}
}
