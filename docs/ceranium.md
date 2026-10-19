# Ceranium and the Pico board

The Pico circuits (`circuits_af9.js` §11) used to start from a wetware board. That is gone: the **Pico board** (`af9:pico_board`) is
laid up at **UV**, the first time any UHV+ circuit part is made, from a new alloy and fibre interconnects. The Advanced Circuit
Manufacturer (UHV) takes the finished board from there (`cleansed_pico_board` and on, unchanged).

Code: `AF9Materials.ceranium()`, `kubejs/server_scripts/mods/gtceu/ceranium.js`, `vein_ceres.js`.

## Chain

| Step | Where | Takes | Makes |
|---|---|---|---|
| 1. find it | Ceres belt (`af9:ceres`) or Space Elevator Mk-IV | dike vein `af9:ceresite_vein` (weight 20, the rarest of the belt; Ceres layer `af9_asteroid`) | raw ceresite, `(Nb,W)2Ce3O8`, byproducts niobium, tungsten, titanium |
| 2. alloy | Mixer, UV | 4 ceresite dust, 2 tritanium, 1 naquadria, 1 trinium dust | 8 ceranium dust |
| 3. melt | EBF, GT's own recipe from the blast property (9000 K) | ceranium dust | ceranium ingot (then plate, foil, fine wire) |
| 4. interconnects | Assembler, UV, cleanroom | 4 fine ceranium wire, 2 fine electrum wire, 2 glass tube, 2 PBI foil, 144 mB PBI | 4 `af9:fiber_interconnects` |
| 5. board | Assembly Line, UV, cleanroom | 2 ceranium plate, 4 fibre interconnects, 16 fine ceranium wire, 4 PBI foil, 2 tritanium foil, 576 mB PBI, 288 mB living solder | `af9:pico_board` |

Everything is UV or below, the ore is the exploration gate. Ceres is reachable by rocket long before UV (it already feeds the
fission and EV lines), so the real hunt is the vein; the Mk-IV elevator drone (UHV lasers) is the renewable source later, as for
quantanium. No other recipe takes ceresite or ceranium.

## Notes

- The gold/electrum `af9:interconnect` has two routes now: the MV one (electrum + sterling silver
  foils, `interconnects.js`) and the UHV Advanced Circuit Manufacturer's fine-wire strip. Fibre
  interconnects were an unused placeholder and are now the UV one.
- Living solder is the UV bio chain's (`solders.js`), the same solder the wetware boards used.
- Not yet tested in game; numbers to tune with a prospector (vein weight) and the mixer yield.
