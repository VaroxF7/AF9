# Performance mod test (from the GTO comparison)

GregTech Odyssey runs fast mostly through a small pack, a perf mod stack and a reworked GregTech. This test adds the
open-licensed part of that stack to a copy of the ATM9 instance (`ATM9 perf test`).

Mods (copied from the GTO instance by `tools/perf-mods/install-perf-mods.ps1`, not stored in the repo): Radium, Alternate
Current, FastNBT, LeanObject, FastRecipeSearch, Gnetum, ImmediatelyFast, AsyncParticles, Flerovium, Krypton Reforged,
fastspawner. Left out: Nolijium (overlaps sodium options mods), DoesPotatoTick, mobtimizations and gpumemleakfix
(all-rights-reserved).

Compare (same world copy, same spot, a few minutes each): `/spark profiler start --timeout 300`, then `/spark profiler open`
(or `--save-to-file`) in the normal instance and in the test copy; also compare F3 fps and the log for startup errors.
If a mod crashes or conflicts, remove it from the copy and note it here.

Not tested yet: nothing has been launched. GTO's jars target GTO's own pack; Radium vs Noisium/C2ME and ImmediatelyFast
vs Oculus are the likely conflicts.

## Java 21 feasibility (checked 2026-11-01, not yet launched)

* The game side: GTO runs the same Minecraft 1.20.1 and Forge 47.4.x on Java 21 (CurseForge per-instance `javaVersionOverride: java21`, runtime 21.0.x already installed under `curseforge/minecraft/Install/java`). The ATM9 instance uses Forge 47.4.6, so the loader is not the blocker.
* af9's own code: `af9-core` compiles to Java 17 bytecode (runs unchanged on 21) and has no use of `sun.misc`, `jdk.internal`, Unsafe or `--add-opens`. Mixins are plain Mixin 0.8.5.
* Build side stays on JDK 17: ForgeGradle for 1.20.1, `.github/workflows/build-af9-core.yml` (java-version 17) and `gtceu-fork/build.sh` need 17 to build. Only the runtime Java changes.
* Unknown: the other ~480 ATM9 mods. Nothing in the logs mentions Java-version warnings, but only a launch proves it. Mods that bundle old ASM or use JDK internals are the usual 21 failures.

How to test: duplicate the ATM9 profile in CurseForge (Profile menu, Duplicate), set Java to 21 for the copy (profile options, Java), run the install script for the perf mods, start it, check `logs/latest.log` for errors and compare with spark. The runtime-only change is reversible by setting Java back to 17.
