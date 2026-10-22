# Wire spools

Wire spools are Star Technology's compressed wire stock: one item holds 64 fine wires of a single material, so a
crafting recipe can ask for a spool instead of a stack of fragile wire. AF9 implements the item as a GregTech
`TagPrefix`, the same way it implements the ultradense plate, and winds it in the Wiremill.

## The item

- **Id**: `gtceu:<material>_wire_spool` (`gtceu:iron_wire_spool`, `gtceu:tungsten_steel_wire_spool`, ...).
- **Tag**: `forge:wire_spools/<material>` and `forge:wire_spools`. The tag is generated for every material, so a recipe
  written against it needs no change when a material is added.
- **Amount**: 64 fine wires = 8 ingots. Fine wire is one eighth of an ingot (`TagPrefix.wireFine`), and the spool's
  `materialAmount` is `8 * M`.
- **Generated for**: every material with `GENERATE_FINE_WIRE`; materials with `DISABLE_MATERIAL_RECIPES` get the item
  but no winding recipe.
- **Item/art**: `AF9TagPrefixes.wireSpool` in AF9 Core. Models live under
  `af9-core/.../assets/gtceu/models/item/material_sets/<set>/wire_spool.json`; the three textures are StarT-Core's
  (LGPL-3.0, see `CREDITS-textures.md`). GregTech tints them with the material colour.

## Winding (Wiremill)

There is no separate Spooler machine: winding is a recipe of the Wiremill (`wiremill` recipe type), so every Wiremill
(`gtceu:lv_wiremill` ... `gtceu:uv_wiremill`) and the Large Wiremill can wind spools. It was a separate `spooling`
recipe type, `<tier>_spooler` machines and a Large Spooler multiblock until they were merged into the Wiremill.

- **Winding recipe**: `64x #forge:fine_wires/<material>` -> `1x gtceu:<material>_wire_spool`, id `af9:spool_<material>`.
- **Duration**: `max(20, material mass * 4)` ticks; **EU/t**: LV (30). Higher tiers overclock normally.
- **Removed ids** (items in existing worlds disappear): `gtceu:lv_spooler` ... `gtceu:uhv_spooler`, `gtceu:large_spooler`.

## Files

| File | What it holds |
|---|---|
| `af9-core/src/main/java/com/af9/core/registry/AF9TagPrefixes.java` | `wireSpool` tag prefix and icon type |
| `af9-core/src/main/resources/assets/gtceu/models/item/material_sets/*/wire_spool.json` | item models |
| `af9-core/src/main/resources/assets/gtceu/textures/item/material_sets/dull/wire_spool*.png` | item textures |
| `kubejs/server_scripts/mods/gtceu/wire_spools.js` | winding recipes (Wiremill) |
| `kubejs/assets/gtceu/lang/en_us.json` | item name |

Use the `forge:wire_spools/<material>` tag in future component recipes where 64 fine wires would otherwise be listed.
