# Nuclear rework: molten salt, the Heat Exchanger, graphite and the actinide burners

The reactor itself is Extreme Reactors 2 (`docs/extreme-reactors.md`; the earlier FX-1 Reactor was removed). This page is the second
half of the story: a reactor run with **FLiBe** as its coolant gives **hot FLiBe**, a new **Heat Exchanger** turns the hot salt into
steam, a **graphite moderator** (as the *Nuclear Graphite Block*) slows the neutrons better than the mod's own graphite, and four
**burner rods** (neptunium, americium, curium, californium) turn the minor actinides of reprocessing into power instead of vitrified waste.

Code: `kubejs/server_scripts/mods/gtceu/nuclear_rework.js` (all recipes), `kubejs/startup_scripts/gtceu/asteroid_fission.js` (the
Heat Exchanger and its recipe type), `af9-core` `AF9Materials.asteroidFission()` / `actinideDusts()` (FLiBe and the dusts) and
`AF9Items` (the burner rods and the graphite items). Textures: `tools/textures/nuclear_items.py`.

## 1. Where the idea comes from

| Source | What we took | What we left |
|---|---|---|
| **TerraFirmaGreg Modern** (`github.com/TerraFirmaGreg-Team/Modpack-Modern`, LGPL-3.0; its nuclear scripts are the `powergen/nuclear` folder of the pack's server scripts) | a graphite moderator line with a long chemistry chain; a molten-salt (FLiBe) coolant with a **heat exchanger** that turns the hot salt into power; fuel rods that burn the minor actinides one rung at a time (neptunium, americium, californium); fission waste that has a use | its reactor is the *Dea's Fission* mod (heat meter, rod NBT, cooling components), which is not in our pack; the heat-by-NBT recycling of rods; the SMR and cooling tower |
| **Star Technology** | fission is a source of the late elements, not only of power; the fission line sits between the plasma/fusion tiers and the fabs | StarT's nuclear scripts are not public (StarT-Core, `github.com/StarT-Dev-Team/StarT-Core`, has no reactor code), so nothing was copied or mirrored beyond that idea |

No code or recipe of either pack was copied: the recipes, amounts and temperatures below are AF9's own, written for Extreme Reactors. TFG's
concept is credited here and in `CREDITS-textures.md`; the licence text is in `licenses/TerraFirmaGreg-Core-Modern-LGPL-3.0.txt`.

## 2. The loop

```text
Brannerite -> ... -> fuel rods                      graphite + coal tar -> green body -> baked body -> purified billet -> moderator
                        |                                                                                                     |
                        v                                                                                                     v
   Extreme Reactors reactor, coolant FLiBe (lithium fluoride + beryllium fluoride), Nuclear Graphite Blocks inside
        rod (one fuel unit) + FLiBe -> spent rod + hot FLiBe
                        |
                        v
   Heat Exchanger: hot FLiBe + distilled water -> supercritical steam + FLiBe (back to the reactor)
                        |
                        v
   Large Steam Turbines / Extreme Reactors (unchanged)
```

## 3. FLiBe in Extreme Reactors

The reactor side is a ModPack API file (`config/extremereactors/modpack_api.json`, made by `tools/er/gen_modpack_api.py`): FLiBe is a coolant
(boiling point 1,400, enthalpy 9,216 per mB) that turns into the vapor *hot FLiBe*, which is `gtceu:hot_flibe`; the Nuclear Graphite Block
is a solid moderator (absorption 0.10, heat efficiency 0.60, moderation 3.0, conductivity 2.5; the mod's graphite block is 0.10 / 0.50 / 2.0 / 2.0).
Water stays the other coolant (distilled water becomes supercritical steam). The fuel numbers are in `docs/extreme-reactors.md`.

## 4. The Heat Exchanger (`gtceu:heat_exchanger`)

A 3 x 3 x 5 tower: stable titanium casing round a column of titanium pipe casing, controller on the middle
of the front. Hatches on any casing, maximums only: 2 energy, 2 + 2 fluid, 1 maintenance. HV, 480 EU/t, perfect overclock.

| Recipe | Takes | Gives | Time |
|---|---|---|---|
| `af9:flibe_heat_exchange` | 1,000 mB hot FLiBe, 960 mB distilled water | 92,160 mB supercritical steam, 1,000 mB FLiBe | 400 t |

The ratio is 96 mB of water per 100 mB of steam (the water coolant's); 1,000 mB of hot FLiBe carry the heat of 92.16 mB of steam per mB. Controller
recipe `af9:heat_exchanger`: assembler, HV, 4 titanium plates, 2 titanium fluid pipes, HV hull, 576 mB soldering alloy.

## 5. FLiBe

Lithium fluoride (`gtceu:lithium_fluoride_dust`, the lithium chain's dust; `af9:lithium_fluoride_direct`: 2 lithium + 1,000 mB fluorine) and
beryllium fluoride (new, `af9:beryllium_fluoride`: 1 beryllium + 2,000 mB fluorine), then `af9:flibe` in the EBF (1,800 K): 2 LiF + 1 BeF2
-> 3,000 mB FLiBe. `gtceu:hot_flibe` is the same salt at 1,100 K. Both are new materials (`AF9Materials`), liquids in the reactor's
circuit. One fill of the loop is enough; nothing is consumed.

## 6. The graphite moderator

| Step | Machine | Takes | Gives |
|---|---|---|---|
| green body | Mixer, HV | 8 graphite dust, 1,000 mB coal tar | 4 `af9:graphite_green_body` |
| baked body | EBF, HV, 1,800 K | 4 green bodies, 2,000 mB nitrogen | 4 `af9:graphite_baked_body` |
| billet | EBF, HV, 2,700 K | 4 baked bodies, 2,000 mB chlorine | 4 `af9:graphite_billet` (boron removed as a chloride) |
| moderator | Assembler, EV | 4 billets, 4 titanium plates | `af9:graphite_moderator` |
| Nuclear Graphite Block | crafting 2 x 2 | 4 moderators | `af9:nuclear_graphite_block` (ER moderator) |

## 7. The actinide burners

Neptunium and protactinium got dusts for the MOX and thorium reprocessing; curium and californium now get theirs too
(`AF9Materials.actinideDusts`). Each rung's pellets are made in the EBF (1,800 K, 8,000 mB oxygen, HV, 4 pellets), the rod in the assembler
(4 pellets + zirconium ingot), or both in one step in the Fuel Fabrication Bay.

| Rung | Pellet dusts | Spent rod gives (macerator, HV, or the Spent Fuel Pool) | Next rung |
|---|---|---|---|
| Neptunium | 4 neptunium + 4 uranium | 2 plutonium, zirconium; 50 % americium | americium |
| Americium | 4 americium + 4 thorium | 3 curium, zirconium; 50 % plutonium | curium |
| Curium | 4 curium + 4 plutonium | 2 curium, zirconium; 50 % californium | californium |
| Californium | 2 californium + 6 plutonium | 2 vitrified waste, zirconium; 20 % plutonium-241 | (end) |

The old route (`fission_fuels.js`) still vitrifies neptunium, americium and protactinium: a player who does not want the ladder loses
nothing. All new items carry the `#af9:radioactive` tag (the radiation warning).

## 8. Design decisions

1. **ER is the reactor.** The earlier idea (molten-salt cycles as recipes of a GT multiblock with a coil-temperature gate) is gone; Extreme Reactors
   already has a reactor with heat, fuel, coolant and moderator physics, so AF9 only adds fuels, a coolant, a vapor and a moderator to it.
2. **Only the Heat Exchanger is new**, because a reactor that makes steam *and* cools salt would not teach "heat goes from the core to the
   water in a second vessel" (TFG's lesson).
3. **The ladder ends in waste, not in a dead end.** Californium's spent rod gives vitrified waste, so the whole chain is "burn it, then lock
   what is left"; the old vitrification recipes stay.
4. **Numbers.** Fuel numbers stay in the range of Extreme Reactors' own fuels (`docs/extreme-reactors.md`); they were not balanced in game.
5. **Textures.** Recoloured from AF9's own thorium fuel textures (`tools/textures/nuclear_items.py`); graphite items are a blue-black ramp.

## 9. What was not run

The Java and the scripts have not been started in a game; the lint suite (`bash tools/lint/run.sh --selftest`) passes and CI compiles.
Look first at:

1. FLiBe and hot FLiBe as fluids (names, colours, buckets), lithium fluoride unchanged, beryllium fluoride dust;
2. curium and californium dusts exist (`actinideDusts`: if GT lacks either material the log shows no error but the last rungs have no dust);
3. an Extreme Reactors reactor with FLiBe in the coolant port and Nuclear Graphite Blocks inside: hot FLiBe comes out of the coolant port;
4. the Heat Exchanger in the multiblock preview (3 x 3 x 5, pipe column), one `af9:flibe_heat_exchange` run;
5. the burner rods in JEI (the pellets in the EBF, the rods in the assembler, the decladding in the macerator) and as fuel in the Access Port.
