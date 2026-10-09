package com.af9.core.machine;

import com.lowdragmc.lowdraglib.gui.texture.ProgressTexture;
import com.lowdragmc.lowdraglib.gui.texture.ResourceTexture;

/**
 * The recipe pages' progress arrow of the Eye of Harmony layout: pointing down, from the markers at the top to the slots under
 * them (GT's own arrow points right, at nothing there). A square picture of two 16 x 32 halves, the empty arrow and the filled one.
 */
public final class DownArrow {

    private static final String TEXTURE = "af9:textures/gui/progress_down.png";

    private DownArrow() {}

    public static ProgressTexture texture() {
        var texture = new ProgressTexture(new ResourceTexture(TEXTURE).getSubTexture(0, 0, 0.5, 1),
                new ResourceTexture(TEXTURE).getSubTexture(0.5, 0, 0.5, 1));
        texture.setFillDirection(ProgressTexture.FillDirection.UP_TO_DOWN);
        return texture;
    }
}
