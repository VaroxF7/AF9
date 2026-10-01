package com.af9.core.compat.jade;

import com.af9.core.AF9Core;
import com.af9.core.litho.LithoMode;
import com.af9.core.machine.LithoConsoleWidget;
import com.af9.core.machine.LithoMachine;
import com.af9.core.machine.OrbitalLithographyMachine;
import com.af9.core.machine.ProcessMachine;
import com.af9.core.machine.console.ConsoleWidget;

import com.gregtechceu.gtceu.api.blockentity.MetaMachineBlockEntity;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.registries.ForgeRegistries;

import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.IServerDataProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.TooltipPosition;
import snownee.jade.api.config.IPluginConfig;
import snownee.jade.api.ui.BoxStyle;
import snownee.jade.api.ui.IElementHelper;

import java.util.Locale;

/**
 * Jade tooltip of the AF9 machines, right under the block name:
 * <ul>
 * <li>lithography machines: the vacuum cleanliness bar, what is printed (product, node, substrate), the run-time bar,
 * the break chance and the line version; GT's own run-time bar is left out for them;</li>
 * <li>process machines (cryostat, accelerator): status and mode, what is made, the run-time bar and the machine's own
 * readouts (the same lines as its console).</li>
 * </ul>
 */
@SuppressWarnings("removal") // new ResourceLocation(ns, path) is the only constructor on 1.20.1
public enum AF9MachineProvider implements IBlockComponentProvider, IServerDataProvider<BlockAccessor> {

    INSTANCE;

    private static final ResourceLocation UID = new ResourceLocation(AF9Core.MOD_ID, "machine_status");
    private static final String KEY = "af9Machine";
    /** Server data of GT's run-time bar; the lithography machines show their own (see {@link #appendTooltip}). */
    private static final String GT_WORKABLE_DATA = "gtceu:workable_provider";
    private static final int BORDER = 0xFF555555;

    @Override
    public ResourceLocation getUid() {
        return UID;
    }

    @Override
    public int getDefaultPriority() {
        return TooltipPosition.HEAD + 50;
    }

    //////////////////////////////////////
    // ********** Server ***********//
    //////////////////////////////////////

    @Override
    public void appendServerData(CompoundTag data, BlockAccessor accessor) {
        if (!(accessor.getBlockEntity() instanceof MetaMachineBlockEntity blockEntity)) return;
        CompoundTag tag = new CompoundTag();
        var machine = blockEntity.getMetaMachine();
        if (machine instanceof LithoMachine litho) {
            LithoMode mode = litho.getActiveMode();
            var logic = litho.getRecipeLogic();
            ResourceLocation product = litho.getCurrentProduct();
            tag.putString("kind", "litho");
            tag.putInt("status", LithoConsoleWidget.statusOf(litho));
            tag.putInt("mode", mode.ordinal());
            tag.putInt("version", litho.getVersion());
            // the orbital station has no vacuum: its start-up instead
            boolean startup = litho instanceof OrbitalLithographyMachine;
            tag.putBoolean("startup", startup);
            tag.putDouble("clean", startup ? ((OrbitalLithographyMachine) litho).getStartupPercent() :
                    litho.getCleanliness());
            tag.putDouble("break", litho.currentBreakChance(mode));
            boolean airCooled = litho.needsAirCooling(mode);
            tag.putInt("coolLoad", airCooled ? mode.heatLoad() : 0);
            tag.putInt("coolCap", airCooled ? litho.getCoolingCapacity() : 0);
            tag.putInt("coolSteps", litho.coolingSteps(mode));
            tag.putBoolean("coolLapsed", litho.hasCoolingLapsed() && logic.isWorking());
            tag.putDouble("opc", litho.getOpcRatio(mode, logic.isWorking()));
            tag.putDouble("cal", litho.getCalibration());
            tag.putInt("vacuum", litho.getVacuumState());
            tag.putString("product", product == null ? "" : "item:" + product);
            tag.putInt("progress", logic.isWorking() ? logic.getProgress() : 0);
            tag.putInt("duration", logic.isWorking() ? logic.getDuration() : 0);
        } else if (machine instanceof ProcessMachine process) {
            var logic = process.getRecipeLogic();
            tag.putString("kind", "process");
            tag.putInt("status", process.getStatus());
            tag.putString("modeKey", ProcessMachine.modeKey(process.getRecipeType()));
            tag.putString("product", process.getCurrentOutput());
            tag.putInt("progress", logic.isWorking() ? logic.getProgress() : 0);
            tag.putInt("duration", logic.isWorking() ? logic.getDuration() : 0);
            ListTag lines = new ListTag();
            for (Component line : process.infoLines()) lines.add(StringTag.valueOf(Component.Serializer.toJson(line)));
            tag.put("lines", lines);
        } else {
            return;
        }
        data.put(KEY, tag);
    }

    //////////////////////////////////////
    // ********** Client ***********//
    //////////////////////////////////////

    @Override
    public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
        CompoundTag data = accessor.getServerData();
        if (!data.contains(KEY, Tag.TAG_COMPOUND)) return;
        CompoundTag tag = data.getCompound(KEY);
        IElementHelper helper = tooltip.getElementHelper();
        int status = tag.getInt("status");
        boolean running = status == ConsoleWidget.STATUS_RUNNING;
        int progress = tag.getInt("progress");
        int duration = tag.getInt("duration");

        if (tag.getString("kind").equals("litho")) {
            // GT's run-time bar would repeat ours: this provider runs first (HEAD), so its data can go before GT reads it
            data.remove(GT_WORKABLE_DATA);
            LithoMode[] modes = LithoMode.values();
            LithoMode mode = modes[Math.max(0, Math.min(modes.length - 1, tag.getInt("mode")))];
            double clean = tag.getDouble("clean");
            // vacuum (the station: its start-up) first, right under the name
            int vacuumState = status == ConsoleWidget.STATUS_OFFLINE ? LithoMachine.VACUUM_OFF : tag.getInt("vacuum");
            Component vacuum = tag.getBoolean("startup") ?
                    Component.translatable("af9.jade.startup", String.format(Locale.ROOT, "%.0f", clean),
                            Component.translatable(startupKey(vacuumState))) :
                    Component.translatable("af9.jade.vacuum", String.format(Locale.ROOT, "%.1f", clean),
                            Component.translatable(vacuumKey(vacuumState)));
            tooltip.add(helper.progress((float) (clean / 100.0), vacuum,
                    helper.progressStyle().color(ConsoleWidget.levelColor(clean)).textColor(-1), box(), true));
            tooltip.add(statusLine(status, Component.translatable("af9.litho.mode." + mode.id)
                    .withStyle(mode.color)));
            if (running) {
                Component product = productName(tag.getString("product"));
                if (product != null) {
                    tooltip.add(Component.translatable("af9.jade.printing", product,
                            Component.translatable("af9.litho.substrate." + mode.substrate))
                            .withStyle(ChatFormatting.GRAY));
                }
                tooltip.add(runtime(helper, progress, duration, mode.argb));
            }
            MutableComponent chance = Component.translatable("af9.jade.break",
                    LithoMode.formatPercent(tag.getDouble("break"))).withStyle(ChatFormatting.GRAY);
            if (tag.getInt("version") > 0) {
                chance.append(Component.translatable("af9.jade.version", tag.getInt("version"))
                        .withStyle(ChatFormatting.DARK_GRAY));
            }
            tooltip.add(chance);
            // air conditioning against the print's heat, the OPC the computation gave, the calibration
            int coolLoad = tag.getInt("coolLoad");
            if (coolLoad > 0) {
                int coolCap = tag.getInt("coolCap");
                int coolSteps = tag.getInt("coolSteps");
                tooltip.add(tag.getBoolean("coolLapsed") ?
                        Component.translatable("af9.jade.cooling_lapsed").withStyle(ChatFormatting.RED) :
                        Component.translatable("af9.jade.cooling", coolCap, coolLoad,
                                coolSteps > 0 ? " +" + coolSteps : "")
                                .withStyle(coolCap < coolLoad ? ChatFormatting.RED : ChatFormatting.GRAY));
            }
            double opc = tag.getDouble("opc");
            double cal = tag.getDouble("cal");
            tooltip.add(Component.translatable("af9.jade.tuning",
                    opc >= 0 ? Math.round(opc * 100) + "%" : "-", Math.round(cal) + "%")
                    .withStyle(cal < LithoMode.CALIBRATION_MIN ? ChatFormatting.RED :
                            cal < LithoMode.AUTO_CALIBRATION_BELOW ? ChatFormatting.YELLOW : ChatFormatting.GRAY));
        } else {
            tooltip.add(statusLine(status, Component.translatable(tag.getString("modeKey"))
                    .withStyle(ChatFormatting.AQUA)));
            if (running) {
                Component product = productName(tag.getString("product"));
                if (product != null) {
                    tooltip.add(Component.translatable("af9.jade.making", product).withStyle(ChatFormatting.GRAY));
                }
                tooltip.add(runtime(helper, progress, duration, 0xFF4CBB17));
            }
            ListTag lines = tag.getList("lines", Tag.TAG_STRING);
            for (int i = 0; i < lines.size(); i++) {
                Component line = Component.Serializer.fromJson(lines.getString(i));
                if (line != null) tooltip.add(line.copy().withStyle(ChatFormatting.GRAY));
            }
        }
    }

    private static String startupKey(int state) {
        return switch (state) {
            case LithoMachine.VACUUM_PUMPING -> "af9.jade.starting";
            case LithoMachine.VACUUM_SEALED -> "af9.jade.ready";
            default -> "af9.jade.shut_down";
        };
    }

    private static String vacuumKey(int state) {
        return switch (state) {
            case LithoMachine.VACUUM_PUMPING -> "af9.jade.pumping";
            case LithoMachine.VACUUM_SEALED -> "af9.jade.sealed";
            case LithoMachine.VACUUM_VENTING -> "af9.jade.venting";
            default -> "af9.jade.off";
        };
    }

    private static Component statusLine(int status, Component mode) {
        int color = ConsoleWidget.statusColor(status);
        return Component.translatable("af9.console.status." + status).withStyle(style -> style.withColor(color))
                .append(Component.literal(" - ").withStyle(ChatFormatting.DARK_GRAY))
                .append(mode);
    }

    private static snownee.jade.api.ui.IElement runtime(IElementHelper helper, int progress, int duration,
                                                         int color) {
        float fraction = duration <= 0 ? 0 : Math.min(1F, (float) progress / duration);
        Component text = Component.translatable("af9.jade.runtime", ConsoleWidget.seconds(progress),
                ConsoleWidget.seconds(duration));
        return helper.progress(fraction, text, helper.progressStyle().color(color).textColor(-1), box(), true);
    }

    private static BoxStyle box() {
        BoxStyle style = new BoxStyle();
        style.borderColor = BORDER;
        return style;
    }

    /** "item:&lt;id&gt;" / "fluid:&lt;id&gt;" to a display name, or null. */
    private static Component productName(String product) {
        int split = product.indexOf(':');
        if (split < 0) return null;
        ResourceLocation id = ResourceLocation.tryParse(product.substring(split + 1));
        if (id == null) return null;
        if (product.startsWith("item:")) {
            Item item = ForgeRegistries.ITEMS.getValue(id);
            return item == null || item == Items.AIR ? null : item.getDescription();
        }
        Fluid fluid = ForgeRegistries.FLUIDS.getValue(id);
        return fluid == null || fluid == Fluids.EMPTY ? null : fluid.getFluidType().getDescription();
    }
}
