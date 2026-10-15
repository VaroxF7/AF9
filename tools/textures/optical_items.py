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
    # dark carrier with chamfered corners, a glowing lens-ring die in the middle, light-guide traces running out to amber pads
    rect(im, 2, 2, 13, 13, K)
    rect(im, 3, 3, 12, 12, D)
    rect(im, 3, 3, 12, 3, M)
    rect(im, 3, 3, 3, 12, M)
    pts(im, (0, 0, 0, 0), (2, 2), (13, 2), (2, 13), (13, 13))
    rect(im, 5, 5, 10, 10, K)                       # die
    rect(im, 6, 6, 9, 9, CD)                        # lens ring
    rect(im, 7, 7, 8, 8, CL)
    pts(im, C, (6, 7), (9, 8), (7, 6), (8, 9))
    for i in (4, 11):                               # light-guide traces, bending toward the pads
        pts(im, C, (i, 6), (i, 9), (6, i), (9, i))
    pts(im, CD, (3, 6), (3, 9), (12, 6), (12, 9), (6, 3), (9, 3), (6, 12), (9, 12))
    for x in (3, 5, 7, 8, 10, 12):                  # amber pads on the top and bottom edge
        pts(im, G, (x, 1), (x, 14))
        pts(im, GD, (x, 0), (x, 15))
    pts(im, GL, (4, 4))                             # pin-1 marker
    return im


def optical_ram():
    im = new()
    # a memory stick standing on its pads: dark board, a column of light-pipe cells glowing cyan, amber contacts
    rect(im, 4, 0, 11, 12, K)
    rect(im, 5, 1, 10, 11, D)
    rect(im, 5, 1, 5, 11, M)
    for y in (2, 5, 8):                             # three photonic memory cells
        rect(im, 6, y, 9, y + 2, K)
        rect(im, 7, y + 1, 8, y + 1, CL)
        pts(im, C, (6, y + 1), (9, y + 1))
        pts(im, CD, (7, y), (8, y), (7, y + 2), (8, y + 2))
    pts(im, V, (10, 1), (10, 11))                   # side clips
    pts(im, L, (4, 6), (11, 6))
    rect(im, 5, 12, 10, 12, M)
    for x in range(5, 11):                          # amber contact edge
        pts(im, G if x % 2 else GL, (x, 13))
        pts(im, GD, (x, 14))
    pts(im, C, (3, 3), (12, 7), (3, 9))             # stray light
    return im


def nano_particles():
    im = new()
    # a dust-pile mound of dark metallic grains (like a nanite pile), cyan-lit specks sparkling in it
    rows = {4: (7, 8), 5: (6, 9), 6: (5, 10), 7: (4, 11), 8: (3, 12), 9: (2, 13), 10: (2, 13), 11: (1, 14), 12: (1, 14), 13: (2, 13)}
    for y, (x0, x1) in rows.items():
        for x in range(x0, x1 + 1):
            c = M
            if y >= 11 or x >= x1 - 1:
                c = D                                # shaded base and right flank
            elif (x * 3 + y * 5) % 4 == 0:
                c = L                                # metallic grain
            if x == x0 or y == 13:
                c = K if (x + y) % 2 else D          # ragged outline
            im.putpixel((x, y), c)
    pts(im, H, (6, 6), (4, 8), (9, 7), (3, 10))      # grain glints
    pts(im, C, (7, 5), (5, 8), (9, 9), (4, 11), (11, 10), (7, 11), (12, 12), (6, 13))
    pts(im, CL, (7, 5), (9, 9), (4, 11))
    pts(im, CD, (6, 8), (10, 9), (8, 12))
    pts(im, CL, (2, 2), (13, 3), (11, 1))            # drifting sparks above the pile
    pts(im, C, (3, 1), (12, 2), (1, 5), (14, 6))
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
    # a piece of circuit board with light traces: green substrate, cyan traces bending at 45 degrees, amber vias at the ends
    rect(im, 0, 1, 15, 14, K)
    rect(im, 1, 2, 14, 13, PCB)
    rect(im, 1, 2, 14, 2, PCBL)
    for x in range(1, 15):
        pts(im, PCBL, (x, 13)) if x % 2 else None
    # three traces from the left pads, one bends up, one down, one straight
    for y in (4, 7, 10):
        pts(im, C, (3, y), (4, y), (5, y))
    pts(im, C, (6, 3), (7, 3), (8, 3), (9, 3), (10, 3))     # upper trace
    pts(im, C, (6, 4), (6, 4))
    pts(im, C, (6, 7), (7, 7), (8, 7), (9, 7), (10, 7))     # straight trace
    pts(im, C, (6, 11), (7, 11), (8, 11), (9, 11), (10, 11))  # lower trace
    pts(im, C, (6, 10), (6, 10))
    pts(im, CL, (4, 4), (4, 7), (4, 10))                    # light running along the traces
    pts(im, CL, (8, 3), (8, 7), (8, 11))
    pts(im, CD, (11, 3), (11, 7), (11, 11), (11, 4), (12, 5), (11, 6), (11, 8), (12, 9), (11, 10))
    pts(im, C, (12, 6), (12, 8))                            # merge into one bus
    pts(im, C, (13, 7))
    for y in (4, 7, 10):                                    # amber vias on the left
        pts(im, G, (2, y))
        pts(im, GL, (2, y - 1) if y > 4 else (2, y))
    pts(im, GD, (13, 4), (13, 10))
    pts(im, G, (13, 3), (13, 11))
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
