# Texture credits

Some item textures are taken from **GT5-Unofficial** (GregTech: New Horizons),
<https://github.com/GTNewHorizons/GT5-Unofficial>, licensed under the GNU Lesser General Public License v3.0 or later.
That project credits Jimbno's UU-Tex pack and other texture artists for parts of its base pack.

| af9 texture (`af9-core/.../textures/item/`) | GT5-Unofficial source (`src/main/resources/assets/gregtech/textures/items/`) | Changes |
|---|---|---|
| `optical_cpu.png` (+ `.mcmeta`) | `gt.metaitem.03/726.png` (Optically Perfected CPU) | none |
| `optical_ram.png` (+ `.mcmeta`) | `gt.metaitem.03/725.png` (Optically Compatible Memory) | animated: a light pulse runs down it (`tools/textures/animate_optical.py`) |
| `optical_fiber_cable.png` (+ `.mcmeta`) | `gt.circuitcomponent/processed/processedcableopticalfiber.png` (Split Optical Fiber Cable) | animated: a light pulse runs along it |
| `nano_particles.png` (+ `.mcmeta`) | `materialicons/NONE/nanites.png` | tinted cyan (multiplied by 120,225,245) |

`fiber_interconnects.png` is our own work (`tools/textures/optical_items.py`, animated by `animate_optical.py`).

## Industrial Cutting Machine

The Industrial Cutting Machine copies the look of the cutting multiblock of GTNH 2.9 (GT5-Unofficial, LGPL-3.0 or later,
<https://github.com/GTNewHorizons/GT5-Unofficial>): purple plated casings, frame boxes, a glass window and an animated saw
blade. The casing texture, the controller panel (`tools/textures/cutter.py`) and the blade renderer
(`SawBladeRender`) are written from scratch for AF9 after that look; no GT5-Unofficial file is copied.
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
