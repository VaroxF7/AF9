package com.af9.core.common;

import com.af9.core.compat.adastra.AdAstraOxygen;

import com.gregtechceu.gtceu.api.GTCEuAPI;
import com.gregtechceu.gtceu.api.machine.trait.RecipeLogic;
import com.gregtechceu.gtceu.api.recipe.GTRecipe;
import com.gregtechceu.gtceu.api.recipe.RecipeCondition;
import com.gregtechceu.gtceu.api.recipe.condition.RecipeConditionType;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;

import org.jetbrains.annotations.NotNull;

/**
 * Recipe condition: the machine stands in air that can be breathed (Ad Astra's oxygen: on the Moon only inside a
 * pressurised, oxygenated room). {@code isOxygenated = false} asks for the opposite. Copied from TerraFirmaGreg's
 * {@code OxygenatedCondition} (TerraFirmaGreg-Team/Core-Modern, LGPL-3.0; see CREDITS-textures.md), with Ad Astra's oxygen
 * for TFG's environment system. In KubeJS: {@code recipe.addCondition(new $OxygenatedCondition(false, true))}.
 */
public class OxygenatedCondition extends RecipeCondition<OxygenatedCondition> {

    public static final Codec<OxygenatedCondition> CODEC = RecordCodecBuilder.create(instance -> RecipeCondition
            .isReverse(instance)
            .and(Codec.BOOL.fieldOf("isOxygenated").forGetter(condition -> condition.isOxygenated))
            .apply(instance, OxygenatedCondition::new));

    /** Registered when GregTech opens its condition registry ({@link #register}). */
    public static RecipeConditionType<OxygenatedCondition> TYPE;

    private final boolean isOxygenated;

    public OxygenatedCondition() {
        super(false);
        this.isOxygenated = true;
    }

    public OxygenatedCondition(boolean isReverse, boolean isOxygenated) {
        super(isReverse);
        this.isOxygenated = isOxygenated;
    }

    /** GregTech's registry event for recipe conditions (mod bus). */
    public static void register(GTCEuAPI.RegisterEvent<String, RecipeConditionType<?>> event) {
        TYPE = new RecipeConditionType<>(OxygenatedCondition::new, CODEC);
        event.register("oxygenated", TYPE);
    }

    @Override
    public RecipeConditionType<OxygenatedCondition> getType() {
        return TYPE;
    }

    @Override
    public boolean isOr() {
        return true;
    }

    @Override
    public Component getTooltips() {
        return Component.translatable(isOxygenated ? "af9.recipe_condition.oxygenated.true" :
                "af9.recipe_condition.oxygenated.false");
    }

    /** The server checks the position of the machine itself (its walls are oxygenated too); the client never runs recipes. */
    @Override
    protected boolean testCondition(@NotNull GTRecipe recipe, @NotNull RecipeLogic recipeLogic) {
        var machine = recipeLogic.machine.self();
        if (!(machine.getLevel() instanceof ServerLevel level)) return false;
        return isOxygenated == AdAstraOxygen.hasOxygen(level, machine.getPos());
    }

    @Override
    public OxygenatedCondition createTemplate() {
        return new OxygenatedCondition();
    }
}
