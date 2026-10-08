#!/usr/bin/env python3
"""AF9 lint for textures, models and language files.

    python3 tools/lint/assets.py [repo root]

Reads the registry the script linter makes (`node tools/lint/scripts.js --json`), the list of what AF9 Core registers
(tools/lint/data/af9-registry.txt), the KubeJS assets, the AF9 Core resources and the AF9 Core Java sources. Checks:

  A2  a texture that is not a square (or a stack of squares when it has an .mcmeta), an .mcmeta that is not valid, whose
      frametime is not a positive integer, whose `frames` point past the end of the strip, or that has no texture beside it
  A3  an item or block texture nobody references (dead file) - a note, since Java and other mods may use it
  A4  a lang file that is not valid JSON, a key twice in one file, a value that is not a string, a `&` colour code that no
      Minecraft formatting knows, a `%s`/`%d` in a value of a key whose Java use passes no argument is NOT checked
  A5  a machine, material or recipe type without a name (no langValue / lang key)
  A6  a model of AF9 Core that points to a texture or a parent that does not exist, a blockstate that points to a model
      that does not exist, an item or block AF9 Core registers without model, name, blockstate or loot table, an item
      model or a blockstate of something that is not registered
  A7  a `Component.translatable("af9...")` key in the Java sources that is in no lang file
  A8  a lang file with different styles of indentation (kubejs lang uses tabs, af9 / gtceu lang four spaces)

Exit code 1 when there is an error.
"""
import json
import os
import re
import struct
import subprocess
import sys

import af9registry

ROOT = os.path.abspath(next((a for a in sys.argv[1:] if not a.startswith('--')), '.'))
HERE = os.path.dirname(os.path.abspath(__file__))
findings = []


def report(level, code, msg, where=''):
    findings.append((level, code, msg, where))


def rel(p):
    return os.path.relpath(p, ROOT)


# ---- helpers ----------------------------------------------------------------------------------------------------------
def png_size(path):
    with open(path, 'rb') as f:
        head = f.read(24)
    if head[:8] != b'\x89PNG\r\n\x1a\n':
        return None
    return struct.unpack('>II', head[16:24])


def load_lang(path):
    """The pairs of a lang file; reports invalid JSON and duplicate keys."""
    pairs = []

    def hook(items):
        pairs.extend(items)
        return dict(items)

    try:
        with open(path, encoding='utf-8') as f:
            text = f.read()
        data = json.loads(text, object_pairs_hook=hook)
    except (OSError, ValueError) as e:
        report('ERROR', 'A4', f'not valid JSON: {e}', rel(path))
        return {}, ''
    seen = {}
    for k, v in pairs:
        if not isinstance(v, str):
            report('ERROR', 'A4', f'value of "{k}" is not a string', rel(path))
        if k in seen and seen[k] != v:
            report('ERROR', 'A4', f'key "{k}" twice with different values', rel(path))
        elif k in seen:
            report('WARN', 'A4', f'key "{k}" twice (same value)', rel(path))
        seen[k] = v
        if isinstance(v, str):
            for m in re.finditer(r'&(\S)', v):
                if m.group(1) not in '0123456789abcdefklmnor':
                    report('WARN', 'A4', f'"{k}": unknown colour code &{m.group(1)}', rel(path))
                    break
            # Minecraft reads %s, %1$s and %% only: any other `%` + letter (a `%d`, a `%f`, a `%` at the end) makes the text
            # fail to translate and shows the key instead; `% ` (a percent sign with a space) is plain text
            rest = re.sub(r'%(?:\d+\$)?s|%%', '', v)
            if re.search(r'%(?:\d+\$)?[A-Za-z]|%$', rest):
                report('ERROR', 'A4', f'"{k}": a format code Minecraft does not know in "{v[:60]}" (only %s, %1$s and %% work)', rel(path))
    return data, text


# ---- the registry of the scripts -------------------------------------------------------------------------------------
try:
    out = subprocess.run(['node', os.path.join(HERE, 'scripts.js'), ROOT, '--json'], capture_output=True, text=True)
    registry = json.loads(out.stdout)['registry']
except (OSError, ValueError, KeyError):
    registry = {'items': [], 'machines': [], 'recipeTypes': [], 'materials': []}
    report('ERROR', 'A5', 'the script linter gave no registry (node tools/lint/scripts.js --json failed): KubeJS items, machines and materials are not checked')

KJS = os.path.join(ROOT, 'kubejs/assets')
# a checkout of GT's source (GT_SRC=... or --gt DIR) lets the textures of GT in a model be checked; without it they are not
GT_SRC = os.environ.get('GT_SRC') or next((sys.argv[i + 1] for i, a in enumerate(sys.argv) if a == '--gt' and i + 1 < len(sys.argv)), None)
GT_TEX = os.path.join(GT_SRC, 'src/main/resources/assets/gtceu/textures') if GT_SRC else None
AF9 = os.path.join(ROOT, 'af9-core/src/main/resources/assets/af9')

lang = {}
lang_files = {}
for p in [os.path.join(KJS, 'kubejs/lang/en_us.json'), os.path.join(KJS, 'gtceu/lang/en_us.json'),
          os.path.join(AF9, 'lang/en_us.json')]:
    if os.path.exists(p):
        data, text = load_lang(p)
        lang_files[p] = data
        lang.update(data)
        # A8 indentation
        indents = {('tab' if l.startswith('\t') else 'space') for l in text.splitlines() if l[:1] in ' \t' and l.strip()}
        if len(indents) > 1:
            report('INFO', 'A8', 'tabs and spaces are mixed in the indentation (harmless; new lines follow the file around them)', rel(p))

# ---- A2 / A3 textures ---------------------------------------------------------------------------------------------------
referenced = set()


def tex_path(ns, path):
    """The file of a texture id in this repo (None: a namespace that is not in this repo)."""
    if ns == 'kubejs':
        return os.path.join(KJS, ns, 'textures', path + '.png')
    if ns == 'gtceu':
        own = os.path.join(KJS, ns, 'textures', path + '.png')
        if os.path.exists(own):
            return own
        return os.path.join(GT_TEX, path + '.png') if GT_TEX else None
    if ns == 'af9':
        return os.path.join(AF9, 'textures', path + '.png')
    return None


def base_pack_file(path):
    """A script that carries the AllTheMods licence header is the base pack's: its textures are the pack's."""
    try:
        with open(os.path.join(ROOT, path), encoding='utf-8') as f:
            head = f.read(600)
    except OSError:
        return False
    return 'AllTheMods' in head or 'All Rights Reserved' in head


# what names a texture of AF9 Core: its models, and the sources and scripts (a machine's casing, an overlay, a renderer)
AF9_TEX = os.path.join(AF9, 'textures')
named_in_code = []
for top, ext in (('af9-core/src/main/java', '.java'), ('kubejs', '.js')):
    for dp, _, fs in os.walk(os.path.join(ROOT, top)):
        for fn in fs:
            if fn.endswith(ext):
                with open(os.path.join(dp, fn), encoding='utf-8') as f:
                    named_in_code.append(f.read())
named_in_code = '\n'.join(named_in_code)
for dp, _, fs in os.walk(os.path.join(AF9, 'models')):
    for fn in fs:
        if fn.endswith('.json'):
            try:
                with open(os.path.join(dp, fn), encoding='utf-8') as f:
                    textures = (json.load(f).get('textures') or {}).values()
            except (OSError, ValueError):
                continue   # A6 reports it
            for t in textures:
                if isinstance(t, str) and t.startswith('af9:'):
                    referenced.add(os.path.normpath(tex_path('af9', t[4:])))


def is_referenced(p):
    """A texture a model or an .mcmeta names, a `_bloom` / `_ctm` / `_emissive` layer of one, or one the code names."""
    if os.path.normpath(p) in referenced:
        return True
    base = re.sub(r'_(bloom|ctm|emissive)\.png$', '.png', p)
    if base != p and os.path.exists(base):
        return True
    if p.startswith(AF9_TEX):
        path = os.path.relpath(p, AF9_TEX).replace(os.sep, '/')[:-4]
        # named whole ("af9:block/machines/x"), or put together from a folder and a name ("block/coils/" + id)
        return path in named_in_code or os.path.basename(path) in named_in_code
    ns, _, path = os.path.relpath(p, KJS).replace(os.sep, '/')[:-4].partition('/textures/')
    folder = path.rpartition('/')[0]
    # a machine's overlay folder ("gtceu:block/multiblock/void_miner_mk2": GT reads its overlay_front*.png)
    if folder and re.search(r'["\']' + re.escape(f'{ns}:{folder}') + r'["\']', named_in_code):
        return True
    # a material's own fluid texture: FluidBuilder().customStill() reads gtceu:block/fluids/fluid.<material>
    fluid = re.fullmatch(r'block/fluids/fluid\.(\w+)', path)
    return bool(fluid and re.search(r'material\("' + fluid.group(1) + r'"\)\s*\.\w+\(new FluidBuilder\(\)\.customStill\(\)',
                                    named_in_code))


# every png under the kubejs assets and among AF9 Core's textures
for dp, _, fs in [w for top in (KJS, AF9_TEX) for w in os.walk(top)]:
    for fn in sorted(fs):
        p = os.path.join(dp, fn)
        if fn.endswith('.png'):
            size = png_size(p)
            if size is None:
                report('ERROR', 'A2', 'not a PNG file', rel(p))
                continue
            w, h = size
            meta = p + '.mcmeta'
            if os.path.exists(meta):
                try:
                    with open(meta, encoding='utf-8') as f:
                        mc = json.load(f)
                except ValueError as e:
                    report('ERROR', 'A2', f'.mcmeta is not valid JSON: {e}', rel(meta))
                    continue
                for k, v in (mc.get('ldlib') or {}).items():
                    # LDLib's connected textures (`connection`) and the like: the texture they name has to exist
                    if isinstance(v, str) and ':' in v:
                        tp = tex_path(*v.split(':', 1))
                        if tp is not None and not os.path.exists(tp):
                            report('ERROR', 'A2', f'ldlib {k} points to {v}, which does not exist', rel(meta))
                        elif tp is not None:
                            referenced.add(os.path.normpath(tp))
                if 'animation' not in mc:
                    if w != h:
                        report('ERROR', 'A2', f'texture {w}x{h} is not square and its .mcmeta has no animation', rel(p))
                    continue
                anim = mc['animation']
                if h % w != 0:
                    report('ERROR', 'A2', f'animated texture {w}x{h}: the height is not a multiple of the width', rel(p))
                frames = h // w if w else 0
                ft = anim.get('frametime', 1)
                if not isinstance(ft, int) or ft < 1:
                    report('ERROR', 'A2', f'frametime {ft!r} is not a positive integer', rel(meta))
                for fr in anim.get('frames', []):
                    idx = fr.get('index') if isinstance(fr, dict) else fr
                    if not isinstance(idx, int) or idx < 0 or idx >= frames:
                        report('ERROR', 'A2', f'frame {fr!r} is outside the {frames} frames of the strip', rel(meta))
                        break
                if frames < 2:
                    report('WARN', 'A2', 'an .mcmeta animation on a texture with a single frame', rel(p))
            elif w != h:
                report('ERROR', 'A2', f'texture {w}x{h} is not square and has no .mcmeta animation', rel(p))
            elif w % 16 != 0 and w not in (8, 4):
                report('WARN', 'A2', f'texture {w}x{h}: not a multiple of 16', rel(p))
            in_af9 = p.startswith(AF9_TEX)
            # of AF9 Core's textures, the items' and blocks' (a GUI texture is named in ways this cannot follow)
            kind = os.path.relpath(p, AF9_TEX).split(os.sep)[0] if in_af9 else ''
            if (not in_af9 or kind in ('item', 'block')) and not is_referenced(p):
                report('INFO', 'A3', 'no model, script or Java source names this texture', rel(p))
        elif fn.endswith('.mcmeta') and not os.path.exists(p[:-7]):
            report('ERROR', 'A2', '.mcmeta without a texture beside it', rel(p))

# ---- A5 names -----------------------------------------------------------------------------------------------------------
TIERS = ('ulv', 'lv', 'mv', 'hv', 'ev', 'iv', 'luv', 'zpm', 'uv', 'uhv', 'uev', 'uiv', 'uxv', 'opv', 'max')
for m in registry['machines']:
    if base_pack_file(m['file']):
        continue
    ids = [m['id']] + [f'{t}_{m["id"]}' for t in TIERS]
    if not m.get('langValue') and not any(('block.gtceu.' + i) in lang for i in ids):
        report('WARN', 'A5', f'machine {m["id"]} has no name (langValue or block.gtceu.{m["id"]} in a lang file)', m['file'])
# materials: GT shows `material.gtceu.<id>` for the fluid, the dust, the cell ...: without it the name is the raw key
for mat in registry.get('materials', []):
    mid = mat['id'] if isinstance(mat, dict) else mat
    if f'material.gtceu.{mid}' not in lang:
        report('ERROR', 'A5', f'material {mid} has no name (material.gtceu.{mid} in the gtceu lang)', 'kubejs/assets/gtceu/lang/en_us.json')
for t in registry['recipeTypes']:
    if base_pack_file(t['file']):
        continue
    if not t.get('langValue') and ('gtceu.' + t['id']) not in lang:
        report('ERROR', 'A5', f'recipe type {t["id"]} has no name (gtceu.{t["id"]} in the gtceu lang)', t['file'])

# ---- A6 AF9 Core models / blockstates / names / loot -------------------------------------------------------------------
# what AF9 Core registers; a machine's block has its model, loot and name from GregTech
af9_registry = af9registry.load(HERE)
af9_items = set(af9_registry['item'])
af9_blocks = {b for b, words in af9_registry['block'].items() if 'machine' not in words}
af9_machine_blocks = set(af9_registry['block']) - af9_blocks
if not af9_items:
    report('ERROR', 'A6', 'tools/lint/data/af9-registry.txt is missing or empty: the items and blocks of AF9 Core are not checked')
java_dir = os.path.join(ROOT, 'af9-core/src/main/java')
java_text = {}
for dp, _, fs in os.walk(java_dir):
    for fn in fs:
        if fn.endswith('.java'):
            with open(os.path.join(dp, fn), encoding='utf-8') as f:
                java_text[os.path.join(dp, fn)] = f.read()


def model_file(rid):
    ns, _, path = rid.partition(':')
    if ns == 'af9':
        return os.path.join(AF9, 'models', path + '.json')
    return None


def check_model(path, what):
    try:
        with open(path, encoding='utf-8') as f:
            m = json.load(f)
    except (OSError, ValueError) as e:
        report('ERROR', 'A6', f'model is not valid JSON: {e}', rel(path))
        return
    parent = m.get('parent')
    if parent and ':' in parent and parent.startswith('af9:'):
        if not os.path.exists(model_file(parent)):
            report('ERROR', 'A6', f'parent {parent} does not exist', rel(path))
    for k, t in (m.get('textures') or {}).items():
        if not isinstance(t, str) or t.startswith('#'):
            continue
        ns, _, p = (t if ':' in t else 'minecraft:' + t).partition(':')
        f = tex_path(ns, p)
        if f is not None and not os.path.exists(f):
            report('ERROR', 'A6', f'texture {t} ({k}) does not exist', rel(path))


for dp, _, fs in os.walk(os.path.join(AF9, 'models')):
    for fn in fs:
        if fn.endswith('.json'):
            check_model(os.path.join(dp, fn), 'model')

bs_dir = os.path.join(AF9, 'blockstates')
if os.path.isdir(bs_dir):
    for fn in os.listdir(bs_dir):
        with open(os.path.join(bs_dir, fn), encoding='utf-8') as f:
            text = f.read()
        for mdl in re.findall(r'"model"\s*:\s*"([^"]+)"', text):
            mf = model_file(mdl)
            if mf and not os.path.exists(mf):
                report('ERROR', 'A6', f'blockstate points to model {mdl} that does not exist', rel(os.path.join(bs_dir, fn)))

STALE = '(or the list of what AF9 Core registers is old: tools/lint/README.md, "The registry list")'
for i in sorted(af9_items - af9_machine_blocks):
    if not os.path.exists(os.path.join(AF9, 'models/item', i + '.json')):
        report('ERROR', 'A6', f'item af9:{i} has no models/item/{i}.json', 'af9-core')
    if f'item.af9.{i}' not in lang and f'block.af9.{i}' not in lang:
        report('ERROR', 'A6', f'item af9:{i} has no name (item.af9.{i} in the af9 lang)', 'af9-core')
for b in sorted(af9_blocks):
    if f'block.af9.{b}' not in lang:
        report('ERROR', 'A6', f'block af9:{b} has no name (block.af9.{b})', 'af9-core')
    if not os.path.exists(os.path.join(AF9, 'blockstates', b + '.json')):
        report('WARN', 'A6', f'block af9:{b} has no blockstate (a custom model may do it in code)', 'af9-core')
    if not os.path.exists(os.path.join(ROOT, 'af9-core/src/main/resources/data/af9/loot_tables/blocks', b + '.json')):
        report('WARN', 'A6', f'block af9:{b} has no loot table: it drops nothing', 'af9-core')
# the other way round: a model or a blockstate of something that is not registered
item_models = os.path.join(AF9, 'models/item')
for fn in sorted(os.listdir(item_models)) if os.path.isdir(item_models) else []:
    if fn.endswith('.json') and fn[:-5] not in af9_items:
        report('WARN', 'A6', f'models/item/{fn}: AF9 Core registers no item af9:{fn[:-5]} {STALE}', 'af9-core')
for fn in sorted(os.listdir(bs_dir)) if os.path.isdir(bs_dir) else []:
    if fn.endswith('.json') and fn[:-5] not in af9_registry['block']:
        report('WARN', 'A6', f'blockstates/{fn}: AF9 Core registers no block af9:{fn[:-5]} {STALE}', 'af9-core')

# ---- A7 translation keys of the Java sources ----------------------------------------------------------------------------
for fn, text in sorted(java_text.items()):
    for m in re.finditer(r'(?:translatable|literal)?\(\s*"(af9\.[a-z0-9_.]+)"', text):
        key = m.group(1)
        if text[m.start():m.start() + 12].startswith('literal'):
            continue
        if key.endswith('.') or key not in lang:
            # keys built with a suffix at run time: `"af9.x." + n` ends with a dot, and the lang file has the numbered keys
            if key.endswith('.') and any(k.startswith(key) for k in lang):
                continue
            report('ERROR', 'A7', f'translation key {key} is in no lang file', rel(fn))

# ---- output ----------------------------------------------------------------------------------------------------------------
order = {'ERROR': 0, 'WARN': 1, 'INFO': 2}
findings.sort(key=lambda f: (order[f[0]], f[1], f[2]))
print(f'assets: {len(af9_items)} AF9 items, {len(af9_registry["block"])} AF9 blocks, {len(lang)} lang keys')
count = lambda l: sum(1 for f in findings if f[0] == l)
print(f'{count("ERROR")} errors, {count("WARN")} warnings, {count("INFO")} notes')
for level, code, msg, where in findings:
    print(f'{level:5} {code} {msg}' + (f'  [{where}]' if where else ''))
sys.exit(1 if count('ERROR') else 0)
