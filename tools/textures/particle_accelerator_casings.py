#!/usr/bin/env python3
"""The Particle Accelerator's casings: GT's Stable Titanium Machine Casing and its connected sheet (gt_src/, LGPL-3.0,
CREDITS-textures.md), recoloured like the Shock Capacitor Casing (electric_implosion.py).

Run from the repository root (needs Pillow):  python3 tools/textures/particle_accelerator_casings.py

  collision_containment_casing   the ring's shell: dark gunmetal blue-grey
  collision_shield_casing        the gates' corner bracing: hazard amber
"""
import colorsys
import json
import os

from PIL import Image

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), '../..')
SRC = os.path.join(ROOT, 'tools/textures/gt_src')
ASSETS = os.path.join(ROOT, 'af9-core/src/main/resources/assets/af9')
BLOCK = os.path.join(ASSETS, 'textures/block')

CASINGS = {
    # name: (hue, saturation factor, value factor)
    'collision_containment_casing': (0.60, 0.9, 0.55),
    'collision_shield_casing': (0.09, 2.6, 0.95),
}


def recolour(im, hue, sat, val):
    out = im.convert('RGBA')
    for y in range(out.height):
        for x in range(out.width):
            r, g, b, a = out.getpixel((x, y))
            h, sl, v = colorsys.rgb_to_hsv(r / 255, g / 255, b / 255)
            r, g, b = colorsys.hsv_to_rgb(hue, min(0.85, max(sl, 0.12) * sat), min(1.0, v * val))
            out.putpixel((x, y), (round(r * 255), round(g * 255), round(b * 255), a))
    return out


def dump(path, data):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, 'w') as f:
        json.dump(data, f, indent=2)
        f.write('\n')


def main():
    sprite = Image.open(os.path.join(SRC, 'machine_casing_stable_titanium.png'))
    sheet = Image.open(os.path.join(SRC, 'machine_casing_stable_titanium_ctm.png'))
    for name, (hue, sat, val) in CASINGS.items():
        recolour(sprite, hue, sat, val).save(os.path.join(BLOCK, name + '.png'))
        recolour(sheet, hue, sat, val).save(os.path.join(BLOCK, name + '_ctm.png'))
        dump(os.path.join(BLOCK, name + '.png.mcmeta'), {'ldlib': {'connection': 'af9:block/%s_ctm' % name}})
        dump(os.path.join(ASSETS, 'blockstates/%s.json' % name), {'variants': {'': {'model': 'af9:block/%s' % name}}})
        dump(os.path.join(ASSETS, 'models/block/%s.json' % name),
             {'parent': 'minecraft:block/cube_all', 'textures': {'all': 'af9:block/%s' % name}})
        dump(os.path.join(ASSETS, 'models/item/%s.json' % name), {'parent': 'af9:block/%s' % name})
        dump(os.path.join(ROOT, 'af9-core/src/main/resources/data/af9/loot_tables/blocks/%s.json' % name),
             {'type': 'minecraft:block', 'pools': [{'rolls': 1, 'entries': [{'type': 'minecraft:item',
              'name': 'af9:' + name}], 'conditions': [{'condition': 'minecraft:survives_explosion'}]}]})


if __name__ == '__main__':
    main()
