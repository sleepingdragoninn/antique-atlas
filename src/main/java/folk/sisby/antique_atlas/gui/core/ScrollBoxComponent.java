package folk.sisby.antique_atlas.gui.core;

import folk.sisby.antique_atlas.AntiqueAtlas;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.client.util.math.Rect2i;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Identifier;

public class ScrollBoxComponent extends Component {
	public static final Identifier ARROW = AntiqueAtlas.id("textures/gui/arrow.png");
	public static final int ARROW_SIZE = 16;
	public static final int ARROW_TEXTURE_WIDTH = 32;
	public static final int ARROW_TEXTURE_HEIGHT = 64;

	protected final int scrollStep;
	protected final boolean vertical;
	protected final ViewportComponent viewport;

	/**
	 * How much the content of the viewport is displaced.
	 */
	protected int scrollPos = 0;

	public ScrollBoxComponent(boolean vertical, int scrollStep) {
		this.vertical = vertical;
		this.scrollStep = scrollStep;
		this.viewport = new ViewportComponent();
		this.addChild(viewport);
	}

	public void renderArrow(DrawContext context, int mouseX, int mouseY, boolean prev) {
		int x = !vertical ? (prev ? getGuiX() - ARROW_SIZE : getGuiX() + getWidth()) : getGuiX() + (getWidth() - ARROW_SIZE) / 2;
		int y = vertical ? (prev ? getGuiY() - ARROW_SIZE : getGuiY() + getHeight()) : getGuiY() + (getHeight() - ARROW_SIZE) / 2;
		boolean hovered = new Rect2i(x, y, ARROW_SIZE, ARROW_SIZE).contains(mouseX, mouseY);
		int u = (prev ? 0 : ARROW_SIZE);
		int v = (vertical ? 0 : ARROW_SIZE) + (hovered ? ARROW_SIZE * 2 : 0);
		context.drawTexture(ARROW, x, y, u, v, ARROW_SIZE, ARROW_SIZE, ARROW_TEXTURE_WIDTH, ARROW_TEXTURE_HEIGHT);
	}

	@Override
	public void render(DrawContext context, int mouseX, int mouseY, float partialTick) {
		super.render(context, mouseX, mouseY, partialTick);
		if (scrollPos > 0) renderArrow(context, mouseX, mouseY, true);
		if (scrollPos < getContentSize() - getViewportSize()) renderArrow(context, mouseX, mouseY, false);
	}

	public boolean clickArrow(double mouseX, double mouseY, boolean prev) {
		int x = !vertical ? (prev ? getGuiX() - ARROW_SIZE : getGuiX() + getWidth()) : getGuiX() + (getWidth() - ARROW_SIZE) / 2;
		int y = vertical ? (prev ? getGuiY() - ARROW_SIZE : getGuiY() + getHeight()) : getGuiY() + (getHeight() - ARROW_SIZE) / 2;
		boolean hovered = new Rect2i(x, y, ARROW_SIZE, ARROW_SIZE).contains((int) mouseX, (int) mouseY);
		if (hovered) {
			int numSteps = (int) Math.round((double) getViewportSize() / scrollStep);
			setScrollPos(scrollPos + numSteps * scrollStep * (prev ? -1 : 1));
			MinecraftClient.getInstance().getSoundManager().play(PositionedSoundInstance.master(SoundEvents.UI_BUTTON_CLICK.value(), 1.0F));
			return true;
		}
		return false;
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int mb) {
		if (scrollPos > 0 && clickArrow(mouseX, mouseY, true)) return true;
		if (scrollPos < getContentSize() - getViewportSize() && clickArrow(mouseX, mouseY, false)) return true;
		return super.mouseClicked(mouseX, mouseY, mb);
	}

	@Override
	public boolean mouseScrolled(double mx, double my, double wheelMove) {
		if (isMouseOver(mx, my)) {
			if (wheelMove != 0) {
				wheelMove = wheelMove > 0 ? -1 : 1;
				setScrollPos((int) (scrollPos + wheelMove * scrollStep));
				return true;
			}
		}

		return super.mouseScrolled(mx, my, wheelMove);
	}

	/**
	 * Offset of the viewport's content in pixels. This method forces
	 * validation of the viewport and its content in order to work correctly
	 * during initGui().
	 */
	public void setScrollPos(int scrollPos) {
		viewport.content.updateSize();
		viewport.updateSize();
		doSetScrollPos(scrollPos);
	}

	/**
	 * Offset of the viewport's content in pixels. This will only work
	 * correctly after the viewport's size has been validated.
	 */
	protected void doSetScrollPos(int newPos) {
		this.scrollPos = Math.max(0, Math.min(newPos, getContentSize() - getViewportSize()));
		updateContentPos();
	}

	protected void updateContentPos() {
		viewport.content.setRelativeCoords(vertical ? viewport.content.getRelativeX() : -scrollPos, vertical ? -scrollPos : viewport.content.getRelativeY());
	}

	public int getContentSize() {
		return vertical ? viewport.contentHeight : viewport.contentWidth;
	}

	public int getViewportSize() {
		return vertical ? viewport.getHeight() : viewport.getWidth();
	}

	public ViewportComponent getViewport() {
		return viewport;
	}
}
