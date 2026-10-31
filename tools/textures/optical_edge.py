#!/usr/bin/env python3
"""C3 (dark prism glass) with four edge-reflection treatments (preview).   python3 tools/textures/optical_edge.py OUT.png"""
import math
import sys
from PIL import Image
import optical_variants as ov
import optical_prism as op

H = ov.hexc
G = op.GLASS


def lerp(a, b, t):
    return tuple(int(a[i] + (b[i] - a[i]) * max(0.0, min(1.0, t))) for i in range(3))


def ang(x, y, cx, cy):
    return math.degrees(math.atan2(y - cy, x - cx))   # 0 = right, 90 = down


def edge_color(mode, x, y, cx, cy, d, base):
    a = ang(x, y, cx, cy)
    s = (x - cx) + (y - cy)
    if mode == 'E1 Soft arc':                   # a smooth blue->violet->magenta arc on the lower right, fading out at its ends
        if d <= 2 and 0 < a < 100:
            k = 1 - abs(a - 50) / 55
            c = lerp(H(0x3a8cff), H(0xd04cb0), (a - 10) / 80)
            return lerp(base, c, 0.35 + 0.65 * k) if d == 2 else lerp(base, c, 0.6 * k)
    if mode == 'E2 Cold rim light':             # thin cyan-white rim light on the upper left, deep blue on the lower right
        if d == 2 and s < -2:
            return lerp(base, H(0xbfe8ff), 0.75 if s < -5 else 0.5)
        if d == 2 and s > 3:
            return lerp(base, H(0x2a58d0), 0.6)
    if mode == 'E3 Double bounce':              # reflection on both sides: warm upper left, cool lower right, inner second line
        if d == 2 and s < -2:
            return lerp(base, H(0xffd89a), 0.6)
        if d == 3 and s < -3:
            return lerp(base, H(0xffd89a), 0.18)
        if d == 2 and s > 3:
            return lerp(base, H(0x5ab4ff), 0.65)
        if d == 3 and s > 4:
            return lerp(base, H(0x5ab4ff), 0.2)
    if mode == 'E4 Bevel sheen':                # a bright bevel on the top and left edges, dark bevel on bottom and right, cyan kick at the corner
        if d == 2 and (y < cy - 1 or x < cx - 2) and s < 1:
            return lerp(base, H(0xe8f4ff), 0.7)
        if d == 2 and s > 2:
            return lerp(base, G[0], 0.7)
        if d == 2 and 1 < a < 70 and s > 5:
            return lerp(base, H(0x30d0ff), 0.8)
    return None


def draw(mode, shape, kind=None):
    mask = ov.disc_mask() if shape == 'wafer' else ov.chip_mask()
    cx, cy = (7.5, 7.5) if shape == 'wafer' else (7.5, 8)
    img = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
    for (x, y) in mask:
        d = ov.edge_depth(mask, x, y)
        s = (x - cx) + (y - cy)
        t = 4 if s < -6 else 5 if s < -3 else 4 if s < 0 else 3 if s < 3 else 2 if s < 6 else 1
        col = G[t]
        if (x + y) % 7 == 0 and d >= 2:
            col = G[min(5, t + 1)]
        if d == 1:
            col = G[0]
        else:
            e = edge_color(mode, x, y, cx, cy, d, col)
            if e:
                col = e
        img.putpixel((x, y), col + (255,))
    for (x, y) in mask:
        s = (x - cx) + (y - cy)
        if ov.edge_depth(mask, x, y) >= 3 and abs((x - cx) - (y - cy)) < 5 and abs(s + 3.5) < 0.6:
            img.putpixel((x, y), (240, 248, 255, 255))
    if kind:
        pts, link = ov.glyph(kind) if shape == 'chip' else ov.glyph(kind, 0, -1)
        for (x, y) in pts:
            if (x, y) in mask and ov.edge_depth(mask, x, y) >= 3:
                img.putpixel((x, y), (H(0xffffff) if (x + y) % 3 == 0 else H(0xdcefff)) + (255,))
        for (x, y) in link:
            if (x, y) in mask:
                img.putpixel((x, y), H(0x60b0ff) + (255,))
    return img


MODES = ['E1 Soft arc', 'E2 Cold rim light', 'E3 Double bounce', 'E4 Bevel sheen']


def main(out):
    S = 9
    cw = 16 * S + 8
    sheet = Image.new('RGBA', (3 * cw, len(MODES) * cw), (34, 38, 46, 255))
    for r, m in enumerate(MODES):
        for c, (shape, kind) in enumerate((('wafer', None), ('chip', 'tpu'), ('chip', 'memristor'))):
            sheet.alpha_composite(draw(m, shape, kind).resize((16 * S, 16 * S), Image.NEAREST), (c * cw, r * cw))
    sheet.save(out)


if __name__ == '__main__':
    main(sys.argv[1])
