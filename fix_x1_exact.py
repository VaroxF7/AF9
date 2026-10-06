#!/usr/bin/env python3
import json

with open('kubejs/assets/kubejs/lang/en_us.json', 'r') as f:
    lang = json.load(f)

# Fix X1 errors with exact values from recipes
x1_fixes = {
    "af9.quest.litho.n200.1": "Fluids per print: the text says [200] mB per print, the recipe af9:print_*_200nm takes [15, 75, 300, 1500, 1500]. Coater Track, per wafer: the text says coating [200] + 1 spent solvent, the recipe af9:coat_phosphorus_wafer has [60, 90, 150] + [45].",
    "af9.quest.litho.n100.1": "Fluids per print: the text says [150] mB per print, the recipe af9:print_*_100nm takes [23, 113, 450, 2250, 2250]. Coater Track, per wafer: the text says coating [150] + 1 spent solvent, the recipe af9:coat_naquadah_wafer has [90, 135, 225] + [68].",
    "af9.quest.litho.n80.1": "Fluids per print: the text says [120] mB per print, the recipe af9:print_*_80nm takes [34, 169, 675, 3375, 3375]. Coater Track, per wafer: the text says coating [120] + 1 spent solvent, the recipe af9:coat_trinium_wafer has [135, 203, 338] + [101].",
    "af9.quest.litho.n65.1": "Fluids per print: the text says [100] mB per print, the recipe af9:print_*_65nm takes [51, 253, 1000, 1013, 5063, 5063]. Coater Track, per wafer: the text says coating [100] + 1 spent solvent, the recipe af9:coat_naquadria_wafer has [203, 304, 304, 506] + [152].",
    "af9.quest.litho.n50.1": "Fluids per print: the text says [80] mB per print, the recipe af9:print_*_50nm takes [76, 100, 380, 1000, 1519, 7594, 7594]. Coater Track, per wafer: the text says coating [80] + 1 spent solvent, the recipe af9:coat_neutronium_wafer has [304, 456, 456, 759] + [228].",
    "af9.quest.litho.n20.1": "Fluids per print: the text says [40] mB per print, the recipe af9:print_*_20nm takes [100, 144, 570, 1000, 2278, 11391, 11391]. Coater Track, per wafer: the text says coating [40] + 1 spent solvent, the recipe af9:coat_transmuted_neutronium_wafer has [456, 1139] + [342].",
    "af9.quest.litho.n7.1": "Fluids per print: the text says [20] mB per print, the recipe af9:print_*_7nm takes [100, 144, 854, 1000, 3417, 17086, 17086]. Coater Track, per wafer: the text says coating [20] + 1 spent solvent, the recipe af9:coat_strange_matter_wafer has [683, 1709] + [513].",
}

with open('kubejs/assets/kubejs/lang/en_us.json', 'r') as f:
    lang = json.load(f)

for key, value in x1_fixes.items():
    lang[key] = value
    print(f"Fixed: {key}")

with open('kubejs/assets/kubejs/lang/en_us.json', 'w', encoding='utf-8') as f:
    json.dump(lang, f, indent=1, ensure_ascii=False)

print(f"Fixed {len(x1_fixes)} X1 errors with exact recipe values")