package com.af9.core.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.math.Axis;

import net.minecraft.Util;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.fml.ModList;

import earth.terrarium.adastra.api.client.events.AdAstraClientEvents;
import earth.terrarium.adastra.client.screens.PlanetsScreen;
import earth.terrarium.adastra.common.constants.PlanetConstants;

/**
 * Ad Astra's planet screen draws the map of its own four planets and nothing else: AF9's bodies (Ceres, Zephyr, Kronos,
 * Helios) get their orbit and icon here, through Ad Astra's solar system event. The icons are the sky bodies of
 * textures/environment (tools/textures/planets.py). Does nothing without Ad Astra.
 */
public final class AdAstraMap {

    private AdAstraMap() {}

    @SuppressWarnings("removal") // new ResourceLocation(ns, path) is the only constructor on 1.20.1
    private static ResourceLocation icon(String name) {
        return new ResourceLocation("af9", "textures/environment/icon_" + name + ".png");
    }

    private static final ResourceLocation CERES = icon("ceres");
    private static final ResourceLocation ZEPHYR = icon("zephyr");
    private static final ResourceLocation KRONOS = icon("kronos");
    private static final ResourceLocation HELIOS = icon("helios");

    public static void register() {
        if (!ModList.get().isLoaded("ad_astra")) return;
        AdAstraClientEvents.RenderSolarSystemEvent.register(AdAstraMap::render);
    }

    private static void render(GuiGraphics graphics, ResourceLocation solarSystem, int width, int height) {
        if (!PlanetConstants.SOLAR_SYSTEM.equals(solarSystem)) return;
        // orbits of Ceres, Zephyr and Kronos outside Mars's four (30 a ring); Helios hugs the Sun's disc
        Tesselator tessellator = Tesselator.getInstance();
        BufferBuilder bufferBuilder = tessellator.getBuilder();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        bufferBuilder.begin(VertexFormat.Mode.DEBUG_LINES, DefaultVertexFormat.POSITION_COLOR);
        PlanetsScreen.drawCircles(4, 3, 0xff24327b, bufferBuilder, width, height);
        PlanetsScreen.drawCircle(bufferBuilder, width / 2f, height / 2f, 15, 40, 0xff7b4a24);
        tessellator.end();

        float rotation = Util.getMillis() / 100f;
        body(graphics, CERES, width, height, 5 * 30, 12, rotation * 0.35f);
        body(graphics, ZEPHYR, width, height, 6 * 30, 14, rotation * 0.25f);
        body(graphics, KRONOS, width, height, 7 * 30, 16, rotation * 0.18f);
        body(graphics, HELIOS, width, height, 15, 9, rotation * 2.2f);
    }

    private static void body(GuiGraphics graphics, ResourceLocation icon, int width, int height, float orbit, int size,
                             float degrees) {
        graphics.pose().pushPose();
        graphics.pose().translate(width / 2f, height / 2f, 0);
        graphics.pose().mulPose(Axis.ZP.rotationDegrees(degrees));
        graphics.pose().translate(orbit - size / 2f, -size / 2f, 0);
        graphics.blit(icon, 0, 0, size, size, 0, 0, 16, 16, 16, 16);
        graphics.pose().popPose();
    }
}
