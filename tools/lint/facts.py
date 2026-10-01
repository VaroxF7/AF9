#!/usr/bin/env python3
"""AF9 lint: facts that the quest texts state and the recipes decide.

    python3 tools/lint/facts.py [repo root]

A number in a quest text that a recipe could change is a promise: when the recipe changes, the text lies. Checked (code X1):

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


def report(msg, where=''):
    findings.append(('ERROR', 'X1', msg, where))


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

print(f'facts: {checked} node texts checked against the recipes')
print(f'{len(findings)} errors, 0 warnings, 0 notes')
for level, code, msg, where in findings:
    print(f'{level:5} {code} {msg}' + (f'  [{where}]' if where else ''))
sys.exit(1 if findings else 0)
