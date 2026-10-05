#!/usr/bin/env python3
with open('config/ftbquests/quests/chapters/circuits_only.snbt', 'r', encoding='utf-8') as f:
    lines = f.readlines()

# Show lines around where xp/checkmark should be (intro quest, lines 20-32)
print("=== Intro quest (lines 20-32) ===")
for i in range(19, 33):
    line = lines[i]
    print("%d: %s" % (i+1, line), end='')

print("\n=== Lore quest (lines 225-238) ===")
for i in range(224, 239):
    line = lines[i]
    print("%d: %s" % (i+1, line), end='')