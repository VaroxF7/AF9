#!/usr/bin/env python3
"""The controller face of the Certus Resonating Manufacturer: a resonance panel with a certus crystal in a fluix
heart, a ring of light around it and three lamps below.

Run from the repository root (needs Pillow):  python3 tools/textures/resonator_face.py [--preview preview.png]

Writes into kubejs/assets/gtceu/textures/block/multiblock/me_resonator:
  overlay_front.png (+ .mcmeta): the machine standing by, the crystal breathing slowly
  overlay_front_paused.png (+ .mcmeta): it waits, dimmed, with an amber lamp
  overlay_front_active.png (+ .mcmeta): it works, the crystal bright and the ring running out
  every one of them has an _emissive layer with the light of the crystal, the ring and the lamps
"""
import json
import math
import os
import sys

from PIL import Image

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), '../..')
OUT = os.path.join(ROOT, 'kubejs/assets/gtceu/textures/block/multiblock/me_resonator')
TAU = 2 * math.pi

RIM = (28, 30, 37)
PLATE = (48, 53, 63)
PLATE_HI = (66, 72, 84)
WINDOW = (14, 16, 22)
WINDOW_HI = (96, 108, 128)
RIVET = (128, 136, 150)
CERTUS = (116, 196, 238)
CERTUS_HI = (222, 246, 255)
CERTUS_LO = (58, 118, 168)
FLUIX = (156, 108, 240)
FLUIX_HI = (232, 196, 255)
SPARK = (255, 108, 92)
LAMP_IDLE = (58, 118, 168)
LAMP_ACTIVE = (86, 226, 132)
LAMP_PAUSED = (226, 176, 84)


def clamp(v):
    return max(0, min(255, round(v)))


def shade(colour, k):
    return tuple(clamp(c * k) for c in colour)


def mix(a, b, t):
    return tuple(clamp(a[i] + (b[i] - a[i]) * t) for i in range(3))


def dot(im, x, y, c):
    if 0 <= x < 16 and 0 <= y < 16:
        im.putpixel((x, y), tuple(c[:3]) + (255,))


def rect(im, x0, y0, x1, y1, c):
    for y in range(y0, y1 + 1):
        for x in range(x0, x1 + 1):
            dot(im, x, y, c)


def diamond(im, cx, cy, c):
    """The certus crystal: a diamond of light in the middle of the panel."""
    for y in range(16):
        for x in range(16):
            d = abs(x + 0.5 - cx) + abs(y + 0.5 - cy)
            if d <= 1.2:
                dot(im, x, y, c)
            elif d <= 2.3:
                dot(im, x, y, shade(c, 0.85))
            elif d <= 3.4:
                dot(im, x, y, shade(c, 0.55))


def ring(im, cx, cy, radius, c):
    """The ring of resonance: a circle of light that runs out of the crystal."""
    for y in range(16):
        for x in range(16):
            d = math.hypot(x + 0.5 - cx, y + 0.5 - cy)
            if abs(d - radius) < 0.55:
                dot(im, x, y, c)


def face(state, frame, frames):
    im = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
    em = Image.new('RGBA', (16, 16), (0, 0, 0, 0))

    # the panel: a steel plate with a dark window in it
    rect(im, 0, 0, 15, 15, RIM)
    rect(im, 1, 1, 14, 14, PLATE)
    for x in range(1, 15):
        dot(im, x, 1, PLATE_HI)
    for x in (0, 15):
        for y in (1, 14):
            dot(im, x, y, RIM)
    for x, y in ((1, 1), (14, 1), (1, 14), (14, 14)):
        dot(im, x, y, RIVET)
    rect(im, 3, 2, 12, 11, WINDOW)
    for x in range(3, 13):
        dot(im, x, 2, WINDOW_HI)
    for y in range(2, 12):
        dot(im, 3, y, WINDOW_HI)
    # the brackets of the sight
    for x, y in ((3, 2), (4, 2), (3, 3), (12, 2), (11, 2), (12, 3),
                 (3, 11), (4, 11), (3, 10), (12, 11), (11, 11), (12, 10)):
        dot(im, x, y, WINDOW_HI)

    if state == 'active':
        pulse = 0.5 + 0.5 * math.sin(TAU * frame / frames)
        speed = 1.0
    elif state == 'paused':
        pulse = 0.18 + 0.08 * math.sin(TAU * frame / frames)
        speed = 0.25
    else:
        pulse = 0.35 + 0.25 * math.sin(TAU * frame / frames)
        speed = 0.6

    # the crystal: certus blue with a fluix heart that flares
    heart = mix(FLUIX, FLUIX_HI, pulse)
    core = mix(CERTUS, CERTUS_HI, pulse)
    diamond(im, 7.5, 6.0, core)
    diamond(em, 7.5, 6.0, mix(core, CERTUS_HI, 0.4))
    # the fluix heart
    dot(im, 7, 6, heart)
    dot(im, 8, 6, heart)
    dot(em, 7, 6, heart)
    dot(em, 8, 6, heart)
    # the redstone spark in the heart, wandering
    sx = 7 + (1 if frame % 4 < 2 else 0)
    sy = 6 if frame % 2 == 0 else 5
    dot(im, sx, sy, SPARK)
    dot(em, sx, sy, SPARK)

    # the ring of resonance running out of the crystal
    radius = 3.6 + speed * 1.6 * (0.5 + 0.5 * math.sin(TAU * frame / frames))
    light = mix(CERTUS_LO, CERTUS, pulse * 0.9)
    ring(im, 7.5, 6.0, radius, light)
    ring(em, 7.5, 6.0, radius, mix(CERTUS_LO, CERTUS, 0.35 + 0.4 * pulse))

    # the lamps below the window
    lamp = {'idle': LAMP_IDLE, 'active': LAMP_ACTIVE, 'paused': LAMP_PAUSED}[state]
    for i in range(3):
        x = 5 + i * 3
        c = lamp if (i == 0 or (state == 'active' and i == 1)) else shade(lamp, 0.45)
        dot(im, x, 13, c)
        dot(em, x, 13, c)
    for x in range(3, 13):
        dot(im, x, 14, RIM)

    return im, em


def strip(frames):
    w, h = frames[0].size
    im = Image.new('RGBA', (w, h * len(frames)))
    for i, fr in enumerate(frames):
        im.paste(fr, (0, i * h))
    return im


def dump(path, data):
    with open(path, 'w', newline='\n') as f:
        json.dump(data, f, indent=2)
        f.write('\n')


def main():
    os.makedirs(OUT, exist_ok=True)
    states = {'overlay_front': ('idle', 12, 6),
              'overlay_front_paused': ('paused', 6, 10),
              'overlay_front_active': ('active', 12, 3)}
    for name, (state, frames, frametime) in states.items():
        base, emissive = [], []
        for f in range(frames):
            b, e = face(state, f, frames)
            base.append(b)
            emissive.append(e)
        for suffix, imgs in (('', base), ('_emissive', emissive)):
            path = os.path.join(OUT, name + suffix + '.png')
            strip(imgs).save(path)
            dump(path + '.mcmeta', {'animation': {'frametime': frametime}})
    if '--preview' in sys.argv:
        preview(sys.argv[sys.argv.index('--preview') + 1])


def preview(dest):
    """A contact sheet: the three states, their frames and their emissive layers."""
    from PIL import ImageDraw
    z = 6
    img = Image.new('RGBA', (16 * z * 14, 16 * z * 8 + 40), (36, 38, 44, 255))
    d = ImageDraw.Draw(img)
    x = 4
    for name in ('overlay_front', 'overlay_front_paused', 'overlay_front_active'):
        base = Image.open(os.path.join(OUT, name + '.png')).convert('RGBA')
        glow = Image.open(os.path.join(OUT, name + '_emissive.png')).convert('RGBA')
        frames = base.height // 16
        d.text((x, 4), name, fill=(230, 232, 238))
        for f in range(min(frames, 12)):
            cell = Image.new('RGBA', (16, 16), (48, 53, 63, 255))
            cell.alpha_composite(base.crop((0, 16 * f, 16, 16 * f + 16)))
            img.alpha_composite(cell.resize((16 * z, 16 * z), Image.NEAREST), (x + f * (16 * z + 3), 16))
            lit = Image.new('RGBA', (16, 16), (0, 0, 0, 255))
            lit.alpha_composite(glow.crop((0, 16 * f, 16, 16 * f + 16)))
            img.alpha_composite(lit.resize((16 * z, 16 * z), Image.NEAREST), (x + f * (16 * z + 3), 16 + 16 * z + 4))
        x += 12 * (16 * z + 3) + 16
    img.save(dest)


if __name__ == '__main__':
    main()
