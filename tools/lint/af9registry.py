"""The list of what AF9 Core registers, for the Python linters.

`tools/lint/data/af9-registry.txt` is written by AF9 Core's dev run (see tools/lint/README.md, "The registry list"): a line
a thing, `<kind> af9:<id> [words]`. The JavaScript linter reads the same file.
"""
import os

HERE = os.path.dirname(os.path.abspath(__file__))


def load(here=HERE):
    """kind -> {id without the namespace: the words after it}, for the kinds item, block, fluid and material.

    A material is GregTech's by id (`material gtceu:<name>`); the word `ore` marks one with an ore."""
    registry = {'item': {}, 'block': {}, 'fluid': {}, 'material': {}}
    try:
        with open(os.path.join(here, 'data', 'af9-registry.txt'), encoding='utf-8') as f:
            rows = [line.split() for line in f if line.strip() and not line.startswith('#')]
    except OSError:
        return registry
    for row in rows:
        if len(row) >= 2 and row[0] in registry and row[1].startswith('gtceu:' if row[0] == 'material' else 'af9:'):
            registry[row[0]][row[1].split(':', 1)[1]] = row[2:]
    return registry
