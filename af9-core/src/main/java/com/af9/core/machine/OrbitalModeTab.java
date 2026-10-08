package com.af9.core.machine;

import com.gregtechceu.gtceu.api.gui.GuiTextures;
import com.gregtechceu.gtceu.api.gui.fancy.FancyMachineUIWidget;
import com.gregtechceu.gtceu.api.machine.fancyconfigurator.MachineModeFancyConfigurator;
import com.gregtechceu.gtceu.api.recipe.GTRecipeType;

import com.lowdragmc.lowdraglib.gui.editor.ColorPattern;
import com.lowdragmc.lowdraglib.gui.texture.GuiTextureGroup;
import com.lowdragmc.lowdraglib.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib.gui.texture.ResourceBorderTexture;
import com.lowdragmc.lowdraglib.gui.texture.TextTexture;
import com.lowdragmc.lowdraglib.gui.widget.ButtonWidget;
import com.lowdragmc.lowdraglib.gui.widget.ImageWidget;
import com.lowdragmc.lowdraglib.gui.widget.Widget;

import net.minecraft.network.chat.Component;

/**
 * GT's machine mode tab for the Orbital Lithography Station: the modes only the Array Mk2 runs are shut (greyed, no
 * click) while the station is the basic one. The list is GT's otherwise ({@link MachineModeFancyConfigurator}).
 */
public class OrbitalModeTab extends MachineModeFancyConfigurator {

    private static final int LOCKED = 0xFF5A5A5A;

    private final OrbitalLithographyMachine station;

    public OrbitalModeTab(OrbitalLithographyMachine station) {
        super(station);
        this.station = station;
    }

    @Override
    public Widget createMainPage(FancyMachineUIWidget widget) {
        GTRecipeType[] types = station.getRecipeTypes();
        var group = new MachineModeConfigurator(0, 0, 140, 20 * types.length + 4);
        group.setBackground(GuiTextures.BACKGROUND_INVERSE);
        for (int i = 0; i < types.length; i++) {
            int index = i;
            var button = new ButtonWidget(2, 2 + i * 20, 136, 20, IGuiTexture.EMPTY, click -> choose(index));
            if (OrbitalLithographyMachine.isMk2Only(types[i])) {
                button.setHoverTooltips(Component.translatable("af9.orbital_array.mode_mk2_only"));
            }
            group.addWidget(button);
            group.addWidget(new ImageWidget(2, 2 + i * 20, 136, 20, () -> {
                boolean shut = station.isModeLocked(index);
                var name = new TextTexture(types[index].registryName.toLanguageKey()).setWidth(136)
                        .setType(TextTexture.TextType.ROLL);
                if (shut) name.setColor(LOCKED);
                int frame = station.getActiveRecipeType() == index ? ColorPattern.CYAN.color : shut ? LOCKED : -1;
                return new GuiTextureGroup(ResourceBorderTexture.BUTTON_COMMON.copy().setColor(frame), name);
            }));
        }
        return group;
    }

    /** GT's click, for a mode that is open. */
    private void choose(int index) {
        if (station.isModeLocked(index)) return;
        boolean update = !station.keepSubscribing() && index != station.getActiveRecipeType();
        station.setActiveRecipeType(index);
        if (update) station.getRecipeLogic().updateTickSubscription();
    }
}
