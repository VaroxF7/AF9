package com.af9.core.machine.console;

import com.af9.core.machine.LithoConsoleWidget;
import com.af9.core.machine.OrbitalLithographyMachine;
import com.af9.core.machine.ParticleAcceleratorMachine;
import com.af9.core.machine.PhotolithographyLineMachine;
import com.af9.core.machine.PhotolithographyScannerMachine;
import com.af9.core.machine.ProcessMachine;
import com.af9.core.machine.fab.FabConsoleWidget;
import com.af9.core.machine.fab.FabMultiblockMachine;

import com.gregtechceu.gtceu.api.machine.MetaMachine;

import com.lowdragmc.lowdraglib.gui.widget.Widget;

/**
 * The console of a machine as a Central Monitor draws it (the Machine Bus Module's console view): the same console
 * widget as on the machine's own screen, on its own, at 0, 0. The server's copy takes the state ({@link BusConsole}),
 * the client's (made for the client's copy of the machine) draws it.
 */
public final class BusConsoles {

    private BusConsoles() {}

    /** The machine's console, a {@link BusConsole} too; null for a machine without one (GT's). */
    public static Widget create(MetaMachine machine) {
        if (machine instanceof PhotolithographyScannerMachine scanner) return new ScannerConsoleWidget(scanner, 0, 0);
        if (machine instanceof OrbitalLithographyMachine orbital) return new OrbitalConsoleWidget(orbital, 0, 0);
        if (machine instanceof PhotolithographyLineMachine line) return new LithoConsoleWidget(line, 0, 0);
        if (machine instanceof ParticleAcceleratorMachine accelerator) {
            return new AcceleratorConsoleWidget(accelerator, 0, 0);
        }
        if (machine instanceof ProcessMachine process) return new ProcessConsoleWidget(process, 0, 0);
        if (machine instanceof FabMultiblockMachine fab) return new FabConsoleWidget(fab, 0, 0, false);
        return null;
    }
}
