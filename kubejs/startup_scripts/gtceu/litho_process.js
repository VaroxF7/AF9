// AF9 - The lithography process around the print: calibration, wafer clean-up, the finer coatings, the Metrology
// Station. Items and materials here; behaviour: AF9 Core (LithoMachine); recipes: server_scripts/mods/gtceu/
// litho_process.js. Spec: docs/semiconductor-factory.md §18

StartupEvents.registry('item', event => {
    // The reference wafer of a calibration run: a Line, Scanner or Orbital Station that has drifted takes one from an
    // input bus (or a Metrology Station for the machines on its bus) and aligns its optics and stages on its marks.
    event.create('calibration_wafer')
        .displayName('Calibration Wafer')
        .texture('kubejs:item/wafers/calibration_wafer')
        .tooltip('A reference wafer with alignment marks. Put it in the input bus of a lithography machine that has')
        .tooltip('drifted (below 70% calibration) and it calibrates itself, or feed a Metrology Station.')
})
