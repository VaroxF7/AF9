#!/usr/bin/env python3
with open('config/ftbquests/quests/chapters/circuits_only.snbt', 'r', encoding='utf-8') as f:
    lines = f.readlines()

for i, line in enumerate(lines[:35]):
    stripped = line.strip()
    if 'id' in stripped.lower():
        print("Line %d: %s" % (i+1, line), end='')