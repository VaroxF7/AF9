#!/usr/bin/env python3
"""The litho light-source blocks (KrF Excimer Laser, ArF Excimer Laser, EUV Light Source): GregTech's own Blacklight sprite
(tools/textures/gt_src/blacklight.png, GregTech CEu Modern, LGPL-3.0, see CREDITS-textures.md) with its violet hue-shifted per
tier (KrF blue, ArF magenta, EUV amber), the diagonal tube unchanged, on a dark bevelled housing with a soft glow behind it.
The grey end caps keep their colour; only the saturated tube pixels are recoloured.

Run from the repository root (needs Pillow):  python3 tools/textures/blacklight_sources.py
"""
import colorsys
import os

from PIL import Image

HERE = os.path.dirname(os.path.abspath(__file__))
OUT = os.path.join(HERE, '../../af9-core/src/main/resources/assets/af9/textures/block')
SRC = Image.open(os.path.join(HERE, 'gt_src/blacklight.png')).convert('RGBA')

HOUSING = [(83, 76, 92), (58, 52, 66), (42, 36, 49), (30, 25, 35), (19, 15, 22)]
TIERS = (('krf_excimer_laser', 0.60), ('arf_excimer_laser', 0.88), ('euv_light_source', 0.10))   # hue of the tube


def recolour(px, hue):
    r, g, b, a = px
    h, s, v = colorsys.rgb_to_hsv(r / 255, g / 255, b / 255)
    if a == 0 or s < 0.35:      # transparent or grey (end caps): untouched
        return px
    r, g, b = colorsys.hsv_to_rgb(hue, s, v)
    return (round(r * 255), round(g * 255), round(b * 255), a)


def housing(glow):
    im = Image.new('RGBA', (16, 16), HOUSING[3] + (255,))
    px = im.load()
    for i in range(16):         # bevelled frame: light top/left, dark bottom/right
        px[i, 0] = px[0, i] = HOUSING[0]
        px[i, 1] = px[1, i] = HOUSING[1]
        px[i, 15] = px[15, i] = HOUSING[4]
        px[i, 14] = px[14, i] = HOUSING[3]
    for x, y in ((2, 2), (13, 2), (2, 13), (13, 13)):   # rivets
        px[x, y] = HOUSING[0]
    for y in range(2, 14):      # glow: a soft diagonal band behind the tube
        for x in range(2, 14):
            d = abs((x + y) - 15)
            if d <= 3:
                k = (4 - d) / 4 * 0.45
                px[x, y] = tuple(round(HOUSING[3][c] * (1 - k) + glow[c] * k) for c in range(3)) + (255,)
    return im


for name, hue in TIERS:
    tube = SRC.copy()
    tube.putdata([recolour(p, hue) for p in SRC.getdata()])
    r, g, b = colorsys.hsv_to_rgb(hue, 0.9, 0.55)
    base = housing((round(r * 255), round(g * 255), round(b * 255)))
    base.alpha_composite(tube)
    base.save(os.path.join(OUT, name + '.png'))
    print('wrote', name)
