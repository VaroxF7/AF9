# Certus crystal biome

Formations of tall white certus quartz spires, in clumps (one or two thick giants of 18-31 blocks with a ring of
smaller spires and cluster buds at the foot), on the Moon and on the asteroids of the Asteroid Field.

- Code: `af9-core/.../space/CertusCrystalFeature.java` (two instances, registered in `AF9Space`).
- Biomes (`data/af9/worldgen/biome`): `af9:certus_crystal` (Moon: only the crystal feature) and
  `af9:certus_crystal_asteroids` (Asteroid Field: the asteroid feature, then the crystals, so the same asteroids
  grow spires). They are two biomes because the Moon must not run the asteroid feature.
- Biome source: `CertusRegionsBiomeSource` (`af9:certus_regions`) in `data/af9/dimension/asteroid_field.json` and in
  `data/ad_astra/dimension/moon.json` (an override of Ad Astra's Moon, a fixed `lunar_wastelands`; re-check it when
  Ad Astra is updated). The world is cut into 3000-block cells, 60 % of them hold one round crystal region of
  180 blocks radius near the middle of the cell (+-500), so two regions are at least 2000 blocks apart: at 32 chunks
  render distance (1024 blocks across) one is visible at a time. Tune `spacing`, `jitter`, `radius`, `chance` in the
  dimension files; the layout is the same in every world (`salt` per dimension).
- Blocks: `ae2:quartz_block` (foot) and `ae2:quartz_glass` (shaft), `ae2:certus_quartz_cluster` buds, a rare
  `ae2:flawless_budding_quartz` in the foot of the giants (it regrows clusters).

Untested in game (not compiled yet). Check: spire height and lean, the Moon biome's sky and fog (the new biome is a
guess; copy `ad_astra:lunar_wastelands`' effects if it differs), square biome edges.
