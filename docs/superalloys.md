# Superalloys

Janne's list (2026-10-18): Inconel 718, Inconel 625, Havar, Kovar, Inconel 738, Hastelloy, Stellite 1, Stellite 12. Every one has
plate, double plate, dense plate, fine wire (with foil), screw (with bolt), rod, ring and a turbine rotor with its own stats, and a
texture set of its own (all parts painted in the alloy's palette and grain).

Code: `AF9Materials.superalloys()` (new alloys, GT's Hastelloy C-276), `AF9Materials.electronicsMetallurgy()` (Kovar),
`af9-core/src/main/java/com/af9/core/recipes/gen/GenAlloysAf9.java` (mixes), `tools/textures/superalloys.py` (textures).

## What was new and what was reused

* **Kovar** existed (the MV circuit pins, `circuits_af9.js`): same id, mix and blast property, plus the parts and a rotor, and a new look.
* **Hastelloy** is GT's GCYM **Hastelloy C-276** (red, 3,800 K, EV, alloy blast smelter and EBF from GT): its recipes and blast property
  are kept; it gets dense plate, fine wire, screw, rotor and a look. Hastelloy X (GT) is untouched.
* **Stellite 1** and **Stellite 12** are new next to GT's Stellite 100 (an iron stellite, untouched).
* **Inconel 625, 718, 738** and **Havar** are new.

## Compositions and melting

A mixer makes 20 dust of the alloy (GT's mixture units), GT's EBF recipe (from the blast property) melts it. Real mass percentages in
brackets, rounded to the 20 parts.

| Alloy | Mix (20 parts) | Real alloy | EBF | Coil | Gas |
|---|---|---|---|---|---|
| Kovar | 6 Fe, 3 Ni, 2 Co (circuits_af9.js) | Fe 54, Ni 29, Co 17 | 1,720 K, MV | Cupronickel | nitrogen |
| Havar | 8 Co, 4 Cr, 3 Ni, 3 Fe, 2 W | Co 42, Cr 20, Ni 13, Fe 18, W 3, Mo 2 | 2,900 K, HV | Nichrome (above Kanthal's 2,700 K) | argon |
| Stellite 1 | 10 Co, 6 Cr, 3 W, 1 C | Co 50, Cr 31, W 12.5, C 2.5 | 3,100 K, HV | Nichrome | argon |
| Inconel 625 | 12 Ni, 4 Cr, 2 Mo, 1 Fe, 1 Nb | Ni 58+, Cr 21, Mo 9, Nb 3.6, Fe 5 | 3,300 K, HV | Nichrome | argon |
| Stellite 12 | 11 Co, 6 Cr, 2 W, 1 C | Co 56, Cr 30, W 8.5, C 1.4 | 3,400 K, EV | Nichrome | argon |
| Inconel 718 | 10 Ni, 4 Cr, 4 Fe, 1 Nb, 1 Mo | Ni 52, Cr 19, Fe 18, Nb 5, Mo 3, Ti 1 | 3,600 K, EV | Nichrome | helium |
| Hastelloy C-276 | GT: 12 Ni, 8 Mo, 7 Cr, 1 W, 1 Co, 1 Cu | Ni 57, Mo 16, Cr 15.5, Fe 5, W 4 | 3,800 K, EV (GT) | RTM alloy | helium |
| Inconel 738 | 15 nichrome (12 Ni, 3 Cr), 2 Co, 1 W, 1 Ti, 1 Al | Ni 61, Cr 16, Co 8.5, W 2.6, Ti 3.4, Al 3.4 | 4,200 K, IV | RTM alloy | helium |

## Turbine rotors (power, efficiency, damage, durability)

| Alloy | Power | Efficiency | Damage | Durability | Character |
|---|---|---|---|---|---|
| Kovar | 110 | 120 | 2.5 | 600 | cheap, MV |
| Havar | 150 | 125 | 3.5 | 1,800 | solid all-rounder |
| Stellite 1 | 160 | 120 | 4.0 | 3,200 | wear resistant |
| Inconel 625 | 170 | 150 | 4.0 | 2,400 | efficient |
| Stellite 12 | 175 | 140 | 4.5 | 4,200 | tough |
| Inconel 718 | 200 | 165 | 5.0 | 3,600 | strong and efficient |
| Hastelloy C-276 | 190 | 175 | 4.5 | 5,000 | most efficient, longest lasting of the Ni alloys |
| Inconel 738 | 260 | 190 | 6.5 | 5,600 | the turbine blade alloy: top power |

## Looks

| Alloy | Colour | Grain |
|---|---|---|
| Kovar | blued steel | glass-bead sheen, fine lines |
| Havar | warm gold | vertical hairlines, burgundy flecks |
| Stellite 1 | icy silver | dark carbide specks, mirror streaks |
| Inconel 625 | teal steel | diagonal weave |
| Stellite 12 | polished cobalt blue | bright streaks, cyan sparkle |
| Inconel 718 | bronze brown | amber temper speckles |
| Hastelloy | crimson | silver rivet dots |
| Inconel 738 | charcoal | orange casting glints, cross-hatch |

## Large Chemical Reactor structure (2026-11-01, af9-core 0.1.243)

Janne's 11 x 3 x 3 barrel (`chemreac.nbt`) replaces GT's 3 x 3 x 3 cross; machine, recipes, fab modes and hatch rules are unchanged
(`kubejs/startup_scripts/gtceu/large_chemical_reactor.js` swaps the pattern after the registries fill, like the Void Miner MK1).
Three blocks in the colours of the alloys, all GT's Inert PTFE casing recoloured (`tools/textures/lcr_casings.py`):

| block | where | recipe |
|---|---|---|
| `af9:inconel_625_casing` (teal steel) | the end plate | assembler, 6 Inconel 625 plates + steel frame, HV |
| `af9:inconel_718_casing` (bronze brown) | the barrel's shell | assembler, 6 Inconel 718 plates + steel frame, EV |
| `af9:inconel_718_slope` | the four long corners of the barrel | cutter, 1 Inconel 718 casing -> 2 slopes |

Structure (aisles back to front, rows bottom to top; R 625 casing, X Inert PTFE casing, B 718 casing, L slope, C coil, P PTFE pipe casing):

```
RXLLLLLLXXX / RXBBBBBBXXX / RXLLLLLLXXX
#XBBBBBBXPX / RXCCCCCCX#X / #XBBBBBBXPX
RXLLLLLLXXX / RXBBBBBBXSX / RXLLLLLLXXX
```

Hatches go in the Inert PTFE casings (at least 10 of them, as before); the six coils may be any one heating coil type (GT's LCR has
no coil bonus). The slope is a stair-placed triangle prism (`SlopeBlock`: `facing` = the wall's side, `half` = flat side) and matches in
any state.
