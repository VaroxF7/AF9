package com.af9.core.compat.powah;

import com.af9.core.AF9Core;

import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * The Energizing Orb Mk2: Powah's orb with a screen. A block, a block entity type and a menu of its own, registered only
 * with Powah loaded (the classes extend Powah's). Its item is registered by Powah itself: it makes an item for every
 * block of its kind in the registry.
 */
public final class OrbMk2 {

    public static final String ID = "energizing_orb_mk2";

    private static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, AF9Core.MOD_ID);
    private static final DeferredRegister<BlockEntityType<?>> TILES =
            DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, AF9Core.MOD_ID);
    private static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(ForgeRegistries.MENU_TYPES, AF9Core.MOD_ID);

    public static final RegistryObject<Block> BLOCK = BLOCKS.register(ID, OrbMk2Block::new);
    public static final RegistryObject<BlockEntityType<OrbMk2Tile>> TILE = TILES.register(ID,
            () -> BlockEntityType.Builder.of(OrbMk2Tile::new, BLOCK.get()).build(null));
    public static final RegistryObject<MenuType<OrbMk2Menu>> MENU = MENUS.register(ID,
            () -> IForgeMenuType.create(OrbMk2Menu::client));

    private OrbMk2() {}

    public static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
        TILES.register(modBus);
        MENUS.register(modBus);
    }

    /** Logs whether the orb's recipes take counts (the mixins apply when Powah's classes load). */
    public static void verify() {
        try {
            Class<?> recipe = Class.forName("owmii.powah.block.energizing.EnergizingRecipe");
            if (OrbCounts.class.isAssignableFrom(recipe)) {
                AF9Core.LOGGER.info("Powah's Energizing Orb takes counted ingredients");
            } else {
                AF9Core.LOGGER.warn("Powah's Energizing Orb recipes were not patched: counted ingredients count as one each");
            }
        } catch (ClassNotFoundException | LinkageError e) {
            AF9Core.LOGGER.warn("Powah's Energizing Orb class not found: {}", e.toString());
        }
    }
}
