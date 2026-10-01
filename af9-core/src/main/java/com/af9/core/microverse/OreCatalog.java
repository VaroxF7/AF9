package com.af9.core.microverse;

import com.af9.core.AF9Core;

import com.gregtechceu.gtceu.api.GTCEuAPI;
import com.gregtechceu.gtceu.api.data.chemical.ChemicalHelper;
import com.gregtechceu.gtceu.api.data.chemical.material.Material;
import com.gregtechceu.gtceu.api.data.chemical.material.properties.PropertyKey;
import com.gregtechceu.gtceu.api.data.tag.TagPrefix;
import com.gregtechceu.gtceu.api.data.worldgen.GTOreDefinition;
import com.gregtechceu.gtceu.api.data.worldgen.WorldGeneratorUtils;
import com.gregtechceu.gtceu.api.data.worldgen.generator.VeinGenerator;
import com.gregtechceu.gtceu.api.registry.GTRegistries;

import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

/**
 * Every ore GregTech has, and the microverse that grows it (the Microverse Projector, docs/microverse.md). The recipes
 * are made from this in {@code server_scripts/mods/gtceu/microverse.js}, so an ore a modpack adds to GT is farmable
 * without anybody writing it down.
 * <p>
 * An ore is a material with GT's ore property. Its microverse tier is the lowest tier of the world-gen layers of the
 * ore veins that hold it:
 * <ol>
 * <li>stone and deepslate: the Overworld;</li>
 * <li>netherrack: the Nether;</li>
 * <li>end stone: the End;</li>
 * <li>the {@code af9_asteroid} layer: the Asteroid Field.</li>
 * </ol>
 * A layer GT does not know here counts as the End. An ore that no vein holds (veins of weight 0, like the pitchblende
 * and uraninite ones the Asteroid Field replaces, count as no vein) is tier 4: its microverse is the one of the Field.
 */
public final class OreCatalog {

    public static final int TIERS = 4;

    private OreCatalog() {}

    /** The tier of a world-gen layer by its key. */
    static int tierOfLayer(String key) {
        return switch (key) {
            case "stone", "deepslate" -> 1;
            case "netherrack" -> 2;
            case "endstone" -> 3;
            case "af9_asteroid" -> 4;
            default -> 3;
        };
    }

    /** Ore material name -> microverse tier, for every ore GT has now. */
    static Map<String, Integer> tiers() {
        Map<String, Integer> result = new TreeMap<>();
        for (Material material : GTCEuAPI.materialManager.getRegisteredMaterials()) {
            if (material.hasProperty(PropertyKey.ORE)) result.put(material.getName(), TIERS);
        }
        int veins = 0;
        for (GTOreDefinition vein : GTRegistries.ORE_VEINS) {
            if (vein.weight() <= 0 || vein.veinGenerator() == null) continue;
            veins++;
            Optional<String> key = WorldGeneratorUtils.getWorldGenLayerKey(vein.layer());
            int tier = key.map(OreCatalog::tierOfLayer).orElse(3);
            VeinGenerator generator = vein.veinGenerator();
            try {
                for (Material material : generator.getAllMaterials()) {
                    result.computeIfPresent(material.getName(), (name, old) -> Math.min(old, tier));
                }
            } catch (RuntimeException exception) {
                // a generator that cannot say what it holds (it holds no material)
            }
        }
        if (veins == 0) {
            AF9Core.LOGGER.warn("Microverse: GT has no ore veins registered yet; every ore is tier {}", TIERS);
        }
        return result;
    }

    /** The ores of one microverse tier, by material name (alphabetical). */
    public static List<String> materials(int tier) {
        List<String> names = new ArrayList<>();
        Map<String, Integer> all = tiers();
        all.forEach((name, t) -> {
            if (t == tier) names.add(name);
        });
        AF9Core.LOGGER.info("Microverse: {} ores in tier {} ({} in all)", names.size(), tier, all.size());
        return names;
    }

    /**
     * The item a microverse grows for an ore: its raw ore, else its crushed ore (the item id), or "" when the ore has
     * neither (then there is nothing to grow).
     */
    public static String ore(String materialName) {
        Material material = GTCEuAPI.materialManager.getRegisteredMaterials().stream()
                .filter(candidate -> candidate.getName().equals(materialName)).findFirst().orElse(null);
        if (material == null) return "";
        for (TagPrefix prefix : new TagPrefix[] { TagPrefix.rawOre, TagPrefix.crushed }) {
            ItemStack stack = ChemicalHelper.get(prefix, material);
            if (!stack.isEmpty()) {
                var key = ForgeRegistries.ITEMS.getKey(stack.getItem());
                if (key != null) return key.toString();
            }
        }
        return "";
    }
}
