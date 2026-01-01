package com.github.bsfdsagfadg.gogsky.client;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;

/**
 * 客户端 Tick 处理器，用于同步游戏时间和渲染帧时间
 */
public class ClientTickHandler {
    public static int ticksInGame = 0;
    public static float partialTicks = 0;

    /**
     * 获取总游戏时间（包含 partialTicks），用于平滑动画
     */
    public static float total() {
        return ticksInGame + partialTicks;
    }

    public static void init() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (!client.isPaused()) {
                ticksInGame++;
            }
        });
    }

    public static void renderTick(float renderTickTime) {
        partialTicks = renderTickTime;
    }
}