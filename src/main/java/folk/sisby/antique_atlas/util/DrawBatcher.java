package folk.sisby.antique_atlas.util;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.BuiltBuffer;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;

import java.lang.reflect.Method;

public class DrawBatcher implements AutoCloseable {

	protected final Matrix4f matrix4f;
	protected final BufferBuilder bufferBuilder;
	protected final VertexConsumer vertexConsumer;
	protected final float textureWidth;
	protected final float textureHeight;
	protected final int light;
	protected final boolean inWorld;

	public static boolean areWeShadersRightNow() {
		try {
			Class<?> apiClass = Class.forName("net.irisshaders.iris.api.v0.IrisApi");
			Method instanceMethod = apiClass.getDeclaredMethod("getInstance");
			Method inUseMethod = apiClass.getDeclaredMethod("isShaderPackInUse");
			Object apiInstance = instanceMethod.invoke(null);
			return (boolean) inUseMethod.invoke(apiInstance);
		} catch (Exception e) {
			return false;
		}
	}

	public static void drawSingle(MatrixStack matrices, VertexConsumerProvider vertexConsumers, Identifier texture, int textureWidth, int textureHeight, int light, int x, int y, float z, int width, int height, int u, int v, int regionWidth, int regionHeight, int argb, boolean drawingTransparent) {
		try (DrawBatcher batcher = new DrawBatcher(matrices, vertexConsumers, texture, textureWidth, textureHeight, light, drawingTransparent)) {
			batcher.add(x, y, z, width, height, u, v, regionWidth, regionHeight, argb);
		}
	}

	public DrawBatcher(MatrixStack matrices, VertexConsumerProvider vertexConsumers, Identifier texture, int textureWidth, int textureHeight, int light, boolean drawingTransparent) {
		this.inWorld = vertexConsumers != null;
		if (vertexConsumers == null) {
			RenderSystem.enableBlend();
			RenderSystem.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
			RenderSystem.setShaderTexture(0, texture);
			RenderSystem.setShader(GameRenderer::getPositionColorTexLightmapProgram);
			this.bufferBuilder = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR_TEXTURE_LIGHT);
			this.vertexConsumer = bufferBuilder;
		} else {
			this.bufferBuilder = null;
			if (areWeShadersRightNow()) {
				if (drawingTransparent) {
					this.vertexConsumer = vertexConsumers.getBuffer(RenderLayer.getEntityNoOutline(texture));
				} else {
					this.vertexConsumer = vertexConsumers.getBuffer(RenderLayer.getEntitySolid(texture));
				}
			} else {
				this.vertexConsumer = vertexConsumers.getBuffer(RenderLayer.getText(texture));
			}
		}
		this.matrix4f = matrices.peek().getPositionMatrix();
		this.textureWidth = textureWidth;
		this.textureHeight = textureHeight;
		this.light = light;
	}

	public void add(int x, int y, float z, int width, int height, int u, int v, int regionWidth, int regionHeight, int argb) {
		this.innerAdd(x, x + width, y, y + height, z,
			(u + 0.0F) / textureWidth,
			(u + (float) regionWidth) / textureWidth,
			(v + 0.0F) / textureHeight,
			(v + (float) regionHeight) / textureHeight,
			argb
		);
	}

	protected void innerAdd(float x1, float x2, float y1, float y2, float z, float u1, float u2, float v1, float v2, int argb) {
		if (inWorld) {
			vertexConsumer.vertex(matrix4f, x1, y1, z).color(argb).texture(u1, v1).overlay(0).light(light).normal(0,0,0);
			vertexConsumer.vertex(matrix4f, x1, y2, z).color(argb).texture(u1, v2).overlay(0).light(light).normal(0,0,0);
			vertexConsumer.vertex(matrix4f, x2, y2, z).color(argb).texture(u2, v2).overlay(0).light(light).normal(0,0,0);
			vertexConsumer.vertex(matrix4f, x2, y1, z).color(argb).texture(u2, v1).overlay(0).light(light).normal(0,0,0);
		} else {
			vertexConsumer.vertex(matrix4f, x1, y1, z).color(argb).texture(u1, v1).light(light);
			vertexConsumer.vertex(matrix4f, x1, y2, z).color(argb).texture(u1, v2).light(light);
			vertexConsumer.vertex(matrix4f, x2, y2, z).color(argb).texture(u2, v2).light(light);
			vertexConsumer.vertex(matrix4f, x2, y1, z).color(argb).texture(u2, v1).light(light);
		}
	}

	@Override
	public void close() {
		if (bufferBuilder != null) {
			BuiltBuffer bb = bufferBuilder.endNullable();
			if (bb != null) BufferRenderer.drawWithGlobalProgram(bb);
			RenderSystem.disableBlend();
		}
	}
}
