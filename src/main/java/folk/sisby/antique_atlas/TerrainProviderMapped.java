package folk.sisby.antique_atlas;

import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;

public record TerrainProviderMapped(TerrainTileProvider provider, @Nullable TileElevation elevation, @Nullable Identifier override) {
}
