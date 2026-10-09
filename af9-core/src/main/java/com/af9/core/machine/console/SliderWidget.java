package com.af9.core.machine.console;

import com.lowdragmc.lowdraglib.gui.widget.Widget;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.function.DoubleConsumer;

/**
 * A slider for the AF9 pages (LDLib has none): a track, its filled part and a knob. A click anywhere on the track
 * sets the value, and so does dragging where the screen passes drags on (a machine's screen does; the recipe viewers
 * only pass clicks). It lives on the client alone: the value goes to whoever listens, nothing is sent.
 */
public class SliderWidget extends Widget {

    private static final int TRACK = 0xFF1B2230, EDGE = 0xFF5A6472, FILL = 0xFFFFC857, KNOB = 0xFFFFFFFF;

    private final DoubleConsumer onChange;
    private double min = 0, max = 1, value = 0;
    private boolean dragging;

    public SliderWidget(DoubleConsumer onChange, int x, int y, int width, int height) {
        super(x, y, width, height);
        this.onChange = onChange;
        setClientSideWidget();
    }

    public SliderWidget setRange(double min, double max) {
        this.min = Math.min(min, max);
        this.max = Math.max(min, max);
        this.value = Mth.clamp(value, this.min, this.max);
        return this;
    }

    /** Sets the value and tells the listener. */
    public SliderWidget setValue(double value) {
        this.value = Mth.clamp(value, min, max);
        if (onChange != null) onChange.accept(this.value);
        return this;
    }

    public double getValue() {
        return value;
    }

    @OnlyIn(Dist.CLIENT)
    private void setFromMouse(double mouseX) {
        int left = getPosition().x + 2;
        int width = Math.max(1, getSize().width - 4);
        setValue(min + (max - min) * Mth.clamp((mouseX - left) / width, 0, 1));
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0 || !isMouseOverElement(mouseX, mouseY)) return false;
        dragging = true;
        setFromMouse(mouseX);
        playButtonClickSound();
        return true;
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (!dragging) return false;
        setFromMouse(mouseX);
        return true;
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        dragging = false;
        return false;
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void drawInBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        int x = getPosition().x, y = getPosition().y, width = getSize().width, height = getSize().height;
        int middle = y + height / 2;
        graphics.fill(x, middle - 2, x + width, middle + 2, EDGE);
        graphics.fill(x + 1, middle - 1, x + width - 1, middle + 1, TRACK);
        double share = max > min ? (value - min) / (max - min) : 0;
        int knob = x + 2 + (int) Math.round(share * (width - 4));
        graphics.fill(x + 1, middle - 1, knob, middle + 1, FILL);
        graphics.fill(knob - 2, y, knob + 2, y + height, EDGE);
        graphics.fill(knob - 1, y + 1, knob + 1, y + height - 1, KNOB);
        super.drawInBackground(graphics, mouseX, mouseY, partialTicks);
    }
}
