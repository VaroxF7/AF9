package com.af9.core.staged;

import com.af9.core.AF9Core;

import com.gregtechceu.gtceu.GTCEu;
import com.gregtechceu.gtceu.api.cover.CoverDefinition;
import com.gregtechceu.gtceu.api.registry.GTRegistries;
import com.gregtechceu.gtceu.client.renderer.cover.ICoverRenderer;
import com.gregtechceu.gtceu.client.renderer.cover.SimpleCoverRenderer;

import net.minecraft.resources.ResourceLocation;

import java.util.function.Supplier;

/**
 * AF9's covers, registered the way GT registers its own (see {@code GTCovers}): the definition goes straight into
 * {@link GTRegistries#COVERS}. GregTech freezes that registry at the end of its cover pass, so this class has to be
 * loaded from {@code AF9Addon#registerCovers}, which GT calls inside that pass; the cover item
 * ({@code AF9Items}) then only reads the already-registered definition. The renderer reuses GT's activity detector
 * texture, so the cover needs no art of its own.
 */
public final class StagedCovers {

    @SuppressWarnings("removal") // new ResourceLocation(ns, path) is the only constructor on 1.20.1
    public static final CoverDefinition STAGED_STEP_DETECTOR = register(
            new ResourceLocation(AF9Core.MOD_ID, "staged_step_detector"),
            StagedStepDetectorCover::new,
            () -> () -> new SimpleCoverRenderer(GTCEu.id("block/cover/activity_detector")));

    private StagedCovers() {}

    private static CoverDefinition register(ResourceLocation id,
            CoverDefinition.CoverBehaviourProvider behavior,
            Supplier<Supplier<ICoverRenderer>> renderer) {
        var definition = new CoverDefinition(id, behavior, renderer);
        GTRegistries.COVERS.register(definition.getId(), definition);
        return definition;
    }

    /** Loads this class (and so registers the cover) from GregTech's cover pass. */
    public static void init() {}
}
