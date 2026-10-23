# PCB Coolant

Janne's request (2026-10-23). GT: polychlorinated biphenyl (one reactor step from biphenyl) mixed 3:1 with distilled
water. Now an askarel, the dielectric of old transformers. Code: `AF9Materials.askarel()`,
`kubejs/server_scripts/mods/gtceu/pcb_coolant.js`, quests in the `zero_point_module` chapter (next to GT's biphenyl quests),
JEI page `kubejs/client_scripts/mud_coolant_info.js`.

| Step | Machine | Takes | Makes |
|---|---|---|---|
| 0. biphenyl (GT) | Chemical Reactor | benzene, oxygen | biphenyl dust |
| 1. dichlorobiphenyl | Chemical Reactor, HV | 2 biphenyl dust, 2000 mB chlorine, FeCl3 (not used up) | 1000 mB Dichlorobiphenyl, 2000 mB HCl |
| 2. polychlorination | Large Chemical Reactor, EV (circuit 2) | 1000 mB dichlorobiphenyl, 2000 mB chlorine, FeCl3 | 1000 mB PCB, 2000 mB HCl |
| 3. trichlorobenzene | Chemical Reactor, HV (circuit 3) | 1000 mB dichlorobenzene (aromatics.js), 1000 mB chlorine, FeCl3 | 1000 mB Trichlorobenzene, 1000 mB HCl |
| 4. askarel | Mixer, HV | 600 mB PCB, 400 mB trichlorobenzene | 1000 mB Askarel |
| 5. calcium chloride | Chemical Reactor, MV | 1 calcite, 2000 mB HCl | 1 calcium chloride, 1000 mB CO2, 1000 mB water |
| 6. dry | Chemical Reactor, HV | 1000 mB askarel, 1 calcium chloride | 1000 mB Dry Askarel |
| 7. stabiliser (GT) | Chemical Reactor, LV | glycerol, HCl | epichlorohydrin |
| 8. blend | Mixer, HV | 900 mB dry askarel, 100 mB epichlorohydrin | 1000 mB PCB Coolant |

GT's one-step PCB recipe (and its Large Chemical Reactor copy) and the mixer recipe are removed.

## Progression

First user: the IV HPCA and the Active Transformer (the fork's HPCA coolers use it too). The chain tops out at an EV Large
Chemical Reactor, which comes before IV. Inputs: benzene (wood tar, MV), chlorine, iron (FeCl3 from iron and HCl, GT), glycerol
(biodiesel), calcite (ore, or the drilling fluid chain's by-product). No input comes from anything that needs PCB Coolant.
The HCl that every chlorination gives is used up again by steps 5 and 7. The quests sit in the ZPM chapter as before.
