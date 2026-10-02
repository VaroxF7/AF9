# Computation

Where AF9's computation (CWU/t) comes from, besides GT's own HPCA: a single-block **CWU Server** (LV to IV), computers built
from **racks of cards** (the N1 computation arrays). All of them are GT computation sources
(`IOpticalComputationProvider`): GT's Optical Fiber Cable leads them to a reception hatch (a Research Station, the Orbital
Station, a computation hatch on a Photolithography Line or Scanner, the Metrology Station). Code: af9-core
`com.af9.core.compute`, `com.af9.core.machine.CWUServerMachine`; KubeJS `startup_scripts/gtceu/cwu_server.js` and `computation.js`, `server_scripts/mods/gtceu/cwu_server.js` and
`computation.js`. The glass fibre in their recipes is fine borosilicate glass wire.

*The machine bus (Optical Bus Cable, Bus Connector, Bus Controller, Interconnect Hatch, the Machine Bus Module for the Central
Monitor, ME autocrafting through the controller), the ME Computation Link (ME networks needing computation for their channels)
and the Crafting CPU Array (an AE2 crafting CPU as a multiblock) were taken out of the pack. Their documents are in the history
(docs/machine-bus.md, and the ME link's section 3 of this file).*

## 1. CWU Server

`gtceu:<tier>_cwu_server`, LV to IV (`CWUServerMachine`, a GT `TieredEnergyMachine`; definition in
`startup_scripts/gtceu/cwu_server.js`): a single block that turns EU into computation. It gives what is asked of it
each tick, up to **LV 4, MV 8, HV 16, EV 32, IV 64 CWU/t** (`cwutFor`: 4 doubling each tier), and pays from its buffer
(64 A of its voltage) `VA[tier] / max` EU per CWU: one amp of its tier at full output (MV 120 EU/t for 8 CWU/t), less
when less is drawn; without the energy it gives what the energy covers. Power goes in on any side but the front (one
amp). It is a GT computation source (`IOpticalComputationProvider`, every side), so it feeds everything:

- GT's Optical Fiber Cable to a reception hatch (a Research Station, the Orbital Station);

Bridging (for a Network Switch) always allowed. A soft mallet or its screen switches it off; its screen shows what it
gave last tick, the EU per CWU and its energy, and has a switch.

**Front lights** (model properties `cwu_lights`, `cwu_alt_lights`, set every 10 ticks; models
`af9:block/machine/cwu_server_<state>`): a steady red dot while **offline**: switched off, out of energy, or nothing next
to it that could draw from it (GT Optical Fiber Cable joined to it on any side); the power LEDs steady
green while **idle**; blinking while **busy** (it gave computation within the last second). Two blinking patterns of different lengths (8
frames at 2 ticks, 11 at 3), picked by the block position (`Mth.getSeed`), so servers side by side do not blink in step.

| Output | Machine | Inputs (plus the tier's machine hull) |
|---|---|---|
| LV CWU Server | Assembler, LV, 10 s | 4 LV circuits, 2 tin cable, 144 mB tin |
| MV CWU Server | Assembler, MV, 10 s | 2 MV circuits, 2 APU chips, 4 RAM chips, 2 fine borosilicate glass wire, 144 mB soldering alloy |
| HV CWU Server | Assembler, HV, 10 s | 2 HV circuits, 4 APU, 8 RAM, 2 ASIC chips, 2 fine borosilicate glass wire, 288 mB soldering alloy |
| EV CWU Server | Assembler, EV, 10 s | 2 EV circuits, 8 APU, 16 RAM, 4 ASIC chips, 4 fine borosilicate glass wire, 432 mB soldering alloy |
| IV CWU Server | Assembler, IV, 10 s | 2 IV circuits, 8 APU, 4 eDRAM, 4 MRAM, 8 ASIC chips, 4 fine borosilicate glass wire, 576 mB soldering alloy |

## 2. Computation arrays

Code: af9-core `com.af9.core.compute` (`ComputationArrayMachine`, `ComputerRackPartMachine`, the cards `ComputeCard`,
the coolants `ComputeCoolant`); definitions `kubejs/startup_scripts/gtceu/computation.js`, recipes
`kubejs/server_scripts/mods/gtceu/computation.js`. Computers built from racks of cards: computation that grows with
the factory, as the CWU Server's (§1) stops at IV.

**Structures.** No recipes: switched on and formed, an array draws its energy every tick (the cards', the racks', its
own) and its coolant every second, and puts out its cards' computation scaled down by the share of the heat the
coolant took. Out of energy it puts out nothing; nothing burns, nothing breaks.

| Array | Id | Structure | Own draw, heat |
|---|---|---|---|
| N1 Computation Array | `gtceu:n1_computation_array` | MV, 3 x 3 x 6 of Server Casing (`kubejs:server_casing`); eight MV Computer Racks in the middle row of the four inner slices, a steel pipe casing between them | 32 EU/t, 2 heat/t |
| N1 Supercomputer Array | `gtceu:n1_supercomputer_array` | LuV, 2 wide, 4 high, 7 to 30 long; every slice between the end slices holds four racks (MV or LuV; the two middle rows) between GT computer heat vents (the bottom and top rows): 20 to 112 racks; the end slices are GT computer casing, the controller second from the bottom | 512 EU/t, 8 heat/t |

Parts on the casings (the supercomputer's: its end slices), maxima only: energy hatches (2 / 4; the supercomputer one laser hatch), Coolant Hatches (2 / 4),
one Computation Transmitter Hatch.

The Server Casing has connected textures (LDLib, like the Plascrete Filter Casing: `server_casing.png.mcmeta` points to
`server_casing_ctm.png`): a wall of casings is one perforated panel, the frame and the rivets only around its outline.

**Computer Rack** (`gtceu:mv_computer_rack`, `gtceu:luv_computer_rack`): four card slots. The MV rack takes Tube and
Silicon cards (its own fans 4 EU/t, 1 heat/t), the LuV rack every card (128 EU/t, 2 heat/t). The MV rack is a machine
hull with a rack front; the LuV rack is GT's computer casing with a panel (GT's HPCA component face) on all four
sides and no front (`af9:block/machine/part/computer_rack_luv`), so a supercomputer's walls show a panel wherever a
rack is, however it was placed.

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
| Photonic (UHV) | 32, 10, 131072 | 64, 22, 262144 | 5, 65536 |
| Atomic (UHV) | 64, 14, 262144 | 128, 30, 524288 | 7, 131072 |
| Sub-atomic (UHV) | 128, 20, 524288 | 256, 44, 1048576 | 10, 262144 |

The last three tiers are made from the chips of the new families (`docs/semiconductor-factory.md` §18.7): Photonic from
photonic ICs and spin logic (memory), Atomic from TMD logic and memristors (memory), Sub-atomic from quantum-dot ICs.
They fit the LuV rack only; all are made at UV.

**Coolant** (through Coolant Hatches, heat taken per mB): distilled water 8 (only while the hatch is part of an array:
the MV Coolant Hatch comes before the Supercooling Cryostat), supercooled hydrogen 64, argon 96, xenon 160, endion 256.

**Where the computation goes.** An array is a GT computation source (`IOpticalComputationProvider`, as GT's HPCA): each
tick it gives what is asked of it up to its output. A Computation Transmitter Hatch in it feeds GT's Optical Fiber
Cable (a Research Station, the Orbital Station). Bridging (for a Network Switch) always
allowed. Its screen shows racks and cards, the computation (put out / fully cooled), what it gave last tick, the draw,
the heat and the cooling.

| Output | Machine | Inputs |
|---|---|---|
| 2 Server Casing | Assembler, 16 EU/t, 2.5 s (or crafted) | 6 aluminium plates, a steel frame |
| MV Coolant Hatch | Assembler, MV, 20 s | MV input hatch, 2 MV pumps, a frostproof casing, 4 polyethylene plates, 1000 mB distilled water |
| MV Computer Rack | Assembler, MV, 10 s | MV hull, 2 MV circuits, 2 MV motors, 4 fine borosilicate glass wire, 4 aluminium plates, 144 mB soldering alloy |
| LuV Computer Rack | Assembler, LuV, 20 s, cleanroom | LuV hull, 2 LuV circuits, 2 LuV motors, 4 fine borosilicate glass wire, 4 rhodium-plated palladium plates, 576 mB soldering alloy |
| N1 Computation Array | Assembler, MV, 30 s | MV hull, 4 MV circuits, 4 Server Casing, 4 MV pumps, 2 MV motors, 8 fine borosilicate glass wire, 288 mB soldering alloy |
| N1 Supercomputer Array | Assembler, LuV, 60 s, cleanroom | LuV hull, 4 LuV circuits, 8 computer casings, 4 heat vents, 4 LuV pumps, 2 LuV field generators, 16 fine borosilicate glass wire, 1152 mB soldering alloy |
| Cards | Circuit Assembler, the tier's voltage, 20 s (cleanroom from Nano) | the tier's board, its chips (Tube: vacuum tubes, magnetic iron rods for RAM), fine wire, 144 mB soldering alloy |
