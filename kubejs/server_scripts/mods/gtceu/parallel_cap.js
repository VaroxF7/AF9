// AF9 - the pack allows 64 parallels at most. The UV Parallel Control Hatch (256 parallels, the top tier) is gone:
// the base pack registers the machine, which this overlay cannot unregister; so nothing makes it, nothing takes it,
// and the recipe viewer hides it (client_scripts/parallel_cap_hide.js). The ZPM hatch (64) is the highest.
// KEEP-IN-SYNC: the id is repeated in client_scripts/parallel_cap_hide.js.

ServerEvents.recipes(event => {
    event.remove({ output: 'gtceu:uv_parallel_hatch' })
    event.remove({ input: 'gtceu:uv_parallel_hatch' })
})
