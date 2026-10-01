package com.af9.core.mixin.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;
import com.mojang.blaze3d.vertex.PoseStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * No clouds in the Asteroid Field and on Ceres: they have no atmosphere. Ad Astra's sky effects set a cloud height of
 * 192 for every dimension that uses them (the Field uses its Mars orbit sky), and vanilla draws clouds whenever one is
 * set.
 */
@Mixin(LevelRenderer.class)
public abstract class LevelRendererCloudsMixin {

    @Inject(method = "renderClouds", at = @At("HEAD"), cancellable = true, require = 0)
    @SuppressWarnings("removal") // new ResourceLocation(ns, path) is the only constructor on 1.20.1
    private void af9$noClouds(PoseStack poseStack, Matrix4f projectionMatrix, float partialTick, double camX,
                              double camY, double camZ, CallbackInfo ci) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) return;
        ResourceLocation dimension = level.dimension().location();
        if (dimension.getNamespace().equals("af9") &&
                (dimension.getPath().equals("asteroid_field") || dimension.getPath().equals("ceres"))) {
            ci.cancel();
        }
    }
}
