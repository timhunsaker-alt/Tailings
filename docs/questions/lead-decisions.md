# The lead's decisions while Tim was away (2026-11-09)

Decisions taken outside any one agent's wave, in the order they came up.

### 1. What a ground tip leaves in the truck
- **Chosen:** on the ground a load goes out to the nearest whole block (soil) or quarter of a bank block (rock as muck); the rest
  spills and the truck comes away empty. The pit hopper still keeps part blocks for the next load (Tim's answer, option 1).
- **Why:** the filmed `pit_dispatch` showed a truck that had tipped 30 t of sand drawn full afterwards, because 1.2 t (under a
  block) stayed in its body for ever. Real trucks tip clean; a fraction of a block is lost in the dump anyway.
- **Alternatives:** keep the fraction in the body (the old way, drawn full); or draw the load by how much is in the body.

### 2. Filming the dispatch office with its selector, not a PLC
- **Chosen:** the filmed `pit_dispatch` switches the office HAND (runs) / OFF (parks) by its selector. The PLC's RUN in AUTO is
  proved by the server scenes `pit_dispatch` and `pit_dispatch_fallback`.
- **Why:** the client test's polling can't write RUN every scan, so RUN goes stale and the route parks (correctly).
- **Alternatives:** a real PLC with a ladder program on the control cable in the film (longer to build; worth doing in the
  playthrough).

### 3. Wildcatter 0.6.0 is not released on GitHub
- **Chosen:** Wildcatter master is at 0.6.0 (the ammonia plant) and pushed, but the GitHub release was not created: the permission
  system blocks creating a public release without you. Tailings main stays on Wildcatter 0.5.1 until it is released; Tailings work
  that needs 0.6.0 (P3 blasting) builds against a local copy (`../wildcatter-local-releases`) and waits on its branch.
- **To do when you are back:** `gh release create v0.6.0` (from timhunsaker-alt) with the two jars and the kit, then Tailings can
  move to 0.6.0 and merge P3.

### 4. Raw ore items (from the specs agent, X-1) — left for you
- The plant counts one raw ore item as a whole ore block, so Fortune multiplies ore (about 8.8 iron ingots a block at Fortune III,
  plain copper about 14). The recommendation is to take only ore blocks (Silk Touch) and refuse raw items, but that changes how
  everyone feeds the plant, so it is not built. See `specs-tailings.md`, X-1 and X-3.
