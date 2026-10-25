# The Dimensionally Transcendent Plasma Forge (DTPF)

After GTNH's: a vast hall that forges plasma into matter. The machine is `gtceu:dtpf` (af9-core `PlasmaForgeMachine`; the
structure and the machine definition in `kubejs/startup_scripts/gtceu/dtpf.js`; the recipes in
`kubejs/server_scripts/mods/gtceu/dtpf.js`). The structure is AF9's own design, not a copy of GTNH's.

## 1. The structure (43 x 13 x 11): a beam hall

Janne's own build, taken 1:1 from the structure he exported from his world (`dtpf.nbt`) and written into
`kubejs/startup_scripts/gtceu/dtpf.js` as `DTPF_AISLES`: 43 plates, back to front, each a hand-written grid of characters
(no maths, no loops). 587 blocks, 11 wide, 13 high, 43 long. The first hall (a 21 x 22 x 45 forge line) and the second (a
37 x 29 x 37 skeleton) are gone. From the front, where the controller is:

| Part | Where | Blocks |
|---|---|---|
| Front plate | z 42: a 7 x 7 plus-shaped plate of transcendent casing, the controller in the middle (x 5, y 6) | N, S |
| Front ring | z 41: an 11-wide ring of transcendent casing, open in the middle where the plate sits, a 3 x 3 ring of bridge casing round the end of the beam | N, B |
| The beam | a line of superconducting coil down the axis (x 5, y 6), z 1 to 41 | C |
| Big rings | z 11, 21, 31: 61 blocks of transcendent casing round the beam, a bridge casing either side of the beam and an injection casing above and below it | N, B, W |
| Small rings | z 6, 16, 26, 36: 33 blocks of transcendent casing with an injection casing either side of the beam | N, W |
| Pairs and clusters | injection casings beside the beam at z 5, 7, 15, 17, 25, 27, 35, 37; a bridge casing and an injection casing at z 10, 12, 20, 22, 30, 32 | W, B |
| Back ring and plate | z 1 and z 0: as the front's, the plate without a controller | N, B |

Blocks (AF9 Core's `registry/AF9Blocks`, textures in `tools/textures/dtpf_blocks.py`):

* **Dimensionally Transcendent Casing** `af9:dtpf_transcendent_casing` (N): GT5-Unofficial's `MACHINE_DIM_TRANS_CASING`, with a
  connected-texture sheet.
* **Dimensional Injection Casing** `af9:dtpf_injection_casing` (W): the original's `MACHINE_DIM_INJECTOR`. The controller's casing
  and the **only block that takes hatches**: any of them may be a hatch. Its texture is an animated *field stabilising pulse*
  (9 frames, a core that contracts and flares), after GTNH's.
* **Dimensional Bridge Casing** `af9:dtpf_bridge_casing` (B): the original's `MACHINE_DIM_BRIDGE`, an active block: dark while the
  forge idles, the four animated frames and an emissive layer while it works.
* **Superconducting Coil** `gtceu:superconducting_coil` (C): GT's own, the beam.
* The **Plasma Containment Coil** and **Plasma Resonance Coil** (`af9:dtpf_containment_coil`, `af9:dtpf_resonance_coil`) are no
  part of the hall any more; the resonance coil is still an ingredient of the Mk2 refit.

Recipes of the blocks (`server_scripts/mods/gtceu/dtpf.js`): the casings in the UV assembler (transcendent from mk3 fusion casing
and tritanium / neutronium plates; injection and bridge from it). Nothing needs the forge's own products, so the first hall builds
from scratch. The controller takes 8 transcendent casings, the Mk2 refit 4 bridge casings and 2 resonance coils.

**The controller's face** (`tools/textures/dtpf_face.py`, AF9's own): a screen that looks down the beam tube, a bright dot for
the beam and the field's hoops round it. Idle they stand dim; working, two hoops at a time run out of the beam towards you
(8 frames, with an emissive copy); paused, the beam is red.

**The force field** (`af9-core` `ForceFieldRender` / `ForceFieldGeometry`, a block entity renderer on the controller): a tube 2.6
blocks in radius round the beam between the two end rings (the rings' blocks come as near as 2.9 blocks to the beam, so it clips
nothing), low while the hall stands and hard while a recipe runs. Four styles (`DTPF_FIELD_STYLE` in `dtpf.js`): 0 hoops,
1 plasma sheath (**the one in use**, slowed to a fifth of its first pace), 2 helix, 3 lattice.

**The injectors.** The 42 injection casings feed the field in **vortices**, one for each row of three casings on the same side
(14 of them, found from `DTPF_AISLES` by `dtpfInjectors()` and grouped in `ForceFieldGeometry.injectors()`): the osmium plasma
swirls out of the three casings together and spins into the outside of the field, where it lands as a turning spiral. Four
styles exist (`FUNNEL`, `BRAID`, `CYCLONE`, `ROTOR`; `injectorStyle`), the default being `FUNNEL` until the owner picks one.
They are a glimmer while the field is not being fed and pump hard while it takes osmium plasma and the stabilisers' power
(`isFieldInjecting`). The field also gives off a slight blue **bloom** (additive haze round the tube that tints the nearby
casings; it is not a real light source). The field itself is only there while it holds any plasma (a cold hall has none).

**The field kills.** Anything living within 2.6 blocks of the beam between the end rings (`PlasmaForgeMachine.touchesField`,
checked every other tick while the field has strength) is set alight for 10 s and takes 1,000,000 damage of the damage type
`af9:dtpf_field`: it bypasses armour, enchantments, effects and shields like the Orbital Station's light ring. A player it kills
gets the same vaporised death screen as the ring (`AF9Network.sendRingDeath`) with its own message ("... touched the magnetic
field of a Dimensionally Transcendent Plasma Forge and was vaporized"). Creative and spectator players are spared. The geometry is plain Java with no
Minecraft in it, so the previews of the styles are made from the same code the game runs.

## 2. The machine

* **Hatches** on any injection casing (`setMaxGlobalLimited`: maximums, never required counts): **1 laser hatch** (the recipes'
  power), up to 6 energy hatches or wireless receivers (auxiliary power), 8 fluid input, 4 fluid output, 4 item input and 4 item
  output buses, 2 plasma hatches. **No maintenance hatch, no coolant.**
* **Power.** The recipes' own power comes through **one input only**: the laser hatch, or one wireless energy receiver in Primary
  mode (`PlasmaForgeMachine.isPowerLayoutValid`; a laser hatch and a primary receiver together hold the forge, with a message).
  Every plain energy input hatch is held in **Auxiliary** mode (set again every second, a screwdriver cannot switch it back); a
  wireless receiver keeps the mode it is set to. The auxiliary power is `docs/auxiliary-power.md`'s.
* **The magnetic field.** The field is *made of osmium plasma*, and keeping it up is what it costs to run the hall:
  * **osmium plasma** from the plasma hatches fills it: up to 2,500 mB a second in, **250,000 mB for a full field**, and it burns
    250 mB a second to stay up (the Mk2 half the burn);
  * **the stabilisers**: **10 A of LuV** (327,680 EU/t) of auxiliary power, every tick, whatever the hall is doing.
  A freshly built hall has **no field and no force field**: it exists only once the stabilisers have their power and the plasma
  flows (the force field is drawn, and kills, while the field holds any plasma, and it grows brighter with it). Powered, the
  field gains what the hall drew less the burn; with too little plasma it falls slowly; without the stabilisers' power it takes
  in nothing and falls three times the burn a second. A missed tick or two is forgiven (the second only counts as unpowered
  after three). A switched-off controller or a broken structure lets the field go.
  * **Ready (50 %)** is the gate for starting a recipe (`FORGE_GATE`, with the message "The magnetic field is below 50%").
  * **A weaker field costs more**: when a run starts, its time and its EU/t are multiplied by `getFieldPenalty()`, 1 at a full
    field rising linearly to **2 at 25 %** (so 1.67 at the 50 % start). It is fixed for the run.
  * **Below 25 %** a running recipe collapses and the run is lost (`onWorking`), so a flicker in the power does not cost a
    twenty minute run, but a real outage or a dry plasma hatch does.
* **Gate** (`FORGE_GATE`): it starts a recipe only when its primary power can supply the recipe's full EU/t, the field is up, and
  the power layout is valid.
* **Perfect overclocks** (`OC_PERFECT`), and the **running-time ramp** (`RAMP`, from GTNH's discount): while a recipe runs the
  forge warms up, 1 tick of warmth per tick of running time, to 30 minutes; a pause cools it three times as fast. Warmth scales
  the recipe's EU/t by up to **-50 %** and its run time by up to **-25 %**. A broken structure starts cold.
* **Plasma Heater** (`gtceu:plasma_heater`, 5x4x5, EV): the sub multi that ionises 4 osmium dust with 2,000 mB Endion to 2,000 mB
  osmium plasma, the field's only fuel. Its plasma hatches are filled from here.
* **Mk2** (`gtceu:dtpf_mk2`, same hall): the Mk1 controller turned with the heater's heart (assembly line refit). Half the osmium
  (50/25 mB a second) for **four times the input amperage**: the same recipes, hungrier power.
* **The console** (`PlasmaForgeConsoleWidget`): four gauges (the magnetic field with its two marks, the stabilisers, the osmium
  plasma, the primary power and its inputs), what is being made, the ramp and the run-time bar, with a tooltip on each gauge.
  Every text was checked against the font's glyph widths: the widest label and value of a column take 90 of its 100 px.
* **Modules** are not built yet: `docs/dtpf-modules.md` is the concept.

## 3. The recipes (`plasma_forge`, 3 items and 3 fluids in, 3 and 3 out; UHV voltage, 100 A, **1200 s** each)

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
bore names the Mk1 and Mk2 and says what the forge needs (its field up: osmium plasma and 10 A of LuV stabiliser power, one laser hatch,
1200 s a run); the page also carries two short data lines (each fits the page's 176 px in one line).
The recipe type's maximum is 3 of each so the rows fit.
