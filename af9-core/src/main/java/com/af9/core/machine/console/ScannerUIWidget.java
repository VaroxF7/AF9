package com.af9.core.machine.console;

import com.gregtechceu.gtceu.api.gui.fancy.IFancyUIProvider;

/**
 * GT's machine screen around the Photolithography Scanner's page ({@link ScannerConsoleWidget}), with a panel on each
 * side of the player inventory: the process left, the system right ({@link SidePanelsUIWidget}).
 */
public class ScannerUIWidget extends SidePanelsUIWidget<ScannerConsoleWidget> {

    /** Page plus GT's border; the inventory row below it. */
    public static final int WIDTH = width(ScannerConsoleWidget.WIDTH);
    public static final int HEIGHT = height(ScannerConsoleWidget.HEIGHT);

    public ScannerUIWidget(IFancyUIProvider mainPage) {
        super(mainPage, ScannerConsoleWidget.WIDTH, ScannerConsoleWidget.HEIGHT, ScannerConsoleWidget.class,
                ScannerConsoleWidget.SidePanel::new);
    }
}
