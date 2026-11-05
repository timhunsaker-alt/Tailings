"""The flotation bank at true size (docs/multiblocks/flotation-bank.md), each part drawn facing north from its own main cell:
- conditioning_tank, 3 x 4 x 3: the 3 m agitated tank, its bridge, the 15 kW drive and the reagent hopper;
- flotation_bank, 7 x 4 x 13: eight 20 m3 tank cells (about 3.3 m across) in two rows, four roughers on the east, two
  scavengers and two cleaners on the west, their froth launders, the agitator drives and the walkway between;
- air_blower, 2 x 2 x 3: a multistage centrifugal blower on its skid with its 75 kW motor; the header leaves its east side;
- concentrate_thickener, 8 x 3 x 8: the 8 m high-rate thickener on its legs, the bridge and rake drive;
- pressure_filter, 3 x 3 x 5: a plate-and-frame filter, the plate pack between its head and tail, the hydraulic closing ram.
"""
import tlib
from tlib import disc, model, motor_box

# ---- the conditioning tank --------------------------------------------------------------------------------------------------------
CT = model("conditioning_tank", 3, 4, 3, "grey")
CT.box((0.0, 0.0, 0.0), (3.0, 0.25, 3.0), "concrete")
CT.ring("y", (1.5, 1.5), 1.4, 0.25, 3.4, 0.08, "grey", sides=20)            # the 3 m tank
disc(CT, 1.5, 1.5, 1.4, 3.1, 3.15, "dark")                                  # the slurry
CT.box((0.1, 3.4, 1.3), (2.9, 3.55, 1.7), "grating")                        # the bridge
CT.box((1.2, 3.55, 1.2), (1.8, 3.95, 1.8), "blue")                          # the 15 kW agitator drive
CT.box((2.1, 3.4, 2.1), (2.8, 3.95, 2.8), "galv")                           # the reagent hopper
motor_box(CT, 0.4, 0.2)
CT.pipe([(1.5, 0.6, 0.0), (1.5, 0.6, 0.1)], 6, "rubber")                    # fines slurry in, the front
CT.pipe([(1.5, 0.6, 2.9), (1.5, 0.6, 3.0)], 6, "rubber")                    # conditioned slurry out, the back
CT.collide((0.1, 0.0, 0.1), (2.9, 3.55, 2.9))

# ---- the flotation bank -----------------------------------------------------------------------------------------------------------
FB = model("flotation_bank", 7, 4, 13, "grey")
FB.box((0.0, 0.0, 0.0), (7.0, 0.3, 13.0), "concrete")
ROWS = {1.75: ("scavenger", "scavenger", "cleaner", "cleaner"), 5.25: ("rougher",) * 4}
for cx in ROWS:
	for i in range(4):
		cz = 1.65 + 3.25 * i
		FB.ring("y", (cx, cz), 1.6, 0.3, 3.3, 0.08, "grey", sides=20)       # a 20 m3 tank cell
		disc(FB, cx, cz, 1.6, 3.0, 3.05, "dark")                            # its froth
		FB.ring("y", (cx, cz), 1.7, 3.0, 3.3, 0.1, "galv", sides=20)        # its froth launder
		FB.box((cx - 0.3, 3.3, cz - 0.3), (cx + 0.3, 3.85, cz + 0.3), "blue")   # its 45 kW agitator drive
		FB.box((cx - 1.65, 3.3, cz - 0.15), (cx + 1.65, 3.42, cz + 0.15), "iron")  # the drive's beam
FB.box((3.3, 3.3, 0.2), (3.7, 3.42, 12.8), "grating")                       # the walkway between the rows
FB.handrail([(3.3, 0.2), (3.3, 12.8)], 3.42, "galv", 0.5)
FB.handrail([(3.7, 0.2), (3.7, 12.8)], 3.42, "galv", 0.5)
FB.pipe([(0.0, 3.6, 1.65), (0.15, 3.6, 1.65), (0.15, 3.6, 11.4)], 6, "galv")   # the air header from the blower, west side
motor_box(FB, 0.35, 0.15)
FB.pipe([(3.5, 0.6, 0.0), (3.5, 0.6, 0.2)], 6, "rubber")                    # conditioned slurry in, the front
FB.pipe([(6.5, 0.6, 12.8), (6.5, 0.6, 13.0)], 4, "rubber")                  # concentrate slurry out, the back, east
FB.pipe([(0.5, 0.6, 12.8), (0.5, 0.6, 13.0)], 6, "rubber")                  # final tail out, the back, west
FB.collide((0.0, 0.0, 0.0), (7.0, 3.42, 13.0))

# ---- the air blower ---------------------------------------------------------------------------------------------------------------
AB = model("air_blower", 2, 2, 3, "blue")
AB.box((0.1, 0.0, 0.1), (1.9, 0.2, 2.9), "iron")                            # the skid
AB.cylinder("z", (1.0, 0.85), 0.6, 0.2, 1.5, "blue", 16)                     # the multistage casing
AB.cylinder("z", (1.0, 0.75), 0.4, 1.7, 2.85, "blue", 12)                    # the 75 kW motor
AB.cylinder("z", (1.0, 0.8), 0.15, 1.5, 1.7, "iron", 8)                      # the coupling
AB.pipe([(1.0, 1.45, 0.8), (1.0, 1.9, 0.8), (2.0, 1.9, 0.8)], 6, "galv")     # the air header, out of the east side
motor_box(AB, 0.3, 0.3, 0.5, 0.4)
AB.collide((0.1, 0.0, 0.1), (1.9, 1.5, 2.9))

# ---- the concentrate thickener ----------------------------------------------------------------------------------------------------
TH = model("concentrate_thickener", 8, 3, 8, "grey")
for x in (1.0, 7.0):
	for z in (1.0, 7.0):
		TH.box((x - 0.15, 0.0, z - 0.15), (x + 0.15, 0.8, z + 0.15), "iron")  # its legs
TH.cylinder("y", (4.0, 4.0), 0.7, 0.0, 0.8, "grey", 14)                      # the underflow cone
TH.ring("y", (4.0, 4.0), 3.95, 0.8, 2.6, 0.08, "grey", sides=28)             # the 8 m tank
disc(TH, 4.0, 4.0, 3.95, 2.35, 2.4, "dark")                                 # the concentrate, under its bridge
TH.box((0.3, 2.6, 3.65), (7.7, 2.8, 4.35), "grating")                       # the bridge
TH.box((3.6, 2.8, 3.6), (4.4, 2.98, 4.4), "blue")                           # the 5.5 kW rake drive
TH.pipe([(4.0, 0.5, 0.0), (4.0, 0.5, 0.8)], 4, "rubber")                    # concentrate slurry in, the front
TH.pipe([(4.0, 0.4, 7.2), (4.0, 0.4, 8.0)], 3, "rubber")                    # thickened concentrate out, the back
TH.box((0.05, 0.0, 0.35), (0.25, 0.2, 0.65), "galv")                        # the junction stand at the cable
motor_box(TH, 0.2, 0.3, 0.45, 0.4)
TH.collide((0.05, 0.8, 0.05), (7.95, 2.8, 7.95))
TH.collide((3.3, 0.0, 3.3), (4.7, 0.8, 4.7))

# ---- the pressure filter ----------------------------------------------------------------------------------------------------------
PF = model("pressure_filter", 3, 3, 5, "grey")
for x in (0.3, 2.5):
	PF.box((x, 0.0, 0.2), (x + 0.2, 1.4, 0.5), "iron")                      # the head and tail stands
	PF.box((x, 0.0, 4.5), (x + 0.2, 1.4, 4.8), "iron")
	PF.box((x, 1.4, 0.2), (x + 0.2, 1.6, 4.8), "iron")                      # the side beams the plates hang from
PF.box((0.4, 1.4, 0.2), (2.6, 2.8, 0.6), "blue")                            # the fixed head
PF.box((0.4, 1.4, 4.2), (2.6, 2.8, 4.6), "blue")                            # the moving tail
z = 0.65
while z < 4.1:
	PF.box((0.55, 1.5, z), (2.45, 2.7, z + 0.09), "polymer")                # a filter plate
	z += 0.12
PF.cylinder("z", (1.5, 2.1), 0.15, 4.6, 4.95, "steel", 10)                   # the hydraulic closing ram
PF.box((0.6, 0.6, 0.7), (2.4, 1.3, 4.1), "galv")                            # the cake chute to the hopper under it
motor_box(PF, 0.3, 0.3, 0.5, 0.4)
PF.pipe([(1.5, 2.0, 0.0), (1.5, 2.0, 0.2)], 3, "rubber")                    # thickened concentrate in, the front
PF.collide((0.3, 0.0, 0.2), (2.7, 2.8, 4.95))

MODELS = [CT, FB, AB, TH, PF]
