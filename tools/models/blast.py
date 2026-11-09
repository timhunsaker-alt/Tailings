"""The open pit's blasting parts (docs/multiblocks/open-pit-p3-blasting.md), drawn facing north from the main cell:
- matrix_fill_point, 1 x 2 x 1: the emulsion truck's filling point: a standpipe on a concrete plinth, fed by pipe on any face, with
  its valve, meter and hose hung on a bracket.
"""
import tlib
from tlib import model

M = model("matrix_fill_point", 1, 2, 1, "galv")
M.box((0.05, 0.0, 0.05), (0.95, 0.2, 0.95), "concrete")                    # the plinth
M.box((0.35, 0.2, 0.35), (0.65, 1.7, 0.65), "galv")                        # the standpipe
M.box((0.3, 0.6, 0.3), (0.7, 0.75, 0.7), "steel")                          # its flange
M.box((0.28, 1.0, 0.28), (0.72, 1.3, 0.72), "hipaint")                     # the valve body
M.box((0.45, 1.3, 0.1), (0.55, 1.36, 0.9), "danger")                       # the valve handle
M.decal((0.36, 0.8, 0.349), (0.64, 0.98, 0.35), "gauge", "north", "galv")  # its meter
M.box((0.35, 1.7, 0.35), (0.65, 1.8, 0.95), "galv")                        # the gooseneck
M.box((0.42, 0.7, 0.85), (0.58, 1.8, 0.95), "rubber")                      # the hose hanging from it
M.box((0.4, 0.55, 0.82), (0.6, 0.75, 0.98), "steel")                       # the coupling
M.box((0.05, 1.9, 0.05), (0.95, 2.0, 0.95), "hipaint")                     # a high-visibility cap, to find it from a truck cab
M.collide((0.05, 0.0, 0.05), (0.95, 0.2, 0.95))
M.collide((0.3, 0.2, 0.3), (0.7, 1.8, 0.7))

MODELS = [M]
