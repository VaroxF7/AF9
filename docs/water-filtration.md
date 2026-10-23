# Water Filtration Plant: the basin that skims

A 10x7x3 open watertight basin (controller in the middle of the 7-wide front wall, middle layer) with two
1x6 strips of **Water Filter Casing** (`af9:water_filter_casing`, watertight plating with a wire-mesh screen, CTM-joined,
`tools/textures/water_filter_casing.py`) in its floor, that filters water and aqueous waste. The 8x5 above the floor,
two layers high, is the open water; walls, floor and rim are GCYM watertight casing, which also takes the hatches. Inside: animated water in the biome's own colour, 5–15 seeded debris chunks
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
| 9 | LuV biosorption (needs breeder) | salt water + LuV culture | lithium brine + salt |
| 10 | ZPM biosorption (needs breeder) | salt water + ZPM culture | concentrated brine + salt |

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

## Bio Vat upgrade (LuV/ZPM modes)

Circuits 9 and 10 need the **Bio Vat** (`gtceu:bio_vat`, 5x4x5, MV bones) built and piped to the
plant — the modes only run on what it grows. The Bio Vat is GTNH's Bacterial Vat (BartWorks
`MTEBioVat`, MIT, GT5-Unofficial), see `docs/bio-vat.md`:

- biochaff + water (MV, circuit 1, laminated glass) → **bacteria culture** (LuV strain);
- that culture + water (HV, circuit 2, fusion glass) → **enriched culture** (ZPM strain, subcultured).

LuV biosorption turns 2,000 mB salt water + 500 mB culture straight into 1,000 mB lithium
brine (better than the MV brine pumping it replaces); ZPM biosorption turns the same feed
with enriched culture straight into 1,000 mB **concentrated** brine, skipping the ponds.
Biosorption, after the labs that pull lithium with bacteria instead of lime.
