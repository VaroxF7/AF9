#!/usr/bin/env python3
import json

with open('kubejs/assets/kubejs/lang/en_us.json', 'r') as f:
    lang = json.load(f)

# Fix X1 errors: Exact format required by facts linter
# Format: "Fluids per print: X mB drilling fluid per wafer. Coater Track, per wafer: X mL; N spent solvent out."
x1_fixes = {
    "af9.quest.litho.n200.1": "Fluids per print: 200 mB drilling fluid per wafer. Coater Track, per wafer: 200 mL; 1 spent solvent out.",
    "af9.quest.litho.n100.1": "Fluids per print: 150 mB drilling fluid per wafer. Coater Track, per wafer: 150 mL; 1 spent solvent out.",
    "af9.quest.litho.n80.1": "Fluids per print: 120 mB drilling fluid per wafer. Coater Track, per wafer: 120 mL; 1 spent solvent out.",
    "af9.quest.litho.n65.1": "Fluids per print: 100 mB drilling fluid per wafer. Coater Track, per wafer: 100 mL; 1 spent solvent out.",
    "af9.quest.litho.n50.1": "Fluids per print: 80 mB drilling fluid per wafer. Coater Track, per wafer: 80 mL; 1 spent solvent out.",
    "af9.quest.litho.n20.1": "Fluids per print: 40 mB drilling fluid per wafer. Coater Track, per wafer: 40 mL; 1 spent solvent out.",
    "af9.quest.litho.n7.1": "Fluids per print: 20 mB drilling fluid per wafer. Coater Track, per wafer: 20 mL; 1 spent solvent out.",
}

# Load the lang file
with open('kubejs/assets/kubejs/lang/en_us.json', 'r') as f:
    lang = json.load(f)

# Apply X1 fixes
for key, value in x1_fixes.items():
    lang[key] = value
    print(f"Fixed: {key}")

# Save the updated lang file
with open('kubejs/assets/kubejs/lang/en_us.json', 'w', encoding='utf-8') as f:
    json.dump(lang, f, indent=1, ensure_ascii=False)

print(f"Fixed {len(x1_fixes)} X1 errors")