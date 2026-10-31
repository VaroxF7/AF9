#!/usr/bin/env python3
"""Four candidate looks for Sanguinite (preview sheet only; the pick is then written into superalloys.py).

    GT_SRC=/path/to/GregTech-Modern python3 tools/textures/sanguinite_variants.py OUT.png
"""
import sys
import superalloys as sa
from PIL import Image

R = sa.rgb
V = {
    # near-black body, glowing vein network, white-hot sparks
    'A heartblood': dict(pal=sa.ramp(0x07010a, 0x1c0410, 0x45081c, 0x8a1030, 0xd8284c), accent=[R(0xff3a52), R(0xfff0e0)], salt=11),
    # black with cold violet shadows, crimson to magenta speckle cloud, white stars (nebula)
    'B nebula': dict(pal=sa.ramp(0x05020c, 0x1a0824, 0x4a0c3a, 0x9a1456, 0xff3c8a), accent=[R(0xff5ec8), R(0xffffff), R(0x7a3cff)], salt=12),
    # faceted garnet crystal: hard dark/bright facet blocks, white glints
    'C garnet': dict(pal=sa.ramp(0x10020a, 0x3a0618, 0x7a0e30, 0xc01c46, 0xffb8c8), accent=[R(0xffe4ec), R(0x22020c)], salt=13),
    # forged ember: charcoal-red with molten orange-gold cracks
    'D ember': dict(pal=sa.ramp(0x080103, 0x220509, 0x4e0a12, 0x8a1218, 0xc42a1e), accent=[R(0xff7a22), R(0xffd070), R(0x120103)], salt=14),
}


def grain(name, x, y, light, spec):
    a = spec['accent']; pal = spec['pal']
    n = sa.noise(x, y, spec['salt']); t = sa.tone(light)
    if name.startswith('A'):
        if (x * 3 + y * 2) % 7 == 0 and light > 0.2:
            return a[0]                      # veins
        if n < 3:
            return a[1]                      # sparks
        if n < 14:
            return pal[max(0, t - 1)]
    elif name.startswith('B'):
        if n < 4:
            return a[1]                      # stars
        if n < 15:
            return a[n % 3]                  # coloured speckle
        if (x // 2 + y // 2) % 3 == 0:
            return pal[max(0, t - 1)]
    elif name.startswith('C'):
        f = ((x // 3) + (y // 3)) % 3        # facets
        if n < 4:
            return a[0]
        return pal[max(0, min(4, t + (1 if f == 0 else -1 if f == 1 else 0)))]
    elif name.startswith('D'):
        if (x + y * 3) % 8 == 0 or (x * 2 - y) % 9 == 0:
            return a[0] if n % 2 else a[1]   # molten cracks
        if n < 6:
            return a[2]
    return None


sa.grain = grain
sa.tone = lambda light: max(0, min(4, int((max(0.0, light) ** 1.8) * 5)))


def main(out):
    shapes = ['dust', 'ingot', 'plate', 'foil', 'rod', 'wire_fine', 'nugget', 'screw', 'rotor']
    S = 6
    sheet = Image.new('RGBA', (len(shapes) * (16 * S + 6), len(V) * (16 * S + 6)), (36, 40, 48, 255))
    for r, (name, spec) in enumerate(V.items()):
        for c, shape in enumerate(shapes):
            im = sa.sprite(shape, name, spec).resize((16 * S, 16 * S), Image.NEAREST)
            sheet.alpha_composite(im, (c * (16 * S + 6), r * (16 * S + 6)))
    sheet.save(out)


main(sys.argv[1])
