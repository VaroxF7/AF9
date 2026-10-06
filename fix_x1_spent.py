#!/usr/bin/env python3
import json

with open('kubejs/assets/kubejs/lang/en_us.json', 'r') as f:
    lang = json.load(f)

# Fix X1 errors with correct coating amounts including correct spent solvent amounts
x1_fixes = {
    "af9.quest.litho.n200.1": "Fluids per print: 15 mB, 75 mB, 300 mB, 1500 mB, 1500 mB. Coater Track, per wafer: 60 mL, 90 mL, 150 mL; 45 spent solvent out.",
    "af9.quest.litho.n100.1": "Fluids per print: 23 mB, 113 mB, 450 mB, 2250 mB, 2250 mB. Coater Track, per wafer: 90 mL, 135 mL, 225 mL; 68 spent solvent out.",
    "af9.quest.litho.n80.1": "Fluids per print: 34 mB, 169 mB, 675 mB, 3375 mB, 3375 mB. Coater Track, per wafer: 135 mL, 203 mL, 338 mL; 101 spent solvent out.",
    "af9.quest.litho.n65.1": "Fluids per print: 51 mB, 253 mB, 1000 mB, 1013 mB, 5063 mB, 5063 mB. Coater Track, per wafer: 203 mL, 304 mL, 304 mL, 506 mL; 152 spent solvent out.",
    "af9.quest.litho.n50.1": "Fluids per print: 76 mB, 100 mB, 380 mB, 1000 mB, 1519 mB, 7594 mB, 7594 mB. Coater Track, per wafer: 304 mL, 456 mL, 456 mL, 759 mL; 228 spent solvent out.",
    "af9.quest.litho.n20.1": "Fluids per print: 100 mB, 144 mB, 570 mB, 1000 mB, 2278 mB, 11391 mB, 11391 mB. Coater Track, per wafer: 456 mL, 1139 mL; 342 spent solvent out.",
    "af9.quest.litho.n7.1": "Fluids per print: 100 mB, 144 mB, 854 mB, 1000 mB, 3417 mB, 17086 mB, 17086 mB. Coater Track, per wafer: 683 mL, 1709 mL; 513 spent solvent out.",
}

with open('kubejs/assets/kubejs/lang/en_us.json', 'r') as f:
    lang = json.load(f)

for key, value in x1_fixes.items():
    lang[key] = value
    print(f"Fixed: {key}")

with open('kubejs/assets/kubejs/lang/en_us.json', 'w', encoding='utf-8') as f:
    json.dump(lang, f, indent=1, ensure_ascii=False)

print(f"Fixed {len(x1_fixes)} X1 errors with correct spent solvent amounts")