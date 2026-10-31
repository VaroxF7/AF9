#!/usr/bin/env python3
"""Draws the Optically Reflective Wafer family (the 20 nm substrate, id transmuted_neutronium) and the Optical TPU / Memristor chips.

    python3 tools/textures/optical_wafers.py     (from the repository root; needs Pillow; run coated_wafers.py afterwards)

The look of GTNH's optical parts: the blank is a dark-outlined disc of gunmetal with an amber glow and purple fibre sheen
across the face and a sharp warm glint. The chip wafers and chips keep their old shapes and glyphs, re-lit with the same mirror
ramp, and carry a glowing waveguide accent (amber for the TPU, green for the memristor). Broken and contaminated variants copy
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
RAMP = [(10, 8, 16), (30, 28, 42), (62, 60, 80), (104, 100, 124), (168, 160, 176), (236, 226, 214)]
CYAN, VIOLET = (255, 150, 30), (150, 70, 225)   # amber glow, purple fibre: the GTNH optical palette


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
    return lerp(CYAN, VIOLET, 0.5 + 0.5 * math.sin(t * 6.0 + 0.6))


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
            c = (255, 236, 180)
        elif abs(s + 5.6) < 0.5 and d < 6.0:
            c = lerp(c, (255, 214, 120), 0.6)
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


GUN = [(8, 7, 14), (34, 33, 46), (58, 58, 76), (88, 90, 112), (126, 130, 156)]   # gunmetal, outline to light
AMBER = [(120, 46, 8), (222, 104, 14), (255, 160, 32), (255, 214, 96)]
PURPLE = [(70, 26, 120), (140, 62, 214), (196, 130, 255)]
GREEN = [(20, 110, 52), (46, 190, 84), (130, 255, 150)]


def disc(old):
    """the wafer: a dark-outlined gunmetal disc, an amber ring of cells, purple fibre strands, a warm glint"""
    out = Image.new('RGBA', old.size, (0, 0, 0, 0))
    w, h = old.size
    solid = [(x, y) for y in range(h) for x in range(w) if old.getpixel((x, y))[3]]
    cx = sum(x for x, _ in solid) / len(solid)
    cy = sum(y for _, y in solid) / len(solid)
    for (x, y) in solid:
        d = depth(old, x, y)
        s = (x - cx) + (y - cy)
        tone = 3 if s < -4 else 2 if s < 1 else 1 if s < 5 else 0.6
        c = GUN[0] if d == 1 else GUN[int(round(tone))] if tone >= 1 else GUN[1]
        r = math.hypot(x - cx, y - cy)
        if d >= 2:
            if 3.6 <= r < 4.5 and (int(math.atan2(y - cy, x - cx) * 8 / math.pi) % 2 == 0):
                c = AMBER[1] if s > -1 else AMBER[0]              # the amber ring of cells
            if r < 1.6:
                c = AMBER[2] if s < 0 else AMBER[1]               # the core
            if abs((x - cx) - (y - cy) * 0.0) < 0.5 and 1.8 < abs(y - cy) < 3.4:
                c = PURPLE[1]                                      # vertical fibre strands
            if abs(y - cy) < 0.5 and 1.8 < abs(x - cx) < 3.4:
                c = PURPLE[1] if s > 0 else PURPLE[2]
        if d == 2 and s < -3:
            c = GUN[4]                                             # rim light, upper left
        out.putpixel((x, y), c + (255,))
    for (gx, gy) in ((-3, -3), (-4, -2), (-2, -4)):
        gxp, gyp = int(round(cx + gx)), int(round(cy + gy))
        if out.getpixel((gxp, gyp))[3] and depth(old, gxp, gyp) >= 2:
            out.putpixel((gxp, gyp), AMBER[3] + (255,))
    return out


def chip(old, cells, wafer=False):
    """a chip: gunmetal body, 2x2 glowing cells joined by purple traces, silver pins"""
    out = Image.new('RGBA', old.size, (0, 0, 0, 0))
    w, h = old.size
    solid = [(x, y) for y in range(h) for x in range(w) if old.getpixel((x, y))[3]]
    cx = sum(x for x, _ in solid) / len(solid)
    cy = sum(y for _, y in solid) / len(solid)
    for (x, y) in solid:
        d = depth(old, x, y)
        s = (x - cx) + (y - cy)
        c = GUN[0] if d == 1 else GUN[3] if (d == 2 and s < 0) else GUN[1] if d == 2 else GUN[2] if s < 0 else GUN[1]
        out.putpixel((x, y), c + (255,))
    ox, oy = int(round(cx)) - 3, int(round(cy)) - 3
    for i, (dx, dy) in enumerate(((0, 0), (4, 0), (0, 4), (4, 4))):
        col = cells[i % len(cells)]
        for ax in range(3):
            for ay in range(3):
                v = col[-1] if (ax == 0 and ay == 0) else col[-2] if (ax + ay < 3) else col[-3] if (ax + ay < 4) else col[0]
                out.putpixel((ox + dx + ax - 0, oy + dy + ay - 0), v + (255,))
    for k in range(3):                                           # purple traces between the cells
        out.putpixel((ox + 3, oy + 1 + 4 * (k // 2) + (k % 2) * 0), PURPLE[1] + (255,))
        out.putpixel((ox + 1 + (k % 2) * 4, oy + 3), PURPLE[1] + (255,))
    out.putpixel((ox + 3, oy + 5), PURPLE[1] + (255,))
    return out


def main():
    """the final look: dark prism glass with the E1 soft-arc edge reflection (optical_edge.py), glyph per chip"""
    import optical_edge as oe
    if not os.path.isdir(SRC):
        os.makedirs(os.path.join(SRC, 'wafers'))
        os.makedirs(os.path.join(SRC, 'chips'))
        for f in FILES:
            shutil.copy(os.path.join(ITEM, f + '.png'), os.path.join(SRC, f + '.png'))
    old = {f: Image.open(os.path.join(SRC, f + '.png')).convert('RGBA') for f in FILES}
    mode = 'E1 Soft arc'
    new = {}
    base = oe.draw(mode, 'wafer')
    new['wafers/transmuted_neutronium_wafer'] = base
    for dmg in ('broken', 'contaminated'):
        new[f'wafers/{dmg}_transmuted_neutronium_wafer'] = damage(
            base, old['wafers/transmuted_neutronium_wafer'], old[f'wafers/{dmg}_transmuted_neutronium_wafer'])
    for name in ('tpu', 'memristor'):
        chip_im = oe.draw(mode, 'chip', name)
        new[f'wafers/{name}_wafer'] = oe.draw(mode, 'wafer', name)
        new[f'chips/{name}_chip'] = chip_im
        new[f'chips/contaminated_{name}_chip'] = damage(chip_im, old[f'chips/{name}_chip'], old[f'chips/contaminated_{name}_chip'])
    for f, im in new.items():
        im.save(os.path.join(ITEM, f + '.png'))
        print('wrote', f)


main()
