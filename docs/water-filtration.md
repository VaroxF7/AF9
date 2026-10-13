# Water Filtration Plant: the basin that skims

A 5x3x5 open stainless basin behind glass (controller front-bottom-centre) that filters water
and aqueous waste. Inside: animated water in the biome's own colour, 5–15 seeded debris chunks
floating on top, and a 3D skimmer arm sweeping front to back at a steady speed while it runs.

Code: `kubejs/startup_scripts/gtceu/water_filtration.js` (structure, `water_filtration` type),
`kubejs/server_scripts/mods/gtceu/water_filtration.js` (recipes),
AF9 Core `FiltrationPlantMachine` + `FiltrationBasinRender`.

## Modes (the circuit picks the run, the filter is never used up)

| circuit | mode | in | out |
|---|---|---|---|
| 1–4 | river / murky / pond / brackish water (LV–EV) | water | distilled water + sand, gravel, clay, dirt |
| 5 | lithium brine pumping (MV) | salt water | lithium brine + halite + salt (feeds `docs/lithium-brine.md`) |
| 6 | desalination (MV) | salt water | distilled water + salt |
| 7 | spent piranha polishing (MV) | spent piranha | wash water + 200 reclaimed piranha + filter cake |
| 8 | resist carbon bed (HV) | spent resist solvent | 300 PGMEA + carbon + filter cake |

## Wastewater cleaning

Mechanical separation only: unreacted pre-liquid passes the bed and comes back (200 piranha
per strip, 300 PGMEA per spin-off), suspended solids cake out as spent filter cake, dissolved
values stay in the wash water, which loops back to washing, never to chemistry. The full
treatments stay the better-paying paths: lime neutralisation for spent piranha
(`litho_process.js`, gypsum + cake + water) and cleanroom fractionation for spent resist
solvent (600 PGMEA + cake). The carbon bed here takes half the PGMEA with no cleanroom —
the quick-and-dirty alternative.

## Spent filter cake

The universal waste dust of every cleaning run (`gtceu:filter_cake_dust`, rough brown cake).
It is never voided by a recipe: every cleaning run pays its cake, and the cake pays back as
furnace fuel — KubeJS gives it coal's burn time (1600 ticks), so it burns in any furnace
or trash-burns like coal. Lime cakes and distillation chars join the same dust.
