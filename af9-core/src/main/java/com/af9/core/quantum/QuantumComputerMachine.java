package com.af9.core.quantum;

import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.api.capability.IOpticalComputationProvider;
import com.gregtechceu.gtceu.api.gui.fancy.IFancyUIProvider;
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.gregtechceu.gtceu.api.machine.TickableSubscription;
import com.gregtechceu.gtceu.api.machine.feature.multiblock.IMultiPart;
import com.gregtechceu.gtceu.api.machine.multiblock.MultiblockDisplayText;
import com.gregtechceu.gtceu.api.machine.multiblock.WorkableElectricMultiblockMachine;
import com.gregtechceu.gtceu.api.machine.trait.RecipeLogic;
import com.gregtechceu.gtceu.utils.FormattingUtil;

import com.lowdragmc.lowdraglib.gui.util.ClickData;
import com.lowdragmc.lowdraglib.gui.widget.ComponentPanelWidget;
import com.lowdragmc.lowdraglib.syncdata.annotation.DescSynced;
import com.lowdragmc.lowdraglib.syncdata.annotation.Persisted;
import com.lowdragmc.lowdraglib.syncdata.field.ManagedFieldHolder;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.TickTask;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Quantum Computer: GTNH's TecTech Quantum Computer ({@code MTEQuantumComputer}, GT5-Unofficial, LGPL-3.0) as a GT
 * computation source ({@link IOpticalComputationProvider}). A long corridor of racks (two Computer Rack Hatches a slice,
 * {@link RackPartMachine}); processors in the racks make the computation, heat vents keep them alive.
 * <p>
 * Once a second, while the computer is switched on and paid for, every rack runs its components
 * ({@link RackPartMachine#run}) and the sum is this second's capacity in CWU/t, which the computation hatches hand out
 * ({@link #requestCWUt}): GT's Computation Data Transmission Hatch (also AF9's wireless one) leads it out. It draws
 * {@code max(LuV, ZPM * overclock * overvolt)} EU/t for the controller and for every rack that computes: TecTech's
 * amps ({@code 1 + racks}). The two ratios are TecTech's parameters (0 to 5, in the screen): the computation of a
 * processor is {@code C * (1 + oc^2) / (1 + (oc - ov)^2)}, its heat {@code H * oc * ov^2}.
 */
public class QuantumComputerMachine extends WorkableElectricMultiblockMachine implements IOpticalComputationProvider {

    protected static final ManagedFieldHolder MANAGED_FIELD_HOLDER = new ManagedFieldHolder(
            QuantumComputerMachine.class, WorkableElectricMultiblockMachine.MANAGED_FIELD_HOLDER);

    /** Ticks in a computing cycle: the racks run, heat and cool, once a second. */
    public static final int CYCLE = 20;
    /** The ratios in tenths: 0 to 5, 1.0 by default (TecTech's range). */
    public static final int MAX_RATIO = 50;

    /** Ratios, in tenths. */
    @Persisted
    @DescSynced
    private int overclockTenths = 10;
    @Persisted
    @DescSynced
    private int overvoltTenths = 10;
    /** CWU/t of this second, and what has been handed out this tick (server only). */
    @Persisted
    @DescSynced
    private int capacity;
    private int allocated;
    /** CWU/t handed out last tick, for the screen. */
    @DescSynced
    private int shownUsed;
    @DescSynced
    private long shownEUt;
    @DescSynced
    private int rackCount;
    @DescSynced
    private int activeRacks;
    @DescSynced
    private int hottest;
    @DescSynced
    private boolean running;
    @DescSynced
    private boolean lacksEnergy;

    private final List<RackPartMachine> racks = new ArrayList<>();
    private TickableSubscription tickSubs;

    public QuantumComputerMachine(IMachineBlockEntity holder) {
        super(holder);
    }

    @Override
    public ManagedFieldHolder getFieldHolder() {
        return MANAGED_FIELD_HOLDER;
    }

    //////////////////////////////////////
    // ******** Lifecycle *********//
    //////////////////////////////////////

    @Override
    public void onStructureFormed() {
        super.onStructureFormed();
        racks.clear();
        for (IMultiPart part : getParts()) {
            if (part instanceof RackPartMachine rack) racks.add(rack);
        }
        rackCount = racks.size();
        if (getLevel() instanceof ServerLevel level) {
            level.getServer().tell(new TickTask(0, this::updateSubscription));
        }
    }

    @Override
    public void onStructureInvalid() {
        super.onStructureInvalid();
        stop();
        racks.clear();
        rackCount = 0;
        updateSubscription();
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (getLevel() instanceof ServerLevel level) {
            level.getServer().tell(new TickTask(0, this::updateSubscription));
        }
    }

    @Override
    public void onUnload() {
        super.onUnload();
        if (tickSubs != null) {
            tickSubs.unsubscribe();
            tickSubs = null;
        }
    }

    private void updateSubscription() {
        if (isInValid()) return;
        if (isFormed()) {
            tickSubs = subscribeServerTick(tickSubs, this::tick);
        } else if (tickSubs != null) {
            tickSubs.unsubscribe();
            tickSubs = null;
        }
    }

    private void stop() {
        running = false;
        capacity = 0;
        allocated = 0;
        shownUsed = 0;
        shownEUt = 0;
        activeRacks = 0;
        getRecipeLogic().setStatus(RecipeLogic.Status.IDLE);
    }

    public boolean isRunning() {
        return running;
    }

    //////////////////////////////////////
    // ******** Computation *********//
    //////////////////////////////////////

    @Override
    public int requestCWUt(int cwut, boolean simulate, Collection<IOpticalComputationProvider> seen) {
        seen.add(this);
        if (!running || cwut <= 0) return 0;
        int give = Math.min(cwut, capacity - allocated);
        if (give <= 0) return 0;
        if (!simulate) allocated += give;
        return give;
    }

    @Override
    public int getMaxCWUt(Collection<IOpticalComputationProvider> seen) {
        seen.add(this);
        return running ? capacity : 0;
    }

    @Override
    public boolean canBridge(Collection<IOpticalComputationProvider> seen) {
        seen.add(this);
        return true;
    }

    public float overclock() {
        return overclockTenths / 10F;
    }

    public float overvolt() {
        return overvoltTenths / 10F;
    }

    /** EU/t it draws now: TecTech's {@code max(LuV, ZPM * oc * ov)} for each amp ({@code 1 + the racks that compute}). */
    public long euPerTick() {
        double base = Math.max(GTValues.V[GTValues.LuV], GTValues.V[GTValues.ZPM] * overclock() * overvolt());
        return (long) Math.ceil(base) * (1 + activeRacks);
    }

    private void tick() {
        if (!isFormed()) return;
        shownUsed = allocated;
        allocated = 0;
        if (!isWorkingEnabled() || energyContainer == null) {
            if (running) stop();
            lacksEnergy = false;
            return;
        }
        long need = euPerTick();
        boolean paid = energyContainer.getEnergyStored() >= need && energyContainer.removeEnergy(need) == need;
        lacksEnergy = !paid;
        if (!paid) {
            // like the HPCA: no energy, no computation, and the racks only cool
            running = false;
            capacity = 0;
            activeRacks = 0;
            shownEUt = 0;
            getRecipeLogic().setStatus(RecipeLogic.Status.WAITING);
            return;
        }
        shownEUt = need;
        running = true;
        getRecipeLogic().setStatus(RecipeLogic.Status.WORKING);
        if (getOffsetTimer() % CYCLE == 0) cycle();
    }

    /** The racks' second. */
    private void cycle() {
        int total = 0;
        int active = 0;
        int peak = 0;
        var random = getLevel().getRandom();
        for (RackPartMachine rack : racks) {
            if (rack.isInValid()) continue;
            int computed = rack.run(overclock(), overvolt(), random);
            if (computed > 0) {
                total += computed;
                active++;
            }
            peak = Math.max(peak, rack.getHeat());
        }
        capacity = total;
        activeRacks = active;
        hottest = peak;
    }

    //////////////////////////////////////
    // ************ Screen ************//
    //////////////////////////////////////

    /** Tabs of the racks are left out of the screen: a corridor has up to 26 of them. */
    @Override
    public List<IFancyUIProvider> getSubTabs() {
        return List.of();
    }

    @Override
    public void addDisplayText(List<Component> textList) {
        MultiblockDisplayText.builder(textList, isFormed())
                .setWorkingStatus(isWorkingEnabled(), running)
                .setWorkingStatusKeys("gtceu.multiblock.idling", "gtceu.multiblock.work_paused",
                        "gtceu.multiblock.running")
                .addCustom(tl -> {
                    tl.add(Component.translatable("af9.quantum.energy", FormattingUtil.formatNumbers(shownEUt),
                            1 + activeRacks).withStyle(ChatFormatting.GRAY));
                    tl.add(Component.translatable("af9.quantum.computation",
                            Component.literal(FormattingUtil.formatNumbers(shownUsed) + " / " +
                                    FormattingUtil.formatNumbers(capacity) + " CWU/t").withStyle(ChatFormatting.AQUA))
                            .withStyle(ChatFormatting.GRAY));
                    tl.add(Component.translatable("af9.quantum.racks", rackCount, activeRacks,
                            Component.literal(FormattingUtil.formatNumbers(hottest)).withStyle(heatColor()))
                            .withStyle(ChatFormatting.GRAY));
                    if (lacksEnergy) {
                        tl.add(Component.translatable("af9.quantum.no_energy").withStyle(ChatFormatting.RED));
                    }
                    tl.add(ratioLine("af9.quantum.overclock", overclock(), "oc"));
                    tl.add(ratioLine("af9.quantum.overvolt", overvolt(), "ov"));
                    tl.add(Component.translatable("af9.quantum.hint").withStyle(ChatFormatting.DARK_GRAY));
                })
                .addWorkingStatusLine();
    }

    private ChatFormatting heatColor() {
        return hottest < 1000 ? ChatFormatting.GREEN : hottest < 4000 ? ChatFormatting.YELLOW : ChatFormatting.RED;
    }

    /** "Overclock 1.0  [-1] [-.1] [+.1] [+1]" */
    private static MutableComponent ratioLine(String key, float value, String id) {
        MutableComponent line = Component.translatable(key, String.format("%.1f", value));
        line.append(" ").append(ComponentPanelWidget.withButton(Component.literal("[-1]"), id + "-10"));
        line.append(" ").append(ComponentPanelWidget.withButton(Component.literal("[-.1]"), id + "-1"));
        line.append(" ").append(ComponentPanelWidget.withButton(Component.literal("[+.1]"), id + "+1"));
        line.append(" ").append(ComponentPanelWidget.withButton(Component.literal("[+1]"), id + "+10"));
        return line;
    }

    @Override
    public void handleDisplayClick(String componentData, ClickData clickData) {
        if (clickData.isRemote || componentData.length() < 4) return;
        int step;
        try {
            step = Integer.parseInt(componentData.substring(2).replace("+", ""));
        } catch (NumberFormatException e) {
            return;
        }
        if (componentData.startsWith("oc")) {
            overclockTenths = Mth.clamp(overclockTenths + step, 0, MAX_RATIO);
        } else if (componentData.startsWith("ov")) {
            overvoltTenths = Mth.clamp(overvoltTenths + step, 0, MAX_RATIO);
        } else {
            return;
        }
        markDirty();
    }
}
