#!/usr/bin/env python3
"""The four casings of the Orbital Array Mk2 that replace GTCEu's: each is based on the original's sprite (tools/textures/gt_src/,
see CREDITS-textures.md) with its greys re-cut to a cool steel ramp and AF9's teal "orbital" accent (the colour of the
Orbital Light Casing's glow) laid into it.

  orbital_combined_structure_casing   Stress-Proof Casing: same stone corners and cross, a teal core line in the cross;
                                      joins its neighbours (LDLib connected texture, like the original)
  orbital_truss_casing                Sturdy HSS-E Casing, ribbed steel-blue beam plating
  orbital_girder_casing               the same beam on its side: graphite, a teal core in the groove
  orbital_deck_casing                 Inert PTFE Casing: the same inset plate, lit by a teal edge (hatches go on it)
All four join their neighbours (the lone block is the original's shape; where blocks meet the frame goes and the plating
runs on).

Run from the repository root (needs Pillow):  python3 tools/textures/orbital_mk2_casings.py [--preview <dir>]
"""
import json
import os
import sys

from PIL import Image

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import space_elevator as se  # noqa: E402

SRC = os.path.join(se.ROOT, 'tools/textures/gt_src')
OUT = os.path.join(se.ROOT, 'af9-core/src/main/resources/assets/af9/textures/block')

TEAL_HI = (176, 244, 248, 255)
TEAL = (64, 206, 222, 255)
TEAL_DIM = (30, 112, 130, 255)
TEAL_FAINT = (26, 74, 92, 255)


def ramp(stops, v):
    """Colour at v on a piecewise-linear ramp [(value, (r, g, b)), ...]."""
    if v <= stops[0][0]:
        return stops[0][1]
    for (v0, c0), (v1, c1) in zip(stops, stops[1:]):
        if v <= v1:
            t = (v - v0) / float(v1 - v0)
            return tuple(round(a + (b - a) * t) for a, b in zip(c0, c1))
    return stops[-1][1]


def remap(sprite, stops, only=lambda px: True):
    out = sprite.copy()
    for y in range(16):
        for x in range(16):
            px = sprite.getpixel((x, y))
            if only(px):
                out.putpixel((x, y), ramp(stops, px[0]) + (255,))
    return out


def load(name):
    return Image.open(os.path.join(SRC, name + '.png')).convert('RGBA')


# ---- Combined Structure Casing -------------------------------------------------------------------------------------
STEEL = [(22, (13, 16, 22)), (36, (24, 29, 38)), (49, (32, 38, 50)), (76, (62, 71, 86)), (90, (80, 91, 108)),
         (104, (99, 112, 131)), (119, (128, 142, 162))]
NAVY = [(31, (14, 22, 36)), (41, (22, 33, 52))]


def combined():
    base = load('stress_proof_casing')
    out = remap(base, STEEL, lambda px: px[2] - px[0] <= 8)
    # the cross's recess: navy, with a teal core line down its spine and along its arm (centre pixel brightest)
    for y in range(16):
        for x in range(16):
            px = base.getpixel((x, y))
            if px[2] - px[0] > 8:
                out.putpixel((x, y), ramp(NAVY, px[0]) + (255,))
    for y in range(5, 11):
        out.putpixel((7, y), TEAL_FAINT)
    for x in range(4, 12):
        out.putpixel((x, 8), TEAL_FAINT)
    out.putpixel((7, 8), TEAL)
    out.putpixel((8, 8), TEAL_DIM)
    return out


def combined_inside(sprite, x, y):
    """Where the faces join: a navy plate wall in 8 px courses, a teal core line through every second course."""
    cx, cy = x % 8, y % 8
    if cy == 0:
        return (12, 16, 24, 255)                                  # the seam between plates
    if cy == 1:
        return (36, 48, 70, 255)                                  # the lit edge under it
    if cy == 4 and (y // 8) % 2 == 0 and 1 <= cx <= 6:
        return TEAL_FAINT
    shade = 20 + 3 * ((cx * 5 + cy * 3) % 4)                      # a little grain, repeating every 8 px
    return (shade - 6, shade + 6, shade + 22, 255)


# ---- Truss, Girder and Deck: the original's plating, running on across blocks ----------------------------------------
# Where blocks join, the face is the original's own plating repeated without the block's edge (a plate is 8 px wide, so two
# plates and their seam run on through every block); the frame stays only where the group of blocks ends (LDLib connected
# texture, the 32 x 32 sheet space_elevator.ctm_sheet writes). The shading is the GT casing's, recoloured.
# rivet pixels of the original, one in each corner of each plate
RIVETS = [(3, 3), (11, 3), (3, 11), (11, 11)]
TRUSS_STEEL = [(59, (20, 26, 38)), (69, (30, 38, 54)), (84, (52, 64, 88)), (106, (80, 98, 128)), (122, (116, 136, 168))]
GIRDER_GRAPHITE = [(59, (17, 18, 21)), (69, (28, 29, 34)), (84, (46, 48, 56)), (106, (72, 75, 86)), (122, (104, 108, 122))]


def plates(stops, rivet):
    base = load('machine_casing_sturdy_hsse')
    out = remap(base, stops)
    return out


def truss_base():
    return plates(TRUSS_STEEL, (232, 184, 92, 255))


def girder_base():
    return plates(GIRDER_GRAPHITE, TEAL).transpose(Image.TRANSPOSE)


def shade(px, k):
    return tuple(max(0, min(255, round(c * k))) for c in px[:3]) + (255,)


def beam(base, accent, a, c):
    """A beam of plates: `a` runs along it, `c` across (both px of the face). Plates 8 px wide with a dark seam and a lit edge on
    the left, a shaded edge on the right; a soft diagonal sheen (a band every 16 px, as the deck's); a groove down the middle of
    each plate (its core `accent`, when it has one); a panel joint across every 16 px."""
    cx, cy = c % 8, a % 16
    px = shade(base, 0.94 + 0.12 * (1 - abs((a + c) % 16 - 7.5) / 7.5))
    if cx == 0:
        return shade(base, 0.38)
    if cx == 1:
        return shade(px, 1.3)
    if cx == 7:
        return shade(px, 0.7)
    if cx == 3:
        return shade(px, 0.55)
    if cx == 4:
        return accent if accent else shade(px, 1.18)
    if cx == 5 and accent:
        return shade(px, 1.18)
    if cy == 0:
        return shade(px, 0.55)
    if cy == 1:
        return shade(px, 1.2)
    return px


TRUSS_BASE = (62, 78, 106)
GIRDER_BASE = (66, 68, 78)


def plated(sprite, x, y):
    """The Truss: vertical plates, steel blue."""
    return beam(TRUSS_BASE, None, y, x)


def plated_flat(sprite, x, y):
    """The Girder: the same beam on its side, graphite, a teal core in the groove."""
    return beam(GIRDER_BASE, TEAL_FAINT, x, y)


def plating(sprite, inside):
    """The lone block: the same plating with a one-pixel frame, lit top and left, shaded bottom and right."""
    out = Image.new('RGBA', (16, 16))
    for y in range(16):
        for x in range(16):
            px = inside(sprite, x, y)
            if min(x, y) == 0:
                px = shade(px, 1.3)
            elif max(x, y) == 15:
                px = shade(px, 0.6)
            out.putpixel((x, y), px)
    return out


DECK = [(27, (11, 14, 19)), (34, (17, 21, 29)), (40, (24, 29, 39)), (53, (40, 48, 63)), (59, (49, 58, 76)),
        (65, (58, 68, 88)), (69, (65, 77, 98)), (78, (86, 99, 124))]


def deck_base(lit):
    out = remap(load('machine_casing_inert_ptfe'), DECK)
    if lit:                                                   # the frame's inner edge glows: dim, lit a little more top-left
        for i in range(3, 13):
            out.putpixel((3, i), TEAL_DIM)
            out.putpixel((i, 3), TEAL_DIM)
            out.putpixel((12, i), TEAL_FAINT)
            out.putpixel((i, 12), TEAL_FAINT)
        out.putpixel((3, 3), TEAL)
    return out


def deck_inside(sprite, x, y):
    """The inset plate's sheen (the PTFE casing's diagonal, 53 -> 62 on its grey ramp): a diagonal band that runs on
    through the blocks, one band every 16 px."""
    v = 53 + 9 * (1 - abs((x + y) % 16 - 7.5) / 7.5)
    return ramp(DECK, v) + (255,)


def save(im, name):
    im.save(os.path.join(OUT, name + '.png'))


def plated_src(src, fn):
    """The inside function bound to the plates' source sprite (the lone block's sprite is already shaded)."""
    return lambda sprite, x, y: fn(src, x, y)


def connected(name, sprite, inside, border):
    sheet = se.ctm_sheet(sprite, border, border, inside)
    save(sprite, name)
    save(sheet, name + '_ctm')
    with open(os.path.join(OUT, name + '.png.mcmeta'), 'w', newline='\n') as f:
        json.dump({'ldlib': {'connection': 'af9:block/%s_ctm' % name}}, f, indent=2)
        f.write('\n')
    return sheet


def main():
    truss_src, girder_src = truss_base(), girder_base()
    sprites = {'orbital_combined_structure_casing': combined(), 'orbital_truss_casing': plating(truss_src, plated),
               'orbital_girder_casing': plating(girder_src, plated_flat), 'orbital_deck_casing': deck_base(True)}
    insides = {'orbital_combined_structure_casing': (combined_inside, 3), 'orbital_truss_casing': (plated_src(truss_src, plated), 0),
               'orbital_girder_casing': (plated_src(girder_src, plated_flat), 0), 'orbital_deck_casing': (deck_inside, 4)}
    sheets = {n: connected(n, sprites[n], *insides[n]) for n in sprites}
    if '--preview' in sys.argv:
        dest = sys.argv[sys.argv.index('--preview') + 1]
        os.makedirs(dest, exist_ok=True)
        z = 4
        names = list(sprites)
        shapes = ((1, 1), (1, 2), (3, 2), (3, 3))
        w = max(sum(a * 16 * z + 8 for a, _ in shapes) , 1) + 8
        img = Image.new('RGBA', (w, len(names) * (3 * 16 * z + 8) + 8), (36, 38, 44, 255))
        for r, n in enumerate(names):
            x0 = 4
            for a, b in shapes:
                t = se.tile(sprites[n], sheets[n], a, b).resize((a * 16 * z, b * 16 * z), Image.NEAREST)
                img.paste(t, (x0, 4 + r * (3 * 16 * z + 8)))
                x0 += a * 16 * z + 8
        img.save(os.path.join(dest, 'orbital_mk2_casings.png'))


if __name__ == '__main__':
    main()
