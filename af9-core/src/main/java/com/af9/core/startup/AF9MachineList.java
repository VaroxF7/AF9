package com.af9.core.startup;

import com.af9.core.startup.machines.AirConditioning;
import com.af9.core.startup.machines.CwuServer;
import com.af9.core.startup.machines.DtpfModules;
import com.af9.core.startup.machines.OrbitalModules;
import com.af9.core.startup.machines.PlasmaTransformer;
import com.af9.core.startup.machines.QuadHatches;

/**
 * The machines in the order the KubeJS scripts registered them (startup script file name order): the creative tab and
 * the recipe viewers list them in registration order.
 */
public final class AF9MachineList {

    private AF9MachineList() {}

    public static void register() {
        AirConditioning.register();
        CwuServer.register();
        DtpfModules.register();
        OrbitalModules.register();
        PlasmaTransformer.register();
        QuadHatches.register();
    }
}
