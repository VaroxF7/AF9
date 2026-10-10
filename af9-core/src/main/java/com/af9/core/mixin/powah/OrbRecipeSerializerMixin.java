package com.af9.core.mixin.powah;

import com.af9.core.compat.powah.OrbCounts;
import com.af9.core.compat.powah.OrbRecipes;
import com.google.gson.JsonObject;

import net.minecraft.core.NonNullList;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import owmii.powah.block.energizing.EnergizingRecipe;

/**
 * Reads the counts of an Energizing Orb recipe (from the data pack's JSON) and sends them with it to the clients, where the
 * ingredients also get their counts, so JEI and EMI show "4x". The injections are required: a recipe whose counts reach the
 * server but not the client (or the other way round) would break the packet.
 */
@Pseudo
@Mixin(targets = "owmii.powah.block.energizing.EnergizingRecipe$Serializer", remap = false)
public abstract class OrbRecipeSerializerMixin {

    @Inject(method = "fromJson(Lnet/minecraft/resources/ResourceLocation;Lcom/google/gson/JsonObject;)" +
            "Lowmii/powah/block/energizing/EnergizingRecipe;", at = @At("RETURN"), remap = false)
    private void af9$readCounts(ResourceLocation id, JsonObject json, CallbackInfoReturnable<EnergizingRecipe> cir) {
        EnergizingRecipe recipe = cir.getReturnValue();
        if (recipe == null) return;
        ((OrbCounts) (Object) recipe).af9SetCounts(OrbRecipes.readCounts(GsonHelper.getAsJsonArray(json, "ingredients")));
        ((OrbCounts) (Object) recipe).af9SetNc(OrbRecipes.readNc(GsonHelper.getAsJsonArray(json, "ingredients")));
    }

    @Inject(method = "toNetwork(Lnet/minecraft/network/FriendlyByteBuf;" +
            "Lowmii/powah/block/energizing/EnergizingRecipe;)V", at = @At("RETURN"), remap = false)
    private void af9$writeCounts(FriendlyByteBuf buffer, EnergizingRecipe recipe, CallbackInfo ci) {
        int[] counts = ((OrbCounts) (Object) recipe).af9Counts();
        buffer.writeVarInt(counts.length);
        for (int count : counts) buffer.writeVarInt(count);
        boolean[] nc = ((OrbCounts) (Object) recipe).af9Nc();
        buffer.writeVarInt(nc.length);
        for (boolean keep : nc) buffer.writeBoolean(keep);
    }

    @Inject(method = "fromNetwork(Lnet/minecraft/resources/ResourceLocation;Lnet/minecraft/network/FriendlyByteBuf;)" +
            "Lowmii/powah/block/energizing/EnergizingRecipe;", at = @At("RETURN"), remap = false)
    private void af9$readNetworkCounts(ResourceLocation id, FriendlyByteBuf buffer,
                                       CallbackInfoReturnable<EnergizingRecipe> cir) {
        int[] counts = new int[buffer.readVarInt()];
        for (int i = 0; i < counts.length; i++) counts[i] = buffer.readVarInt();
        boolean[] nc = new boolean[buffer.readVarInt()];
        for (int i = 0; i < nc.length; i++) nc[i] = buffer.readBoolean();
        EnergizingRecipe recipe = cir.getReturnValue();
        if (recipe == null) return;
        ((OrbCounts) (Object) recipe).af9SetCounts(counts);
        ((OrbCounts) (Object) recipe).af9SetNc(nc);
        // the client's ingredients are plain item lists by now (the server resolved tags): they take the counts for display
        NonNullList<Ingredient> ingredients = ((Recipe<?>) (Object) recipe).getIngredients();
        for (int i = 0; i < ingredients.size() && i < counts.length; i++) {
            ingredients.set(i, OrbRecipes.withCount(ingredients.get(i), counts[i]));
        }
    }
}
