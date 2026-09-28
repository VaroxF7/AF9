package com.af9.core.machine.console;

import com.af9.core.bus.BusConnectorPartMachine;

import com.gregtechceu.gtceu.api.machine.feature.multiblock.IMultiController;

import com.lowdragmc.lowdraglib.gui.widget.Widget;
import com.lowdragmc.lowdraglib.gui.widget.WidgetGroup;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * The strip under an AF9 machine's console while a Bus Connector sits in the machine: on a bus, "This Machine is Part
 * of a BUS" and its BUS MACHINE ID (the connector's name, its address on the bus); else that the connector is on no
 * bus. Without a connector the page has no strip ({@link #wrap}); the machine runs as any multiblock either way.
 */
public class BusBannerWidget extends ConsoleWidget {

    public static final int HEIGHT = 24;

    private final IMultiController machine;
    private boolean connector;
    private boolean onBus;
    private String id = "";

    public BusBannerWidget(IMultiController machine, int x, int y, int width) {
        super(x, y, width, HEIGHT);
        this.machine = machine;
    }

    /** The page with the strip under it if the machine has a Bus Connector; else the page as it is. */
    public static Widget wrap(Widget page, IMultiController machine) {
        if (BusConnectorPartMachine.of(machine) == null) return page;
        int width = page.getSize().width;
        int height = page.getSize().height;
        var group = new WidgetGroup(0, 0, width, height + HEIGHT);
        group.addWidget(page);
        group.addWidget(new BusBannerWidget(machine, 0, height, width));
        return group;
    }

    @Override
    protected boolean sample() {
        BusConnectorPartMachine port = BusConnectorPartMachine.of(machine);
        boolean hasPort = port != null;
        boolean bus = hasPort && port.isOnBus();
        String name = hasPort ? port.getLabel() : "";
        boolean changed = hasPort != connector || bus != onBus || !name.equals(id);
        connector = hasPort;
        onBus = bus;
        id = name;
        return changed;
    }

    @Override
    protected void writeState(FriendlyByteBuf buffer) {
        buffer.writeBoolean(connector);
        buffer.writeBoolean(onBus);
        buffer.writeUtf(id);
    }

    @Override
    protected void readState(FriendlyByteBuf buffer) {
        connector = buffer.readBoolean();
        onBus = buffer.readBoolean();
        id = buffer.readUtf();
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void drawInBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        int x0 = getPosition().x;
        int y0 = getPosition().y;
        int width = getSize().width;
        graphics.fill(x0, y0 + 2, x0 + width, y0 + HEIGHT, BG);
        border(graphics, x0, y0 + 2, width, HEIGHT - 2, onBus ? withAlpha(INFO, 0xAA) : EDGE);
        if (!connector) return;
        if (!onBus) {
            graphics.drawString(font(), fit(Component.translatable("af9.bus.not_on_bus").getString(), width - 12),
                    x0 + 6, y0 + 9, MUTED, false);
            return;
        }
        graphics.fill(x0 + 5, y0 + 6, x0 + 9, y0 + 10, INFO);
        graphics.drawString(font(), fit(Component.translatable("af9.bus.part_of_bus").getString(), width - 20),
                x0 + 13, y0 + 4, INFO, false);
        String shown = id.isEmpty() ? Component.translatable("af9.bus.machine_id.none").getString() : id;
        graphics.drawString(font(), fit(Component.translatable("af9.bus.machine_id", shown).getString(), width - 20),
                x0 + 13, y0 + 14, id.isEmpty() ? MUTED : TEXT, false);
    }
}
