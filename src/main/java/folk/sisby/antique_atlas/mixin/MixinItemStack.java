package folk.sisby.antique_atlas.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import folk.sisby.antique_atlas.AntiqueAtlas;
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
	protected TypedActionResult<ItemStack> openAtlasWithItem(TypedActionResult<ItemStack> original, World world, PlayerEntity user, Hand hand) {
		return world.isClient() && original.getResult() == ActionResult.PASS && AntiqueAtlas.isHandheldAtlas(user.getStackInHand(hand)) && AntiqueAtlas.openAtlasScreen() != null ? TypedActionResult.success(original.getValue()) : original;
	}
}
