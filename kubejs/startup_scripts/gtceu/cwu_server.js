// AF9 - The CWU Server: a single block that turns EU into computation (LV-IV). Behaviour: af9-core
// com.af9.core.machine.CWUServerMachine. Recipes: server_scripts/mods/gtceu/cwu_server.js. Spec: docs/computation.md

const $CWUServer = Java.loadClass('com.af9.core.machine.CWUServerMachine')

GTCEuStartupEvents.registry('gtceu:machine', event => {
    // CWU Server (LV-IV): turns EU into computation, 4 CWU/t at LV doubling to 64 at IV, one amp of its tier at full
    // output. A GT computation source: GT Optical Fiber Cable leads it away. Its front lights: a
    // steady red dot offline (off, unpowered or nothing to give to), steady green idle, blinking while it gives (two
    // patterns, scattered by position); the models come from af9-core
    event.create('cwu_server', 'custom')
        .tiers(GTValues.LV, GTValues.MV, GTValues.HV, GTValues.EV, GTValues.IV)
        .machine((holder, tier) => new $CWUServer(holder, tier))
        .definition((tier, builder) => {
            $CWUServer.lightsModel(builder)
            builder
                .langValue(`${GTValues.VLVH[tier]} CWU Server ${GTValues.VLVT[tier]}`)
                .rotationState(RotationState.ALL)
                ['tooltips(net.minecraft.network.chat.Component[])']([
                    Component.translatable('af9.cwu_server.tooltip.0', `${$CWUServer.cwutFor(tier)}`),
                    Component.translatable('af9.cwu_server.tooltip.1', `${GTValues.VA[tier]}`),
                    Component.translatable('af9.cwu_server.tooltip.2')])
        })
})
