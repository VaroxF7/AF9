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
FLOOR = -4.0  # the plate under the ground

solid = []  # (from, to, texture, rotation or None)
glow = []


def box(x0, y0, z0, x1, y1, z1, tex, rot=None, into=None):
    (solid if into is None else into).append(((x0, y0, z0), (x1, y1, z1), tex, rot))


def octagon(apothem, y0, y1, tex, into=None):
    """A regular octagonal prism of the given flat-to-centre distance, round (CX, CZ): four overlapping bars. Each bar
    is a hair shorter than the one before, so the tops (and bottoms) of the bars do not fight."""
    side = 2 * apothem / (1 + math.sqrt(2))
    h = side / 2
    box(CX - apothem, y0, CZ - h, CX + apothem, y1, CZ + h, tex, into=into)
    box(CX - h, y0 + 0.01, CZ - apothem, CX + h, y1 - 0.01, CZ + apothem, tex, into=into)
    for n, angle in enumerate((45, -45)):
        box(CX - apothem, y0 + 0.02 * (n + 1), CZ - h, CX + apothem, y1 - 0.02 * (n + 1), CZ + h, tex,
            rot={'origin': [CX, 0, CZ], 'axis': 'y', 'angle': angle}, into=into)


def build():
    # The pattern's outline is x -16..32, y 0..64, z 0..48. The bodies keep 1 unit inside it (a hatch in a casing spot
    # stays a real block and reaches out of the skin), the relief on them (corner posts, rims, vents, pipes)
    # stands out of the outline by up to 1.6 units for depth. No two faces share a plane, to keep them from fighting.

    # ---- under the ground: the terrain's top face under the structure is not drawn (the blocks above it are opaque
    # to it), so a plate fills the hole round the base that the 1 unit margin leaves
    box(-16, -4, 0, 32, 0, 48, 'casing')

    # ---- base (layer 0): the casing slab with feet (no vent banks: they hid the hatches)
    box(-15, 0, 1, 31, 16, 47, 'casing')
    for (x0, x1) in ((-15, -9), (25, 31)):
        for (z0, z1) in ((1, 7), (41, 47)):
            box(x0 - 0.6, 0, z0 - 0.6, x1 + 0.6, 16.4, z1 + 0.6, 'plate')

    # ---- body (layers 1 and 2): corner posts and the round furnace between them
    for (x0, x1) in ((-15.6, -10), (26, 31.6)):
        for (z0, z1) in ((0.4, 6), (42, 47.6)):
            box(x0, 15.8, z0, x1, 63.4, z1, 'casing')
    octagon(21, 18, 46, 'plate')
    for (y0, y1) in ((20, 28), (36, 44)):
        octagon(22, y0, y1, 'coil')
    for (y0, y1) in ((16, 20), (28, 36), (44, 48)):
        octagon(22.5, y0, y1, 'hoop')
    # bolts on the hoops, one at each flat
    for y in (17.5, 31.5, 45.5):
        for (x, z) in ((CX - 23.4, CZ), (CX + 23.4, CZ), (CX, CZ - 23.4), (CX, CZ + 23.4)):
            box(x - 1, y, z - 1, x + 1, y + 1, z + 1, 'plate')

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

    # ---- top (layer 3): a casing slab under the muffler hatch (the middle of the top) with a rim round it, gauges and
    # two flues that run into a collar round the muffler. Hatches in the top layer stand 1 unit out of the slab.
    box(-15, 47.9, 1, 31, 63, 47, 'casing')
    box(-16.1, 61, -0.9, 32.1, 64.4, 1.3, 'hoop')
    box(-16.1, 61, 46.7, 32.1, 64.4, 48.9, 'hoop')
    box(-16.1, 61, 1.3, -13.9, 64.4, 46.7, 'hoop')
    box(29.9, 61, 1.3, 32.1, 64.4, 46.7, 'hoop')
    # a pressure gauge and two valves on the front of the top slab
    box(-12, 50, -0.6, -6, 56, 1.1, 'hoop')
    box(-11, 51, -1.2, -7, 55, -0.5, 'vent')
    box(22, 51, -1.0, 24, 55, 1.1, 'pipe')
    box(26, 51, -1.0, 28, 55, 1.1, 'pipe')
    # the collar round the muffler hatch (its top is the block's: the pattern's middle of the top layer)
    box(-1.5, 62.9, 14.5, 17.5, 66, 16, 'hoop')
    box(-1.5, 62.9, 32, 17.5, 66, 33.5, 'hoop')
    box(-1.5, 62.9, 16, 0, 66, 32, 'hoop')
    box(16, 62.9, 16, 17.5, 66, 32, 'hoop')
    # risers at the sides and the flues from them into the collar
    for (x0, x1) in ((-13, -9), (25, 29)):
        box(x0, 62.9, 21, x1, 69, 27, 'pipe')
        box(x0 - 0.6, 66, 20.4, x1 + 0.6, 68, 27.6, 'hoop')
    box(-9, 62.9, 22, -1.5, 66, 26, 'pipe')
    box(17.5, 62.9, 22, 25, 66, 26, 'pipe')
    for x in (-6, 22):
        box(x - 1, 65.4, 21.4, x + 1, 66.4, 26.6, 'hoop')

    # ---- steel bands round the coil bands (no glow parts: the furnace has no window)
    for (y0, y1) in ((22, 26), (38, 42)):
        octagon(22.6, y0 + 1.5, y1 - 1.5, 'hoop')


def clip(items, lo, hi, base):
    """The part of the items between lo and hi (model-space y), with y counted from base."""
    out = []
    for (a, b, tex, rot) in items:
        y0, y1 = max(a[1], lo), min(b[1], hi)
        if y1 - y0 <= 1e-6:
            continue
        out.append(((a[0], y0 - base, a[2]), (b[0], y1 - base, b[2]), tex, rot))
    return out


def cuts(lo, hi):
    """The block-grid planes (multiples of 16) strictly between lo and hi, with the ends: the cell boundaries."""
    out = [lo]
    k = math.floor(lo / 16) + 1
    while k * 16 < hi - 1e-6:
        out.append(k * 16)
        k += 1
    out.append(hi)
    return out


def face_uv(face, p0, p1, cell):
    """The face's UVs as the vanilla default would give them, from coordinates inside one 16-unit cell (so they stay
    within the sprite: a face that is not cut at the cell borders reads past the sprite into the atlas' neighbours)."""
    x0, y0, z0 = (p0[i] - cell[i] for i in range(3))
    x1, y1, z1 = (p1[i] - cell[i] for i in range(3))
    return {
        'north': [16 - x1, 16 - y1, 16 - x0, 16 - y0],
        'south': [x0, 16 - y1, x1, 16 - y0],
        'west': [z0, 16 - y1, z1, 16 - y0],
        'east': [16 - z1, 16 - y1, 16 - z0, 16 - y0],
        'up': [x0, z0, x1, z1],
        'down': [x0, 16 - z1, x1, 16 - z0],
    }[face]


def elements(a, b, tex, rot, lo):
    """A box as elements of at most one block-grid cell each (an element wider than 16 units would put its UVs outside
    the sprite). Only the faces on the box's own outline are kept, the cut faces inside it are hidden."""
    xs, ys, zs = cuts(a[0], b[0]), cuts(a[1], b[1]), cuts(a[2], b[2])
    out = []
    for i in range(len(xs) - 1):
        for j in range(len(ys) - 1):
            for k in range(len(zs) - 1):
                p0 = (xs[i], ys[j], zs[k])
                p1 = (xs[i + 1], ys[j + 1], zs[k + 1])
                cell = (math.floor((p0[0] + 1e-6) / 16) * 16, math.floor((p0[1] + 1e-6) / 16) * 16,
                        math.floor((p0[2] + 1e-6) / 16) * 16)
                edges = {'west': i == 0, 'east': i == len(xs) - 2, 'down': j == 0, 'up': j == len(ys) - 2,
                         'north': k == 0, 'south': k == len(zs) - 2}
                faces = {}
                for face, outer in edges.items():
                    if not outer:
                        continue
                    uv = [round(v, 3) for v in face_uv(face, p0, p1, cell)]
                    faces[face] = {'texture': '#' + tex, 'uv': uv}
                el = {'from': [round(v, 3) for v in p0], 'to': [round(v, 3) for v in p1], 'faces': faces}
                if rot is not None:
                    r = dict(rot)
                    r['origin'] = [rot['origin'][0], rot['origin'][1] - lo, rot['origin'][2]]
                    el['rotation'] = r
                out.append(el)
    return out


def write_model(name, items, lo, textures):
    used = sorted({t for (_, _, t, _) in items})
    data = {
        'textures': {k: TEX[k] for k in used},
        'elements': [e for (a, b, t, r) in items for e in elements(a, b, t, r, lo)],
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

    # the window: heat shimmering in a loop (8 frames in a strip, animated by the .mcmeta), the ember of a machine that
    # waits, and the cold one
    frames = 8
    hot = Image.new('RGBA', (16, 16 * frames))
    for f in range(frames):
        t = 2 * math.pi * f / frames
        for y in range(16):
            for x in range(16):
                d = max(abs(x - 7.5), abs(y - 7.5)) / 7.5
                wave = (math.sin(2 * math.pi * x / 8 + t) + math.sin(2 * math.pi * y / 5 - 2 * t) +
                        math.sin(2 * math.pi * (x + y) / 11 + 3 * t)) / 3
                heat = max(0.0, min(1.0, 0.62 + 0.30 * wave - 0.30 * d * d))
                r = 255
                g = int(60 + 195 * heat ** 1.4)
                b = int(10 + 180 * max(0.0, heat - 0.55) / 0.45)
                hot.putpixel((x, 16 * f + y), (r, g, min(255, b), 255))
    save('glow_on', hot)
    with open(os.path.join(TEXTURES, 'glow_on.png.mcmeta'), 'w') as f:
        json.dump({'animation': {'frametime': 2, 'interpolate': True}}, f)
    wait = Image.new('RGBA', (16, 16))
    cold = Image.new('RGBA', (16, 16))
    for y in range(16):
        for x in range(16):
            d = max(abs(x - 7.5), abs(y - 7.5)) / 7.5
            v = int(40 + 18 * (1 - d) + rnd.randint(-4, 4))
            cold.putpixel((x, y), (v + 14, v, v - 4, 255))
            e = int(70 + 60 * (1 - d) + rnd.randint(-6, 6))
            wait.putpixel((x, y), (e + 40, int(e * 0.35), 10, 255))
    save('glow_off', cold)
    save('glow_wait', wait)


def main():
    build()
    write_model('ebf_lower', clip(solid, FLOOR, SPLIT, 0), 0, TEX)
    write_model('ebf_upper', clip(solid, SPLIT, 80, SPLIT), SPLIT, TEX)
    write_model('ebf_glow_lower', clip(glow, FLOOR, SPLIT, 0), 0, TEX)
    write_model('ebf_glow_upper', clip(glow, SPLIT, 80, SPLIT), SPLIT, TEX)
    paint()
    print('solid elements:', len(solid), 'glow elements:', len(glow))


if __name__ == '__main__':
    main()
