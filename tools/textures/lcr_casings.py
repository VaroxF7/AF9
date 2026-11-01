#!/usr/bin/env python3
"""The Large Chemical Reactor's two casings: the Inconel 625 Casing (the end plate) and the Inconel 718 Casing (the barrel and the
slopes that round it). Both are GregTech CEu's Inert PTFE casing (machine_casing_inert_ptfe and its connected-texture sheet
machine_casing_inert_ptfe_ctm): every pixel keeps its place and its shading, only the grey is moved onto the alloy's colour
(Inconel 625 teal steel 0x5f7a85, Inconel 718 bronze brown 0x8a6f58, the colours of the materials in AF9Materials).

    python3 tools/textures/lcr_casings.py [--preview <dir>]

Sources: tools/textures/gt_src/machine_casing_inert_ptfe.png and machine_casing_inert_ptfe_ctm.png (GregTech CEu Modern, LGPL-3.0).
Writes af9-core's block sprites, `_ctm` sheets and .mcmeta (LDLib's connection). The slope uses the 718 sprite, as a plain copy
(inconel_718_slope.png, no sheet). Block states, models, loot tables and names are plain files in af9-core's resources.
"""
import os
import sys

from PIL import Image

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
import dtpf_blocks as db  # noqa: E402
import space_elevator as se  # noqa: E402

GT = os.path.join(HERE, 'gt_src')
CASINGS = {
    'inconel_625_casing': (0x5f, 0x7a, 0x85),
    'inconel_718_casing': (0x8a, 0x6f, 0x58),
}
LO, HI = 27, 78          # the darkest and the lightest grey of the PTFE casing
DARK, LIGHT = 0.30, 1.08  # the grey's range as a share of the alloy's colour (the lightest keeps a little over the colour)


def recolour(im, colour):
    out = im.copy()
    for y in range(out.height):
        for x in range(out.width):
            r, g, b, a = out.getpixel((x, y))
            t = max(0.0, min(1.0, (r - LO) / (HI - LO)))
            k = DARK + (LIGHT - DARK) * t
            out.putpixel((x, y), tuple(min(255, round(c * k)) for c in colour) + (a,))
    return out


def sources():
    return (Image.open(os.path.join(GT, 'machine_casing_inert_ptfe.png')).convert('RGBA'),
            Image.open(os.path.join(GT, 'machine_casing_inert_ptfe_ctm.png')).convert('RGBA'))


def apply():
    sprite, sheet = sources()
    se.OUT = db.BLOCK
    built = {}
    for name, colour in CASINGS.items():
        built[name] = (recolour(sprite, colour), recolour(sheet, colour))
        se.save(built[name][0], name)
        se.save(built[name][1], name + '_ctm')
        with open(os.path.join(db.BLOCK, name + '.png.mcmeta'), 'w', newline='\n') as f:
            f.write('{\n    "ldlib": {\n        "connection": "af9:block/%s_ctm"\n    }\n}\n' % name)
    # the slope takes the 718 sprite as a plain texture of its own: it is not a cube, so no connected texture
    se.save(built['inconel_718_casing'][0], 'inconel_718_slope')
    return built


def preview(path):
    """GT's Inert PTFE casing and the two recoloured ones: sprite, sheet and a 5 x 3 slab joined the way the game joins it."""
    os.makedirs(path, exist_ok=True)
    built = apply()
    sprite, sheet = sources()
    rows = [('Inert PTFE (GT)', sprite, sheet)] + [(n, built[n][0], built[n][1]) for n in CASINGS]
    im = Image.new('RGBA', (1000, 3 * 250 + 20), (14, 16, 22, 255))
    for i, (_, sp, sh) in enumerate(rows):
        y = 10 + i * 250
        im.paste(sp.resize((224, 224), Image.NEAREST), (10, y))
        im.paste(sh.resize((224, 224), Image.NEAREST), (244, y))
        im.paste(se.tile(sp, sh, 5, 3).resize((480, 224), Image.NEAREST), (480, y))
    im.save(os.path.join(path, 'lcr_casings_recolour.png'))


if __name__ == '__main__':
    if len(sys.argv) >= 3 and sys.argv[1] == '--preview':
        preview(sys.argv[2])
    else:
        apply()
