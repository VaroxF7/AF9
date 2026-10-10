package com.af9.core.droppod.client;

import com.af9.core.AF9Core;
import com.af9.core.droppod.AF9DropPod;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Client registration of the drop pod's model layer and renderer. */
@Mod.EventBusSubscriber(modid = AF9Core.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class DropPodClient {

    private DropPodClient() {}

    @SubscribeEvent
    public static void registerLayers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(DropPodModel.LAYER, DropPodModel::createLayer);
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(AF9DropPod.DROP_POD.get(), DropPodRenderer::new);
    }
}
