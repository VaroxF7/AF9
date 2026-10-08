# AF9 — a semiconductor factory on top of GregTech CEu Modern

[![Build AF9 Core](https://github.com/VaroxF7/AF9/actions/workflows/build-af9-core.yml/badge.svg)](https://github.com/VaroxF7/AF9/actions/workflows/build-af9-core.yml)
![Minecraft 1.20.1](https://img.shields.io/badge/minecraft-1.20.1-green)
![Forge 47.4.0](https://img.shields.io/badge/forge-47.4.0-orange)
![GTCEu 7.2.0](https://img.shields.io/badge/GTCEu-7.2.0-blue)
![JDK 17](https://img.shields.io/badge/JDK-17-red)

**AF9 turns a GregTech CEu Modern (7.2.0) pack on Minecraft 1.20.1 / Forge into a semiconductor factory:**
quartz → polysilicon → Czochralski boules → wafers → real fab chemistry → lithography from
**350&nbsp;nm down to 1&nbsp;nm** → AF9's own chips and circuits → the computation that feeds it all.
Around the fab: uranium only from asteroids, a fission reactor, a ZPM space elevator that mines
renewable ore, a platinum-group refinery, and off-world oil.

> 🌐 **Landing page:** <https://varoxf7.github.io/AF9/> (`docs/index.html` in this repo)
> 📦 **Play it without building:** GitHub → *Actions → Build AF9 Core → latest run → Artifacts → **AF9-update*** (jar + `kubejs/` + `config/`).

This repository holds **AF9's layer on top of the pack**: the AF9 Core mod, the KubeJS scripts and
assets, the quests, and the configs AF9 changes. Everything else comes from the pack itself.

```text
quartz → MG-Si → polysilicon → boule (EBF Boule Melting) → blank wafer → Coater Track
  → Photolithography Line / Scanner / Orbital Station → GT chip wafers → cutter dies
  → circuit assembler / assembly line → circuits → computation → faster lithography …
```

---

## Contents

- [At a glance](#at-a-glance)
- [What AF9 adds](#what-af9-adds)
- [1 · Semiconductor factory](#1--semiconductor-factory-quartz-to-1-nm)
- [2 · Asteroid fission](#2--asteroid-fission-uranium-only-from-space)
- [3 · Computation](#3--computation-cwut-to-feed-it-all)
- [4 · Space Elevator](#4--space-elevator-gtnhs-renewable-ore-source)
- [5 · Platinum-group refinery](#5--platinum-group-refinery)
- [6 · Oil, off-world](#6--oil-off-world)
- [Progression roadmap](#progression-roadmap)
- [Repository map](#repository-map)
- [Install (play)](#install-play)
- [Build (develop)](#build-develop)
- [Lint & CI](#lint--ci)
- [Quests](#quests)
- [Requirements & compatibility](#requirements--compatibility)
- [Docs index](#docs-index)
- [Credits](#credits)

---

## At a glance

| | |
|---|---|
| Minecraft / Forge | 1.20.1 / 47.4.0 |
| GregTech CEu Modern | **7.2.0** |
| KubeJS | 2001.6.5-build.16 |
| Ad Astra | 1.15.20 (Asteroid Field is Ad Astra data) |
| AF9 Core | version in `af9-core/gradle.properties` (bumped by each jar build), mod id `af9` (Forge mod, JDK 17) |
| Optional | Jade, Curios (gloves), Extreme Reactors (supercritical steam vapor), EMI Accelerator (cache-safe) |
| GTCEu config | `enableCleanroom=true`, `cleanMultiblocks=false`, `enableMaintenance=true`, `highTierContent=false`, `orderedAssemblyLineItems=true` |
| Convention | `gtceu:` = base GregTech + everything AF9 registers through GT/KubeJS · `af9:` = AF9 Core items, dimensions, recipe ids. **No NBT anywhere in the chip chain.** |

---

## What AF9 adds

| System | One line | Deep doc |
|---|---|---|
| 🔬 Semiconductor factory | quartz → EG polysilicon → CZ boules → 9 wafer substrates → fab chemistry → 350–1 nm lithography → GT + AF9 chips | [`docs/semiconductor-factory.md`](docs/semiconductor-factory.md) |
| ☢️ Asteroid fission | Brannerite only in the Asteroid Field → yellowcake → FX-1 Reactor → supercritical steam + plutonium → Fusion Reactor Mk1 | [`docs/asteroid-fission.md`](docs/asteroid-fission.md) |
| 🧮 Computation | CWU Servers (LV–IV) as `IOpticalComputationProvider` | [`docs/computation.md`](docs/computation.md) |
| 🛰️ Space Elevator | GTNH's 35×35×43 (ext. 47×47) tower, Mining Drones + Modules, ore expeditions + GTNH Space Pumping liquid missions | [`docs/space-elevator.md`](docs/space-elevator.md) |
| ⚗️ Platinum-group refinery | matte → leach → chloride liquor → Pt/Pd/Au, then Ru/Os via tetroxides, Ir/Rh last — in GT's own machines | [`docs/platinum-group-metals.md`](docs/platinum-group-metals.md) |
| 🛢️ Oil, off-world | Overworld oil off; Oil Regolith in asteroids → Impure → Shiny → Oil/Heavy Oil; fluids drilled in the Field | [`docs/oil.md`](docs/oil.md) |
| 🔴 Sanguinite | bright-red UHV superconductor (lossless UV 4A): Nt/Ti dusts + H₂ + Ares gas + LXA-1 → Sanguinite Hearth Furnace under supercooled endion (preheated 13,000 K, 4A UV, 60 s; EBF cannot smelt it) → Bulk Blast Chiller | [`docs/uhv-superconductor.md`](docs/uhv-superconductor.md) |
| 🔥 Plasma Forge (DTPF) | a 37×29×37 industrial skeleton of lattice pylons, diagonal struts and beams (after GTNH's, built differently) that forges strange matter and chromodynium plasma back into matter, with a running-time ramp: −50 % EU/t, −25 % time after 30 min | [`docs/dtpf.md`](docs/dtpf.md) |
| 🧹 Lint | `bash tools/lint/run.sh` checks recipes, multiblocks, quests, textures, lang without starting the game | [`tools/lint/README.md`](tools/lint/README.md) · [`docs/review-findings.md`](docs/review-findings.md) |

Plus the supporting machines: **SMC fab family** (chemistry / separation / electrochemistry / thermal,
with changeover purges + built-in cleanrooms), **Coater Track**,
**Supercooling Cryostat + Coolant Hatches**, **Particle Accelerator** (47×47 storage ring),
**Wireless Energy** (substation transmitter → any multiblock receiver, data-stick channels), and
**wafer contamination** (gloves or cleanroom, or the wafer is ruined).

---

## 1 · Semiconductor factory (quartz to 1 nm)

Full spec: [`docs/semiconductor-factory.md`](docs/semiconductor-factory.md).
Machine logic: [`af9-core/README.md`](af9-core/README.md) (classes `litho/*`, `fab/*`, `machine/*`).

### The chain

```text
high-purity quartz → MG-Si (SMC thermal, coke, 1800 K)
  → hydrochlorination → crude chlorosilanes → distillation (TCS/STC/DCS/BCl3 → fumed silica, boron)
sand → silica + L-01 (from Lunar Air, drilled on the Moon) → Moon Sand → O2 bath → polysilicon
  (+ Artemite dopant, mined on the Moon)
  → melt charge + seed + crucible → EBF Boule Melting (argon/xenon/endion) → CZ boule
  → cutter → blank wafer (one of 9 substrates)
  → Coater Track: HMDS prime → BARC → resist → TARC → bake (solvent distilled back)
  → Lithography (reticle of the node's mask class + developer + rinse + clean air + plasma)
  → GT chip wafers (or broken wafers) → cutter dies → circuits
```

Three printers cover nine substrates, **one mode per substrate**. A better substrate prints its own
chips **and every lower substrate's chips** — as GT's own plain chip wafers, no NBT — and yields
*more wafers per print* (not more dies per wafer; the cutter recipes are GT's, unchanged).

### The nine nodes (load-bearing numbers)

`LithoMode` (Java) = `AF9_WAFERS` (`kubejs/server_scripts/mods/gtceu/photolithography.js`) =
the item tables of `AF9Items` (`af9-core/src/main/java/com/af9/core/registry/AF9Items.java`). Files win over docs.

| Substrate | Blank wafer | Node | Tier / EU/t | Light λ / NA | Resist | Base break | Prints on |
|---|---|---|---|---|---|---|---|
| silicon | `gtceu:silicon_wafer` | 350 nm | MV · 4A = 480 | i-line 365 nm / 0.60 | `photoresist` | 2% | Line V1 |
| phosphorus | `gtceu:phosphorus_wafer` | 200 nm | HV · 1,920 | KrF 248 / 0.70 | `krf_photoresist` | 3% | Line V2 |
| naquadah | `gtceu:naquadah_wafer` | 100 nm | EV · 7,680 | ArF dry 193 / 0.75 | `arf_photoresist` | 5% | Line V3 |
| trinium | `af9:trinium_wafer` | 80 nm | IV · 30,720 | ArF dry 193 / 0.93 | `arf_photoresist` | 7% | Scanner V1 |
| naquadria | `af9:naquadria_wafer` | 65 nm | LuV · 122,880 | ArF immersion 193 / 1.20 | `arf_photoresist` | 9% | Scanner V2 |
| neutronium | `gtceu:neutronium_wafer` | 50 nm | ZPM · 491,520 | ArF immersion 193 / 1.35 | `arf_photoresist` | 12% | Orbital |
| transmuted neutronium | `af9:transmuted_neutronium_wafer` | 20 nm | UV · 1,966,080 | EUV tin plasma 13.5 / 0.33 | `euv_photoresist` | 18% | Orbital + EUV source |
| strange matter | `af9:strange_matter_wafer` | 7 nm | UHV · 7,864,320 | high-NA EUV 13.5 / 0.55 | `euv_photoresist` | 25% | Orbital + EUV source |
| chromodynium | `af9:chromodynium_wafer` | **1 nm** | UHV · 50A = 98,304,000 | X-ray FEL 1.0 / 0.50 (orbit only) | `af9:dry_resist_cartridge` | 35% | Orbital, in orbit |

Substrate yields (wafers-per-print scale): Si 1 · P 4 · Nq 8 · Ke 10 · Nq\* 12 · Nt 16 · Nt\* 24 · Sq 32 · Qc 64.

### What decides a print

- **Vacuum cleanliness 0–100.** Pumps draw ⅛ A continuously; sealed (100) in 10 s × level, vents
  100→0 in 60 s after a 3 s grace. **Prints only start sealed** — the machine waits by itself.
- **Break roll** when a print finishes: `(base + (100−low)/100 × 0.5) × 0.75^surplus`, capped 0.95
  (`surplus` = line versions above the mode). Broken prints give `af9:broken_<substrate>_wafer`
  (macerate → silicon dust) instead of the wafer.
- **Line versions** (lens slices + light source, like the Assembly Line's length): each version above
  the mode = ×0.8 time, ×0.75 break chance.
- **Cooling** (Air Conditioning Hatches) and **OPC computation** (computation hatch)
  improve the run; the Scanner/Line cool with air.
- **Masks in three classes** by node: chrome → MoSi phase-shift → EUV multilayer (Mo/Si mirrors);
  **Coater Track** primes/coats/bakes first; spun-off solvent is distilled back.
- **Orbit rule:** the Orbital Lithography Station prints only in an `*_orbit` dimension
  (Ad Astra), starts up in 10 s instead of pumping, and its light ring burns what it touches
  (pixel "VAPORIZED" death screen).

### AF9's own chips (beyond GT's)

Printed like GT's; their wafers are `af9:<chip>_wafer` → cutter → `af9:<chip>_chip`.

| Chip | What | Own substrate | Dies/wafer |
|---|---|---|---|
| `rf_transceiver` | radio on a chip | silicon (350 nm) | 8 |
| `apu` | CPU+GPU on one die | silicon | 6 |
| `mcu` | microcontroller | silicon | 16 |
| `asic` | application-specific IC | phosphorus (200 nm) | 8 |
| `edram` / `mram` / `feram` | embedded / magneto-resistive / ferro-electric RAM | trinium (80 nm) | 16 each |
| `vpu` | video processing unit | naquadria (65 nm) | 6 |
| `tpu` | tensor / AI unit | transmuted neutronium (20 nm) | 4 |

Plus six research families (acoustic-wave, photonics, spintronics,
2D-materials, neuromorphic, quantum-dot) for the pack's UHV wetware mainframe (§18.7 of the fab doc).
Uses: `server_scripts/mods/gtceu/chip_uses.js`.

---

## 2 · Asteroid fission (uranium only from space)

Full spec: [`docs/asteroid-fission.md`](docs/asteroid-fission.md).

```text
GT rocket (gregified parts, Aluminised Hydrolox) → Ceres → Asteroid Field (af9:asteroid_field)
  → Brannerite ore → purified dust → uranyl sulfate → yellowcake → uranium dust (+ UF6 → U-235)
  → pellets (EBF) → fuel rod (assembler, zirconium)
  → FX-1 Reactor (+ water + NaK) → supercritical steam → turbines → spent rod → Pu-239 / Pu-241
  → Fusion Reactor Mk1 (needs Pu-241 + AF9 chips)
```

- **The field:** seed-fixed clusters (island + 12–24 satellites, ~7.2 clusters/km²), andesite/tuff/
  basalt/blackstone so GT ore blocks grow in-rock at every height; **ancient temples** (shrine/temple/
  grand temple, loot `af9:chests/ancient_*`) in ~76% of clusters; ~9% brannerite + pentlandite,
  magnetite, cooperite by world-position noise.
- **Rockets, gregified:** every part and rocket is a GT machine recipe (T1–T2 assembler, T3–T4 Assembly
  Line with scanner research). T2 (titanium/ASIC) reaches Ceres; propellant is 2 recipes from 4 base
  ingredients (Al dust + ethylene + H₂ + O₂ → triethylaluminium → **Aluminised Hydrolox**, 3,000 mB =
  one launch). No oil needed for the first flight (ethylene via ethanol).
- **FX-1 Reactor** (`gtceu:fx1_reactor`, 5×5×5, EV 1,920 EU/t): 1 rod + distilled water + NaK →
  **61,440 mB supercritical steam** (80 EU/mB in steam turbines; ~4 Large Steam Turbines per rod,
  ~2.4 M EU net) + hot NaK (vacuum-freezer loop, closed) + spent rod → reprocessing → plutonium.
- **Old uranium closed:** pitchblende/uraninite veins weight 0, GT U-238/Pu separations removed,
  void-miner circuits rewritten — plutonium comes from spent fuel, ~4 ore per rod net.
- **Radiation:** AF9 warns *above the hotbar* near radioactive inventory/drops/ores/running FX-1
  (GT hazards + `#af9:radioactive`), before GT's hazmat punishment. Toggle: `[radiation] hints`.

---

## 3 · Computation (CWU/t to feed it all)

Full spec: [`docs/computation.md`](docs/computation.md). All sources are
`IOpticalComputationProvider` — fiber to Research Station / Orbital Station / litho computation
hatches.

| Source | Output | Notes |
|---|---|---|
| CWU Server LV→IV | 4 / 8 / 16 / 32 / 64 CWU/t | single block, EU→CWU (`VA/max` per CWU), front LEDs (red offline / green idle / blink busy) |

Larger computation comes from GT's own HPCA.

> Removed and staying removed: N1 computation / supercomputer arrays (racks of cards), machine bus, ME Computation Link, Crafting CPU Array
> (see history docs).

---

## 4 · Space Elevator (GTNH's, renewable ore from ZPM)

Full spec: [`docs/space-elevator.md`](docs/space-elevator.md). Structure block-for-block from GTNH
(`STRUCTURE_PIECE_MAIN`/`EXTENDED`), code `com.af9.core.elevator`.

- **Tower:** 35×35×43, 2,711 blocks (extended ring 47×47, +12 module slots); motors MK-I…V power
  6/12/15/18/24 module slots; modules MK-I/II/III fly 2/4/8 expeditions each; cable must see sky;
  animated cable (512 blocks) + climber rides on formation and every 100 s while online.
- **Ore missions** (`gtceu:space_mining`): drone (not consumed) + hydrogen + supercooled coolant +
  full EU/t for minutes → **8–48 stacks of raw ore per expedition**, all expeditions of a run to the
  *same* drawn asteroid (vein weighted by GT weight; tier-gated by drone; Mk-IV draws the exotic
  asteroid 1 run in 6). Every GT ore is reachable; EMI can look up ore → expedition.

| Drone | Reaches | H₂ / coolant | Energy | Time | Stacks |
|---|---|---|---|---|---|
| Mk-I | tier 1 (Overworld) | 64 B / 50 B H₂ | 4A ZPM | 3 min | 8–16 |
| Mk-II | 1–2 (+Nether) | 80 B / 64 B Ar | 8A ZPM | 4 min | 12–24 |
| Mk-III | 1–3 (+End) | 96 B / 80 B Xe | 16A ZPM | 5 min | 16–32 |
| Mk-IV | all + exotic | 100 B / 100 B endion | 32A ZPM | 6 min | 24–48 |

- **Liquid missions** (`gtceu:space_pumping`, GTNH's Space Pumping table): same flight cost,
  picked on the screen (ASTEROIDS / PLANET TYPE 2–8 → fluid), one kind at a time, to fluid hatches.
  Drone range-gates planet types (MK-I → 2–3 … MK-IV → 8).
- **Screen** tells the first missing thing (OFFLINE / PAUSED / NO SKY / NO MODULE / NO DRONE /
  NO POWER / NO HYDROGEN / NO COOLANT / NO ROOM / IDLE / RUNNING) with numbers.

---

## 5 · Platinum-group refinery

Full spec: [`docs/platinum-group-metals.md`](docs/platinum-group-metals.md).
GT's own chain untouched; AF9 adds a parallel, higher-yield route in GT's own machines (HV→IV).

```text
purified ore → EBF (silica flux, O2) → PGM Matte → leach → residue + Ni/Cu solutions
  → chloride liquor (Pt,Pd,Au) + insoluble residue (Rh,Ir,Ru,Os)
  liquor → Au → (NH4)2PtCl6 → Pt; filtrate → Pd(NH3)2Cl2 → Pd
  residue → fusion cake → Ru/Os liquor → RuO4/OsO4 distillation → Ru, Os
          → Rh/Ir liquor → Ir salt → Ir; filtrate → Rh salt → Rh (Zn reduction, ZnCl2 electrolyzed back)
```

Per 8-residue batch (≈4 cooperite): **Pt 4 · Pd 3 · Au 1 · Rh/Ru/Ir/Os 1 each**
(GT's line: Pt 2.7, Pd 1.6, others ~⅓–½). NH₄Cl/HCl/Zn/part-Cl loop back.

---

## 6 · Oil, off-world

Full spec: [`docs/oil.md`](docs/oil.md).

```text
Oil Regolith (asteroid pockets, ~7% of rock, falling sand-like block)
  → centrifuge (MV): 2 regolith → 1,500 mB Impure Oil + sand (+25% S)
  → chemical reactor: + H2SO4 → Shiny Oil
  → distillation tower: → Oil 600 + Heavy Oil 300 (+S) → GT refining as usual
```

- Overworld oil deposits removed (by id, so EMI bedrock diagrams agree); oilsands veins weight 0.
- Asteroid fluid deposits drilled with the Fluid Drilling Rig (prospector Fluid mode, 8×8-chunk
  regions): N₂/O₂ 40 · heavy water 20 · H₂SO₄/HCl/HNO₃ · HF/H₃PO₄ · acetic acid; heavy water
  electrolyzes to deuterium + oxygen.

---

## Progression roadmap

| Stage | You build | It unlocks |
|---|---|---|
| MV | quartz → MG-Si → polysilicon → Si boule → 350 nm line V1, CWU Server LV/MV, Coater Track | GT LV–MV chips on Si; computation for research |
| HV | 200 nm (KrF), FX-1 fuel chain, Oil Regolith → oil, PGM matte (Pt/Pd) | ASIC/MCU/APU; first fission power; benzene/propene |
| EV | 100 nm (ArF dry), rockets T2 → Ceres/Field, Supercooling Cryostat | Naquadah circuits; brannerite; cold coolants |
| IV–LuV | Scanner 80/65 nm (ArF + immersion), rockets T3–T4, Particle Accelerator | trinium/naquadria chips; e/m/FE-RAM, VPU; transmuted neutronium (NTD) |
| ZPM | Orbital Station (50 nm), Space Elevator Mk-I+, Fusion Reactor Mk1 (Pu-241) | renewable ore; neutronium HASoC; TPU path |
| UV–UHV | EUV 20/7 nm, high-NA, X-ray FEL 1 nm in orbit | strange-matter / chromodynium nodes; endgame computation |

Quest chapters track this order: MV → HV → EV → IV → LuV → ZPM → UV → UHV, plus
`photolithography`, `asteroid_fission`, `platinum_group`, `space_elevator`, `oil`, `circuits`.

---

## Repository map

| Path | What |
|---|---|
| `af9-core/` | AF9 Core Forge mod — the pack's blocks, items and materials, their textures and models, and the machine logic (see [`af9-core/README.md`](af9-core/README.md)) |
| `kubejs/startup_scripts/gtceu/` | AF9 machines, parts, recipe types |
| `kubejs/server_scripts/mods/gtceu/` | AF9 recipes |
| `kubejs/assets/` | lang (`gtceu`, `kubejs`), fluid textures |
| `config/ftbquests/` | quest chapters AF9 adds/extends |
| `config/` | other configs AF9 changes (`fml.toml`, …) |
| `docs/` | design docs + landing page (`docs/index.html`) |
| `tools/lint/` | offline checker (recipes, multiblocks, quests, textures, lang) |
| `tools/textures/` | texture helpers |

Key scripts: `photolithography.js` + `wafers.js` + `litho_process.js` (nodes must agree with
`LithoMode.java`), `fab_machines.js` / `fab_chemistry.js`, `cryogenics.js`, `particle_accelerator.js`,
`asteroid_fission.js` + `rockets.js` + `fusion_reactor.js`, `computation.js` + `cwu_server.js`,
`space_elevator.js`, `platinum_group.js`, `oil.js` + `vein_oil.js` + `vein_asteroid.js`, `uhv_superconductor.js`, `miner.js`,
`rubber.js`, `biofuels.js` + `aromatics.js` + `diesel.js` + `nether_chemistry.js` + `ethylene.js` (green chemistry),
`wireless_energy.js`, `chip_uses.js` / `circuits_af9.js`.

---

## Install (play)

1. Build AF9 Core (or skip building — see step 2).
2. **Easiest:** GitHub → **Actions → Build AF9 Core → latest run → Artifacts → `AF9-update`** —
   extract into the instance folder (the one containing `mods/`), overwrite, delete any older
   `af9-core-*.jar`. `AF9_BUILD.txt` records the commit.
3. Manual: put `af9-core-<version>.jar` into `mods/`, copy `kubejs/` + `config/` over the
   instance's own. **Every client and server needs the jar** (KubeJS scripts load its classes).
4. Needs: GregTech CEu Modern **7.2.0**, Ad Astra **1.15** (Asteroid Field data). Jade + Curios optional.

## Build (develop)

```bash
cd af9-core
./gradlew build        # Windows: gradlew build
# jar → af9-core/build/libs/af9-core-<version>.jar
```

Needs **JDK 17**. Versions are pinned in `af9-core/gradle.properties` to what GTCEu 7.2.0 was
built against — bump them together with the pack. With the AF9 fork of GTCEu (`gtceu_fork_version`)
published to mavenLocal (`./gradlew publishToMavenLocal` in `../GregTech-Modern-AF9`), the build compiles
against the fork; without it (CI) against the GTCEu 7.2.0 release.

## Lint & CI

```bash
bash tools/lint/run.sh            # full check, no game needed
bash tools/lint/run.sh --selftest # + self-test (what CI runs)
```

CI (`.github/workflows/build-af9-core.yml`, on pushes to `main` touching `af9-core/`,
`kubejs/`, `config/`, `tools/lint/`): **lint → build → package** (`af9-core` jar artifact +
`AF9-update` instance-update artifact). Findings log: [`docs/review-findings.md`](docs/review-findings.md).

---

## Quests

`config/ftbquests/quests/chapters/` — AF9 chapters plus tier chapters it extends:

`asteroid_fission` (34 quests, rockets → Mk1) · `photolithography` · `platinum_group` ·
`space_elevator` · `oil` · `circuits` · `medium/high/extreme/insane/ludicrous/ultimate/ultra_high_voltage` ·
`zero_point_module` (Mk1 now needs the Pu-241 quest) · `void_miner` · `applied_energistics_2`.

---

## Requirements & compatibility

- **Hard:** MC 1.20.1, Forge 47.4.0, GTCEu 7.2.0, Ad Astra 1.15.20, KubeJS 2001.6.5-build.16, JDK 17.
- **Soft:** Jade (controller tooltips: vacuum, status, mode, runtime), Curios (wafer gloves in
  `hands` slot), Extreme Reactors (supercritical steam registered as vapor via IMC+reflection —
  AF9 builds without it), EMI Accelerator (AF9 hashes the registry and clears its cache on change).
- **Do not disable** `enableCleanroom` — most cuts and HV–LuV circuits need it; the litho line
  itself needs none (built-in FFUs, `filter_casing`).

---

## Docs index

- [Semiconductor factory](docs/semiconductor-factory.md) — wafers, silicon, SoCs, chips, lithography
- [Asteroid fission](docs/asteroid-fission.md) — field, Brannerite, FX-1, plutonium, Mk1
- [Computation](docs/computation.md) — CWU servers
- [Space Elevator](docs/space-elevator.md) — structure, drones, ore + liquid missions, screen
- [Platinum-group metals](docs/platinum-group-metals.md) — refinery chain
- [Oil](docs/oil.md) — regolith chain, fluid deposits, prospecting
- [UHV superconductor](docs/uhv-superconductor.md) — Sanguinite: Ares gas, LXA-1 missions, RHF smelt
- [Rubber](docs/rubber.md) — latex from plants, rubber / liquid rubber / silicone / SBR
- [Quantanium](docs/quantanium.md) — the UHV unlock ore, Asteroid Field dike vein
- [Review findings](docs/review-findings.md) — what the lint found
- [AF9 Core](af9-core/README.md) — Java class map
- [Lint](tools/lint/README.md) — checker usage

---

## Credits

GregTech CEu Modern · GregTech: New Horizons (Space Elevator structure/logic, platline method —
structure re-expressed; the Space Elevator block textures are GTNH Intergalactic's, in GTCEu's connected-texture
format by `tools/textures/space_elevator.py`, originals in `tools/textures/gtnh_space_elevator/`; the Pico circuit components are GTNH's textures too,
`tools/textures/gtnh_pico/`; so are the Void Miner MK2 / MK3 casings and faces and their structures, `tools/textures/gtnh_void_miner/`, and the
DTPF's controller face, `tools/textures/gtnh_dtpf/`) · Ad Astra · KubeJS · LDLib · Jade · Curios ·
Extreme Reactors · EMI. Upstream pack: AllTheMods ATM-9.
