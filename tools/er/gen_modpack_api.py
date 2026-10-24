#!/usr/bin/env python3
"""Writes config/extremereactors/modpack_api.json (Extreme Reactors 2's ModPack API file) and the tag lists the KubeJS side needs.

    python3 tools/er/gen_modpack_api.py            # from the repository root

Spec: docs/extreme-reactors.md. The fuel ladder is one table here: the rod, the spent rod, the numbers. Extreme Reactors' own
fuels (yellorium, blutonium, verderium) are not touched; these are added above them. Run it after editing the table.
"""
import json
from pathlib import Path

# id, fuel rod, spent rod, colour, moderation, absorption, hardness, reactivity, fission rate
# Extreme Reactors' own: yellorium 1.5/0.5/1.0 (reactivity 1.05, rate 0.01), blutonium 2.23/0.6/2.0 (1.0871, 0.051),
# verderium 3.74/0.874/2.005 (1.0984, 0.0743). Ours start between yellorium and blutonium and end above verderium.
FUELS = [
    ('leu', 'fx_fuel_rod', 'fx_spent_fuel_rod', 0xC8D63C, 1.8, 0.55, 1.5, 1.060, 0.020),
    ('thorium', 'fx_thorium_rod', 'fx_spent_thorium_rod', 0x8FC48A, 1.6, 0.50, 1.2, 1.065, 0.012),
    ('mox', 'fx_mox_rod', 'fx_spent_mox_rod', 0xA8FF4C, 2.0, 0.60, 2.0, 1.080, 0.040),
    ('mox_2', 'fx_mox_2_rod', 'fx_spent_mox_2_rod', 0x9AEA45, 2.1, 0.62, 2.2, 1.090, 0.050),
    ('heu', 'fx_heu_rod', 'fx_spent_heu_rod', 0x46FA46, 2.2, 0.62, 2.2, 1.090, 0.050),
    ('lep', 'fx_lep_rod', 'fx_spent_lep_rod', 0xFF7A4C, 2.3, 0.65, 2.4, 1.095, 0.060),
    ('mox_3', 'fx_mox_3_rod', 'fx_spent_mox_3_rod', 0x8CD93E, 2.4, 0.66, 2.5, 1.100, 0.060),
    ('hep', 'fx_hep_rod', 'fx_spent_hep_rod', 0xFF4C4C, 2.6, 0.70, 2.8, 1.105, 0.075),
    ('mox_4', 'fx_mox_4_rod', 'fx_spent_mox_4_rod', 0x7EC837, 2.8, 0.72, 3.0, 1.110, 0.070),
    ('np', 'fx_np_rod', 'fx_spent_np_rod', 0x2FD8C8, 4.0, 0.85, 3.0, 1.120, 0.085),
    ('am', 'fx_am_rod', 'fx_spent_am_rod', 0xE6A23C, 4.4, 0.88, 3.4, 1.135, 0.100),
    ('cm', 'fx_cm_rod', 'fx_spent_cm_rod', 0xA06CE0, 4.8, 0.92, 3.8, 1.150, 0.120),
    ('cf', 'fx_cf_rod', 'fx_spent_cf_rod', 0xE0364A, 5.2, 0.95, 4.2, 1.180, 0.150),
]


def section(add, remove=None):
    return {'WipeExistingValuesBeforeAdding': False, 'Add': add, 'Remove': remove or []}


def main():
    reactants, sources, reactions = [], [], []
    for fid, rod, spent, colour, mod, absorb, hard, react, rate in FUELS:
        reactants.append({'Name': f'af9_{fid}', 'TranslationKey': f'reactant.af9.{fid}', 'IsFuel': True, 'RgbColour': colour,
                          'FuelProperties': {'ModerationFactor': mod, 'AbsorptionCoefficient': absorb, 'HardnessDivisor': hard}})
        reactants.append({'Name': f'af9_{fid}_waste', 'TranslationKey': f'reactant.af9.{fid}_waste', 'IsFuel': False,
                          'RgbColour': colour // 3})
        # one rod is one fuel unit (1,000 mB, Extreme Reactors' ingot), one spent rod one unit of waste
        sources.append({'SourceTagId': f'af9:er/fuel/{fid}', 'ProductName': f'af9_{fid}', 'ProductQuantity': 1000})
        sources.append({'SourceTagId': f'af9:er/waste/{fid}', 'ProductName': f'af9_{fid}_waste', 'ProductQuantity': 1000})
        reactions.append({'SourceReactant': f'af9_{fid}', 'ProductReactant': f'af9_{fid}_waste', 'Reactivity': react,
                          'FissionRate': rate})

    api = {
        'Enabled': True,
        # coolants: distilled water boils to supercritical steam (registered by AF9 Core), FLiBe to hot FLiBe for the Heat Exchanger.
        # Enthalpy is the heat one mB of vapor takes: 100 per mB of supercritical steam (80 EU, 320 FE) is the 3.2 FE per heat
        # of Extreme Reactors' own water (4 per mB of steam, 10 FE); hot FLiBe is 92.16 mB of steam a mB, as the Heat Exchanger
        # turns it (docs/nuclear-rework.md)
        'Coolants': section([
            {'Name': 'af9_distilled_water', 'BoilingPoint': 374.0, 'EnthalpyOfVaporization': 100.0,
             'TranslationKey': 'coolant.af9.distilled_water'},
            {'Name': 'af9_flibe', 'BoilingPoint': 1400.0, 'EnthalpyOfVaporization': 9216.0,
             'TranslationKey': 'coolant.af9.flibe'}]),
        'Vapors': section([
            {'Name': 'af9_hot_flibe', 'FluidEnergyDensity': 1.0, 'TranslationKey': 'vapor.af9.hot_flibe'}]),
        'CoolantSources': section([
            {'SourceTagId': 'af9:er/coolant/distilled_water', 'ProductName': 'af9_distilled_water', 'ProductQuantity': 1},
            {'SourceTagId': 'af9:er/coolant/flibe', 'ProductName': 'af9_flibe', 'ProductQuantity': 1}]),
        'VaporSources': section([
            {'SourceTagId': 'af9:er/vapor/hot_flibe', 'ProductName': 'af9_hot_flibe', 'ProductQuantity': 1}]),
        'FluidTransitions': section([
            {'Source': 'af9_distilled_water', 'SourceQuantity': 1, 'Product': 'supercritical_steam', 'ProductQuantity': 1},
            {'Source': 'af9_flibe', 'SourceQuantity': 1, 'Product': 'af9_hot_flibe', 'ProductQuantity': 1}]),
        'ReactorReactants': section(reactants),
        'ReactorReactantSources': section(sources),
        'ReactorReactantReaction': section(reactions),
        # Extreme Reactors' graphite block: 0.10 / 0.50 / 2.00 / 2.0. The nuclear one slows better and carries heat away faster
        'ReactorSolidModerators': section([
            {'TagId': 'af9:er/moderator/nuclear_graphite', 'Absorption': 0.10, 'HeatEfficiency': 0.60, 'Moderation': 3.0,
             'HeatConductivity': 2.5}]),
    }
    out = Path('config/extremereactors/modpack_api.json')
    out.write_text(json.dumps(api, indent=2) + '\n')

    # the same table for kubejs/server_scripts/mods/extremereactors/er_bridge.js
    table = ',\n'.join(f"    ['{f[0]}', 'af9:{f[1]}', 'af9:{f[2]}']" for f in FUELS)
    print('const FUELS = [\n' + table + '\n]')


if __name__ == '__main__':
    main()
