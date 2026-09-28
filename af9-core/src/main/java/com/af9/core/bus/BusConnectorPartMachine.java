package com.af9.core.bus;

import com.af9.core.compute.ComputationArrayMachine;

import com.gregtechceu.gtceu.api.capability.IDataAccessHatch;
import com.gregtechceu.gtceu.api.capability.IMonitorComponent;
import com.gregtechceu.gtceu.api.capability.IOpticalComputationProvider;
import com.gregtechceu.gtceu.api.capability.IOpticalDataAccessHatch;
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.gregtechceu.gtceu.api.machine.MetaMachine;
import com.gregtechceu.gtceu.api.machine.TickableSubscription;
import com.gregtechceu.gtceu.api.machine.feature.multiblock.IMultiController;
import com.gregtechceu.gtceu.api.machine.feature.multiblock.IMultiPart;
import com.gregtechceu.gtceu.api.machine.feature.multiblock.IWorkableMultiController;
import com.gregtechceu.gtceu.api.machine.multiblock.PartAbility;
import com.gregtechceu.gtceu.api.machine.multiblock.part.MultiblockPartMachine;
import com.gregtechceu.gtceu.api.recipe.GTRecipe;
import com.gregtechceu.gtceu.common.machine.multiblock.electric.CentralMonitorMachine;
import com.gregtechceu.gtceu.common.machine.multiblock.electric.research.DataBankMachine;
import com.gregtechceu.gtceu.common.recipe.condition.ResearchCondition;

import com.lowdragmc.lowdraglib.gui.texture.ColorRectTexture;
import com.lowdragmc.lowdraglib.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib.gui.texture.ResourceTexture;
import com.lowdragmc.lowdraglib.gui.util.ClickData;
import com.lowdragmc.lowdraglib.gui.widget.ComponentPanelWidget;
import com.lowdragmc.lowdraglib.gui.widget.DraggableScrollableWidgetGroup;
import com.lowdragmc.lowdraglib.gui.widget.LabelWidget;
import com.lowdragmc.lowdraglib.gui.widget.TextFieldWidget;
import com.lowdragmc.lowdraglib.gui.widget.Widget;
import com.lowdragmc.lowdraglib.gui.widget.WidgetGroup;
import com.lowdragmc.lowdraglib.syncdata.annotation.Persisted;
import com.lowdragmc.lowdraglib.syncdata.field.ManagedFieldHolder;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;

import java.util.Collection;
import java.util.List;

/**
 * Bus Connector: the port of a machine on the machine bus (a data center's bus connector). One sits in a machine's
 * structure (the AF9 multiblocks take one: {@link #BUS_CONNECTOR}; GT's Assembly Line, Research Station, Data Bank and
 * Network Switch take it where their optical reception hatch goes), in the wall of GT's Central Monitor
 * ({@link AF9Bus#installMonitorWall}) or in a Bus Controller; Optical Bus Cable from its front face joins it to the
 * others ({@link BusNetwork}).
 * <p>
 * On a machine it shares what its screen allows ({@link #sharedData}, bits of {@link BusData}) and takes the commands
 * its screen allows ({@link #acceptedCommands}): the Central Monitor's Machine Bus Module reads and sends them. It
 * counts the machine's finished runs. It is the machine's optical reception hatch too: the machine draws its CWU/t from
 * the bus's computation ({@link BusComputationContainer}) and its research from the bus's Data Banks
 * ({@link #isRecipeAvailable}); in a Data Bank it puts that bank's research on the bus ({@link #getDataSource}), in a
 * computation array that array's computation ({@link #getComputationSource}). A Bus
 * Controller sets the machine's recipe ({@link #getRecipeId}) and supplies it, unless the screen refuses that.
 * In a Central Monitor it is the monitor's port: it answers the taps on the Advanced Monitors of the module's screens
 * ({@link MachineBusModule#handleTouches}).
 */
@SuppressWarnings("removal") // new ResourceLocation(ns, path) is the only constructor on 1.20.1
public class BusConnectorPartMachine extends MultiblockPartMachine
                                     implements IMonitorComponent, IOpticalDataAccessHatch {

    protected static final ManagedFieldHolder MANAGED_FIELD_HOLDER = new ManagedFieldHolder(
            BusConnectorPartMachine.class, MultiblockPartMachine.MANAGED_FIELD_HOLDER);

    /** Registered by the connector's definition (KubeJS), taken by the AF9 multiblocks' patterns. */
    public static final PartAbility BUS_CONNECTOR = new PartAbility("af9_bus_connector");
    public static final int MAX_LABEL = 32;
    /** Ticks a walk of the bus is kept. */
    private static final int BUS_CACHE_TICKS = 20;
    /** Ticks between two looks at the monitor's touch screens (a monitor port). */
    private static final int TOUCH_INTERVAL = 2;
    private static final IGuiTexture ICON = new ResourceTexture(
            new ResourceLocation("af9", "textures/block/overlay/machine/overlay_bus_connector.png"));

    @Persisted
    private String label = "";
    @Persisted
    private int sharedData = BusData.ALL_DATA;
    @Persisted
    private int acceptedCommands = BusData.ALL_COMMANDS;
    @Persisted
    private long runs;
    /** The recipe a Bus Controller set for this machine ("" none). */
    @Persisted
    private String recipe = "";
    /** Whether this machine refuses a Bus Controller (its recipe and its supply). */
    @Persisted
    private boolean controllerRefused;
    /** The ME craft a Bus Controller sent: its recipe ("" none), runs not finished yet, the controller's position. */
    @Persisted
    private String meRecipe = "";
    @Persisted
    private int meRuns;
    @Persisted
    private long meController;

    private final BusComputationContainer computation;
    private final IDataAccessHatch dataSource = new BankData();
    private BusNetwork.Bus bus;
    private BusNetwork.Net network;
    /** Game time of the last walk, -1 before the first. */
    private long busTime = -1;
    /** What the Bus Controller that supplies this machine did last (not kept). */
    private Component supplyStatus;
    private TickableSubscription touchSubs;

    public BusConnectorPartMachine(IMachineBlockEntity holder) {
        super(holder);
        this.computation = new BusComputationContainer(this);
    }

    @Override
    public ManagedFieldHolder getFieldHolder() {
        return MANAGED_FIELD_HOLDER;
    }

    //////////////////////////////////////
    // ********** The machine **********//
    //////////////////////////////////////

    /** The machine this connector is the port of (not a Central Monitor or a Bus Controller), or null. */
    public IMultiController getMachineController() {
        for (IMultiController controller : getControllers()) {
            if (!(controller instanceof CentralMonitorMachine) && !(controller instanceof BusControllerMachine)) {
                return controller;
            }
        }
        return null;
    }

    /** The Central Monitor this connector sits in, or null. */
    public CentralMonitorMachine getMonitor() {
        for (IMultiController controller : getControllers()) {
            if (controller instanceof CentralMonitorMachine monitor) return monitor;
        }
        return null;
    }

    /** The Bus Controller this connector is the port of, or null. */
    public BusControllerMachine getBusController() {
        for (IMultiController controller : getControllers()) {
            if (controller instanceof BusControllerMachine busController) return busController;
        }
        return null;
    }

    public String getLabel() {
        return label;
    }

    public int getSharedData() {
        return sharedData;
    }

    public int getAcceptedCommands() {
        return acceptedCommands;
    }

    public long getRuns() {
        return runs;
    }

    /** A run of the machine finished. */
    @Override
    public boolean afterWorking(IWorkableMultiController controller) {
        runs++;
        if (meRuns > 0) meRuns--;
        markDirty();
        return super.afterWorking(controller);
    }

    //////////////////////////////////////
    // ******** An ME craft *********//
    //////////////////////////////////////

    /** The recipe of the ME craft a Bus Controller sent this machine, or null (com.af9.core.ae2.BusPatterns). */
    public ResourceLocation getMeRecipe() {
        return meRecipe.isEmpty() ? null : ResourceLocation.tryParse(meRecipe);
    }

    public boolean hasMeCraft() {
        return !meRecipe.isEmpty();
    }

    /** Runs of the ME craft sent and not finished yet. */
    public int getMeRuns() {
        return meRuns;
    }

    /** The Bus Controller that returns the ME craft's products (its position). */
    public long getMeController() {
        return meController;
    }

    /** One more run of an ME craft sent, by {@code controller}. */
    public void addMeRun(ResourceLocation recipe, long controller) {
        meRecipe = recipe.toString();
        meRuns++;
        meController = controller;
        markDirty();
    }

    public void setMeController(long controller) {
        meController = controller;
        markDirty();
    }

    /** The ME craft is done (or forgotten): the machine is free again. */
    public void clearMeCraft() {
        meRecipe = "";
        meRuns = 0;
        markDirty();
    }

    //////////////////////////////////////
    // ****** The Bus Controller *******//
    //////////////////////////////////////

    /** The recipe a Bus Controller set for this machine, or null. */
    public ResourceLocation getRecipeId() {
        return recipe.isEmpty() ? null : ResourceLocation.tryParse(recipe);
    }

    public void setRecipeId(ResourceLocation id) {
        String value = id == null ? "" : id.toString();
        if (value.equals(recipe)) return;
        recipe = value;
        markDirty();
    }

    /** Whether a Bus Controller may set this machine's recipe and supply it. */
    public boolean acceptsController() {
        return !controllerRefused;
    }

    //////////////////////////////////////
    // ************ The bus ************//
    //////////////////////////////////////

    /** This connector's bus (walked at most once a second). */
    public BusNetwork.Bus getBus() {
        long now = getLevel() == null ? 0 : getLevel().getGameTime();
        if (bus == null || busTime < 0 || now < busTime || now - busTime >= BUS_CACHE_TICKS) {
            bus = BusNetwork.walk(this);
            network = null;
            busTime = now;
        }
        return bus;
    }

    /** The network this connector's bus belongs to (its own bus alone without a Bus Controller on it). */
    public BusNetwork.Net getNetwork() {
        BusNetwork.Bus own = getBus();
        if (network == null) network = BusNetwork.network(List.of(own), List.of());
        return network;
    }

    public Component getSupplyStatus() {
        return supplyStatus;
    }

    public void setSupplyStatus(Component status) {
        supplyStatus = status;
    }

    /** The machine ports on this one's bus (Central Monitors', Bus Controllers' and empty connectors left out). */
    public List<BusConnectorPartMachine> getMachinesOnBus() {
        return getBus().connectors().stream()
                .filter(c -> c != this && !c.isInValid() && c.getMachineController() != null)
                .toList();
    }

    //////////////////////////////////////
    // ***** Computation, research *****//
    //////////////////////////////////////

    public BusComputationContainer getComputation() {
        return computation;
    }

    /** The receiving side: never a transmitter hatch. */
    @Override
    public boolean isTransmitter() {
        return false;
    }

    @Override
    public boolean isCreative() {
        return false;
    }

    /**
     * Research for this machine: a recipe without research always; else if a Data Bank on the bus or on another bus
     * of its network has it (not over an overloaded bus), or one of the machine's own data hatches (the connector
     * never blocks what they hold).
     */
    @Override
    public boolean isRecipeAvailable(GTRecipe recipe, Collection<IDataAccessHatch> seen) {
        seen.add(this);
        if (recipe.conditions.stream().noneMatch(ResearchCondition.class::isInstance)) return true;
        if (!getBus().overloaded()) {
            for (BusNetwork.Bus bus : getNetwork().buses()) {
                if (bus.overloaded()) continue;
                for (IDataAccessHatch source : bus.data()) {
                    if (!seen.contains(source) && source.isRecipeAvailable(recipe, seen)) return true;
                }
            }
        }
        for (IMultiController controller : getControllers()) {
            for (IMultiPart part : controller.getParts()) {
                if (part != this && part instanceof IDataAccessHatch hatch && !seen.contains(hatch) &&
                        hatch.isRecipeAvailable(recipe, seen)) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public GTRecipe modifyRecipe(GTRecipe recipe) {
        return IOpticalDataAccessHatch.super.modifyRecipe(recipe);
    }

    /** Whether its machine is part of a bus: the cable joins this port to another connector or an ME Computation Link. */
    public boolean isOnBus() {
        BusNetwork.Bus bus = getBus();
        return bus.connectors().size() > 1 || !bus.consumers().isEmpty();
    }

    /** The Bus Connector in a machine's structure, or null. */
    public static BusConnectorPartMachine of(IMultiController machine) {
        if (machine == null || !machine.isFormed()) return null;
        for (IMultiPart part : machine.getParts()) {
            if (part instanceof BusConnectorPartMachine connector) return connector;
        }
        return null;
    }

    /**
     * Under the machine's own text on GT's multiblock screen while its machine is on a bus: "This Machine is Part of a
     * BUS" and its BUS MACHINE ID, this connector's name. The machine's screen is covered by that card then
     * ({@link com.af9.core.machine.console.BusPlacardWidget}); these lines show when it is opened from the Central
     * Monitor.
     */
    @Override
    public void addMultiText(List<Component> text) {
        if (getMachineController() == null || !isOnBus()) return;
        text.add(Component.translatable("af9.bus.part_of_bus").withStyle(ChatFormatting.AQUA));
        text.add(Component.translatable("af9.bus.machine_id", label.isEmpty() ?
                Component.translatable("af9.bus.machine_id.none").withStyle(ChatFormatting.GRAY) :
                Component.literal(label).withStyle(ChatFormatting.WHITE)).withStyle(ChatFormatting.AQUA));
    }

    /** The research this connector puts on the bus: its Data Bank's, if it sits in one; else null. */
    public IDataAccessHatch getDataSource() {
        return getMachineController() instanceof DataBankMachine ? dataSource : null;
    }

    /** The computation this connector puts on the bus: its computation array's, if it sits in one; else null. */
    public IOpticalComputationProvider getComputationSource() {
        return getMachineController() instanceof ComputationArrayMachine array ? array : null;
    }

    /** A Data Bank's research, as its Optical Data Transmitter Hatch gives it: only while the bank runs. */
    private final class BankData implements IDataAccessHatch {

        @Override
        public boolean isRecipeAvailable(GTRecipe recipe, Collection<IDataAccessHatch> seen) {
            seen.add(this);
            if (!(getMachineController() instanceof DataBankMachine bank) || !bank.isFormed() ||
                    !bank.getRecipeLogic().isWorking()) {
                return false;
            }
            for (IMultiPart part : bank.getParts()) {
                if (part == BusConnectorPartMachine.this || !(part instanceof IDataAccessHatch hatch) ||
                        seen.contains(hatch)) {
                    continue;
                }
                var block = part.self().getBlockState().getBlock();
                boolean data = PartAbility.DATA_ACCESS.isApplicable(block) ||
                        PartAbility.OPTICAL_DATA_RECEPTION.isApplicable(block);
                if (data && hatch.isRecipeAvailable(recipe, seen)) return true;
            }
            return false;
        }

        @Override
        public boolean isCreative() {
            return false;
        }
    }

    //////////////////////////////////////
    // ****** Central Monitor port *****//
    //////////////////////////////////////

    @Override
    public void addedToController(IMultiController controller) {
        super.addedToController(controller);
        updateTouchSubscription();
    }

    @Override
    public void removedFromController(IMultiController controller) {
        super.removedFromController(controller);
        updateTouchSubscription();
    }

    @Override
    public void onUnload() {
        super.onUnload();
        if (touchSubs != null) {
            touchSubs.unsubscribe();
            touchSubs = null;
        }
    }

    private void updateTouchSubscription() {
        if (getLevel() == null || getLevel().isClientSide) return;
        boolean port = getMonitor() != null;
        if (port && touchSubs == null) {
            touchSubs = subscribeServerTick(this::touchTick);
        } else if (!port && touchSubs != null) {
            touchSubs.unsubscribe();
            touchSubs = null;
        }
    }

    private void touchTick() {
        if (getOffsetTimer() % TOUCH_INTERVAL != 0) return;
        CentralMonitorMachine monitor = getMonitor();
        if (monitor != null) MachineBusModule.handleTouches(monitor, this);
    }

    @Override
    public IGuiTexture getComponentIcon() {
        return ICON;
    }

    //////////////////////////////////////
    // ************ GUI *************//
    //////////////////////////////////////

    /** The name field over the settings: what the connector shares and which commands it takes. */
    @Override
    public Widget createUIWidget() {
        var group = new WidgetGroup(0, 0, 182, 160);
        group.addWidget(new LabelWidget(4, 5, Component.translatable("af9.bus.connector.label")));
        group.addWidget(new TextFieldWidget(52, 3, 126, 12, () -> label, this::setLabel)
                .setMaxStringLength(MAX_LABEL));
        boolean client = getLevel() != null && getLevel().isClientSide;
        group.addWidget(scrolling(0, 19, 182, 141, new ComponentPanelWidget(4, 1, this::addDisplayText)
                .textSupplier(client ? null : this::addDisplayText)
                .setMaxWidthLimit(172)
                .clickHandler(this::handleDisplayClick)));
        return group;
    }

    /**
     * A text panel in a box of its own size that scrolls (mouse wheel, or drag the bar) when the text runs longer; the
     * bus screens' text grows with the bus.
     */
    public static DraggableScrollableWidgetGroup scrolling(int x, int y, int width, int height, Widget text) {
        DraggableScrollableWidgetGroup box = new DraggableScrollableWidgetGroup(x, y, width, height)
                .setYScrollBarWidth(3)
                .setYBarStyle(null, new ColorRectTexture(0x80FFFFFF).setRadius(1));
        box.addWidget(text);
        return box;
    }

    private void setLabel(String text) {
        String trimmed = text == null ? "" : text.strip();
        if (trimmed.length() > MAX_LABEL) trimmed = trimmed.substring(0, MAX_LABEL);
        if (trimmed.equals(label)) return;
        label = trimmed;
        markDirty();
    }

    private void addDisplayText(List<Component> text) {
        CentralMonitorMachine monitor = getMonitor();
        BusControllerMachine busController = getBusController();
        IMultiController machine = getMachineController();
        if (monitor != null) {
            text.add(Component.translatable("af9.bus.connector.monitor_port").withStyle(ChatFormatting.AQUA));
        } else if (busController != null) {
            text.add(Component.translatable("af9.bus.connector.controller_port").withStyle(ChatFormatting.AQUA));
        } else if (machine != null) {
            text.add(Component.translatable("af9.bus.connector.machine_port",
                    Component.translatable(machine.self().getDefinition().getDescriptionId()))
                    .withStyle(ChatFormatting.AQUA));
        } else {
            text.add(Component.translatable("af9.bus.connector.no_machine").withStyle(ChatFormatting.GRAY));
        }
        BusNetwork.Bus bus = getBus();
        BusNetwork.Net net = getNetwork();
        int all = bus.connectors().size() - 1;
        text.add(all <= 0 ? Component.translatable("af9.bus.connector.alone").withStyle(ChatFormatting.GRAY) :
                Component.translatable("af9.bus.connector.bus", all, bus.machines(), BusNetwork.MAX_MACHINES));
        if (bus.overloaded()) {
            text.add(Component.translatable("af9.bus.connector.overloaded", BusNetwork.MAX_MACHINES)
                    .withStyle(ChatFormatting.RED));
        }
        int hatches = 0, research = 0;
        for (BusNetwork.Bus other : net.buses()) {
            if (other.overloaded()) continue;
            hatches += other.computation().size();
            research += other.data().size();
        }
        text.add(Component.translatable("af9.bus.connector.sources", computation.getMaxCWUt(), BusNetwork.MAX_CWUT,
                hatches, research).withStyle(ChatFormatting.GRAY));
        if (!net.controllers().isEmpty()) {
            text.add(Component.translatable("af9.bus.connector.network", net.buses().size(),
                    net.controllers().size()).withStyle(ChatFormatting.GRAY));
        }
        if (monitor != null) {
            text.add(Component.translatable("af9.bus.connector.monitor_hint").withStyle(ChatFormatting.DARK_GRAY));
            return;
        }
        if (busController != null) return;
        text.add(Component.translatable("af9.bus.connector.shares").withStyle(ChatFormatting.GOLD));
        text.add(toggles(BusData.DATA_KEYS, "af9.bus.data.", sharedData, "d"));
        text.add(Component.translatable("af9.bus.connector.accepts").withStyle(ChatFormatting.GOLD));
        text.add(toggles(BusData.COMMAND_KEYS, "af9.bus.command.", acceptedCommands, "c"));
        text.add(ComponentPanelWidget.withButton(Component.literal(controllerRefused ? "□ " : "■ ")
                .append(Component.translatable("af9.bus.connector.controller"))
                .withStyle(controllerRefused ? ChatFormatting.DARK_GRAY : ChatFormatting.GREEN), "controller"));
        ResourceLocation id = getRecipeId();
        if (id != null) {
            text.add(Component.translatable("af9.bus.connector.recipe", id.toString()).withStyle(ChatFormatting.GRAY));
            if (supplyStatus != null) {
                text.add(Component.translatable("af9.bus.controller.supply").withStyle(ChatFormatting.GOLD)
                        .append(" ").append(supplyStatus));
            }
        }
        text.add(Component.translatable("af9.bus.connector.hint").withStyle(ChatFormatting.DARK_GRAY));
    }

    /** One clickable box per bit: "■ Status  □ Energy ...". */
    static MutableComponent toggles(String[] keys, String langPrefix, int mask, String idPrefix) {
        MutableComponent line = Component.empty();
        for (int i = 0; i < keys.length; i++) {
            boolean on = (mask & (1 << i)) != 0;
            if (i > 0) line.append("  ");
            line.append(ComponentPanelWidget.withButton(Component.literal(on ? "■ " : "□ ")
                    .append(Component.translatable(langPrefix + keys[i]))
                    .withStyle(on ? ChatFormatting.GREEN : ChatFormatting.DARK_GRAY), idPrefix + i));
        }
        return line;
    }

    private void handleDisplayClick(String id, ClickData click) {
        if (click.isRemote) return;
        if (id.equals("controller")) {
            controllerRefused = !controllerRefused;
            markDirty();
            return;
        }
        if (id.length() < 2) return;
        int bit;
        try {
            bit = 1 << Integer.parseInt(id.substring(1));
        } catch (NumberFormatException e) {
            return;
        }
        if (id.charAt(0) == 'd') sharedData = (sharedData ^ bit) & BusData.ALL_DATA;
        else if (id.charAt(0) == 'c') acceptedCommands = (acceptedCommands ^ bit) & BusData.ALL_COMMANDS;
        else return;
        markDirty();
    }

    /** The connector at a position, if there is one. */
    public static BusConnectorPartMachine at(Level level, long pos) {
        return MetaMachine.getMachine(level, BlockPos.of(pos)) instanceof BusConnectorPartMachine connector ?
                connector : null;
    }
}
