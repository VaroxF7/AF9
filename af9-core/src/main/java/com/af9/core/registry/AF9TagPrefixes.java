package com.af9.core.registry;

import com.gregtechceu.gtceu.api.GTCEuAPI;
import com.gregtechceu.gtceu.api.data.chemical.material.Material;
import com.gregtechceu.gtceu.api.data.chemical.material.info.MaterialFlags;
import com.gregtechceu.gtceu.api.data.chemical.material.info.MaterialIconType;
import com.gregtechceu.gtceu.api.data.chemical.material.properties.PropertyKey;
import com.gregtechceu.gtceu.api.data.tag.TagPrefix;
import com.gregtechceu.gtceu.common.data.GTRecipeTypes;

import net.minecraft.data.recipes.FinishedRecipe;

import java.util.function.Consumer;

import static com.gregtechceu.gtceu.api.GTValues.M;

/**
 * The Ultrdense Plate: four Dense Plates of the same material pressed into one ({@code gtceu:ultradense_iron_plate},
 * and the plates of every other material that has dense plates). It is a GregTech part rather than a pack item: a
 * {@link TagPrefix} makes GregTech itself register the item, its tag ({@code gtceu:ultradense_plates/<material>}),
 * its name ({@code tagprefix.ultradense_plate}), its unification and its recycling, the same way
 * {@link TagPrefix#plateDense} registers the dense plate. Nothing lists materials here: the condition of the prefix
 * is dense plates, so a material gets ultrdense plates exactly when it has dense ones.
 * <p>
 * The look is the dense plate's own art, pressed together: a new icon type ({@code plate_ultradense}) with a darker
 * body and a harder edge, in the seven icon sets GregTech draws dense plates for, under
 * {@code assets/gtceu/{models,textures}/item/material_sets/<set>}. A set whose dense model inherits from another one
 * (bright, magnetic, radioactive) inherits here the same way, and a material of a set without a model of its own
 * falls back to its parent.
 * <p>
 * The recipes follow GregTech's ({@link #addRecipes}): the Bender, four dense plates in, {@code 96 EU/t}, and the
 * mass of the material times the {@code 36} units an ultrdense plate holds, as the dense plate's recipe takes its
 * mass times the {@code 9} a dense plate holds.
 */
public final class AF9TagPrefixes {

    /**
     * The look of the shape, independent of the material: GregTech takes the texture and the model of
     * {@code plate_ultradense} from the icon set the material is drawn in.
     */
    private static final MaterialIconType PLATE_ULTRDENSE = new MaterialIconType("plateUltradense");

    /**
     * Four dense plates in one item: a dense plate holds {@code 9} material units ({@link TagPrefix#plateDense}),
     * so this one holds {@code 36}. The condition is the dense plate's flag, so the two parts always match.
     */
    public static final TagPrefix plateUltradense = new TagPrefix("ultradensePlate")
            .idPattern("ultradense_%s_plate")
            .defaultTagPath("ultradense_plates/%s")
            .unformattedTagPath("ultradense_plates")
            .langValue("Ultradense %s Plate")
            .materialAmount(M * 36)
            .maxStackSize(7)
            .materialIconType(PLATE_ULTRDENSE)
            .unificationEnabled(true)
            .enableRecycling()
            .generateItem(true)
            .generationCondition(mat -> mat.hasFlag(MaterialFlags.GENERATE_DENSE));

    private AF9TagPrefixes() {}

    /**
     * GregTech calls this while it sets up its TagPrefixes, before it makes the items of them. The fields of this
     * class register themselves on the way in, so the method only has to name the class.
     */
    public static void init() {}

    /**
     * {@code 4x dense plate -> ultrdense plate} on the Bender, for every material the prefix generates for. The dust
     * property is what makes a dense plate pressable in GregTech's own recipe, and a material without unification
     * keeps its recipes out of the way here as it does everywhere else.
     */
    public static void addRecipes(Consumer<FinishedRecipe> provider) {
        for (Material material : GTCEuAPI.materialManager.getRegisteredMaterials()) {
            if (!material.hasProperty(PropertyKey.DUST) || !material.shouldGenerateRecipesFor(TagPrefix.plateDense)
                    || !material.shouldGenerateRecipesFor(plateUltradense)) {
                continue;
            }
            GTRecipeTypes.BENDER_RECIPES.recipeBuilder(
                    "bend_" + material.getName() + "_dense_plate_to_ultradense_plate")
                    .inputItems(TagPrefix.plateDense, material, 4)
                    .outputItems(plateUltradense, material)
                    .duration((int) Math.max(material.getMass() * 36L, 1L))
                    .EUt(96)
                    .save(provider);
        }
    }
}
