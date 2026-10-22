package com.af9.core.client;

import com.af9.core.AF9Core;
import com.af9.core.client.render.BioVatRender;
import com.af9.core.client.render.EcrwArmRender;
import com.af9.core.client.render.HearthTurntableRender;
import com.af9.core.client.render.FiltrationBasinRender;
import com.af9.core.client.render.LaserEngraverRender;
import com.af9.core.client.render.LightRingRender;
import com.af9.core.client.render.LithoChamberRender;
import com.af9.core.client.render.PondWalls;
import com.af9.core.client.render.SawBladeRender;
import com.af9.core.client.render.SpaceElevatorRender;
import com.af9.core.client.skin.StructureSkinRender;
import com.af9.core.client.skin.StructureSkins;
import com.af9.core.compat.emi.EmiAcceleratorCompat;
import com.af9.core.wafer.WaferContamination;
import com.af9.core.wireless.WirelessLink;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterColorHandlersEvent;
import net.minecraftforge.client.event.ModelEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

import java.util.List;

/**
 * Client side: the dynamic machine renders, the tooltip of everything that contaminates in a player's inventory,
 * the wireless link a data stick carries, and EMI Accelerator's cache kept in step with the pack's items.
 */
public final class AF9Client {

    private AF9Client() {}

    /** Mod construction: the render types must exist before the machine models are built. */
    @SuppressWarnings("removal") // FMLJavaModLoadingContext.get() is the only way on 47.x
    public static void init() {
        LightRingRender.register();
        LithoChamberRender.register();
        SpaceElevatorRender.register();
        LaserEngraverRender.register();
        EcrwArmRender.register();
        HearthTurntableRender.register();
        SawBladeRender.register();
        PondWalls.register();
        FiltrationBasinRender.register();
        BioVatRender.register();
        // the placeable buckets tint their content layer with the fluid's colour
        FMLJavaModLoadingContext.get().getModEventBus()
                .addListener((RegisterColorHandlersEvent.Item event) -> BucketTint.register(event));
        StructureSkinRender.register();
        // the map of Ad Astra's planet screen gets AF9's bodies
        AdAstraMap.register();
        // every item is registered by client setup, and EMI has not loaded its list yet
        FMLJavaModLoadingContext.get().getModEventBus()
                .addListener((FMLClientSetupEvent event) -> EmiAcceleratorCompat.checkCache());
        // formed multiblocks drawn as one model (the Electric Blast Furnace)
        var modBus = FMLJavaModLoadingContext.get().getModEventBus();
        modBus.addListener(StructureSkins::registerModels);
        modBus.addListener(StructureSkins::attachRenders);
        modBus.addListener((FMLClientSetupEvent event) -> event.enqueueWork(StructureSkins::registerEmbeddium));
        MinecraftForge.EVENT_BUS.addListener((TickEvent.ClientTickEvent event) -> StructureSkins.onClientTick(event));
        MinecraftForge.EVENT_BUS.addListener(com.af9.core.client.skin.HiddenBlocks::onHighlight);
    }

    @Mod.EventBusSubscriber(modid = AF9Core.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
    public static final class ForgeBusEvents {

        @SubscribeEvent
        public static void onTooltip(ItemTooltipEvent event) {
            WirelessLink link = WirelessLink.read(event.getItemStack());
            if (link != null) {
                event.getToolTip().add(Math.min(1, event.getToolTip().size()),
                        Component.translatable("af9.wireless.stick_tooltip", link.where())
                                .withStyle(ChatFormatting.AQUA));
            }
            if (!WaferContamination.isSensitive(event.getItemStack())) return;
            List<Component> tooltip = event.getToolTip();
            // right below the item name
            tooltip.add(Math.min(1, tooltip.size()),
                    Component.translatable("af9.wafer.tooltip.contaminates").withStyle(ChatFormatting.RED));
            tooltip.add(Math.min(2, tooltip.size()),
                    Component.translatable("af9.wafer.tooltip.protection").withStyle(ChatFormatting.GRAY));
        }
    }
}
