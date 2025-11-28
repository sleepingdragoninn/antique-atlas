package folk.sisby.antique_atlas.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import folk.sisby.antique_atlas.AntiqueAtlas;
import folk.sisby.antique_atlas.gui.HandheldAtlasRenderer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.item.HeldItemRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(HeldItemRenderer.class)
public class MixinHeldItemRenderer {
	@Inject(method = "renderFirstPersonMap", at = @At("HEAD"), cancellable = true)
	void renderFirstPersonAtlas(MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, ItemStack stack, CallbackInfo ci) {
		if (MinecraftClient.getInstance().player == null || MinecraftClient.getInstance().world == null) return;
		if (!(AntiqueAtlas.isHandheldAtlas(stack))) return;
		HandheldAtlasRenderer.fromContext(MinecraftClient.getInstance().player).renderHandheldAtlas(matrices, vertexConsumers, light);
		ci.cancel();
	}

	@ModifyExpressionValue(method = "renderFirstPersonItem", at = @At(value = "INVOKE", target = "Lnet/minecraft/item/ItemStack;isOf(Lnet/minecraft/item/Item;)Z", ordinal = 0))
	private boolean enableFirstPersonAtlasRendering(boolean original, AbstractClientPlayerEntity player, float tickDelta, float pitch, Hand hand, float swingProgress, ItemStack stack, float equipProgress, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light) {
		return original || AntiqueAtlas.isHandheldAtlas(stack);
	}
}
