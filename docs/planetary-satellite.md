# Planetary Satellite

The Planetary Satellite (`gtceu:planetary_satellite`) is the only source of neutronium. It takes in power and a reflector
plate and gathers neutron rays into **liquid neutronium** (solidify it to ingots as ever).

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

`gtceu:neutron_collection`: one reflector plate (consumed) -> liquid neutronium, 240 ticks (12 s), UV 1 A (524,288 EU/t);
imperfect overclocks. The amount is rolled in 12 mB steps (1 to 10 steps, 12 to 120 mB) by `AF9Modifiers.SATELLITE_YIELD`; with parallel
hatches or batch mode every parallel run rolls on its own. The reflector sets the lowest roll:

| plate | yield per run |
|---|---|
| iridium | 12 - 120 mB (avg 66) |
| osmiridium | 36 - 120 mB (avg 78) |
| tritanium | 72 - 120 mB (avg 96) |

The tier is read from the recipe id suffix (`af9:neutronium_rays_<plate>`), recipes in `server_scripts/mods/gtceu/planetary_satellite.js`.

GT's fusion recipe for neutronium plasma (americium and naquadria) is removed, so the satellite is the only source.

## Crafting

- Satellite Casing (2): stable titanium casing + 6 Enriched Naquadah Trinium Europium Duranide ingots + HSS-S frame (assembler, UV, circuit 27).
- Controller: 4 UV circuits, 2 UV field generators, 2 UV sensors round a Satellite Casing.

The casing's texture is GTNH's siphon casing recoloured in the material's colours (`tools/textures/satellite_casing.py`). It has connected textures (LDLib `.mcmeta` + `_ctm` sheet, like the Space Elevator's), so neighbouring casings join. While the machine works the controller's orb breathes, a glint sweeps over it and its marker blinks (16-frame animated overlay).
