# The DTPF's modules

Status: **built** (af9-core 0.1.159, untested in the game: CI compiles it). The hall itself is `docs/dtpf.md`.

The hall has **six module slots**, taken from Janne's own `dtpf.nbt`: at the top and the bottom of each of the three big rings
a floating plate of **12 Inconel 738 Module Casings** hangs behind the middle injection casing, with the module's controller
in the plate's middle row. The slots are *optional*: a slot without its module is simply not part of the structure (the
pattern accepts air in all 13 cells), so the hall forms with no modules at all.

```
  y+2      M M     M M          (M = Inconel 738 Module Casing, P = the module controller, W = the injection casing)
  y+1    M M M P M M M
  y        . . M W M . .        the controller P faces the field (down at the top, up at the bottom)
```

## 1. The slots

* `PlasmaForgeMachine.SLOT_BACK` / `SLOT_UP` / `SLOT_CASINGS` hold the geometry: ring 11, 21 and 31 blocks behind the
  controller (plates 10, 20, 30 of the nbt), 5 blocks up or down, 12 casings round the controller.
* The hall's pattern (`dtpf.js`) has `M` (module casing or air) and `P` (any module controller or air,
  `PlasmaForgeMachine.modules()`, which also enforces the maximum of each kind with an error message).
* **The hall owns the check** (no sub-pattern in the modules): every second `scanModules()` looks at the six slots. A
  controller found in one is **turned to face the field** (`setFrontFacing`: down at the top, up at the bottom, whatever the
  hall's rotation), linked, and counted when all 12 casings stand and the hall has room for its kind. A module set up or broken
  later is noticed within a second. Without its casings a module shows "not all in place" and gives nothing.
* The controllers are single-block multiblocks (`gtceu:dtpf_module_<kind>`, `DtpfModuleMachine`, rotation state ALL) with an
  animated face (`tools/textures/dtpf_modules.py`), lit while they count. The casing (`af9:dtpf_module_casing`) is the
  Dimensionally Transcendent Casing's grid in Inconel 738 slate, seams lit blue, rust rivets, connected texture. Its recipe is in
  `server_scripts/mods/gtceu/dtpf.js` (assembler, UV); **the controllers' recipes are the pack owner's**.

## 2. The four modules

| Module | Max | Effect |
|---|---|---|
| **Parallel Processing Assembly** | 6 | +10 parallels each (as many as the energy allows; six give 61, under the pack's 64). The hall has no parallel hatch and no batch mode. |
| **Superheating Element** | 1 | Heats the field tenfold (**1,000 K to 10,000 K**) at **2x the EU/t** of every recipe. Recipes need a field heat (below). |
| **Osmic Retention Stabilisation Sequencer** | 1 | Holds the field at **30 %** of its strength at least; **no run is lost to a weak field** (the 25 % rule is off); halves the osmium plasma the field burns. |
| **Manipulated Celestial Forge** | 2 | Runs **1.5 times as fast** at the same voltage, **no extra energy**; two make 2.25x (each multiplies again). |

* Modules multiply with the running-time ramp and the overclocks (`MODULES` is in the recipe modifier chain between `RAMP` and
  the perfect overclock). Parallels multiply the EU/t like any parallel recipe; the Superheater's 2x multiplies on top.
* **Field heat.** The field is `BASE_HEAT` = 1,000 K, 10,000 K with the Superheating Element. Every `plasma_forge` recipe has
  `forge_heat` data (Kelvin; `.addData('forge_heat', n)`) and starts only when the field is at least that hot (`FORGE_GATE`,
  message "The field is too cold"). The JEI page shows "Field heat: n K". Heats in use (starting values, easy to change):

  | Recipe | K | | Recipe | K |
  |---|---|---|---|---|
  | carbon plasma, nano particles | 1,000 | | chromodynium plasma | 4,000 |
  | tritanium plates, chromodynium plates | 2,000 | | stellarium (alloys) / pulsium | 2,000 / 5,000 |
  | strange matter plasma | 3,000 | | neutronium plates | 6,000 |
  | star matter plasma steel plasma | 8,000 | | idontknowium plasma | 10,000 |

* **The Sequencer's numbers.** The field's burn is **576 mB of osmium plasma a second** (288 per 10 ticks), 288 mB/s with the
  Sequencer (the Mk2 halves both: 288 and 144). The floor applies while the controller is switched on and the field exists
  (a hall that has never been fed has no field to hold); below the 50 % start line the run still costs up to 1.93x time and EU/t
  (`getFieldPenalty`).

## 3. The screen

The console has one more line, **MODULES** (`P`arallel, `H`eater, `S`equencer, `F`orge, how many count) and the **FIELD HEAT**
in Kelvin, with a hover tooltip; the controller's own screen shows its kind, whether it is in a hall and whether its casings
are complete.

## 4. Open questions

1. Whether the recipes' heats are right, and whether the Superheater should also be needed for the first recipes.
2. The Sequencer's wording was "10x osmium per tick ... base 288 mB per 10 ticks ... half that with the module": built as the
   base burn 576 mB/s, halved by the module, drawn once a second. Say if it should be something else.
