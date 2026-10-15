# Certus crystal biome

Formations of tall white certus quartz spires, in clumps (one or two thick giants of 18-31 blocks with a ring of
smaller spires and cluster buds at the foot), on the Moon and on the asteroids of the Asteroid Field.

- Code: `af9-core/.../space/CertusCrystalFeature.java` (two instances, registered in `AF9Space`).
- Biomes (`data/af9/worldgen/biome`): `af9:certus_crystal` (Moon: only the crystal feature) and
  `af9:certus_crystal_asteroids` (Asteroid Field: the asteroid feature, then the crystals, so the same asteroids
  grow spires). They are two biomes because the Moon must not run the asteroid feature.
- Biome sources: `data/af9/dimension/asteroid_field.json` and `data/ad_astra/dimension/moon.json` (an override of Ad
  Astra's Moon, a fixed `lunar_wastelands`) are checkerboards of 256-block squares, one square in three crystal.
  Re-check the override against the Ad Astra version when it is updated.
- Blocks: `ae2:quartz_block` (foot) and `ae2:quartz_glass` (shaft), `ae2:certus_quartz_cluster` buds, a rare
  `ae2:flawless_budding_quartz` in the foot of the giants (it regrows clusters).

Untested in game (not compiled yet). Check: spire height and lean, the Moon biome's sky and fog (the new biome is a
guess; copy `ad_astra:lunar_wastelands`' effects if it differs), square biome edges.
