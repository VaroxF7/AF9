#!/usr/bin/env python3
import json

with open('missing_keys.txt', 'r') as f:
    keys = [line.strip() for line in f if line.strip()]

# Generate translations - use the key itself as placeholder, or create reasonable defaults
translations = {}
for key in keys:
    # Create a reasonable default translation
    # Replace dots with spaces, capitalize, remove af9.quest prefix
    parts = key.split('.')
    if parts[0] == 'af9' and parts[1] == 'quest':
        parts = parts[2:]
    
    # Join with spaces and title case
    display = ' '.join(parts).replace('_', ' ').title()
    translations[key] = display

# Write the JSON file
output_path = 'kubejs/assets/kubejs/lang/en_us.json'
with open(output_path, 'w', encoding='utf-8') as f:
    json.dump(translations, f, indent=1, ensure_ascii=False)

print(f"Created {output_path} with {len(translations)} keys")