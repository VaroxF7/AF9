package com.af9.core.bus;

import com.af9.core.client.render.MachineBusRenderer;

import com.gregtechceu.gtceu.api.item.IComponentItem;
import com.gregtechceu.gtceu.api.item.component.IAddInformation;
import com.gregtechceu.gtceu.api.item.component.IItemComponent;
import com.gregtechceu.gtceu.api.item.component.IMonitorModuleItem;
import com.gregtechceu.gtceu.api.machine.MetaMachine;
import com.gregtechceu.gtceu.api.machine.feature.multiblock.IMultiPart;
import com.gregtechceu.gtceu.client.renderer.monitor.IMonitorRenderer;
import com.gregtechceu.gtceu.common.machine.multiblock.electric.CentralMonitorMachine;
import com.gregtechceu.gtceu.common.machine.multiblock.electric.monitor.MonitorGroup;
import com.gregtechceu.gtceu.common.machine.multiblock.part.monitor.AdvancedMonitorPartMachine;
import com.gregtechceu.gtceu.common.network.GTNetwork;
import com.gregtechceu.gtceu.common.network.packets.SCPacketMonitorGroupNBTChange;

import com.lowdragmc.lowdraglib.gui.util.ClickData;
import com.lowdragmc.lowdraglib.gui.widget.ComponentPanelWidget;
import com.lowdragmc.lowdraglib.gui.widget.Widget;
import com.lowdragmc.lowdraglib.gui.widget.WidgetGroup;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * Machine Bus Module: a module for GT's Central Monitor that shows the machines on the monitor's bus and sends them
 * commands. Its monitor group reaches the bus through a Bus Connector in the monitor's wall (the group's target if
 * that is a connector, else the wall's first one).
 * <p>
 * Every monitor tick (faster at higher monitor tiers) it takes a snapshot of each machine ({@link BusData#snapshot})
 * into the module's NBT, which GT sends to the players; the screen draws from it ({@link MachineBusRenderer}) and runs
 * the progress bar on between two ticks. Two views: one machine in detail, with touch buttons (start/stop, batch, the
 * mode) on Advanced Monitors, or a table of all machines (a tap opens one). The settings (which machine, the view,
 * what to show, the buttons) and the same commands are on the module's page of the Central Monitor's screen.
 */
public class MachineBusModule implements IMonitorModuleItem, IAddInformation {

    // settings
    public static final String VIEW = "view", SELECTED = "sel", SHOW = "show", NO_TOUCH = "notouch";
    // data
    public static final String TIME = "t", PORT = "port", DEVICES = "dev";
    public static final int PORT_NONE = 0, PORT_EMPTY = 1, PORT_OK = 2;

    //////////////////////////////////////
    // *********** Settings ************//
    //////////////////////////////////////

    /** The fields the screen shows (bits of {@link BusData}); all until set. */
    public static int shown(CompoundTag tag) {
        return tag.contains(SHOW) ? tag.getInt(SHOW) : BusData.ALL_DATA;
    }

    public static boolean touchEnabled(CompoundTag tag) {
        return !tag.getBoolean(NO_TOUCH);
    }

    public static boolean isModule(ItemStack stack) {
        if (!(stack.getItem() instanceof IComponentItem item)) return false;
        for (IItemComponent component : item.getComponents()) {
            if (component instanceof MachineBusModule) return true;
        }
        return false;
    }

    /** The Bus Connector in the wall a group reads the bus through, or null. */
    public static BusConnectorPartMachine findPort(CentralMonitorMachine monitor, MonitorGroup group) {
        Level level = monitor.getLevel();
        BlockPos target = group.getTargetRaw();
        if (level != null && target != null && MetaMachine.getMachine(level, target) instanceof
                BusConnectorPartMachine connector) {
            return connector;
        }
        for (IMultiPart part : monitor.getParts()) {
            if (part instanceof BusConnectorPartMachine connector) return connector;
        }
        return null;
    }

    //////////////////////////////////////
    // ************* Data **************//
    //////////////////////////////////////

    @Override
    public void tick(ItemStack stack, CentralMonitorMachine machine, MonitorGroup group) {
        update(stack, machine, group);
    }

    /** Takes a snapshot of every machine on the bus into the module. */
    public static void update(ItemStack stack, CentralMonitorMachine monitor, MonitorGroup group) {
        CompoundTag tag = stack.getOrCreateTag();
        Level level = monitor.getLevel();
        if (level != null) tag.putLong(TIME, level.getGameTime());
        BusConnectorPartMachine port = findPort(monitor, group);
        ListTag devices = new ListTag();
        if (port != null) {
            for (BusConnectorPartMachine connector : port.getMachinesOnBus()) {
                devices.add(BusData.snapshot(connector));
            }
        }
        tag.put(DEVICES, devices);
        tag.putInt(PORT, port == null ? PORT_NONE : devices.isEmpty() ? PORT_EMPTY : PORT_OK);
        CompoundTag selected = BusScreenLayout.selected(tag);
        if (selected != null) tag.putLong(SELECTED, selected.getLong(BusData.POS));
    }

    /** Sends the module to the players at once (as GT's monitor tick does). */
    public static void sync(CentralMonitorMachine monitor, MonitorGroup group, ItemStack stack) {
        Level level = monitor.getLevel();
        if (level == null || level.isClientSide) return;
        GTNetwork.sendToAllPlayersTrackingChunk(level.getChunkAt(monitor.getPos()),
                new SCPacketMonitorGroupNBTChange(stack, group, monitor));
        monitor.markDirty();
    }

    //////////////////////////////////////
    // ************ Actions ************//
    //////////////////////////////////////

    /**
     * Runs an action of the screen or the settings page (server). Commands go to the selected machine, and only if
     * it is on the port's bus.
     *
     * @return whether anything changed
     */
    public static boolean run(String id, CentralMonitorMachine monitor, MonitorGroup group) {
        ItemStack stack = group.getItemStackHandler().getStackInSlot(0);
        if (!isModule(stack)) return false;
        CompoundTag tag = stack.getOrCreateTag();
        ListTag devices = BusScreenLayout.devices(tag);
        switch (id) {
            case "prev", "next" -> {
                if (devices.isEmpty()) return false;
                int index = selectedIndex(tag);
                index = Math.floorMod(index + (id.equals("next") ? 1 : -1), devices.size());
                tag.putLong(SELECTED, devices.getCompound(index).getLong(BusData.POS));
            }
            case "view" -> tag.putInt(VIEW, BusScreenLayout.view(tag) == BusScreenLayout.VIEW_TABLE ?
                    BusScreenLayout.VIEW_DETAIL : BusScreenLayout.VIEW_TABLE);
            case "list" -> tag.putInt(VIEW, BusScreenLayout.VIEW_TABLE);
            case "touch" -> tag.putBoolean(NO_TOUCH, touchEnabled(tag));
            case "power" -> {
                if (!command(tag, monitor, group, BusData.CMD_POWER, 0)) return false;
            }
            case "batch" -> {
                if (!command(tag, monitor, group, BusData.CMD_BATCH, 0)) return false;
            }
            case "mode_prev", "mode_next" -> {
                if (!command(tag, monitor, group, BusData.CMD_MODE, id.equals("mode_next") ? 1 : -1)) return false;
            }
            default -> {
                if (id.startsWith("row:")) {
                    int row = parse(id.substring(4));
                    if (row < 0 || row >= devices.size()) return false;
                    tag.putLong(SELECTED, devices.getCompound(row).getLong(BusData.POS));
                    tag.putInt(VIEW, BusScreenLayout.VIEW_DETAIL);
                } else if (id.startsWith("s")) {
                    int bit = parse(id.substring(1));
                    if (bit < 0 || bit >= BusData.DATA_KEYS.length) return false;
                    tag.putInt(SHOW, shown(tag) ^ (1 << bit));
                } else {
                    return false;
                }
            }
        }
        update(stack, monitor, group);
        sync(monitor, group, stack);
        return true;
    }

    private static boolean command(CompoundTag tag, CentralMonitorMachine monitor, MonitorGroup group, int command,
                                   int step) {
        BusConnectorPartMachine port = findPort(monitor, group);
        if (port == null) return false;
        long selected = tag.getLong(SELECTED);
        for (BusConnectorPartMachine connector : port.getMachinesOnBus()) {
            if (connector.getPos().asLong() == selected) return BusData.command(connector, command, step);
        }
        return false;
    }

    private static int selectedIndex(CompoundTag tag) {
        ListTag devices = BusScreenLayout.devices(tag);
        long selected = tag.getLong(SELECTED);
        for (int i = 0; i < devices.size(); i++) {
            if (devices.getCompound(i).getLong(BusData.POS) == selected) return i;
        }
        return 0;
    }

    private static int parse(String text) {
        try {
            return Integer.parseInt(text);
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    /**
     * The taps on the Advanced Monitors of the port's module screens (the port calls this every other tick). A tap on
     * a button runs its action; the monitor forgets the tap either way. Only while the monitor is on.
     */
    public static void handleTouches(CentralMonitorMachine monitor, BusConnectorPartMachine port) {
        Level level = monitor.getLevel();
        if (level == null || !monitor.isFormed()) return;
        boolean on = monitor.getRecipeLogic().isActive();
        for (MonitorGroup group : monitor.getMonitorGroups()) {
            ItemStack stack = group.getItemStackHandler().getStackInSlot(0);
            if (!isModule(stack) || findPort(monitor, group) != port) continue;
            BusScreenLayout.Screen screen = BusScreenLayout.screen(monitor, group);
            if (screen == null) continue;
            for (BlockPos pos : group.getRelativePositions()) {
                if (!(MetaMachine.getMachine(level, pos) instanceof AdvancedMonitorPartMachine touch) ||
                        !touch.isClicked()) {
                    continue;
                }
                touch.resetClicked();
                if (!on) continue;
                CompoundTag tag = stack.getOrCreateTag();
                float units = BusScreenLayout.unitsPerBlock(screen, tag);
                float[] at = BusScreenLayout.tapPosition(monitor, screen, pos, touch.getClickPosX(),
                        touch.getClickPosY(), units);
                for (BusScreenLayout.Button button : BusScreenLayout.buttons(tag, screen.cols() * units,
                        screen.rows() * units)) {
                    if (button.rect().contains(at[0], at[1])) {
                        run(button.id(), monitor, group);
                        break;
                    }
                }
            }
        }
    }

    //////////////////////////////////////
    // ********* Screen and UI *********//
    //////////////////////////////////////

    @Override
    public IMonitorRenderer getRenderer(ItemStack stack, CentralMonitorMachine machine, MonitorGroup group) {
        return new MachineBusRenderer(stack.getOrCreateTag());
    }

    /** The module's page in the Central Monitor's screen: clickable lines, run on the server. */
    @Override
    public Widget createUIWidget(ItemStack stack, CentralMonitorMachine machine, MonitorGroup group) {
        var page = new WidgetGroup(0, 0, 140, 110);
        boolean client = machine.getLevel() != null && machine.getLevel().isClientSide;
        page.addWidget(new ComponentPanelWidget(0, 0, text -> settingsText(text, machine, group))
                .textSupplier(client ? null : text -> settingsText(text, machine, group))
                .setMaxWidthLimit(140)
                .clickHandler((id, click) -> handleSettingsClick(id, click, machine, group)));
        return page;
    }

    private static void settingsText(List<Component> text, CentralMonitorMachine monitor, MonitorGroup group) {
        ItemStack stack = group.getItemStackHandler().getStackInSlot(0);
        if (!isModule(stack)) return;
        CompoundTag tag = stack.getOrCreateTag();
        ListTag devices = BusScreenLayout.devices(tag);
        text.add(Component.translatable("af9.bus.module.title").withStyle(ChatFormatting.GOLD));
        BusConnectorPartMachine port = findPort(monitor, group);
        text.add(port == null ? Component.translatable("af9.bus.module.no_port").withStyle(ChatFormatting.RED) :
                devices.isEmpty() ? Component.translatable("af9.bus.module.no_machines")
                        .withStyle(ChatFormatting.YELLOW) :
                        Component.translatable("af9.bus.module.machines", devices.size())
                                .withStyle(ChatFormatting.GRAY));
        CompoundTag selected = BusScreenLayout.selected(tag);
        if (selected != null) {
            text.add(Component.empty()
                    .append(ComponentPanelWidget.withButton(Component.literal("◀ "), "prev"))
                    .append(deviceName(selected).copy().withStyle(ChatFormatting.WHITE))
                    .append(ComponentPanelWidget.withButton(Component.literal(" ▶"), "next")));
        }
        boolean table = BusScreenLayout.view(tag) == BusScreenLayout.VIEW_TABLE;
        text.add(Component.translatable("af9.bus.module.view").append(" ")
                .append(ComponentPanelWidget.withButton(Component.translatable(table ? "af9.bus.module.view.table" :
                        "af9.bus.module.view.detail").withStyle(ChatFormatting.AQUA), "view"))
                .append("  ").append(Component.translatable("af9.bus.module.touch")).append(" ")
                .append(ComponentPanelWidget.withButton(Component.translatable(touchEnabled(tag) ?
                        "af9.bus.module.on" : "af9.bus.module.off").withStyle(touchEnabled(tag) ?
                                ChatFormatting.GREEN : ChatFormatting.DARK_GRAY), "touch")));
        text.add(Component.translatable("af9.bus.module.show").withStyle(ChatFormatting.GOLD));
        text.add(BusConnectorPartMachine.toggles(BusData.DATA_KEYS, "af9.bus.data.", shown(tag), "s"));
        if (selected != null && selected.getBoolean(BusData.FORMED)) {
            int accepts = selected.getInt(BusData.ACCEPTS);
            MutableComponent commands = Component.translatable("af9.bus.module.send").withStyle(ChatFormatting.GOLD);
            if ((accepts & BusData.CMD_POWER) != 0) {
                commands.append(" ").append(ComponentPanelWidget.withButton(Component.translatable(
                        selected.getBoolean(BusData.ENABLED) ? "af9.bus.button.stop" : "af9.bus.button.start")
                        .withStyle(ChatFormatting.AQUA), "power"));
            }
            if ((accepts & BusData.CMD_BATCH) != 0) {
                commands.append(" ").append(ComponentPanelWidget.withButton(
                        Component.translatable("af9.bus.button.batch").withStyle(ChatFormatting.AQUA), "batch"));
            }
            if ((accepts & BusData.CMD_MODE) != 0 && selected.getInt(BusData.MODES) > 1) {
                commands.append(" ").append(ComponentPanelWidget.withButton(Component.literal("◀")
                        .withStyle(ChatFormatting.AQUA), "mode_prev"))
                        .append(Component.translatable("af9.bus.button.mode"))
                        .append(ComponentPanelWidget.withButton(Component.literal("▶")
                                .withStyle(ChatFormatting.AQUA), "mode_next"));
            }
            if (accepts == 0) commands.append(" ").append(Component.translatable("af9.bus.module.read_only")
                    .withStyle(ChatFormatting.DARK_GRAY));
            text.add(commands);
        }
    }

    private static void handleSettingsClick(String id, ClickData click, CentralMonitorMachine monitor,
                                            MonitorGroup group) {
        if (click.isRemote) return;
        run(id, monitor, group);
    }

    /** A machine's name on the bus: its connector's label, or the machine's own name. */
    public static Component deviceName(CompoundTag device) {
        String label = device.getString(BusData.LABEL);
        if (!label.isEmpty()) return Component.literal(label);
        String key = device.getString(BusData.NAME_KEY);
        return key.isEmpty() ? Component.translatable("af9.bus.module.unknown") : Component.translatable(key);
    }

    @Override
    public String getType() {
        return "af9_machine_bus";
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        for (int i = 0; i < 3; i++) {
            tooltip.add(Component.translatable("af9.bus.module.tooltip." + i).withStyle(ChatFormatting.GRAY));
        }
    }
}
