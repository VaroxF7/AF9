#!/usr/bin/env python3
"""Textures of the lithography fluids and gases: calm, muted, one colour each, nothing as loud as the plasmas.
Liquids are slow waves (viscous resists, clear rinses, murky waste, bubbling acids, icy coolants); gases are translucent
drifting wisps (a heavy fog for the metal chlorides, a faint glow for the laser and etch gases).

Run from the repository root (needs Pillow):  python3 tools/textures/litho_fluids.py [--sheet preview.png]

Writes into af9-core/src/main/resources/assets/gtceu/textures/block/fluids:
  fluid.<material>.png (+ .mcmeta): 32 frames of 16 x 16, tiles and loops. AF9Materials gives these materials
  customStill().disableColor(), so the texture is shown as it is.
"""
import json
import math
import os
import sys

from PIL import Image, ImageDraw

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), '../..')
FLUIDS = os.path.join(ROOT, 'af9-core/src/main/resources/assets/gtceu/textures/block/fluids')
FRAMES = 32
TAU = 2 * math.pi

# material: (style, light colour, dark colour)   style: calm viscous clear murky bubbly icy | wisp fog glow
TEXTURES = {
    # resists and coatings: ambers and ochres, thick and slow
    'photoresist': ('viscous', 0xb8683a, 0x6e3418),
    'krf_photoresist': ('viscous', 0xc9a24e, 0x7d5f22),
    'arf_photoresist': ('viscous', 0xd2c486, 0x8c7e44),
    'euv_photoresist': ('viscous', 0xb7a45a, 0x5e5426),
    'unfiltered_krf_photoresist': ('murky', 0xa8895a, 0x5c4630),
    'unfiltered_arf_photoresist': ('murky', 0xb0a478, 0x625a3c),
    'barc': ('viscous', 0xb99a3c, 0x6a561c),
    'tarc': ('clear', 0x9cc6c8, 0x50808a),
    'spent_resist_solvent': ('murky', 0x7c7248, 0x3e381e),
    # AE2: the Certus Resonating Manufacturer's stabiliser, fluix in solution
    'liquid_resonating_fluix': ('resonance', 0xd8f2ff, 0x1c2a58),
    # developer, rinse and cleaning baths: clear blues and greys
    'tmah_developer': ('clear', 0x9cc2d2, 0x4c7c92),
    'ultrapure_water': ('clear', 0x78a8d8, 0x3a68a0),
    'sc1_solution': ('calm', 0x9fc0d4, 0x5a8094),
    'sc2_solution': ('calm', 0xb4c49a, 0x6c7e52),
    'piranha_solution': ('bubbly', 0xc2aa6a, 0x7a622c),
    'spent_piranha': ('murky', 0x6a5a40, 0x2c2214),
    'silicon_etchant': ('bubbly', 0xb8b078, 0x6c6638),
    'tin_tetrachloride': ('clear', 0xb8b8a0, 0x6a6a52),
    # supercooled coolants (and the dense liquids before them): icy, a little sparkle
    'dense_hydrogen': ('icy', 0x6a98c8, 0x2e5686),
    'supercooled_hydrogen': ('icy', 0xa8d0ec, 0x5c8cb4),
    'dense_argon': ('icy', 0x4e9eaa, 0x1e5c68),
    'supercooled_argon': ('icy', 0x88c8cc, 0x468c94),
    'dense_xenon': ('icy', 0x6c5eb0, 0x30286c),
    'supercooled_xenon': ('icy', 0x9c90cc, 0x5c4e94),
    'dense_endion': ('icy', 0x5438a0, 0x241258),
    'supercooled_endion': ('icy', 0x8466c4, 0x4a3290),
    # gases
    'hmds_vapor': ('wisp', 0xa4b4c4, 0x687c8e),
    'extreme_clean_dry_air': ('wisp', 0xc6d6e4, 0x8498ac),
    'tetrafluoromethane': ('wisp', 0xc2ccd2, 0x7e8a92),
    'hafnium_tetrachloride': ('fog', 0xa6aea4, 0x626a5e),
    'krf_excimer_gas': ('glow', 0xa896c8, 0x5c4c7c),
    'arf_excimer_gas': ('glow', 0x92aad0, 0x4c627e),
    'etch_plasma_gas': ('glow', 0x8c70b8, 0x44306c),
}
GASES = ('wisp', 'fog', 'glow')
VIOLET = (0x7c, 0x4e, 0xd8)   # fluix's ender half
SPARK = (0xff, 0x60, 0x4a)    # fluix's redstone half


def rgb(c):
    return ((c >> 16) & 255, (c >> 8) & 255, c & 255)


def hash2(x, y, s=0):
    n = (x * 374761393 + y * 668265263 + s * 2147483647) & 0xFFFFFFFF
    n = ((n ^ (n >> 13)) * 1274126177) & 0xFFFFFFFF
    return ((n ^ (n >> 16)) & 0xFFFF) / 65535.0


def _facets(x, y, seed):
    """Facets of a tiling 4 x 4 voronoi: (a brightness per facet 0..1, how near the pixel is to a facet's border)."""
    pts = [((i + 0.2 + 0.6 * hash2(i, j, seed)) * 4, (j + 0.2 + 0.6 * hash2(i, j, seed + 3)) * 4, hash2(i, j, seed + 9))
           for i in range(4) for j in range(4)]
    best = []
    for px_, py_, h in pts:
        dx, dy = abs(x + 0.5 - px_), abs(y + 0.5 - py_)
        dx, dy = min(dx, 16 - dx), min(dy, 16 - dy)
        best.append((dx * dx + dy * dy, h))
    best.sort()
    return best[0][1], max(0.0, 1.0 - (best[1][0] ** 0.5 - best[0][0] ** 0.5) / 1.6)


def field(style, x, y, p, seed):
    """Value 0..1 at pixel x, y and phase p (0..TAU); every wave number is an integer so the texture tiles and loops."""
    u, v = TAU * x / 16, TAU * y / 16
    s = seed * 0.7
    if style == 'calm':
        w = math.sin(2 * u + v + p + s) + 0.7 * math.sin(v - 3 * u - p) + 0.4 * math.sin(2 * v + u + 2 * p)
        return 0.5 + w / 4.2
    if style == 'viscous':
        w = math.sin(u + v + p + s) + 0.6 * math.sin(2 * v - u - p) + 0.25 * math.sin(3 * u + 2 * v + p)
        return 0.5 + w / 3.4
    if style == 'clear':
        a = math.sin(2 * u + v + p + s)
        b = math.sin(u - 2 * v + p)
        ridge = 1 - abs(math.sin((a + b) * 1.3))  # caustic lines
        return 0.35 + 0.55 * ridge ** 3 + 0.1 * (a + b) / 4
    if style == 'murky':
        w = math.sin(u + 2 * v + p + s) + math.sin(2 * u - v - p) + 0.5 * math.sin(3 * v + 2 * u + p)
        return 0.5 + w / 5 + (hash2(x, y, seed) - 0.5) * 0.18
    if style == 'bubbly':
        w = math.sin(2 * u + v + p + s) + 0.6 * math.sin(v - 2 * u - p)
        return 0.5 + w / 3.6
    if style == 'icy':
        w = math.sin(2 * u + v + p + s) + 0.6 * math.sin(2 * v - 3 * u + p)
        crystal = max(0.0, math.sin(3 * u + 2 * v + s)) * max(0.0, math.sin(2 * u - 3 * v))
        return 0.42 + w / 5 + 0.3 * crystal
    if style == 'resonance':  # fluix: faceted liquid crystal, a sheen sweeping over the facets (no waves, unlike the plasmas)
        cell, edge = _facets(x, y, seed)
        sweep = max(0.0, math.sin(u + v - p)) ** 3
        return max(0.0, min(1.0, 0.25 + 0.4 * cell + 0.35 * sweep * (0.4 + cell) - 0.3 * edge))
    # gases
    if style == 'fog':
        w = math.sin(u + v - p + s) + 0.8 * math.sin(2 * v + u - p) + 0.4 * math.sin(3 * u - v + 2 * p)
        return 0.5 + w / 4.4
    w = math.sin(2 * u + v + p + s) + 0.8 * math.sin(v - 2 * u + 2 * p) + 0.5 * math.sin(3 * v + u - p)
    return 0.5 + w / 4.6


def frame(name, style, light, dark, f):
    seed = sum(ord(c) for c in name) % 97
    p = TAU * f / FRAMES
    lo, hi = rgb(dark), rgb(light)
    img = Image.new('RGBA', (16, 16))
    px = img.load()
    for y in range(16):
        for x in range(16):
            t = max(0.0, min(1.0, field(style, x, y, p, seed)))
            c = [lo[i] + (hi[i] - lo[i]) * t for i in range(3)]
            a = 255
            if style in GASES:
                a = int(120 + 95 * t)
                if style == 'glow':  # faint, slow flicker of the discharge: a few pixels brighten and fade
                    h = hash2(x, y, seed)
                    if h > 0.9:
                        k = 0.5 + 0.5 * math.sin(p * (1 + int(h * 100) % 2) + h * 40)
                        c = [c[i] + (255 - c[i]) * 0.35 * k for i in range(3)]
                        a = min(255, a + 30)
            if style == 'icy':
                h = hash2(x, y, seed + 5)
                if h > 0.93:
                    k = max(0.0, math.sin(p + h * 50)) ** 4
                    c = [c[i] + (255 - c[i]) * 0.55 * k for i in range(3)]
            if style == 'resonance':
                cell, edge = _facets(x, y, seed)
                # facets lean violet or teal; the borders stay deep indigo; a few motes of light drift up the column
                mix_to = VIOLET if cell > 0.5 else (0x4c, 0xd8, 0xc8)
                c = [c[i] + (mix_to[i] - c[i]) * 0.3 for i in range(3)]
                h = hash2(x, y, seed + 7)
                if h > 0.9 and edge < 0.5:
                    k = max(0.0, math.sin(p + h * 60)) ** 3
                    c = [c[i] + (255 - c[i]) * 0.7 * k for i in range(3)]
            px[x, y] = tuple(max(0, min(255, int(v))) for v in c) + (a,)
    if style == 'bubbly':
        d = ImageDraw.Draw(img)
        for i in range(3):
            bx = int(hash2(i, 1, seed) * 16)
            by = int(hash2(i, 2, seed) * 16)
            y = (by - f // 2) % 16  # rises one tile per loop
            px = img.load()
            px[bx, y] = tuple(min(255, int(v + (255 - v) * 0.6)) for v in px[bx, y][:3]) + (255,)
            px[(bx + 1) % 16, y] = tuple(min(255, int(v + (255 - v) * 0.25)) for v in px[(bx + 1) % 16, y][:3]) + (255,)
    return img


def sheet(name):
    style, light, dark = TEXTURES[name]
    out = Image.new('RGBA', (16, 16 * FRAMES))
    for f in range(FRAMES):
        out.paste(frame(name, style, light, dark, f), (0, 16 * f))
    return out


def main():
    os.makedirs(FLUIDS, exist_ok=True)
    for name in TEXTURES:
        path = os.path.join(FLUIDS, 'fluid.%s.png' % name)
        sheet(name).save(path)
        with open(path + '.mcmeta', 'w', newline='\n') as out:
            json.dump({'animation': {'frametime': 3, 'interpolate': True}}, out, indent=2)
            out.write('\n')
    print('%d textures' % len(TEXTURES))
    if '--sheet' in sys.argv:
        preview(sys.argv[sys.argv.index('--sheet') + 1])


def preview(path, scale=6, cols=6, shown=(0, 8, 16, 24)):
    """A contact sheet: every texture as a 3 x 3 tiling on a dark panel, with its name; four animation frames side by side."""
    names = list(TEXTURES)
    tile = 16 * scale
    rows = (len(names) + cols - 1) // cols
    w = cols * (tile * 2 + 12) + 12
    h = rows * (tile * 2 + 34) + 12
    img = Image.new('RGB', (w, h), (26, 28, 34))
    d = ImageDraw.Draw(img)
    for i, name in enumerate(names):
        cx = 12 + (i % cols) * (tile * 2 + 12)
        cy = 12 + (i // cols) * (tile * 2 + 34)
        full = sheet(name)
        panel = Image.new('RGBA', (tile * 2, tile * 2), (40, 44, 52, 255))
        for ty in range(2):
            for tx in range(2):
                fr = full.crop((0, 16 * shown[(ty * 2 + tx) % len(shown)], 16, 16 * shown[(ty * 2 + tx) % len(shown)] + 16))
                panel.alpha_composite(fr.resize((tile, tile), Image.NEAREST), (tx * tile, ty * tile))
        img.paste(panel.convert('RGB'), (cx, cy + 16))
        kind = 'gas' if TEXTURES[name][0] in GASES else 'liquid'
        d.text((cx, cy), '%s (%s)' % (name, kind), fill=(220, 224, 232))
    img.save(path)


if __name__ == '__main__':
    main()
