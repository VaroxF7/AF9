# AF9 Core

Small Forge mod (1.20.1, GTCEu 7.2.0) with the machine logic KubeJS can't provide on its own.
Machines, materials and recipes stay in `../kubejs`; KubeJS plugs the Java classes in via `.machine(...)`.

| Class | Used by | Does |
|---|---|---|
| `litho/LithoMode` | everything below | The nine lithography modes, one per wafer substrate (350 nm silicon ... 7 nm strange matter on the line, 1 nm chromodynium in orbit): tier and power, light source (i-line, KrF, ArF, ArF immersion, EUV, high-NA EUV, X-ray FEL) with wavelength, NA and resist, base break chance, the break-chance and speed maths. Must match `AF9_WAFERS` (KubeJS server) and `AF9_WAFER_TABLE` (KubeJS startup) |
| `machine/LithoMachine` | both lithography machines | Vacuum cleanliness 0-100 (rises with power and no maintenance problems, falls 10-15 per wafer), the break roll, printed/broken counters, recipe gates `LITHO_GATE` (mode allowed + power) and `STRIP_BROKEN` |
| `machine/PhotolithographyLineMachine` | `kubejs/startup_scripts/gtceu/photolithography.js` | Line version 1-8 (lens slices + light source), `LITHO_VERSION` (faster above a mode's version; fewer breaks in the roll), one structure preview page per version, recipe info (node, light, break chance) |
| `machine/OrbitalLithographyMachine` | same script | The 1 nm Orbital Lithography Station: prints only in an orbit dimension (Ad Astra `*_orbit`) |
| `machine/LithoRecipeLogic` | both lithography machines | Rolls the break when a print finishes and hands out the broken wafer instead |
| `machine/LithoConsoleWidget` | both lithography machines | The lithography console: mode tiles, vacuum bar, break chance, power, what is printing, run-time bar, counters |
| `machine/ProcessMachine`, `machine/console/*` | the machines below | Console base (`ConsoleWidget`: frame, tiles, run-time bar, sync) and the process console (`ProcessConsoleWidget`: mode buttons, power, coolant, output, readout lines) |
| `machine/SupercoolerMachine` | `kubejs/startup_scripts/gtceu/cryogenics.js` | Supercooling Cryostat: dense cooling / supercooling, chamber temperature readout down to the -5000 K rating |
| `machine/part/CoolantHatchPartMachine` | same script | Coolant Hatch (LuV-UHV): fluid input hatch that only accepts `gtceu:supercooled_*`, ability `af9_coolant_input` |
| `machine/ParticleAcceleratorMachine` | `kubejs/startup_scripts/gtceu/particle_accelerator.js` | Particle Accelerator: beam energy and coolant readout |
| `common/AF9Modifiers`, `common/IPowerGated` | cryostat, accelerator | `POWER_GATE`: a recipe only starts when the hatches can deliver its full EU/t (the cryostat's 4A HV) |
| `blast/BouleMelting` | GT's Electric Blast Furnace | Adds the `gtceu:boule_melting` mode to the EBF and the Endion coil bonus (faster, parallels) |
| `wafer/WaferContamination` | wafer items (`#af9:wafers`) | Wafers a player takes into the inventory turn into contaminated wafers, unless the player wears gloves (`#af9:wafer_gloves`) or stands in a clean Cleanroom |
| `compat/jade/*` | Jade | Controller tooltip: vacuum bar (lithography), status, mode, product, run-time bar, readout lines |
| `client/AF9Client` | wafer items | Contamination warning in the wafer tooltip |
| `fab/FabFamily`, `fab/IFabMachine` | the SMC fab machines | The four fab families (chemistry, separation, electrochemistry, thermal): changeover purge fluid and time, console colour, process steps per mode |
| `fab/FabModifiers` | `kubejs/startup_scripts/gtceu/fab_machines.js` | Recipe modifiers: changeover `PURGE`, `STRUCTURE_PARALLEL` (trays / membrane cells), `COIL_DISCOUNT`, `TIER_TEMPERATURE` (single furnaces), `THERMAL_OVERCLOCK` (GT's EBF rules) |
| `fab/FabRecipeLogic`, `fab/FabRecipeInfo` | the fab machines, EMI/JEI | Tells the machine which recipe finished (purge bookkeeping); temperature, coil and single-block tier on the thermal modes' EMI pages |
| `machine/fab/FabMultiblockMachine` | the four SMC multiblocks | Built-in clean room from filter casings, PTFE-pipe parallels, coil heat, run and changeover counters, console with clickable mode tiles |
| `machine/fab/FabTieredMachine` | the SMC single blocks (MV-LuV) | GT's slot page with a console strip above it, furnace temperature per tier, changeover purge |
| `machine/fab/FabConsoleWidget` | both fab machine classes | The fab console (status, modes, power, progress with purge share, clean class, parallels, heat, process steps) |

## Get it without building

GitHub Actions (`.github/workflows/build-af9-core.yml`) builds the mod on every push to `main` that changes
`af9-core/`, `kubejs/`, `config/` or `defaultconfigs/` (or by hand: Actions → Build AF9 Core → Run workflow). On GitHub: **Actions → Build AF9 Core → the latest run → Artifacts**:

- **AF9-update**: `mods/af9-core-<version>.jar` + `kubejs/`, `config/`, `defaultconfigs/` of that commit. Extract it
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
