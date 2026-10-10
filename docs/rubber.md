# Rubber without rubber trees

GT's rubber trees are gone from the pack. All latex now comes from farmable
plants, and the rest of the rubber chemistry is GT's own (it never needed trees).

Code: `kubejs/server_scripts/mods/gtceu/rubber.js` (the whole AF9 layer).

## 1. Rubber: solid and liquid (steam age on Create, 3 steps)

```
plant -> extractor, ULV (LV machine now; steam extractors are removed): 4x vine, 4x sugar cane, 4x kelp,
  16x any leaves, 2x any saplings, or 1x slime ball -> 1 sticky resin
  (Create way: the garden plants press as usual; resin itself comes from the farm)
sticky resin -> press (Create): 1 resin -> 3 raw rubber dust
  (or extractor, ULV: same yield; or centrifuge, LV: 1 resin -> 3 raw rubber + 100 mB glue + 15 % plant ball)
raw rubber + sulfur -> mixer, heated (Create): 3 raw + 1 sulfur -> 1 rubber ingot (solid)
  (or alloy smelter, ULV; or chemical reactor, LV: 9 raw + 1 sulfur -> 1,296 mB rubber fluid, the liquid rubber for cables)
```

The vulcanization recipes are GT's (`rubber_bar`, `rubber`, `rubber_sheet`):
kept as-is. Sulfur comes from mining, as before. Slime smelting to resin
in the furnace keeps working; the extractor slime recipe is just faster.

## 2. Silicone rubber (LV-MV, 4 steps, no oil)

GT's shortcut chain, unchanged (no trees ever in it):

```
salt + sulfuric acid -> hydrochloric acid (GT: sodium_bisulfate_from_salt)
carbon + oxygen -> carbon monoxide, + hydrogen -> methanol (GT: methanol_from_monoxide)
silicon + HCl + methanol -> polydimethylsiloxane (GT: polydimethylsiloxane_from_silicon)
9 polydimethylsiloxane + sulfur -> silicone fluid (GT: silicone_rubber)
```

Silicon comes from quartz as usual; chlorine from salt water or H2 + Cl2.

## 3. Styrene-butadiene rubber (late, needs asteroid oil)

Unchanged GT chain, documented here so the gating is clear:

```
ethylene + benzene -> styrene (GT: styrene_from_benzene)
oil cracking -> butane -> butene -> butadiene (electrolyzers, MV)
butadiene + styrene + air/oxygen -> raw SBR dust (GT: raw_sbr_from_air/oxygen)
9 raw SBR + sulfur -> SBR fluid (GT: styrene_butadiene_rubber)
```

AF9's oil lives in the Asteroid Field (`docs/oil.md`), so SBR stays the
space-age rubber. Nothing in it referenced trees.

## 4. What was removed

`rubber.js` removes every recipe that takes `gtceu:rubber_log`,
`rubber_wood`, `stripped_rubber_log`, `stripped_rubber_wood`,
`rubber_leaves` or `rubber_sapling` (plus the two separations by id).
Generic wood recipes that take any log or sapling by tag (charcoal, sticks)
are left alone: they make charcoal and sticks, not rubber.

Worldgen: rubber trees must stay disabled where the pack disables them
(GT configured feature `RUBBER`); with no trees in the world the loot resin
on natural logs never appears either.
