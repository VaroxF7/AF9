// AF9 - Crafting of the SMC fab machines (machines: startup_scripts/gtceu/fab_machines.js, spec: docs §11)
// Each single block upgrades GT's machine of the same tier: chemically resistant lines (polyethylene at MV, PTFE from
// HV on: GT's PTFE only comes at HV), filtered feeds, the tier's pumps or heating wire. The SMC Large Chemical Reactor is GT's Large Chemical Reactor with everything its own recipe takes
// (HV circuits, stainless rotor, PTFE pipes, HV motor) plus filter casings.

ServerEvents.recipes(event => {
    const PTFE_PIPE = 'gtceu:polytetrafluoroethylene_normal_fluid_pipe'
    const PE_PIPE = 'gtceu:polyethylene_normal_fluid_pipe'
    const PTFE_LARGE_PIPE = 'gtceu:polytetrafluoroethylene_large_fluid_pipe'
    const FLUID_FILTER = 'gtceu:fluid_filter'
    const FILTER_CASING = 'gtceu:filter_casing'

    // ---- Single blocks, MV-LuV ----
    const tiers = [
        // tier, cable, heating wire (GT's crafting components of that tier)
        ['mv', 'copper', 'cupronickel'],
        ['hv', 'gold', 'kanthal'],
        ['ev', 'aluminium', 'nichrome'],
        ['iv', 'platinum', 'rtm_alloy'],
        ['luv', 'niobium_titanium', 'hssg'],
    ]
    tiers.forEach(([t, cable, coil]) => {
        const circuit = `#gtceu:circuits/${t}`
        const pump = `gtceu:${t}_electric_pump`
        const pipe = t === 'mv' ? PE_PIPE : PTFE_PIPE

        event.shaped(`gtceu:${t}_smc_chemical_reactor`, ['FPF', 'UXU', 'CPC'], {
            F: FLUID_FILTER, P: pipe, U: pump, X: `gtceu:${t}_chemical_reactor`, C: circuit
        }).id(`af9:${t}_smc_chemical_reactor`)

        event.shaped(`gtceu:${t}_smc_fractionating_still`, ['FPF', 'UXU', 'CPC'], {
            F: FLUID_FILTER, P: pipe, U: pump, X: `gtceu:${t}_distillery`, C: circuit
        }).id(`af9:${t}_smc_fractionating_still`)

        event.shaped(`gtceu:${t}_smc_electrolytic_cell`, ['FPF', 'WXW', 'CPC'], {
            F: FLUID_FILTER, P: pipe, W: `gtceu:${cable}_single_cable`, X: `gtceu:${t}_electrolyzer`, C: circuit
        }).id(`af9:${t}_smc_electrolytic_cell`)

        event.shaped(`gtceu:${t}_smc_thermal_furnace`, ['FPF', 'WXW', 'CWC'], {
            F: FLUID_FILTER, P: pipe, W: `gtceu:${coil}_double_wire`, X: `gtceu:${t}_arc_furnace`, C: circuit
        }).id(`af9:${t}_smc_thermal_furnace`)
    })

    // ---- Multiblocks ----
    // Every controller also has an ASIC version: one ASIC chip (phosphorus wafers, HV) wherever it takes a circuit
    const controller = (id, pattern, circuit, keyOf) => {
        event.shaped(`gtceu:${id}`, pattern, keyOf(circuit)).id(`af9:${id}`)
        event.shaped(`gtceu:${id}`, pattern, keyOf('kubejs:asic_chip')).id(`af9:${id}_asic`)
    }

    controller('smc_large_chemical_reactor', ['CRC', 'PMP', 'FXF'], '#gtceu:circuits/hv', C => ({
        C: C, R: 'gtceu:stainless_steel_rotor', P: PTFE_LARGE_PIPE, M: 'gtceu:hv_electric_motor',
        F: FILTER_CASING, X: 'gtceu:large_chemical_reactor'
    }))

    controller('smc_rectification_column', ['CPC', 'FHF', 'UPU'], '#gtceu:circuits/hv', C => ({
        C: C, P: PTFE_LARGE_PIPE, F: FILTER_CASING, H: 'gtceu:hv_machine_hull',
        U: 'gtceu:hv_electric_pump'
    }))

    controller('smc_membrane_cell_hall', ['CWC', 'EHE', 'FPF'], '#gtceu:circuits/hv', C => ({
        C: C, W: 'gtceu:gold_quadruple_cable', E: 'gtceu:hv_electrolyzer',
        H: 'gtceu:hv_machine_hull', F: FILTER_CASING, P: PTFE_LARGE_PIPE
    }))

    // MV: the furnaces carry the MV silicon line (MG-Si, CZ boules)
    controller('smc_thermal_processing_furnace', ['CKC', 'PXP', 'WCW'], '#gtceu:circuits/mv', C => ({
        C: C, K: 'gtceu:cupronickel_coil_block', P: PE_PIPE,
        X: 'gtceu:electric_blast_furnace', W: 'gtceu:copper_single_cable'
    }))
})
