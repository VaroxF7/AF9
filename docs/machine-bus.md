# The machine bus

A data center's backplane for the factory: machines share their state over a cable, GT's Central Monitor shows it and
sends commands back; the cable carries computation (CWU/t) and research from HPCAs and Data Banks to every machine on
it; a Bus Controller sets each machine's recipe and keeps it supplied, and Bus Controllers link into one network. AE2
networks with an ME Controller need its computation too (§7). Code: af9-core `com.af9.core.bus`; the Bus
Connector's and the Bus Controller's definitions `kubejs/startup_scripts/gtceu/machine_bus.js`; recipes
`kubejs/server_scripts/mods/gtceu/machine_bus.js`; quests in the Photolithography chapter (under the MCU chip).

## 1. Parts

| Part | Id | What |
|---|---|---|
| Optical Bus Cable | `af9:optical_bus_cable` | The bus: a thin pipe-shaped block (`OpticalBusCableBlock`, no block entity), glass fibre in an aqua jacket. It joins the cables next to it, a Bus Connector whose front face points at it, GT's transmitter hatches facing it and a CWU Server on any side but its front. It branches (GT's Optical Fiber Cable takes two connections per block and is IV). The old `af9:polycat_cable` (block and item) is remapped to it on load (`AF9Bus.remapOldCable`). |
| Bus Connector | `gtceu:mv_bus_connector` | A multiblock part (`BusConnectorPartMachine`, abilities `af9_bus_connector`, GT's `optical_data_reception` and `computation_data_reception`). A machine's port, a Central Monitor's port in its wall or a Bus Controller's port. The cable plugs into its front face. |
| Machine Bus Module | `af9:machine_bus_module` | A GT monitor module (`MachineBusModule`, a GT `ComponentItem`): one goes into a monitor group of the Central Monitor. |
| Bus Controller | `gtceu:bus_controller` | An MV multiblock (`BusControllerMachine`): the bus's PLC, up to four buses (§5). |
| Interconnect Hatch | `gtceu:mv_interconnect_hatch` | A Bus Controller's part (`BusInterconnectPartMachine`, ability `af9_bus_interconnect`): its link to other controllers over Optical Bus Cable (§6). |

**The bus** (`BusNetwork.walk`): from a connector's front face through the cable, breadth first, up to 4096 cable blocks,
in loaded chunks only (never loads a chunk: an unloaded stretch cuts the bus there). Every connector whose port touches
a cable of the run is on the bus; two connectors whose ports touch are on one bus without cable. GT's transmitter
hatches whose front faces a cable of the run (or the port) are the bus's sources (§2b). A connector keeps its walk for
20 ticks. The bus's identity (`Bus.id`) is its lowest connector position, the same whichever connector walks it.

**Limits** (`BusNetwork`): a bus serves at most **16 machines** (`MAX_MACHINES`: connectors that are a machine's port;
Central Monitors' and Bus Controllers' ports do not count) and carries at most **1024 CWU/t** (`MAX_CWUT`), research
without limit. With more than 16 machines it is **overloaded**: it carries no computation, research or supplies (its
sources are cut off from the network too) until it has 16 or fewer; the connectors' screens, the Bus Controller and the
monitor module (its settings page, the table header, the detail view's status line) say so. The monitors still show
the machines.

**Where connectors go.** At most one per machine, wherever the machine takes hatches: Photolithography Line and Scanner
(plascrete), the Orbital Lithography Station (the top deck's PTFE casings, with its other hatches), the four SMC fab
multiblocks (any casing), the Particle Accelerator (the ring's clean casings), the Supercooling Cryostat (frostproof
casings), the Bus Controller (its shell). The patterns take
`Predicates.abilities(BusConnectorPartMachine.BUS_CONNECTOR).setMaxGlobalLimited(1, 0)`. Because the connector also
carries GT's `OPTICAL_DATA_RECEPTION` and `COMPUTATION_DATA_RECEPTION` abilities, GT's multiblocks take it where their
reception hatch goes: the Assembly Line (its data hatch position), the Research Station (its computation hatch), the
Data Bank and the Network Switch. GT counts a part against every limit it matches, so the orbital station allows two
computation and two optical data reception parts (the connector and a hatch of its own).

**The Central Monitor's wall** takes a connector too. GT builds the wall's block predicate once into the private static
`CentralMonitorMachine.MULTI_PREDICATE`; at common setup `AF9Bus.installMonitorWall` puts back that predicate `.or()`
the connector's ability (reflection; if GT renames the field, an error is logged and the wall does not take one). It is
not given GT's `DATA_ACCESS` ability on purpose: the orbital station allows exactly one data access part (its 1 nm
research), a connector counted as one would block the data hatch; the wall's data access limit would count it too.

## 2. What a machine shares, what it takes

The connector's screen: a **name** (max 32 characters; the monitors list the machine by it, or by the machine's own
name), what it **shares** and which **commands** it takes, all on until clicked off. Bits (`BusData`):

| Data | Snapshot keys | What |
|---|---|---|
| status | `s` | idle / working / waiting / suspended (GT's recipe logic), not formed |
| progress | `pr`, `mx` | the run's progress and length, ticks |
| recipe | `rt`, `out`, `mode`, `mc` | the recipe type, the running recipe's first item output; lithography: the node and its colour |
| energy | `eu`, `ec`, `et` | stored / capacity of the energy hatches, the running recipe's EU/t |
| production | `runs`, `pt`, `bk` | finished runs (counted by the connector, `IMultiPart.afterWorking`); lithography: printed and broken wafers |
| settings | `b`, `v`, `vm`, `sp` | batch; the line's version, its maximum and speed factor |
| process | `cl`, `vs`, `bc`, `gev` | lithography: vacuum cleanliness and state, break chance; the accelerator's beam energy |

Always in the snapshot (the buttons need them): the connector's position `p`, name `n`, machine name key `k`, masks
`pub` / `acc`, formed `f`, enabled `on`, number of recipe types `mn`.

| Command | What |
|---|---|
| power | start / stop (GT's work toggle) |
| batch | batch mode on / off |
| mode | the next / previous recipe type. A Photolithography Line or Scanner only switches to the nodes its built version prints; the orbital station to any of its nodes (it prints only in orbit anyway). As GT's mode button: `setActiveRecipeType`, then `updateTickSubscription`. |

A command only reaches a machine on the port's bus, through a connector that takes it.

## 2b. Computation and research

**Sources** (`BusNetwork.Bus`: `computation`, `data`):

| Source | Gives |
|---|---|
| Computation Transmitter Hatch (an HPCA, a Network Switch) facing the cable | its CWU/t (the hatch's `NotifiableComputationContainer`) |
| Optical Data Transmitter Hatch (a Data Bank) facing the cable | its research, while the bank runs (GT's own check) |
| A Bus Connector in a Data Bank | that bank's research: its data access and optical reception parts, while it runs (`BusConnectorPartMachine.getDataSource`) |
| A Bus Connector in a computation array | the array's CWU/t (`BusConnectorPartMachine.getComputationSource`, §9) |
| A CWU Server, the cable on any side but its front | its CWU/t (§8) |

**The connector is its machine's optical reception hatch.** It carries a `BusComputationContainer` (GT's
`NotifiableComputationContainer`, IO in, drawing from the bus instead of one Optical Fiber Cable): the machine's CWU/t
recipes draw through it (`duration_is_total_cwu` recipes, the Research Station's, advance by the CWU drawn, as GT's).
A request takes from the sources on the connector's own bus first, then from those on the other buses of its network
(§6), in order until met. Every bus has a budget of 1024 CWU/t a tick (`BusLoad`, per level and bus id, reset each
tick): a request never takes more than its own bus has left, and computation from another bus counts on that bus's
budget too. The maximum it reports is the sum over the network's buses, each at most 1024, the whole at most 1024.
Bridging (for a Network Switch) needs every source to bridge. It is an `IOpticalDataAccessHatch` (receiver): a research recipe passes if a source on the bus or another
bus of its network holds it (no overloaded bus), or one of the machine's own data hatches does (the connector never blocks what they hold; a data hatch of the
machine's own still blocks what only the bus holds, as GT checks every data part). All calls carry GT's `seen` set, so
a Network Switch or a Data Bank on the bus that also takes from it does not loop.

The connector's screen shows its machines against the 16, the CWU/t it can draw, the computation hatches and research
sources of its network and, on a network, how many buses and Bus Controllers it has.

## 3. The module and its screen

**Data.** GT's Central Monitor ticks its modules every `160 / 2^(tier - 1)` ticks (8 s at LV, 4 s at MV, 1 s at EV, 5
ticks at LuV, every tick at UV) while it has power. Each tick the module snapshots every machine on the bus into its own
NBT (`dev`, a list; `t` the game time; `port`: 0 no connector in the wall, 1 no machines, 2 fine); GT sends the module's
NBT to the players. The group reads the bus through its **target** if that is a connector (GT's set-target button),
else through the wall's first connector.

**Screen** (`MachineBusRenderer`, layout `BusScreenLayout`, the same on both sides). The group's bounding box in blocks,
drawn in canvas units (the font is 9 units high), as many units per block as the view needs to fit (detail 200 x 124,
table 200 x rows). Two views:

- **Detail**, one machine: a status light, the name, the node / recipe chip; the state, the version (`V2/3 x0.80`) and
  batch; the progress bar with percent and time left; energy and draw, the product, runs (or printed / broken), vacuum
  and break chance (or beam energy); the touch buttons.
- **Table**, the whole bus: a row per machine (light, name, chip, a small progress bar, runs or printed wafers).

The bar runs on from the snapshot while the machine works (`progress + ticks since the snapshot`, wrapping into the
next run), so it moves every frame whatever the monitor's tier.

**Touch.** GT's Advanced Monitor keeps where it was last right-clicked (fractions of the block along the monitor's right
axis and the world's up). The wall's connector looks every 2 ticks (`BusConnectorPartMachine.touchTick` ->
`MachineBusModule.handleTouches`), maps the tap onto the canvas (`BusScreenLayout.tapPosition`), runs the button under
it and clears the tap; the monitor answers at once, whatever its tier. Only while the monitor is on. Buttons (detail
view, if "touch" is on): START / STOP, BATCH, the mode (`<`, the node, `>`), LIST (to the table, with more than one
machine). A tap on a table row opens that machine.

**Settings** (the module's page in the Central Monitor's screen, clickable lines run on the server): the machine
(`<` name `>`), the view, touch buttons on / off, the fields to show (the same bits as the connector's; a field shows
only if the machine shares it too), and the same commands.

## 4. Recipes (MV)

| Output | Machine | Inputs |
|---|---|---|
| 8 Optical Bus Cable | Assembler, LV, 5 s | 8 fine borosilicate glass wire (MV extruder), 2 polyethylene foil |
| Bus Connector | Assembler, MV, 10 s | MV hull, 2 MCU chips, 4 Optical Bus Cable, an MV circuit, 144 mB soldering alloy |
| Machine Bus Module | Assembler, MV, 20 s | plastic circuit board, an MCU chip, 2 Optical Bus Cable, 4 fine red alloy wire, an MV circuit, 144 mB soldering alloy |
| Bus Controller | Assembler, MV, 20 s | MV hull, 4 MCU chips, 2 MV circuits, an MV robot arm, 8 Optical Bus Cable, 288 mB soldering alloy |

The MCU is AF9's silicon chip (350 nm, `docs/semiconductor-factory.md` §5.3b): the bus comes with the Photolithography
Line, like GT's Central Monitor at MV. The computation and research it carries come later, with the HPCA and the Data
Bank.

## 5. The Bus Controller

**Structure.** 3 x 3 x 3 of solid steel casing (`gtceu:solid_machine_casing`), the centre free, the controller in the
middle of the front face. Parts anywhere on the shell, maxima only: 8 item input buses, 4 fluid input hatches (plain or
ME: GT's ME input / stocking buses and hatches count, so AE2 sends it the ingredients), 2 energy hatches, 4 Bus
Connectors (its ports: one on each bus it runs, so up to 4 buses and 64 machines), 1 Interconnect Hatch (§6). Recipe type `dummy`, its own logic (`BusControllerMachine.SupplyLogic`): while formed and switched on it
draws 120 EU/t; without the power it waits.

**The recipe of a machine.** Its screen shows its ports (of 4), the network's buses, controllers and machines, the
CWU/t and research sources, and how many buses are overloaded; it lists every machine of the network that runs
recipes (`◀ name ▶`). The ghost slot in
the top right corner takes the product (a bucket or cell of a fluid product matches fluid outputs); the screen offers
the selected machine's recipes that make it (at most 64, over the recipe types the machine may run: a line's only up
to its built version), `◀ n/N ▶`, with what one run takes, and `[Set this recipe]`. The recipe is kept on the machine's
connector (`recipe`, persisted), `[Clear]` removes it. The connector's screen can refuse the controller
("A Bus Controller may set this machine's recipe", on until clicked off).

**Supply** (`BusSupply`, once a second while running). Each bus is supplied by one controller: of the controllers with a
port on it, the one at the lowest position. For each machine with a set recipe on the buses it supplies (none on an
overloaded bus), taking inputs from its own buses and hatches first, then from the linked controllers':

1. The machine must be formed and run the recipe's type: it is switched to it (`BusData.selectMode`, as the monitor's
   mode button; a line only to a node its built version prints).
2. If the machine's own plain input buses and hatches already hold one run (every input at its amount; a not-consumed
   one, a reticle or a lens, once) nothing moves; the programmed circuit is set.
3. Else the missing inputs are taken from the controllers' input buses and hatches (plain or ME; simulated first, all
   or nothing) and put into the machine: all items into the first plain input bus that takes them all, each fluid into
   a hatch that holds it or an empty one. An Assembly Line with ordered inputs gets its i-th item in its i-th bus (the
   bus empty or holding that item), as GT checks it. The recipe's programmed circuit goes into the receiving buses'
   circuit slot. ME buses of the machine are never filled.
4. The products stay in the machine's output buses and hatches.

The last supply is kept on the machine's connector (not saved): stocked, supplied, waiting for (what the controllers
lack), no room, not formed, the mode is locked, refused, unknown recipe, the bus is overloaded. The controller's
screen and the connector's show it.

## 6. The network: Interconnect Hatch

A Bus Controller takes one Interconnect Hatch (`gtceu:mv_interconnect_hatch`). Optical Bus Cable plugs into its front
face; every Interconnect Hatch on one run of cable links its controller to the others' (`BusNetwork.walkInterconnects`,
kept 20 ticks), so one branching run joins any number of controllers. An Interconnect Hatch only talks to Interconnect
Hatches: Bus Connectors on the same cable do not see it, nor it them (keep the link cable apart from the buses for
clarity).

**The network** (`BusNetwork.network`, `Net`): from some buses and controllers, every controller with a port on one of
its buses, every bus one of its controllers has a port on, every controller linked to one of its controllers. A
connector's network starts from its own bus (without a controller on it, the bus alone); a controller's from its
ports' buses and itself (kept 20 ticks). Over it:

- computation and research flow between all its buses (§2b; each bus at most 1024 CWU/t, overloaded buses left out);
- every controller lists and sets recipes for every machine of it (§5);
- a machine is supplied by the controller with a port on its bus, from that controller's inputs first, then the other
  controllers' in the order found: one AE2-fed controller can feed the network.

| Output | Machine | Inputs |
|---|---|---|
| Interconnect Hatch | Assembler, MV, 10 s | MV hull, 2 MCU chips, an MV emitter, an MV sensor, 4 Optical Bus Cable, 144 mB soldering alloy |

## 7. ME networks need computation (AE2)

Code: af9-core `com.af9.core.ae2` (only set up when AE2 is loaded), the Mixin
`com.af9.core.mixin.ae2.PathingCalculationMixin` (`af9.mixins.json`, not required); settings `af9-common.toml`,
`[meComputation]`: `enabled` (true), `channelsPerCwut` (4). Built against AE2 15.4.9 (compile only, modmaven).

**The rule.** An AE2 network with an ME Controller (controller state online) needs computation for its channels:
1 CWU/t per `channelsPerCwut` channels, every tick. The channels it wants are its devices that need a channel (a
multiblock such as a crafting CPU counts once, as AE2 gives it one channel), counted each second. Networks without a
controller (ad-hoc, 8 channels) need none.

**Short of it** (`MEComputationService`, an AE2 grid service, one per network): the network draws its CWU/t every
tick through its ME Computation Links, in turn. Averaged over a second: if it got all it asked for, its channels are
not limited; else it may use `supplied × channelsPerCwut` channels. The cap is applied inside AE2's channel assignment
(`PathingCalculation.tryUseChannel`, a BFS out from the controllers, dense cables first): once the cap is reached no
more channels are granted, so the devices farthest from the controller go without. Channels are only reassigned
(`IPathingService.repath`, the network reboots briefly) when the cap really changes: at once when the network gets all
it needs again or first falls short, else at most every 5 s and only for a change of at least a tenth (or of
`channelsPerCwut`). A new network starts with all its channels for its first second.

**ME Computation Link** (`af9:me_computation_link`, `MEComputationLinkBlock` / `MEComputationLinkBlockEntity`): an
AE2 in-world grid node on five sides (it needs no channel itself, 1 AE/t); its back (`facing`, placed against the
block clicked) takes the computation:

| On its back | It draws |
|---|---|
| Optical Bus Cable | from the bus (`BusNetwork.requestCWUt`): the link is a `BusConsumer`, one of the bus's 16 machines, drawing from its 1024 CWU/t and its network |
| a GT Computation Transmitter Hatch (HPCA, Network Switch) | from the hatch's computation |
| GT Optical Fiber Cable | from what the fibre leads to (the link shows GT's fibre a receiving port, so the fibre connects) |

Any number per network. Right-click: what it draws from, how much, and the network's channels, needs, supply and cap.
The ME Controller's tooltip names the rule. Quests: the link under the Optical Bus Cable (Photolithography chapter),
and a paragraph on ATM9's ME Controller quest.

| Output | Machine | Inputs |
|---|---|---|
| ME Computation Link | Assembler, MV, 10 s | 2 calculation processors, fluix glass cable, quartz fiber, an MCU chip, 4 Optical Bus Cable, 144 mB soldering alloy |

## 8. CWU Server

`gtceu:<tier>_cwu_server`, LV to IV (`CWUServerMachine`, a GT `TieredEnergyMachine`; definition in
`startup_scripts/gtceu/machine_bus.js`): a single block that turns EU into computation. It gives what is asked of it
each tick, up to **LV 4, MV 8, HV 16, EV 32, IV 64 CWU/t** (`cwutFor`: 4 doubling each tier), and pays from its buffer
(64 A of its voltage) `VA[tier] / max` EU per CWU: one amp of its tier at full output (MV 120 EU/t for 8 CWU/t), less
when less is drawn; without the energy it gives what the energy covers. Power goes in on any side but the front (one
amp). It is a GT computation source (`IOpticalComputationProvider`, every side), so it feeds everything:

- an ME Computation Link against it (§7): one LV server runs a 16-channel ME network at the default rate;
- GT's Optical Fiber Cable to a reception hatch (a Research Station, the Orbital Station);
- the machine bus, Optical Bus Cable on any side but its front (the front is its lights; `BusNetwork.facesBus`): a
  source like a transmitter hatch (`BusNetwork.isTransmitter`), each bus still at most 1024 CWU/t.

Bridging (for a Network Switch) always allowed. A soft mallet or its screen switches it off; its screen shows what it
gave last tick, the EU per CWU and its energy.

**Front lights** (model properties `cwu_lights`, `cwu_alt_lights`, set every 10 ticks; models
`af9:block/machine/cwu_server_<state>`): a steady red dot while **offline**: switched off, out of energy, or nothing next
to it that could draw from it (Optical Bus Cable or a Bus Connector facing it on any side but its front, an ME
Computation Link with its back against it or GT Optical Fiber Cable joined to it on any side); the power LEDs steady
green while **idle**; blinking while **busy** (it gave computation within the last second). Two blinking patterns of different lengths (8
frames at 2 ticks, 11 at 3), picked by the block position (`Mth.getSeed`), so servers side by side do not blink in step.

| Output | Machine | Inputs (plus the tier's machine hull) |
|---|---|---|
| LV CWU Server | Assembler, LV, 10 s | 4 LV circuits, 2 tin cable, 144 mB tin |
| MV CWU Server | Assembler, MV, 10 s | 2 MV circuits, 2 APU chips, 4 RAM chips, 2 Optical Bus Cable, 144 mB soldering alloy |
| HV CWU Server | Assembler, HV, 10 s | 2 HV circuits, 4 APU, 8 RAM, 2 ASIC chips, 2 Optical Bus Cable, 288 mB soldering alloy |
| EV CWU Server | Assembler, EV, 10 s | 2 EV circuits, 8 APU, 16 RAM, 4 ASIC chips, 4 Optical Bus Cable, 432 mB soldering alloy |
| IV CWU Server | Assembler, IV, 10 s | 2 IV circuits, 8 APU, 4 eDRAM, 4 MRAM, 8 ASIC chips, 4 Optical Bus Cable, 576 mB soldering alloy |

## 9. Computation arrays

Code: af9-core `com.af9.core.compute` (`ComputationArrayMachine`, `ComputerRackPartMachine`, the cards `ComputeCard`,
the coolants `ComputeCoolant`); definitions `kubejs/startup_scripts/gtceu/computation.js`, recipes
`kubejs/server_scripts/mods/gtceu/computation.js`. Computers built from racks of cards: computation that grows with
the factory, as the CWU Server's (§8) stops at IV.

**Structures.** No recipes: switched on and formed, an array draws its energy every tick (the cards', the racks', its
own) and its coolant every second, and puts out its cards' computation scaled down by the share of the heat the
coolant took. Out of energy it puts out nothing; nothing burns, nothing breaks.

| Array | Id | Structure | Own draw, heat |
|---|---|---|---|
| N1 Computation Array | `gtceu:n1_computation_array` | MV, 3 x 3 x 6 of Server Casing (`kubejs:server_casing`); eight MV Computer Racks in the middle row of the four inner slices, a steel pipe casing between them | 32 EU/t, 2 heat/t |
| N1 Supercomputer Array | `gtceu:n1_supercomputer_array` | LuV, 2 wide, 4 high, 7 to 30 long of GT computer casing; every slice between the end slices holds two racks (MV or LuV) under two computer heat vents: 10 to 56 racks | 512 EU/t, 8 heat/t |

Parts on the casings, maxima only: energy hatches (2 / 4; the supercomputer one laser hatch), Coolant Hatches (2 / 4),
one Bus Connector, one Computation Transmitter Hatch.

**Computer Rack** (`gtceu:mv_computer_rack`, `gtceu:luv_computer_rack`): four card slots. The MV rack takes Tube and
Silicon cards (its own fans 4 EU/t, 1 heat/t), the LuV rack every card (128 EU/t, 2 heat/t).

**Cards** (`af9:<tier>_<kind>_card`): processors (CPU, GPU) compute; RAM feeds them. A processor needs a RAM card of
its tier or higher in the same rack, one RAM card a processor; without one it runs at a quarter (full energy and heat
still).

| Tier | CPU CWU/t, heat, EU/t | GPU | RAM heat, EU/t |
|---|---|---|---|
| Tube (MV) | 1, 1, 8 | 2, 3, 16 | 1, 4 |
| Silicon (HV) | 2, 2, 32 | 4, 5, 64 | 1, 16 |
| Nano (IV) | 4, 3, 512 | 8, 8, 1024 | 2, 256 |
| Quantum (LuV) | 8, 5, 2048 | 16, 12, 4096 | 3, 1024 |
| Tensor (UV) | 16, 8, 32768 | 32, 18, 65536 | 4, 16384 |

**Coolant** (through Coolant Hatches, heat taken per mB): distilled water 8 (only while the hatch is part of an array:
the MV Coolant Hatch comes before the Supercooling Cryostat), supercooled hydrogen 64, argon 96, xenon 160, endion 256.

**Where the computation goes.** An array is a GT computation source (`IOpticalComputationProvider`, as GT's HPCA): each
tick it gives what is asked of it up to its output. A Bus Connector in it puts it on the machine bus (§2b), one of the
bus's 16 machines, still at most 1024 CWU/t a bus; a Computation Transmitter Hatch in it feeds GT's Optical Fiber
Cable (a Research Station, the Orbital Station) or an ME Computation Link (§7). Bridging (for a Network Switch) always
allowed. Its screen shows racks and cards, the computation (put out / fully cooled), what it gave last tick, the draw,
the heat and the cooling; the monitor module's detail view the computation, cooling, racks and heat.

| Output | Machine | Inputs |
|---|---|---|
| 2 Server Casing | Assembler, 16 EU/t, 2.5 s (or crafted) | 6 aluminium plates, a steel frame |
| MV Coolant Hatch | Assembler, MV, 20 s | MV input hatch, 2 MV pumps, a frostproof casing, 4 polyethylene plates, 1000 mB distilled water |
| MV Computer Rack | Assembler, MV, 10 s | MV hull, 2 MV circuits, 2 MV motors, 4 Optical Bus Cable, 4 aluminium plates, 144 mB soldering alloy |
| LuV Computer Rack | Assembler, LuV, 20 s, cleanroom | LuV hull, 2 LuV circuits, 2 LuV motors, 4 Optical Bus Cable, 4 rhodium-plated palladium plates, 576 mB soldering alloy |
| N1 Computation Array | Assembler, MV, 30 s | MV hull, 4 MV circuits, 4 Server Casing, 4 MV pumps, 2 MV motors, 8 Optical Bus Cable, 288 mB soldering alloy |
| N1 Supercomputer Array | Assembler, LuV, 60 s, cleanroom | LuV hull, 4 LuV circuits, 8 computer casings, 4 heat vents, 4 LuV pumps, 2 LuV field generators, 16 Optical Bus Cable, 1152 mB soldering alloy |
| Cards | Circuit Assembler, the tier's voltage, 20 s (cleanroom from Nano) | the tier's board, its chips (Tube: vacuum tubes, magnetic iron rods for RAM), fine wire, 144 mB soldering alloy |
