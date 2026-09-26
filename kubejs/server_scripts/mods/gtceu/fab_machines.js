// AF9 - Crafting of the SMC fab machines (machines: startup_scripts/gtceu/fab_machines.js, spec: docs §11)
// Each single block upgrades GT's machine of the same tier: chemically resistant lines (polyethylene at MV, PTFE from
// HV on: GT's PTFE only comes at HV), filtered feeds, the tier's pumps or heating wire. The SMC Large Chemical Reactor is GT's Large Chemical Reactor with everything its own recipe takes
// (HV circuits, stainless rotor, PTFE pipes, HV motor) plus filter casings.

ServerEvents.recipes(allthemods => {
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

        allthemods.shaped(`gtceu:${t}_smc_chemical_reactor`, ['FPF', 'UXU', 'CPC'], {
            F: FLUID_FILTER, P: pipe, U: pump, X: `gtceu:${t}_chemical_reactor`, C: circuit
        }).id(`af9:${t}_smc_chemical_reactor`)

        allthemods.shaped(`gtceu:${t}_smc_fractionating_still`, ['FPF', 'UXU', 'CPC'], {
            F: FLUID_FILTER, P: pipe, U: pump, X: `gtceu:${t}_distillery`, C: circuit
        }).id(`af9:${t}_smc_fractionating_still`)

        allthemods.shaped(`gtceu:${t}_smc_electrolytic_cell`, ['FPF', 'WXW', 'CPC'], {
            F: FLUID_FILTER, P: pipe, W: `gtceu:${cable}_single_cable`, X: `gtceu:${t}_electrolyzer`, C: circuit
        }).id(`af9:${t}_smc_electrolytic_cell`)

        allthemods.shaped(`gtceu:${t}_smc_thermal_furnace`, ['FPF', 'WXW', 'CWC'], {
            F: FLUID_FILTER, P: pipe, W: `gtceu:${coil}_double_wire`, X: `gtceu:${t}_arc_furnace`, C: circuit
        }).id(`af9:${t}_smc_thermal_furnace`)
    })

    // ---- Multiblocks ----
    allthemods.shaped('gtceu:smc_large_chemical_reactor', ['CRC', 'PMP', 'FXF'], {
        C: '#gtceu:circuits/hv', R: 'gtceu:stainless_steel_rotor', P: PTFE_LARGE_PIPE, M: 'gtceu:hv_electric_motor',
        F: FILTER_CASING, X: 'gtceu:large_chemical_reactor'
    }).id('af9:smc_large_chemical_reactor')

    allthemods.shaped('gtceu:smc_rectification_column', ['CPC', 'FHF', 'UPU'], {
        C: '#gtceu:circuits/hv', P: PTFE_LARGE_PIPE, F: FILTER_CASING, H: 'gtceu:hv_machine_hull',
        U: 'gtceu:hv_electric_pump'
    }).id('af9:smc_rectification_column')

    allthemods.shaped('gtceu:smc_membrane_cell_hall', ['CWC', 'EHE', 'FPF'], {
        C: '#gtceu:circuits/hv', W: 'gtceu:gold_quadruple_cable', E: 'gtceu:hv_electrolyzer',
        H: 'gtceu:hv_machine_hull', F: FILTER_CASING, P: PTFE_LARGE_PIPE
    }).id('af9:smc_membrane_cell_hall')

    // MV: the furnaces carry the MV silicon line (MG-Si, CZ boules)
    allthemods.shaped('gtceu:smc_thermal_processing_furnace', ['CKC', 'PXP', 'WCW'], {
        C: '#gtceu:circuits/mv', K: 'gtceu:cupronickel_coil_block', P: PE_PIPE,
        X: 'gtceu:electric_blast_furnace', W: 'gtceu:copper_single_cable'
    }).id('af9:smc_thermal_processing_furnace')
})
