package com.af9.core.client;

import com.af9.core.AF9Core;
import com.af9.core.client.render.ModeFluidRender;
import com.af9.core.wafer.WaferContamination;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;

/**
 * Client side: the dynamic machine renders, and the tooltip of everything that contaminates in a player's inventory.
 */
public final class AF9Client {

    private AF9Client() {}

    /** Mod construction: the render types must exist before the machine models are built. */
    public static void init() {
        ModeFluidRender.register();
    }

    @Mod.EventBusSubscriber(modid = AF9Core.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
    public static final class ForgeBusEvents {

        @SubscribeEvent
        public static void onTooltip(ItemTooltipEvent event) {
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
