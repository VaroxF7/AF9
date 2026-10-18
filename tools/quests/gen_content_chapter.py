#!/usr/bin/env python3
"""Writes the Content chapter of the quest book: the superalloys (every part, per alloy) and every material AF9 adds.

    python3 tools/quests/gen_content_chapter.py [--force]

Reads af9-core/.../AF9Materials.java (which method registers a material, in which form) and tools/lint/data/af9-registry.txt;
writes config/ftbquests/quests/chapters/content.snbt and adds its texts to kubejs/assets/kubejs/lang/en_us.json.
The chapter is edited in game, so a second run refuses to overwrite it (--force does).
"""
import hashlib
import json
import os
import re
import sys

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), '../..')
JAVA = os.path.join(ROOT, 'af9-core/src/main/java/com/af9/core/registry/AF9Materials.java')
REG = os.path.join(ROOT, 'tools/lint/data/af9-registry.txt')
OUT = os.path.join(ROOT, 'config/ftbquests/quests/chapters/content.snbt')
LANG = os.path.join(ROOT, 'kubejs/assets/kubejs/lang/en_us.json')
GROUP = '1DA67E79B40AB130'

SECTIONS = [
    ('fab', 'Fab Chemistry', 'Silicon, fluorochemicals, air gases and the resists of the fab line', ['fabChemistry'],
     'gtceu:high_purity_quartz_dust'),
    ('litho', 'Photolithography', 'Litho process chemicals, coating and development of the wafer line',
     ['lithoProcess', 'photolithography'], 'gtceu:krf_photoresist_bucket'),
    ('circuit', 'Circuit Metals', 'Moon silicon, bond wire and pin alloys, solders',
     ['electronicsMetallurgy', 'moonSilicon', 'solders'], 'gtceu:aluminium_tranquillite_ingot'),
    ('alloys', 'Alloys and Exotic Metals', 'The 15 alloys, rhenium and hafnium chains, ceranium, quantanium, UHV metals',
     ['alloys', 'annealedBronze', 'ceranium', 'orbitalStructureAlloy', 'starMatterSteel', 'quantanium',
      'uhvSuperconductor'], 'gtceu:rheniloy_ingot'),
    ('pgm', 'Platinum Group', 'Matte, sulfate leach, chlorides and the seven metals', ['platinumGroup'],
     'gtceu:platinum_ingot'),
    ('fission', 'Fission and Waste', 'Brannerite, uranium chain, reactor fuel and vitrified waste',
     ['asteroidFission', 'vitrifiedWaste'], 'gtceu:uranium_ingot'),
    ('fuels', 'Oil, Fuels and Brines', 'Oil, diesel, biofuels, aromatics, lapis sulfonate, lithium brine, water filtration',
     ['oil', 'diesel', 'biofuels', 'aromatics', 'lapisSulfonate', 'lithiumBrine', 'waterFiltration'],
     'gtceu:diesel_bucket'),
    ('cryo', 'Cryogenics', 'Dense and supercooled gases of the Supercooling Cryostat', ['cryogenics'],
     'gtceu:supercooled_argon_bucket'),
    ('physics', 'Particle Physics', 'The accelerator and antimatter', ['particleAccelerator', 'antimatter'],
     'gtceu:strange_matter_bucket'),
    ('misc', 'Planets, Rockets and More', 'Planet metals, rocket insulation, motors, cultures, quartz, boules',
     ['planetMetals', 'rocketInsulation', 'aeQuartz', 'bioCultures', 'motorParts', 'bouleMelting'],
     'gtceu:rocket_insulation_dust'),
]

# the alloys: id, name, short description, (parts as item id patterns)
ALLOYS = [
    ('inconel_625', 'Inconel 625', 'Ni 60, Cr 20, Mo 10, Fe 5, Nb 5 (20 dust in the mixer). EBF 3,300 K, HV, argon, Nichrome coil. Rotor: power 170, efficiency 150, 2,400 durability.'),
    ('inconel_718', 'Inconel 718', 'Ni 50, Cr 20, Fe 20, Nb 5, Mo 5 (20 dust in the mixer). EBF 3,600 K, EV, helium, Nichrome coil. Rotor: power 200, efficiency 165, 3,600 durability.'),
    ('inconel_738', 'Inconel 738', '15 nichrome, 2 Co, 1 W, 1 Ti, 1 Al (20 dust in the mixer). EBF 4,200 K, IV, helium, RTM alloy coil. The turbine blade alloy: rotor power 260, efficiency 190, 5,600 durability.'),
    ('havar', 'Havar', 'Co 40, Cr 20, Ni 15, Fe 15, W 10 (20 dust in the mixer). EBF 2,900 K, HV, argon, Nichrome coil. Rotor: power 150, efficiency 125, 1,800 durability.'),
    ('kovar', 'Kovar', 'Fe 6, Ni 3, Co 2: the circuit pin alloy, now with parts and a rotor. EBF 1,720 K, MV, nitrogen. Rotor: power 110, efficiency 120, 600 durability.'),
    ('hastelloy_c_276', 'Hastelloy C-276', "GT's Hastelloy (Ni 12, Mo 8, Cr 7, W, Co, Cu): kept as it is, plus dense plate, fine wire, screw and a rotor. EBF 3,800 K, EV, helium, RTM alloy coil. Rotor: power 190, efficiency 175, 5,000 durability."),
    ('stellite_1', 'Stellite 1', 'Co 50, Cr 30, W 15, C 5 (20 dust in the mixer). EBF 3,100 K, HV, argon, Nichrome coil. Rotor: power 160, efficiency 120, 3,200 durability.'),
    ('stellite_12', 'Stellite 12', 'Co 55, Cr 30, W 10, C 5 (20 dust in the mixer). EBF 3,400 K, EV, argon, Nichrome coil. Rotor: power 175, efficiency 140, 4,200 durability.'),
]
PARTS = [('{a}_ingot', 1), ('{a}_plate', 1), ('double_{a}_plate', 1), ('dense_{a}_plate', 1), ('fine_{a}_wire', 1),
         ('{a}_screw', 1), ('{a}_rotor', 1)]
PART_NAMES = ['Ingot', 'Plate', 'Double Plate', 'Dense Plate', 'Fine Wire', 'Screw', 'Turbine Rotor']

# materials already shown elsewhere in the chapter
SKIP = {'kovar', 'inconel_625', 'inconel_718', 'inconel_738', 'havar', 'stellite_1', 'stellite_12'}


def hid(*parts):
    h = hashlib.md5('|'.join(parts).encode()).hexdigest()[:16].upper()
    return str(int(h[0], 16) & 7) + h[1:]  # first digit 0-7: FTB Quests reads 8-F as a negative long


def parse():
    src = open(JAVA, encoding='utf-8').read()
    reg = []
    for line in open(REG, encoding='utf-8'):
        if line.startswith('material '):
            reg.append(line.split(':', 1)[1].split()[0].strip())
    res = {}
    cur = '?'
    helpers = [('dust', 'dust'), ('roughDust', 'dust'), ('metal', 'ingot'), ('liquid', 'fluid'), ('gas', 'fluid'),
               ('ebfAlloy', 'ingot'), ('superalloy', 'ingot')]
    for chunk in re.split(r'(?=\n    (?:private|public) static )', src):
        mm = re.match(r'\n    (?:private|public) static [\w.<>]+ (\w+)\(', chunk)
        if mm:
            cur = mm.group(1)
        for h, form in helpers:
            for x in re.finditer(r'(?<![\w.])%s\(\s*"(\w+)"' % h, chunk):
                res.setdefault(x.group(1), (cur, form))
        for x in re.finditer(r'material\("(\w+)"\)(.*?);', chunk, re.S):
            body = x.group(2)
            if '.ingot(' in body:
                form = 'ingot'
            elif '.dust(' in body:
                form = 'dust'
            elif '.plasma(' in body:
                form = 'plasma'
            elif re.search(r'\.(liquid|gas|fluid)\(', body):
                form = 'fluid'
            else:
                form = 'dust'
            res[x.group(1)] = (cur, form)
    for gas in ('hydrogen', 'argon', 'xenon', 'endion'):
        for pre in ('dense', 'supercooled'):
            res['%s_%s' % (pre, gas)] = ('cryogenics', 'fluid')
    for el in ('rhenium', 'hafnium'):
        res[el] = ('alloys', 'ingot')
    return res


def title(mat):
    return ' '.join(w.capitalize() for w in mat.split('_'))


def task(mat, form):
    if form == 'ingot':
        return {'type': 'item', 'item': 'gtceu:%s_ingot' % mat}
    if form == 'dust':
        return {'type': 'item', 'item': 'gtceu:%s_dust' % mat}
    if form == 'plasma':
        return {'type': 'fluid', 'fluid': 'gtceu:%s_plasma' % mat}
    return {'type': 'fluid', 'fluid': 'gtceu:%s' % mat}


def icon_of(t):
    return t['item'] if t['type'] == 'item' else t['fluid']


def snbt_task(owner, t):
    tid = hid(owner, t.get('item') or t.get('fluid'))
    if t['type'] == 'item':
        return '{ id: "%s" item: "%s" type: "item" }' % (tid, t['item'])
    return '{ amount: 1000L fluid: "%s" id: "%s" type: "fluid" }' % (t['fluid'], tid)


class Chapter:
    def __init__(self):
        self.quests = []
        self.lang = {}

    def quest(self, key, tasks, x, y, deps=(), size=1.0, shape='', title=None, subtitle=None, desc=None, icon=None, xp=10):
        qid = hid('quest', key)
        lines = ['\t\t{']
        if desc:
            lines.append('\t\t\tdescription: [%s]' % ' '.join('"{af9.quest.content.%s.d%d}"' % (key, i + 1)
                                                               for i in range(len(desc))))
            for i, d in enumerate(desc):
                self.lang['af9.quest.content.%s.d%d' % (key, i + 1)] = d
        if deps:
            lines.append('\t\t\tdependencies: [%s]' % ' '.join('"%s"' % d for d in deps))
        lines.append('\t\t\ticon: "%s"' % (icon or icon_of(tasks[0])))
        lines.append('\t\t\tid: "%s"' % qid)
        lines.append('\t\t\trewards: [{ id: "%s" type: "xp" xp: %d }]' % (hid('reward', key), xp))
        if shape:
            lines.append('\t\t\tshape: "%s"' % shape)
        if size != 1.0:
            lines.append('\t\t\tsize: %sd' % size)
        if subtitle:
            lines.append('\t\t\tsubtitle: "{af9.quest.content.%s.sub}"' % key)
            self.lang['af9.quest.content.%s.sub' % key] = subtitle
        lines.append('\t\t\ttasks: [%s]' % ' '.join(snbt_task(key, t) for t in tasks) if tasks else
                     '\t\t\ttasks: [{ id: "%s" type: "checkmark" }]' % hid('task', key))
        if title:
            lines.append('\t\t\ttitle: "{af9.quest.content.%s.title}"' % key)
            self.lang['af9.quest.content.%s.title' % key] = title
        lines.append('\t\t\tx: %sd' % x)
        lines.append('\t\t\ty: %sd' % y)
        lines.append('\t\t}')
        self.quests.append('\n'.join(lines))
        return qid


def main():
    force = '--force' in sys.argv
    if os.path.exists(OUT) and not force:
        sys.exit('%s exists (it is edited in game): pass --force to overwrite it' % OUT)
    mats = parse()
    ch = Chapter()
    intro = ch.quest('intro', None, 0.0, 0.0, size=2.5, shape='gear', title='AF9 Content',
                     subtitle='Every material the pack adds, to tick off',
                     desc=['This chapter lists the superalloys with every part, then every material AF9 adds, grouped by the line that makes it. '
                           'A quest is done when you hold the item (dust or ingot) or have the fluid in a tank.',
                           'The sections read left to right from the first chemical of a line to its product.'],
                     icon='gtceu:inconel_718_ingot', xp=25)
    y = 3.5
    # ---- the superalloys ----
    head = ch.quest('superalloys', None, 0.0, y, deps=[intro], size=1.5, shape='hexagon', title='Superalloys',
                    subtitle='Inconel, Havar, Kovar, Hastelloy, Stellite',
                    desc=['Eight nickel and cobalt superalloys, each with its own colours and texture: ingot, plate, double plate, dense plate, '
                          'fine wire, screw and a turbine rotor with its own power, efficiency and durability.',
                          'Kovar and Hastelloy C-276 were already in the pack and gained these parts; the other six are new.'],
                    icon='gtceu:inconel_738_rotor', xp=25)
    y += 2.0
    for a, name, text in ALLOYS:
        prev = head
        for i, (pat, _) in enumerate(PARTS):
            item = 'gtceu:' + pat.format(a=a)
            key = 'sa_%s_%d' % (a, i)
            kw = {}
            if i == 0:
                kw = dict(title=name, subtitle='Superalloy', desc=[text], size=1.5)
            prev = ch.quest(key, [{'type': 'item', 'item': item}], 0.0 + i * 1.75, y, deps=[prev], xp=10, **kw)
        y += 2.0
    y += 1.0
    # ---- the rest ----
    by_section = {}
    section_of = {}
    for sid, _, _, methods, _ in SECTIONS:
        for m in methods:
            section_of[m] = sid
    for mat, (method, form) in mats.items():
        if mat in SKIP or mat.startswith('?') or method not in section_of:
            continue
        by_section.setdefault(section_of[method], []).append((mat, form))
    COLS = 12
    for sid, name, sub, methods, icon in SECTIONS:
        items = by_section.get(sid, [])
        if not items:
            continue
        sh = ch.quest('sec_' + sid, None, 0.0, y, deps=[intro], size=1.5, shape='hexagon', title=name, subtitle=sub,
                      desc=['%s: %d materials.' % (sub, len(items))], icon=icon, xp=25)
        y += 2.0
        for n, (mat, form) in enumerate(items):
            ch.quest('m_' + mat, [task(mat, form)], (n % COLS) * 1.5, y + (n // COLS) * 1.5, deps=[sh])
        y += ((len(items) - 1) // COLS + 1) * 1.5 + 1.5
    out = ['{', '\tdefault_hide_dependency_lines: false', '\tdefault_quest_shape: ""', '\tfilename: "content"',
           '\tgroup: "%s"' % GROUP, '\ticon: "gtceu:inconel_718_ingot"', '\tid: "%s"' % hid('chapter', 'content'),
           '\torder_index: 20', '\tprogression_mode: "flexible"', '\tquest_links: [ ]', '\tquests: [']
    out.append('\n'.join(ch.quests))
    out += ['\t]', '\tsubtitle: ["{af9.quest.content.chapter.subtitle}"]', '\ttitle: "{af9.quest.content.chapter.title}"', '}']
    with open(OUT, 'w', newline='\n') as f:
        f.write('\n'.join(out) + '\n')
    ch.lang['af9.quest.content.chapter.title'] = 'AF9 Content'
    ch.lang['af9.quest.content.chapter.subtitle'] = 'The superalloys and every material the pack adds'
    # lang: replace the old content keys, keep the file's tab style
    text = open(LANG, encoding='utf-8').read()
    lines = [l for l in text.split('\n') if '"af9.quest.content.' not in l]
    close = max(i for i, l in enumerate(lines) if l.strip() == '}')
    prev = close - 1
    while not lines[prev].strip():
        prev -= 1
    if not lines[prev].rstrip().endswith(','):
        lines[prev] = lines[prev].rstrip() + ','
    new = ['\t%s: %s,' % (json.dumps(k), json.dumps(v, ensure_ascii=False)) for k, v in ch.lang.items()]
    new[-1] = new[-1].rstrip(',')
    lines[close:close] = new
    with open(LANG, 'w', encoding='utf-8', newline='\n') as f:
        f.write('\n'.join(lines))
    json.load(open(LANG, encoding='utf-8'))
    print('%d quests, %d texts' % (len(ch.quests), len(ch.lang)))


if __name__ == '__main__':
    main()
