---
title: "AF9 Semiconductor Factory — Agent-Optimized Spec: Wafers, Silicon, SoCs, Chips, Lithography"
branch: "idea/recipes-for-lithography"
head_commit: "1058701 + working-tree power rework (uncommitted 2026-09-24: 4A own-tier MV→LuV, OC_PERFECT)"
minecraft: "1.20.1"
forge: "47.4.0"
gtceu: "7.2.0 (GregTech CEu Modern)"
kubejs: "2001.6.5-build.16"
af9_core: "0.1.0 (mod_id `af9`)"
gtceu_config: "enableCleanroom=true, cleanMultiblocks=false, enableMaintenance=true, highTierContent=false, orderedAssemblyLineItems=true"
status: " Implementation matches idea/recipes-for-lithography WORKING TREE (not just HEAD 1058701). Old committed doubling model (MUV 480 … LUV 7680, OC_NON_PERFECT) is SUPERSEDED by working-tree 4A-own-tier model (MUV 480 … LUV 122880, OC_PERFECT). Commit 9745aca Mk I-III / high_grade / premium system is also SUPERSEDED — do NOT use it."
agent_hint: "All exact IDs are in backticks. `gtceu:` = base GregTech item/machine/recipe-type. `kubejs:` = AF9 custom item. `af9:` = AF9 custom recipe ID (output namespace varies — see §6). NBT is load-bearing — use strongNBT."
---

# 0. How agents must read this doc

## 0.1 Conventions

- `ITEM` = registry ID, e.g. `gtceu:silicon_wafer`. Always use full `namespace:path`.
- `RECIPE` = recipe ID, e.g. `af9:ram_wafer_muv`. AF9 recipe IDs live under `af9:` even when they output `gtceu:` items.
- `MACHINE` = controller block ID, e.g. `gtceu:photolithography_line`.
- `TYPE` = recipe-type ID, e.g. `gtceu:lithography_muv`.
- EU/t = GregTech volts × amps. `VA[LV]=30, VA[MV]=120, VA[HV]=480, VA[EV]=1920, VA[IV]=7680, VA[LuV]=30720`.
- Duration: `t` = ticks, 20t = 1s. `900t = 45s`.
- NBT wafers/chips: `{AF9Litho:{Node:<int>,Transistors:<int>}}`. Predicate `af9:litho_mode` = 0 plain, 1 MUV, 2 HUV, 3 EUV, 4 XUV, 5 LUV.
- `notConsumable` = catalyst / reticle / lens, not used up.
- `cleanroom(CLEANROOM)` = recipe only runs when cutter is inside a formed + powered `gtceu:cleanroom`. AF9 litho line itself needs NO cleanroom.

## 0.2 Source-of-truth files (idea/recipes-for-lithography WORKING TREE, 2026-09-24)

```text
kubejs/startup_scripts/gtceu/photolithography.js   # materials, items, 5 recipe types, multiblock shape (OC_PERFECT in working tree)
kubejs/server_scripts/mods/gtceu/photolithography.js # all AF9 recipes + removals (modes: 4A own-tier MV→LuV in working tree)
af9-core/src/main/java/com/af9/core/litho/LithoMode.java # 5 modes, eut()=VA[tier]*4, density, dieFactor, NBT TAG (working tree: HUV HV4A, EUV EV4A, XUV IV4A, LUV LuV4A)
af9-core/src/main/java/com/af9/core/machine/PhotolithographyLineMachine.java # LITHO_GATE, UI
af9-core/src/main/java/com/af9/core/machine/LithoRecipeLogic.java # wafer counters
kubejs/assets/gtceu/lang/en_us.json                 # machine + material display names, tooltips 0-9 (working-tree tooltips 5+7 = 4A-next-tier + perfect-OC)
kubejs/assets/gtceu/models/item/*.json              # ilc/ram/cpu/ulpic/lpic/simple_soc wafer+chip overrides (litho_mode predicate)
kubejs/assets/af9/models/item/litho/*.json + textures/item/litho/*.png # per-mode textures
```

> AGENT: if any doc contradicts these files, the files win. The old `high_grade` / `premium` / `Mk I-III + KrF + Twin-Stage` design from `9745aca` is dead. Do not invent `kubejs:ram_wafer_high_grade`, `af9:ram_wafer_mk1`, `krf_excimer_laser_module` logic — those IDs/textures still exist on disk as leftovers but are NOT the current recipe path.

## 0.3 One-paragraph mental model

Real fab: quartz → MG-Si → ultra-pure polysilicon → Czochralski boule → diamond-wire wafers → RCA clean + CMP → repeat 100s of times: HMDS prime → resist coat → bake → expose through reticle → bake → develop → etch → implant → deposit → CMP. Wafer → probe → dice → package → PCB. In AF9/GregTech 1.20.1 this is compressed to: EBF boule → cutter blank wafer → Photolithography Line (reticle + 5 chemistries) → NBT wafer → cutter dies → circuit assembler. Reticles are the Minecraft reticle/mask, chemistries are HMDS/photoresist/TMAH fluids, the stepper is the multiblock, dies-per-wafer scaling is the NBT math.

---

# 1. Real silicon wafer manufacturing (reference — no Minecraft)

## 1.1 Stage 0 — Quartz mining

- Input: high-purity quartz/quartzite lumps `SiO2` (>98% SiO2, low B/P), carbon reductants (coke, coal, charcoal, wood chips for bed permeability), graphite electrodes.
- Output: feed for arc furnace.
- Why it matters: B/P in quartz + charcoal sets floor for later purity. Solar/electronic grade needs low-B/P quartz.

## 1.2 Stage 1 — Metallurgical-grade silicon (carbothermic reduction)

- Machine (real): open/semi-open submerged electric arc furnace, 3-phase graphite electrodes.
- Chemistry:

```text
Low-T (~1600C):  2C + SiO(g) -> SiC + CO(g)
High-T (~1800-2000C): SiO2 + SiC -> 3SiO(g) + CO(g)
                      SiO(g) + SiC -> 2Si(l) + CO(g)
Net: SiO2 + 2C -> Si(l) + 2CO(g)
```

- Key numbers: melt point Si **1414C**, tap ~1650-1700C, **11-13 MWh / ton Si**, ~2.5-3t quartz + ~1.5t C per 1t Si, ~100kg electrode / t Si.
- Output: MG-Si 98-99% (2N). Typical impurities: 0.5% Fe, 0.4% Al, 100-400ppm Ca/Cr/Mg/Mn/Ti, 20-40ppm B/P/Cu.
- Refine: O2 lancing, slagging (CaO-Al2O3-SiO2), ladle treatment.
- Byproducts: CO, CO2, SiO → microsilica fume, SiC dross, slag.
- Failures: SiO loss (poor permeability), SiC inclusions, Al/Ca/Fe inclusions, CO hazard.
- Minecraft map: `gtceu:silicon_dust` smelt/EBF step. GregTech compresses this entire stage into dust → EBF boule.

## 1.3 Stage 2 — Purification to electronic-grade polysilicon (9N-11N)

### Siemens process (dominant, >90% EGS)

1. Hydrochlorination, fluidized bed ~300-350C: `Si + 3HCl -> SiHCl3 + H2` (TCS = trichlorosilane, bp 31.8C). Side: `SiCl4` (STC, bp 57.6C), `SiH2Cl2` (DCS, bp 8.3C).
2. Fractional distillation to ppb-ppt (BCl3 bp 12.6C is hard — multi-column).
3. CVD on inverted-U slim rods, bell-jar, **1050-1175C (typ. 1080-1100C)**: `SiHCl3 + H2 -> Si(s) + 3HCl(g)`. 200-300h, grow to 150-200mm dia. Break to chunks for CZ.
4. Closed loop: `SiCl4 + H2 -> SiHCl3 + HCl`, `3SiCl4 + 2H2 + Si -> 4SiHCl3`.
- Energy: historic 80-120 kWh/kg, modern closed-loop 45-65 kWh/kg.
- Purity: **9N-11N = 99.9999999-99.999999999%**, donors <0.02 ppba, C <0.1 ppma.
- Alternative: Fluidized Bed (FBR) `SiH4 -> Si + 2H2` at 600-700C, 10-20 kWh/kg, granular beads, slightly lower purity.

### Minecraft map

No Siemens unit in GTCEuM 1.20.1. AF9 does NOT simulate it — EBF boule recipe stands in for it. Do not add TCS/STC chain unless extending beyond silicon (see §9).

## 1.4 Stage 3 — Crystal growth (Czochralski CZ, >90% wafers)

- Hardware: fused-quartz crucible `SiO2` in graphite susceptor + heaters, steel chamber, vacuum + **Ar purge 10-30 mbar, 50-150 slpm**.
- Sequence: charge → pump/purge → melt ~1500C superheat → seed dip → Dash neck (2-4mm, fast pull kills dislocations) → shoulder → body → tail → cool.
- Numbers: pull **0.5-2.5 mm/min** (300mm: 0.5-1.0, 200mm: 0.8-1.5), crystal 5-20 rpm vs crucible 5-15 rpm counter-rotated, MCZ magnet 0.2-0.4T for 200/300mm.
- Doping the melt: **B → p-type, P/As/Sb → n-type**. Segregation k0: B 0.8, P 0.35, As 0.3, Sb 0.023 → axial resistivity gradient.
- Contamination: Oi 5-18 ppma from crucible (`Si + SiO2 -> 2SiO(g)`), C <0.5 ppma from graphite.
- Sizes: 300mm boule >250kg, 2m long.
- Alternative: Float Zone (FZ) — no crucible, RF coil, Oi 10-100× lower, resistivity to >5000 ohm-cm, limited to ~75-200mm, for power/RF/detectors. No B-O LID.
- Anatomy: seed (Dash neck) → shoulder → body (prime) → tail (dislocated, high dopant) → heel residue. Yield target >80%.
- Failures: loss of zero-dislocation (twinning/slip), swirl/COPs/voids (V/G control), O/dopant striations, crucible devitrification particles.
- Minecraft map: `gtceu:electric_blast_furnace` boule recipes (§6.1). `gtceu:silicon_boule` = CZ boule, `gtceu:phosphorus_boule` = n-doped boule, etc.

## 1.5 Stage 4 — Wafering (ingot → wafer)

1. Cropping (diamond saw removes seed/shoulder/tail), OD grinding to SEMI dia ±0.3mm, Laue orientation to `<100> ±0.5°`.
2. Flat/notch: <200mm = primary + secondary flats encode type/orientation; ≥200mm = single notch on `<100>`.
3. Wire sawing: diamond wire (steel core 60-80µm + 8-15µm diamond, water coolant), 1000s wafers at once. Kerf 120-180µm. Legacy slurry (SiC + PEG) is slower/thicker.
4. SEMI M1 thickness: 100mm 525µm, 150mm 625µm, 200mm 725µm, 300mm 775µm ±20-25µm. As-cut + ~30µm damage/side.
5. Lapping (Al2O3 slurry), edge profiling (chamfer prevents chipping).
6. Types at this point: p-type B (0.5-50 ohm-cm CMOS), n-type P/As/Sb, intrinsic/FZ high-rho.
- Failures: wire marks, microcracks 10-20µm subsurface, TTV/bow/warp, edge chips.
- Minecraft map: `gtceu:cutter` boule → 16× blank wafer (§6.2). Kerf/lapping abstracted into fluid cost (lubricant/distilled/water).

## 1.6 Stage 5 — Finishing (etch → clean → polish → epi → metrology)

1. Etch damage removal: alkaline `KOH/NaOH` (20-30µm/side, matte) or acid `HNO3+HF` (`Si + 4HNO3 + 6HF -> H2SiF6 + 4NO2 + 4H2O`, glossy).
2. RCA clean (Werner Kern): **SC-1** `H2O:NH4OH:H2O2 5:1:1→40:1:1, 70-80C** (particles+organics) → rinse → dilute HF HF:H2O 1:50-1:100 (H-terminated) → **SC-2** `H2O:HCl:H2O2 6:1:1→160:4:1** (metals Fe/Cu/Ni/Zn/Na) → spin-rinse-dry. Piranha `H2SO4:H2O2` for heavy organics. Megasonic assist.
3. CMP polish: colloidal silica 10-100nm or ceria/alumina, pH 10-11 (KOH/NH4OH). Roughness <1nm Ra, GBIR <1µm, SFQR <0.2µm.
4. Epitaxy (optional epi wafer): CVD 1050-1150C in H2 (`SiHCl3/H2, SiH2Cl2/H2, SiH4`), 1-20µm p/p+, n/n+, buried layers, denuded zone + gettering.
5. Metrology: 4-point resistivity, FTIR Oi 1107cm⁻¹ / C 605cm⁻¹, µPCD lifetime (>1ms prime), thickness/TTV/bow/warp/flatness, laser particle counter, EPD/OISF/slip X-ray.
6. Grades: **Prime** (device spec), **Test** (certified particles, broader), **Monitor/Dummy/Mechanical** (process control only), **Reclaimed** (stripped + re-polished, monitor only).
- Minecraft map: distilled-water rinse + filter-casing FFU + `extreme_clean_dry_air` purge stand in for RCA/CMP/epi. No separate epi item — NBT node stands in for epi/prime distinction.

## 1.7 Chemical master list (real → AF9 fluid)

| Real chemical | Purpose | AF9 fluid (`gtceu:`) |
|---|---|---|
| `SiO2`, `Si`, `SiHCl3`, `SiH2Cl2`, `SiCl4`, `SiH4` | feed/poly/epi | abstracted into `silicon_dust` / boule (no separate fluids) |
| `HCl`, `H2`, `Ar`, `N2` | hydrochlorination, carrier, CZ blanket | `hydrochloric_acid`, `hydrogen`, `nitrogen` (used in HMDS chain), air |
| `HF`, `HNO3`, `NH4OH`, `H2O2`, `KOH` | etch + RCA + CMP pH | abstracted into `distilled_water` rinse + `tmah_developer` (TMAH is KOH-analogue, metal-ion-free) |
| HMDS `[(CH3)3Si]2NH` | adhesion promoter | `hexamethyldisilazane` → `hmds_vapor` |
| Novolac `(C7H6O)n` + DNQ `C10H6N2O` in xylene | positive resist | `novolac_resin` + `diazonaphthoquinone_dust` → `photoresist` |
| TMAH `(CH3)4NOH` 2.38% / 0.26N | developer (K+/Na+-free) | `tmah_developer` |
| CMP slurry colloidal `SiO2`/`CeO2` | planarization | `lubricant` (cutter) + `distilled_water` (rinse) |
| Compressed dry air over zeolite | purge | `extreme_clean_dry_air` |

---

# 2. Real lithography + FEOL/BEOL (reference)

## 2.1 Cleanroom (why)

0.1µm particle on 5nm wafer = ~20× feature = killer defect. Human sheds ~1e6 particles/min. Fab: ISO 3-4 ballroom + FOUP ISO 1-2 micro-environment, FFU/ULPA vertical laminar flow, bunny suits, ±0.1C, ±2% RH, AMC filters (acids/bases/organics poison resist).

| ISO | Particles/m³ ≥0.1µm | Typical use |
|---|---|---|
| ISO 1 | ≤10 | EUV scanner mini-env, FOUP interior target |
| ISO 3 | ≤1,000 | modern FEOL litho bay |
| ISO 4 | ≤10,000 | FEOL etch/diffusion bay |
| ISO 5 | ≤100,000 | gowning, sub-fab |

Minecraft map: `gtceu:cleanroom` multiblock (plascrete + `cleanroom_glass` + `filter_casing` roof) for high-tier cutters; AF9 litho line has built-in FFUs (`filter_casing`) so it needs NO cleanroom — tooltip 2 is load-bearing.

## 2.2 Full photolithography cycle (one mask layer, repeat 30-80×)

```text
RCA clean + dehydration bake → thermal oxidation (pad/screen oxide, 900-1200C) → HMDS prime vapor 120-150C
  → resist spin coat 1000-5000rpm + BARC/TARC → soft bake (PAB) 90-120C 60-90s
  → align + expose (4× reduction stepper/scanner) → post-exposure bake (PEB) 90-130C
  → develop (TMAH 0.26N puddle/spray) → hard bake 100-140C + O2 descum
  → overlay/CD check (pass → etch, fail → rework strip)
```

- Resist tones: **positive** (exposed dissolves; DNQ-novolac for g/i-line, CAR polymer+PAG+quencher for DUV/EUV — photon → H+ acid, PEB catalyzes 100s events) = standard logic. **Negative** (exposed stays, crosslinks; lift-off/MEMS) + NTD (CAR + n-butyl acetate for fine trenches).
- DUV resists: KrF 248nm, ArF 193nm, 0.5-2µm thick. EUV resists: <30-60nm (anti-collapse), CAR EUV + MOR Sn-oxo (higher 13.5nm absorption, lower blur/LWR).
- Exposure generations: contact (1:1, research) → proximity (10-20µm gap) → projection (4× stepper/scanner, production). **DUV 193nm ArF immersion** (water n=1.44, NA 1.35, pitch ~76-80nm single, ~38nm double-patterned). **EUV 13.5nm** (Sn-droplet + CO2 LPP 250-500W, all-reflective Mo/Si Bragg mirrors in vacuum, NA 0.33 → ~13nm half-pitch, NA 0.55 High-NA → ~8nm).
- Masks/reticles: 6" quartz 152mm. DUV chrome/MoSi + OPC + PSM. EUV reflective (LTEM + 40 Mo/Si bilayers + Ru cap + TaN absorber). Pellicles: DUV fluoropolymer, EUV <50nm SiNx/CNT ~90% transmission, >600C tolerant.
- Developers: TMAH 0.26N (K+/Na+ forbidden — shifts Vt), NTD n-butyl acetate. Dark erosion ~10%.
- Metrology: overlay ≤1/5 min pitch (7nm: ≤3.6nm, Box-in-Box/AIM/uDBO), CD by CD-SEM/scatterometry. Reworkable before etch.

Minecraft map (AF9, one recipe = one full cycle): `hmds_vapor` prime → `photoresist` coat → soft bake (cupronickel coil) → UV expose (lamp + reticle + lens) → PEB → `tmah_developer` develop → `distilled_water` rinse → hard bake. All five fluids in one recipe, 900t fixed.

## 2.3 Pattern transfer — etch

- Dry (anisotropic, production): RIE (capacitive, -100 to -1000V bias), ICP (high density low damage), DRIE Bosch (`SF6` etch / `C4F8` passivate cycles, 10-500µm trenches, MEMS/TSV). Gases: `CF4/O2`, `CHF3/O2`, `C4F8`, `SF6`, `NF3`, `HBr/Cl2/O2`, `BCl3/Cl2`, `O2` strip.
- Wet (isotropic/selective, cheap): `HF` (`SiO2+6HF->H2SiF6+2H2O`), BOE `NH4F:HF` 6:1/7:1, `KOH` anisotropic Si {100}/{110} vs {111} 54.7° (MEMS, K+ banned FEOL), TMAH (CMOS-compatible KOH analogue + developer), hot `H3PO4` 150-180C (`Si3N4` selective), piranha/SC-1/SC-2.
- <50nm anisotropic lines = dry only (wet undercuts).
- Minecraft map: abstracted — no separate etch recipe. `laser_engraver` reticle-writing + cutter dicing stand in for etch. Future extension could add `chemical_bath` HF/BOE step (§9).

## 2.4 Doping — implant (dominant) vs diffusion (legacy/power)

- Species: p-type **B, BF2, In** (holes), n-type **P, As, Sb** (electrons). Must sit substitutionally → anneal.
- Implant: mass-separated beam 1keV-3MeV (depth), dose 1e11-1e16 cm⁻² (concentration), 7° tilt anti-channeling, screen oxide. Amorphizes → anneal: furnace 800-1100C (old, too much diffusion), RTP spike 1000-1100C 1-10s (standard), flash/laser ms-µs (ultra-shallow), SPE regrowth, C/N co-implant anti-TED.
- Diffusion: `POCl3` n / `BBr3/B2O3` p, 850-1150C hours, isotropic, deep junctions — now only power/CCD/photodiode/well drive.
- Minecraft map: `phosphorus_boule` (P-doped) stands in for n-doping. No implant machine — NBT node stands in for junction scaling.

## 2.5 Deposition + planarization

- PVD/sputter (Ar + Al/Cu/Ti/Ta/W targets, line-of-sight) → CVD LPCVD (dense conformal 770-850C `Si3N4`, TEOS `SiO2`, `WF6+H2→W`) → PECVD (<400C over metal) → HDP/APCVD/SACVD (gap fill STI/PMD/IMD) → ALD (atomic `HfO2/ZrO2/Al2O3` high-k, 0.5-1Å/cycle, 3D perfect) → epi (crystalline Si/SiGe/Si:P raised S/D).
- Dielectrics: `SiO2` k~3.9, `Si3N4` k~7 (barrier/spacer), high-k `HfO2` (EOT <1nm since 45nm), low-k SiCOH/ULK k~2.4-2.7 (IMD, cuts RC), `SiC/SiCN` cap.
- Metals: old Al-1%Si-0.5%Cu PVD+RIE → now Cu damascene (trench + plate + CMP, no volatile Cu etch product). Liners Ta/TaN, Ti/TiN, W plugs, Co/NiSi silicide, Co/Ru sub-10nm, Ru/Mo future.
- CMP: nano-abrasive silica/ceria/alumina + `H2O2` + citric/oxalic/glycine + BTA inhibitor. Oxide pH 10-12, W pH 2-4, Cu acidic + BTA. Defects: dishing/erosion/corrosion/scratches. No multilayer without CMP.
- Minecraft map: `soldering_alloy` + `lubricant` + plate/casing recipes stand in for BEOL. No damascene simulation.

## 2.6 Repetition → transistor + interconnect

One litho-etch-dep-implant-CMP loop = 10-30 steps. 30-80 masks = **400-1400 total steps, 2-4 months** fab time.

- FEOL (transistors in Si): STI etch + HDP + CMP → wells → fin etch (SADP/SAQP) or nanosheet Si/SiGe + release → dummy poly → LDD + SiN spacer → raised S/D epi (SiGe pMOS / Si:P nMOS) → ILD + CMP → RMG (high-k `HfO2` ALD + TiN/TaN/TiAl + W/Co) → contacts.
- FinFET (6-8nm fin, gate wraps 3 sides) → GAA nanosheet (2-4 stacked 5-12nm sheets, gate wraps 4 sides, inner SiN spacer; Samsung 3GAE first, TSMC N2 next).
- MOL+BEOL (wires above): 10-15 dual-damascene levels (low-k IMD → via+trench etch → TaN/Ru barrier → Cu seed → plate → CMP). Pitch doubles upward. Top thick Al for power. Passivation `SiN`/polyimide.
- Metrology/inspection/test: CD-SEM/scatterometry/ellipsometry/XRR/AFM/TEM, KLA patterned + Surfscan unpatterned + e-beam review, scribe-line PCM (Van der Pauw, Kelvin, comb-serpentine, C-V, ring oscillator, SRAM vehicle), wafer sort probe + binning (Bin1 good, speed/leakage bins, auto I-PAT outlier removal).

## 2.7 Node names (what 7/5/3nm really mean)

Marketing density label, NOT gate length (decoupled since ~22nm; Intel 10nm ≈ TSMC/Samsung 7nm). Real metrics: CPP (contacted gate pitch), MMP (min metal pitch), fin pitch, SRAM area, MTr/mm².

| Label | CPP | MMP | Gate (phys) | SRAM 6T | Device |
|---|---|---|---|---|---|
| 7nm | 54-57nm | 36-40nm | ~18-20nm | ~0.027µm² | FinFET |
| 5nm | ~45-48nm | ~28-30nm | ~14-16nm | smaller | FinFET |
| 3nm | ~42-45nm | ~21-24nm | ~12-16nm | smaller | Samsung GAA / TSMC N3 FinFET-last → N2 GAA |

DUV multi-patterning (LELE overlay-risk, SADP/SAQP pitch-walk, 3-4 masks/layer, +30-40 steps) vs EUV single (replaces 3-4 DUV masks, -20-25% masks, stochastic/tip-to-tip/pellicle/source cost). Production = mix-and-match (EUV critical, DUVi rest). High-NA 0.55 needed <3nm single.

AF9 nodes (350/250/200/100/50nm) are 1990s-2000s real nodes (350nm ≈ Pentium II era) — deliberately pre-EUV. Names MUV/HUV/EUV/XUV/LUV are AF9-internal (real EUV is 13.5nm, not 200nm — see pitfalls §10.5).

---

# 3. Chips in general + System-on-Chips (glossary for agents)

Hierarchy: `transistor > gate/cell > block/IP > die > packaged IC / chiplet > SiP/module > PCB assembly > system`.

| Term | What (1 sentence) | How made (1 sentence) | How used/connected (1 sentence) | Example |
|---|---|---|---|---|
| Transistor | Electrically-controlled switch/amplifier (source/drain + gate). | Litho + etch + implant + gate-stack (now FinFET/GAA). | Wired by BEOL Cu stack, switches billions/s. | TSMC N2 GAA device |
| Logic gate / cell | Transistor group for NAND/NOR/flop. | Standard-cell library + litho as part of wafer flow. | Tiled by synthesis, wired by BEOL. | ARM Cortex cell |
| Wafer | 200/300mm mono-Si disk with 100s identical dies. | CZ boule → wire-saw → lap/polish → 100s fab steps. | Probed at sort, thinned, diced. | 300mm logic wafer ~500 phone SoCs |
| Die / bare die | Singulated Si rectangle, unpackaged circuit. | Front-end fab + backgrind 50-150µm + blade/laser/stealth dice. | Die-attach + wire-bond/flip-chip, or KGD for SiP. | Bare GPU die |
| IC | Full circuit on one die, normally packaged. | Monolithic CMOS/BiCMOS/BCD foundry flow. | Soldered/socketed, power+signal via pins/balls. | Timer, op-amp |
| ASIC | Custom IC for one product/function. | Full-custom/standard-cell tapeout, high NRE amortized. | Single-function accel/controller in host. | Miner hash ASIC |
| ASSP | Function-specific catalog IC for many customers. | ASIC flow productized by vendor. | Drop-in via USB/Ethernet/PCIe. | USB3 controller |
| FPGA | Reconfigurable LUT+DSP+SRAM+routing IC. | Standard CMOS + SRAM/flash config cells, field-programmed. | Board-mounted, JTAG/SPI bitstream. | Xilinx Kintex prototype |
| MCU | All-in-one mini-computer (CPU+flash+SRAM+ADC/GPIO). | Mature low-leakage embedded-flash process. | Control board, bare-metal/RTOS, GPIO/ADC/PWM/I2C/SPI/UART. | STM32, PIC |
| CPU | General-purpose core, needs external memory, MMU for OS. | Leading-node logic + large cache. | Socketed with DDR + PCIe/IO, runs OS. | Desktop/server CPU |
| GPU | Many-core parallel (graphics/matrix). | Leading-node + shader/CUDA + HBM/GDDR ctrl. | PCIe card or SoC block, driver-fed. | Gaming/AI GPU |
| NPU / AI accel | Matrix-multiply engine for neural nets. | Systolic/tensor + SRAM, SoC block or standalone. | On-die bus or PCIe + DMA. | Phone NPU, datacenter accel |
| SRAM | Fast 6T volatile on-chip cache. | Logic process dense bitcells alongside CPU. | Direct to pipeline, core V/speed. | 30MB L3 |
| DRAM | Dense 1T1C volatile main memory, needs refresh. | DRAM process (trench/stacked caps) → chips/modules. | DDR PHY, short traces to SoC. | DDR5 DIMM, LPDDR5 |
| NAND 2D vs 3D | Non-volatile charge-trap; 2D planar vs 3D 100+ stacked layers. | Oxide/nitride stack + high-aspect etch + staircase contacts. | ONFI/Toggle or NVMe/PCIe as SSD/UFS/eMMC. | 3D TLC SSD |
| HBM | 3D DRAM stack on logic base for TB/s. | Thinned <50µm + TSV + microbump/hybrid bond on interposer. | Co-packaged with GPU via 1024-bit IF. | HBM3 + AI GPU |
| Power/analog/RF | Non-digital: PMIC, ADC/DAC, amp, RF PA/filter. | BCD/SOI/SiGe/GaN/SiC/RF-CMOS. | Around digital, clean power / antenna. | Phone PMIC + 5G FEM |
| Sensor/MEMS | Micro-mechanical + CMOS (accel/gyro/pressure/mic). | Bulk/surface etch + wafer bond/cap + CMOS readout. | I2C/SPI/I3S, vent/hole. | Bosch accel |
| Imager / photonics | CIS pixel array (visible) / PIC waveguides+lasers (comms). | Opto-CMOS + microlens/CF + BSI / SOI + III-V bond. | MIPI CSI under lens / fiber + driver. | Phone camera, O-transceiver |
| SoC | Whole computer on one die (CPU+GPU+NPU+ISP+modem+DDR ctrl+IO+security). | One tapeout, licensed IP + NoC fabric, leading node. | Flip-chip BGA + LPDDR/power/flash around it. | Phone flagship SoC |
| SoC blocks | ISP (camera), modem (4G/5G/WiFi), DDR ctrl, IO (PCIe/USB/UART). | Hard/soft IP in same die. | AXI/NoC/DMA inside, MIPI/DDR PHY/SerDes outside. | 5G + ISP in phone SoC |
| SiP / module | Multi-die + passives in one package as system. | Die-attach + wire/flip + SMT on laminate, molded. | One SMT component, saves area/RF path. | BT+MCU+antenna FEM |
| Chiplet | System split into function-optimized dies, recombined in package. | Each on best node (logic 3nm + IO 28nm + HBM), joined by adv. packaging. | UCIe/BoW over interposer/bridge/microbumps. | Server CPU compute+IO |
| Interposer | Wiring platform between chiplets and substrate. | Si (BEOL+TSV) / organic laminate / EMIB bridge. | Under dies, RDL + TSV/C4 down. | Si interposer under GPU+HBM |
| TSV/microbump/hybrid/RDL | Vertical Cu via / solder pillar / Cu-Cu fusion / rewiring metal. | Etch+fill+CMP / plate+reflow / sputter+plate. | Stack memory, fan out pads to balls. | HBM TSV, WLCSP RDL |
| Thinning | Backgrind 775→50-150µm for thin/stack. | Grind + stress-relief etch/polish with tape. | Enables low-profile/TSV/stack. | 16-die NAND stack |
| Dicing | Singulation (blade diamond / laser ablate / stealth crack+expand). | Taped wafer post-sort, kerf loss in scribe lines. | Singulated dies to attach. | Blade MCU, stealth memory |
| Die attach | Bond die to leadframe/substrate (thermal+mech+elec). | Epoxy/DAF/eutectic/Ag-sinter + cure. | Heat+ground path, base for bond/flip. | MOSFET to Cu leadframe |
| Wire bond vs flip-chip | Edge-pad wires (Au/Cu/Al, cheap/low-IO) vs face-down bumps (fast/high-IO). | Thermosonic/wedge one-by-one vs wafer-bump + reflow + underfill. | QFN/BGA wire vs BGA flip for CPU/GPU. | QFN MCU vs BGA SoC |
| Substrate/leadframe/mold | Laminate router / stamped metal / epoxy protector. | BT/ABF layup+drill+plate / etch+stamp / transfer mold. | Carrier to PCB + heat spread. | ABF under server CPU |
| Packages | QFN (side pads+thermal) / BGA (balls) / LGA (lands) / WLCSP (die-size) / 2.5D-3D / PoP (stacked pkg). | Attach + wire/flip + mold/ball; WLCSP at wafer level pre-dice. | Reflow SMT, by IO/size/cost/thermal. | QFN sensor, BGA SoC, HBM 2.5D, PoP SoC+DRAM |
| Test | Sort/probe (wafer) → final (pkg) → burn-in (infant mortality) → binning (speed/power grades). | Probe cards + ATE + chambers at V/T corners. | Emap bad dies, ship KGD/graded parts. | i7 vs i5 bin, DRAM burn-in |
| PCB | FR4 + Cu traces + vias, SMT reflow. | Laminate etch/drill/plate + paste + pick-place + reflow. | Power + SE/diff signals SoC↔RAM↔power↔conn. | Motherboard + DIMM + PCIe |
| IO | RDL/microbumps (on-pkg fine) / PCIe/DDR (fast off-chip) / SPI/I2C/UART (slow control). | Pkg-formed / PHY+ctrl IP in dies. | RDL <1mm, DDR SoC-RAM, PCIe CPU-GPU/SSD, SPI/I2C sensor/flash. | SSD + DIMM + SPI flash + I2C sensor |

GT mapping: `gtceu:ilc_chip` ≈ jellybean logic, `gtceu:ram_chip` ≈ SRAM/DRAM cell array, `gtceu:cpu_chip` ≈ simple CPU, `gtceu:ulpic/lpic` ≈ low-power / high-density PIC variants, `gtceu:simple_soc` ≈ MCU-class SoC (CPU+RAM+IO on one `simple_soc_wafer`). Higher GT tiers (`mpic/hpic/uhpic/nano_cpu/qbit_cpu/advanced_soc/highly_advanced_soc/crystal_*`) are NOT in AF9 litho scope — they use base GT laser/chemical paths (§9).

---

# 4. GregTech CEu Modern 1.20.1 ground truth (base mod, no AF9)

> Verified against `GregTechCEu/GregTech-Modern 1.20.1 v7.5.3`, `GTValues.java`, `GTItems.java`, `GTMachines.java`, `GTMultiMachines.java`, `GTRecipeTypes.java`, `CircuitRecipes.java`, `CleanroomMachine.java`. Flag `UNCERTAIN` where JEI must confirm.

## 4.1 Voltage + circuit tiers

- Volts: ULV 7, LV 30, MV 120, HV 480, EV 1920, IV 7680, LuV 30720, ZPM 122880, UV 491520, UHV 1966080, UEV 7864320.
- Circuit tiers (no Primitive tier; ULV = vacuum tube):

```text
ULV: vacuum_tube, nand_chip
LV: basic_electronic_circuit, basic_integrated_circuit, microchip_processor
MV: good_electronic_circuit, good_integrated_circuit, micro_processor
HV: advanced_integrated_circuit, micro_processor_assembly, nano_processor
EV: micro_processor_computer, nano_processor_assembly, quantum_processor
IV: micro_processor_mainframe, nano_processor_computer, quantum_processor_assembly, crystal_processor
LuV: nano_processor_mainframe, quantum_processor_computer, crystal_processor_assembly, wetware_processor
ZPM: quantum_processor_mainframe, crystal_processor_computer, wetware_processor_assembly
UV: crystal_processor_mainframe, wetware_processor_computer
UHV: wetware_processor_mainframe
```

No UEV+ circuits. UHV Wetware Mainframe is top.

## 4.2 Base items (exact IDs)

Boules: `gtceu:silicon_boule`, `gtceu:phosphorus_boule`, `gtceu:naquadah_boule`, `gtceu:neutronium_boule`.
Blank wafers: `gtceu:silicon_wafer`, `gtceu:phosphorus_wafer`, `gtceu:naquadah_wafer`, `gtceu:neutronium_wafer`.
AF9-scope engraved wafers: `gtceu:ilc_wafer`, `gtceu:ram_wafer`, `gtceu:cpu_wafer`, `gtceu:ulpic_wafer`, `gtceu:lpic_wafer`, `gtceu:simple_soc_wafer`.
AF9-scope chips: `gtceu:ilc_chip`, `gtceu:ram_chip`, `gtceu:cpu_chip`, `gtceu:ulpic_chip`, `gtceu:lpic_chip`, `gtceu:simple_soc` (NOTE: NO `_chip` suffix).
Out-of-scope (base GT only): `gtceu:mpic_wafer/chip`, `gtceu:soc_wafer` / `gtceu:soc`, `gtceu:nand_memory_wafer/chip`, `gtceu:nor_memory_wafer/chip`, `gtceu:hpic_wafer/chip`, `gtceu:uhpic_wafer/chip`, `gtceu:nano_cpu_wafer/chip`, `gtceu:qbit_cpu_wafer/chip`, `gtceu:advanced_soc_wafer` / `gtceu:advanced_soc`, `gtceu:highly_advanced_soc_wafer` / `gtceu:highly_advanced_soc`, `gtceu:crystal_cpu/soc`, `gtceu:raw_crystal_chip`, `gtceu:engraved_crystal_chip`, etc.
Boards: `gtceu:resin/phenolic/plastic/epoxy/fiber_reinforced_circuit_board` + `_printed_` variants, `gtceu:multilayer_fiber_reinforced_circuit_board` (UNCERTAIN exact string — verify JEI, constant `MULTILAYER_FIBER_BOARD`), `gtceu:wetware_circuit_board` / `_printed_`.
Components: `gtceu:vacuum_tube`, `glass_tube`, `resistor/capacitor/transistor/diode/inductor`, `smd_*`, `advanced_smd_*`, `carbon_fibers`, `petri_dish`, `stem_cells`, `neuro_processing_unit`, `quantum_eye/star`, `gravi_star`.
Lenses: `gtceu:<color>_glass_lens` (recipe tag `#forge:lenses/<color>` in AF9 scripts; base tag helper is `lenses/...` — namespace `forge` vs `c` UNCERTAIN on 1.20.1, follow AF9 scripts).
Fluids: `gtceu:sodium_persulfate`, `gtceu:iron_iii_chloride` (code `Iron3Chloride`), `gtceu:sulfuric_acid`, `gtceu:soldering_alloy`, `gtceu:sterilized_growth_medium` (constant `SterileGrowthMedium` — note `sterilized_`), `gtceu:mutagen`, `gtceu:bacterial_sludge`, `gtceu:distilled_water`, `gtceu:lubricant`, water. **NO `photoresist` in base — AF9 adds it.**

## 4.3 Base machines (exact IDs)

Recipe types: `gtceu:cutter`, `gtceu:laser_engraver`, `gtceu:chemical_reactor`, `gtceu:electric_blast_furnace`, `gtceu:circuit_assembler`, `gtceu:assembler`, `gtceu:autoclave`, `gtceu:chemical_bath`, `gtceu:forming_press`, `gtceu:fluid_solidifier`, `gtceu:assembly_line`, `gtceu:large_chemical_reactor`, `gtceu:research_station`, `gtceu:scanner`.
Singleblocks are tiered: `gtceu:<tier>_<name>` e.g. `gtceu:lv_cutter`, `gtceu:mv_cutter`, `gtceu:hv_laser_engraver`, `gtceu:ev_chemical_reactor`, `gtceu:hv_circuit_assembler`.
Multiblocks: `gtceu:cleanroom`, `gtceu:electric_blast_furnace`, `gtceu:large_chemical_reactor`, `gtceu:assembly_line`.
NO `gtceu:crystallizer`, NO EUV/lithography machine in base (uses `autoclave` + EBF instead).

Canonical base chain: `EBF boule → cutter wafer → laser_engraver etched wafer → cutter chip / chemical_reactor upgrade → assembler SMD → chemical_reactor/bath board → circuit_assembler / assembly_line circuit`.

Lens map (base, kept by AF9): red→ILC, green→RAM, light_blue→CPU, blue→ULPIC (+ crystal SoC), orange→LPIC, cyan→Simple SoC, gray→NAND, pink→NOR, brown→MPIC/PIC, yellow→SoC, purple→ASoC, black→HASoC, lime→Crystal CPU.

## 4.4 Cleanroom (base)

- Controller `gtceu:cleanroom`, dummy recipe type. Hollow box: walls `gtceu:plascrete` / `gtceu:cleanroom_glass`, floor `#gtceu:cleanroom_floors`, roof ring `gtceu:filter_casing` (normal) or `gtceu:sterilizing_filter_casing` (sterile — exact ID from `CleanroomFilterType.java`, NOT `filter_casing_sterile`), 1-3 energy hatches, ≤8 iron doors, ≤30 passthrough hatches. Bounds `MIN_RADIUS=2, MAX_RADIUS=7, MIN_DEPTH=3, MAX_DEPTH=14`.
- Recipes carry `.cleanroom(CLEANROOM)` or `STERILE_CLEANROOM` (only `data_module` needs sterile in base). Machine must sit inside formed + powered volume.
- Pack config: `enableCleanroom:true`, `cleanMultiblocks:false`. Do not disable — only `lpic` cutting needs it in AF9 scope, and disabling hides real gating.

## 4.5 What base GT does NOT have (why AF9 exists)

- No EUV/UV lithography machine, no `photoresist` chemistry, no `crystallizer` singleblock.
- SoC items DO exist (`simple_soc/soc/advanced_soc/highly_advanced_soc/crystal_soc` + wafers) — the gap is the process, not the item.
- Only `data_module` needs sterile cleanroom — custom sterile wafers must add `STERILE_CLEANROOM` explicitly.
- UNCERTAIN in JEI: exact `multilayer_...` board strings, `forge` vs `c` lens tag, whether JEI shows water/lubricant boule-cutting variants (7.x source shows none — AF9 cutter adds them explicitly).

---

# 5. AF9 Photolithography Line — current implementation (HEAD 1058701)

## 5.1 Machine + recipe types

| Thing | ID | Display name |
|---|---|---|
| Controller | `gtceu:photolithography_line` | `block.gtceu.photolithography_line` = Photolithography Line |
| Type MUV | `gtceu:lithography_muv` | Lithography MUV (350 nm) |
| Type HUV | `gtceu:lithography_huv` | Lithography HUV (250 nm) |
| Type EUV | `gtceu:lithography_euv` | Lithography EUV (200 nm) |
| Type XUV | `gtceu:lithography_xuv` | Lithography XUV (100 nm) |
| Type LUV | `gtceu:lithography_luv` | Lithography LUV (50 nm) |

Tooltips (`kubejs/assets/gtceu/lang/en_us.json`, working tree):

```text
0 Prints chip wafers onto Silicon Wafers through a reticle (not consumed).
1 Full cell: HMDS prime > resist coat > bake > UV exposure > bake > develop > rinse.
2 Built-in fan filter units: no Cleanroom needed.
3 Five UV modes (switch in the controller), finest last:
4   MUV 350nm HUV 250nm EUV 200nm XUV 100nm LUV 50nm
5 Each finer mode: 4A of the next voltage tier, 1.5x chemicals, much denser transistors and more dies per wafer.
6 Wafers remember their mode: node and transistor count show in the tooltip and change the texture.
7 Power: exactly 2 Energy Hatches (4A): MV MUV, HV HUV, EV EUV, IV XUV, LuV LUV. Perfect overclock with stronger hatches.
8 Needs 1 Maintenance Hatch and at least 1 Fluid Input Hatch (5 fluids per recipe).
9 Wafers go in and out at the cassette station: the face with the controller.
```

Java: `PhotolithographyLineMachine` (`LITHO_GATE` power gate + UI), `LithoRecipeLogic` (per-mode counters), `LithoMode` enum (numbers must match KubeJS). Modifiers (working tree): `LITHO_GATE + OC_PERFECT` (stronger hatches perfect-overclock; old HEAD was `OC_NON_PERFECT`). Appearance `CASING_STAINLESS_CLEAN` + `gcym/large_engraving_laser`. Sound electrolyzer, lens overlay, arrow progress.

## 5.2 Structure — 3×3×20, back (lamp) → front (controller)

Each aisle = bottom → middle → top rows. From `startup_scripts/gtceu/photolithography.js`:

```text
Stepper (exposure tool):
 CCC / CLC / CCC  # UV light source / illuminator (L=puple_lamp)
 CCC / CRC / CCC  # reticle stage (R=stainless_steel_gearbox)
 CCC / WTW / CCC  # projection lens ×3 (W=cleanroom_glass, T=tempered_glass)
 CCC / WTW / CCC
 CCC / WTW / CCC
 CRC / WRW / CCC  # wafer XY stage
Track (coater/developer):
 CCC / CRC / CCC  # track<->stepper interface
 CCC / CRC / CFC  # transfer robot (F=filter_casing FFU)
 CCC / CHC / CFC  # hard bake (H=cupronickel_coil hot plate)
 CSC / WXW / CPC  # developer rinse (S=steel_gearbox, X=inert_casing, P=ptfe_pipe)
 CSC / WXW / CPC  # developer TMAH puddle
 CCC / CKC / CFC  # chill plate (K=frostproof)
 CCC / CHC / CFC  # post-exposure bake
 CCC / CHC / CFC  # soft bake
 CSC / WXW / CPC  # spin coater
 CSC / WXW / CPC  # spin coater resist dispense
 CCC / CKC / CFC  # chill plate
 CPC / WHW / CFC  # HMDS vapour prime oven
 CCC / CRC / CFC  # transfer robot
 III / IMI / CCC  # cassette station + controller (M=controller, I=clean_machine_casing or 1-2 IMPORT/EXPORT_ITEMS)
```

Predicates: `C` = `clean_machine_casing` min 100 OR exactly 2 `INPUT_ENERGY` OR ≥1 `IMPORT_FLUIDS` OR exactly 1 `MAINTENANCE`. `I` = `clean_machine_casing` OR 1-2 item import/export. `F` = `filter_casing` (FFU — reason for no cleanroom). `R` = stainless gearbox, `S` = steel gearbox, `X` = inert casing (PTFE-lined cups), `P` = ptfe pipe casing, `H` = cupronickel coil, `K` = frostproof, `T` = tempered glass (lens), `W` = cleanroom glass, `L` = purple lamp. `M` = controller.

> AGENT build rules: exactly 2 energy hatches (4A total — LITHO_GATE blocks otherwise), exactly 1 maintenance hatch, ≥1 fluid import (5 fluids/recipe — use 2-3 for throughput), 1-2 item import + 1-2 export at cassette face. Hatches must match mode voltage (§5.3). No cleanroom.

## 5.3 Five UV modes (numbers are load-bearing — WORKING TREE: 4A own-tier)

From `LithoMode.java` + server recipes (working tree). `EUt = VA[hatchTier] × 4`. Duration always `900t`. Chemicals scale `1.5^index`. Perfect overclock with stronger hatches (`OC_PERFECT`).

| Mode | Node | Hatch tier | Amps | EU/t (effective) | Min hatches | Density `(350/node)²` | Die factor `√(350/node)` |
|---|---|---|---|---|---|---|---|
| `muv` | 350nm | MV | 4A | 480 | 2× MV 2A | 1.0× | 1.0× |
| `huv` | 250nm | HV | 4A | 1920 | 2× HV 2A | 1.96× | ~1.183× |
| `euv` | 200nm | EV | 4A | 7680 | 2× EV 2A | 3.0625× | ~1.323× |
| `xuv` | 100nm | IV | 4A | 30720 | 2× IV 2A | 12.25× | ~1.871× |
| `luv` | 50nm | LuV | 4A | 122880 | 2× LuV 2A | 49.0× | ~2.646× |

> OLD HEAD (1058701 committed, SUPERSEDED): HUV HV 2A 960, EUV HV 4A 1920, XUV EV 2A 3840, LUV EV 4A 7680 with `OC_NON_PERFECT`. Do NOT use — working tree is 4A own-tier MV→LuV with `OC_PERFECT`.

Chemicals per recipe (mB, `Math.round(base × 1.5^index)`):

| Mode | `hmds_vapor` | `photoresist` | `tmah_developer` | `distilled_water` | `extreme_clean_dry_air` |
|---|---|---|---|---|---|
| muv | 40 | 100 | 200 | 1000 | 1000 |
| huv | 60 | 150 | 300 | 1500 | 1500 |
| euv | 90 | 225 | 450 | 2250 | 2250 |
| xuv | 135 | 338 | 675 | 3375 | 3375 |
| luv | 203 | 506 | 1013 | 5063 | 5063 |

Sequence comment in code: `HMDS prime → resist coat → soft bake → UV exposure → PEB → TMAH develop → DI rinse → hard bake`.

## 5.4 Items — names + IDs

Machine output namespace is `gtceu:`, reticles/masks are `kubejs:`, recipe IDs are `af9:`.

| ID | Display name | Stack | Notes |
|---|---|---|---|
| `kubejs:photomask_blank` | Chrome-on-Quartz Photomask Blank | 1 | Assembler quartzite+chromium+resist. Laser-written into reticles. |
| `kubejs:ilc_reticle` | ILC Reticle | 1 | `notConsumable`, red lens |
| `kubejs:ram_reticle` | RAM Reticle | 1 | green lens |
| `kubejs:cpu_reticle` | CPU Reticle | 1 | light_blue lens |
| `kubejs:ulpic_reticle` | ULPIC Reticle | 1 | blue lens |
| `kubejs:lpic_reticle` | LPIC Reticle | 1 | orange lens, cutter needs cleanroom |
| `kubejs:simple_soc_reticle` | Simple SoC Reticle | 1 | cyan lens, chip has NO `_chip` suffix |

Tooltip on all reticles: `Photomask for the Photolithography Line. Not consumed.`

Materials (`gtceu:`, from `en_us.json`):

```text
extreme_clean_dry_air = Extreme Clean Dry Air (gas)
trimethylchlorosilane = Trimethylchlorosilane (liquid, (CH3)3SiCl)
hexamethyldisilazane = Hexamethyldisilazane (liquid, ((CH3)3Si)2NH)
hmds_vapor = HMDS Vapor (gas, ((CH3)3Si)2NH(N2))
novolac_resin = Novolac Resin (liquid, (C7H6O)n)
diazonaphthoquinone = Diazonaphthoquinone (dust, C10H6N2O)
photoresist = Positive Photoresist (liquid)
tetramethylammonium_chloride = Tetramethylammonium Chloride (dust, (CH3)4NCl)
tmah_developer = TMAH Developer (liquid, (CH3)4NOH(H2O) 2.38%)
```

## 5.5 Chips in AF9 scope (transistors at MUV + cutter base)

`transistors` = per-die at 350nm (roughly what such a chip had on real 350nm). `cut` = dies per plain wafer. `cutEUt` = cutter EU/t. `lens` = reticle-writing lens. `engrave` = removed base recipe. `gtCut` = removed base cutter prefix. `cleanroom` = cutter needs cleanroom.

| id | wafer `gtceu:` | chip `gtceu:` | lens | engrave (removed) | gtCut (removed) | cut | cutEUt | cleanroom | MUV transistors |
|---|---|---|---|---|---|---|---|---|---|
| ilc | `ilc_wafer` | `ilc_chip` | red | `engrave_ilc_silicon` | `cut_ilc` | 8 | 64 | false | 50000 |
| ram | `ram_wafer` | `ram_chip` | green | `engrave_ram_silicon` | `cut_ram` | 32 | 96 | false | 16000000 |
| cpu | `cpu_wafer` | `cpu_chip` | light_blue | `engrave_cpu_silicon` | `cut_cpu` | 8 | 120 | false | 5500000 |
| ulpic | `ulpic_wafer` | `ulpic_chip` | blue | `engrave_ulpic_silicon` | `cut_ulpic` | 6 | 120 | false | 2000 |
| lpic | `lpic_wafer` | `lpic_chip` | orange | `engrave_lpic_silicon` | `cut_lpic` | 4 | 480 | **true** | 5000 |
| simple_soc | `simple_soc_wafer` | `simple_soc` | cyan | `engrave_ssoc_silicon` | `cut_ssoc` | 6 | 64 | false | 1000000 |

> TRAPS: `simple_soc` chip is `gtceu:simple_soc` (no `_chip`), wafer `gtceu:simple_soc_wafer`, reticle `kubejs:simple_soc_reticle`, but removed engraver is `engrave_ssoc_silicon` and removed cutter prefix `cut_ssoc`. Do not invent `engrave_simple_soc_silicon` / `simple_soc_chip`.

## 5.6 NBT + yields (computed — matches `lithoNbt()` + cutter code)

`Transistors = round(base × (350/node)²)`. Dies `= round(cut × √(350/node))`. Cutter copies wafer NBT to chips via `strongNBT` — plain wafers (quest rewards, other GT paths) still cut to base counts.

Dies per wafer:

| chip (base) | muv 350 | huv 250 | euv 200 | xuv 100 | luv 50 |
|---|---|---|---|---|---|
| ilc (8) | 8 | 9 | 11 | 15 | 21 |
| ram (32) | 32 | 38 | 42 | 60 | 85 |
| cpu (8) | 8 | 9 | 11 | 15 | 21 |
| ulpic (6) | 6 | 7 | 8 | 11 | 16 |
| lpic (4) | 4 | 5 | 5 | 7 | 11 |
| simple_soc (6) | 6 | 7 | 8 | 11 | 16 |

Transistors per die:

| chip | muv | huv (×1.96) | euv (×3.0625) | xuv (×12.25) | luv (×49) |
|---|---|---|---|---|---|
| ilc | 50000 | 98000 | 153125 | 612500 | 2450000 |
| ram | 16000000 | 31360000 | 49000000 | 196000000 | 784000000 |
| cpu | 5500000 | 10780000 | 16843750 | 67375000 | 269500000 |
| ulpic | 2000 | 3920 | 6125 | 24500 | 98000 |
| lpic | 5000 | 9800 | 15313 | 61250 | 245000 |
| simple_soc | 1000000 | 1960000 | 3062500 | 12250000 | 49000000 |

Example NBT: LUV RAM wafer = `{AF9Litho:{Node:50,Transistors:784000000}}`. Model predicate `af9:litho_mode` 5, gold tint, tooltip shows node + transistors (via `AF9Client`).

Cutter fluid variants (all share count above; `totalEU = 900 × cutEUt`; `lubricant clamp(totalEU/1280,1,250) 900t` / `distilled clamp(totalEU/426,3,750) 1350t` / `water clamp(totalEU/320,4,1000) 1800t`):

| chip | cutEUt | totalEU | lubricant | distilled | water |
|---|---|---|---|---|---|
| ilc | 64 | 57600 | 45 | 135 | 180 |
| ram | 96 | 86400 | 67 | 202 | 270 |
| cpu | 120 | 108000 | 84 | 253 | 337 |
| ulpic | 120 | 108000 | 84 | 253 | 337 |
| lpic | 480 | 432000 | **250** (clamped from 337) | **750** (from 1014) | **1000** (from 1350) + cleanroom |
| simple_soc | 64 | 57600 | 45 | 135 | 180 |

Cutter splits >64 into 64-stacks (single-block has 2 output slots) — e.g. 85 RAM = 64+21.

---

# 6. Minecraft recipe chains (copy-pasteable, 1.20.1 GTCEuM + AF9)

## 6.1 Step 0 — Boule (EBF, base GT, unchanged)

```text
gtceu:electric_blast_furnace
  32x gtceu:silicon_dust + gtceu:small_gallium_arsenide_dust → gtceu:silicon_boule | 9000t | 120 EU/t (MV) | 1784K
  (phosphorus 12000t HV 2484K → phosphorus_boule; naquadah 15000t EV 5400K; neutronium 18000t IV 6484K — out of AF9 scope, §9)
```

Real analogue: quartz + C → MG-Si → Siemens → CZ pull. Minecraft: dusts + EBF heat.

## 6.2 Step 1 — Blank wafer (cutter, base GT, unchanged)

```text
gtceu:cutter
  gtceu:silicon_boule → 16x gtceu:silicon_wafer | 400t | 64 EU/t | no fluid in 7.x source (JEI check)
  (phosphorus_boule → phosphorus_wafer 800t HV + cleanroom — out of scope)
```

Real analogue: wire-saw + lap + edge + RCA + CMP. Quest text `boule → 16 wafers via cutter` is correct.

## 6.3 Step 2a — Machine + mask blanks (assembler, AF9)

```text
af9:photolithography_line (gtceu:assembler)
  gtceu:mv_machine_hull + 4x #gtceu:circuits/mv + 2x gtceu:mv_emitter + 2x gtceu:mv_sensor
  + 2x gtceu:mv_robot_arm + 4x gtceu:mv_electric_motor + 2x gtceu:mv_electric_pump
  + 4x gtceu:glass_lens + 8x gtceu:stainless_steel_plate + 576mB gtceu:soldering_alloy
  → gtceu:photolithography_line | 1200t | 120 EU/t (MV)

af9:photomask_blank (gtceu:assembler)
  gtceu:quartzite_plate + gtceu:chromium_plate + 100mB gtceu:photoresist
  → kubejs:photomask_blank | 400t | 120 EU/t (MV)
```

Real analogue: stepper build + chrome-on-quartz mask blank (pre-coated resist). Glass lenses = projection optics, steel = stages, emitter/sensor/arms/motors/pumps = robots + focus + dispense.

## 6.4 Step 2b — Reticles (laser_engraver, AF9, replaces base engraving)

```text
af9:<id>_reticle (gtceu:laser_engraver)
  kubejs:photomask_blank + notConsumable #forge:lenses/<color>
  → kubejs:<id>_reticle | 1800t | 120 EU/t (MV)

ilc red, ram green, cpu light_blue, ulpic blue, lpic orange, simple_soc cyan
```

Real analogue: e-beam mask writer patterning the reticle (OPC/PSM abstracted into lens color). Reticle is NOT consumed in litho — correct (real reticles print 1000s wafers).

Removed base recipes (do NOT re-add): `gtceu:laser_engraver/engrave_ilc_silicon`, `engrave_ram_silicon`, `engrave_cpu_silicon`, `engrave_ulpic_silicon`, `engrave_lpic_silicon`, `engrave_ssoc_silicon`.

## 6.5 Step 2c — Chemistries (chemical_reactor + mixer, AF9)

Purge gas:

```text
af9:extreme_clean_dry_air (gtceu:chemical_reactor)
  notConsumable gtceu:zeolite_dust + 1000mB gtceu:air → 1000mB gtceu:extreme_clean_dry_air | 200t | 30 EU/t (LV)
```

HMDS adhesion chain:

```text
af9:trimethylchlorosilane (gtceu:chemical_reactor)
  gtceu:magnesium_dust + 1000mB gtceu:dimethyldichlorosilane + 1000mB gtceu:chloromethane
  → 3x gtceu:magnesium_chloride_dust + 1000mB gtceu:trimethylchlorosilane | 300t | MV
af9:hexamethyldisilazane (gtceu:chemical_reactor)
  2000mB gtceu:trimethylchlorosilane + 3000mB gtceu:ammonia
  → 4x gtceu:ammonium_chloride_dust + 1000mB gtceu:hexamethyldisilazane | 400t | MV
af9:hmds_vapor (gtceu:mixer)
  100mB gtceu:hexamethyldisilazane + 900mB gtceu:nitrogen → 1000mB gtceu:hmds_vapor | 100t | LV
```

Real: `(CH3)2SiCl2 + CH3Cl + Mg → (CH3)3SiCl`, then `2(CH3)3SiCl + 3NH3 → [(CH3)3Si]2NH + 2NH4Cl`, vaporized in N2 for prime oven. HMDS reaction in code comments is simplified-balanced.

Resist chain (positive DNQ-novolac):

```text
af9:novolac_resin (gtceu:chemical_reactor)
  1000mB gtceu:phenol + 1000mB gtceu:formaldehyde (+100mB gtceu:hydrochloric_acid notConsumed)
  → 1000mB gtceu:novolac_resin + 1000mB minecraft:water | 400t | MV
af9:diazonaphthoquinone (gtceu:chemical_reactor)
  1000mB gtceu:naphthalene + 1000mB gtceu:nitric_acid + 1000mB gtceu:ammonia
  → gtceu:diazonaphthoquinone_dust + 2000mB minecraft:water + 1000mB gtceu:hydrogen | 600t | MV
af9:photoresist (gtceu:mixer)
  gtceu:diazonaphthoquinone_dust + 1000mB gtceu:novolac_resin + 3000mB gtceu:dimethylbenzene (xylene)
  → 4000mB gtceu:photoresist | 400t | MV
```

Real: `C6H5OH + CH2O → novolac + H2O`, DNQ simplified `C10H8 + HNO3 + NH3 → C10H6N2O + 2H2O + H2`, dissolved in xylene.

Developer chain (TMAH 2.38%):

```text
af9:tetramethylammonium_chloride (gtceu:chemical_reactor)
  1000mB gtceu:dimethylamine + 2000mB gtceu:chloromethane
  → gtceu:tetramethylammonium_chloride_dust + 1000mB gtceu:hydrochloric_acid | 300t | MV
af9:tmah_developer (gtceu:chemical_reactor)
  gtceu:tetramethylammonium_chloride_dust + 3x gtceu:potassium_hydroxide_dust + 5000mB gtceu:distilled_water
  → 2x gtceu:rock_salt_dust (KCl) + 5000mB gtceu:tmah_developer | 300t | MV
```

Real: `(CH3)2NH + 2CH3Cl → (CH3)4NCl + HCl`, then `(CH3)4NCl + KOH → (CH3)4NOH + KCl`, diluted to 2.38% (0.26N). K+ stays in salt byproduct — developer is metal-ion-free (correct: K+/Na+ forbidden on wafer).

## 6.6 Step 3 — Print wafers (Photolithography Line, AF9, 30 recipes)

Pattern: `af9:<chip>_wafer_<mode>` on `gtceu:lithography_<mode>`.

```text
af9:ram_wafer_muv (gtceu:lithography_muv) — example, all 6 chips × 5 modes follow it
  gtceu:silicon_wafer + notConsumable kubejs:ram_reticle
  + 40mB hmds_vapor + 100mB photoresist + 200mB tmah_developer + 1000mB distilled_water + 1000mB extreme_clean_dry_air
  → gtceu:ram_wafer{AF9Litho:{Node:350,Transistors:16000000}} | 900t | 480 EU/t (MV 4A)
```

Full table: input always `gtceu:silicon_wafer` + chip reticle; fluids/duration/EU/t per §5.3; output `chip.wafer` + NBT per §5.6. Recipe IDs: `af9:ilc_wafer_muv … af9:ilc_wafer_luv`, `ram`, `cpu`, `ulpic`, `lpic`, `simple_soc` (30 total).

Real analogue per fluid: HMDS prime (adhesion) → resist coat (light-sensitive) → soft bake (solvent drive) → expose (reticle pattern) → PEB (CAR catalyze — abstracted, DNQ path) → TMAH develop (dissolve exposed) → DI rinse → hard bake (harden). 4A own-tier voltage jump (480 → 122880 EU/t) + 1.5× chemicals per node = real cost of finer pitch (more masks, tighter overlay, lower yield).

## 6.7 Step 4 — Dice wafers (cutter, AF9, 90 recipes — replaces base)

Pattern: `af9:cut_<chip>_<mode>[|_water|_distilled_water]` on `gtceu:cutter`. Input uses `strongNBT` — plain (no-NBT) wafers fall through to base counts.

```text
af9:cut_ram_muv (gtceu:cutter) — example
  gtceu:ram_wafer{AF9Litho:{Node:350,Transistors:16000000}} (strongNBT) + 67mB gtceu:lubricant
  → 32x gtceu:ram_chip with same NBT (split 64-stacks if >64) | 900t | 96 EU/t
af9:cut_ram_muv_distilled_water: same wafer + 202mB distilled_water | 1350t | 96 EU/t
af9:cut_ram_muv_water: same wafer + 270mB water | 1800t | 96 EU/t
```

- 6 chips × 5 modes (plain + muv/huv/euv/xuv/luv = 6 wafer variants) × 3 fluids = 108 theoretical; code generates plain + 5 NBT variants = 6 × 3 = 18 per chip, 108 total recipe objects but IDs are `af9:cut_<chip>_<plain|muv|huv|euv|xuv|luv><fluid-suffix>`. (Code comment says 36× — counts per fluid family; trust code loop, not comment.)
- Removed base: `gtceu:cutter/cut_ilc[|_water|_distilled_water]`, `cut_ram…`, `cut_cpu…`, `cut_ulpic…`, `cut_lpic…`, `cut_ssoc…` (18 removals).
- `lpic` variants add `.cleanroom(CLEANROOM)` — cutter must be inside cleanroom. All others no cleanroom.
- Dies keep wafer NBT (tooltip + texture persist to chip).

Real analogue: backgrind → dice (blade/laser/stealth) → sort/bin. Lubricant vs distilled vs water = diamond-wire coolant quality (speed vs defect tradeoff: 900t vs 1350t vs 1800t).

## 6.8 Step 5 — Circuits (circuit_assembler, base GT, unchanged)

AF9 chips feed base GT circuits. Examples (base recipes, not AF9 — verify JEI, `harderCircuitRecipes:false`):

```text
micro_processor (MV): gtceu:cpu_chip + resin_printed_board + … → circuit
micro_processor_assembly, micro_processor_computer/mainframe… upscale with ram/ulpic/lpic/simple_soc + SMDs + boards
```

Boards etched with `sodium_persulfate` or `iron_iii_chloride` (2:1 amount, e.g. good 200 vs 100mB) + `sulfuric_acid` for substrate. SMDs via assembler. AF9 scope stops at chip supply — circuits themselves are base GT.

---

# 7. Connection graph (what plugs into what)

```text
[EBF] silicon_dust + GaAs → silicon_boule
  → [CUTTER] → 16x silicon_wafer (blank)
    → [PHOTOLITHOGRAPHY_LINE + reticle + 5 fluids] → NBT wafer (ilc/ram/cpu/ulpic/lpic/simple_soc × muv…luv)
      → [CUTTER (+cleanroom iff lpic)] → NBT chips (dies per §5.6)
        → [CIRCUIT_ASSEMBLER + boards + SMDs] → MV/HV circuits (base GT)

Side chains into litho:
 [CHEM_REACTOR air+zeolite] → extreme_clean_dry_air ─┐
 [CHEM_REACTOR DMDCS+chloromethane+Mg] → TMCS → [CHEM_REACTOR +ammonia] → HMDS → [MIXER +N2] → hmds_vapor ─┤
 [CHEM_REACTOR phenol+formaldehyde] → novolac ─┐                                                        ├→ [LITHO]
 [CHEM_REACTOR naphthalene+nitric+ammonia] → DNQ_dust ─┴→ [MIXER +xylene] → photoresist ─┤              │
 [CHEM_REACTOR dimethylamine+chloromethane] → TMACl → [CHEM_REACTOR +KOH+DW] → tmah_developer ─┘        │
 [ASSEMBLER quartzite+chromium+resist] → photomask_blank → [LASER_ENGRAVER +lens] → reticle ────────────┘
```

Machine IO summary:

| Machine | Item in | Fluid in | Item out | Needs |
|---|---|---|---|---|
| EBF | dusts | — | boule | heat (1784K Si) |
| Cutter (boule) | boule | (none 7.x) | 16× blank | power |
| Assembler (line) | hull+circuits+parts+lenses+plates | soldering_alloy 576 | line controller | MV |
| Assembler (blank) | quartzite+chromium plates | photoresist 100 | mask blank | MV |
| Laser (reticle) | mask blank + lens (NC) | — | reticle | MV 1800t |
| Chem/Mixer (5 fluids) | dusts/fluids per §6.5 | per §6.5 | fluids + salt byproducts | LV/MV |
| Litho line | silicon_wafer + reticle (NC) | 5 fluids §5.3 | NBT wafer | 2× energy (V per mode) + maint + fluid in, NO cleanroom |
| Cutter (dice) | NBT wafer (strongNBT) | lub/DW/water §5.6 | NBT chips | power (+cleanroom iff lpic) |
| Circuit assembler | chips + boards + SMDs | soldering/flux | circuits | base GT tiers |

---

# 8. How each wafer/chip is used (AF9 scope)

| Wafer → Chip | Real analogue | Used in (base GT circuits) | Notes |
|---|---|---|---|
| `ilc_wafer` → `ilc_chip` (8 base) | jellybean logic / glue | LV/MV basic/good ICs | red lens, cheapest, 50kT MUV |
| `ram_wafer` → `ram_chip` (32 base) | SRAM/DRAM array | memory-heavy assemblies (nano/quantum paths) | green, highest dies (85 LUV), 16MT MUV |
| `cpu_wafer` → `cpu_chip` (8 base) | simple CPU | `micro_processor` family core | light_blue, 5.5MT MUV |
| `ulpic_wafer` → `ulpic_chip` (6 base) | ultra-low-power PIC/MCU | low-power / control circuits | blue, 2kT MUV (tiny — sensor-class) |
| `lpic_wafer` → `lpic_chip` (4 base) | high-density low-power | HV+ circuits, ONLY cutter needing cleanroom | orange, fewest dies, 5kT MUV — cleanroom gating is intentional progression |
| `simple_soc_wafer` → `simple_soc` (6 base) | MCU-class SoC (CPU+RAM+IO monolithic) | SoC-based assemblies (no separate RAM/CPU) | cyan, 1MT MUV, NO `_chip` suffix |

Higher GT wafers/chips (NOT printable in AF9 line — use base GT laser/chemical): `mpic` (brown), `soc` (yellow), `nand/nor_memory` (gray/pink), `hpic` (chemical IV), `uhpic` (LuV), `nano_cpu`/`qbit_cpu` (EV), `advanced/highly_advanced_soc` (naquadah/neutronium), `crystal_*` (autoclave+EBF+lime/blue lenses). See §9 for how to extend.

---

# 9. Extension points (explicitly out of scope — do not assume)

1. **Doped/compound boules**: `phosphorus_boule` (P n-doping, 12000t HV 2484K + cleanroom cutter), `naquadah_boule` (15000t EV 5400K), `neutronium_boule` (18000t IV 6484K). AF9 litho input is hardcoded `gtceu:silicon_wafer` — printing on P/Naq/Neut wafers needs new recipes + lens map.
2. **Higher chips**: `mpic_wafer` (900t HV+cleanroom laser brown), `soc_wafer` (yellow), `nand/nor` (gray/pink), `hpic/uhpic_wafer` (chemical_reactor 1200t IV/LuV+cleanroom), `nano_cpu/qbit_cpu_wafer` (1200t EV+cleanroom), `advanced_soc` (naquadah 900t EV), `highly_advanced_soc` (neutronium 900t IV). Each needs mode + transistor base + die math if added.
3. **Wet etch extension**: add `chemical_bath` HF/BOE step only after a real HF chain (fluorspar + sulfuric → HF) is designed. Do not invent `gtceu:hydrofluoric_acid` without checking material registry.
4. **Sterile**: only `data_module` needs `STERILE_CLEANROOM` in base. Sterile-gated wafers need `sterilizing_filter_casing` roof + explicit flag.
5. **High-tier content**: `highTierContent:false` hides UEV+. Do not spec UEV+ circuits/machines without flipping it + adding recipes (pack intent: not playable vanilla).
6. **Leftovers to clean or ignore**: `kubejs:krf_excimer_laser_module` / `twin_stage_scanner_module` textures + old `high_grade/premium` lang keys may still exist on disk from `9745aca` — they are NOT referenced by HEAD recipes. New agents: ignore or delete, do not wire into Mk logic.

---

# 10. Agent validation checklist (run before any PR)

## 10.1 IDs

- [ ] Controller is `gtceu:photolithography_line` (NOT `af9:`). Recipe IDs are `af9:` (e.g. `af9:ram_wafer_euv`, `af9:cut_cpu_luv_water`, `af9:photolithography_line`, `af9:photomask_blank`, `af9:<id>_reticle`, `af9:<chem>`).
- [ ] Recipe types are `gtceu:lithography_muv|huv|euv|xuv|luv` (NOT `gtceu:photolithography` — that was the old single-type ID).
- [ ] Chip `simple_soc` has NO `_chip`. Reticle `kubejs:simple_soc_reticle`, wafer `gtceu:simple_soc_wafer`, engrave removal `engrave_ssoc_silicon`, cutter removal `cut_ssoc`. Grep for `simple_soc_chip` = must be zero hits.
- [ ] Lenses: `#forge:lenses/red|green|light_blue|blue|orange|cyan` for ilc/ram/cpu/ulpic/lpic/simple_soc. Do not swap (cyan≠light_blue).
- [ ] NBT exact: `{AF9Litho:{Node:<50|100|200|250|350>,Transistors:<int>}}` on `Item.of(...)`, cutter inputs use `.strongNBT()`. `TAG=AF9Litho`, `TAG_NODE=Node`, `TAG_TRANSISTORS=Transistors` per `LithoMode.java`.

## 10.2 Numbers

- [ ] Litho duration always `900t`. EU/t = 480 / 1920 / 7680 / 30720 / 122880 for muv/huv/euv/xuv/luv (4A MV/HV/EV/IV/LuV). Fluids per §5.3 (round, NOT floor — `Math.round`).
- [ ] Dies/transistors per §5.6 (`Math.round` + `sqrt`/`pow`). Spot-check: RAM LUV 85 dies / 784MT; LPIC EUV 5 dies (NOT 6); ULPIC HUV 7 dies (NOT 7.5).
- [ ] Cutter: duration 900/1350/1800 for lub/DW/water, EU/t = cutEUt (64/96/120/120/480/64), fluids per §5.6 table (clamp! LPIC lub 250 NOT 337). LPIC ONLY has cleanroom.
- [ ] Energy hatches exactly 2 (LITHO_GATE, 4A total). Voltage: MV=MUV, HV=HUV, EV=EUV, IV=XUV, LuV=LUV. All modes 4A (`VA[tier]*4`). Stronger hatches perfect-overclock (`OC_PERFECT`).
- [ ] Mask blank 400t MV, reticles 1800t MV, line controller 1200t MV, chemistries per §6.5 (200/300/400/100/400/600/400/300/300t).

## 10.3 Structure/behavior

- [ ] Pattern is 3×3×20 per §5.2 (20 aisles). `C` min 100 + exactly 2 energy + exactly 1 maintenance + ≥1 fluid import. `I` 1-2 item IO. `F` filter_casing present (FFU = no cleanroom justification).
- [ ] `LITHO_GATE + OC_PERFECT` on machine (working tree; old HEAD was `OC_NON_PERFECT`). Recipe switching via GT mode tab + controller buttons (5 modes, finest last).
- [ ] Wafers/chips show node/transistors tooltip + per-mode texture via `af9:litho_mode` 1-5 (`AF9Client`). Plain (0) = base GT texture.
- [ ] Base removals present: 6× `laser_engraver/engrave_*` + 18× `cutter/cut_*` (6 prefixes × 3 fluids). No duplicate recipes in JEI.

## 10.4 Realism language (for quests/tooltips — do not misstate)

- [ ] Say `MUV 350nm` etc. as AF9-internal nodes. Do NOT claim real EUV = 200nm (real EUV 13.5nm, DUV 193nm, KrF 248nm, i-line 365nm).
- [ ] Say TMAH developer is metal-ion-free (K+ leaves as `rock_salt_dust`/KCl). Do NOT say KOH touches the wafer.
- [ ] Say HMDS is vapor-primed in N2 (not dipped). Say resist is DNQ-novolac positive (not CAR/EUV MOR — those are sub-100nm real extensions).
- [ ] Say dies/wafer grow ~√(350/node), transistors ~ (350/node)². Do NOT promise linear scaling.
- [ ] Say fab repeats 400-1400 steps / 2-4 months; one Minecraft recipe = one mask layer abstracted (prime→coat→bake→expose→bake→develop→rinse→bake).

## 10.5 Top 10 mistakes (from 5 parallel research agents + code review)

1. Using `af9:photolithography_line` as machine ID (it is the assembler recipe ID; machine is `gtceu:`).
2. Using old single `gtceu:photolithography` type, old `Mk I-III / high_grade / premium / KrF+Twin` numbers, or old committed doubling model (960/1920/3840/7680, `OC_NON_PERFECT`) — superseded by 5-mode NBT + working-tree 4A own-tier (480/1920/7680/30720/122880, `OC_PERFECT`).
3. Inventing `simple_soc_chip` / `engrave_simple_soc_silicon` (correct: `simple_soc` / `engrave_ssoc_silicon` / `cut_ssoc`).
4. Forgetting `strongNBT` on cutter inputs (NBT wafers would not match, plain wafers would over-match).
5. Using `Math.floor` for litho fluids/dies (code uses `Math.round`; LPIC EUV 5 vs 6 hinges on it).
6. Forgetting clamp on cutter fluids (LPIC lub 250 / distilled 750 / water 1000, NOT 337/1014/1350).
7. Adding cleanroom to litho recipes or removing it from `cut_lpic_*` (line has FFUs, NO cleanroom; lpic dice NEEDS `CLEANROOM`).
8. Allowing 1 or 4 energy hatches (must be exactly 2; voltage gates modes).
9. Claiming base GTCEuM has `photoresist` / EUV machine / `crystallizer` (it does NOT — AF9 adds them; base uses persulfate/iron3 + laser + autoclave/EBF).
10. Calling AF9 `EUV 200nm` real EUV (real EUV 13.5nm all-reflective vacuum; AF9 names are gameplay abstractions of 350→50nm scaling).

---

# Appendix A. File map for implementers

```text
af9-core/build.gradle, gradle.properties, settings.gradle  # JDK17, GTCEu 7.2.0 pins (ldlib 1.0.40.b vs pack 1.0.45 + configuration 2.2.0 vs 3.1.0 drift — bump together)
af9-core/src/main/java/com/af9/core/AF9Core.java            # MOD_ID af9
af9-core/src/main/java/com/af9/core/litho/LithoMode.java    # enum MUV…LUV, TAG, eut(), density, dieFactor, modelIndex
af9-core/src/main/java/com/af9/core/machine/PhotolithographyLineMachine.java
af9-core/src/main/java/com/af9/core/machine/LithoRecipeLogic.java
af9-core/src/main/java/com/af9/core/client/AF9Client.java   # litho_mode predicate + tooltip
kubejs/startup_scripts/gtceu/photolithography.js           # materials + items + 5 types + 3x3x20 pattern
kubejs/server_scripts/mods/gtceu/photolithography.js       # machine + blanks + reticles + chemistries + 30 litho + 108 cutter + removals
kubejs/assets/gtceu/lang/en_us.json                        # §§5.1/5.4 names + tooltips
kubejs/assets/gtceu/models/item/{ilc,ram,cpu,ulpic,lpic,simple_soc}_{wafer,chip}.json  # predicate overrides
kubejs/assets/af9/models/item/litho/*.json + textures/item/litho/*.png  # per-mode art
config/gtceu.yaml                                          # cleanroom/maintenance/highTier flags
config/ftbquests/quests/chapters/{medium,high,extreme,insane}_voltage.snbt  # STALE text (says laser-engrave; IDs still valid via plain-NBT fallthrough — update text, not IDs)
```

# Appendix B. Copy-paste snippets (KubeJS)

```js
// Litho (example: CPU EUV — working tree: EV 4A = 7680 EU/t)
allthemods.recipes.gtceu.lithography_euv('af9:cpu_wafer_euv')
  .itemInputs('gtceu:silicon_wafer')
  .notConsumable('kubejs:cpu_reticle')
  .inputFluids(
    Fluid.of('gtceu:hmds_vapor', 90),
    Fluid.of('gtceu:photoresist', 225),
    Fluid.of('gtceu:tmah_developer', 450),
    Fluid.of('gtceu:distilled_water', 2250),
    Fluid.of('gtceu:extreme_clean_dry_air', 2250))
  .itemOutputs(Item.of('gtceu:cpu_wafer', '{AF9Litho:{Node:200,Transistors:16843750}}'))
  .duration(900).EUt(7680)

// Dice (example: CPU EUV, lubricant)
allthemods.recipes.gtceu.cutter('af9:cut_cpu_euv')
  .itemInputs(Item.of('gtceu:cpu_wafer', '{AF9Litho:{Node:200,Transistors:16843750}}').strongNBT())
  .inputFluids(Fluid.of('gtceu:lubricant', 84))
  .itemOutputs(Item.of('gtceu:cpu_chip', 11, '{AF9Litho:{Node:200,Transistors:16843750}}'))
  .duration(900).EUt(120)
```

> AGENT: split >64 outputs into 64-stacks (see `chipStacks()`), generate all 3 fluid variants, add `.cleanroom(CleanroomType.CLEANROOM)` IFF `lpic`.

# Appendix C. Research provenance (5 parallel agents)

- Agent A (codebase explore): existing IDs, wafer grades, line logic, version drift, 13 contradictions to avoid — folded into §§5/10.
- Agent B (wafer fab): quartz → MG-Si (11-13 MWh/t, 1414C) → Siemens (1080-1100C, 9N-11N) / FBR → CZ (0.5-2.5mm/min, Ar, B/P, Oi) / FZ → wire-saw → RCA SC-1/SC-2 + CMP + epi → SEMI specs/grades — folded into §1.
- Agent C (lithography/FEOL/BEOL): cleanroom ISO, HMDS/DNQ/CAR/MOR, DUVi 193nm NA1.35 vs EUV 13.5nm, RIE/DRIE vs HF/BOE/KOH/TMAH, implant B/P/As/Sb + RTP, PVD/CVD/ALD/epi, CMP, 400-1400 steps, nodes/CPP/MMP, KLA/CD-SEM/PCM/sort — folded into §2.
- Agent D (chips/SoCs): 36-term hierarchy transistor→system with what/made/used/example — folded into §3 table.
- Agent E (GTCEuM 1.20.1): 7.x tiers/IDs/machines/EU-t/fluids/cleanroom/what-is-missing (UNCERTAIN flagged) — folded into §4 + §§6-8.
- Numbers cross-checked against `LithoMode.java` + server scripts (§§5.3/5.6). Any conflict → code wins, flagged in §10.

*End of spec — implement, then run §10 checklist and update stale quest text (`medium/high/extreme_voltage.snbt` still say laser-engrave; wafers still satisfy via plain-NBT fallthrough).*
