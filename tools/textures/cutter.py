"""Industrial Cutting Machine textures (original pixel art after the look of GTNH's cutting multiblock):
the purple plated casing and the controller's front panel (idle / working / paused, plus emissive layers).
Run from the repo root: python3 tools/textures/cutter.py"""
import os
from PIL import Image

AF9 = 'af9-core/src/main/resources/assets/af9/textures/block'
GT = 'kubejs/assets/gtceu/textures/block/multiblock/industrial_cutter'


def casing():
    base, edge, dark, bar, bar_hi, dot = (0x6A5AA6, 0x4C3F86, 0x3C316E, 0x8478C4, 0xA094DC, 0x54478F)
    im = Image.new('RGBA', (16, 16))
    px = im.load()
    for y in range(16):
        for x in range(16):
            c = base
            if x in (0, 15) or y in (0, 15):
                c = edge
            elif x in (1, 14) or y in (1, 14):
                c = dark if (x + y) % 2 == 0 else edge
            # two upright bars either side of a centre stud, as on the original plate
            if 3 <= y <= 12 and (x in (3, 4, 5, 6) or x in (9, 10, 11, 12)):
                c = bar
                if x in (3, 9):
                    c = bar_hi
                if x in (6, 12):
                    c = edge
            if 7 <= y <= 8 and 7 <= x <= 8:
                c = dot
            px[x, y] = (c >> 16, (c >> 8) & 255, c & 255, 255)
    im.save(f'{AF9}/cutter_casing.png')


def panel(lamp, teal, white):
    """The front: a black strip with a status lamp above, two display cells and a lamp below."""
    im = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
    px = im.load()

    def fill(x0, y0, x1, y1, rgb):
        for y in range(y0, y1 + 1):
            for x in range(x0, x1 + 1):
                px[x, y] = rgb + (255,)

    fill(2, 2, 11, 5, (0x12, 0x14, 0x18))
    fill(7, 3, 10, 4, lamp)
    fill(2, 8, 11, 12, (0x12, 0x14, 0x18))
    fill(3, 9, 5, 11, teal)
    fill(7, 9, 9, 11, teal)
    fill(13, 9, 14, 10, white)
    return im


def overlays():
    os.makedirs(GT, exist_ok=True)
    off, green, amber = (0x1E, 0x38, 0x24), (0x4A, 0xFF, 0x5A), (0xFF, 0xB0, 0x30)
    teal_off, teal_on = (0x1C, 0x4E, 0x50), (0x2C, 0xD2, 0xB4)
    states = {
        'overlay_front': (off, teal_off, (0x70, 0x70, 0x60)),
        'overlay_front_active': (green, teal_on, (0xFF, 0xF2, 0xA0)),
        'overlay_front_paused': (amber, teal_off, (0x70, 0x70, 0x60)),
    }
    for name, (lamp, teal, white) in states.items():
        panel(lamp, teal, white).save(f'{GT}/{name}.png')
    # emissive layers: the lamps and cells only
    for name, (lamp, teal, white) in states.items():
        if name == 'overlay_front':
            continue
        full = panel(lamp, teal, white)
        em = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
        src, dst = full.load(), em.load()
        for y in range(16):
            for x in range(16):
                if src[x, y][3] and src[x, y][:3] in (lamp, teal, white):
                    dst[x, y] = src[x, y]
        em.save(f'{GT}/{name}_emissive.png')
    Image.new('RGBA', (16, 16), (0, 0, 0, 0)).save(f'{GT}/overlay_front_emissive.png')


casing()
overlays()
