package com.af9.core;

import com.af9.core.ae2.AF9AE2;
import com.af9.core.blast.BouleMelting;
import com.af9.core.bus.AF9Bus;
import com.af9.core.client.AF9Client;
import com.af9.core.compute.AF9Compute;
import com.af9.core.common.AF9Sounds;
import com.af9.core.compat.adastra.AdAstraCompat;
import com.af9.core.compat.extremereactors.ExtremeReactorsCompat;
import com.af9.core.fab.FabRecipeInfo;
import com.af9.core.machine.ParticleAcceleratorMachine;
import com.af9.core.machine.PhotolithographyLineMachine;
import com.af9.core.network.AF9Network;
import com.af9.core.pattern.AF9Filters;
import com.af9.core.space.AF9Space;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * AF9 Core: Java-side machine logic for the AF9 modpack.
 * Machines, materials and recipes are defined in KubeJS; this mod supplies the behaviour KubeJS cannot
 * (consoles, module detection, recipe gating, the lithography vacuum and break roll, wafer and chip contamination,
 * the EBF's Boule Melting mode, the Plascrete Filter Casing as a cleanroom filter, fluids inside running machines, Jade
 * tooltips, the machine bus, ME networks needing computation, the asteroids of the Asteroid Field, the radiation
 * warning). Settings: {@link AF9Config}.
 */
@Mod(AF9Core.MOD_ID)
public class AF9Core {

    public static final String MOD_ID = "af9";
    public static final Logger LOGGER = LogManager.getLogger();

    @SuppressWarnings("removal")
    public AF9Core() {
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::commonSetup);
        AF9Sounds.register(FMLJavaModLoadingContext.get().getModEventBus());
        AF9Bus.register(FMLJavaModLoadingContext.get().getModEventBus());
        AF9Compute.register(FMLJavaModLoadingContext.get().getModEventBus());
        // the Asteroid Field's feature
        AF9Space.register(FMLJavaModLoadingContext.get().getModEventBus());
        // supercritical steam in Extreme Reactors' turbines (only with Extreme Reactors loaded)
        FMLJavaModLoadingContext.get().getModEventBus().addListener(ExtremeReactorsCompat::enqueue);
        // AE2: ME networks need computation (only with AE2 loaded; nothing of it loads without)
        if (ModList.get().isLoaded("ae2")) AF9AE2.init(FMLJavaModLoadingContext.get().getModEventBus());
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, AF9Config.SPEC);
        if (FMLEnvironment.dist == Dist.CLIENT) AF9Client.init();
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        // GT recipe types are registered (by KubeJS) before common setup; touch them on the main thread
        event.enqueueWork(PhotolithographyLineMachine::registerRecipeInfo);
        event.enqueueWork(ParticleAcceleratorMachine::registerRecipeInfo);
        event.enqueueWork(FabRecipeInfo::register);
        // Boule Melting: second mode of GT's Electric Blast Furnace (before any machine is created)
        event.enqueueWork(BouleMelting::install);
        // the KubeJS block exists now; structures are only checked later
        event.enqueueWork(AF9Filters::register);
        // the orbital station's magnetic field sets gravity through Ad Astra
        event.enqueueWork(AdAstraCompat::init);
        // the orbital ring's death screen
        event.enqueueWork(AF9Network::register);
        // the Central Monitor's wall takes a Bus Connector (the connector's ability is registered by now)
        event.enqueueWork(AF9Bus::installMonitorWall);
    }
}
