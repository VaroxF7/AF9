# HPCA (expandable)

GT's High Performance Computation Array, changed by the fork (`gtceu-fork/patches/0012`). The components are the same
(Computation, Advanced Computation, Heat Sink, Active Cooler, Bridge, Empty); what changed is the size and the coolant.

## Size

The component section is one slice (3 slots, one per row) repeated **3 to 12 times** behind the controller: **9 to 36
component slots**. The smallest build is GT's own. Every slice is the same: casing and vents on the sides, three component
slots in the middle. Energy, coolant (fluid input), transmission and maintenance hatches go in the computer casings as before.
The controller screen draws every slot (the grid shrinks to fit past 3 slices).

## Active Coolers and the coolant

An Active Cooler takes up to 8 mB/t and cools by the coolant it gets, at that full rate:

| Coolant | Cooling per Active Cooler |
|---|---|
| PCB Coolant (and any other fluid of `gtceu:hpca_coolants` not listed) | 2 (as in GT) |
| Supercooled hydrogen | 8 |
| Supercooled argon | 12 |
| Supercooled xenon | 16 |
| Supercooled endion | 20 |

(Same grades as the lithography station's coolants, `litho/Coolant`.) The array draws the strongest coolant present first;
a stronger coolant also needs less fluid to hold the temperature (the coolers only draw what the heat needs unless the array
runs hot). The table is `HPCAMachine.registerCoolant`; the supercooled fluids are in the tag `kubejs/data/gtceu/tags/fluids/hpca_coolants.json`.

Computation per slot is unchanged: Computation Component 4 CWU/t (cooling demand 2), Advanced 16 CWU/t (demand 4); so a
full 36-slot array of Advanced Computation Components would need 144 cooling.

The hatches of the array feed computation to everything that takes it (Research Station, Orbital Lithography Station, the
"CWU: N" recipe line, wireless transmission hatches); nothing there changed.
