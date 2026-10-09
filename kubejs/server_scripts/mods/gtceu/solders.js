// AF9 - Soldering alloys: the pack's own solders beyond GT's tin and soldering alloy.
// Materials: af9-core, registry/AF9Materials (solders()). Uses: circuits_af9.js (HV-UV + wetware),
// plasma_soldering (UHV atomic soldering in the Hyper-Intensity Laser Engraver). Spec: docs/solders.md
//
//   high_grade_solder  HV-UV circuits, replaces soldering alloy (tin stays as budget option).
//                      Mixer at MV (one tier below its first users) + GT's EBF from the blast property.
//   living_solder      UV wetware + neuron circuits, grown sterile (bio-chain, no EBF shortcut).
//   plasma_solder      UHV+ atomic soldering, condensed from QGP + quantanium in quark synthesis.
//
// The high-grade mixer lives in circuits_af9.js beside the other circuit-alloy mixers; the EBF recipe is GT's own
// (blast property). This file holds the bio-chain and the plasma condensation.

ServerEvents.recipes(event => {
    const VA = GTValues.VA
    // UHV recipes draw 100 A of UHV power (XPS 300 A, NVM 1,000 A: circuits_af9.js), see docs/dyson-swarm.md
    const UHV_AMPS = 100

    // ---- Living solder: sterile incubation (chemical reactor, UV, sterile cleanroom) ----
    // Stem cells in sterilized growth medium + mutagen, scaffolded on silver nanowires: a conductive bio-hydrogel
    // that solders wetware without killing the cells (reflow would). Sterile: a contaminated culture is dead solder.
    // chemical_reactor slots: 2 items, 3 fluids in — 2 items + 2 fluids in, 1 fluid out fits.
    event.recipes.gtceu.chemical_reactor('af9:living_solder_incubation')
        .itemInputs('gtceu:stem_cells', '4x gtceu:fine_silver_wire')
        .inputFluids(Fluid.of('gtceu:sterilized_growth_medium', 1000), Fluid.of('gtceu:mutagen', 500))
        .outputFluids(Fluid.of('gtceu:living_solder', 2000))
        .cleanroom(CleanroomType.STERILE_CLEANROOM)
        .duration(600)
        .EUt(VA[GTValues.UV])

    // ---- Plasma solder: quark-stabilised metallic plasma (quark synthesis, UHV) ----
    // A QGP trap's quark-gluon plasma quenched with quantanium dust: the plasma freezes into a metastable
    // metallic plasma that deposits ion-by-ion (no reflow, atomic-level joints). The traps come back empty like the
    // strange matter / chromodynium recipes. quark_synthesis slots: 2 items in, 2 out, 1 fluid (coolant) in.
    event.recipes.gtceu.quark_synthesis('af9:plasma_solder_dust')
        .itemInputs('8x af9:qgp_trap', 'gtceu:quantanium_dust')
        .inputFluids('#af9:coolant/endion 4000')
        .itemOutputs('gtceu:plasma_solder_dust', '8x af9:magnetic_trap')
        .duration(2400)
        .EUt(VA[GTValues.UHV], UHV_AMPS)

    // ---- Plasma atomic soldering: the UHV wetware mainframe in the Hyper-Intensity Laser Engraver ----
    // The photonic bill (10 UV supercomputers + photonic/spin dies), deposited ion-by-ion under the engraver's laser.
    // plasma_soldering slots: 9 items, 1 out, 2 fluids in.
    // it is the only path to the mainframe — both assembly line versions are gone (circuits_af9.js).
    event.recipes.gtceu.plasma_soldering('af9:wetware_mainframe_uhv_plasma')
        .itemInputs(
            '2x gtceu:tritanium_frame',
            '10x gtceu:wetware_processor_computer',
            '64x af9:photonic_ic_chip',
            '16x af9:photonic_ic_chip',
            '32x af9:spin_logic_chip',
            '64x gtceu:enriched_naquadah_trinium_europium_duranide_double_wire',
            '128x gtceu:polybenzimidazole_foil',
            '8x gtceu:europium_plate')
        .inputFluids(
            Fluid.of('gtceu:plasma_solder', 720),
            Fluid.of('gtceu:polybenzimidazole', 1152))
        .itemOutputs('gtceu:wetware_processor_mainframe')
        .duration(1000)
        .EUt(VA[GTValues.UHV], UHV_AMPS)
})
