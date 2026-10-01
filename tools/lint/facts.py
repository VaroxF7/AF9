#!/usr/bin/env python3
"""AF9 lint: facts that the quest texts state and the recipes decide.

    python3 tools/lint/facts.py [repo root]

A number in a quest text that a recipe could change is a promise: when the recipe changes, the text lies. Checked (codes X1, X2):

  * (X2) the numbers of `com.af9.core.litho.LithoMode` (Java) against the print recipes (KubeJS): every mode's recipe type exists, its
    prints take `kubejs:coated_<substrate>_wafer` (a blank on the 1 nm station) and give `kubejs:broken_<substrate>_wafer` as the
    chanced output at the mode's base break chance, at `EUt(VA[tier], 4)` (50 A at 1 nm)
  * the quests of the nodes (`af9.quest.litho.n<node>.*`) say "Fluids per print: <amount> <fluid>, ..." and, for the coater,
    "Coater Track, per wafer: <amount> <fluid>, ...; <amount> spent solvent out.": the amounts have to be those of the
    `lithography_<node>` print recipe and of the `wafer_coating` recipe of the node's substrate.

Run by tools/lint/run.sh. When you change a print or coating recipe, run it: the message names the key to rewrite.
"""
import json
import os
import re
import subprocess
import sys

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
        want_in = f'kubejs:{substrate}_wafer' if nm == '1' else f'kubejs:coated_{substrate}_wafer'
        if want_in not in r['itemIn']:
            report(f'{r["id"]}: takes {wafer_in}, LithoMode {mid} ({substrate}) wants {want_in}', r['file'], 'X2')
        broken = f'kubejs:broken_{substrate}_wafer'
        if broken not in r['chanceItems'] or int(base) not in r['chances']:
            report(f'{r["id"]}: broken wafer {r["chanceItems"]} at {r["chances"]}, LithoMode {mid} wants {broken} at {base}', r['file'], 'X2')
        eut = r['EUt']
        want_eut = [VOLT[tier], 50 if nm == '1' else 4]
        if not eut or [int(x) for x in eut] != want_eut:
            report(f'{r["id"]}: EUt {eut}, LithoMode {mid} wants {want_eut} (tier {tier}, amps)', r['file'], 'X2')

print(f'facts: {checked} node texts and modes checked against the recipes')
print(f'{len(findings)} errors, 0 warnings, 0 notes')
for level, code, msg, where in findings:
    print(f'{level:5} {code} {msg}' + (f'  [{where}]' if where else ''))
sys.exit(1 if findings else 0)
