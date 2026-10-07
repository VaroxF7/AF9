package com.af9.core.machine;

import com.af9.core.AF9Core;
import com.af9.core.machine.console.ProcessConsoleWidget;
import com.af9.core.staged.StagedRecipeData;
import com.af9.core.staged.StagedRecipeLogic;
import com.af9.core.staged.StagedRecipes;

import com.gregtechceu.gtceu.api.capability.recipe.FluidRecipeCapability;
import com.gregtechceu.gtceu.api.capability.recipe.ItemRecipeCapability;
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.gregtechceu.gtceu.api.machine.MachineDefinition;
import com.gregtechceu.gtceu.api.recipe.GTRecipe;
import com.gregtechceu.gtceu.api.recipe.GTRecipeType;
import com.gregtechceu.gtceu.api.recipe.content.Content;
import com.gregtechceu.gtceu.api.recipe.ingredient.FluidIngredient;
import com.gregtechceu.gtceu.api.registry.GTRegistries;

import com.lowdragmc.lowdraglib.gui.texture.ColorBorderTexture;
import com.lowdragmc.lowdraglib.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib.gui.widget.ButtonWidget;
import com.lowdragmc.lowdraglib.gui.widget.Widget;
import com.lowdragmc.lowdraglib.gui.widget.WidgetGroup;
import com.lowdragmc.lowdraglib.syncdata.field.ManagedFieldHolder;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.List;

/**
 * The Staged Assembly: a multiblock that builds its recipes in stages ({@code gtceu:staged_assembly}, defined in
 * {@code kubejs/startup_scripts/gtceu/staged_assembly.js}). One recipe carries every stage's inputs; the machine
 * runs the stages one after the other and only accepts exactly the running stage's inputs, so each stage is fed by
 * hand (or by automation watching the step). Behaviour: {@link StagedRecipeLogic}.
 * <p>
 * The console shows the stage ("Step 2/4"), what the waiting stage takes (counts only, so no language is baked in),
 * the final product and the finished crafts; the cancel button aborts the running craft. Jade renders the same lines
 * through the process branch of the AF9 provider, and the product icon is always the final output.
 */
public class StagedAssemblyMachine extends ProcessMachine {

    protected static final ManagedFieldHolder MANAGED_FIELD_HOLDER = new ManagedFieldHolder(
            StagedAssemblyMachine.class, ProcessMachine.MANAGED_FIELD_HOLDER);

    /** The staged recipe type (gtceu namespace). */
    public static final String RECIPE_TYPE = "staged_assembly";

    /** The invisible cancel button in the console's left column, under the power readout. */
    private static final int CANCEL_X = 6;
    private static final int CANCEL_Y = 68;
    private static final int CANCEL_W = 96;
    private static final int CANCEL_H = 12;

    public StagedAssemblyMachine(IMachineBlockEntity holder) {
        super(holder);
    }

    @Override
    public ManagedFieldHolder getFieldHolder() {
        return MANAGED_FIELD_HOLDER;
    }

    @Override
    protected com.gregtechceu.gtceu.api.machine.trait.RecipeLogic createRecipeLogic(Object... args) {
        // called from the super constructor: must not touch this class's fields
        return new StagedRecipeLogic(this);
    }

    @Override
    public String titleKey() {
        return "af9.staged.console.title";
    }

    @Override
    public int modeColor(int index) {
        return 0xFF60A5FA;
    }

    /** Aborts the running staged craft (the console button); already-consumed inputs are not refunded. */
    public void cancelStagedCraft() {
        if (getRecipeLogic() instanceof StagedRecipeLogic staged) staged.cancelStagedCraft();
    }

    /**
     * Common setup: the staged type names its stage count on its EMI / JEI pages, and the controller is its icon
     * where GT left none.
     */
    @SuppressWarnings("removal") // new ResourceLocation(ns, path) is the only constructor on 1.20.1
    public static void registerRecipeInfo() {
        GTRecipeType type = GTRegistries.RECIPE_TYPES.get(new ResourceLocation("gtceu", RECIPE_TYPE));
        MachineDefinition definition = GTRegistries.MACHINES.get(new ResourceLocation("gtceu", RECIPE_TYPE));
        if (type == null) {
            AF9Core.LOGGER.warn("Recipe type gtceu:{} not found - is the AF9 KubeJS startup script loaded?",
                    RECIPE_TYPE);
            return;
        }
        // rendered as plain labels, so the text must not contain '%'
        type.addDataInfo(data -> {
            int stages = StagedRecipeData.read(data).steps().size();
            if (stages == 0) return "";
            return Component.translatable("af9.recipe.staged.stages", stages).getString();
        });
        if (type.getIconSupplier() == null && definition != null) type.setIconSupplier(definition::asStack);
    }

    //////////////////////////////////////
    // *********** Screen **********//
    //////////////////////////////////////

    @Override
    public Widget createUIWidget() {
        var group = (WidgetGroup) ProcessConsoleWidget.create(this);
        // invisible until hovered (the fab reset button works the same way); no-op while idle
        var cancel = new ButtonWidget(CANCEL_X, CANCEL_Y, CANCEL_W, CANCEL_H, IGuiTexture.EMPTY,
                click -> {
                    if (!click.isRemote) cancelStagedCraft();
                });
        cancel.setHoverTexture(new ColorBorderTexture(1, 0xFFFFFFFF));
        cancel.setHoverTooltips(Component.translatable("af9.staged.console.cancel"),
                Component.translatable("af9.staged.console.cancel_tooltip"));
        group.addWidget(cancel);
        return group;
    }

    /** Stage, waiting inputs, final product and finished crafts; empty while no craft is loaded. */
    @Override
    public List<Component> infoLines() {
        List<Component> lines = new ArrayList<>();
        if (!(getRecipeLogic() instanceof StagedRecipeLogic staged) || !staged.hasStagedCraft()) return lines;
        List<GTRecipe> steps = staged.getStagedSteps();
        int index = Math.max(0, Math.min(steps.size() - 1, staged.getStageIndex()));
        lines.add(Component.translatable("af9.staged.console.step", index + 1, steps.size()));
        boolean working = getRecipeLogic().isWorking();
        GTRecipe waiting = working ? (index + 1 < steps.size() ? steps.get(index + 1) : null)
                : steps.get(index);
        if (waiting == null) {
            lines.add(Component.translatable("af9.staged.console.last"));
        } else {
            int items = waiting.getInputContents(ItemRecipeCapability.CAP).size();
            int fluids = waiting.getInputContents(FluidRecipeCapability.CAP).size();
            lines.add(Component.translatable("af9.staged.console.next", items, fluids));
        }
        Component product = firstOutputName(steps.get(steps.size() - 1));
        lines.add(Component.translatable("af9.staged.console.final",
                product == null ? Component.literal("-") : product));
        lines.add(Component.translatable("af9.staged.console.done", staged.getCraftsCompleted()));
        return lines;
    }

    /** While a staged craft is loaded the product is its final output, whatever stage is running. */
    @Override
    public String getCurrentOutput() {
        if (getRecipeLogic() instanceof StagedRecipeLogic staged && staged.hasStagedCraft()) {
            List<GTRecipe> steps = staged.getStagedSteps();
            String output = outputId(steps.get(steps.size() - 1));
            if (!output.isEmpty()) return output;
        }
        return super.getCurrentOutput();
    }

    /** "item:&lt;id&gt;" or "fluid:&lt;id&gt;" of a recipe's first output, "" when it has none. */
    private static String outputId(GTRecipe recipe) {
        for (Content content : recipe.outputs.getOrDefault(ItemRecipeCapability.CAP, List.of())) {
            if (content.content instanceof Ingredient ingredient && ingredient.getItems().length > 0) {
                ResourceLocation id = ForgeRegistries.ITEMS.getKey(ingredient.getItems()[0].getItem());
                if (id != null) return "item:" + id;
            }
        }
        for (Content content : recipe.outputs.getOrDefault(FluidRecipeCapability.CAP, List.of())) {
            if (content.content instanceof FluidIngredient ingredient && ingredient.getStacks().length > 0) {
                ResourceLocation id = ForgeRegistries.FLUIDS.getKey(ingredient.getStacks()[0].getFluid());
                if (id != null) return "fluid:" + id;
            }
        }
        return "";
    }

    /** Display name of a recipe's first output, for the console and Jade lines. */
    private static Component firstOutputName(GTRecipe recipe) {
        for (Content content : recipe.outputs.getOrDefault(ItemRecipeCapability.CAP, List.of())) {
            if (content.content instanceof Ingredient ingredient && ingredient.getItems().length > 0) {
                ItemStack stack = ingredient.getItems()[0];
                if (!stack.isEmpty()) return stack.getHoverName();
            }
        }
        for (Content content : recipe.outputs.getOrDefault(FluidRecipeCapability.CAP, List.of())) {
            if (content.content instanceof FluidIngredient ingredient && ingredient.getStacks().length > 0) {
                FluidStack stack = ingredient.getStacks()[0];
                if (!stack.isEmpty()) return stack.getFluid().getFluidType().getDescription();
            }
        }
        return null;
    }
}
