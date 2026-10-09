#!/usr/bin/env python3
"""The faces of the Space Elevator's Mining Modules (the sub-multiblock controllers, MK-I to MK-III).

Run from the repository root (needs Pillow):  python3 tools/textures/space_module.py

An overlay for the controller's front (over the elevator's base casing): GTNH's miner module glyph (the pickaxe,
tools/textures/gtnh_space_elevator/OVERLAY_SIDE_MINER_MODULE.png) on GTNH's support structure plate, with one pip a tier.
Writes kubejs/assets/gtceu/textures/block/multiblock/space_mining_module_mk<tier>/overlay_front*.png.
"""
import os

from PIL import Image

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), '../..')
SRC = os.path.join(ROOT, 'tools/textures/gtnh_space_elevator')
OUT = os.path.join(ROOT, 'kubejs/assets/gtceu/textures/block/multiblock')
TINT = [(0x4F, 0xC3, 0xF7, 255), (0x66, 0xE0, 0x8A, 255), (0xFF, 0xB3, 0x47, 255)]


def face(tier):
    plate = Image.open(os.path.join(SRC, 'SupportStructure_Side.png')).convert('RGBA').copy()
    plate.alpha_composite(Image.open(os.path.join(SRC, 'OVERLAY_SIDE_MINER_MODULE.png')).convert('RGBA'))
    for i in range(tier):
        for dx, dy in ((0, 0), (1, 0), (0, 1), (1, 1)):
            plate.putpixel((5 + i * 3 + dx, 12 + dy), TINT[tier - 1])
    return plate


def glow(tier):
    """The pips only, for the emissive layer."""
    im = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
    for i in range(tier):
        for dx, dy in ((0, 0), (1, 0), (0, 1), (1, 1)):
            im.putpixel((5 + i * 3 + dx, 12 + dy), TINT[tier - 1])
    return im


def main():
    for tier in range(1, 4):
        folder = os.path.join(OUT, 'space_mining_module_mk%d' % tier)
        os.makedirs(folder, exist_ok=True)
        base, light = face(tier), glow(tier)
        for name in ('overlay_front', 'overlay_front_active', 'overlay_front_paused'):
            base.save(os.path.join(folder, name + '.png'))
            light.save(os.path.join(folder, name + '_emissive.png'))


if __name__ == '__main__':
    main()
    print('Space module faces written')
