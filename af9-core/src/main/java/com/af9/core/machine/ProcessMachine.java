package com.af9.core.machine;

import com.af9.core.common.IPowerGated;
import com.af9.core.machine.console.ConsoleWidget;
import com.af9.core.machine.console.ProcessConsoleWidget;
import com.af9.core.machine.part.CoolantHatchPartMachine;

import com.gregtechceu.gtceu.api.capability.recipe.FluidRecipeCapability;
import com.gregtechceu.gtceu.api.capability.recipe.ItemRecipeCapability;
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.gregtechceu.gtceu.api.machine.feature.multiblock.IMaintenanceMachine;
import com.gregtechceu.gtceu.api.machine.multiblock.WorkableElectricMultiblockMachine;
import com.gregtechceu.gtceu.api.recipe.GTRecipe;
import com.gregtechceu.gtceu.api.recipe.GTRecipeType;
import com.gregtechceu.gtceu.api.recipe.RecipeHelper;
import com.gregtechceu.gtceu.api.recipe.content.Content;
import com.gregtechceu.gtceu.api.recipe.ingredient.FluidIngredient;

import com.lowdragmc.lowdraglib.gui.widget.Widget;
import com.lowdragmc.lowdraglib.syncdata.field.ManagedFieldHolder;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.List;

/**
 * Base of the AF9 process multiblocks with a console (Supercooling Cryostat, Particle Accelerator): power gate,
 * mode selection, status, what the running recipe makes and the coolant in the coolant hatches. The console and the
 * Jade tooltip both read it from here.
 */
public abstract class ProcessMachine extends WorkableElectricMultiblockMachine implements IPowerGated {

    protected static final ManagedFieldHolder MANAGED_FIELD_HOLDER = new ManagedFieldHolder(ProcessMachine.class,
            WorkableElectricMultiblockMachine.MANAGED_FIELD_HOLDER);

    protected ProcessMachine(IMachineBlockEntity holder) {
        super(holder);
    }

    @Override
    public ManagedFieldHolder getFieldHolder() {
        return MANAGED_FIELD_HOLDER;
    }

    /** Console title key. */
    public abstract String titleKey();

    /** ARGB colour of a mode (recipe type index) in the console. */
    public abstract int modeColor(int index);

    /** Whether the machine takes its fluids from coolant hatches (and so can run dry). */
    public boolean usesCoolant() {
        return false;
    }

    /** EU/t below which the machine cannot run any of its recipes (shown as "no power"). */
    public long minimumEUt() {
        return 0;
    }

    /** Machine-specific console / Jade lines (server side; translated on the client). */
    public abstract List<Component> infoLines();

    @Override
    public Widget createUIWidget() {
        return ProcessConsoleWidget.create(this);
    }

    //////////////////////////////////////
    // ********* Machine state ********//
    //////////////////////////////////////

    @Override
    public long getAvailableEUt() {
        return energyContainer == null || !isFormed() ? 0 :
                energyContainer.getInputVoltage() * energyContainer.getInputAmperage();
    }

    public boolean hasMaintenanceProblems() {
        return getParts().stream().anyMatch(part -> part instanceof IMaintenanceMachine maintenance &&
                maintenance.hasMaintenanceProblems());
    }

    /** EU/t of the running (or last) recipe, 0 if there is none. */
    public long getNeededEUt() {
        GTRecipe recipe = getRecipeLogic().getLastRecipe();
        return recipe == null ? 0 : RecipeHelper.getRealEUt(recipe).getTotalEU();
    }

    public static String modeKey(GTRecipeType type) {
        return type.registryName.getNamespace() + "." + type.registryName.getPath();
    }

    /** Console / Jade status code (see {@link ConsoleWidget}). */
    public int getStatus() {
        if (!isFormed()) return ConsoleWidget.STATUS_OFFLINE;
        if (hasMaintenanceProblems()) return ConsoleWidget.STATUS_MAINTENANCE;
        var logic = getRecipeLogic();
        if (!logic.isWorkingEnabled()) return ConsoleWidget.STATUS_PAUSED;
        if (logic.isWorking()) return ConsoleWidget.STATUS_RUNNING;
        if (usesCoolant() && getCoolantAmount() <= 0) return ConsoleWidget.STATUS_NO_COOLANT;
        if (logic.isWaiting() || getAvailableEUt() < minimumEUt()) return ConsoleWidget.STATUS_NO_POWER;
        return ConsoleWidget.STATUS_IDLE;
    }

    /**
     * What the running recipe makes: "item:&lt;id&gt;" or "fluid:&lt;id&gt;" of its first output, "" when idle.
     */
    public String getCurrentOutput() {
        GTRecipe recipe = getRecipeLogic().getLastRecipe();
        if (recipe == null || !getRecipeLogic().isWorking()) return "";
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

    /** mB of coolant in all coolant hatches together. */
    public long getCoolantAmount() {
        long amount = 0;
        for (var part : getParts()) {
            if (part instanceof CoolantHatchPartMachine hatch) {
                for (int i = 0; i < hatch.tank.getTanks(); i++) amount += hatch.tank.getFluidInTank(i).getAmount();
            }
        }
        return amount;
    }

    /** The first coolant found in the coolant hatches, or empty. */
    public FluidStack getCoolant() {
        for (var part : getParts()) {
            if (part instanceof CoolantHatchPartMachine hatch) {
                for (int i = 0; i < hatch.tank.getTanks(); i++) {
                    FluidStack stack = hatch.tank.getFluidInTank(i);
                    if (!stack.isEmpty()) return stack;
                }
            }
        }
        return FluidStack.EMPTY;
    }
}
