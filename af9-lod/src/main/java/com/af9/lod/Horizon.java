package com.af9.lod;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.logging.LogUtils;
import net.minecraftforge.common.MinecraftForge;
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
        MinecraftForge.EVENT_BUS.register(LodWorld.INSTANCE);
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
