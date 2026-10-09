package owmii.powah.client.render.tile;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import owmii.powah.block.energizing.EnergizingOrbTile;

/** Compile-time stand-in for Powah's class (not shipped): its constructor is protected, a subclass registers it for another tile. */
public class EnergizingOrbRenderer implements BlockEntityRenderer<EnergizingOrbTile> {

    protected EnergizingOrbRenderer(BlockEntityRendererProvider.Context context) {}

    @Override
    public void render(EnergizingOrbTile tile, float partialTick, PoseStack pose, MultiBufferSource buffers, int light,
                       int overlay) {}
}
