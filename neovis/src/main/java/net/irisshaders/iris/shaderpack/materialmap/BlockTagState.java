package net.irisshaders.iris.shaderpack.materialmap;

import net.minecraftforge.event.TagsUpdatedEvent;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Neovis: counts block tag updates so that pipelines whose block.properties use tags rebuild their block ID map
 * once the client has the real tags. Forge binds client tags when the server's tag packet arrives, which can be after
 * the shader pipeline was created on world join, and again after /reload.
 */
public final class BlockTagState {
	private static final AtomicInteger GENERATION = new AtomicInteger();

	private BlockTagState() {
	}

	public static int generation() {
		return GENERATION.get();
	}

	public static void onTagsUpdated(TagsUpdatedEvent event) {
		GENERATION.incrementAndGet();
	}
}
