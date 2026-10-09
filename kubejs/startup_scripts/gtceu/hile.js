// AF9 - the Hyper-Intensity Laser Engraver (HILE), GTNH's Industrial Laser Engraver: a 5 x 5 x 5 engraver whose laser comes through a
// glass shaft from the laser hatch at the top onto the plate at the bottom. It runs plasma atomic soldering (gtceu:plasma_soldering:
// UHV+ circuits deposited ion-by-ion with plasma solder; recipes in server_scripts/mods/gtceu/solders.js) and takes over from
// the Orbital Array Mk2, which no longer runs that type. Its UHV recipes draw 100 A of UHV: a laser target hatch carries that.
// Spec: docs/solders.md
//
// The structure is GTNH's, block for block (MTEIndustrialLaserEngraver). Rows bottom -> top, aisles back -> front:
//   S controller (front, bottom centre)   C Laser Containment Casing: any of them may be a hatch
//   F tungstensteel frame   G glass (any of three tiers)   R Laser Resistant Plate   L laser target hatch (above the glass)

const $LaserEngraverMachine = Java.loadClass('com.af9.core.machine.LaserEngraverMachine')
const $HileModels = Java.loadClass('com.af9.core.machine.AF9MachineModels')

GTCEuStartupEvents.registry('gtceu:machine', event => {
    const tooltips = (key, count) => {
        const lines = []
        for (let i = 0; i < count; i++) lines.push(Component.translatable(`${key}.${i}`))
        return lines
    }

    const HILE_SHAPE = [
            ['CCCCC', 'CCCCC', 'CCCCC', 'CCCCC', 'CCCCC'],
            ['CCCCC', 'C C C', 'C C C', 'C C C', 'CCCCC'],
            ['CCCCC', 'C   C', 'F   F', 'F   F', 'FCCCF'],
            ['CCRCC', 'C G C', '  G  ', '  G  ', ' FLF '],
            [' CSC ', '     ', '     ', '     ', '  F  '],
    ]

    event.create('hyper_intensity_laser_engraver', 'multiblock')
        .langValue('Hyper-Intensity Laser Engraver')
        .machine(holder => new $LaserEngraverMachine(holder))
        .rotationState(RotationState.NON_Y_AXIS)
        .recipeTypes([GTRecipeTypes.get('plasma_soldering')])
        // a parallel hatch multiplies the runs; perfect overclocks
        .recipeModifiers([GTRecipeModifiers.PARALLEL_HATCH, GTRecipeModifiers.OC_PERFECT])
        .appearanceBlock(() => Block.getBlock('af9:laser_containment_casing'))
        ['tooltips(net.minecraft.network.chat.Component[])'](tooltips('af9.hile.tooltip', 5))
        .pattern(definition => {
            // parts have a maximum only, never a required count (setMaxGlobalLimited(max, preview count))
            const casing = Predicates.blocks('af9:laser_containment_casing')
                .or(Predicates.abilities(PartAbility.INPUT_ENERGY).setMaxGlobalLimited(2, 1))
                .or(Predicates.abilities(PartAbility.IMPORT_ITEMS).setMaxGlobalLimited(4, 2))
                .or(Predicates.abilities(PartAbility.EXPORT_ITEMS).setMaxGlobalLimited(2, 1))
                .or(Predicates.abilities(PartAbility.IMPORT_FLUIDS).setMaxGlobalLimited(4, 2))
                .or(Predicates.abilities(PartAbility.PARALLEL_HATCH).setMaxGlobalLimited(1, 0))
                .or(Predicates.abilities(PartAbility.MAINTENANCE).setMaxGlobalLimited(1, 1))
            const glass = Predicates.blocks('gtceu:tempered_glass')
                .or(Predicates.blocks('gtceu:laminated_glass'))
                .or(Predicates.blocks('gtceu:fusion_glass'))
            let pattern = FactoryBlockPattern.start()
            HILE_SHAPE.forEach(aisle => { pattern = pattern.aisle(aisle) })
            return pattern
                .where('S', Predicates.controller(Predicates.blocks(definition.get())))
                .where(' ', Predicates.any())
                .where('C', casing)
                .where('F', Predicates.blocks('gtceu:tungstensteel_frame'))
                .where('G', glass)
                .where('R', Predicates.blocks('af9:laser_resistant_plate'))
                .where('L', Predicates.abilities(PartAbility.INPUT_LASER).setMaxGlobalLimited(1, 1))
                .build()
        })
        // the controller: GTNH's engraver face (kubejs assets, overlay_front*)
        // and its beam while it works: down the shaft one block behind the controller, from the plate (1 block above the
        // controller's floor) to the laser hatch's face (4); af9-core LaserEngraverRender
        .model($HileModels.workableCasingWithLaserBeam('af9:block/laser_containment_casing',
            'gtceu:block/multiblock/hile', 1, 1, 4))
        .hasBER(true)
})
