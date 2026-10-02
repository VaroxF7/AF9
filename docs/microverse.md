# Microverse Projector

Every ore GregTech has, farmable. After Nomifactory's Microverse Projector: a multiblock projects a small world, a drone
works it, raw ore comes out.

Code: `af9-core` `com.af9.core.microverse.OreCatalog` (which ores there are and their tier). KubeJS:
`kubejs/startup_scripts/gtceu/microverse.js` (items, recipe type, structure), `kubejs/server_scripts/mods/gtceu/microverse.js`
(crafting and the ore recipes).

## 1. The recipe

Recipe type `gtceu:microverse_mining`, run by `gtceu:microverse_projector`. One recipe for **every ore material GT has**
(a material with GT's ore property), `af9:microverse/mk<tier>/<material>`:

| Input | Used up? |
|---|---|
| 1 Miner Drone of the ore's tier | yes |
| 1 Microverse Core of the tier | no |
| 1 dust of the ore (the *seed*: `gtceu:<material>_dust`) | no |

Output: 16 raw ores (`raw_` item; the crushed ore for a material without one). The seed is what says *which* ore: recipes of a
tier share drone and core, so the dust tells them apart (a programmed circuit has only 32 values, there are more ores). It also
means you need one dust of an ore before you can farm it. Time 30 / 40 / 50 / 60 s, voltage EV / IV / LuV / ZPM for Mk1 to Mk4;
the Parallel Hatch and overclocking work.

## 2. Tiers

An ore's tier is the lowest tier of the world-gen layers of the veins that hold it (`OreCatalog`):

| Mk | Microverse | Layers |
|---|---|---|
| 1 | Overworld | stone, deepslate |
| 2 | Nether | netherrack |
| 3 | End | end stone (and any layer unknown to the catalog) |
| 4 | Asteroid | `af9_asteroid`, and every ore that no vein holds |

Veins of weight 0 count as no vein: pitchblende and uraninite (the Asteroid Field replaced their veins) are Mk4, like the
brannerite. An ore a modpack adds to GT is in the catalog without anybody writing it down. The log says how many ores each tier
holds (`Microverse: N ores in tier T`).

## 3. Items

| Item | Made from |
|---|---|
| Miner Drone Mk1-4 (two per recipe) | robot arm, sensor, motor, 2 circuits, 4 plates of the tier (titanium, tungsten steel, rhodium-plated palladium, naquadah alloy), solder, circuit 11-14 |
| Microverse Core Mk1-4 | 2 field generators, an emitter, 4 circuits, 8 plates of the tier; Mk2 also an HBM Memory Stick, Mk3 a CPU Cluster, Mk4 a CPU Superpositioned Cluster and an HBM Memory Stack (docs/crafting-cpu.md) |

## 4. Structure

`gtceu:microverse_projector` (EV assembler recipe): 5 x 5 x 5 of fusion casings with a 3 x 3 x 3 cube of fusion glass inside and
a superconducting coil in its centre. Aisles front (controller) to back. Hatches (maxima only) on any casing: 2 item inputs,
2 item outputs, 2 energy, a Parallel Hatch, maintenance.
