# DTPF modules: a concept (not built yet)

Status: **concept for review**, nothing of it is in the game. The hall itself is `docs/dtpf.md`. This follows the Orbital
Array Mk2's modules (`kubejs/startup_scripts/gtceu/orbital_modules.js`, `af9-core` `OrbitalModuleMachine`): a module is a
small multiblock of its own that runs no recipe; the hall finds it when it forms and gives every run the bonus of each
module whose own structure stands. Modules of one kind stack up to a maximum.

## 1. Why modules fit this hall

A forging run is **1200 s** at UHV on one laser hatch, and the hall costs **10 A of LuV** and osmium plasma just to stand
(the magnetic field). Everything a player will want is what a run lacks: more output per run (parallels), a cheaper or
faster run, and a cheaper field. The modules are those three levers, and each lever costs upkeep, so a bigger hall is a
bigger commitment and not a free upgrade.

## 2. Where they stand

Option A (recommended): **flank sockets**. The three big rings (z 11, 21, 31) each end in a casing at the middle of
their left and right edge (x 0 and x 10, the beam's height). Replacing that casing with a *module socket* gives **6
sockets**; the four small rings' outer casings could give 8 more later. A module is a column of three (its controller
between two casings, like the Orbital modules) that sticks out sideways from the socket, so the hall's silhouette grows
two wings of machinery and the pattern of the hall itself stays exactly the structure Janne built.

Option B: **module rail** along the top prongs of the big rings (the casings at y 12), modules standing on them like the
Array's crown. More room, but it clutters the roof the force field's renderer looks past.

Option C: **no sockets, a bus**: modules anywhere within a few blocks, linked by a data stick (like the wireless hatches).
Simplest to build, and the least like the other endgame machines.

Whichever it is, the controller face and the column are the pack's usual: injection casing family, one animated hero
element, active only while the hall works.

## 3. The modules (numbers are a starting point)

| Module | Effect each | Max | Upkeep (LuV aux, on top of the hall's 10 A) |
|---|---|---|---|
| Parallel Lattice | +8 parallels (1 + 8 x 8 = 65, capped to the pack's **64**) | 8 | +2 A |
| Field Economiser | -5 % EU/t of the recipes (stacks with the ramp's -50 %) | 6 | +1 A |
| Chrono Accelerator | -10 % run time (stacks with the ramp's -25 %) | 3 | +2 A |
| Plasma Recycler | -25 % osmium plasma burned by the field | 2 | +1 A |
| Field Governor | the field survives a power cut for 30 s, and the stabilisers need 5 A instead of 10 | 1 | none (it is the saving) |
| Dimensional Injector (optional) | +10 % output of the run | 5 | +2 A |

Rules to keep it honest:

* **Parallels never pass 64** (pack rule). Parallel recipes need their amperage on the laser hatch (100 A UHV a run, so 64
  parallels is 6,400 A: a UEV or UIV laser hatch and a good reason for the Mk2's four-times amperage to matter).
* The modules' own power is **auxiliary** like the field's: the energy hatches already held in Auxiliary mode pay it.
  A hall with all 6 sockets full of the dearest modules pays about 20 A of LuV more.
* A module that loses its structure stops giving its bonus; the field is not affected.
* The ramp, the overclocks (perfect) and the modules multiply; the console shows the sum of all of it.

## 4. The screen

The hall's console gets one more row, **MODULES**, a strip of small tiles (one per kind: icon, count / max) and the same
hover tooltips as the gauges. The Jade tooltip shows the totals (parallels, EU %, time %).

## 5. Progression and recipes

* A module's controller is an assembly line recipe at **UHV-UEV**, built from the hall's own products (transcendent
  casing, bridge casing, resonance coil) and the Plasma Forge's matter, so the forge makes its own upgrades.
* The first module (Field Economiser) at UHV, the Parallel Lattice at UEV: the hall works first, grows second.

## 6. Open questions for tomorrow

1. Option A, B or C for where they stand.
2. Whether the Dimensional Injector (more output) is wanted: with 1200 s runs it is the strongest lever and the easiest to
   break the pack's balance with.
3. Whether recipe outputs should be scaled up for the 1200 s runs now, or left for the parallels to make worth it.
