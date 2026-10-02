# Space Elevator

After GTNH's Space Elevator (https://wiki.gtnewhorizons.com/wiki/Space_Elevator): a tower on a cable that reaches into space. In AF9 it is
the **renewable ore source** of the pack from ZPM on: it sends Mining Drones to the asteroids and brings their ore home.

Code: `af9-core` `com.af9.core.elevator` (`SpaceElevatorMachine`, the asteroids; `OreCatalog`, GT's ores and veins) and
`com.af9.core.client.render.SpaceElevatorRender` (the platform that turns on the cable). KubeJS:
`kubejs/startup_scripts/gtceu/space_elevator.js` (blocks, drones, recipe type, the machine and its pattern),
`kubejs/server_scripts/mods/gtceu/space_elevator.js` (crafting and the expeditions).

## 1. How it works

`gtceu:space_elevator` runs the recipe type `gtceu:space_mining`: a **Mining Drone** (`kubejs:space_mining_drone_mk1..4`, **not used up**) in
the input bus, **hydrogen** and a **supercooled coolant** in the fluid hatches, and energy for minutes. Nothing is made by the recipe
itself: when a run starts (`SpaceElevatorMachine.ASTEROID`, a recipe modifier that re-rolls every run) the elevator draws an
**asteroid** and the run puts out its ore, **8 to 48 stacks of raw ore** in the output buses (the recipe viewers show no outputs).

| Drone | Reaches | Hydrogen | Coolant (supercooled) | Energy | Time | Stacks |
|---|---|---|---|---|---|---|
| Mk-I | tier 1 ores (the Overworld's) | 64 B | 50 B hydrogen | 4A ZPM (524,288 EU/t) | 3 min | 8-16 |
| Mk-II | tier 1-2 (and the Nether's) | 80 B | 64 B argon | 8A ZPM (1,048,576 EU/t) | 4 min | 12-24 |
| Mk-III | tier 1-3 (and the End's) | 96 B | 80 B xenon | 16A ZPM (2,097,152 EU/t) | 5 min | 16-32 |
| Mk-IV | all, and the exotic asteroid | 100 B | 100 B endion | 32A ZPM (4,194,304 EU/t) | 6 min | 24-48 |

The coolants are the Supercooling Cryostat's (`cryogenics.js`). The energy is the recipe's: the machine only starts a run its hatches can
supply in full (`IPowerGated`, as the Particle Accelerator): Mk-I runs on one 4A ZPM hatch, Mk-II on two, Mk-III on four (the most
there are), Mk-IV on laser hatches (up to two). A run that cannot start waits.

### Asteroids

`OreCatalog` lists every ore material of GT (a material with GT's ore property) and GT's ore veins (the registry of veins, those with a weight above
0), each vein with its **tier**: the lowest tier of its world-gen layer (stone and deepslate 1, netherrack 2, end stone 3, the Asteroid Field's
layer 4). An expedition of drone tier *n* picks one vein of tier *n* or lower, **weighted by the vein's weight**, and puts out its ores: half of the
stacks are the vein's first ore, the rest are shared by its other ores. Ores that **no vein holds** (the veins GT lost to AF9: pitchblende and
uraninite, and any ore a mod adds without a vein) are the **exotic asteroid**'s: the Mk-IV draws it one run in six, three of those ores at random. So
every ore GT has is mined here, and an ore another mod adds to GT needs no script.

The screen shows where the last expedition went. The ore is the raw ore (`raw_` item; the crushed ore for a material without one).

## 2. Structure

13 x 13 x 26, fixed (a layer of the pattern each aisle, bottom to top; `seChar` in the startup script), 714 blocks:

* the **base**: two layers of a disc of Space Elevator Base Casing (272 blocks) with the controller in the front of its bottom layer; the hatches go in it
  (maxima: 4 energy, 2 laser, 4 fluid input, 1 item input, 4 item output, 1 maintenance);
* a **cone** of Space Elevator Glass (224 blocks), a shell that narrows from radius 5.5 to 2.5 over layers 2 to 8;
* a **pillar** of 3 x 3 (Support Structure at the corners, Base Casing on the sides) up to layer 25, with the **Space Elevator Cable** in its middle (25).

The blocks (all assembler recipes, LuV; the cable ZPM): casing 4 from a naquadah alloy frame, 4 plates and a fusion casing; support 4 from two frames and 4
tungsten steel plates; glass 8 from 8 fusion glass and 2 plates; cable 2 from 8 rods and 2 LuV field generators.

## 3. Crafting

The Space Elevator: Assembly Line, **ZPM**, 1200 ticks: a ZPM machine hull, 8 motors, 4 field generators, 2 sensors, 2 emitters, 4 circuits, 16 base casings,
8 supports, 8 glass, 2 cables, 4 double naquadah alloy plates, 4608 mB soldering alloy. The drones: assembler, 600 ticks, soldering alloy and parts of ZPM (Mk-I) or UV (GT has no
parts above UV while `highTierContent` is off): Mk-I a robot arm, 2 sensors, an emitter, 4 circuits and 4 naquadah alloy plates; Mk-II the same in UV;
Mk-III 2 arms, 4 sensors, 2 emitters, 8 circuits and 4 tritanium plates; Mk-IV 4 arms, 8 sensors, 4 emitters, 16 circuits and 4 neutronium plates.

## 4. The platform

While the structure is formed, `SpaceElevatorRender` draws the upper part of the elevator (a GT dynamic render, `hasBER`): a platform on the cable 27 blocks above the
controller and 6 behind it (so it turns with the controller), **turning slowly** around the cable: `SPIN` 0.08 degrees a tick (a full turn in a little under four
minutes; twice that while a run is on). It is a model made in code from a few shapes, no model file: a hub that holds the cable, a ring of 4.7 blocks radius
with light strips on eight spokes, eight posts with lamps, six blue glass tank pods on arms, orange spotlights under the ring; above it the cable runs up
150 blocks (as far as the world's top) to a small station with solar wings. The colours are cells of a 4 x 4 palette texture
(`assets/af9/textures/entity/space_elevator.png`); the lights are drawn at full brightness, the glass is translucent. The render box covers the platform and the
cable, so it shows from far away (256 blocks).

## 5. Not done

GTNH's elevator also has **modules** (mining, pumping, assembler), a galaxy map for travel, and plasma, drill tips and rods as inputs. Here the elevator mines
by itself, with hydrogen and a coolant, and has no travel function. The climber that oscillates on GTNH's cable is replaced by the turning platform.
