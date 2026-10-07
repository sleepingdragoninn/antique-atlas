package folk.sisby.antique_atlas.gametest;

import folk.sisby.antique_atlas.AntiqueAtlas;
import folk.sisby.antique_atlas.gui.AntiqueAtlasConfigScreen;
import folk.sisby.antique_atlas.gui.AtlasScreen;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.ParentElement;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Opens the atlas and the config screen in a fresh world and takes screenshots. Run with {@code ./gradlew runProductionClientGameTest}.
 */
public class AtlasClientGameTest implements FabricClientGameTest {
	private static final String GIVE_ATLAS = "give @a minecraft:book[minecraft:item_name=\"Antique Atlas\"]";

	@Override
	public void runTest(ClientGameTestContext context) {
		try (TestSingleplayerContext world = context.worldBuilder().create()) {
			world.getClientLevel().waitForChunksRender();
			world.getServer().runCommand(GIVE_ATLAS);
			context.waitTicks(40);
			context.takeScreenshot("antique_atlas_handheld");

			openAtlas(context);
			context.waitTicks(100);
			context.takeScreenshot("antique_atlas_screen");

			context.getInput().pressKey(GLFW.GLFW_KEY_MINUS);
			context.getInput().pressKey(GLFW.GLFW_KEY_MINUS);
			context.getInput().pressKey(GLFW.GLFW_KEY_LEFT);
			context.waitTicks(40);
			context.takeScreenshot("antique_atlas_screen_zoomed_out");

			context.getInput().pressKey(GLFW.GLFW_KEY_ESCAPE);
			context.waitTicks(20);
			context.takeScreenshot("antique_atlas_handheld_after");

			context.setScreen(() -> new AntiqueAtlasConfigScreen(null));
			context.waitTicks(20);
			context.takeScreenshot("antique_atlas_config");
			clickOption(context, "gui.antique_atlas.config.requireItem");
			context.waitTicks(5);
			context.clickScreenButton("gui.done");
			context.waitTicks(5);
			assertTrue(AntiqueAtlas.CONFIG.requireItem, "requireItem was not enabled by the config screen");
			assertTrue(readConfigFile().contains("requireItem = true"), "requireItem was not saved to the config file");

			world.getServer().runCommand("clear @a");
			context.waitTicks(20);
			context.getInput().pressKey(GLFW.GLFW_KEY_M);
			context.waitTicks(20);
			assertTrue(!context.computeOnClient(client -> client.currentScreen instanceof AtlasScreen), "atlas opened without an atlas item while requireItem is enabled");

			world.getServer().runCommand(GIVE_ATLAS);
			context.waitTicks(20);
			openAtlas(context);
			context.getInput().pressKey(GLFW.GLFW_KEY_ESCAPE);
			context.waitTicks(20);

			context.setScreen(() -> new AntiqueAtlasConfigScreen(null));
			context.waitTicks(5);
			clickOption(context, "gui.antique_atlas.config.requireItem");
			context.waitTicks(5);
			context.clickScreenButton("gui.done");
			context.waitTicks(5);
			assertTrue(!AntiqueAtlas.CONFIG.requireItem, "requireItem was not disabled by the config screen");
		}
	}

	private static void openAtlas(ClientGameTestContext context) {
		context.getInput().pressKey(GLFW.GLFW_KEY_M);
		context.waitForScreen(AtlasScreen.class);
	}

	private static void clickOption(ClientGameTestContext context, String key) {
		String label = Text.translatable(key).getString();
		double[] center = context.computeOnClient(client -> {
			ClickableWidget widget = findWidget(client.currentScreen.children(), label);
			assertTrue(widget != null, "no option button labelled " + label);
			int scale = client.getWindow().getScaleFactor();
			return new double[]{(widget.getX() + widget.getWidth() / 2.0) * scale, (widget.getY() + widget.getHeight() / 2.0) * scale};
		});
		context.getInput().setCursorPos(center[0], center[1]);
		context.getInput().pressMouse(GLFW.GLFW_MOUSE_BUTTON_LEFT);
	}

	private static ClickableWidget findWidget(List<? extends Element> elements, String label) {
		for (Element element : elements) {
			if (element instanceof ClickableWidget widget && widget.getMessage().getString().startsWith(label)) return widget;
			if (element instanceof ParentElement parent) {
				ClickableWidget widget = findWidget(parent.children(), label);
				if (widget != null) return widget;
			}
		}
		return null;
	}

	private static String readConfigFile() {
		Path path = FabricLoader.getInstance().getConfigDir().resolve("antique-atlas.toml");
		try {
			return Files.readString(path);
		} catch (IOException e) {
			throw new AssertionError("could not read " + path, e);
		}
	}

	private static void assertTrue(boolean condition, String message) {
		if (!condition) throw new AssertionError(message);
	}
}
