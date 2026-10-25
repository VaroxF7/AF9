// AF9 - The Dimensionally Transcendent Plasma Forge's modules: the controllers of the hall's six module slots (the hall's
// pattern, dtpf.js: a floating plate of 12 Inconel 738 Module Casings with a controller in its middle, behind the middle
// injection casing at the top and bottom of each big ring; an empty slot is simply not part of the structure). A module runs no
// recipe: the hall finds it, turns it to face outwards (the controller is rotatable to every side), links it and gives its
// bonus to every run while the slot's casings are all in place (af9-core PlasmaForgeMachine.scanModules, DtpfModuleMachine.Kind).
//   Parallel Processing Assembly      +10 parallels each (as the energy allows; no batch mode)            max 6
//   Superheating Element              field heat x10 (1,000 K to 10,000 K), recipe EU/t x2                 max 1
//   Osmic Retention Stabilisation Sequencer   field held at 30 percent at least, no run lost to a weak field, plasma burn halved   max 1
//   Manipulated Celestial Forge       runs 1.5 times as fast, same EU/t                                    max 2
// The controllers' recipes are the pack owner's (server_scripts/mods/gtceu/dtpf.js has the casing's).
// Docs: docs/dtpf.md, docs/dtpf-modules.md

const $DtpfModule = Java.loadClass('com.af9.core.machine.DtpfModuleMachine')

GTCEuStartupEvents.registry('gtceu:machine', event => {
    const names = {
        parallel: 'Parallel Processing Assembly',
        superheater: 'Superheating Element',
        sequencer: 'Osmic Retention Stabilisation Sequencer',
        forge: 'Manipulated Celestial Forge'
    }
    const tooltips = (key, count) => {
        const lines = []
        for (let i = 0; i < count; i++) lines.push(Component.translatable(`${key}.${i}`))
        return lines
    }
    Object.keys(names).forEach(kind => {
        event.create(`dtpf_module_${kind}`, 'multiblock')
            .langValue(names[kind])
            .machine(holder => new $DtpfModule(holder, $DtpfModule.kindOf(kind)))
            .rotationState(RotationState.ALL)
            .appearanceBlock(() => Block.getBlock('af9:dtpf_module_casing'))
            ['tooltips(net.minecraft.network.chat.Component[])'](tooltips(`af9.dtpf_module_${kind}.tooltip`, 8))
            // the controller alone: its casings are the hall's to check (they come with the slot)
            .pattern(definition => FactoryBlockPattern.start()
                .aisle('S')
                .where('S', Predicates.controller(Predicates.blocks(definition.get())))
                .build())
            .workableCasingModel('af9:block/dtpf_module_casing', `gtceu:block/multiblock/dtpf_module_${kind}`)
    })
})
