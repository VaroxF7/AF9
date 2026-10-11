"""
Paints the textures of the motor parts of every tier (LV to UHV): housing, rotor, stator, and the industrial motor
housing. Run from af9-core:

    python tools/motor_parts.py

The bases (tools/motor_parts/) are 16x16 in GT's motor palette (grey steel, dark outline); each tier recolours them with its own
(dark, mid, light) ramp, the tier colours the placeholders already used. The rotor and stator bases face left.
"""
import os

from PIL import Image

OUT = os.path.join(os.path.dirname(os.path.abspath(__file__)), '..', 'src', 'main', 'resources', 'assets', 'af9',
                   'textures', 'item')

# tier: (dark, mid, light)
RAMPS = {
    'lv': ((0x3a, 0x3a, 0x3a), (0xa7, 0xa7, 0xa7), (0xd9, 0xd9, 0xd9)),
    'mv': ((0x2b, 0x40, 0x4b), (0x7d, 0xb9, 0xd8), (0xa2, 0xf0, 0xff)),
    'hv': ((0x4b, 0x4d, 0x4f), (0xd8, 0xdc, 0xe4), (0xff, 0xff, 0xff)),
    'ev': ((0x52, 0x31, 0x51), (0xed, 0x8e, 0xea), (0xff, 0xb8, 0xff)),
    'iv': ((0x24, 0x2c, 0x48), (0x68, 0x7e, 0xce), (0x87, 0xa3, 0xff)),
    'luv': ((0x39, 0x2d, 0x42), (0xa4, 0x82, 0xbf), (0xd5, 0xa9, 0xf8)),
    'zpm': ((0x18, 0x3c, 0x3f), (0x47, 0xad, 0xb6), (0x5c, 0xe0, 0xec)),
    'uv': ((0x44, 0x1e, 0x24), (0xc3, 0x57, 0x69), (0xfd, 0x71, 0x88)),
    'uhv': ((0x51, 0x52, 0x54), (0xe8, 0xec, 0xf2), (0xff, 0xff, 0xff)),
}
# reinforced titanium niobium alloy: AF9Materials colour 0x8a94c8
RTNA = ((0x2c, 0x31, 0x52), (0x8a, 0x94, 0xc8), (0xb0, 0xba, 0xff))

COPPER = {(0xc1, 0x5a, 0x36), (0xe7, 0x7c, 0x56), (0x9c, 0x4e, 0x31), (0x8a, 0x41, 0x29)}


def lum(c):
    return (c[0] * 299 + c[1] * 587 + c[2] * 114) // 1000


def lerp(a, b, t):
    return tuple(int(round(a[i] + (b[i] - a[i]) * t)) for i in range(3))


def tint(c, ramp):
    """Grey steel -> the ramp: the base's body grey (about 0x78) becomes the mid colour, its darks the dark, its lights the light."""
    dark, mid, light = ramp
    v = lum(c)
    if v <= 0x24:  # the outline
        return tuple(int(x * 0.5) for x in dark)
    if v <= 0x3e:
        return lerp(tuple(int(x * 0.5) for x in dark), dark, (v - 0x24) / (0x3e - 0x24))
    if v <= 0x78:
        return lerp(dark, mid, (v - 0x3e) / (0x78 - 0x3e))
    return lerp(mid, light, min(1.0, (v - 0x78) / (0xe2 - 0x78)))


def recolour(im, ramp, keep=()):
    out = im.copy()
    for y in range(im.height):
        for x in range(im.width):
            p = im.getpixel((x, y))
            if p[3] == 0 or p[:3] in keep:
                continue
            out.putpixel((x, y), tint(p[:3], ramp) + (255,))
    return out


def industrial(base):
    """The housing, fatter and heavier: a thick bevelled shell, ribs and bolts, in the alloy's colours."""
    im = recolour(base, RTNA)
    w, h = im.size
    filled = {(x, y) for y in range(h) for x in range(w) if im.getpixel((x, y))[3] > 0}
    # the outside: transparent pixels reachable from the border
    outside, stack = set(), [(x, y) for x in range(w) for y in range(h) if x in (0, w - 1) or y in (0, h - 1)]
    while stack:
        x, y = stack.pop()
        if (x, y) in outside or (x, y) in filled or not (0 <= x < w and 0 <= y < h):
            continue
        outside.add((x, y))
        stack += [(x + 1, y), (x - 1, y), (x, y + 1), (x, y - 1)]
    ring = {(x, y) for (x, y) in outside
            if any((x + dx, y + dy) in filled for dx in (-1, 0, 1) for dy in (-1, 0, 1))}
    dark, mid, light = RTNA
    out = Image.new('RGBA', (w, h), (0, 0, 0, 0))
    for (x, y) in filled:
        out.putpixel((x, y), im.getpixel((x, y)))
    for (x, y) in ring:
        # the new rim: lit on its top-left, dark on its bottom-right
        lit = (x + y) < (min(px + py for px, py in filled) + 6)
        out.putpixel((x, y), (light if (y <= min(py for _, py in filled) and lit) else
                              mid if lit else dark) + (255,))
    # the old outline is now a groove between the rim and the body: keep it dark
    for (x, y) in filled:
        p = base.getpixel((x, y))
        if lum(p[:3]) <= 0x24:
            out.putpixel((x, y), tuple(int(c * 0.6) for c in dark) + (255,))
    # a rim outline round all of it
    final = out.copy()
    for y in range(h):
        for x in range(w):
            if out.getpixel((x, y))[3] == 0 and any(
                    0 <= x + dx < w and 0 <= y + dy < h and out.getpixel((x + dx, y + dy))[3] > 0
                    for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1))):
                final.putpixel((x, y), tuple(int(c * 0.5) for c in dark) + (255,))
    return final


def main():
    bases = os.path.join(os.path.dirname(os.path.abspath(__file__)), 'motor_parts')
    housing = Image.open(os.path.join(bases, 'housing.png')).convert('RGBA')
    rotor = Image.open(os.path.join(bases, 'rotor.png')).convert('RGBA')
    stator = Image.open(os.path.join(bases, 'stator.png')).convert('RGBA')
    for tier, ramp in RAMPS.items():
        recolour(housing, ramp).save(os.path.join(OUT, tier + '_motor_housing.png'))
        recolour(rotor, ramp).save(os.path.join(OUT, tier + '_rotor.png'))
        recolour(stator, ramp, keep=COPPER).save(os.path.join(OUT, tier + '_stator.png'))
    industrial(housing).save(os.path.join(OUT, 'industrial_electric_motor_housing.png'))


if __name__ == '__main__':
    main()
