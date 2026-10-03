#!/usr/bin/env python3
"""AF9 lint: facts that the quest texts state and the recipes decide.

    python3 tools/lint/facts.py [repo root]

A number in a quest text that a recipe could change is a promise: when the recipe changes, the text lies. Checked (codes X1-X4):

  * (X2) the numbers of `com.af9.core.litho.LithoMode` (Java) against the print recipes (KubeJS): every mode's recipe type exists, its
    prints take `af9:coated_<substrate>_wafer` (a blank on the 1 nm station) and give `af9:broken_<substrate>_wafer` as the
    chanced output at the mode's base break chance, at `EUt(VA[tier], 4)` (50 A at 1 nm)
  * the quests of the nodes (`af9.quest.litho.n<node>.*`) say "Fluids per print: <amount> <fluid>, ..." and, for the coater,
    "Coater Track, per wafer: <amount> <fluid>, ...; <amount> spent solvent out.": the amounts have to be those of the
    `lithography_<node>` print recipe and of the `wafer_coating` recipe of the node's substrate.

  * (X3) the quests of the Asteroid Fission chapter (`af9.quest.fx.*`) state the fluid amounts and run times of the recipes of
    kubejs/server_scripts/mods/gtceu/asteroid_fission.js and rockets.js (the reactor cycle, the leach, the propellant ...)
  * (X4) names that Java, data and scripts share: the rock AsteroidFieldFeature builds and the stones of the ore layer, the
    reactor id RadiationWatch looks for, the fluid tag ExtremeReactorsCompat maps, the dimensions of the asteroid layer, the ore
    veins, the planets and the space station recipe, the loot tables of the temples (they exist, and every item in them does:
    an unknown item makes Minecraft drop the whole table)

Run by tools/lint/run.sh. When you change a print or coating recipe, run it: the message names the key to rewrite.
"""
import json
import os
import re
import subprocess
import sys

import af9registry

ROOT = os.path.abspath(next((a for a in sys.argv[1:] if not a.startswith('--')), '.'))
HERE = os.path.dirname(os.path.abspath(__file__))
findings = []


def report(msg, where='', code='X1'):
    findings.append(('ERROR', code, msg, where))


out = subprocess.run(['node', os.path.join(HERE, 'scripts.js'), ROOT, '--dump'], capture_output=True, text=True)
recipes = []
for line in out.stdout.splitlines():
    if line.startswith('{'):
        recipes.append(json.loads(line))
if not recipes:
    report('the script linter gave no recipes (node tools/lint/scripts.js --dump failed)')

lang = {}
with open(os.path.join(ROOT, 'kubejs/assets/kubejs/lang/en_us.json'), encoding='utf-8') as f:
    lang = json.load(f)

SUBSTRATE = {'200nm': 'phosphorus', '100nm': 'naquadah', '80nm': 'trinium', '65nm': 'naquadria', '50nm': 'neutronium',
             '20nm': 'transmuted_neutronium', '7nm': 'strange_matter'}


def amounts(text):
    """The amounts of 'a X, b Y, c Z' (up to a ';' or '.' that ends the list)."""
    return sorted(int(n) for n in re.findall(r'(\d+) [A-Za-z][A-Za-z ]*?(?=,|;|\.|$)', text))


checked = 0
for node, substrate in SUBSTRATE.items():
    prefix = f'af9.quest.litho.n{node[:-2]}.'
    texts = [(k, v) for k, v in lang.items() if k.startswith(prefix) and v.startswith('Fluids per print:')]
    if not texts:
        report(f'{prefix}*: no "Fluids per print:" text for the {node} node')
        continue
    key, text = texts[0]
    m = re.match(r'Fluids per print: (.*?)\. Coater Track, per wafer: (.*?); (\d+) spent solvent out\.$', text)
    if not m:
        report(f'{key}: not in the form "Fluids per print: ... . Coater Track, per wafer: ...; N spent solvent out."', 'kubejs lang')
        continue
    print_recipe = next((r for r in recipes if r['type'] == f'lithography_{node}'), None)
    coat_recipe = next((r for r in recipes if r['id'] == f'af9:coat_{substrate}_wafer'), None)
    if not print_recipe or not coat_recipe:
        report(f'{key}: no print or coating recipe for the {node} node')
        continue
    checked += 1
    want_print = sorted(a for _, a in print_recipe['fluidAmounts'])
    got_print = amounts(m.group(1))
    if want_print != got_print:
        report(f'{key}: the text says {got_print} mB per print, the recipe af9:print_*_{node} takes {want_print}', 'kubejs lang')
    coat_in = sorted(a for f, a in coat_recipe['fluidAmounts'] if f != 'gtceu:spent_resist_solvent')
    waste = [a for f, a in coat_recipe['fluidAmounts'] if f == 'gtceu:spent_resist_solvent']
    if coat_in != amounts(m.group(2)) or [int(m.group(3))] != waste:
        report(f'{key}: the text says coating {amounts(m.group(2))} + {m.group(3)} spent solvent, the recipe {coat_recipe["id"]} '
               f'has {coat_in} + {waste}', 'kubejs lang')

# ---- X2: LithoMode (Java) against the print recipes (KubeJS) -------------------------------------------------------------------
VOLT = {'MV': 128, 'HV': 512, 'EV': 2048, 'IV': 8192, 'LuV': 32768, 'ZPM': 131072, 'UV': 524288, 'UHV': 2097152}
java = open(os.path.join(ROOT, 'af9-core/src/main/java/com/af9/core/litho/LithoMode.java'), encoding='utf-8').read()
# the enum constants span two lines: join them first
flat = re.sub(r'\n\s+', ' ', java)
modes = re.findall(r'\bN(\d+)\("(\w+)", "(\w+)", \d+, GTValues\.(\w+), .*?, (\d+), Machine\.\w+, \d+\)', flat)
if len(modes) != 9:
    report(f'LithoMode.java: found {len(modes)} modes, expected 9 (did its constants change shape? then fix tools/lint/facts.py)', code='X2')
for nm, mid, substrate, tier, base in modes:
    rtype = 'orbital_lithography' if nm == '1' else f'lithography_{mid}'
    prints = [r for r in recipes if r['type'] == rtype]
    if not prints:
        report(f'LithoMode {mid}: no recipes of type {rtype}', code='X2')
        continue
    checked += 1
    for r in prints:
        wafer_in = [i for i in r['itemIn'] if i and i.endswith('_wafer')]
        want_in = f'af9:{substrate}_wafer' if nm == '1' else f'af9:coated_{substrate}_wafer'
        if want_in not in r['itemIn']:
            report(f'{r["id"]}: takes {wafer_in}, LithoMode {mid} ({substrate}) wants {want_in}', r['file'], 'X2')
        broken = f'af9:broken_{substrate}_wafer'
        if broken not in r['chanceItems'] or int(base) not in r['chances']:
            report(f'{r["id"]}: broken wafer {r["chanceItems"]} at {r["chances"]}, LithoMode {mid} wants {broken} at {base}', r['file'], 'X2')
        eut = r['EUt']
        want_eut = [VOLT[tier], 50 if nm == '1' else 4]
        if not eut or [int(x) for x in eut] != want_eut:
            report(f'{r["id"]}: EUt {eut}, LithoMode {mid} wants {want_eut} (tier {tier}, amps)', r['file'], 'X2')

# ---- X3: the Asteroid Fission quests against the recipes ---------------------------------------------------------------------------
def quest_text(key):
    return ' '.join(v for k, v in lang.items() if k.startswith(f'af9.quest.fx.{key}.') and k.rsplit('.', 1)[1].isdigit())


FLUID_QUESTS = {
    'tea': 'af9:triethylaluminium', 'fuel': 'af9:aluminised_hydrolox', 'leach': 'af9:brannerite_leach',
    'yellowcake': 'af9:yellowcake_precipitation', 'uf6': 'af9:uranium_hexafluoride_from_yellowcake',
    'pellets': 'af9:fx_fuel_pellets', 'reactor': 'af9:fx1_fuel_cycle', 'dissolve': 'af9:dissolve_irradiated_fuel',
    'plutonium': 'af9:separate_spent_fuel', 'coolant': 'af9:cool_hot_sodium_potassium',
}
for key, rid in FLUID_QUESTS.items():
    recipe = next((r for r in recipes if r['id'] == rid), None)
    text = quest_text(key)
    if not recipe:
        report(f'af9.quest.fx.{key}: its recipe {rid} does not exist', code='X3')
        continue
    if not text:
        report(f'af9.quest.fx.{key}: no text', 'kubejs lang', 'X3')
        continue
    checked += 1
    for fluid, amount in recipe['fluidAmounts']:
        if f'{amount:,} mB' not in text:
            report(f'af9.quest.fx.{key}: the recipe {rid} moves {amount:,} mB of {fluid}, the text does not say "{amount:,} mB"',
                   'kubejs lang', 'X3')
reactor = next((r for r in recipes if r['id'] == 'af9:fx1_fuel_cycle'), None)
freezer = next((r for r in recipes if r['id'] == 'af9:cool_hot_sodium_potassium'), None)
turbine = next((r for r in recipes if r['id'] == 'af9:supercritical_steam'), None)
if reactor and freezer and turbine:
    steam = dict(reactor['fluidAmounts'])['gtceu:supercritical_steam']
    cycle = int(reactor['duration'][0])
    if f'{cycle:,} ticks' not in quest_text('reactor'):
        report(f'af9.quest.fx.reactor: the cycle is {cycle:,} ticks, the text does not say so', 'kubejs lang', 'X3')
    if f'{int(freezer["duration"][0])} ticks' not in quest_text('coolant'):
        report(f'af9.quest.fx.coolant: the Vacuum Freezer takes {int(freezer["duration"][0])} ticks, the text does not say so',
               'kubejs lang', 'X3')
    if f'{round(steam / cycle)} mB/t' not in quest_text('steam') or f'{steam:,} mB' not in quest_text('steam'):
        report(f'af9.quest.fx.steam: a rod makes {steam:,} mB, {round(steam / cycle)} mB/t: the text says otherwise', 'kubejs lang', 'X3')
    amount, ticks, eut = dict(turbine['fluidAmounts'])['gtceu:supercritical_steam'], int(turbine['duration'][0]), abs(int(turbine['EUt'][0]))
    per_turbine = f'{2 * amount / ticks:g} mB/t'     # two parallels in a Large Steam Turbine
    if per_turbine not in quest_text('turbines') or f'{2 * eut:,} EU/t' not in quest_text('turbines'):
        report(f'af9.quest.fx.turbines: a Large Steam Turbine (2 parallels) takes {per_turbine} for {2 * eut:,} EU/t: the text says otherwise',
               'kubejs lang', 'X3')
    if f'{eut} EU/t' not in quest_text('turbines'):
        report(f'af9.quest.fx.turbines: the turbine recipe is {eut} EU/t, the text does not say so', 'kubejs lang', 'X3')
    per_mb = eut * ticks / amount
    if f'{per_mb:g} EU per mB' not in quest_text('steam').replace('&e', '').replace('&r', ''):
        report(f'af9.quest.fx.steam: the turbine recipe is worth {per_mb:g} EU per mB, the text does not say so', 'kubejs lang', 'X3')

# ---- X4: names that Java, data and scripts share ----------------------------------------------------------------------------------
def read(rel):
    with open(os.path.join(ROOT, rel), encoding='utf-8') as f:
        return f.read()


def code(rel, pattern, what):
    m = re.search(pattern, read(rel), re.S)
    if not m:
        report(f'{rel}: could not find {what} (did it change shape? then fix tools/lint/facts.py)', code='X4')
    return m


startup = 'kubejs/startup_scripts/gtceu/asteroid_fission.js'
server = 'kubejs/server_scripts/mods/gtceu/asteroid_fission.js'
feature = 'af9-core/src/main/java/com/af9/core/space/AsteroidFieldFeature.java'
rock_m = code(feature, r'BlockState rockAt\(.*?\n    }', 'rockAt')
# the ore layer of the Asteroid Field (AF9Space): its stones are a block tag, its dimension a constant
space = 'af9-core/src/main/java/com/af9/core/space/AF9Space.java'
rock_tag = 'af9-core/src/main/resources/data/af9/tags/blocks/asteroid_rock.json'
layer_m = code(space, r'new SimpleWorldGenLayer\(ORE_LAYER, \(\) -> new TagMatchTest\(ASTEROID_ROCK\), Set\.of\(ASTEROID_FIELD_DIMENSION\)\)',
               'the ore layer of the Asteroid Field')
tag_m = code(space, r'ASTEROID_ROCK = BlockTags\.create\(new ResourceLocation\(AF9Core\.MOD_ID,\s*"asteroid_rock"\)\)', 'the tag of the layer\'s stones')
layer_dim_m = code(space, r'ASTEROID_FIELD_DIMENSION = new ResourceLocation\(AF9Core\.MOD_ID,\s*"(\w+)"\)', 'the dimension of the ore layer')
if rock_m and layer_m and tag_m:
    checked += 1
    rock = sorted({'minecraft:' + b.lower() for b in re.findall(r'Blocks\.([A-Z_]+)\.defaultBlockState', rock_m.group(0))})
    try:
        layer = sorted(json.loads(read(rock_tag))['values'])
    except (OSError, ValueError, KeyError):
        layer = None
        report(f'{rock_tag}: the stones of the ore layer af9_asteroid are not there (or not valid JSON)', code='X4')
    if layer is not None and rock != layer:
        report(f'AsteroidFieldFeature builds {rock}, the layer af9_asteroid (the tag af9:asteroid_rock) lets ores grow into {layer}: '
               f'ore veins would miss or replace the wrong rock', code='X4')
rad = code('af9-core/src/main/java/com/af9/core/radiation/RadiationWatch.java', r'REACTOR_ID = new ResourceLocation\("(\w+)", "(\w+)"\)', 'REACTOR_ID')
if rad:
    checked += 1
    if f"event.create('{rad.group(2)}', 'multiblock')" not in read(startup) or rad.group(1) != 'gtceu':
        report(f'RadiationWatch.REACTOR_ID is {rad.group(1)}:{rad.group(2)}, the startup script defines no such multiblock', code='X4')
tag = code('af9-core/src/main/java/com/af9/core/compat/extremereactors/ExtremeReactorsCompat.java', r'FLUID_TAG = "([^"]+)"', 'FLUID_TAG')
vapor = code('af9-core/src/main/java/com/af9/core/compat/extremereactors/ExtremeReactorsCompat.java', r'VAPOR_LANG_KEY = "([^"]+)"', 'VAPOR_LANG_KEY')
if tag and vapor:
    checked += 1
    if f"event.add('{tag.group(1)}'" not in read(server):
        report(f'ExtremeReactorsCompat maps the fluid tag {tag.group(1)}, no server script puts supercritical steam in it', code='X4')
    if f'"{vapor.group(1)}"' not in read('af9-core/src/main/resources/assets/af9/lang/en_us.json'):
        report(f'the vapor name {vapor.group(1)} is in no lang file of AF9 Core (Extreme Reactors would show the key)', code='X4')
data = os.path.join(ROOT, 'af9-core/src/main/resources/data/af9')
dims = {'af9:' + n[:-5] for n in os.listdir(os.path.join(data, 'dimension'))} if os.path.isdir(os.path.join(data, 'dimension')) else set()
planets = {}
planet_dir = os.path.join(data, 'planets')
for n in sorted(os.listdir(planet_dir)) if os.path.isdir(planet_dir) else []:
    planets[n[:-5]] = json.load(open(os.path.join(planet_dir, n), encoding='utf-8'))
for n, planet in planets.items():
    checked += 1
    if planet['dimension'] not in dims and not planet['dimension'].startswith('ad_astra:'):
        report(f'planets/{n}.json: the dimension {planet["dimension"]} has no data/af9/dimension file', code='X4')
    if 'orbit' in planet and planet['orbit'] not in {p['dimension'] for p in planets.values()}:
        report(f'planets/{n}.json: its orbit {planet["orbit"]} is no planet of AF9', code='X4')
space = {p['dimension'] for p in planets.values() if 'orbit' not in p}
used_dims = set(re.findall(r"\.dimensions\('([^']+)'\)", read(startup) + read(server)))
if layer_dim_m:
    used_dims.add('af9:' + layer_dim_m.group(1))
for d in sorted(used_dims):
    checked += 1
    if d not in dims:
        report(f'a script lets ore generate in {d}, which has no data/af9/dimension file', code='X4')
recipes_dir = os.path.join(data, 'recipes')
for n in sorted(os.listdir(recipes_dir)) if os.path.isdir(recipes_dir) else []:
    recipe = json.load(open(os.path.join(recipes_dir, n), encoding='utf-8'))
    if recipe.get('type') == 'ad_astra:space_station_recipe':
        checked += 1
        if recipe['dimension'] not in space:
            report(f'recipes/{n}: the space station of {recipe["dimension"]}, which is no space planet of AF9 (a planet without orbit)', code='X4')
        if recipe['dimension'] not in dims:
            report(f'recipes/{n}: the dimension {recipe["dimension"]} has no data/af9/dimension file', code='X4')

# the loot tables of the ancient temples: the sizes of TempleLayout name them, AsteroidFieldFeature builds the id from them
layout_src = read('af9-core/src/main/java/com/af9/core/space/TempleLayout.java')
tables = sorted(set(re.findall(r'^\s+(?:SHRINE|TEMPLE|GRAND_TEMPLE)\("(\w+)"', layout_src, re.M)))
if not tables:
    report('TempleLayout.java: found no sizes with a loot table (did Kind change shape? then fix tools/lint/facts.py)', code='X4')
if 'new ResourceLocation("af9", "chests/" + temple.kind.lootTable)' not in read(feature):
    report('AsteroidFieldFeature no longer builds the chest loot table id as af9:chests/<TempleLayout.Kind.lootTable>', code='X4')
af9_registry = af9registry.load(HERE)
af9_items = set(af9_registry['item'])
materials_dir = os.path.join(ROOT, 'kubejs/startup_scripts')
ore_materials = {name for name, words in af9_registry['material'].items() if 'ore' in words}
for dirpath, _, files in os.walk(materials_dir):
    for f in files:
        if f.endswith('.js'):
            body = open(os.path.join(dirpath, f), encoding='utf-8').read()
            for m in re.finditer(r"event\.create\('(\w+)'\)(.*?)(?=event\.create\(|\Z)", body, re.S):
                if re.search(r'\.ore\(', m.group(2).split('\n\n')[0]):
                    ore_materials.add(m.group(1))
gt_materials = {l.strip() for l in open(os.path.join(HERE, 'data/gt-materials.txt'), encoding='utf-8') if l.strip()}
for table in tables:
    checked += 1
    path = os.path.join(data, 'loot_tables', 'chests', table + '.json')
    if not os.path.isfile(path):
        report(f'TempleLayout names the loot table af9:chests/{table}, which is not in data/af9/loot_tables/chests', code='X4')
        continue
    loot = json.load(open(path, encoding='utf-8'))
    for pool in loot.get('pools', []):
        for entry in pool.get('entries', []):
            name = entry.get('name', '')
            ns, _, item = name.partition(':')
            if ns == 'kubejs' or (ns == 'af9' and item not in af9_items):
                report(f'chests/{table}.json: {name} is no item AF9 Core registers (the chips are af9:<chip>_chip)', code='X4')
            if ns == 'gtceu' and item.startswith('raw_') and item[4:] not in gt_materials | ore_materials:
                report(f'chests/{table}.json: {name} is no raw ore of GT or AF9 ({item[4:]} has no ore)', code='X4')
            if not name or ':' not in name:
                report(f'chests/{table}.json: an entry without an item name', code='X4')

print(f'facts: {checked} node texts, modes and shared names checked against the recipes')
print(f'{len(findings)} errors, 0 warnings, 0 notes')
for level, code, msg, where in findings:
    print(f'{level:5} {code} {msg}' + (f'  [{where}]' if where else ''))
sys.exit(1 if findings else 0)
