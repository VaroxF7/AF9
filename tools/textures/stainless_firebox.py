#!/usr/bin/env python3
"""The Stainless Steel Firebox Casing's side texture: GT's Titanium Firebox recoloured to the Clean Stainless Steel Casing.

Run from the repository root (needs Pillow):  python3 tools/textures/stainless_firebox.py

The titanium firebox's side (and its connected-texture sheet) is a ramp of purple greys round a black grate. Each shade
of the ramp takes the shade of the Clean Stainless Steel Casing's ramp at the same place (by lightness); the grate
stays black. The top and bottom are the Clean Stainless Steel Casing itself, the glowing layer is GT's own.
Originals: tools/textures/gt_src/ (GregTech CEu Modern, LGPL-3.0).
"""
import json
import os

from PIL import Image

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), '../..')
SRC = os.path.join(ROOT, 'tools/textures/gt_src')
OUT = os.path.join(ROOT, 'af9-core/src/main/resources/assets/af9/textures/block')


def lum(c):
    return 0.299 * c[0] + 0.587 * c[1] + 0.114 * c[2]


def colours(im):
    return {p[:3] for p in im.getdata() if p[3] and lum(p) > 40}


def ramp(im):
    return sorted(colours(im), key=lum)


def recolour(sprite, source_ramp, target_ramp):
    """Each shade takes the target shade at the same rank-fraction of its ramp (interpolated between two)."""
    mapping = {}
    for i, c in enumerate(source_ramp):
        t = i / (len(source_ramp) - 1) * (len(target_ramp) - 1)
        lo = int(t)
        hi = min(lo + 1, len(target_ramp) - 1)
        f = t - lo
        mapping[c] = tuple(round(target_ramp[lo][k] * (1 - f) + target_ramp[hi][k] * f) for k in range(3))
    out = sprite.copy()
    px = out.load()
    for y in range(out.height):
        for x in range(out.width):
            p = px[x, y]
            if p[:3] in mapping:
                px[x, y] = mapping[p[:3]] + (p[3],)
    return out


def main():
    side = Image.open(os.path.join(SRC, 'firebox_titanium.png')).convert('RGBA')
    ctm = Image.open(os.path.join(SRC, 'firebox_titanium_ctm.png')).convert('RGBA')
    stainless = Image.open(os.path.join(SRC, 'clean_stainless_steel.png')).convert('RGBA')
    src, dst = ramp(side), ramp(stainless)
    os.makedirs(OUT, exist_ok=True)
    recolour(side, src, dst).save(os.path.join(OUT, 'stainless_firebox_casing.png'))
    recolour(ctm, src, dst).save(os.path.join(OUT, 'stainless_firebox_casing_ctm.png'))
    with open(os.path.join(OUT, 'stainless_firebox_casing.png.mcmeta'), 'w', newline='\n') as f:
        json.dump({'ldlib': {'connection': 'af9:block/stainless_firebox_casing_ctm'}}, f, indent=4)
        f.write('\n')


if __name__ == '__main__':
    main()
