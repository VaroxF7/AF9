#!/usr/bin/env python3
"""The controller face of the Certus Resonating Manufacturer: a steel plate in the casing's colours with a tall
slot of climbing certus light and three lamps below.

Run from the repository root (needs Pillow):  python3 tools/textures/resonator_face.py [--preview preview.png]

Writes into kubejs/assets/gtceu/textures/block/multiblock/me_resonator:
  overlay_front.png (+ .mcmeta): the machine standing by, the crystal breathing slowly
  overlay_front_paused.png (+ .mcmeta): it waits, dimmed, with an amber lamp
  overlay_front_active.png (+ .mcmeta): it works, the crystal bright and the ring running out
  every one of them has an _emissive layer with the light of the crystal, the ring and the lamps
"""
import json
import math
import os
import sys

from PIL import Image

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), '../..')
OUT = os.path.join(ROOT, 'kubejs/assets/gtceu/textures/block/multiblock/me_resonator')
TAU = 2 * math.pi

RIM = (14, 20, 30)
PLATE = (40, 52, 68)
PLATE_HI = (82, 100, 122)
WINDOW = (14, 16, 22)
WINDOW_HI = (96, 108, 128)
RIVET = (128, 136, 150)
CERTUS = (116, 196, 238)
CERTUS_HI = (222, 246, 255)
CERTUS_LO = (58, 118, 168)
FLUIX = (156, 108, 240)
FLUIX_HI = (232, 196, 255)
SPARK = (255, 108, 92)
LAMP_IDLE = (58, 118, 168)
LAMP_ACTIVE = (86, 226, 132)
LAMP_PAUSED = (226, 176, 84)


def clamp(v):
    return max(0, min(255, round(v)))


def shade(colour, k):
    return tuple(clamp(c * k) for c in colour)


def mix(a, b, t):
    return tuple(clamp(a[i] + (b[i] - a[i]) * t) for i in range(3))


def dot(im, x, y, c):
    if 0 <= x < 16 and 0 <= y < 16:
        im.putpixel((x, y), tuple(c[:3]) + (255,))


def rect(im, x0, y0, x1, y1, c):
    for y in range(y0, y1 + 1):
        for x in range(x0, x1 + 1):
            dot(im, x, y, c)


def face(state, frame, frames):
    """A brutalist plate in the casing's steel: a tall slot (the glass column of the machine, seen from the front)
    with a stack of certus segments in it, and three lamps in a bar below. Working: the light climbs the slot."""
    im = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
    em = Image.new('RGBA', (16, 16), (0, 0, 0, 0))

    rect(im, 0, 0, 15, 15, RIM)
    rect(im, 1, 1, 14, 14, PLATE)
    for x in range(1, 15):
        dot(im, x, 1, PLATE_HI)
    for y in range(1, 15):
        dot(im, 1, y, PLATE_HI)
    # the bolts in the corners
    for x, y in ((2, 2), (13, 2), (2, 13), (13, 13)):
        dot(im, x, y, RIVET)
    # side rails: two dark grooves either side of the slot
    for y in range(4, 12):
        dot(im, 3, y, RIM)
        dot(im, 12, y, RIM)
    # the slot: a deep recess, a bright lip on its lower edge
    rect(im, 6, 2, 9, 11, WINDOW)
    for x in range(5, 11):
        dot(im, x, 12, WINDOW_HI)
    for y in range(2, 12):
        dot(im, 5, y, RIM)
        dot(im, 10, y, RIM)

    if state == 'active':
        lit = lambda i: 1.0 if (i - frame * 10 // frames) % 5 < 2 else 0.35
        colour = CERTUS
    elif state == 'paused':
        lit = lambda i: 0.3 if i == 0 else 0.12
        colour = LAMP_PAUSED
    else:
        breathe = 0.35 + 0.3 * math.sin(TAU * frame / frames)
        lit = lambda i: breathe if i in (0, 1) else 0.12
        colour = CERTUS
    # five segments of two rows each, the lowest (index 4) the first to light
    for i in range(5):
        k = lit(4 - i)   # i counts from the top, the level from the bottom
        y0 = 2 + i * 2
        c = mix(WINDOW, colour, k) if k < 1 else mix(colour, CERTUS_HI, 0.25)
        for y in (y0, y0 + 1):
            for x in (7, 8):
                dot(im, x, y, c)
                dot(em, x, y, c)
        if k >= 1:
            for x in (6, 9):
                dot(im, x, y0, shade(colour, 0.7))
                dot(em, x, y0, shade(colour, 0.7))

    lamp = {'idle': LAMP_IDLE, 'active': LAMP_ACTIVE, 'paused': LAMP_PAUSED}[state]
    rect(im, 4, 13, 11, 13, RIM)
    for i in range(3):
        x = 5 + i * 3
        c = lamp if (i == 0 or (state == 'active' and i == 1)) else shade(lamp, 0.4)
        dot(im, x, 13, c)
        dot(em, x, 13, c)

    return im, em


def strip(frames):
    w, h = frames[0].size
    im = Image.new('RGBA', (w, h * len(frames)))
    for i, fr in enumerate(frames):
        im.paste(fr, (0, i * h))
    return im


def dump(path, data):
    with open(path, 'w', newline='\n') as f:
        json.dump(data, f, indent=2)
        f.write('\n')


def main():
    os.makedirs(OUT, exist_ok=True)
    states = {'overlay_front': ('idle', 12, 6),
              'overlay_front_paused': ('paused', 6, 10),
              'overlay_front_active': ('active', 12, 3)}
    for name, (state, frames, frametime) in states.items():
        base, emissive = [], []
        for f in range(frames):
            b, e = face(state, f, frames)
            base.append(b)
            emissive.append(e)
        for suffix, imgs in (('', base), ('_emissive', emissive)):
            path = os.path.join(OUT, name + suffix + '.png')
            strip(imgs).save(path)
            dump(path + '.mcmeta', {'animation': {'frametime': frametime}})
    if '--preview' in sys.argv:
        preview(sys.argv[sys.argv.index('--preview') + 1])


def preview(dest):
    """A contact sheet: the three states, their frames and their emissive layers."""
    from PIL import ImageDraw
    z = 6
    img = Image.new('RGBA', (16 * z * 14, 16 * z * 8 + 40), (36, 38, 44, 255))
    d = ImageDraw.Draw(img)
    x = 4
    for name in ('overlay_front', 'overlay_front_paused', 'overlay_front_active'):
        base = Image.open(os.path.join(OUT, name + '.png')).convert('RGBA')
        glow = Image.open(os.path.join(OUT, name + '_emissive.png')).convert('RGBA')
        frames = base.height // 16
        d.text((x, 4), name, fill=(230, 232, 238))
        for f in range(min(frames, 12)):
            cell = Image.new('RGBA', (16, 16), (48, 53, 63, 255))
            cell.alpha_composite(base.crop((0, 16 * f, 16, 16 * f + 16)))
            img.alpha_composite(cell.resize((16 * z, 16 * z), Image.NEAREST), (x + f * (16 * z + 3), 16))
            lit = Image.new('RGBA', (16, 16), (0, 0, 0, 255))
            lit.alpha_composite(glow.crop((0, 16 * f, 16, 16 * f + 16)))
            img.alpha_composite(lit.resize((16 * z, 16 * z), Image.NEAREST), (x + f * (16 * z + 3), 16 + 16 * z + 4))
        x += 12 * (16 * z + 3) + 16
    img.save(dest)


if __name__ == '__main__':
    main()
