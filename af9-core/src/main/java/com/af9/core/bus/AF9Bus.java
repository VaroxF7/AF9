package com.af9.core.bus;

import com.af9.core.AF9Core;

import com.gregtechceu.gtceu.api.item.ComponentItem;
import com.gregtechceu.gtceu.api.pattern.Predicates;
import com.gregtechceu.gtceu.api.pattern.TraceabilityPredicate;
import com.gregtechceu.gtceu.common.machine.multiblock.electric.CentralMonitorMachine;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.MissingMappingsEvent;
import net.minecraftforge.registries.RegistryObject;

import java.lang.reflect.Field;

/**
 * The machine bus: Optical Bus Cable, the Bus Connector (a GT part, defined in KubeJS:
 * {@code startup_scripts/gtceu/machine_bus.js}, behaviour {@link BusConnectorPartMachine}), the Bus Controller
 * ({@link BusControllerMachine}, a KubeJS multiblock) and the Central Monitor's Machine Bus Module. Spec:
 * docs/machine-bus.md.
 */
public final class AF9Bus {

    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS,
            AF9Core.MOD_ID);
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, AF9Core.MOD_ID);

    public static final RegistryObject<Block> OPTICAL_BUS_CABLE = BLOCKS.register("optical_bus_cable",
            () -> new OpticalBusCableBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_CYAN)
                    .strength(0.3f)
                    .sound(SoundType.WOOL)
                    .noOcclusion()));
    public static final RegistryObject<Item> OPTICAL_BUS_CABLE_ITEM = ITEMS.register("optical_bus_cable",
            () -> new BlockItem(OPTICAL_BUS_CABLE.get(), new Item.Properties()));
    /** The bus's first cable, replaced by the optical one: laid cable and items turn into Optical Bus Cable. */
    private static final String OLD_CABLE = "polycat_cable";
    public static final RegistryObject<Item> MACHINE_BUS_MODULE = ITEMS.register("machine_bus_module", () -> {
        ComponentItem item = ComponentItem.create(new Item.Properties().stacksTo(1));
        item.attachComponents(new MachineBusModule());
        return item;
    });

    private AF9Bus() {}

    public static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        modBus.addListener(AF9Bus::fillCreativeTabs);
        MinecraftForge.EVENT_BUS.addListener(AF9Bus::remapOldCable);
    }

    private static void remapOldCable(MissingMappingsEvent event) {
        for (var mapping : event.getMappings(ForgeRegistries.Keys.BLOCKS, AF9Core.MOD_ID)) {
            if (mapping.getKey().getPath().equals(OLD_CABLE)) mapping.remap(OPTICAL_BUS_CABLE.get());
        }
        for (var mapping : event.getMappings(ForgeRegistries.Keys.ITEMS, AF9Core.MOD_ID)) {
            if (mapping.getKey().getPath().equals(OLD_CABLE)) mapping.remap(OPTICAL_BUS_CABLE_ITEM.get());
        }
    }

    private static void fillCreativeTabs(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.FUNCTIONAL_BLOCKS) event.accept(OPTICAL_BUS_CABLE_ITEM);
        if (event.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES) event.accept(MACHINE_BUS_MODULE);
    }

    /**
     * Lets a Bus Connector into the wall of GT's Central Monitor: its structure takes the blocks of one predicate,
     * kept in a private static field built on first use. The field gets that predicate plus the connector (the
     * connector's ability; KubeJS has registered it by common setup).
     */
    public static void installMonitorWall() {
        try {
            Field field = CentralMonitorMachine.class.getDeclaredField("MULTI_PREDICATE");
            field.setAccessible(true);
            TraceabilityPredicate wall = CentralMonitorMachine.getMultiPredicate()
                    .or(Predicates.abilities(BusConnectorPartMachine.BUS_CONNECTOR).setPreviewCount(0));
            field.set(null, wall);
        } catch (ReflectiveOperationException | RuntimeException e) {
            AF9Core.LOGGER.error("AF9: the Central Monitor does not take Bus Connectors (GT changed?)", e);
        }
    }
}
