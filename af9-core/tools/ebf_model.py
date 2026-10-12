"""
Generates the formed-structure skin of GT's Electric Blast Furnace: two block models (the machine is 4 blocks high and a
vanilla model may only span 3, so it is cut at y = 32) and the textures painted for it. Run from af9-core:

    python tools/ebf_model.py

Model space: 1/16 block units, origin at the controller block's minimum corner, x to the controller's right, y up,
z away from the front (the pattern is 3 wide, 4 high, 3 deep; the controller is the middle block of the front, bottom).
Everything keeps 1 unit (0.5 on top) clear of the pattern's outline, so a hatch standing in a casing spot, which stays a
real block, reaches out of the skin instead of fighting its faces.

The "coil" texture is a marker: the render swaps it for the sprite of the coil the player built with, and "glow" for
the lit or the cold furnace window.
"""
import json
import math
import os
import random

from PIL import Image

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), '..', 'src', 'main', 'resources', 'assets', 'af9')
MODELS = os.path.join(ROOT, 'models', 'multiblock')
TEXTURES = os.path.join(ROOT, 'textures', 'block', 'multiblock', 'ebf')

TEX = {
    'casing': 'gtceu:block/casings/solid/machine_casing_heatproof',
    'plate': 'af9:block/multiblock/ebf/plate',
    'hoop': 'af9:block/multiblock/ebf/hoop',
    'pipe': 'af9:block/multiblock/ebf/pipe',
    'vent': 'af9:block/multiblock/ebf/vent',
    'coil': 'af9:block/multiblock/ebf/coil_marker',
    'glow': 'af9:block/multiblock/ebf/glow_on',
}

CX, CZ = 8.0, 24.0  # the middle of the pattern's footprint
SPLIT = 32.0

solid = []  # (from, to, texture, rotation or None)
glow = []


def box(x0, y0, z0, x1, y1, z1, tex, rot=None, into=None):
    (solid if into is None else into).append(((x0, y0, z0), (x1, y1, z1), tex, rot))


def octagon(apothem, y0, y1, tex, into=None):
    """A regular octagonal prism of the given flat-to-centre distance, round (CX, CZ): four overlapping bars."""
    side = 2 * apothem / (1 + math.sqrt(2))
    h = side / 2
    box(CX - apothem, y0, CZ - h, CX + apothem, y1, CZ + h, tex, into=into)
    box(CX - h, y0, CZ - apothem, CX + h, y1, CZ + apothem, tex, into=into)
    for angle in (45, -45):
        box(CX - apothem, y0, CZ - h, CX + apothem, y1, CZ + h, tex,
            rot={'origin': [CX, 0, CZ], 'axis': 'y', 'angle': angle}, into=into)


def mirror_x(x0, x1):
    return 2 * CX - x1, 2 * CX - x0


def build():
    # ---- base (layer 0): the casing slab with feet, vents and bolts
    box(-15, 0, 1, 31, 16, 47, 'casing')
    for (x0, x1) in ((-15, -9), (25, 31)):
        for (z0, z1) in ((1, 7), (41, 47)):
            box(x0 - 0.5, 0, z0 - 0.5, x1 + 0.5, 16, z1 + 0.5, 'plate')
    # front vent banks either side of the controller, side vent banks, the back one
    for (x0, x1) in ((-12, -3), (19, 28)):
        box(x0, 3, 0.5, x1, 13, 1.0, 'vent')
    for z0 in (10, 28):
        box(-15.5, 3, z0, -15, 13, z0 + 10, 'vent')
        box(31, 3, z0, 31.5, 13, z0 + 10, 'vent')
    box(-9, 3, 46.5, 25, 13, 47, 'vent')

    # ---- body (layers 1 and 2): corner columns and the round furnace between them
    for (x0, x1) in ((-15, -10), (26, 31)):
        for (z0, z1) in ((1, 6), (42, 47)):
            box(x0, 16, z0, x1, 48, z1, 'casing')
    octagon(21, 18, 46, 'plate')
    for (y0, y1) in ((20, 28), (36, 44)):
        octagon(22, y0, y1, 'coil')
    for (y0, y1) in ((16, 20), (28, 36), (44, 48)):
        octagon(22.5, y0, y1, 'hoop')
    # bolts on the hoops, one at each flat
    for y in (17.5, 31.5, 45.5):
        for (x, z) in ((CX - 23.2, CZ), (CX + 23.2, CZ), (CX, CZ - 23.2), (CX, CZ + 23.2)):
            box(x - 1, y, z - 1, x + 1, y + 1, z + 1, 'plate')

    # door: a plated frame on the front with the furnace window in it (the window is the glow model)
    box(-9, 19, 0.5, 25, 45, 2, 'plate')
    box(-10, 18, 0.25, 26, 20, 2, 'hoop')
    box(-10, 44, 0.25, 26, 46, 2, 'hoop')
    box(-10, 20, 0.25, -8, 44, 2, 'hoop')
    box(24, 20, 0.25, 26, 44, 2, 'hoop')
    box(-6, 22, 0.25, 22, 42, 0.5, 'hoop')  # the frame round the window
    box(22.5, 29, 0, 24.5, 35, 0.5, 'pipe')  # handle

    # brass pipes: a riser at each front corner, a manifold across the back with its stubs
    for x in (-8, 24):
        box(x - 1.5, 16, 2.0, x + 1.5, 48, 5.0, 'pipe')
        box(x - 2, 26, 1.8, x + 2, 28, 5.2, 'hoop')
        box(x - 2, 38, 1.8, x + 2, 40, 5.2, 'hoop')
    box(-12, 38, 45.5, 28, 43, 47.5, 'pipe')
    for x in (-6, 6, 22):
        box(x - 2, 36, 44.5, x + 2, 45, 47.7, 'hoop')
    box(-14, 22, 45.5, -11, 43, 47.5, 'pipe')
    box(27, 22, 45.5, 30, 43, 47.5, 'pipe')

    # ---- top (layer 3): the casing slab, a rim and gauges
    box(-15, 48, 1, 31, 63, 47, 'casing')
    box(-15.5, 61, 0.5, 31.5, 63, 1.5, 'hoop')
    box(-15.5, 61, 46.5, 31.5, 63, 47.5, 'hoop')
    box(-15.5, 61, 1.5, -14.5, 63, 46.5, 'hoop')
    box(30.5, 61, 1.5, 31.5, 63, 46.5, 'hoop')
    # a pressure gauge and two valves on the front of the top slab
    box(-12, 53, 0.5, -6, 59, 1.0, 'hoop')
    box(-11, 54, 0.25, -7, 58, 0.5, 'vent')
    box(22, 54, 0.25, 24, 58, 1.0, 'pipe')
    box(26, 54, 0.25, 28, 58, 1.0, 'pipe')
    # pipes from the furnace roof out to the sides
    box(-14, 48, 12, -10, 52, 36, 'pipe')
    box(26, 48, 12, 30, 52, 36, 'pipe')

    # ---- glow: the window and slits in the hoops
    box(-4, 24, 0, 20, 40, 0.3, 'glow', into=glow)
    for (y0, y1) in ((22, 26), (38, 42)):
        octagon(22.6, y0 + 1.5, y1 - 1.5, 'glow', into=glow)


def clip(items, lo, hi):
    out = []
    for (a, b, tex, rot) in items:
        y0, y1 = max(a[1], lo), min(b[1], hi)
        if y1 - y0 <= 1e-6:
            continue
        out.append(((a[0], y0 - lo, a[2]), (b[0], y1 - lo, b[2]), tex, rot))
    return out


def element(a, b, tex, rot, lo):
    faces = {f: {'texture': '#' + tex} for f in ('north', 'east', 'south', 'west', 'up', 'down')}
    el = {'from': [round(v, 3) for v in a], 'to': [round(v, 3) for v in b], 'faces': faces}
    if rot is not None:
        r = dict(rot)
        r['origin'] = [rot['origin'][0], rot['origin'][1] - lo, rot['origin'][2]]
        el['rotation'] = r
    return el


def write_model(name, items, lo, textures):
    used = sorted({t for (_, _, t, _) in items})
    data = {
        'textures': {k: TEX[k] for k in used},
        'elements': [element(a, b, t, r, lo) for (a, b, t, r) in items],
    }
    data['textures']['particle'] = TEX['casing']
    os.makedirs(MODELS, exist_ok=True)
    with open(os.path.join(MODELS, name + '.json'), 'w') as f:
        json.dump(data, f, indent=1)
        f.write('\n')


def paint():
    os.makedirs(TEXTURES, exist_ok=True)
    rnd = random.Random(9)

    def save(name, img):
        img.save(os.path.join(TEXTURES, name + '.png'))

    def noise(base, spread):
        return tuple(max(0, min(255, c + rnd.randint(-spread, spread))) for c in base)

    # dark steel plating with a bevel and rivets
    img = Image.new('RGBA', (16, 16))
    for y in range(16):
        for x in range(16):
            c = noise((74, 78, 86), 5)
            if x in (0, 15) or y in (0, 15):
                c = (46, 49, 55)
            elif x in (1, 14) or y in (1, 14):
                c = (96, 101, 110)
            img.putpixel((x, y), c + (255,))
    for (x, y) in ((3, 3), (12, 3), (3, 12), (12, 12)):
        img.putpixel((x, y), (150, 154, 162, 255))
        img.putpixel((x + 1, y + 1), (30, 32, 36, 255))
    save('plate', img)

    # hoop: darker banded iron
    img = Image.new('RGBA', (16, 16))
    for y in range(16):
        for x in range(16):
            band = 38 + (14 if y % 8 < 2 else 0)
            c = noise((band + 20, band + 22, band + 28), 4)
            img.putpixel((x, y), c + (255,))
    save('hoop', img)

    # pipe: brass with a seam
    img = Image.new('RGBA', (16, 16))
    for y in range(16):
        for x in range(16):
            shade = 1.0 - 0.35 * abs(x - 7.5) / 7.5
            c = tuple(int(v * shade) for v in noise((196, 150, 62), 6))
            img.putpixel((x, y), c + (255,))
    save('pipe', img)

    # vent: dark slats
    img = Image.new('RGBA', (16, 16))
    for y in range(16):
        for x in range(16):
            c = (24, 25, 28) if y % 4 in (0, 1) else noise((88, 92, 100), 4)
            img.putpixel((x, y), c + (255,))
    save('vent', img)

    # coil marker: copper-ish stand-in if the swap ever fails
    img = Image.new('RGBA', (16, 16))
    for y in range(16):
        for x in range(16):
            c = noise((178, 98, 52), 10) if (x + y) % 4 else (120, 62, 30)
            img.putpixel((x, y), c + (255,))
    save('coil_marker', img)

    # the window: white-hot centre to orange edge, and the cold one
    hot = Image.new('RGBA', (16, 16))
    cold = Image.new('RGBA', (16, 16))
    for y in range(16):
        for x in range(16):
            d = max(abs(x - 7.5), abs(y - 7.5)) / 7.5
            r = 255
            g = int(235 - 150 * d + rnd.randint(-12, 12))
            b = int(150 - 140 * d + rnd.randint(-8, 8))
            hot.putpixel((x, y), (r, max(0, min(255, g)), max(0, min(255, b)), 255))
            v = int(40 + 18 * (1 - d) + rnd.randint(-4, 4))
            cold.putpixel((x, y), (v + 14, v, v - 4, 255))
    save('glow_on', hot)
    save('glow_off', cold)


def main():
    build()
    write_model('ebf_lower', clip(solid, 0, SPLIT), 0, TEX)
    write_model('ebf_upper', clip(solid, SPLIT, 64), SPLIT, TEX)
    write_model('ebf_glow_lower', clip(glow, 0, SPLIT), 0, TEX)
    write_model('ebf_glow_upper', clip(glow, SPLIT, 64), SPLIT, TEX)
    paint()
    print('solid elements:', len(solid), 'glow elements:', len(glow))


if __name__ == '__main__':
    main()
