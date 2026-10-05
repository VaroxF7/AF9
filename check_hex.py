#!/usr/bin/env python3
import re
with open('config/ftbquests/quests/chapters/extreme_voltage.snbt', 'r', encoding='utf-8') as f:
    content = f.read()
# Find all id: patterns
ids = re.findall(r'"id":\s*"([^"]+)"', content)
print('IDs found:', len(ids))
for id in ids[:20]:
    print(' -', id)
# Check if any are 16 hex digits
hex16 = [id for id in ids if re.fullmatch(r'[0-9A-F]{16}', id)]
print('Hex16 IDs:', len(hex16))
for id in hex16[:5]:
    print(' -', id)