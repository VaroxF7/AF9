# Agriculture Simulation Bed

`gtceu:agri_sim`: simulates the pack's seeds, above all Mystical Agriculture's essence seeds. KubeJS multiblock:
`kubejs/startup_scripts/gtceu/agri_sim.js`, recipes `kubejs/server_scripts/mods/gtceu/agri_sim.js`, logic
`AgriSimMachine`, render `AgriSimRender` (af9-core), face texture `tools/textures/agri_sim.py`.

## Structure (11 x 3 x 11, Janne's agrisim.nbt)

Corrosion-proof casing floor and walls, a garden of soil in the middle layer (two H-shaped beds either side of a casing
cross, the Industrial Foregoing water condensator in the centre), open air above the soil. The controller replaces the
nbt's orange wool block: bottom row, one block behind the front wall, which has a one-wide, two-high notch in front of it.
Hatches go in any casing (maximums only): 2 energy, 2 item buses in, 2 out, 2 fluid hatches in, 1 parallel hatch,
1 maintenance. Soil blocks: see below. The air above the soil must stay free (the crops are drawn there).

## Modes (the mode tab, one recipe type each)

| Mode | Seeds |
|---|---|
| `agri_sim_elemental` | Mystical Agriculture elemental (air, earth, water, fire) and tier 1 |
| `agri_sim_tier2` / `tier3` / `tier4` | tier 2 / 3 / 4 essence seeds |
| `agri_sim_tier5` | tier 5 and Mystical Agradditions' tier 6 |
| `agri_sim_crops` | every other seed: vanilla, Farmer's Delight, Croptopia, Thermal, and mod seeds whose produce is unknown (those multiply the seed) |

The seed is never consumed: it goes into the controller's seed slot (screen, bottom right; the recipe names it as a not consumed input and runs only with it there). Every recipe is water only, HV (512 EU/t), 60 s. Essence per run by tier 0-6: 16, 16, 12, 10, 8, 6, 4; water
250 mB x (tier + 1); Crop mode: 4-24 produce per run, .
A seed whose mod or crop is not in the pack has no recipe (`Item.exists`).

## Soil (average over the garden)

| Soil | Output | Time |
|---|---|---|
| Mystical inferium / prudentium farmland | x1 | x1 |
| Farmer's Delight rich soil farmland, Mystical tertium / imperium / supremium farmland | x1.25 | x0.9 |
| Thermal Phytosoil (tilled) | x1.5 | x0.75 |

## Parallel hatch or batch mode

Never both. With a parallel hatch the batch toggle reads off and cannot be switched on. Batch mode (own modifier: GT's
only batches recipes under its duration limit) runs 8 recipes in one cycle: same speed, fewer recipe checks. Parallel hatch
at most 64 (pack rule). Overclocks are normal.

## Render

While a recipe runs, the real crop block of the seed (the seed item's block, with Mystical's tinted flowers) stands on every
soil block at its growth stage (progress, per run when batching). Idle: bare. Skipped in a shader pack's shadow pass and
beyond 64 blocks; the frustum culls the garden. Controller face: idle, working and paused animations.

## Crafting

EV: titanium plates, EV circuits, tempered glass, EV hull, EV pumps, rich soil.
