"""Shared by Tailings' part models: Wildcatter's materials and two of its small helpers, drawn the Wildcatter way.

Every part is drawn facing north in its own frame (x east, y up, z south), its main cell at the north-west bottom corner, so the
footprint is W x H x L = x x y x z. Every motor box is on the west face of the main cell (x = 0), as the specs say.
"""
import math

import mlib
import plantlib

TEX = dict(plantlib.TEX)
# a thickener's slurry and the clearer water at its rim, in vanilla's textures (build.py names them in the minecraft namespace);
# pool() adds them to the models that draw them, so no other model's textures change
POOL_TEX = {"slurry": "vanilla/block/mud", "water": "vanilla/block/clay"}


def model(name, w, h, d, particle="grey", hollow=None):
	"""hollow: a part that is a shell, (x1, y1, z1, x2, y2, z2) inclusive, the inside cells left out of its footprint in the box's
	order, as Machine.hollow() leaves them out in the game (the dragline house). Nothing may be drawn there."""
	cells = mlib.footprint_box(w, h, d, 0, 0)
	if hollow:
		x1, y1, z1, x2, y2, z2 = hollow
		cells = [c for c in cells if not (x1 <= c[0] <= x2 and y1 <= c[1] <= y2 and z1 <= c[2] <= z2)]
	return mlib.Model(name, cells, dict(TEX), TEX[particle])


def disc(M, cx, cz, r, y0, y1, tex):
	"""A round lid or liquid surface too wide for one turned slab: an octagon of three flat boxes inside the radius."""
	a, b = r * 0.92, r * 0.38
	M.box((cx - b, y0, cz - a), (cx + b, y1, cz + a), tex)
	M.box((cx - a, y0, cz - b), (cx + a, y1, cz + b), tex)
	M.box((cx - r * 0.65, y0, cz - r * 0.65), (cx + r * 0.65, y1, cz + r * 0.65), tex)


def _chord(r, d):
	return math.sqrt(r * r - d * d) if abs(d) < r else 0.0


def pool(M, cx, cz, r, floor, surface, step=0.125, rim=0.8, floor_tex="concrete"):
	"""A thickener's inside, filling its wall of inside radius r: a solid floor at {@code floor}, and at {@code surface} a round liquid
	surface drawn a row of {@code step} at a time, each row as wide as the circle at its middle: no gap at the wall, and a row's corners
	reach at most step / 2 past r, into the wall when r is the wall's inside face (keep step under the wall's thickness):
	clearer water out to the wall, where the overflow launder takes it, and the muddy slurry over the bed inside {@code rim} of r."""
	M.textures.update(POOL_TEX)
	n = math.ceil(2 * r / step)
	for k in range(n):
		z0 = cz - n * step / 2 + k * step
		z1 = z0 + step
		d = abs((z0 + z1) / 2 - cz)
		h, hs = _chord(r, d), _chord(r * rim, d)
		if h <= 0.02:
			continue
		M.box((cx - h, floor, z0), (cx + h, floor + 0.1, z1), floor_tex)
		if hs > 0.02:
			M.box((cx - hs, surface, z0), (cx + hs, surface + 0.05, z1), "slurry")
			M.box((cx - h, surface, z0), (cx - hs, surface + 0.04, z1), "water")   # the clear water each side of the slurry
			M.box((cx + hs, surface, z0), (cx + h, surface + 0.04, z1), "water")
		else:
			M.box((cx - h, surface, z0), (cx + h, surface + 0.04, z1), "water")


def motor_box(M, y=0.5, z=0.3, h=0.6, w=0.5):
	"""The motor's terminal box on the west face of the main cell, with its warning sign: where the cable lands."""
	M.box((0.0, y, z), (0.3, y + h, z + w), "grey")
	M.decal((-0.01, y + 0.1, z + 0.1), (0.0, y + h - 0.1, z + w - 0.1), "danger", "west", "grey")


def stepped(M, x1, x2, y_start, y_end, z1, z2, steps, thick, tex):
	"""A deck that falls from y_start at z1 to y_end at z2 in `steps` flat steps (a screen deck, a chute)."""
	for i in range(steps):
		za = z1 + (z2 - z1) * i / steps
		zb = z1 + (z2 - z1) * (i + 1) / steps
		y = y_start + (y_end - y_start) * (i + 0.5) / steps
		M.box((x1, y, za), (x2, y + thick, zb), tex)
