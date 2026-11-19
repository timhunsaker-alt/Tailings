#!/usr/bin/env python3
"""Turns the Ludo sources in art/sound_source into the mod's mono .ogg files and sounds.json (docs/sound.md). Run from the repo root."""
import json
import os
import sys

import numpy as np
import soundfile as sf

SRC = "art/sound_source"
OUT = "common/src/main/resources/assets/tailings/sounds"
ONE_SHOTS = ("muck_tip", "muck_load", "ring_set", "car_couple", "blast_siren")
LOOPS = ("jaw_crusher", "cone_crusher", "vibrating_screen", "ball_mill", "belt_conveyor", "apron_feeder", "slurry_rush", "agitator",
	"pressure_filter", "ore_sorter", "tbm_cutter", "mine_locomotive", "gyratory_crusher", "sag_mill", "large_ball_mill", "mine_hoist", "xrt_sorter",
	"rotary_drill", "track_dozer", "rock_breaker", "slurry_pump", "tank_cell")

# loops whose Ludo take ends far from where it starts: the last 60 ms are cross-faded into the first, so the loop does not click
CROSSFADE = ("rotary_drill",)

os.makedirs(OUT, exist_ok=True)
for name in ONE_SHOTS + LOOPS:
	found = [f for f in os.listdir(SRC) if f.startswith(name + ".")]
	if not found:
		sys.exit(f"no source for {name}")
	data, rate = sf.read(os.path.join(SRC, found[0]), always_2d=True)
	mono = data.mean(axis=1)
	if name in ONE_SHOTS:
		loud = np.abs(mono) > 0.01
		first, last = np.argmax(loud), len(loud) - np.argmax(loud[::-1])
		mono = mono[max(0, first - 200):last + 200]
	if name in CROSSFADE:
		k = int(rate * 0.06)
		ramp = np.linspace(0, 1, k)
		mono = np.concatenate([mono[k:-k], mono[-k:] * (1 - ramp) + mono[:k] * ramp])
	mono = mono / np.abs(mono).max() * 0.9
	if name in ONE_SHOTS:
		fade = min(len(mono) // 20, int(rate * 0.02))
		mono[:fade] *= np.linspace(0, 1, fade)
		mono[-fade:] *= np.linspace(1, 0, fade)
	sf.write(f"{OUT}/{name}.ogg", mono, rate, format="OGG", subtype="VORBIS")
	rms = float(np.sqrt((mono ** 2).mean()))
	seam = float(abs(mono[0] - mono[-1]))
	step = float(np.abs(np.diff(mono)).mean())
	print(f"{name:18} {len(mono) / rate:5.2f} s  rms {rms:.3f}" + (f"  seam {seam:.3f} (typical step {step:.3f})" if name in LOOPS else ""))
with open("common/src/main/resources/assets/tailings/sounds.json", "w") as f:
	json.dump({n: {"sounds": [f"tailings:{n}"], "subtitle": f"subtitles.tailings.{n}"} for n in ONE_SHOTS + LOOPS}, f, indent=2)
	f.write("\n")
