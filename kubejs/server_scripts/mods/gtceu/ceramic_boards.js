// AF9 - Ceramic Circuit Boards (TerraFirmaGreg's idea, redrawn): GT's plastic circuit board is a fired ceramic tile here, made
// from MV to EV, each tier from a new ceramic and a new foil. The board is still gtceu:plastic_circuit_board (every circuit
// that took it still takes it; the name and the texture change: kubejs/assets/gtceu), the printed one stays
// gtceu:plastic_printed_circuit_board. GT's four polymer recipes for the blank board are gone.
// Materials: af9-core, registry/AF9Materials (ceramicBoards()), items: AF9Items. Spec: docs/ceramic-boards.md
//
//   tier  ceramic (made at)                           substrate (EBF)                          boards (chemical reactor)
//   MV    silicon carbide: silicon + carbon, 1800 K   SiC plate + 4 molybdenum foil, 1800 K     4 boards, 20 s
//   HV    alumina: aluminium + oxygen                 alumina plate + 4 cupronickel foil, 2700 K  8 boards, 6 s
//   EV    beryllia: beryllium + oxygen                beryllia plate + 2 chromium, 3600 K       16 boards, 2.5 s
// Every board of a higher tier costs less (fewer foils and acid, less energy) and takes less time, but the batch asks for
// the tier's machine, its coils and its rarer ceramic. Energy per board: 12,800 EU (MV), 7,680 (HV), 6,400 (EV).

ServerEvents.recipes(event => {
    const VA = GTValues.VA
    const gt = event.recipes.gtceu

    // ---- GT's blank plastic boards: gone (and their large reactor copies) ----
    const polymers = ['polyethylene', 'pvc', 'ptfe', 'pbi']
    polymers.forEach(polymer => {
        event.remove({ id: `gtceu:chemical_reactor/plastic_board_${polymer}` })
        event.remove({ id: `gtceu:large_chemical_reactor/plastic_board_${polymer}` })
    })

    // ---- The ceramics ----
    // MV: silicon carbide, silicon and carbon in the EBF (Kanthal coils)
    gt.electric_blast_furnace('af9:ceramic_boards/silicon_carbide')
        .itemInputs('gtceu:silicon_dust', 'gtceu:carbon_dust')
        .itemOutputs('2x gtceu:silicon_carbide_dust')
        .blastFurnaceTemp(1800)
        .duration(400)
        .EUt(VA[GTValues.MV])
    // HV: alumina, aluminium burnt in oxygen: 2 Al + 1.5 O2 -> Al2O3
    gt.chemical_reactor('af9:ceramic_boards/alumina')
        .itemInputs('2x gtceu:aluminium_dust')
        .inputFluids(Fluid.of('gtceu:oxygen', 3000))
        .itemOutputs('5x gtceu:alumina_dust')
        .duration(100)
        .EUt(VA[GTValues.HV])
    // EV: beryllia, beryllium burnt in oxygen (the beryllium is the rare part: beryl, the Void Miner)
    gt.chemical_reactor('af9:ceramic_boards/beryllia')
        .itemInputs('gtceu:beryllium_dust')
        .inputFluids(Fluid.of('gtceu:oxygen', 1000))
        .itemOutputs('2x gtceu:beryllia_dust')
        .duration(100)
        .EUt(VA[GTValues.EV])
    // the plates, pressed from the dust
    const plates = ['silicon_carbide', 'alumina', 'beryllia']
    plates.forEach(ceramic => {
        gt.compressor(`af9:ceramic_boards/${ceramic}_plate`)
            .itemInputs(`2x gtceu:${ceramic}_dust`)
            .itemOutputs(`gtceu:${ceramic}_plate`)
            .duration(200)
            .EUt(VA[GTValues.LV])
    })

    // ---- The substrates: the plate fired onto its foil, hotter coils at every tier ----
    gt.electric_blast_furnace('af9:ceramic_boards/silicon_carbide_substrate')
        .itemInputs('gtceu:silicon_carbide_plate', '4x gtceu:molybdenum_foil')
        .itemOutputs('af9:silicon_carbide_substrate')
        .blastFurnaceTemp(1800)
        .duration(200)
        .EUt(VA[GTValues.MV])
    gt.electric_blast_furnace('af9:ceramic_boards/alumina_substrate')
        .itemInputs('gtceu:alumina_plate', '4x gtceu:cupronickel_foil')
        .itemOutputs('af9:alumina_substrate')
        .blastFurnaceTemp(2700)
        .duration(160)
        .EUt(VA[GTValues.HV])
    gt.electric_blast_furnace('af9:ceramic_boards/beryllia_substrate')
        .itemInputs('gtceu:beryllia_plate', '2x gtceu:chromium_dust')
        .itemOutputs('af9:beryllia_substrate')
        .blastFurnaceTemp(3600)
        .duration(120)
        .EUt(VA[GTValues.EV])

    // ---- The boards: the substrate cut and etched in sulfuric acid with a pinch of nickel, more boards per batch at every tier ----
    // substrate, boards, ticks, tier
    const boards = [['silicon_carbide', 4, 400, GTValues.MV], ['alumina', 8, 120, GTValues.HV], ['beryllia', 16, 50, GTValues.EV]]
    boards.forEach(([ceramic, count, ticks, tier]) => {
        gt.chemical_reactor(`af9:ceramic_boards/${ceramic}_board`)
            .itemInputs(`af9:${ceramic}_substrate`, 'gtceu:nickel_dust')
            .inputFluids(Fluid.of('gtceu:sulfuric_acid', 250))
            .itemOutputs(`${count}x gtceu:plastic_circuit_board`)
            .duration(ticks)
            .EUt(VA[tier])
    })
})
