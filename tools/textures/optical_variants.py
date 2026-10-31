#!/usr/bin/env python3
"""Four hand-built looks for the Optically Reflective Wafer and the Optical TPU / Memristor chips (preview sheet).

    python3 tools/textures/optical_variants.py OUT.png
"""
import math
import sys
from PIL import Image

SZ = 16


def disc_mask():
    rows = ['................', '................', '.....######.....', '...##########...', '..############..', '.##############.',
            '################', '################', '################', '################', '################',
            '.##############.', '..############..', '...##########...', '.....######.....', '................']
    return {(x, y) for y in range(16) for x in range(16) if rows[y][x] == '#'}


def chip_mask():
    return {(x, y) for y in range(3, 14) for x in range(1, 15)}


def edge_depth(mask, x, y):
    for d in range(1, 4):
        for dx in range(-d, d + 1):
            for dy in range(-d, d + 1):
                if (x + dx, y + dy) not in mask:
                    return d
    return 4


def line(p, q):
    (x0, y0), (x1, y1) = p, q
    pts = []
    n = max(abs(x1 - x0), abs(y1 - y0), 1)
    for i in range(n + 1):
        pts.append((round(x0 + (x1 - x0) * i / n), round(y0 + (y1 - y0) * i / n)))
    return pts


def hsv(h, s, v):
    import colorsys
    r, g, b = colorsys.hsv_to_rgb(h % 1.0, s, v)
    return (int(r * 255), int(g * 255), int(b * 255))


def hexc(h):
    return ((h >> 16) & 255, (h >> 8) & 255, h & 255)


def glyph(kind, x0=0, y0=0, small=False):
    """pixel sets of a glyph, centred on a chip (7.5, 8) or wafer (7.5, 7.5)"""
    if kind == 'tpu':
        cells = [(5, 5), (9, 5), (5, 9), (9, 9)] if not small else [(5, 5), (9, 5), (5, 9), (9, 9)]
        pts = set()
        for cx, cy in cells:
            for a in range(2):
                for b in range(2):
                    pts.add((cx + a + x0, cy + b + y0))
        return pts, {(7 + x0, 6 + y0), (7 + x0, 10 + y0), (6 + x0, 8 + y0), (10 + x0, 8 + y0)}
    path = [(3, 8), (5, 8), (6, 5), (9, 11), (10, 8), (12, 8)]
    pts = set()
    for a, b in zip(path, path[1:]):
        for p in line(a, b):
            pts.add((p[0] + x0, p[1] + y0))
    return pts, set()


# variant: tones (outline, 1..5 dark to light), accent colours
V = {
    'A Chrome mirror': dict(tones=[hexc(h) for h in (0x0c1220, 0x2a3446, 0x56657d, 0x8696ae, 0xc2cfe0, 0xf4f9ff)],
                            glow=hexc(0x40e8ff), glow2=hexc(0xd8fbff), rim=hexc(0x2ac8e8)),
    'B GTNH amber': dict(tones=[hexc(h) for h in (0x0a0810, 0x26232f, 0x403d50, 0x5e5a74, 0x827e9c, 0xb0abc8)],
                         glow=hexc(0xff9a1c), glow2=hexc(0xffe07a), rim=hexc(0x8c3ee0)),
    'C Prism glass': dict(tones=[hexc(h) for h in (0x05040a, 0x14121e, 0x211e30, 0x322e48, 0x4a4466, 0x7c74a0)],
                          glow=hexc(0xffffff), glow2=hexc(0xffffff), rim=hexc(0xffffff)),
    'D Fibre glass': dict(tones=[hexc(h) for h in (0x041512, 0x0c3a34, 0x14584e, 0x1f8070, 0x38b49c, 0xa8f0dc)],
                          glow=hexc(0xc4ff3a), glow2=hexc(0xffffe0), rim=hexc(0x6cff9a)),
}


def band_tone(x, y, mask, cx, cy):
    s = (x - cx) + (y - cy)
    if s < -6:
        return 4
    if s < -3:
        return 5
    if s < 0:
        return 4
    if s < 3:
        return 3
    if s < 6:
        return 2
    return 1


def draw(name, v, shape, kind=None):
    mask = disc_mask() if shape == 'wafer' else chip_mask()
    cx, cy = (7.5, 7.5) if shape == 'wafer' else (7.5, 8)
    img = Image.new('RGBA', (SZ, SZ), (0, 0, 0, 0))
    T = v['tones']
    for (x, y) in mask:
        d = edge_depth(mask, x, y)
        t = band_tone(x, y, mask, cx, cy)
        s = (x - cx) + (y - cy)
        col = T[t]
        if name[0] == 'C':
            h = ((x + y) / 18.0) % 1.0
            rain = hsv(h, 0.75, 0.55 + 0.25 * (t / 5))
            col = tuple(int(T[t][i] * 0.45 + rain[i] * 0.55) for i in range(3)) if (x + y) % 6 < 4 else T[max(1, t - 1)]
        if d == 1:
            col = T[0]
        elif d == 2 and s < -2:
            col = tuple(min(255, int(c * 1.25 + 14)) for c in col)
        elif d == 2 and s > 3:
            col = v['rim'] if name[0] in 'AB' else tuple(int(c * 0.7) for c in col)
        img.putpixel((x, y), col + (255,))
    # glints (the GT-style diagonal streak)
    for (x, y) in mask:
        s = (x - cx) + (y - cy)
        if edge_depth(mask, x, y) >= 2 and abs(s + 3.5) < 0.6 and abs((x - cx) - (y - cy)) < 5:
            img.putpixel((x, y), v['glow2'] + (255,) if name[0] != 'D' else T[5] + (255,))
    # motif
    if kind:
        pts, link = glyph(kind) if shape == 'chip' else glyph(kind, 0, -1)
        for (x, y) in pts:
            if (x, y) in mask and edge_depth(mask, x, y) >= 2:
                lit = (x + y) % 2 == 0
                c = v['glow'] if name[0] != 'C' else hsv((x + y) / 12.0, 0.9, 1.0)
                img.putpixel((x, y), c + (255,))
        for (x, y) in link:
            if (x, y) in mask:
                img.putpixel((x, y), (v['rim'] if name[0] != 'C' else (255, 255, 255)) + (255,))
        # a dark pocket behind the glyph so it reads
    else:
        # blank wafer motif
        for (x, y) in mask:
            r = math.hypot(x - cx, y - cy)
            if edge_depth(mask, x, y) < 2:
                continue
            if name[0] == 'A' and 2.6 <= r < 3.6 and (x - cx) + (y - cy) > -2:
                img.putpixel((x, y), v['rim'] + (255,))
            if name[0] == 'B':
                if r < 1.3:
                    img.putpixel((x, y), v['glow'] + (255,))
                elif 2.4 <= r < 3.2:
                    img.putpixel((x, y), T[1] + (255,))
                elif (abs(x - cx) < 0.6 or abs(y - cy) < 0.6) and 3.2 <= r < 5.4:
                    img.putpixel((x, y), v['rim'] + (255,))
            if name[0] == 'D':
                cheb = max(abs(x - cx), abs(y - cy))
                if int(cheb + 0.5) in (2, 4) and (x * 7 + y * 3) % 5 != 0:
                    img.putpixel((x, y), v['glow'] + (255,))
    return img


def main(out):
    S = 9
    cw = 16 * S + 8
    sheet = Image.new('RGBA', (3 * cw, len(V) * cw), (34, 38, 46, 255))
    for r, (name, v) in enumerate(V.items()):
        for c, (shape, kind) in enumerate((('wafer', None), ('chip', 'tpu'), ('chip', 'memristor'))):
            sheet.alpha_composite(draw(name, v, shape, kind).resize((16 * S, 16 * S), Image.NEAREST), (c * cw, r * cw))
    sheet.save(out)


if __name__ == '__main__':
    main(sys.argv[1])
