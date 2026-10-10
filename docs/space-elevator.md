# Space Elevator

GTNH's Space Elevator (https://wiki.gtnewhorizons.com/wiki/Space_Elevator): a tower on a cable that reaches into space. In AF9 it is
the **renewable ore source** of the pack from ZPM on: it sends Mining Drones to the asteroids and brings their ore home. A **liquid
mission** sends them to a planet instead, for a fluid (GTNH's Space Pumping).

Code: `af9-core` `com.af9.core.elevator` (`SpaceElevatorMachine`: the missions, the asteroids, the motors, the modules, the cable, the
drone slot, what it lacks; `OreCatalog`: GT's ores and veins; `PlanetCatalog`: the planets' fluids; `ClimberRide`: the climber's rides),
`com.af9.core.machine.console.SpaceElevatorConsoleWidget`
(its screen) and `com.af9.core.client.render.SpaceElevatorRender` (the cable and the climber). KubeJS:
`kubejs/startup_scripts/gtceu/space_elevator.js` (blocks, drones, the two recipe types, the machine and its structure),
`kubejs/server_scripts/mods/gtceu/space_elevator.js` (crafting and the expeditions).

## 0. The modules are machines of their own (sub-multiblocks)

As in GTNH the elevator flies nothing itself. The work is done by its **Mining Modules** (`gtceu:space_mining_module_mk1..3`,
`SpaceModuleMachine`): each is a small multiblock, a **controller** that stands in a module slot of the tower, with

* its **own screen** (the console of sections 5 and 6: drone slot, mission picker, what it lacks, counters, size switch),
* its **own drone** and its **own hatches**, in the casings round it (up to 2 item and 2 fluid inputs, 2 Coolant Hatches, 4 item and 4
  fluid outputs; the ore or the fluid of its missions goes to *its* output hatches),
* its **own energy buffer**, filled every tick from the tower's energy and laser hatches (`SpaceElevatorMachine.powerTick`): the
  expeditions' energy is taken from that buffer.

The tower finds the modules when it forms and **connects** them (`SpaceModuleMachine.connect`): which motors it has, how many slots they
power, and whether this module is powered (the best tiers first, up to the slots, never above the motors' tier). A module that is
not powered stays dark; one outside a formed tower is not connected and does nothing. The tower's own screen shows the motors,
the modules and the size switch; everything about drones and missions below is on the **module's** screen. Where the text below says
"the elevator" for a drone, a hatch, a mission or a screen, it means the module.

## 1. How it works

`gtceu:space_elevator` runs the recipe type `gtceu:space_mining`: a **Mining Drone** (`af9:space_mining_drone_mk1..4`, **not used
up**) in the drone slot of its screen (section 5; or in an input bus), **hydrogen** in a fluid input hatch, a **supercooled coolant**
in **Coolant Hatches** (below) and energy for minutes. Nothing is made by the recipe itself: when a run starts
(`SpaceElevatorMachine.ASTEROID`, a recipe modifier that re-rolls every run) the elevator draws an **asteroid** and the run puts out
its ore, **8 to 48 stacks of raw ore an expedition** in the output buses. The recipe viewers' page lists the ores the drone's
asteroids hold (section 6); the controller's screen lists the ore of the run that is on. The table is one expedition; the **Mining
Modules** of the tower fly several at once (below).

| Drone | Reaches | Hydrogen | Coolant (supercooled) | Energy | Time | Stacks |
|---|---|---|---|---|---|---|
| Mk-I | tier 1 ores (the Overworld's) | 64 B | 50 B hydrogen | 4A ZPM (491,520 EU/t) | 3 min | 8-16 |
| Mk-II | tier 1-2 (and the Nether's) | 80 B | 64 B argon | 8A ZPM (983,040 EU/t) | 4 min | 12-24 |
| Mk-III | tier 1-3 (and the End's) | 96 B | 80 B xenon | 16A ZPM (1,966,080 EU/t) | 5 min | 16-32 |
| Mk-IV | all, and the exotic asteroid | 100 B | 100 B endion | 32A ZPM (3,932,160 EU/t) | 6 min | 24-48 |

The coolants are the Supercooling Cryostat's (`cryogenics.js`). The energy is the recipe's, a ZPM recipe of several amps (the recipe
viewers say "ZPM" and the amps): the machine only starts a run its hatches can
supply in full (`IPowerGated`, as the Particle Accelerator): Mk-I runs on one 4A ZPM hatch, Mk-II on two, Mk-III on four (the most
there are), Mk-IV on laser hatches (up to two). A run that cannot start waits.

### The coolant: Coolant Hatches

As AF9's other cooled machines the elevator has **Coolant Hatches** for its supercooled coolant (`gtceu:<tier>_coolant_hatch`,
`cryogenics.js`: a fluid input hatch that takes supercooled fluids only): **up to 4**, in the places of the other hatches (section 2).
The preview shows one, the terminal builds two, and the screen names the Coolant Hatch when the coolant runs short. The hydrogen goes
through fluid input hatches: a Coolant Hatch does not take it.

A Coolant Hatch holds an eighth of a fluid hatch of its tier, and an expedition takes all its coolant when it starts. Four of them
hold the coolant of this many expeditions at once:

| 4 Coolant Hatches | Hold | Mk-I (50 B) | Mk-II (64 B) | Mk-III (80 B) | Mk-IV (100 B) |
|---|---|---|---|---|---|
| ZPM (128 B each) | 512 B | 10 | 8 | 6 | 5 |
| UV (256 B each) | 1,024 B | 20 | 16 | 12 | 10 |
| UHV (512 B each) | 2,048 B | 40 | 32 | 25 | 20 |

A bigger run takes the rest of its coolant from the **fluid input hatches**: to GT a Coolant Hatch is a fluid input hatch, and GT
takes a recipe's fluids from all of them together, so supercooled coolant in a fluid input hatch counts as well (the screen's coolant
is `SpaceElevatorMachine.stockOf`: all of it).

### Liquid missions

Instead of an asteroid an expedition can go to a **planet** and bring home a **fluid**: GTNH's Space Pumping
(`gtnhintergalactic.recipe.SpacePumpingRecipes`, the table of its Space Pumping Module), as a mission of AF9's elevator.

* **The same flight**: a liquid mission takes what the drone's ore mission takes (the table above: hydrogen, coolant, energy, time)
  and is flown by the same Mining Modules, as many at once. Its recipe is the ore mission's in a recipe type of its own
  (`gtceu:space_pumping`); the fluid is not in the recipe: the run gets it when it starts (`SpaceElevatorMachine.MISSION`).
* **Picked on the screen** (section 5): the target (the asteroids, or a **planet type**, GTNH's 2 to 8 plus AF9's
  own type 9) and the planet's fluid. The
  elevator flies **one kind of mission at a time**: the kind is its recipe type that is on (GT's own mode tab is left out of the
  screen). A run that is on flies to its end.
* **The drone says how far**: MK-I reaches planet types 2 and 3, MK-II 4 and 5, MK-III 6 and 7, MK-IV types 8 and 9
  (`PlanetCatalog.droneFor`), each the nearer ones too.
* **GTNH's amounts**: what GTNH's module pumps in a second (for 1A of UHV) is here what one mission brings. A better drone brings no
  more of a planet's fluid, it only reaches further.
* The fluid goes to **fluid output hatches** (up to 6; an ME one takes any run): a run flies as many missions as they have room for.

| Planet type | Drone | Fluids (buckets a mission) |
|---|---|---|
| 2 | MK-I | chlorobenzene 896 |
| 3 | MK-I | lava 1,800, natural gas 1,400 |
| 4 | MK-II | sulfuric acid 784, molten iron 896, oil 1,400, heavy oil 1,792, molten lead 896, raw oil 1,400, light oil 780, carbon dioxide 1,680 |
| 5 | MK-II | carbon monoxide 4,480, helium-3 2,800, salt water 2,800, helium 1,400, liquid oxygen 896, neon 32, argon 32, krypton 8, methane 1,792, hydrogen sulfide 392, ethane 1,194 |
| 6 | MK-III | deuterium 1,568, tritium 240, ammonia 240, xenon 16, ethylene 1,792 |
| 7 | MK-III | hydrofluoric acid 672, fluorine 1,792, nitrogen 1,792, oxygen 1,792 |
| 8 | MK-IV | hydrogen 1,568, liquid air 875, molten copper 672, distilled water 17,920, radon 64, molten tin 672 |
| 9 (AF9) | MK-IV | bio diesel 1,400, bioethanol 1,792, benzene 1,400, chloroform 896, chlorobenzene 1,120, hydrogen sulfide 784, radon 128, methane 2,200, ethane 1,500, helium 1,800, argon 64, neon 64, krypton 16, xenon 32, helium plasma 80, nitrogen plasma 20, oxygen plasma 12, iron plasma 2, argon plasma 24 |

The last five of planet type 9 are the **termination shock**: plasma skimmed from the solar wind, in the 40 : 10 : 6 : 1 the Mega
Fusion Reactor fuses star matter from (helium, nitrogen, oxygen and iron plasma), and argon plasma for the turbines
(`docs/fusion-power.md`). A mission's 80 B of helium plasma is worth 6.5 GEU in a plasma turbine, the price of a Mk-IV flight.

GTNH's gas types keep their numbers (`PlanetCatalog`: planet type 5, gas type 2 is helium-3). Three of GTNH's fluids are not there:
ender goo (3, 1), extra heavy oil (3, 2) and GalaxySpace's unknown water (8, 4), which GregTech does not have here. The argon and the
xenon missions bring less than their drone's coolant is made of (a MK-II mission takes 64 B of supercooled argon, a MK-III one 80 B of
supercooled xenon): GTNH's numbers, kept as they are.

### Modules and motors

As in GTNH the elevator itself does nothing: its **modules** do the work, and its **motors** say how many of them.

* A **Space Mining Module** (`gtceu:space_mining_module_mk1..3`, a controller of its own) in a **module slot** of the tower flies expeditions:
  **one at once each** (no parallels: more modules fly more expeditions, a faster run comes from the overclock of the hatches' voltage,
  like any GT machine). Without a powered module nothing flies.
* The **motors' tier** (the 88 motors round the shaft, all of one tier, `af9:space_elevator_motor_mk1..5`) powers
  **6 / 12 / 15 / 18 / 24 module slots** (MK-I to MK-V, GTNH's numbers), and only modules of **its own tier or lower** (a MK-III module
  needs MK-III motors, and on lesser motors it does nothing: the screen says so). With more modules than slots the best ones are
  powered and the rest stand idle (GTNH refuses such a tower, and one with a module above its motors).
* A module's run is **one expedition**: it takes the recipe's hydrogen, coolant and EU/t once, when the hatches can supply them in
  full and the output buses have room, and it overclocks like any GT machine as far as the hatches' voltage and amps go (each
  overclock: four times the EU/t, half the time; the amps of the recipe stay the amps). The drone is not used up.

So one MK-I module flies one Mk-I expedition (4 A of ZPM at the recipe's voltage; a hatch of a higher voltage overclocks it, a
shorter run at four times the EU/t per tier). Six MK-I modules on MK-I motors fly 6 at once, twelve MK-III modules on MK-III motors
12, on laser hatches. The basic tower has 12 module slots, the **extended** one 24 (section 2): MK-III motors power 15 of them,
MK-IV 18, MK-V all 24.

### Asteroids

`OreCatalog` lists every ore material of GT (a material with GT's ore property) and GT's ore veins (the registry of veins, those with a weight above
0), each vein with its **tier**: the lowest tier of its world-gen layer (stone and deepslate 1, netherrack 2, end stone 3, the Asteroid Field's
layer 4). An expedition of drone tier *n* picks one vein of tier *n* or lower, **weighted by the vein's weight**, and puts out its ores: half of the
stacks are the vein's first ore, the rest are shared by its other ores. **Lean ores** (`SpaceMissionMachine.LEAN_DIVISOR`: quantanium, pitchblende,
uraninite) bring a quarter of their share: the UHV gate and the two closed old uranium ways stay a trickle at full Mk-IV yields. Ores that **no vein holds**
(the veins GT lost to AF9: pitchblende and
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
| Ultra High Strength Concrete Floor (`af9:ultra_high_strength_concrete_floor`) | 800 | the floor, a disc 35 across |
| Space Elevator Base Casing (`af9:space_elevator_base_casing`) | 593 to 785 | the blue of the tower: frame, column, feet |
| Stress-Proof Casing (`gtceu:stress_proof_casing`) | 620 | the dark ribs that taper to the top |
| Space Elevator Internal Structure (`af9:space_elevator_internal_structure`) | 360 | the decks: the second layer, the rings up the column, the crown |
| Space Elevator Motor (`af9:space_elevator_motor_mk1..5`) | 88 | the central column, round the shaft, 22 layers |
| Neutronium Frame Box (`gtceu:neutronium_frame`) | 56 | four arcs half way up the frame |
| Space Elevator Cable (`af9:space_elevator_cable`) | 1 | on top of the shaft, 22 above the controller |
| Space Mining Module (`gtceu:space_mining_module_mk1..3`) | 0 to 12 | the module slots: round the column, three a side, in the 4th layer |
| the controller | 1 | **front centre of the central column, 4th layer** |

Rules of the structure (GTNH's):

* **The motors are all of one tier** (`SpaceElevatorMachine.motors()`: any of MK-I to MK-V, the first one found sets the tier). The
  screen shows it.
* **The cable must see the sky** (`SpaceElevatorMachine.cable()`): every block above the Cable block is air, up to the world's top.
  The shaft under it (23 blocks, down through the motors) is air too. Something built over a formed tower stops the expeditions
  until it is gone (the screen says so).
* **Upright only**: the controller faces sideways, the tower cannot be turned on its side or flipped.
* **Hatches** have maxima only: 4 energy and 2 laser hatches in the **bottom centre casings** (72 places: the floor under the column
  and three layers round its foot); 8 fluid input hatches, **4 Coolant Hatches** (section 1), 6 fluid output hatches (the liquid
  missions'), 2 item input and 12 item output buses
  there or in the **module slots** (12 slots round the column, three a side: the module's own place and 9 places round it). A Coolant
  Hatch is a part of its own here and not one of the 8 fluid hatches (`SpaceElevatorMachine.plainFluidHatches`: to GT it is a fluid
  input hatch too). There is no maintenance hatch (as in GTNH).
  Any tier is taken, but the structure preview shows **ZPM parts** and the terminal builds with them
  (`SpaceElevatorMachine.zpmFirst`: GT would take its first ones, ULV, which hold nothing an expedition needs). A tower the terminal
  builds in creative has 4 energy hatches, 2 input buses, 2 Coolant Hatches, 2 fluid output hatches, 2 fluid input hatches and 4 output
  buses: GT's terminal
  builds a place with the first kind of part that is not full and counts the place for every kind that is not full, so the kinds are
  listed with rising maxima (with 8 output buses it built 8 fluid hatches and no bus at all, and such a tower cannot run).
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
| Stress-Proof Casing | GT's | GT's own LuV casing | LuV |
| Internal Structure | 16 | a naquadah alloy frame, 4 osmiridium plates, 4 tungsten steel plates (circuit 2) | LuV |
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
8 stress-proof casings, 8 internal structures, a cable, 4 double naquadah alloy plates, 4608 mB soldering alloy. The drones: assembler, 600 ticks, soldering alloy and
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
* Both are drawn at full brightness (as GTNH's), from 288 blocks away, level, and whether or not the controller is on the screen
  (`shouldRenderOffScreen`: a block entity is otherwise only drawn with its own chunk section, and looking up at the climber the
  controller is out of view).

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

`SpaceElevatorConsoleWidget` (in `com.af9.core.machine.console`, with the other consoles), in the Orbital Lithography Station's layout:
GT's machine screen (title bar, the parts' tabs, the player inventory) round a console page, and a panel on each side of the inventory
(`SidePanelsUIWidget`). There are two switches and a slot on it, and one thing is picked: **the mission**.

* **Left, the ascent**: the four Mining Drones as tiles (the one that flies is lit; a tile's tooltip says what its expedition takes
  and brings), the tower on the ground, the cable up to orbit with its running light and the **climber where its ride has it**, the
  asteroid of the run with its name, and the drones flying out to it and back with the ore; under it the state and the run-time bar.
* **Right, the run**: the drone that flies; the **mission selector**, two rows of arrows: the target (ASTEROIDS, or PLANET TYPE 2 to 9)
  and under it the planet's fluid with the buckets a mission brings (on an ore mission that row shows **the run's ore in stacks**); the
  **drone slot**, the **ONLINE** switch and the **size switch** (35x35 / 47x47: section 2), the expeditions that fly of those the modules
  could, the counters (expeditions flown, ore or fluid brought home; RESET) and the **hint**. On a liquid mission the scene on the left
  has the planet for the asteroid, in its fluid's colour.
* **Beside the inventory**: left the process (the motors' tier and the slots it powers, modules powered of those in the slots, flights,
  hydrogen and coolant in the hatches over what an expedition takes, the sky above the cable), right the system (status, the energy
  the hatches supply over what an expedition takes, tier, size, switch).

### The drone slot

`SpaceElevatorMachine.droneSlot`: a slot of the controller that GT reads as it reads an input bus (the Orbital Station's reticle slot
is the same thing), so the elevator needs **no input bus**. The drone stays in it: it is not used up. With the slot empty, a drone in
an input bus flies (the best one); only the chosen drone's expedition runs (`chosenDrone`), so two drones never make GT pick. A broken
controller drops its drone.

### What it lacks

While nothing flies, the status and the hint name the **first thing the elevator lacks** (`SpaceElevatorMachine.getStatus`), in the
order a run needs things, with the numbers of the drone that would fly:

| Status | When | The hint |
|---|---|---|
| OFFLINE | the structure is not formed | finish it; the size switch picks which |
| PAUSED | switched off (GT also switches a multiblock off that ran out of energy five times) | press ONLINE |
| NO SKY | something stands over the cable | it needs open sky |
| NO MODULE | no Mining Module is powered | put one in a slot; or: *Mining Modules MK-III need Motors MK-III: these are MK-I* |
| NO DRONE | no drone in the slot or in a bus; on a liquid mission also: the planet picked lies beyond the drone | put one in the slot; *Planet type 5 lies beyond this drone: it takes a Mining Drone MK-II or better* |
| NO POWER | the hatches supply less than one expedition takes | *An expedition takes 491,520 EU/t (4A ZPM). The hatches supply 64.* |
| NO HYDROGEN | less hydrogen in the hatches than one expedition takes | how many buckets |
| NO COOLANT | less of the drone's coolant than one expedition takes | how many buckets of which: *fill a Coolant Hatch* |
| NO ROOM | the asteroid drawn does not fit the output buses, or there is none; a liquid mission: the fluid does not fit the fluid output hatches, or there is none | add or empty output buses, or fluid output hatches |
| IDLE | nothing is missing | the next expedition starts by itself |
| RUNNING | a run is on | |

The status codes are the consoles' shared ones (`ConsoleWidget.STATUS_*`, lang `af9.console.status.<code>`; 16 to 20 are the
elevator's). The run's asteroid goes with the run (`ASTEROID_TAG` in the recipe's data), so a reloaded world still names it.

## 6. The recipe page

`SpaceMiningRecipeUI` (installed in common setup, `SpaceElevatorMachine.registerRecipeInfo`), in the style of the other AF9 pages: the
drone top left (kept), under it the hydrogen and, marked as coolant (its hover text names the Coolant Hatch), the supercooled
fluid, both piped into the foot of a little tower with its cable, its climber and a drone on its way to an asteroid
(`SpaceMiningFlowWidget`); then GT's arrow and, on the right, what the recipe itself cannot say, because a run gets its asteroid only
when it starts: **the ores this drone's asteroids hold**, taking turns in nine slots, with the stacks an expedition brings under them.

* The ores come from the same catalogue the machine draws its asteroids from (`OreCatalog.reach`: the ores of the veins of the drone's
  tier and below, for the Mk-IV the exotic ones as well), read on the client from the veins GT syncs to it (`ClientOreVeins`).
* The slots are **outputs to the recipe viewers** (as GT's own ore vein pages do it), so looking up a raw ore in EMI finds the
  expedition that brings it. They are not outputs of the recipe: the recipe has none.
* GT's lines under the page are its own: the time, the total energy, and the usage as amps of ZPM (3.75A for a 4A recipe: GT divides
  the EU/t by the tier's full voltage, and a recipe takes 15/16 of it an amp).
* The **liquid missions** have the same page in their own category (Space Pumping), with a planet for the asteroid and, in the nine
  slots, **the fluids of the planets the drone reaches**, each with the buckets a mission brings and, on its hover text, its planet
  type and the drone it takes. They are outputs to the recipe viewers too: looking up helium-3 finds the missions that bring it.

## 7. Not done

GTNH's modules are machines of their own (mining, pumping, assembler; each with its own buses, drone, parameters and an asteroid filter); here
the Mining Module is such a machine too (section 0: own screen, drone, hatches and energy buffer), but there are only Mining Modules, and no asteroid filter or parameters.
GTNH's Space Pumping Module pumps all the time beside the mining, for energy alone, up to four fluids at once, set by parameters; here its
fluids are liquid missions of the module itself, one kind of mission at a time, picked on its screen. There is no assembler module.
GTNH's elevator also has a galaxy map for travel, and plasma, drill tips, rods and computation as inputs; here it runs on hydrogen and a coolant
and has no travel function. GTNH's screen (a TecTech controller's, with its parameters) is not rebuilt: the elevator has AF9's console
(section 5). GTNH's climber model and textures are GTNH's own and are not used: the climber, the cable and the block textures are made by
hand after pictures (section 4).

## Ore expeditions per asteroid (the module rework)

* **One recipe a programmed circuit.** A module takes the circuit set with the button on the left of its screen: its number
  is the asteroid, the n-th of the veins the drone's tier first reaches (alphabetical; the exotic one last, MK-4). Recipes:
  `af9:space_mining_mk<tier>_<n>` (`server_scripts/mods/gtceu/space_elevator.js`, `ASTEROIDS` counts them: 24 / 12 / 4 / 8 now).
  A run brings a random number of stacks of the vein's ores (the tier's 8-16 / 12-24 / 16-32 / 24-48, half of them the main ore;
  lean ores — quantanium, pitchblende, uraninite — a quarter of their share, so a targeted quantanium run brings 6-12 stacks).
  The recipe pages list the ores of that asteroid with their ranges.
* **Used up:** a drill head and a crate (MK-1 tungsten carbide / stainless steel, MK-2 HSS-E / titanium, MK-3 naquadah alloy /
  tungsten steel, MK-4 neutronium / tungsten steel). The drone is not used up.
* **Modules:** a module flies the drones of its own tier and below (the MK-3 module also the MK-4 drone). Each flies its own
  recipe, as many at once as the tower's motors power.
* **Outputs:** the ore (and the fluid of a liquid mission) goes to the **tower's** output buses and hatches, not the module's;
  a module keeps its own only where the tower has none.
* **Computation:** the tower needs data hatches: 20 / 60 / 120 CWU/t for each powered MK-1 / MK-2 / MK-3 module. Its screen
  says how much it has and how much hydrogen and coolant the modules in the slots need a second.
