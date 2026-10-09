#!/usr/bin/env python3
"""AF9's dimension markers: the cube "planets" GregTech shows in the dimension slot of a recipe page and of an ore vein's
page (JEI / EMI). GregTech has three of its own (Overworld, Nether, End: a map of the world wrapped round a cube) and a
barrier for every other dimension; these are for AF9's dimensions and the two of other mods AF9's recipes name.

Run from the repository root (needs Pillow):  python3 tools/textures/dimension_markers.py

Writes assets/af9/textures/block/dim_markers/<name>/<face>.png (16 x 16, the six faces of the cube) and
assets/af9/models/item/<name>_marker.json. AF9 Core registers the items and binds them to their dimensions
(registry/AF9DimensionMarkers). Every texel is drawn as the point of a sphere that lies behind it on the cube, so the six
faces meet without a seam and a gas giant's bands run round the cube. The palettes are the bodies' of
tools/textures/planets.py (the sky and the solar system map).
"""
import json
import math
import os
import random

from PIL import Image

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), '../..')
RES = os.path.join(ROOT, 'af9-core/src/main/resources/assets/af9')
SIZE = 16

# a face's texel (u right, v down, both -1 .. 1) as a point of the cube, by the way block/cube lays its textures out
FACES = {
    'north': lambda u, v: (-u, -v, -1.0),
    'south': lambda u, v: (u, -v, 1.0),
    'west': lambda u, v: (-1.0, -v, u),
    'east': lambda u, v: (1.0, -v, -u),
    'up': lambda u, v: (u, 1.0, v),
    'down': lambda u, v: (u, -1.0, -v),
}


def lattice(seed, ix, iy, iz):
    h = (ix * 374761393 + iy * 668265263 + iz * 2147483647 + seed * 144665) & 0xFFFFFFFF
    h = ((h ^ (h >> 13)) * 1274126177) & 0xFFFFFFFF
    return ((h ^ (h >> 16)) & 0xFFFF) / 65535.0


def noise(seed, p, scale):
    x, y, z = (c * scale for c in p)
    x0, y0, z0 = math.floor(x), math.floor(y), math.floor(z)
    fx, fy, fz = x - x0, y - y0, z - z0
    fx, fy, fz = (f * f * (3 - 2 * f) for f in (fx, fy, fz))
    total = 0.0
    for dz in (0, 1):
        for dy in (0, 1):
            for dx in (0, 1):
                w = (fx if dx else 1 - fx) * (fy if dy else 1 - fy) * (fz if dz else 1 - fz)
                total += w * lattice(seed, x0 + dx, y0 + dy, z0 + dz)
    return total


def fbm(seed, p, scale, octaves=3):
    total, weight, norm = 0.0, 1.0, 0.0
    for o in range(octaves):
        total += weight * noise(seed + o * 17, p, scale * (2 ** o))
        norm += weight
        weight *= 0.5
    return total / norm


def angle(a, b):
    return math.acos(max(-1.0, min(1.0, a[0] * b[0] + a[1] * b[1] + a[2] * b[2])))


def points(seed, count, low, high):
    """count points of the sphere, each with a radius (radians) between low and high."""
    rnd = random.Random(seed)
    out = []
    for _ in range(count):
        z = rnd.uniform(-1, 1)
        t = rnd.uniform(0, 2 * math.pi)
        r = math.sqrt(1 - z * z)
        out.append(((r * math.cos(t), z, r * math.sin(t)), rnd.uniform(low, high)))
    return out


def step(palette, t):
    """t (0 .. 1) to one of the palette's colours: flat steps, the pixel look of GregTech's own markers."""
    return palette[max(0, min(len(palette) - 1, int(t * len(palette))))]


def cratered(seed, palette, craters, maria=None, spots=()):
    holes = points(seed + 5, *craters)

    def shade(s, texel):
        n = fbm(seed, s, 2.4)
        t = n * 1.5 - 0.25
        if maria and fbm(seed + 90, s, 1.3, 2) < maria[0]:
            col = step(maria[1], t)
        else:
            col = step(palette, t)
        for centre, radius in holes:
            d = angle(s, centre)
            if d < radius * 0.62:
                col = palette[0]            # the floor, in shadow
            elif d < radius:
                col = palette[-1]           # the rim, lit
        for centre, radius, colour in spots:
            if angle(s, centre) < radius:
                col = colour
        return col
    return shade


def banded(seed, bands, wobble, spot=None):
    def shade(s, texel):
        lat = math.asin(max(-1.0, min(1.0, s[1])))
        t = (lat / math.pi + 0.5) * len(bands) + (fbm(seed, s, 2.6) - 0.5) * wobble
        col = bands[max(0, min(len(bands) - 1, int(t)))]
        if spot:
            centre, radius, core, rim = spot
            # an oval: twice as long as it is high
            turn = math.atan2(s[2], s[0]) - math.atan2(centre[2], centre[0])
            turn = (turn + math.pi) % (2 * math.pi) - math.pi
            d = math.hypot(turn * math.cos(lat) / 2.0, lat - math.asin(centre[1]))
            if d < radius * 0.7:
                col = core
            elif d < radius:
                col = rim
        return col
    return shade


def sun(seed, palette, spots):
    dark = points(seed + 3, *spots)

    def shade(s, texel):
        col = step(palette, fbm(seed, s, 3.6) * 1.7 - 0.35)
        for centre, radius in dark:
            d = angle(s, centre)
            if d < radius * 0.6:
                col = (0x9C, 0x38, 0x08)
            elif d < radius:
                col = palette[0]
        return col
    return shade


def rocks(seed, palette, count):
    """Space with boulders in it: the Asteroid Field has no body, the field is the place."""
    stones = points(seed + 7, count, 0.17, 0.36)
    light = (-0.45, 0.75, -0.48)

    def shade(s, texel):
        for centre, radius in stones:
            d = angle(s, centre)
            edge = radius * (0.7 + 0.6 * noise(seed + 40, s, 5.0))
            if d < edge:
                # lit from the upper left: the side of the stone that faces the light is the bright one
                side = sum((s[i] - centre[i]) * light[i] for i in range(3)) / max(edge, 1e-6)
                t = 0.45 + 0.9 * side + (fbm(seed, s, 6.0, 2) - 0.5) * 0.5
                return step(palette, t)
        h = lattice(seed + 99, *texel)
        if h > 0.965:
            return (0xD6, 0xDC, 0xEC)
        if h > 0.92:
            return (0x4E, 0x56, 0x72)
        return step([(0x05, 0x06, 0x0B), (0x08, 0x0A, 0x12), (0x0B, 0x0E, 0x1A)], fbm(seed + 60, s, 1.6, 2))
    return shade


def mine(seed, stone, deep, ores):
    """A world of stone over deepslate, shot through with ore: the Mining Dimension."""
    def shade(s, texel):
        n = fbm(seed, s, 3.0)
        layer = stone if s[1] + (fbm(seed + 20, s, 2.0, 2) - 0.5) * 0.5 > -0.12 else deep
        h = lattice(seed + 77, *texel)
        if h > 0.925:
            return ores[int(lattice(seed + 78, *texel) * len(ores)) % len(ores)]
        return step(layer, n * 1.5 - 0.25)
    return shade


CERES_SPOT = (0xF0, 0xEC, 0xDE)
MARKERS = {
    # the Mining Dimension (allthemodium:mining)
    'mining_dimension': mine(61, [(0x66, 0x66, 0x66), (0x78, 0x78, 0x78), (0x8A, 0x8A, 0x8A)],
                             [(0x36, 0x36, 0x3C), (0x44, 0x44, 0x4B), (0x52, 0x52, 0x5A)],
                             [(0x4A, 0xED, 0xD9), (0xFC, 0xEE, 0x4B), (0xD8, 0x1E, 0x12), (0x17, 0xDD, 0x62),
                              (0x2B, 0x5F, 0xD0), (0xD8, 0xAF, 0x93), (0xF7, 0xB5, 0x1E), (0x2F, 0xD8, 0xA5),
                              (0xB0, 0x4F, 0xE0)]),
    # the Moon (ad_astra:moon): pale highlands, dark seas
    'moon': cratered(71, [(0x7C, 0x7D, 0x82), (0x98, 0x99, 0x9D), (0xB4, 0xB5, 0xB7), (0xCF, 0xD0, 0xD1)],
                     (9, 0.16, 0.3), maria=(0.42, [(0x52, 0x55, 0x5E), (0x60, 0x63, 0x6C), (0x6E, 0x71, 0x79)])),
    'asteroid_field': rocks(51, [(0x2C, 0x29, 0x27), (0x4A, 0x43, 0x3D), (0x6A, 0x60, 0x58), (0x8A, 0x7E, 0x72)], 15),
    # Ceres: grey and cratered, with the two bright salt spots of its largest crater
    'ceres': cratered(41, [(0x4E, 0x49, 0x44), (0x66, 0x60, 0x5A), (0x80, 0x79, 0x70), (0x9A, 0x92, 0x88)],
                      (8, 0.2, 0.36),
                      spots=[((0.42, 0.52, -0.74), 0.11, CERES_SPOT), ((0.6, 0.42, -0.68), 0.07, CERES_SPOT)]),
    'zephyr': banded(11, [(0x7A, 0x34, 0x1E), (0xC9, 0x7A, 0x3A), (0xF2, 0xB9, 0x6B), (0xFF, 0xE3, 0xB0),
                          (0xC9, 0x7A, 0x3A), (0x7A, 0x34, 0x1E), (0xF2, 0xB9, 0x6B), (0xC9, 0x7A, 0x3A),
                          (0xFF, 0xE3, 0xB0), (0x9C, 0x52, 0x2A), (0x7A, 0x34, 0x1E)], 1.5,
                     spot=((-0.18, -0.3, -0.94), 0.3, (0xE8, 0x5A, 0x2A), (0xFF, 0xE3, 0xB0))),
    'kronos': banded(23, [(0x8E, 0x6B, 0x34), (0xC9, 0xA2, 0x5E), (0xE8, 0xD8, 0xA8), (0xF4, 0xEC, 0xD2),
                          (0xCF, 0xC0, 0x9C), (0xE8, 0xD8, 0xA8), (0xC9, 0xA2, 0x5E), (0xCF, 0xC0, 0x9C),
                          (0x8E, 0x6B, 0x34)], 0.7),
    'helios': sun(31, [(0xE8, 0x62, 0x0C), (0xFF, 0x8C, 0x1A), (0xFF, 0xB4, 0x30), (0xFF, 0xD8, 0x5C),
                       (0xFF, 0xF2, 0xAC)], (4, 0.1, 0.17)),
}
# Kronos keeps its ring, as on the map: a smaller cube with a flat ring round it
RINGED = {'kronos': [(0xB8, 0xA8, 0x80), (0xD8, 0xC8, 0xA0)]}


def face(shade, name):
    img = Image.new('RGBA', (SIZE, SIZE))
    for j in range(SIZE):
        for i in range(SIZE):
            u = (i + 0.5) / SIZE * 2 - 1
            v = (j + 0.5) / SIZE * 2 - 1
            p = FACES[name](u, v)
            length = math.sqrt(sum(c * c for c in p))
            s = tuple(c / length for c in p)
            img.putpixel((i, j), shade(s, (i, j, list(FACES).index(name))) + (255,))
    return img


def ring(colours):
    """The ring from above: two lanes round a hole that the cube (the middle half of the model) stands in."""
    img = Image.new('RGBA', (SIZE, SIZE), (0, 0, 0, 0))
    c = (SIZE - 1) / 2
    for j in range(SIZE):
        for i in range(SIZE):
            d = max(abs(i - c), abs(j - c))     # a square ring: it is a cube's
            if 6 <= d < 7:
                img.putpixel((i, j), colours[0] + (255,))
            elif 7 <= d:
                img.putpixel((i, j), colours[1] + (255,))
    return img


def model(name):
    tex = 'af9:block/dim_markers/%s/' % name
    textures = {f: tex + f for f in sorted(FACES)}
    textures['particle'] = '#north'
    if name not in RINGED:
        return {'parent': 'minecraft:block/cube', 'gui_light': 'front', 'textures': textures}
    textures['ring'] = tex + 'ring'
    whole = [0, 0, 16, 16]
    return {
        'parent': 'minecraft:block/block', 'gui_light': 'front', 'textures': textures,
        'elements': [
            {'from': [4, 4, 4], 'to': [12, 12, 12],
             'faces': {f: {'uv': whole, 'texture': '#' + f} for f in sorted(FACES)}},
            {'from': [0, 7.5, 0], 'to': [16, 8.5, 16],
             'faces': {'up': {'uv': whole, 'texture': '#ring'}, 'down': {'uv': whole, 'texture': '#ring'}}},
        ],
    }


def main():
    for name, shade in MARKERS.items():
        folder = os.path.join(RES, 'textures/block/dim_markers', name)
        os.makedirs(folder, exist_ok=True)
        for f in FACES:
            face(shade, f).save(os.path.join(folder, f + '.png'))
        if name in RINGED:
            ring(RINGED[name]).save(os.path.join(folder, 'ring.png'))
        with open(os.path.join(RES, 'models/item', name + '_marker.json'), 'w', newline='\n') as out:
            json.dump(model(name), out, indent=2)
            out.write('\n')
    print('%d markers written' % len(MARKERS))


if __name__ == '__main__':
    main()
