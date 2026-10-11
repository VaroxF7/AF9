"""
Paints the textures of the motor parts of every tier (LV to UHV): housing, rotor, stator, and the industrial motor
housing. Run from af9-core:

    python tools/motor_parts.py

The bases (tools/motor_parts/) are 16x16 in GT's motor palette (grey steel, dark outline); each tier recolours them with its own
(dark, mid, light) ramp, the tier colours the placeholders already used. The rotor and stator bases face left.
"""
import json
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

# the dark grey of the parts that are not the tier's (rotor shaft and flange, stator end ring)
GREY = ((0x22, 0x22, 0x26), (0x62, 0x60, 0x66), (0x9a, 0x99, 0x9e))

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


def recolour_split(im, ramp, other, in_other, is_other_colour=None):
    """Tier colour, except where in_other(x, y, rgb) says the pixel is of the other (dark grey) ramp."""
    out = im.copy()
    for y in range(im.height):
        for x in range(im.width):
            p = im.getpixel((x, y))
            if p[3] == 0:
                continue
            out.putpixel((x, y), tint(p[:3], other if in_other(x, y, p[:3]) else ramp) + (255,))
    return out


def rotor_back(im, ramp):
    """The rotor's far end (right): a dark end plate with a centre pin and a ring of balance holes, a band before it."""
    dark, mid, light = ramp
    out = im.copy()
    # the band where the body meets the plate
    for y in range(4, 12):
        if out.getpixel((12, y))[3] > 0:
            out.putpixel((12, y), tint((0x50, 0x4a, 0x50), ramp) + (255,))
    plate = {5: 0x26, 6: 0x5c, 7: 0xa8, 8: 0xa8, 9: 0x5c, 10: 0x26}
    for y, v in plate.items():
        out.putpixel((14, y), tint((v, v, v), GREY) + (255,))
    # balance holes, two on the body next to the plate
    for y in (5, 10):
        out.putpixel((13, y), tint((0x1d, 0x1c, 0x21), GREY) + (255,))
    # a bright rim light on the top edge of the end plate
    out.putpixel((13, 4), tint((0xe2, 0xe0, 0xe0), ramp) + (255,))
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


# the two status lights on the housing's side change colour every few frames
LED_COLOURS = {'g': (0x5c, 0xff, 0x7a), 'a': (0xff, 0xb0, 0x2e), 'r': (0xff, 0x3a, 0x3a), 'o': (0x30, 0x34, 0x3a)}
LED_UPPER, LED_LOWER = (13, 7), (13, 9)
LED_FRAMES = ('ga', 'gr', 'ag', 'rg', 'ra', 'ga', 'og', 'ao', 'gg', 'ar')


def industrial_strip(base):
    """An animation strip: the module and the housing, and for each frame the colours of the side lights."""
    strip = Image.new('RGBA', (16, 16 * len(LED_FRAMES)), (0, 0, 0, 0))
    for i, (upper, lower) in enumerate(LED_FRAMES):
        frame = base.copy()
        frame.putpixel(LED_UPPER, LED_COLOURS[upper] + (255,))
        frame.putpixel(LED_LOWER, LED_COLOURS[lower] + (255,))
        strip.paste(frame, (0, 16 * i))
    return strip


def main():
    bases = os.path.join(os.path.dirname(os.path.abspath(__file__)), 'motor_parts')
    housing = Image.open(os.path.join(bases, 'housing.png')).convert('RGBA')
    rotor = Image.open(os.path.join(bases, 'rotor.png')).convert('RGBA')
    stator = Image.open(os.path.join(bases, 'stator.png')).convert('RGBA')
    for tier, ramp in RAMPS.items():
        recolour(housing, ramp).save(os.path.join(OUT, tier + '_motor_housing.png'))
        # the rotor faces left: the shaft, bearing seat and flange (x <= 7) are dark grey, the body is the tier's
        rotor_back(recolour_split(rotor, ramp, GREY, lambda x, y, c: x <= 7), ramp).save(
            os.path.join(OUT, tier + '_rotor.png'))
        # the stator: the winding ring round its end is dark grey, the steel stack the tier's, and the neck at the
        # other end is always copper
        stator_out = stator.copy()
        for y in range(stator.height):
            for x in range(stator.width):
                p = stator.getpixel((x, y))
                if p[3] == 0 or (p[:3] in COPPER and x >= 14):
                    continue
                stator_out.putpixel((x, y), tint(p[:3], GREY if (p[:3] in COPPER or x <= 5) else ramp) + (255,))
        # make the neck two pixels wide: copper turns over the last column of the steel
        for y in range(5, 11):
            stator_out.putpixel((13, y), ((0xe7, 0x7c, 0x56) if y % 2 else (0xc1, 0x5a, 0x36)) + (255,))
        stator_out.save(os.path.join(OUT, tier + '_stator.png'))
    industrial_strip(industrial(housing)).save(os.path.join(OUT, 'industrial_electric_motor_housing.png'))
    with open(os.path.join(OUT, 'industrial_electric_motor_housing.png.mcmeta'), 'w') as f:
        json.dump({'animation': {'frametime': 10}}, f)


if __name__ == '__main__':
    main()
