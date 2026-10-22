# Texture credits

Some item textures are taken from **GT5-Unofficial** (GregTech: New Horizons),
<https://github.com/GTNewHorizons/GT5-Unofficial>, licensed under the GNU Lesser General Public License v3.0 or later.
That project credits Jimbno's UU-Tex pack and other texture artists for parts of its base pack.

| af9 texture (`af9-core/.../textures/item/`) | GT5-Unofficial source (`src/main/resources/assets/gregtech/textures/items/`) | Changes |
|---|---|---|
| `optical_cpu.png` (+ `.mcmeta`) | `gt.metaitem.03/726.png` (Optically Perfected CPU) | none |
| `optical_ram.png` (+ `.mcmeta`) | `gt.metaitem.03/725.png` (Optically Compatible Memory) | animated: a light pulse runs down it (`tools/textures/animate_optical.py`) |
| `optical_fiber_cable.png` (+ `.mcmeta`) | `gt.circuitcomponent/processed/processedcableopticalfiber.png` (Split Optical Fiber Cable) | animated: a light pulse runs along it |

`nano_particles.png` (an orbiting swarm, `tools/textures/nano_particles.py`) and `fiber_interconnects.png` are our own work (`tools/textures/optical_items.py`, animated by `animate_optical.py`); neither is taken from GTNH.

## Industrial Cutting Machine

The Industrial Cutting Machine is a copy of GTNH's (GT5-Unofficial `MTEIndustrialCuttingMachine`, LGPL-3.0 or later,
<https://github.com/GTNewHorizons/GT5-Unofficial>; licence text in `licenses/GT5-Unofficial-LGPL-3.0.txt`). Its textures:

| af9 file | GT5-Unofficial source (`src/main/resources/assets/`) | Changes |
|---|---|---|
| `af9-core/.../textures/entity/cutter/cutter_t1..t4.png` (saw blade, four blur frames each) | `gregtech/textures/model/cutter_t1..t4.png` | none |
| `af9-core/.../textures/block/cutter_casing.png` (Cutting Factory Frame) | `gregtech/textures/blocks/iconsets/CUTTING_FACTORY_FRAME.png` | none |
| `af9-core/.../textures/block/cutter_casing_ctm.png` (+ `cutter_casing.png.mcmeta`) | — (our own sheet) | the connected texture: the frame stays only at the edge of a group of blocks, the plating runs on across them (`tools/textures/industrial_cutter.py`) |
| `kubejs/assets/gtceu/textures/block/multiblock/industrial_cutter/overlay_front*.png` (+ `.mcmeta`, controller face) | `miscutils/textures/blocks/iconsets/controllerFaces/industrialCuttingMachine{,Active,Glow,ActiveGlow}.png` | redrawn in the same spirit: a viewport with the saw and a readout, two cells and the lamp; idle, working (the saw turns, sparks fly) and paused, each with an emissive layer (`tools/textures/industrial_cutter.py`) |

The structure (9 x 4 x 3) and the blade's size, position, speed, spin-up and motion blur are taken from the original;
`SawBladeRender` is a port of its `renderTESR` to 1.20 (the GL clip plane under the floor is a hand-made polygon clip).
GTNH's sawblade items, tiers and recipe changes are not ported: the blade texture follows the energy hatch tier instead.

## Large / Extreme Combustion Engine (block textures)

The Large and Extreme Combustion Engines wear GTNH's look. Taken from GT5-Unofficial (LGPL-3.0 or later), unchanged unless noted,
into `kubejs/assets/gtceu/textures/block/` (they replace GTCEu's textures of the same name):

| af9 texture | GT5-Unofficial source (`textures/blocks/iconsets/`) | Changes |
|---|---|---|
| `casings/solid/machine_casing_stable_titanium.png` | `MACHINE_CASING_STABLE_TITANIUM.png` | the sprite is unchanged; it gets a connected texture of our own (`machine_casing_stable_titanium_ctm.png` + `.mcmeta`, `tools/textures/industrial_cutter.py`): the frame stays only at the edge of a group of blocks, the plating runs on across them |
| `casings/gearbox/machine_casing_gearbox_titanium.png` | `MACHINE_CASING_GEARBOX_TITANIUM.png` | none |
| `casings/pipe/machine_casing_engine_intake.png`, `..._extreme_engine_intake.png` | `MACHINE_CASING_ENGINE_INTAKE.png`, `MACHINE_CASING_EXTREME_ENGINE_INTAKE.png` | none |
| `casings/pipe/machine_casing_*engine_intake_active.png` (+ `.mcmeta`) | same | 4-frame animation, a brightness pulse |
| `multiblock/generator/large_combustion_engine/overlay_front*.png` | `OVERLAY_FRONT_DIESEL_ENGINE[_ACTIVE][_GLOW].png` | the paused face is the idle one; no animation |
| `multiblock/generator/extreme_combustion_engine/overlay_front*.png` | `OVERLAY_FRONT_EXTREME_DIESEL_ENGINE[_ACTIVE][_GLOW].png` | as above |

## Planetary Satellite

The Planetary Satellite is a copy of GTNH's Planetary Gas Siphon (GT5-Unofficial `MTEPlanetaryGasSiphon`, gtnhintergalactic;
LGPL-3.0 or later, <https://github.com/GTNewHorizons/GT5-Unofficial>; licence text in `licenses/GT5-Unofficial-LGPL-3.0.txt`).

| af9 file | GT5-Unofficial source (`src/main/resources/assets/`) | Changes |
|---|---|---|
| `af9-core/.../textures/block/satellite_casing.png` (+ `_ctm.png`, `.mcmeta`) | `gtnhintergalactic/textures/blocks/gasSiphon/SiphonCasing.png` | recoloured to the colours of Enriched Naquadah Trinium Europium Duranide (`tools/textures/satellite_casing.py`) |
| `kubejs/assets/gtceu/textures/block/multiblock/planetary_satellite/overlay_front*.png` (controller face) | `gregtech/textures/blocks/iconsets/OVERLAY_FRONT_PLANETARYSIPHON{,_GLOW,_ACTIVE_GLOW}.png` | animated while working: the orb breathes, a glint sweeps over it (`tools/textures/satellite_casing.py`) |

The six Neutron Ray Reflectors keep the plate of GregTech CEu Modern's Iridium Neutron Reflector
(`gtceu/textures/item/neutron_reflector.png`, <https://github.com/GregTechCEu/GregTech-Modern>, LGPL-3.0; copy in `tools/textures/gt_src/`),
each tier on its own colour ramp with a glow sweeping over it (16-frame strip + `.mcmeta`, `tools/textures/ray_reflectors.py`):

| af9 texture (`af9-core/.../textures/item/`) | Changes over the source |
|---|---|
| `iridium_ray_reflector.png` (+ `.mcmeta`) | recoloured cold silver; a glow band crosses it left to right |
| `osmiridium_ray_reflector.png` (+ `.mcmeta`) | recoloured blue steel; the band crosses it diagonally |
| `unobtainium_vibranium_ray_reflector.png` (+ `.mcmeta`) | recoloured violet; the band runs down it while it breathes |
| `tritanium_ray_reflector.png` (+ `.mcmeta`) | recoloured teal; the band crosses it left to right |
| `ceranium_ray_reflector.png` (+ `.mcmeta`) | recoloured amber; the band crosses it diagonally |
| `neutronium_ray_reflector.png` (+ `.mcmeta`) | recoloured crimson; the band runs down it while it breathes |

## Mk2 Orbital Array casings

`orbital_light_casing` (+ `_ctm`) is derived from CosmicCore's `resonantly_tuned_virtue_meld_casing.png` (MIT, as below): its frame is kept and recoloured to warm gunmetal (`tools/textures/orbital_light_src.py`, base in `tools/textures/orbital_src/orbital_light_base.png`); the window (chrome and gold conductor tracks) is AF9's own.

The four structure casings (`orbital_combined_structure_casing` (+ `_ctm`), `orbital_truss_casing`, `orbital_girder_casing`,
`orbital_deck_casing`) are re-cut from GregTech CEu Modern's `gcym/stress_proof_casing.png`, `casings/solid/machine_casing_sturdy_hsse.png` and
`machine_casing_inert_ptfe.png` (LGPL-3.0; the sources are in `tools/textures/gt_src/`): the same shapes on a cool steel
colour ramp, with rivets and an inlay in AF9's teal (`tools/textures/orbital_mk2_casings.py`). The girder is the truss turned on its side.

The magnet casing texture is taken from **CosmicCore / Cosmic Frontiers** (`resources/assets/cosmiccore/textures/block/casings/solid/`;
textures in `block` are MIT-licensed per its license section, `licenses` in the archive), unchanged:

| af9 file (`af9-core/.../textures/block/`) | CosmicCore source |
|---|---|
| `orbital_magnet_casing.png` | `stellar_neutronium_grade_magnet.png` |

## Shock Capacitor Casing (Electric Implosion Compressor)

`shock_capacitor_casing.png` and `_ctm.png` are GregTech CEu's `machine_casing_stable_titanium.png` and its `_ctm.png`
(GregTech-Modern v7.5.3, LGPL-3.0, copies in `tools/textures/gt_src/`) with the hue moved from violet to teal
(`tools/textures/electric_implosion.py`).

## Certus Resonating Manufacturer

The Certus Resonating Manufacturer is a copy of TerraFirmaGreg's ME Resonance Fabricator (their id `me_assembler`) from
**TerraFirmaGreg-Team/Core-Modern** (commit `325b0835`, `MEAssemblerMachine`, `MEAssemblerRedstonePort`, `TFGPredicates`,
`TFGMultiMachines.ME_ASSEMBLER`, `TFGTRecipeTypes.ME_ASSEMBLER`) and **TerraFirmaGreg-Team/Modpack-Modern** (commit
`dd977368`, textures and crafting recipes), LGPL-3.0; licence text in `licenses/TerraFirmaGreg-Core-Modern-LGPL-3.0.txt`.
The structure and the recipe page are taken over; the Java is ported to AF9 Core (`MEResonanceFabricatorMachine`,
`ResonanceHatchPartMachine`) and the machine is registered in KubeJS
(`startup_scripts/gtceu/me_resonance_fabricator.js`). The budding grades, the wear and the redstone port are gone: the
Liquid Resonating Fluix in the Resonating Fluix Hatch stabilises the machine instead. The casings are GTCEu's Stable Titanium Machine Casing (`tools/textures/gt_src/`, LGPL-3.0) recoloured by
`tools/textures/resonator_casings.py` (cold steel for `ae2_machine_casing`, whose seams glow certus blue while the machine
works; graphite for `ptfe_black_casing`), joined by CTM. Recipe textures are TFG's.

The controller face is AF9's own: `kubejs/assets/gtceu/textures/block/multiblock/me_resonator/overlay_front*.png`
(`tools/textures/resonator_face.py`) — a certus crystal in a fluix heart with a ring of light around it.

Not taken over: the shader maps (`_n`, `_s`) of the AE2 casing. The Manufacturer's recipes (`kubejs/server_scripts/mods/ae2/me_resonance_fabricator_recipes.js`) and the `OxygenatedCondition` (Ad Astra's oxygen instead of TFG's environment system) are copied from TFG's `me_assembler` recipes and `OxygenatedCondition`, 82 recipes; the three Spatial Cell Components are left out (TFG's UV LEDs).
Substitutions: TFG's `cryogenized_fluix` in the AE2 casing recipe is `gtceu:liquid_certus_quartz`; the Moon-only rule of its
recipes became a cleanroom condition. Its AE2 blocks are read by id, so AF9 Core compiles without AE2.

## Superalloy item textures

The silhouettes and shading of the Kovar, Inconel, Havar, Hastelloy and Stellite parts (`assets/gtceu/textures/item/material_sets/<alloy>`) are
recoloured from the SHINY icon set of **GregTech CEu Modern** (<https://github.com/GregTechCEu/GregTech-Modern>, LGPL-3.0), repainted
with a palette and grain per alloy by `tools/textures/superalloys.py`.

## Wire spool items

The three spool textures (`af9-core/.../assets/gtceu/textures/item/material_sets/dull/wire_spool{,_secondary,_overlay}.png`)
are from **StarT-Core** (<https://github.com/StarT-Dev-Team/StarT-Core>, LGPL-3.0 or later), unchanged. The
`wire_spool.json` models under `af9-core/.../assets/gtceu/models/item/material_sets/` are the same `item/generated`
models StarT-Core uses; they all point at the dull spool textures, which GregTech tints with the material colour.
Licence text in `licenses/StarT-Core-LGPL-3.0.txt`.

## Redstone etching

The etching chain (redstone nitrate and chloride, p-toluenesulfonic acid, redstone tri-p-toluenesulfonate) follows TerraFirmaGreg's
`recipes.etching.js` / `recipes.circuit_board.js` (Modpack-Modern, LGPL-3.0), rewritten for AF9's materials (`circuit_etching.js`).

## XPS circuit board

`af9-core/.../textures/item/xps/xps_board.png` is GregTech CEu Modern's printed wetware circuit board
(`wetware_printed_circuit_board.png`, <https://github.com/GregTechCEu/GregTech-Modern>, LGPL-3.0), recoloured: a sand laminate with
sanguinite-red traces (`tools/textures/xps_board.py`).

## Ceramic Circuit Boards

`kubejs/assets/gtceu/textures/item/plastic_circuit_board.png` and `plastic_printed_circuit_board.png` (they replace GT's) and
`af9-core/.../textures/item/ceramic/*_substrate.png` are GregTech CEu Modern's plastic circuit boards
(<https://github.com/GregTechCEu/GregTech-Modern>, LGPL-3.0) redrawn as ivory ceramic with copper traces, two clipped corners and four
drilled holes (`tools/textures/ceramic_boards.py`). The idea (a ceramic board in three tiers) is TerraFirmaGreg's
(<https://github.com/TerraFirmaGreg-Team/Modpack-Modern>, LGPL-3.0, `recipes.circuit_board.js`); the recipes are rewritten.

## Nuclear rework

The molten-salt reactor concept (graphite moderator line, FLiBe coolant, heat exchanger, actinide burner rods) follows the idea of
**TerraFirmaGreg Modern**'s nuclear line (<https://github.com/TerraFirmaGreg-Team/Modpack-Modern>, LGPL-3.0); no code, recipe or texture was
copied, the recipes and numbers are our own (`docs/nuclear-rework.md`). The new item textures are recolours of AF9's own thorium fuel
textures (`tools/textures/nuclear_items.py`). Licence text in `licenses/TerraFirmaGreg-Core-Modern-LGPL-3.0.txt`.

## Bio Vat

The Bio Vat is GTNH's Bacterial Vat (BartWorks `MTEBioVat`, GT5-Unofficial `src/main/java/bartworks/common/tileentities/multis/MTEBioVat.java`,
MIT licence of bartimaeusnek; GT5-Unofficial is LGPL-3.0 or later, <https://github.com/GTNewHorizons/GT5-Unofficial>; licence text in
`licenses/GT5-Unofficial-LGPL-3.0.txt`). Its textures:

| af9 file | GT5-Unofficial source (`src/main/resources/assets/gregtech/textures/blocks/iconsets/`) | Changes |
|---|---|---|
| `kubejs/assets/gtceu/textures/block/multiblock/bio_vat/overlay_front.png` | `OVERLAY_FRONT_DISTILLATION_TOWER.png` | none (converted to RGBA) |
| `.../bio_vat/overlay_front_active.png` | `OVERLAY_FRONT_DISTILLATION_TOWER_ACTIVE.png` | none (converted to RGBA) |
| `.../bio_vat/overlay_front_emissive.png` | `OVERLAY_FRONT_DISTILLATION_TOWER_GLOW.png` | none (converted to RGBA) |
| `.../bio_vat/overlay_front_active_emissive.png` | `OVERLAY_FRONT_DISTILLATION_TOWER_ACTIVE_GLOW.png` | none (converted to RGBA) |
| `.../bio_vat/overlay_front_paused.png`, `overlay_front_paused_emissive.png` | the idle face and its glow | copies of the idle ones |

The structure (5 x 4 x 5), the glass-tier gate and the output-fill bonus are taken from the original; the culture in the tank is drawn
(`BioVatRender`) instead of placed as fluid blocks. The radiation hatch and the petri-dish cultures are not ported.

