# Distillation Facility

The IV distillation path. A 10 x 7 x 15 walk-through of **real GT fluid pipes** round three **Distillation Tower Modules**, built 1:1 from
Janne's `v3.nbt` (kubejs/startup_scripts/gtceu/distillation_facility.js, af9-core `com.af9.core.distillation`). It replaces the
Distillation Tower as the machine to build for distillation; the Distillation Tower itself is **still there** (not disabled, see the open point at the end).

## The structure

* Controller: where the file's green wool stood, in the arch at the front (controller facing SOUTH, as the file stands in the world; turned for the other
  facings). Machine id `gtceu:distillation_facility`.
* Casings, as in the file: Inert Machine Casing (the arch, the middle block), Stable Machine Casing (the two turbine houses), Stainless Firebox Casing
  (the tower feet), Clean Machine Casing (the towers), Kanthal Coil Blocks (the tower coils), HSLA Steel Frames (3, between the pipes).
* Pipes: Tungsten and Stainless Steel Small Fluid Pipes, 47 in all, each with the file's exact `connections` mask. The pattern accepts a pipe spot only for
  that exact block **and** that exact mask, and the controller sets the masks itself while the facility is unformed, so an auto-built facility forms without
  wrench work. This is the Chemical Plant's pipe tech ([chemical-plant.md](chemical-plant.md): `PipeBlockMixin` keeps the connections, the same data tag),
  in `DistillationFacilityPipes`. The frames some of the file's pipes carry are not asked for (an auto-build cannot place them).
* **Hatches go only on the Turbine Casing spine**: the 14 Steel Turbine Casings of the file down the middle of the floor. Up to **6 energy hatches**, **1 maintenance
  hatch**, up to 3 fluid input and 8 fluid output hatches, 2 item input and 2 item output buses. Parts have maximums only (the pack's rule).
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
* The facility is **its own clean room** (clean stainless casing all over): the clean-room recipes of the fab modes run without building a GT Cleanroom round it.
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

## Open points

* **The old Distillation Tower is not disabled.** Hiding it from the recipe viewers and the crafting is a one-line change once Janne says so (`FabModes` already gives it the fab
  modes; the facility's modes are the same).
* The file's spine casing is the **Steel Turbine Casing** (`gtceu:steel_turbine_casing`); that is what the pattern asks for.
* The facility is its own clean room (a decision to confirm): the fab distillation and cryogenic modes would otherwise need a GT Cleanroom round a 10 x 15 facility.
