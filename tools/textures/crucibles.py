"""Textures of the 3D crucible items (models/item/<crucible>.json, TerraFirmaGreg's quartz crucible model).
fused_quartz_crucible_{side,top}: TFG's quartz_crucible_{side,top} (TerraFirmaGreg-Team/Core-Modern, LGPL-3.0) as they are.
tritanium_crucible_{side,top}: the same textures mapped by brightness onto a gunmetal blue ramp, with a darker rim line.
    python3 tools/textures/crucibles.py <Core-Modern checkout>"""
import sys
from PIL import Image

SRC = sys.argv[1] + '/src/main/resources/assets/tfg/textures/block/quartz_crucible_%s.png'
OUT = 'af9-core/src/main/resources/assets/af9/textures/item/boules/%s_crucible_%s.png'
DARK, LIGHT = (28, 40, 62), (168, 196, 226)

for part in ('side', 'top'):
    im = Image.open(SRC % part).convert('RGBA')
    im.save(OUT % ('fused_quartz', part))
    out = im.copy()
    px = out.load()
    for y in range(im.height):
        for x in range(im.width):
            r, g, b, a = px[x, y]
            t = ((r + g + b) / 3 - 120) / 135
            t = max(0.0, min(1.0, t))
            px[x, y] = tuple(int(DARK[i] + (LIGHT[i] - DARK[i]) * t) for i in range(3)) + (a,)
    out.save(OUT % ('tritanium', part))
