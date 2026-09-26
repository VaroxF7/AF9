package com.af9.core.machine;

import com.af9.core.litho.LithoMode;
import com.af9.core.machine.console.ConsoleWidget;

import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;

import com.lowdragmc.lowdraglib.gui.widget.Widget;
import com.lowdragmc.lowdraglib.syncdata.field.ManagedFieldHolder;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * Controller logic of the Orbital Lithography Station (structure and recipes in KubeJS): prints the orbital modes,
 * 50 nm (ArF immersion), 20 and 7 nm (EUV) and 1 nm on chromodynium wafers with an X-ray free-electron laser (50A of
 * UHV from a laser hatch and energy hatches).
 * <p>
 * It only prints in orbit (a dimension whose path ends in "orbit", e.g. Ad Astra's ad_astra:earth_orbit): the XFEL
 * needs the vacuum of space, and without gravity the resist goes on dry. Its vacuum counts as level 6 (60 s from 0 to
 * 100, after the scanner's 5, see {@link LithoMachine}). Coolant comes from coolant hatches (supercooled fluids only).
 */
public class OrbitalLithographyMachine extends LithoMachine {

    protected static final ManagedFieldHolder MANAGED_FIELD_HOLDER = new ManagedFieldHolder(
            OrbitalLithographyMachine.class, LithoMachine.MANAGED_FIELD_HOLDER);

    /** One level above the Mk2 scanner's last version: 60 s from 0 to 100. */
    public static final int VACUUM_LEVEL = 6;

    public OrbitalLithographyMachine(IMachineBlockEntity holder) {
        super(holder);
    }

    @Override
    public ManagedFieldHolder getFieldHolder() {
        return MANAGED_FIELD_HOLDER;
    }

    @Override
    public List<LithoMode> getModes() {
        return LithoMode.ORBITAL_MODES;
    }

    @Override
    public boolean canPrint(LithoMode mode) {
        return mode.onOrbitalStation() && isInOrbit();
    }

    @Override
    public int blockedStatus(LithoMode mode) {
        if (!mode.onOrbitalStation()) return ConsoleWidget.STATUS_LOCKED;
        return isInOrbit() ? -1 : ConsoleWidget.STATUS_NO_ORBIT;
    }

    @Override
    public int surplusFor(LithoMode mode) {
        return 0;
    }

    @Override
    protected int vacuumLevel() {
        return VACUUM_LEVEL;
    }

    @Override
    public String titleKey() {
        return "af9.orbital_litho.console.title";
    }

    /** True in an orbit dimension (path "orbit" or ending in "_orbit", any mod). */
    public boolean isInOrbit() {
        Level level = getLevel();
        return level != null && isOrbit(level.dimension().location());
    }

    public static boolean isOrbit(ResourceLocation dimension) {
        String path = dimension.getPath();
        return path.equals("orbit") || path.endsWith("_orbit");
    }

    @Override
    public Widget createUIWidget() {
        return LithoConsoleWidget.create(this);
    }
}
