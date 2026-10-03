package com.af9.core.client;

import com.gregtechceu.gtceu.api.data.worldgen.GTOreDefinition;
import com.gregtechceu.gtceu.client.ClientProxy;

import net.minecraft.resources.ResourceLocation;

import java.util.Map;

/**
 * GT's ore veins as a client knows them: the copy the server syncs to it (GT's own ore vein pages of the recipe
 * viewers read the same). The server's registry of veins is not filled on a client of a dedicated server. Client only:
 * call it from client code.
 */
public final class ClientOreVeins {

    private ClientOreVeins() {}

    public static Iterable<Map.Entry<ResourceLocation, GTOreDefinition>> get() {
        return ClientProxy.CLIENT_ORE_VEINS.entrySet();
    }
}
