package com.af9.core.machine;

import com.af9.core.machine.console.ConsoleWidget;

import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.gregtechceu.gtceu.utils.GTUtil;

import com.lowdragmc.lowdraglib.syncdata.field.ManagedFieldHolder;

import net.minecraft.network.chat.Component;
import net.minecraftforge.fluids.FluidStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Particle Accelerator (structure and recipes in KubeJS): a linear accelerator whose superconducting magnets and RF
 * cavities are cooled from coolant hatches only (supercooled fluids). Modes: neutron irradiation (a spallation
 * neutron beam turns neutronium wafers into transmuted neutronium wafers), heavy-ion collision (quark-gluon plasma in
 * magnetic traps) and quark synthesis (strange matter, chromodynium).
 * <p>
 * The beam energy the console shows grows with the voltage tier: 1 GeV at ZPM, doubling per tier.
 */
public class ParticleAcceleratorMachine extends ProcessMachine {

    protected static final ManagedFieldHolder MANAGED_FIELD_HOLDER = new ManagedFieldHolder(
            ParticleAcceleratorMachine.class, ProcessMachine.MANAGED_FIELD_HOLDER);

    private static final int[] COLORS = { 0xFF4ADE80, 0xFFF59E0B, 0xFFF472B6 };

    public ParticleAcceleratorMachine(IMachineBlockEntity holder) {
        super(holder);
    }

    @Override
    public ManagedFieldHolder getFieldHolder() {
        return MANAGED_FIELD_HOLDER;
    }

    @Override
    public String titleKey() {
        return "af9.particle_accelerator.console.title";
    }

    @Override
    public int modeColor(int index) {
        return COLORS[Math.max(0, Math.min(COLORS.length - 1, index))];
    }

    @Override
    public boolean usesCoolant() {
        return true;
    }

    /** Beam energy in GeV at the hatches' voltage tier: 1 GeV at ZPM, x2 per tier (0 below ZPM). */
    public double getBeamEnergyGeV() {
        if (!isFormed() || energyContainer == null) return 0;
        int tier = GTUtil.getTierByVoltage(energyContainer.getInputVoltage());
        return tier < GTValues.ZPM ? 0 : Math.pow(2, tier - GTValues.ZPM);
    }

    @Override
    public List<Component> infoLines() {
        List<Component> lines = new ArrayList<>();
        lines.add(Component.translatable("af9.particle_accelerator.console.beam",
                String.format(Locale.ROOT, "%.0f", getBeamEnergyGeV())));
        FluidStack coolant = getCoolant();
        lines.add(coolant.isEmpty() ? Component.translatable("af9.particle_accelerator.console.no_coolant") :
                Component.translatable("af9.particle_accelerator.console.coolant",
                        coolant.getDisplayName(), ConsoleWidget.compact(getCoolantAmount())));
        boolean running = getRecipeLogic().isWorking();
        lines.add(Component.translatable(running ? "af9.particle_accelerator.console.beam_on" :
                "af9.particle_accelerator.console.beam_off"));
        return lines;
    }
}
