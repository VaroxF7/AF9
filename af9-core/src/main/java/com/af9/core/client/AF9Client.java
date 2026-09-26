package com.af9.core.client;

import com.af9.core.AF9Core;
import com.af9.core.client.render.LightRingRender;
import com.af9.core.client.render.ModeFluidRender;
import com.af9.core.wafer.WaferContamination;
import com.af9.core.wireless.WirelessLink;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;

/**
 * Client side: the dynamic machine renders, the tooltip of everything that contaminates in a player's inventory,
 * and the wireless link a data stick carries.
 */
public final class AF9Client {

    private AF9Client() {}

    /** Mod construction: the render types must exist before the machine models are built. */
    public static void init() {
        ModeFluidRender.register();
        LightRingRender.register();
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
