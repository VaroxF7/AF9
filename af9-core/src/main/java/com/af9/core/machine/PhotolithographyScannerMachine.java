package com.af9.core.machine;

import com.af9.core.litho.LithoMode;
import com.af9.core.machine.console.BusPlacardWidget;
import com.af9.core.machine.console.ConsoleWidget;
import com.af9.core.machine.console.ScannerConsoleWidget;
import com.af9.core.machine.console.ScannerUIWidget;

import com.gregtechceu.gtceu.api.capability.recipe.IO;
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.gregtechceu.gtceu.api.machine.feature.IMachineLife;
import com.gregtechceu.gtceu.api.machine.trait.NotifiableItemStackHandler;

import com.lowdragmc.lowdraglib.gui.modular.ModularUI;
import com.lowdragmc.lowdraglib.gui.widget.Widget;
import com.lowdragmc.lowdraglib.syncdata.annotation.Persisted;
import com.lowdragmc.lowdraglib.syncdata.field.ManagedFieldHolder;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * The Photolithography Scanner (Mk2, 80 and 65 nm; structure and recipes in KubeJS): the versioned line machine with
 * {@link PhotolithographyLineMachine#MK2}, plus
 * <ul>
 * <li>its ArF excimer laser in a slot of its screen ({@link #laserSlot}, a recipe input like the orbital station's EUV
 * slot: the 80 and 65 nm prints keep an ArF Excimer Laser, not consumed; it may also sit in an input bus), no longer a
 * block of the structure;</li>
 * <li>its own screen: {@link ScannerConsoleWidget} in a {@link ScannerUIWidget}, the orbital station's layout.</li>
 * </ul>
 */
@SuppressWarnings("removal") // new ResourceLocation(ns, path) is the only constructor on 1.20.1
public class PhotolithographyScannerMachine extends PhotolithographyLineMachine implements IMachineLife {

    protected static final ManagedFieldHolder MANAGED_FIELD_HOLDER = new ManagedFieldHolder(
            PhotolithographyScannerMachine.class, PhotolithographyLineMachine.MANAGED_FIELD_HOLDER);

    /** The ArF excimer laser the 80 and 65 nm prints keep (not consumed). */
    public static final ResourceLocation ARF_LASER = new ResourceLocation("kubejs", "arf_excimer_laser");

    /**
     * The laser slot of the scanner's screen: a recipe input (GT reads the controller's own handlers). No pipe access
     * (capability IO NONE), which also makes the handler itself refuse inserts: the screen's slot works on its
     * {@code storage}.
     */
    @Persisted
    public final NotifiableItemStackHandler laserSlot;

    public PhotolithographyScannerMachine(IMachineBlockEntity holder) {
        super(holder, MK2);
        laserSlot = new NotifiableItemStackHandler(this, 1, IO.IN, IO.NONE)
                .setFilter(PhotolithographyScannerMachine::isLaser);
    }

    public static boolean isLaser(ItemStack stack) {
        return !stack.isEmpty() && ARF_LASER.equals(ForgeRegistries.ITEMS.getKey(stack.getItem()));
    }

    /** Whether the laser is in its slot (the screen and the status; an input bus would do for the prints too). */
    public boolean hasLaser() {
        return !laserSlot.getStackInSlot(0).isEmpty();
    }

    @Override
    public ManagedFieldHolder getFieldHolder() {
        return MANAGED_FIELD_HOLDER;
    }

    /** No laser, no light: NO LASER before anything else the mode lacks. */
    @Override
    public int blockedStatus(LithoMode mode) {
        if (!hasLaser()) return ConsoleWidget.STATUS_NO_LIGHT;
        return super.blockedStatus(mode);
    }

    /** Broken controller: the laser drops. */
    @Override
    public void onMachineRemoved() {
        clearInventory(laserSlot.storage);
    }

    //////////////////////////////////////
    // ************ GUI *************//
    //////////////////////////////////////

    @Override
    public Widget createUIWidget() {
        return BusPlacardWidget.wrap(ScannerConsoleWidget.createPage(this), this);
    }

    /** GT's machine screen with the scanner's page and a panel on each side of the player inventory. */
    @Override
    public ModularUI createUI(Player entityPlayer) {
        return new ModularUI(ScannerUIWidget.WIDTH, ScannerUIWidget.HEIGHT, this, entityPlayer)
                .widget(new ScannerUIWidget(this));
    }
}
