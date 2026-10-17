#!/usr/bin/env python3
"""Endion and Resonant Endion coil textures with connected textures (LDLib's .mcmeta + `_ctm` sheet, the format
tools/textures/space_elevator.py describes), so a coil bank reads as one panel: the two-pixel frame stays only on the
outside of the bank and the diagonal weave runs on across the seams (it repeats every 4 px, so it lines up between blocks).

Run from the repository root (needs Pillow):  python3 tools/textures/endion_coils.py
Edits the textures in place (the sprites stay as they are, only the sheets and .mcmeta files are written); the bloom layer
of the working coil gets a sheet too, so its glow continues over the seams.
"""
import os
import sys

from PIL import Image

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import space_elevator as se  # noqa: E402

OUT = os.path.join(se.ROOT, 'af9-core/src/main/resources/assets/af9/textures/block/coils')
NAMES = ['endion_coil', 'endion_coil_bloom', 'resonant_endion_coil', 'resonant_endion_coil_bloom']


def main():
    se.OUT = OUT
    for name in NAMES:
        sprite = Image.open(os.path.join(OUT, name + '.png')).convert('RGBA')
        se.save(se.ctm_sheet(sprite, 2, 2), name + '_ctm')
        with open(os.path.join(OUT, name + '.png.mcmeta'), 'w', newline='\n') as f:
            f.write('{\n    "ldlib": {\n        "connection": "af9:block/coils/%s_ctm"\n    }\n}\n' % name)


if __name__ == '__main__':
    main()
