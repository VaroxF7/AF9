#!/usr/bin/env python3
"""Four animation styles for the Optically Reflective Wafer family (E1 dark prism glass): preview GIF, and (with
--write V N) the animated textures of one style.

    python3 tools/textures/optical_anim.py OUT.gif        preview of all four
"""
import math
import sys
from PIL import Image
import optical_variants as ov
import optical_edge as oe

N = 16
H = ov.hexc
MODE = 'E1 Soft arc'


def lerp(a, b, t):
    t = max(0.0, min(1.0, t))
    return tuple(int(a[i] + (b[i] - a[i]) * t) for i in range(3))


def glyph_order(kind, shape):
    """glyph pixels in drawing order, for a packet to run along"""
    pts, link = ov.glyph(kind) if shape == 'chip' else ov.glyph(kind, 0, -1)
    if kind == 'memristor':
        path = [(3, 8), (5, 8), (6, 5), (9, 11), (10, 8), (12, 8)]
        order = []
        for a, b in zip(path, path[1:]):
            for p in ov.line(a, b):
                q = (p[0], p[1] - (1 if shape == 'wafer' else 0))
                if q not in order:
                    order.append(q)
        return order, pts
    return sorted(pts), pts


def px(img, x, y, c):
    if 0 <= x < 16 and 0 <= y < 16 and img.getpixel((x, y))[3]:
        img.putpixel((x, y), tuple(int(v) for v in c) + (255,))


def frame(style, shape, kind, f):
    ph = f / N
    old_edge = oe.edge_color
    if style == 3:                                           # the edge arc's colours cycle and the arc slides round the rim
        def edge_color(mode, x, y, cx, cy, d, base):
            a = oe.ang(x, y, cx, cy)
            if d <= 2:
                k = max(0.0, 1 - abs(((a - 50 - 360 * ph + 540) % 360) - 180 + 0) / 1e9)
                center = (50 + 360 * ph) % 360
                dist = abs(((a - center + 180) % 360) - 180)
                if dist < 70:
                    kk = 1 - dist / 70
                    cols = [H(0x3a8cff), H(0x8a5cf0), H(0xd04cb0), H(0x40d0ff)]
                    u = (ph * 2 + a / 360.0) % 1.0 * len(cols)
                    c = lerp(cols[int(u) % 4], cols[(int(u) + 1) % 4], u - int(u))
                    return lerp(base, c, (0.35 + 0.65 * kk) if d == 2 else 0.6 * kk)
            return None
        oe.edge_color = edge_color
    img = oe.draw(MODE, shape, kind)
    oe.edge_color = old_edge
    mask = ov.disc_mask() if shape == 'wafer' else ov.chip_mask()
    cx, cy = (7.5, 7.5) if shape == 'wafer' else (7.5, 8)
    if style == 1:                                           # a sheen band sweeps across, with a pause
        pos = -11 + 22 * min(1.0, ph * 1.4)
        for (x, y) in mask:
            s = (x - cx) + (y - cy)
            if ov.edge_depth(mask, x, y) >= 2 and abs(s - pos) < 1.0:
                c = img.getpixel((x, y))[:3]
                px(img, x, y, lerp(c, (200, 230, 255), 0.55 if abs(s - pos) < 0.5 else 0.3))
    elif style == 2:                                         # the glyph breathes, a packet runs along the traces
        if kind:
            order, pts = glyph_order(kind, shape)
            for i, (x, y) in enumerate(order):
                glow = 0.5 + 0.5 * math.sin(2 * math.pi * (ph - i / max(1, len(order))))
                px(img, x, y, lerp(H(0x6fb4ff), (255, 255, 255), glow))
            if kind == 'tpu':
                k = int(ph * 4)
                for (x, y) in pts:
                    cell = (0 if x < 8 else 1) + (0 if y < (8 if shape == 'chip' else 7) else 2)
                    if cell == k:
                        px(img, x, y, (255, 255, 255))
        else:
            for (x, y) in mask:
                r = math.hypot(x - cx, y - cy)
                if ov.edge_depth(mask, x, y) >= 3 and abs(r - (ph * 7)) < 0.7:
                    px(img, x, y, lerp(img.getpixel((x, y))[:3], H(0x8ac0ff), 0.6))
    elif style == 4:                                         # a bright packet runs the circuit; the wafer shows an orbiting spark
        if kind:
            order, pts = glyph_order(kind, shape)
            head = int(ph * (len(order) + 4))
            for t in range(3):
                i = head - t
                if 0 <= i < len(order):
                    px(img, order[i][0], order[i][1], lerp(H(0xffffff), H(0x60b0ff), t / 3))
        else:
            ang = 2 * math.pi * ph
            for t in range(3):
                a = ang - t * 0.35
                x, y = round(cx + 4.2 * math.cos(a)), round(cy + 4.2 * math.sin(a))
                if (x, y) in mask:
                    px(img, x, y, lerp(H(0xffffff), H(0x60b0ff), t / 3))
    return img


STYLES = {1: 'Sheen sweep', 2: 'Pulse and breathe', 3: 'Colour-cycling edge', 4: 'Running packet'}


def preview(out):
    S = 8
    cw = 16 * S + 8
    gif = []
    for f in range(N):
        sheet = Image.new('RGBA', (3 * cw, 4 * cw), (34, 38, 46, 255))
        for r, st in enumerate(STYLES):
            for c, (shape, kind) in enumerate((('wafer', None), ('chip', 'tpu'), ('chip', 'memristor'))):
                sheet.alpha_composite(frame(st, shape, kind, f).resize((16 * S, 16 * S), Image.NEAREST), (c * cw, r * cw))
        gif.append(sheet.convert('RGB').convert('P', palette=Image.ADAPTIVE, colors=128))
    gif[0].save(out, save_all=True, append_images=gif[1:], duration=110, loop=0)




def write(style=3, frametime=4):
    """the final textures: 16-frame vertical strips + .mcmeta for the wafer family (damage of the old textures copied on)"""
    import json
    import os
    import optical_wafers_paths as paths
    item, src = paths.ITEM, paths.SRC
    old = {f: Image.open(os.path.join(src, f + '.png')).convert('RGBA') for f in paths.FILES}

    def damage(base, clean, dmg):
        out = base.copy()
        for y in range(16):
            for x in range(16):
                if dmg.getpixel((x, y)) != clean.getpixel((x, y)) and dmg.getpixel((x, y))[3]:
                    out.putpixel((x, y), dmg.getpixel((x, y)))
        return out

    jobs = {}   # texture -> list of frames
    for f in range(N):
        base = frame(style, 'wafer', None, f)
        jobs.setdefault('wafers/transmuted_neutronium_wafer', []).append(base)
        for dmg in ('broken', 'contaminated'):
            jobs.setdefault(f'wafers/{dmg}_transmuted_neutronium_wafer', []).append(damage(
                base, old['wafers/transmuted_neutronium_wafer'], old[f'wafers/{dmg}_transmuted_neutronium_wafer']))
        for name in ('tpu', 'memristor'):
            chip = frame(style, 'chip', name, f)
            jobs.setdefault(f'wafers/{name}_wafer', []).append(frame(style, 'wafer', name, f))
            jobs.setdefault(f'chips/{name}_chip', []).append(chip)
            jobs.setdefault(f'chips/contaminated_{name}_chip', []).append(
                damage(chip, old[f'chips/{name}_chip'], old[f'chips/contaminated_{name}_chip']))
    for tex, frames in jobs.items():
        strip = Image.new('RGBA', (16, 16 * N))
        for i, fr in enumerate(frames):
            strip.paste(fr, (0, 16 * i))
        strip.save(os.path.join(item, tex + '.png'))
        with open(os.path.join(item, tex + '.png.mcmeta'), 'w', newline='\n') as m:
            json.dump({'animation': {'frametime': frametime}}, m)
            m.write('\n')
        print('wrote', tex)


if __name__ == '__main__':
    if sys.argv[1] == '--write':
        write()
    else:
        preview(sys.argv[1])
