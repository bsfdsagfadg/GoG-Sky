package com.github.bsfdsagfadg.gogsky.client.mixin;

import com.github.bsfdsagfadg.gogsky.client.render.StarColorHelper;
import net.minecraft.client.renderer.SkyRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/**
 * Mixins to SkyRenderer for GoG sky visual enhancements.
 *
 * Star coloring: SkyRenderer.renderStars creates Vector4f(b,b,b,b) for the
 * DynamicUniforms color modulator. We redirect R/G/B channels to match the
 * Botania multi-layer tint pattern (white, cyan, pink). GogSkybox sets the
 * desired tint via StarColorHelper.set() before each context.renderStars() call.
 */
@Mixin(SkyRenderer.class)
public class SkyRendererMixin {
    @ModifyArg(
            method = "renderStars",
            at = @At(value = "INVOKE", target = "Lorg/joml/Vector4f;<init>(FFFF)V"),
            index = 0
    )
    private float gogModifyStarR(float original) {
        return StarColorHelper.modifierR(original);
    }

    @ModifyArg(
            method = "renderStars",
            at = @At(value = "INVOKE", target = "Lorg/joml/Vector4f;<init>(FFFF)V"),
            index = 1
    )
    private float gogModifyStarG(float original) {
        return StarColorHelper.modifierG(original);
    }

    @ModifyArg(
            method = "renderStars",
            at = @At(value = "INVOKE", target = "Lorg/joml/Vector4f;<init>(FFFF)V"),
            index = 2
    )
    private float gogModifyStarB(float original) {
        return StarColorHelper.modifierB(original);
    }
}
