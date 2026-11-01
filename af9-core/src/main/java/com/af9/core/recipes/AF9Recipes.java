package com.af9.core.recipes;

import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.api.data.chemical.ChemicalHelper;
import com.gregtechceu.gtceu.api.data.chemical.material.Material;
import com.gregtechceu.gtceu.api.data.tag.TagPrefix;
import com.gregtechceu.gtceu.common.data.GTMaterials;
import com.gregtechceu.gtceu.common.data.GTRecipeTypes;
import com.gregtechceu.gtceu.data.recipe.builder.GTRecipeBuilder;

import net.minecraft.data.recipes.FinishedRecipe;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

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
        com.af9.core.recipes.gen.GenNetherChemistry.register(out);
        com.af9.core.recipes.gen.GenBiofuels.register(out);
        com.af9.core.recipes.gen.GenAromatics.register(out);
    }

    /** A recipe builder of the given type, with the AF9 recipe id. */
    static GTRecipeBuilder af9(com.gregtechceu.gtceu.api.recipe.GTRecipeType type, String name) {
        return type.recipeBuilder(new ResourceLocation("af9", name));
    }

    static ItemStack dust(Material material) {
        return ChemicalHelper.get(TagPrefix.dust, material);
    }

    /** Late-game ethylene (was server_scripts/mods/gtceu/ethylene.js; spec docs/green-chemistry.md section 7). */
    private static final class Ethylene {

        static void register(Consumer<FinishedRecipe> out) {
            long luv = GTValues.VA[GTValues.LuV];

            af9(GTRecipeTypes.LARGE_CHEMICAL_RECIPES, "syngas_gasification")
                    .inputItems(TagPrefix.dust, GTMaterials.Carbon, 4)
                    .inputFluids(GTMaterials.Water.getFluid(4000))
                    .outputFluids(GTMaterials.CarbonMonoxide.getFluid(4000), GTMaterials.Hydrogen.getFluid(4000))
                    .duration(200).EUt(luv).save(out);

            af9(GTRecipeTypes.LARGE_CHEMICAL_RECIPES, "syngas_gasification_coke")
                    .inputItems(TagPrefix.dust, GTMaterials.Coke, 4)
                    .inputFluids(GTMaterials.Water.getFluid(4000))
                    .outputFluids(GTMaterials.CarbonMonoxide.getFluid(4000), GTMaterials.Hydrogen.getFluid(4000))
                    .duration(200).EUt(luv).save(out);

            af9(GTRecipeTypes.LARGE_CHEMICAL_RECIPES, "methanol_from_syngas")
                    .inputFluids(GTMaterials.CarbonMonoxide.getFluid(4000), GTMaterials.Hydrogen.getFluid(8000))
                    .outputFluids(GTMaterials.Methanol.getFluid(4000))
                    .duration(200).EUt(luv).save(out);

            ItemStack zeolite = dust(GTMaterials.Zeolite);

            af9(GTRecipeTypes.LARGE_CHEMICAL_RECIPES, "mto_ethylene")
                    .circuitMeta(1)
                    .notConsumable(zeolite)
                    .inputFluids(GTMaterials.Methanol.getFluid(8000))
                    .outputFluids(GTMaterials.Ethylene.getFluid(4000), GTMaterials.Water.getFluid(8000))
                    .duration(200).EUt(luv).save(out);

            af9(GTRecipeTypes.LARGE_CHEMICAL_RECIPES, "mto_olefins_mixed")
                    .circuitMeta(2)
                    .notConsumable(zeolite)
                    .inputFluids(GTMaterials.Methanol.getFluid(12000))
                    .outputFluids(GTMaterials.Ethylene.getFluid(3000), GTMaterials.Propene.getFluid(2000),
                            GTMaterials.Water.getFluid(12000))
                    .duration(200).EUt(luv).save(out);
        }
    }
}
