#!/usr/bin/env python3
"""Animated Sanguinite candidates in the spirit of GTNH's Infinity: a colour gradient sweeps across each part, a bright
shimmer band travels with it, dark outline kept. Writes a preview GIF and a still sheet.

    GT_SRC=/path/to/GregTech-Modern python3 tools/textures/sanguinite_animated.py OUT_PREFIX
"""
import math
import os
import sys
import superalloys as sa
from PIL import Image

N = 16  # frames


def stops(*cols):
    return [tuple(((c >> 16) & 255, (c >> 8) & 255, c & 255)) for c in cols]


def grad(st, u):
    u = (u % 1.0) * len(st)
    i = int(u) % len(st)
    t = u - int(u)
    a, b = st[i], st[(i + 1) % len(st)]
    return [a[k] + (b[k] - a[k]) * t for k in range(3)]


V = {
    '1 Heartbeat': dict(st=stops(0x5a0814, 0xc8142e, 0xff3a4a, 0xc8142e), sweep=(1, 1), shimmer=(1.0, 0.55), pulse=True),
    '2 Blood spectrum': dict(st=stops(0xd01838, 0xe02a8a, 0x8a2ad8, 0xe0482a, 0xe8b02a, 0xd01838), sweep=(1, 1), shimmer=(1.0, 0.5)),
    '3 Ember flow': dict(st=stops(0x2a0408, 0x8a1010, 0xe04a14, 0xffb23a, 0xe04a14, 0x8a1010), sweep=(1, 0.6), shimmer=(1.0, 0.4)),
    '4 Nebula': dict(st=stops(0x1a0620, 0x7a0e3e, 0xd01e5a, 0x6a2ac8, 0x1a0620), sweep=(0.6, 1), shimmer=(1.0, 0.6), stars=True),
}


def frame(shape, spec, f):
    base = Image.open(os.path.join(sa.SHINY, shape + '.png')).convert('RGBA')
    ovl_p = os.path.join(sa.SHINY, shape + '_overlay.png')
    ovl = Image.open(ovl_p).convert('RGBA') if os.path.exists(ovl_p) else None
    out = Image.new('RGBA', base.size, (0, 0, 0, 0))
    lums = [sa.luminance(base.getpixel((x, y))) for y in range(16) for x in range(16) if base.getpixel((x, y))[3]]
    lo, hi = min(lums), max(lums)
    span = max(1.0, hi - lo)
    ph = f / N
    sx, sy = spec['sweep']
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
                out.putpixel((x, y), (14, 3, 8, 255)); continue
            u = (x * sx + y * sy) / 24.0 - ph
            col = grad(spec['st'], u)
            if spec.get('pulse'):
                col = [c * (0.8 + 0.3 * math.sin(2 * math.pi * ph)) for c in col]
            shade = 0.38 + 0.85 * light ** 1.2
            col = [c * shade for c in col]
            d = ((x + y) / 31.0 - ph * 1.0) % 1.0           # travelling shimmer band
            band = max(0.0, 1 - abs(d - 0.5) * 9) * spec['shimmer'][1]
            col = [c + (255 - c) * band * (0.4 + 0.6 * light) for c in col]
            if spec.get('stars') and sa.noise(x, y, 7 + f // 4) < 4:
                col = [255, 230, 240]
            if light > 0.92:
                col = [c + (255 - c) * 0.35 for c in col]
            out.putpixel((x, y), tuple(int(max(0, min(255, c))) for c in col) + (255,))
    return out


def main(prefix):
    shapes = ['dust', 'ingot', 'plate', 'foil', 'rod', 'wire_fine', 'nugget', 'screw', 'rotor']
    S = 6
    cw = 16 * S + 6
    gif = []
    for f in range(N):
        sheet = Image.new('RGBA', (len(shapes) * cw, len(V) * cw), (30, 33, 40, 255))
        for r, spec in enumerate(V.values()):
            for c, shape in enumerate(shapes):
                sheet.alpha_composite(frame(shape, spec, f).resize((16 * S, 16 * S), Image.NEAREST), (c * cw, r * cw))
        if f in (0, 5, 10):
            sheet.save('%s_still%d.png' % (prefix, f))
        gif.append(sheet.convert('RGB').convert('P', palette=Image.ADAPTIVE, colors=128))
    gif[0].save(prefix + '.gif', save_all=True, append_images=gif[1:], duration=120, loop=0)


main(sys.argv[1])
