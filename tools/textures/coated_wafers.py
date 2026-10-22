#!/usr/bin/env python3
"""Draws the coated wafers: each substrate's blank wafer with a film of the resist of its node over it.

    GT_SRC=/path/to/GregTech-Modern python3 tools/textures/coated_wafers.py     (from the repository root; needs Pillow)

The blanks of silicon, phosphorus, naquadah and neutronium are GT's textures (GT_SRC); the others are in this repository. The strange
matter wafer is animated (a strip of frames with an .mcmeta): every frame is coated, the .mcmeta is copied.
The resist tints: i-line amber (350 nm), KrF rose, ArF teal (100 and 80 nm), the immersion nodes' resist under its topcoat sky blue,
the 50 nm one lilac, the tin-oxo EUV resists silver-violet.
The film is drawn thick: faint spin rings, a dark bead on the rim, a wet streak of light, so a coated wafer is told from a blank one at a glance.
"""
import math
import os
import shutil
import sys

from PIL import Image

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), '../..')
WAFERS = os.path.join(ROOT, 'af9-core/src/main/resources/assets/af9/textures/item/wafers')
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
    """The substrate under a spun film: the film hides most of the wafer's grain, shows faint rings where the spin left it
    thicker or thinner, gathers into a darker, more saturated bead on the rim, and shines wet on the upper left."""
    out = frame.copy()
    w, h = frame.size
    solid = [(x, y) for y in range(h) for x in range(w) if frame.getpixel((x, y))[3]]
    cx = sum(x for x, _ in solid) / len(solid)
    cy = sum(y for _, y in solid) / len(solid)

    def edge(x, y):
        return any(not (0 <= x + dx < w and 0 <= y + dy < h) or frame.getpixel((x + dx, y + dy))[3] == 0
                   for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)))

    for (x, y) in solid:
        r, g, b, a = frame.getpixel((x, y))
        luma = (0.3 * r + 0.59 * g + 0.11 * b) / 255
        d = math.hypot(x - cx, y - cy)
        ring = 0.9 + 0.12 * math.sin(d * 2.1)                  # striations of the spin
        film = [c * (0.62 + 0.55 * luma) * ring for c in tint]
        k = 0.78                                               # how much of the film shows over the wafer
        px = [r * (1 - k) + film[0] * k, g * (1 - k) + film[1] * k, b * (1 - k) + film[2] * k]
        if edge(x, y):                                         # the edge bead
            px = [c * 0.62 for c in px]
            m = sum(px) / 3
            px = [m + (c - m) * 1.35 for c in px]
        # the wet shine: a diagonal streak on the upper left, a faint rim light on the lower right
        lit = 0
        if abs((x - cx) + (y - cy) + 3.2) < 1.1 and (x - cx) < 1 and (y - cy) < 1:
            lit = 78
        elif (x - cx) + (y - cy) > 6.5 and not edge(x, y):
            lit = 22
        out.putpixel((x, y), tuple(max(0, min(255, int(c + lit))) for c in px) + (a,))
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
