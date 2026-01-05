package folk.sisby.antique_atlas.reloader;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import folk.sisby.antique_atlas.AntiqueAtlas;
import folk.sisby.antique_atlas.AntiqueAtlasConfig;
import folk.sisby.antique_atlas.TerrainTileProvider;
import folk.sisby.antique_atlas.TileElevation;
import folk.sisby.antique_atlas.TileTexture;
import folk.sisby.antique_atlas.util.ForgeTags;
import net.fabricmc.fabric.api.resource.IdentifiableResourceReloadListener;
import net.fabricmc.fabric.api.tag.convention.v1.ConventionalBiomeTags;
import net.minecraft.registry.Registry;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.tag.BiomeTags;
import net.minecraft.resource.JsonDataLoader;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;
import net.minecraft.util.profiler.Profiler;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.BiomeKeys;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class BiomeTileProviders extends JsonDataLoader implements IdentifiableResourceReloadListener {
	public static final BiomeTileProviders INSTANCE = new BiomeTileProviders();
	public static final Identifier ID = AntiqueAtlas.id("tile_provider/biome");

	public static BiomeTileProviders getInstance() {
		return INSTANCE;
	}

	protected final Map<Identifier, TerrainTileProvider> tileProviders = new HashMap<>();
	protected final Map<Identifier, Identifier> biomeFallbacks = new HashMap<>();
	protected boolean hasFallbacks = false;

	public BiomeTileProviders() {
		super(new Gson(), "atlas/biome");
	}

	public TerrainTileProvider getTileProvider(Identifier providerId) {
		return tileProviders.getOrDefault(providerId, tileProviders.getOrDefault(biomeFallbacks.get(providerId), TerrainTileProvider.DEFAULT));
	}

	/**
	 * Register fallbacks for any biomes present in the client world that don't have explicit sets.
	 * Doing this on world join catches data-biomes that might not be registered in other worlds.
	 */
	public void registerFallbacks(Registry<Biome> biomeRegistry) {
		for (Biome biome : biomeRegistry) {
			Identifier biomeId = biomeRegistry.getId(biome);
			if (tileProviders.containsKey(biomeId)) continue;
			Identifier fallbackBiome = getFallbackBiome(biomeRegistry.getEntry(biome));
			if (fallbackBiome != null && tileProviders.containsKey(fallbackBiome)) {
				biomeFallbacks.put(biomeId, fallbackBiome);
				AntiqueAtlas.LOGGER.info("[Antique Atlas] Set fallback biome for {} to {}. You can set a more fitting texture using a resource pack!", biomeId, fallbackBiome);
			} else if (fallbackBiome != null) {
				AntiqueAtlas.LOGGER.error("[Antique Atlas] Fallback biome for {} is {}, which has no defined tile provider.", biomeId, fallbackBiome);
			} else {
				AntiqueAtlas.LOGGER.warn("[Antique Atlas] No fallback could be found for {}. This shouldn't happen! This means the biome is not in ANY conventional or vanilla tag on the client!", biomeId);
				if (AntiqueAtlas.CONFIG.fallbackFailHandling == AntiqueAtlasConfig.FallbackHandling.CRASH) throw new IllegalStateException("Antique Atlas fallback biome registration failed! Fix the missing biome or change fallbackFailHandling in antique_atlas.toml");
			}
		}
		hasFallbacks = true;
	}

	public void clearFallbacks() {
		hasFallbacks = false;
		biomeFallbacks.clear();
	}

	public boolean hasFallbacks() {
		return hasFallbacks;
	}

	public static Identifier getFallbackBiome(RegistryEntry<Biome> biome) {
		if (biome.isIn(ConventionalBiomeTags.VOID) || biome.isIn(ForgeTags.Biomes.IS_VOID)) {
			return BiomeKeys.THE_VOID.getValue();
		} else if (biome.isIn(BiomeTags.IS_END) || biome.isIn(ConventionalBiomeTags.IN_THE_END) || biome.isIn(ConventionalBiomeTags.END_ISLANDS)) {
			if (biome.isIn(ConventionalBiomeTags.VEGETATION_DENSE) || biome.isIn(ConventionalBiomeTags.VEGETATION_SPARSE) || biome.isIn(ForgeTags.Biomes.IS_LUSH)) return BiomeKeys.END_HIGHLANDS.getValue();
			return BiomeKeys.END_BARRENS.getValue();
		} else if (biome.isIn(ConventionalBiomeTags.NETHER_FORESTS)) {
			return BiomeKeys.WARPED_FOREST.getValue();
		} else if (biome.isIn(BiomeTags.IS_NETHER) || biome.isIn(ConventionalBiomeTags.IN_NETHER)) {
			return BiomeKeys.SOUL_SAND_VALLEY.getValue();
		} else if (biome.isIn(ConventionalBiomeTags.SWAMP) || biome.isIn(ForgeTags.Biomes.IS_SWAMP)) {
			return BiomeKeys.SWAMP.getValue();
		} else if (biome.isIn(BiomeTags.IS_OCEAN) || biome.isIn(BiomeTags.IS_DEEP_OCEAN) || biome.isIn(ConventionalBiomeTags.DEEP_OCEAN) || biome.isIn(ConventionalBiomeTags.OCEAN) || biome.isIn(ConventionalBiomeTags.SHALLOW_OCEAN) || biome.isIn(BiomeTags.IS_RIVER) || biome.isIn(ConventionalBiomeTags.RIVER) || biome.isIn(ConventionalBiomeTags.AQUATIC) || biome.isIn(ConventionalBiomeTags.AQUATIC_ICY) || biome.isIn(ForgeTags.Biomes.IS_WATER)) {
			if (biome.isIn(ConventionalBiomeTags.ICY) || biome.isIn(ConventionalBiomeTags.AQUATIC_ICY)) return BiomeKeys.FROZEN_RIVER.getValue();
			return BiomeKeys.RIVER.getValue();
		} else if (biome.isIn(ConventionalBiomeTags.STONY_SHORES)) {
			return BiomeKeys.STONY_SHORE.getValue();
		} else if (biome.isIn(BiomeTags.IS_BEACH) || biome.isIn(ConventionalBiomeTags.BEACH)) {
			return BiomeKeys.BEACH.getValue();
		} else if (biome.isIn(BiomeTags.IS_JUNGLE) || biome.isIn(ConventionalBiomeTags.JUNGLE) || biome.isIn(ConventionalBiomeTags.TREE_JUNGLE)) {
			return BiomeKeys.JUNGLE.getValue();
		} else if (biome.isIn(ConventionalBiomeTags.FLOWER_FORESTS) || biome.isIn(ConventionalBiomeTags.FLORAL)) {
			return BiomeKeys.FLOWER_FOREST.getValue();
		} else if (biome.isIn(BiomeTags.IS_SAVANNA) || biome.isIn(ConventionalBiomeTags.SAVANNA) || biome.isIn(ConventionalBiomeTags.TREE_SAVANNA)) {
			return BiomeKeys.SAVANNA.getValue();
		} else if (biome.isIn(BiomeTags.IS_BADLANDS) || biome.isIn((ConventionalBiomeTags.BADLANDS)) || biome.isIn((ConventionalBiomeTags.MESA))) {
			return BiomeKeys.BADLANDS.getValue();
		} else if (biome.isIn(ConventionalBiomeTags.TREE_CONIFEROUS) || biome.isIn(ForgeTags.Biomes.IS_CONIFEROUS) || biome.isIn(BiomeTags.IS_TAIGA) || biome.isIn(ConventionalBiomeTags.TAIGA)) {
			if (biome.isIn(ConventionalBiomeTags.ICY) || biome.isIn(ConventionalBiomeTags.SNOWY)) return BiomeKeys.SNOWY_TAIGA.getValue();
			return BiomeKeys.TAIGA.getValue();
		} else if (biome.isIn(ConventionalBiomeTags.BIRCH_FOREST) || biome.isIn(ConventionalBiomeTags.TREE_DECIDUOUS)) {
			return BiomeKeys.BIRCH_FOREST.getValue();
		} else if (biome.isIn(BiomeTags.IS_FOREST) || biome.isIn(ConventionalBiomeTags.FOREST)) {
			return BiomeKeys.FOREST.getValue();
		} else if (biome.isIn(ConventionalBiomeTags.PLAINS) || biome.isIn(ConventionalBiomeTags.SNOWY_PLAINS) || biome.isIn(ForgeTags.Biomes.IS_PLAINS) || biome.isIn(ConventionalBiomeTags.SNOWY) || biome.isIn(ForgeTags.Biomes.IS_SNOWY)) {
			if (biome.isIn(ConventionalBiomeTags.ICY) || biome.isIn(ConventionalBiomeTags.SNOWY)) return BiomeKeys.SNOWY_PLAINS.getValue();
			return BiomeKeys.PLAINS.getValue();
		} else if (biome.isIn(ConventionalBiomeTags.DESERT) || biome.isIn(ConventionalBiomeTags.WASTELAND) || biome.isIn(ConventionalBiomeTags.DEAD) || biome.isIn(ForgeTags.Biomes.IS_SANDY) || biome.isIn(ForgeTags.Biomes.IS_DESERT) || biome.isIn(ForgeTags.Biomes.IS_DEAD) || biome.isIn(ForgeTags.Biomes.IS_WASTELAND)) {
			return BiomeKeys.DESERT.getValue();
		} else if (biome.isIn(ConventionalBiomeTags.ICY)) {
			return BiomeKeys.FROZEN_OCEAN.getValue();
		} else if (biome.isIn(ForgeTags.Biomes.IS_PLATEAU)) {
			return BiomeKeys.MEADOW.getValue();
		} else if (biome.isIn(ConventionalBiomeTags.EXTREME_HILLS) || biome.isIn(ConventionalBiomeTags.WINDSWEPT)) {
			return BiomeKeys.WINDSWEPT_HILLS.getValue();
		} else if (biome.isIn(ConventionalBiomeTags.MOUNTAIN_PEAK) || biome.isIn(ForgeTags.Biomes.IS_PEAK)) {
			return BiomeKeys.JAGGED_PEAKS.getValue();
		} else if (biome.isIn(BiomeTags.IS_MOUNTAIN) || biome.isIn(ConventionalBiomeTags.MOUNTAIN) || biome.isIn(ConventionalBiomeTags.MOUNTAIN_SLOPE) || biome.isIn(ForgeTags.Biomes.IS_SLOPE) || biome.isIn(ForgeTags.Biomes.IS_MOUNTAIN)) {
			return BiomeKeys.STONY_PEAKS.getValue();
		} else if (biome.isIn(ConventionalBiomeTags.MUSHROOM) || biome.isIn(ForgeTags.Biomes.IS_MUSHROOM)) {
			return BiomeKeys.MUSHROOM_FIELDS.getValue();
		} else if (biome.isIn(BiomeTags.IS_HILL)) {
			return BiomeKeys.WINDSWEPT_GRAVELLY_HILLS.getValue();
		} else if (biome.isIn(ConventionalBiomeTags.CAVES) || biome.isIn(ConventionalBiomeTags.UNDERGROUND) || biome.isIn(ForgeTags.Biomes.IS_UNDERGROUND) || biome.isIn(ForgeTags.Biomes.IS_CAVE)) {
			return BiomeKeys.DRIPSTONE_CAVES.getValue();
		} else if (biome.isIn(ForgeTags.Biomes.IS_SPOOKY)) {
			return BiomeKeys.DARK_FOREST.getValue();
		} else if (biome.isIn(ForgeTags.Biomes.IS_MAGICAL)) {
			return BiomeKeys.MUSHROOM_FIELDS.getValue();
		} else if (biome.isIn(ConventionalBiomeTags.VEGETATION_DENSE) || biome.isIn(ForgeTags.Biomes.IS_DENSE)) {
			return BiomeKeys.FOREST.getValue();
		} else if (biome.isIn(ConventionalBiomeTags.VEGETATION_SPARSE) || biome.isIn(ForgeTags.Biomes.IS_SPARSE)) {
			return BiomeKeys.PLAINS.getValue();
		} else if (biome.isIn(ConventionalBiomeTags.CLIMATE_HOT) || biome.isIn(ForgeTags.Biomes.IS_HOT)) {
			return BiomeKeys.DESERT.getValue();
		} else if (biome.isIn(ConventionalBiomeTags.CLIMATE_COLD) || biome.isIn(ForgeTags.Biomes.IS_COLD)) {
			return BiomeKeys.SNOWY_PLAINS.getValue();
		} else if (biome.isIn(ConventionalBiomeTags.CLIMATE_TEMPERATE)) {
			return BiomeKeys.PLAINS.getValue();
		} else if (biome.isIn(ConventionalBiomeTags.CLIMATE_DRY) || biome.isIn(ForgeTags.Biomes.IS_DRY)) {
			return BiomeKeys.BADLANDS.getValue();
		} else if (biome.isIn(ConventionalBiomeTags.CLIMATE_WET) || biome.isIn(ForgeTags.Biomes.IS_WET)) {
			return BiomeKeys.SWAMP.getValue();
		}
		return null;
	}

	public static TileTexture getTexture(Map<Identifier, TileTexture> textures, Identifier id) {
		if (textures.containsKey(id)) {
			return textures.get(id);
		} else {
			throw new IllegalStateException("texture %s is not present!".formatted(id));
		}
	}

	public static @Nullable List<TileTexture> resolveTextureJson(Map<Identifier, TileTexture> textures, JsonElement textureJson) {
		if (textureJson instanceof JsonPrimitive texturePrimitive && texturePrimitive.isString()) {
			return List.of(getTexture(textures, Identifier.tryParse(texturePrimitive.getAsString())));
		} else if (textureJson instanceof JsonArray textureArray) {
			return textureArray.asList().stream().map(je -> getTexture(textures, Identifier.tryParse(je.getAsString()))).toList();
		} else if (textureJson instanceof JsonObject textureObject && textureObject.keySet().stream().allMatch(k -> textureObject.get(k) instanceof JsonPrimitive jp && jp.isNumber())) {
			Multiset<TileTexture> outList = HashMultiset.create();
			textureObject.entrySet().forEach(e -> outList.add(getTexture(textures, Identifier.tryParse(e.getKey())), e.getValue().getAsInt()));
			return outList.stream().toList();
		}
		return null;
	}

	@Override
	protected void apply(Map<Identifier, JsonElement> prepared, ResourceManager manager, Profiler profiler) {
		AntiqueAtlas.LOGGER.info("[Antique Atlas] Reloading Biome Tile Providers...");
		Map<Identifier, TileTexture> textures = TileTextures.getInstance().getTextures();
		Set<TileTexture> unusedTextures = new HashSet<>(textures.values().stream().filter(t -> t.id().getPath().startsWith("biome")).toList());
		Map<Identifier, Identifier> providerParents = new HashMap<>();
		for (Map.Entry<Identifier, JsonElement> fileEntry : prepared.entrySet()) {
			Identifier fileId = fileEntry.getKey();
			try {
				JsonObject fileJson = fileEntry.getValue().getAsJsonObject();
				if (fileJson.has("parent")) {
					Identifier parentId = Identifier.tryParse(fileJson.getAsJsonPrimitive("parent").getAsString());
					providerParents.put(fileId, parentId);
					continue;
				}
				JsonElement overrideJson = fileJson.get("overrides");
				Map<Identifier, List<TileTexture>> overrides = new HashMap<>();
				if (overrideJson != null) {
					JsonObject overrideObject = overrideJson.getAsJsonObject();
					List<TileTexture> overrideTextures = null;
					for (String rawId : overrideObject.keySet()) {
						Identifier id = AntiqueAtlas.id(rawId);
						if (id == null) throw new IllegalStateException("Invalid id %s in overrides object!".formatted(rawId));
						overrideTextures = resolveTextureJson(textures, overrideObject.get(rawId));
						if (overrideTextures == null) throw new IllegalStateException("Malformed object %s in overrides object!".formatted(rawId));
						overrideTextures.forEach(unusedTextures::remove);
						overrides.put(id, overrideTextures);
					}
				} else {
					overrides = null;
				}
				JsonElement textureJson = fileJson.get("textures");
				List<TileTexture> defaultTextures = resolveTextureJson(textures, textureJson);
				if (defaultTextures != null) {
					defaultTextures.forEach(unusedTextures::remove);
					tileProviders.put(fileId, new TerrainTileProvider(fileId, defaultTextures, overrides));
				} else {
					JsonObject textureObject = textureJson.getAsJsonObject();
					Map<TileElevation, List<TileTexture>> textureElevations = new HashMap<>();
					Set<TileElevation> skippedElevations = new HashSet<>();
					List<TileTexture> elevationTextures = null;
					for (TileElevation elevation : TileElevation.values()) {
						if (textureObject.has(elevation.getName())) {
							elevationTextures = resolveTextureJson(textures, textureObject.get(elevation.getName()));
							if (elevationTextures == null) throw new IllegalStateException("Malformed object %s in textures object!".formatted(elevation.getName()));
							elevationTextures.forEach(unusedTextures::remove);
							textureElevations.put(elevation, elevationTextures);
							for (TileElevation skipped : skippedElevations) {
								textureElevations.put(skipped, elevationTextures);
							}
							skippedElevations.clear();
						} else {
							skippedElevations.add(elevation);
						}
					}
					if (textureElevations.isEmpty()) {
						throw new IllegalStateException("No elevation keys were found in the textures object!");
					}
					for (TileElevation elevation : skippedElevations) {
						textureElevations.put(elevation, elevationTextures);
					}
					tileProviders.put(fileId, new TerrainTileProvider(fileId, textureElevations, overrides));
				}
			} catch (Exception e) {
				AntiqueAtlas.LOGGER.error("[Antique Atlas] Error reading biome tile provider {}!", fileId, e);
			}
		}
		providerParents.forEach((id, parentId) -> {
			if (tileProviders.containsKey(parentId)) {
				tileProviders.put(id, tileProviders.get(parentId));
			} else {
				AntiqueAtlas.LOGGER.error("[Antique Atlas] Error reading biome tile provider {}!", id, new IllegalStateException("Parent id %s doesn't exist".formatted(parentId)));
			}
		});

		for (TileTexture texture : unusedTextures) {
			if (texture.displayId().startsWith("test") || texture.displayId().startsWith("base")) continue;
			AntiqueAtlas.LOGGER.warn("[Antique Atlas] Tile texture {} isn't referenced by any biome tile provider!", texture.displayId());
		}
	}

	@Override
	public Identifier getFabricId() {
		return ID;
	}

	@Override
	public Collection<Identifier> getFabricDependencies() {
		return List.of(TileTextures.ID);
	}
}
