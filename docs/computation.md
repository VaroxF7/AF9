# Computation

Where AF9's computation (CWU/t) comes from, besides GT's own HPCA: a single-block **CWU Server** (LV to IV).
It is a GT computation source (`IOpticalComputationProvider`): GT's Optical Fiber Cable leads it to a reception hatch
(a Research Station, the Orbital Station, a computation hatch on a Photolithography Line or Scanner). Code: af9-core
`com.af9.core.machine.CWUServerMachine`; KubeJS `startup_scripts/gtceu/cwu_server.js`,
`server_scripts/mods/gtceu/cwu_server.js`. The glass fibre in their recipes is fine borosilicate glass wire.

*The N1 computation / supercomputer arrays (racks of CPU/GPU/RAM cards) were taken out of the pack. Larger
computation now comes from GT's own HPCA.*

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
