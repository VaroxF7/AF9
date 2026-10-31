#!/usr/bin/env python3
"""The Optical Quantum Gravitational Field Generator (af9:optical_quantum_gravitational_field_generator): the Orbital
Array's Mk2 upgrade. A lensing singularity in an octagonal steel gimbal with four glowing bolts, a teal accretion ring
turning round it (it passes behind the core and in front of it) and a spark running along the ring. 12 frames.
Drawn from scratch in the style of the good GregTech pack sprites: a dark outline, hue-shifted ramps (cool shadows, warm
or bright lights), one light from the top left, a few sharp highlights, no noise.
    python3 tools/textures/orbital_upgrade.py
writes af9-core/src/main/resources/assets/af9/textures/item/optical_quantum_gravitational_field_generator.png (+ .mcmeta)
and, with --preview, a magnified strip of the frames to the path after it."""
import math
import os
import sys
from PIL import Image

OUT = os.path.join(os.path.dirname(os.path.abspath(__file__)), '..', '..', 'af9-core', 'src', 'main', 'resources',
                   'assets', 'af9', 'textures', 'item', 'optical_quantum_gravitational_field_generator.png')
FRAMES = 12
S = 16
C = 7.5  # centre of the 16x16 canvas

OUTLINE = (9, 8, 22, 255)
# steel gimbal: dark to light, cool in the shadow
STEEL = [(24, 26, 48), (47, 52, 82), (76, 86, 124), (118, 132, 170), (178, 192, 222)]
# the singularity: black violet with a magenta lensing rim
CORE = [(5, 3, 14), (14, 8, 34), (40, 16, 80), (112, 40, 150), (214, 96, 226)]
# the accretion ring: deep teal to white-cyan
RING = [(8, 60, 84), (14, 118, 140), (40, 188, 196), (130, 240, 232), (240, 255, 250)]
BOLT = [(90, 40, 20), (200, 110, 30), (255, 190, 70), (255, 240, 170)]


def col(ramp, i):
    c = ramp[max(0, min(len(ramp) - 1, i))]
    return (c[0], c[1], c[2], 255)


def octagon(dx, dy):
    """Distance-like value of the gimbal's octagon: a square of half-width 6.6 with its corners cut at |dx|+|dy| = 10.6."""
    return max(max(abs(dx), abs(dy)) - 6.6, (abs(dx) + abs(dy) - 10.6) * 0.8)


def frame(n):
    px = {}
    ang = 2 * math.pi * n / FRAMES

    # --- backdrop: the dim sky inside the frame, a few fixed stars
    stars = {(4, 4): 2, (11, 5): 1, (5, 11): 1, (10, 10): 2, (3, 8): 1, (12, 8): 1}
    for y in range(S):
        for x in range(S):
            dx, dy = x - C, y - C
            if octagon(dx, dy) < -1.1:
                d = math.hypot(dx, dy) / 7.0
                px[(x, y)] = (int(10 + 10 * (1 - d)), int(12 + 12 * (1 - d)), int(34 + 26 * (1 - d)), 255)
    for (x, y), k in stars.items():
        tw = (n + x * 3 + y) % FRAMES
        px[(x, y)] = (150, 170, 230, 255) if k == 2 and tw < 4 else (70, 86, 140, 255)

    # --- gimbal: octagonal ring, 1 px thick (2 px in the corners), lit from the top left
    for y in range(S):
        for x in range(S):
            dx, dy = x - C, y - C
            o = octagon(dx, dy)
            if o > 0.4 or o < -1.1:
                continue
            outer = o > -0.5
            lit = (-dx - dy) / 12.0  # -1 .. 1, positive at the top left
            level = 2 + (1 if lit > 0.25 else 0) + (1 if lit > 0.6 else 0) - (1 if lit < -0.25 else 0)
            if not outer:
                level -= 1  # the inner lip is in shadow
            px[(x, y)] = col(STEEL, level)

    # four bolts in the corners of the frame, glowing
    for sx, sy in ((-1, -1), (1, -1), (-1, 1), (1, 1)):
        bx, by = int(C + sx * 4.5), int(C + sy * 5.5)
        pulse = 1 if (n + (0 if sx * sy > 0 else FRAMES // 2)) % FRAMES < FRAMES // 2 else 0
        px[(bx, by)] = col(BOLT, 2 + pulse)
        if sx < 0 and sy < 0:
            px[(bx, by)] = col(BOLT, 3)

    # --- the lensed light: a thin violet halo round the core, brightest where the ring's light bends
    for y in range(S):
        for x in range(S):
            dx, dy = x - C, y - C
            r = math.hypot(dx, dy)
            if 3.2 <= r <= 4.3:
                a = math.atan2(dy, dx)
                glow = 0.5 + 0.5 * math.cos(a - ang)
                px[(x, y)] = col(CORE, 2 + (1 if glow > 0.5 else 0) + (1 if glow > 0.85 else 0))

    # --- the singularity: a sphere of radius 3.6, lit from the top left, a magenta lensing rim bottom right
    for y in range(S):
        for x in range(S):
            dx, dy = x - C, y - C
            r = math.hypot(dx, dy)
            if r > 3.3:
                continue
            nx, ny = dx / 3.3, dy / 3.3
            light = -(nx + ny) * 0.5  # top left
            rim = max(0.0, (nx + ny) * 0.5) * (r / 3.3) ** 2  # bottom right glow
            level = 1 + (1 if light > 0.35 else 0) + (2 if rim > 0.42 else 1 if rim > 0.2 else 0)
            if r < 2.0 and light < 0.1:
                level = 0
            px[(x, y)] = col(CORE, level)
    px[(5, 5)] = (190, 150, 235, 255)  # the sharp highlight on the glass of the core
    px[(6, 5)] = (110, 70, 170, 255)

    # --- the accretion ring: a tilted ellipse turning about the core; the far half is behind it
    tilt = 0.36 + 0.06 * math.sin(ang)  # how far it is opened towards us
    layers = []
    for k in range(120):
        t = 2 * math.pi * k / 120
        ex, ey = 5.2 * math.cos(t), 5.2 * tilt * math.sin(t)
        rot = -0.5  # the ring's own turn in the picture's plane
        x = C + ex * math.cos(rot) - ey * math.sin(rot)
        y = C + ex * math.sin(rot) + ey * math.cos(rot)
        front = math.sin(t) > 0
        heat = 0.5 + 0.5 * math.cos(t - ang)
        layers.append((front, int(round(x)), int(round(y)), heat))
    for front in (False, True):
        for f, x, y, heat in layers:
            if f != front or not (0 <= x < S and 0 <= y < S):
                continue
            if not front and (x, y) in px and px[(x, y)][:3] in [c[:3] for c in CORE]:
                continue  # the far half hides behind the core
            level = 1 + (1 if heat > 0.3 else 0) + (1 if heat > 0.75 else 0)
            if not front:
                level = max(0, level - 1)
            px[(x, y)] = col(RING, level)

    # a spark running along the ring's near side
    t = ang + 1.2
    sx = C + 5.2 * math.cos(t)
    sy = C + 5.2 * tilt * math.sin(t)
    rot = -0.5
    qx = int(round(C + (sx - C) * math.cos(rot) - (sy - C) * math.sin(rot)))
    qy = int(round(C + (sx - C) * math.sin(rot) + (sy - C) * math.cos(rot)))
    if math.sin(t) > 0 and 0 <= qx < S and 0 <= qy < S:
        px[(qx, qy)] = col(RING, 4)
        for ox, oy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
            p = (qx + ox, qy + oy)
            if 0 <= p[0] < S and 0 <= p[1] < S and p not in px:
                px[p] = (60, 200, 210, 140)

    # --- the dark outline round everything
    img = Image.new('RGBA', (S, S), (0, 0, 0, 0))
    out = img.load()
    for (x, y), c in px.items():
        out[x, y] = c
    for y in range(S):
        for x in range(S):
            if (x, y) in px:
                continue
            for ox, oy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                if (x + ox, y + oy) in px and px[(x + ox, y + oy)][3] == 255:
                    out[x, y] = OUTLINE
                    break
    return img


def main():
    sheet = Image.new('RGBA', (S, S * FRAMES), (0, 0, 0, 0))
    frames = [frame(n) for n in range(FRAMES)]
    for n, f in enumerate(frames):
        sheet.paste(f, (0, n * S))
    sheet.save(OUT)
    with open(OUT + '.mcmeta', 'w') as f:
        f.write('{ "animation": { "frametime": 3 } }\n')
    print('wrote', os.path.normpath(OUT))
    if len(sys.argv) > 2 and sys.argv[1] == '--preview':
        scale = 8
        strip = Image.new('RGBA', (S * scale * 6, S * scale * 2), (58, 58, 66, 255))
        for n, f in enumerate(frames):
            big = f.resize((S * scale, S * scale), Image.NEAREST)
            strip.alpha_composite(big, ((n % 6) * S * scale, (n // 6) * S * scale))
        strip.save(sys.argv[2])


if __name__ == '__main__':
    main()
