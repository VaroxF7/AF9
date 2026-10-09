#!/usr/bin/env python3
"""The planets of AF9 in Ad Astra: the body textures of the sky and the solar system map, and the sky of every dimension.

Run from the repository root (needs Pillow):  python3 tools/textures/planets.py

Writes af9-core/src/main/resources/assets/af9/textures/environment/<body>.png (the sky body, also the icon on Ad Astra's
solar system map, which AF9 draws, com.af9.core.client.AdAstraMap), assets/af9/planet_renderers/<dimension>.json (Ad Astra
reads every namespace; the renderer is found by the 'effects' of the dimension type, which this sets to af9:<dimension>) and
the effects of data/af9/dimension_type/*.json. Bodies are drawn as shaded spheres of banded noise.
"""
import json
import math
import os
import random

from PIL import Image

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), '../..')
RES = os.path.join(ROOT, 'af9-core/src/main/resources')
ENV = os.path.join(RES, 'assets/af9/textures/environment')
os.makedirs(ENV, exist_ok=True)


def value_noise(seed, cell):
    rnd = random.Random(seed)
    cache = {}

    def g(ix, iy):
        if (ix, iy) not in cache:
            cache[(ix, iy)] = rnd.random()
        return cache[(ix, iy)]

    def at(x, y):
        gx, gy = x / cell, y / cell
        x0, y0 = math.floor(gx), math.floor(gy)
        fx, fy = gx - x0, gy - y0
        fx, fy = fx * fx * (3 - 2 * fx), fy * fy * (3 - 2 * fy)
        a = g(x0, y0) * (1 - fx) + g(x0 + 1, y0) * fx
        b = g(x0, y0 + 1) * (1 - fx) + g(x0 + 1, y0 + 1) * fx
        return a * (1 - fy) + b * fy
    return at


def mix(a, b, t):
    return tuple(int(a[i] + (b[i] - a[i]) * t) for i in range(3))


def sphere(size, seed, shade, light=(-0.5, -0.5), spots=(), ring=None, glow=0.0):
    """shade(u, v, noise) -> rgb for the point (u, v) of the sphere (v: -1 top to 1 bottom).
    ring: (inner, outer, rgb, tilt) as a flat ring seen at a tilt, drawn behind and in front of the sphere."""
    img = Image.new('RGBA', (size, size), (0, 0, 0, 0))
    n1, n2 = value_noise(seed, size / 5), value_noise(seed + 1, size / 14)
    c = (size - 1) / 2
    r = size * (0.5 if ring is None else 0.22)

    def ring_pixel(x, y, front):
        inner, outer, col, tilt = ring
        dx, dy = (x - c) / r, (y - c) / r
        dy2 = dy / tilt
        d = math.hypot(dx, dy2)
        if not (inner <= d <= outer):
            return None
        if dy >= 0 and not front:
            return None
        if dy < 0 and front:
            return None
        lane = 0.75 + 0.25 * math.sin(d * 38)
        a = int(200 * (0.5 + 0.5 * lane))
        return mix((col[0] // 2, col[1] // 2, col[2] // 2), col, lane) + (a,)

    for y in range(size):
        for x in range(size):
            if ring:
                p = ring_pixel(x, y, False)
                if p:
                    img.putpixel((x, y), p)
            dx, dy = (x - c) / r, (y - c) / r
            d2 = dx * dx + dy * dy
            if d2 <= 1.0:
                z = math.sqrt(1 - d2)
                nz = n1(x, y) * 0.65 + n2(x, y) * 0.35
                col = shade(dx, dy, nz)
                for sx, sy, sr, scol in spots:
                    sd = math.hypot((dx - sx) / sr, (dy - sy) / (sr * 0.6))
                    if sd < 1:
                        col = mix(col, scol, 1 - sd * sd)
                lit = max(0.0, -(dx * light[0] + dy * light[1]) / math.hypot(*light) * 0.55 + z * 0.55 + 0.1)
                lit = min(1.0, lit) if not glow else 1.0
                col = tuple(min(255, int(ch * (0.35 + 0.75 * lit))) for ch in col)
                img.putpixel((x, y), col + (255,))
            if ring:
                p = ring_pixel(x, y, True)
                if p:
                    img.putpixel((x, y), p)
    return img


def band(palette, dist=0.18, wob=0.06):
    def shade(u, v, nz):
        t = (v * 0.5 + 0.5 + (nz - 0.5) * dist) * (len(palette) - 1) * 1.7
        t = (math.sin(t * math.pi) * 0.5 + 0.5 + (nz - 0.5) * wob * 4)
        t = min(1, max(0, t))
        i = min(len(palette) - 2, int(t * (len(palette) - 1)))
        return mix(palette[i], palette[i + 1], t * (len(palette) - 1) - i)
    return shade


def rock(base, dark, craters):
    def shade(u, v, nz):
        t = min(1, max(0, nz * 1.3 - 0.15))
        col = mix(dark, base, t)
        for cx, cy, cr in craters:
            d = math.hypot(u - cx, v - cy)
            if d < cr:
                col = mix(col, dark, 0.7 * (1 - d / cr))
            elif d < cr * 1.25:
                col = mix(col, base, 0.35)
        return col
    return shade


def sun(u, v, nz):
    t = min(1, max(0, nz * 1.6 - 0.2))
    return mix((255, 120, 20), (255, 245, 170), t)


BODIES = {
    # name: (size, image)
    'zephyr': (64, sphere(64, 11, band([(0x7A, 0x34, 0x1E), (0xC9, 0x7A, 0x3A), (0xF2, 0xB9, 0x6B), (0xFF, 0xE3, 0xB0),
                                         (0xC9, 0x7A, 0x3A)]),
                          spots=[(0.25, 0.3, 0.2, (0xE8, 0x5A, 0x2A))])),
    'kronos': (128, sphere(128, 23, band([(0x8E, 0x6B, 0x34), (0xC9, 0xA2, 0x5E), (0xF4, 0xEC, 0xD2), (0xCF, 0xC0, 0x9C),
                                           (0xE8, 0xD8, 0xA8)], dist=0.1),
                           ring=(1.35, 2.15, (0xD8, 0xC8, 0xA0), 0.28))),
    'helios': (64, sphere(64, 31, sun, glow=1.0)),
    'ceres': (32, sphere(32, 41, rock((0x9A, 0x92, 0x88), (0x4E, 0x49, 0x44), [(-0.3, -0.2, 0.28), (0.35, 0.3, 0.22),
                                                                                (0.1, -0.55, 0.15)]))),
    'asteroid_field': (32, sphere(32, 51, rock((0x6A, 0x60, 0x58), (0x2C, 0x29, 0x27), [(0.0, 0.0, 0.2)]))),
}
for name, (_, img) in BODIES.items():
    img.save(os.path.join(ENV, name + '.png'))
# the map icons: 16x16 views of the same bodies (the ring of Kronos is its map icon's whole point: keep it)
for name, (size, img) in BODIES.items():
    img.resize((16, 16), Image.LANCZOS).save(os.path.join(ENV, 'icon_' + name + '.png'))


def argb(rgb, alpha=255):
    v = (alpha << 24) | (rgb[0] << 16) | (rgb[1] << 8) | rgb[2]
    return v - (1 << 32) if v >= 1 << 31 else v


def vec(x=0.0, y=0.0, z=0.0):
    return [float(x), float(y), float(z)]


def body(texture, scale, rotation, movement='STATIC', glow=(255, 255, 255), glow_scale=None, local=(0, 0, 0), blend=True):
    return {'back_light_color': argb(glow), 'back_light_scale': float(glow_scale if glow_scale is not None else scale * 2),
            'blend': blend, 'global_rotation': vec(*rotation), 'local_rotation': vec(*local), 'movement_type': movement,
            'scale': float(scale), 'texture': texture}


SUN = 'ad_astra:textures/environment/sun.png'
RED_SUN = 'ad_astra:textures/environment/red_sun.png'


def tex(n):
    return f'af9:textures/environment/{n}.png'


STARS = [{'data': -1447239681, 'weight': 3}, {'data': -1143472129, 'weight': 5}, {'data': -726785, 'weight': 100},
         {'data': -3038977, 'weight': 80}, {'data': -7697665, 'weight': 150}, {'data': -2817, 'weight': 60},
         {'data': -464897, 'weight': 40}]


def renderer(dim, renderables, orbit, fog=False, thick=False, sunrise=(0xE0, 0x90, 0x50), stars=1500, brightness=None):
    out = {'custom_clouds': True, 'custom_sky': True, 'custom_weather': False, 'dimension': dim,
           'has_fog': fog, 'has_thick_fog': thick, 'render_in_rain': orbit, 'sky_renderables': renderables,
           'star_colors': STARS if orbit else [{'data': -1, 'weight': 1}], 'stars': stars, 'sunrise_angle': 0,
           'sunrise_color': argb(sunrise, 0)}
    if brightness is not None:
        out['star_brightness'] = brightness
    return out


# the sky below an orbit: the sun, the planet as large as Ad Astra's own orbits show theirs
def orbit_sky(name, scale, glow, sun_tex=SUN, sun_scale=7.0):
    return renderer(f'af9:{name}_orbit' if name not in ('asteroid_field',) else 'af9:asteroid_field', [
        body(sun_tex, sun_scale, (0, 0, 0), 'TIME_OF_DAY', (255, 255, 217), sun_scale * 3, blend=False),
        body(tex(name if name != 'asteroid_field' else 'ceres'), scale, (180, 0, 0), 'STATIC', glow, scale * 3, blend=False)],
        True, stars=13000, brightness=0.6)


SKIES = {
    'zephyr': renderer('af9:zephyr', [
        body(SUN, 6, (0, 0, 0), 'TIME_OF_DAY', (255, 214, 150), 18),
        body(tex('kronos'), 34, (-35, 0, 160), 'TIME_OF_DAY_REVERSED', (232, 216, 168), 60, local=(0, 25, 0))],
        False, fog=True, thick=True, sunrise=(0xF0, 0x9A, 0x50), stars=900),
    'kronos': renderer('af9:kronos', [
        body(SUN, 3.5, (0, 0, 0), 'TIME_OF_DAY', (255, 236, 190), 10),
        body(tex('zephyr'), 22, (20, 0, 200), 'TIME_OF_DAY_REVERSED', (242, 185, 107), 44)],
        False, fog=True, thick=True, sunrise=(0xE8, 0xD8, 0xA8), stars=900),
    # on the Sun: no night, the whole sky one flare
    'helios': renderer('af9:helios', [
        body(tex('helios'), 140, (0, 0, 0), 'STATIC', (255, 170, 60), 400)],
        False, fog=True, thick=True, sunrise=(0xFF, 0xA0, 0x30), stars=0),
    'ceres': renderer('af9:ceres', [
        body(SUN, 3, (0, 0, 0), 'TIME_OF_DAY', (255, 255, 217), 9, blend=False),
        body(tex('asteroid_field'), 1.2, (-30, 0, 150), 'TIME_OF_DAY', (160, 150, 140), 3)],
        False, stars=3000, brightness=0.6),
    'zephyr_orbit': orbit_sky('zephyr', 80, (242, 185, 107)),
    'kronos_orbit': orbit_sky('kronos', 110, (232, 216, 168), sun_scale=4.0),
    'helios_orbit': orbit_sky('helios', 90, (255, 170, 60)),
    'asteroid_field': orbit_sky('asteroid_field', 60, (170, 160, 150), sun_scale=4.0),
}
SKIES['helios_orbit']['sky_renderables'][0]['scale'] = 0.1  # the Sun is the body below: no second one
rdir = os.path.join(RES, 'assets/af9/planet_renderers')
os.makedirs(rdir, exist_ok=True)
for name, js in SKIES.items():
    # the 'effects' of the dimension type is the key Ad Astra looks the renderer up by (as a dimension key)
    js['dimension'] = f'af9:{name}'
    with open(os.path.join(rdir, name + '.json'), 'w') as f:
        json.dump(js, f, indent=2)
        f.write('\n')
    tp = os.path.join(RES, 'data/af9/dimension_type', name + '.json')
    dt = json.load(open(tp))
    dt['effects'] = f'af9:{name}'
    with open(tp, 'w') as f:
        json.dump(dt, f, indent=2)
        f.write('\n')
