---
title: "AF9 Semiconductor Factory — Agent-Optimized Spec: Wafers, Silicon, SoCs, Chips, Lithography"
branch: "main"
head_commit: "see git log (2026-09-25 c: one mode per substrate — nine wafer substrates 350 nm to 1 nm, plain printed wafers without NBT or packages, vacuum cleanliness and broken wafers, EBF Boule Melting with Endion coils, Supercooling Cryostat and Coolant Hatch, Particle Accelerator, Orbital Lithography Station, wafer contamination, Jade; 2026-09-25 b: fab chemistry lines §6.11-6.16; 2026-09-25 a: real light source + resist per mode; 2026-09-24: all GT chip wafers in the line, tier-matched HV-LuV circuit metals, XCDA, zircon, high-k)"
minecraft: "1.20.1"
forge: "47.4.0"
gtceu: "7.2.0 (GregTech CEu Modern)"
kubejs: "2001.6.5-build.16"
af9_core: "0.1.0 (mod_id `af9`)"
gtceu_config: "enableCleanroom=true, cleanMultiblocks=false, enableMaintenance=true, highTierContent=false, orderedAssemblyLineItems=true"
status: "Matches main. One lithography mode per wafer substrate (§5.2): Photolithography Line 350 nm Si (MV) → 200 nm P → 100 nm Nq → 80 nm trinium → 65 nm naquadria → 50 nm Nt → 20 nm transmuted neutronium → 7 nm strange matter (UHV), each 4A of its tier + OC_PERFECT and with its real light source and resist (i-line, KrF, ArF, ArF immersion, EUV, high-NA EUV); the Orbital Lithography Station prints 1 nm chromodynium wafers in orbit (50A UHV laser). A substrate prints its own chips and every lower substrate's chips as GT's own plain chip wafers (a better substrate gives more of them per blank), no NBT. Vacuum cleanliness 0-100, air cooling, OPC computation, calibration and a Metrology Station decide the break roll (§18, §5.4). Boules are 10x material in the EBF's Boule Melting mode (§12). Superseded: wafer packages, per-mode chips (MUV/HUV/EUV/XUV/LUV), the old CZ boules, Mk I-III modules, high_grade/premium items."
agent_hint: "All exact IDs are in backticks. `gtceu:` = base GregTech item/machine/recipe-type (KubeJS GT machines, materials and recipe types also land in `gtceu:`). `kubejs:` = AF9 custom item/block. `af9:` = AF9 custom recipe ID (output namespace varies — see §6). No NBT anywhere in the chip chain."
---

# 0. How agents must read this doc

## 0.1 Conventions

- `ITEM` = registry ID, e.g. `gtceu:silicon_wafer`. Always use full `namespace:path`.
- `RECIPE` = recipe ID, e.g. `af9:print_ram_350nm`. AF9 recipe IDs live under `af9:` even when they output `gtceu:` items.
- `MACHINE` = controller block ID, e.g. `gtceu:photolithography_line`.
- `TYPE` = recipe-type ID, e.g. `gtceu:lithography_350nm`.
- EU/t = GregTech volts × amps. `VA[LV]=30, VA[MV]=120, VA[HV]=480, VA[EV]=1920, VA[IV]=7680, VA[LuV]=30720`.
- Duration: `t` = ticks, 20t = 1s. `900t = 45s`.
- No NBT: printed wafers and chips are plain items (the old `{AF9Litho}` packages, per-mode chips and the `af9:litho_mode` predicate are removed). A chip is a chip whatever substrate it came from.
- `notConsumable` = catalyst / reticle / lens, not used up.
- `cleanroom(CLEANROOM)` = recipe only runs when cutter is inside a formed + powered `gtceu:cleanroom`. AF9 litho line itself needs NO cleanroom.

## 0.2 Source-of-truth files (main)

See Appendix A for the full map. The numbers live in three places that must agree: `AF9_WAFERS` in
`kubejs/server_scripts/mods/gtceu/photolithography.js` (recipes), `AF9_WAFER_TABLE` in `kubejs/startup_scripts/gtceu/wafers.js`
(item registration) and `LithoMode` in `af9-core/src/main/java/com/af9/core/litho/` (machine behaviour).

> AGENT: if any doc contradicts these files, the files win. Dead designs, no longer registered: the `high_grade` / `premium` / `Mk I-III + KrF + Twin-Stage` design from `9745aca`, and the wafer packages (`kubejs:<chip>_wafer_package`, `{AF9Litho}` NBT, modes MUV/HUV/EUV/XUV/LUV, `gtceu:lithography_muv` …). Do not invent `kubejs:ram_wafer_high_grade`, `kubejs:ram_wafer_package` or `gtceu:lithography_muv`.

## 0.3 One-paragraph mental model

Real fab: quartz → MG-Si → ultra-pure polysilicon → Czochralski boule → diamond-wire wafers → RCA clean + CMP → repeat 100s of times: HMDS prime → resist coat → bake → expose through reticle → bake → develop → etch → implant → deposit → CMP. Wafer → probe → dice → package → PCB. In AF9/GregTech 1.20.1 this is compressed to: Siemens polysilicon → melt charges + seed crystal (SMC) → boule (EBF Boule Melting) → cutter blank wafer of one of nine substrates → Coater Track (HMDS prime, BARC, resist, TARC, bake: the blank becomes a coated wafer, §6.5b) → Photolithography Line (built in 8 versions like the Assembly Line's lengths; the mode is the substrate: 350 nm silicon … 7 nm strange matter; a reticle of the node's mask class + developer, rinse water, clean air, etch plasma + the mode's laser gas / immersion water / HfCl4 / tin) → GT's chip wafers (as many as the substrate yields) or broken wafers, decided by the machine's vacuum, cooling, computation and calibration (§5.4, §18) → cutter dies → circuit assembler. Higher substrates print every lower substrate's chips too and give more chip wafers per blank. Reticles are the Minecraft reticle/mask, chemistries are HMDS/photoresist/TMAH fluids, the stepper is the multiblock. Each mode uses the light source its real node used (mercury i-line 365 nm → KrF 248 nm → ArF 193 nm → ArF immersion → EUV 13.5 nm → high-NA EUV) and the resist made for that light (DNQ-novolac → chemically amplified PHOST → chemically amplified methacrylate → tin-oxo EUV resist); the 1 nm chromodynium node is an X-ray free-electron laser in orbit. Wafers taken into a player's inventory get contaminated unless the player wears gloves or stands in a clean Cleanroom.

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
- Minecraft map (AF9, §6.12): quartzite leached in HCl → `high_purity_quartz`, SMC thermal furnace (calcination & reduction) at 1800 K with coke → `metallurgical_grade_silicon` + CO.

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

No Siemens unit in GTCEuM 1.20.1; AF9 simulates it (§6.12): hydrochlorination → `crude_chlorosilanes` → distillation (TCS/STC/DCS/BCl3) → carbon polishing → `siemens_feed_gas` → CVD bell jar (SMC thermal furnace) → `polysilicon` + vent gas → recovery and STC conversion (closed loop), STC also to fumed silica or TEOS. FBR is not modelled.

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
- Minecraft map: melt charges (`electronic_grade_silicon` = etched poly chunks + boron from the BCl3 cut for p-type, or phosphorus for n-type; SMC blending), a seed crystal (SMC crystal growth) and a crucible (fused quartz; tritanium for the exotic melts) go into the EBF's **Boule Melting** mode under argon / xenon / endion (§12). `gtceu:silicon_boule` = p-type CZ boule, `gtceu:phosphorus_boule` = n-type.

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
- Minecraft map: distilled-water rinse + filter-casing FFU + `extreme_clean_dry_air` purge stand in for RCA/CMP/epi. No separate epi item — the substrate (nine blank wafers, §5.2) stands in for the epi/prime distinction.

## 1.7 Chemical master list (real → AF9 fluid)

| Real chemical | Purpose | AF9 fluid (`gtceu:`) |
|---|---|---|
| `SiO2`, `Si`, `SiHCl3`, `SiH2Cl2`, `SiCl4`, `SiH4` | feed/poly/epi | abstracted into `silicon_dust` / boule (no separate fluids) |
| `HCl`, `H2`, `Ar`, `N2` | hydrochlorination, carrier, CZ blanket | `hydrochloric_acid`, `hydrogen`, `nitrogen` (used in HMDS chain), air |
| `HF`, `HNO3`, `NH4OH`, `H2O2`, `KOH` | etch + RCA + CMP pH | abstracted into `distilled_water` rinse + `tmah_developer` (TMAH is KOH-analogue, metal-ion-free) |
| HMDS `[(CH3)3Si]2NH` | adhesion promoter | `hexamethyldisilazane` → `hmds_vapor` |
| Novolac `(C7H6O)n` + DNQ `C10H6N2O` in xylene | i-line positive resist (g/i-line only, bleaches too weakly below ~300 nm) | `novolac_resin` + `diazonaphthoquinone_dust` → `photoresist` (i-line Photoresist) |
| KrF CAR: poly(4-hydroxystyrene) (PHOST, partly protected) + PAG in PGMEA | 248 nm chemically amplified resist | `polyhydroxystyrene` + `triphenylsulfonium_triflate` + `propylene_glycol_methyl_ether_acetate` → `krf_photoresist` |
| ArF CAR: methacrylate copolymer (acid-labile esters; no aromatics, they absorb 193 nm) + PAG in PGMEA | 193 nm chemically amplified resist | `methyl_methacrylate` → `methacrylate_resin` + PAG + PGMEA → `arf_photoresist` |
| PAG triphenylsulfonium triflate `(C6H5)3S+ CF3SO3-` | photoacid generator (photon → triflic acid, PEB amplifies) | `trifluoromethanesulfonic_acid` → `triphenylsulfonium_triflate` |
| Excimer premix: ~1 % Kr or Ar + ~0.1 % F2 in Ne | KrF / ArF laser gas | `krf_excimer_gas` / `arf_excimer_gas` (AF9: 5 % + 1 %, §6.14) |
| Ultrapure water 18.2 MΩ·cm, degassed | immersion film (n = 1.44 at 193 nm) | `ultrapure_water` (65 and 50 nm) |
| TMAH `(CH3)4NOH` 2.38% / 0.26N | developer (K+/Na+-free) | `tmah_developer` |
| CMP slurry colloidal `SiO2`/`CeO2` | planarization | `lubricant` (cutter) + `distilled_water` (rinse) |
| Clean dry air: catalytic oxidizer (hopcalite / Pt) → CO2 scrubber → 13X molecular sieve → cryogenic cold box → membrane filter | purge | `oxidized_air` → `decarbonated_air` → `dry_air` → `cryogenic_supercooled_air` → `extreme_clean_dry_air` (§6.5) |
| `HfCl4` + `H2O` ALD → `HfO2` high-k gate (45 nm and below) | gate dielectric | `hafnium_tetrachloride` (50, 20 and 7 nm, from zircon, §6.9) |

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

Minecraft map (AF9, one recipe = one full cycle): `hmds_vapor` prime → resist coat (the mode's resist) → soft bake (cupronickel coil) → expose (light source + reticle + lens; excimer modes burn laser gas, 65/50 nm through `ultrapure_water`, EUV modes burn molten tin + hydrogen) → PEB (where a CAR's acid amplifies) → `tmah_developer` develop → `distilled_water` rinse → hard bake. All fluids in one recipe (5 at 350 nm, 6 at 200-80 nm, 7 at 65 nm, 8 at 50-7 nm), 900t fixed. The orbital 1 nm print is dry: a solid resist cartridge and supercooled endion for the optics (§6.6).

## 2.3 Pattern transfer — etch

- Dry (anisotropic, production): RIE (capacitive, -100 to -1000V bias), ICP (high density low damage), DRIE Bosch (`SF6` etch / `C4F8` passivate cycles, 10-500µm trenches, MEMS/TSV). Gases: `CF4/O2`, `CHF3/O2`, `C4F8`, `SF6`, `NF3`, `HBr/Cl2/O2`, `BCl3/Cl2`, `O2` strip.
- Wet (isotropic/selective, cheap): `HF` (`SiO2+6HF->H2SiF6+2H2O`), BOE `NH4F:HF` 6:1/7:1, `KOH` anisotropic Si {100}/{110} vs {111} 54.7° (MEMS, K+ banned FEOL), TMAH (CMOS-compatible KOH analogue + developer), hot `H3PO4` 150-180C (`Si3N4` selective), piranha/SC-1/SC-2.
- <50nm anisotropic lines = dry only (wet undercuts).
- Minecraft map: abstracted — no separate etch recipe. `laser_engraver` reticle-writing + cutter dicing stand in for etch. Future extension could add `chemical_bath` HF/BOE step (§9).

## 2.4 Doping — implant (dominant) vs diffusion (legacy/power)

- Species: p-type **B, BF2, In** (holes), n-type **P, As, Sb** (electrons). Must sit substitutionally → anneal.
- Implant: mass-separated beam 1keV-3MeV (depth), dose 1e11-1e16 cm⁻² (concentration), 7° tilt anti-channeling, screen oxide. Amorphizes → anneal: furnace 800-1100C (old, too much diffusion), RTP spike 1000-1100C 1-10s (standard), flash/laser ms-µs (ultra-shallow), SPE regrowth, C/N co-implant anti-TED.
- Diffusion: `POCl3` n / `BBr3/B2O3` p, 850-1150C hours, isotropic, deep junctions — now only power/CCD/photodiode/well drive.
- Minecraft map: `phosphorus_boule` (P-doped) stands in for n-doping. No implant machine — the substrate stands in for junction scaling; the Particle Accelerator's neutron transmutation doping (§14, real NTD turns Si-30 into P-31) makes transmuted neutronium.

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

AF9 nodes 350/200/100/80/65/50 nm are 1990s-2000s real nodes (350 nm ≈ Pentium II era); 20 and 7 nm are EUV-era, 1 nm is beyond any roadmap (an orbital X-ray FEL, §5). The light sources are the real ones for those nodes: 350 nm = mercury-lamp i-line steppers, 180-200 nm = KrF 248 nm (sub-wavelength from 180 nm, hence higher NA + OPC), 80-100 nm = ArF 193 nm dry, 50-65 nm = ArF immersion (NA 1.2-1.35), 20 nm = EUV 13.5 nm (NA 0.33), 7 nm = high-NA EUV (NA 0.55). Rayleigh `CD = k1·λ/NA` keeps every AF9 mode at k1 ≥ 0.29 (7 nm; single exposure limit 0.25).

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

GT mapping: `gtceu:ilc_chip` ≈ jellybean logic, `gtceu:ram_chip` ≈ SRAM/DRAM cell array, `gtceu:cpu_chip` ≈ simple CPU, `gtceu:ulpic/lpic` ≈ low-power / high-density PIC variants, `gtceu:simple_soc` ≈ MCU-class SoC (CPU+RAM+IO on one `simple_soc_wafer`). All GT chip wafers (incl. `mpic/hpic/uhpic/nano_cpu/qbit_cpu/nand/nor/soc/advanced_soc/highly_advanced_soc`) are made in the AF9 line (§5.5); only `crystal_*` keeps GT's autoclave/laser path.

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
Chip wafers + chips (all made in the AF9 line now, see §5.3): `ilc`, `ram`, `cpu`, `ulpic`, `lpic`, `simple_soc`, `nand_memory`, `nor_memory`, `mpic`, `soc`, `advanced_soc`, `highly_advanced_soc`, `nano_cpu`, `qbit_cpu`, `hpic`, `uhpic` (`<x>_wafer` + chip; chips without `_chip`: `simple_soc`, `soc`, `advanced_soc`, `highly_advanced_soc`).
Still base GT only: `gtceu:crystal_cpu/soc`, `gtceu:raw_crystal_chip`, `gtceu:engraved_crystal_chip`.
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
- Pack config: `enableCleanroom:true`, `cleanMultiblocks:false`. Do not disable — cutting most chips (§5.3) and most HV-LuV circuits need it in AF9, and disabling hides real gating.

## 4.5 What base GT does NOT have (why AF9 exists)

- No EUV/UV lithography machine, no `photoresist` chemistry, no `crystallizer` singleblock.
- SoC items DO exist (`simple_soc/soc/advanced_soc/highly_advanced_soc/crystal_soc` + wafers) — the gap is the process, not the item.
- Only `data_module` needs sterile cleanroom — custom sterile wafers must add `STERILE_CLEANROOM` explicitly.
- UNCERTAIN in JEI: exact `multilayer_...` board strings, `forge` vs `c` lens tag, whether JEI shows water/lubricant boule-cutting variants (7.x source shows none — AF9 cutter adds them explicitly).

---

# 5. AF9 lithography — current implementation

Three machines print chip wafers: the **Photolithography Line** (Mk1: 350, 200, 100 nm), the **Photolithography Scanner** (Mk2: 80, 65 nm) and the **Orbital Lithography Station** (50, 20, 7, 1 nm, only in orbit). There is **one mode per wafer substrate**, and what comes out is GT's own chip wafer (AF9's own chips: their own wafers), as many as the substrate yields (§5.3). No NBT, no packages, no per-mode chips, no per-substrate printed wafers (all removed).

## 5.1 Machines + recipe types

| Thing | ID | Display name |
|---|---|---|
| Mk1 controller | `gtceu:photolithography_line` | Photolithography Line |
| Mk2 controller | `gtceu:photolithography_scanner` | Photolithography Scanner Mk2 |
| Orbital controller | `gtceu:orbital_lithography_station` | Orbital Lithography Station |
| Mk1 modes | `gtceu:lithography_350nm`, `_200nm`, `_100nm` | Lithography 350 nm (Silicon) … 100 nm (Naquadah) |
| Mk2 modes | `gtceu:lithography_80nm`, `_65nm` | 80 nm (Trinium), 65 nm (Naquadria) |
| Orbital modes | `gtceu:lithography_50nm`, `_20nm`, `_7nm`, `gtceu:orbital_lithography` | 50 nm (Neutronium) … 1 nm (Chromodynium) |
| Coater controller | `gtceu:wafer_coater` | Coater Track (type `gtceu:wafer_coating`, §6.5b) |

Java (AF9 Core): `LithoMode` (the 9 modes: substrate, node, tier, light, λ, NA, resist, base break chance), `LithoMachine` (shared by both: vacuum cleanliness, break roll, counters, `LITHO_GATE`, `STRIP_BROKEN`), `PhotolithographyLineMachine` (Mk1 and Mk2 from a `Spec`: modes, lens block and slices, light sources, vacuum level, title; versions, `LITHO_VERSION`, version preview pages, EMI info), `OrbitalLithographyMachine` (orbit check), `LithoRecipeLogic` (break roll when a print finishes), `LithoConsoleWidget` (console), `compat/jade/AF9MachineProvider` (Jade).
Modifiers: line and scanner `LITHO_GATE + STRIP_BROKEN + LITHO_VERSION + OC_PERFECT + BATCH_MODE` (no parallel hatch); orbital `LITHO_GATE + STRIP_BROKEN + COOLANT + OC_PERFECT + BATCH_MODE`.
Tooltips: `af9.photolithography_line.tooltip.0-16`, `af9.photolithography_scanner.tooltip.0-9`, `af9.orbital_lithography_station.tooltip.0-9` (`kubejs/assets/gtceu/lang/en_us.json`).

## 5.2 The nine substrates (load-bearing numbers)

`LithoMode` (Java) = `AF9_WAFERS` (server `photolithography.js`) = `AF9_WAFER_TABLE` (startup `wafers.js`). Change them together.

| # | Substrate id | Blank wafer | Mode | Tier | EU/t | Light | λ nm | NA | k1 | Resist | Base break | Machine (version) |
|---|---|---|---|---|---|---|---|---|---|---|---|---|
| 0 | `silicon` | `gtceu:silicon_wafer` | 350 nm | MV | 4A = 480 | mercury i-line | 365 | 0.60 | 0.58 | `photoresist` | 2 % | Mk1 line (V1) |
| 1 | `phosphorus` | `gtceu:phosphorus_wafer` | 200 nm | HV | 1920 | KrF excimer | 248 | 0.70 | 0.56 | `krf_photoresist` | 3 % | Mk1 line (V2) |
| 2 | `naquadah` | `gtceu:naquadah_wafer` | 100 nm | EV | 7680 | ArF excimer (dry) | 193 | 0.75 | 0.39 | `arf_photoresist` | 5 % | Mk1 line (V3) |
| 3 | `trinium` | `kubejs:trinium_wafer` | 80 nm | IV | 30720 | ArF (dry) | 193 | 0.93 | 0.39 | `arf_photoresist` | 7 % | Mk2 scanner (V1) |
| 4 | `naquadria` | `kubejs:naquadria_wafer` | 65 nm | LuV | 122880 | ArF immersion | 193 | 1.20 | 0.40 | `arf_photoresist` | 9 % | Mk2 scanner (V2) |
| 5 | `neutronium` | `gtceu:neutronium_wafer` | 50 nm | ZPM | 491520 | ArF immersion | 193 | 1.35 | 0.35 | `arf_photoresist` | 12 % | orbital station |
| 6 | `transmuted_neutronium` | `kubejs:transmuted_neutronium_wafer` | 20 nm | UV | 1966080 | EUV (tin plasma) | 13.5 | 0.33 | 0.49 | `euv_photoresist` | 18 % | orbital station (+ EUV source) |
| 7 | `strange_matter` | `kubejs:strange_matter_wafer` | 7 nm | UHV | 7864320 | high-NA EUV | 13.5 | 0.55 | 0.29 | `euv_photoresist` | 25 % | orbital station (+ EUV source) |
| 8 | `chromodynium` | `kubejs:chromodynium_wafer` | 1 nm | UHV | 50A = 98304000 | X-ray FEL (orbit) | 1.0 | 0.50 | 0.50 | `kubejs:dry_resist_cartridge` | 35 % | orbital station |

Order note: trinium comes before naquadria because GT smelts trinium at LuV and naquadria only at ZPM (trinium dust also comes from AF9's trinium ore). The user-facing "placeholder" wafers are Trinium (4th), Naquadria (5th), Transmuted Neutronium (7th), Strange Matter (8th) and Chromodynium (9th).

Colours on the consoles / tiles: 350 violet, 200 blue, 100 cyan, 80 green, 65 lime, 50 gold, 20 orange, 7 rose, 1 white.

## 5.3 Printed wafers

A substrate prints every chip whose own substrate (GT's) is the same or lower. GT's native substrates: silicon for ILC, RAM, CPU, ULPIC, LPIC, Simple SoC; phosphorus for NAND, NOR, MPIC, SoC; naquadah for ASoC; neutronium for HASoC. Derived wafers (Nano CPU, Qubit CPU from CPU; HPIC, UHPIC from MPIC) follow the substrate of the wafer they come from (GT's own: silicon for Nano/Qubit CPU, phosphorus for HPIC/UHPIC).

- **A print is GT's own chip wafer**: `gtceu:<chip>_wafer` (AF9's own chips: `kubejs:<chip>_wafer`, §5.3b), plain, no NBT. There are no per-substrate printed wafer items (the old `kubejs:<substrate>_<chip>_wafer` variants are removed): a chip is a chip whatever substrate it was printed on. Chip ids are GT's wafer ids: `ilc ram cpu ulpic lpic simple_soc nand_memory nor_memory mpic soc advanced_soc highly_advanced_soc nano_cpu qbit_cpu hpic uhpic`.
- **A better substrate gives more wafers per print, not more dies per wafer.** One chip wafer on the chip's own substrate; above it `floor(substrate yield ÷ class divisor)` wafers (`AF9_WAFERS.yieldOf`): substrate yields 1 / 4 / 8 / 10 / 12 / 16 / 24 / 32 / 64 (Si … Qc), class divisors by the chip's own substrate 1 / 2 / 8 / 10 / 12 / 16 / 24 / 32 (GT's engraving numbers, extended). The Cutter then cuts them with GT's own recipes (the bold dies of the table, unchanged).

Chip wafers per print (bold: the chip's own substrate, 1 wafer):

| chip | Si | P | Nq | Ke | Nq* | Nt | Nt* | Sq | Qc | dies per wafer (GT cutter) | cut EU/t | cleanroom |
|---|---|---|---|---|---|---|---|---|---|---|---|---|
| ilc | **1** | 4 | 8 | 10 | 12 | 16 | 24 | 32 | 64 | 8 | 64 | no |
| ram | **1** | 4 | 8 | 10 | 12 | 16 | 24 | 32 | 64 | 32 | 96 | no |
| cpu | **1** | 4 | 8 | 10 | 12 | 16 | 24 | 32 | 64 | 8 | 120 | no |
| ulpic | **1** | 4 | 8 | 10 | 12 | 16 | 24 | 32 | 64 | 6 | 120 | no |
| lpic | **1** | 4 | 8 | 10 | 12 | 16 | 24 | 32 | 64 | 4 | 480 | yes |
| simple_soc | **1** | 4 | 8 | 10 | 12 | 16 | 24 | 32 | 64 | 6 | 64 | no |
| nand_memory | — | **1** | 4 | 5 | 6 | 8 | 12 | 16 | 32 | 32 | 192 | yes |
| nor_memory | — | **1** | 4 | 5 | 6 | 8 | 12 | 16 | 32 | 16 | 192 | yes |
| mpic | — | **1** | 4 | 5 | 6 | 8 | 12 | 16 | 32 | 4 | 1920 | yes |
| soc | — | **1** | 4 | 5 | 6 | 8 | 12 | 16 | 32 | 6 | 480 | yes |
| advanced_soc | — | — | **1** | 1 | 1 | 2 | 3 | 4 | 8 | 6 | 1920 | yes |
| highly_advanced_soc | — | — | — | — | — | **1** | 1 | 2 | 4 | 6 | 7680 | yes |
| nano_cpu | **1** | 4 | 8 | 10 | 12 | 16 | 24 | 32 | 64 | 8 | 480 | yes |
| qbit_cpu | **1** | 4 | 8 | 10 | 12 | 16 | 24 | 32 | 64 | 4 | 1920 | yes |
| hpic | — | **1** | 4 | 5 | 6 | 8 | 12 | 16 | 32 | 2 | 7680 | yes |
| uhpic | — | **1** | 4 | 5 | 6 | 8 | 12 | 16 | 32 | 2 | 30720 | yes |

Dies per wafer, cut EU/t and clean room are GT's own cutter recipes, unchanged.

> TRAPS: chip items without `_chip`: `simple_soc`, `soc`, `advanced_soc`, `highly_advanced_soc`. Reticle ids use the old short names (`nand_reticle`, `nor_reticle`, `mpic_reticle` "PIC Reticle"). GT's engraving prefixes: `engrave_ssoc`, `engrave_asoc`, `engrave_hasoc`, `engrave_pic`.

## 5.3b AF9's own chips

Nine chips beyond GT's (items: `kubejs/startup_scripts/gtceu/chips.js`; recipes: `AF9_WAFERS.chips` `own(...)` in `server_scripts/mods/gtceu/photolithography.js`). Printed like GT's (a substrate prints every chip whose own substrate is the same or lower), but their chip wafers are AF9's: `kubejs:<chip>_wafer` → Cutter → `kubejs:<chip>_chip`; `kubejs:contaminated_<chip>_chip` for the contamination, `kubejs:<chip>_reticle`. Chip wafers per print: 1 on the chip's own substrate, above it the substrate's yield ÷ the class divisor (`CLASS_DIVISOR`: trinium 10, naquadria 12, transmuted neutronium 24, like GT's 1 / 2 / 8 / 16).

| chip | what | own substrate | reticle: lens / mask blank | dies / wafer | cut |
|---|---|---|---|---|---|
| `rf_transceiver` | RF transceiver: a radio on a chip | silicon (350 nm) | lime / chrome | 8 | MV |
| `apu` | APU: CPU and GPU on one die | silicon | magenta / chrome | 6 | MV |
| `mcu` | microcontroller | silicon | white (GT's Glass Lens) / chrome | 16 | MV |
| `asic` | application-specific IC | phosphorus (200 nm) | light gray / chrome | 8 | HV, clean room |
| `edram` | embedded DRAM (cache) | trinium (80 nm) | green / MoSi phase-shift | 16 | IV, clean room |
| `mram` | magnetoresistive RAM | trinium | blue / MoSi phase-shift | 16 | IV, clean room |
| `feram` | ferroelectric RAM | trinium | yellow / MoSi phase-shift | 16 | IV, clean room |
| `vpu` | video processing unit | naquadria (65 nm) | purple / MoSi phase-shift | 6 | LuV, clean room |
| `tpu` | tensor processing unit (AI) | transmuted neutronium (20 nm) | orange / EUV multilayer | 4 | UV, clean room |

The 16 dye lens colours: GT's 12 chips have 12 of them on the chrome blank; the four left (white, lime, magenta, light gray) go to the four silicon / phosphorus chips. The finer chips' reticles are written on other blanks (`kubejs:phase_shift_mask_blank`, `kubejs:euv_mask_blank`), so their lens colours can repeat without two laser-engraver recipes matching the same inputs. Packages: `kubejs:edram_cpu_package` / `kubejs:edram_soc_package` (§6.4). Their uses: §8 (`server_scripts/mods/gtceu/chip_uses.js` and the scripts it names). Textures: `kubejs/assets/kubejs/textures/item` (`chips/`, `wafers/`, `<chip>_reticle`, the blanks, the packages), made on GT's own templates: GT's blank die recoloured to the substrate's class (grey silicon, GT's SoC copper for phosphorus, the substrate wafer's tones above) with the chip's 6×6 glyph in the die's two darkest tones, lit from the top left like GT's; the chip wafer is the substrate wafer with the glyph; the contaminated chip carries the smudges of GT's contaminated CPU chip; reticles: the chrome reticle frame, lens-coloured corners, the glyph in the blank's absorber (chrome, MoSi, TaBN on the Mo/Si multilayer).

## 5.4 Vacuum cleanliness and broken wafers

Every lithography machine keeps an exposure vacuum, a **cleanliness score 0-100** (`LithoMachine`, persisted):

- **Starts at 0** when the structure forms: `onStructureInvalid` (a broken or changed structure) sets `c = 0`; a normal unload keeps it.
- Every 10 ticks while formed, the pumps draw 1/8 A of the hatch voltage (×10 ticks) **all the time** (pumping down or holding). Paid: `c` rises **linearly to 100 in 10 s × level** (Mk1 line V1-V3 = 10-30 s, Mk2 scanner V1-V2 = 40-50 s) and stays at 100. The orbital station has no vacuum (it is space): see its start-up in §5.6.
- Not paid for 3 s (`POWER_GRACE_TICKS`, so a short dip does not flicker): it **vents linearly, 100 → 0 in 60 s**.
- State (persisted, shown on the console and in Jade): pumping down / sealed / venting / off. Finished wafers do **not** lower `c`; maintenance problems do not touch the vacuum.
- **Prints only start on a sealed vacuum** (`c` = 100): `LITHO_GATE` refuses the recipe until then and GT keeps retrying it, so the machine starts by itself once sealed. Console and Jade status `PUMPING` (code 9) meanwhile.
- **Break roll** when a print finishes (`LithoRecipeLogic.onRecipeFinish`, before the outputs are handed out), from the **lowest `c` the print went through** (`printLow`, persisted: set when the print starts, lowered every vacuum update while the recipe logic is active, so also while it waits for power): `p = (base + (100 − low)/100 × 0.5) × 0.75^surplus`, at most 0.95; `surplus` = line versions above the mode (0 on the orbital station). A broken print puts out `kubejs:broken_<substrate>_wafer` (same count) instead of the printed wafer.
- The recipes list the broken wafer as a chanced output at the base chance (for EMI); `STRIP_BROKEN` removes it before the run, so only the roll decides. `alwaysTryModifyRecipe` = true, so every run starts from the original recipe.
- A power cut mid-print vents the chamber after 3 s: a 30 s cut leaves the print at about 55, +22 % break chance; the console shows "this print dipped to …". Jade leaves GT's own run-time bar out for these machines (they have their own).
- Broken wafers: macerator → 2 small silicon dust (chromodynium: small chromodynium dust), `af9:reclaim_broken_<substrate>_wafer`.

## 5.5 Versions (like the Assembly Line's length)

Mk1 line and Mk2 scanner grow by projection-lens slices; the light source must allow the version.

| Machine | Version | Lens slices | Light source (must allow it) | Runs modes | Length | Vacuum pump-down |
|---|---|---|---|---|---|---|
| Mk1 line | 1 | 3 tempered glass | purple lamp (mercury i-line) | 350 nm | 10 | 10 s |
| Mk1 line | 2 | 4 | `kubejs:krf_excimer_laser` | + 200 nm | 11 | 20 s |
| Mk1 line | 3 | 5 | `kubejs:arf_excimer_laser` | + 100 nm | 12 | 30 s |
| Mk2 scanner | 1 | 4 × 6 laminated glass | `kubejs:arf_excimer_laser` | 80 nm | 11 | 40 s |
| Mk2 scanner | 2 | 5 × 6 | ArF | + 65 nm | 12 | 50 s |
| Orbital station | — | — | (X-ray FEL; EUV Light Source in the controller's EUV slot or an input bus for 20 / 7 nm; the reticle only in the controller's reticle slot) | 50, 20, 7, 1 nm | 25 × 25 × 18 | 60 s |

Version = min(lens slices − (V1 slices − 1), light cap), read on structure formation (`PhotolithographyLineMachine.Spec`: `MK1` lamp 1, KrF 2, ArF 3; `MK2` ArF 2). `LITHO_GATE` refuses modes above the version (console LOCKED, the tile shows `V<n>` in red). Per version above a mode: run time ×0.8 (`LITHO_VERSION`, before the overclock) and break chance ×0.75 (in the roll). EMI shows one structure page per version (`versionShapes(definition, spec)`).

Light sources (assembler): KrF laser HV, ArF laser EV; `af9:euv_light_source` = UV hull + 4 UV emitters + 4 UV circuits + 8 glass lenses + 2 UV pumps + 8 neutronium plates + 2304 mB molten tin, 2400t UV: not a structure block any more but a not-consumed input of the 20 and 7 nm prints (the station's EUV source). Mk2: `af9:photolithography_scanner`, IV assembler (IV hull, 4 IV circuits, 2 emitters, 2 sensors, 2 robot arms, 4 motors, 2 pumps, 8 glass lenses, 8 tungstensteel plates, 1152 soldering alloy). Orbital station: ZPM assembler (it prints from 50 nm on): ZPM hull, 4 ZPM emitters, 4 ZPM field generators, 4 ZPM circuits, 4 ZPM sensors, 4 ZPM robot arms, 16 naquadah alloy plates, 4000 supercooled endion.

Consoles: the mode tiles are a status indicator (active mode lit, locked modes with their version), not buttons; GT's side tab switches the mode. The same for the fab and process consoles.

## 5.6 Structures

Mk1 line (3×3×10-12, built from plascrete; aisles **front → back**, `FactoryBlockPattern.start(LEFT, UP, BACK)`, so the controller comes before the repeatable lens aisle and GT's auto-build places it right; each aisle bottom/middle/top): `III/IMI/CFC` cassette station + controller, `CSC/WXW/FPF` prime + spin coater, `CKC/CHC/FFF` bake + chill plates, `CSC/WXW/FPF` developer, `CRC/WRW/CCC` wafer stage, `CCC/WTW/CCC` × 3-5 projection lens (`setRepeatable(3, 5)`), `CCC/CRC/CCC` reticle stage, `CCC/CLC/CCC` light source; `L` = purple lamp / KrF / ArF, `C`/`I` = `gtceu:plascrete` (or hatches/buses), `X` = solid steel casing, `P` = `kubejs:plascrete_pipe_casing` (an MV machine: no PTFE blocks, GT's PTFE only comes at HV), `F` = `kubejs:plascrete_filter_casing` (MV fan filter units, see §11.3). Plascrete Pipe Casing: shaped `PIP/IFI/PIP` (plascrete, polyethylene fluid pipe, steel frame) → 2, `af9:plascrete_pipe_casing`. `C` = plascrete or up to 2 energy hatches, up to 8 fluid inputs, up to 1 maintenance; `I` = up to 2 item input + 2 output buses at the controller. Every part has a maximum only, never a required count (all AF9 multiblocks, `setMaxGlobalLimited(max, preview count)`); a print needs 4A of its tier, i.e. two normal energy hatches.

Mk2 scanner (the user's build: a cleanroom tube 3×3, 10 long at V1 and 12 at V2, plascrete; aisles front → back like the line, rows bottom → top; `PhotolithographyScannerMachine`): `CCC/CMC/CCC` front + controller, `CCC/C#C/CFC` cap, `CGC/W#W/CGC` ×2 front window section (G `gtceu:stainless_steel_gearbox` wafer stages, W `gtceu:cleanroom_glass`), `CPC/C#C/CFC` ×2 track (P `kubejs:plascrete_pipe_casing`, F `kubejs:plascrete_filter_casing`), `CGC/W#W/CGC` back window run (`setRepeatable(2, 4)`: 2 aisles a version), `CCC/C#C/CFC` cap, `CCC/CCC/CCC` back; `#` air (the tube). Parts on any plascrete `C`: 2 + 2 item buses, 2 energy, 8 fluid inputs, 1 maintenance (maximums). Version = window sections − 1 (`Spec` MK2: `cleanroom_glass`, 4 per slice, 2 slices at V1, `aislesPerSlice` 2; no light blocks: the ArF Excimer Laser is a kept recipe input of the 80 / 65 nm prints, in the laser slot of the screen (`laserSlot`, like the orbital station's EUV slot) or an input bus; status NO LASER (14) without it). Screen: `console/ScannerConsoleWidget` in `console/ScannerUIWidget` (the orbital station's layout: clickable node tiles, the optical column and the wafer, the laser slot, switches, side panels for the vacuum, version, immersion and laser, and the system).

Light ring (`LightRingRender`, GT's fusion ring for any `ILightRingMachine`): while the orbital station prints, a glowing torus lies just inside the rim at the exposure deck (centre 3 behind = below the controller, radius 9.6, tube 0.25, across the controller's front axis: clear of the rim everywhere, it only crosses the four cross beams), pulsing between the node's colour (`LithoMode.argb`) and white every 50 ticks and fading out when the print stops; with Shimmer the tube and its core also bloom. A white-hot core runs inside the tube (0.45 of its radius); around it a wide glow in the node's colour (four wider, fainter tori: 1.7 / 2.8 / 4.5 / 7 times the tube at 45 / 26 / 14 / 6 %, breathing with the pulse) and 2-4 particles a tick along the ring (electric sparks, dust in the node's colour, end-rod glints). Set in the startup script: `AF9MachineModels.workableCasingWithLightRing(casing, overlay, up, back, radius, thickness, normal)` + `.hasBER(true)`, the numbers from `OrbitalLithographyMachine.RING_UP / RING_BACK / RING_RADIUS / RING_THICKNESS`. One render object serves every station of the model, so the fade-out, the last colour and the effects are kept per machine (a station only glows while it prints itself). The ring and its lightning are drawn after the translucent blocks (`LightRingRender.Deferred`, Forge's `RenderLevelStageEvent` AFTER_TRANSLUCENT_BLOCKS; gathered in the block entity pass) in `client/render/AF9RenderTypes` that test depth but never write it, each torus its own strip: blocks in front hide the ring, and see-through blocks behind its glow (GT's frames, glass) stay visible. Drawn in the block entity pass with GT's light ring type, the glow wrote depth first and those blocks vanished behind it (GT's frames are translucent). Shimmer (installed in the pack) draws its bloom into the world's own depth buffer (`CopyDepthColorTarget.hookDepthBuffer`) before the translucent blocks, so the bloom (tube and core only) uses the same depth-less type, and the ring itself and its lightning are drawn after the translucent blocks with or without Shimmer; a ring is drawn once a frame. The tori are quads (they can share any batch). With a shader pack (Oculus, checked through its API by reflection: `client/render/IrisCompat`) the pack draws the world and anything drawn after the translucent blocks or through Shimmer is lost: the ring and its lightning then go through the pack with the block entities, in lightning's shader (`AF9RenderTypes.SHADER_RING`: a pack swaps vanilla's colour shader for its plain basic program, lightning's for its lightning program, which it lights up), and not into the shadow map. The shader ring and the lightning bind a plain white texture (`af9:textures/misc/white.png`): lightning's shader has none, but a pack's lightning program may sample the bound one (Photon multiplies by it and drops fragments under 0.1 alpha, so a leftover texture hid the ring; Photon also draws all lightning white, so there the ring is white, Complementary keeps its colour).

Deadly ring: while it is lit, every 2 ticks the station burns every living thing within `RING_BURN` = 0.625 of the ring's core circle (checked at five heights of the entity's middle line, its half width added; players in creative or spectator mode are left alone): set on fire, flames, lava sparks and a fire-charge whoosh, then damage `af9:orbital_ring` (`data/af9/damage_type/orbital_ring.json`, burning effects, never scaled; tagged `bypasses_armor`, `bypasses_effects`, `bypasses_enchantments`, `bypasses_shield`) of 10^6: dead, totems aside. Death message `death.attack.af9.orbital_ring`: "... touched the light ring of an Orbital Lithography Station and was vaporized". A player it kills gets `RingDeathPacket` (AF9 Core's network channel `af9:main`), and `client/RingDeathOverlay` swaps the vanilla death screen for `client/VaporizedScreen` (at once if it is open, else as it opens; the death message read from the vanilla screen): a pixel "WASTED". The red veil, a white-hot flash (0.6 s), stepped heat edges, a black band across the middle of the screen (dithered ember rims) opening from its centre line, VAPORIZED in the Minecraft font scaled 4x, dropping in from twice that, shaking during the flash and flickering like fire; under it the death message (wrapped) and the score, then its own pixel buttons (notched frames, orange when hovered, working after 20 ticks): respawn (spectate in hardcore) and title screen, doing what vanilla's do (the title button goes through the chat-report check and asks first). Embers rise at the sides. It is a `DeathScreen` subclass, so everything that closes the death screen on respawn closes it; it ends when the player is alive again.

Sound (`common/AF9Sounds`, `assets/af9/sounds.json`, vanilla sounds pitched down, attenuation 40-64): `af9:orbital_station` is the working sound of the four orbital recipe types (GT loops it while a print runs: the beacon hum an octave down); the ring adds `af9:orbital_station_pulse` (warden heartbeat, half pitch) on every white flash and `af9:orbital_station_ignite` (the end portal opening at 0.6 pitch) when it lights up.

Batch mode instead of a parallel hatch: modifiers `LITHO_GATE, STRIP_BROKEN, COOLANT, OC_PERFECT, BATCH_MODE`; GT's batch runs several prints as one once overclocks bring a print under `batchDuration` (100 ticks), multiplying inputs (coolant included), outputs and time. Every print of a run rolls its own break (`LithoMachine.finishPrints`: parallels × batch rolls; the broken ones give a broken wafer each, the rest their chip wafers).

Coolant (`OrbitalLithographyMachine.COOLANT`, a recipe modifier after `STRIP_BROKEN` and before `BATCH_MODE`): every orbital print draws a supercooled fluid from the coolant hatches (`litho/Coolant`, grades hydrogen 1 < argon 2 < xenon 3 < endion 4). Per node: minimum / best / mB per print: 50 nm hydrogen / xenon / 100, 20 nm argon / endion / 150, 7 nm xenon / endion / 250, 1 nm endion / endion / 500. The station takes the best useful coolant in its hatches (best grade down to the minimum, then colder ones), adds it as a fluid input (batch mode multiplies it), shortens the run ×0.9 per grade above the minimum (up to the best) and stores the coolant in the recipe data (`af9_coolant`); the break roll multiplies the chance by ×0.8 per such grade. No usable coolant: console / Jade status NO COOLANT. The 1 nm recipes no longer list supercooled endion; EMI shows three coolant lines per mode (amount per print, minimum, best), and every coolant is named by its full fluid name (`af9.litho.coolant.<id>` = "Supercooled Argon" ...), never just the gas: the dense fluids do not count. The PROCESS panel shows the coolant's state on its row (NEEDS / MIN / +n COLDER) and the fluid below it.

Computation and research: the 7 and 1 nm prints draw 32 / 96 CWU/t (`.CWUt`, GT's computation capability, through a Computation Hatch). Every 1 nm print is researched on its own, like GT's assembly line: `stationResearch` on the chip's reticle (`af9_litho_1nm_<chip>`, 2A of ZPM, 48 CWU/t, 256000 CWU: about 267 s) gives a data orb for the station's data hatch (or a data bank). The Research Station recipes (`gtceu:research_station/af9_litho_1nm_<chip>`: the reticle + an empty data orb into the orb with the research) only exist because `PhotolithographyLineMachine.registerRecipeInfo` gives the 1 nm type GT's research build hook (`onRecipeBuild(ResearchManager::createDefaultResearchRecipe)`, which GT sets on its assembly line type only; without it `stationResearch` just registers the research and no recipe writes it). `LITHO_GATE` refuses researched prints on a machine without a data hatch (GT's data hatches only block what they do not hold). Status NO COMPUTATION when the computation hatches cannot supply the node (their optical network's `getMaxCWUt()`, 0 without a hatch or without an HPCA behind it; the PROCESS panel shows it as available / needed CWU/t), NO DATA without a data hatch. EMI: GT puts its "Min. Computation" and "Requires Research" lines on the same row; `PhotolithographyLineMachine.respaceTexts` (the recipe type's UI builder) stacks them again, and the page gets one line more for each (`setMaxTooltips(4)`, `setMinRecipeConditions(1)`).

EMI / JEI page of every lithography recipe (`litho/LithoRecipeUI`, installed on each recipe type in common setup with `setRecipeUI`, keeping the type's slot overlays, progress bar and tooltip count; 176 × 66 plus the text lines): laid out like an assembly line. The items on top (blank wafer, reticle, EUV Light Source or dry resist cartridge), the track chemicals below in rows of four; a pipe off every row into a manifold, the manifold into the machine, level with the first track row (its pipe runs straight on into the machine): its controller drawn large in a frame glowing in the node's colour, the node above it ("20 nm"), the machine below (MK1 LINE / MK2 SCANNER / ORBITAL STATION, clear of the slots; the frame's tooltip names it and the light); GT's arrow, then the outputs stacked (the printed wafers, the chanced broken wafer: that is the break chance, the page has no break line). A 1 nm print's research: the data orb in its own row under the track (the dry page's second row: "RESEARCH" and a data line into the manifold), written with the recipe's research the way GT's assembly line's research slot writes it (`ResearchManager.writeResearchToNBT`, a catalyst), its hover naming the research as its Research Station recipe does it (found on the client by the research on its output orb, or by its id `research_station/<research id>`): e.g. 'Research Station: CPU Reticle + an empty Data Orb, both in its Object Holder' and '2A of ZPM (245,760 EU/t) and 48 CWU/t, 256,000 CWU in all: 267 s' (the recipe's EU/t and CWU/t; a research's duration is its total CWU), then that the orb goes in the station's data hatch; GT's "Requires Research" line stays under the page. Dashes flow along the pipes to the machine: in the node's colour from the items, in the colours of each row's fluids from the track (`litho/LithoFlowWidget`, the fluids' tints). The 1 nm dry process shows DRY PROCESS where the track would be ("no track chemicals", clear of the manifold and the research row; its hover says the resist comes dry, from the Dry Resist Cartridge). The slots are GT's own (same ids: GT binds the recipe to them, EMI shows and looks them up as usual); only the template is ours.

Start-up (instead of a vacuum: space is one): switched on (GT's work toggle) and powered, the station's systems draw the lines' pump power (1/8 A of the hatch voltage) and start up in `STARTUP_SECONDS` = 10 (`startupTicks`, persisted); switched off, or 3 s without power, it shuts down and starts from 0 again. Prints start once it is ready (`LITHO_GATE` reads `isVacuumSealed()`, which the station answers with its start-up) and roll the node's base break chance (the station reports a vacuum of 100: `getCleanliness`, `getPrintVacuum`, `rollVacuum`). Console: START-UP row (OFF / percent / READY, a bar, "starting up, n s left"), status STARTING UP (13); Jade "Start-up n % (starting / ready / shut down)".

Magnetic field (`OrbitalField`, Ad Astra's `EntityGravityEvent`, `compat/adastra/AdAstraCompat`): while the station is formed, switched on (GT's work toggle) and powered (from the start-up's first second), every entity inside its field (the station's own box, 12 to each side and 17 below the controller, and 4 blocks above the deck; nothing around it) falls with `orbitalField.gravity` = 1.0 (Earth) instead of Ad Astra's orbit gravity 0: you stand on the deck and jump about 1.25 blocks. Gravity is only raised, never lowered. Both sides keep their loaded stations (the player moves client-side); `fieldActive` is synced.

Screen (`OrbitalStationUIWidget` = GT's `FancyMachineUIWidget` + two panels beside the player inventory, page `OrbitalConsoleWidget` 384 × 148, one synced state): left the exposure field (the four node tiles; the wafer being printed, a disc with its die grid exposed die by die in serpentine order in the node's colour, a scan slit on the current die and a beam from the optics, a progress ring with a running glint; the state and the run-time bar), right the exposure panel (node and light, the product, the reticle slot and the EUV Light Source slot side by side, each with its state, the ONLINE / OFFLINE switch = GT's work toggle, break chance, counters + reset, a hint for the current state). Beside the inventory: PROCESS (vacuum, coolant, computation, magnetic field, orbit) and SYSTEM (status, power available / needed, energy tier, batch, switch). A BATCH switch sits next to ONLINE / OFFLINE (GT's batch toggle, also on GT's button panel). The panels only show on the station's own page. EUV slot: `euvSlot`, a 1-slot `NotifiableItemStackHandler` (IO.IN, only `kubejs:euv_light_source`) on the controller: GT attaches a controller's own recipe handlers, so the prints' not-consumed EUV source is found there like in an input bus; it drops when the controller is broken. Reticle slot: `reticleSlot`, the same kind of handler (only `kubejs:*_reticle`). The prints keep their reticle as a not-consumed input (without it every chip's print of a node would have the same inputs, and GT's recipe lookup drops conflicting recipes; EMI also shows which reticle), but the station only runs a print whose reticle is the one in the slot: `LITHO_GATE` asks the machine (`LithoMachine.canRun`), and the station compares the recipe's reticle with the slot. A reticle in an input bus does not count; an empty slot shows NO RETICLE (status 12). Both slots have capability IO NONE (no pipes), and GT's handler then refuses every insert, the screen's slots too: they work on the handlers' `storage` (like GT's bus screens).

Facing: `RotationState.ALL` + extended facing; the controller faces up out of the middle of the top deck (placed looking down). The pattern is `start(RIGHT, FRONT, UP)`: rows along the controller's front, aisles along its up; with the controller facing up (upwards north) every block sits where the old horizontal controller (facing north) had it, so an existing station only needs its controller turned up. GT draws previews for a controller facing north, which would stand the platform on its edge: `OrbitalLithographyMachine.previewShapes` turns GT's preview into the controller-up orientation (the linear map of GT's `setActualRelativeOffset` for (north, up) and (up, north)) and faces the controller up.

Orbital station (the orbital array), 25 × 25 × 18: the user's `sol_array` pattern, taken over unchanged (25 aisles of 18 rows, rows bottom → top; `FactoryBlockPattern.start(RIGHT, FRONT, UP)` for the controller facing up, see above). The controller `K` is in the middle of the top deck (aisle 12, row 17). `O` also takes 1 Computation Hatch, 1 Data Access Hatch and 1 Optical Data Hatch (maximums).

| Letter | Block | Count | Part of the station |
|---|---|---|---|
| `O` | `gtceu:inert_machine_casing` or parts | 32 | top deck (PTFE): controller and every hatch, nowhere else |
| `L` | `gtceu:stress_proof_casing` | 101 | deck plate |
| `H` | `gtceu:shock_proof_cutting_casing` | 73 | exposure deck, cross beams |
| `D` | `gtceu:sturdy_machine_casing` | 96 | outer ring |
| `C` | `gtceu:nonconducting_casing` | 288 | spokes and rims |
| `F` | `gtceu:hsss_frame` | 88 | trusses |
| `A` | `gtceu:hsse_frame` | 54 | undulator mast (12 blocks below the platform) |
| `B` | `gtceu:hssg_coil_block` | 26 | undulator magnets (the mast's core) |
| ` ` | anything | | |

`O` parts, maximums only: up to 1 **laser** hatch and 3 energy hatches (together 50A of UHV) or wireless energy receivers (§17), up to 2 **coolant hatches** (ability `af9_coolant_input`), up to 8 fluid input hatches (the track chemicals), 1 computation hatch, 1 data hatch, item input and output buses, 1 maintenance; no parallel hatch (batch mode instead). Modifiers `LITHO_GATE + STRIP_BROKEN + COOLANT + OC_PERFECT + BATCH_MODE`. Model: inert PTFE casing + GT fusion-reactor overlay. Adapted from the old project: `workableCasingRenderer` → `workableCasingModel`, `ELECTRIC_OVERCLOCK.apply(PERFECT_OVERCLOCK)` → `OC_PERFECT`, the `sol_array` recipe type → `orbital_lithography`, `autoAbilities`/normal fluid hatches → coolant hatches + item output buses, exact 3 energy hatches → up to 3.

Orbit: `OrbitalLithographyMachine.isOrbit` = dimension path `orbit` or ending in `_orbit` (Ad Astra 1.15: `ad_astra:earth_orbit`, `moon_orbit`, `mars_orbit`, `venus_orbit`, `mercury_orbit`, `glacio_orbit`). Elsewhere it forms but never prints (console NOT IN ORBIT).

## 5.7 Consoles (custom UI)

`LithoConsoleWidget` (230×144; GT's side tabs stay), no scanlines or sweep line:
- header: title (`LITHO LINE` / `LITHO SCANNER` / `ORBITAL LITHO`) + line version, the power gauge (`available/needed EU/t` over a thin bar, right before the status; `ConsoleWidget.drawHeaderPower` drops the unit, then the gauge, when the title leaves no room), status word + LED (OFFLINE, IDLE, RUNNING, NO POWER, PAUSED, MAINTENANCE, LOCKED, NOT IN ORBIT, PUMPING),
- mode tiles (node + substrate symbol Si, P, Nq, Ke, Nq*, Nt, Nt*, Sq, Qc; `V<n>` in red when locked; hover: mode, version needed, power, substrate, light, NA/k1, resist, break chances),
- VACUUM bar (0-100, green ≥80, yellow ≥50, red) with pump state (pumping down / sealed / leaking / off),
- BREAK CHANCE of the running print (from its lowest vacuum) or of the next one (sealed), with "this print dipped to …" or "prints start once sealed" under it,
- PRINTING: the item icon + name of the running print, node + light, substrate, version bonus (`Speed x1.56, breaks x0.56`) or orbit state,
- RUNTIME: full-width progress bar with `12.3 s / 45.0 s` and percent,
- counters PRINTED / BROKEN / YIELD + RESET.

Jade (§16) shows the same: the vacuum bar at the top, then status + mode, what is printed, the run-time bar and the break chance.

## 5.8 Items

| ID | Notes |
|---|---|
| `kubejs:photomask_blank`, `kubejs:<chip>_reticle` (12), `kubejs:molecular_sieve`, `kubejs:saturated_molecular_sieve` | as before |
| `kubejs:trinium_wafer`, `naquadria_wafer`, `transmuted_neutronium_wafer`, `strange_matter_wafer`, `chromodynium_wafer` | the new blank substrates |
| `kubejs:broken_<substrate>_wafer` (9) | failed prints (§5.4) |
| `kubejs:contaminated_<substrate>_wafer` (9) | handled without protection (§15) |
| `kubejs:dry_resist_cartridge` | orbital resist, one per wafer |
| `kubejs:<chip>_wafer`, `<chip>_chip`, `contaminated_<chip>_chip`, `<chip>_reticle` for `rf_transceiver apu mcu asic edram mram feram vpu tpu` | AF9's own chips (§5.3b) |
| `kubejs:phase_shift_mask_blank`, `kubejs:euv_mask_blank` | the finer chips' mask blanks (§6.3) |
| `kubejs:edram_cpu_package`, `kubejs:edram_soc_package` | a CPU / SoC die with two eDRAM dies (§6.4) |

Materials (startup `photolithography.js`): the XCDA / HMDS / i-line / TMAH set as before, plus `tin_tetrachloride` and `euv_photoresist` (tin-oxo methacrylate clusters in PGMEA).

# 6. Recipe chains

## 6.1 Step 0 — Boule (EBF Boule Melting, AF9)

See §12. GT's four EBF boule recipes and the old SMC crystal-growth boules are removed; the SMC crystal growth mode now grows seed crystals.

## 6.2 Step 1 — Blank wafers (cutter)

```text
gtceu:cutter (GT, unchanged)
  gtceu:silicon_boule    → 16x gtceu:silicon_wafer           | 400t  | 64 EU/t
  gtceu:phosphorus_boule → 32x gtceu:phosphorus_wafer        | 800t  | HV  | cleanroom
  gtceu:naquadah_boule   → 64x gtceu:naquadah_wafer          | 1600t | EV  | cleanroom
  gtceu:neutronium_boule → 64x + 32x gtceu:neutronium_wafer  | 2400t | IV  | cleanroom
af9:cut_<substrate>_boule (gtceu:cutter, 250 mB lubricant, cleanroom)
  kubejs:trinium_boule        → 64x kubejs:trinium_wafer        | 2000t | IV
  kubejs:naquadria_boule      → 80x kubejs:naquadria_wafer      | 2200t | LuV
  kubejs:strange_matter_boule → 96x kubejs:strange_matter_wafer | 2800t | UV
  kubejs:chromodynium_boule   → 128x kubejs:chromodynium_wafer  | 3200t | UHV
```

`kubejs:transmuted_neutronium_wafer` has no boule: the Particle Accelerator irradiates neutronium wafers (§14).
Real analogue: wire-saw + lap + edge + RCA + CMP.

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

```text
af9:wafer_coater (gtceu:assembler, MV)
  gtceu:mv_machine_hull + 2x mv_electric_pump + 2x mv_electric_motor + mv_robot_arm + 4x #gtceu:circuits/mv
  + 8x stainless_steel_plate + 4x kubejs:plascrete_pipe_casing + 288mB soldering_alloy → gtceu:wafer_coater | 400t | MV

af9:ule_glass_substrate (gtceu:fab_calcination, 1800 K)
  gtceu:quartzite_plate + gtceu:rutile_dust → kubejs:ule_glass_substrate | 600t | HV
af9:mo_si_mirror (gtceu:fab_cvd, 1200 K)
  kubejs:ule_glass_substrate + 4x molybdenum_dust + 4x silicon_dust + 2000mB argon → kubejs:mo_si_mirror | 1800t | LuV
  (EUV optics: the EUV Light Source takes 2, the Orbital Lithography Station 6, §18.10)

af9:phase_shift_mask_blank (gtceu:fab_cvd, clean room, 900 K)
  gtceu:quartzite_plate + gtceu:small_molybdenum_dust + gtceu:small_silicon_dust + 100mB gtceu:arf_photoresist
  → kubejs:phase_shift_mask_blank | 600t | EV
af9:euv_mask_blank (gtceu:fab_cvd, clean room, 1200 K)
  gtceu:quartzite_plate + 2x gtceu:molybdenum_dust + 2x gtceu:silicon_dust + 100mB gtceu:euv_photoresist + 1000mB gtceu:argon
  → kubejs:euv_mask_blank | 1200t | LuV
```

Real analogue: stepper build + chrome-on-quartz mask blank (pre-coated resist); an attenuated phase-shift blank (MoSi film, 6 % transmission, 180° shift) for 100 to 65 nm; an EUV blank (40 Mo/Si bilayers on low-expansion glass, sputtered; the Ru cap and the TaBN absorber are not modelled). Glass lenses = projection optics, steel = stages, emitter/sensor/arms/motors/pumps = robots + focus + dispense.

## 6.4 Step 2b — Reticles (laser_engraver)

The mask has to fit the light (§18.9): **chrome** reticles print 350 and 200 nm, **phase-shift** (PSM) reticles 100, 80 and 65 nm, **EUV** reticles 50, 20, 7 and 1 nm. A chip has a reticle of its own (native) class, `kubejs:<chip>_reticle`, and one of every finer class, `kubejs:<chip>_psm_reticle` and `kubejs:<chip>_euv_reticle` (67 items, 27 chips; `startup_scripts/gtceu/reticles.js`, `AF9_WAFERS.reticles`).

```text
native class (the class of the chip's own substrate: silicon, phosphorus -> chrome; naquadah, trinium, naquadria -> psm; neutronium and up -> euv)
af9:<chip>_reticle (gtceu:laser_engraver)
  <the class's blank> + notConsumable #forge:lenses/<color> → kubejs:<chip>_reticle | 1800t | chrome MV, psm EV, euv ZPM
finer classes (the chip's own reticle is the master, kept)
af9:<chip>_<psm|euv>_reticle (gtceu:laser_engraver)
  <the class's blank> + notConsumable kubejs:<chip>_reticle → kubejs:<chip>_<class>_reticle | 1800t | psm EV, euv ZPM

blanks: chrome kubejs:photomask_blank, psm kubejs:phase_shift_mask_blank, euv kubejs:euv_mask_blank

lens colours (own reticle; the colours of one class's chips differ, so a blank + lens is one recipe):
chrome  ilc red, ram green, cpu light_blue, ulpic blue, lpic orange, simple_soc cyan, nand gray, nor pink, mpic brown, soc yellow,
        rf_transceiver lime, apu magenta, mcu white, asic light_gray
psm     advanced_soc orange, saw_filter red, edram green, mram blue, feram yellow, photonic_ic cyan, vpu purple, spin_logic lime
euv     highly_advanced_soc black, tpu orange, tmd_logic pink, memristor cyan, quantum_dot_ic yellow
```

A print takes the reticle of its node's class (`AF9_WAFERS.reticleItem(chip, maskClass(substrate))`); the 1 nm prints and their research (the Research Station scans the EUV reticle) the EUV one. Why a master and not the lens for the finer classes: there are 16 lens colours, but 22 chips with a PSM reticle and 27 with an EUV one.

eDRAM packages (gtceu:assembler, IV, clean room): `gtceu:cpu_chip` or `gtceu:soc` + 2x `kubejs:edram_chip` + `gtceu:epoxy_plate` + 4x `gtceu:fine_gold_wire` + 72mB `gtceu:soldering_alloy` → `kubejs:edram_cpu_package` / `kubejs:edram_soc_package` | 400t.

Removed base (all substrates): `gtceu:laser_engraver/engrave_<ilc|ram|cpu|ulpic|lpic|ssoc|nand|nor|pic|soc|asoc|hasoc>_<silicon|phosphorus|naquadah|neutronium>`, **except** `engrave_ulpic_silicon` (MV bootstrap, §6.8: MV Energy Hatches need ULPIC before the line can run).

## 6.5 Step 2c — Chemistries

### Extreme clean dry air (XCDA)

A fab's clean-dry-air plant, all five steps in the purification mode of the SMC Fractionating Still or Rectification Column (§11). The whole chain runs at MV but
slowly: the MV cold step is a Joule-Thomson expander that only chills a quarter of its air per pass (the rest comes back
as `dry_air`). HV adds a platinum oxidizer, a caustic scrubber and liquid-air cooling, which are 4× faster each and turn
all of the air cold. Circuits 1/2 keep the two cold steps apart.

```text
support
af9:hopcalite (synthesis) copper_dust + 6x pyrolusite_dust + 1000 oxygen → 7x hopcalite_dust | 400t MV   (Cu + 2 MnO2 + O → CuMn2O4)
af9:molecular_sieve (wet) 4x zeolite_dust + bentonite_dust + 500 distilled_water → 4x kubejs:molecular_sieve | 600t MV   (bentonite binder: clay + distilled water is GT's clay recipe)
af9:regenerate_molecular_sieve (smelting) kubejs:saturated_molecular_sieve → kubejs:molecular_sieve

1 oxidize   af9:xcda_oxidize_hopcalite  NC hopcalite_dust + 4000 air → 4000 oxidized_air | 600t MV
            af9:xcda_oxidize_platinum   NC platinum_dust  + 4000 air → 4000 oxidized_air | 150t HV | cleanroom
2 scrub CO2 af9:xcda_scrub_lime         small_calcium_hydroxide_dust + 4000 oxidized_air → small_calcite_dust + 4000 decarbonated_air | 400t MV
            af9:xcda_scrub_caustic      small_sodium_hydroxide_dust  + 4000 oxidized_air → small_soda_ash_dust + 4000 decarbonated_air | 100t HV | cleanroom
3 dry       af9:xcda_dry                kubejs:molecular_sieve + 4000 decarbonated_air → kubejs:saturated_molecular_sieve + 4000 dry_air | 400t MV
4 cool      af9:xcda_cool_expansion     circuit 1, 4000 dry_air → 1000 cryogenic_supercooled_air + 3000 dry_air | 800t MV
            af9:xcda_cool_liquid_air    circuit 2, 4000 dry_air + 1000 liquid_air → 4000 cryogenic_supercooled_air + 1000 air | 200t HV | cleanroom
5 filter    af9:xcda_filter             NC gtceu:fluid_filter + 4000 cryogenic_supercooled_air → 4000 extreme_clean_dry_air | 200t MV
```

Throughput with one reactor per step: MV ≈ 1000 XCDA per 40 s (the expander is the bottleneck; a 350 nm print uses 1000 per
45 s), HV ≈ 4000 per 10 s. Liquid air is GT's HV vacuum freezer recipe (4000 air → 4000 liquid air), but the Vacuum Freezer needs EV circuits, so in practice the liquid-air step arrives with EV circuits. Cryogenic Supercooled Air also feeds the air-separation cold box (§6.14).

### Laser gases, resists and immersion water

Moved to the fab chemistry lines: excimer premix §6.14, KrF resist §6.15, ArF resist and ultrapure water §6.16.

### i-line resist, HMDS and developer (350 nm)

GT's formaldehyde (methanol + O2 over silver) is an HV recipe, and HV circuits need printed chips (the 350 nm mode is the only source), so the i-line resist used to be unreachable at MV. AF9 adds the Formox process at MV. TMAH is made the electronic-grade way, by membrane electrolysis of the chloride (a KOH metathesis would leave potassium in the developer, which shifts transistor thresholds); the chloride is dissolved first because GT's circuit-less distilled-water electrolysis would otherwise take the cell.

```text
af9:molybdenum_trioxide (calcination) molybdenite_dust + 7000 oxygen → molybdenum_trioxide_dust + 2000 sulfur_dioxide | 300t MV | 900K
af9:iron_molybdate (calcination) hematite_dust + 3x molybdenum_trioxide_dust → iron_molybdate_dust | 300t MV | 800K
af9:formaldehyde_formox (synthesis) NC iron_molybdate_dust + 1000 methanol + 3000 air → 1000 formaldehyde + 1000 water | 200t MV
af9:novolac_resin (synthesis) NC 100 hydrochloric_acid + 1000 phenol + 1000 formaldehyde → 1000 novolac_resin + 1000 water | 400t MV
af9:diazonaphthoquinone (synthesis) 1000 naphthalene + 1000 nitric_acid + 1000 ammonia → diazonaphthoquinone_dust + 2000 water + 1000 hydrogen | 600t MV
af9:photoresist (blending) diazonaphthoquinone_dust + 1000 novolac_resin + 3000 dimethylbenzene → 4000 photoresist | 400t MV
af9:trimethylchlorosilane (synthesis) magnesium_dust + 1000 dimethyldichlorosilane + 1000 chloromethane → 3x magnesium_chloride_dust + 1000 trimethylchlorosilane | 300t MV
af9:hexamethyldisilazane (synthesis) 2000 trimethylchlorosilane + 3000 ammonia → 4x ammonium_chloride_dust + 1000 hexamethyldisilazane | 400t MV
af9:hmds_vapor (blending) 100 hexamethyldisilazane + 900 nitrogen → 1000 hmds_vapor | 100t LV
af9:tetramethylammonium_chloride (synthesis) 1000 dimethylamine + 2000 chloromethane → tetramethylammonium_chloride_dust + 1000 hydrochloric_acid | 300t MV
af9:tetramethylammonium_chloride_solution (blending) tetramethylammonium_chloride_dust + 5000 distilled_water → 5000 tetramethylammonium_chloride_solution | 100t LV
af9:tmah_developer (electrolysis) 5000 tetramethylammonium_chloride_solution → 5000 tmah_developer + 1000 chlorine + 1000 hydrogen | 300t MV
af9:molecular_sieve (wet) 4x zeolite_dust + bentonite_dust + 500 distilled_water → 4x kubejs:molecular_sieve | 600t MV
```

## 6.5b Step 2d — Coated wafers (Coater Track)

`gtceu:wafer_coater` (assembler, MV), recipe type `gtceu:wafer_coating`, `af9:coat_<substrate>_wafer`: a blank wafer + the node's coating fluids → `kubejs:coated_<substrate>_wafer` (eight, silicon to strange matter; chromodynium has none: the 1 nm station deposits its resist dry on a blank), 300t, `EUt(VA[tier])`. Fluids (mB, `round(base × 1.5^index)`): `hmds_vapor` 40, the node's resist 100, `barc` 60 (200 to 50 nm), `tarc` 60 (65 and 50 nm, immersion); out: `spent_resist_solvent` 30 (the spin spins most of the resist off). Fluid slots in 4, out 2.

Structure (§18.8): 3 × 3 × 6 of plascrete, plascrete pipe casings (dispense lines), a stainless-steel gearbox (spin chuck), heatproof casing (hotplate), plascrete filter casings, cleanroom glass; hatches (maximums): 2 item in, 2 item out, 2 energy, 6 fluid in, 2 fluid out, 1 parallel hatch, 1 Bus Connector, 1 maintenance.

Waste: `af9:recover_resist_solvent` (`fab_fractionation`, HV, clean room): 1000 `spent_resist_solvent` → **600 PGMEA** + 1 carbon dust: 60 % comes back, never all of it.

## 6.6 Step 3 — Print wafers (Photolithography Line, Orbital Lithography Station)

Recipe IDs `af9:print_<chip>_<node>` on `gtceu:lithography_<node>` (158 recipes: 9 + 14 + 16 + 20 + 22 + 24 + 26 + 27) and `gtceu:orbital_lithography` (27). Input: the mode's **coated wafer** (§6.5b) + the reticle of the node's mask class (not consumed, §6.4) + fluids; output: GT's chip wafer(s) (§5.3) + the broken wafer as a chanced output at the base chance (display only, §5.4). 900t, `EUt(VA[tier], 4)`. The recipe count grows with every chip of §5.3b and §18.

Fluids per print (mB): `tmah_developer` 200, `distilled_water` 1000 and `extreme_clean_dry_air` 1000 × `1.5^index`; from 200 nm the etch plasma `etch_plasma_gas` `round(50 × 1.5^index)` (the 350 nm print, an MV print, etches wet); laser gas `round(10 × 1.5^index)` (200-50 nm); `ultrapure_water` 1000 flat (65 and 50 nm, immersion); `hafnium_tetrachloride` 100 flat (50, 20, 7 nm, high-k gate); molten `tin` 144 + `hydrogen` 1000 flat (20 and 7 nm, the EUV plasma source). HMDS, resist, BARC and TARC are the coater's (§6.5b):

| Mode | `tmah_developer` | `distilled_water` | `extreme_clean_dry_air` | `etch_plasma_gas` | laser gas | extras | fluid slots | reticle class |
|---|---|---|---|---|---|---|---|---|
| 350nm | 200 | 1000 | 1000 | — | — | — | 3 | chrome |
| 200nm | 300 | 1500 | 1500 | 75 | 15 KrF | — | 5 | chrome |
| 100nm | 450 | 2250 | 2250 | 113 | 23 ArF | — | 5 | psm |
| 80nm | 675 | 3375 | 3375 | 169 | 34 ArF | — | 5 | psm |
| 65nm | 1013 | 5063 | 5063 | 253 | 51 ArF | UPW | 6 | psm |
| 50nm | 1519 | 7594 | 7594 | 380 | 76 ArF | UPW, HfCl4 | 7 | euv |
| 20nm | 2278 | 11391 | 11391 | 570 | — | tin, H2, HfCl4 | 7 | euv |
| 7nm | 3417 | 17086 | 17086 | 854 | — | tin, H2, HfCl4 | 7 | euv |

Coating per wafer (§6.5b; resist: i-line `photoresist`, KrF, ArF (100 to 50 nm), EUV (20 and 7 nm)):

| Substrate (mode) | `hmds_vapor` | resist | `barc` | `tarc` | spent solvent out |
|---|---|---|---|---|---|
| silicon (350) | 40 | 100 | — | — | 30 |
| phosphorus (200) | 60 | 150 | 90 | — | 45 |
| naquadah (100) | 90 | 225 | 135 | — | 68 |
| trinium (80) | 135 | 338 | 203 | — | 101 |
| naquadria (65) | 203 | 506 | 304 | 304 | 152 |
| neutronium (50) | 304 | 759 | 456 | 456 | 228 |
| transmuted neutronium (20) | 456 | 1139 | — | — | 342 |
| strange matter (7) | 683 | 1709 | — | — | 513 |

(The numbers of the node quests are checked against the recipes by `tools/lint/facts.py`.)

Orbital (`af9:print_<chip>_1nm`): `kubejs:chromodynium_wafer` + `kubejs:dry_resist_cartridge` + reticle (the station's reticle slot) + 500 mB `supercooled_endion` (coolant hatch) → the chromodynium wafer, 35 % chanced broken; 7200t, `EUt(VA[UHV], 50)` = 98,304,000 EU/t (12.5× the 7 nm mode's power × 8× its time = 100× its energy).

EUV resist chain (`photolithography.js`): `af9:tin_tetrachloride` (fab synthesis, cleanroom) tin dust + 4000 chlorine → 1000 SnCl4, EV; `af9:euv_photoresist` (fab synthesis, cleanroom) 1000 SnCl4 + 2000 methacrylic acid + 4000 PGMEA + 1000 ultrapure water → 4000 `euv_photoresist` + 2000 HCl, IV; `af9:dry_resist_cartridge` (fab CVD, 600 K, cleanroom) tungstensteel plate + 1000 EUV resist → cartridge, UV.

Derived wafers (GT's chemistry on any substrate): `af9:chemical_reactor/<chip>_wafer[_suffix]_<substrate>` and the same on `large_chemical_reactor`, cleanroom, GT's EU/t and time: Nano CPU (CPU wafer + 16 carbon fibers + 576 glowstone, EV 1200t), Qubit CPU (Nano CPU wafer + 2 quantum eyes + 288 GaAs, EV 900t; or + 1 InGaP + 50 radon, EV 1200t), HPIC (MPIC wafer + 2 InGaP + 288 vanadium gallium, IV 1200t), UHPIC (HPIC wafer + 8 InGaP + 576 naquadah, LuV 1200t). GT's own recipes stay for GT's wafers.

Removed base: GT's engraving of every chip wafer on every substrate except `engrave_ulpic_silicon` (the MV bootstrap).

## 6.7 Step 4 — Cutting (cutter)

GT's chip wafers are cut by GT's own cutter recipes (dies per wafer, EU/t, clean room: the table of §5.3), whatever substrate they were printed on. AF9's own chip wafers: `af9:cut_<chip>_wafer`, `kubejs:<chip>_wafer` + 100mB distilled water → the dies of §5.3b | 900t | the chip substrate's voltage (at most UV); a clean room from phosphorus up. (The old `af9:cut_<chip>_<substrate>` recipes of the printed substrate wafers no longer exist.)

## 6.8 Step 5 — Circuits

Chips are plain GT chips; a chip is a chip whatever substrate it came from. `tiered_circuits.js` keeps GT's HV-LuV circuit recipes with each tier's own metals (the Circuits quest page): HV gold fine wire + stainless steel bolts, EV platinum-iridium fine wire (bootstrap Workstation: plain platinum) + titanium bolts, IV tungstensteel fine wire and frames + tungsten wire, LuV osmiridium fine wire + niobium-titanium wire + rhodium-plated palladium bolts. `AF9_WAFERS.chipStack(id, count)` gives `"<count>x gtceu:<chip>"`. MV and lower: `mv_circuits.js` (Al-Si wire, Kovar), unchanged. The pack's LuV Nano Mainframe (its own Assembly Line recipe) takes 16 plain RAM chips.

## 6.9 Electronics metallurgy (alloys + zircon)

Files: `startup_scripts/gtceu/electronics_metallurgy.js` (materials), `server_scripts/mods/gtceu/electronics_metallurgy.js` (mixers, zircon chain, ore vein). GT generates each alloy's EBF recipe from its blast property (circuit 1 without gas, circuit 2 with the gas at 0.67× time; above 1750 K a hot ingot + vacuum freezer), the parts and centrifuge decomposition. Only alloys of a tier metal exist; HV, IV and LuV use GT's own metals.

| Alloy `gtceu:` | Tier | Mixer (circuit) → dust, EU/t | EBF | Gas | EBF EU/t, time | Part used | Real-world role |
|---|---|---|---|---|---|---|---|
| `aluminium_silicon` | MV | 16 aluminium + 1 silicon (2) → 17, LV | 1700 K | nitrogen | 120, 400t | `fine_aluminium_silicon_wire` | Al-Si wedge-bonding wire |
| `kovar` | MV | 6 iron + 3 nickel + 2 cobalt (3) → 11, LV | 1720 K | nitrogen | 120, 600t | `kovar_bolt` | glass-to-metal seals, IC package pins |
| `platinum_iridium` | EV | 9 platinum + 1 iridium (2) → 10, HV | 2100 K (hot ingot) | helium | 1920, 600t | `fine_platinum_iridium_wire` | inert noble-metal wire |

Mixer time = dust count × 30t. The MV alloys mix at LV and their EBF runs on two LV hatches (+1 tier), so MV circuits need no MV machine. Circuit 3 keeps Kovar apart from GT's invar (circuit 1, subset inputs). Iridium comes from the EV-era platinum group chain (`rarest_metal_mixture_separation`, LCR at IV voltage), hence plain platinum in the EV bootstrap.

**Zirconium** is a bare element in GT; AF9 gives `GTMaterials.Zirconium` dust + ingot + blast (2128 K, helium, HV 800t; hot ingot → vacuum freezer HV 200t). It is the zircon chain's main metal; no circuit uses it (not a tier metal).

**Zircon chain** (all HV EU/t):

```text
ore       af9:zircon_sands_vein — Mining Dimension, stone layer y129-248, dike: zircon 4, ilmenite 2, monazite 1, almandine 1
          (weight 30, cluster 35, density 0.6). Zircon ore byproducts: ilmenite, rutile, monazite.
1 EBF     af9:zircon_dissociation        6x zircon_dust + 100 argon → 3x zirconia_dust + 3x silicon_dioxide_dust | 2300 K, 600t
2 EBF     af9:zirconia_carbochlorination 3x zirconia_dust + 2x carbon_dust + 4000 chlorine → 1000 crude_zirconium_tetrachloride | 1300 K, 400t (CO flared)
3 column  af9:zirconium_hafnium_separation 1000 crude_zirconium_tetrachloride → 900 zirconium_tetrachloride + 100 hafnium_tetrachloride | 600t HV | cleanroom (SMC Rectification Column only, no still cuts)
4 EBF     af9:zirconium_kroll            2x magnesium_dust + 1000 zirconium_tetrachloride → zirconium_dust + 6x magnesium_chloride_dust | 1150 K, 400t
5 EBF     GT blast_zirconium             zirconium_dust → hot_zirconium_ingot → vacuum freezer → zirconium_ingot
```

MgCl2 goes back to Mg + Cl2 in GT's electrolyzer. Real zircon holds ~2 % hafnium; AF9 gives 10 % so the high-k modes have enough (100 mB HfCl4 per 50/20/7 nm print = one column batch = 6 zircon dust). Zircon, zirconia and the chlorides have formulas but no components, so GT adds no electrolyzer shortcut.

## 6.10 Quests (FTB Quests)

The whole lithography system is one chapter, **Photolithography** (`config/ftbquests/quests/chapters/photolithography.snbt`,
GregTech group, order 13, 74 quests + 26 quest links). Text: `kubejs/assets/kubejs/lang/en_us.json` under
`af9.quest.litho.<quest>.title|subtitle|1..n`.

Layout, left to right by voltage tier:
- **Middle row (y = 0): one hexagon per node** — Photolithography Line (350 nm), 200 nm KrF, 100 nm ArF, 80 nm, 65 nm
  immersion, 50 nm high-k, 20 nm EUV, 7 nm high-NA EUV, 1 nm orbital. Each asks for the node's blank substrate (+ its new
  light source) and explains version, power, yield, break chance and fluids per print.
- **Above: chemistry** — fab plant, XCDA, air separation, HMDS, TMAH, Formox, i-line resist, photomask blank, reticles, the
  coater track (MV); from the HV Cleanroom: fluorine, triflic acid, rare gases, KrF gas and laser, resist building
  blocks, PHOST, PAG, KrF resist, the SMC multiblocks (HV); methacrylates, ArF resist, ArF gas and laser, ultrapure water
  (EV); zircon/hafnium; tin tetrachloride, EUV resist, EUV light source, dry resist cartridge, orbital station parts.
- **Below: silicon and substrates** — MG-Si, chlorosilanes, Siemens polysilicon, EGS, HF, silicon charge/seed/crucible,
  then a "substrate highway" of melt charges (phosphorus ... chromodynium) with their boules; cryostat, coolant hatch,
  particle accelerator, transmuted neutronium, QGP, strange matter, chromodynium, tritanium crucible, Endion coils.
- **Quest links** show the GT quests of the voltage chapters that belong to the flow (MV laser engraver, silicon boule,
  cutter, silicon wafer, ULPIC wafer/chip, MV energy hatch, HV cleanroom, the printed chip wafers of each node, the
  phosphorus/naquadah/neutronium boules, the derived Nano CPU / Qubit / HPIC / UHPIC wafers). They are the same quests.

The AF9 quests that used to sit in the voltage chapters moved into this chapter; each old position now holds a quest link
to it, so the voltage chapters still show them in place. Only the four "Circuit Metals" quests stay in MV-IV. GT quests
whose dependencies changed: silicon boule (charge, seed, crucible), ILC/RAM wafers (the line), NAND/NOR/PIC/SoC wafers
(+ 200 nm), naquadah/phosphorus/neutronium boules (+ their melt charges), Nano CPU (+ 100 nm), Qubit (+ Nano CPU), UHPIC
(+ HPIC), HASoC (+ 50 nm), HV Cleanroom (+ the line).

**Quest IDs must start with 0-7.** FTB Quests 2001 reads an ID with `Long.parseLong(hex, 16)` (signed): an ID from
`8000000000000000` up does not parse, so the quest gets a new random ID on every load and every dependency on the old ID
is dropped silently. The first AF9 quests had 72 such IDs; all were replaced. The chapter is generated (positions checked
so that no dependency line passes under another quest); new IDs are 63-bit random and unique across all quest files.

---

## 6.11 Fab chemistry: gating rules and verification

Files: `startup_scripts/gtceu/fab_chemistry.js` (72 materials, formulas only, so GT generates no electrolyzer/centrifuge shortcut), `server_scripts/mods/gtceu/fab_chemistry.js` (recipes). Five lines, each 10-20 unit operations with real chemistry, inspired by Nomifactory / Cosmic Frontiers style chains.

All of these recipes run only in the SMC fab machines (§11): GT's Chemical Reactor, Large Chemical Reactor, mixer, bath, autoclave, blast furnace, electrolyzer and distillation tower have none of them. Non-thermal recipes from HV power on need a clean room (single blocks: a GT Cleanroom; the fab multiblocks bring their own filter ceiling).

Tier gating (GT 7.2.0 defaults, the pack does not change them): EBF needs LV circuits, Pyrolyse Oven MV, Large Chemical Reactor and Cracker HV circuits, **Distillation Tower and Vacuum Freezer EV circuits**; the SMC single blocks need their tier's circuits, the SMC Thermal Processing Furnace MV circuits, the other SMC multiblocks HV circuits. AF9 circuits need printed chips (GT's laser engraving is removed except the ULPIC bootstrap): HV circuits need the 350 nm mode running. Each mode's consumables must be reachable at the mode's own tier. So:

| Phase | Machines | Must supply |
|---|---|---|
| MV0 (before the first print) | LV/MV single blocks incl. the MV SMC single blocks (the Fractionating Still takes column recipes one cut per circuit, EUt/4, twice the time), SMC Thermal Processing Furnace (2 MV hatches reach HV voltage), pyrolyse oven | silicon boule (EBF Boule Melting, one MV hatch), i-line resist, HMDS, TMAH, XCDA, argon, HF |
| HV0 (350 nm runs: HV circuits) | + HV single blocks (in a Cleanroom), SMC LCR, SMC Rectification Column, SMC Membrane Cell Hall, cracker | KrF resist, KrF gas (neon, krypton, fluorine), phosphorus boule, HfCl4 |
| EV0 (EV circuits) | + EV single blocks, Vacuum Freezer | ArF resist, ArF gas, ultrapure water |

Earlier designs broke this: GT's formaldehyde is HV (i-line resist needed it at MV), krypton came from liquid air (Vacuum Freezer = EV circuits, but the 200 nm KrF mode is HV), fluorine had no source at all (AF9's fluorite has no decomposition flag; GT's only fluorine is uranium hexafluoride electrolysis), boron neither (GT 7.2.0 has no borax source). All fixed below.

Verification done for this design (re-run on every change, see §10): a reachability analysis over all GT 7.2.0 recipes (parsed from the v.7.2.0 tag) plus the AF9 scripts gives every lithography input at the phase above; a subset-conflict check in both directions within every fab recipe type finds no recipe that could be hijacked (GT picks any recipe whose inputs are all present, circuits aside; the fab types hold only AF9 recipes, so GT's own recipes cannot interfere).

## 6.12 Line 1 — Electronic-grade silicon (MV)

Real route: quartz → submerged-arc MG-Si (98-99 %) → fluidized-bed hydrochlorination → chlorosilane distillation → Siemens bell-jar CVD (1100 °C, closed loop with vent-gas recovery and STC conversion) → etched poly chunks → Czochralski. The boron that MG-Si carries ends up as BCl3 in the light ends, which is where AF9 takes its p-type dopant from.

```text
af9:high_purity_quartz (wet) quartzite_dust + 250 hydrochloric_acid → high_purity_quartz_dust + 250 diluted_hydrochloric_acid | 200t LV
af9:metallurgical_grade_silicon (calcination) high_purity_quartz_dust + 2x coke_dust → metallurgical_grade_silicon_dust + 2000 carbon_monoxide | 400t MV | 1800K
af9:crude_chlorosilanes (synthesis) NC copper_dust + metallurgical_grade_silicon_dust + 3000 hydrochloric_acid → 1000 crude_chlorosilanes + 1000 hydrogen | 300t MV
af9:chlorosilane_distillation (column) 1000 crude_chlorosilanes → 850 trichlorosilane + 100 silicon_tetrachloride + 40 dichlorosilane + 10 boron_trichloride | 300t MV
af9:electronic_grade_trichlorosilane (purify) NC activated_carbon_dust + 1000 trichlorosilane → 1000 electronic_grade_trichlorosilane | 200t MV
af9:dichlorosilane_redistribution (synthesis) NC activated_carbon_dust + 1000 dichlorosilane + 1000 silicon_tetrachloride → 2000 trichlorosilane | 200t MV
af9:boron_from_trichloride (synthesis) 1000 boron_trichloride + 3000 hydrogen → boron_dust + 3000 hydrochloric_acid | 200t MV
af9:siemens_feed_gas (blending) 1000 electronic_grade_trichlorosilane + 4000 hydrogen → 5000 siemens_feed_gas | 100t LV
af9:siemens_polysilicon (CVD) 10000 siemens_feed_gas → polysilicon_ingot + 8000 siemens_vent_gas | 1600t HV | 1400K
af9:siemens_vent_gas_recovery (column) 8000 siemens_vent_gas → 6000 hydrogen + 1000 hydrochloric_acid + 600 trichlorosilane + 400 silicon_tetrachloride | 400t MV
af9:silicon_tetrachloride_hydroconversion (synthesis) circuit 1 + 1000 silicon_tetrachloride + 1000 hydrogen → 1000 trichlorosilane + 1000 hydrochloric_acid | 300t MV
af9:fumed_silica (synthesis) circuit 2 + 1000 silicon_tetrachloride + 2000 hydrogen + 2000 oxygen → silicon_dioxide_dust + 4000 hydrochloric_acid | 200t MV
af9:silicon_etchant (blending) 1000 nitric_acid + 1000 hydrofluoric_acid → 2000 silicon_etchant | 100t LV
af9:electronic_grade_silicon (wet) polysilicon_dust + 100 silicon_etchant → electronic_grade_silicon_dust | 100t MV
(boules: melt charges, seed crystals and Boule Melting — §12)
```

Mass balance: per polysilicon ingot the Siemens reactor takes 2000 TCS; the vent gas returns 600 TCS + 400 STC (→ TCS), so one ingot costs ~1000 TCS ≈ 1 MG-Si. STC can instead become fumed silica (circuit 2) or TEOS (§6.15). At MV the two distillations run in the SMC Fractionating Still, one cut per circuit; the SMC Rectification Column (HV) runs them whole.

## 6.13 Line 2 — Fluorochemicals (HF at MV, fluorine and triflic acid at HV)

Real route: acid-grade fluorspar + H2SO4 in a rotary kiln → crude HF → anhydrous HF; Moissan-type medium-temperature cells (molten KF·2HF, carbon anodes, 90 °C) → F2, HF stripped over NaF (regenerated by heating). Triflic acid: Grillo methanesulfonic acid → sulfonyl chloride (thionyl chloride from SCl2 + SO3) → halogen exchange with KF → Simons electrochemical fluorination in anhydrous HF over nickel → CF3SO2F → potassium triflate → H2SO4 → vacuum distillation.

```text
af9:crude_hydrogen_fluoride (synthesis) fluorite_dust + 1000 sulfuric_acid → gypsum_dust + 2000 crude_hydrogen_fluoride | 300t MV
af9:anhydrous_hydrogen_fluoride (column) 2000 crude_hydrogen_fluoride → 1800 hydrofluoric_acid + 150 sulfuric_acid + 50 water | 200t MV
af9:potassium_fluoride (synthesis) potassium_hydroxide_dust + 1000 hydrofluoric_acid → potassium_fluoride_dust + 1000 water | 100t MV
af9:potassium_bifluoride_electrolyte (blending) potassium_fluoride_dust + 2000 hydrofluoric_acid → 1000 potassium_bifluoride_electrolyte | 200t MV
af9:fluorine_electrolysis (electrolysis) NC carbon_dust + 1000 potassium_bifluoride_electrolyte → potassium_fluoride_dust + 2000 crude_fluorine + 2000 hydrogen | 400t HV | cleanroom
af9:sodium_fluoride (synthesis) sodium_hydroxide_dust + 1000 hydrofluoric_acid → sodium_fluoride_dust + 1000 water | 100t MV
af9:fluorine_purification (purify) sodium_fluoride_dust + 2000 crude_fluorine → sodium_bifluoride_dust + 1800 fluorine | 100t HV | cleanroom
af9:sodium_bifluoride_regeneration (calcination) sodium_bifluoride_dust → sodium_fluoride_dust + 200 hydrofluoric_acid | 100t MV | 700K


af9:methanesulfonic_acid (synthesis) 1000 methane + 1000 sulfur_trioxide + 50 hydrogen_peroxide → 1000 methanesulfonic_acid + 50 water | 300t HV | cleanroom
af9:sulfur_dichloride (synthesis) sulfur_dust + 2000 chlorine → 1000 sulfur_dichloride | 100t MV
af9:thionyl_chloride (synthesis) 1000 sulfur_dichloride + 1000 sulfur_trioxide → 1000 thionyl_chloride + 1000 sulfur_dioxide | 200t MV
af9:methanesulfonyl_chloride (synthesis) 1000 methanesulfonic_acid + 1000 thionyl_chloride → 1000 methanesulfonyl_chloride + 1000 sulfur_dioxide + 1000 hydrochloric_acid | 200t HV | cleanroom
af9:methanesulfonyl_fluoride (synthesis) potassium_fluoride_dust + 1000 methanesulfonyl_chloride → rock_salt_dust + 1000 methanesulfonyl_fluoride | 200t HV | cleanroom
af9:simons_cell_electrolyte (blending) 1000 methanesulfonyl_fluoride + 3000 hydrofluoric_acid → 4000 simons_cell_electrolyte | 100t HV | cleanroom
af9:electrochemical_fluorination (ECF) NC nickel_plate + 4000 simons_cell_electrolyte → 1000 trifluoromethanesulfonyl_fluoride + 6000 hydrogen | 600t HV | cleanroom
af9:potassium_triflate (synthesis) 2x potassium_hydroxide_dust + 1000 trifluoromethanesulfonyl_fluoride → potassium_triflate_dust + potassium_fluoride_dust + 1000 water | 200t HV | cleanroom
af9:trifluoromethanesulfonic_acid (synthesis) 2x potassium_triflate_dust + 1000 sulfuric_acid → potassium_sulfate_dust + 2000 trifluoromethanesulfonic_acid | 300t HV | cleanroom
```

## 6.14 Line 3 — Air gases and excimer premix (argon at MV, Ne/Kr/Xe at HV)

Real route: the double-column cold box takes side draws: crude argon (deoxo with H2 over Pd), crude neon from the condenser head (H2 burnt off, N2 adsorbed on cold charcoal, He split off), krypton-xenon concentrate from the oxygen sump (catalytic hydrocarbon burner, molecular-sieve drying, rectification). Feed is AF9's Cryogenic Supercooled Air (MV expansion cooler, §6.5), not GT's liquid air (EV-gated Vacuum Freezer). Premix = 5 % rare gas + 1 % purified fluorine in neon (real ~1 % / 0.1 %).

```text
af9:air_rectification (cryo column) 4000 cryogenic_supercooled_air → 2960 nitrogen + 600 oxygen + 400 crude_argon + 24 crude_neon + 16 krypton_xenon_concentrate | 300t MV
af9:argon_deoxo (purify) NC palladium_dust + 1000 crude_argon + 100 hydrogen → 950 argon + 50 water | 100t MV
af9:crude_neon_purification (purify) NC platinum_dust + NC activated_carbon_dust + 1000 crude_neon + 50 oxygen → 700 neon_helium_mixture + 250 nitrogen | 200t HV | cleanroom
af9:neon_helium_separation (cryo column) 1000 neon_helium_mixture → 720 neon + 280 helium | 200t HV | cleanroom
af9:krypton_xenon_catalytic_burner (purify) NC platinum_dust + 1000 krypton_xenon_concentrate → 980 crude_krypton_xenon + 20 carbon_dioxide | 100t HV | cleanroom
af9:krypton_xenon_drying (purify) kubejs:molecular_sieve + 1000 crude_krypton_xenon → kubejs:saturated_molecular_sieve + 1000 purified_krypton_xenon | 100t HV | cleanroom
af9:krypton_xenon_rectification (cryo column) 1000 purified_krypton_xenon → 700 oxygen + 270 krypton + 30 xenon | 300t HV | cleanroom
af9:krf_excimer_gas (blending) 940 neon + 50 krypton + 10 fluorine → 1000 krf_excimer_gas | 200t HV | cleanroom
af9:arf_excimer_gas (blending) 940 neon + 50 argon + 10 fluorine → 1000 arf_excimer_gas | 200t HV | cleanroom
```

Real air: 0.93 % Ar, 18 ppm Ne, 1.1 ppm Kr, 0.09 ppm Xe; AF9 boosts all of them (as GT boosts helium) but keeps Ne:Kr ≈ 16:1 in the cold box and Kr:Xe ≈ 9:1. Xenon becomes available at HV (GT: IV).

## 6.15 Line 4 — KrF resist (HV)

The real KrF chemically amplified resist: poly(4-hydroxystyrene) made by the Hoechst Celanese 4-acetoxystyrene route (phenol → HF-catalysed acylation → acetylation → Pd/C hydrogenation → dehydration → AIBN radical polymerization → methanolysis), about 30 % protected as t-BOC carbonate (Boc2O from isobutylene → tert-butanol → sodium tert-butoxide + CO2 + phosgene). PAG: triphenylsulfonium triflate (Friedel-Crafts diphenyl sulfoxide + phenyl Grignard in THF, THF from n-butane via maleic anhydride over VPO; anion swap with triflic acid). Quencher: tributylamine (oxo butyraldehyde → n-butanol → amination). Solvent: PGMEA (HPPO propylene oxide over TS-1 → PGME → esterification over an acidic ion-exchange resin). Formulated, then point-of-use filtered (20 nm).

```text
catalysts and reagents
af9:palladium_chloride (synthesis) palladium_dust + 2000 chlorine → palladium_chloride_dust | 200t MV
af9:palladium_on_carbon (synthesis) palladium_chloride_dust + 8x activated_carbon_dust + 2000 hydrogen → 8x palladium_on_carbon_dust + 2000 hydrochloric_acid | 200t MV
af9:acidic_ion_exchange_resin (synthesis) tiny_azobisisobutyronitrile_dust + 1000 styrene + 1000 sulfuric_acid → acidic_ion_exchange_resin_dust + 1000 water | 300t HV | cleanroom
af9:tetraethyl_orthosilicate (synthesis) 1000 silicon_tetrachloride + 4000 ethanol → 1000 tetraethyl_orthosilicate + 4000 hydrochloric_acid | 200t MV
af9:titanium_silicalite (synthesis) tiny_rutile_dust + 1000 tetraethyl_orthosilicate + 2000 water → titanium_silicalite_dust + 4000 ethanol | 600t HV | cleanroom

AIBN
af9:hydrazine (synthesis) sodium_hydroxide_dust + 1000 monochloramine + 1000 ammonia → salt_dust + 1000 hydrazine + 1000 water | 200t HV | cleanroom
af9:acetone_cyanohydrin (synthesis) NC sodium_hydroxide_dust + 1000 acetone + 1000 hydrogen_cyanide → 1000 acetone_cyanohydrin | 200t HV | cleanroom
af9:hydrazobisisobutyronitrile (synthesis) 2000 acetone_cyanohydrin + 1000 hydrazine → hydrazobisisobutyronitrile_dust + 2000 water | 200t HV | cleanroom
af9:azobisisobutyronitrile (synthesis) hydrazobisisobutyronitrile_dust + 2000 chlorine → azobisisobutyronitrile_dust + 2000 hydrochloric_acid | 200t HV | cleanroom

resin (4-acetoxystyrene route)
af9:hydroxyacetophenone (synthesis) NC 1000 hydrofluoric_acid + 1000 phenol + 1000 acetic_anhydride → hydroxyacetophenone_dust + 1000 acetic_acid | 300t HV | cleanroom
af9:acetoxyacetophenone (synthesis) hydroxyacetophenone_dust + 1000 acetic_anhydride → acetoxyacetophenone_dust + 1000 acetic_acid | 200t HV | cleanroom
af9:acetoxyphenyl_methyl_carbinol (synthesis) NC palladium_on_carbon_dust + acetoxyacetophenone_dust + 2000 hydrogen → 1000 acetoxyphenyl_methyl_carbinol | 300t HV | cleanroom
af9:acetoxystyrene (synthesis) NC sodium_bisulfate_dust + 1000 acetoxyphenyl_methyl_carbinol → 1000 acetoxystyrene + 1000 water | 200t HV | cleanroom
af9:poly_acetoxystyrene (synthesis) tiny_azobisisobutyronitrile_dust + 1000 acetoxystyrene → poly_acetoxystyrene_dust | 400t HV | cleanroom
af9:polyhydroxystyrene (synthesis) poly_acetoxystyrene_dust + NC 1000 ammonia + 1000 methanol → polyhydroxystyrene_dust + 1000 methyl_acetate | 300t HV | cleanroom

t-BOC protection
af9:isobutylene (synthesis) NC zeolite_dust + 1000 butene → 1000 isobutylene | 200t HV | cleanroom
af9:tert_butanol (synthesis) NC acidic_ion_exchange_resin_dust + 1000 isobutylene + 1000 water → 1000 tert_butanol | 200t HV | cleanroom
af9:sodium_tert_butoxide (synthesis) sodium_dust + 1000 tert_butanol → sodium_tert_butoxide_dust + 1000 hydrogen | 100t HV | cleanroom
af9:phosgene (synthesis) NC activated_carbon_dust + 1000 carbon_monoxide + 2000 chlorine → 1000 phosgene | 100t HV | cleanroom
af9:di_tert_butyl_dicarbonate (synthesis) 2x sodium_tert_butoxide_dust + 1000 carbon_dioxide + 1000 phosgene → 2x salt_dust + 1000 di_tert_butyl_dicarbonate | 300t HV | cleanroom
af9:tboc_polyhydroxystyrene (synthesis) polyhydroxystyrene_dust + 300 di_tert_butyl_dicarbonate → tboc_polyhydroxystyrene_dust + 300 carbon_dioxide + 300 tert_butanol | 300t HV | cleanroom

PAG
af9:aluminium_chloride (synthesis) aluminium_dust + 3000 chlorine → aluminium_chloride_dust | 200t MV
af9:diphenyl_sulfoxide (synthesis) NC aluminium_chloride_dust + 2000 benzene + 1000 thionyl_chloride → diphenyl_sulfoxide_dust + 2000 hydrochloric_acid | 300t HV | cleanroom
af9:vanadyl_pyrophosphate (synthesis) 2x vanadium_dust + 2000 phosphoric_acid + 5000 oxygen → vanadyl_pyrophosphate_dust + 3000 water | 400t HV | cleanroom
af9:maleic_anhydride (synthesis) NC vanadyl_pyrophosphate_dust + 1000 butane + 7000 oxygen → maleic_anhydride_dust + 4000 water | 300t HV | cleanroom
af9:tetrahydrofuran (synthesis) NC palladium_on_carbon_dust + maleic_anhydride_dust + 10000 hydrogen → 1000 tetrahydrofuran + 2000 water | 300t HV | cleanroom
af9:phenylmagnesium_chloride (synthesis) magnesium_dust + 1000 chlorobenzene + 1000 tetrahydrofuran → 1000 phenylmagnesium_chloride | 200t HV | cleanroom
af9:triphenylsulfonium_chloride (synthesis) diphenyl_sulfoxide_dust + 1000 phenylmagnesium_chloride + 2000 hydrochloric_acid → triphenylsulfonium_chloride_dust + magnesium_chloride_dust + 1000 tetrahydrofuran + 1000 water | 300t HV | cleanroom
af9:triphenylsulfonium_triflate (synthesis) triphenylsulfonium_chloride_dust + 1000 trifluoromethanesulfonic_acid → triphenylsulfonium_triflate_dust + 1000 hydrochloric_acid | 200t HV | cleanroom

quencher
af9:butanol (synthesis) NC nickel_dust + 1000 butyraldehyde + 2000 hydrogen → 1000 butanol | 200t HV | cleanroom
af9:tributylamine (synthesis) NC nickel_dust + 3000 butanol + 1000 ammonia → 1000 tributylamine + 3000 water | 300t HV | cleanroom

solvent
af9:propylene_oxide (synthesis) NC titanium_silicalite_dust + 1000 propene + 1000 hydrogen_peroxide → 1000 propylene_oxide + 1000 water | 200t HV | cleanroom
af9:propylene_glycol_methyl_ether (synthesis) NC sodium_hydroxide_dust + 1000 propylene_oxide + 1000 methanol → 1000 propylene_glycol_methyl_ether | 200t HV | cleanroom
af9:propylene_glycol_methyl_ether_acetate (synthesis) NC acidic_ion_exchange_resin_dust + 1000 propylene_glycol_methyl_ether + 1000 acetic_acid → 1000 propylene_glycol_methyl_ether_acetate + 1000 water | 200t HV | cleanroom

formulation
af9:unfiltered_krf_photoresist (blending) tboc_polyhydroxystyrene_dust + small_triphenylsulfonium_triflate_dust + 3000 propylene_glycol_methyl_ether_acetate + 10 tributylamine → 4000 unfiltered_krf_photoresist | 400t HV | cleanroom
af9:krf_photoresist (purify) NC fluid_filter + 4000 unfiltered_krf_photoresist → 4000 krf_photoresist | 200t HV | cleanroom
```

## 6.16 Line 5 — ArF resist and immersion water (EV)

ArF resists cannot use aromatic rings (they absorb 193 nm). AF9 uses the first industrial 193 nm platform, a methacrylate terpolymer (methyl methacrylate / tert-butyl methacrylate as the acid-labile unit / methacrylic acid for development), from the acetone cyanohydrin route. Its ammonium bisulfate waste goes back to SO2 (spent-acid regeneration), as in real ACH plants. Modern ArF resins add adamantyl and lactone monomers (§9).

```text
af9:methacrylamide_sulfate (synthesis) 1000 acetone_cyanohydrin + 1000 sulfuric_acid → 1000 methacrylamide_sulfate | 200t EV | cleanroom
af9:methyl_methacrylate (synthesis) 1000 methacrylamide_sulfate + 1000 methanol → ammonium_bisulfate_dust + 1000 methyl_methacrylate | 200t EV | cleanroom
af9:methacrylic_acid (synthesis) 1000 methacrylamide_sulfate + 1000 water → ammonium_bisulfate_dust + 1000 methacrylic_acid | 200t EV | cleanroom
af9:tert_butyl_methacrylate (synthesis) NC acidic_ion_exchange_resin_dust + 1000 methacrylic_acid + 1000 isobutylene → 1000 tert_butyl_methacrylate | 200t EV | cleanroom
af9:spent_acid_regeneration (calcination) 2x ammonium_bisulfate_dust + 1000 oxygen → 2000 sulfur_dioxide | 200t HV | 1300K
af9:methacrylate_resin (synthesis) tiny_azobisisobutyronitrile_dust + 1000 methyl_methacrylate + 1000 tert_butyl_methacrylate + 500 methacrylic_acid → 2x methacrylate_resin_dust | 400t EV | cleanroom
af9:unfiltered_arf_photoresist (blending) methacrylate_resin_dust + small_triphenylsulfonium_triflate_dust + 3000 propylene_glycol_methyl_ether_acetate + 10 tributylamine → 4000 unfiltered_arf_photoresist | 400t EV | cleanroom
af9:arf_photoresist (purify) NC fluid_filter + 4000 unfiltered_arf_photoresist → 4000 arf_photoresist | 200t EV | cleanroom
af9:ultrapure_water (purify) NC fluid_filter + 4000 distilled_water → 4000 ultrapure_water | 200t EV | cleanroom
```

# 7. Connection graph

```text
EGS (+ dopant) → [blending] melt charge, [crystal growth] seed crystal → [EBF Boule Melting: 10 charges + seed + crucible + gas] boule → [CUTTER] blank wafers
  (silicon, phosphorus, naquadah, trinium, naquadria, neutronium, strange matter, chromodynium;
   4 neutronium wafers + Be target + supercooled H2 → [PARTICLE_ACCELERATOR neutron irradiation] 4 transmuted neutronium wafers)
  → [PHOTOLITHOGRAPHY_LINE version 1-8, mode = substrate (350 nm Si … 7 nm strange matter), + reticle + track fluids (+ mode's resist, laser gas, UPW, HfCl4, tin + H2)]
  | [ORBITAL_LITHOGRAPHY_STATION in orbit, 1 nm chromodynium, + reticle + dry resist cartridge + supercooled endion]
     → GT's chip wafer(s) (as many as the substrate yields, §5.3)  or  kubejs:broken_<substrate>_wafer (break roll: vacuum, cooling, OPC, calibration)
     → [CHEMICAL_REACTOR / LCR, cleanroom] derived wafer (nano/qbit CPU, HPIC, UHPIC) on the same substrate
     → [CUTTER] plain GT chips (more per wafer on higher substrates)
        → [CIRCUIT_ASSEMBLER] + that tier's metals (MV Al-Si + Kovar, HV gold + stainless, EV Pt-Ir + titanium,
                                IV tungstensteel + tungsten, LuV osmiridium + NbTi + rhodium-plated palladium)
Coolants: gas → [SUPERCOOLING_CRYOSTAT dense cooling] dense gas → [same, supercooling] supercooled gas (H2, Ar, Xe, endion) → coolant hatches;
          ender air → [centrifuge / distillation] endion → endionite, Endion coils, supercooled endion;
          magnetic trap + lead → [ACCELERATOR ion collision] QGP trap → [quark synthesis] strange matter, chromodynium
Side chains (fab modes of §11 in brackets, GT machines in capitals): air → [purify hopcalite/Pt] → [purify lime/NaOH] → [purify molecular sieve] → [purify expander / liquid air] → [purify filter] → XCDA;
             zircon ore → [EBF] zirconia → [EBF +C +Cl2] crude ZrCl4 → [column] ZrCl4 + HfCl4 (→ 50/20/7 nm high-k) → [EBF Kroll +Mg] Zr → [EBF] ingot;
             DMDCS+CH3Cl+Mg → TMCS → +NH3 → HMDS → +N2 → hmds_vapor;
             phenol+CH2O → novolac, naphthalene+HNO3+NH3 → DNQ, +xylene → photoresist (i-line, 350 nm);
             quartzite → [wet HCl] HPQ → [calcination +coke] MG-Si → [synthesis +HCl, Cu] crude chlorosilanes → [column/still] TCS / STC / DCS / BCl3
               → [purify carbon] EG-TCS → [blending +H2] feed → [CVD Siemens] polysilicon + vent gas → [column/still] H2, HCl, TCS, STC (loop)
               → [macerator] → [wet HNA etch] EGS → melt charges + seeds (+ boron from BCl3) → [EBF Boule Melting, argon] silicon boule;
             fluorite + H2SO4 → crude HF → [column/still] AHF → KF → KF·2HF → [electrolysis, carbon] crude F2 → [purify NaF] F2;
             CH4+SO3 → MSA → [+SOCl2] MsCl → [+KF] MsF → [+HF, ECF Ni] CF3SO2F → [+KOH] KOTf → [+H2SO4] triflic acid;
             cryogenic supercooled air → [cryo column / still] N2, O2, crude Ar (→ deoxo → Ar), crude Ne (→ Pt/charcoal → [cryo column] Ne + He),
               Kr/Xe concentrate (→ Pt burner → sieve → [cryo column] O2, Kr, Xe) → [blending +F2] KrF / ArF excimer gas;
             phenol → 4-HAP → 4-AAP → [Pd/C] carbinol → 4-acetoxystyrene → [AIBN] PAS → PHOST → [+Boc2O] t-BOC PHOST;
             butene → isobutylene → tBuOH → NaOtBu → [+CO2 +COCl2] Boc2O; MgCl/THF Grignard + Ph2SO → TPS-Cl → [+TfOH] PAG;
             butane → [VPO] maleic anhydride → THF; butyraldehyde → n-butanol → tributylamine; propene + H2O2 [TS-1] → PO → PGME → PGMEA;
             acetone + HCN → ACH → [+H2SO4] methacrylamide sulfate → MMA / MAA → [+isobutylene] tBMA → [AIBN, synthesis] methacrylate resin;
             resin + PAG + PGMEA + tributylamine → [blending] → [purify filter] KrF / ArF resist; distilled water → [purify filter] ultrapure water;
             tin + Cl2 → SnCl4 → [+MAA, PGMEA, UPW] EUV resist → [CVD + tungstensteel] dry resist cartridge (orbital);
             molybdenite → MoO3 → [+hematite] iron molybdate → Formox formaldehyde (MV) → novolac;
             dimethylamine+CH3Cl → TMACl → [+DW] solution → [electrolysis] tmah_developer;
             quartzite+chromium+resist → photomask blank → laser + lens → reticle
```

# 8. How the chips are used

Chips are plain GT chips (no mode, no NBT): any substrate's cut gives the same `gtceu:<chip>`, a higher substrate just gives more of them (§5.3).

| Chip | Where (AF9) |
|---|---|
| ilc | LV/MV integrated circuits; HV Advanced Integrated Circuit |
| ram | MV Microprocessor; HV-LuV processors, assemblies, computers, mainframes, incl. the pack's LuV Nano Mainframe (16 RAM chips) |
| cpu | LV/MV microprocessors; source for nano_cpu wafers |
| ulpic, lpic, mpic, hpic, uhpic | GT energy parts (hatches, batteries, …): unchanged ingredients |
| simple_soc, soc | GT NAND chip / SoC processor recipes: unchanged |
| nand, nor | GT uses (crystal computer ZPM, data sticks …) and the IV/LuV nano and quantum computers |
| advanced_soc | HV nano and EV quantum SoC processors |
| highly_advanced_soc | LuV wetware SoC processor |
| nano_cpu | HV nano, EV quantum, IV crystal and LuV wetware processors |
| qbit_cpu | EV quantum processors |

AF9's own chips (§5.3b). "On top": added to the recipe, which keeps everything else; "extra": a second recipe beside the original, which stays.

| Chip | Where | Script |
|---|---|---|
| rf_transceiver | wireless energy hatches, EV-UHV: 2 on top of each receiver / transmitter; AE2 Wireless Receiver (so the Wireless Access Point and every wireless terminal): 1, in the shape `F / IQI / IRI` | wireless_energy.js, chip_uses.js |
| apu | extra MV Microprocessor: 1 APU in place of the CPU chip + RAM, 3 out instead of 2 | mv_circuits.js |
| mcu | GT Machine Controller, Activity / Item / Fluid Detector covers: 1 on top (they had no circuit); extra LV / MV Robot Arm and Sensor Assembler recipes: 1 MCU in place of the circuit; the machine bus (`docs/machine-bus.md`): 2 per Bus Connector, 1 per Machine Bus Module | chip_uses.js, machine_bus.js |
| asic | on top: GT Large Miners EV 2 / IV 4 / LuV 8, Fluid Drilling Rigs HV 2 / EV 4 (not MV: before phosphorus), Void Miner 4; extra SMC fab multiblock controller recipes with ASICs in place of the circuits | chip_uses.js, miner.js, fab_machines.js |
| edram packages | extra, half the time, 1 package in place of 4 RAM: Quantum Computer (and ASMD) 4 CPU packages, Crystal Processor Assembly 6 SoC packages, the pack's Nano Mainframe (Assembly Line, 800 t) 4 CPU packages | tiered_circuits.js |
| mram | extra, half GT's time, no research, clean room: Data Orb (Circuit Assembler: epoxy board, CPU chip, 4 MRAM, 16 fine Pt wire), Data Bank (Assembler: computer casing, 16 MRAM, 2 CPU chips, 32 fine NbTi wire, 4 optical pipes), Advanced Data Access Hatch (Assembler: LuV input bus, 8 MRAM, 2 CPU chips, 32 fine NbTi wire) | chip_uses.js |
| feram | the Tensor RAM card of the computation arrays (the pack's UV memory) | computation.js |
| vpu | GT LuV Sensor (Assembly Line): 2 on top; LuV Scanner: GT's shape with 2 VPUs in place of the bottom two ZPM circuits (`CEC / WHW / VSV`); Orbital Lithography Station: 8 on top | chip_uses.js, photolithography.js |
| tpu | GT HPCA Advanced Computation Component: 4 on top; the pack's UHV Wetware Mainframe: 16 on top | chip_uses.js |

# 9. Extension points (not done yet)

1. Transistor system (B): 3×3 transistor breadboard, printed PCBs, x10 transistor demand — design pending.
2. Circuits do not ask for a substrate: a UV circuit could be made to need 20 nm chips (e.g. only cut from transmuted neutronium wafers) if the late game should depend on the new substrates beyond die counts.
3. Wet etch (HF/BOE) step, sterile cleanroom wafers, crystal chips (autoclave path) — untouched.
4. Quests are updated (§6.10); non-English quest languages show the new English text for changed quests until translated.
5. Hafnium metal: still has no items (only HfCl4: the high-k gate and the hafnium-oxo resist, §18.6); zirconium ingots have no use beyond the zirconium-oxo resist.
6. Masks: one reticle per chip serves every mode; GT's chips are binary chrome, AF9's 80 nm and finer chips have phase-shift or EUV reticles (§5.3b), GT's own chips do not. Real sub-wavelength and EUV modes need phase-shift masks with OPC, EUV reflective Mo/Si masks; a PSM / reflective reticle tier would be the next realism step.
7. BARC and TARC are fluids of the Coater Track (§18.6, §6.5b); multi-patterning is a switch on the Line and Scanner (§18.11); the modes themselves are single exposure (k1 ≥ 0.29), the optics' k1 is not simulated.
8. ArF resin is the first-generation methacrylate terpolymer. Modern ArF monomers (2-methyl-2-adamantyl methacrylate from dicyclopentadiene → adamantane → adamantanone; α-methacryloyloxy-γ-butyrolactone from 1,4-butanediol) would extend Line 5. The i-line DNQ chain (§6.5) is still the short 3-step version (real PAC: DNQ-5-sulfonyl chloride esterified onto a trihydroxybenzophenone).
9. Pd/C, VPO, TS-1, iron molybdate, acidic resin and the other catalysts are not consumed; catalyst deactivation is not modelled.
10. The orbital station's orbit test is by dimension name (`orbit` / `*_orbit`); a new space mod's orbit dimension with another name needs a line in `OrbitalLithographyMachine.isOrbit`.
11. The coater is a multiblock of its own with a coated wafer per substrate (§18.8); the developer / PEB / hard bake stay in the Line (one run exposes, bakes and develops, so no further wafer state is needed). Not built: strip as a step of its own, a fluid output on the prints for the spent etch plasma, the HF / calcium fluoride loop that would hang on it.
12. The Temperature Update (warmth around machines the player feels, the hotbar message, the screen effect): the Air Conditioning Hatch is an `IHeatEmitter` (§18.1); the system that reads it is not built. The computation arrays have their own heat (§ machine-bus 9) and could implement it too.

---

# 10. Agent validation checklist

- [ ] IDs: controllers `gtceu:photolithography_line`, `gtceu:orbital_lithography_station`; recipe types `gtceu:lithography_<node>` and `gtceu:orbital_lithography`; recipe IDs `af9:*`. Chip IDs per §5.3 traps.
- [ ] Numbers come from `AF9_WAFERS` (server), `AF9_WAFER_TABLE` (startup) and `LithoMode` (Java) — change all three together (substrate order, node, tier, light, wavelength, NA, resist, base break chance, colours).
- [ ] Light/resist: 350 nm i-line + `photoresist`, 200 nm KrF + `krf_photoresist` + `krf_excimer_gas`, 100/80 nm ArF + `arf_photoresist` + `arf_excimer_gas`, 65 nm + `ultrapure_water`, 50 nm + UPW + `hafnium_tetrachloride`, 20/7 nm `euv_photoresist` + molten tin + hydrogen + HfCl4, 1 nm `kubejs:dry_resist_cartridge` + `supercooled_endion`; fluid slots 5/6/6/6/7/8/8/8 and orbital 1 (`setMaxIOSize` in the startup script).
- [ ] Fab chemistry goes on the `fab_*` recipe types only (§11), never on GT's chemical machines; within a fab type no recipe may hold every input of a circuit-less other one. Recipes AF9 still puts on GT machines (alloy mixers, zircon cracking, Kroll) must not hold every input of a circuit-less GT recipe of the same type (e.g. GT ethenone = sulfuric + acetic acid, acetic acid = CO + methanol); give both circuits or change the route.
- [ ] Tier check for consumables (§6.11): 350 nm inputs at MV0 (single blocks ≤ MV incl. MV SMC single blocks and still cuts, SMC Thermal Processing Furnace, pyrolyse), 200 nm inputs at HV0 (+ HV single blocks, SMC multiblocks, cracker), no Distillation Tower / Vacuum Freezer before 200 nm chips. Run the reachability analysis over GT 7.2.0 + AF9 recipes after every chain change.
- [ ] Subset conflicts in both directions within each fab type and, for AF9 recipes on GT machines, against every GT 7.2.0 recipe of the same machine (CR recipes also run in the LCR); circuit-gated recipes only clash with the same circuit. Watch GT's generic ones: distilled-water electrolysis, clay/quartzite autoclave, graphite electrolysis, ethenone (sulfuric + acetic acid), acetic acid (CO + methanol), steel (iron + oxygen).
- [ ] New fab materials: formula only, no components (no GT decomposition shortcut); ID must not exist in GT 7.2.0.
- [ ] Prints: the mode's blank wafer + reticle (NC) + fluids → GT's chip wafer(s) (`AF9_WAFERS.yieldOf`) + the broken wafer as a chanced output at the base chance; 900t, `EUt(VA[tier], 4)` (orbital 7200t, `EUt(VA[UHV], 50)`); chemicals `round(base×1.5^i)`; a substrate prints exactly the chips whose native substrate is at or below it. Line versions (§5.5): `LithoMode` constants and `PhotolithographyLineMachine` light sources match the startup structure (lens aisle `setRepeatable(3, 10)`, `L` = lamp / KrF / ArF / EUV source).
- [ ] Derived wafers: chemical reactor + LCR recipes on every substrate that prints the source chip, same substrate out, cleanroom; GT's own derived recipes stay for GT's wafers.
- [ ] Cutter: GT's chip wafers keep GT's cutter recipes whatever substrate printed them; AF9's own chip wafers have `af9:cut_<chip>_wafer`.
- [ ] Circuits: plain chips only (`AF9_WAFERS.chipStack`); no NBT, no mode tags.
- [ ] Metals: a tier's circuits only use that tier's metals (Circuits quest page), each makeable with the previous tier's machines (mixer/EBF voltage one tier lower; two hatches give +1 tier on an EBF).
- [ ] Bootstrap check for any change: a tier-T circuit, energy hatch or the line must never need something only tier-T machines make.
- [ ] Fab recipes (§11): a free fluid input left for the changeover purge; circuits count as item inputs; thermal modes carry `blastFurnaceTemp`, the others none; `cleanroom(CLEANROOM)` exactly on the non-thermal recipes from HV power on; single-block modes of one family share one slot layout; single-block fluid amounts ≤ 16000 mB (MV tanks).
- [ ] Dry run: load the AF9 server scripts (incl. `fab_machines.js`) with stubs and check no duplicate IDs, every tagged ingredient has a producer, every AF9 fluid/dust used has a producer, every recipe within its machine's slots (items in incl. NC, items out, fluids in incl. NC/out; 8 fluids at 50-7 nm), EBF recipes have a temperature, ≤64 per stack.
- [ ] New mixer alloys: circuit number must not collide with a GT mixer recipe whose inputs are a subset (invar, cupronickel use circuit 1).
- [ ] New materials need a `material.gtceu.<id>` line in `kubejs/assets/gtceu/lang/en_us.json`; new KubeJS items need a texture in `kubejs/assets/kubejs/textures/item/`.
- [ ] Multiblock parts (all AF9 multiblocks): a maximum only, never a minimum or exact count — `setMaxGlobalLimited(max, preview count)`, no `setMinGlobalLimited` / `setExactLimit` / GT `autoAbilities` (it forces energy and maintenance), no casing minimums (the dry run flags all of these). Every recipe must run on normal 2A hatches within those maximums (≤ 4A on two energy hatches).
- [ ] Line: up to 2 energy hatches, `LITHO_GATE + STRIP_BROKEN + LITHO_VERSION + OC_PERFECT`; orbital: up to 1 laser + 3 energy hatches, 2 coolant hatches, computation + data hatch, batch mode, `LITHO_GATE + STRIP_BROKEN + COOLANT + OC_PERFECT + BATCH_MODE`, pattern unchanged from the `sol_array` design; no cleanroom for either.
- [ ] Quests: IDs are 16 hex digits starting with 0-7 (FTB Quests parses them as signed longs), unique across all quest files; new lithography quests go into `photolithography.snbt`, text into `af9.quest.litho.*`.
- [ ] Realism wording: node names are gameplay labels; chromodynium, strange-matter and transmuted-neutronium wafers are fiction on real physics names.
- [ ] Wafers: every new printable/blank wafer is in `#af9:wafers/<substrate>` (server tags) so contamination knows it; broken and contaminated wafers are not. Every substrate has a broken and a contaminated wafer item + texture.
- [ ] Boules: every boule recipe is 3 items + 1 fluid (the EBF's slots), has `blastFurnaceTemp`, and its temperature is reachable with the coils the tier has (chromodynium 12000 K = Resonant Endion).
- [ ] New machines: cryostat recipes `EUt(VA[HV], 4)` (POWER_GATE); coolant hatch filter accepts only `gtceu:supercooled_*`; accelerator and orbital recipes take their fluid through a coolant hatch.

---

# 11. SMC fab machines (the semiconductor chemistry plant)

Fab chemistry is its own plant. None of GT's chemical machines (Chemical Reactor, Large Chemical Reactor, mixer, chemical bath, autoclave, electrolyzer, blast furnace, distillation tower) has any AF9 fab recipe: the 133 recipes of §6.5, §6.9 (Zr/Hf split) and §6.12-6.16 (21 of them still cuts) are on twelve `fab_*` recipe types, and only the SMC ("semiconductor") machines run those. Every family has a multiblock and tiered single blocks, and they share the family's modes (GT recipe types; GT's mode tab, or the console's mode tiles on multiblocks).

Files: `kubejs/startup_scripts/gtceu/fab_machines.js` (types, machines, structures), `kubejs/server_scripts/mods/gtceu/fab_machines.js` (crafting), `af9-core/.../fab/` and `af9-core/.../machine/fab/` (behaviour, consoles).

## 11.1 Families

| Family | Multiblock | Single blocks (MV-LuV) | Modes (`gtceu:` recipe types) | Changeover purge |
|---|---|---|---|---|
| Chemistry | `smc_large_chemical_reactor` (HV) | `<tier>_smc_chemical_reactor` | `fab_synthesis`, `fab_blending`, `fab_wet_processing` | 1000 mB N2 or Ar, +100 t |
| Separation | `smc_rectification_column` (HV) | `<tier>_smc_fractionating_still` | column: `fab_distillation`, `fab_cryogenic_rectification`, `fab_fractionation`, `fab_purification`; still: `fab_fractionation`, `fab_purification` | 2000 mB N2, +100 t |
| Electrochemistry | `smc_membrane_cell_hall` (HV) | `<tier>_smc_electrolytic_cell` | `fab_electrolysis`, `fab_electrofluorination` | 1000 mB distilled water, +100 t |
| Thermal | `smc_thermal_processing_furnace` (MV) | `<tier>_smc_thermal_furnace` | `fab_calcination`, `fab_cvd`, `fab_crystal_growth` | 1000 mB Ar or N2, +200 t |

The multiblock tier is the circuit tier of its crafting recipe. The thermal multiblock is MV because the MV silicon line (MG-Si, silicon seed crystals) needs it or the MV single furnace.

## 11.2 Modes (recipe types)

Slot layout = items in, items out, fluids in, fluids out. A single block's slots come from its first mode, so a single block's modes share one layout; every layout leaves one fluid input free for the purge (checked by the dry run, circuits count as item inputs).

| Type | Layout | Recipes (by EU/t tier) | What runs there |
|---|---|---|---|
| `fab_synthesis` | 3/2/4/3 | 61 (21 MV, 35 HV, 5 EV) | every reaction step: chlorosilanes, HF, KF, MSA → triflic acid, Pd/C, TEOS, TS-1, hydrazine → AIBN, PHOST route, Boc2O, PAG, THF, tributylamine, PO → PGMEA, MMA/MAA/tBMA, methacrylate resin, hopcalite, Formox, novolac, DNQ, HMDS, TMACl |
| `fab_blending` | 3/2/4/3 | 11 (4 LV, 2 MV, 4 HV, 1 EV) | Siemens feed gas, HNA etchant, KF·2HF and Simons electrolytes, excimer premixes, unfiltered resists, i-line resist, HMDS vapour, TMACl solution |
| `fab_wet_processing` | 3/2/4/3 | 3 | HCl quartz leach, HNA poly etch, molecular sieve (hydrothermal) |
| `fab_distillation` | 1/1/2/6 | 4 | chlorosilanes, Siemens vent gas, anhydrous HF, Zr/Hf split (column only) |
| `fab_cryogenic_rectification` | 1/1/2/6 | 3 | air (N2, O2, crude Ar, crude Ne, Kr/Xe), Ne/He, Kr/Xe |
| `fab_fractionation` | 2/1/3/2 | 21 (16 LV, 5 MV) | one cut per run of each distillation / cryogenic recipe except the Zr/Hf split: circuit = cut number, EU/t ÷ 4, time × 2, the other cuts are lost (GT's distillery rule) |
| `fab_purification` | 2/1/3/2 | 17 | XCDA (8 steps), carbon-bed TCS polish, NaF scrub of F2, argon deoxo, neon purification, Kr/Xe burner and drier, resist filtration, ultrapure water |
| `fab_electrolysis` | 2/2/3/4 | 2 | KF·2HF → F2, TMACl → TMAH developer |
| `fab_electrofluorination` | 2/2/3/4 | 1 | Simons ECF → CF3SO2F |
| `fab_calcination` | 3/2/2/2 | 5 | MG-Si (carbothermic reduction), NaHF2 and spent-acid regeneration, MoO3 roast, iron molybdate |
| `fab_cvd` | 3/2/2/2 | 1 | Siemens polysilicon |
| `fab_crystal_growth` | 3/2/2/2 | 4 | seed crystals for the eight boules (§12) |

Clean room: every non-thermal recipe from HV power on has `cleanroom(CLEANROOM)` (60 recipes). Single blocks need a GT Cleanroom for them; the four multiblocks with filter casings bring their own. Thermal recipes carry `blastFurnaceTemp` and never need a clean room.

## 11.3 Structures

```text
SMC Large Chemical Reactor, 5 wide × 5 deep × 4 high (aisles back → front, rows bottom → top)
  XXXXX XGGGX XGGGX XXXXX     X inert PTFE casing or hatches
  XXXXX GACAG GAAAG XFFFX     A air: the open vessel (its mode's fluid shows here while it runs)
  XXXXX GCPCG GAPAG XFFFX     C up to one heating coil (optional), else air (jacket)
  XXXXX GACAG GAAAG XFFFX     P PTFE pipe casing (stirrer / dip pipe)
  XXXXX XGSGX XGGGX XXXXX     G cleanroom glass, F filter casings (one type), S controller
SMC Rectification Column, 3 × 3, 3-10 high (layers bottom → top)
  XSX/XKX/XXX sump + cold box (K frostproof casing) | XXX/XPX/XXX × 1-8 trays (P PTFE pipe casing) | FFF/FFF/FFF filter hood
  X clean stainless casing or hatches
SMC Membrane Cell Hall, 3 wide × 4 high × 3-10 deep (aisles front → back: controller before the repeatable cells, for GT's auto-build)
  front XXX/XSX/XXX/FFF | cells XXX/EPE/XXX/FFF × 1-8 (E titanium frame electrodes, P PTFE pipe casing membrane) | back plate XXX/XXX/XXX/FFF
  X stable titanium casing or hatches
SMC Thermal Processing Furnace, horizontal tube furnace, 5 × 5 × 5 (aisles back → front, rows bottom → top)
  XXXXX XPPPX XPPPX XPPPX XXXXX     gas cabinet (P plascrete pipe casing: an MV machine, no PTFE)
  XXXXX GCCCG GCTCG GCCCG XXXXX     heater zone 1: C heating coils (one type) around T, the process tube (tempered glass)
  XXXXX GCCCG GCTCG GCCCG XXXXX     heater zone 2; G tempered-glass windows
  XXXXX XRRRX XRTRX XRRRX XXXXX     wafer-boat load station (R steel gearbox)
  XXXXX XGXGX XGSGX XGXGX XXXXX     front: S controller (GT's multi_furnace face) between two 3-high windows G;
                                    X heatproof casing or hatches
```

Hatches on any X, maximums only (nothing is required): 2 energy, 4 item inputs, 4 item outputs, 8 fluid inputs, 8 fluid outputs, 1 maintenance, 1 parallel, 1 laser hatch.

Filter roofs (`F`) take any GT cleanroom filter, all of one type, through `AF9Filters.cleanroomFilters()`: GT's own predicate with a fixed candidate order (GT's comes from a hash map, so its preview and the terminal's auto-build could pick the UV-tier sterilizing filter). First the MV `kubejs:plascrete_filter_casing` (below), then GT's Filter Casing, sterilizing filters last.

Plascrete Filter Casing (`kubejs:plascrete_filter_casing`, block in `photolithography.js` startup): shaped `PBP/IMI/PRP` (plascrete, iron bars, item filter, MV motor, steel rotor) → 2, `af9:plascrete_filter_casing`. AF9 Core registers it in `GTCEuAPI.CLEANROOM_FILTERS` (`AF9Filters.FilterType.PLASCRETE`, ISO 5 like GT's Filter Casing) at common setup, so it also works in GT's Cleanroom. The lithography machines (Mk1 line, Mk2 scanner) take only this one in their ceilings. Connected textures through LDLib, like GT's filter casing: `plascrete_filter_casing.png.mcmeta` points to `plascrete_filter_casing_ctm.png`, the 2×2 sheet of all sides connected / up-down / left-right / inner corners, so a ceiling reads as one recessed louvre panel with the plascrete frame around its outline (generated offline, like the wafer textures).

SMC LCR vessel fluid (`SmcReactorMachine` + `ModeFluidRender`): while the reactor is active, the air blocks of the 3 × 3 × 2 vessel (1-3 blocks behind the controller, its row and the one above, turned with the controller) are drawn as one fluid volume on every outer face, 1/16 inside the blocks: `fab_wet_processing` water, `fab_blending` distilled water, `fab_synthesis` Thermal's destabilized redstone (`thermal:redstone`; `gtceu:redstone` without Thermal). The mode is the running recipe's type (the selected mode is not synced to clients). The map lives in the startup script (`AF9MachineModels.workableCasingWithModeFluids(casing, overlay, {recipe type: fluid})` + `.hasBER(true)`); GT's "render fluids" client option turns it off.

## 11.4 Behaviour (AF9 Core)

- Changeover purge (`FabModifiers.PURGE`): when a machine starts a recipe other than the last one it finished (recipe ID), that first run also needs the family's purge fluid and takes the purge time on top (not overclocked or multiplied, applied after all other modifiers). The machine then keeps running the same recipe without purge. Counted on the console.
- Built-in clean room (`FabMultiblockMachine`): the filter casings' type decides it: plascrete or GT filter casing = ISO 5 (`CLEANROOM`), sterilizing filter casing = ISO 3 (both types). It is a GT `DummyCleanroom`, not a provider block, so a fab multiblock can still stand inside a GT Cleanroom (the thermal furnace has no filters and uses the surrounding one if any; its recipes need none).
- Parallels: PTFE pipe casings are trays (column) and membranes (cell hall), each one a parallel (`STRUCTURE_PARALLEL`, limited by inputs, output space and energy like GT's parallel hatch); the SMC LCR gets 2 with sterile filters. A parallel hatch multiplies on top.
- Overclocks: SMC LCR perfect (like GT's LCR) with the coil discount (1/20 EU/t per coil tier above cupronickel, at most half; no coil, no discount); column and cell hall non-perfect; thermal furnace GT's EBF rules (coil temperature + 100 K per energy tier above MV, heat discounts and perfect overclocks per 1800 K surplus); all multiblocks batch mode. Single blocks non-perfect.
- Single-block furnaces reach the temperature of their tier's coil: MV 1800 K, HV 2700 K, EV 3600 K, IV 4500 K, LuV 5400 K (`TIER_TEMPERATURE`); seed crystals above 5400 K (neutronium 7200 K, strange matter 9000 K, chromodynium 12000 K) need the multiblock with hot enough coils (chromodynium: Resonant Endion). EMI shows temperature, coil and the smallest single block on every thermal recipe (`FabRecipeInfo`).

## 11.5 Consoles

Multiblocks replace GT's text display with the fab console (190 × 125): header with family and status LED (offline, idle, running, no power, paused, maintenance, purging), one tile per mode with its run counter (click to switch; tooltip lists the mode's process steps), power bar (needed vs available EU/t), progress with the purge share of the run, plant panel (clean-room class, parallels, heat, the family's purge), and the process-step track of the active mode (purge cell first on a changeover). Clicking the changeover counter resets the counters. Single blocks keep GT's slot page for the active mode with a console strip above it (mode, steps, progress, clean class, heat, purge).

Process steps per mode: synthesis CHRG HEAT REAC QNCH SEPR DSCH; blending DOSE MIX DGAS DSCH; wet LOAD SOAK RNSE DRY; purification FEED ADSB FILT PROD; distillation FEED BOIL RFLX DRAW; cryogenic COOL LIQF RECT DRAW; fractionation FEED BOIL CNDS DRAW; electrolysis FILL POL ELEC STRP; electrofluorination FILL POL FLUR STRP; calcination LOAD RAMP SOAK COOL; CVD PURG RAMP DEPO COOL; crystal growth MELT DIP NECK BODY TAIL COOL.

## 11.6 Crafting (`af9:` shaped)

```text
<tier>_smc_chemical_reactor      FPF / UXU / CPC   X <tier>_chemical_reactor, U <tier> pump
<tier>_smc_fractionating_still   FPF / UXU / CPC   X <tier>_distillery, U <tier> pump
<tier>_smc_electrolytic_cell     FPF / WXW / CPC   X <tier>_electrolyzer, W <tier> single cable (Cu, Au, Al, Pt, NbTi)
<tier>_smc_thermal_furnace       FPF / WXW / CWC   X <tier>_arc_furnace, W <tier> heating wire, double (cupronickel … HSS-G)
  F fluid filter, P fluid pipe (polyethylene at MV, PTFE from HV: GT's PTFE only comes at HV), C circuit of the tier
smc_large_chemical_reactor       CRC / PMP / FXF   X GT Large Chemical Reactor + everything its recipe takes (HV circuits, stainless rotor, large PTFE pipes, HV motor), F filter casings
smc_rectification_column         CPC / FHF / UPU   HV hull, HV pumps, large PTFE pipes, filter casings, HV circuits
smc_membrane_cell_hall           CWC / EHE / FPF   HV electrolyzers, gold quadruple cable, HV hull, filter casings, large PTFE pipe, HV circuits
smc_thermal_processing_furnace   CKC / PXP / WCW   X Electric Blast Furnace, K cupronickel coil block, polyethylene pipes, copper cable, MV circuits
```

## 11.7 Quests

MV chapter: "The Fab Plant" (the four MV single blocks) before HF, XCDA and the i-line chemistry quests; "Thermal Processing Furnace" (optional). HV chapter: "SMC Large Chemical Reactor", "Rectification Column", "Membrane Cell Hall" (optional, after the fab plant). Texts `af9.quest.*.fabPlant|fabFurnace|smcLcr|smcColumn|smcCells`.

## 11.8 Verification (re-run on every change)

Dry run of the startup and server scripts with stubs (slots incl. circuits and purge headroom, temperatures, clean-room rule, single-block tank sizes, one layout per single block), subset conflicts within every fab type (0), tier reachability with the SMC machines (i-line inputs MV0, KrF HV0, ArF / ultrapure water EV0, HfCl4 HV0), Java checked against the GT 7.2.0 and LDLib sources plus a `javac` syntax pass; the real build runs on GitHub Actions (`.github/workflows/build-af9-core.yml`, the Maven repositories are not reachable from the agent sandbox).

---

# 12. Boule Melting and the Endion coils

Files: `startup_scripts/gtceu/boule_melting.js` (recipe type, Endion, Endionite, coils, charges, seeds, crucibles, new boules), `server_scripts/mods/gtceu/boule_melting.js` (recipes), AF9 Core `blast/BouleMelting` (adds the mode to GT's EBF at common setup, coil bonus, EMI info).

**The EBF gets a second machine mode, `gtceu:boule_melting`** (GT's mode tab; `ELECTRIC_BLAST_FURNACE.setRecipeTypes([blast, boule_melting])`, modifier list `COIL_BONUS` + GT's own `ebfOverclock` + batch). The mode keeps GT's EBF rules (coil temperature + 100 K per tier above MV, `ebf_temp` on every recipe). Max IO 3 items in, 1 out, 1 fluid in. Its EMI/JEI icon is the EBF (`setIconSupplier`: GT only sets icons for types a machine builder registers).

A boule is **ten times the material** of GT's old boule, and so it fits the EBF's three input slots the material comes pre-blended: **10 melt charges + 1 seed crystal + 1 crucible**, under a protective gas. Energy as the user asked: 2× GT's for silicon, 4× phosphorus, 6× naquadah, 8× neutronium; the new substrates follow on. A boule draws at most **4A of its tier** (two normal energy hatches, the most GT's EBF takes; LV-HV have no 4A hatches); above 4× the run is longer instead (`BOULE_MAX_AMPS`), so the total energy keeps the multiplier.

| Boule | Charge (SMC blending, per charge = GT's old boule) | Seed (SMC crystal growth) | Gas | Temp | EU/t (tier × amps) | Time | Crucible | → wafers |
|---|---|---|---|---|---|---|---|---|
| `gtceu:silicon_boule` | 32 EGS + tiny boron (MV) | 4 EGS + tiny boron, 100 Ar | 2500 Ar | 1784 K | MV × 2 | 9000t | fused quartz | 16 (GT) |
| `gtceu:phosphorus_boule` | 64 EGS + 8 phosphorus (HV) | 4 EGS + small P, 200 Ar | 10000 Ar | 2484 K | HV × 4 | 12000t | fused quartz | 32 (GT) |
| `gtceu:naquadah_boule` | 144 EGS + naquadah + GaAs (EV) | small Nq, 400 Ar | 80000 Ar | 5400 K | EV × 4 | 22500t | fused quartz | 64 (GT) |
| `kubejs:trinium_boule` | 192 EGS + 2 trinium + GaAs (IV) | small trinium, 400 Ar | 80000 Ar | 6000 K | IV × 4 | 24000t | fused quartz | 64 |
| `kubejs:naquadria_boule` | 240 EGS + 2 naquadria + 2 GaAs (LuV) | small naquadria, 200 Xe | 80000 Xe | 6800 K | IV × 4 | 29750t | fused quartz | 80 |
| `gtceu:neutronium_boule` | 288 EGS + 4 neutronium + 2 GaAs (ZPM) | small Nt, 400 Xe | 80000 Xe | 7200 K | IV × 4 | 36000t | fused quartz | 96 (GT) |
| `kubejs:strange_matter_boule` | 288 EGS + strange matter + 4 neutronium (UV) | small strange, 800 Xe | 160000 Xe | 9000 K | UV × 4 | 40000t | tritanium | 96 |
| `kubejs:chromodynium_boule` | 4 chromodynium + strange matter (UHV) | 4 small Qc + small strange, 400 Ed | 80000 endion | 12000 K | UHV × 4 | 48000t | tritanium | 128 |

Power: GT 7.2 recipes carry real amps (`EUt(VA[tier], amps)`), so the EBF's hatches must deliver voltage × amps every tick: silicon one normal MV hatch (2A), every other boule two normal hatches of its tier (4A). Two hatches of a tier count as the next tier for the EBF (GT's `EnergyContainerList`), which adds its 100 K but gives no overclock (4 × 4A would be needed). The recipe's voltage tier stays the listed tier (GT checks the per-amp voltage). The Endion parallels are capped by the EBF's voltage (GT's `ParallelLogic`), so a parallel only happens when the hatches can pay for it.

The charge blending is not thermal, so from HV on (every charge but silicon's) it needs a clean room like all non-thermal fab recipes (§11: single blocks a GT Cleanroom, the fab multiblocks bring their own filter ceiling).

Seeds: 1200t at the charge tier and the boule's temperature (SMC thermal single blocks reach MV 1800 K … LuV 5400 K; hotter seeds need the SMC Thermal Processing Furnace with coils). Crucibles: `af9:fused_quartz_crucible` (EBF, 6 quartzite dust, 1800 K, MV), `af9:tritanium_crucible` (assembler, 6 tritanium plates + 500 supercooled argon, UV). New boule cutting `af9:cut_<id>_boule` (lubricant 250, cleanroom).

**Endion** (`gtceu:endion`, gas, "Ed"): the End's noble gas. `af9:ender_air_separation` (centrifuge, replaces GT's): 10000 Ender Air → 3900 NO2 + 1000 D + **250 endion**, HV. `af9:distill_liquid_ender_air` (replaces GT's): GT's outputs + **8000 endion**, IV.

**Endionite** (`gtceu:endionite`, ingot, 5400 K highest-gas EBF, plate/foil/fine wire/rod/frame): `af9:endionite_dust` mixer 2 tungstensteel + 1 naquadah dust + 1000 endion → 3, EV.

| Coil | Temp | Level / discount / tier | Recipe | Boule Melting bonus |
|---|---|---|---|---|
| `kubejs:endion_coil_block` | 8100 K | 8 / 6 / 5 | 16 fine endionite wire + 8 endionite foil + tungstensteel frame + 2000 endion, IV | ×0.75 time, up to 2 parallels |
| `kubejs:resonant_endion_coil_block` | 12600 K | 16 / 16 / 8 | Endion coil + 32 fine endionite wire + 4 neutronium plates + UV field generator + 1000 supercooled endion, UV | ×0.5 time, up to 4 parallels |

The coils are `gtceu:coil` blocks (KubeJS), so every GT coil multiblock accepts them; the bonus is read by coil name in `BouleMelting.COIL_BONUS` (before the EBF overclock, so the overclock sees the parallel EU/t). Chromodynium (12000 K) needs Resonant Endion coils (tritanium 10800 + 800 at UHV is not enough). Textures: `kubejs:block/coils/<coil>` + `_bloom` (active glow).

# 13. Cryogenics: Supercooling Cryostat and Coolant Hatch

Files: `startup_scripts/gtceu/cryogenics.js`, `server_scripts/mods/gtceu/cryogenics.js`, AF9 Core `SupercoolerMachine`, `part/CoolantHatchPartMachine`, `common/AF9Modifiers.POWER_GATE`.

`gtceu:supercooling_cryostat` (HV, crafted at HV): modes `gtceu:dense_cooling` and `gtceu:supercooling` (0/0/1/1 IO). Every recipe is `EUt(VA[HV], 4)` = 1920 EU/t; `POWER_GATE` refuses to start below the recipe's full EU/t, so it needs **4A of HV (two normal HV energy hatches)**; `OC_PERFECT` above that.

| Gas | Dense cooling (1000 gas → 250 dense) | Supercooling (250 dense → 250 supercooled) |
|---|---|---|
| hydrogen | 200t, `dense_hydrogen` 14 K | 400t, `supercooled_hydrogen` |
| argon | 300t, `dense_argon` 84 K | 600t, `supercooled_argon` |
| xenon | 400t, `dense_xenon` 161 K | 800t, `supercooled_xenon` |
| endion | 600t, `dense_endion` 40 K | 1200t, `supercooled_endion` |

Supercooled fluids are 1 K fluids (GT refuses temperatures below 0 K); the cryostat's console and Jade show the supercooling target as −5000 K, the rating the user asked for.

Structure 5×5×5: frostproof shell (maximums only: 2 energy hatches, 2 fluid inputs, 2 fluid outputs, 1 maintenance), PTFE pipe heat exchangers, stainless gearboxes (compressors), tempered-glass windows around an air-filled cold chamber. Console (`ProcessConsoleWidget`): mode tiles DENSE / SUPERCOOL, power vs recipe, the fluid being made, the 4A gate, chamber temperature falling from the inlet to the target over the run, run-time bar.

**Coolant Hatch** `gtceu:<hv|ev|iv|luv|zpm|uv|uhv>_coolant_hatch`: a 1-slot fluid input hatch (1000 × 2^tier mB: 8000 at HV, 64 000 at LuV) whose tank only accepts `gtceu:supercooled_*` fluids; abilities `IMPORT_FLUIDS` + `CoolantHatchPartMachine.COOLANT_INPUT`. The Particle Accelerator and the Orbital Lithography Station take fluids only through it. Crafted from the tier's input hatch + 2 pumps (UV pumps for UHV) + frostproof casing + 4 PTFE plates + 1000 supercooled hydrogen, at the tier's voltage.

# 14. Particle Accelerator

Files: `startup_scripts/gtceu/particle_accelerator.js`, `server_scripts/mods/gtceu/particle_accelerator.js`, AF9 Core `ParticleAcceleratorMachine`, `console/AcceleratorConsoleWidget`, `console/SidePanelsUIWidget`, `client/render/LightRingRender`.

`gtceu:particle_accelerator` (crafted at ZPM): a storage ring **47 × 47, 7 high**, the layout of GTNH's Compact Fusion Computer (GT5-Unofficial `MTELargeFusionComputer`, layers L0 L1 L2 L3 L2 L1 L0 as top views in the startup script). An empty beam tube (`H`, 560 blocks that must be air: GTNH's superconducting coils, left out so the beam can be seen running through it) inside a shell of `gtceu:clean_machine_casing` (`C`, ~1660, the bending magnets; a diamond around the tube: 3 wide at y 1 and 5, 5 wide at y 2-4), four gates at the compass points with `gtceu:fusion_glass` (`B`) and `gtceu:naquadah_alloy_frame` corners (`F`, 128). The controller sits in the south gate's outer wall at y 3, facing out (GTNH has it on the inner wall; GTNH's drone-hatch spots are casing here). Parts, maximums only, one set for the whole ring (a shared predicate, so the maximums count across it): anywhere on the clean-steel casing (`C`, and GTNH's energy spots `E`, casing here too) or in the gates' glass spots (`I`): 2 coolant hatches, 2 item inputs, 2 item outputs, 1 maintenance, 4 energy / 2 laser hatches. `POWER_GATE` + `OC_NON_PERFECT`. The linac's blocks (beamline casing, RF cavity, spallation target housing) are gone.

Light ring, the beam: `LightRingRender` with a `wall` and no lightning (`AF9MachineModels.workableCasingWithLightRing(..., 'up', false, RING_WALL)`; its lightning, `arcs`, was switched off): centre 23 behind the controller at its height, radius 20 (along the middle of the empty beam tube, so the shell hides it and it shows through the gates' fusion glass, which is cutout), tube 0.3, lying flat, glowing twice as hard as GT's ring (`RING_GLOW`, `ILightRingMachine.ringGlow()`: its glow layers are added light, `AF9RenderTypes.LIGHT_RING_GLOW`, each that many times stronger up to 0.9; a fatter opaque white-hot core; with Shimmer the two inner glow layers bloom too; it glows in the tube and its gate windows, not over the shell: wide outer layers reaching past it, tried at four times, washed the whole ring in the colour), in the colour of the running mode (`ringColor`, synced: GT does not sync the active recipe type); its own sounds (`AF9Sounds`, vanilla files in af9's sounds.json): the working sound of all three modes `particle_accelerator` (the beacon hum at pitch 1.4, the magnets' whine), `particle_accelerator_ignite` when the ring lights (a respawn anchor charging, pitch 0.7), `particle_accelerator_pass` on every white flash (a trident's riptide, pitch 1.5, quiet: a bunch of particles going round; `ringPulseSound()`). Its sparks spit off the tube's inner wall (`wall` 2.5 in from the beam). The render's lightning (`arcs`, not used by the accelerator any more; `ILightRingMachine.ringArcSound()` for a sound where each big bolt lands): every tick, 40 % a bolt (4-7 ticks, reaching 25-70 % of the tube's inner radius into the middle, up to 1.5 above or below the ring's plane) and 50 % a dart (2 ticks, 1.5-4.5 long), at most 12 at once, their lengths counted from the tube's inner face (`wall` 2.5 in from the beam): each starts at the beam and breaks out of the inner wall, and the arms fork off its outer half; the sparks spit off the inner wall; each is a jagged path (midpoint displacement, re-forked every other tick so it crackles) with 1-3 arms off the bolts, drawn as camera-facing ribbons, a glow in the mode's colour under a white-hot core, in vanilla's lightning render type (additive); an electric spark where each lands.

EMI / JEI page (`AcceleratorRecipeUI`, installed on the three recipe types in common setup, the lithography page's style; 176 × 66): the items top left and, apart from them, the coolant bottom left, marked as coolant whatever the fluid (its slot in ice on dark frost with an icy glow, "COOLANT" over it, its own cryo line in ice and frost, and a hover text after GT's own lines: Coolant, only through a Coolant Hatch, keeps the superconducting magnets cold), piped through a manifold into the ring's west gate; the ring from above in the middle (a beam pipe of dots, four gates, the controller in the middle, the mode's short name above and PARTICLE ACCELERATOR below, the ring's tooltip names machine and mode); particle bunches racing round it in the mode's colour (neutrons: one bunch and a spray off the target inside the south gate every lap; collisions: two bunches and a flash where they meet; quarks: the same and three colour charges circling the machine); GT's arrow and the outputs stacked (`AcceleratorFlowWidget`). GT's own slots.

Screen (the orbital station's layout, `SidePanelsUIWidget` + `AcceleratorConsoleWidget` 384 × 148): left the ring from above: the three modes as tiles (click one to switch the mode, as GT's mode button: `setActiveRecipeType` + `updateTickSubscription`), the beam pipe with its four gates, the bunches racing round it faster as the run goes on, a progress ring, the state and the run-time bar. Neutron irradiation: one bunch and a neutron spray off the target at the south gate on every lap; heavy-ion collision: two bunches against each other and a flash where they meet (south and north gate, twice a lap); quark synthesis: the same, and the condensate growing in the middle with its three colour charges circling it. Right: the mode, the beam energy, the recipe's items (in > out, the running recipe or the active mode's last run), energy per run, ONLINE / OFFLINE, the coolant in the hatches, the run counter (`runs`, persisted) with RESET, a hint for the status (`af9.accelerator.hint.<status>`). Beside the inventory: PROCESS (coolant amount and fluid, magnets COLD / WARM, beam, RF, mode) and SYSTEM (status, power available / needed, tier, switch, runs).

| Mode | Recipe | Coolant | EU/t | Time |
|---|---|---|---|---|
| `gtceu:neutron_irradiation` | 4 neutronium wafers + beryllium spallation target → 4 `kubejs:transmuted_neutronium_wafer` | 1000 supercooled hydrogen | UV × 2 | 1200t |
| `gtceu:ion_collision` | magnetic trap + 16 lead ingots → `kubejs:qgp_trap` | 2000 supercooled argon | UV × 4 | 600t |
| `gtceu:quark_synthesis` | 4 QGP traps + neutronium dust → `gtceu:strange_matter_dust` + 4 traps | 4000 supercooled xenon | UHV × 2 | 1200t |
| `gtceu:quark_synthesis` | 8 QGP traps + strange matter dust → `gtceu:chromodynium_dust` + 8 traps | 4000 supercooled endion | UHV × 4 | 2400t |

Coolant grades: a recipe takes its coolant or any colder one. It asks for the grade's fluid tag (`#af9:coolant/<grade> <mB>`, set in the server script's `ServerEvents.tags('fluid')`): `af9:coolant/hydrogen` holds all four supercooled fluids, `.../argon` argon, xenon and endion, `.../xenon` xenon and endion, `.../endion` endion. EMI cycles through them in the coolant slot, and its hover says "Supercooled Argon or any colder one".

Materials: `strange_matter` (dust, "(uds)n"), `chromodynium` (ingot, 12000 K EBF, plate/foil/rod/frame, "Qc"), each with its own animated icon set (GT icon sets `strange_matter` and `chromodynium`, children of `shiny`; af9-core `assets/gtceu/.../material_sets/<set>`: an item model per shape whose untinted top layer is the animated art, the tinted layers empty; Chromodynium's block and frame grey, tinted by GT): Strange Matter a dark violet void with twinkling glints (32 frames), Chromodynium a pearl metal with a sheen sweeping through its colour charge, pink, orange, yellow, mint (24 frames), both at 2 ticks a frame; their wafers, boules, melt charges and seed crystals carry the same effects over their own art. Items: `kubejs:beryllium_spallation_target` (4 Be plates + 2 tungstensteel plates, LuV), `kubejs:magnetic_trap` (ZPM field generator + 4 NbTi plates + 2 tungstensteel plates, ZPM), `kubejs:qgp_trap`. Console: mode tiles NEUTRONS / COLLIDER / QUARKS, beam energy (1 GeV at ZPM, ×2 per tier), coolant fluid + amount (NO COOLANT status when dry), beam on/off, run-time bar.

# 15. Wafer and chip contamination

AF9 Core `wafer/WaferContamination` (server player tick, every 10 ticks): when a player has a wafer or a chip in the inventory (incl. armor/offhand slots), on the cursor or in the 2×2 crafting grid, it becomes `kubejs:contaminated_<substrate>_wafer` / `kubejs:contaminated_<chip>` (same count), unless the player
- wears an item of `#af9:wafer_gloves` (`gtceu:rubber_gloves`, `gtceu:hazmat_chestpiece`) in an armor slot or a Curios slot (GT tags its Rubber Gloves for the Curios `hands` slot; `compat/curios/CuriosCompat`, only when Curios is loaded), or
- stands strictly inside the walls of a formed, clean GT Cleanroom whose controller is within one chunk.
Spectators are exempt; creative players too only when `includeCreative = false` in `config/af9-common.toml` (default true, so testing in creative shows it). Machines, pipes, chests and ME systems never contaminate. Wafers = item tag `#af9:wafers` (all), `#af9:wafers/<substrate>` decides the contaminated item (server tags in `photolithography.js`: the blank wafer + every printed/derived wafer of the substrate; contaminated and broken wafers are not in them). Chips = `#af9:chips`, GT's 16 chips (`AF9_WAFERS.chips[].chip`); `kubejs:contaminated_<chip path>` items and textures (`textures/item/chips/`, GT's chip texture with grime, generated offline like the wafers) in `wafers.js`. Tooltip on every wafer and chip (`AF9Client`). Recovery: `af9:clean_contaminated_<substrate>_wafer` (SMC wet processing, 100 HF + 1000 distilled water, cleanroom) → the blank wafer (the print is lost); `af9:clean_contaminated_<chip>` (SMC wet processing, 10 HF + 250 distilled water, MV, no clean room) → the chip.

# 16. Jade

`compat/jade/AF9JadePlugin` + `AF9MachineProvider` (uid `af9:machine_status`, priority HEAD + 50 = right under the block name; compiled against Jade 11.6.3 like GT, the pack runs 11.13.2). Lithography machines: vacuum bar (`Vacuum 87.3 / 100 (pumping)`), status + mode, `Printing <item> on <substrate>`, run-time bar, break chance + line version. Process machines (cryostat, accelerator): status + mode, `Making <item/fluid>`, run-time bar, the console's readout lines.

---

# 17. Wireless energy hatches

AF9 Core `wireless/*`, startup `kubejs/startup_scripts/gtceu/wireless_energy.js`, recipes `server_scripts/mods/gtceu/wireless_energy.js`. Point-to-point, GT:NH-style links through a data stick, any distance and any dimension.

| Hull tier (look, crafting) | Max amps | Receiver id | Transmitter id | Built on |
|---|---|---|---|---|
| EV | 2 | `gtceu:ev_wireless_energy_receiver` | `gtceu:ev_wireless_energy_transmitter` | EV energy / dynamo hatch |
| IV | 4 | `gtceu:iv_wireless_energy_receiver` | `…transmitter` | IV 4A hatches |
| LuV | 16 | `gtceu:luv_…` | | LuV 16A hatches |
| ZPM | 100 | `gtceu:zpm_…` | | 4 ZPM 16A hatches |
| UV | 256 | `gtceu:uv_…` | | UV 256A laser target / source hatch |
| UHV | 1000 | `gtceu:uhv_…` | | UHV 1024A laser target / source hatch |

Assembler at the hull tier: the base hatch + 2 sensors (receiver) or emitters (transmitter) + a field generator + 2 circuits of the tier + 2 RF Transceiver chips + 576 mB soldering alloy, 30 s.

- **Transmitter** (`WirelessTransmitterHatch`, ability `OUTPUT_ENERGY`): a dynamo-type part for the Power Substation (or any multiblock that puts energy into its output hatches). It never emits into cables. Every tick it moves up to amps × voltage from its buffer (20 ticks of full throughput) into its own channel. **Voltage**: the highest input voltage of its multiblock's energy inputs (a PSS fed by UV hatches sends UV), re-read every second; a multiblock without energy inputs uses the tier set on the hatch's screen ([-] / [+], default EV). Breaking it deletes the channel.
- **Receiver** (`WirelessReceiverHatch`, ability `INPUT_ENERGY`): an energy input hatch for any multiblock, no cables. Every AF9 multiblock and every other multiblock of the pack takes it where it takes energy hatches (their patterns use `INPUT_ENERGY` or `autoAbilities`; GT's fusion reactors by tier). The Micro Universe Orb, which only takes laser hatches, also takes the receivers by id (`micro_universe_orb.js`, `I`). Single-block machines have no hatches. It works at its channel's voltage and pulls up to its amps × voltage per tick into its buffer (16 ticks of full input). GT reads a multiblock's hatch voltages when it forms, so a voltage change (the first link, a new substation input) makes the receiver re-form its multiblock (`onPartUnload` → async re-check). Unlinked or without a transmitter it reports 0 V and 0 A, so it does not lower the multiblock's voltage.
- **Channels** (`WirelessChannels`, overworld saved data `af9_wireless`): one per transmitter (UUID), with its buffer, voltage, amperage and position. The two ends only need their own chunks loaded.
- **Data stick** (GT's `IDataStickInteractable`): right-click a transmitter to write its link to the stick (`af9_wireless` tag, shown in the stick's tooltip); right-click a receiver to link it; shift-right-click a receiver to copy its link onto the stick.
- The hatches' energy role is fixed (GT caches a part's handler IO the first time a multiblock asks, and an energy container reports none at 0 V). Overlay tint: cyan receivers, orange transmitters.

# 18. The lithography process: cooling, computation, calibration, metrology, chemistry, new chips

What the process around the print adds to §5 (AF9 Core `LithoMachine`, `MetrologyStationMachine`, `AirConditioningHatchPartMachine`;
KubeJS `startup_scripts/gtceu/air_conditioning.js`, `litho_process.js`, `chips.js`; `server_scripts/mods/gtceu/litho_process.js`).

## 18.1 Air cooling: the Air Conditioning Hatch

The Photolithography Line and Scanner are built as clean rooms, so they cool with air (the Orbital Station is in space and cools with supercooled fluids, §5.6). A print puts a **heat load** into the chamber, in cooling units (CU); the **Air Conditioning Hatches** of the structure (ability `af9_air_conditioning`, up to 2, MV-IV) have to carry it away. A hatch is worth 1 CU at MV and doubles per tier.

| Node | 350 nm | 200 nm | 100 nm | 80 nm | 65 nm |
|---|---|---|---|---|---|
| Heat load (CU) | 1 | 2 | 4 | 8 | 16 |

| Hatch | MV | HV | EV | IV |
|---|---|---|---|---|
| Cooling units | 1 | 2 | 4 | 8 |
| Draw while a print runs | ¼ A of its tier | | | |

- **Short of cooling: no print** (status NO COOLING, code 15). The hatches draw their power from the machine's own energy hatches every 10 ticks while a print runs (`updateCooling`); a print during which they went without power breaks **twice** as often (`COOLING_LAPSE_FACTOR`, status text COOLING LOST POWER).
- **Every doubling of the CU above the heat load** (up to 2): the print runs ×0.9 as long (`Coolant.TIME_FACTOR`, in `LITHO_VERSION`) and breaks ×0.8 as often (`Coolant.BREAK_FACTOR`). Two IV hatches (16 CU) are exactly what a 65 nm print needs.
- Config `config/af9-common.toml` `[lithography] airCooling = true`: switch it off and the Line and Scanner need no hatches.
- Recipes: an assembler recipe per tier (hull, pump, motor, 2 circuits, 4 tier plates, soldering alloy).
- **The warm air** leaves from the hatch's front. `AirConditioningHatchPartMachine` implements `thermal/IHeatEmitter` (`getHeatOutput()` in heat units per tick: 2 per CU in use while the machine cools, `getHeatPos()`, `getHeatDirection()`): **the hook for the Temperature Update**. Nothing reads it yet; the temperature system (warmth around machines the player feels, the hotbar message, the screen effect) is meant to collect the emitters and warm the blocks in front of them.

Crafting (`litho_process.js`, assembler, programmed circuit 1): the tier's machine hull + pump + motor + 2 circuits + 4 plates (aluminium MV, stainless steel HV, titanium EV, tungsten steel IV) + 144 mB soldering alloy. The circuit tells the IV recipe from the Scanner's, which holds all of it and more (`tools/lint` R7).

## 18.2 OPC: computation that improves the yield

Every node up to 20 nm has an **OPC demand** (optical proximity correction and alignment, `LithoMode.opcDemand`), drawn each tick while a print runs from the machine's computation source (a Bus Connector, or a computation hatch on the orbital station):

| Node | 350 | 200 | 100 | 80 | 65 | 50 | 20 | 7 | 1 |
|---|---|---|---|---|---|---|---|---|---|
| OPC CWU/t | 2 | 4 | 8 | 16 | 24 | 32 | 48 | – | – |

It is an extra, the print runs without it. The share of the demand the computation met over the print (`opcSum / opcTicks`) cuts the break chance by up to 30% (`OPC_BONUS`): factor `1 − 0.3 × share`. The 7 and 1 nm nodes already draw 32 / 96 CWU/t as a recipe input (§5.6).

## 18.3 Calibration

Every finished print wears the optics and stages (`driftPerPrint`, percentage points per print):

| Node | 350 | 200 | 100 | 80 | 65 | 50 | 20 | 7 | 1 |
|---|---|---|---|---|---|---|---|---|---|
| Drift per print | 0.5 | 0.75 | 1 | 1.5 | 2 | 2.5 | 3 | 4 | 5 |

- The break chance rises with the drift: factor `1 + (100 − calibration)/100` (doubled at 0 %).
- **Below 20 %** no print starts (status CALIBRATE, code 16).
- An **idle** machine below 70 % takes a **Calibration Wafer** (`kubejs:calibration_wafer`, assembler: silicon wafer, chromium plate, 100 mB photoresist → 4) from one of its input buses and calibrates itself in 20 s (status CALIBRATING, code 17); the calibration is persisted. A Metrology Station does it for every machine on its bus (§18.5).

## 18.4 The break chance, all together

`p = min(0.95, (base + (100 − vacuum)/100 × 0.5) × 0.75^version surplus × coolant (orbital) × cooling × OPC × calibration × metrology)`; cooling = 0.8^doublings (2.0 if the hatches lost power); OPC = 1 − 0.3 × share; calibration = 1 + (100 − calibration)/100; metrology = 0.85 with a Metrology Station's feedback on the bus network (else 1). The consoles show the result; the Line's console has the AIR COOLING row and the OPC / CAL line, the Scanner's the same under its hint, Jade both.

## 18.5 Metrology Station

`gtceu:metrology_station` (HV assembler recipe), 3 × 3 × 5 of plascrete (aisles front → back: the controller, a measuring tube with cleanroom glass, the wafer stage, a back); item input and output bus, energy (2), fluid input, maintenance and **a Bus Connector** (maximums). Recipe type `gtceu:metrology`, `af9:metrology_run`: a Calibration Wafer + 100 mB distilled water, **24 CWU/t**, 600 t at HV; the wafer comes back 9 times in 10.

A finished run **calibrates every Line, Scanner and Orbital Station on the station's bus network** (`MetrologyStationMachine.runFinished`) and starts the **feedback**: for 10 minutes after the run, and while one is measuring, their prints break ×0.85. The station's screen lists the machines on its bus with their calibration.

## 18.6 Chemistry

All in the SMC fab machines (§11), recipes in `server_scripts/mods/gtceu/litho_process.js`, materials in `startup_scripts/gtceu/litho_process.js`.

- **RCA clean and strip.** SC-1 (ammonia, peroxide, water 1:1:5 → `sc1_solution`), SC-2 (HCl, peroxide, water 1:1:6 → `sc2_solution`), piranha (sulfuric acid, peroxide 3:1 → `piranha_solution`), blending at MV. The clean-up of **contaminated wafers** is now a real RCA clean (SC-1, a dilute HF dip, SC-2: `af9:clean_contaminated_<substrate>_wafer`); **broken wafers** can be **reworked** (`af9:rework_broken_<substrate>_wafer`: piranha strip, SC-1, SC-2 → the blank wafer back **60 %** of the time, spent piranha out) beside the grinding. Spent piranha + calcium hydroxide → gypsum (`af9:spent_piranha_neutralisation`).
- **Ethyl lactate**: ethanol → acetaldehyde (copper) → lactonitrile (+ HCN, base) → lactic acid (+ water, sulfuric acid; ammonium bisulfate out) → ethyl lactate (+ ethanol, acid catalyst), MV.
- **BARC** (bottom anti-reflective coat): naphthalene nitrated to nitronaphthalene (the dye, mixed acid), then methacrylate resin + dye in ethyl lactate (`af9:barc`, HV, clean room). **A fluid of the Coater Track for the 200, 100, 80, 65 and 50 nm nodes** (60 × 1.5^index mB). **TARC** (the immersion top coat, 65 and 50 nm): tetrafluoroethylene + PGMEA (`af9:tarc`, 500 + 1000 → 1500, HV, clean room), the Coater Track's fourth fluid.
- **Etch plasma** (§18.8): `af9:tetrafluoromethane` (carbon + 2000 fluorine, HV, clean room), `af9:etch_plasma_gas` (500 CF4 + 250 chlorine + 2000 argon + 250 oxygen → 3000 mB, HV, clean room). A print from 200 nm on burns `round(50 × 1.5^index)` mB.
- **Metal-oxo EUV resists** (IV, clean room): `af9:zirconium_oxo_resist` (ZrCl4 + methacrylic acid + PGMEA + UPW → 4000 `euv_photoresist`) and `af9:hafnium_oxo_resist` (HfCl4 → 5000), beside the tin-oxo resist (§6.6). They give the zircon chain's zirconium and hafnium tetrachloride a use.

## 18.7 The new chip families

Six chips beyond §5.3b, one per family the finer substrates open up, printed and cut like the others (`chips.js`, `AF9_WAFERS.chips`; textures made on the existing chips' shared shading, each substrate's palette, a 6 × 6 glyph):

| Chip | Family | Own substrate | Reticle (lens / blank) | Dies | Functional layer (chain) |
|---|---|---|---|---|---|
| `saw_filter` | acoustic wave (SAW / BAW) | naquadah, 100 nm (EV) | red / phase-shift | 8 | aluminium nitride, lithium niobate |
| `photonic_ic` | silicon photonics | trinium, 80 nm (IV) | cyan / phase-shift | 6 | silicon nitride (+ germanium, InP from GT) |
| `spin_logic` | spintronics (MTJ logic) | naquadria, 65 nm (LuV) | lime / phase-shift | 8 | CoFeB (+ magnesia from GT) |
| `tmd_logic` | 2D materials | neutronium, 50 nm (ZPM) | pink / EUV | 6 | tungsten diselenide, hexagonal boron nitride (+ molybdenite, graphene from GT) |
| `memristor` | neuromorphic (ReRAM, PCM) | transmuted neutronium, 20 nm (UV) | cyan / EUV | 8 | GST alloy (Ge2Sb2Te5) |
| `quantum_dot_ic` | sub-atomic (quantum dots, SET) | strange matter, 7 nm (UHV) | yellow / EUV | 4 | CdSe quantum-dot colloid |

The layers are made in the fab machines (`af9:aluminium_nitride`, `lithium_niobate`, `silicon_nitride`, `cobalt_iron_boron`, `tungsten_diselenide`, `boron_nitride`, `gst_alloy`, `quantum_dot_colloid`: thermal steps in the thermal furnace, the rest from HV power on in a clean room) and are taken, with the chips, by the uses below. The reticles and cuts of the strange-matter chips run at UV (no UHV machine needed). Tier of each family: the substrate's.

**Uses.** The **wireless energy hatches** (EV-UHV) take a SAW filter and 2 aluminium nitride films on top (`wireless_energy.js`). The **card tiers** of the computation arrays (below) are made from the others.

| Card tier | CPU | GPU | RAM |
|---|---|---|---|
| Photonic | 2 photonic ICs, 4 Si3N4 | 4 photonic ICs, 8 Si3N4 | 4 spin logic, 2 CoFeB |
| Atomic | 2 TMD logic, 2 WSe2, 2 hBN | 4 TMD logic, 4 WSe2, 4 hBN | 4 memristors, 2 GST |
| Sub-atomic | 2 quantum-dot ICs, 250 mB colloid | 4 quantum-dot ICs, 500 mB colloid | 2 quantum-dot ICs, 4 memristors, 250 mB colloid |

All on a multilayer fibre-reinforced board with 8 YBCO wire, at UV in a clean room, in the circuit assembler with a **programmed circuit** (1 CPU, 2 GPU, 3 RAM: the three cards of a tier take the same things in different amounts, so without it the machine could make either) and soldering alloy; the Sub-atomic cards take the colloid as ink *instead of* solder (GT's assembler and circuit assembler have **one** fluid slot). Numbers of the cards: `docs/machine-bus.md` §9.

## 18.8 The Coater Track, coated wafers, the etch plasma

The track of the spec is split in two: the coating is a multiblock of its own, the developing stays in the Line (a wafer would otherwise have to carry a state per step; one item per substrate, `coated_<substrate>_wafer`, is enough since exposure, PEB and develop happen in one machine run).

- **Coater Track** (`gtceu:wafer_coater`, MV, §6.5b): spin coat, BARC, topcoat, bake. Kept as a plain GT multiblock (`WorkableElectricMultiblockMachine`, no AF9 Core class), 3 × 3 × 6 of plascrete; Parallel Hatch, Bus Connector; modifiers `PARALLEL_HATCH + OC_NON_PERFECT`.
- **Coated wafers**: the prints take them (`kubejs:coated_<substrate>_wafer`, tagged like the blanks: they contaminate in a bare-handed inventory and are RCA-cleaned back to a blank wafer; a broken print's rework strips the resist the same way). Textures: `tools/textures/coated_wafers.py` (each substrate's blank with the film of its node's resist; the strange-matter one is animated).
- **Waste and recovery**: 30 mB spent resist solvent per 100 mB resist, distilled back to **60 %** PGMEA (`af9:recover_resist_solvent`); a coater without a fluid output hatch stops when its tank is full.
- **Etch plasma**: from 200 nm every print burns CF4 / Cl2 / Ar / O2 plasma (§18.6). The 350 nm print etches wet and stays an MV recipe. Not built: a fluid output on the prints for the spent plasma (the print types have no fluid outputs; the Line, Scanner and Orbital hatches would need output hatches and the consoles a row), and the HF / calcium fluoride recycling loop that would hang on it.

## 18.9 Mask classes

`maskClass(substrateIndex)`: chrome for 350 and 200 nm, phase-shift for 100, 80 and 65 nm, EUV from 50 nm (the orbital station). The 22 chips with a PSM reticle and the 27 with an EUV one exceed the 16 lens colours, so the finer classes are written from the chip's own (native) reticle, not from a lens (§6.4). Tiers: chrome MV, phase-shift EV, EUV ZPM. Two chips changed class with this: the ASoC (native 100 nm, now phase-shift, lens purple → orange so it does not repeat the VPU's) and the HASoC (native 50 nm, now EUV). Textures: `tools/textures/reticles.py` draws every class from the chip's reticle (chrome: dark chrome with the pattern clear; phase-shift: lavender, purple pattern; EUV: a blue mirror with a black pattern).

## 18.10 EUV optics

No glass passes 13.5 nm light, so every optic of an EUV tool is a mirror: `kubejs:mo_si_mirror` (§6.3: ULE glass substrate + molybdenum + silicon sputtered in argon, LuV). The **EUV Light Source** takes 2 (its collector), the **Orbital Lithography Station** 6 (its projection optics: it is crafted at ZPM, where the LuV mirrors are available). Textures: `tools/textures/optics.py`.

## 18.11 Multi-patterning

A **screwdriver on the controller** of a Line or Scanner (not while a print runs) switches multi-patterning: the machine then prints the mode **one version above its own** (a V2 line the 100 nm mode, a V1 scanner 65 nm), exposing every layer twice: **2× the run time, 1.5× the break chance, 2× the OPC demand, 2× the calibration wear** (`LithoMode.MULTI_PATTERNING_*`, `LithoMachine.isMultiPatterned`, `LITHO_VERSION`). The consoles show **MP x2** in their tuning line, Jade a "Multi-patterned" line. The Orbital Station has no versions and no multi-patterning.

# Appendix A. File map

```text
af9-core/ (Forge mod `af9`, GTCEu 7.2.0 addon; built by GitHub Actions, jar → mods/)
  litho/LithoMode.java                 # the 9 modes: substrate, node, tier, light (λ, NA, k1), resist, colour, base break chance, break/speed maths
  machine/LithoMachine                 # shared by the litho machines: vacuum cleanliness, break roll, counters, LITHO_GATE, STRIP_BROKEN, §18: air cooling, OPC, calibration, metrology feedback
  machine/MetrologyStationMachine      # §18.5: calibrates the litho machines on its bus, feedback
  machine/part/AirConditioningHatchPartMachine # §18.1: cooling units, draw, IHeatEmitter
  thermal/IHeatEmitter                 # the hook for the Temperature Update (heat units per tick, position, direction)
  machine/PhotolithographyLineMachine  # Mk1 line + Mk2 scanner (Spec), versions, LITHO_VERSION, preview pages, recipe info
  machine/OrbitalLithographyMachine    # the 1 nm station: orbit check
  machine/LithoRecipeLogic             # break roll when a print finishes (swaps the output for the broken wafer)
  machine/LithoConsoleWidget           # the litho console (tiles, vacuum, break chance, power, printing, run time, counters)
  machine/ProcessMachine               # base of the cryostat and the accelerator (mode buttons, status, coolant, output)
  machine/SupercoolerMachine           # Supercooling Cryostat (4A HV gate, chamber temperature readout)
  machine/ParticleAcceleratorMachine   # Particle Accelerator: the storage ring's light ring, runs, beam energy, coolant
  machine/console/AcceleratorConsoleWidget # its screen: the ring from above, the run, side panels
  machine/console/SidePanelsUIWidget   # GT's screen with a panel each side of the inventory (orbital, accelerator)
  machine/part/CoolantHatchPartMachine # coolant hatch: fluid input hatch that only accepts gtceu:supercooled_*
  machine/console/ConsoleWidget        # console base (frame, tiles, run-time bar, sync); ProcessConsoleWidget = cryostat/accelerator console
  common/AF9Modifiers, IPowerGated     # POWER_GATE: no start below the recipe's full EU/t
  blast/BouleMelting                   # EBF second mode gtceu:boule_melting + Endion coil bonus
  wafer/WaferContamination             # player inventory → contaminated wafers (gloves / clean Cleanroom protect)
  compat/jade/AF9JadePlugin, AF9MachineProvider  # Jade: vacuum bar, status, product, run time, info lines
  client/AF9Client                     # wafer tooltip (contamination warning), registers the dynamic renders
  client/render/ModeFluidRender        # GT dynamic render: a fluid per machine mode inside a running multiblock
  client/render/LightRingRender        # GT's fusion ring for ILightRingMachine: halo, sparks, pulse and ignition sounds
  litho/Coolant                        # the orbital station's coolant grades (supercooled H2 < Ar < Xe < endion)
  litho/LithoRecipeUI, LithoFlowWidget # the lithography recipes' EMI page: items and track piped into the machine
  machine/OrbitalField                 # the orbital station's magnetic field (gravity through Ad Astra)
  compat/adastra/AdAstraCompat         # registers the field's gravity listener when Ad Astra is loaded
  common/AF9Sounds                     # sound events (assets/af9/sounds.json: vanilla sounds pitched down)
  machine/AF9MachineModels             # workable casing model + ModeFluidRender or LightRingRender, for KubeJS .model(...)
  machine/fab/SmcReactorMachine        # SMC LCR: FabMultiblockMachine + IFluidRenderMulti (the open vessel's blocks)
  pattern/AF9Filters                   # Plascrete Filter Casing as a GT cleanroom filter; ordered filter predicate
  compat/curios/CuriosCompat           # gloves in a Curios slot
  wireless/WirelessEnergyHatch         # §17: shared part of the wireless hatches (run-time voltage, channel, screen)
  wireless/WirelessTransmitterHatch    # dynamo side: substation -> channel, voltage from the multiblock's inputs
  wireless/WirelessReceiverHatch       # energy input side: channel -> multiblock, re-forms it on a new voltage
  wireless/WirelessChannels, WirelessLink  # saved channels; the data stick's link
  fab/FabFamily, IFabMachine           # §11: the four families (purge fluid/time, console colour, process steps per mode)
  fab/FabModifiers, FabRecipeLogic     # PURGE, STRUCTURE_PARALLEL, COIL_DISCOUNT, TIER_TEMPERATURE, THERMAL_OVERCLOCK; changeover bookkeeping
  fab/FabRecipeInfo                    # temperature, coil and single-block tier on the thermal modes' EMI pages
  machine/fab/FabMultiblockMachine     # SMC multiblocks: built-in clean room from filter casings, PTFE-pipe parallels, counters, console
  machine/fab/FabTieredMachine         # SMC single blocks: GT slot page + console strip, furnace temperature per tier
  machine/fab/FabConsoleWidget         # the fab console (full for multiblocks, strip for single blocks)
kubejs/startup_scripts/gtceu/wafers.js             # AF9_WAFER_TABLE: new blank substrates, broken + contaminated wafers (a print is GT's own chip wafer)
kubejs/startup_scripts/gtceu/air_conditioning.js   # §18.1: the Air Conditioning Hatch MV-IV
kubejs/startup_scripts/gtceu/litho_process.js      # §18: calibration wafer, chemistry materials, family materials, metrology recipe type + station, coater recipe type + station
kubejs/startup_scripts/gtceu/reticles.js           # §6.4 / §18.9: the 67 reticles of the three mask classes
tools/lint/                                        # the linters (README there): scripts, quests, assets, facts, self-test
tools/textures/                                    # reticles.py, coated_wafers.py, optics.py: draw the textures
kubejs/startup_scripts/gtceu/chips.js              # AF9's own chips (§5.3b) and the six new families (§18.7)
kubejs/server_scripts/mods/gtceu/litho_process.js  # §18: hatches, calibration wafer, chemistry, family chains, the three new card tiers, the Metrology Station
kubejs/startup_scripts/gtceu/photolithography.js   # litho, XCDA, i-line and EUV resist materials, reticles, sieves, light sources, 9 recipe types, both litho structures, tooltips
kubejs/startup_scripts/gtceu/boule_melting.js      # endion, endionite, charges, seeds, crucibles, new boules, Endion coils, recipe type boule_melting
kubejs/startup_scripts/gtceu/cryogenics.js         # dense/supercooled fluids, dense_cooling + supercooling, Supercooling Cryostat, coolant hatches LuV-UHV
kubejs/startup_scripts/gtceu/particle_accelerator.js # strange matter, chromodynium, traps, 3 recipe types, the Particle Accelerator ring
kubejs/startup_scripts/gtceu/electronics_metallurgy.js # tier alloys (Al-Si, Kovar, Pt-Ir), zircon/zirconia/chlorides, zirconium properties
kubejs/server_scripts/mods/gtceu/photolithography.js # AF9_WAFERS table + wafer tags, machine/light-source crafting, XCDA/i-line/EUV chemistry, prints, derived wafers, cutting, reclaim/clean, removals
kubejs/server_scripts/mods/gtceu/boule_melting.js  # Ender Air → endion, endionite, coils, crucibles, charges/seeds/boules, new boule cutting, GT boule removals
kubejs/server_scripts/mods/gtceu/cryogenics.js     # cryostat + coolant hatch crafting, dense cooling + supercooling
kubejs/server_scripts/mods/gtceu/particle_accelerator.js # accelerator + consumables crafting, neutron irradiation, ion collision, quark synthesis
kubejs/server_scripts/mods/gtceu/electronics_metallurgy.js # alloy mixers, zircon chain, zircon sands ore vein
kubejs/startup_scripts/gtceu/fab_chemistry.js      # 72 fab-chemistry materials + the resists, laser gases, ultrapure water the line uses
kubejs/server_scripts/mods/gtceu/fab_chemistry.js  # lines 1-5 (§6.12-6.16): Siemens polysilicon + EGS, fluorochemicals, air gases, KrF / ArF resist, ultrapure water (fab_* types, `column()` makes the still cuts)
kubejs/startup_scripts/gtceu/fab_machines.js       # §11: 12 fab recipe types (slot layouts), 4 SMC single-block families (MV-LuV), 4 SMC multiblocks (structures, modifiers)
kubejs/server_scripts/mods/gtceu/fab_machines.js   # §11: crafting of the SMC machines
kubejs/server_scripts/mods/gtceu/mv_circuits.js     # MV circuits without transistors/diodes (Al-Si wire, Kovar pins)
kubejs/server_scripts/mods/gtceu/tiered_circuits.js # HV-LuV circuits: plain chips + tier metals
config/ftbquests/quests/chapters/*.snbt             # quests (§6.10); text in kubejs/assets/kubejs/lang/en_us.json (af9.quest.*)
kubejs/assets/gtceu/lang/en_us.json                 # machine/recipe-type/block names, tooltips, mode descriptions, AF9 material names
af9-core/src/main/resources/assets/af9/lang/en_us.json # consoles, Jade, recipe info, substrate/light names, wafer messages
kubejs/assets/kubejs/textures/item/wafers/*         # 5 new blanks, 9 broken, 9 contaminated, the own chips' wafers, the calibration wafer
kubejs/assets/kubejs/textures/item/boules/*         # melt charges, seed crystals, 4 new boules, 2 crucibles
kubejs/assets/kubejs/textures/item/accelerator/*    # spallation target, magnetic trap, QGP trap
kubejs/assets/kubejs/textures/item/                 # photomask blank, 12 reticles, molecular sieve (+ saturated), dry resist cartridge
kubejs/assets/kubejs/textures/block/*               # KrF / ArF lasers, EUV source
kubejs/assets/kubejs/textures/block/coils/*         # Endion + Resonant Endion coils (+ _bloom active layers)
```

Removed with the substrate redesign: wafer packages (`kubejs:<chip>_wafer_package`, `kubejs/assets/kubejs/textures/item/litho/*`), per-mode chip models and textures (`kubejs/assets/af9/models|textures/item/litho/*`, `kubejs/assets/gtceu/models/item/<chip>.json` predicate overrides), `compat/emi/AF9EmiPlugin`, the `af9:litho_mode` predicate, recipe types `gtceu:lithography_muv|huv|euv|xuv|luv`.

# Appendix B. Copy-paste snippets (KubeJS)

```js
// server scripts share AF9_WAFERS (photolithography.js)
AF9_WAFERS.chipStack('ram', 8)                                   // '8x gtceu:ram_chip' (chip items without _chip: simple_soc, soc, advanced_soc, highly_advanced_soc)
// substrates by index: 0 silicon, 1 phosphorus, 2 naquadah, 3 trinium, 4 naquadria, 5 neutronium,
// 6 transmuted_neutronium, 7 strange_matter, 8 chromodynium (AF9_WAFERS.substrates[i].id)
AF9_WAFERS.printed(2, AF9_WAFERS.chip('cpu'))   // 'kubejs:naquadah_cpu_wafer'
AF9_WAFERS.printed(0, AF9_WAFERS.chip('cpu'))   // 'gtceu:cpu_wafer' (the chip's own substrate); null below it
AF9_WAFERS.dies(2, AF9_WAFERS.chip('cpu'))      // 24 chips per naquadah CPU wafer
AF9_WAFERS.wafersOf(5)                          // every neutronium wafer item (blank + printed), = tag #af9:wafers/neutronium
```

# Appendix C. Research provenance (5 parallel agents)

- Agent A (codebase explore): existing IDs, wafer grades, line logic, version drift, 13 contradictions to avoid — folded into §§5/10.
- Agent B (wafer fab): quartz → MG-Si (11-13 MWh/t, 1414C) → Siemens (1080-1100C, 9N-11N) / FBR → CZ (0.5-2.5mm/min, Ar, B/P, Oi) / FZ → wire-saw → RCA SC-1/SC-2 + CMP + epi → SEMI specs/grades — folded into §1.
- Agent C (lithography/FEOL/BEOL): cleanroom ISO, HMDS/DNQ/CAR/MOR, DUVi 193nm NA1.35 vs EUV 13.5nm, RIE/DRIE vs HF/BOE/KOH/TMAH, implant B/P/As/Sb + RTP, PVD/CVD/ALD/epi, CMP, 400-1400 steps, nodes/CPP/MMP, KLA/CD-SEM/PCM/sort — folded into §2.
- Agent D (chips/SoCs): 36-term hierarchy transistor→system with what/made/used/example — folded into §3 table.
- Agent E (GTCEuM 1.20.1): 7.x tiers/IDs/machines/EU-t/fluids/cleanroom/what-is-missing (UNCERTAIN flagged) — folded into §4 + §§6-8.
- Numbers cross-checked against `LithoMode.java` + server scripts (§§5.2/5.3). Any conflict → code wins, flagged in §10.

*End of spec — after any change run the §10 checklist (dry run, item/fluid id check, lang keys, textures) and let GitHub Actions build AF9 Core.*
