#!/usr/bin/env python3
import json, subprocess
result = subprocess.run(['node', 'tools/lint/scripts.js', '--json'], capture_output=True, text=True, cwd='C:/Users/Arnik/Documents/GitHub/AF9')
d = json.loads(result.stdout)
errors = [x for x in d.get('findings', []) if x.get('level') == 'ERROR']
print('Scripts lint:', len(errors), 'errors')
for e in errors:
    print('  ' + e['code'] + ': ' + e['msg'][:80])