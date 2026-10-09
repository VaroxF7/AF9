package com.af9.core.client;

import com.af9.core.AF9Core;

import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.List;

/**
 * Long machine tooltips (the multiblocks') in pages: {@value #PAGE_LINES} lines at a time under the item's name, a footer
 * with the page and how to turn it (hold shift and scroll). Applies to the items of AF9 and GregTech whose tooltip is
 * longer than a page.
 */
@Mod.EventBusSubscriber(modid = AF9Core.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class TooltipPager {

    /** Lines of a page, the item's name not counted. */
    public static final int PAGE_LINES = 8;
    /** How long after the last tooltip frame a scroll still turns the page of the item shown (ms). */
    private static final long HOVER_GRACE = 250;

    private static ResourceLocation shown;
    private static int page;
    private static int pages = 1;
    private static long lastFrame;

    private TooltipPager() {}

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onTooltip(ItemTooltipEvent event) {
        List<Component> tooltip = event.getToolTip();
        ResourceLocation id = ForgeRegistries.ITEMS.getKey(event.getItemStack().getItem());
        if (id == null || !(id.getNamespace().equals(AF9Core.MOD_ID) || id.getNamespace().equals("gtceu"))) return;
        int body = tooltip.size() - 1;
        if (body <= PAGE_LINES) return;
        if (!id.equals(shown)) {
            shown = id;
            page = 0;
        }
        pages = (body + PAGE_LINES - 1) / PAGE_LINES;
        page = Math.max(0, Math.min(page, pages - 1));
        lastFrame = Util.getMillis();
        List<Component> paged = new ArrayList<>();
        paged.add(tooltip.get(0));
        paged.addAll(tooltip.subList(1 + page * PAGE_LINES, Math.min(tooltip.size(), 1 + (page + 1) * PAGE_LINES)));
        paged.add(Component.translatable("af9.tooltip.page", page + 1, pages).withStyle(ChatFormatting.DARK_GRAY));
        tooltip.clear();
        tooltip.addAll(paged);
    }

    /** Shift + scroll over a paged tooltip turns the page (the scroll is not passed on). */
    @SubscribeEvent
    public static void onScroll(ScreenEvent.MouseScrolled.Pre event) {
        if (pages <= 1 || !Screen.hasShiftDown() || Util.getMillis() - lastFrame > HOVER_GRACE) return;
        page = Math.max(0, Math.min(pages - 1, page + (event.getScrollDelta() < 0 ? 1 : -1)));
        event.setCanceled(true);
    }
}
