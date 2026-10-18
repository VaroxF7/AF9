# Orbital Array Mk2: the structure casings

The Mk2's pattern no longer uses GTCEu's Stress-Proof, Sturdy HSS-E and Inert PTFE casings (the Mk1 station keeps them). Four AF9 casings
take their places, each costing a good deal more; all are assembled at UHV, four casings to a run (`kubejs/server_scripts/mods/gtceu/orbital_casings.js`,
change the numbers there). The pattern keeps Janne's hand edits; only the block each letter stands for changed (`orbitalWheres` in `photolithography.js`).

| Casing | Replaces | Where (letter, cells) | Run (4 casings) |
|---|---|---|---|
| Orbital Combined Structure Casing | Stress-Proof, `L` (5,571) | the tower's walls, fins, the rings' top levels | 4 ceraquadalloy plate, 1 ceraquadalloy frame, 4 tritanium plate |
| Orbital Truss Casing | Sturdy HSS-E, `D` (1,152) | the inner half of every ring's band | 4 tritanium plate, 2 tritanium frame, 2 naquadah alloy plate |
| Orbital Girder Casing | Sturdy HSS-E, `E` (1,212) | the outer half of the band, under the non-conducting face | 4 osmiridium plate, 2 tritanium frame, 2 europium plate |
| Orbital Deck Casing | Inert PTFE, `O` and `Q` (1,039) | the crown and the deck: the casing every hatch goes on, and the casings round a module slot | 2 ceranium plate, 4 PBI foil, 144 mB PBI |

The non-conducting casing (`C`, 4,172) stays GT's.

## Ceraquadalloy

The new alloy of the Combined Structure Casing: Trinaquadalloy hardened with the belt's and the satellite's metals.

| Step | Where | Takes | Makes |
|---|---|---|---|
| 1. alloy | Mixer, UHV | 4 trinaquadalloy, 2 ceranium, 1 quantanium, 1 neutronium dust | 8 ceraquadalloy dust |
| 2. melt | EBF, GT's own recipe (blast 10,800 K, UHV) | ceraquadalloy dust | ceraquadalloy ingot (then plate, frame, rod) |

A Combined Structure Casing is 1.5 ingots of the alloy plus a tritanium plate, so the Mk2's 5,571 of them need about 8,400 ingots: around 4,200
trinaquadalloy, 2,100 ceranium, 1,050 quantanium and 1,050 neutronium. Material: `AF9Materials.orbitalStructureAlloy()`.

## Textures

`tools/textures/orbital_mk2_casings.py` (sources in `tools/textures/gt_src/`, credits in `CREDITS-textures.md`). Each is the GT casing's own shape on a cool steel ramp with AF9's teal (the Orbital Light Casing's glow) as the accent, and all four are
connected textures (LDLib, 32 x 32 `_ctm` sheet): where blocks meet the frame disappears and the plating runs on across them. The Combined
Structure Casing has the stress-proof stone corners and cross with a teal core line, a stone frame at the edge of the group and a navy plate wall
inside. The Truss (steel-blue ribbed beam plating, a groove up every plate) and the Girder (the same beam on its side in graphite, a teal core in the groove) have no frame and no rivets; the Deck keeps the PTFE casing's frame with a teal inner edge and a diagonal sheen across the inset field.

## Notes

- The controller's own face and the hatches still draw the PTFE casing (Mk1 and Mk2 share the controller and its model).
- Not compiled or run in the cloud; CI "Build AF9 Core" is the check.
