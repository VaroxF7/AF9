package com.af9.core.compute;

import com.af9.core.AF9Core;

import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.EnumMap;
import java.util.Map;

/**
 * The computation arrays' cards (af9:&lt;tier&gt;_&lt;kind&gt;_card, {@link ComputeCard}). The arrays and the racks
 * are GT machines defined in KubeJS ({@code startup_scripts/gtceu/computation.js}). Spec: docs/machine-bus.md §9.
 */
public final class AF9Compute {

    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, AF9Core.MOD_ID);
    public static final Map<ComputeCard, RegistryObject<Item>> CARDS = new EnumMap<>(ComputeCard.class);

    static {
        for (ComputeCard card : ComputeCard.values()) {
            CARDS.put(card, ITEMS.register(card.id(), () -> new ComputeCardItem(card, new Item.Properties())));
        }
    }

    private AF9Compute() {}

    public static void register(IEventBus modBus) {
        ITEMS.register(modBus);
        modBus.addListener(AF9Compute::fillCreativeTabs);
    }

    private static void fillCreativeTabs(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() != CreativeModeTabs.INGREDIENTS) return;
        for (RegistryObject<Item> card : CARDS.values()) event.accept(card);
    }
}
