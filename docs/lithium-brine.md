# Lithium brine: salar water to battery chemicals

Like Mekanism's brine and old GT's (and GTNH's) lithium chains, but through real salar chemistry:
pump the brine, evaporate it in ponds, lime out the magnesium and calcium, crystallize lithium
chloride, then build the battery chemicals off it: carbonate, hydroxide, oxide, fluoride and the
Li-ion electrolyte salt LiPF6. MV to LuV.

Code: `kubejs/server_scripts/mods/gtceu/lithium_brine.js`.
Materials: AF9 Core `AF9Materials.lithiumBrine()` (the two brines, Li2CO3, LiOH, Li2O, LiF, LiPF6);
everything else is GT's own (halite, soda ash, slaked lime, borax, HF, P2O5).

## The line

```text
salt water --(MV Filtration Plant, filter kept, circuit 5)--> lithium brine + halite + salt
  --(HV Distillery, the evaporation ponds)--> concentrated brine + borax (boron recovery)
  --(HV Chemical Reactor: slaked lime + CO2)--> lithium chloride + limestone cake + water
  --(EV LCR: soda ash)--> lithium carbonate + salt            [2 LiCl + Na2CO3 -> Li2CO3 + 2 NaCl]
  --(EV Chemical Reactor: slaked lime)--> lithium hydroxide + limestone   [causticization]
  +--(IV Pyrolyse Oven, under nitrogen)--> lithium oxide + CO2 (ceramics)
  +--(IV LCR: hydrofluoric acid)--> lithium fluoride + water  [LiOH + HF -> LiF + H2O]
       --(LuV LCR: P2O5 + HF)--> lithium hexafluorophosphate (the electrolyte salt)
```

GT's own electrolyzer splits the chloride to lithium metal, which the SAW filters'
lithium niobate is cut from (`litho_process.js`).

## Notes for changing it

- The liming step merges Mg/Ca removal the way the plants report it: magnesium co-precipitates
  with the limestone filter cake (read as the calcium carbonate output). A purist's split would
  add a magnesium hydroxide material and a second filtration.
- The LuV step is a one-pot game version of the industrial PF5 route
  (PCl5 + LiF chemistry folded into P2O5 + LiF + HF). Stoichiometry is illustrative, not balanced.
- Yields improve with voltage the way the filtration plant's water runs do
  (MV 60% -> EV 93% clean water); brine follows the same courtesy.
