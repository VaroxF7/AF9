package com.af9.lod;

import net.minecraftforge.fml.common.Mod;

/** Client-only entry point. Rendering hooks are added milestone by milestone, see README.md. */
@Mod(Horizon.MOD_ID)
public class Horizon {
    public static final String MOD_ID = "af9_lod";

    public Horizon() {
        // M1: GL capability check (4.6, compute, multi-draw-indirect), nothing is hooked yet.
    }
}
