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
import net.fabricmc.fabric.api.event.lifecycle.v1.CommonLifecycleEvents;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.ResourcePackActivationType;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.item.ModelPredicateProviderRegistry;
import net.minecraft.client.util.ModelIdentifier;
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

	public static final ModelIdentifier ATLAS_MODEL = new ModelIdentifier(AntiqueAtlas.id("atlas"), "inventory");

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
		return stack.isOf(Items.BOOK) && ATLAS_NAMES.stream().anyMatch(n -> stack.getName().getString().toLowerCase().contains(n.toLowerCase()));
	}

	public static boolean hasHandheldAtlas(PlayerEntity player) {
		if (isHandheldAtlas(player.getOffHandStack())) return true;
		for (ItemStack itemStack : player.getInventory().main) {
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
		ResourceManagerHelper.get(ResourceType.CLIENT_RESOURCES).registerReloadListener(TileTextures.getInstance());
		ResourceManagerHelper.get(ResourceType.CLIENT_RESOURCES).registerReloadListener(StructureTileProviders.getInstance());
		ResourceManagerHelper.get(ResourceType.CLIENT_RESOURCES).registerReloadListener(BiomeTileProviders.getInstance());
		ResourceManagerHelper.get(ResourceType.CLIENT_RESOURCES).registerReloadListener(MarkerTextures.getInstance());

		SurveyorClientEvents.Register.worldLoad(id("world_data"), WorldAtlasData::onLoad);
		SurveyorClientEvents.Register.terrainUpdated(id("world_data"), (w, s, k) -> WorldAtlasData.getOrCreate(w).onTerrainUpdated(w, s, k));
		SurveyorClientEvents.Register.structuresAdded(id("world_data"), (w, s, k) -> WorldAtlasData.getOrCreate(w).onStructuresAdded(w, s, k));
		SurveyorClientEvents.Register.landmarksAdded(id("world_data"), (w, s, k) -> WorldAtlasData.getOrCreate(w).onLandmarksAdded(w, s, k));
		SurveyorClientEvents.Register.landmarksRemoved(id("world_data"), (w, s, k) -> WorldAtlasData.getOrCreate(w).onLandmarksRemoved(w, s, k));
		ClientTickEvents.END_WORLD_TICK.register((w -> WorldAtlasData.getOrCreate(w).tick(w)));
		CommonLifecycleEvents.TAGS_LOADED.register(((manager, client) -> BiomeTileProviders.getInstance().registerFallbacks(manager.get(RegistryKeys.BIOME))));
		ClientPlayConnectionEvents.DISCONNECT.register(((handler, client) -> BiomeTileProviders.getInstance().clearFallbacks()));
		ClientPlayConnectionEvents.DISCONNECT.register(((handler, client) -> WorldAtlasData.WORLDS.clear()));

		ModelPredicateProviderRegistry.register(Items.BOOK, AntiqueAtlas.id("atlas"), ((stack, world, entity, seed) -> isHandheldAtlas(stack) ? 1.0F : 0.0F));
		ItemGroupEvents.modifyEntriesEvent(ItemGroups.TOOLS).register(e -> e.addAfter(Items.MAP, getHandheldAtlas()));

		WorldSummary.enableTerrain();
		WorldSummary.enableStructures();
		WorldSummary.enableLandmarks();

		FabricLoader.getInstance().getModContainer(ID).ifPresent(c -> ResourceManagerHelper.registerBuiltinResourcePack(id("shader_patch"), c, Text.of("Shader Patch"), ResourcePackActivationType.NORMAL));
	}
}
