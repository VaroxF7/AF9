package com.af9.core.elevator;

import com.af9.core.AF9Core;

import com.gregtechceu.gtceu.api.GTCEuAPI;
import com.gregtechceu.gtceu.api.data.chemical.ChemicalHelper;
import com.gregtechceu.gtceu.api.data.chemical.material.Material;
import com.gregtechceu.gtceu.api.data.chemical.material.properties.PropertyKey;
import com.gregtechceu.gtceu.api.data.tag.TagPrefix;
import com.gregtechceu.gtceu.api.data.worldgen.GTOreDefinition;
import com.gregtechceu.gtceu.api.data.worldgen.WorldGeneratorUtils;
import com.gregtechceu.gtceu.api.registry.GTRegistries;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Every ore GregTech has and where the Space Elevator finds it (docs/space-elevator.md): the asteroids a Mining Drone
 * brings home are made from GT's ore veins, so an ore a modpack adds to GT is mined without anybody writing it down.
 * <p>
 * An ore is a material with GT's ore property. Its tier is the lowest tier of the world-gen layers of the veins that hold it:
 * <ol>
 * <li>stone, deepslate and the Moon ({@code af9_moon}): the Overworld and the Moon;</li>
 * <li>netherrack: the Nether;</li>
 * <li>end stone: the End;</li>
 * <li>the {@code af9_asteroid} layer: the Asteroid Field.</li>
 * </ol>
 * A layer GT does not know here counts as the End. An ore that no vein holds (veins of weight 0 count as no vein) is tier 4.
 * <p>
 * The veins are the server's ({@link GTRegistries#ORE_VEINS}). A client holds the copy GT syncs to it instead: the
 * methods that take the veins are for it (the recipe viewer page, {@link SpaceMiningRecipeUI}).
 */
public final class OreCatalog {

    public static final int TIERS = 4;

    /** Ore materials by name, filled lazily on the first lookup instead of scanning the registry per call. */
    private static final ConcurrentHashMap<String, Material> MATERIALS_BY_NAME = new ConcurrentHashMap<>();

    private OreCatalog() {}

    /** An ore material by name, null when GT has none; late registrations are picked up on the next miss. */
    private static Material materialByName(String name) {
        if (name == null) return null;
        Material hit = MATERIALS_BY_NAME.get(name);
        if (hit != null) return hit;
        for (Material material : GTCEuAPI.materialManager.getRegisteredMaterials()) {
            MATERIALS_BY_NAME.putIfAbsent(material.getName(), material);
        }
        return MATERIALS_BY_NAME.get(name);
    }

    /** The tier of a world-gen layer by its key. */
    public static int tierOfLayer(String key) {
        return switch (key) {
            case "stone", "deepslate", "af9_moon" -> 1;
            case "netherrack" -> 2;
            case "endstone" -> 3;
            case "af9_asteroid" -> 4;
            default -> 3;
        };
    }

    /** Ore material name -> tier, for every ore GT has now. */
    public static Map<String, Integer> tiers() {
        return tiers(GTRegistries.ORE_VEINS.entries());
    }

    /** The same, of the veins given. */
    public static Map<String, Integer> tiers(Iterable<Map.Entry<ResourceLocation, GTOreDefinition>> source) {
        Map<String, Integer> result = new TreeMap<>();
        for (Material material : GTCEuAPI.materialManager.getRegisteredMaterials()) {
            if (material.hasProperty(PropertyKey.ORE)) result.put(material.getName(), TIERS);
        }
        int veins = 0;
        for (Map.Entry<ResourceLocation, GTOreDefinition> entry : source) {
            GTOreDefinition vein = entry.getValue();
            if (vein.weight() <= 0 || vein.veinGenerator() == null) continue;
            veins++;
            Optional<String> key = WorldGeneratorUtils.getWorldGenLayerKey(vein.layer());
            int tier = key.map(OreCatalog::tierOfLayer).orElse(3);
            try {
                for (Material material : vein.veinGenerator().getAllMaterials()) {
                    result.computeIfPresent(material.getName(), (name, old) -> Math.min(old, tier));
                }
            } catch (RuntimeException exception) {
                // a generator that cannot say what it holds (it holds no material)
            }
        }
        if (veins == 0) {
            AF9Core.LOGGER.warn("Space Elevator: GT has no ore veins registered yet; every ore is tier {}", TIERS);
        }
        return result;
    }

    /** An ore vein of GT: its id, weight, tier (that of its layer) and the ore materials in it. */
    public record Vein(String id, int weight, int tier, List<String> materials) {}

    /** The ore veins GT has now (weight above 0) that hold at least one ore material. */
    public static List<Vein> veins() {
        return veins(GTRegistries.ORE_VEINS.entries());
    }

    /** The same, of the veins given. */
    public static List<Vein> veins(Iterable<Map.Entry<ResourceLocation, GTOreDefinition>> source) {
        return veins(source, tiers(source));
    }

    /** The same, with the ore tiers computed once by the caller and shared. */
    private static List<Vein> veins(Iterable<Map.Entry<ResourceLocation, GTOreDefinition>> source,
            Map<String, Integer> ores) {
        List<Vein> result = new ArrayList<>();
        for (Map.Entry<ResourceLocation, GTOreDefinition> entry : source) {
            GTOreDefinition vein = entry.getValue();
            if (vein.weight() <= 0 || vein.veinGenerator() == null) continue;
            int tier = WorldGeneratorUtils.getWorldGenLayerKey(vein.layer()).map(OreCatalog::tierOfLayer).orElse(3);
            List<String> materials = new ArrayList<>();
            try {
                for (Material material : vein.veinGenerator().getAllMaterials()) {
                    String name = material.getName();
                    if (ores.containsKey(name) && !materials.contains(name)) materials.add(name);
                }
            } catch (RuntimeException exception) {
                continue;
            }
            if (!materials.isEmpty()) result.add(new Vein(entry.getKey().toString(), vein.weight(), tier, materials));
        }
        return result;
    }

    /** The ores that no vein holds, by material name (alphabetical): the Space Elevator's exotic asteroid. */
    public static List<String> exotics() {
        return exotics(GTRegistries.ORE_VEINS.entries());
    }

    /** The same, of the veins given. */
    public static List<String> exotics(Iterable<Map.Entry<ResourceLocation, GTOreDefinition>> source) {
        return exotics(tiers(source));
    }

    /** The ores of tier {@link #TIERS} in tiers computed once by the caller. */
    private static List<String> exotics(Map<String, Integer> tiers) {
        List<String> names = new ArrayList<>();
        tiers.forEach((name, tier) -> {
            if (tier == TIERS) names.add(name);
        });
        return names;
    }

    /**
     * The ores a drone of a tier can bring home, by material name: those of the veins of its tier and below (in the
     * veins' order, a vein's main ore first), and for the best drone the exotic ones as well.
     */
    public static List<String> reach(int tier, Iterable<Map.Entry<ResourceLocation, GTOreDefinition>> source) {
        Map<String, Integer> oreTiers = tiers(source);
        Set<String> names = new LinkedHashSet<>();
        for (Vein vein : veins(source, oreTiers)) {
            if (vein.tier() <= tier) names.addAll(vein.materials());
        }
        if (tier >= TIERS) names.addAll(exotics(oreTiers));
        return new ArrayList<>(names);
    }

    /** The id of the exotic asteroid among the veins ({@link #veinIds}). */
    public static final String EXOTIC = "exotic";

    /**
     * The asteroids a drone of this tier first reaches, by vein id (alphabetical, so a programmed circuit's number keeps its
     * asteroid) and, for the best drone, the exotic one: one expedition recipe each (server_scripts/mods/gtceu/space_elevator.js).
     */
    public static List<String> veinIds(int tier) {
        return veinIds(tier, GTRegistries.ORE_VEINS.entries());
    }

    /** The same, of the veins given (the client's, for the recipe pages). */
    public static List<String> veinIds(int tier, Iterable<Map.Entry<ResourceLocation, GTOreDefinition>> source) {
        Map<String, Integer> oreTiers = tiers(source);
        List<String> ids = new ArrayList<>();
        for (Vein vein : veins(source, oreTiers)) {
            if (vein.tier() == tier && !ids.contains(vein.id())) ids.add(vein.id());
        }
        java.util.Collections.sort(ids);
        if (tier >= TIERS && !exotics(oreTiers).isEmpty()) ids.add(EXOTIC);
        return ids;
    }

    /** The ore materials of an asteroid: its vein's, or all the exotic ones. */
    public static List<String> veinMaterials(String id) {
        return veinMaterials(id, GTRegistries.ORE_VEINS.entries());
    }

    public static List<String> veinMaterials(String id, Iterable<Map.Entry<ResourceLocation, GTOreDefinition>> source) {
        if (EXOTIC.equals(id)) return exotics(source);
        for (Vein vein : veins(source)) {
            if (vein.id().equals(id)) return vein.materials();
        }
        return List.of();
    }

    /** The item ids of an asteroid's ores (the raw ore, else the crushed one), the main ore first. */
    public static List<String> veinOreItems(String id) {
        return veinOreItems(id, GTRegistries.ORE_VEINS.entries());
    }

    public static List<String> veinOreItems(String id, Iterable<Map.Entry<ResourceLocation, GTOreDefinition>> source) {
        List<String> ids = new ArrayList<>();
        for (String material : veinMaterials(id, source)) {
            ItemStack stack = ore(material, 1);
            if (stack == null) continue;
            String item = idOf(stack);
            if (!ids.contains(item)) ids.add(item);
        }
        return ids;
    }

    /**
     * The ores only a drone of this tier brings: those of the veins of exactly its tier (and for the best drone the exotic
     * ones). The recipe page of a tier lists these, so each ore is on the page of the drone that first reaches it.
     */
    public static List<String> only(int tier, Iterable<Map.Entry<ResourceLocation, GTOreDefinition>> source) {
        Map<String, Integer> oreTiers = tiers(source);
        Set<String> names = new LinkedHashSet<>();
        for (Vein vein : veins(source, oreTiers)) {
            if (vein.tier() == tier) names.addAll(vein.materials());
        }
        if (tier >= TIERS) names.addAll(exotics(oreTiers));
        return new ArrayList<>(names);
    }

    /**
     * The item an asteroid gives for an ore: its raw ore, else its crushed ore, or null when the ore has neither (then
     * there is nothing to mine).
     */
    public static ItemStack ore(String materialName, int count) {
        Material material = materialByName(materialName);
        if (material == null) return null;
        for (TagPrefix prefix : new TagPrefix[] { TagPrefix.rawOre, TagPrefix.crushed }) {
            ItemStack stack = ChemicalHelper.get(prefix, material);
            if (!stack.isEmpty()) {
                ItemStack result = stack.copy();
                result.setCount(count);
                return result;
            }
        }
        return null;
    }

    /** The registry id of an item stack (for the log). */
    static String idOf(ItemStack stack) {
        ResourceLocation key = ForgeRegistries.ITEMS.getKey(stack.getItem());
        return key == null ? "?" : key.toString();
    }
}
