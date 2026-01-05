package folk.sisby.antique_atlas;

import net.minecraft.util.Identifier;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.MathHelper;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public record TerrainTileProvider(Identifier id, Map<TileElevation, List<TileTexture>> textures) {
	public static TerrainTileProvider DEFAULT = new TerrainTileProvider(AntiqueAtlas.id("default"), List.of(TileTexture.DEFAULT));

	public TerrainTileProvider(Identifier id, List<TileTexture> textures) {
		this(id, Arrays.stream(TileElevation.values()).collect(Collectors.toMap(e -> e, e -> textures)));
	}

	public TileTexture getTexture(ChunkPos pos, @Nullable TileElevation elevation) {
		int variation = (int) (MathHelper.hashCode(pos.x, pos.z, pos.x * pos.z) & 0x7FFFFFFF);
		TileElevation usedElevation = elevation == null ? TileElevation.VALLEY : elevation;
		return textures.get(usedElevation).get(variation % textures.get(usedElevation).size());
	}
}
