---
title: "AF9 Asteroid Fission: the Asteroid Field, Brannerite, the FX-1 Reactor, supercritical steam, plutonium and the Fusion Reactor Mk1"
branch: "main"
minecraft: "1.20.1"
forge: "47.4.0"
gtceu: "7.2.0 (GregTech CEu Modern)"
ad_astra: "1.15.20"
af9_core: "0.1.0 (mod_id `af9`)"
status: "Implemented. Java compiles on CI, the lint suite (run.sh --selftest) passes; nothing of it has been started in the game yet (§9 lists what to look at first)."
agent_hint: "`gtceu:` = base GregTech and everything AF9 registers through GT's KubeJS (materials, machines, recipe types). `kubejs:` = AF9's plain items (the fuel pellet, rod and spent rod). `af9:` = AF9 recipe ids, dimensions, planets and the Java feature. Numbers live in the scripts named in §10; the lint (`tools/lint/facts.py`, X3 and X4) holds the quest texts and the shared names to them."
---

# 1. What it is

The old way to uranium and plutonium was a vein and an electrolyzer. It is closed. Uranium comes from **Brannerite**, an ore
that generates only in the **Asteroid Field**, a void dimension near Mars that an Ad Astra rocket reaches; the ore becomes
yellowcake, the yellowcake fuel rods, and the **FX-1 Reactor** turns rods, water and coolant into **supercritical steam**
(power, in GT's and Extreme Reactors' turbines) and **spent fuel**, which is reprocessed into **plutonium**. Plutonium-241 is
what the **Fusion Reactor Mk1** needs, and that is what the whole update is for.

```text
Ad Astra rocket (gregified parts, Aluminised Hydrolox)
   -> Ceres, its space = the Asteroid Field (af9:asteroid_field)      station built through Ad Astra's planet menu
        -> Brannerite ore in the asteroids (GT ore veins, layer af9_asteroid)
             -> purified dust -> leach (uranyl sulfate) -> yellowcake -> reduction (EBF, hydrogen) -> uranium dust
                  -> (UF6 -> GT's enrichment -> U-235 dust)
                  -> pellets (EBF) -> fuel rod (assembler, zirconium)
                       -> FX-1 Reactor (+ water, + NaK) -> supercritical steam -> Large Steam Turbines / Extreme Reactors
                       -> spent rod -> macerator -> nitric acid -> centrifuge -> plutonium (239 and 241), uranium back
                            -> Fusion Reactor Mk1 (3 double plates of Pu-241, 4 ingots Pu-239, AF9's chips)
```

# 2. The Asteroid Field

## 2.1 Dimensions and planets (Ad Astra)

Ad Astra lists a **planet** in its menu only if it has an orbit; a pure space dimension (an orbit) is not selectable by itself.
So the field comes with a world to land on:

| Planet (`af9-core/src/main/resources/data/af9/planets/`) | Dimension | Is | Tier | Gravity | Orbit |
|---|---|---|---|---|---|
| `ceres.json` | `af9:ceres` | a small moon-like world (Ad Astra's Moon generator and sky) | 2 | 0.27 | `af9:asteroid_field` |
| `asteroid_field.json` | `af9:asteroid_field` | space (no orbit field): the asteroids, the stations | 2 | 0 | none |

The menu shows **Ceres** (tier 2: Mars's tier). *Land* goes to Ceres; *Construct Space Station* builds the station in the
field, at the chunk the rocket is over, at y = 100 (Ad Astra's rule). Falling out of the field drops a player onto Ceres (Ad
Astra's below-world rule for space dimensions).

- `af9-core/src/main/resources/data/af9/dimension/asteroid_field.json`: a noise dimension with Ad Astra's `ad_astra:orbit` noise
  settings (all air) and the biome `af9:asteroid_field`; `dimension_type/asteroid_field.json` is Mars Orbit's type (effects
  `ad_astra:mars_orbit`: the sky has Mars in it).
- `af9-core/src/main/resources/data/af9/worldgen/biome/asteroid_field.json` is Ad Astra's empty orbit biome with one feature, the
  placed feature `af9:asteroid_field` (decoration step 0, before GT's ore placement which runs at the tail of the same step).
- `af9-core/src/main/resources/data/af9/dimension/ceres.json`: `ad_astra:moon` type, noise settings and biome (a copy of the
  Moon's generator).
- The space station recipe `af9-core/src/main/resources/data/af9/recipes/asteroid_field_space_station.json`: 64 titanium
  plates, 32 stainless steel plates, 32 aluminium plates, 32 titanium rods.

`tools/lint/facts.py` (X4) checks that the planets, dimensions, the layer and the veins name each other.

## 2.2 The asteroids (`AsteroidFieldFeature`)

`af9-core/src/main/java/com/af9/core/space/AsteroidFieldFeature.java`, registered by `AF9Space`. The field is made of **clusters**: a large
island with a swarm of smaller rocks around it, and a lot of empty space between the clusters. Every chunk draws every cluster that
reaches into it and fills only its own part, so a rock comes out whole in any order of chunk generation. A cluster is fixed by the world
seed and the square *cell* of 420 blocks it belongs to (a cell holds one with a chance of 55 %, at a random place in it):

| Part | Size | Where |
|---|---|---|
| the island | radius 45-75 blocks (each axis x0.8-1.2), vertically 0.45-0.70 of that: a flattened lump | the cluster's centre |
| satellites, 12-24 | pebbles r 2-4 (28 %), small 4-8 (40 %), medium 9-16 (24 %), large 18-28 (8 %) | from the island's edge to 100 blocks beyond it, anywhere in 50 blocks above or below the island: a band of about 100 blocks |

The cluster's height is the middle of the band (y 5..270; the station is at y = 100) plus a slow noise over the plane (`DRIFT`: +-90 blocks,
features about 420 blocks wide: whole regions lie higher or lower) and a random lift (+-55), so clusters hang at all heights, not in one flat band
like the End's islands. The radius of a satellite leans to the small end (`random^1.6`), each axis is stretched by 0.75-1.25 (vertically 0.6-1.1)
and the surface of every rock is pushed in and out by two layers of simplex noise (amplitudes 0.25 and 0.10). About 2 blocks of rock in a column
of the 300-block band: mostly empty space, and the rock that there is lies together, so a vein finds an island to grow in.

The rock is andesite, tuff, basalt and blackstone, by a slow noise in patches. These are four of the stones GT has ore blocks for;
the ore layer targets exactly them (`.targets(...)` in the startup script; lint X4).

## 2.3 The ore (GT's ore veins)

- **Layer** `af9_asteroid` (`kubejs/startup_scripts/gtceu/asteroid_fission.js`): GT's world gen layer for the four stones, in
  `af9:asteroid_field` only. Ore veins of this layer grow only into those blocks.
- **Veins** (`kubejs/server_scripts/mods/gtceu/vein_asteroid.js`, `GTCEuServerEvents.oreVeins`; the file has to load after the
  pack's `mining_dim_ores.js`, which moves every vein it finds to the Mining dimension: scripts load alphabetically): one per 3 x 3 chunks, chosen by
  weight: brannerite 60 (cluster 200, density 0.55), pentlandite 20, magnetite 15 (clusters 170, density 0.5), cooperite 10 (cluster 150, density 0.45).
  Height 0-280. `discardChanceOnAirExposure(0)`: the ore may sit on the surface, in the void's face.
- GT places a vein's blocks only where there is rock, and only in chunks generated after the veins existed: a field explored before
  has none. The rock is much sparser now, so a vein gives fewer ore blocks than it did with 12 % of the band rock. **If the field turns out to be too poor or too rich**, the knobs are the clusters and densities of the veins, and
  `CLASSES` (cell sizes and counts) in the Java.
- **Brannerite** (`(U,Ca,Ce)(Ti,Fe)2O6`): dust and ore, two crushed ores per ore, by-products rutile, thorium, neodymium, GT's
  radioactive hazard x0.6. No components, so GT adds no electrolyzer or centrifuge shortcut.

# 3. Rockets and propellant

## 3.1 Gregified rockets (`kubejs/server_scripts/mods/gtceu/rockets.js`)

Ad Astra's crafting recipes for the rocket parts, the NASA Workbench, the Launch Pad and the four rockets (the workbench's own
recipes) are removed. All are assembler recipes now, with the item graph kept: nose cone, fins, engine frame, an engine and a
tank per tier that take the previous tier's, and the rocket in the NASA Workbench's 14 slots (a custom `ad_astra:nasa_workbench`
recipe `af9:nasa_workbench/tier_<n>_rocket`).

| Rocket | Metal (parts, hull blocks) | GT tier of the parts | Chip in the engine | Engine / tank | Reaches |
|---|---|---|---|---|---|
| 1 | stainless steel | HV | MCU | `steel_engine`, `steel_tank` | Moon |
| 2 | titanium | EV | ASIC | `desh_engine`, `desh_tank` | Mars, Ceres, the Asteroid Field |
| 3 | tungsten steel | IV | MRAM | `ostrum_engine`, `ostrum_tank` | Venus, Mercury |
| 4 | HSS-E | LuV | VPU | `calorite_engine`, `calorite_tank` | Glacio |

An engine: the previous engine (the frame for tier 1), two pumps, a motor, 8 plates, 4 screws, two chips, 288 mB soldering alloy.
A tank: the previous tank and a drum of the tier (tier 1: a drum and a pump), 8 plates and a fluid regulator. The nose cone takes a
sensor and two MCUs, the fins stainless steel plates and rods. (The recipes carry programmed circuits where their inputs would
overlap another assembler recipe: lint R7.)

## 3.2 Propellant

Not Ad Astra's fuel (the Fuel Refinery recipe `ad_astra:refining/fuel_from_refining_oil` is removed) and not GT's rocket fuel.

| Step | Machine, tier | Takes | Gives |
|---|---|---|---|
| Triethylaluminium | Chemical Reactor, MV | 1 aluminium dust, 3,000 mB ethylene, 1,500 mB hydrogen | 1,000 mB `gtceu:triethylaluminium` |
| Aluminised Hydrolox | Chemical Reactor, MV | 2 aluminium dust, 4,000 mB hydrogen, 2,000 mB oxygen, 400 mB triethylaluminium | 3,000 mB `gtceu:aluminised_hydrolox` |

3,000 mB is a rocket's tank and one launch (Ad Astra's rule). The fluid tags `ad_astra:tier_1..4_rocket_fuel` and
`tier_1_rover_fuel` hold nothing else (the tag script empties them first: Ad Astra's own `#ad_astra:fuel` also brings in oil fuel,
diesel and biodiesel).

# 4. The uranium chain (`kubejs/server_scripts/mods/gtceu/asteroid_fission.js`)

| Step | Machine, tier | Takes | Gives |
|---|---|---|---|
| (ore processing) | GT's: macerator, ore washer, centrifuge ... | Brannerite ore | purified Brannerite dust (and rutile, thorium, neodymium) |
| Leach | Chemical Reactor, MV | 2 Brannerite dust, 2,000 mB sulfuric acid | 1,000 mB Uranyl Sulfate Solution, rutile dust, 15 % thorium dust |
| Precipitation | Chemical Reactor, MV | 1,000 mB uranyl sulfate, 1,000 mB ammonia | 3 Yellowcake, 1,000 mB diluted sulfuric acid |
| UF6 | Chemical Reactor, MV | 3 yellowcake, 4,000 mB hydrofluoric acid, 2,000 mB fluorine | 1,000 mB uranium hexafluoride, 2,000 mB water |
| (enrichment) | GT's centrifuge and electrolyzer | UF6 | U-235 and U-238 dust |
| Reduction | Electric Blast Furnace, HV, 1,500 K | 3 yellowcake, 8,000 mB hydrogen | 6 uranium dust, 8,000 mB steam |
| Pellets | Electric Blast Furnace, HV, 1,800 K | 12 uranium dust, 4 tiny U-235 dust, 8,000 mB oxygen | 4 `kubejs:fx_fuel_pellet` |
| Fuel rod | Assembler, HV | 4 pellets, 1 zirconium ingot (the zircon chain: `docs/semiconductor-factory.md` §6.9) | `kubejs:fx_fuel_rod` |

The **reduction** is what makes the natural uranium dust of the pellets: GT's own chain only gives U-235 and U-238 dust, and with the
pitchblende and uraninite veins replaced by the asteroids nothing else did (the chain was a dead end before it). One ore is about three yellowcake and a thousand mB of UF6; GT's enrichment gives a tenth of it as U-235. A rod needs 12 uranium dust
and 4 tiny U-235 dust and the reprocessing gives back 8 and 2 x 60 %: about **4 ore per rod net**. Tiny U-235 dust also comes from
`af9:uranium_238_separation` (GT's centrifuge on uranium dust, 23 %, its tiny plutonium removed).

# 5. The FX-1 Reactor

## 5.1 Structure (`kubejs/startup_scripts/gtceu/asteroid_fission.js`)

5 x 5 x 5 (a plain GT multiblock, `gtceu:fx1_reactor`): a vessel of stable titanium casing (`gtceu:stable_machine_casing`) around a
core of heatproof casing, titanium pipe casing as the coolant channels (6) and one air block, the fuel rod's. Hatches on any
casing, maximums only: 2 energy, 1 + 1 item, 3 + 4 fluid, 1 maintenance. Overclocks like any GT multiblock (IV hatches).

## 5.2 Recipes

| Recipe | Machine, tier | Takes | Gives | Time |
|---|---|---|---|---|
| `af9:fx1_fuel_cycle` | FX-1 Reactor, EV (1,920 EU/t) | 1 fuel rod, 640 mB distilled water, 1,000 mB sodium-potassium | the spent rod, 61,440 mB supercritical steam, 1,000 mB hot sodium-potassium | 1,200 ticks |
| `af9:cool_hot_sodium_potassium` | Vacuum Freezer, HV | 1,000 mB hot sodium-potassium | 1,000 mB sodium-potassium | 100 ticks |
| `af9:supercritical_steam` | any steam turbine (GT's `steam_turbine` type), -512 EU/t | 128 mB supercritical steam | 1 mB distilled water | 20 ticks |

The coolant is GT's own **sodium-potassium** (`gtceu:sodium_potassium`, made in the chemical reactor); **hot sodium-potassium** is new.
The loop is closed: 1:1, nothing is used up.

## 5.3 Power

Supercritical steam (`gtceu:supercritical_steam`, 647 K) is **80 EU per mB** in the steam turbine recipe (GT's steam: 0.5). The
recipe's 512 EU/t is below the Large Steam Turbine's maximum (HV: 1,024 EU/t at full rotor power), so every Large Steam
Turbine runs it, with two parallels: **12.8 mB/t for 1,024 EU/t**. A rod makes 51 mB/t for 1,200 ticks (61,440 mB), enough for
**four** Large Steam Turbines at full power (4,096 EU/t); the reactor costs 1,920 EU/t, so a rod is worth about 2.4 million EU net.
Single steam turbine blocks run it only if they can carry 512 EU/t.

**Extreme Reactors** (`af9-core/src/main/java/com/af9/core/compat/extremereactors/ExtremeReactorsCompat.java`): AF9 Core registers a
vapor `supercritical_steam` (320 FE/mB: GT's 80 EU at 4 FE per EU, 32 times its steam) and maps the fluid tag
`forge:supercritical_steam` to it, by Extreme Reactors' own inter-mod messages (`fluid-register`, then `fluid-mapping-register`) and
reflection, so AF9 Core needs nothing of the mod to build. The server script puts the fluid in the tag
(`ServerEvents.tags('fluid')`).

# 6. Plutonium and the Fusion Reactor Mk1

| Step | Machine, tier | Takes | Gives |
|---|---|---|---|
| Decladding | Macerator, HV | spent fuel rod | 4 irradiated fuel dust, 1 zirconium dust |
| Dissolving | Chemical Reactor, HV | 4 irradiated fuel dust, 3,000 mB nitric acid | 3,000 mB dissolved spent fuel |
| Separation | Centrifuge, EV | 3,000 mB dissolved spent fuel | 1 plutonium dust, 8 uranium dust; chances: plutonium-241 30 %, 2 tiny U-235 60 %, neodymium 15 %, molybdenum 15 %; 2,000 mB nitric acid back |

**Closed:** GT's `centrifuge/uranium_238_separation` (tiny plutonium) and `centrifuge/plutonium_239_separation` (Pu-241), the void
miner's circuit 5 (pitchblende, uraninite) and the raw plutonium in circuit 6, and every ore vein with pitchblende or uraninite
(`modifyAll`: weight 0, wherever the pack put them). Plutonium from a naquadah vein's spread stays but can no longer be turned
into Pu-241.

**Fusion Reactor Mk1** (`kubejs/server_scripts/mods/gtceu/fusion_reactor.js`, `af9:fusion_reactor_mk1`, Assembly Line, LuV): GT's
recipe is removed by output and replaced: a superconducting coil, 4 ZPM circuits, **3 double plates of plutonium-241** (GT: 1),
**4 plutonium ingots**, 2 double osmiridium plates (GT: 1), 2 IV field generators, 64 UHPIC, 32 ITBTC wire, **8 VPU, 8 spin logic and
8 photonic IC chips**, 4 fusion glass; 2,304 mB soldering alloy and 2,304 mB niobium-titanium (GT: 1,152 each). Research: a scan of the
ITBTC wire as before. Six ingots of plutonium-241 are about **20 spent rods**.

# 7. Radiation

GT's hazard system poisons a player who carries radioactive material without a full hazmat suit. `RadiationWatch`
(`af9-core/src/main/java/com/af9/core/radiation/RadiationWatch.java`) warns before it happens: once a second, near radioactive
material, a message above the hotbar (mild, strong, dangerous) with whether the player's hazmat suit is complete. Sources: the
inventory (1 per stack, up to 2), dropped items within 6 blocks, ore blocks of radioactive materials within 3 blocks, a running
FX-1 Reactor within 16 blocks. Radioactive is GT's notion (a carcinogenic hazard that anything triggers, or an element with a
half-life) plus the item tag `#af9:radioactive` (the pellet, the rod, the spent rod). Brannerite, yellowcake and the irradiated fuel
carry GT's radioactive hazard themselves. Setting: `[radiation] hints` in `config/af9-common.toml`.

# 8. Quests

The chapter `config/ftbquests/quests/chapters/asteroid_fission.snbt` (texts `af9.quest.fx.*` in
`kubejs/assets/kubejs/lang/en_us.json`): 30 quests from the rockets to the Mk1. ATM9's Mk1 quest (`zero_point_module.snbt`) now
needs the plutonium-241 quest too.

# 9. What was not run

The Java compiles on CI and the lint suite passes, but none of this has been in a running game. Look first at:

1. the world: that `af9:asteroid_field` loads (a planet menu entry for Ceres, the station button), how the asteroids look and how
   much ore a vein leaves (§2.3);
2. GT's ore veins of the layer `af9_asteroid` show up in the JEI/EMI vein page and generate;
3. the radiation warning above the hotbar, with and without a hazmat suit (GT's hazard system must be on);
4. the FX-1 in the multiblock preview, one fuel cycle, a Large Steam Turbine on the steam;
5. Extreme Reactors, if installed: the log line `Extreme Reactors: could not ...` means the vapor was not registered;
6. the quest task "dimension" of `af9.quest.fx.field` (an FTB Quests task type).

# 10. Files

| File | Holds |
|---|---|
| `kubejs/startup_scripts/gtceu/asteroid_fission.js` | materials, items, the ore layer, the FX-1's recipe type and structure |
| `kubejs/server_scripts/mods/gtceu/asteroid_fission.js` | the ore veins, the closed old ways, the uranium chain, the reactor, steam turbine, reprocessing, tags |
| `kubejs/server_scripts/mods/gtceu/rockets.js` | the gregified rockets, the propellant, the fuel tags |
| `kubejs/server_scripts/mods/gtceu/fusion_reactor.js` | the Fusion Reactor Mk1 |
| `kubejs/server_scripts/mods/gtceu/miner.js` | the void miner without uranium and plutonium |
| `af9-core/src/main/java/com/af9/core/space/AsteroidFieldFeature.java`, `AF9Space.java` | the asteroids and their registration |
| `af9-core/src/main/java/com/af9/core/radiation/RadiationWatch.java` | the radiation warning |
| `af9-core/src/main/java/com/af9/core/compat/extremereactors/ExtremeReactorsCompat.java` | supercritical steam for Extreme Reactors |
| `af9-core/src/main/resources/data/af9/` | dimensions, dimension type, biome, features, planets, the station recipe |
| `kubejs/assets/kubejs/textures/item/fx_fuel_pellet.png`, `fx_fuel_rod.png`, `fx_spent_fuel_rod.png` | the three items |
| `config/ftbquests/quests/chapters/asteroid_fission.snbt` | the quests |
