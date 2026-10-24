#!/usr/bin/env python3
"""The Dimensionally Transcendent Plasma Forge's controller face (AF9's own: GTNH's face was a text monitor).

Run from the repository root (needs Pillow):  python3 tools/textures/dtpf_face.py

A bezel of steel and a screen that looks down the beam tube: the beam is the bright dot in the middle and the field's hoops
round it. Idle, the hoops stand dim; working, they run out of the beam towards you, two at a time (8 frames, with an
emissive copy so they glow); paused, the beam is red and the hoops are dark. Writes kubejs/assets/gtceu/textures/block/
multiblock/dtpf/overlay_front{,_paused,_active,_active_emissive}.png and the animation's .mcmeta files.
"""
import json
import math
import os

from PIL import Image

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), '../..')
FACE = os.path.join(ROOT, 'kubejs/assets/gtceu/textures/block/multiblock/dtpf')
FRAMES = 8
C = 7.5  # the screen's centre, in pixels


def bezel(im):
    """The frame: dark edge, lit steel ring, a darker lip round the screen."""
    for y in range(16):
        for x in range(16):
            ring = min(x, y, 15 - x, 15 - y)
            if ring == 0:
                im.putpixel((x, y), (10, 20, 36, 255))
            elif ring == 1:
                lit = x == 1 or y == 1  # top-left catches the light
                im.putpixel((x, y), (66, 118, 170, 255) if lit else (28, 58, 96, 255))
            else:
                continue
    return im


def screen(im, tint):
    """The black-blue glass: a faint gradient, brighter at the middle."""
    for y in range(2, 14):
        for x in range(2, 14):
            d = math.hypot(x - C, y - C)
            v = max(0.0, 1 - d / 8.5)
            im.putpixel((x, y), (int(2 + 2 * v + tint[0]), int(5 + 6 * v + tint[1]), int(12 + 14 * v + tint[2]), 255))
    return im


def add(im, x, y, rgb, k):
    """Light a pixel: the colour times k, added over what is there (k from 0 to 1)."""
    if not (2 <= x <= 13 and 2 <= y <= 13):
        return
    r, g, b, a = im.getpixel((x, y))
    im.putpixel((x, y), (min(255, int(r + rgb[0] * k)), min(255, int(g + rgb[1] * k)), min(255, int(b + rgb[2] * k)),
                         255 if a else 255))


def ring(im, radius, rgb, k=1.0):
    for y in range(2, 14):
        for x in range(2, 14):
            near = 1 - abs(math.hypot(x - C, y - C) - radius) / 0.7
            if near > 0.25:
                # pixel art: a few clean steps rather than a blur
                add(im, x, y, rgb, round(near * 3) / 3 * k)


def beam(im, rgb):
    for x, y in ((7, 7), (8, 7), (7, 8), (8, 8)):
        add(im, x, y, rgb, 1.0)


def corners(im, rgb):
    """The reticle: an L in each corner of the screen."""
    for cx, sx in ((2, 1), (13, -1)):
        for cy, sy in ((2, 1), (13, -1)):
            for i in range(3):
                add(im, cx + sx * i, cy, rgb, 1.0)
                add(im, cx, cy + sy * i, rgb, 1.0)


def idle():
    im = screen(bezel(Image.new('RGBA', (16, 16), (0, 0, 0, 0))), (0, 0, 0))
    ring(im, 5.0, (38, 96, 190), 0.8)
    ring(im, 2.6, (46, 120, 220), 0.9)
    beam(im, (150, 215, 255))
    corners(im, (30, 72, 120))
    return im


def paused():
    im = screen(bezel(Image.new('RGBA', (16, 16), (0, 0, 0, 0))), (0, 0, 0))
    ring(im, 5.0, (40, 50, 70), 0.6)
    beam(im, (210, 60, 50))
    corners(im, (60, 70, 90))
    return im


def active_frame(i, glow_only=False):
    """Frame i of 8: two hoops run out of the beam, the second half a cycle behind."""
    if glow_only:
        im = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
        # the glow layer only holds what is lit: start from black so `add` lights over nothing
        for y in range(2, 14):
            for x in range(2, 14):
                im.putpixel((x, y), (0, 0, 0, 255))
    else:
        im = screen(bezel(Image.new('RGBA', (16, 16), (0, 0, 0, 0))), (0, 0, 0))
    ring(im, 5.3, (60, 140, 255), 0.55 + 0.25 * math.sin(i / FRAMES * 2 * math.pi))
    for phase in (i / FRAMES, (i / FRAMES + 0.5) % 1.0):
        radius = 1.2 + 4.4 * phase
        fade = 1.0 - 0.75 * phase
        ring(im, radius, (130, 225, 255), fade)
    beam(im, (235, 252, 255))
    corners(im, (44, 110, 175))
    if glow_only:
        # transparent where nothing is lit
        for y in range(16):
            for x in range(16):
                r, g, b, a = im.getpixel((x, y))
                im.putpixel((x, y), (r, g, b, 255 if (r or g or b) else 0))
    return im


def strip(frames):
    out = Image.new('RGBA', (16, 16 * len(frames)))
    for n, f in enumerate(frames):
        out.paste(f, (0, 16 * n))
    return out


def save_palette(im, name):
    im.save(os.path.join(FACE, name))


def main():
    os.makedirs(FACE, exist_ok=True)
    save_palette(idle(), 'overlay_front.png')
    save_palette(paused(), 'overlay_front_paused.png')
    save_palette(strip([active_frame(i) for i in range(FRAMES)]), 'overlay_front_active.png')
    save_palette(strip([active_frame(i, True) for i in range(FRAMES)]), 'overlay_front_active_emissive.png')
    for name in ('overlay_front_active.png', 'overlay_front_active_emissive.png'):
        with open(os.path.join(FACE, name + '.mcmeta'), 'w') as f:
            f.write(json.dumps({'animation': {'frametime': 4}}, indent=2) + '\n')


if __name__ == '__main__':
    main()
