#!/usr/bin/env python3
"""The Electric Implosion Compressor: the Shock Capacitor Casing (connected texture, animated), the machine face of the
single blocks and of the Large Electric Implosion Compressor, and the items of the tier 2 rocket and its insulation.

Run from the repository root (needs Pillow):  python3 tools/textures/electric_implosion.py [--preview <dir>]

  casing   GT's Stable Titanium Machine Casing and its connected sheet (gt_src/), recoloured teal.
  face     the controller and the single blocks: a bezel with a viewport on two electrodes, a lamp strip below it.
           Idle: dark, a red lamp. Active (8 frames): an arc jumps between the electrodes, the chamber flashes, a green
           lamp; the arc and the lamps are also written to the emissive layer.
  items    the tier 2 rocket cone and fin, the rocket insulation tile, the insulation foil and the wool: drawn, not
           copied; the metal forms of GT's materials come from GT's icon sets.

`--preview <dir>` writes contact sheets and animated previews of all of it.
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
ASSETS = os.path.join(ROOT, 'af9-core/src/main/resources/assets/af9')
DATA = os.path.join(ROOT, 'af9-core/src/main/resources/data/af9')
BLOCK = os.path.join(ASSETS, 'textures/block')
ITEM = os.path.join(ASSETS, 'textures/item')
KJS = os.path.join(ROOT, 'kubejs/assets/gtceu/textures/block')
FACES = ('multiblock/large_electric_implosion_compressor', 'machines/electric_implosion_compressor')
FRAMES = 16
FRAMETIME = 2
ARC_FRAMES = 8


def clamp(v):
    return max(0, min(255, round(v)))


def shade(c, k):
    return tuple(clamp(v * k) for v in c[:3]) + (255,)


def mix(a, b, t):
    return tuple(clamp(a[i] + (b[i] - a[i]) * t) for i in range(3)) + (255,)


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


OUTLINE = (22, 25, 32)
BEVEL_HI = (134, 144, 160)
BEVEL_MID = (92, 101, 117)
BEVEL_LO = (58, 64, 78)
COPPER = [(226, 142, 80), (150, 80, 42)]
BOLT = (246, 196, 140)
CYAN_CORE = (214, 250, 255)
CYAN = (64, 196, 255)


def recolour(im, hue=0.5, sat=2.4, val=0.78):
    """GT's Stable Titanium Machine Casing (tools/textures/gt_src, LGPL-3.0, CREDITS-textures.md) turned from violet to teal:
    the tones keep their value, the hue is moved, so the bevels and the connected texture are GT's own."""
    import colorsys
    out = im.convert('RGBA')
    for y in range(out.height):
        for x in range(out.width):
            r, g, b, a = out.getpixel((x, y))
            h, sl, v = colorsys.rgb_to_hsv(r / 255, g / 255, b / 255)
            r, g, b = colorsys.hsv_to_rgb(hue, min(0.85, max(sl, 0.12) * sat), v * val)
            out.putpixel((x, y), (clamp(r * 255), clamp(g * 255), clamp(b * 255), a))
    return out


def write_casing():
    """The Shock Capacitor Casing: GT's Stable Titanium casing and its connected sheet, recoloured teal."""
    name = 'shock_capacitor_casing'
    src = os.path.join(ROOT, 'tools/textures/gt_src')
    sprite = recolour(Image.open(os.path.join(src, 'machine_casing_stable_titanium.png')))
    sheet = recolour(Image.open(os.path.join(src, 'machine_casing_stable_titanium_ctm.png')))
    sprite.save(os.path.join(BLOCK, name + '.png'))
    sheet.save(os.path.join(BLOCK, name + '_ctm.png'))
    dump(os.path.join(BLOCK, name + '.png.mcmeta'), {'ldlib': {'connection': 'af9:block/%s_ctm' % name}})
    for stale in (name + '_ctm.png.mcmeta',):
        path = os.path.join(BLOCK, stale)
        if os.path.exists(path):
            os.remove(path)
    dump(os.path.join(ASSETS, 'blockstates/%s.json' % name), {'variants': {'': {'model': 'af9:block/%s' % name}}})
    dump(os.path.join(ASSETS, 'models/block/%s.json' % name),
         {'parent': 'minecraft:block/cube_all', 'textures': {'all': 'af9:block/%s' % name}})
    dump(os.path.join(ASSETS, 'models/item/%s.json' % name), {'parent': 'af9:block/%s' % name})
    return [sprite] * FRAMES, [sheet] * FRAMES


# ---------------------------------------------------------------------------------------------------------------------
# The machine face
# ---------------------------------------------------------------------------------------------------------------------
WINDOW = (8, 12, 20)
WINDOW_DIM = (4, 7, 13)
FLASH = (18, 42, 62)
METAL = [(168, 178, 194), (122, 132, 148), (84, 92, 108)]
LED_OFF = {'red': (70, 18, 18), 'amber': (74, 54, 14), 'green': (14, 62, 26)}
LED_ON = {'red': (255, 70, 60), 'amber': (255, 190, 50), 'green': (80, 255, 110)}
PANEL = (84, 92, 106)


def rect(im, x0, y0, x1, y1, c):
    for y in range(y0, y1 + 1):
        for x in range(x0, x1 + 1):
            im.putpixel((x, y), c + (255,) if len(c) == 3 else c)


def face_base(led):
    """The face with its window empty: bezel, electrodes, the lamp strip. led: which lamp the base shows lit (None: all dim)."""
    im = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
    # bezel and window
    rect(im, 1, 1, 14, 9, OUTLINE)
    rect(im, 2, 2, 13, 8, BEVEL_MID)
    for x in range(2, 14):
        im.putpixel((x, 2), BEVEL_HI + (255,))
        im.putpixel((x, 8), BEVEL_LO + (255,))
    for y in range(2, 9):
        im.putpixel((2, y), BEVEL_HI + (255,))
        im.putpixel((13, y), BEVEL_LO + (255,))
    rect(im, 3, 3, 12, 7, WINDOW)
    rect(im, 3, 3, 12, 3, WINDOW_DIM)
    # the electrodes: a metal clamp on the wall, a copper rod to a bright tip, a bolt on the clamp
    for side in (0, 1):
        xs = (3, 4, 5) if side == 0 else (12, 11, 10)
        for y, tone in ((4, METAL[0]), (5, METAL[1]), (6, METAL[2])):
            im.putpixel((xs[0], y), tone + (255,))
        im.putpixel((xs[1], 5), COPPER[0] + (255,))
        im.putpixel((xs[2], 5), COPPER[0] + (255,))
        im.putpixel((xs[1], 4), COPPER[1] + (255,))
        im.putpixel((xs[1], 6), COPPER[1] + (255,))
        im.putpixel((xs[0], 5), BOLT + (255,))
    # the lamp strip: three lamps (2 px each) in dark sockets, a vent on the right
    rect(im, 1, 10, 14, 14, OUTLINE)
    rect(im, 2, 11, 13, 13, PANEL)
    for x in range(2, 14):
        im.putpixel((x, 11), shade(PANEL, 1.3))
        im.putpixel((x, 13), shade(PANEL, 0.7))
    for i, name in enumerate(('red', 'amber', 'green')):
        x = 3 + i * 3
        lit = led == name
        c = LED_ON[name] if lit else LED_OFF[name]
        for dx in (0, 1):
            im.putpixel((x + dx, 12), c + (255,))
        im.putpixel((x - 1, 12), OUTLINE + (255,))
        im.putpixel((x + 2, 12), OUTLINE + (255,))
    for y in (11, 13):                                       # the vent slots
        im.putpixel((12, y), OUTLINE + (255,))
        im.putpixel((13, y), OUTLINE + (255,))
    return im


def put(im, pts, c):
    for x, y in pts:
        im.putpixel((x, y), c + (255,))


def face_idle():
    base = face_base('red')
    # a pilot spark between the tips
    base.putpixel((7, 5), (22, 70, 98, 255))
    base.putpixel((8, 5), (14, 48, 70, 255))
    emissive = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
    emissive.putpixel((3, 12), LED_ON['red'] + (255,))
    emissive.putpixel((4, 12), LED_ON['red'] + (255,))
    emissive.putpixel((7, 5), (22, 70, 98, 255))
    return base, emissive


def face_paused():
    base = face_base('amber')
    emissive = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
    for dx in (0, 1):
        emissive.putpixel((6 + dx, 12), LED_ON['amber'] + (255,))
    return base, emissive


def arc_path(seed):
    """One crackle between the tips (x 5 and 10): a zig-zag of 1 px steps inside rows 4..6, sometimes forked."""
    rnd = random.Random(seed)
    y = 5
    pts = []
    for x in range(6, 10):
        y = max(4, min(6, y + rnd.choice((-1, 0, 1))))
        pts.append((x, y))
    # join the path up, so it never leaves a gap between two columns
    full = []
    prev = (5, 5)
    for p in pts + [(10, 5)]:
        if abs(p[1] - prev[1]) == 2:
            full.append((prev[0] + 0, 5))
        full.append(p)
        prev = p
    fork = []
    if seed % 3 == 0:
        fx, fy = 7, pts[1][1]
        fork.append((fx, 4 if fy >= 5 else 6))
    return [p for p in full if 6 <= p[0] <= 9], fork


def face_active(f):
    base = face_base('green')
    flash = f in (1, 5)
    for y in range(3, 8):
        for x in range(6, 10):
            base.putpixel((x, y), (FLASH if flash else WINDOW) + (255,))
    em = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
    pts, fork = arc_path(f * 5 + 2)
    for layer in (base, em):
        for x, y in pts:
            if (x + f) % 2 == 0:
                for dy in (-1, 1):
                    if 3 < y + dy < 7 and (x, y + dy) not in pts:
                        layer.putpixel((x, y + dy), (26, 78, 128, 255))
        put(layer, fork, CYAN)
        put(layer, pts, CYAN_CORE)
        for x in (4, 5, 10, 11):
            layer.putpixel((x, 5), CYAN_CORE + (255,) if x in (5, 10) else (255, 214, 170, 255))
        layer.putpixel((6, 5), CYAN + (255,)) if (6, 5) not in pts else None
    for dx in (0, 1):
        em.putpixel((9 + dx, 12), LED_ON['green'] + (255,))
    return base, em


def write_face():
    idle, idle_em = face_idle()
    paused, paused_em = face_paused()
    active = [face_active(f) for f in range(ARC_FRAMES)]
    for sub in FACES:
        d = os.path.join(KJS, sub)
        os.makedirs(d, exist_ok=True)
        idle.save(os.path.join(d, 'overlay_front.png'))
        idle_em.save(os.path.join(d, 'overlay_front_emissive.png'))
        paused.save(os.path.join(d, 'overlay_front_paused.png'))
        paused_em.save(os.path.join(d, 'overlay_front_paused_emissive.png'))
        strip([a[0] for a in active]).save(os.path.join(d, 'overlay_front_active.png'))
        strip([a[1] for a in active]).save(os.path.join(d, 'overlay_front_active_emissive.png'))
        anim = {'animation': {'frametime': 2}}
        dump(os.path.join(d, 'overlay_front_active.png.mcmeta'), anim)
        dump(os.path.join(d, 'overlay_front_active_emissive.png.mcmeta'), anim)
    return idle, active


# ---------------------------------------------------------------------------------------------------------------------
# Items
# ---------------------------------------------------------------------------------------------------------------------
ALLOY = [(226, 230, 238), (190, 196, 208), (152, 160, 176), (110, 118, 136), (72, 78, 94)]   # titanium, cool silver-grey
CERAMIC = [(250, 245, 228), (232, 222, 196), (206, 192, 160), (170, 154, 124), (120, 106, 88)]
HEAT = [(255, 150, 90), (222, 96, 60), (160, 54, 40), (96, 30, 30)]


def blank():
    return Image.new('RGBA', (16, 16), (0, 0, 0, 0))


def outline(im, c=(26, 22, 24)):
    """A one px dark outline round the opaque pixels (drawn on a copy, outside only)."""
    out = im.copy()
    for y in range(16):
        for x in range(16):
            if im.getpixel((x, y))[3]:
                continue
            for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                nx, ny = x + dx, y + dy
                if 0 <= nx < 16 and 0 <= ny < 16 and im.getpixel((nx, ny))[3]:
                    out.putpixel((x, y), c + (255,))
                    break
    return out


def cone():
    """The nose cone, tip to the upper right: a heat cap, the alloy body, a ceramic tile ring, a steel collar at the base."""
    im = blank()
    bx, by, tx, ty = 2.6, 13.4, 14.2, 1.8
    ax, ay = tx - bx, ty - by
    length = math.hypot(ax, ay)
    ax, ay = ax / length, ay / length
    nx, ny = -ay, ax                                      # perpendicular, pointing to the lower right
    for y in range(16):
        for x in range(16):
            px, py = x + 0.5 - bx, y + 0.5 - by
            t = (px * ax + py * ay) / length
            d = px * nx + py * ny
            if t < 0 or t > 1.0:
                continue
            radius = 5.2 * (1 - t) ** 0.92
            if abs(d) > radius:
                continue
            u = d / max(radius, 0.01)                     # -1 the lit edge ... +1 the dark edge
            level = 0 if u < -0.6 else 1 if u < -0.15 else 2 if u < 0.35 else 3 if u < 0.75 else 4
            if t > 0.78:
                c = HEAT[min(3, max(0, level - 1))] if level > 0 else (255, 200, 150)
            elif t < 0.09:
                c = (METAL[0], METAL[0], METAL[1], METAL[2], (52, 58, 70))[level]
            elif 0.40 <= t < 0.52:
                c = CERAMIC[min(4, level)]
            elif 0.52 <= t < 0.55 or 0.37 <= t < 0.40:
                c = ALLOY[min(4, level + 2)]
            else:
                c = ALLOY[level]
            im.putpixel((x, y), c + (255,))
    return outline(im)


def fin():
    """A swept fin, root on the left: the leading edge rises to the tip, a ceramic tile stripe and two bolts at the root."""
    im = blank()
    poly = [(1.5, 14.5), (1.5, 6.5), (12.0, 1.5), (14.5, 1.5), (14.5, 4.0), (7.5, 14.5)]

    def inside(x, y):
        n, j = len(poly), False
        for i in range(n):
            x1, y1 = poly[i]
            x2, y2 = poly[(i + 1) % n]
            if (y1 > y) != (y2 > y) and x < (x2 - x1) * (y - y1) / (y2 - y1) + x1:
                j = not j
        return j

    for y in range(16):
        for x in range(16):
            if not inside(x + 0.5, y + 0.5):
                continue
            # distance across the fin from its leading edge (the line (1.5, 8)-(11.5, 1.5))
            lead = ((x + 0.5 - 1.5) * 5.0 + (y + 0.5 - 6.5) * 10.5) / math.hypot(5.0, 10.5)
            level = 0 if lead < 0.9 else 1 if lead < 2.6 else 2 if lead < 5.5 else 3
            c = ALLOY[level]
            if 3.4 <= lead < 5.2:
                c = CERAMIC[min(3, level)]
            if x <= 2:
                c = ALLOY[min(4, level + 1)]
            im.putpixel((x, y), c + (255,))
    for y in (10, 12):
        im.putpixel((3, y), (236, 228, 214, 255))
    return outline(im)


def tile_item():
    """The rocket insulation: a panel of 3 x 3 ceramic tiles, as the shuttle had them, on a dark backing."""
    im = blank()
    rect(im, 1, 1, 14, 14, (44, 38, 38))
    for ty in range(3):
        for tx in range(3):
            x0, y0 = 2 + tx * 4, 2 + ty * 4
            for dy in range(3):
                for dx in range(3):
                    level = 0 if (dx == 0 and dy == 0) else 3 if (dx == 2 and dy == 2) else 1 if (dx == 2 or dy == 2) else 0
                    if dx == 1 and dy == 1:
                        level = 0
                    c = CERAMIC[level if (tx + ty) % 2 == 0 else min(4, level + 1)]
                    im.putpixel((x0 + dx, y0 + dy), c + (255,))
    for x in range(1, 15):
        im.putpixel((x, 1), (86, 76, 72, 255))
        im.putpixel((x, 14), (24, 20, 20, 255))
    return im


def foil():
    """A crinkled sheet of insulation foil: pale ceramic with a gold sheen, folds as light and dark runs."""
    im = blank()
    rnd = random.Random(11)
    for y in range(16):
        for x in range(16):
            u, v = x + y * 0.35, y - x * 0.35
            if not (2.0 <= x <= 13.0 and 2.0 <= y <= 13.0 and (x - 2) + (y - 2) > 2 and (13 - x) + (13 - y) > 2):
                continue
            ridge = math.sin(u * 0.95) + 0.6 * math.sin(v * 1.7 + 1.3)
            level = 0 if ridge > 1.0 else 1 if ridge > 0.2 else 2 if ridge > -0.6 else 3
            c = CERAMIC[level]
            c = mix(c, (255, 220, 140), 0.18 + 0.1 * (rnd.random() - 0.5))
            im.putpixel((x, y), c)
    return outline(im, (60, 50, 40))


def wool():
    """Insulation wool spun to a fine wire: a loose hank of pale fibre wound round a core, a thread to the side."""
    im = blank()
    rnd = random.Random(5)
    cx, cy = 7.5, 8.0
    for y in range(16):
        for x in range(16):
            d = math.hypot((x - cx) / 5.4, (y - cy) / 4.2)
            if d > 1.0 + 0.16 * (rnd.random() - 0.4):
                continue
            wind = math.sin((x * 0.9 + y * 1.3)) + 0.7 * math.sin(x * 0.4 - y * 0.8)
            level = 0 if wind > 0.9 else 1 if wind > 0.1 else 2 if wind > -0.7 else 3
            level = min(4, level + (1 if d > 0.78 else 0))
            im.putpixel((x, y), CERAMIC[level] + (255,))
    for i in range(7):                                       # the fine wire off the hank
        x, y = 12 + i // 2, 6 - i
        if 0 <= x < 16 and 0 <= y < 16:
            im.putpixel((x, y), (214, 168, 96, 255))
    im.putpixel((12, 7), (214, 168, 96, 255))
    return outline(im, (70, 60, 48))


ITEMS = {
    'rocket_cone_tier_2': cone,
    'rocket_fin_tier_2': fin,
    'alumina_silicate_rocket_insulation': tile_item,
    'alumina_silicate_insulation_foil': foil,
    'insulation_wool': wool,
}


def write_items():
    os.makedirs(ITEM, exist_ok=True)
    out = {}
    for name, draw in ITEMS.items():
        im = draw()
        im.save(os.path.join(ITEM, name + '.png'))
        dump(os.path.join(ASSETS, 'models/item/%s.json' % name),
             {'parent': 'minecraft:item/generated', 'textures': {'layer0': 'af9:item/%s' % name}})
        out[name] = im
    return out


# ---------------------------------------------------------------------------------------------------------------------
# Previews
# ---------------------------------------------------------------------------------------------------------------------
HULL = (112, 120, 134, 255)


def up(im, k):
    return im.resize((im.width * k, im.height * k), Image.NEAREST)


def front_view(sprite, sheet, face):
    """The Large Electric Implosion Compressor from the front: 3 x 3 casing blocks joined as the game joins them, the
    controller face in the middle."""
    wall = se.tile(sprite, sheet, 3, 3)
    wall.alpha_composite(face, (16, 16))
    return wall


def preview(outdir, sprites, sheets, idle, active, items):
    os.makedirs(outdir, exist_ok=True)
    bg = (46, 50, 58, 255)
    # one sheet: casing (lone block, 4 x 2 joined, ctm sheet), the front, the face frames, the items
    sheet = Image.new('RGBA', (1180, 860), bg)
    f = 5
    lone = up(sprites[f], 8)
    sheet.alpha_composite(lone, (12, 12))
    joined = up(se.tile(sprites[f], sheets[f], 4, 2), 8)
    sheet.alpha_composite(joined, (160, 12))
    front = up(front_view(sprites[f], sheets[f], active[2][0]), 8)
    sheet.alpha_composite(front, (12, 280))
    x = 420
    for i, (b, e) in enumerate([(idle, None)] + [(a[0], a[1]) for a in active[:7]]):
        cell = Image.new('RGBA', (16, 16), HULL)
        cell.alpha_composite(b)
        sheet.alpha_composite(up(cell, 4), (420 + (i % 4) * 70, 280 + (i // 4) * 70))
    x = 12
    for name, im in items.items():
        sheet.alpha_composite(up(im, 8), (x, 700))
        x += 140
    sheet.save(os.path.join(outdir, 'electric-implosion-preview.png'))
    # animated: the front view, 16 frames
    frames = []
    for f in range(FRAMES):
        view = front_view(sprites[f], sheets[f], active[f % ARC_FRAMES][0])
        frame = Image.new('RGBA', (48 * 8, 48 * 8), bg)
        frame.alpha_composite(up(view, 8))
        frames.append(frame.convert('RGB'))
    frames[0].save(os.path.join(outdir, 'electric-implosion-front-active.gif'), save_all=True,
                   append_images=frames[1:], duration=70, loop=0)
    # the joined casing, big: 6 x 3
    big = [Image.new('RGBA', (96 * 6, 48 * 6), bg) for _ in range(FRAMES)]
    for f in range(FRAMES):
        big[f].alpha_composite(up(se.tile(sprites[f], sheets[f], 6, 3), 6))
    big[0].convert('RGB').save(os.path.join(outdir, 'shock-capacitor-casing-joined.png'))


def main():
    os.makedirs(BLOCK, exist_ok=True)
    sprites, sheets = write_casing()
    idle, active = write_face()
    items = write_items()
    if '--preview' in sys.argv:
        preview(sys.argv[sys.argv.index('--preview') + 1], sprites, sheets, idle, active, items)


if __name__ == '__main__':
    main()
