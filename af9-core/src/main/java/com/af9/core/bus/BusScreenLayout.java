package com.af9.core.bus;

import com.gregtechceu.gtceu.api.pattern.util.RelativeDirection;
import com.gregtechceu.gtceu.common.machine.multiblock.electric.CentralMonitorMachine;
import com.gregtechceu.gtceu.common.machine.multiblock.electric.monitor.MonitorGroup;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

import java.util.ArrayList;
import java.util.List;

/**
 * Where things are on a Machine Bus Module's screen: the same on both sides, the client draws with it, the server
 * hit-tests the taps on the Advanced Monitors with it.
 * <p>
 * A screen is the bounding box of its monitor group, in blocks; the canvas inside is measured in units (the font is
 * 9 units high), as many per block as the view needs to fit ({@link #unitsPerBlock}). x runs right and y down as the
 * player sees the wall.
 */
public final class BusScreenLayout {

    /** The views; the console view only while the module holds a console's state (else the detail view). */
    public static final int VIEW_DETAIL = 0, VIEW_TABLE = 1, VIEW_CONSOLE = 2, VIEWS = 3;
    public static final float PAD = 6, DETAIL_W = 200, DETAIL_H = 124, TABLE_W = 200, HEADER_H = 18, ROW_H = 14;
    public static final float BUTTON_H = 16, BUTTON_GAP = 4;
    /** Detail view: the bar and the stat lines. */
    public static final float STATUS_Y = PAD + 14, BAR_Y = PAD + 28, BAR_H = 12, STATS_Y = PAD + 46, LINE_H = 11;

    private BusScreenLayout() {}

    /** A screen: its top left monitor (relative to the wall, see {@link CentralMonitorMachine#toRelative}), size. */
    public record Screen(int minX, int minY, int cols, int rows) {}

    public record Rect(float x, float y, float w, float h) {

        public boolean contains(float px, float py) {
            return px >= x && px < x + w && py >= y && py < y + h;
        }
    }

    /** A tap target: an action id of {@link MachineBusModule#run} and where it is. */
    public record Button(String id, Rect rect) {}

    public static Screen screen(CentralMonitorMachine monitor, MonitorGroup group) {
        int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE, maxX = Integer.MIN_VALUE, maxY = Integer.MIN_VALUE;
        for (BlockPos pos : group.getRelativePositions()) {
            BlockPos rel = monitor.toRelative(pos);
            minX = Math.min(minX, rel.getX());
            minY = Math.min(minY, rel.getY());
            maxX = Math.max(maxX, rel.getX());
            maxY = Math.max(maxY, rel.getY());
        }
        if (minX > maxX) return null;
        return new Screen(minX, minY, maxX - minX + 1, maxY - minY + 1);
    }

    /** The view the screen shows. */
    public static int view(CompoundTag tag) {
        int view = tag.getInt(MachineBusModule.VIEW);
        if (view == VIEW_CONSOLE) return tag.contains(MachineBusModule.CONSOLE) ? VIEW_CONSOLE : VIEW_DETAIL;
        return view == VIEW_TABLE ? VIEW_TABLE : VIEW_DETAIL;
    }

    public static ListTag devices(CompoundTag tag) {
        return tag.getList(MachineBusModule.DEVICES, Tag.TAG_COMPOUND);
    }

    /** The selected machine's snapshot (the first one if the selection is gone), or null. */
    public static CompoundTag selected(CompoundTag tag) {
        ListTag devices = devices(tag);
        long sel = tag.getLong(MachineBusModule.SELECTED);
        for (int i = 0; i < devices.size(); i++) {
            if (devices.getCompound(i).getLong(BusData.POS) == sel) return devices.getCompound(i);
        }
        return devices.isEmpty() ? null : devices.getCompound(0);
    }

    /** Units per block: the view's design size fits into the screen either way. */
    public static float unitsPerBlock(Screen screen, CompoundTag tag) {
        float w, h;
        switch (view(tag)) {
            case VIEW_TABLE -> {
                w = TABLE_W;
                h = HEADER_H + ROW_H * Math.max(3, devices(tag).size()) + PAD;
            }
            case VIEW_CONSOLE -> {
                w = Math.max(1, tag.getInt(MachineBusModule.CONSOLE_W));
                h = Math.max(1, tag.getInt(MachineBusModule.CONSOLE_H));
            }
            default -> {
                w = DETAIL_W;
                h = DETAIL_H;
            }
        }
        return Math.max(w / screen.cols(), h / screen.rows());
    }

    /** Rows the table shows on a canvas this high. */
    public static int tableRows(float height) {
        return Math.max(0, (int) ((height - HEADER_H - PAD) / ROW_H));
    }

    public static Rect row(int index, float width) {
        return new Rect(PAD, HEADER_H + index * ROW_H, width - 2 * PAD, ROW_H);
    }

    /** The tap targets of the screen (canvas w x h units). */
    public static List<Button> buttons(CompoundTag tag, float w, float h) {
        List<Button> buttons = new ArrayList<>();
        ListTag devices = devices(tag);
        if (view(tag) == VIEW_CONSOLE) return buttons;
        if (view(tag) == VIEW_TABLE) {
            int rows = Math.min(devices.size(), tableRows(h));
            for (int i = 0; i < rows; i++) buttons.add(new Button("row:" + i, row(i, w)));
            return buttons;
        }
        if (!MachineBusModule.touchEnabled(tag)) return buttons;
        CompoundTag device = selected(tag);
        float y = h - PAD - BUTTON_H;
        float x = PAD;
        if (device != null && device.getBoolean(BusData.FORMED)) {
            int accepts = device.getInt(BusData.ACCEPTS);
            if ((accepts & BusData.CMD_POWER) != 0) {
                buttons.add(new Button("power", new Rect(x, y, 36, BUTTON_H)));
                x += 36 + BUTTON_GAP;
            }
            if ((accepts & BusData.CMD_BATCH) != 0) {
                buttons.add(new Button("batch", new Rect(x, y, 36, BUTTON_H)));
                x += 36 + BUTTON_GAP;
            }
            if ((accepts & BusData.CMD_MODE) != 0 && device.getInt(BusData.MODES) > 1) {
                buttons.add(new Button("mode_prev", new Rect(x, y, 14, BUTTON_H)));
                buttons.add(new Button("mode_next", new Rect(x + 16, y, 40, BUTTON_H)));
                buttons.add(new Button("mode_next", new Rect(x + 58, y, 14, BUTTON_H)));
            }
        }
        if (devices.size() > 1) buttons.add(new Button("list", new Rect(w - PAD - 30, y, 30, BUTTON_H)));
        return buttons;
    }

    /**
     * Where a tap on a monitor lands on the canvas, in units: its place in the group plus the tap within the block.
     * GT keeps the tap as fractions along the monitor's right axis and the world's up (see
     * {@code AdvancedMonitorPartMachine#onUse}); the canvas runs the other way on some sides.
     */
    public static float[] tapPosition(CentralMonitorMachine monitor, Screen screen, BlockPos monitorPos,
                                      double clickX, double clickY, float unitsPerBlock) {
        Direction front = monitor.getFrontFacing();
        Direction up = monitor.getUpwardsFacing();
        boolean flipped = monitor.isFlipped();
        Direction canvasRight = RelativeDirection.RIGHT.getRelative(front, up, flipped).getOpposite();
        Direction canvasDown = RelativeDirection.UP.getRelative(front, up, flipped).getOpposite();
        double fx = canvasRight.getAxisDirection() == Direction.AxisDirection.POSITIVE ? clickX : 1 - clickX;
        Direction.Axis yAxis = front.getAxis().isVertical() ? Direction.Axis.X : Direction.Axis.Y;
        double fy = canvasDown.getAxis() != yAxis ? clickY :
                canvasDown.getAxisDirection() == Direction.AxisDirection.POSITIVE ? clickY : 1 - clickY;
        BlockPos rel = monitor.toRelative(monitorPos);
        return new float[] { (float) ((rel.getX() - screen.minX() + fx) * unitsPerBlock),
                (float) ((rel.getY() - screen.minY() + fy) * unitsPerBlock) };
    }
}
