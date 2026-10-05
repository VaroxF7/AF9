#!/usr/bin/env python3
with open('config/ftbquests/quests/chapters/circuits_only.snbt', 'r', encoding='utf-8') as f:
    lines = f.readlines()

# Show key lines
for i in [24, 25, 26, 226, 227, 228, 230, 231]:
    if i < len(lines):
        print("Line %d: %s" % (i+1, lines[i]), end='')
print()