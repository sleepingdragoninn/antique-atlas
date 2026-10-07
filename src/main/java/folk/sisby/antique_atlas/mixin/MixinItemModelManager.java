package folk.sisby.antique_atlas.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import folk.sisby.antique_atlas.AntiqueAtlas;
import net.minecraft.client.item.ItemModelManager;
import net.minecraft.client.render.item.ItemRenderState;
import net.minecraft.item.ItemDisplayContext;
import net.minecraft.item.ItemStack;
import net.minecraft.util.HeldItemContext;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ItemModelManager.class)
public class MixinItemModelManager {
	@ModifyExpressionValue(method = "update", at = @At(value = "INVOKE", target = "Lnet/minecraft/item/ItemStack;get(Lnet/minecraft/component/ComponentType;)Ljava/lang/Object;"))
	protected Object useAtlasModel(Object original, ItemRenderState renderState, ItemStack stack, ItemDisplayContext displayContext, World world, HeldItemContext heldItemContext, int seed) {
		return AntiqueAtlas.isHandheldAtlas(stack) ? AntiqueAtlas.ATLAS_MODEL : original;
	}
}
