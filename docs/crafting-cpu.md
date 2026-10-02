# Crafting CPU Array

AE2's autocrafting CPU as a GregTech multiblock. The CPU's storage and its parallel crafts are not built from AE2's
crafting blocks but come from what is in the machine: **HBM Memory** (bytes) and **CPU Clusters** (co-processors).

Code: `af9-core` `com.af9.core.cpu` (`CraftingCpuMachine`, `CpuRackPartMachine`, `CpuPart`) and `com.af9.core.ae2`
(`CpuCoreBlock`, `CpuCoreBlockEntity`, `CpuCoreType`); `mixin/ae2/CraftingCPUClusterAccessor`. KubeJS:
`kubejs/startup_scripts/gtceu/crafting_cpu.js` (items, rack, structure), `kubejs/server_scripts/mods/gtceu/crafting_cpu.js`
(recipes).

## 1. Parts

| Item | Id | Gives | Made from |
|---|---|---|---|
| HBM Memory Stick | `kubejs:hbm_memory_stick` | 4 MiB of crafting storage | 8x what a Silicon RAM card takes (8 plastic boards, 32 RAM chips, 64 fine gold wire, 1152 mB solder, circuit 4), circuit assembler, HV |
| HBM Memory Stack | `kubejs:hbm_memory_stack` | 32 MiB | 8x what a Nano RAM card takes (8 epoxy boards, 32 eDRAM chips, 64 fine platinum wire, 1152 mB solder, circuit 5), cleanroom, IV |
| CPU Cluster | `kubejs:cpu_cluster` | 16 co-processors | four CPUs: an epoxy board, 4 Nano CPU chips, 8 fine platinum wire, cleanroom, IV |
| CPU Superpositioned Cluster | `kubejs:superpositioned_cpu_cluster` | 64 co-processors | eight CPUs: a fibre-reinforced board, 8 Qubit CPU chips, 16 fine osmiridium wire, cleanroom, LuV |

The Silicon and Nano RAM cards of the computer racks got a programmed circuit (3) so that a circuit assembler holding the
HBM inputs cannot run the card's recipe instead (lint R7).

The numbers (bytes, co-processors, EU/t per item) are `CpuPart`'s.

## 2. Structure

`gtceu:crafting_cpu_array`, 3 x 3 x 3 to 3 x 3 x 10, advanced computer casings (`gtceu:advanced_computer_casing`):

* the front slice holds the controller; every slice between the front and the back (1 to 8) holds a **CPU Rack**
  (`gtceu:hv_cpu_rack`) left and right of a heat vent in the middle row;
* energy hatches (up to 2) on any casing;
* **one Crafting CPU Core** (`af9:crafting_cpu_core`) in place of any casing. GT's auto-build places casings only: swap
  one for the core, the one a cable can reach. A structure with no core forms but has no CPU (the screen says so).

A CPU Rack has four slots, each takes up to 8 of one kind of the parts above.

## 3. The CPU

The Crafting CPU Core is an AE2 crafting unit (`AbstractCraftingUnitBlock`, `CraftingBlockEntity`): AE2 forms a
crafting CPU from it, with the grid node, the terminal listing and the job saving AE2's own CPUs have. On its own the
core is a CPU of 1 byte and no co-processors. The array overwrites the bytes and the co-processor count of the core's
cluster every second (through the accessor mixin, `CraftingCPUClusterAccessor`), so it also puts them back after AE2
re-forms the cluster. A core takes a cable on any face.

* **Bytes** = the sum of the memory in the racks. **Co-processors** = the sum of the clusters', at most 1024 (AE2 runs one
  craft per co-processor each tick, on the server thread).
* The array draws `256 EU/t + what its parts draw` (Stick 8, Stack 64, Cluster 64, Superpositioned Cluster 512) every tick
  while switched on. Without energy, switched off or unformed, the CPU has 0 bytes and no co-processors: nothing is
  started on it, a running job waits.
* AE2 is optional: without it the array forms and says so, nothing else happens.

Several cores in one AE2 cluster (placed next to each other, or next to AE2's own crafting blocks) share one CPU; the
last array to set it wins. One core per CPU.

## 4. Not done

* The array has no coolant and no heat, as the computation arrays have.
* Nothing yet reads the core's name for the AE2 terminal: the CPU is listed as AE2 names it.
