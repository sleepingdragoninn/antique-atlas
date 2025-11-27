package folk.sisby.antique_atlas.reloader;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import folk.sisby.antique_atlas.AntiqueAtlas;
import folk.sisby.antique_atlas.TileTexture;
import folk.sisby.antique_atlas.util.CodecUtil;
import net.fabricmc.fabric.api.resource.IdentifiableResourceReloadListener;
import net.minecraft.resource.Resource;
import net.minecraft.resource.ResourceManager;
import net.minecraft.resource.SinglePreparationResourceReloader;
import net.minecraft.resource.metadata.ResourceMetadata;
import net.minecraft.resource.metadata.ResourceMetadataReader;
import net.minecraft.util.Identifier;
import net.minecraft.util.dynamic.Codecs;
import net.minecraft.util.profiler.Profiler;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class TileTextures extends SinglePreparationResourceReloader<Map<Identifier, TileTextures.TileTextureMeta>> implements IdentifiableResourceReloadListener {
	private static final TileTextures INSTANCE = new TileTextures();
	public static final Identifier ID = AntiqueAtlas.id("tile_textures");

	public static TileTextures getInstance() {
		return INSTANCE;
	}

	private final Map<Identifier, TileTexture> textures = new HashMap<>();

	public Map<Identifier, TileTexture> getTextures() {
		return textures;
	}

	@Override
	protected Map<Identifier, TileTextures.TileTextureMeta> prepare(ResourceManager manager, Profiler profiler) {
		Map<Identifier, TileTextureMeta> textureMeta = new HashMap<>();
		for (Map.Entry<Identifier, Resource> e : manager.findResources("textures/atlas/tile", id -> id.getPath().endsWith(".png")).entrySet()) {
			Identifier id = Identifier.of(e.getKey().getNamespace(), e.getKey().getPath().substring("textures/atlas/tile/".length(), e.getKey().getPath().length() - ".png".length()));
			try {
				ResourceMetadata metadata = e.getValue().getMetadata();
				metadata.decode(TileTextureMeta.METADATA).ifPresentOrElse(meta -> {
					textureMeta.put(id, meta);
				}, () -> {
					AntiqueAtlas.LOGGER.info("[Antique Atlas] Metadata not present for {} - using defaults.", e.getKey());
					textureMeta.put(id, TileTextureMeta.DEFAULT);
				});
			} catch (IOException ex) {
				AntiqueAtlas.LOGGER.error("[Antique Atlas] Failed to access tile texture metadata for {}", e.getKey(), ex);
				textureMeta.put(id, TileTextureMeta.DEFAULT);
			}
		}
		return textureMeta;
	}

	@Override
	protected void apply(Map<Identifier, TileTextureMeta> prepared, ResourceManager manager, Profiler profiler) {
		AntiqueAtlas.LOGGER.info("[Antique Atlas] Reloading Tile Textures...");
		// Validate IDs
		prepared.forEach((id, meta) -> meta.warnMissing(id, prepared.keySet()));

		// Validate Parents
		Map<Identifier, Identifier> invalidParents = new HashMap<>();
		prepared.forEach((id, meta) -> {
			if (meta.parent.isPresent() && !prepared.containsKey(meta.parent.orElseThrow())) {
				invalidParents.put(id, meta.parent.orElseThrow());
				AntiqueAtlas.LOGGER.error("[Antique Atlas] Failed to reload a tile texture! {} had invalid parent {}", id, meta.parent);
			}
		});
		invalidParents.keySet().forEach(prepared::remove);

		// Propagate fields to children
		prepared.forEach((id, meta) -> {
			Optional<TileTextureMeta> parent = meta.parent.map(prepared::get);
			while (parent.isPresent()) {
				meta.inheritFromAncestor(parent.orElseThrow());
				parent = parent.orElseThrow().parent.map(prepared::get);
			}
		});

		// Populate Tags
		Map<Identifier, Set<Identifier>> textureTags = new HashMap<>();
		prepared.forEach((id, meta) -> meta.tags.forEach(tag -> textureTags.computeIfAbsent(tag, t -> new HashSet<>()).add(id)));

		// Substitute Tags
		prepared.forEach((id, meta) -> meta.substituteTags(id, textureTags));

		// Apply TilesToThis
		prepared.forEach((id, meta) -> meta.applyTilesToThis(id, prepared));

		// Create Builders
		Map<Identifier, TileTexture.Builder> textureBuilders = new HashMap<>();
		prepared.forEach((id, meta) -> textureBuilders.put(id, meta.toBuilder(id)));

		// Create Empty Textures
		textures.clear();
		textureBuilders.forEach((id, builder) -> textures.put(id, TileTexture.empty(id, builder.innerBorder())));

		// Build Textures
		textureBuilders.forEach((id, builder) -> builder.build(textures));
	}

	@Override
	public Identifier getFabricId() {
		return ID;
	}

	public static final class TileTextureMeta {
		public static final TileTextureMeta DEFAULT = new TileTextureMeta(Optional.empty(), Optional.empty(), Set.of(), Set.of(), Set.of(), Set.of(), Set.of(), Set.of(), Set.of());

		public static final Codec<TileTextureMeta> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			Identifier.CODEC.optionalFieldOf("parent").forGetter(TileTextureMeta::parent),
			CodecUtil.ofEnum(BorderType.class).optionalFieldOf("borderType").forGetter(TileTextureMeta::borderType),
			CodecUtil.set(Identifier.CODEC).fieldOf("tags").orElseGet(HashSet::new).forGetter(TileTextureMeta::tags),
			CodecUtil.set(Codecs.TAG_ENTRY_ID).fieldOf("tilesTo").orElseGet(HashSet::new).forGetter(TileTextureMeta::tilesTo),
			CodecUtil.set(Codecs.TAG_ENTRY_ID).fieldOf("tilesToHorizontal").orElseGet(HashSet::new).forGetter(TileTextureMeta::tilesToHorizontal),
			CodecUtil.set(Codecs.TAG_ENTRY_ID).fieldOf("tilesToVertical").orElseGet(HashSet::new).forGetter(TileTextureMeta::tilesToVertical),
			CodecUtil.set(Codecs.TAG_ENTRY_ID).fieldOf("tilesToThis").orElseGet(HashSet::new).forGetter(TileTextureMeta::tilesToThis),
			CodecUtil.set(Codecs.TAG_ENTRY_ID).fieldOf("tilesToThisHorizontal").orElseGet(HashSet::new).forGetter(TileTextureMeta::tilesToThisHorizontal),
			CodecUtil.set(Codecs.TAG_ENTRY_ID).fieldOf("tilesToThisVertical").orElseGet(HashSet::new).forGetter(TileTextureMeta::tilesToThisVertical)
		).apply(instance, TileTextureMeta::new));

		public enum BorderType {
			OUTER, INNER
		}

		public static final ResourceMetadataReader<TileTextureMeta> METADATA = new CodecUtil.CodecResourceMetadataSerializer<>(CODEC, AntiqueAtlas.id("tiling"));
		private final Optional<Identifier> parent;
		private Optional<BorderType> borderType;
		private final Set<Identifier> tags;
		private final Set<Codecs.TagEntryId> tilesTo;
		private final Set<Codecs.TagEntryId> tilesToHorizontal;
		private final Set<Codecs.TagEntryId> tilesToVertical;
		private final Set<Codecs.TagEntryId> tilesToThis;
		private final Set<Codecs.TagEntryId> tilesToThisHorizontal;
		private final Set<Codecs.TagEntryId> tilesToThisVertical;

		public TileTextureMeta(Optional<Identifier> parent, Optional<BorderType> borderType, Set<Identifier> tags, Set<Codecs.TagEntryId> tilesTo, Set<Codecs.TagEntryId> tilesToHorizontal, Set<Codecs.TagEntryId> tilesToVertical, Set<Codecs.TagEntryId> tilesToThis, Set<Codecs.TagEntryId> tilesToThisHorizontal, Set<Codecs.TagEntryId> tilesToThisVertical) {
			this.parent = parent;
			this.borderType = borderType;
			this.tags = tags;
			this.tilesTo = tilesTo;
			this.tilesToHorizontal = tilesToHorizontal;
			this.tilesToVertical = tilesToVertical;
			this.tilesToThis = tilesToThis;
			this.tilesToThisHorizontal = tilesToThisHorizontal;
			this.tilesToThisVertical = tilesToThisVertical;
		}

		public void warnMissing(Identifier thisId, Set<Identifier> identifiers) {
			for (Set<Codecs.TagEntryId> entrySet : List.of(tilesTo, tilesToHorizontal, tilesToVertical, tilesToThis, tilesToThisHorizontal, tilesToThisVertical)) {
				for (Codecs.TagEntryId entry : entrySet) {
					if (!entry.tag() && !identifiers.contains(entry.id())) {
						AntiqueAtlas.LOGGER.warn("[Antique Atlas] Tile texture {} references texture {}, which is missing!", thisId, entry.id());
					}
				}
			}
		}

		void inheritFromAncestor(TileTextureMeta other) {
			if (other.borderType.isPresent()) borderType = other.borderType;
			tags.addAll(other.tags);
			tilesTo.addAll(other.tilesTo);
			tilesToHorizontal.addAll(other.tilesToHorizontal);
			tilesToVertical.addAll(other.tilesToVertical);
			tilesToThis.addAll(other.tilesToThis);
			tilesToThisHorizontal.addAll(other.tilesToThisHorizontal);
			tilesToThisVertical.addAll(other.tilesToThisVertical);
		}

		void substituteTags(Identifier thisId, Map<Identifier, Set<Identifier>> tags) {
			for (Set<Codecs.TagEntryId> entrySet : List.of(tilesTo, tilesToHorizontal, tilesToVertical, tilesToThis, tilesToThisHorizontal, tilesToThisVertical)) {
				Set<Codecs.TagEntryId> entryTags = new HashSet<>();
				for (Codecs.TagEntryId entry : entrySet) {
					if (entry.tag()) {
						entryTags.add(entry);
					}
				}
				if (!entryTags.isEmpty()) entrySet.removeAll(entryTags);
				for (Codecs.TagEntryId entry : entryTags) {
					Set<Identifier> resolvedIds = tags.getOrDefault(entry.id(), Set.of());
					if (resolvedIds.isEmpty()) {
						AntiqueAtlas.LOGGER.warn("[Antique Atlas] Tile texture {} references tag {}, which is empty", thisId, entry.id());
					} else {
						entrySet.addAll(resolvedIds.stream().map(id -> new Codecs.TagEntryId(id, false)).toList());
					}
				}
			}
		}

		void applyTilesToThis(Identifier thisId, Map<Identifier, TileTextureMeta> map) {
			for (Codecs.TagEntryId entryId : tilesToThis) {
				if (entryId.tag()) throw new IllegalStateException("tags must be resolved to apply tilesToThis!");
				if (map.containsKey(entryId.id())) {
					map.get(entryId.id()).tilesTo.add(new Codecs.TagEntryId(thisId, false));
				} else {
					AntiqueAtlas.LOGGER.warn("[Antique Atlas] Tile texture {} references texture {}, which is missing", thisId, entryId.id());
				}
			}
			for (Codecs.TagEntryId entryId : tilesToThisHorizontal) {
				if (entryId.tag()) throw new IllegalStateException("tags must be resolved to apply tilesToThis!");
				if (map.containsKey(entryId.id())) {
					map.get(entryId.id()).tilesToHorizontal.add(new Codecs.TagEntryId(thisId, false));
				} else {
					AntiqueAtlas.LOGGER.warn("[Antique Atlas] Tile texture {} references texture {}, which is missing", thisId, entryId.id());
				}
			}
			for (Codecs.TagEntryId entryId : tilesToThisVertical) {
				if (entryId.tag()) throw new IllegalStateException("tags must be resolved to apply tilesToThis!");
				if (map.containsKey(entryId.id())) {
					map.get(entryId.id()).tilesToVertical.add(new Codecs.TagEntryId(thisId, false));
				} else {
					AntiqueAtlas.LOGGER.warn("[Antique Atlas] Tile texture {} references texture {}, which is missing", thisId, entryId.id());
				}
			}
		}

		public TileTexture.Builder toBuilder(Identifier thisId) {
			return new TileTexture.Builder(thisId, borderType.orElse(BorderType.OUTER) == BorderType.INNER, tilesTo.stream().map(Codecs.TagEntryId::id).collect(Collectors.toSet()), tilesToHorizontal.stream().map(Codecs.TagEntryId::id).collect(Collectors.toSet()), tilesToVertical.stream().map(Codecs.TagEntryId::id).collect(Collectors.toSet()));
		}

		public Optional<Identifier> parent() {
			return parent;
		}

		public Optional<BorderType> borderType() {
			return borderType;
		}

		public Set<Identifier> tags() {
			return tags;
		}

		public Set<Codecs.TagEntryId> tilesTo() {
			return tilesTo;
		}

		public Set<Codecs.TagEntryId> tilesToHorizontal() {
			return tilesToHorizontal;
		}

		public Set<Codecs.TagEntryId> tilesToVertical() {
			return tilesToVertical;
		}

		public Set<Codecs.TagEntryId> tilesToThis() {
			return tilesToThis;
		}

		public Set<Codecs.TagEntryId> tilesToThisHorizontal() {
			return tilesToThisHorizontal;
		}

		public Set<Codecs.TagEntryId> tilesToThisVertical() {
			return tilesToThisVertical;
		}
	}
}
