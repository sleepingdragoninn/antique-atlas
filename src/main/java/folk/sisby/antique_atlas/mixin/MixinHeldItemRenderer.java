package folk.sisby.antique_atlas.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import folk.sisby.antique_atlas.AntiqueAtlas;
import folk.sisby.antique_atlas.WorldAtlasData;
import folk.sisby.antique_atlas.gui.AtlasScreen;
import folk.sisby.antique_atlas.gui.tiles.TileRenderIterator;
import folk.sisby.antique_atlas.util.DrawBatcher;
import folk.sisby.antique_atlas.util.DrawUtil;
import folk.sisby.antique_atlas.util.MathUtil;
import folk.sisby.antique_atlas.util.Rect;
import folk.sisby.surveyor.PlayerSummary;
import folk.sisby.surveyor.client.SurveyorClient;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.item.HeldItemRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.util.math.Rect2i;
import net.minecraft.item.ItemStack;
import net.minecraft.util.DyeColor;
import net.minecraft.util.Hand;
import net.minecraft.util.math.ColorHelper;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import org.joml.Vector2d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import static folk.sisby.antique_atlas.gui.AtlasScreen.*;

@Mixin(HeldItemRenderer.class)
public class MixinHeldItemRenderer {
	@Inject(method = "renderFirstPersonMap", at = @At("HEAD"), cancellable = true)
	void renderFirstPersonAtlas(MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, ItemStack stack, CallbackInfo ci) {
		if (MinecraftClient.getInstance().player == null || MinecraftClient.getInstance().world == null) return;
		if (!(AntiqueAtlas.isHandheldAtlas(stack))) return;
		// Refactor to actually abstract AtlasScreen code eventually pls

		matrices.push();
		matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(180.0F));
		matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(180.0F));

		float scale = 0.38F * 142.0F / 218.0F;
		matrices.scale(scale, scale, 0.38F);
		matrices.translate(-1.2D, -0.88D, 0D);
		matrices.scale(1.0F / 128.0F, 1.0F / 128.0F, 1.0F / 128.0F);

		int bookX = 0;
		int bookY = 0;
		int bookWidth = 310;
		int bookHeight = 218;
		int mapWidth = bookWidth - MAP_BORDER_WIDTH * 2;
		int mapHeight = bookHeight - MAP_BORDER_HEIGHT * 2;
		int tileChunks = 1;

		matrices.push();
		matrices.translate(0, 0, 0.01);
		try (DrawBatcher batcher = new DrawBatcher(matrices, vertexConsumers, AtlasScreen.BOOK, bookWidth, bookHeight, light)) {
			batcher.add(bookX, bookY, bookWidth, bookHeight, 0, 0, bookWidth, bookHeight, 0xFFFFFFFF);
		}
		matrices.pop();

		if (MinecraftClient.getInstance().currentScreen instanceof AtlasScreen) {
			matrices.pop();
			ci.cancel();
			return;
		}

		int mapOffsetX = -MinecraftClient.getInstance().player.getBlockX();
		int mapOffsetY = -MinecraftClient.getInstance().player.getBlockZ();
		int mapStartChunkX = MathUtil.roundToBase(screenXToWorldX(bookX + MAP_BORDER_WIDTH, bookX, mapOffsetX, mapWidth, 1) / 16.0, tileChunks) - 2 * tileChunks;
		int mapStartChunkZ = MathUtil.roundToBase(screenYToWorldZ(bookY + MAP_BORDER_HEIGHT, bookY, mapOffsetY, mapHeight, 1) / 16.0, tileChunks) - 2 * tileChunks;
		int mapEndChunkX = MathUtil.roundToBase(screenXToWorldX(bookX + MAP_BORDER_WIDTH + mapWidth, bookX, mapOffsetX, mapWidth, 1) / 16.0, tileChunks) + 2 * tileChunks;
		int mapEndChunkZ = MathUtil.roundToBase(screenYToWorldZ(bookY + MAP_BORDER_HEIGHT + mapHeight, bookY, mapOffsetY, mapHeight, 1) / 16.0, tileChunks) + 2 * tileChunks;
		double mapStartScreenX = worldXToScreenX(mapStartChunkX << 4, bookX, mapOffsetX, mapWidth, 1);
		double mapStartScreenY = worldZToScreenY(mapStartChunkZ << 4, bookY, mapOffsetY, mapHeight, 1);
		WorldAtlasData worldAtlasData = WorldAtlasData.getOrCreate(MinecraftClient.getInstance().world);
		TileRenderIterator tiles = new TileRenderIterator(worldAtlasData);
		tiles.setScope(new Rect(mapStartChunkX, mapStartChunkZ, mapEndChunkX, mapEndChunkZ));
		tiles.setStep(tileChunks);

		matrices.push();
		AtlasScreen.renderTiles(matrices, vertexConsumers, bookX + MAP_BORDER_WIDTH, bookY + MAP_BORDER_HEIGHT, mapWidth, mapHeight, mapStartScreenX, mapStartScreenY, 1, 16, 1, light, tiles);
		matrices.pop();

		matrices.push();
		matrices.translate(0, 0, -0.02);
		Rect2i mapArea = new Rect2i(bookX + MAP_BORDER_WIDTH, bookY + MAP_BORDER_HEIGHT, mapWidth, mapHeight);
		worldAtlasData.getAllMarkers(tileChunks).forEach((landmark, texture) -> {
			double markerX = worldXToScreenX(landmark.pos().getX(), bookX, mapOffsetX, mapWidth, 1) - bookX;
			double markerY = worldZToScreenY(landmark.pos().getZ(), bookY, mapOffsetY, mapHeight, 1) - bookY;
			DyeColor color = landmark.color();
			Vector2d markerPoint = new Vector2d(markerX, markerY);
			float alpha = (float) MathHelper.clamp(MathUtil.innerDistanceToEdge(mapArea, markerPoint) / 32.0, 0, 1);
			texture.draw(matrices, vertexConsumers, markerX, markerY, 1, tileChunks, color == null ? null : color.getColorComponents(), 1F, alpha, light);
		});
		matrices.pop();

		matrices.push();
		matrices.translate(0, 0, -0.04);
		Map<UUID, PlayerSummary> friends = SurveyorClient.getFriends();
		PlayerSummary playerSummary = friends.remove(SurveyorClient.getClientUuid());
		Map<UUID, PlayerSummary> orderedFriends = new LinkedHashMap<>(friends);
		if (playerSummary != null) orderedFriends.put(SurveyorClient.getClientUuid(), playerSummary);
		orderedFriends.forEach((uuid, friend) -> {
			float tint = friend.online() ? 1 : 0.5f;
			float greenTint = friend == playerSummary ? 1 : 0.7f;
			int argb = ColorHelper.Argb.getArgb(255, (int) (tint * 255), (int) (tint * greenTint * 255), (int) (tint * 255));
			double playerOffsetX = worldXToScreenX(MinecraftClient.getInstance().player.getPos().getX(), bookX, mapOffsetX, mapWidth, 1) - bookX;
			double playerOffsetY = worldZToScreenY(MinecraftClient.getInstance().player.getPos().getZ(), bookY, mapOffsetY, mapHeight, 1) - bookY;
			float playerRotation = ((float) Math.round(MinecraftClient.getInstance().player.getHeadYaw() / 360f * PLAYER_ROTATION_STEPS) / PLAYER_ROTATION_STEPS) * 360f;
			DrawUtil.drawCenteredWithRotation(matrices, vertexConsumers, PLAYER, playerOffsetX, playerOffsetY, 1, PLAYER_ICON_WIDTH, PLAYER_ICON_HEIGHT, playerRotation, light, argb);
		});

		matrices.pop();
		matrices.push();
		// Overlay the frame so that edges of the map are smooth:
		matrices.translate(0, 0, -0.03);
		try (DrawBatcher batcher = new DrawBatcher(matrices, vertexConsumers, BOOK_FRAME, bookWidth, bookHeight, light)) {
			batcher.add(bookX, bookY, bookWidth, bookHeight, 0, 0, bookWidth, bookHeight, 0xFFFFFFFF);
		}
		matrices.pop();

		matrices.pop();
		ci.cancel();
	}

	@ModifyExpressionValue(method = "renderFirstPersonItem", at = @At(value = "INVOKE", target = "Lnet/minecraft/item/ItemStack;isOf(Lnet/minecraft/item/Item;)Z", ordinal = 0))
	private boolean enableFirstPersonAtlasRendering(boolean original, AbstractClientPlayerEntity player, float tickDelta, float pitch, Hand hand, float swingProgress, ItemStack stack, float equipProgress, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light) {
		return original || AntiqueAtlas.isHandheldAtlas(stack);
	}
}
