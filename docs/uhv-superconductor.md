# Sanguinite — the bright-red UHV superconductor

Sanguinite (`gtceu:sanguinite`, bright red `0xFF1A1A`) is AF9's UHV superconductor:
a lossless UV 4A cable (`cableProperties`, no loss) smelted in the **Rotary Hearth
Furnace** (`gtceu:mega_blast_furnace`, which runs `electric_blast_furnace` recipes).

The EBF takes a single fluid, so the chain blends first and smelts after:

```text
4 neutronium dust + 10 tritanium dust + 10,000 mB hydrogen + 2,000 mB Ares gas + 4,000 mB LXA-1
  → Large Chemical Reactor (UV, 30 s) → 14 crude sanguinite dust
  → Rotary Hearth Furnace, circuit 10 + 50,000 mB supercooled endion
    → 14 hot sanguinite ingots (13,000 K, 4A UV = 2,097,152 EU/t, 60 s)
  → Bulk Blast Chiller (GT's own vacuum-freezer cooling, from the blast property)
    → sanguinite ingots → lossless UV 4A wire and cable (GT's own wiremill/assembler recipes)
```

## The gases

- **Ares gas** (`gtceu:ares_gas`, `Mrs`): the rust-red noble-gas wisp of the
  Martian asteroid field. Bedrock fluid vein `af9:void_ares_gas` in
  `af9:asteroid_field` (weight 8, yield 100–250), drilled with the Fluid Drilling
  Rig like the other field deposits (`kubejs/server_scripts/mods/gtceu/vein_oil.js`).
- **LXA-1** (`gtceu:lxa_1`, `Lx`): the far dark's light exotic. Space Elevator
  liquid mission, planet type 9 gas 8 (`af9-core` `PlanetCatalog`), 64 buckets a
  Mk-IV mission — about 16 smelts per flight.

## Numbers (load-bearing)

| | |
|---|---|
| Blend (`af9:crude_sanguinite_mix`, LCR) | 2 item + 3 fluid inputs (LCR takes 3/5); UV 1A, 600 ticks |
| Smelt (`af9:sanguinite_hot_ingot`, EBF/RHF) | 14 crude + circuit 10 + 50,000 mB supercooled endion; 13,000 K — Resonant Endion Coils (12,600 K) at UHV only; 4A UV, 1,200 ticks |
| Cooling | GT auto vacuum-freezer recipe (hot → ingot), runs in the Bulk Blast Chiller |
| Cable | UV, 4A, 0 loss (`cableProperties(VA[UV], 4, 0, true)`); wire/foil/plate/rod from flags |

## Files

- Materials: `af9-core/src/main/java/com/af9/core/registry/AF9Materials.java` (`uhvSuperconductor`)
- Mission: `af9-core/src/main/java/com/af9/core/elevator/PlanetCatalog.java` (planet 9, gas 8)
- Recipes: `kubejs/server_scripts/mods/gtceu/uhv_superconductor.js`
- Deposit: `kubejs/server_scripts/mods/gtceu/vein_oil.js` (`af9:void_ares_gas`)
- Names: `material.gtceu.{sanguinite,crude_sanguinite,ares_gas,lxa_1}` in `kubejs/assets/gtceu/lang/en_us.json`
