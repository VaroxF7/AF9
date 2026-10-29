package com.af9.core.distillation;

import com.gregtechceu.gtceu.api.capability.recipe.FluidRecipeCapability;
import com.gregtechceu.gtceu.api.capability.recipe.ItemRecipeCapability;
import com.gregtechceu.gtceu.api.recipe.GTRecipe;
import com.gregtechceu.gtceu.api.recipe.content.Content;
import com.gregtechceu.gtceu.api.recipe.ingredient.FluidIngredient;
import com.gregtechceu.gtceu.data.recipe.builder.GTRecipeBuilder;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraftforge.fluids.FluidStack;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.Predicate;

/**
 * The Distillation Facility's output buffer: what the towers finish goes in here first (whatever the output hatches hold at that
 * moment), and the facility moves it into the output hatches as they have room. The towers keep running while the buffer has room
 * and stop starting runs when it is full ({@link #hasRoom}), so a facility with its few output hatches (3) needs an automation
 * that empties them, and it takes a while to block.
 * <p>
 * The room is a soft limit: a run that started with room may finish into a buffer that is full by then.
 */
final class DistillationOutputBuffer {

    /** All fluids together, in mB. */
    static final int FLUID_CAPACITY = 256_000;
    /** Different item stacks (the same item with the same tags is one stack, of any size). */
    static final int ITEM_CAPACITY = 64;
    /** Most tries to put something into the hatches in one go (each is a hatch simulation). */
    private static final int MAX_TRIES = 32;

    private final List<ItemStack> items = new ArrayList<>();
    private final List<FluidStack> fluids = new ArrayList<>();

    boolean hasRoom() {
        return fluidTotal() < FLUID_CAPACITY && items.size() < ITEM_CAPACITY;
    }

    boolean isEmpty() {
        return items.isEmpty() && fluids.isEmpty();
    }

    int fluidTotal() {
        int total = 0;
        for (FluidStack fluid : fluids) total += fluid.getAmount();
        return total;
    }

    int itemStacks() {
        return items.size();
    }

    /** Takes the item and fluid outputs of a finished run (a chanced output is rolled). */
    void addOutputs(GTRecipe recipe, RandomSource random) {
        for (Content content : recipe.outputs.getOrDefault(FluidRecipeCapability.CAP, List.of())) {
            if (skipped(content, random)) continue;
            FluidIngredient ingredient = FluidRecipeCapability.CAP.of(content.getContent());
            FluidStack[] stacks = ingredient.getStacks();
            if (stacks.length > 0 && !stacks[0].isEmpty()) add(stacks[0].copy());
        }
        for (Content content : recipe.outputs.getOrDefault(ItemRecipeCapability.CAP, List.of())) {
            if (skipped(content, random)) continue;
            Ingredient ingredient = ItemRecipeCapability.CAP.of(content.getContent());
            ItemStack[] stacks = ingredient.getItems();
            if (stacks.length > 0 && !stacks[0].isEmpty()) add(stacks[0].copy());
        }
    }

    private static boolean skipped(Content content, RandomSource random) {
        return content.isChanced() && content.maxChance > 0 && random.nextInt(content.maxChance) >= content.chance;
    }

    private void add(FluidStack stack) {
        for (FluidStack fluid : fluids) {
            if (fluid.isFluidEqual(stack)) {
                fluid.grow(stack.getAmount());
                return;
            }
        }
        fluids.add(stack);
    }

    private void add(ItemStack stack) {
        for (ItemStack item : items) {
            if (ItemStack.isSameItemSameTags(item, stack)) {
                item.grow(stack.getCount());
                return;
            }
        }
        items.add(stack);
    }

    /**
     * Moves what fits into the output hatches; {@code insert} puts a one-output recipe into them and says whether it all went in.
     * True if anything left the buffer.
     */
    boolean flush(Predicate<GTRecipe> insert) {
        boolean moved = false;
        int tries = 0;
        for (Iterator<ItemStack> it = items.iterator(); it.hasNext() && tries < MAX_TRIES;) {
            ItemStack stack = it.next();
            while (stack.getCount() > 0 && tries < MAX_TRIES) {
                tries++;
                ItemStack part = stack.copyWithCount(Math.min(stack.getCount(), stack.getMaxStackSize()));
                if (!insert.test(GTRecipeBuilder.ofRaw().outputItems(part).buildRawRecipe())) break;
                stack.shrink(part.getCount());
                moved = true;
            }
            if (stack.isEmpty()) it.remove();
        }
        for (Iterator<FluidStack> it = fluids.iterator(); it.hasNext() && tries < MAX_TRIES;) {
            FluidStack fluid = it.next();
            int chunk = fluid.getAmount();
            while (fluid.getAmount() > 0 && tries < MAX_TRIES) {
                tries++;
                int send = Math.min(fluid.getAmount(), chunk);
                FluidStack part = new FluidStack(fluid, send);
                if (insert.test(GTRecipeBuilder.ofRaw().outputFluids(part).buildRawRecipe())) {
                    fluid.shrink(send);
                    moved = true;
                } else if (chunk > 1000) {
                    chunk = Math.max(1000, chunk / 2);
                } else {
                    break;
                }
            }
            if (fluid.isEmpty()) it.remove();
        }
        return moved;
    }

    CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        ListTag itemList = new ListTag();
        for (ItemStack stack : items) {
            // (an item stack's own save keeps the count in a byte, the buffer's stacks can be bigger)
            CompoundTag entry = stack.copyWithCount(1).save(new CompoundTag());
            entry.putInt("AF9Count", stack.getCount());
            itemList.add(entry);
        }
        tag.put("items", itemList);
        ListTag fluidList = new ListTag();
        for (FluidStack fluid : fluids) fluidList.add(fluid.writeToNBT(new CompoundTag()));
        tag.put("fluids", fluidList);
        return tag;
    }

    void load(CompoundTag tag) {
        items.clear();
        fluids.clear();
        for (Tag entry : tag.getList("items", Tag.TAG_COMPOUND)) {
            CompoundTag compound = (CompoundTag) entry;
            ItemStack stack = ItemStack.of(compound);
            if (stack.isEmpty()) continue;
            stack.setCount(compound.getInt("AF9Count"));
            if (!stack.isEmpty()) items.add(stack);
        }
        for (Tag entry : tag.getList("fluids", Tag.TAG_COMPOUND)) {
            FluidStack fluid = FluidStack.loadFluidStackFromNBT((CompoundTag) entry);
            if (!fluid.isEmpty()) fluids.add(fluid);
        }
    }
}
