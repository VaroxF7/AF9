#!/usr/bin/env python3
"""The Orbital Light Casing and Orbital Magnet Casing: connected textures (LDLib's .mcmeta + `_ctm` sheet, the format
tools/textures/space_elevator.py describes) and a slight animation (16 frames).

Run from the repository root (needs Pillow):  python3 tools/textures/orbital_casings.py
The plain sprites are in tools/textures/orbital_src/; the script writes the animated strips (sprite: 16 x 16n, sheet:
32 x 32n, n frames each, both animated with the same frame time so the sheet follows the sprite) and the .mcmeta files.

  light   a ring of casings reads as one panel of lit windows (the border stays on the outside, the window repeats inside);
          a pulse of current runs down the gold coils (every 8 px, so it runs on across blocks)
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
            if r - b > 80 and r > 120:                                     # the lit window: its tones breathe a little out of step
                k = 0.86 + 0.16 * math.sin(2 * math.pi * (t - ((y - 4) % 8) / 8.0))
                out.putpixel((x, y), (clamp(r * k), clamp(g * k), clamp(b * k), a))
    return out


def light_glow(image, f):
    """The light casing's emissive layer while its station works (the `_active_emissive` texture, drawn over the plain one so it
    blooms): only the lit gold and chrome kept (of the sprite and of the connected sheet alike), driven harder than the idle
    pulse and running twice as fast down the coils."""
    t = f / FRAMES
    out = Image.new('RGBA', image.size, (0, 0, 0, 0))
    for y in range(image.size[1]):
        for x in range(image.size[0]):
            r, g, b, a = image.getpixel((x, y))
            if r - b > 80 and r > 120:
                k = 1.05 + 0.25 * math.sin(2 * math.pi * (2 * t - ((y - 4) % 8) / 8.0))
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


FLOOR = [(30, 31, 38), (36, 37, 45), (24, 25, 31)]
RIVET = [(246, 247, 251), (200, 205, 216), (150, 156, 170), (104, 109, 124)]


def joined_interior(sprite, x, y):
    """The light casing's inside where blocks join: one calm cell per block. The sprite's own glyph sits in the middle (4..11),
    a gold rail runs along the block seams (two pixels where two blocks meet, hidden under the border at the face's outer
    edge) and four 2 px conductors link the glyph to it and chrome rivets sit in the corners. Rail and conductors take their gold from the glyph's bright gold
    pixel (7, 4), so they pulse with it."""
    if 4 <= x <= 11 and 4 <= y <= 11:
        return sprite.getpixel((x, y))
    gold = sprite.getpixel((7, 4))

    def tone(f):
        return tuple(clamp(c * f) for c in gold[:3]) + (255,)

    if x in (0, 15) or y in (0, 15):
        return tone(0.86 if x == 0 or y == 0 else 0.64)
    if x in (7, 8) and (y < 4 or y > 11):
        return tone(0.82 if x == 7 else 0.68)
    if y in (7, 8) and (x < 4 or x > 11):
        return tone(0.82 if y == 7 else 0.68)
    if x in (1, 2, 13, 14) and y in (1, 2, 13, 14):         # chrome rivets, four of them where four cells meet
        return RIVET[(x in (13, 14)) + (y in (13, 14)) + (x in (2, 14) and y in (2, 14))] + (255,)
    return FLOOR[(x * 3 + y * 5) % 3] + (255,)


def strip(frames):
    w, h = frames[0].size
    im = Image.new('RGBA', (w, h * len(frames)))
    for i, fr in enumerate(frames):
        im.paste(fr, (0, i * h))
    return im


def write_glow(name, tx, ty, inside):
    base = Image.open(os.path.join(SRC, name + '.png')).convert('RGBA')
    frames = [light(base, f) for f in range(FRAMES)]
    glow = name + '_active_emissive'
    strip([light_glow(fr, f) for f, fr in enumerate(frames)]).save(os.path.join(OUT, glow + '.png'))
    strip([light_glow(se.ctm_sheet(fr, tx, ty, inside), f) for f, fr in enumerate(frames)]).save(
        os.path.join(OUT, glow + '_ctm.png'))
    animation = {'animation': {'frametime': FRAMETIME, 'interpolate': True}}
    with open(os.path.join(OUT, glow + '.png.mcmeta'), 'w', newline='\n') as f:
        json.dump(dict(animation, ldlib={'connection': 'af9:block/%s_ctm' % glow}), f, indent=2)
        f.write('\n')
    with open(os.path.join(OUT, glow + '_ctm.png.mcmeta'), 'w', newline='\n') as f:
        json.dump(animation, f, indent=2)
        f.write('\n')


def write_active_copy(name):
    """The active model's bottom layer is its own `_active` texture (like every other af9 active casing): a copy of the plain one."""
    import shutil
    for suffix in ('', '_ctm'):
        for ext in ('.png', '.png.mcmeta'):
            shutil.copyfile(os.path.join(OUT, name + suffix + ext), os.path.join(OUT, name + '_active' + suffix + ext))
    path = os.path.join(OUT, name + '_active.png.mcmeta')
    with open(path) as f:
        text = f.read().replace(name + '_ctm', name + '_active_ctm')
    with open(path, 'w', newline='\n') as f:
        f.write(text)


def main():
    write_glow('orbital_light_casing', 4, 4, joined_interior)
    for name, anim, tx, ty, inside in (('orbital_light_casing', light, 4, 4, joined_interior),
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
    write_active_copy('orbital_light_casing')


if __name__ == '__main__':
    main()
