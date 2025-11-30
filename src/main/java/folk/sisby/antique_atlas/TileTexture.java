package folk.sisby.antique_atlas;

import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;
import net.minecraft.util.Identifier;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public record TileTexture(Identifier id, boolean innerBorder, Set<TileTexture> tilesTo, Set<TileTexture> tilesToHorizontal, Set<TileTexture> tilesToVertical) {
	public static TileTexture empty(Identifier id, boolean innerBorder) {
		return new TileTexture(Identifier.of(id.getNamespace(), "textures/atlas/tile/%s.png".formatted(id.getPath())), innerBorder, new ReferenceOpenHashSet<>(), new ReferenceOpenHashSet<>(), new ReferenceOpenHashSet<>());
	}

	public static final TileTexture DEFAULT = empty(AntiqueAtlas.id(AntiqueAtlas.CONFIG.fallbackFailHandling == AntiqueAtlasConfig.FallbackHandling.TEST ? "test" : "missing"), false);

	public String displayId() {
		Identifier trimmed = id.withPath(p -> p.substring("textures/atlas/tile/".length(), id.getPath().length() - 4));
		return id.getNamespace().equals(AntiqueAtlas.ID) ? trimmed.getPath() : trimmed.toString();
	}

	public boolean tiles(TileTexture other) {
		return this == other || (innerBorder ^ (tilesTo.contains(other) || tilesToHorizontal.contains(other) || tilesToVertical.contains(other)));
	}

	public boolean tilesHorizontally(TileTexture other) {
		return this == other || (innerBorder ^ (tilesTo.contains(other) || tilesToHorizontal.contains(other)));
	}

	public boolean tilesVertically(TileTexture other) {
		return this == other || (innerBorder ^ (tilesTo.contains(other) || tilesToVertical.contains(other)));
	}

	public record Builder(Identifier id, boolean innerBorder, Set<Identifier> tilesTo, Set<Identifier> tilesToHorizontal, Set<Identifier> tilesToVertical) {
		public void build(Map<Identifier, TileTexture> emptyTextures) {
			if (!tilesTo.isEmpty()) emptyTextures.get(id).tilesTo.addAll(tilesTo.stream().map(emptyTextures::get).collect(Collectors.toSet()));
			if (!tilesToHorizontal.isEmpty()) emptyTextures.get(id).tilesToHorizontal.addAll(tilesToHorizontal.stream().map(emptyTextures::get).collect(Collectors.toSet()));
			if (!tilesToVertical.isEmpty()) emptyTextures.get(id).tilesToVertical.addAll(tilesToVertical.stream().map(emptyTextures::get).collect(Collectors.toSet()));
		}
	}
}
