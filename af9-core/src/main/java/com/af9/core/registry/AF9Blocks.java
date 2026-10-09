package com.af9.core.registry;

import com.af9.core.AF9Core;

import com.gregtechceu.gtceu.api.GTCEuAPI;
import com.gregtechceu.gtceu.api.block.SimpleCoilType;
import com.gregtechceu.gtceu.common.block.CoilBlock;
import com.gregtechceu.gtceu.common.data.GTMaterials;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.function.Supplier;

/**
 * AF9's plain blocks: the casings and parts of its multiblocks' structures, and the Endion coils. They were KubeJS
 * blocks ({@code kubejs:<id>}) until AF9 Core took the pack's registrations over; a world from before is remapped
 * ({@link AF9Remaps}). All of them are metal or stone of strength 5, mined with a pickaxe, and drop themselves.
 * <p>
 * To add one: a line here, its name ({@code block.af9.<id>}) and tooltip lines ({@code block.af9.<id>.tooltip.<n>})
 * in the lang file, a block state, a block model and an item model ({@code assets/af9}), its texture (a
 * {@code _ctm} texture and an {@code .mcmeta} beside it for connected textures), a loot table
 * ({@code data/af9/loot_tables/blocks}) and a line in {@code data/minecraft/tags/blocks/mineable/pickaxe.json}; then
 * the dev run, which writes the linters' list of what is registered ({@code tools/lint/README.md}). A block with
 * behaviour of its own is registered where that is (Oil Regolith: {@link com.af9.core.space.AF9Space}). The blocks a
 * machine's logic names have a constant here.
 */
@SuppressWarnings("removal") // new ResourceLocation(ns, path) is the only constructor on 1.20.1
public final class AF9Blocks {

    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS,
            AF9Core.MOD_ID);
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, AF9Core.MOD_ID);

    /** The motor tiers and the Mining Module tiers of the Space Elevator. */
    public static final int MOTORS = 5, MODULES = 3;
    /** The ids of those blocks: this and the tier ({@code af9:space_elevator_motor_mk3}). */
    public static final String MOTOR = "space_elevator_motor_mk", MODULE = "space_mining_module_mk";

    // ---- Photolithography: the light sources of the line's versions (the mercury lamp of version 1 is the purple
    // lamp): the KrF excimer laser allows version 2, the ArF excimer laser up to 6, the EUV source up to 8
    // (PhotolithographyLineMachine) ----
    public static final RegistryObject<Block> KRF_EXCIMER_LASER = block("krf_excimer_laser", SoundType.METAL, 6F, 0, 0);
    public static final RegistryObject<Block> ARF_EXCIMER_LASER = block("arf_excimer_laser", SoundType.METAL, 6F, 0, 0);
    /** Laser-produced plasma: CO2 laser pulses hit tin droplets, a multilayer collector mirror gathers the light. */
    public static final RegistryObject<Block> EUV_LIGHT_SOURCE = block("euv_light_source", SoundType.METAL, 6F, 9, 0);
    /**
     * A fan filter unit in a plascrete frame, from MV parts: the lithography machines' ceiling. A GT cleanroom filter
     * as well ({@link com.af9.core.pattern.AF9Filters}: ISO 5, like GT's Filter Casing).
     */
    public static final RegistryObject<Block> PLASCRETE_FILTER_CASING = block("plascrete_filter_casing",
            SoundType.METAL, 6F, 0, 0);
    /** The cable on top of the Space Elevator's shaft. */
    public static final RegistryObject<Block> SPACE_ELEVATOR_CABLE = block("space_elevator_cable", SoundType.METAL,
            12F, 7, 1);

    static {
        // ---- The Endion coils: usable wherever GT takes heating coils; Boule Melting gets its bonus from them
        // (BouleMelting reads the coil by these ids). Temperature, level, energy discount, tier ----
        coil("endion_coil_block", 8100, 8, 6, 5, "block/coils/endion_coil");
        coil("resonant_endion_coil_block", 12600, 16, 16, 8, "block/coils/resonant_endion_coil");

        // ---- Photolithography: the chemical lines of the MV machines (GT's PTFE Pipe Casing needs PTFE, which only
        // comes at HV) ----
        block("plascrete_pipe_casing", SoundType.METAL, 6F, 0, 0);

        // ---- Space Elevator: the blocks of the tower (GTNH's), the motors round the cable (all 88 of a tower are of
        // one tier, the elevator's), the Mining Modules in the module slots ----
        block("space_elevator_base_casing", SoundType.METAL, 12F, 0, 0);
        block("space_elevator_internal_structure", SoundType.METAL, 12F, 0, 0);
        block("ultra_high_strength_concrete_floor", SoundType.STONE, 12F, 0, 0);
        // ---- Void Miner MK2 / MK3: GTNH's mining casings, item pipe casing and bolted casings of their structures ----
        block("mining_black_plutonium_casing", SoundType.METAL, 12F, 0, 0);
        block("black_plutonium_item_pipe_casing", SoundType.METAL, 12F, 0, 0);
        block("bolted_naquadah_alloy_casing", SoundType.METAL, 12F, 0, 0);
        block("rebolted_naquadah_alloy_casing", SoundType.METAL, 12F, 0, 0);
        block("mining_neutronium_casing", SoundType.METAL, 12F, 0, 0);
        block("bolted_iridium_casing", SoundType.METAL, 12F, 0, 0);
        block("rebolted_iridium_casing", SoundType.METAL, 12F, 0, 0);

        // ---- Dyson Swarm: GTNH Intergalactic's receiver, deployment unit and command centre casings (the floor is the
        // Space Elevator's ultra high strength concrete) ----
        block("dyson_receiver_casing", SoundType.METAL, 12F, 0, 0);
        block("dyson_receiver_dish", SoundType.METAL, 12F, 0, 0);
        block("dyson_deployment_casing", SoundType.METAL, 12F, 0, 0);
        block("dyson_deployment_core", SoundType.METAL, 12F, 0, 0);
        block("dyson_deployment_magnet", SoundType.METAL, 12F, 0, 0);
        block("dyson_control_casing", SoundType.METAL, 12F, 0, 0);
        block("dyson_control_primary", SoundType.METAL, 12F, 0, 0);
        block("dyson_control_secondary", SoundType.METAL, 12F, 0, 0);
        block("dyson_control_toroid", SoundType.METAL, 12F, 0, 0);

        // ---- Hyper-Intensity Laser Engraver: GTNH's Laser Containment Casing and the plate the beam lands on ----
        block("laser_containment_casing", SoundType.METAL, 12F, 0, 0);
        block("laser_resistant_plate", SoundType.METAL, 12F, 0, 0);

        for (int tier = 1; tier <= MOTORS; tier++) block(MOTOR + tier, SoundType.METAL, 12F, 0, 2);
        for (int tier = 1; tier <= MODULES; tier++) block(MODULE + tier, SoundType.METAL, 12F, 0, 2);
    }

    private AF9Blocks() {}

    private static BlockBehaviour.Properties properties(SoundType sound, float resistance, int light) {
        return BlockBehaviour.Properties.of().strength(5F, resistance).sound(sound).requiresCorrectToolForDrops()
                .lightLevel(state -> light);
    }

    /** A block and its item; the item's tooltip has so many lines. */
    private static RegistryObject<Block> block(String id, SoundType sound, float resistance, int light,
                                               int tooltipLines) {
        RegistryObject<Block> block = BLOCKS.register(id, () -> new Block(properties(sound, resistance, light)));
        withItem(id, block, tooltipLines);
        return block;
    }

    /**
     * A heating coil: GT's coil block with a coil type of its own, known to GT's coil predicate. The texture is the
     * coil's ({@code <texture>_bloom} glows on the working one); its wire is endionite.
     */
    private static void coil(String id, int temperature, int level, int energyDiscount, int tier, String texture) {
        SimpleCoilType type = new SimpleCoilType(id, temperature, level, energyDiscount, tier,
                () -> GTMaterials.get("endionite"), new ResourceLocation(AF9Core.MOD_ID, texture));
        RegistryObject<CoilBlock> coil = BLOCKS.register(id,
                () -> new CoilBlock(properties(SoundType.METAL, 6F, 0), type));
        Supplier<CoilBlock> block = coil;
        GTCEuAPI.HEATING_COILS.put(type, block);
        withItem(id, coil, 0);
    }

    private static void withItem(String id, RegistryObject<? extends Block> block, int tooltipLines) {
        ITEMS.register(id, () -> new TooltipBlockItem(block.get(), new Item.Properties(), tooltipLines));
    }

    public static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
    }
}
