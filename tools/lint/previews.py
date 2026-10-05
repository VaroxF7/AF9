#!/usr/bin/env python3
"""AF9 lint for multiblock preview pages.

    python3 tools/lint/previews.py [repo root]

Reads AF9 Core's Java sources (nothing is compiled) and checks every
MultiblockShapeInfo construction:

  P1  `new MultiblockShapeInfo(<array>)` where <array> is neither GTCEu's own
      `getPreview(...)` output nor passed through a null-sanitizer like
      `solid(...)` (a method that fills null cells with `BlockInfo.EMPTY`).

Why: GTCEu's preview widget dereferences every cell of a page with no null
check (PatternPreviewWidget: a single null BlockInfo throws), and that throw
aborts the whole JEI registration: every GTCEu recipe and every multiblock
preview vanishes while items and vanilla recipes look fine. A hand-built page
array (the preview turn math) must therefore never reach the constructor with
a hole in it; GTCEu's own getPreview output has the same trust level as GTCEu's
own machines.

A variable assigned from `getPreview(...)` in the same file counts as GTCEu's
own output (e.g. a page that only swaps some cells afterwards).
"""
import glob
import os
import re
import sys

ROOT = os.path.abspath(next((a for a in sys.argv[1:] if not a.startswith('--')), '.'))
findings = []


def report(level, code, msg, where):
    findings.append((level, code, msg, where))


def strip_comments(text):
    """Remove // and /* */ comments, keeping newlines so line numbers hold."""
    out = []
    i, n = 0, len(text)
    instr = None
    while i < n:
        c = text[i]
        if instr:
            out.append(c)
            if c == '\\' and i + 1 < n:
                out.append(text[i + 1])
                i += 2
                continue
            if c == instr:
                instr = None
            i += 1
        elif c in ('"', "'"):
            instr = c
            out.append(c)
            i += 1
        elif c == '/' and i + 1 < n and text[i + 1] == '/':
            while i < n and text[i] != '\n':
                out.append(' ')
                i += 1
            # keep the newline itself (falls through to append below)
            if i < n:
                out.append('\n')
                i += 1
        elif c == '/' and i + 1 < n and text[i + 1] == '*':
            i += 2
            while i < n and not (text[i] == '*' and i + 1 < n and text[i + 1] == '/'):
                out.append('\n' if text[i] == '\n' else ' ')
                i += 1
            i += 2
        else:
            out.append(c)
            i += 1
    return ''.join(out)


def balanced_arg(text, open_paren):
    """The text between the paren at open_paren and its match (strings aware)."""
    i = open_paren + 1
    depth = 1
    instr = None
    start = i
    while i < len(text):
        c = text[i]
        if instr:
            if c == '\\':
                i += 2
                continue
            if c == instr:
                instr = None
        elif c in ('"', "'"):
            instr = c
        elif c == '(':
            depth += 1
        elif c == ')':
            depth -= 1
            if depth == 0:
                return text[start:i]
        i += 1
    return None


checked = 0
for f in sorted(glob.glob(os.path.join(ROOT, 'af9-core/src/main/java/**/*.java'), recursive=True)):
    with open(f, encoding='utf-8') as fh:
        raw = fh.read()
    text = strip_comments(raw)
    rel = os.path.relpath(f, ROOT)
    for m in re.finditer(r'new\s+MultiblockShapeInfo\s*\(', text):
        arg = balanced_arg(text, m.end() - 1)
        if arg is None or not arg.strip():
            continue
        checked += 1
        line = text.count('\n', 0, m.start()) + 1
        if re.search(r'getPreview\s*\(', arg) or re.search(r'\bsolid\s*\(', arg):
            continue
        var = arg.strip()
        if re.fullmatch(r'[A-Za-z_]\w*', var):
            # the variable's last assignment before this site: GTCEu's own
            # getPreview(...) output has the same trust level as GTCEu's own
            # machines (e.g. a page that only swaps some cells afterwards)
            before = text[:m.start()]
            ok = False
            for a in re.finditer(r'(?<![=!<>])\b' + re.escape(var) + r'\s*=\s*(?!=)', before):
                rhs = before[a.end():].split(';', 1)[0]
                if re.search(r'getPreview\s*\(', rhs):
                    ok = True
            if ok:
                continue
        head = ' '.join(arg.strip().split())[:60]
        report('ERROR', 'P1',
               f'new MultiblockShapeInfo({head}...): a hand-built page array that is neither '
               f"GTCEu's getPreview(...) output nor null-sanitized (wrap it in solid(...)): one "
               f'null cell aborts the whole JEI registration',
               f'{rel}:{line}')

order = {'ERROR': 0, 'WARN': 1, 'INFO': 2}
findings.sort(key=lambda x: (order[x[0]], x[1], x[3]))
print(f'previews: {checked} MultiblockShapeInfo constructions checked')
count = lambda l: sum(1 for x in findings if x[0] == l)
print(f'{count("ERROR")} errors, {count("WARN")} warnings, 0 notes')
for level, code, msg, where in findings:
    print(f'{level:5} {code} {msg}  [{where}]')
sys.exit(1 if count('ERROR') else 0)
