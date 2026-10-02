# Space Elevator

GTNH's Space Elevator (https://wiki.gtnewhorizons.com/wiki/Space_Elevator): a tower on a cable that reaches into space. In AF9 it is
the **renewable ore source** of the pack from ZPM on: it sends Mining Drones to the asteroids and brings their ore home.

Code: `af9-core` `com.af9.core.elevator` (`SpaceElevatorMachine`: the asteroids, the motors, the modules, the cable; `OreCatalog`: GT's
ores and veins) and `com.af9.core.client.render.SpaceElevatorRender` (the cable and the platform that turns on it). KubeJS:
`kubejs/startup_scripts/gtceu/space_elevator.js` (blocks, drones, recipe type, the machine and its structure),
`kubejs/server_scripts/mods/gtceu/space_elevator.js` (crafting and the expeditions).

## 1. How it works

`gtceu:space_elevator` runs the recipe type `gtceu:space_mining`: a **Mining Drone** (`kubejs:space_mining_drone_mk1..4`, **not used up**) in
an input bus, **hydrogen** and a **supercooled coolant** in the fluid hatches, and energy for minutes. Nothing is made by the recipe
itself: when a run starts (`SpaceElevatorMachine.ASTEROID`, a recipe modifier that re-rolls every run) the elevator draws an
**asteroid** and the run puts out its ore, **8 to 48 stacks of raw ore an expedition** in the output buses (the recipe viewers show no
outputs; the controller's screen lists the ore of the run that is on). The table is one expedition; the **Mining Modules** of the tower
fly several at once (below).

| Drone | Reaches | Hydrogen | Coolant (supercooled) | Energy | Time | Stacks |
|---|---|---|---|---|---|---|
| Mk-I | tier 1 ores (the Overworld's) | 64 B | 50 B hydrogen | 4A ZPM (524,288 EU/t) | 3 min | 8-16 |
| Mk-II | tier 1-2 (and the Nether's) | 80 B | 64 B argon | 8A ZPM (1,048,576 EU/t) | 4 min | 12-24 |
| Mk-III | tier 1-3 (and the End's) | 96 B | 80 B xenon | 16A ZPM (2,097,152 EU/t) | 5 min | 16-32 |
| Mk-IV | all, and the exotic asteroid | 100 B | 100 B endion | 32A ZPM (4,194,304 EU/t) | 6 min | 24-48 |

The coolants are the Supercooling Cryostat's (`cryogenics.js`). The energy is the recipe's: the machine only starts a run its hatches can
supply in full (`IPowerGated`, as the Particle Accelerator): Mk-I runs on one 4A ZPM hatch, Mk-II on two, Mk-III on four (the most
there are), Mk-IV on laser hatches (up to two). A run that cannot start waits.

### Modules and motors

As in GTNH the elevator itself does nothing: its **modules** do the work, and its **motors** say how many of them.

* A **Space Mining Module** (`kubejs:space_mining_module_mk1..3`, a block) in a **module slot** of the tower flies expeditions:
  **MK-I 2 at once, MK-II 4, MK-III 8** (GTNH's parallels). Without a powered module nothing flies.
* The **motors' tier** (the 88 motors round the shaft, all of one tier, `kubejs:space_elevator_motor_mk1..5`) powers
  **6 / 12 / 15 / 18 / 24 module slots** (MK-I to MK-V, GTNH's numbers), and only modules of **its own tier or lower** (a MK-III module
  needs MK-III motors). With more modules than slots the best ones are powered and the rest stand idle (GTNH refuses such a tower).
* A run flies as many expeditions at once as the powered modules allow, the hatches can **supply in full** (EU/t), the hydrogen
  and the coolant in the hatches **last for** and the output buses **have room for**: every expedition takes the recipe's full
  hydrogen, coolant and EU/t, and all of a run go to the **same asteroid** (its ore times the expeditions). One drone in the bus
  serves them all: it is not used up.

So one MK-I module with one 4A ZPM hatch flies a single Mk-I expedition; with 8A it flies two. Six MK-I modules on MK-I motors fly
up to 12 at once, twelve MK-III modules on MK-III motors up to 96, on laser hatches.

### Asteroids

`OreCatalog` lists every ore material of GT (a material with GT's ore property) and GT's ore veins (the registry of veins, those with a weight above
0), each vein with its **tier**: the lowest tier of its world-gen layer (stone and deepslate 1, netherrack 2, end stone 3, the Asteroid Field's
layer 4). An expedition of drone tier *n* picks one vein of tier *n* or lower, **weighted by the vein's weight**, and puts out its ores: half of the
stacks are the vein's first ore, the rest are shared by its other ores. Ores that **no vein holds** (the veins GT lost to AF9: pitchblende and
uraninite, and any ore a mod adds without a vein) are the **exotic asteroid**'s: the Mk-IV draws it one run in six, three of those ores at random. So
every ore GT has is mined here, and an ore another mod adds to GT needs no script.

The screen names the asteroid the elevator drew last (the vein it is made from). The ore is the raw ore (`raw_` item; the crushed ore for a material without one).

## 2. Structure

**GTNH's structure, block for block**: 35 x 35 and 43 high, 2,711 blocks. It is taken from GTNH's own definition
(`gtnhintergalactic.tile.multi.elevator.TileEntitySpaceElevator`, `STRUCTURE_PIECE_MAIN`; programming minecraft7771 and BlueWeabo,
design Sampsa, Jimbno, Adam and Baunti; GT5-Unofficial, LGPL-3.0) and held by the startup script as the table `SE_MAIN`: the slices
from the front to the middle one (the back half mirrors the front), each slice its rows from its highest block down. The script
unfolds it into GT's pattern (aisles front to back, rows top to bottom); it was checked against GTNH's source, position by position.

| Block | Count | Where |
|---|---|---|
| Ultra High Strength Concrete Floor (`kubejs:ultra_high_strength_concrete_floor`) | 800 | the floor, a disc 35 across |
| Space Elevator Base Casing (`kubejs:space_elevator_base_casing`) | 593 to 785 | the blue of the tower: frame, column, feet |
| Space Elevator Support Structure (`kubejs:space_elevator_support`) | 620 | the dark ribs that taper to the top |
| Space Elevator Internal Structure (`kubejs:space_elevator_internal_structure`) | 360 | the decks: the second layer, the rings up the column, the crown |
| Space Elevator Motor (`kubejs:space_elevator_motor_mk1..5`) | 88 | the central column, round the shaft, 22 layers |
| Neutronium Frame Box (`gtceu:neutronium_frame`) | 56 | four arcs half way up the frame |
| Space Elevator Cable (`kubejs:space_elevator_cable`) | 1 | on top of the shaft, 22 above the controller |
| Space Mining Module (`kubejs:space_mining_module_mk1..3`) | 0 to 12 | the module slots: round the column, three a side, in the 4th layer |
| the controller | 1 | **front centre of the central column, 4th layer** |

Rules of the structure (GTNH's):

* **The motors are all of one tier** (`SpaceElevatorMachine.motors()`: any of MK-I to MK-V, the first one found sets the tier). The
  screen shows it.
* **The cable must see the sky** (`SpaceElevatorMachine.cable()`): every block above the Cable block is air, up to the world's top.
  The shaft under it (23 blocks, down through the motors) is air too. Something built over a formed tower stops the expeditions
  until it is gone (the screen says so).
* **Upright only**: the controller faces sideways, the tower cannot be turned on its side or flipped.
* **Hatches** have maxima only: 4 energy and 2 laser hatches in the **bottom centre casings** (72 places: the floor under the column
  and three layers round its foot); 8 fluid input hatches, 2 item input and 8 item output buses there or in the **module slots**
  (12 slots round the column, three a side: the module's own place and 9 places round it). There is no maintenance hatch (as in GTNH).
* **A module slot** holds a Mining Module of any tier, or Base Casing (`SpaceElevatorMachine.modules()` notes the modules down; what
  they do is section 1).

The tower spans 35 x 35 blocks, 9 chunks or more: all of them have to be loaded for it to form and to work.

### The blocks

All Assembler recipes; a craft makes many, the tower takes hundreds:

| Block | A craft makes | From | Tier |
|---|---|---|---|
| Ultra High Strength Concrete Floor | 8 | 8 dark concrete, 2 tungsten steel rods, 72 mB polybenzimidazole | IV |
| Base Casing | 16 | a naquadah alloy frame, 4 naquadah alloy plates, 8 tungsten steel plates (circuit 1) | LuV |
| Support Structure | 16 | 2 naquadah alloy frames, 8 tungsten steel plates (circuit 2) | LuV |
| Internal Structure | 16 | a naquadah alloy frame, 4 osmiridium plates, 4 tungsten steel plates (circuit 3) | LuV |
| Cable | 1 | 32 carbon fibre plates, 8 naquadah alloy rods, 2 LuV field generators, soldering alloy | ZPM |
| Motor MK-I | 4 | 4 ZPM electric motors, a naquadah alloy frame, 4 naquadah alloy plates, soldering alloy | ZPM |
| Motor MK-II | 4 | 4 MK-I, 4 UV electric motors, 4 tritanium plates | UV |
| Motor MK-III | 4 | 4 MK-II, 4 UV electric motors, 4 neutronium plates, 8 endionite foil | UV |
| Motor MK-IV | 4 | 4 MK-III, 4 UV field generators, 4 neutronium gears, 2 UHV circuits | UV |
| Motor MK-V | 4 | 4 MK-IV, 2 gravi stars, 4 chromodynium plates, 4 UHV circuits | UV |
| Mining Module MK-I | 1 | a ZPM machine hull, 2 robot arms, 2 sensors, 2 emitters and 4 circuits of ZPM, 4 base casings | ZPM |
| Mining Module MK-II | 1 | a MK-I, 2 robot arms, 2 sensors, 2 emitters and 4 circuits of UV, 4 tritanium plates | UV |
| Mining Module MK-III | 1 | a MK-II, 4 UV robot arms, 2 UV field generators, 4 UHV circuits, 4 neutronium plates | UV |

The Neutronium Frame Boxes are GT's (neutronium comes from the Mk-III fusion reactor).

## 3. Crafting

The Space Elevator: Assembly Line, **ZPM**, 1200 ticks: a ZPM machine hull, 8 motors, 4 field generators, 2 sensors, 2 emitters, 4 circuits, 16 base casings,
8 supports, 8 internal structures, a cable, 4 double naquadah alloy plates, 4608 mB soldering alloy. The drones: assembler, 600 ticks, soldering alloy and
parts of ZPM (Mk-I) or UV (GT has no parts above UV while `highTierContent` is off): Mk-I a robot arm, 2 sensors, an emitter, 4 circuits and 4 naquadah
alloy plates; Mk-II the same in UV; Mk-III 2 arms, 4 sensors, 2 emitters, 8 circuits and 4 tritanium plates; Mk-IV 4 arms, 8 sensors, 4 emitters, 16
circuits and 4 neutronium plates.

## 4. The cable and the platform

While the structure is formed, `SpaceElevatorRender` draws the upper part of the elevator (a GT dynamic render, `hasBER`): the **cable**, from the Cable
block on top of the shaft (22 above the controller and 3 behind it, so it turns with the controller) up 150 blocks (as far as the world's top) to a small
station with solar wings, and the **platform** that rides it 64 blocks above the Cable block (GTNH's climber), **turning slowly** around the cable:
`SPIN` 0.08 degrees a tick (a full turn in a little under four minutes; twice that while a run is on). It is a model made in code from a few shapes,
no model file: a hub that holds the cable, a ring of 4.7 blocks radius with light strips on eight spokes, eight posts with lamps, six blue glass tank
pods on arms, orange spotlights under the ring. The colours are cells of a 4 x 4 palette texture (`assets/af9/textures/entity/space_elevator.png`); the
lights are drawn at full brightness, the glass is translucent. The render box covers the cable and the platform, so they show from far away (256 blocks
from the platform). The numbers are `SpaceElevatorMachine.CABLE_UP`, `CABLE_BACK`, `PLATFORM_UP` and `CABLE_LENGTH`.

## 5. Not done

GTNH's modules are machines of their own (mining, pumping, assembler; each with its own buses, drone, parameters and an asteroid filter); here a
module is a block that makes the elevator fly more expeditions, the buses and the drone are the elevator's, and there are only Mining Modules.
GTNH's elevator also has an **extended** structure with twelve more module slots, a galaxy map for travel, and plasma, drill tips, rods and computation
as inputs; here it runs on hydrogen and a coolant and has no travel function. GTNH's climber goes up and down its cable; here the platform turns on it.
