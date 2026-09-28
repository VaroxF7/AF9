package com.af9.core.machine;

import com.af9.core.bus.BusConnectorPartMachine;
import com.af9.core.bus.BusConsumer;
import com.af9.core.bus.OpticalBusCableBlock;

import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.api.blockentity.PipeBlockEntity;
import com.gregtechceu.gtceu.api.capability.IControllable;
import com.gregtechceu.gtceu.api.capability.IOpticalComputationProvider;
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.gregtechceu.gtceu.api.machine.MetaMachine;
import com.gregtechceu.gtceu.api.machine.TickableSubscription;
import com.gregtechceu.gtceu.api.machine.TieredEnergyMachine;
import com.gregtechceu.gtceu.api.machine.feature.IFancyUIMachine;
import com.gregtechceu.gtceu.api.registry.registrate.MachineBuilder;
import com.gregtechceu.gtceu.client.model.machine.MachineRenderState;
import com.gregtechceu.gtceu.common.blockentity.OpticalPipeBlockEntity;
import com.gregtechceu.gtceu.common.data.models.GTMachineModels;
import com.gregtechceu.gtceu.data.model.builder.MachineModelBuilder;

import com.lowdragmc.lowdraglib.gui.widget.ComponentPanelWidget;
import com.lowdragmc.lowdraglib.gui.widget.Widget;
import com.lowdragmc.lowdraglib.gui.widget.WidgetGroup;
import com.lowdragmc.lowdraglib.syncdata.annotation.DescSynced;
import com.lowdragmc.lowdraglib.syncdata.annotation.Persisted;
import com.lowdragmc.lowdraglib.syncdata.field.ManagedFieldHolder;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.TickTask;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraftforge.client.model.generators.BlockModelBuilder;
import net.minecraftforge.client.model.generators.BlockModelProvider;

import java.util.Collection;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * CWU Server: a single-block machine that turns EU into computation, LV to IV ({@link #cwutFor}: LV 4, MV 8, HV 16, EV
 * 32, IV 64 CWU/t). It gives what is asked of it each tick up to that, and pays for it from its buffer: at full output
 * one amp of its tier ({@code VA[tier]} EU/t), less when less is drawn. It is a GT computation source
 * ({@link IOpticalComputationProvider}, on every side): an ME Computation Link against it takes it directly, GT's
 * Optical Fiber Cable leads it to a reception hatch, and Optical Bus Cable on any side but its front makes it a source
 * on the machine bus ({@link com.af9.core.bus.BusNetwork#facesBus}). Power goes in on any side but the front, which
 * is its lights. A soft mallet (or the screen) switches it off.
 * <p>
 * Its front lights ({@link #LIGHTS}, {@link #lightsModel}): a red dot, steady, while it is offline (switched off, out
 * of energy, or nothing next to it that could draw from it: Optical Bus Cable or a Bus Connector facing it on any side
 * but its front, an ME Computation Link or GT Optical Fiber Cable on any side); steady green while online and idle;
 * blinking while it gives computation. The blinking has two patterns of different lengths ({@link #ALT_LIGHTS}, chosen
 * by the position), so servers side by side do not blink in step.
 */
@SuppressWarnings("removal") // new ResourceLocation(ns, path) is the only constructor on 1.20.1
public class CWUServerMachine extends TieredEnergyMachine implements IOpticalComputationProvider, IControllable,
                              IFancyUIMachine {

    protected static final ManagedFieldHolder MANAGED_FIELD_HOLDER = new ManagedFieldHolder(CWUServerMachine.class,
            TieredEnergyMachine.MANAGED_FIELD_HOLDER);

    @Persisted
    @DescSynced
    private boolean workingEnabled = true;
    /** CWU/t given this tick, and last tick's. */
    private int given, lastGiven;
    private long givenTick = -1;
    /** Game time it last gave any computation. */
    private long lastUse = Long.MIN_VALUE / 2;
    private TickableSubscription lightsSubs;

    public CWUServerMachine(IMachineBlockEntity holder, int tier) {
        super(holder, tier);
        energyContainer.setSideInputCondition(side -> side != getFrontFacing());
    }

    @Override
    public ManagedFieldHolder getFieldHolder() {
        return MANAGED_FIELD_HOLDER;
    }

    /** The CWU/t a server of a tier gives at most: 4 at LV, doubling each tier. */
    public static int cwutFor(int tier) {
        return 4 << Math.max(0, tier - GTValues.LV);
    }

    public int getMaxOutput() {
        return cwutFor(tier);
    }

    /** EU per CWU: at full output one amp of its tier. */
    public double euPerCwu() {
        return (double) GTValues.VA[tier] / getMaxOutput();
    }

    /** CWU/t it gave last tick. */
    public int getLastGiven() {
        roll();
        return lastGiven;
    }

    private void roll() {
        Level level = getLevel();
        long now = level == null ? 0 : level.getGameTime();
        if (now != givenTick) {
            lastGiven = givenTick == now - 1 ? given : 0;
            given = 0;
            givenTick = now;
        }
    }

    //////////////////////////////////////
    // ********** Computation **********//
    //////////////////////////////////////

    @Override
    public int requestCWUt(int cwut, boolean simulate, Collection<IOpticalComputationProvider> seen) {
        seen.add(this);
        if (!workingEnabled || cwut <= 0 || getLevel() == null) return 0;
        roll();
        int room = getMaxOutput() - given;
        int affordable = (int) Math.floor(energyContainer.getEnergyStored() / euPerCwu());
        int give = Math.max(0, Math.min(cwut, Math.min(room, affordable)));
        if (!simulate && give > 0) {
            given += give;
            lastUse = getLevel().getGameTime();
            energyContainer.removeEnergy((long) Math.ceil(give * euPerCwu()));
        }
        return give;
    }

    @Override
    public int getMaxCWUt(Collection<IOpticalComputationProvider> seen) {
        seen.add(this);
        return workingEnabled && energyContainer.getEnergyStored() >= euPerCwu() ? getMaxOutput() : 0;
    }

    @Override
    public boolean canBridge(Collection<IOpticalComputationProvider> seen) {
        return true;
    }

    @Override
    public boolean isWorkingEnabled() {
        return workingEnabled;
    }

    @Override
    public void setWorkingEnabled(boolean enabled) {
        workingEnabled = enabled;
    }

    //////////////////////////////////////
    // ************ Lights *************//
    //////////////////////////////////////

    /** What the front shows. */
    public enum Lights implements StringRepresentable {

        /** A red dot, steady. */
        OFFLINE,
        /** Steady green. */
        IDLE,
        /** Blinking. */
        BUSY;

        @Override
        public String getSerializedName() {
            return name().toLowerCase(Locale.ROOT);
        }
    }

    public static final EnumProperty<Lights> LIGHTS = EnumProperty.create("cwu_lights", Lights.class);
    /** The second blinking pattern. */
    public static final BooleanProperty ALT_LIGHTS = BooleanProperty.create("cwu_alt_lights");
    /** Ticks between two looks at the lights. */
    private static final int LIGHTS_INTERVAL = 10;
    /** Ticks it keeps blinking after it last gave computation. */
    private static final int BUSY_TICKS = 20;

    /**
     * The lights for a server's definition (KubeJS, instead of an overlay model): its model properties and a model
     * per state, the tier's hull under {@code af9:block/machine/cwu_server_<offline|idle|busy|busy_alt>}.
     */
    public static void lightsModel(MachineBuilder<?> builder) {
        builder.modelProperty(LIGHTS, Lights.IDLE);
        builder.modelProperty(ALT_LIGHTS, false);
        builder.model((ctx, prov, model) -> {
            Map<Lights, BlockModelBuilder> plain = new EnumMap<>(Lights.class);
            for (Lights lights : Lights.values()) {
                plain.put(lights, lightsOverlay(prov.models(), model, lights.getSerializedName()));
            }
            BlockModelBuilder busyAlt = lightsOverlay(prov.models(), model, "busy_alt");
            model.forAllStatesModels(state -> state.getValue(LIGHTS) == Lights.BUSY && state.getValue(ALT_LIGHTS) ?
                    busyAlt : plain.get(state.getValue(LIGHTS)));
            model.addReplaceableTextures("bottom", "top", "side");
        });
    }

    /** As GT's overlay tiered hull model: the overlay model with the tier's hull textures. */
    private static BlockModelBuilder lightsOverlay(BlockModelProvider models, MachineModelBuilder<BlockModelBuilder> model,
                                                   String lights) {
        BlockModelBuilder overlay = models.nested();
        overlay.parent(models.getExistingFile(new ResourceLocation("af9", "block/machine/cwu_server_" + lights)));
        return GTMachineModels.tieredHullTextures(overlay, model.getOwner().getTier());
    }

    @Override
    public void onLoad() {
        super.onLoad();
        // a tick later, as GT's own machines subscribe on load
        if (getLevel() instanceof ServerLevel level) {
            level.getServer().tell(new TickTask(0, () -> {
                if (!isInValid() && lightsSubs == null) lightsSubs = subscribeServerTick(this::lightsTick);
            }));
        }
    }

    @Override
    public void onUnload() {
        super.onUnload();
        if (lightsSubs != null) {
            lightsSubs.unsubscribe();
            lightsSubs = null;
        }
    }

    private void lightsTick() {
        if (getOffsetTimer() % LIGHTS_INTERVAL != 0) return;
        MachineRenderState state = getRenderState();
        if (!state.hasProperty(LIGHTS) || !state.hasProperty(ALT_LIGHTS)) return;
        MachineRenderState next = state.setValue(LIGHTS, lights()).setValue(ALT_LIGHTS, isAltPosition(getPos()));
        if (next != state) setRenderState(next);
    }

    /** What the front should show now. */
    public Lights lights() {
        Level level = getLevel();
        if (level == null || !workingEnabled || energyContainer.getEnergyStored() < euPerCwu() || !isConnected()) {
            return Lights.OFFLINE;
        }
        return level.getGameTime() - lastUse <= BUSY_TICKS ? Lights.BUSY : Lights.IDLE;
    }

    /**
     * Whether something next to it could draw its computation: Optical Bus Cable or a Bus Connector facing it on any
     * side but its front, an ME Computation Link with its back against it or GT Optical Fiber Cable joined to it on
     * any side.
     */
    public boolean isConnected() {
        Level level = getLevel();
        if (level == null) return false;
        BlockPos pos = getPos();
        Direction front = getFrontFacing();
        for (Direction side : Direction.values()) {
            BlockPos next = pos.relative(side);
            if (!level.isLoaded(next)) continue;
            if (side != front) {
                if (level.getBlockState(next).getBlock() instanceof OpticalBusCableBlock) return true;
                if (MetaMachine.getMachine(level, next) instanceof BusConnectorPartMachine connector &&
                        connector.getFrontFacing() == side.getOpposite()) {
                    return true;
                }
            }
            BlockEntity entity = level.getBlockEntity(next);
            if (entity instanceof BusConsumer consumer && consumer.getPortSide() == side.getOpposite()) return true;
            if (entity instanceof OpticalPipeBlockEntity pipe &&
                    PipeBlockEntity.isConnected(pipe.getConnections(), side.getOpposite())) {
                return true;
            }
        }
        return false;
    }

    /** The second blinking pattern, for about half the positions, scattered. */
    public static boolean isAltPosition(BlockPos pos) {
        return (Mth.getSeed(pos) >> 16 & 1) == 1;
    }

    //////////////////////////////////////
    // ************ Screen *************//
    //////////////////////////////////////

    @Override
    public Widget createUIWidget() {
        var group = new WidgetGroup(0, 0, 182, 80);
        boolean client = getLevel() != null && getLevel().isClientSide;
        group.addWidget(BusConnectorPartMachine.scrolling(0, 0, 182, 80, new ComponentPanelWidget(4, 5,
                this::addDisplayText)
                .textSupplier(client ? null : this::addDisplayText)
                .setMaxWidthLimit(172)
                .clickHandler((id, click) -> {
                    if (!click.isRemote && id.equals("power")) setWorkingEnabled(!workingEnabled);
                })));
        return group;
    }

    private void addDisplayText(List<Component> text) {
        text.add(ComponentPanelWidget.withButton(Component.translatable(workingEnabled ? "af9.cwu_server.on" :
                "af9.cwu_server.off").withStyle(workingEnabled ? ChatFormatting.GREEN : ChatFormatting.RED), "power"));
        text.add(Component.translatable("af9.cwu_server.giving", getLastGiven(), getMaxOutput())
                .withStyle(ChatFormatting.AQUA));
        text.add(Component.translatable("af9.cwu_server.power", String.format(Locale.ROOT, "%.1f", euPerCwu()),
                GTValues.VA[tier]).withStyle(ChatFormatting.GRAY));
        text.add(Component.translatable("af9.cwu_server.stored", energyContainer.getEnergyStored(),
                energyContainer.getEnergyCapacity()).withStyle(ChatFormatting.GRAY));
        text.add(Component.translatable("af9.cwu_server.hint").withStyle(ChatFormatting.DARK_GRAY));
    }
}
