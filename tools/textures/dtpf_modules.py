#!/usr/bin/env python3
"""The DTPF's module blocks: the Inconel 738 Module Casing and the four module controllers' faces.

  dtpf_module_casing   a plate of Inconel 738 (the alloy's dark slate, its rust-orange on a few rivets) in the blue grid of the
                       Dimensionally Transcendent Casing, seams lit blue; joins its neighbours (LDLib connected texture).
                       The 12 casings round a module's controller are the module's structure.
  overlay_front*       the controllers' faces (kubejs/assets/gtceu/textures/block/multiblock/dtpf_module_<kind>/): a bezel of
                       the same plate and a small screen with the module's sign; idle dim, paused red, working animated
                       (4 frames, with an emissive copy).
                         parallel     three lanes with a pulse running down each
                         superheater  a heating element: a glowing zigzag over embers
                         sequencer    a ring of four osmium cells that light one after the other
                         forge        a star on its orbit, the ring turning

Block state, models and loot table of the casing are written here; the lang line, the pickaxe tag and the registry entry are
edited by hand (af9-core/.../registry/AF9Blocks.java). Run from the repository root (needs Pillow):
    python3 tools/textures/dtpf_modules.py
"""
import json
import math
import os
import sys

from PIL import Image

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import dtpf_blocks as db  # noqa: E402
import space_elevator as se  # noqa: E402

ROOT = se.ROOT
FACES = os.path.join(ROOT, 'kubejs/assets/gtceu/textures/block/multiblock')
KINDS = ('parallel', 'superheater', 'sequencer', 'forge')
FRAMES = 4

SLATE = (58, 63, 74)          # Inconel 738's colour
RUST = (194, 86, 42)          # and its secondary
ICE = (110, 225, 255)
BLUE = (30, 120, 210)
WHITE = (240, 250, 255)


def mix(a, b, t):
    t = max(0.0, min(1.0, t))
    return tuple(round(a[i] + (b[i] - a[i]) * t) for i in range(3))


def casing_sprite():
    """The plate: the dimensional casing's grid in Inconel, the seams a lit blue."""
    src = db.load(os.path.join(db.SRC, 'MACHINE_DIM_TRANS_CASING.png'))
    out = Image.new('RGBA', (16, 16))
    for y in range(16):
        for x in range(16):
            r, g, b, _ = src.getpixel((x, y))
            lum = b / 255.0                                # 0.1 (deep groove) to 0.55 (bright seam)
            grain = 0.5 + 0.5 * math.sin((x * 1.7 + y * 0.45) * 2.3)   # a faint brushed grain along the plate
            base = mix((26, 36, 54), (70, 98, 136), (lum - 0.15) * 1.9 + 0.06 * grain)   # Inconel slate with the hall's blue cast
            seam = max(0.0, (lum - 0.42) * 4.0)            # the bright lines of the grid
            c = mix(base, BLUE, seam * 0.95)
            out.putpixel((x, y), (c[0], c[1], c[2], 255))
    # the outer frame: a lit top and left edge, a shaded bottom and right edge, so the blocks read as plates
    for i in range(16):
        for (x, y, k) in ((i, 0, 0.5), (0, i, 0.5)):
            r, g, b, _ = out.getpixel((x, y))
            c = mix((r, g, b), ICE, k * 0.45)
            out.putpixel((x, y), (c[0], c[1], c[2], 255))
        for (x, y) in ((i, 15), (15, i)):
            r, g, b, _ = out.getpixel((x, y))
            out.putpixel((x, y), (int(r * 0.55), int(g * 0.55), int(b * 0.6), 255))
    # four rivets of the alloy's rust colour in the corners of the inner panel, and a lit node in the middle
    for (x, y) in ((3, 3), (12, 3), (3, 12), (12, 12)):
        out.putpixel((x, y), RUST + (255,))
        c = mix(RUST, (60, 28, 14), 0.6)
        out.putpixel((x + 1, y), c + (255,))
        out.putpixel((x, y + 1), c + (255,))
    for (x, y, k) in ((7, 7, 1.0), (8, 7, 0.8), (7, 8, 0.8), (8, 8, 0.6)):
        c = mix(BLUE, ICE, k)
        out.putpixel((x, y), c + (255,))
    return out


def casing():
    se.OUT = db.BLOCK
    se.connected('dtpf_module_casing', casing_sprite(), 2, 2)
    db.plain_block('dtpf_module_casing')
    db.cube('dtpf_module_casing', 'dtpf_module_casing')


# ---------------------------------------------------------------------------------------------------- the controllers' faces

def bezel(im):
    """The frame: the casing's lit top and left edge, shaded bottom and right, a dark lip round the screen."""
    for y in range(16):
        for x in range(16):
            ring = min(x, y, 15 - x, 15 - y)
            if ring == 0:
                im.putpixel((x, y), (12, 16, 24, 255))
            elif ring == 1:
                lit = x == 1 or y == 1
                im.putpixel((x, y), (88, 110, 134, 255) if lit else (36, 44, 58, 255))
    return im


def screen(tint=(0, 0, 0)):
    im = bezel(Image.new('RGBA', (16, 16), (0, 0, 0, 0)))
    for y in range(2, 14):
        for x in range(2, 14):
            d = math.hypot(x - 7.5, y - 7.5)
            v = max(0.0, 1 - d / 8.5)
            im.putpixel((x, y), (int(3 + 3 * v + tint[0]), int(6 + 6 * v + tint[1]), int(12 + 12 * v + tint[2]), 255))
    return im


def put(im, x, y, rgb, k=1.0):
    if not (2 <= x <= 13 and 2 <= y <= 13):
        return
    r, g, b, _ = im.getpixel((x, y))
    im.putpixel((x, y), (min(255, int(r + rgb[0] * k)), min(255, int(g + rgb[1] * k)), min(255, int(b + rgb[2] * k)), 255))


def sign_parallel(im, f, state):
    col = {'idle': BLUE, 'paused': (140, 30, 30), 'active': ICE}[state]
    for lane, x0 in enumerate((4, 7, 10)):
        for y in range(3, 13):
            put(im, x0, y, col, 0.35)
            put(im, x0 + 1, y, col, 0.35)
        if state == 'active':
            head = (f * 3 + lane * 4) % 12 + 2
            for t in range(3):
                k = 1.0 - 0.3 * t
                put(im, x0, head - t, WHITE if t == 0 else ICE, k)
                put(im, x0 + 1, head - t, WHITE if t == 0 else ICE, k)


def sign_superheater(im, f, state):
    col = {'idle': (150, 70, 30), 'paused': (110, 25, 25), 'active': (255, 150, 60)}[state]
    zig = [(3, 10), (5, 5), (7, 10), (9, 5), (11, 10), (12, 6)]
    for (x0, y0), (x1, y1) in zip(zig, zig[1:]):
        steps = max(abs(x1 - x0), abs(y1 - y0))
        for s in range(steps + 1):
            x = round(x0 + (x1 - x0) * s / steps)
            y = round(y0 + (y1 - y0) * s / steps)
            pulse = 1.0 if state != 'active' else 0.7 + 0.3 * math.sin(f * 1.6 + x)
            put(im, x, y, col, 0.55 * pulse)
            if state == 'active':
                put(im, x, y, WHITE, 0.25 * pulse)
    for x in range(3, 13):
        put(im, x, 12, (110, 40, 20) if state != 'paused' else (60, 20, 20), 0.5 + (0.3 if (x + f) % 3 == 0 and state == 'active' else 0))


def sign_sequencer(im, f, state):
    cells = ((7, 3), (11, 7), (7, 11), (3, 7))
    col = {'idle': BLUE, 'paused': (140, 30, 30), 'active': ICE}[state]
    for i, (cx, cy) in enumerate(cells):
        lit = 1.0 if (state == 'active' and i == f % 4) else 0.3
        for dx in (0, 1):
            for dy in (0, 1):
                put(im, cx + dx - 0 + (0 if dx else 0), cy + dy, col, lit)
        if lit > 0.9:
            put(im, cx, cy, WHITE, 0.5)
    for i in range(16):
        a = math.pi * 2 * i / 16
        put(im, round(7.5 + 4.2 * math.cos(a)), round(7.5 + 4.2 * math.sin(a)), col, 0.12)


def sign_forge(im, f, state):
    col = {'idle': BLUE, 'paused': (140, 30, 30), 'active': ICE}[state]
    for i in range(24):
        a = math.pi * 2 * i / 24
        put(im, round(7.5 + 5.0 * math.cos(a)), round(7.5 + 5.0 * math.sin(a)), col, 0.3)
    for (dx, dy) in ((0, 0), (1, 0), (0, 1), (1, 1)):
        put(im, 7 + dx, 7 + dy, WHITE if state == 'active' else col, 0.8)
    a = (f / FRAMES) * math.pi * 2 if state == 'active' else math.pi / 3
    for k in range(3):
        b = a - k * 0.35
        x, y = round(7.5 + 5.0 * math.cos(b)), round(7.5 + 5.0 * math.sin(b))
        put(im, x, y, WHITE if k == 0 else col, 1.0 - 0.3 * k)
        if k == 0:
            put(im, x + 1, y, col, 0.6)
            put(im, x, y + 1, col, 0.6)


SIGNS = {'parallel': sign_parallel, 'superheater': sign_superheater, 'sequencer': sign_sequencer, 'forge': sign_forge}


def faces():
    for kind in KINDS:
        out = os.path.join(FACES, 'dtpf_module_%s' % kind)
        os.makedirs(out, exist_ok=True)
        sign = SIGNS[kind]
        idle = screen()
        sign(idle, 0, 'idle')
        idle.save(os.path.join(out, 'overlay_front.png'))
        paused = screen((10, 0, 0))
        sign(paused, 0, 'paused')
        paused.save(os.path.join(out, 'overlay_front_paused.png'))
        strip = Image.new('RGBA', (16, 16 * FRAMES))
        glow = Image.new('RGBA', (16, 16 * FRAMES), (0, 0, 0, 0))
        for f in range(FRAMES):
            fr = screen()
            sign(fr, f, 'active')
            strip.paste(fr, (0, 16 * f))
            em = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
            lit = Image.new('RGBA', (16, 16), (0, 0, 0, 255))
            sign(lit, f, 'active')
            for y in range(2, 14):
                for x in range(2, 14):
                    r, g, b, _ = lit.getpixel((x, y))
                    if max(r, g, b) > 70:
                        em.putpixel((x, y), (r, g, b, 255))
            glow.paste(em, (0, 16 * f))
        strip.save(os.path.join(out, 'overlay_front_active.png'))
        glow.save(os.path.join(out, 'overlay_front_active_emissive.png'))
        meta = {'animation': {'frametime': 5}}
        for name in ('overlay_front_active', 'overlay_front_active_emissive'):
            with open(os.path.join(out, name + '.png.mcmeta'), 'w', newline='\n') as fh:
                fh.write(json.dumps(meta, indent=2) + '\n')


if __name__ == '__main__':
    casing()
    faces()
    print('done')
