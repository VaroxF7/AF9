package com.af9.core.client;

import com.af9.core.AF9Core;
import com.af9.core.wafer.WaferContamination;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;

/**
 * Client side of the wafers: every wafer that contaminates in a player's inventory says so in its tooltip.
 */
public final class AF9Client {

    private AF9Client() {}

    @Mod.EventBusSubscriber(modid = AF9Core.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
    public static final class ForgeBusEvents {

        @SubscribeEvent
        public static void onTooltip(ItemTooltipEvent event) {
            if (!event.getItemStack().is(WaferContamination.ALL_WAFERS)) return;
            List<Component> tooltip = event.getToolTip();
            // right below the item name
            tooltip.add(Math.min(1, tooltip.size()),
                    Component.translatable("af9.wafer.tooltip.contaminates").withStyle(ChatFormatting.RED));
            tooltip.add(Math.min(2, tooltip.size()),
                    Component.translatable("af9.wafer.tooltip.protection").withStyle(ChatFormatting.GRAY));
        }
    }
}
