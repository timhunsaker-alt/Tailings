"""Dense-medium separation at true size (docs/multiblocks/dense-medium-separation.md, L7), each part drawn facing north from its main cell:
- deslime_screen, 2 x 2 x 4: a short wet screen at 1 mm on springs, spray bars across it, its undersize hopper;
- dms_cyclone_module, 3 x 6 x 4: a steel stand carrying the mixing box on top, one 610 mm DMS cyclone lying at about 10 degrees
  (overflow, the floats, out of its north end; spigot, the sinks, out of its south end), and the 55 kW medium pump and correct-medium
  sump at its foot;
- drain_rinse_screen, 2 x 2 x 5: a long screen, the drain section at the feed end and the rinse sprays over the discharge end;
- medium_recovery, 2 x 3 x 3: a wet drum magnetic separator in its tank, the densifier over it, and the ferrosilicon make-up hopper.
"""
import tlib
from tlib import model, motor_box, stepped


def springs(M, w, d, low, high):
	"""Four coil springs on their mounts, higher at the feed (north) end, as an inclined screen sits."""
	for z, top in ((0.3, high), (d - 0.6, low)):
		for x in (0.15, w - 0.45):
			M.box((x, 0.2, z), (x + 0.3, top - 0.25, z + 0.3), "iron")
			M.cylinder("y", (x + 0.15, z + 0.15), 0.1, top - 0.25, top, "galv", 8)


def spray_bar(M, w, y, z):
	"""A spray bar across the deck on the water line, its nozzles under it."""
	M.cylinder("x", (y, z), 0.05, 0.25, w - 0.25, "pipe_water", 8)
	for k in range(4):
		x = 0.4 + k * (w - 0.8) / 3
		M.box((x - 0.03, y - 0.12, z - 0.03), (x + 0.03, y - 0.04, z + 0.03), "brass")


# ---- the deslime screen ------------------------------------------------------------------------------------------------------------
DS = model("deslime_screen", 2, 2, 4, "padgreen")
DS.box((0.0, 0.0, 0.0), (2.0, 0.2, 4.0), "concrete")
springs(DS, 2, 4, 0.75, 0.95)
DS.box((0.2, 0.75, 0.15), (0.3, 1.85, 3.9), "padgreen")                     # the side plates
DS.box((1.7, 0.75, 0.15), (1.8, 1.85, 3.9), "padgreen")
stepped(DS, 0.3, 1.7, 1.25, 0.95, 0.2, 3.9, 6, 0.05, "polymer")             # the 1 mm polyurethane deck, falling south
DS.box((0.3, 0.75, 0.2), (1.7, 0.9, 3.9), "galv")                           # the undersize pan under it
DS.box((0.3, 1.45, 0.15), (1.7, 1.85, 0.35), "padgreen")                    # the feed box at the north end
for z in (1.2, 2.1, 3.0):
	spray_bar(DS, 2, 1.7, z)
DS.pipe([(1.0, 1.7, 3.0), (1.0, 1.95, 3.0), (1.0, 1.95, 1.2), (1.0, 1.7, 1.2)], 2, "pipe_water")
DS.cylinder("x", (1.6, 2.2), 0.18, 1.8, 2.0, "blue", 10)                    # the vibrator motor, on the east side
DS.box((1.8, 0.35, 0.8), (2.0, 0.7, 2.0), "galv")                           # the fines launder out of the east face
motor_box(DS, 0.25, 0.1, 0.4, 0.35)
DS.collide((0.2, 0.0, 0.15), (1.8, 1.85, 3.9))

# ---- the DMS cyclone module --------------------------------------------------------------------------------------------------------
DM = model("dms_cyclone_module", 3, 6, 4, "steel")
DM.box((0.0, 0.0, 0.0), (3.0, 0.25, 4.0), "concrete")
for x in (0.2, 2.6):
	for z in (0.2, 3.6):
		DM.box((x, 0.25, z), (x + 0.2, 5.95, z + 0.2), "blue")              # the stand's columns
for y in (2.2, 4.3):
	DM.box((0.2, y, 0.2), (2.8, y + 0.1, 3.8), "grating")                   # the cyclone floor and the mixing-box floor
	for z in (0.2, 3.7):
		DM.box((0.2, y + 0.1, z), (2.8, y + 1.1, z + 0.1), "iron")          # the handrails, ends
DM.box((0.6, 4.4, 1.0), (2.4, 5.6, 3.0), "galv")                            # the mixing box
DM.box((0.75, 5.6, 1.15), (2.25, 5.65, 2.85), "grating")
DM.box((1.1, 5.65, 0.0), (1.9, 5.9, 1.3), "rubber")                         # the gravel chute in from the deslime screen
# the 610 mm cyclone, lying at about 10 degrees: the barrel high at the north (feed) end, the cone tapering to the spigot at the south
for k, (r, z0, z1) in enumerate(((0.31, 0.35, 1.25), (0.27, 1.25, 1.95), (0.22, 1.95, 2.6), (0.16, 2.6, 3.15), (0.1, 3.15, 3.55))):
	DM.cylinder("z", (1.5, 3.2 - 0.12 * k), r, z0, z1, "rubber" if k == 0 else "steel", 14)
DM.pipe([(1.5, 3.25, 0.35), (1.5, 3.25, 0.0)], 8, "rubber")                # the overflow, the floats, out of the north end
DM.box((1.1, 2.55, 3.55), (1.9, 2.75, 4.0), "galv")                         # the spigot box, the sinks, out of the south end
DM.pipe([(1.15, 3.45, 0.7), (0.55, 3.45, 0.7)], 6, "rubber")                # the tangential feed inlet
# the correct-medium sump and the medium pump at the foot
DM.box((1.6, 0.25, 0.6), (2.6, 1.6, 2.6), "galv")
DM.cylinder("z", (0.95, 0.9), 0.35, 1.2, 2.0, "blue", 12)                   # the pump casing
DM.cylinder("z", (0.95, 0.9), 0.28, 2.0, 3.0, "grey", 12)                   # its 55 kW motor
DM.pipe([(0.9, 0.95, 1.2), (0.55, 0.95, 1.2), (0.55, 3.45, 1.2), (0.55, 3.45, 0.7)], 6, "rubber")   # the medium up to the cyclone
DM.cylinder("y", (2.4, 3.2), 0.12, 0.25, 2.2, "pipe_water", 8)              # the water line up the stand
motor_box(DM, 0.4, 0.2)
DM.collide((0.0, 0.0, 0.0), (3.0, 5.95, 4.0))

# ---- the drain-and-rinse screen ----------------------------------------------------------------------------------------------------
DR = model("drain_rinse_screen", 2, 2, 5, "padgreen")
DR.box((0.0, 0.0, 0.0), (2.0, 0.2, 5.0), "concrete")
springs(DR, 2, 5, 0.75, 0.85)
DR.box((0.2, 0.75, 0.1), (0.3, 1.8, 4.9), "padgreen")
DR.box((1.7, 0.75, 0.1), (1.8, 1.8, 4.9), "padgreen")
stepped(DR, 0.3, 1.7, 1.15, 0.95, 0.15, 4.9, 6, 0.05, "polymer")             # the 1 mm deck, nearly flat
DR.box((0.3, 0.75, 0.15), (1.7, 0.9, 2.3), "steel")                         # the drain section's pan: medium back to the sump
DR.box((0.3, 0.75, 2.3), (1.7, 0.9, 4.9), "galv")                           # the rinse section's pan: dilute medium to recovery
DR.box((0.3, 1.4, 0.1), (1.7, 1.8, 0.3), "padgreen")                        # the feed box
for z in (2.8, 3.5, 4.2):
	spray_bar(DR, 2, 1.65, z)
DR.pipe([(0.25, 1.65, 4.2), (0.1, 1.65, 4.2), (0.1, 1.65, 2.8), (0.25, 1.65, 2.8)], 2, "pipe_water")
DR.cylinder("x", (1.55, 2.0), 0.18, 1.8, 2.0, "blue", 10)                   # the vibrator motor, east side
DR.box((0.4, 0.4, 4.85), (1.6, 0.95, 5.0), "galv")                          # the discharge lip at the south end
motor_box(DR, 0.25, 0.1, 0.4, 0.35)
DR.collide((0.2, 0.0, 0.1), (1.8, 1.8, 4.9))

# ---- the medium recovery -----------------------------------------------------------------------------------------------------------
MR = model("medium_recovery", 2, 3, 3, "blue")
MR.box((0.0, 0.0, 0.0), (2.0, 0.2, 3.0), "concrete")
MR.box((0.2, 0.2, 0.3), (1.8, 0.9, 1.7), "steel")                           # the separator's tank
MR.cylinder("x", (0.95, 1.0), 0.45, 0.3, 1.7, "dark", 14)                   # the magnet drum, 900 mm, in the tank
MR.cylinder("x", (0.95, 1.0), 0.06, 0.15, 1.85, "iron", 6)                  # its shaft
MR.box((0.3, 0.6, 1.5), (1.7, 0.85, 2.0), "galv")                           # the magnetics' lip: ferrosilicon to the densifier
for x in (0.2, 1.6):
	for z in (1.9, 2.7):
		MR.box((x, 0.2, z), (x + 0.2, 2.3, z + 0.2), "blue")                # the densifier's stand
MR.cylinder("y", (1.0, 2.4), 0.3, 1.2, 2.0, "galv", 12)                     # the densifying cyclone over its sump
MR.cylinder("y", (1.0, 2.4), 0.12, 0.9, 1.2, "galv", 10)
MR.box((0.5, 2.3, 0.4), (1.5, 2.9, 1.4), "galv")                            # the ferrosilicon make-up hopper on top
MR.box((0.6, 2.9, 0.5), (1.4, 2.95, 1.3), "grating")
MR.cylinder("y", (1.0, 0.9), 0.12, 1.4, 2.3, "galv", 8)                     # its feeder down to the tank
MR.pipe([(1.0, 2.0, 2.4), (1.0, 2.6, 2.4), (1.0, 2.6, 3.0)], 4, "rubber")    # the corrected medium back to the module
motor_box(MR, 0.3, 0.1, 0.4, 0.35)
MR.collide((0.2, 0.0, 0.3), (1.8, 2.95, 2.9))

MODELS = [DS, DM, DR, MR]
