package com.af9.core.recipes;

import net.minecraft.resources.ResourceLocation;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * What KubeJS's {@code event.remove({...})} did, in Java: recipes of other mods, of GregTech and of the base pack that
 * AF9 replaces. A filter is a set of conditions that must all hold (id, id pattern, mod, type, output, input), each
 * condition a list of alternatives. It runs at the head of the recipe manager's load
 * ({@link com.af9.core.mixin.RecipeManagerRemovalMixin}) after KubeJS's own recipe event and GregTech's recipe pack, and
 * never removes a recipe AF9 itself registered ({@link #protect}): a removal meant the old recipe, not the new one.
 */
public final class Removals {

    private static final Logger LOGGER = LogManager.getLogger("AF9/Removals");
    private static final List<Filter> FILTERS = new ArrayList<>();
    private static final Set<ResourceLocation> PROTECTED = new HashSet<>();

    private Removals() {}

    /** Recipe ids AF9 registered: a removal never touches them. */
    public static void protect(ResourceLocation id) {
        PROTECTED.add(id);
    }

    public static Filter filter() {
        return new Filter();
    }

    public static final class Filter {

        private List<String> ids;
        private List<Pattern> idPatterns;
        private List<String> mods;
        private List<String> types;
        private List<String> outputs;
        private List<String> inputs;

        public Filter id(String... v) {
            ids = List.of(v);
            return this;
        }

        /** A regular expression the whole id must match. */
        public Filter idRegex(String regex) {
            idPatterns = List.of(Pattern.compile(regex));
            return this;
        }

        public Filter mod(String... v) {
            mods = List.of(v);
            return this;
        }

        public Filter type(String... v) {
            types = List.of(v);
            return this;
        }

        public Filter output(String... v) {
            outputs = List.of(v);
            return this;
        }

        public Filter input(String... v) {
            inputs = List.of(v);
            return this;
        }

        /** Registers the filter (it applies at the next recipe load). */
        public void remove() {
            if (ids == null && idPatterns == null && mods == null && types == null && outputs == null && inputs == null) {
                LOGGER.error("an empty removal filter was ignored");
                return;
            }
            FILTERS.add(this);
        }

        boolean matches(ResourceLocation id, JsonElement json) {
            if (PROTECTED.contains(id)) return false;
            if (ids != null && !ids.contains(id.toString())) return false;
            if (idPatterns != null && idPatterns.stream().noneMatch(p -> p.matcher(id.toString()).matches())) return false;
            if (mods != null && !mods.contains(id.getNamespace())) return false;
            JsonObject obj = json.isJsonObject() ? json.getAsJsonObject() : null;
            if (types != null) {
                if (obj == null || !obj.has("type") || !types.contains(obj.get("type").getAsString())) return false;
            }
            if (outputs != null && (obj == null || !mentions(obj, OUTPUT_KEYS, outputs))) return false;
            if (inputs != null && (obj == null || !mentions(obj, INPUT_KEYS, inputs))) return false;
            return true;
        }
    }

    private static final Set<String> OUTPUT_KEYS = Set.of("result", "results", "output", "outputs");
    private static final Set<String> INPUT_KEYS = Set.of("ingredient", "ingredients", "input", "inputs", "key", "base",
            "addition", "template");

    /** Does a subtree under one of the keys name one of the wanted items (as {@code item}, {@code id} or a bare string; tags as {@code #tag})? */
    private static boolean mentions(JsonObject recipe, Set<String> keys, List<String> wanted) {
        for (String key : keys) {
            if (recipe.has(key) && scan(recipe.get(key), wanted)) return true;
        }
        return false;
    }

    private static boolean scan(JsonElement e, List<String> wanted) {
        if (e == null || e.isJsonNull()) return false;
        if (e.isJsonPrimitive()) {
            JsonPrimitive p = e.getAsJsonPrimitive();
            return p.isString() && wanted.contains(p.getAsString());
        }
        if (e.isJsonArray()) {
            for (JsonElement c : (JsonArray) e) if (scan(c, wanted)) return true;
            return false;
        }
        for (Map.Entry<String, JsonElement> en : e.getAsJsonObject().entrySet()) {
            String k = en.getKey();
            JsonElement v = en.getValue();
            if (v.isJsonPrimitive() && v.getAsJsonPrimitive().isString()) {
                String s = v.getAsString();
                if ((k.equals("item") || k.equals("id")) && wanted.contains(s)) return true;
                if (k.equals("tag") && wanted.contains("#" + s)) return true;
            } else if (scan(v, wanted)) {
                return true;
            }
        }
        return false;
    }

    /** Removes every matching recipe from the recipe manager's raw map (called from the mixin). */
    public static void apply(Map<ResourceLocation, JsonElement> recipes) {
        if (FILTERS.isEmpty()) return;
        int removed = 0;
        int[] perFilter = new int[FILTERS.size()];
        Iterator<Map.Entry<ResourceLocation, JsonElement>> it = recipes.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<ResourceLocation, JsonElement> en = it.next();
            for (int i = 0; i < FILTERS.size(); i++) {
                if (FILTERS.get(i).matches(en.getKey(), en.getValue())) {
                    it.remove();
                    removed++;
                    perFilter[i]++;
                    break;
                }
            }
        }
        LOGGER.info("AF9 removed {} recipes with {} filters ({} filters matched nothing)", removed, FILTERS.size(),
                java.util.Arrays.stream(perFilter).filter(n -> n == 0).count());
    }
}
