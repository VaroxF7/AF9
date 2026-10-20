#!/usr/bin/env python3
"""The casings of the Certus Resonating Manufacturer: the black PTFE casing and the AE2 casing, lightened and
given a slow shimmer, the AE2 casing with a faint certus-blue tint in it.

Run from the repository root (needs Pillow):  python3 tools/textures/resonator_casings.py [--preview preview.png]

The plain art is in tools/textures/resonator_src/ (the ported TFG textures); the script writes the animated strips
(sprite: 16 x 16n, sheet: 32 x 32n, n frames each, both animated with the same frame time so the sheet follows
the sprite) and the .mcmeta files. The connected textures (LDLib's .mcmeta + `_ctm` sheet) stay as they are.
"""
import json
import math
import os
import sys

from PIL import Image

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), '../..')
SRC = os.path.join(ROOT, 'tools/textures/resonator_src')
OUT = os.path.join(ROOT, 'af9-core/src/main/resources/assets/af9/textures/block')
FRAMES = 16
FRAMETIME = 4
TAU = 2 * math.pi

# name: (gain, lift, shimmer, tint) -- gain and lift lighten the greys, shimmer runs over the bright pixels,
# tint is the certus blue that the AE2 casing gets in its shimmer
CASINGS = {
    'ptfe_black_casing': (0.72, 44.0, 0.05, None),
    'ae2_machine_casing': (0.85, 56.0, 0.05, (110, 190, 235)),
}


def clamp(v):
    return max(0, min(255, round(v)))


def lighten(sprite, gain, lift, shimmer, tint, frame, phase_of):
    """One frame of the lightened casing: the greys are lifted, the bright pixels (the bevels) breathe with a
    wave that runs across the block, and the AE2 casing gets a little certus blue in it."""
    out = sprite.copy()
    px = out.load()
    values = [px[x, y][0] for y in range(sprite.height) for x in range(sprite.width)]
    lo, hi = min(values), max(values)
    threshold = lo + 0.55 * (hi - lo)
    t = frame / FRAMES
    for y in range(sprite.height):
        for x in range(sprite.width):
            r, g, b, a = px[x, y]
            v = (r + g + b) / 3
            if v <= 0:
                continue
            lifted = v * gain + lift
            if v >= threshold:
                wave = 1.0 + shimmer * math.sin(TAU * (t - phase_of(x, y)))
                lifted *= wave
                if tint is not None:
                    r = r * wave + (tint[0] - r) * 0.08
                    g = g * wave + (tint[1] - g) * 0.08
                    b = b * wave + (tint[2] - b) * 0.08
                    lifted = (r + g + b) / 3
            k = lifted / v
            px[x, y] = (clamp(r * k), clamp(g * k), clamp(b * k), a)
    return out


def sprite_phase(x, y):
    """The wave runs diagonally over the block, so it carries on from one block to the next."""
    return ((x + y) % 16) / 16.0


def sheet_phase(x, y):
    """The sheet holds four corner tiles: the wave uses the position inside the tile."""
    return sprite_phase(x % 16, y % 16)


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
    for name, (gain, lift, shimmer, tint) in CASINGS.items():
        sprite = Image.open(os.path.join(SRC, name + '.png')).convert('RGBA')
        sheet = Image.open(os.path.join(SRC, name + '_ctm.png')).convert('RGBA')

        frames = [lighten(sprite, gain, lift, shimmer, tint, f, sprite_phase) for f in range(FRAMES)]
        strip(frames).save(os.path.join(OUT, name + '.png'))

        sheets = []
        for f in range(FRAMES):
            sheet_frames = []
            for ty in range(sheet.height // 16):
                for tx in range(sheet.width // 16):
                    tile = sheet.crop((tx * 16, ty * 16, tx * 16 + 16, ty * 16 + 16))
                    sheet_frames.append(lighten(tile, gain, lift, shimmer, tint, f, sheet_phase))
            full = Image.new('RGBA', sheet.size)
            for i, tile in enumerate(sheet_frames):
                full.paste(tile, ((i % 2) * 16, (i // 2) * 16))
            sheets.append(full)
        strip(sheets).save(os.path.join(OUT, name + '_ctm.png'))

        animation = {'animation': {'frametime': FRAMETIME, 'interpolate': True}}
        dump(os.path.join(OUT, name + '.png.mcmeta'),
             dict(animation, ldlib={'connection': 'af9:block/%s_ctm' % name}))
        dump(os.path.join(OUT, name + '_ctm.png.mcmeta'), animation)

    # the active AE2 casing (the glowing state) keeps its own animation: only the light of the greys changes
    for name in ('ae2_machine_casing_active', 'ae2_machine_casing_active_emissive'):
        gain, lift, shimmer, tint = CASINGS['ae2_machine_casing']
        for suffix, phase, tile in (('', sprite_phase, 16), ('_ctm', sheet_phase, 32)):
            im = Image.open(os.path.join(SRC, name + suffix + '.png')).convert('RGBA')
            frames = []
            for f in range(im.height // tile):
                piece = im.crop((0, f * tile, im.width, f * tile + tile))
                frames.append(lighten(piece, gain, lift, shimmer, tint, f, phase))
            strip(frames).save(os.path.join(OUT, name + suffix + '.png'))

    if '--preview' in sys.argv:
        preview(sys.argv[sys.argv.index('--preview') + 1])


def preview(dest):
    """A contact sheet: both casings, the lone block and the joined wall, with the controller face."""
    from PIL import ImageDraw
    z = 6
    img = Image.new('RGBA', (16 * z * 12, 16 * z * 6 + 30), (36, 38, 44, 255))
    d = ImageDraw.Draw(img)
    x = 4
    for name in CASINGS:
        sprite = Image.open(os.path.join(OUT, name + '.png')).convert('RGBA')
        sheet = Image.open(os.path.join(OUT, name + '_ctm.png')).convert('RGBA')
        d.text((x, 4), name, fill=(230, 232, 238))
        for f in (0, 4, 8):
            lone = sprite.crop((0, 16 * f, 16, 16 * f + 16)).resize((16 * z, 16 * z), Image.NEAREST)
            img.alpha_composite(lone, (x + f * (16 * z + 4), 16))
        img.alpha_composite(sheet.crop((0, 0, 32, 32)).resize((16 * z, 16 * z), Image.NEAREST), (x, 16 + 16 * z + 4))
        x += 16 * z * 3 + 40
    img.save(dest)


if __name__ == '__main__':
    main()
