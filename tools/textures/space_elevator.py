#!/usr/bin/env python3
"""Space Elevator block textures: GTNH Intergalactic's own art, in GregTech CEu Modern's connected-texture format.

Run from the repository root (needs Pillow):  python3 tools/textures/space_elevator.py [--preview <dir>]

Sources (the originals of GTNH's GT5-Unofficial, src/main/resources/assets/gtnhintergalactic/textures/blocks/spaceElevator
and the miner module overlay of assets/gregtech/textures/blocks/iconsets) are in tools/textures/gtnh_space_elevator/.
For every face that should join its neighbours it writes the sprite (the lone block: all four outside corners) and
`<sprite>_ctm.png`, the sheet GT's CTM code (client/model/ctm/TextureConnections, from Chisel's CTM) cuts the quadrants of a
face from: a 4 x 4 grid of 8 px cells, row = quadrant row (top 0 / bottom 1) [+ 2 when the horizontal neighbour is joined],
column = quadrant column [+ 2 when the vertical neighbour is joined]:

    quadrant   all of h, v, diagonal joined   only h joined   only v joined   h and v, not the diagonal
    top-left   (0,0)                          (2,0)           (0,2)           (2,2)
    top-right  (0,1)                          (2,1)           (0,3)           (2,3)
    bot-left   (1,0)                          (3,0)           (1,2)           (3,2)
    bot-right  (1,1)                          (3,1)           (1,3)           (3,3)

(neither joined: the sprite's own quadrant). `--preview` also tiles each face the way the game does, as a check.
"""
import os
import sys

from PIL import Image

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), '../..')
SRC = os.path.join(ROOT, 'tools/textures/gtnh_space_elevator')
OUT = os.path.join(ROOT, 'af9-core/src/main/resources/assets/af9/textures/block')


def load(name):
    return Image.open(os.path.join(SRC, name + '.png')).convert('RGBA')


def middle(sprite, x, y):
    """The face with its borders and corner pieces gone, repeating every 8 px: the sprite's middle 8 x 8."""
    return sprite.getpixel((4 + x % 8, 4 + y % 8))


def same(sprite, x, y):
    """A face that is already seamless: the sprite itself."""
    return sprite.getpixel((x, y))


def black(sprite, x, y):
    """A face that is one flat colour away from its corners: the sprite's centre."""
    return sprite.getpixel((8, 8))


def lane(sprite, x, y):
    """The motors' sides: the chevron lane (6 px wide) repeating every 8 px with a 2 px gap, the rows every 8 px."""
    return sprite.getpixel((5 + x % 8, 4 + y % 8)) if x % 8 < 6 else sprite.getpixel((5, 0))


def ctm_sheet(sprite, tx, ty, inside_of=middle):
    """The 32 x 32 sheet of a sprite. tx / ty: how thick its left+right / top+bottom border is (0: none, the face
    simply continues); inside_of: what fills the face where nothing borders it."""
    sheet = Image.new('RGBA', (32, 32), (0, 0, 0, 0))

    def inside(x, y):
        return inside_of(sprite, x, y)

    def pixel(r, c, px, py, h, v, d):
        x, y = c * 8 + px, r * 8 + py                       # on the 16 x 16 face
        dx, dy = (x if c == 0 else 15 - x), (y if r == 0 else 15 - y)   # distance from the outer edge
        if not v and dy < ty:                               # the border along the edge this quadrant touches vertically
            return sprite.getpixel((4 + x % 8, y))
        if not h and dx < tx:
            return sprite.getpixel((x, 4 + y % 8))
        if h and v and not d and dx < tx and dy < ty:       # the inside corner where the diagonal block is missing
            return sprite.getpixel((4 + x % 8, y))
        return inside(x, y)

    for rr in range(4):
        for cc in range(4):
            # cell (rr, cc): rr >= 2 is "the horizontal neighbour is joined", cc >= 2 "the vertical neighbour is joined";
            # the first 2 x 2 cells are "all joined", the last 2 x 2 "both, but not the diagonal"
            h, v = rr >= 2, cc >= 2
            d = not (h and v)
            if rr < 2 and cc < 2:
                h = v = d = True
            for py in range(8):
                for px in range(8):
                    sheet.putpixel((cc * 8 + px, rr * 8 + py), pixel(rr % 2, cc % 2, px, py, h, v, d))
    return sheet


def tile(sprite, sheet, w, h):
    """w x h blocks of one face, joined as the game joins them (a check of the sheet and of this file's reading of it)."""
    face = Image.new('RGBA', (w * 16, h * 16))
    for bx in range(w):
        for by in range(h):
            for r in range(2):
                for c in range(2):
                    hn = (bx > 0) if c == 0 else (bx < w - 1)
                    vn = (by > 0) if r == 0 else (by < h - 1)
                    dn = (bx + (-1 if c == 0 else 1) in range(w)) and (by + (-1 if r == 0 else 1) in range(h))
                    if not (hn or vn):
                        cell = sprite.crop((c * 8, r * 8, c * 8 + 8, r * 8 + 8))
                    else:
                        if hn and vn and dn:
                            rr, cc = r, c
                        else:
                            rr, cc = r + (2 if hn else 0), c + (2 if vn else 0)
                        cell = sheet.crop((cc * 8, rr * 8, cc * 8 + 8, rr * 8 + 8))
                    face.paste(cell, (bx * 16 + c * 8, by * 16 + r * 8))
    return face


def save(im, name):
    im.save(os.path.join(OUT, name + '.png'))


def connected(name, sprite, tx, ty, inside_of=middle):
    """A sprite that joins its neighbours: the sprite, its sheet and the .mcmeta that ties them together."""
    save(sprite, name)
    sheet = ctm_sheet(sprite, tx, ty, inside_of)
    save(sheet, name + '_ctm')
    with open(os.path.join(OUT, name + '.png.mcmeta'), 'w', newline='\n') as f:
        f.write('{\n    "ldlib": {\n        "connection": "af9:block/%s_ctm"\n    }\n}\n' % name)
    return sprite, sheet


def cable():
    """GTNH's cable decal on a dark core: the cube the shaft is built from."""
    deco = load('Cable')
    face = Image.new('RGBA', (16, 16), (24, 28, 36, 255))
    face.alpha_composite(deco)
    return face


def module(tier):
    """A mining module: GTNH's support-structure plate with the miner module's pickaxe and one pip per tier."""
    plate = load('SupportStructure_Side').copy()
    glyph = load('OVERLAY_SIDE_MINER_MODULE')
    plate.alpha_composite(glyph)
    tint = [(0x4F, 0xC3, 0xF7, 255), (0x66, 0xE0, 0x8A, 255), (0xFF, 0xB3, 0x47, 255)][tier - 1]
    for i in range(tier):
        for dx, dy in ((0, 0), (1, 0), (0, 1), (1, 1)):
            plate.putpixel((5 + i * 3 + dx, 13 + dy - 1), tint)
    return plate


def main():
    os.makedirs(OUT, exist_ok=True)
    previews = {}
    # base casing: the blue plate; a border one pixel wide, rivets only at the corners of the whole face
    previews['base_casing'] = connected('space_elevator_base_casing', load('BaseCasing'), 1, 1)
    # internal structure: the plate on the ends, the circuit band (edge to edge, already seamless) on the sides
    previews['internal_top'] = connected('space_elevator_internal_structure_top', load('InternalStructure'), 1, 1)
    previews['internal_side'] = connected('space_elevator_internal_structure', load('InternalStructure_Side'), 0, 0, same)
    # motors: black top with brackets only at the outside corners; sides with the chevrons running on up the stack
    previews['motor_top'] = connected('space_elevator_motor_top', load('Motor'), 0, 0, black)
    for tier in range(1, 6):
        previews['motor_%d' % tier] = connected('space_elevator_motor_mk%d' % tier, load('MotorT%d_Side' % tier), 5, 0, lane)
    save(cable(), 'space_elevator_cable')
    for tier in range(1, 4):
        save(module(tier), 'space_mining_module_mk%d' % tier)
    save(load('SupportStructure'), 'space_mining_module_top')
    if '--preview' in sys.argv:
        dest = sys.argv[sys.argv.index('--preview') + 1]
        os.makedirs(dest, exist_ok=True)
        names = list(previews)
        img = Image.new('RGBA', (len(names) * 3 * 16 * 4 + 8, 4 * 16 * 4 * 2 + 24), (60, 0, 60, 255))
        for i, n in enumerate(names):
            sprite, sheet = previews[n]
            single = sprite.resize((16 * 4, 16 * 4), Image.NEAREST)
            img.paste(single, (i * 3 * 64 + 4, 4))
            t = tile(sprite, sheet, 3, 4).resize((3 * 16 * 4, 4 * 16 * 4), Image.NEAREST)
            img.paste(t, (i * 3 * 64 + 4, 4 * 16 + 12))
        img.save(os.path.join(dest, 'preview.png'))


if __name__ == '__main__':
    main()
