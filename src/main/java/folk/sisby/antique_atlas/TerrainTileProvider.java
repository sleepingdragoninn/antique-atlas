package folk.sisby.antique_atlas;

import net.minecraft.util.Identifier;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.MathHelper;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public record TerrainTileProvider(Identifier id, Map<TileElevation, List<TileTexture>> textures, @Nullable Map<Identifier, List<TileTexture>> overrides) {
	public static final TerrainTileProvider DEFAULT = new TerrainTileProvider(AntiqueAtlas.id("test"), List.of(TileTexture.DEFAULT), null);

	public TerrainTileProvider(Identifier id, List<TileTexture> textures) {
		this(id, textures, null);
	}

	public TerrainTileProvider(Identifier id, List<TileTexture> textures, @Nullable Map<Identifier, List<TileTexture>> overrides) {
		this(id, Arrays.stream(TileElevation.values()).collect(Collectors.toMap(e -> e, e -> textures)), overrides);
	}

	public TileTexture getTexture(ChunkPos pos, @Nullable TileElevation elevation) {
		return getTexture(pos, elevation, null);
	}

	public TileTexture getTexture(ChunkPos pos, @Nullable TileElevation elevation, @Nullable Identifier override) {
		int variation = (int) (MathHelper.hashCode(pos.x, pos.z, pos.x * pos.z) & 0x7FFFFFFF);
		if (override != null) {
			return overrides.get(override).get(variation % overrides.get(id).size());
		} else {
			TileElevation usedElevation = elevation == null ? TileElevation.VALLEY : elevation;
			return textures.get(usedElevation).get(variation % textures.get(usedElevation).size());
		}
	}
}
