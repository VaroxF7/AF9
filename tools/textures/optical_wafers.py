#!/usr/bin/env python3
"""Draws the Optically Reflective Wafer family (the 20 nm substrate, id transmuted_neutronium) and the Optical TPU / Memristor chips.

    python3 tools/textures/optical_wafers.py     (from the repository root; needs Pillow; run coated_wafers.py afterwards)

A dielectric mirror: the blank is a dark-outlined disc of cold chrome with a thin-film sheen that slides from cyan to violet
across the face and a sharp white glint. The chip wafers and chips keep their old shapes and glyphs, re-lit with the same mirror
ramp, and carry a glowing waveguide accent (cyan for the TPU, magenta for the memristor). Broken and contaminated variants copy
the damage (cracks, stains) of the previous textures onto the new base. Reads the previous textures from git (HEAD~ files
saved under tools/textures/optical_src when first run); safe to run again.
"""
import math
import os
import shutil

from PIL import Image

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), '../..')
ITEM = os.path.join(ROOT, 'af9-core/src/main/resources/assets/af9/textures/item')
SRC = os.path.join(os.path.dirname(os.path.abspath(__file__)), 'optical_src')
FILES = ['wafers/transmuted_neutronium_wafer', 'wafers/broken_transmuted_neutronium_wafer',
         'wafers/contaminated_transmuted_neutronium_wafer', 'wafers/tpu_wafer', 'wafers/memristor_wafer',
         'chips/tpu_chip', 'chips/memristor_chip', 'chips/contaminated_tpu_chip', 'chips/contaminated_memristor_chip']

# chrome ramp, dark to bright
RAMP = [(14, 16, 40), (34, 40, 86), (66, 82, 142), (112, 140, 192), (170, 204, 232), (232, 246, 255)]
CYAN, VIOLET = (60, 235, 255), (170, 96, 255)


def lerp(a, b, t):
    return tuple(a[i] + (b[i] - a[i]) * t for i in range(3))


def ramp(v):
    v = max(0.0, min(1.0, v)) * (len(RAMP) - 1)
    i = min(int(v), len(RAMP) - 2)
    return lerp(RAMP[i], RAMP[i + 1], v - i)


def luma(p):
    return (0.3 * p[0] + 0.59 * p[1] + 0.11 * p[2]) / 255


def film(x, y, w, h):
    """cyan -> violet across the diagonal, banded like a thin film"""
    t = (x + y) / (w + h - 2)
    return lerp(CYAN, VIOLET, 0.5 + 0.5 * math.sin(t * 5.2 + 0.6))


def depth(img, x, y):
    """steps from the silhouette's edge (1 = edge pixel)"""
    w, h = img.size
    for d in range(1, 4):
        for dx in range(-d, d + 1):
            for dy in range(-d, d + 1):
                nx, ny = x + dx, y + dy
                if not (0 <= nx < w and 0 <= ny < h) or img.getpixel((nx, ny))[3] == 0:
                    return d
    return 4


def remap(img, sheen, accent=None, glyph_floor=0.36):
    out = img.copy()
    w, h = img.size
    for y in range(h):
        for x in range(w):
            p = img.getpixel((x, y))
            if p[3] == 0:
                continue
            l = luma(p)
            base = ramp(0.04 + 0.96 * (l ** 1.6))
            f = film(x, y, w, h)
            c = lerp(base, f, sheen * (0.25 + 0.55 * l))
            d = depth(img, x, y)
            if d == 1:
                c = lerp(RAMP[0], base, 0.15)                 # the dark outline
            elif accent and l < glyph_floor and d >= 3:
                c = accent                                    # the glowing waveguide
            out.putpixel((x, y), tuple(int(max(0, min(255, v))) for v in c) + (p[3],))
    return out


def blank(old):
    out = remap(old, 0.5)
    w, h = out.size
    solid = [(x, y) for y in range(h) for x in range(w) if old.getpixel((x, y))[3]]
    cx = sum(x for x, _ in solid) / len(solid)
    cy = sum(y for _, y in solid) / len(solid)
    for (x, y) in solid:
        d = math.hypot(x - cx, y - cy)
        s = (x - cx) + (y - cy)
        p = out.getpixel((x, y))
        c = p[:3]
        if abs(s + 3.0) < 0.9 and d < 6.2:       # the sharp glint
            c = (250, 253, 255)
        elif abs(s + 5.6) < 0.5 and d < 6.0:
            c = lerp(c, (255, 255, 255), 0.6)
        elif abs(d - 3.4) < 0.45 and s > -1:      # a faint engraved target ring
            c = lerp(c, RAMP[1], 0.45)
        out.putpixel((x, y), tuple(int(v) for v in c) + (p[3],))
    return out


def damage(new_base, old_base, old_dmg):
    """copy what differs between the old clean and the old damaged texture onto the new base"""
    out = new_base.copy()
    for y in range(out.height):
        for x in range(out.width):
            if old_dmg.getpixel((x, y)) != old_base.getpixel((x, y)) and old_dmg.getpixel((x, y))[3]:
                out.putpixel((x, y), old_dmg.getpixel((x, y)))
    return out


def main():
    if not os.path.isdir(SRC):
        os.makedirs(os.path.join(SRC, 'wafers'))
        os.makedirs(os.path.join(SRC, 'chips'))
        for f in FILES:
            shutil.copy(os.path.join(ITEM, f + '.png'), os.path.join(SRC, f + '.png'))
    old = {f: Image.open(os.path.join(SRC, f + '.png')).convert('RGBA') for f in FILES}
    new = {}
    new['wafers/transmuted_neutronium_wafer'] = blank(old['wafers/transmuted_neutronium_wafer'])
    new['wafers/broken_transmuted_neutronium_wafer'] = damage(
        new['wafers/transmuted_neutronium_wafer'], old['wafers/transmuted_neutronium_wafer'],
        old['wafers/broken_transmuted_neutronium_wafer'])
    new['wafers/contaminated_transmuted_neutronium_wafer'] = damage(
        new['wafers/transmuted_neutronium_wafer'], old['wafers/transmuted_neutronium_wafer'],
        old['wafers/contaminated_transmuted_neutronium_wafer'])
    for chip, acc, wafer_clean in (('tpu', CYAN, True), ('memristor', (255, 90, 220), True)):
        nw = remap(old[f'wafers/{chip}_wafer'], 0.45, acc)
        nc = remap(old[f'chips/{chip}_chip'], 0.45, acc)
        new[f'wafers/{chip}_wafer'] = nw
        new[f'chips/{chip}_chip'] = nc
        new[f'chips/contaminated_{chip}_chip'] = damage(nc, old[f'chips/{chip}_chip'], old[f'chips/contaminated_{chip}_chip'])
    for f, im in new.items():
        im.save(os.path.join(ITEM, f + '.png'))
        print('wrote', f)


main()
