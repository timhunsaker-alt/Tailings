"""The mine trains' later parts at true size, each drawn facing north from its own main cell:
- track_grizzly, 1 x 1 x 4 (bottom-dump-cars.md, L3): a rail bridge over a dump pocket, its steel bearers on concrete curbs and its
  bars 300 mm apart between and under the rails; the mine rail runs on top of it;
- dump_cam_rail, 1 x 1 x 4 (L3): the cam rail beside the track grizzly that opens each bottom-dump car's doors as it passes, rising at
  its leading end, and the closing ramp at its far end that shuts them;
- battery_charger, 2 x 2 x 1 (battery-locomotive.md, L2): a traction battery charger cabinet on a wall, its louvres, meter and
  controls on the front, and its three-phase 30 A plug and cable at its west end, where the receptacle is.
"""
import tlib
from tlib import model

# ---- the track grizzly ------------------------------------------------------------------------------------------------------------
TG = model("track_grizzly", 1, 1, 4, "iron")
TG.box((0.0, 0.0, 0.0), (0.15, 1.0, 4.0), "concrete")                      # the curbs the bridge sits on
TG.box((0.85, 0.0, 0.0), (1.0, 1.0, 4.0), "concrete")
for z in (0.0, 1.3, 2.7, 3.85):
	TG.box((0.15, 0.7, z), (0.85, 0.85, z + 0.15), "iron")                 # the cross bearers
for x in (0.15, 0.45, 0.75):
	TG.box((x, 0.85, 0.0), (x + 0.1, 1.0, 4.0), "steel")                    # the grizzly bars, 300 mm apart
TG.collide((0.0, 0.0, 0.0), (0.15, 1.0, 4.0))
TG.collide((0.85, 0.0, 0.0), (1.0, 1.0, 4.0))
TG.collide((0.15, 0.85, 0.0), (0.85, 1.0, 4.0))

# ---- the dump cam rail ------------------------------------------------------------------------------------------------------------
CR = model("dump_cam_rail", 1, 1, 4, "iron")
CR.box((0.1, 0.0, 0.0), (0.9, 0.15, 4.0), "concrete")                      # its footing
for z in (0.2, 1.4, 2.6, 3.6):
	CR.box((0.4, 0.15, z), (0.6, 0.45, z + 0.2), "iron")                    # its posts
for i, h in enumerate((0.3, 0.4, 0.5)):                                      # the lead-in that lifts the car's cam roller
	CR.box((0.3, 0.45, i * 0.25), (0.7, 0.45 + h * 0.2, i * 0.25 + 0.25), "steel")
CR.box((0.3, 0.45, 0.75), (0.7, 0.55, 3.25), "steel")                       # the cam bar, holding the doors open
for i, h in enumerate((0.55, 0.7, 0.85)):                                    # the closing ramp, which pushes them shut
	CR.box((0.3, 0.45, 3.25 + i * 0.25), (0.7, h, 3.5 + i * 0.25), "blue")
CR.collide((0.1, 0.0, 0.0), (0.9, 0.55, 4.0))

# ---- the battery charger ----------------------------------------------------------------------------------------------------------
BC = model("battery_charger", 2, 2, 1, "grey")
BC.box((0.0, 0.0, 0.85), (2.0, 2.0, 1.0), "galv")                           # its mounting frame on the wall
BC.box((0.15, 0.2, 0.35), (1.85, 1.85, 0.85), "grey")                       # the cabinet
BC.decal((0.3, 1.2, 0.34), (1.0, 1.7, 0.35), "louvre", "north", "grey")     # its cooling louvres
BC.decal((1.1, 1.3, 0.34), (1.6, 1.7, 0.35), "gauge", "north", "grey")      # the charge meter
BC.decal((1.1, 0.7, 0.34), (1.6, 1.2, 0.35), "controller", "north", "grey") # the controls
BC.decal((0.3, 0.5, 0.34), (0.9, 0.9, 0.35), "ratingplate", "north", "grey")
BC.box((0.0, 0.3, 0.45), (0.15, 0.6, 0.75), "rubber")                       # the three-phase 30 A plug's cord, to the west
BC.box((1.85, 0.4, 0.4), (2.0, 1.6, 0.8), "rubber")                         # the charging cable coiled on its hook
BC.collide((0.15, 0.2, 0.35), (1.85, 1.85, 1.0))
BC.collide((0.0, 0.0, 0.85), (2.0, 2.0, 1.0))

MODELS = [TG, CR, BC]
