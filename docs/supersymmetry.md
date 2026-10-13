# Supersymmetry: how the realistic-chemistry pack works, and what AF9 can steal

Review of the **Supersymmetry** modpack (SymmetricDevs/Supersymmetry, branch `master-ceu`,
1.12.2 GregTechCE + custom `susy-core` + GroovyScript), researched October 2026 from the live
repo and wiki. Purpose: inspiration for AF9's chemical lines — what SuSy does, how long its
chains run, where its chemistry is faithful, and where it cheats.

Pack status at review time: BETA, "EV mostly done" (CurseForge); ~108k downloads. Everything
below was read out of actual repo files (paths given); step counts are recipe counts ±1.

## 1. What the pack is

A GTCE-based expert pack whose whole identity is **real industrial chemistry as progression**.
Custom content lives in two places: `groovy/` (materials + ~thousands of recipes) and the
`susy-core` mod (~100 custom recipe maps, ~80–90 custom machine types). Design pillars:

- **Vessels, not magic boxes.** Separate multiblocks for batch reactor, CSTR, fixed/trickle/
  fluidized bed, bubble column, roaster, rotary kiln, crystallizer, dryer, evaporation pool,
  clarifier, froth flotation, mixer-settler, electrolytic cell, polymerization tank, catalytic
  reformer, coking tower, vacuum / cryogenic / sieve distillation, PSA, ion exchange, CVD/ALD,
  even lithography/etch tools. The machine *is* the unit operation.
- **Catalysts as a parallel tier ladder.** Recipes take catalysts as not-consumed inputs with a
  *minimum tier* (`CatalystInfo(tier, yield, energy, speed)`); better catalysts in the same family
  satisfy more recipes and run them better. Two physical forms: supported dusts (Pt/Pd/Ni on
  alumina, Raney Ni/Co/Cu leached in NaOH, zeolites, Wustite for Haber…) and packed beds that
  live in the reactor. Only FCC models deactivation (coke burn + regen); everything else is
  magic never-fouling beds.
- **Separations are half the game.** Debutanizer/depropanizer/deethanizer trains, amine wash +
  regen loops, glycol dehydration, cryogenic ASU with Ar side-arm and He/Ne/Kr/Xe recovery,
  Parex-like p-xylene crystallization, solvent extraction as single mixer-settler passes.
- **Progression**: primitive → steam → LV (drills, Nether) → MV (aluminium via Hall–Héroult in
  their electrolytic cell) → HV (oil, titanium) → EV (partially done; space after).

## 2. Scale (order of magnitude, not census)

- `groovy/material/`: 12 files, ~0.9 MB. Biggest: organic chemistry (196 KB), unknown-composition
  (152 KB), SuSy materials (148 KB). Low-thousands of declared material handles incl.
  intermediates, slurries, solutions — not all "elements".
- `susy-core` `SuSyRecipeMaps.java`: ~100 custom recipe maps (batch/CSTR/fixed/trickle/
  fluidized/bubble-column reactors, roaster, crystallizer, dryer, evaporation pool, clarifier,
  flotation, mixer-settler, sintering oven, polymerization tank, reformer, coking tower, vacuum/
  sieve/high-temp/cryo distillation, PSA, ion exchange, CVD/ALD, litho/etch set, …).
- `SuSyMetaTileEntities.java`: ~150+ registered IDs, ≈80–90 distinct machine types.
- Single files hit 94 KB / ~340 recipes (`ChemistryOverhaul.groovy`).

## 3. The chemical lines (length = recipe steps)

### Oil & petrochemicals
| Line | Length | Route |
|---|---|---|
| Crude front end (desalt → preheat → atmospheric distillation, 3 crude grades) | ~7 | electrostatic desalting → steam-stripped sieve tower → overheads split + sour-water stripper. Real desalter front end. |
| Light-ends train (debutanizer → splitter → depropanizer → deethanizer → amine → iC4/nC4) | ~8 | Ternary FCC light-ends recovery with cryo split to ethane/ethylene/methane. |
| Hydrotreating + amine/H₂S loop | ~4 per fraction | fixed-bed HDS → stripper → ethanolamine regen to H₂S. |
| Steam cracking (7 feeds) | 2 per feed | cracker/roaster + 2 sieve splits; e.g. heavy naphtha → ethylene 1350 + propene 500 + butadiene 155 + … (schematic yields). |
| FCC with catalyst regen | ~4 | crack → sieve (spent catalyst out) → roaster burn → regen. Only chain modeling deactivation. |
| Hydrocracking, thermal cracking/visbreaking/delayed+fluid+flexi coking | 2–6 | incl. propane deasphalting with supercritical propane recycle, slack-wax crystallization, carbon black kiln. |
| Isomerization / HF alkylation / SPA polymerization / catalytic reforming | 2–4 each | Both real isomerization catalyst generations (newer 4× faster); HF acid inventory recycled; Pt reformer is net H₂ producer. |
| Gas-to-liquids (reforming, acetylene, LTFT/HTFT, WGSR+PSA, MTG) | ~30 | H-ZSM-5 methanol-to-gasoline; LTFT vs HTFT olefinicity split; Fe-Cr vs Cu-Zn-Al shift pair. |
| Natural gas (glycol dehydration → amine → NGL split + He) | 4 | Triethylene-glycol drying and LNG helium recovery modeled. |
| Coal tar/BTX/naphthalene/pyridine/quinoline | ~12 | Coke-oven grades → tar distillation → acid/base washes → furfural extraction. |
| Biofuels (biodiesel + HVO green diesel/jet) | 7–10 | Transesterification + glycerol recovery; hydrotreating + hydroisomerization for cold flow (real NExBTL analog). |
| Aromatics complex (BTEX, xylenes loop, TDP/HDA, cumene→phenol) | ~15 | Furfural *and* sulfolane extraction (sulfolane correctly faster); Parex-like crystallizer + Isomar recycle; faithful 1:1 phenol:acetone. |
| Alcohols / aldehydes / acids (MeOH, EtOH, oxo, Ziegler–Alfol, Wacker, Monsanto/Cativa) | 14–30 each | Rh (Monsanto) vs Ir (Cativa) with Cativa faster; Schulz–Flory C2–C16 slate commented in-file. |
| Lubricants (PAO, PIB, ester oils, full additive slate, graded blending) | ~60 | Pt-SAPO-11 hydrodewaxing, BF₃ PAO, ZDDP/MoDTP, 7-base-oil blending ladder. |
| Pharma/explosives/surfactants | 5–12 each | Paracetamol 5 steps; RDX/PETN/TNT; Triton X-100; Novec fluorosurfactant. |

### Inorganic & acids
| Line | Length | Route |
|---|---|---|
| Sulfuric acid (Contact + lead-chamber legacy) | 28 | V₂O₅ fixed bed; full nitrosylsulfuric-acid/NOₓ chamber loop with lead foil. |
| Nitric acid (Ostwald + arc backup) | 29 | Wustite Haber + recycle; Pt-Rh gauze with explicit Pt-only selectivity penalty; exact 3NO₂+H₂O absorption stoichiometry. |
| HCl web (direct + Mannheim + Deacon + CaCl₂ recycle) | ~25 | CuCl₂ vs RuO₂ Deacon with 4× rate difference. |
| HF/F₂ (fluorite + Moissan KF·HF electrolysis) | ~6 | Ni/Pt electrodes, MV. |
| Chlor-alkali (diaphragm vs membrane) | 23-file | Asbestos 720 t vs Nafion 540 t; Downs cells with CaCl₂ flux for Na/Li/K. |
| Solvay/Hou soda ash | ~6, all LV | NH₄Cl co-product explicit; ammonia regen via quicklime. |
| Air separation (double-column cryo ASU) | 58 | Mol-sieve decarburization + vacuum regen, HP/LP columns, Ar side-arm with Pt deoxo, He/Ne wash + PSA, Kr/Xe from O₂ sump. |
| Silicon (carbothermic → Siemens + disproportionation cascade) | ~24 | SiCl₄ hydrochlorination, AlCl₃ cascade to silane, 25% single-pass CVD + offgas fractionation. |
| Lithium (spodumene/petalite/amblygonite/lepidolite + salar brine) | 35 | α→β decrepitation, acid roast, Li₂CO₃ strike, LiCl roast, molten-salt Li electrolysis, LiOH causticization, LiH/LiAlH₄. |
| Boron (kernite/colemanite/borax → boric acid → B → BCl₃) | 34 | Magnesiothermic reduction, boron solvent-extraction loop, LiBF₄ electrolyte salt. |
| Aluminium (Bayer → Hall–Héroult → Hoopes) | 28 | Cryolite/AlF₃ sub-chains, red-mud valorization. |
| Rare earths (Ce/La + stubs) | 9–11 | TBP/Aliquat mixer-settlers → fluoride → calciothermic reduction in Ta crucible under Ar. |
| Deuterium (Girdler sulfide, 8× staged enrichment), H₂O₂ (anthraquinone), zeolites (A/X), desalination (MSF), wastewater (flocculant → RO → ion exchange) | 4–32 | Real named processes throughout. |

## 4. Realism verdict (fact-checked)

**Faithful, verified against real chemistry:** Ostwald absorption stoichiometry; air 78.08/20.95
PSA split; Pt-Rh vs Pt-only gauze behavior; Fe-based Haber catalyst + recycle; V₂O₅ contact;
fluorite→HF; KF·HF fluorine electrolysis; Downs-cell flux; Hall–Héroult cryolite; Siemens chain
depth; Bayer digestion split; spodumene decrepitation; Hou NH₄Cl co-product; RuO₂ Deacon;
Monsanto→Cativa speedup; H-ZSM-5 MTG; Schulz–Flory comment; Parex/Isomar loop shape;
amine + glycol loops; Girdler sulfide; anthraquinone H₂O₂; cumene 1:1 phenol:acetone; FCC
regen burn; newer isomerization catalyst 4× faster. The catalyst choices read like they were
picked by someone who knows refinery practice.

**Game simplifications (honest ones):** mB treated as pseudo-moles with inflated light-gas
volumes (documented `ASSUMPTIONS` in steam cracking — never cite yields as data); one-block
sharp splits replace superfractionator trains; tray columns collapse into BCR/CSTR seconds;
no numeric temperatures anywhere (cryo modeled by machine type); SX cascades are single passes;
flotation is one-shot; energy = voltage tier + ticks, kinetics = duration; lumped
pseudo-components (`fuel_gas`, `reformate`, `antiknock`) with arbitrary blending multipliers;
deliberate anachronisms (TEL, MMT, CFCs — one file jokes "I eated the ozone layer").

## 5. Direct comparison: their brine/lithium vs ours

SuSy runs **sea-brine + salar-brine purification → NaCl/KCl/Na₂SO₄ crystallization → Li₂CO₃
strike + boron solvent extraction** (`BrineChain`, 10 recipes), then a 35-step hard-rock line
(spodumene decrepitation → acid roast → carbonate → chloride → molten-salt Li electrolysis →
LiOH causticization). AF9's `docs/lithium-brine.md` line covers the same salar chemistry in
8 recipes with GT machines. Takeaways: (a) our chain shape is sound — theirs just resolves
each separation (crystallizers, SX loop) that we fold into one block; (b) they chose **LiBF₄**
as the electrolyte salt where we chose LiPF₆ — both real (LiBF₄ older, LiPF₆ modern standard),
no change needed; (c) their boron SX loop validates giving our borax side-stream a future
upgrade path.

## 6. Inspiration list for AF9 (concrete)

1. **Tiered not-consumed catalysts.** AF9 has nothing like this; a Pt-reformer-style bed item
   (made once, slotted into LCR/filtration-style recipes with minimum-tier gating) would add
   depth to litho/fab chemistry without new machines.
2. **Regeneration loops as gameplay.** Amine (H₂S in → H₂S out elsewhere), glycol TEG,
   sulfolane/furfural recycle, TBP/Aliquat return — AF9's orb glue and solvents are
   once-through; closing even one loop (PGMEA already recycles; dielectric paste could too)
   echoes SuSy's best trick.
3. **Evaporation ponds for brine.** SuSy's `evaporation_pool` (solar, slow, free energy)
   validates AF9's distillery-as-ponds — or suggests a future solar pond multiblock.
4. **Explicit yield penalties for cheaper paths.** Pt vs Pt-Rh gauze, sulfolane vs furfural,
   Cativa vs Monsanto — AF9 already does this (carbon bed 300 vs fractionation 600 PGMEA);
   apply the pattern deliberately everywhere.
5. **Wastewater line.** SuSy has flocculant → clarifier → RO → ion-exchange DI water; AF9's
   filtration plant + filter cake covers the same ground in fewer blocks — fine.
6. **Cite the chemistry in-file.** SuSy comments carry Ullmann citations, assumption tables,
   and honest "never all of it" notes. AF9's docs already do this — keep it up; it's what
   makes a chain feel researched instead of invented.
7. **Don't copy scale.** SuSy's 60-recipe lubricant file and 58-recipe ASU are a full-time
   job to balance; AF9's folded-block style (one machine, honest abstraction in docs) is the
   right scope for this team.

Sources: `SymmetricDevs/Supersymmetry@master-ceu` (`groovy/postInit/chemistry/**`,
`groovy/material/`, `susy-core` recipe maps/machines), wiki `susy.miraheze.org`
(Voltage_Tiers, Steel/Aluminium_Production), CurseForge page. Chain inventories were read
file-by-file; counts are recipe counts ±1; realism verdicts are mine, checked against
standard industrial chemistry.
