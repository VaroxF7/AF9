#!/usr/bin/env python3
"""Draws the photonic item textures of the lithography line: optical CPU, optical RAM, nano particles, optical fibre cable and
fibre interconnects. Run from the repository root (needs Pillow):  python3 tools/textures/optical_items.py [preview.png]
All five share one palette: gunmetal body, steel bevel, amber contacts, cyan light."""
import os
import sys

from PIL import Image

TEX = os.path.join(os.path.dirname(os.path.abspath(__file__)), '../../af9-core/src/main/resources/assets/af9/textures/item')

K = (14, 18, 24, 255)       # outline / void
D = (30, 38, 48, 255)       # dark gunmetal
M = (52, 62, 74, 255)       # gunmetal
L = (96, 110, 124, 255)     # steel
H = (170, 184, 196, 255)    # steel highlight
G = (216, 156, 52, 255)     # amber contact
GL = (252, 214, 112, 255)   # amber highlight
GD = (150, 98, 30, 255)     # amber shade
C = (60, 220, 240, 255)     # cyan light
CL = (200, 252, 255, 255)   # cyan hot core
CD = (24, 120, 150, 255)    # cyan dim
V = (150, 110, 230, 255)    # violet accent
PCB = (22, 58, 56, 255)
PCBL = (34, 88, 82, 255)


def new():
    return Image.new('RGBA', (16, 16), (0, 0, 0, 0))


def rect(im, x0, y0, x1, y1, c):
    for y in range(y0, y1 + 1):
        for x in range(x0, x1 + 1):
            im.putpixel((x, y), c)


def pts(im, c, *p):
    for x, y in p:
        im.putpixel((x, y), c)


def optical_cpu():
    im = new()
    rect(im, 1, 1, 14, 14, K)
    rect(im, 2, 2, 13, 13, L)                       # steel heat spreader
    rect(im, 2, 2, 13, 2, H)
    rect(im, 2, 2, 2, 13, H)
    rect(im, 13, 3, 13, 13, M)
    rect(im, 3, 13, 13, 13, M)
    rect(im, 4, 4, 11, 11, K)                       # recessed die window
    rect(im, 5, 5, 10, 10, D)
    rect(im, 6, 6, 9, 9, CD)                        # photonic core
    rect(im, 7, 7, 8, 8, CL)
    pts(im, C, (6, 6), (9, 6), (6, 9), (9, 9))
    # waveguides out to the four sides
    pts(im, C, (7, 5), (8, 5), (7, 10), (8, 10), (5, 7), (5, 8), (10, 7), (10, 8))
    pts(im, V, (3, 3), (12, 3), (3, 12), (12, 12))  # corner screws in violet glass
    # amber pins along the bottom and sides
    for x in (4, 6, 9, 11):
        pts(im, G, (x, 14))
        pts(im, GD, (x, 15))
    for y in (5, 10):
        pts(im, G, (0, y), (15, y))
    return im


def optical_ram():
    im = new()
    rect(im, 0, 2, 15, 12, K)
    rect(im, 1, 3, 14, 11, PCB)
    rect(im, 1, 3, 14, 3, PCBL)                     # lit top edge
    for x0 in (2, 6, 10):                           # three photonic memory dies
        rect(im, x0, 4, x0 + 3, 8, K)
        rect(im, x0 + 1, 5, x0 + 2, 7, D)
        pts(im, C, (x0 + 1, 6), (x0 + 2, 6))
        pts(im, CL, (x0 + 1, 5))
    pts(im, CD, (14, 5), (14, 7))                   # bus trace to the edge
    rect(im, 1, 9, 14, 9, PCBL)                     # trace rail
    pts(im, C, (3, 9), (7, 9), (11, 9))
    pts(im, M, (1, 11), (14, 11))
    for x in range(1, 15):                          # gold finger edge connector
        pts(im, G if x % 2 else GL, (x, 12))
        pts(im, GD, (x, 13)) if x % 2 else None
    pts(im, K, (7, 13), (8, 13))                    # keying notch
    pts(im, (0, 0, 0, 0), (7, 13), (8, 13))
    pts(im, K, (7, 12), (8, 12))
    # ejector latch ears
    rect(im, 0, 3, 0, 11, L)
    return im


def nano_particles():
    im = new()
    # a drifting cloud of glowing grains, some clumped: (x, y, colour, bright pixel)
    grains = [(3, 3, C), (7, 2, V), (11, 4, C), (5, 6, G), (9, 7, C), (13, 8, V), (2, 9, C),
              (6, 11, V), (10, 12, G), (12, 11, C), (4, 13, C), (8, 9, CL)]
    for x, y, c in grains:
        pts(im, c, (x, y))
        pts(im, K, (x + 1, y + 1)) if (x + y) % 3 == 0 else None
    # two denser clumps
    for cx, cy in ((7, 5), (10, 10)):
        rect(im, cx, cy, cx + 1, cy + 1, CD)
        pts(im, CL, (cx, cy))
        pts(im, C, (cx + 1, cy))
        pts(im, K, (cx + 1, cy + 1))
    pts(im, H, (2, 6), (13, 3), (5, 9), (11, 13))
    return im


def optical_fiber_cable():
    im = new()
    # a diagonal fibre, dark jacket round a glowing core, amber ferrule at each end
    for i in range(3, 13):
        x, y = i, 15 - i
        pts(im, K, (x - 1, y), (x + 1, y), (x, y - 1), (x, y + 1))
    for i in range(3, 13):
        x, y = i, 15 - i
        pts(im, M, (x, y))
        pts(im, C if i % 3 else CL, (x, y))
    for i in range(4, 12):
        x, y = i, 15 - i
        pts(im, D, (x - 1, y), (x, y + 1)) if i % 2 else None
    for (x0, y0, c) in ((1, 13, G), (12, 2, G)):   # ferrules
        rect(im, x0, y0, x0 + 2, y0 + 2, c)
        pts(im, GL, (x0, y0 + 2) if y0 > 8 else (x0, y0))
        pts(im, GD, (x0 + 2, y0) if y0 > 8 else (x0 + 2, y0 + 2))
        pts(im, K, (x0 - 1, y0 + 1), (x0 + 3, y0 + 1)) if 0 < x0 < 13 else None
    pts(im, CL, (0, 14), (15, 1))
    return im


def fiber_interconnects():
    im = new()
    # steel MPO block at the bottom, a fan of four glowing fibres rising out of it
    rect(im, 3, 8, 12, 13, K)
    rect(im, 4, 9, 11, 12, M)
    rect(im, 4, 9, 11, 9, L)
    rect(im, 4, 9, 4, 12, L)
    pts(im, H, (4, 9), (5, 9))
    for x in (5, 6, 9, 10):                         # fibre ports
        pts(im, K, (x, 11))
        pts(im, C, (x, 10))
    for x in (4, 7, 10):                            # amber guide pins
        pts(im, G, (x, 14))
        pts(im, GD, (x, 15))
    rect(im, 3, 13, 12, 13, D)
    for x, top in ((4, 2), (6, 0), (9, 0), (11, 2)):  # fibres fanning out
        for y in range(top, 8):
            pts(im, C if (y + x) % 3 else CL, (x, y))
    pts(im, CL, (4, 2), (6, 0), (9, 0), (11, 2))
    for x in (4, 6, 9, 11):
        pts(im, CD, (x, 7))
    return im


ITEMS = {'optical_cpu': optical_cpu, 'optical_ram': optical_ram, 'nano_particles': nano_particles,
         'optical_fiber_cable': optical_fiber_cable, 'fiber_interconnects': fiber_interconnects}


def main():
    sheet = Image.new('RGBA', (len(ITEMS) * 16 * 10 + 20, 16 * 10 + 20), (110, 110, 110, 255))
    for i, (name, fn) in enumerate(ITEMS.items()):
        im = fn()
        im.save(os.path.join(TEX, name + '.png'))
        big = im.resize((160, 160), Image.NEAREST)
        sheet.paste(big, (10 + i * 160, 10), big)
        print('wrote', name)
    if len(sys.argv) > 1:
        sheet.save(sys.argv[1])


if __name__ == '__main__':
    main()
