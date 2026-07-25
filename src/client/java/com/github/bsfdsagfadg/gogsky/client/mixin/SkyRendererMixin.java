package com.github.bsfdsagfadg.gogsky.client.mixin;

import com.github.bsfdsagfadg.gogsky.client.ClientTickHandler;
import com.github.bsfdsagfadg.gogsky.client.GogSkyConfig;
import com.github.bsfdsagfadg.gogsky.client.render.SkyblockSkyRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.FogParameters;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.SkyRenderer;
import com.mojang.blaze3d.vertex.PoseStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 核心 Mixin：将自定义天空效果注入到 SkyRenderer 中 (1.21.4 旧 API)
 */
@Mixin(SkyRenderer.class)
public abstract class SkyRendererMixin {

    @Inject(method = "renderSunMoonAndStars", at = @At("HEAD"))
    private void onRenderSunMoonAndStars(PoseStack poseStack, MultiBufferSource.BufferSource bufferSource, float f, int i, float g, float h, FogParameters fogParameters, CallbackInfo ci) {
        Minecraft mc = Minecraft.getInstance();
        if (GogSkyConfig.isEnabled(mc.level)) {
            SkyblockSkyRenderer.renderExtra(poseStack, bufferSource, mc.level, ClientTickHandler.partialTicks, 0);
        }
    }

    @Inject(method = "renderSun", at = @At("HEAD"))
    private void onRenderSun(float alpha, MultiBufferSource multiBufferSource, PoseStack poseStack, CallbackInfo ci) {
        if (GogSkyConfig.isEnabled(Minecraft.getInstance().level)) {
            poseStack.scale(2.0F, 1.0F, 2.0F);
        }
    }

    @Inject(method = "renderMoon", at = @At("HEAD"))
    private void onRenderMoon(int i, float alpha, MultiBufferSource multiBufferSource, PoseStack poseStack, CallbackInfo ci) {
        if (GogSkyConfig.isEnabled(Minecraft.getInstance().level)) {
            poseStack.scale(1.5F, 1.0F, 1.5F);
        }
    }
}