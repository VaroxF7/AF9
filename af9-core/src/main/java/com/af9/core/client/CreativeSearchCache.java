package com.af9.core.client;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.af9.core.AF9Core;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * The creative search tree needs the tooltip of every creative stack (about 98,800), which took 22 s on the render
 * thread at the first search. After the player joins a world this works through the search tab's stacks a few
 * milliseconds per client tick and keeps each tooltip as plain text; when vanilla then builds the tree it finds the
 * lines here (see CreativeSearchTooltipMixin) and a stack not cached yet is simply computed as before.
 */
@Mod.EventBusSubscriber(modid = AF9Core.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class CreativeSearchCache {

    private static final long BUDGET_NANOS = 6_000_000L;

    private record Key(Item item, CompoundTag tag) {
    }

    private static final Map<Key, List<String>> CACHE = new HashMap<>();
    private static List<ItemStack> queue = null;
    private static int next = 0;
    private static boolean done = false;
    private static boolean computing = false;

    private CreativeSearchCache() {
    }

    /** Cached tooltip lines of the stack (plain text, formatting stripped) or null. */
    public static List<Component> lookup(ItemStack stack) {
        if (computing || CACHE.isEmpty()) {
            return null;
        }
        List<String> lines = CACHE.get(new Key(stack.getItem(), stack.getTag()));
        if (lines == null) {
            return null;
        }
        List<Component> out = new ArrayList<>(lines.size());
        for (String s : lines) {
            out.add(Component.literal(s));
        }
        return out;
    }

    public static boolean isComputing() {
        return computing;
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END || done) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) {
            return;
        }
        try {
            if (queue == null) {
                Collection<ItemStack> all = CreativeModeTabs.searchTab().getSearchTabDisplayItems();
                if (all == null || all.isEmpty()) {
                    return;
                }
                queue = new ArrayList<>(all);
                next = 0;
            }
            long end = System.nanoTime() + BUDGET_NANOS;
            computing = true;
            try {
                while (next < queue.size() && System.nanoTime() < end) {
                    ItemStack stack = queue.get(next++);
                    try {
                        Key key = new Key(stack.getItem(), stack.getTag());
                        if (CACHE.containsKey(key)) {
                            continue;
                        }
                        List<String> lines = new ArrayList<>();
                        for (Component c : stack.getTooltipLines(null, TooltipFlag.Default.NORMAL.asCreative())) {
                            String s = ChatFormatting.stripFormatting(c.getString());
                            s = s == null ? "" : s.trim();
                            if (!s.isEmpty()) {
                                lines.add(s);
                            }
                        }
                        CACHE.put(key, lines);
                    } catch (Throwable ignored) {
                        // a stack whose tooltip fails is computed (and fails) at the tree build as before
                    }
                }
            } finally {
                computing = false;
            }
            if (next >= queue.size()) {
                done = true;
                queue = null;
                AF9Core.LOGGER.info("Creative search: cached the tooltips of {} stacks", CACHE.size());
            }
        } catch (Throwable t) {
            done = true;
            queue = null;
            AF9Core.LOGGER.warn("Creative search cache stopped: {}", Objects.toString(t));
        }
    }
}
