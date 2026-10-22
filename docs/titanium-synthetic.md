# Synthetic titanium (HV to end of EV) - design draft

Status: draft for Janne, no recipes written yet. Replaces the single "64 moon sand -> 100 mB TiCl4" bridge in
`planet_metals.js` with a real chain. Ends when the IV player flies the tier 3 rocket to Venus and mines the Venus ilmenite vein
(landed: `af9:venus_ilmenite_vein`, af9-core 0.1.74).

## Reading I am assuming (please confirm)

1. Titanium for HV and EV (T2 rocket alloy, EV machines, T3 rocket parts) comes **only** from the synthetic chain below.
2. Real ilmenite ore exists only on **Venus** (renewable). The Mars ilmenite vein and the Mars-sand separator/leach recipes are
   switched off, so Mars gives desh/zircon only. (If you want Mars ilmenite kept as a bonus, say so; it is one line.)
3. The chain is a gate: slow, many steps, big chemical bills, low yield at first and better yield with each stage.
   The old Earth-side loopholes stay closed (bauxite electrolysis and sludge give no rutile).

## Feedstocks

- **Lunar regolith** (moon sand, moon stone): carries ilmenite traces (real: mare regolith is up to 10 % ilmenite).
- **Red mud**: new byproduct of the Bayer process on bauxite (real: 5-20 % TiO2). Needs a new `red_mud` fluid/dust pair in af9-core's AF9Materials. Bauxite stays an aluminium ore; this adds the by-product the removed rutile used to be.

## Stage 1 - HV: "Lunar Beneficiation" (about 10 steps, 100 mB TiCl4 per ~16 sand-equivalents, poor)

```
moon sand --electromagnetic separator--> magnetic fraction (ilmenite-rich "lunar concentrate") + silica
concentrate + hydrogen (1200 K, HV chem reactor/ EBF)  -> iron + titanium dioxide (real hydrogen reduction of ilmenite) + steam
iron fines out (a use for the iron: feeds back into steel)
TiO2 (impure) + sulfuric acid (digest) -> titanyl sulfate solution + iron(II) sulfate slurry   [sulfate process]
slurry -> centrifuge: ferrous sulfate crystals (copperas: sold as waste or reduced) / clarified TiOSO4
TiOSO4 + steam (hydrolysis) -> metatitanic acid -> calcine in EBF -> pigment-grade TiO2 (rutile dust, 1 per ~48 sand)
rutile dust + carbon + chlorine (chem reactor) -> TiCl4 crude -> distillation (new): TiCl4 + VOCl3 residue
TiCl4 -> GT Kroll (blast furnace + magnesium) -> titanium
```
Waste streams that must be handled: dilute sulfuric acid (recycle by concentrating), ferrous sulfate, chlorine off-gas (recycle
via the existing MgCl2 electrolysis loop). Yield stays small: stage 1 is "enough for the T2 alloy and a few EV hulls".

## Stage 2 - mid EV: "Red Mud Recovery" (needs the Large Chemical Reactor and PTFE, so it is naturally EV)

```
bauxite --Bayer (NaOH, steam, LCR)--> sodium aluminate liquor + RED MUD (new)
red mud --HCl leach (PTFE-lined LCR)--> iron/titanium chloride solution + sodalite tailings
solution --solvent extraction (new: kerosene/ TBP-like "extractant" made from diesel + phosphoric acid, mixer + centrifuge)--> TiOCl2 strip liquor
liquor --hydrolysis--> TiO2 --> same chlorination/Kroll tail as stage 1
```
Gives about 4x the titanium per run of stage 1 and uses bauxite, the one Earth ore the pack kept. Heavy: needs the diesel ladder,
phosphorus and chlorine chains, so it ties into the oil and chemical lines chapters.

## Stage 3 - end of EV: "Titanium Loop" (efficiency, so EV machines and the T3 rocket are affordable)

- Closed TiCl4 loop: all chlorine returned by MgCl2 electrolysis (already GT), magnesium recycled, a catalytic upgrade (vanadium oxide scrubber) for 2x TiCl4 per rutile.
- Optional "plasma arc" EBF-tier recipe (ammonium chloride + TiO2 -> TiN dust -> Ti) as the premium path, EV laser-tier energy.

## Hand-off to Venus (IV)

T3 rocket (needs titanium and 2 EV circuits) -> Venus -> `af9:venus_ilmenite_vein` + Venus sand: ilmenite goes straight into the
sulfate/chloride tail at 10-20x the yield. Synthetic stages are not removed (they remain the "off-world-free" fallback).

## Work packages

| # | Work | Where |
|---|---|---|
| 1 | AF9 materials: red mud, lunar concentrate, titanyl sulfate, metatitanic acid, extractant, TiOCl2, TiN | af9-core `AF9Materials` + registry list + lang |
| 2 | Stage 1 recipes (about 12) | new `titanium_synthetic.js`, change `planet_metals.js` bridge |
| 3 | Stage 2 + 3 recipes (about 14) | same file |
| 4 | Switch off Mars ilmenite + Mars sand recipes (if confirmed) | `vein_mars.js`, `planet_metals.js` |
| 5 | Quests: HV and EV chapters ("Almost Titanium" series, links in TFG-style) | quest files + lang |
| 6 | Docs a titanium doc (planned), update asteroid-fission section 7 | docs |

## Questions

1. Confirm assumptions 1-2 (Mars ilmenite off, titanium only synthetic until Venus)?
2. Stage 2's feedstock is red mud from bauxite. OK, or do you prefer a different feedstock (e.g. fly-ash, scrap titanium recycling)?
3. New materials need a jar rebuild (af9-core). OK to bump to 0.1.75 with it?

Chemistry basis: hydrogen reduction of ilmenite, the sulfate and chloride (Kroll) processes and red-mud titanium recovery are
real industrial processes; the numbers here are game balance, not chemistry.
