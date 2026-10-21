# Planetary Satellite

The Planetary Satellite (`gtceu:planetary_satellite`) is the only source of neutronium. It takes in power and a Neutron Ray
Reflector and gathers neutron rays into **liquid neutronium** (solidify it to ingots as ever).

## Structure

GTNH's Planetary Gas Siphon (GT5-Unofficial `MTEPlanetaryGasSiphon`, LGPL-3.0): a 3 x 3 tower with a wide dish on its crown and a
crossed ring of arms round its middle (13 x 23 x 13 in GTNH). The pattern is Janne's rebuilt version, read from his world: 15 x 24 x 15,
the arms end in five-block bars, a cross of frames sits on the dish, and an Orbital Magnet Casing and an Orbital Light Casing sit
in pairs on the front tip. Pattern: `startup_scripts/gtceu/planetary_satellite.js`.

| char | block |
|---|---|
| `C` | Satellite Casing (`af9:satellite_casing`, made with Enriched Naquadah Trinium Europium Duranide); hatches go here |
| `B` | HSS-S frame |
| `L` / `M` | Orbital Light Casing / Orbital Magnet Casing (decoration on the front tip; both connect and shimmer, `tools/textures/orbital_casings.py`) |
| `K` | Superconducting Coil Block: a column up the middle of the tower from the bottom to the top, and the siphon's coil spots |

Hatches (maximums only): energy 2, fluid input 2, item input 1, fluid output 2, parallel hatch 1, maintenance 1. The controller sits in the tower's
front face; its face is the siphon's.

## Recipe

`gtceu:neutron_collection`: a Neutron Ray Reflector in the dish -> liquid neutronium, 240 ticks (12 s), UV 1 A (524,288 EU/t);
imperfect overclocks. The reflector is a **not-consumed input**: it is never used up, but it wears 1 point a completed cycle
and breaks when its durability is gone - worn by `AF9Modifiers.satelliteWear`, wired to the machine's `afterWorking`, which GT
fires first in `RecipeLogic.onRecipeFinish`, so a batch or a 16-parallel cycle costs one point like any other cycle. The amount
is rolled in 12 mB steps (1 to 10 steps, 12 to 120 mB) by `AF9Modifiers.SATELLITE_YIELD`; with parallel
hatches or batch mode every parallel run rolls on its own. The reflector sets the lowest roll:

| reflector | yield per run | durability |
|---|---|---|
| Iridium (`af9:iridium_ray_reflector`) | 12 - 120 mB (avg 66) | 1,000 runs |
| Osmiridium (`af9:osmiridium_ray_reflector`) | 36 - 120 mB (avg 78) | 5,000 runs |
| Unobtainium-Vibranium (`af9:unobtainium_vibranium_ray_reflector`) | 96 - 120 mB (avg 108) | 10,000 runs |
| Tritanium (`af9:tritanium_ray_reflector`) | 120 - 144 mB (avg 132) | 20,000 runs |
| Ceranium (`af9:ceranium_ray_reflector`) | 144 - 168 mB (avg 156) | 40,000 runs |
| Neutronium (`af9:neutronium_ray_reflector`) | 168 - 192 mB (avg 180) | 80,000 runs |

The tier is read from the recipe id suffix (`af9:neutronium_rays_<reflector>`), recipes in `server_scripts/mods/gtceu/planetary_satellite.js`.

Every fusion recipe for neutronium (GT's americium and naquadria one, and the Mega Fusion Reactor's copy of it) is removed, so the satellite is the only source.

## Crafting

- Satellite Casing (2): stable titanium casing + 6 Enriched Naquadah Trinium Europium Duranide ingots + HSS-S frame (assembler, UV, circuit 27).
- Controller: 4 UV circuits, 2 UV field generators, 2 UV sensors round a Satellite Casing.
- Reflectors (assembler, UV, a ladder each built on the tier below): GT's Iridium Neutron Reflector + 4 Iridium plates -> Iridium;
  Iridium + 4 Osmiridium plates -> Osmiridium; Osmiridium + 4 Unobtainium-Vibranium alloy ingots -> Unobtainium-Vibranium; then 4 Tritanium plates -> Tritanium, 4 Ceranium plates -> Ceranium, 4 Neutronium plates -> Neutronium. A worn
  reflector trades up the same way.

The reflectors keep the plate of GTCEu's Iridium Neutron Reflector, recoloured per tier with a glow sweeping over it (a 16-frame
animated texture, `tools/textures/ray_reflectors.py`; credited in `CREDITS-textures.md`).

The casing's texture is GTNH's siphon casing recoloured in the material's colours (`tools/textures/satellite_casing.py`). It has connected textures (LDLib `.mcmeta` + `_ctm` sheet, like the Space Elevator's), so neighbouring casings join. While the machine works the controller's orb breathes, a glint sweeps over it and its marker blinks (16-frame animated overlay).
