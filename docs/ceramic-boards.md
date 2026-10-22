# Ceramic Circuit Boards

GT's plastic circuit board is a fired ceramic tile in AF9, made from MV to EV (idea: TerraFirmaGreg's ceramic boards, LGPL-3.0, credited in
`CREDITS-textures.md`). The item is still `gtceu:plastic_circuit_board` (and `plastic_printed_circuit_board`), so every recipe that takes it keeps
working; only the name ("Ceramic Circuit Board") and the texture (`tools/textures/ceramic_boards.py`) changed. GT's four polymer recipes for the blank board
(polyethylene, PVC, PTFE, PBI) are removed. The etching of the printed board (`circuit_etching.js`) is untouched.

Recipes: `kubejs/server_scripts/mods/gtceu/ceramic_boards.js`. Materials (silicon carbide, alumina, beryllia: dust and plate) in
`AF9Materials.ceramicBoards()`, the three substrates in `AF9Items`.

| Tier | Ceramic | Substrate (EBF) | Boards (chemical reactor, substrate + nickel dust + 250 mB sulfuric acid) |
|---|---|---|---|
| MV | Silicon carbide: silicon + carbon dust, EBF 1,800 K, MV, 20 s, 2 dust | plate + 4 molybdenum foil, 1,800 K, MV, 10 s | **4 boards**, 20 s, MV |
| HV | Alumina: 2 aluminium dust + 3,000 mB oxygen, HV, 5 s, 5 dust | plate + 4 cupronickel foil, 2,700 K, HV, 8 s | **8 boards**, 6 s, HV |
| EV | Beryllia: beryllium dust + 1,000 mB oxygen, EV, 5 s, 2 dust | plate + 2 chromium dust, 3,600 K, EV, 6 s | **16 boards**, 2.5 s, EV |

Plates: 2 dust in the compressor (LV). The rule: a higher tier makes more boards per batch, from fewer foils and the same acid, in less time, so each board
is cheaper (energy per board: 12,800 EU at MV, 7,680 at HV, 6,400 at EV); the batch still asks for the tier's machine, hotter coils and a rarer ceramic.
The numbers are my pick; Janne decides on them.

# The tier 3 rocket

The tier 3 rocket (`rockets.js`) flies on tungstensteel and Ostrum: hull of 3 tungstensteel blocks and 3 Ostrum blocks (`ad_astra:ostrum_block`), engine
and tank with 4 tungstensteel plates and 4 Ostrum plates each, tungstensteel screws and drum. Titanium is out of it. Open point: tungsten comes only from Venus
sand (`docs/asteroid-fission.md` section 7), the tier 3 rocket's own destination.
