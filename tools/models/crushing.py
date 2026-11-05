"""The crushing circuit at true size (docs/multiblocks/crushing-circuit.md), each part drawn facing north from its own main cell:
- grizzly, 3 x 2 x 4: a static bar grizzly with a 150 mm gap, on short legs over the jaw's feed opening;
- jaw_crusher, 3 x 4 x 4: a C100-class jaw on its plinth, open on top for the grizzly, flywheels, the 110 kW motor; discharge south;
- double_deck_screen, 3 x 3 x 6: a 1.8 x 4.8 m inclined two-deck screen on springs, its vibrator, the undersize hopper;
- cone_crusher, 3 x 4 x 3: an HP200-class cone on its base, the feed hopper, the 132 kW motor and V-belt guard;
- conveyor_drive, 1 x 1 x 2: a belt run's head pulley, gearbox and 7.5 kW motor;
- ball_mill, 4 x 4 x 6: a 3.2 x 4.5 m overflow ball mill on its trunnions, ring gear and pinion, the 600 kW motor (4.16 kV box);
- cyclone_cluster, 2 x 4 x 2: four 250 mm hydrocyclones on their distributor over the pump box and the 45 kW slurry pump.
"""
import tlib
from tlib import model, motor_box, stepped

# ---- the grizzly ------------------------------------------------------------------------------------------------------------------
GZ = model("grizzly", 3, 2, 4, "iron")
for x in (0.15, 2.65):
	for z in (0.2, 3.6):
		GZ.box((x, 0.0, z), (x + 0.2, 0.9, z + 0.2), "iron")                # legs onto the jaw's frame
GZ.box((0.05, 0.9, 0.0), (0.2, 1.9, 4.0), "padgreen")                       # the side plates
GZ.box((2.8, 0.9, 0.0), (2.95, 1.9, 4.0), "padgreen")
GZ.box((0.2, 0.9, 0.0), (2.8, 1.9, 0.12), "padgreen")                       # the feed lip at the north end
x = 0.25
while x < 2.75:
	GZ.box((x, 1.15, 0.12), (x + 0.1, 1.3, 3.95), "iron")                   # bars, 100 mm, with 150 mm gaps
	x += 0.25
GZ.box((0.2, 0.95, 1.0), (2.8, 1.15, 1.15), "iron")                         # cross members under the bars
GZ.box((0.2, 0.95, 2.8), (2.8, 1.15, 2.95), "iron")
GZ.collide((0.05, 0.9, 0.0), (2.95, 1.3, 4.0))

# ---- the jaw crusher --------------------------------------------------------------------------------------------------------------
JC = model("jaw_crusher", 3, 4, 4, "padgreen")
JC.box((0.0, 0.0, 0.0), (3.0, 0.8, 4.0), "concrete")                        # the plinth
JC.box((0.6, 0.8, 0.5), (0.9, 3.95, 3.3), "padgreen")                       # the frame's cheek plates, open on top
JC.box((2.1, 0.8, 0.5), (2.4, 3.95, 3.3), "padgreen")
JC.box((0.9, 0.8, 0.5), (2.1, 3.95, 0.9), "padgreen")                       # the fixed jaw's back
JC.box((0.9, 1.2, 1.7), (2.1, 3.7, 2.1), "iron")                            # the swing jaw and pitman
JC.box((0.9, 0.8, 2.9), (2.1, 1.4, 3.3), "padgreen")                        # the toggle seat
JC.cylinder("x", (3.1, 2.3), 0.85, 0.25, 0.55, "iron", 16)                   # the flywheels on the eccentric shaft
JC.cylinder("x", (3.1, 2.3), 0.85, 2.45, 2.75, "iron", 16)
JC.cylinder("x", (3.1, 2.3), 0.12, 0.55, 2.45, "steel", 8)                   # the eccentric shaft
JC.box((0.9, 0.8, 3.3), (2.1, 1.0, 4.0), "steel")                           # the discharge chute, out of the south face
JC.cylinder("z", (2.6, 1.3), 0.38, 3.3, 3.98, "blue", 12)                    # the 110 kW motor
motor_box(JC, 0.9, 0.3)
JC.collide((0.0, 0.0, 0.0), (3.0, 0.8, 4.0))
JC.collide((0.6, 0.8, 0.5), (2.4, 3.95, 3.3))

# ---- the double-deck screen -------------------------------------------------------------------------------------------------------
SC = model("double_deck_screen", 3, 3, 6, "padgreen")
SC.box((0.0, 0.0, 0.0), (3.0, 0.3, 6.0), "concrete")
for z in (0.4, 5.3):
	for x in (0.3, 2.4):
		SC.box((x, 0.3, z), (x + 0.3, 0.9 if z < 1 else 0.6, z + 0.3), "iron")       # the spring mounts, higher at the feed end
		SC.cylinder("y", (x + 0.15, z + 0.15), 0.12, 0.9 if z < 1 else 0.6, 1.2 if z < 1 else 0.9, "galv", 8)   # the coil springs
SC.box((0.5, 1.2, 0.2), (0.62, 2.9, 5.9), "padgreen")                       # the side plates
SC.box((2.38, 1.2, 0.2), (2.5, 2.9, 5.9), "padgreen")
stepped(SC, 0.62, 2.38, 2.5, 1.7, 0.3, 5.9, 8, 0.06, "grating")              # the 40 mm top deck, falling south
stepped(SC, 0.62, 2.38, 2.0, 1.2, 0.3, 5.9, 8, 0.06, "grating")              # the 6 mm bottom deck
SC.box((0.62, 2.5, 0.2), (2.38, 2.9, 0.4), "padgreen")                      # the feed box at the north end
SC.cylinder("x", (2.2, 3.0), 0.25, 0.0, 0.5, "blue", 10)                     # the vibrator motor, on the west side
SC.cylinder("x", (2.2, 3.0), 0.15, 2.5, 2.9, "iron", 8)                      # the exciter's far bearing
SC.box((0.7, 0.3, 1.2), (2.3, 1.1, 4.8), "galv")                            # the undersize hopper: sand out of its east face
SC.box((2.3, 0.4, 2.7), (3.0, 0.7, 3.3), "galv")
motor_box(SC, 0.35, 0.35, 0.5, 0.4)
SC.collide((0.0, 0.0, 0.0), (3.0, 1.1, 6.0))
SC.collide((0.5, 1.1, 0.2), (2.5, 2.9, 5.9))

# ---- the cone crusher -------------------------------------------------------------------------------------------------------------
CC = model("cone_crusher", 3, 4, 3, "padgreen")
CC.box((0.0, 0.0, 0.0), (3.0, 0.6, 3.0), "concrete")
CC.cylinder("y", (1.5, 1.4), 1.0, 0.6, 1.9, "padgreen", 16)                  # the main frame
CC.cylinder("y", (1.5, 1.4), 0.85, 1.9, 2.6, "padgreen", 16)                 # the adjustment ring and bowl
CC.cylinder("y", (1.5, 1.4), 0.6, 2.6, 3.1, "iron", 14)                      # the feed hopper
CC.cylinder("y", (1.5, 1.4), 0.75, 3.1, 3.7, "iron", 14)
CC.cylinder("z", (2.6, 0.95), 0.12, 2.1, 2.9, "steel", 8)                    # the countershaft
CC.box((2.3, 0.6, 2.4), (2.9, 1.3, 2.95), "padgreen")                       # the V-belt guard
CC.cylinder("z", (0.75, 0.9), 0.36, 2.3, 2.98, "blue", 12)                   # the 132 kW motor
CC.box((1.2, 0.6, 2.4), (1.8, 0.9, 3.0), "steel")                           # the product chute, out of the south face
motor_box(CC, 0.7, 0.3)
CC.collide((0.0, 0.0, 0.0), (3.0, 0.6, 3.0))
CC.collide((0.5, 0.6, 0.4), (2.5, 3.7, 2.4))

# ---- the conveyor drive -----------------------------------------------------------------------------------------------------------
CD = model("conveyor_drive", 1, 1, 2, "grey")
CD.box((0.1, 0.0, 0.05), (0.18, 0.75, 1.95), "iron")                        # the drive frame
CD.box((0.82, 0.0, 0.05), (0.9, 0.75, 1.95), "iron")
CD.cylinder("x", (0.6, 0.5), 0.25, 0.18, 0.82, "rubber", 12)                 # the lagged head pulley
CD.box((0.3, 0.1, 1.0), (0.7, 0.45, 1.5), "blue")                           # the shaft-mounted gearbox
CD.cylinder("z", (0.5, 0.28), 0.17, 1.5, 1.95, "blue", 10)                   # the 7.5 kW motor
motor_box(CD, 0.1, 0.3, 0.35, 0.3)
CD.collide((0.1, 0.0, 0.05), (0.9, 0.85, 1.95))

# ---- the ball mill ----------------------------------------------------------------------------------------------------------------
BM = model("ball_mill", 4, 4, 6, "padgreen")
BM.box((0.0, 0.0, 0.0), (4.0, 0.4, 6.0), "concrete")
BM.box((0.6, 0.4, 0.05), (3.4, 1.4, 0.6), "concrete")                       # the feed-end trunnion pier
BM.box((0.6, 0.4, 5.4), (3.4, 1.4, 5.95), "concrete")                       # the discharge-end pier
BM.cylinder("z", (2.0, 2.2), 0.45, 0.05, 0.75, "iron", 14)                   # the feed trunnion and chute
BM.box((1.7, 2.4, 0.0), (2.3, 3.9, 0.4), "steel")                           # the feed spout, where the belt delivers
BM.ring("z", (2.0, 2.2), 1.6, 0.75, 5.25, 0.08, "padgreen", sides=24)        # the 3.2 m shell
for z in (0.72, 5.22):                                                      # the shell heads: octagons inside the shell
	r = 1.58
	BM.box((2.0 - r * 0.38, 2.2 - r * 0.92, z), (2.0 + r * 0.38, 2.2 + r * 0.92, z + 0.06), "padgreen")
	BM.box((2.0 - r * 0.92, 2.2 - r * 0.38, z), (2.0 + r * 0.92, 2.2 + r * 0.38, z + 0.06), "padgreen")
	BM.box((2.0 - r * 0.65, 2.2 - r * 0.65, z), (2.0 + r * 0.65, 2.2 + r * 0.65, z + 0.06), "padgreen")
BM.ring("z", (2.0, 2.2), 1.75, 4.2, 4.5, 0.12, "iron", sides=24)             # the ring gear
BM.cylinder("z", (2.0, 2.2), 0.45, 5.25, 5.95, "iron", 14)                   # the discharge trunnion
BM.box((3.5, 0.4, 3.9), (3.95, 1.1, 4.8), "iron")                            # the pinion and its bearings
BM.box((3.55, 1.1, 4.0), (3.95, 1.4, 4.7), "iron")
BM.box((0.15, 0.4, 3.9), (0.6, 1.3, 5.3), "blue")                            # the 600 kW motor, on the west side
motor_box(BM, 0.42, 0.15, 0.55, 0.55)
BM.pipe([(2.0, 0.6, 5.95), (2.0, 0.6, 6.0)], 8, "rubber")                    # mill discharge out, the back
BM.collide((0.0, 0.0, 0.0), (4.0, 1.4, 6.0))
BM.collide((0.4, 1.4, 0.75), (3.6, 3.8, 5.25))

# ---- the cyclone cluster ----------------------------------------------------------------------------------------------------------
CY = model("cyclone_cluster", 2, 4, 2, "grey")
CY.box((0.05, 0.0, 0.05), (1.95, 1.2, 1.95), "rubber")                       # the pump box
CY.cylinder("z", (1.4, 0.45), 0.3, 0.0, 0.6, "blue", 10)                    # the 45 kW slurry pump's motor
for x in (0.2, 1.75):
	for z in (0.2, 1.75):
		CY.box((x, 1.2, z), (x + 0.08, 2.6, z + 0.08), "galv")              # the stand
CY.box((0.15, 2.5, 0.15), (1.85, 2.62, 1.85), "grating")                     # the platform
CY.cylinder("y", (1.0, 1.0), 0.18, 2.62, 3.2, "steel", 10)                   # the distributor
for cx, cz in ((0.55, 0.55), (1.45, 0.55), (0.55, 1.45), (1.45, 1.45)):
	CY.cylinder("y", (cx, cz), 0.04, 1.3, 1.9, "rubber", 8)                  # the apex (underflow)
	CY.cylinder("y", (cx, cz), 0.1, 1.9, 2.5, "rubber", 8)                   # the cone
	CY.cylinder("y", (cx, cz), 0.14, 2.62, 3.4, "rubber", 8)                 # the 250 mm cylinder section
CY.box((0.2, 3.4, 0.2), (1.8, 3.6, 1.8), "steel")                           # the overflow launder
motor_box(CY, 0.3, 0.25, 0.5, 0.4)
CY.pipe([(1.0, 0.6, 0.0), (1.0, 0.6, 0.05)], 8, "rubber")                    # mill discharge in, the front
CY.pipe([(1.0, 3.5, 1.8), (1.0, 3.5, 2.0)], 6, "rubber")                     # fines slurry out, the back
CY.collide((0.05, 0.0, 0.05), (1.95, 3.6, 1.95))

MODELS = [GZ, JC, SC, CC, CD, BM, CY]
