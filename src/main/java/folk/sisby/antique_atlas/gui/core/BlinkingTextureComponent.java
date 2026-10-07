package folk.sisby.antique_atlas.gui.core;

import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.ColorHelper;

/**
 * Displays a texture that changes alpha at regular intervals.
 * By default the texture file is assumed to be full image, but that behavior
 * can be altered by overriding the method {@link #drawImage(DrawContext, float)}.
 *
 * @author Hunternif
 */
public class BlinkingTextureComponent extends Component {
	protected Identifier texture;
	/**
	 * The number of milliseconds the icon spends visible or invisible.
	 */
	protected final long blinkTime;
	protected final float visibleAlpha;
	protected final float invisibleAlpha;

	protected long lastTickTime;
	/**
	 * The flag that switches value every "blink".
	 */
	protected boolean isVisible;

	public BlinkingTextureComponent(long blinkTime, float visibleAlpha, float invisibleAlpha) {
		this.blinkTime = blinkTime;
		this.visibleAlpha = visibleAlpha;
		this.invisibleAlpha = invisibleAlpha;
	}

	public BlinkingTextureComponent() {
		this(500, 1, 0.25f);
	}

	public void setTexture(Identifier texture, int width, int height) {
		this.texture = texture;
		setSize(width, height);
		// Set up the timer so that the image appears visible at the first moment:
		lastTickTime = 0;
		isVisible = false;
	}

	@Override
	public void render(DrawContext context, int mouseX, int mouseY, float partialTick) {
		long currentTime = System.currentTimeMillis();
		if (lastTickTime + blinkTime < currentTime) {
			lastTickTime = currentTime;
			isVisible = !isVisible;
		}
		drawImage(context, isVisible ? visibleAlpha : invisibleAlpha);
	}

	protected void drawImage(DrawContext context, float alpha) {
		context.drawTexture(RenderPipelines.GUI_TEXTURED, texture, getGuiX(), getGuiY(), 0, 0, getWidth(), getHeight(), getWidth(), getHeight(), ColorHelper.getWhite(alpha));
	}
}
