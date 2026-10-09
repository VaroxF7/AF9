#!/usr/bin/env python3
"""Idontknowium: nobody knows what it is, and it looks like a neutron star: a white-blue core, a shell of magnetic arcs, the
pulsar's two beams sweeping round it and sparks of magenta.

Run from the repository root (needs Pillow):  python3 tools/textures/idontknowium.py

Writes into af9-core/src/main/resources/assets/gtceu:
  textures/item/material_sets/idontknowium/{dust,dust_small,dust_tiny}_overlay.png (+ .mcmeta: 32 frames of 16 x 16),
  textures/item/material_sets/idontknowium/empty.png, models/item/material_sets/idontknowium/{dust,dust_small,dust_tiny}.json
  (the icon set of AF9Materials: the dust is the star), and
  textures/block/fluids/fluid.idontknowium_plasma.png (+ .mcmeta): the plasma's still texture (32 frames, tiles and loops).
"""
import json
import math
import os

from PIL import Image

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), '../..')
RES = os.path.join(ROOT, 'af9-core/src/main/resources/assets/gtceu')
FRAMES = 32
TAU = 2 * math.pi


def clamp(v):
    return max(0, min(255, int(v)))


def ramp(t):
    """0..1 to the star's colours: black-indigo, deep blue, electric cyan, white-hot."""
    stops = [(0.0, (6, 4, 28)), (0.35, (20, 40, 150)), (0.6, (40, 170, 255)), (0.82, (190, 240, 255)), (1.0, (255, 255, 255))]
    t = max(0.0, min(1.0, t))
    for (a, ca), (b, cb) in zip(stops, stops[1:]):
        if t <= b:
            f = (t - a) / (b - a)
            return tuple(ca[i] + (cb[i] - ca[i]) * f for i in range(3))
    return stops[-1][1]


def star_frame(frame, radius, beam):
    """One 16 x 16 frame of the dust: the star, its beams, its sparks (transparent round it)."""
    img = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
    px = img.load()
    phase = TAU * frame / FRAMES
    c = 7.5
    for y in range(16):
        for x in range(16):
            dx, dy = x - c, y - c
            d = math.hypot(dx, dy)
            angle = math.atan2(dy, dx)
            r, g, b, a = 0.0, 0.0, 0.0, 0.0
            if d <= radius:
                # the surface: hot at the middle, swirling bands
                heat = 1.0 - (d / radius) ** 1.4
                heat += 0.18 * math.sin(3 * angle + phase * 2 + d * 1.3) * (1 - heat)
                col = ramp(heat)
                r, g, b, a = col[0], col[1], col[2], 255
            # the pulsar's two beams, turning with the frames
            for k in (0, 1):
                ang = phase + k * math.pi + 0.5
                off = abs(math.sin(angle - ang)) * d
                if d > radius - 1 and off < 1.3 and d < beam:
                    strength = (1 - (d - radius) / (beam - radius + 1e-6)) * (1 - off / 1.3) * 1.6
                    if strength > 0.12:
                        mix = clamp(255 * min(1.0, strength))
                        if a < mix:
                            r, g, b, a = 150 + 90 * min(1.0, strength), 235, 255, mix
            # the shell of arcs
            arc = radius + 1.4 + 0.5 * math.sin(4 * angle - phase * 2)
            if abs(d - arc) < 0.55 and a < 200:
                r, g, b, a = 255, 60, 220, 190
            if a > 0:
                px[x, y] = (clamp(r), clamp(g), clamp(b), clamp(a))
    # sparks
    for i in range(3):
        s = (frame * 3 + i * 11) % FRAMES
        sx = int(c + (radius + 2.5) * math.cos(phase * (1 + i) + i * 2.1))
        sy = int(c + (radius + 2.5) * math.sin(phase * (1 + i) + i * 2.1))
        if 0 <= sx < 16 and 0 <= sy < 16 and s % 2 == 0:
            px[sx, sy] = (255, 120, 240, 230)
    return img


def sheet(draw):
    out = Image.new('RGBA', (16, 16 * FRAMES), (0, 0, 0, 0))
    for f in range(FRAMES):
        out.paste(draw(f), (0, 16 * f))
    return out


def plasma_frame(frame):
    """One 16 x 16 frame of the plasma: tiles on every side and loops over the frames (integer wave numbers, whole turns)."""
    img = Image.new('RGBA', (16, 16), (0, 0, 0, 255))
    px = img.load()
    p = TAU * frame / FRAMES
    waves = [(1, 0, 1), (0, 1, -1), (1, 1, 2), (2, -1, -2), (1, -2, 1), (3, 1, 1), (-2, 3, -1)]
    for y in range(16):
        for x in range(16):
            u, v = TAU * x / 16, TAU * y / 16
            # a warped field: waves bend each other, so it boils instead of sliding
            warp = 0.9 * math.sin(v + p) + 0.9 * math.sin(u - p)
            total = 0.0
            for i, (kx, ky, speed) in enumerate(waves):
                total += math.sin(kx * (u + 0.5 * warp) + ky * (v - 0.5 * warp) + speed * p + i)
            t = 0.5 + 0.5 * total / len(waves) * 1.9
            col = list(ramp(max(0.0, min(1.0, t)) ** 1.3))
            # the arcs: thin bright lines where the field crosses a level
            ridge = abs(math.sin(2.5 * total + p))
            if ridge > 0.975:
                col = [255, 70 + 120 * (ridge - 0.975) / 0.025, 235]
            # magenta flares
            flare = math.sin(3 * u + 2 * v + 2 * p) * math.sin(2 * u - 3 * v - p)
            if flare > 0.985:
                col = [max(col[0], 255), 90, 240]
            px[x, y] = (clamp(col[0]), clamp(col[1]), clamp(col[2]), 255)
    return img


def write_mcmeta(path, frametime):
    with open(path + '.mcmeta', 'w', newline='\n') as out:
        json.dump({'animation': {'frametime': frametime, 'interpolate': True}}, out, indent=2)
        out.write('\n')


def main():
    items = os.path.join(RES, 'textures/item/material_sets/idontknowium')
    models = os.path.join(RES, 'models/item/material_sets/idontknowium')
    fluids = os.path.join(RES, 'textures/block/fluids')
    for folder in (items, models, fluids):
        os.makedirs(folder, exist_ok=True)
    for name, radius, beam in (('dust', 4.6, 8.0), ('dust_small', 3.6, 7.0), ('dust_tiny', 2.6, 6.0)):
        path = os.path.join(items, name + '_overlay.png')
        sheet(lambda f, r=radius, b=beam: star_frame(f, r, b)).save(path)
        write_mcmeta(path, 2)
        with open(os.path.join(models, name + '.json'), 'w', newline='\n') as out:
            json.dump({'parent': 'item/generated', 'textures': {
                'layer0': 'gtceu:item/material_sets/idontknowium/empty',
                'layer1': 'gtceu:item/material_sets/idontknowium/empty',
                'layer2': 'gtceu:item/material_sets/idontknowium/%s_overlay' % name}}, out, indent=2)
            out.write('\n')
    Image.new('RGBA', (16, 16), (0, 0, 0, 0)).save(os.path.join(items, 'empty.png'))
    path = os.path.join(fluids, 'fluid.idontknowium_plasma.png')
    sheet(plasma_frame).save(path)
    write_mcmeta(path, 2)
    print('idontknowium textures written')


if __name__ == '__main__':
    main()
