package com.af9.core.recipes;

import com.af9.core.common.OxygenatedCondition;

import com.gregtechceu.gtceu.api.machine.multiblock.CleanroomType;
import com.gregtechceu.gtceu.api.recipe.ingredient.FluidIngredient;
import com.gregtechceu.gtceu.common.data.GTRecipeTypes;
import com.gregtechceu.gtceu.data.recipe.builder.GTRecipeBuilder;

import net.minecraft.core.registries.Registries;
import net.minecraft.data.recipes.FinishedRecipe;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The builder the generated recipe classes use ({@code recipes/gen}). It takes the same strings KubeJS did
 * ({@code "4x gtceu:iron_dust"}, {@code "#forge:ingots/iron"}, a fluid as id + amount) and applies them, in the order
 * the script gave them, to GregTech's {@link GTRecipeBuilder}. One place to fix when GregTech's builder changes.
 */
public final class Dsl {

    private static final Pattern COUNTED = Pattern.compile("^(\\d+)x\\s+(.+)$");

    private static final org.apache.logging.log4j.Logger LOGGER = org.apache.logging.log4j.LogManager.getLogger("AF9/Recipes");
    /** The first unknown item or fluid of the recipe being built; the recipe is skipped (and logged) instead of aborting every recipe. */
    private static String error;

    private static final String MISSING = "unknown item ";
    /** Recipes left out because an item is missing (a removed mod's), by that item. */
    private static final java.util.Map<String, java.util.List<String>> SKIPPED = new java.util.TreeMap<>();

    private Dsl() {}

    /** One summary of the recipes left out for missing items (instead of an error each); the recipe ids at debug level. */
    public static void logSkipped() {
        if (SKIPPED.isEmpty()) return;
        int recipes = SKIPPED.values().stream().mapToInt(java.util.List::size).sum();
        LOGGER.info("{} recipes left out, their items are not in this pack (a removed mod?): {}", recipes,
                SKIPPED.entrySet().stream().map(e -> e.getKey() + " (" + e.getValue().size() + ")").collect(java.util.stream.Collectors.joining(", ")));
        SKIPPED.forEach((item, ids) -> LOGGER.debug("missing {}: {}", item, ids));
        SKIPPED.clear();
    }

    /** A recipe of the GT recipe type {@code type}, id {@code af9:<name>} (or the given namespace). */
    public static B gt(Consumer<FinishedRecipe> out, String type, String id) {
        ResourceLocation rl = new ResourceLocation(id.contains(":") ? id : "af9:" + id);
        com.gregtechceu.gtceu.api.recipe.GTRecipeType t = GTRecipeTypes.get(type);
        if (t == null) throw new IllegalArgumentException("unknown recipe type " + type);
        // GregTech files the recipe under <type>/<path>; that is the id a removal filter would see
        Removals.protect(rl.withPath(t.registryName.getPath() + "/" + rl.getPath()));
        return new B(out, t.recipeBuilder(rl), rl.toString());
    }

    /** A recipe given as JSON (crafting table, smelting, other mods' types), fed into the same recipe pack. */
    public static void json(Consumer<FinishedRecipe> out, String id, String json) {
        ResourceLocation rl = new ResourceLocation(id);
        com.google.gson.JsonObject body = com.google.gson.JsonParser.parseString(json).getAsJsonObject();
        ResourceLocation type = new ResourceLocation(body.get("type").getAsString());
        net.minecraft.world.item.crafting.RecipeSerializer<?> serializer = ForgeRegistries.RECIPE_SERIALIZERS.getValue(type);
        if (serializer == null) {
            LOGGER.error("recipe {} skipped: unknown recipe type {}", id, type);
            return;
        }
        Removals.protect(rl);
        out.accept(new FinishedRecipe() {

            @Override
            public void serializeRecipeData(com.google.gson.JsonObject o) {
                body.entrySet().forEach(e -> o.add(e.getKey(), e.getValue()));
            }

            @Override
            public ResourceLocation getId() {
                return rl;
            }

            @Override
            public net.minecraft.world.item.crafting.RecipeSerializer<?> getType() {
                return serializer;
            }

            @Override
            public com.google.gson.JsonObject serializeAdvancement() {
                return null;
            }

            @Override
            public ResourceLocation getAdvancementId() {
                return null;
            }
        });
    }

    /** A recipe that threw while it was built: logged and skipped, the others still register. */
    public static void fail(String id, RuntimeException e) {
        error = null;
        LOGGER.error("recipe {} skipped: {}", id, e.toString());
    }

    public static FluidStack fluid(String id, int amount) {
        Fluid fluid = ForgeRegistries.FLUIDS.getValue(new ResourceLocation(id));
        if (fluid == null || fluid == net.minecraft.world.level.material.Fluids.EMPTY) {
            if (error == null) error = "unknown fluid " + id;
            return FluidStack.EMPTY;
        }
        return new FluidStack(fluid, amount);
    }

    /** The JSON of one item input (as GregTech serializes it), for adding an input to a recipe that already exists. */
    public static com.google.gson.JsonObject itemInputContent(String spec) {
        ItemStack st = stack(spec);
        if (st.isEmpty()) {
            error = null;
            return null;
        }
        GTRecipeBuilder tmp = GTRecipeTypes.ASSEMBLER_RECIPES.recipeBuilder(new ResourceLocation("af9", "tmp")).inputItems(st);
        com.google.gson.JsonObject json = new com.google.gson.JsonObject();
        tmp.build().serializeRecipeData(json);
        return json.getAsJsonObject("inputs").getAsJsonArray("item").get(0).getAsJsonObject();
    }

    /** A fluid tag ({@code #ns:path}) and an amount. */
    public static FluidIngredient fluidTag(String tag, int amount) {
        return FluidIngredient.of(TagKey.create(Registries.FLUID, new ResourceLocation(tag)), amount);
    }

    public static ItemStack stack(String spec) {
        Matcher m = COUNTED.matcher(spec);
        int count = 1;
        String id = spec;
        if (m.matches()) {
            count = Integer.parseInt(m.group(1));
            id = m.group(2);
        }
        String nbt = null;
        int brace = id.indexOf('{');
        if (brace > 0) {
            nbt = id.substring(brace);
            id = id.substring(0, brace);
        }
        Item item = ForgeRegistries.ITEMS.getValue(new ResourceLocation(id));
        if (item == null || item == net.minecraft.world.item.Items.AIR) {
            if (error == null) error = MISSING + id;
            return ItemStack.EMPTY;
        }
        ItemStack st = new ItemStack(item, count);
        if (nbt != null) {
            try {
                st.setTag(net.minecraft.nbt.TagParser.parseTag(nbt));
            } catch (com.mojang.brigadier.exceptions.CommandSyntaxException e) {
                throw new IllegalArgumentException("bad NBT in " + spec, e);
            }
        }
        return st;
    }

    /** A Universal Fluid Cell holding 1000 mB of a fluid, as an item spec (NBT in braces). */
    public static String cell(String fluid) {
        return "gtceu:universal_fluid_cell{Fluid:{Amount:1000,FluidName:\"" + fluid + "\"}}";
    }

    private static boolean isTag(String spec) {
        return spec.startsWith("#") || (COUNTED.matcher(spec).matches() && COUNTED.matcher(spec).replaceFirst("$2").startsWith("#"));
    }

    private static int tagCount(String spec) {
        Matcher m = COUNTED.matcher(spec);
        return m.matches() ? Integer.parseInt(m.group(1)) : 1;
    }

    private static TagKey<Item> tag(String spec) {
        Matcher m = COUNTED.matcher(spec);
        String id = m.matches() ? m.group(2) : spec;
        return TagKey.create(Registries.ITEM, new ResourceLocation(id.substring(1)));
    }

    public static final class B {

        private final Consumer<FinishedRecipe> out;
        private final GTRecipeBuilder b;
        private final String id;

        B(Consumer<FinishedRecipe> out, GTRecipeBuilder b, String id) {
            this.out = out;
            this.b = b;
            this.id = id;
        }

        public B itemInputs(String... specs) {
            for (String s : specs) {
                if (isTag(s)) {
                    b.inputItems(tag(s), tagCount(s));
                } else {
                    ItemStack st = stack(s);
                    if (!st.isEmpty()) b.inputItems(st);
                }
            }
            return this;
        }

        /** An input item that holds a given fluid: a cell with {@code Fluid.FluidName} and {@code Fluid.Amount} 1000, matched by NBT. */
        public B inputItemNbtPredicate(String spec, String fluid) {
            ItemStack st = stack(spec);
            if (st.isEmpty()) return this;
            b.inputItemNbtPredicate(st, new com.gregtechceu.gtceu.api.recipe.ingredient.nbtpredicate.AllNBTPredicate(java.util.List.of(
                    new com.gregtechceu.gtceu.api.recipe.ingredient.nbtpredicate.EqualsNBTPredicate("Fluid.FluidName",
                            net.minecraft.nbt.StringTag.valueOf(fluid)),
                    new com.gregtechceu.gtceu.api.recipe.ingredient.nbtpredicate.EqualsNBTPredicate("Fluid.Amount",
                            net.minecraft.nbt.IntTag.valueOf(1000)))));
            return this;
        }

        public B notConsumable(String spec) {
            if (isTag(spec)) b.notConsumable(Ingredient.of(tag(spec)));
            else {
                ItemStack st = stack(spec);
                if (!st.isEmpty()) b.notConsumable(st);
            }
            return this;
        }

        public B itemOutputs(String... specs) {
            for (String s : specs) {
                ItemStack st = stack(s);
                if (!st.isEmpty()) b.outputItems(st);
            }
            return this;
        }

        /** An output item rolled between min and max when the run ends (GT's ranged outputs). */
        public B itemOutputsRanged(String spec, int min, int max) {
            ItemStack st = stack(spec);
            if (!st.isEmpty()) b.outputItemsRanged(st, net.minecraft.util.valueproviders.UniformInt.of(min, max));
            return this;
        }

        public B inputFluids(Object... fluids) {
            for (Object f : fluids) {
                if (f instanceof FluidStack s) {
                    if (!s.isEmpty()) b.inputFluids(s);
                } else {
                    b.inputFluids((FluidIngredient) f);
                }
            }
            return this;
        }

        public B outputFluids(Object... fluids) {
            for (Object f : fluids) {
                if (f instanceof FluidStack s) {
                    if (!s.isEmpty()) b.outputFluids(s);
                } else {
                    b.outputFluids((FluidIngredient) f);
                }
            }
            return this;
        }

        public B notConsumableFluid(Object fluid) {
            if (fluid instanceof FluidStack s) {
                if (!s.isEmpty()) b.notConsumableFluid(s);
            } else {
                b.notConsumableFluid((FluidIngredient) fluid);
            }
            return this;
        }

        public B EUt(long eut, long amperage) {
            b.EUt(eut, amperage);
            return this;
        }

        public B fusionStartEU(long eu) {
            b.fusionStartEU(eu);
            return this;
        }

        public B dimension(String dimension) {
            b.dimension(new ResourceLocation(dimension));
            return this;
        }

        /** Research Station research; -1 leaves a value at the builder's default. */
        public B stationResearch(String stack, String researchId, long eut, long amperage, int cwut, int totalCwu) {
            b.stationResearch(r -> {
                r.researchStack(stack(stack));
                if (researchId != null) r.researchId(researchId);
                if (eut >= 0) r.EUt(eut, amperage);
                if (cwut >= 0) {
                    if (totalCwu >= 0) r.CWUt(cwut, totalCwu);
                    else r.CWUt(cwut);
                }
                return r;
            });
            return this;
        }

        /** Scanner research; -1 leaves a value at the builder's default. */
        public B scannerResearch(String stack, String researchId, long eut, long amperage, int duration) {
            b.scannerResearch(r -> {
                r.researchStack(stack(stack));
                if (researchId != null) r.researchId(researchId);
                if (eut >= 0) r.EUt(eut, amperage);
                if (duration >= 0) r.duration(duration);
                return r;
            });
            return this;
        }

        public B chancedOutput(String spec, int chance, int tierBoost) {
            ItemStack st = stack(spec);
            if (!st.isEmpty()) b.chancedOutput(st, chance, tierBoost);
            return this;
        }

        public B chancedInput(String spec, int chance, int tierBoost) {
            ItemStack st = stack(spec);
            if (!st.isEmpty()) b.chancedInput(st, chance, tierBoost);
            return this;
        }

        public B circuit(int n) {
            b.circuitMeta(n);
            return this;
        }

        public B duration(int ticks) {
            b.duration(ticks);
            return this;
        }

        public B EUt(long eut) {
            b.EUt(eut);
            return this;
        }

        public B CWUt(int cwut) {
            b.CWUt(cwut);
            return this;
        }

        public B cleanroom(String type) {
            b.cleanroom("sterile".equals(type) ? CleanroomType.STERILE_CLEANROOM : CleanroomType.CLEANROOM);
            return this;
        }

        public B blastFurnaceTemp(int kelvin) {
            b.blastFurnaceTemp(kelvin);
            return this;
        }

        public B oxygenated(boolean reverse, boolean oxygenated) {
            b.addCondition(new OxygenatedCondition(reverse, oxygenated));
            return this;
        }

        /**
         * The staged-assembly data: the first {@code globalItems} item inputs and {@code globalFluids} fluid inputs belong to every
         * stage, then each layer takes the next run of items and fluids. {@code layers} is (ticks, items, fluids) per layer.
         */
        public B addStaged(int globalItems, int globalFluids, int... layers) {
            int count = layers.length / 3;
            if (count < 1 || count > 7 || layers.length % 3 != 0) throw new IllegalArgumentException("staged " + id + ": needs 1-7 layers");
            net.minecraft.nbt.ListTag layerList = new net.minecraft.nbt.ListTag();
            int item = globalItems;
            int fluid = globalFluids;
            for (int i = 0; i < count; i++) {
                int ticks = layers[i * 3];
                int items = layers[i * 3 + 1];
                int fluids = layers[i * 3 + 2];
                if (items + fluids < 1 || items + fluids > 5) throw new IllegalArgumentException("staged " + id + ": layer " + (i + 1) + " has " + (items + fluids) + " inputs (needs 1-5)");
                net.minecraft.nbt.CompoundTag layer = new net.minecraft.nbt.CompoundTag();
                layer.putInt("duration", ticks);
                layer.put("items", range(item, items));
                layer.put("fluids", range(fluid, fluids));
                layerList.add(layer);
                item += items;
                fluid += fluids;
            }
            net.minecraft.nbt.CompoundTag tag = new net.minecraft.nbt.CompoundTag();
            tag.put("layers", layerList);
            tag.put("globalItems", range(0, globalItems));
            tag.put("globalFluids", range(0, globalFluids));
            b.addData("af9_staged", tag);
            return this;
        }

        private static net.minecraft.nbt.ListTag range(int from, int count) {
            net.minecraft.nbt.ListTag list = new net.minecraft.nbt.ListTag();
            for (int i = 0; i < count; i++) list.add(net.minecraft.nbt.IntTag.valueOf(from + i));
            return list;
        }

        public B addData(String key, int value) {
            b.addData(key, value);
            return this;
        }

        public B addData(String key, String value) {
            b.addData(key, value);
            return this;
        }

        public void save() {
            if (error != null) {
                if (error.startsWith(MISSING)) {
                    // an item of a mod that is not installed (or not in this pack): the recipe is left out, reported once in the summary
                    SKIPPED.computeIfAbsent(error.substring(MISSING.length()), k -> new java.util.ArrayList<>()).add(id);
                } else {
                    LOGGER.error("recipe {} skipped: {}", id, error);
                }
                error = null;
                return;
            }
            b.save(out);
        }
    }
}
