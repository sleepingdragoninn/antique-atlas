package folk.sisby.antique_atlas.reloader;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import folk.sisby.antique_atlas.AntiqueAtlas;
import folk.sisby.antique_atlas.MarkerTexture;
import folk.sisby.antique_atlas.StructureTileProvider;
import folk.sisby.antique_atlas.TileTexture;
import folk.sisby.surveyor.landmark.Landmark;
import folk.sisby.surveyor.landmark.WorldLandmarks;
import folk.sisby.surveyor.landmark.component.LandmarkComponentTypes;
import folk.sisby.surveyor.structure.JigsawPieceSummary;
import folk.sisby.surveyor.structure.StructurePieceSummary;
import folk.sisby.surveyor.structure.StructureStartSummary;
import it.unimi.dsi.fastutil.Pair;
import net.fabricmc.fabric.api.resource.IdentifiableResourceReloadListener;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.resource.JsonDataLoader;
import net.minecraft.resource.ResourceManager;
import net.minecraft.structure.pool.StructurePoolElementType;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.profiler.Profiler;
import net.minecraft.world.World;
import net.minecraft.world.gen.structure.Structure;
import net.minecraft.world.gen.structure.StructureType;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static folk.sisby.antique_atlas.reloader.BiomeTileProviders.resolveTextureJson;

public class StructureTileProviders extends JsonDataLoader implements IdentifiableResourceReloadListener {
	public static final StructureTileProviders INSTANCE = new StructureTileProviders();

	public static final Identifier ID = AntiqueAtlas.id("structures");

	public static StructureTileProviders getInstance() {
		return INSTANCE;
	}

	protected final Map<Identifier, StructureTileProvider> startTiles = new HashMap<>();
	protected final Map<Identifier, StructureTileProvider> typeTiles = new HashMap<>();
	protected final Map<Identifier, StructureTileProvider> tagTiles = new HashMap<>();
	protected final Map<Identifier, StructureTileProvider> pieceTypeTiles = new HashMap<>();
	protected final Map<Identifier, StructureTileProvider> pieceJigsawSingleTiles = new HashMap<>();
	protected final Map<Identifier, StructureTileProvider> pieceJigsawFeatureTiles = new HashMap<>();
	protected final Map<Identifier, MarkerTexture> startMarkers = new HashMap<>();
	protected final Map<Identifier, MarkerTexture> typeMarkers = new HashMap<>();
	protected final Map<Identifier, MarkerTexture> tagMarkers = new HashMap<>();
	protected final Map<Identifier, MarkerTexture> pieceTypeMarkers = new HashMap<>();
	protected final Map<Identifier, MarkerTexture> pieceJigsawSingleMarkers = new HashMap<>();
	protected final Map<Identifier, MarkerTexture> pieceJigsawFeatureMarkers = new HashMap<>();

	public enum ProviderType {
		START("start"),
		TAG("tag"),
		TYPE("type"),
		PIECE_TYPE("piece/type"),
		JIGSAW_SINGLE("piece/jigsaw/single"),
		JIGSAW_FEATURE("piece/jigsaw/feature");

		public final String key;

		ProviderType(String key) {
			this.key = key;
		}

		public String prefix() {
			return key + "/";
		}

		public String translation(Identifier id) {
			return "structure.%s.%s".formatted(key.replace('/', '.'), id.toString().replace(':', '.'));
		}
	}

	protected final Map<ProviderType, Pair<Map<Identifier, StructureTileProvider>, Map<Identifier, MarkerTexture>>> PROVIDER_MAPS = Map.of(
		ProviderType.START, Pair.of(startTiles, startMarkers),
		ProviderType.TYPE, Pair.of(typeTiles, typeMarkers),
		ProviderType.TAG, Pair.of(tagTiles, tagMarkers),
		ProviderType.PIECE_TYPE, Pair.of(pieceTypeTiles, pieceTypeMarkers),
		ProviderType.JIGSAW_SINGLE, Pair.of(pieceJigsawSingleTiles, pieceJigsawSingleMarkers),
		ProviderType.JIGSAW_FEATURE, Pair.of(pieceJigsawFeatureTiles, pieceJigsawFeatureMarkers)
	);

	public StructureTileProviders() {
		super(new Gson(), "atlas/structure");
	}

	public void resolve(Map<ChunkPos, TileTexture> outTiles, Map<ChunkPos, StructureTileProvider> structureProviders, Map<ChunkPos, String> tilePredicates, StructurePieceSummary piece, World world) {
		if (piece instanceof JigsawPieceSummary jigsawPiece) {
			if (pieceJigsawSingleTiles.containsKey(jigsawPiece.getId())) {
				StructureTileProvider provider = (jigsawPiece.getElementType() == StructurePoolElementType.FEATURE_POOL_ELEMENT ? pieceJigsawFeatureTiles : pieceJigsawSingleTiles).get(jigsawPiece.getId());
				provider.getTextures(world, jigsawPiece.getBoundingBox(), jigsawPiece.getJunctions(), tilePredicates).forEach((pos, texture) -> {
					if (structureProviders.containsKey(pos) && structureProviders.get(pos).priority() < provider.priority()) return;
					outTiles.put(pos, texture);
					structureProviders.put(pos, provider);
				});
			}
		} else {
			Identifier pieceTypeId = Registries.STRUCTURE_PIECE.getId(piece.getType());
			if (pieceTypeTiles.containsKey(pieceTypeId)) {
				StructureTileProvider provider = pieceTypeTiles.get(pieceTypeId);
				provider.getTextures(world, piece.getBoundingBox(), tilePredicates).forEach((pos, texture) -> {
					if (structureProviders.containsKey(pos) && structureProviders.get(pos).priority() < provider.priority()) return;
					outTiles.put(pos, texture);
					structureProviders.put(pos, provider);
				});
			}
		}
	}

	public void resolve(Map<ChunkPos, TileTexture> outTiles, Map<ChunkPos, StructureTileProvider> structureProviders, Map<ChunkPos, String> debugPredicates, Map<Landmark, MarkerTexture> outMarkers, World world, RegistryKey<Structure> key, ChunkPos pos, StructureStartSummary summary, RegistryKey<StructureType<?>> type, Collection<TagKey<Structure>> tags) {
		if (startMarkers.containsKey(key.getValue())) {
			MarkerTexture texture = startMarkers.get(key.getValue());
			outMarkers.put(Landmark.create(WorldLandmarks.GLOBAL, key.getValue().withPath(p -> "start/" + p + "/" + pos.x + "/" + pos.z), b -> b
				.add(LandmarkComponentTypes.POS, pos.getCenterAtY(0))
				.add(LandmarkComponentTypes.NAME, Text.translatable(ProviderType.START.translation(key.getValue())))
			), texture);
		} else if (type != null && typeMarkers.containsKey(type.getValue())) {
			MarkerTexture texture = typeMarkers.get(type.getValue());
			outMarkers.put(Landmark.create(WorldLandmarks.GLOBAL, key.getValue().withPath(p -> "start/" + p + "/" + pos.x + "/" + pos.z), b -> b
				.add(LandmarkComponentTypes.POS, pos.getCenterAtY(0))
				.add(LandmarkComponentTypes.NAME, Text.translatable(ProviderType.TYPE.translation(type.getValue())))
			), texture);
		} else {
			tagMarkers.entrySet().stream().filter(entry -> tags.contains(TagKey.of(RegistryKeys.STRUCTURE, entry.getKey()))).findFirst().ifPresent(entry ->
				outMarkers.put(Landmark.create(WorldLandmarks.GLOBAL, key.getValue().withPath(p -> "start/" + p + "/" + pos.x + "/" + pos.z), b -> b
					.add(LandmarkComponentTypes.POS, pos.getCenterAtY(0))
					.add(LandmarkComponentTypes.NAME, Text.translatable(ProviderType.TAG.translation(entry.getKey())))
				), entry.getValue()));
		}

		if (startTiles.containsKey(key.getValue())) {
			StructureTileProvider provider = startTiles.get(key.getValue());
			provider.getTextures(world, summary.getBoundingBox(), debugPredicates).forEach((pos2, texture) -> {
				if (structureProviders.containsKey(pos) && structureProviders.get(pos).priority() < provider.priority()) return;
				outTiles.put(pos2, texture);
				structureProviders.put(pos2, provider);
			});
		} else if (type != null && typeTiles.containsKey(type.getValue())) {
			StructureTileProvider provider = typeTiles.get(key.getValue());
			provider.getTextures(world, summary.getBoundingBox(), debugPredicates).forEach((pos2, texture) -> {
				if (structureProviders.containsKey(pos) && structureProviders.get(pos).priority() < provider.priority()) return;
				outTiles.put(pos2, texture);
				structureProviders.put(pos2, provider);
			});
		} else {
			tags.stream().filter(t -> tagTiles.containsKey(t.id())).findFirst().ifPresent(tag -> {
				StructureTileProvider provider = tagTiles.get(tag.id());
				provider.getTextures(world, summary.getBoundingBox(), debugPredicates).forEach((pos2, texture) -> {
					if (structureProviders.containsKey(pos) && structureProviders.get(pos).priority() < provider.priority()) return;
					outTiles.put(pos2, texture);
					structureProviders.put(pos2, provider);
				});
			});
		}

		summary.getChildren().forEach(p -> resolve(outTiles, structureProviders, debugPredicates, p, world));
	}

	@Override
	protected void apply(Map<Identifier, JsonElement> prepared, ResourceManager manager, Profiler profiler) {
		AntiqueAtlas.LOGGER.info("[Antique Atlas] Reloading Structure Tile / Marker Providers...");
		Map<Identifier, TileTexture> textures = TileTextures.getInstance().getTextures();
		Set<TileTexture> unusedTextures = new HashSet<>(textures.values().stream().filter(t -> t.id().getPath().startsWith("structure")).toList());

		PROVIDER_MAPS.values().forEach(p -> p.left().clear());
		PROVIDER_MAPS.values().forEach(p -> p.right().clear());
		for (Map.Entry<Identifier, JsonElement> fileEntry : prepared.entrySet()) {
			Identifier fileId = fileEntry.getKey();
			PROVIDER_MAPS.forEach((providerType, pair) -> {
				Map<Identifier, StructureTileProvider> providerMap = pair.left();
				Map<Identifier, MarkerTexture> markerMap = pair.right();
				if (fileId.getPath().startsWith(providerType.prefix())) {
					Identifier id = Identifier.of(fileId.getNamespace(), fileId.getPath().substring(providerType.prefix().length()));
					try {
						JsonObject fileJson = fileEntry.getValue().getAsJsonObject();
						if (fileJson.has("textures")) {
							JsonElement textureJson = fileJson.get("textures");
							int priority = fileJson.has("priority") ? fileJson.get("priority").getAsInt() : 999;
							List<TileTexture> defaultTextures = resolveTextureJson(textures, textureJson);
							if (defaultTextures != null) {
								StructureTileProvider provider = new StructureTileProvider(id, priority, defaultTextures);
								providerMap.put(provider.id(), provider);
								unusedTextures.removeAll(provider.allTextures());
							} else {
								JsonObject textureObject = textureJson.getAsJsonObject();
								Map<StructureTileProvider.ChunkMatcher, List<TileTexture>> matchers = new HashMap<>();
								for (String matcherKey : textureObject.keySet()) {
									Identifier matcherId = AntiqueAtlas.id(matcherKey);
									StructureTileProvider.ChunkMatcher matcher = StructureTileProvider.getChunkMatcher(matcherId);
									if (matcher == null) throw new IllegalStateException("Matcher %s does not exist!".formatted(matcherId.toString()));
									List<TileTexture> matcherTextures = resolveTextureJson(textures, textureObject.get(matcherKey));
									if (matcherTextures == null) throw new IllegalStateException("Malformed object %s in textures object!".formatted(matcherId.toString()));
									matcherTextures.forEach(unusedTextures::remove);
									matchers.put(matcher, matcherTextures);
								}
								if (matchers.isEmpty()) {
									throw new IllegalStateException("No matcher keys were found in the textures object!");
								}
								StructureTileProvider provider = new StructureTileProvider(id, priority, matchers);
								providerMap.put(provider.id(), provider);
								unusedTextures.removeAll(provider.allTextures());
							}
						}
						if (fileJson.has("markers")) {
							JsonElement markerJson = fileJson.get("markers");
							Identifier markerTextureId = Identifier.tryParse(markerJson.getAsString());
							MarkerTexture texture = MarkerTextures.getInstance().asMap().get(markerTextureId);
							if (texture == null) throw new IllegalStateException("Marker texture %s does not exist!".formatted(markerTextureId));
							AntiqueAtlas.CONFIG.structureMarkers.putIfAbsent(fileId.toString(), true);
							if (AntiqueAtlas.CONFIG.structureMarkers.get(fileId.toString())) {
								markerMap.put(id, texture);
							}
						}
					} catch (Exception e) {
						AntiqueAtlas.LOGGER.error("[Antique Atlas] Error reading structure tile provider {}!", fileId, e);
					}
				}
			});
		}

		for (TileTexture texture : unusedTextures) {
			AntiqueAtlas.LOGGER.warn("[Antique Atlas] Tile texture {} isn't referenced by any structure tile provider!", texture.displayId());
		}
	}

	@Override
	public Identifier getFabricId() {
		return ID;
	}

	@Override
	public Collection<Identifier> getFabricDependencies() {
		return List.of(TileTextures.ID, MarkerTextures.ID);
	}
}
