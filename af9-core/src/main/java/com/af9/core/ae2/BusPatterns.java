package com.af9.core.ae2;

import com.af9.core.bus.BusConnectorPartMachine;
import com.af9.core.bus.BusControllerMachine;
import com.af9.core.bus.BusData;
import com.af9.core.bus.BusNetwork;
import com.af9.core.bus.BusSupply;

import com.gregtechceu.gtceu.api.machine.feature.IRecipeLogicMachine;
import com.gregtechceu.gtceu.api.machine.feature.multiblock.IMultiController;
import com.gregtechceu.gtceu.api.machine.feature.multiblock.IMultiPart;
import com.gregtechceu.gtceu.api.recipe.GTRecipe;
import com.gregtechceu.gtceu.api.recipe.GTRecipeType;
import com.gregtechceu.gtceu.api.recipe.ingredient.FluidIngredient;
import com.gregtechceu.gtceu.api.recipe.ingredient.SizedIngredient;
import com.gregtechceu.gtceu.common.data.GTRecipeTypes;
import com.gregtechceu.gtceu.integration.ae2.machine.MEPatternBufferPartMachine;

import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.crafting.StrictNBTIngredient;
import net.minecraftforge.fluids.FluidStack;

import appeng.api.config.Actionable;
import appeng.api.crafting.IPatternDetails;
import appeng.api.crafting.PatternDetailsHelper;
import appeng.api.networking.IGrid;
import appeng.api.networking.security.IActionSource;
import appeng.api.stacks.AEFluidKey;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.GenericStack;
import appeng.api.storage.MEStorage;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * ME autocrafting through a Bus Controller (docs/machine-bus.md §5b). GT's ME Pattern Buffer in the controller's shell
 * holds the patterns: an ME crafting request pushes a pattern's ingredients into the buffer's slot for it, and once a
 * second ({@link #run}) the controller
 * <ol>
 * <li>returns the products of the ME crafts it sent: from the plain output buses and hatches of each machine, every
 * stack of an item or fluid its recipe makes goes into the buffer's ME network (as far as it takes it). A craft is done
 * once the machine idles and no longer holds a run of it; then the machine is free again;</li>
 * <li>sends on what the buffers hold: for each slot with ingredients, a machine of its network (on a bus that is not
 * overloaded, formed, not refusing the controller) with a recipe that makes the pattern's primary output out of what
 * the slot holds, and free: no ME craft, idle, its input buses empty; or already on this recipe with fewer than
 * {@link #MAX_QUEUED} runs out and no whole run waiting in it. It switches the machine's mode and puts one run into its
 * plain input bus and hatches, circuit set ({@link BusSupply#send}); as long as the slot holds runs and machines are
 * free.</li>
 * </ol>
 * The machine's connector keeps the craft ({@link BusConnectorPartMachine#getMeRecipe}); the controller's own supply
 * leaves the machine alone meanwhile. Only loaded with AE2.
 */
@SuppressWarnings("removal") // new ResourceLocation(ns, path) is the only constructor on 1.20.1
public final class BusPatterns {

    /** Runs of one ME craft a machine may hold at once: the running one and the next. */
    public static final int MAX_QUEUED = 2;
    /** Recipes looked through at most for one pattern and recipe type. */
    private static final int MAX_CANDIDATES = 64;

    /** The recipes of a type that make an output, per recipe manager (a new one after /reload). */
    private static final Map<RecipeManager, Map<String, List<GTRecipe>>> CANDIDATES = new WeakHashMap<>();

    private BusPatterns() {}

    /** One round for a controller, over the machines of its network. */
    public static void run(BusControllerMachine controller) {
        Level level = controller.getLevel();
        if (level == null) return;
        List<MEPatternBufferPartMachine> buffers = buffers(controller);
        MEPatternBufferPartMachine online = null;
        for (MEPatternBufferPartMachine buffer : buffers) {
            if (buffer.getGrid() != null) {
                online = buffer;
                break;
            }
        }
        List<BusConnectorPartMachine> machines = machines(controller);
        if (online != null) returnProducts(controller, level, online, machines);
        for (MEPatternBufferPartMachine buffer : buffers) sendOn(controller, level, buffer, machines);
    }

    /** The screen's line: the controller's pattern buffers and the crafts waiting in them. */
    public static void addText(BusControllerMachine controller, List<Component> text) {
        List<MEPatternBufferPartMachine> buffers = buffers(controller);
        if (buffers.isEmpty()) return;
        int waiting = 0;
        boolean online = false;
        for (MEPatternBufferPartMachine buffer : buffers) {
            online |= buffer.getGrid() != null;
            for (MEPatternBufferPartMachine.InternalSlot slot : buffer.getInternalInventory()) {
                if (!slot.isItemEmpty() || !slot.isFluidEmpty()) waiting++;
            }
        }
        text.add(Component.translatable(online ? "af9.bus.controller.me" : "af9.bus.controller.me_offline",
                buffers.size(), waiting).withStyle(online ? ChatFormatting.GRAY : ChatFormatting.RED));
    }

    private static List<MEPatternBufferPartMachine> buffers(BusControllerMachine controller) {
        List<MEPatternBufferPartMachine> buffers = new ArrayList<>();
        if (!controller.isFormed()) return buffers;
        for (IMultiPart part : controller.getParts()) {
            if (part instanceof MEPatternBufferPartMachine buffer) buffers.add(buffer);
        }
        return buffers;
    }

    /** The machines of the network on buses that are not overloaded, each once. */
    private static List<BusConnectorPartMachine> machines(BusControllerMachine controller) {
        List<BusConnectorPartMachine> machines = new ArrayList<>();
        for (BusNetwork.Bus bus : controller.getNetwork().buses()) {
            if (bus.overloaded()) continue;
            for (BusConnectorPartMachine connector : bus.connectors()) {
                if (!machines.contains(connector) && !connector.isInValid() &&
                        connector.getMachineController() instanceof IRecipeLogicMachine) {
                    machines.add(connector);
                }
            }
        }
        return machines;
    }

    //////////////////////////////////////
    // ********** Products back ********//
    //////////////////////////////////////

    private static void returnProducts(BusControllerMachine controller, Level level,
                                       MEPatternBufferPartMachine buffer, List<BusConnectorPartMachine> machines) {
        IGrid grid = buffer.getGrid();
        MEStorage storage = grid.getStorageService().getInventory();
        IActionSource source = buffer.getActionSource();
        long self = controller.getPos().asLong();
        List<BusControllerMachine> controllers = controller.getNetwork().controllers();
        for (BusConnectorPartMachine connector : machines) {
            if (!connector.hasMeCraft()) continue;
            // the controller that sent it returns it; one that is gone leaves it to this one
            if (connector.getMeController() != self) {
                boolean senderThere = controllers.stream()
                        .anyMatch(other -> other.getPos().asLong() == connector.getMeController());
                if (senderThere) continue;
                connector.setMeController(self);
            }
            GTRecipe recipe = recipe(level, connector.getMeRecipe());
            if (recipe == null) {
                connector.clearMeCraft();
                continue;
            }
            BusSupply.collectProducts(connector, recipe,
                    stack -> (int) storage.insert(AEItemKey.of(stack), stack.getCount(), Actionable.MODULATE, source),
                    fluid -> (int) storage.insert(AEFluidKey.of(fluid), fluid.getAmount(), Actionable.MODULATE,
                            source));
            IMultiController machine = connector.getMachineController();
            boolean idle = machine instanceof IRecipeLogicMachine rlm && rlm.getRecipeLogic().isIdle();
            if (idle && !BusSupply.machineHoldsRun(connector, recipe)) connector.clearMeCraft();
        }
    }

    //////////////////////////////////////
    // *********** Sending on **********//
    //////////////////////////////////////

    private static void sendOn(BusControllerMachine controller, Level level, MEPatternBufferPartMachine buffer,
                               List<BusConnectorPartMachine> machines) {
        MEPatternBufferPartMachine.InternalSlot[] slots = buffer.getInternalInventory();
        var patterns = buffer.getPatternInventory();
        for (int i = 0; i < slots.length && i < patterns.getSlots(); i++) {
            MEPatternBufferPartMachine.InternalSlot slot = slots[i];
            if (slot.isItemEmpty() && slot.isFluidEmpty()) continue;
            ItemStack patternStack = patterns.getStackInSlot(i);
            if (patternStack.isEmpty()) continue;
            IPatternDetails pattern = PatternDetailsHelper.decodePattern(patternStack, level);
            GenericStack output = pattern == null ? null : pattern.getPrimaryOutput();
            if (output == null) continue;
            SlotSource source = new SlotSource(slot);
            // as many runs as the slot holds and machines take
            boolean sent = true;
            while (sent && !(slot.isItemEmpty() && slot.isFluidEmpty())) {
                sent = sendOne(controller, level, source, output, machines);
            }
        }
    }

    /** One run to the first free machine that makes the output out of what the source holds. */
    private static boolean sendOne(BusControllerMachine controller, Level level, SlotSource source,
                                   GenericStack output, List<BusConnectorPartMachine> machines) {
        ItemStack wantedItem = output.what() instanceof AEItemKey item ? item.toStack() : ItemStack.EMPTY;
        FluidStack wantedFluid = output.what() instanceof AEFluidKey fluid ?
                fluid.toStack((int) Math.max(1, Math.min(Integer.MAX_VALUE, output.amount()))) : FluidStack.EMPTY;
        if (wantedItem.isEmpty() && wantedFluid.isEmpty()) return false;
        for (BusConnectorPartMachine connector : machines) {
            if (!connector.acceptsController()) continue;
            IMultiController target = connector.getMachineController();
            if (target == null || !target.isFormed() || !(target.self() instanceof IRecipeLogicMachine rlm)) continue;
            ResourceLocation busy = connector.getMeRecipe();
            if (busy == null && (!rlm.getRecipeLogic().isIdle() || !BusSupply.inputsEmpty(connector))) continue;
            if (busy != null && connector.getMeRuns() >= MAX_QUEUED) continue;
            for (GTRecipe recipe : recipesMaking(level, rlm, wantedItem, wantedFluid)) {
                if (busy != null) {
                    if (!busy.equals(recipe.getId()) || BusSupply.machineHoldsRun(connector, recipe)) continue;
                }
                if (!BusSupply.holdsRun(source, connector, recipe)) continue;
                if (BusSupply.send(source, connector, recipe) == null) {
                    connector.addMeRun(recipe.getId(), controller.getPos().asLong());
                    return true;
                }
            }
        }
        return false;
    }

    /** The recipes of the machine's allowed types that make the output (cached per type and output). */
    private static List<GTRecipe> recipesMaking(Level level, IRecipeLogicMachine rlm, ItemStack item,
                                                FluidStack fluid) {
        Map<String, List<GTRecipe>> cache = CANDIDATES.computeIfAbsent(level.getRecipeManager(),
                manager -> new HashMap<>());
        String outputKey = item.isEmpty() ? "fluid:" + BuiltInRegistries.FLUID.getKey(fluid.getFluid()) :
                "item:" + BuiltInRegistries.ITEM.getKey(item.getItem()) + (item.hasTag() ? item.getTag() : "");
        List<GTRecipe> found = new ArrayList<>();
        GTRecipeType[] types = rlm.getRecipeTypes();
        for (int i = 0; i < types.length; i++) {
            GTRecipeType type = types[i];
            if (type == GTRecipeTypes.DUMMY_RECIPES || !BusData.modeAllowed(rlm.self(), i)) continue;
            found.addAll(cache.computeIfAbsent(type.registryName + "|" + outputKey, key -> {
                List<GTRecipe> recipes = new ArrayList<>();
                for (GTRecipe recipe : level.getRecipeManager().getAllRecipesFor(type)) {
                    if (BusControllerMachine.makes(recipe, item, fluid)) {
                        recipes.add(recipe);
                        if (recipes.size() >= MAX_CANDIDATES) break;
                    }
                }
                return List.copyOf(recipes);
            }));
        }
        return found;
    }

    private static GTRecipe recipe(Level level, ResourceLocation id) {
        return id != null && level.getRecipeManager().byKey(id).orElse(null) instanceof GTRecipe recipe ? recipe :
                null;
    }

    /** A pattern buffer slot as the source of a run: what AE2 pushed into it. */
    private record SlotSource(MEPatternBufferPartMachine.InternalSlot slot) implements BusSupply.RunSource {

        @Override
        public List<ItemStack> items() {
            return slot.getItems().stream().map(ItemStack::copy).toList();
        }

        @Override
        public List<FluidStack> fluids() {
            return slot.getFluids().stream().map(FluidStack::copy).toList();
        }

        @Override
        public boolean take(List<ItemStack> items, List<FluidStack> fluids) {
            List<Ingredient> itemIngredients = new ArrayList<>();
            for (ItemStack stack : items) {
                itemIngredients.add(SizedIngredient.create(stack.hasTag() ? StrictNBTIngredient.of(stack) :
                        Ingredient.of(stack.getItem()), stack.getCount()));
            }
            List<FluidIngredient> fluidIngredients = new ArrayList<>();
            for (FluidStack fluid : fluids) fluidIngredients.add(FluidIngredient.of(fluid.copy()));
            if (!itemIngredients.isEmpty() && !empty(slot.handleItemInternal(new ArrayList<>(itemIngredients), true))) {
                return false;
            }
            if (!fluidIngredients.isEmpty() &&
                    !empty(slot.handleFluidInternal(new ArrayList<>(fluidIngredients), true))) {
                return false;
            }
            if (!itemIngredients.isEmpty()) slot.handleItemInternal(new ArrayList<>(itemIngredients), false);
            if (!fluidIngredients.isEmpty()) slot.handleFluidInternal(new ArrayList<>(fluidIngredients), false);
            return true;
        }

        private static boolean empty(List<?> left) {
            return left == null || left.isEmpty();
        }
    }
}
