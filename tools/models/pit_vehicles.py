#!/usr/bin/env python3
"""The open pit's haul truck and wheel loader at their real size (docs/multiblocks/open-pit.md): paints their skins and writes
their model classes, client/HaulTruckModel.java and client/WheelLoaderModel.java.

Boxes are given in metres in the machine's own frame: x across (plus is its right), y up from the ground, z along (minus is the
front). They become entity-model pixels at 16 to the metre: model x is minus x (the renderer mirrors it back), model y is minus y.
A box in a group hangs from that group's hinge, so the renderer can turn it: the truck's body tips about its rear hinge, the loader's
arms lift about their pins. Skins are 1024 x 1024 (a 10 m body is 160 px long, too big for the mine car's 256 sheet).

Run from the repo root: python3 tools/models/pit_vehicles.py
"""
import random

from PIL import Image

OUT_TEX = "common/src/main/resources/assets/tailings/textures/entity/"
OUT_JAVA = "common/src/main/java/dev/wildcatter/tailings/client/"
SHEET = 1024

YELLOW = (214, 166, 34)
BLACK = (30, 30, 32)
DARK = (56, 58, 62)
STEEL = (96, 100, 106)
ROCK = (92, 84, 74)
GLASS = (44, 68, 82)
RUST = (112, 74, 44)

# name, group, (x0, x1), (y0, y1), (z0, z1), colour, pattern
TRUCK_HINGE = (0.0, 2.0, 4.6)
TRUCK = [
	("tyre_fl", None, (-3.1, -2.3), (0.0, 2.7), (-4.2, -1.5), BLACK, "tread"),
	("tyre_fr", None, (2.3, 3.1), (0.0, 2.7), (-4.2, -1.5), BLACK, "tread"),
	("tyre_rlo", None, (-3.1, -2.3), (0.0, 2.7), (1.0, 3.7), BLACK, "tread"),
	("tyre_rli", None, (-2.25, -1.45), (0.0, 2.7), (1.0, 3.7), BLACK, "tread"),
	("tyre_rri", None, (1.45, 2.25), (0.0, 2.7), (1.0, 3.7), BLACK, "tread"),
	("tyre_rro", None, (2.3, 3.1), (0.0, 2.7), (1.0, 3.7), BLACK, "tread"),
	("axle", None, (-1.45, 1.45), (1.1, 1.6), (2.1, 2.6), DARK, "flat"),
	("frame", None, (-0.9, 0.9), (1.3, 2.0), (-4.9, 4.0), DARK, "plate"),
	("hood", None, (-1.1, 1.1), (1.6, 3.0), (-5.0, -2.6), YELLOW, "louvres"),
	("grille", None, (-1.2, 1.2), (1.5, 3.0), (-5.1, -5.0), DARK, "louvres"),
	("bumper", None, (-2.0, 2.0), (1.2, 1.6), (-5.2, -4.9), YELLOW, "hazard"),
	("deck", None, (-3.1, 3.1), (3.0, 3.15), (-5.0, -2.4), DARK, "plate"),
	("cab", None, (-3.0, -1.3), (3.15, 5.0), (-4.6, -2.6), YELLOW, "cab"),
	("floor", "body", (-2.75, 2.75), (2.0, 2.3), (-2.4, 4.9), YELLOW, "plate"),
	("side_l", "body", (-2.75, -2.55), (2.3, 4.6), (-2.4, 4.5), YELLOW, "ribs"),
	("side_r", "body", (2.55, 2.75), (2.3, 4.6), (-2.4, 4.5), YELLOW, "ribs"),
	("front_wall", "body", (-2.75, 2.75), (2.3, 5.0), (-2.6, -2.4), YELLOW, "plate"),
	("canopy", "body", (-3.1, 3.1), (5.0, 5.2), (-5.0, -2.4), YELLOW, "plate"),
	("load", "body", (-2.55, 2.55), (2.3, 4.4), (-2.4, 4.4), ROCK, "rock"),
]

LOADER_HINGE = (0.0, 3.0, -3.8)
LOADER = [
	("tyre_fl", None, (-2.25, -1.25), (0.0, 3.0), (-4.0, -1.0), BLACK, "tread"),
	("tyre_fr", None, (1.25, 2.25), (0.0, 3.0), (-4.0, -1.0), BLACK, "tread"),
	("tyre_rl", None, (-2.25, -1.25), (0.0, 3.0), (1.5, 4.5), BLACK, "tread"),
	("tyre_rr", None, (1.25, 2.25), (0.0, 3.0), (1.5, 4.5), BLACK, "tread"),
	("front_frame", None, (-1.0, 1.0), (1.0, 2.2), (-4.5, 0.2), DARK, "plate"),
	("rear_frame", None, (-1.2, 1.2), (1.0, 3.4), (0.2, 7.0), YELLOW, "louvres"),
	("counterweight", None, (-1.4, 1.4), (1.0, 2.6), (7.0, 7.5), DARK, "hazard"),
	("fender_f", None, (-2.3, 2.3), (3.0, 3.2), (-4.1, -0.9), YELLOW, "plate"),
	("fender_r", None, (-2.3, 2.3), (3.0, 3.2), (1.4, 4.6), YELLOW, "plate"),
	("cab", None, (-1.1, 1.1), (3.4, 6.0), (-0.6, 1.6), YELLOW, "cab"),
	("boom_l", "arm", (-1.2, -0.8), (1.0, 3.2), (-7.0, -3.6), YELLOW, "plate"),
	("boom_r", "arm", (0.8, 1.2), (1.0, 3.2), (-7.0, -3.6), YELLOW, "plate"),
	("bucket", "arm", (-2.25, 2.25), (0.0, 1.8), (-7.5, -6.0), STEEL, "plate"),
	("bucket_load", "arm", (-2.1, 2.1), (1.3, 2.1), (-7.4, -6.1), ROCK, "rock"),
]


def shade(colour, rng, spread=7):
	d = rng.randint(-spread, spread)
	return tuple(max(0, min(255, c + d)) for c in colour)


def scale(colour, k):
	return tuple(max(0, min(255, int(c * k))) for c in colour)


def face(img, x, y, w, h, colour, pattern, rng):
	for j in range(h):
		for i in range(w):
			c = shade(colour, rng, 3)
			edge = i == 0 or j == 0 or i == w - 1 or j == h - 1
			if pattern == "plate" and edge and w > 2 and h > 2:
				c = scale(colour, 0.78)
			elif pattern == "hazard" and (i + j) % 8 < 4:
				c = BLACK
			elif pattern == "ribs" and i % 16 == 7:
				c = scale(colour, 1.18)
			elif pattern == "ribs" and rng.random() < 0.04:
				c = RUST
			elif pattern == "rock":
				c = shade(colour if rng.random() > 0.12 else RUST, rng, 20)
			elif pattern == "tread" and (j % 6 < 2):
				c = scale(colour, 1.6)
			elif pattern == "louvres" and j % 4 == 1 and 1 < i < w - 2 and 1 < j < h - 2:
				c = BLACK
			elif pattern == "cab" and h > 8 and 0.15 * h < j < 0.55 * h and 2 < i < w - 3:
				c = GLASS
			img.putpixel((x + i, y + j), c + (255,))


def px(v):
	return int(round(v * 16))


def paint(boxes, name, hinge, cls, doc, groups, anim):
	rng = random.Random(name)
	img = Image.new("RGBA", (SHEET, SHEET), (0, 0, 0, 0))
	u = v = row = 0
	root_lines, group_lines = [], {g: [] for g in groups}
	for part, group, (x0, x1), (y0, y1), (z0, z1), colour, pattern in boxes:
		# model space: x mirrored, y down
		mx0, my0, mz0 = px(-x1), px(-y1), px(z0)
		sx, sy, sz = px(x1 - x0), px(y1 - y0), px(z1 - z0)
		sx, sy, sz = max(sx, 1), max(sy, 1), max(sz, 1)
		w, h = 2 * (sx + sz), sy + sz
		if u + w > SHEET:
			u, v, row = 0, v + row + 1, 0
		if v + h > SHEET:
			raise SystemExit(f"{name}: the skin is full at {part}")
		face(img, u + sz, v, sx, sz, scale(colour, 1.08), pattern if pattern in ("plate", "rock") else "flat", rng)
		face(img, u + sz + sx, v, sx, sz, scale(colour, 0.7), "flat", rng)
		face(img, u, v + sz, sz, sy, scale(colour, 0.88), pattern, rng)
		face(img, u + sz, v + sz, sx, sy, colour, pattern, rng)
		face(img, u + sz + sx, v + sz, sz, sy, scale(colour, 0.88), pattern, rng)
		face(img, u + sz + sx + sz, v + sz, sx, sy, scale(colour, 0.92), pattern, rng)
		if group:
			hx, hy, hz = px(-hinge[0]), px(-hinge[1]), px(hinge[2])
			group_lines[group].append(f'\t\t{group}.addOrReplaceChild("{part}", CubeListBuilder.create().texOffs({u}, {v})'
							f'.addBox({mx0 - hx}F, {my0 - hy}F, {mz0 - hz}F, {sx}F, {sy}F, {sz}F), PartPose.ZERO);')
		else:
			root_lines.append(f'\t\troot.addOrReplaceChild("{part}", CubeListBuilder.create().texOffs({u}, {v})'
							f'.addBox({mx0}F, {my0}F, {mz0}F, {sx}F, {sy}F, {sz}F), PartPose.ZERO);')
		u += w + 1
		row = max(row, h)
	img.save(OUT_TEX + name + "_placeholder.png")   # drawn by script: a placeholder until the art comes from Ludo
	hx, hy, hz = px(-hinge[0]), px(-hinge[1]), px(hinge[2])
	body = list(root_lines)
	for g in groups:
		body.append(f'\t\tPartDefinition {g} = root.addOrReplaceChild("{g}", CubeListBuilder.create(), PartPose.offset({hx}F, {hy}F, {hz}F));')
		body.extend(group_lines[g])
	constants = "".join(f'\tpublic static final String {g.upper()} = "{g}";\n' for g in groups)
	java = f'''package dev.wildcatter.tailings.client;

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

/** {doc} Written by tools/models/pit_vehicles.py, with its 1024 x 1024 placeholder skin: change the script, not this file. */
public class {cls} extends EntityModel<PitVehicleRenderer.PitState> {{
	public static final ModelLayerLocation LAYER = new ModelLayerLocation(Tailings.id("{name}"), "main");
{constants}
	public {cls}(ModelPart root) {{
		super(root, id -> RenderTypes.entityCutout(id));
	}}

	public static LayerDefinition createBodyLayer() {{
		MeshDefinition mesh = new MeshDefinition();
		PartDefinition root = mesh.getRoot();
{chr(10).join(body)}
		return LayerDefinition.create(mesh, {SHEET}, {SHEET});
	}}

	/** Set when the frame is drawn, from this machine's own state. */
	@Override
	public void setupAnim(PitVehicleRenderer.PitState state) {{
		super.setupAnim(state);
{anim}
	}}
}}
'''
	with open(OUT_JAVA + cls + ".java", "w") as f:
		f.write(java)
	print(f"{cls}: {len(boxes)} boxes, skin rows to v = {v + row}")


if __name__ == "__main__":
	paint(TRUCK, "haul_truck", TRUCK_HINGE, "HaulTruckModel",
		"The 100 t class haul truck at its real size, 10.0 x 6.2 x 5.2 m; its body tips about its rear hinge.", ["body"],
		"\t\tModelPart body = root().getChild(BODY);\n"
		"\t\tbody.xRot = -state.lift * 0.84F;   // the body tips to 48 degrees (proposed)\n"
		"\t\tbody.getChild(\"load\").visible = state.loaded;")
	paint(LOADER, "wheel_loader", LOADER_HINGE, "WheelLoaderModel",
		"The wheel loader at its real size, 15 x 4.5 x 6 m; its arms and bucket lift about the boom pins.", ["arm"],
		"\t\tModelPart arm = root().getChild(ARM);\n"
		"\t\tarm.xRot = -state.lift * 0.9F;   // the arms lift the bucket over a truck's side\n"
		"\t\tarm.getChild(\"bucket_load\").visible = state.loaded;")
