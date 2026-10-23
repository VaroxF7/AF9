#!/usr/bin/env python3
"""The Large Wiremill's controller face (kubejs/assets/gtceu/textures/block/multiblock/large_wiremill/).

Run from the repository root (needs Pillow):  python3 tools/textures/large_wiremill.py [--preview <dir>]

The face follows the Industrial Cutting Machine's (tools/textures/industrial_cutter.py): a viewport over a deck, split
by a hazard stripe, with a bar display beside the view. The view shows the machine side-on: the die plate on the
right, the wire running left through the flyer to the spool.
  idle    8 frames: the display breathes red, the spool's winding creeps one line, a glint runs along the wire, the lamp
          is dim. (The saw's idle only breathes; the wire here keeps crawling so the face is never dead.)
  active  12 frames: the flyer's arm turns round the wire, stripes run left along it, the display fills green, the lamp
          lights; lit parts are in the emissive layer too.
  paused  4 frames: amber, blinking slowly, everything still.
"""
import os
import sys

from PIL import Image

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import industrial_cutter as ic  # noqa: E402

ROOT = ic.ROOT
OUT = os.path.join(ROOT, 'kubejs/assets/gtceu/textures/block/multiblock/large_wiremill')

COPPER = (201, 119, 47)
COPPER_D = (167, 95, 36)
COPPER_HI = (236, 162, 84)
DIE = (92, 100, 114)
ORANGE = (210, 105, 30)
SPOOL_Y = 4        # the wire runs along this row inside the viewport (rows 2..7)
IDLE_FRAMES = 8
ACTIVE_FRAMES = 12
PAUSED_FRAMES = 4


def view(im, em, frame, state):
    """The machine in the left of the viewport (x 2..8, y 3..7; the bar display takes x 9..13): spool, flyer, die plate."""
    dot, rect, shade = ic.dot, ic.rect, ic.shade
    wire_y = SPOOL_Y + 1
    # the spool: two flanges and the winding between, light from the upper left
    rect(im, 2, 3, 2, 7, ic.STEEL)
    rect(im, 5, 3, 5, 7, ic.STEEL)
    dot(im, 2, 3, ic.STEEL_HI)
    dot(im, 5, 3, ic.STEEL_HI)
    creep = (frame // 2) % 2 if state == 'idle' else (frame % 2 if state == 'active' else 0)
    for y in range(4, 8):
        for x in (3, 4):
            dot(im, x, y, COPPER if (y + x + creep) % 2 == 0 else COPPER_D)
    for x in (3, 4):
        dot(im, x, 4, COPPER_HI)
    # the flyer: a wheel on the wire with the arm swinging round it while it works
    rect(im, 6, 3, 6, 7, ic.STEEL_LO)
    dot(im, 6, 3, ORANGE)
    dot(im, 6, 7, ORANGE)
    arm_y = (3, 4, 6, 7)[frame % 4] if state == 'active' else 3
    dot(im, 6, arm_y, ic.STEEL_HI)
    # the die plate with its hole, the wire through it
    rect(im, 8, 3, 8, 7, DIE)
    dot(im, 8, 3, ic.STEEL_HI)
    dot(im, 8, wire_y, (10, 12, 16))
    # the wire along its row, stripes running left while it works, one glint crawling in idle
    for x in (7, 8):
        stripe = (x + frame) % 4 < 2 if state == 'active' else True
        c = COPPER_HI if stripe else COPPER_D
        if state == 'idle' and x == 8 - (frame % 4 // 2):
            c = ic.STEEL_HI
        dot(im, x, wire_y, c)
        if state == 'active' and stripe:
            dot(em, x, wire_y, shade(COPPER_HI, 0.8))
    dot(im, 6, wire_y, COPPER_D)
    if state == 'active':
        dot(em, 6, arm_y, ic.STEEL_HI)


def deck(im, em, frame, state):
    """The deck under the window: two reels of fine wire in brass cradles, and the lamp (the saw's)."""
    dot, rect, shade = ic.dot, ic.rect, ic.shade
    rect(im, 1, 10, 14, 14, ic.PLATE_LO)
    rect(im, 1, 10, 7, 10, ic.CAP)
    for x in (1, 4, 7):
        dot(im, x, 10, ic.CAP_LO)
    dot(im, 2, 10, ic.CAP_HI)
    for x0 in (2, 5):                                    # a reel: copper turns on a dark core
        rect(im, x0, 11, x0 + 1, 14, COPPER_D)
        for y in range(11, 15):
            for x in range(x0, x0 + 2):
                if (x + y + (frame // 2 if state == 'active' else 0)) % 2:
                    dot(im, x, y, COPPER)
        dot(im, x0, 11, COPPER_HI)
    for x, c in ((1, ic.STEEL), (4, ic.STEEL_LO), (7, ic.STEEL_LO)):
        rect(im, x, 11, x, 14, c)
    dot(im, 1, 11, ic.STEEL_HI)
    dot(im, 7, 11, ic.STEEL_HI)

    rect(im, 9, 10, 14, 14, ic.OUTLINE)                  # the lamp
    rect(im, 10, 11, 13, 13, (12, 13, 16))
    lamp = {'idle': ((150, 152, 156), (196, 168, 104)),
            'active': (ic.LAMP, ic.LAMP_CORE),
            'paused': ((176, 150, 106), (214, 176, 96))}[state]
    if state == 'idle' and frame % 8 >= 4:              # idle: the lamp breathes with the display
        lamp = (shade(lamp[0], 0.8), shade(lamp[1], 0.8))
    if state == 'paused' and frame % 4 >= 2:
        lamp = (shade(lamp[0], 0.7), shade(lamp[1], 0.7))
    for x, y in ((11, 11), (12, 11), (10, 12), (11, 12), (12, 12), (13, 12), (11, 13), (12, 13)):
        c = lamp[1] if (y == 12 and x in (11, 12)) else (lamp[0] if x + y < 24 else shade(lamp[0], 0.8))
        dot(im, x, y, c)
        dot(em, x, y, c)


def face(state, frame):
    im = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
    em = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
    ic.rect(im, 1, 1, 14, 8, ic.OUTLINE)                 # the viewport
    ic.rect(im, 2, 2, 13, 7, ic.GLASS)
    for x in range(2, 14):
        ic.dot(im, x, 2, ic.GLASS_HI)
    for y in range(2, 8):
        ic.dot(im, 2, y, ic.GLASS_HI)
    for x in range(1, 15):                               # the hazard stripe under the window
        ic.dot(im, x, 9, ic.HAZ_Y if (x // 2) % 2 == 0 else ic.HAZ_D)
    view(im, em, frame, state)
    # the display: the saw's bar readout, in this window's top-right corner is taken by the view, so it overlays the
    # right of the viewport's top rows instead (3 x 4 cells)
    ic.readout(im, em, frame % 4 if state == 'idle' else frame, state)
    deck(im, em, frame, state)
    return im, em


def main():
    states = {'overlay_front': ('idle', IDLE_FRAMES, 6),
              'overlay_front_paused': ('paused', PAUSED_FRAMES, 10),
              'overlay_front_active': ('active', ACTIVE_FRAMES, 3)}
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
        img.save(os.path.join(dest, 'large_wiremill_face.png'))


if __name__ == '__main__':
    main()
