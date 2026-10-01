// AF9 - AF9's own chips, next to GT's: chip wafers, chips, contaminated chips, reticles, the mask blanks of the finer
// chips and the eDRAM packages. Spec: docs/semiconductor-factory.md
//
// They are printed like GT's chips (server_scripts/mods/gtceu/photolithography.js, AF9_WAFERS.chips): a substrate
// prints every chip whose own substrate is the same or lower, the Cutter dices the chip wafer into dies.
//   chip             substrate                 reticle (lens / mask blank; the mask classes: reticles.js)
//   RF Transceiver   Silicon (350 nm)          lime / chrome
//   APU              Silicon (350 nm)          magenta / chrome
//   MCU              Silicon (350 nm)          white (glass lens) / chrome
//   ASIC             Phosphorus (200 nm)       light gray / chrome
//   eDRAM            Trinium (80 nm)           green / MoSi phase-shift
//   MRAM             Trinium (80 nm)           blue / MoSi phase-shift
//   FeRAM            Trinium (80 nm)           yellow / MoSi phase-shift
//   VPU              Naquadria (65 nm)         purple / MoSi phase-shift
//   TPU              Transmuted Neutronium     orange / EUV multilayer
// The families the finer substrates open up (spec: the new circuit families):
//   SAW Filter       Naquadah (100 nm)         red / MoSi phase-shift      acoustic wave: RF filters
//   Photonic IC      Trinium (80 nm)           cyan / MoSi phase-shift     silicon photonics
//   Spin Logic       Naquadria (65 nm)         lime / MoSi phase-shift     spintronics: MTJ logic
//   TMD Logic        Neutronium (50 nm)        pink / EUV multilayer       2D-material transistors
//   Memristor        Transmuted Neutronium     cyan / EUV multilayer       neuromorphic: ReRAM / PCM crossbars
//   Quantum-Dot IC   Strange Matter (7 nm)     yellow / EUV multilayer     single-electron and quantum-dot logic
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
    ['tpu', 'TPU', 'Tensor Processing Unit: an AI accelerator', 'Raw Tensor Processor'],
    ['saw_filter', 'SAW Filter', 'Surface acoustic wave filter: a radio front end on a piezo film', 'Raw Acoustic Wave Filter'],
    ['photonic_ic', 'Photonic IC', 'Silicon photonics: waveguides, resonators and modulators on a chip',
        'Raw Photonic Circuit'],
    ['spin_logic', 'Spin Logic', 'Magnetic tunnel junction logic: computes with electron spin', 'Raw Spintronic Logic'],
    ['tmd_logic', 'TMD Logic', 'Logic from atomically thin 2D semiconductors (WSe2, MoS2)', 'Raw 2D-Material Logic'],
    ['memristor', 'Memristor', 'ReRAM and phase-change crossbars: memory that computes like a neuron',
        'Raw Memristor Array'],
    ['quantum_dot_ic', 'Quantum-Dot IC', 'Single-electron transistors and quantum dots', 'Raw Quantum-Dot Circuit']
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
    })

    // The masks of the finer chips: an attenuated phase-shift blank (a MoSi film that shifts the light half a wave:
    // sharper edges at 80 and 65 nm) and an EUV blank (Mo/Si bilayers that reflect 13.5 nm light; EUV masks mirror)
    event.create('phase_shift_mask_blank')
        .displayName('MoSi Phase-Shift Mask Blank')
        .tooltip('Photomask blank for the 100 to 65 nm chips (eDRAM, MRAM, FeRAM, VPU, SAW Filter, Photonic IC,')
        .tooltip('Spin Logic).')
    event.create('euv_mask_blank')
        .displayName('EUV Multilayer Mask Blank')
        .tooltip('Reflective photomask blank for the 50 nm and finer chips (TPU, TMD Logic, Memristor, Quantum-Dot IC).')

    // eDRAM next to the processor on one package: the cache chiplet
    event.create('edram_cpu_package')
        .displayName('eDRAM CPU Package')
        .tooltip('A CPU die with its eDRAM cache on one package.')
    event.create('edram_soc_package')
        .displayName('eDRAM SoC Package')
        .tooltip('An SoC with its eDRAM cache on one package.')
})
