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

## Quantum Computer

The Quantum Computer is a port of GTNH's TecTech Quantum Computer (GT5-Unofficial `MTEQuantumComputer` and `MTEHatchRack`;
LGPL-3.0 or later, <https://github.com/GTNewHorizons/GT5-Unofficial>; licence text in `licenses/GT5-Unofficial-LGPL-3.0.txt`).
The component numbers in `RackComponent` and the structure are TecTech's, the code is AF9's.

| af9 file | GT5-Unofficial source (`src/main/resources/assets/gregtech/textures/blocks/iconsets/`) | Changes |
|---|---|---|
| `af9-core/.../textures/block/quantum_computer_casing.png`, `_end.png` | `EM_PC.png`, `EM_PC_NONSIDE.png` | none |
| `af9-core/.../textures/block/quantum_computer_vent_casing.png`, `_end.png` | `EM_PC_VENT.png`, `EM_PC_VENT_NONSIDE.png` | none |
| `af9-core/.../textures/block/quantum_computer_advanced_casing.png`, `_end.png` | `EM_PC_ADV.png`, `EM_PC_ADV_NONSIDE.png` | none |
| `kubejs/assets/gtceu/textures/block/multiblock/quantum_computer/overlay_front*.png` (controller face) | `EM_COMPUTER.png`, `EM_COMPUTER_ACTIVE.png` (+ `.mcmeta`) | none; the paused face is the idle one |
| `kubejs/assets/gtceu/textures/block/multiblock/computer_rack_hatch/overlay_front*.png` | `EM_RACK.png`, `EM_RACK_ACTIVE.png` (+ `.mcmeta`) | none; the paused face is the idle one |

The Rack Heat Vent icons (`item/rack_heat_vent_mk1-4.png`) are AF9's own (`tools/textures/rack_vents.py`).

## Mk2 Orbital Array casings

`orbital_light_casing` (+ `_ctm`) is AF9's own art, drawn from scratch (`tools/textures/orbital_light_src.py`, then `orbital_casings.py`); it replaces the Cosmic Frontiers texture.

The magnet casing texture is taken from **CosmicCore / Cosmic Frontiers** (`resources/assets/cosmiccore/textures/block/casings/solid/`;
textures in `block` are MIT-licensed per its license section, `licenses` in the archive), unchanged:

| af9 file (`af9-core/.../textures/block/`) | CosmicCore source |
|---|---|
| `orbital_magnet_casing.png` | `stellar_neutronium_grade_magnet.png` |
