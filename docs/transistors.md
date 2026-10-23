# Transistors (virtual)

Transistors are no item. A silicon chip wafer carries a fixed count; printing it uses up the fluids that count costs.

The fluids go in through the machine's auxiliary fluid hatches (line 8, scanner 9, station 11).

| per 100 transistors | |
|---|---|
| Polyethylene | 12 mB |
| Silicon | 1 mB |
| Electrum | 2 mB |

Counts per wafer: CPU 12000, APU 12000, MCU 8200, RAM 8000, Simple SoC 6700, LPIC 5000, ILC 4500, ULPIC 3540, RF Transceiver 1200.

Price rises 20% per node (`NODE_PRICE` 1.2, node index 0 = 350 nm ... 8 = 1 nm), and is billed per wafer printed (a finer node prints more wafers per run). The 1 nm station bills the average of its wafer range. Example: CPU at 350 nm = 1440 mB polyethylene, 120 mB silicon, 240 mB electrum.

Only the silicon chips (native substrate silicon) cost transistors; the higher wafers are unchanged. GT's component items (SMD transistors on boards) are not the virtual resource and stay.

Code: `AF9_WAFERS.transistorFluids` in `kubejs/server_scripts/mods/gtceu/photolithography.js`. JEI page: `kubejs/client_scripts/transistors_info.js`. Quest: Photolithography chapter, "Transistors".
