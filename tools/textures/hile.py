#!/usr/bin/env python3
"""The Hyper-Intensity Laser Engraver (GTNH's Industrial Laser Engraver): its casing, plate and the controller's face.

Run from the repository root (needs Pillow):  python3 tools/textures/hile.py

GTNH's art is in tools/textures/gtnh_hile/: the Laser Containment Casing and the engraver's face are copied. The Laser
Resistant Plate (a TESR block in GTNH, with no texture of its own) is drawn here: a dark plate with a lit focus ring where
the beam lands. Writes the blockstates, models, loot tables and textures of both blocks and the face (kubejs assets).
"""
import json
import os
import shutil

from PIL import Image

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), '../..')
SRC = os.path.join(ROOT, 'tools/textures/gtnh_hile')
ASSETS = os.path.join(ROOT, 'af9-core/src/main/resources/assets/af9')
DATA = os.path.join(ROOT, 'af9-core/src/main/resources/data/af9')
FACE = os.path.join(ROOT, 'kubejs/assets/gtceu/textures/block/multiblock/hile')
BLOCKS = ('laser_containment_casing', 'laser_resistant_plate')


def dump(path, obj):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, 'w') as f:
        f.write(json.dumps(obj, indent=2) + '\n')


def plate():
    im = Image.new('RGBA', (16, 16))
    for y in range(16):
        for x in range(16):
            f = 1.0
            if x in (0, 15) or y in (0, 15):
                f = 1.25 if (x == 0 or y == 0) else 0.7
            elif (x * 5 + y * 11) % 13 == 0:
                f = 1.08
            v = round(46 * f)
            im.putpixel((x, y), (v, v + 2, v + 8, 255))
    for x in range(16):                       # the focus ring: a hot circle round the middle
        for y in range(16):
            d = ((x - 7.5) ** 2 + (y - 7.5) ** 2) ** 0.5
            if 3.2 <= d < 4.4:
                im.putpixel((x, y), (255, 96, 64, 255))
            elif 4.4 <= d < 5.2:
                im.putpixel((x, y), (120, 40, 40, 255))
            elif d < 1.6:
                im.putpixel((x, y), (255, 214, 190, 255))
    return im


def main():
    tex = os.path.join(ASSETS, 'textures/block')
    os.makedirs(tex, exist_ok=True)
    shutil.copy(os.path.join(SRC, 'MACHINE_CASING_LASER.png'), os.path.join(tex, 'laser_containment_casing.png'))
    plate().save(os.path.join(tex, 'laser_resistant_plate.png'))
    for b in BLOCKS:
        dump(os.path.join(ASSETS, f'blockstates/{b}.json'), {'variants': {'': {'model': f'af9:block/{b}'}}})
        dump(os.path.join(ASSETS, f'models/block/{b}.json'),
             {'parent': 'minecraft:block/cube_all', 'textures': {'all': f'af9:block/{b}'}})
        dump(os.path.join(ASSETS, f'models/item/{b}.json'), {'parent': f'af9:block/{b}'})
        dump(os.path.join(DATA, f'loot_tables/blocks/{b}.json'), {
            'type': 'minecraft:block',
            'pools': [{'rolls': 1, 'entries': [{'type': 'minecraft:item', 'name': f'af9:{b}'}],
                       'conditions': [{'condition': 'minecraft:survives_explosion'}]}]})
    os.makedirs(FACE, exist_ok=True)
    faces = {'overlay_front': 'OVERLAY_FRONT_ENGRAVER', 'overlay_front_emissive': 'OVERLAY_FRONT_ENGRAVER_GLOW',
             'overlay_front_active': 'OVERLAY_FRONT_ENGRAVER_ACTIVE',
             'overlay_front_active_emissive': 'OVERLAY_FRONT_ENGRAVER_ACTIVE_GLOW',
             'overlay_front_paused': 'OVERLAY_FRONT_ENGRAVER', 'overlay_front_paused_emissive': 'OVERLAY_FRONT_ENGRAVER_GLOW'}
    for name, src in faces.items():
        shutil.copy(os.path.join(SRC, src + '.png'), os.path.join(FACE, name + '.png'))
        if 'active' in name and 'paused' not in name:
            dump(os.path.join(FACE, name + '.png.mcmeta'), {'animation': {'frametime': 2}})


if __name__ == '__main__':
    main()
    print('HILE assets written')
