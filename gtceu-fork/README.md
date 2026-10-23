# The AF9 fork of GTCEu

The pack runs GregTech CEu Modern 7.5.3 with changes of its own. They live here as a patch series on top of upstream's
release, so the fork and the AF9 Core addon (`../af9-core`) are in one repository and one build.

* `upstream.properties`: the GregTech-Modern release the fork starts from (repository and tag).
* `patches/*.patch`: the fork's changes, one `git format-patch` file per commit, applied in file name order with `git am`.
* `build.sh <version>`: clones upstream at the tag, applies the patches and publishes the result to mavenLocal as
  `com.gregtechceu.gtceu:gtceu-1.20.1:<version>`.

`af9-core`'s build runs `build.sh` itself when the fork (`gtceu_fork_version` in `af9-core/gradle.properties`) is not in
mavenLocal yet, then compiles against it; CI does the same. `-Paf9SkipForkBuild` skips that (the build falls back to the
GTCEu release), `-Paf9RequireFork` fails the build where the fork is missing.

## What the patches change

* `0001` Layered (staged) recipes: the recipe support AF9's staged assembly is built on.
* `0002` The fork's own version, so no cache takes it for upstream's release.
* `0003` KubeJS recipes, a bad recipe fails alone. GT 7.5 throws from the recipe builders on an unknown or empty item / fluid,
  and KubeJS (2001.6.5-build.16) posts its recipe event without an exception handler: the first throw ended the event and every
  recipe script after it added nothing. The base pack's scripts were written for GT 7.2, which let such a recipe fail on its own,
  and name ids the game does not have (`gtceu:max_energy_input_hatch`, `minecraft:nether_quartz`): one of them took every recipe
  of the scripts behind it along, AF9's too. With the patch the builder reports the id (`Invalid or empty ... item (recipe ID:
  ...)` in `logs/kubejs/server.log`, with script and line) and the recipe fails when it is built, as one of KubeJS's "failed
  recipes"; any other error in a recipe script ends that script only (`Error in recipe script ...`).
* `0004`-`0008` Hidden machines, auxiliary power (energy hatch modes, Power Management tab), Assembly Line laser hatch.
* `0009` Fusion reactors: no start buffer or heat (its standby power was replaced by the field of `0015`).
* `0015` Fusion reactors: a magnetic field instead of standby power ([docs/fusion-power.md](../docs/fusion-power.md)); its 19 / 23 / 27 block rings were replaced by `0016`.
* `0016` Fusion reactors: one round 19-block ring for Mk1 to Mk3, energy hatches optional in the structure (the ring was dropped again by `0017`).
* `0017` Fusion reactors: GT's own 15 x 15 ring structure and render again; energy hatches stay optional (no minimum).
* `0020` Electric Blast Furnace: the normal 3 x 3 furnace with a 3 x 4 face of casing and coils added on each side (a cross, 5 x 5 without the corners, 4 high); the controller is in the front face, one block further out.
* `0021` The recipe UI editor's template menu no longer crashes on recipe types made without a group (KubeJS `event.create('name')`): they go under "other".
* `0019` Fusion reactors: at most two energy hatches in the structure.
* `0012` The HPCA is expandable (3 to 12 slices of 3 component slots) and its Active Coolers cool by coolant: PCB Coolant 2, supercooled hydrogen / argon / xenon / endion 8 / 12 / 16 / 20 ([docs/hpca.md](../docs/hpca.md)).
* `0011` The multiblock preview zooms out to fit a structure over 40 blocks (the scene clipped the Mk2 tower to a third of itself).
* `0014` A machine with several modes has the slots of its biggest mode: the tiered machines size their item and fluid
  inventories from all their modes (not only the first), and `SimpleTieredMachine.createEditableUI` builds the GUI of a
  machine with modes from that layout. AF9's fab modes are modes of the normal GT machines (Chemical Reactor, Distillery,
  Electrolyzer, Arc Furnace, Large Chemical Reactor, Distillation Tower, Large Electrolysis Chamber, Electric Blast
  Furnace), and the fab recipes need more slots than the GT machines had.

## Changing the fork

1. Check out upstream at the tag, make the change in a commit.
2. `git format-patch -1 -o <this repo>/gtceu-fork/patches/` (number the files so they apply in order, `0001-...patch`).
3. Raise `gtceu_fork_version` in `af9-core/gradle.properties` (`7.5.3-af9.2`): that makes the next build publish it again.

Export the patches of an existing fork checkout with `git format-patch v.7.2.0-1.20.1..HEAD -o gtceu-fork/patches/`.
