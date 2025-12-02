package folk.sisby.antique_atlas.util;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.RotationAxis;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;

public class DrawUtil {
	public static void drawCenteredWithRotation(MatrixStack matrices, VertexConsumerProvider vertexConsumers, Identifier texture, double x, double y, float z, float scale, int textureWidth, int textureHeight, float rotation, int light, int argb) {
		matrices.push();
		matrices.translate(x, y, 0.0);
		matrices.scale(scale, scale, 1.0F);
		matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(180 + rotation));
		matrices.translate(-textureWidth / 2f, -textureHeight / 2f, 0f);
		DrawBatcher.drawSingle(matrices, vertexConsumers, texture, textureWidth, textureHeight, light, 0, 0, z, textureWidth, textureHeight, 0, 0, textureWidth, textureHeight, argb, false);
		matrices.pop();
	}

	public static void fill(MatrixStack matrices, VertexConsumerProvider vertexConsumers, RenderLayer layer, float z, int light, int x1, int y1, int x2, int y2, float alpha, float[] color) {
		BufferBuilder bufferBuilder = null;
		VertexConsumer vertexConsumer;
		if (vertexConsumers == null) {
			RenderSystem.enableBlend();
			RenderSystem.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
			MinecraftClient.getInstance().gameRenderer.getLightmapTextureManager().enable();
			RenderSystem.setShader(GameRenderer::getPositionColorLightmapProgram);
			bufferBuilder = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR_LIGHT);
			vertexConsumer = bufferBuilder;
		} else {
			vertexConsumer = vertexConsumers.getBuffer(layer);
		}

		Matrix4f matrix4f = matrices.peek().getPositionMatrix();

		vertexConsumer.vertex(matrix4f, x1, y1, z).color(color[0], color[1], color[2], alpha).light(light);
		vertexConsumer.vertex(matrix4f, x1, y2, z).color(color[0], color[1], color[2], alpha).light(light);
		vertexConsumer.vertex(matrix4f, x2, y2, z).color(color[0], color[1], color[2], alpha).light(light);
		vertexConsumer.vertex(matrix4f, x2, y1, z).color(color[0], color[1], color[2], alpha).light(light);
		if (bufferBuilder != null) {
			BufferRenderer.drawWithGlobalProgram(bufferBuilder.end());
			MinecraftClient.getInstance().gameRenderer.getLightmapTextureManager().disable();
			RenderSystem.disableBlend();
		}
	}
}
