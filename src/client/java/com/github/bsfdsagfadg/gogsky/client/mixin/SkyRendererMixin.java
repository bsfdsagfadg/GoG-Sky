package com.github.bsfdsagfadg.gogsky.client.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SkyRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/**
 * Mixins to SkyRenderer for GoG sky visual enhancements.
 *
 * Star coloring: SkyRenderer.renderStars creates Vector4f(b,b,b,b) for the
 * DynamicUniforms color modulator. We redirect R/G/B channels to match the
 * Botania multi-layer tint pattern (white, cyan, pink). GogSkybox sets the
 * desired tint via gogSetStarColor() before each context.renderStars() call.
 */
@Mixin(SkyRenderer.class)
public class SkyRendererMixin {
    @Unique
    private static float gogStarR = 1F;
    @Unique
    private static float gogStarG = 1F;
    @Unique
    private static float gogStarB = 1F;

    /** Called by GogSkybox before each context.renderStars() call. */
    public static void gogSetStarColor(float r, float g, float b) {
        gogStarR = r;
        gogStarG = g;
        gogStarB = b;
    }

    @ModifyArg(
            method = "renderStars",
            at = @At(value = "INVOKE", target = "Lorg/joml/Vector4f;<init>(FFFF)V"),
            index = 0
    )
    private float gogModifyStarR(float original) {
        return original * gogStarR;
    }

    @ModifyArg(
            method = "renderStars",
            at = @At(value = "INVOKE", target = "Lorg/joml/Vector4f;<init>(FFFF)V"),
            index = 1
    )
    private float gogModifyStarG(float original) {
        return original * gogStarG;
    }

    @ModifyArg(
            method = "renderStars",
            at = @At(value = "INVOKE", target = "Lorg/joml/Vector4f;<init>(FFFF)V"),
            index = 2
    )
    private float gogModifyStarB(float original) {
        return original * gogStarB;
    }
}
