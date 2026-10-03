#!/usr/bin/env python3
"""Draws the reticle textures of every mask class (chrome, psm, euv) from the reticle that is there.

    python3 tools/textures/reticles.py            (from the repository root; needs Pillow)

A reticle is a quartz plate (frame), a ring, an interior with the chip's pattern and the lens colour in the four corners.
The classes differ in colours and polarity: chrome = dark chrome with the pattern clear; phase-shift = lavender with the pattern
in purple; EUV = a blue mirror with the pattern in black. For every chip the reticle of its native class
(`<chip>_reticle.png`) is read, its pattern taken, and the reticle of every class from the native one up is drawn again.
Safe to run again: the class a texture is in is detected from its colours.
The native class of each chip (AF9_RETICLE_TABLE in startup_scripts/gtceu/reticles.js) is read from that file.
"""
import os
import re
import sys

from PIL import Image

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), '../..')
TEX = os.path.join(ROOT, 'af9-core/src/main/resources/assets/af9/textures/item')
TABLE = os.path.join(ROOT, 'kubejs/startup_scripts/gtceu/reticles.js')

# interior colours of the classes: background (a function of the pixel: the EUV mirror is a checker), pattern, ring
CLASSES = ['chrome', 'psm', 'euv']
BG = {
    'chrome': lambda x, y: (52, 56, 62, 255),
    'psm': lambda x, y: (206, 212, 238, 255),
    'euv': lambda x, y: (92, 120, 196, 255) if (x + y) % 5 else (120, 150, 220, 255),
}
PATTERN = {'chrome': (190, 226, 238, 255), 'psm': (96, 72, 116, 255), 'euv': (26, 26, 34, 255)}
RING = {'chrome': (52, 56, 62, 255), 'psm': (96, 72, 116, 255), 'euv': (26, 26, 34, 255)}
# the lens colour of a chip that changed it (the lens decides the corners of the native reticle)
LENS_OVERRIDE = {'advanced_soc': (240, 140, 40, 255)}


def look_of(im):
    colours = {im.getpixel((x, y)) for x in range(16) for y in range(16)}
    if (96, 72, 116, 255) in colours:
        return 'psm'
    if (92, 120, 196, 255) in colours:
        return 'euv'
    return 'chrome'


def native_class(native):
    return 0 if native <= 1 else 1 if native <= 4 else 2


def is_ring(x, y):
    return (x in (3, 12) or y in (3, 12)) and 3 <= x <= 12 and 3 <= y <= 12


def is_corner(x, y):
    return (x, y) in ((3, 3), (12, 3), (3, 12), (12, 12))


def convert(src, src_class, dst_class, lens=None):
    out = src.copy()
    for y in range(3, 13):
        for x in range(3, 13):
            if is_corner(x, y):
                if lens:
                    out.putpixel((x, y), lens)
            elif is_ring(x, y):
                out.putpixel((x, y), RING[dst_class])
            else:
                p = src.getpixel((x, y))
                # chrome: dark ground, light pattern; psm / euv: light ground, dark pattern (the ground of an EUV mirror
                # is a checker of two blues)
                is_pattern = p == PATTERN[src_class]
                out.putpixel((x, y), PATTERN[dst_class] if is_pattern else BG[dst_class](x, y))
    return out


def main():
    with open(TABLE, encoding='utf-8') as f:
        table = re.findall(r"\['([a-z_]+)', '[^']+', (\d)\]", f.read())
    written = 0
    for chip, native in table:
        k0 = native_class(int(native))
        path = os.path.join(TEX, f'{chip}_reticle.png')
        src = Image.open(path).convert('RGBA')
        src_class = look_of(src)
        lens = LENS_OVERRIDE.get(chip)
        for k in range(k0, 3):
            cls = CLASSES[k]
            name = f'{chip}_reticle.png' if k == k0 else f'{chip}_{cls}_reticle.png'
            if src_class == cls and lens is None:
                out = src
            else:
                out = convert(src, src_class, cls, lens)
            out.save(os.path.join(TEX, name))
            written += 1
    print(f'reticles: {written} textures written for {len(table)} chips', file=sys.stderr)


main()
