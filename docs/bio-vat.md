# Bio Vat

GTNH's **Bacterial Vat** (BartWorks `MTEBioVat` in GT5-Unofficial, `src/main/java/bartworks/common/tileentities/multis/`,
MIT licence of bartimaeusnek; licence text of GT5-Unofficial in `licenses/`) in GTCEu. It **replaces the Bacteria Breeding
Station** (removed): same job, the cultures of the [Water Filtration Plant](water-filtration.md)'s LuV/ZPM biosorption modes.

Code: `kubejs/startup_scripts/gtceu/bio_vat.js` (structure, recipe type), `kubejs/server_scripts/mods/gtceu/bio_vat.js` (recipes),
`af9-core/.../machine/BioVatMachine.java` (rules), `.../client/render/BioVatRender.java` (the culture in the tank).

## Structure (5 x 4 x 5, the original's)

Clean stainless casings round a 3 x 3 x 2 tank. Layers bottom to top: casing floor (controller front-centre), two glass
rings with the open tank inside, casing roof. Hatches go in the casings, maximums only: 2 energy, 1 item bus,
2 fluid inputs, 1 fluid output, 1 maintenance. The glass is `gtceu:tempered_glass`, `laminated_glass` or `fusion_glass`.

## Rules (from the original)

- **Glass tier.** Every recipe names the glass it needs (`glass_tier` in its data): tempered 1, laminated 2, fusion 3.
  The vat has the tier of the weakest pane of its two glass rings; a recipe above it does not run (the machine says so).
- **The fuller the better.** A run is multiplied by how full the *output* hatch is (the original's `calcMod`): with
  `y` half the output capacity and `x` what is stored, the bonus is `ceil((y - (x-y)^2 / y) / y * max)`, clamped to
  `1..max`, so most at half full, less again towards full. The multiplier is parallel runs without extra EU, as many as the
  inputs allow. `max` is 1000 in GTNH and **64** here.
- **The tank is seen.** A formed vat is always liquid-filled (very transparent culture green when the hatches are empty); the culture stands in the tank as high as the hatches are full, in the fluid's colour, with a rippling
  surface and bubbles while a run goes on. GTNH places fluid blocks in the world; this is drawn, nothing is left in the world.

Not ported: the **radiation hatch** (sieverts) and the **petri-dish bio cultures** with DNA/plasmid chains. The cultures here
are plain fluids; the vat runs on glass tier and fill instead.

## Recipes

| Recipe | Input | Output | Glass | Power |
|---|---|---|---|---|
| `af9:bacteria_culture` (circuit 1) | 4 biochaff, 2000 mB water | 1000 mB bacteria culture | laminated (2) | MV, 400 t |
| `af9:enriched_bacteria_culture` (circuit 2) | 1000 mB culture, 1000 mB water | 1000 mB enriched culture | fusion (3) | HV, 400 t |

Machine recipe: stainless plates, tempered glass, MV circuit, MV hull (`af9:bio_vat`).

## Credits

The structure, the glass-tier and fill rules and the controller face (`OVERLAY_FRONT_DISTILLATION_TOWER*` as in the original)
come from GT5-Unofficial; see `CREDITS-textures.md`.
