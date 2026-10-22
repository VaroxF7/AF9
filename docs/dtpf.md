# The Dimensionally Transcendent Plasma Forge (DTPF)

After GTNH's: a vast hall that forges plasma into matter. The machine is `gtceu:dtpf` (af9-core `PlasmaForgeMachine`; the
structure and the machine definition in `kubejs/startup_scripts/gtceu/dtpf.js`; the recipes in
`kubejs/server_scripts/mods/gtceu/dtpf.js`). The structure is AF9's own design, not a copy of GTNH's.

## 1. The structure (21 x 22 x 45): a forge line

Rebuilt from scratch (the first hall was a 37 x 29 x 37 skeleton of pylons and struts). The archetype is a *line* that teaches
the process from the front: inputs on the **injection deck**, ionised and compressed in the **plasma bore**, forged in the
**press**. One function, `dtpfCell(x, y, z)` in `kubejs/startup_scripts/gtceu/dtpf.js`, is the whole design; the aisles follow
it. About 3,700 blocks, all connected, symmetric left to right (checked offline). From the front:

| Part | Where | Blocks |
|---|---|---|
| Foundation | a slab 21 x 45 at y 0, the axis lit down it | N, B |
| Injection deck | a low hall 17 wide and 6 deep at the front, roof at y 4; its front wall (y 1-3) is the hatch wall, the controller is in it two up | W (hatches), N |
| Feed conduits | along both sides from the deck to the press, 3 high; the top row is a dashed line of bridge casing | N, B |
| Plasma bore | an octagonal tube 31 long (radius 3) at y 10, on a pier from the deck's roof; glass windows in the gaps between the rings, a glass plasma bar down its axis, a 2-thick end plate at the front | N, G |
| Coil rings | five 11 x 11 square rings, two thick, 5 blocks apart, round the bore: containment coil, resonance coil at the corners; spokes to the bore, rails between the rings, legs under their feet | K, R, N |
| Press | at the back, 19 x 13 x 8 plated, a bridge band at y 6, the bore's mouth ringed with resonance coil; a hammer head and two exhaust stacks (capped with bridge casing) on its roof | N, B, R |

Blocks (all AF9 Core's, `registry/AF9Blocks`; textures in `tools/textures/dtpf_blocks.py`):

* **Dimensionally Transcendent Casing** `af9:dtpf_transcendent_casing` (N): GT5-Unofficial's `MACHINE_DIM_TRANS_CASING`,
  unchanged, with a connected-texture sheet so the plating runs on across blocks.
* **Dimensional Injection Casing** `af9:dtpf_injection_casing` (W): the original's `MACHINE_DIM_INJECTOR`. The controller's
  casing, and the only block that takes hatches.
* **Dimensional Bridge Casing** `af9:dtpf_bridge_casing` (B): the original's `MACHINE_DIM_BRIDGE`, an active block: dark while the
  forge idles, the four animated frames and an emissive layer while it works.
* **Plasma Containment Coil** `af9:dtpf_containment_coil` (K) and **Plasma Resonance Coil** `af9:dtpf_resonance_coil` (R): AF9's
  own, the Endion coils' weave turned to the DTPF's blue (containment) and ice cyan (resonance), with bloom while the forge works.
  Structure blocks only, not GT heating coils (nothing else reads them, so the Electric Blast Furnace is untouched).
* `gtceu:fusion_glass` (G) for the bore's windows and plasma bar.

Recipes of the blocks (`server_scripts/mods/gtceu/dtpf.js`): the casings in the UV assembler (transcendent from mk3 fusion casing
and tritanium / neutronium plates; injection and bridge from it); the coils at **UHV** in the Assembly Line (containment: a Resonant
Endion coil, tritanium, a field generator, UHV circuits, sanguinite wire, plasma solder and supercooled Endion; resonance: a containment
coil with neutronium, strange matter plasma and more). Nothing needs the forge's own products, so the first hall builds from scratch.
The controller takes 8 transcendent casings, the Mk2 refit 4 bridge casings and 2 resonance coils.

The controller is in the front wall, two up (the face is GTNH's DTPF overlay).

## 2. The machine

* **Hatches** only on the injection deck's front wall (the fix for "hatches everywhere" of the first hall): up to 4 energy, 2 laser
  target, 8 fluid input, 4 fluid output, 4 item input and 4 item output buses, 2 plasma hatches, 1 maintenance
  (`setMaxGlobalLimited`: maximums, never required counts).
* **Gate** (`FORGE_GATE`): it starts a recipe only when its hatches can supply the recipe's full EU/t (voltage x amps of all
  of them), as the lithography stations do — and only while ready (below).
* **Readiness**: the forge keeps a core temperature (console + Jade, 0-12,000 K). It burns **osmium plasma** out of its
  plasma hatches: 100 mB a second climbing, 50 mB holding (2,000 mB per 20 s, 500 mB per 10 s). Fed, it climbs 150 K a
  second to 9,000 K (ready) in about a minute and tops at 12,000 K; starved, it cools three times as fast, and a broken
  structure goes cold. No plasma, no forging — the gate holds every recipe until readiness.
* **Plasma Heater** (`gtceu:plasma_heater`, 5x4x5, EV): the sub multi that ionises 4 osmium dust with 2,000 mB Endion
  to 2,000 mB osmium plasma, the forge's only heater fuel. Its plasma hatches are filled from here.
* **Mk2** (`gtceu:dtpf_mk2`, same hall): the Mk1 controller turned with the heater's heart (assembly line refit).
  Half the osmium (50/25 mB a second) for **four times the input amperage** — the same recipes, hungrier power.
* **Running-time ramp** (`RAMP`, from GTNH's discount): while a recipe runs the forge warms up, 1 tick of warmth per tick of
  running time, to 30 minutes; a pause cools it three times as fast. Warmth scales the recipe's EU/t by up to **-50 %** and
  its run time by up to **-25 %**. A broken structure starts cold. The console and Jade show the ramp.
* Perfect overclocks.

## 3. The recipes (`plasma_forge`, 3 items and 3 fluids in, 3 and 3 out; UHV voltage, 100 A)

| Recipe | Takes | Gives |
|---|---|---|
| `af9:dtpf/strange_matter_plasma` | 8 strange matter dust, 2,000 mB Endion | 2,000 mB strange matter plasma |
| `af9:dtpf/chromodynium_plasma` | 4 chromodynium dust, 4,000 mB Endion | 2,000 mB chromodynium plasma |
| `af9:dtpf/carbon_plasma` | 8 carbon dust, 2,000 mB Endion | 2,000 mB carbon plasma |
| `af9:dtpf/idontknowium_plasma` | 2,000 mB star matter plasma, 2,000 mB anti-matter plasma (the Mega Fusion Reactor's both) | 1,000 mB Idontknowium Plasma (12,000 K) |
| `af9:dtpf/star_matter_plasma_steel_plasma` | 4 steel ingots, 1,000 mB star matter plasma | 1,000 mB Star Matter Plasma Steel plasma (superstate) |
| `af9:dtpf/nano_particles` | 1,000 mB carbon plasma, magnetic trap kept | 4 nano particles (optical assembly) |
| `af9:dtpf/chromodynium_plates` | 2,000 mB chromodynium plasma | 6 chromodynium plates (4 dust: 1.5x) |
| `af9:dtpf/tritanium_plates` | 4 tritanium ingots, 500 mB strange matter plasma | 6 tritanium plates |
| `af9:dtpf/neutronium_plates` | 4 neutronium ingots, 1,000 mB strange matter plasma | 6 neutronium plates |

The plasmas are AF9 Core's fluids `gtceu:strange_matter_plasma`, `gtceu:chromodynium_plasma`,
`gtceu:anti_matter_plasma` and `gtceu:idontknowium_plasma` (`.plasma()` on the
materials), plus carbon plasma (`.plasma()` on GT's carbon, ionised here for nano particles). Two are ionised here from their dust; **Anti Matter Plasma is not**: the Mega Fusion Reactor fuses it
from 4,000 mB niobium plasma, 4,000 mB sulfur plasma and 4,000 mB nickel plasma (2,000 mB, UEV voltage,
`fusion_reactor.js`), and the forge then forges it with 2,000 mB of star matter plasma into **Idontknowium Plasma**.
The controller is made in AF9's Advanced Circuit Manufacturer (`af9:dtpf`, UHV on 300 A, 128 CWU/t, 60 s):
16x UHV circuits, UHV machine hull, 8x transcendent casing, 2x field generator (UV: GT registers no UHV
field generator while its high-tier content is off), 4x AI Acceleration Card, 32x Qubit CPU chip
(`gtceu:qbit_cpu_chip`), 16x eDRAM chip (`af9:edram_chip`), 8 stacks (8x64) fine sanguinite wire,
5,760 mB plasma solder.

## 4. AI Acceleration Card

`af9:ai_acceleration_card` (AF9 Core `registry/AF9Items`): TPU and memristor on a wetware board,
traced in 128 fine sanguinite wire. UHV circuit assembler, plasma solder canonical (tin stays as a dead
budget option), clean room — `af9:ai_acceleration_card` in `kubejs/server_scripts/mods/gtceu/circuits_af9.js`.
The compute card of the new DTPF: its recipes take the card.

## 5. The recipe page (EMI / JEI)

`PlasmaForgeRecipeUI` / `PlasmaForgeFlowWidget` (AF9 Core `machine/`): a page of its own in the Particle Accelerator's style. Items
in a row top left with the fluids in a row under them, piped into the plasma bore (a glass tube through three coil rings that light
in turn, a plasma bar in the tint of the recipe's first fluid), GT's arrow over the bore, the forged matter piped out to the right,
items over fluids. Only the pipes of slots the recipe uses are drawn; GT's own slot ids, so EMI looks recipes up as usual. Hovering the
bore names the Mk1 and Mk2 and says what the forge needs (a core at 9,000 K, osmium plasma); the page also carries that as a data line.
The recipe type's maximum is 3 of each so the rows fit.
