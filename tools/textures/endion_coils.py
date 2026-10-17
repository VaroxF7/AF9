#!/usr/bin/env python3
"""Endion and Resonant Endion coil textures with connected textures (LDLib's .mcmeta + `_ctm` sheet, the format
tools/textures/space_elevator.py describes), so a coil bank reads as one panel: the two-pixel frame stays only on the
outside of the bank and the diagonal weave runs on across the seams (it repeats every 4 px, so it lines up between blocks).

Run from the repository root (needs Pillow):  python3 tools/textures/endion_coils.py
Edits the textures in place (the sprites stay as they are, only the sheets and .mcmeta files are written); the bloom layer
of the working coil gets a sheet too, so its glow continues over the seams.

The bloom layer is built the way GTCEu's superconducting coils do it: a full, opaque copy of the coil whose frame is
unchanged and whose weave is recoloured to a bright ramp of the coil's own hue (the bloom pass lifts the brightest pixels).
"""
import os
import sys

from PIL import Image

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import space_elevator as se  # noqa: E402

OUT = os.path.join(se.ROOT, 'af9-core/src/main/resources/assets/af9/textures/block/coils')
NAMES = ['endion_coil', 'endion_coil_bloom', 'resonant_endion_coil', 'resonant_endion_coil_bloom']

# weave tone of the coil -> its lit tone in the bloom layer
LIT = {
    'endion_coil': {(0x35, 0x1a, 0x65): (0x80, 0x62, 0xd0), (0x73, 0x45, 0xce): (0xb0, 0x8e, 0xf0),
                    (0xbf, 0x9d, 0xfa): (0xf3, 0xea, 0xff)},
    'resonant_endion_coil': {(0x3c, 0x23, 0x75): (0x76, 0x64, 0xd8), (0x8c, 0x69, 0xf2): (0xa8, 0x96, 0xf8),
                             (0x98, 0xe5, 0xfe): (0xe2, 0xfb, 0xff)},
}


def bloom_of(name):
    """The coil with its weave lit up (the frame stays as it is)."""
    sprite = Image.open(os.path.join(OUT, name + '.png')).convert('RGBA')
    ramp = LIT[name]
    for y in range(16):
        for x in range(16):
            r, g, b, a = sprite.getpixel((x, y))
            if (r, g, b) in ramp:
                sprite.putpixel((x, y), ramp[(r, g, b)] + (a,))
    return sprite


def main():
    se.OUT = OUT
    for name in ('endion_coil', 'resonant_endion_coil'):
        se.save(bloom_of(name), name + '_bloom')
    for name in NAMES:
        sprite = Image.open(os.path.join(OUT, name + '.png')).convert('RGBA')
        se.save(se.ctm_sheet(sprite, 2, 2), name + '_ctm')
        with open(os.path.join(OUT, name + '.png.mcmeta'), 'w', newline='\n') as f:
            f.write('{\n    "ldlib": {\n        "connection": "af9:block/coils/%s_ctm"\n    }\n}\n' % name)


if __name__ == '__main__':
    main()
