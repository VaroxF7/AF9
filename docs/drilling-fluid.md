# Drilling Fluid

Janne's request (2026-10-23). GT's mud was one mixer recipe (stone dust, 20 mB lubricant, water). It is now a water-based
drilling mud in six steps. Code: `AF9Materials.drillingMud()`, `kubejs/server_scripts/mods/gtceu/drilling_fluid.js`,
quests in the `void_miner` chapter, JEI page `kubejs/client_scripts/mud_coolant_info.js`.

| Step | Machine | Takes | Makes |
|---|---|---|---|
| 1. soften | Mixer, LV | 4000 mB water, 1 soda ash | 4000 mB Softened Water, 1 calcite |
| 2. activate | Mixer, MV | 4 bentonite dust, 1 soda ash, 3500 mB softened water | 3500 mB Sodium Bentonite Slurry, 1 calcite |
| 3. chloroacetic acid | Chemical Reactor, HV (circuit 1) | 1000 mB acetic acid, 1000 mB chlorine | 1000 mB Chloroacetic Acid, 1000 mB HCl |
| 4. mercerise | Mixer, MV | 6 wood dust, 1 lye, 1500 mB water | 1500 mB Alkali Cellulose |
| 5. etherify | Chemical Reactor, HV | 1500 mB alkali cellulose, 750 mB chloroacetic acid, 1 lye | 1500 mB CMC Solution, 1 salt |
| 6. blend | Mixer, HV | 3500 mB slurry, 1500 mB CMC solution, 2 salt, 1 calcite, 1 lye | 5000 mB Drilling Fluid |

Real mud: sodium bentonite for body, carboxymethyl cellulose (cellulose + chloroacetic acid) for carrying power, salt as
shale inhibitor, calcite as weight, caustic soda for pH. GT's two mixer recipes are removed.

## Progression

The first thing that drinks drilling fluid is the Void Miner (2000 mB a run); GT's Large Miner (EV) is the other.
Everything above is HV or lower. The inputs: bentonite (deepslate olivine vein, mined, not void-mined), soda ash (EBF from
calcite and salt, or lye and CO2), sawdust (logs), lye and chlorine (salt water), acetic acid (bioethanol to ethylene to
acetic acid, or methanol and CO). No step needs oil, asteroids, lubricant or an ore of the Void Miner. The old recipe's
lubricant (an EV vacuum-tower product, or the brewery) is gone from the chain. Calcite and salt come back out of the chain
(one calcite and one salt more than it uses per batch).
