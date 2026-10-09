package com.af9.core.compat.powah;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import owmii.powah.lib.logistics.energy.Energy;

/**
 * The screen of the Energizing Orb Mk2: the six input slots (a stack each), the product and the charge. On the server the
 * slots are the tile's inventory; on the client a plain container the server fills.
 */
public class OrbMk2Menu extends AbstractContainerMenu {

    public static final int PRODUCT = 0;
    public static final int FIRST_INPUT = 1;
    public static final int INPUTS = 6;
    private static final int PLAYER_START = OrbMk2Container.SIZE;
    private static final int PLAYER_END = PLAYER_START + 36;

    // positions in the picture (the screen draws the same)
    public static final int INPUT_X = 30;
    public static final int INPUT_Y = 20;
    public static final int PRODUCT_X = 134;
    public static final int PRODUCT_Y = 29;

    private final Container container;
    private final ContainerData data;
    private final BlockPos pos;

    public OrbMk2Menu(int id, Inventory playerInventory, Container container, ContainerData data, BlockPos pos) {
        super(OrbMk2.MENU.get(), id);
        this.container = container;
        this.data = data;
        this.pos = pos;
        addSlot(new ProductSlot(container, PRODUCT, PRODUCT_X, PRODUCT_Y));
        for (int i = 0; i < INPUTS; i++) {
            addSlot(new InputSlot(container, FIRST_INPUT + i, INPUT_X + (i % 3) * 18, INPUT_Y + (i / 3) * 18));
        }
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(playerInventory, 9 + row * 9 + col, 8 + col * 18, 84 + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) addSlot(new Slot(playerInventory, col, 8 + col * 18, 142));
        addDataSlots(data);
    }

    /** The client's menu: plain slots, the numbers come in data slots. */
    public static OrbMk2Menu client(int id, Inventory playerInventory, FriendlyByteBuf buffer) {
        return new OrbMk2Menu(id, playerInventory, new SimpleContainer(OrbMk2Container.SIZE), new SimpleContainerData(TileData.COUNT),
                buffer.readBlockPos());
    }

    public BlockPos pos() {
        return pos;
    }

    /** The charge in the orb, in RF. */
    public long stored() {
        return (data.get(1) & 0xFFFFL) << 16 | (data.get(0) & 0xFFFFL);
    }

    /** What the recipe in the orb needs, in RF; 0 for none. */
    public long required() {
        return (data.get(3) & 0xFFFFL) << 16 | (data.get(2) & 0xFFFFL);
    }

    public boolean hasRecipe() {
        return (data.get(4) & 1) != 0;
    }

    @Override
    public boolean stillValid(Player player) {
        return container.stillValid(player);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (slot == null || !slot.hasItem()) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem();
        ItemStack before = stack.copy();
        if (index < PLAYER_START) {
            // out of the orb, into the inventory
            if (!moveItemStackTo(stack, PLAYER_START, PLAYER_END, true)) return ItemStack.EMPTY;
        } else if (!moveItemStackTo(stack, FIRST_INPUT, FIRST_INPUT + INPUTS, false)) {
            return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        return before;
    }

    /** An input: any item while the product has not been taken. */
    private static final class InputSlot extends Slot {

        InputSlot(Container container, int index, int x, int y) {
            super(container, index, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return container.canPlaceItem(getContainerSlot(), stack);
        }
    }

    /** The product: taken, not put into. */
    private static final class ProductSlot extends Slot {

        ProductSlot(Container container, int index, int x, int y) {
            super(container, index, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return false;
        }
    }

    /** The numbers of the tile for the screen, in 16-bit halves (a data slot goes over the wire as a short). */
    public static final class TileData implements ContainerData {

        static final int COUNT = 5;

        private final OrbMk2Tile tile;

        public TileData(OrbMk2Tile tile) {
            this.tile = tile;
        }

        @Override
        public int get(int index) {
            Energy buffer = tile.getBuffer();
            switch (index) {
                case 0: return (int) (buffer.getStored() & 0xFFFFL);
                case 1: return (int) ((buffer.getStored() >>> 16) & 0xFFFFL);
                case 2: return (int) (buffer.getCapacity() & 0xFFFFL);
                case 3: return (int) ((buffer.getCapacity() >>> 16) & 0xFFFFL);
                case 4: return tile.containRecipe() ? 1 : 0;
                default: return 0;
            }
        }

        @Override
        public void set(int index, int value) {}

        @Override
        public int getCount() {
            return COUNT;
        }
    }
}
