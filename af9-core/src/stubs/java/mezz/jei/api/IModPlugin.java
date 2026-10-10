package mezz.jei.api;

import mezz.jei.api.registration.IRecipeCatalystRegistration;
import net.minecraft.resources.ResourceLocation;

/** Compile-time stand-in for JEI's interface (not shipped). */
public interface IModPlugin {

    ResourceLocation getPluginUid();

    default void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {}
}
