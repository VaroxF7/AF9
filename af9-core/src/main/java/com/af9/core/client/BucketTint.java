package com.af9.core.client;

import com.af9.core.AF9Core;
import com.af9.core.fluid.PlaceableFluids;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.client.event.RegisterColorHandlersEvent;
import net.minecraftforge.client.extensions.common.IClientFluidTypeExtensions;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * The placeable buckets ({@link PlaceableFluids}) tint their content layer with the fluid's colour: the models
 * paint vanilla's bucket on layer 0 and {@code af9:item/bucket_content} on layer 1.
 */
public final class BucketTint {

    private BucketTint() {}

    public static void register(RegisterColorHandlersEvent.Item event) {
        for (String path : PlaceableFluids.paths()) {
            Item bucket = ForgeRegistries.ITEMS.getValue(new ResourceLocation(AF9Core.MOD_ID, path + "_bucket"));
            Fluid fluid = bucket == null || bucket == Items.AIR ? null : PlaceableFluids.fluidOfBucket(bucket);
            if (fluid == null) continue;
            int tint = IClientFluidTypeExtensions.of(fluid).getTintColor(new FluidStack(fluid, 1));
            event.register((stack, layer) -> layer == 1 ? tint : -1, bucket);
        }
    }
}
