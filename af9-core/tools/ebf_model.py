"""
Generates the formed-structure skin of GT's Electric Blast Furnace: the block models and the textures painted for them.
Run from af9-core:

    python tools/ebf_model.py          (then python tools/ebf_preview.py to look at the result)

Model space: 1/16 block units, origin at the controller block's minimum corner, x to the controller's right, y up,
z away from the front. The pattern is 3 wide (x -16..32), 4 high (y 0..64) and 3 deep (z 0..48); the controller is the
middle block of the front, at the bottom.

A vanilla model may only hold coordinates from -16 to 32 on each axis, which is one block less than the structure is
deep, so the skin is not one model but a set of parts: 'ebf_<x>_<y>_<z>.json', each a window of that size, drawn moved by
the offset in its name (units, 'm' for minus). The generator cuts every shape at the windows' borders on its own and
prints the list of parts for StructureSkins.java.

Rules the shapes follow, because the machine's hatches stay real blocks:
  * the bottom layer (y 0..16) and the top layer (y 48..64) are where the hatches go: nothing stands out of the pattern's
    outline there (the skin is 0.5 to 1 unit inside it), what stands out is relief on the two layers between, and above
    the top;
  * the controller (x 0..16, y 0..16, z 0..16) keeps its front free.

Textures: 'coil' is a marker the render swaps for the sprite of the coil the player built with, 'glow' for the lit or the
cold furnace window; both are drawn here as parts of the model (the glow parts are drawn at full brightness while the
machine works).
"""
import glob
import math
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import ebf_paint  # noqa: E402

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), '..', 'src', 'main', 'resources', 'assets', 'af9')
MODELS = os.path.join(ROOT, 'models', 'multiblock')
TEXTURES = os.path.join(ROOT, 'textures', 'block', 'multiblock', 'ebf')

TEX = {
    'casing': 'gtceu:block/casings/solid/machine_casing_heatproof',
    'plate': 'af9:block/multiblock/ebf/plate',
    'post': 'af9:block/multiblock/ebf/post',
    'hoop': 'af9:block/multiblock/ebf/hoop',
    'pipe': 'af9:block/multiblock/ebf/pipe',
    'pipe_h': 'af9:block/multiblock/ebf/pipe_h',
    'brass': 'af9:block/multiblock/ebf/brass',
    'vent': 'af9:block/multiblock/ebf/vent',
    'rivet': 'af9:block/multiblock/ebf/rivet',
    'hazard': 'af9:block/multiblock/ebf/hazard',
    'grate': 'af9:block/multiblock/ebf/grate',
    'wheel': 'af9:block/multiblock/ebf/wheel',
    'gauge': 'af9:block/multiblock/ebf/gauge',
    'coil': 'af9:block/multiblock/ebf/coil_marker',
    'glow': 'af9:block/multiblock/ebf/glow_on',
}

# The windows of each axis, as offsets in units: a window with offset o holds o-16 .. o+32. Earlier ones are preferred
# when a shape fits in several.
WINDOWS = {
    0: [0, -4, 4],
    1: [0, 40],
    2: [16, -4, 36],
}

FACES = ('north', 'south', 'west', 'east', 'up', 'down')
FACE_AXES = {'north': (0, 1), 'south': (0, 1), 'west': (2, 1), 'east': (2, 1), 'up': (0, 2), 'down': (0, 2)}
FACE_NORMAL_AXIS = {'north': 2, 'south': 2, 'west': 0, 'east': 0, 'up': 1, 'down': 1}
FACE_SIGN = {'north': -1, 'south': 1, 'west': -1, 'east': 1, 'up': 1, 'down': -1}
AXIS = {'x': 0, 'y': 1, 'z': 2}
FLAT = [6, 6, 10, 10]  # a plain spot of a texture, for faces that are stretched far beyond a sprite
EPS = 1e-6

CX, CZ = 8.0, 24.0  # the middle of the pattern's footprint

ITEMS = []


def box(a, b, tex, faces=FACES, uv=None, uwin=None, vwin=None, rot=None, opaque=True, tile=True, **extra):
    """A box from a to b (model units). tex: a texture name, or a dict by face ('sides', 'ends', 'default' work too)."""
    it = dict(a=tuple(float(v) for v in a), b=tuple(float(v) for v in b), tex=tex, faces=set(faces), uv=uv, uwin=uwin,
              vwin=vwin, rot=rot, opaque=opaque, tile=tile, axis=None, awin=None)
    it.update(extra)
    ITEMS.append(it)
    return it


# the bars of a regular prism with n sides: (swapped across, turn in degrees). A bar is as wide as the prism's flat to
# flat (its two sides are the prism's) and as long as one side; turning the bars gives all the sides. A turn of a bar
# about the prism's middle is the same in either direction, so the prism comes out right whatever the sign convention.
BARS = {
    8: [(False, 0), (True, 0), (False, 45), (False, -45)],
    16: [(False, 0), (True, 0), (False, 22.5), (False, -22.5), (False, 45), (False, -45), (True, 22.5), (True, -22.5)],
}
CROSS = {0: (1, 2), 1: (0, 2), 2: (0, 1)}  # the two axes across a prism's axis
SIDE_FACES = {(0): ('west', 'east'), (1): ('down', 'up'), (2): ('north', 'south')}
CAP_FACES = {0: ('west', 'east'), 1: ('down', 'up'), 2: ('north', 'south')}


def prism(axis, c, apothem, lo, hi, tex, n=8, awin=None):
    """A regular n-sided prism round the line through c along axis, from lo to hi on that axis. Built of bars, so its
    sides are flat boxes (a texture on them reads straight) and no rotation is off-centre."""
    ax = AXIS[axis]
    u, v = CROSS[ax]
    h = apothem * math.tan(math.pi / n) * 1.01
    for k, (swapped, turn) in enumerate(BARS[n]):
        a, b = [0.0] * 3, [0.0] * 3
        a[ax], b[ax] = lo + 0.01 * k, hi - 0.01 * k
        du, dv = (h, apothem) if swapped else (apothem, h)
        a[u], b[u] = c[u] - du, c[u] + du
        a[v], b[v] = c[v] - dv, c[v] + dv
        # the faces the bar shows: its two flat sides (across the prism) and its caps
        across = v if swapped else u
        faces = set(SIDE_FACES[across]) | set(CAP_FACES[ax])
        rot = None
        if turn:
            origin = list(c)
            origin[ax] = lo
            rot = dict(origin=origin, axis=axis, angle=turn)
        box(a, b, tex, faces=faces, rot=rot, axis=ax, awin=awin)


def square_tube(cx, cz, outer, inner, y0, y1, tex):
    """A hollow octagon of the given flat-to-flat widths: four walls and the same four turned by 45 degrees (a turn of
    all four about the middle is the same in either direction)."""
    t = outer - inner
    h = outer * math.tan(math.pi / 8) * 1.02
    for turn in (0, 45):
        rot = None if not turn else dict(origin=[cx, y0, cz], axis='y', angle=turn)
        box((cx - h, y0, cz - outer), (cx + h, y1, cz - outer + t), tex, rot=rot)
        box((cx - h, y0, cz + outer - t), (cx + h, y1, cz + outer), tex, rot=rot)
        box((cx - outer, y0, cz - h), (cx - outer + t, y1, cz + h), tex, rot=rot)
        box((cx + outer - t, y0, cz - h), (cx + outer, y1, cz + h), tex, rot=rot)


def window_uv(bbox, a, b):
    """The uv of a north face piece of a flat window that is drawn as one stretched picture over its box bbox."""
    (bx0, by0), (bx1, by1) = bbox
    w, h = bx1 - bx0, by1 - by0
    return [16 * (bx1 - b[0]) / w, 16 * (by1 - b[1]) / h, 16 * (bx1 - a[0]) / w, 16 * (by1 - a[1]) / h]


def cell_vent(face, c0, c1):
    """A framed vent on one block-wide cell of the base's face (inside the outline: a hatch there covers it)."""
    y0, y1 = 3.5, 12.5
    if face in ('north', 'south'):
        z0, z1 = (0.4, 1.0) if face == 'north' else (47.0, 47.6)
        v0, v1 = (0.25, 1.0) if face == 'north' else (47.0, 47.75)
        box((c0 + 2, y0, z0), (c1 - 2, y1, z1), 'hoop', vwin=(0, 16), faces=(face,) + ('up', 'down', 'west', 'east'))
        box((c0 + 3, y0 + 1, v0), (c1 - 3, y1 - 1, v1), 'vent', faces=(face,))
    else:
        x0, x1 = (-15.6, -15.0) if face == 'west' else (31.0, 31.6)
        v0, v1 = (-15.75, -15.0) if face == 'west' else (31.0, 31.75)
        box((x0, y0, c0 + 2), (x1, y1, c1 - 2), 'hoop', vwin=(0, 16), faces=(face,) + ('up', 'down', 'north', 'south'))
        box((v0, y0 + 1, c0 + 3), (v1, y1 - 1, c1 - 3), 'vent', faces=(face,))


# =====================================================================================================================
# The furnace
# =====================================================================================================================

def build():
    ITEMS.clear()

    # ---- under the ground: the terrain's top face under the structure is not drawn (the blocks above it are opaque to
    # it), so a plate fills the hole round the base that the margin leaves
    box((-16, -4, 0), (32, 0, 48), 'casing', faces=('up', 'north', 'south', 'west', 'east'))

    # ---- base (layer 0, hatches): the casing slab, a hazard skirt round its foot and a lip round the deck
    box((-15, 0, 1), (31, 16, 47), {'default': 'casing', 'up': 'plate'})
    box((-15.5, 0, 0.5), (31.5, 2.5, 47.5), 'plate', faces=('north', 'south', 'west', 'east'))
    box((-15.5, 15, 0.5), (31.5, 16.1, 2.0), 'hoop', vwin=(0, 16))
    box((-15.5, 15, 46.0), (31.5, 16.1, 47.5), 'hoop', vwin=(0, 16))
    box((-15.5, 15, 2.0), (-14.0, 16.1, 46.0), 'hoop', vwin=(0, 16))
    box((30.0, 15, 2.0), (31.5, 16.1, 46.0), 'hoop', vwin=(0, 16))

    # ---- corner columns: the full height, with a thicker cladding (it stands out of the outline) on the two layers
    # between, collars where it meets the base and the top, a cap above the top with a lamp on the front two
    for (xa, xb, xo) in ((-15, -10.5, -16.5), (26.5, 31, 32.5)):
        for (za, zb, zo) in ((1, 5.5, -0.5), (42.5, 47, 48.5)):
            box((xa, 0, za), (xb, 64, zb), 'post', uwin='center')
            box((xa - 0.4, 64, za - 0.4), (xb + 0.4, 66.2, zb + 0.4), 'hoop', vwin=(0, 16))
            x0, x1 = (xo, xb) if xo < 0 else (xa, xo)
            z0, z1 = (zo, zb) if zo < 24 else (za, zo)
            box((x0, 16, z0), (x1, 48, z1), 'post', uwin='center')
            cx0, cx1 = (x0 - 0.7, x1 + 0.5) if xo < 0 else (x0 - 0.5, x1 + 0.7)
            cz0, cz1 = (z0 - 0.7, z1 + 0.5) if zo < 24 else (z0 - 0.5, z1 + 0.7)
            for (y0, y1) in ((16, 18.5), (45.5, 48)):
                box((cx0, y0, cz0), (cx1, y1, cz1), 'hoop', vwin=(0, 16))

    # ---- panels on the faces of the two hatch layers: a framed vent on every cell of the base, a band round the top.
    # All of it inside the outline, so a hatch standing in a cell just covers it.
    for (c0, c1) in ((-16, 0), (16, 32)):  # the front cells (the controller's keeps its front free)
        cell_vent('north', c0, c1)
    for (c0, c1) in ((0, 16), (16, 32), (32, 48)):
        cell_vent('west', c0, c1)
        cell_vent('east', c0, c1)
    for (c0, c1) in ((-16, 0), (0, 16), (16, 32)):
        cell_vent('south', c0, c1)
    box((-15.5, 54.5, 0.5), (31.5, 57, 47.5), 'hoop', vwin=(0, 16), faces=('north', 'south', 'west', 'east'))

    # ---- the furnace: a round drum (16 sides) between the columns, the two coil bands in steel rings, a belly with
    # the glow slits and studs, flanges at both ends
    drum = (CX, 0.0, CZ)
    coil = {'default': 'coil', 'up': 'hoop', 'down': 'hoop'}
    for (y0, y1, A, tex, awin) in (
            (16.0, 19.0, 22.4, 'hoop', (0, 16)),
            (19.0, 21.0, 20.0, 'plate', (4, 6)),
            (21.0, 22.0, 21.8, 'hoop', (0, 16)),
            (22.0, 28.0, 21.2, coil, (5, 11)),
            (28.0, 29.0, 21.8, 'hoop', (0, 16)),
            (29.0, 35.0, 20.4, 'plate', (4, 10)),
            (35.0, 36.0, 21.8, 'hoop', (0, 16)),
            (36.0, 42.0, 21.2, coil, (5, 11)),
            (42.0, 43.0, 21.8, 'hoop', (0, 16)),
            (43.0, 45.0, 20.0, 'plate', (4, 6)),
            (45.0, 48.0, 22.4, 'hoop', (0, 16))):
        prism('y', drum, A, y0, y1, tex, n=16, awin=awin)
    for k in range(8):
        ang = math.radians(k * 45)
        prism('y', (CX + 21.0 * math.cos(ang), 0, CZ + 21.0 * math.sin(ang)), 1.4, 29.0, 35.0, 'hoop', n=8,
              awin=(0, 16))

    # ---- the door on the front: plating round the window, a frame, the window (the glow parts), bolts, hinges, handle
    for (a, b) in (((-8, 39, -0.6), (24, 45, 2.5)), ((-8, 19, -0.6), (24, 25, 2.5)),
                   ((-8, 25, -0.6), (-1.5, 39, 2.5)), ((17.5, 25, -0.6), (24, 39, 2.5))):
        box(a, b, 'plate')
    box((-9.2, 18, -1.0), (25.2, 20, 2.5), 'hoop')
    box((-9.2, 44, -1.0), (25.2, 46, 2.5), 'hoop', vwin=(0, 16))
    box((-9.2, 20, -1.0), (-8, 44, 2.5), 'hoop')
    box((24, 20, -1.0), (25.2, 44, 2.5), 'hoop')
    for (a, b) in (((-4.5, 39, -1.8), (20.5, 41.5, 0.4)), ((-4.5, 22.5, -1.8), (20.5, 25, 0.4)),
                   ((-4.5, 25, -1.8), (-1.5, 39, 0.4)), ((17.5, 25, -1.8), (20.5, 39, 0.4))):
        box(a, b, 'hoop', vwin=(0, 16))
    bbox = ((-1.5, 25), (17.5, 39))
    for (a, b) in (((-1.5, 28), (17.5, 36)), ((1.5, 25), (14.5, 28)), ((1.5, 36), (14.5, 39))):
        box((a[0], a[1], 0.4), (b[0], b[1], 0.5), 'glow', faces=('north',), uv={'north': window_uv(bbox, a, b)},
            tile=False)
    for (a, b) in (((-1.5, 25), (1.5, 28)), ((14.5, 25), (17.5, 28)), ((-1.5, 36), (1.5, 39)),
                   ((14.5, 36), (17.5, 39))):
        box((a[0], a[1], 0.3), (b[0], b[1], 0.5), 'hoop', faces=('north',))
    for y in (21.2,):
        for d in (-12, -6, 0, 6, 12):
            box((CX + d - 0.8, y - 0.8, -1.4), (CX + d + 0.8, y + 0.8, -0.6), 'rivet', uv={f: [0, 0, 16, 16] for f in FACES},
                tile=False)
    for y in (28.0, 36.0):
        for x in (-6.2, 22.2):
            box((x - 0.8, y - 0.8, -1.4), (x + 0.8, y + 0.8, -0.6), 'rivet', uv={f: [0, 0, 16, 16] for f in FACES},
                tile=False)
    for y0 in (20.5, 36.5):
        prism('y', (-8.8, 0, -1.0), 1.3, y0, y0 + 7.0, 'hoop', n=8, awin=(0, 16))
        box((-8.4, y0 + 1.5, -1.5), (6.0, y0 + 4.5, -0.6), 'hoop', vwin=(0, 16))
    box((21.5, 28, -2.6), (23.2, 36, -1.6), 'hoop', vwin=(0, 16))
    for y in (28.0, 34.8):
        box((21.9, y, -1.7), (22.8, y + 1.2, -0.6), 'hoop')

    # ---- gauges on the front columns
    for x in (-12.9, 28.9):
        box((x - 3.4, 29.5, -1.7), (x + 3.4, 37.5, -0.8), 'brass')
        box((x - 3.0, 30.0, -1.75), (x + 3.0, 37.0, -1.7), 'gauge', faces=('north',),
            uv={'north': [0, 0, 16, 16]}, opaque=False, tile=False)

    # ---- the top (layer 3, hatches): a casing slab 1 unit under the hatches' tops, a parapet round it
    box((-15, 48, 1), (31, 63, 47), {'default': 'casing', 'up': 'plate'})
    box((-15.5, 63, 0.5), (31.5, 65.5, 2.0), 'hoop', vwin=(0, 16))
    box((-15.5, 63, 46.0), (31.5, 65.5, 47.5), 'hoop', vwin=(0, 16))
    box((-15.5, 63, 2.0), (-14.0, 65.5, 46.0), 'hoop', vwin=(0, 16))
    box((30.0, 63, 2.0), (31.5, 65.5, 46.0), 'hoop', vwin=(0, 16))

    # ---- the exhaust stack over the muffler hatch (the middle of the top): a collar, a hollow stack with a rim, embers
    prism('y', drum, 10.0, 63.0, 65.5, 'hoop', n=8, awin=(0, 16))
    prism('y', drum, 8.4, 65.5, 67.0, 'hoop', n=8, awin=(0, 16))
    square_tube(CX, CZ, 6.4, 5.0, 67.0, 71.0, 'plate')
    square_tube(CX, CZ, 7.2, 5.0, 70.0, 72.0, 'hoop')
    box((CX - 4.2, 67.3, CZ - 4.2), (CX + 4.2, 67.4, CZ + 4.2), 'glow', faces=('up',), uv={'up': [0, 0, 16, 16]},
        tile=False)

    # ---- the roof: flues from the stack to a riser on each side, vents in the deck
    for sgn in (-1, 1):
        rx = CX + sgn * 19.0
        prism('y', (rx, 0, CZ), 2.8, 63.0, 71.0, 'pipe', n=8)
        prism('y', (rx, 0, CZ), 3.6, 66.0, 67.6, 'hoop', n=8, awin=(0, 16))
        prism('y', (rx, 0, CZ), 3.2, 70.4, 72.0, 'hoop', n=8, awin=(0, 16))
        fx0, fx1 = (rx, CX - 6.0) if sgn < 0 else (CX + 6.0, rx)
        prism('x', (0, 68.4, CZ), 1.7, fx0, fx1, 'pipe_h', n=8)
        mid = (fx0 + fx1) / 2
        prism('x', (0, 68.4, CZ), 2.5, mid - 0.8, mid + 0.8, 'hoop', n=8)
    for (x0, x1, z0, z1) in ((-10, -2, 36, 44), (18, 26, 36, 44), (-10, -2, 4, 12), (18, 26, 4, 12)):
        box((x0, 63.0, z0), (x1, 63.25, z1), 'grate', faces=('up',))

    # ---- side risers and their stubs into the flanges
    for x in (-16.4, 32.4):
        prism('y', (x, 0, CZ), 2.4, 17.0, 47.0, 'pipe', n=8)
        for y in (17.5, 31.0, 44.5):
            prism('y', (x, 0, CZ), 3.2, y, y + 1.6, 'hoop', n=8, awin=(0, 16))
        sx0, sx1 = (x, CX - 22.4 + 1.0) if x < 0 else (CX + 22.4 - 1.0, x)
        for y in (20.0, 44.0):
            prism('x', (0, y, CZ), 1.5, sx0, sx1, 'pipe_h', n=8)

    for x in (-16.4, 32.4):  # a valve wheel on the front of each riser
        box((x - 0.5, 37.0, 20.6), (x + 0.5, 38.0, 21.9), 'hoop')
        box((x - 3.2, 34.8, 20.15), (x + 3.2, 41.2, 20.6), 'wheel', faces=('north',), uv={'north': [0, 0, 16, 16]},
            opaque=False, tile=False)

    # ---- the back: a manifold between the columns and two connectors
    prism('x', (0, 32.0, 46.8), 2.0, -9.0, 25.0, 'pipe_h', n=8)
    for x in (-4.0, 8.0, 20.0):
        prism('x', (0, 32.0, 46.8), 2.9, x - 0.8, x + 0.8, 'hoop', n=8)
    for x in (-4.0, 20.0):
        prism('y', (x, 0, 46.8), 1.6, 19.0, 45.0, 'pipe', n=8)


# =====================================================================================================================
# Cutting the shapes into parts and elements
# =====================================================================================================================

def rotate(p, rot):
    o = rot['origin']
    q = [p[i] - o[i] for i in range(3)]
    ax = AXIS[rot['axis']]
    a = math.radians(rot['angle'])
    c, s = math.cos(a), math.sin(a)
    i, j = [(1, 2), (2, 0), (0, 1)][ax]
    q[i], q[j] = q[i] * c - q[j] * s, q[i] * s + q[j] * c
    return [q[k] + o[k] for k in range(3)]


def world_extent(it):
    a, b = it['a'], it['b']
    if not it['rot']:
        return [(a[i], b[i]) for i in range(3)]
    pts = [rotate((x, y, z), it['rot']) for x in (a[0], b[0]) for y in (a[1], b[1]) for z in (a[2], b[2])]
    return [(min(p[i] for p in pts), max(p[i] for p in pts)) for i in range(3)]


def split_axis(lo, hi, offsets):
    """The pieces [(offset, lo, hi)] of an interval, each inside one window; one piece if a window holds it all."""
    for o in offsets:
        if o - 16 - EPS <= lo and hi <= o + 32 + EPS:
            return [(o, lo, hi)]
    out, s = [], lo
    while s < hi - EPS:
        best = None
        for o in offsets:
            if o - 16 - EPS <= s < o + 32 - EPS and (best is None or o + 32 > best[1]):
                best = (o, o + 32)
        if best is None:
            raise ValueError('nothing holds %s in %s' % (s, offsets))
        e = min(hi, best[1])
        out.append((best[0], s, e))
        s = e
    return out


def covered(it, face, occluders):
    n_ax = FACE_NORMAL_AXIS[face]
    plane = it['b'][n_ax] if FACE_SIGN[face] > 0 else it['a'][n_ax]
    axes = [i for i in range(3) if i != n_ax]
    for o in occluders:
        if o is it:
            continue
        if FACE_SIGN[face] > 0:
            if not (o['a'][n_ax] <= plane + EPS and o['b'][n_ax] > plane + 1e-3):
                continue
        elif not (o['b'][n_ax] >= plane - EPS and o['a'][n_ax] < plane - 1e-3):
            continue
        if all(o['a'][i] <= it['a'][i] + EPS and o['b'][i] >= it['b'][i] - EPS for i in axes):
            return True
    return False


def face_uv(face, p0, p1, cell):
    """The face's uv as the vanilla default would give it, from coordinates inside one 16-unit cell."""
    x0, y0, z0 = (p0[i] - cell[i] for i in range(3))
    x1, y1, z1 = (p1[i] - cell[i] for i in range(3))
    return {
        'north': [16 - x1, 16 - y1, 16 - x0, 16 - y0],
        'south': [x0, 16 - y1, x1, 16 - y0],
        'west': [z0, 16 - y1, z1, 16 - y0],
        'east': [16 - z1, 16 - y1, 16 - z0, 16 - y0],
        'up': [x0, z0, x1, z1],
        'down': [x0, 16 - z1, x1, 16 - z0],
    }[face]


def centred(width):
    width = min(width, 16.0)
    return [8 - width / 2, 8 + width / 2]


def tex_of(it, face):
    t = it['tex']
    if isinstance(t, str):
        return t
    if face in t:
        return t[face]
    if face in ('north', 'south', 'west', 'east') and 'sides' in t:
        return t['sides']
    if face in ('up', 'down') and 'ends' in t:
        return t['ends']
    return t['default']


def element_uv(it, face, p0, p1):
    if it['uv'] and face in it['uv']:
        return it['uv'][face]
    cell = [math.floor((p0[i] + EPS) / 16) * 16 for i in range(3)]
    u1, v1, u2, v2 = face_uv(face, p0, p1, cell)
    ua, va = FACE_AXES[face]
    if it['axis'] is not None:
        ax = it['axis']
        if ua != ax and va != ax:
            return list(FLAT)
        if ua == ax:  # along the axis in u (a horizontal prism): the v across is centred
            v1, v2 = centred(p1[va] - p0[va])
        else:
            u1, u2 = centred(p1[ua] - p0[ua])
            if it['awin']:
                v1, v2 = it['awin']
        return [u1, v1, u2, v2]
    if it['uwin']:
        u1, u2 = centred(p1[ua] - p0[ua]) if it['uwin'] == 'center' else it['uwin']
    if it['vwin']:
        v1, v2 = centred(p1[va] - p0[va]) if it['vwin'] == 'center' else it['vwin']
    return [u1, v1, u2, v2]


def name_of(off):
    return 'ebf_' + '_'.join(str(int(o)).replace('-', 'm') for o in off)


def emit(it, occluders, parts):
    a, b, rot = it['a'], it['b'], it['rot']
    ext = world_extent(it)
    fixed = rot is not None or it['axis'] is not None  # a turned shape or a bar of a prism is not cut across
    if it['axis'] is not None:
        cut_axes = {it['axis']}
    elif rot:
        cut_axes = {AXIS[rot['axis']]}
    elif it['tile'] and not it['uv']:
        cut_axes = {0, 1, 2}
        if it['uwin']:
            cut_axes -= {0, 2}
        if it['vwin']:
            cut_axes -= {1}
    else:
        cut_axes = set()
    faces = set(it['faces'])
    if not fixed and it['opaque']:
        faces = {f for f in faces if not covered(it, f, occluders)}
    if not faces:
        return 0
    # per axis: the intervals this shape is cut into: (window offset, lo, hi, lo is the shape's own, hi is the shape's own)
    per_axis = []
    for i in range(3):
        if fixed and i not in cut_axes:
            segs = split_axis(ext[i][0], ext[i][1], WINDOWS[i])
            if len(segs) != 1:
                raise ValueError('a turned shape does not fit one window on axis %d: %s %s' % (i, a, b))
            per_axis.append([(segs[0][0], a[i], b[i], True, True)])
            continue
        out = []
        for (o, lo, hi) in split_axis(a[i], b[i], WINDOWS[i]):
            cuts = [lo]
            if i in cut_axes:
                k = math.floor(lo / 16) + 1
                while k * 16 < hi - 1e-6:
                    if k * 16 > lo + 1e-6:
                        cuts.append(k * 16.0)
                    k += 1
            cuts.append(hi)
            for j in range(len(cuts) - 1):
                out.append((o, cuts[j], cuts[j + 1], abs(cuts[j] - a[i]) < EPS, abs(cuts[j + 1] - b[i]) < EPS))
        per_axis.append(out)
    count = 0
    for sx in per_axis[0]:
        for sy in per_axis[1]:
            for sz in per_axis[2]:
                seg = (sx, sy, sz)
                off = tuple(s[0] for s in seg)
                p0 = [seg[i][1] for i in range(3)]
                p1 = [seg[i][2] for i in range(3)]
                outer = {'west': seg[0][3], 'east': seg[0][4], 'down': seg[1][3], 'up': seg[1][4],
                         'north': seg[2][3], 'south': seg[2][4]}
                fdict = {}
                for face in faces:
                    if not outer[face]:
                        continue
                    name = tex_of(it, face)
                    fdict[face] = {'uv': [round(v, 3) for v in element_uv(it, face, p0, p1)], 'texture': '#' + name}
                if not fdict:
                    continue
                lo = [round(p0[i] - off[i], 3) for i in range(3)]
                hi = [round(p1[i] - off[i], 3) for i in range(3)]
                for v in lo + hi:
                    if v < -16 - 1e-6 or v > 32 + 1e-6:
                        raise ValueError('out of the window: %s %s %s' % (lo, hi, it['tex']))
                el = {'from': lo, 'to': hi, 'faces': fdict}
                if rot:
                    ax = AXIS[rot['axis']]
                    origin = [round(rot['origin'][i] - off[i], 3) for i in range(3)]
                    origin[ax] = round((lo[ax] + hi[ax]) / 2, 3)
                    el['rotation'] = {'origin': origin, 'axis': rot['axis'], 'angle': rot['angle']}
                parts.setdefault(off, []).append(el)
                count += 1
    return count


def write_parts(parts):
    import json
    for old in glob.glob(os.path.join(MODELS, 'ebf_*.json')):
        os.remove(old)
    os.makedirs(MODELS, exist_ok=True)
    quads = 0
    for off in sorted(parts):
        els = parts[off]
        used = sorted({f['texture'][1:] for e in els for f in e['faces'].values()})
        textures = {k: TEX[k] for k in used}
        textures['particle'] = TEX['casing']
        text = '{"textures":' + json.dumps(textures, separators=(',', ':')) + ',"elements":[\n'
        text += ',\n'.join(json.dumps(e, separators=(',', ':')) for e in els) + '\n]}\n'
        with open(os.path.join(MODELS, name_of(off) + '.json'), 'w') as f:
            f.write(text)
        quads += sum(len(e['faces']) for e in els)
    return quads


def main():
    build()
    occluders = [it for it in ITEMS if it['opaque'] and not it['rot'] and it['axis'] is None]
    parts = {}
    n = sum(emit(it, occluders, parts) for it in ITEMS)
    quads = write_parts(parts)
    ebf_paint.paint(TEXTURES)
    print('shapes:', len(ITEMS), ' elements:', n, ' quads:', quads, ' parts:', len(parts))
    print('Parts for StructureSkins.java (x, y, z in blocks):')
    for off in sorted(parts):
        print('    part("%s", %s, %s, %s),' % (name_of(off).replace('ebf_', 'multiblock/ebf_', 1), *(
            repr(o / 16.0) + 'F' for o in off)))


if __name__ == '__main__':
    main()
