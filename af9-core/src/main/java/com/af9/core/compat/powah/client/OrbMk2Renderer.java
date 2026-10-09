package com.af9.core.compat.powah.client;

import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import owmii.powah.client.render.tile.EnergizingOrbRenderer;

/** Powah's orb renderer (the floating items and the sphere) for the Mk2's tile type. */
public class OrbMk2Renderer extends EnergizingOrbRenderer {

    public OrbMk2Renderer(BlockEntityRendererProvider.Context context) {
        super(context);
    }
}
