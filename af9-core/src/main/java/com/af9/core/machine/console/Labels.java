package com.af9.core.machine.console;

import com.lowdragmc.lowdraglib.gui.widget.LabelWidget;

import java.util.function.Supplier;

/**
 * Labels for the pages the KubeJS scripts build. LDLib's {@link LabelWidget} takes a String, a Component or a
 * Supplier in the same place, and a script's string fits two of the three: Rhino refuses to pick ("the choice of Java
 * constructor ... is ambiguous") and the page does not build. These take one kind each.
 */
public final class Labels {

    private Labels() {}

    /** A label with a fixed text. */
    public static LabelWidget of(int x, int y, String text) {
        return new LabelWidget(x, y, text);
    }

    /** A label whose text is asked for anew each frame (a script's function may return any kind of string). */
    public static LabelWidget live(int x, int y, Supplier<?> text) {
        return new LabelWidget(x, y, () -> String.valueOf(text.get()));
    }
}
