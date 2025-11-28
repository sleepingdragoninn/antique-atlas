package folk.sisby.antique_atlas.gui;

import folk.sisby.antique_atlas.AntiqueAtlas;
import folk.sisby.antique_atlas.MarkerTexture;
import folk.sisby.antique_atlas.TileTexture;
import folk.sisby.antique_atlas.WorldAtlasData;
import folk.sisby.antique_atlas.gui.core.ScreenState;
import folk.sisby.antique_atlas.gui.tiles.SubTile;
import folk.sisby.antique_atlas.gui.tiles.SubTileQuartet;
import folk.sisby.antique_atlas.gui.tiles.TileRenderIterator;
import folk.sisby.antique_atlas.util.ColorUtil;
import folk.sisby.antique_atlas.util.DrawBatcher;
import folk.sisby.antique_atlas.util.DrawUtil;
import folk.sisby.antique_atlas.util.MathUtil;
import folk.sisby.antique_atlas.util.Rect;
import folk.sisby.surveyor.PlayerSummary;
import folk.sisby.surveyor.landmark.Landmark;
import folk.sisby.surveyor.landmark.component.LandmarkComponentTypes;
import it.unimi.dsi.fastutil.objects.Reference2ObjectArrayMap;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ColorHelper;
import net.minecraft.util.math.MathHelper;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiFunction;

public interface AtlasRenderer {
	Map<Identifier, AtlasOverlay> overlays = new HashMap<>();

	static void registerOverlay(Identifier id, AtlasOverlay overlay) {
		overlays.put(id, overlay);
	}

	Identifier BOOK = AntiqueAtlas.id("textures/gui/book.png");
	Identifier BOOK_FULLSCREEN = AntiqueAtlas.id("textures/gui/book_fullscreen.png");
	Identifier BOOK_FULLSCREEN_R = AntiqueAtlas.id("textures/gui/book_fullscreen_r.png");
	Identifier BOOK_FRAME = AntiqueAtlas.id("textures/gui/book_frame.png");
	Identifier BOOK_FRAME_FULLSCREEN = AntiqueAtlas.id("textures/gui/book_frame_fullscreen.png");
	Identifier BOOK_FRAME_FULLSCREEN_R = AntiqueAtlas.id("textures/gui/book_frame_fullscreen_r.png");
	Identifier BOOK_FRAME_NARROW = AntiqueAtlas.id("textures/gui/book_frame_narrow.png");
	Identifier BOOK_FRAME_NARROW_FULLSCREEN = AntiqueAtlas.id("textures/gui/book_frame_narrow_fullscreen.png");
	Identifier BOOK_FRAME_NARROW_FULLSCREEN_R = AntiqueAtlas.id("textures/gui/book_frame_narrow_fullscreen_r.png");
	Identifier PLAYER = AntiqueAtlas.id("textures/gui/player.png");
	Identifier ERASER = AntiqueAtlas.id("textures/gui/eraser.png");
	Identifier ICON_ADD_MARKER = AntiqueAtlas.id("textures/gui/icons/add_marker.png");
	Identifier ICON_DELETE_MARKER = AntiqueAtlas.id("textures/gui/icons/del_marker.png");
	Identifier ICON_SHOW_MARKERS = AntiqueAtlas.id("textures/gui/icons/show_markers.png");
	Identifier ICON_HIDE_MARKERS = AntiqueAtlas.id("textures/gui/icons/hide_markers.png");
	Text TEXT_ADD_MARKER = Text.translatable("gui.antique_atlas.addMarker");
	Text TEXT_ADD_MARKER_HERE = Text.translatable("gui.antique_atlas.addMarkerHere");

	int DEFAULT_BOOK_WIDTH = 310;
	int DEFAULT_BOOK_HEIGHT = 218;
	int MAP_BORDER_WIDTH = 17;
	int MAP_BORDER_HEIGHT = 11;
	float PLAYER_ROTATION_STEPS = 16;
	int PLAYER_ICON_WIDTH = 7;
	int PLAYER_ICON_HEIGHT = 8;
	int BOOKMARK_SPACING = 2;
	int MARKER_SIZE = 32;
	int NAVIGATE_STEP = 24; // How much the map view is offset, in blocks, per click (or per tick).
	int MAX_LIGHT = 15728640;

	ScreenState.State<AtlasScreen> NORMAL = new ScreenState.ToggleState<>();
	ScreenState.State<AtlasScreen> PLACING_MARKER = new ScreenState.ToggleState<>(s -> s.addMarkerBookmark);
	ScreenState.State<AtlasScreen> DELETING_MARKER = new ScreenState.ToggleState<>(s -> s.deleteMarkerBookmark, s -> s.addChild(s.eraser), s -> s.removeChild(s.eraser));
	ScreenState.State<AtlasScreen> HIDING_MARKERS = new ScreenState.ToggleState<>(s -> s.markerVisibilityBookmark, s -> {
		s.markerVisibilityBookmark.setTitle(Text.translatable("gui.antique_atlas.showMarkers"));
		s.markerVisibilityBookmark.setIconTexture(ICON_SHOW_MARKERS);
	}, s -> {
		s.clearTargetBookmarks(s.playerBookmark);
		s.markerVisibilityBookmark.setTitle(Text.translatable("gui.antique_atlas.hideMarkers"));
		s.markerVisibilityBookmark.setIconTexture(ICON_HIDE_MARKERS);
	});

	int bookX();

	int bookY();

	int bookWidth();

	int bookHeight();

	int mapWidth();

	int mapHeight();

	double mapOffsetX();

	double mapOffsetY();

	int tilePixels();

	int tileChunks();

	int mapScale();

	PlayerEntity player();

	WorldAtlasData worldAtlasData();

	double getPixelsPerBlock();

	double guiScale();

	default int screenXToWorldX(double screenX) {
		return screenXToWorldX(screenX, bookX(), mapOffsetX(), mapWidth(), getPixelsPerBlock());
	}

	default int screenYToWorldZ(double screenY) {
		return screenYToWorldZ(screenY, bookY(), mapOffsetY(), mapHeight(), getPixelsPerBlock());
	}

	default double worldXToScreenX(double x) {
		return worldXToScreenX(x, bookX(), mapOffsetX(), mapWidth(), getPixelsPerBlock());
	}

	default double worldZToScreenY(double z) {
		return worldZToScreenY(z, bookY(), mapOffsetY(), mapHeight(), getPixelsPerBlock());
	}

	static int screenXToWorldX(double screenX, int bookX, double mapOffsetX, int mapWidth, double pixelsPerBlock) {
		double mapX = (int) Math.round(screenX - bookX - MAP_BORDER_WIDTH);
		return (int) Math.round((mapX - (mapWidth / 2f) - mapOffsetX) / pixelsPerBlock);
	}

	static int screenYToWorldZ(double screenY, int bookY, double mapOffsetY, int mapHeight, double pixelsPerBlock) {
		double mapY = (int) Math.round(screenY - bookY - MAP_BORDER_HEIGHT);
		return (int) Math.round((mapY - (mapHeight / 2f) - mapOffsetY) / pixelsPerBlock);
	}

	static double worldXToScreenX(double x, int bookX, double mapOffsetX, int mapWidth, double pixelsPerBlock) {
		double mapX = x * pixelsPerBlock + mapOffsetX + (mapWidth / 2f);
		return mapX + bookX + MAP_BORDER_WIDTH;
	}

	static double worldZToScreenY(double z, int bookY, double mapOffsetY, int mapHeight, double pixelsPerBlock) {
		double mapY = z * pixelsPerBlock + mapOffsetY + (mapHeight / 2f);
		return mapY + bookY + MAP_BORDER_HEIGHT;
	}

	default void renderMarker(MatrixStack matrices, VertexConsumerProvider vertexConsumers, Landmark landmark, MarkerTexture texture, float z, int light, BiFunction<Double, Double, Float> alpha, boolean pinned, boolean hovering, float markerScale) {
		double markerX = worldXToScreenX(landmark.getOrDefault(LandmarkComponentTypes.POS, BlockPos.ORIGIN).getX()) - bookX();
		double markerY = worldZToScreenY(landmark.getOrDefault(LandmarkComponentTypes.POS, BlockPos.ORIGIN).getZ()) - bookY();

		float tint = hovering ? 0.8f : 1.0f;

		if (pinned) {
			markerX = MathHelper.clamp(markerX, MAP_BORDER_WIDTH, mapWidth() + MAP_BORDER_WIDTH);
			markerY = MathHelper.clamp(markerY, MAP_BORDER_HEIGHT, mapHeight() + MAP_BORDER_HEIGHT);
		}

		Integer color = landmark.get(LandmarkComponentTypes.COLOR);
		texture.draw(matrices, vertexConsumers, markerX, markerY, z, markerScale, tileChunks(), color == null ? null : ColorUtil.componentsFromRgb(color), tint, alpha.apply(markerX, markerY), light);
	}

	default void renderPlayer(MatrixStack matrices, VertexConsumerProvider vertexConsumers, float z, int light, PlayerSummary player, float iconScale, float alpha, boolean hovering, boolean self) {
		double playerOffsetX = worldXToScreenX(player.pos().getX()) - bookX();
		double playerOffsetY = worldZToScreenY(player.pos().getZ()) - bookY();

		playerOffsetX = MathHelper.clamp(playerOffsetX, MAP_BORDER_WIDTH, mapWidth() + MAP_BORDER_WIDTH);
		playerOffsetY = MathHelper.clamp(playerOffsetY, MAP_BORDER_HEIGHT, mapHeight() + MAP_BORDER_HEIGHT);

		// Draw the icon:
		float tint = (player.online() ? 1 : 0.5f) * (hovering ? 0.9f : 1);
		float greenTint = self ? 1 : 0.7f;
		int argb = ColorHelper.Argb.getArgb((int) (alpha * 255.0), (int) (tint * 255), (int) (tint * greenTint * 255), (int) (tint * 255));
		float playerRotation = ((float) Math.round(player.yaw() / 360f * PLAYER_ROTATION_STEPS) / PLAYER_ROTATION_STEPS) * 360f;

		DrawUtil.drawCenteredWithRotation(matrices, vertexConsumers, PLAYER, playerOffsetX, playerOffsetY, z, iconScale, PLAYER_ICON_WIDTH, PLAYER_ICON_HEIGHT, playerRotation, light, argb);
	}

	default void renderTiles(MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light) {
		int mapStartChunkX = MathUtil.roundToBase(screenXToWorldX(bookX()) >> 4, tileChunks()) - 2 * tileChunks();
		int mapStartChunkZ = MathUtil.roundToBase(screenYToWorldZ(bookY()) >> 4, tileChunks()) - 2 * tileChunks();
		int mapEndChunkX = MathUtil.roundToBase(screenXToWorldX(bookX() + bookWidth()) >> 4, tileChunks()) + 2 * tileChunks();
		int mapEndChunkZ = MathUtil.roundToBase(screenYToWorldZ(bookY() + bookHeight()) >> 4, tileChunks()) + 2 * tileChunks();
		double mapStartScreenX = worldXToScreenX(mapStartChunkX << 4);
		double mapStartScreenY = worldZToScreenY(mapStartChunkZ << 4);
		TileRenderIterator tiles = new TileRenderIterator(worldAtlasData());
		tiles.setScope(new Rect(mapStartChunkX, mapStartChunkZ, mapEndChunkX, mapEndChunkZ));
		tiles.setStep(tileChunks());
		int mapX = bookX() + MAP_BORDER_WIDTH;
		int mapY = bookY() + MAP_BORDER_HEIGHT;
		float effectiveScale = (float)(mapScale()/guiScale());
		matrices.push();
		matrices.translate(mapStartScreenX, mapStartScreenY, 0);
		matrices.scale(effectiveScale, effectiveScale, 1.0F);

		Map<TileTexture, Collection<SubTile>> tileTextures = new Reference2ObjectArrayMap<>();
		for (SubTileQuartet subTiles : tiles) {
			for (SubTile subtile : subTiles) {
				if (subtile == null || subtile.texture == null) continue;
				tileTextures.computeIfAbsent(subtile.texture, k -> new ArrayList<>()).add(subtile.copy());
			}
		}
		int subTilePixels = tilePixels() / 2;
		tileTextures.forEach((texture, subtiles) -> {
			try (DrawBatcher batcher = new DrawBatcher(matrices, vertexConsumers, texture.id(), 32, 48, light)) {
				for (SubTile subtile : subtiles) {
					int drawX = subtile.x * subTilePixels;
					int drawY = subtile.y * subTilePixels;
					// a non-scope bounds check allows subtile-level accuracy, and keeps border tiling accurate.
					if (drawX * effectiveScale > mapX + mapWidth() - mapStartScreenX || drawY * effectiveScale > mapY + mapHeight() - mapStartScreenY || (drawX + subTilePixels) * effectiveScale < mapX - mapStartScreenX || (drawY + subTilePixels) * effectiveScale < mapY - mapStartScreenY) continue;
					batcher.add(drawX, drawY, 0, subTilePixels, subTilePixels, subtile.getTextureU() * 8, subtile.getTextureV() * 8, 8, 8, 0xFFFFFFFF);
				}
			}
		});

		matrices.pop();
	}
}
