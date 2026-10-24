# The 15 new alloys

Janne's pick from the 50 proposed (2026-10-16): duralumin, monel, vanadium spring steel, cupro-beryllium, zincarry (amalgam of
cinnabar), wolfram invar, rheniloy (rhenium superalloy), albeme (beryllium aluminium), cobalt-molybdenum supersteel, titanium
aluminide, hafnitinol, chronium alloy, stellarium, triniar (trinium-stellarite) and pulsium (neutron star alloy).

Code: `AF9Materials.alloys()` (materials), `kubejs/server_scripts/mods/gtceu/alloys_af9.js` (recipes), `vein_ceres.js` (chronite vein),
`miner.js` (asteroid void miner circuit 8), `space_elevator.js` (one more Mk-IV asteroid).

| Alloy | Mix | Melt | Blast |
|---|---|---|---|
| Duralumin | 9 Al, 1 Cu, 1 Mg | EBF | 1,200 K |
| Monel | 2 Ni, 1 Cu | EBF | 1,750 K |
| Vanadium Spring Steel | 8 steel, 1 V, 1 Cr | EBF | 1,900 K |
| Cupro-Beryllium | 24 Cu, 1 Be | EBF | 1,500 K |
| Zincarry | 4 Zn, 1 Ag, 1,000 mB mercury | mixer only, a paste | |
| Wolfram Invar | 6 invar, 1 W | EBF | 2,300 K |
| Rheniloy | 8 Ni3Re master alloy, 1 Ta, 1 W | EBF | 3,000 K |
| Albeme | 3 Al, 2 Be | EBF | 1,800 K |
| Cobalt-Molybdenum Supersteel | 8 steel, 2 Co, 1 Mo | EBF | 2,600 K |
| Titanium Aluminide | 1 Ti, 1 Al | EBF | 3,200 K |
| Hafnitinol | 4 Ni, 4 Ti, 1 Hf | EBF | 3,500 K |
| Chronium Alloy | 3 ceranium, 1 chronium | Sanguinite Hearth, 6,000 K, xenon coolant | no blast property |
| Triniar | 5 trinium, 2 stellarite, 1 naquadria | Sanguinite Hearth, 6,000 K, xenon coolant | no blast property |
| Stellarium | 5 iron plasma, 2 stellarite, 1 quantanium | Plasma Forge, UHV | no blast property |
| Pulsium | 4 stellarium, 2 neutronite shard + strange matter and chromodynium plasma | Plasma Forge, UHV | no blast property |

## Chains

* **Rhenium**: the molybdenite roast (EBF, oxygen) catches 25 % of a Re2O7 flue dust per batch next to the MoO3 -> ammonia and water
  make ammonium perrhenate -> hydrogen reduction at 1,200 K -> rhenium; 2 Re + 6 Ni in hydrogen at 2,000 K give the Ni3Re master alloy
  the rheniloy melt starts from.
* **Hafnium**: the hafnium tetrachloride the zircon line distils (`electronics_metallurgy.js`) is reduced with magnesium (Kroll), giving
  hafnium and magnesium chloride.
* **Chronium**: chronite (Ceres dike vein, weight 15, void miner asteroids circuit 8) -> sulfuric acid leach (niobium stays) -> soda
  ash precipitates the carbonate -> calcination to the oxide mix -> calcium reduction under argon -> chronium.
* **Stellarite and neutronite shard** have no vein: they come only from the Space Elevator's exotic asteroid (Mk-IV).
  The neutronite shard is also the **MK3 Void Miner's** asteroid circuit 9 (UV, 20 stacks of raw shard at MK1 counts, tripled by the MK3; MK1/MK2 have no circuit 9),
  and a lean ore of the elevator (half share, `SpaceMissionMachine.LEAN_DIVISOR`). Its ore byproducts are naquadria, trinium and osmium: **no neutronium** from the
  ore, the Planetary Satellite stays the only neutronium source.
* **Beryllium, vanadium, cobalt, tantalum, mercury** are GT's own (vanadium magnetite, beryl, cinnabar and so on).

## Not done yet (to decide)

* **Uses**: no machine part or circuit takes these alloys yet; each needs a place in an existing recipe (the unlock column of the
  proposal). Ask which to wire first.
* **Heat treatment** (quench, temper, age) is not a recipe step; the process depth is in the chains, the hot ingots (blast above
  1,750 K) are cooled in the Vacuum Freezer by GT.
* **Rhenium and hafnium** were bare elements in GT; `ensureMetal` gives them dust, ingot and an EBF melt (zirconium's way).
* Numbers (yields, times, vein weight) are first guesses to tune with a prospector.
