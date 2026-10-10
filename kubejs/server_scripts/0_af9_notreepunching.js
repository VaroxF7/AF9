// AF9 - runs before the base pack's conflicts.js (the 0 sorts first).
//
// No Tree Punching replaces the vanilla recipes of the stick, the wooden and stone tools, the brick, the campfires and
// the flower pot with placeholders of the type notreepunching:empty (they have no ingredients and no key). The base pack's
// conflicts.js goes through the recipes of `minecraft:stick` and `minecraft:wooden_*` and adds a "key" to each: on a
// placeholder `json.get('key')` is null, "Cannot call method add of null" stopped the script and every recipe change after
// it was lost. The placeholders do nothing (the vanilla recipe is gone either way), so they are removed first.
ServerEvents.recipes(event => {
    event.remove({ type: 'notreepunching:empty' })
})
