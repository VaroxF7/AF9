package com.af9.core.compat.powah.client;

import com.af9.core.compat.powah.OrbMk2;
import com.af9.core.compat.powah.OrbRecipes;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;

import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

/** The client side of the Energizing Orb Mk2: its screen and the renderer of its tile (with Powah loaded only). */
public final class OrbMk2Client {

    private OrbMk2Client() {}

    public static void init(IEventBus modBus) {
        MinecraftForge.EVENT_BUS.addListener(OrbMk2Client::onTooltip);
        modBus.addListener(OrbMk2Client::clientSetup);
        modBus.addListener(OrbMk2Client::registerRenderers);
    }

    /** Every item an orb recipe keeps (a mold) says so: a red NC line, not consumed. */
    private static void onTooltip(ItemTooltipEvent event) {
        Level level = Minecraft.getInstance().level;
        if (level == null || event.getItemStack().isEmpty()) return;
        if (!OrbRecipes.isMold(level, event.getItemStack())) return;
        event.getToolTip().add(Math.min(1, event.getToolTip().size()),
                Component.translatable("af9.orb_mk2.nc").withStyle(ChatFormatting.RED));
    }

    private static void clientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> MenuScreens.register(OrbMk2.MENU.get(), OrbMk2Screen::new));
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        // Powah's renderer is written for its own tile class, which the Mk2's extends
        BlockEntityRendererProvider provider = OrbMk2Renderer::new;
        event.registerBlockEntityRenderer(OrbMk2.TILE.get(), provider);
    }
}
