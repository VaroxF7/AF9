package com.af9.core.client;

import com.af9.core.AF9Core;
import com.af9.core.litho.LithoMode;

import net.minecraft.ChatFormatting;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.List;
import java.util.Locale;

/**
 * Client side of the lithography properties: the af9:litho_mode model predicate (picks the per-mode texture in
 * kubejs/assets/gtceu/models/item/*) and the wafer/chip tooltip.
 */
@SuppressWarnings("removal") // new ResourceLocation(ns, path) is the only constructor on 1.20.1
public final class AF9Client {

    /** Every item the Photolithography Line (or its cutter recipes) puts {@link LithoMode#TAG} on. */
    public static final List<String> LITHO_ITEMS = List.of(
            "ilc_wafer", "ram_wafer", "cpu_wafer", "ulpic_wafer", "lpic_wafer", "simple_soc_wafer",
            "ilc_chip", "ram_chip", "cpu_chip", "ulpic_chip", "lpic_chip", "simple_soc");

    private AF9Client() {}

    @Mod.EventBusSubscriber(modid = AF9Core.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static final class ModBusEvents {

        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event) {
            // ItemProperties is not thread-safe; register on the main thread
            event.enqueueWork(() -> {
                ResourceLocation predicate = new ResourceLocation(AF9Core.MOD_ID, "litho_mode");
                for (String path : LITHO_ITEMS) {
                    Item item = ForgeRegistries.ITEMS.getValue(new ResourceLocation("gtceu", path));
                    if (item == null || item == Items.AIR) {
                        AF9Core.LOGGER.warn("Item gtceu:{} not found, no lithography texture for it", path);
                        continue;
                    }
                    ItemProperties.register(item, predicate, (stack, level, entity, seed) -> {
                        LithoMode mode = LithoMode.fromStack(stack);
                        return mode == null ? 0F : mode.modelIndex();
                    });
                }
            });
        }
    }

    @Mod.EventBusSubscriber(modid = AF9Core.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
    public static final class ForgeBusEvents {

        @SubscribeEvent
        public static void onTooltip(ItemTooltipEvent event) {
            CompoundTag litho = LithoMode.getLithoTag(event.getItemStack());
            if (litho == null) return;
            LithoMode mode = LithoMode.fromNode(litho.getInt(LithoMode.TAG_NODE));
            if (mode == null) return;
            List<Component> tooltip = event.getToolTip();
            // right below the item name
            int at = Math.min(1, tooltip.size());
            tooltip.add(at, Component.translatable("af9.litho.tooltip.transistors",
                    Component.literal(String.format(Locale.ROOT, "%,d", litho.getInt(LithoMode.TAG_TRANSISTORS)))
                            .withStyle(ChatFormatting.GREEN))
                    .withStyle(ChatFormatting.GRAY));
            tooltip.add(at, Component.translatable("af9.litho.tooltip.process",
                    Component.translatable("af9.litho.mode." + mode.id).withStyle(mode.color))
                    .withStyle(ChatFormatting.GRAY));
        }
    }
}
