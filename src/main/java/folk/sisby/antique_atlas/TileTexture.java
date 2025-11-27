package folk.sisby.antique_atlas;

import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;
import net.minecraft.util.Identifier;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public final class TileTexture {
	public static TileTexture empty(Identifier id, boolean innerBorder) {
		return new TileTexture(Identifier.of(id.getNamespace(), "textures/atlas/tile/%s.png".formatted(id.getPath())), innerBorder, new ReferenceOpenHashSet<>(), new ReferenceOpenHashSet<>(), new ReferenceOpenHashSet<>());
	}

	public static final TileTexture DEFAULT = empty(AntiqueAtlas.id(AntiqueAtlas.CONFIG.fallbackFailHandling == AntiqueAtlasConfig.FallbackHandling.TEST ? "test" : "missing"), false);
	private final Identifier id;
	private final boolean innerBorder;
	private final Set<TileTexture> tilesTo;
	private final Set<TileTexture> tilesToHorizontal;
	private final Set<TileTexture> tilesToVertical;

	public TileTexture(Identifier id, boolean innerBorder, Set<TileTexture> tilesTo, Set<TileTexture> tilesToHorizontal, Set<TileTexture> tilesToVertical) {
		this.id = id;
		this.innerBorder = innerBorder;
		this.tilesTo = tilesTo;
		this.tilesToHorizontal = tilesToHorizontal;
		this.tilesToVertical = tilesToVertical;
	}

	public String displayId() {
		String trimmedPath = id.getPath().substring("textures/atlas/tile/".length(), id.getPath().length() - 4);
		return id.getNamespace().equals(AntiqueAtlas.ID) ? trimmedPath : Identifier.of(id.getNamespace(), trimmedPath).toString();
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

	public Identifier id() {
		return id;
	}

	public record Builder(Identifier id, boolean innerBorder, Set<Identifier> tilesTo, Set<Identifier> tilesToHorizontal, Set<Identifier> tilesToVertical) {
		public TileTexture build(Map<Identifier, TileTexture> emptyTextures) {
			TileTexture emptyTexture = emptyTextures.get(id);
			if (!tilesTo.isEmpty()) emptyTexture.tilesTo.addAll(tilesTo.stream().map(emptyTextures::get).collect(Collectors.toSet()));
			if (!tilesToHorizontal.isEmpty()) emptyTexture.tilesToHorizontal.addAll(tilesToHorizontal.stream().map(emptyTextures::get).collect(Collectors.toSet()));
			if (!tilesToVertical.isEmpty()) emptyTexture.tilesToVertical.addAll(tilesToVertical.stream().map(emptyTextures::get).collect(Collectors.toSet()));
			return emptyTexture;
		}
	}
}
