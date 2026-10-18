#!/usr/bin/env python3
"""The four casings of the Orbital Array Mk2 that replace GTCEu's: each is the original's sprite (tools/textures/gt_src/,
see CREDITS-textures.md) with its greys re-cut to a cool steel ramp and AF9's teal "orbital" accent (the colour of the
Orbital Light Casing's glow) laid into it.

  orbital_combined_structure_casing   Stress-Proof Casing: same stone corners and cross, a teal core line in the cross;
                                      joins its neighbours (LDLib connected texture, like the original)
  orbital_truss_casing                Sturdy HSS-E Casing, two plates and rivets: dark steel blue, gold rivets
  orbital_girder_casing               the same plates turned on their side: graphite, teal rivets
  orbital_deck_casing                 Inert PTFE Casing: the same inset plate, lit by a teal edge (hatches go on it)

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


# ---- Truss and Girder (HSS-E replacements) -------------------------------------------------------------------------
# rivet pixels of the original, one in each corner of each plate
RIVETS = [(3, 3), (11, 3), (3, 11), (11, 11)]


def plates(stops, rivet):
    base = load('machine_casing_sturdy_hsse')
    out = remap(base, stops)
    for x, y in RIVETS:
        out.putpixel((x, y), rivet)
    return out


TRUSS_STEEL = [(59, (20, 26, 38)), (69, (30, 38, 54)), (84, (52, 64, 88)), (106, (80, 98, 128)), (122, (116, 136, 168))]
GIRDER_GRAPHITE = [(59, (17, 18, 21)), (69, (28, 29, 34)), (84, (46, 48, 56)), (106, (72, 75, 86)), (122, (104, 108, 122))]


def truss():
    return plates(TRUSS_STEEL, (232, 184, 92, 255))


def girder():
    return plates(GIRDER_GRAPHITE, TEAL).transpose(Image.TRANSPOSE)


# ---- Deck casing (PTFE replacement) --------------------------------------------------------------------------------
DECK = [(27, (11, 14, 19)), (34, (17, 21, 29)), (40, (24, 29, 39)), (53, (40, 48, 63)), (59, (49, 58, 76)),
        (65, (58, 68, 88)), (69, (65, 77, 98)), (78, (86, 99, 124))]


def deck():
    out = remap(load('machine_casing_inert_ptfe'), DECK)
    # the inset plate is lit along its top and left edge, its bottom and right edge glows dim
    for i in range(4, 12):
        out.putpixel((4, i), TEAL_DIM)
        out.putpixel((i, 4), TEAL_DIM)
        out.putpixel((11, i), TEAL_FAINT)
        out.putpixel((i, 11), TEAL_FAINT)
    out.putpixel((4, 4), TEAL)
    return out


def save(im, name):
    im.save(os.path.join(OUT, name + '.png'))


def main():
    sprites = {}
    sprites['orbital_combined_structure_casing'] = combined()
    sprites['orbital_truss_casing'] = truss()
    sprites['orbital_girder_casing'] = girder()
    sprites['orbital_deck_casing'] = deck()
    for name, sprite in sprites.items():
        save(sprite, name)
    # only the Combined Structure Casing joins its neighbours (as the Stress-Proof Casing does): a 3 px border, the plate
    # wall inside
    name = 'orbital_combined_structure_casing'
    sheet = se.ctm_sheet(sprites[name], 3, 3, combined_inside)
    save(sheet, name + '_ctm')
    with open(os.path.join(OUT, name + '.png.mcmeta'), 'w', newline='\n') as f:
        json.dump({'ldlib': {'connection': 'af9:block/%s_ctm' % name}}, f, indent=2)
        f.write('\n')
    if '--preview' in sys.argv:
        dest = sys.argv[sys.argv.index('--preview') + 1]
        os.makedirs(dest, exist_ok=True)
        z = 6
        orig = [load('stress_proof_casing'), load('machine_casing_sturdy_hsse'), load('machine_casing_sturdy_hsse'),
                load('machine_casing_inert_ptfe')]
        img = Image.new('RGBA', (4 * (16 * z + 8) + 3 * 16 * z + 8, 2 * 16 * z + 24 + 3 * 16 * z), (36, 38, 44, 255))
        for i, (name, sprite) in enumerate(sprites.items()):
            x0 = i * (16 * z + 8) + 4
            img.paste(orig[i].resize((16 * z, 16 * z), Image.NEAREST), (x0, 4))
            img.paste(sprite.resize((16 * z, 16 * z), Image.NEAREST), (x0, 16 * z + 12))
        tiled = se.tile(sprites[name := 'orbital_combined_structure_casing'], sheet, 3, 3).resize((3 * 16 * z, 3 * 16 * z),
                                                                                              Image.NEAREST)
        img.paste(tiled, (4 * (16 * z + 8) + 4, 4))
        # a 4 x 4 field of each plain casing, as a wall
        for i, n in enumerate(('orbital_truss_casing', 'orbital_girder_casing', 'orbital_deck_casing')):
            wall = Image.new('RGBA', (3 * 16, 3 * 16))
            for a in range(3):
                for b in range(3):
                    wall.paste(sprites[n], (a * 16, b * 16))
            img.paste(wall.resize((3 * 16 * 2, 3 * 16 * 2), Image.NEAREST), (4 * (16 * z + 8) + 4 + i * 100, 3 * 16 * z + 16))
        img.save(os.path.join(dest, 'orbital_mk2_casings.png'))


if __name__ == '__main__':
    main()
