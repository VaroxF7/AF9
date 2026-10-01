# AF9

AF9 turns a GregTech CEu Modern (7.2.0) pack on Minecraft 1.20.1 Forge into a semiconductor factory: silicon from
quartz to wafers, a real fab chemistry, lithography from 350 nm down to 1 nm, AF9's own chips and circuits, a machine
bus that runs the factory, and computation to feed it.

This repository holds AF9's layer on top of the pack: the AF9 Core mod, the KubeJS scripts and assets, the quests and
the configs AF9 changes. Everything else comes from the pack itself.

## What's in it

| Path | What |
|---|---|
| `af9-core/` | AF9 Core, a small Forge mod with the machine logic KubeJS can't provide on its own (see `af9-core/README.md`) |
| `kubejs/startup_scripts/gtceu/` | AF9's machines, materials, parts and recipe types |
| `kubejs/server_scripts/mods/gtceu/` | AF9's recipes |
| `kubejs/assets/` | textures, models and lang for the KubeJS side |
| `config/ftbquests/` | the quest chapters AF9 adds or extends |
| `config/` | the other configs AF9 changes |
| `docs/` | the design documents |

## Features

- **Semiconductor factory** (`docs/semiconductor-factory.md`): quartz to electronic-grade polysilicon, Czochralski
  boules and wafers; the SMC fab machines (chemistry, separation, electrochemistry, thermal) with changeover purges and
  built-in clean rooms; the Photolithography Line and Scanner and the Orbital Lithography Station, nine lithography
  modes with vacuum, break chance and real light sources; AF9's chips, GT's circuits on plain chips and tier metals;
  the Supercooling Cryostat, the Particle Accelerator and wireless energy. The Line and Scanner cool with air (Air
  Conditioning Hatches), draw OPC computation from the bus, are measured and corrected by
  a Metrology Station, and can multi-pattern one version above their own; a Coater Track primes, coats and bakes the
  wafers first (BARC, resist, TARC; the spun-off solvent is distilled back), the masks come in three classes by node
  (chrome, phase-shift, EUV) and the EUV tools are built from Mo/Si mirrors; the chemistry has RCA clean, piranha
  strip, ethyl lactate, BARC, TARC, an etch plasma and metal-oxo resists; six chip families (acoustic wave, photonics,
  spintronics, 2D materials, neuromorphic, quantum dots) feed three more card tiers of the computation arrays.
- **Lint** (`tools/lint/`): `bash tools/lint/run.sh` checks the recipes, multiblocks, quests, textures and lang files
  without starting the game (CI runs it before every build); `docs/review-findings.md` lists what it found.
- **Machine bus** (`docs/machine-bus.md`): Optical Bus Cable and Bus Connectors join the machines; the Central
  Monitor shows, runs and opens them; the Bus Controller picks their recipes, supplies them and crafts for the ME
  network through GT's ME Pattern Buffer; computation and research flow over the bus.
- **Computation**: CWU Servers (LV-IV), the N1 Computation Array and the N1 Supercomputer Array with Computer Racks
  and cards, and the ME Computation Link: an ME network needs computation for its channels.

## Install

Build AF9 Core (or take the jar from GitHub Actions, see `af9-core/README.md`), put it into the instance's `mods/`
folder, and copy `kubejs/` and `config/` over the instance's own. Every client and server needs the jar: the KubeJS
scripts load its classes.

## Build

```
cd af9-core
gradlew build
```

Needs JDK 17. The jar lands in `af9-core/build/libs/`.
