#!/usr/bin/env python3
with open('config/ftbquests/quests/chapters/circuits_only.snbt', 'r', encoding='utf-8') as f:
    lines = f.readlines()

# Show all lines with id: patterns
for i, line in enumerate(lines):
    stripped = line.strip()
    if '"id"' in stripped:
        print("Line %d: %s" % (i+1, line), end='')