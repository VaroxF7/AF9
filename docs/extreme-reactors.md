# Nuclear on Extreme Reactors

AF9's reactor side is **Extreme Reactors 2** (`bigreactors`, ATM9's version 2.0.94). Its own progression stays as it is; AF9 adds
fuels, coolants and a moderator on top of it and keeps the GT side (making fuel, reprocessing, steam turbines) where it was.

Files: `config/extremereactors/modpack_api.json` (made by `tools/er/gen_modpack_api.py`, where the fuel table lives),
`kubejs/server_scripts/mods/extremereactors/er_bridge.js` (the tags the file names, the moderator block recipe),
`af9-core` `ExtremeReactorsCompat` (supercritical steam as a vapor, unchanged), `AF9Blocks` (`nuclear_graphite_block`).
Spec of the GT side: `docs/nuclear-rework.md`, `docs/asteroid-fission.md`.

## 1. How Extreme Reactors decides what a fuel is

Everything is **tags**. An item tag is a source of a reactant (a rod in an access port = 1,000 mB of the reactant); the port ejects waste
as the *first item of the waste's tag*. AF9 gives each fuel its own tag with exactly one item, so the rod goes in and the matching
spent rod comes out, and the GT reprocessing (macerator or Spent Fuel Pool, nitric acid, centrifuge) runs on it unchanged.

## 2. The ladder (what is native, what is ours)

| Tier | Fuel | Where from | Burns as |
|---|---|---|---|
| ER native | yellorium (also **GT uranium ingots**) | yellorite ore, or the asteroid uranium chain | cyanite waste |
| ER native | blutonium (also **GT plutonium ingots**) | cyanite in ER's Reprocessor | magentite |
| ER native | verderium | Fluidizer: yellorium + blutonium | rossinite |
| AF9 | uranium rod (LEU), thorium rod | GT: asteroid uranium, pellets, rod | spent rod, then reprocessing |
| AF9 | MOX 1-2, HEU-235, LEP-241, MOX 3, HEP-239, MOX 4 | plutonium from reprocessing | spent rods |
| AF9 | neptunium, americium, curium, californium rods | minor actinides of reprocessing | the next rung's spent rod |

| AF9 | breeder blanket | depleted uranium and steel | weak fuel that breeds plutonium (then the centrifuge) |

The numbers stay in the range of ER's own fuels (reactivity / fission rate; ER: yellorium 1.05 / 0.01, blutonium 1.0871 / 0.051,
verderium 1.0984 / 0.0743):

| Fuel | Reactivity / rate | Moderation / absorption / hardness |
|---|---|---|
| blanket | 1.030 / 0.006 | 1.2 / 0.40 / 1.0 |
| LEU, thorium | 1.055 / 0.015, 1.058 / 0.012 | 1.6 / 0.52 / 1.2, 1.5 / 0.50 / 1.1 |
| MOX 1-4 | 1.070 / 0.030 to 1.095 / 0.065 | 2.0 / 0.58 / 1.8 up to 2.8 / 0.70 / 2.0 |
| HEU, LEP, HEP | 1.080 / 0.045, 1.084 / 0.050, 1.092 / 0.060 | 2.2 to 2.6 / 0.60 to 0.68 / 2.0 |
| Np, Am, Cm, Cf | 1.098 / 0.070 to 1.115 / 0.100 | 3.2 / 0.78 / 2.0 up to 3.8 / 0.90 / 2.0 |

Only the burner rods pass verderium's rate (0.0743), by design: the ladder is the late game of ER. The exact table is in
`tools/er/gen_modpack_api.py`; the output of a reactor depends on its size and rod layout, so these are first guesses.

## 3. Cooling and steam

| Coolant (in) | Boils at | Vapor (out) | Goes to |
|---|---|---|---|
| water (native) | 100 | steam (native) | Extreme Reactors' turbines |
| `gtceu:distilled_water` | 374 | `gtceu:supercritical_steam` | GT Large Steam Turbines (80 EU/mB, recipe exists) or ER turbines (320 FE/mB) |
| `gtceu:flibe` | 1,400 | `gtceu:hot_flibe` | the **Heat Exchanger** (GT): 1,000 mB hot FLiBe + 960 mB distilled water -> 92,160 mB steam + 1,000 mB FLiBe, back to the reactor's coolant port |

Enthalpy is set so the three give the same power for the same heat (water 4 per mB of 10 FE steam, supercritical steam 100 per mB of
320 FE, hot FLiBe 9,216 per mB = 92.16 mB of steam). A higher boiling point lets the reactor run hotter while cooled; supercritical
steam is the pipe-friendly 32-fold version, FLiBe the closed loop for the hottest fuels.

## 4. The moderator

`af9:nuclear_graphite_block` (4 graphite moderators, from the purified graphite chain of `docs/nuclear-rework.md` §6) is an ER solid
moderator: absorption 0.10, heat efficiency 0.60, moderation 3.0, conductivity 2.5. ER's own graphite block is 0.10 / 0.50 / 2.0 / 2.0.

## 5. Deadlock audit

Every item of the ladder was followed back to raw materials; nothing needs a later item of its own chain.

- **ER reactor parts** need yellorium or uranium ingots (controller, fuel rods), ER's own graphite ingot (smelted from coal) and
  iron/steel: yellorite ore (ER worldgen, to check in the instance) or GT uranium ingots from the asteroid chain. Either route alone is
  enough, and the asteroid chain needs rockets, not a reactor.
- **LEU rod** needs asteroid uranium dust, tiny U-235 (from uranium dust) and zirconium: no reactor needed.
- **Plutonium** (HEP, MOX, LEP) comes from reprocessing LEU spent rods, which burn in an ER reactor: a first reactor
  exists before any plutonium is needed.
- **Neptunium and americium** come from MOX reprocessing (chances), **curium** from spent americium rods, **californium** from spent
  curium rods: a strict ladder, each rung made by the one below.
- **FLiBe** needs lithium, beryllium and fluorine (all GT materials with ores or chains), the **graphite moderator** coal tar, nitrogen,
  chlorine and titanium; the **Heat Exchanger** HV parts; none of them needs a reactor.
- **Coolants and tags**: `gtceu:distilled_water`, `flibe` and `hot_flibe` are in the tags the API file names; the vapors are Extreme
  Reactors' IMC (`supercritical_steam`) and the file's (`af9_hot_flibe`).
- **Breeder blanket**: an ER fuel of its own (reactivity 1.03: slow), so it burns beside a real fuel and gives its plutonium in the centrifuge.
- The earlier FX-1 Reactor and its cycles are gone; nothing needs them.

## 6. Check in the game

1. Start log: `Processing ModPack API Wrapper config` and no warnings about unknown reactants, coolants or tags.
2. Hold an LEU rod: its tooltip says it is reactor fuel. Put one into a reactor's solid access port and run it; the waste port gives a
   spent rod.
3. A reactor with a fluid port on distilled water: supercritical steam out, a GT Large Steam Turbine on it.
4. FLiBe in a coolant port: hot FLiBe out into the Heat Exchanger and FLiBe back.
5. A Nuclear Graphite Block inside the reactor volume counts as a moderator (the controller screen shows the reactor's
   moderation).

Extreme Reactors is ZeroNoRyouki's (`github.com/ZeroNoRyouki/ExtremeReactors2`); only its documented ModPack API file and inter-mod messages are used, nothing of its code or assets is copied.
