package com.af9.core;

import com.af9.core.blast.BouleMelting;
import com.af9.core.client.AF9Client;
import com.af9.core.common.AF9Sounds;
import com.af9.core.compat.xei.StagedRecipeUI;
import com.af9.core.compat.adastra.AdAstraCompat;
import com.af9.core.compat.extremereactors.ExtremeReactorsCompat;
import com.af9.core.elevator.SpaceMissionMachine;
import com.af9.core.fab.FabRecipeInfo;
import com.af9.core.machine.ParticleAcceleratorMachine;
import com.af9.core.machine.PhotolithographyLineMachine;
import com.af9.core.machine.StagedAssemblyMachine;
import com.af9.core.machine.VoidMinerMachine;
import com.af9.core.network.AF9Network;
import com.af9.core.pattern.AF9Filters;
import com.af9.core.registry.AF9Blocks;
import com.af9.core.registry.AF9Items;
import com.af9.core.registry.AF9Materials;
import com.af9.core.registry.AF9Tabs;
import com.af9.core.space.AF9Space;

import com.gregtechceu.gtceu.api.registry.registrate.GTRegistrate;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * AF9 Core: the AF9 modpack's own content and its Java-side machine logic.
 * It registers the pack's blocks, items and materials ({@code com.af9.core.registry}); machines, recipe types and
 * recipes are defined in KubeJS, and this mod supplies the behaviour KubeJS cannot
 * (consoles, module detection, recipe gating, the lithography vacuum and break roll, wafer and chip contamination,
 * the EBF's Boule Melting mode, the Plascrete Filter Casing as a cleanroom filter, fluids inside running machines, Jade
 * tooltips, the asteroids of the Asteroid Field, the radiation warning). Settings: {@link AF9Config}.
 */
@Mod(AF9Core.MOD_ID)
public class AF9Core {

    public static final String MOD_ID = "af9";
    public static final Logger LOGGER = LogManager.getLogger();
    /** GregTech's registrate for this mod: what is registered through GregTech's builders goes through it. */
    public static final GTRegistrate REGISTRATE = GTRegistrate.create(MOD_ID);

    @SuppressWarnings("removal")
    public AF9Core() {
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::commonSetup);
        AF9Sounds.register(FMLJavaModLoadingContext.get().getModEventBus());
        REGISTRATE.registerRegistrate();
        // the pack's materials, in GregTech's material phase
        FMLJavaModLoadingContext.get().getModEventBus().addListener(AF9Materials::register);
        // the pack's plain blocks and items
        AF9Blocks.register(FMLJavaModLoadingContext.get().getModEventBus());
        AF9Items.register(FMLJavaModLoadingContext.get().getModEventBus());
        AF9Tabs.register(FMLJavaModLoadingContext.get().getModEventBus());
        // the Asteroid Field's feature
        AF9Space.register(FMLJavaModLoadingContext.get().getModEventBus());
        // supercritical steam in Extreme Reactors' turbines (only with Extreme Reactors loaded)
        FMLJavaModLoadingContext.get().getModEventBus().addListener(ExtremeReactorsCompat::enqueue);
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, AF9Config.SPEC);
        if (FMLEnvironment.dist == Dist.CLIENT) AF9Client.init();
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        // GT recipe types are registered (by KubeJS) before common setup; touch them on the main thread
        event.enqueueWork(PhotolithographyLineMachine::registerRecipeInfo);
        event.enqueueWork(ParticleAcceleratorMachine::registerRecipeInfo);
        event.enqueueWork(SpaceMissionMachine::registerRecipeInfo);
        event.enqueueWork(FabRecipeInfo::register);
        // Boule Melting: second mode of GT's Electric Blast Furnace (before any machine is created)
        event.enqueueWork(BouleMelting::install);
        // Void Miner, rebuilt: GT's controller block and structure stay, AF9 takes over the definition
        event.enqueueWork(VoidMinerMachine::install);
        event.enqueueWork(VoidMinerMachine::registerRecipeInfo);
        // Staged Assembly: the staged recipe pages and their JEI / EMI look (the cover registers earlier, in
        // AF9Addon.registerCovers, while GregTech still accepts covers)
        event.enqueueWork(StagedAssemblyMachine::registerRecipeInfo);
        event.enqueueWork(StagedRecipeUI::install);
        // the blocks exist now; structures are only checked later
        event.enqueueWork(AF9Filters::register);
        // the orbital station's magnetic field sets gravity through Ad Astra
        event.enqueueWork(AdAstraCompat::init);
        // the orbital ring's death screen
        event.enqueueWork(AF9Network::register);
    }
}
