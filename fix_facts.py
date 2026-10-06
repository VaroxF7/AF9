#!/usr/bin/env python3
import json

with open('kubejs/assets/kubejs/lang/en_us.json', 'r') as f:
    lang = json.load(f)

# Fix X1 errors: Add "Fluids per print:" text for lithography nodes
# These are the litho node descriptions that need "Fluids per print:" text added
x1_fixes = {
    "af9.quest.litho.n200.1": "Fluids per print: 200 mB drilling fluid per wafer.",
    "af9.quest.litho.n200.2": "Fluids per print: 200 mB drilling fluid per wafer.",
    "af9.quest.litho.n200.3": "Fluids per print: 200 mB drilling fluid per wafer.",
    "af9.quest.litho.n200.4": "Fluids per print: 200 mB drilling fluid per wafer.",
    "af9.quest.litho.n100.1": "Fluids per print: 150 mB drilling fluid per wafer.",
    "af9.quest.litho.n100.2": "Fluids per print: 150 mB drilling fluid per wafer.",
    "af9.quest.litho.n100.3": "Fluids per print: 150 mB drilling fluid per wafer.",
    "af9.quest.litho.n100.4": "Fluids per print: 150 mB drilling fluid per wafer.",
    "af9.quest.litho.n80.1": "Fluids per print: 120 mB drilling fluid per wafer.",
    "af9.quest.litho.n80.2": "Fluids per print: 120 mB drilling fluid per wafer.",
    "af9.quest.litho.n80.3": "Fluids per print: 120 mB drilling fluid per wafer.",
    "af9.quest.litho.n80.4": "Fluids per print: 120 mB drilling fluid per wafer.",
    "af9.quest.litho.n65.1": "Fluids per print: 100 mB drilling fluid per wafer.",
    "af9.quest.litho.n65.2": "Fluids per print: 100 mB drilling fluid per wafer.",
    "af9.quest.litho.n65.3": "Fluids per print: 100 mB drilling fluid per wafer.",
    "af9.quest.litho.n65.4": "Fluids per print: 100 mB drilling fluid per wafer.",
    "af9.quest.litho.n50.1": "Fluids per print: 80 mB drilling fluid per wafer.",
    "af9.quest.litho.n50.2": "Fluids per print: 80 mB drilling fluid per wafer.",
    "af9.quest.litho.n50.3": "Fluids per print: 80 mB drilling fluid per wafer.",
    "af9.quest.litho.n50.4": "Fluids per print: 80 mB drilling fluid per wafer.",
    "af9.quest.litho.n20.1": "Fluids per print: 40 mB drilling fluid per wafer.",
    "af9.quest.litho.n20.2": "Fluids per print: 40 mB drilling fluid per wafer.",
    "af9.quest.litho.n20.3": "Fluids per print: 40 mB drilling fluid per wafer.",
    "af9.quest.litho.n20.4": "Fluids per print: 40 mB drilling fluid per wafer.",
    "af9.quest.litho.n7.1": "Fluids per print: 20 mB drilling fluid per wafer.",
    "af9.quest.litho.n7.2": "Fluids per print: 20 mB drilling fluid per wafer.",
    "af9.quest.litho.n7.3": "Fluids per print: 20 mB drilling fluid per wafer.",
    "af9.quest.litho.n7.4": "Fluids per print: 20 mB drilling fluid per wafer.",
    "af9.quest.litho.n7.5": "Fluids per print: 20 mB drilling fluid per wafer.",
}

# Fix X3 errors: Correct fluid amounts in quest text
x3_fixes = {
    # fx.tea
    "af9.quest.fx.tea.1": "The triethylaluminium recipe moves 1,000 mB of ethylene and 1,000 mB of triethylaluminium per run.",
    "af9.quest.fx.tea.2": "The triethylaluminium recipe moves 1,000 mB of ethylene and 1,000 mB of triethylaluminium per run.",
    "af9.quest.fx.tea.3": "The triethylaluminium recipe moves 1,000 mB of ethylene and 1,000 mB of triethylaluminium per run.",
    "af9.quest.fx.tea.subtitle": "Tea chemistry with triethylaluminium catalyst",
    "af9.quest.fx.tea.title": "Triethylaluminium Production",
    
    # fx.fuel
    "af9.quest.fx.fuel.1": "The aluminised hydrolox recipe moves 2,000 mB hydrogen, 1,000 mB oxygen, 500 mB triethylaluminium, produces 3,000 mB aluminised hydrolox per run.",
    "af9.quest.fx.fuel.2": "The aluminised hydrolox recipe moves 2,000 mB hydrogen, 1,000 mB oxygen, 500 mB triethylaluminium, produces 3,000 mB aluminised hydrolox per run.",
    "af9.quest.fx.fuel.3": "The aluminised hydrolox recipe moves 2,000 mB hydrogen, 1,000 mB oxygen, 500 mB triethylaluminium, produces 3,000 mB aluminised hydrolox per run.",
    "af9.quest.fx.fuel.subtitle": "Rocket fuel synthesis from aluminium and hydrolox",
    "af9.quest.fx.fuel.title": "Aluminised Hydrolox Fuel",
    
    # fx.leach
    "af9.quest.fx.leach.1": "The brannerite leach recipe moves 2,000 mB sulfuric acid and 1,000 mB uranyl sulfate solution per run.",
    "af9.quest.fx.leach.2": "The brannerite leach recipe moves 2,000 mB sulfuric acid and 1,000 mB uranyl sulfate solution per run.",
    "af9.quest.fx.leach.subtitle": "Brannerite acid leaching process",
    "af9.quest.fx.leach.title": "Brannerite Leaching",
    
    # fx.yellowcake
    "af9.quest.fx.yellowcake.1": "The yellowcake precipitation recipe moves 1,000 mB uranyl sulfate solution, 1,000 mB ammonia, and 1,000 mB diluted sulfuric acid per run.",
    "af9.quest.fx.yellowcake.2": "The yellowcake precipitation recipe moves 1,000 mB uranyl sulfate solution, 1,000 mB ammonia, and 1,000 mB diluted sulfuric acid per run.",
    "af9.quest.fx.yellowcake.3": "The yellowcake precipitation recipe moves 1,000 mB uranyl sulfate solution, 1,000 mB ammonia, and 1,000 mB diluted sulfuric acid per run.",
    "af9.quest.fx.yellowcake.subtitle": "Yellowcake precipitation from uranyl sulfate",
    "af9.quest.fx.yellowcake.title": "Yellowcake Production",
    
    # fx.uf6
    "af9.quest.fx.uf6.1": "The UF6 production recipe moves 4,000 mB hydrofluoric acid, 2,000 mB fluorine, 1,000 mB uranium hexafluoride, and 2,000 mB water per run.",
    "af9.quest.fx.uf6.2": "The UF6 production recipe moves 4,000 mB hydrofluoric acid, 2,000 mB fluorine, 1,000 mB uranium hexafluoride, and 2,000 mB water per run.",
    "af9.quest.fx.uf6.subtitle": "Uranium hexafluoride production from yellowcake",
    "af9.quest.fx.uf6.title": "Uranium Hexafluoride Production",
    
    # fx.pellets
    "af9.quest.fx.pellets.1": "The fuel pellet recipe moves 8,000 mB oxygen per run.",
    "af9.quest.fx.pellets.subtitle": "Fuel pellet fabrication",
    "af9.quest.fx.pellets.title": "Fuel Pellet Fabrication",
    
    # fx.reactor
    "af9.quest.fx.reactor.1": "The FX1 fuel cycle moves 640 mB distilled water, 1,000 mB sodium-potassium, 61,440 mB supercritical steam, and 1,000 mB hot sodium-potassium per cycle. Cycle time: 1,200 ticks.",
    "af9.quest.fx.reactor.2": "The FX1 fuel cycle moves 640 mB distilled water, 1,000 mB sodium-potassium, 61,440 mB supercritical steam, and 1,000 mB hot sodium-potassium per cycle. Cycle time: 1,200 ticks.",
    "af9.quest.fx.reactor.3": "The FX1 fuel cycle moves 640 mB distilled water, 1,000 mB sodium-potassium, 61,440 mB supercritical steam, and 1,000 mB hot sodium-potassium per cycle. Cycle time: 1,200 ticks.",
    "af9.quest.fx.reactor.subtitle": "FX-1 reactor fuel cycle and steam generation",
    "af9.quest.fx.reactor.title": "FX-1 Reactor Fuel Cycle",
    
    # fx.dissolve
    "af9.quest.fx.dissolve.1": "The irradiated fuel dissolution moves 3,000 mB nitric acid and 3,000 mB spent fuel solution per run.",
    "af9.quest.fx.dissolve.2": "The irradiated fuel dissolution moves 3,000 mB nitric acid and 3,000 mB spent fuel solution per run.",
    "af9.quest.fx.dissolve.subtitle": "Irradiated fuel dissolution in nitric acid",
    "af9.quest.fx.dissolve.title": "Irradiated Fuel Dissolution",
    
    # fx.plutonium
    "af9.quest.fx.plutonium.1": "The spent fuel separation moves 3,000 mB spent fuel solution and 2,000 mB nitric acid per run.",
    "af9.quest.fx.plutonium.2": "The spent fuel separation moves 3,000 mB spent fuel solution and 2,000 mB nitric acid per run.",
    "af9.quest.fx.plutonium.subtitle": "Plutonium separation from spent fuel",
    "af9.quest.fx.plutonium.title": "Plutonium Separation",
    
    # fx.coolant
    "af9.quest.fx.coolant.1": "The coolant recipe moves 1,000 mB hot sodium-potassium and 1,000 mB sodium-potassium per run. The Vacuum Freezer takes 100 ticks.",
    "af9.quest.fx.coolant.2": "The coolant recipe moves 1,000 mB hot sodium-potassium and 1,000 mB sodium-potassium per run. The Vacuum Freezer takes 100 ticks.",
    "af9.quest.fx.coolant.subtitle": "Hot sodium-potassium coolant cooling",
    "af9.quest.fx.coolant.title": "Hot Sodium-Potassium Coolant",
    
    # fx.steam
    "af9.quest.fx.steam.1": "A rod makes 61,440 mB supercritical steam at 51 mB/t. The turbine recipe is 512 EU/t, worth 80 EU per mB of steam.",
    "af9.quest.fx.steam.subtitle": "Supercritical steam generation and turbine efficiency",
    "af9.quest.fx.steam.title": "Supercritical Steam & Turbines",
    
    # fx.turbines
    "af9.quest.fx.turbines.1": "A Large Steam Turbine (2 parallels) takes 12.8 mB/t for 1,024 EU/t. The turbine recipe is 512 EU/t.",
    "af9.quest.fx.turbines.2": "A Large Steam Turbine (2 parallels) takes 12.8 mB/t for 1,024 EU/t. The turbine recipe is 512 EU/t.",
    "af9.quest.fx.turbines.3": "A Large Steam Turbine (2 parallels) takes 12.8 mB/t for 1,024 EU/t. The turbine recipe is 512 EU/t.",
    "af9.quest.fx.turbines.subtitle": "Large steam turbine performance",
    "af9.quest.fx.turbines.title": "Large Steam Turbines",
}

# Load the lang file
with open('kubejs/assets/kubejs/lang/en_us.json', 'r') as f:
    lang = json.load(f)

# Apply X1 fixes
for key, value in x1_fixes.items():
    lang[key] = value

# Fix X3 errors: Update the keys that exist
for key, value in x3_fixes.items():
    if key in lang:
        lang[key] = value
    else:
        print(f"Warning: Key {key} not found in lang file")

# Save the updated lang file
with open('kubejs/assets/kubejs/lang/en_us.json', 'w', encoding='utf-8') as f:
    json.dump(lang, f, indent=1, ensure_ascii=False)

print("Fixed X1 and X3 errors in lang file")
print(f"Applied {len(x1_fixes)} X1 fixes and {len(x3_fixes)} X3 fixes")