package com.af9.core.bus;

import com.gregtechceu.gtceu.api.capability.IMonitorComponent;
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.gregtechceu.gtceu.api.machine.MetaMachine;
import com.gregtechceu.gtceu.api.machine.TickableSubscription;
import com.gregtechceu.gtceu.api.machine.feature.multiblock.IMultiController;
import com.gregtechceu.gtceu.api.machine.feature.multiblock.IWorkableMultiController;
import com.gregtechceu.gtceu.api.machine.multiblock.PartAbility;
import com.gregtechceu.gtceu.api.machine.multiblock.part.MultiblockPartMachine;
import com.gregtechceu.gtceu.common.machine.multiblock.electric.CentralMonitorMachine;

import com.lowdragmc.lowdraglib.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib.gui.texture.ResourceTexture;
import com.lowdragmc.lowdraglib.gui.util.ClickData;
import com.lowdragmc.lowdraglib.gui.widget.ComponentPanelWidget;
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

import java.util.List;

/**
 * Bus Connector: the port of a machine on the machine bus (a data center's bus connector). One sits in a machine's
 * structure (the AF9 multiblocks take one: {@link #BUS_CONNECTOR}) or in the wall of GT's Central Monitor
 * ({@link AF9Bus#installMonitorWall}); Polycat Cable from its front face joins it to the others ({@link BusNetwork}).
 * <p>
 * On a machine it shares what its screen allows ({@link #sharedData}, bits of {@link BusData}) and takes the commands
 * its screen allows ({@link #acceptedCommands}): the Central Monitor's Machine Bus Module reads and sends them. It
 * counts the machine's finished runs. In a Central Monitor it is the monitor's port: it answers the taps on the
 * Advanced Monitors of the module's screens ({@link MachineBusModule#handleTouches}).
 */
@SuppressWarnings("removal") // new ResourceLocation(ns, path) is the only constructor on 1.20.1
public class BusConnectorPartMachine extends MultiblockPartMachine implements IMonitorComponent {

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

    private List<BusConnectorPartMachine> bus = List.of();
    private long busTime = Long.MIN_VALUE;
    private TickableSubscription touchSubs;

    public BusConnectorPartMachine(IMachineBlockEntity holder) {
        super(holder);
    }

    @Override
    public ManagedFieldHolder getFieldHolder() {
        return MANAGED_FIELD_HOLDER;
    }

    //////////////////////////////////////
    // ********** The machine **********//
    //////////////////////////////////////

    /** The machine this connector is the port of (not a Central Monitor), or null. */
    public IMultiController getMachineController() {
        for (IMultiController controller : getControllers()) {
            if (!(controller instanceof CentralMonitorMachine)) return controller;
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
        markDirty();
        return super.afterWorking(controller);
    }

    //////////////////////////////////////
    // ************ The bus ************//
    //////////////////////////////////////

    /** Every connector on this one's bus, this one first (walked at most once a second). */
    public List<BusConnectorPartMachine> getBus() {
        long now = getLevel() == null ? 0 : getLevel().getGameTime();
        if (now - busTime >= BUS_CACHE_TICKS || now < busTime) {
            bus = BusNetwork.walk(this);
            busTime = now;
        }
        return bus;
    }

    /** The machine ports on this one's bus (other monitors' ports and empty connectors left out). */
    public List<BusConnectorPartMachine> getMachinesOnBus() {
        return getBus().stream()
                .filter(c -> c != this && !c.isInValid() && c.getMachineController() != null)
                .toList();
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
        group.addWidget(new ComponentPanelWidget(4, 20, this::addDisplayText)
                .textSupplier(client ? null : this::addDisplayText)
                .setMaxWidthLimit(174)
                .clickHandler(this::handleDisplayClick));
        return group;
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
        IMultiController machine = getMachineController();
        if (monitor != null) {
            text.add(Component.translatable("af9.bus.connector.monitor_port").withStyle(ChatFormatting.AQUA));
        } else if (machine != null) {
            text.add(Component.translatable("af9.bus.connector.machine_port",
                    Component.translatable(machine.self().getDefinition().getDescriptionId()))
                    .withStyle(ChatFormatting.AQUA));
        } else {
            text.add(Component.translatable("af9.bus.connector.no_machine").withStyle(ChatFormatting.GRAY));
        }
        int machines = getMachinesOnBus().size();
        int all = getBus().size() - 1;
        text.add(all <= 0 ? Component.translatable("af9.bus.connector.alone").withStyle(ChatFormatting.GRAY) :
                Component.translatable("af9.bus.connector.bus", all, machines));
        if (monitor != null) {
            text.add(Component.translatable("af9.bus.connector.monitor_hint").withStyle(ChatFormatting.DARK_GRAY));
            return;
        }
        text.add(Component.translatable("af9.bus.connector.shares").withStyle(ChatFormatting.GOLD));
        text.add(toggles(BusData.DATA_KEYS, "af9.bus.data.", sharedData, "d"));
        text.add(Component.translatable("af9.bus.connector.accepts").withStyle(ChatFormatting.GOLD));
        text.add(toggles(BusData.COMMAND_KEYS, "af9.bus.command.", acceptedCommands, "c"));
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
        if (click.isRemote || id.length() < 2) return;
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
