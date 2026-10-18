# Electric Implosion Compressor and the tier 2 rocket's hull

Janne's spec of 2026-10-18. Machines: `kubejs/startup_scripts/gtceu/electric_implosion.js`. Recipes:
`kubejs/server_scripts/mods/gtceu/electric_implosion.js`, `rocket_hull.js`, `rockets.js` (the tier 2 row), `fab_chemistry.js`
(the vapor). Materials and items: `af9-core` (`AF9Materials.rocketInsulation()`, `AF9Items`, `AF9Blocks`). Art:
`tools/textures/electric_implosion.py` (`--preview <dir>` writes contact sheets and an animated front view).

## The tier 2 rocket

Assembler, HV: Rocket Cone Tier 2, 4x Rocket Fin Tier 2, **2x** Desh Engine, **2x** Desh Tank, 4x dense plate of Desh Titanium
Rocket Alloy, 4x Alumina Silicate Rocket Insulation (the count is a pick, Janne gave none), 2x HV robot arm, solder. The six
desh blocks of the old recipe are gone.

| step | machine | recipe |
|---|---|---|
| alloy dust | mixer, HV | 4 titanium + 2 desh + 16 chromium dust -> 4 Desh Titanium Rocket Alloy dust |
| dense plate | Implosion Compressor, LV | 4 dust + 2 dynamite (or 1 industrial TNT) -> 1 dense plate |
| dense plate | Electric Implosion Compressor, HV | 4 dust + 500 mB Lunar Acid Vapor -> 1 dense plate |
| cone | assembler, HV | rocket nose cone, 2 desh plate, HV sensor, 2 ASIC chips, solder |
| fins | assembler, HV | 4 rocket fins, 4 desh plate, 4 desh rod -> 4 fins tier 2 |

GT's bender recipes for the alloy's dense plate (9 plates, 9 ingots) are removed: the implosion is the only way. The cone and fin
recipes are proposals (Janne gave only what the rocket takes); change them in `rocket_hull.js`.

## The insulation (items only, recipes to come)

Alumina Silicate Insulation (dust), Liquid Alumina Silicate Insulation Mixture (fluid), Alumina Silicate Insulation Foil, Insulation
Wool (fine wire) and Alumina Silicate Rocket Insulation exist; no recipe makes or takes them yet except the rocket taking the
insulation. `tools/lint/data/sources.txt` lists the insulation as a source until Janne's recipes land.

## Electric Implosion Compressor

* Recipe type `electric_implosion_compressor` (2 items in, 2 out, 1 fluid in, 1 out). GT's own Implosion Compressor keeps the
  explosive recipes; the electric one only runs the vapor recipes, so every gem the explosive one makes stays explosive-only.
* Single blocks HV-LuV (`gtceu:hv_electric_implosion_compressor` ...), crafted from the tier's hull, piston, emitter, circuits and cable.
* Multiblock `gtceu:large_electric_implosion_compressor`, 3 x 3 x 3 like GT's Large Chemical Reactor: 20 Shock Capacitor Casings (hatches
  go in them), 5 cupronickel coil blocks in the cross, a titanium frame in the middle, the controller in the middle of the front.
  Parallel hatch, perfect overclocks. Crafted from GT's Implosion Compressor, a piston, circuits, the new casing and gold cable.
* Shock Capacitor Casing (`af9:shock_capacitor_casing`): GT's Stable Titanium Machine Casing with its connected sheet (copied to
  `tools/textures/gt_src/`, LGPL-3.0), recoloured teal by `tools/textures/electric_implosion.py` (an earlier hand-drawn capacitor
  casing was dropped). The face is shared by the single blocks and the controller: a viewport on two electrodes, an arc that
  crackles while it works (emissive), a red / amber / green lamp.

## Lunar Acid Vapor

`gtceu:lunar_acid_vapor` (HF + SO2 formula, acid attribute: needs acid-proof pipes and tanks) is a new cut of the Lunar Air
distillation (`fab_chemistry.js`): 4,000 mB Lunar Air -> 2,600 L-01, 700 argon (was 1,000), 160 radon, **340 vapor**.

## Twi-re-sten Insulation

Tungsten-rhenium carbide cermet (`gtceu:twiresten_insulation`): dust, ingot (EBF, 4,200 K, IV), plate, dense plate, foil, fine wire.
Mixer, EV: 2 tungsten + rhenium + tungsten carbide dust -> 4 dust (the rhenium comes from `alloys_af9.js`).

## Nichrome coil (EV)

GT's HV assembler recipe is replaced: **EV**, 4 nichrome quadruple wires, 8 stainless steel foil, 8 Twi-re-sten foil and 500 mB Lunar
Acid Vapor (the assembler has one fluid slot, so GT's aluminium is gone).

## Open

* "A new liquid for smth else": not added, waiting for what it is for.
* Annealed Bronze and Lapis Butene-II-Sulfonate: another thread.
