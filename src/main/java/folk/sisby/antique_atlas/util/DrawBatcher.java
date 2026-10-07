package folk.sisby.antique_atlas.util;

import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;

public class DrawBatcher implements AutoCloseable {
	protected final DrawTarget target;
	protected final Identifier texture;
	protected final int textureWidth;
	protected final int textureHeight;
	protected final boolean transparent;
	protected final List<DrawTarget.Quad> quads = new ArrayList<>();

	public static void drawSingle(DrawTarget target, Identifier texture, int textureWidth, int textureHeight, int x, int y, float z, int width, int height, int u, int v, int regionWidth, int regionHeight, int argb, boolean transparent) {
		target.quad(texture, textureWidth, textureHeight, x, y, z, width, height, u, v, regionWidth, regionHeight, argb, transparent);
	}

	public DrawBatcher(DrawTarget target, Identifier texture, int textureWidth, int textureHeight, boolean transparent) {
		this.target = target;
		this.texture = texture;
		this.textureWidth = textureWidth;
		this.textureHeight = textureHeight;
		this.transparent = transparent;
	}

	public void add(int x, int y, float z, int width, int height, int u, int v, int regionWidth, int regionHeight, int argb) {
		quads.add(new DrawTarget.Quad(x, y, z, width, height, u, v, regionWidth, regionHeight, argb));
	}

	@Override
	public void close() {
		if (!quads.isEmpty()) target.quads(texture, textureWidth, textureHeight, transparent, quads);
		quads.clear();
	}
}
