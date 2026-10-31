#!/usr/bin/env python3
"""The Optical Quantum Gravitational Field Generator (af9:optical_quantum_gravitational_field_generator): the Orbital
Array's Mk2 upgrade. A dark lensing core in a gimbal ring with a teal accretion ring turning round it, 8 frames.
    python3 tools/textures/orbital_upgrade.py
writes af9-core/src/main/resources/assets/af9/textures/item/optical_quantum_gravitational_field_generator.png (+ .mcmeta)."""
import math
import os
from PIL import Image

OUT = os.path.join(os.path.dirname(os.path.abspath(__file__)), '..', '..', 'af9-core', 'src', 'main', 'resources',
                   'assets', 'af9', 'textures', 'item', 'optical_quantum_gravitational_field_generator.png')
FRAMES = 8
S = 16


def frame(n):
    img = Image.new('RGBA', (S, S), (0, 0, 0, 0))
    px = img.load()
    cx = cy = 7.5
    ang = 2 * math.pi * n / FRAMES
    for y in range(S):
        for x in range(S):
            dx, dy = x - cx, y - cy
            r = math.hypot(dx, dy)
            # the gimbal frame: a steel square ring
            edge = max(abs(dx), abs(dy))
            if 6.5 <= edge <= 7.6:
                shade = 150 if (x + y) % 2 else 120
                px[x, y] = (shade, shade + 6, shade + 16, 255)
                continue
            # the core: black with a violet rim
            if r < 3.4:
                v = int(40 + 50 * (r / 3.4) ** 2)
                px[x, y] = (v // 2, 6, v, 255)
                continue
            # the accretion ring: tilted ellipse turning round, hot on the side coming at us
            ex = dx * math.cos(ang) + dy * math.sin(ang)
            ey = (-dx * math.sin(ang) + dy * math.cos(ang)) * 2.4
            e = math.hypot(ex, ey)
            if 4.3 <= e <= 6.1:
                heat = 0.5 + 0.5 * math.cos(math.atan2(ey, ex) + ang)
                px[x, y] = (int(40 + 120 * heat), int(180 + 60 * heat), int(200 + 50 * heat), 255)
                continue
            if r < 6.4:
                px[x, y] = (10, 14, 28, 255)
    return img


def main():
    sheet = Image.new('RGBA', (S, S * FRAMES), (0, 0, 0, 0))
    for n in range(FRAMES):
        sheet.paste(frame(n), (0, n * S))
    sheet.save(OUT)
    with open(OUT + '.mcmeta', 'w') as f:
        f.write('{ "animation": { "frametime": 4 } }\n')
    print('wrote', os.path.normpath(OUT))


if __name__ == '__main__':
    main()
