// AF9 - how transistors work, as an information page of the recipe viewers (on the lithography machines and the silicon chip wafers).
// The counts and prices must match AF9_WAFERS (server_scripts/mods/gtceu/photolithography.js).
JEIEvents.information(event => {
    const text = [
        'Transistors are no item in AF9, they are a virtual price. A silicon chip wafer carries a fixed number of transistors, and printing it uses up the fluids that many transistors cost, through the auxiliary fluid hatches of the lithography machine.',
        'Every 100 transistors cost 12 mB Polyethylene (the insulator), 1 mB Silicon and 2 mB Electrum (the metal). The ready-made numbers are in the recipe of every print.',
        'Transistors per wafer: CPU 12000, APU 12000, MCU 8200, RAM 8000, Simple SoC 6700, LPIC 5000, ILC 4500, ULPIC 3540, RF Transceiver 1200.',
        'A transistor gets dearer with every node, as in a real fab: the price rises 20% per node, from 1x at 350 nm to about 3.6x at 7 nm and 4.3x at 1 nm. And a finer node prints more wafers at once, which are all billed.',
        'Example: a CPU wafer at 350 nm is 12000 transistors = 1440 mB Polyethylene, 120 mB Silicon, 240 mB Electrum.',
        'Only the silicon chips cost transistors for now.'
    ]
    ;['gtceu:photolithography_line', 'gtceu:photolithography_scanner', 'gtceu:orbital_lithography_station',
        'gtceu:cpu_wafer', 'gtceu:ram_wafer', 'gtceu:ilc_wafer', 'gtceu:simple_soc_wafer', 'gtceu:ulpic_wafer', 'gtceu:lpic_wafer',
        'af9:rf_transceiver_wafer', 'af9:apu_wafer', 'af9:mcu_wafer'].forEach(id => event.addItem(id, text))
})
