# Solders

Three solders cover the whole circuit chain: GT's `tin` stays the budget option everywhere, GT's `soldering_alloy`
stays on the MV bootstrap (the high-grade EBF needs HV), and AF9's own three take it from there.

Code: `af9-core/src/main/java/com/af9/core/registry/AF9Materials.java` (`solders()`),
`kubejs/server_scripts/mods/gtceu/solders.js` (bio-chain, plasma condensation),
`kubejs/server_scripts/mods/gtceu/circuits_af9.js` (who solders what),
`kubejs/startup_scripts/gtceu/photolithography.js` (`plasma_soldering` type + Array Mk2 pattern),
`af9-core/src/main/java/com/af9/core/machine/OrbitalLithographyMachine.java` (Mk2 sizes, `PLASMA_GATE`, ring).

## 1. High-grade solder (HV-UV, full replacement)

`gtceu:high_grade_solder`: SAC-InBi lead-free solder (Sn9BiInAg). Mixed in the MV mixer (one tier below its first
users, like the MV circuit alloys in `circuits_af9.js`): 9 tin + 1 bismuth + 1 indium + 1 silver dust (circuit 12) →
12 dust, 360 ticks at MV. GT derives the HV EBF recipe from the blast property (1500 K, LOW, HV, 500 ticks).
Its look is its own icon set (`solder`, parented to SHINY): satin silver with brushed streaks and a whisper of
bismuth iridescence (`tools/textures/solder.py` → `assets/gtceu/.../material_sets/solder/`), not the plain-white
METALLIC tint.

Every HV-LuV circuit assembler recipe that GT generated a `soldering_alloy` copy of now comes as tin + high-grade
(`af9:<id>` + `af9:<id>_high_grade`, 144 / 72 mB × solder multiplier). MV stays tin + soldering alloy (bootstrap:
the high-grade EBF needs HV, so no MV machine could melt it).

## 2. Living solder (UV wetware + neuron, bio-chain)

`gtceu:living_solder`: a conductive bio-hydrogel (silver nanowires in a crosslinked protein matrix, kept alive in
sterilized growth medium). No components and no blast property, so GT adds no electrolyzer, centrifuge or EBF
shortcut past the sterile chain.

`af9:living_solder_incubation` (chemical reactor, UV, sterile cleanroom): stem cells + 4 fine silver wire +
1000 sterilized growth medium + 500 mutagen → 2000 living solder, 600 ticks at UV. Reflow would kill the cells, so
all wetware / neuron circuits solder with this and never see a furnace: LuV `wetware_processor` (+ SoC version),
UV `wetware_processor_computer` (sterile cleanroom, replaces the old quantanium version — quantanium's UHV use
moves to the plasma condensation below).

## 3. Plasma solder + atomic soldering (UHV and beyond, Array Mk2 only)

`gtceu:plasma_solder` (dust + ingot + fluid, no components): quark-stabilised metallic plasma for atomic-level
soldering — ion-by-ion deposition, no reflow, nothing wasted.

`af9:plasma_solder_dust` (quark synthesis, UHV): 8 QGP traps + quantanium dust + 4000 supercooled endion →
plasma solder dust + 8 empty magnetic traps, 2400 ticks at 4A UHV (same pattern as the strange matter /
chromodynium recipes). GT's own extractor melts the dust to the fluid the soldering type consumes.

`gtceu:plasma_soldering` (new recipe type, 9 items / 1 out / 2 fluids, chain in `solders.js`): runs only in the
**Hyper-Intensity Laser Engraver** (`gtceu:hyper_intensity_laser_engraver`, `startup_scripts/gtceu/hile.js`: GTNH's
Industrial Laser Engraver, 5x5x5, a laser target hatch on top, a glass shaft and the Laser Resistant Plate under it;
perfect overclocks, parallel hatch). It left the Orbital Lithography Array Mk2. `af9:wetware_mainframe_uhv_plasma`: the photonic UHV mainframe bill
(10 UV supercomputers + photonic/spin dies), 720 mB plasma solder, 1000 ticks at UHV — the only way to a
`gtceu:wetware_processor_mainframe` (both assembly line versions, `af9:wetware_mainframe_uhv` and
`af9:wetware_mainframe_uhv_lean`, were removed).

## 4. Array Mk2 (extended, like the Space Elevator's sizes)

Same controller, two sizes (basic 25x25 / extended tower 73 x 130 x 73), switched with a screwdriver on the controller
between runs (af9-core `setExtended`, like the Elevator's size switch; the KubeJS pattern hands the extended one over
with `setExtendedPattern`; the preview has a second page for the Mk2 with the tower cut out of the pattern, 37 wide, the rings are drawn by the machine once it is formed and are not in it). The extended one is a
tower (`ORBITAL_MK2_SLICES`, built from the cell functions `orbitalTowerCell` and `orbitalRingCell`, d counting levels
below the controller), after the concept art: a hollow slab-sided body (17 x 11 above the big ring, 15 x 11 and 11 x 9
below it, then a stepped taper) of stress-proof and non-conducting panel columns with tungsten carbide frame corner posts, ribs standing
out of the faces, HSS-G coil light strips and rows of small lit windows, floors every 12 levels, round an
HSS-E frame core; an angular crown of PTFE casing on top (flat along the back and over the controller, falling away to
the front and sides, with a tall fin on the left, a sawtooth of spurs on the right and a dorsal fin; the hatches go on
the crown), an octagonal collar plate (19 wide) at each ring's middle level, pylons down the waist and an HSS-E needle
below, 130 levels in all. Three rings round it (`ORBITAL_RINGS`: top level, thickness, outer radius, band width): the middle one at d 40
(radius 36), one above at d 22 (26) and one below at d 58 (18), each a band of sturdy casing with a stress-proof top and a
non-conducting outer face, joined to the collar by an atomic casing beam between tungsten carbide frame rails along the axes (they start outside the collar, `ORBITAL_HALF` 9 = `TOWER_HALF`), about 9,000
blocks in all. The light rings (`RINGS_MK2`: back, radius, spin) lie 1.5 blocks inside the bands and turn like a
station's wheels, the middle one against the other two (habitat modules in `LightRingRender`, drawn per `RingSpec` from
`ringSpecs()`, so one model serves both sizes). Burn distance, the magnetic field box (`HALF_WIDTH_MK2` 36, `DEPTH_MK2`
134) and the render bounding box follow the rings; plasma soldering glows plasma-violet, hotter with bloom.

## 5. What only the Mk2 runs, and its beam focus

The extended station is the only one that runs **the 1 nm prints** (chromodynium, `LithoMode.N1`; `canPrint` and the
console's MK2 ONLY). The recipes carry
`MK2_GATE` (orbit, extended, aligned, the full EU/t, a started-up station); the 1 nm prints check the same in
`canPrint` / `canRun`. A basic station shows MK2 ONLY for them.

**Beam focus** (`OrbitalLithographyMachine.focus`, 0-1000, the console's BEAM FOCUS bar) is the Mk2's setup and its upkeep:

| Rule | Numbers (per half second, the station's tick interval) |
|---|---|
| builds up while formed on the extended size, switched on, started up and in orbit | +5 (1 % a second) |
| extra gain from computation the hatches can supply beyond what the running recipe draws | up to +10, full at 64 CWU/t spare |
| a running recipe drifts it down | -12 |
| every finished run | -30 |
| off, unpowered or out of orbit | -2 |
| lost with the structure and when the size is switched | 0 |

The Mk2's own work starts from **25 %** (ALIGNING until then). Above **60 % (sharp)** runs take 0.9x as long and every
print's break chance is 0.8x; above **90 % (locked)** 0.8x and 0.6x (`FOCUS` modifier, `machineBreakFactor`). The numbers
make the challenge: with no spare computation a running recipe loses 1.4 % a second, so a line without a pause needs an
HPCA of roughly 45 CWU/t more than the recipe draws to hold its focus (a 1 nm print draws 96), and a station that runs dry
has to re-align before the next run: from nothing 25 s on its own, about 8 s with the HPCA's spare computation.

**Focus lock** (an option: sneak + screwdriver on the controller, `focusLockOption`, kept when the size is switched): once
the focus is full and the lock is on, it **latches** (`focusLocked`) and stays full until the structure is broken or the
size is switched: no drift, no cost per run, no loss while the station is off. The price is a **supplemental power
connection**: every half second an energy hatch of the station that is *not the laser hatch* pays 2,048 EU/t of its
own buffer (`supplementalPaid`; an EV hatch or better on a full amp), so the lock needs a hatch of its own beside the
laser hatch that carries the 1 nm print. Without it for 5 s (10 intervals) the lock lets go: the focus stays full and
then follows the ordinary rules (drift, runs). The console shows FOCUS · LOCK ARMED while the option waits for a full
focus and "latched" once it holds.

## 6. Pico circuits

GTNH's Pico components as the XPS tier's own circuit line, made in the **Advanced Circuit Manufacturer**
(`advanced_circuit_manufacturer` type: up to 16 items and 4 fluids in, computation through its hatch, UHV on
300 A through its laser hatch; `circuits_af9.js` §11):

| Recipe | Takes | CWU/t | Time |
|---|---|---|---|
| `af9:pico_board` | not here: laid up at UV in the Assembly Line from ceranium and fibre interconnects (`ceranium.js`, `docs/ceranium.md`) | | |
| `af9:cleansed_pico_board` | pico board, 2,000 mB distilled water, 1,000 mB argon | 32 | 300 ticks |
| `af9:pico_cpu` | cleansed board, 2 TPU, 4 memristor, 4 quantum-dot chips, 2 chromodynium plates, 16 fine chromodynium wire, 576 mB plasma solder, 288 mB PBI | 64 | 800 ticks |
| `af9:organized_pico_circuit` | pico CPU, 8 photonic IC, 8 spin-logic chips, 8 fine sanguinite wire, 288 mB plasma solder, 144 mB PBI | 96 | 600 ticks |
| `af9:processed_pico_circuit_casing` | 2 chromodynium plates, tritanium frame, 16 PBI foil, 576 mB PBI | 32 | 400 ticks |
| `af9:pico_circuit_rack` | 4 organized circuits, casing, 32 fine sanguinite wire, 1,440 mB plasma solder, 1,152 mB PBI | 128 | 1,200 ticks |

They are inputs of the ladder above: the XPS processor takes a Pico CPU (lean: photonic package instead of the loose
dies), the XPS mainframe the organized circuit, the NVM processor a rack and the NVM mainframe 2 (leans add the qram
and qlos calibration chips), and the 256m MEGA cell an organized circuit with sanguinite wire. Textures are GTNH's
(`tools/textures/gtnh_pico/`).
