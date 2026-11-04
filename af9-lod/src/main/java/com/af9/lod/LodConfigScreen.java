package com.af9.lod;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraftforge.common.ForgeConfigSpec;

/** Settings screen: enable, debug draw, max distance, near band. Opened from the mod list's Config button. */
public final class LodConfigScreen extends Screen {
    private final Screen parent;

    public LodConfigScreen(Screen parent) {
        super(Component.literal("AF9 Horizon"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int x = width / 2 - 100, y = height / 4;
        addRenderableWidget(toggle(x, y, "GPU LOD renderer", LodConfig.ENABLED));
        addRenderableWidget(toggle(x, y + 24, "Debug draw", LodConfig.DEBUG_DRAW));
        addRenderableWidget(new IntSlider(x, y + 48, "Max distance (chunks)", LodConfig.MAX_DISTANCE_CHUNKS, 16, 1024));
        addRenderableWidget(new IntSlider(x, y + 72, "Near band (chunks)", LodConfig.NEAR_BAND_CHUNKS, 0, 256));
        addRenderableWidget(Button.builder(Component.literal("Done"), b -> onClose())
                .bounds(x, y + 108, 200, 20).build());
    }

    private static Button toggle(int x, int y, String label, ForgeConfigSpec.BooleanValue value) {
        return Button.builder(text(label, value.get()), b -> {
            boolean now = !value.get();
            value.set(now);
            value.save();
            b.setMessage(text(label, now));
        }).bounds(x, y, 200, 20).build();
    }

    private static Component text(String label, boolean on) {
        return Component.literal(label + ": " + (on ? "ON" : "OFF"));
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partial) {
        renderBackground(g);
        g.drawCenteredString(font, title, width / 2, height / 4 - 24, 0xFFFFFF);
        super.render(g, mouseX, mouseY, partial);
    }

    @Override
    public void onClose() {
        Minecraft.getInstance().setScreen(parent);
    }

    private static final class IntSlider extends AbstractSliderButton {
        private final String label;
        private final ForgeConfigSpec.IntValue cfg;
        private final int min, max;

        IntSlider(int x, int y, String label, ForgeConfigSpec.IntValue cfg, int min, int max) {
            super(x, y, 200, 20, Component.empty(), (cfg.get() - min) / (double) (max - min));
            this.label = label;
            this.cfg = cfg;
            this.min = min;
            this.max = max;
            updateMessage();
        }

        private int current() {
            return min + (int) Math.round(this.value * (max - min));
        }

        @Override
        protected void updateMessage() {
            setMessage(Component.literal(label + ": " + current()));
        }

        @Override
        protected void applyValue() {
            cfg.set(current());
            cfg.save();
        }
    }
}
