// AF9 - The Orbital Lithography Array Mk2's modules: small multiblocks of their own that stand in the module slots on the
// crown's ridge (columns: casing, slot, casing) (photolithography.js, orbitalModuleSlot; af9-core OrbitalLithographyMachine.modules()), as the Space Elevator's
// Mining Modules stand in the tower's (space_elevator.js). They run no recipe: the Array finds them when it forms and gives
// every print the bonus of each module whose own structure stands (af9-core OrbitalModuleMachine.Kind). Same kind stacks.
//   Chrono      run time x0.90          Multiplex   +4 parallels
//   Yield       break chance x0.85      Thrift      EU/t x0.88, coolant per print x0.85
// Recipes: server_scripts/mods/gtceu/orbital_modules.js

const $ModuleDirection = Java.loadClass('com.gregtechceu.gtceu.api.pattern.util.RelativeDirection')
const $OrbitalModule = Java.loadClass('com.af9.core.machine.OrbitalModuleMachine')

GTCEuStartupEvents.registry('gtceu:machine', event => {
    const names = { chrono: 'Chrono', multiplex: 'Multiplex', yield: 'Yield', thrift: 'Thrift' }
    Object.keys(names).forEach(kind => {
        event.create(`orbital_module_${kind}`, 'multiblock')
            .langValue(`Orbital Array ${names[kind]} Module`)
            .machine(holder => new $OrbitalModule(holder, $OrbitalModule.kindOf(kind)))
            .rotationState(RotationState.NON_Y_AXIS)
            .appearanceBlock(() => Block.getBlock('gtceu:inert_machine_casing'))
            ['tooltips(net.minecraft.network.chat.Component[])']([
                Component.translatable(`af9.orbital_module_${kind}.tooltip.0`),
                Component.translatable('af9.orbital_module.tooltip.slot'),
                Component.translatable('af9.orbital_module.tooltip.stack')
            ])
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
