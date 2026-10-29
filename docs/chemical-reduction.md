# Chemical reduction

Hydrogen reduction of GT's roasted oxides in the Chemical Reactor, and tungsten's hydrogen route. The oxide meets hydrogen, the
metal dust and water come out, and no carbon is burnt. Code: `kubejs/server_scripts/mods/gtceu/chemical_reduction.js`; the one
new material (tungsten trioxide) is in `af9-core/src/main/java/com/af9/core/registry/AF9Materials.java` (`chemicalReduction()`).

## Recipes

| Recipe | Takes | Gives | Tier |
|---|---|---|---|
| `af9:chemical_reduction/copper` | 2 cupric oxide, 1,000 mB hydrogen | 1 copper, 1,000 mB water | MV |
| `af9:chemical_reduction/cobalt` | 2 cobalt oxide, 1,000 mB hydrogen | 1 cobalt, 1,000 mB water | MV |
| `af9:chemical_reduction/lead` | 2 massicot, 1,000 mB hydrogen | 1 lead, 1,000 mB water | LV |
| `af9:chemical_reduction/nickel` | 2 garnierite, 1,000 mB hydrogen | 1 nickel, 1,000 mB water | MV |
| `af9:chemical_reduction/tin` | 3 cassiterite, 2,000 mB hydrogen | 1 tin, 2,000 mB water | MV |
| `af9:chemical_reduction/antimony` | 5 antimony trioxide, 3,000 mB hydrogen | 2 antimony, 3,000 mB water | MV |
| `af9:chemical_reduction/tungstic_acid_calcination` | 7 tungstic acid (EBF, 1,000 K) | 4 tungsten trioxide, 1,000 mB steam | HV |
| `af9:chemical_reduction/tungsten` | 4 tungsten trioxide, 3,000 mB hydrogen | 1 tungsten, 3,000 mB water | HV |
| `af9:chemical_reduction/molybdenum` | 4 molybdenum trioxide, 3,000 mB hydrogen | 1 molybdenum, 3,000 mB water | HV |

GT's one-step `tungstic_acid_electrolysis` is removed: tungsten now takes the calcination and the hydrogen step, at the same HV.
The molybdenum recipe gives the trioxide of the fab line's molybdenite roast (`alloys_af9.js`, `photolithography.js`) a way to the metal.

## What was audited and not ported

The GTNH Chemical Reactor recipes (`ChemicalRecipes.java`, `AcidRecipes.java`, the Bartworks, GT++, GoodGenerator, GTNH Lanthanides and
KekzTech loaders; 590 recipes of the two GT loaders alone) were compared with GTCEu 7.5.3's chemistry and with this pack:

* About half of the GTNH recipes already exist in GTCEu (its chemistry descends from the same lineage): acids, polymers, PGM chain, petrochemistry.
* Every other GTNH recipe whose materials GTCEu has is filler (golden apples, magma cream, ender eyes, cell shuffling) and is skipped.
* The rest needs materials that only GTNH has (Kevlar, polyurethane, NMP, cobalt naphthenate, ferrocene fuels, lanthanide salts, ...) with no
  user in this pack, or duplicates chains the pack already built its own way (silane and halosilanes, thionyl and sulfur chlorides,
  hydrazine and chloramine, acetaldehyde, butanol, zirconium and rhenium, aromatics). They are skipped, not forgotten: a chain comes
  in when something here needs its product.
* TerraFirmaGreg's other reactor scripts are tied to TFC, its planets or its nuclear/power rework and are not ported. Its chromium line
  rewrites GT's ruby and garnet processing and needs chromium(III) oxide; it is left out for now.

## Credits

The hydrogen routes follow TerraFirmaGreg-Modern's `recipes.chromium.js` and `recipes.tungsten.js` (Modpack-Modern @ 3287e4eb,
LGPL-3.0, `licenses/TerraFirmaGreg-Core-Modern-LGPL-3.0.txt`); the GTNH audit read GT5-Unofficial @ a41c4f8 (LGPL-3.0,
`licenses/GT5-Unofficial-LGPL-3.0.txt`). The recipes are rewritten for GTCEu Modern and this pack's tiers, not copied verbatim.
