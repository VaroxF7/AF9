# Tinkers' Construct x GregTech

Every GT fluid with a solid form can be melted in the Smeltery / Foundry and cast on the Casting Table / Basin with
GT's casting molds. Built at recipe load from GT's material registry by
`af9-core/.../compat/tconstruct/TinkersGtRecipes.java`, registered by `kubejs/server_scripts/mods/tconstruct/gt_molds.js`.

## Melting
- Shapes: dust (also small / tiny), ingot, nugget, block, plate (double, dense), gear (small), rod (long), bolt, screw,
  ring, foil, spring (small), rotor, round, fine wire, the five fluid pipes.
- Amount: the shape's material amount at the fluid's scale (GT fluids: ingot = 144 mB, like GT's solidifier;
  Tinkers' own metals: ingot = 90 mB).
- Temperature: the blast temperature, 700 for other metals, 400 for the rest. Tinkers' hottest fuel burns at 1800, so
  GT's EBF-tier metals (titanium 1941 and up) cannot be melted in the Smeltery at all, aluminium / stainless steel
  (1700) need the Foundry's hottest fuel, steel (1000) lava. Casting is not gated: the fluid had to be made in GT.
- Time: 57 ticks for an ingot at 700, as many times the square root of the ingots.

## Casting (Table; Basin for blocks and the anvil)
| Mold | Fluid | Makes |
|---|---|---|
| ingot | 1 ingot | ingot |
| nugget | 1/9 | nugget |
| plate | 1 | plate |
| gear | 4 | gear |
| small gear | 1 | small gear |
| rotor | 4 | rotor |
| block (Basin) | 9 | block |
| tiny / small / normal / large / huge pipe | 1/2, 1, 3, 6, 12 | fluid pipe |
| bottle / ball / block | 1 glass | glass bottle / glass tube / glass |
| anvil (Basin) | 31 iron | anvil |

The molds are not used up. GT-only metals can also be cast with Tinkers' gold casts (ingot, nugget, plate, gear, rod)
and, used up, its sand casts; a block needs no cast in the Basin.

## Tinkers' own metals
A metal that has a `tconstruct:molten_<name>` fluid (iron, copper, tin, gold, steel, ..., aluminium as `aluminum`)
uses that fluid (90 mB per ingot) instead of a second fluid of the same metal. Tinkers' own melting recipes are left
alone for dust, ingot, nugget, block, plate, gear and rod; the GT molds and the other shapes are added.

## Alloys
A GT alloy of ingot metals that Tinkers has no alloy for (e.g. cupronickel-like mixes, the AF9 alloys) is mixed in the
Smeltery from the molten parts in GT's component ratio. Temperature: the highest of the alloy and its parts.
