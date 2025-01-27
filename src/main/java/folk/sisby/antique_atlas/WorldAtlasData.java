package folk.sisby.antique_atlas;

import com.google.common.collect.Multimap;
import folk.sisby.antique_atlas.reloader.BiomeTileProviders;
import folk.sisby.antique_atlas.reloader.MarkerTextures;
import folk.sisby.antique_atlas.reloader.StructureTileProviders;
import folk.sisby.antique_atlas.util.Rect;
import folk.sisby.surveyor.WorldSummary;
import folk.sisby.surveyor.client.SurveyorClient;
import folk.sisby.surveyor.landmark.Landmark;
import folk.sisby.surveyor.landmark.LandmarkType;
import folk.sisby.surveyor.landmark.PlayerDeathLandmark;
import folk.sisby.surveyor.landmark.SimplePointLandmark;
import folk.sisby.surveyor.landmark.WorldLandmarks;
import folk.sisby.surveyor.structure.WorldStructureSummary;
import folk.sisby.surveyor.terrain.WorldTerrainSummary;
import it.unimi.dsi.fastutil.Pair;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.registry.RegistryKey;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.DyeColor;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;
import net.minecraft.world.gen.structure.Structure;

import java.util.BitSet;
import java.util.Collection;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;

public class WorldAtlasData {
	public static final Map<RegistryKey<World>, WorldAtlasData> WORLDS = new HashMap<>();

	public static WorldAtlasData getOrCreate(World world) {
		return WorldAtlasData.WORLDS.computeIfAbsent(world.getRegistryKey(), k -> new WorldAtlasData());
	}

	public static void onLoad(World world, WorldSummary summary, ClientPlayerEntity player, Map<ChunkPos, BitSet> terrain, Multimap<RegistryKey<Structure>, ChunkPos> structures, Multimap<LandmarkType<?>, BlockPos> landmarks) {
		WorldAtlasData data = getOrCreate(world);
		data.onTerrainUpdated(world, summary.terrain(), WorldTerrainSummary.toKeys(terrain, player.getChunkPos()));
		data.onStructuresAdded(world, summary.structures(), structures);
		data.onLandmarksAdded(world, summary.landmarks(), landmarks);
		AntiqueAtlas.LOGGER.info("[Antique Atlas] Beginning to load terrain for {} - {} chunks available.", world.getRegistryKey().getValue(), data.terrainDeque.size());
	}

	private final Map<ChunkPos, TileTexture> biomeTiles = new HashMap<>();
	private final Map<ChunkPos, TileTexture> structureTiles = new HashMap<>();
	private final Map<LandmarkType<?>, Map<BlockPos, Pair<Landmark<?>, MarkerTexture>>> landmarkMarkers = new ConcurrentHashMap<>();
	private final Map<Landmark<?>, MarkerTexture> structureMarkers = new ConcurrentHashMap<>();

	private final Rect tileScope = new Rect(0, 0, 0, 0);
	private final Set<ChunkPos> terrainDequeHash = new HashSet<>();
	private final Deque<ChunkPos> terrainDeque = new ConcurrentLinkedDeque<>();
	boolean isFinished = false;

	// Debug Display Info
	private final Map<ChunkPos, String> debugBiomePredicates = new HashMap<>();
	private final Map<ChunkPos, String> debugStructurePredicates = new HashMap<>();
	private final Map<ChunkPos, TerrainTileProvider> debugBiomes = new HashMap<>();
	private final Map<ChunkPos, StructureTileProvider> debugStructures = new HashMap<>();

	public void onTerrainUpdated(World world, WorldTerrainSummary ignored2, Collection<ChunkPos> chunks) {
		for (ChunkPos pos : chunks) {
			if (!biomeTiles.containsKey(pos) && !terrainDequeHash.contains(pos)) {
				terrainDequeHash.add(pos);
				terrainDeque.add(pos);
			}
		}
	}

	public void onStructuresAdded(World world, WorldStructureSummary ws, Multimap<RegistryKey<Structure>, ChunkPos> summaries) {
		summaries.forEach((key, pos) -> StructureTileProviders.getInstance().resolve(structureTiles, debugStructures, debugStructurePredicates, structureMarkers, world, key, pos, ws.get(key, pos), ws.getType(key), ws.getTags(key)));
	}

	public void tick(World world) {
		if (!BiomeTileProviders.getInstance().hasFallbacks()) return;
		for (int i = 0; i < AntiqueAtlas.CONFIG.chunkTickLimit; i++) {
			ChunkPos pos = terrainDeque.pollFirst();
			terrainDequeHash.remove(pos);
			if (pos == null) break;
			Pair<TerrainTileProvider, TileElevation> tile = world.getRegistryKey() == World.NETHER ? TerrainTiling.terrainToTileNether(world, pos) : TerrainTiling.terrainToTile(world, pos);
			if (tile != null) {
				tileScope.extendTo(pos.x, pos.z);
				biomeTiles.put(pos, tile.left().getTexture(pos, tile.right()));
				debugBiomes.put(pos, tile.left());
				debugBiomePredicates.put(pos, tile.right() == null ? null : tile.right().getName());
			}
		}
		if (!isFinished && terrainDeque.isEmpty()) {
			isFinished = true;
			AntiqueAtlas.LOGGER.info("[Antique Atlas] Finished loading terrain for {} - {} tiles.", world.getRegistryKey().getValue(), biomeTiles.size());
		}
	}

	public Rect getScope() {
		return tileScope;
	}

	public TileTexture getTile(int x, int z) {
		return getTile(new ChunkPos(x, z));
	}

	public TileTexture getTile(ChunkPos pos) {
		if (!biomeTiles.containsKey(pos)) return null;
		return structureTiles.getOrDefault(pos, biomeTiles.get(pos));
	}

	public Identifier getProvider(ChunkPos pos) {
		if (structureTiles.containsKey(pos)) {
			return debugStructures.get(pos).id();
		} else {
			return debugBiomes.containsKey(pos) ? debugBiomes.get(pos).id() : null;
		}
	}

	public String getTilePredicate(ChunkPos pos) {
		if (structureTiles.containsKey(pos)) {
			return debugStructurePredicates.get(pos);
		} else {
			return debugBiomePredicates.get(pos);
		}
	}

	private void addLandmarkMarker(Landmark<?> landmark, MarkerTexture texture) {
		landmarkMarkers.computeIfAbsent(landmark.type(), t -> new ConcurrentHashMap<>()).put(landmark.pos(), Pair.of(landmark, texture));
	}

	private void addLandmark(Landmark<?> baseLandmark) {
		if (baseLandmark.type() == PlayerDeathLandmark.TYPE) {
			PlayerDeathLandmark landmark = (PlayerDeathLandmark) baseLandmark;

			AntiqueAtlasConfig.GraveStyle style = AntiqueAtlas.CONFIG.graveStyle;
			if (landmark.name() == null && style == AntiqueAtlasConfig.GraveStyle.CAUSE) style = AntiqueAtlasConfig.GraveStyle.DIED;
			MutableText timeText = Text.literal(String.valueOf(1 + (landmark.created() / 24000))).formatted(Formatting.WHITE);
			String key = "gui.antique_atlas.marker.death.%s".formatted(style.toString().toLowerCase());
			MutableText text = switch (style) {
				case CAUSE -> Text.translatable(key, landmark.name().copy().formatted(Formatting.GRAY).formatted(Formatting.RED), timeText).formatted(Formatting.GRAY);
				case GRAVE, ITEMS, DIED -> Text.translatable(key, Text.translatable("gui.antique_atlas.marker.death.%s.verb".formatted(style.toString().toLowerCase())).formatted(Formatting.RED), timeText).formatted(Formatting.GRAY);
				case EUPHEMISMS -> Text.translatable(key, Text.translatable("gui.antique_atlas.marker.death.%s.verb.%s".formatted(style.toString().toLowerCase(), new Random(landmark.seed()).nextInt(11))).formatted(Formatting.RED), timeText).formatted(Formatting.GRAY);
			};
			addLandmarkMarker(new PlayerDeathLandmark(landmark.pos(), landmark.owner(), text, landmark.created(), landmark.seed()), MarkerTextures.getInstance().getLandmarkType(landmark.type(), style == AntiqueAtlasConfig.GraveStyle.ITEMS ? "items" : null));
		} else {
			addLandmarkMarker(baseLandmark, MarkerTextures.getInstance().getOrDefault(baseLandmark.texture(), MarkerTextures.getInstance().getLandmarkType(baseLandmark.type())));
		}
	}

	public void onLandmarksAdded(World ignored, WorldLandmarks worldLandmarks, Multimap<LandmarkType<?>, BlockPos> landmarks) {
		landmarks.forEach((type, pos) -> this.addLandmark(worldLandmarks.get(type, pos)));
	}

	public void onLandmarksRemoved(World ignored, WorldLandmarks ignored2, Multimap<LandmarkType<?>, BlockPos> landmarks) {
		landmarks.forEach((type, pos) -> {
			if (landmarkMarkers.containsKey(type)) {
				landmarkMarkers.get(type).remove(pos);
				if (landmarkMarkers.get(type).isEmpty()) landmarkMarkers.remove(type);
			}
		});
	}

	public static boolean landmarkIsEditable(Landmark<?> landmark) {
		return landmark.owner() != null && SurveyorClient.getClientUuid().equals(landmark.owner());
	}

	public boolean deleteLandmark(World world, Landmark<?> landmark) {
		WorldLandmarks summary = WorldSummary.of(world).landmarks();
		if (summary == null || !landmarkIsEditable(landmark)) return false;
		summary.remove(world, landmark.type(), landmark.pos());
		return true;
	}

	public Map<Landmark<?>, MarkerTexture> getEditableLandmarks() {
		Map<Landmark<?>, MarkerTexture> map = new HashMap<>();
		landmarkMarkers.forEach((type, landmarks) -> landmarks.forEach((pos, pair) -> {
			if (landmarkIsEditable(pair.left())) map.put(pair.left(), pair.right());
		}));
		return map;
	}

	public Map<Landmark<?>, MarkerTexture> getAllMarkers(int tileChunks) {
		Map<Landmark<?>, MarkerTexture> map = new HashMap<>();
		landmarkMarkers.forEach((type, landmarks) -> landmarks.forEach((pos, pair) -> map.put(pair.left(), pair.right())));
		structureMarkers.forEach((landmark, texture) -> {
			if (tileChunks >= texture.nearClip() && tileChunks <= texture.farClip()) map.put(landmark, texture);
		});
		return map;
	}

	public MarkerTexture getMarkerTexture(Landmark<?> landmark) {
		return landmarkMarkers.containsKey(landmark.type()) && landmarkMarkers.get(landmark.type()).containsKey(landmark.pos()) ? landmarkMarkers.get(landmark.type()).get(landmark.pos()).right() : structureMarkers.get(landmark);
	}

	public void placeCustomMarker(World world, MarkerTexture selectedTexture, DyeColor color, MutableText label, BlockPos blockPos) {
		WorldLandmarks summary = WorldSummary.of(world).landmarks();
		if (summary == null) return;
		summary.put(world, new SimplePointLandmark(
			blockPos,
			SurveyorClient.getClientUuid(),
			color,
			label,
			null,
			selectedTexture.keyId()
		));
	}
}
