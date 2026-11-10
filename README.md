# Tailings

Tailings is a Minecraft **26.3** add-on (Java **25**) for [Wildcatter](https://github.com/timhunsaker-alt/Wildcatter). It is **its own jar** on **Fabric and NeoForge**, and it **requires Wildcatter 0.4.1 or newer** on the same loader. It will not load without it. Tailings has no grid, ores, or pipes of its own. Machines draw power from Wildcatter, and Wildcatter's ore blocks are the feed.

The mod is about getting rock out of the ground and the mineral out of the rock. A plain mine-and-smelt is **1x (25%)** of the mineral a block holds. Gravity or magnetic concentration is **2x (50%)**. Flotation is **3x (75%)**. The full line, not built yet, is **4x**, which pays **95%** of what the block holds (a written exception: 95/25 is 3.8). Nothing is created that was not already in the rock. Why that is allowed is in [docs/VISION.md](docs/VISION.md). How the current systems work is in [docs/DESIGN.md](docs/DESIGN.md).

Version **0.1.0**. License [CC0](LICENSE).

## Install

You need Minecraft 26.3, Java 25, and Wildcatter 0.4.1 or newer built for the same loader.

**Fabric:** Fabric Loader 0.20.1 or newer, Fabric API `0.165.0+26.3`, the Wildcatter Fabric jar, and `tailings-fabric-0.1.0.jar`.

**NeoForge:** NeoForge 26.3.0.101 or newer, the Wildcatter NeoForge jar, and `tailings-neoforge-0.1.0.jar`.

Put the Tailings jar and the Wildcatter jar in the instance `mods` folder. `fabric.mod.json` and `neoforge.mods.toml` both require `wildcatter` at `[0.3.0,)`.

Wildcatter 0.4.1 is not on Maven. The release jars are:

- https://github.com/timhunsaker-alt/Wildcatter/releases/download/v0.4.1/wildcatter-fabric-0.4.1.jar
- https://github.com/timhunsaker-alt/Wildcatter/releases/download/v0.4.1/wildcatter-neoforge-0.4.1.jar

## What you can do now

Phases 1 through 3 are in the game. The current phase is a realignment with Wildcatter's art and multiblock patterns (R1, safety fixes, is done); Phase 4 (full 95% recovery, by-products, the lined tailings cell) comes after it. Status and the known gaps are in [docs/ROADMAP.md](docs/ROADMAP.md). History is in [CHANGELOG.md](CHANGELOG.md).

- **Hand mill (2x).** A stamp mill and a hand screen break rock down the size ladder (crushed, gravel, sand). A shaking table and a magnetic separator pay the 50% route. Nine nuggets craft into one mineral item. Iron and gold use the vanilla nuggets.
- **Concentrator (3x).** A true-size crushing circuit (grizzly, jaw, belt, screen, cone, ball mill, cyclones) grinds sand to fines. A flotation bank (conditioner, four roughers, two scavengers, two cleaners, blower, thickener, filter) pays 75% for the sulfides and for vanilla copper and gold. The motors are Wildcatter's, at 480 V or 4.16 kV.
- **The mine.** A moving tunnel drill cuts an 8×8 face and leaves a 6×6 lined drive, with smooth 1-in-2 ramps, a stair walkway, and twin mine rail. It pauses when the muck buffer is full. A traction rectifier feeds the rail. A locomotive hauls up to ten cars (nine stacks each) to a dump that sorts the load. Fed rails power lights and signals. Trains hurt players and mobs unless `config/tailings-train.properties` says otherwise. A running drill or train keeps its own chunks loaded with a ticket that lets go about 10 s after it stops.
- **Ore blocks only.** The plant takes ore blocks (Silk Touch, the drill, a blast), not raw ore items or an ore's mined drop.
- **World rules.** What would change an existing world (the drill's NO VENTILATION interlock, stale air, the paste rules, the all-HMC dry mill) is a rule saved with the world: on in a new world (all but the paste wall breach, which is off until set), off in one saved before them. An operator lists and sets them with `/tailings rule`.

## Docs

| Doc | What it is |
|---|---|
| [docs/VISION.md](docs/VISION.md) | Why the recovery ladder is not ore multiplication |
| [docs/DESIGN.md](docs/DESIGN.md) | How the systems, blocks, items, entities, and recipes actually work |
| [docs/ROADMAP.md](docs/ROADMAP.md) | Phase status, what each merged PR delivered, and what is still a stand-in |
| [docs/ART.md](docs/ART.md) | Where the textures and entity models live, and how they were made |
| [docs/multiblocks/](docs/multiblocks/) | Machine specs: crushing circuit, flotation bank, tunnel drill and haulage |
| [CHANGELOG.md](CHANGELOG.md) | Merged pull requests |
| [PROGRESS.md](PROGRESS.md) | Short status notes |

## Building

The Gradle JVM must be Java 25. Fabric Loom 1.19 refuses a Java 21 Gradle JVM. NeoForge's build tools also want a Java 21 installation on the machine (CI installs both).

```bash
./gradlew build
./gradlew :common:test
./gradlew :neoforge:runGameTestServer
```

`build` compiles both loader jars and runs the plain-Java tests. The game-test server is a separate task. Jars land in `fabric/build/libs/` and `neoforge/build/libs/`.

Wildcatter is `compileOnly` (and NeoForge `runtimeOnly` for the game tests). It is downloaded from the GitHub release above and is not bundled into the Tailings jar.

Layout, same shape as Wildcatter: `common/` (the mod), `fabric/` and `neoforge/` (entry points). Recovery, rock lists, milling math, the plant, and haulage rules are plain Java with no Minecraft imports. `verifyPlainJava` enforces that for `recovery`, `rock`, `mine`, and `haul`.
