# Oil

Oil is no longer found in the ground of the Overworld. It comes from **Oil Regolith**, a sand-like rock in the asteroids, and the
Asteroid Field's fluid deposits hold nitrogen, oxygen, heavy water and acids.

Code: `af9-core` `com.af9.core.space` (`OilRegolithBlock`, `AF9Space`, `AsteroidFieldFeature`). KubeJS:
`kubejs/startup_scripts/gtceu/oil.js` (the fluids), `kubejs/server_scripts/mods/gtceu/oil.js` (the chain),
`kubejs/server_scripts/mods/gtceu/vein_oil.js` (the world).

## 1. The chain

```
Oil Regolith (asteroid rock)
  -> centrifuge, MV: 2 regolith -> 1,500 mB Impure Oil, sand, 25 % sulfur
Impure Oil (black and brown)
  -> chemical reactor, MV: 1,000 mB + 100 mB sulfuric acid -> 900 mB Shiny Oil, 100 mB diluted sulfuric acid
Shiny Oil (amber, with glints running over it)
  -> distillation tower, MV: 1,000 mB -> 600 mB Oil, 300 mB Heavy Oil, 15 % sulfur
Oil, Heavy Oil: GT's own (its refining goes on as it always did)
```

Impure Oil and Shiny Oil have their own animated textures (`kubejs/assets/gtceu/textures/block/fluids/fluid.impure_oil.png` and
`fluid.shiny_oil.png`, 16 frames, generated offline). GT draws them untinted (`customStill().disableColor()`).

## 2. Oil Regolith

`af9:oil_regolith`: a falling block (like sand), shovel. `AsteroidFieldFeature` grows it in **pockets** of the asteroids' rock: a second
noise decides (`OIL_POCKET` 0.38, about a seventh of the rock). So the oil sits in the asteroids with the ore, mostly in the big ones.

## 3. The world's oil is off

* GT's oil fluid veins (`gtceu:oil`, `oil_heavy`, `oil_light`, `oil_medium`) get weight 0 (`vein_oil.js` goes over GT's registry of
  bedrock fluid veins, so it does not depend on the vein ids; the log says how many it switched off).
* Ore veins that hold `oilsands` get weight 0 (the same `modifyAll` as the uranium veins).
* Ores no vein holds are Mk4 for the Microverse Projector (`docs/microverse.md`): oil sands could be farmed there, with a dust as seed.

**Rocket fuel does not need oil.** Aluminised Hydrolox needs triethylaluminium, which needs ethylene, and GT makes ethylene from
ethanol (`ethylene_from_ethanol`; ethanol from biomass by distillation), so the first flight to the Asteroid Field needs no oil.
The oil products that have no such route (benzene, propene and what is made from them) wait for the first oil.

## 4. The asteroids' fluid deposits

GT's bedrock fluid veins (the prospector finds them, the Fluid Drilling Rig drills them) in `af9:asteroid_field`, one per region,
chosen by weight:

| Deposit | Fluid | Weight | Yield (mB/s) |
|---|---|---|---|
| `af9:void_nitrogen` | nitrogen | 40 | 250-600 |
| `af9:void_oxygen` | oxygen | 40 | 250-600 |
| `af9:void_heavy_water` | heavy water | 20 | 100-300 |
| `af9:void_sulfuric_acid` | sulfuric acid | 20 | 150-350 |
| `af9:void_hydrochloric_acid` | hydrochloric acid | 15 | 150-350 |
| `af9:void_nitric_acid` | nitric acid | 15 | 150-350 |
| `af9:void_hydrofluoric_acid` | hydrofluoric acid | 10 | 100-250 |
| `af9:void_phosphoric_acid` | phosphoric acid | 10 | 100-250 |
| `af9:void_acetic_acid` | acetic acid | 8 | 100-250 |

Each depletes by 1 % a drilling cycle with a 1 in 100 chance, down to 25 mB/s. **Heavy Water** (D2O) is AF9's material (GT has deuterium and no
heavy water); an electrolyzer turns 1,000 mB into 2,000 mB deuterium and 1,000 mB oxygen.

Only chunks generated after this exist have the rock; the fluid deposits are not in blocks and apply to the whole dimension.
