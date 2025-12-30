package folk.sisby.antique_atlas;

import folk.sisby.kaleido.api.WrappedConfig;
import folk.sisby.kaleido.lib.quiltconfig.api.annotations.Comment;
import folk.sisby.kaleido.lib.quiltconfig.api.annotations.IntegerRange;
import folk.sisby.kaleido.lib.quiltconfig.api.values.ValueList;
import folk.sisby.kaleido.lib.quiltconfig.api.values.ValueMap;
import folk.sisby.surveyor.client.SurveyorClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.registry.RegistryKey;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@SuppressWarnings("CanBeFinal")
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

	public enum EmptyHandling {
		CLOUDS,
		EMPTY
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

	@Comment("How to display areas that aren't explored yet")
	public EmptyHandling emptyHandling = EmptyHandling.EMPTY;

	public Map<String, Boolean> structureMarkers = ValueMap.builder(true)
		.put("minecraft:type/end_city", false)
		.build();

	@Comment("Options to adjust map behaviour for custom or modified dimensions.")
	public Dimensions dimensions = new Dimensions();

	public static class Dimensions implements Section {
		@Comment("Cycle order and coordinate scales of each dimension.")
		@Comment("If not 0, the relative position of the player will be shown.")
		public Map<String, Integer> scales = ValueMap.builder(0)
			.put("minecraft:overworld", 8)
			.put("minecraft:the_nether", 1)
			.put("minecraft:the_end", 0)
			.build();

		public List<RegistryKey<World>> getOrder(ClientPlayNetworkHandler handler) {
			List<RegistryKey<World>> dims = new ArrayList<>(SurveyorClient.getSummaries(handler).keySet().stream().sorted(Comparator.comparing(RegistryKey::toString)).toList());
			dims.removeIf(WorldAtlasData::isEmpty);
			scales.keySet().removeIf(v -> handler.getWorldKeys().stream().noneMatch(d -> d.getValue().toString().equals(v)));
			dims.stream().filter(dim -> !scales.containsKey(dim.getValue().toString())).forEach(dim -> scales.put(dim.getValue().toString(), 0));
			return dims.stream().sorted(Comparator.comparing(dim -> scales.keySet().stream().toList().indexOf(dim.getValue().toString()))).toList();
		}

		public Map<RegistryKey<World>, Integer> getScales(ClientPlayNetworkHandler handler) {
			List<RegistryKey<World>> dims = new ArrayList<>(SurveyorClient.getSummaries(handler).keySet().stream().sorted(Comparator.comparing(RegistryKey::toString)).toList());
			dims.removeIf(WorldAtlasData::isEmpty);
			scales.keySet().removeIf(v -> handler.getWorldKeys().stream().noneMatch(d -> d.getValue().toString().equals(v)));
			dims.stream().filter(dim -> !scales.containsKey(dim.getValue().toString())).forEach(dim -> scales.put(dim.getValue().toString(), 0));
			return dims.stream().collect(Collectors.toMap(k -> k, k -> scales.get(k.getValue().toString())));
		}
	}
}
