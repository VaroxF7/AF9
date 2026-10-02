package com.af9.core.elevator;

import com.af9.core.machine.console.ScrollingText;

import com.gregtechceu.gtceu.api.gui.GuiTextures;

import com.lowdragmc.lowdraglib.gui.texture.ColorRectTexture;
import com.lowdragmc.lowdraglib.gui.texture.GuiTextureGroup;
import com.lowdragmc.lowdraglib.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib.gui.texture.ProgressTexture;
import com.lowdragmc.lowdraglib.gui.texture.ResourceTexture;
import com.lowdragmc.lowdraglib.gui.widget.ComponentPanelWidget;
import com.lowdragmc.lowdraglib.gui.widget.ImageWidget;
import com.lowdragmc.lowdraglib.gui.widget.ProgressWidget;
import com.lowdragmc.lowdraglib.gui.widget.SwitchWidget;
import com.lowdragmc.lowdraglib.gui.widget.WidgetGroup;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import it.unimi.dsi.fastutil.booleans.BooleanConsumer;

import java.util.function.BooleanSupplier;

/**
 * The Space Elevator's screen, laid out as GTNH's (a TecTech controller without an inventory: a dark blue screen that
 * fills a plain window, the elevator's buttons on the screen's lower right, the power switch under it), and kept simple:
 * none of TecTech's parameters, LEDs or power pass, nothing to configure. On it:
 * <ul>
 * <li>the status text ({@link SpaceElevatorMachine#addDisplayText});</li>
 * <li>the <b>size</b> switch (GTNH's extension button): basic or extended structure;</li>
 * <li>an info sign with GTNH's contributors, and the elevator's logo;</li>
 * <li>under the screen, where TecTech has its LED strip: the run's progress, and the <b>power</b> switch.</li>
 * </ul>
 * The pictures of the size switch and the logo are drawn for AF9; the buttons themselves are GT's.
 */
public class SpaceElevatorScreen extends WidgetGroup {

    /** GTNH's window. */
    public static final int WIDTH = 198, HEIGHT = 192;
    private static final int RIM = 0xFF2A3F8F, PANE = 0xFF000A2E, PROGRESS = 0xFF2AA0C6;

    /** Two states, one over the other: the basic tower, the extended one. */
    private static final ResourceTexture SIZE = new ResourceTexture("af9:textures/gui/space_elevator/size.png");
    private static final ResourceTexture LOGO = new ResourceTexture("af9:textures/gui/space_elevator/logo.png");

    public SpaceElevatorScreen(SpaceElevatorMachine machine) {
        super(0, 0, WIDTH, HEIGHT);
        boolean client = machine.getLevel() != null && machine.getLevel().isClientSide;
        // the screen and its text
        addWidget(new ImageWidget(4, 4, 190, 164, new ColorRectTexture(RIM)));
        addWidget(new ImageWidget(5, 5, 188, 162, new ColorRectTexture(PANE)));
        addWidget(ScrollingText.box(8, 8, 182, 136, new ComponentPanelWidget(2, 1, machine::addDisplayText)
                .textSupplier(client ? null : machine::addDisplayText)
                .setMaxWidthLimit(174)));
        // the elevator's own buttons, where GTNH has them
        addWidget(new Switch(116, 147, onButton(SIZE, false), onButton(SIZE, true), machine::isExtended,
                machine::setExtended, "af9.space_elevator.gui.size."));
        addWidget(new ImageWidget(136, 147, 18, 18, GuiTextures.INFO_ICON).setHoverTooltips(
                Component.translatable("af9.space_elevator.gui.info.0"),
                Component.translatable("af9.space_elevator.gui.info.1").withStyle(ChatFormatting.LIGHT_PURPLE),
                Component.translatable("af9.space_elevator.gui.info.2").withStyle(ChatFormatting.GRAY),
                Component.translatable("af9.space_elevator.gui.info.3").withStyle(ChatFormatting.GRAY),
                Component.translatable("af9.space_elevator.gui.info.4").withStyle(ChatFormatting.GRAY)));
        addWidget(new ImageWidget(156, 147, 18, 18, LOGO));
        // under the screen: the run's progress and the power switch
        addWidget(new ImageWidget(4, 172, 168, 14, new ColorRectTexture(RIM)));
        addWidget(new ProgressWidget(() -> machine.getRecipeLogic().isWorking() ?
                machine.getRecipeLogic().getProgressPercent() : 0, 5, 173, 166, 12,
                new ProgressTexture(new ColorRectTexture(PANE), new ColorRectTexture(PROGRESS)))
                .setFillDirection(ProgressTexture.FillDirection.LEFT_TO_RIGHT));
        addWidget(new Switch(176, 170, half(GuiTextures.BUTTON_POWER, false), half(GuiTextures.BUTTON_POWER, true),
                machine::isWorkingEnabled, machine::setWorkingEnabled, "af9.space_elevator.gui.power."));
    }

    /** A state of a two-state picture: the off state is its upper half, the on state its lower. */
    private static IGuiTexture half(ResourceTexture states, boolean on) {
        return states.getSubTexture(0, on ? 0.5 : 0, 1, 0.5);
    }

    /** The same on GT's button, raised while off and pressed in while on. */
    private static IGuiTexture onButton(ResourceTexture states, boolean on) {
        return new GuiTextureGroup(half(GuiTextures.TOGGLE_BUTTON_BACK, on), half(states, on));
    }

    /**
     * A two-state button. The server holds its state ({@code state}) and a click sets it there ({@code set}). Its
     * tooltip follows the state (GT's toggle button keeps the tooltip of the state it was built in): the line
     * {@code <tooltip>on} or {@code <tooltip>off}, then {@code <tooltip>hint}.
     */
    private static class Switch extends SwitchWidget {

        private final String tooltip;
        private Boolean described;

        Switch(int x, int y, IGuiTexture off, IGuiTexture on, BooleanSupplier state, BooleanConsumer set,
               String tooltip) {
            super(x, y, 18, 18, (click, pressed) -> {
                if (!click.isRemote) set.accept(pressed.booleanValue());
            });
            this.tooltip = tooltip;
            setTexture(off, on);
            setSupplier(state::getAsBoolean);
            // the server's goes out with the screen; a client's own is replaced by it
            isPressed = state.getAsBoolean();
        }

        @Override
        @OnlyIn(Dist.CLIENT)
        public void updateScreen() {
            super.updateScreen();
            if (described != null && described == isPressed) return;
            described = isPressed;
            setHoverTooltips(Component.translatable(tooltip + (isPressed ? "on" : "off")),
                    Component.translatable(tooltip + "hint").withStyle(ChatFormatting.GRAY));
        }
    }
}
