#!/usr/bin/env python3
"""The Industrial Cutting Machine's two textures: the Cutting Factory Frame's connected sheet and the controller's face.

Run from the repository root (needs Pillow):  python3 tools/textures/industrial_cutter.py [--preview <dir>]

  casing   cutter_casing (GTNH's CUTTING_FACTORY_FRAME, CREDITS-textures.md) gets a connected sheet: the frame stays
           only at the edge of a group of blocks, the plating runs on across them (LDLib's .mcmeta + `_ctm` sheet, the
           format of space_elevator.ctm_sheet). The Stable Titanium Machine Casing (kubejs's GTNH sprite, which
           overwrote GT's own sprite and its connection) gets the same: its sheet is rebuilt from that sprite, the
           .mcmeta points at it again. Its edge gradient would stutter block to block, so the sheet's frame is flat
           (the mean of the sprite's edge row / column) while the lone block keeps GTNH's gradient.

  face     the controller: a viewport (the saw blade and the readout) over a deck of two cells and the lamp, split by
           a hazard stripe. Idle: the blade still, a red readout, a dim lamp. Working (8 frames): the blade turns,
           sparks fly off the cut, the readout fills green, the lamp lights. Paused: amber, blinking slowly. The lit
           parts are written to the emissive layers too.
"""
import json
import math
import os
import random
import sys

from PIL import Image

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import space_elevator as se  # noqa: E402

ROOT = se.ROOT
AF9_BLOCK = os.path.join(ROOT, 'af9-core/src/main/resources/assets/af9/textures/block')
KJS_CASING = os.path.join(ROOT, 'kubejs/assets/gtceu/textures/block/casings/solid')
KJS_FACE = os.path.join(ROOT, 'kubejs/assets/gtceu/textures/block/multiblock/industrial_cutter')

ACTIVE_FRAMES = 8
IDLE_FRAMES = 4
PAUSED_FRAMES = 4


def clamp(v):
    return max(0, min(255, round(v)))


def shade(c, k):
    return tuple(clamp(v * k) for v in c[:3]) + (255,)


def dump(path, obj):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, 'w', newline='\n') as f:
        f.write(json.dumps(obj, indent=2) + '\n')


def strip(frames):
    w, h = frames[0].size
    im = Image.new('RGBA', (w, h * len(frames)))
    for i, fr in enumerate(frames):
        im.paste(fr, (0, i * h))
    return im


# ---- the casings ----------------------------------------------------------------------------------------------------
def cutter_inside(color):
    """Where the frames meet: the block's own inset (the bevelled square, rows 3..13) with flat plating in the band
    the frame took up, so the wall is one field of inset panels."""
    def inside(sprite, x, y):
        if 3 <= x <= 12 and 3 <= y <= 12:
            return sprite.getpixel((x, y))
        return color[:3] + (255,)
    return inside


RIVETS = ({(a, 2) for a in (2, 5, 10, 13)} | {(a, 13) for a in (2, 5, 10, 13)} |
          {(2, b) for b in (5, 10)} | {(13, b) for b in (5, 10)})


def stable_inside(sprite, palette, vlo, vhi):
    """Where the frames meet: the inset's own colour ramp run as a diagonal sheen that goes on through the blocks
    (one band every 16 px, like the Orbital Deck Casing's), with the rivet ring of every block kept."""
    def inside(_, x, y):
        t = 1 - abs((x + y) % 16 - 7.5) / 7.5
        c = palette(vlo + (vhi - vlo) * t)
        if (x % 16, y % 16) in RIVETS:
            return shade(c, 0.82)
        return c
    return inside


def stable_sheet(sprite, inside, edge):
    """The 32 x 32 sheet (space_elevator.ctm_sheet's layout), but the frame cells take flat edge colours: the sprite's
    edges are a gradient along the block, which would restart at every block of a wall."""
    sheet = Image.new('RGBA', (32, 32), (0, 0, 0, 0))

    def pixel(r, c, px, py, h, v, d):
        x, y = c * 8 + px, r * 8 + py
        dx, dy = (x if c == 0 else 15 - x), (y if r == 0 else 15 - y)
        if not v and dy < 2:
            return edge[dy][0]
        if not h and dx < 2:
            return edge[dx][1]
        if h and v and not d and dx < 2 and dy < 2:          # the inside corner where the diagonal block is missing
            return edge[min(dy, 1)][0]
        return inside(sprite, x, y)

    for rr in range(4):
        for cc in range(4):
            h, v = rr >= 2, cc >= 2
            d = not (h and v)
            if rr < 2 and cc < 2:
                h = v = d = True
            for py in range(8):
                for px in range(8):
                    sheet.putpixel((cc * 8 + px, rr * 8 + py), pixel(rr % 2, cc % 2, px, py, h, v, d))
    return sheet


def write_casing():
    """cutter_casing + _ctm + .mcmeta (af9) and machine_casing_stable_titanium + _ctm + .mcmeta (kubejs/gtceu)."""
    out = {}
    cutter = Image.open(os.path.join(AF9_BLOCK, 'cutter_casing.png')).convert('RGBA')
    sheet = se.ctm_sheet(cutter, 3, 3, cutter_inside(cutter.getpixel((8, 8))))
    sheet.save(os.path.join(AF9_BLOCK, 'cutter_casing_ctm.png'))
    dump(os.path.join(AF9_BLOCK, 'cutter_casing.png.mcmeta'),
         {'ldlib': {'connection': 'af9:block/cutter_casing_ctm'}})
    out['cutter'] = (cutter, sheet)

    stable = Image.open(os.path.join(KJS_CASING, 'machine_casing_stable_titanium.png')).convert('RGBA')
    ramp = sorted((sum(stable.getpixel((x, y))[:3]) / 3, stable.getpixel((x, y)))
                  for y in range(16) for x in range(16)
                  if min(x, y, 15 - x, 15 - y) >= 2 and (x, y) not in RIVETS)

    def palette(v):
        if v <= ramp[0][0]:
            return ramp[0][1]
        if v >= ramp[-1][0]:
            return ramp[-1][1]
        for (v0, c0), (v1, c1) in zip(ramp, ramp[1:]):
            if v <= v1:
                t = (v - v0) / max(v1 - v0, 1e-6)
                return tuple(clamp(a + (b - a) * t) for a, b in zip(c0[:3], c1[:3])) + (255,)
        return ramp[-1][1]

    mid = (ramp[0][0] + ramp[-1][0]) / 2
    edge = []
    for i in range(2):
        row = [stable.getpixel((x, i)) for x in range(16)]
        col = [stable.getpixel((i, y)) for y in range(16)]
        edge.append((tuple(clamp(sum(p[k] for p in row) / 16) for k in range(3)) + (255,),
                     tuple(clamp(sum(p[k] for p in col) / 16) for k in range(3)) + (255,)))
    inside = stable_inside(stable, palette, mid - 21, mid + 21)
    sheet = stable_sheet(stable, inside, edge)
    sheet.save(os.path.join(KJS_CASING, 'machine_casing_stable_titanium_ctm.png'))
    dump(os.path.join(KJS_CASING, 'machine_casing_stable_titanium.png.mcmeta'),
         {'ldlib': {'connection': 'gtceu:block/casings/solid/machine_casing_stable_titanium_ctm'}})
    out['stable'] = (stable, sheet)
    return out


# ---- the face -------------------------------------------------------------------------------------------------------
OUTLINE = (18, 20, 25)
PLATE_HI = (70, 76, 88)
PLATE = (50, 55, 66)
PLATE_LO = (33, 37, 45)
GLASS = (9, 12, 17)
SCREEN = (7, 9, 12)
STEEL_HI = (198, 208, 224)
STEEL = (138, 148, 164)
STEEL_LO = (82, 90, 104)
BLADE_HI = (214, 224, 238)
CAP = (156, 118, 54)
CAP_HI = (206, 164, 84)
CAP_LO = (92, 68, 30)
FLUID = (36, 134, 128)
FLUID_D = (22, 92, 92)
FLUID_TOP = (96, 232, 66)
HAZ_Y = (230, 190, 34)
HAZ_D = (26, 27, 32)
RED = (255, 84, 66)
AMBER = (255, 196, 84)
GREEN = (96, 255, 148)
LAMP = (240, 243, 246)
LAMP_CORE = (255, 226, 108)
SPARK = (255, 246, 214)


def rect(im, x0, y0, x1, y1, c):
    for y in range(y0, y1 + 1):
        for x in range(x0, x1 + 1):
            im.putpixel((x, y), c + (255,) if len(c) == 3 else c)


def dot(im, x, y, c):
    if 0 <= x < 16 and 0 <= y < 16:
        im.putpixel((x, y), c + (255,) if len(c) == 3 else c)


def disc(im, cx, cy, r, colour):
    """A filled disc: light from the upper left, a dark rim at the lower right."""
    for y in range(16):
        for x in range(16):
            dx, dy = x + 0.5 - cx, y + 0.5 - cy
            d = math.hypot(dx, dy)
            if d <= r:
                k = 1.12 - 0.16 * (dx + dy + r) / (2 * r)
                dot(im, x, y, shade(colour, k))
            elif d <= r + 0.65:
                dot(im, x, y, shade(colour, 0.55))


def blade(im, em, frame, state):
    """The saw in the viewport: a disc with eight teeth, turning while the machine works; sparks off the cut."""
    cx, cy, r = 5.5, 5.0, 2.1
    disc(im, cx, cy, r, (150, 161, 178))
    dot(im, 5, 4, STEEL_HI)
    dot(im, 5, 5, STEEL)
    dot(im, 6, 5, STEEL_LO)
    dot(im, 5, 6, STEEL_LO)
    if state != 'active':
        phase = 0.0 if state == 'idle' else 0.6
    else:
        phase = frame * (math.tau / 8)
    for k in range(8):
        a = phase + k * (math.tau / 8)
        x, y = cx + (r + 0.55) * math.cos(a) - 0.5, cy + (r + 0.55) * math.sin(a) - 0.5
        tooth = BLADE_HI if state == 'active' else STEEL
        dot(im, round(x), round(y), tooth)
        if state == 'active':
            dot(em, round(x), round(y), shade(tooth, 1.0))
    if state == 'active':                                   # sparks at the cut, flying down the kerf
        rnd = random.Random(frame * 17 + 3)
        for i in range(3):
            sx = 7 + rnd.randrange(0, 4)
            sy = 6 + rnd.randrange(0, 2) + (i == 0)
            c = SPARK if i else (255, 255, 255)
            dot(im, sx, sy, c)
            dot(em, sx, sy, c)
        dot(im, 7, 7, SPARK)
        dot(em, 7, 7, SPARK)


def readout(im, em, frame, state):
    """The little screen beside the blade: bars that stand by in red, fill green while the machine cuts, and hold
    amber while it is paused."""
    rect(im, 9, 3, 13, 7, OUTLINE)
    rect(im, 10, 4, 12, 6, SCREEN)
    if state == 'idle':
        lit = [(0, 1), (0, 0), (1, 0)]                       # a dim 'L' in the corner, like a standby mark
        colour, dim = RED, shade(RED, 0.35)
        blink = True
    elif state == 'paused':
        lit = [(1, 0), (1, 1), (1, 2)]
        colour, dim = AMBER, shade(AMBER, 0.35)
        blink = frame % 4 < 2
    else:
        n = (frame + 1) % 5
        lit = [(b, a) for b in range(3) for a in range(3) if (a * 3 + b) % 5 < n]
        colour, dim = GREEN, shade(GREEN, 0.35)
        blink = True
    for b in range(3):
        for a in range(3):
            c = colour if (b, a) in lit else dim
            if not blink and (b, a) in lit:
                c = dim
            dot(im, 10 + a, 4 + b, c)
            if (b, a) in lit and (blink or state != 'paused'):
                dot(em, 10 + a, 4 + b, c)


def deck(im, em, frame, state):
    """The deck: two cells of coolant with their brass caps, and the lamp."""
    rect(im, 1, 10, 7, 10, CAP)
    dot(im, 1, 10, CAP_LO)
    dot(im, 7, 10, CAP_LO)
    dot(im, 4, 10, CAP_LO)
    dot(im, 2, 10, CAP_HI)
    for x0 in (2, 5):                                        # the two cells
        rect(im, x0, 11, x0 + 1, 14, FLUID)
        for y in range(11, 15):
            for x in range(x0, x0 + 2):
                if (x + y) % 2:
                    dot(im, x, y, FLUID_D)
        top = 11 if state != 'active' or frame % 2 == 0 else 12
        rect(im, x0, top, x0 + 1, top, FLUID_TOP)
        if state == 'active' and frame % 4 == 0:             # a bubble rising
            dot(im, x0 + (frame // 4) % 2, 14, shade(FLUID_TOP, 1.1))
    for x, c in ((1, STEEL), (4, STEEL_LO), (7, STEEL_LO)):
        rect(im, x, 11, x, 14, c)
    dot(im, 1, 11, STEEL_HI)
    dot(im, 7, 11, STEEL_HI)

    rect(im, 9, 10, 14, 14, OUTLINE)                         # the lamp box
    rect(im, 10, 11, 13, 13, (12, 13, 16))
    lamp = {'idle': ((168, 170, 174), (196, 168, 104)),
            'active': (LAMP, LAMP_CORE),
            'paused': ((172, 150, 110), (214, 176, 96))}[state]
    if state == 'paused' and frame % 4 >= 2:
        lamp = (shade(lamp[0], 0.7), shade(lamp[1], 0.7))
    disc(im, 11.5, 12.0, 1.7, lamp[0])
    dot(im, 11, 12, lamp[1])
    dot(im, 12, 12, lamp[1])
    for x in range(10, 14):
        dot(em, x, 12, shade(lamp[0], 1.0))
    for x in (11, 12):
        dot(em, x, 12, lamp[1])


def face_base(state, frame):
    im = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
    em = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
    rect(im, 1, 1, 14, 8, OUTLINE)                           # the viewport
    rect(im, 2, 2, 13, 7, GLASS)
    for x in range(2, 14):
        dot(im, x, 2, PLATE_HI)
        dot(im, x, 7, PLATE_LO)
    for y in range(2, 8):
        dot(im, 2, y, PLATE_HI)
        dot(im, 13, y, PLATE_LO)
    dot(im, 3, 3, GLASS)
    for x in range(1, 15):                                   # the hazard stripe
        dot(im, x, 9, HAZ_Y if (x // 2) % 2 == 0 else HAZ_D)
    blade(im, em, frame, state)
    readout(im, em, frame, state)
    deck(im, em, frame, state)
    return im, em


def write_face():
    states = {'overlay_front': ('idle', IDLE_FRAMES, 6),
              'overlay_front_paused': ('paused', PAUSED_FRAMES, 10),
              'overlay_front_active': ('active', ACTIVE_FRAMES, 3)}
    os.makedirs(KJS_FACE, exist_ok=True)
    out = {}
    for name, (state, frames, frametime) in states.items():
        base, emissive = [], []
        for f in range(frames):
            b, e = face_base(state, f)
            base.append(b)
            emissive.append(e)
        for suffix, imgs in (('', base), ('_emissive', emissive)):
            path = os.path.join(KJS_FACE, name + suffix + '.png')
            strip(imgs).save(path)
            dump(path + '.mcmeta', {'animation': {'frametime': frametime}})
        out[state] = (base, emissive)
    return out


# ---- previews -------------------------------------------------------------------------------------------------------
def wall(sprite, sheet, w, h, face=None):
    im = se.tile(sprite, sheet, w, h)
    if face is not None:
        im.alpha_composite(face, (im.width // 2 - 8, im.height // 2 - 8))
    return im


def preview(dest, casings, faces):
    os.makedirs(dest, exist_ok=True)
    z = 6
    img = Image.new('RGBA', (9 * 16 * z + 16, 8 * 16 * z + 140), (36, 38, 44, 255))
    from PIL import ImageDraw
    d = ImageDraw.Draw(img)
    cutter, stable = casings['cutter'], casings['stable']
    y = 4
    d.text((4, y), 'cutter casing: lone block / joined, with the controller face', fill=(255, 255, 255))
    y += 14
    lone = cutter[0].resize((16 * z, 16 * z), Image.NEAREST)
    img.alpha_composite(lone, (4, y))
    active_face = faces['active'][0][2]
    joined = wall(cutter[0], cutter[1], 8, 4, active_face).resize((8 * 16 * z, 4 * 16 * z), Image.NEAREST)
    img.alpha_composite(joined, (4 + 16 * z + 8, y))
    y += 4 * 16 * z + 12
    d.text((4, y), 'stable titanium casing: lone block / joined', fill=(255, 255, 255))
    y += 14
    lone = stable[0].resize((16 * z, 16 * z), Image.NEAREST)
    img.alpha_composite(lone, (4, y))
    joined = wall(stable[0], stable[1], 8, 4).resize((8 * 16 * z, 4 * 16 * z), Image.NEAREST)
    img.alpha_composite(joined, (4 + 16 * z + 8, y))
    y += 4 * 16 * z + 14
    d.text((4, y), 'the face: idle / paused / active, and the emissive layers', fill=(255, 255, 255))
    y += 14
    x = 4
    for state in ('idle', 'paused', 'active'):
        for f in range(len(faces[state][0])):
            cell = Image.new('RGBA', (16, 16), (120, 106, 160, 255))
            cell.alpha_composite(faces[state][0][f])
            img.alpha_composite(cell.resize((16 * 3, 16 * 3), Image.NEAREST), (x, y))
            glow = Image.new('RGBA', (16, 16), (0, 0, 0, 255))
            glow.alpha_composite(faces[state][1][f])
            img.alpha_composite(glow.resize((16 * 3, 16 * 3), Image.NEAREST), (x, y + 16 * 3 + 4))
            x += 16 * 3 + 4
        x += 8
    img.save(os.path.join(dest, 'industrial_cutter.png'))
    # the working wall, animated
    frames = []
    for f in range(ACTIVE_FRAMES):
        view = wall(cutter[0], cutter[1], 8, 4, faces['active'][0][f])
        frame = Image.new('RGBA', (view.width, view.height), (36, 38, 44, 255))
        frame.alpha_composite(view)
        frames.append(frame.convert('RGB').resize((view.width * 3, view.height * 3), Image.NEAREST))
    frames[0].save(os.path.join(dest, 'industrial_cutter-active.gif'), save_all=True,
                   append_images=frames[1:], duration=90, loop=0)


def main():
    casings = write_casing()
    faces = write_face()
    if '--preview' in sys.argv:
        preview(sys.argv[sys.argv.index('--preview') + 1], casings, faces)


if __name__ == '__main__':
    main()
