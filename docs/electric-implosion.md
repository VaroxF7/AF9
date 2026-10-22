# Electric Implosion Compressor and the tier 2 rocket's hull

Janne's spec of 2026-10-18. Machines: `kubejs/startup_scripts/gtceu/electric_implosion.js`. Recipes:
`kubejs/server_scripts/mods/gtceu/electric_implosion.js` (all implosion recipes), `rocket_hull.js` (alloy, cone, fins), `rockets.js` (the tier 2 row), `fab_chemistry.js`
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

## Implosion rules (one table)

Every implosion recipe is generated from the `IMPLOSIONS` table in `server_scripts/mods/gtceu/electric_implosion.js`; nothing
else defines one. Each product gets the same three recipes, ids `af9:implosion/<product>/<route>`:

* `dynamite`: GT Implosion Compressor, LV, 20 ticks, 4 dust + 2 dynamite
* `industrial_tnt`: same machine, 4 dust + 1 industrial TNT
* `electric`: Electric Implosion Compressor, 4 dust + 500 mB Lunar Acid Vapor, tier and time per row (dense plate: HV, 200 ticks)

### Charged route (capacitor bank)

The Large Electric Implosion Compressor (Java: `LargeElectricImplosionMachine`) has an internal buffer of 8,000,000 EU. Its
energy hatches fill it at their input rate whenever the structure is formed. A *charged* recipe takes no explosive and no
vapor; it fires `burst_eu` (200,000 EU, per parallel) from the buffer when it starts, and is not found while the bank holds
less. The charged recipes are generated for **every** `gtceu:implosion_compressor` recipe (GT's gems and our own table
rows): same inputs without the dynamite / industrial TNT / TNT / powder barrel, same outputs, ids
`af9:implosion/<product>/charged` and `af9:implosion_charged/<original id>`. They are their own recipe type, `charged_implosion` (the Large machine's second mode, screwdriver), so the single blocks have
no bank and only run the vapor recipes. Vapor recipes do not touch the bank. The console shows the bank's charge.

Currently one product (the dense plate). GT's own gem implosions are untouched. Anything that needs an implosion product takes
the item itself (`rockets.js` takes the dense plate), so it never cares about the route.

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

## Naquadah from U-235

The capacitor bank's discharge is also the transmutation: the shock drives the fissile nuclei together until they fuse
into naquadah. The reaction runs in the same recipe type, so the Large Electric Implosion Compressor and the single
blocks HV-LuV all take it.

| takes | gives | tier / time |
|---|---|---|
| 4 `gtceu:uranium_235_dust` + 2,000 mB Lunar Acid Vapor | 1 `gtceu:naquadah_dust` | EV, 1,920 EU/t, 600 ticks |

The U-235 is the enriched product of the asteroid fission chain (`docs/asteroid-fission.md` §4). Four full dusts per
naquadah dust make the implosion a deliberate sink for enrichment rather than a cheap route. The field naquadah vein,
the void miner's naquadah circuits and the Mk-IV space elevator keep their naquadah for now; the implosion is a third
source, not a replacement.

## Twi-re-sten Insulation

Twinite-redstone-tungsten alloy (`gtceu:twiresten_insulation`): dust, ingot (EBF, 4,200 K, IV), plate, dense plate, foil, fine wire.
Mixer, EV: twinite dust (`thermal_extra:twinite_dust`) + redstone dust + tungsten dust -> 3 dust.

## Nichrome coil (EV)

GT's HV assembler recipe is replaced: **EV**, 4 nichrome quadruple wires, 8 stainless steel foil, 8 Twi-re-sten foil and 500 mB Lunar
Acid Vapor (the assembler has one fluid slot, so GT's aluminium is gone).

## Open

* "A new liquid for smth else": not added, waiting for what it is for.
* Annealed Bronze and Lapis Butene-II-Sulfonate: another thread.
