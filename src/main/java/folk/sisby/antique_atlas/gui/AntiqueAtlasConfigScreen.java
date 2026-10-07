package folk.sisby.antique_atlas.gui;

import folk.sisby.antique_atlas.AntiqueAtlas;
import folk.sisby.antique_atlas.AntiqueAtlasConfig;
import folk.sisby.kaleido.lib.quiltconfig.api.values.TrackedValue;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.option.GameOptionsScreen;
import net.minecraft.client.option.SimpleOption;
import net.minecraft.text.Text;

import java.util.List;
import java.util.function.Consumer;

public class AntiqueAtlasConfigScreen extends GameOptionsScreen {
	public AntiqueAtlasConfigScreen(Screen parent) {
		super(parent, MinecraftClient.getInstance().options, Text.translatable("gui.antique_atlas.config.title"));
	}

	@Override
	protected void addOptions() {
		AntiqueAtlasConfig config = AntiqueAtlas.CONFIG;
		body.addAll(
			option("requireItem", config.requireItem, v -> config.requireItem = v),
			option("fullscreen", config.fullscreen, v -> config.fullscreen = v),
			option("keepZoom", config.keepZoom, v -> config.keepZoom = v),
			option("keepOffset", config.keepOffset, v -> config.keepOffset = v)
		);
	}

	@SuppressWarnings("unchecked")
	private static SimpleOption<Boolean> option(String key, boolean value, Consumer<Boolean> setter) {
		TrackedValue<Boolean> tracked = (TrackedValue<Boolean>) AntiqueAtlas.CONFIG.getValue(List.of(key));
		return SimpleOption.ofBoolean("gui.antique_atlas.config." + key, SimpleOption.constantTooltip(Text.translatable("gui.antique_atlas.config." + key + ".tooltip")), value, v -> {
			setter.accept(v);
			tracked.setValue(v, true);
		});
	}
}
