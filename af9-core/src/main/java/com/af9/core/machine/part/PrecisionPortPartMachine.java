package com.af9.core.machine.part;

import com.gregtechceu.gtceu.api.capability.recipe.IO;
import com.gregtechceu.gtceu.api.gui.GuiTextures;
import com.gregtechceu.gtceu.api.gui.fancy.ConfiguratorPanel;
import com.gregtechceu.gtceu.api.gui.widget.SlotWidget;
import com.gregtechceu.gtceu.api.gui.widget.TankWidget;
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.gregtechceu.gtceu.api.machine.fancyconfigurator.CircuitFancyConfigurator;
import com.gregtechceu.gtceu.api.machine.feature.IFancyUIMachine;
import com.gregtechceu.gtceu.api.machine.multiblock.PartAbility;
import com.gregtechceu.gtceu.api.machine.multiblock.part.MultiblockPartMachine;
import com.gregtechceu.gtceu.api.machine.multiblock.part.TieredPartMachine;
import com.gregtechceu.gtceu.api.machine.trait.NotifiableFluidTank;
import com.gregtechceu.gtceu.api.machine.trait.NotifiableItemStackHandler;
import com.gregtechceu.gtceu.common.item.IntCircuitBehaviour;

import com.lowdragmc.lowdraglib.gui.widget.Widget;
import com.lowdragmc.lowdraglib.gui.widget.WidgetGroup;
import com.lowdragmc.lowdraglib.syncdata.annotation.Persisted;
import com.lowdragmc.lowdraglib.syncdata.field.ManagedFieldHolder;

/**
 * Precision Fabricator Input Port and Output Port: the only hatches of the High Temperature Precision Fabricator,
 * in place of its item buses and fluid hatches. One class, two faces.
 * <p>
 * The <b>input port</b> is an input hatch and an input bus in one block: nine fluid tanks (the 3 x 3 grid), three
 * item slots, one slot for the non-consumable the run keeps (the holder rod), and a ghost programmed circuit (the
 * circuit button of the screen, no item needed). The <b>output port</b> has the one item slot the boule comes out in.
 * The recipes work on them like on any hatch and bus.
 */
public class PrecisionPortPartMachine extends TieredPartMachine implements IFancyUIMachine {

    protected static final ManagedFieldHolder MANAGED_FIELD_HOLDER = new ManagedFieldHolder(
            PrecisionPortPartMachine.class, MultiblockPartMachine.MANAGED_FIELD_HOLDER);

    /** Registered by the definitions (KubeJS), required by the fabricator's pattern. */
    public static final PartAbility INPUT = new PartAbility("af9_precision_input");
    public static final PartAbility OUTPUT = new PartAbility("af9_precision_output");

    public static final int FLUID_TANKS = 9, ITEM_SLOTS = 3;
    /** A tank holds this many millibuckets at LV and doubles per tier (the boules' polysilicon is up to 73,728 mB). */
    public static final int TANK_BASE = 4000;

    public final IO io;
    @Persisted
    public final NotifiableItemStackHandler items, nonConsumable, circuit;
    @Persisted
    public final NotifiableFluidTank tanks;

    public PrecisionPortPartMachine(IMachineBlockEntity holder, int tier, IO io) {
        super(holder, tier);
        this.io = io;
        boolean in = io == IO.IN;
        items = new NotifiableItemStackHandler(this, in ? ITEM_SLOTS : 1, io, io);
        // the output port has only its one slot: the rest are one-slot placeholders no recipe or pipe reaches (IO.NONE)
        nonConsumable = new NotifiableItemStackHandler(this, 1, in ? IO.IN : IO.NONE, in ? IO.IN : IO.NONE);
        circuit = new NotifiableItemStackHandler(this, 1, in ? IO.IN : IO.NONE, IO.NONE)
                .setFilter(IntCircuitBehaviour::isIntegratedCircuit).shouldSearchContent(false);
        tanks = new NotifiableFluidTank(this, in ? FLUID_TANKS : 1, TANK_BASE << tier, in ? IO.IN : IO.NONE,
                in ? IO.IN : IO.NONE);
    }

    @Override
    public ManagedFieldHolder getFieldHolder() {
        return MANAGED_FIELD_HOLDER;
    }

    /** The ghost circuit: the button above the screen's tabs, like the Void Miner's. */
    @Override
    public void attachConfigurators(ConfiguratorPanel configuratorPanel) {
        if (io == IO.IN) configuratorPanel.attachConfigurators(new CircuitFancyConfigurator(circuit.storage));
    }

    //////////////////////////////////////
    // *********** Screen ***********//
    //////////////////////////////////////

    @Override
    public Widget createUIWidget() {
        if (io != IO.IN) {
            WidgetGroup group = new WidgetGroup(0, 0, 34, 34);
            WidgetGroup container = new WidgetGroup(4, 4, 26, 26);
            container.addWidget(new SlotWidget(items.storage, 0, 4, 4, true, false)
                    .setBackgroundTexture(GuiTextures.SLOT));
            container.setBackground(GuiTextures.BACKGROUND_INVERSE);
            group.addWidget(container);
            return group;
        }
        // items on top (three inputs, then the non-consumable one), the fluids in a 3 x 3 grid under them
        WidgetGroup group = new WidgetGroup(0, 0, 18 * 4 + 16, 18 * 4 + 16 + 4);
        WidgetGroup container = new WidgetGroup(4, 4, 18 * 4 + 8, 18 * 4 + 8 + 4);
        for (int i = 0; i < ITEM_SLOTS; i++) {
            container.addWidget(new SlotWidget(items.storage, i, 4 + i * 18, 4, true, true)
                    .setBackgroundTexture(GuiTextures.SLOT));
        }
        container.addWidget(new SlotWidget(nonConsumable.storage, 0, 4 + 3 * 18 + 4, 4, true, true)
                .setBackgroundTexture(GuiTextures.SLOT));
        for (int i = 0; i < FLUID_TANKS; i++) {
            container.addWidget(new TankWidget(tanks.getStorages()[i], 4 + (i % 3) * 18, 4 + 22 + (i / 3) * 18,
                    true, true).setBackground(GuiTextures.FLUID_SLOT));
        }
        container.setBackground(GuiTextures.BACKGROUND_INVERSE);
        group.addWidget(container);
        return group;
    }
}
