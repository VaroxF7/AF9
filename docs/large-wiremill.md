# Large Wiremill

`gtceu:large_wiremill` (GCYM's Large Wire Factory, rebuilt; the GCYM machine is removed in gtceu-fork patch `0022`, the id
stays, so the quest, recipes and items that name it keep working). KubeJS multiblock: `kubejs/startup_scripts/gtceu/large_wiremill.js`,
logic `LargeWiremillMachine`, render `WireDrawRender` (af9-core), face texture `tools/textures/large_wiremill.py`.

## Structure (13 x 5 x 5, one-block shell, rows bottom to top, columns left to right seen from the front)

| Part | Columns | What |
|---|---|---|
| Left casing end | 0-2 | stress-proof casing shell, hollow (air) inside |
| Glass chamber | 3-7 | casing floor, laminated glass walls and roof, steel frame edges, air inside: the machine is drawn here |
| Die plate | 8 | steel frames, one hole (air) in the middle: the die |
| Right casing box | 9-12 | casing shell, hollow inside; the controller is in its front wall at column 10 (column 9, next to the plate, in the first sketch) |

Hatches (energy, item in/out, parallel, maintenance; maximums only) go in any stress-proof casing, at least 120 casings.
Runs the Wiremill's recipes and its Spooling mode, parallel hatch, non-perfect overclocks, batch mode.

## Render

The wire is one unbroken line from the right box's end wall, through the die, through the flyer's two wheels, to the arm
that carries it to a pulley and lays it on the take-up spool at the left end. While a recipe runs the flyer turns (a turn a
second at full speed, eased up and down) and light and dark stripes run along the wire; idle, everything stands still.
The controller face has an idle animation (8 frames: breathing display and lamp, the wire's glint crawling), a working one
(12) and a paused one (4), like the Industrial Cutting Machine's.
