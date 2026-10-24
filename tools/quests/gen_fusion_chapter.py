#!/usr/bin/env python3
"""Writes the Fusion Power chapter of the quest book: reactor, magnetic field, fuels, plasma turbines and the net-profit math.

    python3 tools/quests/gen_fusion_chapter.py [--force]

Writes config/ftbquests/quests/chapters/fusion_power.snbt and adds its texts (af9.quest.fusion.*) to
kubejs/assets/kubejs/lang/en_us.json. The numbers are those of kubejs/server_scripts/mods/gtceu/fusion_power.js and
docs/fusion-power.md (section 8 has the ledger). The chapter is edited in game, so a second run refuses to overwrite it.
"""
import hashlib
import json
import os
import sys

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), '../..')
OUT = os.path.join(ROOT, 'config/ftbquests/quests/chapters/fusion_power.snbt')
LANG = os.path.join(ROOT, 'kubejs/assets/kubejs/lang/en_us.json')
GROUP = '1DA67E79B40AB130'
P = 'af9.quest.fusion.'


def hid(*parts):
    h = hashlib.md5('|'.join(parts).encode()).hexdigest()[:16].upper()
    return str(int(h[0], 16) & 7) + h[1:]  # first digit 0-7: FTB Quests reads 8-F as a negative long


# key, x, y, size, shape, icon, task, deps, xp, title, subtitle, paragraphs
# task: ('check',) | ('item', id) | ('fluid', id, mB)
Q = [
    ('intro', 0, 0, 2.5, 'gear', 'gtceu:luv_fusion_reactor', ('check',), [], 100,
     'Fusion: Power, Not a Sink', 'Why the reactor pays for itself',
     ["A fusion reactor does not burn power to make power. It fuses fluids into &eplasma&r, and a &ePlasma Turbine&r burns that plasma for far more EU than the reaction and the magnet took. Every reaction in this pack is a net gain; this chapter shows where each number comes from.",
      "The loop: &6fuel fluids&r -> &6reactor&r (EU in, magnetic field) -> &6plasma&r -> &6Large Plasma Turbine&r (EU out). Profit = what the plasma is worth minus what the run and the field cost.",
      "Recipes: &7fusion_power.js&r. Rules and tables: &7docs/fusion-power.md&r."]),
    ('reactor', 3, 0, 1.5, 'square', 'gtceu:luv_fusion_reactor', ('item', 'gtceu:luv_fusion_reactor'), ['intro'], 50,
     'Fusion Reactor Mk1', 'LuV, the first ring',
     ["The &eFusion Reactor Mk1&r is a LuV machine on GT's 15 x 15 ring. It takes reactions up to a start class of &e160 M&r and makes helium and calcium plasma.",
      "&eHatches go on any casing block of the ring&r: one fluid input hatch, one fluid output hatch, one laser hatch, no maintenance hatch. Power comes from &etwo energy hatches&r or a laser hatch; any further energy hatch has to be set to &eAuxiliary&r (screwdriver), else the reactor builds no field. The reactor stores no EU: it has no start buffer, no heat and no standby draw."]),
    ('field', 6, 0, 1.5, 'square', 'gtceu:fusion_coil', ('check',), ['reactor'], 50,
     'The Magnetic Field', 'Upkeep instead of a start buffer',
     ["Every reaction needs a &emagnetic field&r before it starts, and each run &edrains&r some of it. The reactor rebuilds it from its energy hatches (the auxiliary ones first, set with a screwdriver) at &e1 A&r of its tier per tick until it is full.",
      "Mk1 / Mk2 / Mk3 hold &e1,000 / 4,000 / 16,000&r and build 1 % of that per tick; the Mega Fusion Reactor holds 64,000 and builds 640. A full field costs nothing.",
      "A reaction starts at &emin_field&r = start class / 160,000 (160 M = 1,000) and a run drains a quarter of that. The controller screen shows the field, the minimum and the drain."]),
    ('upkeep', 9, 0, 1.5, 'square', 'gtceu:fusion_casing', ('check',), ['field'], 50,
     'What the Field Costs', 'About 3,277 EU per point',
     ["Rebuilding one point of field costs &e3,277 EU&r on the Mk1, Mk2 and Mk3 (1 A of LuV / ZPM / UV over 10 / 40 / 160 points per tick) and &e819 EU&r on the Mega (1 A of UV over 640).",
      "So a run's upkeep is &edecay x 3,277 EU&r. Deuterium + tritium (min field 250, drain 62) costs about &e203,000 EU&r a run to keep the field up.",
      "Upkeep is small next to the output, but it is not zero: it is part of the ledger in the last quest."]),
    ('dt', 3, 3, 1.5, 'square', 'gtceu:helium_plasma_bucket', ('check',), ['reactor'], 75,
     'First Plasma', 'Deuterium + tritium',
     ["Fuse &e125 mB deuterium + 125 mB tritium&r into &e125 mB helium plasma&r (16 ticks at 4,096 EU/t = &e65,536 EU&r, start class 40 M).",
      "Deuterium comes from Glacio's ice and Mercury, tritium from the Space Elevator's type 6 mission or flare spallation at Mercury orbit. Collect the plasma in a tank, then tick this off."]),
    ('turbine', 6, 3, 1.5, 'square', 'gtceu:helium_plasma_bucket', ('check',), ['dt'], 75,
     'Burning Plasma', 'The Large Plasma Turbine',
     ["The &eLarge Plasma Turbine&r burns plasma as a generator fuel. 1 mB of helium plasma is worth &e81,920 EU&r, so the 125 mB of one run are worth &e10,240,000 EU&r (the generator's duration x 2,048 EU/t).",
      "The element comes back as its fluid (helium here), so the loop is a closed one. Rotor efficiency above 100 % burns the same plasma slower, never less EU per mB.",
      "The EU figures in this chapter are at the base 100 % rotor."]),
    ('dtmath', 9, 3, 2, 'square', 'minecraft:redstone', ('check',), ['turbine', 'upkeep'], 100,
     'Net Profit, Mk1 Edition', 'Deuterium + tritium: about 38x',
     ["One run: &e10,240,000 EU&r out of the turbine. In: &e65,536 EU&r for the run + about &e203,000 EU&r of field upkeep = &e268,700 EU&r.",
      "Net: &a+9,971,000 EU&r a run, a return of about &a38x&r. Run it in the 16-tick cycle and the reactor makes about &e640,000 EU/t&r of plasma while drawing 4,096 EU/t.",
      "Leave the field alone and nothing is lost: it only draws while below its capacity."]),
    ('he3', 3, 6, 1.5, 'square', 'gtceu:helium_3_bucket', ('fluid', 'gtceu:helium_3', 1000), ['dt'], 75,
     'Helium-3 Instead of Tritium', 'Same plasma, cheaper run',
     ["&e125 mB deuterium + 125 mB helium-3&r also make 125 mB helium plasma, at only &e1,920 EU/t&r (30,720 EU a run, start class 60 M). Field upkeep is about 305,000 EU.",
      "Net about &a+9.9 M EU&r a run (&a30x&r). Helium-3 comes from the Moon's atmosphere, the solar wind in orbit, or a type 5 Space Elevator mission."]),
    ('calcium', 6, 6, 1.5, 'square', 'gtceu:calcium_dust', ('check',), ['he3'], 75,
     'Calcium: The Weak One', 'Only about 1.9x',
     ["&e128 mB magnesium + 128 mB oxygen&r make &e16 mB calcium plasma&r (128 ticks at 7,680 EU/t = 983,040 EU, field upkeep about 613,000 EU), worth 3,014,656 EU.",
      "Net about &a+1.4 M EU&r a run, a return of only &e1.9x&r. It is the lowest-profit reaction in the pack: burn it only when magnesium and oxygen are spare."]),
    ('mk2', 12, 3, 1.5, 'square', 'gtceu:zpm_fusion_reactor', ('item', 'gtceu:zpm_fusion_reactor'), ['dtmath'], 75,
     'Fusion Reactor Mk2', 'ZPM, field 4,000',
     ["The &eMk2&r runs everything the Mk1 does plus start classes up to &e320 M&r. Its field holds 4,000 and builds 40 per tick.",
      "New plasmas: &esulfur&r (aluminium + lithium), &ezinc&r (copper + tritium) and &eniobium&r (cobalt + silicon)."]),
    ('mk2fuel', 15, 3, 1.5, 'square', 'gtceu:niobium_dust', ('check',), ['mk2'], 100,
     'Niobium and Sulfur Plasma', 'Dense fuel, big margins',
     ["Niobium: 144 mB cobalt + 144 mB silicon -> 144 mB plasma, 16 ticks at 49,152 EU/t. Out &e38.9 M EU&r (270,336 EU/mB), in 786,432 + about 1.0 M upkeep: &a+37 M EU&r, about &a21x&r.",
      "Sulfur: 16 mB aluminium + 16 mB lithium -> 144 mB, 32 ticks at 10,240 EU/t. Out &e24.5 M EU&r for about 1.56 M in: &a+22.9 M EU&r, about &a16x&r.",
      "Zinc is the weakest of the three (about 9.5x) but it is the key input of bismuth."]),
    ('mk3', 18, 3, 1.5, 'square', 'gtceu:uv_fusion_reactor', ('item', 'gtceu:uv_fusion_reactor'), ['mk2fuel'], 75,
     'Fusion Reactor Mk3', 'UV, field 16,000',
     ["The &eMk3&r takes start classes up to &e640 M&r, holds a field of 16,000 and builds 160 per tick (still 3,277 EU a point).",
      "New plasmas: &esilver&r (gold + arsenic), &ebismuth&r (tantalum + zinc plasma) and &eradon&r (iridium + fluorine). Fluorine is Venus's."]),
    ('bismuth', 21, 3, 1.5, 'square', 'gtceu:bismuth_dust', ('check',), ['mk3'], 100,
     'A Plasma That Eats Plasma', 'Bismuth needs zinc plasma',
     ["Tantalum 144 mB + &ezinc plasma 72 mB&r -> bismuth plasma 144 mB, 16 ticks at 98,304 EU/t. The zinc plasma is 16.2 M EU of fuel you did not burn.",
      "Honest ledger: 1.57 M run + 1.79 M upkeep + 16.2 M of zinc plasma = &e19.6 M EU&r in, 61.3 M out: &a+41.8 M EU&r, about &a3.1x&r. Still positive, but the thinnest margin of the Mk3.",
      "Silver (about 16x) and radon (about 12x) need no plasma input."]),
    ('mega', 24, 3, 1.5, 'square', 'gtceu:mega_fusion_reactor', ('check',), ['bismuth'], 100,
     'Mega Fusion Reactor', 'UIV, field 64,000',
     ["The &eMega Fusion Reactor&r holds a field of 64,000 and rebuilds it at 640 per tick for 1 A of UV: only &e819 EU&r a point. It runs every Mk1-Mk3 recipe in half the time on 1.5 times the power.",
      "Its own recipes are gated by &evoltage&r (UHV and UEV), not by start class: no reaction asks above 640 M."]),
    ('megafuel', 27, 3, 2, 'square', 'gtceu:plutonium_241_dust', ('check',), ['mega'], 150,
     'Lead, Thorium, Plutonium', 'Hundreds of millions per run',
     ["Lead: platinum + beryllium (576 mB each) -> 576 mB, 8 ticks at 1.97 M EU/t. Out &e244 M EU&r for 16.4 M in: &a+228 M EU&r, about &a15x&r.",
      "Thorium: osmium + silicon -> out 271 M, in 32.2 M: &a+239 M EU&r, about &a8x&r.",
      "Plutonium-241: lutetium + vanadium -> out 287 M, in 63.7 M: &a+223 M EU&r, about &a4.5x&r. The richest fuel per mB (497,664 EU) has the thinnest margin of the three, because the UEV run costs the most."]),
    ('star', 24, 6, 1.5, 'square', 'gtceu:star_matter_plasma_bucket', ('check',), ['mega'], 100,
     'Star Matter', 'The 1.6 M EU per mB fuel',
     ["&eStar matter plasma&r burns for &e1,638,400 EU/mB&r. A Mega run of 1,000 mB takes helium, nitrogen, oxygen and iron plasma worth 1.35 GEU, so it returns &e1.21x&r.",
      "The cheap route is a Mk-IV Space Elevator mission to the termination shock, which brings the ingredients as plasma already. The quark plasmas and Idontknowium burn nowhere: do not try."]),
    ('ramp', 3, -3, 1.5, 'square', 'minecraft:clock', ('check',), ['reactor'], 50,
     'Warm-Up', 'Up to 25 % faster',
     ["A reactor that runs without a pause speeds up: after &e30 minutes&r of continuous running a run takes up to &e25 % less time&r. EU/t never changes, so the energy per run falls with it.",
      "It resets when inputs run out, the structure breaks or you suspend the machine. Plan a continuous supply of fuel."]),
    ('ledger', 30, 0, 2.5, 'gear', 'minecraft:emerald', ('check',), ['megafuel', 'star'], 200,
     'The Ledger', 'Every reaction, in and out',
     ["&6Net per run = plasma value - run EU - field upkeep (- plasma inputs).&r Return = out / in:",
      "D+T 38x | D+He-3 30x | Calcium 1.9x | Sulfur 16x | Zinc 9.5x | Niobium 21x | Silver 16x | Bismuth 3.1x | Radon 12x | Lead 15x | Thorium 8x | Plutonium-241 4.5x.",
      "Not counted: making the input fluids (electrolysis, mining, atmospheres) and the turbine's rotor losses. Fusion is a &enet EU gain at the reactor&r; what a given line nets depends on how cheaply you make its fuel, which is why Glacio's deuterium and the Moon's helium-3 are the Mk1's best friends.",
      "Finish this and you know fusion pays. Go and build the biggest one."]),
]


def gen():
    keys = {q[0]: hid('fusion', q[0]) for q in Q}
    lang = {}
    lines = ['{',
             '\tdefault_hide_dependency_lines: false',
             '\tdefault_quest_shape: ""',
             '\tfilename: "fusion_power"',
             f'\tgroup: "{GROUP}"',
             '\ticon: "gtceu:uv_fusion_reactor"',
             f'\tid: "{hid("fusion", "chapter")}"',
             '\torder_index: 21',
             '\tprogression_mode: "flexible"',
             '\tquest_links: [ ]',
             '\tquests: [']
    for key, x, y, size, shape, icon, task, deps, xp, title, sub, paras in Q:
        lang[f'{P}{key}.title'] = title
        lang[f'{P}{key}.subtitle'] = sub
        desc = []
        for n, text in enumerate(paras, 1):
            lang[f'{P}{key}.{n}'] = text
            if desc:
                desc.append('\t\t\t\t""')
            desc.append(f'\t\t\t\t"{{{P}{key}.{n}}}"')
        lines.append('\t\t{')
        if deps:
            lines.append('\t\t\tdependencies: [' + ', '.join(f'"{keys[d]}"' for d in deps) + ']')
        lines += ['\t\t\tdescription: [', *desc, '\t\t\t]',
                  f'\t\t\ticon: "{icon}"',
                  f'\t\t\tid: "{keys[key]}"',
                  '\t\t\trewards: [{',
                  f'\t\t\t\tid: "{hid("fusion", key, "rw")}"',
                  '\t\t\t\ttype: "xp"',
                  f'\t\t\t\txp: {xp}',
                  '\t\t\t}]',
                  f'\t\t\tshape: "{shape}"',
                  f'\t\t\tsize: {size}d',
                  f'\t\t\tsubtitle: "{{{P}{key}.subtitle}}"',
                  '\t\t\ttasks: [',
                  '\t\t\t\t{']
        tid = hid('fusion', key, 'task')
        if task[0] == 'check':
            lines += [f'\t\t\t\t\tid: "{tid}"', '\t\t\t\t\ttype: "checkmark"']
        elif task[0] == 'item':
            lines += [f'\t\t\t\t\tid: "{tid}"', f'\t\t\t\t\titem: "{task[1]}"', '\t\t\t\t\ttype: "item"']
        else:
            lines += [f'\t\t\t\t\tamount: {task[2]}L', f'\t\t\t\t\tfluid: "{task[1]}"', f'\t\t\t\t\tid: "{tid}"',
                      '\t\t\t\t\ttype: "fluid"']
        lines += ['\t\t\t\t}', '\t\t\t]',
                  f'\t\t\ttitle: "{{{P}{key}.title}}"',
                  f'\t\t\tx: {float(x)}d', f'\t\t\ty: {float(y)}d', '\t\t}']
    lang[f'{P}chapter.title'] = 'Fusion Power'
    lang[f'{P}chapter.subtitle'] = 'Reactor, magnets, plasma and the profit math'
    lines += ['\t]', f'\tsubtitle: ["{{{P}chapter.subtitle}}"]', f'\ttitle: "{{{P}chapter.title}}"', '}']
    return '\n'.join(lines) + '\n', lang


def main():
    if os.path.exists(OUT) and '--force' not in sys.argv:
        sys.exit('fusion_power.snbt exists (edited in game): --force overwrites it')
    snbt, lang = gen()
    with open(OUT, 'w', encoding='utf-8', newline='\n') as f:
        f.write(snbt)
    text = open(LANG, encoding='utf-8').read().rstrip()
    assert text.endswith('}')
    new = [f'\t{json.dumps(k)}: {json.dumps(v, ensure_ascii=False)}' for k, v in lang.items() if f'"{k}"' not in text]
    body = text[:-1].rstrip() + (',\n' + ',\n'.join(new) if new else '') + '\n}\n'
    json.loads(body)
    open(LANG, 'w', encoding='utf-8', newline='\n').write(body)


if __name__ == '__main__':
    main()
