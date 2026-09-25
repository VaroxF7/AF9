// AF9 - Wafers: the nine substrates, their broken and contaminated wafers. Spec: docs/semiconductor-factory.md
//
// One lithography mode per substrate (com.af9.core.litho.LithoMode in af9-core; the server side of this table is
// AF9_WAFERS in server_scripts/mods/gtceu/photolithography.js - keep the three in sync):
//   #  substrate               node   machine                      made by
//   0  Silicon                 350 nm Photolithography Line V1     boule (EBF Boule Melting)
//   1  Phosphorus-doped        200 nm line V2                      boule
//   2  Naquadah-doped          100 nm line V3                      boule
//   3  Trinium-doped            80 nm line V4                      boule
//   4  Naquadria-doped          65 nm line V5                      boule
//   5  Neutronium-doped         50 nm line V6                      boule
//   6  Transmuted Neutronium    20 nm line V7                      neutron irradiation (Particle Accelerator)
//   7  Strange Matter-doped      7 nm line V8                      boule
//   8  Chromodynium              1 nm Orbital Lithography Station  boule
// (Trinium comes before naquadria because GT smelts trinium at LuV and naquadria only at ZPM.)
//
// A substrate prints every chip whose own substrate (GT's: silicon for ILC ... Simple SoC, phosphorus for NAND, NOR,
// MPIC, SoC, naquadah for ASoC, neutronium for HASoC) is the same or lower. The print is always GT's own chip wafer
// (gtceu:ram_wafer ...); a better substrate gives more of them per blank (AF9_WAFERS.yieldOf in the server script).
//
// Every substrate also has a broken wafer (a failed print) and a contaminated wafer (handled without gloves outside a
// clean room, see af9-core WaferContamination). Chips contaminate the same way: every chip GT's cutter makes of a
// printed wafer has a contaminated chip (kubejs:contaminated_<chip>).

const AF9_WAFER_TABLE = (() => {
    // id, blank wafer, blank wafer name (GT's names for GT's four)
    const substrates = [
        { id: 'silicon', blank: 'gtceu:silicon_wafer', name: 'Silicon Wafer' },
        { id: 'phosphorus', blank: 'gtceu:phosphorus_wafer', name: 'Phosphorus-doped Wafer' },
        { id: 'naquadah', blank: 'gtceu:naquadah_wafer', name: 'Naquadah-doped Wafer' },
        { id: 'trinium', blank: 'kubejs:trinium_wafer', name: 'Trinium-doped Wafer' },
        { id: 'naquadria', blank: 'kubejs:naquadria_wafer', name: 'Naquadria-doped Wafer' },
        { id: 'neutronium', blank: 'gtceu:neutronium_wafer', name: 'Neutronium-doped Wafer' },
        { id: 'transmuted_neutronium', blank: 'kubejs:transmuted_neutronium_wafer', name: 'Transmuted Neutronium Wafer' },
        { id: 'strange_matter', blank: 'kubejs:strange_matter_wafer', name: 'Strange Matter-doped Wafer' },
        { id: 'chromodynium', blank: 'kubejs:chromodynium_wafer', name: 'Chromodynium Wafer' }
    ]
    // GT's chips (item id, English name): the dies the cutter makes of the printed wafers
    const chips = [
        ['ilc_chip', 'IC Chip'], ['ram_chip', 'RAM Chip'], ['cpu_chip', 'CPU Chip'], ['ulpic_chip', 'ULPIC Chip'],
        ['lpic_chip', 'LPIC Chip'], ['simple_soc', 'Simple SoC'], ['nand_memory_chip', 'NAND Memory Chip'],
        ['nor_memory_chip', 'NOR Memory Chip'], ['mpic_chip', 'MPIC Chip'], ['soc', 'SoC'], ['advanced_soc', 'ASoC'],
        ['highly_advanced_soc', 'HASoC'], ['nano_cpu_chip', 'Nano CPU Chip'], ['qbit_cpu_chip', 'Qubit CPU Chip'],
        ['hpic_chip', 'HPIC Chip'], ['uhpic_chip', 'UHPIC Chip']
    ]
    return { substrates: substrates, chips: chips }
})()

StartupEvents.registry('item', allthemods => {
    const table = AF9_WAFER_TABLE
    // new blank wafers (GT has silicon, phosphorus, naquadah and neutronium)
    table.substrates.filter(s => s.blank.startsWith('kubejs:')).forEach(s => {
        allthemods.create(s.blank.substring('kubejs:'.length))
            .displayName(s.name)
            .texture(`kubejs:item/wafers/${s.id}_wafer`)
    })

    // failed prints and handled wafers, one of each per substrate
    table.substrates.forEach(s => {
        allthemods.create(`broken_${s.id}_wafer`)
            .displayName(`Broken ${s.name}`)
            .texture(`kubejs:item/wafers/broken_${s.id}_wafer`)
            .tooltip('A print that failed in the vacuum. Macerate it to reclaim the material.')
        allthemods.create(`contaminated_${s.id}_wafer`)
            .displayName(`Contaminated ${s.name}`)
            .texture(`kubejs:item/wafers/contaminated_${s.id}_wafer`)
            .tooltip('Touched by bare hands. Strip and clean it (SMC wet processing) to get a blank wafer back.')
    })

    // chips handled without gloves outside a clean room
    table.chips.forEach(([id, name]) => {
        allthemods.create(`contaminated_${id}`)
            .displayName(`Contaminated ${name}`)
            .texture(`kubejs:item/chips/contaminated_${id}`)
            .tooltip('Touched by bare hands. Rinse it (SMC wet processing) to get the chip back.')
    })
})
