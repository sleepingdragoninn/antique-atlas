package folk.sisby.antique_atlas.mixin;

import folk.sisby.antique_atlas.AntiqueAtlas;
import net.minecraft.client.color.block.BlockColors;
import net.minecraft.client.render.model.ModelLoader;
import net.minecraft.client.render.model.json.JsonUnbakedModel;
import net.minecraft.client.util.ModelIdentifier;
import net.minecraft.util.Identifier;
import net.minecraft.util.profiler.Profiler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.Map;

@Mixin(ModelLoader.class)
public abstract class MixinModelLoader {
	@Shadow protected abstract void addModel(ModelIdentifier modelId);

	@Inject(method = "<init>", at = @At(value = "INVOKE", target = "Ljava/util/Map;values()Ljava/util/Collection;"))
	protected void loadAtlasModel(BlockColors blockColors, Profiler profiler, Map<Identifier, JsonUnbakedModel> jsonUnbakedModels, Map<Identifier, List<ModelLoader.SourceTrackedData>> blockStates, CallbackInfo ci) {
		addModel(AntiqueAtlas.ATLAS_MODEL);
	}
}
