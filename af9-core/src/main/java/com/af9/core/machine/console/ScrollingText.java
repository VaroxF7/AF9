package com.af9.core.machine.console;

import com.lowdragmc.lowdraglib.gui.texture.ColorRectTexture;
import com.lowdragmc.lowdraglib.gui.widget.DraggableScrollableWidgetGroup;
import com.lowdragmc.lowdraglib.gui.widget.Widget;

/** A text panel that scrolls, for the screens whose text grows (the computation arrays, the CPU array, the CWU Server). */
public final class ScrollingText {

    private ScrollingText() {}

    /**
     * A text panel in a box of its own size that scrolls (mouse wheel, or drag the bar) when the text runs longer.
     */
    public static DraggableScrollableWidgetGroup box(int x, int y, int width, int height, Widget text) {
        DraggableScrollableWidgetGroup box = new DraggableScrollableWidgetGroup(x, y, width, height)
                .setYScrollBarWidth(3)
                .setYBarStyle(null, new ColorRectTexture(0x80FFFFFF).setRadius(1));
        box.addWidget(text);
        return box;
    }
}
