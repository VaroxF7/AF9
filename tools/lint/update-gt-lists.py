#!/usr/bin/env python3
"""Regenerates tools/lint/data/gt-{names,materials,recipe-types}.txt from a checkout of GregTechCEu/GregTech-Modern.

    python3 tools/lint/update-gt-lists.py /path/to/GregTech-Modern

The lists say which GT ids exist, so the linters can tell a typo (`gtceu:cleanroom_glas`) from a real name. They are made from the
branch head (1.20.1, version 8.0.0 at the time of writing), the pack runs 7.2.0: a name that is new in 8.0.0 passes the lint here but
does not exist in the pack, one that was renamed fails it. Ids that loops make (coil blocks, lenses, pipes, lamps, gems) are not
listed but matched by regular expressions in data/gt-patterns.txt. Names of the base pack that GT's lists lack go to data/pack.txt.
The lists are over-approximations on purpose (every lower-case string literal in the registration code): a name that is only
missing here is a finding, a typo that happens to equal another literal is not.
"""
import glob
import os
import re
import sys

if len(sys.argv) < 2:
    sys.exit(__doc__)
gt = os.path.join(sys.argv[1], 'src/main/java/com/gregtechceu/gtceu')
out = os.path.join(os.path.dirname(os.path.abspath(__file__)), 'data')


def read(path):
    with open(path, encoding='utf-8') as f:
        return f.read()


def write(name, items):
    with open(os.path.join(out, name), 'w', encoding='utf-8') as f:
        f.write('\n'.join(sorted(items)) + '\n')
    print(f'{name}: {len(items)}')


# registration code: items, blocks, machines, casings, covers, tools, pipes
reg_dirs = ['common/data', 'common/data/machines', 'common/block', 'common/data/items', 'common/machine/multiblock',
            'data/pack']
names = set()
for d in reg_dirs:
    for f in glob.glob(os.path.join(gt, d, '*.java')):
        names |= set(re.findall(r'"([a-z][a-z0-9_]*)"', read(f)))
write('gt-names.txt', names)

materials = set()
for f in glob.glob(os.path.join(gt, '**/*.java'), recursive=True):
    t = read(f)
    materials |= set(re.findall(r'Material\.Builder\(\s*GTCEu\.id\("([a-z0-9_]+)"\)', t))
    materials |= set(re.findall(r'new\s+Material\.Builder\(\s*GTCEu\.id\("([a-z0-9_]+)"\)', t))
write('gt-materials.txt', materials)

types = set()
rt = os.path.join(gt, 'common/data/GTRecipeTypes.java')
if os.path.exists(rt):
    types = set(re.findall(r'register\(\s*"([a-z0-9_]+)"', read(rt)))
write('gt-recipe-types.txt', types)
