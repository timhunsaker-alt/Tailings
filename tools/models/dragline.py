"""The walking dragline's parts (docs/multiblocks/dragline.md, P6), drawn as they stand to be built, each facing north in its own frame
(x east, y up, z south from its main cell's north-west bottom corner), as the rope shovel's parts in pit.py:

- dragline_tub, 15 x 2 x 15: the round base (an octagon of three plates), the roller path and centre pintle on its top that the house
  turns on, and the trailing cable's coupler at its rear with its danger sign.
- dragline_shoe, 3 x 3 x 14: a walking shoe, its pad ribbed underfoot, its side plates and the cam's hub on top.
- dragline_house, 24 x 12 x 20: the revolving frame; the machinery house with its louvres (the hoist, drag and swing motors on their
  generator sets); the cab on its stand at the front left corner; the boom foot's brackets and the drag rope fairlead at the front;
  the counterweight at the rear and the sets' coolers on the roof.
- dragline_mast, 6 x 20 x 4: the A-frame's two legs and back struts, braced, with the head and the suspension ropes' equaliser.
- dragline_boom_section, 4 x 4 x 15: 15 m of the lattice boom, its four chords and the lacing every 1.5 m; five laid in a line from the
  house's front make the 75 m boom, laid on the ground as a boom is before it is raised.
- dragline_bucket, 5 x 4 x 6: the 33 m³ bucket, its mouth and teeth to the south (toward the machine, which drags it in), its arch,
  the hoist chains' spreader bar and its trunnions.
"""
import tlib
from tlib import model, disc

# ---- the tub -----------------------------------------------------------------------------------------------------------------------
T = model("dragline_tub", 15, 2, 15, "steel")
disc(T, 7.5, 7.5, 7.45, 0.0, 1.6, "steel")                                 # the round base
disc(T, 7.5, 7.5, 6.2, 1.6, 2.0, "galv")                                   # the roller path the house turns on
T.box((6.6, 2.0, 6.6), (8.4, 2.6, 8.4), "iron")                            # the centre pintle
for i in range(8):
	# the base's stiffening ribs round its edge
	import math
	a = i * math.pi / 4
	cx, cz = 7.5 + 6.6 * math.sin(a), 7.5 + 6.6 * math.cos(a)
	T.box((cx - 0.3, 0.2, cz - 0.3), (cx + 0.3, 1.7, cz + 0.3), "dark")
T.box((6.5, 0.3, 14.55), (8.5, 1.4, 15.0), "grey")                         # the trailing cable's coupler at its rear
T.decal((6.7, 0.5, 15.0), (8.3, 1.2, 15.01), "danger", "south", "grey")
T.collide((0.4, 0.0, 3.0), (14.6, 1.6, 12.0))
T.collide((3.0, 0.0, 0.4), (12.0, 1.6, 14.6))
T.collide((1.5, 0.0, 1.5), (13.5, 1.6, 13.5))

# ---- a walking shoe ----------------------------------------------------------------------------------------------------------------
S = model("dragline_shoe", 3, 3, 14, "steel")
S.box((0.0, 0.0, 0.3), (3.0, 0.5, 13.7), "steel")                          # the pad
S.box((0.0, 0.3, 0.0), (3.0, 0.7, 0.3), "steel")                           # its ends, turned up
S.box((0.0, 0.3, 13.7), (3.0, 0.7, 14.0), "steel")
for z in range(1, 14, 2):
	S.box((0.1, 0.5, z), (2.9, 0.7, z + 0.3), "dark")                      # the stiffeners across its top
S.box((0.3, 0.5, 1.0), (0.7, 2.0, 13.0), "hipaint")                        # its side plates
S.box((2.3, 0.5, 1.0), (2.7, 2.0, 13.0), "hipaint")
S.box((0.3, 2.0, 2.0), (2.7, 2.3, 12.0), "hipaint")                        # its top plate
S.box((0.6, 2.3, 5.5), (2.4, 3.0, 8.5), "iron")                            # the cam's hub, under the house's side
S.collide((0.0, 0.0, 0.0), (3.0, 2.3, 14.0))

# ---- the house ---------------------------------------------------------------------------------------------------------------------
# a shell: the machinery house's closed inside, 22 x 9 x 15 cells that draw nothing, is left out (Machine.hollow(), p6-dragline.md #3)
H = model("dragline_house", 24, 12, 20, "grey", hollow=(1, 2, 4, 22, 10, 18))
H.box((0.0, 0.0, 0.0), (24.0, 1.2, 20.0), "steel")                         # the revolving frame
H.box((0.5, 1.2, 3.0), (23.5, 11.6, 19.5), "grey")                         # the machinery house: hoist, drag and swing sets
H.box((0.3, 11.6, 2.8), (23.7, 12.0, 19.7), "galv")                        # its roof
for z in (5.0, 9.0, 13.0):
	H.decal((0.49, 6.0, z), (0.5, 10.0, z + 3.0), "louvre", "west", "grey")
	H.decal((23.5, 6.0, z), (23.51, 10.0, z + 3.0), "louvre", "east", "grey")
for x in (4.0, 16.0):
	H.box((x, 12.0, 11.0), (x + 4.0, 12.8, 15.0), "radfin")               # the generator sets' coolers on the roof
H.box((0.5, 1.2, 0.2), (4.5, 4.0, 3.0), "steel")                           # the cab's stand at the front left corner
H.box((0.5, 4.0, 0.2), (4.5, 7.5, 3.0), "hipaint")                         # the cab
H.decal((0.8, 5.0, 0.19), (4.2, 7.0, 0.2), "gauge", "north", "hipaint")    # its front window and panel
H.box((10.0, 1.2, 0.0), (14.0, 3.2, 1.5), "dark")                          # the boom foot's brackets
H.box((10.8, 1.2, 1.5), (13.2, 2.6, 3.0), "iron")                          # the drag rope fairlead
H.box((1.0, 1.2, 19.5), (23.0, 6.0, 20.0), "dark")                         # the counterweight at the rear
H.box((20.0, 1.2, 0.6), (23.5, 2.6, 3.0), "steel")                         # the walkway's stair landing at the front right
H.decal((8.0, 2.0, 2.99), (10.0, 3.0, 3.0), "nameplate", "north", "grey")
H.collide((0.0, 0.0, 0.0), (24.0, 1.2, 20.0))
H.collide((0.5, 1.2, 3.0), (23.5, 12.0, 19.5))
H.collide((0.5, 1.2, 0.2), (4.5, 7.5, 3.0))

# ---- the mast ----------------------------------------------------------------------------------------------------------------------
M = model("dragline_mast", 6, 20, 4, "hipaint")
for x in (0.0, 5.3):
	M.box((x, 0.0, 0.0), (x + 0.7, 20.0, 0.7), "hipaint")                  # the A-frame's legs
	M.box((x, 0.0, 3.3), (x + 0.7, 16.0, 4.0), "hipaint")                  # its back struts
	M.box((x, 15.5, 0.7), (x + 0.7, 16.0, 3.3), "hipaint")                 # the struts' heads, to the legs
	for y in (4.0, 8.0, 12.0):
		M.box((x + 0.15, y, 0.7), (x + 0.55, y + 0.3, 3.3), "dark")        # the side bracing
for y in (5.0, 10.0, 15.0):
	M.box((0.7, y, 0.1), (5.3, y + 0.4, 0.6), "dark")                      # the cross bracing between the legs
M.box((0.0, 19.2, 0.0), (6.0, 20.0, 2.0), "hipaint")                       # the head
M.box((1.5, 18.4, 0.2), (4.5, 19.2, 1.6), "iron")                          # the suspension ropes' equaliser
M.collide((0.0, 0.0, 0.0), (6.0, 20.0, 0.7))
M.collide((0.0, 0.0, 3.3), (6.0, 16.0, 4.0))

# ---- a boom section ----------------------------------------------------------------------------------------------------------------
B = model("dragline_boom_section", 4, 4, 15, "hipaint")
for x, y in ((0.0, 0.0), (3.5, 0.0), (0.0, 3.5), (3.5, 3.5)):
	B.box((x, y, 0.0), (x + 0.5, y + 0.5, 15.0), "hipaint")                # the four chords
for i in range(10):
	z = 0.65 + i * 1.5
	B.box((0.1, 0.5, z), (0.4, 3.5, z + 0.2), "hipaint")                   # the lacing: on each side, top and bottom
	B.box((3.6, 0.5, z), (3.9, 3.5, z + 0.2), "hipaint")
	B.box((0.5, 0.1, z), (3.5, 0.4, z + 0.2), "hipaint")
	B.box((0.5, 3.6, z), (3.5, 3.9, z + 0.2), "hipaint")
for z in (0.0, 14.7):
	B.box((0.0, 0.0, z), (4.0, 0.6, z + 0.3), "dark")                      # the pinned joints' end frames
	B.box((0.0, 3.4, z), (4.0, 4.0, z + 0.3), "dark")
B.collide((0.0, 0.0, 0.0), (4.0, 4.0, 15.0))

# ---- the bucket --------------------------------------------------------------------------------------------------------------------
K = model("dragline_bucket", 5, 4, 6, "steel")
K.box((0.3, 0.0, 0.5), (4.7, 0.4, 5.6), "steel")                           # the floor
K.box((0.3, 0.4, 0.5), (0.7, 3.0, 5.6), "steel")                           # the sides
K.box((4.3, 0.4, 0.5), (4.7, 3.0, 5.6), "steel")
K.box((0.7, 0.4, 0.5), (4.3, 2.6, 0.9), "steel")                           # the back
K.box((0.3, 0.0, 5.6), (4.7, 0.6, 6.0), "iron")                            # the lip at its mouth, toward the machine
for x in (0.5, 1.4, 2.3, 3.2, 4.1):
	K.box((x, 0.05, 6.0), (x + 0.35, 0.4, 6.45), "dark")                   # the teeth
K.box((0.3, 3.0, 5.0), (4.7, 3.6, 5.6), "hipaint")                         # the arch over its mouth
K.box((0.1, 1.2, 2.4), (0.3, 1.8, 3.0), "iron")                            # the trunnions the hoist chains hang from
K.box((4.7, 1.2, 2.4), (4.9, 1.8, 3.0), "iron")
K.box((0.15, 1.8, 2.6), (0.25, 3.8, 2.8), "iron")                          # the hoist chains
K.box((4.75, 1.8, 2.6), (4.85, 3.8, 2.8), "iron")
K.box((0.1, 3.8, 2.4), (4.9, 4.0, 3.0), "iron")                            # the spreader bar
K.collide((0.3, 0.0, 0.5), (4.7, 3.0, 6.0))

MODELS = [T, S, H, M, B, K]
