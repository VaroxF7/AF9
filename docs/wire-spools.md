# Wire spools

Wire spools are Star Technology's compressed wire stock: one item holds 64 fine wires of a single material, so a
crafting recipe can ask for a spool instead of a stack of fragile wire. AF9 implements the item as a GregTech
`TagPrefix`, the same way it implements the ultradense plate, and adds a dedicated Spooler to wind it.

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

## The Spooler (single block, LV-UHV)

A tiered `SimpleTieredMachine` running the `spooling` recipe type. It has one item input and one item output and no
fluid slots.

- **Winding recipe**: `64x #forge:fine_wires/<material>` -> `1x gtceu:<material>_wire_spool`.
- **Duration**: `max(20, material mass * 4)` ticks; **EU/t**: LV (30). Higher tiers overclock normally
  (`OC_NON_PERFECT`).
- **Machine ids**: `gtceu:lv_spooler` ... `gtceu:uhv_spooler`.
- **Crafting**: LV–UV in the assembler, one machine hull of the tier, two circuit tags, the tier's motor, conveyor and
  robot arm, and four plates of the tier metal. EU/t is the tier's VA, 200 ticks. The UHV Spooler is a shaped recipe
  instead (GT's UHV assembler is dev-only in the pack): a UHV hull, UV circuits, a UV motor, conveyor and robot arm,
  and four neutronium plates, so it can be built before the first UHV circuit.
- **Face**: the Wiremill's overlay for now (the same winding coils); the machine name and recipes are the Spooler's.

## The Large Spooler (multiblock, IV)

A 3 x 3 x 3 controller multiblock, unlocked at IV.

```
XXX   XCX   XXX
XCX   CPC   XSX
XXX   XCX   XXX
```

- `X` = `gtceu:robust_machine_casing` (the tungstensteel casing; energy, item, parallel and maintenance hatches go here)
- `C` = `gtceu:tungsten_steel_frame`
- `P` = `gtceu:tungstensteel_pipe_casing` (the reel axle)
- `S` = the controller, front middle
- **Modifiers**: `PARALLEL_HATCH` and `OC_PERFECT`.
- **Crafting**: assembler at IV, four robust casings, four tungstensteel frames, four IV circuits, IV motors,
  conveyors and robot arms, four tungstensteel gears and four tungstensteel spools. The spools gate it behind a
  singleblock Spooler of IV or higher.
- **Face**: the Large Electric Implosion Compressor's overlay for now.

## Files

| File | What it holds |
|---|---|
| `af9-core/src/main/java/com/af9/core/registry/AF9TagPrefixes.java` | `wireSpool` tag prefix and icon type |
| `af9-core/src/main/resources/assets/gtceu/models/item/material_sets/*/wire_spool.json` | item models |
| `af9-core/src/main/resources/assets/gtceu/textures/item/material_sets/dull/wire_spool*.png` | item textures |
| `kubejs/startup_scripts/gtceu/spooler.js` | recipe type, single blocks, multiblock |
| `kubejs/server_scripts/mods/gtceu/wire_spools.js` | winding recipes and machine recipes |
| `kubejs/assets/gtceu/lang/en_us.json` | item name, recipe type name and machine tooltips |

The Large Spooler controller consumes four tungstensteel spools, so the item has a use beyond storage. Otherwise it is a
general-purpose part: use the `forge:wire_spools/<material>` tag in future component recipes where 64 fine wires would
otherwise be listed.
