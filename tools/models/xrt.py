"""The X-ray transmission ore sorter at true size (docs/multiblocks/xrt-ore-sorter.md, L8), each part drawn facing north from its main
cell:
- xrt_sorter, 4 x 5 x 8: the vibrating feeder's hopper over the north end, the 1.2 m belt running south through the shielded X-ray
  cabinet (the source on top, the line-scan detector under the belt), the ejectors' air manifold at the head pulley, the product chute
  out of the south face and the waste chute out of the east face, its control cabinet;
- sorter_compressor, 2 x 2 x 3: a screw compressor in its louvred enclosure on a skid, the air receiver standing at the back.
"""
import tlib
from tlib import model, motor_box

# ---- the X-ray sorter ----------------------------------------------------------------------------------------------------------
XS = model("xrt_sorter", 4, 5, 8, "grey")
XS.box((0.0, 0.0, 0.0), (4.0, 0.2, 8.0), "concrete")
for x in (0.3, 3.5):
	for z in (0.4, 3.0, 5.6, 7.2):
		XS.box((x, 0.2, z), (x + 0.2, 2.0, z + 0.2), "blue")                # the frame's columns
XS.box((0.3, 2.0, 0.4), (3.7, 2.15, 7.4), "blue")                           # the belt's frame
XS.box((1.35, 2.15, 1.0), (2.65, 2.25, 7.0), "rubber")                       # the 1.2 m belt
XS.cylinder("x", (2.2, 1.0), 0.2, 1.3, 2.7, "iron", 10)                    # the tail pulley
XS.cylinder("x", (2.2, 7.0), 0.25, 1.3, 2.7, "steel", 12)                  # the head pulley, where the pieces leave
XS.cylinder("x", (2.2, 7.0), 0.18, 2.7, 3.1, "blue", 10)                   # its drive
# the vibrating feeder: a hopper over the north end, its pan spreading the rock in one layer onto the belt
XS.box((1.0, 3.6, 0.3), (3.0, 4.9, 1.9), "galv")
XS.box((1.15, 4.9, 0.45), (2.85, 4.95, 1.75), "grating")
XS.box((1.2, 2.9, 0.6), (2.8, 3.6, 1.6), "galv")
XS.box((1.3, 2.55, 1.0), (2.7, 2.7, 2.4), "steel")                          # the feeder pan
XS.box((1.25, 2.7, 1.0), (1.35, 3.0, 2.4), "steel")
XS.box((2.65, 2.7, 1.0), (2.75, 3.0, 2.4), "steel")
# the X-ray cabinet, shielded, over the belt; the source on top, the detector line under the belt
XS.box((0.9, 2.25, 3.2), (3.1, 4.3, 5.0), "grey")
XS.box((1.5, 4.3, 3.7), (2.5, 4.8, 4.5), "hipaint")                         # the X-ray source
XS.box((1.7, 4.8, 3.9), (2.3, 4.9, 4.3), "dark")
XS.box((1.2, 1.7, 3.9), (2.8, 2.0, 4.3), "dark")                            # the line-scan detector
XS.decal((3.1, 3.2, 3.6), (3.11, 3.9, 4.6), "danger", "east", "grey")       # its radiation warning
XS.decal((0.89, 3.2, 3.6), (0.9, 3.9, 4.6), "danger", "west", "grey")
XS.box((0.8, 2.25, 3.0), (3.2, 2.9, 3.2), "rubber")                         # the lead curtains at each end
XS.box((0.8, 2.25, 5.0), (3.2, 2.9, 5.2), "rubber")
# the ejectors: an air manifold across the head pulley, a row of valves, the air line from the compressor along the west side
XS.box((1.2, 1.75, 7.25), (2.8, 1.95, 7.45), "steel")
for i in range(8):
	x = 1.3 + i * 0.18
	XS.box((x, 1.95, 7.28), (x + 0.1, 2.08, 7.42), "copper")
XS.pipe([(0.0, 0.6, 6.6), (0.6, 0.6, 6.6), (0.6, 1.85, 6.6), (0.6, 1.85, 7.35), (1.2, 1.85, 7.35)], 2, "galv")
# the splitter plate between the two streams, the product chute south and the waste chute east
XS.box((1.2, 0.9, 7.55), (2.8, 1.9, 7.6), "steel")
XS.box((1.2, 0.3, 7.6), (2.8, 1.5, 8.0), "galv")                            # product: blown over the splitter
XS.box((1.2, 0.3, 6.1), (2.8, 1.5, 7.5), "galv")                            # waste: falls short of it...
XS.box((2.8, 0.3, 6.1), (4.0, 1.0, 7.0), "galv")                            # ...and out of the east face
# the control cabinet and a walkway along the west side
XS.box((3.3, 0.2, 1.0), (3.95, 1.9, 2.0), "grey")
XS.decal((3.95, 0.8, 1.15), (3.96, 1.6, 1.85), "controller", "east", "grey")
XS.box((0.0, 1.2, 2.2), (0.3, 1.25, 7.0), "grating")
XS.box((0.0, 2.25, 2.2), (0.05, 2.35, 7.0), "galv")                         # its handrail
motor_box(XS, 0.4, 0.3)
XS.collide((0.3, 0.0, 0.3), (3.7, 4.3, 8.0))

# ---- the sorter's compressor ---------------------------------------------------------------------------------------------------
SC = model("sorter_compressor", 2, 2, 3, "blue")
SC.box((0.05, 0.0, 0.05), (1.95, 0.15, 1.95), "iron")                       # the skid
SC.box((0.15, 0.15, 0.15), (1.85, 1.55, 1.85), "blue")                      # the enclosure
SC.decal((1.85, 0.4, 0.4), (1.86, 1.3, 1.6), "louvre", "east", "blue")
SC.decal((0.15, 0.9, 0.9), (0.14, 1.35, 1.6), "louvre", "west", "blue")
SC.decal((0.6, 0.6, 0.14), (1.4, 1.2, 0.15), "ratingplate", "north", "blue")
SC.box((0.5, 1.55, 0.5), (1.5, 1.65, 1.5), "louvre")                        # the cooling air out of the roof
SC.cylinder("y", (1.0, 2.45), 0.42, 0.15, 1.75, "galv", 14)               # the air receiver
SC.cylinder("y", (1.0, 2.45), 0.3, 1.75, 1.85, "galv", 12)
SC.box((0.9, 0.0, 2.35), (1.1, 0.15, 2.55), "iron")
SC.pipe([(1.0, 1.2, 1.85), (1.0, 1.2, 2.05)], 2, "galv")                    # compressor to receiver
SC.pipe([(1.0, 0.6, 2.85), (1.0, 0.6, 3.0)], 2, "galv")                     # the air out, the back
SC.decal((1.2, 1.1, 2.86), (1.4, 1.3, 2.87), "gauge", "south", "galv")
motor_box(SC, 0.3, 0.3, 0.5, 0.4)
SC.collide((0.05, 0.0, 0.05), (1.95, 1.85, 2.9))

MODELS = [XS, SC]
