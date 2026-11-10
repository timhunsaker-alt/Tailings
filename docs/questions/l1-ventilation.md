# L1, ventilation of the drive (2026-11-09): decisions taken while Tim was away

Tim (2026-11-09): "make decisions best recommendation and complete the rest of these two mods". The spec is the draft
[../multiblocks/ventilation.md](../multiblocks/ventilation.md); its "As built" lists what was built. The lead's rule for this wave:
**nothing may stop, slow or harm machines or players in existing worlds without Tim's say-so.** Entries 1 and 2 are the ones that need
Tim. Format as in [README.md](README.md).

### 1. Does a TBM stop, and do players suffer, without ventilation? (needs Tim)
- **Chosen:** not by default. Both rules are in `config/tailings-ventilation.properties`, and both are off unless the file says
  `interlock=true` (the TBM stops NO VENTILATION after 60 s with no duct air at its back-up) or `stale_air=true` (Mining Fatigue I
  after 30 s of stale air, Weakness I after 2 minutes). With the file absent, as in every world today, ventilation is a readout: the
  cab says `air 0.00/0.30 m/s POOR`, the readout `POOR AIR: 0.00 m/s (0.0 m3/s), needs 0.30`, the junction has an AIR AT BACK-UP
  signal, and the gas detector reads STALE AIR and RADON.
- **Why:** the spec (L1-1 in specs-tailings.md) makes ventilation a requirement, and the lead's hard rule is that existing worlds keep
  running. Every TBM driving today has no fan, so the interlock on by default would stop all of them a minute after the update.
- **Recommendation for Tim:** turn the interlock on for new worlds once a drive's fan and duct are in the playthrough; leave stale air
  off (or on only for servers that want the hardship). A per-world default could replace the server-wide file (entry 2).
- **Alternatives:** the spec as written (on for everyone, breaks running drives); on with a long grace (an hour) so players see the
  warning first; readouts only, forever.

### 2. A config file, not a gamerule (needs Tim if he wants per-world)
- **Chosen:** `config/tailings-ventilation.properties`, read once per server run, beside `tailings-train.properties`.
- **Why:** Tailings has one config file and no gamerule (DESIGN "Config"); this follows the pattern. A test turns the rules on for its
  own machine or player only (`TbmCore.setTestInterlock`, `VentNetwork.TEST_STALE`), so scenes running side by side are not affected.
- **Alternatives:** a Tailings gamerule (per world, `/gamerule` in game; needs a registration on both loaders); per-world saved data.

### 3. The fan's curve
- **Chosen:** a square-law curve from 3 kPa at shut-off to 25 m³/s in free air, scaled by the fan laws for its motor's speed: 15 m³/s
  at 1.9 kPa, its duty point on 3 km of duct. The spec's "15 m³/s at up to 3 kPa" is read as "a 15 m³/s fan whose pressure tops out at
  3 kPa".
- **Why:** with the spec's duct (Darcy, f = 0.02, 1 % leakage a 100 m) a fan held at 15 m³/s reaches 10.8 m³/s at 3.27 km on leakage
  alone, and a booster then could not help, against the spec's "past that, a booster adds its 1.5 kPa". On a curve, one fan gives
  11.3 m³/s at 3 km and 10.3 at 3.5 km; with the booster, 11.7 at 3.5 km: the spec's numbers.
- **Alternatives:** a fixed 15 m³/s fan with pressure-driven leakage (closer to McPherson's leaky-duct model, a solver of its own).

### 4. How a booster counts
- **Chosen:** in series with the main fan, the same air through both (the air leaked before it is not taken off its share): shut-off
  1.5 kPa, 25 m³/s free, 30 kW. It counts only when the duct comes into its north face; turned the other way the fan reads "a booster
  fan faces against the air" and the line stops at it.
- **Why:** simple, and enough for the spec's "3.5 km needs a booster".
- **Alternatives:** solve the duct in segments between fans (the booster's place along the line would matter).

### 5. A line, not a network
- **Chosen:** a fan reads its duct as one line out of its south face; at a branch the farther end is the line's end and the rest is not
  counted. Two fans on one duct each read it.
- **Why:** the spec leaves the network solver (Hardy Cross), regulators and doors out.
- **Alternatives:** the network solver (spec "Left out").

### 6. The duct is drawn a metre round
- **Chosen:** `vent_duct` is a block a metre, joining like Wildcatter's pipe (an arm toward each duct and fan beside it); it is drawn and
  collides as a metre round with a hanger band, not the real 1.4 m.
- **Why:** 1.4 m does not fit a block. The friction is worked at 1.4 m.
- **Alternatives:** a 2 x 2 duct (a big part per metre; four times the blocks).

### 7. Where the cassette pays out
- **Chosen:** at the crown of the bore's left side (across 1, up 6), at row 27, just behind the conveyor gantry (the back-up's tail and
  the loading point's start), a metre each block advanced. Down the decline the crown drops a block every two slices; there a second
  metre joins the two. A cell already taken is skipped.
- **Why:** real cassettes ride the back-up and the duct hangs from the crown behind it; the crown corner is clear of the gantries
  (4 m high) and trains, and of the machine's own clearance check.
- **Alternatives:** pay out from the reel gantry's row (the duct would run over the conveyor gantry).

### 8. Loading a cassette
- **Chosen:** right-click the coupled reel gantry with a `vent_duct_cassette`: it goes on when the one aboard has under 10 m left, and
  the old one's last metres are paid out first. Not a slot in the cab.
- **Why:** a new cab slot would move the muck slots' numbers in every saved TBM and its screen.
- **Alternatives:** a cab slot (changes saved inventories); top-up at any time (wasteful of the 100 m figure).

### 9. The interlock holds, it does not latch
- **Chosen:** with the interlock on, after 60 s (`TbmCore.AIR_GRACE_TICKS`) of poor air while driving it holds, NO VENTILATION, and
  drives on by itself once the air at the back-up is back.
- **Why:** like waiting for power; the player does not have to press Go again after mending a duct.
- **Alternatives:** latch it (a real gas trip needs a reset at the cab).

### 10. What counts as fresh, and as a drive
- **Chosen:** a place is fresh if a fan's line ends within 30 m of it (straight line) or its duct passes within 5 m of it, and that
  line delivers 10.8 m³/s (0.3 m/s over 36 m²). A player is in a drive when out of sight of the sky and on (or up to 2 blocks over) the
  TBM's floor, walkway, slope, rail or switch. A cave the bore breaks into is therefore not a drive (spec: "the cave is not").
- **Why:** cheap to ask (a map lookup), and it matches the spec's "portal to the duct's end plus 30 m".
- **Alternatives:** distance along the bore (needs the bore's path); every underground place (would touch players far from any drive).

### 11. Radon
- **Chosen:** a reading only: STALE AIR in a drive with tuff or sandstone within about 4 m (sampled every 2 m) reads RADON on the gas
  detector and the cab. It has no effect of its own.
- **Why:** the spec names it as a reading; no dose model is specified.
- **Alternatives:** a dose that adds to stale air's effects.

### 12. Clearing the effects
- **Chosen:** 5 s in fresh air takes off Mining Fatigue and Weakness that stale air gave (short, ambient effects only; a potion's or a
  beacon's is left). The effects are given 5 s at a time while stale.
- **Why:** inside the spec's "within 10 s"; never touches effects the player got elsewhere.
- **Alternatives:** let them run out.

### 13. The fan runs round the clock and keeps its chunk
- **Chosen:** a ventilation fan asks for its motor whenever it is built (its selector, PLC and power still rule), reads its line once
  a second, and while it has duct and runs it keeps its chunk ticking with the `tailings:runner` ticket. Its line (the duct cells and
  boosters) is saved with it; a cell in a chunk that is not loaded is taken as last read.
- **Why:** real forcing fans run continuously; a 3 km line runs through chunks nobody is in, and must still reach the far TBM after a
  restart.
- **Alternatives:** read only loaded chunks (a far drive would lose its air when the middle unloads).

### 14. PLC points
- **Chosen:** the fan has the motor points (RUN, RUNNING, STALLED, AUTO, AMPS, FILL) and FLOW (4-20 mA over 0-15 m³/s) as its seventh;
  the cable junction adds AIR AT BACK-UP (4-20 mA over 0-15 m³/s) as its eleventh, after its existing ten.
- **Why:** the spec's points, appended so an existing PLC's wiring keeps its numbers.
- **Alternatives:** none considered.

### 15. The gas detector
- **Chosen:** a `gas_detector` item: held in a drive it shows the reading each second on the action bar; a click reads it once.
- **Why:** the spec's `gas_detector`; it only reads.
- **Alternatives:** a block-mounted monitor on the PLC.

### 16. The cab screen loses its title
- **Chosen:** the TBM screen's readout starts where its title was, to fit a fifth line, `air 0.33/0.30 m/s, duct 64 m`.
- **Why:** the toggles sit 47 px under the old first line; a wrapped status line already filled it.
- **Alternatives:** a taller screen (moves every slot).

### 17. Recipes (placeholders)
- **Chosen:** fan: steel, copper, iron bars and an industrial cable; booster: the same without the middle bars; 8 duct from 6 yellow
  wool and 2 string; a cassette from 8 duct round an iron block; the detector from yellow dye, redstone, a glass pane and copper.
- **Why:** in step with the other placeholder recipes (Wildcatter steel and cable for motor parts).
- **Alternatives:** Wildcatter's PVC (it has no PVC sheet yet).

### 18. The tests' shortcuts
- **Chosen:** `vent_booster` lays 15 m of duct and adds 3,482 m as test-only length (`VentState.setExtraMetres`, not saved);
  `vent_cassette` loads a real 100 m cassette and then sets it to 6 m; the interlock scene uses a 3 s grace.
- **Why:** 3.5 km of duct, 100 advances and a minute's grace do not fit a game test.
- **Alternatives:** none; the full lengths are in `AirflowTest`.

### 19. Art
- **Chosen:** three Ludo pictures (3 credits): the duct's yellow fabric (block texture) and the cassette's and detector's icons; the fan
  and booster drawn in Wildcatter's materials by `tools/models/vent.py`.
- **Why:** art-wave.md's pipeline; no placeholders left.
- **Alternatives:** none.
