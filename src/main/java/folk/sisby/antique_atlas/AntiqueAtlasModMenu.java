package folk.sisby.antique_atlas;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import folk.sisby.antique_atlas.gui.AntiqueAtlasConfigScreen;

public class AntiqueAtlasModMenu implements ModMenuApi {
	@Override
	public ConfigScreenFactory<?> getModConfigScreenFactory() {
		return AntiqueAtlasConfigScreen::new;
	}
}
