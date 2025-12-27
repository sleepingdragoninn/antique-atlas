package folk.sisby.antique_atlas.gui;

import folk.sisby.antique_atlas.MarkerTexture;
import folk.sisby.antique_atlas.WorldAtlasData;
import folk.sisby.antique_atlas.gui.core.Component;
import folk.sisby.antique_atlas.gui.core.ScrollBoxComponent;
import folk.sisby.antique_atlas.gui.core.ToggleButtonRadioGroup;
import folk.sisby.antique_atlas.reloader.MarkerTextures;
import folk.sisby.surveyor.WorldSummary;
import folk.sisby.surveyor.landmark.Landmark;
import folk.sisby.surveyor.landmark.WorldLandmarks;
import folk.sisby.surveyor.landmark.component.LandmarkComponentTypes;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.item.Item;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.DyeColor;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * This GUI is used select marker icon and enter a label.
 * When the user clicks on the confirmation button, the call to MarkerAPI is made.
 *
 * @author Hunternif
 */
public class MarkerModal extends Component {
	protected World world;
	protected Landmark baseLandmark = null;

	protected MarkerTexture selectedTexture = MarkerTexture.DEFAULT;
	protected DyeColor selectedColor = DyeColor.WHITE;

	public static final int BUTTON_WIDTH = 80;
	public static final int BUTTON_SPACING = 8;

	public static final int TYPE_SPACING = 1;

	protected ButtonWidget btnDone;
	protected ButtonWidget btnCancel;
	protected TextFieldWidget textField;
	protected ScrollBoxComponent textureScrollBox;
	protected ToggleButtonRadioGroup<TexturePreviewButton<MarkerTexture>> textureRadioGroup;
	protected ScrollBoxComponent colorScrollBox;
	protected ToggleButtonRadioGroup<TexturePreviewButton<DyeColor>> colorRadioGroup;
	protected Map<MarkerTexture, TexturePreviewButton<MarkerTexture>> textureButtons = new LinkedHashMap<>();
	protected Map<DyeColor, TexturePreviewButton<DyeColor>> colorButtons = new LinkedHashMap<>();

	protected final List<IMarkerTypeSelectListener> markerListeners = new ArrayList<>();

	public MarkerModal() {
	}

	void setMarkerData(World world, Landmark baseLandmark) {
		this.world = world;
		this.baseLandmark = baseLandmark;
		this.selectedColor = Objects.requireNonNullElse(DyeColor.byFireworkColor(baseLandmark.getOrDefault(LandmarkComponentTypes.COLOR, DyeColor.WHITE.getFireworkColor())), DyeColor.WHITE);
		this.selectedTexture = MarkerTextures.getInstance().fromLandmark(baseLandmark);
		if (!selectedTexture.keyId().getPath().startsWith("custom/")) selectedTexture = textureButtons.keySet().stream().findFirst().orElse(MarkerTexture.DEFAULT);
		if (colorRadioGroup != null) updateSelected();
	}

	void addMarkerListener(IMarkerTypeSelectListener listener) {
		markerListeners.add(listener);
	}

	protected void updateSelected() {
		colorRadioGroup.setSelectedButton(colorButtons.get(selectedColor));
		textureRadioGroup.setSelectedButton(textureButtons.get(selectedTexture));
	}

	@Override
	public void init() { // set up in here because it scales to parent size
		removeAllChildren();
		super.init();

		addDrawableChild(btnDone = ButtonWidget.builder(Text.translatable("gui.done"), (button) -> {
			MutableText label = Text.literal(textField.getText());
			WorldLandmarks summary = WorldSummary.of(world).landmarks();
			if (summary != null) {
				summary.remove(baseLandmark.owner(), baseLandmark.id());
				summary.put(WorldAtlasData.copyLandmarkWith(
					baseLandmark,
					selectedTexture.keyId().withSuffixedPath("/" + selectedColor.getName() + "/" + baseLandmark.get(LandmarkComponentTypes.POS).getX() + "/" + baseLandmark.get(LandmarkComponentTypes.POS).getZ()),
					copy -> {
					Item item = world.getRegistryManager().get(RegistryKeys.ITEM).get(selectedTexture.item());
					if (item != null) copy.set(LandmarkComponentTypes.STACK, item.getDefaultStack().copy());
					copy.set(LandmarkComponentTypes.COLOR, selectedColor.getFireworkColor());
					copy.set(LandmarkComponentTypes.NAME, label);
				}));
			}
			((AtlasScreen) getParent()).updateBookmarkerList();
			ClientPlayerEntity player = MinecraftClient.getInstance().player;
			if (player != null) MinecraftClient.getInstance().getSoundManager().play(PositionedSoundInstance.master(SoundEvents.ENTITY_VILLAGER_WORK_CARTOGRAPHER, 1F));
			closeChild();
		}).dimensions(this.width / 2 - BUTTON_WIDTH - BUTTON_SPACING / 2, this.height / 2 + 70, BUTTON_WIDTH, 20).build());
		addDrawableChild(btnCancel = ButtonWidget.builder(Text.translatable("gui.cancel"), (button) -> closeChild())
			.dimensions(this.width / 2 + BUTTON_SPACING / 2, this.height / 2 + 70, BUTTON_WIDTH, 20).build());
		textField = new TextFieldWidget(MinecraftClient.getInstance().textRenderer, (this.width - 200) / 2, this.height / 2 - 65, 200, 20, Text.translatable("gui.antique_atlas.marker.label"));
		textField.setEditable(true);
		textField.setFocusUnlocked(true);
		textField.setFocused(true);
		textField.setPlaceholder(Text.translatable("gui.antique_atlas.marker.label"));
		textField.setText(baseLandmark.getOrDefault(LandmarkComponentTypes.NAME, Text.empty()).getString());

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
			textureButtons.put(texture, markerGui);
			textureRadioGroup.addButton(markerGui);
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
			colorButtons.put(color, colorGui);
			colorRadioGroup.addButton(colorGui);
			colorScrollBox.getViewport().addContent(colorGui).setRelativeX(colorContentX);
			colorContentX += TexturePreviewButton.FRAME_SIZE + TYPE_SPACING;
		}

		updateSelected();
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

	public interface IMarkerTypeSelectListener {
		void onSelectMarkerType(MarkerTexture texture);
	}
}
