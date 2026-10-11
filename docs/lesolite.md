# Lesolite

Lesolite (`Ls`) is the amber lunar mineral behind the EV SMD substrates. It lives in one place only: the
Moon, as a GT blob vein beside artemite. Nothing takes it yet: the SMD recipes that will need it are a
separate change, as is the quest that asks for the first raw ore.

Code: `af9-core/src/main/java/com/af9/core/registry/AF9Materials.java` (the material, `moonSilicon()`),
`kubejs/server_scripts/mods/gtceu/vein_moon.js` (the vein, with artemite's).

## 1. The material

`gtceu:lesolite`: ingot, dust and ore; crushing gives two crushed ores per raw ore; byproducts aluminium
and nickel (both already obtainable elsewhere). Formula only, no components, so GT adds no electrolyzer or
centrifuge shortcut past the ore chain. No blast furnace temperature, so it smelts in a normal furnace:
the gate is finding the ore on the Moon, not a coil tier.

Textures need no files: GT draws every material form (ore, raw, crushed, dust, ingot, block) procedurally
from the colour and the icon set. The name is `material.gtceu.lesolite` in `kubejs/assets/gtceu/lang/en_us.json`
(the assets linter requires it, `A5`).

## 2. The vein

`af9:lesolite_vein`: a standard blob in the Moon's own layer (`af9_moon`,
`af9-core/src/main/java/com/af9/core/space/AF9Space.java`), dimensions `ad_astra:moon`, height 40-120 (the
terrain band, like artemite). Solid terrain, so a blob like artemite's, not a dike (dikes are for the belts'
floating rocks: `vein_ceres.js`, `vein_field.js`).

Tuning (verify in game with the prospector): `clusterSize` 24, `density` 0.8, weight 30 (artemite's 45: a
little rarer, it only feeds the EV substrates). `af9_moon` is tier 1, so every elevator drone draws it
(`OreCatalog`), weighted by its weight.

## 3. Still open

- SMD consumers: the pack's own SMD recipes taking lesolite (foil substrates).
- Quest: nothing asks for the first raw ore yet (the Space Elevator's exotic quest is the brannerite model:
  one raw ore).
