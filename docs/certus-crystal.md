# Certus crystal biome

Outcrops of certus quartz (one tall spire of 12-24 blocks with 4-8 slanted shards at its foot, AE2's own buds and
clusters on the faces, about 200 blocks an outcrop), on the Moon and on the asteroids of the Asteroid Field.

- Code: `af9-core/.../space/CertusCrystalFeature.java` (two instances, registered in `AF9Space`).
- Biomes (`data/af9/worldgen/biome`): `af9:certus_crystal` (Moon: only the crystal feature) and
  `af9:certus_crystal_asteroids` (Asteroid Field: the asteroid feature, then the crystals, so the same asteroids
  grow spires). They are two biomes because the Moon must not run the asteroid feature.
- Biome source: `CertusRegionsBiomeSource` (`af9:certus_regions`) in `data/af9/dimension/asteroid_field.json` and in
  `data/ad_astra/dimension/moon.json` (an override of Ad Astra's Moon, a fixed `lunar_wastelands`; re-check it when
  Ad Astra is updated). The world is cut into 3000-block cells, 60 % of them hold one round crystal region of
  140 blocks radius near the middle of the cell (+-500), so two regions are at least 2000 blocks apart: at 32 chunks
  render distance (1024 blocks across) one is visible at a time. Tune `spacing`, `jitter`, `radius`, `chance` in the
  dimension files; the layout is the same in every world (`salt` per dimension).
- Blocks: only `ae2:quartz_block` (the crystal), the budding varieties (`damaged`/`chipped`/`flawed`/`flawless` in
  the lowest blocks of the spire, mostly damaged) and `ae2:small/medium/large_quartz_bud` + `ae2:quartz_cluster`.
  **No quartz glass anywhere.** (Before the 0.1.202 redo the buds were looked up as `ae2:certus_quartz_cluster`, which
  does not exist, so they fell back to amethyst clusters.)

Worldgen only applies to newly generated chunks.
