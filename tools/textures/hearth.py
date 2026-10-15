#!/usr/bin/env python3
"""The Sanguinite Hearth's two casings with a 3D model of their own: the Hearth Pipe Casing (pipes run across a recessed
steel face) and the Hearth Support Casing (a diagonal truss in a concrete frame).

Run from the repository root (needs Pillow):  python3 tools/textures/hearth.py

Both are a full-block shape (so neighbours cull and GT's pattern sees an ordinary casing) with a model of elements: a
recessed core, a frame of 2 px beams along the 12 edges, and the pipes or braces standing out of the core inside the
frame. Writes the blockstates, models, loot tables and textures (af9-core assets and data).
"""
import json
import os

from PIL import Image

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), '../..')
ASSETS = os.path.join(ROOT, 'af9-core/src/main/resources/assets/af9')
DATA = os.path.join(ROOT, 'af9-core/src/main/resources/data/af9')
BLOCKS = ('hearth_pipe_casing', 'hearth_support_casing')


def dump(path, obj):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, 'w') as f:
        f.write(json.dumps(obj, indent=2) + '\n')


def grain(x, y):
    return ((x * 7 + y * 13 + x * y * 3) % 11) / 11.0


def frame():
    """Board-marked concrete, bevelled, with a bolt in each corner."""
    im = Image.new('RGBA', (16, 16))
    for y in range(16):
        for x in range(16):
            v = 78 + round(14 * grain(x, y))
            if x == 0 or y == 0:
                v += 22
            elif x == 15 or y == 15:
                v -= 24
            im.putpixel((x, y), (v, v + 1, v + 4, 255))
    for bx, by in ((2, 2), (13, 2), (2, 13), (13, 13)):
        im.putpixel((bx, by), (40, 42, 46, 255))
        im.putpixel((bx - 1, by - 1), (120, 122, 128, 255))
    return im


def core():
    """Dark steel plate in panels, a hazard-orange line along the foot."""
    im = Image.new('RGBA', (16, 16))
    for y in range(16):
        for x in range(16):
            v = 38 + round(8 * grain(x, y))
            if x in (0, 8) or y in (0, 8):
                v -= 14
            im.putpixel((x, y), (v, v + 2, v + 8, 255))
    for x in range(16):
        if (x // 2) % 2 == 0:
            im.putpixel((x, 14), (214, 118, 32, 255))
            im.putpixel((x, 15), (150, 80, 24, 255))
    return im


def pipe():
    """A pipe seen side-on: shaded across its height so it reads round, a seam band and a copper ring."""
    im = Image.new('RGBA', (16, 16))
    for y in range(16):
        light = 0.55 + 0.75 * (1 - abs((y - 5.5) / 8.0) ** 1.6)
        for x in range(16):
            v = round(150 * light)
            r, g, b = v, v + 4, v + 10
            if x in (3, 4) or x in (11, 12):
                r, g, b = round(v * 1.15), round(v * 0.8), round(v * 0.5)
            if x == 7:
                r, g, b = r - 28, g - 28, b - 28
            im.putpixel((x, y), (max(0, r), max(0, g), max(0, b), 255))
    return im


def box(a, b, tex, **extra):
    el = {'from': a, 'to': b,
          'faces': {d: {'uv': [0, 0, 16, 16], 'texture': tex} for d in ('north', 'south', 'east', 'west', 'up', 'down')}}
    el.update(extra)
    return el


def shell():
    """The recessed core and the twelve edge beams (the verticals and the Z beams trimmed so no two overlap)."""
    els = [box([2, 2, 2], [14, 14, 14], '#core')]
    for y in (0, 14):
        for z in (0, 14):
            els.append(box([0, y, z], [16, y + 2, z + 2], '#frame'))      # along X, full length
    for x in (0, 14):
        for z in (0, 14):
            els.append(box([x, 2, z], [x + 2, 14, z + 2], '#frame'))      # along Y, between the X beams
    for x in (0, 14):
        for y in (0, 14):
            els.append(box([x, y, 2], [x + 2, y + 2, 14], '#frame'))  # along Z, between the others
    return els


def pipe_model():
    els = shell()
    for y in (3, 10):                                  # two pipes across every side face, standing off the core
        els.append(box([2, y, 0.5], [14, y + 3, 2.5], '#pipe'))            # north
        els.append(box([2, y, 13.5], [14, y + 3, 15.5], '#pipe'))          # south
        els.append(box([0.5, y, 2], [2.5, y + 3, 14], '#pipe'))            # west
        els.append(box([13.5, y, 2], [15.5, y + 3, 14], '#pipe'))          # east
    return els


def support_model():
    els = shell()
    for axis, origin, lo, hi in (('z', [8, 8, 1], [7, -2, 0.5], [9, 18, 2]),
                                 ('z', [8, 8, 15], [7, -2, 14], [9, 18, 15.5]),
                                 ('x', [1, 8, 8], [0.5, 7, -2], [2, 9, 18]),
                                 ('x', [15, 8, 8], [14, 7, -2], [15.5, 9, 18])):
        d = 2 if axis == 'z' else 0          # the depth axis of the pair (the beams are thin in it)
        outer_is_lo = lo[d] < 8
        mid = round((lo[d] + hi[d]) / 2 + (0.125 if outer_is_lo else -0.125), 3)
        for angle in (45, -45):
            # the two crossing beams take their own half of the depth (45 the outer one), so no two faces share a plane
            a, b = list(lo), list(hi)
            if (angle == 45) == outer_is_lo:
                b[d] = mid
            else:
                a[d] = mid
            els.append(box(a, b, '#pipe', rotation={'angle': angle, 'axis': axis, 'origin': origin}))
    return els


def main():
    tex = os.path.join(ASSETS, 'textures/block')
    os.makedirs(tex, exist_ok=True)
    frame().save(os.path.join(tex, 'hearth_frame.png'))
    core().save(os.path.join(tex, 'hearth_core.png'))
    pipe().save(os.path.join(tex, 'hearth_pipe.png'))
    textures = {'frame': 'af9:block/hearth_frame', 'core': 'af9:block/hearth_core', 'pipe': 'af9:block/hearth_pipe',
                'particle': 'af9:block/hearth_frame'}
    for b, elements in (('hearth_pipe_casing', pipe_model()), ('hearth_support_casing', support_model())):
        dump(os.path.join(ASSETS, f'blockstates/{b}.json'), {'variants': {'': {'model': f'af9:block/{b}'}}})
        dump(os.path.join(ASSETS, f'models/block/{b}.json'),
             {'parent': 'minecraft:block/block', 'textures': textures, 'elements': elements})
        dump(os.path.join(ASSETS, f'models/item/{b}.json'), {'parent': f'af9:block/{b}'})
        dump(os.path.join(DATA, f'loot_tables/blocks/{b}.json'), {
            'type': 'minecraft:block',
            'pools': [{'rolls': 1, 'entries': [{'type': 'minecraft:item', 'name': f'af9:{b}'}],
                       'conditions': [{'condition': 'minecraft:survives_explosion'}]}]})


if __name__ == '__main__':
    main()
    print('Hearth casing assets written')
