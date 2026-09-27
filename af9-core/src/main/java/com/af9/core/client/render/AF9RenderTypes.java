package com.af9.core.client.render;

import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;

/**
 * AF9's render types (client). The light ring's glow and its lightning are see-through light: they test depth (walls in
 * front hide them) but never write it, so nothing drawn after them disappears behind the glow (GT's frames, glass and
 * other see-through blocks did, with GT's light ring type). Both are drawn after the translucent blocks
 * ({@link LightRingRender}); with a shader pack the ring goes through the pack instead, in {@link #SHADER_RING}.
 */
@SuppressWarnings("removal") // new ResourceLocation(ns, path) is the only constructor on 1.20.1
public final class AF9RenderTypes extends RenderType {

    /**
     * Plain white, bound for the shader pack's sake: lightning's shader has no texture, but a pack's lightning program
     * may sample the bound one (Photon multiplies by it and drops what comes out nearly transparent, so whatever was
     * bound before could hide the ring).
     */
    public static final ResourceLocation WHITE = new ResourceLocation("af9", "textures/misc/white.png");

    /** The ring's tori: quads, blended, both sides, colour only. */
    public static final RenderType LIGHT_RING = create("af9_light_ring", DefaultVertexFormat.POSITION_COLOR,
            VertexFormat.Mode.QUADS, 4096, false, false,
            CompositeState.builder()
                    .setShaderState(POSITION_COLOR_SHADER)
                    .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
                    .setCullState(NO_CULL)
                    .setWriteMaskState(COLOR_WRITE)
                    .createCompositeState(false));

    /**
     * A hot ring's glow ({@link com.af9.core.machine.ILightRingMachine#ringGlow()} above 1): as {@link #LIGHT_RING}
     * but added light, so the layers add up and burn out to the colour.
     */
    public static final RenderType LIGHT_RING_GLOW = create("af9_light_ring_glow", DefaultVertexFormat.POSITION_COLOR,
            VertexFormat.Mode.QUADS, 4096, false, false,
            CompositeState.builder()
                    .setShaderState(POSITION_COLOR_SHADER)
                    .setTransparencyState(LIGHTNING_TRANSPARENCY)
                    .setCullState(NO_CULL)
                    .setWriteMaskState(COLOR_WRITE)
                    .createCompositeState(false));

    /**
     * The ring's tori with a shader pack: in lightning's shader, which a pack replaces with its lightning program and
     * lights up (with vanilla's colour shader, a pack draws it as plain unlit geometry); added light, both sides,
     * colour only; white bound ({@link #WHITE}). Photon draws all lightning white, so there the ring is white.
     */
    public static final RenderType SHADER_RING = create("af9_shader_ring", DefaultVertexFormat.POSITION_COLOR,
            VertexFormat.Mode.QUADS, 4096, false, false,
            CompositeState.builder()
                    .setShaderState(RENDERTYPE_LIGHTNING_SHADER)
                    .setTextureState(new TextureStateShard(WHITE, false, false))
                    .setTransparencyState(LIGHTNING_TRANSPARENCY)
                    .setCullState(NO_CULL)
                    .setWriteMaskState(COLOR_WRITE)
                    .createCompositeState(false));

    /** The lightning: quads, added light (vanilla lightning's blend), both sides, colour only; white bound. */
    public static final RenderType LIGHTNING = create("af9_lightning", DefaultVertexFormat.POSITION_COLOR,
            VertexFormat.Mode.QUADS, 4096, false, false,
            CompositeState.builder()
                    .setShaderState(RENDERTYPE_LIGHTNING_SHADER)
                    .setTextureState(new TextureStateShard(WHITE, false, false))
                    .setTransparencyState(LIGHTNING_TRANSPARENCY)
                    .setCullState(NO_CULL)
                    .setWriteMaskState(COLOR_WRITE)
                    .createCompositeState(false));

    private AF9RenderTypes(String name, VertexFormat format, VertexFormat.Mode mode, int bufferSize,
                           boolean affectsCrumbling, boolean sortOnUpload, Runnable setup, Runnable clear) {
        super(name, format, mode, bufferSize, affectsCrumbling, sortOnUpload, setup, clear);
    }
}
