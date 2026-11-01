package com.af9.core.startup;

import com.af9.core.AF9Core;

import com.gregtechceu.gtceu.api.blockentity.IMachineBlockEntity;
import com.gregtechceu.gtceu.api.machine.MachineDefinition;
import com.gregtechceu.gtceu.api.machine.MetaMachine;
import com.gregtechceu.gtceu.api.machine.MultiblockMachineDefinition;
import com.gregtechceu.gtceu.api.machine.multiblock.MultiblockControllerMachine;
import com.gregtechceu.gtceu.api.pattern.TraceabilityPredicate;
import com.gregtechceu.gtceu.api.pattern.Predicates;
import com.gregtechceu.gtceu.api.recipe.GTRecipeType;
import com.gregtechceu.gtceu.api.registry.registrate.MachineBuilder;
import com.gregtechceu.gtceu.api.registry.registrate.MultiblockMachineBuilder;
import com.gregtechceu.gtceu.common.data.GTRecipeTypes;
import com.gregtechceu.gtceu.common.data.models.GTMachineModels;
import com.gregtechceu.gtceu.common.registry.GTRegistration;
import com.gregtechceu.gtceu.data.pack.GTDynamicResourcePack;
import com.gregtechceu.gtceu.data.pack.event.RegisterDynamicResourcesEvent;
import com.gregtechceu.gtceu.utils.data.RuntimeBlockstateProvider;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.registries.ForgeRegistries;

import com.google.gson.JsonObject;
import com.tterrag.registrate.providers.DataGenContext;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.TreeMap;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Registers AF9's GregTech machines in Java. They used to be KubeJS startup scripts
 * ({@code GTCEuStartupEvents.registry('gtceu:machine')}); GregTech's KubeJS plugin registered them exactly as below:
 * through GregTech's own registrate (so the ids stay {@code gtceu:<name>}, tiered ones {@code gtceu:<tier>_<name>}) from
 * GregTech's machine registry event, and then wrote their models and names at runtime, which this class does too.
 *
 * <p>Registrations are in {@code com.af9.core.startup.machines.*}, one class per topic. Keep their calls in the plain
 * form {@code multiblock("id", Machine::new, b -> b.langValue(...)...)} or {@code tiered("id", tiers, factory, (tier, b) ->
 * ...)}: tools/lint/scripts.js reads them from the source (the pattern, the recipe types, the tooltips, the tiers).
 */
public final class AF9Machines {

    private AF9Machines() {}

    /** Every builder registered here, for the runtime models. */
    private static final List<MachineBuilder<?, ?>> BUILDERS = new ArrayList<>();
    private static final List<MachineDefinition> DEFINITIONS = new ArrayList<>();

    // ---- registration --------------------------------------------------------------------------------------------

    /** A multiblock controller: {@code gtceu:<id>}. */
    public static MultiblockMachineDefinition multiblock(String id,
                                                         Function<IMachineBlockEntity, ? extends MultiblockControllerMachine> machine,
                                                         Consumer<MultiblockMachineBuilder<MultiblockMachineDefinition, ?>> config) {
        MultiblockMachineBuilder<MultiblockMachineDefinition, ?> builder = GTRegistration.REGISTRATE.multiblock(id,
                machine);
        config.accept(builder);
        MultiblockMachineDefinition definition = builder.register();
        BUILDERS.add(builder);
        DEFINITIONS.add(definition);
        return definition;
    }

    /** A tiered single block machine: {@code gtceu:<tier>_<id>} for every tier of the list ({@code GTValues} tier numbers). */
    public static void tiered(String id, int[] tiers, BiFunction<IMachineBlockEntity, Integer, ? extends MetaMachine> machine,
                              BiConsumer<Integer, MachineBuilder<MachineDefinition, ?>> definition) {
        for (int tier : tiers) {
            String tierName = com.gregtechceu.gtceu.api.GTValues.VN[tier].toLowerCase(Locale.ROOT);
            MachineBuilder<MachineDefinition, ?> builder = GTRegistration.REGISTRATE.machine(tierName + "_" + id,
                    holder -> machine.apply(holder, tier));
            // GregTech's defaults for a custom tiered machine of the scripts, overridden by the definition
            builder.langValue("%s %s %s".formatted(com.gregtechceu.gtceu.api.GTValues.VLVH[tier],
                    englishName(id), com.gregtechceu.gtceu.api.GTValues.VLVT[tier])).tier(tier);
            definition.accept(tier, builder);
            MachineDefinition def = builder.register();
            BUILDERS.add(builder);
            DEFINITIONS.add(def);
        }
    }

    private static String englishName(String path) {
        StringBuilder sb = new StringBuilder();
        for (String word : path.split("_")) {
            if (word.isEmpty()) continue;
            if (sb.length() > 0) sb.append(' ');
            sb.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1));
        }
        return sb.toString();
    }

    // ---- helpers for the definitions -----------------------------------------------------------------------------

    /** A block by id (resolved when the pattern is built, after all registries are filled). */
    public static Block block(String id) {
        Block block = ForgeRegistries.BLOCKS.getValue(new ResourceLocation(id));
        if (block == null || block == Blocks.AIR) {
            throw new IllegalStateException("AF9 machine pattern: unknown block " + id);
        }
        return block;
    }

    public static Supplier<Block> blockSupplier(String id) {
        return () -> block(id);
    }

    /** {@code Predicates.blocks('a', 'b')} of the scripts. */
    public static TraceabilityPredicate blocks(String... ids) {
        Block[] blocks = new Block[ids.length];
        for (int i = 0; i < ids.length; i++) blocks[i] = block(ids[i]);
        return Predicates.blocks(blocks);
    }

    /** GT's recipe type by name (before the machine: the types register first). */
    public static GTRecipeType type(String name) {
        return GTRecipeTypes.get(name);
    }

    /** The lines {@code <key>0 .. <key><count-1>} as translatable components (the key ends with a dot). */
    public static Component[] lines(String key, int count) {
        Component[] out = new Component[count];
        for (int i = 0; i < count; i++) out[i] = Component.translatable(key + i);
        return out;
    }

    public static ResourceLocation rl(String id) {
        return new ResourceLocation(id);
    }

    // ---- runtime assets ------------------------------------------------------------------------------------------

    /**
     * Client: the models, item models and names of the machines, as GregTech's KubeJS plugin made them (it ran at the
     * last step of GregTech's own dynamic assets, before they are written out).
     */
    public static void onDynamicResources(RegisterDynamicResourcesEvent event) {
        JsonObject lang = new JsonObject();
        TreeMap<String, String> names = new TreeMap<>();
        for (int i = 0; i < BUILDERS.size(); i++) {
            MachineBuilder<?, ?> builder = BUILDERS.get(i);
            MachineDefinition definition = DEFINITIONS.get(i);
            try {
                ResourceLocation id = definition.getId();
                if (builder.model() != null || builder.blockModel() != null) {
                    DataGenContext<Block, Block> context = new DataGenContext<>(() -> definition.getBlock(),
                            definition.getName(), id);
                    if (builder.blockModel() != null) {
                        builder.blockModel().accept(context, RuntimeBlockstateProvider.INSTANCE);
                    } else {
                        GTMachineModels.createMachineModel(builder.model()).accept(context,
                                RuntimeBlockstateProvider.INSTANCE);
                    }
                    JsonObject item = new JsonObject();
                    item.addProperty("parent", id.withPrefix("block/machine/").toString());
                    GTDynamicResourcePack.addItemModel(id, item);
                }
                if (definition.getLangValue() != null) {
                    names.put(definition.getDescriptionId(), definition.getLangValue());
                }
            } catch (RuntimeException e) {
                AF9Core.LOGGER.error("AF9 machine {}: runtime model failed", DEFINITIONS.get(i).getId(), e);
            }
        }
        names.forEach(lang::addProperty);
        GTDynamicResourcePack.addResource(new ResourceLocation("gtceu", "lang/en_us.json"), lang);
    }
}
