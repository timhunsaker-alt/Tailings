"""Shared by Tailings' part models: Wildcatter's materials and two of its small helpers, drawn the Wildcatter way.

Every part is drawn facing north in its own frame (x east, y up, z south), its main cell at the north-west bottom corner, so the
footprint is W x H x L = x x y x z. Every motor box is on the west face of the main cell (x = 0), as the specs say.
"""
import mlib
import plantlib

TEX = dict(plantlib.TEX)


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
