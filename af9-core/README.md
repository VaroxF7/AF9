# AF9 Core

Small Forge mod (1.20.1, GTCEu 7.2.0) with the machine logic KubeJS can't provide on its own.
Machines, materials and recipes stay in `../kubejs`; KubeJS plugs the Java classes in via `.machine(...)`.

| Class | Used by | Does |
|---|---|---|
| `machine/PhotolithographyLineMachine` | `kubejs/startup_scripts/gtceu/photolithography.js` | Module detection (Mk I-III), tier/power recipe gate `LITHO_GATE`, controller UI with buttons, saved statistics |
| `machine/LithoRecipeLogic` | the machine above | Counts finished wafers per quality grade |

## Build

Needs JDK 17 (Temurin 17 is installed).

```
gradlew build
```

The jar lands in `build/libs/af9-core-<version>.jar` and goes into the instance's `mods/` folder.
Every AF9 client and server needs it, because the KubeJS scripts load its classes.

Versions are pinned in `gradle.properties` to what GTCEu 7.2.0 was built against; bump them together with the pack.
