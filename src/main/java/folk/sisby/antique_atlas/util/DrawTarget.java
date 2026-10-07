package folk.sisby.antique_atlas.util;

import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.RotationAxis;

import java.util.List;

/**
 * Where the atlas is drawn: the GUI via {@link DrawContext}, or the world via the render command queue.
 */
public interface DrawTarget {
	void push();

	void pop();

	void translate(double x, double y);

	void scale(float scale);

	void rotateZ(float degrees);

	void quads(Identifier texture, int textureWidth, int textureHeight, boolean transparent, List<Quad> quads);

	void fill(int x1, int y1, int x2, int y2, float z, int argb);

	default DrawBatcher batch(Identifier texture, int textureWidth, int textureHeight, boolean transparent) {
		return new DrawBatcher(this, texture, textureWidth, textureHeight, transparent);
	}

	default void quad(Identifier texture, int textureWidth, int textureHeight, int x, int y, float z, int width, int height, int u, int v, int regionWidth, int regionHeight, int argb, boolean transparent) {
		quads(texture, textureWidth, textureHeight, transparent, List.of(new Quad(x, y, z, width, height, u, v, regionWidth, regionHeight, argb)));
	}

	record Quad(int x, int y, float z, int width, int height, int u, int v, int regionWidth, int regionHeight, int argb) {
	}

	record Gui(DrawContext context) implements DrawTarget {
		@Override
		public void push() {
			context.getMatrices().pushMatrix();
		}

		@Override
		public void pop() {
			context.getMatrices().popMatrix();
		}

		@Override
		public void translate(double x, double y) {
			context.getMatrices().translate((float) x, (float) y);
		}

		@Override
		public void scale(float scale) {
			context.getMatrices().scale(scale, scale);
		}

		@Override
		public void rotateZ(float degrees) {
			context.getMatrices().rotate((float) Math.toRadians(degrees));
		}

		@Override
		public void quads(Identifier texture, int textureWidth, int textureHeight, boolean transparent, List<Quad> quads) {
			for (Quad q : quads) {
				context.drawTexture(RenderPipelines.GUI_TEXTURED, texture, q.x(), q.y(), q.u(), q.v(), q.width(), q.height(), q.regionWidth(), q.regionHeight(), textureWidth, textureHeight, q.argb());
			}
		}

		@Override
		public void fill(int x1, int y1, int x2, int y2, float z, int argb) {
			context.fill(x1, y1, x2, y2, argb);
		}
	}

	record World(MatrixStack matrices, OrderedRenderCommandQueue queue, int light) implements DrawTarget {
		public static boolean areWeShadersRightNow() {
			try {
				Class<?> apiClass = Class.forName("net.irisshaders.iris.api.v0.IrisApi");
				Object apiInstance = apiClass.getDeclaredMethod("getInstance").invoke(null);
				return (boolean) apiClass.getDeclaredMethod("isShaderPackInUse").invoke(apiInstance);
			} catch (Exception e) {
				return false;
			}
		}

		@Override
		public void push() {
			matrices.push();
		}

		@Override
		public void pop() {
			matrices.pop();
		}

		@Override
		public void translate(double x, double y) {
			matrices.translate(x, y, 0.0);
		}

		@Override
		public void scale(float scale) {
			matrices.scale(scale, scale, 1.0F);
		}

		@Override
		public void rotateZ(float degrees) {
			matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(degrees));
		}

		@Override
		public void quads(Identifier texture, int textureWidth, int textureHeight, boolean transparent, List<Quad> quads) {
			if (quads.isEmpty()) return;
			boolean shaders = areWeShadersRightNow();
			RenderLayer layer = shaders ? (transparent ? RenderLayers.entityTranslucent(texture) : RenderLayers.entitySolid(texture)) : RenderLayers.text(texture);
			List<Quad> copy = List.copyOf(quads);
			queue.submitCustom(matrices, layer, (entry, vertexConsumer) -> {
				for (Quad q : copy) {
					float x1 = q.x();
					float x2 = q.x() + q.width();
					float y1 = q.y();
					float y2 = q.y() + q.height();
					float u1 = (q.u() + 0.0F) / textureWidth;
					float u2 = (q.u() + (float) q.regionWidth()) / textureWidth;
					float v1 = (q.v() + 0.0F) / textureHeight;
					float v2 = (q.v() + (float) q.regionHeight()) / textureHeight;
					if (shaders) {
						vertexConsumer.vertex(entry, x1, y1, q.z()).color(q.argb()).texture(u1, v1).overlay(0).light(light).normal(entry, 0, 0, 1);
						vertexConsumer.vertex(entry, x1, y2, q.z()).color(q.argb()).texture(u1, v2).overlay(0).light(light).normal(entry, 0, 0, 1);
						vertexConsumer.vertex(entry, x2, y2, q.z()).color(q.argb()).texture(u2, v2).overlay(0).light(light).normal(entry, 0, 0, 1);
						vertexConsumer.vertex(entry, x2, y1, q.z()).color(q.argb()).texture(u2, v1).overlay(0).light(light).normal(entry, 0, 0, 1);
					} else {
						vertexConsumer.vertex(entry, x1, y1, q.z()).color(q.argb()).texture(u1, v1).light(light);
						vertexConsumer.vertex(entry, x1, y2, q.z()).color(q.argb()).texture(u1, v2).light(light);
						vertexConsumer.vertex(entry, x2, y2, q.z()).color(q.argb()).texture(u2, v2).light(light);
						vertexConsumer.vertex(entry, x2, y1, q.z()).color(q.argb()).texture(u2, v1).light(light);
					}
				}
			});
		}

		@Override
		public void fill(int x1, int y1, int x2, int y2, float z, int argb) {
			queue.submitCustom(matrices, RenderLayers.textBackgroundSeeThrough(), (entry, vertexConsumer) -> {
				vertexConsumer.vertex(entry, x1, y1, z).color(argb).light(light);
				vertexConsumer.vertex(entry, x1, y2, z).color(argb).light(light);
				vertexConsumer.vertex(entry, x2, y2, z).color(argb).light(light);
				vertexConsumer.vertex(entry, x2, y1, z).color(argb).light(light);
			});
		}
	}
}
