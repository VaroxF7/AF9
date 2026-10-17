# Quantum Computer

GTNH's TecTech **Quantum Computer** in GT terms: the large computation source. The HPCA gives tens of CWU/t for a full
build; one Computer Rack Hatch with a single crystal processor gives 120. The pack's wireless computation hatches
(`docs/computation.md`) take its output anywhere.

Source: GT5-Unofficial `tectech.thing.metaTileEntity.multi.MTEQuantumComputer` and `hatch.MTEHatchRack` (LGPL-3.0; textures
and credits in `CREDITS-textures.md`). Code: af9-core `com.af9.core.quantum` (`QuantumComputerMachine`, `RackPartMachine`,
`RackComponent`); KubeJS `startup_scripts/gtceu/quantum_computer.js` (machines, structure), `server_scripts/mods/gtceu/quantum_computer.js`
(recipes).

## 1. What it is

`gtceu:quantum_computer` (UV) is a GT computation source (`IOpticalComputationProvider`). Anything that takes GT's
**Computation Data Transmission Hatch** on an HPCA takes the same hatch on it, and so the **Wireless Computation
Transmission Hatch** (`gtceu:uv_wireless_computation_transmitter`) from `docs/computation.md`: a receiver with the same
ID gets the computation in any other multiblock (Research Station, Orbital Station, a lithography machine).

Once a second, while it is switched on and paid for, every rack runs its components; the sum is this second's capacity in
CWU/t. The hatches hand it out like the HPCA's (`requestCWUt` / `getMaxCWUt`, allocation per tick). No energy means no
computation and racks that only cool. A soft mallet (or the screen) switches it off.

## 2. Structure

TecTech's, block for block: a corridor **3 wide and 4 high, 5 to 17 long** (1 to 13 rack slices). One column is air (the
racks face it, the vents blow into it) and two are the wall. Rows bottom to top, back to front:

| Slice | Rows (air, middle, wall) | Count |
|---|---|---|
| back | `_AA` ×4 | 1 |
| cap | `-CB`, `_DD`, `_DD`, `-CB` | 1 |
| rack slice | `-CB`, `_ED`, `_ED`, `-CB` | 1 to 13 |
| cap | as above | 1 |
| front | `_AA`, `_SA`, `_AA`, `_AA` (the controller in the second row, middle) | 1 |

`A` Quantum Computer Casing or a hatch, `B` Quantum Computer Casing, `C` Quantum Computer Vent Casing (with air beside it),
`D` Advanced Quantum Computer Casing, `E` Computer Rack Hatch or Advanced Quantum Computer Casing, `-` air, `_` anything.

Hatches go in the `A` casing of the front and back slice, maximums only: energy hatches (8), laser target hatches (2),
Computation Data Transmission Hatches (4, also the wireless one), a maintenance hatch (1). Up to 26 racks (two in each slice).

## 3. Racks, processors, heat

A **Computer Rack Hatch** (`gtceu:uv_computer_rack_hatch`) has four slots and its own heat. The slots are locked while the
computer runs and while the rack is above 2,000 heat (as in TecTech).

Each second the computer runs its racks. For every component not burnt out (`oc` is the overclock ratio, `ov` the overvolt
ratio, `h` the rack's heat):

* a **processor** makes `H × oc × ov²` heat × `(1 − h / 100,000)` and, if `ov` beats a random roll, computes
  `C × (1 + oc²) / (1 + (oc − ov)²)` CWU/t. At `oc = ov = 1` that is twice its base value: a crystal processor gives 120;
* a **heat vent** takes `10 × (1 + cool × h / 100,000)` heat, only while the rack is at 0 heat or above.

Then the rack cools by itself: at least 20 heat, a thousandth of what it has, and `h / 10,000` for each vent. A component
in a rack hotter than its limit **burns out** (destroyed, counted on the rack's screen). The rack's heat and what it computes
show in its screen, the computer's screen shows the sum.

| Item | Base CWU/t | Heat/s | Burns out above |
|---|---|---|---|
| Crystal Processor (IV) / Assembly (LuV) / Computer (ZPM) / Mainframe (UV) | 60 / 80 / 100 / 120 | 56 / 54 / 52 / 50 | 2,000 |
| Wetware Processor (LuV) / Assembly (ZPM) / Computer (UV) / Mainframe (UHV) | 160 / 180 / 200 / 220 | 46 / 44 / 42 / 40 | 4,000 |
| XPS Processor / Mainframe | 260 / 300 | 26 / 22 | 8,000 |
| NVM Processor / Mainframe | 340 / 400 | 14 / 10 | 10,000 |
| Rack Heat Vent MK-I / II / III / IV | cooling constant 40 / 80 / 120 / 160 | | 2,000 / 4,000 / 6,000 / 8,000 |

TecTech's table, the circuits of the pack's ladder in the places of GTNH's: its bio and optical circuits (200–260) are the
XPS and NVM tiers here, its piko / quantum / Planck circuits (260–360) the NVM tier. GTNH's vents are IC2's reactor vents;
here they are the Rack Heat Vents (`af9:rack_heat_vent_mk1` to `mk4`).

Rule of thumb at `oc = ov = 1`: one crystal processor with three MK-I vents settles near 450 heat (with MK-II vents near 250);
two processors need vents that take about 46 heat each, which only MK-III and MK-IV reach, at the heat they work at (a vent
takes more the hotter the rack), so hot racks want the high-limit wetware, XPS or NVM processors and the better vents. Every
processor and vent shows its numbers in its tooltip.

## 4. Power

It draws `max(LuV, ZPM × oc × ov)` EU/t for each amp, and the amps are **1 for the controller plus 1 for every rack that
computes** (TecTech's `1 + racks`): 131,072 EU/t ×(1 + racks) at the default ratios (a 6-rack computer: 917,504 EU/t, UV
hatches or laser hatches). Without that much in the hatches it does not run; the screen says so.

## 5. Tiers and recipes

The computer and its Rack Hatch are UV; the casings are ZPM, the vents EV to ZPM, in one chain.

| Output | Machine | Inputs |
|---|---|---|
| 2 Quantum Computer Casing | Assembler, ZPM | Robust machine casing, 2 LuV circuits, 4 stainless steel plates, 288 mB soldering alloy |
| 2 Quantum Computer Vent Casing | Assembler, ZPM | Quantum Computer Casing, 2 stainless steel rotors, IV motor, 4 aluminium plates |
| 2 Advanced Quantum Computer Casing | Assembler, ZPM | Quantum Computer Casing, 2 ZPM circuits, 2 ZPM sensors, 4 tungsten steel plates, 576 mB soldering alloy |
| Quantum Computer | Crafting | 4 UV circuits, 2 UV emitters, 2 UV field generators, Advanced Quantum Computer Casing |
| UV Computer Rack Hatch | Assembler, UV | UV hull, 2 Advanced Quantum Computer Casing, 2 UV circuits, 2 UV conveyors, UV robot arm, 1,152 mB soldering alloy |
| Rack Heat Vent MK-I | Assembler, EV | 4 aluminium plates, stainless steel rotor, EV motor |
| Rack Heat Vent MK-II | Assembler, IV | MK-I, 4 titanium plates, tungsten steel rotor, IV motor |
| Rack Heat Vent MK-III | Assembler, LuV | MK-II, 4 tungsten steel plates, rhodium-plated palladium rotor, LuV motor |
| Rack Heat Vent MK-IV | Assembler, ZPM | MK-III, 4 osmiridium plates, naquadah alloy rotor, ZPM motor |

## 6. Left out of GTNH's

The Uncertainty Hatch, the data packets with their trace and routing (the Data Input and Output Hatches become GT's computation
hatches), the Parametrizer's hatch (the two ratios are on the screen) and the explosions of overheated racks.
