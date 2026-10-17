# Planetary Satellite

The Planetary Satellite (`gtceu:planetary_satellite`) is the only source of neutronium. It gathers neutron rays and turns
them into **small piles of neutronium dust**; four piles make a dust (the packager), and from there it is GT's neutronium
as ever (ingots, plates, ...).

## Structure

GTNH's Planetary Gas Siphon, block for block (GT5-Unofficial `MTEPlanetaryGasSiphon`, LGPL-3.0): 13 x 23 x 13, a 3 x 3 tower
with a wide dish on its crown and a crossed ring of arms round its middle. Pattern: `startup_scripts/gtceu/planetary_satellite.js`.

| char | block |
|---|---|
| `C` | Satellite Casing (`af9:satellite_casing`, made with Enriched Naquadah Trinium Europium Duranide); hatches go here |
| `B` | HSS-S frame |
| `K` | Superconducting Coil Block: a column up the middle of the tower from the bottom to the top, and the siphon's coil spots |

Hatches (maximums only): energy 2, fluid input 2, item input 1, item output 2, maintenance 1. The controller sits in the tower's
front face; its face is the siphon's.

## Recipe

`gtceu:neutron_collection`: 2,000 mB supercooled xenon (the detectors' coolant) -> 8 small neutronium dust, 600 ticks, UV 1 A
(524,288 EU/t); imperfect overclocks. About 4 neutronium dust a minute on a UV hatch.

GT's fusion recipe for neutronium plasma (americium and naquadria) is removed, so the satellite is the only source.

## Crafting

- Satellite Casing (2): stable titanium casing + 6 Enriched Naquadah Trinium Europium Duranide ingots + HSS-S frame (assembler, UV, circuit 27).
- Controller: 4 UV circuits, 2 UV field generators, 2 UV sensors round a Satellite Casing.

The casing's texture is GTNH's siphon casing recoloured in the material's colours (`tools/textures/satellite_casing.py`).
