#!/usr/bin/env python3
"""Four restrained prism-glass looks for the Optically Reflective Wafer and Optical TPU / Memristor chips (preview).

    python3 tools/textures/optical_prism.py OUT.png
"""
import math
import sys
from PIL import Image
import optical_variants as ov

H = ov.hexc
GLASS = [H(h) for h in (0x04030a, 0x12101c, 0x1d1a2c, 0x2b2740, 0x3c3758, 0x5b5480)]


def mix(a, b, t):
    return tuple(int(a[i] + (b[i] - a[i]) * t) for i in range(3))


VARS = {
    'C1 Amber fringe': dict(glass=GLASS, fringe=[H(0x40d8ff), H(0xffb030)], glyph=H(0xff9a1c), glyph2=H(0xffe08a), link=H(0x8c3ee0),
                            ring=H(0xff9a1c), stripe=None),
    'C2 Violet fibre': dict(glass=[mix(g, H(0x2a1060), 0.25) for g in GLASS], fringe=[H(0xa070ff), H(0x60e0ff)], glyph=H(0xc8a8ff),
                            glyph2=H(0xffffff), link=H(0x7a4ae0), ring=H(0x9a6aff), stripe=H(0x7a4ae0)),
    'C3 Edge refraction': dict(glass=GLASS, fringe=[H(0xff4060), H(0x40a0ff)], glyph=H(0xe8f6ff), glyph2=H(0xffffff), link=H(0x60b0ff),
                               ring=None, stripe=None, edge=True),
    'C4 Gold lens': dict(glass=[mix(g, H(0x2a1a04), 0.3) for g in GLASS], fringe=[H(0x40d0ff), H(0xffd060)], glyph=H(0xffc030),
                         glyph2=H(0xfff0b0), link=H(0xff7a1c), ring=H(0xe0a020), stripe=None),
}


def draw(v, shape, kind=None):
    mask = ov.disc_mask() if shape == 'wafer' else ov.chip_mask()
    cx, cy = (7.5, 7.5) if shape == 'wafer' else (7.5, 8)
    g = v['glass']
    img = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
    for (x, y) in mask:
        d = ov.edge_depth(mask, x, y)
        s = (x - cx) + (y - cy)
        t = 4 if s < -6 else 5 if s < -3 else 4 if s < 0 else 3 if s < 3 else 2 if s < 6 else 1
        col = g[t]
        if (x + y) % 7 == 0 and d >= 2:                       # faint glass striations
            col = g[min(5, t + 1)]
        if d == 1:
            col = g[0]
        elif d == 2 and s < -2:
            col = tuple(min(255, int(c * 1.3 + 10)) for c in col)
        elif d == 2 and s > 3:
            col = g[2]
        if v.get('edge') and d == 2 and s > 1:                # chromatic fringe on the lower-right rim only
            col = v['fringe'][0] if (x + y) % 2 else v['fringe'][1]
        img.putpixel((x, y), col + (255,))
    for (x, y) in mask:                                       # the glint: white core, one-pixel two-tone fringe
        s = (x - cx) + (y - cy)
        if ov.edge_depth(mask, x, y) >= 2 and abs((x - cx) - (y - cy)) < 5:
            if abs(s + 3.5) < 0.6:
                img.putpixel((x, y), (240, 248, 255, 255))
            elif abs(s + 5.0) < 0.6 and not v.get('edge'):
                img.putpixel((x, y), v['fringe'][0] + (255,))
            elif abs(s + 2.0) < 0.6 and not v.get('edge'):
                img.putpixel((x, y), v['fringe'][1] + (255,))
    if v.get('stripe') and shape == 'wafer':
        for (x, y) in mask:
            if ov.edge_depth(mask, x, y) >= 2 and abs((x - cx) + (y - cy) - 4) < 0.6 and abs((x - cx) - (y - cy)) < 5:
                img.putpixel((x, y), v['stripe'] + (255,))
    if kind:
        pts, link = ov.glyph(kind) if shape == 'chip' else ov.glyph(kind, 0, -1)
        for (x, y) in pts:
            if (x, y) in mask and ov.edge_depth(mask, x, y) >= 2:
                img.putpixel((x, y), (v['glyph2'] if (x + y) % 3 == 0 else v['glyph']) + (255,))
        for (x, y) in link:
            if (x, y) in mask:
                img.putpixel((x, y), v['link'] + (255,))
    elif v.get('ring'):
        for (x, y) in mask:
            r = math.hypot(x - cx, y - cy)
            if ov.edge_depth(mask, x, y) < 2:
                continue
            if r < 1.2:
                img.putpixel((x, y), v['glyph2'] + (255,))
            elif 3.0 <= r < 3.8 and (x - cx) + (y - cy) > -3:
                img.putpixel((x, y), v['ring'] + (255,))
    return img


def main(out):
    S = 9
    cw = 16 * S + 8
    sheet = Image.new('RGBA', (3 * cw, len(VARS) * cw), (34, 38, 46, 255))
    for r, v in enumerate(VARS.values()):
        for c, (shape, kind) in enumerate((('wafer', None), ('chip', 'tpu'), ('chip', 'memristor'))):
            sheet.alpha_composite(draw(v, shape, kind).resize((16 * S, 16 * S), Image.NEAREST), (c * cw, r * cw))
    sheet.save(out)


if __name__ == '__main__':
    main(sys.argv[1])
