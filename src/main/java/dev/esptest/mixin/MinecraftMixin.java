package dev.esptest.mixin;

import dev.esptest.EspManager;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Makes vanilla treat ESP targets as "glowing", so its own entity-outline pass (the one that draws
 * spectral-arrow / spectator outlines through walls) kicks in for them.
 */
@Mixin(Minecraft.class)
public class MinecraftMixin {

	@Inject(method = "shouldEntityAppearGlowing", at = @At("HEAD"), cancellable = true)
	private void esptest$forceGlow(Entity entity, CallbackInfoReturnable<Boolean> cir) {
		if (EspManager.outlineColor(entity) != 0) {
			cir.setReturnValue(true);
		}
	}
}
