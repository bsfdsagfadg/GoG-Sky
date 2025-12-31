package com.github.bsfdsagfadg.gogsky.client.mixin;

import com.github.bsfdsagfadg.gogsky.client.ClientTickHandler;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public class GameRendererMixin {
    @Inject(method = "render", at = @At("HEAD"))
    private void onRender(DeltaTracker deltaTracker, boolean bl, CallbackInfo ci) {
        ClientTickHandler.renderTick(deltaTracker.getGameTimeDeltaPartialTick(false));
    }
}