# Sanguinite — the bright-red UHV superconductor

Sanguinite (`gtceu:sanguinite`, bright red `0xFF1A1A`) is AF9's UHV superconductor:
a lossless UV 4A cable (`cableProperties`, no loss) smelted in the **Sanguinite
Hearth Furnace** (`gtceu:sanguinite_hearth_furnace`, a standalone Rotary-Hearth copy running only
`gtceu:sanguinite_hearth`). Neither the EBF nor the Rotary Hearth can smelt sanguinite: the print lives on the
hearth's own type and the material's auto EBF recipe is removed.

The EBF takes a single fluid, so the chain blends first and smelts after:

```text
4 neutronium dust + 10 tritanium dust + 10,000 mB hydrogen + 2,000 mB Ares gas + 4,000 mB LXA-1
  → Large Chemical Reactor (UV, 30 s) → 14 crude sanguinite dust
  → Sanguinite Hearth Furnace, circuit 10 + 50,000 mB supercooled endion (preheated to 13,000 K)
    → 14 hot sanguinite ingots (13,000 K, 4A UV = 2,097,152 EU/t, 60 s)
  → Bulk Blast Chiller (GT's own vacuum-freezer cooling, from the blast property)
    → sanguinite ingots → lossless UV 4A wire and cable (GT's own wiremill/assembler recipes)
```

## The hearth (gameplay)

The structure is the Rotary Hearth's, unchanged (13×17×13: high-temperature smelting casings, coils, muffler,
Naquadah Alloy frames, tungstensteel firebox/pipe, engine intakes, heat vents; hatches maximums-only on any casing,
the AF9 convention, so GT's 360-casing minimum is dropped). The controller is
a refitted RHF controller (assembler, UHV: RHF controller + UHV hull + field generators + pumps under an endion
quench loop). What makes it its own machine is heat (`SanguiniteHearthMachine`):

- **Preheat**: a heat mass 20–13,100 K. Switched on and powered it climbs toward its coils' maximum (coil
  temperature + 100 K per energy hatch tier above MV, the EBF's display maths) in 60 s; without power or while
  switched off it cools over 4 minutes. Prints only start at 13,000 K (`HEARTH_GATE`, plus the recipe's full EU/t),
  and GT retries gated recipes, so a cold hearth starts by itself once hot. Breaking the structure vents it cold.
- **Hold the heat**: an enabled, powered hearth sits at temperature on a 1/8 A trickle (the heaters' draw, like the
  lithography pumps), so back-to-back smelts start at once.
- **Automate it**: keep it on under power with an ME level emitter on the crude stock (or a clock) and feed it
  through the buses; a parallel hatch multiplies the 14-ingot prints, batch mode folds overclocked runs. Resonant
  Endion coils (12,600 K) with UV hatches reach 13,100 K: just past the smelt, nowhere else to go.
- **Jade**: hearth heat bar with its preheat state, then the run-time bar.

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
| Smelt (`af9:sanguinite_hot_ingot`, hearth-only) | 14 crude + circuit 10 + 50,000 mB supercooled endion; 13,000 K — Resonant Endion Coils (12,600 K) at UHV only; 4A UV, 1,200 ticks; preheated hearth required |
| Cooling | GT auto vacuum-freezer recipe (hot → ingot), runs in the Bulk Blast Chiller |
| Cable | UV, 4A, 0 loss (`cableProperties(VA[UV], 4, 0, true)`); wire/foil/plate/rod from flags |

## Files

- Hearth: `af9-core/src/main/java/com/af9/core/machine/SanguiniteHearthMachine.java` (heat, preheat, `HEARTH_GATE`), Jade in `compat/jade/AF9MachineProvider`
- Structure + type: `kubejs/startup_scripts/gtceu/sanguinite_hearth.js` (RHF copy, `sanguinite_hearth` type, 9 tooltips)
- Materials: `af9-core/src/main/java/com/af9/core/registry/AF9Materials.java` (`uhvSuperconductor`)
- Mission: `af9-core/src/main/java/com/af9/core/elevator/PlanetCatalog.java` (planet 9, gas 8)
- Recipes: `kubejs/server_scripts/mods/gtceu/uhv_superconductor.js`
- Deposit: `kubejs/server_scripts/mods/gtceu/vein_oil.js` (`af9:void_ares_gas`)
- Names: `material.gtceu.{sanguinite,crude_sanguinite,ares_gas,lxa_1}` in `kubejs/assets/gtceu/lang/en_us.json`
