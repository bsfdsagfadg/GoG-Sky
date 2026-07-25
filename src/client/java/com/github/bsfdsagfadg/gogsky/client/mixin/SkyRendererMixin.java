package com.github.bsfdsagfadg.gogsky.client.mixin;

import com.github.bsfdsagfadg.gogsky.client.ClientTickHandler;
import com.github.bsfdsagfadg.gogsky.client.GogSkyConfig;
import com.github.bsfdsagfadg.gogsky.client.render.SkyblockSkyRenderer;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
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
    @Shadow @Final private RenderSystem.AutoStorageIndexBuffer quadIndices;
    @Shadow private int starIndexCount;

    @Inject(method = "renderSunMoonAndStars", at = @At("HEAD"))
    private void onRenderSunMoonAndStars(PoseStack poseStack, float sunAngle, float moonAngle, float starAngle, net.minecraft.world.level.MoonPhase moonPhase, float rainBrightness, float starBrightness, CallbackInfo ci) {
        Minecraft mc = Minecraft.getInstance();
        if (GogSkyConfig.isEnabled(mc.level)) {
            MultiBufferSource.BufferSource bufferSource = mc.renderBuffers().bufferSource();
            float celAng = sunAngle / (float) (2 * Math.PI); // sunAngle is radians, convert to 0-1
            SkyblockSkyRenderer.renderExtra(poseStack, bufferSource, mc.level, celAng, ClientTickHandler.partialTicks, 0);
            bufferSource.endBatch(); // Flush the rendering
        }
    }

    @Inject(method = "renderSunMoonAndStars", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/SkyRenderer;renderStars(FLcom/mojang/blaze3d/vertex/PoseStack;)V"))
    private void onRenderStars(PoseStack poseStack, float sunAngle, float moonAngle, float starAngle, net.minecraft.world.level.MoonPhase moonPhase, float rainBrightness, float starBrightness, CallbackInfo ci) {
        Minecraft mc = Minecraft.getInstance();
        if (GogSkyConfig.isEnabled(mc.level)) {
            GpuBuffer indices = this.quadIndices.getBuffer(this.starIndexCount);
            SkyblockSkyRenderer.renderStars(this.starBuffer, indices, this.starIndexCount, this.quadIndices, poseStack, sunAngle / (float) (2 * Math.PI), ClientTickHandler.partialTicks);
        }
    }

    @Inject(method = "renderSun", at = @At("HEAD"))
    private void onRenderSun(float alpha, PoseStack poseStack, CallbackInfo ci) {
        if (GogSkyConfig.isEnabled(Minecraft.getInstance().level)) {
            poseStack.scale(2.0F, 1.0F, 2.0F);
        }
    }

    @Inject(method = "renderMoon", at = @At("HEAD"))
    private void onRenderMoon(net.minecraft.world.level.MoonPhase moonPhase, float alpha, PoseStack poseStack, CallbackInfo ci) {
        if (GogSkyConfig.isEnabled(Minecraft.getInstance().level)) {
            poseStack.scale(1.5F, 1.0F, 1.5F);
        }
    }
}