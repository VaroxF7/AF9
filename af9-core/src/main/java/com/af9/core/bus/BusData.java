package com.af9.core.bus;

import com.af9.core.litho.LithoMode;
import com.af9.core.machine.LithoMachine;
import com.af9.core.machine.ParticleAcceleratorMachine;
import com.af9.core.machine.PhotolithographyLineMachine;

import com.gregtechceu.gtceu.api.capability.recipe.ItemRecipeCapability;
import com.gregtechceu.gtceu.api.machine.MetaMachine;
import com.gregtechceu.gtceu.api.machine.feature.IRecipeLogicMachine;
import com.gregtechceu.gtceu.api.machine.feature.multiblock.IMultiController;
import com.gregtechceu.gtceu.api.machine.multiblock.WorkableElectricMultiblockMachine;
import com.gregtechceu.gtceu.api.machine.multiblock.WorkableMultiblockMachine;
import com.gregtechceu.gtceu.api.machine.trait.RecipeLogic;
import com.gregtechceu.gtceu.api.misc.EnergyContainerList;
import com.gregtechceu.gtceu.api.recipe.GTRecipe;
import com.gregtechceu.gtceu.api.recipe.GTRecipeType;
import com.gregtechceu.gtceu.api.recipe.RecipeHelper;
import com.gregtechceu.gtceu.api.recipe.content.Content;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.List;

/**
 * What goes over the machine bus: the data a Bus Connector shares about its machine (the connector's share mask, a
 * module's show mask: the same bits) and the commands it takes, and the snapshot of one machine as the Machine Bus
 * Module keeps it (NBT, synced to the players by GT's Central Monitor).
 */
public final class BusData {

    // ---- data (share / show masks) ----
    public static final int STATUS = 1;
    public static final int PROGRESS = 1 << 1;
    public static final int RECIPE = 1 << 2;
    public static final int ENERGY = 1 << 3;
    public static final int PRODUCTION = 1 << 4;
    public static final int SETTINGS = 1 << 5;
    public static final int PROCESS = 1 << 6;
    public static final int ALL_DATA = (1 << 7) - 1;
    /** Lang keys af9.bus.data.&lt;key&gt;, in bit order. */
    public static final String[] DATA_KEYS = { "status", "progress", "recipe", "energy", "production", "settings",
            "process" };

    // ---- commands (accept mask) ----
    public static final int CMD_POWER = 1;
    public static final int CMD_BATCH = 1 << 1;
    public static final int CMD_MODE = 1 << 2;
    public static final int ALL_COMMANDS = (1 << 3) - 1;
    /** Lang keys af9.bus.command.&lt;key&gt;, in bit order. */
    public static final String[] COMMAND_KEYS = { "power", "batch", "mode" };

    // ---- snapshot keys ----
    public static final String POS = "p", LABEL = "n", NAME_KEY = "k", SHARED = "pub", ACCEPTS = "acc",
            FORMED = "f", STATUS_ID = "s", ENABLED = "on", PROGRESS_TICKS = "pr", MAX_PROGRESS = "mx",
            RECIPE_KEY = "rt", MODE = "mode", MODE_COLOR = "mc", MODES = "mn", PRODUCT = "out", STORED = "eu",
            CAPACITY = "ec", EUT = "et", RUNS = "runs", PRINTED = "pt", BROKEN = "bk", BATCH = "b", VERSION = "v",
            MAX_VERSION = "vm", SPEED = "sp", CLEANLINESS = "cl", VACUUM = "vs", BREAK = "bc", BEAM_GEV = "gev";

    /** Machine status, as {@link RecipeLogic.Status} (the ordinal) plus "not formed". */
    public static final int STATE_IDLE = 0, STATE_WORKING = 1, STATE_WAITING = 2, STATE_SUSPEND = 3,
            STATE_UNFORMED = 4;

    private BusData() {}

    /** The machine behind a connector: what its share mask lets out. */
    public static CompoundTag snapshot(BusConnectorPartMachine connector) {
        CompoundTag tag = new CompoundTag();
        int shared = connector.getSharedData();
        tag.putLong(POS, connector.getPos().asLong());
        tag.putString(LABEL, connector.getLabel());
        tag.putInt(SHARED, shared);
        tag.putInt(ACCEPTS, connector.getAcceptedCommands());
        IMultiController controller = connector.getMachineController();
        tag.putBoolean(FORMED, controller != null && controller.isFormed());
        if (controller == null) return tag;
        MetaMachine machine = controller.self();
        tag.putString(NAME_KEY, machine.getDefinition().getDescriptionId());
        RecipeLogic logic = controller instanceof IRecipeLogicMachine rlm ? rlm.getRecipeLogic() : null;
        // what the command buttons need, whatever is shared
        tag.putBoolean(ENABLED, logic == null || logic.isWorkingEnabled());
        if (controller instanceof IRecipeLogicMachine rlm) tag.putInt(MODES, rlm.getRecipeTypes().length);

        if ((shared & STATUS) != 0) {
            tag.putInt(STATUS_ID, !controller.isFormed() ? STATE_UNFORMED :
                    logic == null ? STATE_IDLE : logic.getStatus().ordinal());
        }
        if ((shared & PROGRESS) != 0 && logic != null) {
            tag.putInt(PROGRESS_TICKS, logic.getProgress());
            tag.putInt(MAX_PROGRESS, logic.getMaxProgress());
        }
        if ((shared & RECIPE) != 0 && controller instanceof IRecipeLogicMachine rlm) {
            GTRecipeType type = rlm.getRecipeType();
            if (type != null) tag.putString(RECIPE_KEY, type.registryName.toLanguageKey());
            if (machine instanceof LithoMachine litho) {
                LithoMode mode = litho.getActiveMode();
                tag.putString(MODE, mode.id);
                tag.putInt(MODE_COLOR, mode.argb);
            }
            ResourceLocation product = logic != null ? product(logic) : null;
            if (product != null) tag.putString(PRODUCT, product.toString());
        }
        if ((shared & ENERGY) != 0 && machine instanceof WorkableElectricMultiblockMachine electric) {
            EnergyContainerList energy = electric.getEnergyContainer();
            if (energy != null) {
                tag.putLong(STORED, energy.getEnergyStored());
                tag.putLong(CAPACITY, energy.getEnergyCapacity());
            }
            GTRecipe running = logic != null && logic.isActive() ? logic.getLastRecipe() : null;
            tag.putLong(EUT, running == null ? 0 : RecipeHelper.getRealEUt(running).getTotalEU());
        }
        if ((shared & PRODUCTION) != 0) {
            tag.putLong(RUNS, connector.getRuns());
            if (machine instanceof LithoMachine litho) {
                tag.putLong(PRINTED, litho.getPrinted());
                tag.putLong(BROKEN, litho.getBroken());
            }
        }
        if ((shared & SETTINGS) != 0) {
            tag.putBoolean(BATCH, controller.isBatchEnabled());
            if (machine instanceof PhotolithographyLineMachine line) {
                tag.putInt(VERSION, line.getVersion());
                tag.putInt(MAX_VERSION, line.getSpec().maxVersion());
                LithoMode mode = line.getActiveMode();
                tag.putDouble(SPEED, LithoMode.speedFactor(line.surplusFor(mode)));
            }
        }
        if ((shared & PROCESS) != 0) {
            if (machine instanceof LithoMachine litho) {
                tag.putDouble(CLEANLINESS, litho.getCleanliness());
                tag.putInt(VACUUM, litho.getVacuumState());
                tag.putDouble(BREAK, litho.currentBreakChance(litho.getActiveMode()));
            } else if (machine instanceof ParticleAcceleratorMachine accelerator) {
                tag.putDouble(BEAM_GEV, accelerator.getBeamEnergyGeV());
            }
        }
        return tag;
    }

    /** Registry id of the first item the running recipe puts out, or null. */
    private static ResourceLocation product(RecipeLogic logic) {
        GTRecipe recipe = logic.getLastRecipe();
        if (recipe == null || !logic.isActive()) return null;
        for (Content content : recipe.outputs.getOrDefault(ItemRecipeCapability.CAP, List.of())) {
            if (content.content instanceof Ingredient ingredient && ingredient.getItems().length > 0) {
                return ForgeRegistries.ITEMS.getKey(ingredient.getItems()[0].getItem());
            }
        }
        return null;
    }

    /**
     * Runs a command on the connector's machine, if the connector takes it. Power and batch toggle; the mode steps
     * {@code step} (+1 / -1) through the modes the machine may run: a line's modes up to its built version, any
     * other machine's recipe types.
     *
     * @return whether anything changed
     */
    public static boolean command(BusConnectorPartMachine connector, int command, int step) {
        if ((connector.getAcceptedCommands() & command) == 0) return false;
        IMultiController controller = connector.getMachineController();
        if (controller == null || !controller.isFormed()) return false;
        MetaMachine machine = controller.self();
        switch (command) {
            case CMD_POWER -> {
                if (!(machine instanceof WorkableMultiblockMachine workable)) return false;
                workable.setWorkingEnabled(!workable.isWorkingEnabled());
                return true;
            }
            case CMD_BATCH -> {
                controller.setBatchEnabled(!controller.isBatchEnabled());
                return true;
            }
            case CMD_MODE -> {
                if (!(machine instanceof IRecipeLogicMachine rlm)) return false;
                int count = rlm.getRecipeTypes().length;
                if (count < 2) return false;
                int current = rlm.getActiveRecipeType();
                for (int i = 1; i < count; i++) {
                    int next = Math.floorMod(current + i * Integer.signum(step == 0 ? 1 : step), count);
                    if (selectMode(machine, next)) return true;
                }
                return false;
            }
            default -> {
                return false;
            }
        }
    }

    /**
     * Switches a machine to one of its recipe types, if it may run it ({@link #modeAllowed}); as GT's mode button:
     * switch, then let the recipe logic look again.
     *
     * @return whether the machine runs that type now
     */
    public static boolean selectMode(MetaMachine machine, int index) {
        if (!(machine instanceof IRecipeLogicMachine rlm) || index < 0 || index >= rlm.getRecipeTypes().length ||
                !modeAllowed(machine, index)) {
            return false;
        }
        if (rlm.getActiveRecipeType() == index) return true;
        rlm.setActiveRecipeType(index);
        rlm.getRecipeLogic().updateTickSubscription();
        return true;
    }

    /** A line may only switch to the modes its built version prints; other machines to any of their recipe types. */
    public static boolean modeAllowed(MetaMachine machine, int index) {
        if (machine instanceof PhotolithographyLineMachine line) {
            List<LithoMode> modes = line.getModes();
            return index < modes.size() && line.canPrint(modes.get(index));
        }
        return true;
    }
}
