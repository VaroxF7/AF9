#!/usr/bin/env python3
"""Turns a structure file (vanilla .nbt, gzipped) of the Orbital Array Mk2 into the pattern the station is built from.

    python3 tools/orbital_mk2_layout.py path/to/orbital.nbt

writes kubejs/startup_scripts/gtceu/orbital_mk2_layout.js (global.ORBITAL_MK2_AISLES) and prints the block counts.
No dependencies. The structure must have the controller (gtceu:orbital_lithography_station) at the top, facing up with
upwards west, as the station is built from it: aisles run to the controller's up (west, -x), characters to its right
(south, +z), rows to its front (up). The pattern is the box 36 blocks to every side of the controller and from the lowest
block up to the controller's level (the controller is the last row, the middle character of the middle aisle).

Cells that need rules the file cannot hold (hatches, module slots) are marked on top of the file's blocks, see HATCH_RULE
and MODULE_SLOTS below; everything else is the file's block, 1:1. Air is '.', anything. The wafer cell (red wool in the
file) is '#', air that must stay air.
"""
import gzip
import math
import os
import struct
import sys

HALF = 36
OUT = os.path.join(os.path.dirname(os.path.abspath(__file__)), '..', 'kubejs', 'startup_scripts', 'gtceu',
                   'orbital_mk2_layout.js')

CHAR = {
    'gtceu:orbital_lithography_station': 'K',
    'gtceu:superconducting_coil': 'S',
    'af9:orbital_truss_casing': 'D',
    'af9:orbital_girder_casing': 'E',
    'gtceu:tungsten_carbide_frame': 'F',
    'af9:orbital_magnet_casing': 'M',
    'af9:orbital_light_casing': 'N',
    'gtceu:atomic_casing': 'H',
    'af9:orbital_combined_structure_casing': 'L',
    'gtceu:nonconducting_casing': 'C',
    'gtceu:fusion_glass': 'G',
    'gtceu:heat_vent': 'V',
    'gtceu:tungstensteel_turbine_casing': 'T',
    'gtceu:computer_casing': 'P',
    'create:metal_girder': 'g',
    'gtceu:magnetic_neodymium_block': 'n',
    'gtceu:magnetic_samarium_block': 's',
    'minecraft:red_wool': '#',
    'minecraft:air': '.',
}
# a rotor holder of any tier in the file is a 'R' (the pattern asks for UV or better)
HOLDER = '_rotor_holder'

# hatches go on the printing chamber's deck: the nonconducting casing of its top face (nothing above it), within this
# many blocks of the axis (dx, dz from the axis)
HATCH_Y_FROM_CONTROLLER = 52
HATCH_RADIUS = 9.5
# module slots: (dx, dz from the axis) of a nonconducting casing in the deck's rim with casing above and below it
MODULE_SLOTS = [(-6, -9), (6, -9), (-6, 9), (6, 9), (-9, -2), (9, 2)]
MODULE_Y_FROM_CONTROLLER = 53


def nbt(data):
    pos = [0]

    def take(fmt):
        size = struct.calcsize(fmt)
        value = struct.unpack_from(fmt, data, pos[0])
        pos[0] += size
        return value[0]

    def string():
        n = take('>H')
        s = data[pos[0]:pos[0] + n].decode('utf8', 'replace')
        pos[0] += n
        return s

    def payload(tag):
        if tag == 1: return take('>b')
        if tag == 2: return take('>h')
        if tag == 3: return take('>i')
        if tag == 4: return take('>q')
        if tag == 5: return take('>f')
        if tag == 6: return take('>d')
        if tag == 7:
            n = take('>i')
            pos[0] += n
            return None
        if tag == 8: return string()
        if tag == 9:
            item = take('>b')
            return [payload(item) for _ in range(take('>i'))]
        if tag == 10:
            out = {}
            while True:
                t = take('>b')
                if t == 0: return out
                key = string()
                out[key] = payload(t)
        if tag == 11:
            n = take('>i')
            pos[0] += 4 * n
            return None
        if tag == 12:
            n = take('>i')
            pos[0] += 8 * n
            return None
        raise ValueError('tag %d' % tag)

    tag = take('>b')
    string()
    return payload(tag)


def main(path):
    raw = open(path, 'rb').read()
    try:
        raw = gzip.decompress(raw)
    except OSError:
        pass
    tree = nbt(raw)
    palette = [p['Name'] for p in tree['palette']]
    cells = {}
    controller = None
    for b in tree['blocks']:
        name = palette[b['state']]
        if name == 'minecraft:air':
            continue
        x, y, z = b['pos']
        if name == 'gtceu:orbital_lithography_station':
            controller = (x, y, z)
        cells[(x, y, z)] = name
    if controller is None:
        sys.exit('no controller in the file')
    cx, cy, cz = controller
    low = min(y for (_, y, _) in cells)
    print('controller', controller, 'lowest block', low, 'blocks', len(cells))
    for (x, y, z) in cells:
        if abs(x - cx) > HALF or abs(z - cz) > HALF or y > cy:
            sys.exit('block %s outside the pattern box' % ((x, y, z),))
    rows = cy - low + 1

    def block_char(name):
        if name.endswith(HOLDER):
            return 'R'
        if name not in CHAR:
            sys.exit('no character for %s' % name)
        return CHAR[name]

    grid = {}
    for p, name in cells.items():
        grid[p] = block_char(name)
    # hatches: the deck's top face (but not over a module slot: the module needs plain casing above and below it)
    hatches = 0
    over_slot = {(cx + dx, cy - HATCH_Y_FROM_CONTROLLER, cz + dz) for dx, dz in MODULE_SLOTS}
    for (x, y, z), ch in list(grid.items()):
        if ch != 'C' or cy - y != HATCH_Y_FROM_CONTROLLER:
            continue
        if math.hypot(x - cx, z - cz) > HATCH_RADIUS or (x, y + 1, z) in grid or (x, y, z) in over_slot:
            continue
        grid[(x, y, z)] = 'O'
        hatches += 1
    # module slots
    for dx, dz in MODULE_SLOTS:
        p = (cx + dx, cy - MODULE_Y_FROM_CONTROLLER, cz + dz)
        for q in (p, (p[0], p[1] + 1, p[2]), (p[0], p[1] - 1, p[2])):
            if grid.get(q) != 'C':
                sys.exit('module slot %s is not in nonconducting casing' % (q,))
        grid[p] = 'Y'

    aisles = []
    counts = {}
    for a in range(2 * HALF + 1):
        x = cx + HALF - a            # aisles run to the controller's up: west, -x
        lines = []
        for r in range(rows):
            y = low + r              # rows run up
            line = ''
            for c in range(2 * HALF + 1):
                z = cz - HALF + c    # characters run to the controller's right: south, +z
                ch = grid.get((x, y, z), '.')
                counts[ch] = counts.get(ch, 0) + 1
                line += ch
            lines.append(line)
        aisles.append(lines)
    print('aisles', len(aisles), 'rows', rows, 'width', 2 * HALF + 1)
    print('hatch cells', hatches, 'counts', {k: v for k, v in sorted(counts.items()) if k != '.'})

    out = []
    out.append('// priority: 100')
    out.append('// AF9 - the Orbital Array Mk2 layout, generated by tools/orbital_mk2_layout.py from Janne\'s orbital.nbt: do not edit by hand,')
    out.append('// change the file (or the tool) and generate again. 73 aisles (the controller\'s up, west), each %d rows (the controller\'s front,' % rows)
    out.append('// up: the first row is the foot, the last the controller\'s), each 73 characters (its right, south). One string per aisle, a row to a')
    out.append('// line (a string of its own per row would make the script too big for the JVM). Read by photolithography.js.')
    out.append('//   . anything         # air that stays air (the wafer)      K the controller')
    out.append('//   L af9:orbital_combined_structure_casing   D af9:orbital_truss_casing   E af9:orbital_girder_casing')
    out.append('//   M af9:orbital_magnet_casing   N af9:orbital_light_casing   H gtceu:atomic_casing   C gtceu:nonconducting_casing')
    out.append('//   F gtceu:tungsten_carbide_frame   S gtceu:superconducting_coil   G gtceu:fusion_glass   V gtceu:heat_vent')
    out.append('//   T gtceu:tungstensteel_turbine_casing   P gtceu:computer_casing   g create:metal_girder')
    out.append('//   n gtceu:magnetic_neodymium_block   s gtceu:magnetic_samarium_block   R a rotor holder, UV or better')
    out.append('//   O nonconducting casing or a hatch (the chamber deck)   Y nonconducting casing or an orbital module')
    out.append('global.ORBITAL_MK2_AISLES = [')
    for lines in aisles:
        out.append('`' + '\n'.join(lines) + '`,')
    out.append(']')
    with open(OUT, 'w') as f:
        f.write('\n'.join(out) + '\n')
    print('wrote', os.path.normpath(OUT))


if __name__ == '__main__':
    main(sys.argv[1])
