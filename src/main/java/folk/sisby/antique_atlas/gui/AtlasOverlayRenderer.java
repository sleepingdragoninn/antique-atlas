package folk.sisby.antique_atlas.gui;

import folk.sisby.surveyor.PlayerSummary;
import net.minecraft.client.gui.DrawContext;

import java.util.Map;
import java.util.UUID;

public interface AtlasOverlayRenderer {
	void render(AtlasRenderContext context);

	record AtlasRenderContext(AtlasScreen screen, DrawContext context, int mouseX, int mouseY, float markerScale, Map<UUID, PlayerSummary> friends) {
	}
}
