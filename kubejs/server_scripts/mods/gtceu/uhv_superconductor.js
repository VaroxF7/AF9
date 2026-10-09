// AF9 - Sanguinite, the bright-red UHV superconductor, smelted in the Sanguinite Hearth Furnace
// (gtceu:sanguinite_hearth_furnace, a standalone Rotary-Hearth copy running only gtceu:sanguinite_hearth: the EBF
// cannot smelt sanguinite at all). Materials: AF9Materials.uhvSuperconductor. Spec: docs/uhv-superconductor.md
//
// The chain blends first and smelts after:
//   1. Large Chemical Reactor: 4 neutronium + 10 tritanium dusts, hydrogen, Ares gas (Martian asteroid field,
//      vein_oil.js) and LXA-1 (Space Elevator far-dark mission, PlanetCatalog) -> 14 crude sanguinite dust.
//   2. Sanguinite Hearth Furnace: tin alloy + barium + europium + titanium + electrum + crude dusts, circuit 8,
//      coolant (supercooled, Coolant Hatch, fluid_in_0) -> 1000 mB molten sanguinite at 10800 K (Tritanium coils,
//      ZPM, 60 s). Helium version adds 1000 mB helium (fluid_in_1) and runs twice as fast. The hearth preheats
//      first (HEARTH_GATE, 300 s at LuV, 4A LuV readiness, vents to 0 K).
//   3. Vacuum Freezer: 144 mB molten + 1000 mB supercooled hydrogen (+ ingot mold, not consumed) -> sanguinite
//      ingot; GT wires it into lossless UV 4A cables on its own.

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

    // No shortcuts past the hearth (removes first, so the prints below survive them):
    // - auto EBF dust -> hot ingot (old blast property) would let the EBF smelt it: gone.
    // - the plain molten + mold solidifier would skip the supercooled casting: gone, only the freezer runs.
    // - old hot-ingot prints and GT auto vacuum cooling are gone with the hot ingot itself.
    event.remove({ id: 'gtceu:electric_blast_furnace/sanguinite' })
    event.remove({ type: 'gtceu:electric_blast_furnace', output: 'gtceu:hot_sanguinite_ingot' })
    event.remove({ type: 'gtceu:electric_blast_furnace', output: 'gtceu:sanguinite_ingot' })
    event.remove({ type: 'gtceu:fluid_solidifier', output: 'gtceu:sanguinite_ingot' })
    event.remove({ id: 'gtceu:fluid_solidifier/solidify_sanguinite_to_ingot' })
    event.remove({ id: 'gtceu:vacuum_freezer/cool_hot_sanguinite_ingot' })

    // ---- 2. The smelt: dusts to molten sanguinite in the Sanguinite Hearth Furnace ----
    // ZPM, 60 s, circuit 8, Tritanium coils (10800 K). Coolant first (fluid_in_0, Coolant Hatch only):
    // supercooled hydrogen or any colder grade; helium version is the same plus helium, twice as fast.
    const hearthDusts = ['64x gtceu:tin_alloy_dust', '64x gtceu:tin_alloy_dust', '4x gtceu:barium_dust',
        '8x gtceu:europium_dust', '32x gtceu:titanium_dust', '16x gtceu:electrum_dust',
        '4x gtceu:crude_sanguinite_dust']
    event.recipes.gtceu.sanguinite_hearth('af9:molten_sanguinite')
        .itemInputs(hearthDusts)
        .circuit(8)
        .inputFluids('#af9:coolant/hydrogen 1000')
        .outputFluids(Fluid.of('gtceu:sanguinite', 1000))
        .blastFurnaceTemp(10800)
        .duration(1200)
        .EUt(VA[GTValues.ZPM])
    event.recipes.gtceu.sanguinite_hearth('af9:molten_sanguinite_helium')
        .itemInputs(hearthDusts)
        .circuit(8)
        .inputFluids('#af9:coolant/hydrogen 1000', Fluid.of('gtceu:helium', 1000))
        .outputFluids(Fluid.of('gtceu:sanguinite', 1000))
        .blastFurnaceTemp(10800)
        .duration(600)
        .EUt(VA[GTValues.ZPM])

    // ---- 3. The casting: molten to ingot under supercooled hydrogen in the Vacuum Freezer ----
    // 144 mB molten (one ingot) + 1000 mB supercooled hydrogen, ingot mold kept, MV, 19.5 s.
    event.recipes.gtceu.vacuum_freezer('af9:sanguinite_ingot_cooling')
        .notConsumable('gtceu:ingot_casting_mold')
        .inputFluids(Fluid.of('gtceu:sanguinite', 144), Fluid.of('gtceu:supercooled_hydrogen', 1000))
        .itemOutputs('gtceu:sanguinite_ingot')
        .duration(390)
        .EUt(VA[GTValues.MV])

    // ---- Machines ----
    // The hearth controller: a Rotary Hearth controller refitted with a UHV hull, field generators and a
    // coolant loop. Assembled at UHV.
    // (UV parts: GT has no parts above UV while its high-tier content is off, as it is in the pack)
    event.recipes.gtceu.assembler('af9:sanguinite_hearth_furnace')
        .itemInputs('gtceu:mega_blast_furnace', 'gtceu:uhv_machine_hull', '4x gtceu:uv_field_generator',
            '4x #gtceu:circuits/uhv', '4x gtceu:uv_sensor', '4x gtceu:uv_electric_pump',
            '8x gtceu:naquadah_alloy_plate')
        .inputFluids(Fluid.of('gtceu:supercooled_hydrogen', 4000))
        .itemOutputs('gtceu:sanguinite_hearth_furnace')
        .duration(2400)
        .EUt(VA[GTValues.UHV])
})
