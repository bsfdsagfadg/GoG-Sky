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

@Mixin(SkyRenderer.class)
public abstract class SkyRendererMixin {

    @Shadow @Final private GpuBuffer starBuffer;
    @Shadow @Final private RenderSystem.AutoStorageIndexBuffer starIndices;
    @Shadow private int starIndexCount;

    @Inject(method = "renderSunMoonAndStars", at = @At("HEAD"))
    private void onRenderSunMoonAndStars(PoseStack poseStack, MultiBufferSource.BufferSource bufferSource, float f, int i, float g, float h, CallbackInfo ci) {
        Minecraft mc = Minecraft.getInstance();
        if (GogSkyConfig.isEnabled(mc.level)) {
            SkyblockSkyRenderer.renderExtra(poseStack, bufferSource, mc.level, ClientTickHandler.partialTicks, 0);
        }
    }

    @Inject(method = "renderSunMoonAndStars", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/SkyRenderer;renderStars(FLcom/mojang/blaze3d/vertex/PoseStack;)V"))
    private void onRenderStars(PoseStack poseStack, MultiBufferSource.BufferSource bufferSource, float f, int i, float g, float h, CallbackInfo ci) {
        Minecraft mc = Minecraft.getInstance();
        if (GogSkyConfig.isEnabled(mc.level)) {
            GpuBuffer indices = this.starIndices.getBuffer(this.starIndexCount);
            SkyblockSkyRenderer.renderStars(this.starBuffer, indices, this.starIndexCount, this.starIndices, poseStack, ClientTickHandler.partialTicks);
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