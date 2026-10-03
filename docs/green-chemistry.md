# Green chemistry

Farm and Nether feedstocks for sulfur acids, fuels and aromatics: nether wart to sulfuric acid and
hydrogen sulfide, Nether flora to radon, a full biodiesel / bioethanol / ethanol line, and GTNH-style
benzene, chlorobenzene and chloroform. GT's own biomass, ethanol and oil chemistry is untouched; these
lines run beside it.

Code: `kubejs/startup_scripts/gtceu/biofuels.js` (biodiesel, bioethanol),
`kubejs/startup_scripts/gtceu/aromatics.js` (dichloromethane),
`kubejs/server_scripts/mods/gtceu/nether_chemistry.js`,
`kubejs/server_scripts/mods/gtceu/biofuels.js`,
`kubejs/server_scripts/mods/gtceu/aromatics.js`.
Planet type 9 (the far dark) brings these fluids home: `PlanetCatalog`, §5.

## 1. Nether wart to sulfuric acid and hydrogen sulfide

```
nether wart / wart blocks -macerator, MV-> sulfur dust
sulfur + hydrogen         -chemical reactor, MV-> hydrogen sulfide (H2 + S -> H2S)
hydrogen sulfide + oxygen -chemical reactor, HV-> sulfuric acid (wet sulfuric acid process, H2S + 2 O2 -> H2SO4)
sulfur + water + oxygen   -large chemical reactor, EV-> sulfuric acid (GTNH's all-in-one as the upscale)
```

| Recipe | Takes | Gives | Time |
|---|---|---|---|
| `af9:nether_wart_sulfur` | 1 nether wart | 2 sulfur dust, 25 % a third | 200 ticks, MV |
| `af9:nether_wart_block_sulfur` | 1 wart block | 16 sulfur dust (lossy: a block unwarts to nine) | 400 ticks, MV |
| `af9:sulfur_hydrogenation` | 1 sulfur dust, 1,000 mB hydrogen | 1,000 mB hydrogen sulfide | 160 ticks, MV |
| `af9:wet_sulfuric_acid` | 1,000 mB hydrogen sulfide, 2,000 mB oxygen | 1,000 mB sulfuric acid | 200 ticks, HV |
| `af9:contact_sulfuric_acid` | 1 sulfur dust, 1,000 mB water, 3,000 mB oxygen | 1,000 mB sulfuric acid | 100 ticks, EV |

The sulfur also feeds GT's own contact chain (sulfur dioxide, sulfur trioxide) and the oil wash
(`af9:impure_oil_wash`).

## 2. Nether flora to radon

A trace of radon seeps out of every load of crushed Nether plants (`af9:radon_from_<plant>`,
centrifuge, HV, 300 ticks): **16 plants** (crimson roots, warped roots, nether sprouts, crimson and
warped fungi, weeping and twisting vines) give **500 mB radon**, 2 ash and 15 % glowstone dust.
Thirty-two plants charge the boule furnace's radon blanket once
(`kubejs/server_scripts/mods/gtceu/boule_melting.js`).

## 3. Biodiesel, bioethanol, ethanol

```
crops + water        -brewery, LV->     fermented biomass (GT's own fluid)
fermented biomass    -distillery, MV->  bioethanol (the 95 % azeotrope)
bioethanol           -reactor, MV->     ethanol (fuel grade, dried over AF9's molecular sieves)
bioethanol           -reactor, MV->     ethylene + water (oil-free road to Aluminised Hydrolox)
seeds                -extractor, LV->   seed oil (GT's own fluid)
seed oil + methanol  -reactor, HV->     biodiesel + glycerol (transesterification, soda lye catalyst)
biodiesel            -combustion gen->  38,400 EU a bucket (AF9 balance)
bioethanol           -gas turbine->    32,000 EU a bucket (AF9 balance)
```

| Recipe | Takes | Gives |
|---|---|---|
| `af9:mash_<crop>` (×8: wheat, potato, carrot, beetroot, melon slice, apple, sugar cane, sweet berries) | 4 crops, 1,000 mB water | 1,000 mB fermented biomass, 400 ticks LV |
| `af9:bioethanol_distillation` | 1,000 mB fermented biomass | 600 mB bioethanol, 200 ticks MV |
| `af9:bioethanol_drying` | 1,000 mB bioethanol (molecular sieve, not consumed) | 900 mB ethanol, 200 ticks MV |
| `af9:bioethanol_dehydration` | 1,000 mB bioethanol | 1,000 mB ethylene, 500 mB water, 200 ticks MV |
| `af9:seed_oil_<seed>` (×4: pumpkin, melon, beetroot, wheat seeds) | 4 seeds | 200 mB seed oil, 100 ticks LV |
| `af9:biodiesel_transesterification` | 1,000 mB seed oil, 100 mB methanol, 1 soda lye | 1,000 mB biodiesel, 100 mB glycerol, 200 ticks HV |

The glycerol feeds GT's nitroglycerin chain. The ethylene keeps the first Asteroid Field flight
oil-free (docs/oil.md §3).

## 4. Benzene, chlorobenzene, chloroform (GTNH-style)

```
wood tar                    -distillation tower, MV-> benzene + toluene + phenol + creosote (AF9 topping)
toluene + hydrogen          -reactor, HV->          benzene + methane (hydrodealkylation)
benzene + chlorine          -reactor, MV->          chlorobenzene + HCl (FeCl3, circuit 1)
benzene + 2 chlorine        -reactor, HV->          dichlorobenzene + 2 HCl (FeCl3, circuit 2)
chlorobenzene + hydrogen    -reactor, HV->          benzene + HCl (Pd/C: the elevator's chlorobenzene recycled)
methane -Cl2-> chloromethane -Cl2-> dichloromethane -Cl2-> chloroform (+ HCl each step)
```

| Recipe | Takes | Gives | Time |
|---|---|---|---|
| `af9:wood_tar_topping` | 1,000 mB wood tar | 400 benzene, 200 toluene, 100 phenol, 300 creosote | 200 ticks, MV |
| `af9:toluene_dealkylation` | 1,000 mB toluene, 1,000 mB hydrogen | 1,000 mB benzene, 1,000 mB methane | 200 ticks, HV |
| `af9:chlorobenzene_synthesis` | 1,000 mB benzene, 1,000 mB chlorine (ferric chloride, circuit 1) | 1,000 mB chlorobenzene, 1,000 mB HCl | 200 ticks, MV |
| `af9:dichlorobenzene_synthesis` | 1,000 mB benzene, 2,000 mB chlorine (ferric chloride, circuit 2) | 1,000 mB dichlorobenzene, 2,000 mB HCl | 300 ticks, HV |
| `af9:chlorobenzene_hydrodechlorination` | 1,000 mB chlorobenzene, 1,000 mB hydrogen (Pd/C) | 1,000 mB benzene, 1,000 mB HCl | 200 ticks, HV |
| `af9:chloromethane_synthesis` | 1,000 mB methane, 1,000 mB chlorine | 1,000 mB chloromethane, 1,000 mB HCl | 160 ticks, MV |
| `af9:dichloromethane_synthesis` | 1,000 mB chloromethane, 1,000 mB chlorine | 1,000 mB dichloromethane, 1,000 mB HCl | 160 ticks, MV |
| `af9:chloroform_synthesis` | 1,000 mB dichloromethane, 1,000 mB chlorine | 1,000 mB chloroform, 1,000 mB HCl | 160 ticks, MV |

The two chlorination circuits are load-bearing: the mono recipe's inputs sit inside the di recipe's
(lint R7). The chain ends at chloroform on purpose. Chlorobenzene keeps its fab use (the Grignard
reagent, semiconductor-factory §6.14).

## 5. Planet type 9: the far dark

AF9's own planet type past GTNH's table (`PlanetCatalog`, Mk-IV only): biodiesel 1,400, bioethanol
1,792, benzene 1,400, chloroform 896, chlorobenzene 1,120, hydrogen sulfide 784 and radon 128 buckets
a mission — the green-chemistry fluids plus richer cuts of the sour gas and radon the nearer types
already bring (392 and 64). Biodiesel and bioethanol are KubeJS fluids, looked up by id at runtime;
while they do not exist the missions are skipped like GTNH's three missing fluids.
