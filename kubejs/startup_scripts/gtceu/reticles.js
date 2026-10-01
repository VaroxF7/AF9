// AF9 - The reticles (photomasks) of the printed chips. Recipes: server_scripts/mods/gtceu/photolithography.js
// (AF9_WAFERS.reticles); spec: docs/semiconductor-factory.md §3.
//
// The mask has to fit the light, so every print takes a reticle of its node's class:
//   chrome  chrome-on-quartz binary mask   350 and 200 nm
//   psm     MoSi attenuated phase-shift    100, 80 and 65 nm
//   euv     reflective Mo/Si multilayer    50, 20, 7 and 1 nm (the orbital station)
// A chip has a reticle of its own (native) class, `<chip>_reticle`, written through the chip's lens, and one of every
// finer class, `<chip>_psm_reticle` and `<chip>_euv_reticle`, written onto that class's blank from the native one.
// The native class is the one of the chip's own substrate (index below); keep it in sync with maskClass() in
// AF9_WAFERS (server script).
// Textures: kubejs/assets/kubejs/textures/item/<reticle>.png (tools/textures/reticles.py draws them).

const AF9_RETICLE_TABLE = [
    // reticle id, name, index of the chip's own substrate (silicon 0 ... chromodynium 8)
    ['ilc', 'ILC', 0],
    ['ram', 'RAM', 0],
    ['cpu', 'CPU', 0],
    ['ulpic', 'ULPIC', 0],
    ['lpic', 'LPIC', 0],
    ['simple_soc', 'Simple SoC', 0],
    ['rf_transceiver', 'RF Transceiver', 0],
    ['apu', 'APU', 0],
    ['mcu', 'MCU', 0],
    ['nand', 'NAND', 1],
    ['nor', 'NOR', 1],
    ['mpic', 'PIC', 1],
    ['soc', 'SoC', 1],
    ['asic', 'ASIC', 1],
    ['advanced_soc', 'ASoC', 2],
    ['saw_filter', 'SAW Filter', 2],
    ['edram', 'eDRAM', 3],
    ['mram', 'MRAM', 3],
    ['feram', 'FeRAM', 3],
    ['photonic_ic', 'Photonic IC', 3],
    ['vpu', 'VPU', 4],
    ['spin_logic', 'Spin Logic', 4],
    ['highly_advanced_soc', 'HASoC', 5],
    ['tmd_logic', 'TMD Logic', 5],
    ['tpu', 'TPU', 6],
    ['memristor', 'Memristor', 6],
    ['quantum_dot_ic', 'Quantum-Dot IC', 7]
]

StartupEvents.registry('item', event => {
    const classes = [
        { id: 'chrome', suffix: '', label: '', tip: 'Chrome-on-quartz mask: prints at 350 and 200 nm.' },
        { id: 'psm', suffix: '_psm', label: 'Phase-Shift ', tip: 'Phase-shift mask: prints at 100, 80 and 65 nm.' },
        { id: 'euv', suffix: '_euv', label: 'EUV ', tip: 'Reflective EUV mask: prints at 50 nm and finer (orbital station).' }
    ]
    const classOf = native => native <= 1 ? 0 : native <= 4 ? 1 : 2
    AF9_RETICLE_TABLE.forEach(([id, name, native]) => {
        for (let k = classOf(native); k < classes.length; k++) {
            const cls = classes[k]
            // the native class keeps the plain `<chip>_reticle` id
            const itemId = k === classOf(native) ? `${id}_reticle` : `${id}${cls.suffix}_reticle`
            event.create(itemId)
                .displayName(`${name} ${cls.label}Reticle`)
                .maxStackSize(1)
                .tooltip('Photomask for the lithography machines. Not consumed.')
                .tooltip(cls.tip)
        }
    })
})
