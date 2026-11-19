"""The large electric rotary blasthole drill's four parts (docs/multiblocks/open-pit-p3-blasting.md, As built: four parts), drawn as
they stand built, each facing north in its own frame (x east, y up, z south from its main cell's north-west bottom corner), as the rope
shovel's parts in pit.py. The machine's front, its cab end, is north; the mast stands at its back end over the hole.

- drill_crawlers, 7 x 2 x 10: the two crawler tracks on their frame, the car body between them, and the three levelling jacks (one at
  the front, two at the back corners).
- drill_deck, 7 x 4 x 15: the main frame; the machinery house (the electric drive and its transformer, the hydraulic pumps); the
  screw compressor for the flushing air behind it; the cab at the front left corner; the mast table at the back end, up to the mast's
  foot; the trailing cable's reel at the back right with its danger sign.
- drill_mast, 3 x 20 x 3: the lattice mast's four chords and lacing, the rotary head on its pull-down chains, the drill pipe down its
  middle (over the hole) and the crown at its top: 26 m to the top, standing on the deck.
- drill_dust_collector, 2 x 3 x 3: the collector's body and its filter cartridges, its fan on top and the duct to the deck bushing's
  skirt at the mast's foot (on its east face).
"""
from tlib import model

# ---- the crawlers -----------------------------------------------------------------------------------------------------------------
C = model("drill_crawlers", 7, 2, 10, "steel")
for x0 in (0.0, 5.7):
	C.box((x0, 0.0, 0.2), (x0 + 1.3, 0.3, 9.8), "rubber")                  # the crawler shoes' lower run
	C.box((x0 + 0.15, 0.3, 0.6), (x0 + 1.15, 1.3, 9.4), "steel")           # the crawler frame
	C.box((x0, 1.3, 0.2), (x0 + 1.3, 1.6, 9.8), "rubber")                  # its upper run
	C.box((x0 + 0.1, 0.3, 0.0), (x0 + 1.2, 1.4, 0.8), "iron")              # the idler at the front
	C.box((x0 + 0.1, 0.3, 9.2), (x0 + 1.2, 1.4, 10.0), "iron")             # the drive tumbler at the rear
C.box((1.3, 0.5, 2.0), (5.7, 1.6, 8.0), "steel")                           # the car body between them
C.box((0.8, 1.6, 0.4), (6.2, 2.0, 9.6), "galv")                            # the frame's top, under the deck
C.box((3.0, 0.0, 0.0), (4.0, 1.6, 0.6), "hipaint")                         # the front levelling jack
for x in (1.4, 4.8):
	C.box((x, 0.0, 9.4), (x + 0.8, 1.6, 10.0), "hipaint")                  # the two back jacks
C.decal((2.2, 0.8, 1.99), (4.8, 1.4, 2.0), "nameplate", "north", "steel")
C.collide((0.0, 0.0, 0.2), (7.0, 2.0, 9.8))

# ---- the deck ---------------------------------------------------------------------------------------------------------------------
D = model("drill_deck", 7, 4, 15, "grey")
D.box((0.0, 0.0, 0.0), (7.0, 0.6, 15.0), "steel")                          # the main frame
D.box((3.0, 0.6, 2.2), (6.8, 3.6, 10.4), "grey")                           # the machinery house: the drive, its transformer, the pumps
D.box((2.9, 3.6, 2.1), (6.9, 3.8, 10.5), "galv")                           # its roof
for z in (3.0, 6.0):
	D.decal((6.8, 1.6, z), (6.81, 3.2, z + 2.0), "louvre", "east", "grey")
D.box((4.0, 3.8, 6.0), (6.0, 4.0, 9.0), "radfin")                          # the transformer's cooler on the roof
D.box((0.2, 0.6, 0.2), (2.6, 3.8, 3.0), "hipaint")                         # the cab at the front left corner
D.decal((0.4, 2.0, 0.19), (2.4, 3.4, 0.2), "gauge", "north", "hipaint")    # its front window and panel
D.box((3.2, 0.6, 10.4), (6.6, 2.8, 12.2), "dark")                          # the screw compressor for the flushing air
D.box((1.4, 0.6, 12.0), (5.0, 4.0, 15.0), "hipaint")                       # the mast table, up to the mast's foot
D.box((5.4, 0.6, 12.6), (6.9, 2.6, 14.8), "iron")                          # the trailing cable's reel at the back right
D.decal((6.9, 1.0, 13.0), (6.91, 2.2, 14.4), "danger", "east", "iron")
D.box((0.1, 0.6, 3.4), (0.3, 1.6, 11.8), "galv")                           # the walkway's handrail down the left side
D.decal((3.2, 1.6, 2.19), (5.2, 2.6, 2.2), "nameplate", "north", "grey")
D.collide((0.0, 0.0, 0.0), (7.0, 0.6, 15.0))
D.collide((3.0, 0.6, 2.2), (6.8, 3.8, 10.4))
D.collide((0.2, 0.6, 0.2), (2.6, 3.8, 3.0))
D.collide((1.4, 0.6, 10.4), (6.9, 4.0, 15.0))

# ---- the mast ---------------------------------------------------------------------------------------------------------------------
M = model("drill_mast", 3, 20, 3, "hipaint")
for x, z in ((0.0, 0.0), (2.6, 0.0), (0.0, 2.6), (2.6, 2.6)):
	M.box((x, 0.0, z), (x + 0.4, 19.4, z + 0.4), "hipaint")                # the four chords
for y in range(2, 19, 2):
	M.box((0.4, y, 0.05), (2.6, y + 0.2, 0.3), "hipaint")                  # the lacing on its front, back and sides
	M.box((0.4, y, 2.7), (2.6, y + 0.2, 2.95), "hipaint")
	M.box((0.05, y, 0.4), (0.3, y + 0.2, 2.6), "hipaint")
	M.box((2.7, y, 0.4), (2.95, y + 0.2, 2.6), "hipaint")
M.box((1.3, 0.0, 1.3), (1.7, 16.0, 1.7), "galv")                           # the drill pipe, over the hole
M.box((0.5, 16.0, 0.5), (2.5, 17.6, 2.5), "iron")                          # the rotary head on its chains
M.box((1.35, 17.6, 1.35), (1.65, 19.4, 1.65), "dark")                      # the pull-down chains over it
M.box((0.0, 19.4, 0.0), (3.0, 20.0, 3.0), "hipaint")                       # the crown
M.box((0.3, 0.0, 0.3), (2.7, 0.6, 2.7), "dark")                            # the deck bushing and the bit's breakout table at its foot
M.collide((0.0, 0.0, 0.0), (3.0, 20.0, 3.0))

# ---- the dust collector -----------------------------------------------------------------------------------------------------------
K = model("drill_dust_collector", 2, 3, 3, "grey")
K.box((0.1, 0.0, 0.2), (1.9, 2.2, 2.8), "grey")                            # its body, the filter cartridges inside
for z in (0.6, 1.4, 2.2):
	K.box((0.0, 0.6, z), (0.1, 1.8, z + 0.4), "galv")                      # the cartridges' access doors on its west face
K.box((0.4, 2.2, 0.7), (1.6, 2.9, 2.3), "radfin")                          # the fan on top
K.box((1.9, 0.2, 1.1), (2.0, 0.8, 1.9), "galv")                            # the duct to the skirt at the mast's foot
K.box((0.3, 0.0, 0.0), (1.7, 0.4, 0.2), "dark")                            # the dump valve for the fines
K.collide((0.1, 0.0, 0.2), (1.9, 2.9, 2.8))

MODELS = [C, D, M, K]
