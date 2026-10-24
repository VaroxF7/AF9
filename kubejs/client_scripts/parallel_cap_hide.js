// AF9 - hide the removed UV Parallel Control Hatch (256 parallels) from the recipe viewer.
// Its recipes are removed in server_scripts/mods/gtceu/parallel_cap.js.

JEIEvents.hideItems(event => {
    // KEEP-IN-SYNC: duplicates the id in parallel_cap.js
    event.hide('gtceu:uv_parallel_hatch')
})
