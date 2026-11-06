"""Phase 4's parts at true size (docs/multiblocks/full-recovery.md), each drawn facing north from its main cell."""
import tlib
from tlib import disc, model, motor_box


def tank(M, cx, cz, r, top, tex="grey"):
	M.ring("y", (cx, cz), r, 0.25, top, 0.08, tex, sides=18)
	disc(M, cx, cz, r, top - 0.3, top - 0.25, "dark")


SC = model("scavenger_bank", 5, 4, 9, "grey")
SC.box((0, 0, 0), (5, 0.25, 9), "concrete")
for cz in (1.2, 3.4, 5.6, 7.8):
	tank(SC, 2.5, cz, 1.05, 3.1)
	SC.ring("y", (2.5, cz), 1.15, 2.85, 3.1, 0.1, "galv", sides=18)
	SC.box((2.2, 3.1, cz - 0.3), (2.8, 3.8, cz + 0.3), "blue")
SC.box((2.3, 3.1, 0.2), (2.7, 3.2, 8.8), "grating")
motor_box(SC, 0.35, 0.15)
SC.pipe([(2.5, 0.6, 0), (2.5, 0.6, 0.2)], 6, "rubber")
SC.pipe([(4.5, 0.6, 8.8), (4.5, 0.6, 9)], 4, "rubber")
SC.pipe([(0.5, 0.6, 8.8), (0.5, 0.6, 9)], 6, "rubber")
SC.collide((0, 0, 0), (5, 3.2, 9))

RG = model("regrind_mill", 3, 3, 5, "grey")
RG.box((0, 0, 0), (3, 0.25, 5), "concrete")
RG.cylinder("z", (1.5, 1.35), 1.05, 0.7, 3.8, "iron", 18)
RG.box((0.9, 0.25, 0.3), (2.1, 1.1, 0.7), "concrete")
RG.box((0.9, 0.25, 3.8), (2.1, 1.1, 4.2), "concrete")
RG.cylinder("z", (1.5, 0.9), 0.4, 4.2, 4.9, "blue", 12)
motor_box(RG, 0.35, 0.15)
RG.pipe([(1.5, 0.6, 0), (1.5, 0.6, 0.3)], 6, "rubber")
RG.pipe([(0.5, 0.45, 4.8), (0.5, 0.45, 5)], 6, "rubber")
RG.collide((0.3, 0, 0), (2.7, 2.4, 5))

CL = model("cleaner_bank", 4, 3, 6, "grey")
CL.box((0, 0, 0), (4, 0.25, 6), "concrete")
for cx in (1.1, 2.9):
	for cz in (1.5, 4.5):
		tank(CL, cx, cz, 0.8, 2.1, "galv")
		CL.box((cx - 0.2, 2.1, cz - 0.2), (cx + 0.2, 2.7, cz + 0.2), "blue")
motor_box(CL, 0.35, 0.15)
CL.pipe([(2, 0.6, 0), (2, 0.6, 0.2)], 6, "rubber")
CL.pipe([(2, 0.5, 5.8), (2, 0.5, 6)], 4, "rubber")
CL.collide((0, 0, 0), (4, 2.2, 6))

LT = model("leach_tank", 5, 6, 5, "rubber")
LT.box((0, 0, 0), (5, 0.25, 5), "concrete")
tank(LT, 2.5, 2.5, 2.3, 5.2, "rubber")
LT.box((0.2, 5.2, 2.2), (4.8, 5.35, 2.8), "grating")
LT.box((2.1, 5.35, 2.1), (2.9, 5.9, 2.9), "blue")
motor_box(LT, 0.35, 0.15)
LT.pipe([(2.5, 0.6, 0), (2.5, 0.6, 0.3)], 6, "rubber")
LT.pipe([(4.7, 0.6, 2.5), (5, 0.6, 2.5)], 4, "galv")
LT.pipe([(2.5, 0.6, 4.7), (2.5, 0.6, 5)], 6, "rubber")
LT.collide((0.2, 0, 0.2), (4.8, 5.35, 4.8))

CC = model("ccd_thickener", 12, 4, 12, "grey")
CC.box((0, 0, 0), (12, 0.25, 12), "concrete")
tank(CC, 6, 6, 5.7, 3.0, "grey")
CC.box((0.3, 3.0, 5.7), (11.7, 3.2, 6.3), "grating")
CC.box((5.5, 3.2, 5.5), (6.5, 3.8, 6.5), "blue")
motor_box(CC, 0.35, 0.15)
CC.pipe([(6, 0.6, 0), (6, 0.6, 0.3)], 8, "rubber")
CC.pipe([(11.5, 0.6, 11.7), (11.5, 0.6, 12)], 6, "rubber")
CC.pipe([(0.5, 0.6, 11.7), (0.5, 0.6, 12)], 8, "rubber")
CC.collide((0.3, 0, 0.3), (11.7, 3.2, 11.7))

EW = model("electrowinning_bank", 5, 3, 9, "copper")
EW.box((0, 0, 0), (5, 0.25, 9), "concrete")
for z in (0.5, 2.7, 4.9, 7.1):
	EW.box((0.5, 0.25, z), (4.5, 1.7, z + 1.8), "rubber")
	for i in range(8):
		EW.box((0.8 + i * 0.45, 1.3, z + 0.1), (0.9 + i * 0.45, 2.1, z + 1.7), "copper")
	EW.box((0.5, 2.1, z + 0.8), (4.5, 2.2, z + 1.0), "copper")
EW.box((0, 0.25, 0), (0.45, 2.4, 0.5), "galv")
motor_box(EW, 0.35, 0.05)
EW.pipe([(2.5, 0.6, 0), (2.5, 0.6, 0.5)], 6, "rubber")
EW.collide((0, 0, 0), (5, 2.2, 9))

BP = model("byproduct_plant", 5, 5, 8, "grey")
BP.box((0, 0, 0), (5, 0.25, 8), "concrete")
BP.cylinder("z", (1.4, 1.3), 1.0, 0.4, 5.0, "iron", 16)          # the roaster
BP.cylinder("y", (3.6, 2.0), 0.9, 0.25, 3.6, "galv", 14)         # a leach vessel
BP.cylinder("y", (3.6, 5.2), 0.9, 0.25, 3.6, "grey", 14)         # a cementation vessel
BP.cylinder("y", (1.4, 6.6), 0.25, 0.25, 4.8, "iron", 8)         # the stack
BP.box((0.3, 0.25, 5.8), (2.6, 2.2, 7.8), "blue")                # the filter house
motor_box(BP, 0.35, 0.15)
BP.collide((0, 0, 0), (5, 3.6, 8))

HP = model("heap_pad", 10, 3, 10, "rubber")
HP.box((0, 0, 0), (10, 0.15, 10), "rubber")                      # the liner
for level in range(5):
	inset = 0.6 + level * 0.8
	HP.box((inset, 0.15 + level * 0.45, inset), (10 - inset, 0.6 + level * 0.45, 10 - inset), "concrete")   # the heap
for z in (2.5, 5.0, 7.5):
	HP.pipe([(1.0, 2.5, z), (9.0, 2.5, z)], 3, "rubber")          # the spray lines
motor_box(HP, 0.35, 0.05, 0.5, 0.4)
HP.pipe([(5, 0.3, 9.6), (5, 0.3, 10)], 6, "rubber")              # the drain
HP.pipe([(9.6, 0.5, 5), (10, 0.5, 5)], 4, "galv")                # acid in, east
HP.collide((0, 0, 0), (10, 2.4, 10))

MODELS = [SC, RG, CL, LT, CC, EW, BP, HP]
