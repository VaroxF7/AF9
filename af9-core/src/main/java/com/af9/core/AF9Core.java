package com.af9.core;

import com.af9.core.blast.BouleMelting;
import com.af9.core.fab.FabRecipeInfo;
import com.af9.core.machine.PhotolithographyLineMachine;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * AF9 Core: Java-side machine logic for the AF9 modpack.
 * Machines, materials and recipes are defined in KubeJS; this mod supplies the behaviour KubeJS cannot
 * (consoles, module detection, recipe gating, the lithography vacuum and break roll, wafer contamination, the EBF's
 * Boule Melting mode, Jade tooltips).
 */
@Mod(AF9Core.MOD_ID)
public class AF9Core {

    public static final String MOD_ID = "af9";
    public static final Logger LOGGER = LogManager.getLogger();

    @SuppressWarnings("removal")
    public AF9Core() {
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::commonSetup);
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        // GT recipe types are registered (by KubeJS) before common setup; touch them on the main thread
        event.enqueueWork(PhotolithographyLineMachine::registerRecipeInfo);
        event.enqueueWork(FabRecipeInfo::register);
        // Boule Melting: second mode of GT's Electric Blast Furnace (before any machine is created)
        event.enqueueWork(BouleMelting::install);
    }
}
