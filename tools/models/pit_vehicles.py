#!/usr/bin/env python3
"""The open pit's haul truck and wheel loader at their real size (docs/multiblocks/open-pit.md): paints their skins and writes
their model classes, client/HaulTruckModel.java and client/WheelLoaderModel.java.

Boxes are given in metres in the machine's own frame: x across (plus is its right), y up from the ground, z along (minus is the
front). They become entity-model pixels at 16 to the metre: model x is minus x (the renderer mirrors it back), model y is minus y.
A box in a group hangs from that group's hinge, so the renderer can turn it: the truck's body tips about its rear hinge, the loader's
arms lift about their pins. Skins are 1024 x 1024 (a 10 m body is 160 px long, too big for the mine car's 256 sheet).

Wheels are as round as the pixel grid allows: each tyre is a stack of 1/16 m slices, each as long as the circle is wide at its
height, with a steel hub and six wheel nuts on its outer face. Each wheel is its own group on its axle, so the renderer rolls it.

Skins are painted in Ludo's materials (tools/models/skin.py): run python3 tools/process_art.py first.

Run from the repo root: python3 tools/models/pit_vehicles.py
"""
import math
import random

from PIL import Image

from skin import sample

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
# each colour's Ludo material (tools/models/skin.py); glass is see-through, so it has none
MATERIAL = {YELLOW: "paint_yellow", BLACK: "rubber_tread", DARK: "steel_dark", STEEL: "steel_worn", ROCK: "rock", RUST: "steel_rust"}

# name, group, (x0, x1), (y0, y1), (z0, z1), colour, pattern
TRUCK_HINGE = (0.0, 2.0, 4.6)
TRUCK = [
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

# wheels: name, (x0, x1) across, centre height, centre along, radius, the side its hub faces (-1 left, 1 right, 0 an inner dual)
TRUCK_WHEELS = [
	("wheel_fl", (-3.1, -2.3), 1.35, -2.85, 1.35, -1),
	("wheel_fr", (2.3, 3.1), 1.35, -2.85, 1.35, 1),
	("wheel_rlo", (-3.1, -2.3), 1.35, 2.35, 1.35, -1),
	("wheel_rli", (-2.25, -1.45), 1.35, 2.35, 1.35, 0),
	("wheel_rri", (1.45, 2.25), 1.35, 2.35, 1.35, 0),
	("wheel_rro", (2.3, 3.1), 1.35, 2.35, 1.35, 1),
]

LOADER_HINGE = (0.0, 3.0, -3.8)
LOADER = [
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


LOADER_WHEELS = [
	("wheel_fl", (-2.25, -1.25), 1.5, -2.5, 1.5, -1),
	("wheel_fr", (1.25, 2.25), 1.5, -2.5, 1.5, 1),
	("wheel_rl", (-2.25, -1.25), 1.5, 3.0, 1.5, -1),
	("wheel_rr", (1.25, 2.25), 1.5, 3.0, 1.5, 1),
]

# The track dozer (open-pit-p4-dry-stack.md): 8.8 x 4.9 x 4.6 m with its semi-U blade. It runs on tracks: each side a tread over an
# idler at the front and a sprocket at the rear (round, turned as it rolls), with its rollers in the track frame between them.
DOZER_HINGE = (0.0, 0.8, -1.0)
DOZER = [
	("track_frame_l", None, (-2.0, -1.3), (0.3, 1.2), (-2.6, 2.6), DARK, "plate"),
	("track_frame_r", None, (1.3, 2.0), (0.3, 1.2), (-2.6, 2.6), DARK, "plate"),
	("tread_top_l", None, (-2.1, -1.2), (1.2, 1.32), (-2.6, 2.6), BLACK, "tread"),
	("tread_top_r", None, (1.2, 2.1), (1.2, 1.32), (-2.6, 2.6), BLACK, "tread"),
	("tread_low_l", None, (-2.1, -1.2), (0.0, 0.14), (-2.6, 2.6), BLACK, "tread"),
	("tread_low_r", None, (1.2, 2.1), (0.0, 0.14), (-2.6, 2.6), BLACK, "tread"),
	("rollers_l", None, (-1.95, -1.35), (0.14, 0.4), (-1.8, 1.8), STEEL, "plate"),
	("rollers_r", None, (1.35, 1.95), (0.14, 0.4), (-1.8, 1.8), STEEL, "plate"),
	("fender_l", None, (-2.4, -1.2), (1.35, 1.5), (-2.9, 2.9), YELLOW, "plate"),
	("fender_r", None, (1.2, 2.4), (1.35, 1.5), (-2.9, 2.9), YELLOW, "plate"),
	("mainframe", None, (-1.2, 1.2), (0.6, 1.4), (-3.4, 3.0), DARK, "plate"),
	("hood", None, (-1.0, 1.0), (1.4, 2.8), (-3.6, -0.6), YELLOW, "louvres"),
	("grille", None, (-0.9, 0.9), (1.5, 2.7), (-3.7, -3.6), DARK, "louvres"),
	("stack", None, (-0.6, -0.4), (2.8, 3.6), (-2.6, -2.4), DARK, "flat"),
	("tank", None, (-1.3, 1.3), (1.4, 2.6), (1.0, 3.0), YELLOW, "plate"),
	("cab", None, (-1.1, 1.1), (1.6, 4.6), (-0.6, 1.0), YELLOW, "cab"),
	("drawbar", None, (-1.0, 1.0), (0.8, 1.6), (3.0, 3.6), DARK, "hazard"),
	("arm_l", "blade", (-2.35, -2.05), (0.6, 1.0), (-3.9, -1.0), DARK, "plate"),
	("arm_r", "blade", (2.05, 2.35), (0.6, 1.0), (-3.9, -1.0), DARK, "plate"),
	("tilt_ram", "blade", (-0.2, 0.2), (1.2, 1.6), (-3.9, -3.0), STEEL, "flat"),
	("blade", "blade", (-2.45, 2.45), (0.15, 2.1), (-4.4, -3.95), YELLOW, "ribs"),
	("edge", "blade", (-2.45, 2.45), (0.0, 0.15), (-4.45, -3.95), STEEL, "plate"),
	("blade_load", "blade", (-2.2, 2.2), (0.2, 1.6), (-5.4, -4.45), ROCK, "rock"),
]

# its idlers (front) and sprockets (rear), the round ends its treads run round
DOZER_WHEELS = [
	("idler_l", (-2.1, -1.2), 0.66, -2.6, 0.66, -1),
	("idler_r", (1.2, 2.1), 0.66, -2.6, 0.66, 1),
	("sprocket_l", (-2.1, -1.2), 0.66, 2.6, 0.66, -1),
	("sprocket_r", (1.2, 2.1), 0.66, 2.6, 0.66, 1),
]


def disc(radius_px):
	"""A disc's pixel rows about its axle: (the row's top, its half-length) for each row, as round as whole pixels allow."""
	rows = []
	n = int(math.ceil(radius_px))
	for j in range(-n, n):
		mid = j + 0.5
		if abs(mid) < radius_px:
			half = int(round((radius_px * radius_px - mid * mid) ** 0.5))
			if half > 0:
				rows.append((j, half))
	return rows


def face(img, x, y, w, h, colour, pattern, rng, k=1.0):
	"""One face of a box, top left at (x, y) on the skin: its colour's material, shaded by k, with the pattern's details."""
	mat = MATERIAL[colour]
	for j in range(h):
		for i in range(w):
			X, Y = x + i, y + j
			c = sample(mat, X, Y, k)
			edge = i == 0 or j == 0 or i == w - 1 or j == h - 1
			if pattern == "plate" and edge and w > 2 and h > 2:
				c = sample(mat, X, Y, k * 0.78)
			elif pattern == "hazard" and (i + j) % 8 < 4:
				c = sample("steel_dark", X, Y, 0.55)
			elif pattern == "ribs" and i % 16 == 7:
				c = sample(mat, X, Y, k * 1.18)
			elif pattern == "ribs" and rng.random() < 0.04:
				c = sample("steel_rust", X, Y, k)
			elif pattern == "tread" and (j % 6 < 2):
				c = sample(mat, X, Y, k * 1.6)
			elif pattern == "louvres" and j % 4 == 1 and 1 < i < w - 2 and 1 < j < h - 2:
				c = sample("steel_dark", X, Y, 0.35)
			elif pattern == "cab" and h > 8 and 0.15 * h < j < 0.55 * h and 2 < i < w - 3:
				c = None
			# the windows are clear: the cutout drops these pixels, so from the seat the driver sees out
			img.putpixel((X, Y), GLASS + (0,) if c is None else c + (255,))


def px(v):
	return int(round(v * 16))


def paint(boxes, name, hinge, cls, doc, groups, anim, wheels=()):
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
		face(img, u + sz, v, sx, sz, colour, pattern if pattern in ("plate", "rock") else "flat", rng, 1.08)
		face(img, u + sz + sx, v, sx, sz, colour, "flat", rng, 0.7)
		face(img, u, v + sz, sz, sy, colour, pattern, rng, 0.88)
		face(img, u + sz, v + sz, sx, sy, colour, pattern, rng)
		face(img, u + sz + sx, v + sz, sz, sy, colour, pattern, rng, 0.88)
		face(img, u + sz + sx + sz, v + sz, sx, sy, colour, pattern, rng, 0.92)
		if group:
			hx, hy, hz = px(-hinge[0]), px(-hinge[1]), px(hinge[2])
			group_lines[group].append(f'\t\t{group}.addOrReplaceChild("{part}", CubeListBuilder.create().texOffs({u}, {v})'
							f'.addBox({mx0 - hx}F, {my0 - hy}F, {mz0 - hz}F, {sx}F, {sy}F, {sz}F), PartPose.ZERO);')
		else:
			root_lines.append(f'\t\troot.addOrReplaceChild("{part}", CubeListBuilder.create().texOffs({u}, {v})'
							f'.addBox({mx0}F, {my0}F, {mz0}F, {sx}F, {sy}F, {sz}F), PartPose.ZERO);')
		u += w + 1
		row = max(row, h)
	# the wheels: a tread region, a hub region and a nut region, each shared by every slice (a slice uses the corner it needs)
	wheel_lines = []
	if wheels:
		biggest = max(px(w[4]) for w in wheels)
		width = max(px(w[1][1] - w[1][0]) for w in wheels)
		regions = {}
		for key, (sx, sy, sz), colour, pattern in (("tread", (width, 1, 2 * biggest), BLACK, "flat"), ("hub", (1, 1, 2 * biggest), STEEL, "flat"),
				("nut", (1, 2, 2), DARK, "flat")):
			w, h = 2 * (sx + sz), sy + sz
			if u + w > SHEET:
				u, v, row = 0, v + row + 1, 0
			# the whole rectangle, solid: a smaller slice lays its faces over other parts of it, which must not be see-through
			face(img, u, v, w, h, colour, "flat", rng, 0.9)
			for fx, fy, fw, fh in ((u + sz, v, sx, sz), (u + sz + sx, v, sx, sz), (u, v + sz, sz, sy), (u + sz, v + sz, sx, sy),
					(u + sz + sx, v + sz, sz, sy), (u + sz + sx + sz, v + sz, sx, sy)):
				face(img, fx, fy, fw, fh, colour, pattern, rng)
			# the tread's lugs across its rolling face, so a turning wheel shows it
			if key == "tread":
				for j in range(0, 2 * biggest, 5):
					for i in range(sx):
						img.putpixel((u + sz + i, v + j), sample(MATERIAL[BLACK], u + sz + i, v + j, 1.9) + (255,))
						img.putpixel((u + sz + i, v + j + 1), sample(MATERIAL[BLACK], u + sz + i, v + j + 1, 1.9) + (255,))
			regions[key] = (u, v)
			u += w + 1
			row = max(row, h)
		for wname, (x0, x1), cy, cz, r, hub_side in wheels:
			wheel_lines.append(f'\t\tPartDefinition {wname} = root.addOrReplaceChild("{wname}", CubeListBuilder.create(), PartPose.offset(0.0F, {-px(cy)}F, {px(cz)}F));')
			mx0, sx = px(-x1), px(x1 - x0)
			tu, tv = regions["tread"]
			for k, (top, half) in enumerate(disc(r * 16)):
				wheel_lines.append(f'\t\t{wname}.addOrReplaceChild("t{k}", CubeListBuilder.create().texOffs({tu}, {tv})'
						f'.addBox({mx0}F, {top}F, {-half}F, {sx}F, 1.0F, {2 * half}F), PartPose.ZERO);')
			if hub_side:
				# its outer face in model x, which is mirrored: a right-hand wheel's outer face is the low model x
				hx = mx0 - 1 if hub_side > 0 else mx0 + sx
				hu, hv = regions["hub"]
				for k, (top, half) in enumerate(disc(r * 16 * 0.45)):
					wheel_lines.append(f'\t\t{wname}.addOrReplaceChild("h{k}", CubeListBuilder.create().texOffs({hu}, {hv})'
							f'.addBox({hx}F, {top}F, {-half}F, 1.0F, 1.0F, {2 * half}F), PartPose.ZERO);')
				nu, nv = regions["nut"]
				nx = hx - 1 if hub_side > 0 else hx + 1
				for k in range(6):
					a = k * math.pi / 3
					ny, nz = int(round(r * 16 * 0.28 * math.sin(a))) - 1, int(round(r * 16 * 0.28 * math.cos(a))) - 1
					wheel_lines.append(f'\t\t{wname}.addOrReplaceChild("n{k}", CubeListBuilder.create().texOffs({nu}, {nv})'
							f'.addBox({nx}F, {ny}F, {nz}F, 1.0F, 2.0F, 2.0F), PartPose.ZERO);')
	img.save(OUT_TEX + name + ".png")   # painted in the Ludo materials (art/final/vehicle)
	hx, hy, hz = px(-hinge[0]), px(-hinge[1]), px(hinge[2])
	body = list(root_lines)
	for g in groups:
		body.append(f'\t\tPartDefinition {g} = root.addOrReplaceChild("{g}", CubeListBuilder.create(), PartPose.offset({hx}F, {hy}F, {hz}F));')
		body.extend(group_lines[g])
	body.extend(wheel_lines)
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

/** {doc} Written by tools/models/pit_vehicles.py, with its {SHEET} x {SHEET} skin painted in Ludo's materials: change the script, not this file. */
public class {cls} extends EntityModel<PitVehicleRenderer.PitState> {{
	public static final ModelLayerLocation LAYER = new ModelLayerLocation(Tailings.id("{name}"), "main");
	/** Its wheels, each turning on its own axle as it rolls, and their radius in metres. */
	public static final String[] WHEELS = {{{", ".join(chr(34) + w[0] + chr(34) for w in wheels)}}};
	public static final float WHEEL_RADIUS = {wheels[0][4] if wheels else 1.0}F;
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
		for (String wheel : WHEELS) {{
			root().getChild(wheel).xRot = state.roll / WHEEL_RADIUS;   // turned through the distance rolled over the radius
		}}
{anim}
	}}
}}
'''
	with open(OUT_JAVA + cls + ".java", "w") as f:
		f.write(java)
	print(f"{cls}: {len(boxes)} boxes, skin rows to v = {v + row}")


def main(names):
	if not names or "haul_truck" in names:
		truck()
	if not names or "wheel_loader" in names:
		loader()
	if not names or "track_dozer" in names:
		dozer()


def dozer():
	paint(DOZER, "track_dozer", DOZER_HINGE, "TrackDozerModel",
		"The 70 t track dozer at its real size, 8.8 x 4.9 x 4.6 m; its blade and push arms rise and fall about their trunnions.", ["blade"],
		"\t\tModelPart blade = root().getChild(BLADE);\n"
		"\t\tblade.y += (0.5F - state.lift) * 24F;   // CUT a block lower than CARRY, RAISED a block and a half higher\n"
		"\t\tblade.getChild(\"blade_load\").visible = state.loaded;", DOZER_WHEELS)


def truck():
	paint(TRUCK, "haul_truck", TRUCK_HINGE, "HaulTruckModel",
		"The 100 t class haul truck at its real size, 10.0 x 6.2 x 5.2 m; its body tips about its rear hinge.", ["body"],
		"\t\tModelPart body = root().getChild(BODY);\n"
		"\t\tbody.xRot = -state.lift * 0.84F;   // the body tips to 48 degrees (proposed)\n"
		"\t\tbody.getChild(\"load\").visible = state.loaded;", TRUCK_WHEELS)


def loader():
	paint(LOADER, "wheel_loader", LOADER_HINGE, "WheelLoaderModel",
		"The wheel loader at its real size, 15 x 4.5 x 6 m; its arms and bucket lift about the boom pins.", ["arm"],
		"\t\tModelPart arm = root().getChild(ARM);\n"
		"\t\tarm.xRot = -state.lift * 0.9F;   // the arms lift the bucket over a truck's side\n"
		"\t\tarm.getChild(\"bucket_load\").visible = state.loaded;", LOADER_WHEELS)


if __name__ == "__main__":
	import sys
	main(sys.argv[1:])
