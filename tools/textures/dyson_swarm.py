#!/usr/bin/env python3
"""The Dyson Swarm: its casings, the controller's face and the three sails (GTNH Intergalactic's art).

Run from the repository root (needs Pillow):  python3 tools/textures/dyson_swarm.py

GTNH's own art is in tools/textures/gtnh_dyson_swarm/: the nine casings (a top and a side texture each) and the
controller's face are copied; the sail is GTNH's Dyson swarm module, recoloured once a tier (the module is a sheet of
four photovoltaic panels: its frame, cells and stripes are the three colours of a tier).
Writes the blockstates, models, loot tables and textures of the casings, the item models and textures of the sails
and the controller's face (kubejs assets). The registry, lang and tag entries are edited by hand.
"""
import json
import os
import shutil

from PIL import Image

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), '../..')
SRC = os.path.join(ROOT, 'tools/textures/gtnh_dyson_swarm')
ASSETS = os.path.join(ROOT, 'af9-core/src/main/resources/assets/af9')
DATA = os.path.join(ROOT, 'af9-core/src/main/resources/data/af9')
FACE = os.path.join(ROOT, 'kubejs/assets/gtceu/textures/block/multiblock/dyson_swarm')

# AF9 block id -> GTNH's texture name
CASINGS = {
    'dyson_receiver_casing': 'ReceiverCasing',
    'dyson_receiver_dish': 'ReceiverDish',
    'dyson_deployment_casing': 'DeploymentUnitCasing',
    'dyson_deployment_core': 'DeploymentUnitCore',
    'dyson_deployment_magnet': 'DeploymentUnitMagnet',
    'dyson_control_casing': 'ControlCasing',
    'dyson_control_primary': 'ControlPrimary',
    'dyson_control_secondary': 'ControlSecondary',
    'dyson_control_toroid': 'ControlToroid',
}

# sail id -> (frame, cells, stripes)
SAILS = {
    'allthemodium_sail': ((0x3a, 0x2b, 0x05), (0xd9, 0xa5, 0x21), (0xff, 0xf2, 0xa6)),
    'unobtainium_alloy_sail': ((0x1c, 0x0b, 0x33), (0x8a, 0x3f, 0xd0), (0xe7, 0xc8, 0xff)),
    'chromodynium_star_matter_tritan_alloy_sail': ((0x14, 0x06, 0x1f), (0xff, 0x3c, 0x78), (0x3c, 0xff, 0xb4)),
}


def write(path, text):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, 'w') as f:
        f.write(text)


def dump(path, obj):
    write(path, json.dumps(obj, indent=2) + '\n')


def casings():
    for block, name in CASINGS.items():
        os.makedirs(os.path.join(ASSETS, 'textures/block'), exist_ok=True)
        shutil.copy(os.path.join(SRC, name + '.png'), os.path.join(ASSETS, f'textures/block/{block}_top.png'))
        shutil.copy(os.path.join(SRC, name + '_Side.png'), os.path.join(ASSETS, f'textures/block/{block}.png'))
        dump(os.path.join(ASSETS, f'blockstates/{block}.json'), {'variants': {'': {'model': f'af9:block/{block}'}}})
        dump(os.path.join(ASSETS, f'models/block/{block}.json'), {
            'parent': 'minecraft:block/cube_column',
            'textures': {'end': f'af9:block/{block}_top', 'side': f'af9:block/{block}'}})
        dump(os.path.join(ASSETS, f'models/item/{block}.json'), {'parent': f'af9:block/{block}'})
        dump(os.path.join(DATA, f'loot_tables/blocks/{block}.json'), {
            'type': 'minecraft:block',
            'pools': [{'rolls': 1, 'entries': [{'type': 'minecraft:item', 'name': f'af9:{block}'}],
                       'conditions': [{'condition': 'minecraft:survives_explosion'}]}]})


def sails():
    module = Image.open(os.path.join(SRC, 'dysonSwarmModule.png')).convert('RGBA')
    for sail, (frame, cells, stripes) in SAILS.items():
        out = module.copy()
        for y in range(out.height):
            for x in range(out.width):
                r, g, b, a = out.getpixel((x, y))
                if a == 0:
                    continue
                if (r, g, b) == (0x0b, 0x13, 0x34):
                    out.putpixel((x, y), frame + (255,))
                elif (r, g, b) == (0x17, 0x79, 0xb0):
                    out.putpixel((x, y), cells + (255,))
                elif (r, g, b) == (255, 255, 255):
                    out.putpixel((x, y), stripes + (255,))
        os.makedirs(os.path.join(ASSETS, 'textures/item/dyson'), exist_ok=True)
        out.save(os.path.join(ASSETS, f'textures/item/dyson/{sail}.png'))
        dump(os.path.join(ASSETS, f'models/item/{sail}.json'),
             {'parent': 'minecraft:item/generated', 'textures': {'layer0': f'af9:item/dyson/{sail}'}})


def face():
    os.makedirs(FACE, exist_ok=True)
    # name -> (GTNH file); the paused face is the idle one
    files = {'overlay_front': 'OVERLAY_FRONT_DYSONSPHERE', 'overlay_front_active': 'OVERLAY_FRONT_DYSONSPHERE_ACTIVE',
             'overlay_front_emissive': 'OVERLAY_FRONT_DYSONSPHERE_GLOW',
             'overlay_front_active_emissive': 'OVERLAY_FRONT_DYSONSPHERE_ACTIVE_GLOW',
             'overlay_front_paused': 'OVERLAY_FRONT_DYSONSPHERE',
             'overlay_front_paused_emissive': 'OVERLAY_FRONT_DYSONSPHERE_GLOW'}
    for name, src in files.items():
        shutil.copy(os.path.join(SRC, src + '.png'), os.path.join(FACE, name + '.png'))


if __name__ == '__main__':
    casings()
    sails()
    face()
    print('Dyson swarm assets written')
