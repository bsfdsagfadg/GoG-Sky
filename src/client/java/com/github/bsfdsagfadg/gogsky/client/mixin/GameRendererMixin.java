package com.github.bsfdsagfadg.gogsky.client.mixin;

import com.github.bsfdsagfadg.gogsky.client.ClientTickHandler;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 注入 GameRenderer 以获取每帧的渲染内插值 (Partial Ticks)
 */
@Mixin(GameRenderer.class)
public class GameRendererMixin {
    @Inject(method = "render", at = @At("HEAD"))
    private void onRender(DeltaTracker deltaTracker, boolean bl, CallbackInfo ci) {
        // 将当前帧的 Delta 时间传递给全局 Tick 处理器，用于平滑渲染动画
        ClientTickHandler.renderTick(deltaTracker.getGameTimeDeltaPartialTick(false));
    }
}