#!/usr/bin/env python3
"""AF9 lint for the documents: README.md and docs/*.md.

    python3 tools/lint/docs.py [repo root]

  D1  a path in `code` that starts like a repository path (kubejs/, af9-core/, config/, docs/, tools/, .github/) but is no file or
      directory (a `*` matches anything; `<...>` is a placeholder; a path ends at a space, a `:` or a `#`; paths with `...`, files
      a run or build makes, and lines saying "removed" are skipped)
  D2  a relative Markdown link [text](path) to a file that does not exist
  D3  a section reference `§N.M` / `§N` of docs/semiconductor-factory.md that has no heading (only in that document and README)

Stale paths are the usual way a document lies: a file was renamed and the file map was not.
"""
import glob
import os
import re
import sys

ROOT = os.path.abspath(next((a for a in sys.argv[1:] if not a.startswith('--')), '.'))
findings = []


def report(level, code, msg, where):
    findings.append((level, code, msg, where))


docs = [os.path.join(ROOT, 'README.md')] + sorted(glob.glob(os.path.join(ROOT, 'docs/*.md')))
# files a run or a build makes (not in the repository), and a line that tells what was removed names paths that are gone on purpose
GENERATED = ('config/af9-common.toml', 'af9-core/build')


def exists(path):
    if path.startswith(GENERATED):
        return True
    path = re.sub(r'<[^>]*>', '*', path)
    if '*' in path:
        return bool(glob.glob(os.path.join(ROOT, path)))
    return os.path.exists(os.path.join(ROOT, path))


# headings of the main document: "## 6.4 Step ..." -> 6.4 ; "# 9. Extension" -> 9
main = os.path.join(ROOT, 'docs/semiconductor-factory.md')
sections = set()
if os.path.exists(main):
    with open(main, encoding='utf-8') as f:
        for line in f:
            m = re.match(r'#+ (\d+(?:\.\d+)?[a-z]?)\.? ', line)
            if m:
                sections.add(m.group(1))

for doc in docs:
    rel = os.path.relpath(doc, ROOT)
    with open(doc, encoding='utf-8') as f:
        text = f.read()
    in_block = False
    for n, line in enumerate(text.splitlines(), 1):
        if line.startswith('```'):
            in_block = not in_block
            continue
        if re.search(r'\b[Rr]emoved\b', line):
            continue   # "Removed with the redesign: `old/path`": gone on purpose
        # D1: inline code and, in code blocks, comments after '#' are not paths: only whole tokens that start like one
        tokens = re.findall(r'`([^`]+)`', line) if not in_block else re.findall(r'(?:^|\s)((?:kubejs|af9-core|config|docs|tools|\.github)/[^\s#:]+)', line)
        for tok in tokens:
            tok = tok.strip()
            m = re.match(r'((?:kubejs|af9-core|config|docs|tools|\.github)/[^\s#:,;()]+)', tok)
            if not m:
                continue
            if '...' in tok:
                continue   # an abbreviated path (af9-core/.../fab)
            path = m.group(1).rstrip('.')
            if path.endswith('/'):
                path = path[:-1]
            if not exists(path):
                report('ERROR', 'D1', f'`{path}` is no file or directory', f'{rel}:{n}')
        # D2: markdown links
        for target in re.findall(r'\]\(([^)#\s]+)(?:#[^)]*)?\)', line):
            if re.match(r'[a-z]+:', target):
                continue
            base = os.path.dirname(doc)
            if not os.path.exists(os.path.normpath(os.path.join(base, target))):
                report('ERROR', 'D2', f'link to {target}, which does not exist', f'{rel}:{n}')
        # D3: section references
        if rel in ('README.md', 'docs/semiconductor-factory.md'):
            for ref in re.findall(r'§(\d+(?:\.\d+)?[a-z]?)', line):
                if sections and ref not in sections and not in_block:
                    # a sub-section like §6.5b may be named 6.5b: accept the parent too
                    if re.sub(r'[a-z]$', '', ref) in sections and ref.endswith(tuple('abcdefgh')):
                        continue
                    report('WARN', 'D3', f'§{ref} has no heading in docs/semiconductor-factory.md', f'{rel}:{n}')

order = {'ERROR': 0, 'WARN': 1, 'INFO': 2}
findings.sort(key=lambda f: (order[f[0]], f[1], f[3]))
print(f'docs: {len(docs)} documents, {len(sections)} sections of the main one')
count = lambda l: sum(1 for f in findings if f[0] == l)
print(f'{count("ERROR")} errors, {count("WARN")} warnings, 0 notes')
for level, code, msg, where in findings:
    print(f'{level:5} {code} {msg}  [{where}]')
sys.exit(1 if count('ERROR') else 0)
