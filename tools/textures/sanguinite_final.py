#!/usr/bin/env python3
"""Writes Sanguinite's animated icon set (Heartbeat): a saturated blood-red body whose gradient slowly drifts, a gentle
pulse and a soft shimmer band; dark outline. 16 frames per part, vertical strip + .mcmeta, in the style of GTNH's Infinity.

    GT_SRC=/path/to/GregTech-Modern python3 tools/textures/sanguinite_final.py     (from the repository root)
"""
import json
import math
import os
import superalloys as sa
from PIL import Image

N = 16
ST = [(100, 8, 26), (232, 16, 44), (255, 52, 66), (232, 16, 44)]   # deep blood, vivid red, hot red


def grad(u):
    u = (u % 1.0) * len(ST)
    i = int(u) % len(ST)
    t = u - int(u)
    a, b = ST[i], ST[(i + 1) % len(ST)]
    return [a[k] + (b[k] - a[k]) * t for k in range(3)]


def frame(shape, f):
    base = Image.open(os.path.join(sa.SHINY, shape + '.png')).convert('RGBA')
    ovl_p = os.path.join(sa.SHINY, shape + '_overlay.png')
    ovl = Image.open(ovl_p).convert('RGBA') if os.path.exists(ovl_p) else None
    out = Image.new('RGBA', base.size, (0, 0, 0, 0))
    lums = [sa.luminance(base.getpixel((x, y))) for y in range(16) for x in range(16) if base.getpixel((x, y))[3]]
    lo, hi = min(lums), max(lums)
    span = max(1.0, hi - lo)
    ph = f / N
    for y in range(16):
        for x in range(16):
            p = base.getpixel((x, y))
            from_ovl = False
            if not p[3] and ovl is not None and ovl.getpixel((x, y))[3]:
                p, from_ovl = ovl.getpixel((x, y)), True
            if not p[3]:
                continue
            light = max(0.0, min(1.0, (sa.luminance(p) - lo) / span)) if not from_ovl else 0.5
            if light < 0.12 and not from_ovl:
                out.putpixel((x, y), (18, 2, 8, 255))
                continue
            col = grad((x + y) / 24.0 - ph)
            col = [c * (0.94 + 0.06 * math.sin(2 * math.pi * ph)) for c in col]      # a faint pulse
            shade = 0.52 + 0.7 * light ** 1.1                                         # brighter body: the red pops
            col = [c * shade for c in col]
            d = ((x + y) / 31.0 - ph) % 1.0
            band = max(0.0, 1 - abs(d - 0.5) * 9) * 0.22                              # a soft shimmer
            col = [c + (255 - c) * band * (0.4 + 0.6 * light) for c in col]
            if light > 0.92:
                col = [c + (255 - c) * 0.3 for c in col]
            out.putpixel((x, y), tuple(int(max(0, min(255, c))) for c in col) + (255,))
    return out


def main():
    items = os.path.join(sa.RES, 'textures/item/material_sets/sanguinite')
    models = os.path.join(sa.RES, 'models/item/material_sets/sanguinite')
    for shape in sa.SHAPES:
        if not os.path.exists(os.path.join(sa.SHINY, shape + '.png')):
            continue
        strip = Image.new('RGBA', (16, 16 * N))
        for f in range(N):
            strip.paste(frame(shape, f), (0, 16 * f))
        strip.save(os.path.join(items, shape + '_overlay.png'))
        with open(os.path.join(items, shape + '_overlay.png.mcmeta'), 'w', newline='\n') as m:
            json.dump({'animation': {'frametime': 3}}, m)
            m.write('\n')
    print('sanguinite animated set written')
    # preview
    S = 8
    sheet = Image.new('RGBA', (9 * (16 * S + 6), 16 * S), (30, 33, 40, 255))
    for c, shape in enumerate(['dust', 'ingot', 'plate', 'foil', 'rod', 'wire_fine', 'nugget', 'screw', 'rotor']):
        sheet.alpha_composite(frame(shape, 5).resize((16 * S, 16 * S), Image.NEAREST), (c * (16 * S + 6), 0))
    sheet.save('/tmp/claude-0/s/sang_final.png')


main()
