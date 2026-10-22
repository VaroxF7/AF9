# Synthetic titanium (HV to the end of EV)

There is no titanium ore until Venus: the only ilmenite is the Venus vein (`af9:venus_ilmenite_vein`, `vein_venus.js`; Mars's
ilmenite vein and its Mars-sand recipes are gone). The tier 3 rocket (which needs titanium) therefore comes from two synthetic
chains that both end in GT's rutile dust; GT's chlorination and Kroll run unchanged from there. Recipes:
`kubejs/server_scripts/mods/gtceu/titanium_synthetic.js`; materials: `AF9Materials.syntheticTitanium()`.
The Earth-side loopholes stay closed (`planet_metals.js`: bauxite electrolysis and sludge, end stone, brannerite give no rutile).

## Stage 1, HV: lunar regolith (poor: about 64 moon sand per titanium)

| step | machine | in -> out |
|---|---|---|
| beneficiation | electromagnetic separator | 32 moon sand -> lunar ilmenite concentrate, 16 silica, 25 % iron |
| hydrogen reduction | EBF 1800 K | 2 concentrate + 4,000 mB hydrogen -> reduced titania slag, 2 iron, steam |
| sulfate digest | chemical reactor | slag + 2,000 mB sulfuric acid -> silica, 1,000 mB crude titanyl sulfate liquor |
| copperas | centrifuge | crude liquor -> 2 ferrous sulfate + 1,000 mB titanyl sulfate liquor |
| hydrolysis | chemical reactor | liquor + 4,000 mB steam -> metatitanic acid + 800 mB sulfuric acid back |
| calcination | EBF 1800 K | 2 metatitanic acid -> 2 rutile |
| copperas roasting | EBF 1800 K | 2 ferrous sulfate -> hematite + 2,000 mB SO2 (the sulfur comes home) |

## Stage 2, EV: red mud of bauxite (alternative route, about 8 bauxite per titanium)

| step | machine | in -> out |
|---|---|---|
| Bayer digest | LCR | 16 bauxite + 4 NaOH + 4,000 mB water -> 8 red mud + 4,000 mB sodium aluminate liquor |
| aluminate | electrolyzer | liquor -> 4 aluminium dust + 4 NaOH + water |
| leach | LCR | 8 red mud + 4,000 mB HCl -> 4 silica + 1,000 mB red mud leach liquor |
| extractant | mixer | 1,000 mB diesel + 500 mB phosphoric acid -> 1,500 mB titanium extractant |
| extraction | chemical reactor | leach liquor + 1,500 mB extractant -> 1,000 mB loaded extract + 500 mB iron(III) chloride |
| stripping | chemical reactor | loaded extract + 500 mB HCl -> 1,000 mB strip liquor + 1,200 mB extractant back |
| hydrolysis | chemical reactor | strip liquor + 3,000 mB steam -> 2 metatitanic acid + 1,000 mB HCl back |

Metatitanic acid then calcines as in stage 1.

## Stage 3, end of EV

Catalysed chlorination (LCR): rutile + 2 carbon + 4,000 mB chlorine, vanadium pentoxide not consumed -> 2,000 mB
TiCl4 + CO (GT's own gives 1,000 mB per rutile).

## Hand-off

Tier 3 rocket -> Venus -> Venus ilmenite goes straight into GT's chain (ilmenite -> rutile in the EBF); the synthetic stages stay.
Real-world basis: hydrogen reduction of ilmenite, the sulfate and chloride processes, red-mud titanium recovery and solvent
extraction with tributyl phosphate. The numbers are game balance.
