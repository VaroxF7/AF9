# Fusion Power

GTNH's fusion reactor makes plasma and the plasma turbines burn it. GT ships eight burnable plasmas; this page is the other
eleven of GTNH's, in four steps from the first fusion reactor to the Mega Fusion Reactor, and the fuel values that make each
pay. Recipes: `server_scripts/mods/gtceu/fusion_power.js`; the plasma forms GT lacks: af9-core `AF9Materials.fusionPlasmas`.

## 1. The steps

| Reactor | Voltage | Start EU it holds | Burnable plasmas it makes |
| --- | --- | --- | --- |
| Fusion Reactor Mk1 | LuV | 160 M (16 hatches) | helium (D + T, **D + He-3**), **calcium** |
| Fusion Reactor Mk2 | ZPM | 320 M | + argon, oxygen, nitrogen, tin (GT's), **sulfur, zinc, niobium** |
| Fusion Reactor Mk3 | UV | 640 M | + iron, nickel, americium (GT's), **silver, bismuth, radon** |
| Mega Fusion Reactor | UIV | 640 M (2 hatches of 320 M) | + star matter, anti-matter; **lead, thorium, plutonium-241** |

Bold ones are new. A higher reactor runs the recipes of the lower ones, and the Mega runs every Mk1-Mk3 recipe in half the time on
one and a half times the power (the base pack's rule for GT's recipes, applied here to AF9's too).

## 2. The reactions

Amounts are GTNH's in GT's units (a nugget 16 mB, an ingot 144). *Fuel* is what 1 mB is worth in a plasma turbine (the generator
duration x 2,048 EU/t: GTNH's table), *out* what a run's plasma is worth, *in* the EU a run draws (EUt x duration).

| Reaction | Reactor | Runs | Plasma | Start EU | Out | In | Return |
| --- | --- | --- | --- | --- | --- | --- | --- |
| deuterium 125 + tritium 125 (GT) | Mk1 | 16 t, 4,096 EU/t | helium 125 | 40 M | 10,240,000 | 65,536 | 156x |
| deuterium 125 + helium-3 125 | Mk1 | 16 t, 1,920 EU/t | helium 125 | 60 M | 10,240,000 | 30,720 | 333x |
| magnesium 128 + oxygen 128 | Mk1 | 128 t, 7,680 EU/t | calcium 16 | 120 M | 3,014,656 | 983,040 | 3.1x |
| aluminium 16 + lithium 16 | Mk2 | 32 t, 10,240 EU/t | sulfur 144 | 240 M | 24,477,696 | 327,680 | 75x |
| copper 72 + tritium 250 | Mk2 | 16 t, 49,152 EU/t | zinc 72 | 180 M | 16,220,160 | 786,432 | 21x |
| cobalt 144 + silicon 144 | Mk2 | 16 t, 49,152 EU/t | niobium 144 | 200 M | 38,928,384 | 786,432 | 50x |
| gold 144 + arsenic 144 | Mk3 | 16 t, 49,152 EU/t | silver 144 | 350 M | 40,697,856 | 786,432 | 52x |
| tantalum 144 + **zinc plasma** 72 | Mk3 | 16 t, 98,304 EU/t | bismuth 144 | 350 M | 61,341,696 | 1,572,864 + 16,220,160 of zinc plasma | 3.4x |
| iridium 144 + fluorine 500 | Mk3 | 32 t, 98,304 EU/t | radon 144 | 450 M | 64,880,640 | 3,145,728 | 21x |
| platinum 576 + beryllium 576 | Mega | 8 t, 1,966,080 EU/t (UHV) | lead 576 | 500 M | 244,187,136 | 15,728,640 | 16x |
| osmium 576 + silicon 576 | Mega | 8 t, 3,932,160 EU/t (UEV) | thorium 576 | 580 M | 271,319,040 | 31,457,280 | 8.6x |
| lutetium 576 + vanadium 576 | Mega | 8 t, 7,864,320 EU/t (UEV) | plutonium-241 576 | 640 M | 286,654,464 | 62,914,560 | 4.6x |

The Mega-only recipes are gated by voltage: their EU/t is above the Mk3's tier, and the Mega's start energy cannot go past
640 M (two energy hatches), so a bigger start would never run. GTNH starts its Mk IV and V recipes on 6 GEU; the lead recipe
there takes tellurium, which has no fluid here, so platinum and beryllium stand in.

## 3. The fuels

Every new plasma is a plasma-generator fuel (EV, 2,048 EU/t; the Large Plasma Turbine takes them); the element comes back as
its fluid where it has one. GTNH's EU per litre, which are whole multiples of 2,048:

| Plasma | EU / mB | Plasma | EU / mB |
| --- | --- | --- | --- |
| sulfur | 169,984 | thorium | 471,040 |
| calcium | 188,416 | plutonium-241 | 497,664 |
| zinc | 225,280 | lead | 423,936 |
| niobium | 270,336 | bismuth | 425,984 |
| silver | 282,624 | radon | 450,560 |

The endgame's one: **star matter plasma** 1,638,400 EU/mB (a Mega Fusion run of 1,000 mB takes helium, nitrogen, oxygen and iron
plasma worth 1.35 GEU, so it returns 1.21x). The quark plasmas (strange matter, chromodynium, anti-matter, superstate) and
Idontknowium are the Plasma Forge's and burn nowhere: the forge draws UHV power at 100 A (196 MEU/t), which no fuel could pay back.

## 4. The plasma forms

GT gives plasma to helium, nitrogen, oxygen, argon, iron, tin, nickel and americium only. AF9 Core adds one to sulfur, calcium,
zinc, niobium, silver, bismuth, radon, lead, thorium and plutonium-241 (`gtceu:<element>_plasma`). Sulfur and calcium have no
fluid of their own: their plasma is their only one, like the quark matters'.

## 5. Where the fluids come from

The reactions take gases and molten metals GT's own chains make; the pack adds worlds to go and get them from. Four sources feed the
fusion line, and each one needs somewhere else than the base:

| Source | What it brings | Where it is made |
| --- | --- | --- |
| **Atmospheres** (Gas Collector, a circuit of 4 to 26 for each gas, in the dimension) | helium-3 off the **Moon**, deuterium out of **Glacio**'s ice and from **Mercury**, fluorine from **Venus**'s acid clouds, argon / neon / nitrogen on **Mars**; in **orbit** the solar wind: helium-3 and hydrogen, deuterium, and tritium from flare spallation at **Mercury orbit** | `server_scripts/mods/gtceu/atmospheres.js` |
| **Space Elevator liquid missions** (GTNH's Space Pumping) | deuterium 1,568 B, tritium 240 B (type 6), helium-3 2,800 B (type 5), fluorine 1,792 B (type 7) a mission; and from the new **termination shock** of type 9 the plasma itself: helium plasma 80 B, nitrogen 20 B, oxygen 12 B, iron 2 B (star matter's ingredients in its 40 : 10 : 6 : 1) and argon plasma 24 B a Mk-IV mission | `PlanetCatalog` ([space-elevator.md](space-elevator.md)) |
| **Void Miner** (ores, in the area's own dimension) | the raw ore of the solid inputs: tantalite (tantalum), realgar (arsenic), cobaltite (cobalt), vanadium magnetite, beryllium and emerald, bastnasite and monazite (lutetium), platinum | `server_scripts/mods/gtceu/miner.js` |
| **Plasma chains** (the fusion reactors and the Plasma Forge) | zinc plasma (Mk2) feeds the bismuth reaction (Mk3); helium, nitrogen, oxygen and iron plasma feed star matter (Mega); strange matter and chromodynium plasma (the Plasma Forge's) are fused into **anti-matter plasma** in the Mega; star matter and anti-matter plasma are forged into Idontknowium in the Plasma Forge | this page, [dtpf.md](dtpf.md) |

A route a player can read straight from the first column: *Glacio's deuterium* and *the Moon's helium-3* are the Mk1's whole fuel from
two dimensions, *Venus's fluorine* and a platinum-group ore make the Mk3's radon plasma, and a Mk-IV mission to the termination
shock brings the Mega Fusion Reactor its star matter without a single Mk3 reaction.
