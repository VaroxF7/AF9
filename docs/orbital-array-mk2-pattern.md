# Orbital Array Mk2: the pattern and its light

The Mk2 is Janne's `orbital.nbt`, built 1:1: a 73 x 99 x 73 spire of superconducting coils and magnets standing through three wheels,
a halo of four arms round the crown, a printing chamber with a floating silicon wafer and a deck round it, and a hover lance
(drawn, not built) leaving the open foot. The controller is the top block of the middle column, facing up.

## The pattern

`tools/orbital_mk2_layout.py path/to/orbital.nbt` reads the structure file and writes `kubejs/startup_scripts/gtceu/orbital_mk2_layout.js`
(`global.ORBITAL_MK2_AISLES`, one string per aisle, a row per line). `photolithography.js` turns it into the GT pattern; `orbitalWheres`
there says what each letter is. Do not edit the layout by hand: change the file or the tool and generate again.

What the file cannot hold is marked on top of it by the tool:

- **Hatches** (`O`): the non-conducting casing of the chamber deck's top face within 9.5 blocks of the axis, with nothing above it.
  Energy, laser, coolant, computation, data, optical, fluid, item and maintenance hatches go there.
- **Module slots** (`Y`): six non-conducting casings in the deck's rim with casing above and below (`MODULE_SLOTS` in the tool).
- **Rotor holders** (`R`): the four in the file, UV or better. UHV and above only exist in GT's dev builds, so effectively UV. The rotors are
  cosmetic: `RotorHolderCosmeticMixin` keeps them spinning without wear or effect. The holders are turned to face up.
- **The wafer cell** (`#`, the red wool): air that must stay air. The wafer floats there.

The file's coils are all plain superconducting coils, so the old coil bonus cannot be reached any more.

## The wheels

`OrbitalLithographyMachine.BANDS_MK2` (keep it in step with the file): the big wheel in the middle (back 34 to 36, radius 36), a smaller one
above (back 52, 53, radius 18) and below (back 16 to 18, radius 26). A wheel is its rim plus its spokes from `hub` blocks out of the axis;
the big one also takes the shield (the pillars and plates between back 30 and 41) so the plasma field's shield turns with it. The client hides
those blocks and draws them turning (`StructureSkins.trackRings`, `RingBlocks`, `RingDraw`). Players are carried on the rims' tops only.

## The light (client, `client/skin`)

`OrbitalFx`, `WaferDraw`, `CoreDraw` and `FxGeo`: everything additive, written straight into one buffer (about 2,000 quads at most), fewer
with distance.

- **Power lasers**: four blue plasma beams from the arm tips over the arms into the computation core round the controller.
- **Plasma field**: a barrel of field lines, rings, a sweeping scan ring and arcs round the magnets at the spire's middle; it flares with the shot.
- **The wafer loop**, 200 ticks (10 s) on the level's clock, not scaled by the machine: 0 to 30 a new wafer is built out of dust; 26 to 62 the
  core charges; 58 to 94 the central beam runs down the spire; 100 to 130 the light spreads over the wafer die by die; at 130 the wafer is
  atomized (flash, shock rings, dust) and the beam runs on to the deck; 140 to 168 the beam is drawn back up. It runs while the machine works
  and floats whole while it does not (also before the station has power).
- **Hover lance** (`CoreDraw`): source glow and ring in the foot's opening, a violet plume with twisting strands, shock diamonds, rings and embers.

Not compiled or run in the cloud; CI "Build AF9 Core" is the check.
