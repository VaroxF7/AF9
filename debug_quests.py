#!/usr/bin/env python3
import re

with open('config/ftbquests/quests/chapters/circuits_only.snbt', 'r', encoding='utf-8') as f:
    content = f.read()

# Find id: xp patterns
print("Finding id: xp patterns...")
for m in re.finditer(r'"id":\s*"xp"', content):
    start = m.start()
    print(f'Found at {start}: ...{content[start-5:start+30]}...')

print("\nFinding id: checkmark patterns...")
for m in re.finditer(r'"id":\s*"checkmark"', content):
    start = m.start()
    print(f'Found at {start}: ...{content[start-5:start+30]}...')

print("\nFinding linked_quest patterns...")
for m in re.finditer(r'linked_quest:', content):
    start = m.start()
    print(f'Found at {start}: ...{content[start-5:start+50]}...')

print("\nFinding quest positions...")
for m in re.finditer(r'\"id\":\s*"circuits_intro"', content):
    start = m.start()
    print(f'Found circuits_intro at {start}')