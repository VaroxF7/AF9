"""Laser Containment Casing (HILE): the GTNH sprite plus its connected-texture sheet (LDLib .mcmeta + `_ctm`, the format of
space_elevator.ctm_sheet), so a wall of the casing loses the frame between blocks and the diagonal sheen runs on.

Run: python3 tools/textures/laser_containment.py"""
import json
import os
import sys

from PIL import Image

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import space_elevator as se  # noqa: E402

BLOCK = os.path.join(os.path.dirname(os.path.abspath(__file__)), '..', '..', 'af9-core', 'src', 'main', 'resources',
                     'assets', 'af9', 'textures', 'block')


def inside(sprite, x, y):
    """The sheen is diagonal; run it on across the block borders from the sprite's first interior row."""
    return sprite.getpixel((1 + (x + y) % 14, 1))


def main():
    sprite = Image.open(os.path.join(BLOCK, 'laser_containment_casing.png')).convert('RGBA')
    se.ctm_sheet(sprite, 1, 1, inside).save(os.path.join(BLOCK, 'laser_containment_casing_ctm.png'))
    with open(os.path.join(BLOCK, 'laser_containment_casing.png.mcmeta'), 'w') as f:
        json.dump({'ldlib': {'connection': 'af9:block/laser_containment_casing_ctm'}}, f, indent=2)
        f.write('\n')


if __name__ == '__main__':
    main()
