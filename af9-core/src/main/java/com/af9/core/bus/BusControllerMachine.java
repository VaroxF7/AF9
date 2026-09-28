package com.af9.core.bus;

import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.api.capability.recipe.FluidRecipeCapability;
import com.gregtechceu.gtceu.api.capability.recipe.ItemRecipeCapability;
import com.gregtechceu.gtceu.api.gui.GuiTextures;
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
import com.lowdragmc.lowdraglib.gui.widget.ImageWidget;
import com.lowdragmc.lowdraglib.gui.widget.LabelWidget;
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
import java.util.List;

/**
 * Bus Controller: the machine bus's PLC. A small multiblock with input buses and hatches (plain or ME, so AE2 can send
 * it items), energy, up to four Bus Connectors (one on each bus it runs) and an Interconnect Hatch (the link to other
 * Bus Controllers). Its buses, and every bus and controller linked to it, are one network ({@link BusNetwork.Net}):
 * computation and research flow between the buses, and its screen lists every machine of the network; for each it
 * picks a recipe (put the product, or a bucket or cell of a fluid product, into the slot, then choose among the
 * machine's recipes that make it). While it runs it supplies the machines of its own buses once a second
 * ({@link BusSupply}): one run of ingredients into the machine's own input bus whenever it holds none, from its own
 * inputs first, then from the linked controllers'. The recipe is kept on the machine's connector
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
    /** Bus Connectors (buses) one controller takes. */
    public static final int MAX_PORTS = 4;
    /** Ticks the network is kept. */
    private static final int NETWORK_CACHE_TICKS = 20;

    /** The machine the screen shows: its connector's position. */
    @Persisted
    private long selected;
    /** The chosen one among the recipes that make the product. */
    @Persisted
    private int candidate;
    /** The product slot (a ghost item). */
    @Persisted
    private final CustomItemStackHandler product = new CustomItemStackHandler(1);

    private BusNetwork.Net network;
    /** Game time the network was found, -1 before. */
    private long networkTime = -1;
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

    /** This controller's Bus Connectors: its ports, one on each bus it runs. */
    public List<BusConnectorPartMachine> getPorts() {
        List<BusConnectorPartMachine> ports = new ArrayList<>();
        for (IMultiPart part : getParts()) {
            if (part instanceof BusConnectorPartMachine connector) ports.add(connector);
        }
        return ports;
    }

    /** The Bus Controllers linked to this one through Interconnect Hatches. */
    public List<BusControllerMachine> getLinkedControllers() {
        List<BusControllerMachine> linked = new ArrayList<>();
        for (IMultiPart part : getParts()) {
            if (!(part instanceof BusInterconnectPartMachine hatch)) continue;
            for (BusControllerMachine controller : hatch.getLinkedControllers()) {
                if (controller != this && !linked.contains(controller)) linked.add(controller);
            }
        }
        return linked;
    }

    /** This controller's network: its buses and every bus and controller linked to it (found at most once a second). */
    public BusNetwork.Net getNetwork() {
        long now = getLevel() == null ? 0 : getLevel().getGameTime();
        if (network == null || networkTime < 0 || now < networkTime || now - networkTime >= NETWORK_CACHE_TICKS) {
            List<BusNetwork.Bus> buses = new ArrayList<>();
            for (BusConnectorPartMachine port : getPorts()) buses.add(port.getBus());
            network = BusNetwork.network(buses, List.of(this));
            networkTime = now;
        }
        return network;
    }

    /** The machines of the network that run recipes, each once. */
    public List<BusConnectorPartMachine> getMachines() {
        List<BusConnectorPartMachine> machines = new ArrayList<>();
        for (BusNetwork.Bus bus : getNetwork().buses()) {
            for (BusConnectorPartMachine connector : bus.connectors()) {
                if (machines.contains(connector) || connector.isInValid()) continue;
                IMultiController controller = connector.getMachineController();
                if (controller instanceof IRecipeLogicMachine rlm && runsRecipes(rlm)) machines.add(connector);
            }
        }
        return machines;
    }

    /** The bus of the network a machine's connector is on, or null. */
    private BusNetwork.Bus busOf(BusConnectorPartMachine connector) {
        for (BusNetwork.Bus bus : getNetwork().buses()) {
            if (bus.connectors().contains(connector)) return bus;
        }
        return null;
    }

    /**
     * Whether this controller supplies the machines of a bus: it has a port on it, and of the controllers with a port
     * on it, it is the one at the lowest position (one supplier per bus).
     */
    private boolean supplies(BusNetwork.Bus bus) {
        long self = getPos().asLong();
        boolean port = false;
        for (BusConnectorPartMachine connector : bus.connectors()) {
            BusControllerMachine controller = connector.getBusController();
            if (controller == null || !controller.isFormed()) continue;
            if (controller == this) {
                port = true;
            } else if (controller.getPos().asLong() < self) {
                return false;
            }
        }
        return port;
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

    /**
     * One supply round: every machine with a set recipe on the buses this controller supplies, from its own inputs
     * first, then the linked controllers'. Each machine's connector keeps what happened.
     */
    private void supply() {
        Level level = getLevel();
        if (level == null) return;
        BusNetwork.Net net = getNetwork();
        List<BusControllerMachine> sources = new ArrayList<>(net.controllers());
        sources.remove(this);
        sources.add(0, this);
        for (BusNetwork.Bus bus : net.buses()) {
            if (!supplies(bus)) continue;
            for (BusConnectorPartMachine connector : bus.connectors()) {
                ResourceLocation id = connector.getRecipeId();
                if (id == null || connector.getMachineController() == null) continue;
                connector.setSupplyStatus(supplyOne(level, bus, connector, id, sources));
            }
        }
    }

    private static Component supplyOne(Level level, BusNetwork.Bus bus, BusConnectorPartMachine connector,
                                       ResourceLocation id, List<BusControllerMachine> sources) {
        if (bus.overloaded()) {
            return Component.translatable("af9.bus.supply.overloaded", BusNetwork.MAX_MACHINES)
                    .withStyle(ChatFormatting.RED);
        }
        if (!connector.acceptsController()) {
            return Component.translatable("af9.bus.supply.refused").withStyle(ChatFormatting.RED);
        }
        GTRecipe recipe = recipe(level, id);
        if (recipe == null) return Component.translatable("af9.bus.supply.unknown").withStyle(ChatFormatting.RED);
        return BusSupply.supply(sources, connector, recipe);
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

    /**
     * GT's multiblock screen, laid out for this text: a fixed head (the name, the product slot in the top right
     * corner) and under it the text, wrapped to the screen's width, scrolling (GT's own wraps wider than its screen,
     * so the lines were cut off at the right, and the text ran under the slot).
     */
    @Override
    public Widget createUIWidget() {
        var group = new WidgetGroup(0, 0, 190, 125);
        group.addWidget(new ImageWidget(4, 4, 182, 117, GuiTextures.DISPLAY));
        group.addWidget(new LabelWidget(8, 10, getBlockState().getBlock().getDescriptionId()));
        group.addWidget(new PhantomSlotWidget(product, 0, 166, 6).setClearSlotOnRightClick(true)
                .setMaxStackSize(1));
        boolean client = getLevel() != null && getLevel().isClientSide;
        group.addWidget(BusConnectorPartMachine.scrolling(4, 26, 182, 94, new ComponentPanelWidget(4, 0,
                this::addDisplayText)
                .textSupplier(client ? null : this::addDisplayText)
                .setMaxWidthLimit(170)
                .clickHandler(this::handleDisplayClick)));
        group.setBackground(GuiTextures.BACKGROUND_INVERSE);
        return group;
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
        List<BusConnectorPartMachine> ports = getPorts();
        if (ports.isEmpty()) {
            text.add(Component.translatable("af9.bus.controller.no_port").withStyle(ChatFormatting.RED));
            return;
        }
        BusNetwork.Net net = getNetwork();
        List<BusConnectorPartMachine> machines = getMachines();
        int overloaded = 0, research = 0;
        for (BusNetwork.Bus bus : net.buses()) {
            if (bus.overloaded()) overloaded++;
            else research += bus.data().size();
        }
        text.add(Component.translatable("af9.bus.controller.network", ports.size(), MAX_PORTS, net.buses().size(),
                net.controllers().size(), machines.size()).withStyle(ChatFormatting.GRAY));
        text.add(Component.translatable("af9.bus.controller.sources", ports.get(0).getComputation().getMaxCWUt(),
                BusNetwork.MAX_CWUT, research).withStyle(ChatFormatting.GRAY));
        if (overloaded > 0) {
            text.add(Component.translatable("af9.bus.controller.overloaded", overloaded, BusNetwork.MAX_MACHINES)
                    .withStyle(ChatFormatting.RED));
        }
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
        BusNetwork.Bus bus = busOf(machine);
        Component status = bus != null && bus.overloaded() ?
                Component.translatable("af9.bus.supply.overloaded", BusNetwork.MAX_MACHINES)
                        .withStyle(ChatFormatting.RED) :
                machine.getSupplyStatus();
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
