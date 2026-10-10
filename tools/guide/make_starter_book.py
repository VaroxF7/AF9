"""Writes AF9's starter guide: a Patchouli book (data/af9/patchouli_books/starter_guide)."""
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2] / 'af9-core/src/main/resources/data/af9/patchouli_books/starter_guide'
LANG = ROOT / 'en_us'
CAT = 'af9:start'


def text(s):
    return {'type': 'patchouli:text', 'text': s}


def entry(slug, name, icon, sort, pages, secret=False):
    data = {'name': name, 'icon': icon, 'category': CAT, 'sortnum': sort, 'pages': pages}
    p = LANG / 'entries' / (slug + '.json')
    p.parent.mkdir(parents=True, exist_ok=True)
    p.write_text(json.dumps(data, indent=2, ensure_ascii=False) + '\n', encoding='utf8')


def write(path, data):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(data, indent=2, ensure_ascii=False) + '\n', encoding='utf8')


write(ROOT / 'book.json', {
    'name': 'AF9 Starter Guide',
    'landing_text': 'Welcome to AF9, pilot. This book says what to do first: your kit, getting wood without punching '
                    'trees, and the road to the Steam Age.$(br2)Read the entries in order.',
    'version': 1,
    'show_progress': False,
    'book_texture': 'patchouli:textures/gui/book_brown.png',
    'creative_tab': 'misc',
})

write(LANG / 'categories' / 'start.json', {
    'name': 'First steps',
    'description': 'Your kit, your first tools and the way to the Steam Age.',
    'icon': 'minecraft:flint',
    'sortnum': 0,
})

entry('welcome', 'Welcome to AF9', 'minecraft:compass', 0, [
    text('You came down in a drop pod with a kit. It holds:$(br)'
         '$(li)An $(bold)Advanced Prospector (HV)$(), fully charged$(br)'
         '$(li)A $(bold)Steel Mining Hammer$() (mines 3x3)$(br)'
         '$(li)The $(bold)Forgotten Hat$(), already on your head$(br)'
         '$(li)This guide$(br)'
         '$(li)0 to 23 $(bold)dried kelp$(), a random handful of food'),
    text('This pack has $(bold)no tree punching$(): your hands cannot break logs. Do not waste time on trees yet, the next '
         'entry says how to get a tool that can.$(br2)'
         'The tech is GregTech (CEu) with Create for the steam age, Powah for power parts and a lot of space after that. '
         'The quest book has a chapter for every voltage.'),
])

entry('no_tree_punching', 'Wood without punching', 'minecraft:flint', 1, [
    text('$(bold)1. Rocks.$() Small loose rocks lie on the ground (stone, andesite, granite, diorite, sandstone). Pick them up '
         'with your hands. Gravel is everywhere: dig it with the hammer or your hands, and $(bold)three gravel craft into '
         'two flint$().'),
    text('$(bold)2. Knap.$() Hold a $(bold)flint$() and use it on an exposed stone block: it breaks into '
         '$(bold)flint shards$(). Repeat until you have a handful (an axe takes 1, a pickaxe 4).$(br2)'
         '$(bold)3. Sticks.$() Break any $(bold)leaves$(): they drop sticks.'),
    text('$(bold)4. Flint knife.$() In the crafting grid, a flint shard with a stick below it.$(br2)'
         '$(bold)5. Plant fibre.$() Cut tall grass (also leaves, saplings, flowers, wheat) with the knife. '
         '$(bold)Three fibres make a plant string$() (shapeless). Normal string works too.'),
    text('$(bold)6. Flint axe.$() Top row: plant string, flint shard. Row below: stick on the left. '
         'This is the first tool that harvests wood: logs at last.$(br2)'
         '$(bold)7. Flint pickaxe$() (if you want one more): shard, string, shard; shard, stick, shard; stick below. '
         'Your Steel Mining Hammer already mines stone and ore, so you may skip this one.'),
    text('$(bold)Fire.$() A $(bold)fire starter$(): stick and plant string on the top row, flint shard and stick below, '
         'lights campfires and furnaces without flint and steel.$(br2)'
         'Logs do not make planks by hand either: you need the axe or a saw. Planks, sticks and a crafting table follow '
         'as usual from there.'),
])

entry('your_tools', 'Your kit', 'gtceu:prospector.hv', 2, [
    text('$(bold)Advanced Prospector (HV).$() Right-click to scan: a map of the ore veins around you opens (radius in '
         'chunks is on its tooltip). $(bold)Shift + right-click$() changes the mode: ores, fluids (oil and gas) and '
         'bedrock ore veins where the pack has them. It runs on its own battery, already full; charge it in a charger '
         'when it runs low.'),
    text('The map marks the veins by the ore they hold. Copper, tin and iron are all you need for the Steam Age.$(br2)'
         '$(bold)Steel Mining Hammer.$() Breaks a $(bold)3 x 3$() area of what a pickaxe breaks (stone, ore, dirt). '
         'It costs durability for each block. It does not chop wood and you cannot punch ores out with hands.'),
    text('$(bold)Forgotten Hat.$() A hat from Quark, worn for the look. Take it off and put it on any time.$(br2)'
         '$(bold)Dried kelp.$() Eat it, or keep it: 4 kelp make a plant ball in the Create compactor, which you will '
         'want later.'),
])

entry('steam_age', 'Road to the Steam Age', 'create:andesite_alloy', 3, [
    text('The steam age here runs on $(bold)Create$(), not on GregTech\'s steam machines (those are removed). '
         'The first goal: $(bold)steel and your first GT machine$(). The order:$(br2)'
         '$(li)Find copper, tin and iron with the prospector, mine them with the hammer$(br)'
         '$(li)Make a stone furnace or a Create blaze burner; smelt$(br)'
         '$(li)Build Create power: a water wheel or windmill, shafts, cogwheels'),
    text('$(bold)Coke and steel.$() Coal in a Create mixer, $(bold)superheated$() (blaze burner on a blaze cake), makes '
         'coke and creosote (2 coal: 2 coke and 1000 mB). Iron plus coke in a superheated mixer makes $(bold)steel$() '
         '(1 iron and 2 coke give 1 steel; with calcite flux 1 iron, 1 coke and 1 calcite give 2 steel).'),
    text('$(bold)Bronze, plates, wood.$() Copper and tin dust, 3 to 1, in a heated mixer make bronze dust; smelt it as '
         'usual. Ingots go through the Create press for $(bold)plates$(). A log in a millstone gives 2 wood dust. '
         'Water comes from Create\'s mechanical pump, not the primitive pump.'),
    text('$(bold)Glass and tubes.$() Sand in a heated mixer gives molten glass; stick plus 144 mB through a spout makes '
         'a glass tube. Glass tube, steel bolt and copper wire make a vacuum tube; with polished rose quartz it is an '
         '$(bold)electron tube$(), the part of your first circuits.'),
    text('$(bold)Rubber.$() Rubber trees give sticky resin; resin pressed gives 3 raw rubber dust; with sulfur in a '
         'heated mixer it is a rubber ingot. Cables and many machine parts need it.$(br2)'
         'The $(bold)LV circuits$() are made with Create too: a board goes through deployers and a press (see the '
         'quest book).'),
])

entry('next', 'After the Steam Age', 'gtceu:lv_machine_hull', 4, [
    text('Your first GT machine hull is the start of $(bold)LV$(). From there the quest book leads you: MV, then HV, '
         'where machine parts become heavy and Powah\'s Energizing Orb Mk2 makes them (counts in its recipes, molds in '
         'its own slot, marked with a red NC).$(br2)'
         'JEI (or your recipe viewer) shows every recipe: press $(bold)R$() on an item for how to make it and $(bold)U$() for '
         'what it makes.'),
    text('Good habits: label your vein finds on a map, keep a chest of $(bold)every$() part type early, and build '
         'power before you build machines. Lost? Open the quest book, pick the chapter of the voltage you are on and do '
         'the quests in order.$(br2)'
         'Good luck, pilot.'),
])
print('written')
