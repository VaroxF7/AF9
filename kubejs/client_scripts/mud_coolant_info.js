// AF9 - how Drilling Fluid and PCB Coolant are made, as information pages of the recipe viewers (on their buckets).
// The numbers must match server_scripts/mods/gtceu/drilling_fluid.js and pcb_coolant.js (docs/drilling-fluid.md, docs/pcb-coolant.md).
JEIEvents.information(event => {
    const mud = [
        'Drilling Fluid is a water-based mud, made in six steps and nothing above HV: soda ash softens water (mixer); bentonite and soda ash in that water make sodium bentonite slurry (mixer); sawdust, lye and water make alkali cellulose (mixer); acetic acid and chlorine make chloroacetic acid (chemical reactor); alkali cellulose and chloroacetic acid make a carboxymethyl cellulose solution (chemical reactor); slurry, CMC solution, salt, calcite and lye blend to 5000 mB of mud (mixer).',
        'Nothing in it needs oil, asteroids or the Void Miner. Soda ash gives back calcite, the CMC step gives back salt, so a line feeds itself with bentonite, sawdust, lye, soda ash, acetic acid and chlorine.',
        'The Void Miner drinks 2000 mB of it per run.'
    ]
    const coolant = [
        'PCB Coolant is an askarel, the dielectric of old transformers: polychlorinated biphenyl cut with trichlorobenzene, dried and stabilised.',
        'Biphenyl is chlorinated twice over ferric chloride (reactor to dichlorobiphenyl, then the EV Large Chemical Reactor to the PCB); dichlorobenzene takes a third chlorine to trichlorobenzene. 600 mB PCB and 400 mB trichlorobenzene mix to askarel (HV mixer); calcium chloride dries it (reactor); 900 mB of dry askarel and 100 mB epichlorohydrin make 1000 mB of coolant.',
        'Every chlorination gives HCl: it makes the epichlorohydrin from glycerol and the calcium chloride from calcite, so the acid is used up again. The first user is the HPCA.'
    ]
    event.addItem('gtceu:drilling_fluid_bucket', mud)
    event.addItem('gtceu:void_miner', mud)
    event.addItem('gtceu:pcb_coolant_bucket', coolant)
})
