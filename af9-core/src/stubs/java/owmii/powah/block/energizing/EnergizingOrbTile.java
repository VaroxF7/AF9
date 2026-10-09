package owmii.powah.block.energizing;

import owmii.powah.lib.logistics.inventory.Inventory;

/** Compile-time stand-in for Powah's class (not shipped). */
public abstract class EnergizingOrbTile extends net.minecraft.world.level.block.entity.BlockEntity {

    protected EnergizingOrbTile() {
        super(null, null, null);
    }

    public abstract Inventory getInventory();
}
