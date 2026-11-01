package com.af9.core.startup;

import com.af9.core.startup.machines.AdvancedCircuitManufacturer;
import com.af9.core.startup.machines.AgriSim;
import com.af9.core.startup.machines.AirConditioning;
import com.af9.core.startup.machines.AsteroidFission;
import com.af9.core.startup.machines.BioVat;
import com.af9.core.startup.machines.ChemicalPlant;
import com.af9.core.startup.machines.Cryogenics;
import com.af9.core.startup.machines.CwuServer;
import com.af9.core.startup.machines.DtpfModules;
import com.af9.core.startup.machines.ElectricImplosion;
import com.af9.core.startup.machines.IndustrialCutter;
import com.af9.core.startup.machines.LargeWiremill;
import com.af9.core.startup.machines.LithoProcess;
import com.af9.core.startup.machines.LogisticsHub;
import com.af9.core.startup.machines.MeResonanceFabricator;
import com.af9.core.startup.machines.OrbitalModules;
import com.af9.core.startup.machines.PlasmaTransformer;
import com.af9.core.startup.machines.PrecisionFabricator;
import com.af9.core.startup.machines.QuadHatches;
import com.af9.core.startup.machines.StagedAssembly;
import com.af9.core.startup.machines.WaterFiltration;
import com.af9.core.startup.machines.WirelessEnergy;

/**
 * The machines in the order the KubeJS scripts registered them (startup script file name order): the creative tab and
 * the recipe viewers list them in registration order.
 */
public final class AF9MachineList {

    private AF9MachineList() {}

    public static void register() {
        AdvancedCircuitManufacturer.register();
        AgriSim.register();
        AirConditioning.register();
        AsteroidFission.register();
        BioVat.register();
        ChemicalPlant.register();
        Cryogenics.register();
        CwuServer.register();
        DtpfModules.register();
        ElectricImplosion.register();
        IndustrialCutter.register();
        LargeWiremill.register();
        LithoProcess.register();
        LogisticsHub.register();
        MeResonanceFabricator.register();
        OrbitalModules.register();
        PlasmaTransformer.register();
        PrecisionFabricator.register();
        QuadHatches.register();
        StagedAssembly.register();
        WaterFiltration.register();
        WirelessEnergy.register();
    }
}
