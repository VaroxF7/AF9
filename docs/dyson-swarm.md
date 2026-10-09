# Dyson Swarm

A power plant that catches starlight with **sails**: GTNH Intergalactic's Dyson Swarm (`gtnhintergalactic.tile.multi.TileEntityDysonSwarm`)
with its structure copied block for block, its rules kept (sails stay in the swarm, an hourly cycle, collisions), and three AF9
sails with a yield each. Machine: `gtceu:dyson_swarm` (af9-core `DysonSwarmMachine`, `startup_scripts/gtceu/dyson_swarm.js`);
recipes: `server_scripts/mods/gtceu/dyson_swarm.js`.

## 1. The power rule

UHV and above are made on many amps of UHV power, and the swarm is what pays for it:

| Tier | Recipes | Power |
| --- | --- | --- |
| UHV | the UHV mainframe (plasma soldering), the Particle Accelerator's UHV runs (strange matter, chromodynium, plasma solder dust), the DTPF | 100 A of UHV (209,715,200 EU/t) |
| XPS | the XPS processor and mainframe (Assembly Line, normal and lean), the Pico line (`pico_fabrication`) | 300 A of UHV |
| NVM | the NVM processor and mainframe (Assembly Line, normal and lean) | 1,000 A of UHV (2,097,152,000 EU/t) |

The amps are the recipes' (`.EUt(VA[GTValues.UHV], amps)`). A machine runs a recipe only when its hatches supply all of it:
laser target hatches (1,024 A at UHV) carry it where the machine takes them (the DTPF, the Particle Accelerator, the Orbital Array).

## 2. The structure

16 x 20 x 16, GTNH's: a floor of ultra high strength concrete, the **receiver** (a dish round a sphere of air, with the base
casing the power leaves through), the **deployment unit** (casing, core and superconducting magnets between tritanium frames)
and the **command centre** (casing, primary and secondary windings, a toroid). The air of the sphere must stay empty.

| Part | Block | Hatches |
| --- | --- | --- |
| Receiver base | `af9:dyson_receiver_casing` | up to 8 power output (energy or laser source) |
| Deployment unit base | `af9:dyson_deployment_casing` | up to 4 item input (sails), 4 fluid input (coolant) |
| Command centre base | `af9:dyson_control_casing` | up to 2 computation, 1 maintenance |
| others | `dyson_receiver_dish`, `dyson_deployment_core`, `dyson_deployment_magnet`, `dyson_control_primary`, `dyson_control_secondary`, `dyson_control_toroid`; HSS-S, titanium and tritanium frames; stable casing; resonant endion coil | - |

Textures are GTNH Intergalactic's (`tools/textures/gtnh_dyson_swarm/`, `tools/textures/dyson_swarm.py`).

## 3. The sails

| Sail | Yield | Made from (Assembly Line, UHV voltage) |
| --- | --- | --- |
| `af9:allthemodium_sail` | 100 % | Allthemodium plate, photonic dies, neutronium plate; **100 A** |
| `af9:unobtainium_alloy_sail` | 200 % | Allthemodium sails, Unobtainium plate, spin logic dies; **300 A** |
| `af9:chromodynium_star_matter_tritan_alloy_sail` | 350 % | Unobtainium Alloy sails, chromodynium and tritanium plate, memristors, strange matter plasma (the DTPF's); **1,000 A** |

One sail a run (30 s, 45 s and 60 s; each takes 128 carbon fiber mesh and 128 fine sanguinite wire; the chromodynium sail also 10,000 mB plasma solder and 1,000 mB nickel plasma), and every sail recipe has to be **researched** first (the Research Station scans the sail below it, or the photonic package for the first, with 96 / 128 / 192 CWU/t of computation).
Put them in an input bus: the swarm takes them (up to 10,000 in all) and they stay. A **plunger** right-clicked on the controller takes them back, lowest tier first, as many as the plunger has uses left (sneak to drop what does not fit the inventory).

## 4. The cycle

* **Power**: every sail gives 262,144 EU/t (an eighth of a UHV amp) at 100 %; the swarm's output is the sum of its sails' yields
  times the light of the dimension (`DysonSwarmMachine.SWARM` sets it when a cycle starts). A full swarm of Allthemodium sails
  (10,000) gives 2.6 GEU/t, more than an NVM line's 1,000 A; of star matter sails, 9.2 GEU/t.
* **Cycle**: an hour (72,000 ticks) on 360 B of supercooled hydrogen. A swarm without sails does not start.
* **Collisions** (GTNH's formula): at the end of a cycle `n * 2 * 0.066 / (e^(-0.00005 * (n - 1)) + e^(0.00003 * c))` of the `n`
  sails are lost, `c` the computation the hatches give (CWU/t, at most 100,000). More sails collide more; computation steers
  them clear. The loss is shared out by tier.
* **Light**: Overworld 100 %, Moon 100 %, Mars 81 %, Venus 176 %, Mercury 161 %, Glacio 32 %, the orbit of Earth 110 %, of Mars 89 %,
  of Venus 194 %; the asteroid field 61 %; the Nether and the Mining dimension none; any other dimension 100 %.
* The console and Jade show the sails by tier, the light, the output and the loss per cycle.

## 5. The recipe page

The swarm's page in JEI / EMI (`gtceu:dyson_swarm`) has a slider for the number of sails (0 to 10,000) and shows what that many sails of each tier make in EU/t.
