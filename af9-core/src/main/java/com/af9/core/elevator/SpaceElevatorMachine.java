package com.af9.core.elevator;

import com.af9.core.common.IPowerGated;

import com.gregtechceu.gtceu.api.capability.recipe.ItemRecipeCapability;
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.gregtechceu.gtceu.api.machine.multiblock.MultiblockDisplayText;
import com.gregtechceu.gtceu.api.machine.multiblock.WorkableElectricMultiblockMachine;
import com.gregtechceu.gtceu.api.recipe.GTRecipe;
import com.gregtechceu.gtceu.api.recipe.RecipeHelper;
import com.gregtechceu.gtceu.api.recipe.chance.logic.ChanceLogic;
import com.gregtechceu.gtceu.api.recipe.content.Content;
import com.gregtechceu.gtceu.api.recipe.ingredient.SizedIngredient;
import com.gregtechceu.gtceu.api.recipe.modifier.ModifierFunction;
import com.gregtechceu.gtceu.api.recipe.modifier.RecipeModifier;

import com.lowdragmc.lowdraglib.syncdata.field.ManagedFieldHolder;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.List;

/**
 * Space Elevator (after GTNH's, docs/space-elevator.md): a tower on a cable that reaches into space. A Mining Drone (not used
 * up) in an input bus, hydrogen and a supercooled coolant in the fluid hatches and a very great deal of energy send an
 * expedition to a random asteroid; when it is back the output buses hold its ore, tens of stacks of raw ore.
 * <p>
 * The asteroid is made from one of GT's ore veins (weighted by the vein's weight, among the veins of the drone's tier and
 * below; the best drone also finds the exotic asteroid, whose ores no vein holds): about half the stacks are the vein's
 * main ore, the rest are shared by its other ores. Re-rolled for every run ({@link #ASTEROID}); the recipes themselves
 * (KubeJS: {@code server_scripts/mods/gtceu/space_elevator.js}) only name the drone, the fluids and the energy.
 * <p>
 * While the structure is formed the platform on the cable turns slowly
 * ({@link com.af9.core.client.render.SpaceElevatorRender}).
 */
public class SpaceElevatorMachine extends WorkableElectricMultiblockMachine implements ISpaceElevatorMachine,
        IPowerGated {

    protected static final ManagedFieldHolder MANAGED_FIELD_HOLDER = new ManagedFieldHolder(
            SpaceElevatorMachine.class, WorkableElectricMultiblockMachine.MANAGED_FIELD_HOLDER);

    /** Stacks of ore an expedition of each drone tier brings home: at least and at most. */
    private static final int[] MIN_STACKS = { 8, 12, 16, 24 };
    private static final int[] MAX_STACKS = { 16, 24, 32, 48 };
    private static final int STACK = 64;
    /** The share of the stacks of the vein's main ore. */
    private static final double MAIN_SHARE = 0.5;
    /** How many of the exotic ores one exotic asteroid holds, and its share of the Mk4 draws (1 in this many). */
    private static final int EXOTIC_ORES = 3;
    private static final int EXOTIC_ONE_IN = 6;
    private static final String DRONE = "space_mining_drone_mk";

    /** Where the last expedition went (the screen shows it). */
    private String lastAsteroid = "";

    public SpaceElevatorMachine(IMachineBlockEntity holder) {
        super(holder);
    }

    @Override
    public ManagedFieldHolder getFieldHolder() {
        return MANAGED_FIELD_HOLDER;
    }

    /** Re-modify every run: each expedition goes to a new asteroid. */
    @Override
    public boolean alwaysTryModifyRecipe() {
        return true;
    }

    @Override
    public long getAvailableEUt() {
        return energyContainer == null || !isFormed() ? 0 :
                energyContainer.getInputVoltage() * energyContainer.getInputAmperage();
    }

    @Override
    public boolean isElevatorFormed() {
        return isFormed();
    }

    @Override
    public boolean isElevatorWorking() {
        return isFormed() && getRecipeLogic().isWorking();
    }

    //////////////////////////////////////
    // ********** Asteroids ***********//
    //////////////////////////////////////

    /**
     * Only starts a run the hatches can supply with its full EU/t, and gives it its asteroid: the ore the run puts out
     * (the recipe's own outputs are dropped).
     */
    public static final RecipeModifier ASTEROID = (machine, recipe) -> {
        if (!(machine instanceof SpaceElevatorMachine elevator)) {
            return RecipeModifier.nullWrongType(SpaceElevatorMachine.class, machine);
        }
        if (elevator.getAvailableEUt() < RecipeHelper.getRealEUt(recipe).getTotalEU()) return ModifierFunction.NULL;
        int tier = droneTier(recipe);
        if (tier < 1) return ModifierFunction.NULL;
        RandomSource random = elevator.getLevel() != null ? elevator.getLevel().getRandom() : RandomSource.create();
        StringBuilder where = new StringBuilder();
        List<ItemStack> ores = asteroid(tier, random, where);
        if (ores.isEmpty()) return ModifierFunction.NULL;
        elevator.lastAsteroid = where.toString();
        return modified -> {
            GTRecipe result = modified.copy();
            List<Content> outputs = new ArrayList<>();
            for (ItemStack stack : ores) {
                outputs.add(new Content(SizedIngredient.create(stack), ChanceLogic.getMaxChancedValue(),
                        ChanceLogic.getMaxChancedValue(), 0));
            }
            result.outputs.put(ItemRecipeCapability.CAP, outputs);
            return result;
        };
    };

    /** The tier of the Mining Drone a recipe takes ({@code space_mining_drone_mk<n>}), 0 if there is none. */
    private static int droneTier(GTRecipe recipe) {
        for (Content content : recipe.inputs.getOrDefault(ItemRecipeCapability.CAP, List.of())) {
            if (!(content.content instanceof Ingredient ingredient)) continue;
            for (ItemStack stack : ingredient.getItems()) {
                ResourceLocation key = ForgeRegistries.ITEMS.getKey(stack.getItem());
                if (key != null && key.getPath().startsWith(DRONE)) {
                    try {
                        return Integer.parseInt(key.getPath().substring(DRONE.length()));
                    } catch (NumberFormatException exception) {
                        return 0;
                    }
                }
            }
        }
        return 0;
    }

    /** The ore of one expedition of a drone tier, as stacks (up to 64 each); {@code name} gets where it went. */
    static List<ItemStack> asteroid(int tier, RandomSource random, StringBuilder name) {
        int index = Math.min(tier, MIN_STACKS.length) - 1;
        int stacks = MIN_STACKS[index] + random.nextInt(MAX_STACKS[index] - MIN_STACKS[index] + 1);
        List<String> materials = new ArrayList<>();
        List<OreCatalog.Vein> pool = new ArrayList<>();
        int weight = 0;
        for (OreCatalog.Vein vein : OreCatalog.veins()) {
            if (vein.tier() <= tier) {
                pool.add(vein);
                weight += vein.weight();
            }
        }
        List<String> exotics = OreCatalog.exotics();
        boolean exotic = !exotics.isEmpty() &&
                (pool.isEmpty() || tier >= OreCatalog.TIERS && random.nextInt(EXOTIC_ONE_IN) == 0);
        if (exotic) {
            name.append("exotic");
            List<String> bag = new ArrayList<>(exotics);
            while (materials.size() < EXOTIC_ORES && !bag.isEmpty()) {
                materials.add(bag.remove(random.nextInt(bag.size())));
            }
        } else if (!pool.isEmpty()) {
            int roll = random.nextInt(weight);
            OreCatalog.Vein picked = pool.get(0);
            for (OreCatalog.Vein vein : pool) {
                if (roll < vein.weight()) {
                    picked = vein;
                    break;
                }
                roll -= vein.weight();
            }
            name.append(picked.id());
            materials.addAll(picked.materials());
        }
        List<ItemStack> result = new ArrayList<>();
        int main = materials.size() <= 1 ? stacks : Math.max(1, (int) Math.round(stacks * MAIN_SHARE));
        int others = Math.max(1, materials.size() - 1);
        for (int i = 0; i < materials.size(); i++) {
            int share = i == 0 ? main : (stacks - main) / others;
            if (i == 1) share += (stacks - main) - share * others;
            addStacks(result, materials.get(i), share * STACK);
        }
        return result;
    }

    /** {@code count} items of an ore as stacks of at most 64; nothing when GT has no item of the ore. */
    private static void addStacks(List<ItemStack> into, String material, int count) {
        int left = count;
        while (left > 0) {
            ItemStack stack = OreCatalog.ore(material, Math.min(STACK, left));
            if (stack == null) return;
            into.add(stack);
            left -= stack.getCount();
        }
    }

    //////////////////////////////////////
    // ************ Screen ************//
    //////////////////////////////////////

    @Override
    public void addDisplayText(List<Component> text) {
        MultiblockDisplayText.builder(text, isFormed())
                .setWorkingStatus(recipeLogic.isWorkingEnabled(), recipeLogic.isActive())
                .addEnergyUsageLine(energyContainer)
                .addWorkingStatusLine();
        if (!isFormed()) return;
        if (!lastAsteroid.isEmpty()) {
            text.add(Component.translatable("af9.space_elevator.asteroid", lastAsteroid).withStyle(ChatFormatting.AQUA));
        }
        text.add(Component.translatable("af9.space_elevator.power", getAvailableEUt()).withStyle(ChatFormatting.GRAY));
    }
}
