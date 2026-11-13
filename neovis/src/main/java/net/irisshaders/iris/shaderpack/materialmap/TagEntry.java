package net.irisshaders.iris.shaderpack.materialmap;

import java.util.Map;

/**
 * A block tag entry in block.properties, written as {@code %namespace:tag} or {@code %namespace:tag:key=value}.
 * Ported from Iris 1.10 (LGPL-3.0); exposed to packs through {@code IRIS_TAG_SUPPORT 2}.
 */
public record TagEntry(NamespacedId id, Map<String, String> propertyPredicates) implements Entry {
}
