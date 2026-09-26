package com.af9.core.machine;

import com.af9.core.machine.console.AcceleratorConsoleWidget;
import com.af9.core.machine.console.ConsoleWidget;
import com.af9.core.machine.console.SidePanelsUIWidget;

import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.api.capability.recipe.ItemRecipeCapability;
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.gregtechceu.gtceu.api.recipe.GTRecipe;
import com.gregtechceu.gtceu.api.recipe.GTRecipeType;
import com.gregtechceu.gtceu.api.recipe.RecipeHelper;
import com.gregtechceu.gtceu.api.recipe.content.Content;
import com.gregtechceu.gtceu.utils.GTUtil;

import com.lowdragmc.lowdraglib.gui.modular.ModularUI;
import com.lowdragmc.lowdraglib.gui.widget.Widget;
import com.lowdragmc.lowdraglib.syncdata.annotation.DescSynced;
import com.lowdragmc.lowdraglib.syncdata.annotation.Persisted;
import com.lowdragmc.lowdraglib.syncdata.field.ManagedFieldHolder;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.StringJoiner;

/**
 * Particle Accelerator (structure and recipes in KubeJS): a storage ring built like GTNH's Compact Fusion Computer, 47
 * blocks across and 7 high, superconducting bending magnets inside a clean-steel shell with four glass gates. Its
 * magnets are cooled from coolant hatches only (supercooled fluids). Modes: neutron irradiation (a spallation
 * neutron beam turns neutronium wafers into transmuted neutronium wafers), heavy-ion collision (quark-gluon plasma in
 * magnetic traps) and quark synthesis (strange matter, chromodynium).
 * <ul>
 * <li>While it runs, a light ring glows inside the ring in the mode's colour, with lightning leaping from it into the
 * middle ({@link com.af9.core.client.render.LightRingRender}, placed by particle_accelerator.js with
 * {@link #RING_BACK} ...).</li>
 * <li>Its own screen, the orbital station's layout: {@link AcceleratorConsoleWidget} in a
 * {@link SidePanelsUIWidget}.</li>
 * <li>The beam energy grows with the voltage tier: 1 GeV at ZPM, doubling per tier.</li>
 * </ul>
 */
public class ParticleAcceleratorMachine extends ProcessMachine implements ILightRingMachine {

    protected static final ManagedFieldHolder MANAGED_FIELD_HOLDER = new ManagedFieldHolder(
            ParticleAcceleratorMachine.class, ProcessMachine.MANAGED_FIELD_HOLDER);

    private static final int[] COLORS = { 0xFF4ADE80, 0xFFF59E0B, 0xFFF472B6 };

    /**
     * The light ring (the model's, particle_accelerator.js reads these): at the controller's height, {@code RING_BACK}
     * behind it in the middle of the storage ring, lying flat, just inside the ring's inner wall.
     */
    public static final float RING_UP = 0, RING_BACK = 23, RING_RADIUS = 15.5F, RING_THICKNESS = 0.3F;

    /** Colour of the ring: the mode of the run it lit up for (synced; GT does not sync the active recipe type). */
    @Persisted
    @DescSynced
    private int ringColor = COLORS[0];
    /** Runs completed, for the screen's counter. */
    @Persisted
    private long runs;

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

    //////////////////////////////////////
    // ********** Runs ***********//
    //////////////////////////////////////

    @Override
    public boolean beforeWorking(GTRecipe recipe) {
        if (!super.beforeWorking(recipe)) return false;
        GTRecipeType[] types = getRecipeTypes();
        for (int i = 0; i < types.length; i++) {
            if (types[i] == recipe.recipeType) ringColor = modeColor(i);
        }
        return true;
    }

    @Override
    public void afterWorking() {
        super.afterWorking();
        runs++;
    }

    public long getRuns() {
        return runs;
    }

    public void resetRuns() {
        runs = 0;
    }

    /** EU of one run of the running (or last) recipe. */
    public long getEnergyPerRun() {
        GTRecipe recipe = getRecipeLogic().getLastRecipe();
        return recipe == null ? 0 : RecipeHelper.getRealEUt(recipe).getTotalEU() * recipe.duration;
    }

    /**
     * The items of the recipe the screen shows (the running one, else the last run of the active mode), "id*count"
     * joined by ";": its inputs or its outputs, at most three.
     */
    public String shownRecipeItems(boolean inputs) {
        GTRecipe recipe = getRecipeLogic().getLastRecipe();
        if (recipe == null || recipe.recipeType != getRecipeType()) return "";
        List<Content> contents = (inputs ? recipe.inputs : recipe.outputs)
                .getOrDefault(ItemRecipeCapability.CAP, List.of());
        StringJoiner joined = new StringJoiner(";");
        int shown = 0;
        for (Content content : contents) {
            if (shown >= 3) break;
            ItemStack[] stacks = ItemRecipeCapability.CAP.of(content.content).getItems();
            if (stacks.length == 0 || stacks[0].isEmpty()) continue;
            ResourceLocation id = ForgeRegistries.ITEMS.getKey(stacks[0].getItem());
            if (id == null) continue;
            joined.add(id + "*" + stacks[0].getCount());
            shown++;
        }
        return joined.toString();
    }

    //////////////////////////////////////
    // ********* Light ring *********//
    //////////////////////////////////////

    @Override
    public boolean isRingLit() {
        return getRecipeLogic().isWorking();
    }

    @Override
    public int getRingColor() {
        return ringColor;
    }

    /** No throb on each flash (the orbital station's); the lightning crackles instead. */
    @Override
    public SoundEvent ringPulseSound() {
        return null;
    }

    //////////////////////////////////////
    // *********** Screen ***********//
    //////////////////////////////////////

    @Override
    public Widget createUIWidget() {
        return AcceleratorConsoleWidget.createPage(this);
    }

    /** GT's machine screen with the accelerator's page and a panel on each side of the player inventory. */
    @Override
    public ModularUI createUI(Player entityPlayer) {
        return new ModularUI(SidePanelsUIWidget.width(AcceleratorConsoleWidget.WIDTH),
                SidePanelsUIWidget.height(AcceleratorConsoleWidget.HEIGHT), this, entityPlayer)
                .widget(new SidePanelsUIWidget<>(this, AcceleratorConsoleWidget.WIDTH,
                        AcceleratorConsoleWidget.HEIGHT, AcceleratorConsoleWidget.class,
                        AcceleratorConsoleWidget.SidePanel::new));
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
