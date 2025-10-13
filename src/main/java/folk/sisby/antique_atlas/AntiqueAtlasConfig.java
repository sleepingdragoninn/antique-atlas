package folk.sisby.antique_atlas;

import folk.sisby.kaleido.api.WrappedConfig;
import folk.sisby.kaleido.lib.quiltconfig.api.annotations.Comment;
import folk.sisby.kaleido.lib.quiltconfig.api.annotations.IntegerRange;
import folk.sisby.kaleido.lib.quiltconfig.api.values.ValueMap;

import java.util.Map;

public class AntiqueAtlasConfig extends WrappedConfig {
	public enum GraveStyle {
		CAUSE,
		GRAVE,
		ITEMS,
		DIED,
		EUPHEMISMS
	}

	public enum FallbackHandling {
		TEST,
		MISSING,
		CRASH
	}

	@Comment("Whether to display the map in full-screen")
	@Comment("The background is slightly less stylish, but more tiles are shown at once")
	public boolean fullscreen = true;

	@Comment("Whether to require an item to display the map")
	public boolean requireItem = false;

	@Comment("Whether to keep scale after closing the map")
	public boolean keepZoom = false;

	@Comment("Whether to keep offset after closing the map")
	public boolean keepOffset = false;

	@Comment("How to depict player death locations.")
	public GraveStyle graveStyle = GraveStyle.EUPHEMISMS;

	@Comment("The maximum number of chunks to represent as a tile, as a power of 2")
	@Comment("Effectively the 'minimum zoom'")
	@Comment("0: 1x1 chunk = 1 tile | 6: 64x64 chunks = 1 tile")
	@IntegerRange(min = 0, max = 6)
	public int maxTileChunks = 5;

	@Comment("The maximum size to render a tile at, as a power of 2 multiplier")
	@Comment("Effectively the 'maximum zoom'")
	@Comment("0: 1 tile = 16x16 | 3: 1 tile = 128x128")
	@IntegerRange(min = 0, max = 3)
	public int maxTilePixels = 1;

	@Comment("The effective GUI scale for tiles and markers - independent of the overall GUI scale.")
	@Comment("0 will match your GUI scale - pixels will be the same size as the background & buttons")
	@Comment("-1 will use half your GUI scale, rounding up.")
	@Comment("-2 will use half your GUI scale, rounding down.")
	@IntegerRange(min = -2, max = 10)
	public int mapScale = 0;

	@Comment("The maximum number of chunks to load onto the map per tick after entering a world")
	public int chunkTickLimit = 100;

	@Comment("How to handle biomes that aren't in any minecraft, conventional, or forge biome tags")
	public FallbackHandling fallbackFailHandling = FallbackHandling.MISSING;

	@Comment("Disables fading when Marker Icons approach the edge of the Atlas.")
	@Comment("Needed for shader support, pair with Shader Patch resource pack.")
	public boolean shaderCompat = false;

	@Comment("Whether to show debug information about hovered tiles and markers")
	public boolean debugRender = false;

	public Map<String, Boolean> structureMarkers = ValueMap.builder(true)
		.put("minecraft:type/end_city", false)
		.build();
}
