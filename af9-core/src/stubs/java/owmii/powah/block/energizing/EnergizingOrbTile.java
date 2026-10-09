package owmii.powah.block.energizing;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import owmii.powah.lib.logistics.energy.Energy;
import owmii.powah.lib.logistics.inventory.Inventory;

/** Compile-time stand-in for Powah's class (not shipped): the members AF9 Core's orb Mk2 and mixins use. */
public class EnergizingOrbTile extends BlockEntity {

    public EnergizingOrbTile(BlockPos pos, BlockState state) {
        super(null, pos, state);
    }

    public Inventory getInventory() {
        return null;
    }

    public Energy getBuffer() {
        return null;
    }

    public boolean containRecipe() {
        return false;
    }

    public int getSlotLimit(int index) {
        return 0;
    }

    public boolean canInsert(int index, ItemStack stack) {
        return false;
    }

    public boolean canExtract(int index, ItemStack stack) {
        return false;
    }
}
