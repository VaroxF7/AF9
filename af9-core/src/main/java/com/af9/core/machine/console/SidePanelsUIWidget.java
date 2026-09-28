package com.af9.core.machine.console;

import com.gregtechceu.gtceu.api.gui.fancy.FancyMachineUIWidget;
import com.gregtechceu.gtceu.api.gui.fancy.IFancyUIProvider;

import com.lowdragmc.lowdraglib.gui.widget.Widget;

import java.util.List;

/**
 * GT's machine screen (title bar, side tabs, configurators, player inventory) around a console page, with a panel on
 * each side of the player inventory: the process left, the system right, both drawn from the page's console. The
 * panels only show on the machine's own page, not on its parts' pages. Used by the Orbital Lithography Station and the
 * Particle Accelerator.
 *
 * @param <C> the console on the page, which the panels read
 */
public class SidePanelsUIWidget<C extends ConsoleWidget> extends FancyMachineUIWidget {

    /** Makes a side panel: the process (system false) or the system panel, at the given bounds. */
    @FunctionalInterface
    public interface PanelFactory<C> {

        Widget create(C console, boolean system, int x, int y, int width, int height);
    }

    /** Page plus GT's border; the inventory row below it. */
    public static int width(int pageWidth) {
        return pageWidth + 8;
    }

    public static int height(int pageHeight) {
        return pageHeight + 8 + 86;
    }

    private final Class<C> consoleType;
    private final PanelFactory<C> panels;
    private Widget process;
    private Widget system;
    private BusPlacardWidget placard;

    public SidePanelsUIWidget(IFancyUIProvider mainPage, int pageWidth, int pageHeight, Class<C> consoleType,
                              PanelFactory<C> panels) {
        super(mainPage, width(pageWidth), height(pageHeight));
        this.consoleType = consoleType;
        this.panels = panels;
    }

    @Override
    protected void setupFancyUI(IFancyUIProvider fancyUI, boolean showInventory) {
        super.setupFancyUI(fancyUI, showInventory);
        if (process != null) {
            removeWidget(process);
            removeWidget(system);
            process = null;
            system = null;
        }
        placard = null;
        if (fancyUI != mainPage || !showInventory || playerInventory == null) return;
        List<C> consoles = pageContainer.getWidgetsByType(consoleType);
        if (consoles.isEmpty()) return;
        C console = consoles.get(0);
        int invX = playerInventory.getSelfPosition().x;
        int invY = playerInventory.getSelfPosition().y;
        int invW = playerInventory.getSize().width;
        int height = playerInventory.getSize().height - 6;
        int right = invX + invW + 4;
        addWidget(process = panels.create(console, false, 4, invY + 2, invX - 8, height));
        addWidget(system = panels.create(console, true, right, invY + 2, getSize().width - right - 4, height));
        List<BusPlacardWidget> placards = pageContainer.getWidgetsByType(BusPlacardWidget.class);
        placard = placards.isEmpty() ? null : placards.get(0);
    }

    /** The panels hide with the page while the machine is part of a bus ({@link BusPlacardWidget}). */
    @Override
    public void updateScreen() {
        super.updateScreen();
        boolean locked = placard != null && placard.isLocked();
        if (process != null) process.setVisible(!locked);
        if (system != null) system.setVisible(!locked);
    }
}
