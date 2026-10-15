// AF9 - AE2 cable dyeing, in the assembler at LV (dry, like the cables).
//
// Only the fluix base of each family is assembler-built (ae2_devices.js); the
// fifteen dye variants move from the crafting grid to the assembler (base
// cable + vanilla dye, 100t). Vanilla shapeless dye recipes for these outputs
// are removed mod-scoped. See docs/ae2.md.

ServerEvents.recipes(event => {
    if (!Platform.isLoaded('ae2')) return
    const VA = GTValues.VA
    const LV = VA[GTValues.LV]
    const gtr = event.recipes.gtceu

    const colors = ['white', 'orange', 'magenta', 'light_blue', 'yellow', 'lime',
        'pink', 'gray', 'light_gray', 'cyan', 'purple', 'blue', 'brown',
        'green', 'red', 'black']
    // [family suffix, fluix base cable]
    const families = [
        ['glass_cable', 'ae2:fluix_glass_cable'],
        ['covered_cable', 'ae2:fluix_covered_cable'],
        ['covered_dense_cable', 'ae2:fluix_covered_dense_cable'],
        ['smart_cable', 'ae2:fluix_smart_cable'],
        ['smart_dense_cable', 'ae2:fluix_smart_dense_cable']
    ]
    colors.forEach(color => {
        families.forEach(family => {
            const suffix = family[0]
            const base = family[1]
            const out = 'ae2:' + color + '_' + suffix
            event.remove({ mod: 'ae2', output: out })
            gtr.assembler('af9:dye_' + color + '_' + suffix)
                .itemInputs(base, 'minecraft:' + color + '_dye')
                .itemOutputs(out)
                .duration(100)
                .EUt(LV)
        })
    })
})
