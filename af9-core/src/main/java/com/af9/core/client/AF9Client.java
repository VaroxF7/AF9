package com.af9.core.client;

import com.af9.core.AF9Core;
import com.af9.core.client.render.LightRingRender;
import com.af9.core.client.render.LithoChamberRender;
import com.af9.core.client.render.ModeFluidRender;
import com.af9.core.client.render.SpaceElevatorRender;
import com.af9.core.compat.emi.EmiAcceleratorCompat;
import com.af9.core.wafer.WaferContamination;
import com.af9.core.wireless.WirelessLink;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
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
        ModeFluidRender.register();
        LightRingRender.register();
        LithoChamberRender.register();
        SpaceElevatorRender.register();
        // every item is registered by client setup, and EMI has not loaded its list yet
        FMLJavaModLoadingContext.get().getModEventBus()
                .addListener((FMLClientSetupEvent event) -> EmiAcceleratorCompat.checkCache());
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
