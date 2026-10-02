# Space Elevator

GTNH's Space Elevator (https://wiki.gtnewhorizons.com/wiki/Space_Elevator): a tower on a cable that reaches into space. In AF9 it is
the **renewable ore source** of the pack from ZPM on: it sends Mining Drones to the asteroids and brings their ore home.

Code: `af9-core` `com.af9.core.elevator` (`SpaceElevatorMachine`: the asteroids, the motors, the modules, the cable; `SpaceElevatorScreen`:
its screen; `OreCatalog`: GT's ores and veins; `ClimberRide`: the climber's rides) and `com.af9.core.client.render.SpaceElevatorRender` (the
cable and the climber). KubeJS:
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
up to 12 at once, twelve MK-III modules on MK-III motors up to 96, on laser hatches. The basic tower has 12 module slots, the
**extended** one 24 (section 2): MK-III motors power 15 of them, MK-IV 18, MK-V all 24.

### Asteroids

`OreCatalog` lists every ore material of GT (a material with GT's ore property) and GT's ore veins (the registry of veins, those with a weight above
0), each vein with its **tier**: the lowest tier of its world-gen layer (stone and deepslate 1, netherrack 2, end stone 3, the Asteroid Field's
layer 4). An expedition of drone tier *n* picks one vein of tier *n* or lower, **weighted by the vein's weight**, and puts out its ores: half of the
stacks are the vein's first ore, the rest are shared by its other ores. Ores that **no vein holds** (the veins GT lost to AF9: pitchblende and
uraninite, and any ore a mod adds without a vein) are the **exotic asteroid**'s: the Mk-IV draws it one run in six, three of those ores at random. So
every ore GT has is mined here, and an ore another mod adds to GT needs no script.

The screen names the asteroid the elevator drew last (the vein it is made from). The ore is the raw ore (`raw_` item; the crushed ore for a material without one).

## 2. Structure

**GTNH's structure, block for block**: 35 x 35 and 43 high, 2,711 blocks (and its extended size, below). It is taken from GTNH's own
definition (`gtnhintergalactic.tile.multi.elevator.TileEntitySpaceElevator`, `STRUCTURE_PIECE_MAIN`; programming minecraft7771 and
BlueWeabo, design Sampsa, Jimbno, Adam and Baunti; GT5-Unofficial, LGPL-3.0) and held by the startup script as the table `SE_MAIN`:
the slices from the front to the middle one (the back half mirrors the front), each slice its rows from its highest block down. The
script unfolds it into GT's pattern (aisles front to back, rows top to bottom); both sizes were checked against GTNH's source, position
by position.

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

The tower spans 35 x 35 blocks, 9 chunks or more (the extended one 47 x 47): all of them have to be loaded for it to form and to work.

### The extended structure

GTNH's second size (`STRUCTURE_PIECE_EXTENDED`, the table `SE_EXTENSION`): a **ring of 47 x 47** round the tower's foot, in its bottom five
layers, with **twelve more module slots** (three in the middle of every side, on platforms of their own): 24 in all. It adds 432 Ultra High
Strength Concrete Floor (1,232), 188 Internal Structure (548) and up to 120 Base Casing (the new slots and the places round them: 593 to 905).

The size is **switched on the controller's screen** (the size switch, GTNH's extension button: section 5). The machine then checks the
other pattern (`SpaceElevatorMachine.getPattern()`: the startup script builds both and hands the extended one over,
`setExtendedPattern`): switched to extended, the tower only forms with the whole ring built. Switching takes a formed tower apart and
checks it anew, so a run that is on is lost. The structure preview has both sizes as its two pages (`previews`); the terminal builds
the size that is switched on.

GTNH only checks the extension from MK-III motors on. Here the size is the player's choice at any tier: with MK-I or MK-II motors the
extension's slots are simply not powered (6 and 12 slots).

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

## 4. The cable and the climber

While the structure is formed, `SpaceElevatorRender` draws the upper part of the elevator (a GT dynamic render, `hasBER`), as GTNH draws its
own (`gtnhintergalactic.render.RenderSpaceElevatorCable`, `TileEntitySpaceElevatorCable`). The numbers and the way it moves are GTNH's; **the
model and the textures are not**: GTNH's climber is a model of its own (by Adam, textured by Jimbno), and AF9 ships none of GTNH's files.
What is drawn here is made by hand, in code, after pictures of GTNH's.

* **The cable**: a rope of four bands wound round each other (an octagon 0.9 across, a turn every 2.96 blocks, each band 0.75 high), from the
  floor of the shaft (23 blocks under the Cable block, through the motors) **512 blocks** up, far past the world's top. A **blue light** runs
  up it every three seconds and lights the lamps of the bands it passes. Further than 96 blocks from the camera the rope is drawn as a plain
  eight-sided column. A band's tile is `assets/af9/textures/entity/space_elevator_strand.png`.
* **The climber**: a wheel round the cable (radius 6.2, dark, blue on top, gold underneath) with a hub and four spokes; a white pod stands
  on the end of three of them, a rack of six blue tanks hangs on the fourth, close by the wheel: about 23 blocks across. Its colours are cells
  of a 4 x 4 palette texture (`space_elevator.png`). It rests **50 blocks above the Cable block** (100 where that would be under y 100).
* Both are drawn at full brightness (as GTNH's), from 288 blocks away, level.

### The rides

`ClimberRide`, GTNH's animation: the climber moves a block a tick, slower over the first and the last 30 blocks of its way, and turns half a
degree a tick while a ride is on. Between rides it **stands still**.

* **Formation**: a tower that forms calls the climber down from orbit, 250 blocks above its rest (about 17 seconds). A tower that was formed
  before the world was loaded has it already.
* **Delivery**: while the elevator is **switched on** (whether or not a run is on), every 2,000 ticks (100 seconds) the climber rides up to
  orbit, waits 200 ticks and comes back: about 55 seconds.

The machine keeps the ride, the game time it began at and the climber's turn (`climberRide`, `climberStart`, `climberTurn`, synced); where the
climber is follows from the time since, so every client draws the same ride. Where the Cable block is from the controller:
`SpaceElevatorMachine.CABLE_UP`, `CABLE_BACK`.

## 5. The screen

`SpaceElevatorScreen` (the controller's `createUI`), laid out as GTNH's: a TecTech controller without an inventory, so one plain window
(198 x 192) filled by a dark blue screen, the elevator's buttons on the screen's lower right and the power switch under it. **Nothing on it
is configured**: TecTech's parameters, LEDs, power pass and safe void are left out, and so are GT's side tabs. The player inventory is not
shown (as in GTNH).

* **The text** (it scrolls when it is long), in short lines: GTNH's two first, *Incomplete Structure* or *Ready* and *Number of modules*;
  then the size, the motors' tier and the slots it powers, the powered modules and the expeditions they fly at once, what is wrong (no
  powered module, the cable under a roof), and the run: GT's status, progress and ore lines, the asteroid, the energy the hatches supply.
* **The size switch** (GTNH's extension button): the tower seen from above, grey for the basic structure, blue with its ring for the
  extended one (section 2).
* **The info sign**: GTNH's contributors, on hover (GTNH opens a window for them).
* **The logo**: the elevator's own.
* Under the screen, where TecTech has its LED strip: the **progress** of the run and the **power switch** (a soft mallet does the same).
  While the elevator is switched on the climber makes its deliveries (section 4).

The buttons and the info sign are GT's own; the size switch's picture and the logo are drawn for AF9
(`assets/af9/textures/gui/space_elevator/`), as GTNH's GUI textures are not used. The tooltip of a switch names the state it is in.

## 6. Not done

GTNH's modules are machines of their own (mining, pumping, assembler; each with its own buses, drone, parameters and an asteroid filter); here a
module is a block that makes the elevator fly more expeditions, the buses and the drone are the elevator's, and there are only Mining Modules.
GTNH's elevator also has a galaxy map for travel, and plasma, drill tips, rods and computation as inputs; here it runs on hydrogen and a coolant
and has no travel function (so its screen has no teleport button). GTNH's climber model and textures are GTNH's own and are not used: the
climber, the cable, the block textures and the screen's pictures are made by hand after pictures (sections 4 and 5).
