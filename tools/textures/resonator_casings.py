#!/usr/bin/env python3
"""The casings of the Certus Resonating Manufacturer, made from GTCEu's Stable Titanium Machine Casing (LGPL-3.0,
tools/textures/gt_src/) recoloured: the AE2 casing in cold blue steel whose seams carry a certus light while the
machine works, the black casing in graphite. Same plates and bevels as every other GT casing, joined by CTM.

Run from the repository root (needs Pillow):  python3 tools/textures/resonator_casings.py [--preview preview.png]

Writes into af9-core/.../assets/af9/textures/block:
  ae2_machine_casing(.png/_ctm.png): static, plain
  ae2_machine_casing_active(_ctm), ae2_machine_casing_active_emissive(_ctm): animated strips (16 frames) where the
      seams pulse in certus blue, a wave running over the block
  ptfe_black_casing(.png/_ctm.png): static graphite
"""
import json
import math
import os
import sys

from PIL import Image

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), '../..')
SRC = os.path.join(ROOT, 'tools/textures/gt_src')
OUT = os.path.join(ROOT, 'af9-core/src/main/resources/assets/af9/textures/block')
FRAMES = 16
FRAMETIME = 3
TAU = 2 * math.pi

# luminance (of the source, 82..230) -> colour
STEEL = [(0.0, (14, 20, 30)), (0.45, (40, 52, 68)), (0.75, (82, 100, 122)), (1.0, (150, 172, 194))]
GRAPHITE = [(0.0, (10, 11, 13)), (0.45, (28, 30, 34)), (0.75, (52, 55, 62)), (1.0, (104, 110, 122))]
ALU_TQ = [(0.0, (12, 26, 28)), (0.45, (38, 74, 76)), (0.75, (88, 142, 138)), (1.0, (170, 214, 206))]  # silver-teal
CERTUS = (96, 200, 255)
SEAM_LUM = 0.26     # the grooves: the darkest source pixels carry the light


def clamp(v):
    return max(0, min(255, round(v)))


def ramp(stops, t):
    t = max(0.0, min(1.0, t))
    for (t0, c0), (t1, c1) in zip(stops, stops[1:]):
        if t <= t1:
            f = (t - t0) / (t1 - t0)
            return tuple(clamp(a + (b - a) * f) for a, b in zip(c0, c1))
    return stops[-1][1]


def lum(px):
    return max(0.0, min(1.0, ((px[0] * 0.3 + px[1] * 0.59 + px[2] * 0.11) - 80) / 150.0))


def recolour(tile, stops):
    out = tile.copy()
    for y in range(tile.height):
        for x in range(tile.width):
            p = tile.getpixel((x, y))
            out.putpixel((x, y), ramp(stops, lum(p)) + (p[3],))
    return out


def seam_mask(tile):
    return [[tile.getpixel((x, y))[3] > 0 and lum(tile.getpixel((x, y))) <= SEAM_LUM
             for x in range(tile.width)] for y in range(tile.height)]


def glow(tile, frame):
    """(lit casing, emissive layer) for one frame: the seams breathe, a wave runs diagonally over the 16 px tile."""
    base = recolour(tile, STEEL)
    emi = Image.new('RGBA', tile.size, (0, 0, 0, 0))
    mask = seam_mask(tile)
    for y in range(tile.height):
        for x in range(tile.width):
            if not mask[y][x]:
                continue
            wave = 0.5 + 0.5 * math.sin(TAU * (frame / FRAMES - ((x % 16 + y % 16) % 16) / 16.0))
            k = 0.45 + 0.55 * wave
            c = tuple(clamp(v * k) for v in CERTUS)
            base.putpixel((x, y), c + (255,))
            emi.putpixel((x, y), c + (255,))
    return base, emi


def tiles(sheet):
    """A sheet's 16 x 16 tiles with their position."""
    return [((tx * 16, ty * 16), sheet.crop((tx * 16, ty * 16, tx * 16 + 16, ty * 16 + 16)))
            for ty in range(sheet.height // 16) for tx in range(sheet.width // 16)]


def apply(sheet, fn):
    out = Image.new('RGBA', sheet.size, (0, 0, 0, 0))
    for pos, tile in tiles(sheet):
        out.paste(fn(tile), pos)
    return out


def strip(frames):
    w, h = frames[0].size
    im = Image.new('RGBA', (w, h * len(frames)))
    for i, fr in enumerate(frames):
        im.paste(fr, (0, i * h))
    return im


def dump(path, data):
    with open(path, 'w', newline='\n') as f:
        json.dump(data, f, indent=2)
        f.write('\n')


def main():
    sprite = Image.open(os.path.join(SRC, 'machine_casing_stable_titanium.png')).convert('RGBA')
    sheet = Image.open(os.path.join(SRC, 'machine_casing_stable_titanium_ctm.png')).convert('RGBA')

    for name, stops in (('ae2_machine_casing', STEEL), ('ptfe_black_casing', GRAPHITE),
                        ('aluminium_tranquillite_casing', ALU_TQ)):
        recolour(sprite, stops).save(os.path.join(OUT, name + '.png'))
        apply(sheet, lambda t, s=stops: recolour(t, s)).save(os.path.join(OUT, name + '_ctm.png'))
        for suffix in ('', '_ctm'):
            path = os.path.join(OUT, name + suffix + '.png.mcmeta')
            if os.path.exists(path):
                os.remove(path)
        dump(os.path.join(OUT, name + '.png.mcmeta'), {'ldlib': {'connection': 'af9:block/%s_ctm' % name}})

    for suffix, src in (('', sprite), ('_ctm', sheet)):
        lit, emi = [], []
        for f in range(FRAMES):
            pair = [glow(t, f) for _, t in tiles(src)]
            a = Image.new('RGBA', src.size, (0, 0, 0, 0))
            b = Image.new('RGBA', src.size, (0, 0, 0, 0))
            for (pos, _), (base, e) in zip(tiles(src), pair):
                a.paste(base, pos)
                b.paste(e, pos)
            lit.append(a)
            emi.append(b)
        strip(lit).save(os.path.join(OUT, 'ae2_machine_casing_active%s.png' % suffix))
        strip(emi).save(os.path.join(OUT, 'ae2_machine_casing_active_emissive%s.png' % suffix))

    anim = {'animation': {'frametime': FRAMETIME, 'interpolate': True}}
    dump(os.path.join(OUT, 'ae2_machine_casing_active.png.mcmeta'),
         dict(anim, ldlib={'connection': 'af9:block/ae2_machine_casing_active_ctm'}))
    dump(os.path.join(OUT, 'ae2_machine_casing_active_ctm.png.mcmeta'), anim)
    dump(os.path.join(OUT, 'ae2_machine_casing_active_emissive.png.mcmeta'),
         dict(anim, ldlib={'connection': 'af9:block/ae2_machine_casing_active_emissive_ctm'}))
    dump(os.path.join(OUT, 'ae2_machine_casing_active_emissive_ctm.png.mcmeta'), anim)

    if '--preview' in sys.argv:
        preview(sys.argv[sys.argv.index('--preview') + 1])


def preview(dest):
    """Both casings as lone blocks and as a 5 x 3 wall (idle and lit), beside the sheet."""
    z = 8
    img = Image.new('RGBA', (16 * z * 14, 16 * z * 4 + 20), (30, 32, 38, 255))
    names = ['ae2_machine_casing', 'ae2_machine_casing_active', 'ptfe_black_casing']
    for row, name in enumerate(names):
        s = Image.open(os.path.join(OUT, name + '.png')).convert('RGBA')
        for col, f in enumerate((0, 4, 8, 12) if 'active' in name else (0,)):
            tile = s.crop((0, f * 16, 16, f * 16 + 16)).resize((16 * z, 16 * z), Image.NEAREST)
            img.alpha_composite(tile, (10 + col * (16 * z + 6), 10 + row * (16 * z + 4)))
    img.save(dest)


if __name__ == '__main__':
    main()
