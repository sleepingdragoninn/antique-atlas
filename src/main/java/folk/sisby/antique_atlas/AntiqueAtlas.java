package folk.sisby.antique_atlas;

import folk.sisby.antique_atlas.gui.AtlasScreen;
import folk.sisby.antique_atlas.gui.core.ScreenState;
import folk.sisby.antique_atlas.reloader.BiomeTileProviders;
import folk.sisby.antique_atlas.reloader.MarkerTextures;
import folk.sisby.antique_atlas.reloader.StructureTileProviders;
import folk.sisby.antique_atlas.reloader.TileTextures;
import folk.sisby.surveyor.PlayerSummary;
import folk.sisby.surveyor.WorldSummary;
import folk.sisby.surveyor.client.SurveyorClient;
import folk.sisby.surveyor.client.SurveyorClientEvents;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.CommonLifecycleEvents;
import net.fabricmc.fabric.api.resource.v1.ResourceLoader;
import net.fabricmc.fabric.api.resource.v1.pack.PackActivationType;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemGroups;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.resource.ResourceType;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class AntiqueAtlas implements ClientModInitializer {
	public static final String ID = "antique_atlas";
	public static final String NAME = "Antique Atlas";

	public static final Logger LOGGER = LogManager.getLogger(NAME);

	public static final AntiqueAtlasConfig CONFIG = AntiqueAtlasConfig.createToml(FabricLoader.getInstance().getConfigDir(), "", "antique-atlas", AntiqueAtlasConfig.class);
	public static final ScreenState<AtlasScreen> lastState = new ScreenState<>();

	public static final Identifier ATLAS_MODEL = AntiqueAtlas.id("atlas");

	public static final List<String> ATLAS_NAMES = List.of(
		"Antique Atlas"
	);

	public static Identifier id(String path) {
		return path.contains(":") ? Identifier.tryParse(path) : Identifier.of(ID, path);
	}

	public static ItemStack getHandheldAtlas() {
		ItemStack stack = Items.BOOK.getDefaultStack().copy();
		stack.set(DataComponentTypes.ITEM_NAME, Text.translatable("item.antique_atlas.atlas"));
		stack.set(DataComponentTypes.LORE, new LoreComponent(List.of(
			Text.translatable("item.antique_atlas.atlas.lore").setStyle(Style.EMPTY.withColor(Formatting.GRAY).withItalic(false)),
			Text.translatable("item.antique_atlas.atlas.hint", Text.translatable("item.antique_atlas.atlas")).setStyle(Style.EMPTY.withColor(Formatting.GRAY).withItalic(false))
		)));
		return stack;
	}

	public static AtlasScreen openAtlasScreen() {
		if (MinecraftClient.getInstance().currentScreen == null && (!AntiqueAtlas.CONFIG.requireItem || (MinecraftClient.getInstance().player != null && AntiqueAtlas.hasHandheldAtlas(MinecraftClient.getInstance().player)))) {
			AtlasScreen screen = new AtlasScreen();
			screen.init();
			screen.prepareToOpen();
			screen.tick();
			MinecraftClient.getInstance().setScreen(screen);
			return screen;
		}
		return null;
	}

	public static boolean isHandheldAtlas(ItemStack stack) {
		return stack.getItem() == Items.BOOK && ATLAS_NAMES.stream().anyMatch(n -> stack.getName().getString().toLowerCase().contains(n.toLowerCase()));
	}

	public static boolean hasHandheldAtlas(PlayerEntity player) {
		if (isHandheldAtlas(player.getOffHandStack())) return true;
		for (ItemStack itemStack : player.getInventory().getMainStacks()) {
			if (isHandheldAtlas(itemStack)) {
				return true;
			}
		}
		return false;
	}

	public static Map<UUID, PlayerSummary> getOrderedFriends() {
		Map<UUID, PlayerSummary> friends = SurveyorClient.getFriends();
		PlayerSummary playerSummary = friends.remove(SurveyorClient.getClientUuid());
		Map<UUID, PlayerSummary> orderedFriends = new LinkedHashMap<>(friends);
		if (playerSummary != null) orderedFriends.put(SurveyorClient.getClientUuid(), playerSummary);
		return orderedFriends;
	}

	@Override
	public void onInitializeClient() {
		AntiqueAtlasKeybindings.init();
		ResourceLoader resourceLoader = ResourceLoader.get(ResourceType.CLIENT_RESOURCES);
		resourceLoader.registerReloadListener(TileTextures.ID, TileTextures.getInstance());
		resourceLoader.registerReloadListener(MarkerTextures.ID, MarkerTextures.getInstance());
		resourceLoader.registerReloadListener(StructureTileProviders.ID, StructureTileProviders.getInstance());
		resourceLoader.registerReloadListener(BiomeTileProviders.ID, BiomeTileProviders.getInstance());
		resourceLoader.addListenerOrdering(TileTextures.ID, StructureTileProviders.ID);
		resourceLoader.addListenerOrdering(MarkerTextures.ID, StructureTileProviders.ID);
		resourceLoader.addListenerOrdering(TileTextures.ID, BiomeTileProviders.ID);

		SurveyorClientEvents.Register.terrainUpdated(id("world_data"), (s, k) -> WorldAtlasData.getOrCreate(s.dimension()).onTerrainUpdated(s, k));
		SurveyorClientEvents.Register.structuresAdded(id("world_data"), (s, k) -> WorldAtlasData.getOrCreate(s.dimension()).onStructuresAdded(s, k));
		SurveyorClientEvents.Register.landmarksAdded(id("world_data"), (s, k) -> WorldAtlasData.getOrCreate(s.dimension()).onLandmarksAdded(s, k));
		SurveyorClientEvents.Register.landmarksRemoved(id("world_data"), (s, k) -> WorldAtlasData.getOrCreate(s.dimension()).onLandmarksRemoved(s, k));
		ClientTickEvents.END_LEVEL_TICK.register((w -> SurveyorClient.getSummaries(MinecraftClient.getInstance().getNetworkHandler()).values().forEach(s -> WorldAtlasData.getOrCreate(s.dimension()).tick(s))));
		CommonLifecycleEvents.TAGS_LOADED.register(((manager, client) -> BiomeTileProviders.getInstance().registerFallbacks(manager.getOrThrow(RegistryKeys.BIOME))));
		ClientPlayConnectionEvents.DISCONNECT.register(((handler, client) -> BiomeTileProviders.getInstance().clearFallbacks()));
		ClientPlayConnectionEvents.DISCONNECT.register(((handler, client) -> WorldAtlasData.WORLDS.clear()));

		CreativeModeTabEvents.modifyOutputEvent(ItemGroups.TOOLS).register(output -> output.insertAfter(Items.MAP, getHandheldAtlas()));

		WorldSummary.enableTerrain();
		WorldSummary.enableStructures();
		WorldSummary.enableLandmarks();

		FabricLoader.getInstance().getModContainer(ID).ifPresent(c -> ResourceLoader.registerBuiltinPack(id("shader_patch"), c, Text.of("Shader Patch"), PackActivationType.NORMAL));
	}
}
