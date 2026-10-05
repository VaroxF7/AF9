// AF9 - Sanguinite, the bright-red UHV superconductor, smelted in the Sanguinite Hearth Furnace
// (gtceu:sanguinite_hearth_furnace, a standalone Rotary-Hearth copy running only gtceu:sanguinite_hearth: the EBF
// cannot smelt sanguinite at all). Materials: AF9Materials.uhvSuperconductor. Spec: docs/uhv-superconductor.md
//
// The EBF takes a single fluid, so the chain blends first and smelts after:
//   1. Large Chemical Reactor: 4 neutronium + 10 tritanium dusts, hydrogen, Ares gas (Martian asteroid field,
//      vein_oil.js) and LXA-1 (Space Elevator far-dark mission, PlanetCatalog) -> 14 crude sanguinite dust.
//   2. Sanguinite Hearth Furnace: 14 crude + circuit 10 + supercooled endion -> 14 hot sanguinite ingots,
//      13000 K (Resonant Endion Coils at UHV), 4A UV, 60 s. The hearth preheats first (HEARTH_GATE).
//   3. Bulk Blast Chiller: GT's own vacuum-freezer cooling (from the blast property) takes the hot ingots to
//      sanguinite ingots; GT wires them into lossless UV 4A cables on its own.

ServerEvents.recipes(event => {
    const VA = GTValues.VA

    // ---- 1. The blend: dusts and all three gases in the Large Chemical Reactor (3 items, 5 fluids) ----
    event.recipes.gtceu.large_chemical_reactor('af9:crude_sanguinite_mix')
        .itemInputs('4x gtceu:neutronium_dust', '10x gtceu:tritanium_dust')
        .inputFluids(Fluid.of('gtceu:hydrogen', 10000), Fluid.of('gtceu:ares_gas', 2000),
            Fluid.of('gtceu:lxa_1', 4000))
        .itemOutputs('14x gtceu:crude_sanguinite_dust')
        .duration(600)
        .EUt(VA[GTValues.UV])

    // ---- 2. The smelt: crude blend under supercooled endion in the Sanguinite Hearth Furnace ----
    // 4A UV (2,097,152 EU/t), a full minute, circuit 10. The 50 buckets of supercooled endion are the
    // quench that freezes the lossless phase in; the Bulk Blast Chiller finishes the ingots. Hearth-only:
    // neither the EBF nor the Rotary Hearth runs this type, and the material's auto EBF recipe is removed below.
    event.recipes.gtceu.sanguinite_hearth('af9:sanguinite_hot_ingot')
        .itemInputs('14x gtceu:crude_sanguinite_dust')
        .circuit(10)
        .inputFluids(Fluid.of('gtceu:supercooled_endion', 50000))
        .itemOutputs('14x gtceu:hot_sanguinite_ingot')
        .blastFurnaceTemp(13000)
        .duration(1200)
        .EUt(VA[GTValues.UV], 4)

    // The blast property's auto EBF print (sanguinite dust -> hot ingot) would let the EBF smelt it: gone
    // (by id and, belt-and-braces in case GT names it after its output, by type+output).
    // The auto vacuum-freezer cooling (hot ingot -> ingot) stays: the Bulk Blast Chiller runs it.
    event.remove({ id: 'gtceu:electric_blast_furnace/sanguinite' })
    event.remove({ type: 'gtceu:electric_blast_furnace', output: 'gtceu:hot_sanguinite_ingot' })

    // ---- Machines ----
    // The hearth controller: a Rotary Hearth controller refitted with a UHV hull, field generators and an
    // endion quench loop. Assembled at UHV.
    event.recipes.gtceu.assembler('af9:sanguinite_hearth_furnace')
        .itemInputs('gtceu:mega_blast_furnace', 'gtceu:uhv_machine_hull', '4x gtceu:uhv_field_generator',
            '4x #gtceu:circuits/uhv', '4x gtceu:uhv_sensor', '4x gtceu:uhv_electric_pump',
            '8x gtceu:naquadah_alloy_plate')
        .inputFluids(Fluid.of('gtceu:supercooled_endion', 4000))
        .itemOutputs('gtceu:sanguinite_hearth_furnace')
        .duration(2400)
        .EUt(VA[GTValues.UHV])
})
