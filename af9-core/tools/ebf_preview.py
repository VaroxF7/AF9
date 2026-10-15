"""
Software preview of the Electric Blast Furnace skin: reads the written model files (every ebf_<x>_<y>_<z>.json, the
numbers are the part's offset in model units, 'm' for minus), puts the parts together the way StructureSkinRender does
and draws them with their textures, vanilla's entity light and a z-buffer. No game needed.

    python tools/ebf_preview.py [out_dir]

writes front.png, back.png, side.png, top.png and a close-up of the door. The GT casing texture is not in this repo, so a
stand-in is drawn for it.
"""
import glob
import math
import os
import re
import sys

import numpy as np
from PIL import Image

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.join(HERE, '..', 'src', 'main', 'resources', 'assets', 'af9')
MODELS = os.path.join(ROOT, 'models', 'multiblock')
TEXTURES = os.path.join(ROOT, 'textures', 'block', 'multiblock', 'ebf')

FACES = {
    # face: (top-left, top-right, bottom-left, bottom-right) of the face as seen from outside, from (x0,y0,z0,x1,y1,z1)
    'north': lambda a, b: ((b[0], b[1], a[2]), (a[0], b[1], a[2]), (b[0], a[1], a[2]), (a[0], a[1], a[2])),
    'south': lambda a, b: ((a[0], b[1], b[2]), (b[0], b[1], b[2]), (a[0], a[1], b[2]), (b[0], a[1], b[2])),
    'west': lambda a, b: ((a[0], b[1], a[2]), (a[0], b[1], b[2]), (a[0], a[1], a[2]), (a[0], a[1], b[2])),
    'east': lambda a, b: ((b[0], b[1], b[2]), (b[0], b[1], a[2]), (b[0], a[1], b[2]), (b[0], a[1], a[2])),
    'up': lambda a, b: ((a[0], b[1], a[2]), (b[0], b[1], a[2]), (a[0], b[1], b[2]), (b[0], b[1], b[2])),
    'down': lambda a, b: ((a[0], a[1], b[2]), (b[0], a[1], b[2]), (a[0], a[1], a[2]), (b[0], a[1], a[2])),
}
NORMALS = {'north': (0, 0, -1), 'south': (0, 0, 1), 'west': (-1, 0, 0), 'east': (1, 0, 0), 'up': (0, 1, 0),
           'down': (0, -1, 0)}
AXIS = {'x': 0, 'y': 1, 'z': 2}


def rotate(p, rot):
    """Right-handed rotation about an axis through the origin point, the way the game's quaternion does it."""
    o = rot['origin']
    q = [p[i] - o[i] for i in range(3)]
    a = math.radians(rot['angle'])
    c, s = math.cos(a), math.sin(a)
    ax = AXIS[rot['axis']]
    i, j = [(1, 2), (2, 0), (0, 1)][ax]  # the plane the rotation turns, i towards j
    qi, qj = q[i] * c - q[j] * s, q[i] * s + q[j] * c
    q[i], q[j] = qi, qj
    return tuple(q[k] + o[k] for k in range(3))


def rotate_dir(n, rot):
    return rotate(n, {'origin': (0, 0, 0), 'axis': rot['axis'], 'angle': rot['angle']})


def casing_stand_in():
    """The GT heatproof casing: the real texture when EBF_CASING names its png (it is GT's, so it is not kept in this
    repo; the release jar has it), else something green that looks like it."""
    path = os.environ.get('EBF_CASING')
    if path and os.path.exists(path):
        return np.array(Image.open(path).convert('RGBA'))
    rnd = np.random.RandomState(3)
    img = np.zeros((16, 16, 4), dtype=np.uint8)
    for y in range(16):
        for x in range(16):
            c = np.array([116, 146, 120]) + rnd.randint(-5, 6)
            if x in (0, 15) or y in (0, 15):
                c = np.array([74, 97, 67])
            elif x in (1, 14) or y in (1, 14):
                c = np.array([146, 184, 159])
            img[y, x] = list(c) + [255]
    for (x, y) in ((3, 3), (12, 3), (3, 12), (12, 12)):
        img[y, x] = [160, 196, 170, 255]
    return img


class Textures:
    def __init__(self, frame):
        self.cache = {}
        self.frame = frame

    def get(self, ref):
        if ref not in self.cache:
            name = ref.split('/')[-1]
            if ref.startswith('gtceu:'):
                arr = casing_stand_in()
            else:
                arr = np.array(Image.open(os.path.join(TEXTURES, name + '.png')).convert('RGBA'))
                if arr.shape[0] > arr.shape[1]:  # an animation strip: one frame
                    n = arr.shape[0] // arr.shape[1]
                    arr = arr[(self.frame % n) * arr.shape[1]:(self.frame % n + 1) * arr.shape[1]]
            self.cache[ref] = arr
        return self.cache[ref]


def load_quads(textures, lit):
    """Every quad of every part: (4 corner points TL, TR, BL, BR, 4 uv, texture array, normal, glows)."""
    quads = []
    for path in sorted(glob.glob(os.path.join(MODELS, 'ebf_*_*_*.json'))):
        m = re.match(r'ebf_(m?\d+)_(m?\d+)_(m?\d+)\.json', os.path.basename(path))
        off = [int(g.replace('m', '-')) for g in m.groups()]
        import json
        data = json.load(open(path))
        for el in data['elements']:
            a = [el['from'][i] + off[i] for i in range(3)]
            b = [el['to'][i] + off[i] for i in range(3)]
            rot = el.get('rotation')
            if rot:
                rot = {'origin': [rot['origin'][i] + off[i] for i in range(3)], 'axis': rot['axis'],
                       'angle': rot['angle']}
            for face, fd in el['faces'].items():
                tl, tr, bl, br = FACES[face](a, b)
                n = NORMALS[face]
                if rot:
                    tl, tr, bl, br = (rotate(p, rot) for p in (tl, tr, bl, br))
                    n = rotate_dir(n, rot)
                u1, v1, u2, v2 = fd['uv']
                ref = data['textures'][fd['texture'][1:]]
                tex = textures.get(ref)
                quads.append(((tl, tr, bl, br), (u1, v1, u2, v2), tex, n, ref.endswith('glow_on') and lit))
    return quads


def light(n):
    l0 = np.array([0.2, 1.0, -0.7])
    l1 = np.array([-0.2, 1.0, 0.7])
    l0, l1 = l0 / np.linalg.norm(l0), l1 / np.linalg.norm(l1)
    n = np.array(n, dtype=float)
    acc = max(0.0, float(np.dot(l0, n))) + max(0.0, float(np.dot(l1, n)))
    return min(1.0, acc * 0.6 + 0.4)


def render(quads, az, el, scale, size, center, bg=(34, 38, 48), focus=None):
    W, H = size
    azr, elr = math.radians(az), math.radians(el)
    ca, sa = math.cos(azr), math.sin(azr)
    ce, se = math.cos(elr), math.sin(elr)
    color = np.zeros((H, W, 3), dtype=np.float32)
    color[:] = bg
    # a ground tone below y = 0
    zbuf = np.full((H, W), 1e9, dtype=np.float32)

    def project(p):
        x, y, z = p[0] - center[0], p[1] - center[1], p[2] - center[2]
        xr = x * ca + z * sa
        zr = -x * sa + z * ca
        sx = -xr
        sy = y * ce + zr * se
        depth = -y * se + zr * ce
        return (W / 2 + sx * scale, H / 2 - sy * scale + (focus or 0), depth)

    for (pts, uv, tex, n, glows) in quads:
        P = [project(p) for p in pts]
        # back-face: skip faces turned away (the render is no-cull, but a closed model never shows its backs)
        nr = (n[0] * ca + n[2] * sa, n[1], -n[0] * sa + n[2] * ca)
        facing = -(-nr[1] * se + nr[2] * ce)  # normal against the view direction
        if facing <= 1e-6:
            continue
        shade = 1.0 if glows else light(n)
        u1, v1, u2, v2 = uv
        th, tw = tex.shape[:2]
        tl, tr, bl, br = P
        for tri in (((tl, (0, 0)), (tr, (1, 0)), (bl, (0, 1))), ((tr, (1, 0)), (br, (1, 1)), (bl, (0, 1)))):
            (p0, s0), (p1, s1), (p2, s2) = tri
            minx = int(max(0, math.floor(min(p0[0], p1[0], p2[0]))))
            maxx = int(min(W - 1, math.ceil(max(p0[0], p1[0], p2[0]))))
            miny = int(max(0, math.floor(min(p0[1], p1[1], p2[1]))))
            maxy = int(min(H - 1, math.ceil(max(p0[1], p1[1], p2[1]))))
            if minx > maxx or miny > maxy:
                continue
            den = (p1[1] - p2[1]) * (p0[0] - p2[0]) + (p2[0] - p1[0]) * (p0[1] - p2[1])
            if abs(den) < 1e-9:
                continue
            xs, ys = np.meshgrid(np.arange(minx, maxx + 1) + 0.5, np.arange(miny, maxy + 1) + 0.5)
            w0 = ((p1[1] - p2[1]) * (xs - p2[0]) + (p2[0] - p1[0]) * (ys - p2[1])) / den
            w1 = ((p2[1] - p0[1]) * (xs - p2[0]) + (p0[0] - p2[0]) * (ys - p2[1])) / den
            w2 = 1 - w0 - w1
            inside = (w0 >= -1e-4) & (w1 >= -1e-4) & (w2 >= -1e-4)
            if not inside.any():
                continue
            depth = w0 * p0[2] + w1 * p1[2] + w2 * p2[2]
            s = w0 * s0[0] + w1 * s1[0] + w2 * s2[0]
            t = w0 * s0[1] + w1 * s1[1] + w2 * s2[1]
            u = u1 + s * (u2 - u1)
            v = v1 + t * (v2 - v1)
            tx = np.clip((u / 16 * tw).astype(int), 0, tw - 1)
            ty = np.clip((v / 16 * th).astype(int), 0, th - 1)
            texel = tex[ty, tx]
            ok = inside & (texel[..., 3] > 25)
            sub = zbuf[miny:maxy + 1, minx:maxx + 1]
            ok &= depth < sub - 1e-4
            if not ok.any():
                continue
            sub[ok] = depth[ok]
            col = color[miny:maxy + 1, minx:maxx + 1]
            col[ok] = texel[..., :3][ok].astype(np.float32) * shade
    return Image.fromarray(np.clip(color, 0, 255).astype(np.uint8))


VIEWS = {
    'front': dict(az=-32, el=24),
    'back': dict(az=148, el=24),
    'side': dict(az=-100, el=18),
    'top': dict(az=-30, el=62),
}


def main():
    out = sys.argv[1] if len(sys.argv) > 1 else os.path.join(HERE, 'preview')
    only = sys.argv[2].split(',') if len(sys.argv) > 2 else None
    lit = os.environ.get('EBF_LIT', '1') == '1'
    os.makedirs(out, exist_ok=True)
    textures = Textures(frame=int(os.environ.get('EBF_FRAME', '2')))
    quads = load_quads(textures, lit)
    print(len(quads), 'quads')
    for name, v in VIEWS.items():
        if only and name not in only:
            continue
        img = render(quads, v['az'], v['el'], scale=9, size=(760, 860), center=(8, 36, 24))
        img.save(os.path.join(out, name + '.png'))
    if not only or 'door' in only:
        img = render(quads, -18, 14, scale=22, size=(900, 700), center=(8, 32, 0))
        img.save(os.path.join(out, 'door.png'))


if __name__ == '__main__':
    main()
