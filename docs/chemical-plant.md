# Chemical Plant (test version)

A 6 x 4 x 6 plant of **real GT fluid pipes**, built 1:1 from Janne's `testing.nbt` (kubejs/startup_scripts/gtceu/chemical_plant.js).

* Pipes: Bronze Huge, Stainless Steel Quadruple and Niobium-Titanium Huge, 17 in all, each with the file's exact `connections` mask.
* The pattern accepts a pipe spot only for that exact block **and** that exact mask (af9-core `ChemicalPlantPipes.pipe`). A wrong pipe or a
  wrong connection stops the plant from forming (error: "needs this pipe with exactly these connections").
* Auto-build: while the plant is unformed, the controller sets the connections of every right pipe to the file's masks once a second
  (`ChemicalPlantPipes.repair`), so an auto-built plant forms without wrench work.
* The masks are written for a controller facing SOUTH (the file as it stands in the world); they are turned for the other three facings.
  No flip, no extended facing.
* Casings: the file's Module Casing (stand-in). The file had no controller; it replaces the casing in the front wall, second layer, column 3.
  Hatches go in any casing (maximums only).
* Recipes: the Chemical Reactor's and the Large Chemical Reactor's (mode tab), parallel hatch, perfect overclock. The step-skipping chains come later.
* Pipes of a formed plant are still ordinary working pipes (no cosmetic flag yet).
