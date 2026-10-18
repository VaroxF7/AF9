#!/usr/bin/env python3
"""The Orbital Light Casing and Orbital Magnet Casing: connected textures (LDLib's .mcmeta + `_ctm` sheet, the format
tools/textures/space_elevator.py describes) and a slight animation (16 frames).

Run from the repository root (needs Pillow):  python3 tools/textures/orbital_casings.py
The plain sprites are in tools/textures/orbital_src/; the script writes the animated strips (sprite: 16 x 16n, sheet:
32 x 32n, n frames each, both animated with the same frame time so the sheet follows the sprite) and the .mcmeta files.

  light   a ring of casings reads as one panel of lit windows (the border stays on the outside, the window repeats inside);
          the orange window breathes, its four tones a little out of step
  magnet  the casing's bright pole pieces shimmer: a slow pulse travels along them (every 8 px, so it runs on across blocks)
"""
import json
import math
import os
import sys

from PIL import Image

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import space_elevator as se  # noqa: E402

SRC = os.path.join(se.ROOT, 'tools/textures/orbital_src')
OUT = os.path.join(se.ROOT, 'af9-core/src/main/resources/assets/af9/textures/block')
FRAMES = 16
FRAMETIME = 4


def clamp(v):
    return max(0, min(255, round(v)))


def light(sprite, f):
    t = f / FRAMES
    out = sprite.copy()
    for y in range(16):
        for x in range(16):
            r, g, b, a = sprite.getpixel((x, y))
            if g > 150:                                       # the lit window: its tones breathe a little out of step
                k = 0.88 + 0.14 * math.sin(2 * math.pi * (t + ((x + y) % 4) / 16.0))
                out.putpixel((x, y), (clamp(r * k), clamp(g * k), clamp(b * k), a))
    return out


def magnet(sprite, f):
    t = f / FRAMES
    out = sprite.copy()
    for y in range(16):
        for x in range(16):
            r, g, b, a = sprite.getpixel((x, y))
            if r >= 190 and abs(r - g) < 6:                   # the bright pole pieces: a pulse runs along x
                k = 0.88 + 0.16 * math.sin(2 * math.pi * (t - (x % 8) / 8.0))
                out.putpixel((x, y), (clamp(r * k), clamp(g * k), clamp(b * k), a))
    return out


def cells_after_border(sprite, x, y):
    """The light casing's inside: its 8 x 8 cells start where the 4 px border ends, so a face is always whole cells."""
    return sprite.getpixel((4 + (x + 4) % 8, 4 + (y + 4) % 8))


def strip(frames):
    w, h = frames[0].size
    im = Image.new('RGBA', (w, h * len(frames)))
    for i, fr in enumerate(frames):
        im.paste(fr, (0, i * h))
    return im


def main():
    for name, anim, tx, ty, inside in (('orbital_light_casing', light, 4, 4, cells_after_border),
                                       ('orbital_magnet_casing', magnet, 1, 2, se.middle)):
        base = Image.open(os.path.join(SRC, name + '.png')).convert('RGBA')
        frames = [anim(base, f) for f in range(FRAMES)]
        strip(frames).save(os.path.join(OUT, name + '.png'))
        strip([se.ctm_sheet(fr, tx, ty, inside) for fr in frames]).save(os.path.join(OUT, name + '_ctm.png'))
        animation = {'animation': {'frametime': FRAMETIME, 'interpolate': True}}
        with open(os.path.join(OUT, name + '.png.mcmeta'), 'w', newline='\n') as f:
            json.dump(dict(animation, ldlib={'connection': 'af9:block/%s_ctm' % name}), f, indent=2)
            f.write('\n')
        with open(os.path.join(OUT, name + '_ctm.png.mcmeta'), 'w', newline='\n') as f:
            json.dump(animation, f, indent=2)
            f.write('\n')


if __name__ == '__main__':
    main()
