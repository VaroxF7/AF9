#!/usr/bin/env python3
"""AF9 lint for the links between KubeJS and Java.

    python3 tools/lint/links.py [repo root]       (GT_SRC=<GT checkout> also checks the classes of GT)

  J1  `Java.loadClass('pkg.Class')` of a class that does not exist (AF9 Core's sources; GT's with GT_SRC)
  J2  `$Class.MEMBER` / `$Class.method(` in a script where `$Class` is such a class and the Java source of that class does not
      mention MEMBER / method at all (a typo in a static field: KubeJS stops loading with an error screen at startup)

The members are matched by name in the class's source (nothing is compiled), so a name inherited from a superclass of GT is not
found in the class itself: only classes of AF9 Core are checked for J2, and GT's J2 findings are warnings.

(J3, a KubeJS item or block that the Java names and no script registers, is gone: AF9 Core registers them itself and names
them through `registry/AF9Blocks` and `registry/AF9Items`, which the compiler checks.)
"""
import glob
import os
import re
import sys

ROOT = os.path.abspath(next((a for a in sys.argv[1:] if not a.startswith('--')), '.'))
GT = os.environ.get('GT_SRC')
findings = []


def report(level, code, msg, where):
    findings.append((level, code, msg, where))


def source_of(cls):
    """The Java file of a fully qualified class name, or None when its tree is not here."""
    rel = cls.replace('.', '/') + '.java'
    if cls.startswith('com.af9.'):
        p = os.path.join(ROOT, 'af9-core/src/main/java', rel)
        return p, True
    if cls.startswith('com.gregtechceu.') and GT:
        p = os.path.join(GT, 'src/main/java', rel)
        return p, True
    return None, False


loaded = {}   # variable -> (class, file)
scripts = sorted(glob.glob(os.path.join(ROOT, 'kubejs/**/*.js'), recursive=True))
texts = {}
for f in scripts:
    with open(f, encoding='utf-8') as fh:
        texts[f] = fh.read()
checked = 0
for f, t in texts.items():
    rel = os.path.relpath(f, ROOT)
    for var, cls in re.findall(r"(?:const|let|var)\s+(\$\w+)\s*=\s*Java\.loadClass\(\s*['\"]([\w.$]+)['\"]\s*\)", t):
        base = cls.split('$')[0]   # an inner class: look in the outer one
        path, known = source_of(base)
        if not known:
            continue
        checked += 1
        if not os.path.exists(path):
            report('ERROR', 'J1', f'{var} = Java.loadClass({cls!r}): no such class ({os.path.relpath(path, ROOT) if path.startswith(ROOT) else path})', rel)
            continue
        loaded[var] = (cls, path)

member_re = re.compile(r'(\$\w+)\.([A-Za-z_]\w*)')
cache = {}
for f, t in texts.items():
    rel = os.path.relpath(f, ROOT)
    for n, line in enumerate(t.splitlines(), 1):
        code = line.split('//')[0]
        for var, member in member_re.findall(code):
            if var not in loaded or member in ('class',):
                continue
            cls, path = loaded[var]
            if path not in cache:
                with open(path, encoding='utf-8') as fh:
                    cache[path] = fh.read()
            if not re.search(r'\b' + re.escape(member) + r'\b', cache[path]):
                level = 'ERROR' if cls.startswith('com.af9.') else 'WARN'
                report(level, 'J2', f'{var}.{member}: {cls} does not mention {member}', f'{rel}:{n}')

order = {'ERROR': 0, 'WARN': 1, 'INFO': 2}
findings.sort(key=lambda x: (order[x[0]], x[1], x[3]))
print(f'links: {checked} Java classes loaded by the scripts checked')
count = lambda l: sum(1 for x in findings if x[0] == l)
print(f'{count("ERROR")} errors, {count("WARN")} warnings, 0 notes')
for level, code, msg, where in findings:
    print(f'{level:5} {code} {msg}  [{where}]')
sys.exit(1 if count('ERROR') else 0)
