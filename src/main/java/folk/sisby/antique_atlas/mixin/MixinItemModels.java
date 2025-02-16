package folk.sisby.antique_atlas.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import folk.sisby.antique_atlas.AntiqueAtlas;
import net.minecraft.client.render.item.ItemModels;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ItemModels.class)
public class MixinItemModels {
	@ModifyReturnValue(method = "getModel(Lnet/minecraft/item/ItemStack;)Lnet/minecraft/client/render/model/BakedModel;", at = @At("RETURN"))
	private BakedModel useAtlasBookModel(BakedModel original, ItemStack stack) {
		if (AntiqueAtlas.isHandheldAtlas(stack)) {
			return ((ItemModels) (Object) this).getModelManager().getModel(AntiqueAtlas.ATLAS_MODEL);
		}
		return original;
	}
}
