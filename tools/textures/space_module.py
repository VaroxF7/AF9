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


# the Pumping Module's glyph: a drop over the support plate (16 x 16, rows from the top; 1 = drop, 2 = its highlight)
DROP = ['0000000110000000', '0000000110000000', '0000001111000000', '0000001111000000', '0000011111100000',
        '0000011111100000', '0000111111110000', '0000111111110000', '0001111111111000', '0001111111111000',
        '0001111111111000', '0001111111111000', '0000111111110000', '0000011111100000']
DROP_TINT = (0x4F, 0xC3, 0xF7, 255)


def pumping():
    plate = Image.open(os.path.join(SRC, 'SupportStructure_Side.png')).convert('RGBA').copy()
    light = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
    for y, row in enumerate(DROP):
        for x, cell in enumerate(row):
            if cell == '1':
                shade = 0 if (x + y) % 5 else 1
                colour = tuple(min(255, c + 40) for c in DROP_TINT[:3]) + (255,) if shade else DROP_TINT
                plate.putpixel((x, y + 1), colour)
                light.putpixel((x, y + 1), DROP_TINT)
    return plate, light


def main():
    for tier in range(1, 4):
        folder = os.path.join(OUT, 'space_mining_module_mk%d' % tier)
        os.makedirs(folder, exist_ok=True)
        base, light = face(tier), glow(tier)
        for name in ('overlay_front', 'overlay_front_active', 'overlay_front_paused'):
            base.save(os.path.join(folder, name + '.png'))
            light.save(os.path.join(folder, name + '_emissive.png'))


def main_pumping():
    folder = os.path.join(OUT, 'space_pumping_module')
    os.makedirs(folder, exist_ok=True)
    base, light = pumping()
    for name in ('overlay_front', 'overlay_front_active', 'overlay_front_paused'):
        base.save(os.path.join(folder, name + '.png'))
        light.save(os.path.join(folder, name + '_emissive.png'))


if __name__ == '__main__':
    main_pumping()
    main()
    print('Space module faces written')
