#!/usr/bin/env python3
"""AF9 lint for the FTB Quests chapters.

    python3 tools/lint/quests.py [repo root] [--chapter NAME ...]

Reads config/ftbquests/quests (SNBT) and the lang files, and the registry the script linter makes
(`node tools/lint/scripts.js --json`). Checks:

  Q1  an id that appears twice anywhere in the quest files (quests, tasks, rewards, links, chapters)
  Q2  an id that is not 16 hex digits of upper case, or whose first digit is above 7 (FTB Quests reads it as a signed long)
  Q3  a dependency on a quest that does not exist, on itself, or a dependency cycle
  Q4  two quests (or a quest and a link) on the same spot, or closer than 0.8
  Q5  a text key {af9...} that is in no lang file, or paragraphs of a description that skip a number
  Q6  a task that wants an item or fluid nobody defines, with a bad amount or without an id
  Q7  a quest without a task, a reward without a type, a chapter without a title
  Q8  a text key of af9.quest.* in the lang file that no quest uses (dead text)

Only the chapters AF9 wrote (their titles are {af9.quest...} keys) are checked strictly. The base pack's chapters (ATM9's, which
this repo extends here and there) are checked for Q1, Q2, for {af9...} texts and for kubejs: / af9: items; their dependencies on
chapters that are not in this repo and their GT items (GT's lists here are made from the source, not complete for blocks that
loops make) would only be noise.
"""
import json
import os
import re
import subprocess
import sys
from collections import Counter, defaultdict

ROOT = os.path.abspath(next((a for a in sys.argv[1:] if not a.startswith('--')), '.'))
HERE = os.path.dirname(os.path.abspath(__file__))
findings = []


def report(level, code, msg, where=''):
    findings.append((level, code, msg, where))


# ---- SNBT -------------------------------------------------------------------------------------------------------------
class Snbt:
    def __init__(self, text):
        self.t = text
        self.i = 0

    def ws(self):
        t = self.t
        while self.i < len(t) and (t[self.i] in ' \t\r\n,'):
            self.i += 1

    def value(self):
        self.ws()
        c = self.t[self.i]
        if c == '{':
            return self.compound()
        if c == '[':
            return self.list()
        if c == '"':
            return self.string()
        m = re.compile(r'[^\s,\]\}\[\{:"]+').match(self.t, self.i)
        self.i = m.end()
        return m.group(0)

    def string(self):
        self.i += 1
        out = []
        while self.t[self.i] != '"':
            if self.t[self.i] == '\\':
                self.i += 1
            out.append(self.t[self.i])
            self.i += 1
        self.i += 1
        return ''.join(out)

    def key(self):
        self.ws()
        if self.t[self.i] == '"':
            return self.string()
        m = re.compile(r'[A-Za-z0-9_.+-]+').match(self.t, self.i)
        self.i = m.end()
        return m.group(0)

    def compound(self):
        self.i += 1
        d = {}
        while True:
            self.ws()
            if self.t[self.i] == '}':
                self.i += 1
                return d
            k = self.key()
            self.ws()
            assert self.t[self.i] == ':', f'expected : at {self.i} after {k}'
            self.i += 1
            d[k] = self.value()

    def list(self):
        self.i += 1
        out = []
        while True:
            self.ws()
            if self.t[self.i] == ']':
                self.i += 1
                return out
            out.append(self.value())


def load_snbt(path):
    with open(path, encoding='utf-8') as f:
        text = f.read()
    return Snbt(text).value()


# ---- inputs -----------------------------------------------------------------------------------------------------------
qdir = os.path.join(ROOT, 'config/ftbquests/quests')
if not os.path.isdir(qdir):
    print('no config/ftbquests/quests here')
    sys.exit(0)


def snbt_files(sub):
    d = os.path.join(qdir, sub)
    return sorted(os.path.join(d, f) for f in os.listdir(d) if f.endswith('.snbt')) if os.path.isdir(d) else []


chapters = {}
for f in snbt_files('chapters'):
    try:
        chapters[os.path.basename(f)] = load_snbt(f)
    except Exception as e:  # noqa
        report('ERROR', 'Q7', f'{os.path.basename(f)} does not parse: {e}', f)
others = {}
for sub in ('reward_tables',):
    for f in snbt_files(sub):
        try:
            others[os.path.join(sub, os.path.basename(f))] = load_snbt(f)
        except Exception as e:  # noqa
            report('ERROR', 'Q7', f'{sub}/{os.path.basename(f)} does not parse: {e}', f)

lang = {}
for f in (os.path.join(ROOT, 'kubejs/assets/kubejs/lang/en_us.json'),):
    if os.path.exists(f):
        with open(f, encoding='utf-8') as fh:
            lang.update(json.load(fh))

# the registry of the script linter
registry = {'items': [], 'materials': [], 'machines': []}
try:
    out = subprocess.run(['node', os.path.join(HERE, 'scripts.js'), ROOT, '--json'], capture_output=True, text=True)
    registry = json.loads(out.stdout)['registry']
except Exception as e:  # noqa
    report('WARN', 'Q6', f'the script linter did not give its registry ({e}); item ids are not checked')
kube_items = {i['id'] for i in registry['items']}
materials = {m['id'] if isinstance(m, dict) else m for m in registry['materials']}
machine_ids = set()
for m in registry['machines']:
    if m['tiers']:
        from_tier = ['ulv', 'lv', 'mv', 'hv', 'ev', 'iv', 'luv', 'zpm', 'uv', 'uhv', 'uev', 'uiv', 'uxv', 'opv', 'max']
        for t in m['tiers']:
            try:
                machine_ids.add(f"{from_tier[int(t)]}_{m['id']}")
            except (ValueError, IndexError):
                machine_ids.add(m['id'])
    machine_ids.add(m['id'])


def read_lines(p):
    try:
        with open(p, encoding='utf-8') as f:
            return {l.strip() for l in f if l.strip() and not l.startswith('#')}
    except OSError:
        return set()


DATA = os.path.join(HERE, 'data')
gt_materials = read_lines(os.path.join(DATA, 'gt-materials.txt'))
gt_names = read_lines(os.path.join(DATA, 'gt-names.txt'))
pack = read_lines(os.path.join(DATA, 'pack.txt'))
gt_patterns = [re.compile(x) for x in read_lines(os.path.join(DATA, 'gt-patterns.txt'))]
tier_prefix = ('ulv', 'lv', 'mv', 'hv', 'ev', 'iv', 'luv', 'zpm', 'uv', 'uhv', 'uev', 'uiv', 'uxv', 'opv', 'max')
SHAPES = sorted(['dust', 'small_dust', 'tiny_dust', 'ingot', 'hot_ingot', 'plate', 'rod', 'long_rod', 'foil', 'gear',
                 'small_gear', 'nugget', 'block', 'bolt', 'screw', 'ring', 'frame', 'gem', 'lens', 'rotor', 'spring',
                 'wire', 'single_wire', 'double_wire', 'quadruple_wire', 'octal_wire', 'hex_wire', 'single_cable',
                 'double_cable', 'quadruple_cable', 'octal_cable', 'hex_cable', 'crushed', 'pure_dust', 'impure_dust',
                 'raw', 'dense_plate', 'round', 'bucket'], key=len, reverse=True)


def material_of(name):
    n = name
    for p in ('small_', 'tiny_', 'fine_', 'long_', 'dense_', 'hot_', 'raw_', 'crushed_', 'purified_', 'impure_', 'pure_'):
        if n.startswith(p):
            n = n[len(p):]
            break
    for s in SHAPES:
        if n.endswith('_' + s):
            return n[:-len(s) - 1]
    return n


def strip_tier(name):
    for t in tier_prefix:
        if name.startswith(t + '_'):
            return name[len(t) + 1:]
    return name


# af9 items from the Java sources
af9_items = set()
for dp, _, fs in os.walk(os.path.join(ROOT, 'af9-core/src/main/java')):
    for fn in fs:
        if fn.endswith('.java'):
            with open(os.path.join(dp, fn), encoding='utf-8') as f:
                text = f.read()
            af9_items |= set(re.findall(r'(?:ITEMS|BLOCKS)\.register\("([a-z0-9_]+)"', text))
            if fn == 'ComputeCard.java':
                af9_items |= {m.lower() + '_card' for m in re.findall(r'^\s+([A-Z_]+)\(\d+, Kind\.', text, re.M)}
mdir = os.path.join(ROOT, 'af9-core/src/main/resources/assets/af9/models/item')
if os.path.isdir(mdir):
    af9_items |= {f[:-5] for f in os.listdir(mdir) if f.endswith('.json')}


def item_exists(rid):
    ns, _, name = rid.partition(':')
    if rid in pack:
        return True
    if ns == 'kubejs':
        return name in kube_items or name in materials
    if ns == 'af9':
        return name in af9_items
    if ns == 'gtceu':
        base = strip_tier(name)
        mat = material_of(name)
        return (any(r.fullmatch(name) for r in gt_patterns) or name in gt_names or base in gt_names or name in gt_materials or name in materials or mat in gt_materials
                or mat in materials or material_of(base) in gt_materials or material_of(base) in materials
                or name in machine_ids or base in machine_ids)
    return True   # other mods: not checked


def fluid_exists(rid):
    ns, _, name = rid.partition(':')
    if ns == 'gtceu':
        return name in gt_materials or name in materials or name in gt_names or rid in pack
    return True


# ---- quests of every chapter ------------------------------------------------------------------------------------------
all_ids = Counter()
quest_by_id = {}
quest_chapter = {}
quest_deps = {}
foreign_missing = Counter()
used_keys = set()
af9_chapters = set()


def walk_ids(node, where):
    if isinstance(node, dict):
        # an `id` with a colon is an item or tag of an icon, a filter or a stack, not an id of the quest file
        if 'id' in node and isinstance(node['id'], str) and ':' not in node['id']:
            all_ids[node['id']] += 1
            if not re.fullmatch(r'[0-9A-F]{16}', node['id']):
                report('ERROR', 'Q2', f'id {node["id"]} is not 16 upper-case hex digits', where)
            elif int(node['id'][0], 16) > 7:
                report('ERROR', 'Q2', f'id {node["id"]} starts above 7: FTB Quests reads it as a negative long', where)
        for v in node.values():
            walk_ids(v, where)
    elif isinstance(node, list):
        for v in node:
            walk_ids(v, where)


for fname, ch in chapters.items():
    walk_ids(ch, fname)
    quests = ch.get('quests', [])
    text_keys = [ch.get('title', '')] + [q.get('title', '') for q in quests]
    if any(isinstance(t, str) and t.startswith('{af9.') for t in text_keys):
        af9_chapters.add(fname)
    for q in quests:
        quest_by_id[q.get('id')] = q
        quest_chapter[q.get('id')] = fname
for fname, ch in others.items():
    walk_ids(ch, fname)

for i, n in all_ids.items():
    if n > 1:
        report('ERROR', 'Q1', f'id {i} is used {n} times', '')

# links count as quests for the positions
positions = defaultdict(list)
for fname, ch in chapters.items():
    for q in ch.get('quests', []):
        try:
            positions[fname].append((float(q['x'].rstrip('dD')), float(q['y'].rstrip('dD')), 'quest ' + str(q.get('title', q.get('id'))),
                                     str(q.get('title', '')).startswith('{af9.')))
        except (KeyError, ValueError):
            report('ERROR', 'Q4', f'quest {q.get("id")} has no position', fname)
    for l in ch.get('quest_links', []):
        try:
            positions[fname].append((float(l['x'].rstrip('dD')), float(l['y'].rstrip('dD')), 'link ' + str(l.get('linked_quest')), False))
        except (KeyError, ValueError):
            pass
    for l in ch.get('quest_links', []):
        if l.get('linked_quest') not in quest_by_id:
            report('ERROR', 'Q3', f'link {l.get("id")} points to quest {l.get("linked_quest")} that does not exist', fname)

for fname, pts in positions.items():
    if fname not in af9_chapters:
        continue
    for a in range(len(pts)):
        for b in range(a + 1, len(pts)):
            dx, dy = pts[a][0] - pts[b][0], pts[a][1] - pts[b][1]
            if not (pts[a][3] or pts[b][3]):
                continue   # two quests or links of the base pack: its business
            if (dx * dx + dy * dy) ** 0.5 < 0.8:
                report('WARN', 'Q4', f'{pts[a][2]} and {pts[b][2]} are on the same spot or touch ({pts[a][0]},{pts[a][1]})', fname)

# dependencies
for qid, q in quest_by_id.items():
    deps = q.get('dependencies', [])
    if isinstance(deps, str):
        deps = [deps]
    quest_deps[qid] = deps
    for d in deps:
        if d == qid:
            report('ERROR', 'Q3', f'quest {qid} depends on itself', quest_chapter[qid])
        elif d not in quest_by_id:
            # a dependency on a link is also allowed
            linked = any(l.get('id') == d for ch in chapters.values() for l in ch.get('quest_links', []))
            if not linked:
                if quest_chapter[qid] in af9_chapters:
                    report('ERROR', 'Q3', f'quest {qid} ({q.get("title", "")}) depends on {d}, which does not exist', quest_chapter[qid])
                else:
                    foreign_missing[quest_chapter[qid]] += 1
for ch_name, n in foreign_missing.items():
    report('INFO', 'Q3', f'{n} dependencies of the base pack chapter lead to quests of chapters that are not in this repo', ch_name)
state = {}


def dfs(n, path):
    state[n] = 1
    for d in quest_deps.get(n, []):
        if d not in quest_by_id:
            continue
        if state.get(d) == 1:
            report('ERROR', 'Q3', f'dependency cycle: {" -> ".join(path + [n, d])}', quest_chapter.get(n, ''))
        elif d not in state:
            dfs(d, path + [n])
    state[n] = 2


for n in quest_by_id:
    if n not in state:
        dfs(n, [])

# texts and tasks
for fname, ch in chapters.items():
    quests = ch.get('quests', [])
    mine = fname in af9_chapters
    if mine and not ch.get('title'):
        report('ERROR', 'Q7', 'the chapter has no title', fname)
    for q in quests:
        label = f'{fname}: {q.get("title", q.get("id"))}'
        tasks = q.get('tasks', [])
        if not tasks:
            report('ERROR', 'Q7', 'quest without a task', label)
        for t in tasks:
            if not isinstance(t, dict):
                continue
            ty = t.get('type')
            if ty == 'item':
                item = t.get('item')
                rid = item if isinstance(item, str) else (item or {}).get('id') if isinstance(item, dict) else None
                if not rid:
                    report('ERROR', 'Q6', 'item task without an item', label)
                elif not item_exists(rid) and (mine or rid.split(':')[0] in ('kubejs', 'af9')):
                    report('ERROR', 'Q6', f'item task {rid} does not exist (not in the scripts, AF9 Core, GT or the pack lists)', label)
            elif ty == 'fluid':
                f = t.get('fluid')
                if not f:
                    report('ERROR', 'Q6', 'fluid task without a fluid', label)
                elif not fluid_exists(f) and mine:
                    report('ERROR', 'Q6', f'fluid task {f} does not exist', label)
                amount = t.get('amount')
                if amount is not None and not re.fullmatch(r'\d+[Ll]?', str(amount)):
                    report('ERROR', 'Q6', f'fluid task amount {amount} is not a number', label)
        for r in q.get('rewards', []):
            if isinstance(r, dict) and 'type' not in r:
                report('ERROR', 'Q7', 'reward without a type', label)
        if True:
            keys = []
            for field in ('title', 'subtitle'):
                v = q.get(field)
                if isinstance(v, str) and v.startswith('{'):
                    keys.append(v.strip('{}'))
            desc = q.get('description', [])
            paras = []
            for line in desc:
                if isinstance(line, str) and line.startswith('{') and line.endswith('}'):
                    keys.append(line.strip('{}'))
                    paras.append(line.strip('{}'))
            for k in keys:
                used_keys.add(k)
                if k.startswith('af9.') and k not in lang:
                    report('ERROR', 'Q5', f'text key {k} is in no lang file', label)
            nums = []
            for k in paras:
                m = re.search(r'\.(\d+)$', k)
                if m:
                    nums.append(int(m.group(1)))
            if str(q.get('title', '')).startswith('{af9.') and nums and nums != list(range(nums[0], nums[0] + len(nums))):
                report('WARN', 'Q5', f'description paragraphs are numbered {nums}', label)

# dead texts
for k in lang:
    if k.startswith('af9.quest.') and k not in used_keys and not k.startswith('af9.quest.ae2'):
        # chapter texts and keys of quests that are not in an AF9 chapter are used by other chapters; only report litho/*
        if k.startswith('af9.quest.litho.') and not k.endswith(('.chapter.title', '.chapter.subtitle')):
            report('INFO', 'Q8', f'text {k} is used by no quest', 'kubejs lang')

# ---- output -----------------------------------------------------------------------------------------------------------
order = {'ERROR': 0, 'WARN': 1, 'INFO': 2}
findings.sort(key=lambda f: (order[f[0]], f[1]))
total_q = sum(len(c.get('quests', [])) for c in chapters.values())
print(f'quests: {len(chapters)} chapters, {total_q} quests, {len(af9_chapters)} of them AF9 chapters')
print(f'{sum(f[0] == "ERROR" for f in findings)} errors, {sum(f[0] == "WARN" for f in findings)} warnings, '
      f'{sum(f[0] == "INFO" for f in findings)} notes')
for level, code, msg, where in findings:
    print(f'{level:5} {code} {msg}' + (f'  [{where}]' if where else ''))
sys.exit(1 if any(f[0] == 'ERROR' for f in findings) else 0)
