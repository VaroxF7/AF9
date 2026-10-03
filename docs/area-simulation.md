# Area Simulation

The **Area Simulation Chamber** (`gtceu:hv_area_simulation_chamber` to `gtceu:luv_area_simulation_chamber`) is the pack's
**renewable ore source from HV on**, long before the Space Elevator (`docs/space-elevator.md`). It grows raw ore on
Anode Rods out of three gases. There are no drones and no data sticks: set the area with a programmed circuit and feed it.

Code: `kubejs/startup_scripts/gtceu/area_simulation.js` (lithium gas, the rod, the Area Data, the recipe type `gtceu:area_simulation`,
the tiered single block) and `kubejs/server_scripts/mods/gtceu/area_simulation.js` (crafting and the 55 simulation recipes).

## 1. How it works

`gtceu:area_simulation` takes **Anode Rods** (`kubejs:anode_rod`, used up), **hydrogen**, **carbon dioxide** and
**lithium gas** in, and puts **raw ore** out. An **Area Data** (`kubejs:area_data_<area>`, kept) picks one of the seven
areas, and a **programmed circuit (1-8)** picks the ore in it:

| Area Data | Area | Voltage | Rods | Hydrogen | Carbon dioxide | Lithium gas |
|---|---|---|---|---|---|---|
| Overworld | Overworld | HV (512 EU/t) | 1 | 1,000 mB | 500 mB | 250 mB |
| Nether | Nether | HV (512 EU/t) | 2 | 2,000 mB | 1,000 mB | 500 mB |
| End | End | EV (2,048 EU/t) | 2 | 3,000 mB | 1,500 mB | 750 mB |
| Moon | Moon | EV (2,048 EU/t) | 3 | 4,000 mB | 2,000 mB | 1,000 mB |
| Mars | Mars | IV (8,192 EU/t) | 4 | 5,000 mB | 2,500 mB | 1,250 mB |
| Venus | Venus | IV (8,192 EU/t) | 5 | 6,000 mB | 3,000 mB | 1,500 mB |
| Asteroids | Asteroids | LuV (32,768 EU/t) | 6 | 7,000 mB | 3,500 mB | 1,750 mB |

A run takes **60 to 400 seconds** (`1200 + 400` ticks a stack of ore out): 2 stacks take 100 seconds, 16 stacks 380.
The chamber **perfect-overclocks** and has **no parallel hatch**: one area, one ore, one run at a time.

The areas hold 7 to 8 ores each (55 recipes): the Overworld's iron, copper, tin, coal, redstone, magnetite, hematite
and chalcopyrite; the Nether's copper, gold, tetrahedrite, sulfur, quartz, wulfenite, quartzite and beryllium; the End's
gold, naquadah, magnetite, bauxite, ilmenite, cooperite, platinum and scheelite; the Moon's aluminium, ilmenite,
quartz, certus quartz, bauxite, magnetite, tin and silver; Mars's iron, hematite, goethite, limonite, malachite,
sulfur, pyrite and redstone; Venus's sulfur, pyrite, tetrahedrite, wulfenite, molybdenite, powellite, barite and
quartzite. **Asteroid mode** simulates the Asteroid Field (`docs/asteroid-fission.md`, `docs/oil.md`): brannerite,
magnetite, pentlandite, cooperite, naquadah, platinum and Oil Regolith (for the oil chain).

## 2. The parts

* **Anode Rod** (`kubejs:anode_rod`): assembler, **HV**, 200 ticks: 4 copper ingots and 2 carbon fibre plates make 4.
  Copper-jacketed carbon: the ore condenses on it, and it is used up.
* **Lithium Gas** (`gtceu:lithium_gas`): the third simulation gas, next to hydrogen and carbon dioxide. An
  **Electrolyzer** (EV) vaporises it out of lithium dust, 1,000 mB a dust.
* **Area Data** (`kubejs:area_data_<area>`): one per area, kept, in the recipe's data slot. Assembler, at the area's
  voltage, 400 ticks: a data stick with 4 of the area's plates (iron, black steel, titanium, aluminium, stainless
  steel, tungsten steel, naquadah alloy).
* **The chamber**: assembler-like shaped recipe per tier (HV, EV, IV, LuV): GT's electrolyzer of the tier, 2 pumps,
  2 fluid filters, 2 tier circuits and 2 Anode Rods.
