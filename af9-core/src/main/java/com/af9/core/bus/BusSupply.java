package com.af9.core.bus;

import com.gregtechceu.gtceu.api.capability.recipe.FluidRecipeCapability;
import com.gregtechceu.gtceu.api.capability.recipe.IO;
import com.gregtechceu.gtceu.api.capability.recipe.IRecipeCapabilityHolder;
import com.gregtechceu.gtceu.api.capability.recipe.IRecipeHandler;
import com.gregtechceu.gtceu.api.capability.recipe.ItemRecipeCapability;
import com.gregtechceu.gtceu.api.machine.MetaMachine;
import com.gregtechceu.gtceu.api.machine.feature.IRecipeLogicMachine;
import com.gregtechceu.gtceu.api.machine.feature.multiblock.IMultiController;
import com.gregtechceu.gtceu.api.machine.trait.NotifiableFluidTank;
import com.gregtechceu.gtceu.api.machine.trait.NotifiableItemStackHandler;
import com.gregtechceu.gtceu.api.recipe.GTRecipe;
import com.gregtechceu.gtceu.api.recipe.content.Content;
import com.gregtechceu.gtceu.api.recipe.ingredient.FluidIngredient;
import com.gregtechceu.gtceu.api.recipe.ingredient.IntCircuitIngredient;
import com.gregtechceu.gtceu.api.recipe.ingredient.SizedIngredient;
import com.gregtechceu.gtceu.common.machine.multiblock.electric.AssemblyLineMachine;
import com.gregtechceu.gtceu.common.machine.multiblock.part.ItemBusPartMachine;
import com.gregtechceu.gtceu.config.ConfigHolder;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

/**
 * How a Bus Controller supplies a machine: one run of the machine's set recipe, taken from the controller's input
 * buses and hatches (plain or ME) and put into the machine's own input buses and hatches (plain ones, never an ME
 * bus), whenever the machine does not already hold a run. Not-consumed inputs (reticles, lenses, molds) go in once; a
 * programmed circuit is set in the receiving bus's circuit slot. An Assembly Line with ordered inputs gets its i-th
 * item in its i-th input bus, as GT checks it. The products stay in the machine.
 */
public final class BusSupply {

    private BusSupply() {}

    /**
     * One input of a recipe: an item or a fluid and how much (a not-consumed one, a reticle or a lens, only has to be
     * there); an item's place among the recipe's items (an Assembly Line's bus), -1 for a fluid.
     */
    private record Need(Ingredient item, FluidIngredient fluid, int amount, int index) {

        boolean isItem() {
            return item != null;
        }
    }

    /** Where a taken item goes: its ingredient's index, for an Assembly Line. */
    private record Taken(ItemStack stack, int index) {}

    /**
     * Supplies the connector's machine with one run of {@code recipe} if it needs one.
     *
     * @return what happened, for the controller's screen
     */
    public static Component supply(BusControllerMachine controller, BusConnectorPartMachine connector,
                                   GTRecipe recipe) {
        IMultiController target = connector.getMachineController();
        if (target == null || !target.isFormed()) return status("unformed", ChatFormatting.RED);
        MetaMachine machine = target.self();
        if (!(machine instanceof IRecipeLogicMachine rlm) || !(machine instanceof IRecipeCapabilityHolder holder)) {
            return status("no_recipes", ChatFormatting.RED);
        }
        int type = indexOf(rlm, recipe);
        if (type < 0) return status("wrong_machine", ChatFormatting.RED);
        if (!BusData.selectMode(machine, type)) return status("mode_locked", ChatFormatting.RED);

        List<Need> needs = needs(recipe);
        ItemStack circuit = circuit(recipe);
        List<NotifiableItemStackHandler> itemTargets = itemTargets(holder);
        List<NotifiableFluidTank> fluidTargets = fluidTargets(holder);
        boolean ordered = machine instanceof AssemblyLineMachine &&
                ConfigHolder.INSTANCE.machines.orderedAssemblyLineItems;

        // what the machine still lacks for one run
        List<Need> missing = new ArrayList<>();
        for (Need need : needs) {
            long have = need.isItem() ?
                    (ordered ? orderedCount(itemTargets, need.index, need) : count(itemTargets, need)) :
                    countFluid(fluidTargets, need);
            if (have < need.amount) missing.add(need);
        }
        if (missing.isEmpty()) {
            setCircuit(itemTargets, circuit);
            return status("stocked", ChatFormatting.GREEN);
        }

        // take them from the controller (simulated first)
        List<NotifiableItemStackHandler> itemSources = itemSources(controller);
        List<NotifiableFluidTank> fluidSources = fluidSources(controller);
        Map<NotifiableItemStackHandler, int[]> reserved = new IdentityHashMap<>();
        List<Taken> takenItems = new ArrayList<>();
        List<FluidStack> takenFluids = new ArrayList<>();
        MutableComponent lacking = null;
        for (Need need : missing) {
            int amount = need.amount;
            boolean found = need.isItem() ?
                    reserveItems(itemSources, reserved, need, amount, takenItems, need.index) :
                    reserveFluid(fluidSources, need, amount, takenFluids);
            if (!found) {
                Component name = need.isItem() ? displayName(need.item) : displayName(need.fluid);
                MutableComponent part = Component.literal(amount + "× ").append(name);
                lacking = lacking == null ? part : lacking.append(", ").append(part);
            }
        }
        if (lacking != null) {
            return Component.translatable("af9.bus.supply.lacking", lacking).withStyle(ChatFormatting.YELLOW);
        }

        // room in the machine?
        List<NotifiableItemStackHandler> itemPlan = planItems(itemTargets, takenItems, ordered);
        if (itemPlan == null) return status("no_room", ChatFormatting.YELLOW);
        List<NotifiableFluidTank> fluidPlan = planFluids(fluidTargets, takenFluids);
        if (fluidPlan == null) return status("no_room", ChatFormatting.YELLOW);

        // move them
        for (var entry : reserved.entrySet()) {
            int[] counts = entry.getValue();
            for (int slot = 0; slot < counts.length; slot++) {
                if (counts[slot] > 0) entry.getKey().extractItemInternal(slot, counts[slot], false);
            }
        }
        for (FluidStack fluid : takenFluids) drainFrom(fluidSources, fluid);
        for (int i = 0; i < takenItems.size(); i++) {
            ItemStack left = insert(itemPlan.get(i), takenItems.get(i).stack());
            if (!left.isEmpty()) giveBack(itemSources, left);
        }
        for (int i = 0; i < takenFluids.size(); i++) {
            fluidPlan.get(i).fillInternal(takenFluids.get(i), IFluidHandler.FluidAction.EXECUTE);
        }
        setCircuit(itemTargets, circuit);
        return status("supplied", ChatFormatting.AQUA);
    }

    private static Component status(String key, ChatFormatting color) {
        return Component.translatable("af9.bus.supply." + key).withStyle(color);
    }

    private static int indexOf(IRecipeLogicMachine rlm, GTRecipe recipe) {
        var types = rlm.getRecipeTypes();
        for (int i = 0; i < types.length; i++) {
            if (types[i] == recipe.recipeType) return i;
        }
        return -1;
    }

    //////////////////////////////////////
    // ************ Inputs *************//
    //////////////////////////////////////

    /** The recipe's item and fluid inputs, items in recipe order; the programmed circuit left out. */
    private static List<Need> needs(GTRecipe recipe) {
        List<Need> needs = new ArrayList<>();
        int index = 0;
        for (Content content : recipe.inputs.getOrDefault(ItemRecipeCapability.CAP, List.of())) {
            Ingredient ingredient = ItemRecipeCapability.CAP.of(content.content);
            if (SizedIngredient.getInner(ingredient) instanceof IntCircuitIngredient) continue;
            needs.add(new Need(ingredient, null, itemAmount(ingredient), index++));
        }
        for (Content content : recipe.inputs.getOrDefault(FluidRecipeCapability.CAP, List.of())) {
            FluidIngredient fluid = FluidRecipeCapability.CAP.of(content.content);
            needs.add(new Need(null, fluid, Math.max(1, fluid.getAmount()), -1));
        }
        return needs;
    }

    /** The recipe's programmed circuit, or empty. */
    private static ItemStack circuit(GTRecipe recipe) {
        for (Content content : recipe.inputs.getOrDefault(ItemRecipeCapability.CAP, List.of())) {
            Ingredient ingredient = ItemRecipeCapability.CAP.of(content.content);
            if (SizedIngredient.getInner(ingredient) instanceof IntCircuitIngredient circuit &&
                    circuit.getItems().length > 0) {
                return circuit.getItems()[0].copy();
            }
        }
        return ItemStack.EMPTY;
    }

    private static int itemAmount(Ingredient ingredient) {
        if (ingredient instanceof SizedIngredient sized) return Math.max(1, sized.getAmount());
        ItemStack[] items = ingredient.getItems();
        return items.length > 0 ? Math.max(1, items[0].getCount()) : 1;
    }

    private static boolean matches(Need need, ItemStack stack) {
        return !stack.isEmpty() && need.item.test(stack);
    }

    //////////////////////////////////////
    // ****** The machine's inputs *****//
    //////////////////////////////////////

    /** The machine's plain item input buses, in GT's order (an ME bus takes nothing from outside). */
    private static List<NotifiableItemStackHandler> itemTargets(IRecipeCapabilityHolder holder) {
        List<NotifiableItemStackHandler> targets = new ArrayList<>();
        for (IRecipeHandler<?> handler : holder.getCapabilitiesFlat(IO.IN, ItemRecipeCapability.CAP)) {
            if (handler instanceof NotifiableItemStackHandler items && items.getHandlerIO() == IO.IN &&
                    items.canCapInput() && items.shouldSearchContent()) {
                targets.add(items);
            }
        }
        return targets;
    }

    private static List<NotifiableFluidTank> fluidTargets(IRecipeCapabilityHolder holder) {
        List<NotifiableFluidTank> targets = new ArrayList<>();
        for (IRecipeHandler<?> handler : holder.getCapabilitiesFlat(IO.IN, FluidRecipeCapability.CAP)) {
            if (handler instanceof NotifiableFluidTank tank && tank.getHandlerIO() == IO.IN && tank.canCapInput()) {
                targets.add(tank);
            }
        }
        return targets;
    }

    private static long count(List<NotifiableItemStackHandler> handlers, Need need) {
        long count = 0;
        for (NotifiableItemStackHandler handler : handlers) {
            for (int slot = 0; slot < handler.getSlots(); slot++) {
                ItemStack stack = handler.getStackInSlot(slot);
                if (matches(need, stack)) count += stack.getCount();
            }
        }
        return count;
    }

    private static long countFluid(List<NotifiableFluidTank> tanks, Need need) {
        long count = 0;
        for (NotifiableFluidTank tank : tanks) {
            for (int i = 0; i < tank.getTanks(); i++) {
                FluidStack fluid = tank.getFluidInTank(i);
                if (!fluid.isEmpty() && need.fluid.test(fluid)) count += fluid.getAmount();
            }
        }
        return count;
    }

    /** An Assembly Line's i-th input: what its i-th bus starts with, if that is the i-th ingredient. */
    private static long orderedCount(List<NotifiableItemStackHandler> buses, int index, Need need) {
        if (index < 0 || index >= buses.size()) return 0;
        ItemStack first = firstStack(buses.get(index));
        return matches(need, first) ? first.getCount() : 0;
    }

    private static ItemStack firstStack(NotifiableItemStackHandler handler) {
        for (int slot = 0; slot < handler.getSlots(); slot++) {
            ItemStack stack = handler.getStackInSlot(slot);
            if (!stack.isEmpty()) return stack;
        }
        return ItemStack.EMPTY;
    }

    /** The recipe's circuit in every receiving bus that has a circuit slot (GT reads the bus's own circuit). */
    private static void setCircuit(List<NotifiableItemStackHandler> targets, ItemStack circuit) {
        if (circuit.isEmpty()) return;
        for (NotifiableItemStackHandler handler : targets) {
            if (handler.getMachine() instanceof ItemBusPartMachine bus && bus.getCircuitInventory().getSlots() > 0 &&
                    !ItemStack.isSameItemSameTags(bus.getCircuitInventory().getStackInSlot(0), circuit)) {
                bus.getCircuitInventory().storage.setStackInSlot(0, circuit.copy());
            }
        }
    }

    //////////////////////////////////////
    // **** The controller's inputs ****//
    //////////////////////////////////////

    /** The controller's item input buses, plain or ME. */
    private static List<NotifiableItemStackHandler> itemSources(BusControllerMachine controller) {
        List<NotifiableItemStackHandler> sources = new ArrayList<>();
        for (IRecipeHandler<?> handler : controller.getCapabilitiesFlat(IO.IN, ItemRecipeCapability.CAP)) {
            if (handler instanceof NotifiableItemStackHandler items && items.getHandlerIO() == IO.IN &&
                    items.shouldSearchContent()) {
                sources.add(items);
            }
        }
        return sources;
    }

    private static List<NotifiableFluidTank> fluidSources(BusControllerMachine controller) {
        List<NotifiableFluidTank> sources = new ArrayList<>();
        for (IRecipeHandler<?> handler : controller.getCapabilitiesFlat(IO.IN, FluidRecipeCapability.CAP)) {
            if (handler instanceof NotifiableFluidTank tank && tank.getHandlerIO() == IO.IN) sources.add(tank);
        }
        return sources;
    }

    /** Reserves {@code amount} items of a need across the sources' slots; false if they do not hold enough. */
    private static boolean reserveItems(List<NotifiableItemStackHandler> sources,
                                        Map<NotifiableItemStackHandler, int[]> reserved, Need need, int amount,
                                        List<Taken> taken, int index) {
        int left = amount;
        Map<NotifiableItemStackHandler, int[]> plan = new IdentityHashMap<>();
        List<ItemStack> stacks = new ArrayList<>();
        for (NotifiableItemStackHandler source : sources) {
            int[] used = reserved.get(source);
            for (int slot = 0; slot < source.getSlots() && left > 0; slot++) {
                ItemStack stack = source.getStackInSlot(slot);
                if (!matches(need, stack)) continue;
                int free = stack.getCount() - (used == null ? 0 : used[slot]);
                if (free <= 0) continue;
                // what the slot would really give (an ME bus gives what its network holds)
                int take = source.extractItemInternal(slot, Math.min(free, left), true).getCount();
                if (take <= 0) continue;
                plan.computeIfAbsent(source, s -> new int[s.getSlots()])[slot] += take;
                stacks.add(stack.copyWithCount(take));
                left -= take;
            }
            if (left <= 0) break;
        }
        if (left > 0) return false;
        for (var entry : plan.entrySet()) {
            int[] counts = reserved.computeIfAbsent(entry.getKey(), s -> new int[s.getSlots()]);
            for (int slot = 0; slot < counts.length; slot++) counts[slot] += entry.getValue()[slot];
        }
        for (ItemStack stack : stacks) taken.add(new Taken(stack, index));
        return true;
    }

    /** Reserves a need's fluid in one source tank (drained on execute); false if none holds enough. */
    private static boolean reserveFluid(List<NotifiableFluidTank> sources, Need need, int amount,
                                        List<FluidStack> taken) {
        for (NotifiableFluidTank source : sources) {
            for (int i = 0; i < source.getTanks(); i++) {
                FluidStack fluid = source.getFluidInTank(i);
                if (fluid.isEmpty() || !need.fluid.test(fluid)) continue;
                FluidStack wanted = new FluidStack(fluid, amount);
                long already = taken.stream().filter(f -> f.isFluidEqual(wanted)).mapToLong(FluidStack::getAmount)
                        .sum();
                if (source.drainInternal(new FluidStack(fluid, (int) Math.min(Integer.MAX_VALUE, already + amount)),
                        IFluidHandler.FluidAction.SIMULATE).getAmount() < already + amount) {
                    continue;
                }
                taken.add(wanted);
                return true;
            }
        }
        return false;
    }

    private static void drainFrom(List<NotifiableFluidTank> sources, FluidStack fluid) {
        int left = fluid.getAmount();
        for (NotifiableFluidTank source : sources) {
            if (left <= 0) return;
            left -= source.drainInternal(new FluidStack(fluid, left), IFluidHandler.FluidAction.EXECUTE).getAmount();
        }
    }

    private static void giveBack(List<NotifiableItemStackHandler> sources, ItemStack stack) {
        ItemStack left = stack;
        for (NotifiableItemStackHandler source : sources) {
            if (left.isEmpty()) return;
            if (source.canCapInput()) left = insert(source, left);
        }
    }

    //////////////////////////////////////
    // ************ Placing ************//
    //////////////////////////////////////

    /**
     * The bus each taken stack goes into, or null if they do not all fit: an Assembly Line's i-th ingredient into its
     * i-th bus (empty or holding that ingredient), else all into the first bus that takes them all.
     */
    private static List<NotifiableItemStackHandler> planItems(List<NotifiableItemStackHandler> targets,
                                                             List<Taken> taken, boolean ordered) {
        if (taken.isEmpty()) return List.of();
        if (ordered) {
            Map<NotifiableItemStackHandler, List<ItemStack>> perBus = new IdentityHashMap<>();
            List<NotifiableItemStackHandler> plan = new ArrayList<>();
            for (Taken item : taken) {
                if (item.index() < 0 || item.index() >= targets.size()) return null;
                NotifiableItemStackHandler bus = targets.get(item.index());
                ItemStack first = firstStack(bus);
                if (!first.isEmpty() && !ItemStack.isSameItemSameTags(first, item.stack())) return null;
                perBus.computeIfAbsent(bus, b -> new ArrayList<>()).add(item.stack());
                plan.add(bus);
            }
            for (var entry : perBus.entrySet()) {
                if (!fits(entry.getKey(), entry.getValue())) return null;
            }
            return plan;
        }
        List<ItemStack> stacks = taken.stream().map(Taken::stack).toList();
        for (NotifiableItemStackHandler bus : targets) {
            if (fits(bus, stacks)) {
                List<NotifiableItemStackHandler> plan = new ArrayList<>();
                for (int i = 0; i < stacks.size(); i++) plan.add(bus);
                return plan;
            }
        }
        return null;
    }

    /** Whether the stacks all fit into the bus together (a dry run on a copy of its slots). */
    private static boolean fits(NotifiableItemStackHandler bus, List<ItemStack> stacks) {
        int slots = bus.getSlots();
        ItemStack[] copy = new ItemStack[slots];
        for (int slot = 0; slot < slots; slot++) copy[slot] = bus.getStackInSlot(slot).copy();
        for (ItemStack stack : stacks) {
            int left = stack.getCount();
            for (int pass = 0; pass < 2 && left > 0; pass++) {
                for (int slot = 0; slot < slots && left > 0; slot++) {
                    ItemStack in = copy[slot];
                    int limit = Math.min(bus.getSlotLimit(slot), stack.getMaxStackSize());
                    if (pass == 0 && !in.isEmpty() && ItemStack.isSameItemSameTags(in, stack)) {
                        int add = Math.min(left, limit - in.getCount());
                        if (add > 0) {
                            in.grow(add);
                            left -= add;
                        }
                    } else if (pass == 1 && in.isEmpty() && bus.isItemValid(slot, stack)) {
                        int add = Math.min(left, limit);
                        copy[slot] = stack.copyWithCount(add);
                        left -= add;
                    }
                }
            }
            if (left > 0) return false;
        }
        return true;
    }

    /** Puts a stack into a bus: onto matching stacks first, then into empty slots. The rest comes back. */
    private static ItemStack insert(NotifiableItemStackHandler bus, ItemStack stack) {
        ItemStack left = stack.copy();
        for (int pass = 0; pass < 2 && !left.isEmpty(); pass++) {
            for (int slot = 0; slot < bus.getSlots() && !left.isEmpty(); slot++) {
                ItemStack in = bus.getStackInSlot(slot);
                if (pass == 0 ? in.isEmpty() || !ItemStack.isSameItemSameTags(in, left) : !in.isEmpty()) continue;
                left = bus.insertItemInternal(slot, left, false);
            }
        }
        return left;
    }

    /** The hatch each taken fluid goes into (one holding it, or an empty one not yet planned), or null. */
    private static List<NotifiableFluidTank> planFluids(List<NotifiableFluidTank> targets, List<FluidStack> fluids) {
        List<NotifiableFluidTank> plan = new ArrayList<>();
        List<NotifiableFluidTank> claimed = new ArrayList<>();
        for (FluidStack fluid : fluids) {
            NotifiableFluidTank chosen = null;
            for (NotifiableFluidTank tank : targets) {
                if (!holds(tank, fluid)) continue;
                if (tank.fillInternal(fluid, IFluidHandler.FluidAction.SIMULATE) == fluid.getAmount()) {
                    chosen = tank;
                    break;
                }
            }
            if (chosen == null) {
                for (NotifiableFluidTank tank : targets) {
                    if (claimed.contains(tank) || !isEmpty(tank)) continue;
                    if (tank.fillInternal(fluid, IFluidHandler.FluidAction.SIMULATE) == fluid.getAmount()) {
                        chosen = tank;
                        claimed.add(tank);
                        break;
                    }
                }
            }
            if (chosen == null) return null;
            plan.add(chosen);
        }
        return plan;
    }

    private static boolean holds(NotifiableFluidTank tank, FluidStack fluid) {
        for (int i = 0; i < tank.getTanks(); i++) {
            if (tank.getFluidInTank(i).isFluidEqual(fluid)) return true;
        }
        return false;
    }

    private static boolean isEmpty(NotifiableFluidTank tank) {
        for (int i = 0; i < tank.getTanks(); i++) {
            if (!tank.getFluidInTank(i).isEmpty()) return false;
        }
        return true;
    }

    //////////////////////////////////////
    // ************* Names *************//
    //////////////////////////////////////

    static Component displayName(Ingredient ingredient) {
        ItemStack[] items = ingredient.getItems();
        return items.length > 0 ? items[0].getHoverName() : Component.literal("?");
    }

    static Component displayName(FluidIngredient fluid) {
        FluidStack[] stacks = fluid.getStacks();
        return stacks.length > 0 ? stacks[0].getDisplayName() : Component.literal("?");
    }
}
