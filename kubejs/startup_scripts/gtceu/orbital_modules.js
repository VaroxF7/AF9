// AF9 - The Orbital Lithography Array Mk2's modules: small multiblocks of their own that stand in the module columns on the
// crown's roof (photolithography.js, orbitalModuleSlots; af9-core OrbitalLithographyMachine.modules()), as the Space Elevator's
// Mining Modules stand in the tower's. They run no recipe: the Array finds them when it forms and gives every print the bonus
// of each module whose own structure stands (af9-core OrbitalModuleMachine.Kind). Modules of one kind stack, up to a maximum.
//   Parallel Processing Computer      +10 parallels each (as the energy allows)         max 6
//   Energy Efficiency Module          -5% energy of the recipes (not the auxiliary)     max 6
//   Endion Evaporation Annihilation Stack   recipes 10% faster                          max 2
//   Stabilisation Sequencer           half the auxiliary power, running and idle         max 1
// Recipes: server_scripts/mods/gtceu/orbital_modules.js

const $ModuleDirection = Java.loadClass('com.gregtechceu.gtceu.api.pattern.util.RelativeDirection')
const $OrbitalModule = Java.loadClass('com.af9.core.machine.OrbitalModuleMachine')

GTCEuStartupEvents.registry('gtceu:machine', event => {
    const names = {
        parallel: 'Parallel Processing Computer',
        efficiency: 'Energy Efficiency Module',
        evaporation: 'Endion Evaporation Annihilation Stack',
        sequencer: 'Stabilisation Sequencer'
    }
    const tooltips = (key, count) => {
        const lines = []
        for (let i = 0; i < count; i++) lines.push(Component.translatable(`${key}.${i}`))
        return lines
    }
    Object.keys(names).forEach(kind => {
        event.create(`orbital_module_${kind}`, 'multiblock')
            .langValue(names[kind])
            .machine(holder => new $OrbitalModule(holder, $OrbitalModule.kindOf(kind)))
            .rotationState(RotationState.NON_Y_AXIS)
            .appearanceBlock(() => Block.getBlock('gtceu:inert_machine_casing'))
            ['tooltips(net.minecraft.network.chat.Component[])'](tooltips(`af9.orbital_module_${kind}.tooltip`, 8))
            // a module is a column: its controller with a PTFE casing above and below it, the slot's (as the elevator's)
            .pattern(definition => FactoryBlockPattern.start($ModuleDirection.RIGHT, $ModuleDirection.UP,
                $ModuleDirection.BACK)
                .aisle('M', 'S', 'M')
                .where('S', Predicates.controller(Predicates.blocks(definition.get())))
                .where('M', Predicates.blocks('gtceu:inert_machine_casing'))
                .build())
            .workableCasingModel('gtceu:block/casings/solid/machine_casing_inert_ptfe',
                `gtceu:block/multiblock/orbital_module_${kind}`)
    })
})
