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

`gtceu:plasma_soldering` (new recipe type, 10 items / 1 out / 2 fluids, chain in `solders.js`): runs only on
the Orbital Lithography Array Mk2 — extended 35x35 pattern, in orbit (`PLASMA_GATE` in af9-core checks orbit +
extended + full EU/t + sealed start-up). `af9:wetware_mainframe_uhv_plasma`: the same photonic UHV mainframe bill
as the assembly line version with half the plasma solder (720 vs 1440 mB) in half the time (1000 vs 2000 ticks) at
UHV. The assembly line version stays as the pre-Mk2 path.

## 4. Array Mk2 (extended, like the Space Elevator's sizes)

Same controller, two sizes (basic 25x25 / extended 35x35), switched with a screwdriver on the controller between
runs (af9-core `setExtended`, like the Elevator's size switch; the KubeJS pattern hands the extended one over with
`setExtendedPattern`, previews show both). Same structure scaled out: the basic 25x25 centred (offset 5) plus a
larger circular rim (radius 14.5-17.5) + cross spokes in the outer band, same blocks per level (O top deck, D sturdy
ring, C elsewhere; mast/air levels stay empty). Light ring radius 9.6 → 14.6 (`RING_RADIUS_MK2`, read dynamically
through `ringRadius()` so one model serves both; burn distance, magnetic field box and render bounding box follow
the size; plasma soldering glows plasma-violet, hotter with bloom).
