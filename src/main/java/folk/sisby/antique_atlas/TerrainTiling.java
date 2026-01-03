package folk.sisby.antique_atlas;

import folk.sisby.antique_atlas.reloader.BiomeTileProviders;
import folk.sisby.surveyor.WorldSummary;
import folk.sisby.surveyor.terrain.ChunkSummary;
import folk.sisby.surveyor.terrain.LayerSummary;
import folk.sisby.surveyor.terrain.WorldTerrain;
import it.unimi.dsi.fastutil.Pair;
import it.unimi.dsi.fastutil.objects.Reference2BooleanArrayMap;
import it.unimi.dsi.fastutil.objects.Reference2IntArrayMap;
import net.fabricmc.fabric.api.tag.convention.v1.ConventionalBiomeTags;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.tag.BiomeTags;
import net.minecraft.util.Identifier;
import net.minecraft.util.collection.IndexedIterable;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;

/**
 * Hottest class in the mod. Might get ugly.
 */
public class TerrainTiling {
	public static final int EMPTY_PRIORITY = 16;
	public static final int RAVINE_PRIORITY = 12;
	public static final int LAVA_PRIORITY = 6;
	public static final int WATER_PRIORITY = 4;
	public static final int ICE_PRIORITY = 3;
	public static final int BEACH_PRIORITY = 3;

	public static final List<Identifier> CUSTOM_TILES = List.of(
		FeatureTiles.BEDROCK_ROOF,
		FeatureTiles.EMPTY,
		FeatureTiles.END_VOID,
		FeatureTiles.WATER,
		FeatureTiles.ICE,
		FeatureTiles.TILE_RAVINE,
		FeatureTiles.TILE_LAVA,
		FeatureTiles.TILE_LAVA_SHORE
	);

	public static final int NETHER_SCAN_HEIGHT = 50;
	public static final Map<Biome, Integer> priorityCache = new Reference2IntArrayMap<>();
	public static final Map<Biome, Boolean> swampCache = new Reference2BooleanArrayMap<>();
	private static final int SEA_LEVEL = 63;

	public static int priorityForBiome(Registry<Biome> biomeRegistry, Biome biome) {
		return priorityCache.computeIfAbsent(biome, b -> {
			RegistryEntry<Biome> biomeEntry = biomeRegistry.getEntry(biome);
			if (biomeEntry.isIn(BiomeTags.IS_BEACH)) {
				return BEACH_PRIORITY;
			} else if (biomeEntry.isIn(BiomeTags.IS_NETHER)) {
				return 2;
			} else {
				return 1;
			}
		});
	}

	public static Pair<TerrainTileProvider, TileElevation> frequencyToTexture(int[][] possibleTiles, Registry<Biome> biomeRegistry, IndexedIterable<Biome> biomePalette) {
		int elevationOrdinal = -1;
		int biomeIndex = -1;
		int bestFrequency = 0;
		for (int i = 0; i < possibleTiles.length; i++) {
			for (int j = 0; j < possibleTiles[i].length; j++) {
				if (possibleTiles[i][j] > bestFrequency) {
					elevationOrdinal = i;
					biomeIndex = j;
					bestFrequency = possibleTiles[i][j];
				}
			}
		}
		if (bestFrequency == 0) return null;
		int customTileIndex = biomeIndex - possibleTiles[0].length + CUSTOM_TILES.size();
		Identifier providerId = customTileIndex >= 0 ? CUSTOM_TILES.get(customTileIndex) : biomeRegistry.getId(biomePalette.get(biomeIndex));
		return Pair.of(BiomeTileProviders.getInstance().getTileProvider(providerId), elevationOrdinal == TileElevation.values().length ? null : TileElevation.values()[elevationOrdinal]);
	}

	public static Pair<TerrainTileProvider, TileElevation> terrainToTile(WorldSummary summary, DynamicRegistryManager manager, ChunkPos pos) {
		Registry<Biome> biomeRegistry = manager.get(RegistryKeys.BIOME);
		int defaultTile = CUSTOM_TILES.indexOf(summary.dimension() == World.END ? FeatureTiles.END_VOID : FeatureTiles.EMPTY);
		boolean checkRavines = summary.dimension() == World.OVERWORLD;

		int topY = 999;

		WorldTerrain terrain = summary.terrain();
		if (terrain == null) return null;
		ChunkSummary chunk = terrain.get(pos);
		if (chunk == null) return null; // Skip events fired for chunks we don't have yet (e.g. new shares)
		@Nullable LayerSummary.Raw lithograph = chunk.toSingleLayer(null, null, topY);
		IndexedIterable<Biome> biomePalette = terrain.getBiomePalette(pos);
		IndexedIterable<Block> blockPalette = terrain.getBlockPalette(pos);
		if (lithograph == null) return Pair.of(BiomeTileProviders.getInstance().getTileProvider(CUSTOM_TILES.get(defaultTile)), null);

		int elevationSize = TileElevation.values().length;
		int elevationCount = elevationSize + 1;
		int biomeCount = biomePalette.size();
		int baseTileCount = biomeCount + CUSTOM_TILES.size();
		int[][] possibleTiles = new int[elevationCount][baseTileCount];

		for (int i = 0; i < lithograph.depths().length; i++) {
			if (!lithograph.exists().get(i)) {
				possibleTiles[elevationSize][defaultTile] += EMPTY_PRIORITY;
				continue;
			}
			int height = topY - lithograph.depths()[i] + lithograph.waterDepths()[i];
			Block block = blockPalette.get(lithograph.blocks()[i]);
			Biome biome = biomePalette.get(lithograph.biomes()[i]);

			if (checkRavines && height - SEA_LEVEL < -7) {
				possibleTiles[elevationSize][biomeCount + CUSTOM_TILES.indexOf(FeatureTiles.TILE_RAVINE)] += RAVINE_PRIORITY;
			} else if (lithograph.waterDepths()[i] > 0) {
				possibleTiles[elevationSize][biomeCount + CUSTOM_TILES.indexOf(FeatureTiles.WATER)] += WATER_PRIORITY;
			} else if (block == Blocks.ICE) {
				possibleTiles[elevationSize][biomeCount + CUSTOM_TILES.indexOf(FeatureTiles.ICE)] += ICE_PRIORITY;
			} else if (block == Blocks.LAVA) {
				possibleTiles[elevationSize][biomeCount + CUSTOM_TILES.indexOf(FeatureTiles.TILE_LAVA)] += LAVA_PRIORITY;
			}
			possibleTiles[TileElevation.fromBlocksAboveSea(height - SEA_LEVEL).ordinal()][lithograph.biomes()[i]] += priorityForBiome(biomeRegistry, biome);
		}

		return frequencyToTexture(possibleTiles, biomeRegistry, biomePalette);
	}

	public static Pair<TerrainTileProvider, TileElevation> terrainToTileNether(WorldSummary summary, DynamicRegistryManager manager, ChunkPos pos) {
		Registry<Biome> biomeRegistry = manager.get(RegistryKeys.BIOME);
		int defaultTile = CUSTOM_TILES.indexOf(FeatureTiles.BEDROCK_ROOF);

		int topY = 999;
		int logicalTopY = 126;

		WorldTerrain terrain = summary.terrain();
		if (terrain == null) return null;
		ChunkSummary chunk = terrain.get(pos);
		if (chunk == null) return null; // Skip events fired for chunks we don't have yet (e.g. new shares)
		@Nullable LayerSummary.Raw lowLithograph = chunk.toSingleLayer(null, NETHER_SCAN_HEIGHT, topY);
		@Nullable LayerSummary.Raw fullLithograph = chunk.toSingleLayer(null, logicalTopY, topY);
		IndexedIterable<Biome> biomePalette = terrain.getBiomePalette(pos);
		IndexedIterable<Block> blockPalette = terrain.getBlockPalette(pos);

		int elevationSize = TileElevation.values().length;
		int elevationCount = elevationSize + 1;
		int biomeCount = biomePalette.size();
		int baseTileCount = biomeCount + CUSTOM_TILES.size();
		int[][] possibleTiles = new int[elevationCount][baseTileCount];

		if (fullLithograph == null) {
			return Pair.of(BiomeTileProviders.getInstance().getTileProvider(CUSTOM_TILES.get(defaultTile)), null);
		}

		int SEA_DEPTH = topY - 31;

		if (lowLithograph == null) {
			for (int i = 0; i < fullLithograph.depths().length; i++) {
				if (!fullLithograph.exists().get(i)) {
					possibleTiles[elevationSize][defaultTile] += EMPTY_PRIORITY;
				} else {
					Biome biome = biomePalette.get(fullLithograph.biomes()[i]);
					possibleTiles[elevationSize][fullLithograph.biomes()[i]] += priorityForBiome(biomeRegistry, biome);
				}
			}
		} else {
			for (int i = 0; i < lowLithograph.depths().length; i++) {
				if (!lowLithograph.exists().get(i) || lowLithograph.depths()[i] > SEA_DEPTH) {
					Biome biome = biomePalette.get(fullLithograph.biomes()[i]);
					possibleTiles[elevationSize][fullLithograph.biomes()[i]] += priorityForBiome(biomeRegistry, biome);
				} else {
					Block block = blockPalette.get(lowLithograph.blocks()[i]);
					if (block == Blocks.LAVA) { // Lava Sea
						possibleTiles[elevationSize][biomeCount + CUSTOM_TILES.indexOf(FeatureTiles.TILE_LAVA)] += LAVA_PRIORITY;
					} else { // Low Floor
						possibleTiles[elevationSize][biomeCount + CUSTOM_TILES.indexOf(FeatureTiles.TILE_LAVA_SHORE)] += BEACH_PRIORITY;
					}
				}
			}
		}

		return frequencyToTexture(possibleTiles, biomeRegistry, biomePalette);
	}
}
