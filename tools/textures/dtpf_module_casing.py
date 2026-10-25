#!/usr/bin/env python3
"""Four versions of the DTPF's Inconel 738 Module Casing, all after GregTech CEu's Atomic Casing (gcym/atomic_casing, the
placeholder of the structure): square frame inside square frame, a dark groove, a panel and a round nucleus in the middle.

  1  Atomic Blue    the Atomic Casing itself, its purple moved to the hall's Inconel blue, the nucleus a lit ice blue
  2  Lit Frame      the same layout, the frame a brushed steel bevel, a blue line lit in the groove, a white-hot nucleus
  3  Electron Ring  the nucleus on a thin orbit with an electron on each diagonal, rust rivets in the corners
  4  Hot Core       a chamfered recess with an amber core (Inconel's rust) under blue panels

    python3 tools/textures/dtpf_module_casing.py --preview <dir>     writes the four sprites and the preview sheet
    python3 tools/textures/dtpf_module_casing.py --apply <1-4>       writes the chosen one as the block's texture

The chosen casing connects to its neighbours (LDLib CTM) but keeps its nucleus in every block.
"""
import math
import os
import sys

from PIL import Image, ImageDraw

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
import dtpf_blocks as db  # noqa: E402
import space_elevator as se  # noqa: E402

SRC = os.path.join(HERE, 'gt_src', 'atomic_casing.png')
NAME = 'dtpf_module_casing'

RUST = (194, 86, 42)
ICE = (110, 225, 255)
BLUE = (30, 120, 210)
WHITE = (240, 250, 255)
AMBER = (255, 170, 70)


def mix(a, b, t):
    t = max(0.0, min(1.0, t))
    return tuple(round(a[i] + (b[i] - a[i]) * t) for i in range(3))


def ramp(stops, t):
    t = max(0.0, min(1.0, t))
    for (t0, c0), (t1, c1) in zip(stops, stops[1:]):
        if t <= t1:
            return mix(c0, c1, (t - t0) / (t1 - t0) if t1 > t0 else 0)
    return stops[-1][1]


INCONEL = [(0.0, (10, 15, 28)), (0.35, (24, 38, 64)), (0.7, (44, 70, 112)), (1.0, (84, 124, 178))]


def atomic():
    return Image.open(SRC).convert('RGBA')


def ring(x, y):
    return min(x, y, 15 - x, 15 - y)


def dist(x, y):
    return math.hypot(x - 7.5, y - 7.5)


def new():
    return Image.new('RGBA', (16, 16), (0, 0, 0, 255))


def put(im, x, y, c):
    im.putpixel((x, y), tuple(c) + (255,))


# ----------------------------------------------------------------------------------------------------------- 1 Atomic Blue
def v1():
    src = atomic()
    out = new()
    for y in range(16):
        for x in range(16):
            r, g, b, _ = src.getpixel((x, y))
            v = (max(r, g, b) - 26) / 74.0
            c = ramp(INCONEL, v * 1.05)
            put(out, x, y, c)
    # the nucleus lit: a bright disc with a darker rim
    for y in range(16):
        for x in range(16):
            d = dist(x, y)
            if d < 1.7:
                put(out, x, y, mix(WHITE, ICE, d / 1.7 * 0.5))
            elif d < 2.7:
                put(out, x, y, mix(ICE, BLUE, (d - 1.7)))
            elif d < 3.5:
                c = out.getpixel((x, y))[:3]
                put(out, x, y, mix(c, BLUE, 0.25))
    return out


# ----------------------------------------------------------------------------------------------------------- 2 Lit Frame
def v2():
    out = new()
    for y in range(16):
        for x in range(16):
            rg = ring(x, y)
            grain = 0.5 + 0.5 * math.sin((x * 1.9 + y * 0.5) * 2.1)
            if rg == 0:
                c = (8, 12, 22)
            elif rg == 1:
                lit = x == 1 or y == 1
                c = (92, 120, 152) if lit else (30, 44, 66)
            elif rg == 2:
                c = (16, 24, 40)
            elif rg == 3:
                c = mix((20, 60, 130), BLUE, 0.5 + 0.2 * grain)        # the lit blue line
            elif rg <= 5:
                c = mix((28, 44, 72), (52, 78, 118), (rg - 4) * 0.4 + 0.12 * grain)
            else:
                c = mix((16, 24, 40), (26, 40, 66), 0.5)
            put(out, x, y, c)
    for y in range(16):
        for x in range(16):
            d = dist(x, y)
            if d < 1.4:
                put(out, x, y, WHITE)
            elif d < 2.5:
                put(out, x, y, mix(ICE, (60, 160, 230), (d - 1.4) / 1.1))
            elif d < 3.4:
                put(out, x, y, mix(out.getpixel((x, y))[:3], BLUE, 0.55 - (d - 2.5) * 0.4))
    return out


# --------------------------------------------------------------------------------------------------------- 3 Electron Ring
def v3():
    out = new()
    for y in range(16):
        for x in range(16):
            rg = ring(x, y)
            if rg == 0:
                c = (10, 14, 26)
            elif rg == 1:
                c = (74, 100, 132) if (x == 1 or y == 1) else (28, 40, 60)
            elif rg == 2:
                c = (18, 28, 46)
            else:
                grain = 0.5 + 0.5 * math.sin((x * 1.3 - y * 0.9) * 1.9)
                c = mix((26, 42, 70), (46, 72, 112), 0.35 + 0.25 * grain - (rg - 3) * 0.04)
            put(out, x, y, c)
    for (x, y) in ((3, 3), (12, 3), (3, 12), (12, 12)):
        put(out, x, y, RUST)
        put(out, x + (1 if x < 8 else -1), y + (1 if y < 8 else -1), mix(RUST, (30, 16, 10), 0.7))
    # the orbit and its electrons
    for i in range(48):
        a = math.tau * i / 48
        x, y = round(7.5 + 4.6 * math.cos(a)), round(7.5 + 4.6 * math.sin(a))
        put(out, x, y, mix(out.getpixel((x, y))[:3], BLUE, 0.55))
    for k in range(4):
        a = math.pi / 4 + k * math.pi / 2
        x, y = round(7.5 + 4.6 * math.cos(a)), round(7.5 + 4.6 * math.sin(a))
        put(out, x, y, WHITE)
        for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
            put(out, x + dx, y + dy, mix(out.getpixel((x + dx, y + dy))[:3], ICE, 0.5))
    for y in range(16):
        for x in range(16):
            d = dist(x, y)
            if d < 1.5:
                put(out, x, y, WHITE)
            elif d < 2.4:
                put(out, x, y, mix(ICE, BLUE, d - 1.5))
    return out


# -------------------------------------------------------------------------------------------------------------- 4 Hot Core
def v4():
    out = new()
    for y in range(16):
        for x in range(16):
            rg = ring(x, y)
            cheb = max(abs(x - 7.5), abs(y - 7.5))
            octa = abs(x - 7.5) + abs(y - 7.5)
            grain = 0.5 + 0.5 * math.sin((x * 1.7 + y * 0.45) * 2.3)
            if rg == 0:
                c = (8, 12, 20)
            elif rg == 1:
                c = (84, 112, 146) if (x == 1 or y == 1) else (26, 38, 58)
            elif cheb <= 4.5 and octa <= 8.0:
                # the chamfered recess: dark, shaded at the top and left (we look into it)
                shade = 0.25 if (x < 7.5 or y < 7.5) else 0.45
                c = mix((8, 12, 22), (30, 44, 70), shade + (4.5 - cheb) * 0.02)
                if cheb > 3.8 or octa > 7.2:
                    c = mix(c, (96, 128, 164) if (x >= 8 or y >= 8) else (14, 20, 34), 0.6)
            else:
                c = mix((22, 36, 62), (50, 76, 118), 0.3 + 0.25 * grain - (rg - 2) * 0.03)
            put(out, x, y, c)
    for y in range(16):
        for x in range(16):
            d = dist(x, y)
            if d < 1.3:
                put(out, x, y, WHITE)
            elif d < 2.3:
                put(out, x, y, mix(AMBER, (255, 235, 200), 0.5 - (d - 1.3) * 0.4))
            elif d < 3.1:
                put(out, x, y, mix(out.getpixel((x, y))[:3], RUST, 0.7 - (d - 2.3) * 0.5))
    # a lit blue tick at the middle of each side of the panel
    for (x, y) in ((7, 2), (8, 2), (7, 13), (8, 13), (2, 7), (2, 8), (13, 7), (13, 8)):
        put(out, x, y, mix(BLUE, ICE, 0.4))
    return out


VARIANTS = {1: ('Atomic Blue', v1), 2: ('Lit Frame', v2), 3: ('Electron Ring', v3), 4: ('Hot Core', v4)}


def preview(path):
    """Each sprite 12x, then a plate of the module's 12 casings round a controller blank, and the Atomic Casing for reference."""
    os.makedirs(path, exist_ok=True)
    cell = 12 * 16
    sheet = Image.new('RGBA', (cell * 5 + 60, cell + 3 * 16 * 6 + 80), (12, 14, 20, 255))
    d = ImageDraw.Draw(sheet)
    sprites = [('Atomic Casing (GT)', atomic())] + [('%d %s' % (k, VARIANTS[k][0]), VARIANTS[k][1]()) for k in VARIANTS]
    for i, (label, im) in enumerate(sprites):
        sheet.paste(im.resize((cell, cell), Image.NEAREST), (10 + i * (cell + 10), 24))
        d.text((10 + i * (cell + 10), 6), label, fill=(220, 230, 240, 255))
        if i:
            im.resize((cell, cell), Image.NEAREST).save(os.path.join(path, 'module_casing_%d.png' % i))
    # the plate: 5 x 3 blocks, the middle one a dark blank where the controller stands (as in the hall: 12 casings round it)
    for i, (label, im) in enumerate(sprites):
        ox, oy = 10 + i * (cell + 10), cell + 40
        for by in range(3):
            for bx in range(5):
                if bx == 2 and by == 1:
                    sheet.paste(Image.new('RGBA', (38, 38), (40, 46, 60, 255)), (ox + bx * 38, oy + by * 38))
                    continue
                sheet.paste(im.resize((38, 38), Image.NEAREST), (ox + bx * 38, oy + by * 38))
    sheet.save(os.path.join(path, 'module_casing_options.png'))
    return sheet


def inner(sprite, x, y):
    """Where a face is joined to its neighbour: the frame is gone, the groove and the panel run out to the edge (so every
    block keeps its own nucleus in a slab of casings)."""
    return sprite.getpixel((max(2, min(13, x)), max(2, min(13, y))))


def apply(n):
    """The chosen sprite as the block's texture, connected (LDLib CTM: the 2 px frame stays on the sides nothing joins)."""
    name, make = VARIANTS[n]
    se.OUT = db.BLOCK
    sprite, sheet = se.connected(NAME, make(), 2, 2, inner)
    os.makedirs('/tmp/dtpf_ctm_check', exist_ok=True)
    se.tile(sprite, sheet, 5, 3).resize((80 * 6, 48 * 6), Image.NEAREST).save('/tmp/dtpf_ctm_check/tile_%d.png' % n)
    print('applied', n, name)


if __name__ == '__main__':
    if len(sys.argv) >= 3 and sys.argv[1] == '--preview':
        preview(sys.argv[2])
    elif len(sys.argv) >= 3 and sys.argv[1] == '--apply':
        apply(int(sys.argv[2]))
    else:
        print(__doc__)
