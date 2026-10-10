package com.af9.core.compat.powah.client;

import java.util.ArrayList;
import java.util.List;

import com.af9.core.AF9Core;
import com.af9.core.compat.powah.OrbCounts;
import com.af9.core.compat.powah.OrbMk2;

import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import owmii.powah.block.energizing.EnergizingRecipe;

/**
 * The Energizing Orb Mk2's own JEI page for Powah's energizing recipes: every input slot shows its count (the stack of 4
 * rods is four rods), the molds (what a craft keeps) stand apart in their own slots with a red NC, and the energy and
 * the product are shown with their numbers. Powah's page cannot show these (one item per slot).
 */
@SuppressWarnings({"rawtypes", "unchecked"})
public final class OrbMk2JeiCategory implements IRecipeCategory<Recipe> {

    public static final RecipeType<Recipe> TYPE = new RecipeType<>(new ResourceLocation(AF9Core.MOD_ID, "orb_mk2"), Recipe.class);

    private static final int WIDTH = 150;
    private static final int HEIGHT = 56;
    private static final int RED = 0xFFCC2222;

    private final IDrawable background;
    private final IDrawable icon;
    private final IDrawable arrow;

    public OrbMk2JeiCategory(IGuiHelper helper) {
        this.background = helper.createBlankDrawable(WIDTH, HEIGHT);
        this.icon = helper.createDrawableItemStack(new ItemStack(OrbMk2.BLOCK.get()));
        this.arrow = helper.getRecipeArrow();
    }

    @Override
    public RecipeType<Recipe> getRecipeType() {
        return TYPE;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("block.af9.energizing_orb_mk2");
    }

    @Override
    public IDrawable getBackground() {
        return background;
    }

    @Override
    public IDrawable getIcon() {
        return icon;
    }

    @Override
    public ResourceLocation getRegistryName(Recipe recipe) {
        return recipe.getId();
    }

    private static List<ItemStack> counted(Ingredient ingredient, int count) {
        List<ItemStack> out = new ArrayList<>();
        for (ItemStack stack : ingredient.getItems()) {
            ItemStack copy = stack.copy();
            copy.setCount(Math.max(1, Math.min(count, Math.max(1, copy.getMaxStackSize()))));
            out.add(copy);
        }
        return out;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, Recipe recipe, IFocusGroup focuses) {
        List<Ingredient> ingredients = recipe.getIngredients();
        int[] counts = ((OrbCounts) recipe).af9Counts();
        boolean[] nc = ((OrbCounts) recipe).af9Nc();
        int input = 0;
        int mold = 0;
        for (int i = 0; i < ingredients.size(); i++) {
            int count = i < counts.length ? Math.max(1, counts[i]) : 1;
            boolean keep = i < nc.length && nc[i];
            int x;
            int y;
            if (keep) {
                x = 1;
                y = 1 + 28 * mold++;
            } else {
                x = 31 + 18 * (input % 3);
                y = 1 + 18 * (input / 3);
                input++;
            }
            var slot = builder.addSlot(keep ? RecipeIngredientRole.CATALYST : RecipeIngredientRole.INPUT, x, y)
                    .addItemStacks(counted(ingredients.get(i), count));
            if (keep) {
                slot.addTooltipCallback((view, tooltip) ->
                        tooltip.add(Component.translatable("af9.orb_mk2.nc").withStyle(ChatFormatting.RED)));
            }
        }
        ItemStack result = recipe.getResultItem(RegistryAccess.EMPTY);
        builder.addSlot(RecipeIngredientRole.OUTPUT, 129, 10).addItemStack(result);
    }

    @Override
    public void draw(Recipe recipe, IRecipeSlotsView slots, GuiGraphics graphics, double mouseX, double mouseY) {
        var font = Minecraft.getInstance().font;
        boolean[] nc = ((OrbCounts) recipe).af9Nc();
        int mold = 0;
        for (boolean keep : nc) {
            if (!keep) continue;
            int y = 1 + 28 * mold++;
            graphics.drawString(font, Component.translatable("af9.orb_mk2.nc_short"), 4, y + 19, RED, false);
        }
        arrow.draw(graphics, 98, 9);
        long energy = ((EnergizingRecipe) recipe).getEnergy();
        graphics.drawString(font, Component.translatable("af9.orb_mk2.jei_energy", String.format("%,d", energy)), 31, 42,
                0xFF444444, false);
    }

    /** The orb recipes among the recipes of the manager (the mixin makes Powah's energizing recipes {@link OrbCounts}). */
    public static List<Recipe> recipes(Iterable<Recipe<?>> all) {
        List<Recipe> out = new ArrayList<>();
        for (Recipe<?> recipe : all) {
            if (recipe instanceof OrbCounts) out.add(recipe);
        }
        return out;
    }
}
