# AF9 Multiblock Mechanics Research — Java-native, non-litho

> Status: research only. No lithography. Future work is pure `af9-core` Java, no new KubeJS.
> Base patterns: `WorkableElectricMultiblockMachine` subclasses in
> `af9-core/src/main/java/com/af9/core/machine/`, consoles in `machine/console/`,
> expedition logic in `elevator/SpaceElevatorMachine.java`.

## Design constraints

- Industrial / sci-fi production theme only. No magic.
- Minigames never gate progress: no input = 100% baseline, skilled play = up to +30–40%.
- Persistent bonuses never obsolete the next tier: cap total at ~35%.
- All state in Java with `@Persisted` / `@DescSynced` + `ManagedFieldHolder`.
- All bonuses via `RecipeModifier.getModifier()` (same hook as `LITHO_VERSION` / fab `PURGE`).
- Definition + recipe types + machines move to Java via `GTRegistries` / `GTRecipeType`.

---

## 1. Controller Settings (PLC / SCADA style)

Real industrial controllers are programmed, not just on/off. GTCEu only has mode tab + batch toggle.

### 1A. Operating Envelope Editor

- **What:** Per-controller sliders: max parallel cap, input voltage clamp, overdrive % (0–30%), idle power floor.
- **Play:** Clamp a chem plant to 1A so it never trips the substation. Overdrive a refinery at +20% speed for +60% power + extra heat.
- **Why new:** No GT pack lets you derate a multiblock. This is how real VFDs/PLCs work.
- **Java sketch:** `FancyConfigurator` sliders on the controller (see `CombinedDirectionalFancyConfigurator` import in `elevator/SpaceElevatorMachine.java`). `@Persisted int parallelCap, overdrivePct`. Apply in `RecipeModifier`.

### 1B. Fault-Response Protocols (Redline settings)

- **What:** Dropdowns: on power sag / on output jam / on maintenance fail → `HOLD` / `VENT+STOP` / `SAFE-SHUTDOWN` / `DUMP-TO-FLUID-HATCH`.
- **Play:** Hot machines vent coolant instead of exploding. Precision chem holds progress 60s waiting for power.
- **Why new:** GTCEu machines just stop. No industrial SIS / ESD concept exists.
- **Java sketch:** Override energy-drop / recipe-interrupt path (same area as `FabRecipeLogic`). Store `enum FaultAction` as `@Persisted`. VENT reserves a fluid-output slot; reuse `machine/part/CoolantHatchPartMachine.java` pattern.

### 1C. Recipe Filter Matrix

- **What:** Allow/deny per recipe + input-bus priority + campaign lock (stay on one recipe for N runs).
- **Play:** Lock a rectification column to one separation for 50 runs for a campaign bonus. Prevents accidental mode flips.
- **Why new:** GT mode tab is all-or-nothing. No per-recipe PLC allowlist.
- **Java sketch:** `ResourceLocation` allowlist `@Persisted`, checklist UI in a `SidePanelsUIWidget`-style panel, gate in `canStartRecipe()`.

### 1D. Load-Following Scheduler

- **What:** Time slots or wireless-channel thresholds: RUN / IDLE / BOOST.
- **Play:** Refinery idles when solar dips, boosts when ZPM buffer > 80%.
- **Java sketch:** `TickableSubscription` sampling `energyContainer` + world `dayTime` + `wireless/WirelessChannels.java`. Gate only, no recipe change.

---

## 2. Minigames (operator skill, bonus-only)

### 2A. Field Harmonics Tuning

- **What:** Oscilloscope widget. Recipe has a target frequency (e.g. 47.3 kHz) that drifts with temperature. Player drags a slider to match. Closeness = 0–25% speed + 0–15% yield.
- **Fits:** Particle Accelerator, plasma / RF furnaces, Sanguinite Hearth.
- **Why new:** Nothing in GT requires tuning. Closest analogues are magic-mod research minigames.
- **Java sketch:** New `TuningWidget extends Widget` in `machine/console/`. `@DescSynced float tuneHz, targetHz`. Target from recipe hash + machine temp. Modifier reads `1 - abs(tune - target) / window`. Target drifts slowly so it needs re-touches.

### 2B. Cold-Start Bring-Up Sequence

- **What:** First formation or after long offline: 3-stage checklist — Purge → Preheat → Strike. Each is a button with timer + required fluid (N2 purge, heat, plasma strike). Skip = runs at 50% for 10 min (thermal soak penalty).
- **Fits:** Thermal furnaces, cryostat, fission successor, fusion.
- **Why new:** EBF preheat is passive. No interactive bring-up in any tech mod.
- **Java sketch:** `enum BringUpPhase` `@Persisted`, `TickableSubscription` timers, console wizard panel, `common/AF9Sounds.java` cues.

### 2C. Containment Balance

- **What:** 2D dot drifts (plasma position). Player trims with X/Y nudge buttons, or auto-trim burns extra coolant. Centered = full yield, edge = waste-gas byproduct grows.
- **Fits:** Fusion-adjacent, quark synthesis, any magnetic-bottle machine.
- **Why new:** Mekanism fusion has injection rate but no spatial control. This is tokamak-operator fantasy.
- **Java sketch:** `@DescSynced float plasmaX, plasmaY` with random walk in `onTick()`. Nudge packets via `network/AF9Network.java` pattern (see `RingDeathPacket`). Auto-trim toggle consumes `supercooled_*` from hatches.

---

## 3. Skill Trees (GTNH-style, per-controller)

GTNH's shape: Assembly Line scanner research + data sticks + computation-gated unlocks + permanent tier jumps. Copy the shape, localize it per-machine.

### 3A. Firmware Branches (core proposal)

- **What:** Each multiblock earns `runtimeXP` per completed recipe EU. 3 branches x 5 ranks:
  - `THROUGHPUT` — −4%/rank duration
  - `EFFICIENCY` — −5%/rank EU/t
  - `FIDELITY` — +3%/rank yield, −4%/rank byproduct
- Pick one branch per rank. Irreversible without wipe. Rank 5 unlocks capstone (e.g. Throughput: +1 parallel).
- **Play:** Two identical columns diverge over a week — fast workhorse vs efficient baseline. Wipe with blank data stick (resets to 0, partial XP refund).
- **Why new:** GTNH researches recipes globally. Nobody does per-controller RPG leveling for machines.
- **Java sketch:** `machine/skill/FirmwareTrait.java`, `@Persisted int[] ranks, xp`. Hook `afterRecipeComplete()` (same place as `FabRecipeLogic`). UI: 3-column tree in controller GUI + Jade line `FW T3/E2/F1`.

### 3B. Research Imprinting

- **What:** Consume Research Station data sticks + CWU to imprint permanent recipe-class bonuses (e.g. CVD imprint: −10% H2). 3 imprint slots per controller.
- **Play:** Direct GTNH homage (scanner research → data orb → Assembly Line), localized to one multiblock.
- **Java sketch:** Custom `IRecipeHandler` consuming data-stick NBT, `ResourceLocation[] imprints` `@Persisted`. Modifier checks imprint table. Reuses `IOpticalComputationProvider` / CWU path from `docs/computation.md`.

### 3C. Tooling Lineage

- **What:** Lenses, targets, membranes, drill heads have HP + XP. Use → wear + level. Refurbish at 0 HP keeps level. Level = up to +15% output. Example: `Membrane Mk.II (Veteran)`.
- **Play:** Attachment to specific tooling pieces, refurb economy.
- **Java sketch:** Item NBT `af9_tool_xp`, `af9_tool_hp` + `registry/TooltipItem.java` lines. No new machine class needed.

### 3D. Fleet Doctrines

- **What:** Generalize the drone/module system: expedition units gain veteran missions → +stacks. Applies to any expedition machine (deep-core drill, orbital tug).
- **Java sketch:** Extract `ExpeditionTrait` from `SpaceElevatorMachine.java` for reuse.

---

## 4. Dimension / Environment Coupling

Existing coverage: orbit-gating (`OrbitalLithographyMachine`), sky-check (elevator). Available axes:

| Axis | Java hook | Industrial use |
|---|---|---|
| Dimension ID | `level.dimension().location()` | Vacuum furnace only in vacuum (Moon / no-atmo) |
| Altitude (Y) | `controllerPos.getY()` | Radiators +25% above Y=200, geothermal +1% / 10 blocks below Y=0 |
| Sky visibility | `level.canSeeSky(pos)` | Solar furnace / sinter works day + sky only |
| Day/night | `level.getDayTime()` | Solar + tidal; night-cooled condensers |
| Biome / temperature | `level.getBiome(pos)` | Cryo condensers better in cold biomes / asteroid field |
| Gravity | Ad Astra gravity API / dimension tag | Crystal growth perfect in zero-G, centrifuges need gravity |
| Radiation | `radiation/RadiationWatch.java` | RTG trickle machines, rad-hardened vs civilian variants |
| Vibration / EMI | custom field, see 4A | Precision vs heavy zoning |
| Atmosphere gas | Ad Astra O2 / custom tag | Combustion needs O2, CVD needs inert |

### 4A. Vibration & EMI Zoning (top pick)

- **What:** Heavy machines (crushers, large turbines, elevator motors) emit vibration radius 16–32 blocks. Precision machines in radius get +defect / −speed unless on damping foundation (shock-absorber block under controller).
- **Play:** Forces factory zoning — heavy hall vs metrology lab. Rewards planning over compact boxes.
- **Why new:** Nobody simulates factory-floor interference. Cleanrooms check their own structure, never neighbors.
- **Java sketch:** `VibrationMap` in `MultiblockWorldSavedData`, `TickableSubscription` scan every 5s, `BlockPos` → field strength. Damping = check blocks below controller for `af9:damper_casing`. Console + Jade line `VIB 0.3 m/s²`. Cheap: only controllers tick, cached.

### 4B. Gravity-Tiered Processes

- **What:** Crystal growth / foam / ZBLAN fiber: standard in gravity, perfect in zero-G (orbit / asteroid), degraded in high-G. Same recipe, different output table by gravity class.
- **Play:** Gives orbit / asteroid a permanent production role beyond mining.
- **Java sketch:** `GravityClass { GRAVITY, LOW_G, ZERO_G }` via `compat/adastra/AdAstraCompat.java` + dimension allowlist. `RecipeModifier` swaps chanced outputs. Survey item shows class before building.

### 4C. Thermal Environment + Heat Rejection Loop

- **What:** Machines produce heat into local air/fluid. Reject via air (free, worse in hot biome / daytime), water (medium), supercooled loop back to Cryostat (best, closed loop). Overheat → derate, not explosion (unless Redline = catastrophic).
- **Play:** Desert noon vs cold night matters. Cryostat becomes central plant utility.
- **Java sketch:** `HeatTrait`, `@Persisted double heatJ`, ambient from biome + Y + time. Link to `CoolantHatchPartMachine` return temp. Console temp bar (see `SupercoolerMachine` chamber readout).

### 4D. Site Survey + Foundation System

- **What:** Handheld survey tool: right-click prospective controller spot → gravity class, vibration, altitude bonus, sky, biome temp, dimension legality. Foundations (concrete / damping / cryo-piped) give small permanent bonuses.
- **Play:** Prospecting for factories like prospecting for ore. Kills build-anywhere meta.
- **Java sketch:** New `SurveyToolItem` + `ISurveyor` interface on machines. Client overlay, no GUI needed. Foundations = structure predicate counting foundation casings via `TraceabilityPredicate`.

---

## Recommended prototype order (pure Java)

1. **Firmware Branches (3A)** — highest GTNH fit, reuses recipe-logic + console patterns, zero worldgen risk.
2. **Vibration Zoning (4A)** — cheapest true never-seen factory-planning mechanic: one world-saved map + one casing.
3. **Harmonics Tuning (2A)** — one reusable widget for every future RF/plasma machine, streamer-friendly.

Next step: turn one into a full Java spec — classes, NBT schema, UI mock, modifier math, and KubeJS-to-Java migration notes.
