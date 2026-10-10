// AF9 - which Powah energizing rod a player can have at which GT tier. Powah's own recipes chain the rods one from the
// last with only Powah materials, so the top two were reachable long before the voltage they belong to. Up to LuV the
// best rod is the niotic one (Powah's recipe, unchanged). The spirited rod needs a UV electric motor in its recipe (UV)
// and the nitro rod a UHV machine hull (UHV), with everything else as in Powah.

ServerEvents.recipes(event => {
    event.remove({ id: 'powah:crafting/energizing_rod_spirited' })
    event.remove({ id: 'powah:crafting/energizing_rod_nitro' })

    event.shaped('powah:energizing_rod_spirited', [
        'Q  ',
        'BCB',
        'H M'
    ], {
        Q: '#c:quartz_blocks',
        B: 'powah:capacitor_spirited',
        C: 'powah:dielectric_casing',
        H: 'powah:energizing_rod_niotic',
        M: 'gtceu:uv_electric_motor'
    }).id('af9:powah/energizing_rod_spirited')

    event.shaped('powah:energizing_rod_nitro', [
        'Q  ',
        'BCB',
        'H M'
    ], {
        Q: '#c:quartz_blocks',
        B: 'powah:capacitor_nitro',
        C: 'powah:dielectric_casing',
        H: 'powah:energizing_rod_spirited',
        M: 'gtceu:uhv_machine_hull'
    }).id('af9:powah/energizing_rod_nitro')
})
