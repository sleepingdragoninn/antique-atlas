package folk.sisby.antique_atlas;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import folk.sisby.surveyor.WorldSummary;
import net.minecraft.structure.JigsawJunction;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.MathHelper;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;

public record StructureTileProvider(Identifier id, int priority, Map<ChunkMatcher, List<TileTexture>> matchers) {
	public static final BiMap<Identifier, ChunkMatcher> CHUNK_MATCHERS = HashBiMap.create();
	private static final int SEA_LEVEL = 63;

	public static ChunkMatcher getChunkMatcher(Identifier id) {
		return CHUNK_MATCHERS.get(id);
	}

	public StructureTileProvider(Identifier id, int priority, List<TileTexture> textures) {
		this(id, priority, Map.of(ChunkMatcher::center, textures));
	}

	public StructureTileProvider(Identifier id, int priority, ChunkMatcher matcher, List<TileTexture> textures) {
		this(id, priority, Map.of(matcher, textures));
	}

	public Map<ChunkPos, TileTexture> getTextures(Function<ChunkMatcher, Collection<ChunkPos>> matcherFunction, Map<ChunkPos, String> tilePredicates) {
		Map<ChunkPos, TileTexture> outMap = new HashMap<>();
		matchers.forEach((matcher, textures) -> {
			for (ChunkPos pos : matcherFunction.apply(matcher)) {
				tilePredicates.put(pos, Objects.toString(CHUNK_MATCHERS.inverse().get(matcher), null));
				int variation = (int) (MathHelper.hashCode(pos.x, pos.z, pos.x * pos.z) & 0x7FFFFFFF);
				outMap.put(pos, textures.get(variation % textures.size()));
			}
		});
		return outMap;
	}

	public Map<ChunkPos, TileTexture> getTextures(WorldSummary summary, BlockBox box, Map<ChunkPos, String> tilePredicates) {
		return getTextures(matcher -> matcher.matches(summary, box), tilePredicates);
	}

	public Map<ChunkPos, TileTexture> getTextures(WorldSummary summary, BlockBox box, List<JigsawJunction> junctions, Map<ChunkPos, String> tilePredicates) {
		return getTextures(matcher -> matcher.matches(summary, box, junctions), tilePredicates);
	}

	public Set<TileTexture> allTextures() {
		Set<TileTexture> outSet = new HashSet<>();
		matchers.values().forEach(outSet::addAll);
		return outSet;
	}

	public interface ChunkMatcher {
		Collection<ChunkPos> matches(WorldSummary summary, BlockBox box);

		default Collection<ChunkPos> matches(WorldSummary summary, BlockBox box, List<JigsawJunction> junctions) {
			return matches(summary, box);
		}

		static Collection<ChunkPos> center(WorldSummary summary, BlockBox box) {
			return Collections.singleton(new ChunkPos(box.getCenter()));
		}

		static Collection<ChunkPos> topAboveGround(WorldSummary summary, BlockBox box) {
			if (SEA_LEVEL <= box.getMaxY()) {
				return Collections.singleton(new ChunkPos(box.getCenter()));
			}

			return Collections.emptyList();
		}

		static Collection<ChunkPos> aboveGround(WorldSummary summary, BlockBox box) {
			BlockPos center = new BlockPos(box.getCenter());
			if (SEA_LEVEL - 4 <= center.getY()) {
				return Collections.singleton(new ChunkPos(center));
			}

			return Collections.emptyList();
		}

		static Collection<ChunkPos> bridgeHorizontal(WorldSummary summary, BlockBox box) {
			HashSet<ChunkPos> matches = new HashSet<>();

			if (box.getBlockCountX() > 16) {
				int chunkZ = box.getCenter().getZ() >> 4;
				for (int x = box.getMinX(); x < box.getMaxX(); x += 16) {
					matches.add(new ChunkPos(x >> 4, chunkZ));
				}
			}

			return matches;
		}

		static Collection<ChunkPos> bridgeVertical(WorldSummary summary, BlockBox box) {
			HashSet<ChunkPos> matches = new HashSet<>();

			if (box.getBlockCountZ() > 16) {
				int chunkX = box.getCenter().getX() >> 4;
				for (int z = box.getMinZ(); z < box.getMaxZ(); z += 16) {
					matches.add(new ChunkPos(chunkX, z >> 4));
				}
			}

			return matches;
		}

		static Collection<ChunkPos> centerIfHorizontal(WorldSummary summary, BlockBox box) {
			if (box.getBlockCountX() > box.getBlockCountZ()) {
				return Collections.singleton(new ChunkPos(box.getCenter()));
			} else {
				return Collections.emptySet();
			}
		}

		static Collection<ChunkPos> centerIfVertical(WorldSummary summary, BlockBox box) {
			if (box.getBlockCountZ() > box.getBlockCountX()) {
				return Collections.singleton(new ChunkPos(box.getCenter()));
			} else {
				return Collections.emptySet();
			}
		}
	}

	public interface ChunkJunctionMatcher extends ChunkMatcher {
		default Collection<ChunkPos> matches(WorldSummary summary, BlockBox box) {
			return matches(summary, box, List.of());
		}

		Collection<ChunkPos> matches(WorldSummary summary, BlockBox box, List<JigsawJunction> junctions);

		static Collection<ChunkPos> straightHorizontal(WorldSummary summary, BlockBox box, List<JigsawJunction> junctions) {
			if (junctions.size() == 2 && (junctions.get(0).getSourceZ() == junctions.get(1).getSourceZ() || junctions.get(0).getSourceX() != junctions.get(1).getSourceX())) {
				return List.of(new ChunkPos(box.getCenter()));
			}
			return List.of();
		}

		static Collection<ChunkPos> straightVertical(WorldSummary summary, BlockBox box, List<JigsawJunction> junctions) {
			if (junctions.size() == 2 && (junctions.get(0).getSourceX() == junctions.get(1).getSourceX() || junctions.get(0).getSourceZ() != junctions.get(1).getSourceZ())) {
				return List.of(new ChunkPos(new BlockPos((junctions.get(0).getSourceX() + junctions.get(1).getSourceX()) / 2, 0, (junctions.get(0).getSourceZ() + junctions.get(1).getSourceZ()) / 2)));
			}
			return List.of();
		}
	}

	static {
		CHUNK_MATCHERS.put(AntiqueAtlas.id("center"), ChunkMatcher::center);
		CHUNK_MATCHERS.put(AntiqueAtlas.id("center_above_ground"), ChunkMatcher::aboveGround);
		CHUNK_MATCHERS.put(AntiqueAtlas.id("center_top_above_ground"), ChunkMatcher::topAboveGround);
		CHUNK_MATCHERS.put(AntiqueAtlas.id("center_horizontal"), ChunkMatcher::centerIfHorizontal);
		CHUNK_MATCHERS.put(AntiqueAtlas.id("center_vertical"), ChunkMatcher::centerIfVertical);
		CHUNK_MATCHERS.put(AntiqueAtlas.id("bridge_horizontal"), ChunkMatcher::bridgeHorizontal);
		CHUNK_MATCHERS.put(AntiqueAtlas.id("bridge_vertical"), ChunkMatcher::bridgeVertical);
		CHUNK_MATCHERS.put(AntiqueAtlas.id("path_straight_horizontal"), (ChunkJunctionMatcher) ChunkJunctionMatcher::straightHorizontal);
		CHUNK_MATCHERS.put(AntiqueAtlas.id("path_straight_vertical"), (ChunkJunctionMatcher) ChunkJunctionMatcher::straightVertical);
	}
}

