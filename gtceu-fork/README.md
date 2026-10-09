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

## Changing the fork

1. Check out upstream at the tag, make the change in a commit.
2. `git format-patch -1 -o <this repo>/gtceu-fork/patches/` (number the files so they apply in order, `0001-...patch`).
3. Raise `gtceu_fork_version` in `af9-core/gradle.properties` (`7.5.3-af9.2`): that makes the next build publish it again.

Export the patches of an existing fork checkout with `git format-patch v.7.2.0-1.20.1..HEAD -o gtceu-fork/patches/`.
