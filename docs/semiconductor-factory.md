---
title: "AF9 Semiconductor Factory — Agent-Optimized Spec: Wafers, Silicon, SoCs, Chips, Lithography"
branch: "main"
head_commit: "see git log (2026-09-25 b: fab chemistry lines §6.11-6.16 — Siemens polysilicon + CZ, fluorine + triflic acid, air separation, full KrF/ArF resist syntheses, Formox, TMAH electrolysis; 2026-09-25 a: real light source + resist per mode (i-line, KrF, ArF, ArF immersion), excimer gas + rare-gas recovery, chemically amplified resists, ultrapure immersion water; 2026-09-24: all GT chip wafers in the line, tier-tagged HV-LuV circuits, XCDA, zircon, LUV high-k)"
minecraft: "1.20.1"
forge: "47.4.0"
gtceu: "7.2.0 (GregTech CEu Modern)"
kubejs: "2001.6.5-build.16"
af9_core: "0.1.0 (mod_id `af9`)"
gtceu_config: "enableCleanroom=true, cleanMultiblocks=false, enableMaintenance=true, highTierContent=false, orderedAssemblyLineItems=true"
status: "Matches main. Modes 4A own-tier MV to LuV + OC_PERFECT, each mode on its own substrate and with its real light source and resist (MUV mercury i-line + DNQ, HUV/EUV KrF + KrF CAR, XUV ArF + ArF CAR, LUV ArF immersion; §5.3), excimer modes burn laser gas, every consumable comes from a real multi-step chemical line that is reachable at its tier (§6.11-6.16; boules from Siemens polysilicon), all 16 GT chip wafers printed or derived in the line, HV-LuV circuits need tier-matched chips (HV/EV bootstraps one mode lower) and their tier's metals (§6.8), XCDA comes from a 5-step chemical-reactor chain (§6.5), LUV needs HfCl4 from the zircon chain. Older designs (Mk I-III modules, high_grade/premium items, silicon-only line, zeolite one-step XCDA, one DNQ resist for every mode) are superseded."
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

## 0.2 Source-of-truth files (main)

See Appendix A for the full map. The numbers live in two places that must agree: `AF9_LITHO` in
`kubejs/server_scripts/mods/gtceu/photolithography.js` and `LithoMode` in `af9-core/src/main/java/com/af9/core/litho/`.

> AGENT: if any doc contradicts these files, the files win. The old `high_grade` / `premium` / `Mk I-III + KrF + Twin-Stage` design from `9745aca` is dead and its items/blocks are no longer registered. Do not invent `kubejs:ram_wafer_high_grade`, `af9:ram_wafer_mk1` or `krf_excimer_laser_module`.

## 0.3 One-paragraph mental model

Real fab: quartz → MG-Si → ultra-pure polysilicon → Czochralski boule → diamond-wire wafers → RCA clean + CMP → repeat 100s of times: HMDS prime → resist coat → bake → expose through reticle → bake → develop → etch → implant → deposit → CMP. Wafer → probe → dice → package → PCB. In AF9/GregTech 1.20.1 this is compressed to: Siemens polysilicon → CZ boule (SMC thermal furnace) → cutter blank wafer → Photolithography Line (reticle + 5 track chemistries + the mode's laser gas / immersion water / HfCl4) → NBT wafer → cutter dies → circuit assembler. Reticles are the Minecraft reticle/mask, chemistries are HMDS/photoresist/TMAH fluids, the stepper is the multiblock, dies-per-wafer scaling is the NBT math. Each mode uses the light source its real node used (mercury i-line 365 nm → KrF 248 nm → ArF 193 nm → ArF immersion) and the resist chemistry made for that light (DNQ-novolac → chemically amplified PHOST → chemically amplified methacrylate).

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
- Minecraft map: AF9 CZ recipes in the crystal-growth mode of the SMC thermal furnaces (§6.1, §11): `electronic_grade_silicon` (etched poly chunks) + boron (p-type, from the BCl3 cut) or phosphorus (n-type) under argon from the AF9 cold box. `gtceu:silicon_boule` = p-type CZ boule, `gtceu:phosphorus_boule` = n-type.

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
| Novolac `(C7H6O)n` + DNQ `C10H6N2O` in xylene | i-line positive resist (g/i-line only, bleaches too weakly below ~300 nm) | `novolac_resin` + `diazonaphthoquinone_dust` → `photoresist` (i-line Photoresist) |
| KrF CAR: poly(4-hydroxystyrene) (PHOST, partly protected) + PAG in PGMEA | 248 nm chemically amplified resist | `polyhydroxystyrene` + `triphenylsulfonium_triflate` + `propylene_glycol_methyl_ether_acetate` → `krf_photoresist` |
| ArF CAR: methacrylate copolymer (acid-labile esters; no aromatics, they absorb 193 nm) + PAG in PGMEA | 193 nm chemically amplified resist | `methyl_methacrylate` → `methacrylate_resin` + PAG + PGMEA → `arf_photoresist` |
| PAG triphenylsulfonium triflate `(C6H5)3S+ CF3SO3-` | photoacid generator (photon → triflic acid, PEB amplifies) | `trifluoromethanesulfonic_acid` → `triphenylsulfonium_triflate` |
| Excimer premix: ~1 % Kr or Ar + ~0.1 % F2 in Ne | KrF / ArF laser gas | `krf_excimer_gas` / `arf_excimer_gas` (AF9: 5 % + 1 %, §6.14) |
| Ultrapure water 18.2 MΩ·cm, degassed | immersion film (n = 1.44 at 193 nm) | `ultrapure_water` (LUV) |
| TMAH `(CH3)4NOH` 2.38% / 0.26N | developer (K+/Na+-free) | `tmah_developer` |
| CMP slurry colloidal `SiO2`/`CeO2` | planarization | `lubricant` (cutter) + `distilled_water` (rinse) |
| Clean dry air: catalytic oxidizer (hopcalite / Pt) → CO2 scrubber → 13X molecular sieve → cryogenic cold box → membrane filter | purge | `oxidized_air` → `decarbonated_air` → `dry_air` → `cryogenic_supercooled_air` → `extreme_clean_dry_air` (§6.5) |
| `HfCl4` + `H2O` ALD → `HfO2` high-k gate (45 nm and below) | gate dielectric | `hafnium_tetrachloride` (LUV only, from zircon, §6.9) |

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

Minecraft map (AF9, one recipe = one full cycle): `hmds_vapor` prime → resist coat (the mode's resist) → soft bake (cupronickel coil) → expose (light source + reticle + lens; excimer modes burn laser gas, LUV through `ultrapure_water`) → PEB (where a CAR's acid amplifies) → `tmah_developer` develop → `distilled_water` rinse → hard bake. All fluids in one recipe (5 for MUV, 6 for HUV-XUV, 8 for LUV), 900t fixed.

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

AF9 nodes (350/250/200/100/50nm) are 1990s-2000s real nodes (350nm ≈ Pentium II era) — deliberately pre-EUV. Names MUV/HUV/EUV/XUV/LUV are AF9-internal (real EUV is 13.5nm, not 200nm — see §10). The light sources are the real ones for those nodes: 350 nm = mercury-lamp i-line steppers, 250 nm (1997) and 180-200 nm = KrF 248 nm (sub-wavelength from 180 nm, hence higher NA + OPC), 90-100 nm = ArF 193 nm dry, 45-50 nm = ArF immersion (NA 1.2-1.35). Rayleigh `CD = k1·λ/NA` keeps every AF9 mode at k1 ≥ 0.35 (single exposure limit 0.25).

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
Chip wafers + chips (all made in the AF9 line now, see §5.5): `ilc`, `ram`, `cpu`, `ulpic`, `lpic`, `simple_soc`, `nand_memory`, `nor_memory`, `mpic`, `soc`, `advanced_soc`, `highly_advanced_soc`, `nano_cpu`, `qbit_cpu`, `hpic`, `uhpic` (`<x>_wafer` + chip; chips without `_chip`: `simple_soc`, `soc`, `advanced_soc`, `highly_advanced_soc`).
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
- Pack config: `enableCleanroom:true`, `cleanMultiblocks:false`. Do not disable — cutting most chips (§5.5) and most HV-LuV circuits need it in AF9, and disabling hides real gating.

## 4.5 What base GT does NOT have (why AF9 exists)

- No EUV/UV lithography machine, no `photoresist` chemistry, no `crystallizer` singleblock.
- SoC items DO exist (`simple_soc/soc/advanced_soc/highly_advanced_soc/crystal_soc` + wafers) — the gap is the process, not the item.
- Only `data_module` needs sterile cleanroom — custom sterile wafers must add `STERILE_CLEANROOM` explicitly.
- UNCERTAIN in JEI: exact `multilayer_...` board strings, `forge` vs `c` lens tag, whether JEI shows water/lubricant boule-cutting variants (7.x source shows none — AF9 cutter adds them explicitly).

---

# 5. AF9 Photolithography Line — current implementation

## 5.1 Machine + recipe types

| Thing | ID | Display name |
|---|---|---|
| Controller | `gtceu:photolithography_line` | Photolithography Line |
| Type MUV | `gtceu:lithography_muv` | Lithography MUV (350 nm) |
| Type HUV | `gtceu:lithography_huv` | Lithography HUV (250 nm) |
| Type EUV | `gtceu:lithography_euv` | Lithography EUV (200 nm) |
| Type XUV | `gtceu:lithography_xuv` | Lithography XUV (100 nm) |
| Type LUV | `gtceu:lithography_luv` | Lithography LUV (50 nm) |

Controller tooltip lines 0-14 (`kubejs/assets/gtceu/lang/en_us.json`, `af9.photolithography_line.tooltip.N`, short on purpose so the tooltip stays narrow): what it prints, the process chain, no-cleanroom, the five modes with substrates, light source per mode (2 lines), laser gas + matching resist, scaling per mode, substrate yield, power rule, hatches, HV-LuV chip rule, perfect OC, LUV high-k (HfCl4).

Java (AF9 Core, `af9-core/`): `PhotolithographyLineMachine` (`LITHO_GATE` power gate, console UI, mode switching, counters), `LithoConsoleWidget` (the console), `LithoRecipeLogic` (per-mode counters), `LithoMode` (numbers that must match KubeJS: node, tier, substrate, light, wavelength, NA, resist), `AF9Client` (texture predicate + tooltip), `AF9EmiPlugin` (wafer variants in EMI). Modifiers: `LITHO_GATE + OC_PERFECT`. Appearance `CASING_STAINLESS_CLEAN` + `gcym/large_engraving_laser` overlay.

EMI recipe info per mode: one line, `Node: <nm>nm, <light>` e.g. `Node: 250nm, KrF 248nm` (lang `af9.recipe.litho_node` + `af9.litho.light.<light>`). Substrate, scaling, NA/k1 and hatches are on the controller console and tooltip, not in EMI.

## 5.2 Structure — 3×3×20, back (lamp) → front (controller)

Unchanged. Each aisle = bottom → middle → top rows:

```text
Stepper:  CCC/CLC/CCC lamp | CCC/CRC/CCC reticle stage | CCC/WTW/CCC lens x3 | CRC/WRW/CCC wafer stage
Track:    CCC/CRC/CCC interface | CCC/CRC/CFC robot | CCC/CHC/CFC hard bake | CSC/WXW/CPC rinse | CSC/WXW/CPC develop
          CCC/CKC/CFC chill | CCC/CHC/CFC PEB | CCC/CHC/CFC soft bake | CSC/WXW/CPC coat x2 | CCC/CKC/CFC chill
          CPC/WHW/CFC HMDS prime | CCC/CRC/CFC robot | III/IMI/CCC cassette + controller
```

`C` = `clean_machine_casing` min 100 OR exactly 2 `INPUT_ENERGY` OR ≥1 `IMPORT_FLUIDS` OR exactly 1 `MAINTENANCE`. `I` = casing OR 1-2 item import + 1-2 export. `F` filter_casing, `R` stainless gearbox, `S` steel gearbox, `X` inert casing, `P` ptfe pipe casing, `H` cupronickel coil, `K` frostproof casing, `T` tempered glass, `W` cleanroom glass, `L` purple lamp, `M` controller.

## 5.3 Five UV modes (load-bearing numbers)

`EUt = VA[tier] × 4` (exactly 2 energy hatches; they must supply 4A of the mode's tier: two hatches of that tier, or for XUV/LUV two 16A hatches of the tier below, since 4A/16A hatches only exist from EV and two hatches let the line use the next voltage tier). `LITHO_GATE` only checks total EU/t; GT checks the voltage. Duration `900t` for printed wafers. Chemicals `round(base × 1.5^index)`. Perfect overclock with stronger hatches. Each mode prints on its own substrate; better substrates give more printed wafers per run (GT's laser-engraver amounts).

| Mode | Node | Tier | EU/t | Substrate `gtceu:` | Density `(350/node)²` | Die factor `√(350/node)` |
|---|---|---|---|---|---|---|
| `muv` | 350 | MV | 480 | `silicon_wafer` | x1 | x1 |
| `huv` | 250 | HV | 1920 | `phosphorus_wafer` | x1.96 | x1.18 |
| `euv` | 200 | EV | 7680 | `naquadah_wafer` | x3.06 | x1.32 |
| `xuv` | 100 | IV | 30720 | `neutronium_wafer` | x12.25 | x1.87 |
| `luv` | 50 | LuV | 122880 | `neutronium_wafer` | x49 | x2.65 |

Exposure tool per mode: the light source the real node used, the lens NA and the matching resist (`light`, `wavelength`, `na`, `resist`, `laserGas`, `immersion`, `highK` in `AF9_LITHO.modes`; `light`, `wavelengthNm`, `numericalAperture`, `resist` in `LithoMode`). `k1 = node × NA / λ` (Rayleigh) is shown on the console tile tooltips.

| Mode | Light (`light`) | λ nm | NA | k1 | Resist `gtceu:` | Laser gas `gtceu:` | Real counterpart |
|---|---|---|---|---|---|---|---|
| muv | mercury lamp i-line (`i_line`) | 365 | 0.60 | 0.58 | `photoresist` (DNQ-novolac) | — | 350 nm i-line stepper (1995) |
| huv | KrF excimer laser (`krf`) | 248 | 0.60 | 0.60 | `krf_photoresist` | `krf_excimer_gas` | 250 nm, first KrF node (1997) |
| euv | KrF excimer laser (`krf`) | 248 | 0.70 | 0.56 | `krf_photoresist` | `krf_excimer_gas` | 180 nm, sub-wavelength KrF + OPC |
| xuv | ArF excimer laser (`arf`) | 193 | 0.85 | 0.44 | `arf_photoresist` | `arf_excimer_gas` | 90 nm dry ArF scanner |
| luv | ArF + water immersion (`arf_immersion`) | 193 | 1.35 | 0.35 | `arf_photoresist` | `arf_excimer_gas` | 45 nm immersion scanner |

Chemicals per printed-wafer recipe (mB). Track chemicals are `round(base × 1.5^index)`; the resist column is the mode's resist; laser gas is `round(10 × 1.5^index)`:

| Mode | `hmds_vapor` | resist | `tmah_developer` | `distilled_water` | `extreme_clean_dry_air` | laser gas | `ultrapure_water` | `hafnium_tetrachloride` | fluid slots |
|---|---|---|---|---|---|---|---|---|---|
| muv | 40 | 100 `photoresist` | 200 | 1000 | 1000 | — | — | — | 5 |
| huv | 60 | 150 `krf_photoresist` | 300 | 1500 | 1500 | 15 `krf_excimer_gas` | — | — | 6 |
| euv | 90 | 225 `krf_photoresist` | 450 | 2250 | 2250 | 23 `krf_excimer_gas` | — | — | 6 |
| xuv | 135 | 338 `arf_photoresist` | 675 | 3375 | 3375 | 34 `arf_excimer_gas` | — | — | 6 |
| luv | 203 | 506 `arf_photoresist` | 1013 | 5063 | 5063 | 51 `arf_excimer_gas` | 1000 | 100 | 8 |

LUV exposes through **1000 mB `ultrapure_water`** (the immersion film; flat) and grows **100 mB `hafnium_tetrachloride`** (flat, not ×1.5): the HfO2 high-k gate dielectric grown by ALD, which real fabs introduced at 45 nm. Fluid slots per recipe type (`setMaxIOSize(2, 1, n, 0)`, `fluidInputs` in the startup script): muv 5, huv/euv/xuv 6, luv 8. Derived wafers (nano/qbit CPU, HPIC, UHPIC) take only their own fluid, no resist, gas or water.

## 5.4 Items

| ID | Display name | Notes |
|---|---|---|
| `kubejs:photomask_blank` | Chrome-on-Quartz Photomask Blank | assembler: quartzite + chromium plate + 100 mB photoresist |
| `kubejs:<chip>_reticle` | `<Chip> Reticle` | one per printed chip (12): ilc, ram, cpu, ulpic, lpic, simple_soc, nand, nor, mpic, soc, advanced_soc, highly_advanced_soc. Stack 1, `notConsumable`. |
| `kubejs:molecular_sieve` | Molecular Sieve 13X | XCDA dryer adsorbent, consumed per dry step; autoclave: 4 zeolite + bentonite + 500 mB distilled water → 4 |
| `kubejs:saturated_molecular_sieve` | Saturated Molecular Sieve | output of the dry step; smelt (any furnace) → `molecular_sieve` |

Materials (`gtceu:`, startup `photolithography.js`): `oxidized_air`, `decarbonated_air`, `dry_air` (gases), `cryogenic_supercooled_air` (gas, 95 K = cryogenic, needs cryo-proof pipes like liquid air), `extreme_clean_dry_air` (gas), `hopcalite` (dust, CuMn2O4), `trimethylchlorosilane`, `hexamethyldisilazane`, `hmds_vapor` (gas), `novolac_resin`, `diazonaphthoquinone` (dust), `photoresist` (display "i-line Photoresist"), `tetramethylammonium_chloride` (dust), `tmah_developer`; laser modes: `krf_excimer_gas`, `arf_excimer_gas` (gases), `trifluoromethanesulfonic_acid` ("Triflic Acid"), `triphenylsulfonium_triflate` (dust, the PAG), `polyhydroxystyrene` (dust), `methyl_methacrylate`, `methacrylate_resin` (dust), `propylene_glycol_methyl_ether`, `propylene_glycol_methyl_ether_acetate` ("PGMEA"), `krf_photoresist`, `arf_photoresist`, `ultrapure_water`. GT's own `neon`, `krypton`, `argon`, `fluorine`, `hydrogen_cyanide` are used as they are. Metallurgy materials: §6.9. Everything else (72 materials: chlorosilanes, fluorochemicals, air-gas streams, resist intermediates, catalysts) is in `fab_chemistry.js`, see §6.11-6.16.

## 5.5 Chips — every GT chip wafer (source of truth: `AF9_LITHO` in `server_scripts/mods/gtceu/photolithography.js`)

Printed wafers per run (from 1 substrate wafer):

| chip | wafer `gtceu:` | chip `gtceu:` | source | first mode | muv | huv | euv | xuv | luv |
|---|---|---|---|---|---|---|---|---|---|
| ilc | `ilc_wafer` | `ilc_chip` | red reticle | muv | 1 wafer | 4 wafers | 8 wafers | 16 wafers | 16 wafers |
| ram | `ram_wafer` | `ram_chip` | green reticle | muv | 1 wafer | 4 wafers | 8 wafers | 16 wafers | 16 wafers |
| cpu | `cpu_wafer` | `cpu_chip` | light_blue reticle | muv | 1 wafer | 4 wafers | 8 wafers | 16 wafers | 16 wafers |
| ulpic | `ulpic_wafer` | `ulpic_chip` | blue reticle | muv | 1 wafer | 4 wafers | 8 wafers | 16 wafers | 16 wafers |
| lpic | `lpic_wafer` | `lpic_chip` | orange reticle | muv | 1 wafer | 4 wafers | 8 wafers | 16 wafers | 16 wafers |
| simple_soc | `simple_soc_wafer` | `simple_soc` | cyan reticle | muv | 1 wafer | 4 wafers | 8 wafers | 16 wafers | 16 wafers |
| nand | `nand_memory_wafer` | `nand_memory_chip` | gray reticle | huv | — | 1 wafer | 4 wafers | 8 wafers | 8 wafers |
| nor | `nor_memory_wafer` | `nor_memory_chip` | pink reticle | huv | — | 1 wafer | 4 wafers | 8 wafers | 8 wafers |
| mpic | `mpic_wafer` | `mpic_chip` | brown reticle | huv | — | 1 wafer | 4 wafers | 8 wafers | 8 wafers |
| soc | `soc_wafer` | `soc` | yellow reticle | huv | — | 1 wafer | 4 wafers | 8 wafers | 8 wafers |
| advanced_soc | `advanced_soc_wafer` | `advanced_soc` | purple reticle | euv | — | — | 1 wafer | 2 wafers | 2 wafers |
| highly_advanced_soc | `highly_advanced_soc_wafer` | `highly_advanced_soc` | black reticle | xuv | — | — | — | 1 wafer | 1 wafer |
| nano_cpu | `nano_cpu_wafer` | `nano_cpu_chip` | derived | euv | — | — | 1 wafer | 1 wafer | 1 wafer |
| qbit_cpu | `qbit_cpu_wafer` | `qbit_cpu_chip` | derived | euv | — | — | 1 wafer | 1 wafer | 1 wafer |
| hpic | `hpic_wafer` | `hpic_chip` | derived | xuv | — | — | — | 1 wafer | 1 wafer |
| uhpic | `uhpic_wafer` | `uhpic_chip` | derived | luv | — | — | — | — | 1 wafer |

Dies per wafer (plain = GT count):

| chip | plain | muv | huv | euv | xuv | luv | cutEUt | cleanroom |
|---|---|---|---|---|---|---|---|---|
| ilc | 8 | 8 | 9 | 11 | 15 | 21 | 64 | no |
| ram | 32 | 32 | 38 | 42 | 60 | 85 | 96 | no |
| cpu | 8 | 8 | 9 | 11 | 15 | 21 | 120 | no |
| ulpic | 6 | 6 | 7 | 8 | 11 | 16 | 120 | no |
| lpic | 4 | 4 | 5 | 5 | 7 | 11 | 480 | yes |
| simple_soc | 6 | 6 | 7 | 8 | 11 | 16 | 64 | no |
| nand | 32 | — | 38 | 42 | 60 | 85 | 192 | yes |
| nor | 16 | — | 19 | 21 | 30 | 42 | 192 | yes |
| mpic | 4 | — | 5 | 5 | 7 | 11 | 1920 | yes |
| soc | 6 | — | 7 | 8 | 11 | 16 | 480 | yes |
| advanced_soc | 6 | — | — | 8 | 11 | 16 | 1920 | yes |
| highly_advanced_soc | 6 | — | — | — | 11 | 16 | 7680 | yes |
| nano_cpu | 8 | — | — | 11 | 15 | 21 | 480 | yes |
| qbit_cpu | 4 | — | — | 5 | 7 | 11 | 1920 | yes |
| hpic | 2 | — | — | — | 4 | 5 | 7680 | yes |
| uhpic | 2 | — | — | — | — | 5 | 30720 | yes |

Transistors per die (NBT `Transistors`):

| chip | base (350 nm) | muv | huv | euv | xuv | luv |
|---|---|---|---|---|---|---|
| ilc | 50,000 | 50,000 | 98,000 | 153,125 | 612,500 | 2,450,000 |
| ram | 16,000,000 | 16,000,000 | 31,360,000 | 49,000,000 | 196,000,000 | 784,000,000 |
| cpu | 5,500,000 | 5,500,000 | 10,780,000 | 16,843,750 | 67,375,000 | 269,500,000 |
| ulpic | 2,000 | 2,000 | 3,920 | 6,125 | 24,500 | 98,000 |
| lpic | 5,000 | 5,000 | 9,800 | 15,313 | 61,250 | 245,000 |
| simple_soc | 1,000,000 | 1,000,000 | 1,960,000 | 3,062,500 | 12,250,000 | 49,000,000 |
| nand | 32,000,000 | — | 62,720,000 | 98,000,000 | 392,000,000 | 1,568,000,000 |
| nor | 16,000,000 | — | 31,360,000 | 49,000,000 | 196,000,000 | 784,000,000 |
| mpic | 20,000 | — | 39,200 | 61,250 | 245,000 | 980,000 |
| soc | 8,000,000 | — | 15,680,000 | 24,500,000 | 98,000,000 | 392,000,000 |
| advanced_soc | 20,000,000 | — | — | 61,250,000 | 245,000,000 | 980,000,000 |
| highly_advanced_soc | 40,000,000 | — | — | — | 490,000,000 | 1,960,000,000 |
| nano_cpu | 12,000,000 | — | — | 36,750,000 | 147,000,000 | 588,000,000 |
| qbit_cpu | 25,000,000 | — | — | 76,562,500 | 306,250,000 | 1,225,000,000 |
| hpic | 50,000 | — | — | — | 612,500 | 2,450,000 |
| uhpic | 100,000 | — | — | — | — | 4,900,000 |

Derived wafers (made in the line from a printed wafer of the same mode; exact-NBT input, 1 wafer out, no reticle):

| Wafer | From | Extra inputs | Duration | Modes |
|---|---|---|---|---|
| `nano_cpu_wafer` | `cpu_wafer` | 16x `carbon_fibers` + 576 mB `glowstone` | 1200t | euv, xuv, luv |
| `qbit_cpu_wafer` | `nano_cpu_wafer` | 2x `quantum_eye` + 288 mB `gallium_arsenide` (or `_radon`: 1x `indium_gallium_phosphide_dust` + 50 mB `radon`, 1200t) | 900t | euv, xuv, luv |
| `hpic_wafer` | `mpic_wafer` | 2x `indium_gallium_phosphide_dust` + 288 mB `vanadium_gallium` | 1200t | xuv, luv |
| `uhpic_wafer` | `hpic_wafer` | 8x `indium_gallium_phosphide_dust` + 576 mB `naquadah` | 1200t | luv |

> TRAPS: chip IDs without `_chip`: `simple_soc`, `soc`, `advanced_soc`, `highly_advanced_soc`. PIC is `mpic_wafer`/`mpic_chip` but GT's recipe names say `pic` (`engrave_pic_*`, `cut_pic`). GT engraving prefixes: `engrave_ssoc`, `engrave_asoc`, `engrave_hasoc`.

## 5.6 NBT

`{AF9Litho:{Node:<350|250|200|100|50>,Transistors:<int>}}`, `Transistors = round(base × (350/node)²)` (base per die at 350 nm, table above; all values fit an int). Cutting copies the tag to every die. Model predicate `af9:litho_mode` = 0 plain, 1 MUV … 5 LUV picks the per-mode tinted texture (violet, blue, cyan, green, gold) for all 16 wafers and 16 chips. Tooltip: `Printed with Lithography <MODE> (<nm>)` + `Transistors per die: <n>`.

## 5.7 Controller console (custom UI)

`PhotolithographyLineMachine#createUIWidget` replaces GT's text display with `LithoConsoleWidget` (190×125, GT's side tabs for power/mode/parts stay):
- header: title, status word + LED (OFFLINE / IDLE / RUNNING / NO POWER / PAUSED / MAINTENANCE),
- five clickable mode tiles (colour per mode, dimmed when the hatches can't power it; hover shows hatches, substrate, light, NA/k1, resist),
- POWER gauge (available vs needed EU/t), PROGRESS bar, TRACK of 8 stations lighting up with progress,
- OUTPUT (node, light source e.g. `KrF 248nm` / `ArFi 193nm`, density, dies, substrate), PRINTED counters per mode + RESET button.
- mode tile tooltips: mode, power, substrate, light (long name), lens NA + k1, resist (its GT material name).
State is sampled server-side each tick and synced only when it changes.

# 6. Recipe chains

## 6.1 Step 0 — Boule (Czochralski, AF9)

GT's boule recipes (silicon dust + gallium arsenide, nitrogen) are removed. All four boules are pulled from `gtceu:electronic_grade_silicon_dust` (the Siemens line, §6.12) under argon; silicon takes a boron dopant (p-type, the CMOS substrate), phosphorus boules stay n-type. Naquadah and neutronium keep GT's exotic extras. They are pulled in the crystal-growth mode of the SMC thermal furnaces (§11): a single block reaches its tier's coil temperature (MV 1800 K is enough for silicon, LuV 5400 K for naquadah), the neutronium boule (6484 K) needs the SMC Thermal Processing Furnace.

```text
af9:silicon_boule (CZ) 32x electronic_grade_silicon_dust + tiny_boron_dust + 250 argon → silicon_boule | 9000t MV | 1784K
af9:phosphorus_boule (CZ) 64x electronic_grade_silicon_dust + 8x phosphorus_dust + 1000 argon → phosphorus_boule | 12000t HV | 2484K
af9:naquadah_boule (CZ) 144x electronic_grade_silicon_dust + naquadah_ingot + gallium_arsenide_dust + 8000 argon → naquadah_boule | 15000t EV | 5400K
af9:neutronium_boule (CZ) 288x electronic_grade_silicon_dust + 4x neutronium_ingot + 2x gallium_arsenide_dust + 8000 xenon → neutronium_boule | 18000t IV | 6484K
```

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

## 6.4 Step 2b — Reticles (laser_engraver)

```text
af9:<chip>_reticle (gtceu:laser_engraver)
  kubejs:photomask_blank + notConsumable #forge:lenses/<color> → kubejs:<chip>_reticle | 1800t | 120 EU/t (MV)

ilc red, ram green, cpu light_blue, ulpic blue, lpic orange, simple_soc cyan,
nand gray, nor pink, mpic brown, soc yellow, advanced_soc purple, highly_advanced_soc black
```

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

Throughput with one reactor per step: MV ≈ 1000 XCDA per 40 s (the expander is the bottleneck; MUV uses 1000 per 45 s
print), HV ≈ 4000 per 10 s. Liquid air is GT's HV vacuum freezer recipe (4000 air → 4000 liquid air), but the Vacuum Freezer needs EV circuits, so in practice the liquid-air step arrives with HUV chips. Cryogenic Supercooled Air also feeds the air-separation cold box (§6.14).

### Laser gases, resists and immersion water

Moved to the fab chemistry lines: excimer premix §6.14, KrF resist §6.15, ArF resist and ultrapure water §6.16.

### i-line resist, HMDS and developer (MUV)

GT's formaldehyde (methanol + O2 over silver) is an HV recipe, and HV circuits need MUV chips, so the i-line resist used to be unreachable at MV. AF9 adds the Formox process at MV. TMAH is made the electronic-grade way, by membrane electrolysis of the chloride (a KOH metathesis would leave potassium in the developer, which shifts transistor thresholds); the chloride is dissolved first because GT's circuit-less distilled-water electrolysis would otherwise take the cell.

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

## 6.6 Step 3 — Print wafers (Photolithography Line)

Recipe IDs `af9:<chip>_wafer_<mode>` on `gtceu:lithography_<mode>`, for every printed chip in every mode from its first mode on (63 recipes incl. derived):

```text
af9:ram_wafer_muv (gtceu:lithography_muv)
  gtceu:silicon_wafer + NC kubejs:ram_reticle + 40 hmds_vapor + 100 photoresist + 200 tmah_developer + 1000 distilled_water + 1000 extreme_clean_dry_air
  → gtceu:ram_wafer{AF9Litho:{Node:350,Transistors:16000000}} | 900t | 480 EU/t (4A MV)
af9:ram_wafer_huv (gtceu:lithography_huv)
  gtceu:phosphorus_wafer + NC kubejs:ram_reticle + 60 hmds_vapor + 150 krf_photoresist + 300 tmah_developer + 1500 distilled_water + 1500 extreme_clean_dry_air + 15 krf_excimer_gas
  → 4x gtceu:ram_wafer{AF9Litho:{Node:250,Transistors:31360000}} | 900t | 1920 EU/t (4A HV)
af9:ram_wafer_luv (gtceu:lithography_luv)
  gtceu:neutronium_wafer + NC kubejs:ram_reticle + 203 hmds_vapor + 506 arf_photoresist + 1013 tmah_developer + 5063 distilled_water + 5063 extreme_clean_dry_air + 51 arf_excimer_gas + 1000 ultrapure_water + 100 hafnium_tetrachloride
  → 16x gtceu:ram_wafer{AF9Litho:{Node:50,Transistors:784000000}} | 900t | 122880 EU/t (4A LuV)
af9:nano_cpu_wafer_euv (gtceu:lithography_euv)
  gtceu:cpu_wafer{AF9Litho:{Node:200,Transistors:16843750}} (exact) + 16x carbon_fibers + 576 glowstone
  → gtceu:nano_cpu_wafer{AF9Litho:{Node:200,Transistors:36750000}} | 1200t | 7680 EU/t
```

Removed base: `gtceu:chemical_reactor/` and `gtceu:large_chemical_reactor/` `nano_cpu_wafer`, `qbit_cpu_wafer_quantum_eye`, `qbit_cpu_wafer_radon`, `hpic_wafer`, `uhpic_wafer`.

## 6.7 Step 4 — Dice wafers (cutter, replaces base)

`af9:cut_<chip>_<plain|muv|huv|euv|xuv|luv>[|_distilled_water|_water]` (228 recipes). Input `strongNBT` (plain = no tag). Output = dies from §5.5 with the wafer's tag, split into stacks of ≤64 (2 output slots). Fluids: lubricant `clamp(totalEU/1280,1,250)` 900t, distilled `clamp(totalEU/426,3,750)` 1350t, water `clamp(totalEU/320,4,1000)` 1800t, `totalEU = 900 × cutEUt`. Cleanroom per §5.5. Removed base: `gtceu:cutter/<gtCut>[|_water|_distilled_water]` for all 16 chips.

## 6.8 Step 5 — Circuits

**Rule:** a tier's circuits are built from that tier's own products: chips printed in its lithography mode and the metals the pack's *Circuits* quest page lists for that tier (MV aluminium, HV stainless steel (+ gold, the HV cable), EV titanium + platinum, IV tungstensteel + tungsten, LuV rhodium-plated palladium (+ osmiridium, niobium-titanium = LuV cable)). Every metal part is makeable with the previous tier's machines, like GT's own tier materials.

**Bootstraps** (a tier's first circuit must not need that tier's machines):

| Tier | Why | Bootstrap |
|---|---|---|
| MV | the line needs MV Energy Hatches, which need a ULPIC chip | GT's `gtceu:laser_engraver/engrave_ulpic_silicon` is kept (plain ULPIC wafer, no mode); Good Electronic Circuit stays chip-free |
| HV | HUV printing needs 1920 EU/t = two HV hatches; the HV hatch is an HV assembler recipe (HV circuits) | `integrated_circuit_hv` (Advanced Integrated Circuit) takes **MUV** ILC + RAM chips |
| EV | EUV needs two EV hatches; EV hatch = EV assembler | `workstation_ev` takes **HUV** RAM chips and plain `fine_platinum_wire` (iridium is EV-era) |
| IV | IV hatch needs HPIC (XUV) | none: XUV (30720 EU/t) runs on two **EV 16A** hatches (65536 EU/t, two hatches allow IV voltage) |
| LuV | LuV hatch needs UHPIC (LUV) | none: LUV (122880 EU/t) runs on two **IV 16A** hatches |

MV and lower (`mv_circuits.js` + base GT): any chip works (plain ingredient, NBT ignored).
- Good Electronic Circuit: phenolic board + 2 basic electronic + 2 vacuum tubes + 2 copper wire (bootstrap: no chips).
- Good Integrated Circuit: phenolic board + 2 basic integrated + 2 ILC chips + 2 resistors + 4 **Aluminium-Silicon fine wire** + 4 **Kovar bolts** → 2.
- Microprocessor: plastic board + CPU chip + RAM chip + 4 resistors + 4 capacitors + 4 **Aluminium-Silicon fine wire** → 2.

HV to LuV (`tiered_circuits.js`): GT's own recipes with exact-NBT chips of the tier's mode — HV = HUV, EV = EUV, IV = XUV, LuV = LUV; a chip GT only makes later uses its first mode (Nano CPU and ASoC are EUV even in HV circuits, HASoC at least XUV). Each has a tin (144 × solder) and soldering alloy (72 × solder) version (`af9:<gt_id>` / `af9:<gt_id>_soldering_alloy`):

| Tier | GT recipe IDs replaced (`gtceu:circuit_assembler/…` + `_soldering_alloy`) | Tagged chips | Metals (replacing GT's) |
|---|---|---|---|
| HV | `integrated_circuit_hv` (bootstrap), `processor_assembly_hv`, `nano_processor_hv(_asmd)`, `nano_processor_hv_soc` | ilc, ram (MUV in the bootstrap, else HUV); nano_cpu, advanced_soc (EUV) | `fine_gold_wire` (for electrum / red alloy), `stainless_steel_bolt` (for annealed copper / platinum bolts) |
| EV | `workstation_ev` (bootstrap), `nano_processor_assembly_ev(_asmd)`, `quantum_processor_ev(_asmd)`, `quantum_processor_ev_soc` | ram (HUV in the bootstrap, else EUV), qbit_cpu, nano_cpu, advanced_soc (EUV) | `fine_platinum_iridium_wire` (bootstrap: `fine_platinum_wire`), `titanium_bolt` (for blue alloy / NbTi bolts) |
| IV | `mainframe_iv(_asmd)`, `nano_computer_iv(_asmd)`, `quantum_assembly_iv(_asmd)`, `crystal_processor_iv` | ram, nor, nano_cpu (XUV) | `fine_tungsten_steel_wire` (for electrum / platinum / NbTi), `tungsten_steel_frame` (for aluminium frames), `tungsten_single_wire` (for annealed copper) |
| LuV | `quantum_computer_luv(_asmd)`, `crystal_assembly_luv`, `wetware_processor_luv`, `wetware_processor_luv_soc` | ram, nor, nano_cpu, highly_advanced_soc (LUV) | `fine_osmiridium_wire` (for platinum), `fine_niobium_titanium_wire` (kept / for YBCO), `rhodium_plated_palladium_bolt` (for naquadah) |

**LuV Nano Mainframe:** the newer ATM9 script `kubejs/server_scripts/circuits_for_atm.js` removes GT's circuit-assembler mainframe and makes it on the Assembly Line (`circuit_1_iv_luv`). AF9 adds 16 LUV RAM chips there (`AF9_LITHO.tagged('ram', 'luv', 16)`) and does not re-add the circuit-assembler version. The same ATM9 file also moves the ZPM quantum mainframe and the UV/UHV mainframes (not AF9 scope).

Not changed: SMD parts, boards, crystal CPU parts, ZPM+ circuits, non-circuit uses of chips (energy hatches, batteries…).

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

MgCl2 goes back to Mg + Cl2 in GT's electrolyzer. Real zircon holds ~2 % hafnium; AF9 gives 10 % so LUV has enough (100 mB HfCl4 per LUV print run = one column batch = 6 zircon dust). Zircon, zirconia and the chlorides have formulas but no components, so GT adds no electrolyzer shortcut.

## 6.10 Quests (FTB Quests)

Changed chapters (`config/ftbquests/quests/chapters/`): `medium_voltage`, `high_voltage`, `extreme_voltage`, `insane_voltage`, `ludicrous_voltage`, `zero_point_module`, `ultra_high_voltage`, `circuits`. Text lives in `kubejs/assets/kubejs/lang/en_us.json` under `af9.quest.*` (113 keys). Changed quests point at these new keys, so other languages fall back to the new English text instead of the old laser-engraving text.

| Chapter | New quests (ID) | Changed quests |
|---|---|---|
| MV | Photolithography Line `AD8FD756AA394586`, Lithography Chemistry `240C1BFF666269E2` (fluid tasks), Reticles `412010740F2B823B`, Extreme Clean Dry Air `F01C688974686C8E`, MV Circuit Metals `5CF8D8E0B8E0D908`, Hydrogen Fluoride `97A469B25693E8F2`, Electronic-Grade Silicon `F3C933DD2ADF7BB7` (MG-Si, EG-TCS, polysilicon, EGS), Air Separation `75865FF47260C1E7` (crude argon, argon) — the last three in a column at x = 13, The Fab Plant `AE2076353F8220B1` (the four MV SMC single blocks; HF, XCDA and Lithography Chemistry need it), Thermal Processing Furnace `01B10745D9CCECA1` (§11) | Silicon Boule `26D1F1ECF66194E6` (needs EGS + Air Separation, AF9 text line), MV Laser Engraver (reticles + ULPIC bootstrap), ILC/RAM wafer (line + reticle deps), ULPIC wafer (bootstrap), Advanced Integrated Circuit (HV bootstrap) |
| HV | HUV Lithography `F5459779599BE107` (NBT task: HUV RAM wafer; needs the two below), Excimer Lasers `72569D77E20C8733` (krypton + KrF gas; needs Air Separation + Fluorine), Chemically Amplified Resist `A9D8D2084CFA2A1F` (PAG + t-BOC PHOST + KrF resist; needs Fluorine + Building Blocks), Fluorine and Triflic Acid `885201A0FC63A534`, Resist Building Blocks `9EF2C24FF660CA97` (AIBN, PGMEA, tributylamine), HV Circuit Metals `51D7534BBA00287A`, SMC Large Chemical Reactor `DA794521237FF495`, Rectification Column `ED124BD4285F0055`, Membrane Cell Hall `5D823D017C7CE01F` (§11, optional) | CPU chip (+ CPU reticle task), Simple SoC wafer (+ reticle task), Microprocessor, Processor Assembly, Workstation |
| EV | EUV Lithography `418DC14B94C9272A`, EV Circuit Metals `2EC82E995F48F0D6` | LPIC (+ reticle task), HV Energy Hatch, Workstation |
| IV | XUV Lithography `46CAB00FF64D12DE` (needs ArF chemistry), ArF Lithography Chemistry `1DB07C3B15CDDAAC` (ArF gas + methacrylate resin + ArF resist), IV Circuit Metals `30C5810673F9AFE6` | SoC / PIC / NOR / NAND (lens or engraver task → reticle), HPIC, Nano CPU wafer, phosphorus wafers, IV Energy Hatch (EV 16A path), IV mainframe, Nanoprocessor |
| LuV | Zircon and Hafnium `84516B9D84575F62` (fluid task HfCl4), Immersion Lithography `C786A524FDA78C1A` (fluid task ultrapure water), LUV Lithography `106D52BD58E747F8` (needs both) | Qubit wafers, LuV Energy Hatch (IV 16A path), Large Engraving Laser, Nano Mainframe, Quantum Computer |
| ZPM / UHV / Circuits | — | UHPIC wafer, naquadah boule, HASoC (lens → reticle task), circuits page intro |

Mode quests use exact-NBT item tasks (`match_nbt: true`, e.g. `{AF9Litho:{Node:250,Transistors:31360000}}` on `gtceu:ram_wafer`). New quests reward 100 XP. IDs were generated unique across all quest files; positions were checked for overlap. The repo's `circuits.snbt` / `high_voltage.snbt` also carry the newer ATM9 instance's two small fixes (Wetware Mainframe row position, a normalized tool tag).

---

## 6.11 Fab chemistry: gating rules and verification

Files: `startup_scripts/gtceu/fab_chemistry.js` (72 materials, formulas only, so GT generates no electrolyzer/centrifuge shortcut), `server_scripts/mods/gtceu/fab_chemistry.js` (recipes). Five lines, each 10-20 unit operations with real chemistry, inspired by Nomifactory / Cosmic Frontiers style chains.

All of these recipes run only in the SMC fab machines (§11): GT's Chemical Reactor, Large Chemical Reactor, mixer, bath, autoclave, blast furnace, electrolyzer and distillation tower have none of them. Non-thermal recipes from HV power on need a clean room (single blocks: a GT Cleanroom; the fab multiblocks bring their own filter ceiling).

Tier gating (GT 7.2.0 defaults, ATM9 does not change them): EBF needs LV circuits, Pyrolyse Oven MV, Large Chemical Reactor and Cracker HV circuits, **Distillation Tower and Vacuum Freezer EV circuits**; the SMC single blocks need their tier's circuits, the SMC Thermal Processing Furnace MV circuits, the other SMC multiblocks HV circuits. AF9 circuits: HV needs MUV chips, EV needs HUV chips. So:

| Phase | Machines | Must supply |
|---|---|---|
| MV0 (before MUV prints) | LV/MV single blocks incl. the MV SMC single blocks (the Fractionating Still takes column recipes one cut per circuit, EUt/4, twice the time), SMC Thermal Processing Furnace (2 MV hatches reach HV voltage), pyrolyse oven | silicon boule, i-line resist, HMDS, TMAH, XCDA, argon, HF |
| HV0 (MUV runs) | + HV single blocks (in a Cleanroom), SMC LCR, SMC Rectification Column, SMC Membrane Cell Hall, cracker | KrF resist, KrF gas (neon, krypton, fluorine), phosphorus boule, HfCl4 |
| EV0 (HUV runs) | + EV single blocks, Vacuum Freezer | ArF resist, ArF gas, ultrapure water |

Earlier designs broke this: GT's formaldehyde is HV (i-line resist needed it at MV), krypton came from liquid air (Vacuum Freezer = EV circuits = HUV chips = krypton), fluorine had no source at all (AF9's fluorite has no decomposition flag; GT's only fluorine is uranium hexafluoride electrolysis), boron neither (GT 7.2.0 has no borax source). All fixed below.

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
af9:silicon_boule (CZ) 32x electronic_grade_silicon_dust + tiny_boron_dust + 250 argon → silicon_boule | 9000t MV | 1784K
af9:phosphorus_boule (CZ) 64x electronic_grade_silicon_dust + 8x phosphorus_dust + 1000 argon → phosphorus_boule | 12000t HV | 2484K
af9:naquadah_boule (CZ) 144x electronic_grade_silicon_dust + naquadah_ingot + gallium_arsenide_dust + 8000 argon → naquadah_boule | 15000t EV | 5400K
af9:neutronium_boule (CZ) 288x electronic_grade_silicon_dust + 4x neutronium_ingot + 2x gallium_arsenide_dust + 8000 xenon → neutronium_boule | 18000t IV | 6484K
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
[CZ] electronic-grade silicon (+ dopant, argon) → silicon / phosphorus / naquadah / neutronium boule → [CUTTER] → blank substrate wafers
  → [PHOTOLITHOGRAPHY_LINE, mode = substrate + light, + reticle + track fluids (+ mode's resist, laser gas, UPW, HfCl4)] → printed wafer {AF9Litho}
     → [LINE, same mode: + carbon fibres / quantum eye / IGP …] → derived wafer (nano/qbit CPU, HPIC, UHPIC) {AF9Litho}
     → [CUTTER, exact NBT] → chips {AF9Litho}
        → [CIRCUIT_ASSEMBLER] MV and lower: any chip | HV-LuV: chip of the tier's mode (exact NBT)
                              + that tier's metals (MV Al-Si + Kovar, HV gold + stainless, EV Pt-Ir + titanium,
                                IV tungstensteel + tungsten, LuV osmiridium + NbTi + rhodium-plated palladium)
Side chains (fab modes of §11 in brackets, GT machines in capitals): air → [purify hopcalite/Pt] → [purify lime/NaOH] → [purify molecular sieve] → [purify expander / liquid air] → [purify filter] → XCDA;
             zircon ore → [EBF] zirconia → [EBF +C +Cl2] crude ZrCl4 → [column] ZrCl4 + HfCl4 (→ LUV) → [EBF Kroll +Mg] Zr → [EBF] ingot;
             DMDCS+CH3Cl+Mg → TMCS → +NH3 → HMDS → +N2 → hmds_vapor;
             phenol+CH2O → novolac, naphthalene+HNO3+NH3 → DNQ, +xylene → photoresist (i-line, MUV);
             quartzite → [wet HCl] HPQ → [calcination +coke] MG-Si → [synthesis +HCl, Cu] crude chlorosilanes → [column/still] TCS / STC / DCS / BCl3
               → [purify carbon] EG-TCS → [blending +H2] feed → [CVD Siemens] polysilicon + vent gas → [column/still] H2, HCl, TCS, STC (loop)
               → [macerator] → [wet HNA etch] EGS → [CZ + boron (from BCl3) + argon] silicon boule;
             fluorite + H2SO4 → crude HF → [column/still] AHF → KF → KF·2HF → [electrolysis, carbon] crude F2 → [purify NaF] F2;
             CH4+SO3 → MSA → [+SOCl2] MsCl → [+KF] MsF → [+HF, ECF Ni] CF3SO2F → [+KOH] KOTf → [+H2SO4] triflic acid;
             cryogenic supercooled air → [cryo column / still] N2, O2, crude Ar (→ deoxo → Ar), crude Ne (→ Pt/charcoal → [cryo column] Ne + He),
               Kr/Xe concentrate (→ Pt burner → sieve → [cryo column] O2, Kr, Xe) → [blending +F2] KrF / ArF excimer gas;
             phenol → 4-HAP → 4-AAP → [Pd/C] carbinol → 4-acetoxystyrene → [AIBN] PAS → PHOST → [+Boc2O] t-BOC PHOST;
             butene → isobutylene → tBuOH → NaOtBu → [+CO2 +COCl2] Boc2O; MgCl/THF Grignard + Ph2SO → TPS-Cl → [+TfOH] PAG;
             butane → [VPO] maleic anhydride → THF; butyraldehyde → n-butanol → tributylamine; propene + H2O2 [TS-1] → PO → PGME → PGMEA;
             acetone + HCN → ACH → [+H2SO4] methacrylamide sulfate → MMA / MAA → [+isobutylene] tBMA → [AIBN, synthesis] methacrylate resin;
             resin + PAG + PGMEA + tributylamine → [blending] → [purify filter] KrF / ArF resist; distilled water → [purify filter] ultrapure water;
             molybdenite → MoO3 → [+hematite] iron molybdate → Formox formaldehyde (MV) → novolac;
             dimethylamine+CH3Cl → TMACl → [+DW] solution → [electrolysis] tmah_developer;
             quartzite+chromium+resist → photomask blank → laser + lens → reticle
```

# 8. How the chips are used

| Chip | Where (AF9) |
|---|---|
| ilc | LV/MV integrated circuits (any mode); HV Advanced Integrated Circuit (MUV, bootstrap) |
| ram | MV Microprocessor (any); HV Advanced Integrated Circuit (MUV) and EV Workstation (HUV) bootstraps; other HV-LuV processors, assemblies, computers, mainframes (tier mode), incl. ATM9's LuV Nano Mainframe (LUV) |
| cpu | LV/MV microprocessors (any); source for nano_cpu wafers |
| ulpic, lpic, mpic, hpic, uhpic | GT energy parts (hatches, batteries, …): unchanged ingredients, any mode |
| simple_soc, soc | GT NAND chip / SoC processor recipes: unchanged, any mode |
| nand | GT uses (crystal computer ZPM, data sticks …): unchanged |
| nor | IV/LuV nano and quantum computers (tier mode) |
| advanced_soc | HV nano and EV quantum SoC processors (EUV) |
| highly_advanced_soc | LuV wetware SoC processor (LUV) |
| nano_cpu | HV nano processors (EUV), EV quantum (EUV), IV crystal (XUV), LuV wetware (LUV) |
| qbit_cpu | EV quantum processors (EUV) |

# 9. Extension points (not done yet)

1. Transistor system (B): 3×3 transistor breadboard, printed PCBs, x10 transistor demand — design pending.
2. ZPM+ circuits and non-circuit chip uses still accept any mode.
3. Wet etch (HF/BOE) step, sterile cleanroom wafers, crystal chips (autoclave path) — untouched.
4. Quests are updated (§6.10); non-English quest languages show the new English text for changed quests until translated.
5. Hafnium metal: only HfCl4 is used (LUV high-k); `GTMaterials.Hafnium` still has no items. Zirconium ingots have no use yet.
6. The repo still lacks other parts of the newer ATM9 release the test instance runs (only the 4 KubeJS scripts were synced).
7. Masks: one binary chrome reticle per chip serves every mode. Real sub-wavelength modes (EUV 200 nm with KrF, XUV, LUV) need phase-shift masks with OPC; a PSM reticle tier (MoSi on quartz) would be the next realism step.
8. BARC/topcoat coats, multi-patterning and real 13.5 nm EUV (tin-plasma source, reflective optics) are not modelled; LUV at k1 0.35 is still single exposure.
9. ArF resin is the first-generation methacrylate terpolymer. Modern ArF monomers (2-methyl-2-adamantyl methacrylate from dicyclopentadiene → adamantane → adamantanone; α-methacryloyloxy-γ-butyrolactone from 1,4-butanediol) would extend Line 5. The i-line DNQ chain (§6.5) is still the short 3-step version (real PAC: DNQ-5-sulfonyl chloride esterified onto a trihydroxybenzophenone).
10. Pd/C, VPO, TS-1, iron molybdate, acidic resin and the other catalysts are not consumed; catalyst deactivation is not modelled.

---

# 10. Agent validation checklist

- [ ] IDs: controller `gtceu:photolithography_line`; recipe types `gtceu:lithography_<mode>`; recipe IDs `af9:*`. Chip IDs per §5.5 traps.
- [ ] Numbers come from `AF9_LITHO` (KubeJS) and `LithoMode` (Java) — change both together (node, tier, substrate, light, wavelength, NA, resist). Transistor base × 49 must stay < 2,147,483,647.
- [ ] Light/resist: MUV i-line + `photoresist`, HUV/EUV KrF + `krf_photoresist` + `krf_excimer_gas`, XUV ArF + `arf_photoresist` + `arf_excimer_gas`, LUV the same + `ultrapure_water` + `hafnium_tetrachloride`; fluid slots 5/6/6/6/8 (`fluidInputs` in the startup script).
- [ ] Fab chemistry goes on the `fab_*` recipe types only (§11), never on GT's chemical machines; within a fab type no recipe may hold every input of a circuit-less other one. Recipes AF9 still puts on GT machines (alloy mixers, zircon cracking, Kroll) must not hold every input of a circuit-less GT recipe of the same type (e.g. GT ethenone = sulfuric + acetic acid, acetic acid = CO + methanol); give both circuits or change the route.
- [ ] Tier check for consumables (§6.11): MUV inputs at MV0 (single blocks ≤ MV incl. MV SMC single blocks and still cuts, SMC Thermal Processing Furnace, pyrolyse), HUV inputs at HV0 (+ HV single blocks, SMC multiblocks, cracker), no Distillation Tower / Vacuum Freezer before HUV chips. Run the reachability analysis over GT 7.2.0 + AF9 recipes after every chain change.
- [ ] Subset conflicts in both directions within each fab type and, for AF9 recipes on GT machines, against every GT 7.2.0 recipe of the same machine (CR recipes also run in the LCR); circuit-gated recipes only clash with the same circuit. Watch GT's generic ones: distilled-water electrolysis, clay/quartzite autoclave, graphite electrolysis, ethenone (sulfuric + acetic acid), acetic acid (CO + methanol), steel (iron + oxygen).
- [ ] New fab materials: formula only, no components (no GT decomposition shortcut); ID must not exist in GT 7.2.0.
- [ ] Printed wafers: substrate per mode, GT yields (1/4/8/16 silicon class, 1/4/8 phosphorus class, ASoC 1/2, HASoC 1), 900t, `VA[tier]×4`, chemicals `round(base×1.5^i)`.
- [ ] Derived wafers: exact-NBT input of the same mode, only from their first mode on.
- [ ] Cutter: `strongNBT` input, plain variant kept, ≤64 per stack, GT fluid formulas with clamps, cleanroom per table.
- [ ] Circuits: MV and lower untouched by tags; HV-LuV use `AF9_LITHO.tagged(chip, mode, n)` with `max(tier mode, chip first mode)`; bootstraps (HV Advanced IC = MUV, EV Workstation = HUV) stay one mode lower.
- [ ] Metals: a tier's circuits only use that tier's metals (Circuits quest page), each makeable with the previous tier's machines (mixer/EBF voltage one tier lower; two hatches give +1 tier on an EBF).
- [ ] Bootstrap check for any change: a tier-T circuit, energy hatch or the line must never need something only tier-T machines make.
- [ ] Fab recipes (§11): a free fluid input left for the changeover purge; circuits count as item inputs; thermal modes carry `blastFurnaceTemp`, the others none; `cleanroom(CLEANROOM)` exactly on the non-thermal recipes from HV power on; single-block modes of one family share one slot layout; single-block fluid amounts ≤ 16000 mB (MV tanks).
- [ ] Dry run: load the AF9 server scripts (incl. `fab_machines.js`) plus `circuits_for_atm.js` with stubs and check no duplicate IDs, every tagged ingredient has a producer, every AF9 fluid/dust used has a producer, every recipe within its machine's slots (items in incl. NC, items out, fluids in incl. NC/out; LUV 8 fluids), EBF recipes have a temperature, ≤64 per stack.
- [ ] New mixer alloys: circuit number must not collide with a GT mixer recipe whose inputs are a subset (invar, cupronickel use circuit 1).
- [ ] New materials need a `material.gtceu.<id>` line in `kubejs/assets/gtceu/lang/en_us.json`; new KubeJS items need a texture in `kubejs/assets/kubejs/textures/item/`.
- [ ] Energy hatches exactly 2, `LITHO_GATE + OC_PERFECT`; no cleanroom for the line itself.
- [ ] Realism wording: AF9 node names are gameplay labels (real EUV is 13.5 nm).

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

The multiblock tier is the circuit tier of its crafting recipe. The thermal multiblock is MV because the MV silicon line (MG-Si, CZ boules) needs it or the MV single furnace.

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
| `fab_crystal_growth` | 3/2/2/2 | 4 | silicon / phosphorus / naquadah / neutronium boules |

Clean room: every non-thermal recipe from HV power on has `cleanroom(CLEANROOM)` (60 recipes). Single blocks need a GT Cleanroom for them; the four multiblocks with filter casings bring their own. Thermal recipes carry `blastFurnaceTemp` and never need a clean room.

## 11.3 Structures

```text
SMC Large Chemical Reactor, 5 wide × 5 deep × 4 high (aisles back → front, rows bottom → top)
  XXXXX XGGGX XGGGX XXXXX     X inert PTFE casing or hatches (≥ 30 casings)
  XXXXX GKCKG GKKKG XFFFX     K inert PTFE casing (the vessel)
  XXXXX GCPCG GKPKG XFFFX     C exactly one heating coil, the rest inert casing (jacket)
  XXXXX GKCKG GKKKG XFFFX     P PTFE pipe casing (stirrer / dip pipe)
  XXXXX XGSGX XGGGX XXXXX     G cleanroom glass, F filter casings (one type), S controller
SMC Rectification Column, 3 × 3, 3-10 high (layers bottom → top)
  XSX/XKX/XXX sump + cold box (K frostproof casing) | XXX/XPX/XXX × 1-8 trays (P PTFE pipe casing) | FFF/FFF/FFF filter hood
  X clean stainless casing or hatches
SMC Membrane Cell Hall, 3 wide × 4 high × 3-10 deep (aisles back → front)
  back plate XXX/XXX/XXX/FFF | cells XXX/EPE/XXX/FFF × 1-8 (E titanium frame electrodes, P PTFE pipe casing membrane) | front XXX/XSX/XXX/FFF
  X stable titanium casing or hatches
SMC Thermal Processing Furnace, EBF shape without muffler
  XXX/CCC/CCC/XXX | XXX/C#C/C#C/XXX | XSX/CCC/CCC/XXX     C heating coils (one type), # air, X heatproof casing or hatches
```

Hatches on any X: GT's auto abilities for the modes (item/fluid in/out, 1-2 energy), one maintenance hatch, optionally one parallel hatch and one laser hatch.

## 11.4 Behaviour (AF9 Core)

- Changeover purge (`FabModifiers.PURGE`): when a machine starts a recipe other than the last one it finished (recipe ID), that first run also needs the family's purge fluid and takes the purge time on top (not overclocked or multiplied, applied after all other modifiers). The machine then keeps running the same recipe without purge. Counted on the console.
- Built-in clean room (`FabMultiblockMachine`): the filter casings' type decides it: filter casing = ISO 5 (`CLEANROOM`), sterilizing filter casing = ISO 3 (both types). It is a GT `DummyCleanroom`, not a provider block, so a fab multiblock can still stand inside a GT Cleanroom (the thermal furnace has no filters and uses the surrounding one if any; its recipes need none).
- Parallels: PTFE pipe casings are trays (column) and membranes (cell hall), each one a parallel (`STRUCTURE_PARALLEL`, limited by inputs, output space and energy like GT's parallel hatch); the SMC LCR gets 2 with sterile filters. A parallel hatch multiplies on top.
- Overclocks: SMC LCR perfect (like GT's LCR) with the coil discount (1/20 EU/t per coil tier above cupronickel, at most half); column and cell hall non-perfect; thermal furnace GT's EBF rules (coil temperature + 100 K per energy tier above MV, heat discounts and perfect overclocks per 1800 K surplus); all multiblocks batch mode. Single blocks non-perfect.
- Single-block furnaces reach the temperature of their tier's coil: MV 1800 K, HV 2700 K, EV 3600 K, IV 4500 K, LuV 5400 K (`TIER_TEMPERATURE`); the neutronium boule (6484 K) needs the multiblock. EMI shows temperature, coil and the smallest single block on every thermal recipe (`FabRecipeInfo`).

## 11.5 Consoles

Multiblocks replace GT's text display with the fab console (190 × 125): header with family and status LED (offline, idle, running, no power, paused, maintenance, purging), one tile per mode with its run counter (click to switch; tooltip lists the mode's process steps), power bar (needed vs available EU/t), progress with the purge share of the run, plant panel (clean-room class, parallels, heat, the family's purge), and the process-step track of the active mode (purge cell first on a changeover). Clicking the changeover counter resets the counters. Single blocks keep GT's slot page for the active mode with a console strip above it (mode, steps, progress, clean class, heat, purge).

Process steps per mode: synthesis CHRG HEAT REAC QNCH SEPR DSCH; blending DOSE MIX DGAS DSCH; wet LOAD SOAK RNSE DRY; purification FEED ADSB FILT PROD; distillation FEED BOIL RFLX DRAW; cryogenic COOL LIQF RECT DRAW; fractionation FEED BOIL CNDS DRAW; electrolysis FILL POL ELEC STRP; electrofluorination FILL POL FLUR STRP; calcination LOAD RAMP SOAK COOL; CVD PURG RAMP DEPO COOL; crystal growth MELT DIP NECK BODY TAIL COOL.

## 11.6 Crafting (`af9:` shaped)

```text
<tier>_smc_chemical_reactor      FPF / UXU / CPC   X <tier>_chemical_reactor, U <tier> pump
<tier>_smc_fractionating_still   FPF / UXU / CPC   X <tier>_distillery, U <tier> pump
<tier>_smc_electrolytic_cell     FPF / WXW / CPC   X <tier>_electrolyzer, W <tier> single cable (Cu, Au, Al, Pt, NbTi)
<tier>_smc_thermal_furnace       FPF / WXW / CWC   X <tier>_arc_furnace, W <tier> heating wire, double (cupronickel … HSS-G)
  F fluid filter, P PTFE fluid pipe, C circuit of the tier
smc_large_chemical_reactor       CRC / PMP / FXF   X GT Large Chemical Reactor + everything its recipe takes (HV circuits, stainless rotor, large PTFE pipes, HV motor), F filter casings
smc_rectification_column         CPC / FHF / UPU   HV hull, HV pumps, large PTFE pipes, filter casings, HV circuits
smc_membrane_cell_hall           CWC / EHE / FPF   HV electrolyzers, gold quadruple cable, HV hull, filter casings, large PTFE pipe, HV circuits
smc_thermal_processing_furnace   CKC / PXP / WCW   X Electric Blast Furnace, K cupronickel coil block, PTFE pipes, copper cable, MV circuits
```

## 11.7 Quests

MV chapter: "The Fab Plant" (the four MV single blocks) before HF, XCDA and the MUV chemistry quests; "Thermal Processing Furnace" (optional). HV chapter: "SMC Large Chemical Reactor", "Rectification Column", "Membrane Cell Hall" (optional, after the fab plant). Texts `af9.quest.*.fabPlant|fabFurnace|smcLcr|smcColumn|smcCells`.

## 11.8 Verification (re-run on every change)

Dry run of the startup and server scripts with stubs (slots incl. circuits and purge headroom, temperatures, clean-room rule, single-block tank sizes, one layout per single block), subset conflicts within every fab type (0), tier reachability with the SMC machines (MUV inputs MV0, KrF HV0, ArF / ultrapure water EV0, HfCl4 HV0), Java checked against the GT 7.2.0 and LDLib sources plus a `javac` syntax pass (no Gradle build is possible offline: the GT/Forge Maven repositories are not reachable).

---

# Appendix A. File map

```text
af9-core/ (Forge mod `af9`, GTCEu 7.2.0 addon; build: gradlew build, jar → mods/)
  litho/LithoMode.java                 # 5 modes, substrates, light sources (λ, NA, k1), resists, colours, WAFERS/CHIPS lists, NBT tag names
  machine/PhotolithographyLineMachine  # LITHO_GATE, console UI, mode switching, counters, EMI info
  machine/LithoConsoleWidget           # the console (drawing + server→client sync)
  machine/LithoRecipeLogic             # per-mode counters
  client/AF9Client                     # af9:litho_mode predicate + wafer/chip tooltip
  compat/emi/AF9EmiPlugin              # each mode's wafer as its own EMI entry
  fab/FabFamily, IFabMachine           # §11: the four families (purge fluid/time, console colour, process steps per mode)
  fab/FabModifiers, FabRecipeLogic     # PURGE, STRUCTURE_PARALLEL, COIL_DISCOUNT, TIER_TEMPERATURE, THERMAL_OVERCLOCK; changeover bookkeeping
  fab/FabRecipeInfo                    # temperature, coil and single-block tier on the thermal modes' EMI pages
  machine/fab/FabMultiblockMachine     # SMC multiblocks: built-in clean room from filter casings, PTFE-pipe parallels, counters, console
  machine/fab/FabTieredMachine         # SMC single blocks: GT slot page + console strip, furnace temperature per tier
  machine/fab/FabConsoleWidget         # the fab console (full for multiblocks, strip for single blocks)
kubejs/startup_scripts/gtceu/photolithography.js  # litho, XCDA and MUV chemistry materials, reticles, sieves, 5 recipe types (fluid slots), structure, tooltips
kubejs/startup_scripts/gtceu/electronics_metallurgy.js # tier alloys (Al-Si, Kovar, Pt-Ir), zircon/zirconia/chlorides, zirconium properties
kubejs/server_scripts/mods/gtceu/photolithography.js # AF9_LITHO table (incl. light/resist/laserGas) + line/cutter/XCDA/MUV chemistry (Formox, DNQ, HMDS, TMAH) recipes + removals
kubejs/server_scripts/mods/gtceu/electronics_metallurgy.js # alloy mixers, zircon chain, zircon sands ore vein
kubejs/startup_scripts/gtceu/fab_chemistry.js      # 72 fab-chemistry materials + the resists, laser gases, ultrapure water the line uses
kubejs/server_scripts/mods/gtceu/fab_chemistry.js  # lines 1-5 (§6.12-6.16): Siemens + CZ boules, fluorochemicals, air gases, KrF / ArF resist, ultrapure water (fab_* types, `column()` makes the still cuts)
kubejs/startup_scripts/gtceu/fab_machines.js       # §11: 12 fab recipe types (slot layouts), 4 SMC single-block families (MV-LuV), 4 SMC multiblocks (structures, modifiers)
kubejs/server_scripts/mods/gtceu/fab_machines.js   # §11: crafting of the SMC machines
kubejs/server_scripts/mods/gtceu/mv_circuits.js     # MV circuits without transistors/diodes (Al-Si wire, Kovar pins)
kubejs/server_scripts/mods/gtceu/tiered_circuits.js # HV-LuV circuits: tier-matched chips + tier metals, HV/EV bootstraps
kubejs/server_scripts/circuits_for_atm.js          # newer ATM9 (synced from the instance): LuV Nano Mainframe on the Assembly Line + LUV RAM (AF9)
kubejs/server_scripts/ore_syn_recipes.js, updates_ig.js, kubejs/startup_scripts/ore_syn.js  # newer ATM9, synced unchanged
config/ftbquests/quests/chapters/*.snbt             # quests (§6.10); text in kubejs/assets/kubejs/lang/en_us.json (af9.quest.*)
kubejs/assets/gtceu/lang/en_us.json                 # machine/recipe-type names, tooltips 0-14, AF9 material names
kubejs/assets/kubejs/textures/item/                 # photomask blank, 12 reticles, molecular sieve (+ saturated)
kubejs/assets/gtceu/models/item/*.json              # 32 predicate overrides (16 wafers + 16 chips)
kubejs/assets/af9/models|textures/item/litho/*      # 160 per-mode models/textures
```

# Appendix B. Copy-paste snippets (KubeJS)

```js
// chip of a mode as an exact-NBT ingredient (server scripts share AF9_LITHO)
AF9_LITHO.tagged('ram', 'euv', 8)            // 8x EUV RAM chips
AF9_LITHO.nbt(AF9_LITHO.chip('nor'), AF9_LITHO.mode('xuv'))  // '{AF9Litho:{Node:100,Transistors:196000000}}'
```

# Appendix C. Research provenance (5 parallel agents)

- Agent A (codebase explore): existing IDs, wafer grades, line logic, version drift, 13 contradictions to avoid — folded into §§5/10.
- Agent B (wafer fab): quartz → MG-Si (11-13 MWh/t, 1414C) → Siemens (1080-1100C, 9N-11N) / FBR → CZ (0.5-2.5mm/min, Ar, B/P, Oi) / FZ → wire-saw → RCA SC-1/SC-2 + CMP + epi → SEMI specs/grades — folded into §1.
- Agent C (lithography/FEOL/BEOL): cleanroom ISO, HMDS/DNQ/CAR/MOR, DUVi 193nm NA1.35 vs EUV 13.5nm, RIE/DRIE vs HF/BOE/KOH/TMAH, implant B/P/As/Sb + RTP, PVD/CVD/ALD/epi, CMP, 400-1400 steps, nodes/CPP/MMP, KLA/CD-SEM/PCM/sort — folded into §2.
- Agent D (chips/SoCs): 36-term hierarchy transistor→system with what/made/used/example — folded into §3 table.
- Agent E (GTCEuM 1.20.1): 7.x tiers/IDs/machines/EU-t/fluids/cleanroom/what-is-missing (UNCERTAIN flagged) — folded into §4 + §§6-8.
- Numbers cross-checked against `LithoMode.java` + server scripts (§§5.3/5.6). Any conflict → code wins, flagged in §10.

*End of spec — implement, then run §10 checklist and update stale quest text (`medium/high/extreme_voltage.snbt` still say laser-engrave; wafers still satisfy via plain-NBT fallthrough).*
