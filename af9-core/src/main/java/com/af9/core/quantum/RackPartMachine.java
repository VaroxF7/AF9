package com.af9.core.quantum;

import com.gregtechceu.gtceu.api.gui.GuiTextures;
import com.gregtechceu.gtceu.api.gui.widget.SlotWidget;
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.gregtechceu.gtceu.api.machine.TickableSubscription;
import com.gregtechceu.gtceu.api.machine.feature.IMachineLife;
import com.gregtechceu.gtceu.api.machine.feature.multiblock.IMultiController;
import com.gregtechceu.gtceu.api.machine.multiblock.PartAbility;
import com.gregtechceu.gtceu.api.machine.multiblock.part.MultiblockPartMachine;
import com.gregtechceu.gtceu.api.transfer.item.CustomItemStackHandler;
import com.gregtechceu.gtceu.utils.FormattingUtil;

import com.lowdragmc.lowdraglib.gui.widget.ComponentPanelWidget;
import com.lowdragmc.lowdraglib.gui.widget.Widget;
import com.lowdragmc.lowdraglib.gui.widget.WidgetGroup;
import com.lowdragmc.lowdraglib.syncdata.annotation.DescSynced;
import com.lowdragmc.lowdraglib.syncdata.annotation.Persisted;
import com.lowdragmc.lowdraglib.syncdata.field.ManagedFieldHolder;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.TickTask;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;

import java.util.List;

/**
 * Computer Rack Hatch of the Quantum Computer: GTNH's TecTech rack ({@code MTEHatchRack}, GT5-Unofficial, LGPL-3.0) with
 * four component slots. It holds its own heat: the controller runs the processors once a second
 * ({@link #run}), and the rack cools by itself once a second ({@link #coolSecond}): at least 20 heat, a thousandth of
 * what it has, and what its vents take. A component in a rack hotter than it can bear burns out. While the computer
 * runs, or the rack is above 2,000 heat, the slots are locked (as in TecTech).
 */
public class RackPartMachine extends MultiblockPartMachine implements IMachineLife {

    protected static final ManagedFieldHolder MANAGED_FIELD_HOLDER = new ManagedFieldHolder(RackPartMachine.class,
            MultiblockPartMachine.MANAGED_FIELD_HOLDER);

    /** Registered by the hatch definition (KubeJS), required by the Quantum Computer's pattern. */
    public static final PartAbility RACK = new PartAbility("af9_computer_rack");

    public static final int SLOTS = 4;
    /** Above this heat the slots stay locked, whatever the computer does. */
    public static final int LOCK_HEAT = 2000;
    private static final int SECOND = 20;

    @Persisted
    @DescSynced
    private final CustomItemStackHandler components;
    /** Rack heat; below zero while the rack is colder than ambient, where only processors work. */
    @Persisted
    @DescSynced
    private int heat;
    /** CWU/t this rack gave in its last second. */
    @Persisted
    @DescSynced
    private int computation;
    /** Components burnt out here since the rack was placed. */
    @Persisted
    @DescSynced
    private int burnt;
    @DescSynced
    private boolean locked;
    private TickableSubscription coolSubs;

    public RackPartMachine(IMachineBlockEntity holder) {
        super(holder);
        this.components = new RackStorage();
    }

    @Override
    public ManagedFieldHolder getFieldHolder() {
        return MANAGED_FIELD_HOLDER;
    }

    @Override
    public boolean canShared() {
        return false;
    }

    public int getHeat() {
        return Math.max(0, heat);
    }

    public int getComputation() {
        return computation;
    }

    public boolean isLocked() {
        return locked;
    }

    //////////////////////////////////////
    // ************ Storage ************//
    //////////////////////////////////////

    /** One component a slot, and nothing goes in or out while the computer runs or the rack is hot. */
    private final class RackStorage extends CustomItemStackHandler {

        RackStorage() {
            super(SLOTS);
        }

        @Override
        public int getSlotLimit(int slot) {
            return 1;
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return !locked && RackComponent.isComponent(stack);
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            return locked ? ItemStack.EMPTY : super.extractItem(slot, amount, simulate);
        }
    }

    @Override
    public void onMachineRemoved() {
        clearInventory(components);
    }

    //////////////////////////////////////
    // ************* Heat **************//
    //////////////////////////////////////

    /**
     * The processors' second while the computer runs (TecTech's {@code getComputationPower}): every component not
     * burnt out heats (a processor, by its heat constant times overclock times overvolt squared) or cools (a vent, 10
     * and more the hotter the rack); a processor computes if the overvolt beats a roll.
     *
     * @return CWU/t of this rack for the next second
     */
    public int run(float overclock, float overvolt, RandomSource random) {
        float delta = 0F;
        float computing = 0F;
        for (int i = 0; i < components.getSlots(); i++) {
            ItemStack stack = components.getStackInSlot(i);
            RackComponent component = stack.getCount() == 1 ? RackComponent.of(stack) : null;
            if (component == null) continue;
            if (heat > component.maxHeat()) {
                burnOut(i);
            } else if (!component.isVent() || heat >= 0) {
                float cooling = component.isVent() ? component.cool() : -1F;
                float constant = component.isVent() ? -10F : component.heat() * overclock * overvolt * overvolt;
                delta += (1F + cooling * heat / 100000F) * constant;
                if (overvolt > random.nextFloat()) {
                    computing += component.computation() * (1 + overclock * overclock) /
                            (1 + (overclock - overvolt) * (overclock - overvolt));
                }
            }
        }
        heat += (int) Math.ceil(delta);
        computation = (int) Math.floor(computing);
        return computation;
    }

    /** What the rack gives up in a second without being run: the passive loss and the vents. */
    private void coolSecond() {
        if (heat <= 0) return;
        float vents = 0F;
        for (int i = 0; i < components.getSlots(); i++) {
            ItemStack stack = components.getStackInSlot(i);
            RackComponent component = stack.getCount() == 1 ? RackComponent.of(stack) : null;
            if (component == null) continue;
            if (heat - 20 > component.maxHeat()) {
                burnOut(i);
            } else if (component.isVent()) {
                vents -= heat / 10000F;
            }
        }
        heat += (int) Math.max(-heat, Math.ceil(vents));
        heat -= Math.max(heat / 1000, 20);
    }

    private void burnOut(int slot) {
        components.setStackInSlot(slot, ItemStack.EMPTY);
        burnt++;
    }

    /** The lowest heat limit among the components, 0 for none. */
    public int heatLimit() {
        int limit = 0;
        for (int i = 0; i < components.getSlots(); i++) {
            RackComponent component = RackComponent.of(components.getStackInSlot(i));
            if (component != null && (limit == 0 || component.maxHeat() < limit)) limit = component.maxHeat();
        }
        return limit;
    }

    /** Whether a quantum computer this rack belongs to is running. */
    private boolean computerRunning() {
        if (!isFormed()) return false;
        for (IMultiController controller : getControllers()) {
            if (controller instanceof QuantumComputerMachine computer && computer.isRunning()) return true;
        }
        return false;
    }

    private void tick() {
        if (getOffsetTimer() % SECOND != 0) return;
        coolSecond();
        boolean now = computerRunning() || heat > LOCK_HEAT;
        if (now != locked) locked = now;
        if (!computerRunning() && computation != 0) computation = 0;
    }

    @Override
    public void onLoad() {
        super.onLoad();
        // a tick later, as GT's own machines subscribe on load
        if (getLevel() instanceof ServerLevel level) {
            level.getServer().tell(new TickTask(0, () -> {
                if (!isInValid() && coolSubs == null) coolSubs = subscribeServerTick(this::tick);
            }));
        }
    }

    @Override
    public void onUnload() {
        super.onUnload();
        if (coolSubs != null) {
            coolSubs.unsubscribe();
            coolSubs = null;
        }
    }

    //////////////////////////////////////
    // ************** GUI **************//
    //////////////////////////////////////

    @Override
    public boolean shouldOpenUI(Player player, InteractionHand hand, BlockHitResult hit) {
        return true;
    }

    @Override
    public Widget createUIWidget() {
        var group = new WidgetGroup(0, 0, 176, 74);
        for (int i = 0; i < SLOTS; i++) {
            group.addWidget(new SlotWidget(components, i, 52 + i * 18, 4, true, true)
                    .setBackgroundTexture(GuiTextures.SLOT));
        }
        group.addWidget(new ComponentPanelWidget(4, 28, this::addDisplayText)
                .textSupplier(getLevel() != null && getLevel().isClientSide ? null : this::addDisplayText)
                .setMaxWidthLimit(168));
        return group;
    }

    private void addDisplayText(List<Component> text) {
        int limit = heatLimit();
        ChatFormatting color = limit == 0 || heat < limit / 2 ? ChatFormatting.GREEN :
                heat < limit * 4 / 5 ? ChatFormatting.YELLOW : ChatFormatting.RED;
        text.add(Component.translatable("af9.quantum.rack.heat", Component.literal(
                FormattingUtil.formatNumbers(getHeat())).withStyle(color),
                limit == 0 ? "-" : FormattingUtil.formatNumbers(limit)));
        text.add(Component.translatable("af9.quantum.rack.computation", FormattingUtil.formatNumbers(computation))
                .withStyle(ChatFormatting.AQUA));
        if (burnt > 0) {
            text.add(Component.translatable("af9.quantum.rack.burnt", burnt).withStyle(ChatFormatting.RED));
        }
        text.add(Component.translatable(locked ? "af9.quantum.rack.locked" : "af9.quantum.rack.open")
                .withStyle(ChatFormatting.GRAY));
    }
}
