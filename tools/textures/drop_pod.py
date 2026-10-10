#!/usr/bin/env python3
"""The drop pod's model and texture, from one table of boxes.

Writes
  af9-core/src/main/resources/assets/af9/textures/entity/drop_pod.png
  af9-core/src/main/java/com/af9/core/droppod/client/DropPodGeometry.java   (the boxes, with their texture offsets)

An octagonal capsule: a floor on four thrusters and four legs, a waist-high white hull (the rider is seen from the waist up),
eight posts under an orange roof, a stepped dome with a spike and a beacon, a seat, a sliding door and a restraint bar.
Every box grows by a hair more than the one before it (CubeDeformation), so no two faces are ever in the same plane: no
z-fighting. Units are pixels (16 to a block), x and z as the viewer sees the pod (the front is +z), y up from the feet.

Run:  python tools/textures/drop_pod.py
"""
import math
import random
from pathlib import Path

from PIL import Image, ImageDraw

ROOT = Path(__file__).resolve().parents[2]
PNG = ROOT / 'af9-core/src/main/resources/assets/af9/textures/entity/drop_pod.png'
JAVA = ROOT / 'af9-core/src/main/java/com/af9/core/droppod/client/DropPodGeometry.java'
W = 128

cubes = []   # name, style, (w, h, d), (x, ybottom, z), (rx, ry, rz) degrees


def add(name, style, w, h, d, x, yb, z, ry=0.0, rx=0.0, rz=0.0):
    cubes.append((name, style, (w, h, d), (x, yb, z), (rx, ry, rz)))


def tangent(x, z):
    """The yRot (degrees) that lays a box's x axis along the octagon's side at the viewer-frame point (x, z)."""
    return math.degrees(math.atan2(-x, z))


# ---- below the floor: thrusters and legs ----
for sx in (-1, 1):
    for sz in (-1, 1):
        n = f'{"r" if sx > 0 else "l"}{"f" if sz > 0 else "b"}'
        add(f'bell_{n}', 'bell', 8, 3, 8, sx * 8, 0, sz * 8)
        add(f'nozzle_{n}', 'dark', 6, 3, 6, sx * 8, 3, sz * 8)
        add(f'strut_{n}', 'steel', 3, 8, 3, sx * 12, 2, sz * 12)
        add(f'pad_{n}', 'pad', 7, 2, 7, sx * 14, 0, sz * 14)
add('keel', 'dark', 14, 3, 14, 0, 3, 0)

# ---- the floor: an octagon of two plates ----
add('floor_a', 'floor', 22, 3, 22, 0, 6, 0)
add('floor_b', 'floor', 22, 3, 22, 0, 6, 0, ry=45)

# ---- the hull: an octagon of eight panels, waist high ----
R = 7.8
for name, x, z in (('back', 0, -11), ('back_l', -R, -R), ('back_r', R, -R), ('left', -11, 0), ('right', 11, 0),
                   ('front_l', -R, R), ('front_r', R, R)):
    add(f'hull_{name}', 'hull', 10, 18, 2, x, 9, z, ry=tangent(x, z))
add('seat', 'seat', 10, 16, 2, 0, 10, -8.5)

# ---- the posts and the roof ----
for sx in (-1, 1):
    for sz in (-1, 1):
        add(f'post_a_{sx}_{sz}', 'post', 2, 16, 2, sx * 4.6, 27, sz * 11.2)
        add(f'post_b_{sx}_{sz}', 'post', 2, 16, 2, sx * 11.2, 27, sz * 4.6)
add('roof_a', 'roof', 22, 3, 22, 0, 43, 0)
add('roof_b', 'roof', 22, 3, 22, 0, 43, 0, ry=45)
add('dome1_a', 'white', 15, 4, 15, 0, 46, 0)
add('dome1_b', 'white', 15, 4, 15, 0, 46, 0, ry=45)
add('dome2_a', 'orange', 9, 4, 9, 0, 50, 0)
add('dome2_b', 'orange', 9, 4, 9, 0, 50, 0, ry=45)
add('spike', 'steel', 3, 8, 3, 0, 54, 0)
add('beacon', 'glow', 3, 3, 3, 0, 62, 0)
add('light_l', 'glow', 3, 3, 1, -6, 20, 12.2)
add('light_r', 'glow', 3, 3, 1, 6, 20, 12.2)

# ---- the door (slides up) and the restraint (swings) ----
add('door', 'door', 12, 18, 2, 0, 9, 11.8)
add('restraint', 'hazard', 20, 3, 3, 0, 26.5, 9.0)

# ---- the texture: a shelf packer over every box's unfolded faces ----
regions = []
for i, (name, style, (w, h, d), pos, rot) in enumerate(cubes):
    regions.append((i, 2 * (w + d), d + h))
order = sorted(regions, key=lambda r: -r[2])
placed = {}
x = y = shelf = 0
for i, rw, rh in order:
    if x + rw > W:
        x = 0
        y += shelf
        shelf = 0
    placed[i] = (x, y)
    x += rw
    shelf = max(shelf, rh)
H = y + shelf

rng = random.Random(11)
img = Image.new('RGBA', (W, H), (0, 0, 0, 0))
draw = ImageDraw.Draw(img)
PAL = {
    'steel': (120, 126, 134), 'dark': (62, 66, 74), 'white': (228, 230, 232), 'orange': (226, 116, 38),
    'glow': (255, 214, 120), 'hazard': (240, 196, 36), 'black': (26, 26, 28), 'pad': (84, 88, 96),
    'seat': (58, 60, 70), 'floor': (92, 98, 108), 'post': (46, 50, 58), 'roof': (232, 112, 32),
    'door': (232, 234, 236), 'hull': (232, 234, 236), 'bell': (52, 54, 60),
}


def faces(u, v, w, h, d):
    return {'top': (u + d, v, w, d), 'bottom': (u + d + w, v, w, d), 'west': (u, v + d, d, h),
            'north': (u + d, v + d, w, h), 'east': (u + d + w, v + d, d, h), 'south': (u + 2 * d + w, v + d, w, h)}


def fill(rect, base, noise=7):
    fx, fy, fw, fh = rect
    for i in range(fw):
        for j in range(fh):
            n = rng.randint(-noise, noise)
            img.putpixel((fx + i, fy + j), tuple(max(0, min(255, c + n)) for c in base) + (255,))


def outline(rect, color):
    fx, fy, fw, fh = rect
    draw.rectangle([fx, fy, fx + fw - 1, fy + fh - 1], outline=color + (255,))


def rivets(rect, color, step=5):
    fx, fy, fw, fh = rect
    if fw < 6 or fh < 6:
        return
    for i in range(2, fw - 1, step):
        for j in (1, fh - 2):
            img.putpixel((fx + i, fy + j), color + (255,))
    for j in range(2, fh - 1, step):
        for i in (1, fw - 2):
            img.putpixel((fx + i, fy + j), color + (255,))


def stripes(rect, band=3):
    fx, fy, fw, fh = rect
    for i in range(fw):
        for j in range(fh):
            c = PAL['hazard'] if ((i + j) // band) % 2 == 0 else PAL['black']
            img.putpixel((fx + i, fy + j), c + (255,))


for i, (name, style, (w, h, d), pos, rot) in enumerate(cubes):
    u, v = placed[i]
    fs = faces(u, v, w, h, d)
    base = PAL.get(style, PAL['steel'])
    for fname, rect in fs.items():
        fill(rect, base, 4 if style in ('glow', 'door', 'hull', 'white') else 7)
        edge = tuple(int(c * 0.72) for c in base)
        outline(rect, edge)
        if style in ('steel', 'floor', 'pad', 'post', 'dark'):
            rivets(rect, tuple(min(255, int(c * 1.35)) for c in base))
    if style == 'hull':
        # an orange band at the foot and a seam up the middle of the outer faces
        for fname in ('north', 'south', 'east', 'west'):
            fx, fy, fw, fh = fs[fname]
            draw.rectangle([fx, fy + fh - 4, fx + fw - 1, fy + fh - 3], fill=PAL['orange'] + (255,))
            draw.line([fx + fw // 2, fy + 1, fx + fw // 2, fy + fh - 6], fill=(190, 194, 200, 255))
    if style == 'door':
        stripes(fs['south'])
        fx, fy, fw, fh = fs['south']
        draw.rectangle([fx + 2, fy + 2, fx + fw - 3, fy + 6], fill=(36, 62, 90, 255), outline=PAL['black'] + (255,))
        outline(fs['south'], PAL['black'])
    if style == 'hazard':
        for rect in fs.values():
            stripes(rect, 2)
    if style == 'bell':
        fx, fy, fw, fh = fs['bottom']
        fill(fs['bottom'], (255, 150, 40), 14)
        draw.rectangle([fx + 1, fy + 1, fx + fw - 2, fy + fh - 2], outline=(255, 226, 140, 255))
    if style == 'seat':
        for rect in fs.values():
            fx, fy, fw, fh = rect
            for k in range(3, fw - 2, 4):
                draw.line([fx + k, fy + 1, fx + k, fy + fh - 2], fill=(78, 80, 92, 255))

PNG.parent.mkdir(parents=True, exist_ok=True)
img.save(PNG)

# ---- the Java: one child part per box, growth rising a hair at a time ----
lines = []
for i, (name, style, (w, h, d), (x, yb, z), (rx, ry, rz)) in enumerate(cubes):
    u, v = placed[i]
    cx, cy, cz = -x, -(yb + h / 2.0), z          # model space: x mirrored by the renderer's scale(-1, -1, 1), y down
    grow = 0.004 * i
    pose = (f'PartPose.offsetAndRotation({cx:.3f}F, {cy:.3f}F, {cz:.3f}F, {math.radians(rx):.5f}F, '
            f'{math.radians(ry):.5f}F, {math.radians(rz):.5f}F)')
    lines.append(
        f'        pod.addOrReplaceChild("{name}", CubeListBuilder.create().texOffs({u}, {v})\n'
        f'                .addBox({-w / 2.0:.1f}F, {-h / 2.0:.1f}F, {-d / 2.0:.1f}F, {w}F, {h}F, {d}F, '
        f'new CubeDeformation({grow:.3f}F)),\n                {pose});')

JAVA.write_text(f'''package com.af9.core.droppod.client;

import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;

/**
 * GENERATED by tools/textures/drop_pod.py (the boxes of the pod and their places on the {W} x {H} texture): do not edit
 * by hand, change the table in the script and run it again.
 */
final class DropPodGeometry {{

    static final int TEXTURE_WIDTH = {W};
    static final int TEXTURE_HEIGHT = {H};

    private DropPodGeometry() {{}}

    static LayerDefinition layer() {{
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition pod = mesh.getRoot().addOrReplaceChild("pod", CubeListBuilder.create(), PartPose.offset(0, 24, 0));
{chr(10).join(lines)}
        return LayerDefinition.create(mesh, TEXTURE_WIDTH, TEXTURE_HEIGHT);
    }}
}}
''', encoding='utf8')
print('wrote', PNG, f'{W}x{H}', len(cubes), 'boxes')
print('wrote', JAVA)
