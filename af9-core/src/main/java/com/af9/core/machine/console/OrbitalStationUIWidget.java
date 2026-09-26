package com.af9.core.machine.console;

import com.gregtechceu.gtceu.api.gui.fancy.FancyMachineUIWidget;
import com.gregtechceu.gtceu.api.gui.fancy.IFancyUIProvider;

import java.util.List;

/**
 * GT's machine screen (title bar, side tabs, configurators, player inventory) around the Orbital Lithography
 * Station's page ({@link OrbitalConsoleWidget}), with a panel on each side of the player inventory: the process
 * left, the system right. The panels only show on the station's own page, not on its parts' pages.
 */
public class OrbitalStationUIWidget extends FancyMachineUIWidget {

    /** Page plus GT's border; the inventory row below it. */
    public static final int WIDTH = OrbitalConsoleWidget.WIDTH + 8;
    public static final int HEIGHT = OrbitalConsoleWidget.HEIGHT + 8 + 86;

    private OrbitalConsoleWidget.SidePanel process;
    private OrbitalConsoleWidget.SidePanel system;

    public OrbitalStationUIWidget(IFancyUIProvider mainPage) {
        super(mainPage, WIDTH, HEIGHT);
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
        if (fancyUI != mainPage || !showInventory || playerInventory == null) return;
        List<OrbitalConsoleWidget> consoles = pageContainer.getWidgetsByType(OrbitalConsoleWidget.class);
        if (consoles.isEmpty()) return;
        OrbitalConsoleWidget console = consoles.get(0);
        int invX = playerInventory.getSelfPosition().x;
        int invY = playerInventory.getSelfPosition().y;
        int invW = playerInventory.getSize().width;
        int height = playerInventory.getSize().height - 6;
        int right = invX + invW + 4;
        addWidget(process = new OrbitalConsoleWidget.SidePanel(console, false, 4, invY + 2, invX - 8, height));
        addWidget(system = new OrbitalConsoleWidget.SidePanel(console, true, right, invY + 2,
                getSize().width - right - 4, height));
    }
}
