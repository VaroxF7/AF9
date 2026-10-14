# AE2 integration — base at MV, addons at HV

AE2's early game (charger, fluix, cables, presses from meteorites) is untouched. What changes
is everything silicon: printed circuits and processors are fab work, and storage scales with
GT's circuits. No new items. Recipes: `kubejs/server_scripts/mods/ae2/`
(`ae2_processors.js`, `ae2_components.js`, `ae2_devices.js`, `ae2_addons.js`).

Rule of thumb: **base AE2 builds at MV, addon stuff (MEGA Cells, Infinity Booster) at HV.**
Removals are mod-scoped (`{ mod: 'ae2', output: ... }`), so the new `af9:` recipes never match.

## Quartz (`ae2_quartz.js`, MV)

- **Growth** in the autoclave: certus / fluix seed + water → 1 crystal (600t), or +
  distilled water → 2 crystals (400t). Pool growth, budding and seeds stay as the pre-MV path.
- **Polishing** in the cutter + distilled water: certus / fluix crystal → 2x dust,
  sky stone block → 4x sky dust, ender pearl → 2x ender dust (100t). 2x the inscriber's
  crushing yield.
- **Fluix pearls** in the assembler (fluix crystal + 4x ender dust, solder pair).

## Processors (`ae2_processors.js`, MV)

- **Printed circuits** in the Photolithography Line, 350 nm (MV, 4A = 512 EU/t, 900t):
  `af9:coated_silicon_wafer` under the matching AE2 press as its mask (not consumed) +
  TMAH / distilled water / extreme clean dry air → 4x printed (`printed_silicon`,
  `printed_logic/calculation/engineering_processor`), with the mode's 2% broken wafer.
  Same wafer, fluids, EUt and break chance as the other 350 nm prints.
- **Processors** in the assembler (MV, 200t): printed + `printed_silicon` + redstone +
  solder (tin 144 budget, soldering alloy 72 canonical) → `logic/calculation/engineering_processor`.
- Removed: all `ae2:` recipes outputting the four printeds and the three processors.
  The inscriber keeps crushing and the name press.

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

## Addons (`ae2_addons.js`, all HV, cleanroom, high-grade solder)

- **MEGA Cells** (`megacells:`, guarded): 1m ← 3x AE2 256k, then 4m/16m/64m/256m each 3x
  previous, all + engineering processor + 4x sky dust + quartz glass + HV advanced
  integrated circuit; `bulk_item_cell` (chest + 1m component + HV circuit + sky dust).
  MEGA cell assembly stays vanilla and follows.
- **Infinity Booster** (`aeinfinitybooster:`, guarded): `infinity_card` (engineering +
  calculation + fluix + HV circuit), `dimension_card` (infinity card + engineering +
  diamonds + HV circuit).

## Intentionally vanilla

Raw materials and world interaction (certus/fluix/sky dust, budding, charging, crushing,
fluix mixing, meteorite loot, `ae2:silicon` smelting — now a dead end), decorative
compacting, facades, paintballs, combat tools, condenser/singularity mechanics, spatial
cells, and any ExtendedAE recipes (their outputs are untouched, so nothing is removed
that this pack does not redefine).

## Consequences

- First AE2 processor needs: EBF silicon boule → cutter blank → Coater Track → Line V1
  (MV circuits, MV energy) + a meteorite press. The whole base network is MV after that;
  MEGA scale is HV.
- Follow-ups, not done here: quest gating for the AE2 chapter, EMI descriptions.
