#!/usr/bin/env python3
"""Energizing Orb Mk2: Powah's orb block model, in electrum gold.

Run from the repository root (needs Pillow), with Powah's jar (the pack's mods folder):
    python3 tools/textures/orb_mk2.py <path to Powah-5.0.11.jar>

Reads Powah's block model of the orb and its two grey textures and writes, into af9-core/src/main/resources/assets/af9:
  models/block/energizing_orb_mk2.json         the same elements, pointing at the textures below
  textures/block/energizing_orb_mk2.png        the orb's body, grey values mapped onto a gold ramp
  textures/block/energizing_orb_mk2_base.png   the base, the same ramp
The pictures are made from Powah's by value only; the result is committed, so the build needs no Powah jar.
"""
import io
import json
import os
import sys
import zipfile

from PIL import Image

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), '../..')
OUT = os.path.join(ROOT, 'af9-core/src/main/resources/assets/af9')

# dark to light: shadowed gold, electrum, bright metal
RAMP = [(0.0, (38, 28, 8)), (0.35, (122, 92, 28)), (0.65, (214, 172, 62)), (1.0, (255, 236, 150))]


def gold(value):
    v = value / 255.0
    for (a, ca), (b, cb) in zip(RAMP, RAMP[1:]):
        if v <= b:
            f = (v - a) / (b - a)
            return tuple(int(ca[i] + (cb[i] - ca[i]) * f) for i in range(3))
    return RAMP[-1][1]


def recolor(data):
    src = Image.open(io.BytesIO(data)).convert('RGBA')
    out = Image.new('RGBA', src.size)
    sp, op = src.load(), out.load()
    for y in range(src.size[1]):
        for x in range(src.size[0]):
            r, g, b, a = sp[x, y]
            lum = (r * 299 + g * 587 + b * 114) // 1000
            op[x, y] = gold(lum) + (a,)
    return out


def main():
    jar = zipfile.ZipFile(sys.argv[1])
    tex = os.path.join(OUT, 'textures/block')
    os.makedirs(tex, exist_ok=True)
    recolor(jar.read('assets/powah/textures/block/energizing_orb.png')).save(os.path.join(tex, 'energizing_orb_mk2.png'))
    recolor(jar.read('assets/powah/textures/block/energizing_orb_base.png')).save(os.path.join(tex, 'energizing_orb_mk2_base.png'))
    model = json.loads(jar.read('assets/powah/models/block/energizing_orb.json'))
    model.pop('credit', None)
    model['textures'] = {
        '0': 'af9:block/energizing_orb_mk2',
        '1': 'af9:block/energizing_orb_mk2_base',
        'particle': 'af9:block/energizing_orb_mk2',
    }
    models = os.path.join(OUT, 'models/block')
    os.makedirs(models, exist_ok=True)
    with open(os.path.join(models, 'energizing_orb_mk2.json'), 'w', newline='\n') as f:
        json.dump(model, f, indent=2)
        f.write('\n')
    print('orb mk2 model and textures written')


if __name__ == '__main__':
    main()
