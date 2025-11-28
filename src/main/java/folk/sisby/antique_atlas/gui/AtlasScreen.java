package folk.sisby.antique_atlas.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import folk.sisby.antique_atlas.AntiqueAtlas;
import folk.sisby.antique_atlas.AntiqueAtlasKeybindings;
import folk.sisby.antique_atlas.MarkerTexture;
import folk.sisby.antique_atlas.TileTexture;
import folk.sisby.antique_atlas.WorldAtlasData;
import folk.sisby.antique_atlas.gui.core.ButtonComponent;
import folk.sisby.antique_atlas.gui.core.Component;
import folk.sisby.antique_atlas.gui.core.CursorComponent;
import folk.sisby.antique_atlas.gui.core.ScreenState;
import folk.sisby.antique_atlas.gui.core.ScrollBoxComponent;
import folk.sisby.surveyor.PlayerSummary;
import folk.sisby.surveyor.client.SurveyorClient;
import folk.sisby.surveyor.landmark.Landmark;
import folk.sisby.surveyor.landmark.component.LandmarkComponentTypes;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.DyeColor;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.ColumnPos;
import org.joml.Vector2d;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL11;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.BiFunction;
import java.util.stream.Stream;

public class AtlasScreen extends Component implements AtlasRenderer {
	// Atlas Renderer
	public final int bookWidth;
	public final int bookHeight;
	public final int mapWidth;
	public final int mapHeight;
	public static double mapOffsetX;
	public static double mapOffsetY;
	public static int tilePixels = 16;
	public static int tileChunks = 1;
	public int mapScale;
	public PlayerEntity player;
	public WorldAtlasData worldAtlasData;

	// Screen Components
	public final BookmarkButton addMarkerBookmark; // Button for placing a marker at current position, local to this Atlas instance.
	public final BookmarkButton deleteMarkerBookmark; // Button for deleting local markers.
	public final BookmarkButton markerVisibilityBookmark; // Button for showing/hiding all markers.
	public final TextBookmarkButton resetScaleBookmark; // Button for displaying the scale, and setting the scale to 1 chunk / 1 tile / 16px.
	public final BookmarkButton playerBookmark; // Button for restoring player's position at the center of the Atlas.
	public final ScrollBoxComponent markerScrollBox = new ScrollBoxComponent(true, BookmarkButton.HEIGHT + BOOKMARK_SPACING);
	public final MarkerModal markerModal = new MarkerModal();
	public final BlinkingMarkerComponent markerCursor = new BlinkingMarkerComponent();
	public final CursorComponent eraser = new CursorComponent();
	public final List<BookmarkButton> markerBookmarks = new ArrayList<>();

	// Screen State
	public final ScreenState<AtlasScreen> state = new ScreenState<>((oldState, newState) -> AntiqueAtlas.lastState.switchTo(newState, this));
	public Landmark hoveredLandmark = null;
	public PlayerSummary hoveredFriend = null;
	public ButtonComponent selectedButton = null; // prevents marker being cancelled right after being pressed
	public Integer targetOffsetX, targetOffsetY; // only screen has smooth scrolling
	public boolean isMouseOverMap = false;
	public boolean isDragging = false;
	public final boolean fullscreen;

	public AtlasScreen() {
		fullscreen = AntiqueAtlas.CONFIG.fullscreen;
		if (fullscreen) {
			bookWidth = (int) (MinecraftClient.getInstance().getWindow().getScaledWidth() * 0.9 - 40);
			bookHeight = (int) (MinecraftClient.getInstance().getWindow().getScaledHeight() * 0.9);
		} else {
			bookWidth = DEFAULT_BOOK_WIDTH;
			bookHeight = DEFAULT_BOOK_HEIGHT;
		}
		setSize(bookWidth, bookHeight);
		mapWidth = bookWidth - MAP_BORDER_WIDTH * 2;
		mapHeight = bookHeight - MAP_BORDER_HEIGHT * 2;
		mapScale = calculateMapScale();

		playerBookmark = new BookmarkButton(Text.translatable("gui.antique_atlas.followPlayer"), AntiqueAtlas.id("textures/gui/player.png"), DyeColor.GRAY.getFireworkColor(), null, 7, 8, false);
		addChild(playerBookmark).offsetGuiCoords(bookWidth - 10, bookHeight - MAP_BORDER_HEIGHT - BookmarkButton.HEIGHT - 10);
		playerBookmark.addListener(b -> {
			selectedButton = playerBookmark;
			clearTargetBookmarks(playerBookmark);
			playerBookmark.setSelected(true);
		});

		addMarkerBookmark = new BookmarkButton(TEXT_ADD_MARKER, ICON_ADD_MARKER, DyeColor.RED.getFireworkColor(), null, 16, 16, false);
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
		deleteMarkerBookmark = new BookmarkButton(Text.translatable("gui.antique_atlas.delMarker"), ICON_DELETE_MARKER, DyeColor.YELLOW.getFireworkColor(), null, 16, 16, false);
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
		markerVisibilityBookmark = new BookmarkButton(Text.translatable("gui.antique_atlas.hideMarkers"), ICON_HIDE_MARKERS, DyeColor.GREEN.getFireworkColor(), null, 16, 16, false);
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

		for (Identifier id : overlays.keySet()) {
			overlays.get(id).onScreenInit(this);
		}
	}


	public int calculateMapScale() {
		return switch (AntiqueAtlas.CONFIG.mapScale) {
			case -2 -> Math.max(1, (int) Math.floor(guiScale() / 2.0));
			case -1 -> Math.max(1, (int) Math.ceil(guiScale() / 2.0));
			case 0 -> (int) guiScale();
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
			BookmarkButton bookmark = new MarkerBookmarkButton(landmark.getOrDefault(LandmarkComponentTypes.NAME, Text.literal(landmark.id().getPath())), texture, landmark.getOrDefault(LandmarkComponentTypes.COLOR, 0xFFFFFF), true);

			bookmark.addListener(button -> {
				if (state.is(NORMAL)) {
					clearTargetBookmarks(bookmark);
					setTargetPosition(new ColumnPos(landmark.getOrDefault(LandmarkComponentTypes.POS, BlockPos.ORIGIN).getX(), landmark.getOrDefault(LandmarkComponentTypes.POS, BlockPos.ORIGIN).getZ()));
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
		if (markerModal.getParent() != null) {
			return markerModal.mouseClicked(mouseX, mouseY, mouseState);
		}
		if (super.mouseClicked(mouseX, mouseY, mouseState)) return true;

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

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double wheelMove) {
		updateMouse(mouseX, mouseY);
		if (super.mouseScrolled(mouseX, mouseY, wheelMove)) return true;
		if (markerModal.getParent() == null && wheelMove != 0) {
			int direction = wheelMove > 0 ? 1 : -1;
			if ((wheelMove > 0 ? zoomIn(true, (16 << AntiqueAtlas.CONFIG.maxTilePixels)) : zoomOut(true, 1 << AntiqueAtlas.CONFIG.maxTileChunks)) && (isMouseOverMap || isDragging)) { // Keep mouse over the same block.
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

	public void updateAtlasData() {
		if (MinecraftClient.getInstance().world != null) {
			worldAtlasData = WorldAtlasData.getOrCreate(MinecraftClient.getInstance().world);
		}
	}

	public void navigateMap(int dx, int dy) {
		mapOffsetX += dx;
		mapOffsetY += dy;
		clearTargetBookmarks(null);
	}

	public void softNavigateMap(int dx, int dy) {
		mapOffsetX += dx;
		mapOffsetY += dy;
	}

	public void setMapPosition(int x, int z) {
		mapOffsetX = (int) (-x * getPixelsPerBlock());
		mapOffsetY = (int) (-z * getPixelsPerBlock());
	}

	public void setTargetPosition(ColumnPos pos) {
		targetOffsetX = pos.x();
		targetOffsetY = pos.z();
	}

	public double getTargetPositionX() {
		return -targetOffsetX * getPixelsPerBlock();
	}

	public double getTargetPositionY() {
		return -targetOffsetY * getPixelsPerBlock();
	}

	public void updateScaleBookmark() {
		int tileSizeBlocks = (tileChunks * 16 * 16) / tilePixels;
		int defaultTileSizeBlocks = 16;
		int rulerSizeBlocks = (int)(tileSizeBlocks / getEffectiveScale());
		resetScaleBookmark.setLabel(Text.literal(
			rulerSizeBlocks == 16 | rulerSizeBlocks >= 32 ? "%dc".formatted(rulerSizeBlocks / 16) : "%db".formatted(rulerSizeBlocks)).formatted(
			tileSizeBlocks < defaultTileSizeBlocks ? Formatting.DARK_RED : tileSizeBlocks == defaultTileSizeBlocks ? Formatting.BLACK : Formatting.DARK_BLUE
		));
	}

	public boolean zoomIn(boolean playSound, int maxTilePixels) {
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

	public boolean zoomOut(boolean playSound, int maxTileChunks) {
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

	public void resetZoom() {
		if (zoomIn(true, 8)) {
			while (zoomIn(false, 8)) ;
		} else if (zoomOut(true, 1)) {
			while (zoomOut(false, 1)) ;
		}
	}

	@Override
	public void render(DrawContext context, int mouseX, int mouseY, float partialTick) {
		int trueMouseX = mouseX;
		int trueMouseY = mouseY;
		if (markerModal.getParent() != null) {
			mouseX = -100;
			mouseY = -100;
		}
		super.renderBackground(context);
		mapScale = calculateMapScale();
		RenderSystem.setShaderColor(1, 1, 1, 1);

		if (fullscreen) {
			int left_width = bookWidth / 2 - 15;
			context.drawNineSlicedTexture(BOOK_FULLSCREEN, getGuiX(), getGuiY(), left_width, bookHeight, 50, 140, 218, 0, 0);
			context.drawNineSlicedTexture(BOOK_FULLSCREEN, getGuiX() + left_width, getGuiY(), 29, bookHeight, 50, 29, 218, 140, 0);
			context.drawNineSlicedTexture(BOOK_FULLSCREEN_R, getGuiX() + left_width + 29, getGuiY(), left_width + 1, bookHeight, 50, 140, 218, 0, 0);
		} else {
			context.drawTexture(BOOK, getGuiX(), getGuiY(), 0, 0, bookWidth, bookHeight, bookWidth, bookHeight);
		}

		if (worldAtlasData == null) return;

		RenderSystem.enableScissor(
			(int) (guiScale() * (getGuiX() + MAP_BORDER_WIDTH)),
			(int) (guiScale() * (getGuiY() + MAP_BORDER_HEIGHT)),
			(int) (guiScale() * mapWidth),
			(int) (guiScale() * mapHeight)
		);
		RenderSystem.enableBlend();
		RenderSystem.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

		RenderSystem.setShaderColor(1, 1, 1, state.is(DELETING_MARKER) ? 0.5f : 1.0f);
		renderTiles(context.getMatrices(), null, MAX_LIGHT);
		RenderSystem.setShaderColor(1, 1, 1, 1);

		// Overlay the frame so that edges of the map are smooth:
		if (fullscreen) {
			int left_width = bookWidth / 2 - 15;
			context.drawNineSlicedTexture(BOOK_FRAME_FULLSCREEN, getGuiX(), getGuiY(), left_width, bookHeight, 50, 140, 218, 0, 0);
			context.drawNineSlicedTexture(BOOK_FRAME_FULLSCREEN, getGuiX() + left_width, getGuiY(), 29, bookHeight, 50, 29, 218, 140, 0);
			context.drawNineSlicedTexture(BOOK_FRAME_FULLSCREEN_R, getGuiX() + left_width + 29, getGuiY(), left_width + 1, bookHeight, 50, 140, 218, 0, 0);
		} else {
			context.drawTexture(BOOK_FRAME, getGuiX(), getGuiY(), 0, 0, bookWidth, bookHeight, bookWidth, bookHeight);
		}

		context.getMatrices().push();
		context.getMatrices().translate(getGuiX(), getGuiY(), 0);
		float markerScale = getEffectiveScale() * (tilePixels / 16.0F);

		Map<UUID, PlayerSummary> friends = AntiqueAtlas.getOrderedFriends();

		for (Identifier id : overlays.keySet()) {
			overlays.get(id).onScreenRender(new AtlasOverlay.AtlasScreenRenderContext(this, context, mouseX, mouseY, markerScale, friends));
		}

		hoveredLandmark = null;
		hoveredFriend = null;
		if (!state.is(HIDING_MARKERS)) {
			if (isMouseOverMap) {
				double bestDistance = Double.MAX_VALUE;
				for (Map.Entry<Landmark, MarkerTexture> entry : worldAtlasData.getAllMarkers(tileChunks).entrySet()) {
					Landmark landmark = entry.getKey();
					MarkerTexture texture = entry.getValue();
					double markerX = worldXToScreenX(landmark.getOrDefault(LandmarkComponentTypes.POS, BlockPos.ORIGIN).getX());
					double markerY = worldZToScreenY(landmark.getOrDefault(LandmarkComponentTypes.POS, BlockPos.ORIGIN).getZ());
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
				boolean hovering = hoveredLandmark == landmark && markerModal.getParent() == null;
				BiFunction<Double, Double, Float> alpha = (x, y) -> state.is(PLACING_MARKER) || (state.is(DELETING_MARKER) && !hovering) || (hovering && x <= MAP_BORDER_WIDTH || x >= mapWidth + MAP_BORDER_WIDTH || y <= MAP_BORDER_HEIGHT || y >= mapHeight + MAP_BORDER_HEIGHT) ? 0.5f : 1.0f;
				renderMarker(context.getMatrices(), null, landmark, texture, 0, MAX_LIGHT, alpha, WorldAtlasData.landmarkIsEditable(landmark), hovering, markerScale);
				if (hovering && landmark.get(LandmarkComponentTypes.NAME) != null && !landmark.get(LandmarkComponentTypes.NAME).getString().isEmpty()) {
					context.drawTooltip(textRenderer, Stream.concat(Stream.of(landmark.get(LandmarkComponentTypes.NAME)), landmark.getOrDefault(LandmarkComponentTypes.LORE, new ArrayList<Text>()).stream().map(t -> t.copy().formatted(Formatting.GRAY))).toList(), (int) getMouseX() - getGuiX(), (int) getMouseY() - getGuiY());
				}
			});
		}

		context.getMatrices().pop();

		RenderSystem.disableScissor();

		if (fullscreen) {
			int left_width = bookWidth / 2 - 15;
			context.drawNineSlicedTexture(BOOK_FRAME_NARROW_FULLSCREEN, getGuiX(), getGuiY(), left_width, bookHeight, 50, 140, 218, 0, 0);
			context.drawNineSlicedTexture(BOOK_FRAME_NARROW_FULLSCREEN, getGuiX() + left_width, getGuiY(), 29, bookHeight, 50, 29, 218, 140, 0);
			context.drawNineSlicedTexture(BOOK_FRAME_NARROW_FULLSCREEN_R, getGuiX() + left_width + 29, getGuiY(), left_width + 1, bookHeight, 50, 140, 218, 0, 0);
		} else {
			context.drawTexture(BOOK_FRAME_NARROW, getGuiX(), getGuiY(), 0, 0, bookWidth, bookHeight, bookWidth, bookHeight);
		}

		markerScrollBox.getViewport().setClipped(state.is(HIDING_MARKERS));

		context.getMatrices().push();
		context.getMatrices().translate(getGuiX(), getGuiY(), 0);
		friends.forEach((uuid, friend) -> {
			boolean self = uuid.equals(SurveyorClient.getClientUuid());
			boolean hovering = hoveredFriend == friend && markerModal.getParent() == null;
			if (state.is(HIDING_MARKERS) && (!playerBookmark.isSelected() || self)) return;
			renderPlayer(context.getMatrices(), null, 0, MAX_LIGHT, friend, getEffectiveScale(), state.is(PLACING_MARKER) ? 0.5F : 1.0F, hovering, self);
			if (hovering && !self) {
				context.drawTooltip(textRenderer, Text.literal(friend.username()).formatted(friend.online() ? Formatting.LIGHT_PURPLE : Formatting.GRAY), (int) getMouseX() - getGuiX(), (int) getMouseY() - getGuiY());
			}
		});
		context.getMatrices().pop();

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
				context.drawText(textRenderer, Text.literal(hoveredLandmark.id().toString()), getGuiX() + bookWidth - textRenderer.getWidth(Text.literal(hoveredLandmark.id().toString())), getGuiY() - 12, 0xFFFFFFFF, true);
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

		if (markerModal.getParent() != null) {
			markerModal.setClipped(true);
			super.render(context, mouseX, mouseY, partialTick);
			markerModal.setClipped(false);
			markerModal.render(context, trueMouseX, trueMouseY, partialTick);
		} else {
			super.render(context, mouseX, mouseY, partialTick);
		}
	}

	@Override
	public double guiScale() {
		return MinecraftClient.getInstance().getWindow().getScaleFactor();
	}

	@Override
	public void close() {
		super.close();
		markerModal.closeChild();
		removeChild(markerCursor);
	}

	@Override
	public void onChildClosed(Component child) {
		if (child.equals(markerModal)) {
			removeChild(markerCursor);
		}
	}

	float getEffectiveScale() {
		return (float) (mapScale() / guiScale());
	}

	@Override
	public double getPixelsPerBlock()  {
		return (double) getEffectiveScale() * ((double) tilePixels()) / ((double) tileChunks() * 16.0);
	}

	@Override
	public int bookX() {
		return getGuiX();
	}

	@Override
	public int bookY() {
		return getGuiY();
	}

	@Override
	public int bookHeight() {
		return bookHeight;
	}

	@Override
	public int mapWidth() {
		return mapWidth;
	}

	@Override
	public int mapHeight() {
		return mapHeight;
	}

	@Override
	public double mapOffsetX() {
		return mapOffsetX;
	}

	@Override
	public double mapOffsetY() {
		return mapOffsetY;
	}

	@Override
	public int mapScale() {
		return mapScale;
	}

	@Override
	public int tilePixels() {
		return tilePixels;
	}

	@Override
	public int tileChunks() {
		return tileChunks;
	}

	@Override
	public PlayerEntity player() {
		return player;
	}

	@Override
	public int bookWidth() {
		return bookWidth;
	}

	@Override
	public WorldAtlasData worldAtlasData() {
		return worldAtlasData;
	}
}
