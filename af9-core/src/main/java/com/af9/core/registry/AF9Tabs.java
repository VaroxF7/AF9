package com.af9.core.registry;

import com.af9.core.AF9Core;
import com.af9.core.compute.AF9Compute;
import com.af9.core.space.AF9Space;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * The AF9 creative tab: everything the pack adds, in one place. First the blocks, then the plain items in the order
 * they are registered in ({@link AF9Blocks}, {@link AF9Items}), the rack cards, the Oil Regolith, and after them
 * whatever else is registered under {@code af9:}, by id.
 */
public final class AF9Tabs {

    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB,
            AF9Core.MOD_ID);

    public static final RegistryObject<CreativeModeTab> AF9 = TABS.register("af9", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.af9"))
            .icon(() -> new ItemStack(AF9Blocks.EUV_LIGHT_SOURCE.get()))
            .displayItems((parameters, output) -> contents().forEach(output::accept))
            .build());

    private AF9Tabs() {}

    /** What the tab shows, every item once. */
    public static List<ItemStack> contents() {
        Set<Item> items = new LinkedHashSet<>();
        for (DeferredRegister<Item> register : List.of(AF9Blocks.ITEMS, AF9Items.ITEMS, AF9Compute.ITEMS,
                AF9Space.ITEMS)) {
            for (RegistryObject<Item> entry : register.getEntries()) items.add(entry.get());
        }
        // anything registered somewhere else: sorted, so the tab does not depend on a world's registry order
        List<ResourceLocation> ids = new ArrayList<>(ForgeRegistries.ITEMS.getKeys());
        ids.removeIf(id -> !id.getNamespace().equals(AF9Core.MOD_ID));
        ids.sort(null);
        for (ResourceLocation id : ids) items.add(ForgeRegistries.ITEMS.getValue(id));
        items.remove(Items.AIR);
        List<ItemStack> stacks = new ArrayList<>(items.size());
        for (Item item : items) stacks.add(new ItemStack(item));
        return stacks;
    }

    public static void register(IEventBus modBus) {
        TABS.register(modBus);
    }
}
