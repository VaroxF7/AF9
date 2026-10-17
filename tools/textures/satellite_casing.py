#!/usr/bin/env python3
"""The Planetary Satellite's casing and controller face (GTNH's Planetary Gas Siphon, GT5-Unofficial, LGPL-3.0).

Run from the repository root (needs Pillow):  python3 tools/textures/satellite_casing.py

The siphon's casing (gtnhintergalactic SiphonCasing.png) is recoloured through a gradient in the colours of
Enriched Naquadah Trinium Europium Duranide (GT: 0xc6b083 with 0x45063d): its blacks stay black, its purple plate
goes through the material's purple to its gold. The controller face is GTNH's own, unchanged.
"""
import os

from PIL import Image

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), '../..')
GT5 = os.environ.get('GT5_SRC', '/tmp/GT5-Unofficial')
CASING_OUT = os.path.join(ROOT, 'af9-core/src/main/resources/assets/af9/textures/block/satellite_casing.png')
FACE_OUT = os.path.join(ROOT, 'kubejs/assets/gtceu/textures/block/multiblock/planetary_satellite')

STOPS = [(0.0, (6, 5, 10)), (0.30, (0x45, 0x06, 0x3D)), (1.0, (0xC6, 0xB0, 0x83))]


def ramp(t):
    t = max(0.0, min(1.0, t))
    for (t0, c0), (t1, c1) in zip(STOPS, STOPS[1:]):
        if t <= t1:
            f = (t - t0) / (t1 - t0)
            return tuple(round(a + (b - a) * f) for a, b in zip(c0, c1)) + (255,)
    return STOPS[-1][1] + (255,)


def main():
    src = Image.open(os.path.join(GT5, 'src/main/resources/assets/gtnhintergalactic/textures/blocks/gasSiphon/SiphonCasing.png')).convert('RGBA')
    out = Image.new('RGBA', src.size)
    for y in range(src.height):
        for x in range(src.width):
            r, g, b, a = src.getpixel((x, y))
            out.putpixel((x, y), ramp((0.3 * r + 0.59 * g + 0.11 * b) / 80.0))
    out.save(CASING_OUT)

    faces = os.path.join(GT5, 'src/main/resources/assets/gregtech/textures/blocks/iconsets')
    os.makedirs(FACE_OUT, exist_ok=True)
    for dst, name in (('overlay_front', ''), ('overlay_front_paused', ''), ('overlay_front_active', ''),
                      ('overlay_front_emissive', '_GLOW'), ('overlay_front_paused_emissive', '_GLOW'),
                      ('overlay_front_active_emissive', '_ACTIVE_GLOW')):
        Image.open(os.path.join(faces, 'OVERLAY_FRONT_PLANETARYSIPHON%s.png' % name)).convert('RGBA').save(
            os.path.join(FACE_OUT, dst + '.png'))


if __name__ == '__main__':
    main()
