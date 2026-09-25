// AF9 - Wafers: the nine substrates and every printed wafer. Spec: docs/semiconductor-factory.md
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
// MPIC, SoC, naquadah for ASoC, neutronium for HASoC) is the same or lower. On its own substrate a chip is GT's wafer
// item; on a higher one it is kubejs:<substrate>_<chip>_wafer: the substrate's wafer with the chip's pattern on it.
// The derived wafers (Nano CPU, Qubit CPU, HPIC, UHPIC) follow the substrate of the wafer they are made from.
//
// Every substrate also has a broken wafer (a failed print) and a contaminated wafer (handled without gloves outside a
// clean room, see af9-core WaferContamination).

const AF9_WAFER_TABLE = (() => {
    // id, blank wafer, display prefix of its printed wafers, blank wafer name (GT's names for GT's four)
    const substrates = [
        { id: 'silicon', blank: 'gtceu:silicon_wafer', prefix: 'Silicon', name: 'Silicon Wafer' },
        { id: 'phosphorus', blank: 'gtceu:phosphorus_wafer', prefix: 'Phosphorus-doped', name: 'Phosphorus-doped Wafer' },
        { id: 'naquadah', blank: 'gtceu:naquadah_wafer', prefix: 'Naquadah-doped', name: 'Naquadah-doped Wafer' },
        { id: 'trinium', blank: 'kubejs:trinium_wafer', prefix: 'Trinium-doped', name: 'Trinium-doped Wafer' },
        { id: 'naquadria', blank: 'kubejs:naquadria_wafer', prefix: 'Naquadria-doped', name: 'Naquadria-doped Wafer' },
        { id: 'neutronium', blank: 'gtceu:neutronium_wafer', prefix: 'Neutronium-doped', name: 'Neutronium-doped Wafer' },
        { id: 'transmuted_neutronium', blank: 'kubejs:transmuted_neutronium_wafer', prefix: 'Transmuted Neutronium',
            name: 'Transmuted Neutronium Wafer' },
        { id: 'strange_matter', blank: 'kubejs:strange_matter_wafer', prefix: 'Strange Matter-doped',
            name: 'Strange Matter-doped Wafer' },
        { id: 'chromodynium', blank: 'kubejs:chromodynium_wafer', prefix: 'Chromodynium', name: 'Chromodynium Wafer' }
    ]
    // chip = GT's wafer id without _wafer; native = index of the chip's own substrate; from = derived from
    const chips = [
        { id: 'ilc', name: 'ILC', native: 0 },
        { id: 'ram', name: 'RAM', native: 0 },
        { id: 'cpu', name: 'CPU', native: 0 },
        { id: 'ulpic', name: 'ULPIC', native: 0 },
        { id: 'lpic', name: 'LPIC', native: 0 },
        { id: 'simple_soc', name: 'Simple SoC', native: 0 },
        { id: 'nand_memory', name: 'NAND Memory', native: 1 },
        { id: 'nor_memory', name: 'NOR Memory', native: 1 },
        { id: 'mpic', name: 'MPIC', native: 1 },
        { id: 'soc', name: 'SoC', native: 1 },
        { id: 'advanced_soc', name: 'ASoC', native: 2 },
        { id: 'highly_advanced_soc', name: 'HASoC', native: 5 },
        { id: 'nano_cpu', name: 'Nano CPU', native: 0, from: 'cpu' },
        { id: 'qbit_cpu', name: 'Qubit CPU', native: 0, from: 'nano_cpu' },
        { id: 'hpic', name: 'HPIC', native: 1, from: 'mpic' },
        { id: 'uhpic', name: 'UHPIC', native: 1, from: 'hpic' }
    ]
    // item id of a chip printed on a substrate (index), or null if the substrate cannot carry it
    const printed = (substrateIndex, chip) => {
        if (substrateIndex < chip.native) return null
        if (substrateIndex === chip.native) return `gtceu:${chip.id}_wafer`
        return `kubejs:${substrates[substrateIndex].id}_${chip.id}_wafer`
    }
    return { substrates: substrates, chips: chips, printed: printed }
})()

StartupEvents.registry('item', allthemods => {
    const table = AF9_WAFER_TABLE
    // new blank wafers (GT has silicon, phosphorus, naquadah and neutronium)
    table.substrates.filter(s => s.blank.startsWith('kubejs:')).forEach(s => {
        allthemods.create(s.blank.substring('kubejs:'.length))
            .displayName(s.name)
            .texture(`kubejs:item/wafers/${s.id}_wafer`)
    })

    // printed wafers on a higher substrate than the chip's own
    table.substrates.forEach((s, index) => {
        table.chips.forEach(chip => {
            const id = table.printed(index, chip)
            if (!id || !id.startsWith('kubejs:')) return
            allthemods.create(id.substring('kubejs:'.length))
                .displayName(`${s.prefix} ${chip.name} Wafer`)
                .texture(`kubejs:item/wafers/${s.id}_${chip.id}_wafer`)
        })
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
})
