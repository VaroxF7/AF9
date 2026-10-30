# Distillation Facility

The IV distillation path. A 10 x 7 x 15 walk-through of **real GT fluid pipes** round three **Distillation Tower Modules**, built 1:1 from
Janne's `v3.nbt` (kubejs/startup_scripts/gtceu/distillation_facility.js, af9-core `com.af9.core.distillation`). It replaces the
Distillation Tower: **the Distillation Tower is gone** (gtceu-fork patch `0027` drops the machine and its crafting; its recipe type stays and is the first mode of every tower; the GCYM Large Distillery is built from an IV Distillery instead).

## The structure

* Controller: where the file's green wool stood, in the arch at the front (controller facing SOUTH, as the file stands in the world; turned for the other
  facings). Machine id `gtceu:distillation_facility`.
* Casings, as in the file: Inert Machine Casing (the arch, the middle block), Stable Machine Casing (the two turbine houses), Stainless Firebox Casing
  (the tower feet), Clean Machine Casing (the towers), Kanthal Coil Blocks (the tower coils), HSLA Steel Frames (3, between the pipes).
* Pipes: Tungsten and Stainless Steel Small Fluid Pipes, 47 in all, each with the file's exact `connections` mask. The pattern accepts a pipe spot only for
  that exact block **and** that exact mask, and the controller sets the masks itself while the facility is unformed, so an auto-built facility forms without
  wrench work. This is the Chemical Plant's pipe tech ([chemical-plant.md](chemical-plant.md): `PipeBlockMixin` keeps the connections, the same data tag),
  in `DistillationFacilityPipes`. The 9 pipes the file has in a frame (the middle column of the long runs) are **not** asked for with a frame (a framed or a plain pipe both form; the frame requirement of 0.1.192-0.1.195 was removed because the structure stopped forming). The JEI preview shows the pipes with their connections (fork patch `0028`, a preview hook).
* **Hatches go only on the Turbine Casing spine**: the 14 Steel Turbine Casings of the file down the middle of the floor. Up to **6 energy hatches**, **1 maintenance
  hatch**, up to 3 fluid input hatches, 2 item input buses and **3 output hatches in all** (fluid hatches and item buses together, Janne: better automation challenges). Recipes with more fluids than that (up to 8, e.g. liquid ender air) need Quadruple / Nonuple Output Hatches, one tank a fluid; the lint counts 9 tanks a hatch for the facility (`MULTI_TANK_OUT`). Parts have maximums only (the pack's rule).
* **3 muffler hatches** where the file has them (the top of each tower).
* **3 rotor holders, EV or better**, where the file has them (behind each tower, facing up: the facility turns them to face up). The rotor turns in the open
  air above the holder, so the 3 x 3 above it must stay free.

## The three towers (sub-multis)

The three spots where the file had Distillation Tower controllers are **Distillation Tower Modules** (`gtceu:distillation_module`): small multiblocks of their own
in the Distillation Tower's shape (a 3 x 3 column: a Kanthal coil at its foot, a hollow shaft of 2 or 3 layers, a muffler hatch on top). The facility turns
a module to face the right way.

* **The recipe is set in the module and runs on the facility.** A module has its own mode tab and its own recipe lock; what it runs on is the facility's: the
  energy hatches (it overclocks to their voltage), the fluid and item hatches for its inputs and outputs, the clean room, the maintenance hatch (problems stop the
  towers until fixed) and its rotor. A module outside a formed facility runs nothing.
* **3 towers, 3 parallels.** Every tower runs one recipe at a time. With nothing locked, a tower takes whatever recipe the facility's inputs match, so three towers fed
  one fluid are three parallels of one recipe. With **[Lock to this recipe]** on the tower's screen (it locks to the recipe the tower runs or ran last) a tower only
  ever runs that recipe, so the three can run three different ones. **[Unlock]** releases it.
* **At most 1 parallel hatch per tower**: in any casing of the tower. It gives that tower its hatch's parallels, **capped at 21 a tower** so the three towers together stay within the pack's 64 parallels
  (63 runs at most). A second hatch in the same tower stops that tower from forming.
* **Perfect overclock, no batch mode.**
* The facility is **its own clean room once it is filtered** (see Filtered Distillation below): the clean-room recipes of the fab modes run without a GT Cleanroom round it.
* The facility itself runs no recipe: it shows whether any tower works (the model is active, the rotors keep spinning, the muffler smokes while any tower works).

### The three modes

Each tower has the Distillation Tower's three modes (GT's mode tab on the tower), as the old tower has through `FabModes`:

1. **Distillation Tower**: GT's distillation recipes (oil, air, the pack's fractions).
2. **Distillation**: the fab chemistry's columns (`fab_distillation`).
3. **Cryogenic Rectification**: the fab chemistry's cold columns (`fab_cryogenic_rectification`).

Outputs: GT's Distillation Tower puts the n-th output into the hatch on the n-th layer. Here the outputs go to the facility's fluid output hatches as in any GT multiblock
(any hatch with room, the fluid voiding switch of the tower's screen works); put a hatch for every fluid of the recipe, or switch voiding on for the ones you throw away.

## The rotors

Each tower runs with the rotor in the holder of its row (the holder at the same place along the facility's length). A tower **needs** its rotor (in the holder, front
face free); without it, the tower says why and does not start. Rotor stats are GT's (`rotorStats(power, efficiency, ...)`, efficiency 105 to 250 %).

At **full spin**, with `grade = (efficiency - 100 %) / 100`:

| | |
|---|---|
| speed | runs take `1 / (1 + 0.5 x grade + 0.04 x (holder tier - EV))` as long |
| energy | `EU/t x (1 - min(30 %, 0.2 x grade))` |

So a 130 % rotor is 15 % faster and saves 6 %, a 180 % rotor is 40 % faster and saves 16 %, a 250 % rotor in an EV holder is 75 % faster and saves 30 %, and each holder
tier above EV adds 4 % speed.

The bonus is the **spin's share**: the holder's rotor speed against its top speed (2000 + 1000 x tier). A working tower spins its rotor up by 4 a tick (GT's turbines: 1), so
EV is at full spin after 75 seconds of work; when every tower stands still it slows down by 3 a tick (about 100 seconds from full). Each run is modified anew, so the bonus
follows the spin. The rotor wears as in a turbine (1 durability a second of work).

## Filtered Distillation

The fab modes (Distillation, Cryogenic Rectification) are clean-room recipes: they run only while the facility is **filtered**. The plain distillation recipes (oil, air, the pack's fractions) never ask for it.

* The facility has a **cleanliness score of 0-100 %**, like the clean room's (on its screen).
* **Liquid Oxygen** (`gtceu:liquid_oxygen`) in any fluid input hatch, **100 mB every second**, constantly: while it comes the cleanliness climbs 100 % in 30 seconds; at 100 % the facility is filtered.
* Without oxygen the cleanliness falls (100 % in 60 seconds). The facility stays filtered until it is below **80 %**, then the fab recipes stop (the running ones are reset). The plain recipes keep running.
* A **Cleaning Maintenance Hatch** (UV in this pack's clean room type; in the maintenance place) replaces all of it: no oxygen, the cleanliness is always 100 % and the facility always filtered.

(The numbers are constants of `DistillationFacilityMachine`: `OXYGEN_PER_SECOND`, `CLEAN_PER_SECOND`, `DIRT_PER_SECOND`, `READY_LOST_BELOW`.)

## Crafting

`kubejs/server_scripts/mods/gtceu/distillation_facility.js`: the module is the old tower's recipe (HV hull, 4 large stainless pipes, 4 EV circuits, 2 HV pumps); the facility is 3 modules round an IV hull with 2 IV circuits, 2 IV pumps and an IV motor.

## Parallel hatches, textures, recipe pinning

* Each tower has its own pattern char (`l`, `m`, `n`) with **at most one Parallel Hatch**, so an auto-build puts one in every tower, not all three in the first. A module on its own also allows one.
* Hatches in a tower wear the Clean Stainless casing; the controller and the hatches on the spine wear the Steel Turbine Casing (`DistillationFacilityMachine.partAppearance`).
* The **Large Fractionating Distillery** now runs only distillery recipes (fork patch 0029); distillation recipes belong to the facility. Facility and modules share the recipe types, so the viewers group them as workstations.
* **EMI "+" button**: open a module's screen, then press "+" on a recipe in EMI. The recipe is pinned on the module (mode and recipe lock) via `PinTowerRecipePacket` (`DistillationEmiPlugin`).

## Output buffer

The towers do not need room in the output hatches when they start or finish: a finished run's outputs go into the facility's **output buffer** (256,000 mB of fluid in all, 64 different item stacks; `DistillationOutputBuffer`), and the facility moves them into the output hatches a few times a second as they have room. While the buffer has room the towers keep running; **when it is full the towers stop starting runs** (red line on the facility screen) until the hatches have taken enough. The room is a soft limit (a run that started may finish into a full buffer). The buffer is saved with the facility (not kept when the controller is broken). Parallels are limited by the inputs only.

## Open points

* The file's spine casing is the **Steel Turbine Casing** (`gtceu:steel_turbine_casing`); confirmed by Janne.
* The quests that named the Distillation Tower (extreme_voltage, photolithography chapters) name the facility now.
