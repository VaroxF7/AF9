# AF9 Recipe-Logic Research — GTNH-inspired, Java-native

> Status: research only. No implementation. Future work is pure `af9-core` Java, no KubeJS.
> What AF9 already has (do not duplicate): fab `PURGE` changeover, `STRUCTURE_PARALLEL`
> (trays / membranes), `COIL_DISCOUNT`, `TIER_TEMPERATURE`, EBF-style overclock + `BATCH_MODE`,
> litho `LITHO_GATE` + break roll + vacuum 0–100, coolant hatches, computation hatches,
> wireless energy channels, orbit gating.
> Reference GTNH sources: Assembly Line (ordered slices + data-stick scanning → Research
> Station + Quantum Computer), Eye of Harmony (tiered field generators, success + pity,
> overflow penalty, Astral Arrays, wireless EU, planet-in-controller), Fusion (start cost,
> free consecutive runs), EBF coils (temp unlocks + discount / perfect OC), IC2 nuclear
> (heat layout + core-temp %), GT++ multis (EU discount + speed boost + tier parallels).

GTCEu Modern 7.2.0 already exposes recipe conditions in Java: cleanroom, `fusionStartEU`,
station/scanner research, environmental hazard, quest / game-stage gates. Everything below
is implemented as a new `RecipeCondition`, `RecipeModifier`, or `RecipeLogic` subclass in
`com.af9.core.recipe.*` — no KubeJS builder calls.

---

## 1. Research-gated recipes (GTNH's best gate, done AF9-native)

### 1A. Imprint keys (scanner research without KubeJS)
- **GTNH origin:** AL recipe won't start without a scanned data stick in the data-access hatch. Scanner → stick, later Research Station + Quantum Computer → data orb.
- **AF9 take:** Generic `ResearchCondition`: recipe declares `researchKey` (e.g. `trinium_cvd`). Controller needs an imprint item with that key in a dedicated research slot (not consumed). Scanning is its own recipe type on the Research Station that burns CWU + EU and writes the key NBT.
- **Play:** Unlock once, run forever. Data Bank block holds 64 keys, read via one hatch — same wallshare trick as GTNH AL data hatches.
- **Java:** `ResearchCondition implements IRecipeCondition`, `ResearchSlotTrait` (ghost-slot, no consumption), `ResearchStationLogic` writing `af9_research:<key>` NBT. EMI shows "Requires imprint: X".

### 1B. Tiered science (station + computation, not just an item)
- **GTNH origin:** Late recipes need Research Station + Quantum Computer + CWU/t-scale compute, not just a stick.
- **AF9 take:** Two-axis gate: key item AND sustained CWU/t during the run (your `IOpticalComputationProvider` path). Lose compute mid-run → pause, not fail (see 2B).
- **Java:** Condition checks `COMPUTATION_DATA_RECEPTION` hatch allocation at `canStartRecipe()` and per-tick in `RecipeLogic.update()`.

## 2. Startup / sustain mechanics (fusion's lesson, generalized)

### 2A. Ignition cost + free chaining
- **GTNH origin:** Every fusion recipe has a minimum energy capacity / start cost. Consecutive recipes skip it — keep it running or pay again.
- **AF9 take:** Any plasma / EUV / fusion-adjacent recipe type gets `ignitionEU` stored in the controller's buffer. First run after idle drains it; chained runs (same recipe class within 60s) skip it. Rewards 24/7 baseload over batching.
- **Java:** `IgnitionTrait` with `@Persisted long storedIgnition`, `RecipeModifier` returning `NULL` when `stored < cost`, `onRecipeFinish()` refills the chain timer.

### 2B. Keep-warm idle drain
- **GTNH origin:** Fusion wants to run passively for long periods; startup dominates cost.
- **AF9 take:** Hot machines (thermal furnace, hearth, fusion successor) draw 1–5% EU/t while idle-but-hot to stay at temperature. Cold = full re-ignition + bring-up penalty. Player chooses: pay idle tax or pay restart.
- **Java:** `TickableSubscription` drain when `isFormed() && !isActive() && heat > threshold`. Ties into the HeatTrait from the mechanics doc.

### 2C. Campaign bonus (LOAD-bearing twin of PURGE)
- **GTNH origin:** Platline / chemical lines punish product-hopping implicitly (cleanup, dead stock).
- **AF9 take:** You already punish switching (`PURGE`). Add the carrot: Nth consecutive identical recipe = +1% yield up to +15% at 15 runs, reset on switch. Fouling caps it (see 5A) so infinite campaigns aren't free.
- **Java:** Counter in `FabRecipeLogic.onRecipeFinish()`-style class, `@Persisted ResourceLocation lastRecipe; int streak`.

## 3. Precision mechanics (ordered input, exact amount)

### 3A. Slice-ordered assembly (AL homage, new domain)
- **GTNH origin:** AL slices: one unique input per bus, exact NEI order, no implied recipes. Automation puzzle with priorities / blocking mode / renaming duplicates.
- **AF9 take:** Don't clone the AL. Apply ordering to 1–2 new machines only: e.g. UHV `Plasma Soldering Array` (already stubbed as `plasma_soldering`) and a future `Component Assembly Line`. Order enforced by bus index, duplicates disambiguated by slot not rename (improvement: controller UI shows expected item per slice ghost).
- **Java:** `OrderedInputTrait`: map `inputBusIndex → Ingredient`. `RecipeModifier` returns `NULL` with a named error (`PatternStringError`) naming the wrong slice. Ghost rendering in console UI.

### 3B. Stoichiometric exactness (EOH overflow, generalized sanely)
- **GTNH origin:** EOH overflow penalty: `1 - e^(-(30R)²)` — 1% over = ~10% penalty, 8% over = ~100%. Brutal, automation-defining.
- **AF9 take:** Soften and reuse: recipes declare `exactFluids` with tolerance bands. Inside ±1% = full yield. 1–5% over = linear 5–30% yield loss + extra duration. Never void the run — GTNH's lesson is the curve, not the cruelty.
- **Play:** Makes precise hatch sizing + level-maintainers matter without save-scumming.
- **Java:** `ExactnessModifier`: compare `FluidIngredient.amount` consumed vs recipe nominal at `getModifier()` time. Needs hatch-gauge reads, not just recipe IO.

### 3C. Phase-locked feeding
- **GTNH origin:** None — this is the novel one in this family.
- **What:** Long recipes (5+ min) have 2–4 feed windows (e.g. "inject catalyst at 40–50% progress"). Feeding inside the window = bonus; missing = base yield. Automatable with timers, doable by hand.
- **Java:** `RecipeLogic.update()` progress hooks + input-bus polling at thresholds. Console shows window countdown.

## 4. Probabilistic mechanics with pity (EOH success, production-safe)

### 4A. Breakthrough rolls + pity (never all-or-nothing)
- **GTNH origin:** EOH success chance = base − dilation penalty + stabilisation boost; fail = only consolation spacetime; pity `S·(1−S)` per fail forces eventual success; Astral Arrays bypass pity via averaging.
- **AF9 take:** Never fail a whole run. Roll for *bonus* output only: base yield guaranteed, breakthrough (e.g. +50% dies, perfect crystal) at shown %. Pity accumulates per controller + recipe class, resets on breakthrough or recipe switch. No spacetime-consolation trap.
- **Java:** `BreakthroughModifier` appending chanced outputs scaled by `@Persisted float pity`. Roll in `onRecipeFinish()`, synced for console "PITY 62%".

### 4B. Defect lottery (litho-adjacent, for chem/crystal)
- **GTNH origin:** EOH yield penalty vs success tradeoff across stabilisation tiers.
- **AF9 take:** Crystal / epitaxy recipes roll defect class: Perfect / Standard / Reworkable. Reworkable loops back as input (grind + regrow), never trash. Stabilisation-analogue (clean filters, low vibration) shifts the distribution, never to 100%.
- **Java:** Chanced-output table swap by environment traits (vibration map, filter tier).

## 5. Residue / loop mechanics (platline + LFTR lessons)

### 5A. Fouling + CIP (the missing half of PURGE)
- **GTNH origin:** Large Neutralization Engine "toxic residue automation challenge" (upcoming GTNH), platline sludge loops.
- **What:** Each run adds `@Persisted fouling 0–100`. Yield decays ~0.3%/point. CIP cleaning recipe (caustic + time, no product) resets to 0. Campaign bonus (2C) vs fouling = the core tension: long runs pay, but foul.
- **Java:** `FoulingTrait`, cleaning `GTRecipeType` accepted on same controller, console fouling bar.

### 5B. Breeder loops (LFTR fuel-1 → fuel-2 pattern)
- **GTNH origin:** LFTR Fuel 1 is bad power but breeds U-233 for Fuel 2. Explicit breeder economy.
- **AF9 take:** 1–2 deliberate breeder loops: e.g. irradiate fertile blanks in the accelerator → fissile feed for the fission successor; grow seed crystals → cut → regrow tail. Breeder recipe is power-negative alone, system-positive.
- **Java:** Plain recipes + EMI tagging (`breeder` category), no new code beyond yield balance. Resist making everything loop — two loops max.

### 5C. Slag / tail assay (multi-output triage)
- **GTNH origin:** EOH oversized recipes (329 outputs, ME hatch required), distillation tower fractionation, platline byproduct maze.
- **What:** One flagship separator (rectification column successor) with 6+ fluid/item outputs where *split ratio* is player-set (3 presets: Light / Even / Heavy). Same inputs, different slate. Forces downstream planning.
- **Java:** `SplitRatio` `@Persisted enum` selecting among 3 precomputed chanced-output tables. Recipe viewers show all three.

## 6. Subsystem tiers (EOH field generators, reusable)

- **GTNH origin:** 138 compression + 168 dilation + 48 stabilisation generators, 9 tiers each. Compression gates recipe tier, dilation trades time for success, stabilisation trades yield for success. Wallshareable to save cost. Tier table is the whole endgame climb.
- **AF9 take:** Extract the pattern, shrink it: any flagship multiblock gets up to 3 subsystem slots (e.g. `FOCUS` / `DRIVE` / `STABILITY`), each 1–5 tiers from structure blocks. Same trade triangle: speed vs efficiency vs success/yield. Cap: never more than one flagship uses this (your Particle Accelerator successor is the candidate).
- **Java:** `SubsystemTrait`: count tiered casings at formation (`PatternMatchContext`), expose `focusTier/driveTier/stabTier`. One `RecipeModifier` applying the triangle. Wallshare-safe by counting only interior blocks.

## 7. Catalysts & programmed circuits (cheap depth)

- **GTNH origin:** Catalysts not consumed (cracking, platline), programmed circuits selecting EOH overclock count / AL recipe variant, Astral Arrays as parallel tokens (log-scale: `PE = ⌊log(8·AA)/log(1.7)⌋`, parallels `2^PE`).
- **AF9 take:**
  - Non-consumed catalyst slot (cracking, reforming, enzyme): catalyst degrades 1 HP per N runs, not per recipe — use Tooling Lineage NBT.
  - Circuit-selected variants: one recipe, circuit 1/2/3 = yield / speed / efficiency variant. No recipe bloat.
  - Parallel tokens: capped consumable (e.g. `Flux Lattice`, max 8) giving `+1 parallel` each on ONE machine, power ×1.6 each. Log-scale is overkill at AF9 scale — keep linear and capped.
- **Java:** `CatalystTrait` (not-consumed inventory + HP), circuit check in `getModifier()`, token count → `ParallelLogic.getParallelAmount` cap.

## 8. Variable-rate / power-following recipes (new vs GTNH)

- **GTNH origin:** None — GTNH assumes flat EU/t. This is the gap.
- **What:** `FLEX` recipes accept 50–150% EU/t and scale duration inversely (within bounds). Over-supply wastes some power (85% efficiency above 100%), under-supply slows linearly. Lets renewables / elevator-batch power drive chem without batteries.
- **Java:** `FlexModifier`: read `energyContainer.getInputPerSec()` vs nominal, adjust `durationMultiplier`. Needs per-tick EU sampling, not just start-gate.

---

## Prototype order (smallest Java first)

1. **Catalyst slot + circuit variants (7)** — 1 trait, immediate depth on existing chem.
2. **Campaign + fouling (2C + 5A)** — 2 counters on existing `FabRecipeLogic` lineage, creates the switch-vs-stay game.
3. **Ignition + chaining (2A)** — 1 trait, makes baseload vs batching real.
4. **Imprint keys (1A)** — 1 condition + station recipe type, GTNH-authentic gating for all future UHV.
5. **Breakthrough + pity (4A)** — 1 modifier, only after the above (needs pity persistence).
6. **Subsystem tiers (6)** — only with a new flagship; do not retrofit.

## Explicit non-goals (GTNH things to NOT copy)

- Full-fail long recipes (EOH fail = hours lost). AF9 rolls bonuses, never voids runs.
- 1%-overflow = 10% penalty brutality. AF9 uses ±1% free band + linear decay.
- Uncapped log-scale parallels (Astral Arrays to 65k). AF9 caps tokens at 8.
- Slice-ordering on every machine. One array only, with ghost-slot UI (no rename-mold hack).
- IC2-style heat-component Tetris inside the controller grid. Vibration/EMI zoning (other doc) covers layout depth without inventory puzzles.
