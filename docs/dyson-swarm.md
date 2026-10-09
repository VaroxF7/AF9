# Dyson Swarm

A power plant that catches starlight with **sails**: GTNH Intergalactic's Dyson Swarm (`gtnhintergalactic.tile.multi.TileEntityDysonSwarm`)
with its structure copied block for block, its hourly cycle kept, and three AF9 sails with a yield each. The power goes out through
the **Dyson Output Hatch**, which feeds a Power Substation directly. Machine: `gtceu:dyson_swarm` (af9-core `DysonSwarmMachine`,
`startup_scripts/gtceu/dyson_swarm.js`); hatch: `gtceu:uhv_dyson_output_hatch` (`DysonOutputHatchPartMachine`); one swarm to a star:
`DysonStars`; recipes: `server_scripts/mods/gtceu/dyson_swarm.js`.

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
| Receiver base | `af9:dyson_receiver_casing` | up to 8 Dyson Output Hatches |
| Deployment unit base | `af9:dyson_deployment_casing` | up to 4 item input (sails), 4 fluid input (coolant) |
| Command centre base | `af9:dyson_control_casing` | 1 maintenance |
| others | `dyson_receiver_dish`, `dyson_deployment_core`, `dyson_deployment_magnet`, `dyson_control_primary`, `dyson_control_secondary`, `dyson_control_toroid`; HSS-S, titanium and tritanium frames; stable casing; resonant endion coil | - |

Textures are GTNH Intergalactic's (`tools/textures/gtnh_dyson_swarm/`, `tools/textures/dyson_swarm.py`).

## 3. The sails

| Sail | Yield | Power | Made from (Assembly Line, UHV voltage) |
| --- | --- | --- | --- |
| `af9:allthemodium_sail` | 100 % | 599,186 EU/t (2/7 A of UHV) | Allthemodium plate, photonic dies, neutronium plate; **100 A** |
| `af9:unobtainium_alloy_sail` | 200 % | 1,198,372 EU/t (4/7 A) | Allthemodium sails, Unobtainium plate, spin logic dies; **300 A** |
| `af9:chromodynium_star_matter_tritan_alloy_sail` | 350 % | 2,097,152 EU/t (**1 A**) | Unobtainium Alloy sails, chromodynium and tritanium plate, memristors, strange matter plasma (the DTPF's); **1,000 A** |

One sail a run (30 s, 45 s and 60 s; each takes 128 carbon fiber mesh and 128 fine sanguinite wire; the chromodynium sail also 10,000 mB plasma solder and 1,000 mB nickel plasma), and every sail recipe has to be **researched** first (the Research Station scans the sail below it, or the photonic package for the first, with 96 / 128 / 192 CWU/t of computation).
Put them in an input bus: the swarm takes them (up to 10,000 in all) and they stay. A **plunger** right-clicked on the controller takes them back, lowest tier first, as many as the plunger has uses left (sneak to drop what does not fit the inventory; without sneaking what does not fit stays in the swarm).

## 4. The power

Every sail adds its own yield to the total, in any mix: the swarm's output is the sum. 1,000 Allthemodium and 9,000 Unobtainium Alloy sails are
1,000 x 2/7 + 9,000 x 4/7 = 5,429 A, and one best sail added (or put in place of a lower one) adds its own amp on top. The best sail is
**exactly one amp of UHV**, so a full swarm (10,000 sails) of the best is **10,000 A of UHV** (20,971,520,000 EU/t), the most a swarm gives.
There is no light factor and no loss.

| Swarm | Power |
| --- | --- |
| 10,000 Allthemodium | 2,857 A |
| 10,000 Unobtainium Alloy | 5,714 A |
| 10,000 Star Matter Tritan Alloy | 10,000 A |
| 1 of each | 1.857 A (3,894,710 EU/t) |

The output is set when a cycle starts (the swarm takes what is in its buses, then scales the cycle to its sails), and a cycle keeps it to its end.

## 5. The cycle

* **Cycle**: an hour (72,000 ticks) of power on 360 B of supercooled hydrogen. A swarm without sails does not start.
* **A generator's cycle**: the cycle only runs while the output hatches take the power. A full buffer (nothing downstream uses it) pauses the cycle where it
  stands, so the hydrogen is paid per hour of power actually made; the console says NO ROOM then.
* **No collisions**: the sails stay in the swarm for good.
* The console and Jade show the sails by tier, the star, the output (EU/t and amps of UHV) and why the swarm is not running.

## 6. One swarm to a star

The swarm finds its star from the dimension it stands in (Ad Astra's planet data, so it follows datapacks too):

| Star | Dimensions |
| --- | --- |
| the Sun (`ad_astra:solar_system`) | the Overworld, Moon, Mars, Venus, Mercury, the asteroid field, AF9's planets (Zephyr, Kronos, Helios, Ceres), every orbit, and any dimension no planet claims (the End) |
| Alpha Centauri (`ad_astra:proxima_centauri`) | Glacio and the orbit of Glacio |
| none | the Nether and the Mining dimension: a swarm there does not run |

Any number of swarms can be built, but only one runs on a star: the first to start a cycle holds it (saved with the world, `af9_dyson_stars`), the
others show STAR TAKEN and where the holder is. A holder lets go when its controller is broken, its structure is broken or it has no sails and no cycle.
A holder in an unloaded chunk keeps the star, so a restart never hands it to another swarm.

## 7. The Dyson Output Hatch

`gtceu:uhv_dyson_output_hatch`: UHV, 10,000 A (21 GEU/t, a full swarm of the best sails), the only power output the swarm takes. Made in the Assembler
from a UHV hull, a UHV 1,024 A laser source hatch, two UV field generators and tritanium plates.

* It puts out of its front like a dynamo hatch.
* A **Power Substation** touching the hatch (any hatch or the controller of a formed one, on any side) takes the power **directly into its energy bank**,
  past the 64 A of the substation's own input hatches. The substation's output hatches and batteries carry it on.

## 8. The recipe page

The swarm's page in JEI / EMI (`gtceu:dyson_swarm`) has a slider for the number of sails (0 to 10,000) and shows what that many sails of each tier make in amps of UHV.
