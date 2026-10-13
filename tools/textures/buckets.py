#!/usr/bin/env python3
"""Buckets and liquid blocks for the placeable fluids: the content overlay, one item model and one
blockstate per fluid, and the names. The fluid list lives in Java
(af9-core/src/main/java/com/af9/core/fluid/PlaceableFluids.java, between BEGIN-FLUIDS and END-FLUIDS):
add the gtceu fluid path there, rerun this, rebuild.

Run from the repository root (needs Pillow):  python3 tools/textures/buckets.py

Writes into af9-core/src/main/resources/assets/af9:
  textures/item/bucket_content.png (white mask of the bucket's interior, tinted per fluid by BucketTint),
  models/item/<fluid>_bucket.json (vanilla bucket below, the tinted content above),
  blockstates/<fluid>.json (the vanilla water shape; the fluid's own textures come from its type),
and into af9-core/src/main/resources/data/af9:
  loot_tables/blocks/<fluid>.json (empty: a spilled liquid drops nothing),
and adds item.af9.<fluid>_bucket / block.af9.<fluid> to lang/en_us.json (title-cased fallback names).
"""
import json
import os
import re

from PIL import Image

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), '../..')
RES = os.path.join(ROOT, 'af9-core/src/main/resources/assets/af9')
JAVA = os.path.join(ROOT, 'af9-core/src/main/java/com/af9/core/fluid/PlaceableFluids.java')


def fluids():
    with open(JAVA, encoding='utf-8') as src:
        body = src.read()
    block = body.split('BEGIN-FLUIDS')[1].split('END-FLUIDS')[0]
    return re.findall(r'"([a-z0-9_]+)"', block)


def name(path):
    return ' '.join(word.capitalize() for word in path.split('_'))


def main():
    paths = fluids()
    print('%d placeable fluids' % len(paths))
    models = os.path.join(RES, 'models/item')
    states = os.path.join(RES, 'blockstates')
    textures = os.path.join(RES, 'textures/item')
    loot = os.path.join(ROOT, 'af9-core/src/main/resources/data/af9/loot_tables/blocks')
    for folder in (models, states, textures, loot):
        os.makedirs(folder, exist_ok=True)
    for path in paths:
        with open(os.path.join(models, path + '_bucket.json'), 'w', newline='\n') as out:
            json.dump({'parent': 'minecraft:item/generated', 'textures': {
                'layer0': 'minecraft:item/bucket',
                'layer1': 'af9:item/bucket_content'}}, out, indent=2)
            out.write('\n')
        with open(os.path.join(states, path + '.json'), 'w', newline='\n') as out:
            json.dump({'variants': {'': {'model': 'minecraft:block/water'}}}, out, indent=2)
            out.write('\n')
        with open(os.path.join(loot, path + '.json'), 'w', newline='\n') as out:
            json.dump({'type': 'minecraft:block', 'pools': []}, out, indent=2)
            out.write('\n')
    # the interior of vanilla's bucket, filled white and tinted per fluid at runtime
    img = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
    px = img.load()
    for y in range(4, 7):
        for x in range(5, 11):
            px[x, y] = (255, 255, 255, 255)
    img.save(os.path.join(textures, 'bucket_content.png'))
    lang = os.path.join(RES, 'lang/en_us.json')
    with open(lang, encoding='utf-8') as src:
        text = src.read()
        data = json.loads(text)
    for path in paths:
        data.setdefault('item.af9.%s_bucket' % path, '%s Bucket' % name(path))
        data.setdefault('block.af9.%s' % path, name(path))
    with open(lang, 'w', encoding='utf-8', newline='\n') as out:
        json.dump(data, out, indent=4, ensure_ascii=False)
        out.write('\n')
    print('buckets written')


if __name__ == '__main__':
    main()
