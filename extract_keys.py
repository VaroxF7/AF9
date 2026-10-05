#!/usr/bin/env python3
import subprocess
import re

result = subprocess.run(['python3', 'tools/lint/quests.py', '.'], capture_output=True, text=True, cwd='C:/Users/Arnik/Documents/GitHub/AF9')
output = result.stdout + result.stderr

# Extract missing keys
pattern = r'ERROR Q5 text key ([^\s]+) is in no lang file'
keys = set(re.findall(pattern, output))

with open('missing_keys.txt', 'w') as f:
    for key in sorted(keys):
        f.write(key + '\n')

print(f"Found {len(keys)} unique missing keys")
for key in sorted(keys):
    print(f"  {key}")