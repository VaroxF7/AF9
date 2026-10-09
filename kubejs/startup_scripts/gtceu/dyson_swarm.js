// AF9 - the Dyson Swarm: a power plant that catches starlight with sails. Machine logic: af9-core DysonSwarmMachine (sails in
// the input buses fly on in the swarm, the receiver turns what they catch into power). Recipe: server_scripts/mods/gtceu/dyson_swarm.js.
// Spec: docs/dyson-swarm.md
//
// The structure is GTNH Intergalactic's Dyson Swarm (gtnhintergalactic.tile.multi.TileEntityDysonSwarm), block for block:
// a 16 x 20 x 16 site with the receiver dish and its sphere of air, the deployment unit with its magnets and the command
// centre with its toroid, on a floor of ultra high strength concrete. Transcribed from the original's arrays (the block
// counts checked against its tooltip), aisles back -> front, rows bottom -> top; the original's casings
// are af9-core blocks (dyson_*), its frames GT's, its air ('A') must stay empty:
//   S controller   U concrete floor      A air (the receiver's sphere)   D receiver dish
//   E receiver base casing: a hatch spot for the power OUTPUT (energy or laser)
//   X receiver base casing      F HSS-S frame   G titanium frame   K tritanium frame (the UHV superconductor base's)
//   H stable casing (the original's Hermetic Casing X)   C coil
//   I deployment unit casing: a hatch spot for the sails (input bus) and the coolant (input hatch)
//   Z deployment unit casing    J core   M superconducting magnet
//   O command centre casing: a hatch spot for the computation (data) hatch
//   Y command centre casing    P primary windings   R secondary windings   T toroid casing

const $DysonSwarmMachine = Java.loadClass('com.af9.core.machine.DysonSwarmMachine')
const $LabelWidget = Java.loadClass('com.lowdragmc.lowdraglib.gui.widget.LabelWidget')
const $SliderWidget = Java.loadClass('com.lowdragmc.lowdraglib.gui.widget.SliderWidget')

GTCEuStartupEvents.registry('gtceu:recipe_type', event => {
    // one cycle: an hour on supercooled hydrogen, the power is the machine's (DysonSwarmMachine.SWARM)
    event.create('dyson_swarm')
        .category('multiblock')
        .setEUIO('out')
        .setMaxIOSize(0, 0, 1, 0)
        .setProgressBar(GuiTextures.PROGRESS_BAR_ARROW, FillDirection.LEFT_TO_RIGHT)
        .setSound(GTSoundEntries.ARC)
        // the recipe page: what a swarm of N sails of one tier makes, N set with the slider (a swarm of a mix is the sum)
        .setUiBuilder((recipe, group) => {
            const EU_PER_SAIL = 262144
            const tiers = [['Allthemodium', 1.0], ['Unobtainium Alloy', 2.0], ['Star Matter Tritan Alloy', 3.5]]
            const state = { n: 10000 }
            const grouped = n => String(Math.round(n)).replace(/\B(?=(\d{3})+(?!\d))/g, ',')
            group.addWidget(new $LabelWidget(4, 58, 'Sails in the swarm (slide):'))
            try {
                const slider = new $SliderWidget(v => { state.n = Math.round(v) }, 4, 70, 150, 10)
                slider.setRange(0, 10000)
                slider.setValue(10000)
                group.addWidget(slider)
            } catch (e) {
                console.warn('dyson_swarm page: no slider (' + e + ')')
            }
            group.addWidget(new $LabelWidget(4, 84, () => grouped(state.n) + ' sails of one tier give:'))
            tiers.forEach((tier, i) => {
                group.addWidget(new $LabelWidget(4, 96 + i * 11,
                    () => tier[0] + ': ' + grouped(state.n * EU_PER_SAIL * tier[1]) + ' EU/t'))
            })
        })
})

GTCEuStartupEvents.registry('gtceu:machine', event => {
    const tooltips = (key, count) => {
        const lines = []
        for (let i = 0; i < count; i++) lines.push(Component.translatable(`${key}.${i}`))
        return lines
    }

    const SWARM_SHAPE = [
        ['UUUUUUUUUUUUUUUU', 'YYYYYYY    ZZZZZ', 'OOOOOOO    ZIIIZ', 'YYYYYYY    ZZZZZ', '                ', '                ', '                ', '                ', '                ', '                ', '                ', '                ', '                ', '                ', '                ', '                ', '                ', '                ', '                ', '                '],
        ['UUUUUUUUUUUUUUUU', 'YYYYYYY    ZZZZZ', 'OYYYYYO    IZZZI', 'YYYYYYY    ZZZZZ', '  PPP       KMK ', '            KMK ', '            KMK ', '  TTT       KMK ', '            KMK ', '  TTT       KMK ', '            KMK ', '  TTT       KMK ', '             K  ', '  TTT        K  ', '             K  ', '             K  ', '  TTT        K  ', '  TTT        K  ', '  TTT        K  ', '             K  '],
        ['UUUUUUUUUUUUUUUU', 'YYYYYYY    ZZZZZ', 'OYYYYYO    IZZZI', 'YYPPPYY    ZZJZZ', ' P   P      M M ', '            M M ', '            M M ', ' T G T      M M ', '            M M ', ' T G T      M M ', '            M M ', ' T G T      M M ', '            K K ', ' T G T      K K ', '            K K ', '  TTT       K K ', ' TTTTT      K K ', ' TTTTT      K K ', ' TTTTT      K K ', '  TTT       K K '],
        ['UUUUUUUUUUUUUUUU', 'YYYYYYY    ZZZZZ', 'OYYYYYO    IZZZI', 'YYPYPYY    ZZZZZ', ' P R P      KMK ', '   R        KMK ', '   R        KMK ', ' TGRGT      KMK ', '   R        KMK ', ' TGRGT      KMK ', '   R        KMK ', ' TGRGT      KMK ', '   R         K  ', ' TGRGT       K  ', '   R         K  ', '  TRT        K  ', ' TTTTT       K  ', ' TTTTT       K  ', ' TTTTT       K  ', '  TTT        K  '],
        ['UUUUUUUUUUUUUUUU', 'YYYYYYY    ZZZZZ', 'OYYYYYO    ZIIIZ', 'YYPPPYY    ZZZZZ', ' P   P          ', '                ', '                ', ' T G T          ', '                ', ' T G T          ', '                ', ' T G T          ', '                ', ' T G T          ', '                ', '  TTT           ', ' TTTTT          ', ' TTTTT          ', ' TTTTT          ', '  TTT           '],
        ['UUUUUUUUUUUUUUUU', 'YYYYYYY         ', 'OYYYYYO         ', 'YYYYYYY         ', '  PPP           ', '                ', '                ', '  TTT           ', '                ', '  TTT           ', '                ', '  TTT    DDD    ', '         AAA    ', '  TTT    AAA    ', '         AAA    ', '         AAA    ', '  TTT    AAA    ', '  TTT    AAA    ', '  TTT    AAA    ', '         AAA    '],
        ['UUUUUUUUUUUUUUUU', 'YYYYYYY         ', 'OOOOOOO         ', 'YYYYYYY         ', '                ', '                ', '                ', '                ', '                ', '                ', '         DDD    ', '        DAAAD   ', '        AAAAA   ', '        AAAAA   ', '        AAAAA   ', '        AAAAA   ', '        AAAAA   ', '        AAAAA   ', '        AAAAA   ', '        AAAAA   '],
        ['UUUUUUUUUUUUUUUU', '                ', '                ', '                ', '                ', '                ', '                ', '                ', '                ', '                ', '        DDDDD   ', '       DAAAAAD  ', '       AAAAAAA  ', '       AAAAAAA  ', '       AAAAAAA  ', '       AAAAAAA  ', '       AAAAAAA  ', '       AAAAAAA  ', '       AAAAAAA  ', '       AAAAAAA  '],
        ['UUUUUUUUUUUUUUUU', '        XXXXX   ', '        XEEEX   ', '        XXXXX   ', '                ', '                ', '                ', '                ', '                ', '         DDD    ', '       DDAAADD  ', '      DAAAAAAAD ', '      AAAAAAAAA ', '      AAAAAAAAA ', '      AAAAAAAAA ', '      AAAAAAAAA ', '      AAAAAAAAA ', '      AAAAAAAAA ', '      AAAAAAAAA ', '      AAAAAAAAA '],
        ['UUUUUUUUUUUUUUUU', '        XXXXX   ', '        ECCCE   ', '        XXXXX   ', '         F F    ', '         F F    ', '         F F    ', '         F F    ', '         F F    ', '        DDDDD   ', '      DDAAAAADD ', '     DAAAAAAAAAD', '     AAAAAAAAAAA', '     AAAAAAAAAAA', '     AAAAAAAAAAA', '     AAAAAAAAAAA', '     AAAAAAAAAAA', '     AAAAAAAAAAA', '     AAAAAAAAAAA', '     AAAAAAAAAAA'],
        ['UUUUUUUUUUUUUUUU', '        XXXXX   ', '        ECCCE   ', '        XXXXX   ', '                ', '                ', '                ', '                ', '                ', '        DDDDD   ', '      DDAAFAADD ', '     DAAAAFAAAAD', '     AAAAAFAAAAA', '     AAAAAHAAAAA', '     AAAAAAAAAAA', '     AAAAAAAAAAA', '     AAAAAAAAAAA', '     AAAAAAAAAAA', '     AAAAAAAAAAA', '     AAAAAAAAAAA'],
        ['UUUUUUUUUUUUUUUU', '        XXXXX   ', '        ECCCE   ', '        XXXXX   ', '         F F    ', '         F F    ', '         F F    ', '         F F    ', '         F F    ', '        DDDDD   ', '      DDAAAAADD ', '     DAAAAAAAAAD', '     AAAAAAAAAAA', '     AAAAAAAAAAA', '     AAAAAAAAAAA', '     AAAAAAAAAAA', '     AAAAAAAAAAA', '     AAAAAAAAAAA', '     AAAAAAAAAAA', '     AAAAAAAAAAA'],
        ['UUUUUUUUUUUUUUUU', '        XXSXX   ', '        XEEEX   ', '        XXXXX   ', '                ', '                ', '                ', '                ', '                ', '         DDD    ', '       DDAAADD  ', '      DAAAAAAAD ', '      AAAAAAAAA ', '      AAAAAAAAA ', '      AAAAAAAAA ', '      AAAAAAAAA ', '      AAAAAAAAA ', '      AAAAAAAAA ', '      AAAAAAAAA ', '      AAAAAAAAA '],
        ['UUUUUUUUUUUUUUUU', '                ', '                ', '                ', '                ', '                ', '                ', '                ', '                ', '                ', '        DDDDD   ', '       DAAAAAD  ', '       AAAAAAA  ', '       AAAAAAA  ', '       AAAAAAA  ', '       AAAAAAA  ', '       AAAAAAA  ', '       AAAAAAA  ', '       AAAAAAA  ', '       AAAAAAA  '],
        ['UUUUUUUUUUUUUUUU', '                ', '                ', '                ', '                ', '                ', '                ', '                ', '                ', '                ', '         DDD    ', '        DAAAD   ', '        AAAAA   ', '        AAAAA   ', '        AAAAA   ', '        AAAAA   ', '        AAAAA   ', '        AAAAA   ', '        AAAAA   ', '        AAAAA   '],
        ['UUUUUUUUUUUUUUUU', '                ', '                ', '                ', '                ', '                ', '                ', '                ', '                ', '                ', '                ', '         DDD    ', '         AAA    ', '         AAA    ', '         AAA    ', '         AAA    ', '         AAA    ', '         AAA    ', '         AAA    ', '         AAA    '],
    ]

    event.create('dyson_swarm', 'multiblock')
        .langValue('Dyson Swarm')
        .machine(holder => new $DysonSwarmMachine(holder))
        .rotationState(RotationState.NON_Y_AXIS)
        .recipeTypes([GTRecipeTypes.get('dyson_swarm')])
        // the sails set the power; no overclocking (a generator)
        .recipeModifiers([$DysonSwarmMachine.SWARM])
        .appearanceBlock(() => Block.getBlock('af9:dyson_receiver_casing'))
        ['tooltips(net.minecraft.network.chat.Component[])'](tooltips('af9.dyson_swarm.tooltip', 7))
        .pattern(definition => {
            // parts have a maximum only, never a required count (setMaxGlobalLimited(max, preview count))
            const receiver = Predicates.blocks('af9:dyson_receiver_casing')
                .or(Predicates.abilities(PartAbility.OUTPUT_ENERGY).setMaxGlobalLimited(8, 2))
                .or(Predicates.abilities(PartAbility.OUTPUT_LASER).setMaxGlobalLimited(8, 2))
            const deployment = Predicates.blocks('af9:dyson_deployment_casing')
                .or(Predicates.abilities(PartAbility.IMPORT_ITEMS).setMaxGlobalLimited(4, 1))
                .or(Predicates.abilities(PartAbility.IMPORT_FLUIDS).setMaxGlobalLimited(4, 1))
            const command = Predicates.blocks('af9:dyson_control_casing')
                .or(Predicates.abilities(PartAbility.COMPUTATION_DATA_RECEPTION).setMaxGlobalLimited(2, 1))
                .or(Predicates.abilities(PartAbility.MAINTENANCE).setMaxGlobalLimited(1, 1))
            let pattern = FactoryBlockPattern.start()
            SWARM_SHAPE.forEach(aisle => { pattern = pattern.aisle(aisle) })
            return pattern
                .where('S', Predicates.controller(Predicates.blocks(definition.get())))
                .where(' ', Predicates.any())
                .where('A', Predicates.air())
                .where('U', Predicates.blocks('af9:ultra_high_strength_concrete_floor'))
                .where('D', Predicates.blocks('af9:dyson_receiver_dish'))
                .where('E', receiver)
                .where('X', Predicates.blocks('af9:dyson_receiver_casing'))
                .where('F', Predicates.blocks('gtceu:hsss_frame'))
                .where('G', Predicates.blocks('gtceu:titanium_frame'))
                .where('K', Predicates.blocks('gtceu:tritanium_frame'))
                .where('H', Predicates.blocks('gtceu:stable_machine_casing'))
                .where('C', Predicates.blocks('af9:resonant_endion_coil_block'))
                .where('I', deployment)
                .where('Z', Predicates.blocks('af9:dyson_deployment_casing'))
                .where('J', Predicates.blocks('af9:dyson_deployment_core'))
                .where('M', Predicates.blocks('af9:dyson_deployment_magnet'))
                .where('O', command)
                .where('Y', Predicates.blocks('af9:dyson_control_casing'))
                .where('P', Predicates.blocks('af9:dyson_control_primary'))
                .where('R', Predicates.blocks('af9:dyson_control_secondary'))
                .where('T', Predicates.blocks('af9:dyson_control_toroid'))
                .build()
        })
        // the controller: the receiver casing with GTNH's Dyson Swarm face (kubejs assets, overlay_front*)
        .workableCasingModel('af9:block/dyson_receiver_casing', 'gtceu:block/multiblock/dyson_swarm')
})
