package com.af9.core.startup;

import com.af9.core.startup.machines.AdvancedCircuitManufacturer;
import com.af9.core.startup.machines.AgriSim;
import com.af9.core.startup.machines.AirConditioning;
import com.af9.core.startup.machines.AsteroidFission;
import com.af9.core.startup.machines.AtmosphericSimulation;
import com.af9.core.startup.machines.BioVat;
import com.af9.core.startup.machines.ChemicalPlant;
import com.af9.core.startup.machines.Cryogenics;
import com.af9.core.startup.machines.CwuServer;
import com.af9.core.startup.machines.DistillationFacility;
import com.af9.core.startup.machines.Dtpf;
import com.af9.core.startup.machines.DtpfModules;
import com.af9.core.startup.machines.ElectricImplosion;
import com.af9.core.startup.machines.Hile;
import com.af9.core.startup.machines.IndustrialCutter;
import com.af9.core.startup.machines.LargeWiremill;
import com.af9.core.startup.machines.LithoProcess;
import com.af9.core.startup.machines.LogisticsHub;
import com.af9.core.startup.machines.MeResonanceFabricator;
import com.af9.core.startup.machines.OrbitalModules;
import com.af9.core.startup.machines.ParticleAccelerator;
import com.af9.core.startup.machines.PlanetarySatellite;
import com.af9.core.startup.machines.PlasmaTransformer;
import com.af9.core.startup.machines.PrecisionFabricator;
import com.af9.core.startup.machines.QuadHatches;
import com.af9.core.startup.machines.SanguiniteHearth;
import com.af9.core.startup.machines.SpaceElevator;
import com.af9.core.startup.machines.StagedAssembly;
import com.af9.core.startup.machines.VoidMining;
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
        AtmosphericSimulation.register();
        BioVat.register();
        ChemicalPlant.register();
        Cryogenics.register();
        CwuServer.register();
        DistillationFacility.register();
        Dtpf.register();
        DtpfModules.register();
        ElectricImplosion.register();
        Hile.register();
        IndustrialCutter.register();
        LargeWiremill.register();
        LithoProcess.register();
        LogisticsHub.register();
        MeResonanceFabricator.register();
        OrbitalModules.register();
        ParticleAccelerator.register();
        PlanetarySatellite.register();
        PlasmaTransformer.register();
        PrecisionFabricator.register();
        QuadHatches.register();
        SanguiniteHearth.register();
        SpaceElevator.register();
        StagedAssembly.register();
        VoidMining.register();
        WaterFiltration.register();
        WirelessEnergy.register();
    }
}
