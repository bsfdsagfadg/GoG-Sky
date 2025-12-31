package com.github.bsfdsagfadg.gogsky.client;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;

public class ClientTickHandler {
    public static int ticksInGame = 0;
    public static float partialTicks = 0;

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