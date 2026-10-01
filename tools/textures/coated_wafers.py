#!/usr/bin/env python3
"""Draws the coated wafers: each substrate's blank wafer with a film of the resist of its node over it.

    GT_SRC=/path/to/GregTech-Modern python3 tools/textures/coated_wafers.py     (from the repository root; needs Pillow)

The blanks of silicon, phosphorus, naquadah and neutronium are GT's textures (GT_SRC); the others are in this repository. The strange
matter wafer is animated (a strip of frames with an .mcmeta): every frame is coated, the .mcmeta is copied.
The resist tints: i-line amber (350 nm), KrF rose, ArF teal (100 and 80 nm), the immersion nodes' resist under its topcoat sky blue,
the 50 nm one lilac, the tin-oxo EUV resists silver-violet.
"""
import os
import shutil
import sys

from PIL import Image

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), '../..')
WAFERS = os.path.join(ROOT, 'kubejs/assets/kubejs/textures/item/wafers')
GT_SRC = os.environ.get('GT_SRC')

# substrate, where its blank texture is, the resist tint
SUBSTRATES = [
    ('silicon', 'gt', (222, 150, 48)),
    ('phosphorus', 'gt', (214, 98, 112)),
    ('naquadah', 'gt', (56, 186, 170)),
    ('trinium', 'repo', (70, 166, 204)),
    ('naquadria', 'repo', (104, 164, 236)),
    ('neutronium', 'gt', (164, 126, 224)),
    ('transmuted_neutronium', 'repo', (176, 158, 204)),
    ('strange_matter', 'repo', (206, 184, 236)),
]


def coat(frame, tint):
    out = frame.copy()
    for y in range(frame.height):
        for x in range(frame.width):
            r, g, b, a = frame.getpixel((x, y))
            if a == 0:
                continue
            luma = (0.3 * r + 0.59 * g + 0.11 * b) / 255
            k = 0.62                                   # how much of the film shows over the wafer
            film = tuple(min(255, int(c * (0.55 + 0.7 * luma))) for c in tint)
            out.putpixel((x, y), (int(r * (1 - k) + film[0] * k), int(g * (1 - k) + film[1] * k),
                                  int(b * (1 - k) + film[2] * k), a))
    # the wet shine of a spun film: a bright speck on the upper left
    for (x, y) in ((5, 4), (6, 4), (5, 5)):
        if frame.getpixel((x, y))[3]:
            r, g, b, a = out.getpixel((x, y))
            out.putpixel((x, y), (min(255, r + 70), min(255, g + 70), min(255, b + 70), a))
    return out


def main():
    for sid, where, tint in SUBSTRATES:
        if where == 'gt':
            if not GT_SRC:
                sys.exit('GT_SRC is needed for the blanks of GT (silicon, phosphorus, naquadah, neutronium)')
            src = os.path.join(GT_SRC, 'src/main/resources/assets/gtceu/textures/item', f'{sid}_wafer.png')
        else:
            src = os.path.join(WAFERS, f'{sid}_wafer.png')
        im = Image.open(src).convert('RGBA')
        w = im.width
        frames = im.height // w
        out = Image.new('RGBA', im.size)
        for f in range(frames):
            out.paste(coat(im.crop((0, f * w, w, (f + 1) * w)), tint), (0, f * w))
        dst = os.path.join(WAFERS, f'coated_{sid}_wafer.png')
        out.save(dst)
        if os.path.exists(src + '.mcmeta'):
            shutil.copy(src + '.mcmeta', dst + '.mcmeta')
    print(f'coated wafers: {len(SUBSTRATES)} textures', file=sys.stderr)


main()
