package folk.sisby.antique_atlas.gui;

import folk.sisby.antique_atlas.util.DrawTarget;
import folk.sisby.surveyor.PlayerSummary;
import net.minecraft.client.gui.DrawContext;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.UUID;

public interface AtlasOverlay {
	default void onScreenInit(AtlasScreen screen) {
	}

	default void onScreenRender(AtlasScreenRenderContext context) {
		onRender(new AtlasRenderContext(context.screen(), new DrawTarget.Gui(context.context()), context.mouseX(), context.mouseY(), context.markerScale(), context.friends()));
	}

	default void onRender(AtlasRenderContext context) {
	}

	record AtlasScreenRenderContext(AtlasScreen screen, DrawContext context, int mouseX, int mouseY, float markerScale, Map<UUID, PlayerSummary> friends) {
	}

	record AtlasRenderContext(AtlasRenderer renderer, DrawTarget target, @Nullable Integer mouseX, @Nullable Integer mouseY, float markerScale, Map<UUID, PlayerSummary> friends) {
	}
}
