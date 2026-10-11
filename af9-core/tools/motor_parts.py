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
    """The housing in the alloy's colours with a small controller module (a BMS board) on top of it."""
    im = recolour(base, RTNA)
    outline = (0x1d, 0x1c, 0x21)
    board, board_lit, chip, pin = (0x2f, 0x5d, 0x4a), (0x4b, 0x8a, 0x6c), (0x16, 0x16, 0x1a), (0xd9, 0xb8, 0x4a)
    led_ok, led_warn = (0x5c, 0xff, 0x7a), (0xff, 0xb0, 0x2e)
    # the module: x 2..8, y 0..3, sitting on the housing's top edge
    for y in range(0, 4):
        for x in range(2, 9):
            edge = y == 0 or x in (2, 8)
            im.putpixel((x, y), outline + (255,) if edge else (board_lit if y == 1 else board) + (255,))
    for x in (3, 4, 5, 6, 7):  # the connector row, where the board meets the housing
        im.putpixel((x, 3), (pin if x % 2 else outline) + (255,))
    im.putpixel((3, 2), chip + (255,))  # the controller chip
    im.putpixel((4, 2), chip + (255,))
    im.putpixel((6, 2), chip + (255,))
    im.putpixel((3, 1), led_ok + (255,))  # status lights
    im.putpixel((5, 1), led_warn + (255,))
    im.putpixel((7, 1), led_ok + (255,))
    return im


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
