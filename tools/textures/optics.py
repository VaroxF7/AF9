#!/usr/bin/env python3
"""Draws the EUV optics textures: the ULE glass substrate (a pale, ground-flat plate) and the Mo/Si multilayer mirror (a bluish
silver disc on that plate). Run from the repository root (needs Pillow):  python3 tools/textures/optics.py"""
import math
import os

from PIL import Image

TEX = os.path.join(os.path.dirname(os.path.abspath(__file__)), '../../kubejs/assets/kubejs/textures/item')


def substrate():
    im = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
    for y in range(2, 14):
        for x in range(2, 14):
            c = (200, 230, 240, 255)
            if y == 2 or x == 2:
                c = (236, 248, 252, 255)       # lit edges
            elif y == 13 or x == 13:
                c = (132, 164, 180, 255)       # shaded edges
            elif (x + y) % 7 == 0 and 4 <= x <= 11 and 4 <= y <= 11:
                c = (222, 242, 250, 255)       # a glint across the glass
            im.putpixel((x, y), c)
    for x, y in ((2, 2), (13, 2), (2, 13), (13, 13)):
        im.putpixel((x, y), (0, 0, 0, 0))      # rounded corners
    return im


def mirror():
    im = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
    cx = cy = 7.5
    for y in range(16):
        for x in range(16):
            d = math.hypot(x - cx, y - cy)
            if d > 6.6:
                continue
            if d > 5.6:
                c = (44, 50, 84, 255)           # the rim
            else:
                # bluish silver, lighter toward the upper left
                t = d / 5.6
                light = 1.0 - 0.45 * t + 0.25 * (-(x - cx) - (y - cy)) / 11
                light = max(0.0, min(1.0, light))
                c = (int(70 + 150 * light), int(86 + 150 * light), int(130 + 120 * light), 255)
                # the multilayer: faint rings
                if int(d * 2) % 2 == 0:
                    c = (c[0] - 14, c[1] - 14, c[2] - 8, 255)
            im.putpixel((x, y), c)
    # the highlight
    for x, y in ((4, 4), (5, 4), (4, 5), (6, 3), (3, 6)):
        im.putpixel((x, y), (236, 244, 255, 255))
    return im


substrate().save(os.path.join(TEX, 'ule_glass_substrate.png'))
mirror().save(os.path.join(TEX, 'mo_si_mirror.png'))
print('optics: 2 textures')
