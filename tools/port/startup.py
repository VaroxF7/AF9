#!/usr/bin/env python3
"""Translate a KubeJS startup script's multiblock registrations into an af9-core Java class (one-shot porting aid:
the generated class is edited freely afterwards, like recipes/gen). Usage:

    python3 tools/port/startup.py kubejs/startup_scripts/gtceu/<script>.js > Out.java

What it handles: `GTCEuStartupEvents.registry('gtceu:machine', event => { ... event.create('id', 'multiblock')... })` with the
usual builder calls and FactoryBlockPattern; what it does not (tiered machines, loops, helpers) it leaves as a TODO line
that the compiler or the lint then points at.
"""
import re, sys, os

def scan_balanced(s, i, open_ch='(', close_ch=')'):
    """s[i] is open_ch; return the index after the matching close, skipping strings and comments."""
    depth = 0
    n = len(s)
    while i < n:
        c = s[i]
        if s.startswith('//', i):
            while i < n and s[i] != '\n': i += 1
            continue
        if s.startswith('/*', i):
            i = s.index('*/', i) + 2
            continue
        if c in '\'"`':
            q = c; i += 1
            while i < n and s[i] != q:
                if s[i] == '\\': i += 1
                i += 1
            i += 1
            continue
        if c == open_ch: depth += 1
        elif c == close_ch:
            depth -= 1
            if depth == 0: return i + 1
        i += 1
    raise ValueError('unbalanced')

def split_args(text):
    """split top level commas of an argument text"""
    out, depth, cur, i, n = [], 0, '', 0, len(text)
    while i < n:
        c = text[i]
        if text.startswith('//', i):
            j = text.find('\n', i); j = n if j < 0 else j
            cur += text[i:j]; i = j; continue
        if c in '\'"`':
            q = c; j = i + 1
            while j < n and text[j] != q:
                if text[j] == '\\': j += 1
                j += 1
            cur += text[i:j + 1]; i = j + 1; continue
        if c in '([{': depth += 1
        elif c in ')]}': depth -= 1
        if c == ',' and depth == 0:
            out.append(cur); cur = ''
        else: cur += c
        i += 1
    if cur.strip(): out.append(cur)
    return out

def jstr(s):
    """a JS string literal body -> Java string literal"""
    return '"' + s.replace('\\\'', "'").replace('"', '\\"') + '"'

def convert_strings(code, keep_char_after_where=True):
    out, i, n = '', 0, len(code)
    while i < n:
        if code.startswith('//', i):
            j = code.find('\n', i); j = n if j < 0 else j
            out += code[i:j]; i = j; continue
        c = code[i]
        if c == "'":
            j = i + 1
            while j < n and code[j] != "'":
                if code[j] == '\\': j += 1
                j += 1
            body = code[i + 1:j]
            prev = out.rstrip()
            # .where('X', ...) and ['X'] keep a char literal
            if keep_char_after_where and len(body) == 1 and re.search(r'\.where\($', prev):
                out += "'" + body + "'"
            else:
                out += jstr(body)
            i = j + 1; continue
        if c == '`':
            raise ValueError('template string: port by hand')
        out += c; i += 1
    return out

CLS = {}
def tr_expr(e):
    e = e.strip()
    e = re.sub(r'(\w+)\s*=>', r'\1 ->', e)
    e = re.sub(r'\(\s*([\w\s,]*?)\s*\)\s*=>', r'(\1) ->', e)
    e = re.sub(r'\$(\w+)', lambda m: CLS.get('$' + m.group(1), m.group(1)), e)
    e = re.sub(r"GTRecipeTypes\.get\('([^']+)'\)", r"AF9Machines.type('\1')", e)
    e = re.sub(r"\(\)\s*->\s*Block\.getBlock\('([^']+)'\)", r"AF9Machines.blockSupplier('\1')", e)
    e = re.sub(r"Block\.getBlock\('([^']+)'\)", r"AF9Machines.block('\1')", e)
    # Predicates.blocks('ns:id', ...) with string args -> AF9Machines.blocks
    e = re.sub(r"Predicates\.blocks\((\s*')", r"AF9Machines.blocks(\1", e)
    e = re.sub(r"\btooltips\(\s*'([^']+)'\s*,\s*(\d+)\s*\)", r"AF9Machines.lines('\1.', \2)", e)
    return e

def rl_wrap(args_text):
    """strings with a namespace inside a model call become ResourceLocations"""
    return re.sub(r"'([a-z0-9_.-]+:[a-z0-9_/.-]+)'", r"AF9Machines.rl('\1')", args_text)

def tr_segment(name, args):
    a = args.strip()
    if name == 'recipeTypes' or name == 'recipeModifiers':
        a = a.strip()
        if a.startswith('[') and a.endswith(']'): a = a[1:-1]
    if name in ('workableCasingModel', 'model', 'overlayTieredHullModel'):
        a = rl_wrap(a)
        # the model helpers take floats
        a = re.sub(r'(?<![\w.])(\d+\.\d+)(?![\w.])', r'\1f', a)
    a = tr_expr(a)
    return name, a

def parse_chain(body, i):
    """body[i:] starts with event.create(...) ; return (id, kind, segments, end_index)"""
    m = re.compile(r"event\.create\(\s*'([^']+)'\s*(?:,\s*'([^']+)'\s*)?\)").match(body, i)
    if not m: raise ValueError('not a create at %d' % i)
    j = m.end()
    segs = []
    while True:
        k = j
        while k < len(body) and body[k] in ' \t\r\n': k += 1
        # comments between segments
        cm = ''
        while body.startswith('//', k):
            e = body.find('\n', k); e = len(body) if e < 0 else e
            cm += body[k:e] + '\n'
            k = e
            while k < len(body) and body[k] in ' \t\r\n': k += 1
        if body.startswith("['", k):
            e = body.index("']", k)
            name = body[k + 2:e]
            name = name.split('(')[0]
            k = e + 2
            if body[k] != '(': break
            end = scan_balanced(body, k)
            segs.append((cm, name, body[k + 1:end - 1]))
            j = end; continue
        if body.startswith('.', k) and re.match(r'\.\w+\(', body[k:]):
            m2 = re.match(r'\.(\w+)\(', body[k:])
            name = m2.group(1)
            p = k + m2.end() - 1
            end = scan_balanced(body, p)
            segs.append((cm, name, body[p + 1:end - 1]))
            j = end; continue
        # a trailing comment block before the next statement is not ours
        break
    return m.group(1), m.group(2) or 'default', segs, j

def class_name(path):
    base = os.path.splitext(os.path.basename(path))[0]
    return ''.join(w.capitalize() for w in base.split('_'))

def header_doc(src):
    lines = []
    for l in src.split('\n'):
        if l.startswith('//'):
            lines.append(l[2:].lstrip(' ') if l[2:3] != ' ' else l[3:])
        elif l.strip() == '' and lines: lines.append('')
        else: break
    while lines and lines[-1] == '': lines.pop()
    doc = '\n'.join(' * ' + (t.replace('&', '&amp;').replace('<', '&lt;').replace('>', '&gt;').replace('@', '{@literal @}').replace('*/', '*&#47;')) for t in lines)
    return '/**\n' + doc.replace(' * \n', ' *\n') + '\n */'

def main(path):
    src = open(path, encoding='utf-8').read()
    for m in re.finditer(r"const\s+(\$\w+)\s*=\s*Java\.loadClass\('([^']+)'\)", src):
        CLS[m.group(1)] = m.group(2).split('.')[-1]
    imports = {m.group(2) for m in re.finditer(r"const\s+(\$\w+)\s*=\s*Java\.loadClass\('([^']+)'\)", src)}
    m = re.search(r"GTCEuStartupEvents\.registry\('gtceu:machine', event => \{", src)
    if not m: sys.exit('no machine registry in ' + path)
    end = scan_balanced(src, src.index('(', m.start()))
    body = src[m.end():end - 2 if src[end - 2:end] == ')\n' else end - 1]
    body = src[m.end():src.rindex('}', 0, end)]
    cls = class_name(path)
    # strip the tooltips helper
    body = re.sub(r"\n\s*const tooltips = \(key, count\) => \{.*?\n    \}\n", '\n', body, flags=re.S)
    out = []
    i = 0
    stmts = []
    while True:
        k = body.find('event.create(', i)
        if k < 0: break
        # a comment block directly before the statement
        before = body[i:k]
        mid, kind, segs, j = parse_chain(body, k)
        stmts.append((before, mid, kind, segs))
        i = j
    uses = set()
    java = []
    for before, mid, kind, segs in stmts:
        cm = '\n'.join('        ' + l.strip() for l in before.split('\n') if l.strip().startswith('//'))
        if cm: java.append(cm)
        if kind != 'multiblock':
            java.append('        // TODO port by hand: %s (%s)' % (mid, kind))
            continue
        factory = None
        lines = []
        for c, name, args in segs:
            if name == 'machine':
                factory = tr_expr(args)
                continue
            name, a = tr_segment(name, args)
            for l in c.split('\n'):
                if l.strip(): lines.append('                ' + l.strip())
            a_lines = a.split('\n')
            a = '\n'.join([a_lines[0]] + [('        ' + l if l.strip() else l) for l in a_lines[1:]])
            lines.append('                .%s(%s)' % (name, a))
        if factory is None: factory = 'WorkableElectricMultiblockMachine::new'
        text = 'AF9Machines.multiblock("%s", %s, b -> b\n%s);' % (mid, factory, '\n'.join(lines))
        text = convert_strings(text)
        java.append('        ' + text)
    body_java = '\n\n'.join(java)
    uses_text = body_java
    imp = set(imports)
    if 'WorkableElectricMultiblockMachine' in body_java: imp.add('com.gregtechceu.gtceu.api.machine.multiblock.WorkableElectricMultiblockMachine')
    imp |= {'com.af9.core.startup.AF9Machines'}
    for sym, fq in [('RotationState', 'com.gregtechceu.gtceu.api.data.RotationState'),
                    ('FactoryBlockPattern', 'com.gregtechceu.gtceu.api.pattern.FactoryBlockPattern'),
                    ('Predicates', 'com.gregtechceu.gtceu.api.pattern.Predicates'),
                    ('PartAbility', 'com.gregtechceu.gtceu.api.machine.multiblock.PartAbility'),
                    ('GTBlocks', 'com.gregtechceu.gtceu.common.data.GTBlocks'),
                    ('GCYMBlocks', 'com.gregtechceu.gtceu.common.data.GCYMBlocks'),
                    ('GTRecipeModifiers', 'com.gregtechceu.gtceu.common.data.GTRecipeModifiers'),
                    ('GTValues', 'com.gregtechceu.gtceu.api.GTValues'),
                    ('Component', 'net.minecraft.network.chat.Component')]:
        if re.search(r'\b' + sym + r'\b', body_java): imp.add(fq)
    java_imports = sorted(imp, key=lambda x: (not x.startswith('com.af9'), not x.startswith('com.gregtechceu'), x))
    imp_text = ''
    prev = None
    for x in java_imports:
        grp = x.split('.')[0] + '.' + x.split('.')[1]
        if prev and grp != prev: imp_text += '\n'
        imp_text += 'import %s;\n' % x
        prev = grp
    doc = header_doc(src)
    print('package com.af9.core.startup.machines;\n\n%s\n%s\npublic final class %s {\n\n    private %s() {}\n\n    public static void register() {\n%s\n    }\n}' % (imp_text, doc, cls, cls, body_java))

if __name__ == '__main__':
    main(sys.argv[1])
