package com.github.bsfdsagfadg.gogsky.client;

import com.github.bsfdsagfadg.gogsky.client.render.GogSkybox;
import me.flashyreese.mods.nuit.api.skyboxes.SkyboxType;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.resources.Identifier;

public class GogSkyClient implements ClientModInitializer {
    // Register the custom skybox type — static field ensures it's registered
    // when the class loads during Fabric's client init, before resource reload.
    public static final SkyboxType<GogSkybox> GOG_SKYBOX = SkyboxType.register(
            Identifier.fromNamespaceAndPath("gog-sky", "gog_skybox"),
            1,
            GogSkybox.CODEC
    );

    @Override
    public void onInitializeClient() {
        // Type registration happens via the static field above.
        // The JSON under assets/nuit/sky/gog_skybox.json is loaded
        // by nuit's resource listener and decoded using GOG_SKYBOX's codec.
    }
}
