#!/usr/bin/env python3
"""The blocks of the rebuilt Dimensionally Transcendent Plasma Forge: three casings that are GTNH's own, and two coils.

  dtpf_transcendent_casing   GT5-Unofficial's Dimensionally Transcendent Casing (MACHINE_DIM_TRANS_CASING), the hall's
                             plating; joins its neighbours (LDLib connected texture)
  dtpf_injection_casing      the Dimensional Injection Casing (MACHINE_DIM_INJECTOR): the front deck, where the hatches go
  dtpf_bridge_casing         the Dimensional Bridge (MACHINE_DIM_BRIDGE): dark while the forge is idle, its four animated
                             frames and an emissive layer while it works (an active block)
  dtpf_containment_coil      the Endion coil's weave turned to the DTPF's blue; glows while the forge works
  dtpf_resonance_coil        the same in ice cyan and a brighter bloom

The sources are in tools/textures/gtnh_dtpf/ (LGPL-3.0 or later, CREDITS-textures.md). Writes the textures, block states,
block and item models and loot tables; the lang lines, the pickaxe tag and the registry list are edited by hand
(af9-core/.../registry/AF9Blocks.java describes the steps).

Run from the repository root (needs Pillow):  python3 tools/textures/dtpf_blocks.py
"""
import colorsys
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
COILS = os.path.join(BLOCK, 'coils')


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


def casings():
    # the hall's plating, joined
    sprite = load(os.path.join(SRC, 'MACHINE_DIM_TRANS_CASING.png'))
    se.OUT = BLOCK
    se.connected('dtpf_transcendent_casing', sprite, 2, 2)
    plain_block('dtpf_transcendent_casing')
    cube('dtpf_transcendent_casing', 'dtpf_transcendent_casing')
    # the injection casing, as it is
    load(os.path.join(SRC, 'MACHINE_DIM_INJECTOR.png')).save(os.path.join(BLOCK, 'dtpf_injection_casing.png'))
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


def retint(im, hue, sat, val):
    """The coil's weave (everything with colour in it) moved to a hue; the grey frame stays."""
    out = im.copy()
    for y in range(out.height):
        for x in range(out.width):
            r, g, b, a = out.getpixel((x, y))
            h, s, v = colorsys.rgb_to_hsv(r / 255, g / 255, b / 255)
            if s < 0.18:
                continue
            r, g, b = colorsys.hsv_to_rgb(hue, min(1.0, s * sat), min(1.0, v * val))
            out.putpixel((x, y), (round(r * 255), round(g * 255), round(b * 255), a))
    return out


def coils():
    se.OUT = COILS
    for name, src, hue, sat, val in (('dtpf_containment_coil', 'endion_coil', 0.60, 1.0, 1.0),
                                     ('dtpf_resonance_coil', 'resonant_endion_coil', 0.50, 0.9, 1.05)):
        sprite = retint(load(os.path.join(COILS, src + '.png')), hue, sat, val)
        bloom = retint(load(os.path.join(COILS, src + '_bloom.png')), hue, sat, val)
        for tex, im in ((name, sprite), (name + '_bloom', bloom)):
            se.save(im, tex)
            se.save(se.ctm_sheet(im, 2, 2), tex + '_ctm')
            mcmeta(os.path.join(COILS, tex + '.png'), {'ldlib': {'connection': 'af9:block/coils/%s_ctm' % tex}})
        plain_block(name, active=True)
        dump(os.path.join(ASSETS, 'models/block/%s.json' % name),
             {'parent': 'minecraft:block/cube_all', 'textures': {'all': 'af9:block/coils/%s' % name}})
        cube_lit(name + '_active', 'coils/' + name, 'coils/' + name + '_bloom')


if __name__ == '__main__':
    casings()
    coils()
