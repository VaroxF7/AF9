"""
The textures of the Electric Blast Furnace skin (see ebf_model.py). Every one is 16x16, painted with a small palette per
material; 'coil_marker' and the 'glow_*' ones are placeholders the render swaps for the sprite of the coil the player
built with and for the lit, waiting or cold furnace window.
"""
import json
import math
import os
import random

from PIL import Image

# material ramps, dark to light
STEEL = [(22, 22, 23), (35, 35, 36), (52, 51, 51), (72, 70, 68), (98, 95, 90), (132, 127, 120), (172, 166, 156)]
BRASS = [(44, 34, 22), (74, 58, 36), (104, 82, 52), (130, 104, 66), (160, 130, 84), (190, 160, 108)]
COPPER = [(78, 36, 20), (122, 58, 30), (170, 92, 48), (208, 126, 70), (236, 168, 108)]
RED = [(30, 26, 24), (52, 44, 40), (76, 64, 56), (104, 86, 70), (130, 104, 84)]
HAZARD = [(24, 24, 28), (226, 182, 34), (246, 214, 84)]


def new(alpha=255):
    return Image.new('RGBA', (16, 16), (0, 0, 0, 0 if alpha == 0 else 255))


def put(img, x, y, c):
    if 0 <= x < 16 and 0 <= y < 16:
        img.putpixel((x, y), tuple(c) + ((255,) if len(c) == 3 else ()))


def noisy(rnd, c, spread):
    d = rnd.choice((-spread, 0, 0, spread))
    return tuple(max(0, min(255, v + d)) for v in c)


def fill(img, rnd, c, spread=3):
    for y in range(16):
        for x in range(16):
            put(img, x, y, noisy(rnd, c, spread))


def weather(img, rnd, rust=0.05, soot=0.0):
    """Rust specks and a sooty top-to-bottom stain on an opaque iron texture."""
    for y in range(16):
        for x in range(16):
            r, g, b, a = img.getpixel((x, y))
            if a == 0:
                continue
            if rnd.random() < rust:
                r, g, b = min(255, r + 38), g + 6, max(0, b - 14)
            if soot:
                f = 1 - soot * (1 - y / 15.0)
                r, g, b = int(r * f), int(g * f), int(b * f)
            img.putpixel((x, y), (r, g, b, a))


def paint(textures_dir):
    os.makedirs(textures_dir, exist_ok=True)
    rnd = random.Random(7)

    def save(name, img):
        img.save(os.path.join(textures_dir, name + '.png'))

    # ---- plate: riveted dark steel panel with a bevel
    img = new()
    fill(img, rnd, STEEL[2], 3)
    for i in range(16):
        put(img, i, 0, STEEL[4])
        put(img, 0, i, STEEL[4])
        put(img, i, 15, STEEL[0])
        put(img, 15, i, STEEL[0])
    for i in range(1, 15):
        put(img, i, 1, STEEL[3])
        put(img, 1, i, STEEL[3])
        put(img, i, 14, STEEL[1])
        put(img, 14, i, STEEL[1])
    put(img, 0, 0, STEEL[5])
    put(img, 15, 0, STEEL[3])
    put(img, 0, 15, STEEL[3])
    for (x, y) in ((3, 3), (11, 3), (3, 11), (11, 11)):
        put(img, x, y, STEEL[5])
        put(img, x + 1, y, STEEL[4])
        put(img, x, y + 1, STEEL[4])
        put(img, x + 1, y + 1, STEEL[0])
    for (x, y, n) in ((6, 7, 4), (9, 5, 3), (5, 10, 3)):  # scratches
        for k in range(n):
            put(img, x + k, y - k // 2, STEEL[3])
    weather(img, rnd, 0.06, 0.18)
    save('plate', img)

    # ---- post: a column with a weld seam at the cell border, the stripe a column is seen through is columns 4-11
    img = new()
    fill(img, rnd, STEEL[2], 2)
    for y in range(16):
        put(img, 4, y, STEEL[0])
        put(img, 5, y, STEEL[4])
        put(img, 6, y, STEEL[3])
        put(img, 9, y, STEEL[2])
        put(img, 10, y, STEEL[1])
        put(img, 11, y, STEEL[0])
        put(img, 7, y, STEEL[1])
        put(img, 8, y, STEEL[4])
    for x in range(16):
        put(img, x, 0, STEEL[1])
        put(img, x, 15, STEEL[3])
    for y in (4, 11):
        for x in (5, 10):
            put(img, x, y, STEEL[6])
            put(img, x, y + 1, STEEL[0])
    weather(img, rnd, 0.06, 0.18)
    save('post', img)

    # ---- hoop: a banded iron ring, lit from the top, in shadow at the bottom
    img = new()
    rows = [(112, 120, 136), (88, 96, 110), (66, 73, 86), (58, 64, 76), (52, 58, 69), (48, 53, 63), (46, 51, 61),
            (46, 51, 61), (46, 51, 61), (46, 51, 61), (44, 49, 59), (42, 46, 55), (38, 42, 50), (32, 36, 43),
            (26, 28, 34), (18, 20, 25)]
    rows = [(r + 2, g, b - 2) for (r, g, b) in rows]
    for y in range(16):
        for x in range(16):
            put(img, x, y, noisy(rnd, rows[y], 2))
    for x in (2, 7, 12):  # bolt heads, only seen when the band is tall
        put(img, x, 8, STEEL[5])
        put(img, x + 1, 9, STEEL[0])
    weather(img, rnd, 0.06, 0.18)
    save('hoop', img)

    # ---- pipe: brass, a seam ring at the cell border (the texture is read through the middle columns)
    img = new()
    for y in range(16):
        for x in range(16):
            base = (96, 72, 54)
            if 6 <= x <= 8:
                base = (112, 86, 62)
            if x >= 11:
                base = BRASS[2]
            put(img, x, y, noisy(rnd, base, 4))
    for x in range(16):
        put(img, x, 0, BRASS[1])
        put(img, x, 1, BRASS[4])
        put(img, x, 15, BRASS[2])
    save('pipe', img)
    save('pipe_h', img.transpose(Image.TRANSPOSE))

    # ---- brass: a fitting plate
    img = new()
    fill(img, rnd, BRASS[3], 4)
    for i in range(16):
        put(img, i, 0, BRASS[5])
        put(img, 0, i, BRASS[4])
        put(img, i, 15, BRASS[0])
        put(img, 15, i, BRASS[1])
    for i in range(1, 15):
        put(img, i, 14, BRASS[2])
        put(img, 14, i, BRASS[2])
    save('brass', img)

    # ---- vent: dark slats
    img = new()
    for y in range(16):
        for x in range(16):
            m = y % 4
            c = [STEEL[0], STEEL[4], STEEL[2], STEEL[1]][m]
            if x in (0, 15):
                c = STEEL[0]
            put(img, x, y, noisy(rnd, c, 2))
    save('vent', img)

    # ---- rivet: a bolt head or a lug (read at a very small size)
    img = new()
    for y in range(16):
        for x in range(16):
            d = math.hypot(x - 6.5, y - 6.5) / 9
            c = STEEL[5] if d < 0.45 else STEEL[4] if d < 0.8 else STEEL[2]
            put(img, x, y, c)
    save('rivet', img)

    # ---- hazard stripes
    img = new()
    for y in range(16):
        for x in range(16):
            put(img, x, y, HAZARD[1] if ((x + y) // 4) % 2 else HAZARD[0])
    for i in range(16):
        put(img, i, 0, HAZARD[2])
    save('hazard', img)

    # ---- grate: a dark mesh
    img = new()
    for y in range(16):
        for x in range(16):
            bar = (x % 4 == 0) or (y % 4 == 0)
            put(img, x, y, STEEL[3] if bar else STEEL[0])
    save('grate', img)

    # ---- wheel (cutout): a red valve wheel with four spokes
    img = new(0)
    for y in range(16):
        for x in range(16):
            dx, dy = x - 7.5, y - 7.5
            d = math.hypot(dx, dy)
            c = None
            if 5.2 <= d <= 7.4:
                c = RED[3] if (dx + dy) < 0 else RED[1]
                if d > 6.7:
                    c = RED[0]
            elif d < 5.2 and (abs(dx) < 0.95 or abs(dy) < 0.95) and d > 2.0:
                c = RED[2] if (dx + dy) < 0 else RED[1]
            elif d <= 2.0:
                c = BRASS[3] if d > 1.0 else BRASS[1]
            if c:
                put(img, x, y, c)
    save('wheel', img)

    # ---- gauge (cutout): a round dial with a brass bezel
    img = new(0)
    for y in range(16):
        for x in range(16):
            dx, dy = x - 7.5, y - 7.5
            d = math.hypot(dx, dy)
            c = None
            if d <= 7.5:
                c = BRASS[1] if d > 6.6 else BRASS[4] if d > 5.7 else (232, 228, 212)
                if d <= 5.7:
                    ang = math.degrees(math.atan2(dy, dx))
                    # ticks on the lower-left to lower-right sweep
                    for t in range(-225, 46, 45):
                        if abs(((ang - t + 180) % 360) - 180) < 5 and d > 3.8:
                            c = (30, 30, 34)
                    if 0.5 < d < 4.6 and abs(((ang + 40 + 180) % 360) - 180) < 9:
                        c = (190, 40, 34)
                    if d < 1.2:
                        c = (40, 40, 44)
                    if dy > 3.5 and abs(dx) < 3 and d > 3.2:
                        c = (206, 200, 184)
            if c:
                put(img, x, y, c)
    save('gauge', img)

    # ---- the coil stand-in (swapped for the real coil's sprite)
    img = new()
    for y in range(16):
        for x in range(16):
            band = y % 4
            c = [COPPER[3], COPPER[4], COPPER[2], COPPER[0]][band]
            put(img, x, y, noisy(rnd, c, 6))
    save('coil_marker', img)

    # ---- the furnace window: flames in a loop (8 frames), embers of a machine that waits, the cold window
    ramp = [(40, 8, 6), (96, 18, 8), (170, 40, 10), (226, 84, 14), (255, 138, 28), (255, 196, 70), (255, 238, 150)]

    def fire(h):
        h = max(0.0, min(0.9999, h)) * (len(ramp) - 1)
        i = int(h)
        f = h - i
        a, b = ramp[i], ramp[min(i + 1, len(ramp) - 1)]
        return tuple(int(a[k] + (b[k] - a[k]) * f) for k in range(3))

    frames = 8
    hot = Image.new('RGBA', (16, 16 * frames))
    for f in range(frames):
        t = 2 * math.pi * f / frames
        for y in range(16):
            for x in range(16):
                dx, dy = abs(x - 7.5) / 7.5, abs(y - 7.5) / 7.5
                vign = max(dx, dy) ** 3
                rise = 1 - y / 15.0  # hotter low in the chamber? flames lick upward: bright at the bottom
                tongue = math.sin(2 * math.pi * x / 6.0 + t) * 0.5 + math.sin(2 * math.pi * x / 4.0 - 2 * t + y * 0.6) * 0.5
                flick = math.sin(2 * math.pi * (y / 7.0) - t) * 0.5
                heat = 0.50 + 0.22 * tongue + 0.14 * flick + 0.34 * (1 - rise) * 0.0 + 0.36 * (y / 15.0) - 0.55 * vign
                if rnd.random() < 0.04:
                    heat += 0.25
                hot.putpixel((x, 16 * f + y), fire(heat) + (255,))
    hot.save(os.path.join(textures_dir, 'glow_on.png'))
    with open(os.path.join(textures_dir, 'glow_on.png.mcmeta'), 'w') as fh:
        json.dump({'animation': {'frametime': 2, 'interpolate': True}}, fh)
    wait = new()
    cold = new()
    for y in range(16):
        for x in range(16):
            d = max(abs(x - 7.5), abs(y - 7.5)) / 7.5
            e = 0.34 + 0.22 * (1 - d) + rnd.uniform(-0.07, 0.07)
            put(wait, x, y, fire(e * 0.9))
            v = int(36 + 16 * (1 - d) + rnd.randint(-4, 4))
            put(cold, x, y, (v + 12, v + 2, v - 4))
    save('glow_wait', wait)
    save('glow_off', cold)
