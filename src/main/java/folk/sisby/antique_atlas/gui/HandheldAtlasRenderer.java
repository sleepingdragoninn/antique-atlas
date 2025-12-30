package folk.sisby.antique_atlas.gui;

import folk.sisby.antique_atlas.AntiqueAtlas;
import folk.sisby.antique_atlas.WorldAtlasData;
import folk.sisby.antique_atlas.util.DrawBatcher;
import folk.sisby.antique_atlas.util.MathUtil;
import folk.sisby.surveyor.client.SurveyorClient;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.util.math.Rect2i;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.registry.RegistryKey;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.world.World;
import org.joml.Vector2d;

public record HandheldAtlasRenderer(int bookX, int bookY, int bookWidth, int bookHeight, int mapWidth, int mapHeight, int tilePixels, int tileChunks, double guiScale, double mapOffsetX, double mapOffsetY, int mapScale, PlayerEntity player, WorldAtlasData worldAtlasData, RegistryKey<World> dim) implements AtlasRenderer {
	public static HandheldAtlasRenderer fromContext(PlayerEntity player) {
		return new HandheldAtlasRenderer(
			0,
			0,
			DEFAULT_BOOK_WIDTH,
			DEFAULT_BOOK_HEIGHT,
			DEFAULT_BOOK_WIDTH - MAP_BORDER_WIDTH * 2,
			DEFAULT_BOOK_HEIGHT - MAP_BORDER_HEIGHT * 2,
			16,
			1,
			1,
			-player.getBlockX(),
			-player.getBlockZ(),
			1,
			player,
			WorldAtlasData.getOrCreate(player.getWorld().getRegistryKey()),
			player.getWorld().getRegistryKey()
		);
	}

	public void renderHandheldAtlas(MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light) {
		matrices.push();
		matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(180.0F));
		matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(180.0F));
		matrices.scale(0.38F * 142.0F / 218.0F, 0.38F * 142.0F / 218.0F, 0.38F);
		matrices.translate(-1.2D, -0.88D, 0D);
		matrices.scale(1.0F / 128.0F, 1.0F / 128.0F, 1.0F / 128.0F);

		DrawBatcher.drawSingle(matrices, vertexConsumers, AtlasScreen.BOOK, bookWidth, bookHeight, light, bookX, bookY, 0.01F, bookWidth, bookHeight, 0, 0, bookWidth, bookHeight, 0xFFFFFFFF, false);

		if (!(MinecraftClient.getInstance().currentScreen instanceof AtlasScreen)) {
			renderTiles(matrices, vertexConsumers, light);

			overlays.keySet().forEach(id -> overlays.get(id).onRender(new AtlasOverlay.AtlasRenderContext(this, matrices, vertexConsumers, null, null, light, 1.0F, AntiqueAtlas.getOrderedFriends())));

			Rect2i mapArea = new Rect2i(bookX + MAP_BORDER_WIDTH, bookY + MAP_BORDER_HEIGHT, mapWidth, mapHeight);

			worldAtlasData.getAllMarkers(tileChunks).forEach((landmark, texture) -> renderMarker(matrices, vertexConsumers, landmark, texture, -0.02F, light, (x, y) -> (float) MathHelper.clamp(MathUtil.innerDistanceToEdge(mapArea, new Vector2d(x, y)) / 32.0, 0, 1), false, false, 1));

			AntiqueAtlas.getOrderedFriends().forEach((uuid, friend) -> renderPlayer(matrices, vertexConsumers, -0.04F, light, friend, 1, 1, false, uuid.equals(SurveyorClient.getClientUuid())));

			DrawBatcher.drawSingle(matrices, vertexConsumers, BOOK_FRAME, bookWidth, bookHeight, light, bookX, bookY, -0.03F, bookWidth, bookHeight, 0, 0, bookWidth, bookHeight, 0xFFFFFFFF, true);
		}

		matrices.pop();
	}

	@Override
	public double getPixelsPerBlock() {
		return 1;
	}
}
