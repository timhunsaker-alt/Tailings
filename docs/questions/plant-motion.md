# The crushers and apron feeders move (2026-11-19)

**Accepted by Tim 2026-11-19** (as built; answers-2026-11-19, C24: the real stroke kept).

Decided while Tim is away (lead's brief: "the jaw crusher and the apron feeder move while they run", for the PLC belts short).
Each is open for Tim's review.

### 1. Which crusher
- **Chosen:** both. The brief names the jaw crusher, but the crusher in the belts short (`PlcBelts`) is the pit line's 42-65
  gyratory with its primary apron feeder. The jaw, the gyratory and all three apron feeders (the primary, the reclaim feeder and the
  dump station's) now draw their moving parts.
- **Why:** the short needed the gyratory; the brief asked for the jaw. One renderer serves all five.
- **Alternatives:** the jaw only (nothing would change in the short); film a jaw instead of the gyratory (the short is the pit line).

### 2. How they move
- **Chosen:** the moving parts are left out of the still block models (tools/models crushing.py, pitline.py, dump.py) and drawn by
  `client.PlantRenderer`, a block entity renderer, from the same boxes, turned with the part. They move while `running()` (the motor
  turns: powered and started, or a test's forced run), work or none, and stop where they stand. They turn by game time, not the wall clock as Wildcatter's `Spin` does, so a film saved a
  frame a tick shows them at their real speed. The server
  sends `moving` with the sound flag.
- **Why:** the belts' renderer is the repo's way; a contraption (the drill's way) is for machines that travel.
- **Alternatives:** Wildcatter's `Kinetic` rotors (block models per moving part, more models for the same boxes).

### 3. Speeds and strokes
- **Chosen:** the C100-class jaw at 260 rpm, its swing jaw's top on a 16 mm eccentric circle and a 32 mm stroke at the discharge
  (both under a pixel: what reads is the flywheels, each with a counterweight so it reads as turning). The 42-65 gyratory's
  eccentric at 150 rpm; its mantle's head gyrates by the little it does near the spider (about 1.5 mm); its drive coupling turns with
  the eccentric, a quarter of its real 600 rpm, because 10 turns a second strobes on film. The apron feeders' pans at 0.1 m/s,
  a constant design speed (a 6-tooth head sprocket), not tied to the feed rate.
- **Why:** real speeds, slowed only where it strobes. The brief asked for a jaw's real stroke; a real stroke does not show at
  Minecraft's scale, so no stroke is enlarged.
- **Alternatives:** an enlarged stroke so the jaw visibly bites (a display choice like Wildcatter's rpm / 20); pans at the speed the
  feed rate needs (at the plant's 50 t/h, a 2.4 m apron barely moves: about 3 mm/s).
- **Tim (2026-11-19):** keep the real stroke; no stroke enlarged for display (answers-2026-11-19, C24).
