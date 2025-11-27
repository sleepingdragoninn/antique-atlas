package folk.sisby.antique_atlas.util;

import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.util.Identifier;
import net.minecraft.world.biome.Biome;

public class ForgeTags {
	public static class Biomes {
		public static final TagKey<Biome> IS_HOT = tag("is_hot");
		public static final TagKey<Biome> IS_HOT_OVERWORLD = tag("is_hot/overworld");
		public static final TagKey<Biome> IS_HOT_NETHER = tag("is_hot/nether");
		public static final TagKey<Biome> IS_HOT_END = tag("is_hot/end");

		public static final TagKey<Biome> IS_COLD = tag("is_cold");
		public static final TagKey<Biome> IS_COLD_OVERWORLD = tag("is_cold/overworld");
		public static final TagKey<Biome> IS_COLD_NETHER = tag("is_cold/nether");
		public static final TagKey<Biome> IS_COLD_END = tag("is_cold/end");

		public static final TagKey<Biome> IS_SPARSE = tag("is_sparse");
		public static final TagKey<Biome> IS_SPARSE_OVERWORLD = tag("is_sparse/overworld");
		public static final TagKey<Biome> IS_SPARSE_NETHER = tag("is_sparse/nether");
		public static final TagKey<Biome> IS_SPARSE_END = tag("is_sparse/end");
		public static final TagKey<Biome> IS_DENSE = tag("is_dense");
		public static final TagKey<Biome> IS_DENSE_OVERWORLD = tag("is_dense/overworld");
		public static final TagKey<Biome> IS_DENSE_NETHER = tag("is_dense/nether");
		public static final TagKey<Biome> IS_DENSE_END = tag("is_dense/end");

		public static final TagKey<Biome> IS_WET = tag("is_wet");
		public static final TagKey<Biome> IS_WET_OVERWORLD = tag("is_wet/overworld");
		public static final TagKey<Biome> IS_WET_NETHER = tag("is_wet/nether");
		public static final TagKey<Biome> IS_WET_END = tag("is_wet/end");
		public static final TagKey<Biome> IS_DRY = tag("is_dry");
		public static final TagKey<Biome> IS_DRY_OVERWORLD = tag("is_dry/overworld");
		public static final TagKey<Biome> IS_DRY_NETHER = tag("is_dry/nether");
		public static final TagKey<Biome> IS_DRY_END = tag("is_dry/end");

		public static final TagKey<Biome> IS_CONIFEROUS = tag("is_coniferous");

		public static final TagKey<Biome> IS_SPOOKY = tag("is_spooky");
		public static final TagKey<Biome> IS_DEAD = tag("is_dead");
		public static final TagKey<Biome> IS_LUSH = tag("is_lush");
		public static final TagKey<Biome> IS_MUSHROOM = tag("is_mushroom");
		public static final TagKey<Biome> IS_MAGICAL = tag("is_magical");
		public static final TagKey<Biome> IS_RARE = tag("is_rare");
		public static final TagKey<Biome> IS_PLATEAU = tag("is_plateau");
		public static final TagKey<Biome> IS_MODIFIED = tag("is_modified");

		public static final TagKey<Biome> IS_WATER = tag("is_water");
		public static final TagKey<Biome> IS_DESERT = tag("is_desert");
		public static final TagKey<Biome> IS_PLAINS = tag("is_plains");
		public static final TagKey<Biome> IS_SWAMP = tag("is_swamp");
		public static final TagKey<Biome> IS_SANDY = tag("is_sandy");
		public static final TagKey<Biome> IS_SNOWY = tag("is_snowy");
		public static final TagKey<Biome> IS_WASTELAND = tag("is_wasteland");
		public static final TagKey<Biome> IS_VOID = tag("is_void");
		public static final TagKey<Biome> IS_UNDERGROUND = tag("is_underground");

		public static final TagKey<Biome> IS_CAVE = tag("is_cave");
		public static final TagKey<Biome> IS_PEAK = tag("is_peak");
		public static final TagKey<Biome> IS_SLOPE = tag("is_slope");
		public static final TagKey<Biome> IS_MOUNTAIN = tag("is_mountain");

		private static TagKey<Biome> tag(String name) {
			return TagKey.of(RegistryKeys.BIOME, Identifier.of("forge", name));
		}
	}
}
