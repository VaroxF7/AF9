# Wire spools

Wire spools are Star Technology's compressed wire stock: one item holds 64 fine wires of a single material, so a
crafting recipe can ask for a spool instead of a stack of fragile wire. AF9 implements the item as a GregTech
`TagPrefix`, the same way it implements the ultradense plate, and winds it in the Wiremill's Spooling mode.

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

There is no separate Spooler machine: winding is the Spooling mode of the Wiremill: the `spooling` recipe type (1 or 2 item inputs, 1 output, no fluids) is
added to every `gtceu:<tier>_wiremill` by AF9 Core (`fab/SpoolingMode`), switched with GT's mode tab like the fab modes.
The 0.1.111 and earlier merge put the recipes into the plain `wiremill` type with no mode, so the Wiremill showed no
Spooling mode. It was a separate `spooling`
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
| `kubejs/startup_scripts/gtceu/spooling.js` | the `spooling` recipe type |
| `af9-core/.../fab/SpoolingMode.java` | adds the type to the Wiremills as a mode |
| `kubejs/server_scripts/mods/gtceu/wire_spools.js` | winding recipes (Spooling mode) |
| `kubejs/assets/gtceu/lang/en_us.json` | item name |

Use the `forge:wire_spools/<material>` tag in future component recipes where 64 fine wires would otherwise be listed.

## Superconductor Spool

`af9:superconductor_spool` (AF9 Core item, replaces the removed Mainframe Interconnect) is the IV spool a Microprocessor
Mainframe is built around. It is not a `wireSpool` TagPrefix item: superconductor materials have no fine wire.

- **Winding**: Wiremill, id `af9:superconductor_spool`: 16x `gtceu:mercury_barium_calcium_cuprate_single_wire` + 2x
  `gtceu:tungsten_steel_ring` (the reel), 300 ticks at HV (`circuits_af9.js`).
- **Used by**: `mainframe_iv` (replaces the interconnect and the loose 16 MBCC wires) and `mainframe_iv_lean`.
- **Removed**: `af9:mainframe_interconnect`, its assembler recipe, model and texture (items in existing worlds vanish).
