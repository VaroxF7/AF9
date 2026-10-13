package com.af9.core.machine;

import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;

import com.lowdragmc.lowdraglib.syncdata.field.ManagedFieldHolder;

import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * The Evaporation Condensation Retaining Well (structure and recipes in KubeJS:
 * {@code startup_scripts/gtceu/ecrw.js}, {@code server_scripts/mods/gtceu/ecrw.js}): a shallow round pond that
 * boils dilute fluids down and keeps the condensate, the replacement for Mekanism's evaporation plant and the
 * evaporation GregTech had a while back. Salt water in, salt and distilled water out; the chemical and bio
 * processing lines take it from there.
 * <p>
 * Behaviour is plain GregTech (perfect overclocks from the KubeJS definition); the machine only adds the pond's
 * look: the rake arm that turns while a recipe runs ({@link IEcrwMachine}, rendered by
 * {@link com.af9.core.client.render.EcrwArmRender}).
 */
public class EvaporationWellMachine extends ProcessMachine implements IEcrwMachine {

    protected static final ManagedFieldHolder MANAGED_FIELD_HOLDER = new ManagedFieldHolder(
            EvaporationWellMachine.class, ProcessMachine.MANAGED_FIELD_HOLDER);

    /** The pond's colour in its console: shallow teal. */
    private static final int COLOR = 0xFF2DD4BF;

    public EvaporationWellMachine(IMachineBlockEntity holder) {
        super(holder);
    }

    @Override
    public ManagedFieldHolder getFieldHolder() {
        return MANAGED_FIELD_HOLDER;
    }

    @Override
    public String titleKey() {
        return "af9.ecrw.console.title";
    }

    @Override
    public int modeColor(int index) {
        return COLOR;
    }

    @Override
    public boolean pondFormed() {
        return isFormed();
    }

    @Override
    public boolean rakeSpinning() {
        return isFormed() && getRecipeLogic().isWorking();
    }

    /** The run's progress in seconds while it works, nothing while idle. */
    @Override
    public List<Component> infoLines() {
        List<Component> lines = new ArrayList<>();
        var logic = getRecipeLogic();
        if (logic != null && logic.isWorking() && logic.getDuration() > 0) {
            lines.add(Component.translatable("af9.ecrw.console.progress",
                    String.format(Locale.ROOT, "%.1f", logic.getProgress() / 20F),
                    String.format(Locale.ROOT, "%.1f", logic.getDuration() / 20F)));
        }
        return lines;
    }
}
