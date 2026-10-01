# AF9 lint

Four linters that read the repository the way the game would and say what is wrong before anybody has to start Minecraft.
Nothing of GT or Minecraft runs: the scripts load against stubs, the Java is only scanned for names.

```
bash tools/lint/run.sh               # all three
bash tools/lint/run.sh --selftest    # and the self-test (see below)
GT_SRC=/path/to/GregTech-Modern bash tools/lint/run.sh    # also checks GT's own textures and lang keys
```

Needs Node 18+ and Python 3. An exit code of 1 means an **error**; warnings and notes never fail.
The CI workflow runs `run.sh --selftest` before it builds (`.github/workflows/build-af9-core.yml`).

| file | reads | finds |
|---|---|---|
| `scripts.js` | `kubejs/startup_scripts`, `kubejs/server_scripts` | recipes, recipe types, machines, multiblock patterns |
| `quests.py` | `config/ftbquests/quests`, the lang files | quests, tasks, dependencies, texts |
| `assets.py` | KubeJS assets, `af9-core` resources and Java | textures, models, lang files, names |
| `facts.py` | the recipes (`scripts.js --dump`), the quest lang | numbers in quest texts that the recipes decide |
| `selftest.sh` | all of the above, on a scratch copy | proves each rule still finds its mistake |

## Rules

### Scripts (`scripts.js`)

| code | level | what it finds | how to avoid it |
|---|---|---|---|
| S1 | error | a script threw while loading (typo, undefined name) | run the linter before pushing; the first line of the message is the JS error. A stub that is missing (a new GT global) goes into `ctx` in `scripts.js` |
| F1 | warn | a file with the AllTheMods licence header is in this repo | files of the base pack are not ours to ship; do not copy them into the overlay |
| R1 | error | the same recipe id twice in one recipe type (the second silently replaces the first) | give every recipe a unique `af9:` id; generate ids from the loop variable |
| R2 | error | more items / fluids than the machine's slots (`setMaxIOSize`) | check `[items in, items out, fluids in, fluids out]` of the type; circuits and not-consumed items count |
| R3 | error / warn | an id nobody defines: a `kubejs:` item that is not registered, a `gtceu:` name that GT does not have, an `af9:` item that AF9 Core does not register | copy the id from the registration, not from memory; `gtceu:<material>_<shape>` needs the material to have that shape |
| R4 | warn | a recipe type that exists nowhere | the type is spelled `event.recipes.gtceu.<type>`; register new types in `startup_scripts` |
| R5 | warn | an AF9 item that a recipe takes and no recipe makes | add the recipe that makes it, or (world / loot / quest / Java source) one line in `data/sources.txt` |
| R6 | note | AF9 content that no recipe makes or takes | dead content: use it or remove it |
| R7 | warn | in one AF9 recipe type, a recipe whose inputs are all in another (the machine could pick either) | give one of them a circuit or another input |
| R8 | error / warn | no `duration()` / `EUt()`, a duration or EUt of 0, a chance outside 1..10000, a fluid amount that is not a positive integer, a circuit outside 0..32, an item count of 0 | every GT recipe sets `.duration(n).EUt(n)`; a chance is in hundredths of a percent (10000 = 100 %) |
| R9 | warn | a `fab_*` recipe from HV (512 EU/t) on with neither `.cleanroom()` nor `.blastFurnaceTemp()` | the rule (docs §11): non-thermal fab recipes from HV need `.cleanroom(CleanroomType.CLEANROOM)`, thermal ones carry the temperature |
| R10 | error | an AF9 item or material that can never be made: its recipes need ingredients that are themselves unmakeable (a cycle, a missing step) | walk the chain from raw materials; a catalyst that is only made from itself is a dead end |
| R11 | error | a recipe with more different fluid inputs / outputs than the multiblock has fluid hatches for | raise the hatch maximum of the pattern or split the recipe; AF9's coolant hatches count as fluid inputs |
| R12 | error | a recipe above the highest tier of the single blocks that run its type, when no multiblock runs it | lower the EUt or add a tier / a multiblock |
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
| A1 | a registered KubeJS item or block whose texture file is not in the repo | the texture is `kubejs/assets/kubejs/textures/<path>.png` for `kubejs:<path>`; mind the default `kubejs:item/<id>` |
| A2 | a texture that is not square (or a stack of squares with an `.mcmeta`), a broken `.mcmeta`, a frametime that is not a positive integer, `frames` that point past the strip, LDLib `connection` textures that are missing | a strip's height is `frames x width`; validate JSON before committing |
| A3 | a texture that nothing references | note only (a model or Java may use it) |
| A4 | a lang file that is not valid JSON, a key twice, a value that is not a string, a colour code `&x` Minecraft does not know, a **`%d`** / `%f` / `%` at the end of a value | Minecraft reads `%s`, `%1$s` and `%%` only; a text with another `%x` shows its key. `20 % faster` (a space after the `%`) is fine |
| A5 | an item, block, machine or recipe type without a name | `displayName(...)` / `langValue(...)` in the script, or the lang key (`gtceu.<type>` for a recipe type) |
| A6 | an AF9 Core model that points to a texture or parent that does not exist, a blockstate to a missing model, an item or block without model or name, a block without loot table | |
| A7 | a `Component.translatable("af9...")` in the Java that is in no lang file | the key shows raw in the game |
| A8 | tabs and spaces mixed in a lang file | note only: new lines follow the file around them |

### Facts (`facts.py`)

| code | what it finds | how to avoid it |
|---|---|---|
| X1 | a quest text of a node (`af9.quest.litho.n<node>.*`) whose "Fluids per print" / "Coater Track, per wafer" amounts differ from the print and coating recipes | after changing a print or coating recipe, rewrite that text (the message names the key); keep its form `Fluids per print: a X, b Y. Coater Track, per wafer: a X, b Y; N spent solvent out.` |

## Data (`data/`)

* `gt-names.txt`, `gt-materials.txt`, `gt-recipe-types.txt`: the ids GT defines, made by `python3 tools/lint/update-gt-lists.py <GT checkout>`
  from the branch head. **The pack runs GT 7.2.0, the head is 8.0.0**: a name that is new in 8.0.0 passes the lint but does not
  exist in the pack, so a `gtceu:` finding is advisory (WARN), not proof.
* `gt-patterns.txt`: regular expressions for ids that loops in GT make (coil blocks, lenses, pipes, lamps, flawless gems ...).
* `pack.txt`: what the base pack (ATM9's KubeJS and mods, which this repo is laid over) provides and GT's lists lack.
* `sources.txt`: ids a recipe may take that no recipe makes (Java makes them, the world, a quest, the creative tab).

## Self-test

`selftest.sh` copies the repository to a scratch directory, adds the fixtures `selftest/startup.js` and `selftest/server.js` (every
line one mistake) and mutates a quest chapter, a lang file, a texture and a model, then checks that every rule finds its mistake.
When you change a linter, run it: a linter that finds nothing proves nothing. When you add a rule, add its mistake to the fixture.
