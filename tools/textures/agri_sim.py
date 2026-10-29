#!/usr/bin/env python3
"""The Agriculture Simulation Bed's controller face (kubejs/assets/gtceu/textures/block/multiblock/agri_sim/).

Run from the repository root (needs Pillow):  python3 tools/textures/agri_sim.py [--preview <dir>]

The face follows the Industrial Cutting Machine's (tools/textures/industrial_cutter.py): a viewport over a deck, split
by a hazard stripe, with the bar display beside the view and the coolant cells (here the water) under it. The view is
a strip of soil with three seedlings.
  idle    4 frames: three sprouts, the display breathes red, the lamp is dim.
  active  12 frames: the seedlings grow from sprout to flowering crop and start again, the flowers glow (emissive),
          the display fills green, the lamp lights.
  paused  4 frames: amber, blinking slowly, everything still at half height.
"""
import os
import sys

from PIL import Image

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import industrial_cutter as ic  # noqa: E402

ROOT = ic.ROOT
OUT = os.path.join(ROOT, 'kubejs/assets/gtceu/textures/block/multiblock/agri_sim')

SOIL = (92, 62, 38)
SOIL_HI = (128, 90, 56)
SOIL_LO = (58, 38, 24)
STEM = (74, 150, 58)
STEM_HI = (126, 206, 88)
LEAF = (58, 124, 48)
FLOWERS = ((255, 214, 92), (150, 220, 255), (255, 140, 190))
IDLE_FRAMES = 4
ACTIVE_FRAMES = 12
PAUSED_FRAMES = 4
PLANTS = (3, 5, 7)          # the columns of the three seedlings inside the viewport (x 2..8)


def height(state, frame, plant):
    """How many pixels tall a seedling stands (0..4); each plant a little out of step with the others."""
    if state == 'idle':
        return 1
    if state == 'paused':
        return 2
    return 1 + ((frame + plant * 4) % ACTIVE_FRAMES) * 4 // (ACTIVE_FRAMES - 1) % 5


def view(im, em, frame, state):
    dot, rect = ic.dot, ic.rect
    rect(im, 2, 7, 8, 7, SOIL)                                 # the soil strip under the plants
    for x in range(2, 9):
        dot(im, x, 7, SOIL_HI if (x + 1) % 3 == 0 else SOIL)
    rect(im, 2, 6, 8, 6, SOIL_LO)
    for x in range(2, 9):
        if x % 2 == 0:
            dot(im, x, 6, SOIL)
    for k, x in enumerate(PLANTS):
        h = height(state, frame, k)
        for i in range(h):
            dot(im, x, 5 - i, STEM_HI if i == h - 1 else STEM)
        if h >= 3:                                              # leaves from the third pixel on
            dot(im, x - 1, 4, LEAF)
            dot(im, x + 1, 4, LEAF)
        if h >= 4:                                              # a flower at the top
            c = FLOWERS[k]
            dot(im, x, 5 - h, c)
            dot(im, x - 1, 5 - h, ic.shade(c, 0.8))
            dot(im, x + 1, 5 - h, ic.shade(c, 0.8))
            if state == 'active':
                dot(em, x, 5 - h, c)


def face(state, frame):
    im = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
    em = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
    ic.rect(im, 1, 1, 14, 8, ic.OUTLINE)                       # the viewport
    ic.rect(im, 2, 2, 13, 7, ic.GLASS)
    for x in range(2, 14):
        ic.dot(im, x, 2, ic.GLASS_HI)
    for y in range(2, 8):
        ic.dot(im, 2, y, ic.GLASS_HI)
    for x in range(1, 15):                                     # the hazard stripe under the window
        ic.dot(im, x, 9, ic.HAZ_Y if (x // 2) % 2 == 0 else ic.HAZ_D)
    view(im, em, frame, state)
    ic.readout(im, em, frame, state)
    ic.deck(im, em, frame, state)
    return im, em


def main():
    states = {'overlay_front': ('idle', IDLE_FRAMES, 6),
              'overlay_front_paused': ('paused', PAUSED_FRAMES, 10),
              'overlay_front_active': ('active', ACTIVE_FRAMES, 4)}
    os.makedirs(OUT, exist_ok=True)
    sheets = {}
    for name, (state, frames, frametime) in states.items():
        base, emissive = [], []
        for f in range(frames):
            b, e = face(state, f)
            base.append(b)
            emissive.append(e)
        for suffix, imgs in (('', base), ('_emissive', emissive)):
            path = os.path.join(OUT, name + suffix + '.png')
            ic.strip(imgs).save(path)
            ic.dump(path + '.mcmeta', {'animation': {'frametime': frametime}})
        sheets[state] = base
    if '--preview' in sys.argv:
        dest = sys.argv[sys.argv.index('--preview') + 1]
        os.makedirs(dest, exist_ok=True)
        z = 8
        n = sum(len(v) for v in sheets.values())
        img = Image.new('RGBA', (n * (16 * z + 4), 16 * z), (60, 64, 74, 255))
        x = 0
        for state in ('idle', 'paused', 'active'):
            for f in sheets[state]:
                img.alpha_composite(f.resize((16 * z, 16 * z), Image.NEAREST), (x, 0))
                x += 16 * z + 4
        img.save(os.path.join(dest, 'agri_sim_face.png'))


if __name__ == '__main__':
    main()
