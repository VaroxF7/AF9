"""Particle Accelerator cells: GT's Universal Cell (base + fluid overlay) with the fluid's colour on the overlay.

Usage: python3 tools/textures/accelerator_cells.py <GT assets dir: .../assets/gtceu/textures/item/universal_fluid_cell>
Writes af9-core/src/main/resources/assets/af9/textures/item/accelerator/<cell>.png and the item models.
GT's textures are LGPL (GregTech CEu Modern); see CREDITS-textures.md.
"""
import json
import sys
from pathlib import Path

from PIL import Image

CELLS = {
    "tungsten_cell": (0x3b, 0x3a, 0x32),
    "rhenium_127_cell": (0xb4, 0xc9, 0xf0),
    "osmium_plasma_cell": (0x54, 0xaf, 0xff),
}

src = Path(sys.argv[1])
root = Path(__file__).resolve().parents[2] / "af9-core/src/main/resources/assets/af9"
base = Image.open(src / "base.png").convert("RGBA")
overlay = Image.open(src / "overlay.png").convert("RGBA")
overlay = overlay.crop((0, 0, base.width, base.height))  # first frame of the animated strip
for name, (r, g, b) in CELLS.items():
    tinted = overlay.copy()
    px = tinted.load()
    for y in range(tinted.height):
        for x in range(tinted.width):
            pr, pg, pb, pa = px[x, y]
            if pa:
                lum = (pr + pg + pb) / 765
                px[x, y] = (int(r * lum), int(g * lum), int(b * lum), pa)
    out = Image.alpha_composite(base, tinted)
    out.save(root / "textures/item/accelerator" / f"{name}.png")
    (root / "models/item" / f"{name}.json").write_text(json.dumps(
        {"parent": "minecraft:item/generated", "textures": {"layer0": f"af9:item/accelerator/{name}"}}, indent=2) + "\n")
