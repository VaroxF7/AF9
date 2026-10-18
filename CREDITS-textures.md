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
<https://github.com/GTNewHorizons/GT5-Unofficial>; licence text in `licenses/GT5-Unofficial-LGPL-3.0.txt`). Copied, unchanged:

| af9 file | GT5-Unofficial source (`src/main/resources/assets/`) |
|---|---|
| `af9-core/.../textures/entity/cutter/cutter_t1..t4.png` (saw blade, four blur frames each) | `gregtech/textures/model/cutter_t1..t4.png` |
| `af9-core/.../textures/block/cutter_casing.png` (Cutting Factory Frame) | `gregtech/textures/blocks/iconsets/CUTTING_FACTORY_FRAME.png` |
| `kubejs/assets/gtceu/textures/block/multiblock/industrial_cutter/overlay_front*.png` (+ `.mcmeta`, controller face) | `miscutils/textures/blocks/iconsets/controllerFaces/industrialCuttingMachine{,Active,Glow,ActiveGlow}.png` (the paused face reuses the idle one) |

The structure (9 x 4 x 3) and the blade's size, position, speed, spin-up and motion blur are taken from the original;
`SawBladeRender` is a port of its `renderTESR` to 1.20 (the GL clip plane under the floor is a hand-made polygon clip).
GTNH's sawblade items, tiers and recipe changes are not ported: the blade texture follows the energy hatch tier instead.

## Large / Extreme Combustion Engine (block textures)

The Large and Extreme Combustion Engines wear GTNH's look. Taken from GT5-Unofficial (LGPL-3.0 or later), unchanged unless noted,
into `kubejs/assets/gtceu/textures/block/` (they replace GTCEu's textures of the same name):

| af9 texture | GT5-Unofficial source (`textures/blocks/iconsets/`) | Changes |
|---|---|---|
| `casings/solid/machine_casing_stable_titanium.png` | `MACHINE_CASING_STABLE_TITANIUM.png` | none (plain, no connected textures) |
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
