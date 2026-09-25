package com.af9.core.fab;

import com.gregtechceu.gtceu.api.data.chemical.material.Material;
import com.gregtechceu.gtceu.api.recipe.GTRecipeType;
import com.gregtechceu.gtceu.common.data.GTMaterials;

import net.minecraft.world.level.material.Fluid;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * The four families of AF9 fab machines. Each has a single-block (tiered) and a multiblock version that share the
 * family's recipe types (machine modes, defined in kubejs/startup_scripts/gtceu/fab_machines.js).
 * <p>
 * A family also sets its product changeover: when a machine switches to a different recipe than the last one it
 * finished, the next run is preceded by a purge (flush) that takes extra time and a purge fluid, as real fab
 * equipment is cleaned between products. See {@link FabModifiers#PURGE}.
 */
public enum FabFamily {

    CHEMISTRY("chemistry", 0xFF4FC3F7, 1000, 100, () -> List.of(GTMaterials.Nitrogen, GTMaterials.Argon)),
    SEPARATION("separation", 0xFF81C784, 2000, 100, () -> List.of(GTMaterials.Nitrogen)),
    ELECTROCHEMISTRY("electrochemistry", 0xFFFFD54F, 1000, 100, () -> List.of(GTMaterials.DistilledWater)),
    THERMAL("thermal", 0xFFFF8A65, 1000, 200, () -> List.of(GTMaterials.Argon, GTMaterials.Nitrogen));

    /** Stage track shown on the consoles, per recipe type path; one short code per real process step. */
    private static final Map<String, String[]> STAGES = Map.ofEntries(
            Map.entry("fab_synthesis", new String[] { "CHRG", "HEAT", "REAC", "QNCH", "SEPR", "DSCH" }),
            Map.entry("fab_blending", new String[] { "DOSE", "MIX", "DGAS", "DSCH" }),
            Map.entry("fab_wet_processing", new String[] { "LOAD", "SOAK", "RNSE", "DRY" }),
            Map.entry("fab_purification", new String[] { "FEED", "ADSB", "FILT", "PROD" }),
            Map.entry("fab_distillation", new String[] { "FEED", "BOIL", "RFLX", "DRAW" }),
            Map.entry("fab_cryogenic_rectification", new String[] { "COOL", "LIQF", "RECT", "DRAW" }),
            Map.entry("fab_fractionation", new String[] { "FEED", "BOIL", "CNDS", "DRAW" }),
            Map.entry("fab_electrolysis", new String[] { "FILL", "POL", "ELEC", "STRP" }),
            Map.entry("fab_electrofluorination", new String[] { "FILL", "POL", "FLUR", "STRP" }),
            Map.entry("fab_calcination", new String[] { "LOAD", "RAMP", "SOAK", "COOL" }),
            Map.entry("fab_cvd", new String[] { "PURG", "RAMP", "DEPO", "COOL" }),
            Map.entry("fab_crystal_growth", new String[] { "MELT", "DIP", "NECK", "BODY", "TAIL", "COOL" }));
    private static final String[] DEFAULT_STAGES = { "IN", "RUN", "OUT" };

    public final String id;
    /** ARGB accent colour of the family's consoles. */
    public final int argb;
    /** mB of purge fluid a product changeover takes (one of {@link #purgeFluids()}). */
    public final int purgeAmount;
    /** Extra ticks a product changeover adds to the run (not overclocked). */
    public final int purgeTicks;
    private final Supplier<List<Material>> purgeMaterials;

    FabFamily(String id, int argb, int purgeAmount, int purgeTicks, Supplier<List<Material>> purgeMaterials) {
        this.id = id;
        this.argb = argb;
        this.purgeAmount = purgeAmount;
        this.purgeTicks = purgeTicks;
        this.purgeMaterials = purgeMaterials;
    }

    /** Fluids accepted for the changeover purge. Resolved lazily: GT's materials exist only after its init. */
    public List<Fluid> purgeFluids() {
        List<Fluid> fluids = new ArrayList<>();
        for (Material material : purgeMaterials.get()) {
            Fluid fluid = material.getFluid();
            if (fluid != null) fluids.add(fluid);
        }
        return fluids;
    }

    public static String[] stagesOf(GTRecipeType type) {
        return type == null ? DEFAULT_STAGES : STAGES.getOrDefault(type.registryName.getPath(), DEFAULT_STAGES);
    }
}
