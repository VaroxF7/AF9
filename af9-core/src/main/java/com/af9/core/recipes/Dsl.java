package com.af9.core.recipes;

import com.af9.core.common.OxygenatedCondition;

import com.gregtechceu.gtceu.api.machine.multiblock.CleanroomType;
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

    private Dsl() {}

    /** A recipe of the GT recipe type {@code type}, id {@code af9:<name>} (or the given namespace). */
    public static B gt(Consumer<FinishedRecipe> out, String type, String id) {
        ResourceLocation rl = new ResourceLocation(id.contains(":") ? id : "af9:" + id);
        return new B(out, GTRecipeTypes.get(type).recipeBuilder(rl), rl.toString());
    }

    public static FluidStack fluid(String id, int amount) {
        Fluid fluid = ForgeRegistries.FLUIDS.getValue(new ResourceLocation(id));
        if (fluid == null || fluid == net.minecraft.world.level.material.Fluids.EMPTY) {
            if (error == null) error = "unknown fluid " + id;
            return FluidStack.EMPTY;
        }
        return new FluidStack(fluid, amount);
    }

    public static ItemStack stack(String spec) {
        Matcher m = COUNTED.matcher(spec);
        int count = 1;
        String id = spec;
        if (m.matches()) {
            count = Integer.parseInt(m.group(1));
            id = m.group(2);
        }
        Item item = ForgeRegistries.ITEMS.getValue(new ResourceLocation(id));
        if (item == null || item == net.minecraft.world.item.Items.AIR) {
            if (error == null) error = "unknown item " + id;
            return ItemStack.EMPTY;
        }
        return new ItemStack(item, count);
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

        public B inputFluids(FluidStack... fluids) {
            for (FluidStack f : fluids) if (!f.isEmpty()) b.inputFluids(f);
            return this;
        }

        public B outputFluids(FluidStack... fluids) {
            for (FluidStack f : fluids) if (!f.isEmpty()) b.outputFluids(f);
            return this;
        }

        public B notConsumableFluid(FluidStack fluid) {
            if (!fluid.isEmpty()) b.notConsumableFluid(fluid);
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
                LOGGER.error("recipe {} skipped: {}", id, error);
                error = null;
                return;
            }
            b.save(out);
        }
    }
}
