#!/usr/bin/env python3
"""The DTPF's Inconel 738 Module Casing: GregTech CEu's Atomic Casing (gcym/atomic_casing and its connected-texture sheet
atomic_casing_ctm, the structure's placeholder) with its purple moved to the hall's Inconel blue. Only the colour changes: every
pixel keeps its place and its shading, the frame, the panel and the sheet's quadrants stay as they are.

    python3 tools/textures/dtpf_module_casing.py [--preview <dir>]

Sources: tools/textures/gt_src/atomic_casing.png and atomic_casing_ctm.png (GregTech CEu Modern, LGPL-3.0). Writes the block's
sprite, `_ctm` sheet and .mcmeta (LDLib's connection) into af9-core's textures; the block state, model and loot table come from
dtpf_modules.py.
"""
import colorsys
import os
import sys

from PIL import Image

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
import dtpf_blocks as db  # noqa: E402
import space_elevator as se  # noqa: E402

GT = os.path.join(HERE, 'gt_src')
NAME = 'dtpf_module_casing'
HUE = 0.605            # the hall's blue (the Atomic Casing's purple is 0.75)
SAT = 1.25             # a little deeper than the purple
VAL = 1.18             # and a little lighter, so the block does not vanish in the hall's dark


def recolour(im):
    out = im.copy()
    for y in range(out.height):
        for x in range(out.width):
            r, g, b, a = out.getpixel((x, y))
            h, s, v = colorsys.rgb_to_hsv(r / 255, g / 255, b / 255)
            r, g, b = colorsys.hsv_to_rgb(HUE, min(1.0, s * SAT), min(1.0, v * VAL))
            out.putpixel((x, y), (round(r * 255), round(g * 255), round(b * 255), a))
    return out


def build():
    sprite = recolour(Image.open(os.path.join(GT, 'atomic_casing.png')).convert('RGBA'))
    sheet = recolour(Image.open(os.path.join(GT, 'atomic_casing_ctm.png')).convert('RGBA'))
    return sprite, sheet


def apply():
    sprite, sheet = build()
    se.OUT = db.BLOCK
    se.save(sprite, NAME)
    se.save(sheet, NAME + '_ctm')
    with open(os.path.join(db.BLOCK, NAME + '.png.mcmeta'), 'w', newline='\n') as f:
        f.write('{\n    "ldlib": {\n        "connection": "af9:block/%s_ctm"\n    }\n}\n' % NAME)
    return sprite, sheet


def preview(path):
    """The original and the recoloured one: the sprite, the sheet, and a 5 x 3 slab joined the way the game joins it."""
    os.makedirs(path, exist_ok=True)
    sprite, sheet = apply()
    orig = Image.open(os.path.join(GT, 'atomic_casing.png')).convert('RGBA')
    orig_sheet = Image.open(os.path.join(GT, 'atomic_casing_ctm.png')).convert('RGBA')
    sheet_im = Image.new('RGBA', (1100, 460), (12, 14, 20, 255))
    x = 10
    for im, big in ((orig, 160), (sprite, 160), (orig_sheet, 160), (sheet, 160)):
        sheet_im.paste(im.resize((big, big), Image.NEAREST), (x, 10))
        x += big + 10
    for i, (sp, sh) in enumerate(((orig, orig_sheet), (sprite, sheet))):
        sheet_im.paste(se.tile(sp, sh, 5, 3).resize((400, 240), Image.NEAREST), (10 + i * 410, 190))
    sheet_im.save(os.path.join(path, 'module_casing_recolour.png'))


if __name__ == '__main__':
    if len(sys.argv) >= 3 and sys.argv[1] == '--preview':
        preview(sys.argv[2])
    else:
        apply()
