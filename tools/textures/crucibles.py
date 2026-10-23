"""Fused quartz crucible and tritanium crucible item icons (16 x 16): a tapered cup seen from a little above, rim
ellipse with the dark inside, a lit left edge, a shaded right edge and a lower belly. Quartz is milky white-blue and
nearly glassy at the rim; tritanium is gunmetal blue with two riveted bands. Run from the repo root:
    python3 tools/textures/crucibles.py"""
from PIL import Image

OUT = 'af9-core/src/main/resources/assets/af9/textures/item/boules/'

# row -> (left, right) inclusive columns of the cup body, top to bottom
BODY = {3: (2, 13), 4: (2, 13), 5: (3, 12), 6: (3, 12), 7: (3, 12), 8: (3, 12), 9: (4, 11), 10: (4, 11), 11: (4, 11),
        12: (5, 10), 13: (5, 10)}


def cup(pal, bands):
    im = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
    px = im.load()
    for y, (l, r) in BODY.items():
        for x in range(l, r + 1):
            t = (x - l) / max(1, r - l)  # 0 at the lit left edge, 1 at the shaded right
            if x == l: c = pal['light']
            elif x == r: c = pal['dark']
            elif t < 0.3: c = pal['lit']
            elif t > 0.7: c = pal['shade']
            else: c = pal['mid']
            if y == 13: c = pal['dark'] if x != l else pal['shade']  # base
            if y in bands and l < x < r: c = pal['band_hi'] if x < (l + r) / 2 else pal['band_lo']
            px[x, y] = c + (255,)
    # rim: a lighter outline row on top, the dark inside below it
    for x in range(2, 14):
        px[x, 2] = pal['rim'] + (255,)
    for x in range(3, 13):
        px[x, 3] = pal['inside'] + (255,)
    for x in range(4, 12):
        px[x, 4] = pal['inside2'] + (255,)
    px[2, 3] = px[13, 3] = pal['rim'] + (255,)
    px[2, 4] = pal['light'] + (255,)
    px[13, 4] = pal['dark'] + (255,)
    # little feet
    for x in (5, 10):
        px[x, 14] = pal['dark'] + (255,)
    # glint on the left belly
    px[4, 7] = pal['glint'] + (255,)
    px[4, 8] = pal['glint'] + (255,)
    return im


QUARTZ = dict(light=(246, 250, 255), lit=(226, 236, 248), mid=(205, 218, 236), shade=(176, 192, 216),
              dark=(138, 156, 186), rim=(255, 255, 255), inside=(120, 134, 160), inside2=(150, 164, 190),
              glint=(255, 255, 255), band_hi=(0, 0, 0), band_lo=(0, 0, 0))
TRITANIUM = dict(light=(150, 176, 206), lit=(108, 134, 168), mid=(84, 108, 142), shade=(62, 82, 114),
                 dark=(38, 52, 76), rim=(176, 204, 232), inside=(24, 32, 48), inside2=(40, 52, 74),
                 glint=(214, 232, 250), band_hi=(130, 156, 188), band_lo=(52, 70, 100))

cup(QUARTZ, ()).save(OUT + 'fused_quartz_crucible.png')
cup(TRITANIUM, (7, 11)).save(OUT + 'tritanium_crucible.png')
