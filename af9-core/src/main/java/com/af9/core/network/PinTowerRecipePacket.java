package com.af9.core.network;

import com.af9.core.distillation.DistillationModuleMachine;

import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.gregtechceu.gtceu.api.recipe.GTRecipe;

import com.lowdragmc.lowdraglib.gui.modular.ModularUIContainer;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Client to server: the recipe viewer's "+" on a recipe, with a Distillation Tower Module's screen open: the tower is set to that
 * recipe ({@link DistillationModuleMachine#pinTo}): its mode and its recipe lock.
 */
public final class PinTowerRecipePacket {

    private final ResourceLocation recipe;

    public PinTowerRecipePacket(ResourceLocation recipe) {
        this.recipe = recipe;
    }

    public static void encode(PinTowerRecipePacket packet, FriendlyByteBuf buffer) {
        buffer.writeResourceLocation(packet.recipe);
    }

    public static PinTowerRecipePacket decode(FriendlyByteBuf buffer) {
        return new PinTowerRecipePacket(buffer.readResourceLocation());
    }

    public static void handle(PinTowerRecipePacket packet, Supplier<NetworkEvent.Context> context) {
        NetworkEvent.Context ctx = context.get();
        ServerPlayer player = ctx.getSender();
        if (player != null && player.containerMenu instanceof ModularUIContainer menu &&
                menu.getModularUI().holder instanceof IMachineBlockEntity holder &&
                holder.getMetaMachine() instanceof DistillationModuleMachine tower &&
                player.level().getRecipeManager().byKey(packet.recipe).orElse(null) instanceof GTRecipe recipe) {
            boolean set = tower.pinTo(recipe);
            player.displayClientMessage(Component.translatable(set ? "af9.distillation.module.pinned" :
                    "af9.distillation.module.pin_failed", packet.recipe.toString()), true);
        }
        ctx.setPacketHandled(true);
    }
}
