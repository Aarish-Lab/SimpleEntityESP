package dev.esptest.mixin;

import dev.esptest.EspManager;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * After vanilla has filled the render state of an entity, replace the (team) outline colour with the ESP colour.
 */
@Mixin(EntityRenderer.class)
public abstract class EntityRendererMixin<T extends Entity, S extends EntityRenderState> {

	@Inject(method = "extractRenderState", at = @At("TAIL"))
	private void esptest$applyColor(T entity, S state, float partialTick, CallbackInfo ci) {
		int color = EspManager.outlineColor(entity);
		if (color != 0) {
			state.outlineColor = color;
		}
	}
}
