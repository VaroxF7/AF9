# AF9 Core

Small Forge mod (1.20.1, GTCEu 7.2.0) with the machine logic KubeJS can't provide on its own.
Machines, materials and recipes stay in `../kubejs`; KubeJS plugs the Java classes in via `.machine(...)`.

| Class | Used by | Does |
|---|---|---|
| `litho/LithoMode` | everything below | The five UV modes (MUV 350 nm ... LUV 50 nm): power, density and die factors, light source (i-line, KrF, ArF, ArF immersion) with wavelength, NA and resist, the version each needs and the version bonuses, the package list. Must match the KubeJS recipes |
| `machine/PhotolithographyLineMachine` | `kubejs/startup_scripts/gtceu/photolithography.js` | Line version 1-5 (lens slices + light source), recipe gate `LITHO_GATE` (version + power), `LITHO_VERSION` (faster, better yield, more transistors above a mode's version), one structure preview page per version, controller UI with mode buttons, saved statistics, EMI recipe info (node + light + version) |
| `machine/LithoRecipeLogic` | the machine above | Counts printed packages per mode |
| `client/AF9Client` | wafer packages and chips | Chip `af9:litho_mode` model predicate (per-mode textures in `kubejs/assets`), package ribbon tinted per mode, tooltip (mode, line version, transistors per die and wafer) |
| `fab/FabFamily`, `fab/IFabMachine` | the SMC fab machines | The four fab families (chemistry, separation, electrochemistry, thermal): changeover purge fluid and time, console colour, process steps per mode |
| `fab/FabModifiers` | `kubejs/startup_scripts/gtceu/fab_machines.js` | Recipe modifiers: changeover `PURGE`, `STRUCTURE_PARALLEL` (trays / membrane cells), `COIL_DISCOUNT`, `TIER_TEMPERATURE` (single furnaces), `THERMAL_OVERCLOCK` (GT's EBF rules) |
| `fab/FabRecipeLogic`, `fab/FabRecipeInfo` | the fab machines, EMI | Tells the machine which recipe finished (purge bookkeeping); temperature, coil and single-block tier on the thermal modes' EMI pages |
| `machine/fab/FabMultiblockMachine` | the four SMC multiblocks | Built-in clean room from filter casings, PTFE-pipe parallels, coil heat, run and changeover counters, console with clickable mode tiles |
| `machine/fab/FabTieredMachine` | the SMC single blocks (MV-LuV) | GT's slot page with a console strip above it, furnace temperature per tier, changeover purge |
| `machine/fab/FabConsoleWidget` | both fab machine classes | The fab console (status, modes, power, progress with purge share, clean class, parallels, heat, process steps) |

## Build

Needs JDK 17 (Temurin 17 is installed).

```
gradlew build
```

The jar lands in `build/libs/af9-core-<version>.jar` and goes into the instance's `mods/` folder.
Every AF9 client and server needs it, because the KubeJS scripts load its classes.

Versions are pinned in `gradle.properties` to what GTCEu 7.2.0 was built against; bump them together with the pack.
