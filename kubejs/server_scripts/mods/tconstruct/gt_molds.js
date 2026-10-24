// AF9 - Tinkers' Construct x GT: every fluid of GT with a solid form melts in the Smeltery / Foundry and casts on the
// Casting Table / Basin with GT's casting molds (ingot, nugget, plate, gear, small gear, rotor, block, the fluid pipes,
// glass bottle / tube / block, anvil), and GT's alloys are mixed in the Smeltery from the molten parts. The recipes are
// built from the material registry by com.af9.core.compat.tconstruct.TinkersGtRecipes (af9-core): amounts are GT's
// solidifier amounts, the temperature is the blast temperature, so GT's EBF metals need the hottest Tinkers fuel.
// Spec: docs/tinkers-gt.md

ServerEvents.recipes(event => {
    const $TinkersGt = Java.loadClass('com.af9.core.compat.tconstruct.TinkersGtRecipes')
    const flat = $TinkersGt.build()
    for (let i = 0; i + 1 < flat.length; i += 2) {
        event.custom(JSON.parse(String(flat[i + 1]))).id(String(flat[i]))
    }
    console.info('AF9 Tinkers x GT: ' + (flat.length / 2) + ' recipes')
})
