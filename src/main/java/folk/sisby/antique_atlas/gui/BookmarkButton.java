package folk.sisby.antique_atlas.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import folk.sisby.antique_atlas.AntiqueAtlas;
import folk.sisby.antique_atlas.gui.core.ToggleButtonComponent;
import folk.sisby.antique_atlas.util.ColorUtil;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;

public class BookmarkButton extends ToggleButtonComponent {
	public static final Identifier TEXTURE_LEFT = AntiqueAtlas.id("textures/gui/bookmark_left.png");
	public static final Identifier TEXTURE_RIGHT = AntiqueAtlas.id("textures/gui/bookmark_right.png");
	public static final int WIDTH = 24;
	public static final int HEIGHT = 18;

	protected Text title;
	protected Identifier iconTexture;
	protected final float[] backgroundTint;
	protected final float[] iconTint;
	protected final int iconWidth;
	protected final int iconHeight;
	protected final boolean left;
	protected final Identifier backgroundTexture;

	public BookmarkButton(Identifier backgroundTexture, Text title, Identifier iconTexture, @Nullable Integer backgroundTint, @Nullable Integer iconTint, int iconWidth, int iconHeight, boolean left) {
		super(false);
		this.backgroundTexture = backgroundTexture;
		this.title = title;
		this.iconTexture = iconTexture;
		this.backgroundTint = backgroundTint == null ? null : ColorUtil.componentsFromRgb(backgroundTint);
		this.iconWidth = iconWidth;
		this.iconHeight = iconHeight;
		this.iconTint = iconTint == null ? null : ColorUtil.componentsFromRgb(iconTint);
		this.left = left;
		setTitle(title);
		setSize(WIDTH, HEIGHT);
	}

	public BookmarkButton(Text title, Identifier iconTexture, @Nullable Integer backgroundTint, @Nullable Integer iconTint, int iconWidth, int iconHeight, boolean left) {
		this(left ? TEXTURE_LEFT : TEXTURE_RIGHT, title, iconTexture, backgroundTint, iconTint, iconWidth, iconHeight, left);
	}

	public void setIconTexture(Identifier iconTexture) {
		this.iconTexture = iconTexture;
	}

	public Text getTitle() {
		return title;
	}

	public void setTitle(Text title) {
		this.title = title;
	}

	public void drawIcon(DrawContext context, int x, int y) {
		if (iconTint != null) RenderSystem.setShaderColor(iconTint[0], iconTint[1], iconTint[2], 1.0F);
		context.drawTexture(iconTexture, x, y, 0, 0, iconWidth, iconHeight, iconWidth, iconHeight);
		RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
	}

	@Override
	public void render(DrawContext context, int mouseX, int mouseY, float partialTick) {
		boolean mouseOver = isMouseOver(mouseX, mouseY);
		boolean isExtended = mouseOver || isSelected();

		RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
		if (backgroundTint != null) RenderSystem.setShaderColor(backgroundTint[0], backgroundTint[1], backgroundTint[2], 1.0F);
		context.drawTexture(backgroundTexture, getGuiX(), getGuiY(), 0, isExtended ? 0 : HEIGHT, WIDTH, HEIGHT, WIDTH, HEIGHT * 2);
		RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

		if (iconTexture != null) {
			int iconX = getGuiX() + 10 - iconWidth / 2 + (isExtended ? (left ? 3 : 1) : (left ? 4 : 0));
			int iconY = getGuiY() + 9 - iconHeight / 2;
			drawIcon(context, iconX, iconY);
		}

		renderTooltip(context, mouseX, mouseY, partialTick, mouseOver);
	}

	public void renderTooltip(DrawContext context, int mouseX, int mouseY, float partialTick, boolean mouseOver) {
		if (mouseOver && !title.getString().isEmpty()) {
			context.drawTooltip(textRenderer, title, mouseX, mouseY);
		}
	}
}
