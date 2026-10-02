# Platinum group metals

A refinery line that parts platinum, palladium, gold, rhodium, ruthenium, iridium and osmium, step by step, the way real PGM
refineries do and the way GTNH's platline does. It runs on GT's own machines (Electric Blast Furnace, Chemical Reactor, Electrolyzer,
Distillation Tower); there is no new machine.

Code: `kubejs/startup_scripts/gtceu/platinum_group.js` (the 23 materials), `kubejs/server_scripts/mods/gtceu/platinum_group.js` (the
recipes). Quests: `config/ftbquests/quests/chapters/platinum_group.snbt`.

**Nothing existing is changed.** GT's platinum group chain (`platinum_group_sludge`, aqua regia, the centrifuge, `inert_metal_mixture`,
`rarest_metal_mixture`) and everything that uses these metals work as before. The line gives the same metals by another route, and
GT's sludge is one more feed of it. The tiers are GT's own: platinum and palladium from HV, ruthenium and rhodium from EV, osmium and
iridium from IV.

## 1. The chain

```
purified ore (cooperite, pentlandite, chalcopyrite, bornite, chalcocite, tetrahedrite)
  -> EBF, HV, 1,800 K: ore + silica flux + oxygen -> PGM Matte, dark ash, sulfur dioxide
PGM Matte (the converter matte)
  -> chemical reactor, HV: + sulfuric acid + oxygen -> PGM Leach Residue; nickel and copper solutions (electrolyzed to dust)
PGM Leach Residue    <- also: 3 GT platinum group sludge + 1,000 mB HCl, chemical reactor
  -> chemical reactor, HV: + HCl + chlorine -> Chloride Liquor (Pt, Pd, Au) + PGM Insoluble Residue (Rh, Ir, Ru, Os)

Liquor:   + SO2                       -> gold dust, Gold-Free Liquor
          + ammonium chloride          -> (NH4)2PtCl6 and Palladium Filtrate
          (NH4)2PtCl6   EBF 1,200 K    -> platinum dust, ammonium chloride, HCl
          filtrate + NH3, then + HCl   -> Pd(NH3)2Cl2
          Pd(NH3)2Cl2 + H2, EBF 900 K  -> palladium dust, ammonium chloride

Residue:  + NaOH + saltpeter, EBF 1,500 K -> Fusion Cake
          + water                          -> Ruthenate-Osmate Liquor and Rhodium-Iridium Oxide
          liquor + chlorine (EV)           -> RuO4 / OsO4 vapour
          distillation tower (EV)          -> RuO4 vapour (bp 40 C), OsO4 vapour (bp 130 C)
          RuO4 + HCl + NH4Cl (EV)          -> (NH4)2RuCl6;  + H2, EBF 1,200 K -> ruthenium dust
          OsO4 + HCl + ethanol + NH4Cl (IV)-> (NH4)2OsCl6;  + H2, EBF 1,300 K -> osmium dust
          oxide + HCl + chlorine (EV)      -> Rhodium-Iridium Chloride Liquor
          + NH4Cl + HNO3 (EV)              -> (NH4)2IrCl6 and Rhodium Chloride Filtrate
          (NH4)2IrCl6 + H2, EBF 1,300 K    -> iridium dust
          filtrate + NaNO2 (EV)            -> Na3Rh(NO2)6;  + HCl -> RhCl3 solution
          RhCl3 + zinc (EV)                -> rhodium dust, zinc chloride (electrolyzed back to zinc and chlorine)
```

## 2. Yield

One batch is 8 leach residue, which is 4 cooperite or 8 sulfide ore:

| Metal | per batch | GT's line, the same ore (16 sludge) |
|---|---|---|
| Platinum | 4 | 2.7 |
| Palladium | 3 | 1.6 |
| Gold | 1 | (none) |
| Rhodium, ruthenium, iridium, osmium | 1 each | about a third to a half each |

Ammonium chloride (the 8 of the platinum step; the palladium step gives 6 back, the platinum calcination 2), hydrochloric acid, zinc
and part of the chlorine come back. What is consumed: ore, flux, oxygen, hydrogen, sulfuric acid, ammonia, nitric acid, sodium hydroxide,
saltpeter, sodium nitrite and ethanol.

## 3. Materials

Only formulas, no components, so GT adds no electrolyzer or centrifuge shortcut that skips a step. Names are in
`kubejs/assets/gtceu/lang/en_us.json` (`material.gtceu.<id>`).

The two vapours' ids end in `_vapour` (`ruthenium_tetroxide_vapour`, `osmium_tetroxide_vapour`, `platinum_group_tetroxide_vapour`):
GT's `ruthenium_tetroxide` and `osmium_tetroxide` are dusts and stay GT's.
