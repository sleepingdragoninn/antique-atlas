package folk.sisby.antique_atlas.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import folk.sisby.antique_atlas.AntiqueAtlas;
import folk.sisby.antique_atlas.gui.AtlasScreen;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ItemStack.class)
public class MixinItemStack {
	@ModifyReturnValue(method = "use", at = @At("RETURN"))
	private TypedActionResult<ItemStack> openAtlasWithItem(TypedActionResult<ItemStack> original, World world, PlayerEntity user, Hand hand) {
		ItemStack stack = user.getStackInHand(hand);
		if (world.isClient() && original.getResult() == ActionResult.PASS && AntiqueAtlas.isHandheldAtlas(stack) && MinecraftClient.getInstance().currentScreen == null) {
			AtlasScreen screen = new AtlasScreen();
			screen.init();
			screen.prepareToOpen();
			screen.tick();
			MinecraftClient.getInstance().setScreen(screen);
			return TypedActionResult.success(original.getValue());
		}
		return original;
	}
}
