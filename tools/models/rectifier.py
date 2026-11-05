"""The traction rectifier at true size (docs/multiblocks/haulage.md): a 2 x 3 x 2 mine traction substation, its transformer
section and rectifier cubicle side by side, louvred, the 480 V AC box on the west face of the main cell and the 250 V DC bus
leaving the east side at track height."""
import tlib
from tlib import model, motor_box

TR = model("traction_rectifier", 2, 3, 2, "grey")
TR.box((0.0, 0.0, 0.0), (2.0, 0.15, 2.0), "concrete")
TR.box((0.15, 0.15, 0.15), (1.85, 2.6, 0.95), "grey")                       # the transformer section
TR.box((0.15, 0.15, 1.05), (1.85, 2.6, 1.85), "grey")                       # the rectifier cubicle
TR.box((0.1, 2.6, 0.1), (1.9, 2.72, 1.9), "grey")                           # the roof
for z in (0.25, 1.15):
	TR.decal((1.85, 1.6, z), (1.86, 2.4, z + 0.6), "louvre", "east", "grey")     # ventilation louvres
TR.decal((1.85, 0.6, 1.25), (1.86, 1.2, 1.65), "ratingplate", "east", "grey")
TR.decal((0.6, 1.6, 0.14), (1.1, 2.1, 0.15), "danger", "north", "grey")
TR.box((1.85, 0.3, 1.3), (2.0, 0.45, 1.6), "copper")                        # the DC bus out to the track
motor_box(TR, 0.4, 0.3, 0.6, 0.45)
TR.collide((0.1, 0.0, 0.1), (1.9, 2.72, 1.9))

MODELS = [TR]
