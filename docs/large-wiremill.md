# Large Wiremill

`gtceu:large_wiremill` (GCYM's Large Wire Factory, rebuilt; the GCYM machine is removed in gtceu-fork patch `0022`, the id
stays, so the quest, recipes and items that name it keep working). KubeJS multiblock: `kubejs/startup_scripts/gtceu/large_wiremill.js`,
logic `LargeWiremillMachine`, render `WireDrawRender` (af9-core), face texture `tools/textures/large_wiremill.py`.

## Structure (9 x 4 x 3, rows bottom to top, columns left to right seen from the front; the pattern is Janne's)

| Part | Columns | What |
|---|---|---|
| Left casing wall | 0 | stress-proof casing, the full end of the box |
| Glass channel | 1-4 | casing floor; laminated glass on the front, the back and the roof; one block wide and high, open (spaces in the pattern) in the middle: the machine is drawn here |
| Die plate | 5 | steel frames, rising one block above the roof, with a hole in the middle (the die) |
| Right casing box | 6-8 | solid stress-proof casing; the controller is in its front wall at column 7 (column 6, next to the plate, in the first sketch) |

Hatches (energy, item in/out, parallel, maintenance; maximums only) go in any stress-proof casing.
Runs the Wiremill's recipes and its Spooling mode, parallel hatch, non-perfect overclocks, batch mode.

## Render

A cabling machine like the real one (the reference photo): two solid orange discs on six rods carry six white bobbins;
a strand runs from each bobbin to a closing die on the axis, where they come together into one wire. The wire runs
through the die in the frame plate (its right end is the plate's right face, where the casing box starts) and, the other
way, through the discs into the take-up spool at the left casing wall: one unbroken line, no gap. While a recipe runs
the carrier turns (a turn a second at full speed, eased up and down) and light and dark stripes run along the wire;
idle, everything stands still. The controller face has an idle animation (8 frames), a working one (12) and a paused
one (4), like the Industrial Cutting Machine's.
