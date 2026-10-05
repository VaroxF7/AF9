import json, subprocess

linters = [
    ('scripts', ['node', 'tools/lint/scripts.js', '--json']),
    ('quests', ['python3', 'tools/lint/quests.py', '.']),
    ('assets', ['python3', 'tools/lint/assets.py', '.']),
    ('facts', ['python3', 'tools/lint/facts.py', '.']),
    ('docs', ['python3', 'tools/lint/docs.py', '.']),
    ('links', ['python3', 'tools/lint/links.py', '.']),
    ('previews', ['python3', 'tools/lint/previews.py', '.']),
]

for name, cmd in linters:
    try:
        result = subprocess.run(cmd, capture_output=True, text=True, cwd='C:/Users/Arnik/Documents/GitHub/AF9')
        if name == 'scripts':
            d = json.loads(result.stdout)
            errors = [x for x in d.get('findings', []) if x.get('level') == 'ERROR']
            print(f'{name}: {len(errors)} errors')
            for e in errors[:3]:
                msg = e['msg'][:60] if e.get('msg') else ''
                print(f'  {e["code"]}: {msg}')
        else:
            output = result.stdout + result.stderr
            error_lines = [l for l in output.split('\n') if 'ERROR' in l.upper()]
            print(f'{name}: {len(error_lines)} error lines')
            for l in error_lines[:2]:
                print(f'  {l[:80]}')
    except Exception as ex:
        print(f'{name}: error - {ex}')