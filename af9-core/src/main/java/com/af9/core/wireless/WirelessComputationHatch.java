package com.af9.core.wireless;

import com.gregtechceu.gtceu.api.capability.IOpticalComputationProvider;
import com.gregtechceu.gtceu.api.capability.recipe.IO;
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.gregtechceu.gtceu.api.machine.trait.NotifiableComputationContainer;
import com.gregtechceu.gtceu.api.recipe.GTRecipe;
import com.gregtechceu.gtceu.common.machine.multiblock.part.OpticalComputationHatchMachine;
import com.gregtechceu.gtceu.utils.FormattingUtil;

import com.lowdragmc.lowdraglib.gui.widget.ComponentPanelWidget;
import com.lowdragmc.lowdraglib.gui.widget.TextFieldWidget;
import com.lowdragmc.lowdraglib.gui.widget.Widget;
import com.lowdragmc.lowdraglib.gui.widget.WidgetGroup;
import com.lowdragmc.lowdraglib.syncdata.annotation.DescSynced;
import com.lowdragmc.lowdraglib.syncdata.annotation.Persisted;
import com.lowdragmc.lowdraglib.syncdata.field.ManagedFieldHolder;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.BlockHitResult;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Wireless Computation Transmitter / Receiver. The transmitter sits on a multiblock that is a computation source (an
 * HPCA) and sends its computation under the ID typed on the hatch's screen; every receiver with the same ID gives a
 * multiblock that computation, any distance, any dimension.
 * <p>
 * Nothing about the link is stored outside the two hatches: each keeps its ID in its own save data ({@link #linkId}),
 * and the transmitters announce themselves in a registry in memory when they load ({@link #onLoad}) and leave it when
 * they unload. So a restart or an unloaded chunk cannot break a link: a receiver whose transmitter is not loaded
 * simply has no computation for the moment, and finds it again by ID as soon as the transmitter loads.
 */
public class WirelessComputationHatch extends OpticalComputationHatchMachine {

    protected static final ManagedFieldHolder MANAGED_FIELD_HOLDER = new ManagedFieldHolder(
            WirelessComputationHatch.class, OpticalComputationHatchMachine.MANAGED_FIELD_HOLDER);

    /** Loaded transmitters by ID (in memory only; rebuilt by the transmitters' own load). */
    private static final Map<String, WirelessComputationHatch> TRANSMITTERS = new ConcurrentHashMap<>();

    public static final int MAX_ID_LENGTH = 32;

    /** The link ID, as typed on the screen; "" none. */
    @Persisted
    @DescSynced
    private String linkId = "";
    private String registeredId = "";

    public WirelessComputationHatch(IMachineBlockEntity holder, boolean transmitter) {
        super(holder, transmitter);
    }

    @Override
    public ManagedFieldHolder getFieldHolder() {
        return MANAGED_FIELD_HOLDER;
    }

    /** Called from the super constructor: the receiver gets a container that asks the registry, not an optical cable. */
    @Override
    protected NotifiableComputationContainer createComputationContainer(Object... args) {
        if (args.length > 0 && args[args.length - 1] instanceof Boolean transmitter && !transmitter) {
            return new ReceiverContainer(this);
        }
        return super.createComputationContainer(args);
    }

    public String getLinkId() {
        return linkId;
    }

    /** The screen's text field: the new ID. */
    public void setLinkId(String id) {
        String clean = id == null ? "" : id.trim();
        if (clean.length() > MAX_ID_LENGTH) clean = clean.substring(0, MAX_ID_LENGTH);
        if (clean.equals(linkId)) return;
        linkId = clean;
        markDirty();
        register();
    }

    ////////////////////////////////////////////////
    // ********* Registry (transmitters) *********//
    ////////////////////////////////////////////////

    @Override
    public void onLoad() {
        super.onLoad();
        register();
    }

    @Override
    public void onUnload() {
        super.onUnload();
        unregister();
    }

    private boolean onServer() {
        return getLevel() != null && !getLevel().isClientSide;
    }

    private void unregister() {
        if (!registeredId.isEmpty()) TRANSMITTERS.remove(registeredId, this);
        registeredId = "";
    }

    private void register() {
        if (!isTransmitter() || !onServer()) return;
        unregister();
        if (linkId.isEmpty()) return;
        // the first transmitter with an ID keeps it; a second one with the same ID shows a warning and sends nothing
        if (TRANSMITTERS.putIfAbsent(linkId, this) == null) registeredId = linkId;
    }

    /** Whether this transmitter is the one the receivers of its ID reach. */
    public boolean isLinkOwner() {
        return !linkId.isEmpty() && TRANSMITTERS.get(linkId) == this;
    }

    /** The computation source the receivers of an ID reach: the loaded transmitter's, or null. */
    private static IOpticalComputationProvider providerOf(String id) {
        if (id.isEmpty()) return null;
        WirelessComputationHatch transmitter = TRANSMITTERS.get(id);
        if (transmitter == null || transmitter.getLevel() == null) return null;
        return transmitter.computationContainer;
    }

    /** What a receiver of this ID can draw now, CWU/t (0: no transmitter loaded, or nothing behind it). */
    private static int availableFor(String id) {
        IOpticalComputationProvider provider = providerOf(id);
        return provider == null ? 0 : Math.max(0, provider.getMaxCWUt(new ArrayList<>()));
    }

    /** The receiver's container: forwards to the transmitter of its ID. */
    private static final class ReceiverContainer extends NotifiableComputationContainer {

        private final WirelessComputationHatch hatch;

        ReceiverContainer(WirelessComputationHatch hatch) {
            super(hatch, IO.IN, false);
            this.hatch = hatch;
        }

        private IOpticalComputationProvider source(Collection<IOpticalComputationProvider> seen) {
            seen.add(this);
            IOpticalComputationProvider provider = providerOf(hatch.linkId);
            // a ring of links would ask itself for ever
            return provider == null || seen.contains(provider) ? null : provider;
        }

        @Override
        public int requestCWUt(int cwut, boolean simulate, Collection<IOpticalComputationProvider> seen) {
            IOpticalComputationProvider provider = source(seen);
            return provider == null ? 0 : provider.requestCWUt(cwut, simulate, seen);
        }

        @Override
        public int getMaxCWUt(Collection<IOpticalComputationProvider> seen) {
            IOpticalComputationProvider provider = source(seen);
            return provider == null ? 0 : provider.getMaxCWUt(seen);
        }

        @Override
        public boolean canBridge(Collection<IOpticalComputationProvider> seen) {
            IOpticalComputationProvider provider = source(seen);
            return provider == null || provider.canBridge(seen);
        }

        @Override
        public List<Integer> handleRecipeInner(IO io, GTRecipe recipe, List<Integer> left, boolean simulate) {
            if (io != IO.IN) return left;
            int sum = left.stream().mapToInt(Integer::intValue).sum();
            if (requestCWUt(Integer.MAX_VALUE, true) >= sum) sum -= requestCWUt(sum, simulate);
            return sum <= 0 ? null : Collections.singletonList(sum);
        }
    }

    //////////////////////////////////////
    // ************ GUI *************//
    //////////////////////////////////////

    @Override
    public boolean shouldOpenUI(Player player, InteractionHand hand, BlockHitResult hit) {
        return true;
    }

    @Override
    public Widget createUIWidget() {
        var group = new WidgetGroup(0, 0, 182, 80);
        group.addWidget(new ComponentPanelWidget(4, 4, this::addDisplayText)
                .textSupplier(getLevel() != null && getLevel().isClientSide ? null : this::addDisplayText)
                .setMaxWidthLimit(174));
        group.addWidget(new TextFieldWidget(4, 58, 120, 16, () -> linkId, this::setLinkId)
                .setMaxStringLength(MAX_ID_LENGTH)
                .setHoverTooltips(Component.translatable("af9.wireless_cwu.id_hint")));
        return group;
    }

    private void addDisplayText(List<Component> text) {
        text.add(Component.translatable(isTransmitter() ? "af9.wireless_cwu.transmitter.title" :
                "af9.wireless_cwu.receiver.title").withStyle(ChatFormatting.AQUA));
        if (linkId.isEmpty()) {
            text.add(Component.translatable("af9.wireless_cwu.no_id").withStyle(ChatFormatting.YELLOW));
        } else if (isTransmitter()) {
            if (!isLinkOwner()) {
                text.add(Component.translatable("af9.wireless_cwu.id_taken").withStyle(ChatFormatting.RED));
            } else if (!isFormed()) {
                text.add(Component.translatable("af9.wireless_cwu.not_formed").withStyle(ChatFormatting.YELLOW));
            } else {
                text.add(Component.translatable("af9.wireless_cwu.sending",
                        FormattingUtil.formatNumbers(availableFor(linkId))));
            }
        } else if (providerOf(linkId) == null) {
            text.add(Component.translatable("af9.wireless_cwu.no_transmitter").withStyle(ChatFormatting.YELLOW));
        } else {
            text.add(Component.translatable("af9.wireless_cwu.receiving",
                    FormattingUtil.formatNumbers(availableFor(linkId))));
        }
        text.add(Component.translatable("af9.wireless_cwu.id_label").withStyle(ChatFormatting.GRAY));
    }
}
