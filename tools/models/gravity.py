"""The gravity circuit at true size (docs/multiblocks/gravity-circuit.md), drawn facing north from its main cell:
- spiral_bank, 4 x 5 x 6: rows of spiral concentrators in a steel frame over their launders, a feed distributor on top, the 37 kW
  feed pump at the front.
"""
import tlib
from tlib import model, motor_box

SB = model("spiral_bank", 4, 5, 6, "galv")
SB.box((0.0, 0.0, 0.0), (4.0, 0.25, 6.0), "concrete")
SB.box((0.2, 0.25, 1.2), (3.8, 0.7, 5.8), "galv")                           # the launders
for x in (0.15, 3.75):
	for z in (1.15, 3.35, 5.55):
		SB.box((x, 0.25, z), (x + 0.1, 4.2, z + 0.1), "iron")               # the frame's columns
SB.box((0.15, 4.2, 1.15), (3.85, 4.3, 5.65), "grating")                     # the top deck
for cx in (1.0, 2.0, 3.0):
	for cz in (1.9, 3.5, 5.0):
		SB.cylinder("y", (cx, cz), 0.07, 0.7, 4.2, "iron", 8)               # a spiral's column
		for level in range(5):
			y = 0.95 + level * 0.65
			SB.ring("y", (cx, cz), 0.42, y, y + 0.14, 0.14, "rubber", sides=10)   # its turns
SB.cylinder("y", (2.0, 3.4), 0.5, 4.3, 4.9, "galv", 12)                     # the feed distributor
SB.cylinder("z", (1.0, 0.6), 0.32, 0.25, 0.95, "blue", 12)                  # the feed pump
SB.pipe([(1.0, 0.6, 0.95), (1.0, 4.6, 0.95), (1.0, 4.6, 3.4), (1.5, 4.6, 3.4)], 6, "rubber")   # up to the distributor
motor_box(SB, 0.4, 0.2)
SB.pipe([(1.5, 0.6, 0.0), (1.5, 0.6, 0.25)], 6, "rubber")                   # fines slurry in, the front
SB.pipe([(0.5, 0.45, 5.8), (0.5, 0.45, 6.0)], 6, "rubber")                  # the rest out, the back, west
SB.pipe([(3.5, 0.45, 5.8), (3.5, 0.45, 6.0)], 4, "rubber")                  # concentrate out, the back, east
SB.collide((0.0, 0.0, 0.0), (4.0, 4.3, 6.0))

MODELS = [SB]
