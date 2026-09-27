# The machine bus

A data center's backplane for the factory: machines share their state over a cable, GT's Central Monitor shows it and
sends commands back. Code: af9-core `com.af9.core.bus`; the Bus Connector's definition
`kubejs/startup_scripts/gtceu/machine_bus.js`; recipes `kubejs/server_scripts/mods/gtceu/machine_bus.js`; quests in the
Photolithography chapter (under the MCU chip).

## 1. Parts

| Part | Id | What |
|---|---|---|
| Polycat Cable | `af9:polycat_cable` | The bus: a thin pipe-shaped block (`PolycatCableBlock`, no block entity). It joins the cables next to it and a Bus Connector whose front face points at it. |
| Bus Connector | `gtceu:mv_bus_connector` | A multiblock part (`BusConnectorPartMachine`, ability `af9_bus_connector`). A machine's port, or a Central Monitor's port in its wall. The cable plugs into its front face. |
| Machine Bus Module | `af9:machine_bus_module` | A GT monitor module (`MachineBusModule`, a GT `ComponentItem`): one goes into a monitor group of the Central Monitor. |

**The bus** (`BusNetwork.walk`): from a connector's front face through the cable, breadth first, up to 4096 cable blocks,
in loaded chunks only (never loads a chunk: an unloaded stretch cuts the bus there). Every connector whose port touches
a cable of the run is on the bus; two connectors whose ports touch are on one bus without cable. A connector keeps its
walk for 20 ticks.

**Where connectors go.** At most one per machine, wherever the machine takes hatches: Photolithography Line and Scanner
(plascrete), the Orbital Lithography Station (the top deck's PTFE casings, with its other hatches), the four SMC fab
multiblocks (any casing), the Particle Accelerator (the ring's clean casings), the Supercooling Cryostat (frostproof
casings). The patterns take `Predicates.abilities(BusConnectorPartMachine.BUS_CONNECTOR).setMaxGlobalLimited(1, 0)`.
GT's multiblocks do not take one.

**The Central Monitor's wall** takes a connector too. GT builds the wall's block predicate once into the private static
`CentralMonitorMachine.MULTI_PREDICATE`; at common setup `AF9Bus.installMonitorWall` puts back that predicate `.or()`
the connector's ability (reflection; if GT renames the field, an error is logged and the wall does not take one). It is
not given GT's `DATA_ACCESS` ability on purpose: the orbital station allows exactly one data access part (its 1 nm
research), a connector counted as one would block the data hatch.

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
| 8 Polycat Cable | Assembler, LV, 5 s | 8 fine annealed copper wire, 2 polyethylene foil |
| Bus Connector | Assembler, MV, 10 s | MV hull, 2 MCU chips, 4 Polycat Cable, an MV circuit, 144 mB soldering alloy |
| Machine Bus Module | Assembler, MV, 20 s | plastic circuit board, an MCU chip, 2 Polycat Cable, 4 fine red alloy wire, an MV circuit, 144 mB soldering alloy |

The MCU is AF9's silicon chip (350 nm, `docs/semiconductor-factory.md` §5.3b): the bus comes with the Photolithography
Line, like GT's Central Monitor at MV.
