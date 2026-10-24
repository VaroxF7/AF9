---
title: "AF9 Asteroid Fission: the Asteroid Field, Brannerite, the Extreme Reactors reactor, supercritical steam, plutonium and the Fusion Reactor Mk1"
branch: "main"
minecraft: "1.20.1"
forge: "47.4.0"
gtceu: "7.5.3 (GregTech CEu Modern)"
ad_astra: "1.15.20"
af9_core: "0.1.0 (mod_id `af9`)"
status: "Implemented. Java compiles on CI, the lint suite (run.sh --selftest) passes; nothing of it has been started in the game yet (§9 lists what to look at first)."
agent_hint: "`gtceu:` = base GregTech, AF9's materials (AF9 Core registers them into GT's registry) and what AF9 registers through GT's KubeJS (machines, recipe types). `af9:` = AF9's plain items (the fuel pellet, rod and spent rod; registered by AF9 Core), AF9 recipe ids, dimensions, planets and the Java feature. Numbers live in the scripts named in §10; the lint (`tools/lint/facts.py`, X3 and X4) holds the quest texts and the shared names to them."
---

# 1. What it is

The old way to uranium and plutonium was a vein and an electrolyzer. It is closed. Uranium comes from **Brannerite**, an ore
that generates only in the **Asteroid Field**, a void dimension near Mars that an Ad Astra rocket reaches; the ore becomes
yellowcake, the yellowcake fuel rods, and an **Extreme Reactors reactor** (`docs/extreme-reactors.md`) burns the rods and, with distilled water, turns them into **supercritical steam**
(power, in GT's and Extreme Reactors' turbines) and **spent fuel**, which is reprocessed into **plutonium**. Plutonium-241 is
what the **Fusion Reactor Mk1** needs, and that is what the whole update is for.

```text
Ad Astra rocket (gregified parts, Aluminised Hydrolox)
   -> Ceres, its space = the Asteroid Field (af9:asteroid_field)      station built through Ad Astra's planet menu
         -> Brannerite ore in the Ceres asteroids (GT dike veins)
             -> purified dust -> leach (uranyl sulfate) -> yellowcake -> reduction (EBF, hydrogen) -> uranium dust
                  -> (UF6 -> GT's enrichment -> U-235 dust)
                  -> pellets (EBF) -> fuel rod (assembler, zirconium)
                  -> HEU pellets (EBF, U + U-235) -> HEU rod -> a hotter fuel
                  -> LEP/HEP pellets (EBF, U/Pu + Pu-241) -> hot fuels, MOX-stream reprocessing
                  -> MOX pellets (EBF, Pu) -> MOX rod -> hotter fuels, Pu back and more
                  -> thorium pellets (EBF, Th + U-235 driver) -> thorium rod -> a slow fuel, Th + Pa back
                  -> blanket assemblies (assembler, depleted U) breed in a running core (an ER fuel of their own)
                       -> Extreme Reactors reactor (+ distilled water) -> supercritical steam -> Large Steam Turbines / Extreme Reactors
                       -> spent rods -> macerator -> nitric acid -> centrifuge -> plutonium (239 and 241), uranium back
                       -> minor actinides (Np, Am, Pa) locked in vitrified glass
                            -> Fusion Reactor Mk1 (3 double plates of Pu-241, 4 ingots Pu-239, AF9's chips)
```

# 2. The Asteroid Field

## 2.1 Dimensions and planets (Ad Astra)

Ad Astra lists a **planet** in its menu only if it has an orbit; a pure space dimension (an orbit) is not selectable by itself.
The two belts are different regions, not two copies:

| Planet (`af9-core/src/main/resources/data/af9/planets/`) | Dimension | Is | Tier | Gravity | Orbit |
|---|---|---|---|---|---|
| `ceres.json` | `af9:ceres` | void belt: dark volcanic asteroids (andesite, tuff, basalt, blackstone) in flat lenses, denser with smaller islands; the fission ores | 2 | 0 | `af9:asteroid_field` |
| `asteroid_field.json` | `af9:asteroid_field` | void belt: pale exotic asteroids (granite, diorite, deepslate, end stone) as spires, discs and shard clusters; the exotic ores, the stations | 2 | 0 | none |

The menu shows **Ceres** (tier 2: Mars's tier). *Land* goes to the Ceres belt; *Construct Space Station* builds the station in the
field, at the chunk the rocket is over, at y = 100 (Ad Astra's rule). Falling out of the field drops a player into the Ceres belt (Ad
Astra's below-world rule for space dimensions).

- `af9-core/src/main/resources/data/af9/dimension/asteroid_field.json`: a noise dimension with Ad Astra's `ad_astra:orbit` noise
  settings (all air) and the biome `af9:asteroid_field`; `dimension_type/asteroid_field.json` is Mars Orbit's type (effects
  `ad_astra:mars_orbit`: the sky has Mars in it).
- `af9-core/src/main/resources/data/af9/dimension/ceres.json`: the same void generator with the biome `af9:ceres`
  (a dusty brown haze instead of black); `dimension_type/ceres.json` is the same Mars-orbit type.
- `af9-core/src/main/resources/data/af9/worldgen/biome/asteroid_field.json` is Ad Astra's empty orbit biome with one feature, the
  placed feature `af9:asteroid_field` (decoration step 0, before GT's ore placement which runs at the tail of the same step).
  `worldgen/biome/ceres.json` is the same with the placed feature `af9:ceres_field`.
- Both features are `AsteroidFieldFeature` (`Belt.FIELD` and `Belt.CERES`): the Ceres belt uses denser cells (240 blocks,
  75 % hold a cluster) with smaller lens islands (30-55 blocks) in andesite, tuff, basalt and blackstone.
  The upper field keeps the wide cells (300 blocks, 65 %) with big islands (45-75 blocks) in granite, diorite, deepslate
  and end stone, shaped as spires, discs and shard clusters (`Archetype`, drawn from the cell's random) with a ridged,
  faceted surface.
- The space station recipe `af9-core/src/main/resources/data/af9/recipes/asteroid_field_space_station.json`: 64 titanium
  plates, 32 stainless steel plates, 32 aluminium plates, 32 titanium rods.

`tools/lint/facts.py` (X4) checks that the planets, dimensions, the layer and the veins name each other.

## 2.2 The asteroids (`AsteroidFieldFeature`)

`af9-core/src/main/java/com/af9/core/space/AsteroidFieldFeature.java`, registered by `AF9Space`. The field is made of **clusters**: a large
island with a swarm of smaller rocks around it, and a lot of empty space between the clusters. Every chunk draws every cluster that
reaches into it and fills only its own part, so a rock comes out whole in any order of chunk generation. A cluster is fixed by the world
seed and the square *cell* of 300 blocks it belongs to (a cell holds one with a chance of 65 %, at a random place in it):

| Part | Size | Where |
|---|---|---|
| the island | radius 45-75 blocks; a spire (40 %: 1.7-2.3x as tall as wide), a disc (35 %: 1.0-1.3x as wide, 0.22-0.35 as tall) or a shard crown (25 %) | the cluster's centre |
| satellites, 12-24 (shards +6) | pebbles r 2-4 (28 %), small 4-8 (40 %), medium 9-16 (24 %), large 18-28 (8 %) | from the island's edge to **85** blocks beyond it, anywhere in **42** blocks above or below the island (discs: a flatter swarm, x0.45): needles over spires, chips in the discs' plane, splinters round the shards |

The cluster's height is the middle of the band (y 5..270; the station is at y = 100) plus a slow noise over the plane (`DRIFT`: +-90 blocks,
features about 420 blocks wide: whole regions lie higher or lower) and a random lift (+-55), so clusters hang at all heights, not in one flat band
like the End's islands. The radius of a satellite leans to the small end (`random^1.6`); the surface of every rock is pushed in and out by two
layers of ridged simplex noise (sharp crests like cut crystal, amplitudes 0.30 and 0.12 over a 0.55 middle). About 5 blocks of rock in a column
of the 300-block band (measured over 2,000 x 2,000 blocks: 4.5-5.5): mostly empty space, and the rock that there is lies together, so a vein
finds an island to grow in. (Ceres keeps the old smooth lenses: each axis stretched 0.75-1.25, vertically 0.6-1.1, smooth noise 0.25 and 0.10.)

**Spacing.** Two things set how far apart things are, both constants in the Java:

* *between the clusters*: `CELL` and `CLUSTER_CHANCE`. They were 420 and 55 %: the next island lay 215 blocks from an island's edge on
  average, 380 for the unluckiest tenth, 3.1 clusters per km². Now 300 and 65 %: **100 blocks** on average (median; 200 for the unluckiest
  tenth), 7.2 clusters per km². (Simulated on the world's own noise, many seeds; the table below is for tuning.)
* *inside a cluster*: `SATELLITE_DISTANCE` and `SATELLITE_SPREAD_Y`. The satellites lay up to 100 blocks from the island's edge and 50 blocks
  above and below it; they lie up to 85 and 42, a volume about a third smaller.

| `CELL` | `CLUSTER_CHANCE` | clusters per km² | gap to the next island's edge, median / unluckiest tenth |
|---|---|---|---|
| 420 | 55 % (before) | 3.1 | 215 / 380 |
| 360 | 65 % | 5.0 | 156 / 268 |
| **300** | **65 %** | **7.2** | **102 / 202** |
| 280 | 75 % | 9.6 | 83 / 171 |
| 240 | 75 % | 13.0 | 53 / 129 |

A rock's shape and size do not depend on its place, so a change of the spacing moves the clusters but does not change a cluster. Chunks that
were generated before keep their old rocks; for the first look use a new world or unexplored space.

The rock comes in patches, by a slow noise. The Ceres belt is andesite, tuff, basalt and blackstone; the upper field
is granite, diorite, deepslate and end stone. All eight are stones GT has ore blocks for;
the ore layer targets exactly them (the block tag `af9:asteroid_rock`, `AF9Space`; lint X4).

## 2.3 The ore (GT dike veins, split by belt)

The ores are **GT dike veins** in the belts' own layer (`af9_asteroid`, `AF9Space`; its stones are the block tag
`af9:asteroid_rock`, all eight belt stones): vertical dikes wherever the rock is, piercing every rock of their column
at every height. Dikes, not blobs: a standard GT blob at one random height mostly misses the floating rocks or cuts
one in a thin slab, in rocks that hang anywhere in 250 blocks of height. GT resolves the ore block from the stone the
dike replaces, so the same vein form works in both palettes.

- **How:** one dike per ore (`GTDikeBlockDefinition`, y 5-270, the rocks' whole band), `clusterSize` 16 (fits the medium
  rocks and up), `density` 1.0 (rock the dike hits is solid ore), `discardChanceOnAirExposure` 0.0 (every asteroid is
  exposed to the void, the default would eat the vein), `dimensions` the belt that holds it. GT replaces the asteroid
  stone with its ore block of that stone after the feature places the rock (same decoration step, ore placement at its
  tail).
- **Ceres, the fission belt** (`vein_ceres.js`, `dimensions` `af9:ceres`): brannerite 80 (~9 % of the rock, the uranium
  ore), pentlandite 45 and magnetite 45 (~4.5 % each), cooperite 25 (~2.5 %). Oil Regolith pockets (richer here) hold
  no ore.
- **The field, the exotic belt** (`vein_field.js`, `dimensions` `af9:asteroid_field`): naquadah 65, platinum 45,
  quantanium 30 (the UHV ore, `docs/quantanium.md`), iridium 22 (a small treasure). Every dike is a single pure ore, so
  the field's naquadah leaks no plutonium: the reactor chain stays the only way to it (§6). Quantanium missions also
  bring a quarter share (the lean divisor, `docs/space-elevator.md`).
- **Weights** set both the worldgen share and the Mk-IV elevator's share (`af9_asteroid` is tier 4; naquadah, platinum
  and iridium also have lower-tier veins elsewhere, so lesser drones still find them off the belts).
- **Knobs:** the weights and `clusterSize` in the vein scripts. A GT material or ore block that is missing logs the
  script's error and that vein stays empty.
- GT's layer `af9_asteroid` covers both dimensions (lint X4 checks the tag against the feature and both dimensions
  against `data/af9/dimension`).
- `kubejs/server_scripts/mods/gtceu/vein_asteroid.js` only switches the pitchblende and uraninite veins off (weight 0)
  and makes the naquadah vein raw naquadah only.
- **Brannerite** (`(U,Ca,Ce)(Ti,Fe)2O6`): dust and ore, two crushed ores per ore, by-products thorium, neodymium and iron (no rutile: its titanium stays in the
  residue, titanium is synthetic until Venus's ilmenite), GT's radioactive hazard x0.6. No components, so GT adds no electrolyzer or centrifuge shortcut.

# 3. Rockets and propellant

## 3.1 Gregified rockets (`kubejs/server_scripts/mods/gtceu/rockets.js`)

Ad Astra's crafting recipes for the rocket parts, the NASA Workbench, the Launch Pad, the Rover and the four rockets (the
workbench's own recipes) are removed. Everything is a GT machine recipe now, **the rockets themselves too**: the item graph is kept
(nose cone, fins, engine frame, an engine and a tank per tier that take the previous tier's), the parts are assembler recipes, and
the rocket is an assembler recipe (tiers 1 to 3) or an Assembly Line recipe (tier 4). The NASA Workbench is still made but
has no recipes left (it would have none to show).

| Rocket | Metal (parts, hull blocks) | GT tier of the parts | Chip in the engine | Engine / tank | Rocket made in | Reaches |
|---|---|---|---|---|---|---|
| 1 | stainless steel | LV (expensive MV assembler) | 4x LV circuits | `steel_engine`, `steel_tank` | Assembler, MV | Moon |
| 2 | Desh (the Moon's metal; hull blocks are Ad Astra's) | HV | ASIC | `desh_engine`, `desh_tank` | Assembler, HV | Mars, Ceres, the Asteroid Field |
| 3 | tungstensteel + Ostrum (hull: 3 + 3 blocks; engine and tank: half plates of each) | EV | 2x EV circuits | `ostrum_engine`, `ostrum_tank` | Assembler, EV | Venus, Mercury |
| 4 | HSS-E | LuV | VPU | `calorite_engine`, `calorite_tank` | Assembly Line, LuV | Glacio |

The rocket (`af9:tier_<n>_rocket`): the nose cone, six hull blocks of the tier's metal, four fins, two tanks, the engine and two robot
arms of the tier (LV for tier 1); tier 1 adds 1,152 mB soldering alloy (800 ticks), tiers 2 and 3 576 mB (600 ticks); tier 4 adds four circuits of the tier and 1,152 mB
soldering alloy, and is researched on the previous rocket (a Scanner of the tier below scans it into a data stick, 1,200 ticks,
`scannerResearch`: GT's own mechanism, so a data stick is enough and the research is made once per world). The Rover
(`af9:tier_1_rover`, Assembler EV) takes a desh engine, two wheels, a radio, a large gas tank, titanium blocks and plates and two EV
motors.

An engine: the previous engine (the frame for tier 1), two pumps, a motor, 8 plates, 4 screws, two chips, 288 mB soldering alloy
(tier 1: 12 plates, 8 screws, four LV circuits, 576 mB, 600 ticks).
A tank: the previous tank and a drum of the tier (tier 1: a drum and a pump), 8 plates and a fluid regulator
(tier 1: 12 plates and 288 mB soldering alloy, 500 ticks). The nose cone takes an LV sensor and two LV circuits,
the fins stainless steel plates and rods. (The recipes carry programmed circuits where their inputs would
overlap another assembler recipe: lint R7.)

## 3.2 Propellant

Not Ad Astra's fuel (the Fuel Refinery recipe `ad_astra:refining/fuel_from_refining_oil` is removed) and not GT's rocket fuel.

| Step | Machine, tier | Takes | Gives |
|---|---|---|---|
| 1. Triethylaluminium (the igniter) | Chemical Reactor, MV, 100 ticks | 1 aluminium dust, 1,000 mB ethylene | 1,000 mB `gtceu:triethylaluminium` |
| 2. Aluminised Hydrolox | Chemical Reactor, MV, 200 ticks | 2 aluminium dust, 2,000 mB hydrogen, 1,000 mB oxygen, 500 mB triethylaluminium | 3,000 mB `gtceu:aluminised_hydrolox` |

The whole chain is these two recipes, from four base ingredients (aluminium dust, ethylene, hydrogen, oxygen):

```text
aluminium dust + ethylene  ->  Triethylaluminium
Triethylaluminium + aluminium dust + hydrogen + oxygen  ->  Aluminised Hydrolox (3,000 mB = one launch)
```

Hydrogen and oxygen are in the ratio of water (2 : 1), as in a real hydrolox engine; the aluminium powder burns in them and the
Triethylaluminium lights it. A launch costs 2.5 aluminium dust, 2,000 mB hydrogen, 1,000 mB oxygen and 500 mB ethylene. (The first
version of the chain was three inputs and 1,500 mB of hydrogen more in the igniter, and odd amounts.)

3,000 mB is a rocket's tank and one launch (Ad Astra's rule). The fluid tags `ad_astra:tier_1..4_rocket_fuel` and
`tier_1_rover_fuel` hold nothing else (the tag script empties them first: Ad Astra's own `#ad_astra:fuel` also brings in oil fuel,
diesel and biodiesel).

# 4. The uranium chain (`kubejs/server_scripts/mods/gtceu/asteroid_fission.js`)

| Step | Machine, tier | Takes | Gives |
|---|---|---|---|
| (ore processing) | GT's: macerator, ore washer, centrifuge ... | Brannerite ore | purified Brannerite dust (and thorium, neodymium, iron) |
| Leach | Chemical Reactor, MV | 2 Brannerite dust, 2,000 mB sulfuric acid | 1,000 mB Uranyl Sulfate Solution, 15 % thorium dust |
| Precipitation | Chemical Reactor, MV | 1,000 mB uranyl sulfate, 1,000 mB ammonia | 3 Yellowcake, 1,000 mB diluted sulfuric acid |
| UF6 | Chemical Reactor, MV | 3 yellowcake, 4,000 mB hydrofluoric acid, 2,000 mB fluorine | 1,000 mB uranium hexafluoride, 2,000 mB water |
| (enrichment) | GT's centrifuge and electrolyzer | UF6 | U-235 and U-238 dust |
| Reduction | Electric Blast Furnace, HV, 1,500 K | 3 yellowcake, 8,000 mB hydrogen | 6 uranium dust, 8,000 mB steam |
| Pellets | Electric Blast Furnace, HV, 1,800 K | 12 uranium dust, 4 tiny U-235 dust, 8,000 mB oxygen | 4 `af9:fx_fuel_pellet` |
| Fuel rod | Assembler, HV | 4 pellets, 1 zirconium ingot (the zircon chain: `docs/semiconductor-factory.md` §6.9) | `af9:fx_fuel_rod` |
| MOX pellets | Electric Blast Furnace, HV, 1,800 K | 6 uranium dust, 2 plutonium dust, 8,000 mB oxygen | 4 `af9:fx_mox_pellet` |
| MOX rod | Assembler, HV | 4 MOX pellets, 1 zirconium ingot | `af9:fx_mox_rod` |
| Thorium pellets | Electric Blast Furnace, HV, 1,800 K | 10 thorium dust, 2 tiny U-235 dust (driver), 8,000 mB oxygen | 4 `af9:fx_thorium_pellet` |
| Thorium rod | Assembler, HV | 4 thorium pellets, 1 zirconium ingot | `af9:fx_thorium_rod` |
| Blanket assembly | Assembler, HV | 8 uranium dust, 2 steel plates | `af9:fx_blanket_assembly` |
| HEU-235 pellets | Electric Blast Furnace, HV, 1,800 K | 8 uranium dust, 4 U-235 dust, 8,000 mB oxygen | 4 `af9:fx_heu_pellet` |
| HEU-235 rod | Assembler, HV | 4 HEU pellets, 1 zirconium ingot | `af9:fx_heu_rod` |
| LEP-241 pellets | Electric Blast Furnace, HV, 1,800 K | 6 uranium dust, 2 Pu-241 dust, 8,000 mB oxygen | 4 `af9:fx_lep_pellet` |
| LEP-241 rod | Assembler, HV | 4 LEP pellets, 1 zirconium ingot | `af9:fx_lep_rod` |
| HEP-239 pellets | Electric Blast Furnace, HV, 1,800 K | 6 plutonium dust, 2 Pu-241 dust, 8,000 mB oxygen | 4 `af9:fx_hep_pellet` |
| HEP-239 rod | Assembler, HV | 4 HEP pellets, 1 zirconium ingot | `af9:fx_hep_rod` |

The **reduction** is what makes the natural uranium dust of the pellets: GT's own chain only gives U-235 and U-238 dust, and with the
pitchblende and uraninite veins replaced by the asteroids nothing else did (the chain was a dead end before it). One ore is about three yellowcake and a thousand mB of UF6; GT's enrichment gives a tenth of it as U-235. A rod needs 12 uranium dust
and 4 tiny U-235 dust and the reprocessing gives back 8 and 2 x 60 %: about **4 ore per rod net**. Tiny U-235 dust also comes from
`af9:uranium_238_separation` (GT's centrifuge on uranium dust, 23 %, its tiny plutonium removed).

# 5. The reactor: Extreme Reactors

The FX-1 Reactor that stood here is gone: the reactor side is **Extreme Reactors 2** (`docs/extreme-reactors.md`), and the
rods of this chapter are its fuels. A rod in an Access Port is one fuel unit (1,000 mB, like an ingot) and comes out as a spent
rod; the numbers of every fuel (reactivity, fission rate, moderation) sit in the same range as the mod's own yellorium, blutonium
and verderium, in `tools/er/gen_modpack_api.py`.

## 5.1 Recipes kept here

| Recipe | Machine, tier | Takes | Gives | Time |
|---|---|---|---|---|
| `af9:supercritical_steam` | any steam turbine (GT's `steam_turbine` type), -512 EU/t | 128 mB supercritical steam | 1 mB distilled water | 20 ticks |

Distilled water is the reactor's coolant (Extreme Reactors turns it into `gtceu:supercritical_steam`, registered by AF9 Core); the
molten-salt route (FLiBe and the Heat Exchanger) and the graphite moderator are in `docs/nuclear-rework.md`.

## 5.2 Power

Supercritical steam (`gtceu:supercritical_steam`, 647 K) is **80 EU per mB** in the steam turbine recipe (GT's steam: 0.5). The
recipe's 512 EU/t is below the Large Steam Turbine's maximum (HV: 1,024 EU/t at full rotor power), so every Large Steam
Turbine runs it, with two parallels: **12.8 mB/t for 1,024 EU/t**. A rod makes 51 mB/t for 1,200 ticks (61,440 mB), enough for
**four** Large Steam Turbines at full power (4,096 EU/t); the reactor costs 480 EU/t, so a rod is worth about 4.3 million EU net —
the EV baseload. The enriched rungs trade fissile cost for rate: HEU doubles the LEU rate (8 turbines), LEP burns LEU energy in
600 ticks, HEP burns HEU energy in 600 ticks (16 turbines).
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
| HEU decladding | Macerator, HV | spent HEU rod | 4 irradiated fuel dust, 1 zirconium dust (rides the uranium stream) |
| LEP/HEP decladding | Macerator, HV | spent LEP/HEP rod | 4 irradiated MOX dust, 1 zirconium dust (ride the MOX stream) |
| Dissolving | Chemical Reactor, HV | 4 irradiated fuel dust, 3,000 mB nitric acid | 3,000 mB dissolved spent fuel, 500 mB fission off-gas |
| Separation | Centrifuge, EV | 3,000 mB dissolved spent fuel | 1 plutonium dust, 8 uranium dust; chances: plutonium-241 30 %, 2 tiny U-235 60 %, neodymium 15 %, molybdenum 15 %; 2,000 mB nitric acid back |
| Off-gas split | Distillation Tower, HV | 1,000 mB fission off-gas | 600 mB krypton, 200 mB xenon (boule melts, cryogenics) |
| MOX decladding | Macerator, HV | spent MOX rod | 4 irradiated MOX dust, 1 zirconium dust |
| MOX dissolving | Chemical Reactor, HV | 4 irradiated MOX dust, 3,000 mB nitric acid | 3,000 mB spent MOX solution, 800 mB fission off-gas |
| MOX separation | Centrifuge, EV | 3,000 mB spent MOX solution | 2 plutonium dust, 4 uranium dust; chances: plutonium-241 50 %, neptunium 20 %, americium 8 %; 2,000 mB nitric acid back |
| Thorium decladding | Macerator, HV | spent thorium rod | 6 thorium dust, 1 zirconium dust; chances: protactinium 30 %, tiny U-235 20 % |
| Blanket separation | Centrifuge, EV | spent blanket | 2 plutonium dust, 4 uranium dust, 2 steel dust |
| Vitrification | Mixer, HV | Np/Am/Pa dust, 4 silica, 2 lead each | 4 vitrified waste (per minor actinide; permanently safe) |

**Closed:** GT's `centrifuge/uranium_238_separation` (tiny plutonium) and `centrifuge/plutonium_239_separation` (Pu-241), the void
miner's circuit 5 (pitchblende, uraninite) and the raw plutonium in circuit 6, and every ore vein with pitchblende, uraninite
or naquadah (`modifyAll`: weight 0, wherever the pack put them — GT's **naquadah vein** (`gtceu:naquadah_vein`) held plutonium
besides the naquadah). The upper field's dike (`vein_field.js`) is the only naquadah left in worldgen, and it is pure
naquadah, so the reactor chain stays the only way to plutonium. Naquadah stays renewable three ways: the void miner (End
and Asteroids modes, standing in the dimension), the space elevator (the belt veins are tier 4: the Mk-IV drone), and the
electric implosion of U-235 (4 dust + 2,000 mB Lunar Acid Vapor -> 1 naquadah dust, EV; `docs/electric-implosion.md`).
Plutonium that is already in the world from the old vein can no longer be turned into Pu-241.

**Fusion Reactor Mk1** (`kubejs/server_scripts/mods/gtceu/fusion_reactor.js`, `af9:fusion_reactor_mk1`, Assembly Line, LuV): GT's
recipe is removed by output and replaced: a superconducting coil, 4 ZPM circuits, **3 double plates of plutonium-241** (GT: 1),
**4 plutonium ingots**, 2 double osmiridium plates (GT: 1), 2 IV field generators, 64 UHPIC, 32 ITBTC wire, **8 VPU, 8 spin logic and
8 photonic IC chips**, 4 fusion glass; 2,304 mB soldering alloy and 2,304 mB niobium-titanium (GT: 1,152 each). Research: a scan of the
ITBTC wire as before. Six ingots of plutonium-241 are about **20 spent rods**.

# 7. Radiation

GT's hazard system poisons a player who carries radioactive material without a full hazmat suit. `RadiationWatch`
(`af9-core/src/main/java/com/af9/core/radiation/RadiationWatch.java`) warns before it happens: once a second, near radioactive
material, a message above the hotbar (mild, strong, dangerous) with whether the player's hazmat suit is complete. Sources: the
inventory (1 per stack, up to 2), dropped items within 6 blocks, ore blocks of radioactive materials within 3 blocks. Radioactive is GT's notion (a carcinogenic hazard that anything triggers, or an element with a
half-life) plus the item tag `#af9:radioactive` (the pellet, the rod, the spent rod). Brannerite, yellowcake and the irradiated fuel
carry GT's radioactive hazard themselves. Setting: `[radiation] hints` in `config/af9-common.toml`.

# 8. Quests

The chapter `config/ftbquests/quests/chapters/asteroid_fission.snbt` (texts `af9.quest.fx.*` in
`kubejs/assets/kubejs/lang/en_us.json`): from the rockets (all four, and the Rover) through the fuel ladder
(LEU, HEU, MOX, LEP, HEP, thorium, breeder) to the Mk1, including the HEU/LEP/HEP fuel quests. The EV chapter
carries the power side of the same story (texts `af9.quest.ev.*`: the reactor,
LEU fuel, the turbine hall, reprocessing, waste). ATM9's Mk1 quest (`zero_point_module.snbt`) now
needs the plutonium-241 quest too.

# 9. What was not run

The Java compiles on CI and the lint suite passes, but none of this has been in a running game. Look first at:

1. the world: that `af9:asteroid_field` loads (a planet menu entry for Ceres, the station button), how the asteroids look and how
   much ore a vein leaves (§2.3);
2. the dikes in the rock (brannerite and the metals in Ceres; naquadah, platinum, quantanium and iridium in the
   field) piercing the islands top to bottom, at every height, not in slabs;
3. the radiation warning above the hotbar, with and without a hazmat suit (GT's hazard system must be on);
4. an Extreme Reactors reactor burning an AF9 rod, a Large Steam Turbine on the steam;
5. Extreme Reactors, if installed: the log line `Extreme Reactors: could not ...` means the vapor was not registered;
6. the quest task "dimension" of `af9.quest.fx.field` (an FTB Quests task type);
7. how the field looks with the closer clusters and satellites (§2.2: `CELL`, `CLUSTER_CHANCE`,
   `SATELLITE_DISTANCE`, `SATELLITE_SPREAD_Y`);
8. the Assembly Line rocket recipes (tiers 3 and 4): the recipe in the Assembly Line preview, the research of the tier 2 rocket in
   the Scanner.

# 10. Files

| File | Holds |
|---|---|
| `kubejs/startup_scripts/gtceu/asteroid_fission.js` | the recipe types and structures of the Heat Exchanger, Spent Fuel Pool and Fuel Fabrication Bay |
| `af9-core/src/main/java/com/af9/core/registry/` | the materials (`AF9Materials`, `asteroidFission()`), the fuel pellet and rods (`AF9Items`) |
| `af9-core/src/main/java/com/af9/core/space/AF9Space.java` | the ore layer `af9_asteroid` and the tag of its stones |
| `kubejs/server_scripts/mods/gtceu/asteroid_fission.js` | the closed old ways, the uranium chain, the reactor, steam turbine, reprocessing, tags |
| `kubejs/server_scripts/mods/gtceu/fission_fuels.js` | MOX, thorium and breeder fuels, HEU/LEP/HEP enriched fuels, minor actinides, vitrification |
| `kubejs/server_scripts/mods/gtceu/rockets.js` | the gregified rockets, the propellant, the fuel tags |
| `kubejs/server_scripts/mods/gtceu/fusion_reactor.js` | the Fusion Reactor Mk1 |
| `kubejs/server_scripts/mods/gtceu/miner.js` | the void miner without uranium and plutonium |
| `af9-core/src/main/java/com/af9/core/space/AsteroidFieldFeature.java`, `AF9Space.java` | the asteroids of both belts and their registration |
| `kubejs/server_scripts/mods/gtceu/vein_ceres.js` | the brannerite, pentlandite, magnetite and cooperite dike veins of Ceres |
| `kubejs/server_scripts/mods/gtceu/vein_field.js` | the naquadah, platinum, quantanium and iridium dike veins of the upper field |
| `af9-core/src/main/java/com/af9/core/radiation/RadiationWatch.java` | the radiation warning |
| `af9-core/src/main/java/com/af9/core/compat/extremereactors/ExtremeReactorsCompat.java` | supercritical steam for Extreme Reactors |
| `af9-core/src/main/resources/data/af9/` | dimensions, dimension type, biome, features, planets, the station recipe |
| `af9-core/src/main/resources/assets/af9/textures/item/fx_fuel_pellet.png`, `fx_fuel_rod.png`, `fx_spent_fuel_rod.png` | the uranium items; MOX and thorium have their own, HEU reuses the uranium ones, LEP/HEP the MOX ones |
| `config/ftbquests/quests/chapters/asteroid_fission.snbt` | the quests |

# 7. The planet metals (`planet_metals.js`, `vein_planets.js`)

Each rocket tier flies on the metal of the world before it, and titanium and tungsten start on the planets that need them:

| Metal | Source | Chain |
|---|---|---|
| **Desh** (AF9 material `desh`: plates, rods, bolts, screws) | Moon Desh Ore, Ad Astra's raw desh; macerator to 2 GT dust, furnace to ingot; the two ingots (Ad Astra's, GT's) swap by a shapeless recipe | tier 2 rocket parts (HV) |
| **Titanium** | **Venus ilmenite** (the vein `af9:venus_ilmenite_vein`); before the tier 3 rocket synthetic: lunar regolith (HV) or the red mud of bauxite (EV), docs/titanium-synthetic.md; GT's chain on from rutile (TiCl4, Kroll) | tier 3 rocket, EV machines, the Void Miner MK2 |
| **Tungsten** | **Venus Sand** only: washing (a chance of scheelite) or the acid leach (2 scheelite per 4 sand); GT's chain on from there (tungstic acid, electrolysis) | tier 4 parts, IV and up |

The zircon vein generates on Mars (`af9_mars` layer, `vein_mars.js`; it used to be in the Mining Dimension); the only ilmenite vein (`af9:venus_ilmenite_vein`, ilmenite 5 + iron 2, y 40-120) is on Venus (`af9_venus` layer, `vein_venus.js`), and Mars has none. Rutile is a by-product of ilmenite alone: bauxite, zircon and every other ore lost it; the synthetic chain makes rutile dust directly (docs/titanium-synthetic.md).

Closed on Earth: the GT veins holding ilmenite, rutile, scheelite or tungstate (weight 0, by id or by material, AF9's own veins
spared), the zircon dike's ilmenite (zircon and monazite only now), the Void Miner's End mode (bauxite without ilmenite, no scheelite
mode), the bauxite electrolysis and sludge centrifuging and end stone's tungstate (rutile and tungstate are gone from their outputs),
and brannerite's rutile (leach and ore by-product). The Void Miner MK1 uses stainless plates, MK2 titanium. Kept on purpose: chromite,
quartzite, bauxite, magnesite and olivine, and salt (stainless steel, sensors, aluminium, the magnesium loop, sodium and chlorine).
The 200 nm prints (and the ASIC, so the tier 2 rocket) need propylene oxide before Mars: the chlorohydrin route makes it from
propene, chlorine and water; the titania HPPO route stays the clean upgrade.
