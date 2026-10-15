#!/usr/bin/env python3
"""Animates the optical items that are static: optical RAM, optical fibre cable, fibre interconnects. A light pulse runs over
the item (down the RAM, along the fibre, along the traces of the interconnects) and the strip gets a .mcmeta, like the
GTNH-style optical CPU (animated by its own strip). Run from the repository root (needs Pillow):
    python3 tools/textures/animate_optical.py
Safe to run again: an already animated strip is cut back to its first frame (the base picture) first. Run optical_items.py
before it to redraw the interconnect base."""
import json
import os

from PIL import Image

TEX = os.path.join(os.path.dirname(os.path.abspath(__file__)), '../../af9-core/src/main/resources/assets/af9/textures/item')
FRAMES = 16
FRAMETIME = 2


def blend(c, amount):
    return tuple(int(c[i] + (255 - c[i]) * amount) for i in range(3)) + (c[3],)


def animate(name, along, only=None):
    """along(x, y) -> position of a pixel along the pulse's way (0..15); only(c) -> whether a pixel takes part."""
    path = os.path.join(TEX, name + '.png')
    base = Image.open(path).convert('RGBA')
    base = base.crop((0, 0, 16, 16))
    strip = Image.new('RGBA', (16, 16 * FRAMES), (0, 0, 0, 0))
    for f in range(FRAMES):
        head = f - 2                                # two quiet frames, then the pulse enters and leaves
        fr = base.copy()
        for y in range(16):
            for x in range(16):
                c = base.getpixel((x, y))
                if c[3] == 0 or (only and not only(c)):
                    continue
                d = abs(along(x, y) - head)
                if d == 0:
                    fr.putpixel((x, y), blend(c, 0.6))
                elif d == 1:
                    fr.putpixel((x, y), blend(c, 0.28))
        strip.paste(fr, (0, f * 16))
    strip.save(path)
    with open(path + '.mcmeta', 'w') as fh:
        json.dump({'animation': {'frametime': FRAMETIME}}, fh)
    print('animated', name)


def main():
    animate('optical_ram', lambda x, y: y)
    animate('optical_fiber_cable', lambda x, y: (x - y + 15) // 2)
    animate('fiber_interconnects', lambda x, y: x, only=lambda c: c[2] > 200 and c[1] > 150)


if __name__ == '__main__':
    main()
