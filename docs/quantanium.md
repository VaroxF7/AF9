# Quantanium

Quantanium (`Qn`) is the ore that unlocks UHV. It lives in one place only: the
upper Asteroid Field, as a GT dike vein beside naquadah, platinum and iridium.
Nothing else holds it, and no recipe takes
it yet: the UHV hulls and circuits that will need it are a separate change, as
is the quest that asks for the first raw ore.

Code: `af9-core/src/main/java/com/af9/core/registry/AF9Materials.java` (the material, `quantanium()`),
`kubejs/server_scripts/mods/gtceu/vein_field.js` (the vein, with the field's
other three).

## 1. Reachability audit (was it reachable before?)

No. Before this change nothing named it anywhere: no material, no vein, no
recipe, no quest, and GT 7.2.0 has no Quantanium either (it is in none of
`tools/lint/data/gt-materials.txt`, `tools/lint/data/gt-names.txt`).

## 2. The material

`event.create('quantanium')`: ingot, dust and ore; crushing gives two crushed
ores per raw ore; byproducts titanium, trinium and niobium (all already
obtainable elsewhere). Formula only, no components, so GT adds no
electrolyzer or centrifuge shortcut past the ore chain. No blast furnace
temperature, so it smelts in a normal furnace: the gate is finding the ore,
not a coil tier.

Textures need no files: GT draws every material form (ore, raw, crushed,
dust, ingot, block) procedurally from the colour and the icon set. The name
is `material.gtceu.quantanium` in `kubejs/assets/gtceu/lang/en_us.json`
(the assets linter requires it, `A5`).

## 3. The vein

`af9:quantanium_vein`: a dike in the belts' own layer (`af9_asteroid`,
`af9-core/src/main/java/com/af9/core/space/AF9Space.java`), dimensions
`af9:asteroid_field` (the upper field only; Ceres holds the fission veins),
height 5-270 (the rocks' whole band,
`af9-core/src/main/java/com/af9/core/space/AsteroidFieldFeature.java`).

Dikes, not blobs: a standard GT blob at one random height mostly misses the
floating rocks or cuts one in a thin slab. A vertical dike pierces
every rock of its column. `discardChanceOnAirExposure` is 0.0: every asteroid
is exposed to the void, the default would eat the vein.

The file is named to load after the pack's `mining_dim_ores.js`, which moves
every GT vein to the Mining Dimension: this vein keeps the field (same reason
as `kubejs/server_scripts/mods/gtceu/vein_asteroid.js`). No biomes filter.

Tuning (verify in game with the prospector): `clusterSize` 16 fits the medium
rocks and up; `density` 1.0, so hit rock is solid ore; `weight` 30 sets both
the worldgen share and the elevator's share. Missions bring a quarter share
on top of that (the lean divisor, `docs/space-elevator.md`): a targeted
quantanium run brings 6-12 stacks, not 24-48.

## 4. The Space Elevator

`af9-core/.../elevator/OreCatalog.java` tiers a
vein by its layer: `af9_asteroid` is tier 4, so only the Mk-IV drone draws
this vein (no early-drone leak of a UHV material), weighted by its weight
(`docs/space-elevator.md`). The vein also takes Quantanium out of the exotic
pool (ores no vein holds, one Mk-IV run in six): it now has a dedicated
asteroid instead of the lottery. Quantanium is a lean ore
(`SpaceMissionMachine.LEAN_DIVISOR`): missions bring a quarter of its share.

## 5. Still open

- UHV consumers: liquid Quantanium (`gtceu:quantanium`) is used as liquid in the first UHV circuit (`wetware_processor_computer`).
- Quest: nothing asks for the first raw ore yet (the Space Elevator's exotic
  quest is the brannerite model: one raw ore).
- Bulk: the Mk-IV share follows the weight; if UHV wants stacks, raise the
  weight (it also raises the worldgen share) or add a void-miner asteroids
  circuit (`kubejs/server_scripts/mods/gtceu/miner.js`).
