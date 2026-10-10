# Starter kit and guide book

A player who joins a world for the first time (play time 0, not a spectator) gets, once:

- `gtceu:prospector.hv`, the Advanced Prospector (HV), fully charged;
- `gtceu:steel_mining_hammer` (mines 3 x 3);
- `quark:forgotten_hat`, put on the head (into the inventory when the head slot is taken);
- the **AF9 Starter Guide**, a Patchouli book (`patchouli:guide_book` with `patchouli:book = af9:starter_guide`);
- 0 to 23 `minecraft:dried_kelp` (a random number, none included).

Code: `com.af9.core.starter.StarterKit` (login event, `/af9 starterkit [player]` for operators to give it again).
Config: `starterKit.onFirstJoin` in `af9-common.toml`. A missing item is left out with a warning in the log.

## The book

`af9-core/src/main/resources/data/af9/patchouli_books/starter_guide/book.json` (with `use_resource_pack`: Patchouli 1.20 wants
the contents clientside) and in `af9-core/src/main/resources/assets/af9/patchouli_books/starter_guide/en_us/`: the category
`start` and the entries in `entries/` (welcome, wood without punching, the kit, the road to the Steam Age, after it). The text is generated
by `tools/guide/make_starter_book.py` (edit the text there and run it).

The Steam Age entry follows `docs/early-game.md` (Create mixer, blaze burner, coke, steel, bronze, plates, glass tubes,
rubber, circuits). The wood entry follows No Tree Punching 7.1.0: loose rocks, flint from gravel (3 gravel to 2 flint),
knapping flint on exposed stone into flint shards, sticks from leaves, flint knife (shard over stick), plant fibre from
tall grass with the knife (3 fibre to a plant string), flint axe (string and shard over a stick).
