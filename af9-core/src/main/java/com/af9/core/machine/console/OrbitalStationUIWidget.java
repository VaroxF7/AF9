package com.af9.core.machine.console;

import com.gregtechceu.gtceu.api.gui.fancy.IFancyUIProvider;

/**
 * GT's machine screen around the Orbital Lithography Station's page ({@link OrbitalConsoleWidget}), with a panel on
 * each side of the player inventory: the process left, the system right ({@link SidePanelsUIWidget}).
 */
public class OrbitalStationUIWidget extends SidePanelsUIWidget<OrbitalConsoleWidget> {

    /** Page plus GT's border; the inventory row below it. */
    public static final int WIDTH = width(OrbitalConsoleWidget.WIDTH);
    public static final int HEIGHT = height(OrbitalConsoleWidget.HEIGHT);

    public OrbitalStationUIWidget(IFancyUIProvider mainPage) {
        super(mainPage, OrbitalConsoleWidget.WIDTH, OrbitalConsoleWidget.HEIGHT, OrbitalConsoleWidget.class,
                OrbitalConsoleWidget.SidePanel::new);
    }
}
