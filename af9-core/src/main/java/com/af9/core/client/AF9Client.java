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
import net.minecraftforge.client.event.RegisterColorHandlersEvent;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Client side of the lithography properties: the af9:litho_mode model predicate of the chips (per-mode textures in
 * kubejs/assets/gtceu/models/item/*), the wafer packages' ribbon in the colour of the mode that printed them, and the
 * package/chip tooltip.
 */
@SuppressWarnings("removal") // new ResourceLocation(ns, path) is the only constructor on 1.20.1
public final class AF9Client {

    /** Ribbon of a package without a mode (only seen in creative / EMI's plain entry). */
    private static final int PLAIN_RIBBON = 0xFFB0B0B0;

    private AF9Client() {}

    @Mod.EventBusSubscriber(modid = AF9Core.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static final class ModBusEvents {

        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event) {
            // ItemProperties is not thread-safe; register on the main thread
            event.enqueueWork(() -> {
                ResourceLocation predicate = new ResourceLocation(AF9Core.MOD_ID, "litho_mode");
                for (String path : LithoMode.CHIPS) {
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

        /** Layer 1 of a package model is its ribbon: tinted with the colour of the printing mode. */
        @SubscribeEvent
        public static void onItemColors(RegisterColorHandlersEvent.Item event) {
            for (String path : LithoMode.PACKAGES) {
                Item item = ForgeRegistries.ITEMS.getValue(new ResourceLocation("kubejs", path));
                if (item == null || item == Items.AIR) {
                    AF9Core.LOGGER.warn("Item kubejs:{} not found - is the AF9 KubeJS startup script loaded?", path);
                    continue;
                }
                event.register((stack, tintIndex) -> {
                    if (tintIndex != 1) return -1;
                    LithoMode mode = LithoMode.fromStack(stack);
                    return mode == null ? PLAIN_RIBBON : mode.argb;
                }, item);
            }
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
            // right below the item name, in this order
            List<Component> lines = new ArrayList<>();
            lines.add(Component.translatable("af9.litho.tooltip.process",
                    Component.translatable("af9.litho.mode." + mode.id).withStyle(mode.color))
                    .withStyle(ChatFormatting.GRAY));
            if (litho.contains(LithoMode.TAG_VERSION)) {
                lines.add(Component.translatable("af9.litho.tooltip.version",
                        Component.literal(Integer.toString(litho.getInt(LithoMode.TAG_VERSION)))
                                .withStyle(ChatFormatting.WHITE))
                        .withStyle(ChatFormatting.GRAY));
            }
            if (litho.contains(LithoMode.TAG_TRANSISTORS)) {
                int perDie = litho.getInt(LithoMode.TAG_TRANSISTORS);
                lines.add(Component.translatable("af9.litho.tooltip.transistors", number(perDie))
                        .withStyle(ChatFormatting.GRAY));
                if (litho.contains(LithoMode.TAG_DIES)) {
                    int dies = litho.getInt(LithoMode.TAG_DIES);
                    lines.add(Component.translatable("af9.litho.tooltip.wafer", number(dies),
                            number((long) perDie * dies)).withStyle(ChatFormatting.GRAY));
                }
            }
            List<Component> tooltip = event.getToolTip();
            tooltip.addAll(Math.min(1, tooltip.size()), lines);
        }

        private static Component number(long value) {
            return Component.literal(String.format(Locale.ROOT, "%,d", value)).withStyle(ChatFormatting.GREEN);
        }
    }
}
