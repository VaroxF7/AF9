package com.af9.core.recipes;

import net.minecraft.data.recipes.FinishedRecipe;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.Ingredient;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Edits of other mods' recipes that the KubeJS scripts made with {@code forEachRecipe}: they read a recipe's JSON and
 * add, change or replace it. They run on the raw recipe map at the head of the recipe load
 * ({@link Removals#transform}), after the removals.
 */
public final class Patches {

    private static final Logger LOGGER = LogManager.getLogger("AF9/Patches");

    private Patches() {}

    public static void register() {
        Removals.transform(Patches::chipUses);
        Removals.transform(Patches::circuitEtching);
        Removals.transform(Patches::neutroniumFusion);
        Removals.transform(Patches::chargedImplosion);
        Removals.transform(Patches::dissolverEmptyInputs);
        Removals.transform(Patches::greenhouseTrees);
    }

    /** What a builder saves goes into the raw map under its id. */
    private static java.util.function.Consumer<FinishedRecipe> into(Map<ResourceLocation, JsonElement> map) {
        return fr -> map.put(fr.getId(), fr.serializeRecipe());
    }

    private static JsonObject obj(JsonElement e) {
        return e != null && e.isJsonObject() ? e.getAsJsonObject() : null;
    }

    // ---- chip_uses.js: the chips join the recipes of GT's machines as one more input ----

    private static final String[][] CHIP_INPUTS = {
            { "gtceu:assembler/cover_machine_controller", "af9:mcu_chip" },
            { "gtceu:assembler/cover_activity_detector", "af9:mcu_chip" },
            { "gtceu:assembler/cover_item_detector", "af9:mcu_chip" },
            { "gtceu:assembler/cover_fluid_detector", "af9:mcu_chip" },
            { "gtceu:assembler/ev_large_miner", "2x af9:asic_chip" },
            { "gtceu:assembler/iv_large_miner", "4x af9:asic_chip" },
            { "gtceu:assembler/luv_large_miner", "8x af9:asic_chip" },
            { "gtceu:assembler/hv_fluid_drilling_rig", "2x af9:asic_chip" },
            { "gtceu:assembler/ev_fluid_drilling_rig", "4x af9:asic_chip" },
            { "gtceu:assembly_line/sensor_luv", "2x af9:vpu_chip" },
            { "gtceu:assembler/hpca_advanced_computation_component", "4x af9:tpu_chip" },
    };

    private static void chipUses(Map<ResourceLocation, JsonElement> map) {
        for (String[] target : CHIP_INPUTS) {
            JsonObject recipe = obj(map.get(new ResourceLocation(target[0])));
            if (recipe == null) continue;
            JsonObject content = Dsl.itemInputContent(target[1]);
            if (content == null) continue;
            JsonObject inputs = recipe.has("inputs") ? recipe.getAsJsonObject("inputs") : new JsonObject();
            recipe.add("inputs", inputs);
            JsonArray items = inputs.has("item") ? inputs.getAsJsonArray("item") : new JsonArray();
            inputs.add("item", items);
            items.add(content);
        }
    }

    // ---- circuit_etching.js: the persulfate board recipes get the new etchant ----

    private static void circuitEtching(Map<ResourceLocation, JsonElement> map) {
        for (String board : new String[] { "plastic", "advanced", "extreme", "elite", "wetware" }) {
            ResourceLocation id = new ResourceLocation("gtceu:chemical_reactor/" + board + "_circuit_board_persulfate");
            JsonObject recipe = obj(map.get(id));
            if (recipe == null) continue;
            recipe = recipe.deepCopy();
            boolean swapped = false;
            JsonObject inputs = obj(recipe.get("inputs"));
            if (inputs != null && inputs.has("fluid")) {
                for (JsonElement slot : inputs.getAsJsonArray("fluid")) {
                    JsonObject content = obj(obj(slot).get("content"));
                    if (content == null || !content.has("value")) continue;
                    for (JsonElement ve : content.getAsJsonArray("value")) {
                        JsonObject v = ve.getAsJsonObject();
                        String tag = v.has("tag") ? v.get("tag").getAsString() : null;
                        String fluid = v.has("fluid") ? v.get("fluid").getAsString() : null;
                        String which = tag != null ? tag : fluid;
                        if ("forge:sodium_persulfate".equals(which) || "gtceu:sodium_persulfate".equals(fluid)) {
                            v.remove("tag");
                            v.addProperty("fluid", "gtceu:redstone_tri_p_toluenesulfonate");
                            swapped = true;
                        }
                    }
                }
            }
            if (swapped) {
                map.remove(id);
                map.put(new ResourceLocation("af9:chemical_reactor/" + board + "_circuit_board_redstone_etching"), recipe);
            }
        }
    }

    // ---- planetary_satellite.js: neutronium comes from the satellite, so every fusion recipe for it goes ----

    private static void neutroniumFusion(Map<ResourceLocation, JsonElement> map) {
        for (String type : new String[] { "gtceu:fusion_reactor", "gtceu:mega_fusion_reactor" }) {
            for (ResourceLocation id : Removals.idsOfType(map, type)) {
                JsonObject outputs = obj(obj(map.get(id)).get("outputs"));
                if (outputs != null && outputs.toString().contains("neutronium")) map.remove(id);
            }
        }
    }

    // ---- electric_implosion.js: every other implosion recipe also exists as a charged one, without the explosive ----

    private static final int BURST_EU = 200000;
    private static final Pattern EXPLOSIVE = Pattern.compile("dynamite|industrial_tnt|minecraft:tnt|powderbarrel");

    private static void chargedImplosion(Map<ResourceLocation, JsonElement> map) {
        Set<String> seen = new HashSet<>();
        List<ResourceLocation> ids = Removals.idsOfType(map, "gtceu:implosion_compressor");
        ids.sort(ResourceLocation::compareTo);
        for (ResourceLocation id : ids) {
            if (id.toString().startsWith("af9:")) continue;
            JsonObject recipe = obj(map.get(id)).deepCopy();
            JsonObject inputs = obj(recipe.get("inputs"));
            JsonArray items = inputs != null && inputs.has("item") ? inputs.getAsJsonArray("item") : new JsonArray();
            JsonArray kept = new JsonArray();
            for (JsonElement c : items) if (!EXPLOSIVE.matcher(c.toString()).find()) kept.add(c);
            if (kept.size() == 0 || kept.size() == items.size()) continue;
            String key = kept + String.valueOf(recipe.get("outputs"));
            if (!seen.add(key)) continue;
            recipe.addProperty("type", "gtceu:charged_implosion");
            inputs.add("item", kept);
            JsonObject data = new JsonObject();
            data.addProperty("burst_eu", BURST_EU);
            recipe.add("data", data);
            map.put(new ResourceLocation("af9:implosion_charged/" + id.toString().replaceFirst(":", "/")), recipe);
        }
    }

    // ---- dissolver_empty_inputs.js: an Alchemistry dissolver recipe whose input matches no item breaks LAN joins ----

    private static void dissolverEmptyInputs(Map<ResourceLocation, JsonElement> map) {
        Map<String, Boolean> emptyBySpec = new java.util.HashMap<>();
        int removed = 0;
        for (ResourceLocation id : Removals.idsOfType(map, "alchemistry:dissolver")) {
            JsonObject input = obj(obj(map.get(id)).get("input"));
            JsonObject ingredient = input == null ? null : obj(input.get("ingredient"));
            if (ingredient == null) continue;
            String spec = ingredient.has("tag") ? "#" + ingredient.get("tag").getAsString()
                    : ingredient.has("item") ? ingredient.get("item").getAsString() : null;
            if (spec == null) continue;
            boolean empty = emptyBySpec.computeIfAbsent(spec, s -> {
                try {
                    return Ingredient.fromJson(ingredient).getItems().length == 0;
                } catch (RuntimeException e) {
                    return true;
                }
            });
            if (empty) {
                LOGGER.warn("dissolver recipe {} has an empty input ({}), removed", id, spec);
                map.remove(id);
                removed++;
            }
        }
        LOGGER.info("removed {} alchemistry dissolver recipes with an empty input", removed);
    }

    // ---- greenhouse_recipes.js: the Greenhouse grows the trees of Botany Pots (saplings, propagules) ----

    private static final Pattern TREES = Pattern.compile("sapling|propagule");
    private static final int MAX_SCALE = 1310720;
    private static final int MV = 2;
    private static final int UHV = 9;

    private static void greenhouseTrees(Map<ResourceLocation, JsonElement> map) {
        int treeRecipes = 0;
        int skipped = 0;
        List<ResourceLocation> ids = Removals.idsOfType(map, "botanypots:crop");
        ids.sort(ResourceLocation::compareTo);
        for (ResourceLocation id : ids) {
            JsonObject recipe = obj(map.get(id));
            JsonObject seed = obj(recipe.get("seed"));
            if (seed == null || !seed.has("item")) {
                skipped++;
                continue;
            }
            String input = seed.get("item").getAsString();
            if (!TREES.matcher(input).find()) {
                skipped++;
                continue;
            }
            String ns = id.getNamespace();
            if ((ns.contains("botanytrees") && input.contains("croptopia")) || ns.contains("supplementaries")) continue;
            String[] affix = input.split(":");
            double duration = recipe.get("growthTicks").getAsDouble();
            String base = "kubejs:gtceu/greenhouse/" + affix[0] + "/" + affix[1];
            Dsl.B normal = Dsl.gt(into(map), "greenhouse", base).EUt(MV).notConsumable(input)
                    .inputFluids(Dsl.fluid("minecraft:water", 1000)).duration((int) (duration / 4)).circuit(0);
            Dsl.B max = Dsl.gt(into(map), "greenhouse", base + "_max").EUt(UHV).notConsumable(input)
                    .inputFluids(Dsl.fluid("minecraft:water", 1000 * MAX_SCALE))
                    .duration((int) (duration / 4 * 1024 / 15)).circuit(5);
            for (JsonElement de : recipe.getAsJsonArray("drops")) {
                JsonObject drop = de.getAsJsonObject();
                String item = drop.getAsJsonObject("output").get("item").getAsString();
                int chance = (int) (drop.get("chance").getAsDouble() * 10000);
                if (drop.has("maxRolls")) {
                    int rolls = drop.get("maxRolls").getAsInt();
                    normal.chancedOutput(rolls + "x " + item, chance, chance);
                    max.itemOutputs((long) rolls * MAX_SCALE + "x " + item);
                } else {
                    normal.chancedOutput(item, chance, chance);
                    max.itemOutputs(MAX_SCALE + "x " + item);
                }
            }
            normal.save();
            max.save();
            treeRecipes++;
        }
        LOGGER.info("greenhouse: {} Botany Pots tree recipes added, {} crop recipes left out", treeRecipes, skipped);
    }
}
