# Oil

Oil is no longer found in the ground of the Overworld. It comes from **Oil Regolith**, a sand-like rock in the asteroids, and the
two belts' fluid deposits hold nitrogen, oxygen, heavy water, acids (the upper field) and volatile gases (Ceres).

Code: `af9-core` `com.af9.core.space` (`OilRegolithBlock`, `AF9Space`, `AsteroidFieldFeature`),
`registry/AF9Materials` (the fluids), `kubejs/server_scripts/mods/gtceu/oil.js` (the chain),
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
noise decides (`OIL_POCKET` 0.65 at a scale of 0.05: about **7 %** of the rock, in separate deposits of some hundreds of blocks, which
only the bigger rocks hold). So the oil sits in the asteroids with the ore, mostly in the big ones. (It was 0.38 at 0.07, measured at a
fifth of the rock with the pockets running into each other: regolith everywhere.) Rocks that are already generated keep their old stone;
new chunks have the new share.

## 3. The world's oil is off

* GT's four oil deposits (`gtceu:heavy_oil_deposit`, `light_oil_deposit`, `oil_deposit`, `raw_oil_deposit`) are **removed** (`vein_oil.js`,
  by id with the event's `remove`, so they are gone from the recipe viewers' bedrock fluid diagrams too; first they only had weight 0 and
  still showed; the natural gas deposit stays; the log says how many it removed). The script used to walk GT's
  registry (`entries().forEach`), which Rhino cannot do: it cannot call methods on the unmodifiable views and immutable lists GT hands out
  (`cannot access a member of class java.util.Collections$UnmodifiableMap$UnmodifiableEntrySet`), so the whole event stopped with that
  error and **no deposit was ever registered** (the reason for "no fluid veins"). Where a script has to walk such a collection it copies it
  into an `ArrayList` first (`new ArrayList(collection)`, the collection only as an argument).
* Ore veins that hold `oilsands` get weight 0 (the same `modifyAll` as the uranium veins).

**Rocket fuel does not need oil.** Aluminised Hydrolox needs triethylaluminium, which needs ethylene, and GT makes ethylene from
ethanol (`ethylene_from_ethanol`; ethanol from biomass by distillation), so the first flight to the Asteroid Field needs no oil.
The oil products that have no such route (benzene, propene and what is made from them) wait for the first oil.
From LuV on, ethylene (and propene with it) goes mass-scale without any oil or farm: the
methanol-to-olefins chain (wood + water + power, green-chemistry §7) carries polyethylene
demand until the Space Elevator's ethylene missions.

## 4. The belts' fluid deposits

GT's bedrock fluid veins (the prospector finds them, the Fluid Drilling Rig drills them): `af9:asteroid_field`
holds the acid belt's deposits, `af9:ceres` the lower belt's volatiles (its thin exosphere), one per region,
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
| `af9:ceres_hydrogen` | hydrogen | 40 | 250-600 |
| `af9:ceres_helium` | helium | 30 | 200-500 |
| `af9:ceres_methane` | methane | 30 | 200-500 |
| `af9:ceres_argon` | argon | 20 | 100-300 |
| `af9:ceres_carbon_dioxide` | carbon dioxide | 20 | 150-350 |
| `af9:ceres_neon` | neon | 12 | 50-150 |
| `af9:ceres_krypton` | krypton | 8 | 40-120 |
| `af9:ceres_xenon` | xenon | 6 | 30-100 |

Each depletes by 1 % a drilling cycle with a 1 in 100 chance, down to 25 mB/s. **Heavy Water** (D2O) is AF9's material (GT has deuterium and no
heavy water); an electrolyzer turns 1,000 mB into 2,000 mB deuterium and 1,000 mB oxygen.

Only chunks generated after this exist have the rock; the fluid deposits are not in blocks and apply to the whole dimension.

**Finding them.** A deposit is not a block: it is a number the world keeps for a chunk (GT's *bedrock fluid vein*), one deposit for every square
of 8 x 8 chunks (128 blocks), chosen by the weights above. You see it with the **Prospector** (the pack's quests introduce the HV one): sneak +
right-click switches it to *Fluid* mode, the map then colours every chunk with its fluid, and hovering gives the fluid and the yield. A
**Fluid Drilling Rig** (MV and up) standing on the chunk drills it. There is no bedrock in the Asteroid Field and nothing to see in the rock.

**Old worlds.** GT decides a chunk's vein the first time anything asks for it and saves the answer. A chunk of the Asteroid Field that was
prospected while the dimension had no deposits was saved as "no fluid" and showed nothing even after the deposits were added. `vein_oil.js`
forgets those empty entries when a world loads (log line `vein_oil.js: N empty fluid veins of af9:asteroid_field forgotten`), so the next look
rolls a deposit. The server log also says `vein_oil.js: 9 fluid deposits registered for af9:asteroid_field` when the script ran, and names a
deposit whose fluid does not exist. (The Overworld's chunks that were prospected while oil was on keep their oil: GT saves them too.)
