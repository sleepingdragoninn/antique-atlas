package folk.sisby.antique_atlas.util;

import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.RotationAxis;

public class DrawUtil {
	public static void drawCenteredWithRotation(MatrixStack matrices, VertexConsumerProvider vertexConsumers, Identifier texture, double x, double y, float z, float scale, int textureWidth, int textureHeight, float rotation, int light, int argb) {
		matrices.push();
		matrices.translate(x, y, 0.0);
		matrices.scale(scale, scale, 1.0F);
		matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(180 + rotation));
		matrices.translate(-textureWidth / 2f, -textureHeight / 2f, 0f);
		try (DrawBatcher batcher = new DrawBatcher(matrices, vertexConsumers, texture, textureWidth, textureHeight, light)) {
			batcher.add(0, 0, z, textureWidth, textureHeight, 0, 0, textureWidth, textureHeight, argb);
		}
		matrices.pop();
	}
}
