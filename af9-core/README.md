# AF9 Core

Small Forge mod (1.20.1, GTCEu 7.2.0) with the machine logic KubeJS can't provide on its own.
Machines, materials and recipes stay in `../kubejs`; KubeJS plugs the Java classes in via `.machine(...)`.

| Class | Used by | Does |
|---|---|---|
| `litho/LithoMode` | everything below | The nine lithography modes, one per wafer substrate, and the machine that prints each (Mk1 line 350-100 nm, Mk2 scanner 80-65 nm, orbital station 50-1 nm): tier and power, light source (i-line, KrF, ArF, ArF immersion, EUV, high-NA EUV, X-ray FEL) with wavelength, NA and resist, base break chance, the break-chance and speed maths. Must match `AF9_WAFERS` (KubeJS server) and `AF9_WAFER_TABLE` (KubeJS startup) |
| `machine/LithoMachine` | the lithography machines | Vacuum 0-100 (pumps down in 10 s per level while powered, vents to 0 in 60 s without power, holds through prints), the break roll, printed/broken counters, recipe gates `LITHO_GATE` (mode allowed + power) and `STRIP_BROKEN` |
| `machine/PhotolithographyLineMachine` | `kubejs/startup_scripts/gtceu/photolithography.js` | Mk1 line (versions 1-3) and Mk2 scanner (versions 1-2) from a `Spec` (modes, lens block and slices, light sources): version from lens slices + light source, `LITHO_VERSION` (faster above a mode's version; fewer breaks in the roll), one structure preview page per version, recipe info (node, light, break chance) |
| `machine/OrbitalLithographyMachine` | same script | The Orbital Lithography Station (50, 20, 7, 1 nm): prints only in an orbit dimension (Ad Astra `*_orbit`); starts up in 10 s instead of pumping a vacuum; its light ring glows in the node's colour while it prints and burns whatever touches it (`common/AF9DamageTypes`, `network/AF9Network` + `RingDeathPacket`, `client/RingDeathOverlay`: a pixel "VAPORIZED" death screen) |
| `client/render/LightRingRender`, `machine/ILightRingMachine` | Orbital Lithography Station | GT's fusion ring (glowing torus, pulse to white, fade-out, Shimmer bloom; drawn after the translucent blocks without writing depth, so GT's frames stay visible; with a shader pack through the pack in lightning's shader, `client/render/IrisCompat`) for any machine, placed relative to the controller; `AF9MachineModels.workableCasingWithLightRing` |
| `machine/LithoRecipeLogic` | the lithography machines | Rolls the break when a print finishes and hands out the broken wafer instead |
| `machine/LithoConsoleWidget` | the lithography machines | The lithography console: mode tiles (show the active mode; GT's mode tab switches it), vacuum bar, break chance, power, what is printing, run-time bar, counters |
| `machine/ProcessMachine`, `machine/console/*` | the machines below | Console base (`ConsoleWidget`: frame, tiles, run-time bar, sync) and the process console (`ProcessConsoleWidget`: mode tiles, power, coolant, output, readout lines) |
| `machine/SupercoolerMachine` | `kubejs/startup_scripts/gtceu/cryogenics.js` | Supercooling Cryostat: dense cooling / supercooling, chamber temperature readout down to the -5000 K rating |
| `machine/part/CoolantHatchPartMachine` | same script | Coolant Hatch (LuV-UHV): fluid input hatch that only accepts `gtceu:supercooled_*`, ability `af9_coolant_input` |
| `machine/ParticleAcceleratorMachine` | `kubejs/startup_scripts/gtceu/particle_accelerator.js` | Particle Accelerator, a 47 × 47 storage ring after GTNH's Compact Fusion Computer: light ring in the beam tube (`LightRingRender` with a `wall`), its own screen (`console/AcceleratorConsoleWidget` in `console/SidePanelsUIWidget`), beam energy, coolant, run counter |
| `common/AF9Modifiers`, `common/IPowerGated` | cryostat, accelerator | `POWER_GATE`: a recipe only starts when the hatches can deliver its full EU/t (the cryostat's 4A HV) |
| `space/AsteroidFieldFeature`, `space/AF9Space` | `data/af9` (the biome `af9:asteroid_field`) | The asteroids of the Asteroid Field: five size classes of lumpy rocks (andesite, tuff, basalt, blackstone), drawn per chunk from the world seed and the cell of the class, so they come out whole in any order; GT's ore veins of the layer `af9_asteroid` (KubeJS) grow into them |
| `radiation/RadiationWatch` | player ticks | The warning above the hotbar near radioactive material (GT's hazard notion, the tag `#af9:radioactive`): inventory, dropped items, ore blocks, a running `gtceu:fx1_reactor`; setting `[radiation] hints` |
| `compat/extremereactors/ExtremeReactorsCompat` | Extreme Reactors (`bigreactors`) | Registers supercritical steam as a vapor (320 FE/mB) and maps `forge:supercritical_steam` to it, by the mod's inter-mod messages and reflection; does nothing without it |
| `blast/BouleMelting` | GT's Electric Blast Furnace | Adds the `gtceu:boule_melting` mode to the EBF and the Endion coil bonus (faster, parallels) |
| `wafer/WaferContamination` | wafer items (`#af9:wafers`) | Wafers a player takes into the inventory turn into contaminated wafers, unless the player wears gloves (`#af9:wafer_gloves`, in an armor or Curios slot) or stands in a clean Cleanroom |
| `wireless/*` | `kubejs/startup_scripts/gtceu/wireless_energy.js` | Wireless Energy Transmitter (substation output) and Receiver (any multiblock's input), 2-1000A, voltage from the substation's inputs; linked by data stick through channels saved with the world, any distance and dimension |
| `compat/curios/CuriosCompat` | `WaferContamination` | Gloves in a Curios slot (GT's Rubber Gloves go in `hands`); does nothing without Curios |
| `compat/jade/*` | Jade | Controller tooltip: vacuum bar (lithography), status, mode, product, run-time bar, readout lines |
| `client/AF9Client` | wafer items, machine models | Contamination warning in the wafer tooltip; registers the dynamic renders |
| `client/render/ModeFluidRender`, `machine/AF9MachineModels` | SMC Large Chemical Reactor | GT dynamic render: a fake fluid per machine mode inside a running multiblock, through its glass; `AF9MachineModels.workableCasingWithModeFluids` puts it on a KubeJS machine's model |
| `pattern/AF9Filters` | fab and lithography multiblocks | The Plascrete Filter Casing as a GT cleanroom filter (ISO 5); `cleanroomFilters()` = GT's filter predicate with the MV filters first in the preview and auto-build |
| `fab/FabFamily`, `fab/IFabMachine` | the SMC fab machines | The four fab families (chemistry, separation, electrochemistry, thermal): changeover purge fluid and time, console colour, process steps per mode |
| `fab/FabModifiers` | `kubejs/startup_scripts/gtceu/fab_machines.js` | Recipe modifiers: changeover `PURGE`, `STRUCTURE_PARALLEL` (trays / membrane cells), `COIL_DISCOUNT`, `TIER_TEMPERATURE` (single furnaces), `THERMAL_OVERCLOCK` (GT's EBF rules) |
| `fab/FabRecipeLogic`, `fab/FabRecipeInfo` | the fab machines, EMI/JEI | Tells the machine which recipe finished (purge bookkeeping); temperature, coil and single-block tier on the thermal modes' EMI pages |
| `machine/fab/FabMultiblockMachine` | the four SMC multiblocks | Built-in clean room from filter casings, PTFE-pipe parallels, coil heat, run and changeover counters, console (the mode tiles show the active mode) |
| `machine/fab/SmcReactorMachine` | SMC Large Chemical Reactor | `FabMultiblockMachine` + GT's `IFluidRenderMulti`: the open vessel's air blocks, turned with the controller |
| `machine/fab/FabTieredMachine` | the SMC single blocks (MV-LuV) | GT's slot page with a console strip above it, furnace temperature per tier, changeover purge |
| `machine/fab/FabConsoleWidget` | both fab machine classes | The fab console (status, modes, power, progress with purge share, clean class, parallels, heat, process steps) |

## Get it without building

GitHub Actions (`.github/workflows/build-af9-core.yml`) builds the mod on every push to `main` that changes
`af9-core/`, `kubejs/` or `config/` (or by hand: Actions → Build AF9 Core → Run workflow). On GitHub: **Actions → Build AF9 Core → the latest run → Artifacts**:

- **AF9-update**: `mods/af9-core-<version>.jar` + `kubejs/` and `config/` of that commit. Extract it
  into the instance folder (the one that contains `mods/`) and let it overwrite. Delete an older `af9-core-*.jar` in
  `mods/` if its version differs.
- **af9-core**: just the jar.

## Build

Needs JDK 17 (Temurin 17 is installed).

```
gradlew build
```

The jar lands in `build/libs/af9-core-<version>.jar` and goes into the instance's `mods/` folder.
Every AF9 client and server needs it, because the KubeJS scripts load its classes.

Versions are pinned in `gradle.properties` to what GTCEu 7.2.0 was built against; bump them together with the pack.
