package com.af9.core.machine.console;

/**
 * A machine console a Central Monitor can draw ({@link BusConsoles}): the server takes its whole state as bytes, the
 * module sends them with its data, and the client's copy of the console takes them and draws as on the machine's
 * screen (com.af9.core.client.render.MachineBusRenderer).
 */
public interface BusConsole {

    /** Server: the console's whole state now. */
    byte[] snapshot();

    /** Client: the state out of a {@link #snapshot}. */
    void applySnapshot(byte[] bytes);
}
