package folk.sisby.antique_atlas.util;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

public class CodecUtil {
	public static <T> Codec<Set<T>> set(Codec<T> codec) {
		return codec.listOf().xmap(HashSet::new, ArrayList::new);
	}

	public static <T extends Enum<T>> Codec<T> ofEnum(Class<T> enumClass) {
		return Codec.STRING.flatXmap(id -> {
			try {
				return DataResult.success(Enum.valueOf(enumClass, id.toUpperCase(Locale.ROOT)));
			} catch (Exception e) {
				return DataResult.error(() -> "Unknown type: " + id);
			}
		}, value -> DataResult.success(value.name()));
	}
}
