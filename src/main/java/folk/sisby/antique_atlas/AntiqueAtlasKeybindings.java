package folk.sisby.antique_atlas;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;


public class AntiqueAtlasKeybindings {
	public static final KeyBinding ATLAS_KEYMAPPING = new KeyBinding("key.antique_atlas.open", InputUtil.Type.KEYSYM, 77, "key.antique_atlas.category");

	public static void init() {
		KeyBindingHelper.registerKeyBinding(ATLAS_KEYMAPPING);
		ClientTickEvents.END_CLIENT_TICK.register(AntiqueAtlasKeybindings::onClientTick);
	}

	public static void onClientTick(MinecraftClient client) {
		while (ATLAS_KEYMAPPING.wasPressed()) AntiqueAtlas.openAtlasScreen();
	}
}
