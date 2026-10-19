# AE2 integration — base at MV, MEGA ladder IV to UHV

AE2's early game (charger, fluix, cables, meteorite hunt for sky stone) is untouched. What changes
is everything silicon: printed circuits and processors are fab work, and storage scales with
GT's circuits. No new items. Recipes: `kubejs/server_scripts/mods/ae2/`
(`ae2_processors.js`, `ae2_components.js`, `ae2_devices.js`, `ae2_addons.js`).

Rule of thumb: **every base AE2 processor and recipe takes MV circuits only; HV and EV circuits are for the high-tier addons (MEGA Cells climb IV → UHV, Infinity Booster at HV).**
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
2. **Die packaging** in the assembler: all three printed circuits (logic `ilc`, calculation `cpu`, engineering `lpic`)
   are made **in a cleanroom at MV, 80 s (1600t) each**, from 16x foil (electrum; platinum for engineering), 16x copper
   foil, 16x redstone foil, 32x fine wire (gold; platinum for engineering), 16x fine electrum wire and 4x RAM chip
   per die, on 576 mB tin or 288 mB soldering alloy. The processors themselves need no cleanroom.
3. **Carrier** in the cutter (MV, 200t): a silicon wafer dices straight into
   1x `printed_silicon`. The `expatternprovider:cutter/silicon` and
   `gtceu:forming_press/ae2/ultimate_universal/printed_silicon_circuit` recipes are removed.
4. **Final assembly** in the assembler on 144 mB **liquid certus quartz** (the assembler takes one fluid, so the solder is spent in packaging): printed + printed silicon + redstone + 2x redstone foil + 2x fine electrum wire + MV circuit (MV, no cleanroom) —
   `good_integrated_circuit` for logic and engineering, `micro_processor` for calculation.

Removed: all `ae2:` recipes outputting the four printeds and the three processors.
All `ae2:inscriber` recipes are removed (crushing is replaced by the GT recipes above; the name press is no longer craftable), as are the six `advanced_ae` processor chambers and the `gtceu:forming_press` circuit imports (ae2 logic/calculation/engineering, appflux energy, aae quantum, megacells accumulation); the addon processors (`megacells:accumulation_processor` HV, `appflux:energy_processor` EV, `advanced_ae:quantum_processor` IV, all assembler on 144 mB liquid certus quartz) build from AF9's own AE2 processors plus a tier circuit; meteorite presses are out of the chain
(the Mysterious Cube still drops one random press as a souvenir, meteorites still give
sky stone).

## Liquid certus quartz and redstone foil (`ae2_quartz.js`, `AF9Materials.aeQuartz`)

New fluid `gtceu:liquid_certus_quartz` (extractor, MV: certus crystal → 144 mB, charged → 288 mB) and a
redstone plate (compressor, 4x redstone) and foil (bender, 4x per plate). The materials are registered in
AF9 Core (Java), so this needs a rebuilt core jar.

## Components (`ae2_components.js`, all MV, no cleanroom)

Cell components climb 1k → 4k → 16k → 64k → 256k on MV circuits only, 3x previous +
calculation processor (logic for 1k) + bulk secondary + quartz glass, assembler on liquid certus quartz (144 mB for 1k, doubling per step; the solder is spent in the processor):

| Component | Previous | Secondary | MV circuit |
|---|---|---|---|
| `cell_component_1k` | — (logic + 4 certus quartz) | 4x redstone | `good_integrated_circuit` |
| `cell_component_4k` | 3x 1k | 4x redstone | `micro_processor` |
| `cell_component_16k` | 3x 4k | 4x glowstone | `micro_processor` |
| `cell_component_64k` | 3x 16k | 4x glowstone | `micro_processor` |
| `cell_component_256k` | 3x 64k | 4x sky dust | `micro_processor` |

- **RAM**: every component also takes `gtceu:ram_chip`: 8x (1k), 16x (4k), 32x (16k), 64x (64k),
  128x (256k). 64k and 256k also take NAND memory chips (16x, 32x) and micro processors (4x, 8x);
  the 256k drops its quartz glass, the assembler's nine slots are full. Each step eats 3x the previous component, so the RAM compounds: one 256k component
  costs about 1,700 RAM chips in total. The counts sit in the `ram(...)` calls of `ae2_components.js`.
- **Crafting storages**: `crafting_unit` + component + the same MV circuit as the component.
- **Shells**: `crafting_unit` (4x steel plate + logic + calculation),
  `crafting_accelerator` (unit + 2x engineering + micro processor).
- **Housings**: 2x quartz glass + 3x plates (steel for items, copper for fluids) + 4x redstone.
- Cells (housing + component, shapeless) stay vanilla and follow automatically.

## Devices (`ae2_devices.js`, mostly MV)

Every other base-AE2 buildable — charger, inscriber, vibration chamber, crank, condenser,
growth accelerator, sky stone tank, resonance generator, energy acceptor/cells, controller,
drive, chest, IO port, workbench, all terminals (incl. wireless), interface, pattern
provider, molecular assembler (takes a micro processor, like the drive),
import/export/storage buses, formation/annihilation cores and planes, P2P tunnels, level
emitters, toggle bus, all three monitors, all nine upgrade cards, network tool, memory
card, color applicator, cable anchor, both wrenches and cutting knives, charged staff,
entropy manipulator, matter cannon, tiny TNT, spatial IO/pylon/anchor, quantum ring/link,
blank patterns, view cell, quartz glass, all ten portable cells — builds in the assembler
at MV from processors, steel/iron plates, quartz glass, quartz fiber, crystals and
fluix (tin/soldering-alloy pair; cables and fiber are dry single recipes).

The **controller** builds at MV without a cleanroom (tin/soldering-alloy pair): 6x sky stone block + 4x good integrated circuit + 8x of the `#af9:ae2_processors` tag (logic, calculation or engineering, one recipe plus the tin / soldering-alloy twin).

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
  MEGA components also eat higher-grade RAM per
  component: 1m 64x eDRAM, 4m 64x MRAM, 16m 128x MRAM, 64m 64x eDRAM + 128x MRAM, and the 256m
  goal 128x MRAM + 64x eDRAM on top of the 64m stack. (QRAM is left out: it only comes from the
  lunar-air XPS line, after UHV.) MEGA cell assembly stays vanilla and follows.
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

- First AE2 processor needs: EBF silicon boule → cutter blank → Coater Track → Line V1.
  The whole base network is MV circuits; MEGA scale climbs
  IV → UHV, Infinity Booster is HV.
- Unverified: written without compiling or in-game testing (cloud env cannot build).
- Follow-ups, not done here: quest gating for the AE2 chapter, EMI descriptions.

## ME Resonance Fabricator (`me_resonance_fabricator.js`, HV)

The machine, recipe type (id `gtceu:me_assembler`) and recipes are TerraFirmaGreg's ME Assembler (credits in `CREDITS-textures.md`); the
structure is AF9's own: a 7 x 9 x 7 tower of black PTFE (`af9:ptfe_black_casing`) with stainless steel corner pillars, two bands of AE2
casing (`af9:ae2_machine_casing`), a window band of quartz vibrant glass and a roof plate. In the window band a glass cube holds the
Certus Quartz Block or Budding Certus. Everything inside the walls may be any block (Spatial Pylons and IO Ports for ME spatial
storage fit there). The controller sits on the front face above the base deck; all hatches go in the base deck (the bottom 7 x 7
layer): one energy hatch, one maintenance hatch, up to 3 fluid inputs, 1 item output, up to 9 item buses, one ME Redstone Port,
and a Data Access Hatch for the research slot.

- **Speed** by the block in the middle: Certus Quartz Block x1, Chipped (AE2's damaged) x9, Flawed x33, Flawless x129,
  Exquisite x513 (recipe time divided by 1 + 8 / 32 / 128 / 512). No overclocks, batch mode is always on (the machine
  runs as many recipes at once as fit one batch). A recipe above the machine's voltage tier does not run.
- **Wear**: every run uses up work (recipe time x batched runs / 500, at least 1) from the block's health, rolled
  200-800 per grade; at 0 the block drops one grade (Exquisite to Flawless and so on, down to the plain Certus Block).
  Breaking the machine keeps the health on the item.
- **ME Redstone Port**: signal 0 nothing in the middle, 1 Certus Quartz Block, 2 Chipped ... 5 Exquisite.
- **Recipes**: the type `gtceu:me_assembler` (9 items in, 1 out, 3 fluids in, research slot) has no recipes yet. TFG's
  recipes craft TFG's items, so none were taken over; the machine, the port and both casings are craftable.

