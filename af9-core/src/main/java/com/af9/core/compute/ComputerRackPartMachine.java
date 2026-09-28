package com.af9.core.compute;

import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.api.gui.GuiTextures;
import com.gregtechceu.gtceu.api.gui.widget.SlotWidget;
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.gregtechceu.gtceu.api.machine.MachineDefinition;
import com.gregtechceu.gtceu.api.machine.feature.IMachineLife;
import com.gregtechceu.gtceu.api.machine.feature.multiblock.IMultiController;
import com.gregtechceu.gtceu.api.machine.multiblock.PartAbility;
import com.gregtechceu.gtceu.api.machine.multiblock.part.MultiblockPartMachine;
import com.gregtechceu.gtceu.api.machine.multiblock.part.TieredPartMachine;
import com.gregtechceu.gtceu.api.pattern.Predicates;
import com.gregtechceu.gtceu.api.pattern.TraceabilityPredicate;
import com.gregtechceu.gtceu.api.registry.GTRegistries;
import com.gregtechceu.gtceu.api.transfer.item.CustomItemStackHandler;

import com.lowdragmc.lowdraglib.gui.widget.ComponentPanelWidget;
import com.lowdragmc.lowdraglib.gui.widget.Widget;
import com.lowdragmc.lowdraglib.gui.widget.WidgetGroup;
import com.lowdragmc.lowdraglib.syncdata.annotation.Persisted;
import com.lowdragmc.lowdraglib.syncdata.field.ManagedFieldHolder;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Computer Rack: the component block of the computation arrays, four card slots ({@link ComputeCard}, one card each).
 * The MV rack takes Tube and Silicon cards, the LuV rack every card. A rack also has its own draw and heat (fans,
 * power supply), whatever is in it.
 */
@SuppressWarnings("removal") // new ResourceLocation(ns, path) is the only constructor on 1.20.1
public class ComputerRackPartMachine extends TieredPartMachine implements IMachineLife {

    protected static final ManagedFieldHolder MANAGED_FIELD_HOLDER = new ManagedFieldHolder(
            ComputerRackPartMachine.class, MultiblockPartMachine.MANAGED_FIELD_HOLDER);

    /** Registered by the rack definitions (KubeJS), taken by the arrays' patterns. */
    public static final PartAbility COMPUTER_RACK = new PartAbility("af9_computer_rack");
    public static final int SLOTS = 4;
    /** Highest card tier below LuV racks: Silicon. */
    public static final int MV_MAX_CARD_TIER = 2;

    @Persisted
    public final CustomItemStackHandler cards;

    public ComputerRackPartMachine(IMachineBlockEntity holder, int tier) {
        super(holder, tier);
        cards = new CustomItemStackHandler(SLOTS) {

            @Override
            public int getSlotLimit(int slot) {
                return 1;
            }
        };
        cards.setFilter(this::accepts);
        cards.setOnContentsChanged(this::onCardsChanged);
    }

    @Override
    public ManagedFieldHolder getFieldHolder() {
        return MANAGED_FIELD_HOLDER;
    }

    /**
     * The racks an array's pattern takes: the MV array only MV racks (else a LuV rack would bring every card into
     * it), the supercomputer any. Built when the pattern is (the racks are registered by then).
     */
    public static TraceabilityPredicate racks(boolean mvOnly) {
        if (!mvOnly) return Predicates.abilities(COMPUTER_RACK);
        MachineDefinition mv = GTRegistries.MACHINES.get(new ResourceLocation("gtceu", "mv_computer_rack"));
        return mv == null ? Predicates.abilities(COMPUTER_RACK) : Predicates.machines(mv);
    }

    /** Highest card tier this rack takes. */
    public int maxCardTier() {
        return getTier() >= GTValues.LuV ? ComputeCard.MAX_TIER : MV_MAX_CARD_TIER;
    }

    private boolean accepts(ItemStack stack) {
        ComputeCard card = ComputeCard.of(stack);
        return card != null && card.tier <= maxCardTier();
    }

    public List<ComputeCard> getCards() {
        List<ComputeCard> list = new ArrayList<>();
        for (int i = 0; i < SLOTS; i++) {
            ComputeCard card = ComputeCard.of(cards.getStackInSlot(i));
            if (card != null) list.add(card);
        }
        return list;
    }

    /** The rack's own draw (fans, power supply). */
    public long idleEut() {
        return getTier() >= GTValues.LuV ? 128 : 4;
    }

    /** The rack's own heat per tick. */
    public int idleHeat() {
        return getTier() >= GTValues.LuV ? 2 : 1;
    }

    /** Cards plus the rack itself. */
    public ComputeCard.Load load() {
        ComputeCard.Load cardLoad = ComputeCard.evaluate(getCards());
        return cardLoad.plus(new ComputeCard.Load(0, idleHeat(), idleEut(), 0, 0));
    }

    private void onCardsChanged() {
        markDirty();
        for (IMultiController controller : getControllers()) {
            if (controller instanceof ComputationArrayMachine array) array.onRacksChanged();
        }
    }

    @Override
    public boolean canShared() {
        return false;
    }

    @Override
    public void onMachineRemoved() {
        clearInventory(cards);
    }

    //////////////////////////////////////
    // ************ GUI *************//
    //////////////////////////////////////

    @Override
    public Widget createUIWidget() {
        var group = new WidgetGroup(0, 0, 150, 72);
        int x0 = (150 - SLOTS * 18) / 2;
        for (int i = 0; i < SLOTS; i++) {
            group.addWidget(new SlotWidget(cards, i, x0 + i * 18, 4, true, true)
                    .setBackgroundTexture(GuiTextures.SLOT));
        }
        boolean client = getLevel() != null && getLevel().isClientSide;
        group.addWidget(new ComponentPanelWidget(4, 28, this::addDisplayText)
                .textSupplier(client ? null : this::addDisplayText)
                .setMaxWidthLimit(142));
        return group;
    }

    private void addDisplayText(List<Component> text) {
        ComputeCard.Load load = ComputeCard.evaluate(getCards());
        text.add(Component.translatable("af9.compute.rack.cards",
                Component.translatable("af9.compute.card.tier." + ComputeCard.TIER_NAMES[maxCardTier()]))
                .withStyle(ChatFormatting.GRAY));
        text.add(Component.translatable("af9.compute.rack.cwut",
                String.format(Locale.ROOT, "%.2f", load.quarterCwut() / 4.0)).withStyle(ChatFormatting.AQUA));
        if (load.unfed() > 0) {
            text.add(Component.translatable("af9.compute.rack.unfed", load.unfed()).withStyle(ChatFormatting.YELLOW));
        }
        text.add(Component.translatable("af9.compute.rack.load", load.heat() + idleHeat(), load.eut() + idleEut())
                .withStyle(ChatFormatting.GRAY));
    }
}
