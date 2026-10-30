package com.af9.core.client.skin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.registries.ForgeRegistries;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import org.joml.Matrix4f;

/**
 * The silicon wafer that floats in the Orbital Array Mk2's printing chamber, where the red wool of the structure file
 * stands: the real wafer item, drawn big and flat across the tower's axis, turning slowly and bobbing; while the station
 * works it is the centre of the loop ({@link OrbitalFx}). Drawn in the pattern's own space.
 */
@SuppressWarnings("removal") // new ResourceLocation(ns, path) is the only constructor on 1.20.1
final class WaferDraw {

    /** The wafer's diameter in blocks. */
    private static final float SIZE = 2.3F;
    private static final int FULL_BRIGHT = 0xF000F0;
    private static ItemStack stack;
    private static boolean looked;

    private WaferDraw() {}

    private static ItemStack wafer() {
        if (!looked) {
            looked = true;
            var item = ForgeRegistries.ITEMS.getValue(new ResourceLocation("gtceu", "silicon_wafer"));
            if (item != null && item != Items.AIR) stack = new ItemStack(item);
        }
        return stack;
    }

    /** Where the wafer is now: the bob, and the tremble while the light is on it. */
    private static void place(PoseStack poseStack, float time, float cycle) {
        float bob = 0.03F * Mth.sin(time * 0.07F);
        float tremble = 0F;
        if (cycle >= OrbitalFx.EXPOSE && cycle < OrbitalFx.ATOMIZE) {
            tremble = 0.045F * (cycle - OrbitalFx.EXPOSE) / (OrbitalFx.ATOMIZE - OrbitalFx.EXPOSE);
        }
        float tx = tremble * Mth.sin(time * 2.3F), ty = tremble * Mth.sin(time * 2.9F + 1F);
        poseStack.translate(OrbitalFx.CX + tx, OrbitalFx.CY + ty, OrbitalFx.WAFER_Z + bob);
        // it turns about the beam's axis, quickly while it is built and while the light spreads over it
        float spin = time * 0.022F;
        if (cycle >= 0F && cycle < OrbitalFx.BUILD_END) spin += (1F - cycle / OrbitalFx.BUILD_END) * 6F;
        poseStack.mulPose(Axis.ZP.rotation(spin));
    }

    /**
     * The wafer item. {@code cycle} is the loop's clock (negative: no loop, the wafer floats whole). Call before the
     * additive pass: the item draws with its own render types.
     */
    static void draw(PoseStack poseStack, MultiBufferSource buffer, float time, float cycle) {
        ItemStack item = wafer();
        float scale = OrbitalFx.waferScale(cycle);
        ClientLevel level = Minecraft.getInstance().level;
        if (item == null || level == null || scale <= 0.01F) return;
        poseStack.pushPose();
        place(poseStack, time, cycle);
        poseStack.scale(SIZE * scale, SIZE * scale, SIZE * scale);
        Minecraft.getInstance().getItemRenderer().renderStatic(item, ItemDisplayContext.NONE, FULL_BRIGHT,
                OverlayTexture.NO_OVERLAY, poseStack, buffer, level, 0);
        poseStack.popPose();
    }

    /** A formed station without power: only the wafer floats in the chamber, with a faint shimmer on it. */
    static void drawIdle(PoseStack poseStack, MultiBufferSource buffer, float time) {
        draw(poseStack, buffer, time, -1F);
        VertexConsumer c = buffer.getBuffer(com.af9.core.client.render.AF9RenderTypes.LIGHTNING);
        Matrix4f m = poseStack.last().pose();
        float breathe = 0.5F + 0.5F * Mth.sin(time * 0.05F);
        FxGeo.softRing(c, m, OrbitalFx.CX, OrbitalFx.CY, OrbitalFx.WAFER_Z + 0.1F, 1F, 0F, 0F, 0F, 1F, 0F,
                SIZE * 0.5F + 0.05F, 0.1F, 24, 0.3F, 0.6F, 1F, 0.12F + 0.1F * breathe);
    }
}
