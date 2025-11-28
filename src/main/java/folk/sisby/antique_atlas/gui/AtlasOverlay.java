package folk.sisby.antique_atlas.gui;

import folk.sisby.surveyor.PlayerSummary;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.UUID;

public interface AtlasOverlay {
	default void onScreenInit(AtlasScreen screen) {
	}

	default void onScreenRender(AtlasScreenRenderContext context) {
		onRender(new AtlasRenderContext(context.screen(), context.context().getMatrices(), null, context.mouseX(), context.mouseY(), AtlasScreen.MAX_LIGHT, context.markerScale(), context.friends()));
	}

	default void onRender(AtlasRenderContext context) {
	}

	record AtlasScreenRenderContext(AtlasScreen screen, DrawContext context, int mouseX, int mouseY, float markerScale, Map<UUID, PlayerSummary> friends) {
	}

	record AtlasRenderContext(AtlasRenderer renderer, MatrixStack matrices, VertexConsumerProvider vertexConsumers, @Nullable Integer mouseX, @Nullable Integer mouseY, int light, float markerScale, Map<UUID, PlayerSummary> friends) {
	}
}
