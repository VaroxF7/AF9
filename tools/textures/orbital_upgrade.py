#!/usr/bin/env python3
"""The Optical Quantum Gravitational Field Generator (af9:optical_quantum_gravitational_field_generator): the Orbital
Array's Mk2 upgrade. A free-floating lensing singularity with a bright halo and two tilted rings (teal and violet) turning
round it, each passing behind and in front of the core, with a spark running along each. 12 frames, no frame or backdrop.
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
# the singularity: black violet with a magenta lensing rim
CORE = [(5, 3, 14), (14, 8, 34), (40, 16, 80), (112, 40, 150), (214, 96, 226)]
# the accretion ring: deep teal to white-cyan
RING = [(8, 60, 84), (14, 118, 140), (40, 188, 196), (130, 240, 232), (240, 255, 250)]
# the second ring: violet to pink-white
HALO = [(60, 20, 100), (130, 50, 170), (200, 90, 220), (240, 170, 245), (255, 240, 255)]


def col(ramp, i):
    c = ramp[max(0, min(len(ramp) - 1, i))]
    return (c[0], c[1], c[2], 255)


def ring_points(radius, tilt, rot, phase, ang):
    """The pixels of a tilted ring: (front, x, y, heat). `front` is the half that passes in front of the core."""
    pts = []
    for k in range(240):
        t = 2 * math.pi * k / 240
        ex, ey = radius * math.cos(t), radius * tilt * math.sin(t)
        x = C + ex * math.cos(rot) - ey * math.sin(rot)
        y = C + ex * math.sin(rot) + ey * math.cos(rot)
        pts.append((math.sin(t) > 0, int(round(x)), int(round(y)), 0.5 + 0.5 * math.cos(t - ang + phase)))
    return pts


def frame(n):
    px = {}
    ang = 2 * math.pi * n / FRAMES
    inside = lambda x, y: 0 <= x < S and 0 <= y < S
    rings = (
        # radius, tilt, rotation in the picture, heat phase, ramp
        (6.9, 0.50 + 0.05 * math.sin(ang), -0.35, 0.0, RING),
        (6.1, 0.50 + 0.05 * math.cos(ang), 1.25, math.pi, HALO),
    )

    def draw_ring(radius, tilt, rot, phase, ramp, front):
        seen = set()
        for f, x, y, heat in ring_points(radius, tilt, rot, phase, ang):
            if f != front or not inside(x, y) or (x, y) in seen:
                continue
            seen.add((x, y))
            if not front and math.hypot(x - C, y - C) < 4.1:
                continue  # the far half hides behind the core
            level = 1 + (1 if heat > 0.3 else 0) + (1 if heat > 0.8 else 0)
            if not front:
                level = max(0, level - 1)
            px[(x, y)] = col(ramp, level)

    for radius, tilt, rot, phase, ramp in rings:
        draw_ring(radius, tilt, rot, phase, ramp, False)

    # --- the singularity: a sphere of radius 3.4 lit from the top left, a magenta lensing rim bottom right,
    # a thin bright halo of bent light round it
    for y in range(S):
        for x in range(S):
            dx, dy = x - C, y - C
            r = math.hypot(dx, dy)
            if r > 4.3:
                continue
            if r > 3.4:
                a = math.atan2(dy, dx)
                glow = 0.5 + 0.5 * math.cos(a - ang)
                px[(x, y)] = col(CORE, 2 + (1 if glow > 0.45 else 0) + (1 if glow > 0.8 else 0))
                continue
            nx, ny = dx / 3.4, dy / 3.4
            light = -(nx + ny) * 0.5
            rim = max(0.0, (nx + ny) * 0.5) * (r / 3.4) ** 2
            level = 1 + (1 if light > 0.35 else 0) + (2 if rim > 0.42 else 1 if rim > 0.2 else 0)
            if r < 2.1 and light < 0.1:
                level = 0
            px[(x, y)] = col(CORE, level)
    px[(5, 5)] = (200, 170, 240, 255)  # the sharp highlight on the glass of the core
    px[(6, 5)] = (120, 80, 180, 255)
    px[(5, 6)] = (90, 56, 150, 255)

    for radius, tilt, rot, phase, ramp in rings:
        draw_ring(radius, tilt, rot, phase, ramp, True)

    # sparks running along the near side of both rings
    for radius, tilt, rot, t0, ramp in ((rings[0][0], rings[0][1], rings[0][2], 1.2, RING),
                                         (rings[1][0], rings[1][1], rings[1][2], 1.2 + math.pi, HALO)):
        t = ang + t0
        if math.sin(t) <= 0:
            continue
        ex, ey = radius * math.cos(t), radius * tilt * math.sin(t)
        qx = int(round(C + ex * math.cos(rot) - ey * math.sin(rot)))
        qy = int(round(C + ex * math.sin(rot) + ey * math.cos(rot)))
        if inside(qx, qy):
            px[(qx, qy)] = col(ramp, 4)
            for ox, oy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                q = (qx + ox, qy + oy)
                if inside(*q) and q not in px:
                    px[q] = (ramp[2][0], ramp[2][1], ramp[2][2], 120)

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
