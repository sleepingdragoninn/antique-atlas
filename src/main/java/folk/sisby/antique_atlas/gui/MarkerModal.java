package folk.sisby.antique_atlas.gui;

import folk.sisby.antique_atlas.MarkerTexture;
import folk.sisby.antique_atlas.gui.core.Component;
import folk.sisby.antique_atlas.gui.core.ScrollBoxComponent;
import folk.sisby.antique_atlas.gui.core.ToggleButtonRadioGroup;
import folk.sisby.antique_atlas.reloader.MarkerTextures;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.DyeColor;
import net.minecraft.util.math.ColumnPos;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.List;

/**
 * This GUI is used select marker icon and enter a label.
 * When the user clicks on the confirmation button, the call to MarkerAPI is made.
 *
 * @author Hunternif
 */
public class MarkerModal extends Component {
	private World world;
	private int markerX;
	private int markerZ;

	MarkerTexture selectedTexture = MarkerTexture.DEFAULT;
	DyeColor selectedColor = DyeColor.WHITE;

	private static final int BUTTON_WIDTH = 80;
	private static final int BUTTON_SPACING = 8;

	private static final int TYPE_SPACING = 1;

	private ButtonWidget btnDone;
	private ButtonWidget btnCancel;
	private TextFieldWidget textField;
	private ScrollBoxComponent textureScrollBox;
	private ToggleButtonRadioGroup<TexturePreviewButton<MarkerTexture>> textureRadioGroup;
	private ScrollBoxComponent colorScrollBox;
	private ToggleButtonRadioGroup<TexturePreviewButton<DyeColor>> colorRadioGroup;

	private final List<IMarkerTypeSelectListener> markerListeners = new ArrayList<>();

	MarkerModal() {
	}

	void setMarkerData(World world, int markerX, int markerZ) {
		this.world = world;
		this.markerX = markerX;
		this.markerZ = markerZ;
	}

	void addMarkerListener(IMarkerTypeSelectListener listener) {
		markerListeners.add(listener);
	}

	@Override
	public void init() {
		super.init();

		addDrawableChild(btnDone = ButtonWidget.builder(Text.translatable("gui.done"), (button) -> {
			((AtlasScreen) MinecraftClient.getInstance().currentScreen).getworldAtlasData().placeCustomMarker(world, selectedTexture, selectedColor, Text.literal(textField.getText()), new ColumnPos(markerX, markerZ));
			((AtlasScreen) MinecraftClient.getInstance().currentScreen).updateBookmarkerList();
			ClientPlayerEntity player = MinecraftClient.getInstance().player;
			world.playSound(player, player.getBlockPos(),
				SoundEvents.ENTITY_VILLAGER_WORK_CARTOGRAPHER, SoundCategory.AMBIENT,
				1F, 1F);
			closeChild();
		}).dimensions(this.width / 2 - BUTTON_WIDTH - BUTTON_SPACING / 2, this.height / 2 + 70, BUTTON_WIDTH, 20).build());
		addDrawableChild(btnCancel = ButtonWidget.builder(Text.translatable("gui.cancel"), (button) -> closeChild())
			.dimensions(this.width / 2 + BUTTON_SPACING / 2, this.height / 2 + 70, BUTTON_WIDTH, 20).build());
		textField = new TextFieldWidget(MinecraftClient.getInstance().textRenderer, (this.width - 200) / 2, this.height / 2 - 65, 200, 20, Text.translatable("gui.antique_atlas.marker.label"));
		textField.setEditable(true);
		textField.setFocusUnlocked(true);
		textField.setFocused(true);
		textField.setPlaceholder(Text.translatable("gui.antique_atlas.marker.label"));

		textureScrollBox = new ScrollBoxComponent(false, (TexturePreviewButton.FRAME_SIZE + TYPE_SPACING));
		this.addChild(textureScrollBox);

		int typeCount = (int) MarkerTextures.getInstance().asMap().values().stream().filter(t -> t.keyId().getPath().startsWith("custom/")).count();
		int typesOnScreen = Math.min(typeCount, 7);
		int typeScrollWidth = typesOnScreen * (TexturePreviewButton.FRAME_SIZE + TYPE_SPACING) - TYPE_SPACING;
		textureScrollBox.getViewport().setSize(typeScrollWidth, TexturePreviewButton.FRAME_SIZE + TYPE_SPACING);
		textureScrollBox.setGuiCoords((this.width - typeScrollWidth) / 2, this.height / 2 - 35);

		textureRadioGroup = new ToggleButtonRadioGroup<>();
		textureRadioGroup.addListener(button -> {
			selectedTexture = button.getValue();
			for (IMarkerTypeSelectListener listener : markerListeners) {
				listener.onSelectMarkerType(button.getValue());
			}
		});
		int contentX = 0;
		for (MarkerTexture texture : MarkerTextures.getInstance().asMap().values()) {
			if (!texture.keyId().getPath().startsWith("custom/")) continue;
			if (selectedTexture == MarkerTexture.DEFAULT) selectedTexture = texture;
			TexturePreviewButton<MarkerTexture> markerGui = new MarkerPreviewButton(texture, selectedColor.getColorComponents());
			textureRadioGroup.addButton(markerGui);
			if (selectedTexture.equals(texture)) {
				textureRadioGroup.setSelectedButton(markerGui);
			}
			textureScrollBox.getViewport().addContent(markerGui).setRelativeX(contentX);
			contentX += TexturePreviewButton.FRAME_SIZE + TYPE_SPACING;
		}

		// Color

		colorScrollBox = new ScrollBoxComponent(false, (TexturePreviewButton.FRAME_SIZE + TYPE_SPACING));
		this.addChild(colorScrollBox);

		int colorsOnScreen = Math.min(DyeColor.values().length, 7);
		int colorScrollWidth = colorsOnScreen * (TexturePreviewButton.FRAME_SIZE + TYPE_SPACING) - TYPE_SPACING;
		colorScrollBox.getViewport().setSize(colorScrollWidth, TexturePreviewButton.FRAME_SIZE + TYPE_SPACING);
		colorScrollBox.setGuiCoords((this.width - colorScrollWidth) / 2, this.height / 2 + 10);

		colorRadioGroup = new ToggleButtonRadioGroup<>();
		colorRadioGroup.addListener(button -> {
			selectedColor = button.getValue();
			for (TexturePreviewButton<MarkerTexture> preview : textureRadioGroup) {
				preview.reTint(selectedColor.getColorComponents());
			}
		});
		int colorContentX = 0;
		for (DyeColor color : DyeColor.values()) {
			TexturePreviewButton<DyeColor> colorGui = new TexturePreviewButton<>(color, BookmarkButton.TEXTURE_LEFT, BookmarkButton.WIDTH, BookmarkButton.HEIGHT, BookmarkButton.HEIGHT, color.getColorComponents());
			colorRadioGroup.addButton(colorGui);
			if (selectedColor.equals(color)) {
				colorRadioGroup.setSelectedButton(colorGui);
			}
			colorScrollBox.getViewport().addContent(colorGui).setRelativeX(colorContentX);
			colorContentX += TexturePreviewButton.FRAME_SIZE + TYPE_SPACING;
		}
	}

	@Override
	public void closeChild() {
		super.closeChild();
		if (textureScrollBox != null) {
			textureScrollBox.closeChild();
		}
		if (colorScrollBox != null) {
			colorScrollBox.closeChild();
		}
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		return super.mouseClicked(mouseX, mouseY, button) || textField.mouseClicked(mouseX, mouseY, button);
	}

	@Override
	public boolean keyPressed(int aa, int bb, int cc) {
		return super.keyPressed(aa, bb, cc) || textField.keyPressed(aa, bb, cc);
	}

	@Override
	public boolean charTyped(char aa, int bb) {
		return super.charTyped(aa, bb) || textField.charTyped(aa, bb);
	}

	@Override
	public void render(DrawContext context, int mouseX, int mouseY, float partialTick) {
		this.renderBackground(context);
		drawCentered(context, Text.translatable("gui.antique_atlas.marker.label"), this.height / 2 - 80, 0xDDDDDD, true);
		textField.render(context, mouseX, mouseY, partialTick);
		// Darker background for marker type selector
		context.fillGradient(textureScrollBox.getGuiX() + 1, textureScrollBox.getGuiY() + 1,
			textureScrollBox.getGuiX() + textureScrollBox.getWidth(),
			textureScrollBox.getGuiY() + textureScrollBox.getHeight(),
			0x88101010, 0x99101010);
		context.fillGradient(colorScrollBox.getGuiX() + 1, colorScrollBox.getGuiY() + 1,
			colorScrollBox.getGuiX() + colorScrollBox.getWidth(),
			colorScrollBox.getGuiY() + colorScrollBox.getHeight(),
			0x88101010, 0x99101010);
		super.render(context, mouseX, mouseY, partialTick);
	}

	interface IMarkerTypeSelectListener {
		void onSelectMarkerType(MarkerTexture texture);
	}
}
