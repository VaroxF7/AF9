# AE2 integration — base at MV, MEGA ladder IV to UHV

AE2's early game (charger, fluix, cables, meteorite hunt for sky stone) is untouched. What changes
is everything silicon: printed circuits and processors are fab work, and storage scales with
GT's circuits. No new items. Recipes: `kubejs/server_scripts/mods/ae2/`
(`ae2_processors.js`, `ae2_components.js`, `ae2_devices.js`, `ae2_addons.js`).

Rule of thumb: **base AE2 builds at MV, MEGA Cells climb IV → UHV, Infinity Booster at HV.**
Removals are mod-scoped (`{ mod: 'ae2', output: ... }`), so the new `af9:` recipes never match.

## Quartz (`ae2_quartz.js`, MV)

- **Growth** in the autoclave: certus / fluix seed + water → 1 crystal (600t), or +
  distilled water → 2 crystals (400t). Pool growth, budding and seeds stay as the pre-MV path.
- **Polishing** in the cutter + distilled water: certus / fluix crystal → 2x dust,
  sky stone block → 4x sky dust, ender pearl → 2x ender dust (100t). 2x the inscriber's
  crushing yield.
- **Fluix pearls** in the assembler (fluix crystal + 4x ender dust, solder pair).

## Processors (`ae2_processors.js`, MV)

No presses, no inscriber prints — every chip is printed on a silicon wafer:

1. **Litho 350nm + cutter** (existing chain): coated silicon wafer → GT chip wafer →
   `ilc` / `cpu` / `lpic` / `ram` dies.
2. **Die packaging** in the assembler (MV, 300t, solder pair): silicon die + 2x electrum
   foil + 2x copper foil + 4x gold fine wire + RAM die → AE2 printed
   (`ilc` → printed logic, `cpu` → printed calculation, `lpic` → printed engineering).
3. **Carrier** in the cutter (MV, 200t): a silicon wafer dices straight into
   8x `printed_silicon`.
4. **Final assembly** in the assembler (MV, 200t, solder pair): printed + printed
   silicon + redstone + MV circuit (`good_integrated_circuit` for logic/engineering,
   `micro_processor` for calculation) → `logic/calculation/engineering_processor`.

Removed: all `ae2:` recipes outputting the four printeds and the three processors.
The inscriber keeps crushing and the name press; meteorite presses are out of the chain
(the Mysterious Cube still drops one random press as a souvenir, meteorites still give
sky stone).

## Components (`ae2_components.js`, all MV, no cleanroom)

Cell components climb 1k → 4k → 16k → 64k → 256k on MV circuits only, 3x previous +
calculation processor (logic for 1k) + bulk secondary + quartz glass, assembler with solder:

| Component | Previous | Secondary | MV circuit |
|---|---|---|---|
| `cell_component_1k` | — (logic + 4 certus quartz) | 4x redstone | `good_integrated_circuit` |
| `cell_component_4k` | 3x 1k | 4x redstone | `micro_processor` |
| `cell_component_16k` | 3x 4k | 4x glowstone | `micro_processor` |
| `cell_component_64k` | 3x 16k | 4x glowstone | `micro_processor` |
| `cell_component_256k` | 3x 64k | 4x sky dust | `micro_processor` |

- **Crafting storages**: `crafting_unit` + component + the same MV circuit.
- **Shells**: `crafting_unit` (4x steel plate + logic + calculation),
  `crafting_accelerator` (unit + 2x engineering + micro processor).
- **Housings**: 2x quartz glass + 3x plates (steel for items, copper for fluids) + 4x redstone.
- Cells (housing + component, shapeless) stay vanilla and follow automatically.

## Devices (`ae2_devices.js`, all MV, no cleanroom)

Every other base-AE2 buildable — charger, inscriber, vibration chamber, crank, condenser,
growth accelerator, sky stone tank, resonance generator, energy acceptor/cells, controller,
drive, chest, IO port, workbench, all terminals (incl. wireless), interface, pattern
provider, molecular assembler (takes a micro processor, like the controller and drive),
import/export/storage buses, formation/annihilation cores and planes, P2P tunnels, level
emitters, toggle bus, all three monitors, all nine upgrade cards, network tool, memory
card, color applicator, cable anchor, both wrenches and cutting knives, charged staff,
entropy manipulator, matter cannon, tiny TNT, spatial IO/pylon/anchor, quantum ring/link,
blank patterns, view cell, quartz glass, all ten portable cells — builds in the assembler
at MV from processors, steel/iron plates, quartz glass, quartz fiber, crystals and
fluix (tin/soldering-alloy pair; cables and fiber are dry single recipes).

Only the fluix base of each cable family is redefined (glass, covered, dense, smart,
dense smart); dye variants follow. Quartz fiber is redefined (certus crystal + glass).

## World mechanics (`ae2_world.js`, LV/MV)

- **Fluix dust** in the mixer (LV): charged certus + nether quartz + redstone →
  2x dust. The grid recipe is gone.
- **Crushing** in the macerator (LV, pre-MV yields): sky stone → 2x sky dust,
  certus/fluix crystal → 1x dust, quartz → nether dust. The MV cutter in
  `ae2_quartz.js` still pays double for crystals.
- **Fluix crystals** back out of dust in the chemical bath (MV): 4x dust +
  water → 4x crystal.

## Matter and spatial (`ae2_matter.js`, HV/EV)

- **Singularity** in the assembler at HV (solder pair): 32x matter ball + 4x
  fluix + engineering processor.
- **Entangled pair** at EV: singularity + 2x calculation + 4x fluix pearl → 2x.
- **Spatial cells** 2/16/128 at HV (solder pair): base + secondary + circuit +
  cell housing.

## Dyeing (`ae2_cosmetics.js`, LV)

All fifteen dye variants of the five cable families move from the grid to the
assembler (base cable + vanilla dye, 100t, dry like the cables). Facades stay on the
anchor-plus-block mechanic, which a fixed recipe cannot express.

## Wearables (`ae2_endgame.js`, HV/EV)

- **Wireless booster** at HV (solder pair): receiver + fluix + engineering +
  aluminium plates.
- **Quantum armor** at EV (solder pair): titanium plates + entangled
  singularity + engineering processor + 2x fluix pearl per piece, plus a
  signature circuit each (logic/calculation/engineering/MV for
  helmet/chestplate/leggings/boots, so no two pieces share an input set).

## Addons (`ae2_addons.js`)

- **MEGA Cells** (`megacells:`, guarded): the endgame ladder, one voltage tier per
  cell — 1m at IV, 4m at LuV, 16m at ZPM, 64m at UV (assembler, cleanroom,
  high-grade solder), each 3x previous + engineering processor + 4x sky dust +
  quartz glass + its tier circuit (`nano/computer`, `quantum/computer`,
  `crystal/computer`, `wetware/computer`). The **256m goal builds only in the
  Advanced Circuit Manufacturer at UHV on 300 A** (2x tritanium frame, 2x 64m,
  engineering, organized Pico circuit, sky dust, tritanium wire, europium plate,
  sanguinite wire; high-grade solder + PBI; 128 CWU/t through its computation hatch).
  `bulk_item_cell` rides at IV (chest + 1m + IV circuit + sky dust).
  MEGA cell assembly stays vanilla and follows.
- **Infinity Booster** (`aeinfinitybooster:`, guarded, HV): `infinity_card`
  (engineering + calculation + fluix + HV circuit), `dimension_card` (infinity card
  + engineering + diamonds + HV circuit).

## Meteorites (Moon and Mars only)

AE2 meteorites no longer fall on the Overworld: the biome tag override
(`kubejs/data/ae2/tags/worldgen/biome/has_meteorites.json`, with
`"replace": true` so the pack's list replaces AE2's instead of merging into it)
lists only the lunar wastelands and the three martian biomes. Each meteorite's
Mysterious Cube drops **one random press** instead of all four (loot override
`kubejs/data/ae2/loot_tables/blocks/mysterious_cube.json`: silk touch keeps the
cube, otherwise one of the four presses at equal weight), and the crater fallout
is moon sand / mars sand — no dirt, stone or grass (AF9 Core `ae2.FalloutMixin`
for the default mode plus `ae2.FalloutCopyMixin` for AE2's biome-tagged
sandy/cold modes, everything else runs AE2's code). Bring the meteorite
compass (and a rocket) and hunt off-world; the AE2 quest texts point there.

## Intentionally vanilla

Raw materials and world interaction (certus/fluix/sky dust, budding, charging,
`ae2:silicon` smelting — now a dead end), decorative compacting, facades,
paintballs, combat tools, condenser feeding (trash in, matter balls out),
and any ExtendedAE recipes (their outputs are untouched, so nothing is removed
that this pack does not redefine).

## Consequences

- First AE2 processor needs: EBF silicon boule → cutter blank → Coater Track → Line V1
  (MV circuits, MV energy). The whole base network is MV after that; MEGA scale climbs
  IV → UHV, Infinity Booster is HV.
- Follow-ups, not done here: quest gating for the AE2 chapter, EMI descriptions.
