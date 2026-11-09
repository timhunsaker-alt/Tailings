# Open pit P2b, the haul dispatch office — decided while Tim was away (2026-11-09)

The spec (open-pit.md, "Haul roads, surveying and dispatch") was approved; these were open in it and were decided on the best
recommendation while building (PROGRESS.md, "Open pit P2b"). The new numbers are marked *(proposed)* in the spec.

### 1. How a route gets onto the office
- **Chosen:** click the office with the survey book: the book's route becomes the next free route (1 to 8). Sneak-click with the
  same book takes it off. The chat says which route number it got.
- **Why:** the book already carries the route to trucks and the loader, so it is the one tool for routes; a dispatcher's system
  is set up from the survey of the haul roads.
- **Alternatives:** a screen on the office to pick slots and edit routes (more work, and a GUI for 8 lists of stakes); or trucks
  join the office's route by themselves when they are near it (no control over which route number a PLC sees).

### 2. Which trucks are on a route
- **Chosen:** a truck is on route N when its stakes are route N's stakes, in the same order or reversed. Trucks report to the office
  once a second; TRUCKS counts the ones heard in the last 2 s.
- **Why:** a truck's route already is its stake list (set by the same book), so nothing new to set and nothing to get out of step.
- **Alternatives:** assign trucks to routes at the office (dispatch moving trucks between routes, as real fleet systems do: a
  bigger wave), or match on the route's ends only (looser: two roads to the same dump would be one route).

### 3. What the office does when the PLC stops sending RUN
- **Chosen:** in AUTO, a route's RUN that is not refreshed within Wildcatter's stale time counts as off: the route parks. HAND runs
  every route as if there were no office; OFF parks them all. A new office is in HAND.
- **Why:** the ControlDevice contract says a device falls back when commands stop; Wildcatter's starters stop on a stale command,
  so a cut cable, a dead PLC and STOP look alike. For trucks the safe state is parked, not running with nobody able to stop them.
  HAND keeps a world without a PLC playing exactly as P2a.
- **Alternatives:** keep the last state (trucks run on after the PLC dies: not fail-safe); stop where they are on the road (blocks
  the road, and a stopped PLC is not an emergency).

### 4. What a truck does when it loses the radio
- **Chosen:** if its office has no power (under 85 % of the low tier's nominal) or the truck is over 2 km away, it stops at its next
  stake and reads "no word from dispatch: ..."; when the radio is back it goes on. A route no office has runs as in P2a.
- **Why:** real autonomous haul trucks stop when they lose their link to dispatch. Stopping at the next stake rather than at once
  keeps it out of turns and junctions; without the radio it cannot be sent to PARK.
- **Alternatives:** run on as in P2a (an unpowered office then cannot stop anything); go to PARK anyway (it has no way of hearing
  that it should).

### 5. Where a truck parks
- **Chosen:** RUN off, it drives on along its route and stops at the route's first PARK stake, engine off. If the PARK stake is
  behind it, it finishes its trip (a loaded truck tips first; one at LOAD leaves at once, empty) and parks on the way back. One
  turning, backing in or tipping at an end finishes that first. A route with no PARK stake stops at the next stake. RUN on, it goes
  on from the stake it parked at.
- **Why:** trucks only turn round at the stake before an end (P2a), so they cannot reverse direction mid-road; a loaded truck
  should not stand loaded. Parking where it is heading keeps the route's order of trucks.
- **Alternatives:** turn round on the spot to reach a PARK stake behind (needs open ground anywhere on the route); park off the road
  in a bay beside the stake (a new kind of manoeuvre: a later wave).

### 6. TRUCKS range
- **Chosen:** 4-20 mA over 0-16 trucks: 4 mA and 1 mA a truck *(proposed)*, held at 20 above 16.
- **Why:** 1 mA a truck reads at a glance on any PLC; a route with one loader (about 14 loads an hour, below) is kept busy by 4-6
  trucks on a real haul, so 16 is plenty.
- **Alternatives:** 0-8 (more resolution, too tight for long hauls) or 0-20 (the most a fleet might put on one road; 0.8 mA a truck).

### 7. TONNES range
- **Chosen:** 4-20 mA over 0-2,000 t tipped in the last hour (72,000 ticks) *(proposed)*.
- **Why:** a route is limited by its loader: 5 passes of 40 s plus about a minute to spot a truck is about 14 loads an hour,
  some 1,300 t/h with 91 t loads. 2,000 t/h covers that with room. A real 100 t truck on a 2-3 km haul moves about 300 t/h.
- **Alternatives:** 0-1,000 t/h (more resolution, but a busy route pins it at 20 mA); 0-5,000 t/h (for when the 363 t truck and
  shovels come in P5: may want re-ranging then, or a per-route range set from the PLC with `setRange`).

### 8. The office's power
- **Chosen:** a 5 kW *(proposed)* single-phase load on Wildcatter's low tier (120 V or 230 V by the world's standard, never written
  in the code), landed on the electrical box on the west face of its main cell, as the control supply is. Its radio drops out under
  85 % of nominal, the control supply's figure.
- **Why:** a dispatch office is radios, computers, lights and air conditioning: a few kW on the building's low-voltage supply.
- **Alternatives:** 120/208 V three-phase (Wildcatter's industrial tier is the 480 V class, too big for an office); no power at
  all (then it could never lose its radio).

### 9. RUNNING
- **Chosen:** on while at least one truck heard on the route is working: not parked, not halted on a problem, not dry.
- **Why:** it is the feedback for RUN, as a motor's RUNNING is for its starter: RUN on with RUNNING off is a fault to alarm.
- **Alternatives:** on while RUN is on (no feedback at all); on only while every truck works (one dry truck drops the route).

### 10. Chunks
- **Chosen:** an office with routes keeps its own chunk working with the `tailings:runner` ticket; a truck parked by dispatch keeps
  its chunks as a truck on a route does (its engine is off; its ticket is not).
- **Why:** otherwise a parked truck far from players unloads and never hears RUN come back on, and an office out of loaded chunks
  drops off the radio.
- **Alternatives:** let parked trucks unload (RUN on would not reach them until a player came by).

### 11. A truck backing in at an angle (a P2a fix)
- **Chosen:** backing in, a truck whose tail passes the end along the line, within 4 m of the line, stops there and fills or tips.
- **Why:** on a straight road the turn round at the stake before an end leaves the truck about 15 m to one side; it backed in at an
  angle, missed the end stake by more than 1.5 m and circled behind it for ever (found writing `pit_dispatch`).
- **Alternatives:** make the turn come back onto the line before backing (a three-point turn: better, more work); or require a
  bend at the turn stake, as `pit_truck_haul` has.

### 12. Placeholder recipe
- **Chosen:** iron ingot, lightning rod, iron ingot / glass pane, redstone block, glass pane / smooth stone, iron door, smooth stone.
- **Why:** the mast, the windows, the electronics and the door; until the art wave's recipes from Wildcatter parts.
- **Alternatives:** Wildcatter's own parts (a control panel, a cable), when the recipes are redone.
