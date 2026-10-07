package folk.sisby.antique_atlas.util;

import net.minecraft.util.Identifier;
import net.minecraft.util.math.ColorHelper;

public class DrawUtil {
	public static void drawCenteredWithRotation(DrawTarget target, Identifier texture, double x, double y, float z, float scale, int textureWidth, int textureHeight, float rotation, int argb) {
		target.push();
		target.translate(x, y);
		target.scale(scale);
		target.rotateZ(180 + rotation);
		target.translate(-textureWidth / 2f, -textureHeight / 2f);
		DrawBatcher.drawSingle(target, texture, textureWidth, textureHeight, 0, 0, z, textureWidth, textureHeight, 0, 0, textureWidth, textureHeight, argb, false);
		target.pop();
	}

	public static void fill(DrawTarget target, float z, int x1, int y1, int x2, int y2, float alpha, float[] color) {
		target.fill(x1, y1, x2, y2, z, ColorHelper.fromFloats(alpha, color[0], color[1], color[2]));
	}
}
