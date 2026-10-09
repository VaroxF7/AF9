# Early game: Create circuits and Powah components

Two friendlier ways through the early game, both beside GT's own recipes (the crafting table, the circuit assembler and
the assembler stay as they are; nothing is locked behind Create or Powah).

Code: `kubejs/server_scripts/mods/gtceu/early_circuits.js` (Create), `kubejs/server_scripts/mods/powah/hv_components.js`
(Powah) and AF9 Core's patch of Powah (`com.af9.core.mixin.powah`, `com.af9.core.compat.powah`).

## 1. LV and MV circuits with Create

A sequenced assembly: the board goes through deployers (a part each) and a press, one item that changes as it goes
(`af9:incomplete_circuit`, the transitional item).

```
LV  resin board + 2 resistors + 2 vacuum tubes + 2 red alloy wires, pressed       -> 2 basic electronic circuits
MV  phenolic board + 2 basic circuits + 2 vacuum tubes + 2 copper wires, pressed  -> 1 good electronic circuit
```

The parts are the circuit assembler's own (`circuits_af9.js`), so nothing new is asked of the player: only where the work
is done changes.

## 2. HV components in Powah's Energizing Orb

From HV on, a component is a pile of parts: a motor is nine items. Powah's orb takes one item per slot, so AF9 Core
patches it and adds a Mk2 with a screen:

- a recipe's ingredient may say `"count": 4`; the orb then wants four of it in a slot (up to a stack) and takes exactly
  that many when the craft is done (a surplus stays in the slot);
- the **Energizing Orb Mk2** (`af9:energizing_orb_mk2`) is Powah's orb with a screen: right-click it, six slots hold a
  stack each, the product comes out of its own slot (shift-click), a bar shows the charge. It is a subclass of Powah's
  orb block and tile, so energizing rods find and charge it and the wrench works on it; its slots are on the item
  handler, so hoppers, pipes and AE2 (interfaces, import and export buses) reach them. Craft: Powah's orb in the
  middle, 2 MV motors, 2 good electronic circuits, 4 steel plates (`kubejs/server_scripts/mods/powah/orb_mk2.js`);
- recipe viewers (JEI, EMI) show the counts.

Powah's own orb and its recipes (one of each) are not changed; the plain orb also takes the counted recipes when a hopper
or a pipe fills its slots.

| component | in the orb | GT's assembler recipe |
|---|---|---|
| motor | 2 silver double cables, stainless rod, energized steel, magnetic steel rod, 3 electrum double wires | 9 items |
| conveyor | 2 HV motors, gold cable, 4 rubber plates, dielectric paste | 6 ingots of rubber |
| pump | HV motor, gold cable, stainless pipe, steel rotor, 2 rubber rings, dielectric paste | and a steel screw |
| piston | HV motor, 2 stainless rods, 2 gold cables, 2 stainless plates, small stainless gear, energized steel | 3 plates |
| robot arm | 2 HV motors, HV piston, HV circuit, 2 gold cables, stainless rod, energized steel | 3 cables |
| emitter | 3 chromium rods, 2 gold cables, 2 HV circuits, ender eye | 4 rods |
| sensor | chromium rod, 3 stainless plates, HV circuit, ender eye | 4 plates |
| field generator | quantum eye, 2 stainless plates, 2 HV circuits, 3 mercury barium calcium cuprate quadruple wires | 4 wires |

Each is a part or two cheaper than GT's, and the motor, conveyor, pump, piston and arm take a Powah material
(energized steel: iron and gold in the orb; dielectric paste) in place of a part. The charge is 40,000 RF for the
motor up to 250,000 RF for the field generator; it scales with the rods around the orb (Powah's own rule), so more
rods, a shorter wait. Powah's config ratio for energizing scales these like its own recipes.

### Notes for changing it

- The recipe JSON is plain Powah JSON with `count` on an ingredient (an item or a tag): `{"item": "gtceu:steel_rod",
  "count": 4}`. At most six ingredients (Powah's limit), a count up to 64.
- The patch is mixins into Powah by name (`remap = false`, `@Pseudo`): they apply only with Powah loaded, against Powah
  5.0.11. The three that touch the recipe's packet are required; a different Powah version that renames those methods
  stops the game at launch with the mixin's message instead of sending a broken packet. `OrbInteraction.verify` logs
  "Powah's Energizing Orb takes counted ingredients" at startup when the patch applied.
- Powah's signatures are compile-time stubs in `af9-core/src/stubs` (a source set that does not go into the jar); the Mk2's
  classes extend them (`OrbMk2Block`, `OrbMk2Tile`, the renderer), so they only load with Powah present.
- The dev run takes Powah and Cloth Config from the instance when `-Paf9Instance` has them, so the headless run applies the
  mixins too.

## 3. Quests

- **Medium Voltage** chapter: "Circuits on the Belt" (the deployer and the press), after the MV start quest; it is also
  linked into the **Circuits** chapter between the basic and the good electronic circuit.
- **High Voltage** chapter, a branch under "HV Circuit Metals": "Components in the Orb" (the Mk2 orb and a rod), "Energized
  Steel", "The Motor, Faster", "Piston, Pump and Conveyor", "Robot Arm" and "Emitter, Sensor, Field Generator". The
  existing emitter quest names the orb recipe too.
- The texts are `af9.quest.mv.create.*`, `af9.quest.hv.*` in `kubejs/assets/kubejs/lang/en_us.json`.
