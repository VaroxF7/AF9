package com.af9.core.starter;

import com.af9.core.AF9Config;
import com.af9.core.AF9Core;

import com.gregtechceu.gtceu.api.capability.GTCapabilityHelper;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * The starter kit of a player who joins a world for the first time (docs/starter-kit.md): a fully charged Advanced
 * Prospector (HV), a Steel Mining Hammer (3 x 3), Quark's Forgotten Hat on the head, the starter guide (a Patchouli
 * book, data/af9/patchouli_books/starter_guide) and 0 to 23 dried kelp. {@code /af9 starterkit [player]} gives it again.
 */
@Mod.EventBusSubscriber(modid = AF9Core.MOD_ID)
public final class StarterKit {

    private static final String GIVEN = "af9_starter_kit_given";
    private static final String GUIDE = "af9:starter_guide";

    private StarterKit() {}

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (!AF9Config.STARTER_KIT.get()) return;
        CompoundTag persisted = player.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG);
        if (persisted.getBoolean(GIVEN)) return;
        // marked either way: only ever offered on the first join
        persisted.putBoolean(GIVEN, true);
        player.getPersistentData().put(Player.PERSISTED_NBT_TAG, persisted);
        boolean newPlayer = player.getStats().getValue(Stats.CUSTOM.get(Stats.PLAY_TIME)) == 0;
        if (!newPlayer || player.isSpectator()) return;
        give(player);
    }

    @SubscribeEvent
    public static void onCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("af9").requires(source -> source.hasPermission(2))
                .then(Commands.literal("starterkit")
                        .executes(context -> run(context.getSource(), context.getSource().getPlayerOrException()))
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(context -> run(context.getSource(),
                                        EntityArgument.getPlayer(context, "player"))))));
    }

    private static int run(CommandSourceStack source, ServerPlayer player) {
        give(player);
        source.sendSuccess(() -> Component.translatable("af9.starter_kit.given", player.getDisplayName()), true);
        return 1;
    }

    /** The whole kit: the hat is worn (kept in the inventory when the head is taken), the rest goes to the inventory. */
    public static void give(ServerPlayer player) {
        ItemStack prospector = stack("gtceu:prospector.hv");
        charge(prospector);
        give(player, prospector);
        give(player, stack("gtceu:steel_mining_hammer"));

        ItemStack hat = stack("quark:forgotten_hat");
        if (!hat.isEmpty()) {
            if (player.getItemBySlot(EquipmentSlot.HEAD).isEmpty()) player.setItemSlot(EquipmentSlot.HEAD, hat);
            else give(player, hat);
        }

        ItemStack guide = stack("patchouli:guide_book");
        if (!guide.isEmpty()) {
            guide.getOrCreateTag().putString("patchouli:book", GUIDE);
            give(player, guide);
        }

        int kelp = player.getRandom().nextInt(24);
        if (kelp > 0) {
            ItemStack dried = stack("minecraft:dried_kelp");
            dried.setCount(kelp);
            give(player, dried);
        }
    }

    private static void give(ServerPlayer player, ItemStack stack) {
        if (stack.isEmpty()) return;
        if (!player.getInventory().add(stack)) player.drop(stack, false);
    }

    private static ItemStack stack(String id) {
        Item item = ForgeRegistries.ITEMS.getValue(new ResourceLocation(id));
        if (item == null || item == net.minecraft.world.item.Items.AIR) {
            AF9Core.LOGGER.warn("Starter kit: item {} does not exist, left out", id);
            return ItemStack.EMPTY;
        }
        return new ItemStack(item);
    }

    /** Fills the item's battery. */
    private static void charge(ItemStack stack) {
        if (stack.isEmpty()) return;
        var electric = GTCapabilityHelper.getElectricItem(stack);
        if (electric != null) electric.charge(electric.getMaxCharge(), electric.getTier(), true, false);
    }
}
