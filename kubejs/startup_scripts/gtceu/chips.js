// AF9 - AF9's own chips, next to GT's: chip wafers, chips, contaminated chips, reticles, the mask blanks of the finer
// chips and the eDRAM packages. Spec: docs/semiconductor-factory.md
//
// They are printed like GT's chips (server_scripts/mods/gtceu/photolithography.js, AF9_WAFERS.chips): a substrate
// prints every chip whose own substrate is the same or lower, the Cutter dices the chip wafer into dies.
//   chip             substrate                 reticle (lens / mask blank)
//   RF Transceiver   Silicon (350 nm)          lime / chrome
//   APU              Silicon (350 nm)          magenta / chrome
//   MCU              Silicon (350 nm)          white (glass lens) / chrome
//   ASIC             Phosphorus (200 nm)       light gray / chrome
//   eDRAM            Trinium (80 nm)           green / MoSi phase-shift
//   MRAM             Trinium (80 nm)           blue / MoSi phase-shift
//   FeRAM            Trinium (80 nm)           yellow / MoSi phase-shift
//   VPU              Naquadria (65 nm)         purple / MoSi phase-shift
//   TPU              Transmuted Neutronium     orange / EUV multilayer
// Textures: kubejs/assets/kubejs/textures/item (chips/, wafers/, <chip>_reticle, the blanks, the packages).

const AF9_CHIP_TABLE = [
    // id, name, chip tooltip, wafer tooltip
    ['rf_transceiver', 'RF Transceiver', 'Radio Frequency Transceiver', 'Raw Radio Circuit'],
    ['apu', 'APU', 'Accelerated Processing Unit: CPU and GPU on one die', 'Raw Accelerated Processing Unit'],
    ['mcu', 'MCU', 'Microcontroller: a small CPU with its own memory', 'Raw Microcontroller'],
    ['asic', 'ASIC', 'Application-Specific Integrated Circuit', 'Raw Application-Specific Circuit'],
    ['edram', 'eDRAM', 'Embedded DRAM: the cache of CPUs and SoCs', 'Raw Embedded Memory'],
    ['mram', 'MRAM', 'Magnetoresistive RAM: keeps its data without power', 'Raw Magnetic Memory'],
    ['feram', 'FeRAM', 'Ferroelectric RAM: fast and frugal, keeps its data', 'Raw Ferroelectric Memory'],
    ['vpu', 'VPU', 'Video Processing Unit', 'Raw Video Processor'],
    ['tpu', 'TPU', 'Tensor Processing Unit: an AI accelerator', 'Raw Tensor Processor']
]

StartupEvents.registry('item', event => {
    AF9_CHIP_TABLE.forEach(([id, name, chipTip, waferTip]) => {
        event.create(`${id}_wafer`)
            .displayName(`${name} Wafer`)
            .texture(`kubejs:item/wafers/${id}_wafer`)
            .tooltip(waferTip)
        event.create(`${id}_chip`)
            .displayName(`${name} Chip`)
            .texture(`kubejs:item/chips/${id}_chip`)
            .tooltip(chipTip)
        // handled without gloves outside a clean room (af9-core WaferContamination)
        event.create(`contaminated_${id}_chip`)
            .displayName(`Contaminated ${name} Chip`)
            .texture(`kubejs:item/chips/contaminated_${id}_chip`)
            .tooltip('Touched by bare hands. Rinse it (SMC wet processing) to get the chip back.')
        event.create(`${id}_reticle`)
            .displayName(`${name} Reticle`)
            .maxStackSize(1)
            .tooltip('Photomask for the lithography machines. Not consumed.')
    })

    // The masks of the finer chips: an attenuated phase-shift blank (a MoSi film that shifts the light half a wave:
    // sharper edges at 80 and 65 nm) and an EUV blank (Mo/Si bilayers that reflect 13.5 nm light; EUV masks mirror)
    event.create('phase_shift_mask_blank')
        .displayName('MoSi Phase-Shift Mask Blank')
        .tooltip('Photomask blank for the 80 and 65 nm chips (eDRAM, MRAM, FeRAM, VPU).')
    event.create('euv_mask_blank')
        .displayName('EUV Multilayer Mask Blank')
        .tooltip('Reflective photomask blank for the EUV chips (TPU).')

    // eDRAM next to the processor on one package: the cache chiplet
    event.create('edram_cpu_package')
        .displayName('eDRAM CPU Package')
        .tooltip('A CPU die with its eDRAM cache on one package.')
    event.create('edram_soc_package')
        .displayName('eDRAM SoC Package')
        .tooltip('An SoC with its eDRAM cache on one package.')
})
