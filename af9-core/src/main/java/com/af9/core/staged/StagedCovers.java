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
 * {@link GTRegistries#COVERS}. The cover item ({@code AF9Items}) keeps this class loaded; {@link #init} only exists
 * so common setup states the dependency out loud. The renderer reuses GT's activity detector texture, so the cover
 * needs no art of its own.
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

    /** Common setup; also triggers the class load when nothing referenced the item yet. */
    public static void init() {}
}
