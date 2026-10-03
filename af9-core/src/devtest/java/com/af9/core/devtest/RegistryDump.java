package com.af9.core.devtest;

import com.af9.core.registry.AF9Materials;

import com.gregtechceu.gtceu.api.GTCEuAPI;
import com.gregtechceu.gtceu.api.block.IMachineBlock;
import com.gregtechceu.gtceu.api.capability.recipe.RecipeCapability;
import com.gregtechceu.gtceu.api.data.chemical.material.Material;
import com.gregtechceu.gtceu.api.data.chemical.material.properties.BlastProperty;
import com.gregtechceu.gtceu.api.data.chemical.material.properties.OreProperty;
import com.gregtechceu.gtceu.api.data.chemical.material.properties.PropertyKey;
import com.gregtechceu.gtceu.api.data.chemical.material.stack.MaterialStack;
import com.gregtechceu.gtceu.api.data.worldgen.IWorldGenLayer;
import com.gregtechceu.gtceu.api.data.worldgen.WorldGeneratorUtils;
import com.gregtechceu.gtceu.api.machine.MachineDefinition;
import com.gregtechceu.gtceu.api.machine.MultiblockMachineDefinition;
import com.gregtechceu.gtceu.api.machine.multiblock.PartAbility;
import com.gregtechceu.gtceu.api.recipe.GTRecipeType;
import com.gregtechceu.gtceu.api.registry.GTRegistries;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

/**
 * Dev runs only (the {@code devtest} source set is not in the jar): once the server has started, writes down what the
 * game has registered, a line a thing, sorted, to the file named by {@code -Daf9.dump=<file>}. Two such files, one from
 * before a change and one from after, say exactly what the change did to the registries: used to move AF9's content
 * from KubeJS into AF9 Core without losing or altering any of it.
 * <p>
 * {@code -Daf9.registry=<file>} gets the short list of what AF9 Core itself registers, ids only: the linters read it
 * ({@code tools/lint/data/af9-registry.txt}; the Gradle run sets the property to that file).
 */
@Mod.EventBusSubscriber(modid = "af9", bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class RegistryDump {

    private RegistryDump() {}

    @SubscribeEvent
    public static void onServerStarted(ServerStartedEvent event) {
        String registry = System.getProperty("af9.registry");
        if (registry != null && !registry.isEmpty()) write(registry, af9Registry());
        String target = System.getProperty("af9.dump");
        if (target != null && !target.isEmpty()) write(target, everything());
    }

    /** The file gets these lines, with Unix line ends on any system (the list for the linters is in the repository). */
    private static void write(String target, List<String> lines) {
        try {
            Path file = Path.of(target);
            if (file.getParent() != null) Files.createDirectories(file.getParent());
            Files.writeString(file, String.join("\n", lines) + "\n", StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new IllegalStateException("could not write " + target, exception);
        }
    }

    /** What AF9 Core registers itself: the ids, a line each. */
    private static List<String> af9Registry() {
        TreeSet<String> lines = new TreeSet<>();
        for (ResourceLocation id : ForgeRegistries.ITEMS.getKeys()) {
            if (mine(id)) lines.add("item " + id);
        }
        for (ResourceLocation id : ForgeRegistries.BLOCKS.getKeys()) {
            // a machine's block has its model, loot and name from GregTech
            if (mine(id)) {
                lines.add("block " + id + (ForgeRegistries.BLOCKS.getValue(id) instanceof IMachineBlock ? " machine" : ""));
            }
        }
        for (ResourceLocation id : ForgeRegistries.FLUIDS.getKeys()) {
            if (mine(id)) lines.add("fluid " + id);
        }
        // the materials are GregTech's by id (gtceu:<name>); an ore has a raw ore and ore blocks
        for (Material material : AF9Materials.all()) {
            lines.add("material " + material.getResourceLocation() +
                    (material.hasProperty(PropertyKey.ORE) ? " ore" : ""));
        }
        List<String> text = new ArrayList<>();
        text.add("# What AF9 Core registers: written by its dev run, read by the linters. Not edited by hand unless the run");
        text.add("# is out of reach (it needs a pack instance); see tools/lint/README.md, \"The registry list\".");
        text.addAll(lines);
        return text;
    }

    private static boolean mine(ResourceLocation id) {
        return id.getNamespace().equals("af9");
    }

    /** Everything the game has registered, with what a change could alter. */
    private static List<String> everything() {
        TreeSet<String> lines = new TreeSet<>();
        for (ResourceLocation id : ForgeRegistries.ITEMS.getKeys()) {
            Item item = ForgeRegistries.ITEMS.getValue(id);
            ItemStack stack = new ItemStack(item);
            lines.add("item " + id + " stack=" + stack.getMaxStackSize() + " rarity=" + item.getRarity(stack) +
                    " damage=" + stack.getMaxDamage() + " tags=" + tags(item.builtInRegistryHolder().tags()
                            .map(tag -> tag.location().toString()).toList()));
        }
        for (ResourceLocation id : ForgeRegistries.BLOCKS.getKeys()) {
            Block block = ForgeRegistries.BLOCKS.getValue(id);
            BlockState state = block.defaultBlockState();
            lines.add("block " + id + " hardness=" + block.defaultDestroyTime() + " resistance=" +
                    block.getExplosionResistance() + " sound=" + soundName(state) + " tool=" +
                    state.requiresCorrectToolForDrops() + " light=" + state.getLightEmission() + " loot=" +
                    block.getLootTable() + " machine=" + (block instanceof IMachineBlock) + " tags=" +
                    tags(block.builtInRegistryHolder().tags().map(tag -> tag.location().toString()).toList()));
        }
        for (ResourceLocation id : ForgeRegistries.FLUIDS.getKeys()) lines.add("fluid " + id);
        for (ResourceLocation id : ForgeRegistries.BLOCK_ENTITY_TYPES.getKeys()) lines.add("blockentity " + id);
        for (ResourceLocation id : BuiltInRegistries.CREATIVE_MODE_TAB.keySet()) lines.add("tab " + id);
        for (Material material : GTCEuAPI.materialManager.getRegisteredMaterials()) lines.add(material(material));
        for (GTRecipeType type : GTRegistries.RECIPE_TYPES) lines.add(recipeType(type));
        for (Map.Entry<String, IWorldGenLayer> layer : WorldGeneratorUtils.WORLD_GEN_LAYERS.entrySet()) {
            List<String> levels = new ArrayList<>();
            for (ResourceLocation level : layer.getValue().getLevels()) levels.add(level.toString());
            lines.add("layer " + layer.getKey() + " levels=" + tags(levels));
        }
        for (MachineDefinition machine : GTRegistries.MACHINES) lines.add(machine(machine));
        for (Field field : PartAbility.class.getFields()) {
            if (!Modifier.isStatic(field.getModifiers()) || field.getType() != PartAbility.class) continue;
            try {
                PartAbility ability = (PartAbility) field.get(null);
                List<String> blocks = new ArrayList<>();
                for (Block block : ability.getAllBlocks()) blocks.add(String.valueOf(ForgeRegistries.BLOCKS.getKey(block)));
                lines.add("ability " + ability.getName() + " blocks=" + tags(blocks));
            } catch (IllegalAccessException exception) {
                lines.add("ability " + field.getName() + " unreadable");
            }
        }
        return new ArrayList<>(lines);
    }

    private static String tags(List<String> tags) {
        return new TreeSet<>(tags).toString();
    }

    private static String soundName(BlockState state) {
        // a sound type has no name: its break sound says which it is
        return state.getSoundType().getBreakSound().getLocation().getPath();
    }

    private static String material(Material material) {
        StringBuilder text = new StringBuilder("material ").append(material.getResourceLocation());
        List<String> keys = new ArrayList<>();
        for (Field field : PropertyKey.class.getFields()) {
            if (!Modifier.isStatic(field.getModifiers()) || field.getType() != PropertyKey.class) continue;
            try {
                if (material.hasProperty((PropertyKey<?>) field.get(null))) keys.add(field.getName());
            } catch (IllegalAccessException exception) {
                keys.add(field.getName() + "?");
            }
        }
        text.append(" props=").append(tags(keys));
        text.append(" flags=[").append(flags(material)).append(']');
        text.append(" color=").append(Integer.toHexString(material.getMaterialARGB()));
        text.append(" secondary=").append(Integer.toHexString(material.getMaterialSecondaryARGB()));
        text.append(" iconset=").append(material.getMaterialIconSet().name);
        text.append(" formula=").append(material.getChemicalFormula());
        List<String> components = new ArrayList<>();
        for (MaterialStack stack : material.getMaterialComponents()) {
            components.add(stack.amount() + "x" + stack.material().getName());
        }
        text.append(" components=").append(components);
        if (material.hasProperty(PropertyKey.BLAST)) {
            BlastProperty blast = material.getProperty(PropertyKey.BLAST);
            text.append(" blast=").append(blast.getBlastTemperature()).append('/').append(blast.getGasTier())
                    .append('/').append(blast.getEUtOverride()).append('/').append(blast.getDurationOverride())
                    .append('/').append(blast.getVacuumEUtOverride()).append('/')
                    .append(blast.getVacuumDurationOverride());
        }
        if (material.hasProperty(PropertyKey.ORE)) {
            OreProperty ore = material.getProperty(PropertyKey.ORE);
            List<String> byproducts = new ArrayList<>();
            for (Material byproduct : ore.getOreByProducts()) byproducts.add(byproduct.getName());
            text.append(" ore=").append(ore.getOreMultiplier()).append('/').append(ore.getByProductMultiplier())
                    .append('/').append(byproducts);
        }
        if (material.hasFluid()) {
            text.append(" fluid=").append(ForgeRegistries.FLUIDS.getKey(material.getFluid())).append('@')
                    .append(material.getFluid().getFluidType().getTemperature());
        }
        text.append(" radioactive=").append(material.hasProperty(PropertyKey.HAZARD));
        return text.toString();
    }

    /** The flags of a material are kept in a private field. */
    private static String flags(Material material) {
        try {
            Field field = Material.class.getDeclaredField("flags");
            field.setAccessible(true);
            String[] flags = String.valueOf(field.get(material)).split("\\s+|,");
            return String.join(",", new TreeSet<>(List.of(flags)));
        } catch (ReflectiveOperationException | RuntimeException exception) {
            return "?";
        }
    }

    private static String recipeType(GTRecipeType type) {
        StringBuilder text = new StringBuilder("recipetype ").append(type.registryName).append(" group=")
                .append(type.group);
        List<String> io = new ArrayList<>();
        for (RecipeCapability<?> capability : type.maxInputs.keySet()) {
            io.add(capability.name + "<" + type.maxInputs.getInt(capability));
        }
        for (RecipeCapability<?> capability : type.maxOutputs.keySet()) {
            io.add(capability.name + ">" + type.maxOutputs.getInt(capability));
        }
        text.append(" io=").append(tags(io));
        text.append(" sound=").append(type.getSound() == null ? "-" : String.valueOf(type.getSound().getId()));
        return text.toString();
    }

    private static String machine(MachineDefinition machine) {
        StringBuilder text = new StringBuilder("machine ").append(machine.getId()).append(" tier=")
                .append(machine.getTier());
        List<String> types = new ArrayList<>();
        if (machine.getRecipeTypes() != null) {
            for (GTRecipeType type : machine.getRecipeTypes()) types.add(String.valueOf(type.registryName));
        }
        text.append(" recipeTypes=").append(types);
        text.append(" multiblock=").append(machine instanceof MultiblockMachineDefinition);
        text.append(" lang=").append(machine.getLangValue());
        text.append(" block=").append(ForgeRegistries.BLOCKS.getKey(machine.getBlock()));
        text.append(" blockentity=").append(ForgeRegistries.BLOCK_ENTITY_TYPES.getKey(machine.getBlockEntityType()));
        return text.toString();
    }
}
