package folk.sisby.antique_atlas.gui;

import folk.sisby.antique_atlas.AntiqueAtlas;
import folk.sisby.antique_atlas.gui.core.ToggleButtonComponent;
import folk.sisby.antique_atlas.util.ColorUtil;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.Identifier;


public class TexturePreviewButton<T> extends ToggleButtonComponent {
	public static final Identifier FRAME_SELECTED = AntiqueAtlas.id("textures/gui/frame_selected.png");
	public static final Identifier FRAME_UNSELECTED = AntiqueAtlas.id("textures/gui/frame.png");
	public static final int FRAME_SIZE = 34;

	protected final T value;
	protected final Identifier texture;
	protected final int textureWidth;
	protected final int textureHeight;
	protected final int v;
	protected float[] tint;

	public TexturePreviewButton(T value, Identifier texture, int textureWidth, int textureHeight, int v, float[] tint) {
		super(false);
		this.value = value;
		this.texture = texture;
		this.textureWidth = textureWidth;
		this.textureHeight = textureHeight;
		this.v = v;
		this.tint = tint;
		setSize(FRAME_SIZE, FRAME_SIZE);
	}

	public T getValue() {
		return value;
	}

	public void reTint(float[] tint) {
		if (this.tint != null) this.tint = tint;
	}

	protected void drawTexture(DrawContext context, int x, int y) {
		context.drawTexture(RenderPipelines.GUI_TEXTURED, texture, x, y, 0, v, textureWidth, textureHeight, textureWidth, textureHeight + v, ColorUtil.tint(tint, 1.0F));
	}

	@Override
	public void render(DrawContext context, int mouseX, int mouseY, float partialTick) {
		Identifier frameTexture = isSelected() ? FRAME_SELECTED : FRAME_UNSELECTED;
		context.drawTexture(RenderPipelines.GUI_TEXTURED, frameTexture, getGuiX() + 1, getGuiY() + 1, 0, 0, FRAME_SIZE, FRAME_SIZE, FRAME_SIZE, FRAME_SIZE);

		int centerX = getGuiX() + (FRAME_SIZE - textureWidth) / 2;
		int centerY = getGuiY() + (FRAME_SIZE - textureHeight) / 2;
		drawTexture(context, centerX, centerY);

		super.render(context, mouseX, mouseY, partialTick);
	}
}
