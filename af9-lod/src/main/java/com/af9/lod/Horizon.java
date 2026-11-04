package com.af9.lod;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.logging.LogUtils;
import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

/** Client-only entry point. Rendering hooks are added milestone by milestone, see README.md. */
@Mod(Horizon.MOD_ID)
public class Horizon {
    public static final String MOD_ID = "af9_lod";

    private static final Logger LOGGER = LogUtils.getLogger();

    public Horizon() {
        // GL calls need the render thread with a context, so the check runs at client setup, not in the constructor.
        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, LodConfig.SPEC);
        ModLoadingContext.get().registerExtensionPoint(ConfigScreenHandler.ConfigScreenFactory.class,
                () -> new ConfigScreenHandler.ConfigScreenFactory((mc, parent) -> new LodConfigScreen(parent)));
        MinecraftForge.EVENT_BUS.register(LodWorld.INSTANCE);
        MinecraftForge.EVENT_BUS.register(new LodRenderHook());
        MinecraftForge.EVENT_BUS.register(new ClientHooks());
        FMLJavaModLoadingContext.get().getModEventBus().addListener(ClientHooks::registerKeys);
        FMLJavaModLoadingContext.get().getModEventBus().addListener(Horizon::clientSetup);
    }

    private static void clientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> RenderSystem.recordRenderCall(() -> {
            GlCaps caps = GlCaps.query();
            LOGGER.info("AF9 Horizon GL check: {}", caps);
            if (!caps.usable()) LOGGER.warn("AF9 Horizon: GPU-driven LOD path unavailable on this GPU/driver");
        }));
    }
}
