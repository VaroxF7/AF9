#!/usr/bin/env python3
"""The superalloys' icon sets: Kovar, Inconel 625 / 718 / 738, Havar, Hastelloy C-276, Stellite 1 / 12.

Every alloy gets a palette and a grain of its own, painted over the silhouettes and shading of GT's SHINY set
(dust, ingot, plate, double plate, dense plate, fine wire, screw, bolt, rod, ring, rotor ...), so the parts keep their
GT shapes but no two alloys look alike:

  kovar        blued steel, glass-bead sheen, fine horizontal lines
  inconel_625  teal steel, diagonal weave
  inconel_718  bronze-brown, amber temper speckles
  inconel_738  charcoal with orange casting glints, cross-hatch (a turbine blade casting)
  havar        warm gold-cobalt, vertical hairlines, burgundy flecks
  hastelloy    crimson with silver rivet dots
  stellite_1   icy silver, dark carbide specks, mirror streaks
  stellite_12  polished cobalt blue, bright streaks, cyan sparkle

Run from the repository root (needs Pillow and a checkout of GregTechCEu/GregTech-Modern v7.5.3-1.20.1):
    GT_SRC=/path/to/GregTech-Modern python3 tools/textures/superalloys.py

Writes into af9-core/src/main/resources/assets/gtceu:
  textures/item/material_sets/<alloy>/<shape>_overlay.png (16 x 16, static; colour baked in) + empty.png
  models/item/material_sets/<alloy>/<shape>.json
The GT shapes it reads are LGPL-3.0 (see CREDITS-textures.md).
"""
import json
import os
import sys

from PIL import Image

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), '../..')
RES = os.path.join(ROOT, 'af9-core/src/main/resources/assets/gtceu')
GT_SRC = os.environ.get('GT_SRC', '/tmp/gtsrc/gt')
SHINY = os.path.join(GT_SRC, 'src/main/resources/assets/gtceu/textures/item/material_sets/shiny')

SHAPES = ['dust', 'dust_small', 'dust_tiny', 'ingot', 'ingot_hot', 'ingot_double', 'nugget', 'plate', 'plate_double',
          'plate_dense', 'foil', 'wire_fine', 'screw', 'bolt', 'rod', 'rod_long', 'ring', 'rotor', 'turbine_blade']


def rgb(h):
    return ((h >> 16) & 255, (h >> 8) & 255, h & 255, 255)


def ramp(*hexes):
    """Five tones, dark to bright."""
    return [rgb(h) for h in hexes]


def noise(x, y, salt):
    return ((x * 73856093) ^ (y * 19349663) ^ (salt * 83492791)) % 100


# alloy -> palette (outline, shadow, mid, light, highlight), hot palette, accents, grain
ALLOYS = {
    'kovar': dict(pal=ramp(0x1c2a40, 0x34496b, 0x5d7799, 0x8ea6c4, 0xcfdcee),
                  accent=[rgb(0xb9a8e8), rgb(0x9fd0f0)], salt=1),
    'inconel_625': dict(pal=ramp(0x10262c, 0x24464f, 0x3f6f7b, 0x70a3ae, 0xc4e3e8),
                        accent=[rgb(0xa8f0e8), rgb(0x1d3a42)], salt=2),
    'inconel_718': dict(pal=ramp(0x2a1a10, 0x573a26, 0x8a6240, 0xbd9468, 0xf0d6a8),
                        accent=[rgb(0xe8a23a), rgb(0x3a2214)], salt=3),
    'inconel_738': dict(pal=ramp(0x0a0c10, 0x1e232c, 0x373e4b, 0x5b6678, 0xa4b0c4),
                        accent=[rgb(0xff7a2a), rgb(0xc2301a)], salt=4),
    'havar': dict(pal=ramp(0x2e1a08, 0x6b4a14, 0xb08a2c, 0xe0be5a, 0xfff0b0),
                  accent=[rgb(0x7a2a2a), rgb(0xc86a4a)], salt=5),
    'hastelloy': dict(pal=ramp(0x2a0808, 0x6a1616, 0xa82a2a, 0xd45a52, 0xffc4b8),
                      accent=[rgb(0xe6e8ee), rgb(0x4a0e0e)], salt=6),
    'stellite_1': dict(pal=ramp(0x2a3040, 0x6a7690, 0xaab6cc, 0xdbe4f2, 0xffffff),
                       accent=[rgb(0x1c2236), rgb(0x8fc0ff)], salt=7),
    'stellite_12': dict(pal=ramp(0x08163a, 0x143a80, 0x2a68c4, 0x5aa0f0, 0xc0e6ff),
                        accent=[rgb(0x9af4ff), rgb(0xffffff)], salt=8),
}
HOT = ramp(0x4a1c0a, 0x9a3c14, 0xe0701c, 0xffb040, 0xfff0c0)


def grain(name, x, y, light, spec):
    """The alloy's own surface: returns a replacement colour or None."""
    a1, a2 = spec['accent']
    n = noise(x, y, spec['salt'])
    pal = spec['pal']
    if name == 'kovar':
        if y % 3 == 0:
            return pal[min(4, tone(light) + 1)]
        if n < 9:
            return a1 if n % 2 else a2
    elif name == 'inconel_625':
        if (x + y) % 4 == 0:
            return pal[min(4, tone(light) + 1)]
        if (x - y) % 4 == 0:
            return pal[max(0, tone(light) - 1)]
        if n < 4:
            return a1
    elif name == 'inconel_718':
        if n < 12:
            return a1 if n < 5 else a2
    elif name == 'inconel_738':
        if (x + y) % 5 == 0 and (x - y) % 5 != 0:
            return pal[max(0, tone(light) - 1)]
        if n < 10:
            return a1 if n < 6 else a2
    elif name == 'havar':
        if x % 3 == 0:
            return pal[min(4, tone(light) + 1)]
        if n < 7:
            return a1 if n < 4 else a2
    elif name == 'hastelloy':
        if x % 4 == 1 and y % 4 == 1:
            return a1
        if n < 5:
            return a2
    elif name == 'stellite_1':
        if (x + y) % 6 == 0:
            return pal[4]
        if n < 10:
            return a1 if n < 7 else a2
    elif name == 'stellite_12':
        if (x - y) % 6 == 0:
            return pal[min(4, tone(light) + 2)]
        if n < 6:
            return a1 if n < 3 else a2
    return None


def hot(pal):
    """The ingot fresh out of the EBF: a glow, still a quarter of the alloy's own colour."""
    return [tuple(int(h * 0.72 + a * 0.28) for h, a in zip(hp, ap[:3])) + (255,) for hp, ap in zip(HOT, pal)]


def tone(light):
    return max(0, min(4, int(light * 5)))


def luminance(px):
    return (px[0] + px[1] + px[2]) / 3.0


def sprite(shape, name, spec):
    base = Image.open(os.path.join(SHINY, shape + '.png')).convert('RGBA')
    sec_path = os.path.join(SHINY, shape + '_secondary.png')
    ovl_path = os.path.join(SHINY, shape + '_overlay.png')
    sec = Image.open(sec_path).convert('RGBA') if os.path.exists(sec_path) else None
    ovl = Image.open(ovl_path).convert('RGBA') if os.path.exists(ovl_path) else None
    out = Image.new('RGBA', base.size, (0, 0, 0, 0))
    lums = [luminance(base.getpixel((x, y))) for y in range(base.size[1]) for x in range(base.size[0])
            if base.getpixel((x, y))[3]]
    if not lums:
        return out
    lo, hi = min(lums), max(lums)
    span = max(1.0, hi - lo)
    pal = hot(spec['pal']) if shape == 'ingot_hot' else spec['pal']
    for y in range(base.size[1]):
        for x in range(base.size[0]):
            p = base.getpixel((x, y))
            if not p[3]:
                continue
            light = (luminance(p) - lo) / span
            t = tone(min(0.999, light))
            col = pal[t]
            if shape != 'ingot_hot':
                g = grain(name, x, y, light, spec)
                if g is not None and t > 0:
                    col = g
            if sec is not None and sec.getpixel((x, y))[3] and t > 0:
                col = pal[max(1, t - 1)]
            if ovl is not None and ovl.getpixel((x, y))[3]:
                col = pal[4]
            if t == 0:
                col = pal[0]
            out.putpixel((x, y), col)
    if ovl is not None:
        # Overlay pixels outside the base silhouette are shape in their own right: the spool of wire_fine and
        # the glow of ingot_hot. Dropping them left the fine wire as a bare thread with no spool.
        olums = [luminance(ovl.getpixel((x, y))) for y in range(ovl.size[1]) for x in range(ovl.size[0])
                 if ovl.getpixel((x, y))[3] and not base.getpixel((x, y))[3]]
        if olums:
            olo = min(olums)
            ospan = max(1.0, max(olums) - olo)
            for y in range(ovl.size[1]):
                for x in range(ovl.size[0]):
                    p = ovl.getpixel((x, y))
                    if p[3] and not base.getpixel((x, y))[3]:
                        light = (luminance(p) - olo) / ospan
                        t = tone(min(0.999, light))
                        g = grain(name, x, y, light, spec) if shape != 'ingot_hot' else None
                        out.putpixel((x, y), g if g is not None and t > 0 else pal[t])
    return out


def main():
    if not os.path.isdir(SHINY):
        sys.exit('GT_SRC does not point to a GregTech-Modern checkout: ' + SHINY)
    for name, spec in ALLOYS.items():
        items = os.path.join(RES, 'textures/item/material_sets', name)
        models = os.path.join(RES, 'models/item/material_sets', name)
        for folder in (items, models):
            os.makedirs(folder, exist_ok=True)
        for shape in SHAPES:
            if not os.path.exists(os.path.join(SHINY, shape + '.png')):
                continue
            sprite(shape, name, spec).save(os.path.join(items, shape + '_overlay.png'))
            with open(os.path.join(models, shape + '.json'), 'w', newline='\n') as out:
                json.dump({'parent': 'item/generated', 'textures': {
                    'layer0': 'gtceu:item/material_sets/%s/empty' % name,
                    'layer1': 'gtceu:item/material_sets/%s/empty' % name,
                    'layer2': 'gtceu:item/material_sets/%s/%s_overlay' % (name, shape)}}, out, indent=2)
                out.write('\n')
        Image.new('RGBA', (16, 16), (0, 0, 0, 0)).save(os.path.join(items, 'empty.png'))
    print('superalloy textures written')


if __name__ == '__main__':
    main()
