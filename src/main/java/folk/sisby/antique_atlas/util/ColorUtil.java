package folk.sisby.antique_atlas.util;

import net.minecraft.util.math.ColorHelper;

public class ColorUtil {
	public static float[] componentsFromRgb(int color) {
		return new float[]{ColorHelper.Argb.getRed(color) / 255f, ColorHelper.Argb.getGreen(color) / 255f, ColorHelper.Argb.getBlue(color) / 255f};
	}

	public static int rgbFromComponents(float[] components) {
		return ColorHelper.Argb.getArgb(255, (int) (255 * components[0]), (int) (255 * components[1]), (int) (255 * components[2]));
	}
}
