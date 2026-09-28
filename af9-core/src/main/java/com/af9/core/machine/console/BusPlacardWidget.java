package com.af9.core.machine.console;

import com.af9.core.bus.BusConnectorPartMachine;

import com.gregtechceu.gtceu.api.machine.feature.multiblock.IMultiController;

import com.lowdragmc.lowdraglib.gui.widget.Widget;
import com.lowdragmc.lowdraglib.gui.widget.WidgetGroup;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * The card over a machine's own screen while it is part of a bus: its Bus Connector (the machine's port) is on a bus
 * ({@link BusConnectorPartMachine#isOnBus}). The screen then shows only "This Machine is Part of a BUS" and its BUS
 * MACHINE ID (the connector's name, its address on the bus); the page under it is hidden and takes no clicks, and the
 * machine is run from the Central Monitor. Off the bus the page is the machine's own, as any multiblock's. Lays itself
 * over the page it wraps ({@link #wrap}); AF9's consoles and (a Mixin) GT's multiblock screens use it.
 */
public class BusPlacardWidget extends ConsoleWidget {

    private final IMultiController machine;
    private final Widget page;
    private boolean onBus;
    private String id = "";

    public BusPlacardWidget(IMultiController machine, Widget page) {
        super(0, 0, page.getSize().width, page.getSize().height);
        this.machine = machine;
        this.page = page;
    }

    /** The page with the card over it if the machine has a Bus Connector as its port; else the page as it is. */
    public static Widget wrap(Widget page, IMultiController machine) {
        BusConnectorPartMachine port = BusConnectorPartMachine.of(machine);
        if (port == null || port.getMachineController() != machine) return page;
        var group = new WidgetGroup(0, 0, page.getSize().width, page.getSize().height);
        group.addWidget(page);
        group.addWidget(new BusPlacardWidget(machine, page));
        return group;
    }

    /** Whether the card covers the page now. */
    public boolean isLocked() {
        return onBus;
    }

    @Override
    protected boolean sample() {
        BusConnectorPartMachine port = BusConnectorPartMachine.of(machine);
        boolean bus = port != null && port.isOnBus();
        String name = port == null ? "" : port.getLabel();
        boolean changed = bus != onBus || !name.equals(id);
        onBus = bus;
        id = name;
        return changed;
    }

    @Override
    protected void writeState(FriendlyByteBuf buffer) {
        buffer.writeBoolean(onBus);
        buffer.writeUtf(id);
    }

    @Override
    protected void readState(FriendlyByteBuf buffer) {
        onBus = buffer.readBoolean();
        id = buffer.readUtf();
    }

    @Override
    public void updateScreen() {
        super.updateScreen();
        boolean locked = isLocked();
        page.setVisible(!locked);
        page.setActive(!locked);
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void drawInBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        if (!isLocked()) return;
        Font font = font();
        int x0 = getPosition().x;
        int y0 = getPosition().y;
        int width = getSize().width;
        int height = getSize().height;
        graphics.fill(x0, y0, x0 + width, y0 + height, BG);
        border(graphics, x0, y0, width, height, EDGE);
        border(graphics, x0 + 3, y0 + 3, width - 6, height - 6, withAlpha(INFO, 0x88));
        int cx = x0 + width / 2;
        int cy = y0 + height / 2;

        String title = Component.translatable("af9.bus.part_of_bus").getString();
        drawScaled(graphics, font, title, cx, cy - 20, Math.min(2F, (width - 24F) / font.width(title)), INFO);
        String shown = id.isEmpty() ? Component.translatable("af9.bus.machine_id.none").getString() : id;
        String line = Component.translatable("af9.bus.machine_id", shown).getString();
        drawScaled(graphics, font, line, cx, cy + 4, Math.min(1.5F, (width - 24F) / font.width(line)),
                id.isEmpty() ? MUTED : TEXT);
        String hint = Component.translatable("af9.bus.placard.hint").getString();
        drawSmall(graphics, fit(hint, (width - 16) * 4 / 3), cx, y0 + height - 14, MUTED, true);
    }

    /** Text centred on {@code cx} at a scale, its top at {@code y}. */
    @OnlyIn(Dist.CLIENT)
    private static void drawScaled(GuiGraphics graphics, Font font, String text, int cx, int y, float scale,
                                   int color) {
        graphics.pose().pushPose();
        graphics.pose().translate(cx, y, 0);
        graphics.pose().scale(scale, scale, 1F);
        graphics.drawString(font, text, -font.width(text) / 2, 0, color, false);
        graphics.pose().popPose();
    }
}
