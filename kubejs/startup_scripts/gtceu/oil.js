// AF9 - The new oil: the fluids of the chain Oil Regolith -> Impure Oil -> Shiny Oil -> Oil and Heavy Oil, and Heavy Water.
// The regolith block is af9-core's (com.af9.core.space.OilRegolithBlock, grown in the Asteroid Field's rock); recipes:
// server_scripts/mods/gtceu/oil.js; the world's oil is switched off and the asteroids' fluid deposits made in
// server_scripts/mods/gtceu/vein_oil.js. Spec: docs/oil.md

const $OilFluidBuilder = Java.loadClass('com.gregtechceu.gtceu.api.fluids.FluidBuilder')

GTCEuStartupEvents.registry('gtceu:material', event => {
    // Impure Oil: black and brown, thick. Its own animated texture (kubejs/assets/gtceu/textures/block/fluids/
    // fluid.impure_oil.png), no tint on top of it.
    event.create('impure_oil')
        .liquid(new $OilFluidBuilder().customStill().disableColor())
        .color(0x2a1a0e)

    // Shiny Oil: what the wash leaves, amber with glints running over it (fluid.shiny_oil.png, animated)
    event.create('shiny_oil')
        .liquid(new $OilFluidBuilder().customStill().disableColor())
        .color(0xb87a1a)

    // Heavy Water: D2O, from the Asteroid Field's deposits (GT has deuterium, no heavy water)
    event.create('heavy_water')
        .liquid()
        .color(0x8fd0ff)
        .formula('D2O')
})
