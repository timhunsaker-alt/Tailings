# Machine sound (R8, Tim's request, 2026-11-06)

The rules are Wildcatter's (`wildcatter/docs/kinetics-and-sound.md`): a looping sound plays while a machine runs and fades out when it
stops; one-shots mark events. Sources come from Ludo, are kept in `art/sound_source/`, and `tools/process_sounds.py` makes the `.ogg`s
and `sounds.json`. Their prompts are in [ART.md](ART.md).

**When a plant part sounds:** its motor is running and it has work (`running()` and `want()` this second), so a part waiting for feed is
quiet even though it is switched on. The server sends the change to the client only when it changes.

Plant parts use Wildcatter's `Sounding` and `MachineSound`. The moving machine, its gantries and the locomotive are entities, which
`MachineSound` cannot follow, so they have `SoundingEntity` and `client/EntitySound`, the same fade and rules, moving with the entity.

Volume above 1 is range, not loudness: the game hears a sound over 16 m times its volume.

| Sound | Kind | Machine | Volume |
|---|---|---|---|
| `jaw_crusher` | loop | jaw crusher | 1.5 |
| `cone_crusher` | loop | cone crusher | 1.5 |
| `vibrating_screen` | loop | double-deck screen | 1 |
| `ball_mill` | loop | ball mill | 2 |
| `belt_conveyor` | loop | conveyor drive, conveyor gantry | 0.6 |
| `apron_feeder` | loop | apron feeder | 1 |
| `slurry_rush` | loop | cyclone cluster | 0.8 |
| `agitator` | loop | conditioning tank, flotation bank | 0.6, 1.2 |
| `pressure_filter` | loop | pressure filter | 0.8 |
| `ore_sorter` | loop | ore sorter | 0.8 |
| `tbm_cutter` | loop | the machine, while it drives | 2.5 |
| `mine_locomotive` | loop | locomotive; pitch and volume follow its speed | 0.4 to 1 |
| Wildcatter's `fan` | loop | air blower | 0.8 |
| Wildcatter's `motor_hum` | loop | thickener, pump gantry | 0.4, 0.6 |
| `muck_tip` | one-shot | a car tipping at the dump | 2 |
| `muck_load` | one-shot | muck pouring into a car, each 1.5 s | 2 |
| `ring_set` | one-shot | each ring the machine sets | 2 |
| `car_couple` | one-shot | a car or gantry coupling | 1 |
| Wildcatter's `structure_formed` | one-shot | the machine lifting and setting down | 2 |

Silent on purpose: the grizzly, pocket, ramp, bins and pond (no motor), the reel gantry (the reel does not turn yet, see Known gaps).
