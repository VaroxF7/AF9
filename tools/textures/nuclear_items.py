"""Textures of the nuclear rework (docs/nuclear-rework.md): the burner fuels and the graphite moderator line.

Every texture is a recolour of one of AF9's own fission textures (the thorium pellet, rod and spent rod, which are
AF9's own work): the fuel's accent (the green of the pellets) is moved to the fuel's hue, the grey cladding is kept.
Graphite items remap the luminance to a blue-black ramp. Run from the repository root: python3 tools/textures/nuclear_items.py
"""
import colorsys
from pathlib import Path
from PIL import Image

OUT = Path("af9-core/src/main/resources/assets/af9/textures/item")

# fuel -> hue (0..1) of its accent: neptunium teal, americium amber, curium violet, californium red
FUELS = {"np": 0.50, "am": 0.10, "cm": 0.76, "cf": 0.0}


def recolour(src, hue):
    image = Image.open(OUT / f"{src}.png").convert("RGBA")
    out = Image.new("RGBA", image.size)
    pixels = out.load()
    for x in range(image.width):
        for y in range(image.height):
            r, g, b, a = image.getpixel((x, y))
            if a:
                h, s, v = colorsys.rgb_to_hsv(r / 255, g / 255, b / 255)
                if s > 0.12:  # the accent, not the grey metal
                    r, g, b = (round(c * 255) for c in colorsys.hsv_to_rgb(hue, min(1.0, s * 1.15), v))
            pixels[x, y] = (r, g, b, a)
    return out


def graphite(src, lift):
    """Luminance -> a blue-black ramp, `lift` brightens the item (the finished moderator is the lightest)."""
    image = Image.open(OUT / f"{src}.png").convert("RGBA")
    out = Image.new("RGBA", image.size)
    pixels = out.load()
    for x in range(image.width):
        for y in range(image.height):
            r, g, b, a = image.getpixel((x, y))
            if a:
                lum = (0.3 * r + 0.59 * g + 0.11 * b) / 255
                v = 0.10 + 0.42 * lum + lift
                r, g, b = round(v * 255 * 0.92), round(v * 255 * 0.97), round(v * 255 * 1.08)
                r, g, b = min(r, 255), min(g, 255), min(b, 255)
            pixels[x, y] = (r, g, b, a)
    return out


def main():
    for fuel, hue in FUELS.items():
        recolour("fx_thorium_pellet", hue).save(OUT / f"fx_{fuel}_pellet.png")
        recolour("fx_thorium_rod", hue).save(OUT / f"fx_{fuel}_rod.png")
        recolour("fx_spent_thorium_rod", hue).save(OUT / f"fx_spent_{fuel}_rod.png")
    graphite("fx_thorium_pellet", 0.00).save(OUT / "graphite_green_body.png")
    graphite("fx_thorium_pellet", -0.04).save(OUT / "graphite_baked_body.png")
    graphite("fx_thorium_rod", 0.00).save(OUT / "graphite_billet.png")
    graphite("fx_thorium_rod", 0.10).save(OUT / "graphite_moderator.png")


if __name__ == "__main__":
    main()
