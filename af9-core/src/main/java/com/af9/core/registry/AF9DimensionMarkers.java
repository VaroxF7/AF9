package com.af9.core.registry;

import com.af9.core.AF9Core;

import com.gregtechceu.gtceu.api.data.DimensionMarker;
import com.gregtechceu.gtceu.api.registry.GTRegistries;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.ArrayList;
import java.util.List;

/**
 * The dimension markers: the cube "planets" GregTech shows in the dimension slot of a recipe page and of an ore vein's
 * page (JEI / EMI). GregTech has three of its own (Overworld, Nether, End) and shows a barrier for every other
 * dimension; these are AF9's, for its own dimensions and for the two of other mods its recipes and veins name (the
 * Mining Dimension, the Moon). They are icons and nothing else: items no recipe makes and no tab lists, as
 * GregTech's own are.
 * <p>
 * To add one: a line in the static block, its name ({@code item.af9.<id>}) in the lang files, and its faces and model
 * from {@code tools/textures/dimension_markers.py}.
 */
public final class AF9DimensionMarkers {

    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, AF9Core.MOD_ID);

    /** A marker: its dimension, the tier GregTech sorts and labels it by, its item. */
    private record Marker(ResourceLocation dimension, int tier, RegistryObject<Item> item) {}

    private static final List<Marker> MARKERS = new ArrayList<>();

    static {
        // the tier is the rocket's that gets there (data/af9/planets, Ad Astra's own for the Moon)
        marker("allthemodium:mining", "mining_dimension_marker", 0);
        marker("ad_astra:moon", "moon_marker", 1);
        marker("af9:asteroid_field", "asteroid_field_marker", 2);
        marker("af9:ceres", "ceres_marker", 2);
        marker("af9:zephyr", "zephyr_marker", 3);
        marker("af9:kronos", "kronos_marker", 4);
        marker("af9:helios", "helios_marker", 4);
    }

    private AF9DimensionMarkers() {}

    private static void marker(String dimension, String id, int tier) {
        MARKERS.add(new Marker(ResourceLocation.tryParse(dimension), tier,
                ITEMS.register(id, () -> new Item(new Item.Properties()))));
    }

    public static void register(IEventBus modBus) {
        ITEMS.register(modBus);
    }

    /** Whether an item is one of the markers (the creative tab leaves them out). */
    public static boolean isMarker(Item item) {
        for (Marker marker : MARKERS) {
            if (marker.item().isPresent() && marker.item().get() == item) return true;
        }
        return false;
    }

    /**
     * Binds the markers to their dimensions. GregTech fills its marker registry while it starts up and closes it
     * again; it opens only for GregTech itself, so this runs from {@link com.af9.core.AF9Addon#initializeAddon},
     * which GregTech calls right after its own markers, still in its own start-up. Where the registry does not
     * open, the dimensions keep GregTech's barrier: an icon is no reason to stop the game.
     */
    public static void bind() {
        var registry = GTRegistries.DIMENSION_MARKERS;
        boolean opened = false;
        try {
            registry.unfreeze();
            opened = true;
        } catch (IllegalStateException open) {
            // open already: GregTech has not closed it yet
        }
        try {
            for (Marker marker : MARKERS) {
                registry.registerOrOverride(marker.dimension(),
                        new DimensionMarker(marker.tier(), marker.item(), null));
            }
        } catch (IllegalStateException closed) {
            AF9Core.LOGGER.warn("GregTech's dimension markers are closed: AF9's dimensions keep the barrier icon",
                    closed);
        } finally {
            if (opened) {
                try {
                    registry.freeze();
                } catch (IllegalStateException frozen) {
                    // it never opened
                }
            }
        }
    }
}
