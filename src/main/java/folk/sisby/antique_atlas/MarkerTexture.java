package folk.sisby.antique_atlas;

import com.mojang.blaze3d.systems.RenderSystem;
import folk.sisby.antique_atlas.util.DrawBatcher;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.ColorHelper;
import net.minecraft.util.math.MathHelper;
import org.joml.Vector2d;

public record MarkerTexture(Identifier id, Identifier accentId, int offsetX, int offsetY, int textureWidth, int textureHeight, int mipLevels, int nearClip, int farClip) {
	private static Identifier idToTexture(Identifier id) {
		return id.withPrefixedPath("textures/atlas/marker/").withSuffixedPath(".png");
	}

	public static MarkerTexture ofId(Identifier id, int offsetX, int offsetY, int width, int height, int mipLevels, int nearClip, int farClip, boolean accent) {
		return new MarkerTexture(idToTexture(id), accent ? idToTexture(id.withSuffixedPath("_accent")) : null, offsetX, offsetY, width, height, mipLevels, nearClip, farClip);
	}

	public static MarkerTexture centered(Identifier id, int width, int height, int mipLevels, int nearClip, int farClip, boolean accent) {
		return ofId(id, -width / 2, -height / 2, width, height, mipLevels, nearClip, farClip, accent);
	}

	public static final MarkerTexture DEFAULT = centered(AntiqueAtlas.id("custom/point"), 32, 32, 0, 1, Integer.MAX_VALUE, true);

	public Identifier keyId() {
		return Identifier.of(id.getNamespace(), id.getPath().substring("textures/atlas/marker/".length(), id.getPath().length() - 4));
	}

	public String displayId() {
		return id.getNamespace().equals(AntiqueAtlas.ID) ? keyId().getPath() : keyId().toString();
	}

	public int fullTextureWidth() {
		int width = textureWidth;
		for (int i = 0; i < mipLevels; i++) {
			width += textureWidth >> (i + 1);
		}
		return width;
	}

	public int getU(int mipLevel) {
		int currentMipLevel = mipLevel - 1;
		int u = 0;
		while (currentMipLevel >= 0) {
			u += textureWidth / (1 << currentMipLevel);
			currentMipLevel--;
		}
		return u;
	}

	public Vector2d getCenter(int tileChunks) {
		int mipLevel = MathHelper.clamp(MathHelper.ceilLog2(tileChunks), 0, mipLevels);
		return new Vector2d(((double) offsetX + (double) textureWidth / 2.0) / (double) (1 << mipLevel), ((double) offsetY + (double) textureHeight / 2.0) / (double) (1 << mipLevel));
	}

	public double getSquaredSize(int tileChunks) {
		int mipLevel = MathHelper.clamp(MathHelper.ceilLog2(tileChunks), 0, mipLevels);
		return textureWidth * textureHeight / (double) (1 << mipLevel);
	}

	public void drawIcon(DrawContext context, int x, int y, float[] accent) {
		context.drawTexture(id, x, y, 0, 0, textureWidth, textureHeight, fullTextureWidth(), textureHeight);
		if (accentId != null && accent != null) {
			RenderSystem.setShaderColor(accent[0], accent[1], accent[2], 1F);
			context.drawTexture(accentId, x, y, 0, 0, textureWidth, textureHeight, fullTextureWidth(), textureHeight);
			RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
		}
	}

	public void draw(DrawContext context, double markerX, double markerY, float markerScale, int tileChunks, float[] accent, float tint, float alpha) {
		if (alpha == 0) return;
		context.getMatrices().push();
		context.getMatrices().translate(markerX, markerY, 0.0);
		context.getMatrices().scale(markerScale, markerScale, 1.0F);
		RenderSystem.setShaderColor(tint, tint, tint, alpha);
		if (tileChunks > 1 && mipLevels > 0) {
			int mipLevel = MathHelper.clamp(MathHelper.ceilLog2(tileChunks), 0, mipLevels);
			context.drawTexture(id, offsetX / (1 << mipLevel), offsetY / (1 << mipLevel), getU(mipLevel), 0, textureWidth / (1 << mipLevel), textureHeight / (1 << mipLevel), fullTextureWidth(), textureHeight);
			if (accentId != null && accent != null) {
				RenderSystem.setShaderColor(tint * accent[0], tint * accent[1], tint * accent[2], alpha);
				context.drawTexture(accentId, offsetX / (1 << mipLevel), offsetY / (1 << mipLevel), getU(mipLevel), 0, textureWidth / (1 << mipLevel), textureHeight / (1 << mipLevel), fullTextureWidth(), textureHeight);
			}
		} else {
			context.drawTexture(id, offsetX, offsetY, 0, 0, textureWidth, textureHeight, fullTextureWidth(), textureHeight);
			if (accentId != null && accent != null) {
				RenderSystem.setShaderColor(tint * accent[0], tint * accent[1], tint * accent[2], alpha);
				context.drawTexture(accentId, offsetX, offsetY, 0, 0, textureWidth, textureHeight, fullTextureWidth(), textureHeight);
			}
		}
		RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
		context.getMatrices().pop();
	}

	public void draw(MatrixStack matrices, VertexConsumerProvider vertexConsumers, double markerX, double markerY, float z, float markerScale, int tileChunks, float[] accent, float tint, float alpha, int light) {
		if (alpha == 0) return;
		matrices.push();
		matrices.translate(markerX, markerY, 0.0);
		matrices.scale(markerScale, markerScale, 1.0F);
		int mainArgb = ColorHelper.Argb.getArgb((int) (alpha * 255), (int) (tint * 255), (int) (tint * 255), (int) (tint * 255));
		int accentArgb = accent != null ? ColorHelper.Argb.getArgb((int) (alpha * 255), (int) (tint * accent[0] * 255), (int) (tint * accent[1] * 255), (int) (tint * accent[2] * 255)) : 0;
		if (tileChunks > 1 && mipLevels > 0) {
			int mipLevel = MathHelper.clamp(MathHelper.ceilLog2(tileChunks), 0, mipLevels);
			try (DrawBatcher batcher = new DrawBatcher(matrices, vertexConsumers, id, fullTextureWidth(), textureHeight, light)) {
				batcher.add(offsetX / (1 << mipLevel), offsetY / (1 << mipLevel), z, textureWidth / (1 << mipLevel), textureHeight / (1 << mipLevel), getU(mipLevel), 0, textureWidth / (1 << mipLevel), textureHeight / (1 << mipLevel), mainArgb);
			}
			if (accentId != null && accent != null) {
				try (DrawBatcher batcher = new DrawBatcher(matrices, vertexConsumers, accentId, fullTextureWidth(), textureHeight, light)) {
					batcher.add(offsetX / (1 << mipLevel), offsetY / (1 << mipLevel), z, textureWidth / (1 << mipLevel), textureHeight / (1 << mipLevel), getU(mipLevel), 0, textureWidth / (1 << mipLevel), textureHeight / (1 << mipLevel), accentArgb);
				}
			}
		} else {
			try (DrawBatcher batcher = new DrawBatcher(matrices, vertexConsumers, id, fullTextureWidth(), textureHeight, light)) {
				batcher.add(offsetX, offsetY, z, textureWidth, textureHeight, 0, 0, textureWidth, textureHeight, mainArgb);
			}
			if (accentId != null && accent != null) {
				try (DrawBatcher batcher = new DrawBatcher(matrices, vertexConsumers, accentId, fullTextureWidth(), textureHeight, light)) {
					batcher.add(offsetX, offsetY, z, textureWidth, textureHeight, 0, 0, textureWidth, textureHeight, accentArgb);
				}
			}
		}
		matrices.pop();
	}
}
