# Heavy Iron fixes (branch `heavy-iron-fixes`, 2026-11-22)

Decided while Tim was away, on the best recommendation; each is open for review. The weak spots come from the series plan
(`videos/longform/heavy-iron/PLAN.md`, the "Weak" notes for episodes 3, 4, 5 and 7). Format as in [README.md](README.md).

## Fix 1: the blast (episode 3)

### 1. Every hole throws fragments, not only a bad one
- **Chosen:** each firing hole throws 13 drawn fragments (8 off its face, 5 out of its collar) that fly, strike and land as dust; a bad
  hole's flyrock blocks (blowout 6, short burden 3, overcharge 3) are kept as they were and are now drawn in the air too.
- **Why:** real blasts throw some rock from every hole; the episode needs to show it. Fragments land as dust, not blocks, so a good
  shot does not litter the pit with muck 100 m away (P3 decision 12 kept: only a bad hole's flyrock lands as muck).
- **Alternatives:** every fragment landing as a muck block (litter on every shot); fragments only from bad holes (nothing to film).

### 2. The reach follows Richards and Moore, with k = 13.5
- **Chosen:** face burst L = k²/g (√m/B)^2.6 and cratering L = k²/g (√m/SH)^2.6 (Richards and Moore 2004), k = 13.5, each held to
  the game's 100 m. The default hole (69 kg/m, 4 m of stemming) reaches the 100 m cap; 6 m of stemming 43 m, 8 m 20 m.
- **Why:** it is the rule real blasters use to size a clearance; it is scaled by the charge a metre, the burden (so the powder factor)
  and the stemming, as asked. k's fitted range (13.5-27) is *unverified* here; the low end keeps a well-stemmed shot inside the cap.
- **Alternatives:** k = 27 (every default shot at the cap whatever the stemming); a made-up linear scale on powder factor.

### 3. Anyone in the open within reach is struck once a shot
- **Chosen:** as each hole fires, one fragment is aimed at each player or mob standing in the open within that hole's ballistic reach
  (the face's speed within 60 degrees in front of it, the collar's any way); it hits for 10 (falling-block damage: a helmet counts),
  once a shot whoever fires first. A roof over the head stops it. Random fragments also hit what they cross.
- **Why:** so clearing the zone, or taking cover, matters every time, and the game tests can measure it; a real clearance treats any
  point within flyrock range as struck. *Needs Tim:* this is harsher than chance; a 20 hp villager 90 m behind a default shot loses
  half its health. Cows in the zone die.
- **Alternatives:** chance only (a player at 80 m almost never hit, so the siren would not matter); a chance falling with distance.

### 4. The danger zone, the firing point and the siren
- **Chosen:** ARM reads "flyrock to N m: clear N m" (the zone is the farther of the flyrock reach and the blast's 50 m) and warns when
  the firing point is inside it; FIRE's player check uses the zone; through the siren each player in the zone gets a countdown and
  "clear the zone or take cover" each second. The firing point's 50 m minimum is unchanged.
- **Why:** the siren and clearing now have a figure behind them. Keeping the 50 m firing point and telling the blaster to fire from
  cover is real practice (a blast shelter).
- **Alternatives:** a firing point held outside the flyrock reach (100 m for a default shot; the lead line allows 500 m).

### 5. The muck pile slumps to its angle of repose
- **Chosen:** after the last hole the broken rock rolls off its face by the settle rule (37 degrees), top down, a few thousand cells a
  tick; the swell still spills off the face.
- **Why:** the box of muck stood as a vertical wall, which reads as unbroken rock on camera. The throw forward (real piles run out one
  to two bench heights) is not modelled: it would move most of the shot and change where a shovel finds it.
- **Alternatives:** throwing the front rows' muck forward (a bigger change to where the muck is); a lumpy muck model (thousands of
  non-full blocks cost frames).

## Weak on camera, left as is
- Fragments are small at a long shot's distance; film the blast from 60-80 m.
- No camera shake; the blast sound is still vanilla's explosion.
- `pit_dozer_cut` and other known intermittent scenes are untouched.
