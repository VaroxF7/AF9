# Atmospheric Simulation Room: Lunar air, anywhere

A 7x5x7 pressure room after GT's Cleanroom that holds a **Lunar atmosphere** in any dimension.
The controller in the ceiling centre houses the **Atmospheric Simulator**; XPS and NVM circuits
(the tiers `circuits_af9.js` had parked) build in its lunar air.

Code: `kubejs/startup_scripts/gtceu/atmospheric_simulation.js` (type `atmospheric_simulation`,
room pattern), `kubejs/server_scripts/mods/gtceu/atmospheric_simulation.js` (controller craft,
XPS/NVM recipes), AF9 Core `AtmosphereRoomMachine` (console + suit watch).

## The room

Stainless shell, tempered-glass bands, hollow 3x3x5 middle, controller ceiling-centre like a
cleanroom's, 1x2 door front-bottom-centre. Parts are maximums only: 2 energy, 2+1 item buses,
2+1 fluid hatches, 1 maintenance. Perfect OC.

## Lunar air inside

The room is genuinely lunar inside: AF9 Core watches the air volume, and a player who walks in
without a **full Ad Astra space suit** takes Ad Astra's own oxygen damage (2 damage + air cut,
every second) — the same damage its airless dimensions deal. Suited players work inside freely.
Without Ad Astra loaded the watch stays off and the console says so.

## The circuits (all UHV, plasma solder, lunar air)

| Recipe | In | Time |
|---|---|---|
| `xps_processor` | UHV wetware mainframe, 2 photonic + 2 spin-logic dies, wetware board, sanguinite wire, plasma solder, 1,000 mB lunar air | 600 t |
| `xps_processor_mainframe` | 2 XPS processors, photonic package, spin dies, wire, solder, 2,000 mB lunar air | 800 t |
| `nvm_processor` | XPS mainframe, 2 memristor + 2 quantum-dot dies, wire, solder, 1,000 mB lunar air | 600 t |
| `nvm_processor_mainframe` | 2 NVM processors, 4 memristor + 4 quantum-dot dies, wire, solder, 2,000 mB lunar air | 800 t |

Linear-ladder rules hold (each tier takes the last: wetware mainframe → XPS → XPS mainframe →
NVM → NVM mainframe; photonic/spin gate XPS end, memristor/quantum-dot gate NVM end). Lunar
air comes from Moon drilling or elevator liquid missions. The circuits chapter's XPS/NVM quests
complete off the mainframes.
