#!/usr/bin/env python3
"""Draws the QRAM and QLOS chip textures from the Quantum-Dot IC ones.

    python3 tools/textures/quantum_chips.py   (from the repository root; needs Pillow)

Both chips are strange-matter family (7 nm), so their icons keep the quantum-dot
shading and are tinted apart: QRAM teal (aligned memory), QLOS violet (CPU).
Safe to run again: outputs are rewritten from the quantum-dot sources.
"""
import os

from PIL import Image

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "../..")
ITEM = os.path.join(ROOT, "af9-core/src/main/resources/assets/af9/textures/item")

# (source file, output file, tint RGB, blend toward tint)
JOBS = [
    ("chips/quantum_dot_ic_chip.png", "chips/qram_chip.png", (64, 224, 208), 0.45),
    ("chips/contaminated_quantum_dot_ic_chip.png", "chips/contaminated_qram_chip.png",
     (64, 224, 208), 0.45),
    ("wafers/quantum_dot_ic_wafer.png", "wafers/qram_wafer.png", (64, 224, 208), 0.45),
    ("quantum_dot_ic_reticle.png", "qram_reticle.png", (64, 224, 208), 0.35),
    ("chips/quantum_dot_ic_chip.png", "chips/qlos_chip.png", (178, 102, 255), 0.45),
    ("chips/contaminated_quantum_dot_ic_chip.png", "chips/contaminated_qlos_chip.png",
     (178, 102, 255), 0.45),
    ("wafers/quantum_dot_ic_wafer.png", "wafers/qlos_wafer.png", (178, 102, 255), 0.45),
    ("quantum_dot_ic_reticle.png", "qlos_reticle.png", (178, 102, 255), 0.35),
]


def tint(src, tint_rgb, blend):
    out = src.copy()
    pixels = out.load()
    for y in range(out.height):
        for x in range(out.width):
            r, g, b, a = pixels[x, y]
            if a == 0:
                continue
            pixels[x, y] = (
                round(r + (tint_rgb[0] - r) * blend),
                round(g + (tint_rgb[1] - g) * blend),
                round(b + (tint_rgb[2] - b) * blend),
                a,
            )
    return out


def main():
    for src_name, dst_name, tint_rgb, blend in JOBS:
        with Image.open(os.path.join(ITEM, src_name)) as src:
            tint(src.convert("RGBA"), tint_rgb, blend).save(os.path.join(ITEM, dst_name))
        print(f"{src_name} -> {dst_name}")


if __name__ == "__main__":
    main()
