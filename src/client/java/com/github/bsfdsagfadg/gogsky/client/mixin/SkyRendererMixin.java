package com.github.bsfdsagfadg.gogsky.client.mixin;

import com.github.bsfdsagfadg.gogsky.client.ClientTickHandler;
import com.github.bsfdsagfadg.gogsky.client.GogSkyConfig;
import com.github.bsfdsagfadg.gogsky.client.render.SkyblockSkyRenderer;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.FogParameters;
import net.minecraft.client.renderer.SkyRenderer;
import com.mojang.blaze3d.vertex.PoseStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 核心 Mixin：将自定义天空效果注入到 1.21.8 的 SkyRenderer 中
 */
@Mixin(SkyRenderer.class)
public abstract class SkyRendererMixin {

    @Shadow @Final private GpuBuffer starBuffer;
    @Shadow @Final private RenderSystem.AutoStorageIndexBuffer starIndices;
    @Shadow private int starIndexCount;

    /**
     * 在渲染太阳、月亮和星星之前注入额外的大气效果（行星、极光、彩虹）
     */
    @Inject(method = "renderSunMoonAndStars", at = @At("HEAD"))
    private void onRenderSunMoonAndStars(PoseStack poseStack, MultiBufferSource.BufferSource bufferSource, float f, int i, float g, float h, FogParameters fogParameters, CallbackInfo ci) {
        Minecraft mc = Minecraft.getInstance();
        if (GogSkyConfig.isEnabled(mc.level)) {
            SkyblockSkyRenderer.renderExtra(poseStack, bufferSource, mc.level, ClientTickHandler.partialTicks, 0);
        }
    }

    /**
     * 替换原版的星星渲染，使用自定义的多层旋转星空
     */
    @Inject(method = "renderSunMoonAndStars", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/SkyRenderer;renderStars(Lnet/minecraft/client/renderer/FogParameters;FLcom/mojang/blaze3d/vertex/PoseStack;)V"))
    private void onRenderStars(PoseStack poseStack, MultiBufferSource.BufferSource bufferSource, float f, int i, float g, float h, FogParameters fogParameters, CallbackInfo ci) {
        Minecraft mc = Minecraft.getInstance();
        if (GogSkyConfig.isEnabled(mc.level)) {
            GpuBuffer indices = this.starIndices.getBuffer(this.starIndexCount);
            SkyblockSkyRenderer.renderStars(this.starBuffer, indices, this.starIndexCount, this.starIndices, poseStack, ClientTickHandler.partialTicks);
        }
    }

    /**
     * 放大太阳的渲染尺寸，以符合植物魔法的视觉风格
     */
    @Inject(method = "renderSun", at = @At("HEAD"))
    private void onRenderSun(float alpha, MultiBufferSource multiBufferSource, PoseStack poseStack, CallbackInfo ci) {
        if (GogSkyConfig.isEnabled(Minecraft.getInstance().level)) {
            poseStack.scale(2.0F, 1.0F, 2.0F);
        }
    }

    /**
     * 放大月亮的渲染尺寸
     */
    @Inject(method = "renderMoon", at = @At("HEAD"))
    private void onRenderMoon(int i, float alpha, MultiBufferSource multiBufferSource, PoseStack poseStack, CallbackInfo ci) {
        if (GogSkyConfig.isEnabled(Minecraft.getInstance().level)) {
            poseStack.scale(1.5F, 1.0F, 1.5F);
        }
    }
}