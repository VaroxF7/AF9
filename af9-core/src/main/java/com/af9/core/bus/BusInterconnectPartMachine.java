package com.af9.core.bus;

import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.gregtechceu.gtceu.api.machine.feature.multiblock.IMultiController;
import com.gregtechceu.gtceu.api.machine.multiblock.PartAbility;
import com.gregtechceu.gtceu.api.machine.multiblock.part.MultiblockPartMachine;

import com.lowdragmc.lowdraglib.gui.widget.ComponentPanelWidget;
import com.lowdragmc.lowdraglib.gui.widget.Widget;
import com.lowdragmc.lowdraglib.gui.widget.WidgetGroup;
import com.lowdragmc.lowdraglib.syncdata.field.ManagedFieldHolder;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Interconnect Hatch: a Bus Controller's link to other Bus Controllers. Every Interconnect Hatch on one run of Optical
 * Bus Cable (from its front face) links its controller to the others' ({@link BusNetwork#walkInterconnects}); linked
 * controllers form one network: computation and research flow between all their buses, each lists and sets recipes
 * for every machine on it, and a machine is supplied from its own controller's inputs first, then the others'. An
 * Interconnect Hatch only talks to Interconnect Hatches: Bus Connectors on the same cable do not see it.
 */
public class BusInterconnectPartMachine extends MultiblockPartMachine {

    protected static final ManagedFieldHolder MANAGED_FIELD_HOLDER = new ManagedFieldHolder(
            BusInterconnectPartMachine.class, MultiblockPartMachine.MANAGED_FIELD_HOLDER);

    /** Registered by the hatch's definition (KubeJS), taken by the Bus Controller's pattern. */
    public static final PartAbility BUS_INTERCONNECT = new PartAbility("af9_bus_interconnect");
    /** Ticks a walk of the link is kept. */
    private static final int CACHE_TICKS = 20;

    private List<BusInterconnectPartMachine> linked = List.of();
    /** Game time of the last walk, -1 before the first. */
    private long linkedTime = -1;

    public BusInterconnectPartMachine(IMachineBlockEntity holder) {
        super(holder);
    }

    @Override
    public ManagedFieldHolder getFieldHolder() {
        return MANAGED_FIELD_HOLDER;
    }

    /** Only one controller's link. */
    @Override
    public boolean canShared() {
        return false;
    }

    /** The Bus Controller this hatch is part of, or null. */
    public BusControllerMachine getBusController() {
        for (IMultiController controller : getControllers()) {
            if (controller instanceof BusControllerMachine busController) return busController;
        }
        return null;
    }

    /** The other Interconnect Hatches on this one's cable (walked at most once a second). */
    public List<BusInterconnectPartMachine> getLinked() {
        long now = getLevel() == null ? 0 : getLevel().getGameTime();
        if (linkedTime < 0 || now < linkedTime || now - linkedTime >= CACHE_TICKS) {
            linked = BusNetwork.walkInterconnects(this);
            linkedTime = now;
        }
        return linked;
    }

    /** The Bus Controllers of the other hatches on this one's cable. */
    public List<BusControllerMachine> getLinkedControllers() {
        List<BusControllerMachine> controllers = new ArrayList<>();
        for (BusInterconnectPartMachine hatch : getLinked()) {
            BusControllerMachine controller = hatch.getBusController();
            if (controller != null && !controllers.contains(controller)) controllers.add(controller);
        }
        return controllers;
    }

    @Override
    public Widget createUIWidget() {
        var group = new WidgetGroup(0, 0, 182, 80);
        boolean client = getLevel() != null && getLevel().isClientSide;
        group.addWidget(BusConnectorPartMachine.scrolling(0, 0, 182, 80, new ComponentPanelWidget(4, 5,
                this::addDisplayText)
                .textSupplier(client ? null : this::addDisplayText)
                .setMaxWidthLimit(172)));
        return group;
    }

    private void addDisplayText(List<Component> text) {
        BusControllerMachine own = getBusController();
        text.add(own == null ?
                Component.translatable("af9.bus.interconnect.no_controller").withStyle(ChatFormatting.GRAY) :
                Component.translatable("af9.bus.interconnect.port").withStyle(ChatFormatting.AQUA));
        int controllers = getLinkedControllers().size();
        text.add(controllers == 0 ?
                Component.translatable("af9.bus.interconnect.alone").withStyle(ChatFormatting.GRAY) :
                Component.translatable("af9.bus.interconnect.linked", controllers));
        if (own != null) {
            BusNetwork.Net net = own.getNetwork();
            text.add(Component.translatable("af9.bus.connector.network", net.buses().size(),
                    net.controllers().size()).withStyle(ChatFormatting.GRAY));
        }
        text.add(Component.translatable("af9.bus.interconnect.hint").withStyle(ChatFormatting.DARK_GRAY));
    }
}
