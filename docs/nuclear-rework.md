# Nuclear rework: the molten-salt FX Reactor, the Heat Exchanger, graphite and the actinide burners

The FX-1 Reactor (`docs/asteroid-fission.md`) turns a fuel rod, distilled water and sodium-potassium into supercritical steam. This page is
the second half of the story: the **Mk2 shell** of the same reactor runs **molten-salt cycles**, a new **Heat Exchanger** turns the hot
salt into steam, a **graphite moderator** makes the thermal fuels burn, and four new **burner rods** (neptunium, americium, curium,
californium) turn the minor actinides of reprocessing into power instead of vitrified waste. Nothing of the Mk1 route changes: every
old recipe, rod and spent rod stays as it was.

Code: `kubejs/server_scripts/mods/gtceu/nuclear_rework.js` (all recipes), `kubejs/startup_scripts/gtceu/asteroid_fission.js` (the
Heat Exchanger and its recipe type), `af9-core` `AF9Materials.asteroidFission()` / `actinideDusts()` (FLiBe and the dusts) and
`AF9Items` (the burner rods and the graphite items). Textures: `tools/textures/nuclear_items.py`.

## 1. Where the idea comes from

| Source | What we took | What we left |
|---|---|---|
| **TerraFirmaGreg Modern** (`github.com/TerraFirmaGreg-Team/Modpack-Modern`, LGPL-3.0; its nuclear scripts are the `powergen/nuclear` folder of the pack's server scripts) | a graphite moderator line with a long chemistry chain; a molten-salt (FLiBe) coolant with a **heat exchanger** that turns the hot salt into power; fuel rods that burn the minor actinides one rung at a time (neptunium, americium, californium); fission waste that has a use | its reactor is the *Dea's Fission* mod (heat meter, rod NBT, cooling components), which is not in our pack; the heat-by-NBT recycling of rods; the SMR and cooling tower |
| **Star Technology** | fission is a source of the late elements, not only of power; the fission line sits between the plasma/fusion tiers and the fabs | StarT's nuclear scripts are not public (StarT-Core, `github.com/StarT-Dev-Team/StarT-Core`, has no reactor code), so nothing was copied or mirrored beyond that idea |

No code or recipe of either pack was copied: the recipes, amounts and temperatures below are AF9's own, written for the FX Reactor. TFG's
concept is credited here and in `CREDITS-textures.md`; the licence text is in `licenses/TerraFirmaGreg-Core-Modern-LGPL-3.0.txt`.

## 2. The loop

```text
Brannerite -> ... -> fuel rods                      graphite + coal tar -> green body -> baked body -> purified billet -> moderator
                        |                                                                                                     |
                        v                                                                                                     v
   FX Reactor Mk2 (heating coils in the core shell, Kanthal or better)  <-- FLiBe (lithium fluoride + beryllium fluoride)
        rod + FLiBe (+ moderator for the thermal fuels) -> spent rod + hot FLiBe
                        |
                        v
   Heat Exchanger: hot FLiBe + distilled water -> supercritical steam + FLiBe (back to the reactor)
                        |
                        v
   Large Steam Turbines / Extreme Reactors (unchanged)
```

## 3. The Mk2 molten-salt cycles (`fx1_reactor`)

The reactor is one machine. With the heatproof shell it runs the Mk1 cycles; with heating coils in the shell (the Mk2 upgrade that already
gated MOX-3 and MOX-4) it also runs the molten-salt cycles, whose recipes carry the coil temperature. They take **no water**: the
steam is made in the Heat Exchanger. `hot` is the FLiBe in and out of one cycle (a closed loop, 1:1).

| Fuel | Coils | Moderator | Time | FLiBe in/out | Steam after the exchanger | vs. the Mk1 cycle |
|---|---|---|---|---|---|---|
| Uranium (LEU) | Kanthal 2,700 K | yes | 1,200 t | 1,000 mB | 92,160 mB | 1.5x of 61,440 |
| HEU-235 | Kanthal | yes | 1,200 t | 2,000 mB | 184,320 mB | 1.5x |
| LEP-241 | Kanthal | yes | 600 t | 1,000 mB | 92,160 mB | 1.5x |
| HEP-239 | Kanthal | yes | 600 t | 2,000 mB | 184,320 mB | 1.5x |
| MOX-1 | Kanthal | yes | 1,800 t | 1,500 mB | 138,240 mB | 1.5x |
| MOX-2 | Kanthal | yes | 1,800 t | 2,000 mB | 184,320 mB | 1.5x |
| MOX-3 (EV) | Kanthal | yes | 1,800 t | 2,500 mB | 230,400 mB | 1.5x |
| MOX-4 (EV) | Nichrome 3,600 K | yes | 1,800 t | 3,000 mB | 276,480 mB | 1.5x |
| Thorium | Kanthal | yes | 2,400 t | 2,000 mB | 184,320 mB | **3x** (molten salt suits thorium) |
| Neptunium | Kanthal | no | 2,400 t | 2,000 mB | 184,320 mB | burner |
| Americium | Nichrome 3,600 K | no | 2,400 t | 3,000 mB | 276,480 mB | burner |
| Curium | Tungstensteel 4,500 K | no | 3,000 t | 5,000 mB | 460,800 mB | burner |
| Californium | HSS-G 5,400 K | no | 3,000 t | 8,000 mB | 737,280 mB | burner |

The moderator sits in the reactor's item hatch with the rod (the recipe does not use it up). The rods and spent rods are the same
items as before, so the whole reprocessing chain (decladding, nitric acid, centrifuge) is unchanged. Hatches: the reactor already takes
1 item and 3 + 4 fluid hatches; a molten-salt cycle needs one fluid input and one fluid output.

## 4. The Heat Exchanger (`gtceu:heat_exchanger`)

A 3 x 3 x 5 tower: stable titanium casing round a column of titanium pipe casing (the reactor's own two blocks), controller on the middle
of the front. Hatches on any casing, maximums only: 2 energy, 2 + 2 fluid, 1 maintenance. HV, 480 EU/t, perfect overclock.

| Recipe | Takes | Gives | Time |
|---|---|---|---|
| `af9:flibe_heat_exchange` | 1,000 mB hot FLiBe, 960 mB distilled water | 92,160 mB supercritical steam, 1,000 mB FLiBe | 400 t |

The ratio is the Mk1 cycle's (96 mB of water per 1,000 mB of steam); the salt carries 1.5 times the heat of the same rod. Controller
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

1. **Same machine, new mode.** The Mk2 shell is already the FX Reactor's upgrade path, so the molten-salt cycles are recipes of
   `fx1_reactor` gated by coil temperature. No second reactor controller; only the Heat Exchanger is new, because a reactor that makes
   steam *and* cools salt would not teach "heat goes from the core to the water in a second vessel" (TFG's lesson).
2. **No NBT, no heat meter.** TFG's reactor tracks a heat value per rod. That needs a mod AF9 does not have and Java that could not be
   tested here; the coil temperature plays the heat gate instead, and the yield is fixed per recipe (readable in JEI).
3. **The moderator is the thermal/fast split.** Uranium, plutonium and thorium are thermal fuels and need graphite; the burners are
   fast-spectrum and do not. That is real, and it keeps the item slots (rod + moderator) within the reactor's 2 item inputs.
4. **Steam multipliers.** The salt cycle makes 1.5x the steam of the water cycle (thorium 3x) for a higher price: Kanthal coils, the moderator
   chain, FLiBe, the exchanger hall. The numbers are close to the ladder the old cycles already use; they were not balanced in game.
5. **The ladder ends in waste, not in a dead end.** Californium's spent rod gives vitrified waste, so the whole chain is "burn it, then lock
   what is left"; the old vitrification recipes stay.
6. **Textures.** Recoloured from AF9's own thorium fuel textures (`tools/textures/nuclear_items.py`); graphite items are a blue-black ramp.

## 9. What was not run

The Java and the scripts have not been started in a game; the lint suite (`bash tools/lint/run.sh --selftest`) passes and CI compiles.
Look first at:

1. FLiBe and hot FLiBe as fluids (names, colours, buckets), lithium fluoride unchanged, beryllium fluoride dust;
2. curium and californium dusts exist (`actinideDusts`: if GT lacks either material the log shows no error but the last rungs have no dust);
3. the FX Reactor with coils in the core shell takes `af9:fx1_fuel_flibe_cycle` (rod + graphite moderator in the item hatch, FLiBe in the fluid
   hatch), and the Mk1 shell does not;
4. the Heat Exchanger in the multiblock preview (3 x 3 x 5, pipe column), one `af9:flibe_heat_exchange` run;
5. the burner rods in JEI (the pellets in the EBF, the rods in the assembler, the cycles in the reactor, the decladding in the macerator).
