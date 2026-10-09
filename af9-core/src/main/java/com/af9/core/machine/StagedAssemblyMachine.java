package com.af9.core.machine;

import com.af9.core.AF9Core;
import com.af9.core.machine.console.ProcessConsoleWidget;
import com.af9.core.staged.StagedRecipeLogic;

import com.gregtechceu.gtceu.api.capability.recipe.FluidRecipeCapability;
import com.gregtechceu.gtceu.api.capability.recipe.ItemRecipeCapability;
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.gregtechceu.gtceu.api.machine.MachineDefinition;
import com.gregtechceu.gtceu.api.recipe.GTRecipe;
import com.gregtechceu.gtceu.api.recipe.GTRecipeType;
import com.gregtechceu.gtceu.api.recipe.content.Content;
import com.gregtechceu.gtceu.api.recipe.ingredient.FluidIngredient;
import com.gregtechceu.gtceu.api.recipe.ingredient.IntProviderFluidIngredient;
import com.gregtechceu.gtceu.api.recipe.ingredient.IntProviderIngredient;
import com.gregtechceu.gtceu.api.registry.GTRegistries;
import com.gregtechceu.gtceu.utils.FormattingUtil;

import com.lowdragmc.lowdraglib.gui.texture.ColorBorderTexture;
import com.lowdragmc.lowdraglib.gui.texture.GuiTextureGroup;
import com.lowdragmc.lowdraglib.gui.texture.TextTexture;
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
import java.util.Locale;

/**
 * The Staged Assembly: a multiblock that builds its recipes in stages ({@code gtceu:staged_assembly}, defined in
 * {@code kubejs/startup_scripts/gtceu/staged_assembly.js}). One recipe carries every stage's inputs; the machine
 * runs the stages one after the other and only accepts exactly the running stage's inputs, so each stage is fed by
 * hand (or by automation watching the step). Behaviour: {@link StagedRecipeLogic}.
 * <p>
 * The console says what Star Technology's layered machines say on their screen, in the same words: the current step,
 * the craft's total progress, the final step's outputs and the inputs of the step to feed next, each by name and
 * amount; its cancel button aborts the running craft. Jade names the step and the final outputs, as there, and the
 * product icon is always the final output.
 */
public class StagedAssemblyMachine extends ProcessMachine {

    protected static final ManagedFieldHolder MANAGED_FIELD_HOLDER = new ManagedFieldHolder(
            StagedAssemblyMachine.class, ProcessMachine.MANAGED_FIELD_HOLDER);

    /** The staged recipe type (gtceu namespace). */
    public static final String RECIPE_TYPE = "staged_assembly";

    /** The cancel button in the console's left column, under the power readout. */
    private static final int CANCEL_X = 6;
    private static final int CANCEL_Y = 68;
    private static final int CANCEL_W = 96;
    private static final int CANCEL_H = 12;

    /** Outputs and inputs the console lists at most (the recipes' own limits: 2 outputs, 5 inputs and the globals). */
    private static final int MAX_OUTPUT_LINES = 2;
    private static final int MAX_INPUT_LINES = 6;

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

    /** Common setup: the controller is the staged type's icon on its EMI / JEI pages where GT left none. */
    @SuppressWarnings("removal") // new ResourceLocation(ns, path) is the only constructor on 1.20.1
    public static void registerRecipeInfo() {
        GTRecipeType type = GTRegistries.RECIPE_TYPES.get(new ResourceLocation("gtceu", RECIPE_TYPE));
        MachineDefinition definition = GTRegistries.MACHINES.get(new ResourceLocation("gtceu", RECIPE_TYPE));
        if (type == null) {
            AF9Core.LOGGER.warn("Recipe type gtceu:{} not found - is the AF9 KubeJS startup script loaded?",
                    RECIPE_TYPE);
            return;
        }
        if (type.getIconSupplier() == null && definition != null) type.setIconSupplier(definition::asStack);
    }

    //////////////////////////////////////
    // *********** Screen **********//
    //////////////////////////////////////

    @Override
    public Widget createUIWidget() {
        var group = (WidgetGroup) ProcessConsoleWidget.create(this);
        // does nothing while idle
        var label = new TextTexture("af9.staged.console.cancel").setWidth(CANCEL_W);
        var cancel = new ButtonWidget(CANCEL_X, CANCEL_Y, CANCEL_W, CANCEL_H,
                new GuiTextureGroup(new ColorBorderTexture(1, 0xFF5A6472), label),
                click -> {
                    if (!click.isRemote) cancelStagedCraft();
                });
        cancel.setHoverTexture(new GuiTextureGroup(new ColorBorderTexture(1, 0xFFFFFFFF), label));
        cancel.setHoverTooltips(Component.translatable("af9.staged.console.cancel_tooltip"));
        group.addWidget(cancel);
        return group;
    }

    /** The step, the total progress, the final outputs and the next step's inputs. */
    @Override
    public int consoleLines() {
        return 4 + MAX_OUTPUT_LINES + MAX_INPUT_LINES;
    }

    /**
     * Current step, total progress, the final step's outputs and the inputs of the step to feed next; empty while
     * no craft is loaded.
     */
    @Override
    public List<Component> infoLines() {
        List<Component> lines = new ArrayList<>();
        if (!(getRecipeLogic() instanceof StagedRecipeLogic staged) || !staged.hasStagedCraft()) return lines;
        List<GTRecipe> steps = staged.getStagedSteps();
        int index = Math.max(0, Math.min(steps.size() - 1, staged.getStageIndex()));
        lines.add(Component.translatable("af9.staged.console.step", index + 1, steps.size()));

        int total = staged.getCraftDuration();
        int done = staged.getCraftProgress();
        lines.add(Component.translatable("af9.staged.console.progress",
                String.format(Locale.ROOT, "%.2f", done / 20F), String.format(Locale.ROOT, "%.2f", total / 20F),
                total == 0 ? 0 : (int) (done * 100L / total)));

        lines.add(Component.translatable("af9.staged.console.final"));
        addContents(lines, staged.getLastStep(), true, MAX_OUTPUT_LINES);

        GTRecipe next = staged.getNextStep();
        if (next != null) {
            lines.add(Component.translatable("af9.staged.console.next"));
            addContents(lines, next, false, MAX_INPUT_LINES);
        }
        return lines;
    }

    /** As Star Technology's Jade lines: the step, and the final step's outputs. */
    @Override
    public List<Component> jadeLines() {
        List<Component> lines = new ArrayList<>();
        if (!(getRecipeLogic() instanceof StagedRecipeLogic staged) || !staged.hasStagedCraft()) return lines;
        int count = staged.getStageCount();
        lines.add(Component.translatable("af9.staged.jade.step",
                Math.max(0, Math.min(count - 1, staged.getStageIndex())) + 1, count));
        lines.add(Component.translatable("af9.staged.console.final"));
        addContents(lines, staged.getLastStep(), true, MAX_OUTPUT_LINES);
        return lines;
    }

    /** A line per item and fluid of a recipe's outputs or inputs: its name and how much of it. */
    private static void addContents(List<Component> lines, GTRecipe recipe, boolean outputs, int most) {
        if (recipe == null) return;
        int limit = lines.size() + most;
        List<Content> items = outputs ? recipe.getOutputContents(ItemRecipeCapability.CAP) :
                recipe.getInputContents(ItemRecipeCapability.CAP);
        for (Content item : items) {
            if (lines.size() >= limit) return;
            ItemStack stack;
            if (item.content instanceof IntProviderIngredient provider) {
                stack = provider.getMaxSizeStack();
            } else {
                ItemStack[] stacks = ItemRecipeCapability.CAP.of(item.content).getItems();
                if (stacks.length == 0) continue;
                stack = stacks[0];
            }
            lines.add(Component.translatable("af9.staged.console.contents", stack.getHoverName(),
                    FormattingUtil.formatNumberReadable(stack.getCount())));
        }
        List<Content> fluids = outputs ? recipe.getOutputContents(FluidRecipeCapability.CAP) :
                recipe.getInputContents(FluidRecipeCapability.CAP);
        for (Content fluid : fluids) {
            if (lines.size() >= limit) return;
            FluidStack stack;
            if (fluid.content instanceof IntProviderFluidIngredient provider) {
                stack = provider.getMaxSizeStack();
            } else {
                FluidStack[] stacks = FluidRecipeCapability.CAP.of(fluid.content).getStacks();
                if (stacks.length == 0) continue;
                stack = stacks[0];
            }
            lines.add(Component.translatable("af9.staged.console.contents", stack.getDisplayName(),
                    FormattingUtil.formatBuckets(stack.getAmount())));
        }
    }

    /** While a staged craft is loaded the product is its final output, whatever stage is running. */
    @Override
    public String getCurrentOutput() {
        if (getRecipeLogic() instanceof StagedRecipeLogic staged && staged.hasStagedCraft()) {
            String output = outputId(staged.getLastStep());
            if (!output.isEmpty()) return output;
        }
        return super.getCurrentOutput();
    }

    /** "item:&lt;id&gt;" or "fluid:&lt;id&gt;" of a recipe's first output, "" when it has none. */
    private static String outputId(GTRecipe recipe) {
        if (recipe == null) return "";
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
}
