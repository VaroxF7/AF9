package com.af9.core.bus;

import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.api.capability.recipe.FluidRecipeCapability;
import com.gregtechceu.gtceu.api.capability.recipe.ItemRecipeCapability;
import com.gregtechceu.gtceu.api.gui.widget.PhantomSlotWidget;
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.gregtechceu.gtceu.api.machine.feature.IRecipeLogicMachine;
import com.gregtechceu.gtceu.api.machine.feature.multiblock.IMultiController;
import com.gregtechceu.gtceu.api.machine.feature.multiblock.IMultiPart;
import com.gregtechceu.gtceu.api.machine.multiblock.MultiblockDisplayText;
import com.gregtechceu.gtceu.api.machine.multiblock.WorkableElectricMultiblockMachine;
import com.gregtechceu.gtceu.api.machine.trait.RecipeLogic;
import com.gregtechceu.gtceu.api.recipe.GTRecipe;
import com.gregtechceu.gtceu.api.recipe.GTRecipeType;
import com.gregtechceu.gtceu.api.recipe.content.Content;
import com.gregtechceu.gtceu.api.recipe.ingredient.FluidIngredient;
import com.gregtechceu.gtceu.api.recipe.ingredient.IntCircuitIngredient;
import com.gregtechceu.gtceu.api.recipe.ingredient.SizedIngredient;
import com.gregtechceu.gtceu.api.transfer.item.CustomItemStackHandler;
import com.gregtechceu.gtceu.common.data.GTRecipeTypes;

import com.lowdragmc.lowdraglib.gui.util.ClickData;
import com.lowdragmc.lowdraglib.gui.widget.ComponentPanelWidget;
import com.lowdragmc.lowdraglib.gui.widget.Widget;
import com.lowdragmc.lowdraglib.gui.widget.WidgetGroup;
import com.lowdragmc.lowdraglib.syncdata.annotation.Persisted;
import com.lowdragmc.lowdraglib.syncdata.field.ManagedFieldHolder;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.Level;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Bus Controller: the machine bus's PLC. A small multiblock with input buses and hatches (plain or ME, so AE2 can send
 * it items), energy and a Bus Connector. Its screen lists the machines on its bus; for each it picks a recipe (put the
 * product, or a bucket or cell of a fluid product, into the slot, then choose among the machine's recipes that make it).
 * While it runs it supplies every machine with a set recipe once a second ({@link BusSupply}): one run of ingredients
 * into the machine's own input bus whenever it holds none. The recipe is kept on the machine's connector
 * ({@link BusConnectorPartMachine#getRecipeId}); a connector can refuse the controller.
 */
public class BusControllerMachine extends WorkableElectricMultiblockMachine {

    protected static final ManagedFieldHolder MANAGED_FIELD_HOLDER = new ManagedFieldHolder(
            BusControllerMachine.class, WorkableElectricMultiblockMachine.MANAGED_FIELD_HOLDER);

    /** Ticks between two supply rounds. */
    public static final int SUPPLY_INTERVAL = 20;
    /** EU/t while switched on (an MV machine's worth). */
    public static final long EUT = GTValues.VA[GTValues.MV];
    /** Recipes the picker offers at most. */
    private static final int MAX_CANDIDATES = 64;

    /** The machine the screen shows: its connector's position. */
    @Persisted
    private long selected;
    /** The chosen one among the recipes that make the product. */
    @Persisted
    private int candidate;
    /** The product slot (a ghost item). */
    @Persisted
    private final CustomItemStackHandler product = new CustomItemStackHandler(1);

    private final Map<Long, Component> supplyStatus = new HashMap<>();
    private List<GTRecipe> candidates = List.of();
    /** What {@link #candidates} were found for: the product and the machine. */
    private ItemStack candidatesProduct = ItemStack.EMPTY;
    private long candidatesMachine = Long.MIN_VALUE;

    public BusControllerMachine(IMachineBlockEntity holder) {
        super(holder);
        product.setOnContentsChanged(() -> {
            candidate = 0;
            markDirty();
        });
    }

    @Override
    public ManagedFieldHolder getFieldHolder() {
        return MANAGED_FIELD_HOLDER;
    }

    @Override
    protected RecipeLogic createRecipeLogic(Object... args) {
        return new SupplyLogic(this);
    }

    //////////////////////////////////////
    // ************ The bus ************//
    //////////////////////////////////////

    /** This controller's Bus Connector, or null. */
    public BusConnectorPartMachine getPort() {
        for (IMultiPart part : getParts()) {
            if (part instanceof BusConnectorPartMachine connector) return connector;
        }
        return null;
    }

    /** The machines on the bus that run recipes. */
    public List<BusConnectorPartMachine> getMachines() {
        BusConnectorPartMachine port = getPort();
        if (port == null) return List.of();
        List<BusConnectorPartMachine> machines = new ArrayList<>();
        for (BusConnectorPartMachine connector : port.getMachinesOnBus()) {
            IMultiController controller = connector.getMachineController();
            if (controller instanceof IRecipeLogicMachine rlm && runsRecipes(rlm)) machines.add(connector);
        }
        return machines;
    }

    private static boolean runsRecipes(IRecipeLogicMachine rlm) {
        for (GTRecipeType type : rlm.getRecipeTypes()) {
            if (type != GTRecipeTypes.DUMMY_RECIPES) return true;
        }
        return false;
    }

    private boolean drawEnergy() {
        if (energyContainer == null || energyContainer.getEnergyStored() < EUT) return false;
        energyContainer.removeEnergy(EUT);
        return true;
    }

    /** One supply round: every machine with a set recipe that takes the controller. */
    private void supply() {
        Level level = getLevel();
        if (level == null) return;
        supplyStatus.clear();
        for (BusConnectorPartMachine connector : getMachines()) {
            ResourceLocation id = connector.getRecipeId();
            if (id == null) continue;
            long pos = connector.getPos().asLong();
            if (!connector.acceptsController()) {
                supplyStatus.put(pos, Component.translatable("af9.bus.supply.refused").withStyle(ChatFormatting.RED));
                continue;
            }
            GTRecipe recipe = recipe(level, id);
            if (recipe == null) {
                supplyStatus.put(pos, Component.translatable("af9.bus.supply.unknown").withStyle(ChatFormatting.RED));
                continue;
            }
            supplyStatus.put(pos, BusSupply.supply(this, connector, recipe));
        }
    }

    static GTRecipe recipe(Level level, ResourceLocation id) {
        return level.getRecipeManager().byKey(id).orElse(null) instanceof GTRecipe recipe ? recipe : null;
    }

    /** Runs the supply rounds while formed, switched on and powered; idles otherwise. */
    public static class SupplyLogic extends RecipeLogic {

        public SupplyLogic(BusControllerMachine machine) {
            super(machine);
        }

        public BusControllerMachine getController() {
            return (BusControllerMachine) machine;
        }

        @Override
        public void serverTick() {
            BusControllerMachine controller = getController();
            if (!controller.isFormed() || !isWorkingEnabled()) {
                setStatus(Status.IDLE);
                isActive = false;
            } else if (controller.drawEnergy()) {
                setStatus(Status.WORKING);
                isActive = true;
                progress = (progress + 1) % SUPPLY_INTERVAL;
                if (progress == 0) controller.supply();
            } else {
                setStatus(Status.WAITING);
                isActive = false;
            }
        }

        @Override
        public int getMaxProgress() {
            return SUPPLY_INTERVAL;
        }

        @Override
        public boolean isActive() {
            return getController().isFormed() && isActive;
        }
    }

    //////////////////////////////////////
    // ******** The recipe picker ******//
    //////////////////////////////////////

    /** The machine the screen shows (the first one if the selection is gone), or null. */
    private BusConnectorPartMachine selectedMachine(List<BusConnectorPartMachine> machines) {
        for (BusConnectorPartMachine connector : machines) {
            if (connector.getPos().asLong() == selected) return connector;
        }
        return machines.isEmpty() ? null : machines.get(0);
    }

    /** The selected machine's recipes that make the product (found again when either changes). */
    private List<GTRecipe> candidates(BusConnectorPartMachine connector) {
        ItemStack wanted = product.getStackInSlot(0);
        long machinePos = connector == null ? Long.MIN_VALUE : connector.getPos().asLong();
        if (machinePos == candidatesMachine && ItemStack.isSameItemSameTags(wanted, candidatesProduct)) {
            return candidates;
        }
        candidatesMachine = machinePos;
        candidatesProduct = wanted.copy();
        candidates = List.of();
        Level level = getLevel();
        if (connector == null || wanted.isEmpty() || level == null ||
                !(connector.getMachineController() instanceof IRecipeLogicMachine rlm)) {
            return candidates;
        }
        FluidStack fluid = FluidUtil.getFluidContained(wanted).orElse(FluidStack.EMPTY);
        List<GTRecipe> found = new ArrayList<>();
        GTRecipeType[] types = rlm.getRecipeTypes();
        for (int i = 0; i < types.length && found.size() < MAX_CANDIDATES; i++) {
            if (types[i] == GTRecipeTypes.DUMMY_RECIPES || !BusData.modeAllowed(rlm.self(), i)) continue;
            for (GTRecipe recipe : level.getRecipeManager().getAllRecipesFor(types[i])) {
                if (makes(recipe, wanted, fluid)) {
                    found.add(recipe);
                    if (found.size() >= MAX_CANDIDATES) break;
                }
            }
        }
        candidates = List.copyOf(found);
        return candidates;
    }

    private static boolean makes(GTRecipe recipe, ItemStack wanted, FluidStack fluid) {
        for (Content content : recipe.outputs.getOrDefault(ItemRecipeCapability.CAP, List.of())) {
            for (ItemStack stack : ItemRecipeCapability.CAP.of(content.content).getItems()) {
                if (ItemStack.isSameItem(stack, wanted)) return true;
            }
        }
        if (!fluid.isEmpty()) {
            for (Content content : recipe.outputs.getOrDefault(FluidRecipeCapability.CAP, List.of())) {
                if (FluidRecipeCapability.CAP.of(content.content).test(fluid)) return true;
            }
        }
        return false;
    }

    //////////////////////////////////////
    // ************ Screen *************//
    //////////////////////////////////////

    /** GT's multiblock screen, with the product slot in its top right corner. */
    @Override
    public Widget createUIWidget() {
        Widget widget = super.createUIWidget();
        if (widget instanceof WidgetGroup group) {
            group.addWidget(new PhantomSlotWidget(product, 0, 166, 6).setClearSlotOnRightClick(true)
                    .setMaxStackSize(1));
        }
        return widget;
    }

    @Override
    public void addDisplayText(List<Component> text) {
        MultiblockDisplayText.builder(text, isFormed())
                .setWorkingStatus(recipeLogic.isWorkingEnabled(), recipeLogic.isActive())
                .setWorkingStatusKeys("gtceu.multiblock.idling", "gtceu.multiblock.work_paused",
                        "af9.bus.controller.running")
                .addEnergyUsageLine(energyContainer)
                .addWorkingStatusLine();
        if (!isFormed()) return;
        BusConnectorPartMachine port = getPort();
        if (port == null) {
            text.add(Component.translatable("af9.bus.controller.no_port").withStyle(ChatFormatting.RED));
            return;
        }
        List<BusConnectorPartMachine> machines = getMachines();
        BusNetwork.Bus bus = port.getBus();
        text.add(Component.translatable("af9.bus.controller.bus", machines.size(),
                port.getComputation().getMaxCWUt(), bus.data().size()).withStyle(ChatFormatting.GRAY));
        BusConnectorPartMachine machine = selectedMachine(machines);
        if (machine == null) {
            text.add(Component.translatable("af9.bus.controller.no_machines").withStyle(ChatFormatting.YELLOW));
            return;
        }

        // the machine
        text.add(Component.empty()
                .append(ComponentPanelWidget.withButton(Component.literal("◀ "), "machine_prev"))
                .append(MachineBusModule.deviceName(BusData.snapshot(machine)).copy()
                        .withStyle(ChatFormatting.WHITE))
                .append(ComponentPanelWidget.withButton(Component.literal(" ▶"), "machine_next"))
                .append(Component.literal("  " + (machines.indexOf(machine) + 1) + "/" + machines.size())
                        .withStyle(ChatFormatting.DARK_GRAY)));
        Level level = getLevel();
        ResourceLocation id = machine.getRecipeId();
        GTRecipe set = id == null || level == null ? null : recipe(level, id);
        if (!machine.acceptsController()) {
            text.add(Component.translatable("af9.bus.supply.refused").withStyle(ChatFormatting.RED));
        }
        text.add(Component.translatable("af9.bus.controller.recipe").withStyle(ChatFormatting.GOLD)
                .append(" ")
                .append(set == null ? Component.translatable(id == null ? "af9.bus.controller.none" :
                        "af9.bus.supply.unknown").withStyle(ChatFormatting.GRAY) : describe(set))
                .append(id == null ? Component.empty() : Component.literal("  ").append(
                        ComponentPanelWidget.withButton(Component.translatable("af9.bus.controller.clear")
                                .withStyle(ChatFormatting.RED), "clear"))));
        Component status = supplyStatus.get(machine.getPos().asLong());
        if (id != null && status != null) {
            text.add(Component.translatable("af9.bus.controller.supply").withStyle(ChatFormatting.GOLD).append(" ")
                    .append(status));
        }

        // the picker
        ItemStack wanted = product.getStackInSlot(0);
        if (wanted.isEmpty()) {
            text.add(Component.translatable("af9.bus.controller.put_product").withStyle(ChatFormatting.DARK_GRAY));
            return;
        }
        List<GTRecipe> found = candidates(machine);
        if (found.isEmpty()) {
            text.add(Component.translatable("af9.bus.controller.no_recipes", wanted.getHoverName())
                    .withStyle(ChatFormatting.YELLOW));
            return;
        }
        int index = Math.floorMod(candidate, found.size());
        GTRecipe chosen = found.get(index);
        text.add(Component.empty()
                .append(ComponentPanelWidget.withButton(Component.literal("◀ "), "recipe_prev"))
                .append(Component.literal((index + 1) + "/" + found.size()).withStyle(ChatFormatting.WHITE))
                .append(ComponentPanelWidget.withButton(Component.literal(" ▶ "), "recipe_next"))
                .append(describe(chosen)));
        text.add(Component.translatable("af9.bus.controller.inputs", inputs(chosen)).withStyle(ChatFormatting.GRAY));
        text.add(ComponentPanelWidget.withButton(Component.translatable("af9.bus.controller.assign")
                .withStyle(ChatFormatting.GREEN), "assign"));
    }

    @Override
    public void handleDisplayClick(String id, ClickData click) {
        if (click.isRemote) return;
        List<BusConnectorPartMachine> machines = getMachines();
        BusConnectorPartMachine machine = selectedMachine(machines);
        if (machine == null) return;
        switch (id) {
            case "machine_prev", "machine_next" -> {
                int index = Math.floorMod(machines.indexOf(machine) + (id.equals("machine_next") ? 1 : -1),
                        machines.size());
                selected = machines.get(index).getPos().asLong();
                candidate = 0;
            }
            case "recipe_prev" -> candidate--;
            case "recipe_next" -> candidate++;
            case "assign" -> {
                List<GTRecipe> found = candidates(machine);
                if (found.isEmpty() || !machine.acceptsController()) return;
                machine.setRecipeId(found.get(Math.floorMod(candidate, found.size())).getId());
            }
            case "clear" -> machine.setRecipeId(null);
            default -> {
                return;
            }
        }
        markDirty();
    }

    /** "Assembler: 4× Transistor" — the recipe's type and first product. */
    private static MutableComponent describe(GTRecipe recipe) {
        MutableComponent line = Component.translatable(recipe.recipeType.registryName.toLanguageKey())
                .withStyle(ChatFormatting.AQUA).append(Component.literal(": ").withStyle(ChatFormatting.GRAY));
        for (Content content : recipe.outputs.getOrDefault(ItemRecipeCapability.CAP, List.of())) {
            ItemStack[] items = ItemRecipeCapability.CAP.of(content.content).getItems();
            if (items.length > 0) {
                return line.append(Component.literal(items[0].getCount() + "× ").append(items[0].getHoverName())
                        .withStyle(ChatFormatting.WHITE));
            }
        }
        for (Content content : recipe.outputs.getOrDefault(FluidRecipeCapability.CAP, List.of())) {
            FluidIngredient fluid = FluidRecipeCapability.CAP.of(content.content);
            return line.append(Component.literal(fluid.getAmount() + " mB ").append(BusSupply.displayName(fluid))
                    .withStyle(ChatFormatting.WHITE));
        }
        return line.append(Component.literal(recipe.getId().toString()).withStyle(ChatFormatting.WHITE));
    }

    /** "2× Silicon Wafer, 144 mB Soldering Alloy" — what one run takes. */
    private static MutableComponent inputs(GTRecipe recipe) {
        MutableComponent line = Component.empty();
        boolean first = true;
        for (Content content : recipe.inputs.getOrDefault(ItemRecipeCapability.CAP, List.of())) {
            Ingredient ingredient = ItemRecipeCapability.CAP.of(content.content);
            if (SizedIngredient.getInner(ingredient) instanceof IntCircuitIngredient) continue;
            int amount = ingredient instanceof SizedIngredient sized ? sized.getAmount() : 1;
            if (!first) line.append(", ");
            line.append(amount + "× ").append(BusSupply.displayName(ingredient));
            first = false;
        }
        for (Content content : recipe.inputs.getOrDefault(FluidRecipeCapability.CAP, List.of())) {
            FluidIngredient fluid = FluidRecipeCapability.CAP.of(content.content);
            if (!first) line.append(", ");
            line.append(fluid.getAmount() + " mB ").append(BusSupply.displayName(fluid));
            first = false;
        }
        return line;
    }
}
