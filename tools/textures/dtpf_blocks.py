#!/usr/bin/env python3
"""The blocks of the rebuilt Dimensionally Transcendent Plasma Forge: three casings that are GTNH's own.

  dtpf_transcendent_casing   GT5-Unofficial's Dimensionally Transcendent Casing (MACHINE_DIM_TRANS_CASING), the hall's
                             plating; joins its neighbours (LDLib connected texture)
  dtpf_injection_casing      the Dimensional Injection Casing (MACHINE_DIM_INJECTOR), where the hatches go; its maze lights up
                             in a field-stabilising pulse (nine frames, after GTNH's Stability Casing: the core of the emitter
                             in its middle shrinks to nothing, a ring flares and the core grows back)
  dtpf_bridge_casing         the Dimensional Bridge (MACHINE_DIM_BRIDGE): dark while the forge is idle, its four animated
                             frames and an emissive layer while it works (an active block)

The sources are in tools/textures/gtnh_dtpf/ (LGPL-3.0 or later, CREDITS-textures.md). Writes the textures, block states,
block and item models and loot tables; the lang lines, the pickaxe tag and the registry list are edited by hand
(af9-core/.../registry/AF9Blocks.java describes the steps).

Run from the repository root (needs Pillow):  python3 tools/textures/dtpf_blocks.py
"""
import json
import os
import sys

from PIL import Image

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import space_elevator as se  # noqa: E402

ROOT = se.ROOT
SRC = os.path.join(ROOT, 'tools/textures/gtnh_dtpf')
ASSETS = os.path.join(ROOT, 'af9-core/src/main/resources/assets/af9')
DATA = os.path.join(ROOT, 'af9-core/src/main/resources/data/af9')
BLOCK = os.path.join(ASSETS, 'textures/block')


def dump(path, obj):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, 'w', newline='\n') as f:
        f.write(json.dumps(obj, indent=2) + '\n')


def load(path):
    return Image.open(path).convert('RGBA')


def plain_block(name, active=False):
    """Block state, models and loot table of a casing or coil; active ones get the lit variant."""
    if active:
        dump(os.path.join(ASSETS, 'blockstates/%s.json' % name), {'variants': {
            'active=false': {'model': 'af9:block/%s' % name},
            'active=true': {'model': 'af9:block/%s_active' % name}}})
    else:
        dump(os.path.join(ASSETS, 'blockstates/%s.json' % name), {'variants': {'': {'model': 'af9:block/%s' % name}}})
    dump(os.path.join(ASSETS, 'models/item/%s.json' % name), {'parent': 'af9:block/%s' % name})
    dump(os.path.join(DATA, 'loot_tables/blocks/%s.json' % name), {
        'type': 'minecraft:block',
        'pools': [{'rolls': 1, 'entries': [{'type': 'minecraft:item', 'name': 'af9:%s' % name}],
                   'conditions': [{'condition': 'minecraft:survives_explosion'}]}]})


def cube(name, texture):
    dump(os.path.join(ASSETS, 'models/block/%s.json' % name),
         {'parent': 'minecraft:block/cube_all', 'textures': {'all': 'af9:block/%s' % texture}})


def cube_lit(name, base, lit):
    dump(os.path.join(ASSETS, 'models/block/%s.json' % name),
         {'parent': 'gtceu:block/cube_2_layer/all', 'textures': {'bot_all': 'af9:block/%s' % base,
                                                                   'top_all': 'af9:block/%s' % lit}})


def mcmeta(path, obj):
    dump(path + '.mcmeta', obj)

# the pulse, frame by frame: how far the maze is lit (0 dark, 1 white-hot), the emitter's core radius in pixels (0 none) and
# how much of its ring is lit. Nine frames, the core shrinking to nothing, a dark beat, the ring flaring, the core growing back.
PULSE = (
    (1.00, 3.0, 0.0), (0.80, 2.5, 0.0), (0.55, 1.8, 0.0), (0.30, 1.0, 0.0), (0.10, 0.0, 0.0),
    (0.00, 0.0, 0.0), (0.45, 0.0, 1.0), (0.75, 1.2, 0.8), (0.95, 2.2, 0.3),
)
ICE = (110, 225, 255)
WHITE = (240, 250, 255)
STEEL = (74, 104, 140)
DEEP = (5, 11, 26)


def mix(a, b, t):
    t = max(0.0, min(1.0, t))
    return tuple(round(a[i] + (b[i] - a[i]) * t) for i in range(3))


def injection_frame(base, lit, core, ring):
    """One frame of the injection casing: the maze (its blue lines lit by `lit`), the emitter in the middle."""
    out = base.copy()
    for y in range(16):
        for x in range(16):
            r, g, b, a = base.getpixel((x, y))
            if b > 235 and r < 120 and g > 150:       # the maze's lines
                c = mix((r, g, b), ICE if lit < 0.85 else WHITE, lit * 0.9)
                out.putpixel((x, y), (c[0], c[1], c[2], a))
            elif r < 20 and b > 150:                  # the four nodes in the corners of the tile
                c = mix((r, g, b), ICE, lit)
                out.putpixel((x, y), (c[0], c[1], c[2], a))
    cx = cy = 7.5
    for y in range(16):
        for x in range(16):
            d = ((x - cx) ** 2 + (y - cy) ** 2) ** 0.5
            if d < 3.6:                               # the emitter's well
                c = DEEP
                if d <= core:                         # the core, white-hot in the middle
                    c = mix(ICE, WHITE, 1 - d / max(core, 0.1))
                elif d >= 2.2 and ring > 0:           # the ring flaring round it
                    c = mix(DEEP, ICE, ring * 0.9)
                out.putpixel((x, y), (c[0], c[1], c[2], 255))
            elif d < 4.7:                             # its steel rim, warmed by the flare
                c = mix(STEEL, ICE, lit * 0.45)
                out.putpixel((x, y), (c[0], c[1], c[2], 255))
    return out


def injection_casing():
    base = load(os.path.join(SRC, 'MACHINE_DIM_INJECTOR.png'))
    strip = Image.new('RGBA', (16, 16 * len(PULSE)))
    for i, (lit, core, ring) in enumerate(PULSE):
        strip.paste(injection_frame(base, lit, core, ring), (0, 16 * i))
    strip.save(os.path.join(BLOCK, 'dtpf_injection_casing.png'))
    mcmeta(os.path.join(BLOCK, 'dtpf_injection_casing.png'), {'animation': {'frametime': 6, 'interpolate': True}})


def casings():
    # the hall's plating, joined
    sprite = load(os.path.join(SRC, 'MACHINE_DIM_TRANS_CASING.png'))
    se.OUT = BLOCK
    se.connected('dtpf_transcendent_casing', sprite, 2, 2)
    plain_block('dtpf_transcendent_casing')
    cube('dtpf_transcendent_casing', 'dtpf_transcendent_casing')
    # the injection casing: GTNH's maze with a field emitter in its middle that pulses
    injection_casing()
    plain_block('dtpf_injection_casing')
    cube('dtpf_injection_casing', 'dtpf_injection_casing')
    # the bridge: idle = the strip's dark third frame, active = the whole strip animated, plus its emissive layer
    strip = load(os.path.join(SRC, 'MACHINE_DIM_BRIDGE.png'))
    idle = strip.crop((0, 32, 16, 48))
    idle.save(os.path.join(BLOCK, 'dtpf_bridge_casing.png'))
    strip.save(os.path.join(BLOCK, 'dtpf_bridge_casing_active.png'))
    glow = strip.copy()
    for y in range(glow.height):
        for x in range(glow.width):
            r, g, b, a = glow.getpixel((x, y))
            if b < 180:
                glow.putpixel((x, y), (0, 0, 0, 0))
    glow.save(os.path.join(BLOCK, 'dtpf_bridge_casing_active_emissive.png'))
    for tex in ('dtpf_bridge_casing_active', 'dtpf_bridge_casing_active_emissive'):
        mcmeta(os.path.join(BLOCK, tex + '.png'), {'animation': {'frametime': 4}})
    plain_block('dtpf_bridge_casing', active=True)
    cube('dtpf_bridge_casing', 'dtpf_bridge_casing')
    cube_lit('dtpf_bridge_casing_active', 'dtpf_bridge_casing_active', 'dtpf_bridge_casing_active_emissive')


if __name__ == '__main__':
    casings()
