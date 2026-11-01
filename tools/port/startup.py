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

def template_to_java(code):
    """`a${x}b` -> "a" + (x) + "b" (outside of single/double quoted strings)"""
    out, i, n = '', 0, len(code)
    while i < n:
        c = code[i]
        if code.startswith('//', i):
            j = code.find('\n', i); j = n if j < 0 else j
            out += code[i:j]; i = j; continue
        if c in '\'"':
            q = c; j = i + 1
            while j < n and code[j] != q:
                if code[j] == '\\': j += 1
                j += 1
            out += code[i:j + 1]; i = j + 1; continue
        if c == '`':
            j = i + 1; parts = []; cur = ''
            while code[j] != '`':
                if code.startswith('${', j):
                    k = scan_balanced(code, j + 1, '{', '}')
                    if cur: parts.append(('s', cur)); cur = ''
                    parts.append(('e', code[j + 2:k - 1]))
                    j = k; continue
                cur += code[j]; j += 1
            if cur: parts.append(('s', cur))
            if not parts or parts[0][0] == 'e': parts.insert(0, ('s', ''))
            segs = []
            for kind, text in parts:
                if kind == 's': segs.append('"' + text.replace('"', '\\"') + '"')
                else:
                    segs.append(text if re.fullmatch(r'[\w.\[\]]+', text) else '(' + text + ')')
            out += ' + '.join(segs); i = j + 1; continue
        out += c; i += 1
    return out

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

def tr_tooltips_arg(a):
    a = a.strip()
    if a.startswith('[') and a.endswith(']'): a = a[1:-1]
    return a

def emit_multiblock(mid, segs):
    factory = None
    lines = []
    for c, name, args in segs:
        if name == 'machine':
            factory = tr_expr(args)
            continue
        if name == 'tooltips': args = tr_tooltips_arg(args)
        name, a = tr_segment(name, args)
        for l in c.split('\n'):
            if l.strip(): lines.append('                ' + l.strip())
        a_lines = a.split('\n')
        a = '\n'.join([a_lines[0]] + [('        ' + l if l.strip() else l) for l in a_lines[1:]])
        lines.append('                .%s(%s)' % (name, a))
    if factory is None: factory = 'WorkableElectricMultiblockMachine::new'
    return 'AF9Machines.multiblock("%s", %s, b -> b\n%s);' % (mid, factory, '\n'.join(lines))

def emit_custom(mid, segs):
    tiers = None; machine = None; definition = None; scaling = None; default_tt = None
    for c, name, args in segs:
        if name == 'tiers': tiers = args
        elif name == 'machine': machine = args
        elif name == 'definition': definition = args
        elif name == 'tankScalingFunction': scaling = args
        elif name == 'addDefaultTooltips': default_tt = args.strip()
        elif name == 'addDefaultModel': pass
        else: return None
    if not (tiers and machine and definition): return None
    m = re.match(r'\s*\(\s*tier\s*,\s*builder\s*\)\s*=>\s*\{(.*)\}\s*$', definition, re.S)
    if not m: return None
    body = m.group(1).strip()
    if not body.startswith('builder'): return None
    chain = body[len('builder'):]
    # split the chain like a builder chain
    segs2 = []
    j = 0
    while True:
        k = j
        while k < len(chain) and chain[k] in ' \t\r\n': k += 1
        cm = ''
        while chain.startswith('//', k):
            e = chain.find('\n', k); e = len(chain) if e < 0 else e
            cm += chain[k:e] + '\n'; k = e
            while k < len(chain) and chain[k] in ' \t\r\n': k += 1
        if k >= len(chain): break
        if chain.startswith("['", k):
            e = chain.index("']", k); name = chain[k + 2:e].split('(')[0]; k = e + 2
            end = scan_balanced(chain, k)
            segs2.append((cm, name, chain[k + 1:end - 1])); j = end; continue
        m2 = re.match(r'\.(\w+)\(', chain[k:])
        if not m2: return None
        name = m2.group(1); p = k + m2.end() - 1
        end = scan_balanced(chain, p)
        segs2.append((cm, name, chain[p + 1:end - 1])); j = end
    lines = []
    for c, name, args in segs2:
        if name == 'tooltips': args = tr_tooltips_arg(args)
        name, a = tr_segment(name, args)
        a = a.replace(', null,', ', (String) null,').replace('(null,', '((String) null,') if name.endswith('HullModel') else a
        for l in c.split('\n'):
            if l.strip(): lines.append('                        ' + l.strip())
        a_lines = a.split('\n')
        a = '\n'.join([a_lines[0]] + [('        ' + l if l.strip() else l) for l in a_lines[1:]])
        lines.append('                        .%s(%s)' % (name, a))
    ts = tr_expr(tiers)
    mach = tr_expr(machine)
    extra = ''
    if default_tt == 'true' or scaling:
        extra = '%s, %s, ' % (default_tt or 'false', tr_expr(scaling) if scaling else 'GTMachineUtils.defaultTankSizeFunction')
    return 'AF9Machines.tiered("%s", new int[] { %s },\n                %s%s,\n                (tier, builder) -> builder\n%s);' % (mid, ts, extra, mach, '\n'.join(lines))

def semi_blocks(text):
    """`.pattern(definition -> {` statement blocks (JS, no semicolons) -> Java statements."""
    lines = text.split('\n')
    out, i = [], 0
    while i < len(lines):
        l = lines[i]
        out.append(l)
        i += 1
        if l.rstrip().endswith('definition -> {'):
            j = i
            while j < len(lines) and not lines[j].strip().startswith('})'): j += 1
            blk = lines[i:j]
            ind = min(len(x) - len(x.lstrip()) for x in blk if x.strip() and not x.strip().startswith('//'))
            starts = [k for k, x in enumerate(blk) if x.strip() and not x.strip().startswith(('//', '.', ')')) and len(x) - len(x.lstrip()) == ind]
            res = list(blk)
            for si, st in enumerate(starts):
                end = (starts[si + 1] if si + 1 < len(starts) else len(blk)) - 1
                while end > st and (not res[end].strip() or res[end].strip().startswith('//')): end -= 1
                if not res[end].rstrip().endswith(';'): res[end] = res[end].rstrip() + ';'
                res[st] = re.sub(r'^(\s*)(?:const|let)\s+', r'\1var ', res[st])
            out.extend(res)
            i = j
    return '\n'.join(out)

def main(path):
    src = open(path, encoding='utf-8').read()
    for m in re.finditer(r"const\s+(\$\w+)\s*=\s*Java\.loadClass\('([^']+)'\)", src):
        CLS[m.group(1)] = m.group(2).split('.')[-1]
    imports = {m.group(2) for m in re.finditer(r"const\s+(\$\w+)\s*=\s*Java\.loadClass\('([^']+)'\)", src)}
    fields = []
    while True:
        dm = re.search(r"\n[ ]*const ([A-Z][A-Z0-9_]*) = \[", src)
        if not dm: break
        lb = dm.end() - 1
        rb = scan_balanced(src, lb, '[', ']')
        raw = src[lb:rb]
        nested = raw[1:].lstrip().startswith('[')
        jv = convert_strings(raw.replace('[', '{').replace(']', '}'))
        fields.append('    private static final String%s %s = %s;' % ('[][]' if nested else '[]', dm.group(1), jv))
        src = src[:dm.start()] + src[rb:]
    blocks = list(re.finditer(r"GTCEuStartupEvents\.registry\('gtceu:machine', event => \{", src))
    if not blocks: sys.exit('no machine registry in ' + path)
    java = []
    for m in blocks:
        end = scan_balanced(src, src.index('(', m.start()))
        body = src[m.end():src.rindex('}', 0, end)]
        body = re.sub(r"\[((?:\d+,\s*)*\d+)\]\.map\(i =>\s*Component\.translatable\(`([^`$]*)\$\{i\}`\)\)",
                      lambda mm: "AF9Machines.lines('%s', %d)" % (mm.group(2), len(mm.group(1).split(','))), body)
        body = re.sub(r"let pattern = FactoryBlockPattern\.start\(\)\s*\n\s*(\w+)\.forEach\(aisle => \{ pattern = pattern\.aisle\(aisle\) \}\)",
                      r"var pattern = AF9Machines.aisles(FactoryBlockPattern.start(), \1)", body)
        body = template_to_java(body)
        body = re.sub(r"\n\s*const tooltips = \(key, count\) => \{.*?\n    \}\n", '\n', body, flags=re.S)
        i = 0
        while True:
            k = body.find('event.create(', i)
            if k < 0: break
            before = body[i:k]
            mid, kind, segs, j = parse_chain(body, k)
            i = j
            cm = '\n'.join('        ' + l.strip() for l in before.split('\n') if l.strip().startswith('//'))
            if cm: java.append(cm)
            if kind == 'multiblock': text = emit_multiblock(mid, segs)
            elif kind == 'custom': text = emit_custom(mid, segs)
            else: text = None
            if text is None:
                java.append('        // TODO port by hand: %s (%s)' % (mid, kind))
                continue
            java.append('        ' + semi_blocks(convert_strings(text)))
    cls = class_name(path)
    body_java = '\n\n'.join(java)
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
                    ('GTRecipeTypes', 'com.gregtechceu.gtceu.common.data.GTRecipeTypes'),
                    ('GTMachineUtils', 'com.gregtechceu.gtceu.common.data.machines.GTMachineUtils'),
                    ('GTValues', 'com.gregtechceu.gtceu.api.GTValues'),
                    ('GTCEu', 'com.gregtechceu.gtceu.GTCEu'),
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
    print('package com.af9.core.startup.machines;\n\n%s\n%s\npublic final class %s {\n\n    private %s() {}\n\n%spublic static void register() {\n%s\n    }\n}' % (imp_text, doc, cls, cls, ('\n\n'.join(fields) + '\n\n    ') if fields else '    ', body_java))

if __name__ == '__main__':
    main(sys.argv[1])
