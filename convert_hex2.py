#!/usr/bin/env python3
with open('config/ftbquests/quests/chapters/circuits_only.snbt', 'r', encoding='utf-8') as f:
    content = f.read()

# Find all id: "value" patterns
import re
id_pattern = re.compile(r'id:\s*"([^"]+)"', re.MULTILINE)
ids_found = id_pattern.findall(content)
unique_ids = list(dict.fromkeys(ids_found))

print("Unique IDs found:", len(unique_ids))
for i, id in enumerate(unique_ids):
    print(f"  {i+1}. '{id}' (length: {len(id)})")

# Create mapping to 16-hex-digit IDs
id_mapping = {}
hex_num = 1
for old_id in unique_ids:
    hex_id = format(hex_num, '016X')
    id_mapping[old_id] = hex_id
    hex_num += 1

print("\nID mapping:")
for old, new in id_mapping.items():
    print(f"  '{old}' -> '{new}'")

# Replace all id: "old" with id: "new" in the content
for old_id, new_id in id_mapping.items():
    # Pattern: id: "old_id" (with possible leading whitespace)
    pattern = r'(?<=id:\s\")' + re.escape(old_id) + r'(?=\")'
    matches = len(re.findall(pattern, content))
    if matches > 0:
        content = re.sub(pattern, new_id, content)
        print(f"Replaced {matches} occurrence(s) of id: {old_id}")

# Also replace linked_quest references
for old_id, new_id in id_mapping.items():
    pattern = r'linked_quest:\s*"' + re.escape(old_id) + r'"'
    matches = len(re.findall(pattern, content))
    if matches > 0:
        content = re.sub(pattern, 'linked_quest: "' + new_id + '"', content)
        print(f"Updated linked_quest: {old_id} -> {new_id}")

# Write the modified content
with open('config/ftbquests/quests/chapters/circuits_only.snbt', 'w', encoding='utf-8') as f:
    f.write(content)
print("\nFile updated with hex IDs")