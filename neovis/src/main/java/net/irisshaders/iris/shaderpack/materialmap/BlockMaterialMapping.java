package net.irisshaders.iris.shaderpack.materialmap;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Reference2ReferenceOpenHashMap;
import net.irisshaders.iris.Iris;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraftforge.client.ChunkRenderTypeSet;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class BlockMaterialMapping {
	public static Object2IntMap<BlockState> createBlockStateIdMap(Int2ObjectMap<List<BlockEntry>> blockPropertiesMap) {
		Object2IntMap<BlockState> blockStateIds = new Object2IntOpenHashMap<>();

		blockPropertiesMap.forEach((intId, entries) -> {
			for (BlockEntry entry : entries) {
				addBlockStates(entry, blockStateIds, intId);
			}
		});

		return blockStateIds;
	}

	/**
	 * Neovis: block ID map including block tag entries ({@code %namespace:tag}), like Iris 1.10.
	 * Explicit block entries are added first and tags after them; with putIfAbsent the first mapping of a state wins,
	 * so an explicitly listed block always keeps its own ID even when it is also in a tag used by another ID.
	 * Tags resolve to nothing until the client has received the server's tags (see {@link BlockTagState}).
	 */
	public static Object2IntMap<BlockState> createBlockStateIdMap(Int2ObjectMap<List<BlockEntry>> blockPropertiesMap,
																  Int2ObjectMap<List<TagEntry>> tagPropertiesMap) {
		Object2IntMap<BlockState> blockStateIds = createBlockStateIdMap(blockPropertiesMap);

		tagPropertiesMap.forEach((intId, entries) -> {
			for (TagEntry entry : entries) {
				addTag(entry, blockStateIds, intId);
			}
		});

		return blockStateIds;
	}

	private static void addTag(TagEntry tagEntry, Object2IntMap<BlockState> idMap, int intId) {
		NamespacedId id = tagEntry.id();

		// Same matching as Iris 1.10: namespace and path compared case-insensitively, exactly one tag must match.
		List<TagKey<Block>> compatibleTags = BuiltInRegistries.BLOCK.getTagNames()
			.filter(key -> key.location().getNamespace().equalsIgnoreCase(id.getNamespace())
				&& key.location().getPath().equalsIgnoreCase(id.getName()))
			.toList();

		if (compatibleTags.isEmpty()) {
			if (!FMLEnvironment.production) {
				Iris.logger.warn("Failed to find the block tag " + id.getNamespace() + ":" + id.getName());
			}

			return;
		}

		if (compatibleTags.size() > 1) {
			Iris.logger.error("More than one block tag matched " + id.getNamespace() + ":" + id.getName() + " (" + compatibleTags + "), ignoring it");
			return;
		}

		Map<String, String> propertyPredicates = tagEntry.propertyPredicates();
		int blocksMissingProperties = 0;

		for (Holder<Block> holder : BuiltInRegistries.BLOCK.getTagOrEmpty(compatibleTags.get(0))) {
			Block block = holder.value();
			StateDefinition<Block, BlockState> stateManager = block.getStateDefinition();

			if (propertyPredicates.isEmpty()) {
				for (BlockState state : stateManager.getPossibleStates()) {
					// NB: putIfAbsent: the first successful mapping takes precedence (OptiFine parity, Iris #1327)
					idMap.putIfAbsent(state, intId);
				}

				continue;
			}

			Map<Property<?>, String> properties = new HashMap<>();
			boolean missingProperty = false;

			for (Map.Entry<String, String> predicate : propertyPredicates.entrySet()) {
				Property<?> property = stateManager.getProperty(predicate.getKey());

				if (property == null) {
					// Iris 1.10 ignores a predicate the block doesn't have; Neovis does the same but logs one
					// summary line per tag instead of one warning per block (modded tags can hold hundreds of blocks).
					missingProperty = true;
					continue;
				}

				properties.put(property, predicate.getValue());
			}

			if (missingProperty) {
				blocksMissingProperties++;
			}

			for (BlockState state : stateManager.getPossibleStates()) {
				if (checkState(state, properties)) {
					idMap.putIfAbsent(state, intId);
				}
			}
		}

		if (blocksMissingProperties > 0) {
			Iris.logger.warn("block." + intId + ": " + blocksMissingProperties + " block(s) in tag " + id.getNamespace() + ":" + id.getName()
				+ " lack some of the properties " + propertyPredicates.keySet() + "; those filters were ignored for them");
		}
	}

	public static Map<Holder.Reference<Block>, ChunkRenderTypeSet> createBlockTypeMap(Map<NamespacedId, BlockRenderType> blockPropertiesMap) {
		Map<Holder.Reference<Block>, ChunkRenderTypeSet> blockTypeIds = new Object2ObjectOpenHashMap<>();

		blockPropertiesMap.forEach((id, blockType) -> {
			ResourceLocation resourceLocation = new ResourceLocation(id.getNamespace(), id.getName());

			ForgeRegistries.BLOCKS.getDelegate(resourceLocation).ifPresent(
					block -> blockTypeIds.put(block, ChunkRenderTypeSet.of(convertBlockToRenderType(blockType)))
			);
		});

		return blockTypeIds;
	}

	private static RenderType convertBlockToRenderType(BlockRenderType type) {
		if (type == null) {
			return null;
		}

		return switch (type) {
			case SOLID -> RenderType.solid();
			case CUTOUT -> RenderType.cutout();
			case CUTOUT_MIPPED -> RenderType.cutoutMipped();
			case TRANSLUCENT -> RenderType.translucent();
		};
	}

	private static void addBlockStates(BlockEntry entry, Object2IntMap<BlockState> idMap, int intId) {
		NamespacedId id = entry.id();
		ResourceLocation resourceLocation;
		try {
			resourceLocation = new ResourceLocation(id.getNamespace(), id.getName());
		} catch (Exception exception) {
			throw new IllegalStateException("Failed to get entry for " + intId, exception);
		}

		Optional<Holder.Reference<Block>> delegateOpt = ForgeRegistries.BLOCKS.getDelegate(resourceLocation);

		// If the block doesn't exist, by default the registry will return AIR. That probably isn't what we want.
		if (delegateOpt.isEmpty() || !delegateOpt.get().isBound()) {
			return;
		}

		Block block = delegateOpt.get().get();

		Map<String, String> propertyPredicates = entry.propertyPredicates();

		if (propertyPredicates.isEmpty()) {
			// Just add all the states if there aren't any predicates
			for (BlockState state : block.getStateDefinition().getPossibleStates()) {
				// NB: Using putIfAbsent means that the first successful mapping takes precedence
				//     Needed for OptiFine parity:
				//     https://github.com/IrisShaders/Iris/issues/1327
				idMap.putIfAbsent(state, intId);
			}

			return;
		}

		// As a result, we first collect each key=value pair in order to determine what properties we need to filter on.
		// We already get this from BlockEntry, but we convert the keys to `Property`s to ensure they exist and to avoid
		// string comparisons later.
		Map<Property<?>, String> properties = new HashMap<>();
		StateDefinition<Block, BlockState> stateManager = block.getStateDefinition();

		propertyPredicates.forEach((key, value) -> {
			Property<?> property = stateManager.getProperty(key);

			if (property == null) {
				Iris.logger.warn("Error while parsing the block ID map entry for \"" + "block." + intId + "\":");
				Iris.logger.warn("- The block " + resourceLocation + " has no property with the name " + key + ", ignoring!");

				return;
			}

			properties.put(property, value);
		});

		// Once we have a list of properties and their expected values, we iterate over every possible state of this
		// block and check for ones that match the filters. This isn't particularly efficient, but it works!
		for (BlockState state : stateManager.getPossibleStates()) {
			if (checkState(state, properties)) {
				// NB: Using putIfAbsent means that the first successful mapping takes precedence
				//     Needed for OptiFine parity:
				//     https://github.com/IrisShaders/Iris/issues/1327
				idMap.putIfAbsent(state, intId);
			}
		}
	}

	// We ignore generics here, the actual types don't matter because we just convert
	// them to strings anyways, and the compiler checks just get in the way.
	//
	// If you're able to rewrite this function without SuppressWarnings, feel free.
	// But otherwise it works fine.
	@SuppressWarnings({"rawtypes", "unchecked"})
	private static boolean checkState(BlockState state, Map<Property<?>, String> expectedValues) {
		for (Map.Entry<Property<?>, String> condition : expectedValues.entrySet()) {
			Property property = condition.getKey();
			String expectedValue = condition.getValue();

			String actualValue = property.getName(state.getValue(property));

			if (!expectedValue.equals(actualValue)) {
				return false;
			}
		}

		return true;
	}
}
