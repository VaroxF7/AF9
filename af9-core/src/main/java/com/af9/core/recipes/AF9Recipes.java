package com.af9.core.recipes;

import net.minecraft.data.recipes.FinishedRecipe;

import java.util.function.Consumer;

/**
 * AF9's GregTech recipes, written in Java and registered through GregTech's addon hook
 * ({@link com.af9.core.AF9Addon#addRecipes}). They replace the KubeJS server scripts one theme at a time; the id of
 * every recipe is the one the script gave it ({@code af9:<name>}).
 */
public final class AF9Recipes {

    private AF9Recipes() {}

    public static void register(Consumer<FinishedRecipe> out) {
        Ethylene.register(out);
        com.af9.core.recipes.gen.GenAll.register(out);
    }

    /** Late-game ethylene (was server_scripts/mods/gtceu/ethylene.js; spec docs/green-chemistry.md section 7). */
    private static final class Ethylene {

        static void register(Consumer<FinishedRecipe> out) {

            Dsl.gt(out, "large_chemical_reactor", "af9:syngas_gasification")
                    .itemInputs("4x gtceu:carbon_dust")
                    .inputFluids(Dsl.fluid("minecraft:water", 4000))
                    .outputFluids(Dsl.fluid("gtceu:carbon_monoxide", 4000), Dsl.fluid("gtceu:hydrogen", 4000))
                    .duration(200)
                    .EUt(32768)
                    .save();

            Dsl.gt(out, "large_chemical_reactor", "af9:syngas_gasification_coke")
                    .itemInputs("4x gtceu:coke_dust")
                    .inputFluids(Dsl.fluid("minecraft:water", 4000))
                    .outputFluids(Dsl.fluid("gtceu:carbon_monoxide", 4000), Dsl.fluid("gtceu:hydrogen", 4000))
                    .duration(200)
                    .EUt(32768)
                    .save();

            Dsl.gt(out, "large_chemical_reactor", "af9:methanol_from_syngas")
                    .inputFluids(Dsl.fluid("gtceu:carbon_monoxide", 4000), Dsl.fluid("gtceu:hydrogen", 8000))
                    .outputFluids(Dsl.fluid("gtceu:methanol", 4000))
                    .duration(200)
                    .EUt(32768)
                    .save();

            Dsl.gt(out, "large_chemical_reactor", "af9:mto_ethylene")
                    .circuit(1)
                    .notConsumable("gtceu:zeolite_dust")
                    .inputFluids(Dsl.fluid("gtceu:methanol", 8000))
                    .outputFluids(Dsl.fluid("gtceu:ethylene", 4000), Dsl.fluid("minecraft:water", 8000))
                    .duration(200)
                    .EUt(32768)
                    .save();

            Dsl.gt(out, "large_chemical_reactor", "af9:mto_olefins_mixed")
                    .circuit(2)
                    .notConsumable("gtceu:zeolite_dust")
                    .inputFluids(Dsl.fluid("gtceu:methanol", 12000))
                    .outputFluids(Dsl.fluid("gtceu:ethylene", 3000), Dsl.fluid("gtceu:propene", 2000),
                            Dsl.fluid("minecraft:water", 12000))
                    .duration(200)
                    .EUt(32768)
                    .save();
        }
    }
}
