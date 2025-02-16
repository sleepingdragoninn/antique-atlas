package folk.sisby.antique_atlas.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import folk.sisby.antique_atlas.AntiqueAtlas;
import folk.sisby.antique_atlas.AntiqueAtlasKeybindings;
import folk.sisby.antique_atlas.AtlasStructureLandmark;
import folk.sisby.antique_atlas.MarkerTexture;
import folk.sisby.antique_atlas.TileTexture;
import folk.sisby.antique_atlas.WorldAtlasData;
import folk.sisby.antique_atlas.gui.core.ButtonComponent;
import folk.sisby.antique_atlas.gui.core.Component;
import folk.sisby.antique_atlas.gui.core.CursorComponent;
import folk.sisby.antique_atlas.gui.core.ScreenState;
import folk.sisby.antique_atlas.gui.core.ScreenState.State;
import folk.sisby.antique_atlas.gui.core.ScreenState.ToggleState;
import folk.sisby.antique_atlas.gui.core.ScrollBoxComponent;
import folk.sisby.antique_atlas.gui.tiles.SubTile;
import folk.sisby.antique_atlas.gui.tiles.SubTileQuartet;
import folk.sisby.antique_atlas.gui.tiles.TileRenderIterator;
import folk.sisby.antique_atlas.util.ColorUtil;
import folk.sisby.antique_atlas.util.DrawBatcher;
import folk.sisby.antique_atlas.util.DrawUtil;
import folk.sisby.antique_atlas.util.MathUtil;
import folk.sisby.antique_atlas.util.Rect;
import folk.sisby.surveyor.PlayerSummary;
import folk.sisby.surveyor.client.SurveyorClient;
import folk.sisby.surveyor.landmark.Landmark;
import it.unimi.dsi.fastutil.objects.Reference2ObjectArrayMap;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.DyeColor;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.ColorHelper;
import net.minecraft.util.math.ColumnPos;
import net.minecraft.util.math.MathHelper;
import org.joml.Vector2d;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL11;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class AtlasScreen extends Component {
	public static final Identifier BOOK = AntiqueAtlas.id("textures/gui/book.png");
	public static final Identifier BOOK_FULLSCREEN = AntiqueAtlas.id("book_fullscreen");
	public static final Identifier BOOK_FULLSCREEN_M = AntiqueAtlas.id("middle/book_fullscreen_m");
	public static final Identifier BOOK_FULLSCREEN_R = AntiqueAtlas.id("book_fullscreen_r");
	public static final Identifier BOOK_FRAME = AntiqueAtlas.id("textures/gui/book_frame.png");
	public static final Identifier BOOK_FRAME_FULLSCREEN = AntiqueAtlas.id("book_frame_fullscreen");
	public static final Identifier BOOK_FRAME_FULLSCREEN_M = AntiqueAtlas.id("middle/book_frame_fullscreen_m");
	public static final Identifier BOOK_FRAME_FULLSCREEN_R = AntiqueAtlas.id("book_frame_fullscreen_r");
	public static final Identifier BOOK_FRAME_NARROW = AntiqueAtlas.id("textures/gui/book_frame_narrow.png");
	public static final Identifier BOOK_FRAME_NARROW_FULLSCREEN = AntiqueAtlas.id("book_frame_narrow_fullscreen");
	public static final Identifier BOOK_FRAME_NARROW_FULLSCREEN_M = AntiqueAtlas.id("middle/book_frame_narrow_fullscreen_m");
	public static final Identifier BOOK_FRAME_NARROW_FULLSCREEN_R = AntiqueAtlas.id("book_frame_narrow_fullscreen_r");
	public static final Identifier PLAYER = AntiqueAtlas.id("textures/gui/player.png");
	public static final Identifier ERASER = AntiqueAtlas.id("textures/gui/eraser.png");
	public static final Identifier ICON_ADD_MARKER = AntiqueAtlas.id("textures/gui/icons/add_marker.png");
	public static final Identifier ICON_DELETE_MARKER = AntiqueAtlas.id("textures/gui/icons/del_marker.png");
	public static final Identifier ICON_SHOW_MARKERS = AntiqueAtlas.id("textures/gui/icons/show_markers.png");
	public static final Identifier ICON_HIDE_MARKERS = AntiqueAtlas.id("textures/gui/icons/hide_markers.png");
	private static final Text TEXT_ADD_MARKER = Text.translatable("gui.antique_atlas.addMarker");
	private static final Text TEXT_ADD_MARKER_HERE = Text.translatable("gui.antique_atlas.addMarkerHere");

	public static final int MAP_BORDER_WIDTH = 17;
	public static final int MAP_BORDER_HEIGHT = 11;
	public static final float PLAYER_ROTATION_STEPS = 16;
	public static final int PLAYER_ICON_WIDTH = 7;
	public static final int PLAYER_ICON_HEIGHT = 8;
	private static final int BOOKMARK_SPACING = 2;
	public static final int MARKER_SIZE = 32;
	/**
	 * How much the map view is offset, in blocks, per click (or per tick).
	 */
	private static final int NAVIGATE_STEP = 24;

	public static final State<AtlasScreen> NORMAL = new ToggleState<>();
	public static final State<AtlasScreen> PLACING_MARKER = new ToggleState<>(s -> s.addMarkerBookmark);
	public static final State<AtlasScreen> DELETING_MARKER = new ToggleState<>(s -> s.deleteMarkerBookmark, s -> s.addChild(s.eraser), s -> s.removeChild(s.eraser));
	public static final State<AtlasScreen> HIDING_MARKERS = new ToggleState<>(s -> s.markerVisibilityBookmark, s -> {
		s.markerVisibilityBookmark.setTitle(Text.translatable("gui.antique_atlas.showMarkers"));
		s.markerVisibilityBookmark.setIconTexture(ICON_SHOW_MARKERS);
	}, s -> {
		s.clearTargetBookmarks(s.playerBookmark);
		s.markerVisibilityBookmark.setTitle(Text.translatable("gui.antique_atlas.hideMarkers"));
		s.markerVisibilityBookmark.setIconTexture(ICON_HIDE_MARKERS);
	});

	private final int bookWidth;
	private final int bookHeight;
	private final int mapWidth;
	private final int mapHeight;
	private final boolean fullscreen;

	/**
	 * Button for placing a marker at current position, local to this Atlas instance.
	 */
	private final BookmarkButton addMarkerBookmark;
	/**
	 * Button for deleting local markers.
	 */
	private final BookmarkButton deleteMarkerBookmark;
	/**
	 * Button for showing/hiding all markers.
	 */
	private final BookmarkButton markerVisibilityBookmark;
	/**
	 * Button for displaying the scale, and setting the scale to 1 chunk / 1 tile / 16px.
	 */
	private final TextBookmarkButton resetScaleBookmark;
	/**
	 * Button for restoring player's position at the center of the Atlas.
	 */
	private final BookmarkButton playerBookmark;
	private final ScrollBoxComponent markerScrollBox = new ScrollBoxComponent(true, BookmarkButton.HEIGHT + BOOKMARK_SPACING);
	private final MarkerModal markerModal = new MarkerModal();
	private final BlinkingMarkerComponent markerCursor = new BlinkingMarkerComponent();
	private final CursorComponent eraser = new CursorComponent();

	private final List<BookmarkButton> markerBookmarks = new ArrayList<>();
	private final ScreenState<AtlasScreen> state = new ScreenState<>((oldState, newState) -> AntiqueAtlas.lastState.switchTo(newState, this));
	private Landmark<?> hoveredLandmark = null;
	private PlayerSummary hoveredFriend = null;
	/**
	 * The button which is currently being pressed. Used for continuous
	 * navigation using the arrow buttons. Also used to prevent immediate
	 * canceling of placing marker.
	 */
	private ButtonComponent selectedButton = null;
	private PlayerEntity player;
	private WorldAtlasData worldAtlasData;
	private Integer targetOffsetX, targetOffsetY;
	private boolean isMouseOverMap = false;

	private boolean isDragging = false;

	private static double mapOffsetX;
	private static double mapOffsetY;

	private static int tilePixels = 16;
	private static int tileChunks = 1;
	private int mapScale;

	public AtlasScreen() {
		fullscreen = AntiqueAtlas.CONFIG.fullscreen;
		if (fullscreen) {
			bookWidth = (int) (MinecraftClient.getInstance().getWindow().getScaledWidth() * 0.9 - 40);
			bookHeight = (int) (MinecraftClient.getInstance().getWindow().getScaledHeight() * 0.9);
		} else {
			bookWidth = 310;
			bookHeight = 218;
		}
		setSize(bookWidth, bookHeight);
		mapWidth = bookWidth - MAP_BORDER_WIDTH * 2;
		mapHeight = bookHeight - MAP_BORDER_HEIGHT * 2;
		mapScale = getMapScale();

		playerBookmark = new BookmarkButton(Text.translatable("gui.antique_atlas.followPlayer"), AntiqueAtlas.id("textures/gui/player.png"), DyeColor.GRAY, null, 7, 8, false);
		addChild(playerBookmark).offsetGuiCoords(bookWidth - 10, bookHeight - MAP_BORDER_HEIGHT - BookmarkButton.HEIGHT - 10);
		playerBookmark.addListener(b -> {
			selectedButton = playerBookmark;
			clearTargetBookmarks(playerBookmark);
			playerBookmark.setSelected(true);
		});

		addMarkerBookmark = new BookmarkButton(TEXT_ADD_MARKER, ICON_ADD_MARKER, DyeColor.RED, null, 16, 16, false);
		addChild(addMarkerBookmark).offsetGuiCoords(bookWidth - 10, 14);
		addMarkerBookmark.addListener(button -> {
			if (state.is(PLACING_MARKER)) {
				selectedButton = null;
				state.switchTo(NORMAL, this);
			} else {
				selectedButton = button;
				state.switchTo(PLACING_MARKER, this);

				// While holding shift, we create a marker on the player's position
				if (hasShiftDown()) {
					markerModal.setMarkerData(player.getEntityWorld(), player.getBlockX(), player.getBlockZ());
					addChild(markerModal);

					markerCursor.setTexture(markerModal.selectedTexture.id(), markerModal.selectedTexture.textureWidth(), markerModal.selectedTexture.textureHeight());
					addChildBehind(markerModal, markerCursor).setGuiCoords((int) worldXToScreenX(player.getBlockX() - MARKER_SIZE / 2), (int) worldZToScreenY(player.getBlockZ() - MARKER_SIZE / 2));

					// Un-press all keys to prevent player from walking infinitely:
					KeyBinding.unpressAll();

					selectedButton = null;
					state.switchTo(NORMAL, this);
				}
			}
		});
		deleteMarkerBookmark = new BookmarkButton(Text.translatable("gui.antique_atlas.delMarker"), ICON_DELETE_MARKER, DyeColor.YELLOW, null, 16, 16, false);
		addChild(deleteMarkerBookmark).offsetGuiCoords(bookWidth - 10, 33);
		deleteMarkerBookmark.addListener(button -> {
			if (state.is(DELETING_MARKER)) {
				selectedButton = null;
				state.switchTo(NORMAL, this);
			} else {
				selectedButton = button;
				state.switchTo(DELETING_MARKER, this);
			}
		});
		markerVisibilityBookmark = new BookmarkButton(Text.translatable("gui.antique_atlas.hideMarkers"), ICON_HIDE_MARKERS, DyeColor.GREEN, null, 16, 16, false);
		addChild(markerVisibilityBookmark).offsetGuiCoords(bookWidth - 10, 52);
		markerVisibilityBookmark.addListener(button -> {
			selectedButton = null;
			if (state.is(HIDING_MARKERS)) {
				state.switchTo(NORMAL, this);
			} else {
				selectedButton = null;
				state.switchTo(HIDING_MARKERS, this);
			}
		});
		resetScaleBookmark = new TextBookmarkButton(Text.translatable("gui.antique_atlas.resetScale"), Text.of("1c"));
		addChild(resetScaleBookmark).offsetGuiCoords(bookWidth - 10, 71);
		resetScaleBookmark.addListener(button -> {
			resetZoom();
			resetScaleBookmark.setSelected(false);
		});

		addChild(markerScrollBox).setRelativeCoords(-14, MAP_BORDER_HEIGHT + 8);
		int markersOnScreen = (mapHeight - 20) / ((BookmarkButton.HEIGHT + BOOKMARK_SPACING) - BOOKMARK_SPACING);
		markerScrollBox.getViewport().setSize(BookmarkButton.WIDTH, markersOnScreen * (BookmarkButton.HEIGHT + BOOKMARK_SPACING) - BOOKMARK_SPACING);

		markerModal.addMarkerListener(markerCursor);

		eraser.setTexture(ERASER, 12, 14, 2, 11);

		state.switchTo(AntiqueAtlas.lastState.is(HIDING_MARKERS) ? HIDING_MARKERS : NORMAL, this);
	}

	private int getMapScale() {
		return switch (AntiqueAtlas.CONFIG.mapScale) {
			case -2 -> Math.max(1, (int) Math.floor(MinecraftClient.getInstance().getWindow().getScaleFactor() / 2.0));
			case -1 -> Math.max(1, (int) Math.ceil(MinecraftClient.getInstance().getWindow().getScaleFactor() / 2.0));
			case 0 -> (int) MinecraftClient.getInstance().getWindow().getScaleFactor();
			default -> AntiqueAtlas.CONFIG.mapScale;
		};
	}

	public AtlasScreen prepareToOpen() {
		MinecraftClient.getInstance().getSoundManager().play(PositionedSoundInstance.master(SoundEvents.ITEM_BOOK_PAGE_TURN, 1.0F));

		this.player = MinecraftClient.getInstance().player;
		updateAtlasData();
		if (!AntiqueAtlas.CONFIG.keepOffset) {
			playerBookmark.setSelected(true);
			setMapPosition(player.getBlockX(), player.getBlockZ());
		}
		if (!AntiqueAtlas.CONFIG.keepZoom) {
			resetZoom();
		}

		return this;
	}

	@Override
	public void init() {
		super.init();

		setGuiCoords((this.width - bookWidth) / 2, (this.height - bookHeight) / 2);

		updateScaleBookmark();
		updateBookmarkerList();
	}

	public void updateBookmarkerList() {
		markerScrollBox.getViewport().removeAllContent();
		markerScrollBox.setScrollPos(0);
		markerBookmarks.clear();

		if (worldAtlasData == null) return;

		worldAtlasData.getEditableLandmarks().forEach((landmark, texture) -> {
			BookmarkButton bookmark = new MarkerBookmarkButton(landmark.name(), texture, landmark.color(), true);

			bookmark.addListener(button -> {
				if (state.is(NORMAL)) {
					clearTargetBookmarks(bookmark);
					setTargetPosition(new ColumnPos(landmark.pos().getX(), landmark.pos().getZ()));
				} else if (state.is(DELETING_MARKER)) {
					if (!worldAtlasData.deleteLandmark(player.getEntityWorld(), landmark)) return;
					updateBookmarkerList();
					player.getEntityWorld().playSound(player, player.getBlockPos(), SoundEvents.UI_CARTOGRAPHY_TABLE_TAKE_RESULT, SoundCategory.AMBIENT, 1F, 0.5F);
					if (!hasShiftDown()) {
						state.switchTo(NORMAL, this);
					}
				}
			});

			markerBookmarks.add(bookmark);
		});

		final int[] contentY = {0};
		for (BookmarkButton bookmark : markerBookmarks) {
			markerScrollBox.getViewport().addContent(bookmark).setRelativeY(contentY[0]);
			contentY[0] += BookmarkButton.HEIGHT + BOOKMARK_SPACING;
		}
	}

	public void clearTargetBookmarks(BookmarkButton except) {
		if (playerBookmark != except) playerBookmark.setSelected(false);
		for (BookmarkButton bookmark : markerBookmarks) {
			if (bookmark != except) bookmark.setSelected(false);
		}
	}

	public void updateMouse(double mouseX, double mouseY) {
		double relativeMouseX = mouseX - getGuiX();
		double relativeMouseY = mouseY - getGuiY();
		isMouseOverMap = relativeMouseX >= MAP_BORDER_WIDTH && relativeMouseX <= MAP_BORDER_WIDTH + mapWidth && relativeMouseY >= MAP_BORDER_HEIGHT && relativeMouseY <= MAP_BORDER_HEIGHT + mapHeight;
	}

	@Override
	public void mouseMoved(double mouseX, double mouseY) {
		super.mouseMoved(mouseX, mouseY);
		updateMouse(mouseX, mouseY);
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int mouseState) {
		updateMouse(mouseX, mouseY);
		if (super.mouseClicked(mouseX, mouseY, mouseState)) return true;
		if (markerModal.getParent() != null) return false;

		// If clicked on the map, start dragging
		if (!state.is(NORMAL) && !state.is(HIDING_MARKERS)) {
			if (state.is(PLACING_MARKER) && isMouseOverMap && mouseState == GLFW.GLFW_MOUSE_BUTTON_1) {
				markerModal.setMarkerData(player.getEntityWorld(), screenXToWorldX(mouseX), screenYToWorldZ(mouseY));
				addChild(markerModal);

				markerCursor.setTexture(markerModal.selectedTexture.id(), MARKER_SIZE, MARKER_SIZE);
				addChildBehind(markerModal, markerCursor).setGuiCoords((int) mouseX - MARKER_SIZE / 2, (int) mouseY - MARKER_SIZE / 2);

				// Un-press all keys to prevent player from walking infinitely:
				KeyBinding.unpressAll();

				state.switchTo(NORMAL, this);
				return true;
			} else if (state.is(DELETING_MARKER) && hoveredLandmark != null && isMouseOverMap && mouseState == 0) {
				if (worldAtlasData.deleteLandmark(player.getEntityWorld(), hoveredLandmark)) {
					updateBookmarkerList();
					player.getEntityWorld().playSound(player, player.getBlockPos(), SoundEvents.UI_CARTOGRAPHY_TABLE_TAKE_RESULT, SoundCategory.AMBIENT, 1F, 0.5F);
				}
			}
			if (!hasShiftDown() || !state.is(DELETING_MARKER)) {
				state.switchTo(NORMAL, this);
			}
		} else if (isMouseOverMap && selectedButton == null) {
			isDragging = true;
			return true;
		}

		return false;
	}

	@Override
	public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
		if (keyCode == GLFW.GLFW_KEY_UP) {
			navigateMap(0, NAVIGATE_STEP);
		} else if (keyCode == GLFW.GLFW_KEY_DOWN) {
			navigateMap(0, -NAVIGATE_STEP);
		} else if (keyCode == GLFW.GLFW_KEY_LEFT) {
			navigateMap(NAVIGATE_STEP, 0);
		} else if (keyCode == GLFW.GLFW_KEY_RIGHT) {
			navigateMap(-NAVIGATE_STEP, 0);
		} else if (keyCode == GLFW.GLFW_KEY_EQUAL || keyCode == GLFW.GLFW_KEY_KP_ADD) {
			zoomIn(true, (16 << AntiqueAtlas.CONFIG.maxTilePixels));
		} else if (keyCode == GLFW.GLFW_KEY_MINUS || keyCode == GLFW.GLFW_KEY_KP_SUBTRACT) {
			zoomOut(true, (1 << AntiqueAtlas.CONFIG.maxTileChunks));
		} else if (keyCode == GLFW.GLFW_KEY_ESCAPE || (AntiqueAtlasKeybindings.ATLAS_KEYMAPPING.matchesKey(keyCode, scanCode) && this.markerModal.getParent() == null)) {
			close();
		} else {
			return super.keyPressed(keyCode, scanCode, modifiers);
		}

		return true;
	}

	private double getPixelsPerBlock() {
		return ((double) mapScale / MinecraftClient.getInstance().getWindow().getScaleFactor()) * ((double) tilePixels) / ((double) tileChunks * 16.0);
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double dx, double dy) {
		updateMouse(mouseX, mouseY);
		if (super.mouseScrolled(mouseX, mouseY, dx, dy)) return true;
		if (markerModal.getParent() == null && dy != 0) {
			int direction = dy > 0 ? 1 : -1;
			if ((dy > 0 ? zoomIn(true, (16 << AntiqueAtlas.CONFIG.maxTilePixels)) : zoomOut(true, 1 << AntiqueAtlas.CONFIG.maxTileChunks)) && (isMouseOverMap || isDragging)) { // Keep mouse over the same block.
				double xOffset = (getGuiX() + MAP_BORDER_WIDTH + (double) mapWidth / 2 - mouseX) * direction;
				double yOffset = (getGuiY() + MAP_BORDER_HEIGHT + (double) mapHeight / 2 - mouseY) * direction;
				if (Math.abs(xOffset) > 5 || Math.abs(yOffset) > 5) { // Stay centered if mouse is roughly in the center (e.g. on a centered player pin)
					mapOffsetX += xOffset / (direction < 0 ? 2.0 : 1.0);
					mapOffsetY += yOffset / (direction < 0 ? 2.0 : 1.0);
					clearTargetBookmarks(null);
				}
			}
			return true;
		}
		return false;
	}

	@Override
	public boolean mouseReleased(double mouseX, double mouseY, int mouseState) {
		boolean result = false;
		if (mouseState != -1) {
			result = selectedButton != null || isDragging;
			selectedButton = null;
			isDragging = false;
		}
		return super.mouseReleased(mouseX, mouseY, mouseState) || result;
	}

	@Override
	public boolean mouseDragged(double mouseX, double mouseY, int lastMouseButton, double deltaX, double deltaY) {
		boolean result = false;
		if (isDragging) {
			clearTargetBookmarks(null);
			mapOffsetX += deltaX;
			mapOffsetY += deltaY;
			result = true;
		}
		return super.mouseDragged(mouseX, mouseY, lastMouseButton, deltaX, deltaY) || result;
	}

	@Override
	public void tick() {
		super.tick();
		if (player == null) return;

		if (playerBookmark.isSelected() && (mapOffsetX != -player.getBlockX() * getPixelsPerBlock() || mapOffsetY != -player.getBlockZ() * getPixelsPerBlock())) {
			setTargetPosition(new ColumnPos(player.getBlockX(), player.getBlockZ()));
		}

		if (targetOffsetX != null) {
			if (Math.abs(getTargetPositionX() - mapOffsetX) > NAVIGATE_STEP) {
				softNavigateMap(getTargetPositionX() > mapOffsetX ? NAVIGATE_STEP : -NAVIGATE_STEP, 0);
			} else {
				mapOffsetX = getTargetPositionX();
				targetOffsetX = null;
			}
		}

		if (targetOffsetY != null) {
			if (Math.abs(getTargetPositionY() - mapOffsetY) > NAVIGATE_STEP) {
				softNavigateMap(0, getTargetPositionY() > mapOffsetY ? NAVIGATE_STEP : -NAVIGATE_STEP);
			} else {
				mapOffsetY = getTargetPositionY();
				targetOffsetY = null;
			}
		}
	}

	private void updateAtlasData() {
		if (MinecraftClient.getInstance().world != null) {
			worldAtlasData = WorldAtlasData.getOrCreate(MinecraftClient.getInstance().world);
		}
	}

	private void navigateMap(int dx, int dy) {
		mapOffsetX += dx;
		mapOffsetY += dy;
		clearTargetBookmarks(null);
	}

	private void softNavigateMap(int dx, int dy) {
		mapOffsetX += dx;
		mapOffsetY += dy;
	}

	private void setMapPosition(int x, int z) {
		mapOffsetX = (int) (-x * getPixelsPerBlock());
		mapOffsetY = (int) (-z * getPixelsPerBlock());
	}

	private void setTargetPosition(ColumnPos pos) {
		targetOffsetX = pos.x();
		targetOffsetY = pos.z();
	}

	private double getTargetPositionX() {
		return -targetOffsetX * getPixelsPerBlock();
	}

	private double getTargetPositionY() {
		return -targetOffsetY * getPixelsPerBlock();
	}

	private void updateScaleBookmark() {
		int tileSizeBlocks = (tileChunks * 16 * 16) / tilePixels;
		int defaultTileSizeBlocks = 16;
		int rulerSizeBlocks = (tileSizeBlocks * (int) MinecraftClient.getInstance().getWindow().getScaleFactor()) / mapScale;
		resetScaleBookmark.setLabel(Text.literal(
			rulerSizeBlocks == 16 | rulerSizeBlocks >= 32 ? "%dc".formatted(rulerSizeBlocks / 16) : "%db".formatted(rulerSizeBlocks)).formatted(
			tileSizeBlocks < defaultTileSizeBlocks ? Formatting.DARK_RED : tileSizeBlocks == defaultTileSizeBlocks ? Formatting.BLACK : Formatting.DARK_BLUE
		));
	}

	private boolean zoomIn(boolean playSound, int maxTilePixels) {
		if (tileChunks == 1) {
			if (tilePixels >= maxTilePixels) return false;
			tilePixels <<= 1;
			if (playSound) MinecraftClient.getInstance().getSoundManager().play(PositionedSoundInstance.master(SoundEvents.ITEM_SPYGLASS_USE, 1.0F));
		} else {
			tileChunks >>= 1;
			if (playSound) MinecraftClient.getInstance().getSoundManager().play(PositionedSoundInstance.master(SoundEvents.ITEM_BOOK_PAGE_TURN, 1.0F));
		}
		mapOffsetX *= 2;
		mapOffsetY *= 2;
		updateScaleBookmark();
		return true;
	}

	private boolean zoomOut(boolean playSound, int maxTileChunks) {
		if (tilePixels == 16) {
			if (tileChunks >= maxTileChunks) return false;
			tileChunks <<= 1;
			if (playSound) MinecraftClient.getInstance().getSoundManager().play(PositionedSoundInstance.master(SoundEvents.ITEM_BOOK_PAGE_TURN, 1.0F));
		} else {
			tilePixels >>= 1;
			if (playSound) MinecraftClient.getInstance().getSoundManager().play(PositionedSoundInstance.master(SoundEvents.ITEM_SPYGLASS_USE, 1.0F));
		}
		mapOffsetX /= 2;
		mapOffsetY /= 2;
		updateScaleBookmark();
		return true;
	}

	private void resetZoom() {
		if (zoomIn(true, 8)) {
			while (zoomIn(false, 8)) ;
		} else if (zoomOut(true, 1)) {
			while (zoomOut(false, 1)) ;
		}
	}

	@Override
	public void render(DrawContext context, int mouseX, int mouseY, float partialTick) {
		super.renderBackground(context, mouseX, mouseY, partialTick);
		mapScale = getMapScale();
		RenderSystem.setShaderColor(1, 1, 1, 1);

		if (fullscreen) {
			int left_width = bookWidth / 2 - 15;
			context.drawGuiTexture(BOOK_FULLSCREEN, getGuiX(), getGuiY(), left_width, bookHeight);
			context.drawGuiTexture(BOOK_FULLSCREEN_M, getGuiX() + left_width, getGuiY(), 29, bookHeight);
			context.drawGuiTexture(BOOK_FULLSCREEN_R, getGuiX() + left_width + 29, getGuiY(), left_width + 1, bookHeight);
		} else {
			context.drawTexture(BOOK, getGuiX(), getGuiY(), 0, 0, bookWidth, bookHeight, bookWidth, bookHeight);
		}

		if (worldAtlasData == null) return;

		double guiScale = client.getWindow().getScaleFactor();
		RenderSystem.enableScissor(
			(int) (guiScale * (getGuiX() + MAP_BORDER_WIDTH)),
			(int) (guiScale * (getGuiY() + MAP_BORDER_HEIGHT)),
			(int) (guiScale * mapWidth),
			(int) (guiScale * mapHeight)
		);
		RenderSystem.enableBlend();
		RenderSystem.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

		// 'roundToBase' is required so that when the map scales below the threshold the tiles don't change when map position changes.
		// The +-2 at the end provide margin so that tiles at the edges of the page have their stitched texture correct.
		int mapStartChunkX = MathUtil.roundToBase(screenXToWorldX(getGuiX()) >> 4, tileChunks) - 2 * tileChunks;
		int mapStartChunkZ = MathUtil.roundToBase(screenYToWorldZ(getGuiY()) >> 4, tileChunks) - 2 * tileChunks;
		int mapEndChunkX = MathUtil.roundToBase(screenXToWorldX(getGuiX() + bookWidth) >> 4, tileChunks) + 2 * tileChunks;
		int mapEndChunkZ = MathUtil.roundToBase(screenYToWorldZ(getGuiY() + bookHeight) >> 4, tileChunks) + 2 * tileChunks;
		double mapStartScreenX = worldXToScreenX(mapStartChunkX << 4);
		double mapStartScreenY = worldZToScreenY(mapStartChunkZ << 4);
		TileRenderIterator tiles = new TileRenderIterator(worldAtlasData);
		tiles.setScope(new Rect(mapStartChunkX, mapStartChunkZ, mapEndChunkX, mapEndChunkZ));
		tiles.setStep(tileChunks);
		
		RenderSystem.setShaderColor(1, 1, 1, state.is(DELETING_MARKER) ? 0.5f : 1.0f);
		renderTiles(context.getMatrices(), null, getGuiX() + MAP_BORDER_WIDTH, getGuiY() + MAP_BORDER_HEIGHT, 0, mapWidth, mapHeight, mapStartScreenX, mapStartScreenY, mapScale, tilePixels, guiScale, 15728640, tiles);
		RenderSystem.setShaderColor(1, 1, 1, 1);

		// Overlay the frame so that edges of the map are smooth:
		if (fullscreen) {
			int left_width = bookWidth / 2 - 15;
			context.drawGuiTexture(BOOK_FRAME_FULLSCREEN, getGuiX(), getGuiY(), left_width, bookHeight);
			context.drawGuiTexture(BOOK_FRAME_FULLSCREEN_M, getGuiX() + left_width, getGuiY(), 29, bookHeight);
			context.drawGuiTexture(BOOK_FRAME_FULLSCREEN_R, getGuiX() + left_width + 29, getGuiY(), left_width + 1, bookHeight);
		} else {
			context.drawTexture(BOOK_FRAME, getGuiX(), getGuiY(), 0, 0, bookWidth, bookHeight, bookWidth, bookHeight);
		}
		context.getMatrices().push();
		context.getMatrices().translate(getGuiX(), getGuiY(), 0);
		float markerScale = (float) (((double) tilePixels * mapScale / (guiScale * 16.0)));

		Map<UUID, PlayerSummary> friends = SurveyorClient.getFriends();

		hoveredLandmark = null;
		hoveredFriend = null;
		if (!state.is(HIDING_MARKERS)) {
			if (isMouseOverMap) {
				double bestDistance = Double.MAX_VALUE;
				for (Map.Entry<Landmark<?>, MarkerTexture> entry : worldAtlasData.getAllMarkers(tileChunks).entrySet()) {
					Landmark<?> landmark = entry.getKey();
					MarkerTexture texture = entry.getValue();
					double markerX = worldXToScreenX(landmark.pos().getX());
					double markerY = worldZToScreenY(landmark.pos().getZ());
					Vector2d markerCenter = texture.getCenter(tileChunks);
					double squaredDistance = Vector2d.distanceSquared(markerX + markerScale * markerCenter.x, markerY + markerScale * markerCenter.y, mouseX, mouseY);
					if (squaredDistance > 0 && squaredDistance < bestDistance && squaredDistance < (texture.getSquaredSize(tileChunks) * markerScale * markerScale) / 4.0) {
						bestDistance = squaredDistance;
						hoveredLandmark = landmark;
					}
				}
				for (PlayerSummary friend : friends.values()) {
					double markerX = worldXToScreenX(friend.pos().getX());
					double markerY = worldZToScreenY(friend.pos().getZ());
					double squaredDistance = Vector2d.distanceSquared(markerX, markerY, mouseX, mouseY);
					if (squaredDistance > 0 && squaredDistance < bestDistance && squaredDistance < (PLAYER_ICON_HEIGHT * PLAYER_ICON_WIDTH * 1.5) / 4.0) {
						bestDistance = squaredDistance;
						hoveredFriend = friend;
						hoveredLandmark = null;
					}
				}
			}
			worldAtlasData.getAllMarkers(tileChunks).forEach((landmark, texture) -> {
				renderMarker(context, landmark, texture, WorldAtlasData.landmarkIsEditable(landmark), hoveredLandmark == landmark && markerModal.getParent() == null, markerScale);
			});
		}

		context.getMatrices().pop();

		RenderSystem.disableScissor();

		if (fullscreen) {
			int left_width = bookWidth / 2 - 15;
			context.drawGuiTexture(BOOK_FRAME_NARROW_FULLSCREEN, getGuiX(), getGuiY(), left_width, bookHeight);
			context.drawGuiTexture(BOOK_FRAME_NARROW_FULLSCREEN_M, getGuiX() + left_width, getGuiY(), 29, bookHeight);
			context.drawGuiTexture(BOOK_FRAME_NARROW_FULLSCREEN_R, getGuiX() + left_width + 29, getGuiY(), left_width + 1, bookHeight);
		} else {
			context.drawTexture(BOOK_FRAME_NARROW, getGuiX(), getGuiY(), 0, 0, bookWidth, bookHeight, bookWidth, bookHeight);
		}

		markerScrollBox.getViewport().setHidden(state.is(HIDING_MARKERS));

		context.getMatrices().push();
		context.getMatrices().translate(getGuiX(), getGuiY(), 0);
		PlayerSummary playerSummary = friends.remove(SurveyorClient.getClientUuid());
		Map<UUID, PlayerSummary> orderedFriends = new LinkedHashMap<>(friends);
		if (playerSummary != null) orderedFriends.put(SurveyorClient.getClientUuid(), playerSummary);
		orderedFriends.forEach((uuid, friend) -> {
			if (state.is(HIDING_MARKERS) && (!playerBookmark.isSelected() || friend != playerSummary)) return;
			renderPlayer(context, friend, 1, hoveredFriend == friend && markerModal.getParent() == null, friend == playerSummary);
		});
		context.getMatrices().pop();

		super.render(context, mouseX, mouseY, partialTick);

		RenderSystem.enableBlend();
		RenderSystem.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
		if (state.is(PLACING_MARKER)) {
			RenderSystem.setShaderColor(1, 1, 1, 0.5f);
			context.drawTexture(markerModal.selectedTexture.id(), mouseX + markerModal.selectedTexture.offsetX(), mouseY + markerModal.selectedTexture.offsetY(), 0, 0, markerModal.selectedTexture.textureWidth(), markerModal.selectedTexture.textureHeight(), markerModal.selectedTexture.textureWidth(), markerModal.selectedTexture.textureHeight());
			RenderSystem.setShaderColor(1, 1, 1, 1);
		}
		RenderSystem.disableBlend();

		addMarkerBookmark.setTitle(hasShiftDown() ? TEXT_ADD_MARKER_HERE : TEXT_ADD_MARKER);

		if (AntiqueAtlas.CONFIG.debugRender && !isDragging && isMouseOverMap && markerModal.getParent() == null) {
			int x = screenXToWorldX((int) getMouseX());
			int z = screenYToWorldZ((int) getMouseY());
			ChunkPos pos = new ChunkPos(new BlockPos(x, 0, z));
			context.drawText(textRenderer, Text.literal("%d,%d (%d,%d)".formatted(pos.x, pos.z, x, z)), getGuiX(), getGuiY() - 12, 0xFFFFFFFF, true);
			if (hoveredLandmark != null) {
				MarkerTexture texture = worldAtlasData.getMarkerTexture(hoveredLandmark);
				context.drawText(textRenderer, Text.literal(hoveredLandmark.type().id().toString()), getGuiX() + bookWidth - textRenderer.getWidth(Text.literal(hoveredLandmark.type().id().toString())), getGuiY() - 12, 0xFFFFFFFF, true);
				if (hoveredLandmark instanceof AtlasStructureLandmark sLandmark) context.drawText(textRenderer, Text.literal(sLandmark.displayId().toString()), getGuiX(), getGuiY() + bookHeight, 0xFFFFFFFF, true);
				if (texture != null) context.drawText(textRenderer, Text.literal(texture.displayId()), getGuiX() + bookWidth - textRenderer.getWidth(Text.literal(texture.displayId())), getGuiY() + bookHeight, 0xFFFFFFFF, true);
			} else {
				TileTexture texture = worldAtlasData.getTile(pos);
				Identifier providerId = worldAtlasData.getProvider(pos);
				String predicate = worldAtlasData.getTilePredicate(pos);
				if (texture != null) {
					if (predicate != null) context.drawText(textRenderer, Text.literal(predicate), getGuiX() + bookWidth - textRenderer.getWidth(Text.literal(predicate)), getGuiY() - 12, 0xFFFFFFFF, true);
					context.drawText(textRenderer, Text.literal(providerId.toString()), getGuiX(), getGuiY() + bookHeight, 0xFFFFFFFF, true);
					context.drawText(textRenderer, Text.literal(texture.displayId()), getGuiX() + bookWidth - textRenderer.getWidth(Text.literal(texture.displayId())), getGuiY() + bookHeight, 0xFFFFFFFF, true);
				}
			}
		}
	}

	public static void renderTiles(MatrixStack matrices, VertexConsumerProvider vertexConsumers, int mapX, int mapY, int z, int mapWidth, int mapHeight, double mapStartScreenX, double mapStartScreenY, double mapScale, int pixelsPerTile, double guiScale, int light, TileRenderIterator tiles) {
		matrices.push();
		matrices.translate(mapStartScreenX, mapStartScreenY, 0);
		matrices.scale((float) (mapScale / guiScale), (float) (mapScale / guiScale), 1.0F);

		Map<TileTexture, Collection<SubTile>> tileTextures = new Reference2ObjectArrayMap<>();
		for (SubTileQuartet subTiles : tiles) {
			for (SubTile subtile : subTiles) {
				if (subtile == null || subtile.texture == null) continue;
				tileTextures.computeIfAbsent(subtile.texture, k -> new ArrayList<>()).add(subtile.copy());
			}
		}
		int subTilePixels = pixelsPerTile / 2;
		tileTextures.forEach((texture, subtiles) -> {
			try (DrawBatcher batcher = new DrawBatcher(matrices, vertexConsumers, texture.id(), 32, 48, light)) {
				for (SubTile subtile : subtiles) {
					int drawX = subtile.x * subTilePixels;
					int drawY = subtile.y * subTilePixels;
					// a non-scope bounds check allows subtile-level accuracy, and keeps border tiling accurate.
					if (drawX * (guiScale / mapScale) > mapX + mapWidth - mapStartScreenX || drawY * (guiScale / mapScale) > mapY + mapHeight - mapStartScreenY || (drawX + subTilePixels) * (guiScale / mapScale) < mapX - mapStartScreenX || (drawY + subTilePixels) * (guiScale / mapScale) < mapY - mapStartScreenY) continue;
					batcher.add(drawX, drawY, z, subTilePixels, subTilePixels, subtile.getTextureU() * 8, subtile.getTextureV() * 8, 8, 8, 0xFFFFFFFF);
				}
			}
		});

		matrices.pop();
	}

	private void renderPlayer(DrawContext context, PlayerSummary player, float iconScale, boolean hovering, boolean self) {
		double playerOffsetX = worldXToScreenX(player.pos().getX()) - getGuiX();
		double playerOffsetY = worldZToScreenY(player.pos().getZ()) - getGuiY();

		playerOffsetX = MathHelper.clamp(playerOffsetX, MAP_BORDER_WIDTH, mapWidth + MAP_BORDER_WIDTH);
		playerOffsetY = MathHelper.clamp(playerOffsetY, MAP_BORDER_HEIGHT, mapHeight + MAP_BORDER_HEIGHT);

		// Draw the icon:
		float tint = (player.online() ? 1 : 0.5f) * (hovering ? 0.9f : 1);
		float greenTint = self ? 1 : 0.7f;
		int argb = ColorHelper.Argb.getArgb(state.is(PLACING_MARKER) ? 127 : 255, (int) (tint * 255), (int) (tint * greenTint * 255), (int) (tint * 255));
		float playerRotation = ((float) Math.round(player.yaw() / 360f * PLAYER_ROTATION_STEPS) / PLAYER_ROTATION_STEPS) * 360f;

		DrawUtil.drawCenteredWithRotation(context.getMatrices(), null, PLAYER, playerOffsetX, playerOffsetY, 0, iconScale, PLAYER_ICON_WIDTH, PLAYER_ICON_HEIGHT, playerRotation, 15728640, argb);

		if (hovering && !self) {
			context.drawTooltip(textRenderer, Text.literal(player.username()).formatted(player.online() ? Formatting.LIGHT_PURPLE : Formatting.GRAY), (int) getMouseX() - getGuiX(), (int) getMouseY() - getGuiY());
		}
	}

	private void renderMarker(DrawContext context, Landmark<?> landmark, MarkerTexture texture, boolean editable, boolean hovering, float markerScale) {
		double markerX = worldXToScreenX(landmark.pos().getX()) - getGuiX();
		double markerY = worldZToScreenY(landmark.pos().getZ()) - getGuiY();

		float tint = hovering ? 0.8f : 1.0f;
		float alpha = state.is(PLACING_MARKER) || (state.is(DELETING_MARKER) && !editable) || (editable && markerX <= MAP_BORDER_WIDTH || markerX >= mapWidth + MAP_BORDER_WIDTH || markerY <= MAP_BORDER_HEIGHT || markerY >= mapHeight + MAP_BORDER_HEIGHT) ? 0.5f : 1.0f;

		if (editable) {
			markerX = MathHelper.clamp(markerX, MAP_BORDER_WIDTH, mapWidth + MAP_BORDER_WIDTH);
			markerY = MathHelper.clamp(markerY, MAP_BORDER_HEIGHT, mapHeight + MAP_BORDER_HEIGHT);
		}

		DyeColor color = landmark.color();
		texture.draw(context, markerX, markerY, markerScale, tileChunks, color == null ? null : ColorUtil.getColorFromArgb(color.getEntityColor()), tint, alpha);

		if (hovering && landmark.name() != null && !landmark.name().getString().isEmpty()) {
			context.drawTooltip(textRenderer, landmark.name(), (int) getMouseX() - getGuiX(), (int) getMouseY() - getGuiY());
		}
	}

	@Override
	public boolean shouldPause() {
		return false;
	}

	@Override
	public void close() {
		super.close();
		markerModal.closeChild();
		removeChild(markerCursor);
	}

	private int screenXToWorldX(double screenX) {
		return screenXToWorldX(screenX, getGuiX(), mapOffsetX, mapWidth, getPixelsPerBlock());
	}

	private int screenYToWorldZ(double screenY) {
		return screenYToWorldZ(screenY, getGuiY(), mapOffsetY, mapHeight, getPixelsPerBlock());
	}

	private double worldXToScreenX(double x) {
		return worldXToScreenX(x, getGuiX(), mapOffsetX, mapWidth, getPixelsPerBlock());
	}

	private double worldZToScreenY(double z) {
		return worldZToScreenY(z, getGuiY(), mapOffsetY, mapHeight, getPixelsPerBlock());
	}

	public static int screenXToWorldX(double screenX, int bookX, double mapOffsetX, int mapWidth, double pixelsPerBlock) {
		double mapX = (int) Math.round(screenX - bookX - MAP_BORDER_WIDTH);
		return (int) Math.round((mapX - (mapWidth / 2f) - mapOffsetX) / pixelsPerBlock);
	}

	public static int screenYToWorldZ(double screenY, int bookY, double mapOffsetY, int mapHeight, double pixelsPerBlock) {
		double mapY = (int) Math.round(screenY - bookY - MAP_BORDER_HEIGHT);
		return (int) Math.round((mapY - (mapHeight / 2f) - mapOffsetY) / pixelsPerBlock);
	}

	public static double worldXToScreenX(double x, int bookX, double mapOffsetX, int mapWidth, double pixelsPerBlock) {
		double mapX = x * pixelsPerBlock + mapOffsetX + (mapWidth / 2f);
		return mapX + bookX + MAP_BORDER_WIDTH;
	}

	public static double worldZToScreenY(double z, int bookY, double mapOffsetY, int mapHeight, double pixelsPerBlock) {
		double mapY = z * pixelsPerBlock + mapOffsetY + (mapHeight / 2f);
		return mapY + bookY + MAP_BORDER_HEIGHT;
	}

	@Override
	protected void onChildClosed(Component child) {
		if (child.equals(markerModal)) {
			removeChild(markerCursor);
		}
	}

	public WorldAtlasData getworldAtlasData() {
		return worldAtlasData;
	}
}
