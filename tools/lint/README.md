# AF9 lint

Seven linters that read the repository the way the game would and say what is wrong before anybody has to start Minecraft.
Nothing of GT or Minecraft runs: the scripts load against stubs, the Java is only scanned for names, and what AF9 Core
registers is read from a list that its dev run writes (see "The registry list").

```
bash tools/lint/run.sh               # all of them
bash tools/lint/run.sh --selftest    # and the self-test (see below)
GT_SRC=/path/to/GregTech-Modern bash tools/lint/run.sh    # also checks GT's own textures, lang keys and classes
```

Needs Node 18+ and Python 3. An exit code of 1 means an **error**; warnings and notes never fail.
The CI workflow (job `lint`) runs `run.sh --selftest` before it builds (`.github/workflows/build-af9-core.yml`).

| file | reads | finds |
|---|---|---|
| `scripts.js` | `kubejs/startup_scripts`, `kubejs/server_scripts`, the registry list | recipes, recipe types, machines, multiblock patterns |
| `quests.py` | `config/ftbquests/quests`, the lang files | quests, tasks, dependencies, texts |
| `assets.py` | KubeJS assets, `af9-core` resources and Java, the registry list | textures, models, lang files, names |
| `facts.py` | the recipes (`scripts.js --dump`), the quest lang | numbers in quest texts that the recipes decide |
| `docs.py` | README.md, docs/*.md | stale paths, dead links, sections that do not exist |
| `links.py` | the `Java.loadClass` calls of the scripts, AF9 Core's (and GT's) Java sources | classes and static members that do not exist |
| `previews.py` | AF9 Core's Java sources | multiblock preview pages that can carry null cells |
| `selftest.sh` | all of the above, on a scratch copy | proves each rule still finds its mistake |

## Rules

### Scripts (`scripts.js`)

| code | level | what it finds | how to avoid it |
|---|---|---|---|
| S2 | error | a machine or recipe type registered twice | search the startup scripts for the id before registering; one table per family |
| S4 | error | a startup script that registers an item, a block, a fluid, a material, a material icon set or an ore layer | AF9 Core registers them (`af9-core/src/main/java/com/af9/core/registry`: `AF9Items`, `AF9Blocks`, `AF9Materials`; the ore layer: `space/AF9Space`). Blocks and items are `af9:<id>` with their names, models and textures in AF9 Core's resources; materials keep `gtceu:<name>` |
| S3 | error | a `const` declared in the body of a `for` / `while` loop: KubeJS's engine (Rhino) keeps it at the first pass's value, Node (this linter) does not, so the script passes here and fails in the game (the reticles registered `ilc_reticle` twice that way) | loop with a callback (`forEach`, `map`): a `const` in a callback is new each call |
| S5 | error | a startup script that loads a GT block-holder class (`Java.loadClass('...GTBlocks'/'...GCYMBlocks')`) at its top level: forcing the class init while KubeJS is still constructing (before GT's materials exist) NPEs GT's own startup, and the game crashes with a misleading follow-on error (the hearth died as "missing pattern") | load it inside the registry callback instead, where the registries are up |
| S6 | error | spread / rest syntax (`f(...list)`, `[...a]`, `(...args) =>`): Rhino does not parse it, so the whole script fails to load, while Node (this linter) runs it (four server scripts lost all their recipes that way) | pass the list itself: GT's recipe builder takes an array (`.itemInputs(list)`, `.inputFluids(list)`); elsewhere `concat`, `apply` or a loop |
| S7 | error | `Array.from({ length: n })`: Rhino fills it with n holes, and `forEach` / `map` skip holes, so the body never runs (the DTPF's pattern had no aisles that way, and its empty preview page aborted GT's JEI registration) | `Array(n).fill(0).forEach((_, i) => ...)` |
| S1 | error | a script threw while loading (typo, undefined name) | run the linter before pushing; the first line of the message is the JS error. A stub that is missing (a new GT global) goes into `ctx` in `scripts.js` |
| F1 | warn | a file with the AllTheMods licence header is in this repo | files of the base pack are not ours to ship; do not copy them into the overlay |
| R1 | error | the same recipe id twice in one recipe type (the second silently replaces the first) | give every recipe a unique `af9:` id; generate ids from the loop variable |
| R2 | error | more items / fluids than the machine's slots: AF9's types (`setMaxIOSize`) and GT's (`data/gt-recipe-slots.txt`, from GT 7.5.3: the **assembler has 9 item and ONE fluid slot**, the circuit assembler 6 and one) | check `[items in, items out, fluids in, fluids out]` of the type; circuits and not-consumed items count |
| R3 | error / warn | an id nobody defines: an `af9:` item, block or fluid that AF9 Core does not register (the registry list), a `gtceu:` name that GT does not have, a `kubejs:` item that is not the base pack's | copy the id from the registration, not from memory; `gtceu:<material>_<shape>` needs the material to have that shape |
| R4 | warn | a recipe type that exists nowhere | the type is spelled `event.recipes.gtceu.<type>`; register new types in `startup_scripts` |
| R5 | warn | an AF9 item that a recipe takes and no recipe makes | add the recipe that makes it, or (world / loot / quest / Java source) one line in `data/sources.txt` |
| R6 | note | AF9 content that no recipe makes or takes | dead content: use it or remove it |
| R7 | warn | in one recipe type, a recipe of AF9 whose inputs are all in another's, in at least the same amounts (a machine loaded for the second could run the first instead) | give one of them a programmed circuit (`.circuit(n)`) or another input |
| R8 | error / warn | no `duration()` / `EUt()`, a duration or EUt of 0, a chance outside 1..10000, a fluid amount that is not a positive integer, a circuit outside 0..32, an item count of 0 | every GT recipe sets `.duration(n).EUt(n)`; a chance is in hundredths of a percent (10000 = 100 %) |
| R9 | warn | a `fab_*` recipe from HV (512 EU/t) on with neither `.cleanroom()` nor `.blastFurnaceTemp()` | the rule (docs §11): non-thermal fab recipes from HV need `.cleanroom(CleanroomType.CLEANROOM)`, thermal ones carry the temperature |
| R10 | error | an AF9 item or material that can never be made: its recipes need ingredients that are themselves unmakeable (a cycle, a missing step) | walk the chain from raw materials; a catalyst that is only made from itself is a dead end |
| R11 | error | a recipe with more different fluid inputs / outputs than the multiblock has fluid hatches for | raise the hatch maximum of the pattern or split the recipe; AF9's coolant hatches count as fluid inputs |
| R12 | error | a recipe above the highest tier of the single blocks that run its type, when no multiblock runs it | lower the EUt or add a tier / a multiblock |
| R13 | error | a furnace recipe (`electric_blast_furnace`, `boule_melting`, `fab_calcination`, `fab_cvd`, `fab_crystal_growth`) without `.blastFurnaceTemp()` | thermal recipes carry their temperature; GT's furnace condition and the fab thermal modes read it |
| M1 | error / warn | pattern: rows of different width, aisles of different height, a character without `where()` (or one never used), no or several controllers, a **minimum** or **exact** part count, `autoAbilities`, one ability limited twice | AF9 rule: parts have maximums only (`setMaxGlobalLimited(max, preview)`); `autoAbilities` forces energy + maintenance hatches |
| M3 | error | a pattern names a block nobody defines | the structure could never form; copy the block id from its registration |
| M4 | error | an AF9 recipe type that no machine runs | the recipes could never run; add the type to a machine's `recipeTypes` (or to the Java that attaches it, `BouleMelting`) |
| L1 | error | a `Component.translatable` key (tooltips) that is in no lang file | add it to `kubejs/assets/gtceu/lang/en_us.json` (machine tooltips) or the kubejs lang |

### Quests (`quests.py`)

| code | what it finds | how to avoid it |
|---|---|---|
| Q1 | an id used twice anywhere in the quest files | FTB Quests loads the second over the first; generate ids with `secrets.token_hex(8).upper()` and **check them against all chapters**, not only your own |
| Q2 | an id that is not 16 upper-case hex digits, or whose first digit is above 7 | FTB Quests reads it as a signed long: the first digit is 0-7 |
| Q3 | a dependency on a quest that does not exist, on itself, or a cycle | a quest that depends on a missing id can never unlock |
| Q4 | two AF9 quests (or a quest and a link) on the same spot or closer than 0.8 | quests lie on a grid; look at the chapter before placing |
| Q5 | a `{key}` text that is in no lang file; description paragraphs numbered out of order | the text shows as the raw key; keep the paragraph keys `.1`, `.2`, `.3` in the order they are shown |
| Q6 | a task that wants an item or fluid nobody defines, an amount that is not a positive integer, a task without id | the quest can never be completed |
| Q7 | a quest without a task, a reward without a type, a chapter without a title | |
| Q8 | a quest text key that no quest uses | dead text: remove it |

Only chapters AF9 wrote (and AF9's quests in the base pack's chapters) are checked strictly. The base pack's own quests are only
checked for ids (Q1, Q2) and for `{af9...}` texts.

### Assets (`assets.py`)

| code | what it finds | how to avoid it |
|---|---|---|
| A2 | a texture that is not square (or a stack of squares with an `.mcmeta`; an entity's sheet under `textures/entity` may be any shape), a broken `.mcmeta`, a frametime that is not a positive integer, `frames` that point past the strip, LDLib `connection` textures that are missing | a strip's height is `frames x width`; validate JSON before committing |
| A3 | an item or block texture that no model, script or Java source names | note only |
| A4 | a lang file that is not valid JSON, a key twice, a value that is not a string, a colour code `&x` Minecraft does not know, a **`%d`** / `%f` / `%` at the end of a value | Minecraft reads `%s`, `%1$s` and `%%` only; a text with another `%x` shows its key. `20 % faster` (a space after the `%`) is fine |
| A5 | a machine, material or recipe type without a name | `langValue(...)` in the script, or the lang key (`gtceu.<type>` for a recipe type, `material.gtceu.<id>` for a material) |
| A6 | an AF9 Core model that points to a texture or parent that does not exist, a blockstate to a missing model, an item or block AF9 Core registers without model, name, blockstate or loot table, an item model or a blockstate of something that is not registered | a new item needs its line in `AF9Items`, `item.af9.<id>` in the lang file, `models/item/<id>.json` and the texture; a block also its blockstate, block model, loot table and the line in `data/minecraft/tags/blocks/mineable/pickaxe.json`. Then refresh the registry list |
| A7 | a `Component.translatable("af9...")` in the Java that is in no lang file | the key shows raw in the game |
| A8 | tabs and spaces mixed in a lang file | note only: new lines follow the file around them |

### Facts (`facts.py`)

| code | what it finds | how to avoid it |
|---|---|---|
| X2 | `LithoMode.java` (Java) and the print recipes (KubeJS) disagree: a mode without recipes, a print that does not take the substrate's coated wafer, the wrong broken wafer or break chance, the wrong EUt or amps | the nine modes live in three places (Java `LithoMode`, `AF9_WAFERS` in the server script, the recipe types in the startup script): change them together |
| X1 | a quest text of a node (`af9.quest.litho.n<node>.*`) whose "Fluids per print" / "Coater Track, per wafer" amounts differ from the print and coating recipes | after changing a print or coating recipe, rewrite that text (the message names the key); keep its form `Fluids per print: a X, b Y. Coater Track, per wafer: a X, b Y; N spent solvent out.` |
| X3 | a quest text of the Asteroid Fission chapter (`af9.quest.fx.*`) that does not say a fluid amount, run time, steam rate or EU per mB of its recipe (the reactor cycle, leach, precipitation, UF6, pellets, dissolving, centrifuge, coolant, propellant, steam turbine) | after changing one of those recipes, rewrite the text (the message names the key); amounts are written with a comma and the unit, `61,440 mB`, `1,200 ticks` |
| X4 | a name Java, data and scripts share that does not agree: the rock `AsteroidFieldFeature` builds vs the stones of the ore layer `af9_asteroid`, the reactor id `RadiationWatch` looks for, the fluid tag and vapor name of `ExtremeReactorsCompat`, the dimensions of the layer and the veins, the planets (`data/af9/planets`) and the space station recipe | a typo here fails silently in the game (ore that never grows, a warning that never shows); change both sides together |

### Docs (`docs.py`)

| code | what it finds | how to avoid it |
|---|---|---|
| D1 | a `kubejs/...`, `af9-core/...`, `config/...`, `docs/...`, `tools/...` path in the documents that is no file | when a file moves, search the documents for its old path (the appendix file map of `docs/semiconductor-factory.md` is the usual victim) |
| D2 | a Markdown link to a file that does not exist | |
| D3 | a `§6.4`-style reference to a section of `docs/semiconductor-factory.md` that has no heading | sections are renumbered rarely: keep the numbers, add `b`, `c` ... (6.5b) |

### Links (`links.py`)

| code | what it finds | how to avoid it |
|---|---|---|
| J1 | `Java.loadClass('com.af9...')` of a class that is not in AF9 Core (with `GT_SRC` also GT's) | the script stops with an error at startup; rename the class in the script when you rename it in Java |
| J2 | `$Class.MEMBER` in a script where the Java source of the class does not mention MEMBER | a typo in a static field or method name; copy it from the Java source |

### Previews (`previews.py`)

| code | what it finds | how to avoid it |
|---|---|---|
| P1 | `new MultiblockShapeInfo(<array>)` where the array is neither GTCEu's own `getPreview(...)` output (or a variable assigned from it) nor passed through a null-sanitizer like `solid(...)` | GTCEu's preview widget dereferences every cell with no null check: one null cell aborts the whole JEI registration and every GTCEu recipe vanishes (October 2026: the Orbital Array Mk2's second preview page did exactly this). Fill holes with `BlockInfo.EMPTY` before constructing the page |

## Data (`data/`)

* `gt-names.txt`, `gt-materials.txt`, `gt-recipe-types.txt`, `gt-recipe-slots.txt`: the ids and recipe slots GT defines, made by
  `python3 tools/lint/update-gt-lists.py <GT checkout>` from **GT 7.5.3** (tag `v7.5.3-1.20.1`, the version the pack runs): clone that
  tag, not a branch head, or names of a newer GT pass the lint that the pack does not have. The name lists over-approximate (every
  string literal of the registration code), so a `gtceu:` finding means "no such name anywhere", a missing finding does not prove it exists.
* `gt-dev-only.txt`: the `gtceu:` items a dev run registers and the pack's game does not: the parts above UV, the electric
  machines above UV and the hatches above UHV of GT's high-tier content (off in the pack; outside production GT turns it on by
  itself, so the headless run and the lists above have them), and the flawed / chipped gems. Made by comparing the dev run's
  registry dump (`-Paf9Dump`) with the item registry in a played world's `level.dat` (`fml` > `Registries` > `minecraft:item`).
  A recipe naming one is an R3 error: GT 7.5 refuses the recipe.
* `gt-patterns.txt`: regular expressions for ids that loops in GT make (coil blocks, lenses, pipes, lamps, flawless gems ...).
* `pack.txt`: what the base pack (ATM9's KubeJS and mods, which this repo is laid over) provides and GT's lists lack.
* `sources.txt`: ids a recipe may take that no recipe makes (Java makes them, the world, a quest, the creative tab).
* `af9-registry.txt`: what AF9 Core registers (next section).

## The registry list

`data/af9-registry.txt` says what AF9 Core registers: `item af9:<id>`, `block af9:<id>` (a machine's block is marked
`machine`), `fluid af9:<id>`, `material gtceu:<name>` (one with an ore is marked `ore`), a line each. A linter cannot read
that out of the Java (loops and tables make the ids), so the game writes it: AF9 Core's headless dev run boots Minecraft with
GT and AF9 Core, writes the list and stops, in about a minute or two.

```
cd af9-core
./gradlew runGameTestServer -Paf9Instance="<folder of an installed pack instance>"
```

The run takes the mods AF9 Core needs at runtime and that are on no Maven repository (Ad Astra's libraries, KubeJS) from the
`mods` folder of that instance. Run it after adding, renaming or removing a block, an item or a material, and commit the
list with the change. Where no instance is at hand, edit the list by hand: it is sorted, a line a thing. `assets.py` (A6) holds the list
against the models and block states, so a line that is missing or left over shows up.

## Self-test

`selftest.sh` copies the repository to a scratch directory, adds the fixtures `selftest/startup.js` and `selftest/server.js` (every
line one mistake), a few items to the registry list, and mutates a quest chapter, a lang file, a texture and a model, then checks
that every rule finds its mistake.
When you change a linter, run it: a linter that finds nothing proves nothing. When you add a rule, add its mistake to the fixture.
