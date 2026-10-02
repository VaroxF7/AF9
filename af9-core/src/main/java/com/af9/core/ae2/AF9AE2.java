package com.af9.core.ae2;

import com.af9.core.AF9Config;
import com.af9.core.AF9Core;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import appeng.api.networking.GridServices;

/**
 * AE2: an ME network with an ME Controller needs computation for its channels ({@link MEComputationService}, the cap
 * applied by {@code PathingCalculationMixin}), brought in by the ME Computation Link. Only set up when AE2 is loaded.
 * Spec: docs/computation.md §3.
 */
public final class AF9AE2 {

    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS,
            AF9Core.MOD_ID);
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, AF9Core.MOD_ID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(
            ForgeRegistries.BLOCK_ENTITY_TYPES, AF9Core.MOD_ID);

    public static final RegistryObject<Block> LINK = BLOCKS.register("me_computation_link",
            () -> new MEComputationLinkBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.METAL)
                    .strength(2.2f, 11f)
                    .sound(SoundType.METAL)));
    public static final RegistryObject<Item> LINK_ITEM = ITEMS.register("me_computation_link",
            () -> new BlockItem(LINK.get(), new Item.Properties()));
    @SuppressWarnings("DataFlowIssue") // no data fixer type, as every mod's
    public static final RegistryObject<BlockEntityType<MEComputationLinkBlockEntity>> LINK_ENTITY =
            BLOCK_ENTITIES.register("me_computation_link",
                    () -> BlockEntityType.Builder.of(MEComputationLinkBlockEntity::new, LINK.get()).build(null));

    @SuppressWarnings("removal") // new ResourceLocation(ns, path) is the only constructor on 1.20.1
    private static final ResourceLocation ME_CONTROLLER = new ResourceLocation("ae2", "controller");

    private AF9AE2() {}

    /** From the mod's constructor (AE2's grid services must be registered while mods load). */
    public static void init(IEventBus modBus) {
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        BLOCK_ENTITIES.register(modBus);
        modBus.addListener(AF9AE2::fillCreativeTabs);
        GridServices.register(MEComputationService.class, MEComputationService.class);
        MinecraftForge.EVENT_BUS.addListener(AF9AE2::controllerTooltip);
    }

    private static void fillCreativeTabs(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.FUNCTIONAL_BLOCKS) event.accept(LINK_ITEM);
    }

    /** The ME Controller says it needs computation. */
    private static void controllerTooltip(ItemTooltipEvent event) {
        if (!AF9Config.ME_NEEDS_COMPUTATION.get() ||
                !ME_CONTROLLER.equals(ForgeRegistries.ITEMS.getKey(event.getItemStack().getItem()))) {
            return;
        }
        event.getToolTip().add(Component.translatable("af9.me_link.controller_tooltip",
                AF9Config.ME_CHANNELS_PER_CWUT.get()).withStyle(ChatFormatting.AQUA));
    }
}
